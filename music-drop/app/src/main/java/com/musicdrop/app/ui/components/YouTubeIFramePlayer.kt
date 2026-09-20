package com.musicdrop.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Embedded High-Definition YouTube Player using hardware-accelerated WebView.
 * Uses a high-resolution virtual stage (1280x720) so YouTube streams 1080p/720p HD.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeIFramePlayer(
    videoId: String,
    title: String? = null,
    channel: String? = null,
    resizeMode: Int = 1, // 0: Fit (16:9), 1: Fill (Zoom), 2: Wide
    isPlaying: Boolean = true,
    currentPositionMs: Long = 0L,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val cleanVideoId = remember(videoId) {
        val v = videoId.trim()
        when {
            v.contains("youtu.be/") -> v.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            v.contains("v=") -> v.substringAfter("v=").substringBefore("&").substringBefore("?")
            v.contains("shorts/") -> v.substringAfter("shorts/").substringBefore("?").substringBefore("&")
            v.contains("embed/") -> v.substringAfter("embed/").substringBefore("?").substringBefore("&")
            v.length in 8..15 -> v
            else -> "BddP6PYo2gs"
        }.trim()
    }

    val htmlData = remember(cleanVideoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                #wrapper {
                    position: absolute;
                    top: 0; left: 0; width: 100%; height: 100%;
                    display: flex; align-items: center; justify-content: center;
                    background: #000; overflow: hidden;
                }
                #stage {
                    position: absolute;
                    width: 1280px;
                    height: 720px;
                    top: 50%;
                    left: 50%;
                    transform-origin: center center;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                #player {
                    width: 1280px;
                    height: 720px;
                    border: none;
                    display: block;
                }
            </style>
        </head>
        <body>
            <div id="wrapper">
                <div id="stage">
                    <div id="player"></div>
                </div>
            </div>
            <script>
                // Continuous Ad Skipper & Fast-Forward Engine
                function autoSkipAds() {
                    try {
                        var skipBtns = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .videoAdUiSkipButton, .ytp-skip-ad-button, button.ytp-ad-skip-button, .ytp-ad-overlay-close-button');
                        for (var b = 0; b < skipBtns.length; b++) {
                            try { skipBtns[b].click(); } catch(e) {}
                        }
                        var v = document.querySelector('video');
                        var ad = document.querySelector('.ad-showing, .ad-interrupting, .ytp-ad-player-overlay');
                        if (ad && v) {
                            v.muted = true;
                            v.playbackRate = 16.0;
                            if (isFinite(v.duration) && v.duration > 0) {
                                v.currentTime = v.duration;
                            }
                        }
                    } catch(e) {}
                }
                setInterval(autoSkipAds, 200);

                var scaleMode = $resizeMode; // 0: Fit (16:9), 1: Fill (Zoom), 2: Wide
                function applyStageScale() {
                    var w = window.innerWidth || document.documentElement.clientWidth;
                    var h = window.innerHeight || document.documentElement.clientHeight;
                    if (!w || !h) return;
                    var scaleX = w / 1280;
                    var scaleY = h / 720;
                    var scale;
                    if (scaleMode === 1) {
                        scale = Math.max(scaleX, scaleY) * 1.05;
                    } else if (scaleMode === 2) {
                        scale = Math.max(scaleX, scaleY) * 0.92;
                    } else {
                        scale = Math.min(scaleX, scaleY);
                    }
                    var stage = document.getElementById('stage');
                    if (stage) {
                        stage.style.transform = 'translate(-50%, -50%) scale(' + scale + ')';
                    }
                }
                window.addEventListener('resize', applyStageScale);
                window.addEventListener('orientationchange', applyStageScale);
                applyStageScale();

                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                function enforceQuality() {
                    if (!player) return;
                    try {
                        if (typeof player.setPlaybackQualityRange === 'function') {
                            player.setPlaybackQualityRange('hd1080', 'highres');
                        }
                        if (typeof player.setPlaybackQuality === 'function') {
                            player.setPlaybackQuality('hd1080');
                        }
                    } catch(e) {}
                }

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        width: '1280',
                        height: '720',
                        videoId: '$cleanVideoId',
                        playerVars: {
                            'autoplay': 1,
                            'mute': 1,
                            'controls': 0,
                            'playsinline': 1,
                            'rel': 0,
                            'modestbranding': 1,
                            'enablejsapi': 1,
                            'fs': 0,
                            'iv_load_policy': 3,
                            'origin': 'https://www.youtube.com'
                        },
                        events: {
                            'onReady': function(event) {
                                try {
                                    enforceQuality();
                                    event.target.playVideo();
                                } catch(e) {}
                                var checks = 0;
                                var interval = setInterval(function() {
                                    enforceQuality();
                                    checks++;
                                    if (checks >= 6) clearInterval(interval);
                                }, 500);
                            },
                            'onStateChange': function(event) {
                                if (event.data === 1) { // PLAYING
                                    enforceQuality();
                                }
                            },
                            'onError': function(event) {
                                console.log('YouTube iframe error: ' + event.data);
                            }
                        }
                    });
                }

                document.addEventListener("visibilitychange", function() {
                    if (!document.hidden && player) {
                        try {
                            player.mute();
                            player.playVideo();
                        } catch(e) {}
                    }
                });

                function setResize(mode) {
                    scaleMode = mode;
                    applyStageScale();
                }

                function syncPlay(playing) {
                    if (!player) return;
                    try {
                        if (playing) {
                            player.mute();
                            player.playVideo();
                        } else {
                            player.pauseVideo();
                        }
                    } catch(e) {}
                }

                function syncSeek(sec) {
                    if (!player) return;
                    try { player.seekTo(sec, true); } catch(e) {}
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    webViewRef.value?.onResume()
                    val curSec = (currentPositionMs / 1000).toInt()
                    webViewRef.value?.evaluateJavascript(
                        "try { if (player) { player.mute(); player.seekTo($curSec, true); if ($isPlaying) player.playVideo(); } } catch(e) {}",
                        null
                    )
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    webViewRef.value?.onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .background(androidx.compose.ui.graphics.Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(Color.BLACK)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    webChromeClient = WebChromeClient()
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
                                url.contains("adservice.google.")
                            ) {
                                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    tag = cleanVideoId
                    webViewRef.value = this
                    loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webViewRef.value = webView
                if (webView.tag != cleanVideoId) {
                    webView.tag = cleanVideoId
                    webView.loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "UTF-8", null)
                } else {
                    val curSec = (currentPositionMs / 1000).toInt()
                    webView.evaluateJavascript(
                        "try { setResize($resizeMode); syncPlay($isPlaying); if (player && Math.abs((player.getCurrentTime() || 0) - $curSec) > 2.5) { syncSeek($curSec); } } catch(e) {}",
                        null
                    )
                }
            }
        )

        if (onClose != null) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close Player",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
