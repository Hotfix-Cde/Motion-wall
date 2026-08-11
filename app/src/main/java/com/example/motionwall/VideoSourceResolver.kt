package com.example.motionwall

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import org.json.JSONArray
import org.json.JSONTokener
import java.util.Collections
import java.util.LinkedHashSet

/**
 * Resolves public social-video pages to the actual stream requested by the
 * page. A page URL is HTML, not a video source, so Media3/MediaPlayer cannot
 * play it directly. WebView runs the public page's JavaScript and observes
 * the MP4/HLS request that the page itself makes.
 */
object VideoSourceResolver {
    private const val LOG_TAG = "MotionWallResolver"
    private const val RESOLUTION_TIMEOUT_MS = 15_000L
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
            "Chrome/139.0.0.0 Mobile Safari/537.36"

    fun requiresPageResolution(source: Uri): Boolean =
        isInstagramPage(source) || isPinterestPage(source)

    fun isInstagramPage(source: Uri): Boolean {
        val host = source.host?.lowercase().orEmpty()
        return host == "instagram.com" || host.endsWith(".instagram.com")
    }

    fun isPinterestPage(source: Uri): Boolean {
        val host = source.host?.lowercase().orEmpty()
        return host == "pinterest.com" || host.endsWith(".pinterest.com") ||
            host == "pin.it"
    }

    /**
     * Returns a direct source on the main thread. Non-social URLs are passed
     * through unchanged; local document URIs are never sent to WebView.
     */
    fun resolveForPlayback(
        context: Context,
        source: Uri,
        callback: (Uri?) -> Unit
    ) {
        if (!requiresPageResolution(source)) {
            callback(source)
            return
        }
        Handler(Looper.getMainLooper()).post {
            BrowserResolution(context.applicationContext, source, callback).start()
        }
    }

    private class BrowserResolution(
        private val context: Context,
        private val source: Uri,
        private val callback: (Uri?) -> Unit
    ) {
        private val mainHandler = Handler(Looper.getMainLooper())
        private val candidates = Collections.synchronizedSet(LinkedHashSet<String>())
        private var webView: WebView? = null
        private var finished = false

        fun start() {
            val view = WebView(context)
            webView = view
            view.visibility = View.INVISIBLE
            view.settings.javaScriptEnabled = true
            view.settings.domStorageEnabled = true
            view.settings.mediaPlaybackRequiresUserGesture = false
            view.settings.userAgentString = USER_AGENT
            view.webViewClient = client

            mainHandler.postDelayed({ finish(bestCandidate()) }, RESOLUTION_TIMEOUT_MS)
            view.loadUrl(source.toString())
        }

        private val client = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                probePage(view, 0)
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                consider(request.url.toString())
                return super.shouldInterceptRequest(view, request)
            }
        }

        private fun probePage(view: WebView, attempt: Int) {
            if (finished) return
            view.evaluateJavascript(JAVASCRIPT_PROBE) { raw ->
                extractJavascriptUrls(raw).forEach(::consider)
                val best = bestCandidate()
                if (best != null) {
                    finish(best)
                } else if (attempt < 4) {
                    mainHandler.postDelayed(
                        { probePage(view, attempt + 1) },
                        PROBE_DELAYS_MS[attempt]
                    )
                }
            }
            // Trigger muted media loading where the site requires a play call.
            view.evaluateJavascript(JAVASCRIPT_PLAY, null)
        }

        private fun consider(raw: String) {
            val value = normalizeMediaUrl(raw) ?: return
            candidates.add(value)
            if (isStrongCandidate(value)) {
                mainHandler.post { finish(bestCandidate()) }
            }
        }

        private fun bestCandidate(): String? = synchronized(candidates) {
            candidates.maxByOrNull(::candidatePriority)
        }

        private fun finish(value: String?) {
            if (finished) return
            finished = true
            mainHandler.removeCallbacksAndMessages(null)
            val view = webView
            webView = null
            if (view != null) {
                view.stopLoading()
                view.webViewClient = WebViewClient()
                view.destroy()
            }
            val result = value?.let { Uri.parse(it) }
            Log.d(LOG_TAG, "Social page resolution result: ${result != null}")
            callback(result)
        }

        private fun extractJavascriptUrls(raw: String): List<String> {
            val value = runCatching { JSONTokener(raw).nextValue() }.getOrNull()
            return when (value) {
                is JSONArray -> buildList {
                    for (index in 0 until value.length()) {
                        value.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
                is String -> listOf(value)
                else -> Regex("https?://[^\\\"\\s]+")
                    .findAll(raw)
                    .map { it.value }
                    .toList()
            }
        }

        private fun normalizeMediaUrl(raw: String): String? {
            var value = raw.trim()
                .removePrefix("\\\"")
                .removeSuffix("\\\"")
                .replace("\\/", "/")
                .replace("&amp;", "&")
            val unicodeEscape = Regex("""\\u([0-9a-fA-F]{4})""")
            repeat(3) {
                value = unicodeEscape.replace(value) { match ->
                    match.groupValues[1].toInt(16).toChar().toString()
                }
            }
            if (!value.startsWith("http", ignoreCase = true)) return null
            if (!isMediaUrl(value)) return null
            return removeRangeQuery(value)
        }

        /**
         * Instagram requests media through many small byte ranges while the
         * page is playing. Those request parameters describe only a fragment;
         * they must not be passed to Media3 as the saved wallpaper source.
         */
        private fun removeRangeQuery(value: String): String {
            val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return value
            val rangeNames = setOf("bytestart", "byteend", "range", "start", "end")
            val builder = uri.buildUpon().clearQuery()
            for (name in uri.queryParameterNames) {
                if (name.lowercase() in rangeNames) continue
                uri.getQueryParameters(name).forEach { parameter ->
                    builder.appendQueryParameter(name, parameter)
                }
            }
            return builder.build().toString()
        }

        private fun isMediaUrl(value: String): Boolean {
            val lower = value.lowercase()
            return lower.contains(".mp4") || lower.contains(".m4v") ||
                lower.contains(".m3u8") || lower.contains("video_dashinit") ||
                lower.contains("/video/") || lower.contains("/videos/")
        }

        private fun isStrongCandidate(value: String): Boolean {
            val lower = value.lowercase()
            return lower.contains("video_dashinit") ||
                (lower.contains(".mp4") &&
                    (lower.contains("cdninstagram") || lower.contains("pinimg") ||
                        lower.contains("pinterest")))
        }

        private fun candidatePriority(value: String): Int {
            val lower = value.lowercase()
            return when {
                lower.contains("video_dashinit") -> 100
                lower.contains("xpv_progressive") -> 95
                lower.contains("cdninstagram") && lower.contains(".mp4") -> 90
                lower.contains("pinimg") && lower.contains(".mp4") -> 90
                lower.contains(".mp4") -> 70
                lower.contains(".m4v") -> 60
                lower.contains(".m3u8") -> 50
                else -> 0
            }
        }

        companion object {
            private val PROBE_DELAYS_MS = longArrayOf(500L, 1_000L, 2_000L, 4_000L)
            private const val JAVASCRIPT_PLAY = """
                (function() {
                    try {
                        document.querySelectorAll('video').forEach(function(video) {
                            video.muted = true;
                            var promise = video.play();
                            if (promise) promise.catch(function() {});
                        });
                    } catch (ignored) {}
                })();
            """
            private const val JAVASCRIPT_PROBE = """
                (function() {
                    var urls = [];
                    try {
                        document.querySelectorAll('video').forEach(function(video) {
                            if (video.currentSrc) urls.push(video.currentSrc);
                            if (video.src) urls.push(video.src);
                        });
                        document.querySelectorAll('source').forEach(function(source) {
                            if (source.src) urls.push(source.src);
                        });
                        if (window.performance && performance.getEntriesByType) {
                            performance.getEntriesByType('resource').forEach(function(entry) {
                                if (entry.name) urls.push(entry.name);
                            });
                        }
                        var pageText = document.documentElement
                            ? document.documentElement.innerHTML : '';
                        pageText = pageText.replace(/\\\\u0026/g, '&')
                            .replace(/\\\\\//g, '/');
                        var embedded = pageText.match(/https?:[^\"'<> ]+\\.(?:mp4|m4v|m3u8)[^\"'<> ]*/gi) || [];
                        embedded.forEach(function(url) { urls.push(url); });
                    } catch (ignored) {}
                    return urls.filter(function(url) {
                        return /^https?:/i.test(url) &&
                            (/\.mp4/i.test(url) || /\.m4v/i.test(url) ||
                             /\.m3u8/i.test(url) || /video_dashinit/i.test(url) ||
                             /\/video(s)?\//i.test(url));
                    });
                })();
            """
        }
    }
}
