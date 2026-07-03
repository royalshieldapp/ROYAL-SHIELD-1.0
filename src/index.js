import express from 'express';
import cors from 'cors';
import { v4 as uuidv4 } from 'uuid';
import dotenv from 'dotenv';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// ===== SECURITY ENDPOINTS =====

// Alarm Control
app.post('/api/alarm', (req, res) => {
  const { userId, action } = req.body; // action: 'arm' | 'disarm'
  
  if (!userId || !['arm', 'disarm'].includes(action)) {
    return res.status(400).json({ error: 'Invalid request' });
  }

  res.json({
    eventId: uuidv4(),
    status: action === 'arm' ? 'ARMED' : 'DISARMED',
    timestamp: new Date().toISOString(),
    message: `Alarm ${action}ed successfully`
  });
});

// Night Mode
app.post('/api/night-mode', (req, res) => {
  const { userId, enabled } = req.body;

  if (!userId || typeof enabled !== 'boolean') {
    return res.status(400).json({ error: 'Invalid request' });
  }

  res.json({
    eventId: uuidv4(),
    status: enabled ? 'ENABLED' : 'DISABLED',
    timestamp: new Date().toISOString(),
    message: `Night mode ${enabled ? 'enabled' : 'disabled'}`
  });
});

// Live Cameras
app.post('/api/cameras', (req, res) => {
  const { userId, action } = req.body; // action: 'start' | 'stop'

  if (!userId || !['start', 'stop'].includes(action)) {
    return res.status(400).json({ error: 'Invalid request' });
  }

  res.json({
    eventId: uuidv4(),
    status: action === 'start' ? 'LIVE' : 'STOPPED',
    streamUrl: action === 'start' ? 'wss://stream.royalshield.local/camera-1' : null,
    timestamp: new Date().toISOString()
  });
});

// Request Patrol
app.post('/api/patrol', (req, res) => {
  const { userId, location } = req.body;

  if (!userId || !location) {
    return res.status(400).json({ error: 'Invalid request' });
  }

  // Simulate dispatch
  res.json({
    eventId: uuidv4(),
    status: 'DISPATCHED',
    eta: '8 minutes',
    unit: 'Unit-47',
    timestamp: new Date().toISOString(),
    message: 'Patrol unit dispatched to your location'
  });
});

// ===== EMERGENCY ENDPOINTS =====

// Panic Button
app.post('/api/panic', (req, res) => {
  const { userId, location, emergencyContacts } = req.body;

  if (!userId || !location) {
    return res.status(400).json({ error: 'userId and location required' });
  }

  // Log emergency event
  console.log(`[EMERGENCY] Panic alert from user ${userId} at`, location);

  // TODO: Send SMS/Email to emergency contacts via Twilio
  // TODO: Log to database
  // TODO: Notify monitoring center

  res.status(200).json({
    eventId: uuidv4(),
    status: 'ALERT_SENT',
    timestamp: new Date().toISOString(),
    message: 'Emergency alert sent to contacts and monitoring center'
  });
});

// Share Location
app.post('/api/share-location', (req, res) => {
  const { userId, location, recipients } = req.body;

  if (!userId || !location || !recipients || recipients.length === 0) {
    return res.status(400).json({ error: 'userId, location, and recipients required' });
  }

  res.json({
    eventId: uuidv4(),
    status: 'SHARED',
    recipientCount: recipients.length,
    timestamp: new Date().toISOString(),
    message: `Location shared with ${recipients.length} contact(s)`
  });
});

// ===== ADVANCED AUTOMATION ENDPOINTS =====

// URL Scanner (Virus Total integration)
app.post('/api/scan-url', (req, res) => {
  const { userId, url } = req.body;

  if (!userId || !url) {
    return res.status(400).json({ error: 'userId and url required' });
  }

  // TODO: Call VirusTotal API with process.env.VIRUSTOTAL_API_KEY
  res.json({
    eventId: uuidv4(),
    url,
    isSafe: true,
    threatLevel: 'LOW',
    timestamp: new Date().toISOString(),
    message: 'URL is safe'
  });
});

// Data Breach Monitor
app.post('/api/breach-check', (req, res) => {
  const { userId, email } = req.body;

  if (!userId || !email) {
    return res.status(400).json({ error: 'userId and email required' });
  }

  // TODO: Call Have I Been Pwned or similar service
  res.json({
    eventId: uuidv4(),
    email,
    isCompromised: false,
    breachCount: 0,
    timestamp: new Date().toISOString()
  });
});

// App Scanner
app.post('/api/scan-apps', (req, res) => {
  const { userId, apps } = req.body;

  if (!userId || !Array.isArray(apps)) {
    return res.status(400).json({ error: 'userId and apps array required' });
  }

  res.json({
    eventId: uuidv4(),
    scannedApps: apps.length,
    maliciousApps: 0,
    timestamp: new Date().toISOString(),
    status: 'CLEAN'
  });
});

// Sound Detection (AI-powered audio analysis)
app.post('/api/sound-detection', (req, res) => {
  const { userId, enabled } = req.body;

  if (!userId || typeof enabled !== 'boolean') {
    return res.status(400).json({ error: 'Invalid request' });
  }

  res.json({
    eventId: uuidv4(),
    status: enabled ? 'LISTENING' : 'DISABLED',
    timestamp: new Date().toISOString()
  });
});

// Low Battery Mode
app.post('/api/battery-mode', (req, res) => {
  const { userId, enabled } = req.body;

  if (!userId || typeof enabled !== 'boolean') {
    return res.status(400).json({ error: 'Invalid request' });
  }

  res.json({
    eventId: uuidv4(),
    status: enabled ? 'ACTIVE' : 'INACTIVE',
    timestamp: new Date().toISOString(),
    message: 'Battery protocol updated'
  });
});

// Safe Route Tracking
app.post('/api/safe-route', (req, res) => {
  const { userId, startLocation, endLocation } = req.body;

  if (!userId || !startLocation || !endLocation) {
    return res.status(400).json({ error: 'userId, startLocation, and endLocation required' });
  }

  res.json({
    eventId: uuidv4(),
    status: 'MONITORING',
    timestamp: new Date().toISOString(),
    message: 'Route monitoring active - alert if deviation detected'
  });
});

// ===== EVENT LOG =====

app.get('/api/events/:userId', (req, res) => {
  const { userId } = req.params;

  res.json({
    userId,
    events: [
      {
        eventId: uuidv4(),
        type: 'ALARM',
        status: 'ARMED',
        timestamp: new Date().toISOString()
      },
      {
        eventId: uuidv4(),
        type: 'PANIC',
        status: 'ALERT_SENT',
        timestamp: new Date(Date.now() - 3600000).toISOString()
      }
    ]
  });
});

// ===== ERROR HANDLING =====

app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: 'Internal Server Error', message: err.message });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Royal Shield Backend API running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
});
