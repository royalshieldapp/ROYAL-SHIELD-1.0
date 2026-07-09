from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field
from typing import List, Optional
import logging
import random
from datetime import datetime, timedelta
from config.settings import settings

router = APIRouter(prefix="/api/vpn", tags=["VPN"])
logger = logging.getLogger(__name__)

class VpnConfigPayload(BaseModel):
    serverId: str
    userId: str
    publicKey: str

@router.get("/servers")
def get_servers():
    vpn_provider = settings.vpn_provider

    if not vpn_provider:
        return {
            "success": True,
            "status": "not_configured",
            "code": "VPN_NOT_CONFIGURED",
            "message": "VPN provider not configured. Set VPN_PROVIDER and server details in environment.",
            "servers": []
        }

    servers = []
    servers_env = settings.vpn_servers

    if servers_env:
        for s in servers_env.split(','):
            parts = s.split(':')
            if len(parts) >= 3:
                s_id, s_name, s_host = parts[0], parts[1], parts[2]
                servers.append({
                    "id": s_id.strip(),
                    "name": s_name.strip(),
                    "host": s_host.strip(),
                    "port": settings.vpn_port,
                    "protocol": vpn_provider,
                    "status": "available",
                    "load": random.randint(10, 70)  # Simulated load %
                })

    return {
        "success": True,
        "status": "available" if servers else "no_servers",
        "provider": vpn_provider,
        "servers": servers
    }

@router.post("/config")
def get_vpn_config(payload: VpnConfigPayload):
    vpn_provider = settings.vpn_provider
    server_public_key = settings.vpn_server_public_key

    if not vpn_provider or not server_public_key:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "error": "VPN service not configured",
                "code": "VPN_NOT_CONFIGURED",
                "message": "VPN server keys not set. Configure VPN_SERVER_PUBLIC_KEY in environment."
            }
        )

    # Find host for requested serverId
    servers_env = settings.vpn_servers
    host = None
    if servers_env:
        for s in servers_env.split(','):
            parts = s.split(':')
            if len(parts) >= 3 and parts[0].strip() == payload.serverId:
                host = parts[2].strip()
                break

    if not host:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Server not found: {payload.serverId}"
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
                "publicKey": server_public_key,
                "endpoint": f"{host}:{settings.vpn_port}",
                "allowedIPs": "0.0.0.0/0, ::/0",
                "persistentKeepalive": 25
            }
        },
        "serverId": payload.serverId,
        "provider": vpn_provider,
        "expiresAt": expires_at
    }

@router.get("/status")
def get_vpn_status():
    vpn_provider = settings.vpn_provider
    configured = vpn_provider is not None and settings.vpn_server_public_key is not None

    return {
        "service": "vpn",
        "status": "available" if configured else "not_configured",
        "provider": vpn_provider
    }
