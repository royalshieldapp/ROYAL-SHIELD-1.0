"""
XR Voice Unlock API Routes
Handles secure device unlocking using voice recognition validation token
"""
import time
import logging
from collections import defaultdict
from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/xr", tags=["XR Voice Unlock"])

# In-memory store for rate limiting: IP -> list of timestamps
rate_limit_store = defaultdict(list)
RATE_LIMIT_WINDOW = 60  # seconds
RATE_LIMIT_MAX_REQUESTS = 10  # max requests per minute

class VoiceUnlockRequest(BaseModel):
    token: str = Field(..., description="The verification token for voice unlock (must match ROYAL-VOICE-UNLOCKED-2026)")

class VoiceUnlockResponse(BaseModel):
    success: bool
    message: str
    status: str

@router.post("/voice-unlock", response_model=VoiceUnlockResponse)
async def voice_unlock(payload: VoiceUnlockRequest, request: Request):
    """
    Unlock the XR/device interface using a validated voice-matching token.

    Verifies that the token matches 'ROYAL-VOICE-UNLOCKED-2026' and processes the unlock event.
    """
    client_ip = request.client.host if request.client else "unknown"
    logger.info(f"Voice unlock request received from IP: {client_ip}")

    # Rate limiting validation
    now = time.time()
    # Clean up older timestamps
    rate_limit_store[client_ip] = [ts for ts in rate_limit_store[client_ip] if now - ts < RATE_LIMIT_WINDOW]

    if len(rate_limit_store[client_ip]) >= RATE_LIMIT_MAX_REQUESTS:
        logger.warning(f"Rate limit exceeded for IP: {client_ip} trying to access voice-unlock")
        raise HTTPException(
            status_code=429,
            detail="Too many unlock attempts. Please try again in a minute."
        )

    # Track this attempt
    rate_limit_store[client_ip].append(now)

    # Token validation
    expected_token = "ROYAL-VOICE-UNLOCKED-2026"
    if payload.token != expected_token:
        logger.warning(f"Voice unlock failed: Invalid token from IP {client_ip}")
        raise HTTPException(
            status_code=401,
            detail="Invalid unlock token"
        )

    logger.info(f"Voice unlock successful for IP: {client_ip}")
    return VoiceUnlockResponse(
        success=True,
        message="Voice unlock successful. Device interface unlocked.",
        status="UNLOCKED"
    )
