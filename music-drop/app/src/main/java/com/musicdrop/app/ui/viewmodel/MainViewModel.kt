package com.musicdrop.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicdrop.app.data.model.InstalledAppInfo
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.MediaType
import com.musicdrop.app.data.model.StorageStats
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.data.repository.ContactsRepository
import com.musicdrop.app.data.repository.InstalledAppsRepository
import com.musicdrop.app.data.repository.MediaStoreRepository
import com.musicdrop.app.data.repository.RecentPlaysStore
import com.musicdrop.app.data.repository.DownloadedTracksStore
import com.musicdrop.app.data.repository.SentFilesLog
import com.musicdrop.app.data.repository.SentRecord
import com.musicdrop.app.data.repository.StorageRepository
import com.musicdrop.app.playback.PlaybackConnection
import com.musicdrop.app.data.repository.MusicFavoritesStore
import com.musicdrop.app.data.repository.SaavnRepository
import com.musicdrop.app.data.repository.LrcLibRepository
import com.musicdrop.app.data.repository.YouTubeChartsRepository
import com.musicdrop.app.data.repository.VimeoRepository
import com.musicdrop.app.data.repository.AppleMusicRepository
import com.musicdrop.app.data.repository.MusiXServerRepository
import com.musicdrop.app.data.repository.SpotifyRepository
import com.musicdrop.app.data.youtube.NewPipeYouTubeExtractor
import com.musicdrop.app.data.youtube.PoTokenManager
import com.musicdrop.app.data.youtube.YouTubeMusicRepository
import com.musicdrop.app.data.youtube.YouTubeStreamExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.*
import com.musicdrop.app.data.youtube.YouTubeSearchOutcome
import com.musicdrop.app.data.youtube.YouTubeSearchRepository
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.data.youtube.YouTubeExtractionResult
import com.musicdrop.app.util.ShareHelper
import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AudioFilter {
    ALL,
    SONGS,
    VOICE_NOTES
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaStoreRepository(application)
    private val storageRepository = StorageRepository(application)
    private val contactsRepository = ContactsRepository(application)
    private val appsRepository = InstalledAppsRepository(application)
    val playbackConnection = PlaybackConnection(application)
    val networkMonitor = com.musicdrop.app.data.network.NetworkMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    private val _networkStatusBanner = MutableStateFlow<String?>(null)
    val networkStatusBanner: StateFlow<String?> = _networkStatusBanner.asStateFlow()
    // p2p removed for standalone MusicDrop app

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _recentMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentMedia: StateFlow<List<MediaItem>> = _recentMedia.asStateFlow()

    private val _photos = MutableStateFlow<List<MediaItem>>(emptyList())
    val photos: StateFlow<List<MediaItem>> = _photos.asStateFlow()

    private val _videos = MutableStateFlow<List<MediaItem>>(emptyList())
    val videos: StateFlow<List<MediaItem>> = _videos.asStateFlow()

    private val _allAudio = MutableStateFlow<List<MediaItem>>(emptyList())
    val allAudio: StateFlow<List<MediaItem>> = _allAudio.asStateFlow()

    private val _songs = MutableStateFlow<List<MediaItem>>(emptyList())
    val songs: StateFlow<List<MediaItem>> = _songs.asStateFlow()

    private val _voiceNotes = MutableStateFlow<List<MediaItem>>(emptyList())
    val voiceNotes: StateFlow<List<MediaItem>> = _voiceNotes.asStateFlow()

    private val _documents = MutableStateFlow<List<MediaItem>>(emptyList())
    val documents: StateFlow<List<MediaItem>> = _documents.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _receivedFiles = MutableStateFlow<List<MediaItem>>(emptyList())
    val receivedFiles: StateFlow<List<MediaItem>> = _receivedFiles.asStateFlow()

    private val _sentFiles = MutableStateFlow<List<SentRecord>>(emptyList())
    val sentFiles: StateFlow<List<SentRecord>> = _sentFiles.asStateFlow()

    private val _audioFilter = MutableStateFlow(AudioFilter.ALL)
    val audioFilter: StateFlow<AudioFilter> = _audioFilter.asStateFlow()

    // ---- Downloads & Offline Library ----
    private val _downloadedTracks = MutableStateFlow<List<com.musicdrop.app.data.repository.DownloadedTrack>>(emptyList())
    val downloadedTracks: StateFlow<List<com.musicdrop.app.data.repository.DownloadedTrack>> = _downloadedTracks.asStateFlow()

    // ---- Liked Music (Auto Playlist) ----
    private val _likedMusic = MutableStateFlow<List<com.musicdrop.app.data.repository.LikedMusicItem>>(emptyList())
    val likedMusic: StateFlow<List<com.musicdrop.app.data.repository.LikedMusicItem>> = _likedMusic.asStateFlow()

    // ---- User Custom Playlists ----
    private val _userPlaylists = MutableStateFlow<List<com.musicdrop.app.data.repository.UserPlaylistItem>>(emptyList())
    val userPlaylists: StateFlow<List<com.musicdrop.app.data.repository.UserPlaylistItem>> = _userPlaylists.asStateFlow()

    // ---- Saved Artists & Albums ----
    private val _savedArtists = MutableStateFlow<List<com.musicdrop.app.data.repository.SavedArtistItem>>(emptyList())
    val savedArtists: StateFlow<List<com.musicdrop.app.data.repository.SavedArtistItem>> = _savedArtists.asStateFlow()

    private val _savedAlbums = MutableStateFlow<List<com.musicdrop.app.data.repository.SavedAlbumItem>>(emptyList())
    val savedAlbums: StateFlow<List<com.musicdrop.app.data.repository.SavedAlbumItem>> = _savedAlbums.asStateFlow()

    // ---- Bottom Navigation Bar Toggle (Hidden by default) ----
    private val settingsPrefs = application.getSharedPreferences("musicdrop_settings", android.content.Context.MODE_PRIVATE)
    private val _showBottomNav = MutableStateFlow(settingsPrefs.getBoolean("show_bottom_nav", false))
    val showBottomNav: StateFlow<Boolean> = _showBottomNav.asStateFlow()

    fun setShowBottomNav(show: Boolean) {
        _showBottomNav.value = show
        settingsPrefs.edit().putBoolean("show_bottom_nav", show).apply()
    }

    // ---- Player Theme ----
    private val _playerTheme = MutableStateFlow(
        try {
            val savedName = settingsPrefs.getString("player_theme", com.musicdrop.app.ui.theme.PlayerThemeId.DYNAMIC_BLUR.name)
            com.musicdrop.app.ui.theme.PlayerThemeId.valueOf(savedName ?: com.musicdrop.app.ui.theme.PlayerThemeId.DYNAMIC_BLUR.name)
        } catch (_: Exception) { com.musicdrop.app.ui.theme.PlayerThemeId.DYNAMIC_BLUR }
    )
    val playerTheme: StateFlow<com.musicdrop.app.ui.theme.PlayerThemeId> = _playerTheme.asStateFlow()

    fun setPlayerTheme(theme: com.musicdrop.app.ui.theme.PlayerThemeId) {
        _playerTheme.value = theme
        _playerSkinLayout.value = theme.defaultSkin
        settingsPrefs.edit().putString("player_theme", theme.name).putString("player_skin_layout", theme.defaultSkin.name).apply()
    }

    // ---- Player Skin Layout ----
    private val _playerSkinLayout = MutableStateFlow(
        try {
            val savedSkin = settingsPrefs.getString("player_skin_layout", com.musicdrop.app.ui.theme.PlayerSkinLayout.ROUNDED_CARD.name)
            com.musicdrop.app.ui.theme.PlayerSkinLayout.valueOf(savedSkin ?: com.musicdrop.app.ui.theme.PlayerSkinLayout.ROUNDED_CARD.name)
        } catch (_: Exception) { com.musicdrop.app.ui.theme.PlayerSkinLayout.ROUNDED_CARD }
    )
    val playerSkinLayout: StateFlow<com.musicdrop.app.ui.theme.PlayerSkinLayout> = _playerSkinLayout.asStateFlow()

    fun setPlayerSkinLayout(skin: com.musicdrop.app.ui.theme.PlayerSkinLayout) {
        _playerSkinLayout.value = skin
        settingsPrefs.edit().putString("player_skin_layout", skin.name).apply()
    }

    val equalizerManager: com.musicdrop.app.playback.EqualizerManager?
        get() = com.musicdrop.app.playback.FileDropMediaService.equalizerManager

    // ---- Hidden / Deleted Tracks (Filter unwanted songs from category feeds) ----
    private val _hiddenTrackKeys = MutableStateFlow<Set<String>>(com.musicdrop.app.data.repository.HiddenTracksStore.getHiddenKeys(application))
    val hiddenTrackKeys: StateFlow<Set<String>> = _hiddenTrackKeys.asStateFlow()

    fun isTrackFavorite(mediaItem: MediaItem): Boolean {
        val vid = mediaItem.filePath?.removePrefix("yt:") ?: mediaItem.id.toString()
        return MusicFavoritesStore.isFavorite(getApplication(), vid)
    }

    fun toggleFavoriteTrack(mediaItem: MediaItem): Boolean {
        val vid = mediaItem.filePath?.removePrefix("yt:") ?: mediaItem.id.toString()
        val ytr = YouTubeSearchResult(
            videoId = vid,
            title = mediaItem.name,
            channelTitle = mediaItem.artist,
            thumbnailUrl = mediaItem.albumArtUri?.toString().orEmpty(),
            duration = ""
        )
        val res = MusicFavoritesStore.toggle(getApplication(), ytr)
        _ytFavorites.value = MusicFavoritesStore.getAll(getApplication())
        return res
    }

    fun hideTrack(track: UnifiedTrack) {
        val ctx = getApplication<Application>()
        com.musicdrop.app.data.repository.HiddenTracksStore.hideTrack(ctx, track.key)
        com.musicdrop.app.data.repository.HiddenTracksStore.hideTrack(ctx, "yt:${track.key}")
        if (track.title.isNotBlank()) {
            com.musicdrop.app.data.repository.HiddenTracksStore.hideTrack(ctx, track.title.trim().lowercase())
        }
        _hiddenTrackKeys.value = com.musicdrop.app.data.repository.HiddenTracksStore.getHiddenKeys(ctx)
    }

    fun hideTrackByKey(key: String, title: String? = null) {
        val ctx = getApplication<Application>()
        com.musicdrop.app.data.repository.HiddenTracksStore.hideTrack(ctx, key)
        if (!title.isNullOrBlank()) {
            com.musicdrop.app.data.repository.HiddenTracksStore.hideTrack(ctx, title.trim().lowercase())
        }
        _hiddenTrackKeys.value = com.musicdrop.app.data.repository.HiddenTracksStore.getHiddenKeys(ctx)
    }

    fun unhideTrack(key: String) {
        val ctx = getApplication<Application>()
        com.musicdrop.app.data.repository.HiddenTracksStore.unhideTrack(ctx, key)
        _hiddenTrackKeys.value = com.musicdrop.app.data.repository.HiddenTracksStore.getHiddenKeys(ctx)
    }

    // ---- Multi-Region Quick Picks (India, Pakistan, Malayalam, Tamil) ----
    private val _indiaQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val indiaQuickPicks: StateFlow<List<YouTubeSearchResult>> = _indiaQuickPicks.asStateFlow()

    private val _pakistanQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val pakistanQuickPicks: StateFlow<List<YouTubeSearchResult>> = _pakistanQuickPicks.asStateFlow()

    private val _malayalamQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val malayalamQuickPicks: StateFlow<List<YouTubeSearchResult>> = _malayalamQuickPicks.asStateFlow()

    private val _tamilQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val tamilQuickPicks: StateFlow<List<YouTubeSearchResult>> = _tamilQuickPicks.asStateFlow()

    private val _teluguQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val teluguQuickPicks: StateFlow<List<YouTubeSearchResult>> = _teluguQuickPicks.asStateFlow()

    // Real-time download progress tracking (0.0f to 1.0f) and status label per track key
    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Float>> = _downloadProgressMap.asStateFlow()

    private val _downloadStatusMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val downloadStatusMap: StateFlow<Map<String, String>> = _downloadStatusMap.asStateFlow()

    // ---- Genre-style Quick Picks (Cover, Remix, Lofi, Guitar, Ukulele, Shorts) ----
    private val _coverQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val coverQuickPicks: StateFlow<List<YouTubeSearchResult>> = _coverQuickPicks.asStateFlow()

    private val _guitarQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val guitarQuickPicks: StateFlow<List<YouTubeSearchResult>> = _guitarQuickPicks.asStateFlow()

    private val _ukuleleQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val ukuleleQuickPicks: StateFlow<List<YouTubeSearchResult>> = _ukuleleQuickPicks.asStateFlow()

    private val _shortsQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val shortsQuickPicks: StateFlow<List<YouTubeSearchResult>> = _shortsQuickPicks.asStateFlow()

    private val _remixQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val remixQuickPicks: StateFlow<List<YouTubeSearchResult>> = _remixQuickPicks.asStateFlow()

    private val _lofiQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val lofiQuickPicks: StateFlow<List<YouTubeSearchResult>> = _lofiQuickPicks.asStateFlow()

    // ---- Regional Cover & Guitar collections (India / Pakistan) ----
    private val _indiaCoverQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val indiaCoverQuickPicks: StateFlow<List<YouTubeSearchResult>> = _indiaCoverQuickPicks.asStateFlow()

    private val _indiaGuitarQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val indiaGuitarQuickPicks: StateFlow<List<YouTubeSearchResult>> = _indiaGuitarQuickPicks.asStateFlow()

    private val _pakistanCoverQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val pakistanCoverQuickPicks: StateFlow<List<YouTubeSearchResult>> = _pakistanCoverQuickPicks.asStateFlow()

    private val _pakistanGuitarQuickPicks = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val pakistanGuitarQuickPicks: StateFlow<List<YouTubeSearchResult>> = _pakistanGuitarQuickPicks.asStateFlow()

    // ---- Local Offline Playback Queue ----
    private val _localQueue = MutableStateFlow<List<com.musicdrop.app.data.repository.DownloadedTrack>>(emptyList())
    val localQueue: StateFlow<List<com.musicdrop.app.data.repository.DownloadedTrack>> = _localQueue.asStateFlow()

    // ---- Explore Feed Data ----
    private val _exploreNewReleases = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val exploreNewReleases: StateFlow<List<YouTubeSearchResult>> = _exploreNewReleases.asStateFlow()

    private val _exploreLoading = MutableStateFlow(false)
    val exploreLoading: StateFlow<Boolean> = _exploreLoading.asStateFlow()

    init {
        try {
            _downloadedTracks.value = com.musicdrop.app.data.repository.DownloadedTracksStore.getAll(application)
        } catch (_: Throwable) {}
        try {
            _likedMusic.value = com.musicdrop.app.data.repository.LikedMusicStore.getAll(application)
        } catch (_: Throwable) {}
        try {
            _userPlaylists.value = com.musicdrop.app.data.repository.UserPlaylistsStore.getAll(application)
        } catch (_: Throwable) {}
        try {
            _savedArtists.value = com.musicdrop.app.data.repository.SavedMediaStore.getSavedArtists(application)
        } catch (_: Throwable) {}
        try {
            _savedAlbums.value = com.musicdrop.app.data.repository.SavedMediaStore.getSavedAlbums(application)
        } catch (_: Throwable) {}
        try {
            loadRecentUnified()
        } catch (_: Throwable) {}

        playbackConnection.onPlaybackEnded = {
            try {
                playNextTrackFromQueue()
            } catch (_: Throwable) {}
        }
        playbackConnection.onNeedsFreshStream = {
            try {
                currentPlaybackRetry?.invoke()
            } catch (_: Throwable) {}
        }
        try {
            checkForAppUpdate()
        } catch (_: Throwable) {}
    }

    fun getSafeMusicDir(): java.io.File {
        val app = getApplication<Application>()
        val publicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
        if (publicDir != null) {
            try {
                if (!publicDir.exists()) publicDir.mkdirs()
                if (publicDir.canWrite()) return publicDir
            } catch (_: Exception) {}
        }
        val appExtDir = app.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
        if (appExtDir != null) {
            try {
                if (!appExtDir.exists()) appExtDir.mkdirs()
                if (appExtDir.canWrite()) return appExtDir
            } catch (_: Exception) {}
        }
        val internalDir = java.io.File(app.filesDir, "Music")
        if (!internalDir.exists()) internalDir.mkdirs()
        return internalDir
    }

    /**
     * Primary public Music directory: /storage/emulated/0/Music/MusicDrop/
     * Saved files here are indexed by Android's MediaScanner and show up immediately
     * in the system "Recent" files tab, file managers, and music apps.
     */
    fun getPublicMusicDir(): java.io.File {
        val publicMusic = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
        val musicDropDir = java.io.File(publicMusic, "MusicDrop")
        try {
            if (!musicDropDir.exists()) musicDropDir.mkdirs()
            if (musicDropDir.canWrite()) return musicDropDir
        } catch (_: Exception) {}

        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val dlDropDir = java.io.File(downloadsDir, "MusicDrop")
        try {
            if (!dlDropDir.exists()) dlDropDir.mkdirs()
            if (dlDropDir.canWrite()) return dlDropDir
        } catch (_: Exception) {}

        return getSafeMusicDir()
    }

    /**
     * Primary public Movies directory: /storage/emulated/0/Movies/MusicDrop/
     * Saved MP4 videos here are indexed by Android's MediaScanner and show up immediately
     * in the system "Recent" files tab, file managers, and video gallery apps.
     */
    fun getPublicMoviesDir(): java.io.File {
        val publicMovies = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
        val moviesDropDir = java.io.File(publicMovies, "MusicDrop")
        try {
            if (!moviesDropDir.exists()) moviesDropDir.mkdirs()
            if (moviesDropDir.canWrite()) return moviesDropDir
        } catch (_: Exception) {}

        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val dlDropDir = java.io.File(downloadsDir, "MusicDrop")
        try {
            if (!dlDropDir.exists()) dlDropDir.mkdirs()
            if (dlDropDir.canWrite()) return dlDropDir
        } catch (_: Exception) {}

        return getSafeMusicDir()
    }

    /**
     * Share downloaded audio / video file directly via Android's native share sheet (FileProvider).
     * Allows one-tap sharing of the MP3, M4A, or MP4 video to WhatsApp, Telegram, Gmail, Bluetooth, etc.
     */
    fun shareDownloadedFile(context: android.content.Context, track: com.musicdrop.app.data.repository.DownloadedTrack) {
        val file = java.io.File(track.filePath)
        if (!file.exists() || file.length() == 0L) {
            android.widget.Toast.makeText(context, "Downloaded file not found on storage", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val isVideo = track.mimeType.startsWith("video") || file.name.endsWith(".mp4")
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = if (isVideo) "video/mp4" else if (track.mimeType.isNotBlank()) track.mimeType else "audio/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, track.title)
                putExtra(android.content.Intent.EXTRA_TEXT, "Shared from MusicDrop: ${track.title}")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = android.content.Intent.createChooser(intent, "Share ${track.title}")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Could not share file: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share any media file directly with specified file path and MIME type.
     */
    fun shareMediaFile(context: android.content.Context, filePath: String, mimeType: String, title: String) {
        val file = java.io.File(filePath)
        if (!file.exists() || file.length() == 0L) {
            android.widget.Toast.makeText(context, "File does not exist", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val isVideo = mimeType.startsWith("video") || file.name.endsWith(".mp4")
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = if (isVideo) "video/mp4" else if (mimeType.isNotBlank()) mimeType else "audio/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, title)
                putExtra(android.content.Intent.EXTRA_TEXT, "Shared from MusicDrop: $title")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = android.content.Intent.createChooser(intent, "Share $title")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Could not share file: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share the currently playing track. If downloaded locally, shares the actual MP3/MP4 file.
     * If streaming online, shares track details with a link.
     */
    fun shareCurrentTrack(context: android.content.Context) {
        val current = playbackConnection.currentTrack.value ?: return
        val currentIdStr = current.id.toString()
        val downloaded = _downloadedTracks.value.firstOrNull {
            it.title.equals(current.name, ignoreCase = true) || it.key.contains(currentIdStr)
        }
        if (downloaded != null) {
            shareDownloadedFile(context, downloaded)
        } else {
            val ytId = current.filePath?.takeIf { it.length == 11 && !it.contains("/") } ?: (if (current.id != 0L) currentIdStr else "")
            val link = if (ytId.isNotBlank()) "https://youtu.be/$ytId" else ""
            val shareText = "🎵 ${current.name} - ${current.artist}\n$link\n\nShared via MusicDrop"
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, current.name)
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            }
            val chooser = android.content.Intent.createChooser(intent, "Share ${current.name}")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    // ---- YouTube Music (official Data API v3 search + IFrame Player playback) ----
    private val _ytSearchQuery = MutableStateFlow("")
    val ytSearchQuery: StateFlow<String> = _ytSearchQuery.asStateFlow()
    private val _ytSearchResults = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val ytSearchResults: StateFlow<List<YouTubeSearchResult>> = _ytSearchResults.asStateFlow()
    private val _detailedSearchResult = MutableStateFlow<YouTubeSearchRepository.DetailedSearchResult?>(null)
    val detailedSearchResult: StateFlow<YouTubeSearchRepository.DetailedSearchResult?> = _detailedSearchResult.asStateFlow()
    private val _ytSearchLoading = MutableStateFlow(false)
    val ytSearchLoading: StateFlow<Boolean> = _ytSearchLoading.asStateFlow()
    private val _ytSearchError = MutableStateFlow<String?>(null)
    val ytSearchError: StateFlow<String?> = _ytSearchError.asStateFlow()
    private val _ytFavorites = MutableStateFlow<List<YouTubeSearchResult>>(MusicFavoritesStore.getAll(application))
    val ytFavorites: StateFlow<List<YouTubeSearchResult>> = _ytFavorites.asStateFlow()
    private val _ytCurrentVideo = MutableStateFlow<YouTubeSearchResult?>(null)
    val ytCurrentVideo: StateFlow<YouTubeSearchResult?> = _ytCurrentVideo.asStateFlow()

    // ── Video Mode: watch the current YouTube song as a real video instead of just
    // listening to it. Audio is always the default — this is an explicit per-song
    // toggle, not a global setting. See toggleVideoMode().
    private val _isVideoMode = MutableStateFlow(false)
    val isVideoMode: StateFlow<Boolean> = _isVideoMode.asStateFlow()
    private val _userWantsVideoMode = MutableStateFlow(false)
    val userWantsVideoMode: StateFlow<Boolean> = _userWantsVideoMode.asStateFlow()
    private val _videoModeLoading = MutableStateFlow(false)
    val videoModeLoading: StateFlow<Boolean> = _videoModeLoading.asStateFlow()
    private val _videoModeWebFallback = MutableStateFlow(false)
    val videoModeWebFallback: StateFlow<Boolean> = _videoModeWebFallback.asStateFlow()

    private val _ytIsPlaying = MutableStateFlow(false)
    val ytIsPlaying: StateFlow<Boolean> = _ytIsPlaying.asStateFlow()
    private val _ytExpanded = MutableStateFlow(false)
    val ytExpanded: StateFlow<Boolean> = _ytExpanded.asStateFlow()
    private var ytSearchJob: Job? = null

    // ── Picture-in-Picture / Floating Screen state ──
    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()
    fun setIsInPipMode(inPip: Boolean) {
        _isInPipMode.value = inPip
    }

    private val _ytExtractionResult = MutableStateFlow<YouTubeExtractionResult?>(null)
    val ytExtractionResult: StateFlow<YouTubeExtractionResult?> = _ytExtractionResult.asStateFlow()

    private val _showYtBottomSheet = MutableStateFlow(false)
    val showYtBottomSheet: StateFlow<Boolean> = _showYtBottomSheet.asStateFlow()

    fun setYtExtractionResult(result: YouTubeExtractionResult) {
        _ytExtractionResult.value = result
    }

    fun openYtBottomSheet() {
        _showYtBottomSheet.value = true
    }

    fun closeYtBottomSheet() {
        _showYtBottomSheet.value = false
    }

    private val _showFullPlayer = MutableStateFlow(false)
    val showFullPlayer: StateFlow<Boolean> = _showFullPlayer.asStateFlow()

    fun openFullPlayer() {
        _showFullPlayer.value = true
    }

    fun closeFullPlayer() {
        _showFullPlayer.value = false
    }

    // PoToken — generated by the hidden BotGuard WebView in po_token.html
    private val _ytPoToken = MutableStateFlow<PoTokenManager.PoToken?>(null)
    val ytPoToken: StateFlow<PoTokenManager.PoToken?> = _ytPoToken.asStateFlow()
    private val poTokenManager = PoTokenManager(application)

    fun refreshPoToken() {
        viewModelScope.launch {
            _ytPoToken.value = poTokenManager.getPoToken()
        }
    }

    fun playYouTubeDirectStream(result: YouTubeSearchResult, streamUrl: String) {
        val mediaItem = MediaItem(
            id = result.videoId.hashCode().toLong(),
            uri = Uri.parse(streamUrl),
            name = result.title,
            size = 0L,
            dateAdded = System.currentTimeMillis() / 1000,
            mimeType = "audio/mp4",
            mediaType = MediaType.AUDIO,
            durationMs = 0L,
            albumArtUri = Uri.parse(result.thumbnailUrl)
        )
        playbackConnection.playTrack(mediaItem, listOf(mediaItem))

        // Also download to public Music directory
        try {
            val chromeUserAgent =
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            val cleanTitle = result.title.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().take(80).ifBlank { "Track_${System.currentTimeMillis()}" }
            val request = android.app.DownloadManager.Request(Uri.parse(streamUrl))
                .setTitle(result.title)
                .setDescription("Music Drop Download")
                .addRequestHeader("User-Agent", chromeUserAgent)
                .addRequestHeader("Origin", "https://m.youtube.com")
                .addRequestHeader("Referer", "https://m.youtube.com/")
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_MUSIC, "$cleanTitle.mp3")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            val dm = getApplication<Application>().getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            dm.enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Seeded from disk on creation so a track already played (even in a previous app
    // session — Android kills this process constantly) plays instantly instead of
    // paying the full extraction cost again. Every write below goes through
    // [cacheStream], which keeps this in sync with the persisted copy.
    private val streamCache = java.util.concurrent.ConcurrentHashMap<String, com.musicdrop.app.data.youtube.PWebExtractor.StreamInfo>().apply {
        putAll(com.musicdrop.app.data.youtube.StreamCacheStore.load(application))
    }

    /** Write-through: updates the in-memory cache and persists the snapshot to disk. */
    private fun cacheStream(info: com.musicdrop.app.data.youtube.PWebExtractor.StreamInfo) {
        streamCache[info.videoId] = info
        com.musicdrop.app.data.youtube.StreamCacheStore.save(getApplication(), streamCache)
    }

    /**
     * "Play this exact track again" for whichever source is currently playing — set at
     * the top of every playXxx() function. This is what fixes "plays sometimes, not
     * others": when ExoPlayer reports a dead stream (see [PlaybackConnection.onNeedsFreshStream]),
     * we re-run this instead of blindly retrying the same now-invalid URL.
     */
    private var currentPlaybackRetry: (() -> Unit)? = null

    /** The last track played through any playXxx() entry point — feeds autoplay. */
    private var lastPlayedUnified: UnifiedTrack? = null

    /** Keys played via autoplay recently, so it doesn't loop the same couple of tracks. */
    private val autoplayHistory = ArrayDeque<String>()

    /**
     * The [UnifiedTrack.key] of whatever card is currently being "made ready" to
     * play — set the instant a tap happens, cleared once the real stream is resolved
     * (or fails). Lets any grid show a spinner on the exact card that was tapped
     * instead of leaving the user wondering if the tap registered.
     */
    private val _preparingKey = MutableStateFlow<String?>(null)
    val preparingKey: StateFlow<String?> = _preparingKey.asStateFlow()

    private val downloadHttpClient = okhttp3.OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(12, 5, java.util.concurrent.TimeUnit.MINUTES))
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private suspend fun streamDownloadToFile(
        url: String,
        outFile: java.io.File,
        onProgress: ((Float, Long, Long) -> Unit)? = null
    ): Boolean =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val parent = outFile.parentFile
                if (parent != null && !parent.exists()) parent.mkdirs()

                val tempFile = java.io.File(outFile.parentFile, "${outFile.name}.tmp")
                if (tempFile.exists()) tempFile.delete()

                val isYouTube = url.contains("googlevideo.com") || url.contains("youtube.com")

                // Step 1: Probe Content-Length to determine if parallel chunking can be used
                var contentLength = -1L
                var canParallel = false
                try {
                    val probeReq = okhttp3.Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept-Encoding", "identity")
                        .apply {
                            if (isYouTube) {
                                header("Origin", "https://m.youtube.com")
                                header("Referer", "https://m.youtube.com/")
                            }
                        }
                        .build()
                    downloadHttpClient.newCall(probeReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            contentLength = resp.body?.contentLength() ?: -1L
                            val acceptRanges = resp.header("Accept-Ranges")
                            canParallel = contentLength > 2 * 1024 * 1024L && (acceptRanges?.contains("bytes", ignoreCase = true) == true || isYouTube)
                        }
                    }
                } catch (_: Exception) {}

                // Step 2: 4-Worker Parallel Range Chunk Downloader (bypasses YouTube 1x throttle)
                if (canParallel && contentLength > 0) {
                    try {
                        val numChunks = 4
                        val chunkSize = (contentLength + numChunks - 1) / numChunks

                        val rafInit = java.io.RandomAccessFile(tempFile, "rw")
                        rafInit.setLength(contentLength)
                        rafInit.close()

                        val totalBytesRead = java.util.concurrent.atomic.AtomicLong(0L)
                        var lastReportTime = 0L

                        val results = kotlinx.coroutines.coroutineScope {
                            (0 until numChunks).map { index ->
                                async(Dispatchers.IO) {
                                    val startByte = index * chunkSize
                                    val endByte = minOf((index + 1) * chunkSize - 1, contentLength - 1)
                                    if (startByte > endByte) return@async true

                                    val rangeReq = okhttp3.Request.Builder()
                                        .url(url)
                                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                        .header("Range", "bytes=$startByte-$endByte")
                                        .header("Accept-Encoding", "identity")
                                        .apply {
                                            if (isYouTube) {
                                                header("Origin", "https://m.youtube.com")
                                                header("Referer", "https://m.youtube.com/")
                                            }
                                        }
                                        .build()

                                    downloadHttpClient.newCall(rangeReq).execute().use { response ->
                                        if (!response.isSuccessful && response.code != 206) {
                                            return@async false
                                        }
                                        val stream = response.body?.byteStream() ?: return@async false
                                        java.io.RandomAccessFile(tempFile, "rw").use { raf ->
                                            raf.seek(startByte)
                                            val buf = ByteArray(131072) // 128KB buffer
                                            var readBytes: Int
                                            var bytesForThisChunk = 0L
                                            val maxBytes = endByte - startByte + 1
                                            while (stream.read(buf).also { readBytes = it } != -1) {
                                                val toWrite = minOf(readBytes.toLong(), maxBytes - bytesForThisChunk).toInt()
                                                if (toWrite > 0) {
                                                    raf.write(buf, 0, toWrite)
                                                    bytesForThisChunk += toWrite
                                                    val total = totalBytesRead.addAndGet(toWrite.toLong())
                                                    val now = System.currentTimeMillis()
                                                    if (now - lastReportTime > 150 || total >= contentLength) {
                                                        lastReportTime = now
                                                        val prog = (total.toFloat() / contentLength).coerceIn(0f, 1f)
                                                        onProgress?.invoke(prog, total, contentLength)
                                                    }
                                                }
                                                if (bytesForThisChunk >= maxBytes) break
                                            }
                                        }
                                        true
                                    }
                                }
                            }.awaitAll()
                        }
                        if (results.all { it } && tempFile.exists() && tempFile.length() >= contentLength) {
                            if (outFile.exists()) outFile.delete()
                            val renamed = tempFile.renameTo(outFile)
                            if (!renamed) {
                                tempFile.copyTo(outFile, overwrite = true)
                                tempFile.delete()
                            }
                            return@withContext true
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("MainViewModel", "Parallel chunk download fallback: ${e.message}")
                        if (tempFile.exists()) tempFile.delete()
                    }
                }

                // Step 3: Sequential fallback stream
                val reqBuilder = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Encoding", "identity")

                if (isYouTube) {
                    reqBuilder
                        .header("Accept", "*/*")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Origin", "https://m.youtube.com")
                        .header("Referer", "https://m.youtube.com/")
                        .header("Sec-Fetch-Dest", "audio")
                        .header("Sec-Fetch-Mode", "cors")
                        .header("Sec-Fetch-Site", "cross-site")
                }
                val request = reqBuilder.build()

                downloadHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        android.util.Log.e("MainViewModel", "Download HTTP error ${response.code} for $url")
                        return@withContext false
                    }
                    val body = response.body ?: return@withContext false
                    val singleLen = body.contentLength()

                    val bufferSize = 262144
                    java.io.BufferedInputStream(body.byteStream(), bufferSize).use { input ->
                        java.io.BufferedOutputStream(tempFile.outputStream(), bufferSize).use { output ->
                            val buffer = ByteArray(bufferSize)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            var lastReportTime = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                val now = System.currentTimeMillis()
                                if (singleLen > 0 && (now - lastReportTime > 150 || totalBytesRead == singleLen)) {
                                    lastReportTime = now
                                    val progress = (totalBytesRead.toFloat() / singleLen).coerceIn(0f, 1f)
                                    onProgress?.invoke(progress, totalBytesRead, singleLen)
                                }
                            }
                            output.flush()
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 0) {
                        if (outFile.exists()) outFile.delete()
                        val renamed = tempFile.renameTo(outFile)
                        if (!renamed) {
                            tempFile.copyTo(outFile, overwrite = true)
                            tempFile.delete()
                        }
                        return@withContext true
                    }
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "streamDownloadToFile failed: ${e.message}", e)
                false
            }
        }

    /** Download YouTube audio to the Music folder using reliable streaming. */
    fun downloadYouTubeAudio(result: YouTubeSearchResult, onDone: (success: Boolean, path: String) -> Unit) {
        viewModelScope.launch {
            try {
                // Stage 1: check stream cache or extract fresh stream URL
                val cachedForDownload = streamCache[result.videoId]?.takeIf { it.isFresh() }
                var streamUrl: String? = cachedForDownload?.url
                var mimeType = cachedForDownload?.mimeType ?: "audio/mp4"

                if (streamUrl == null) {
                    // Fast parallel race between extractors instead of sequential 40s wait!
                    val fastApiDeferred = async(Dispatchers.IO) {
                        try {
                            val info = YouTubeStreamExtractor.getInstance(getApplication()).extract(
                                result.videoId,
                                knownTitle  = result.title,
                                knownAuthor = result.channelTitle,
                                knownThumb  = result.thumbnailUrl
                            )
                            if (info != null && info.url.isNotBlank()) {
                                com.musicdrop.app.data.youtube.PWebExtractor.StreamInfo(
                                    url = info.url,
                                    mimeType = info.mimeType,
                                    title = info.title,
                                    author = info.author,
                                    thumbnailUrl = info.thumbnailUrl,
                                    durationMs = info.durationMs,
                                    videoId = info.videoId,
                                    expiresAtMs = com.musicdrop.app.data.youtube.StreamCacheStore.expiryFromUrl(info.url)
                                )
                            } else null
                        } catch (_: Exception) { null }
                    }

                    val newPipeDeferred = async(Dispatchers.IO) {
                        try {
                            val info = NewPipeYouTubeExtractor.getInstance(getApplication()).extract(result.videoId)
                            if (info != null) {
                                com.musicdrop.app.data.youtube.PWebExtractor.StreamInfo(
                                    url = info.url,
                                    mimeType = info.mimeType,
                                    title = result.title,
                                    author = result.channelTitle,
                                    thumbnailUrl = result.thumbnailUrl,
                                    durationMs = info.durationSec * 1000L,
                                    videoId = result.videoId,
                                    expiresAtMs = com.musicdrop.app.data.youtube.StreamCacheStore.expiryFromUrl(info.url)
                                )
                            } else null
                        } catch (_: Exception) { null }
                    }

                    val pWebDeferred = async(Dispatchers.IO) {
                        try {
                            com.musicdrop.app.data.youtube.PWebExtractor
                                .getInstance(getApplication())
                                .extract(result.videoId, result.title, result.channelTitle, result.thumbnailUrl)
                        } catch (_: Exception) { null }
                    }

                    val streamResult = kotlinx.coroutines.selects.select<com.musicdrop.app.data.youtube.PWebExtractor.StreamInfo?> {
                        fastApiDeferred.onAwait { it }
                        newPipeDeferred.onAwait { it }
                        pWebDeferred.onAwait { it }
                    } ?: fastApiDeferred.await() ?: newPipeDeferred.await() ?: pWebDeferred.await()

                    if (streamResult != null) {
                        streamUrl = streamResult.url
                        mimeType = streamResult.mimeType
                        cacheStream(streamResult)
                    }
                }

                if (streamUrl == null) {
                    onDone(false, "")
                    return@launch
                }

                val trackKey = "yt:${result.videoId}"
                _downloadStatusMap.value = _downloadStatusMap.value + (trackKey to "Starting download...")

                val cleanTitle = result.title.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().take(80).ifBlank { "YouTube_Audio" }
                val ext = if (mimeType.contains("webm") || mimeType.contains("opus")) "opus" else "m4a"
                val fileName = "$cleanTitle.$ext"
                val safeDir = getPublicMusicDir()
                var outFile = java.io.File(safeDir, fileName)

                var ok = streamDownloadToFile(streamUrl, outFile) { progress, current, total ->
                    _downloadProgressMap.value = _downloadProgressMap.value + (trackKey to progress)
                    val mbCurrent = current / (1024f * 1024f)
                    val mbTotal = total / (1024f * 1024f)
                    _downloadStatusMap.value = _downloadStatusMap.value + (trackKey to String.format("%.1f / %.1f MB (%.0f%%)", mbCurrent, mbTotal, progress * 100))
                }

                // If failed (e.g. expired URL), refresh extract and retry
                if (!ok) {
                    streamCache.remove(result.videoId)
                    val freshInfo = try {
                        YouTubeStreamExtractor.getInstance(getApplication()).extract(
                            result.videoId,
                            knownTitle  = result.title,
                            knownAuthor = result.channelTitle,
                            knownThumb  = result.thumbnailUrl
                        )
                    } catch (_: Exception) { null }

                    if (freshInfo != null && freshInfo.url.isNotBlank()) {
                        val streamInfo = com.musicdrop.app.data.youtube.PWebExtractor.StreamInfo(
                            url = freshInfo.url,
                            mimeType = freshInfo.mimeType,
                            title = freshInfo.title,
                            author = freshInfo.author,
                            thumbnailUrl = freshInfo.thumbnailUrl,
                            durationMs = freshInfo.durationMs,
                            videoId = freshInfo.videoId,
                            expiresAtMs = com.musicdrop.app.data.youtube.StreamCacheStore.expiryFromUrl(freshInfo.url)
                        )
                        cacheStream(streamInfo)
                        ok = streamDownloadToFile(freshInfo.url, outFile)
                    }
                }

                // Fallback to internal files dir if scoped storage blocked public directory
                if (!ok) {
                    val fallbackDir = java.io.File(getApplication<Application>().filesDir, "Music").apply { if (!exists()) mkdirs() }
                    outFile = java.io.File(fallbackDir, fileName)
                    if (streamUrl != null) {
                        ok = streamDownloadToFile(streamUrl, outFile)
                    }
                }

                if (!ok || !outFile.exists() || outFile.length() == 0L) {
                    onDone(false, "")
                    return@launch
                }

                val downloadedTrack = com.musicdrop.app.data.repository.DownloadedTrack(
                    key = "yt:${result.videoId}",
                    title = result.title,
                    artist = result.channelTitle,
                    duration = result.duration,
                    coverUrl = result.thumbnailUrl,
                    filePath = outFile.absolutePath,
                    mimeType = mimeType,
                    downloadedAtMs = System.currentTimeMillis()
                )
                com.musicdrop.app.data.repository.DownloadedTracksStore.add(getApplication(), downloadedTrack)
                _downloadedTracks.value = com.musicdrop.app.data.repository.DownloadedTracksStore.getAll(getApplication())

                // Notify MediaStore so it appears in Music apps
                android.media.MediaScannerConnection.scanFile(
                    getApplication(), arrayOf(outFile.absolutePath), null, null
                )
                _downloadProgressMap.value = _downloadProgressMap.value - "yt:${result.videoId}"
                _downloadStatusMap.value = _downloadStatusMap.value - "yt:${result.videoId}"
                onDone(true, outFile.absolutePath)
            } catch (e: Exception) {
                _downloadProgressMap.value = _downloadProgressMap.value - "yt:${result.videoId}"
                _downloadStatusMap.value = _downloadStatusMap.value - "yt:${result.videoId}"
                android.util.Log.e("MainViewModel", "Download failed: ${e.message}", e)
                onDone(false, "")
            }
        }
    }

    /** Download YouTube video (MP4) for offline video playback. */
    fun downloadYouTubeVideo(result: YouTubeSearchResult, onDone: (success: Boolean, path: String) -> Unit) {
        viewModelScope.launch {
            val trackKey = "yt:${result.videoId}"
            try {
                _downloadStatusMap.value = _downloadStatusMap.value + (trackKey to "Resolving video stream...")

                // Triple-redundant video stream extraction
                val newPipeDeferred = async(Dispatchers.IO) {
                    try {
                        NewPipeYouTubeExtractor.getInstance(getApplication()).extractVideo(result.videoId)?.url
                    } catch (_: Exception) { null }
                }
                val pWebDeferred = async(Dispatchers.IO) {
                    try {
                        com.musicdrop.app.data.youtube.PWebExtractor.getInstance(getApplication())
                            .extractVideo(result.videoId, result.title, result.channelTitle, result.thumbnailUrl)?.url
                    } catch (_: Exception) { null }
                }
                val yteDeferred = async(Dispatchers.IO) {
                    try {
                        YouTubeStreamExtractor.getInstance(getApplication())
                            .extractVideo(result.videoId, result.title, result.channelTitle, result.thumbnailUrl)?.url
                    } catch (_: Exception) { null }
                }

                val videoUrl = newPipeDeferred.await()?.takeIf { it.isNotBlank() }
                    ?: pWebDeferred.await()?.takeIf { it.isNotBlank() }
                    ?: yteDeferred.await()?.takeIf { it.isNotBlank() }

                if (videoUrl == null) {
                    _downloadStatusMap.value = _downloadStatusMap.value - trackKey
                    android.widget.Toast.makeText(getApplication(), "Could not extract video stream for download", android.widget.Toast.LENGTH_SHORT).show()
                    onDone(false, "")
                    return@launch
                }

                _downloadStatusMap.value = _downloadStatusMap.value + (trackKey to "Starting video download...")

                val cleanTitle = result.title.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().take(80).ifBlank { "YouTube_Video" }
                val fileName = "$cleanTitle.mp4"
                val safeDir = getPublicMoviesDir()
                var outFile = java.io.File(safeDir, fileName)

                var ok = streamDownloadToFile(videoUrl, outFile) { progress, current, total ->
                    _downloadProgressMap.value = _downloadProgressMap.value + (trackKey to progress)
                    val mbCurrent = current / (1024f * 1024f)
                    val mbTotal = total / (1024f * 1024f)
                    _downloadStatusMap.value = _downloadStatusMap.value + (trackKey to String.format("%.1f / %.1f MB (%.0f%%)", mbCurrent, mbTotal, progress * 100))
                }

                if (!ok) {
                    // Fallback to internal Movies dir if public directory blocked
                    val fallbackDir = java.io.File(getApplication<Application>().filesDir, "Movies").apply { if (!exists()) mkdirs() }
                    outFile = java.io.File(fallbackDir, fileName)
                    ok = streamDownloadToFile(videoUrl, outFile)
                }

                if (!ok || !outFile.exists() || outFile.length() == 0L) {
                    _downloadProgressMap.value = _downloadProgressMap.value - trackKey
                    _downloadStatusMap.value = _downloadStatusMap.value - trackKey
                    onDone(false, "")
                    return@launch
                }

                val downloadedTrack = com.musicdrop.app.data.repository.DownloadedTrack(
                    key = "yt_video:${result.videoId}",
                    title = "🎬 ${result.title}",
                    artist = result.channelTitle,
                    duration = result.duration,
                    coverUrl = result.thumbnailUrl,
                    filePath = outFile.absolutePath,
                    mimeType = "video/mp4",
                    downloadedAtMs = System.currentTimeMillis()
                )
                com.musicdrop.app.data.repository.DownloadedTracksStore.add(getApplication(), downloadedTrack)
                _downloadedTracks.value = com.musicdrop.app.data.repository.DownloadedTracksStore.getAll(getApplication())

                android.media.MediaScannerConnection.scanFile(
                    getApplication(), arrayOf(outFile.absolutePath), arrayOf("video/mp4"), null
                )
                _downloadProgressMap.value = _downloadProgressMap.value - trackKey
                _downloadStatusMap.value = _downloadStatusMap.value - trackKey
                onDone(true, outFile.absolutePath)
            } catch (e: Exception) {
                _downloadProgressMap.value = _downloadProgressMap.value - trackKey
                _downloadStatusMap.value = _downloadStatusMap.value - trackKey
                android.util.Log.e("MainViewModel", "Video download failed: ${e.message}", e)
                onDone(false, "")
            }
        }
    }


    // ---- YouTube Music gallery (music.youtube.com scraper, s_web style) ----
    private val _ytMusicResults = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val ytMusicResults: StateFlow<List<YouTubeSearchResult>> = _ytMusicResults.asStateFlow()
    private val _ytMusicLoading = MutableStateFlow(false)
    val ytMusicLoading: StateFlow<Boolean> = _ytMusicLoading.asStateFlow()
    private val _ytMusicError = MutableStateFlow<String?>(null)
    val ytMusicError: StateFlow<String?> = _ytMusicError.asStateFlow()
    private val _ytMusicCtoken = MutableStateFlow<String?>(null)
    val ytMusicCtoken: StateFlow<String?> = _ytMusicCtoken.asStateFlow()
    private val _ytMusicLoadingMore = MutableStateFlow(false)
    val ytMusicLoadingMore: StateFlow<Boolean> = _ytMusicLoadingMore.asStateFlow()
    private var ytMusicSearchJob: Job? = null
    private var ytMusicCurrentQuery = ""

    private val _selectedSearchFilter = MutableStateFlow(YouTubeMusicRepository.SearchFilter.ALL)
    val selectedSearchFilter: StateFlow<YouTubeMusicRepository.SearchFilter> = _selectedSearchFilter.asStateFlow()

    private val _upNextQueue = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val upNextQueue: StateFlow<List<YouTubeSearchResult>> = _upNextQueue.asStateFlow()

    fun setSearchFilter(filter: YouTubeMusicRepository.SearchFilter) {
        _selectedSearchFilter.value = filter
        if (_ytSearchQuery.value.isNotBlank()) {
            setYtSearchQuery(_ytSearchQuery.value)
        } else if (ytMusicCurrentQuery.isNotBlank()) {
            searchYtMusic(ytMusicCurrentQuery)
        }
    }

    fun searchYtMusic(query: String) {
        ytMusicSearchJob?.cancel()
        ytMusicCurrentQuery = query
        _ytMusicCtoken.value = null
        ytMusicSearchJob = viewModelScope.launch {
            delay(400)
            _ytMusicLoading.value = true
            _ytMusicError.value = null
            _ytMusicResults.value = emptyList()
            val page = YouTubeMusicRepository.search(query, filter = _selectedSearchFilter.value)
            _ytMusicResults.value = page.results
            _ytMusicCtoken.value = page.nextCtoken
            if (page.results.isEmpty()) {
                _ytMusicError.value = "No results found. Try a different search."
            }
            _ytMusicLoading.value = false
        }
    }

    fun loadYtMusicNextPage() {
        val ctoken = _ytMusicCtoken.value ?: return
        if (_ytMusicLoadingMore.value) return
        viewModelScope.launch {
            _ytMusicLoadingMore.value = true
            val page = YouTubeMusicRepository.search(ytMusicCurrentQuery, ctoken, filter = _selectedSearchFilter.value)
            _ytMusicResults.value = _ytMusicResults.value + page.results
            _ytMusicCtoken.value = page.nextCtoken
            _ytMusicLoadingMore.value = false
        }
    }

    private val _selectedCountry = MutableStateFlow("IN")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    private val _chartsArtists = MutableStateFlow<List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist>>(emptyList())
    val chartsArtists: StateFlow<List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist>> = _chartsArtists.asStateFlow()

    private val _chartsDaily = MutableStateFlow<List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartPlaylist>>(emptyList())
    val chartsDaily: StateFlow<List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartPlaylist>> = _chartsDaily.asStateFlow()

    private val _chartsWeekly = MutableStateFlow<List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartPlaylist>>(emptyList())
    val chartsWeekly: StateFlow<List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartPlaylist>> = _chartsWeekly.asStateFlow()

    private val _chartsGenres = MutableStateFlow<List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartPlaylist>>(emptyList())
    val chartsGenres: StateFlow<List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartPlaylist>> = _chartsGenres.asStateFlow()

    // ---- Music Taste Learning & Recommendations ----
    private val _madeForYouRecommendations = MutableStateFlow<List<UnifiedTrack>>(emptyList())
    val madeForYouRecommendations: StateFlow<List<UnifiedTrack>> = _madeForYouRecommendations.asStateFlow()

    private val _madeForYouTitle = MutableStateFlow("Made For You")
    val madeForYouTitle: StateFlow<String> = _madeForYouTitle.asStateFlow()

    private val _isRefreshingDashboard = MutableStateFlow(false)
    val isRefreshingDashboard: StateFlow<Boolean> = _isRefreshingDashboard.asStateFlow()

    fun loadTasteRecommendations() {
        val handler = CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e("MainViewModel", "Error loading taste recommendations", throwable)
        }
        viewModelScope.launch(Dispatchers.IO + handler) {
            try {
                val profile = com.musicdrop.app.data.repository.TasteLearningRepository.getPersonalizedRecommendations(getApplication())
                if (profile.tracks.isNotEmpty()) {
                    _madeForYouTitle.value = profile.recommendationsTitle
                    _madeForYouRecommendations.value = profile.tracks
                }
            } catch (e: Throwable) {
                android.util.Log.e("MainViewModel", "Failed to load taste recommendations", e)
            }
        }
    }

    private val _musicFeed = MutableStateFlow<List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtMusicFeedCategory>>(emptyList())
    val musicFeed: StateFlow<List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtMusicFeedCategory>> = _musicFeed.asStateFlow()

    private val _southIndiaTrending = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val southIndiaTrending: StateFlow<List<YouTubeSearchResult>> = _southIndiaTrending.asStateFlow()

    // ── Refresh variety ─────────────────────────────────────────────────────
    // Hitting "refresh" on a trending shelf used to just re-fetch the exact
    // same real chart, so users saw the same songs every time (the chart
    // itself only moves daily/weekly). This blends in a rotating, genuinely
    // different search each time refresh is tapped (force = true) so the
    // shelf visibly changes, while keeping the top of the real chart pinned
    // first so it still reads as an accurate trending list. Official ranked
    // charts (Top Charts daily/weekly) are left untouched — those show a real
    // ranking and shouldn't be shuffled.
    private val varietyQueryPools: Map<String, List<String>> = mapOf(
        "IN" to listOf(
            "latest bollywood songs 2026",
            "new hindi songs trending this week",
            "top viral hindi songs 2026",
            "punjabi viral hits 2026",
            "india music new releases 2026",
            "trending bollywood superhits 2026"
        ),
        "PK" to listOf(
            "pakistan viral songs 2026",
            "coke studio new songs 2026",
            "pakistani new released tracks",
            "urdu trending music 2026"
        ),
        "US" to listOf(
            "us viral hits 2026",
            "billboard new releases 2026",
            "top 40 radio hits 2026",
            "american pop trending songs"
        ),
        "GB" to listOf(
            "uk viral songs 2026",
            "uk new music releases 2026",
            "official uk trending hits",
            "british pop chart hits 2026"
        ),
        "AE" to listOf(
            "khaleeji trending songs 2026",
            "arabic viral hits 2026",
            "gulf new music releases",
            "arabic pop trending 2026"
        ),
        "SA" to listOf(
            "saudi trending songs 2026",
            "arabic viral hits 2026",
            "khaleeji new releases",
            "gulf pop hits 2026"
        )
    )
    private fun varietyQueriesFor(code: String): List<String> =
        varietyQueryPools[code.uppercase()] ?: listOf("trending music 2026", "viral songs this week", "new music releases", "top hits right now")

    /** Keeps [pinnedCount] items from the real/primary list first (so ranking still
     *  reads as authentic), then shuffles the rest of primary + extra together. */
    private fun <T> shuffleWithPinnedHead(
        primary: List<T>,
        extra: List<T>,
        keyOf: (T) -> String,
        pinnedCount: Int = 6,
        cap: Int = 30
    ): List<T> {
        if (primary.isEmpty() && extra.isEmpty()) return emptyList()
        val pinned = primary.take(pinnedCount)
        val pinnedKeys = pinned.map(keyOf).toSet()
        val pool = (primary.drop(pinnedCount) + extra)
            .distinctBy(keyOf)
            .filterNot { pinnedKeys.contains(keyOf(it)) }
            .shuffled()
        return (pinned + pool).distinctBy(keyOf).take(cap)
    }

    private suspend fun varietySearch(query: String, maxResults: Int = 20): List<YouTubeSearchResult> = try {
        when (val outcome = YouTubeSearchRepository.search(query, maxResults = maxResults)) {
            is YouTubeSearchOutcome.Success -> outcome.results
            is YouTubeSearchOutcome.Error -> emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }

    /** On refresh (force = true) only, blends in one alternate real search so a quick-pick
     *  lane visibly changes instead of re-showing the exact same fixed-query results. */
    private suspend fun withVariety(
        force: Boolean,
        primary: List<YouTubeSearchResult>,
        altQueries: List<String>
    ): List<YouTubeSearchResult> {
        if (!force || primary.isEmpty() || altQueries.isEmpty()) return primary
        val extra = varietySearch(altQueries.random())
        return shuffleWithPinnedHead(primary, extra, keyOf = { it.videoId })
    }

    private suspend fun varietyArtists(
        primary: List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist>,
        country: String
    ): List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist> {
        if (primary.isEmpty()) return primary
        val code = country.uppercase()
        val query = when (code) {
            "IN" -> listOf("arijit singh", "anirudh ravichander", "diljit dosanjh", "shreya ghoshal", "sid sriram", "badshah", "pritam", "yo yo honey singh").random()
            "PK" -> listOf("atif aslam", "ali zafar", "asif aslam", "rahat fateh ali khan", "young stunners", "firishta").random()
            "US" -> listOf("the weeknd", "taylor swift", "drake", "billie eilish", "bruno mars", "post malone").random()
            "GB" -> listOf("ed sheeran", "dua lipa", "coldplay", "adele", "harry styles", "sam smith").random()
            "AE", "SA" -> listOf("amr diab", "nancy ajram", "hussain al jassmi", "saad lamjarred", "elissa", "sherine").random()
            else -> "top artists"
        }
        val extra: List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist> = try {
            when (val outcome = YouTubeSearchRepository.search(query, maxResults = 15)) {
                is YouTubeSearchOutcome.Success -> outcome.results.map {
                    com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist(
                        rank = "",
                        title = it.channelTitle.ifBlank { it.title },
                        browseId = it.videoId,
                        subscribers = "Trending Artist",
                        thumbnailUrl = it.thumbnailUrl
                    )
                }
                is YouTubeSearchOutcome.Error -> emptyList()
            }
        } catch (_: Exception) { emptyList() }
        return shuffleWithPinnedHead(primary, extra, keyOf = { it.browseId }, pinnedCount = 4, cap = 20)
    }

    fun loadSouthIndiaTrending(force: Boolean = false) {
        if (!force && _southIndiaTrending.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = com.musicdrop.app.data.repository.YtMusicApiRepository.search("tamil trending songs")
                var songs = if (res.songs.isNotEmpty()) res.songs else {
                    when (val outcome = YouTubeSearchRepository.search("tamil trending songs 2026", maxResults = 20)) {
                        is YouTubeSearchOutcome.Success -> outcome.results
                        is YouTubeSearchOutcome.Error -> emptyList()
                    }
                }
                if (force && songs.isNotEmpty()) {
                    val extraQuery = listOf(
                        "south indian viral hits 2026",
                        "telugu trending songs 2026",
                        "malayalam viral tracks 2026"
                    ).random()
                    val extra = varietySearch(extraQuery)
                    songs = shuffleWithPinnedHead(songs, extra, keyOf = { it.videoId })
                }
                if (songs.isNotEmpty()) {
                    _southIndiaTrending.value = songs
                }
            } catch (_: Exception) {
                try {
                    when (val outcome = YouTubeSearchRepository.search("south indian viral hits 2026", maxResults = 20)) {
                        is YouTubeSearchOutcome.Success -> _southIndiaTrending.value = outcome.results
                        is YouTubeSearchOutcome.Error -> {}
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private val _regionTrendingSongs = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val regionTrendingSongs: StateFlow<List<YouTubeSearchResult>> = _regionTrendingSongs.asStateFlow()

    private val _regionTrendingLoading = MutableStateFlow(false)
    val regionTrendingLoading: StateFlow<Boolean> = _regionTrendingLoading.asStateFlow()

    private val regionTrendingCache = java.util.concurrent.ConcurrentHashMap<String, List<YouTubeSearchResult>>()

    fun loadRegionTrending(country: String = _selectedCountry.value, force: Boolean = false) {
        val code = country.uppercase()
        if (force) {
            regionTrendingCache.remove(code)
        }
        val cached = regionTrendingCache[code]
        if (!force && cached != null && cached.isNotEmpty()) {
            _regionTrendingSongs.value = cached
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _regionTrendingLoading.value = true
            try {
                val apiSongs = try {
                    com.musicdrop.app.data.repository.YtMusicApiRepository.getTrending(code, force = force)
                } catch (_: Exception) { emptyList() }
                var songs = if (apiSongs.isNotEmpty()) apiSongs else {
                    val q = when (code) {
                        "PK" -> "pakistan trending songs 2026"
                        "AE", "SA" -> "arabic gulf trending music 2026"
                        "US" -> "billboard hot 100 music 2026"
                        "GB" -> "uk top 40 official singles 2026"
                        else -> listOf(
                            "india trending songs 2026",
                            "latest bollywood superhits 2026",
                            "top hindi songs trending",
                            "punjabi and hindi chartbusters 2026",
                            "viral hits india 2026"
                        ).random()
                    }
                    when (val outcome = YouTubeSearchRepository.search(q, maxResults = 25)) {
                        is YouTubeSearchOutcome.Success -> outcome.results
                        is YouTubeSearchOutcome.Error -> emptyList()
                    }
                }
                if (songs.isNotEmpty()) {
                    val varietyPool = varietyQueriesFor(code)
                    if (varietyPool.isNotEmpty()) {
                        val extra = varietySearch(varietyPool.random(), maxResults = 25)
                        if (extra.isNotEmpty()) {
                            // Cycle fresh viral / new release songs directly to the head of the playlist!
                            // Ensures the top 3 cards seen first on app launch are always fresh and changing.
                            val freshPicks = extra.shuffled().take(6)
                            val remaining = (songs + extra).distinctBy { it.videoId }.filterNot { s -> freshPicks.any { it.videoId == s.videoId } }
                            songs = (freshPicks + remaining).take(30)
                        } else {
                            songs = songs.shuffled()
                        }
                    } else {
                        songs = songs.shuffled()
                    }
                }
                if (songs.isNotEmpty()) {
                    regionTrendingCache[code] = songs
                    if (_selectedCountry.value.equals(code, ignoreCase = true)) {
                        _regionTrendingSongs.value = songs
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _regionTrendingLoading.value = false
            }
        }
    }

    fun rotateTrendingMix() {
        val code = _selectedCountry.value.uppercase()
        regionTrendingCache.remove(code)
        loadRegionTrending(country = code, force = true)
    }

    fun setCountry(country: String) {
        val code = country.uppercase()
        _selectedCountry.value = code
        loadRegionTrending(country = code, force = true)
        loadYtMusicTrending(force = true, country = code)
        loadYtCharts(country = code, force = true)
    }

    fun loadYtMusicTrending(force: Boolean = false, country: String = _selectedCountry.value) {
        if (!force && _ytMusicResults.value.isNotEmpty()) return
        ytMusicCurrentQuery = "top hits trending $country"
        viewModelScope.launch(Dispatchers.IO) {
            if (_ytMusicResults.value.isEmpty()) {
                _ytMusicLoading.value = true
            }
            _ytMusicError.value = null
            try {
                var items = com.musicdrop.app.data.repository.YtMusicApiRepository.getTrending(country, force = force)
                if (items.isNotEmpty()) {
                    if (force) {
                        val extra = varietySearch(varietyQueriesFor(country.uppercase()).random())
                        items = shuffleWithPinnedHead(items, extra, keyOf = { it.videoId })
                    }
                    _ytMusicResults.value = items
                } else {
                    val page = YouTubeMusicRepository.getTrending()
                    _ytMusicResults.value = if (page.results.isNotEmpty()) page.results else {
                        when (val outcome = YouTubeSearchRepository.search("top hits trending songs $country", maxResults = 25)) {
                            is YouTubeSearchOutcome.Success -> outcome.results
                            is YouTubeSearchOutcome.Error -> emptyList()
                        }
                    }
                    _ytMusicCtoken.value = page.nextCtoken
                }
            } catch (e: Exception) {
                try {
                    val page = YouTubeMusicRepository.getTrending()
                    _ytMusicResults.value = page.results
                } catch (_: Exception) {}
            }
            _ytMusicLoading.value = false
        }
    }

    fun loadYtCharts(country: String = _selectedCountry.value, force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val charts = com.musicdrop.app.data.repository.YtMusicApiRepository.getCharts(country, force = force)
                if (charts.daily.isNotEmpty() || charts.weekly.isNotEmpty() || charts.artists.isNotEmpty()) {
                    // Daily/weekly stay exactly as the real official chart reports them —
                    // only the artists lane gets refresh variety (it's a "who's hot" list,
                    // not a ranked chart, so rotating it doesn't misrepresent anything).
                    _chartsDaily.value = charts.daily
                    _chartsWeekly.value = if (charts.weekly.isNotEmpty()) charts.weekly else charts.videos
                    _chartsGenres.value = charts.genres
                    _chartsArtists.value = if (force) varietyArtists(charts.artists, country) else charts.artists
                } else {
                    loadYtChartsFallback(country)
                }
            } catch (_: Exception) {
                loadYtChartsFallback(country)
            }
        }
    }

    private suspend fun loadYtChartsFallback(country: String) {
        try {
            val fallbackArtists = when (val outcome = YouTubeSearchRepository.search("top popular music artists $country", maxResults = 15)) {
                is YouTubeSearchOutcome.Success -> outcome.results.map {
                    com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist(
                        title = it.channelTitle.ifBlank { it.title },
                        browseId = it.videoId,
                        thumbnailUrl = it.thumbnailUrl
                    )
                }
                is YouTubeSearchOutcome.Error -> emptyList()
            }
            if (fallbackArtists.isNotEmpty()) {
                _chartsArtists.value = fallbackArtists
            }
        } catch (_: Exception) {}
    }

    fun loadMusicFeed(force: Boolean = false) {
        if (!force && _musicFeed.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val feed = com.musicdrop.app.data.repository.YtMusicApiRepository.getMusicFeed(force = force)
                if (feed.isNotEmpty()) {
                    _musicFeed.value = feed
                } else {
                    loadDirectYouTubeFeed()
                }
            } catch (_: Exception) {
                loadDirectYouTubeFeed()
            }
        }
    }

    private suspend fun loadDirectYouTubeFeed() {
        try {
            val trendingPicks = when (val outcome = YouTubeSearchRepository.search("trending music 2026", maxResults = 10)) {
                is YouTubeSearchOutcome.Success -> outcome.results.map {
                    com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartPlaylist(
                        title = it.title,
                        playlistId = it.videoId,
                        thumbnailUrl = it.thumbnailUrl
                    )
                }
                is YouTubeSearchOutcome.Error -> emptyList()
            }
            val popPicks = when (val outcome = YouTubeSearchRepository.search("top global pop hits", maxResults = 10)) {
                is YouTubeSearchOutcome.Success -> outcome.results.map {
                    com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartPlaylist(
                        title = it.title,
                        playlistId = it.videoId,
                        thumbnailUrl = it.thumbnailUrl
                    )
                }
                is YouTubeSearchOutcome.Error -> emptyList()
            }
            val shelves = mutableListOf<com.musicdrop.app.data.repository.YtMusicApiRepository.YtMusicFeedCategory>()
            if (trendingPicks.isNotEmpty()) {
                shelves.add(com.musicdrop.app.data.repository.YtMusicApiRepository.YtMusicFeedCategory("Trending Hits • Live on YouTube", trendingPicks))
            }
            if (popPicks.isNotEmpty()) {
                shelves.add(com.musicdrop.app.data.repository.YtMusicApiRepository.YtMusicFeedCategory("Global Pop & Chartbusters", popPicks))
            }
            if (shelves.isNotEmpty()) {
                _musicFeed.value = shelves
            }
        } catch (_: Exception) {}
    }

    /**
     * Unified category refresh: connects all dashboard categories (Trending by Region,
     * Taste-learning Made For You, Charts, Music Feed, South India, Quick Picks)
     * directly to YouTube so every category updates fresh every time.
     */
    fun refreshAllDashboardCategories(force: Boolean = true) {
        val handler = CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e("MainViewModel", "Error in refreshAllDashboardCategories", throwable)
        }
        viewModelScope.launch(Dispatchers.IO + handler) {
            try {
                _isRefreshingDashboard.value = true
                if (force) {
                    com.musicdrop.app.data.repository.YtMusicApiRepository.clearCache()
                }
                val country = try { _selectedCountry.value } catch (_: Throwable) { "IN" }
                loadTasteRecommendations()
                loadSaavnTrending(force = force)
                loadRegionTrending(country = country, force = force)
                loadYtMusicTrending(force = force, country = country)
                loadYtCharts(country = country, force = force)
                loadMusicFeed(force = force)
                loadSouthIndiaTrending(force = force)
                loadAllQuickPicks(force = force)
                loadExploreData(force = force)
                loadRecentUnified()
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                _isRefreshingDashboard.value = false
            }
        }
    }

    fun refreshDiscover() {
        refreshAllDashboardCategories(force = true)
    }

    fun playYouTubeVideoWithContext(result: YouTubeSearchResult, contextList: List<YouTubeSearchResult>) {
        _localQueue.value = emptyList()
        val idx = contextList.indexOfFirst { it.videoId == result.videoId }
        val upNext = if (idx != -1 && idx < contextList.size - 1) {
            contextList.subList(idx + 1, contextList.size)
        } else {
            contextList.filter { it.videoId != result.videoId }
        }
        _upNextQueue.value = upNext
        if (upNext.size < 6) {
            fetchUpNextRadio(result.videoId, result.channelTitle.ifBlank { result.title }, forceAppend = true, title = result.title)
        }
        playYouTubeVideo(result)
    }

    /**
     * Last resort when YtMusicApiRepository (a free, flaky third-party wrapper) comes
     * back empty for a region/category — falls back to the app's own, more reliable
     * YouTube search so a lane never silently ends up empty and getting backfilled
     * with India's list (see quickPickRegions in DiscoverScreen.kt), which is what
     * made every region pill show the same songs.
     */
    private suspend fun quickPicksSearchFallback(query: String): List<YouTubeSearchResult> {
        return try {
            when (val outcome = YouTubeSearchRepository.search(query)) {
                is YouTubeSearchOutcome.Success -> outcome.results
                is YouTubeSearchOutcome.Error -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadAllQuickPicks(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (force || _indiaQuickPicks.value.isEmpty()) {
                launch {
                    val inTracks: List<YouTubeSearchResult> = try {
                        val trending = com.musicdrop.app.data.repository.YtMusicApiRepository.getTrending("IN")
                        if (trending.isNotEmpty()) trending else com.musicdrop.app.data.repository.YtMusicApiRepository.search("hindi trending songs").songs
                    } catch (_: Exception) { emptyList() }
                    val finalTracks = withVariety(force, inTracks.ifEmpty { quickPicksSearchFallback("hindi trending songs 2026") }, varietyQueriesFor("IN"))
                    if (finalTracks.isNotEmpty()) _indiaQuickPicks.value = finalTracks
                }
            }
            if (force || _pakistanQuickPicks.value.isEmpty()) {
                launch {
                    val pkTracks: List<YouTubeSearchResult> = try {
                        val trending = com.musicdrop.app.data.repository.YtMusicApiRepository.getTrending("PK")
                        val artistsSearch = com.musicdrop.app.data.repository.YtMusicApiRepository.search("pakistan top artists").songs
                        val combined = (trending + artistsSearch).distinctBy { it.videoId }
                        if (combined.isNotEmpty()) combined else com.musicdrop.app.data.repository.YtMusicApiRepository.search("pakistan trending songs").songs
                    } catch (_: Exception) { emptyList() }
                    val finalTracks = withVariety(force, pkTracks.ifEmpty { quickPicksSearchFallback("pakistani trending songs 2026") }, varietyQueriesFor("PK"))
                    if (finalTracks.isNotEmpty()) _pakistanQuickPicks.value = finalTracks
                }
            }
            if (force || _malayalamQuickPicks.value.isEmpty()) {
                launch {
                    val malTracks: List<YouTubeSearchResult> = try {
                        com.musicdrop.app.data.repository.YtMusicApiRepository.search("malayalam trending songs").songs
                    } catch (_: Exception) { emptyList() }
                    val finalTracks = withVariety(
                        force,
                        malTracks.ifEmpty { quickPicksSearchFallback("malayalam trending songs 2026") },
                        listOf("malayalam new movie songs 2026", "malayalam melody hits 2026", "malayalam mass beats 2026")
                    )
                    if (finalTracks.isNotEmpty()) _malayalamQuickPicks.value = finalTracks
                }
            }
            if (force || _tamilQuickPicks.value.isEmpty()) {
                launch {
                    val tamTracks: List<YouTubeSearchResult> = try {
                        com.musicdrop.app.data.repository.YtMusicApiRepository.search("tamil trending songs").songs
                    } catch (_: Exception) { emptyList() }
                    val finalTracks = withVariety(
                        force,
                        tamTracks.ifEmpty { quickPicksSearchFallback("tamil trending songs 2026") },
                        listOf("tamil melody hits 2026", "tamil mass beats 2026", "tamil new movie songs 2026")
                    )
                    if (finalTracks.isNotEmpty()) _tamilQuickPicks.value = finalTracks
                }
            }
            if (force || _teluguQuickPicks.value.isEmpty()) {
                launch {
                    val telTracks: List<YouTubeSearchResult> = try {
                        com.musicdrop.app.data.repository.YtMusicApiRepository.search("telugu trending songs").songs
                    } catch (_: Exception) { emptyList() }
                    val finalTracks = withVariety(
                        force,
                        telTracks.ifEmpty { quickPicksSearchFallback("telugu trending songs 2026") },
                        listOf("telugu melody hits 2026", "telugu mass beats 2026", "telugu new movie songs 2026")
                    )
                    if (finalTracks.isNotEmpty()) _teluguQuickPicks.value = finalTracks
                }
            }
            if (force || _coverQuickPicks.value.isEmpty()) {
                launch {
                    val tracks: List<YouTubeSearchResult> = try {
                        com.musicdrop.app.data.repository.YtMusicApiRepository.search("best cover songs 2026").songs
                    } catch (_: Exception) { emptyList() }
                    val finalTracks = withVariety(
                        force,
                        tracks.ifEmpty { quickPicksSearchFallback("best acoustic cover songs") },
                        listOf("unplugged cover songs 2026", "viral cover songs youtube", "acoustic mashup cover 2026")
                    )
                    if (finalTracks.isNotEmpty()) _coverQuickPicks.value = finalTracks
                }
            }
            if (force || _remixQuickPicks.value.isEmpty()) {
                launch {
                    val tracks: List<YouTubeSearchResult> = try {
                        com.musicdrop.app.data.repository.YtMusicApiRepository.search("trending remix songs 2026").songs
                    } catch (_: Exception) { emptyList() }
                    val finalTracks = withVariety(
                        force,
                        tracks.ifEmpty { quickPicksSearchFallback("dj remix songs party") },
                        listOf("party remix mashup 2026", "dj trending remix 2026", "club dance remix hits")
                    )
                    if (finalTracks.isNotEmpty()) _remixQuickPicks.value = finalTracks
                }
            }
            if (force || _lofiQuickPicks.value.isEmpty()) {
                launch {
                    val tracks: List<YouTubeSearchResult> = try {
                        com.musicdrop.app.data.repository.YtMusicApiRepository.search("lofi chill beats").songs
                    } catch (_: Exception) { emptyList() }
                    val finalTracks = withVariety(
                        force,
                        tracks.ifEmpty { quickPicksSearchFallback("lofi hip hop chill study beats") },
                        listOf("slowed reverb songs 2026", "chill beats to study", "lofi mashup relax")
                    )
                    if (finalTracks.isNotEmpty()) _lofiQuickPicks.value = finalTracks
                }
            }
            if (force || _guitarQuickPicks.value.isEmpty()) {
                launch {
                    val tracks: List<YouTubeSearchResult> = try {
                        com.musicdrop.app.data.repository.YtMusicApiRepository.search("acoustic guitar relaxing fingerstyle songs").songs
                    } catch (_: Exception) { emptyList() }
                    val finalTracks = withVariety(
                        force,
                        tracks.ifEmpty { quickPicksSearchFallback("acoustic guitar songs unplugged") },
                        listOf("fingerstyle guitar cover", "guitar instrumental relaxing", "unplugged guitar sessions")
                    )
                    if (finalTracks.isNotEmpty()) _guitarQuickPicks.value = finalTracks
                }
            }
            if (force || _ukuleleQuickPicks.value.isEmpty()) {
                launch {
                    val tracks: List<YouTubeSearchResult> = try {
                        com.musicdrop.app.data.repository.YtMusicApiRepository.search("ukulele chill indie songs").songs
                    } catch (_: Exception) { emptyList() }
                    val finalTracks = withVariety(
                        force,
                        tracks.ifEmpty { quickPicksSearchFallback("ukulele acoustic relaxing songs") },
                        listOf("ukulele cover songs", "ukulele happy songs", "ukulele instrumental chill")
                    )
                    if (finalTracks.isNotEmpty()) _ukuleleQuickPicks.value = finalTracks
                }
            }
            if (force || _shortsQuickPicks.value.isEmpty()) {
                launch {
                    val tracks: List<YouTubeSearchResult> = try {
                        com.musicdrop.app.data.repository.YtMusicApiRepository.search("trending music shorts clips").songs
                    } catch (_: Exception) { emptyList() }
                    val finalTracks = withVariety(
                        force,
                        tracks.ifEmpty { quickPicksSearchFallback("viral music shorts acoustic rap") },
                        listOf("viral reels songs 2026", "trending shorts music 2026", "insta reels trending audio")
                    )
                    if (finalTracks.isNotEmpty()) _shortsQuickPicks.value = finalTracks
                }
            }
            // ---- Regional cover & guitar collections (new: India / Pakistan) ----
            if (force || _indiaCoverQuickPicks.value.isEmpty()) {
                launch {
                    val tracks = varietySearch("bollywood cover songs 2026")
                    val finalTracks = withVariety(
                        force,
                        tracks.ifEmpty { quickPicksSearchFallback("hindi unplugged cover songs") },
                        listOf("bollywood unplugged mashup", "hindi acoustic cover 2026", "indian singers cover songs")
                    )
                    if (finalTracks.isNotEmpty()) _indiaCoverQuickPicks.value = finalTracks
                }
            }
            if (force || _indiaGuitarQuickPicks.value.isEmpty()) {
                launch {
                    val tracks = varietySearch("bollywood guitar cover fingerstyle")
                    val finalTracks = withVariety(
                        force,
                        tracks.ifEmpty { quickPicksSearchFallback("hindi songs acoustic guitar") },
                        listOf("bollywood unplugged guitar", "hindi guitar instrumental", "indian acoustic guitar sessions")
                    )
                    if (finalTracks.isNotEmpty()) _indiaGuitarQuickPicks.value = finalTracks
                }
            }
            if (force || _pakistanCoverQuickPicks.value.isEmpty()) {
                launch {
                    val tracks = varietySearch("pakistani cover songs 2026")
                    val finalTracks = withVariety(
                        force,
                        tracks.ifEmpty { quickPicksSearchFallback("urdu unplugged cover songs") },
                        listOf("coke studio cover songs", "pakistani acoustic cover 2026", "urdu singers cover songs")
                    )
                    if (finalTracks.isNotEmpty()) _pakistanCoverQuickPicks.value = finalTracks
                }
            }
            if (force || _pakistanGuitarQuickPicks.value.isEmpty()) {
                launch {
                    val tracks = varietySearch("pakistani guitar cover fingerstyle")
                    val finalTracks = withVariety(
                        force,
                        tracks.ifEmpty { quickPicksSearchFallback("urdu songs acoustic guitar") },
                        listOf("pakistani unplugged guitar", "urdu guitar instrumental", "coke studio guitar sessions")
                    )
                    if (finalTracks.isNotEmpty()) _pakistanGuitarQuickPicks.value = finalTracks
                }
            }
        }
    }

    private var isFetchingMoreSamples = false
    private var samplesPageNumber = 1

    fun loadMoreSamples() {
        if (isFetchingMoreSamples) return
        isFetchingMoreSamples = true
        viewModelScope.launch {
            try {
                samplesPageNumber++
                val queries = listOf(
                    "trending music videos 2026",
                    "viral music shorts billboard",
                    "top songs music video live",
                    "hit music videos clips",
                    "popular acoustic songs 2026"
                )
                val q = queries[samplesPageNumber % queries.size]
                val outcome = YouTubeSearchRepository.search(q)
                if (outcome is YouTubeSearchOutcome.Success) {
                    val current = _shortsQuickPicks.value.toMutableList()
                    val newOnes = outcome.results.filter { res -> current.none { it.videoId == res.videoId } }
                    if (newOnes.isNotEmpty()) {
                        current.addAll(newOnes)
                        _shortsQuickPicks.value = current
                    }
                }
            } catch (_: Exception) {
            } finally {
                isFetchingMoreSamples = false
            }
        }
    }

    fun extractAlbumOrMovie(title: String): String? {
        val fromRegex = Regex("""[\(\[]\s*from\s+["'“]([^"'”]+)["'”]\s*[\)\]]""", RegexOption.IGNORE_CASE)
        fromRegex.find(title)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        val fromSimpleRegex = Regex("""[\(\[]\s*from\s+([^\)\]]+)[\)\]]""", RegexOption.IGNORE_CASE)
        fromSimpleRegex.find(title)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        if (title.contains("|")) {
            val parts = title.split("|").map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val candidate = parts[1]
                val lower = candidate.lowercase()
                if (!lower.contains("official") && !lower.contains("video") && !lower.contains("audio") &&
                    !lower.contains("lyrical") && !lower.contains("teaser") && !lower.contains("trailer") &&
                    !lower.contains("song") && candidate.length in 2..40) {
                    return candidate
                }
            }
        }

        if (title.contains(" - ")) {
            val parts = title.split(" - ").map { it.trim() }.filter { it.isNotBlank() }
            for (p in parts) {
                val lower = p.lowercase()
                if (!lower.contains("official") && !lower.contains("video") && !lower.contains("audio") &&
                    !lower.contains("lyrical") && !lower.contains("teaser") && !lower.contains("trailer") &&
                    !lower.contains("song") && !lower.contains("feat") && p.length in 3..35) {
                    return p
                }
            }
        }
        return null
    }

    fun fetchUpNextRadio(
        videoId: String,
        fallbackQuery: String = "",
        forceAppend: Boolean = false,
        title: String = "",
        albumName: String? = null
    ) {
        viewModelScope.launch {
            val resolvedTitle = title.ifBlank { fallbackQuery }
            val resolvedArtist = fallbackQuery.ifBlank { title }
            val currentTrackMeta = playbackConnection.currentTrack.value

            // 1. Detect Album or Movie
            val detectedAlbum = albumName?.takeIf { it.isNotBlank() && !it.equals("YouTube Music", ignoreCase = true) }
                ?: (if (currentTrackMeta?.album?.isNotBlank() == true && currentTrackMeta.album != "YouTube Music") currentTrackMeta.album else null)
                ?: extractAlbumOrMovie(resolvedTitle)

            val cleanArtist = resolvedArtist
                .replace(" - Topic", "", ignoreCase = true)
                .replace("VEVO", "", ignoreCase = true)
                .trim()

            val candidates = mutableListOf<YouTubeSearchResult>()
            val existingIds = (_upNextQueue.value.map { it.videoId } + videoId).toMutableSet()

            // Step 1: Prioritize other songs from the SAME ALBUM / MOVIE
            if (!detectedAlbum.isNullOrBlank()) {
                val albumQuery = if (cleanArtist.isNotBlank() && !detectedAlbum.contains(cleanArtist, ignoreCase = true)) {
                    "$cleanArtist $detectedAlbum songs"
                } else {
                    "$detectedAlbum songs"
                }
                when (val outcome = YouTubeSearchRepository.search(albumQuery)) {
                    is YouTubeSearchOutcome.Success -> {
                        val albumTracks = outcome.results.filter { 
                            it.videoId !in existingIds && (parseDurationToMs(it.duration) ?: 0L) in 30_000L..600_000L
                        }
                        candidates.addAll(albumTracks)
                        existingIds.addAll(albumTracks.map { it.videoId })
                    }
                    else -> {}
                }
            }

            // Step 2: Prioritize other songs by the SAME ARTIST / UPLOADER / CHANNEL
            if (cleanArtist.isNotBlank()) {
                val artistQuery = if (cleanArtist.contains("song", ignoreCase = true)) cleanArtist else "$cleanArtist official songs"
                when (val outcome = YouTubeSearchRepository.search(artistQuery)) {
                    is YouTubeSearchOutcome.Success -> {
                        val artistTracks = outcome.results.filter { 
                            it.videoId !in existingIds && (parseDurationToMs(it.duration) ?: 0L) in 30_000L..600_000L
                        }
                        candidates.addAll(artistTracks)
                        existingIds.addAll(artistTracks.map { it.videoId })
                    }
                    else -> {}
                }
            }

            // Step 3: Supplement with YouTube Music's live related / radio queue (if candidates still < 8)
            if (candidates.size < 8 && videoId.isNotBlank()) {
                val radio = YouTubeMusicRepository.getUpNextRadioQueue(videoId, cleanArtist)
                val radioTracks = radio.filter { 
                    it.videoId !in existingIds && (parseDurationToMs(it.duration) ?: 0L) in 30_000L..600_000L
                }
                candidates.addAll(radioTracks)
                existingIds.addAll(radioTracks.map { it.videoId })
            }

            if (candidates.isNotEmpty()) {
                val current = _upNextQueue.value
                val newItems = candidates.filter { it.videoId != videoId && it.videoId !in current.map { c -> c.videoId } }
                if (current.isEmpty()) {
                    _upNextQueue.value = newItems
                } else if (current.size < 6 || forceAppend) {
                    _upNextQueue.value = current + newItems
                }
            }
        }
    }

    fun playTrackFromQueue(item: YouTubeSearchResult) {
        val currentList = _upNextQueue.value.toMutableList()
        currentList.remove(item)
        _upNextQueue.value = currentList
        playYouTubeVideo(item)
    }

    fun removeFromQueue(item: YouTubeSearchResult) {
        val currentList = _upNextQueue.value.toMutableList()
        currentList.remove(item)
        _upNextQueue.value = currentList
    }

    fun clearQueue() {
        _upNextQueue.value = emptyList()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val current = _upNextQueue.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _upNextQueue.value = current
        }
    }

    fun playNextTrackFromQueue() {
        val localList = _localQueue.value
        val localNext = localList.firstOrNull()
        if (localNext != null) {
            _localQueue.value = localList.drop(1)
            playDownloadedTrack(localNext, _downloadedTracks.value)
            return
        }
        playFromUpNextQueueWithFallback()
    }

    /**
     * Pops one track off the up-next queue and plays it. If YouTube's extraction fails
     * for that track, it's dropped and the NEXT track in the queue is tried
     * automatically. Always maintains a healthy upcoming queue so playback never stops.
     */
    private fun playFromUpNextQueueWithFallback() {
        val currentList = _upNextQueue.value
        val next = currentList.firstOrNull()
        if (next == null) {
            autoplayNext()
            return
        }
        val remaining = currentList.drop(1)
        _upNextQueue.value = remaining
        // Proactively keep queue filled so there's always something in the queue!
        if (remaining.size <= 3) {
            fetchUpNextRadio(next.videoId, next.channelTitle.ifBlank { next.title }, forceAppend = true, title = next.title)
        }
        playYouTubeVideo(next, onExtractionFailed = { playFromUpNextQueueWithFallback() })
    }

    // ── JioSaavn 320kbps Online Music ─────────────────────────────────────────
    private val _saavnResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val saavnResults: StateFlow<List<MediaItem>> = _saavnResults.asStateFlow()

    private val _saavnTrending = MutableStateFlow<List<MediaItem>>(emptyList())
    val saavnTrending: StateFlow<List<MediaItem>> = _saavnTrending.asStateFlow()

    private val _saavnLoading = MutableStateFlow(false)
    val saavnLoading: StateFlow<Boolean> = _saavnLoading.asStateFlow()
    private var saavnSearchJob: Job? = null

    fun loadSaavnTrending(force: Boolean = false) {
        if (!force && (_saavnTrending.value.isNotEmpty() || _saavnLoading.value)) return
        if (force && _saavnLoading.value) return
        viewModelScope.launch {
            _saavnLoading.value = true
            val songs = SaavnRepository.getTrending()
            _saavnTrending.value = songs
            _saavnLoading.value = false
        }
    }

    fun searchSaavn(query: String) {
        saavnSearchJob?.cancel()
        if (query.isBlank()) {
            _saavnResults.value = emptyList()
            _saavnLoading.value = false
            return
        }
        saavnSearchJob = viewModelScope.launch {
            delay(400)
            _saavnLoading.value = true
            val songs = SaavnRepository.search(query)
            _saavnResults.value = songs
            _saavnLoading.value = false
        }
    }

    fun playSaavnTrack(item: MediaItem) {
        currentPlaybackRetry = { playSaavnTrack(item) }
        recordRecentPlay(UnifiedTrack.Saavn(item))
        _isVideoMode.value = false
        _ytCurrentVideo.value = null
        playbackConnection.playTrack(item, listOf(item))
        openFullPlayer()
    }

    fun downloadSaavnTrack(item: MediaItem, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val safeDir = getSafeMusicDir()
                val cleanTitle = item.name.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().take(80).ifBlank { "Saavn_Track" }
                var outFile = java.io.File(safeDir, "$cleanTitle.mp3")
                var ok = streamDownloadToFile(item.uri.toString(), outFile)
                if (!ok) {
                    val fallbackDir = java.io.File(getApplication<Application>().filesDir, "Music").apply { if (!exists()) mkdirs() }
                    outFile = java.io.File(fallbackDir, "$cleanTitle.mp3")
                    ok = streamDownloadToFile(item.uri.toString(), outFile)
                }
                if (!ok || !outFile.exists() || outFile.length() == 0L) {
                    onDone(false, "")
                    return@launch
                }
                val downloadedTrack = com.musicdrop.app.data.repository.DownloadedTrack(
                    key = "sv:${item.id}",
                    title = item.name,
                    artist = item.artist,
                    duration = item.formattedDuration,
                    coverUrl = item.albumArtUri?.toString().orEmpty(),
                    filePath = outFile.absolutePath,
                    mimeType = "audio/mpeg",
                    downloadedAtMs = System.currentTimeMillis()
                )
                com.musicdrop.app.data.repository.DownloadedTracksStore.add(getApplication(), downloadedTrack)
                _downloadedTracks.value = com.musicdrop.app.data.repository.DownloadedTracksStore.getAll(getApplication())
                android.media.MediaScannerConnection.scanFile(
                    getApplication(), arrayOf(outFile.absolutePath), null, null
                )
                onDone(true, outFile.absolutePath)
            } catch (e: Exception) {
                onDone(false, "")
            }
        }
    }

    fun setYtSearchQuery(query: String) {
        _ytSearchQuery.value = query
        ytSearchJob?.cancel()
        if (query.isBlank()) {
            _ytSearchResults.value = emptyList()
            _detailedSearchResult.value = null
            _ytSearchError.value = null
            _ytSearchLoading.value = false
            return
        }
        ytSearchJob = viewModelScope.launch {
            delay(350)
            _ytSearchLoading.value = true
            _ytSearchError.value = null
            try {
                val detailed = YouTubeSearchRepository.searchDetailed(query)
                _detailedSearchResult.value = detailed
                _ytSearchResults.value = detailed.songs
                if (detailed.songs.isEmpty()) {
                    _ytSearchError.value = "No results found for \"$query\""
                }
            } catch (e: Exception) {
                _ytSearchError.value = e.localizedMessage ?: "Search error"
            } finally {
                _ytSearchLoading.value = false
            }
        }
    }

    fun playYouTubeVideo(
        result: YouTubeSearchResult,
        onExtractionFailed: (() -> Unit)? = null,
        startPositionMs: Long = 0L
    ) {
        // If this exact stream turns out to be stale/expired/IP-locked mid-playback,
        // PlaybackConnection asks for a fresh one via this retry — drop the cached
        // entry first so we don't just hand back the same dead URL again.
        currentPlaybackRetry = {
            streamCache.remove(result.videoId)
            com.musicdrop.app.data.youtube.StreamCacheStore.save(getApplication(), streamCache)
            playYouTubeVideo(result)
        }
        // Tapping a card and having nothing visibly happen for a second or two (while
        // the real stream URL resolves) reads as broken — this flags the exact card as
        // "preparing" so the grid can show a spinner on it immediately on tap.
        _preparingKey.value = UnifiedTrack.Youtube(result).key
        recordRecentPlay(UnifiedTrack.Youtube(result))
        fetchUpNextRadio(result.videoId, result.channelTitle.ifBlank { result.title }, title = result.title)
        // Video Mode: If user has toggled video mode on, persist it across song changes and autoplays!
        _isVideoMode.value = _userWantsVideoMode.value
        _ytCurrentVideo.value = result
        _ytIsPlaying.value = true
        val placeholderUri = Uri.parse("https://www.youtube.com/watch?v=${result.videoId}")
        val appMediaItem = MediaItem(
            id = result.videoId.hashCode().toLong(),
            uri = placeholderUri,
            name = result.title.ifBlank { "YouTube Audio" },
            size = 0L,
            dateAdded = System.currentTimeMillis() / 1000,
            mimeType = "audio/mp4",
            mediaType = MediaType.AUDIO,
            durationMs = 0L,
            artist = result.channelTitle,
            album = "YouTube Music",
            isSong = true,
            filePath = result.videoId,
            bucketName = "YouTube Stream",
            albumArtUri = Uri.parse(result.thumbnailUrl)
        )

        // Immediately update player UI metadata and open the full screen MusicPlayerScreen
        playbackConnection.setCurrentTrackMetadata(appMediaItem)
        openFullPlayer()

        // Extract direct audio stream in background and hand directly to ExoPlayer
        viewModelScope.launch {
            var streamMediaItem: com.musicdrop.app.data.model.MediaItem? = null

            // 0. Try the cache (in-memory, seeded from disk on launch) for instant playback —
            // skip a stale entry rather than handing ExoPlayer a URL YouTube would reject.
            val cached = streamCache[result.videoId]?.takeIf { it.isFresh() }
            if (cached != null) {
                streamMediaItem = cached.toMediaItem()
            }

            // 1. Race p_web and NewPipeExtractor in parallel for fastest resolution
            if (streamMediaItem == null) {
                val pWebDeferred = async(Dispatchers.IO) {
                    try {
                        val info = com.musicdrop.app.data.youtube.PWebExtractor
                            .getInstance(getApplication())
                            .extract(result.videoId, result.title, result.channelTitle, result.thumbnailUrl)
                        if (info != null) {
                            cacheStream(info)
                            info.toMediaItem()
                        } else null
                    } catch (e: Exception) { null }
                }

                val newPipeDeferred = async(Dispatchers.IO) {
                    try {
                        val info = NewPipeYouTubeExtractor.getInstance(getApplication()).extract(result.videoId)
                        if (info != null) {
                            // Previously only p_web results were cached, so replaying the exact
                            // same video re-ran the full cipher-solving extraction every time
                            // whenever NewPipeExtractor was the one that had won the race. Now
                            // also persisted to disk (via cacheStream) so it survives the app
                            // process dying, not just an in-app replay.
                            cacheStream(
                                com.musicdrop.app.data.youtube.PWebExtractor.StreamInfo(
                                    url = info.url,
                                    mimeType = info.mimeType,
                                    title = result.title,
                                    author = result.channelTitle,
                                    thumbnailUrl = result.thumbnailUrl,
                                    durationMs = info.durationSec * 1000L,
                                    videoId = result.videoId,
                                    expiresAtMs = com.musicdrop.app.data.youtube.StreamCacheStore.expiryFromUrl(info.url)
                                )
                            )
                        }
                        info?.toMediaItem(
                            videoId = result.videoId,
                            knownTitle = result.title,
                            knownAuthor = result.channelTitle,
                            knownThumb = result.thumbnailUrl
                        )
                    } catch (e: Exception) { null }
                }

                // Await whichever finishes first with a valid result
                val firstResult = kotlinx.coroutines.selects.select<com.musicdrop.app.data.model.MediaItem?> {
                    pWebDeferred.onAwait { it }
                    newPipeDeferred.onAwait { it }
                }

                streamMediaItem = firstResult ?: pWebDeferred.await() ?: newPipeDeferred.await()
            }

            // 2. Last resort: hand-rolled API extractor (ANDROID_TESTSUITE client)
            if (streamMediaItem == null) {
                android.util.Log.w("MainViewModel", "Parallel extraction failed, trying legacy API extractor for ${result.videoId}")
                val poToken = try {
                    poTokenManager.getPoToken()
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "PoToken fetch failed: ${e.message}")
                    null
                }
                _ytPoToken.value = poToken
                val info = YouTubeStreamExtractor.getInstance(getApplication()).extract(
                    result.videoId,
                    knownTitle  = result.title,
                    knownAuthor = result.channelTitle,
                    knownThumb  = result.thumbnailUrl,
                    poToken     = poToken
                )
                if (info != null) {
                    streamMediaItem = info.toMediaItem()
                }
            }

            if (streamMediaItem != null) {
                playbackConnection.playTrack(streamMediaItem, listOf(streamMediaItem), startPositionMs)
            } else {
                android.util.Log.e("MainViewModel", "All extractors failed for ${result.videoId}")
                if (onExtractionFailed != null) {
                    // Called from autoplay/queue fallback chains — let the caller try
                    // the next candidate instead of stopping playback here.
                    onExtractionFailed.invoke()
                } else {
                    closeFullPlayer()
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Couldn't play \"${result.title}\" — YouTube blocked this stream",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            if (_preparingKey.value == UnifiedTrack.Youtube(result).key) _preparingKey.value = null
        }
    }

    fun setVideoMode(enabled: Boolean) {
        _userWantsVideoMode.value = enabled
        _isVideoMode.value = enabled
        if (enabled) {
            resolveVideoForCurrentTrack()
        }
    }

    fun toggleVideoMode() {
        val newMode = !_isVideoMode.value
        setVideoMode(newMode)
    }

    fun extractValidVideoId(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val candidate = when {
            trimmed.contains("/vi_webp/") -> trimmed.substringAfter("/vi_webp/").substringBefore("/").substringBefore("?")
            trimmed.contains("/vi/") -> trimmed.substringAfter("/vi/").substringBefore("/").substringBefore("?")
            trimmed.contains("v=") -> trimmed.substringAfter("v=").substringBefore("&").substringBefore("?")
            trimmed.contains("youtu.be/") -> trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains(".") -> trimmed
            else -> null
        }?.trim()
        return if (candidate?.length == 11) candidate else null
    }

    fun resolveVideoForCurrentTrack() {
        val cur = playbackConnection.currentTrack.value ?: return
        if (!cur.isSong) {
            _ytCurrentVideo.value = null
            return
        }
        viewModelScope.launch {
            var vidId = extractValidVideoId(cur.filePath)
                ?: extractValidVideoId(cur.albumArtUri?.toString())
                ?: extractValidVideoId(cur.uri.toString())

            if (vidId == null) {
                // Clear stale video from previous track so it doesn't leak into this song
                if (_ytCurrentVideo.value != null) {
                    val prevTitle = _ytCurrentVideo.value?.title.orEmpty().lowercase()
                    val curTitle = cur.name.lowercase()
                    if (!prevTitle.contains(curTitle) && !curTitle.contains(prevTitle)) {
                        _ytCurrentVideo.value = null
                    }
                }

                val cleanName = cur.name
                    .replace(Regex("\\.(mp3|m4a|aac|flac|wav|ogg|opus)$", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("[^a-zA-Z0-9 ]"), " ")
                    .trim()

                if (cleanName.isNotBlank() && cleanName.length > 2) {
                    try {
                        _videoModeLoading.value = true
                        val detailed = YouTubeSearchRepository.searchDetailed("$cleanName ${cur.artist} official video")
                        val targetWords = cleanName.lowercase().split(Regex("\\s+")).filter { it.length > 2 && it !in setOf("audio", "song", "track", "unknown") }
                        val matched = detailed.songs.firstOrNull { candidate ->
                            val cTitle = candidate.title.lowercase()
                            val cChannel = candidate.channelTitle.lowercase()
                            val artist = cur.artist.lowercase().trim()
                            (artist.isNotBlank() && artist != "<unknown>" && artist != "unknown artist" && cChannel.contains(artist)) ||
                            cTitle.contains(cleanName.lowercase()) ||
                            (targetWords.isNotEmpty() && targetWords.any { cTitle.contains(it) })
                        }
                        if (matched != null) {
                            _ytCurrentVideo.value = matched
                            vidId = matched.videoId
                        }
                    } catch (_: Exception) {
                    } finally {
                        _videoModeLoading.value = false
                    }
                }
            }
            if (vidId != null && _ytCurrentVideo.value?.videoId != vidId) {
                _ytCurrentVideo.value = YouTubeSearchResult(
                    videoId = vidId,
                    title = cur.name,
                    channelTitle = cur.artist,
                    thumbnailUrl = cur.albumArtUri?.toString().orEmpty()
                )
            }
        }
    }

    // ── App Update ───────────────────────────────────────────────────────────
    private val _availableUpdate = MutableStateFlow<com.musicdrop.app.data.updater.AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<com.musicdrop.app.data.updater.AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _updateDownloadProgress = MutableStateFlow<Float?>(null)
    val updateDownloadProgress: StateFlow<Float?> = _updateDownloadProgress.asStateFlow()

    fun checkForAppUpdate(manualToast: Boolean = false) {
        viewModelScope.launch {
            val info = com.musicdrop.app.data.updater.AppUpdateManager.checkForUpdate(
                context = getApplication(),
                isManualCheck = manualToast
            )
            _availableUpdate.value = info
            if (manualToast) {
                if (info == null) {
                    android.widget.Toast.makeText(getApplication(), "MusicDrop is up to date (v${com.musicdrop.app.BuildConfig.VERSION_NAME})", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(getApplication(), "Update available: v${info.latestVersionName}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startAppUpdate(activity: android.app.Activity) {
        val info = _availableUpdate.value ?: return
        viewModelScope.launch {
            com.musicdrop.app.data.updater.AppUpdateManager.downloadAndInstallApk(
                activity = activity,
                updateInfo = info,
                onProgress = { _updateDownloadProgress.value = it },
                onError = { err ->
                    _updateDownloadProgress.value = null
                    android.widget.Toast.makeText(activity, "Update error: $err", android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    fun dismissUpdate() {
        val info = _availableUpdate.value
        if (info != null) {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("app_update_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putInt("dismissed_version_code", info.latestVersionCode).apply()
            } catch (_: Exception) {}
        }
        _availableUpdate.value = null
        _updateDownloadProgress.value = null
    }

    fun playYtMusicTrack(result: YouTubeSearchResult) = playYouTubeVideo(result)

    fun setYtPlaying(playing: Boolean) {
        _ytIsPlaying.value = playing
    }

    fun setYtExpanded(expanded: Boolean) {
        _ytExpanded.value = expanded
    }

    fun closeYtPlayer() {
        _ytCurrentVideo.value = null
        _ytIsPlaying.value = false
        _ytExpanded.value = false
        _isVideoMode.value = false
    }

    fun toggleYtFavorite(result: YouTubeSearchResult) {
        MusicFavoritesStore.toggle(getApplication(), result)
        _ytFavorites.value = MusicFavoritesStore.getAll(getApplication())
    }

    // ── LRCLIB Lyrics ────────────────────────────────────────────────────────
    private val _lyrics = MutableStateFlow<LrcLibRepository.LrcLibResult?>(null)
    val lyrics: StateFlow<LrcLibRepository.LrcLibResult?> = _lyrics.asStateFlow()
    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()

    fun fetchLyrics(trackName: String, artistName: String, albumName: String = "", durationSec: Int = 0, videoId: String = "") {
        viewModelScope.launch {
            _lyricsLoading.value = true
            var result: LrcLibRepository.LrcLibResult? = null
            if (videoId.isNotBlank()) {
                try {
                    val rawLyrics = com.musicdrop.app.data.repository.YtMusicApiRepository.getLyrics(videoId)
                    if (!rawLyrics.isNullOrBlank()) {
                        result = LrcLibRepository.LrcLibResult(
                            trackName = trackName,
                            artistName = artistName,
                            albumName = albumName,
                            durationSec = durationSec,
                            syncedLyrics = null,
                            plainLyrics = rawLyrics
                        )
                    }
                } catch (_: Exception) {}
            }
            if (result == null) {
                result = LrcLibRepository.search(trackName, artistName, albumName, durationSec)
            }
            _lyrics.value = result
            _lyricsLoading.value = false
        }
    }

    fun clearLyrics() { _lyrics.value = null }

    // ── YouTube Charts ───────────────────────────────────────────────────────
    private val _chartsTopSongs = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val chartsTopSongs: StateFlow<List<YouTubeSearchResult>> = _chartsTopSongs.asStateFlow()
    private val _chartsTrending = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val chartsTrending: StateFlow<List<YouTubeSearchResult>> = _chartsTrending.asStateFlow()
    private val _chartsLoading = MutableStateFlow(false)
    val chartsLoading: StateFlow<Boolean> = _chartsLoading.asStateFlow()
    private val _chartsRegion = MutableStateFlow("US")
    val chartsRegion: StateFlow<String> = _chartsRegion.asStateFlow()

    fun loadCharts(region: String = _chartsRegion.value) {
        viewModelScope.launch {
            _chartsRegion.value = region
            _chartsLoading.value = true
            val page = YouTubeChartsRepository.getCharts(region)
            _chartsTopSongs.value = page.topSongs
            _chartsTrending.value = page.trending
            _chartsLoading.value = false
        }
    }

    fun chartsAvailableRegions(): List<String> = YouTubeChartsRepository.availableRegions()

    // ── Vimeo ────────────────────────────────────────────────────────────────
    // Vimeo has no search feature in the reference app (confirmed by decompiling it —
    // Vimeo code exists only in its download/stream-resolution path) — this is a
    // paste-a-link importer, same shape as the Spotify importer below.
    private val _vimeoImportResults = MutableStateFlow<List<VimeoRepository.VimeoVideo>>(emptyList())
    val vimeoImportResults: StateFlow<List<VimeoRepository.VimeoVideo>> = _vimeoImportResults.asStateFlow()
    private val _vimeoImportLoading = MutableStateFlow(false)
    val vimeoImportLoading: StateFlow<Boolean> = _vimeoImportLoading.asStateFlow()
    private val _vimeoImportError = MutableStateFlow<String?>(null)
    val vimeoImportError: StateFlow<String?> = _vimeoImportError.asStateFlow()

    /** Paste a Vimeo video link — fetches real title/author/thumbnail/duration via oEmbed. */
    fun importVimeoLink(url: String) {
        viewModelScope.launch {
            _vimeoImportLoading.value = true
            _vimeoImportError.value = null
            val video = try {
                VimeoRepository.getOembed(url.trim())
            } catch (e: Exception) {
                null
            }
            if (video == null) {
                _vimeoImportError.value = "Couldn't read that Vimeo link — check it's a valid video URL"
            } else {
                _vimeoImportResults.value = listOf(video) + _vimeoImportResults.value.filter { it.id != video.id }
            }
            _vimeoImportLoading.value = false
        }
    }

    /**
     * Play a Vimeo video — tries real stream extraction first (direct MP4/HLS from
     * Vimeo's own player-config, same approach the reference app's embedded JS uses),
     * so it plays instantly through the normal audio player like any other track.
     * Falls back to the WebView embed overlay only if extraction fails (private/DRM).
     */
    fun playVimeoVideo(video: VimeoRepository.VimeoVideo) {
        // Vimeo re-fetches its stream URL fresh on every call already, so the retry
        // is simply "call this again" — no cache to invalidate.
        currentPlaybackRetry = { playVimeoVideo(video) }
        _preparingKey.value = UnifiedTrack.Vimeo(video).key
        recordRecentPlay(UnifiedTrack.Vimeo(video))
        _isVideoMode.value = false
        viewModelScope.launch {
            val stream = VimeoRepository.getStreamUrl(video.id)
            if (_preparingKey.value == UnifiedTrack.Vimeo(video).key) _preparingKey.value = null
            if (stream != null) {
                val mediaItem = MediaItem(
                    id          = video.id.hashCode().toLong(),
                    uri         = Uri.parse(stream.url),
                    name        = video.title,
                    size        = 0L,
                    dateAdded   = System.currentTimeMillis() / 1000,
                    mimeType    = stream.mimeType,
                    mediaType   = MediaType.AUDIO,
                    artist      = video.authorName.ifBlank { "Vimeo" },
                    album       = "Vimeo",
                    isSong      = true,
                    filePath    = stream.url,
                    bucketName  = "Vimeo",
                    albumArtUri = video.thumbnailUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                )
                playbackConnection.playTrack(mediaItem, listOf(mediaItem))
                openFullPlayer()
            } else {
                // If direct stream fails (DRM/embed restrictions), search YouTube audio bypass
                val match = findBestYouTubeMatch(SpotifyRepository.SpotifyItem(video.title, video.authorName, video.thumbnailUrl, 0L))
                if (match != null) {
                    playYouTubeVideo(match)
                } else {
                    val result = video.asSearchResult()
                    _ytCurrentVideo.value = result
                    _ytIsPlaying.value = true
                    _ytExpanded.value = true
                }
            }
        }
    }

    /**
     * Download a Vimeo video's audio using direct stream or YouTube bypass matching.
     */
    fun downloadVimeoVideo(video: VimeoRepository.VimeoVideo, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val stream = VimeoRepository.getStreamUrl(video.id)
            if (stream != null && !stream.isHls) {
                try {
                    val safeDir = getSafeMusicDir()
                    val cleanTitle = video.title.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().take(80).ifBlank { "Vimeo_Video" }
                    var outFile = java.io.File(safeDir, "$cleanTitle.mp4")
                    var ok = streamDownloadToFile(stream.url, outFile)
                    if (!ok) {
                        val fallbackDir = java.io.File(getApplication<Application>().filesDir, "Music").apply { if (!exists()) mkdirs() }
                        outFile = java.io.File(fallbackDir, "$cleanTitle.mp4")
                        ok = streamDownloadToFile(stream.url, outFile)
                    }
                    if (ok && outFile.exists() && outFile.length() > 0) {
                        val downloadedTrack = com.musicdrop.app.data.repository.DownloadedTrack(
                            key = "vm:${video.id}",
                            title = video.title,
                            artist = video.authorName,
                            duration = video.duration,
                            coverUrl = video.thumbnailUrl,
                            filePath = outFile.absolutePath,
                            mimeType = "video/mp4",
                            downloadedAtMs = System.currentTimeMillis()
                        )
                        com.musicdrop.app.data.repository.DownloadedTracksStore.add(getApplication(), downloadedTrack)
                        _downloadedTracks.value = com.musicdrop.app.data.repository.DownloadedTracksStore.getAll(getApplication())
                        android.media.MediaScannerConnection.scanFile(
                            getApplication(), arrayOf(outFile.absolutePath), null, null
                        )
                        onDone(true, outFile.absolutePath)
                        return@launch
                    }
                } catch (_: Exception) {}
            }
            // Fallback: match via YouTube audio bypass
            val match = findBestYouTubeMatch(SpotifyRepository.SpotifyItem(video.title, video.authorName, video.thumbnailUrl, 0L))
            if (match != null) {
                downloadYouTubeAudio(match, onDone)
            } else {
                onDone(false, "")
            }
        }
    }

    // ── Apple Music metadata ─────────────────────────────────────────────────
    private val _appleTracks = MutableStateFlow<List<AppleMusicRepository.AppleTrack>>(emptyList())
    val appleTracks: StateFlow<List<AppleMusicRepository.AppleTrack>> = _appleTracks.asStateFlow()
    private val _appleLoading = MutableStateFlow(false)
    val appleLoading: StateFlow<Boolean> = _appleLoading.asStateFlow()
    private var appleSearchJob: Job? = null

    fun searchAppleMusic(query: String) {
        appleSearchJob?.cancel()
        if (query.isBlank()) { _appleTracks.value = emptyList(); return }
        appleSearchJob = viewModelScope.launch {
            delay(400)
            _appleLoading.value = true
            _appleTracks.value = AppleMusicRepository.searchTracks(query)
            _appleLoading.value = false
        }
    }

    fun playAppleTrackPreview(track: AppleMusicRepository.AppleTrack) {
        if (track.previewUrl.isBlank()) {
            val match = SpotifyRepository.SpotifyItem(track.trackName, track.artistName, track.hdArtwork, 0L)
            matchAndPlaySpotifyItem(match)
            return
        }
        val mediaItem = MediaItem(
            id = track.trackId,
            uri = Uri.parse(track.previewUrl),
            name = track.trackName,
            size = 0L,
            dateAdded = System.currentTimeMillis() / 1000,
            mimeType = "audio/mp4",
            mediaType = MediaType.AUDIO,
            durationMs = 30_000L,
            artist = track.artistName,
            album = track.albumName.ifBlank { "Apple Music" },
            isSong = true,
            bucketName = "Apple Music",
            albumArtUri = track.hdArtwork.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        )
        playbackConnection.playTrack(mediaItem, listOf(mediaItem))
        openFullPlayer()
    }

    fun downloadAppleTrackPreview(track: AppleMusicRepository.AppleTrack, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (track.previewUrl.isNotBlank()) {
                try {
                    val safeDir = getSafeMusicDir()
                    val cleanTitle = track.trackName.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().take(80).ifBlank { "Apple_Preview" }
                    var outFile = java.io.File(safeDir, "$cleanTitle (preview).m4a")
                    var ok = streamDownloadToFile(track.previewUrl, outFile)
                    if (!ok) {
                        val fallbackDir = java.io.File(getApplication<Application>().filesDir, "Music").apply { if (!exists()) mkdirs() }
                        outFile = java.io.File(fallbackDir, "$cleanTitle (preview).m4a")
                        ok = streamDownloadToFile(track.previewUrl, outFile)
                    }
                    if (ok && outFile.exists() && outFile.length() > 0) {
                        val downloadedTrack = com.musicdrop.app.data.repository.DownloadedTrack(
                            key = "am:${track.trackId}",
                            title = track.trackName,
                            artist = track.artistName,
                            duration = "",
                            coverUrl = track.hdArtwork,
                            filePath = outFile.absolutePath,
                            mimeType = "audio/mp4",
                            downloadedAtMs = System.currentTimeMillis()
                        )
                        com.musicdrop.app.data.repository.DownloadedTracksStore.add(getApplication(), downloadedTrack)
                        _downloadedTracks.value = com.musicdrop.app.data.repository.DownloadedTracksStore.getAll(getApplication())
                        onDone(true, outFile.absolutePath)
                        return@launch
                    }
                } catch (_: Exception) {}
            }
            // Fallback match via YouTube bypass
            val match = SpotifyRepository.SpotifyItem(track.trackName, track.artistName, track.hdArtwork, 0L)
            matchAndDownloadSpotifyItem(match, onDone)
        }
    }

    // ── Spotify import (metadata only — matched to a playable YouTube result) ──
    private val _spotifyImportResults = MutableStateFlow<List<SpotifyRepository.SpotifyItem>>(emptyList())
    val spotifyImportResults: StateFlow<List<SpotifyRepository.SpotifyItem>> = _spotifyImportResults.asStateFlow()
    private val _spotifyImportLoading = MutableStateFlow(false)
    val spotifyImportLoading: StateFlow<Boolean> = _spotifyImportLoading.asStateFlow()
    private val _spotifyImportError = MutableStateFlow<String?>(null)
    val spotifyImportError: StateFlow<String?> = _spotifyImportError.asStateFlow()

    /** Paste a Spotify track/album/playlist link — fetches real metadata (title/artist/cover). */
    fun importSpotifyLink(url: String) {
        viewModelScope.launch {
            _spotifyImportLoading.value = true
            _spotifyImportError.value = null
            _spotifyImportResults.value = emptyList()
            val items = try {
                SpotifyRepository.resolveLink(url.trim())
            } catch (e: Exception) {
                null
            }
            if (items.isNullOrEmpty()) {
                _spotifyImportError.value = "Couldn't read that Spotify link — check it's a track, album, or playlist URL"
            } else {
                _spotifyImportResults.value = items
            }
            _spotifyImportLoading.value = false
        }
    }

    /**
     * Spotify's own audio is DRM-protected and never obtainable — this searches
     * YouTube for the closest title+artist match and plays that instead, exactly the
     * approach (and the exact caveat) the reference app uses for Spotify imports.
     *
     * The old version just took the first search result, which is why some tracks
     * "didn't play right" — YouTube's top hit for a title+artist query is often a
     * lyric video from a random reupload channel, a cover, or a full concert, not the
     * actual song. [bestYouTubeMatchFor] now scores every candidate (duration
     * closeness to the real Spotify track length, title/artist word overlap, and a
     * penalty for cover/remix/live/reaction-type uploads unless the real title itself
     * says that) and picks the best one instead of just the first.
     */
    fun matchAndPlaySpotifyItem(item: SpotifyRepository.SpotifyItem, onNoMatch: () -> Unit = {}) {
        viewModelScope.launch {
            val match = findBestYouTubeMatch(item)
            if (match != null) playYouTubeVideo(match) else onNoMatch()
        }
    }

    /** Same scored match, then downloads the matched YouTube audio. */
    fun matchAndDownloadSpotifyItem(item: SpotifyRepository.SpotifyItem, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val match = findBestYouTubeMatch(item)
            if (match != null) downloadYouTubeAudio(match, onDone) else onDone(false, "")
        }
    }

    /** Runs two search queries (with/without "audio") and scores every candidate. */
    private suspend fun findBestYouTubeMatch(item: SpotifyRepository.SpotifyItem): YouTubeSearchResult? {
        val query = "${item.title} ${item.artist}".trim()
        if (query.isBlank()) return null
        val candidates = LinkedHashMap<String, YouTubeSearchResult>()

        // music.youtube.com's own internal search (WEB_REMIX InnerTube client) is what
        // the reference app itself actually uses for this exact "match Spotify to a
        // playable source" step (confirmed by decompiling it — utils/update/b.smali's
        // getMusicFromYMBySearch hits music.youtube.com/youtubei/v1/search, not plain
        // youtube.com search). It's biased toward official/topic-channel audio uploads
        // rather than random reuploads, so it's tried first and weighted first.
        try {
            YouTubeMusicRepository.search(query).results.forEach { candidates.putIfAbsent(it.videoId, it) }
        } catch (e: Exception) { /* fall through to plain YouTube search below */ }

        // Plain YouTube search as a second pool — catches tracks YT Music's smaller
        // index doesn't have, and gives the scorer more candidates to choose from.
        if (candidates.size < 5) {
            when (val outcome = YouTubeSearchRepository.search(query)) {
                is YouTubeSearchOutcome.Success -> outcome.results.forEach { candidates.putIfAbsent(it.videoId, it) }
                is YouTubeSearchOutcome.Error -> {}
            }
        }
        if (candidates.isEmpty()) return null
        // A small bonus for YT Music-sourced candidates is baked into the ordering
        // itself (LinkedHashMap preserves insertion order, and ties in score are rare
        // once duration+word-overlap scoring runs), so this still resolves to the more
        // reliable source first when candidates are otherwise equally good.
        return candidates.values.maxByOrNull { scoreYouTubeCandidate(it, item) }
    }

    private val undesiredUploadWords = listOf(
        "cover", "reaction", "live", "karaoke", "instrumental", "8d audio",
        "sped up", "slowed", "remix", "tutorial", "reversed", "nightcore"
    )

    /** Higher is a better match. Combines duration closeness with title/artist overlap. */
    private fun scoreYouTubeCandidate(candidate: YouTubeSearchResult, item: SpotifyRepository.SpotifyItem): Double {
        var score = 0.0
        val candidateText = "${candidate.title} ${candidate.channelTitle}".lowercase()
        val itemText = "${item.title} ${item.artist}".lowercase()

        // Duration closeness — the single strongest signal when both sides have one:
        // a lyric-video reupload, cover, or full concert almost never matches to the
        // second the way the real studio track does.
        val candidateMs = parseDurationToMs(candidate.duration)
        if (candidateMs != null && item.durationMs > 0) {
            val diffSec = kotlin.math.abs(candidateMs - item.durationMs) / 1000.0
            score += when {
                diffSec <= 3 -> 50.0
                diffSec <= 8 -> 35.0
                diffSec <= 20 -> 15.0
                diffSec <= 45 -> -10.0
                else -> -40.0
            }
        }

        // Title/artist word overlap — rewards candidates that actually mention the
        // song title and artist rather than just something vaguely similar.
        val itemWords = itemText.split(Regex("[^a-z0-9]+")).filter { it.length > 1 }.toSet()
        val candidateWords = candidateText.split(Regex("[^a-z0-9]+")).filter { it.length > 1 }.toSet()
        if (itemWords.isNotEmpty()) {
            val overlap = itemWords.intersect(candidateWords).size.toDouble() / itemWords.size
            score += overlap * 20.0
        }
        if (candidate.channelTitle.isNotBlank() &&
            item.artist.lowercase().contains(candidate.channelTitle.lowercase().trim())
        ) {
            score += 10.0 // channel name itself matches the artist — likely the official/topic channel
        }

        // Penalize cover/live/remix-type uploads unless the real track is actually
        // that (e.g. Spotify item title itself says "Live" or "Remix").
        for (word in undesiredUploadWords) {
            if (candidateText.contains(word) && !itemText.contains(word)) {
                score -= 25.0
            }
        }

        return score
    }

    /** Parses YouTube's "M:SS" / "H:MM:SS" duration strings to milliseconds, or null. */
    private fun parseDurationToMs(duration: String): Long? {
        if (duration.isBlank()) return null
        val parts = duration.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty() || parts.size > 3) return null
        var seconds = 0L
        for (p in parts) seconds = seconds * 60 + p
        return seconds * 1000L
    }

    /** Fetch HD artwork for the current track and update its cover. */
    fun enrichCurrentTrackArtwork() {
        val track = playbackConnection.currentTrack.value ?: return
        if (track.albumArtUri != null) return // already has art
        viewModelScope.launch {
            val url = AppleMusicRepository.getArtwork(track.name, track.artist) ?: return@launch
            // Post the URL back — the MusicPlayerScreen observes appleArtworkUrl
            _appleArtworkUrl.value = url
        }
    }

    private val _appleArtworkUrl = MutableStateFlow<String?>(null)
    val appleArtworkUrl: StateFlow<String?> = _appleArtworkUrl.asStateFlow()

    // ── MusiX Server ─────────────────────────────────────────────────────────
    private val _trendingTags = MutableStateFlow<List<MusiXServerRepository.TrendingTag>>(emptyList())
    val trendingTags: StateFlow<List<MusiXServerRepository.TrendingTag>> = _trendingTags.asStateFlow()
    private val _featureFlags = MutableStateFlow(MusiXServerRepository.FeatureFlags())
    val featureFlags: StateFlow<MusiXServerRepository.FeatureFlags> = _featureFlags.asStateFlow()
    private val _curatedPlaylists = MutableStateFlow<List<MusiXServerRepository.CuratedPlaylist>>(emptyList())
    val curatedPlaylists: StateFlow<List<MusiXServerRepository.CuratedPlaylist>> = _curatedPlaylists.asStateFlow()

    private fun loadMusiXServer() {
        viewModelScope.launch {
            _trendingTags.value = MusiXServerRepository.getTrendingTags()
        }
        viewModelScope.launch {
            _featureFlags.value = MusiXServerRepository.getFeatureFlags()
        }
        viewModelScope.launch {
            _curatedPlaylists.value = MusiXServerRepository.getFeaturedPlaylists()
        }
    }

    /** Play all tracks in a curated MusiX playlist sequentially. */
    fun playCuratedPlaylist(playlist: MusiXServerRepository.CuratedPlaylist) {
        val first = playlist.tracks.firstOrNull() ?: return
        playYouTubeVideo(first)
        // remaining tracks could be queued in future queue support
    }

    // ── Unified music cards ──────────────────────────────────────────────────
    // The home screen should feel like ONE music library, not a list of which
    // backend a song came from. These are the single play/download entry points
    // the UI calls — same idea as the reference app's own "one download button,
    // dispatch internally by a hidden source field" design — plus a cross-source
    // "recently played" history that works no matter which source a track is from.

    private val _popularUnified = MutableStateFlow<List<UnifiedTrack>>(emptyList())
    val popularUnified: StateFlow<List<UnifiedTrack>> = _popularUnified.asStateFlow()

    private val _recentUnified = MutableStateFlow<List<UnifiedTrack>>(emptyList())
    val recentUnified: StateFlow<List<UnifiedTrack>> = _recentUnified.asStateFlow()

    /** Reads on-device "recently played" history (works across all sources). */
    fun loadRecentUnified() {
        _recentUnified.value = RecentPlaysStore.getAll(getApplication())
    }

    private val playbackHistory = mutableListOf<UnifiedTrack>()
    private var historyIndex = -1

    private fun recordRecentPlay(track: UnifiedTrack) {
        lastPlayedUnified = track
        RecentPlaysStore.record(getApplication(), track)
        _recentUnified.value = RecentPlaysStore.getAll(getApplication())
        loadTasteRecommendations()
        
        // Push to in-session playback history stack
        if (historyIndex < 0 || historyIndex >= playbackHistory.size || playbackHistory[historyIndex].key != track.key) {
            if (historyIndex >= 0 && historyIndex < playbackHistory.size - 1) {
                // If we were navigating back and played a new song, truncate forward history
                playbackHistory.subList(historyIndex + 1, playbackHistory.size).clear()
            }
            playbackHistory.add(track)
            if (playbackHistory.size > 50) playbackHistory.removeAt(0)
            historyIndex = playbackHistory.size - 1
        }
    }

    fun playPreviousTrack() {
        if (historyIndex > 0 && playbackHistory.isNotEmpty()) {
            historyIndex--
            val prev = playbackHistory[historyIndex]
            playUnified(prev)
        } else {
            playbackConnection.seekTo(0L)
        }
    }

    fun downloadCurrentTrack(onDone: (Boolean, String) -> Unit) {
        val current = playbackConnection.currentTrack.value
        if (current != null && current.uri.toString().startsWith("http")) {
            viewModelScope.launch {
                try {
                    val safeDir = getPublicMusicDir()
                    val cleanTitle = current.name.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().take(80).ifBlank { "Track" }
                    val ext = if (current.mimeType.contains("webm") || current.mimeType.contains("opus")) "opus" else "m4a"
                    val fileName = "$cleanTitle.$ext"
                    val outFile = java.io.File(safeDir, fileName)

                    val ok = streamDownloadToFile(current.uri.toString(), outFile)
                    if (ok) {
                        val downloadedTrack = com.musicdrop.app.data.repository.DownloadedTrack(
                            key = current.filePath.orEmpty().ifBlank { "yt:${current.id}" },
                            title = current.name,
                            artist = current.artist,
                            duration = current.formattedDuration,
                            coverUrl = current.albumArtUri?.toString().orEmpty(),
                            filePath = outFile.absolutePath,
                            mimeType = current.mimeType.ifBlank { "audio/mp4" },
                            downloadedAtMs = System.currentTimeMillis()
                        )
                        com.musicdrop.app.data.repository.DownloadedTracksStore.add(getApplication(), downloadedTrack)
                        _downloadedTracks.value = com.musicdrop.app.data.repository.DownloadedTracksStore.getAll(getApplication())
                        android.media.MediaScannerConnection.scanFile(
                            getApplication(),
                            arrayOf(outFile.absolutePath),
                            arrayOf(if (ext == "opus") "audio/opus" else "audio/mp4"),
                            null
                        )
                        onDone(true, outFile.absolutePath)
                        return@launch
                    }
                } catch (_: Exception) {}

                // Fallback if direct stream failed
                val lastPlayed = lastPlayedUnified
                if (lastPlayed != null) {
                    downloadUnified(lastPlayed, onDone)
                } else {
                    onDone(false, "")
                }
            }
            return
        }

        val lastPlayed = lastPlayedUnified
        if (lastPlayed != null) {
            downloadUnified(lastPlayed, onDone)
            return
        }
        val currentYt = _ytCurrentVideo.value
        if (currentYt != null) {
            downloadYouTubeAudio(currentYt, onDone)
            return
        }
        onDone(false, "")
    }

    /** Download official video (MP4 HD) for the currently playing track. */
    fun downloadCurrentVideo(onDone: (Boolean, String) -> Unit) {
        val current = playbackConnection.currentTrack.value ?: return onDone(false, "")
        val path = current.filePath.orEmpty()
        val art = current.albumArtUri?.toString().orEmpty()
        val uriStr = current.uri.toString()

        val vidId = when {
            path.startsWith("yt:") -> path.removePrefix("yt:")
            path.length == 11 && !path.contains("/") && !path.contains(".") && !path.contains(":") -> path
            art.contains("/vi_webp/") -> art.substringAfter("/vi_webp/").substringBefore("/").substringBefore("?")
            art.contains("/vi/") -> art.substringAfter("/vi/").substringBefore("/").substringBefore("?")
            uriStr.contains("v=") -> uriStr.substringAfter("v=").substringBefore("&").substringBefore("?")
            uriStr.contains("youtu.be/") -> uriStr.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            _ytCurrentVideo.value?.takeIf { it.title.equals(current.name, ignoreCase = true) || current.filePath.orEmpty().contains(it.videoId) }?.videoId != null -> _ytCurrentVideo.value?.videoId
            else -> null
        }

        val target = if (vidId != null) {
            YouTubeSearchResult(
                videoId = vidId,
                title = current.name,
                channelTitle = current.artist,
                thumbnailUrl = current.albumArtUri?.toString().orEmpty()
            )
        } else {
            _ytCurrentVideo.value?.takeIf { it.title.equals(current.name, ignoreCase = true) }
        }

        if (target != null) {
            downloadYouTubeVideo(target, onDone)
        } else {
            viewModelScope.launch {
                try {
                    val detailed = YouTubeSearchRepository.searchDetailed("${current.name} ${current.artist} official video")
                    val match = detailed.songs.firstOrNull()
                    if (match != null) {
                        downloadYouTubeVideo(match, onDone)
                    } else {
                        android.widget.Toast.makeText(getApplication(), "No video found to download", android.widget.Toast.LENGTH_SHORT).show()
                        onDone(false, "")
                    }
                } catch (e: Exception) {
                    onDone(false, "")
                }
            }
        }
    }

    fun isCurrentTrackVideoDownloaded(): Boolean {
        val current = playbackConnection.currentTrack.value ?: return false
        val vidId = current.filePath?.takeIf { it.length == 11 && !it.contains("/") && !it.contains(".") }
            ?: current.albumArtUri?.toString()?.let { uriStr ->
                if (uriStr.contains("/vi/")) uriStr.substringAfter("/vi/").substringBefore("/")
                else null
            }
        val targetKey = if (vidId != null) "yt_video:$vidId" else ""
        return _downloadedTracks.value.any {
            (targetKey.isNotBlank() && it.key == targetKey) ||
            (it.title.contains(current.name, ignoreCase = true) && (it.mimeType.startsWith("video") || it.filePath.endsWith(".mp4", ignoreCase = true)))
        }
    }

        fun isCurrentTrackDownloaded(): Boolean {
        val current = playbackConnection.currentTrack.value ?: return false
        val path = current.filePath.orEmpty()
        val key = if (path.isNotBlank()) path else current.id.toString()
        val formattedKey = if (!key.contains(":")) "yt:$key" else key
        return _downloadedTracks.value.any { it.key == formattedKey || it.title.equals(current.name, ignoreCase = true) }
    }

    // ── Song Details & Library Options Actions ──

    fun editSongDetails(
        track: MediaItem,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newCoverUri: String? = null
    ) {
        val updatedTitle = newTitle.trim().ifBlank { track.name }
        val updatedArtist = newArtist.trim().ifBlank { track.artist }
        val updatedAlbum = newAlbum.trim().ifBlank { track.album }
        val updatedCover = newCoverUri?.takeIf { it.isNotBlank() } ?: track.albumArtUri?.toString()

        // 1. Update in DownloadedTracksStore
        val key = track.filePath.orEmpty().ifBlank { "yt:${track.id}" }
        DownloadedTracksStore.updateTrack(
            context = getApplication(),
            key = key,
            newTitle = updatedTitle,
            newArtist = updatedArtist,
            newCoverUrl = updatedCover
        )
        _downloadedTracks.value = DownloadedTracksStore.getAll(getApplication())

        // 2. Update in-memory collections for instant UI update
        _songs.value = _songs.value.map { item ->
            if (item.id == track.id || item.filePath == track.filePath) {
                item.copy(
                    name = updatedTitle,
                    artist = updatedArtist,
                    album = updatedAlbum,
                    albumArtUri = updatedCover?.let { android.net.Uri.parse(it) } ?: item.albumArtUri
                )
            } else item
        }
        _allAudio.value = _allAudio.value.map { item ->
            if (item.id == track.id || item.filePath == track.filePath) {
                item.copy(
                    name = updatedTitle,
                    artist = updatedArtist,
                    album = updatedAlbum,
                    albumArtUri = updatedCover?.let { android.net.Uri.parse(it) } ?: item.albumArtUri
                )
            } else item
        }

        // 3. If currently playing, update notification and full player
        if (playbackConnection.currentTrack.value?.id == track.id) {
            val updatedItem = track.copy(
                name = updatedTitle,
                artist = updatedArtist,
                album = updatedAlbum,
                albumArtUri = updatedCover?.let { android.net.Uri.parse(it) } ?: track.albumArtUri
            )
            playbackConnection.setCurrentTrackMetadata(updatedItem)
        }

        android.widget.Toast.makeText(getApplication(), "Details updated successfully", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun deleteSong(track: MediaItem, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            var deleted = false
            try {
                val key = track.filePath.orEmpty().ifBlank { "yt:${track.id}" }
                DownloadedTracksStore.remove(getApplication(), key)
                _downloadedTracks.value = DownloadedTracksStore.getAll(getApplication())

                val path = track.filePath.orEmpty()
                if (path.isNotBlank()) {
                    val file = java.io.File(path)
                    if (file.exists()) {
                        deleted = file.delete()
                        android.media.MediaScannerConnection.scanFile(
                            getApplication(), arrayOf(file.absolutePath), null, null
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to delete song: ${e.message}", e)
            }

            _songs.value = _songs.value.filter { it.id != track.id && it.filePath != track.filePath }
            _allAudio.value = _allAudio.value.filter { it.id != track.id && it.filePath != track.filePath }

            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "Song removed from device", android.widget.Toast.LENGTH_SHORT).show()
                onDone(deleted)
            }
        }
    }

    fun hideSong(track: MediaItem) {
        val key = track.filePath.orEmpty().ifBlank { track.id.toString() }
        com.musicdrop.app.data.repository.HiddenTracksStore.hideTrack(getApplication(), key)
        _songs.value = _songs.value.filter { it.id != track.id && it.filePath != track.filePath }
        _allAudio.value = _allAudio.value.filter { it.id != track.id && it.filePath != track.filePath }
        android.widget.Toast.makeText(getApplication(), "Song hidden from library", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun changeSongCover(track: MediaItem, newCoverUri: String) {
        editSongDetails(track, track.name, track.artist, track.album, newCoverUri)
    }

    fun setSongAsRingtone(context: Context, track: MediaItem) {
        try {
            val path = track.filePath.orEmpty()
            val file = java.io.File(path)
            if (!file.exists()) {
                android.widget.Toast.makeText(context, "Please download the track first to set as ringtone", android.widget.Toast.LENGTH_LONG).show()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!android.provider.Settings.System.canWrite(context)) {
                    android.widget.Toast.makeText(context, "Please allow 'Modify system settings' permission to set ringtone", android.widget.Toast.LENGTH_LONG).show()
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = android.net.Uri.parse("package:" + context.packageName)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                }
            }

            val uri = android.net.Uri.fromFile(file)
            android.media.RingtoneManager.setActualDefaultRingtoneUri(
                context,
                android.media.RingtoneManager.TYPE_RINGTONE,
                uri
            )
            android.widget.Toast.makeText(context, "Ringtone updated: ${track.name}", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to set ringtone: ${e.message}", e)
            android.widget.Toast.makeText(context, "Could not set ringtone: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun playNext(track: MediaItem) {
        playbackConnection.playNext(track)
        android.widget.Toast.makeText(getApplication(), "Will play next: ${track.name}", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun addToQueue(track: MediaItem) {
        playbackConnection.addToQueue(track)
        android.widget.Toast.makeText(getApplication(), "Added to queue: ${track.name}", android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * "Once the running song ends, ready to play anything" — when a track finishes
     * naturally, pull a few more from the same artist/category on the same source and
     * keep going, instead of just stopping. YouTube Music-style autoplay/radio.
     */
    fun autoplayNext() {
        // Local/offline queue takes priority (mirrors playNextTrackFromQueue) — this is
        // a safety net for when the current track ended without ExoPlayer's own native
        // playlist auto-advance already having handled it (e.g. repeat-one was toggled).
        val localNext = _localQueue.value.firstOrNull()
        if (localNext != null) {
            _localQueue.value = _localQueue.value.drop(1)
            playDownloadedTrack(localNext, _downloadedTracks.value)
            return
        }

        val nextInQueue = _upNextQueue.value.firstOrNull()
        if (nextInQueue != null) {
            playFromUpNextQueueWithFallback()
            return
        }

        val track = lastPlayedUnified ?: _ytCurrentVideo.value?.let { UnifiedTrack.Youtube(it) }
        viewModelScope.launch {
            val candidates = if (track != null) {
                try {
                    fetchMoreLikeThis(track)
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()

            val fallbackPool = if (candidates.isNotEmpty()) candidates else {
                val artistOrChannel = (track as? UnifiedTrack.Youtube)?.result?.channelTitle?.ifBlank { (track as? UnifiedTrack.Youtube)?.result?.title }.orEmpty()
                if (artistOrChannel.isNotBlank()) {
                    val query = if (artistOrChannel.contains("song", ignoreCase = true)) artistOrChannel else "$artistOrChannel official songs"
                    when (val outcome = YouTubeSearchRepository.search(query)) {
                        is YouTubeSearchOutcome.Success -> outcome.results
                            .filter { (parseDurationToMs(it.duration) ?: 0L) in 30_000L..600_000L }
                            .map { UnifiedTrack.Youtube(it) }
                        is YouTubeSearchOutcome.Error -> emptyList()
                    }
                } else emptyList()
            }

            val ordered = fallbackPool.filter { track == null || it.key != track.key }
            val prioritized = ordered.filterNot { it.key in autoplayHistory } + ordered.filter { it.key in autoplayHistory }

            // Ensure upNextQueue is populated with upcoming songs so queue always has tracks!
            if (prioritized.size > 1) {
                val upcoming = prioritized.drop(1).mapNotNull { (it as? UnifiedTrack.Youtube)?.result }
                if (upcoming.isNotEmpty()) {
                    _upNextQueue.value = _upNextQueue.value + upcoming
                }
            }

            playCandidatesWithFallback(prioritized)
        }
    }

    /**
     * Proactively resolves and caches the next upcoming song's stream URL 18s before
     * the current track finishes. When the song ends, playback switches instantaneously
     * with zero network latency, eliminating buffering pauses and gaps.
     */
    fun preloadNextTrack() {
        // 1. Proactively keep queue filled so there's always something in the queue!
        if (_upNextQueue.value.size < 4) {
            val cur = _ytCurrentVideo.value
            if (cur != null) {
                fetchUpNextRadio(
                    videoId = cur.videoId,
                    fallbackQuery = cur.channelTitle.ifBlank { cur.title },
                    forceAppend = true,
                    title = cur.title
                )
            }
        }

        // 2. Pre-extract the top upcoming song's stream URL into memory & disk cache
        val nextInQueue = _upNextQueue.value.firstOrNull() ?: return
        val cached = streamCache[nextInQueue.videoId]?.takeIf { it.isFresh() }
        if (cached != null) return // Already fresh in cache!

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pWeb = com.musicdrop.app.data.youtube.PWebExtractor
                    .getInstance(getApplication())
                    .extract(nextInQueue.videoId, nextInQueue.title, nextInQueue.channelTitle, nextInQueue.thumbnailUrl)
                if (pWeb != null) {
                    cacheStream(pWeb)
                } else {
                    val np = NewPipeYouTubeExtractor.getInstance(getApplication()).extract(nextInQueue.videoId)
                    if (np != null) {
                        cacheStream(
                            com.musicdrop.app.data.youtube.PWebExtractor.StreamInfo(
                                url = np.url,
                                mimeType = np.mimeType,
                                title = nextInQueue.title,
                                author = nextInQueue.channelTitle,
                                thumbnailUrl = nextInQueue.thumbnailUrl,
                                durationMs = np.durationSec * 1000L,
                                videoId = nextInQueue.videoId,
                                expiresAtMs = com.musicdrop.app.data.youtube.StreamCacheStore.expiryFromUrl(np.url)
                            )
                        )
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    fun fallbackToOfflinePlayback() {
        val downloaded = _downloadedTracks.value
        if (downloaded.isNotEmpty()) {
            val pick = downloaded.random()
            playDownloadedTrack(pick, downloaded)
            _networkStatusBanner.value = "You're offline — playing from your downloaded library"
        } else {
            _networkStatusBanner.value = "You're offline — connect to internet or download music"
        }
    }

    fun dismissNetworkBanner() {
        _networkStatusBanner.value = null
    }

    /**
     * Tries autoplay candidates one at a time and moves on to the next whenever a
     * YouTube stream fails to extract (blocked/expired) instead of surfacing an error
     * toast and leaving playback dead — that "song finished, nothing plays next" was
     * the actual bug. Saavn/local candidates aren't retried this way since they don't
     * fail in that mode. Silently gives up once every candidate has been tried.
     */
    private fun playCandidatesWithFallback(candidates: List<UnifiedTrack>) {
        val next = candidates.firstOrNull() ?: run {
            if (!networkMonitor.isCurrentlyConnected()) {
                fallbackToOfflinePlayback()
            }
            return
        }
        val rest = candidates.drop(1)

        autoplayHistory.addLast(next.key)
        while (autoplayHistory.size > 20) autoplayHistory.removeFirst()

        if (next is UnifiedTrack.Youtube) {
            playYouTubeVideo(next.result, onExtractionFailed = { playCandidatesWithFallback(rest) })
        } else {
            playUnified(next)
        }
    }

    /** More tracks "like this one" — same artist/channel where possible, same source. */
    private suspend fun fetchMoreLikeThis(track: UnifiedTrack): List<UnifiedTrack> = when (track) {
        is UnifiedTrack.Youtube -> {
            val queued = _upNextQueue.value.filter { it.videoId != track.result.videoId }
            if (queued.isNotEmpty()) {
                queued.map { UnifiedTrack.Youtube(it) }
            } else {
                val detectedAlbum = extractAlbumOrMovie(track.result.title)
                val cleanArtist = track.result.channelTitle
                    .replace(" - Topic", "", ignoreCase = true)
                    .replace("VEVO", "", ignoreCase = true)
                    .trim()
                val found = mutableListOf<YouTubeSearchResult>()
                if (!detectedAlbum.isNullOrBlank()) {
                    val albumQuery = if (cleanArtist.isNotBlank() && !detectedAlbum.contains(cleanArtist, ignoreCase = true)) {
                        "$cleanArtist $detectedAlbum songs"
                    } else {
                        "$detectedAlbum songs"
                    }
                    when (val res = YouTubeSearchRepository.search(albumQuery)) {
                        is YouTubeSearchOutcome.Success -> {
                            found.addAll(res.results.filter { 
                                it.videoId != track.result.videoId && (parseDurationToMs(it.duration) ?: 0L) in 30_000L..600_000L 
                            })
                        }
                        else -> {}
                    }
                }
                if (found.isEmpty() && cleanArtist.isNotBlank()) {
                    val artistQuery = if (cleanArtist.contains("song", ignoreCase = true)) cleanArtist else "$cleanArtist official songs"
                    when (val res = YouTubeSearchRepository.search(artistQuery)) {
                        is YouTubeSearchOutcome.Success -> {
                            found.addAll(res.results.filter { 
                                it.videoId != track.result.videoId && (parseDurationToMs(it.duration) ?: 0L) in 30_000L..600_000L 
                            })
                        }
                        else -> {}
                    }
                }
                if (found.isEmpty()) {
                    val radio = YouTubeMusicRepository.getUpNextRadioQueue(track.result.videoId, cleanArtist)
                    found.addAll(radio.filter { it.videoId != track.result.videoId })
                }
                if (found.isNotEmpty()) {
                    _upNextQueue.value = found
                    found.map { UnifiedTrack.Youtube(it) }
                } else emptyList()
            }
        }

        is UnifiedTrack.Saavn -> {
            SaavnRepository.search(track.item.artist.ifBlank { track.item.name })
                .filter { it.id != track.item.id }
                .map { UnifiedTrack.Saavn(it) }
        }
        is UnifiedTrack.Vimeo -> {
            val query = track.video.authorName.ifBlank { track.video.title }
            when (val outcome = YouTubeSearchRepository.search(query)) {
                is YouTubeSearchOutcome.Success -> outcome.results.map { UnifiedTrack.Youtube(it) }
                is YouTubeSearchOutcome.Error -> emptyList()
            }
        }
        is UnifiedTrack.Local -> {
            // Offline: no network calls, but keep the music going from what's already
            // downloaded on-device — prefer more tracks by the same artist first.
            val downloaded = _downloadedTracks.value.filter { it.key != track.key }
            val sameArtist = downloaded.filter {
                it.artist.isNotBlank() && it.artist.equals(track.artist, ignoreCase = true)
            }
            (sameArtist.ifEmpty { downloaded }).map { d ->
                UnifiedTrack.Local(
                    key = d.key,
                    title = d.title,
                    artist = d.artist,
                    thumbnailUrl = d.coverUrl,
                    duration = d.duration,
                    filePath = d.filePath,
                    mediaItem = d.toMediaItem()
                )
            }
        }
    }

    /**
     * Blends whichever sources have already loaded results (Saavn trending, YouTube
     * trending, curated MusiX playlists) into one interleaved "Popular" feed, so no
     * single source dominates a screenful and none of them announce themselves.
     */
    private fun buildPopularFeed(
        saavn: List<MediaItem>,
        youtube: List<YouTubeSearchResult>,
        playlists: List<MusiXServerRepository.CuratedPlaylist>
    ): List<UnifiedTrack> {
        val fromPlaylists = playlists.flatMap { it.tracks }.take(10)
        val lanes = listOf(
            saavn.take(10).map { UnifiedTrack.Saavn(it) },
            youtube.take(10).map { UnifiedTrack.Youtube(it) },
            fromPlaylists.map { UnifiedTrack.Youtube(it) }
        )
        // Round-robin interleave so the feed doesn't read as "one source, then another".
        val result = mutableListOf<UnifiedTrack>()
        val seenKeys = mutableSetOf<String>()
        var i = 0
        while (result.size < 30 && lanes.any { i < it.size }) {
            for (lane in lanes) {
                val item = lane.getOrNull(i) ?: continue
                if (seenKeys.add(item.key)) result.add(item)
            }
            i++
        }
        return result
    }

    /** Play a unified card — dispatches to the right source internally with automatic context queue. */
    fun playUnified(track: UnifiedTrack, contextList: List<UnifiedTrack> = emptyList()) {
        if (contextList.isNotEmpty()) {
            val idx = contextList.indexOfFirst { it.key == track.key }
            if (idx != -1 && idx < contextList.size - 1) {
                val upNext = contextList.subList(idx + 1, contextList.size).mapNotNull {
                    when (it) {
                        is UnifiedTrack.Youtube -> it.result
                        is UnifiedTrack.Saavn -> YouTubeSearchResult(
                            videoId = it.item.id.toString(),
                            title = it.item.name,
                            channelTitle = it.item.artist,
                            thumbnailUrl = it.item.albumArtUri?.toString().orEmpty(),
                            duration = it.item.formattedDuration
                        )
                        else -> null
                    }
                }
                _localQueue.value = emptyList()
                _upNextQueue.value = upNext
            }
        }
        when (track) {
            is UnifiedTrack.Youtube -> playYouTubeVideo(track.result)
            is UnifiedTrack.Saavn -> playSaavnTrack(track.item)
            is UnifiedTrack.Vimeo -> playVimeoVideo(track.video)
            is UnifiedTrack.Local -> playDownloadedTrack(
                com.musicdrop.app.data.repository.DownloadedTrack(
                    key = track.key,
                    title = track.title,
                    artist = track.artist,
                    duration = track.duration,
                    coverUrl = track.thumbnailUrl,
                    filePath = track.filePath,
                    mimeType = "audio/mp4",
                    downloadedAtMs = System.currentTimeMillis()
                )
            )
        }
    }

    /** Download a unified card — dispatches to the right source internally. */
    fun downloadUnified(track: UnifiedTrack, onDone: (Boolean, String) -> Unit) {
        when (track) {
            is UnifiedTrack.Youtube -> downloadYouTubeAudio(track.result, onDone)
            is UnifiedTrack.Saavn -> downloadSaavnTrack(track.item, onDone)
            is UnifiedTrack.Vimeo -> downloadVimeoVideo(track.video, onDone)
            is UnifiedTrack.Local -> onDone(true, track.filePath)
        }
    }

    /** Download unified card as full MP4 video (with audio). */
    fun downloadUnifiedVideo(track: UnifiedTrack, onDone: (Boolean, String) -> Unit) {
        when (track) {
            is UnifiedTrack.Youtube -> downloadYouTubeVideo(track.result, onDone)
            is UnifiedTrack.Vimeo -> downloadVimeoVideo(track.video, onDone)
            else -> downloadUnified(track, onDone)
        }
    }

    private val prefs = application.getSharedPreferences("filedrop_prefs", android.content.Context.MODE_PRIVATE)
    private val _appTheme = MutableStateFlow(
        try {
            // Default to official YouTube Music Dark theme
            val savedName = prefs.getString("app_theme", null)
            if (savedName == null || savedName == "MUSIC_PULSE") {
                com.musicdrop.app.ui.theme.AppThemeMode.YOUTUBE_MUSIC
            } else {
                try {
                    com.musicdrop.app.ui.theme.AppThemeMode.valueOf(savedName)
                } catch (e: Exception) {
                    com.musicdrop.app.ui.theme.AppThemeMode.YOUTUBE_MUSIC
                }
            }
        } catch (e: Exception) {
            com.musicdrop.app.ui.theme.AppThemeMode.YOUTUBE_MUSIC
        }
    )
    val appTheme: StateFlow<com.musicdrop.app.ui.theme.AppThemeMode> = _appTheme.asStateFlow()

    fun setAppTheme(mode: com.musicdrop.app.ui.theme.AppThemeMode) {
        _appTheme.value = mode
        prefs.edit().putString("app_theme", mode.name).apply()

        // Sync player theme and skin with main app theme
        val matchedPlayerTheme = when (mode) {
            com.musicdrop.app.ui.theme.AppThemeMode.YOUTUBE_MUSIC -> com.musicdrop.app.ui.theme.PlayerThemeId.MIDNIGHT_OLED
            com.musicdrop.app.ui.theme.AppThemeMode.MUSIC_PULSE -> com.musicdrop.app.ui.theme.PlayerThemeId.CYBERPUNK_NEON
            com.musicdrop.app.ui.theme.AppThemeMode.MUSIC_ORBIT -> com.musicdrop.app.ui.theme.PlayerThemeId.OCEAN_BLUE
            com.musicdrop.app.ui.theme.AppThemeMode.MUSIC_GREENROOM -> com.musicdrop.app.ui.theme.PlayerThemeId.EMERALD_FOREST
            com.musicdrop.app.ui.theme.AppThemeMode.CYBER_DARK -> com.musicdrop.app.ui.theme.PlayerThemeId.CARBON_SLATE
            com.musicdrop.app.ui.theme.AppThemeMode.CLEAN_LIGHT -> com.musicdrop.app.ui.theme.PlayerThemeId.PURE_FROST
            com.musicdrop.app.ui.theme.AppThemeMode.OLED_BLACK -> com.musicdrop.app.ui.theme.PlayerThemeId.VINYL_MIDNIGHT
            com.musicdrop.app.ui.theme.AppThemeMode.SUNSET_NEBULA -> com.musicdrop.app.ui.theme.PlayerThemeId.RADIAL_SUNSET
            com.musicdrop.app.ui.theme.AppThemeMode.IOS_LIGHT -> com.musicdrop.app.ui.theme.PlayerThemeId.PURE_FROST
            com.musicdrop.app.ui.theme.AppThemeMode.NEARBY_SHARE -> com.musicdrop.app.ui.theme.PlayerThemeId.DYNAMIC_BLUR
            com.musicdrop.app.ui.theme.AppThemeMode.TURBO_CONNECT -> com.musicdrop.app.ui.theme.PlayerThemeId.SUNSET_AMBER
            com.musicdrop.app.ui.theme.AppThemeMode.RETRO -> com.musicdrop.app.ui.theme.PlayerThemeId.VINYL_GOLD
            com.musicdrop.app.ui.theme.AppThemeMode.GLASSMORPHISM -> com.musicdrop.app.ui.theme.PlayerThemeId.PURE_FROST
            com.musicdrop.app.ui.theme.AppThemeMode.ROYAL_PLUM -> com.musicdrop.app.ui.theme.PlayerThemeId.RADIAL_SUNSET
            com.musicdrop.app.ui.theme.AppThemeMode.DEEP_NAVY -> com.musicdrop.app.ui.theme.PlayerThemeId.OCEAN_BLUE
            com.musicdrop.app.ui.theme.AppThemeMode.ROSE_GOLD -> com.musicdrop.app.ui.theme.PlayerThemeId.PURE_FROST
        }
        setPlayerTheme(matchedPlayerTheme)
    }

    fun cycleNextTheme(): com.musicdrop.app.ui.theme.AppThemeMode {
        val modes = com.musicdrop.app.ui.theme.AppThemeMode.values()
        val currentIndex = modes.indexOf(_appTheme.value).let { if (it >= 0) it else 0 }
        val nextMode = modes[(currentIndex + 1) % modes.size]
        setAppTheme(nextMode)
        return nextMode
    }

    private val _cardOpacity = MutableStateFlow(prefs.getFloat("card_opacity", 0.55f))
    val cardOpacity: StateFlow<Float> = _cardOpacity.asStateFlow()

    fun setCardOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.05f, 1.0f)
        _cardOpacity.value = clamped
        prefs.edit().putFloat("card_opacity", clamped).apply()
    }

    private val _themeWallpaperUri = MutableStateFlow<String?>(prefs.getString("theme_wallpaper_uri", null))
    val themeWallpaperUri: StateFlow<String?> = _themeWallpaperUri.asStateFlow()

    fun setThemeWallpaper(uriOrUrl: String?) {
        _themeWallpaperUri.value = uriOrUrl
        if (uriOrUrl != null) {
            prefs.edit().putString("theme_wallpaper_uri", uriOrUrl).apply()
        } else {
            prefs.edit().remove("theme_wallpaper_uri").apply()
        }
    }

    private val _appFontFamily = MutableStateFlow(prefs.getString("app_font_family", "DEFAULT") ?: "DEFAULT")
    val appFontFamily: StateFlow<String> = _appFontFamily.asStateFlow()

    fun setAppFontFamily(family: String) {
        _appFontFamily.value = family
        prefs.edit().putString("app_font_family", family).apply()
    }

    private val _appFontColorOption = MutableStateFlow(prefs.getString("app_font_color_opt", "DEFAULT") ?: "DEFAULT")
    val appFontColorOption: StateFlow<String> = _appFontColorOption.asStateFlow()

    fun setAppFontColorOption(option: String) {
        _appFontColorOption.value = option
        prefs.edit().putString("app_font_color_opt", option).apply()
    }

    private val _isDjCrossfadeEnabled = MutableStateFlow(prefs.getBoolean("dj_crossfade_enabled", true))
    val isDjCrossfadeEnabled: StateFlow<Boolean> = _isDjCrossfadeEnabled.asStateFlow()

    fun setDjCrossfadeEnabled(enabled: Boolean) {
        _isDjCrossfadeEnabled.value = enabled
        prefs.edit().putBoolean("dj_crossfade_enabled", enabled).apply()
        playbackConnection.isDjCrossfadeEnabled = enabled
    }

    private val _isSilenceTrimEnabled = MutableStateFlow(prefs.getBoolean("silence_trim_enabled", true))
    val isSilenceTrimEnabled: StateFlow<Boolean> = _isSilenceTrimEnabled.asStateFlow()

    fun setSilenceTrimEnabled(enabled: Boolean) {
        _isSilenceTrimEnabled.value = enabled
        prefs.edit().putBoolean("silence_trim_enabled", enabled).apply()
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
        loadReceivedFiles()
        loadSentFiles()
        loadInstalledApps()
        loadMusiXServer()
        loadCharts()
        loadRecentUnified()

        playbackConnection.isDjCrossfadeEnabled = _isDjCrossfadeEnabled.value

        // "Sometimes plays, sometimes doesn't": a stream URL (mainly YouTube's signed
        // googlevideo.com links) can die mid-session — e.g. a wifi/mobile-data IP
        // change — even though extraction succeeded a minute earlier. Re-extract and
        // replay instead of retrying the same dead URL; give up gracefully if that
        // still doesn't work rather than failing silently.
        playbackConnection.onNeedsFreshStream = { currentPlaybackRetry?.invoke() }
        playbackConnection.onPlaybackEnded = { autoplayNext() }
        playbackConnection.onPreloadNextTrack = { preloadNextTrack() }
        playbackConnection.onOverlapNextTrack = null
        playbackConnection.onSkipPreviousAction = { playPreviousTrack() }
        playbackConnection.onSkipNextAction = { playNextTrackFromQueue() }
        playbackConnection.onPlaybackFailed = {
            if (!networkMonitor.isCurrentlyConnected()) {
                fallbackToOfflinePlayback()
            } else {
                playNextTrackFromQueue()
            }
        }

        // Real-time network monitor for offline/online banners and seamless local fallback
        viewModelScope.launch {
            val initialOnline = networkMonitor.isCurrentlyConnected()
            if (!initialOnline) {
                _networkStatusBanner.value = "You're offline — playing from your downloaded library"
            }
            var wasPreviouslyOnline = initialOnline
            networkMonitor.isOnline.collect { online ->
                if (!online && wasPreviouslyOnline) {
                    _networkStatusBanner.value = "You're offline — playing from your downloaded library"
                    if (!playbackConnection.isPlaying.value) {
                        fallbackToOfflinePlayback()
                    }
                } else if (online && !wasPreviouslyOnline) {
                    _networkStatusBanner.value = "You're back online! Search & online streaming restored"
                    launch {
                        delay(4500)
                        if (_networkStatusBanner.value?.contains("back online", ignoreCase = true) == true) {
                            _networkStatusBanner.value = null
                        }
                    }
                }
                wasPreviouslyOnline = online
            }
        }

        // Let the lock screen / notification / Bluetooth "next" & "previous" controls
        // (handled inside FileDropMediaService, same process) drive the exact same
        // queue logic as the in-app buttons, instead of only seeing ExoPlayer's own
        // often-empty single-item playlist. See PlaybackQueueBridge for why this is
        // needed. Always report true: playNextTrackFromQueue()/playPreviousTrack()
        // already fall back gracefully (autoplay a same-artist/similar recommendation,
        // or just seek to 0) when there's nothing explicitly queued, so "Next" on the
        // lock screen should always try to keep the music going rather than silently
        // doing nothing just because no queue happens to be populated right now.
        com.musicdrop.app.playback.PlaybackQueueBridge.hasNext = { true }
        com.musicdrop.app.playback.PlaybackQueueBridge.hasPrevious = { true }
        com.musicdrop.app.playback.PlaybackQueueBridge.onNext = { playNextTrackFromQueue() }
        com.musicdrop.app.playback.PlaybackQueueBridge.onPrevious = { playPreviousTrack() }

        // Keep the displayed "up next" local queue in sync when ExoPlayer advances
        // between downloaded tracks on its own (native playlist auto-advance / lock
        // screen "next"), not just when the in-app skip button drives it manually.
        viewModelScope.launch {
            playbackConnection.currentTrack.collect { current ->
                if (current == null) return@collect
                val queue = _localQueue.value
                if (queue.isEmpty()) return@collect
                val idx = queue.indexOfFirst { it.toMediaItem().id == current.id }
                if (idx >= 0) {
                    _localQueue.value = queue.subList(idx + 1, queue.size)
                }
                if (_isVideoMode.value) {
                    resolveVideoForCurrentTrack()
                }
            }
        }

        // Recompute the blended "Popular" feed whenever any contributing source updates.
        viewModelScope.launch {
            combine(saavnTrending, ytMusicResults, curatedPlaylists) { saavn, youtube, playlists ->
                buildPopularFeed(saavn, youtube, playlists)
            }.collect { _popularUnified.value = it }
        }
    }

    fun loadInstalledApps(includeSystem: Boolean = true) {
        viewModelScope.launch {
            val apps = appsRepository.getInstalledUserApps(includeSystemApps = includeSystem)
            _installedApps.value = apps
        }
    }

    fun prepareAppsForTransfer(apps: List<InstalledAppInfo>, onReady: (List<MediaItem>) -> Unit) {
        viewModelScope.launch {
            val items = appsRepository.prepareAppsForTransfer(apps)
            onReady(items)
        }
    }

    fun loadReceivedFiles() {
        viewModelScope.launch {
            val rec = mediaRepository.getReceivedFiles()
            _receivedFiles.value = rec
        }
    }

    fun loadSentFiles() {
        viewModelScope.launch {
            _sentFiles.value = SentFilesLog.getAllRecords(getApplication())
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            // Fast phase 1: Instant load of first 50 items for instant UI
            launch {
                val stats = storageRepository.getStorageStats()
                _storageStats.value = stats
            }

            launch {
                val recents = mediaRepository.getRecentMedia(limit = 50)
                _recentMedia.value = recents
            }

            // Phase 2: Category lists
            launch {
                val p = mediaRepository.getPhotos(limit = 300)
                _photos.value = p
            }

            launch {
                val v = mediaRepository.getVideos(limit = 1000)
                _videos.value = v
            }

            launch {
                val a = mediaRepository.getAudioTracks(limit = 5000)
                _allAudio.value = a
                _songs.value = a.filter { it.isSong }
                _voiceNotes.value = a.filter { !it.isSong }
            }

            launch {
                val d = mediaRepository.getDocuments(limit = 500)
                _documents.value = d
            }

            _isLoading.value = false
        }
    }

    fun getDeleteIntentSender(items: List<MediaItem>): android.content.IntentSender? {
        return mediaRepository.getDeleteIntentSender(items)
    }

    fun deleteMediaItems(items: List<MediaItem>, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = mediaRepository.deleteMediaItems(items)
            loadData()
            loadReceivedFiles()
            onComplete(success)
        }
    }

    fun setAudioFilter(filter: AudioFilter) {
        _audioFilter.value = filter
    }

    fun playTrack(track: MediaItem, customList: List<MediaItem>? = null) {
        val isVideo = track.mediaType == MediaType.VIDEO ||
                track.mimeType.startsWith("video") ||
                track.filePath?.endsWith(".mp4", ignoreCase = true) == true ||
                track.filePath?.endsWith(".mkv", ignoreCase = true) == true ||
                track.filePath?.endsWith(".webm", ignoreCase = true) == true
        _isVideoMode.value = isVideo
        if (!isVideo) {
            _ytCurrentVideo.value = null
        }

        val currentAudioList = customList ?: when (_audioFilter.value) {
            AudioFilter.ALL -> _allAudio.value
            AudioFilter.SONGS -> _songs.value
            AudioFilter.VOICE_NOTES -> _voiceNotes.value
        }
        val fullList = currentAudioList.ifEmpty { listOf(track) }
        recordRecentPlay(UnifiedTrack.Local(
            key = "local:${track.id}",
            title = track.name,
            artist = track.artist,
            thumbnailUrl = track.albumArtUri?.toString().orEmpty(),
            duration = track.formattedDuration,
            filePath = track.filePath.orEmpty(),
            mediaItem = track
        ))
        currentPlaybackRetry = { playTrack(track, customList) }
        playbackConnection.playTrack(track, fullList)
        openFullPlayer()
    }

    fun shareItem(item: MediaItem) {
        ShareHelper.shareSingle(getApplication(), item)
    }

    fun shareMultiple(items: List<MediaItem>) {
        ShareHelper.shareMultiple(getApplication(), items)
    }

    // ── Offline Downloads Management ─────────────────────────────────────────
    fun playDownloadedTrack(
        track: com.musicdrop.app.data.repository.DownloadedTrack,
        contextList: List<com.musicdrop.app.data.repository.DownloadedTrack> = _downloadedTracks.value
    ) {
        val idx = contextList.indexOfFirst { it.key == track.key }
        if (idx != -1 && idx < contextList.size - 1) {
            _localQueue.value = contextList.subList(idx + 1, contextList.size)
        } else {
            _localQueue.value = emptyList()
        }
        _upNextQueue.value = emptyList()
        val isVideo = track.mimeType.startsWith("video") || track.filePath.endsWith(".mp4", ignoreCase = true) || track.key.startsWith("yt_video:")
        _isVideoMode.value = isVideo
        if (!isVideo) {
            _ytCurrentVideo.value = null
        }

        val mediaItem = track.toMediaItem()
        currentPlaybackRetry = { playDownloadedTrack(track, contextList) }
        recordRecentPlay(UnifiedTrack.Local(
            key = track.key,
            title = track.title,
            artist = track.artist,
            thumbnailUrl = track.coverUrl,
            duration = track.duration,
            filePath = track.filePath,
            mediaItem = mediaItem
        ))
        // Feed the WHOLE local queue into ExoPlayer (not just this one track) so the
        // player's own next/previous state is real. This is what makes lock screen /
        // notification / Bluetooth "next" actually skip to the next downloaded song,
        // and lets ExoPlayer auto-advance between tracks on its own when one ends —
        // instead of a single-item playlist that always reports "no next track".
        val queueMediaItems = contextList.map { it.toMediaItem() }
        playbackConnection.playTrack(mediaItem, queueMediaItems.ifEmpty { listOf(mediaItem) })
        openFullPlayer()
    }

    fun deleteDownloadedTrack(track: com.musicdrop.app.data.repository.DownloadedTrack) {
        com.musicdrop.app.data.repository.DownloadedTracksStore.deleteFileAndRecord(getApplication(), track)
        _downloadedTracks.value = com.musicdrop.app.data.repository.DownloadedTracksStore.getAll(getApplication())
    }

    fun syncLocalDownloadedFiles() {
        val handler = CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e("MainViewModel", "Error in syncLocalDownloadedFiles", throwable)
        }
        viewModelScope.launch(Dispatchers.IO + handler) {
            try {
                val app = getApplication<Application>()
                val known = try {
                    com.musicdrop.app.data.repository.DownloadedTracksStore.getAll(app).toMutableList()
                } catch (_: Throwable) {
                    mutableListOf()
                }
                val existingPaths = known.map { it.filePath }.toSet()

                val dirs = mutableListOf<java.io.File>()
                try {
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)?.let {
                        dirs.add(it)
                        dirs.add(java.io.File(it, "MusicDrop"))
                    }
                } catch (_: Throwable) {}
                try {
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)?.let {
                        dirs.add(it)
                        dirs.add(java.io.File(it, "MusicDrop"))
                    }
                } catch (_: Throwable) {}
                try {
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)?.let {
                        dirs.add(it)
                        dirs.add(java.io.File(it, "MusicDrop"))
                        dirs.add(java.io.File(it, "FileDrop"))
                    }
                } catch (_: Throwable) {}
                try { app.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)?.let { dirs.add(it) } } catch (_: Throwable) {}
                try { app.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)?.let { dirs.add(it) } } catch (_: Throwable) {}
                try { app.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)?.let { dirs.add(it) } } catch (_: Throwable) {}
                try { dirs.add(java.io.File(app.filesDir, "Music")) } catch (_: Throwable) {}
                try { dirs.add(java.io.File(app.filesDir, "Movies")) } catch (_: Throwable) {}

                val audioExts = setOf("mp3", "m4a", "opus", "wav", "flac", "aac", "ogg", "wma")
                val videoExts = setOf("mp4", "mkv", "webm", "3gp", "mov", "avi")

                for (dir in dirs) {
                    try {
                        if (dir.exists() && dir.isDirectory) {
                            val files = dir.listFiles { f ->
                                if (!f.isFile || f.name.startsWith(".")) return@listFiles false
                                val ext = f.extension.lowercase()
                                ext in audioExts || ext in videoExts
                            } ?: emptyArray()

                            for (file in files) {
                                if (file.absolutePath !in existingPaths && file.length() > 0) {
                                    val cleanName = file.nameWithoutExtension
                                    val ext = file.extension.lowercase()
                                    val isVideo = ext in videoExts

                                    val isVoiceNote = !isVideo && (
                                        cleanName.matches(Regex("""^20\d{6}_\d{6}.*""")) ||
                                        cleanName.startsWith("PTT-", ignoreCase = true) ||
                                        cleanName.startsWith("AUD-", ignoreCase = true) ||
                                        cleanName.startsWith("REC_", ignoreCase = true) ||
                                        cleanName.startsWith("Record", ignoreCase = true) ||
                                        cleanName.startsWith("Voice", ignoreCase = true) ||
                                        cleanName.startsWith("Call", ignoreCase = true) ||
                                        (file.length() < 150_000L && !file.parentFile?.name.equals("MusicDrop", ignoreCase = true))
                                    )

                                    if (isVoiceNote) {
                                        continue
                                    }

                                    val mimeType = when (ext) {
                                        "mp4" -> "video/mp4"
                                        "mkv" -> "video/x-matroska"
                                        "webm" -> "video/webm"
                                        "mp3" -> "audio/mpeg"
                                        "m4a", "aac" -> "audio/mp4"
                                        "opus" -> "audio/opus"
                                        "wav" -> "audio/wav"
                                        "flac" -> "audio/flac"
                                        else -> if (isVideo) "video/*" else "audio/*"
                                    }
                                    val track = com.musicdrop.app.data.repository.DownloadedTrack(
                                        key = if (isVideo) "video:${file.absolutePath.hashCode()}" else "file:${file.absolutePath.hashCode()}",
                                        title = cleanName,
                                        artist = if (isVideo) "Device Video" else "Downloaded Track",
                                        duration = "",
                                        coverUrl = "",
                                        filePath = file.absolutePath,
                                        mimeType = mimeType,
                                        downloadedAtMs = file.lastModified()
                                    )
                                    com.musicdrop.app.data.repository.DownloadedTracksStore.add(app, track)
                                    // Trigger MediaScanner so MediaStore indices it immediately
                                    try {
                                        android.media.MediaScannerConnection.scanFile(app, arrayOf(file.absolutePath), arrayOf(mimeType), null)
                                    } catch (_: Throwable) {}
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                }
                _downloadedTracks.value = com.musicdrop.app.data.repository.DownloadedTracksStore.getAll(app)
            } catch (_: Throwable) {}
        }
    }

    // ── Liked Music System ───────────────────────────────────────────────────
    fun isCurrentTrackLiked(): Boolean {
        val current = playbackConnection.currentTrack.value ?: return false
        val path = current.filePath.orEmpty()
        val key = if (path.isNotBlank()) path else current.id.toString()
        val formattedKey = if (!key.contains(":")) "yt:$key" else key
        return com.musicdrop.app.data.repository.LikedMusicStore.isLiked(getApplication(), formattedKey) ||
                _likedMusic.value.any { it.title.equals(current.name.orEmpty(), ignoreCase = true) }
    }

    fun isTrackLiked(key: String): Boolean {
        return _likedMusic.value.any { it.key == key }
    }

    fun toggleLikeUnifiedTrack(track: UnifiedTrack): Boolean {
        val item = com.musicdrop.app.data.repository.LikedMusicItem(
            key = track.key,
            title = track.title,
            artist = track.artist,
            coverUrl = track.thumbnailUrl,
            duration = track.duration,
            sourceName = track.sourceName,
            likedAtMs = System.currentTimeMillis()
        )
        val isNowLiked = com.musicdrop.app.data.repository.LikedMusicStore.toggleLike(getApplication(), item)
        _likedMusic.value = com.musicdrop.app.data.repository.LikedMusicStore.getAll(getApplication())
        loadTasteRecommendations()
        return isNowLiked
    }

    fun toggleLikeCurrentTrack(): Boolean {
        val current = playbackConnection.currentTrack.value ?: return false
        val path = current.filePath.orEmpty()
        val key = if (path.isNotBlank()) path else current.id.toString()
        val formattedKey = if (!key.contains(":")) "yt:$key" else key
        val item = com.musicdrop.app.data.repository.LikedMusicItem(
            key = formattedKey,
            title = current.name.orEmpty(),
            artist = current.artist.orEmpty(),
            coverUrl = current.albumArtUri?.toString().orEmpty(),
            duration = current.formattedDuration,
            sourceName = "YouTube",
            likedAtMs = System.currentTimeMillis()
        )
        val isNowLiked = com.musicdrop.app.data.repository.LikedMusicStore.toggleLike(getApplication(), item)
        _likedMusic.value = com.musicdrop.app.data.repository.LikedMusicStore.getAll(getApplication())
        loadTasteRecommendations()
        return isNowLiked
    }

    fun playLikedItem(
        item: com.musicdrop.app.data.repository.LikedMusicItem,
        contextList: List<com.musicdrop.app.data.repository.LikedMusicItem> = _likedMusic.value
    ) {
        val idx = contextList.indexOfFirst { it.key == item.key }
        val upNext = if (idx != -1 && idx < contextList.size - 1) {
            contextList.subList(idx + 1, contextList.size).map {
                val videoId = it.key.removePrefix("yt:").removePrefix("sv:").removePrefix("vm:")
                YouTubeSearchResult(
                    videoId = videoId,
                    title = it.title,
                    channelTitle = it.artist,
                    thumbnailUrl = it.coverUrl,
                    duration = it.duration
                )
            }
        } else {
            emptyList()
        }
        _localQueue.value = emptyList()
        _upNextQueue.value = upNext

        val videoId = item.key.removePrefix("yt:").removePrefix("sv:").removePrefix("vm:")
        val ytResult = YouTubeSearchResult(
            videoId = videoId,
            title = item.title,
            channelTitle = item.artist,
            thumbnailUrl = item.coverUrl,
            duration = item.duration
        )
        playYouTubeVideo(ytResult)
    }

    fun playAllLikedMusic() {
        val list = _likedMusic.value
        if (list.isEmpty()) return
        playLikedItem(list.first(), list)
    }

    fun toggleSaveArtist(artist: com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist): Boolean {
        val item = com.musicdrop.app.data.repository.SavedArtistItem(
            browseId = artist.browseId,
            name = artist.title,
            thumbnailUrl = artist.thumbnailUrl,
            subscribers = artist.subscribers
        )
        val res = com.musicdrop.app.data.repository.SavedMediaStore.toggleSaveArtist(getApplication(), item)
        _savedArtists.value = com.musicdrop.app.data.repository.SavedMediaStore.getSavedArtists(getApplication())
        return res
    }

    fun toggleSaveAlbum(album: com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem): Boolean {
        val item = com.musicdrop.app.data.repository.SavedAlbumItem(
            browseId = album.browseId.orEmpty(),
            title = album.title,
            artist = album.type.orEmpty(),
            thumbnailUrl = album.thumbnailUrl.orEmpty(),
            year = album.year.orEmpty(),
            type = album.type.orEmpty()
        )
        val res = com.musicdrop.app.data.repository.SavedMediaStore.toggleSaveAlbum(getApplication(), item)
        _savedAlbums.value = com.musicdrop.app.data.repository.SavedMediaStore.getSavedAlbums(getApplication())
        return res
    }

    fun isArtistSaved(browseId: String): Boolean {
        return _savedArtists.value.any { it.browseId == browseId }
    }

    fun isAlbumSaved(browseId: String): Boolean {
        return _savedAlbums.value.any { it.browseId == browseId }
    }

    // ── Custom User Playlists ────────────────────────────────────────────────
    fun createPlaylist(name: String, description: String = ""): com.musicdrop.app.data.repository.UserPlaylistItem {
        val created = com.musicdrop.app.data.repository.UserPlaylistsStore.create(getApplication(), name, description)
        _userPlaylists.value = com.musicdrop.app.data.repository.UserPlaylistsStore.getAll(getApplication())
        return created
    }

    fun deletePlaylist(id: String) {
        com.musicdrop.app.data.repository.UserPlaylistsStore.delete(getApplication(), id)
        _userPlaylists.value = com.musicdrop.app.data.repository.UserPlaylistsStore.getAll(getApplication())
    }

    fun addTrackToPlaylist(playlistId: String, track: UnifiedTrack) {
        val item = com.musicdrop.app.data.repository.LikedMusicItem(
            key = track.key,
            title = track.title,
            artist = track.artist,
            coverUrl = track.thumbnailUrl,
            duration = track.duration,
            sourceName = track.sourceName,
            likedAtMs = System.currentTimeMillis()
        )
        com.musicdrop.app.data.repository.UserPlaylistsStore.addTrack(getApplication(), playlistId, item)
        _userPlaylists.value = com.musicdrop.app.data.repository.UserPlaylistsStore.getAll(getApplication())
    }

    fun removeTrackFromPlaylist(playlistId: String, trackKey: String) {
        com.musicdrop.app.data.repository.UserPlaylistsStore.removeTrack(getApplication(), playlistId, trackKey)
        _userPlaylists.value = com.musicdrop.app.data.repository.UserPlaylistsStore.getAll(getApplication())
    }

    fun playUserPlaylist(playlist: com.musicdrop.app.data.repository.UserPlaylistItem, startIndex: Int = 0) {
        val list = playlist.tracks
        if (list.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, list.size - 1)
        playLikedItem(list[safeIndex], list)
    }

    // ── Explore Feed ─────────────────────────────────────────────────────────
    fun loadExploreData(force: Boolean = false) {
        if (!force && (_exploreNewReleases.value.isNotEmpty() || _exploreLoading.value)) return
        viewModelScope.launch {
            _exploreLoading.value = true
            try {
                val releases = com.musicdrop.app.data.repository.YtMusicApiRepository.getNewReleases(force = force)
                if (releases.isNotEmpty()) {
                    _exploreNewReleases.value = releases
                } else {
                    val charts = YouTubeChartsRepository.getCharts("IN")
                    val results = if (charts.trending.isNotEmpty()) charts.trending else charts.topSongs
                    if (results.isNotEmpty()) {
                        _exploreNewReleases.value = results
                    } else {
                        val fallback = YouTubeMusicRepository.search("new releases", filter = YouTubeMusicRepository.SearchFilter.ALBUMS)
                        _exploreNewReleases.value = fallback.results
                    }
                }
            } catch (e: Exception) {
                try {
                    val fallback = YouTubeMusicRepository.search("top music", filter = YouTubeMusicRepository.SearchFilter.SONGS)
                    _exploreNewReleases.value = fallback.results
                } catch (_: Exception) {}
            }
            _exploreLoading.value = false
        }
    }

    fun addUnifiedTrackToPlaylist(playlistId: String, track: UnifiedTrack) {
        addTrackToPlaylist(playlistId, track)
    }

    // ── Search History / Recent Searches ──────────────────────────────────────
    private val _recentSearches = MutableStateFlow<List<String>>(
        com.musicdrop.app.data.repository.RecentSearchStore.getSearches(application)
    )
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    fun addRecentSearch(query: String) {
        com.musicdrop.app.data.repository.RecentSearchStore.addSearch(getApplication(), query)
        _recentSearches.value = com.musicdrop.app.data.repository.RecentSearchStore.getSearches(getApplication())
    }

    fun removeRecentSearch(query: String) {
        com.musicdrop.app.data.repository.RecentSearchStore.removeSearch(getApplication(), query)
        _recentSearches.value = com.musicdrop.app.data.repository.RecentSearchStore.getSearches(getApplication())
    }

    fun clearRecentSearches() {
        com.musicdrop.app.data.repository.RecentSearchStore.clearAll(getApplication())
        _recentSearches.value = com.musicdrop.app.data.repository.RecentSearchStore.getSearches(getApplication())
    }

    fun getStreamCacheSizeBytes(): Long {
        var size = 0L
        try {
            val app = getApplication<Application>()
            size += getFolderSizeBytes(app.cacheDir)
            val ext = app.externalCacheDir
            if (ext != null) {
                size += getFolderSizeBytes(ext)
            }
        } catch (_: Throwable) {}
        return size
    }

    fun getFormattedCacheSize(): String {
        val size = getStreamCacheSizeBytes()
        val mb = size / (1024.0 * 1024.0)
        val kb = size / 1024.0
        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.0f KB", kb)
            else -> "$size B"
        }
    }

    fun clearAppStreamCache(onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                streamCache.clear()
                com.musicdrop.app.data.youtube.StreamCacheStore.save(getApplication(), streamCache)
                com.musicdrop.app.data.repository.YtMusicApiRepository.clearCache()
                regionTrendingCache.clear()

                val app = getApplication<Application>()
                app.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                app.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            } catch (_: Throwable) {}
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    private fun getFolderSizeBytes(dir: java.io.File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        try {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) getFolderSizeBytes(file) else file.length()
            }
        } catch (_: Throwable) {}
        return size
    }

    override fun onCleared() {
        super.onCleared()
        playbackConnection.release()
        poTokenManager.release()
    }
}

