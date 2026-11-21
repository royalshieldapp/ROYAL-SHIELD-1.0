from dataclasses import dataclass
from datetime import date, timedelta

import h3
import numpy as np
import pytest
import xgboost as xgb

from services.forecast.artifact_store import LoadedArtifact
from services.forecast.inference import ForecastInferenceService, InferenceUnavailableError
from services.forecast.importers.miami_dade_h3 import ArcGisPcbClient
from services.forecast.quality import DataQualityReport
from services.forecast.training import FEATURE_SCHEMA, MetricSet, decide_promotion


def _artifact_bytes() -> bytes:
    features = np.asarray(
        (
            (1.0, 1.0, 1.0, 1.0, 0.0, 7.0),
            (5.0, 5.0, 5.0, 2.0, 0.0, 7.0),
            (10.0, 8.0, 10.0, 3.0, 0.0, 7.0),
        )
    )
    model = xgb.XGBRegressor(
        objective="reg:squarederror",
        n_estimators=3,
        max_depth=2,
        random_state=42,
        n_jobs=1,
    )
    model.fit(features, np.asarray((1.0, 3.0, 6.0)))
    return bytes(model.get_booster().save_raw(raw_format="json"))


def test_h3_assigns_distinct_cells_to_distinct_miami_locations() -> None:
    downtown = h3.geo_to_h3(25.7617, -80.1918, 9)
    north_miami = h3.geo_to_h3(25.8510, -80.1840, 9)

    assert downtown != north_miami


def test_arcgis_client_verifies_tls_by_default(monkeypatch) -> None:
    captured: dict[str, object] = {}

    class CapturingClient:
        def __init__(self, **kwargs) -> None:
            captured.update(kwargs)

        def close(self) -> None:
            pass

    monkeypatch.setattr(
        "services.forecast.importers.miami_dade_h3.httpx.Client",
        CapturingClient,
    )

    client = ArcGisPcbClient()
    try:
        assert captured["verify"] is True
    finally:
        client.close()


def test_inference_without_durable_cell_history_is_unavailable(monkeypatch) -> None:
    artifact = LoadedArtifact(
        id=1,
        version="forecast-test",
        model_format="xgboost-json",
        artifact_bytes=_artifact_bytes(),
        sha256="not-used-by-this-test",
        metrics={
            "feature_schema": list(FEATURE_SCHEMA),
            "h3_resolution": 9,
            "calibration": {"target_p95": 20.0},
        },
        source_start=date(2026, 1, 1),
        source_end=date(2026, 1, 7),
    )

    class EmptyHistorySession:
        def scalars(self, _statement):
            return ()

    monkeypatch.setattr(
        "services.forecast.inference.PostgresArtifactStore.load_active",
        lambda _store: artifact,
    )

    with pytest.raises(InferenceUnavailableError, match="durable cell history"):
        ForecastInferenceService(EmptyHistorySession()).predict_cell(
            "8928308280fffff",
            horizon_days=7,
            as_of=date(2026, 1, 7),
        )


def test_inference_with_stale_durable_history_is_unavailable(monkeypatch) -> None:
    artifact = LoadedArtifact(
        id=1,
        version="forecast-test",
        model_format="xgboost-json",
        artifact_bytes=_artifact_bytes(),
        sha256="not-used-by-this-test",
        metrics={
            "feature_schema": list(FEATURE_SCHEMA),
            "h3_resolution": 9,
            "calibration": {"target_p95": 20.0},
        },
        source_start=date(2024, 1, 1),
        source_end=date(2024, 1, 1),
    )

    @dataclass
    class Observation:
        observed_date: date
        event_count: int
        source: str

    class StaleHistorySession:
        def scalars(self, _statement):
            return (Observation(date(2024, 1, 1), 4, "miami_dade"),)

    monkeypatch.setattr(
        "services.forecast.inference.PostgresArtifactStore.load_active",
        lambda _store: artifact,
    )

    with pytest.raises(InferenceUnavailableError, match="observations are stale"):
        ForecastInferenceService(StaleHistorySession()).predict_cell(
            "8928308280fffff",
            horizon_days=7,
            as_of=date(2026, 1, 7),
        )


def test_inference_uses_the_artifact_selected_by_the_route(monkeypatch) -> None:
    artifact = LoadedArtifact(
        id=7,
        version="forecast-route-selected",
        model_format="xgboost-json",
        artifact_bytes=_artifact_bytes(),
        sha256="not-used-by-this-test",
        metrics={
            "feature_schema": list(FEATURE_SCHEMA),
            "h3_resolution": 9,
            "calibration": {"target_p95": 20.0},
        },
        source_start=date(2026, 1, 1),
        source_end=date(2026, 1, 7),
    )

    @dataclass
    class Observation:
        observed_date: date
        event_count: int
        source: str

    class CurrentHistorySession:
        def scalars(self, _statement):
            return (Observation(date(2026, 1, 7), 4, "miami_dade"),)

    def unexpected_load(_store):
        raise AssertionError("predict_cell must use the route-selected artifact")

    monkeypatch.setattr(
        "services.forecast.inference.PostgresArtifactStore.load_active",
        unexpected_load,
    )

    prediction = ForecastInferenceService(CurrentHistorySession()).predict_cell(
        "8928308280fffff",
        horizon_days=7,
        as_of=date(2026, 1, 7),
        artifact=artifact,
    )

    assert prediction.model_version == artifact.version


def test_stale_data_cannot_promote_a_model() -> None:
    quality = DataQualityReport(
        sources=("Miami-Dade Sheriff Office Internal Affairs",),
        source_start=date.today() - timedelta(days=500),
        source_end=date.today() - timedelta(days=366),
        valid_rows=40,
        rejected_rows=0,
        deduplicated_rows=0,
        h3_cells=2,
        latest_observation=date.today() - timedelta(days=366),
        freshness=366,
    )
    baseline = MetricSet(mae=10.0, rmse=10.0, r2=0.0, sample_count=8)
    candidate = MetricSet(mae=5.0, rmse=5.0, r2=0.5, sample_count=8)

    promoted, reason = decide_promotion(quality, baseline, candidate, b"model")

    assert promoted is False
    assert reason == "Forecast observations are stale."
