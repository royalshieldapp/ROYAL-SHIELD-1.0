from fastapi import APIRouter, HTTPException, status, Header, Request
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
import logging
import time
import uuid
from datetime import datetime, timedelta
from config.settings import settings

router = APIRouter(prefix="/api/security", tags=["Security"])
logger = logging.getLogger(__name__)

# In-memory event store
SECURITY_EVENTS = []
MAX_EVENTS = 10000

class EventPayload(BaseModel):
    type: str
    severity: str = "LOW"
    source: str
    details: Optional[Dict[str, Any]] = Field(default_factory=dict)
    deviceId: Optional[str] = None
    userId: Optional[str] = None

@router.post("/events", status_code=status.HTTP_201_CREATED)
def log_security_event(payload: EventPayload, request: Request):
    if not payload.type:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="type is required")
    if not payload.source:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="source is required")

    valid_severities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
    severity_upper = payload.severity.upper()
    if severity_upper not in valid_severities:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"severity must be one of: {', '.join(valid_severities)}"
        )

    event_id = f"EVT-{int(time.time() * 1000)}-{str(uuid.uuid4())[:6]}"
    ip_address = request.client.host if request.client else "unknown"

    event = {
        "id": event_id,
        "type": payload.type.upper(),
        "severity": severity_upper,
        "source": payload.source,
        "details": payload.details,
        "deviceId": payload.deviceId or "unknown",
        "userId": payload.userId or "anonymous",
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "ip": ip_address
    }

    # Lock & Append
    if len(SECURITY_EVENTS) >= MAX_EVENTS:
        SECURITY_EVENTS.pop(0)  # Remove oldest

    SECURITY_EVENTS.append(event)
    logger.info(f"[Security] {event['severity']} | {event['type']} from {event['source']} | device={event['deviceId']}")

    return {
        "success": True,
        "eventId": event_id,
        "message": "Security event logged"
    }

@router.get("/events")
def get_security_events(
    x_internal_secret: Optional[str] = Header(None),
    limit: int = 50,
    severity: Optional[str] = None,
    type: Optional[str] = None,
    source: Optional[str] = None,
    deviceId: Optional[str] = None
):
    internal_secret = settings.api_internal_secret

    if internal_secret and x_internal_secret != internal_secret:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Forbidden — internal secret required"
        )

    filtered = list(SECURITY_EVENTS)

    if severity:
        filtered = [e for e in filtered if e["severity"] == severity.upper()]
    if type:
        filtered = [e for e in filtered if e["type"] == type.upper()]
    if source:
        filtered = [e for e in filtered if e["source"] == source]
    if deviceId:
        filtered = [e for e in filtered if e["deviceId"] == deviceId]

    # Limit and sort (newest first)
    limit = min(max(limit, 1), 500)
    filtered.reverse()
    result_slice = filtered[:limit]

    return {
        "success": True,
        "total": len(SECURITY_EVENTS),
        "returned": len(result_slice),
        "events": result_slice
    }

@router.get("/summary")
def get_security_summary():
    last_24h = datetime.utcnow() - timedelta(hours=24)
    recent = []

    for e in SECURITY_EVENTS:
        try:
            # Parse timestamp (stripping trailing Z for datetime.fromisoformat)
            ts_str = e["timestamp"]
            if ts_str.endswith("Z"):
                ts_str = ts_str[:-1]
            ts = datetime.fromisoformat(ts_str)
            if ts > last_24h:
                recent.append(e)
        except Exception:
            pass

    by_severity = {"LOW": 0, "MEDIUM": 0, "HIGH": 0, "CRITICAL": 0}
    by_type = {}
    by_source = {}

    for e in recent:
        sev = e["severity"]
        by_severity[sev] = by_severity.get(sev, 0) + 1

        t = e["type"]
        by_type[t] = by_type.get(t, 0) + 1

        s = e["source"]
        by_source[s] = by_source.get(s, 0) + 1

    return {
        "success": True,
        "period": "24h",
        "totalEvents": len(recent),
        "bySeverity": by_severity,
        "byType": by_type,
        "bySource": by_source
    }
