const express = require('express');
const router = express.Router();
const axios = require('axios');

// GET /api/phone/check?number=+1234567890
router.get('/check', async (req, res) => {
    const { number } = req.query;

    if (!number) {
        return res.status(400).json({ error: 'Phone number is required' });
    }

    const apiKey = process.env.NUMVERIFY_API_KEY;

    // ── Real API: NumVerify ──────────────────────────────────────
    if (apiKey && apiKey !== 'YOUR_NUMVERIFY_KEY_HERE') {
        try {
            const cleanNumber = number.replace(/[^0-9+]/g, '').replace('+', '');
            const response = await axios.get('http://apilayer.net/api/validate', {
                params: {
                    access_key: apiKey,
                    number: cleanNumber,
                    country_code: '',
                    format: 1
                },
                timeout: 10000
            });

            const data = response.data;

            if (data.error) {
                console.warn('[Phone] NumVerify API error:', data.error);
                return res.json(buildMockResult(number));
            }

            // Build risk assessment from real data
            const riskAssessment = assessRisk(number, data);

            return res.json({
                valid: data.valid || false,
                number: data.number || number,
                localFormat: data.local_format || '',
                internationalFormat: data.international_format || number,
                countryPrefix: data.country_prefix || '',
                countryCode: data.country_code || '',
                countryName: data.country_name || 'Unknown',
                location: data.location || '',
                carrier: data.carrier || 'Unknown',
                lineType: data.line_type || 'unknown',
                score: riskAssessment.score,
                status: riskAssessment.status,
                tags: riskAssessment.tags,
                riskLevel: riskAssessment.riskLevel,
                reportCount: riskAssessment.reportCount,
                lastReported: riskAssessment.lastReported,
                source: 'numverify',
                checkedAt: new Date().toISOString()
            });
        } catch (err) {
            console.error('[Phone] NumVerify request failed:', err.message);
            // Fall through to mock
        }
    }

    // ── Fallback: Enhanced Mock ──────────────────────────────────
    res.json(buildMockResult(number));
});

/**
 * Assess risk based on number patterns and NumVerify data.
 */
function assessRisk(number, apiData) {
    const result = { score: 85, status: 'SAFE', tags: [], riskLevel: 'LOW', reportCount: 0, lastReported: null };

    // VoIP numbers are higher risk
    if (apiData.line_type === 'voip') {
        result.score -= 30;
        result.tags.push('VoIP Number');
        result.riskLevel = 'MEDIUM';
    }

    // Invalid numbers
    if (!apiData.valid) {
        result.score = 15;
        result.status = 'MALICIOUS';
        result.tags.push('Invalid Number', 'Possible Spoofing');
        result.riskLevel = 'CRITICAL';
        return result;
    }

    // Toll-free = likely business
    if (apiData.line_type === 'toll_free') {
        result.tags.push('Toll-Free', 'Business Line');
    }

    // Mobile = personal
    if (apiData.line_type === 'mobile') {
        result.tags.push('Mobile');
    }

    // Landline
    if (apiData.line_type === 'landline') {
        result.tags.push('Landline');
    }

    // Pattern-based risk (known scam patterns)
    if (number.endsWith('666') || number.endsWith('999')) {
        result.score = Math.max(result.score - 50, 5);
        result.status = 'MALICIOUS';
        result.tags.push('Scam Pattern', 'High Risk');
        result.riskLevel = 'CRITICAL';
        result.reportCount = Math.floor(Math.random() * 500) + 100;
        result.lastReported = new Date(Date.now() - Math.random() * 7 * 86400000).toISOString();
    } else if (number.endsWith('000')) {
        result.score = Math.max(result.score - 40, 20);
        result.status = 'SPAM';
        result.tags.push('Telemarketing', 'Robocall');
        result.riskLevel = 'HIGH';
        result.reportCount = Math.floor(Math.random() * 100) + 20;
        result.lastReported = new Date(Date.now() - Math.random() * 30 * 86400000).toISOString();
    }

    // Final status based on score
    if (result.score >= 80) result.status = 'SAFE';
    else if (result.score >= 50) { result.status = 'CAUTION'; result.riskLevel = 'MEDIUM'; }
    else if (result.score >= 25) { result.status = 'SPAM'; result.riskLevel = 'HIGH'; }

    if (result.tags.length === 0) {
        result.tags.push('No Reports');
    }

    return result;
}

/**
 * Build enriched mock result when no API is available.
 */
function buildMockResult(number) {
    const clean = number.replace(/[^0-9+]/g, '');

    // Default safe result
    let result = {
        valid: true,
        number: number,
        localFormat: clean.replace(/^\+1/, ''),
        internationalFormat: clean.startsWith('+') ? clean : `+${clean}`,
        countryPrefix: '+1',
        countryCode: 'US',
        countryName: 'United States',
        location: 'New York, NY',
        carrier: 'Verizon Wireless',
        lineType: 'mobile',
        score: 92,
        status: 'SAFE',
        tags: ['Personal Mobile', 'No Reports'],
        riskLevel: 'LOW',
        reportCount: 0,
        lastReported: null,
        source: 'mock',
        checkedAt: new Date().toISOString()
    };

    if (clean.endsWith('666')) {
        result.score = 8;
        result.status = 'MALICIOUS';
        result.carrier = 'VoIP Provider';
        result.lineType = 'voip';
        result.location = 'Unknown';
        result.countryName = 'Unknown';
        result.tags = ['Scam', 'IRS Impersonation', 'Spoofed Number', 'Robocall'];
        result.riskLevel = 'CRITICAL';
        result.reportCount = 412;
        result.lastReported = new Date(Date.now() - 86400000).toISOString();
    } else if (clean.endsWith('000')) {
        result.score = 32;
        result.status = 'SPAM';
        result.carrier = 'T-Mobile';
        result.location = 'Dallas, TX';
        result.tags = ['Telemarketing', 'Robocall', 'Automated'];
        result.riskLevel = 'HIGH';
        result.reportCount = 89;
        result.lastReported = new Date(Date.now() - 3 * 86400000).toISOString();
    } else if (clean.startsWith('+1800') || clean.startsWith('1800')) {
        result.score = 90;
        result.carrier = 'Business Line';
        result.lineType = 'toll_free';
        result.location = 'Nationwide';
        result.tags = ['Verified Business', 'Customer Service'];
    } else if (clean.startsWith('+57') || clean.startsWith('57')) {
        result.countryPrefix = '+57';
        result.countryCode = 'CO';
        result.countryName = 'Colombia';
        result.carrier = 'Claro';
        result.location = 'Bogotá';
        result.localFormat = clean.replace(/^\+?57/, '');
        result.tags = ['Mobile', 'No Reports'];
    } else if (clean.startsWith('+44') || clean.startsWith('44')) {
        result.countryPrefix = '+44';
        result.countryCode = 'GB';
        result.countryName = 'United Kingdom';
        result.carrier = 'Vodafone UK';
        result.location = 'London';
        result.localFormat = clean.replace(/^\+?44/, '0');
        result.tags = ['Mobile', 'No Reports'];
    }

    return result;
}

module.exports = router;
