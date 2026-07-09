"""
Loyalty Points API Routes
Ported from Node.js backend - user loyalty/gamification system
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/loyalty", tags=["Loyalty Program"])

# In-memory store — replace with DB in production
_user_points: dict = {}


def _get_tier(points: int) -> str:
    if points > 5000:
        return "Platinum"
    elif points > 1000:
        return "Gold"
    elif points > 200:
        return "Silver"
    return "Bronze"


def _get_next_tier(current_tier: str) -> str:
    tiers = {"Bronze": "Silver", "Silver": "Gold", "Gold": "Platinum", "Platinum": "Platinum"}
    return tiers.get(current_tier, "Silver")


def _points_to_next_tier(points: int) -> int:
    if points > 5000:
        return 0
    elif points > 1000:
        return 5000 - points
    elif points > 200:
        return 1000 - points
    return 200 - points


class LoyaltyStatusResponse(BaseModel):
    points: int
    tier: str
    nextTier: str
    pointsToNextTier: int


class AddPointsRequest(BaseModel):
    action: str = Field(..., description="Action that earned points (e.g., 'daily_scan')")
    points: int = Field(..., gt=0, description="Points to add")
    userId: str = Field(default="default", description="User identifier")


class AddPointsResponse(BaseModel):
    success: bool
    message: str
    newTotal: int
    currentTier: str


@router.get("/status", response_model=LoyaltyStatusResponse)
async def get_loyalty_status(userId: str = "default"):
    """
    Get current loyalty points and tier status for a user.
    """
    points = _user_points.get(userId, 1250)  # Default starting points
    tier = _get_tier(points)

    return LoyaltyStatusResponse(
        points=points,
        tier=tier,
        nextTier=_get_next_tier(tier),
        pointsToNextTier=_points_to_next_tier(points)
    )


@router.post("/points", response_model=AddPointsResponse)
async def add_loyalty_points(request: AddPointsRequest):
    """
    Add loyalty points for a user action.

    Actions: daily_scan, threat_report, course_complete, referral, etc.
    """
    current = _user_points.get(request.userId, 1250)
    new_total = current + request.points
    _user_points[request.userId] = new_total
    tier = _get_tier(new_total)

    logger.info(
        f"Loyalty: user={request.userId} action={request.action} "
        f"+{request.points} total={new_total} tier={tier}"
    )

    return AddPointsResponse(
        success=True,
        message=f"Added {request.points} points for {request.action}",
        newTotal=new_total,
        currentTier=tier
    )
