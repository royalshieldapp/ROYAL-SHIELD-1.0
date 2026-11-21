"""Transactional, server-authoritative Loyalty operations."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from uuid import uuid4

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from services.loyalty.models import LoyaltyAccount, LoyaltyLedgerEntry, LoyaltyRedemption


@dataclass(frozen=True)
class RewardDefinition:
    reward_id: str
    name: str
    description: str
    cost: int
    enabled: bool = False


REWARDS = {
    "vpn_30_days": RewardDefinition(
        "vpn_30_days", "1 Month Premium VPN", "Unlimited secure tunnel access for 30 days.", 200
    ),
    "matrix_theme": RewardDefinition(
        "matrix_theme", "Matrix Custom Theme", "Unlock the Matrix visual theme.", 500
    ),
    "ai_priority_24h": RewardDefinition(
        "ai_priority_24h", "Priority AI Processing (24h)", "Priority analysis queue for 24 hours.", 800
    ),
    "vip_avatar": RewardDefinition(
        "vip_avatar", "VIP Custom Avatar", "Exclusive digital-agent avatar presets.", 1500
    ),
}


def tier_for(points: int) -> str:
    if points >= 1500:
        return "Elite"
    if points >= 800:
        return "Gold"
    if points >= 300:
        return "Silver"
    return "Bronze"


def next_tier(points: int) -> tuple[str | None, int]:
    for name, threshold in (("Silver", 300), ("Gold", 800), ("Elite", 1500)):
        if points < threshold:
            return name, threshold - points
    return None, 0


def get_or_create_account(db: Session, firebase_uid: str, *, lock: bool = False) -> LoyaltyAccount:
    query = db.query(LoyaltyAccount).filter(LoyaltyAccount.firebase_uid == firebase_uid)
    account = query.with_for_update().one_or_none() if lock else query.one_or_none()
    if account is None:
        account = LoyaltyAccount(firebase_uid=firebase_uid, points=0)
        db.add(account)
        db.flush()
    return account


def status_for(db: Session, firebase_uid: str) -> dict:
    account = get_or_create_account(db, firebase_uid)
    db.commit()
    next_name, remaining = next_tier(account.points)
    return {
        "points": account.points,
        "tier": tier_for(account.points),
        "nextTier": next_name,
        "pointsToNextTier": remaining,
    }


def claim_daily_login(db: Session, firebase_uid: str) -> dict:
    action = "daily_login"
    event_key = f"{action}:{datetime.now(timezone.utc).date().isoformat()}"
    existing = db.query(LoyaltyLedgerEntry).filter_by(firebase_uid=firebase_uid, event_key=event_key).one_or_none()
    if existing is not None:
        return {
            "success": True,
            "action": action,
            "pointsAwarded": 0,
            "newTotal": existing.balance_after,
            "currentTier": tier_for(existing.balance_after),
            "duplicate": True,
        }

    try:
        account = get_or_create_account(db, firebase_uid, lock=True)
        account.points += 10
        db.add(
            LoyaltyLedgerEntry(
                id=str(uuid4()),
                firebase_uid=firebase_uid,
                event_key=event_key,
                action=action,
                delta=10,
                balance_after=account.points,
            )
        )
        db.commit()
    except IntegrityError:
        db.rollback()
        existing = db.query(LoyaltyLedgerEntry).filter_by(firebase_uid=firebase_uid, event_key=event_key).one()
        account = get_or_create_account(db, firebase_uid)
        return {
            "success": True,
            "action": action,
            "pointsAwarded": 0,
            "newTotal": account.points,
            "currentTier": tier_for(account.points),
            "duplicate": True,
        }

    return {
        "success": True,
        "action": action,
        "pointsAwarded": 10,
        "newTotal": account.points,
        "currentTier": tier_for(account.points),
        "duplicate": False,
    }


def redeem_reward(db: Session, firebase_uid: str, reward_id: str, idempotency_key: str) -> dict:
    previous = db.query(LoyaltyRedemption).filter_by(
        firebase_uid=firebase_uid,
        idempotency_key=idempotency_key,
    ).one_or_none()
    if previous is not None:
        account = get_or_create_account(db, firebase_uid)
        return _redemption_response(previous, account.points)

    reward = REWARDS.get(reward_id)
    if reward is None:
        raise ValueError("REWARD_NOT_FOUND")
    if not reward.enabled:
        raise ValueError("REWARD_NOT_AVAILABLE")

    account = get_or_create_account(db, firebase_uid, lock=True)
    if account.points < reward.cost:
        raise ValueError("INSUFFICIENT_POINTS")

    account.points -= reward.cost
    redemption = LoyaltyRedemption(
        id=str(uuid4()),
        firebase_uid=firebase_uid,
        idempotency_key=idempotency_key,
        reward_id=reward.reward_id,
        points_spent=reward.cost,
        status="PENDING",
    )
    db.add(redemption)
    db.add(
        LoyaltyLedgerEntry(
            id=str(uuid4()),
            firebase_uid=firebase_uid,
            event_key=f"redemption:{redemption.id}",
            action="redeem",
            delta=-reward.cost,
            balance_after=account.points,
        )
    )
    db.commit()
    return _redemption_response(redemption, account.points)


def _redemption_response(redemption: LoyaltyRedemption, balance: int) -> dict:
    return {
        "redemptionId": redemption.id,
        "rewardId": redemption.reward_id,
        "pointsSpent": redemption.points_spent,
        "newTotal": balance,
        "currentTier": tier_for(balance),
        "status": redemption.status,
    }
