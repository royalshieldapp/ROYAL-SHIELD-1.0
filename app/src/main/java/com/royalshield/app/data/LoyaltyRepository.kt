package com.royalshield.app.data

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object LoyaltyRepository {
    private const val TAG = "LoyaltyRepository"
    private val BASE_URL = com.royalshield.app.BuildConfig.LOYALTY_API_URL
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()
    private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()

    fun syncPoints(points: Int, action: String = "app_activity", onResult: (Boolean, Int?) -> Unit) {
        val payload = JSONObject().apply {
            put("points", points)
            put("action", action)
        }

        val body = payload.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("$BASE_URL/points")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Sync failed", e)
                onResult(false, null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    val newTotal = json.optInt("newTotal", -1)
                    Log.d(TAG, "Sync Success: $newTotal points")
                    onResult(true, newTotal)
                } else {
                    Log.e(TAG, "Sync Error: ${response.code}")
                    onResult(false, null)
                }
                response.close()
            }
        })
    }

    fun getStatus(onResult: (Boolean, Int?, String?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/status")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Status failed for $BASE_URL", e)
                onResult(false, null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    val points = json.optInt("points", 0)
                    val tier = json.optString("tier", "Bronze")
                    onResult(true, points, tier)
                } else {
                    Log.e(TAG, "Status error: ${response.code}")
                    onResult(false, null, null)
                }
                response.close()
            }
        })
    }
}
