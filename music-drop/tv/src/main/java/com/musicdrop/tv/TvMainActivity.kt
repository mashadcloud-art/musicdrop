package com.musicdrop.tv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.musicdrop.tv.bridge.TvNativeBridge
import com.musicdrop.tv.chromium.TvChromiumHelper
import com.musicdrop.tv.ui.components.TvFocusButton
import com.musicdrop.tv.ui.theme.MusicDropTvTheme

class TvMainActivity : ComponentActivity() {

    private lateinit var nativeBridge: TvNativeBridge
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep TV display awake while watching videos
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        nativeBridge = TvNativeBridge(this, lifecycleScope)

        setContent {
            MusicDropTvTheme {
                TvChromiumApp(
                    bridge = nativeBridge,
                    onWebViewCreated = { wv -> webView = wv }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Intercept TV Remote Back key to navigate browser history
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            webView?.let { wv ->
                if (wv.canGoBack()) {
                    wv.goBack()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.destroy()
        webView = null
    }
}

@Composable
fun TvChromiumApp(
    bridge: TvNativeBridge,
    onWebViewCreated: (WebView) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── 1. The Genuine YouTube on TV Chromium Engine View ───────────────
        AndroidView(
            factory = { context ->
                TvChromiumHelper.createTvWebView(
                    context = context,
                    bridge = bridge,
                    onPageFinished = { isLoading = false }
                ).also { wv ->
                    webViewRef = wv
                    onWebViewCreated(wv)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── 2. Top-Right Download HUD (Pops out when needed) ────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download 4K/HD Video Button
            TvFocusButton(
                onClick = { bridge.downloadCurrentVideo() },
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Download, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Download Video", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Download MP3 Audio Button
            TvFocusButton(
                onClick = { bridge.downloadCurrentAudio() },
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Download MP3", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Reload / Refresh
            TvFocusButton(
                onClick = { webViewRef?.reload() },
                cornerRadius = 50.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Refresh, "Reload", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── 3. Initial Loading Splash ───────────────────────────────────────
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0F0F)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_musicdrop_bird),
                        contentDescription = "MusicDrop TV",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Text("Starting YouTube TV Engine...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    CircularProgressIndicator(color = Color(0xFFFF0000), strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}
