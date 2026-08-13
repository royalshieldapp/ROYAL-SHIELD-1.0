from __future__ import annotations

import hashlib
import json
import logging
import time
from datetime import datetime, timezone

import httpx
from fastapi import APIRouter, Depends, HTTPException, status
from jose import jwt
from pydantic import BaseModel, Field
from sqlalchemy import Boolean, Column, DateTime, Integer, String, UniqueConstraint
from sqlalchemy.orm import Session

from api.security.firebase_auth import require_firebase_uid
from config.settings import settings
from services.geospatial.database.connection import Base, get_db

router = APIRouter(prefix="/api/billing", tags=["Billing"])
logger = logging.getLogger(__name__)

PACKAGE_NAME = "com.royalshield.app"
VALID_PRODUCTS = {
    "lifetime_starter": {"tier": "STARTER", "level": 1},
    "lifetime_gold": {"tier": "GOLD", "level": 2},
    "lifetime_ultimate": {"tier": "ULTIMATE", "level": 3},
    "security_access_099": {"tier": "STARTER", "level": 1},
}


class BillingEntitlement(Base):
    __tablename__ = "billing_entitlements"
    __table_args__ = (UniqueConstraint("purchase_token_hash", name="uq_billing_purchase_token_hash"),)

    id = Column(Integer, primary_key=True)
    firebase_uid = Column(String(128), nullable=False, index=True)
    product_id = Column(String(128), nullable=False)
    tier = Column(String(32), nullable=False)
    level = Column(Integer, nullable=False)
    purchase_token_hash = Column(String(64), nullable=False)
    acknowledged = Column(Boolean, nullable=False, default=False)
    verified_at = Column(DateTime(timezone=True), nullable=False)
    purchase_time = Column(DateTime(timezone=True), nullable=True)


class VerifyPayload(BaseModel):
    purchaseToken: str = Field(min_length=8, max_length=4096)
    productId: str = Field(min_length=1, max_length=128)
    packageName: str = Field(default=PACKAGE_NAME, min_length=1, max_length=255)


def _detail(code: str, message: str) -> dict[str, str]:
    return {"status": "UNAVAILABLE", "code": code, "message": message}


async def get_google_access_token(service_account_json: str) -> str:
    try:
        service_account = json.loads(service_account_json)
    except (TypeError, json.JSONDecodeError):
        logger.error("Billing service-account configuration is not valid JSON")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=_detail("BILLING_AUTH_ERROR", "Google Play verification is unavailable"),
        ) from None

    client_email = service_account.get("client_email")
    private_key = service_account.get("private_key")
    if not client_email or not private_key:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=_detail("BILLING_AUTH_ERROR", "Google Play verification is unavailable"),
        )

    now = int(time.time())
    assertion = jwt.encode(
        {
            "iss": client_email,
            "scope": "https://www.googleapis.com/auth/androidpublisher",
            "aud": "https://oauth2.googleapis.com/token",
            "iat": now,
            "exp": now + 3600,
        },
        private_key,
        algorithm="RS256",
    )
    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.post(
            "https://oauth2.googleapis.com/token",
            data={"grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer", "assertion": assertion},
        )
    if response.status_code != 200:
        logger.error("Google Play OAuth request failed with status %s", response.status_code)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=_detail("BILLING_AUTH_ERROR", "Google Play verification is unavailable"),
        )
    access_token = response.json().get("access_token")
    if not access_token:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=_detail("BILLING_AUTH_ERROR", "Google Play verification is unavailable"),
        )
    return access_token


async def verify_google_play_purchase(
    service_account_json: str,
    package_name: str,
    product_id: str,
    purchase_token: str,
) -> dict:
    access_token = await get_google_access_token(service_account_json)
    url = (
        "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"
        f"{package_name}/purchases/products/{product_id}/tokens/{purchase_token}"
    )
    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.get(url, headers={"Authorization": f"Bearer {access_token}"})

    if response.status_code == 404:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=_detail("PURCHASE_NOT_FOUND", "Google Play did not recognize this purchase"),
        )
    if response.status_code in (401, 403):
        logger.error("Google Play purchase verification authorization failed")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=_detail("BILLING_AUTH_ERROR", "Google Play verification is unavailable"),
        )
    if response.status_code != 200:
        logger.error("Google Play purchase verification failed with status %s", response.status_code)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=_detail("BILLING_PROVIDER_ERROR", "Google Play verification is temporarily unavailable"),
        )
    return response.json()


@router.post("/verify")
async def verify_purchase(
    payload: VerifyPayload,
    firebase_uid: str = Depends(require_firebase_uid),
    db: Session = Depends(get_db),
):
    if payload.packageName != PACKAGE_NAME:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=_detail("PACKAGE_MISMATCH", "Purchase does not belong to Royal Shield"),
        )
    product = VALID_PRODUCTS.get(payload.productId)
    if product is None:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=_detail("PRODUCT_NOT_ALLOWED", "Product is not recognized"),
        )
    service_account_json = settings.google_play_service_account_json
    if not service_account_json:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=_detail("BILLING_NOT_CONFIGURED", "Google Play verification is not configured"),
        )

    token_hash = hashlib.sha256(payload.purchaseToken.encode("utf-8")).hexdigest()
    existing = db.query(BillingEntitlement).filter_by(purchase_token_hash=token_hash).one_or_none()
    if existing and existing.firebase_uid != firebase_uid:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=_detail("PURCHASE_TOKEN_ALREADY_CLAIMED", "Purchase is already linked to another account"),
        )

    purchase = await verify_google_play_purchase(
        service_account_json,
        payload.packageName,
        payload.productId,
        payload.purchaseToken,
    )
    expected_account_id = hashlib.sha256(firebase_uid.encode("utf-8")).hexdigest()
    if purchase.get("obfuscatedExternalAccountId") != expected_account_id:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=_detail("PURCHASE_ACCOUNT_MISMATCH", "Purchase is not linked to this account"),
        )
    if purchase.get("purchaseState") != 0:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=_detail("PURCHASE_NOT_ACTIVE", "Purchase is not completed and active"),
        )
    if purchase.get("consumptionState") == 1:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=_detail("PURCHASE_ALREADY_CONSUMED", "Purchase has already been consumed"),
        )

    purchase_time = None
    if purchase.get("purchaseTimeMillis"):
        purchase_time = datetime.fromtimestamp(int(purchase["purchaseTimeMillis"]) / 1000, tz=timezone.utc)
    verified_at = datetime.now(timezone.utc)
    entitlement = existing or BillingEntitlement(purchase_token_hash=token_hash)
    entitlement.firebase_uid = firebase_uid
    entitlement.product_id = payload.productId
    entitlement.tier = product["tier"]
    entitlement.level = product["level"]
    entitlement.acknowledged = purchase.get("acknowledgementState") == 1
    entitlement.verified_at = verified_at
    entitlement.purchase_time = purchase_time
    db.add(entitlement)
    db.commit()

    return {
        "status": "READY",
        "entitlement": {
            "tier": product["tier"],
            "level": product["level"],
            "productId": payload.productId,
            "verified": True,
            "acknowledged": entitlement.acknowledged,
        },
        "verifiedAt": verified_at.isoformat(),
    }


@router.get("/status")
def get_user_status(
    firebase_uid: str = Depends(require_firebase_uid),
):
    raise HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail=_detail(
            "BILLING_STATUS_REQUIRES_REVERIFICATION",
            "Restore purchases through Google Play to refresh entitlement",
        ),
    )


@router.get("/products")
def get_products():
    return {
        "status": "READY",
        "products": [
            {"id": product_id, "tier": data["tier"], "type": "one-time"}
            for product_id, data in VALID_PRODUCTS.items()
        ],
    }
