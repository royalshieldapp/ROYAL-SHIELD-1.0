package com.royalshield.app.managers

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager for D-ID API interactions.
 * Handles text-to-video generation.
 */
object DidManager {
    private const val TAG = "DidManager"
    private const val BASE_URL = "https://api.d-id.com/"
    
    private fun getApiKey() = PreferencesManager.getDidApiKey() ?: ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val service = retrofit.create(DidApiService::class.java)

    suspend fun createTalk(text: String, sourceUrl: String = "https://create-images-results.d-id.com/DefaultPresenters/Emma_f/image.jpeg"): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = CreateTalkRequest(
                    sourceUrl = sourceUrl,
                    script = Script(
                        type = "text",
                        input = text,
                        provider = ScriptProvider(
                            type = "microsoft",
                            voiceId = "en-US-JennyNeural"
                        )
                    ),
                    config = Config(
                        fluent = true,
                        padAudio = 0.0
                    )
                )

                val response = service.createTalk("Basic ${getApiKey()}", request)
                Log.d(TAG, "Create Talk Response: $response")
                response.id
            } catch (e: Exception) {
                Log.e(TAG, "Error creating talk", e)
                null
            }
        }
    }

    suspend fun createAgentTalk(agentId: String, text: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = CreateAgentTalkRequest(
                    script = Script(
                        type = "text",
                        input = text,
                        provider = ScriptProvider(
                            type = "microsoft",
                            voiceId = "en-US-JennyNeural"
                        )
                    )
                )

                val response = service.createAgentTalk("Basic ${getApiKey()}", agentId, request)
                Log.d(TAG, "Create Agent Talk Response: $response")
                response.id
            } catch (e: Exception) {
                Log.e(TAG, "Error creating agent talk", e)
                null
            }
        }
    }

    suspend fun getTalkStatus(id: String): TalkStatusResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = service.getTalkStatus("Basic ${getApiKey()}", id)
                Log.d(TAG, "Get Talk Status: $response")
                response
            } catch (e: Exception) {
                Log.e(TAG, "Error getting talk status", e)
                null
            }
        }
    }

    // --- API Models ---

    interface DidApiService {
        @POST("talks")
        suspend fun createTalk(
            @Header("Authorization") auth: String,
            @Body request: CreateTalkRequest
        ): CreateTalkResponse

        @POST("agents/{agentId}/talks")
        suspend fun createAgentTalk(
            @Header("Authorization") auth: String,
            @Path("agentId") agentId: String,
            @Body request: CreateAgentTalkRequest
        ): CreateTalkResponse

        @GET("talks/{id}")
        suspend fun getTalkStatus(
            @Header("Authorization") auth: String,
            @Path("id") id: String
        ): TalkStatusResponse
    }

    @JsonClass(generateAdapter = true)
    data class CreateTalkRequest(
        @Json(name = "source_url") val sourceUrl: String,
        @Json(name = "script") val script: Script,
        @Json(name = "config") val config: Config
    )

    @JsonClass(generateAdapter = true)
    data class CreateAgentTalkRequest(
        @Json(name = "script") val script: Script
    )

    @JsonClass(generateAdapter = true)
    data class Script(
        @Json(name = "type") val type: String,
        @Json(name = "input") val input: String,
        @Json(name = "provider") val provider: ScriptProvider
    )

    @JsonClass(generateAdapter = true)
    data class ScriptProvider(
        @Json(name = "type") val type: String,
        @Json(name = "voice_id") val voiceId: String
    )

    @JsonClass(generateAdapter = true)
    data class Config(
        @Json(name = "fluent") val fluent: Boolean,
        @Json(name = "pad_audio") val padAudio: Double
    )

    @JsonClass(generateAdapter = true)
    data class CreateTalkResponse(
        @Json(name = "id") val id: String,
        @Json(name = "created_at") val createdAt: String?,
        @Json(name = "status") val status: String?
    )

    @JsonClass(generateAdapter = true)
    data class TalkStatusResponse(
        @Json(name = "user_id") val userId: String?,
        @Json(name = "kind") val kind: String?,
        @Json(name = "id") val id: String,
        @Json(name = "created_at") val createdAt: String?,
        @Json(name = "status") val status: String,
        @Json(name = "result_url") val resultUrl: String?,
        @Json(name = "metadata") val metadata: Map<String, Any>?
    )
}
