from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field
from typing import Optional, Dict, Any
import httpx
import logging
from datetime import datetime
from config.settings import settings

router = APIRouter(prefix="/api/openclaw", tags=["OpenClaw"])
logger = logging.getLogger(__name__)

class SessionPayload(BaseModel):
    type: str
    target: Optional[str] = None
    options: Optional[Dict[str, Any]] = Field(default_factory=dict)

@router.get("/status")
async def get_openclaw_status():
    base_url = settings.openclaw_base_url
    secret = settings.openclaw_secret

    if not base_url or not secret:
        return {
            "service": "openclaw",
            "status": "not_configured",
            "code": "OPENCLAW_NOT_CONFIGURED",
            "message": "OpenClaw service not configured. Set OPENCLAW_BASE_URL and OPENCLAW_SECRET."
        }

    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get(
                f"{base_url}/health",
                headers={"Authorization": f"Bearer {secret}"}
            )

        if response.status_code == 200:
            data = response.json()
            return {
                "service": "openclaw",
                "status": "available" if data.get("status") == "ok" else "degraded",
                "version": data.get("version", "unknown")
            }
        else:
            return {
                "service": "openclaw",
                "status": "degraded",
                "code": "OPENCLAW_DEGRADED",
                "message": f"Service returned status code {response.status_code}"
            }
    except Exception as e:
        logger.error(f"[OpenClaw] Health check failed: {e}")
        return {
            "service": "openclaw",
            "status": "unavailable",
            "code": "OPENCLAW_UNREACHABLE",
            "message": "OpenClaw service is not responding"
        }

@router.post("/session")
async def create_session(payload: SessionPayload):
    base_url = settings.openclaw_base_url
    secret = settings.openclaw_secret

    if not base_url or not secret:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "error": "OpenClaw service not configured",
                "code": "OPENCLAW_NOT_CONFIGURED",
                "message": "Set OPENCLAW_BASE_URL and OPENCLAW_SECRET in environment variables."
            }
        )

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(
                f"{base_url}/api/sessions",
                json={
                    "type": payload.type,
                    "target": payload.target,
                    "options": payload.options,
                    "clientId": "royal-shield-backend",
                    "timestamp": datetime.utcnow().isoformat() + "Z"
                },
                headers={
                    "Authorization": f"Bearer {secret}",
                    "Content-Type": "application/json"
                }
            )

        if response.status_code in [401, 403]:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail={
                    "error": "OpenClaw authentication failed",
                    "code": "OPENCLAW_AUTH_ERROR"
                }
            )
        elif response.status_code != 200 and response.status_code != 201:
            logger.error(f"[OpenClaw] API error: {response.text}")
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail={
                    "error": "Failed to create OpenClaw session",
                    "code": "OPENCLAW_ERROR"
                }
            )

        data = response.json()
        return {
            "success": True,
            "sessionId": data.get("sessionId") or data.get("id"),
            "status": data.get("status", "created"),
            "data": data
        }

    except httpx.HTTPError as exc:
        logger.error(f"[OpenClaw] Communication error: {exc}")
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail={"error": "Failed to communicate with OpenClaw", "code": "OPENCLAW_ERROR"}
        )

@router.get("/results/{sessionId}")
async def get_session_results(sessionId: str):
    base_url = settings.openclaw_base_url
    secret = settings.openclaw_secret

    if not base_url or not secret:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "error": "OpenClaw service not configured",
                "code": "OPENCLAW_NOT_CONFIGURED"
            }
        )

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{base_url}/api/sessions/{sessionId}",
                headers={"Authorization": f"Bearer {secret}"}
            )

        if response.status_code == 404:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"error": "Session not found", "sessionId": sessionId}
            )
        elif response.status_code != 200:
            logger.error(f"[OpenClaw] Results fetch error: {response.text}")
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="Failed to get OpenClaw results"
            )

        data = response.json()
        return {
            "success": True,
            "sessionId": sessionId,
            "status": data.get("status"),
            "results": data.get("results") or data
        }

    except httpx.HTTPError as exc:
        logger.error(f"[OpenClaw] Results error: {exc}")
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Failed to get OpenClaw results"
        )
