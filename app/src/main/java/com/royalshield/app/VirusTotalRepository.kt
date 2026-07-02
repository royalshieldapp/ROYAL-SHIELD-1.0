package com.royalshield.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.royalshield.app.managers.PreferencesManager

class VirusTotalRepository {

    private val client = OkHttpClient()
    private val baseUrl = "https://www.virustotal.com/api/v3"

    private fun getApiKey(): String? {
        return PreferencesManager.getVirusTotalApiKey()
    }

    suspend fun checkUrl(url: String): UrlResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext UrlResult("unknown", 0, "API Key missing. Configure in Settings.")
        }

        try {
            // 1. Submit URL
            val formBody = "url=$url".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val submitRequest = Request.Builder()
                .url("$baseUrl/urls")
                .post(formBody)
                .addHeader("x-apikey", apiKey)
                .build()

            val submitResponse = client.newCall(submitRequest).execute()
            if (!submitResponse.isSuccessful) {
                return@withContext UrlResult("unknown", 0, "Error submitting URL: ${submitResponse.code}")
            }
            
            val submitJson = JSONObject(submitResponse.body?.string() ?: "{}")
            val analysisId = submitJson.optJSONObject("data")?.optString("id")

            if (analysisId.isNullOrEmpty()) {
                return@withContext UrlResult("unknown", 0, "No analysis ID returned")
            }

            // 2. Poll for results (Simplified: just wait a bit and check once, or check URL directly if cached)
            // For a better UX, we might want to just get the URL report if it exists, 
            // but the proper flow is Submit -> Poll. 
            // To keep it responsive for this demo, let's try to get the URL report directly using the base64 ID.
            
            val urlId = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(url.toByteArray())
            val reportRequest = Request.Builder()
                .url("$baseUrl/urls/$urlId")
                .get()
                .addHeader("x-apikey", apiKey)
                .build()

            val reportResponse = client.newCall(reportRequest).execute()
            if (reportResponse.isSuccessful) {
                val reportJson = JSONObject(reportResponse.body?.string() ?: "{}")
                val attributes = reportJson.optJSONObject("data")?.optJSONObject("attributes")
                val stats = attributes?.optJSONObject("last_analysis_stats")
                
                return@withContext verdictFromStats(stats)
            } else {
                 // If 404, it means it's new. We should poll the analysis ID. 
                 // For this demo, let's just return a "Pending/Unknown" or mock it if we can't wait.
                 return@withContext UrlResult("unknown", 50, "Analysis pending or not found")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext UrlResult("unknown", 0, "Network error: ${e.message}")
        }
    }

    suspend fun scanFileHash(hash: String): ScanResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext ScanResult("API Key missing")
        }

        try {
            val request = Request.Builder()
                .url("$baseUrl/files/$hash")
                .get()
                .addHeader("x-apikey", apiKey)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ScanResult("unknown") // Treat 404 as unknown/clean for now
            }

            val json = JSONObject(response.body?.string() ?: "{}")
            val attributes = json.optJSONObject("data")?.optJSONObject("attributes")
            val stats = attributes?.optJSONObject("last_analysis_stats")
            
            val verdict = verdictFromStats(stats)
            val severity = when (verdict.verdict) {
                "malicious" -> "high"
                "suspicious" -> "medium"
                else -> "low"
            }
            
            return@withContext ScanResult(severity)

        } catch (e: Exception) {
            return@withContext ScanResult("error")
        }
    }

    suspend fun uploadFile(file: java.io.File): ScanResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext ScanResult("API Key missing")
        }

        try {
            // 1. Upload File
            val requestBody = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("file", file.name,
                    file.asRequestBody("application/octet-stream".toMediaType()))
                .build()

            val request = Request.Builder()
                .url("$baseUrl/files")
                .post(requestBody)
                .addHeader("x-apikey", apiKey)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ScanResult("Upload failed: ${response.code}")
            }

            val json = JSONObject(response.body?.string() ?: "{}")
            val analysisId = json.optJSONObject("data")?.optString("id")

            if (analysisId.isNullOrEmpty()) {
                return@withContext ScanResult("No analysis ID")
            }

            // 2. Poll for results (Simplified: wait a bit and check)
            // In a real app, you should poll periodically. Here we wait 5s and check once.
            kotlinx.coroutines.delay(5000)

            val reportRequest = Request.Builder()
                .url("$baseUrl/analyses/$analysisId")
                .get()
                .addHeader("x-apikey", apiKey)
                .build()

            val reportResponse = client.newCall(reportRequest).execute()
            if (!reportResponse.isSuccessful) {
                return@withContext ScanResult("Analysis pending")
            }

            val reportJson = JSONObject(reportResponse.body?.string() ?: "{}")
            val attributes = reportJson.optJSONObject("data")?.optJSONObject("attributes")
            val stats = attributes?.optJSONObject("stats") // Analysis object uses 'stats', not 'last_analysis_stats'

            val verdict = verdictFromStats(stats)
            val severity = when (verdict.verdict) {
                "malicious" -> "high"
                "suspicious" -> "medium"
                else -> "low"
            }

            return@withContext ScanResult(severity)

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext ScanResult("Upload error: ${e.message}")
        }
    }

    fun isFileUploadEnabled(): Boolean {
        // Check if API key is present and maybe some other logic
        return !getApiKey().isNullOrBlank()
    }

    private fun verdictFromStats(stats: JSONObject?): UrlResult {
        if (stats == null) return UrlResult("unknown", 50, "No stats")

        val malicious = stats.optInt("malicious", 0)
        val suspicious = stats.optInt("suspicious", 0)
        val harmless = stats.optInt("harmless", 0)
        val undetected = stats.optInt("undetected", 0)

        return when {
            malicious > 0 -> UrlResult("malicious", 100, "$malicious engines detected malware")
            suspicious > 0 -> UrlResult("suspicious", 60, "$suspicious engines suspicious")
            harmless > 0 -> UrlResult("safe", 0, "Clean")
            else -> UrlResult("unknown", 50, "No data")
        }
    }
}

data class UrlResult(val verdict: String, val score: Int, val reason: String)
data class ScanResult(val severity: String)

fun calculateSHA256(file: java.io.File): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val fis = java.io.FileInputStream(file)
    val buffer = ByteArray(8192)
    var bytesRead: Int
    while (fis.read(buffer).also { bytesRead = it } != -1) {
        digest.update(buffer, 0, bytesRead)
    }
    fis.close()
    return digest.digest().joinToString("") { "%02x".format(it) }
}
