"""Release-safe prediction routes.

Forecast Engine v2 remains unavailable until a durable, checksummed model
artifact and its point-in-time data evidence are loaded and verified.
"""

from datetime import date
from typing import List, Optional

from fastapi import APIRouter, Body, HTTPException, Query
from pydantic import BaseModel

from api.contracts.status import unavailable


router = APIRouter(prefix="/api/v1", tags=["Predictions"])


class LocationInput(BaseModel):
    lat: float
    lng: float


class PredictRiskRequest(BaseModel):
    locations: List[LocationInput]
    prediction_date: Optional[date] = None


def _forecast_unavailable() -> HTTPException:
    detail = unavailable("FORECAST_MODEL_NOT_VERIFIED").model_dump(mode="json")
    return HTTPException(status_code=503, detail=detail)


@router.post("/predict/risk")
async def predict_risk(request: PredictRiskRequest = Body(...)):
    """Reject prediction until the verified production artifact is available."""

    raise _forecast_unavailable()


@router.get("/predict/explain")
async def explain_prediction(
    h3_cell: str = Query(..., description="H3 cell ID"),
    prediction_date: Optional[date] = Query(None, description="Prediction date"),
):
    """Reject explanations until real inference and explanation evidence exists."""

    raise _forecast_unavailable()


@router.get("/predict/trends")
async def get_trend_forecast(
    h3_cell: str = Query(..., description="H3 cell ID"),
    days: int = Query(30, ge=7, le=90, description="Forecast duration in days"),
):
    """Reject time-series output until a verified forecast model exists."""

    raise _forecast_unavailable()
