import asyncio
from dataclasses import replace
from datetime import date

from fastapi import FastAPI
from fastapi.testclient import TestClient

from api.routes import feature_status
from services.forecast.artifact_store import LoadedArtifact
from services.forecast.quality import DataQualityReport


def make_client() -> TestClient:
    app = FastAPI()
    app.include_router(feature_status.router)
    return TestClient(app)


def test_unverified_advanced_features_fail_closed():
    response = make_client().get("/api/v1/features/advanced-status")

    assert response.status_code == 200
    body = response.json()
    assert set(body) == {
        "vpn",
        "forecast",
        "threatMap",
        "automations",
        "reports",
        "policies",
    }
    assert all(feature["status"] == "UNAVAILABLE" for feature in body.values())
    assert body["vpn"]["reason"] == "VPN_E2E_NOT_VERIFIED"
    assert body["forecast"]["reason"] == "FORECAST_MODEL_NOT_VERIFIED"
    assert body["threatMap"]["reason"] == "THREAT_DATA_NOT_VERIFIED"
    assert all(feature["data"] is None for feature in body.values())
    assert all(feature["confidence"] is None for feature in body.values())


def test_production_app_registers_advanced_status_route():
    from api.main import app

    assert "/api/v1/features/advanced-status" in app.openapi()["paths"]


class _QueryRows:
    def __init__(self, rows):
        self._rows = rows

    def all(self):
        return self._rows


class _VerifiedTrainingRun:
    def __init__(self, source_start: date, source_end: date):
        self.source_start = source_start
        self.source_end = source_end


class _VerifiedForecastDb:
    def __init__(
        self,
        source_start: date = date(2026, 10, 1),
        source_end: date = date(2026, 10, 15),
    ):
        self.training_run = _VerifiedTrainingRun(source_start, source_end)

    def execute(self, _query):
        return _QueryRows([
            ("Miami-Dade Sheriff Office Internal Affairs", "PCB_Report"),
        ])

    def scalar(self, _query):
        return self.training_run


class _UnapprovedForecastDb(_VerifiedForecastDb):
    def execute(self, _query):
        return _QueryRows([("unapproved-source", "unapproved-dataset")])


def _verified_artifact(
    *,
    source_start: date = date(2026, 10, 1),
    source_end: date = date(2026, 10, 15),
) -> LoadedArtifact:
    return LoadedArtifact(
        id=1,
        version="forecast-verified",
        model_format="xgboost-json",
        artifact_bytes=b"verified-artifact",
        sha256="a" * 64,
        metrics={
            "feature_schema": [
                "events_total",
                "events_7d",
                "events_30d",
                "frequency",
                "recency",
                "horizon_days",
            ],
            "h3_resolution": 9,
            "calibration": {"target_p95": 5.0},
        },
        source_start=source_start,
        source_end=source_end,
    )


def _current_quality() -> DataQualityReport:
    return DataQualityReport(
        sources=("Miami-Dade Sheriff Office Internal Affairs",),
        source_start=date(2026, 10, 1),
        source_end=date(2026, 10, 15),
        valid_rows=12,
        rejected_rows=0,
        deduplicated_rows=0,
        h3_cells=3,
        latest_observation=date(2026, 10, 15),
        freshness=0,
    )


def test_forecast_ready_requires_durable_current_verified_evidence(monkeypatch):
    """READY must be derived from durable provenance, not the server clock."""

    artifact = _verified_artifact()
    monkeypatch.setattr(
        feature_status.PostgresArtifactStore,
        "load_active",
        lambda _store: artifact,
    )
    monkeypatch.setattr(
        feature_status,
        "audit_observations",
        lambda _db: _current_quality(),
        raising=False,
    )

    result = asyncio.run(
        feature_status.get_advanced_feature_status(db=_VerifiedForecastDb())
    )

    assert result.forecast.status == "READY"
    assert result.forecast.sources == ["Miami-Dade Sheriff Office Internal Affairs"]
    assert result.forecast.dataFreshness is not None
    assert result.forecast.data == {
        "observationCount": 12,
        "h3CellCount": 3,
        "sourceStart": "2026-10-01",
        "sourceEnd": "2026-10-15",
        "artifactFormat": "xgboost-json",
    }
    assert result.forecast.modelVersion == "forecast-verified"
    assert result.threatMap.status == "UNAVAILABLE"


def test_forecast_status_refuses_unapproved_source_provenance(monkeypatch):
    monkeypatch.setattr(
        feature_status.PostgresArtifactStore,
        "load_active",
        lambda _store: _verified_artifact(),
    )
    monkeypatch.setattr(
        feature_status,
        "audit_observations",
        lambda _db: _current_quality(),
        raising=False,
    )

    result = asyncio.run(
        feature_status.get_advanced_feature_status(db=_UnapprovedForecastDb())
    )

    assert result.forecast.status == "UNAVAILABLE"
    assert result.forecast.reason == "FORECAST_MODEL_NOT_VERIFIED"
    assert result.forecast.sources == []
    assert result.forecast.data is None


def test_forecast_status_refuses_stale_durable_observations(monkeypatch):
    stale_start = date(2025, 10, 1)
    stale_end = date(2025, 10, 14)
    monkeypatch.setattr(
        feature_status.PostgresArtifactStore,
        "load_active",
        lambda _store: _verified_artifact(
            source_start=stale_start,
            source_end=stale_end,
        ),
    )
    monkeypatch.setattr(
        feature_status,
        "audit_observations",
        lambda _db: replace(
            _current_quality(),
            source_start=stale_start,
            source_end=stale_end,
            latest_observation=stale_end,
            freshness=366,
        ),
        raising=False,
    )

    result = asyncio.run(
        feature_status.get_advanced_feature_status(
            db=_VerifiedForecastDb(stale_start, stale_end)
        )
    )

    assert result.forecast.status == "UNAVAILABLE"
    assert result.forecast.reason == "FORECAST_MODEL_NOT_VERIFIED"
