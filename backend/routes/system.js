const express = require('express');
const router = express.Router();
const axios = require('axios');

function getVirusTotalApiKey() {
    return process.env.VIRUSTOTAL_API_KEY || process.env.VT_API_KEY;
}

/**
 * GET /api/system/status
 * Returns REAL service status — checks each service availability.
 * No more lying about "OPERATIONAL" when things are broken.
 */
router.get('/status', async (req, res) => {
    const checks = {};
    const startTime = Date.now();

    // Check VirusTotal
    const virusTotalApiKey = getVirusTotalApiKey();
    checks.scanner = virusTotalApiKey && virusTotalApiKey !== 'your_virustotal_key_here'
        ? 'READY' : 'NOT_CONFIGURED';

    // Check Twilio
    checks.smsGateway = process.env.TWILIO_ACCOUNT_SID && process.env.TWILIO_AUTH_TOKEN
        ? 'READY' : 'NOT_CONFIGURED';

    // Check Gemini AI
    checks.aiAssistant = process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY !== 'your_gemini_api_key_here'
        ? 'READY' : 'NOT_CONFIGURED';

    // Check Billing
    checks.billing = process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
        ? 'READY' : 'NOT_CONFIGURED';

    // Check VPN
    checks.vpn = process.env.VPN_PROVIDER && process.env.VPN_SERVER_PUBLIC_KEY
        ? 'READY' : 'NOT_CONFIGURED';

    // Check OpenClaw
    if (process.env.OPENCLAW_BASE_URL && process.env.OPENCLAW_SECRET) {
        try {
            await axios.get(`${process.env.OPENCLAW_BASE_URL}/health`, {
                headers: { 'Authorization': `Bearer ${process.env.OPENCLAW_SECRET}` },
                timeout: 3000
            });
            checks.openclaw = 'CONNECTED';
        } catch {
            checks.openclaw = 'UNREACHABLE';
        }
    } else {
        checks.openclaw = 'NOT_CONFIGURED';
    }

    // Determine overall status
    const configuredCount = Object.values(checks).filter(s => s === 'READY' || s === 'CONNECTED').length;
    const totalCount = Object.keys(checks).length;
    let overallStatus;

    if (configuredCount === totalCount) {
        overallStatus = 'FULLY_OPERATIONAL';
    } else if (configuredCount > 0) {
        overallStatus = 'PARTIALLY_OPERATIONAL';
    } else {
        overallStatus = 'MINIMAL';
    }

    res.json({
        status: overallStatus,
        services: checks,
        configured: `${configuredCount}/${totalCount}`,
        uptime: Math.floor(process.uptime()),
        responseTime: `${Date.now() - startTime}ms`,
        lastUpdate: new Date().toISOString(),
        version: '2.0.0'
    });
});

// Controlled payloads used by Android to measure the real device-to-backend path.
router.get('/speed-test/ping', (_req, res) => {
    res.set('Cache-Control', 'no-store');
    res.status(204).end();
});

router.get('/speed-test/download', (req, res) => {
    const requestedBytes = Number.parseInt(req.query.bytes, 10);
    const bytes = Number.isFinite(requestedBytes)
        ? Math.min(Math.max(requestedBytes, 64 * 1024), 5 * 1024 * 1024)
        : 2 * 1024 * 1024;

    res.set({
        'Cache-Control': 'no-store',
        'Content-Type': 'application/octet-stream',
        'Content-Length': String(bytes)
    });
    res.send(Buffer.alloc(bytes));
});

router.post(
    '/speed-test/upload',
    express.raw({ type: 'application/octet-stream', limit: '5mb' }),
    (req, res) => {
        const bytesReceived = Buffer.isBuffer(req.body) ? req.body.length : 0;
        if (bytesReceived === 0) {
            return res.status(400).json({ error: 'Binary payload is required' });
        }
        res.set('Cache-Control', 'no-store');
        return res.json({ bytesReceived, receivedAt: new Date().toISOString() });
    }
);

module.exports = router;
