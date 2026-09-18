package com.musicdrop.app.data.youtube

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

/**
 * YouTube audio stream extractor.
 * Tries ANDROID_TESTSUITE first (no n-param throttling), then TV_EMBEDDED,
 * then legacy mobile clients as fallback.
 */
class YouTubeStreamExtractor(private val context: Context) {

    data class StreamResult(
        val url: String,
        val mimeType: String,
        val title: String,
        val author: String,
        val durationMs: Long,
        val thumbnailUrl: String,
        val videoId: String
    ) {
        fun toMediaItem(): MediaItem = MediaItem(
            id          = videoId.hashCode().toLong(),
            uri         = Uri.parse(url),
            name        = title.ifBlank { "YouTube Audio" },
            size        = 0L,
            dateAdded   = System.currentTimeMillis() / 1000,
            mimeType    = mimeType.substringBefore(";").trim().ifBlank { "audio/mp4" },
            mediaType   = MediaType.AUDIO,
            durationMs  = durationMs,
            artist      = author,
            album       = "YouTube Music",
            isSong      = true,
            filePath    = url,
            bucketName  = "YouTube Stream",
            albumArtUri = thumbnailUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        )
    }

    private data class ClientProfile(
        val clientName: String,
        val clientVersion: String,
        val userAgent: String,
        val apiKey: String = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8",
        val androidSdkVersion: Int? = null,
        val embedUrl: String? = null
    )

    private val clients = listOf(
        // Best: plain direct URLs, no n-param throttling
        ClientProfile(
            clientName         = "ANDROID",
            clientVersion      = "19.09.37",
            userAgent          = "com.google.android.youtube/19.09.37 (Linux; U; Android 11) gzip",
            androidSdkVersion  = 30
        ),
        // TV embed: also plain direct URLs
        ClientProfile(
            clientName    = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            userAgent     = "Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/6.0 TV Safari/538.1",
            embedUrl      = "https://www.youtube.com/"
        ),
        // iOS: mostly plain URLs
        ClientProfile(
            clientName    = "IOS",
            clientVersion = "21.02.3",
            userAgent     = "com.google.ios.youtube/21.02.3 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)"
        ),
        // Android VR fallback
        ClientProfile(
            clientName        = "ANDROID_VR",
            clientVersion     = "1.65.10",
            userAgent         = "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            androidSdkVersion = 32
        ),
        // Kids fallback
        ClientProfile(
            clientName        = "ANDROID_KIDS",
            clientVersion     = "7.36.1",
            userAgent         = "com.google.android.apps.youtube.kids/7.36.1 (Linux; U; Android 13) gzip",
            androidSdkVersion = 33
        )
    )

    private class ProtobufWriter {
        private val buf = mutableListOf<Byte>()

        private fun writeVarint(value: Long) {
            var v = value
            if (v == 0L) { buf.add(0); return }
            while (v > 0L) {
                var b = (v and 0x7F).toInt()
                v = v ushr 7
                if (v > 0L) b = b or 0x80
                buf.add(b.toByte())
            }
        }

        private fun field(n: Int, w: Int) = writeVarint(((n shl 3) or (w and 7)).toLong())
        fun varint(n: Int, v: Long)        { field(n, 0); writeVarint(v) }
        fun bytesField(n: Int, d: ByteArray) { field(n, 2); writeVarint(d.size.toLong()); d.forEach { buf.add(it) } }
        fun stringField(n: Int, s: String)   = bytesField(n, s.toByteArray(Charsets.UTF_8))
        fun toByteArray(): ByteArray = buf.toByteArray()
        fun toBase64(): String = Base64.encodeToString(toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
    }

    private fun randomVisitorData(): String {
        val rnd = Random()
        val e = ProtobufWriter().apply {
            stringField(2, "")
            varint(4, (rnd.nextInt(255) + 1).toLong())
        }
        val t = ProtobufWriter().apply {
            stringField(1, "US")
            bytesField(2, e.toByteArray())
        }
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val rndStr = (1..11).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
        return ProtobufWriter().apply {
            stringField(1, rndStr)
            varint(5, (System.currentTimeMillis() / 1000) - rnd.nextInt(600_000))
            bytesField(6, t.toByteArray())
        }.toBase64()
    }

    suspend fun extract(
        videoId: String,
        knownTitle: String = "",
        knownAuthor: String = "",
        knownThumb: String = "",
        poToken: PoTokenManager.PoToken? = null
    ): StreamResult? {
        // Outer loop is NOT inside withContext — avoids continue-in-inline-lambda error
        for (profile in clients) {
            val result = tryClient(profile, videoId, knownTitle, knownAuthor, knownThumb, poToken)
            if (result != null) return result
        }
        android.util.Log.e("YTExtractor", "❌ All clients failed for $videoId${if (poToken == null) " (no PoToken attached)" else ""}")
        return null
    }

    /** Tries a single client profile. Returns null on any failure (never throws). */
    private suspend fun tryClient(
        profile: ClientProfile,
        videoId: String,
        knownTitle: String,
        knownAuthor: String,
        knownThumb: String,
        poToken: PoTokenManager.PoToken?
    ): StreamResult? = withContext(Dispatchers.IO) {
        try {
            // A PoToken is bound to the visitorData it was generated against — reusing a
            // fresh random visitorData here would make the token meaningless, so when we
            // have one, its visitorData MUST travel with it on the player request.
            val visitorData = poToken?.visitorData?.takeIf { it.isNotBlank() } ?: randomVisitorData()

            val clientObj = JSONObject().apply {
                put("hl", "en")
                put("gl", "US")
                put("clientName", profile.clientName)
                put("clientVersion", profile.clientVersion)
                put("visitorData", visitorData)
                profile.androidSdkVersion?.let { put("androidSdkVersion", it.toString()) }
            }

            val contextObj = JSONObject().apply {
                put("client", clientObj)
                profile.embedUrl?.let { url ->
                    put("thirdParty", JSONObject().put("embedUrl", url))
                }
            }

            val body = JSONObject().apply {
                put("videoId", videoId)
                put("context", contextObj)
                put("contentCheckOk", true)
                put("racyCheckOk", true)
                poToken?.token?.takeIf { it.isNotBlank() }?.let { token ->
                    put("serviceIntegrityDimensions", JSONObject().put("poToken", token))
                }
            }

            val endpoint = "https://www.youtube.com/youtubei/v1/player?key=${profile.apiKey}&prettyPrint=false"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout   = 12_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("User-Agent", profile.userAgent)
                setRequestProperty("X-Youtube-Client-Name", ytClientId(profile.clientName).toString())
                setRequestProperty("X-Youtube-Client-Version", profile.clientVersion)
            }

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val potTag = if (poToken != null) "pot=yes" else "pot=no"

            val code = conn.responseCode
            if (code !in 200..299) {
                android.util.Log.w("YTExtractor", "[${profile.clientName}] ($potTag) HTTP $code")
                conn.disconnect()
                return@withContext null
            }

            val respJson = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val obj = JSONObject(respJson)

            val status = obj.optJSONObject("playabilityStatus")?.optString("status")
            if (status != "OK") {
                android.util.Log.w("YTExtractor", "[${profile.clientName}] ($potTag) status=$status for $videoId")
                return@withContext null
            }

            val streamingData = obj.optJSONObject("streamingData") ?: run {
                android.util.Log.w("YTExtractor", "[${profile.clientName}] ($potTag) No streamingData")
                return@withContext null
            }

            val candidates = mutableListOf<JSONObject>()
            streamingData.optJSONArray("adaptiveFormats")?.let { arr ->
                for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { candidates.add(it) }
            }
            streamingData.optJSONArray("formats")?.let { arr ->
                for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { candidates.add(it) }
            }

            // Only accept formats with a direct, unencrypted URL
            val audioOnly = candidates.filter { fmt ->
                val mime   = fmt.optString("mimeType", "")
                val url    = fmt.optString("url", "")
                !fmt.has("signatureCipher") && !fmt.has("cipher") &&
                    url.startsWith("http") && mime.startsWith("audio/")
            }.sortedByDescending { it.optLong("bitrate", 0L) }

            val best = audioOnly.firstOrNull { it.optString("mimeType").contains("mp4") }
                ?: audioOnly.firstOrNull()

            if (best == null) {
                android.util.Log.w("YTExtractor", "[${profile.clientName}] ($potTag) No plain audio URL (${candidates.size} formats, all ciphered?)")
                return@withContext null
            }

            val directUrlRaw = best.getString("url")
            // The streaming poToken (bound to the request's visitorData) must travel on the
            // googlevideo URL itself as the pot parameter, otherwise gated streams are throttled.
            val directUrl = poToken?.token?.takeIf { it.isNotBlank() }?.let { pot ->
                directUrlRaw + (if (directUrlRaw.contains('?')) "&" else "?") +
                    "pot=" + android.net.Uri.encode(pot)
            } ?: directUrlRaw
            val mime       = best.optString("mimeType", "audio/mp4")
            val durationMs = best.optString("approxDurationMs", "").toLongOrNull()
                ?: (obj.optJSONObject("videoDetails")?.optString("lengthSeconds")?.toLongOrNull()?.times(1000L) ?: 0L)

            val details = obj.optJSONObject("videoDetails")
            val title   = details?.optString("title")?.ifBlank { null }  ?: knownTitle.ifBlank  { "YouTube Audio" }
            val author  = details?.optString("author")?.ifBlank { null } ?: knownAuthor.ifBlank { "YouTube" }
            val thumb   = knownThumb.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }

            android.util.Log.i("YTExtractor", "✅ [${profile.clientName}] ($potTag) itag=${best.optInt("itag")} mime=$mime")
            StreamResult(
                url          = directUrl,
                mimeType     = mime,
                title        = title,
                author       = author,
                durationMs   = durationMs,
                thumbnailUrl = thumb,
                videoId      = videoId
            )
        } catch (e: Exception) {
            android.util.Log.e("YTExtractor", "[${profile.clientName}] ${e.message}")
            null
        }
    }

    private fun ytClientId(name: String): Int = when (name) {
        "WEB"                            -> 1
        "ANDROID"                        -> 3
        "IOS"                            -> 5
        "TVHTML5_SIMPLY_EMBEDDED_PLAYER" -> 85
        "ANDROID_TESTSUITE"              -> 30
        "ANDROID_VR"                     -> 28
        "ANDROID_KIDS"                   -> 26
        else                             -> 3
    }

    companion object {
        @Volatile private var instance: YouTubeStreamExtractor? = null
        fun getInstance(context: Context): YouTubeStreamExtractor =
            instance ?: synchronized(this) {
                instance ?: YouTubeStreamExtractor(context.applicationContext).also { instance = it }
            }
    }
}
