from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field
from typing import List, Optional
import httpx
import logging
import json
import time
from jose import jwt
from config.settings import settings

router = APIRouter(prefix="/api/billing", tags=["Billing"])
logger = logging.getLogger(__name__)

# Valid product IDs
VALID_PRODUCTS = {
    'lifetime_starter': {'tier': 'STARTER', 'level': 1},
    'lifetime_gold': {'tier': 'GOLD', 'level': 2},
    'lifetime_ultimate': {'tier': 'ULTIMATE', 'level': 3},
    'security_access_099': {'tier': 'STARTER', 'level': 1}
}

class VerifyPayload(BaseModel):
    purchaseToken: str
    productId: str
    packageName: str = "com.royalshield.app"

async def get_google_access_token(service_account_json: str) -> str:
    try:
        service_account = json.loads(service_account_json)
    except Exception as e:
        logger.error(f"[Billing] Failed to parse service account JSON: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Invalid service account configuration format."
        )

    client_email = service_account.get("client_email")
    private_key = service_account.get("private_key")

    if not client_email or not private_key:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Service account JSON missing email or private key."
        )

    now = int(time.time())
    payload = {
        "iss": client_email,
        "scope": "https://www.googleapis.com/auth/androidpublisher",
        "aud": "https://oauth2.googleapis.com/token",
        "iat": now,
        "exp": now + 3600
    }

    try:
        # Sign JWT using RS256 algorithm via python-jose
        token = jwt.encode(payload, private_key, algorithm="RS256")
    except Exception as e:
        logger.error(f"[Billing] JWT signing failed: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"JWT signing failed: {str(e)}"
        )

    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.post(
            "https://oauth2.googleapis.com/token",
            data={
                "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
                "assertion": token
            }
        )

    if response.status_code != 200:
        logger.error(f"[Billing] OAuth token request failed: {response.text}")
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Failed to authenticate with Google Play API."
        )

    return response.json().get("access_token")

@router.post("/verify")
async def verify_purchase(payload: VerifyPayload):
    if payload.productId not in VALID_PRODUCTS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid product ID"
        )

    service_account_json = settings.google_play_service_account_json

    if not service_account_json:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "error": "Billing verification not configured",
                "code": "BILLING_NOT_CONFIGURED",
                "message": "Google Play service account not set. Configure GOOGLE_PLAY_SERVICE_ACCOUNT_JSON."
            }
        )

    try:
        access_token = await get_google_access_token(service_account_json)

        verify_url = (
            f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"
            f"{payload.packageName}/purchases/products/{payload.productId}/tokens/{payload.purchaseToken}"
        )

        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                verify_url,
                headers={"Authorization": f"Bearer {access_token}"}
            )

        if response.status_code == 404:
            return {
                "success": True,
                "valid": False,
                "entitlement": {"tier": "FREE", "level": 0},
                "reason": "Purchase token not found"
            }
        elif response.status_code in [401, 403]:
            logger.error(f"[Billing] Google Play Auth error: {response.text}")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail={
                    "error": "Billing service authentication error",
                    "code": "BILLING_AUTH_ERROR"
                }
            )
        elif response.status_code != 200:
            logger.error(f"[Billing] Google API error: {response.text}")
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="Failed to verify purchase with Google Play."
            )

        purchase = response.json()
        is_valid = purchase.get("purchaseState") == 0  # 0 = purchased
        is_consumed = purchase.get("consumptionState") == 1
        is_acknowledged = purchase.get("acknowledgementState") == 1

        product = VALID_PRODUCTS[payload.productId]

        purchase_time_millis = purchase.get("purchaseTimeMillis")
        purchase_time = (
            time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime(int(purchase_time_millis) / 1000.0))
            if purchase_time_millis else None
        )

        return {
            "success": True,
            "valid": is_valid,
            "entitlement": {
                "tier": product["tier"],
                "level": product["level"],
                "productId": payload.productId,
                "purchaseState": "PURCHASED" if is_valid else "INVALID",
                "acknowledged": is_acknowledged,
                "consumed": is_consumed
            },
            "purchaseTime": purchase_time
        }

    except Exception as e:
        if isinstance(e, HTTPException):
            raise e
        logger.error(f"[Billing] Verification exception: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to verify purchase"
        )

@router.get("/status/{userId}")
def get_user_status(userId: str):
    # TODO: Query database for user's entitlement
    # For now, return free tier as default
    return {
        "success": True,
        "userId": userId,
        "entitlement": {
            "tier": "FREE",
            "level": 0,
            "features": {
                "urlScan": True,
                "fileScan": False,
                "aiAssistant": False,
                "vpn": False,
                "advancedSecurity": False,
                "prioritySupport": False
            }
        },
        "message": "Database not connected — returning default free tier"
    }

@router.get("/products")
def get_products():
    return {
        "success": True,
        "products": [
            {
                "id": "lifetime_starter",
                "name": "Starter Shield",
                "tier": "STARTER",
                "type": "lifetime",
                "features": ["URL Scanning", "Basic AI Assistant", "SOS Alerts"]
            },
            {
                "id": "lifetime_gold",
                "name": "Gold Shield",
                "tier": "GOLD",
                "type": "lifetime",
                "features": ["All Starter features", "File Scanning", "VPN Access", "Advanced AI", "Sound Detection"]
            },
            {
                "id": "lifetime_ultimate",
                "name": "Ultimate Shield",
                "tier": "ULTIMATE",
                "type": "lifetime",
                "features": ["All Gold features", "Security Camera", "XDR Dashboard", "Priority Support", "OpenClaw Access"]
            },
            {
                "id": "security_access_099",
                "name": "Security Access",
                "tier": "STARTER",
                "type": "one-time",
                "features": ["Basic security features access"]
            }
        ]
    }
