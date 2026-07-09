"""
Cyber Threat Intelligence API Routes
Ported from Node.js backend - threat data by geographic location
"""
from fastapi import APIRouter, Query
from pydantic import BaseModel
from typing import List, Optional
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/threats", tags=["Threat Intelligence"])


class ThreatData(BaseModel):
    """Individual threat record"""
    id: str
    type: str
    lat: float
    lng: float
    severity: str
    description: str


class ThreatsResponse(BaseModel):
    threats: List[ThreatData]


@router.get("/", response_model=ThreatsResponse)
async def get_threats(
    lat: Optional[float] = Query(None, description="Center latitude"),
    lng: Optional[float] = Query(None, description="Center longitude")
):
    """
    Get nearby cyber threats by geographic location.

    Returns threat intelligence data centered around the given coordinates.
    TODO: Connect to real threat intel feeds (AlienVault OTX, AbuseIPDB, etc.)
    """
    base_lat = lat if lat else 40.7128
    base_lng = lng if lng else -74.0060

    # Mock threat data — in production, query from
    # AlienVault OTX, AbuseIPDB, or internal threat DB
    threats = [
        ThreatData(
            id="t1",
            type="Botnet",
            lat=base_lat + 0.01,
            lng=base_lng + 0.01,
            severity="HIGH",
            description="Active Mirai Botnet Node"
        ),
        ThreatData(
            id="t2",
            type="Phishing",
            lat=base_lat - 0.005,
            lng=base_lng - 0.005,
            severity="MEDIUM",
            description="SMS Phishing Campaign Source"
        ),
        ThreatData(
            id="t3",
            type="Malware",
            lat=base_lat + 0.015,
            lng=base_lng - 0.01,
            severity="LOW",
            description="Adware Distribution Server"
        ),
    ]

    return ThreatsResponse(threats=threats)
