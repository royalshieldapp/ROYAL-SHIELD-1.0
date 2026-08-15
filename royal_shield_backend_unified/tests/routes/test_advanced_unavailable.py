from fastapi import FastAPI
from fastapi.testclient import TestClient

from api.routes import hotspots, predictions, risk_maps, threats


def make_client() -> TestClient:
    app = FastAPI()
    app.include_router(predictions.router)
    app.include_router(threats.router)
    app.include_router(hotspots.router)
    app.include_router(risk_maps.router)
    return TestClient(app)


def test_prediction_without_verified_artifact_returns_503():
    response = make_client().post(
        "/api/v1/predict/risk",
        json={"locations": [{"lat": 25.76, "lng": -80.19}]},
    )

    assert response.status_code == 503
    detail = response.json()["detail"]
    assert detail["status"] == "UNAVAILABLE"
    assert detail["reason"] == "FORECAST_MODEL_NOT_VERIFIED"
    assert detail["confidence"] is None
    assert detail["data"] is None


def test_trend_forecast_without_verified_model_returns_503():
    response = make_client().get(
        "/api/v1/predict/trends",
        params={"h3_cell": "872830828ffffff", "days": 30},
    )

    assert response.status_code == 503
    assert response.json()["detail"]["reason"] == "FORECAST_MODEL_NOT_VERIFIED"
    assert "predicted_risk" not in response.text


class _EmptyQuery:
    def filter(self, *_args, **_kwargs):
        return self

    def order_by(self, *_args, **_kwargs):
        return self

    def limit(self, *_args, **_kwargs):
        return self

    def all(self):
        return []


class _EmptySession:
    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False

    def query(self, *_args, **_kwargs):
        return _EmptyQuery()


def test_empty_threat_feed_is_unavailable_not_mock_success(monkeypatch):
    monkeypatch.setattr(threats, "SessionLocal", lambda: _EmptySession())

    response = make_client().get("/api/v1/threats/")

    assert response.status_code == 503
    detail = response.json()["detail"]
    assert detail["status"] == "UNAVAILABLE"
    assert detail["reason"] == "THREAT_DATA_NOT_VERIFIED"
    assert detail["data"] is None
    assert "Mock threat" not in response.text


def assert_unavailable(response, reason: str):
    assert response.status_code == 503
    detail = response.json()["detail"]
    assert detail["status"] == "UNAVAILABLE"
    assert detail["reason"] == reason
    assert detail["data"] is None


def test_hotspots_without_verified_source_are_unavailable():
    assert_unavailable(
        make_client().get("/api/v1/hotspots"),
        "HOTSPOT_DATA_NOT_VERIFIED",
    )


def test_hotspot_prediction_without_verified_model_is_unavailable():
    response = make_client().get("/api/v1/hotspots/predict")

    assert_unavailable(response, "FORECAST_MODEL_NOT_VERIFIED")
    assert "0.72" not in response.text


def test_nearby_hotspots_without_verified_source_are_unavailable():
    assert_unavailable(
        make_client().get(
            "/api/v1/hotspots/nearby",
            params={"lat": 25.76, "lng": -80.19},
        ),
        "HOTSPOT_DATA_NOT_VERIFIED",
    )


def test_risk_map_without_verified_source_is_unavailable():
    assert_unavailable(
        make_client().get(
            "/api/v1/risk-map",
            params={
                "bbox_min_lat": 25.70,
                "bbox_min_lng": -80.30,
                "bbox_max_lat": 25.85,
                "bbox_max_lng": -80.10,
            },
        ),
        "RISK_MAP_DATA_NOT_VERIFIED",
    )


def test_risk_zone_without_verified_source_has_no_sample_score():
    response = make_client().get("/api/v1/risk-zones/872830828ffffff")

    assert_unavailable(response, "RISK_MAP_DATA_NOT_VERIFIED")
    assert "45.2" not in response.text


def test_risk_history_without_verified_source_has_no_sample_series():
    response = make_client().get(
        "/api/v1/risk-history",
        params={
            "h3_cell": "872830828ffffff",
            "start_date": "2026-01-01",
            "end_date": "2026-01-31",
        },
    )

    assert_unavailable(response, "RISK_MAP_DATA_NOT_VERIFIED")
    assert "42.1" not in response.text
