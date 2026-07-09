package com.royalshield.app.services

import com.royalshield.app.models.PhishingDetectionResult
import com.royalshield.app.models.PhishingRiskLevel
import java.net.URI
import java.net.URL

class PhishingDetectionService {

    private val suspiciousTlds = setOf(
        ".zip", ".top", ".xyz", ".club", ".online", ".work", ".cam", ".click", ".vip"
    )

    private val urlShorteners = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "ow.ly", "buff.ly", "q.gs", "shorte.st"
    )

    private val highValueBrands = setOf(
        "paypal", "google", "apple", "microsoft", "amazon", "netflix", "facebook", "instagram"
    )

    /**
     * Extracts all URLs from the given text.
     */
    fun extractUrls(text: String): List<String> {
        val regex = "(https?://[\\w\\d.-]+\\.[\\w\\d]{2,}[/\\w\\d._?%&=-]*)".toRegex(RegexOption.IGNORE_CASE)
        return regex.findAll(text).map { it.value }.toList()
    }

    /**
     * Analyzes a text message for phishing indicators based on URLs.
     */
    fun analyzeText(text: String): PhishingDetectionResult {
        val urls = extractUrls(text)
        if (urls.isEmpty()) {
            return PhishingDetectionResult(
                riskLevel = PhishingRiskLevel.LOW,
                explanations = listOf("No URLs detected in the message.")
            )
        }

        var highestRisk = PhishingRiskLevel.LOW
        val allExplanations = mutableListOf<String>()

        for (urlStr in urls) {
            val result = analyzeUrl(urlStr)
            allExplanations.addAll(result.explanations)

            if (result.riskLevel > highestRisk) {
                highestRisk = result.riskLevel
            }
        }

        return PhishingDetectionResult(
            riskLevel = highestRisk,
            explanations = allExplanations.distinct()
        )
    }

    /**
     * Analyzes a single URL for phishing indicators.
     */
    fun analyzeUrl(urlString: String): PhishingDetectionResult {
        val explanations = mutableListOf<String>()
        var riskLevel = PhishingRiskLevel.LOW

        try {
            val uri = URI.create(urlString)
            val host = uri.host?.lowercase() ?: return PhishingDetectionResult(
                riskLevel = PhishingRiskLevel.LOW,
                explanations = listOf("Invalid or missing host in URL.")
            )

            // 1. Check for URL Shorteners
            if (urlShorteners.any { host == it || host.endsWith(".$it") }) {
                riskLevel = maxOf(riskLevel, PhishingRiskLevel.MEDIUM)
                explanations.add("Uses a URL shortener ($host), which is often used to hide the true destination.")
            }

            // 2. Check for suspicious TLDs
            if (suspiciousTlds.any { host.endsWith(it) }) {
                riskLevel = maxOf(riskLevel, PhishingRiskLevel.HIGH)
                explanations.add("Uses a suspicious Top-Level Domain commonly associated with spam or malware.")
            }

            // 3. Check for brand impersonation
            // Remove the TLD for checking against brand names, or check if brand is a substring but not the exact domain
            val domainParts = host.split(".")
            if (domainParts.size >= 2) {
                // e.g. "paypal-login.com" -> body is "paypal-login"
                val domainBody = host.substringBeforeLast(".")
                for (brand in highValueBrands) {
                    if (domainBody.contains(brand)) {
                        // Check if it's the official brand domain. E.g. "paypal.com" -> host == "paypal.com" or ends with ".paypal.com"
                        val isOfficial = host == "$brand.com" || host.endsWith(".$brand.com")
                        if (!isOfficial) {
                            riskLevel = maxOf(riskLevel, PhishingRiskLevel.HIGH)
                            explanations.add("Possible brand impersonation detected for '$brand'. Domain $host is not the official $brand domain.")
                        }
                    }
                }
            }

            // If no risks were found
            if (explanations.isEmpty()) {
                explanations.add("URL appears to be safe based on static analysis.")
            }

        } catch (e: Exception) {
            // Handle malformed URL
            riskLevel = PhishingRiskLevel.MEDIUM
            explanations.add("URL is malformed or intentionally obfuscated.")
        }

        return PhishingDetectionResult(riskLevel, explanations)
    }
}
