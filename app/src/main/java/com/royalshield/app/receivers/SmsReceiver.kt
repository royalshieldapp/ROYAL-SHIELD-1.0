package com.royalshield.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.util.Patterns
import com.royalshield.app.VirusTotalRepository
import com.royalshield.app.util.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SMS Guard (PHASE 2.13-2.15)
 * Intercepts incoming SMS, extracts URLs, and scans them against VirusTotal.
 */
class SmsReceiver : BroadcastReceiver() {

    private val repository = VirusTotalRepository()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody
                val sender = sms.displayOriginatingAddress
                
                Log.d("SMS_GUARD", "Received SMS from $sender: $body")
                
                // Extract URLs using standard Android patterns
                val urls = extractUrls(body)
                
                if (urls.isNotEmpty()) {
                    Log.d("SMS_GUARD", "Found ${urls.size} URLs to scan: $urls")
                    scanAndNotify(context, urls, sender)
                }
            }
        }
    }

    private fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            urls.add(matcher.group())
        }
        return urls
    }

    private fun scanAndNotify(context: Context, urls: List<String>, sender: String) {
        scope.launch {
            for (url in urls) {
                try {
                    val result = repository.checkUrl(url)
                    Log.d("SMS_GUARD", "Scan result for $url: ${result.verdict}")
                    
                    if (result.verdict == "malicious" || result.verdict == "suspicious") {
                        // High Priority Alert
                        NotificationUtils.showSecurityAlert(
                            context = context,
                            title = "🚨 Phishing Link Detected!",
                            message = "The link from $sender is flagged as ${result.verdict.uppercase()}. Avoid clicking it.",
                            isCritical = result.verdict == "malicious"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SMS_GUARD", "Error scanning URL $url", e)
                }
            }
        }
    }
}
