const express = require('express');
const router = express.Router();

function nowIso() {
    return new Date().toISOString();
}

function riskLevel(score) {
    if (score >= 75) return 'CRITICAL';
    if (score >= 50) return 'HIGH';
    if (score >= 25) return 'MEDIUM';
    return 'LOW';
}

function makePolygon(lat, lng, size = 0.004) {
    return [[
        [lng - size, lat - size],
        [lng + size, lat - size],
        [lng + size, lat + size],
        [lng - size, lat + size],
        [lng - size, lat - size]
    ]];
}

function makeFeature(index, lat, lng, score) {
    const level = riskLevel(score);
    return {
        type: 'Feature',
        geometry: {
            type: 'Polygon',
            coordinates: makePolygon(lat, lng)
        },
        properties: {
            h3_cell: `mock-h3-${index}`,
            risk_score: score,
            risk_level: level,
            event_count: 8 + index,
            crime_count: 5 + index,
            fire_count: index % 2,
            recent_7d: 2 + index,
            recent_30d: 8 + index
        }
    };
}

router.get('/risk-map', (req, res) => {
    const minLat = parseFloat(req.query.bbox_min_lat) || 25.70;
    const minLng = parseFloat(req.query.bbox_min_lng) || -80.25;
    const maxLat = parseFloat(req.query.bbox_max_lat) || 25.82;
    const maxLng = parseFloat(req.query.bbox_max_lng) || -80.15;
    const resolution = parseInt(req.query.resolution || '9', 10);
    const centerLat = (minLat + maxLat) / 2;
    const centerLng = (minLng + maxLng) / 2;

    const features = [
        makeFeature(1, centerLat, centerLng, 62),
        makeFeature(2, centerLat + 0.012, centerLng - 0.012, 78),
        makeFeature(3, centerLat - 0.014, centerLng + 0.010, 34)
    ];

    res.json({
        type: 'FeatureCollection',
        features,
        metadata: {
            bbox: {
                min_lat: minLat,
                min_lng: minLng,
                max_lat: maxLat,
                max_lng: maxLng
            },
            resolution,
            zone_count: features.length,
            generated_at: nowIso()
        }
    });
});

router.get('/risk-zones/:h3Cell', (req, res) => {
    const score = 62;
    res.json({
        h3_cell: req.params.h3Cell,
        center: [25.7617, -80.1918],
        risk_score: score,
        risk_level: riskLevel(score),
        statistics: {
            total_events: 18,
            crime_events: 13,
            fire_events: 1,
            osint_events: 4,
            recent_7d: 5,
            recent_30d: 18,
            severity_critical: 1,
            severity_high: 4,
            severity_medium: 8,
            severity_low: 5
        },
        recent_events: [
            {
                type: 'CRIME',
                severity: 'HIGH',
                occurred_at: nowIso(),
                description: 'Recent safety event reported near this zone.'
            }
        ],
        trends: {
            '7d_change': '+6%',
            '30d_change': '-3%',
            direction: 'stable'
        }
    });
});

router.get('/risk-history', (req, res) => {
    res.json({
        h3_cell: req.query.h3_cell || 'mock-h3-1',
        history: [
            { date: req.query.start_date || '2026-07-01', risk_score: 56 },
            { date: req.query.end_date || '2026-07-10', risk_score: 62 }
        ],
        generated_at: nowIso()
    });
});

router.get('/hotspots', (req, res) => {
    const hotspots = [
        {
            hotspot_id: 1,
            center: { lat: 25.7617, lng: -80.1918 },
            h3_cell: 'mock-h3-hotspot-1',
            radius_meters: 650,
            event_count: 14,
            event_types: { CRIME: 10, OSINT: 4 },
            severities: { HIGH: 6, MEDIUM: 8 },
            risk_score: 72,
            risk_level: 'HIGH'
        }
    ];

    res.json({
        hotspots,
        total_count: hotspots.length,
        metadata: {
            clusters_found: hotspots.length,
            total_events: 14,
            recent_events: 6,
            noise_points: 0,
            time_window_days: parseInt(req.query.time_window_days || '30', 10),
            generated_at: nowIso()
        }
    });
});

router.get('/hotspots/predict', (req, res) => {
    res.json({
        predicted_hotspots: [],
        days_ahead: parseInt(req.query.days_ahead || '7', 10),
        generated_at: nowIso(),
        message: 'Predictive hotspot model is not configured on this deployment.'
    });
});

router.get('/hotspots/nearby', (req, res) => {
    res.json({
        lat: parseFloat(req.query.lat || '25.7617'),
        lng: parseFloat(req.query.lng || '-80.1918'),
        radius_meters: parseFloat(req.query.radius_meters || '1000'),
        hotspots: [],
        generated_at: nowIso()
    });
});

router.post('/predict/risk', (req, res) => {
    const locations = Array.isArray(req.body.locations) ? req.body.locations : [];
    const predictionDate = req.body.prediction_date || new Date().toISOString().slice(0, 10);
    const predictions = locations.map((location, index) => {
        const score = 45 + (index * 9);
        return {
            location,
            h3_cell: `mock-h3-predict-${index + 1}`,
            predicted_risk_score: score,
            predicted_risk_level: riskLevel(score),
            confidence: 0.72,
            prediction_date: predictionDate
        };
    });

    res.json({
        predictions,
        total_requested: locations.length,
        total_predicted: predictions.length,
        prediction_date: predictionDate,
        generated_at: nowIso()
    });
});

router.get('/predict/explain', (req, res) => {
    const score = 62;
    res.json({
        h3_cell: req.query.h3_cell || 'mock-h3-1',
        risk_score: score,
        risk_level: riskLevel(score),
        top_factors: [
            { feature: 'recent_incidents', value: 5, contribution: 0.38, direction: 'increases' },
            { feature: 'public_safety_proximity', value: 0.7, contribution: -0.12, direction: 'decreases' }
        ],
        natural_language: 'This zone has elevated recent incident activity, moderated by nearby public safety coverage.',
        confidence: 0.72
    });
});

router.get('/predict/trends', (req, res) => {
    res.json({
        h3_cell: req.query.h3_cell || 'mock-h3-1',
        days: parseInt(req.query.days || '30', 10),
        trend: [
            { date: '2026-07-10', predicted_risk: 62, confidence: 0.72 },
            { date: '2026-07-11', predicted_risk: 60, confidence: 0.70 }
        ],
        generated_at: nowIso()
    });
});

module.exports = router;
