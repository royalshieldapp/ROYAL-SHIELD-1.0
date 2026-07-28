"""Verify Firebase ID tokens for authenticated Android API requests."""
from __future__ import annotations

import json
import logging
from functools import lru_cache

import firebase_admin
from fastapi import Header, HTTPException, status
from firebase_admin import auth, credentials

from config.settings import settings

logger = logging.getLogger(__name__)


@lru_cache(maxsize=1)
def _get_firebase_app():
    try:
        return firebase_admin.get_app()
    except ValueError:
        pass

    raw_credentials = settings.firebase_service_account_json
    if not raw_credentials:
        raise RuntimeError("Firebase Admin credentials are not configured")
    try:
        service_account = json.loads(raw_credentials)
    except json.JSONDecodeError as exc:
        raise RuntimeError("Firebase Admin credentials are invalid") from exc

    options = {"projectId": settings.firebase_project_id} if settings.firebase_project_id else None
    return firebase_admin.initialize_app(
        credentials.Certificate(service_account),
        options=options,
    )


async def require_firebase_uid(authorization: str | None = Header(default=None)) -> str:
    """Return the verified Firebase UID. Client-provided UIDs are never trusted."""
    scheme, _, token = (authorization or "").partition(" ")
    if scheme.lower() != "bearer" or not token.strip():
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Authentication required")

    try:
        _get_firebase_app()
        decoded = auth.verify_id_token(token.strip(), check_revoked=True)
    except RuntimeError:
        logger.error("Firebase Admin is not configured for authenticated API routes")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Authentication service is not configured",
        ) from None
    except Exception as exc:
        logger.info("Rejected Firebase ID token: %s", type(exc).__name__)
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid authentication token") from None

    uid = decoded.get("uid")
    if not isinstance(uid, str) or not uid:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid authentication token")
    return uid
