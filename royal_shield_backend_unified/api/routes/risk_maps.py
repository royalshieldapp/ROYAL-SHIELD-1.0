"""Release-safe risk-map endpoints.

The former implementation returned fabricated example scores and histories.
These routes now fail closed until a durable source and its freshness can be
verified end to end.
"""

from datetime import date
from typing import Optional

from fastapi import APIRouter, HTTPException, Query

from api.contracts.status import unavailable


router = APIRouter(prefix="/api/v1", tags=["Risk Maps"])


def _risk_map_unavailable() -> None:
    raise HTTPException(
        status_code=503,
        detail=unavailable("RISK_MAP_DATA_NOT_VERIFIED").model_dump(mode="json"),
    )


@router.get("/risk-map")
async def get_risk_map(
    bbox_min_lat: float = Query(..., ge=-90, le=90),
    bbox_min_lng: float = Query(..., ge=-180, le=180),
    bbox_max_lat: float = Query(..., ge=-90, le=90),
    bbox_max_lng: float = Query(..., ge=-180, le=180),
    resolution: int = Query(9, ge=7, le=12),
    date_filter: Optional[date] = Query(None),
):
    _risk_map_unavailable()


@router.get("/risk-zones/{h3_cell}")
async def get_zone_details(h3_cell: str):
    _risk_map_unavailable()


@router.get("/risk-history")
async def get_risk_history(
    h3_cell: str = Query(...),
    start_date: date = Query(...),
    end_date: date = Query(...),
):
    _risk_map_unavailable()
