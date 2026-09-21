package com.musicdrop.app.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * Full-Screen YouTube Shorts & Clips Player:
 * - Native VerticalPager: Smooth vertical swipe between clips (TikTok / YouTube Shorts style)
 * - Unlimited Infinite Scroll: Triggers viewModel.loadMoreSamples() as user approaches end of feed
 * - High-Compatibility Embed: Uses youtube-nocookie.com without broken origin mismatches
 * - Active Page Isolation: Only loads WebView on active page to maintain 60fps scrolling
 * - Two-way JS Bridge: Play/Pause synchronization, touch-to-toggle, and restricted video fallback
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ShortsFullScreenPlayer(
    shortsList: List<YouTubeSearchResult>,
    initialIndex: Int,
    viewModel: MainViewModel? = null,
    onClose: () -> Unit,
    onPlayFullSong: (YouTubeSearchResult) -> Unit
) {
    if (shortsList.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (shortsList.size - 1).coerceAtLeast(0))
    ) { shortsList.size }
    val scope = rememberCoroutineScope()

    // Unlimited Infinite Scroll: Auto-load more clips when 3 items from the end
    LaunchedEffect(pagerState.currentPage, shortsList.size) {
        if (pagerState.currentPage >= shortsList.size - 3) {
            viewModel?.loadMoreSamples()
        }
    }

    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        window?.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        onDispose {}
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val currentShort = shortsList[page]
                val isActive = pagerState.currentPage == page
                SingleShortPage(
                    currentShort = currentShort,
                    isActive = isActive,
                    pageIndex = page,
                    totalCount = shortsList.size,
                    onClose = onClose,
                    onPlayFullSong = onPlayFullSong,
                    onPrevious = {
                        if (page > 0) {
                            scope.launch { pagerState.animateScrollToPage(page - 1) }
                        }
                    },
                    onNext = {
                        if (page < shortsList.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(page + 1) }
                        }
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SingleShortPage(
    currentShort: YouTubeSearchResult,
    isActive: Boolean,
    pageIndex: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onPlayFullSong: (YouTubeSearchResult) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var isLiked by remember(currentShort.videoId) { mutableStateOf(false) }
    var isVideoPlaying by remember(currentShort.videoId) { mutableStateOf(true) }
    var hasEmbedError by remember(currentShort.videoId) { mutableStateOf(false) }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }

    fun togglePlayPause() {
        activeWebView?.evaluateJavascript("try { postYt('togglePlay'); } catch(e){}", null)
    }

    // Keep active state synchronized with pager
    LaunchedEffect(isActive) {
        if (!isActive) {
            activeWebView?.evaluateJavascript("try { postYt('pauseVideo'); } catch(e){}", null)
        } else {
            activeWebView?.evaluateJavascript("try { postYt('playVideo'); } catch(e){}", null)
        }
    }

    DisposableEffect(currentShort.videoId) {
        onDispose {
            activeWebView?.evaluateJavascript("try { postYt('pauseVideo'); } catch(e){}", null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Background Thumbnail / Card Artwork (Never a black screen!) ──
        AsyncImage(
            model = currentShort.thumbnailUrl,
            contentDescription = currentShort.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ── Embedded YouTube Video Player (Only rendered on active page) ──
        if (isActive) {
            key(currentShort.videoId) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                            setBackgroundColor(0x00000000)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                allowContentAccess = true
                                allowFileAccess = true
                                loadsImagesAutomatically = true
                                mediaPlaybackRequiresUserGesture = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                            }
                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()
                            tag = currentShort.videoId
                            activeWebView = this
                            addJavascriptInterface(object {
                                @android.webkit.JavascriptInterface
                                fun onPlayerState(playing: Boolean) {
                                    isVideoPlaying = playing
                                }
                                @android.webkit.JavascriptInterface
                                fun onError(errorCode: Int) {
                                    hasEmbedError = true
                                }
                            }, "AndroidBridge")

                            val embedHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        * { margin: 0; padding: 0; box-sizing: border-box; }
                                        html, body { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                                        #player-wrapper { position: relative; width: 100%; height: 100%; overflow: hidden; }
                                        iframe { position: absolute; top: 50%; left: 50%; width: 100%; height: 100%; min-width: 100%; min-height: 100%; border: none; transform: translate(-50%, -50%) scale(1.36); transform-origin: center center; }
                                        #touch-catcher { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 5; }
                                    </style>
                                </head>
                                <body>
                                    <div id="player-wrapper">
                                        <iframe 
                                            id="ytplayer"
                                            src="https://www.youtube-nocookie.com/embed/${currentShort.videoId}?autoplay=1&mute=0&playsinline=1&controls=0&loop=1&playlist=${currentShort.videoId}&enablejsapi=1&rel=0&modestbranding=1&iv_load_policy=3&fs=0"
                                            frameborder="0"
                                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                            allowfullscreen>
                                        </iframe>
                                        <div id="touch-catcher" onclick="togglePlay()"></div>
                                    </div>
                                    <script>
                                        var tag = document.createElement('script');
                                        tag.src = "https://www.youtube.com/iframe_api";
                                        var firstScriptTag = document.getElementsByTagName('script')[0];
                                        firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                                        var player;
                                        function onYouTubeIframeAPIReady() {
                                            player = new YT.Player('ytplayer', {
                                                events: {
                                                    'onReady': function(event) {
                                                        try {
                                                            event.target.unMute();
                                                            event.target.setVolume(100);
                                                            event.target.playVideo();
                                                        } catch(e) {}
                                                    },
                                                    'onStateChange': function(event) {
                                                        try {
                                                            if (event.data === 1) {
                                                                if (window.AndroidBridge) window.AndroidBridge.onPlayerState(true);
                                                                setTimeout(function() {
                                                                    try {
                                                                        event.target.unMute();
                                                                        event.target.setVolume(100);
                                                                    } catch(e) {}
                                                                }, 200);
                                                            } else if (event.data === 2) {
                                                                if (window.AndroidBridge) window.AndroidBridge.onPlayerState(false);
                                                            }
                                                        } catch(e) {}
                                                    },
                                                    'onError': function(event) {
                                                        if (window.AndroidBridge) window.AndroidBridge.onError(event.data);
                                                    }
                                                }
                                            });
                                        }
                                        function togglePlay() {
                                            if (!player) {
                                                postMsg('playVideo');
                                                return;
                                            }
                                            try {
                                                var state = player.getPlayerState();
                                                if (state === 1) {
                                                    player.pauseVideo();
                                                    if (window.AndroidBridge) window.AndroidBridge.onPlayerState(false);
                                                } else {
                                                    player.unMute();
                                                    player.setVolume(100);
                                                    player.playVideo();
                                                    if (window.AndroidBridge) window.AndroidBridge.onPlayerState(true);
                                                }
                                            } catch(e) {
                                                postMsg('playVideo');
                                            }
                                        }
                                        function postYt(func) {
                                            if (player) {
                                                try {
                                                    if (func === 'playVideo') {
                                                        player.unMute();
                                                        player.setVolume(100);
                                                        player.playVideo();
                                                        if (window.AndroidBridge) window.AndroidBridge.onPlayerState(true);
                                                    } else if (func === 'pauseVideo') {
                                                        player.pauseVideo();
                                                        if (window.AndroidBridge) window.AndroidBridge.onPlayerState(false);
                                                    } else if (func === 'togglePlay') {
                                                        togglePlay();
                                                    }
                                                } catch(e) { postMsg(func); }
                                            } else {
                                                postMsg(func);
                                            }
                                        }
                                        function postMsg(func) {
                                            var el = document.getElementById('ytplayer');
                                            if (el && el.contentWindow) {
                                                el.contentWindow.postMessage(JSON.stringify({
                                                    event: 'command',
                                                    func: func,
                                                    args: ''
                                                }), '*');
                                            }
                                        }
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "utf-8", null)
                        }
                    },
                    update = { webView ->
                        activeWebView = webView
                    }
                )
            }
        }

        // ── Restricted Video Fallback Overlay (Never leaves user stranded!) ──
        if (hasEmbedError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E28).copy(alpha = 0.95f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Icon(
                        Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Video Owner Restricted Web Embedding",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "You can still play the complete audio in MusicDrop or open this clip directly in YouTube.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onPlayFullSong(currentShort)
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                        ) {
                            Text("Play Audio", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/shorts/${currentShort.videoId}"))
                                context.startActivity(ytIntent)
                            }
                        ) {
                            Text("Open in YouTube", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ── Top Gradient Scrim & Controls ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Red.copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "SHORTS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Watch this music short: https://youtube.com/shorts/${currentShort.videoId}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Short"))
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
            }
        }

        // ── Right Floating Action Column ───────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Previous Short button
            if (pageIndex > 0) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous",
                        tint = Color.White
                    )
                }
            }

            // Play / Pause toggle button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { togglePlayPause() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isVideoPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    if (isVideoPlaying) "Pause" else "Play",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Like button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { isLiked = !isLiked },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else Color.White
                    )
                }
                Text(
                    if (isLiked) "Liked" else "Like",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Play Audio Only / Add to player
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        onPlayFullSong(currentShort)
                        onClose()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E).copy(alpha = 0.9f))
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = "Play Audio",
                        tint = Color.White
                    )
                }
                Text(
                    "Play Song",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Next Short button
            if (pageIndex < totalCount - 1) {
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }
            }
        }

        // ── Bottom Info Scrim (Title, Artist, Play Full Song) ───────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .padding(bottom = 8.dp)
            ) {
                // Artist badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Red),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            currentShort.channelTitle.firstOrNull()?.uppercase() ?: "M",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        currentShort.channelTitle.ifBlank { "Music Drop Shorts" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Title
                Text(
                    currentShort.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(10.dp))

                // "Listen to Full Song" Pill Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            onPlayFullSong(currentShort)
                            onClose()
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Listen full song in background",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
