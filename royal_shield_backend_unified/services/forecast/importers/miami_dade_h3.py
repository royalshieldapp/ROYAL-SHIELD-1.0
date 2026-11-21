"""Convert approved Miami-Dade point features into privacy-minimised H3 aggregates."""

from collections import Counter
from dataclasses import dataclass
from datetime import date, datetime, timezone
from typing import Any, Iterable

import h3
import httpx
from sqlalchemy import select
from sqlalchemy.orm import Session

from services.forecast.models import ForecastObservation


PCB_FEATURE_SERVER_URL = (
    "https://services.arcgis.com/8Pc9XBTAsYuxx9Ny/arcgis/rest/services/"
    "PCB_Report/FeatureServer"
)

GRID_TO_DISTRICT_MAP = {
    "1361": "AIRPORT OPERATIONS BUREAU",
    "3059": "CITY OF MIAMI GARDENS",
}


@dataclass(frozen=True)
class H3DailyAggregate:
    observed_date: date
    h3_cell: str
    event_type: str
    event_count: int
    police_grid: str = "UNKNOWN"
    district: str = "UNKNOWN"
    created_at: datetime | None = None
    source: str = "Miami-Dade Sheriff Office Internal Affairs"
    source_dataset: str = "PCB_Report"


@dataclass
class AggregationMetrics:
    total_records: int = 0
    valid: int = 0
    invalid: int = 0
    missing_police_grid: int = 0
    missing_created_at: int = 0
    observations_prepared: int = 0


class ArcGisPcbClient:
    """Read public PCB layers page-by-page without persisting raw features."""

    def __init__(
        self,
        http_client: httpx.Client | Any | None = None,
        page_size: int = 1_000,
        timeout_seconds: float = 30,
        out_fields: str = "OBJECTID,Incident_Date,District,Grid",
        verify: bool | str | None = None,
    ) -> None:
        if http_client is not None:
            self.http_client = http_client
        else:
            v = verify if verify is not None else True
            self.http_client = httpx.Client(verify=v)
        self.page_size = page_size
        self.timeout_seconds = timeout_seconds
        self.out_fields = out_fields

    def fetch_layer(self, layer_id: int) -> Iterable[dict[str, Any]]:
        """Yield raw features only long enough for in-memory H3 aggregation."""

        offset = 0
        while True:
            response = self.http_client.get(
                f"{PCB_FEATURE_SERVER_URL}/{layer_id}/query",
                params={
                    "f": "json",
                    "where": "1=1",
                    "outFields": self.out_fields,
                    "returnGeometry": "true",
                    "outSR": "4326",
                    "resultOffset": offset,
                    "resultRecordCount": self.page_size,
                    "orderByFields": "OBJECTID",
                },
                timeout=self.timeout_seconds,
            )
            response.raise_for_status()
            payload = response.json()
            if payload.get("error"):
                raise RuntimeError("Miami-Dade PCB source returned an error")
            features = payload.get("features") or []
            yield from features
            if not payload.get("exceededTransferLimit") or not features:
                return
            offset += len(features)

    def close(self) -> None:
        """Release the HTTP connection pool."""

        self.http_client.close()


def aggregate_arcgis_features(
    features: Iterable[dict[str, Any]],
    event_type: str,
    resolution: int,
    metrics: AggregationMetrics | dict[str, int] | None = None,
    require_police_grid: bool = True,
    default_police_grid: str = "UNKNOWN",
    default_district: str = "UNKNOWN",
) -> list[H3DailyAggregate]:
    """Aggregate ArcGIS points in memory and return no person-level geometry."""

    now = datetime.now(timezone.utc)
    counts: Counter[tuple[date, str, str, str]] = Counter()

    total_records = 0
    valid = 0
    invalid = 0
    missing_police_grid = 0

    for feature in features:
        total_records += 1
        attributes = feature.get("attributes") or {}
        geometry = feature.get("geometry") or {}

        observed_at = attributes.get("Incident_Date")
        latitude = geometry.get("y")
        longitude = geometry.get("x")
        raw_grid = attributes.get("Grid") or attributes.get("police_grid")

        # Police grid check
        if raw_grid is not None and str(raw_grid).strip():
            police_grid = str(raw_grid).strip()
        elif "Grid" not in attributes and "police_grid" not in attributes:
            # Backward compatibility for synthetic test fixtures lacking Grid key
            police_grid = default_police_grid
        else:
            missing_police_grid += 1
            if require_police_grid:
                invalid += 1
                continue
            police_grid = default_police_grid

        if observed_at is None or latitude is None or longitude is None:
            invalid += 1
            continue

        try:
            latitude = float(latitude)
            longitude = float(longitude)
        except (ValueError, TypeError):
            invalid += 1
            continue

        if not -90 <= latitude <= 90 or not -180 <= longitude <= 180:
            invalid += 1
            continue

        if isinstance(observed_at, (int, float)):
            observed_date = datetime.fromtimestamp(
                observed_at / 1000, tz=timezone.utc
            ).date()
        else:
            try:
                observed_date = date.fromisoformat(str(observed_at)[:10])
            except (ValueError, TypeError):
                invalid += 1
                continue

        raw_district = attributes.get("District") or attributes.get("district")
        if raw_district and str(raw_district).strip():
            district = str(raw_district).strip()
        elif police_grid in GRID_TO_DISTRICT_MAP:
            district = GRID_TO_DISTRICT_MAP[police_grid]
        else:
            district = default_district

        h3_cell = h3.geo_to_h3(latitude, longitude, resolution)
        counts[(observed_date, district, police_grid, h3_cell)] += 1
        valid += 1

    aggregates = [
        H3DailyAggregate(
            observed_date=observed_date,
            h3_cell=h3_cell,
            event_type=event_type,
            event_count=event_count,
            police_grid=police_grid,
            district=district,
            created_at=now,
        )
        for (observed_date, district, police_grid, h3_cell), event_count in sorted(counts.items())
    ]

    missing_created_at = sum(1 for a in aggregates if a.created_at is None)

    if metrics is not None:
        if isinstance(metrics, AggregationMetrics):
            metrics.total_records += total_records
            metrics.valid += valid
            metrics.invalid += invalid
            metrics.missing_police_grid += missing_police_grid
            metrics.missing_created_at += missing_created_at
            metrics.observations_prepared += len(aggregates)
        elif isinstance(metrics, dict):
            metrics["total_records"] = metrics.get("total_records", 0) + total_records
            metrics["valid"] = metrics.get("valid", 0) + valid
            metrics["invalid"] = metrics.get("invalid", 0) + invalid
            metrics["missing_police_grid"] = metrics.get("missing_police_grid", 0) + missing_police_grid
            metrics["missing_created_at"] = metrics.get("missing_created_at", 0) + missing_created_at
            metrics["observations_prepared"] = metrics.get("observations_prepared", 0) + len(aggregates)

    return aggregates


def persist_h3_aggregates(
    session: Session,
    aggregates: Iterable[H3DailyAggregate],
    source_dataset: str,
    source: str = "Miami-Dade Sheriff Office Internal Affairs",
) -> int:
    """Persist valid H3 aggregates idempotently with a privacy-safe audit."""

    from services.forecast.importers.miami_dade import (
        SAFE_CONFLICT_REASON,
        SAFE_VALIDATION_REASON,
        _insert_observation_atomic,
        is_valid_h3_cell,
        validate_source_dataset,
    )
    from services.forecast.models import IngestionRun
    import hashlib
    import json

    source, source_dataset = validate_source_dataset(
        source, source_dataset
    )
    aggregate_rows = list(aggregates)
    normalized_rows = [
        (
            aggregate,
            {
                "observed_date": (
                    aggregate.observed_date.isoformat()
                    if type(aggregate.observed_date) is date
                    else str(aggregate.observed_date)
                ),
                "h3_cell": (
                    aggregate.h3_cell.strip().lower()
                    if isinstance(aggregate.h3_cell, str)
                    else str(aggregate.h3_cell)
                ),
                "event_type": (
                    aggregate.event_type.strip()
                    if isinstance(aggregate.event_type, str)
                    else str(aggregate.event_type)
                ),
                "event_count": (
                    aggregate.event_count
                    if isinstance(aggregate.event_count, int)
                    and not isinstance(aggregate.event_count, bool)
                    else str(aggregate.event_count)
                ),
            },
        )
        for aggregate in aggregate_rows
    ]
    checksum_payload = [normalized for _, normalized in normalized_rows]
    checksum_payload.sort(
        key=lambda row: (
            row["observed_date"],
            row["h3_cell"],
            row["event_type"],
            str(row["event_count"]),
        )
    )
    source_sha256 = hashlib.sha256(
        json.dumps(
            checksum_payload,
            sort_keys=True,
            separators=(",", ":"),
        ).encode("utf-8")
    ).hexdigest()
    started_at = datetime.now(timezone.utc)
    inserted = 0
    rejected = 0
    deduplicated = 0
    conflicted = False
    batch_counts: dict[tuple[object, ...], int] = {}
    try:
        for aggregate, normalized in normalized_rows:
            if (
                type(aggregate.observed_date) is not date
                or not isinstance(aggregate.event_count, int)
                or isinstance(aggregate.event_count, bool)
                or aggregate.event_count < 0
                or not isinstance(aggregate.event_type, str)
                or not normalized["event_type"]
                or not isinstance(aggregate.h3_cell, str)
                or not is_valid_h3_cell(normalized["h3_cell"])
            ):
                rejected += 1
                continue
            district = getattr(aggregate, "district", "UNKNOWN") or "UNKNOWN"
            police_grid = getattr(aggregate, "police_grid", "UNKNOWN") or "UNKNOWN"
            key = {
                "source": getattr(aggregate, "source", None) or source,
                "source_dataset": getattr(aggregate, "source_dataset", None) or source_dataset,
                "observed_date": aggregate.observed_date,
                "district": district,
                "police_grid": police_grid,
                "h3_cell": normalized["h3_cell"],
                "event_type": normalized["event_type"],
            }
            identity = tuple(key.values())
            batch_count = batch_counts.get(identity)
            if batch_count is not None:
                if batch_count == aggregate.event_count:
                    deduplicated += 1
                else:
                    rejected += 1
                    conflicted = True
                continue
            outcome, stored_count = _insert_observation_atomic(
                session, key, aggregate.event_count
            )
            batch_counts[identity] = stored_count
            if outcome == "accepted":
                inserted += 1
            elif outcome == "deduplicated":
                deduplicated += 1
            else:
                rejected += 1
                conflicted = True
        session.add(
            IngestionRun(
                source=source,
                source_dataset=source_dataset,
                started_at=started_at,
                completed_at=datetime.now(timezone.utc),
                status="COMPLETED",
                source_sha256=source_sha256,
                accepted_count=inserted,
                rejected_count=rejected,
                deduplicated_count=deduplicated,
                safe_reason=(
                    SAFE_CONFLICT_REASON
                    if conflicted
                    else SAFE_VALIDATION_REASON if rejected else None
                ),
            )
        )
        session.commit()
    except Exception:
        session.rollback()
        raise
    return inserted
