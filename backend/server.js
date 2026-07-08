const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// ─── Security Headers ───────────────────────────────────────────────
app.use(helmet());

// ─── CORS ────────────────────────────────────────────────────────────
app.use(cors({
    origin: process.env.CORS_ORIGINS ? process.env.CORS_ORIGINS.split(',') : '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Internal-Secret']
}));

// ─── Body Parsing ────────────────────────────────────────────────────
app.use(bodyParser.json({ limit: '10mb' }));
app.use(express.static(path.join(__dirname, 'public')));

// ─── Rate Limiting ───────────────────────────────────────────────────
const generalLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many requests, please try again later.' }
});

const aiLimiter = rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: 10,
    message: { error: 'AI rate limit exceeded. Please wait before sending more messages.' }
});

app.use('/api', generalLimiter);
app.use('/api/assistant', aiLimiter);

// ─── Request Logging ─────────────────────────────────────────────────
app.use((req, res, next) => {
    const start = Date.now();
    res.on('finish', () => {
        const duration = Date.now() - start;
        if (req.path !== '/health' && req.path !== '/') {
            console.log(`[${new Date().toISOString()}] ${req.method} ${req.path} → ${res.statusCode} (${duration}ms)`);
        }
    });
    next();
});

// ─── Routes ──────────────────────────────────────────────────────────
const apiRoutes = require('./routes');
app.use('/api', apiRoutes);

// ─── Health Check ────────────────────────────────────────────────────
app.get('/health', (req, res) => {
    res.status(200).json({
        status: 'healthy',
        service: 'Royal Shield Backend',
        version: '2.0.0',
        uptime: Math.floor(process.uptime()),
        timestamp: new Date().toISOString()
    });
});

// ─── Root ────────────────────────────────────────────────────────────
app.get('/', (req, res) => {
    res.json({
        status: 'Online',
        service: 'Royal Shield Backend',
        version: '2.0.0',
        endpoints: {
            health: '/health',
            scan: '/api/scan',
            assistant: '/api/assistant',
            billing: '/api/billing',
            vpn: '/api/vpn',
            sos: '/api/sos',
            security: '/api/security',
            system: '/api/system',
            openclaw: '/api/openclaw'
        }
    });
});

// ─── 404 Handler ─────────────────────────────────────────────────────
app.use((req, res) => {
    res.status(404).json({
        error: 'Not Found',
        path: req.path,
        method: req.method
    });
});

// ─── Global Error Handler ────────────────────────────────────────────
app.use((err, req, res, _next) => {
    console.error(`[ERROR] ${req.method} ${req.path}:`, err.message);
    res.status(err.status || 500).json({
        error: process.env.NODE_ENV === 'production'
            ? 'Internal server error'
            : err.message
    });
});

// ─── Start Server ────────────────────────────────────────────────────
app.listen(PORT, () => {
    console.log(`🛡️  Royal Shield Backend v2.0.0 running on port ${PORT}`);
    console.log(`   Environment: ${process.env.NODE_ENV || 'development'}`);
    console.log(`   Health check: http://localhost:${PORT}/health`);
});
