const nodemailer = require('nodemailer');
const Imap = require('imap');
const { simpleParser } = require('mailparser');
const axios = require('axios');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, './.env') });

/**
 * Configure SMTP Transporter for Sending Emails
 */
const transporter = nodemailer.createTransport({
    host: process.env.OPENCLAW_EMAIL_SMTP_HOST || 'smtp.gmail.com',
    port: parseInt(process.env.OPENCLAW_EMAIL_SMTP_PORT || '465'),
    secure: true,
    auth: {
        user: process.env.OPENCLAW_EMAIL_USER,
        pass: process.env.OPENCLAW_EMAIL_PASS
    }
});

/**
 * Sends an email report from OpenClaw
 */
async function sendEmailReport(to, subject, body, htmlContent = null) {
    try {
        const mailOptions = {
            from: `"OpenClaw Shield" <${process.env.OPENCLAW_EMAIL_USER}>`,
            to,
            subject,
            text: body,
            html: htmlContent
        };
        const info = await transporter.sendMail(mailOptions);
        console.log('✉️ Email report sent successfully:', info.messageId);
        return true;
    } catch (error) {
        console.error('❌ Failed to send email report:', error.message);
        return false;
    }
}

/**
 * Poll IMAP mailbox for incoming command emails
 */
function startEmailCommandListener() {
    const imapConfig = {
        user: process.env.OPENCLAW_EMAIL_USER,
        password: process.env.OPENCLAW_EMAIL_PASS,
        host: process.env.OPENCLAW_EMAIL_IMAP_HOST || 'imap.gmail.com',
        port: parseInt(process.env.OPENCLAW_EMAIL_IMAP_PORT || '993'),
        tls: true,
        tlsOptions: { rejectUnauthorized: false }
    };

    if (!imapConfig.user || !imapConfig.password) {
        console.log('⚠️ OpenClaw Email credentials missing. Incoming email commands listener is disabled.');
        return;
    }

    const imap = new Imap(imapConfig);

    function openInbox(cb) {
        imap.openBox('INBOX', false, cb);
    }

    imap.once('ready', () => {
        console.log('📬 OpenClaw Email IMAP listener connected. Monitoring mailbox...');
        imap.on('mail', () => {
            console.log('✉️ New incoming email detected. Fetching...');
            openInbox((err, box) => {
                if (err) return console.error(err);
                
                // Fetch unread messages
                imap.search(['UNSEEN'], (err, results) => {
                    if (err || !results.length) return;

                    const f = imap.fetch(results, { bodies: '' });
                    f.on('message', (msg, seqno) => {
                        msg.on('body', (stream, info) => {
                            simpleParser(stream, async (err, parsed) => {
                                if (err) return console.error(err);

                                const sender = parsed.from.value[0].address;
                                const subject = parsed.subject || '';
                                const content = parsed.text || '';

                                console.log(`✉️ Received command email from [${sender}] with subject [${subject}]`);

                                // Process only messages starting with "openclaw" or "shield" in subject or body
                                if (subject.toLowerCase().includes('openclaw') || content.toLowerCase().includes('openclaw')) {
                                    console.log('🤖 Triggering OpenClaw analysis from email prompt...');
                                    
                                    const backendUrl = process.env.BACKEND_URL || 'http://localhost:3000';
                                    try {
                                        const response = await axios.post(`${backendUrl}/api/assistant/chat`, {
                                            message: content
                                        });

                                        if (response.data && response.data.reply) {
                                            await sendEmailReport(
                                                sender,
                                                `Re: ${subject} [OpenClaw Response]`,
                                                response.data.reply
                                            );
                                        }
                                    } catch (e) {
                                        console.error('Error forwarding email command to backend:', e.message);
                                    }
                                }
                            });
                        });
                    });
                });
            });
        });
    });

    imap.once('error', (err) => {
        console.error('IMAP listener error:', err.message);
        // Auto-retry connection in 30 seconds
        setTimeout(startEmailCommandListener, 30000);
    });

    imap.once('end', () => {
        console.log('IMAP connection closed.');
    });

    imap.connect();
}

// Start listener if executed directly
if (require.main === module) {
    startEmailCommandListener();
}

module.exports = {
    sendEmailReport,
    startEmailCommandListener
};
