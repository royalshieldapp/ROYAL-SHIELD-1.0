import hashlib

from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from api.routes import billing
from api.security.firebase_auth import require_firebase_uid
from services.geospatial.database.connection import Base, get_db


VALID_PURCHASE = {
    "productId": "lifetime_gold",
    "purchaseToken": "play-token-123",
    "packageName": "com.royalshield.app",
}


def account_hash(uid="firebase-user-1"):
    return hashlib.sha256(uid.encode("utf-8")).hexdigest()


def make_client():
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    session_local = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    Base.metadata.create_all(bind=engine)

    app = FastAPI()
    app.include_router(billing.router)
    app.dependency_overrides[require_firebase_uid] = lambda: "firebase-user-1"

    def override_db():
        db = session_local()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override_db
    return TestClient(app), session_local


def test_unconfigured_play_verifier_does_not_grant_access(monkeypatch):
    client, _ = make_client()
    monkeypatch.setattr(billing.settings, "google_play_service_account_json", None)

    response = client.post(
        "/api/billing/verify",
        json=VALID_PURCHASE,
        headers={"Authorization": "Bearer test"},
    )

    assert response.status_code == 503
    assert response.json()["detail"]["code"] == "BILLING_NOT_CONFIGURED"


def test_verified_purchase_is_bound_to_authenticated_user_and_persisted(monkeypatch):
    client, session_local = make_client()
    monkeypatch.setattr(billing.settings, "google_play_service_account_json", "configured")

    async def verified_purchase(*_args, **_kwargs):
        return {
            "purchaseState": 0,
            "acknowledgementState": 0,
            "consumptionState": 0,
            "purchaseTimeMillis": "1786579200000",
            "obfuscatedExternalAccountId": account_hash(),
        }

    monkeypatch.setattr(billing, "verify_google_play_purchase", verified_purchase)

    response = client.post(
        "/api/billing/verify",
        json=VALID_PURCHASE,
        headers={"Authorization": "Bearer test"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "READY"
    assert body["entitlement"]["tier"] == "GOLD"
    assert body["entitlement"]["verified"] is True

    with session_local() as db:
        saved = db.query(billing.BillingEntitlement).one()
        assert saved.firebase_uid == "firebase-user-1"
        assert saved.product_id == "lifetime_gold"
        assert saved.purchase_token_hash != VALID_PURCHASE["purchaseToken"]


def test_purchase_token_cannot_be_reused_by_another_user(monkeypatch):
    client, _ = make_client()
    monkeypatch.setattr(billing.settings, "google_play_service_account_json", "configured")

    async def verified_purchase(*_args, **_kwargs):
        return {
            "purchaseState": 0,
            "acknowledgementState": 1,
            "consumptionState": 0,
            "obfuscatedExternalAccountId": account_hash(),
        }

    monkeypatch.setattr(billing, "verify_google_play_purchase", verified_purchase)

    first = client.post("/api/billing/verify", json=VALID_PURCHASE)
    assert first.status_code == 200

    client.app.dependency_overrides[require_firebase_uid] = lambda: "firebase-user-2"
    second = client.post("/api/billing/verify", json=VALID_PURCHASE)

    assert second.status_code == 409
    assert second.json()["detail"]["code"] == "PURCHASE_TOKEN_ALREADY_CLAIMED"


def test_cancelled_purchase_never_grants_entitlement(monkeypatch):
    client, _ = make_client()
    monkeypatch.setattr(billing.settings, "google_play_service_account_json", "configured")

    async def cancelled_purchase(*_args, **_kwargs):
        return {
            "purchaseState": 1,
            "acknowledgementState": 0,
            "consumptionState": 0,
            "obfuscatedExternalAccountId": account_hash(),
        }

    monkeypatch.setattr(billing, "verify_google_play_purchase", cancelled_purchase)

    response = client.post("/api/billing/verify", json=VALID_PURCHASE)

    assert response.status_code == 422
    assert response.json()["detail"]["code"] == "PURCHASE_NOT_ACTIVE"


def test_purchase_must_be_bound_to_authenticated_firebase_account(monkeypatch):
    client, _ = make_client()
    monkeypatch.setattr(billing.settings, "google_play_service_account_json", "configured")

    async def purchase_for_someone_else(*_args, **_kwargs):
        return {
            "purchaseState": 0,
            "acknowledgementState": 0,
            "consumptionState": 0,
            "obfuscatedExternalAccountId": account_hash("different-user"),
        }

    monkeypatch.setattr(billing, "verify_google_play_purchase", purchase_for_someone_else)

    response = client.post("/api/billing/verify", json=VALID_PURCHASE)

    assert response.status_code == 422
    assert response.json()["detail"]["code"] == "PURCHASE_ACCOUNT_MISMATCH"


def test_production_app_registers_billing_verification_route():
    from api.main import app

    assert "/api/billing/verify" in app.openapi()["paths"]
