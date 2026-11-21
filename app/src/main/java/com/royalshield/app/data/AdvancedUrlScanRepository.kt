package com.royalshield.app.data

import com.google.firebase.auth.FirebaseAuth
import com.royalshield.app.BuildConfig
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

fun interface AdvancedScanTokenProvider {
    suspend fun token(): String?
}

fun interface AdvancedUrlScanRemote {
    suspend fun submit(url: String, token: String): String
}

sealed interface AdvancedScanResult {
    data class Submitted(val analysisId: String) : AdvancedScanResult
    data class Unavailable(val reason: String) : AdvancedScanResult
    data class Rejected(val reason: String) : AdvancedScanResult
}

class AdvancedUrlScanRepository(
    private val tokenProvider: AdvancedScanTokenProvider,
    private val remote: AdvancedUrlScanRemote,
) {
    suspend fun submit(url: String): AdvancedScanResult {
        if (!isHttpUrl(url)) return AdvancedScanResult.Rejected("INVALID_URL")
        val token = runCatching { tokenProvider.token() }.getOrNull()
        if (token.isNullOrBlank()) return AdvancedScanResult.Unavailable("AUTH_REQUIRED")

        return try {
            AdvancedScanResult.Submitted(remote.submit(url, token))
        } catch (error: AdvancedScanHttpException) {
            val reason = when (error.statusCode) {
                401 -> "AUTH_REQUIRED"
                503 -> "SCANNER_UNAVAILABLE"
                else -> "SCAN_REQUEST_FAILED"
            }
            AdvancedScanResult.Unavailable(reason)
        } catch (_: Exception) {
            AdvancedScanResult.Unavailable("SCAN_REQUEST_FAILED")
        }
    }

    private fun isHttpUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    companion object {
        fun createDefault(): AdvancedUrlScanRepository = AdvancedUrlScanRepository(
            tokenProvider = AdvancedScanTokenProvider {
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            },
            remote = OkHttpAdvancedUrlScanRemote(),
        )
    }
}

private class OkHttpAdvancedUrlScanRemote(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) : AdvancedUrlScanRemote {
    override suspend fun submit(url: String, token: String): String = withContext(Dispatchers.IO) {
        val json = JSONObject().put("url", url).toString()
        val request = Request.Builder()
            .url("${BuildConfig.API_BASE_URL.trimEnd('/')}/api/v1/scan/url")
            .header("Authorization", "Bearer $token")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw AdvancedScanHttpException(response.code)
            val body = response.body?.string() ?: throw IOException("Empty response")
            JSONObject(body).optString("analysisId").takeIf(String::isNotBlank)
                ?: throw IOException("Missing analysis id")
        }
    }
}

private class AdvancedScanHttpException(val statusCode: Int) : IOException()
