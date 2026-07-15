const express = require('express');
const axios = require('axios');

const router = express.Router();

// GET /api/phone/check?number=+13055550123
router.get('/check', async (req, res) => {
    const number = typeof req.query.number === 'string' ? req.query.number.trim() : '';

    if (!/^\+[1-9]\d{7,14}$/.test(number)) {
        return res.status(400).json({
            error: 'Phone number must use E.164 format, for example +13055550123',
            code: 'INVALID_PHONE_NUMBER'
        });
    }

    const apiKey = process.env.NUMVERIFY_API_KEY;
    if (!apiKey || apiKey === 'YOUR_NUMVERIFY_KEY_HERE' || apiKey.startsWith('your_')) {
        return res.status(503).json({
            error: 'Phone lookup provider is not configured',
            code: 'PHONE_PROVIDER_NOT_CONFIGURED'
        });
    }

    try {
        const response = await axios.get('https://apilayer.net/api/validate', {
            params: {
                access_key: apiKey,
                number: number.substring(1),
                format: 1
            },
            timeout: 10000
        });
        const data = response.data;

        if (data.error) {
            console.warn('[Phone] Provider rejected request:', data.error.type || 'provider_error');
            return res.status(502).json({
                error: 'Phone lookup provider rejected the request',
                code: 'PHONE_PROVIDER_ERROR'
            });
        }

        const risk = assessRisk(data);
        return res.json({
            valid: data.valid === true,
            number: data.number || number,
            localFormat: data.local_format || '',
            internationalFormat: data.international_format || number,
            countryPrefix: data.country_prefix || '',
            countryCode: data.country_code || '',
            countryName: data.country_name || 'Unknown',
            location: data.location || '',
            carrier: data.carrier || 'Unknown',
            lineType: data.line_type || 'unknown',
            score: risk.score,
            status: risk.status,
            tags: risk.tags,
            riskLevel: risk.riskLevel,
            reportCount: null,
            lastReported: null,
            source: 'numverify',
            checkedAt: new Date().toISOString()
        });
    } catch (err) {
        console.error('[Phone] Provider unavailable:', err.code || err.message);
        return res.status(502).json({
            error: 'Phone lookup provider is temporarily unavailable',
            code: 'PHONE_PROVIDER_UNAVAILABLE'
        });
    }
});

function assessRisk(apiData) {
    if (!apiData.valid) {
        return {
            score: 15,
            status: 'INVALID',
            tags: ['Invalid Number'],
            riskLevel: 'HIGH'
        };
    }

    const tags = [];
    let score = 85;
    let riskLevel = 'LOW';

    if (apiData.line_type === 'voip') {
        score = 60;
        riskLevel = 'MEDIUM';
        tags.push('VoIP Number');
    } else if (apiData.line_type) {
        tags.push(apiData.line_type);
    }

    if (tags.length === 0) tags.push('Validated Number');
    return { score, status: riskLevel === 'LOW' ? 'VALID' : 'CAUTION', tags, riskLevel };
}

module.exports = router;
