package com.royalshield.app.billing

import com.android.billingclient.api.Purchase
import com.google.firebase.auth.FirebaseAuth
import com.royalshield.app.BuildConfig
import com.royalshield.app.subscription.SubscriptionTier
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest

object BillingVerificationDecision {
    fun obfuscatedAccountId(uid: String): String = MessageDigest.getInstance("SHA-256")
        .digest(uid.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    fun tierFor(status: String?, verified: Boolean, tier: String?): SubscriptionTier? {
        if (status != "READY" || !verified) return null
        return when (tier) {
            "STARTER" -> SubscriptionTier.STARTER
            "GOLD" -> SubscriptionTier.GOLD
            "ULTIMATE" -> SubscriptionTier.ULTIMATE
            else -> null
        }
    }
}

sealed class BillingVerificationResult {
    data class Verified(val tier: SubscriptionTier, val acknowledged: Boolean) : BillingVerificationResult()
    data class Rejected(val message: String) : BillingVerificationResult()
}

fun interface BillingVerifier {
    fun verify(purchase: Purchase, callback: (BillingVerificationResult) -> Unit)
}

class RenderBillingVerifier(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val client: OkHttpClient = OkHttpClient(),
) : BillingVerifier {
    override fun verify(purchase: Purchase, callback: (BillingVerificationResult) -> Unit) {
        val user = auth.currentUser
        val productId = purchase.products.singleOrNull()
        if (user == null || productId == null) {
            callback(BillingVerificationResult.Rejected("Authentication and one valid product are required"))
            return
        }

        user.getIdToken(true)
            .addOnFailureListener { callback(BillingVerificationResult.Rejected("Could not authenticate purchase")) }
            .addOnSuccessListener { tokenResult ->
                val idToken = tokenResult.token
                if (idToken.isNullOrBlank()) {
                    callback(BillingVerificationResult.Rejected("Could not authenticate purchase"))
                    return@addOnSuccessListener
                }
                val json = JSONObject()
                    .put("purchaseToken", purchase.purchaseToken)
                    .put("productId", productId)
                    .put("packageName", BuildConfig.APPLICATION_ID)
                val request = Request.Builder()
                    .url("${BuildConfig.API_BASE_URL.trimEnd('/')}/api/billing/verify")
                    .header("Authorization", "Bearer $idToken")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, error: IOException) {
                        callback(BillingVerificationResult.Rejected("Billing verification is unavailable"))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            val body = response.body?.string().orEmpty()
                            if (!response.isSuccessful) {
                                callback(BillingVerificationResult.Rejected("Purchase was not verified"))
                                return
                            }
                            try {
                                val jsonBody = JSONObject(body)
                                val entitlement = jsonBody.getJSONObject("entitlement")
                                val tier = BillingVerificationDecision.tierFor(
                                    jsonBody.optString("status"),
                                    entitlement.optBoolean("verified", false),
                                    entitlement.optString("tier"),
                                )
                                if (tier == null) {
                                    callback(BillingVerificationResult.Rejected("Purchase was not verified"))
                                } else {
                                    callback(
                                        BillingVerificationResult.Verified(
                                            tier,
                                            entitlement.optBoolean("acknowledged", false),
                                        )
                                    )
                                }
                            } catch (_: Exception) {
                                callback(BillingVerificationResult.Rejected("Invalid billing response"))
                            }
                        }
                    }
                })
            }
    }
}
