package com.musicdrop.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.musicdrop.app.data.model.MediaItem as AppMediaItem
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackConnection(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<AppMediaItem?>(null)
    val currentTrack: StateFlow<AppMediaItem?> = _currentTrack.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private var playlist: List<AppMediaItem> = emptyList()
    private var progressJob: Job? = null
    private var errorRetryCount = 0
    private val scope = CoroutineScope(Dispatchers.Main)
    private var pendingAction: ((MediaController) -> Unit)? = null

    /**
     * Called when playback fails in a way that looks like a stale/expired/IP-locked
     * stream URL (YouTube's signed googlevideo.com links are the classic case — they
     * can stop working mid-session, e.g. after a wifi/mobile-data handoff changes the
     * device's IP) rather than a transient network blip. The caller (MainViewModel)
     * should re-run extraction for the current track and call [playTrack] again with
     * a freshly obtained URL — just re-calling prepare()/play() on the same stale URL
     * would fail again every time, which is the "plays sometimes, not others" symptom.
     */
    var onNeedsFreshStream: (() -> Unit)? = null

    /** Called once retries are exhausted and playback has definitively failed. */
    var onPlaybackFailed: (() -> Unit)? = null

    /**
     * Called when the current track finishes playing naturally (not a manual skip/
     * pause) and there's nothing queued after it — the hook for auto-continuing
     * playback (more from the same artist/category), YouTube-Music-style, instead of
     * just stopping.
     */
    var onPlaybackEnded: (() -> Unit)? = null
    var onPreloadNextTrack: (() -> Unit)? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, FileDropMediaService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                setupPlayerListener()
                controller?.let { c ->
                    _isPlaying.value = c.isPlaying
                    _currentPositionMs.value = c.currentPosition.coerceAtLeast(0L)
                    _durationMs.value = c.duration.coerceAtLeast(0L)
                    _isShuffle.value = c.shuffleModeEnabled
                    _isRepeat.value = c.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF

                    syncTrackFromMediaItem(c.currentMediaItem)
                    if (_isPlaying.value) {
                        startProgressTracker()
                    }

                    pendingAction?.invoke(c)
                    pendingAction = null
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaybackConnection", "Failed to connect MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun withController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            action(c)
        } else {
            pendingAction = action
        }
    }

    private fun syncTrackFromMediaItem(mediaItem: MediaItem?) {
        if (mediaItem == null) return
        val mediaId = mediaItem.mediaId.toLongOrNull()
        val found = playlist.find { it.id == mediaId }
        if (found != null) {
            _currentTrack.value = found
        } else {
            val meta = mediaItem.mediaMetadata
            val title = meta.title?.toString()?.takeIf { it.isNotBlank() }
            val artist = meta.artist?.toString()?.takeIf { it.isNotBlank() }
            if (!title.isNullOrBlank()) {
                _currentTrack.value = AppMediaItem(
                    id = mediaId ?: mediaItem.mediaId.hashCode().toLong(),
                    uri = mediaItem.requestMetadata.mediaUri ?: android.net.Uri.EMPTY,
                    name = title,
                    size = 0L,
                    dateAdded = System.currentTimeMillis() / 1000L,
                    mimeType = "audio/mp4",
                    mediaType = com.musicdrop.app.data.model.MediaType.AUDIO,
                    durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L,
                    artist = artist ?: "MusicDrop",
                    album = meta.albumTitle?.toString() ?: "MusicDrop",
                    isSong = true,
                    filePath = mediaItem.mediaId,
                    albumArtUri = meta.artworkUri
                )
            }
        }
    }

    private fun setupPlayerListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
                    errorRetryCount = 0
                } else if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded?.invoke()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncTrackFromMediaItem(mediaItem)
                _durationMs.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
                errorRetryCount = 0
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                error.printStackTrace()

                if (errorRetryCount >= 2) {
                    errorRetryCount = 0
                    onPlaybackFailed?.invoke()
                    return
                }
                errorRetryCount++

                // These error codes are what a dead/expired/IP-locked stream URL looks
                // like to ExoPlayer (bad HTTP status such as 403/404, or the connection
                // simply failing) — a fresh URL is needed, not another attempt at the
                // same one. Anything else (a genuine transient blip) gets the old
                // blind prepare()+play() retry.
                val staleStreamCodes = intArrayOf(
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                )

                if (error.errorCode in staleStreamCodes && onNeedsFreshStream != null) {
                    onNeedsFreshStream?.invoke()
                } else {
                    scope.launch {
                        delay(1200)
                        controller?.prepare()
                        controller?.play()
                    }
                }
            }
        })
    }

    private var isPreloadTriggered = false
    private var isCrossfadingOut = false
    private var isOverlapTriggered = false
    private var fadeInJob: kotlinx.coroutines.Job? = null

    var isDjCrossfadeEnabled: Boolean = true
    var onOverlapNextTrack: (() -> Unit)? = null

    private fun fadeInVolume() {
        fadeInJob?.cancel()
        fadeInJob = scope.launch {
            controller?.let { c ->
                c.volume = 0.25f
                for (step in 3..10) {
                    delay(120)
                    if (!isActive) break
                    c.volume = step / 10f
                }
                c.volume = 1f
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        isCrossfadingOut = false
        isPreloadTriggered = false
        isOverlapTriggered = false
        progressJob = scope.launch {
            while (isActive) {
                controller?.let { c ->
                    val pos = c.currentPosition.coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    val dur = c.duration
                    if (dur > 0 && dur != androidx.media3.common.C.TIME_UNSET) {
                        _durationMs.value = dur

                        // 1. Preload & pre-resolve the next track 18 seconds before end
                        if (dur > 25_000L && pos >= (dur - 18_000L) && !isPreloadTriggered) {
                            isPreloadTriggered = true
                            onPreloadNextTrack?.invoke()
                        }

                        // 2. Smooth DJ Mashup Crossfade & Overlap (if enabled by user in Settings)
                        if (isDjCrossfadeEnabled && dur > 10_000L && pos >= (dur - 4_500L)) {
                            isCrossfadingOut = true
                            val remaining = (dur - pos).coerceAtLeast(0L)
                            val fadeVol = (remaining / 4_500f).coerceIn(0.12f, 1f)
                            c.volume = fadeVol

                            // Start next song ~3.5s before current song ends for seamless DJ mashup mix
                            if (pos >= (dur - 3_500L) && !isOverlapTriggered) {
                                isOverlapTriggered = true
                                onOverlapNextTrack?.invoke()
                            }
                        } else if (!isCrossfadingOut && (fadeInJob == null || fadeInJob?.isActive == false)) {
                            if (c.volume < 1f) c.volume = 1f
                        }
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        controller?.let {
            _currentPositionMs.value = it.currentPosition.coerceAtLeast(0L)
        }
    }

    fun setCurrentTrackMetadata(track: AppMediaItem) {
        _currentTrack.value = track
        if (track.durationMs > 0) {
            _durationMs.value = track.durationMs
        }
    }

    fun playTrack(track: AppMediaItem, currentList: List<AppMediaItem>, startPositionMs: Long = 0L) {
        playlist = currentList
        _currentTrack.value = track
        if (track.durationMs > 0) {
            _durationMs.value = track.durationMs
        }

        val mediaItems = currentList.map { item ->
            val builder = MediaItem.Builder()
                .setMediaId(item.id.toString())
                .setUri(item.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.name)
                        .setArtist(item.artist)
                        .setAlbumTitle(item.album)
                        .setArtworkUri(item.albumArtUri)
                        .build()
                )

            val effectiveMime = when {
                // Video Mode: keep the real video/* mimeType so ExoPlayer knows to pick
                // video renderers, instead of falling through to audio-only mappings.
                item.mediaType == com.musicdrop.app.data.model.MediaType.VIDEO || item.mimeType.startsWith("video/", ignoreCase = true) -> {
                    if (item.mimeType.startsWith("video/")) item.mimeType else androidx.media3.common.MimeTypes.VIDEO_MP4
                }
                item.mimeType.contains("mp4", ignoreCase = true) || item.mimeType.contains("m4a", ignoreCase = true) -> androidx.media3.common.MimeTypes.AUDIO_MP4
                item.mimeType.contains("mpeg", ignoreCase = true) || item.mimeType.contains("mp3", ignoreCase = true) -> androidx.media3.common.MimeTypes.AUDIO_MPEG
                item.mimeType.contains("opus", ignoreCase = true) -> androidx.media3.common.MimeTypes.AUDIO_OPUS
                item.mimeType.contains("ogg", ignoreCase = true) -> androidx.media3.common.MimeTypes.AUDIO_OGG
                item.mimeType.contains("wav", ignoreCase = true) -> androidx.media3.common.MimeTypes.AUDIO_WAV
                item.mimeType.contains("flac", ignoreCase = true) -> androidx.media3.common.MimeTypes.AUDIO_FLAC
                item.mimeType.contains("aac", ignoreCase = true) -> androidx.media3.common.MimeTypes.AUDIO_AAC
                item.mimeType.contains("mpegurl", ignoreCase = true) || item.mimeType.contains("m3u8", ignoreCase = true) -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                item.uri.toString().contains("googlevideo.com") -> androidx.media3.common.MimeTypes.AUDIO_MP4
                else -> null
            }
            if (effectiveMime != null) {
                builder.setMimeType(effectiveMime)
            }
            builder.build()
        }

        val startIndex = currentList.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        withController { c ->
            c.volume = 0.25f
            c.setMediaItems(mediaItems, startIndex, startPositionMs)
            c.prepare()
            c.play()
            fadeInVolume()
        }
    }

    fun togglePlayPause() {
        withController { c ->
            if (c.isPlaying) c.pause() else c.play()
        }
    }

    fun seekTo(positionMs: Long) {
        withController { c ->
            c.seekTo(positionMs)
        }
        _currentPositionMs.value = positionMs
    }

    var onSkipPreviousAction: (() -> Unit)? = null
    var onSkipNextAction: (() -> Unit)? = null

    fun skipNext() {
        withController { c ->
            if (c.hasNextMediaItem()) {
                c.seekToNextMediaItem()
            } else if (onSkipNextAction != null) {
                onSkipNextAction?.invoke()
            } else {
                onPlaybackEnded?.invoke()
            }
        }
    }

    fun skipPrevious() {
        val pos = _currentPositionMs.value
        if (pos > 3000L) {
            seekTo(0L)
            return
        }
        withController { c ->
            if (c.hasPreviousMediaItem()) {
                c.seekToPreviousMediaItem()
            } else if (onSkipPreviousAction != null) {
                onSkipPreviousAction?.invoke()
            } else {
                c.seekTo(0L)
            }
        }
    }

    fun toggleShuffle() {
        val newMode = !_isShuffle.value
        _isShuffle.value = newMode
        withController { c ->
            c.shuffleModeEnabled = newMode
        }
    }

    fun toggleRepeat() {
        val newMode = !_isRepeat.value
        _isRepeat.value = newMode
        withController { c ->
            c.repeatMode = if (newMode) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    fun pause() {
        withController { c ->
            c.pause()
        }
    }

    fun play() {
        withController { c ->
            c.play()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        withController { c ->
            c.setPlaybackSpeed(speed)
        }
    }

    fun release() {
        stopProgressTracker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

    /**
     * Video Mode: attach a PlayerView's surface to the same MediaController that
     * already drives audio playback, so a video-stream MediaItem (see
     * MainViewModel.toggleVideoMode) renders its picture without needing a second,
     * separate player. Safe to call before the controller has connected — PlayerView
     * just won't have anything to render until it does.
     */
    fun bindPlayerView(playerView: androidx.media3.ui.PlayerView) {
        withController { c ->
            try {
                playerView.player = c
            } catch (t: Throwable) {
                android.util.Log.e("PlaybackConnection", "bindPlayerView error: ${t.message}")
            }
        }
    }

    fun unbindPlayerView(playerView: androidx.media3.ui.PlayerView) {
        try {
            if (playerView.player === controller) {
                playerView.player = null
            }
        } catch (t: Throwable) {
            android.util.Log.e("PlaybackConnection", "unbindPlayerView error: ${t.message}")
        }
    }
}
