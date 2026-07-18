package com.royalshield.app.data

import com.royalshield.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class NetworkSpeedResult(
    val pingMs: Int,
    val downloadMbps: Float,
    val uploadMbps: Float
)

object NetworkSpeedTestRepository {
    private const val DOWNLOAD_BYTES = 2 * 1024 * 1024
    private const val UPLOAD_BYTES = 1024 * 1024
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun run(): NetworkSpeedResult = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
        val systemUrl = "$baseUrl/api/v1/system/speed-test"
        val ping = measurePing("$systemUrl/ping")
        val download = measureDownload("$systemUrl/download?bytes=$DOWNLOAD_BYTES")
        val upload = measureUpload("$systemUrl/upload")
        NetworkSpeedResult(ping, download, upload)
    }

    private fun measurePing(url: String): Int {
        val samples = (1..3).map {
            val started = System.nanoTime()
            client.newCall(Request.Builder().url(url).header("Cache-Control", "no-cache").build())
                .execute().use { response -> check(response.isSuccessful) { "Ping failed: ${response.code}" } }
            nanosToMillis(System.nanoTime() - started)
        }
        return samples.sorted()[samples.size / 2]
    }

    private fun measureDownload(url: String): Float {
        val started = System.nanoTime()
        val bytes = client.newCall(Request.Builder().url(url).header("Cache-Control", "no-cache").build())
            .execute().use { response ->
                check(response.isSuccessful) { "Download failed: ${response.code}" }
                response.body?.bytes()?.size ?: error("Empty download response")
            }
        check(bytes == DOWNLOAD_BYTES) { "Incomplete download payload" }
        return megabitsPerSecond(bytes, System.nanoTime() - started)
    }

    private fun measureUpload(url: String): Float {
        val payload = ByteArray(UPLOAD_BYTES)
        val body = payload.toRequestBody("application/octet-stream".toMediaType())
        val started = System.nanoTime()
        client.newCall(Request.Builder().url(url).post(body).build()).execute().use { response ->
            check(response.isSuccessful) { "Upload failed: ${response.code}" }
        }
        return megabitsPerSecond(payload.size, System.nanoTime() - started)
    }

    private fun nanosToMillis(nanos: Long) = (nanos / 1_000_000.0).roundToInt().coerceAtLeast(1)

    private fun megabitsPerSecond(bytes: Int, nanos: Long): Float {
        val seconds = nanos / 1_000_000_000.0
        return ((bytes * 8.0) / seconds / 1_000_000.0).toFloat()
    }
}
