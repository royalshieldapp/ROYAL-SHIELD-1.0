"""Fail-closed availability for advanced release features."""

from fastapi import APIRouter
from pydantic import BaseModel

from api.contracts.status import AvailabilityResponse, unavailable


router = APIRouter(prefix="/api/v1/features", tags=["Feature Availability"])


class AdvancedFeatureStatusResponse(BaseModel):
    vpn: AvailabilityResponse
    forecast: AvailabilityResponse
    threatMap: AvailabilityResponse
    automations: AvailabilityResponse
    reports: AvailabilityResponse
    policies: AvailabilityResponse


@router.get("/advanced-status", response_model=AdvancedFeatureStatusResponse)
async def get_advanced_feature_status() -> AdvancedFeatureStatusResponse:
    """Return release-safe states until durable E2E evidence exists."""

    return AdvancedFeatureStatusResponse(
        vpn=unavailable("VPN_E2E_NOT_VERIFIED"),
        forecast=unavailable("FORECAST_MODEL_NOT_VERIFIED"),
        threatMap=unavailable("THREAT_DATA_NOT_VERIFIED"),
        automations=unavailable("AUTOMATION_ACTIONS_NOT_VERIFIED"),
        reports=unavailable("REPORT_SOURCES_NOT_VERIFIED"),
        policies=unavailable("POLICY_ENFORCEMENT_NOT_VERIFIED"),
    )
