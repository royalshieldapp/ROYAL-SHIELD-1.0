"""Checksum-verified inference over durable Forecast observations."""

from collections.abc import Sequence
from dataclasses import dataclass
from datetime import date
import math

import numpy as np

from sqlalchemy import select
from sqlalchemy.orm import Session
import xgboost as xgb

from services.forecast.artifact_store import LoadedArtifact, PostgresArtifactStore
from services.forecast.models import ForecastObservation
from services.forecast.training import FEATURE_SCHEMA


class InferenceUnavailableError(RuntimeError):
    """Safe internal signal that inference evidence is incomplete."""


@dataclass(frozen=True)
class CellPrediction:
    status: str
    h3_cell: str
    horizon_days: int
    predicted_event_count: float
    risk_score: float
    model_version: str
    sources: tuple[str, ...]
    data_freshness: date


class ForecastInferenceService:
    def __init__(self, session: Session):
        self.session = session

    def predict_cell(
        self,
        h3_cell: str,
        *,
        horizon_days: int,
        as_of: date,
        artifact: LoadedArtifact | None = None,
    ) -> CellPrediction:
        return self.predict_cells(
            (h3_cell,),
            horizon_days=horizon_days,
            as_of=as_of,
            artifact=artifact,
        )[0]

    def predict_cells(
        self,
        h3_cells: Sequence[str],
        *,
        horizon_days: int,
        as_of: date,
        artifact: LoadedArtifact | None = None,
        observations: Sequence[ForecastObservation] | None = None,
    ) -> tuple[CellPrediction, ...]:
        """Predict multiple verified cells with one artifact load and one model pass."""

        cells = tuple(dict.fromkeys(h3_cells))
        if not cells or any(not cell for cell in cells) or horizon_days < 1:
            raise ValueError("h3_cells and a positive horizon_days are required")

        if artifact is None:
            try:
                artifact = PostgresArtifactStore(self.session).load_active()
            except Exception as error:
                raise InferenceUnavailableError("active artifact is unavailable") from error

        metrics = artifact.metrics
        feature_schema = metrics.get("feature_schema")
        calibration = metrics.get("calibration")
        target_p95 = calibration.get("target_p95") if isinstance(calibration, dict) else None
        if feature_schema != list(FEATURE_SCHEMA):
            raise InferenceUnavailableError("artifact feature schema is unavailable")
        if not isinstance(target_p95, (int, float)) or not math.isfinite(target_p95) or target_p95 <= 0:
            raise InferenceUnavailableError("artifact calibration is unavailable")
        if artifact.model_format != "xgboost-json":
            raise InferenceUnavailableError("artifact format is unavailable")

        if observations is None:
            try:
                observations = tuple(
                    self.session.scalars(
                        select(ForecastObservation)
                        .where(
                            ForecastObservation.h3_cell.in_(cells),
                            ForecastObservation.observed_date <= as_of,
                        )
                        .order_by(
                            ForecastObservation.h3_cell,
                            ForecastObservation.observed_date,
                            ForecastObservation.id,
                        )
                    )
                )
            except Exception as error:
                raise InferenceUnavailableError("durable cell history is unavailable") from error

        rows_by_cell: dict[str, list[ForecastObservation]] = {cell: [] for cell in cells}
        for row in observations:
            row_cell = getattr(row, "h3_cell", cells[0] if len(cells) == 1 else None)
            if row_cell in rows_by_cell and row.observed_date <= as_of:
                rows_by_cell[row_cell].append(row)

        feature_rows: list[list[float]] = []
        metadata: list[tuple[date, tuple[str, ...]]] = []
        for cell in cells:
            rows = sorted(
                rows_by_cell[cell],
                key=lambda row: (row.observed_date, getattr(row, "id", 0) or 0),
            )
            if not rows:
                raise InferenceUnavailableError("durable cell history is unavailable")

            first_date = rows[0].observed_date
            latest_date = rows[-1].observed_date
            elapsed = max(1, (as_of - first_date).days + 1)
            total = float(sum(row.event_count for row in rows))
            last_event = max(
                (row.observed_date for row in rows if row.event_count > 0),
                default=latest_date,
            )
            features = {
                "events_total": total,
                "events_7d": float(
                    sum(
                        row.event_count
                        for row in rows
                        if 0 <= (as_of - row.observed_date).days < 7
                    )
                ),
                "events_30d": float(
                    sum(
                        row.event_count
                        for row in rows
                        if 0 <= (as_of - row.observed_date).days < 30
                    )
                ),
                "frequency": total / elapsed,
                "recency": float((as_of - last_event).days),
                "horizon_days": float(horizon_days),
            }
            feature_rows.append([features[name] for name in feature_schema])
            metadata.append((latest_date, tuple(sorted({row.source for row in rows}))))

        try:
            booster = xgb.Booster()
            booster.load_model(bytearray(artifact.artifact_bytes))
            matrix = xgb.DMatrix(np.asarray(feature_rows, dtype=float))
            predicted_counts = booster.predict(matrix)
        except Exception as error:
            raise InferenceUnavailableError("artifact inference failed") from error

        results: list[CellPrediction] = []
        for cell, raw_prediction, (latest_date, sources) in zip(
            cells, predicted_counts, metadata, strict=True
        ):
            predicted_count = max(0.0, float(raw_prediction))
            if not math.isfinite(predicted_count):
                raise InferenceUnavailableError("artifact inference is not finite")

            freshness_days = (as_of - latest_date).days
            if freshness_days < 0:
                raise InferenceUnavailableError("observation freshness is invalid")
            if freshness_days > 365:
                raise InferenceUnavailableError("observations are stale")
            status = "READY" if freshness_days <= 7 else "DEGRADED"
            results.append(
                CellPrediction(
                    status=status,
                    h3_cell=cell,
                    horizon_days=horizon_days,
                    predicted_event_count=predicted_count,
                    risk_score=min(100.0, predicted_count / float(target_p95) * 100.0),
                    model_version=artifact.version,
                    sources=sources,
                    data_freshness=latest_date,
                )
            )
        return tuple(results)
