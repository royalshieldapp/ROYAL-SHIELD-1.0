const express = require('express');
const router = express.Router();

// Twilio client — lazy-init to avoid crash if env vars missing
function getTwilioClient() {
    const accountSid = process.env.TWILIO_ACCOUNT_SID;
    const authToken = process.env.TWILIO_AUTH_TOKEN;

    if (!accountSid || !authToken) {
        return null;
    }
    return require('twilio')(accountSid, authToken);
}

/**
 * POST /api/sos/alert
 * Body: { location: { lat, lng }, type: "EMERGENCY"|"MEDICAL"|"FIRE", contacts: ["+1234567890", ...] }
 *
 * Sends SMS to all emergency contacts via Twilio.
 */
router.post('/alert', async (req, res) => {
    const { location, type = 'EMERGENCY', contacts = [], userName = 'A Royal Shield user' } = req.body;

    if (!location || !location.lat || !location.lng) {
        return res.status(400).json({ error: 'location.lat and location.lng are required' });
    }

    if (!contacts || contacts.length === 0) {
        return res.status(400).json({ error: 'At least one contact phone number is required' });
    }

    const incidentId = 'INC-' + Date.now();
    const mapsLink = `https://maps.google.com/?q=${location.lat},${location.lng}`;
    const message = `🚨 ROYAL SHIELD ${type} ALERT\n${userName} needs help!\nLocation: ${mapsLink}\nIncident: ${incidentId}`;

    console.log(`[SOS] Incident ${incidentId} | type=${type} | contacts=${contacts.length}`);

    const client = getTwilioClient();
    const fromNumber = process.env.TWILIO_PHONE_NUMBER;

    if (!client || !fromNumber) {
        // Graceful degradation — acknowledge but warn
        console.warn('[SOS] Twilio not configured. Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_PHONE_NUMBER in .env');
        return res.json({
            success: true,
            incidentId,
            warning: 'Twilio not configured — SMS not sent. Configure env vars.',
            smsSent: 0
        });
    }

    // Send SMS to all contacts in parallel
    const smsResults = await Promise.allSettled(
        contacts.map(to =>
            client.messages.create({
                body: message,
                from: fromNumber,
                to
            })
        )
    );

    const sent = smsResults.filter(r => r.status === 'fulfilled').length;
    const failed = smsResults.filter(r => r.status === 'rejected').length;
    const failures = smsResults
        .filter(r => r.status === 'rejected')
        .map((r, i) => ({ contact: contacts[i], error: r.reason?.message }));

    if (failures.length > 0) {
        console.error('[SOS] Some SMS failed:', failures);
    }

    res.json({
        success: true,
        incidentId,
        smsSent: sent,
        smsFailed: failed,
        failures: failures.length > 0 ? failures : undefined
    });
});

module.exports = router;
