from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from services.geospatial.database.connection import Base
from services.loyalty.service import claim_daily_login, redeem_reward, status_for


def _session():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    return sessionmaker(bind=engine)()


def test_new_account_starts_at_zero():
    db = _session()
    status = status_for(db, "firebase-user-1")
    assert status == {
        "points": 0,
        "tier": "Bronze",
        "nextTier": "Silver",
        "pointsToNextTier": 300,
    }


def test_daily_login_is_idempotent():
    db = _session()
    first = claim_daily_login(db, "firebase-user-2")
    second = claim_daily_login(db, "firebase-user-2")
    assert first["pointsAwarded"] == 10
    assert first["newTotal"] == 10
    assert first["duplicate"] is False
    assert second["pointsAwarded"] == 0
    assert second["newTotal"] == 10
    assert second["duplicate"] is True


def test_unfulfilled_reward_cannot_spend_points():
    db = _session()
    claim_daily_login(db, "firebase-user-3")
    try:
        redeem_reward(db, "firebase-user-3", "vpn_30_days", "199c244e-0de8-4b10-a3cc-05bf43e80a98")
    except ValueError as exc:
        assert str(exc) == "REWARD_NOT_AVAILABLE"
    else:
        raise AssertionError("disabled reward was redeemed")
    assert status_for(db, "firebase-user-3")["points"] == 10
