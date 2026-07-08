const express = require('express');
const router = express.Router();
const axios = require('axios');

/**
 * POST /api/billing/verify
 * Verifies a Google Play purchase token server-side.
 *
 * Body: { purchaseToken: string, productId: string, packageName?: string }
 * Returns: { success, valid, entitlement, expiresAt }
 *
 * In production, this calls Google Play Developer API to verify.
 * Requires GOOGLE_PLAY_SERVICE_ACCOUNT_JSON env var with service account credentials.
 */
router.post('/verify', async (req, res) => {
    const { purchaseToken, productId, packageName = 'com.royalshield.app' } = req.body;

    if (!purchaseToken) {
        return res.status(400).json({ error: 'purchaseToken is required' });
    }
    if (!productId) {
        return res.status(400).json({ error: 'productId is required' });
    }

    // Valid product IDs
    const validProducts = {
        'lifetime_starter': { tier: 'STARTER', level: 1 },
        'lifetime_gold': { tier: 'GOLD', level: 2 },
        'lifetime_ultimate': { tier: 'ULTIMATE', level: 3 },
        'security_access_099': { tier: 'STARTER', level: 1 }
    };

    if (!validProducts[productId]) {
        return res.status(400).json({ error: 'Invalid product ID' });
    }

    const serviceAccountJson = process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON;

    if (!serviceAccountJson) {
        // Service account not configured — return config-required status
        return res.status(503).json({
            error: 'Billing verification not configured',
            code: 'BILLING_NOT_CONFIGURED',
            message: 'Google Play service account not set. Configure GOOGLE_PLAY_SERVICE_ACCOUNT_JSON.'
        });
    }

    try {
        // Parse service account and get access token
        const serviceAccount = JSON.parse(serviceAccountJson);
        const accessToken = await getGoogleAccessToken(serviceAccount);

        // Verify purchase with Google Play Developer API
        const verifyUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/purchases/products/${productId}/tokens/${purchaseToken}`;

        const verifyResponse = await axios.get(verifyUrl, {
            headers: { Authorization: `Bearer ${accessToken}` }
        });

        const purchase = verifyResponse.data;
        const isValid = purchase.purchaseState === 0; // 0 = purchased
        const isConsumed = purchase.consumptionState === 1;
        const isAcknowledged = purchase.acknowledgementState === 1;

        const product = validProducts[productId];

        res.json({
            success: true,
            valid: isValid,
            entitlement: {
                tier: product.tier,
                level: product.level,
                productId,
                purchaseState: isValid ? 'PURCHASED' : 'INVALID',
                acknowledged: isAcknowledged,
                consumed: isConsumed
            },
            purchaseTime: purchase.purchaseTimeMillis
                ? new Date(parseInt(purchase.purchaseTimeMillis)).toISOString()
                : null
        });

    } catch (error) {
        if (error.response?.status === 404) {
            return res.json({
                success: true,
                valid: false,
                entitlement: { tier: 'FREE', level: 0 },
                reason: 'Purchase token not found'
            });
        }
        if (error.response?.status === 401 || error.response?.status === 403) {
            console.error('[Billing] Auth error:', error.response?.data);
            return res.status(503).json({
                error: 'Billing service authentication error',
                code: 'BILLING_AUTH_ERROR'
            });
        }
        console.error('[Billing] Verify error:', error.message);
        res.status(500).json({ error: 'Failed to verify purchase' });
    }
});

/**
 * GET /api/billing/status/:userId
 * Returns the current subscription status for a user.
 * In production, check against a database. For now, returns default free tier.
 */
router.get('/status/:userId', (req, res) => {
    const { userId } = req.params;

    if (!userId) {
        return res.status(400).json({ error: 'userId is required' });
    }

    // TODO: Query database for user's entitlement
    // For now, return free tier as default
    res.json({
        success: true,
        userId,
        entitlement: {
            tier: 'FREE',
            level: 0,
            features: {
                urlScan: true,
                fileScan: false,
                aiAssistant: false,
                vpn: false,
                advancedSecurity: false,
                prioritySupport: false
            }
        },
        message: 'Database not connected — returning default free tier'
    });
});

/**
 * GET /api/billing/products
 * Returns available products and their details.
 */
router.get('/products', (req, res) => {
    res.json({
        success: true,
        products: [
            {
                id: 'lifetime_starter',
                name: 'Starter Shield',
                tier: 'STARTER',
                type: 'lifetime',
                features: ['URL Scanning', 'Basic AI Assistant', 'SOS Alerts']
            },
            {
                id: 'lifetime_gold',
                name: 'Gold Shield',
                tier: 'GOLD',
                type: 'lifetime',
                features: ['All Starter features', 'File Scanning', 'VPN Access', 'Advanced AI', 'Sound Detection']
            },
            {
                id: 'lifetime_ultimate',
                name: 'Ultimate Shield',
                tier: 'ULTIMATE',
                type: 'lifetime',
                features: ['All Gold features', 'Security Camera', 'XDR Dashboard', 'Priority Support', 'OpenClaw Access']
            },
            {
                id: 'security_access_099',
                name: 'Security Access',
                tier: 'STARTER',
                type: 'one-time',
                features: ['Basic security features access']
            }
        ]
    });
});

/**
 * Helper: Get Google OAuth2 access token from service account
 */
async function getGoogleAccessToken(serviceAccount) {
    const jwt = require('jsonwebtoken');

    const now = Math.floor(Date.now() / 1000);
    const token = jwt.sign(
        {
            iss: serviceAccount.client_email,
            scope: 'https://www.googleapis.com/auth/androidpublisher',
            aud: 'https://oauth2.googleapis.com/token',
            iat: now,
            exp: now + 3600
        },
        serviceAccount.private_key,
        { algorithm: 'RS256' }
    );

    const response = await axios.post('https://oauth2.googleapis.com/token', {
        grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        assertion: token
    });

    return response.data.access_token;
}

module.exports = router;
