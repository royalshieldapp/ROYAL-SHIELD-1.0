package com.royalshield.app.managers

import com.royalshield.app.VirusTotalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class UrlSafety {
    SAFE, SUSPICIOUS, MALICIOUS
}

data class ScanResult(
    val url: String,
    val safety: UrlSafety,
    val threatType: String? = null
)

object QrSecurityManager {

    private val MALICIOUS_DOMAINS = listOf("malware.test", "phishing.site", "evilr.oot")
    private val SUSPICIOUS_KEYWORDS = listOf("win-iphone", "free-crypto", "verify-bank")

    private val virusTotalRepository = VirusTotalRepository()

    suspend fun analyzeUrl(url: String): ScanResult {
        // First try VirusTotal real analysis
        return try {
            val vtResult = withContext(Dispatchers.IO) {
                virusTotalRepository.checkUrl(url)
            }
            
            when (vtResult.verdict) {
                "malicious" -> ScanResult(url, UrlSafety.MALICIOUS, vtResult.reason)
                "suspicious" -> ScanResult(url, UrlSafety.SUSPICIOUS, vtResult.reason)
                else -> ScanResult(url, UrlSafety.SAFE, vtResult.reason)
            }
        } catch (e: Exception) {
            // API failed, fall back to local heuristics
            localAnalysis(url)
        }
    }

    private fun localAnalysis(url: String): ScanResult {
        val lowerUrl = url.lowercase()
        return when {
            MALICIOUS_DOMAINS.any { lowerUrl.contains(it) } -> {
                ScanResult(url, UrlSafety.MALICIOUS, "Known Malware Host")
            }
            SUSPICIOUS_KEYWORDS.any { lowerUrl.contains(it) } -> {
                ScanResult(url, UrlSafety.SUSPICIOUS, "Potential Phishing Keyword")
            }
            !lowerUrl.startsWith("http") -> {
                ScanResult(url, UrlSafety.SUSPICIOUS, "Non-Standard Protocol")
            }
            else -> {
                ScanResult(url, UrlSafety.SAFE)
            }
        }
    }
}
