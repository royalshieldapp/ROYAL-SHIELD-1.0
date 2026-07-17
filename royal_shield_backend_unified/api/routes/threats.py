"""
Cyber Threat Intelligence API Routes
Ported from Node.js backend - threat data by geographic location
"""
from datetime import datetime
import hmac

from fastapi import APIRouter, Depends, Header, HTTPException, Query
from pydantic import BaseModel
from typing import List, Optional
import logging

from config.settings import settings
from services.data_ingestion.cyber_collectors import refresh_cyber_intelligence
from services.data_ingestion.storage import CyberThreatRecord
from services.geospatial.database.connection import SessionLocal

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/threats", tags=["Threat Intelligence"])


class ThreatData(BaseModel):
    """Individual threat record"""
    id: str
    type: str
    lat: Optional[float] = None
    lng: Optional[float] = None
    severity: str
    description: str
    provider: str
    confidence: int
    indicatorType: str
    lastSeen: str


class ThreatsResponse(BaseModel):
    threats: List[ThreatData]
    total: int


def verify_internal_secret(x_internal_secret: Optional[str] = Header(None)):
    expected = settings.api_internal_secret
    if not expected or not x_internal_secret or not hmac.compare_digest(x_internal_secret, expected):
        raise HTTPException(status_code=401, detail="Unauthorized")


@router.get("/", response_model=ThreatsResponse)
async def get_threats(
    lat: Optional[float] = Query(None, ge=-90, le=90, description="Center latitude"),
    lng: Optional[float] = Query(None, ge=-180, le=180, description="Center longitude"),
    limit: int = Query(100, ge=1, le=500),
):
    """
    Return durable cyber intelligence from configured providers.

    Indicators remain global unless a trusted source supplied coordinates.
    """
    with SessionLocal() as db:
        records = (
            db.query(CyberThreatRecord)
            .filter(CyberThreatRecord.active.is_(True))
            .order_by(CyberThreatRecord.last_seen.desc())
            .limit(limit)
            .all()
        )

    # Mock threat data — in production, query from
    # AlienVault OTX, AbuseIPDB, or internal threat DB
    threats = [ThreatData(
        id=f"{record.provider}:{record.external_id}",
        type=record.title,
        lat=record.latitude,
        lng=record.longitude,
        severity=record.severity.upper(),
        description=record.description or record.indicator_value,
        provider=record.provider,
        confidence=record.confidence,
        indicatorType=record.indicator_type,
        lastSeen=record.last_seen.isoformat(),
    ) for record in records]
    return ThreatsResponse(threats=threats, total=len(threats))


@router.post("/refresh", dependencies=[Depends(verify_internal_secret)])
async def refresh_threats():
    if not settings.enable_cyber_collection:
        raise HTTPException(status_code=503, detail="Cyber collection is disabled")
    return {
        "providers": await refresh_cyber_intelligence(),
        "refreshedAt": datetime.utcnow().isoformat(),
    }
