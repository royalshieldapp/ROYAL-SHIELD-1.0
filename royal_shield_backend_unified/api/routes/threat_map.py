"""
Threat Map API Routes
Handles live threat signals for the Live Satellite Cyber Attack Map widget
"""
from fastapi import APIRouter, HTTPException, Depends, Header, Query
from pydantic import BaseModel, Field, validator
from typing import List, Optional
from datetime import datetime, timezone
from sqlalchemy import Column, DateTime, Float, Integer, String, desc
from sqlalchemy.orm import Session
import logging

from services.geospatial.database.connection import Base, get_db

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/threat-map", tags=["Threat Map"])

MAX_EVENTS = 100


class ThreatMapEventRecord(Base):
    """Durable security signal produced by an authenticated Royal Shield module."""

    __tablename__ = "threat_map_events"

    id = Column(Integer, primary_key=True, autoincrement=True)
    source_city = Column(String(120), nullable=False)
    source_lat = Column(Float, nullable=False)
    source_lng = Column(Float, nullable=False)
    target_city = Column(String(120), nullable=False)
    target_lat = Column(Float, nullable=False)
    target_lng = Column(Float, nullable=False)
    severity = Column(String(16), nullable=False, index=True)
    threat_type = Column(String(120), nullable=False)
    source_module = Column(String(80), nullable=False, index=True)
    observed_at = Column(DateTime(timezone=True), nullable=False, index=True)

class ThreatMapEvent(BaseModel):
    sourceCity: str = Field(..., description="City of origin")
    sourceLat: float = Field(..., description="Latitude of origin")
    sourceLng: float = Field(..., description="Longitude of origin")
    targetCity: str = Field(..., description="Target city")
    targetLat: float = Field(..., description="Target latitude")
    targetLng: float = Field(..., description="Target longitude")
    severity: str = Field(..., description="Severity level: low, medium, high, critical")
    threatType: str = Field(..., description="Type of threat, e.g., Suspicious URL, Malware")
    sourceModule: str = Field(..., description="Module that generated the event, e.g., qr_scanner, virustotal")
    observedAt: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

    @validator('severity')
    def validate_severity(cls, v):
        allowed = {'low', 'medium', 'high', 'critical'}
        if v.lower() not in allowed:
            raise ValueError(f"Severity must be one of {allowed}")
        return v.lower()

    @validator('sourceLat', 'targetLat')
    def validate_lat(cls, v):
        if not (-90 <= v <= 90):
            raise ValueError("Latitude must be between -90 and 90")
        return v

    @validator('sourceLng', 'targetLng')
    def validate_lng(cls, v):
        if not (-180 <= v <= 180):
            raise ValueError("Longitude must be between -180 and 180")
        return v


def _to_response(record: ThreatMapEventRecord) -> ThreatMapEvent:
    observed_at = record.observed_at
    if observed_at.tzinfo is None:
        observed_at = observed_at.replace(tzinfo=timezone.utc)
    return ThreatMapEvent(
        sourceCity=record.source_city,
        sourceLat=record.source_lat,
        sourceLng=record.source_lng,
        targetCity=record.target_city,
        targetLat=record.target_lat,
        targetLng=record.target_lng,
        severity=record.severity,
        threatType=record.threat_type,
        sourceModule=record.source_module,
        observedAt=observed_at.isoformat(),
    )

def verify_internal_secret(x_internal_secret: str = Header(None)):
    """Middleware/Dependency to verify internal requests"""
    from config.settings import settings
    expected_secret = settings.api_internal_secret
    if not expected_secret:
        # If not set in environment, fail securely
        logger.warning("API_INTERNAL_SECRET not configured on the server")
        raise HTTPException(status_code=500, detail="Server misconfiguration")

    if not x_internal_secret or x_internal_secret != expected_secret:
        logger.warning("Unauthorized access attempt to internal threat map endpoint")
        raise HTTPException(status_code=401, detail="Unauthorized")

@router.get("/events", response_model=List[ThreatMapEvent])
async def get_threat_map_events(
    limit: int = Query(default=100, ge=1, le=MAX_EVENTS),
    db: Session = Depends(get_db),
):
    """
    Public endpoint to get recent threat events for the Live Threat Intelligence Signals widget.
    """
    records = (
        db.query(ThreatMapEventRecord)
        .order_by(desc(ThreatMapEventRecord.observed_at))
        .limit(limit)
        .all()
    )
    return [_to_response(record) for record in records]

@router.post("/events", response_model=ThreatMapEvent, dependencies=[Depends(verify_internal_secret)])
async def create_threat_map_event(
    event: ThreatMapEvent,
    db: Session = Depends(get_db),
):
    """
    Protected endpoint to register a new threat event from internal modules
    (e.g., QR Scanner, URL Scanner, VirusTotal results).
    Requires 'X-Internal-Secret' header matching API_INTERNAL_SECRET.
    """
    try:
        observed_at = datetime.fromisoformat(event.observedAt.replace("Z", "+00:00"))
    except ValueError as exc:
        raise HTTPException(status_code=422, detail="observedAt must be an ISO-8601 timestamp") from exc

    record = ThreatMapEventRecord(
        source_city=event.sourceCity.strip()[:120],
        source_lat=event.sourceLat,
        source_lng=event.sourceLng,
        target_city=event.targetCity.strip()[:120],
        target_lat=event.targetLat,
        target_lng=event.targetLng,
        severity=event.severity,
        threat_type=event.threatType.strip()[:120],
        source_module=event.sourceModule.strip()[:80],
        observed_at=observed_at,
    )
    db.add(record)
    db.commit()
    db.refresh(record)

    # Do not log locations or user-derived values.
    logger.info("Threat map event stored from module %s", record.source_module)
    return _to_response(record)
