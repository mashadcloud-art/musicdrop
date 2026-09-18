package com.musicdrop.app.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.musicdrop.app.data.youtube.YouTubeSearchResult

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ShortsFullScreenPlayer(
    shortsList: List<YouTubeSearchResult>,
    initialIndex: Int,
    onClose: () -> Unit,
    onPlayFullSong: (YouTubeSearchResult) -> Unit
) {
    if (shortsList.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, shortsList.size - 1)) }
    val currentShort = shortsList[currentIndex]
    var isLiked by remember(currentIndex) { mutableStateOf(false) }
    val context = LocalContext.current

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
            // ── Background Thumbnail / Card Artwork (Never a black screen!) ──
            coil.compose.AsyncImage(
                model = currentShort.thumbnailUrl,
                contentDescription = currentShort.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // ── WebView Embedded YouTube Shorts Video ──────────────────────────
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
                                mediaPlaybackRequiresUserGesture = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
                            }
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {}
                            tag = currentShort.videoId

                            val embedHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        * { margin: 0; padding: 0; box-sizing: border-box; }
                                        html, body { width: 100%; height: 100%; background: transparent; overflow: hidden; position: relative; }
                                        #player-wrapper { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: transparent; overflow: hidden; }
                                        iframe { position: absolute; top: 50%; left: 50%; width: 100%; height: 100%; min-width: 100%; min-height: 100%; border: none; transform: translate(-50%, -50%) scale(1.36); transform-origin: center center; }
                                    </style>
                                </head>
                                <body>
                                    <div id="player-wrapper" onclick="startPlay()">
                                        <iframe 
                                            id="ytplayer"
                                            type="text/html"
                                            src="https://www.youtube.com/embed/${currentShort.videoId}?autoplay=1&mute=0&playsinline=1&controls=0&loop=1&playlist=${currentShort.videoId}&enablejsapi=1&rel=0&modestbranding=1&iv_load_policy=3&vq=hd1080&hd=1&suggestedQuality=hd1080&origin=https://www.youtube.com&widget_referrer=https://www.youtube.com"
                                            frameborder="0"
                                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                            allowfullscreen>
                                        </iframe>
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
                                                            event.target.setPlaybackQuality('hd1080');
                                                            event.target.playVideo();
                                                        } catch(e) {}
                                                    },
                                                    'onStateChange': function(event) {
                                                        try {
                                                            if (event.data === 1) {
                                                                event.target.unMute();
                                                                event.target.setVolume(100);
                                                                event.target.setPlaybackQuality('hd1080');
                                                            }
                                                        } catch(e) {}
                                                    }
                                                }
                                            });
                                        }
                                        function startPlay() {
                                            if (!player) return;
                                            try {
                                                player.unMute();
                                                player.setVolume(100);
                                                player.playVideo();
                                            } catch(e) {}
                                        }
                                        function postYt(func) {
                                            if (!player) return;
                                            try {
                                                if (func === 'playVideo') {
                                                    player.unMute();
                                                    player.setVolume(100);
                                                    player.playVideo();
                                                } else if (func === 'pauseVideo') {
                                                    player.pauseVideo();
                                                }
                                            } catch(e) {}
                                        }
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "utf-8", null)
                        }
                    },
                    update = { webView ->
                        if (webView.tag != currentShort.videoId) {
                            webView.tag = currentShort.videoId
                            val embedHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        * { margin: 0; padding: 0; box-sizing: border-box; }
                                        html, body { width: 100%; height: 100%; background: transparent; overflow: hidden; position: relative; }
                                        #player-wrapper { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: transparent; overflow: hidden; }
                                        iframe { position: absolute; top: 50%; left: 50%; width: 100%; height: 100%; min-width: 100%; min-height: 100%; border: none; transform: translate(-50%, -50%) scale(1.36); transform-origin: center center; }
                                    </style>
                                </head>
                                <body>
                                    <div id="player-wrapper" onclick="startPlay()">
                                        <iframe 
                                            id="ytplayer"
                                            type="text/html"
                                            src="https://www.youtube.com/embed/${currentShort.videoId}?autoplay=1&mute=0&playsinline=1&controls=0&loop=1&playlist=${currentShort.videoId}&enablejsapi=1&rel=0&modestbranding=1&iv_load_policy=3&vq=hd1080&hd=1&suggestedQuality=hd1080&origin=https://www.youtube.com&widget_referrer=https://www.youtube.com"
                                            frameborder="0"
                                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                            allowfullscreen>
                                        </iframe>
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
                                                            event.target.setPlaybackQuality('hd1080');
                                                            event.target.playVideo();
                                                        } catch(e) {}
                                                    },
                                                    'onStateChange': function(event) {
                                                        try {
                                                            if (event.data === 1) {
                                                                event.target.unMute();
                                                                event.target.setVolume(100);
                                                                event.target.setPlaybackQuality('hd1080');
                                                            }
                                                        } catch(e) {}
                                                    }
                                                }
                                            });
                                        }
                                        function startPlay() {
                                            if (!player) return;
                                            try {
                                                player.unMute();
                                                player.setVolume(100);
                                                player.playVideo();
                                            } catch(e) {}
                                        }
                                        function postYt(func) {
                                            if (!player) return;
                                            try {
                                                if (func === 'playVideo') {
                                                    player.unMute();
                                                    player.setVolume(100);
                                                    player.playVideo();
                                                } else if (func === 'pauseVideo') {
                                                    player.pauseVideo();
                                                }
                                            } catch(e) {}
                                        }
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            webView.loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "utf-8", null)
                        }
                    }
                )
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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
                if (currentIndex > 0) {
                    IconButton(
                        onClick = { currentIndex-- },
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
                if (currentIndex < shortsList.size - 1) {
                    IconButton(
                        onClick = { currentIndex++ },
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
}
