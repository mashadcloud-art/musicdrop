package com.musicdrop.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class FileDropApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("MusicDrop", "Uncaught exception on thread: ${thread.name}", throwable)
            try {
                val logFile = java.io.File(filesDir, "crash.log")
                val trace = android.util.Log.getStackTraceString(throwable)
                logFile.writeText("Crash on ${java.util.Date()}\nThread: ${thread.name}\n\n$trace")
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components {
                add(VideoFrameDecoder.Factory())
                add(com.musicdrop.tv.util.AudioCoverFetcher.Factory(applicationContext))
            }
            .crossfade(true)
            .build()
    }
}
