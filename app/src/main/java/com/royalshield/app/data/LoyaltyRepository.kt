package com.royalshield.app.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object LoyaltyRepository {
    private const val TAG = "LoyaltyRepository"
    private val baseUrl = com.royalshield.app.BuildConfig.LOYALTY_API_URL.trimEnd('/')
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private fun withIdToken(onToken: (String) -> Unit, onFailure: () -> Unit) {
        val user = auth.currentUser ?: return onFailure()
        user.getIdToken(false).addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result?.token else null
            if (token.isNullOrBlank()) {
                Log.w(TAG, "Firebase authentication token unavailable")
                onFailure()
            } else {
                onToken(token)
            }
        }
    }

    fun getStatus(onResult: (Boolean, Int?, String?, String?) -> Unit) {
        withIdToken(
            onFailure = { onResult(false, null, null, "Authentication required") },
            onToken = { token ->
                val request = Request.Builder()
                    .url("$baseUrl/status")
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()
                execute(request) { success, json, error ->
                    if (!success || json == null) {
                        onResult(false, null, null, error)
                    } else {
                        onResult(true, json.optInt("points", 0), json.optString("tier", "Bronze"), null)
                    }
                }
            },
        )
    }

    fun claimDailyLogin(onResult: (Boolean, Int?, String?, Boolean, String?) -> Unit) {
        withIdToken(
            onFailure = { onResult(false, null, null, false, "Authentication required") },
            onToken = { token ->
                val payload = JSONObject().put("action", "daily_login")
                val request = Request.Builder()
                    .url("$baseUrl/claims")
                    .header("Authorization", "Bearer $token")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                execute(request) { success, json, error ->
                    if (!success || json == null) {
                        onResult(false, null, null, false, error)
                    } else {
                        onResult(
                            true,
                            json.optInt("newTotal", 0),
                            json.optString("currentTier", "Bronze"),
                            json.optBoolean("duplicate", false),
                            null,
                        )
                    }
                }
            },
        )
    }

    fun redeemReward(
        rewardId: String,
        idempotencyKey: String,
        onResult: (Boolean, Int?, String?, String?, String?) -> Unit,
    ) {
        withIdToken(
            onFailure = { onResult(false, null, null, null, "Authentication required") },
            onToken = { token ->
                val payload = JSONObject()
                    .put("rewardId", rewardId)
                    .put("idempotencyKey", idempotencyKey)
                val request = Request.Builder()
                    .url("$baseUrl/redeem")
                    .header("Authorization", "Bearer $token")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                execute(request) { success, json, error ->
                    if (!success || json == null) {
                        onResult(false, null, null, null, error)
                    } else {
                        onResult(
                            true,
                            json.optInt("newTotal", 0),
                            json.optString("currentTier", "Bronze"),
                            json.optString("status", "PENDING"),
                            null,
                        )
                    }
                }
            },
        )
    }

    private fun execute(
        request: Request,
        onResult: (Boolean, JSONObject?, String?) -> Unit,
    ) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                Log.w(TAG, "Loyalty request failed: ${error.javaClass.simpleName}")
                onResult(false, null, "Loyalty service unavailable")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    if (it.isSuccessful && json != null) {
                        onResult(true, json, null)
                    } else {
                        val detail = json?.optString("detail")?.takeIf(String::isNotBlank)
                            ?: "Loyalty request failed (${it.code})"
                        onResult(false, null, detail)
                    }
                }
            }
        })
    }
}
