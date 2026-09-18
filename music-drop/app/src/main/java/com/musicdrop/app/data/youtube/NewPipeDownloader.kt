package com.musicdrop.app.data.youtube

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Request as OkHttpRequest

/**
 * NewPipeExtractor's [Downloader] implementation, backed by OkHttp.
 *
 * This is plumbing only — the actual YouTube extraction logic (solving
 * signatureCipher / the n-parameter by running the real player JS through the
 * bundled Rhino engine) lives inside NewPipeExtractor itself. All this class does
 * is give it a way to make HTTP requests, matching the reference implementation
 * NewPipeExtractor's own test suite uses (DownloaderTestImpl).
 */
class NewPipeDownloader private constructor() : Downloader() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        var requestBody: RequestBody? = null
        if (dataToSend != null && dataToSend.isNotEmpty()) {
            requestBody = dataToSend.toRequestBody(null, 0, dataToSend.size)
        }

        val requestBuilder = OkHttpRequest.Builder()
            .method(httpMethod, requestBody)
            .url(url)
            .addHeader("User-Agent", USER_AGENT)

        for ((headerName, headerValues) in headers) {
            if (headerValues.isEmpty()) continue
            requestBuilder.removeHeader(headerName)
            for (value in headerValues) {
                requestBuilder.addHeader(headerName, value)
            }
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested (HTTP 429)", url)
            }

            val responseBodyString = response.body?.string().orEmpty()
            val latestUrl = response.request.url.toString()

            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBodyString,
                latestUrl
            )
        }
    }

    companion object {
        // A common desktop UA — NewPipeExtractor's own reference downloader uses one too;
        // the WEB/mobile client identities are conveyed separately inside the InnerTube
        // request bodies NewPipeExtractor builds, not via this transport-level UA.
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

        @Volatile private var instance: NewPipeDownloader? = null
        fun getInstance(): NewPipeDownloader =
            instance ?: synchronized(this) {
                instance ?: NewPipeDownloader().also { instance = it }
            }
    }
}
