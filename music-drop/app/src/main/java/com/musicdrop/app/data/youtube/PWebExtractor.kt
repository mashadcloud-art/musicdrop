package com.musicdrop.app.data.youtube

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.MediaType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Uses the bundled p_web JS (from the extracted original APK) loaded on local index.html.
 * p_web reads query params (?type=audio&method=all&v=VIDEO_ID) and resolves streams via InnerTube,
 * then calls WebViewJavascriptBridge.callHandler("socialDownload", {param: data}).
 */
@SuppressLint("SetJavaScriptEnabled")
class PWebExtractor(private val context: Context) {

    data class StreamInfo(
        val url: String,
        val mimeType: String,
        val title: String,
        val author: String,
        val thumbnailUrl: String,
        val durationMs: Long,
        val videoId: String,
        // When this googlevideo URL itself expires (parsed from its own "expire=" param
        // by StreamCacheStore) — 0 means unset/unknown, treated as already-stale so a
        // legacy call site that forgets to set this just never gets persisted, rather
        // than being cached forever on a guess.
        val expiresAtMs: Long = 0L
    ) {
        /** True while this URL is still (probably) good to hand to ExoPlayer. */
        fun isFresh(): Boolean = expiresAtMs > System.currentTimeMillis()

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

    private val mainHandler = Handler(Looper.getMainLooper())

    private val pWebJs: String by lazy {
        try {
            context.assets.open("local/p_web").bufferedReader().use { it.readText() }
                .removePrefix("javascript:")
        } catch (e: Exception) {
            android.util.Log.e("PWebExtractor", "Failed to load p_web: ${e.message}")
            ""
        }
    }

    suspend fun extract(
        videoId: String,
        knownTitle: String = "",
        knownAuthor: String = "",
        knownThumb: String = ""
    ): StreamInfo? = withTimeoutOrNull(25_000L) {
        suspendCancellableCoroutine { cont ->
            mainHandler.post {
                if (pWebJs.isBlank()) {
                    android.util.Log.e("PWebExtractor", "p_web not loaded")
                    if (cont.isActive) cont.resume(null)
                    return@post
                }

                val wv = buildWebView()

                // Bridge: Native receives calls from JS
                wv.addJavascriptInterface(object : Any() {
                    @JavascriptInterface
                    fun callHandler(handlerName: String, dataJson: String) {
                        android.util.Log.d("PWebExtractor", "Bridge handler: $handlerName (length=${dataJson.length})")
                        if (handlerName != "socialDownload") return
                        parseAndResume(dataJson, videoId, knownTitle, knownAuthor, knownThumb, cont, wv)
                    }

                    @JavascriptInterface
                    fun callHandlerWithParam(handlerName: String, paramJson: String) {
                        if (handlerName != "socialDownload") return
                        try {
                            val wrapper = JSONObject(paramJson)
                            val inner = wrapper.optString("param", paramJson)
                            parseAndResume(inner, videoId, knownTitle, knownAuthor, knownThumb, cont, wv)
                        } catch (e: Exception) {
                            parseAndResume(paramJson, videoId, knownTitle, knownAuthor, knownThumb, cont, wv)
                        }
                    }
                }, "NativeBridge")

                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        android.util.Log.d("PWebExtractor", "Page loaded: $url - injecting bridge and p_web")
                        val bridge = """
                            window.WebViewJavascriptBridge = {
                                handlers: {},
                                callHandler: function(name, data, callback) {
                                    try {
                                        var dataStr = (typeof data === 'string') ? data : JSON.stringify(data);
                                        NativeBridge.callHandler(name, dataStr);
                                    } catch(e) { console.error('Bridge error: ' + e); }
                                },
                                registerHandler: function(name, cb) {
                                    this.handlers[name] = cb;
                                },
                                send: function(name, data) {
                                    try {
                                        var dataStr = (typeof data === 'string') ? data : JSON.stringify(data);
                                        NativeBridge.callHandler(name, dataStr);
                                    } catch(e) {}
                                }
                            };
                            var evt = document.createEvent("Events");
                            evt.initEvent("WebViewJavascriptBridgeReady", false, false);
                            document.dispatchEvent(evt);
                        """.trimIndent()

                        view?.evaluateJavascript("(function(){ $bridge\n$pWebJs\n})();", null)

                        // Trigger start immediately
                        view?.evaluateJavascript("""
                            (function() {
                                if (window.WebViewJavascriptBridge && window.WebViewJavascriptBridge.handlers['bridgeReady']) {
                                    try {
                                        window.WebViewJavascriptBridge.handlers['bridgeReady'](JSON.stringify({type: 1, ver: 76, lang: 'en'}));
                                    } catch(e) {}
                                }
                            })();
                        """.trimIndent(), null)
                    }
                }

                val targetUrl = "file:///android_asset/index.html?type=audio&method=all&v=$videoId"
                android.util.Log.d("PWebExtractor", "Loading asset url: $targetUrl")
                wv.loadUrl(targetUrl)

                cont.invokeOnCancellation { mainHandler.post { wv.destroy() } }
            }
        }
    }

    private fun parseAndResume(
        dataJson: String,
        videoId: String,
        knownTitle: String,
        knownAuthor: String,
        knownThumb: String,
        cont: kotlinx.coroutines.CancellableContinuation<StreamInfo?>,
        wv: WebView
    ) {
        try {
            val data = JSONObject(dataJson)

            // Unwrap {param: {...}} if present
            val root = if (data.has("param")) {
                val p = data.opt("param")
                if (p is JSONObject) p else JSONObject(p.toString())
            } else data

            val files = root.optJSONArray("files")
            if (files == null || files.length() == 0) {
                android.util.Log.w("PWebExtractor", "No files in socialDownload for $videoId: $dataJson")
                if (cont.isActive) cont.resume(null)
                return
            }

            var bestUrl     = ""
            var bestMime    = "audio/mp4"
            var bestBitrate = 0L

            for (i in 0 until files.length()) {
                val f = files.optJSONObject(i) ?: continue
                val url     = f.optString("url", "")
                val mime    = f.optString("mimeType", f.optString("mime", ""))
                val bitrate = f.optLong("averageBitrate", f.optLong("bitrate", 0L))
                val hasVideo = f.optBoolean("hasVideo", !mime.startsWith("audio/"))

                if (url.isBlank() || !url.startsWith("http")) continue

                // Prefer audio-only streams
                if (!hasVideo || mime.startsWith("audio/")) {
                    if (bitrate > bestBitrate || bestUrl.isBlank()) {
                        bestBitrate = bitrate
                        bestUrl     = url
                        bestMime    = mime.ifBlank { "audio/mp4" }
                    }
                } else if (bestUrl.isBlank()) {
                    bestUrl  = url
                    bestMime = mime.ifBlank { "audio/mp4" }
                }
            }

            if (bestUrl.isBlank()) {
                android.util.Log.w("PWebExtractor", "No playable URL in files for $videoId")
                if (cont.isActive) cont.resume(null)
                return
            }

            val title  = root.optString("title", "").ifBlank { knownTitle.ifBlank { "YouTube Audio" } }
            val author = root.optString("author", "").ifBlank { knownAuthor.ifBlank { "YouTube" } }
            val thumb  = knownThumb.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }
            val durMs  = root.optLong("durationMs", 0L)

            android.util.Log.i("PWebExtractor", "✅ p_web success: $videoId ($bestMime, bitrate=$bestBitrate)")
            val info = StreamInfo(
                bestUrl, bestMime, title, author, thumb, durMs, videoId,
                expiresAtMs = StreamCacheStore.expiryFromUrl(bestUrl)
            )
            if (cont.isActive) cont.resume(info)
            mainHandler.postDelayed({ wv.destroy() }, 500)
        } catch (e: Exception) {
            android.util.Log.e("PWebExtractor", "Parse error: ${e.message}")
            if (cont.isActive) cont.resume(null)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView(): WebView = WebView(context).apply {
        settings.apply {
            javaScriptEnabled                = true
            domStorageEnabled                = true
            databaseEnabled                  = true
            allowFileAccess                  = true
            allowFileAccessFromFileURLs      = true
            allowUniversalAccessFromFileURLs = true
            blockNetworkImage                = true
            loadsImagesAutomatically         = false
            cacheMode                        = WebSettings.LOAD_DEFAULT
            mixedContentMode                 = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString                  = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }
        layoutParams = android.view.ViewGroup.LayoutParams(1, 1)
    }

    companion object {
        @Volatile private var instance: PWebExtractor? = null
        fun getInstance(context: Context): PWebExtractor =
            instance ?: synchronized(this) {
                instance ?: PWebExtractor(context.applicationContext).also { instance = it }
            }
    }
}
