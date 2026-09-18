package com.musicdrop.app.auto

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Foreground Service that draws a full-screen YouTube WebView overlay
 * with OLED blackout screen-off power saving mode for car displays.
 *
 * Uses standard Android SYSTEM_ALERT_WINDOW overlay permission.
 */
class VideoOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var webView: WebView? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        var instance: VideoOverlayService? = null
        const val ACTION_SHOW    = "com.musicdrop.app.SHOW_VIDEO_OVERLAY"
        const val ACTION_HIDE    = "com.musicdrop.app.HIDE_VIDEO_OVERLAY"
        const val EXTRA_URL      = "url"
        const val EXTRA_VIDEO_ID = "videoId"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: ""
                val url = intent.getStringExtra(EXTRA_URL)
                    ?: if (videoId.isNotBlank()) "https://m.youtube.com/watch?v=$videoId&autoplay=1"
                    else "https://m.youtube.com"
                showOverlay(url)
            }
            ACTION_HIDE -> hideOverlay()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showOverlay(url: String) {
        hideOverlay() // remove any existing overlay

        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val reqIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(reqIntent)
            Toast.makeText(this, "Please allow 'Display over other apps' to use Car Video Overlay", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        val wm = windowManager ?: return

        val layout = FrameLayout(this)

        // Full-screen WebView
        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled                = true
                domStorageEnabled                = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode                 = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort                  = true
                loadWithOverviewMode             = true
                userAgentString                  =
                    "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Mobile Safari/537.36"
            }
            webChromeClient = WebChromeClient()
            loadUrl(url)
        }

        layout.addView(webView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Control buttons bar (Screen Off + Close)
        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        // Screen Blackout cover (OLED Screen-Off mode)
        var isBlackedOut = false
        val blackoutCover = View(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            visibility = View.GONE
        }

        val blackoutTv = TextView(this).apply {
            text = "🌙 Screen Off"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCC000000.toInt())
            setPadding(24, 14, 24, 14)
            gravity = Gravity.CENTER
            setOnClickListener {
                isBlackedOut = !isBlackedOut
                blackoutCover.visibility = if (isBlackedOut) View.VISIBLE else View.GONE
                Toast.makeText(
                    this@VideoOverlayService,
                    if (isBlackedOut) "Phone screen blacked out (OLED power saving). Double-tap screen to wake." else "Screen restored",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        buttonBar.addView(blackoutTv)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(16, 1)
        }
        buttonBar.addView(spacer)

        // Close button — top-right corner
        val closeTv = TextView(this).apply {
            text = "✕ Close"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCC000000.toInt())
            setPadding(24, 14, 24, 14)
            gravity = Gravity.CENTER
            setOnClickListener { hideOverlay() }
        }
        buttonBar.addView(closeTv)

        val barLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 28
            marginEnd = 28
        }
        layout.addView(buttonBar, barLp)

        // Blackout cover on top of everything — tap to wake
        blackoutCover.setOnClickListener {
            isBlackedOut = false
            blackoutCover.visibility = View.GONE
        }
        layout.addView(blackoutCover, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Acquire WakeLock so CPU never sleeps while video overlay is running
        try {
            val pm = getSystemService(POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicDrop:CarVideoWakeLock")
            wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours max
        } catch (_: Exception) {}

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            wm.addView(layout, params)
            overlayView = layout
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to display overlay: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideOverlay() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        wakeLock = null

        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }

        webView?.apply {
            stopLoading()
            destroy()
            webView = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        hideOverlay()
        instance = null
        super.onDestroy()
    }
}
