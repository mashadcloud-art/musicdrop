package com.musicdrop.app.data.youtube

import android.content.Context
import android.util.Log
import com.musicdrop.app.data.youtube.potoken.MusicDropPoTokenProvider
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Thin facade over [MusicDropPoTokenProvider] for callers that just need "a poToken for the
 * current session" (legacy YouTubeStreamExtractor path and MainViewModel).
 *
 * Historically this class ran its own WebView + /att/get BotGuard flow, which was broken:
 * it used the ANDROID client for the challenge request (YouTube answers 400
 * FAILED_PRECONDITION), the response shape no longer matched the parsing code, the file://
 * page was CORS-blocked (no allowUniversalAccessFromFileURLs), and the token was minted
 * against an empty identifier while player requests sent a random visitorData.
 *
 * It now delegates to the same NewPipe-style BotGuard generator that powers the
 * NewPipeExtractor PoTokenProvider, so the visitorData and streaming poToken are consistent
 * across both extraction paths.
 */
class PoTokenManager(@Suppress("UNUSED_PARAMETER") private val context: Context) {

    data class PoToken(
        val token: String,
        val visitorData: String,
        val generatedAtMs: Long = System.currentTimeMillis()
    ) {
        fun isValid(): Boolean =
            token.isNotBlank() &&
            System.currentTimeMillis() - generatedAtMs < 6 * 60 * 60 * 1000L // 6 hours
    }

    /**
     * Returns a valid streaming poToken (bound to [PoToken.visitorData]) or null if it could
     * not be generated. Never throws. Safe to call from any thread.
     */
    suspend fun getPoToken(): PoToken? {
        MusicDropPoTokenProvider.attach(context)
        return withTimeoutOrNull(30_000L) {
            try {
                val (visitorData, streamingPot) = MusicDropPoTokenProvider.getWebSessionPoToken()
                PoToken(token = streamingPot, visitorData = visitorData)
            } catch (t: Throwable) {
                Log.w("PoTokenManager", "PoToken unavailable: ${t.javaClass.simpleName}: ${t.message}")
                null
            }
        }
    }

    /** Clears the cached generator/tokens; next call regenerates. */
    fun invalidate() {
        MusicDropPoTokenProvider.invalidate()
    }

    /** Release generator resources (WebView teardown). */
    fun release() {
        MusicDropPoTokenProvider.release()
    }
}
