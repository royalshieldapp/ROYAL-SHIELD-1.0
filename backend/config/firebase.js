const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
try {
    if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
        const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
        if (!admin.apps.length) {
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount)
            });
        }
    } else if (!admin.apps.length) {
        admin.initializeApp();
    }
} catch (error) {
    console.error('Firebase Admin Initialization Error:', error);
}

const db = admin.apps.length ? admin.firestore() : null;

module.exports = { admin, db };
