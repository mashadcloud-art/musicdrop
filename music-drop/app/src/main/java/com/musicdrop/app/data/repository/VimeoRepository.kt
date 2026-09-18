package com.musicdrop.app.data.repository

import com.musicdrop.app.data.youtube.YouTubeSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object VimeoRepository {

    private const val OEMBED_BASE = "https://vimeo.com/api/oembed.json"

    data class VimeoVideo(
        val id: String,
        val title: String,
        val description: String,
        val authorName: String,
        val thumbnailUrl: String,
        val duration: String,
        val viewCount: String
    ) {
        fun asSearchResult() = YouTubeSearchResult(
            videoId      = "vimeo:$id",
            title        = title,
            channelTitle = authorName,
            thumbnailUrl = thumbnailUrl,
            duration     = duration
        )

        fun embedUrl() = "https://player.vimeo.com/video/$id?autoplay=1&loop=0&byline=0&portrait=0"
    }

    suspend fun getOembed(vimeoUrl: String): VimeoVideo? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val encoded = URLEncoder.encode(vimeoUrl, "UTF-8")
            conn = (URL("$OEMBED_BASE?url=$encoded").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "FileDrop/2.6 (Android)")
            }
            if (conn.responseCode !in 200..299) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val videoId = extractVimeoId(vimeoUrl) ?: return@withContext null
            VimeoVideo(
                id           = videoId,
                title        = json.optString("title", "Untitled"),
                description  = "",
                authorName   = json.optString("author_name", ""),
                thumbnailUrl = json.optString("thumbnail_url", ""),
                duration     = formatSeconds(json.optInt("duration", 0)),
                viewCount    = ""
            )
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun extractVimeoId(url: String): String? {
        val regex = Regex("""vimeo\.com/(?:video/)?(\d+)""")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }

    private fun formatSeconds(secs: Int): String {
        if (secs <= 0) return ""
        val m = secs / 60
        val s = secs % 60
        return "$m:${s.toString().padStart(2, '0')}"
    }

    data class VimeoStream(
        val url: String,
        val mimeType: String,
        val quality: String,
        val isHls: Boolean = false
    )

    suspend fun getStreamUrl(vimeoId: String): VimeoStream? = withContext(Dispatchers.IO) {
        try {
            fetchText(
                "https://player.vimeo.com/video/$vimeoId/config",
                referer = "https://vimeo.com/$vimeoId"
            )?.let { body ->
                runCatching { JSONObject(body) }.getOrNull()
                    ?.let { parseConfigJson(it) }
                    ?.let { return@withContext it }
            }

            val html = fetchText("https://vimeo.com/$vimeoId") ?: return@withContext null
            val configUrl = Regex(""""config_url"\s*:\s*"([^"]+)"""")
                .find(html)
                ?.groupValues?.getOrNull(1)
                ?.replace("\\/", "/")
                ?.replace("&amp;", "&")
                ?: return@withContext null

            val configBody = fetchText(configUrl, referer = "https://vimeo.com/$vimeoId") ?: return@withContext null
            runCatching { JSONObject(configBody) }.getOrNull()?.let { parseConfigJson(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseConfigJson(config: JSONObject): VimeoStream? {
        val files = config.optJSONObject("request")?.optJSONObject("files") ?: return null

        val progressive = files.optJSONArray("progressive")
        if (progressive != null && progressive.length() > 0) {
            val options = (0 until progressive.length()).mapNotNull { i ->
                val f = progressive.optJSONObject(i) ?: return@mapNotNull null
                val url = f.optString("url").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val quality = f.optString("quality", "")
                Triple(url, quality, quality.removeSuffix("p").toIntOrNull() ?: 0)
            }
            val best = options.firstOrNull { it.third == 720 }
                ?: options.filter { it.third in 1..720 }.maxByOrNull { it.third }
                ?: options.maxByOrNull { it.third }
            if (best != null) {
                return VimeoStream(url = best.first, mimeType = "video/mp4", quality = best.second)
            }
        }

        files.optJSONObject("hls")?.let { hls ->
            val defaultCdn = hls.optString("default_cdn")
            val hlsUrl = hls.optJSONObject("cdns")?.optJSONObject(defaultCdn)?.optString("url")
            if (!hlsUrl.isNullOrBlank()) {
                return VimeoStream(url = hlsUrl, mimeType = "application/x-mpegURL", quality = "auto", isHls = true)
            }
        }

        return null
    }

    private fun fetchText(url: String, referer: String = "https://vimeo.com/"): String? {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 12_000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                )
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Referer", referer)
            }
            if (conn.responseCode !in 200..299) return null
            return conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return null
        } finally {
            conn?.disconnect()
        }
    }
}
