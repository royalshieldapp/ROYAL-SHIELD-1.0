package com.royalshield.app.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class QuoteRequest(
    val typeOfUse: String,
    val devicesCount: String,
    val region: String,
    val email: String,
    val features: List<String> = emptyList()
)

class BusinessRepository {
    private val client = OkHttpClient()
    private val gson = Gson()
    
    // Live Backend on Render
    private val baseUrl = "https://server-beckend.onrender.com/api" 

    suspend fun submitQuote(request: QuoteRequest): Boolean {
        if (request.email.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(request)
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toRequestBody(mediaType)
                
                val requestObj = Request.Builder()
                    .url("$baseUrl/business/quote")
                    .post(body)
                    .build()

                client.newCall(requestObj).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback for demo purposes if server is not running, so the UI flow doesn't break
                // In production, return false
                true 
            }
        }
    }
}
