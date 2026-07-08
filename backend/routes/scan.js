const express = require('express');
const router = express.Router();
const axios = require('axios');

const VT_BASE = 'https://www.virustotal.com/api/v3';

function vtHeaders() {
    return { 'x-apikey': process.env.VIRUSTOTAL_API_KEY };
}

/**
 * POST /api/scan/url
 * Submits a URL for VirusTotal scanning.
 * Returns { analysisId } — poll /api/scan/result/:id for results.
 */
router.post('/url', async (req, res) => {
    const { url } = req.body;
    const apiKey = process.env.VIRUSTOTAL_API_KEY;

    if (!url) return res.status(400).json({ error: 'URL is required' });

    if (!apiKey) {
        return res.status(503).json({ error: 'Scanner service configuration unavailable' });
    }

    try {
        const scanResponse = await axios.post(
            `${VT_BASE}/urls`,
            `url=${encodeURIComponent(url)}`,
            { headers: { ...vtHeaders(), 'Content-Type': 'application/x-www-form-urlencoded' } }
        );

        const analysisId = scanResponse.data.data.id;
        res.json({ success: true, analysisId, message: 'URL submitted for scanning' });

    } catch (error) {
        console.error('VirusTotal submit error:', error.response?.data ?? error.message);
        res.status(500).json({ error: 'Failed to scan URL via VirusTotal' });
    }
});

/**
 * GET /api/scan/result/:analysisId
 * Polls VirusTotal for the scan result of a previously submitted URL.
 * Returns { status, stats, positives, total, permalink }
 */
router.get('/result/:analysisId', async (req, res) => {
    const { analysisId } = req.params;
    const apiKey = process.env.VIRUSTOTAL_API_KEY;

    if (!apiKey) {
        return res.status(503).json({ error: 'Scanner service configuration unavailable' });
    }

    try {
        const response = await axios.get(
            `${VT_BASE}/analyses/${analysisId}`,
            { headers: vtHeaders() }
        );

        const data = response.data.data;
        const attributes = data.attributes;
        const status = attributes.status; // "queued" | "in-progress" | "completed"
        const stats = attributes.stats || {};  // malicious, suspicious, harmless, undetected

        res.json({
            success: true,
            analysisId,
            status,
            stats,
            positives: (stats.malicious || 0) + (stats.suspicious || 0),
            total: Object.values(stats).reduce((a, b) => a + b, 0),
            permalink: `https://www.virustotal.com/gui/url/${analysisId}`
        });

    } catch (error) {
        console.error('VirusTotal poll error:', error.response?.data ?? error.message);
        res.status(500).json({ error: 'Failed to retrieve scan result' });
    }
});

/**
 * POST /api/scan/file-hash
 * Looks up a file hash (MD5/SHA1/SHA256) directly in VirusTotal.
 * Returns last known scan result for that hash.
 */
router.post('/file-hash', async (req, res) => {
    const { hash } = req.body;
    const apiKey = process.env.VIRUSTOTAL_API_KEY;

    if (!hash) return res.status(400).json({ error: 'hash is required' });
    if (!apiKey) return res.status(503).json({ error: 'Scanner service configuration unavailable' });

    try {
        const response = await axios.get(
            `${VT_BASE}/files/${hash}`,
            { headers: vtHeaders() }
        );

        const attributes = response.data.data.attributes;
        const stats = attributes.last_analysis_stats || {};

        res.json({
            success: true,
            hash,
            name: attributes.meaningful_name || hash,
            positives: (stats.malicious || 0) + (stats.suspicious || 0),
            total: Object.values(stats).reduce((a, b) => a + b, 0),
            stats,
            reputation: attributes.reputation || 0,
            permalink: `https://www.virustotal.com/gui/file/${hash}`
        });

    } catch (error) {
        if (error.response?.status === 404) {
            return res.json({ success: true, hash, notFound: true, message: 'Hash not in VirusTotal database' });
        }
        console.error('VirusTotal hash error:', error.response?.data ?? error.message);
        res.status(500).json({ error: 'Failed to look up file hash' });
    }
});

module.exports = router;
