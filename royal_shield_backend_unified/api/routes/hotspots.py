"""Release-safe hotspot endpoints.

Hotspot results remain unavailable until a durable, verified data source and
the forecast model are validated end to end. Returning empty or fixed values
would falsely imply a successful analysis.
"""

from typing import Optional

from fastapi import APIRouter, HTTPException, Query

from api.contracts.status import unavailable


router = APIRouter(prefix="/api/v1", tags=["Hotspots"])


def _not_available(reason: str) -> None:
    raise HTTPException(
        status_code=503,
        detail=unavailable(reason).model_dump(mode="json"),
    )


@router.get("/hotspots")
async def get_hotspots(
    bbox_min_lat: Optional[float] = Query(None),
    bbox_min_lng: Optional[float] = Query(None),
    bbox_max_lat: Optional[float] = Query(None),
    bbox_max_lng: Optional[float] = Query(None),
    hotspot_type: Optional[str] = Query(None, description="crime | fire | all"),
    severity: Optional[str] = Query(None, description="low | medium | high | critical"),
    time_window_days: int = Query(30, ge=1, le=90),
):
    _not_available("HOTSPOT_DATA_NOT_VERIFIED")


@router.get("/hotspots/predict")
async def predict_hotspots(
    bbox_min_lat: Optional[float] = Query(None),
    bbox_min_lng: Optional[float] = Query(None),
    bbox_max_lat: Optional[float] = Query(None),
    bbox_max_lng: Optional[float] = Query(None),
    days_ahead: int = Query(7, ge=1, le=30),
):
    _not_available("FORECAST_MODEL_NOT_VERIFIED")


@router.get("/hotspots/nearby")
async def get_nearby_hotspots(
    lat: float = Query(..., ge=-90, le=90),
    lng: float = Query(..., ge=-180, le=180),
    radius_meters: float = Query(1000, ge=100, le=10000),
):
    _not_available("HOTSPOT_DATA_NOT_VERIFIED")
