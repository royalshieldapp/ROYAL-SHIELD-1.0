"""Truthful availability contract shared by provider-backed routes."""

from datetime import datetime, timezone
from typing import Any, Literal, TypeAlias

from pydantic import BaseModel, Field


AvailabilityStatus: TypeAlias = Literal["READY", "DEGRADED", "UNAVAILABLE"]


class AvailabilityResponse(BaseModel):
    status: AvailabilityStatus
    generatedAt: datetime
    dataFreshness: datetime | None = None
    sources: list[str] = Field(default_factory=list)
    modelVersion: str | None = None
    confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    reason: str | None = None
    data: dict[str, Any] | None = None


def _now_utc() -> datetime:
    return datetime.now(timezone.utc)


def ready(
    *,
    data: dict[str, Any],
    sources: list[str],
    data_freshness: datetime,
    model_version: str | None = None,
    confidence: float | None = None,
) -> AvailabilityResponse:
    return AvailabilityResponse(
        status="READY",
        generatedAt=_now_utc(),
        dataFreshness=data_freshness,
        sources=list(sources),
        modelVersion=model_version,
        confidence=confidence,
        data=data,
    )


def degraded(
    reason: str,
    *,
    data: dict[str, Any] | None = None,
    sources: list[str] | None = None,
    data_freshness: datetime | None = None,
    model_version: str | None = None,
    confidence: float | None = None,
) -> AvailabilityResponse:
    return AvailabilityResponse(
        status="DEGRADED",
        generatedAt=_now_utc(),
        dataFreshness=data_freshness,
        sources=list(sources or []),
        modelVersion=model_version,
        confidence=confidence,
        reason=reason,
        data=data,
    )


def unavailable(reason: str, source: str | None = None) -> AvailabilityResponse:
    return AvailabilityResponse(
        status="UNAVAILABLE",
        generatedAt=_now_utc(),
        sources=[] if source is None else [source],
        reason=reason,
    )
