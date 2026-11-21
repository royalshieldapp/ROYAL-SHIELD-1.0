from fastapi import FastAPI
from fastapi.testclient import TestClient

from api.routes import xr
from api.security.firebase_auth import require_firebase_uid


def make_app(authenticated: bool = False) -> FastAPI:
    app = FastAPI()
    app.include_router(xr.router)
    if authenticated:
        app.dependency_overrides[require_firebase_uid] = lambda: "firebase-uid"
    return app


def test_voice_unlock_requires_firebase_authentication():
    response = TestClient(make_app()).post(
        "/api/xr/voice-unlock",
        json={"token": "compromised-legacy-value"},
    )

    assert response.status_code == 401
    assert response.json()["detail"] == "Authentication required"


def test_authenticated_legacy_voice_unlock_is_disabled():
    response = TestClient(make_app(authenticated=True)).post(
        "/api/xr/voice-unlock",
        json={"token": "compromised-legacy-value"},
    )

    assert response.status_code == 410
    assert response.json()["detail"] == "Legacy voice unlock is disabled"
