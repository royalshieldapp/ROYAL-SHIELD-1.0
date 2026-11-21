"""Load normalized Forecast Engine 2 events from durable Royal Shield data."""
from datetime import datetime, timezone
from typing import Any, Dict, List

from sqlalchemy import text
from sqlalchemy.orm import Session

from services.data_ingestion.storage import CyberThreatRecord, ForecastEventRecord, ForecastPoiRecord


def load_threat_map_events(db: Session, limit: int = 100000) -> List[Dict[str, Any]]:
    """Convert stored threat-map signals into the common ML event schema."""
    rows = db.execute(
        text(
            """
            SELECT id, target_lat, target_lng, severity, threat_type,
                   source_module, observed_at
            FROM threat_map_events
            ORDER BY observed_at ASC
            LIMIT :limit
            """
        ),
        {"limit": limit},
    ).mappings()

    events = []
    for row in rows:
        occurred_at = row["observed_at"]
        if isinstance(occurred_at, str):
            occurred_at = datetime.fromisoformat(occurred_at.replace("Z", "+00:00"))
        if occurred_at.tzinfo is None:
            occurred_at = occurred_at.replace(tzinfo=timezone.utc)
        events.append(
            {
                "external_id": f"THREAT_MAP_{row['id']}",
                "source": "ROYAL_SHIELD",
                "event_type": row["threat_type"],
                "event_category": "CYBER_THREAT",
                "severity": row["severity"],
                "location": (float(row["target_lat"]), float(row["target_lng"])),
                "occurred_at": occurred_at,
                "source_module": row["source_module"],
            }
        )
    return events


def load_forecast_events(db: Session, limit: int = 500000) -> List[Dict[str, Any]]:
    """Load normalized geolocated events produced by external collectors."""
    records = (
        db.query(ForecastEventRecord)
        .order_by(ForecastEventRecord.occurred_at.asc())
        .limit(limit)
        .all()
    )
    return [
        {
            "external_id": record.external_id,
            "source": record.source,
            "event_type": record.event_type,
            "event_category": record.event_category,
            "severity": record.severity,
            "location": (record.latitude, record.longitude),
            "occurred_at": record.occurred_at,
        }
        for record in records
    ]


def load_forecast_pois(db: Session, limit: int = 500000) -> List[Dict[str, Any]]:
    """Load normalized points of interest used as spatial model features."""
    records = db.query(ForecastPoiRecord).limit(limit).all()
    return [
        {
            "external_id": record.external_id,
            "source": record.source,
            "poi_type": record.poi_type,
            "location": (record.latitude, record.longitude),
        }
        for record in records
    ]


def load_training_data(db: Session) -> tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    """Return all durable, geolocated events and POIs available for training."""
    events = load_forecast_events(db)
    events.extend(load_threat_map_events(db))
    events.sort(key=lambda item: item["occurred_at"])
    return events, load_forecast_pois(db)


def load_cyber_training_data(db: Session, limit: int = 500000) -> List[Dict[str, Any]]:
    """Load provider-neutral cyber observations for the global cyber model."""
    records = (
        db.query(CyberThreatRecord)
        .order_by(CyberThreatRecord.first_seen.asc())
        .limit(limit)
        .all()
    )
    return [{
        "provider": record.provider,
        "indicator_type": record.indicator_type,
        "severity": record.severity,
        "confidence": record.confidence,
        "observed_at": record.first_seen or record.last_seen,
    } for record in records]
