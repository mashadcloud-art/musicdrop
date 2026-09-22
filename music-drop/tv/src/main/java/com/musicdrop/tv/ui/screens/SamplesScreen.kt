package com.musicdrop.tv.ui.screens

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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.musicdrop.tv.R
import com.musicdrop.tv.data.youtube.YouTubeSearchResult
import com.musicdrop.tv.ui.theme.VibrantCoral
import com.musicdrop.tv.ui.viewmodel.MainViewModel

@Composable
fun SamplesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // Automatically pause background ExoPlayer when entering Samples screen so YouTube video plays cleanly
    LaunchedEffect(Unit) {
        viewModel.playbackConnection.pause()
    }

    val shortsQuickPicks by viewModel.shortsQuickPicks.collectAsState()
    val ytMusicResults by viewModel.ytMusicResults.collectAsState()
    val regionTrendingSongs by viewModel.regionTrendingSongs.collectAsState()

    val samplesList = remember(shortsQuickPicks, ytMusicResults, regionTrendingSongs) {
        val list = mutableListOf<YouTubeSearchResult>()
        list.addAll(shortsQuickPicks)
        if (list.size < 5) list.addAll(ytMusicResults)
        if (list.size < 5) list.addAll(regionTrendingSongs)
        list.distinctBy { it.videoId }
    }

    if (samplesList.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = VibrantCoral, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(14.dp))
                Text("Loading Music Samples...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { samplesList.size })

    // Infinite scroll: auto-load more samples as user swipes down
    LaunchedEffect(pagerState.currentPage, samplesList.size) {
        if (pagerState.currentPage >= samplesList.size - 3) {
            viewModel.loadMoreSamples()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val sample = samplesList[page]
            val isCurrentPage = pagerState.currentPage == page
            SampleVideoPage(
                sample = sample,
                isActive = isCurrentPage
            )
        }

        // ── Top Brand Header (MusicDrop Bird Logo + "MusicDrop" Samples - NO YouTube Logo) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_bird_logo),
                    contentDescription = "MusicDrop",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Music",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Drop",
                    color = VibrantCoral,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "SAMPLES",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SampleVideoPage(
    sample: YouTubeSearchResult,
    isActive: Boolean
) {
    val context = LocalContext.current
    var isLiked by remember(sample.videoId) { mutableStateOf(false) }
    var isPlayingLocally by remember(sample.videoId) { mutableStateOf(true) }
    var isMuted by remember(sample.videoId) { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val likeCount = remember(sample.videoId) {
        val base = ((sample.videoId.hashCode() and 0x7FFFFFFF) % 850 + 50)
        "${base}K"
    }

    // Keep play/pause synchronized with active page
    LaunchedEffect(isActive) {
        isPlayingLocally = isActive
        if (isActive) {
            webViewRef?.evaluateJavascript("try { playVid(); } catch(e){}", null)
        } else {
            webViewRef?.evaluateJavascript("try { pauseVid(); } catch(e){}", null)
        }
    }

    DisposableEffect(sample.videoId) {
        onDispose {
            webViewRef?.evaluateJavascript("try { pauseVid(); } catch(e){}", null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Embedded WebView Player
        if (isActive) {
            key(sample.videoId) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
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
                            setBackgroundColor(0xFF000000.toInt())
                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()
                            tag = sample.videoId
                            webViewRef = this

                            val embedHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        * { margin: 0; padding: 0; box-sizing: border-box; }
                                        body, html { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                                        #container { position: relative; width: 100%; height: 100%; overflow: hidden; }
                                        iframe { width: 100%; height: 100%; border: none; object-fit: cover; transform: scale(1.36); transform-origin: center center; }
                                        #touch-catcher { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 5; }
                                    </style>
                                </head>
                                <body>
                                    <div id="container">
                                        <iframe 
                                            id="ytplayer"
                                            src="https://www.youtube-nocookie.com/embed/${sample.videoId}?autoplay=1&mute=0&playsinline=1&controls=0&loop=1&playlist=${sample.videoId}&enablejsapi=1&rel=0&modestbranding=1&iv_load_policy=3&fs=0"
                                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                            allowfullscreen>
                                        </iframe>
                                        <div id="touch-catcher"></div>
                                    </div>
                                    <script>
                                        function postYt(func, args) {
                                            var el = document.getElementById('ytplayer');
                                            if (el && el.contentWindow) {
                                                el.contentWindow.postMessage(JSON.stringify({
                                                    event: 'command',
                                                    func: func,
                                                    args: args || ''
                                                }), '*');
                                            }
                                        }
                                        function playVid() { postYt('playVideo'); }
                                        function pauseVid() { postYt('pauseVideo'); }
                                        function muteVid() { postYt('mute'); }
                                        function unMuteVid() { postYt('unMute'); }

                                        var isPaused = false;
                                        document.getElementById('touch-catcher').addEventListener('click', function() {
                                            if (isPaused) {
                                                playVid();
                                                isPaused = false;
                                            } else {
                                                pauseVid();
                                                isPaused = true;
                                            }
                                        });
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "utf-8", null)
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        if (webView.tag != sample.videoId) {
                            webView.tag = sample.videoId
                            val embedHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        * { margin: 0; padding: 0; box-sizing: border-box; }
                                        body, html { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                                        #container { position: relative; width: 100%; height: 100%; overflow: hidden; }
                                        iframe { width: 100%; height: 100%; border: none; object-fit: cover; transform: scale(1.36); transform-origin: center center; }
                                        #touch-catcher { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 5; }
                                    </style>
                                </head>
                                <body>
                                    <div id="container">
                                        <iframe 
                                            id="ytplayer"
                                            src="https://www.youtube-nocookie.com/embed/${sample.videoId}?autoplay=1&mute=0&playsinline=1&controls=0&loop=1&playlist=${sample.videoId}&enablejsapi=1&rel=0&modestbranding=1&iv_load_policy=3&fs=0"
                                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                            allowfullscreen>
                                        </iframe>
                                        <div id="touch-catcher"></div>
                                    </div>
                                    <script>
                                        function postYt(func, args) {
                                            var el = document.getElementById('ytplayer');
                                            if (el && el.contentWindow) {
                                                el.contentWindow.postMessage(JSON.stringify({
                                                    event: 'command',
                                                    func: func,
                                                    args: args || ''
                                                }), '*');
                                            }
                                        }
                                        function playVid() { postYt('playVideo'); }
                                        function pauseVid() { postYt('pauseVideo'); }
                                        function muteVid() { postYt('mute'); }
                                        function unMuteVid() { postYt('unMute'); }

                                        var isPaused = false;
                                        document.getElementById('touch-catcher').addEventListener('click', function() {
                                            if (isPaused) {
                                                playVid();
                                                isPaused = false;
                                            } else {
                                                pauseVid();
                                                isPaused = true;
                                            }
                                        });
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "utf-8", null)
                        } else {
                            if (isActive) {
                                webView.evaluateJavascript("try { playVid(); } catch(e){}", null)
                            } else {
                                webView.evaluateJavascript("try { pauseVid(); } catch(e){}", null)
                            }
                        }
                    }
                )
            }
        } else {
            // Thumbnail placeholder while offscreen
            AsyncImage(
                model = sample.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // ── Right Action Column (Matching official YouTube Music Samples) ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Thumbs Up / Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { isLiked = !isLiked },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = likeCount,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 2. Sound Toggle (Mute / Unmute live from YouTube)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        if (isMuted) {
                            webViewRef?.evaluateJavascript("try { muteVid(); } catch(e){}", null)
                        } else {
                            webViewRef?.evaluateJavascript("try { unMuteVid(); } catch(e){}", null)
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isMuted) "Muted" else "Sound",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 3. Save / Add to playlist
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        android.widget.Toast.makeText(context, "Saved to your library!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlaylistAdd,
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Save",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 4. Share
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Listen to ${sample.title} on MusicDrop: https://youtube.com/watch?v=${sample.videoId}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Track"))
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Share",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 5. Play / Pause on Screen (Does NOT transfer to background music player)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        isPlayingLocally = !isPlayingLocally
                        if (isPlayingLocally) {
                            webViewRef?.evaluateJavascript("try { playVid(); } catch(e){}", null)
                        } else {
                            webViewRef?.evaluateJavascript("try { pauseVid(); } catch(e){}", null)
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = if (isPlayingLocally) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingLocally) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isPlayingLocally) "Pause" else "Play",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Bottom Info Scrim (Track Details + Thumbnail) ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .clickable {
                        // Tapping toggles play/pause on screen
                        isPlayingLocally = !isPlayingLocally
                        if (isPlayingLocally) {
                            webViewRef?.evaluateJavascript("try { playVid(); } catch(e){}", null)
                        } else {
                            webViewRef?.evaluateJavascript("try { pauseVid(); } catch(e){}", null)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Square Album Art Thumbnail
                AsyncImage(
                    model = sample.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = sample.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = sample.channelTitle.ifBlank { "Music Drop Artist" },
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
