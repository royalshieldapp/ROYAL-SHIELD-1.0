package com.royalshield.app.managers

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Manager to handle emergency SOS messages via Twilio API.
 * This allows sending SMS without opening the default messaging app.
 */
class TwilioManager(private val context: Context) {

    private val client = OkHttpClient()

    // Twilio Credentials (secured)
    private fun getAccountSid() = PreferencesManager.getTwilioAccountSid() ?: com.royalshield.app.BuildConfig.TWILIO_ACCOUNT_SID
    private fun getAuthToken() = PreferencesManager.getTwilioAuthToken() ?: com.royalshield.app.BuildConfig.TWILIO_AUTH_TOKEN
    private fun getFromNumber() = PreferencesManager.getTwilioFromNumber() ?: ""

    // Direct Twilio Messages API
    private val TWILIO_API_URL = "https://api.twilio.com/2010-04-01/Accounts/${getAccountSid()}/Messages.json"
    
    // Render Backend URL (Production)
    private val BACKEND_URL = "https://server-beckend.onrender.com/api/sos/alert"

    // JSON media type
    private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()

    fun sendEmergencySms(to: String, message: String, lat: Double? = null, lng: Double? = null, onResult: (Boolean) -> Unit) {
        // We now prioritize the Render Backend for full feature support (location links, etc)
        // If it fails, we fallback to Direct Twilio API or Direct Device SMS.
        sendToBackend(to, message, lat, lng, onResult)
    }

    private fun sendToBackend(to: String, message: String, lat: Double?, lng: Double?, onResult: (Boolean) -> Unit) {
        val latVal = lat ?: 0.0
        val lngVal = lng ?: 0.0
        
        val jsonPayload = """
            {
                "contacts": ["$to"],
                "userName": "Royal Shield User",
                "type": "EMERGENCY",
                "location": {
                    "lat": $latVal,
                    "lng": $lngVal
                },
                "note": "$message"
            }
        """.trimIndent()

        val body = jsonPayload.toRequestBody(JSON)
        val request = Request.Builder()
            .url(BACKEND_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TwilioManager", "Backend failed, falling back to Direct Twilio", e)
                sendDirectTwilio(to, message, onResult)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("TwilioManager", "SOS Backend Success")
                    onResult(true)
                } else {
                    Log.e("TwilioManager", "Backend Error ${response.code}, falling back")
                    sendDirectTwilio(to, message, onResult)
                }
                response.close()
            }
        })
    }

    private fun sendDirectTwilio(to: String, message: String, onResult: (Boolean) -> Unit) {
        val sid = getAccountSid()
        val token = getAuthToken()
        val from = getFromNumber()

        if (sid.isBlank() || token.isBlank() || from.isBlank()) {
            Log.e("TwilioManager", "Missing Twilio credentials, falling back to Device SMS")
            sendDirectSMS(to, message, onResult)
            return
        }

        val formBody = FormBody.Builder()
            .add("To", to)
            .add("From", from)
            .add("Body", message)
            .build()

        val credential = Credentials.basic(sid, token)

        val request = Request.Builder()
            .url(TWILIO_API_URL)
            .header("Authorization", credential)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TwilioManager", "Direct Twilio failed, falling back to Device SMS", e)
                sendDirectSMS(to, message, onResult)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    sendDirectSMS(to, message, onResult)
                }
                response.close()
            }
        })
    }

    private fun sendDirectSMS(phone: String, message: String, onResult: (Boolean) -> Unit) {
        // Check if we have SEND_SMS permission
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.SEND_SMS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val smsManager = android.telephony.SmsManager.getDefault()
                smsManager.sendTextMessage(phone, null, message, null, null)
                Log.d("TwilioManager", "Direct SMS sent successfully")
                onResult(true)
            } catch (e: Exception) {
                Log.e("TwilioManager", "Direct SMS failed, opening messaging app", e)
                openMessagingApp(phone, message)
                onResult(true) // User will send manually
            }
        } else {
            Log.w("TwilioManager", "SEND_SMS permission not granted, opening messaging app")
            openMessagingApp(phone, message)
            onResult(true) // User will send manually
        }
    }

    private fun openMessagingApp(phone: String, message: String) {
        try {
            val smsIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            Log.e("TwilioManager", "Could not open messaging app", e)
            // Last resort: try generic SMS intent
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("sms:$phone")
                    putExtra("sms_body", message)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("TwilioManager", "No messaging app available", e2)
            }
        }
    }
}
