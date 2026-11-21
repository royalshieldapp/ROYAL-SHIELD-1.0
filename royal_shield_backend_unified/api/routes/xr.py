"""XR routes retained only to reject the compromised legacy unlock flow."""

import logging

from fastapi import APIRouter, Depends, HTTPException, status

from api.security.firebase_auth import require_firebase_uid

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/xr", tags=["XR Voice Unlock"])

@router.post("/voice-unlock")
async def voice_unlock(
    firebase_uid: str = Depends(require_firebase_uid),
):
    """Fail closed until a one-time, device-bound challenge replaces legacy QR tokens."""
    logger.warning(
        "Rejected deprecated voice unlock request for authenticated user suffix=%s",
        firebase_uid[-6:],
    )
    raise HTTPException(
        status_code=status.HTTP_410_GONE,
        detail="Legacy voice unlock is disabled",
    )
