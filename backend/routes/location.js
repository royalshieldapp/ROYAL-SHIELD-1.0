const express = require('express');
const fs = require('fs');
const path = require('path');
const router = express.Router();

const DATA_DIR = path.join(__dirname, '../data');
const DATA_FILE = path.join(DATA_DIR, 'location-events.json');
const MAX_EVENTS = parseInt(process.env.LOCATION_HISTORY_LIMIT || '500', 10);

function ensureDataFile() {
    if (!fs.existsSync(DATA_DIR)) {
        fs.mkdirSync(DATA_DIR, { recursive: true });
    }
    if (!fs.existsSync(DATA_FILE)) {
        fs.writeFileSync(DATA_FILE, '[]');
    }
}

function readEvents() {
    ensureDataFile();
    try {
        const raw = fs.readFileSync(DATA_FILE, 'utf8');
        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed : [];
    } catch (_err) {
        return [];
    }
}

function writeEvents(events) {
    ensureDataFile();
    fs.writeFileSync(DATA_FILE, JSON.stringify(events.slice(-MAX_EVENTS), null, 2));
}

function parseCoordinate(value) {
    const number = Number(value);
    return Number.isFinite(number) ? number : null;
}

function validateLocation(latitude, longitude) {
    if (latitude === null || longitude === null) {
        return 'latitude and longitude must be valid numbers';
    }
    if (latitude < -90 || latitude > 90) {
        return 'latitude must be between -90 and 90';
    }
    if (longitude < -180 || longitude > 180) {
        return 'longitude must be between -180 and 180';
    }
    return null;
}

function requireInternalSecret(req, res, next) {
    const expected = process.env.API_INTERNAL_SECRET;
    if (!expected) return next();

    const provided = req.get('X-Internal-Secret');
    if (provided !== expected) {
        return res.status(403).json({ error: 'Forbidden' });
    }

    next();
}

router.post('/track', (req, res) => {
    const latitude = parseCoordinate(req.body.latitude ?? req.body.lat);
    const longitude = parseCoordinate(req.body.longitude ?? req.body.lng);
    const validationError = validateLocation(latitude, longitude);

    if (validationError) {
        return res.status(400).json({ error: validationError });
    }

    const event = {
        id: `LOC-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        latitude,
        longitude,
        source: String(req.body.source || 'android').slice(0, 40),
        userId: req.body.userId ? String(req.body.userId).slice(0, 80) : 'anonymous',
        accuracyMeters: req.body.accuracyMeters ? parseCoordinate(req.body.accuracyMeters) : null,
        recordedAt: new Date().toISOString()
    };

    const events = readEvents();
    events.push(event);
    writeEvents(events);

    console.log(`[Location] Stored ${event.id} source=${event.source} user=${event.userId}`);

    res.status(201).json({
        success: true,
        id: event.id,
        recordedAt: event.recordedAt
    });
});

router.get('/recent', requireInternalSecret, (req, res) => {
    const limit = Math.min(parseInt(req.query.limit || '25', 10) || 25, 100);
    const events = readEvents().slice(-limit).reverse();

    res.json({
        success: true,
        count: events.length,
        events
    });
});

module.exports = router;
