package com.musicdrop.app.data.youtube.potoken

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor

/**
 * Supplies BotGuard-backed poTokens to NewPipeExtractor (registered via
 * [YoutubeStreamExtractor.setPoTokenProvider]).
 *
 * Same architecture as NewPipe's PoTokenProviderImpl: one shared, WebView-backed
 * [PoTokenGenerator] mints a "streaming" poToken bound to a real visitorData (fetched from
 * Innertube's visitor_id endpoint), and per-video "player request" poTokens bound to the
 * video id. The extractor attaches playerRequestPoToken to the player request body
 * (serviceIntegrityDimensions) and appends streamingDataPoToken as `&pot=` on googlevideo URLs.
 *
 * All public methods are blocking (NewPipeExtractor calls them from worker threads) and are
 * failure-isolated: they return null instead of throwing so extraction can proceed without
 * tokens as a graceful degradation.
 */
object MusicDropPoTokenProvider : PoTokenProvider {

    private const val TAG = "MusicDropPotoken"

    @Volatile
    private var appContext: Context? = null

    private val webViewSupported by lazy { checkWebViewAvailable() }

    @Volatile
    private var webViewBadImpl = false // whether the system has a broken WebView implementation

    private val lock = Any()
    private var webPoTokenVisitorData: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenGenerator? = null

    /** Called once with the app context before the extractor starts using this provider. */
    fun attach(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    //region PoTokenProvider

    override fun getWebClientPoToken(videoId: String): PoTokenResult? = guarded {
        val (generator, visitorData) = ensureWebGeneratorWithRetry()
        val playerPot = mintPlayerPot(videoId)
        PoTokenResult(visitorData, playerPot, webPoTokenStreamingPot!!)
    }

    /** WEB_EMBEDDED is not consumed by NewPipeExtractor 0.26.5; keep it cheap. */
    override fun getWebEmbedClientPoToken(videoId: String): PoTokenResult? = null

    override fun getAndroidClientPoToken(videoId: String): PoTokenResult? = guarded {
        val generator = ensureWebGeneratorWithRetry().first
        val localization = NewPipe.getPreferredLocalization()
        val contentCountry = NewPipe.getPreferredContentCountry()
        val info = InnertubeClientRequestInfo.ofAndroidClient()
        val visitorData = YoutubeParsingHelper.getVisitorDataFromInnertube(
            info,
            localization,
            contentCountry,
            mobileHeaders(YoutubeParsingHelper.getAndroidUserAgent(localization), info),
            YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL,
            null,
            false
        )
        val playerPot = mintPlayerPot(videoId)
        PoTokenResult(visitorData, playerPot, webPoTokenStreamingPot!!)
    }

    override fun getIosClientPoToken(videoId: String): PoTokenResult? = guarded {
        val generator = ensureWebGeneratorWithRetry().first
        val localization = NewPipe.getPreferredLocalization()
        val contentCountry = NewPipe.getPreferredContentCountry()
        val info = InnertubeClientRequestInfo.ofIosClient()
        val visitorData = YoutubeParsingHelper.getVisitorDataFromInnertube(
            info,
            localization,
            contentCountry,
            mobileHeaders(YoutubeParsingHelper.getIosUserAgent(localization), info),
            YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL,
            null,
            false
        )
        val playerPot = mintPlayerPot(videoId)
        PoTokenResult(visitorData, playerPot, webPoTokenStreamingPot!!)
    }
    //endregion

    //region Public suspend API (used by PoTokenManager / the legacy extractor)

    /**
     * Returns (visitorData, streamingPot) for the WEB session. The streaming pot is minted
     * exactly once per generator lifecycle and bound to the same visitorData that callers must
     * use on their player requests.
     */
    suspend fun getWebSessionPoToken(): Pair<String, String> = withContext(Dispatchers.IO) {
        ensureWebGeneratorWithRetry().let { (_, visitorData) ->
            visitorData to (webPoTokenStreamingPot
                ?: throw PoTokenException("streaming pot unavailable"))
        }
    }

    fun invalidate() {
        synchronized(lock) {
            webPoTokenGenerator?.let { Handler(Looper.getMainLooper()).post { it.closeQuietlySafe() } }
            webPoTokenGenerator = null
            webPoTokenVisitorData = null
            webPoTokenStreamingPot = null
        }
    }

    fun release() = invalidate()
    //endregion

    //region Internals

    private fun checkWebViewAvailable(): Boolean = try {
        android.webkit.WebView.getCurrentWebViewPackage() != null
    } catch (t: Throwable) {
        true // assume usable if the check itself is unavailable
    }

    private fun mobileHeaders(
        userAgent: String,
        info: InnertubeClientRequestInfo
    ): Map<String, List<String>> = mapOf(
        "User-Agent" to listOf(userAgent),
        "X-Goog-Api-Format-Version" to listOf("2"),
        "X-Youtube-Client-Name" to listOf(info.clientInfo.clientId ?: "1"),
        "X-Youtube-Client-Version" to listOf(info.clientInfo.clientVersion)
    )

    /**
     * Must be called under [lock] from a non-main thread (uses runBlocking internally).
     * Creates the WebView generator, a real visitorData and the session streaming pot.
     */
    private fun createWebGeneratorLocked(): Pair<PoTokenGenerator, String> {
        val context = appContext
            ?: throw PoTokenException("MusicDropPoTokenProvider not attached to a context")

        val generator = runBlocking {
            withTimeoutOrNull(45_000L) { PoTokenWebView.newPoTokenGenerator(context) }
                ?: throw PoTokenException("PoToken WebView initialization timed out")
        }

        val visitorData = runBlocking {
            withTimeoutOrNull(20_000L) {
                val info = InnertubeClientRequestInfo.ofWebClient()
                info.clientInfo.clientVersion = YoutubeParsingHelper.getClientVersion()
                YoutubeParsingHelper.getVisitorDataFromInnertube(
                    info,
                    NewPipe.getPreferredLocalization(),
                    NewPipe.getPreferredContentCountry(),
                    YoutubeParsingHelper.getYouTubeHeaders(),
                    YoutubeParsingHelper.YOUTUBEI_V1_URL,
                    null,
                    false
                )
            }
        } ?: throw PoTokenException("Could not get visitorData from Innertube")

        // The streaming poToken needs to be generated exactly once before any player token.
        val streamingPot = runBlocking {
            withTimeoutOrNull(15_000L) { generator.generatePoToken(visitorData) }
        } ?: throw PoTokenException("Could not mint streaming poToken")

        webPoTokenGenerator = generator
        webPoTokenVisitorData = visitorData
        webPoTokenStreamingPot = streamingPot
        Log.i(TAG, "PoToken generator ready (visitorData=${visitorData.take(12)}...)")
        return generator to visitorData
    }

    private fun ensureWebGeneratorWithRetry(): Pair<PoTokenGenerator, String> = synchronized(lock) {
        val current = webPoTokenGenerator
        if (current != null && !current.isExpired() &&
            webPoTokenVisitorData != null && webPoTokenStreamingPot != null
        ) {
            return current to webPoTokenVisitorData!!
        }
        // (re)create from scratch
        webPoTokenGenerator?.let { Handler(Looper.getMainLooper()).post { it.closeQuietlySafe() } }
        webPoTokenGenerator = null
        webPoTokenVisitorData = null
        webPoTokenStreamingPot = null
        createWebGeneratorLocked()
    }

    /** Mints the per-video player request pot; recreates the generator once on failure. */
    private fun mintPlayerPot(videoId: String): String {
        val first = try {
            runBlocking { withTimeoutOrNull(15_000L) { ensureWebGeneratorWithRetry().first.generatePoToken(videoId) } }
        } catch (t: Throwable) {
            null
        }
        if (first != null) return first

        Log.w(TAG, "player pot mint failed for $videoId, recreating generator once")
        synchronized(lock) {
            webPoTokenGenerator?.let { Handler(Looper.getMainLooper()).post { it.closeQuietlySafe() } }
            webPoTokenGenerator = null
            webPoTokenVisitorData = null
            webPoTokenStreamingPot = null
        }
        return runBlocking {
            withTimeoutOrNull(30_000L) {
                val (gen, _) = ensureWebGeneratorWithRetry()
                gen.generatePoToken(videoId)
            }
        } ?: throw PoTokenException("player pot mint timed out for $videoId")
    }

    private inline fun <T> guarded(block: () -> T?): T? {
        if (!webViewSupported || webViewBadImpl) return null
        return try {
            block()
        } catch (e: BadWebViewException) {
            webViewBadImpl = true
            Log.e(TAG, "WebView is broken, disabling poToken generation: ${e.message}")
            null
        } catch (e: Throwable) {
            Log.w(TAG, "poToken generation failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    private fun PoTokenGenerator.closeQuietlySafe() {
        try {
            close()
        } catch (_: Throwable) {
        }
    }
    //endregion
}
