"""Truthful availability responses for the geospatial forecast API."""

from datetime import datetime, timezone
from typing import Any, Literal, NoReturn

from fastapi import HTTPException
from pydantic import BaseModel, Field


class ForecastAvailability(BaseModel):
    """Metadata that must accompany every forecast availability result."""

    status: Literal["READY", "DEGRADED", "UNAVAILABLE"]
    generated_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    data_freshness: datetime | None = None
    sources: list[str] = Field(default_factory=list)
    model_version: str | None = None
    confidence: float | None = Field(default=None, ge=0, le=1)
    reason: str | None = None
    data: Any = None


def raise_forecast_unavailable(internal_reason: str = "FORECAST_MODEL_NOT_VERIFIED") -> NoReturn:
    """Return a stable public 503 response without exposing internal details."""

    del internal_reason
    payload = ForecastAvailability(
        status="UNAVAILABLE",
        reason="FORECAST_MODEL_NOT_VERIFIED",
        data=None,
    )
    raise HTTPException(status_code=503, detail=payload.model_dump(mode="json"))
