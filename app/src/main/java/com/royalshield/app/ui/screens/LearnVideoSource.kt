package com.royalshield.app.ui.screens

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal sealed interface LearnVideoSource {
    val originalUrl: String

    data class YouTube(val videoId: String, override val originalUrl: String) : LearnVideoSource
    data class Direct(override val originalUrl: String) : LearnVideoSource
    data class External(override val originalUrl: String) : LearnVideoSource
}

internal fun parseLearnVideoSource(rawUrl: String?): LearnVideoSource? {
    val normalizedUrl = rawUrl?.trim().orEmpty()
    if (normalizedUrl.isEmpty()) return null

    val uri = runCatching { URI(normalizedUrl) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true)) return null

    val host = uri.host?.lowercase()?.trimEnd('.') ?: return null
    val youtubeHosts = setOf(
        "youtube.com", "www.youtube.com", "m.youtube.com",
        "youtube-nocookie.com", "www.youtube-nocookie.com"
    )

    val videoId = when {
        host == "youtu.be" || host == "www.youtu.be" -> uri.path?.trim('/')?.substringBefore('/')
        host in youtubeHosts && uri.path == "/watch" -> uri.rawQuery
            ?.split('&')
            ?.firstNotNullOfOrNull { parameter ->
                val parts = parameter.split('=', limit = 2)
                if (parts.firstOrNull() == "v") {
                    parts.getOrNull(1)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                } else null
            }
        host in youtubeHosts && uri.path?.startsWith("/embed/") == true ->
            uri.path.removePrefix("/embed/").substringBefore('/')
        host in youtubeHosts && uri.path?.startsWith("/shorts/") == true ->
            uri.path.removePrefix("/shorts/").substringBefore('/')
        else -> null
    }

    if (!videoId.isNullOrBlank() && videoId.matches(Regex("[A-Za-z0-9_-]{6,20}"))) {
        return LearnVideoSource.YouTube(videoId = videoId, originalUrl = normalizedUrl)
    }

    val path = uri.path.orEmpty().lowercase()
    return if (path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".m3u8")) {
        LearnVideoSource.Direct(normalizedUrl)
    } else {
        LearnVideoSource.External(normalizedUrl)
    }
}
