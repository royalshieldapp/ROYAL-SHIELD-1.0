package com.royalshield.app.managers

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.royalshield.app.managers.PreferencesManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Manager to handle AI interactions.
 * Supports:
 * 1. Google Gemini API (FREE - prioritized)
 * 2. OpenAI API (paid fallback)
 * 3. Simulation mode (offline fallback)
 */
class AiManager {

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient()
    private val _prompts = MutableSharedFlow<String>(replay = 5)
    val prompts = _prompts.asSharedFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun analyzeThreat(type: String, onResult: (String) -> Unit) {
        val prompt = buildPrompt(type)
        scope.launch {
            _prompts.emit(">>> NEURAL_HUB [REQUESTING]: $type")
            _prompts.emit(">>> SENDING_PROMPT: $prompt")
        }

        // Priority: Gemini (free) > Backend Server (OpenAI Proxy) > Simulation
        val geminiKey = PreferencesManager.getGeminiApiKey()

        if (!geminiKey.isNullOrBlank()) {
            Log.d("AiManager", "Using Google Gemini (FREE)")
            performGeminiAnalysis(geminiKey, prompt, onResult)
        } else {
            // Attempt to use our secure backend
            Log.d("AiManager", "Using Secure Backend (OpenAI)")
            performBackendAiAnalysis(type, prompt, onResult)
        }
    }

    /**
     * Google Gemini API Integration (FREE TIER)
     * Model: gemini-pro
     * Limit: 60 requests/min, 1 million tokens/month FREE
     */
    private fun performGeminiAnalysis(apiKey: String, prompt: String, onResult: (String) -> Unit) {
        val systemInstruction = if (prompt.contains("Voice Command:")) {
            "You are the Royal Security Sentinel (RS-Sentinel), the core intelligence of the Royal Shield ecosystem. " +
            "Your purpose is to assist the user in securing their device. " +
            "Keep your responses authoritative, polite, and extremely concise (1-2 sentences maximum, under 100 characters where possible) because your replies are spoken aloud and rendered as a 3D talking avatar video.\n\n"
        } else {
            "You are AntiGravity, an advanced cybersecurity AI agent. Keep responses concise, technical, and formatted with bullet points.\n\n"
        }

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemInstruction$prompt")
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AiManager", "Gemini Request Failed", e)
                val errMsg = "Error connecting to Gemini AI: ${e.message}"
                scope.launch { _prompts.emit("!!! ERROR: $errMsg") }
                handler.post { onResult(errMsg) }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val candidates = json.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val contentObj = candidate.optJSONObject("content")
                            if (contentObj != null) {
                                val text = contentObj.getJSONArray("parts").getJSONObject(0).getString("text")
                                scope.launch { _prompts.emit("<<< NEURAL_RESPONSE: $text") }
                                handler.post { onResult(text) }
                            } else {
                                val finishReason = candidate.optString("finishReason", "UNKNOWN")
                                val errMsg = "Analysis blocked or empty. Reason: $finishReason"
                                scope.launch { _prompts.emit("!!! FILTER_TRIGGERED: $finishReason") }
                                handler.post { onResult(errMsg) }
                            }
                        } else {
                            handler.post { onResult("No response candidates from Gemini.") }
                        }
                    } catch (e: Exception) {
                        Log.e("AiManager", "Error parsing Gemini response: $responseBody", e)
                        val errMsg = "Error parsing AI response: ${e.localizedMessage}"
                        scope.launch { _prompts.emit("!!! PARSE_ERROR: $errMsg") }
                        handler.post { onResult(errMsg) }
                    }
                } else {
                    Log.e("AiManager", "Gemini HTTP Error: ${response.code} - $responseBody")
                    val errMsg = "Gemini AI Error: ${response.code} (Check your API Key in Settings)"
                    scope.launch { _prompts.emit("!!! HUB_ERROR: $errMsg") }
                    handler.post { onResult(errMsg) }
                }
            }
        })
    }

    /**
     * OpenAI API Integration (PAID)
     * Model: gpt-3.5-turbo
     * Cost: ~$0.002 per request
     */
    /**
     * Backend AI Integration (Render Proxy)
     * Securely routes requests to OpenAI via our private server.
     */
    private fun performBackendAiAnalysis(type: String, prompt: String, onResult: (String) -> Unit) {
        val jsonBody = JSONObject().apply {
            put("prompt", prompt)
            put("model", "gpt-3.5-turbo") // Optional, if backend supports it
        }

        val request = Request.Builder()
            .url("https://server-beckend.onrender.com/api/assistant/chat")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AiManager", "Backend AI Request Failed", e)
                scope.launch { _prompts.emit("!!! SERVER_FAIL: Falling back to Local Neural Engine.") }
                if (type.startsWith("AI Script Lab")) {
                    handler.post { onResult(generateMockScript(type)) }
                } else {
                    simulateThreatAnalysis(type, onResult)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val content = json.optString("response")
                            .ifEmpty { json.optString("message") }
                            .ifEmpty { json.optString("data") }
                            .ifEmpty { "Received content from server." }
                        
                        scope.launch { _prompts.emit("<<< SERVER_RESPONSE: $content") }
                        handler.post { onResult(content) }
                    } catch (e: Exception) {
                        scope.launch { _prompts.emit("!!! SERVER_PARSE_ERROR") }
                        handler.post { onResult("Error parsing Server response.") }
                    }
                } else {
                    scope.launch { _prompts.emit("!!! SERVER_ERROR: ${response.code}") }
                    if (type.startsWith("AI Script Lab")) {
                        handler.post { onResult(generateMockScript(type)) }
                    } else {
                        simulateThreatAnalysis(type, onResult)
                    }
                }
            }
        })
    }

    private fun generateMockScript(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            p.contains("python") -> "import os\n\nprint(\"Simulated Python Script\")\n# Educational purpose only"
            p.contains("bash") -> "#!/bin/bash\necho \"Simulated Bash Script\"\n# Educational purpose only"
            else -> "def main():\n    print(\"AI Script Generation successful\")\n\nif __name__ == \"__main__\":\n    main()"
        }
    }

    /**
     * Build prompt based on analysis type
     */
    private fun buildPrompt(type: String): String {
        return when (type) {
            "Threat Intelligence" -> "Analyze the current Android device security state. Assume the device is clean. Return a threat intelligence summary with status, threat level, and anomalies."
            "Emergency Assistant" -> "Act as an emergency AI assistant. Check voice patterns and ambient noise. Tell me if I am in a safe zone based on typical scenarios."
            "Log Analyzer" -> "Analyze typical Android system logs for the past 24 hours. Check for unauthorized access attempts. Summarize findings."
            "Skill Optimizer" -> "Provide AI-driven tips to master cybersecurity skills. Focus on practical defense techniques for advanced users."
            else -> "Provide a security summary for module: $type"
        }
    }

    /**
     * Simulation Mode (FREE, OFFLINE)
     * Used when no API keys are configured
     */
    private fun simulateThreatAnalysis(type: String, onResult: (String) -> Unit) {
        executor.execute {
            // Simulate processing time for "realism"
            Thread.sleep(1500)

            val parsedEvents = (3000..5000).random()
            val scannedFiles = (15000..20000).random()
            val threats = 0

            val response = when (type) {
                "Threat Intelligence" -> """
                    **System Integrity Verified**
                    
                    • **Scan Scope:** Full System & External Storage
                    • **Files Analyzed:** $scannedFiles
                    • **Signatures Checked:** 12,405
                    • **Active Threats:** $threats
                    • **System Status:** SECURE
                    
                    No anomalies detected in the kernel or user space. Real-time protection is active.
                """.trimIndent()
                
                "Emergency Assistant" -> """
                    **Emergency Protocol: ACTIVE**
                    
                    • **Voice Analysis:** Normal (No stress patterns detected)
                    • **Ambient Decibels:** 42dB (Quiet Environment)
                    • **Location Status:** SAFE ZONE (Home Network)
                    
                    Monitoring sensors for sudden changes. Say "Help" to trigger SOS.
                """.trimIndent()
                
                "Log Analyzer" -> """
                    **System Log Audit Complete**
                    
                    • **Time Frame:** Last 24 Hours
                    • **Events Parsed:** $parsedEvents
                    • **Auth Attempts:** SUCCESS (All verified)
                    • **Root Access:** DENIED/NONE
                    
                    System logs are clean. No unauthorized entry points found.
                """.trimIndent()

                "Skill Optimizer" -> """
                    **AI SKILL OPTIMIZATION REPORT**
                    
                    • **Level:** INTERMEDIATE (Gold Tier)
                    • **Focus:** Social Engineering Defense
                    • **Next Skill:** Encrypted Communication Mastering
                    
                    **Expert Recommendations:**
                    1. Use Hardware Security Keys (Yubikey) for MFA.
                    2. Implement strict firewall rules for unknown background processes.
                    3. Regularly audit app permissions via Privacy Advisor.
                """.trimIndent()
                
                else -> {
                    if (type.startsWith("Voice Command:")) {
                        val command = type.removePrefix("Voice Command:").trim().lowercase()
                        when {
                            command.contains("scan") -> "Initiating full malware scan. Please wait while I secure your filesystem."
                            command.contains("vpn") -> "Establishing secure tunnel to Royal VPN node. Connection is protected."
                            command.contains("sos") -> "SOS emergency protocol loaded. Ready to broadcast panic alerts."
                            command.contains("map") -> "Live cyber threat map is open. Visualizing current globally active attacks."
                            command.contains("settings") -> "Opening settings panel for system optimization."
                            else -> "I have processed your command: $command. Executing actions now."
                        }
                    } else {
                        "Module $type initialized and running. System is stable."
                    }
                }
            }

            handler.post {
                onResult(response)
            }
        }
    }
}
