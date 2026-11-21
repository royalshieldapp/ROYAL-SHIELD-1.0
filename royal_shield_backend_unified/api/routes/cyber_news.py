"""Curated cybersecurity advisories sourced from the official CISA KEV feed."""

from datetime import datetime, timedelta, timezone
from typing import List

import httpx
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field


router = APIRouter(prefix="/api/v1/cyber-news", tags=["Cyber News"])

CISA_KEV_URL = "https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json"
CISA_CATALOG_URL = "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
CACHE_TTL = timedelta(minutes=15)

_cache_payload: dict | None = None
_cache_expires_at: datetime | None = None


class CyberNewsItem(BaseModel):
    cve_id: str = Field(alias="cveId")
    vendor: str
    product: str
    title: str
    summary: str
    required_action: str = Field(alias="requiredAction")
    date_added: str = Field(alias="dateAdded")
    due_date: str = Field(alias="dueDate")
    known_ransomware_use: str = Field(alias="knownRansomwareUse")
    source_url: str = Field(alias="sourceUrl")

    model_config = {"populate_by_name": True}


class CyberNewsResponse(BaseModel):
    source: str
    catalog_version: str = Field(alias="catalogVersion")
    updated_at: str = Field(alias="updatedAt")
    items: List[CyberNewsItem]

    model_config = {"populate_by_name": True}


async def _get_cisa_feed() -> dict:
    global _cache_payload, _cache_expires_at

    now = datetime.now(timezone.utc)
    if _cache_payload is not None and _cache_expires_at is not None and now < _cache_expires_at:
        return _cache_payload

    try:
        async with httpx.AsyncClient(
            timeout=12.0,
            follow_redirects=True,
            headers={"User-Agent": "RoyalShield/1.2 (cyber-news)"},
        ) as client:
            response = await client.get(CISA_KEV_URL)
            response.raise_for_status()
            payload = response.json()
    except (httpx.HTTPError, ValueError) as exc:
        if _cache_payload is not None:
            return _cache_payload
        raise HTTPException(
            status_code=503,
            detail="Official cybersecurity advisory feed is temporarily unavailable",
        ) from exc

    if not isinstance(payload.get("vulnerabilities"), list):
        raise HTTPException(status_code=502, detail="Official advisory feed returned an invalid response")

    _cache_payload = payload
    _cache_expires_at = now + CACHE_TTL
    return payload


@router.get("", response_model=CyberNewsResponse, response_model_by_alias=True)
async def get_cyber_news(limit: int = Query(default=8, ge=1, le=20)):
    """Return the newest known-exploited vulnerabilities published by CISA."""

    payload = await _get_cisa_feed()
    vulnerabilities = sorted(
        payload["vulnerabilities"],
        key=lambda item: item.get("dateAdded", ""),
        reverse=True,
    )[:limit]

    items = [
        CyberNewsItem(
            cveId=item.get("cveID", "Unknown CVE"),
            vendor=item.get("vendorProject", "Unknown vendor"),
            product=item.get("product", "Unknown product"),
            title=item.get("vulnerabilityName", "Known exploited vulnerability"),
            summary=item.get("shortDescription", "No description provided by CISA."),
            requiredAction=item.get("requiredAction", "Review CISA guidance."),
            dateAdded=item.get("dateAdded", ""),
            dueDate=item.get("dueDate", ""),
            knownRansomwareUse=item.get("knownRansomwareCampaignUse", "Unknown"),
            sourceUrl=CISA_CATALOG_URL,
        )
        for item in vulnerabilities
    ]

    return CyberNewsResponse(
        source="CISA Known Exploited Vulnerabilities",
        catalogVersion=str(payload.get("catalogVersion", "unknown")),
        updatedAt=str(payload.get("dateReleased", "")),
        items=items,
    )
