package com.musicdrop.app.auto

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.repository.MediaStoreRepository
import com.musicdrop.app.data.repository.SaavnRepository
import com.musicdrop.app.data.youtube.NewPipeYouTubeExtractor
import com.musicdrop.app.data.youtube.YouTubeMusicRepository
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.data.youtube.YouTubeStreamExtractor
import com.musicdrop.app.playback.FileDropMediaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android Auto Car App Service.
 * Renders a YouTube / music browser on the car screen.
 * Since we're sideloading, developer mode must be enabled on the head unit.
 *
 * Enable developer mode on Android Auto:
 * Phone → Android Auto → tap version 10 times → Developer settings → Unknown sources ON
 */
class FileDropCarService : CarAppService() {

    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR // sideload — allow any host

    override fun onCreateSession(): Session = FileDropSession()
}

class FileDropSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = HomeCarScreen(carContext)
}

/**
 * Home screen shown in Android Auto — browse YouTube, Saavn, local music.
 */
class HomeCarScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onGetTemplate(): Template {
        val items = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("🎵 YouTube Music")
                    .addText("Browse & play YouTube songs")
                    .setOnClickListener { screenManager.push(YouTubeSearchCarScreen(carContext)) }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("📻 JioSaavn")
                    .addText("320kbps Hindi, English & Regional songs")
                    .setOnClickListener { screenManager.push(SaavnCarScreen(carContext)) }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("📁 Local Songs")
                    .addText("Songs on your phone")
                    .setOnClickListener { screenManager.push(LocalMusicCarScreen(carContext)) }
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("MusicDrop Car")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(items)
            .build()
    }
}

/**
 * YouTube Music browse/trending screen for Android Auto.
 */
class YouTubeSearchCarScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val results = mutableListOf<YouTubeSearchResult>()
    private var isLoading = true

    init {
        scope.launch {
            try {
                val page = YouTubeMusicRepository.getTrending()
                results.clear()
                results.addAll(page.results)
            } catch (_: Exception) {}
            isLoading = false
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        if (isLoading) {
            return ListTemplate.Builder()
                .setTitle("YouTube Music")
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        val items = ItemList.Builder()
        results.take(50).forEach { result ->
            items.addItem(
                Row.Builder()
                    .setTitle(result.title)
                    .addText(result.channelTitle)
                    .setOnClickListener {
                        screenManager.push(NowPlayingCarScreen(carContext, result.videoId, result.title, result.channelTitle))
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("YouTube Music")
            .setHeaderAction(Action.BACK)
            .setSingleList(items.build())
            .build()
    }
}

/**
 * JioSaavn browse screen for Android Auto.
 */
class SaavnCarScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val tracks = mutableListOf<MediaItem>()
    private var isLoading = true

    init {
        scope.launch(Dispatchers.IO) {
            try {
                val list = SaavnRepository.getTrending()
                tracks.addAll(list)
            } catch (_: Exception) {}
            isLoading = false
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        if (isLoading) {
            return ListTemplate.Builder()
                .setTitle("JioSaavn")
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        val items = ItemList.Builder()
        tracks.take(50).forEach { track ->
            items.addItem(
                Row.Builder()
                    .setTitle(track.name)
                    .addText("${track.artist} • 320kbps")
                    .setOnClickListener {
                        playTrack(track)
                        screenManager.push(AudioNowPlayingScreen(carContext, track.name, track.artist))
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("JioSaavn Trending")
            .setHeaderAction(Action.BACK)
            .setSingleList(items.build())
            .build()
    }

    private fun playTrack(track: MediaItem) {
        val intent = Intent(carContext, FileDropMediaService::class.java).apply {
            action = "com.musicdrop.app.ACTION_PLAY_URI"
            putExtra("uri", track.uri.toString())
            putExtra("title", track.name)
            putExtra("artist", track.artist)
            putExtra("thumb", track.albumArtUri?.toString())
        }
        carContext.startService(intent)
    }
}

/**
 * Local music browse screen for Android Auto.
 */
class LocalMusicCarScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tracks = mutableListOf<MediaItem>()
    private var isLoading = true

    init {
        scope.launch {
            try {
                val repo = MediaStoreRepository(carContext)
                tracks.addAll(repo.getAudioTracks(limit = 100).filter { it.isSong })
            } catch (_: Exception) {}
            isLoading = false
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        if (isLoading) {
            return ListTemplate.Builder()
                .setTitle("Local Songs")
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        val items = ItemList.Builder()
        tracks.take(50).forEach { track ->
            items.addItem(
                Row.Builder()
                    .setTitle(track.name)
                    .addText(track.artist.ifBlank { "Unknown Artist" })
                    .setOnClickListener {
                        val intent = Intent(carContext, FileDropMediaService::class.java).apply {
                            action = "com.musicdrop.app.ACTION_PLAY_URI"
                            putExtra("uri", track.uri.toString())
                            putExtra("title", track.name)
                            putExtra("artist", track.artist)
                            putExtra("thumb", track.albumArtUri?.toString())
                        }
                        carContext.startService(intent)
                        screenManager.push(AudioNowPlayingScreen(carContext, track.name, track.artist))
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Local Songs")
            .setHeaderAction(Action.BACK)
            .setSingleList(items.build())
            .build()
    }
}

/**
 * Now Playing screen for YouTube video — extracts stream and plays via ExoPlayer.
 * The car screen shows title, channel, and playback controls.
 * Audio plays through car speakers via ExoPlayer.
 */
class NowPlayingCarScreen(
    carContext: androidx.car.app.CarContext,
    private val videoId: String,
    private val title: String,
    private val channel: String
) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var status = "Loading stream…"

    init {
        scope.launch(Dispatchers.IO) {
            status = "Extracting stream…"
            invalidate()
            var streamUrl: String? = null
            try {
                val np = NewPipeYouTubeExtractor.getInstance(carContext).extract(videoId)
                streamUrl = np?.url
            } catch (_: Exception) {}

            if (streamUrl == null) {
                try {
                    val fallback = YouTubeStreamExtractor.getInstance(carContext).extract(videoId)
                    streamUrl = fallback?.url
                } catch (_: Exception) {}
            }

            if (streamUrl != null) {
                status = "Playing"
                val intent = Intent(carContext, FileDropMediaService::class.java).apply {
                    action = "com.musicdrop.app.ACTION_PLAY_URI"
                    putExtra("uri", streamUrl)
                    putExtra("title", title)
                    putExtra("artist", channel)
                    putExtra("thumb", "https://i.ytimg.com/vi/$videoId/hqdefault.jpg")
                }
                carContext.startService(intent)
            } else {
                status = "Could not load stream"
            }
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val rows = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle(title)
                    .addText(channel)
                    .addText(status)
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("Now Playing")
            .setHeaderAction(Action.BACK)
            .setSingleList(rows)
            .build()
    }
}

/**
 * Simple audio Now Playing screen.
 */
class AudioNowPlayingScreen(
    carContext: androidx.car.app.CarContext,
    private val title: String,
    private val artist: String
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val rows = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle(title)
                    .addText(artist)
                    .addText("Playing via MusicDrop")
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("Now Playing")
            .setHeaderAction(Action.BACK)
            .setSingleList(rows)
            .build()
    }
}
