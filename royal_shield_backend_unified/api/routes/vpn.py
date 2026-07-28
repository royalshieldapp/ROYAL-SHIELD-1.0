from __future__ import annotations

import base64
import hashlib
import hmac
import json
import logging
import secrets
import time
from datetime import datetime, timedelta

import httpx
from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field, field_validator

from config.settings import settings

router = APIRouter(prefix="/api/vpn", tags=["VPN"])
logger = logging.getLogger(__name__)


class VpnConfigPayload(BaseModel):
    serverId: str = Field(min_length=1, max_length=64, pattern=r"^[a-zA-Z0-9_-]+$")
    userId: str = Field(min_length=8, max_length=128, pattern=r"^[a-zA-Z0-9_-]+$")
    publicKey: str = Field(min_length=44, max_length=44)

    @field_validator("publicKey")
    @classmethod
    def validate_wireguard_public_key(cls, value: str) -> str:
        try:
            decoded = base64.b64decode(value, validate=True)
        except ValueError as exc:
            raise ValueError("Invalid WireGuard public key") from exc
        if len(decoded) != 32:
            raise ValueError("Invalid WireGuard public key")
        return value


def _parse_servers() -> list[dict]:
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


def _control_plane_configured() -> bool:
    return bool(settings.vpn_control_url and settings.vpn_control_secret)


def _vpn_status_payload() -> dict:
    has_base_config = bool(settings.vpn_provider and settings.vpn_server_public_key and settings.vpn_servers)
    has_control_plane = _control_plane_configured()
    configured = has_base_config and has_control_plane
    if configured:
        state, code, message = "available", "VPN_AVAILABLE", "VPN service available."
    elif has_base_config:
        state = "peer_registration_not_configured"
        code = "VPN_PEER_REGISTRATION_NOT_CONFIGURED"
        message = "VPN server exists, but its authenticated peer control plane is not configured."
    else:
        state = "not_configured"
        code = "VPN_NOT_CONFIGURED"
        message = "VPN provider not configured. Set VPN_PROVIDER, VPN_SERVER_PUBLIC_KEY, and VPN_SERVERS."
    return {
        "service": "vpn",
        "status": state,
        "code": code,
        "message": message,
        "provider": settings.vpn_provider,
        "peerRegistrationEnabled": has_control_plane,
    }


def _signed_control_headers(body: bytes) -> dict[str, str]:
    timestamp = str(int(time.time()))
    nonce = secrets.token_hex(16)
    signed = timestamp.encode() + b"\n" + nonce.encode() + b"\n" + body
    signature = hmac.new(settings.vpn_control_secret.encode(), signed, hashlib.sha256).hexdigest()
    return {
        "Content-Type": "application/json",
        "X-Royal-Timestamp": timestamp,
        "X-Royal-Nonce": nonce,
        "X-Royal-Signature": signature,
    }


async def _register_peer(payload: VpnConfigPayload) -> str:
    request_payload = {"serverId": payload.serverId, "userId": payload.userId, "publicKey": payload.publicKey}
    body = json.dumps(request_payload, separators=(",", ":"), sort_keys=True).encode()
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(
                settings.vpn_control_url.rstrip("/") + "/v1/peers",
                content=body,
                headers=_signed_control_headers(body),
            )
            response.raise_for_status()
            result = response.json()
    except (httpx.HTTPError, ValueError) as exc:
        logger.error("VPN peer registration failed: %s", type(exc).__name__)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "error": "VPN peer registration unavailable",
                "code": "VPN_CONTROL_UNAVAILABLE",
                "message": "The VPN control plane could not register this device.",
            },
        ) from exc
    address = str(result.get("address", ""))
    if not address.endswith("/32"):
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail={
                "error": "Invalid VPN control response",
                "code": "VPN_CONTROL_INVALID_RESPONSE",
                "message": "The VPN control plane returned an invalid client address.",
            },
        )
    return address


@router.get("/servers")
def get_servers():
    status_payload = _vpn_status_payload()
    if status_payload["status"] != "available":
        return {"success": True, **status_payload, "servers": []}
    servers = _parse_servers()
    return {
        "success": True,
        "status": "available" if servers else "no_servers",
        "provider": settings.vpn_provider,
        "servers": servers,
    }


@router.post("/config")
async def get_vpn_config(payload: VpnConfigPayload):
    status_payload = _vpn_status_payload()
    if status_payload["status"] != "available":
        http_status = status.HTTP_503_SERVICE_UNAVAILABLE if status_payload["status"] == "not_configured" else status.HTTP_501_NOT_IMPLEMENTED
        raise HTTPException(
            status_code=http_status,
            detail={
                "error": "VPN service unavailable",
                "code": status_payload["code"],
                "message": status_payload["message"],
            },
        )
    server = next((item for item in _parse_servers() if item["id"] == payload.serverId), None)
    if not server:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={
                "error": "VPN server not found",
                "code": "VPN_SERVER_NOT_FOUND",
                "message": "The requested VPN server does not exist.",
            },
        )
    client_address = await _register_peer(payload)
    expires_at = (datetime.utcnow() + timedelta(days=1)).isoformat() + "Z"
    return {
        "success": True,
        "config": {
            "interface": {"address": client_address, "dns": settings.vpn_dns, "mtu": 1420},
            "peer": {
                "publicKey": settings.vpn_server_public_key,
                "endpoint": f"{server['host']}:{settings.vpn_port}",
                "allowedIPs": "0.0.0.0/0, ::/0",
                "persistentKeepalive": 25,
            },
        },
        "serverId": payload.serverId,
        "provider": settings.vpn_provider,
        "expiresAt": expires_at,
    }


@router.get("/status")
def get_vpn_status():
    return _vpn_status_payload()
