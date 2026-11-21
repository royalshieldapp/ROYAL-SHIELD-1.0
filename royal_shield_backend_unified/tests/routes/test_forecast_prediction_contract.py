"""Contract coverage for real Forecast V2 prediction inference.

These tests deliberately use an isolated SQLite database.  The model is a
real XGBoost JSON booster published through ``PostgresArtifactStore`` so the
route exercises checksum validation and durable observation lookup rather
than a mocked inference result.
"""

from __future__ import annotations

from datetime import date, timedelta
from typing import Iterator

import h3
import numpy as np
import pytest
import xgboost as xgb
from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import StaticPool

from api.routes import hotspots, predictions, risk_maps
from services.forecast.artifact_store import PostgresArtifactStore
from services.forecast.models import ForecastObservation
from services.forecast.training import FEATURE_SCHEMA
from services.geospatial.database.connection import Base, get_db


MIAMI_DOWNTOWN = {"lat": 25.7617, "lng": -80.1918}
NORTH_MIAMI = {"lat": 25.8510, "lng": -80.1840}
H3_RESOLUTION = 9
MODEL_VERSION = "forecast-contract-xgboost-v1"


def _real_xgboost_artifact() -> bytes:
    """Train and serialize a minimal real booster for the API boundary test."""

    feature_rows = np.asarray(
        (
            (1.0, 1.0, 1.0, 1.0, 0.0, 7.0),
            (5.0, 4.0, 5.0, 1.5, 1.0, 7.0),
            (9.0, 7.0, 9.0, 2.0, 0.0, 7.0),
            (14.0, 10.0, 14.0, 2.5, 0.0, 7.0),
        ),
        dtype=float,
    )
    model = xgb.XGBRegressor(
        objective="reg:squarederror",
        n_estimators=4,
        max_depth=2,
        random_state=42,
        n_jobs=1,
    )
    model.fit(feature_rows, np.asarray((1.0, 3.0, 5.0, 8.0), dtype=float))
    return bytes(model.get_booster().save_raw(raw_format="json"))


def _publish_fixture_artifact(session: Session, *, today: date) -> None:
    PostgresArtifactStore(session).publish(
        version=MODEL_VERSION,
        artifact_bytes=_real_xgboost_artifact(),
        model_format="xgboost-json",
        metrics={
            "feature_schema": list(FEATURE_SCHEMA),
            "h3_resolution": H3_RESOLUTION,
            "calibration": {"target_p95": 12.0},
        },
        source_start=today - timedelta(days=14),
        source_end=today,
        activate=True,
    )


@pytest.fixture
def forecast_client() -> Iterator[TestClient]:
    """Serve the predictions router against an isolated durable store."""

    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    session_factory = sessionmaker(bind=engine, expire_on_commit=False)
    Base.metadata.create_all(engine)
    today = date.today()
    downtown_cell = h3.geo_to_h3(
        MIAMI_DOWNTOWN["lat"], MIAMI_DOWNTOWN["lng"], H3_RESOLUTION
    )

    with session_factory() as session:
        session.add_all(
            (
                ForecastObservation(
                    source="Miami-Dade Open Data",
                    source_dataset="PCB_Report",
                    observed_date=today - timedelta(days=7),
                    district="MIAMI-DADE",
                    police_grid="1361",
                    h3_cell=downtown_cell,
                    event_type="verified_pcb_record",
                    event_count=3,
                ),
                ForecastObservation(
                    source="Miami-Dade Open Data",
                    source_dataset="PCB_Report",
                    observed_date=today,
                    district="MIAMI-DADE",
                    police_grid="1361",
                    h3_cell=downtown_cell,
                    event_type="verified_pcb_record",
                    event_count=5,
                ),
            )
        )
        session.commit()
        _publish_fixture_artifact(session, today=today)

    def override_get_db() -> Iterator[Session]:
        session = session_factory()
        try:
            yield session
        finally:
            session.close()

    app = FastAPI()
    app.include_router(predictions.router)
    app.include_router(risk_maps.router)
    app.include_router(hotspots.router)
    app.dependency_overrides[get_db] = override_get_db
    try:
        with TestClient(app) as client:
            yield client
    finally:
        app.dependency_overrides.clear()
        Base.metadata.drop_all(engine)
        engine.dispose()


def test_prediction_with_verified_artifact_and_cell_history_returns_contract(
    forecast_client: TestClient,
) -> None:
    """Catch a route regression that bypasses the model or omits provenance."""

    response = forecast_client.post(
        "/api/v1/predict/risk",
        json={"locations": [MIAMI_DOWNTOWN]},
    )

    assert response.status_code == 200
    payload = response.json()
    prediction = payload["predictions"][0]
    assert payload["status"] == "READY"
    assert payload["model_version"] == MODEL_VERSION
    assert payload["data_freshness"] == date.today().isoformat()
    assert payload["sources"] == ["Miami-Dade Open Data"]
    assert payload["total_requested"] == 1
    assert payload["total_predicted"] == 1
    assert prediction["location"] == MIAMI_DOWNTOWN
    assert isinstance(prediction["predicted_event_count"], float)
    assert prediction["predicted_event_count"] >= 0.0
    assert isinstance(prediction["predicted_risk_score"], float)
    assert 0.0 <= prediction["predicted_risk_score"] <= 100.0


def test_prediction_without_requested_cell_history_returns_unavailable_without_score(
    forecast_client: TestClient,
) -> None:
    """Catch a route regression that manufactures a prediction for an empty cell."""

    assert h3.geo_to_h3(
        NORTH_MIAMI["lat"], NORTH_MIAMI["lng"], H3_RESOLUTION
    ) != h3.geo_to_h3(MIAMI_DOWNTOWN["lat"], MIAMI_DOWNTOWN["lng"], H3_RESOLUTION)

    response = forecast_client.post(
        "/api/v1/predict/risk",
        json={"locations": [NORTH_MIAMI]},
    )

    assert response.status_code == 503
    detail = response.json()["detail"]
    assert detail["status"] == "UNAVAILABLE"
    assert detail["reason"] == "FORECAST_MODEL_NOT_VERIFIED"
    assert detail["model_version"] is None
    assert detail["data_freshness"] is None
    assert detail["confidence"] is None
    assert detail["data"] is None
    assert "predicted_risk_score" not in detail


def test_risk_map_uses_verified_predictions_and_durable_observed_counts(
    forecast_client: TestClient,
) -> None:
    """Catch a map regression that replaces model output with fixed scores."""

    response = forecast_client.get(
        "/api/v1/risk-map",
        params={
            "bbox_min_lat": 25.70,
            "bbox_min_lng": -80.30,
            "bbox_max_lat": 25.80,
            "bbox_max_lng": -80.10,
            "resolution": H3_RESOLUTION,
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["type"] == "FeatureCollection"
    assert payload["metadata"]["status"] == "READY"
    assert payload["metadata"]["model_version"] == MODEL_VERSION
    assert payload["metadata"]["data_freshness"] == date.today().isoformat()
    assert payload["metadata"]["sources"] == ["Miami-Dade Open Data"]
    assert payload["metadata"]["zone_count"] == 1
    feature = payload["features"][0]
    assert feature["properties"]["event_count"] == 8
    assert 0.0 <= feature["properties"]["risk_score"] <= 100.0
    assert feature["geometry"]["type"] == "Polygon"
    assert feature["geometry"]["coordinates"][0][0] == feature["geometry"]["coordinates"][0][-1]


def test_zone_details_separate_observed_statistics_from_predicted_risk(
    forecast_client: TestClient,
) -> None:
    """Catch regressions that present aggregate observations as synthetic events."""

    cell = h3.geo_to_h3(MIAMI_DOWNTOWN["lat"], MIAMI_DOWNTOWN["lng"], H3_RESOLUTION)
    response = forecast_client.get(f"/api/v1/risk-zones/{cell}")

    assert response.status_code == 200
    payload = response.json()
    assert payload["h3_cell"] == cell
    assert payload["statistics"]["total_events"] == 8
    assert payload["statistics"]["recent_7d"] == 5
    assert payload["statistics"]["recent_30d"] == 8
    assert payload["recent_events"] == []
    assert payload["metadata"]["observation_kind"] == "OBSERVED_DATA"
    assert payload["metadata"]["risk_kind"] == "PREDICTED_RISK"
    assert payload["metadata"]["model_version"] == MODEL_VERSION


def test_hotspots_are_derived_from_durable_h3_observations_and_model_output(
    forecast_client: TestClient,
) -> None:
    """Catch fixed or random hotspot fallbacks when verified evidence exists."""

    response = forecast_client.get(
        "/api/v1/hotspots",
        params={
            "bbox_min_lat": 25.70,
            "bbox_min_lng": -80.30,
            "bbox_max_lat": 25.80,
            "bbox_max_lng": -80.10,
            "time_window_days": 30,
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["total_count"] == 1
    assert payload["metadata"]["status"] == "READY"
    assert payload["metadata"]["model_version"] == MODEL_VERSION
    assert payload["metadata"]["method"] == "durable_h3_observations_plus_forecast_model"
    hotspot = payload["hotspots"][0]
    assert hotspot["event_count"] == 8
    assert hotspot["event_types"] == {"verified_pcb_record": 8}
    assert hotspot["severities"] == {}
    assert 0.0 <= hotspot["risk_score"] <= 100.0
