"""
Security Scan API Routes
Ported from Node.js backend - VirusTotal URL/file hash scanning
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field, HttpUrl
from typing import Optional, Dict
from datetime import datetime, timezone
import httpx
import os
import logging

from services.data_ingestion.storage import store_cyber_threats

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/scan", tags=["Security Scan"])

VT_BASE = "https://www.virustotal.com/api/v3"


def _vt_headers() -> dict:
    api_key = os.getenv("VIRUSTOTAL_API_KEY")
    if not api_key:
        return {}
    return {"x-apikey": api_key}


def _check_vt_key():
    if not os.getenv("VIRUSTOTAL_API_KEY"):
        raise HTTPException(
            status_code=503,
            detail="Scanner service configuration unavailable"
        )


# --- Request/Response Models ---

class UrlScanRequest(BaseModel):
    url: HttpUrl = Field(..., description="HTTP(S) URL to scan")


class UrlScanResponse(BaseModel):
    success: bool
    analysisId: str
    message: str


class ScanResultResponse(BaseModel):
    success: bool
    analysisId: str
    status: str
    stats: Dict[str, int]
    positives: int
    total: int
    permalink: str


class FileHashRequest(BaseModel):
    hash: str = Field(
        ...,
        pattern=r"^(?:[A-Fa-f0-9]{32}|[A-Fa-f0-9]{40}|[A-Fa-f0-9]{64})$",
        description="MD5, SHA1, or SHA256 hash",
    )


class FileHashResponse(BaseModel):
    success: bool
    hash: str
    name: Optional[str] = None
    positives: int = 0
    total: int = 0
    stats: Dict[str, int] = {}
    reputation: int = 0
    permalink: str = ""
    notFound: bool = False
    message: Optional[str] = None


# --- Endpoints ---

@router.post("/url", response_model=UrlScanResponse)
async def scan_url(request: UrlScanRequest):
    """
    Submit a URL for VirusTotal scanning.

    Returns an analysisId — poll /api/v1/scan/result/{analysisId} for results.
    """
    _check_vt_key()

    if not request.url:
        raise HTTPException(status_code=400, detail="URL is required")

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                f"{VT_BASE}/urls",
                data={"url": str(request.url)},
                headers={
                    **_vt_headers(),
                    "Content-Type": "application/x-www-form-urlencoded"
                }
            )
            response.raise_for_status()
            data = response.json()
            analysis_id = data["data"]["id"]

        return UrlScanResponse(
            success=True,
            analysisId=analysis_id,
            message="URL submitted for scanning"
        )

    except httpx.HTTPStatusError as e:
        logger.error("VirusTotal submit failed with HTTP %s", e.response.status_code)
        raise HTTPException(status_code=500, detail="Failed to scan URL via VirusTotal")
    except Exception as e:
        logger.error(f"VirusTotal submit error: {e}")
        raise HTTPException(status_code=500, detail="Failed to scan URL via VirusTotal")


@router.get("/result/{analysis_id}", response_model=ScanResultResponse)
async def get_scan_result(analysis_id: str):
    """
    Poll VirusTotal for scan result of a previously submitted URL.

    Returns status (queued | in-progress | completed) and detection stats.
    """
    _check_vt_key()

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(
                f"{VT_BASE}/analyses/{analysis_id}",
                headers=_vt_headers()
            )
            response.raise_for_status()
            data = response.json()

        attributes = data["data"]["attributes"]
        status = attributes["status"]
        stats = attributes.get("stats", {})

        positives = stats.get("malicious", 0) + stats.get("suspicious", 0)
        total = sum(stats.values())

        if status == "completed" and positives > 0:
            now = datetime.now(timezone.utc)
            store_cyber_threats([{
                "provider": "VIRUSTOTAL",
                "external_id": analysis_id,
                "indicator_type": "url_analysis",
                "indicator_value": analysis_id,
                "title": "Malicious or suspicious URL analysis",
                "description": f"VirusTotal detections: {positives} of {total} engines.",
                "severity": "critical" if positives >= 10 else "high",
                "confidence": min(100, round((positives / max(total, 1)) * 100)),
                "active": True,
                "first_seen": now,
                "last_seen": now,
            }])

        return ScanResultResponse(
            success=True,
            analysisId=analysis_id,
            status=status,
            stats=stats,
            positives=positives,
            total=total,
            permalink=f"https://www.virustotal.com/gui/url/{analysis_id}"
        )

    except httpx.HTTPStatusError as e:
        logger.error("VirusTotal poll failed with HTTP %s", e.response.status_code)
        raise HTTPException(status_code=500, detail="Failed to retrieve scan result")
    except Exception as e:
        logger.error(f"VirusTotal poll error: {e}")
        raise HTTPException(status_code=500, detail="Failed to retrieve scan result")


@router.post("/file-hash", response_model=FileHashResponse)
async def check_file_hash(request: FileHashRequest):
    """
    Look up a file hash (MD5/SHA1/SHA256) in VirusTotal.

    Returns last known scan result for that hash.
    """
    _check_vt_key()

    if not request.hash:
        raise HTTPException(status_code=400, detail="hash is required")

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(
                f"{VT_BASE}/files/{request.hash}",
                headers=_vt_headers()
            )

            if response.status_code == 404:
                return FileHashResponse(
                    success=True,
                    hash=request.hash,
                    notFound=True,
                    message="Hash not in VirusTotal database"
                )

            response.raise_for_status()
            data = response.json()

        attributes = data["data"]["attributes"]
        stats = attributes.get("last_analysis_stats", {})
        positives = stats.get("malicious", 0) + stats.get("suspicious", 0)
        total = sum(stats.values())

        if positives > 0:
            now = datetime.now(timezone.utc)
            store_cyber_threats([{
                "provider": "VIRUSTOTAL",
                "external_id": request.hash,
                "indicator_type": "file_hash",
                "indicator_value": request.hash,
                "title": attributes.get("meaningful_name", "Detected malicious file"),
                "description": f"VirusTotal detections: {positives} of {total} engines.",
                "severity": "critical" if positives >= 10 else "high",
                "confidence": min(100, round((positives / max(total, 1)) * 100)),
                "active": True,
                "first_seen": now,
                "last_seen": now,
            }])

        return FileHashResponse(
            success=True,
            hash=request.hash,
            name=attributes.get("meaningful_name", request.hash),
            positives=positives,
            total=total,
            stats=stats,
            reputation=attributes.get("reputation", 0),
            permalink=f"https://www.virustotal.com/gui/file/{request.hash}"
        )

    except httpx.HTTPStatusError as e:
        logger.error("VirusTotal hash lookup failed with HTTP %s", e.response.status_code)
        raise HTTPException(status_code=500, detail="Failed to look up file hash")
    except Exception as e:
        logger.error(f"VirusTotal hash error: {e}")
        raise HTTPException(status_code=500, detail="Failed to look up file hash")
