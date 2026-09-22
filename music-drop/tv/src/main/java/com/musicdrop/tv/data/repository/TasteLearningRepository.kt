package com.musicdrop.tv.data.repository

import android.content.Context
import com.musicdrop.tv.data.model.UnifiedTrack
import com.musicdrop.tv.data.youtube.YouTubeSearchOutcome
import com.musicdrop.tv.data.youtube.YouTubeSearchRepository
import com.musicdrop.tv.data.youtube.YouTubeSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * On-device Music Taste Learning Engine.
 * Analyzes listening frequency, recent plays, and liked tracks to discover the user's
 * preferred artists, genres, and styles, then queries YouTube to dynamically recommend
 * personalized songs tailored to their taste.
 */
object TasteLearningRepository {

    data class TasteProfile(
        val topArtists: List<String>,
        val topGenres: List<String>,
        val recommendationsTitle: String,
        val tracks: List<UnifiedTrack>
    )

    private val GENRE_KEYWORDS = listOf(
        "Acoustic", "Lo-Fi", "Lofi", "Remix", "Punjabi", "Tamil", "Telugu",
        "Malayalam", "Hindi", "Bollywood", "Hip Hop", "Pop", "Rock", "EDM",
        "Dance", "Indie", "Sufi", "Ghazal", "R&B", "Chill", "Romantic", "Party"
    )

    private val IGNORED_ARTISTS = setOf(
        "youtube music", "music drop", "unknown", "various artists", "", "topic", "official"
    )

    /**
     * Discovers user taste from recent plays & liked tracks and queries YouTube for
     * personalized recommendations.
     */
    suspend fun getPersonalizedRecommendations(context: Context): TasteProfile = withContext(Dispatchers.IO) {
        val recentTracks = try { RecentPlaysStore.getAll(context) } catch (_: Exception) { emptyList() }
        val likedTracks = try { LikedMusicStore.getAll(context) } catch (_: Exception) { emptyList() }

        val artistScores = mutableMapOf<String, Int>()
        val genreScores = mutableMapOf<String, Int>()
        val existingKeys = mutableSetOf<String>()

        // Weight liked songs 3x
        for (track in likedTracks) {
            existingKeys.add(track.key)
            val artist = cleanArtistName(track.artist)
            if (artist.isNotBlank() && artist.lowercase() !in IGNORED_ARTISTS) {
                artistScores[artist] = (artistScores[artist] ?: 0) + 3
            }
            extractGenreKeywords(track.title, genreScores)
        }

        // Weight recent plays 2x
        for (track in recentTracks) {
            existingKeys.add(track.key)
            val artist = cleanArtistName(track.artist)
            if (artist.isNotBlank() && artist.lowercase() !in IGNORED_ARTISTS) {
                artistScores[artist] = (artistScores[artist] ?: 0) + 2
            }
            extractGenreKeywords(track.title, genreScores)
        }

        val topArtists = artistScores.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        val topGenres = genreScores.entries
            .sortedByDescending { it.value }
            .take(2)
            .map { it.key }

        // Construct targeted recommendation queries based on learned taste
        val queries = mutableListOf<String>()
        for (artist in topArtists) {
            queries.add("$artist top hits songs")
            queries.add("$artist mix radio")
        }
        for (genre in topGenres) {
            queries.add("$genre trending hits songs")
        }

        // Fallback queries if the user has minimal or no listening history yet
        if (queries.isEmpty()) {
            queries.add("trending songs 2026")
            queries.add("global top hits")
        }

        val collectedResults = mutableListOf<YouTubeSearchResult>()
        coroutineScope {
            val deferred = queries.take(4).map { q ->
                async {
                    try {
                        when (val outcome = YouTubeSearchRepository.search(q, maxResults = 12)) {
                            is YouTubeSearchOutcome.Success -> outcome.results
                            is YouTubeSearchOutcome.Error -> emptyList()
                        }
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
            val results = deferred.awaitAll()
            for (list in results) {
                collectedResults.addAll(list)
            }
        }

        // Deduplicate and prioritize unplayed songs
        val seenIds = mutableSetOf<String>()
        val filtered = collectedResults.filter { item ->
            val id = item.videoId
            if (id.isBlank() || id in seenIds) return@filter false
            seenIds.add(id)
            true
        }

        val unifiedList = filtered
            .shuffled()
            .take(20)
            .map { UnifiedTrack.Youtube(it) }

        val title = when {
            topArtists.size >= 2 -> "Made For You • ${topArtists[0]} & ${topArtists[1]}"
            topArtists.size == 1 -> "Made For You • ${topArtists[0]} Mix"
            topGenres.isNotEmpty() -> "Made For You • ${topGenres[0]} Taste"
            else -> "Made For You • Daily Mix"
        }

        TasteProfile(
            topArtists = topArtists,
            topGenres = topGenres,
            recommendationsTitle = title,
            tracks = unifiedList
        )
    }

    private fun cleanArtistName(raw: String): String {
        return raw
            .replace(" - Topic", "", ignoreCase = true)
            .replace("Official", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .trim()
    }

    private fun extractGenreKeywords(text: String, genreScores: MutableMap<String, Int>) {
        val lower = text.lowercase()
        for (keyword in GENRE_KEYWORDS) {
            if (lower.contains(keyword.lowercase())) {
                genreScores[keyword] = (genreScores[keyword] ?: 0) + 1
            }
        }
    }
}
