"""Read-only collectors for reputable cyber threat-intelligence providers."""
from datetime import datetime, timezone
from typing import Any, Dict, List

import httpx

from config.settings import settings
from services.data_ingestion.storage import store_cyber_threats

CISA_KEV_URL = "https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json"
OTX_SUBSCRIBED_URL = "https://otx.alienvault.com/api/v1/pulses/subscribed"
ABUSEIPDB_BLACKLIST_URL = "https://api.abuseipdb.com/api/v2/blacklist"


def _parse_time(value: Any, fallback: datetime | None = None) -> datetime:
    if isinstance(value, datetime):
        parsed = value
    elif value:
        text = str(value)
        if len(text) == 10:
            text += "T00:00:00+00:00"
        parsed = datetime.fromisoformat(text.replace("Z", "+00:00"))
    else:
        parsed = fallback or datetime.now(timezone.utc)
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)


async def collect_cisa_kev(client: httpx.AsyncClient, limit: int) -> List[Dict[str, Any]]:
    response = await client.get(CISA_KEV_URL)
    response.raise_for_status()
    payload = response.json()
    vulnerabilities = payload.get("vulnerabilities")
    if not isinstance(vulnerabilities, list):
        raise ValueError("CISA KEV returned an invalid payload")
    records = []
    for item in vulnerabilities[:limit]:
        cve = item.get("cveID")
        if not cve:
            continue
        ransomware = str(item.get("knownRansomwareCampaignUse", "")).lower() == "known"
        records.append({
            "provider": "CISA", "external_id": cve, "indicator_type": "cve",
            "indicator_value": cve, "title": item.get("vulnerabilityName") or cve,
            "description": item.get("shortDescription") or "",
            "severity": "critical" if ransomware else "high", "confidence": 100,
            "active": True, "first_seen": _parse_time(item.get("dateAdded")),
            "last_seen": _parse_time(payload.get("dateReleased")),
        })
    return records


async def collect_otx(client: httpx.AsyncClient, limit: int) -> List[Dict[str, Any]]:
    if not settings.otx_api_key:
        return []
    response = await client.get(
        OTX_SUBSCRIBED_URL,
        params={"limit": min(limit, 100), "page": 1},
        headers={"X-OTX-API-KEY": settings.otx_api_key},
    )
    response.raise_for_status()
    records = []
    for pulse in response.json().get("results", []):
        pulse_id = str(pulse.get("id") or "")
        modified = _parse_time(pulse.get("modified") or pulse.get("created"))
        for indicator in pulse.get("indicators") or []:
            indicator_id = str(indicator.get("id") or indicator.get("indicator") or "")
            value = indicator.get("indicator")
            if not pulse_id or not indicator_id or not value:
                continue
            records.append({
                "provider": "OTX", "external_id": f"{pulse_id}:{indicator_id}",
                "indicator_type": indicator.get("type") or "unknown", "indicator_value": value,
                "title": pulse.get("name") or "AlienVault OTX indicator",
                "description": pulse.get("description") or "",
                "severity": "high" if pulse.get("malware_families") else "medium",
                "confidence": 75, "active": indicator.get("is_active", 1) not in (0, False),
                "first_seen": _parse_time(indicator.get("created"), modified), "last_seen": modified,
            })
            if len(records) >= limit:
                return records
    return records


async def collect_abuseipdb(client: httpx.AsyncClient, limit: int) -> List[Dict[str, Any]]:
    if not settings.abuseipdb_api_key:
        return []
    response = await client.get(
        ABUSEIPDB_BLACKLIST_URL,
        params={"limit": min(limit, 1000)},
        headers={"Key": settings.abuseipdb_api_key, "Accept": "application/json"},
    )
    response.raise_for_status()
    records = []
    for item in response.json().get("data", []):
        ip = item.get("ipAddress")
        if not ip:
            continue
        confidence = int(item.get("abuseConfidenceScore") or 0)
        seen = _parse_time(item.get("lastReportedAt"))
        records.append({
            "provider": "ABUSEIPDB", "external_id": ip, "indicator_type": "ip",
            "indicator_value": ip, "title": "Reported abusive IP address",
            "description": "Reputation indicator supplied by AbuseIPDB.",
            "severity": "critical" if confidence >= 95 else "high",
            "confidence": confidence, "active": True, "first_seen": seen, "last_seen": seen,
        })
    return records


async def refresh_cyber_intelligence() -> Dict[str, Dict[str, Any]]:
    """Refresh providers independently so one outage cannot block the others."""
    limit = max(1, min(settings.cyber_feed_limit, 5000))
    collectors = {"cisa": collect_cisa_kev, "otx": collect_otx, "abuseipdb": collect_abuseipdb}
    results: Dict[str, Dict[str, Any]] = {}
    async with httpx.AsyncClient(timeout=30.0, follow_redirects=True, headers={"User-Agent": "RoyalShield/2.0 threat-intelligence"}) as client:
        for name, collector in collectors.items():
            try:
                records = await collector(client, limit)
                results[name] = {"status": "ok", "fetched": len(records), **store_cyber_threats(records)}
            except (httpx.HTTPError, ValueError) as exc:
                results[name] = {"status": "error", "error_type": type(exc).__name__}
    return results
