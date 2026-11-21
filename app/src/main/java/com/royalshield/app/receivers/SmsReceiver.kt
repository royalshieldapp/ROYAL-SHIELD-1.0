package com.royalshield.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.royalshield.app.data.AdvancedScanResult
import com.royalshield.app.data.AdvancedUrlScanRepository
import com.royalshield.app.models.PhishingRiskLevel
import com.royalshield.app.securitysetup.EncryptedSecuritySetupStore
import com.royalshield.app.services.PhishingDetectionService
import com.royalshield.app.util.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SMS Guard (PHASE 2.13-2.15)
 * Receives incoming SMS after explicit opt-in, analyzes locally, and optionally
 * submits extracted URLs to the authenticated advanced scanner.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val setup = EncryptedSecuritySetupStore(context).load(uid)
        if (setup.smsPhishingEnabled != true) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localAnalyzer = PhishingDetectionService()
                val advancedScanner = AdvancedUrlScanRepository.createDefault()
                for (sms in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    val body = sms.displayMessageBody.orEmpty()
                    val urls = extractUrls(body)
                    Log.d("SMS_GUARD", SmsPrivacy.receivedMessage(urls.size))

                    val localResult = localAnalyzer.analyzeText(body)
                    if (localResult.riskLevel == PhishingRiskLevel.HIGH) {
                        NotificationUtils.showSecurityAlert(
                            context = context,
                            title = "Posible phishing detectado",
                            message = "Royal Shield detectó indicadores de alto riesgo en un SMS recibido.",
                            isCritical = true,
                        )
                    }

                    urls.forEach { url ->
                        when (advancedScanner.submit(url)) {
                            is AdvancedScanResult.Submitted ->
                                Log.d("SMS_GUARD", SmsPrivacy.scanCompleted("SUBMITTED"))
                            is AdvancedScanResult.Unavailable ->
                                Log.d("SMS_GUARD", SmsPrivacy.scanCompleted("UNAVAILABLE"))
                            is AdvancedScanResult.Rejected ->
                                Log.d("SMS_GUARD", SmsPrivacy.scanCompleted("REJECTED"))
                        }
                    }
                }
            } catch (error: Exception) {
                Log.e("SMS_GUARD", "SMS protection unavailable", error)
            } finally {
                pendingResult.finish()
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

}
