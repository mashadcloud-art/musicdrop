package com.musicdrop.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.data.repository.MusiXServerRepository
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.R
import com.musicdrop.app.ui.components.AddToPlaylistDialog
import com.musicdrop.app.ui.components.ShortsFullScreenPlayer
import com.musicdrop.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

data class MusicCardItem(
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val query: String
)

data class MoodChip(
    val icon: ImageVector,
    val label: String,
    val tint: Color
)

data class FeaturedMusicDrop(
    val badge: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val gradient: List<Color>,
    val actionType: DropActionType,
    val targetId: String = "",
    val targetName: String = "",
    val searchQuery: String = ""
)

enum class DropActionType {
    ARTIST,
    ALBUM,
    SONG,
    SEARCH
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

private data class MoodShelfData(
    val shelf1: String,
    val shelf2: String,
    val tracks1: List<UnifiedTrack>,
    val tracks2: List<UnifiedTrack>
)

@Composable
fun YouTubeShelfHeader(
    title: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    avatarInitial: String? = null,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onSeeAll != null) { onSeeAll?.invoke() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(10.dp))
        } else if (!avatarInitial.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(appColors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitial,
                    color = appColors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle.uppercase(),
                    color = appColors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = title,
                color = appColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = "More",
            tint = appColors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: MainViewModel,
    onOpenSearchWithQuery: (String) -> Unit,
    onOpenPlaylist: (MusiXServerRepository.CuratedPlaylist) -> Unit = {},
    onOpenSource: (MusicSource) -> Unit = {},
    onOpenArtist: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist) -> Unit = {},
    onOpenAlbum: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        viewModel.refreshAllDashboardCategories(force = true)
        viewModel.syncLocalDownloadedFiles()
    }

    val context = LocalContext.current
    val saavnTrending by viewModel.saavnTrending.collectAsState()
    val isRefreshingDashboard by viewModel.isRefreshingDashboard.collectAsState()
    val madeForYouRecommendations by viewModel.madeForYouRecommendations.collectAsState()
    val madeForYouTitle by viewModel.madeForYouTitle.collectAsState()
    val popular by viewModel.popularUnified.collectAsState()
    val recent by viewModel.recentUnified.collectAsState()
    val likedMusic by viewModel.likedMusic.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val curatedPlaylists by viewModel.curatedPlaylists.collectAsState()
    val preparingKey by viewModel.preparingKey.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val chartsArtists by viewModel.chartsArtists.collectAsState()
    val chartsDaily by viewModel.chartsDaily.collectAsState()
    val chartsWeekly by viewModel.chartsWeekly.collectAsState()
    val chartsGenres by viewModel.chartsGenres.collectAsState()
    val exploreNewReleases by viewModel.exploreNewReleases.collectAsState()
    val musicFeed by viewModel.musicFeed.collectAsState()
    val ytMusicResults by viewModel.ytMusicResults.collectAsState()
    val southIndiaTrending by viewModel.southIndiaTrending.collectAsState()
    val regionTrendingSongs by viewModel.regionTrendingSongs.collectAsState()
    val regionTrendingLoading by viewModel.regionTrendingLoading.collectAsState()
    val indiaQuickPicks by viewModel.indiaQuickPicks.collectAsState()
    val pakistanQuickPicks by viewModel.pakistanQuickPicks.collectAsState()
    val malayalamQuickPicks by viewModel.malayalamQuickPicks.collectAsState()
    val tamilQuickPicks by viewModel.tamilQuickPicks.collectAsState()
    val teluguQuickPicks by viewModel.teluguQuickPicks.collectAsState()
    val downloadTargetTrack = remember { mutableStateOf<UnifiedTrack?>(null) }
    val coverQuickPicks by viewModel.coverQuickPicks.collectAsState()
    val guitarQuickPicks by viewModel.guitarQuickPicks.collectAsState()
    val ukuleleQuickPicks by viewModel.ukuleleQuickPicks.collectAsState()
    val shortsQuickPicks by viewModel.shortsQuickPicks.collectAsState()
    val remixQuickPicks by viewModel.remixQuickPicks.collectAsState()
    val lofiQuickPicks by viewModel.lofiQuickPicks.collectAsState()
    val indiaCoverQuickPicks by viewModel.indiaCoverQuickPicks.collectAsState()
    val indiaGuitarQuickPicks by viewModel.indiaGuitarQuickPicks.collectAsState()
    val pakistanCoverQuickPicks by viewModel.pakistanCoverQuickPicks.collectAsState()
    val pakistanGuitarQuickPicks by viewModel.pakistanGuitarQuickPicks.collectAsState()
    var selectedShortIndex by remember { mutableStateOf<Int?>(null) }

    val downloadingKeys = remember { mutableStateOf(setOf<String>()) }
    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val threeCardsWidth = ((screenWidthDp - 32.dp - 16.dp) / 3).coerceIn(100.dp, 126.dp)
    val playlistTrack = remember { mutableStateOf<UnifiedTrack?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    var isDiscoverRefreshing by remember { mutableStateOf(false) }

    val hiddenTrackKeys by viewModel.hiddenTrackKeys.collectAsState()
    val isTrackVisible: (UnifiedTrack) -> Boolean = remember(hiddenTrackKeys) {
        { track ->
            if (hiddenTrackKeys.isEmpty()) true
            else {
                val key = track.key
                val norm = track.title.trim().lowercase()
                key !in hiddenTrackKeys && "yt:$key" !in hiddenTrackKeys && (norm.isEmpty() || norm !in hiddenTrackKeys)
            }
        }
    }

    val handleHideTrack: (UnifiedTrack) -> Unit = { track ->
        viewModel.hideTrack(track)
        coroutineScope.launch {
            val res = snackbarHostState.showSnackbar(
                message = "Removed \"${track.title.take(24)}\" from category",
                actionLabel = "Undo",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.unhideTrack(track.key)
                viewModel.unhideTrack("yt:${track.key}")
                viewModel.unhideTrack(track.title.trim().lowercase())
            }
        }
    }

    // ── Dynamic Rotating Search Placeholder Hints ───────────────────────────
    val searchHints = remember {
        listOf(
            "Search 'Arijit Singh'...",
            "Search 'Anuv Jain'...",
            "Search 'Sushin Shyam'...",
            "Search 'Anirudh Ravichander'...",
            "Search 'Taylor Swift'...",
            "Search 'Top 2026 Hits'...",
            "Search 'The Weeknd'...",
            "Search 'Bollywood Romance'...",
            "Search songs, albums, artists..."
        )
    }
    var currentHintIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            currentHintIndex = (currentHintIndex + 1) % searchHints.size
        }
    }

    // ── Dynamic Multi-Region Quick Picks (India, Pakistan, Malayalam, Tamil) ──
    var quickPicksSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedRegionIdx by rememberSaveable { mutableIntStateOf(0) }

    val quickPickRegions = remember(
        indiaQuickPicks, pakistanQuickPicks, malayalamQuickPicks, tamilQuickPicks,
        coverQuickPicks, remixQuickPicks, lofiQuickPicks, ytMusicResults, popular, quickPicksSeed
    ) {
        // Each lane falls back to the app's general "popular" feed when its own source
        // is empty — NOT to another lane's list, which is what previously made every
        // region pill silently show India's songs whenever its own fetch failed.
        val popularFallback = popular.take(20)
        val inList = (indiaQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { popularFallback }).take(20)
        val pkList = (pakistanQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { popularFallback }).take(20)
        val malList = (malayalamQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { popularFallback }).take(20)
        val tamList = (tamilQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { popularFallback }).take(20)
        val telList = (teluguQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { popularFallback }).take(20)
        val coverList = (coverQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { popularFallback }).take(20)
        val remixList = (remixQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { popularFallback }).take(20)
        val lofiList = (lofiQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { popularFallback }).take(20)

        listOf(
            "India 🇮🇳" to inList,
            "Pakistan 🇵🇰" to pkList,
            "Malayalam 🌴" to malList,
            "Tamil 🔥" to tamList,
            "Telugu ⚡" to telList,
            "Cover 🎤" to coverList,
            "Remix 🎛️" to remixList,
            "Lofi 🌙" to lofiList
        )
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..22 -> "Good evening"
            else -> "Late Night Music"
        }
    }

    val ytMoodChips = remember {
        listOf(
            "Podcasts",
            "Energize",
            "Feel good",
            "Relax",
            "Workout",
            "Focus",
            "Party",
            "Romance",
            "Commute"
        )
    }
    var selectedMoodChip by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {




            
            
    
    data class HipHopArtistData(
        val name: String,
        val alias: String,
        val followers: String,
        val imageUrl: String,
        val query: String
    )

    val desiHipHopStars = remember {
        listOf(
            HipHopArtistData("DIVINE", "Gully Gang · Mumbai", "7.4M", "https://yt3.googleusercontent.com/a3wZvgiy2ANMBlSzrBHr232HqX6oCBHjQ7a4DOahaeUVtTysin5uV-E9F-olprYHb7WJwF9AUZbcH5A=w120-h120-p-l90-rj", "DIVINE rap songs"),
            HipHopArtistData("Naezy", "The Baa · Aafat", "1.8M", "https://lh3.googleusercontent.com/A7qovVuzXViK39mrt74ZlY7atQzLFKcYLrbUGpodBSA4TOiOnUviZL-dfxAm4AUjfFX3FXYS-shRs0Q=w120-h120-p-l90-rj", "Naezy rap songs"),
            HipHopArtistData("Emiway Bantai", "Bantai Records · Machayenge", "8.2M", "https://yt3.googleusercontent.com/-7rsPejxcna4AlrsVt_SsAFs8TH34Mh967V-jGHCxR7dalOrcbxVQUcVLhMvIKI0_AbJenSCbA=w120-h120-l90-rj-dcFTOeM5kJ", "Emiway Bantai songs"),
            HipHopArtistData("Seedhe Maut", "Calm & Encore · Nayaab", "3.5M", "https://yt3.googleusercontent.com/DUcKt_1YaJ_48_T_hlxWg285BGKkTfwNdzKRV82G-gHZVerUQ8FD8Dl2hkqHLUirrJDnG4C3RA=w120-h120-l90-rj", "Seedhe Maut songs"),
            HipHopArtistData("KR" + "$" + "NA", "Kalamkaar · Still Here", "4.1M", "https://yt3.ggpht.com/ytc/AIdro_n00p_ZePoxDQQ9m1fOAv5f6CPy-GyG97eU5hKHI3wX5cM=w120-h120-l90-rj", "KRSNA rap songs"),
            HipHopArtistData("MC Stan", "Tadipaar · Insaan", "12.5M", "https://yt3.googleusercontent.com/XyR-xmJJoR6J4AqcxHERWkBGfrNxtdLKU0GvK3FsstWqM6Sfuyt7HdDjOZU-sZt3Yiq9eSvVLA=w120-h120-l90-rj-dcpUWO7KEI", "MC Stan songs"),
            HipHopArtistData("Raftaar", "Kalamkaar · Hard Drive", "5.9M", "https://lh3.googleusercontent.com/z610DpdCiipHA1F_igiSvVNupDEv9ES2-bO0N1Ox69vW-lItyCOD9Jsqti4PBm5ZPD3XoKowbHPMxw=w120-h120-p-l90-rj", "Raftaar rap songs"),
            HipHopArtistData("Badshah", "Desi Hip Hop · 3:00 AM", "14.2M", "https://lh3.googleusercontent.com/bbR8znm7CX07mCGQH-M484ckFRaKkSmTjwrwuFZxQUBy7Uc5gQcintkpqDXCuSX0DdLLg2aPskZhC2s=w120-h120-p-l90-rj", "Badshah top songs"),
            HipHopArtistData("Yo Yo Honey Singh", "Glory · Desi Kalakaar", "16.8M", "https://lh3.googleusercontent.com/Ss_NEfGmfpwXCiuoNxiKxWAoU3M484SwZ4UmahATX7KwOqIaoqTyESuNyZV3fzJm25bmjtfSUxsIFI8=w120-h120-p-l90-rj", "Yo Yo Honey Singh songs"),
            HipHopArtistData("King", "New Life · Maan Meri Jaan", "6.2M", "https://yt3.googleusercontent.com/zXNttSOqO-WhRBCImfU_U_SVCrEmk4GUENAM5F_hf7n704lMA6I2fvXvTrw1D_Sf8Lq7Gkz46Q=w120-h120-l90-rj-dcqVSaryEJ", "King hip hop songs")
        )
    }

    val malluRappers = remember {
        listOf(
            HipHopArtistData("Hanumankind", "Big Dawgs · Kerala", "9.8M", "https://yt3.googleusercontent.com/zOS_QYl-65KQgDT1-DVVEBuDt31HEDBz2KqtycWoRbXn6JeKcGE763hTIxqnkEP-2kL69Pwk5w=w120-h120-l90-rj-dcAUCWTxko0EgC", "Hanumankind songs"),
            HipHopArtistData("Dabzee", "Manavalan Thug · Malappuram", "4.5M", "https://yt3.googleusercontent.com/64GelX6IXb5kZPpUlcffL0DS3_qanJ3xUU693M_lPBjaQo7BV64wyP2fyzLDKb9lnCShNeOSXZVUSpMYAw=w120-h120-l90-rj", "Dabzee rap songs"),
            HipHopArtistData("Fejo", "Aparaada · Kochi Rap", "2.1M", "https://yt3.googleusercontent.com/7yyzK3109X05ZQ_g0dDyKwNE8mnbB3YJIfamnwp_A__YVz2NOK-CfOfflGV7ffDaRyaGLwL7E7w6UvKw=w120-h120-l90-rj", "Fejo malayalam rap"),
            HipHopArtistData("ThirumaLi", "Malayali Da · Kottayam", "2.8M", "https://yt3.googleusercontent.com/vFBoXPk44I3XJGYO48qxvwylxXcnOiLxKBNK4KzBtf1LEITqNvUyBCyPPNPwy5TWZIywZ9AM0oWlcJvc=w120-h120-l90-rj", "ThirumaLi songs"),
            HipHopArtistData("Baby Jean", "Kathanar · Wayanad", "1.9M", "https://yt3.googleusercontent.com/BCnmZaaQfRHs_HWyq5eAlmmXbarScypEow1UUHn2aD-4RDjaEn69wpfiIohI3MBewyTYY_GLWg=w120-h120-l90-rj", "Baby Jean malayalam rap"),
            HipHopArtistData("Vedan", "Voice of the Voiceless", "1.7M", "https://yt3.googleusercontent.com/Q0z4edgawcUoGjo9yEi7NO4vML5BRKN9hh2XJ4h_IFG4Z4VlLmj5LmjLkmPqIW0a7R4IY100hw=w120-h120-l90-rj", "Vedan rap songs"),
            HipHopArtistData("Neeraj Madhav (NJ)", "Panipaali · Kozhikode", "3.2M", "https://yt3.googleusercontent.com/6Q4rIkEgW8N9dfGf-EzoWW06qTYfuPyuwfjdGwyFccJ1RvG-afF65OMDnCImM7fBFHwiGoCb2FMrPY9_=w120-h120-l90-rj", "Neeraj Madhav NJ songs"),
            HipHopArtistData("MC Couper", "Kallanum Polisum · TVM", "920K", "https://yt3.googleusercontent.com/BBin1DeTcjk-H6kaBdPKqzHPkgAUKAV6B5V_HKdFpqVm5xkeTub6w7k3o60E7OZYjsJjoTOag3C4Wwf7zg=w120-h120-l90-rj", "MC Couper rap"),
            HipHopArtistData("Street Academics", "Kalapila · Kerala Hip Hop", "1.2M", "https://yt3.ggpht.com/ytc/AIdro_kRVLk8WMoq48w41YhC_lZz6Lz9us7CGjhzSzzWqNiEMA=w120-h120-l90-rj-dcGUOQSiEH", "Street Academics songs")
        )
    }

    val tamilRappers = remember {
        listOf(
            HipHopArtistData("Arivu", "Enjoy Enjaami · Therukural", "5.4M", "https://lh3.googleusercontent.com/0QDZohumAtFlbONVusMVMR4YNz_2en6mRIds0kvyZQ4ccnfLfukO2PsO8-_YdOLBoyk8D42_z7PLFg=w120-h120-p-l90-rj", "Arivu rap songs"),
            HipHopArtistData("Paal Dabba", "Kathu Mela · 170CM", "3.1M", "https://yt3.googleusercontent.com/ABZuKvC3mB6TlhOBV22HJXfM4ZGLRN4uwQ1MnQcp6e8OtHDxT6rUN6efzPZeXPve7-8DEJb8=w120-h120-l90-rj", "Paal Dabba songs"),
            HipHopArtistData("Asal Kolaar", "Jorthaala · Vada Chennai", "2.6M", "https://yt3.googleusercontent.com/RiisZ2o46Kf16cka_RGOWAT4tg_QWkSYqqnC8eCIdH3KiBARfSVzCi5LYF7QY4fqbC1HbCVMgQ=w120-h120-l90-rj", "Asal Kolaar songs"),
            HipHopArtistData("Hiphop Tamizha", "Club Le Mabbu Le · Pioneer", "11.2M", "https://yt3.googleusercontent.com/zkX7FBr1BBzAhH7U5KT6hzBbPx8kjfK1QpTRbW-oM-J8v2f6P0t9idQuPZkssxnIk3U5yBC3KcmU5g=w120-h120-p-l90-rj", "Hiphop Tamizha songs"),
            HipHopArtistData("Yogi B & Natchatra", "Madai Thiranthu · Legend", "2.9M", "https://yt3.googleusercontent.com/2ei96JJuFJ7TgG3cWL7OzYe1vZ0ctQo5odGRkQJDq3tIegP0rTUHxdGRTiBmtQ1rEGl5VVNkakenbNOcfQ=w120-h120-l90-rj", "Yogi B Natchatra songs"),
            HipHopArtistData("ADK", "Aathichudi · Colombo/Chennai", "1.8M", "https://lh3.googleusercontent.com/Q9vhAQKKHv6S6Y388hjLhyLyiTlgheQQWk6_fsv6sTJUh0jzinTL6Fpa1VqTj6LRibBW0gyLhQzkdwg=w120-h120-p-l90-rj", "ADK tamil rap"),
            HipHopArtistData("OfRo", "Therukural · Producer/MC", "1.5M", "https://yt3.googleusercontent.com/vjMgx84SV2a7IhkWmj_gFJ5NFhahGkq0_IDWzpHPn9diCwDBKk9RKMlrY0VfsHTAXrcuDIFaqQ=w120-h120-l90-rj-dcoTaMC54I", "OfRo songs"),
            HipHopArtistData("Ken Karunas", "Vada Chennai · Asuran", "1.1M", "https://lh3.googleusercontent.com/4Jd9XSimz29-o12oJKUmgfTx3otHBHlTy0jb3Ace4ti2bz8Nuo59IeneNlM9EKRQXNRpLwk6YoEuNQ=w120-h120-p-l90-rj", "Ken Karunas rap")
        )
    }

    val defaultShortsFallback = remember {
        listOf(
            YouTubeSearchResult("60ItHLz5WEA", "Faded (Acoustic Guitar Live Short)", "Alan Walker", "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg", "0:45"),
            YouTubeSearchResult("ALZHF5UqnU4", "Alone (Ukulele & Guitar Live Clip)", "Marshmello", "https://i.ytimg.com/vi/ALZHF5UqnU4/hqdefault.jpg", "0:52"),
            YouTubeSearchResult("3AtDnEC4zak", "DIVINE - Kohinoor Live Concert Short", "DIVINE", "https://i.ytimg.com/vi/3AtDnEC4zak/hqdefault.jpg", "0:48"),
            YouTubeSearchResult("k4yXQkG2s1E", "Emiway - Machayenge Hook Step Short", "Emiway Bantai", "https://i.ytimg.com/vi/k4yXQkG2s1E/hqdefault.jpg", "0:39"),
            YouTubeSearchResult("2Vv-BfVoq4g", "Perfect (Ed Sheeran Fingerstyle Guitar Short)", "Ed Sheeran", "https://i.ytimg.com/vi/2Vv-BfVoq4g/hqdefault.jpg", "0:58"),
            YouTubeSearchResult("JGwWNGJdvx8", "Shape of You (Indie Ukulele Cover Short)", "Indie Sessions", "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg", "0:42")
        )
    }

    val activeShortsList = remember(shortsQuickPicks, defaultShortsFallback) {
        (shortsQuickPicks + defaultShortsFallback).distinctBy { it.videoId }
    }

    // ── Computed YouTube Music Shelves Data ─────────────────────────────────
    val recentTracks = remember(recent) { recent.take(15) }
    val likedTracks = remember(likedMusic) { likedMusic.map { it.toUnifiedTrack() }.take(15) }
    val primaryTracks = remember(recentTracks, likedTracks, popular) {
        when {
            recentTracks.isNotEmpty() -> recentTracks
            likedTracks.isNotEmpty() -> likedTracks
            else -> popular.take(15)
        }
    }
    val primaryShelfTitle = when {
        recentTracks.isNotEmpty() -> "Listen again"
        likedTracks.isNotEmpty() -> "Speed dial"
        else -> "Quick picks"
    }

    val quickPicksFeed = remember(popular, indiaQuickPicks) {
        if (indiaQuickPicks.isNotEmpty()) indiaQuickPicks.map { UnifiedTrack.Youtube(it) }.take(15)
        else popular.take(15)
    }

    val forgottenFavorites = remember(popular, likedTracks) {
        (popular.drop(10).take(15) + likedTracks).distinctBy { it.key }.take(15)
    }

    val trendingUnified = remember(regionTrendingSongs, southIndiaTrending, popular, hiddenTrackKeys) {
        val regionTracks = regionTrendingSongs.map { UnifiedTrack.Youtube(it) }
        val southTracks = southIndiaTrending.map { UnifiedTrack.Youtube(it) }
        (regionTracks + southTracks + popular.drop(5)).distinctBy { it.key }.filter(isTrackVisible).take(15)
    }

    val countryTrendingTracks = remember(regionTrendingSongs, ytMusicResults, popular, selectedCountry, hiddenTrackKeys) {
        val list = if (regionTrendingSongs.isNotEmpty()) regionTrendingSongs.map { UnifiedTrack.Youtube(it) }
        else if (ytMusicResults.isNotEmpty()) ytMusicResults.map { UnifiedTrack.Youtube(it) }
        else popular
        list.filter(isTrackVisible)
    }

    val moodData = remember(selectedMoodChip, popular, ytMusicResults, regionTrendingSongs, lofiQuickPicks, remixQuickPicks, coverQuickPicks) {
        val mood = selectedMoodChip ?: return@remember null
        val ytTracks = ytMusicResults.map { UnifiedTrack.Youtube(it) }
        when (mood) {
            "Podcasts" -> {
                val talkTracks = (popular + ytTracks).filter { 
                    it.title.contains("podcast", true) || it.title.contains("show", true) || it.title.contains("episode", true) || it.title.contains("talk", true) 
                }.ifEmpty { ytTracks.take(15) }
                MoodShelfData("Top Podcasts & Shows", "Episodes for you", talkTracks.take(10), talkTracks.drop(10).take(10))
            }
            "Energize" -> {
                val energyTracks = (remixQuickPicks.map { UnifiedTrack.Youtube(it) } + popular).filter {
                    it.title.contains("dance", true) || it.title.contains("party", true) || it.title.contains("remix", true) || it.title.contains("beat", true)
                }.ifEmpty { popular.shuffled().take(15) }
                MoodShelfData("High Energy Hits", "Fast & Furious Beats", energyTracks.take(10), energyTracks.drop(10).take(10))
            }
            "Feel good" -> {
                val feelGoodTracks = popular.take(15)
                MoodShelfData("Feel Good Anthems", "Upbeat Favorites", feelGoodTracks.take(10), feelGoodTracks.drop(10).take(10))
            }
            "Relax" -> {
                val relaxTracks = (lofiQuickPicks.map { UnifiedTrack.Youtube(it) } + popular).take(20)
                MoodShelfData("Chill & Relax", "Peaceful Moments", relaxTracks.take(10), relaxTracks.drop(10).take(10))
            }
            "Workout" -> {
                val workoutTracks = (remixQuickPicks.map { UnifiedTrack.Youtube(it) } + popular).take(20)
                MoodShelfData("Workout Bangers", "Gym Motivation", workoutTracks.take(10), workoutTracks.drop(10).take(10))
            }
            "Focus" -> {
                val focusTracks = (lofiQuickPicks.map { UnifiedTrack.Youtube(it) } + popular).take(20)
                MoodShelfData("Deep Focus", "Study Beats", focusTracks.take(10), focusTracks.drop(10).take(10))
            }
            "Party" -> {
                val partyTracks = (remixQuickPicks.map { UnifiedTrack.Youtube(it) } + popular).take(20)
                MoodShelfData("Party Hits", "Club & Dance Floor", partyTracks.take(10), partyTracks.drop(10).take(10))
            }
            "Romance" -> {
                val romanceTracks = (coverQuickPicks.map { UnifiedTrack.Youtube(it) } + popular).take(20)
                MoodShelfData("Romantic Melodies", "Love Songs & Duets", romanceTracks.take(10), romanceTracks.drop(10).take(10))
            }
            else -> {
                MoodShelfData("$mood Mix", "More from $mood", popular.take(10), popular.drop(10).take(10))
            }
        }
    }

    val mixTracks = remember(regionTrendingSongs, popular, ytMusicResults) {
        (regionTrendingSongs.map { UnifiedTrack.Youtube(it) } + popular).distinctBy { it.key }.take(20)
    }

    val guitarTracks = remember(guitarQuickPicks, popular) {
        if (guitarQuickPicks.isNotEmpty()) guitarQuickPicks.map { UnifiedTrack.Youtube(it) }
        else popular.filter { it.title.contains("guitar", true) || it.title.contains("acoustic", true) }.ifEmpty { popular.take(15) }
    }

    // "Speed dial" (the 3x3 grid) used to just mirror the India quick-picks lane.
    // Now it actually blends previously-played + regional trending + cover songs +
    // fresh picks, matching what the shelf name implies.
    val speedDialTracks = remember(recentTracks, regionTrendingSongs, coverQuickPicks, quickPicksFeed, popular) {
        val previouslyPlayed = recentTracks.take(4)
        val regional = regionTrendingSongs.map { UnifiedTrack.Youtube(it) }.take(4)
        val covers = coverQuickPicks.map { UnifiedTrack.Youtube(it) }.take(3)
        val fresh = quickPicksFeed.ifEmpty { popular }.take(8)
        (previouslyPlayed + regional + covers + fresh).distinctBy { it.key }.take(15)
    }

    // "From the community" and "Music videos for you" used to just re-slice the same
    // India quick-picks list under a different label, so the same 2-3 songs kept
    // showing up in every shelf on screen. These pull from different lanes and
    // exclude whatever's already shown above, so they're genuinely different content.
    val alreadyShownKeys = remember(quickPicksFeed, speedDialTracks) {
        (quickPicksFeed.map { it.key } + speedDialTracks.map { it.key }).toSet()
    }
    val communityDailyTrending = remember(trendingUnified, alreadyShownKeys) {
        trendingUnified.filterNot { it.key in alreadyShownKeys }.ifEmpty { trendingUnified }.take(4)
    }
    val communityChillBeats = remember(lofiQuickPicks, guitarTracks, alreadyShownKeys) {
        (lofiQuickPicks.map { UnifiedTrack.Youtube(it) } + guitarTracks)
            .distinctBy { it.key }
            .filterNot { it.key in alreadyShownKeys }
            .ifEmpty { guitarTracks }
            .take(4)
    }
    val musicVideosForYou = remember(ytMusicResults, alreadyShownKeys) {
        val distinctVideos = ytMusicResults.filterNot { "yt:${it.videoId}" in alreadyShownKeys }
        distinctVideos.ifEmpty { ytMusicResults }
    }

    val indiaCoverTracks = remember(indiaCoverQuickPicks, coverQuickPicks) {
        indiaCoverQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { coverQuickPicks.map { UnifiedTrack.Youtube(it) } }
    }
    val indiaGuitarTracks = remember(indiaGuitarQuickPicks, guitarQuickPicks) {
        indiaGuitarQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { guitarQuickPicks.map { UnifiedTrack.Youtube(it) } }
    }
    val pakistanCoverTracks = remember(pakistanCoverQuickPicks, coverQuickPicks) {
        pakistanCoverQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { coverQuickPicks.map { UnifiedTrack.Youtube(it) } }
    }
    val pakistanGuitarTracks = remember(pakistanGuitarQuickPicks, guitarQuickPicks) {
        pakistanGuitarQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { guitarQuickPicks.map { UnifiedTrack.Youtube(it) } }
    }

    val ukuleleTracks = remember(ukuleleQuickPicks, popular) {
        if (ukuleleQuickPicks.isNotEmpty()) ukuleleQuickPicks.map { UnifiedTrack.Youtube(it) }
        else popular.take(15)
    }

    val acousticCoverTracks = remember(coverQuickPicks, popular) {
        if (coverQuickPicks.isNotEmpty()) coverQuickPicks.map { UnifiedTrack.Youtube(it) }
        else popular.take(15)
    }

        CompositionLocalProvider(LocalOnDeleteTrack provides handleHideTrack) {
            PullToRefreshBox(
                isRefreshing = isDiscoverRefreshing,
                onRefresh = {
                    isDiscoverRefreshing = true
                    viewModel.refreshDiscover()
                    coroutineScope.launch {
                        delay(1200)
                        isDiscoverRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ── TOP SEARCH QUICK-LAUNCH PILL (Navigates to dedicated Search screen) ──
                    item(key = "home_search_bar") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(if (appColors.isDark) Color(0xFF1E1E26) else appColors.surfaceElevated)
                                .border(1.dp, if (appColors.isDark) Color.White.copy(alpha = 0.12f) else appColors.surfaceBorder, RoundedCornerShape(23.dp))
                                .clickable { onOpenSearchWithQuery("") }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search",
                                    tint = appColors.accentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = searchHints[currentHintIndex],
                                    color = appColors.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }



                    if (moodData != null) {
                        if (moodData.tracks1.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = moodData.shelf1,
                                        avatarUrl = moodData.tracks1.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery(selectedMoodChip ?: "") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(moodData.tracks1, key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, moodData.tracks1) },
                                                onDownload = {
                                                downloadTargetTrack.value = track
                                            },
                                            isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (moodData.tracks2.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = moodData.shelf2,
                                        avatarUrl = moodData.tracks2.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery(selectedMoodChip ?: "") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(moodData.tracks2, key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, moodData.tracks2) },
                                                onDownload = {
                                                downloadTargetTrack.value = track
                                            },
                                            isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ── 1. YOUTUBE MIX PREVIEW CARD ("India Trending Mix" / Country Mix style card) ──
                        if (countryTrendingTracks.isNotEmpty()) {
                            item {
                                val mixTitle = when (selectedCountry.uppercase()) {
                                    "PK" -> "Pakistan Trending Hits"
                                    "AE" -> "Dubai & Gulf Hits"
                                    "US" -> "Billboard Hot 100"
                                    "GB" -> "UK Official Charts"
                                    "SA" -> "Saudi Top Hits"
                                    else -> "India Trending Mix"
                                }
                                val mixSubtitle = when (selectedCountry.uppercase()) {
                                    "PK" -> "YouTube Music • Pakistan Top Hits"
                                    "AE" -> "YouTube Music • Gulf & Arabic Hits"
                                    "US" -> "YouTube Music • Billboard Hot 100"
                                    "GB" -> "YouTube Music • UK Official 40"
                                    "SA" -> "YouTube Music • Saudi & Khaleeji Hits"
                                    else -> "YouTube Music • India Top Trending"
                                }
                                YouTubeMixPreviewCard(
                                    title = mixTitle,
                                    subtitle = mixSubtitle,
                                    coverUrl = countryTrendingTracks.firstOrNull()?.thumbnailUrl.orEmpty(),
                                    tracks = countryTrendingTracks,
                                    onPlayAll = { viewModel.playUnified(countryTrendingTracks.first(), countryTrendingTracks) },
                                    onTrackClick = { track -> viewModel.playUnified(track, countryTrendingTracks) },
                                    onDeleteTrack = { track -> handleHideTrack(track) },
                                    onRotateMix = { viewModel.rotateTrendingMix() },
                                    onSeeMore = { onOpenSearchWithQuery(mixTitle) }
                                )
                            }
                        }

                        // ── 2. SPOTLIGHT ARTISTS (Swipeable genres & live YouTube trending creators) ──
                        item {
                            SpotlightArtistsSection(
                                liveArtists = chartsArtists,
                                onOpenArtist = { artistName -> onOpenSearchWithQuery(artistName) },
                                onOpenChartArtist = { chartArtist -> onOpenArtist(chartArtist) }
                            )
                        }

                        // ── 3. SOUTH & REGIONAL 4-SQUARE CARDS (Malayalam, Tamil, Telugu, Hindi) ──
                        item {
                            SouthRegionalCategoriesSection(
                                malayalamTracks = malayalamQuickPicks,
                                tamilTracks = tamilQuickPicks,
                                teluguTracks = teluguQuickPicks,
                                hindiTracks = indiaQuickPicks,
                                onSelectLanguage = { _, query ->
                                    onOpenSearchWithQuery(query)
                                }
                            )
                        }

                        // ── SHELF 0: MADE FOR YOU (Taste-Learning Recommendation Engine) ──
                        if (madeForYouRecommendations.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = madeForYouTitle,
                                        subtitle = "LEARNED FROM YOUR TASTE • LIVE RECOMMENDATIONS",
                                        avatarUrl = madeForYouRecommendations.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery(madeForYouTitle) }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(madeForYouRecommendations, key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, madeForYouRecommendations) },
                                                onDownload = {
                                                    downloadTargetTrack.value = track
                                                },
                                                isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── YouTube Music Official Home Feed ──────────────────────────

                        // ── Speed Dial (3-Page Swipeable: Songs -> Albums -> Artists) ──
                        item {
                            SpeedDialShelf(
                                tracks = speedDialTracks,
                                albums = exploreNewReleases,
                                artists = chartsArtists,
                                onPlayTrack = { track -> viewModel.playUnified(track, speedDialTracks) },
                                onOpenAlbum = { album -> onOpenAlbum(album) },
                                onOpenArtist = { artist -> onOpenArtist(artist) },
                                onOpenSearchWithQuery = { query -> onOpenSearchWithQuery(query) }
                            )
                        }

                        // ── Shelf 1: Mixed for you (Supermix Cards - 1 Full Card at a time, No Cut-Off) ──
                        if (mixTracks.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    YouTubeShelfHeader(
                                        title = "Mixed for you",
                                        subtitle = "AUTOPLAY & CONTINUOUS MIXES",
                                        onSeeAll = { onOpenSearchWithQuery("Supermix") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    val supermixCards = remember(mixTracks, lofiQuickPicks, remixQuickPicks) {
                                        listOf(
                                            Triple("My Supermix", "YouTube Music • Selected for you", mixTracks),
                                            Triple("Chill & Relax Mix", "Lo-Fi & Acoustic • YouTube Music", lofiQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { mixTracks }),
                                            Triple("Energy & Workout Mix", "Fast Beats & Remixes • YouTube Music", remixQuickPicks.map { UnifiedTrack.Youtube(it) }.ifEmpty { mixTracks })
                                        )
                                    }
                                    val supermixPagerState = rememberPagerState(pageCount = { supermixCards.size })
                                    HorizontalPager(
                                        state = supermixPagerState,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { pageIdx ->
                                        val (title, sub, tracks) = supermixCards[pageIdx]
                                        SupermixCard(
                                            mixTitle = title,
                                            subtitle = sub,
                                            tracks = tracks,
                                            onPlayMix = {
                                                tracks.firstOrNull()?.let { viewModel.playUnified(it, tracks) }
                                            },
                                            onPlayTrack = { track -> viewModel.playUnified(track, tracks) },
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(supermixCards.size) { dotIdx ->
                                            val isSelected = supermixPagerState.currentPage == dotIdx
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 3.dp)
                                                    .height(4.dp)
                                                    .width(if (isSelected) 18.dp else 6.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(if (isSelected) appColors.accentPrimary else Color.White.copy(alpha = 0.2f))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 1.5: Trending on Live Saavn Server ──
                        if (saavnTrending.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    YouTubeShelfHeader(
                                        title = "Live Trending (Saavn Server)",
                                        subtitle = "DIRECT HIGH-QUALITY STREAMING FROM SINGAPORE PROXY",
                                        avatarUrl = saavnTrending.firstOrNull()?.albumArtUri?.toString(),
                                        onSeeAll = { onOpenSource(com.musicdrop.app.ui.screens.MusicSource.JIOSAAVN) }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        val saavnUnified = saavnTrending.map { com.musicdrop.app.data.model.UnifiedTrack.Saavn(it) }
                                        items(saavnUnified, key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, saavnUnified) },
                                                onDownload = { downloadTargetTrack.value = track },
                                                isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 1.6: Trending Albums & New Releases (Official YouTube Music) ──
                        if (exploreNewReleases.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    YouTubeShelfHeader(
                                        title = "Trending Albums & New Releases",
                                        subtitle = "OFFICIAL YOUTUBE MUSIC • ALBUMS & EPS",
                                        avatarUrl = exploreNewReleases.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery("new albums 2026") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(exploreNewReleases.take(24), key = { it.videoId }) { album ->
                                            Column(
                                                modifier = Modifier
                                                    .width(threeCardsWidth)
                                                    .clickable {
                                                        onOpenAlbum(
                                                            com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem(
                                                                title = album.title,
                                                                browseId = album.videoId,
                                                                audioPlaylistId = album.videoId,
                                                                thumbnailUrl = album.thumbnailUrl,
                                                                type = album.duration.ifBlank { "Album" },
                                                                artistName = album.channelTitle
                                                            )
                                                        )
                                                    }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(threeCardsWidth)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(appColors.surfaceElevated)
                                                        .border(1.dp, appColors.surfaceBorder, RoundedCornerShape(12.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = album.thumbnailUrl,
                                                        contentDescription = album.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    if (album.duration.isNotBlank()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomStart)
                                                                .padding(6.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color.Black.copy(alpha = 0.75f))
                                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = album.duration.uppercase(),
                                                                color = Color.White,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = album.title,
                                                    color = appColors.textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = album.channelTitle,
                                                    color = appColors.textSecondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 2: Quick picks / Mixed for you ──
                        if (quickPicksFeed.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Quick picks",
                                        subtitle = "SIMILAR TO RECENT LISTENS",
                                        avatarUrl = quickPicksFeed.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery("Top Hits") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(quickPicksFeed, key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, quickPicksFeed) },
                                                onDownload = {
                                                    downloadTargetTrack.value = track
                                                },
                                                isPreparing = track.key == preparingKey,
                                                cardWidth = threeCardsWidth
                                            )
                                        }
                                    }
                                }
                            }
                        }

// ── Shelf 3.5: From the community (2x2 Mosaic Cards matching Image 3) ──
                        if (communityDailyTrending.isNotEmpty() && communityChillBeats.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                                    YouTubeShelfHeader(
                                        title = "From the community",
                                        subtitle = "POPULAR COMMUNITY PLAYLISTS",
                                        onSeeAll = { onOpenSearchWithQuery("Community Playlists") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        item {
                                            CommunityMosaicCard(
                                                playlistTitle = "Daily Trending Tracks",
                                                curator = "MusicDrop Community",
                                                views = "354K views",
                                                tracks = communityDailyTrending,
                                                onClick = {
                                                    communityDailyTrending.firstOrNull()?.let { viewModel.playUnified(it, communityDailyTrending) }
                                                }
                                            )
                                        }
                                        item {
                                            CommunityMosaicCard(
                                                playlistTitle = "Travelling & Chill Beats",
                                                curator = "MusicDrop Curators",
                                                views = "1.2M views",
                                                tracks = communityChillBeats,
                                                onClick = {
                                                    communityChillBeats.firstOrNull()?.let { viewModel.playUnified(it, communityChillBeats) }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 3: Recommended music videos (16:9 Widescreen Cards) ──
                        if (musicVideosForYou.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Music videos for you",
                                        subtitle = "RECOMMENDED FOR YOU",
                                        onSeeAll = { onOpenSearchWithQuery("Music Videos") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(musicVideosForYou.take(10), key = { it.videoId }) { item ->
                                            val isPrep = preparingKey == "yt:${item.videoId}"
                                            Column(
                                                modifier = Modifier
                                                    .width(220.dp)
                                                    .clickable { viewModel.playYouTubeVideoWithContext(item, musicVideosForYou) }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(124.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(appColors.surfaceElevated)
                                                ) {
                                                    AsyncImage(
                                                        model = item.thumbnailUrl,
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.Center)
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.Black.copy(alpha = 0.55f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isPrep) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(20.dp),
                                                                strokeWidth = 2.dp,
                                                                color = Color.White
                                                            )
                                                        } else {
                                                            Icon(
                                                                Icons.Rounded.PlayCircleFilled,
                                                                contentDescription = "Play",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                    if (item.duration.isNotBlank()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomEnd)
                                                                .padding(6.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color.Black.copy(alpha = 0.75f))
                                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                item.duration,
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    item.title,
                                                    color = appColors.textPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    item.channelTitle,
                                                    color = appColors.textSecondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 4: Forgotten favorites / Discover ──
                        if (forgottenFavorites.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Forgotten favorites",
                                        subtitle = "REDISCOVER YOUR MUSIC",
                                        avatarUrl = forgottenFavorites.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery("Favorites") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(forgottenFavorites, key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, forgottenFavorites) },
                                                onDownload = {
                                                downloadTargetTrack.value = track
                                            },
                                            isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 5: Trending now ──
                        if (trendingUnified.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Hello, Summer!",
                                        subtitle = "TUNES FOR THE SEASON ☀️🌴",
                                        avatarUrl = trendingUnified.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery("Trending Now") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(trendingUnified, key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, trendingUnified) },
                                                onDownload = {
                                                downloadTargetTrack.value = track
                                            },
                                            isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 6: Top Artists ──
                        if (chartsArtists.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Top Artists",
                                        subtitle = "POPULAR CREATORS",
                                        onSeeAll = { onOpenSearchWithQuery("Top Artists") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(chartsArtists.take(12)) { artist ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .width(90.dp)
                                                    .clickable { onOpenArtist(artist) }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clip(CircleShape)
                                                        .background(appColors.surfaceElevated)
                                                        .border(1.5.dp, appColors.surfaceBorder, CircleShape)
                                                ) {
                                                    AsyncImage(
                                                        model = artist.thumbnailUrl,
                                                        contentDescription = artist.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    artist.title,
                                                    color = appColors.textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (artist.subscribers.isNotBlank()) {
                                                    Text(
                                                        artist.subscribers,
                                                        color = appColors.textSecondary,
                                                        fontSize = 10.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 7: Official Top Charts ──
                        if (chartsDaily.isNotEmpty() || chartsWeekly.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Official Top Charts",
                                        subtitle = "GLOBAL & REGIONAL CHARTS",
                                        onSeeAll = { onOpenSearchWithQuery("Top Charts") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items((chartsDaily + chartsWeekly).take(10)) { chart ->
                                            Column(
                                                modifier = Modifier
                                                    .width(135.dp)
                                                    .clickable { onOpenSearchWithQuery(chart.title) }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(135.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(appColors.surfaceElevated)
                                                ) {
                                                    AsyncImage(
                                                        model = chart.thumbnailUrl,
                                                        contentDescription = chart.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(6.dp)
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.Black.copy(alpha = 0.65f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.PlayArrow,
                                                            contentDescription = "Play",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    chart.title,
                                                    color = appColors.textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        
                        // ── Shelf 7B: Desi Hip Hop & Gully Icons (Divine, Naezy, Emiway, etc.) ──
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                YouTubeShelfHeader(
                                    title = "Desi Hip Hop & Gully Icons",
                                    subtitle = "DIVINE, NAEZY, EMIWAY & TOP RAPPERS 🔥",
                                    onSeeAll = { onOpenSearchWithQuery("Desi Hip Hop") }
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(desiHipHopStars) { artist ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(96.dp)
                                                .clickable { onOpenSearchWithQuery(artist.query) }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(86.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.radialGradient(
                                                            colors = listOf(Color(0xFF431407), Color(0xFF1E1B4B))
                                                        )
                                                    )
                                                    .border(2.dp, Brush.linearGradient(listOf(Color(0xFFFF5722), Color(0xFFFF9800))), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = artist.name.take(2).uppercase(),
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp
                                                )
                                                AsyncImage(
                                                    model = artist.imageUrl,
                                                    contentDescription = artist.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                artist.name,
                                                color = appColors.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                artist.followers,
                                                color = Color(0xFFFF9800),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 7B-2: Mallu Rappers • Kerala Hip-Hop (Hanumankind, Dabzee, Fejo, etc.) ──
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                YouTubeShelfHeader(
                                    title = "Mallu Rappers • Kerala Hip-Hop",
                                    subtitle = "HANUMANKIND, DABZEE, FEJO & KERALA RAP STARS 🌴🔥",
                                    onSeeAll = { onOpenSearchWithQuery("Malayalam Hip Hop Rap") }
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(malluRappers) { artist ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(96.dp)
                                                .clickable { onOpenSearchWithQuery(artist.query) }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(86.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.radialGradient(
                                                            colors = listOf(Color(0xFF064E3B), Color(0xFF0F172A))
                                                        )
                                                    )
                                                    .border(2.dp, Brush.linearGradient(listOf(Color(0xFF00C853), Color(0xFF64DD17))), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = artist.name.take(2).uppercase(),
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp
                                                )
                                                AsyncImage(
                                                    model = artist.imageUrl,
                                                    contentDescription = artist.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                artist.name,
                                                color = appColors.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                artist.followers,
                                                color = Color(0xFF00E676),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 7B-3: Tamil Rappers • Tamil Hip-Hop (Arivu, Paal Dabba, Hiphop Tamizha, etc.) ──
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                YouTubeShelfHeader(
                                    title = "Tamil Rappers • Tamil Hip-Hop",
                                    subtitle = "ARIVU, PAAL DABBA, HIPHOP TAMIZHA & CHENNAI CYPHER ⚡",
                                    onSeeAll = { onOpenSearchWithQuery("Tamil Hip Hop Rap") }
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(tamilRappers) { artist ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(96.dp)
                                                .clickable { onOpenSearchWithQuery(artist.query) }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(86.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.radialGradient(
                                                            colors = listOf(Color(0xFF450A0A), Color(0xFF1E1B4B))
                                                        )
                                                    )
                                                    .border(2.dp, Brush.linearGradient(listOf(Color(0xFFFF3D00), Color(0xFFFF9100))), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = artist.name.take(2).uppercase(),
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp
                                                )
                                                AsyncImage(
                                                    model = artist.imageUrl,
                                                    contentDescription = artist.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                artist.name,
                                                color = appColors.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                artist.followers,
                                                color = Color(0xFFFF9100),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 7C: Acoustic & Guitar Sessions ──
                        if (guitarTracks.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Acoustic Guitar Sessions",
                                        subtitle = "FINGERSTYLE, UNPLUGGED & RELAXING STRUMS 🎸",
                                        avatarUrl = guitarTracks.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery("Acoustic Guitar Fingerstyle") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(guitarTracks.take(15), key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, guitarTracks) },
                                                onDownload = { downloadTargetTrack.value = track },
                                                isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 7D: Ukulele Vibes & Melodies ──
                        if (ukuleleTracks.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Ukulele Vibes & Melodies",
                                        subtitle = "BREEZY, SUNNY & CHILL UKULELE HITS 🪕",
                                        avatarUrl = ukuleleTracks.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery("Ukulele acoustic chill songs") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(ukuleleTracks.take(15), key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, ukuleleTracks) },
                                                onDownload = { downloadTargetTrack.value = track },
                                                isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 7E: Viral Acoustic Covers ──
                        if (acousticCoverTracks.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Viral Acoustic Covers",
                                        subtitle = "POPULAR HITS SUNG ACOUSTIC & UNPLUGGED 🎙️",
                                        avatarUrl = acousticCoverTracks.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery("Acoustic cover songs unplugged") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(acousticCoverTracks.take(15), key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, acousticCoverTracks) },
                                                onDownload = { downloadTargetTrack.value = track },
                                                isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 7E-2: Indian Cover Songs ──
                        if (indiaCoverTracks.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Indian Cover Songs",
                                        subtitle = "BOLLYWOOD & HINDI UNPLUGGED COVERS 🎙️",
                                        avatarUrl = indiaCoverTracks.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery("Bollywood cover songs") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(indiaCoverTracks.take(15), key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, indiaCoverTracks) },
                                                onDownload = { downloadTargetTrack.value = track },
                                                isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 7E-3: Indian Guitar Sessions ──
                        if (indiaGuitarTracks.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Indian Guitar Sessions",
                                        subtitle = "BOLLYWOOD & HINDI ACOUSTIC GUITAR 🎸",
                                        avatarUrl = indiaGuitarTracks.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery("Bollywood guitar cover") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(indiaGuitarTracks.take(15), key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, indiaGuitarTracks) },
                                                onDownload = { downloadTargetTrack.value = track },
                                                isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 7E-4: Pakistani Cover Songs ──
                        if (pakistanCoverTracks.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Pakistani Cover Songs",
                                        subtitle = "COKE STUDIO & URDU UNPLUGGED COVERS 🎙️",
                                        avatarUrl = pakistanCoverTracks.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery("Pakistani cover songs") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(pakistanCoverTracks.take(15), key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, pakistanCoverTracks) },
                                                onDownload = { downloadTargetTrack.value = track },
                                                isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 7E-5: Pakistani Guitar Sessions ──
                        if (pakistanGuitarTracks.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Pakistani Guitar Sessions",
                                        subtitle = "COKE STUDIO & URDU ACOUSTIC GUITAR 🎸",
                                        avatarUrl = pakistanGuitarTracks.firstOrNull()?.thumbnailUrl,
                                        onSeeAll = { onOpenSearchWithQuery("Pakistani guitar cover") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(pakistanGuitarTracks.take(15), key = { it.key }) { track ->
                                            UnifiedMusicCard(
                                                track = track,
                                                isDownloading = track.key in downloadingKeys.value,
                                                isDownloaded = downloadedTracks.any { it.key == track.key },
                                                onPlay = { viewModel.playUnified(track, pakistanGuitarTracks) },
                                                onDownload = { downloadTargetTrack.value = track },
                                                isPreparing = track.key == preparingKey
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 7F: Shorts & Quick Clips (Vertical Cards -> Full Screen Player) ──
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                YouTubeShelfHeader(
                                    title = "Shorts & Quick Clips",
                                    subtitle = "TAP TO WATCH IN FULL-SCREEN VIDEO PLAYER ⚡",
                                    onSeeAll = { onOpenSearchWithQuery("trending music shorts") }
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    itemsIndexed(activeShortsList) { index, shortItem ->
                                        Column(
                                            modifier = Modifier
                                                .width(135.dp)
                                                .clickable { selectedShortIndex = index }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(135.dp)
                                                    .height(210.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(appColors.surfaceElevated)
                                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                            ) {
                                                AsyncImage(
                                                    model = shortItem.thumbnailUrl,
                                                    contentDescription = shortItem.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Bottom dark gradient scrim
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(90.dp)
                                                        .align(Alignment.BottomCenter)
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                                            )
                                                        )
                                                )

                                                // Top-left SHORTS red tag
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .padding(8.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color.Red.copy(alpha = 0.85f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        .align(Alignment.TopStart)
                                                ) {
                                                    Icon(
                                                        Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(Modifier.width(2.dp))
                                                    Text(
                                                        "SHORT",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }

                                                // Top-right duration badge
                                                if (shortItem.duration.isNotBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(8.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color.Black.copy(alpha = 0.75f))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                            .align(Alignment.TopEnd)
                                                    ) {
                                                        Text(
                                                            shortItem.duration,
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                // Bottom title & channel
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.BottomStart)
                                                        .padding(10.dp)
                                                ) {
                                                    Text(
                                                        shortItem.title,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        lineHeight = 15.sp
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        shortItem.channelTitle,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Shelf 8: Curated Playlists ──
                        if (curatedPlaylists.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    YouTubeShelfHeader(
                                        title = "Featured playlists for you",
                                        subtitle = "COMMUNITY PICKS",
                                        onSeeAll = { onOpenSearchWithQuery("Playlists") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(curatedPlaylists.take(10)) { playlist ->
                                            CuratedPlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
        )

        // Full Screen Shorts Video Player
        LaunchedEffect(selectedShortIndex) {
            if (selectedShortIndex != null) {
                viewModel.playbackConnection.pause()
            }
        }
        selectedShortIndex?.let { idx ->
            ShortsFullScreenPlayer(
                shortsList = activeShortsList,
                initialIndex = idx,
                viewModel = viewModel,
                onClose = { selectedShortIndex = null },
                onPlayFullSong = { shortItem ->
                    viewModel.playUnified(
                        UnifiedTrack.Youtube(shortItem),
                        activeShortsList.map { UnifiedTrack.Youtube(it) }
                    )
                }
            )
        }

        // Add to Playlist Dialog
        playlistTrack.value?.let { track ->
            AddToPlaylistDialog(
                track = track,
                viewModel = viewModel,
                onDismiss = { playlistTrack.value = null },
                onAdded = { name ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Added to $name")
                    }
                }
            )
        }

        // Download Choice Modal (Audio vs Video with live progress)
        downloadTargetTrack.value?.let { track ->
            com.musicdrop.app.ui.components.DownloadChoiceModal(
                track = track,
                viewModel = viewModel,
                onDismiss = { downloadTargetTrack.value = null },
                onComplete = { success, _ ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(if (success) "Downloaded: ${track.title}" else "Download failed")
                    }
                }
            )
        }
    }
}


// ── TOP TRENDING BY REGION (Matching Screenshot 2) ───────────────────────────
@Composable
fun TopTrendingByRegionSection(
    selectedCountry: String,
    onSelectCountry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val countries = listOf(
        Triple("IN", "🇮🇳", "India"),
        Triple("PK", "🇵🇰", "Pakistan"),
        Triple("AE", "🇦🇪", "UAE"),
        Triple("US", "🇺🇸", "USA"),
        Triple("GB", "🇬🇧", "UK"),
        Triple("SA", "🇸🇦", "Saudi")
    )
    Column(modifier = modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = "TOP TRENDING BY REGION",
            color = Color(0xFFFFD600),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(countries) { (code, flag, _) ->
                val isSelected = selectedCountry.equals(code, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(64.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSelected) Color(0xFF0F382A) else Color(0xFF1E1E22))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF00E676) else Color(0x33FFFFFF),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { onSelectCountry(code) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = flag, fontSize = 24.sp)
                }
            }
        }
    }
}

// ── SPOTLIGHT ARTISTS (Swipeable Genres & Fallback Avatars) ──────────────────
data class SpotlightArtist(
    val name: String,
    val subs: String,
    val imageUrl: String,
    val initial: String,
    val gradientColors: List<Color>
)

data class ArtistGenrePage(
    val genreTitle: String,
    val categoryTag: String,
    val icon: ImageVector,
    val artistPool: List<SpotlightArtist>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpotlightArtistsSection(
    onOpenArtist: (String) -> Unit,
    liveArtists: List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist> = emptyList(),
    onOpenChartArtist: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()
    var shuffleOffset by remember { mutableIntStateOf(0) }

    val genrePages = remember {
        listOf(
            ArtistGenrePage(
                genreTitle = "DESI HIP-HOP & RAP",
                categoryTag = "TRENDING NOW",
                icon = Icons.Rounded.Star,
                artistPool = listOf(
                    SpotlightArtist("Badshah", "14.2M", "https://lh3.googleusercontent.com/Ss_NEfGmfpwXCiuoNxiKxWAoU3M484SwZ4UmahATX7KwOqIaoqTyESuNyZV3fzJm25bmjtfSUxsIFI8=w120-h120-p-l90-rj", "B", listOf(Color(0xFFFF1744), Color(0xFFD500F9))),
                    SpotlightArtist("Yo Yo Honey Singh", "16.8M", "https://lh3.googleusercontent.com/Ss_NEfGmfpwXCiuoNxiKxWAoU3M484SwZ4UmahATX7KwOqIaoqTyESuNyZV3fzJm25bmjtfSUxsIFI8=w120-h120-p-l90-rj", "H", listOf(Color(0xFFFF9100), Color(0xFFFF3D00))),
                    SpotlightArtist("Karan Aujla", "7.5M", "https://lh3.googleusercontent.com/k7sgqqcV5VScaMZtTmS8W_tfouLVBpgyJII0epYE2Vjw1-zzhGgUCV51aHxZn6cmZKKJgUfNlIVpZg=w120-h120-p-l90-rj", "K", listOf(Color(0xFF00E5FF), Color(0xFF2979FF))),
                    SpotlightArtist("MC Stan", "4.8M", "https://i.ytimg.com/vi/qG4l8_WbAis/hqdefault.jpg", "M", listOf(Color(0xFF7C4DFF), Color(0xFFD500F9))),
                    SpotlightArtist("Divine", "8.1M", "https://i.ytimg.com/vi/3AtDnEC4zak/hqdefault.jpg", "D", listOf(Color(0xFF00E676), Color(0xFF00B0FF))),
                    SpotlightArtist("Raftaar", "6.2M", "https://i.ytimg.com/vi/oM-225i_d-g/hqdefault.jpg", "R", listOf(Color(0xFFFF5252), Color(0xFFFF7A00)))
                )
            ),
            ArtistGenrePage(
                genreTitle = "BOLLYWOOD & MELODY",
                categoryTag = "LEGENDS & MAESTROS",
                icon = Icons.Default.GraphicEq,
                artistPool = listOf(
                    SpotlightArtist("A.R. Rahman", "10.1M", "https://lh3.googleusercontent.com/KrXTdVSXgcC7l4QGzaxqDLcWy8BeNL7GvhP9FrytGQXgjaYk26_HMCvrN2wST0B4eoOJ6WLYE1SvQQA=w120-h120-p-l90-rj", "A", listOf(Color(0xFFFFD600), Color(0xFFFF6D00))),
                    SpotlightArtist("Arijit Singh", "42.1M", "https://yt3.googleusercontent.com/ykJkyILKum4B2oudDxjnf5WNenWWZAp-WEz0_CHp4cu0VnqB2-uaNDylItqC68WLXV62rdHDun-ahbg=w120-h120-p-l90-rj", "A", listOf(Color(0xFF7C4DFF), Color(0xFF651FFF))),
                    SpotlightArtist("Shreya Ghoshal", "12.6M", "https://yt3.googleusercontent.com/yfH5_-IYxJmhYRpMa7BDzBaVFZDuJRf_P1tmnpz-TEJI0vawEPoGkViSpNHRHPz846_Dm4iRMSDz8vM=w120-h120-l90-rj", "S", listOf(Color(0xFFFF4081), Color(0xFFF50057))),
                    SpotlightArtist("Atif Aslam", "15.3M", "https://yt3.googleusercontent.com/ykJkyILKum4B2oudDxjnf5WNenWWZAp-WEz0_CHp4cu0VnqB2-uaNDylItqC68WLXV62rdHDun-ahbg=w120-h120-p-l90-rj", "A", listOf(Color(0xFF00B0FF), Color(0xFF00E5FF))),
                    SpotlightArtist("Pritam", "18.4M", "https://i.ytimg.com/vi/mNlvxyKUzVw/hqdefault.jpg", "P", listOf(Color(0xFF00E676), Color(0xFF1DE9B6))),
                    SpotlightArtist("Neha Kakkar", "20.1M", "https://yt3.googleusercontent.com/fFEQDkLmuaBzUyXZAHIaQHUm78MRsN5oatXNscSJfE7e7IOFc3cVUqqgEoVo6mvYhp-3D4zd94nZZgY=w120-h120-p-l90-rj", "N", listOf(Color(0xFFFF4081), Color(0xFFFF80AB)))
                )
            ),
            ArtistGenrePage(
                genreTitle = "PUNJABI POWERHOUSE",
                categoryTag = "GLOBAL DESI WAVE",
                icon = Icons.Rounded.Star,
                artistPool = listOf(
                    SpotlightArtist("Diljit Dosanjh", "11.4M", "https://lh3.googleusercontent.com/4Jd9XSimz29-o12oJKUmgfTx3otHBHlTy0jb3Ace4ti2bz8Nuo59IeneNlM9EKRQXNRpLwk6YoEuNQ=w120-h120-p-l90-rj", "D", listOf(Color(0xFFFF6D00), Color(0xFFFFAB00))),
                    SpotlightArtist("Sidhu Moose Wala", "24.6M", "https://i.ytimg.com/vi/b8n9X0c1_2d/hqdefault.jpg", "S", listOf(Color(0xFF00BFA5), Color(0xFF004D40))),
                    SpotlightArtist("AP Dhillon", "6.8M", "https://i.ytimg.com/vi/3AtDnEC4zak/hqdefault.jpg", "A", listOf(Color(0xFF651FFF), Color(0xFFD500F9))),
                    SpotlightArtist("Guru Randhawa", "10.9M", "https://lh3.googleusercontent.com/4Jd9XSimz29-o12oJKUmgfTx3otHBHlTy0jb3Ace4ti2bz8Nuo59IeneNlM9EKRQXNRpLwk6YoEuNQ=w120-h120-p-l90-rj", "G", listOf(Color(0xFFFF1744), Color(0xFFFF6D00))),
                    SpotlightArtist("B Praak", "8.3M", "https://yt3.googleusercontent.com/Pxv5-0nbw22At8GnATJ2UqYSJUm6bHlhVC4Gf0vM8a3ZSo1e5ct3TOXzB_WdaCVn8JL_iCnnKArrqZo=w120-h120-l90-rj", "B", listOf(Color(0xFF2979FF), Color(0xFF00E5FF))),
                    SpotlightArtist("Shubh", "5.1M", "https://i.ytimg.com/vi/sAzlW4DYvms/hqdefault.jpg", "S", listOf(Color(0xFFFFD600), Color(0xFFFF3D00)))
                )
            ),
            ArtistGenrePage(
                genreTitle = "SOUTH SENSATIONS",
                categoryTag = "POWERHOUSE BEATS",
                icon = Icons.Rounded.Star,
                artistPool = listOf(
                    SpotlightArtist("Anirudh Ravichander", "8.9M", "https://lh3.googleusercontent.com/u_YlAOSU7_M6mI6_4Xo0KIIwI_9pVCnLg0BrdLQsW-KENvVuvnvsq-cHhFrCiD9Ft48jqirgp_gWWwg=w120-h120-p-l90-rj", "A", listOf(Color(0xFFFF3D00), Color(0xFFFF9100))),
                    SpotlightArtist("Sid Sriram", "4.7M", "https://lh3.googleusercontent.com/QxbV6wK_wcQWcBY9rBicZlsl1-gX5M6nGjfNN3BTzgknhaSJ6yhnHW7NmF4dTx0Ch9g9-VTD6YUD2crW=w120-h120-p-l90-rj", "S", listOf(Color(0xFF00E5FF), Color(0xFF2979FF))),
                    SpotlightArtist("Sushin Shyam", "2.1M", "https://i.ytimg.com/vi/f6sE6wJ3q8E/hqdefault.jpg", "S", listOf(Color(0xFF00E676), Color(0xFF1DE9B6))),
                    SpotlightArtist("Santhosh Narayanan", "3.0M", "https://lh3.googleusercontent.com/blpZLT0W8240Tac-bCvRIO9_j1v4kP4g9EdKnx0PKotrKRHr82bJjMvVPVxYHK9bdml5Yo_omphru_rx=w120-h120-p-l90-rj", "S", listOf(Color(0xFF7C4DFF), Color(0xFFD500F9))),
                    SpotlightArtist("Devi Sri Prasad", "5.8M", "https://i.ytimg.com/vi/c7z6xH_2j9c/hqdefault.jpg", "D", listOf(Color(0xFFFF6D00), Color(0xFFFFD600))),
                    SpotlightArtist("Hanumankind", "9.8M", "https://lh3.googleusercontent.com/blpZLT0W8240Tac-bCvRIO9_j1v4kP4g9EdKnx0PKotrKRHr82bJjMvVPVxYHK9bdml5Yo_omphru_rx=w120-h120-p-l90-rj", "H", listOf(Color(0xFF00B0FF), Color(0xFF00E5FF)))
                )
            )
        )
    }

    val liveGenrePages = remember(liveArtists) {
        if (liveArtists.isEmpty()) null
        else {
            liveArtists.chunked(3).filter { it.size == 3 }.mapIndexed { pageIdx, chunk ->
                val rankStart = pageIdx * 3 + 1
                val rankEnd = rankStart + chunk.size - 1
                ArtistGenrePage(
                    genreTitle = if (pageIdx == 0) "TOP TRENDING ON YOUTUBE" else "TRENDING ARTISTS #$rankStart - #$rankEnd",
                    categoryTag = if (pageIdx == 0) "LIVE CHARTS • GLOBAL & REGIONAL" else "VERIFIED YOUTUBE CREATORS",
                    icon = if (pageIdx == 0) Icons.Rounded.Star else Icons.Default.GraphicEq,
                    artistPool = chunk.map { ca ->
                        val initial = ca.title.trim().firstOrNull()?.uppercase() ?: "A"
                        val subs = if (ca.subscribers.isNotBlank()) {
                            ca.subscribers.replace("subscribers", "", ignoreCase = true).trim()
                        } else "#${ca.rank} Trending"
                        val grad = when (ca.rank.toIntOrNull()?.rem(4)) {
                            0 -> listOf(Color(0xFFFF1744), Color(0xFFD500F9))
                            1 -> listOf(Color(0xFF00E5FF), Color(0xFF2979FF))
                            2 -> listOf(Color(0xFFFFD600), Color(0xFFFF6D00))
                            else -> listOf(Color(0xFF7C4DFF), Color(0xFF651FFF))
                        }
                        SpotlightArtist(
                            name = ca.title,
                            subs = subs,
                            imageUrl = ca.thumbnailUrl,
                            initial = initial,
                            gradientColors = grad
                        )
                    }
                )
            }
        }
    }

    val effectivePages = liveGenrePages ?: genrePages
    val pagerState = rememberPagerState(pageCount = { effectivePages.size })

    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        // Section Header with Title and "Change" + "Swipe" controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (liveGenrePages != null) "TRENDING ARTISTS" else "SPOTLIGHT ARTISTS",
                color = if (appColors.isDark) Color(0xFFFFD600) else appColors.accentPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Interactive "Change" Button -> Cycles all pages to get new results every time!
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(appColors.surfaceElevated)
                        .border(1.dp, appColors.surfaceBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable {
                            shuffleOffset = (shuffleOffset + 1) % 4
                            coroutineScope.launch {
                                pagerState.animateScrollToPage((pagerState.currentPage + 1) % effectivePages.size)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Change",
                            tint = if (appColors.isDark) Color(0xFFFFD600) else appColors.accentPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Change",
                            color = appColors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Page indicator badge e.g. "1/6"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(appColors.surfaceElevated)
                        .border(1.dp, appColors.surfaceBorder.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${effectivePages.size}",
                        color = appColors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Horizontal Pager for swinging left/right to see different genres & artists
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            val page = effectivePages[pageIndex]
            val pool = page.artistPool
            val displayArtists = if (liveGenrePages != null) pool else {
                val startIdx = (shuffleOffset * 3) % pool.size
                List(3) { i -> pool[(startIdx + i) % pool.size] }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(listOf(appColors.surfaceElevated, appColors.surface))
                    )
                    .border(1.dp, appColors.surfaceBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = page.genreTitle,
                                color = appColors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = page.categoryTag,
                                color = appColors.textSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = Color(0xFFFF4081),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        displayArtists.forEachIndexed { index, artist ->
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(88.dp)
                                        .background(if (appColors.isDark) Color(0x22FFFFFF) else appColors.surfaceBorder)
                                )
                            }
                            val matchedLive = liveArtists.firstOrNull { it.title.equals(artist.name, ignoreCase = true) }
                            val currentScreenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
                            val circleSize = if (currentScreenWidth < 360.dp) 54.dp else 60.dp
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .clickable {
                                        if (matchedLive != null) {
                                            onOpenChartArtist(matchedLive)
                                        } else {
                                            onOpenArtist(artist.name)
                                        }
                                    }
                            ) {
                                // Circular photo with vibrant gradient fallback avatar (never hollow/black!)
                                Box(
                                    modifier = Modifier
                                        .size(circleSize)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(artist.gradientColors))
                                        .border(
                                            width = 1.5.dp,
                                            brush = Brush.sweepGradient(
                                                listOf(Color(0xFFFF4081), Color(0xFF7C4DFF), Color(0xFFFF4081))
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Bold initial is always behind image so failure never produces black void
                                    Text(
                                        text = artist.initial,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    AsyncImage(
                                        model = artist.imageUrl,
                                        contentDescription = artist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = artist.name,
                                    color = appColors.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = artist.subs,
                                    color = appColors.textSecondary,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(6.dp))

                                // "Explore >" pill button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (appColors.isDark) Color(0x2EFFFFFF) else appColors.accentPrimary.copy(alpha = 0.12f))
                                        .border(0.8.dp, if (appColors.isDark) Color.Transparent else appColors.accentPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (matchedLive != null) {
                                                onOpenChartArtist(matchedLive)
                                            } else {
                                                onOpenArtist(artist.name)
                                            }
                                        }
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Explore >",
                                        color = if (appColors.isDark) Color.White else appColors.accentPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Dot Indicators for all pages
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(effectivePages.size.coerceAtMost(10)) { dotIdx ->
                            val isSelected = pagerState.currentPage == dotIdx
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(4.dp)
                                    .width(if (isSelected) 18.dp else 6.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isSelected) Color(0xFFFFD600) else Color(0x44FFFFFF))
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(dotIdx)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── YOUTUBE MIX PREVIEW CARD (Dynamic Rotating Cover + Refresh Mix + Delete Option) ─────────────
@Composable
fun YouTubeMixPreviewCard(
    title: String,
    subtitle: String,
    coverUrl: String,
    tracks: List<UnifiedTrack>,
    onPlayAll: () -> Unit,
    onTrackClick: (UnifiedTrack) -> Unit,
    onSeeMore: () -> Unit,
    onDeleteTrack: ((UnifiedTrack) -> Unit)? = null,
    onRotateMix: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    var isExpanded by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    val displayTracks = if (isExpanded) tracks.take(15) else tracks.take(3)

    // Dynamic auto-changing cover: cycles smoothly through top trending songs every 3.5s
    var activeCoverIndex by remember(tracks) { mutableIntStateOf(0) }
    LaunchedEffect(tracks) {
        if (tracks.size > 1) {
            while (true) {
                delay(3500)
                activeCoverIndex = (activeCoverIndex + 1) % minOf(tracks.size, 6)
            }
        }
    }
    val currentCover = tracks.getOrNull(activeCoverIndex)?.thumbnailUrl?.ifBlank { coverUrl } ?: coverUrl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (appColors.isDark) Color(0x15FFFFFF) else appColors.surfaceElevated)
            .border(1.dp, if (appColors.isDark) Color(0x24FFFFFF) else appColors.surfaceBorder, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            // Header: Cover + Title + Subtitle + Refresh Mix Button
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (appColors.isDark) Color(0xFF282828) else Color(0xFFE2E4E8))
                        .clickable { onPlayAll() }
                ) {
                    AsyncImage(
                        model = currentCover,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Auto-rotating badge indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPlayAll() }
                ) {
                    Text(
                        text = title,
                        color = appColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = appColors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${tracks.size.coerceAtLeast(15)} songs • Auto-updating",
                        color = appColors.textMuted,
                        fontSize = 12.sp
                    )
                }

                if (onRotateMix != null) {
                    IconButton(
                        onClick = onRotateMix,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (appColors.isDark) Color(0x22FFFFFF) else appColors.surfaceBorder.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Rotate Mix",
                            tint = appColors.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3-4 Song Preview Rows with 3-dot menus (Scrolls within block when expanded)
            val listModifier = if (isExpanded) {
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxWidth()
            }
            Column(
                modifier = listModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayTracks.forEach { track ->
                    var showRowMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTrackClick(track) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = appColors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${track.artist} • YouTube",
                                color = appColors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showRowMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "More",
                                    tint = appColors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showRowMenu,
                                onDismissRequest = { showRowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Play Song") },
                                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                                    onClick = {
                                        showRowMenu = false
                                        onTrackClick(track)
                                    }
                                )
                                if (onDeleteTrack != null) {
                                    DropdownMenuItem(
                                        text = { Text("Hide / Delete from Mix", color = Color(0xFFFF5252)) },
                                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5252)) },
                                        onClick = {
                                            showRowMenu = false
                                            onDeleteTrack(track)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // "See more (10) v" expander button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) "Show less" else "See more (${(tracks.size - 3).coerceAtLeast(10)})",
                        color = appColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Bottom Action Row: Play, Radio, Bookmark (Matching Screenshot 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Play Button (Matching Screenshot 1)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(appColors.accentPrimary)
                        .clickable { onPlayAll() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play Mix",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Radio Button ((•))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.2.dp, if (appColors.isDark) Color(0x44FFFFFF) else appColors.surfaceBorder, CircleShape)
                        .clickable { onSeeMore() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Mix Radio",
                        tint = appColors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Bookmark / Save Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.2.dp, if (isSaved) Color(0xFF00E676) else if (appColors.isDark) Color(0x44FFFFFF) else appColors.surfaceBorder, CircleShape)
                        .clickable { isSaved = !isSaved },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Save Playlist",
                        tint = if (isSaved) Color(0xFF00E676) else appColors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CuratedPlaylistCard(
    playlist: MusiXServerRepository.CuratedPlaylist,
    onClick: () -> Unit
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(168.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(appColors.surfaceElevated)
        ) {
            AsyncImage(
                model = playlist.coverUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Top-Left Play Badge (Matching Image 1)
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Open playlist",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (playlist.tracks.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${playlist.tracks.size} songs",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.title,
            color = appColors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (playlist.description.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = playlist.description,
                color = appColors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun sourceBadge(sourceName: String): Pair<ImageVector, Color> = when (sourceName) {
    "YouTube"     -> Icons.Filled.PlayArrow to Color(0xFFFF3B30)
    "JioSaavn"    -> Icons.Filled.MusicNote to Color(0xFF2ED8A7)
    "Vimeo"       -> Icons.Filled.Videocam to Color(0xFF17C3E6)
    "Apple Music" -> Icons.Filled.Album to Color(0xFFFA5F91)
    "Spotify"     -> Icons.Filled.GraphicEq to Color(0xFF1ED760)
    else          -> Icons.Filled.MusicNote to Color(0xFF888888)
}

val LocalOnDeleteTrack = compositionLocalOf<((UnifiedTrack) -> Unit)?> { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnifiedMusicCard(
    track: UnifiedTrack,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    isPreparing: Boolean = false,
    cardWidth: androidx.compose.ui.unit.Dp = 114.dp,
    onDelete: (() -> Unit)? = null
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    var showMenu by remember { mutableStateOf(false) }
    val effectiveOnDelete = onDelete ?: LocalOnDeleteTrack.current?.let { handler -> { handler(track) } }

    Column(modifier = Modifier.width(cardWidth)) {
        Box(
            modifier = Modifier
                .size(cardWidth)
                .clip(RoundedCornerShape(12.dp))
                .background(appColors.surfaceElevated)
                .combinedClickable(
                    onClick = { onPlay() },
                    onLongClick = { showMenu = true }
                )
        ) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // YouTube Music signature Top-Left Play Badge (Matching Image 1)
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPreparing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Top-Right 3-dots menu button for options including Delete / Hide
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Now") },
                        leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onPlay()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isDownloaded) "Downloaded" else "Download") },
                        leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                        enabled = !isDownloading && !isDownloaded,
                        onClick = {
                            showMenu = false
                            onDownload()
                        }
                    )
                    if (effectiveOnDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Hide / Delete from Feed", color = Color(0xFFFF5252)) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5252)) },
                            onClick = {
                                showMenu = false
                                effectiveOnDelete()
                            }
                        )
                    }
                }
            }

            // Bottom-Right Download Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(enabled = !isDownloading && !isDownloaded) { onDownload() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isDownloading -> CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 1.5.dp,
                        color = Color.White
                    )
                    isDownloaded -> Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(15.dp)
                    )
                    else -> Icon(
                        Icons.Rounded.Download,
                        contentDescription = "Download",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            track.title,
            color = appColors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            track.artist,
            color = appColors.textSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CategorySection(
    title: String,
    items: List<MusicCardItem>,
    onItemClick: (MusicCardItem) -> Unit
) {
    Column {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items) { item ->
                Column(
                    modifier = Modifier
                        .width(130.dp)
                        .clickable { onItemClick(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF212121))
                    ) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = Icons.Rounded.PlayCircleFilled,
                            contentDescription = "Play",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(24.dp)
                                .padding(4.dp)
                                .align(Alignment.TopStart)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.subtitle,
                        color = Color(0xFF888888),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistSpotlightItem(
    artist: com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Color(0xFF222222))
                .border(2.dp, Brush.sweepGradient(listOf(Color(0xFFFF1744), Color(0xFF7C4DFF), Color(0xFFFF1744))), CircleShape)
        ) {
            AsyncImage(
                model = artist.thumbnailUrl,
                contentDescription = artist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = artist.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (artist.subscribers.isNotBlank()) artist.subscribers else "Artist",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                "Explore ›",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SongSpotlightItem(
    song: YouTubeSearchResult,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF222222))
                .border(
                    1.5.dp,
                    Brush.sweepGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF5722), Color(0xFFFFD54F))),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = song.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (song.channelTitle.isNotBlank()) song.channelTitle else "Trending",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                if (song.duration.isNotBlank()) song.duration else "Play ▶",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}




// ── 4-SQUARE SOUTH & REGIONAL CATEGORIES (Malayalam, Tamil, Telugu, Hindi) ──
data class RegionalSquareCategory(
    val englishName: String,
    val nativeScript: String,
    val subtitle: String,
    val coverUrl: String,
    val gradient: List<Color>,
    val query: String
)

@Composable
fun SouthRegionalCategoriesSection(
    onSelectLanguage: (String, String) -> Unit,
    malayalamTracks: List<YouTubeSearchResult> = emptyList(),
    tamilTracks: List<YouTubeSearchResult> = emptyList(),
    teluguTracks: List<YouTubeSearchResult> = emptyList(),
    hindiTracks: List<YouTubeSearchResult> = emptyList(),
    modifier: Modifier = Modifier
) {
    val categories = remember(malayalamTracks, tamilTracks, teluguTracks, hindiTracks) {
        listOf(
            RegionalSquareCategory(
                englishName = "Malayalam",
                nativeScript = "മലയാളം",
                subtitle = malayalamTracks.firstOrNull()?.title ?: "Mollywood Top Hits",
                coverUrl = malayalamTracks.firstOrNull()?.thumbnailUrl ?: "https://cdn-images.dzcdn.net/images/cover/88a8de1995af3f04de605e1953bcf1f3/250x250-000000-80-0-0.jpg",
                gradient = listOf(Color(0xFF05342B), Color(0xFF0E4A3E), Color(0xFF00695C)),
                query = "malayalam trending songs 2026"
            ),
            RegionalSquareCategory(
                englishName = "Tamil",
                nativeScript = "தமிழ்",
                subtitle = tamilTracks.firstOrNull()?.title ?: "Kollywood Chartbusters",
                coverUrl = tamilTracks.firstOrNull()?.thumbnailUrl ?: "https://cdn-images.dzcdn.net/images/cover/782c35a76a4abce97963e229c3b0e735/250x250-000000-80-0-0.jpg",
                gradient = listOf(Color(0xFF38081E), Color(0xFF560D2D), Color(0xFF880E4F)),
                query = "tamil trending songs 2026"
            ),
            RegionalSquareCategory(
                englishName = "Telugu",
                nativeScript = "తెలుగు",
                subtitle = teluguTracks.firstOrNull()?.title ?: "Tollywood Blockbusters",
                coverUrl = teluguTracks.firstOrNull()?.thumbnailUrl ?: "https://cdn-images.dzcdn.net/images/cover/00548bffa974509e8b2bf7ccdf2674b1/250x250-000000-80-0-0.jpg",
                gradient = listOf(Color(0xFF3E1A04), Color(0xFF6E2D07), Color(0xFFBF360C)),
                query = "telugu trending songs 2026"
            ),
            RegionalSquareCategory(
                englishName = "Hindi",
                nativeScript = "हिंदी",
                subtitle = hindiTracks.firstOrNull()?.title ?: "Bollywood & Desi Indie",
                coverUrl = hindiTracks.firstOrNull()?.thumbnailUrl ?: "https://cdn-images.dzcdn.net/images/cover/32715eb33ab5e20b6c5be10da46a852c/250x250-000000-80-0-0.jpg",
                gradient = listOf(Color(0xFF140D36), Color(0xFF24165C), Color(0xFF4A148C)),
                query = "hindi trending songs 2026"
            )
        )
    }

    Column(modifier = modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SOUTH & REGIONAL MUSIC",
                    color = Color(0xFFFFD600),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Top hits in Malayalam, Tamil, Telugu & Hindi",
                    color = Color(0xFF88A8B3),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1FFFFFFF))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "4 Categories",
                    color = Color(0xFFFFD600),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 2x2 Square Cards Grid with Prominent Cover Artwork
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Malayalam & Tamil
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RegionalSquareCard(
                    category = categories[0],
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectLanguage(categories[0].englishName, categories[0].query) }
                )
                RegionalSquareCard(
                    category = categories[1],
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectLanguage(categories[1].englishName, categories[1].query) }
                )
            }

            // Row 2: Telugu & Hindi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RegionalSquareCard(
                    category = categories[2],
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectLanguage(categories[2].englishName, categories[2].query) }
                )
                RegionalSquareCard(
                    category = categories[3],
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectLanguage(categories[3].englishName, categories[3].query) }
                )
            }
        }
    }
}

@Composable
fun RegionalSquareCard(
    category: RegionalSquareCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(category.gradient))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        // High-contrast semi-transparent album cover backdrop
        AsyncImage(
            model = category.coverUrl,
            contentDescription = category.englishName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
        )

        // Gradient scrim to ensure text and buttons pop with ultra clarity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x77000000),
                            Color(0x44000000),
                            Color(0xEE000000)
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Native script badge & Frosted Play Mini-FAB
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x88000000))
                        .border(0.5.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = category.nativeScript,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Frosted Play Mini-FAB
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .border(1.dp, Color(0x88FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Bottom: Title & Subtitle over dark scrim
            Column {
                Text(
                    text = category.englishName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.3.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = category.subtitle,
                    color = Color(0xFFDDDDDD),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


// ── AUTHENTIC YOUTUBE MUSIC CARDS (Speed Dial, Mixed for You, Community) ────

/**
 * 3-Page Swipeable Speed Dial matching official YouTube Music home tab.
 * Page 0: 3x3 Grid of Songs (tap to play)
 * Page 1: 3x3 Grid of Albums (tap to explore album)
 * Page 2: 3x3 Grid of Artists (tap to explore artist catalog)
 * With real swipe gestures and live animated indicator dots.
 */
@Composable
fun SpeedDialShelf(
    tracks: List<UnifiedTrack>,
    albums: List<YouTubeSearchResult> = emptyList(),
    artists: List<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist> = emptyList(),
    onPlayTrack: (UnifiedTrack) -> Unit,
    onOpenAlbum: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem) -> Unit = {},
    onOpenArtist: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist) -> Unit = {},
    onOpenSearchWithQuery: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty() && albums.isEmpty() && artists.isEmpty()) return
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    // Fallback albums if explore data is still loading
    val fallbackAlbums = remember {
        listOf(
            YouTubeSearchResult("f6sE6wJ3q8E", "Aavesham (Original Soundtrack)", "Sushin Shyam", "https://i.ytimg.com/vi/f6sE6wJ3q8E/hqdefault.jpg", "Album"),
            YouTubeSearchResult("sAzlW4DYvms", "Animal (Original Motion Picture)", "Manan Bhardwaj", "https://i.ytimg.com/vi/sAzlW4DYvms/hqdefault.jpg", "Album"),
            YouTubeSearchResult("c7z6xH_2j9c", "Pushpa 2: The Rule", "Devi Sri Prasad", "https://i.ytimg.com/vi/c7z6xH_2j9c/hqdefault.jpg", "Album"),
            YouTubeSearchResult("YxWlaYCA8MU", "Leo (Original Motion Picture)", "Anirudh Ravichander", "https://i.ytimg.com/vi/YxWlaYCA8MU/hqdefault.jpg", "Album"),
            YouTubeSearchResult("bdX_jC-xMOU", "Rockstar", "A.R. Rahman", "https://i.ytimg.com/vi/bdX_jC-xMOU/hqdefault.jpg", "Album"),
            YouTubeSearchResult("vD4B3sR3q2k", "Kabir Singh", "Sachet-Parampara", "https://i.ytimg.com/vi/vD4B3sR3q2k/hqdefault.jpg", "Album"),
            YouTubeSearchResult("m0Vl4c7_FwA", "Aashiqui 2", "Mithoon, Ankit Tiwari", "https://i.ytimg.com/vi/m0Vl4c7_FwA/hqdefault.jpg", "Album"),
            YouTubeSearchResult("jH1vX2b_9Zk", "Devara: Part 1", "Anirudh Ravichander", "https://i.ytimg.com/vi/jH1vX2b_9Zk/hqdefault.jpg", "Album"),
            YouTubeSearchResult("b8n9X0c1_2d", "Jawan", "Anirudh Ravichander", "https://i.ytimg.com/vi/b8n9X0c1_2d/hqdefault.jpg", "Album")
        )
    }

    // Fallback artists with high-reliability avatars if charts data is still loading
    val fallbackArtists = remember {
        listOf(
            com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist("1", "Arijit Singh", "UCVGomUS__PL0c4jDXa0QwXA", "42M", "https://yt3.googleusercontent.com/ykJkyILKum4B2oudDxjnf5WNenWWZAp-WEz0_CHp4cu0VnqB2-uaNDylItqC68WLXV62rdHDun-ahbg=w120-h120-p-l90-rj"),
            com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist("2", "Anirudh Ravichander", "UCK-E95XlAlzJtXKIrF4-hjA", "8.9M", "https://lh3.googleusercontent.com/u_YlAOSU7_M6mI6_4Xo0KIIwI_9pVCnLg0BrdLQsW-KENvVuvnvsq-cHhFrCiD9Ft48jqirgp_gWWwg=w120-h120-p-l90-rj"),
            com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist("3", "Shreya Ghoshal", "UCSPd0Wvy-02QYbZM5QckSag", "12.6M", "https://yt3.googleusercontent.com/yfH5_-IYxJmhYRpMa7BDzBaVFZDuJRf_P1tmnpz-TEJI0vawEPoGkViSpNHRHPz846_Dm4iRMSDz8vM=w120-h120-l90-rj"),
            com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist("4", "A.R. Rahman", "UC4nQHrz0kvM7LGc7GOnc72g", "10.1M", "https://lh3.googleusercontent.com/KrXTdVSXgcC7l4QGzaxqDLcWy8BeNL7GvhP9FrytGQXgjaYk26_HMCvrN2wST0B4eoOJ6WLYE1SvQQA=w120-h120-p-l90-rj"),
            com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist("5", "Diljit Dosanjh", "UCdWuR07og626xwU93eSCh9A", "11.4M", "https://lh3.googleusercontent.com/4Jd9XSimz29-o12oJKUmgfTx3otHBHlTy0jb3Ace4ti2bz8Nuo59IeneNlM9EKRQXNRpLwk6YoEuNQ=w120-h120-p-l90-rj"),
            com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist("6", "Sid Sriram", "UCwzzSiogpjsYBMoyVcwDXwQ", "4.7M", "https://lh3.googleusercontent.com/QxbV6wK_wcQWcBY9rBicZlsl1-gX5M6nGjfNN3BTzgknhaSJ6yhnHW7NmF4dTx0Ch9g9-VTD6YUD2crW=w120-h120-p-l90-rj"),
            com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist("7", "Badshah", "UCGPCYz1FTl_dvFFnzQTQzjw", "14.2M", "https://lh3.googleusercontent.com/Ss_NEfGmfpwXCiuoNxiKxWAoU3M484SwZ4UmahATX7KwOqIaoqTyESuNyZV3fzJm25bmjtfSUxsIFI8=w120-h120-p-l90-rj"),
            com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist("8", "Karan Aujla", "UCSmK5WX5U4gdtebWjoL81og", "7.5M", "https://lh3.googleusercontent.com/k7sgqqcV5VScaMZtTmS8W_tfouLVBpgyJII0epYE2Vjw1-zzhGgUCV51aHxZn6cmZKKJgUfNlIVpZg=w120-h120-p-l90-rj"),
            com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist("9", "Hanumankind", "UCAAqmCmhXmhQUegwYDkvaSA", "9.8M", "https://lh3.googleusercontent.com/blpZLT0W8240Tac-bCvRIO9_j1v4kP4g9EdKnx0PKotrKRHr82bJjMvVPVxYHK9bdml5Yo_omphru_rx=w120-h120-p-l90-rj")
        )
    }

    val dialTracks = remember(tracks) { tracks.take(9) }
    val dialAlbums = remember(albums, fallbackAlbums) { (albums.ifEmpty { fallbackAlbums }).take(9) }
    val dialArtists = remember(artists, fallbackArtists) { (artists.ifEmpty { fallbackArtists }).take(9) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Speed dial",
                    color = appColors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (pagerState.currentPage) {
                        0 -> "• Songs"
                        1 -> "• Albums"
                        else -> "• Artists"
                    },
                    color = appColors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Swipe indicator pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x18FFFFFF))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/3 Swipe >",
                    color = appColors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Real HorizontalPager for 3x3 grids (Swipe: Songs -> Albums -> Artists)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 500.dp)
        ) { page ->
            when (page) {
                0 -> {
                    // Page 0: Songs 3x3 Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (row in 0 until 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0 until 3) {
                                    val index = row * 3 + col
                                    val track = dialTracks.getOrNull(index)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(appColors.surfaceElevated)
                                            .clickable(enabled = track != null) {
                                                track?.let { onPlayTrack(it) }
                                            }
                                    ) {
                                        if (track != null) {
                                            AsyncImage(
                                                model = track.thumbnailUrl,
                                                contentDescription = track.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            // Bottom dark scrim with song title
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                        )
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = track.title,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Page 1: Albums 3x3 Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (row in 0 until 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0 until 3) {
                                    val index = row * 3 + col
                                    val album = dialAlbums.getOrNull(index)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(appColors.surfaceElevated)
                                            .clickable(enabled = album != null) {
                                                album?.let {
                                                    onOpenAlbum(
                                                        com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem(
                                                            title = it.title,
                                                            browseId = it.videoId,
                                                            audioPlaylistId = it.videoId,
                                                            thumbnailUrl = it.thumbnailUrl,
                                                            type = it.duration.ifBlank { "Album" },
                                                            artistName = it.channelTitle
                                                        )
                                                    )
                                                }
                                            }
                                    ) {
                                        if (album != null) {
                                            AsyncImage(
                                                model = album.thumbnailUrl,
                                                contentDescription = album.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            // Top Album badge
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(5.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.Black.copy(alpha = 0.65f))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "ALBUM",
                                                    color = Color(0xFFFFD600),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            // Bottom dark scrim with album title
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                                                        )
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = album.title,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Page 2: Artists 3x3 Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (row in 0 until 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0 until 3) {
                                    val index = row * 3 + col
                                    val artist = dialArtists.getOrNull(index)
                                    val artistInitial = artist?.title?.trim()?.firstOrNull()?.uppercase() ?: "A"
                                    val gradColors = when (index % 4) {
                                        0 -> listOf(Color(0xFFFF1744), Color(0xFFD500F9))
                                        1 -> listOf(Color(0xFF00E5FF), Color(0xFF2979FF))
                                        2 -> listOf(Color(0xFFFFD600), Color(0xFFFF6D00))
                                        else -> listOf(Color(0xFF7C4DFF), Color(0xFF651FFF))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(appColors.surfaceElevated)
                                            .clickable(enabled = artist != null) {
                                                artist?.let { onOpenArtist(it) }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (artist != null) {
                                            // Circular avatar in card
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize(0.85f)
                                                        .clip(CircleShape)
                                                        .background(Brush.linearGradient(gradColors)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = artistInitial,
                                                        color = Color.White,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    AsyncImage(
                                                        model = artist.thumbnailUrl,
                                                        contentDescription = artist.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape)
                                                    )
                                                }
                                            }
                                            // Bottom subtle title
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                        )
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = artist.title,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Real Interactive Pager Indicator Dots (Tapping scrolls to page)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                val isSelected = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) appColors.textPrimary else appColors.textSecondary.copy(alpha = 0.35f))
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(i)
                            }
                        }
                )
            }
        }
    }
}

/**
 * Authentic "Mixed for you" Supermix Card.
 * Displays 2x2 artwork mosaic + title, 3 song rows with play count & 3-dots, plus big play & save buttons.
 */
@Composable
fun SupermixCard(
    mixTitle: String,
    subtitle: String,
    tracks: List<UnifiedTrack>,
    onPlayMix: () -> Unit,
    onPlayTrack: (UnifiedTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    val songRows = remember(tracks) { tracks.take(3) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        color = appColors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.surfaceBorder.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Section: 2x2 artwork mosaic + Mix title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayMix() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 2x2 Artwork Mosaic
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.weight(1f)) {
                            AsyncImage(
                                model = tracks.getOrNull(0)?.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            AsyncImage(
                                model = tracks.getOrNull(1)?.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        Row(Modifier.weight(1f)) {
                            AsyncImage(
                                model = tracks.getOrNull(2)?.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            AsyncImage(
                                model = tracks.getOrNull(3)?.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mixTitle,
                        color = appColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = appColors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Middle Section: 3 song rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                songRows.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPlayTrack(track) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = appColors.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            val playCount = remember(track.key) {
                                val plays = ((track.key.hashCode() and 0x7FFFFFFF) % 900 + 50)
                                "${plays}K plays"
                            }
                            Text(
                                text = "${track.artist.ifBlank { "YouTube Music" }} • $playCount",
                                color = appColors.textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = appColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Bottom Section: Circular Play Button + Save Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlayMix,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(appColors.accentPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Mix",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                IconButton(
                    onClick = { /* Saved */ },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(appColors.surface)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BookmarkBorder,
                        contentDescription = "Save Mix",
                        tint = appColors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2x2 Mosaic Card for "From the community" playlists.
 */
@Composable
fun CommunityMosaicCard(
    playlistTitle: String,
    curator: String,
    views: String,
    tracks: List<UnifiedTrack>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current

    Column(
        modifier = modifier
            .width(150.dp)
            .clickable { onClick() }
    ) {
        // 2x2 Grid artwork
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(appColors.surfaceElevated)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    AsyncImage(
                        model = tracks.getOrNull(0)?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    AsyncImage(
                        model = tracks.getOrNull(1)?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Row(Modifier.weight(1f)) {
                    AsyncImage(
                        model = tracks.getOrNull(2)?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    AsyncImage(
                        model = tracks.getOrNull(3)?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = playlistTitle,
            color = appColors.textPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$curator • $views",
            color = appColors.textSecondary,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
