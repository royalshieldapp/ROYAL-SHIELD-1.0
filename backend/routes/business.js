const express = require('express');
const router = express.Router();

// POST /api/business/quote
router.post('/quote', (req, res) => {
    const { contactName, companyName, email, phone, employees, requirements } = req.body;

    console.log('Received Business Quote Request:', { contactName, companyName, email });

    // TODO: Send email to admin using Nodemailer or SendGrid
    // const transporter = nodemailer.createTransport(...);
    // await transporter.sendMail(...);

    res.json({
        success: true,
        message: 'Quote request received successfully. Our team will contact you shortly.',
        ticketId: 'Q-' + Date.now()
    });
});

module.exports = router;
