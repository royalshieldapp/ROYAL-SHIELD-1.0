"""
System Status API Routes
Ported from Node.js backend - system health and service status
"""
from fastapi import APIRouter
from pydantic import BaseModel
from datetime import datetime
import os
from pathlib import Path
import logging

from sqlalchemy import func, text

from config.settings import settings
from services.data_ingestion.storage import CyberThreatRecord
from services.geospatial.database.connection import SessionLocal

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/system", tags=["System"])


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


@router.get("/status", response_model=SystemStatusResponse)
async def get_system_status():
    """
    Get overall system health and individual service statuses.

    Report configuration and durable-service state without exposing secrets.
    """
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
        version="2.0.0"
    )
