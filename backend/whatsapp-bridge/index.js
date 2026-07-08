const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const axios = require('axios');
const fs = require('fs');
const path = require('path');
const FormData = require('form-data');
require('dotenv').config({ path: path.join(__dirname, '../.env') });

const client = new Client({
    authStrategy: new LocalAuth()
});

client.on('qr', (qr) => {
    console.log('\n--- SCAN THIS QR CODE WITH WHATSAPP TO CONNECT ---');
    qrcode.generate(qr, { small: true });
});

client.on('ready', () => {
    console.log('✅ WhatsApp Bridge is ready and listening!');
});

client.on('message', async (msg) => {
    try {
        let messageText = msg.body;
        
        if (msg.hasMedia) {
            const media = await msg.downloadMedia();
            if (media && (media.mimetype.includes('audio') || media.mimetype.includes('ogg'))) {
                console.log('🎙️ Received audio note from:', msg.from);
                
                // Save audio file temporarily
                const tempAudioPath = path.join(__dirname, `temp_${Date.now()}.ogg`);
                fs.writeFileSync(tempAudioPath, Buffer.from(media.data, 'base64'));
                
                // Transcribe audio using Local Whisper API
                console.log('🔄 Transcribing audio via local Whisper...');
                const transcription = await transcribeAudio(tempAudioPath);
                console.log(`📝 Transcribed Text: "${transcription}"`);
                
                // Cleanup temp file
                if (fs.existsSync(tempAudioPath)) {
                    fs.unlinkSync(tempAudioPath);
                }
                
                if (transcription) {
                    messageText = transcription;
                } else {
                    return msg.reply('❌ Could not transcribe audio message via local Whisper.');
                }
            }
        }
        
        // Filter messages to process commands containing "openclaw" or "shield"
        if (messageText && (messageText.toLowerCase().includes('openclaw') || messageText.toLowerCase().includes('shield'))) {
            console.log(`💬 Processing command: "${messageText}"`);
            
            // Forward command to local Royal Shield Backend Assistant
            const backendUrl = process.env.BACKEND_URL || 'http://localhost:3000';
            const response = await axios.post(`${backendUrl}/api/assistant/chat`, {
                message: messageText
            });
            
            if (response.data && response.data.reply) {
                msg.reply(`🤖 *OpenClaw Response:* \n\n${response.data.reply}`);
            } else {
                msg.reply('⚠️ Command received but backend returned empty response.');
            }
        }
    } catch (error) {
        console.error('Error in WhatsApp bridge message handler:', error.message);
        msg.reply('❌ Error processing command. Make sure the backend server is running.');
    }
});

/**
 * Transcribes audio using Local Whisper API
 */
async function transcribeAudio(filePath) {
    const whisperUrl = process.env.LOCAL_WHISPER_URL || 'http://localhost:9000/asr';
    try {
        const form = new FormData();
        form.append('audio_file', fs.createReadStream(filePath));
        
        const response = await axios.post(whisperUrl, form, {
            headers: {
                ...form.getHeaders()
            },
            params: {
                task: 'transcribe',
                language: 'es',
                output: 'json'
            },
            timeout: 60000
        });
        
        return response.data.text || response.data.transcript || null;
    } catch (err) {
        console.error('Local Whisper transcription failed:', err.message);
        return null;
    }
}

client.initialize();
