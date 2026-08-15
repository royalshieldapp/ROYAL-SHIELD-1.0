from fastapi import FastAPI
from fastapi.testclient import TestClient

from api.routes import feature_status


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
