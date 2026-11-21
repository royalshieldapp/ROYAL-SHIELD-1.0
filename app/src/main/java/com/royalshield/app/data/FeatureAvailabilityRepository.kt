package com.royalshield.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.royalshield.app.BuildConfig
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

fun interface FeatureAuthTokenProvider {
    suspend fun token(): String?
}

fun interface FeatureStatusRemoteDataSource {
    suspend fun fetch(authToken: String): String
}

class FeatureAvailabilityRepository(
    private val tokenProvider: FeatureAuthTokenProvider,
    private val remote: FeatureStatusRemoteDataSource,
    private val now: () -> Instant = Instant::now,
) {
    suspend fun refresh(): AdvancedFeatureAvailability {
        val token = runCatching { tokenProvider.token() }.getOrNull()
        if (token.isNullOrBlank()) {
            return AdvancedFeatureAvailability.unavailable("AUTH_REQUIRED")
        }

        val payload = try {
            remote.fetch(token)
        } catch (error: FeatureStatusHttpException) {
            val reason = if (error.statusCode == 401) "AUTH_REQUIRED" else "STATUS_UNREACHABLE"
            return AdvancedFeatureAvailability.unavailable(reason)
        } catch (_: IOException) {
            return AdvancedFeatureAvailability.unavailable("STATUS_UNREACHABLE")
        } catch (_: Exception) {
            return AdvancedFeatureAvailability.unavailable("STATUS_UNVERIFIED")
        }

        val root = runCatching { JsonParser.parseString(payload).asJsonObject }.getOrNull()
            ?: return AdvancedFeatureAvailability.unavailable("STATUS_UNVERIFIED")

        return AdvancedFeatureAvailability(
            vpn = mapModule(root, "vpn"),
            forecast = mapModule(root, "forecast"),
            threatMap = mapModule(root, "threatMap"),
            automations = mapModule(root, "automations"),
            reports = mapModule(root, "reports"),
            policies = mapModule(root, "policies"),
        )
    }

    private fun mapModule(root: JsonObject, name: String): AvailabilityState {
        val module = root.get(name)?.takeIf { it.isJsonObject }?.asJsonObject
            ?: return AvailabilityState.Unavailable("STATUS_UNVERIFIED")
        val status = module.stringOrNull("status")
            ?: return AvailabilityState.Unavailable("STATUS_UNVERIFIED")
        val reason = module.stringOrNull("reason") ?: "STATUS_UNVERIFIED"
        val freshness = module.stringOrNull("dataFreshness")
        val sources = module.getAsJsonArray("sources")
            ?.mapNotNull { element -> runCatching { element.asString }.getOrNull() }
            .orEmpty()

        return when (status) {
            "READY" -> {
                val evidenceTime = freshness?.let { runCatching { Instant.parse(it) }.getOrNull() }
                    ?: return AvailabilityState.Unavailable("STATUS_UNVERIFIED")
                val age = Duration.between(evidenceTime, now())
                if (age.isNegative || age > MAX_READY_AGE) {
                    AvailabilityState.Unavailable("STATUS_STALE")
                } else {
                    AvailabilityState.Ready(
                        dataFreshness = freshness,
                        sources = sources,
                        modelVersion = module.stringOrNull("modelVersion"),
                        confidence = module.get("confidence")
                            ?.takeUnless { it.isJsonNull }
                            ?.let { runCatching { it.asDouble }.getOrNull() },
                    )
                }
            }

            "DEGRADED" -> AvailabilityState.Degraded(reason, freshness, sources)
            "UNAVAILABLE" -> AvailabilityState.Unavailable(reason)
            else -> AvailabilityState.Unavailable("STATUS_UNVERIFIED")
        }
    }

    private fun JsonObject.stringOrNull(name: String): String? =
        get(name)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asString }.getOrNull() }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    companion object {
        private val MAX_READY_AGE: Duration = Duration.ofHours(24)

        fun createDefault(): FeatureAvailabilityRepository = FeatureAvailabilityRepository(
            tokenProvider = FirebaseFeatureAuthTokenProvider(),
            remote = OkHttpFeatureStatusRemoteDataSource(),
        )
    }
}

private class FirebaseFeatureAuthTokenProvider(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : FeatureAuthTokenProvider {
    override suspend fun token(): String? = auth.currentUser
        ?.getIdToken(false)
        ?.await()
        ?.token
}

private class OkHttpFeatureStatusRemoteDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
) : FeatureStatusRemoteDataSource {
    override suspend fun fetch(authToken: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${BuildConfig.API_BASE_URL.trimEnd('/')}/api/v1/features/advanced-status")
            .header("Authorization", "Bearer $authToken")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw FeatureStatusHttpException(response.code)
            response.body?.string() ?: throw IOException("Empty feature-status response")
        }
    }
}

private class FeatureStatusHttpException(val statusCode: Int) : IOException()
