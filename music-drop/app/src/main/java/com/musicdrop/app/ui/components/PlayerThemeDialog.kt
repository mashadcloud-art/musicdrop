package com.musicdrop.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.musicdrop.app.ui.theme.PlayerSkinLayout
import com.musicdrop.app.ui.theme.PlayerThemeId
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerThemeDialog(
    currentTheme: PlayerThemeId,
    onSelectTheme: (PlayerThemeId) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = remember { PlayerThemeId.values().toList() }
    val initialIndex = remember { themes.indexOf(currentTheme).coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { themes.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F14))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ── 1. Top Bar: < Player theme ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Player theme",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── 2. Carousel of Live Player Skins ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 42.dp),
                        pageSpacing = 16.dp,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val theme = themes[pageIndex]
                        val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue
                        val scale = 1f - (pageOffset * 0.08f).coerceIn(0f, 0.15f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.96f)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = (1f - (pageOffset * 0.3f)).coerceIn(0.6f, 1f)
                                }
                                .shadow(16.dp, RoundedCornerShape(28.dp))
                                .clip(RoundedCornerShape(28.dp))
                                .background(theme.getBackgroundBrush())
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                        ) {
                            ThemeSkinCardContent(theme = theme)
                        }
                    }
                }

                // ── 3. Theme Title & Subtitle Badge ──
                val activeTheme = themes[pagerState.currentPage]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = activeTheme.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activeTheme.defaultSkin.subtitle,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.5.sp
                    )
                }

                // ── 4. Large Orange/Amber APPLY Button ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            onSelectTheme(themes[pagerState.currentPage])
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = "APPLY",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSkinCardContent(theme: PlayerThemeId) {
    when (theme.defaultSkin) {
        PlayerSkinLayout.ROUNDED_CARD -> RoundedCardPreview(theme)
        PlayerSkinLayout.RADIAL_RING -> RadialRingPreview(theme)
        PlayerSkinLayout.RADIAL_DRAWER -> RadialDrawerPreview(theme)
        PlayerSkinLayout.IMMERSIVE_DRAWER -> ImmersiveDrawerPreview(theme)
    }
}

/** Layout 1: Rounded Card with Center Pill Seekbar (Image 1) */
@Composable
private fun RoundedCardPreview(theme: PlayerThemeId) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text("Song | Lyrics", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Row {
                Icon(Icons.Rounded.Checkroom, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // Center Album Artwork
        Box(
            modifier = Modifier
                .size(190.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF2C1B10)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
        }

        // Track Info & Side Utilities
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Over the Horizon", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("hello Talk", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.FavoriteBorder, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Icon(Icons.Rounded.Tune, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // Seekbar with floating badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .align(Alignment.CenterStart)
                    .height(4.dp)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("1:20 / 3:14", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Shuffle, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Pause, null, tint = Color.Black, modifier = Modifier.size(28.dp))
            }
            Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Icon(Icons.Rounded.Repeat, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** Layout 2: Radial Clock / Vinyl Ring Skin (Image 2) */
@Composable
private fun RadialRingPreview(theme: PlayerThemeId) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Text("Song | Lyrics", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }

        // Radial Circular Progress Ring
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    style = Stroke(width = 4.dp.toPx())
                )
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = 145f,
                    useCenter = false,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF261D36)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "1:20",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Track Info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Over the Horizon", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("hello Talk", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }

        // 5 Icons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(Icons.Filled.FavoriteBorder, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Icon(Icons.Rounded.Tune, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // Bottom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Shuffle, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Pause, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Icon(Icons.Rounded.Repeat, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** Layout 3: Radial Ring with Up Next Queue Drawer (Image 3) */
@Composable
private fun RadialDrawerPreview(theme: PlayerThemeId) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Ring with side controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(26.dp))
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color.White.copy(alpha = 0.2f), style = Stroke(4.dp.toPx()))
                    drawArc(color = Color(0xFFF59E0B), startAngle = -90f, sweepAngle = 130f, useCenter = false, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                }
                Box(
                    modifier = Modifier
                        .size(114.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF221626)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("1:20", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }

        // Title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Over the Horizon", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("hello Talk", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }

        // 5 Icons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Filled.FavoriteBorder, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Icon(Icons.Rounded.Tune, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }

        // Up Next Drawer Sheet (Card at bottom with songs)
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEA580C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("All The Way Up", color = Color.Black, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text("Ed Records • HD", color = Color.Gray, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0284C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Haunting Winter Snow", color = Color.Black, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text("Zac Nelson • HD", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

/** Layout 4: Scenic Wallpaper Immersive (Image 4) */
@Composable
private fun ImmersiveDrawerPreview(theme: PlayerThemeId) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title at top
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Over the Horizon", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("hello Talk", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }

            // Controls in center
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Pause, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }

            // Up Next Sheet at bottom
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD97706)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.GraphicEq, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hidden Eye", color = Color.Black, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Text("Lord of War", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF475569)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Blue Wolf", color = Color.Black, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Text("Retro Future", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
