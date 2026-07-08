const express = require('express');
const router = express.Router();
const axios = require('axios');

/**
 * GET /api/openclaw/status
 * Returns OpenClaw service availability.
 */
router.get('/status', async (req, res) => {
    const baseUrl = process.env.OPENCLAW_BASE_URL;
    const secret = process.env.OPENCLAW_SECRET;

    if (!baseUrl || !secret) {
        return res.json({
            service: 'openclaw',
            status: 'not_configured',
            code: 'OPENCLAW_NOT_CONFIGURED',
            message: 'OpenClaw service not configured. Set OPENCLAW_BASE_URL and OPENCLAW_SECRET.'
        });
    }

    try {
        const response = await axios.get(`${baseUrl}/health`, {
            headers: { 'Authorization': `Bearer ${secret}` },
            timeout: 5000
        });

        res.json({
            service: 'openclaw',
            status: response.data?.status === 'ok' ? 'available' : 'degraded',
            version: response.data?.version || 'unknown'
        });
    } catch (error) {
        console.error('[OpenClaw] Health check failed:', error.message);
        res.json({
            service: 'openclaw',
            status: 'unavailable',
            code: 'OPENCLAW_UNREACHABLE',
            message: 'OpenClaw service is not responding'
        });
    }
});

/**
 * POST /api/openclaw/session
 * Creates a new OpenClaw session for threat analysis.
 *
 * Body: { type: string, target?: string, options?: object }
 * Returns: { success, sessionId, status }
 */
router.post('/session', async (req, res) => {
    const { type, target, options = {} } = req.body;
    const baseUrl = process.env.OPENCLAW_BASE_URL;
    const secret = process.env.OPENCLAW_SECRET;

    if (!type) {
        return res.status(400).json({ error: 'type is required (e.g., "scan", "monitor", "analyze")' });
    }

    if (!baseUrl || !secret) {
        return res.status(503).json({
            error: 'OpenClaw service not configured',
            code: 'OPENCLAW_NOT_CONFIGURED',
            message: 'Set OPENCLAW_BASE_URL and OPENCLAW_SECRET in environment variables.'
        });
    }

    try {
        const response = await axios.post(`${baseUrl}/api/sessions`, {
            type,
            target,
            options,
            clientId: 'royal-shield-backend',
            timestamp: new Date().toISOString()
        }, {
            headers: {
                'Authorization': `Bearer ${secret}`,
                'Content-Type': 'application/json'
            },
            timeout: 10000
        });

        res.json({
            success: true,
            sessionId: response.data.sessionId || response.data.id,
            status: response.data.status || 'created',
            data: response.data
        });

    } catch (error) {
        console.error('[OpenClaw] Session create error:', error.response?.data ?? error.message);

        if (error.response?.status === 401 || error.response?.status === 403) {
            return res.status(503).json({
                error: 'OpenClaw authentication failed',
                code: 'OPENCLAW_AUTH_ERROR'
            });
        }

        res.status(502).json({
            error: 'Failed to create OpenClaw session',
            code: 'OPENCLAW_ERROR'
        });
    }
});

/**
 * GET /api/openclaw/results/:sessionId
 * Gets results for an OpenClaw session.
 */
router.get('/results/:sessionId', async (req, res) => {
    const { sessionId } = req.params;
    const baseUrl = process.env.OPENCLAW_BASE_URL;
    const secret = process.env.OPENCLAW_SECRET;

    if (!baseUrl || !secret) {
        return res.status(503).json({
            error: 'OpenClaw service not configured',
            code: 'OPENCLAW_NOT_CONFIGURED'
        });
    }

    try {
        const response = await axios.get(`${baseUrl}/api/sessions/${sessionId}`, {
            headers: { 'Authorization': `Bearer ${secret}` },
            timeout: 10000
        });

        res.json({
            success: true,
            sessionId,
            status: response.data.status,
            results: response.data.results || response.data
        });

    } catch (error) {
        if (error.response?.status === 404) {
            return res.status(404).json({ error: 'Session not found', sessionId });
        }
        console.error('[OpenClaw] Results error:', error.message);
        res.status(502).json({ error: 'Failed to get OpenClaw results' });
    }
});

module.exports = router;
