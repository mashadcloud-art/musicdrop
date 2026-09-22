package com.musicdrop.tv.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.musicdrop.tv.data.youtube.YouTubeExtractedFormat
import com.musicdrop.tv.data.youtube.YouTubeExtractionResult
import com.musicdrop.tv.ui.theme.DarkBg
import com.musicdrop.tv.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeInAppWebPane(
    viewModel: MainViewModel,
    initialQuery: String = "",
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("") }

    val defaultWebJs = remember {
        try {
            context.assets.open("local/default_web").bufferedReader().use { it.readText() }
        } catch (e: Exception) { "" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // 1. Top Blue Bar matching original app with "X" and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1565C0))
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = pageTitle.ifBlank { "YouTube" },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // 2. Main WebView Area with floating Download FAB
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
                                }
                            }
                        }

                        // Javascript Bridge for socialDownload
                        addJavascriptInterface(object : Any() {
                            @JavascriptInterface
                            fun send(tag: String, data: String) {
                                if (tag == "socialDownload") {
                                    try {
                                        val json = JSONObject(data)
                                        val videoId = json.optString("videoId")
                                        val title = json.optString("title")
                                        val author = json.optString("author", "YouTube")
                                        val filesArray = json.optJSONArray("files")

                                        val formatList = mutableListOf<YouTubeExtractedFormat>()
                                        if (filesArray != null) {
                                            for (i in 0 until filesArray.length()) {
                                                val fileObj = filesArray.getJSONObject(i)
                                                val url = fileObj.optString("url")
                                                val mime = fileObj.optString("mime", "")
                                                val label = fileObj.optString("qualityLabel", "")
                                                val bitrate = fileObj.optLong("bitrate", 0L)
                                                val len = fileObj.optLong("contentLength", 0L)

                                                if (url.isNotBlank()) {
                                                    formatList.add(
                                                        YouTubeExtractedFormat(
                                                            url = url,
                                                            mimeType = mime,
                                                            qualityLabel = label,
                                                            averageBitrate = bitrate,
                                                            contentLength = len
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        if (formatList.isNotEmpty()) {
                                            val thumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                                            val extractionResult = YouTubeExtractionResult(
                                                videoId = videoId,
                                                title = title,
                                                author = author,
                                                thumbnailUrl = thumb,
                                                formats = formatList
                                            )

                                            coroutineScope.launch {
                                                viewModel.setYtExtractionResult(extractionResult)
                                                pageTitle = title
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun postMessage(data: String) {
                                send("socialDownload", data)
                            }
                        }, "android")

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val url = request?.url?.toString().orEmpty()
                                if (url.contains("doubleclick.net") ||
                                    url.contains("googleads") ||
                                    url.contains("pagead2.googlesyndication.com") ||
                                    url.contains("/api/stats/ads") ||
                                    url.contains("/pagead/") ||
                                    url.contains("adservice.google.") ||
                                    url.contains("youtube.com/ptracking") ||
                                    url.contains("youtube.com/get_midroll_info") ||
                                    url.contains("googlesyndication.com")
                                ) {
                                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val adSkipJs = """
                                    (function() {
                                        if (window.__adSkipperInstalled) return;
                                        window.__adSkipperInstalled = true;
                                        setInterval(function() {
                                            try {
                                                var skipBtns = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .videoAdUiSkipButton, .ytp-skip-ad-button, button.ytp-ad-skip-button, .ytp-ad-overlay-close-button');
                                                skipBtns.forEach(function(b) { b.click(); });
                                                var ad = document.querySelector('.ad-showing, .ad-interrupting, .ytp-ad-player-overlay');
                                                var v = document.querySelector('video');
                                                if (ad && v) {
                                                    v.muted = true;
                                                    v.playbackRate = 16.0;
                                                    if (isFinite(v.duration) && v.duration > 0) {
                                                        v.currentTime = v.duration;
                                                    }
                                                }
                                            } catch(e) {}
                                        }, 200);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(adSkipJs, null)

                                if (defaultWebJs.isNotBlank()) {
                                    val proxy = """
                                        if (!window.WebViewJavascriptBridge) {
                                            window.WebViewJavascriptBridge = {
                                                send: function(tag, data) {
                                                    if (window.android) {
                                                        window.android.send(tag, typeof data === 'string' ? data : JSON.stringify(data));
                                                    }
                                                }
                                            };
                                        }
                                    """.trimIndent()
                                    view?.evaluateJavascript(proxy + "\n" + defaultWebJs, null)
                                }
                            }
                        }

                        val targetUrl = if (initialQuery.isNotBlank()) {
                            "https://m.youtube.com/results?search_query=" + Uri.encode(initialQuery)
                        } else {
                            "https://m.youtube.com"
                        }
                        loadUrl(targetUrl)
                        webViewRef = this
                    }
                },
                update = { webViewRef = it }
            )

            // Native Blue Download FAB (Matching original app)
            val extractionResult = viewModel.ytExtractionResult.collectAsState().value
            if (extractionResult != null) {
                FloatingActionButton(
                    onClick = { viewModel.openYtBottomSheet() },
                    containerColor = Color(0xFF1976D2),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 24.dp)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = "Download")
                }
            }
        }
    }

    // Render Download Bottom Sheet
    com.musicdrop.tv.ui.components.YouTubeDownloadBottomSheet(viewModel = viewModel)
}
