const express = require('express');
const router = express.Router();

let userPoints = 1250; // Mock database

function calculateTier(points) {
    if (points >= 1500) return 'Elite';
    if (points >= 800) return 'Gold';
    if (points >= 300) return 'Silver';
    return 'Bronze';
}

function getNextTier(points) {
    if (points < 300) return { tier: 'Silver', pointsToNextTier: 300 - points };
    if (points < 800) return { tier: 'Gold', pointsToNextTier: 800 - points };
    if (points < 1500) return { tier: 'Elite', pointsToNextTier: 1500 - points };
    return { tier: null, pointsToNextTier: 0 };
}

// GET /api/loyalty/status
router.get('/status', (req, res) => {
    const userTier = calculateTier(userPoints);
    const next = getNextTier(userPoints);
    res.json({
        points: userPoints,
        tier: userTier,
        nextTier: next.tier,
        pointsToNextTier: next.pointsToNextTier
    });
});

// POST /api/loyalty/points
router.post('/points', (req, res) => {
    const { action, points } = req.body;

    if (!points) {
        return res.status(400).json({ error: 'Points value required' });
    }

    userPoints += points;

    const userTier = calculateTier(userPoints);

    res.json({
        success: true,
        message: `Added ${points} points for ${action}`,
        newTotal: userPoints,
        currentTier: userTier
    });
});

module.exports = router;
