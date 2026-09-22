package com.musicdrop.tv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicdrop.tv.data.TvCategory
import com.musicdrop.tv.data.TvVideoItem
import com.musicdrop.tv.data.TvYtRepository
import com.musicdrop.tv.download.TvDownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TvShelf(
    val title: String,
    val emoji: String,
    val videos: List<TvVideoItem>
)

class TvViewModel(application: Application) : AndroidViewModel(application) {

    val downloadManager = TvDownloadManager.getInstance(application)

    // Current playing video overlay
    private val _currentVideo = MutableStateFlow<TvVideoItem?>(null)
    val currentVideo: StateFlow<TvVideoItem?> = _currentVideo.asStateFlow()

    private val _upNextVideos = MutableStateFlow<List<TvVideoItem>>(emptyList())
    val upNextVideos: StateFlow<List<TvVideoItem>> = _upNextVideos.asStateFlow()

    // Home Shelves
    private val _homeShelves = MutableStateFlow<List<TvShelf>>(emptyList())
    val homeShelves: StateFlow<List<TvShelf>> = _homeShelves.asStateFlow()

    private val _isLoadingHome = MutableStateFlow(true)
    val isLoadingHome: StateFlow<Boolean> = _isLoadingHome.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TvVideoItem>>(emptyList())
    val searchResults: StateFlow<List<TvVideoItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Browse / Mixes
    private val _activeGenre = MutableStateFlow("Bollywood Hits")
    val activeGenre: StateFlow<String> = _activeGenre.asStateFlow()

    private val _genreVideos = MutableStateFlow<List<TvVideoItem>>(emptyList())
    val genreVideos: StateFlow<List<TvVideoItem>> = _genreVideos.asStateFlow()

    private val _isLoadingGenre = MutableStateFlow(false)
    val isLoadingGenre: StateFlow<Boolean> = _isLoadingGenre.asStateFlow()

    init {
        loadHomeFeed()
        loadGenre("bollywood hits 2026")
    }

    fun playVideo(video: TvVideoItem) {
        _currentVideo.value = video
        // Populate up next from current search or home shelf
        val currentQueue = _searchResults.value.ifEmpty {
            _homeShelves.value.flatMap { it.videos }
        }.filter { it.id != video.id }
        _upNextVideos.value = currentQueue.take(15)

        // Asynchronously fetch related videos for the video
        viewModelScope.launch {
            val related = TvYtRepository.searchVideos("${video.title} ${video.channelTitle}")
            val filtered = related.filter { it.id != video.id }
            if (filtered.isNotEmpty()) {
                _upNextVideos.value = filtered.take(15)
            }
        }
    }

    fun closePlayer() {
        _currentVideo.value = null
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            _isLoadingHome.value = true
            val shelves = mutableListOf<TvShelf>()

            val trending = TvYtRepository.searchVideos("trending music songs 2026")
            if (trending.isNotEmpty()) {
                shelves.add(TvShelf("🔥 Trending Videos", "🔥", trending))
            }

            val bollywood = TvYtRepository.searchVideos("bollywood latest songs 2026")
            if (bollywood.isNotEmpty()) {
                shelves.add(TvShelf("🎬 Bollywood Blockbusters", "🎬", bollywood))
            }

            val punjabi = TvYtRepository.searchVideos("punjabi hits 2026")
            if (punjabi.isNotEmpty()) {
                shelves.add(TvShelf("🎤 Punjabi Hits", "🎤", punjabi))
            }

            val south = TvYtRepository.searchVideos("tamil malayalam telugu hits 2026")
            if (south.isNotEmpty()) {
                shelves.add(TvShelf("🌴 South Indian Hits", "🌴", south))
            }

            val lofi = TvYtRepository.searchVideos("lofi chill music 2026")
            if (lofi.isNotEmpty()) {
                shelves.add(TvShelf("🌙 Lofi Chill & Sleep", "🌙", lofi))
            }

            _homeShelves.value = shelves
            _isLoadingHome.value = false
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = TvYtRepository.searchVideos(query)
            _isSearching.value = false
        }
    }

    fun loadGenre(query: String, label: String = "") {
        if (label.isNotBlank()) _activeGenre.value = label
        viewModelScope.launch {
            _isLoadingGenre.value = true
            _genreVideos.value = TvYtRepository.searchVideos(query)
            _isLoadingGenre.value = false
        }
    }
}
