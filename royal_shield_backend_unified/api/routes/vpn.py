from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field
import logging
from datetime import datetime, timedelta
from config.settings import settings

router = APIRouter(prefix="/api/vpn", tags=["VPN"])
logger = logging.getLogger(__name__)

class VpnConfigPayload(BaseModel):
    serverId: str
    userId: str
    publicKey: str


def _parse_servers():
    servers = []
    if not settings.vpn_servers:
        return servers

    for raw_server in settings.vpn_servers.split(","):
        parts = [part.strip() for part in raw_server.split(":")]
        if len(parts) < 3:
            logger.warning("Ignoring invalid VPN server entry")
            continue

        server_id, name, host = parts[:3]
        if not server_id or not host:
            logger.warning("Ignoring incomplete VPN server entry")
            continue

        servers.append({
            "id": server_id,
            "name": name or server_id,
            "countryCode": server_id.split("-")[0].upper(),
            "host": host,
            "port": settings.vpn_port,
            "protocol": settings.vpn_provider,
            "status": "available",
            "load": None,
        })

    return servers


def _vpn_status_payload():
    has_base_config = bool(
        settings.vpn_provider
        and settings.vpn_server_public_key
        and settings.vpn_servers
    )
    can_issue_config = settings.vpn_allow_static_config
    configured = has_base_config and can_issue_config

    return {
        "service": "vpn",
        "status": "available" if configured else (
            "peer_registration_not_configured" if has_base_config else "not_configured"
        ),
        "code": "VPN_AVAILABLE" if configured else (
            "VPN_PEER_REGISTRATION_NOT_CONFIGURED" if has_base_config else "VPN_NOT_CONFIGURED"
        ),
        "message": "VPN service available." if configured else (
            "VPN server variables exist, but profile issuing is not enabled."
            if has_base_config
            else "VPN provider not configured. Set VPN_PROVIDER, VPN_SERVER_PUBLIC_KEY, and VPN_SERVERS."
        ),
        "provider": settings.vpn_provider,
        "staticConfigAllowed": can_issue_config,
    }


@router.get("/servers")
def get_servers():
    status_payload = _vpn_status_payload()

    if status_payload["status"] != "available":
        return {
            "success": True,
            **status_payload,
            "servers": [],
        }

    servers = _parse_servers()

    return {
        "success": True,
        "status": "available" if servers else "no_servers",
        "provider": settings.vpn_provider,
        "servers": servers
    }

@router.post("/config")
def get_vpn_config(payload: VpnConfigPayload):
    status_payload = _vpn_status_payload()

    if status_payload["status"] == "not_configured":
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "error": "VPN service not configured",
                "code": "VPN_NOT_CONFIGURED",
                "message": status_payload["message"],
            }
        )

    if status_payload["status"] == "peer_registration_not_configured":
        raise HTTPException(
            status_code=status.HTTP_501_NOT_IMPLEMENTED,
            detail={
                "error": "VPN peer registration not configured",
                "code": "VPN_PEER_REGISTRATION_NOT_CONFIGURED",
                "message": status_payload["message"],
            }
        )

    server = next((item for item in _parse_servers() if item["id"] == payload.serverId), None)

    if not server:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={
                "error": "VPN server not found",
                "code": "VPN_SERVER_NOT_FOUND",
                "message": f"Server not found: {payload.serverId}",
            }
        )

    expires_at = (datetime.utcnow() + timedelta(days=1)).isoformat() + "Z"

    return {
        "success": True,
        "config": {
            "interface": {
                "address": "10.0.0.2/32",  # Assigned dynamically in prod
                "dns": settings.vpn_dns,
                "mtu": 1420
            },
            "peer": {
                "publicKey": settings.vpn_server_public_key,
                "endpoint": f"{server['host']}:{settings.vpn_port}",
                "allowedIPs": "0.0.0.0/0, ::/0",
                "persistentKeepalive": 25
            }
        },
        "serverId": payload.serverId,
        "provider": settings.vpn_provider,
        "expiresAt": expires_at
    }

@router.get("/status")
def get_vpn_status():
    return _vpn_status_payload()
