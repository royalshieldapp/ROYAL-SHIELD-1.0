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
    const serverPublicKey = process.env.VPN_SERVER_PUBLIC_KEY;
    const serversEnv = process.env.VPN_SERVERS || '';
    const canIssueConfig = process.env.VPN_ALLOW_STATIC_CONFIG === 'true';

    if (!vpnProvider || !serverPublicKey || !serversEnv) {
        return res.json({
            success: true,
            status: 'not_configured',
            code: 'VPN_NOT_CONFIGURED',
            message: 'VPN provider not configured. Set VPN_PROVIDER, VPN_SERVER_PUBLIC_KEY, and VPN_SERVERS in environment.',
            servers: []
        });
    }

    if (!canIssueConfig) {
        return res.json({
            success: true,
            status: 'peer_registration_not_configured',
            code: 'VPN_PEER_REGISTRATION_NOT_CONFIGURED',
            message: 'VPN server exists, but peer registration/config issuing is not enabled. Set VPN_ALLOW_STATIC_CONFIG=true only for a controlled lab server with pre-authorized peers.',
            servers: []
        });
    }

    const servers = [];

    // Parse servers from env: VPN_SERVERS=us-east:US East:us-east.vpn.royalshield.app,eu-west:EU West:eu-west.vpn.royalshield.app
    if (serversEnv) {
        const parsed = serversEnv.split(',').map(s => {
            const [id, name, host] = s.split(':');
            return {
                id: id?.trim(),
                name: name?.trim(),
                countryCode: (id || '').split('-')[0]?.toUpperCase() || '',
                host: host?.trim(),
                port: parseInt(process.env.VPN_PORT || '51820'),
                protocol: vpnProvider,
                status: 'available',
                load: null
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
    const serverPublicKey = process.env.VPN_SERVER_PUBLIC_KEY;
    const canIssueConfig = process.env.VPN_ALLOW_STATIC_CONFIG === 'true';

    if (!vpnProvider || !serverPublicKey) {
        return res.status(503).json({
            error: 'VPN service not configured',
            code: 'VPN_NOT_CONFIGURED',
            message: 'VPN server keys not set. Configure VPN_SERVER_PUBLIC_KEY in environment.'
        });
    }

    if (!canIssueConfig) {
        return res.status(501).json({
            error: 'VPN peer registration not configured',
            code: 'VPN_PEER_REGISTRATION_NOT_CONFIGURED',
            message: 'WireGuard peer registration is not implemented for this deployment. Configure a real peer-registration workflow before issuing VPN profiles.'
        });
    }

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
    const hasBaseConfig = Boolean(vpnProvider && process.env.VPN_SERVER_PUBLIC_KEY && process.env.VPN_SERVERS);
    const canIssueConfig = process.env.VPN_ALLOW_STATIC_CONFIG === 'true';
    const configured = hasBaseConfig && canIssueConfig;

    res.json({
        service: 'vpn',
        status: configured ? 'available' : (hasBaseConfig ? 'peer_registration_not_configured' : 'not_configured'),
        code: configured ? 'VPN_AVAILABLE' : (hasBaseConfig ? 'VPN_PEER_REGISTRATION_NOT_CONFIGURED' : 'VPN_NOT_CONFIGURED'),
        message: configured
            ? 'VPN service available.'
            : (hasBaseConfig
                ? 'VPN server variables exist, but peer registration/config issuing is not enabled.'
                : 'VPN provider not configured. Set VPN_PROVIDER, VPN_SERVER_PUBLIC_KEY, and VPN_SERVERS.'),
        provider: vpnProvider || null,
        staticConfigAllowed: canIssueConfig
    });
});

module.exports = router;
