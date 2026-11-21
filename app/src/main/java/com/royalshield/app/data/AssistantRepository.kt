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
    private val endpoint = "${com.royalshield.app.BuildConfig.API_BASE_URL.trimEnd('/')}/api/assistant/chat"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessage(
        message: String,
        history: List<AssistantHistoryEntry>
    ): AssistantRepositoryResult = withContext(Dispatchers.IO) {
        val normalizedMessage = message.trim()
        if (normalizedMessage.isEmpty() || normalizedMessage.length > MAX_MESSAGE_LENGTH) {
            return@withContext AssistantRepositoryResult.Error(
                "El mensaje debe tener entre 1 y $MAX_MESSAGE_LENGTH caracteres."
            )
        }

        val user = auth.currentUser
            ?: return@withContext AssistantRepositoryResult.Error(
                "Inicia sesión para usar Royal Agent."
            )

        val token = try {
            user.getIdToken(false).await().token
        } catch (_: Exception) {
            null
        }
        if (token.isNullOrBlank()) {
            return@withContext AssistantRepositoryResult.Error(
                "No se pudo validar tu sesión. Vuelve a iniciar sesión."
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
                        AssistantRepositoryResult.Error("El asistente devolvió una respuesta vacía.")
                    }
                } else {
                    AssistantRepositoryResult.Error(userFacingError(response.code, json))
                }
            }
        } catch (_: IOException) {
            AssistantRepositoryResult.Error(
                "No se pudo conectar con Royal Agent. Revisa tu conexión e inténtalo de nuevo."
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
            401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
            422 -> serverMessage.ifBlank { "El mensaje no pudo procesarse." }
            429 -> "Has alcanzado el límite temporal del asistente. Inténtalo en un minuto."
            503 -> serverMessage.ifBlank { "El servicio de IA todavía no está configurado." }
            else -> "Royal Agent no está disponible temporalmente ($statusCode)."
        }
    }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 4_000
        const val MAX_HISTORY_ENTRIES = 10
    }
}
