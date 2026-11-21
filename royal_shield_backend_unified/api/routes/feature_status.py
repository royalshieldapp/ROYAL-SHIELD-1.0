"""Truthful availability status for advanced release features."""

import math
from datetime import date, datetime, time, timezone

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from api.contracts.status import AvailabilityResponse, degraded, ready, unavailable
from services.forecast.artifact_store import PostgresArtifactStore
from services.forecast.importers.miami_dade import APPROVED_SOURCE_DATASETS
from services.forecast.models import (
    ForecastObservation,
    ForecastTrainingRun,
    IngestionRun,
)
from services.forecast.quality import DataQualityReport, audit_observations
from services.forecast.training import FEATURE_SCHEMA
from services.geospatial.database.connection import get_db


router = APIRouter(prefix="/api/v1/features", tags=["Feature Availability"])


MAX_FORECAST_FRESHNESS_DAYS = 365
READY_FORECAST_FRESHNESS_DAYS = 7


class AdvancedFeatureStatusResponse(BaseModel):
    vpn: AvailabilityResponse
    forecast: AvailabilityResponse
    threatMap: AvailabilityResponse
    automations: AvailabilityResponse
    reports: AvailabilityResponse
    policies: AvailabilityResponse


def _unavailable_forecast() -> AvailabilityResponse:
    return unavailable("FORECAST_MODEL_NOT_VERIFIED")


def _date_as_utc(value: date) -> datetime:
    return datetime.combine(value, time.min, tzinfo=timezone.utc)


def _artifact_metadata_is_compatible(artifact) -> bool:
    """Check only metadata that the inference service requires before use."""

    metrics = artifact.metrics
    if not isinstance(metrics, dict):
        return False
    calibration = metrics.get("calibration")
    target_p95 = calibration.get("target_p95") if isinstance(calibration, dict) else None
    resolution = metrics.get("h3_resolution")
    return (
        artifact.model_format == "xgboost-json"
        and metrics.get("feature_schema") == list(FEATURE_SCHEMA)
        and isinstance(target_p95, (int, float))
        and not isinstance(target_p95, bool)
        and math.isfinite(target_p95)
        and target_p95 > 0
        and isinstance(resolution, int)
        and not isinstance(resolution, bool)
        and 0 <= resolution <= 15
    )


def _forecast_status_from_evidence(
    *,
    artifact,
    quality: DataQualityReport,
    observation_provenance: set[tuple[str, str]],
    completed_ingestion_provenance: set[tuple[str, str]],
    training_run,
) -> AvailabilityResponse:
    """Return a status only when persisted model and data evidence agree."""

    if (
        quality.valid_rows < 1
        or quality.h3_cells < 1
        or quality.source_start is None
        or quality.source_end is None
        or quality.latest_observation is None
        or quality.freshness is None
        or quality.freshness < 0
        or quality.freshness > MAX_FORECAST_FRESHNESS_DAYS
    ):
        return _unavailable_forecast()

    if (
        not observation_provenance
        or not observation_provenance.issubset(APPROVED_SOURCE_DATASETS)
        or not observation_provenance.issubset(completed_ingestion_provenance)
    ):
        return _unavailable_forecast()

    if (
        training_run is None
        or artifact.source_start is None
        or artifact.source_end is None
        or artifact.source_start > artifact.source_end
        or artifact.source_start != training_run.source_start
        or artifact.source_end != training_run.source_end
        or artifact.source_start != quality.source_start
        or artifact.source_end != quality.source_end
        or not _artifact_metadata_is_compatible(artifact)
    ):
        return _unavailable_forecast()

    sources = sorted({source for source, _dataset in observation_provenance})
    data = {
        "observationCount": quality.valid_rows,
        "h3CellCount": quality.h3_cells,
        "sourceStart": quality.source_start.isoformat(),
        "sourceEnd": quality.source_end.isoformat(),
        "artifactFormat": artifact.model_format,
    }
    common = {
        "data": data,
        "sources": sources,
        "data_freshness": _date_as_utc(quality.latest_observation),
        "model_version": artifact.version,
    }
    if quality.freshness > READY_FORECAST_FRESHNESS_DAYS:
        return degraded("FORECAST_DATA_AGED", **common)
    return ready(**common)


def _verified_forecast_status(db: Session) -> AvailabilityResponse:
    """Load all forecast evidence from durable records, or fail closed."""

    try:
        artifact = PostgresArtifactStore(db).load_active()
        quality = audit_observations(db)
        observation_provenance = set(
            db.execute(
                select(
                    ForecastObservation.source,
                    ForecastObservation.source_dataset,
                ).distinct()
            ).all()
        )
        completed_ingestion_provenance = set(
            db.execute(
                select(IngestionRun.source, IngestionRun.source_dataset)
                .where(
                    IngestionRun.status == "COMPLETED",
                    IngestionRun.accepted_count > 0,
                )
                .distinct()
            ).all()
        )
        training_run = db.scalar(
            select(ForecastTrainingRun)
            .where(
                ForecastTrainingRun.version == artifact.version,
                ForecastTrainingRun.status == "COMPLETED",
                ForecastTrainingRun.promoted.is_(True),
                ForecastTrainingRun.artifact_sha256 == artifact.sha256,
            )
            .order_by(ForecastTrainingRun.completed_at.desc())
        )
        return _forecast_status_from_evidence(
            artifact=artifact,
            quality=quality,
            observation_provenance=observation_provenance,
            completed_ingestion_provenance=completed_ingestion_provenance,
            training_run=training_run,
        )
    except Exception:
        return _unavailable_forecast()


@router.get("/advanced-status", response_model=AdvancedFeatureStatusResponse)
async def get_advanced_feature_status(
    db: Session = Depends(get_db),
) -> AdvancedFeatureStatusResponse:
    """Return verified states based on active database evidence."""

    return AdvancedFeatureStatusResponse(
        vpn=unavailable("VPN_E2E_NOT_VERIFIED"),
        forecast=_verified_forecast_status(db),
        threatMap=unavailable("THREAT_DATA_NOT_VERIFIED"),
        automations=unavailable("AUTOMATION_ACTIONS_NOT_VERIFIED"),
        reports=unavailable("REPORT_SOURCES_NOT_VERIFIED"),
        policies=unavailable("POLICY_ENFORCEMENT_NOT_VERIFIED"),
    )
