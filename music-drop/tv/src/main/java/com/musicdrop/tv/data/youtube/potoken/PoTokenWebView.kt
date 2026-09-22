package com.musicdrop.tv.data.youtube.potoken

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Generates YouTube poTokens using BotGuard running inside an offscreen WebView.
 *
 * The WebView itself has NO network access (blockNetworkLoads): the page is loaded from the
 * bundled po_token.html asset with `https://www.youtube.com` as base URL (so it gets a proper
 * YouTube origin), and all BotGuard service requests (api/jnn/v1/Create + GenerateIT) are
 * performed from Kotlin over OkHttp, with results injected back into the page via
 * evaluateJavascript. This is the same architecture as NewPipe's PoTokenWebView, adapted from
 * RxJava to Kotlin coroutines.
 */
class PoTokenWebView private constructor(
    private val webView: WebView
) : PoTokenGenerator {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Waiters for in-flight generatePoToken calls, keyed by identifier. */
    private val poTokenWaiters =
        mutableListOf<Pair<String, kotlinx.coroutines.CancellableContinuation<String>>>()

    /** Waiter for the one-time initialization (resumed with `this` once BotGuard is ready). */
    @Volatile
    private var initWaiter: kotlinx.coroutines.CancellableContinuation<PoTokenWebView>? = null

    @Volatile
    private var expirationInstant: Instant = Instant.EPOCH

    //region Initialization

    init {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.setSafeBrowsingEnabled(false)
        settings.userAgentString = USER_AGENT
        settings.blockNetworkLoads = true // the WebView itself does not need network access

        // so that we can run async functions and get back the result
        webView.addJavascriptInterface(this, JS_INTERFACE)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                if (m.message().contains("Uncaught")) {
                    // Everything that can fail is guarded by try-catch, so an "Uncaught" error
                    // very likely indicates a syntax error, i.e. a broken/old WebView JS engine.
                    val fmt = "\"${m.message()}\", source: ${m.sourceId()} (${m.lineNumber()})"
                    Log.e(TAG, "This WebView implementation is broken: $fmt")
                    val exception = BadWebViewException(fmt)
                    failInit(exception)
                    popAllPoTokenWaiters().forEach { (_, waiter) ->
                        waiter.resumeWithException(exception)
                    }
                }
                return super.onConsoleMessage(m)
            }
        }
    }

    /**
     * Must be called right after instantiating [PoTokenWebView]. Loads the bundled BotGuard
     * page (with a youtube.com base URL so the page has a proper origin) and appends the
     * bootstrap call that starts the Create -> runBotGuard -> GenerateIT flow.
     */
    private fun loadHtmlAndObtainBotguard(context: Context) {
        scope.launch {
            try {
                val html = withContext(Dispatchers.IO) {
                    context.assets.open("po_token.html").bufferedReader().use { it.readText() }
                }
                webView.loadDataWithBaseURL(
                    "https://www.youtube.com",
                    html.replaceFirst(
                        "</script>",
                        // calls downloadAndRunBotguard() when the page has finished loading
                        "\n$JS_INTERFACE.downloadAndRunBotguard()</script>"
                    ),
                    "text/html",
                    "utf-8",
                    null
                )
            } catch (t: Throwable) {
                failInit(PoTokenException("Could not load po_token.html: ${t.message}"))
            }
        }
    }

    /**
     * Called by the injected JS snippet once the page has loaded. Fetches the BotGuard
     * challenge data (Create endpoint), then runs the BotGuard VM inside the page.
     */
    @JavascriptInterface
    fun downloadAndRunBotguard() {
        scope.launch {
            try {
                makeBotguardServiceRequest(
                    "https://www.youtube.com/api/jnn/v1/Create",
                    "[ \"$REQUEST_KEY\" ]"
                ) { responseBody ->
                    val parsedChallengeData = parseChallengeData(responseBody)
                    evaluateJsOnPage(
                        """try {
                            data = $parsedChallengeData
                            runBotGuard(data).then(function (result) {
                                this.webPoSignalOutput = result.webPoSignalOutput
                                $JS_INTERFACE.onRunBotguardResult(result.botguardResponse)
                            }, function (error) {
                                $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                            })
                        } catch (error) {
                            $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                        }"""
                    )
                }
            } catch (t: Throwable) {
                failInit(t)
            }
        }
    }

    /** Called by the JS snippets when BotGuard initialization fails inside the page. */
    @JavascriptInterface
    fun onJsInitializationError(error: String) {
        Log.e(TAG, "Initialization error from JavaScript: $error")
        failInit(buildExceptionForJsError(error))
    }

    /**
     * Called by the JS snippet after the BotGuard VM produced its response. Exchanges it for
     * an integrity token (GenerateIT endpoint) and stores it in the page.
     */
    @JavascriptInterface
    fun onRunBotguardResult(botguardResponse: String) {
        scope.launch {
            try {
                makeBotguardServiceRequest(
                    "https://www.youtube.com/api/jnn/v1/GenerateIT",
                    "[ \"$REQUEST_KEY\", \"$botguardResponse\" ]"
                ) { responseBody ->
                    val (integrityTokenU8, expirationTimeInSeconds) =
                        parseIntegrityTokenData(responseBody)

                    // leave 10 minutes of margin just to be sure
                    expirationInstant = Instant.now().plusSeconds(expirationTimeInSeconds - 600)

                    evaluateJsOnPage("this.integrityToken = $integrityTokenU8") {
                        completeInitSuccess()
                    }
                }
            } catch (t: Throwable) {
                failInit(t)
            }
        }
    }
    //endregion

    //region Obtaining poTokens

    override suspend fun generatePoToken(identifier: String): String =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                synchronized(poTokenWaiters) {
                    poTokenWaiters.add(identifier to continuation)
                }
                continuation.invokeOnCancellation {
                    synchronized(poTokenWaiters) {
                        poTokenWaiters.removeAll { it.first == identifier && it.second === continuation }
                    }
                }
                val u8Identifier = stringToU8(identifier)
                webView.evaluateJavascript(
                    """try {
                        identifier = "$identifier"
                        u8Identifier = $u8Identifier
                        poTokenU8 = obtainPoToken(webPoSignalOutput, integrityToken, u8Identifier)
                        poTokenU8String = ""
                        for (i = 0; i < poTokenU8.length; i++) {
                            if (i != 0) poTokenU8String += ","
                            poTokenU8String += poTokenU8[i]
                        }
                        $JS_INTERFACE.onObtainPoTokenResult(identifier, poTokenU8String)
                    } catch (error) {
                        $JS_INTERFACE.onObtainPoTokenError(identifier, error + "\n" + error.stack)
                    }""",
                    null
                )
            }
        }

    /** Called by the JS snippet from [generatePoToken] when `obtainPoToken()` throws. */
    @JavascriptInterface
    fun onObtainPoTokenError(identifier: String, error: String) {
        Log.e(TAG, "obtainPoToken error from JavaScript: $error")
        popPoTokenWaiter(identifier)?.resumeWithException(buildExceptionForJsError(error))
    }

    /**
     * Called by the JS snippet from [generatePoToken] with the original identifier and the
     * raw byte output of `obtainPoToken()` (comma-separated byte values).
     */
    @JavascriptInterface
    fun onObtainPoTokenResult(identifier: String, poTokenU8: String) {
        val poToken = try {
            u8ToBase64(poTokenU8)
        } catch (t: Throwable) {
            popPoTokenWaiter(identifier)?.resumeWithException(t)
            return
        }
        popPoTokenWaiter(identifier)?.resume(poToken)
    }

    override fun isExpired(): Boolean = Instant.now().isAfter(expirationInstant)
    //endregion

    //region Helpers

    /**
     * Runs a BotGuard service request over OkHttp (the WebView itself has no network access).
     */
    private suspend fun <T> makeBotguardServiceRequest(
        url: String,
        data: String,
        handleResponseBody: suspend (String) -> T
    ): T = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json+protobuf")
            .header("x-goog-api-key", GOOGLE_API_KEY)
            .header("x-user-agent", "grpc-web-javascript/0.1")
            .post(data.toRequestBody("application/json+protobuf".toMediaType()))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful) {
                throw PoTokenException("Invalid response code: ${response.code}")
            }
            if (body.isNullOrEmpty()) {
                throw PoTokenException("Empty response body from $url")
            }
            handleResponseBody(body)
        }
    }

    private suspend fun evaluateJsOnPage(script: String, onDone: (() -> Unit)? = null) {
        withContext(Dispatchers.Main) {
            webView.evaluateJavascript(script) { onDone?.invoke() }
        }
    }

    private fun completeInitSuccess() {
        val waiter = synchronized(this@PoTokenWebView) {
            val w = initWaiter
            initWaiter = null
            w
        }
        waiter?.resume(this@PoTokenWebView)
    }

    private fun failInit(error: Throwable) {
        val waiter = synchronized(this@PoTokenWebView) {
            val w = initWaiter
            initWaiter = null
            w
        }
        if (waiter != null) {
            waiter.resumeWithException(error)
        } else {
            // initialization already finished: a later-stage failure means the generator is
            // unusable, so surface the error to any in-flight token waiters
            popAllPoTokenWaiters().forEach { (_, waiter2) -> waiter2.resumeWithException(error) }
        }
    }

    private fun popPoTokenWaiter(identifier: String): kotlinx.coroutines.CancellableContinuation<String>? {
        return synchronized(poTokenWaiters) {
            poTokenWaiters.indexOfFirst { it.first == identifier }.takeIf { it >= 0 }?.let {
                poTokenWaiters.removeAt(it).second
            }
        }
    }

    private fun popAllPoTokenWaiters(): List<Pair<String, kotlinx.coroutines.CancellableContinuation<String>>> {
        return synchronized(poTokenWaiters) {
            val result = poTokenWaiters.toList()
            poTokenWaiters.clear()
            result
        }
    }
    //endregion

    //region Close

    override fun close() {
        try {
            scope.cancel()
        } catch (_: Throwable) {
        }
        runOnMainThread {
            try {
                webView.clearHistory()
                // clears RAM cache and disk cache (globally for all WebViews)
                webView.clearCache(true)
                // ensures that the WebView isn't doing anything when destroying it
                webView.loadUrl("about:blank")
                webView.onPause()
                webView.removeAllViews()
                webView.destroy()
            } catch (_: Throwable) {
            }
        }
    }

    private fun closeQuietly() {
        try {
            close()
        } catch (_: Throwable) {
        }
    }

    private fun runOnMainThread(block: () -> Unit) {
        mainHandler.post(block)
    }
    //endregion

    companion object {
        private const val TAG = "PoTokenWebView"

        // Values ported from NewPipe's PoTokenWebView (stitched in from the upstream source;
        // these are public embedded BotGuard constants, not user secrets).
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"

        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
        private const val JS_INTERFACE = "PoTokenWebView"

        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        /**
         * Creates and fully initializes a WebView-backed generator. Must not be called from
         * the main thread (it hops to the main thread internally for WebView work).
         */
        @SuppressLint("SetJavaScriptEnabled")
        suspend fun newPoTokenGenerator(context: Context): PoTokenGenerator =
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val potWebView = PoTokenWebView(WebView(context.applicationContext))
                    potWebView.initWaiter = continuation
                    continuation.invokeOnCancellation { potWebView.closeQuietly() }
                    potWebView.loadHtmlAndObtainBotguard(context.applicationContext)
                }
            }
    }
}
