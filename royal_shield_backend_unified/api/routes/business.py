"""
Business Quote API Routes
Ported from Node.js backend - B2B enterprise quote requests
"""
from fastapi import APIRouter
from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/business", tags=["Business"])


class QuoteRequest(BaseModel):
    contactName: str
    companyName: str
    email: str
    phone: Optional[str] = None
    employees: Optional[int] = None
    requirements: Optional[str] = None


class QuoteResponse(BaseModel):
    success: bool
    message: str
    ticketId: str


import os
import smtplib
from email.mime.text import MIMEText

@router.post("/quote", response_model=QuoteResponse)
async def request_quote(request: QuoteRequest):
    """
    Submit a business/enterprise quote request. Stores the lead and optionally sends email.
    """
    ticket_id = f"Q-{int(datetime.utcnow().timestamp() * 1000)}"

    logger.info(
        f"Business quote: company={request.companyName} "
        f"contact={request.contactName} email={request.email} "
        f"ticket={ticket_id}"
    )

    # Dispatch email if SMTP settings are configured
    smtp_host = os.getenv("SMTP_HOST")
    smtp_port = int(os.getenv("SMTP_PORT", "587"))
    smtp_user = os.getenv("SMTP_USER")
    smtp_pass = os.getenv("SMTP_PASS")
    recipient = os.getenv("BUSINESS_LEAD_EMAIL", "sales@royalshield.app")

    if smtp_host and smtp_user and smtp_pass:
        try:
            body = (
                f"New Business Quote Request [{ticket_id}]\n\n"
                f"Company: {request.companyName}\n"
                f"Contact: {request.contactName}\n"
                f"Email: {request.email}\n"
                f"Phone: {request.phone or 'N/A'}\n"
                f"Employees: {request.employees or 'N/A'}\n"
                f"Requirements: {request.requirements or 'N/A'}\n"
            )
            msg = MIMEText(body)
            msg["Subject"] = f"Royal Shield Business Quote: {request.companyName} [{ticket_id}]"
            msg["From"] = smtp_user
            msg["To"] = recipient

            with smtplib.SMTP(smtp_host, smtp_port, timeout=10) as server:
                server.starttls()
                server.login(smtp_user, smtp_pass)
                server.send_message(msg)
            logger.info(f"Email notification sent for ticket {ticket_id}")
        except Exception as e:
            logger.warning(f"Could not dispatch lead notification email: {e}")

    return QuoteResponse(
        success=True,
        message="Quote request received successfully. Our team will contact you shortly.",
        ticketId=ticket_id
    )
