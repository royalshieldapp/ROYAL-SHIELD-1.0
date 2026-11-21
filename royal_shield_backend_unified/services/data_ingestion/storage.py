"""Durable normalized storage for Forecast Engine 2 training inputs."""
from datetime import datetime, timezone
from typing import Any, Dict, Iterable

from sqlalchemy import Boolean, Column, DateTime, Float, Integer, String, Text, UniqueConstraint
from sqlalchemy.exc import SQLAlchemyError

from services.geospatial.database.connection import Base, SessionLocal


class ForecastEventRecord(Base):
    __tablename__ = "forecast_events"
    __table_args__ = (UniqueConstraint("source", "external_id", name="uq_forecast_event_source_external"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    external_id = Column(String(255), nullable=False)
    source = Column(String(64), nullable=False, index=True)
    event_type = Column(String(120), nullable=False, index=True)
    event_category = Column(String(64), nullable=False, index=True)
    severity = Column(String(16), nullable=False, index=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    occurred_at = Column(DateTime(timezone=True), nullable=False, index=True)
    collected_at = Column(DateTime(timezone=True), nullable=False, default=lambda: datetime.now(timezone.utc))


class ForecastPoiRecord(Base):
    __tablename__ = "forecast_pois"
    __table_args__ = (UniqueConstraint("source", "external_id", name="uq_forecast_poi_source_external"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    external_id = Column(String(255), nullable=False)
    source = Column(String(64), nullable=False, index=True)
    poi_type = Column(String(80), nullable=False, index=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    updated_at = Column(DateTime(timezone=True), nullable=False, default=lambda: datetime.now(timezone.utc))


class CyberThreatRecord(Base):
    """Provider-neutral cyber indicator; location is optional and never inferred."""
    __tablename__ = "cyber_threat_events"
    __table_args__ = (UniqueConstraint("provider", "external_id", name="uq_cyber_provider_external"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    external_id = Column(String(255), nullable=False)
    provider = Column(String(32), nullable=False, index=True)
    indicator_type = Column(String(40), nullable=False, index=True)
    indicator_value = Column(String(512), nullable=False)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    severity = Column(String(16), nullable=False, index=True)
    confidence = Column(Integer, nullable=False, default=0)
    active = Column(Boolean, nullable=False, default=True, index=True)
    first_seen = Column(DateTime(timezone=True), nullable=True)
    last_seen = Column(DateTime(timezone=True), nullable=False, index=True)
    latitude = Column(Float, nullable=True)
    longitude = Column(Float, nullable=True)
    collected_at = Column(DateTime(timezone=True), nullable=False, default=lambda: datetime.now(timezone.utc))


def _aware(value: Any) -> datetime:
    if isinstance(value, str):
        value = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if not isinstance(value, datetime):
        raise ValueError("occurred_at must be a datetime or ISO-8601 string")
    return value if value.tzinfo else value.replace(tzinfo=timezone.utc)


def store_forecast_events(records: Iterable[Dict[str, Any]]) -> Dict[str, int]:
    stats = {"records_inserted": 0, "records_updated": 0, "records_failed": 0}
    with SessionLocal() as db:
        for item in records:
            try:
                with db.begin_nested():
                    lat, lng = item["location"]
                    lat, lng = float(lat), float(lng)
                    if not (-90 <= lat <= 90 and -180 <= lng <= 180):
                        raise ValueError("coordinates out of range")
                    source = str(item["source"])[:64]
                    external_id = str(item["external_id"])[:255]
                    record = db.query(ForecastEventRecord).filter_by(source=source, external_id=external_id).one_or_none()
                    values = {
                        "event_type": str(item["event_type"])[:120],
                        "event_category": str(item["event_category"])[:64],
                        "severity": str(item["severity"]).lower()[:16],
                        "latitude": lat,
                        "longitude": lng,
                        "occurred_at": _aware(item["occurred_at"]),
                    }
                    if record is None:
                        db.add(ForecastEventRecord(source=source, external_id=external_id, **values))
                        action = "records_inserted"
                    else:
                        for key, value in values.items():
                            setattr(record, key, value)
                        record.collected_at = datetime.now(timezone.utc)
                        action = "records_updated"
                    db.flush()
                    stats[action] += 1
            except (KeyError, TypeError, ValueError, SQLAlchemyError):
                stats["records_failed"] += 1
        db.commit()
    return stats


def store_forecast_pois(records: Iterable[Dict[str, Any]]) -> Dict[str, int]:
    stats = {"records_inserted": 0, "records_updated": 0, "records_failed": 0}
    with SessionLocal() as db:
        for item in records:
            try:
                with db.begin_nested():
                    lat, lng = item["location"]
                    lat, lng = float(lat), float(lng)
                    if not (-90 <= lat <= 90 and -180 <= lng <= 180):
                        raise ValueError("coordinates out of range")
                    source = str(item["source"])[:64]
                    external_id = str(item["external_id"])[:255]
                    record = db.query(ForecastPoiRecord).filter_by(source=source, external_id=external_id).one_or_none()
                    values = {
                        "poi_type": str(item["poi_type"])[:80],
                        "latitude": lat,
                        "longitude": lng,
                        "updated_at": datetime.now(timezone.utc),
                    }
                    if record is None:
                        db.add(ForecastPoiRecord(source=source, external_id=external_id, **values))
                        action = "records_inserted"
                    else:
                        for key, value in values.items():
                            setattr(record, key, value)
                        action = "records_updated"
                    db.flush()
                    stats[action] += 1
            except (KeyError, TypeError, ValueError, SQLAlchemyError):
                stats["records_failed"] += 1
        db.commit()
    return stats


def store_cyber_threats(records: Iterable[Dict[str, Any]]) -> Dict[str, int]:
    stats = {"records_inserted": 0, "records_updated": 0, "records_failed": 0}
    with SessionLocal() as db:
        for item in records:
            try:
                with db.begin_nested():
                    provider = str(item["provider"]).upper()[:32]
                    external_id = str(item["external_id"])[:255]
                    confidence = max(0, min(100, int(item.get("confidence", 0))))
                    latitude = item.get("latitude")
                    longitude = item.get("longitude")
                    if latitude is not None or longitude is not None:
                        latitude, longitude = float(latitude), float(longitude)
                        if not (-90 <= latitude <= 90 and -180 <= longitude <= 180):
                            raise ValueError("coordinates out of range")
                    record = db.query(CyberThreatRecord).filter_by(
                        provider=provider, external_id=external_id
                    ).one_or_none()
                    values = {
                        "indicator_type": str(item["indicator_type"]).lower()[:40],
                        "indicator_value": str(item["indicator_value"])[:512],
                        "title": str(item["title"])[:255],
                        "description": str(item.get("description") or "")[:4000],
                        "severity": str(item["severity"]).lower()[:16],
                        "confidence": confidence,
                        "active": bool(item.get("active", True)),
                        "first_seen": _aware(item["first_seen"]) if item.get("first_seen") else None,
                        "last_seen": _aware(item["last_seen"]),
                        "latitude": latitude,
                        "longitude": longitude,
                        "collected_at": datetime.now(timezone.utc),
                    }
                    if record is None:
                        db.add(CyberThreatRecord(provider=provider, external_id=external_id, **values))
                        action = "records_inserted"
                    else:
                        for key, value in values.items():
                            setattr(record, key, value)
                        action = "records_updated"
                    db.flush()
                    stats[action] += 1
            except (KeyError, TypeError, ValueError, SQLAlchemyError):
                stats["records_failed"] += 1
        db.commit()
    return stats
