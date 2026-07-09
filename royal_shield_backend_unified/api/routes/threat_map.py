"""
Threat Map API Routes
Handles live threat signals for the Live Satellite Cyber Attack Map widget
"""
from fastapi import APIRouter, HTTPException, Depends, Request, Header
from pydantic import BaseModel, Field, validator
from typing import List, Optional
from datetime import datetime
import logging
import os

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/threat-map", tags=["Threat Map"])

# Optional: In-memory store for development if PostgreSQL is not available
# Limits to 100 recent events
_IN_MEMORY_EVENTS = []
MAX_EVENTS = 100

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
    observedAt: str = Field(default_factory=lambda: datetime.utcnow().isoformat() + "Z")

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
async def get_threat_map_events():
    """
    Public endpoint to get recent threat events for the Live Threat Intelligence Signals widget.
    """
    # Returns the in-memory list (last N events)
    # In production, this would query the `threat_map_events` table in PostgreSQL
    return list(reversed(_IN_MEMORY_EVENTS))

@router.post("/events", response_model=ThreatMapEvent, dependencies=[Depends(verify_internal_secret)])
async def create_threat_map_event(event: ThreatMapEvent):
    """
    Protected endpoint to register a new threat event from internal modules
    (e.g., QR Scanner, URL Scanner, VirusTotal results).
    Requires 'X-Internal-Secret' header matching API_INTERNAL_SECRET.
    """
    # Add to in-memory store
    _IN_MEMORY_EVENTS.append(event)
    if len(_IN_MEMORY_EVENTS) > MAX_EVENTS:
        _IN_MEMORY_EVENTS.pop(0)

    logger.info(f"New threat map event recorded: {event.threatType} from {event.sourceCity} to {event.targetCity}")

    # TODO: Also save to PostgreSQL `threat_map_events` table when available
    return event
