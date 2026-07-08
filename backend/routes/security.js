const express = require('express');
const router = express.Router();

// In-memory event store — TODO: Replace with PostgreSQL
const securityEvents = [];
const MAX_EVENTS = 10000;

/**
 * POST /api/security/events
 * Logs a security event from the Android app.
 *
 * Body: {
 *   type: string,       // e.g., "THREAT_DETECTED", "VPN_CONNECTED", "SCAN_COMPLETED", "SOS_TRIGGERED"
 *   severity: string,   // "LOW", "MEDIUM", "HIGH", "CRITICAL"
 *   source: string,     // e.g., "url_scanner", "vpn", "sound_detector", "camera"
 *   details: object,    // Event-specific data
 *   deviceId?: string,
 *   userId?: string
 * }
 */
router.post('/events', (req, res) => {
    const { type, severity = 'LOW', source, details = {}, deviceId, userId } = req.body;

    if (!type) return res.status(400).json({ error: 'type is required' });
    if (!source) return res.status(400).json({ error: 'source is required' });

    const validSeverities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
    if (!validSeverities.includes(severity.toUpperCase())) {
        return res.status(400).json({ error: `severity must be one of: ${validSeverities.join(', ')}` });
    }

    const event = {
        id: `EVT-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`,
        type: type.toUpperCase(),
        severity: severity.toUpperCase(),
        source,
        details,
        deviceId: deviceId || 'unknown',
        userId: userId || 'anonymous',
        timestamp: new Date().toISOString(),
        ip: req.ip
    };

    // Keep events within limit
    if (securityEvents.length >= MAX_EVENTS) {
        securityEvents.shift(); // Remove oldest
    }
    securityEvents.push(event);

    console.log(`[Security] ${event.severity} | ${event.type} from ${event.source} | device=${event.deviceId}`);

    res.status(201).json({
        success: true,
        eventId: event.id,
        message: 'Security event logged'
    });
});

/**
 * GET /api/security/events
 * Returns recent security events.
 * Query params: ?limit=50&severity=HIGH&type=THREAT_DETECTED&source=url_scanner
 */
router.get('/events', (req, res) => {
    const internalSecret = process.env.API_INTERNAL_SECRET;
    const providedSecret = req.headers['x-internal-secret'];

    // Protect read access with internal secret
    if (internalSecret && providedSecret !== internalSecret) {
        return res.status(403).json({ error: 'Forbidden — internal secret required' });
    }

    let filtered = [...securityEvents];

    // Filter by severity
    if (req.query.severity) {
        filtered = filtered.filter(e => e.severity === req.query.severity.toUpperCase());
    }

    // Filter by type
    if (req.query.type) {
        filtered = filtered.filter(e => e.type === req.query.type.toUpperCase());
    }

    // Filter by source
    if (req.query.source) {
        filtered = filtered.filter(e => e.source === req.query.source);
    }

    // Filter by deviceId
    if (req.query.deviceId) {
        filtered = filtered.filter(e => e.deviceId === req.query.deviceId);
    }

    // Limit and sort (newest first)
    const limit = Math.min(parseInt(req.query.limit) || 50, 500);
    filtered = filtered.reverse().slice(0, limit);

    res.json({
        success: true,
        total: securityEvents.length,
        returned: filtered.length,
        events: filtered
    });
});

/**
 * GET /api/security/summary
 * Returns a summary of security events
 */
router.get('/summary', (req, res) => {
    const last24h = new Date(Date.now() - 24 * 60 * 60 * 1000);
    const recent = securityEvents.filter(e => new Date(e.timestamp) > last24h);

    const bySeverity = { LOW: 0, MEDIUM: 0, HIGH: 0, CRITICAL: 0 };
    const byType = {};
    const bySource = {};

    recent.forEach(e => {
        bySeverity[e.severity] = (bySeverity[e.severity] || 0) + 1;
        byType[e.type] = (byType[e.type] || 0) + 1;
        bySource[e.source] = (bySource[e.source] || 0) + 1;
    });

    res.json({
        success: true,
        period: '24h',
        totalEvents: recent.length,
        bySeverity,
        byType,
        bySource
    });
});

module.exports = router;
