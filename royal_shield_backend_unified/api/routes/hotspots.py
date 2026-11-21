"""Hotspot endpoints derived from durable H3 aggregates and verified inference."""

from datetime import date, datetime, timezone
from typing import Optional

import h3
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from api.contracts.status import unavailable
from config.settings import settings
from services.forecast.artifact_store import ArtifactStoreError
from services.forecast.inference import InferenceUnavailableError
from services.forecast.presentation import load_forecast_presentation
from services.geospatial.database.connection import get_db


router = APIRouter(prefix="/api/v1", tags=["Hotspots"])


def _not_available(reason: str) -> None:
    raise HTTPException(
        status_code=503,
        detail=unavailable(reason).model_dump(mode="json"),
    )


def _bounds(
    min_lat: float | None,
    min_lng: float | None,
    max_lat: float | None,
    max_lng: float | None,
) -> tuple[float, float, float, float]:
    values = (min_lat, min_lng, max_lat, max_lng)
    if any(value is not None for value in values) and any(value is None for value in values):
        raise HTTPException(
            status_code=422,
            detail="all hotspot bounding-box values are required together",
        )
    return (
        settings.bbox_min_lat if min_lat is None else min_lat,
        settings.bbox_min_lng if min_lng is None else min_lng,
        settings.bbox_max_lat if max_lat is None else max_lat,
        settings.bbox_max_lng if max_lng is None else max_lng,
    )


def _hotspot_payload(
    db: Session,
    *,
    bbox_min_lat: float,
    bbox_min_lng: float,
    bbox_max_lat: float,
    bbox_max_lng: float,
    time_window_days: int,
    horizon_days: int,
    severity: str | None,
) -> dict:
    presentation = load_forecast_presentation(
        db,
        bbox_min_lat=bbox_min_lat,
        bbox_min_lng=bbox_min_lng,
        bbox_max_lat=bbox_max_lat,
        bbox_max_lng=bbox_max_lng,
        resolution=settings.default_h3_resolution,
        horizon_days=horizon_days,
        as_of=date.today(),
        time_window_days=time_window_days,
    )
    requested_severity = severity.upper() if severity else None
    active_cells = [cell for cell in presentation.cells if cell.window_events > 0]
    if not active_cells:
        active_cells = [cell for cell in presentation.cells if cell.total_events > 0]
    cells = [
        cell
        for cell in active_cells
        if (requested_severity is None or cell.risk_level == requested_severity)
    ]
    cells.sort(
        key=lambda cell: (
            cell.prediction.risk_score,
            cell.window_events if cell.window_events > 0 else cell.total_events,
        ),
        reverse=True,
    )
    hotspots = [
        {
            "hotspot_id": index,
            "center": {"lat": cell.center_lat, "lng": cell.center_lng},
            "h3_cell": cell.prediction.h3_cell,
            "radius_meters": float(
                h3.edge_length(settings.default_h3_resolution, "m")
            ),
            "event_count": cell.total_events,
            "event_types": cell.event_types,
            "severities": {},
            "risk_score": cell.prediction.risk_score,
            "risk_level": cell.risk_level,
        }
        for index, cell in enumerate(cells, start=1)
    ]
    return {
        "hotspots": hotspots,
        "total_count": len(hotspots),
        "metadata": {
            "clusters_found": len(hotspots),
            "total_events": sum(cell.total_events for cell in cells),
            "recent_events": sum(cell.window_events for cell in cells),
            "noise_points": 0,
            "time_window_days": time_window_days,
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "status": presentation.status,
            "data_freshness": presentation.data_freshness.isoformat(),
            "sources": list(presentation.sources),
            "model_version": presentation.artifact.version,
            "confidence": None,
            "method": "durable_h3_observations_plus_forecast_model",
        },
    }


@router.get("/hotspots")
async def get_hotspots(
    bbox_min_lat: Optional[float] = Query(None, ge=-90, le=90),
    bbox_min_lng: Optional[float] = Query(None, ge=-180, le=180),
    bbox_max_lat: Optional[float] = Query(None, ge=-90, le=90),
    bbox_max_lng: Optional[float] = Query(None, ge=-180, le=180),
    hotspot_type: Optional[str] = Query(None, description="crime | fire | all"),
    severity: Optional[str] = Query(None, description="low | medium | high | critical"),
    time_window_days: int = Query(30, ge=1, le=90),
    db: Session = Depends(get_db),
):
    if hotspot_type not in (None, "all"):
        _not_available("HOTSPOT_CATEGORY_NOT_VERIFIED")
    try:
        bounds = _bounds(bbox_min_lat, bbox_min_lng, bbox_max_lat, bbox_max_lng)
        return _hotspot_payload(
            db,
            bbox_min_lat=bounds[0],
            bbox_min_lng=bounds[1],
            bbox_max_lat=bounds[2],
            bbox_max_lng=bounds[3],
            time_window_days=time_window_days,
            horizon_days=7,
            severity=severity,
        )
    except HTTPException:
        raise
    except ValueError as error:
        raise HTTPException(status_code=422, detail=str(error)) from error
    except (ArtifactStoreError, InferenceUnavailableError):
        _not_available("HOTSPOT_DATA_NOT_VERIFIED")


@router.get("/hotspots/predict")
async def predict_hotspots(
    bbox_min_lat: Optional[float] = Query(None, ge=-90, le=90),
    bbox_min_lng: Optional[float] = Query(None, ge=-180, le=180),
    bbox_max_lat: Optional[float] = Query(None, ge=-90, le=90),
    bbox_max_lng: Optional[float] = Query(None, ge=-180, le=180),
    days_ahead: int = Query(7, ge=1, le=30),
    db: Session = Depends(get_db),
):
    try:
        bounds = _bounds(bbox_min_lat, bbox_min_lng, bbox_max_lat, bbox_max_lng)
        return _hotspot_payload(
            db,
            bbox_min_lat=bounds[0],
            bbox_min_lng=bounds[1],
            bbox_max_lat=bounds[2],
            bbox_max_lng=bounds[3],
            time_window_days=30,
            horizon_days=days_ahead,
            severity=None,
        )
    except HTTPException:
        raise
    except ValueError as error:
        raise HTTPException(status_code=422, detail=str(error)) from error
    except (ArtifactStoreError, InferenceUnavailableError):
        _not_available("FORECAST_MODEL_NOT_VERIFIED")


@router.get("/hotspots/nearby")
async def get_nearby_hotspots(
    lat: float = Query(..., ge=-90, le=90),
    lng: float = Query(..., ge=-180, le=180),
    radius_meters: float = Query(1000, ge=100, le=10000),
):
    del lat, lng, radius_meters
    _not_available("HOTSPOT_DATA_NOT_VERIFIED")
