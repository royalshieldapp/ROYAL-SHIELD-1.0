"""Forecast endpoints that never fabricate a risk prediction."""

from datetime import date, datetime, timedelta, timezone

import h3
from fastapi import APIRouter, Body, Depends, Query
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session
from sqlalchemy.exc import SQLAlchemyError

from api.schemas.forecast import raise_forecast_unavailable
from services.forecast.artifact_store import ArtifactStoreError, PostgresArtifactStore
from services.forecast.inference import ForecastInferenceService, InferenceUnavailableError
from services.geospatial.database.connection import get_db

router = APIRouter(prefix="/api/v1", tags=["Predictions"])


class LocationInput(BaseModel):
    lat: float = Field(..., ge=-90, le=90)
    lng: float = Field(..., ge=-180, le=180)


class PredictRiskRequest(BaseModel):
    locations: list[LocationInput] = Field(..., min_length=1, max_length=100)
    prediction_date: date | None = None


@router.post("/predict/risk")
async def predict_risk(
    request: PredictRiskRequest = Body(...),
    db: Session = Depends(get_db),
):
    """Run verified inference from one active durable artifact."""

    try:
        if not hasattr(db, "scalars"):
            raise_forecast_unavailable("No approved model artifact is available")
        artifact = PostgresArtifactStore(db).load_active()
        resolution = artifact.metrics.get("h3_resolution")
        if not isinstance(resolution, int) or not 0 <= resolution <= 15:
            raise InferenceUnavailableError("artifact H3 resolution is unavailable")
        as_of = date.today()
        prediction_date = request.prediction_date or (as_of + timedelta(days=7))
        horizon_days = (prediction_date - as_of).days
        if horizon_days < 1 or horizon_days > 90:
            from fastapi import HTTPException
            raise HTTPException(status_code=422, detail="prediction_date must be 1 to 90 days in the future")
        service = ForecastInferenceService(db)
        predictions = []
        cell_results = []
        for location in request.locations:
            h3_cell = h3.geo_to_h3(location.lat, location.lng, resolution)
            result = service.predict_cell(
                h3_cell,
                horizon_days=horizon_days,
                as_of=as_of,
                artifact=artifact,
            )
            cell_results.append(result)
            predictions.append({
                "location": {"lat": location.lat, "lng": location.lng},
                "h3_cell": result.h3_cell,
                "predicted_event_count": result.predicted_event_count,
                "predicted_risk_score": result.risk_score,
                "predicted_risk_level": (
                    "CRITICAL" if result.risk_score >= 75 else
                    "HIGH" if result.risk_score >= 50 else
                    "MEDIUM" if result.risk_score >= 25 else "LOW"
                ),
                "confidence": None,
                "prediction_date": prediction_date.isoformat(),
            })
        statuses = {item.status for item in cell_results}
        status = "DEGRADED" if "DEGRADED" in statuses else "READY"
        freshness = min(item.data_freshness for item in cell_results)
        sources = sorted({source for item in cell_results for source in item.sources})
        return {
            "status": status,
            "predictions": predictions,
            "total_requested": len(request.locations),
            "total_predicted": len(predictions),
            "prediction_date": prediction_date.isoformat(),
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "data_freshness": freshness.isoformat(),
            "sources": sources,
            "model_version": artifact.version,
            "confidence": None,
        }
    except ValueError:
        raise
    except (ArtifactStoreError, InferenceUnavailableError, SQLAlchemyError) as error:
        raise_forecast_unavailable(str(error))


@router.get("/predict/explain")
async def explain_prediction(
    h3_cell: str = Query(..., min_length=1, description="H3 cell ID"),
    prediction_date: date | None = Query(None, description="Prediction date"),
):
    """Reject explanation until it is generated from a verified active model."""

    del h3_cell, prediction_date
    raise_forecast_unavailable(
        "Forecast explanation requires an approved model artifact and verified data."
    )


@router.get("/predict/trends")
async def get_trend_forecast(
    h3_cell: str = Query(..., min_length=1, description="H3 cell ID"),
    days: int = Query(30, ge=7, le=90, description="Forecast duration in days"),
):
    """Reject time-series output until a verified forecast model exists."""

    del h3_cell, days
    raise_forecast_unavailable(
        "Forecast trend output requires an approved model artifact and verified data."
    )
