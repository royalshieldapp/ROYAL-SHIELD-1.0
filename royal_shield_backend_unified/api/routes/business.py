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


@router.post("/quote", response_model=QuoteResponse)
async def request_quote(request: QuoteRequest):
    """
    Submit a business/enterprise quote request.

    In production, this should send an email notification to the sales team
    via SendGrid/SES and store the lead in a CRM.
    """
    ticket_id = f"Q-{int(datetime.utcnow().timestamp() * 1000)}"

    logger.info(
        f"Business quote: company={request.companyName} "
        f"contact={request.contactName} email={request.email} "
        f"ticket={ticket_id}"
    )

    # TODO: Send email notification via SendGrid/SES
    # TODO: Store lead in database/CRM

    return QuoteResponse(
        success=True,
        message="Quote request received successfully. Our team will contact you shortly.",
        ticketId=ticket_id
    )
