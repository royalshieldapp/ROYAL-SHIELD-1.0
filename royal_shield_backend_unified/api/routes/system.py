"""System health, service status, and controlled network diagnostics."""
from collections import defaultdict, deque
from datetime import datetime
import logging
import os
from pathlib import Path
import time

from fastapi import APIRouter, HTTPException, Query, Request, Response, status
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from sqlalchemy import func, text

from config.settings import settings
from services.data_ingestion.storage import CyberThreatRecord
from services.geospatial.database.connection import SessionLocal

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/system", tags=["System"])

MAX_SPEED_TEST_BYTES = 5 * 1024 * 1024
MIN_DOWNLOAD_BYTES = 64 * 1024
SPEED_TEST_WINDOW_SECONDS = 60
SPEED_TEST_REQUESTS_PER_WINDOW = 12
_speed_test_requests: dict[str, deque[float]] = defaultdict(deque)


class ServiceStatus(BaseModel):
    database: str
    threatFeed: str
    smsGateway: str
    paymentSystem: str
    mlEngine: str
    riskPrediction: str


class SystemStatusResponse(BaseModel):
    status: str
    services: ServiceStatus
    lastUpdate: str
    version: str


def _enforce_speed_test_limit(request: Request) -> None:
    """Apply a small per-instance IP limit without logging client identifiers."""
    client_key = request.client.host if request.client else "unknown"
    now = time.monotonic()
    requests = _speed_test_requests[client_key]
    while requests and now - requests[0] >= SPEED_TEST_WINDOW_SECONDS:
        requests.popleft()
    if len(requests) >= SPEED_TEST_REQUESTS_PER_WINDOW:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Speed test rate limit reached. Try again in one minute.",
            headers={"Retry-After": str(SPEED_TEST_WINDOW_SECONDS)},
        )
    requests.append(now)


@router.get("/status", response_model=SystemStatusResponse)
async def get_system_status():
    """Return configuration and durable-service state without exposing secrets."""
    database_status = "UNAVAILABLE"
    threat_status = "EMPTY"
    try:
        with SessionLocal() as db:
            db.execute(text("SELECT 1"))
            database_status = "CONNECTED"
            threat_count = db.query(func.count(CyberThreatRecord.id)).scalar() or 0
            threat_status = "LIVE" if threat_count > 0 else "EMPTY"
    except Exception:
        logger.exception("System database health check failed")

    sms_ready = bool(os.getenv("TWILIO_ACCOUNT_SID") and os.getenv("TWILIO_AUTH_TOKEN"))
    model_ready = Path("models/risk_predictor_latest.pkl").exists()
    overall = "OPERATIONAL" if database_status == "CONNECTED" else "DEGRADED"
    return SystemStatusResponse(
        status=overall,
        services=ServiceStatus(
            database=database_status,
            threatFeed=threat_status,
            smsGateway="READY" if sms_ready else "NOT_CONFIGURED",
            paymentSystem="NOT_CONFIGURED",
            mlEngine="ACTIVE" if model_ready else "MODEL_NOT_TRAINED",
            riskPrediction="ENABLED" if settings.enable_ml_predictions else "DISABLED",
        ),
        lastUpdate=datetime.utcnow().isoformat(),
        version="2.0.0",
    )


@router.get("/speed-test/ping", status_code=status.HTTP_204_NO_CONTENT)
async def speed_test_ping(request: Request) -> Response:
    """Return an uncached empty response for client-to-backend latency measurement."""
    _enforce_speed_test_limit(request)
    return Response(status_code=status.HTTP_204_NO_CONTENT, headers={"Cache-Control": "no-store"})


@router.get("/speed-test/download")
async def speed_test_download(
    request: Request,
    bytes_requested: int = Query(2 * 1024 * 1024, alias="bytes", ge=MIN_DOWNLOAD_BYTES, le=MAX_SPEED_TEST_BYTES),
) -> Response:
    """Return bounded incompressible bytes so transport compression cannot fake throughput."""
    _enforce_speed_test_limit(request)
    payload = os.urandom(bytes_requested)
    return Response(
        content=payload,
        media_type="application/octet-stream",
        headers={"Cache-Control": "no-store", "Content-Length": str(bytes_requested)},
    )


@router.post("/speed-test/upload")
async def speed_test_upload(request: Request) -> JSONResponse:
    """Accept one bounded binary payload and report only its received size."""
    _enforce_speed_test_limit(request)
    content_type = request.headers.get("content-type", "").split(";", 1)[0].strip().lower()
    if content_type != "application/octet-stream":
        raise HTTPException(status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE, detail="Binary payload required")
    declared_length = request.headers.get("content-length")
    if declared_length:
        try:
            if int(declared_length) > MAX_SPEED_TEST_BYTES:
                raise HTTPException(status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE, detail="Payload too large")
        except ValueError as exc:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid Content-Length") from exc
    payload = await request.body()
    if not payload:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Binary payload required")
    if len(payload) > MAX_SPEED_TEST_BYTES:
        raise HTTPException(status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE, detail="Payload too large")
    return JSONResponse(
        content={"bytesReceived": len(payload), "receivedAt": datetime.utcnow().isoformat()},
        headers={"Cache-Control": "no-store"},
    )
