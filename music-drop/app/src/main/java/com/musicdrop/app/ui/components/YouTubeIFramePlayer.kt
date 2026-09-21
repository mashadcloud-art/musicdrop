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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import coil.compose.AsyncImage

class VideoStateBridge(private val onStarted: () -> Unit) {
    @android.webkit.JavascriptInterface
    fun onVideoStarted() {
        onStarted()
    }
}

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
    thumbnailUrl: String? = null,
    resizeMode: Int = 1, // 0: Fit (16:9), 1: Fill (Zoom), 2: Wide
    isPlaying: Boolean = true,
    currentPositionMs: Long = 0L,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    var isVideoStarted by remember(videoId) { mutableStateOf(false) }

    LaunchedEffect(videoId) {
        isVideoStarted = false
        kotlinx.coroutines.delay(4500L)
        isVideoStarted = true
    }

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

    val initialStartSec = remember(cleanVideoId) { (currentPositionMs / 1000).toInt() }

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
                        scale = Math.max(scaleX, scaleY) * 1.25;
                    } else if (scaleMode === 2) {
                        scale = Math.max(scaleX, scaleY) * 0.95;
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

                function loadNewVideo(newVid, startSec) {
                    if (player && typeof player.loadVideoById === 'function') {
                        try {
                            player.loadVideoById({
                                videoId: newVid,
                                startSeconds: startSec || 0,
                                suggestedQuality: 'hd1080'
                            });
                            player.mute();
                            player.playVideo();
                            return true;
                        } catch(e) {}
                    }
                    return false;
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
                            'start': $initialStartSec
                        },
                        events: {
                            'onReady': function(event) {
                                try {
                                    enforceQuality();
                                    event.target.mute();
                                    var initSec = $initialStartSec;
                                    if (initSec > 0.5) {
                                        event.target.seekTo(initSec, true);
                                    }
                                    if ($isPlaying) {
                                        event.target.playVideo();
                                    } else {
                                        event.target.pauseVideo();
                                    }
                                } catch(e) {}
                                var checks = 0;
                                var interval = setInterval(function() {
                                    enforceQuality();
                                    checks++;
                                    try {
                                        if (player && typeof player.getPlayerState === 'function' && player.getPlayerState() === 1) {
                                            if (window.AndroidApp && typeof window.AndroidApp.onVideoStarted === 'function') {
                                                window.AndroidApp.onVideoStarted();
                                            }
                                        }
                                    } catch(err) {}
                                    if (checks >= 6) clearInterval(interval);
                                }, 500);
                            },
                            'onStateChange': function(event) {
                                if (event.data === 1) { // PLAYING
                                    enforceQuality();
                                    try {
                                        if (player) player.mute();
                                        if (window.AndroidApp && typeof window.AndroidApp.onVideoStarted === 'function') {
                                            window.AndroidApp.onVideoStarted();
                                        }
                                    } catch(e) {}
                                }
                            },
                            'onError': function(event) {
                                console.log('YouTube iframe error: ' + event.data);
                                // Error 100, 101, 150: restricted embed -> fallback to nocookie embed
                                try {
                                    var p = document.getElementById('player');
                                    if (p) {
                                        p.innerHTML = '<iframe width="1280" height="720" src="https://www.youtube-nocookie.com/embed/' + '$cleanVideoId' + '?autoplay=1&mute=1&playsinline=1&controls=0&rel=0&enablejsapi=1&start=' + $initialStartSec + '" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen style="width:1280px;height:720px;border:none;" onload="try{if(window.AndroidApp)window.AndroidApp.onVideoStarted();}catch(e){}"></iframe>';
                                    }
                                } catch(err) {}
                            }
                        }
                    });
                }

                document.addEventListener("visibilitychange", function() {
                    if (!document.hidden && player) {
                        try {
                            player.mute();
                            if ($isPlaying) player.playVideo();
                        } catch(e) {}
                    }
                });

                function setResize(mode) {
                    scaleMode = mode;
                    applyStageScale();
                }

                // Tight audio-video synchronization engine
                function syncAudioVideo(targetSec, playing) {
                    if (!player || typeof player.getCurrentTime !== 'function') return;
                    try {
                        if (!playing) {
                            if (typeof player.getPlayerState === 'function' && player.getPlayerState() === 1) {
                                player.pauseVideo();
                            }
                            return;
                        }
                        if (typeof player.getPlayerState === 'function' && player.getPlayerState() !== 1 && player.getPlayerState() !== 3) {
                            player.mute();
                            player.playVideo();
                        }
                        var curSec = player.getCurrentTime() || 0;
                        var diff = targetSec - curSec;
                        // If drift is large (> 1.2s), hard seek to snap instantly
                        if (Math.abs(diff) > 1.2) {
                            player.seekTo(targetSec, true);
                            if (typeof player.setPlaybackRate === 'function') player.setPlaybackRate(1.0);
                        } else if (Math.abs(diff) > 0.12) {
                            // Dynamic micro-rate steering: eliminates buffering stalls while aligning lipsync
                            if (typeof player.setPlaybackRate === 'function') {
                                if (diff > 0.4) {
                                    player.setPlaybackRate(1.15);
                                } else if (diff > 0.12) {
                                    player.setPlaybackRate(1.06);
                                } else if (diff < -0.4) {
                                    player.setPlaybackRate(0.85);
                                } else if (diff < -0.12) {
                                    player.setPlaybackRate(0.94);
                                }
                            }
                        } else {
                            if (typeof player.setPlaybackRate === 'function') {
                                player.setPlaybackRate(1.0);
                            }
                        }
                    } catch(e) {}
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

    // Synchronize audio position continuously to keep video locked
    LaunchedEffect(currentPositionMs, isPlaying) {
        val wv = webViewRef.value ?: return@LaunchedEffect
        val curSec = currentPositionMs / 1000f
        wv.evaluateJavascript(
            "try { syncAudioVideo($curSec, $isPlaying); } catch(e) {}",
            null
        )
    }

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
                try {
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(Color.BLACK)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
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
                        addJavascriptInterface(VideoStateBridge {
                            post {
                                isVideoStarted = true
                            }
                        }, "AndroidApp")
                        loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlData, "text/html", "UTF-8", null)
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("YouTubeIFramePlayer", "Error creating WebView", t)
                    View(context).apply { setBackgroundColor(Color.BLACK) }
                }
            },
            update = { view ->
                val webView = view as? WebView ?: return@AndroidView
                webViewRef.value = webView
                try {
                    val curSec = currentPositionMs / 1000f
                    if (webView.tag != cleanVideoId) {
                        webView.tag = cleanVideoId
                        isVideoStarted = false
                        webView.evaluateJavascript(
                            "try { if (typeof loadNewVideo === 'function' && loadNewVideo('$cleanVideoId', ${(currentPositionMs / 1000).toInt()})) {} else { window.location.reload(); } } catch(e) { window.location.reload(); }",
                            null
                        )
                    } else {
                        webView.evaluateJavascript(
                            "try { setResize($resizeMode); syncAudioVideo($curSec, $isPlaying); } catch(e) {}",
                            null
                        )
                    }
                } catch (_: Throwable) {}
            },
            onRelease = { view ->
                val webView = view as? WebView ?: return@AndroidView
                try {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.onPause()
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView.destroy()
                } catch (_: Throwable) {}
                if (webViewRef.value === webView) {
                    webViewRef.value = null
                }
            }
        )

        // ── MUSICDROP BRANDED ICON LOADING OVERLAY (REPLACES YOUTUBE'S PLAY BUTTON) ──
        AnimatedVisibility(
            visible = !isVideoStarted,
            exit = fadeOut(animationSpec = tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Blurred artwork backdrop if thumbnail is provided
                if (!thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.32f)
                            .blur(radius = 24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // MusicDrop App Icon Image in elevated glowing container
                    Surface(
                        shape = CircleShape,
                        color = androidx.compose.ui.graphics.Color(0xFF161522),
                        border = BorderStroke(
                            2.dp,
                            Brush.linearGradient(
                                listOf(
                                    androidx.compose.ui.graphics.Color(0xFFF97316),
                                    androidx.compose.ui.graphics.Color(0xFFE11D48)
                                )
                            )
                        ),
                        shadowElevation = 24.dp,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = com.musicdrop.app.R.drawable.ic_app_logo),
                                contentDescription = "MusicDrop",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = androidx.compose.ui.graphics.Color(0xFFF97316)
                        )
                        Text(
                            text = "Loading HD Video...",
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

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
