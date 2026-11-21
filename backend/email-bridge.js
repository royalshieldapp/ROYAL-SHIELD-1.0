const nodemailer = require('nodemailer');
const Imap = require('imap');
const { simpleParser } = require('mailparser');
const axios = require('axios');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, './.env') });

function envValue(...names) {
    for (const name of names) {
        const value = process.env[name];
        if (value) return value;
    }
    return undefined;
}

const emailUser = envValue('OPENCLAW_EMAIL_USER');
const emailPassword = envValue('OPENCLAW_EMAIL_PASSWORD', 'OPENCLAW_EMAIL_PASS');
const smtpHost = envValue('OPENCLAW_SMTP_HOST', 'OPENCLAW_EMAIL_SMTP_HOST') || 'smtp.gmail.com';
const smtpPort = parseInt(envValue('OPENCLAW_SMTP_PORT', 'OPENCLAW_EMAIL_SMTP_PORT') || '465');
const smtpSecure = envValue('OPENCLAW_SMTP_SECURE', 'OPENCLAW_EMAIL_SMTP_SECURE');
const imapHost = envValue('OPENCLAW_IMAP_HOST', 'OPENCLAW_EMAIL_IMAP_HOST') || 'imap.gmail.com';
const imapPort = parseInt(envValue('OPENCLAW_IMAP_PORT', 'OPENCLAW_EMAIL_IMAP_PORT') || '993');
const imapTls = envValue('OPENCLAW_IMAP_TLS', 'OPENCLAW_EMAIL_IMAP_TLS');

/**
 * Configure SMTP Transporter for Sending Emails
 */
const transporter = nodemailer.createTransport({
    host: smtpHost,
    port: smtpPort,
    secure: smtpSecure ? smtpSecure === 'true' : smtpPort === 465,
    auth: {
        user: emailUser,
        pass: emailPassword
    }
});

/**
 * Sends an email report from OpenClaw
 */
async function sendEmailReport(to, subject, body, htmlContent = null) {
    try {
        const mailOptions = {
            from: `"OpenClaw Shield" <${emailUser}>`,
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
        user: emailUser,
        password: emailPassword,
        host: imapHost,
        port: imapPort,
        tls: imapTls ? imapTls === 'true' : true,
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
