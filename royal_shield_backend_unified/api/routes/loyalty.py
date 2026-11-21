"""Authenticated, durable Loyalty API."""
from __future__ import annotations

from typing import Literal
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from api.security.firebase_auth import require_firebase_uid
from services.geospatial.database.connection import get_db
from services.loyalty.service import REWARDS, claim_daily_login, redeem_reward, status_for

router = APIRouter(prefix="/api/v1/loyalty", tags=["Loyalty Program"])


class LoyaltyStatusResponse(BaseModel):
    points: int = Field(ge=0)
    tier: str
    nextTier: str | None
    pointsToNextTier: int = Field(ge=0)


class ClaimRequest(BaseModel):
    action: Literal["daily_login"]


class ClaimResponse(BaseModel):
    success: bool
    action: str
    pointsAwarded: int = Field(ge=0)
    newTotal: int = Field(ge=0)
    currentTier: str
    duplicate: bool


class RewardResponse(BaseModel):
    rewardId: str
    name: str
    description: str
    cost: int = Field(gt=0)
    enabled: bool


class RedeemRequest(BaseModel):
    rewardId: str = Field(min_length=1, max_length=64, pattern=r"^[a-z0-9_]+$")
    idempotencyKey: UUID


class RedemptionResponse(BaseModel):
    redemptionId: str
    rewardId: str
    pointsSpent: int = Field(gt=0)
    newTotal: int = Field(ge=0)
    currentTier: str
    status: str


@router.get("/status", response_model=LoyaltyStatusResponse)
def get_loyalty_status(
    firebase_uid: str = Depends(require_firebase_uid),
    db: Session = Depends(get_db),
):
    return status_for(db, firebase_uid)


@router.post("/claims", response_model=ClaimResponse)
def claim_loyalty_action(
    request: ClaimRequest,
    firebase_uid: str = Depends(require_firebase_uid),
    db: Session = Depends(get_db),
):
    if request.action == "daily_login":
        return claim_daily_login(db, firebase_uid)
    raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Unsupported loyalty action")


@router.get("/rewards", response_model=list[RewardResponse])
def list_rewards(firebase_uid: str = Depends(require_firebase_uid)):
    # Authentication is required even though the current catalog is global.
    del firebase_uid
    return [
        RewardResponse(
            rewardId=reward.reward_id,
            name=reward.name,
            description=reward.description,
            cost=reward.cost,
            enabled=reward.enabled,
        )
        for reward in REWARDS.values()
    ]


@router.post("/redeem", response_model=RedemptionResponse)
def redeem_loyalty_reward(
    request: RedeemRequest,
    firebase_uid: str = Depends(require_firebase_uid),
    db: Session = Depends(get_db),
):
    try:
        return redeem_reward(db, firebase_uid, request.rewardId, str(request.idempotencyKey))
    except ValueError as exc:
        code = str(exc)
        if code == "REWARD_NOT_FOUND":
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Reward not found") from None
        if code == "REWARD_NOT_AVAILABLE":
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Reward is not available yet") from None
        if code == "INSUFFICIENT_POINTS":
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Insufficient points") from None
        raise


@router.post("/points", status_code=status.HTTP_410_GONE)
def reject_legacy_points_endpoint():
    """Retire the endpoint that trusted user IDs and point amounts from clients."""
    raise HTTPException(
        status_code=status.HTTP_410_GONE,
        detail="Use authenticated claims or server-issued awards",
    )
