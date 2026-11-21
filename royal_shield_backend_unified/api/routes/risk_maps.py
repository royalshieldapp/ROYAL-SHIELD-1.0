"""Risk-map endpoints backed by durable observations and verified inference."""

from datetime import date, datetime, timezone
from typing import Optional

import h3
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from api.contracts.status import unavailable
from services.forecast.artifact_store import ArtifactStoreError
from services.forecast.inference import InferenceUnavailableError
from services.forecast.presentation import (
    ForecastCellPresentation,
    load_forecast_presentation,
)
from services.geospatial.database.connection import get_db


router = APIRouter(prefix="/api/v1", tags=["Risk Maps"])


def _risk_map_unavailable() -> None:
    raise HTTPException(
        status_code=503,
        detail=unavailable("RISK_MAP_DATA_NOT_VERIFIED").model_dump(mode="json"),
    )


def _horizon_days(prediction_date: date | None) -> int:
    if prediction_date is None:
        return 7
    horizon = (prediction_date - date.today()).days
    if not 1 <= horizon <= 90:
        raise HTTPException(
            status_code=422,
            detail="date_filter must be 1 to 90 days in the future",
        )
    return horizon


def _closed_geojson_boundary(cell: ForecastCellPresentation) -> list[list[float]]:
    coordinates = [[lng, lat] for lat, lng in cell.boundary]
    if coordinates and coordinates[0] != coordinates[-1]:
        coordinates.append(coordinates[0])
    return coordinates


def _change(current: int, previous: int) -> tuple[str, str]:
    if previous == 0:
        return ("0%", "STABLE") if current == 0 else ("NOT_COMPARABLE", "RISING")
    percent = ((current - previous) / previous) * 100.0
    direction = "RISING" if percent > 0 else "FALLING" if percent < 0 else "STABLE"
    return f"{percent:+.1f}%", direction


@router.get("/risk-map")
async def get_risk_map(
    bbox_min_lat: float = Query(..., ge=-90, le=90),
    bbox_min_lng: float = Query(..., ge=-180, le=180),
    bbox_max_lat: float = Query(..., ge=-90, le=90),
    bbox_max_lng: float = Query(..., ge=-180, le=180),
    resolution: int = Query(9, ge=7, le=12),
    date_filter: Optional[date] = Query(None),
    db: Session = Depends(get_db),
):
    try:
        presentation = load_forecast_presentation(
            db,
            bbox_min_lat=bbox_min_lat,
            bbox_min_lng=bbox_min_lng,
            bbox_max_lat=bbox_max_lat,
            bbox_max_lng=bbox_max_lng,
            resolution=resolution,
            horizon_days=_horizon_days(date_filter),
            as_of=date.today(),
        )
    except HTTPException:
        raise
    except ValueError as error:
        raise HTTPException(status_code=422, detail=str(error)) from error
    except (ArtifactStoreError, InferenceUnavailableError):
        _risk_map_unavailable()

    features = [
        {
            "type": "Feature",
            "geometry": {
                "type": "Polygon",
                "coordinates": [_closed_geojson_boundary(cell)],
            },
            "properties": {
                "h3_cell": cell.prediction.h3_cell,
                "risk_score": cell.prediction.risk_score,
                "risk_level": cell.risk_level,
                "event_count": cell.total_events,
                "crime_count": 0,
                "fire_count": 0,
                "recent_7d": cell.recent_7d,
                "recent_30d": cell.recent_30d,
                "data_kind": "PREDICTED_RISK_WITH_OBSERVED_COUNTS",
            },
        }
        for cell in presentation.cells
    ]
    return {
        "type": "FeatureCollection",
        "features": features,
        "metadata": {
            "bbox": {
                "min_lat": bbox_min_lat,
                "min_lng": bbox_min_lng,
                "max_lat": bbox_max_lat,
                "max_lng": bbox_max_lng,
            },
            "resolution": resolution,
            "zone_count": len(features),
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "status": presentation.status,
            "data_freshness": presentation.data_freshness.isoformat(),
            "sources": list(presentation.sources),
            "model_version": presentation.artifact.version,
            "confidence": None,
        },
    }


@router.get("/risk-zones/{h3_cell}")
async def get_zone_details(
    h3_cell: str,
    db: Session = Depends(get_db),
):
    if not h3.h3_is_valid(h3_cell):
        raise HTTPException(status_code=422, detail="h3_cell is invalid")
    resolution = h3.h3_get_resolution(h3_cell)
    center_lat, center_lng = h3.h3_to_geo(h3_cell)
    try:
        presentation = load_forecast_presentation(
            db,
            bbox_min_lat=center_lat - 0.000001,
            bbox_min_lng=center_lng - 0.000001,
            bbox_max_lat=center_lat + 0.000001,
            bbox_max_lng=center_lng + 0.000001,
            resolution=resolution,
            horizon_days=7,
            as_of=date.today(),
        )
        cell = next(
            item for item in presentation.cells if item.prediction.h3_cell == h3_cell
        )
    except (ArtifactStoreError, InferenceUnavailableError, StopIteration):
        _risk_map_unavailable()

    change_7d, direction_7d = _change(cell.recent_7d, cell.previous_7d)
    change_30d, direction_30d = _change(cell.recent_30d, cell.previous_30d)
    direction = direction_7d if direction_7d != "STABLE" else direction_30d
    return {
        "h3_cell": h3_cell,
        "center": [cell.center_lat, cell.center_lng],
        "risk_score": cell.prediction.risk_score,
        "risk_level": cell.risk_level,
        "statistics": {
            "total_events": cell.total_events,
            "crime_events": 0,
            "fire_events": 0,
            "osint_events": None,
            "recent_7d": cell.recent_7d,
            "recent_30d": cell.recent_30d,
            "severity_critical": 0,
            "severity_high": 0,
            "severity_medium": 0,
            "severity_low": 0,
        },
        "recent_events": [],
        "trends": {
            "7d_change": change_7d,
            "30d_change": change_30d,
            "direction": direction,
        },
        "metadata": {
            "status": cell.prediction.status,
            "observation_kind": "OBSERVED_DATA",
            "risk_kind": "PREDICTED_RISK",
            "data_freshness": cell.prediction.data_freshness.isoformat(),
            "sources": list(cell.prediction.sources),
            "model_version": cell.prediction.model_version,
            "confidence": None,
        },
    }


@router.get("/risk-history")
async def get_risk_history(
    h3_cell: str = Query(...),
    start_date: date = Query(...),
    end_date: date = Query(...),
):
    del h3_cell, start_date, end_date
    _risk_map_unavailable()
