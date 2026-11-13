package com.royalshield.app.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.royalshield.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ThreatBriefingItem(
    val id: String,
    val title: String,
    val description: String,
    val author: String,
    val date: String,
    val tags: List<String>,
    val indicatorsCount: Int,
    val targetedCountries: List<String>
)

class ThreatBriefingRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')
) {
    private val gson = Gson()

    suspend fun fetchLiveBriefings(limit: Int = 10): Result<List<ThreatBriefingItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/v1/threats/otx"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RoyalShield-Android/1.3")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("ThreatBriefingRepo", "Failed to fetch threat briefings: HTTP ${response.code}")
                return@withContext Result.failure(Exception("HTTP ${response.code}: Live threat intelligence feed unavailable"))
            }

            val body = response.body?.string()
            if (body.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Empty response from threat intelligence server"))
            }

            val otxResponse = gson.fromJson(body, OtxBackendResponse::class.java)
            val results = otxResponse?.results.orEmpty()

            if (results.isEmpty()) {
                return@withContext Result.failure(Exception("No active threat intelligence pulses found"))
            }

            val briefings = results.take(limit).map { pulse ->
                val rawDate = pulse.created ?: pulse.modified ?: ""
                val formattedDate = if (rawDate.length >= 10) rawDate.substring(0, 10) else rawDate
                ThreatBriefingItem(
                    id = pulse.id ?: "pulse_${System.currentTimeMillis()}",
                    title = pulse.name?.trim()?.takeIf { it.isNotEmpty() } ?: "Unclassified Threat Pulse",
                    description = pulse.description?.trim().orEmpty(),
                    author = pulse.authorName ?: "AlienVault OTX",
                    date = formattedDate,
                    tags = pulse.tags.orEmpty().filter { it.isNotBlank() },
                    indicatorsCount = pulse.indicators?.size ?: 0,
                    targetedCountries = pulse.targetedCountries.orEmpty()
                )
            }

            Result.success(briefings)
        } catch (e: Exception) {
            Log.e("ThreatBriefingRepo", "Error fetching threat briefings", e)
            Result.failure(e)
        }
    }
}

private data class OtxBackendResponse(
    val status: String? = null,
    val source: String? = null,
    val results: List<OtxBackendPulse>? = null
)

private data class OtxBackendPulse(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    @SerializedName("author_name") val authorName: String? = null,
    val created: String? = null,
    val modified: String? = null,
    val tags: List<String>? = null,
    @SerializedName("targeted_countries") val targetedCountries: List<String>? = null,
    val indicators: List<OtxBackendIndicator>? = null
)

private data class OtxBackendIndicator(
    val id: Long? = null,
    val indicator: String? = null,
    val type: String? = null,
    val title: String? = null,
    val description: String? = null
)

