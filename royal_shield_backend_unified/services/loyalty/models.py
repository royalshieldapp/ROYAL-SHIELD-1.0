"""Durable Loyalty accounts, ledger entries and redemptions."""
from __future__ import annotations

from datetime import datetime, timezone

from sqlalchemy import CheckConstraint, Column, DateTime, Integer, String, UniqueConstraint

from services.geospatial.database.connection import Base


def _now() -> datetime:
    return datetime.now(timezone.utc)


class LoyaltyAccount(Base):
    __tablename__ = "loyalty_accounts"
    __table_args__ = (CheckConstraint("points >= 0", name="ck_loyalty_accounts_points_nonnegative"),)

    firebase_uid = Column(String(128), primary_key=True)
    points = Column(Integer, nullable=False, default=0)
    created_at = Column(DateTime(timezone=True), nullable=False, default=_now)
    updated_at = Column(DateTime(timezone=True), nullable=False, default=_now, onupdate=_now)


class LoyaltyLedgerEntry(Base):
    __tablename__ = "loyalty_ledger"
    __table_args__ = (
        UniqueConstraint("firebase_uid", "event_key", name="uq_loyalty_ledger_user_event"),
    )

    id = Column(String(36), primary_key=True)
    firebase_uid = Column(String(128), nullable=False, index=True)
    event_key = Column(String(180), nullable=False)
    action = Column(String(64), nullable=False, index=True)
    delta = Column(Integer, nullable=False)
    balance_after = Column(Integer, nullable=False)
    created_at = Column(DateTime(timezone=True), nullable=False, default=_now, index=True)


class LoyaltyRedemption(Base):
    __tablename__ = "loyalty_redemptions"
    __table_args__ = (
        UniqueConstraint("firebase_uid", "idempotency_key", name="uq_loyalty_redemption_user_key"),
    )

    id = Column(String(36), primary_key=True)
    firebase_uid = Column(String(128), nullable=False, index=True)
    idempotency_key = Column(String(36), nullable=False)
    reward_id = Column(String(64), nullable=False, index=True)
    points_spent = Column(Integer, nullable=False)
    status = Column(String(24), nullable=False, default="PENDING")
    created_at = Column(DateTime(timezone=True), nullable=False, default=_now, index=True)
