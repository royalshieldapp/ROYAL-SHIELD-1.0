"""
SOS Emergency Alert API Routes
Ported from Node.js backend - emergency SMS alerts via Twilio
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime
import logging
import os

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/sos", tags=["SOS Emergency"])


class LocationData(BaseModel):
    lat: float
    lng: float


class SosAlertRequest(BaseModel):
    location: LocationData
    type: str = Field(default="EMERGENCY", description="EMERGENCY | MEDICAL | FIRE")
    contacts: List[str] = Field(..., description="List of phone numbers to notify")
    userName: str = Field(default="A Royal Shield user")


class SmsFailure(BaseModel):
    contact: str
    error: str


class SosAlertResponse(BaseModel):
    success: bool
    incidentId: str
    smsSent: int
    smsFailed: int = 0
    warning: Optional[str] = None
    failures: Optional[List[SmsFailure]] = None


def _get_twilio_client():
    """Lazy-init Twilio client to avoid crash if env vars missing"""
    account_sid = os.getenv("TWILIO_ACCOUNT_SID")
    auth_token = os.getenv("TWILIO_AUTH_TOKEN")

    if not account_sid or not auth_token:
        return None

    try:
        from twilio.rest import Client
        return Client(account_sid, auth_token)
    except ImportError:
        logger.warning("twilio package not installed. Run: pip install twilio")
        return None


@router.post("/alert", response_model=SosAlertResponse)
async def send_sos_alert(request: SosAlertRequest):
    """
    Send emergency SOS alert via SMS to all emergency contacts.

    Sends SMS messages with location (Google Maps link) to all provided
    contact numbers using Twilio. Gracefully degrades if Twilio is not configured.
    """
    if not request.contacts or len(request.contacts) == 0:
        raise HTTPException(
            status_code=400,
            detail="At least one contact phone number is required"
        )

    incident_id = f"INC-{int(datetime.utcnow().timestamp() * 1000)}"
    maps_link = f"https://maps.google.com/?q={request.location.lat},{request.location.lng}"
    message_body = (
        f"🚨 ROYAL SHIELD {request.type} ALERT\n"
        f"{request.userName} needs help!\n"
        f"Location: {maps_link}\n"
        f"Incident: {incident_id}"
    )

    logger.info(
        f"[SOS] Incident {incident_id} | type={request.type} | "
        f"contacts={len(request.contacts)}"
    )

    client = _get_twilio_client()
    from_number = os.getenv("TWILIO_PHONE_NUMBER")

    if not client or not from_number:
        logger.warning(
            "[SOS] Twilio not configured. "
            "Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_PHONE_NUMBER"
        )
        return SosAlertResponse(
            success=True,
            incidentId=incident_id,
            smsSent=0,
            warning="Twilio not configured — SMS not sent. Configure env vars."
        )

    # Send SMS to all contacts
    sent = 0
    failed = 0
    failures: List[SmsFailure] = []

    for contact in request.contacts:
        try:
            client.messages.create(
                body=message_body,
                from_=from_number,
                to=contact
            )
            sent += 1
        except Exception as e:
            failed += 1
            failures.append(SmsFailure(contact=contact, error=str(e)))
            logger.error(f"[SOS] SMS failed to {contact}: {e}")

    return SosAlertResponse(
        success=True,
        incidentId=incident_id,
        smsSent=sent,
        smsFailed=failed,
        failures=failures if failures else None
    )
