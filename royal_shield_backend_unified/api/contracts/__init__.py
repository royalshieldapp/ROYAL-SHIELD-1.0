"""Shared API contracts for truthful provider availability."""

from .status import (
    AvailabilityResponse,
    AvailabilityStatus,
    degraded,
    ready,
    unavailable,
)

__all__ = [
    "AvailabilityResponse",
    "AvailabilityStatus",
    "degraded",
    "ready",
    "unavailable",
]
