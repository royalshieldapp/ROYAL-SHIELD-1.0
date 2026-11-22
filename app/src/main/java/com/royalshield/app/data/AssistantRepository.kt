package com.royalshield.app.data

import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class AssistantHistoryEntry(
    val role: String,
    val content: String
)

sealed interface AssistantRepositoryResult {
    data class Success(val reply: String) : AssistantRepositoryResult
    data class Error(val message: String) : AssistantRepositoryResult
}

class AssistantRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    private val endpoint = if (com.royalshield.app.subscription.EntitlementStore.DEV_SUPERPOWERS) {
        "http://10.0.2.2:8000/api/assistant/chat"
    } else {
        "${com.royalshield.app.BuildConfig.API_BASE_URL.trimEnd('/')}/api/assistant/chat"
    }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessage(
        message: String,
        history: List<AssistantHistoryEntry>
    ): AssistantRepositoryResult = sendMessage(message, history, null)

    suspend fun sendMessage(
        message: String,
        history: List<AssistantHistoryEntry>,
        systemInstruction: String?
    ): AssistantRepositoryResult = withContext(Dispatchers.IO) {
        val normalizedMessage = message.trim()
        if (normalizedMessage.isEmpty() || normalizedMessage.length > MAX_MESSAGE_LENGTH) {
            return@withContext AssistantRepositoryResult.Error(
                "Message must be between 1 and $MAX_MESSAGE_LENGTH characters."
            )
        }

        val user = auth.currentUser
        val token = user?.let {
            try {
                it.getIdToken(false).await().token
            } catch (_: Exception) {
                null
            }
        } ?: if (com.royalshield.app.subscription.EntitlementStore.DEV_SUPERPOWERS) "dev-superpowers-token" else {
            return@withContext AssistantRepositoryResult.Error(
                "Sign in to use Royal Agent."
            )
        }
        if (token.isBlank()) {
            return@withContext AssistantRepositoryResult.Error(
                "Unable to validate session. Please sign in again."
            )
        }

        val payload = JSONObject()
            .put("message", normalizedMessage)
            .put(
                "history",
                JSONArray().apply {
                    history.takeLast(MAX_HISTORY_ENTRIES).forEach { entry ->
                        put(
                            JSONObject()
                                .put("role", entry.role)
                                .put("content", entry.content.take(MAX_MESSAGE_LENGTH))
                        )
                    }
                }
            )

        if (!systemInstruction.isNullOrBlank()) {
            payload.put("systemInstruction", systemInstruction)
        }

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(responseBody) }.getOrNull()
                if (response.isSuccessful) {
                    val reply = json?.optString("reply").orEmpty().trim()
                    if (reply.isNotEmpty()) {
                        AssistantRepositoryResult.Success(reply)
                    } else {
                        AssistantRepositoryResult.Error("The assistant returned an empty response.")
                    }
                } else {
                    AssistantRepositoryResult.Error(userFacingError(response.code, json))
                }
            }
        } catch (_: IOException) {
            AssistantRepositoryResult.Error(
                "Unable to connect to Royal Agent. Please check your connection and try again."
            )
        }
    }

    private fun userFacingError(statusCode: Int, json: JSONObject?): String {
        val detail = json?.opt("detail")
        val serverMessage = when (detail) {
            is JSONObject -> detail.optString("message")
            is String -> detail
            else -> ""
        }.trim()

        return when (statusCode) {
            401 -> "Your session has expired. Please sign in again."
            422 -> serverMessage.ifBlank { "The message could not be processed." }
            429 -> "You have reached the temporary rate limit. Please try again in a minute."
            503 -> serverMessage.ifBlank { "The AI service is not yet configured." }
            else -> "Royal Agent is temporarily unavailable ($statusCode)."
        }
    }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 4_000
        const val MAX_HISTORY_ENTRIES = 10
    }
}
