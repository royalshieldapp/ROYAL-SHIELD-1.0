# Royal VPN peer agent

Runs beside WireGuard and exposes only an authenticated peer-registration endpoint.

Security properties:

- Listens on loopback; Caddy terminates public HTTPS.
- Every mutation requires an HMAC-SHA256 signature, timestamp, and one-time nonce.
- WireGuard private keys never leave the VM.
- Client private keys never leave Android.
- Peer addresses and identities are persisted with mode 0600.
- The service runs with a restricted systemd sandbox and CAP_NET_ADMIN only.

Required environment file at /etc/royal-vpn-peer-agent.env:

VPN_CONTROL_SECRET=<random secret of at least 32 characters>
VPN_CONTROL_HOST=<reserved-ip-with-dashes>.sslip.io
VPN_INTERFACE=wg0
VPN_CLIENT_NETWORK=10.66.0.0/24

The same VPN_CONTROL_SECRET and the HTTPS URL must be configured in Render as
VPN_CONTROL_SECRET and VPN_CONTROL_URL.
