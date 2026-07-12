package com.royalshield.app.vpn

import android.util.Log
import com.royalshield.app.BuildConfig
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class VpnBackendStatus(
    val available: Boolean,
    val status: String,
    val message: String,
    val provider: String?
)

data class VpnServerProfile(
    val id: String,
    val name: String,
    val countryCode: String = "",
    val host: String = ""
)

class VpnConfigurationException(
    val code: String,
    override val message: String
) : IOException(message)

/**
 * Fetches WireGuard VPN status and profiles from the Royal Shield backend.
 * This class intentionally does not provide fallback/mock configs.
 */
class VpnProfileRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

    suspend fun getVpnStatus(): Result<VpnBackendStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val json = executeJson(
                Request.Builder()
                    .url("$baseUrl/api/vpn/status")
                    .get()
                    .build()
            )
            val status = json.optString("status", "not_configured")
            val message = json.optString(
                "message",
                if (status == "available") "VPN service available" else "VPN service is not configured"
            )
            VpnBackendStatus(
                available = status == "available",
                status = status,
                message = message,
                provider = json.optString("provider").ifBlank { null }
            )
        }.onFailure {
            Log.e(TAG, "Failed to fetch VPN status", it)
        }
    }

    suspend fun getVpnServers(): Result<List<VpnServerProfile>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = executeJson(
                Request.Builder()
                    .url("$baseUrl/api/vpn/servers")
                    .get()
                    .build()
            )
            val status = json.optString("status", "not_configured")
            if (status != "available") {
                throw VpnConfigurationException(
                    code = json.optString("code", "VPN_NOT_CONFIGURED"),
                    message = json.optString("message", "VPN service is not configured")
                )
            }

            val servers = json.optJSONArray("servers") ?: return@runCatching emptyList()
            buildList {
                for (index in 0 until servers.length()) {
                    val item = servers.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    if (id.isBlank()) continue
                    add(
                        VpnServerProfile(
                            id = id,
                            name = item.optString("name", id),
                            countryCode = item.optString("countryCode", ""),
                            host = item.optString("host", "")
                        )
                    )
                }
            }
        }.onFailure {
            Log.e(TAG, "Failed to fetch VPN servers", it)
        }
    }

    suspend fun getVpnConfig(serverId: String, userId: String = "android-client"): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val keyPair = KeyPair()
                val payload = JSONObject().apply {
                    put("serverId", serverId)
                    put("userId", userId)
                    put("publicKey", keyPair.publicKey.toBase64())
                }

                val json = executeJson(
                    Request.Builder()
                        .url("$baseUrl/api/vpn/config")
                        .post(payload.toString().toRequestBody(JSON))
                        .build()
                )

                if (!json.optBoolean("success", false)) {
                    throw VpnConfigurationException(
                        code = json.optString("code", "VPN_CONFIG_ERROR"),
                        message = json.optString("message", "VPN configuration could not be issued")
                    )
                }

                val config = json.getJSONObject("config")
                val interfaceConfig = config.getJSONObject("interface")
                val peerConfig = config.getJSONObject("peer")

                """
                [Interface]
                Address = ${interfaceConfig.getString("address")}
                DNS = ${interfaceConfig.optString("dns", "1.1.1.1, 1.0.0.1")}
                PrivateKey = ${keyPair.privateKey.toBase64()}
                MTU = ${interfaceConfig.optInt("mtu", 1420)}

                [Peer]
                PublicKey = ${peerConfig.getString("publicKey")}
                Endpoint = ${peerConfig.getString("endpoint")}
                AllowedIPs = ${peerConfig.optString("allowedIPs", "0.0.0.0/0, ::/0")}
                PersistentKeepalive = ${peerConfig.optInt("persistentKeepalive", 25)}
                """.trimIndent()
            }.onFailure {
                Log.e(TAG, "Failed to fetch VPN config", it)
            }
        }

    private fun executeJson(request: Request): JSONObject {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val json = if (body.isBlank()) JSONObject() else JSONObject(body)

            if (!response.isSuccessful) {
                throw VpnConfigurationException(
                    code = json.optString("code", "HTTP_${response.code}"),
                    message = json.optString("message", json.optString("error", "VPN request failed"))
                )
            }
            return json
        }
    }

    private companion object {
        private const val TAG = "VpnProfileRepository"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
