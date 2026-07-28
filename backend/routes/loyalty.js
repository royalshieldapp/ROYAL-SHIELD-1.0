const express = require('express');
const router = express.Router();
const { db, admin } = require('../config/firebase');

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
router.get('/status', async (req, res) => {
    try {
        const userId = req.query.userId || req.headers['x-user-id'];
        if (!userId) {
            return res.status(400).json({ error: 'User ID is required' });
        }

        if (!db) {
             return res.status(500).json({ error: 'Database not initialized' });
        }

        const loyaltyRef = db.collection('users').doc(userId).collection('loyalty').doc('status');
        const doc = await loyaltyRef.get();
        
        let userPoints = 0;
        if (doc.exists) {
            userPoints = doc.data().points || 0;
        }

        const userTier = calculateTier(userPoints);
        const next = getNextTier(userPoints);

        res.json({
            points: userPoints,
            tier: userTier,
            nextTier: next.tier,
            pointsToNextTier: next.pointsToNextTier
        });
    } catch (error) {
        console.error('Error fetching loyalty status:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

// POST /api/loyalty/points
router.post('/points', async (req, res) => {
    try {
        const { action, points, userId } = req.body;
        
        const uid = userId || req.headers['x-user-id'];
        if (!uid) {
            return res.status(400).json({ error: 'User ID is required' });
        }

        if (!points || typeof points !== 'number') {
            return res.status(400).json({ error: 'Valid points value required' });
        }

        if (!db) {
            return res.status(500).json({ error: 'Database not initialized' });
        }

        const loyaltyRef = db.collection('users').doc(uid).collection('loyalty').doc('status');
        
        // Use a transaction or increment to update safely
        await loyaltyRef.set({
            points: admin.firestore.FieldValue.increment(points),
            lastAction: action,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        const updatedDoc = await loyaltyRef.get();
        const newTotal = updatedDoc.data().points;
        const userTier = calculateTier(newTotal);

        res.json({
            success: true,
            message: `Added ${points} points for ${action}`,
            newTotal: newTotal,
            currentTier: userTier
        });
    } catch (error) {
        console.error('Error adding points:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

module.exports = router;
