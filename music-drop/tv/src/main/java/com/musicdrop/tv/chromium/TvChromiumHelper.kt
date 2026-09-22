package com.musicdrop.tv.chromium

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import com.musicdrop.tv.bridge.TvNativeBridge

object TvChromiumHelper {

    // Official Android TV Smart TV User-Agent accepted by Google for https://www.youtube.com/tv
    const val YOUTUBE_TV_USER_AGENT =
        "Mozilla/5.0 (Linux; GoogleTV 14; BRAVIA 4K 2026 Build/1.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    const val YOUTUBE_TV_URL = "https://www.youtube.com/tv"

    @SuppressLint("SetJavaScriptEnabled")
    fun createTvWebView(
        context: Context,
        bridge: TvNativeBridge,
        onPageFinished: (() -> Unit)? = null
    ): WebView {
        return WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            // Essential TV Chromium configurations
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                userAgentString = YOUTUBE_TV_USER_AGENT
                useWideViewPort = true
                loadWithOverviewMode = true
                allowFileAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            // Expose our native bridge to JavaScript
            addJavascriptInterface(bridge, "MusicDropBridge")

            // Custom WebChromeClient for hardware acceleration & fullscreen video
            webChromeClient = object : WebChromeClient() {
                override fun getDefaultVideoPoster(): Bitmap? {
                    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                }
            }

            // Custom WebViewClient to inject Ad-Bypass script on every page change & redirect
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    view?.evaluateJavascript(TvAdBypassScript.SCRIPT, null)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(TvAdBypassScript.SCRIPT, null)
                    onPageFinished?.invoke()
                }

                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    // Periodic injection to defeat dynamically loaded ads
                    view?.evaluateJavascript(TvAdBypassScript.SCRIPT, null)
                }
            }

            // Map D-pad and TV Remote Keys directly into the Chromium page
            setOnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            // Forward Enter/OK to WebView
                            return@setOnKeyListener false
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            if (canGoBack()) {
                                goBack()
                                return@setOnKeyListener true
                            }
                        }
                    }
                }
                false
            }

            // Load official Google YouTube on TV interface
            loadUrl(YOUTUBE_TV_URL)
        }
    }
}
