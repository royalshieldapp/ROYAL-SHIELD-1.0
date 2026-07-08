const express = require('express');
const router = express.Router();
const axios = require('axios');

/**
 * POST /api/assistant/chat
 * AI Proxy — forwards chat to Gemini API securely.
 * The API key stays on the server, never exposed to the client.
 *
 * Body: { message: string, history?: [{ role, content }], model?: string }
 * Returns: { success, reply, model }
 */
router.post('/chat', async (req, res) => {
    const { message, history = [], model = 'gemini-1.5-flash' } = req.body;

    if (!message || typeof message !== 'string' || message.trim().length === 0) {
        return res.status(400).json({ error: 'message is required and must be a non-empty string' });
    }

    const apiKey = process.env.GEMINI_API_KEY;

    if (!apiKey || apiKey === 'your_gemini_api_key_here') {
        return res.status(503).json({
            error: 'AI service not configured',
            code: 'AI_NOT_CONFIGURED',
            message: 'Gemini API key not set on the server. Configure GEMINI_API_KEY in environment variables.'
        });
    }

    try {
        // Build conversation contents for Gemini
        const contents = [];

        // Add history if provided
        for (const entry of history.slice(-10)) { // Keep last 10 messages
            contents.push({
                role: entry.role === 'assistant' ? 'model' : 'user',
                parts: [{ text: entry.content }]
            });
        }

        // Add current message
        contents.push({
            role: 'user',
            parts: [{ text: message }]
        });

        const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

        const response = await axios.post(geminiUrl, {
            contents,
            generationConfig: {
                temperature: 0.7,
                topK: 40,
                topP: 0.95,
                maxOutputTokens: 2048
            },
            safetySettings: [
                { category: 'HARM_CATEGORY_HARASSMENT', threshold: 'BLOCK_MEDIUM_AND_ABOVE' },
                { category: 'HARM_CATEGORY_HATE_SPEECH', threshold: 'BLOCK_MEDIUM_AND_ABOVE' },
                { category: 'HARM_CATEGORY_SEXUALLY_EXPLICIT', threshold: 'BLOCK_MEDIUM_AND_ABOVE' },
                { category: 'HARM_CATEGORY_DANGEROUS_CONTENT', threshold: 'BLOCK_MEDIUM_AND_ABOVE' }
            ]
        }, {
            headers: { 'Content-Type': 'application/json' },
            timeout: 30000
        });

        const candidate = response.data.candidates?.[0];
        const reply = candidate?.content?.parts?.[0]?.text;

        if (!reply) {
            const finishReason = candidate?.finishReason;
            if (finishReason === 'SAFETY') {
                return res.status(422).json({
                    error: 'Response blocked by safety filters',
                    code: 'SAFETY_BLOCKED'
                });
            }
            return res.status(500).json({
                error: 'AI returned empty response',
                code: 'EMPTY_RESPONSE'
            });
        }

        res.json({
            success: true,
            reply,
            model,
            usage: {
                promptTokens: response.data.usageMetadata?.promptTokenCount,
                responseTokens: response.data.usageMetadata?.candidatesTokenCount
            }
        });

    } catch (error) {
        if (error.response?.status === 429) {
            return res.status(429).json({
                error: 'AI rate limit exceeded',
                code: 'RATE_LIMITED',
                retryAfter: 60
            });
        }
        if (error.response?.status === 400) {
            return res.status(400).json({
                error: 'Invalid request to AI service',
                code: 'INVALID_REQUEST',
                details: error.response?.data?.error?.message
            });
        }
        console.error('[AI] Gemini error:', error.response?.data ?? error.message);
        res.status(502).json({
            error: 'AI service temporarily unavailable',
            code: 'AI_UNAVAILABLE'
        });
    }
});

/**
 * GET /api/assistant/status
 * Returns AI service availability
 */
router.get('/status', (req, res) => {
    const apiKey = process.env.GEMINI_API_KEY;
    const configured = apiKey && apiKey !== 'your_gemini_api_key_here';

    res.json({
        service: 'ai_assistant',
        status: configured ? 'available' : 'not_configured',
        model: 'gemini-1.5-flash',
        provider: 'google'
    });
});

module.exports = router;
