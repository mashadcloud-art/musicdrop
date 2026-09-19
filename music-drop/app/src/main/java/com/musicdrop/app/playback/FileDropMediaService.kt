package com.musicdrop.app.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicdrop.app.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileDropMediaService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "music_drop_playback_channel"
        const val NOTIFICATION_ID = 1001
    }

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // 1. Create Notification Channel for Foreground Media Playback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Drop Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background & Screen-off audio playback controls"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        // 2. Setup WakeLock and WifiLock for background/locked playback
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicDrop:PlaybackWakeLock")
            wakeLock?.setReferenceCounted(false)

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MusicDrop:WifiLock")
            wifiLock?.setReferenceCounted(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Setup AudioAttributes and ExoPlayer
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val chromeUserAgent =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        val ytCookies = try {
            android.webkit.CookieManager.getInstance().getCookie("https://m.youtube.com") ?: ""
        } catch (e: Exception) { "" }

        val headers = mutableMapOf(
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Origin" to "https://m.youtube.com",
            "Referer" to "https://m.youtube.com/",
            "Sec-Fetch-Dest" to "audio",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "cross-site"
        )
        if (ytCookies.isNotBlank()) {
            headers["Cookie"] = ytCookies
        }

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent(chromeUserAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(25_000)
            .setReadTimeoutMs(60_000)
            .setDefaultRequestProperties(headers)

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1000
            )
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                // Eliminate leading/trailing dead air & silence so tracks start and end instantly
                skipSilenceEnabled = true
            }

        // Auto-clean audio cache older than 30 minutes in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.musicdrop.app.data.cache.AudioCacheManager.pruneOldAudioCache(applicationContext)
        }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    try {
                        wakeLock?.acquire(3 * 60 * 60 * 1000L) // up to 3 hours
                        wifiLock?.acquire()
                    } catch (e: Exception) { e.printStackTrace() }
                } else {
                    try {
                        if (wakeLock?.isHeld == true) wakeLock?.release()
                        if (wifiLock?.isHeld == true) wifiLock?.release()
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // Guaranteed background queue auto-advance when song ends naturally
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                    } else if (PlaybackQueueBridge.hasNext()) {
                        PlaybackQueueBridge.onNext()
                    }
                }
            }
        })

        // ForwardingPlayer to ensure Previous, Play/Pause, Next are ALWAYS active on Lock Screen & Notification
        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .build()
            }

            override fun seekToNextMediaItem() {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                } else if (PlaybackQueueBridge.hasNext()) {
                    PlaybackQueueBridge.onNext()
                }
            }

            override fun seekToPreviousMediaItem() {
                if (player.currentPosition > 3000L) {
                    player.seekTo(0L)
                } else if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                } else if (PlaybackQueueBridge.hasPrevious()) {
                    PlaybackQueueBridge.onPrevious()
                } else {
                    player.seekTo(0L)
                }
            }

            // The system media notification / lock screen / Bluetooth headset buttons can
            // dispatch either the "MediaItem" or plain seekToNext/seekToPrevious commands
            // depending on platform version — route both through the same logic above so
            // Next/Previous behaves identically no matter which one fires.
            override fun seekToNext() = seekToNextMediaItem()
            override fun seekToPrevious() = seekToPreviousMediaItem()
        }

        // 4. Session Activity Intent
        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bitmapLoader = androidx.media3.datasource.DataSourceBitmapLoader(this)

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .setBitmapLoader(bitmapLoader)
            .setCallback(object : MediaSession.Callback {
                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    return when (playerCommand) {
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> androidx.media3.session.SessionResult.RESULT_SUCCESS
                        else -> super.onPlayerCommandRequest(session, controller, playerCommand)
                    }
                }
            })
            .build()

        // 5. Media Notification Provider
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(com.musicdrop.app.R.string.app_name)
                .setNotificationId(NOTIFICATION_ID)
                .build()
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.musicdrop.app.ACTION_PLAY_URI" || intent?.action == "com.filedrop.app.ACTION_PLAY_URI") {
            val uriStr = intent.getStringExtra("uri")
            val title = intent.getStringExtra("title") ?: "Now Playing"
            val artist = intent.getStringExtra("artist") ?: "MusicDrop"
            val thumb = intent.getStringExtra("thumb")
            if (!uriStr.isNullOrBlank()) {
                val mediaItem = androidx.media3.common.MediaItem.Builder()
                    .setUri(android.net.Uri.parse(uriStr))
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(artist)
                            .setArtworkUri(thumb?.let { android.net.Uri.parse(it) })
                            .build()
                    )
                    .build()
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) { e.printStackTrace() }

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
