const express = require('express');
const router = express.Router();

// ─── Existing Routes ─────────────────────────────────────────────────
const phoneRoutes = require('./phone');
const sosRoutes = require('./sos');
const threatRoutes = require('./threats');
const courseRoutes = require('./courses');
const systemRoutes = require('./system');
const loyaltyRoutes = require('./loyalty');
const scanRoutes = require('./scan');
const businessRoutes = require('./business');
const mockupRoutes = require('./mockups');
const riskRoutes = require('./risk');

// ─── New Routes (v2.0) ──────────────────────────────────────────────
const assistantRoutes = require('./assistant');
const billingRoutes = require('./billing');
const vpnRoutes = require('./vpn');
const openclawRoutes = require('./openclaw');
const securityRoutes = require('./security');

// ─── Register Routes ────────────────────────────────────────────────
router.use('/phone', phoneRoutes);
router.use('/sos', sosRoutes);
router.use('/threats', threatRoutes);
router.use('/courses', courseRoutes);
router.use('/system', systemRoutes);
router.use('/loyalty', loyaltyRoutes);
router.use('/scan', scanRoutes);
router.use('/business', businessRoutes);
router.use('/mockups', mockupRoutes);
router.use('/v1', riskRoutes);

// v2.0 endpoints
router.use('/assistant', assistantRoutes);
router.use('/billing', billingRoutes);
router.use('/vpn', vpnRoutes);
router.use('/openclaw', openclawRoutes);
router.use('/security', securityRoutes);

// Also support /api/ai/chat for backward compatibility with AiManager.kt
router.use('/ai', assistantRoutes);

module.exports = router;
