package com.musicdrop.tv.data

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
            val responseHeaders = mutableMapOf<String, List<String>>()
            for (name in response.headers.names()) {
                responseHeaders[name] = response.headers.values(name)
            }

            return Response(
                response.code,
                response.message,
                responseHeaders,
                responseBodyString,
                response.request.url.toString()
            )
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

        private var instance: NewPipeDownloader? = null

        fun getInstance(): NewPipeDownloader {
            return instance ?: synchronized(this) {
                instance ?: NewPipeDownloader().also { instance = it }
            }
        }
    }
}
