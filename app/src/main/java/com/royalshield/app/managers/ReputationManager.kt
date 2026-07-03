package com.royalshield.app.managers

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ReputationResult(
    val number: String,
    val score: Int, // 0-100 (100 = Safe)
    val status: ReputationStatus,
    val carrier: String = "Unknown",
    val country: String = "Unknown",
    val tags: List<String> = emptyList(),
    val valid: Boolean = true,
    val localFormat: String = "",
    val internationalFormat: String = "",
    val countryPrefix: String = "",
    val countryCode: String = "",
    val location: String = "",
    val lineType: String = "unknown",
    val riskLevel: String = "LOW",
    val reportCount: Int = 0,
    val lastReported: String? = null,
    val source: String = "local"
)

enum class ReputationStatus {
    SAFE, CAUTION, SPAM, MALICIOUS
}

/**
 * API response model matching the backend JSON structure.
 */
private data class PhoneCheckApiResponse(
    val valid: Boolean = true,
    val number: String = "",
    val localFormat: String = "",
    val internationalFormat: String = "",
    val countryPrefix: String = "",
    val countryCode: String = "",
    val countryName: String = "",
    val location: String = "",
    val carrier: String = "Unknown",
    val lineType: String = "unknown",
    val score: Int = 50,
    val status: String = "SAFE",
    val tags: List<String> = emptyList(),
    val riskLevel: String = "LOW",
    val reportCount: Int = 0,
    val lastReported: String? = null,
    val source: String = "mock",
    val checkedAt: String? = null,
    val error: String? = null
)

object ReputationManager {

    // Royal Shield Backend URL
    private const val BASE_URL = "https://server-beckend.onrender.com/api"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Checks a phone number against the Royal Shield backend.
     * Falls back to local mock logic if the backend is unavailable.
     */
    suspend fun checkPhoneNumber(number: String): ReputationResult {
        return withContext(Dispatchers.IO) {
            try {
                checkViaBackend(number)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to local mock if backend is unavailable
                checkLocalFallback(number)
            }
        }
    }

    /**
     * Calls the Royal Shield backend API: GET /api/phone/check?number=...
     */
    private fun checkViaBackend(number: String): ReputationResult {
        val encodedNumber = java.net.URLEncoder.encode(number, "UTF-8")
        val url = "$BASE_URL/phone/check?number=$encodedNumber"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Backend returned ${response.code}")
            }

            val body = response.body?.string()
                ?: throw Exception("Empty response body")

            val apiResponse = gson.fromJson(body, PhoneCheckApiResponse::class.java)

            if (apiResponse.error != null) {
                throw Exception("Backend error: ${apiResponse.error}")
            }

            val status = try {
                ReputationStatus.valueOf(apiResponse.status.uppercase())
            } catch (_: Exception) {
                ReputationStatus.SAFE
            }

            return ReputationResult(
                number = apiResponse.number.ifEmpty { number },
                score = apiResponse.score.coerceIn(0, 100),
                status = status,
                carrier = apiResponse.carrier,
                country = apiResponse.countryName.ifEmpty { apiResponse.countryCode },
                tags = apiResponse.tags,
                valid = apiResponse.valid,
                localFormat = apiResponse.localFormat,
                internationalFormat = apiResponse.internationalFormat,
                countryPrefix = apiResponse.countryPrefix,
                countryCode = apiResponse.countryCode,
                location = apiResponse.location,
                lineType = apiResponse.lineType,
                riskLevel = apiResponse.riskLevel,
                reportCount = apiResponse.reportCount,
                lastReported = apiResponse.lastReported,
                source = apiResponse.source
            )
        }
    }

    /**
     * Local mock fallback when backend is not available.
     * Provides realistic-looking data for demonstration.
     */
    private suspend fun checkLocalFallback(number: String): ReputationResult {
        delay(800) // Simulate processing

        return when {
            number.endsWith("666") -> ReputationResult(
                number = number,
                score = 10,
                status = ReputationStatus.MALICIOUS,
                carrier = "VoIP Provider",
                country = "Unknown",
                tags = listOf("Scam", "IRS Impersonation", "Spoofed Number"),
                valid = true,
                lineType = "voip",
                riskLevel = "CRITICAL",
                reportCount = 347,
                lastReported = "2025-12-01T14:30:00Z",
                location = "Unknown",
                countryCode = "US",
                countryPrefix = "+1",
                source = "local"
            )
            number.endsWith("000") -> ReputationResult(
                number = number,
                score = 35,
                status = ReputationStatus.SPAM,
                carrier = "T-Mobile",
                country = "United States",
                tags = listOf("Telemarketing", "Robocall", "Automated"),
                valid = true,
                lineType = "mobile",
                riskLevel = "HIGH",
                reportCount = 89,
                lastReported = "2025-11-28T09:15:00Z",
                location = "Dallas, TX",
                countryCode = "US",
                countryPrefix = "+1",
                source = "local"
            )
            number.startsWith("+1800") -> ReputationResult(
                number = number,
                score = 90,
                status = ReputationStatus.SAFE,
                carrier = "Business Line",
                country = "United States",
                tags = listOf("Verified Business", "Customer Service"),
                valid = true,
                lineType = "toll_free",
                riskLevel = "LOW",
                reportCount = 0,
                lastReported = null,
                location = "Nationwide",
                countryCode = "US",
                countryPrefix = "+1",
                source = "local"
            )
            else -> ReputationResult(
                number = number,
                score = 92,
                status = ReputationStatus.SAFE,
                carrier = "Verizon Wireless",
                country = "United States",
                tags = listOf("Personal Mobile", "No Reports"),
                valid = true,
                lineType = "mobile",
                riskLevel = "LOW",
                reportCount = 0,
                lastReported = null,
                location = "New York, NY",
                countryCode = "US",
                countryPrefix = "+1",
                source = "local"
            )
        }
    }
}
