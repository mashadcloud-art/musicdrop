package com.musicdrop.tv.chromium

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.KeyEvent
import android.webkit.*
import android.widget.FrameLayout
import com.musicdrop.tv.bridge.TvNativeBridge

object TvChromiumHelper {

    // PlayStation 4 Cobalt Leanback Shell User-Agent:
    // Recognized by Google for YouTube on TV, does NOT enforce Google Play Services device sign-in
    const val YOUTUBE_TV_USER_AGENT =
        "Mozilla/5.0 (PS4; Leanback Shell) Cobalt/26.lts.0-qa; compatible;"

    const val YOUTUBE_TV_URL = "https://www.youtube.com/tv"

    @SuppressLint("SetJavaScriptEnabled")
    fun createTvWebView(
        context: Context,
        bridge: TvNativeBridge,
        onPageFinished: (() -> Unit)? = null
    ): WebView {
        // Initialize CookieManager with Google consent cookies to prevent consent / sign-in dialogs
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setCookie(
                "https://www.youtube.com",
                "SOCS=CAESEwgDEgk0ODEzNzk5NDIaAmVuIAEaBgiA_LyaBg; path=/; domain=.youtube.com; Secure"
            )
            cookieManager.setCookie(
                "https://www.youtube.com",
                "CONSENT=YES+cb.20210720-07-p0.en+FX+417; path=/; domain=.youtube.com; Secure"
            )
            cookieManager.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            // Essential TV Chromium configurations
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()

            try {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                userAgentString = YOUTUBE_TV_USER_AGENT
                useWideViewPort = true
                loadWithOverviewMode = true
                allowFileAccess = true
                allowContentAccess = true
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
                    // Periodic injection to defeat dynamically loaded ads and popups
                    view?.evaluateJavascript(TvAdBypassScript.SCRIPT, null)
                }
            }

            // Map D-pad and TV Remote Keys directly into the Chromium page
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
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

