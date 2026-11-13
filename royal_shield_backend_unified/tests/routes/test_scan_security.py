from fastapi import FastAPI
from fastapi.testclient import TestClient

from api.routes import scan
from api.security.firebase_auth import require_firebase_uid


def make_app() -> FastAPI:
    app = FastAPI()
    app.include_router(scan.router)
    return app


def test_url_scan_requires_firebase_authentication(monkeypatch):
    monkeypatch.delenv("VIRUSTOTAL_API_KEY", raising=False)

    response = TestClient(make_app()).post(
        "/api/v1/scan/url",
        json={"url": "https://example.com"},
    )

    assert response.status_code == 401
    assert response.json()["detail"] == "Authentication required"


def test_authenticated_url_scan_reports_missing_provider_as_unavailable(monkeypatch):
    monkeypatch.delenv("VIRUSTOTAL_API_KEY", raising=False)
    app = make_app()
    app.dependency_overrides[require_firebase_uid] = lambda: "firebase-uid"

    response = TestClient(app).post(
        "/api/v1/scan/url",
        json={"url": "https://example.com"},
    )

    assert response.status_code == 503
    assert response.json()["detail"] == "Scanner service configuration unavailable"


def test_url_scan_rejects_non_http_input_before_provider_call(monkeypatch):
    monkeypatch.setenv("VIRUSTOTAL_API_KEY", "test-only")
    app = make_app()
    app.dependency_overrides[require_firebase_uid] = lambda: "firebase-uid"

    response = TestClient(app).post(
        "/api/v1/scan/url",
        json={"url": "not-a-url"},
    )

    assert response.status_code == 422
