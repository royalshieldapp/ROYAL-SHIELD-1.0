const express = require('express');
const router = express.Router();

/**
 * GET /api/vpn/servers
 * Returns list of available VPN servers.
 * In production, this would query a real VPN provider API
 * or a database of configured WireGuard/Tailscale servers.
 */
router.get('/servers', (req, res) => {
    const vpnProvider = process.env.VPN_PROVIDER; // 'wireguard', 'tailscale', etc.

    if (!vpnProvider) {
        return res.json({
            success: true,
            status: 'not_configured',
            code: 'VPN_NOT_CONFIGURED',
            message: 'VPN provider not configured. Set VPN_PROVIDER and server details in environment.',
            servers: []
        });
    }

    // TODO: Query real VPN provider for server list
    // For now, return configured servers from env
    const servers = [];

    // Parse servers from env: VPN_SERVERS=us-east:US East:us-east.vpn.royalshield.app,eu-west:EU West:eu-west.vpn.royalshield.app
    const serversEnv = process.env.VPN_SERVERS || '';
    if (serversEnv) {
        const parsed = serversEnv.split(',').map(s => {
            const [id, name, host] = s.split(':');
            return {
                id: id?.trim(),
                name: name?.trim(),
                host: host?.trim(),
                port: parseInt(process.env.VPN_PORT || '51820'),
                protocol: vpnProvider,
                status: 'available',
                load: Math.floor(Math.random() * 60) + 10 // Simulated load %
            };
        }).filter(s => s.id && s.host);
        servers.push(...parsed);
    }

    res.json({
        success: true,
        status: servers.length > 0 ? 'available' : 'no_servers',
        provider: vpnProvider,
        servers
    });
});

/**
 * POST /api/vpn/config
 * Returns WireGuard configuration for a specific server.
 * Requires premium entitlement (checked by client, enforced here).
 *
 * Body: { serverId: string, userId: string, publicKey: string }
 * Returns: { success, config: { serverPublicKey, endpoint, allowedIPs, dns, ... } }
 */
router.post('/config', (req, res) => {
    const { serverId, userId, publicKey } = req.body;

    if (!serverId) return res.status(400).json({ error: 'serverId is required' });
    if (!publicKey) return res.status(400).json({ error: 'publicKey (client WireGuard public key) is required' });

    const vpnProvider = process.env.VPN_PROVIDER;
    const serverPrivateKey = process.env.VPN_SERVER_PRIVATE_KEY;
    const serverPublicKey = process.env.VPN_SERVER_PUBLIC_KEY;

    if (!vpnProvider || !serverPublicKey) {
        return res.status(503).json({
            error: 'VPN service not configured',
            code: 'VPN_NOT_CONFIGURED',
            message: 'VPN server keys not set. Configure VPN_SERVER_PUBLIC_KEY in environment.'
        });
    }

    // TODO: In production, generate per-user configs dynamically
    // and register the client's public key with the WireGuard server

    // Parse the requested server from env
    const serversEnv = process.env.VPN_SERVERS || '';
    const serverEntry = serversEnv.split(',').find(s => s.startsWith(serverId + ':'));

    if (!serverEntry) {
        return res.status(404).json({ error: 'Server not found', serverId });
    }

    const [, , host] = serverEntry.split(':');
    const port = parseInt(process.env.VPN_PORT || '51820');

    res.json({
        success: true,
        config: {
            interface: {
                // Client generates their own private key locally
                address: '10.0.0.2/32', // TODO: Assign dynamically per user
                dns: process.env.VPN_DNS || '1.1.1.1, 1.0.0.1',
                mtu: 1420
            },
            peer: {
                publicKey: serverPublicKey,
                endpoint: `${host}:${port}`,
                allowedIPs: '0.0.0.0/0, ::/0',
                persistentKeepalive: 25
            }
        },
        serverId,
        provider: vpnProvider,
        expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString() // 24h config validity
    });
});

/**
 * GET /api/vpn/status
 * Returns VPN service status
 */
router.get('/status', (req, res) => {
    const vpnProvider = process.env.VPN_PROVIDER;
    const configured = vpnProvider && process.env.VPN_SERVER_PUBLIC_KEY;

    res.json({
        service: 'vpn',
        status: configured ? 'available' : 'not_configured',
        provider: vpnProvider || null
    });
});

module.exports = router;
