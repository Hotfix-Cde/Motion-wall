package com.example.motionwall

import android.net.Uri
import android.text.TextUtils
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves a public Instagram Reel/post page to the video URL exposed by its
 * public HTML/embed metadata. Ordinary direct HTTP(S) video URLs are returned
 * unchanged. No login, cookies, scraping service, or download is used.
 */
object VideoSourceResolver {
    private const val LOG_TAG = "MotionWallResolver"
    private const val MAX_HTML_BYTES = 3_000_000
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    fun resolveForPlayback(source: Uri): Uri? {
        if (!isInstagramPage(source)) return source

        for (page in instagramCandidates(source)) {
            val html = fetchHtml(page) ?: continue
            extractVideoUrl(html)?.let { value ->
                normalizeVideoUrl(value, page)?.let { return it }
            }
        }
        Log.w(LOG_TAG, "No public video stream found for Instagram URL: $source")
        return null
    }

    fun isInstagramPage(source: Uri): Boolean {
        val host = source.host?.lowercase().orEmpty()
        return host == "instagram.com" || host.endsWith(".instagram.com")
    }

    private fun instagramCandidates(source: Uri): List<String> {
        val path = source.encodedPath?.trimEnd('/').orEmpty()
        val embed = if (path.isNotBlank()) {
            "https://www.instagram.com$path/embed/captioned/"
        } else {
            source.toString()
        }
        return listOf(embed, source.toString()).distinct()
    }

    private fun fetchHtml(address: String): String? {
        val connection = (URL(address).openConnection() as? HttpURLConnection)
            ?: return null
        return try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
                    "Chrome/120.0 Mobile Safari/537.36"
            )
            connection.setRequestProperty(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
            )
            if (connection.responseCode !in 200..399) return null

            connection.inputStream.bufferedReader().use { reader ->
                buildString {
                    var total = 0
                    while (total < MAX_HTML_BYTES) {
                        val line = reader.readLine() ?: break
                        append(line).append('\n')
                        total += line.length + 1
                    }
                }
            }
        } catch (error: Exception) {
            Log.w(LOG_TAG, "Could not read public video page", error)
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun extractVideoUrl(html: String): String? {
        // Public embeds commonly expose an Open Graph video URL.
        Regex("""<meta\s+[^>]*>""", RegexOption.IGNORE_CASE)
            .findAll(html)
            .forEach { match ->
                val tag = match.value
                val property = attribute(tag, "property")
                    ?: attribute(tag, "name")
                    ?: return@forEach
                if (property.lowercase() in setOf(
                        "og:video",
                        "og:video:url",
                        "og:video:secure_url",
                        "twitter:player:stream"
                    )
                ) {
                    attribute(tag, "content")?.let { return cleanEscapedUrl(it) }
                }
            }

        // Instagram currently exposes public progressive Reel sources in
        // video_versions[]. Prefer the Reel MP4, not a GIF or a comment asset.
        val mediaKeys = Regex(
            """[\"](?:video_url|playable_url|playable_url_quality_hd|url)[\"]\s*:\s*[\"]([^\"]+\.(?:mp4|m4v|m3u8)[^\"]*)[\"]""",
            RegexOption.IGNORE_CASE
        )
        val mediaCandidates = mediaKeys.findAll(html)
            .mapNotNull { match ->
                val value = cleanEscapedUrl(match.groupValues[1])
                if (value.startsWith("http")) value else null
            }
            .toList()
        if (mediaCandidates.isNotEmpty()) {
            return mediaCandidates.maxByOrNull { mediaPriority(it) }
        }

        // Last fallback for a standard HTML video tag.
        val videoTag = Regex(
            """<video\s+[^>]*src\s*=\s*[\"']([^\"']+)[\"']""",
            RegexOption.IGNORE_CASE
        ).find(html)
        return videoTag?.groupValues?.getOrNull(1)?.let(::cleanEscapedUrl)
    }

    private fun attribute(tag: String, name: String): String? {
        val pattern = Regex(
            """(?:^|\s)$name\s*=\s*[\"']([^\"']+)[\"']""",
            RegexOption.IGNORE_CASE
        )
        return pattern.find(tag)?.groupValues?.getOrNull(1)
    }

    private fun normalizeVideoUrl(value: String, page: String): Uri? {
        val cleaned = cleanEscapedUrl(value)
        if (TextUtils.isEmpty(cleaned)) return null
        val absolute = if (cleaned.startsWith("//")) "https:$cleaned" else cleaned
        val uri = runCatching { Uri.parse(absolute) }.getOrNull() ?: return null
        if (uri.scheme != "http" && uri.scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        Log.d(LOG_TAG, "Resolved public video from $page")
        return uri
    }

    private fun mediaPriority(value: String): Int {
        val lower = value.lowercase()
        return when {
            lower.contains("video_dashinit") -> 100
            lower.contains("xpv_progressive") -> 90
            lower.contains(".mp4") -> 60
            lower.contains(".m4v") -> 50
            lower.contains(".m3u8") -> 40
            else -> 0
        }
    }

    private fun cleanEscapedUrl(value: String): String {
        var result = value
            .replace("&amp;", "&")
            .replace("""\/""", "/")
            .trim()
        val unicodeEscape = Regex("""\\u([0-9a-fA-F]{4})""")
        repeat(3) {
            result = unicodeEscape.replace(result) { match ->
                match.groupValues[1].toInt(16).toChar().toString()
            }
        }
        return result.replace("\\\\/", "/")
    }
}
