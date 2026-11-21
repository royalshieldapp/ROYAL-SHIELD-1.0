#!/usr/bin/env python3
from __future__ import annotations

import base64
import hashlib
import hmac
import ipaddress
import json
import os
import re
import subprocess
import tempfile
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

LISTEN_HOST = os.getenv("VPN_AGENT_HOST", "127.0.0.1")
LISTEN_PORT = int(os.getenv("VPN_AGENT_PORT", "8787"))
INTERFACE = os.getenv("VPN_INTERFACE", "wg0")
NETWORK = ipaddress.ip_network(os.getenv("VPN_CLIENT_NETWORK", "10.66.0.0/24"))
STATE_PATH = Path(os.getenv("VPN_AGENT_STATE", "/var/lib/royal-vpn/peers.json"))
SECRET = os.environ["VPN_CONTROL_SECRET"].encode()
MAX_CLOCK_SKEW = 60
MAX_BODY_BYTES = 4096
CLIENT_ID_RE = re.compile(r"^[A-Za-z0-9_-]{8,128}$")
SERVER_ID_RE = re.compile(r"^[A-Za-z0-9_-]{1,64}$")
LOCK = threading.Lock()
NONCES: dict[str, int] = {}


def load_state() -> dict:
    if not STATE_PATH.exists():
        return {"clients": {}}
    with STATE_PATH.open("r", encoding="utf-8") as handle:
        value = json.load(handle)
    if not isinstance(value.get("clients"), dict):
        raise ValueError("Invalid peer state")
    return value


def save_state(state: dict) -> None:
    STATE_PATH.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix="peers-", dir=STATE_PATH.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            json.dump(state, handle, sort_keys=True)
            handle.flush()
            os.fsync(handle.fileno())
        os.chmod(temporary, 0o600)
        os.replace(temporary, STATE_PATH)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def validate_public_key(value: str) -> None:
    try:
        decoded = base64.b64decode(value, validate=True)
    except ValueError as exc:
        raise ValueError("Invalid WireGuard public key") from exc
    if len(decoded) != 32:
        raise ValueError("Invalid WireGuard public key")


def verify_request(headers, body: bytes) -> None:
    timestamp_text = headers.get("X-Royal-Timestamp", "")
    nonce = headers.get("X-Royal-Nonce", "")
    supplied = headers.get("X-Royal-Signature", "")
    try:
        timestamp = int(timestamp_text)
    except ValueError as exc:
        raise PermissionError("Invalid authentication") from exc
    now = int(time.time())
    if abs(now - timestamp) > MAX_CLOCK_SKEW or not re.fullmatch(r"[a-f0-9]{32}", nonce):
        raise PermissionError("Invalid authentication")

    signed = timestamp_text.encode() + b"\n" + nonce.encode() + b"\n" + body
    expected = hmac.new(SECRET, signed, hashlib.sha256).hexdigest()
    if not hmac.compare_digest(expected, supplied):
        raise PermissionError("Invalid authentication")

    expired = [key for key, seen_at in NONCES.items() if now - seen_at > MAX_CLOCK_SKEW]
    for key in expired:
        NONCES.pop(key, None)
    if nonce in NONCES:
        raise PermissionError("Replay rejected")
    NONCES[nonce] = now


def next_address(state: dict) -> str:
    used = {entry["address"].split("/")[0] for entry in state["clients"].values()}
    live = subprocess.run(
        ["wg", "show", INTERFACE, "allowed-ips"],
        check=True,
        capture_output=True,
        text=True,
        timeout=5,
    )
    for line in live.stdout.splitlines():
        fields = line.split()
        for allowed_ip in fields[1:]:
            try:
                network = ipaddress.ip_network(allowed_ip.rstrip(","), strict=False)
            except ValueError:
                continue
            if network.prefixlen == network.max_prefixlen:
                used.add(str(network.network_address))
    server_ip = str(next(NETWORK.hosts()))
    for candidate in NETWORK.hosts():
        candidate_text = str(candidate)
        if candidate_text != server_ip and candidate_text not in used:
            return candidate_text + "/32"
    raise RuntimeError("VPN address pool exhausted")


def register_peer(payload: dict) -> dict:
    client_id = str(payload.get("userId", ""))
    server_id = str(payload.get("serverId", ""))
    public_key = str(payload.get("publicKey", ""))
    if not CLIENT_ID_RE.fullmatch(client_id) or not SERVER_ID_RE.fullmatch(server_id):
        raise ValueError("Invalid peer identity")
    validate_public_key(public_key)

    with LOCK:
        state = load_state()
        clients = state["clients"]
        current = clients.get(client_id)
        address = current["address"] if current else next_address(state)

        for other_id, entry in clients.items():
            if other_id != client_id and entry["publicKey"] == public_key:
                raise ValueError("Public key already registered")

        if current and current["publicKey"] != public_key:
            subprocess.run(
                ["wg", "set", INTERFACE, "peer", current["publicKey"], "remove"],
                check=True,
                timeout=5,
            )

        subprocess.run(
            ["wg", "set", INTERFACE, "peer", public_key, "allowed-ips", address],
            check=True,
            timeout=5,
        )
        subprocess.run(["wg-quick", "save", INTERFACE], check=True, timeout=5)
        clients[client_id] = {
            "publicKey": public_key,
            "address": address,
            "serverId": server_id,
            "updatedAt": int(time.time()),
        }
        save_state(state)
        return {"success": True, "address": address, "serverId": server_id}


class Handler(BaseHTTPRequestHandler):
    server_version = "RoyalVpnPeerAgent/1.0"

    def send_json(self, code: int, payload: dict) -> None:
        encoded = json.dumps(payload, separators=(",", ":")).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def do_GET(self) -> None:
        if self.path == "/health":
            self.send_json(200, {"status": "healthy"})
        else:
            self.send_json(404, {"error": "not_found"})

    def do_POST(self) -> None:
        if self.path != "/v1/peers":
            self.send_json(404, {"error": "not_found"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length < 2 or length > MAX_BODY_BYTES:
                raise ValueError("Invalid request size")
            body = self.rfile.read(length)
            with LOCK:
                verify_request(self.headers, body)
            payload = json.loads(body)
            self.send_json(200, register_peer(payload))
        except PermissionError:
            self.send_json(401, {"error": "unauthorized"})
        except (ValueError, json.JSONDecodeError):
            self.send_json(400, {"error": "invalid_request"})
        except (subprocess.SubprocessError, OSError, RuntimeError):
            self.send_json(503, {"error": "peer_registration_failed"})

    def log_message(self, format_string: str, *args) -> None:
        return


if __name__ == "__main__":
    if len(SECRET) < 32:
        raise SystemExit("VPN_CONTROL_SECRET must contain at least 32 characters")
    ThreadingHTTPServer((LISTEN_HOST, LISTEN_PORT), Handler).serve_forever()
