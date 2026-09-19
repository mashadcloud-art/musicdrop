package com.musicdrop.app.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            val prevLight = insetsController.isAppearanceLightStatusBars
            insetsController.isAppearanceLightStatusBars = false
            onDispose { insetsController.isAppearanceLightStatusBars = prevLight }
        } else {
            onDispose {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F14))
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .padding(top = 10.dp)
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

                // ── 4. Large Orange/Amber APPLY Button (Matching Screenshot 3) ──
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
                        shape = RoundedCornerShape(26.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "👑",
                                fontSize = 18.sp
                            )
                            Text(
                                text = "APPLY",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSkinCardContent(theme: PlayerThemeId) {
    when (theme.defaultSkin) {
        PlayerSkinLayout.VINYL_TURNTABLE -> VinylTurntablePreview(theme)
        PlayerSkinLayout.ROUNDED_CARD -> RoundedCardPreview(theme)
        PlayerSkinLayout.RADIAL_RING -> RadialRingPreview(theme)
        PlayerSkinLayout.RADIAL_DRAWER -> RadialDrawerPreview(theme)
        PlayerSkinLayout.IMMERSIVE_DRAWER -> ImmersiveDrawerPreview(theme)
    }
}

/** Layout: Vinyl Turntable with Grooved Disc & Tonearm Needle (Matching Screenshot 3) */
@Composable
private fun VinylTurntablePreview(theme: PlayerThemeId) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin_preview")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Orange Crown Corner Ribbon (Matching Screenshot 3 top left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(48.dp)
                .clip(RoundedCornerShape(bottomEnd = 24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("👑", fontSize = 16.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Pill: Song | Lyrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Song", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text(" | ", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        Text("Lyrics", color = Color.White.copy(alpha = 0.7f), fontSize = 11.5.sp)
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                }
                Row {
                    Icon(Icons.Rounded.Checkroom, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // Center: Rotating Vinyl Record Disc with Grooves & Tonearm
            Box(
                modifier = Modifier.size(195.dp),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl Disc Canvas (Black grooved record)
                Canvas(
                    modifier = Modifier
                        .size(182.dp)
                        .rotate(rotation)
                ) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Outer vinyl base
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1E1E24), Color(0xFF121216), Color(0xFF070709))
                        ),
                        radius = radius,
                        center = center
                    )

                    // Concentric vinyl sound grooves
                    for (i in 1..9) {
                        val grooveRadius = radius * (0.42f + (i * 0.058f))
                        drawCircle(
                            color = Color.White.copy(alpha = if (i % 2 == 0) 0.08f else 0.04f),
                            radius = grooveRadius,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Vinyl rim sheen highlight
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f),
                        radius = radius - 1.5f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Center Album Artwork Label (The mountain/galaxy artwork inside the vinyl)
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .rotate(rotation)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2C3E50), Color(0xFF4A2B68), Color(0xFF0F172A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(34.dp)
                    )
                    // Center Spindle Hole
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F0F14))
                            .border(1.5.dp, Color(0xFF94A3B8), CircleShape)
                    )
                }

                // Metallic Tonearm Needle Arm Overlay (Extending from top right onto the record)
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val pivot = Offset(size.width * 0.62f, size.height * 0.08f)
                    val elbow = Offset(size.width * 0.82f, size.height * 0.28f)
                    val stylus = Offset(size.width * 0.68f, size.height * 0.44f)

                    // Pivot base circle
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFFE2E8F0), Color(0xFF64748B))),
                        radius = 8.dp.toPx(),
                        center = pivot
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = pivot
                    )

                    // Metallic arm lines
                    drawLine(
                        brush = Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))),
                        start = pivot,
                        end = elbow,
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        brush = Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0))),
                        start = elbow,
                        end = stylus,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Headshell / Stylus cartridge resting on record
                    drawCircle(
                        color = Color(0xFFF1F5F9),
                        radius = 4.5.dp.toPx(),
                        center = stylus
                    )
                }
            }

            // Track Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Over the Horizon",
                    color = Color.White,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "hello Talk",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            }

            // 5 Utility Icons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.FavoriteBorder, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = Color.White, modifier = Modifier.size(21.dp))
                Icon(Icons.Rounded.Tune, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // Pill Seekbar: [ 10 ] [ 0:36 / 2:59 ] [ 10 ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Replay10, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.32f)
                            .align(Alignment.CenterStart)
                            .height(3.dp)
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = "0:36 / 2:59",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(Icons.Rounded.Forward10, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }

            // Playback Controls Row: [ 🔀 ] [ ⏮ ] [ ⏸ ] [ ⏭ ] [ 🔁 ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Shuffle, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(19.dp))
                Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(25.dp))
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Pause, null, tint = Color.Black, modifier = Modifier.size(28.dp))
                }
                Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(25.dp))
                Icon(Icons.Rounded.Repeat, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(19.dp))
            }
        }
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

/** Layout 3: Radial Ring with Up Next Queue Drawer (Matching Screenshot 2) */
@Composable
private fun RadialDrawerPreview(theme: PlayerThemeId) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Ring with side controls (⏮ on left, ⏭ on right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Box(
                    modifier = Modifier.size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(color = Color.White.copy(alpha = 0.25f), style = Stroke(4.5.dp.toPx()))
                        // Scrubber arc up to 1:40
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(5.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // White Knob dot at 1:40
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(center.x - radius, center.y)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(122.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF3B2A1C), Color(0xFF1F1510))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1:40", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
                Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }

            // Title with Shuffle on Left & Comment on Right (Screenshot 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Shuffle, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text("| Hridayam | Pranav | Ka...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Think Music India", color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp)
                }
                Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }

            // 5 Icons Row: Favorite, Timer, Add to playlist, Queue, Equalizer ON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.FavoriteBorder, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tune, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("ON", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Up Next Drawer Sheet with Floating Amber Circle Play Button (Screenshot 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        // Song 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Naughty Boy ft. Beyoncé", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("NaughtyBoyVEVO", color = Color.Gray, fontSize = 10.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFF1F5F9)) {
                                        Text("320K", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Icon(Icons.Filled.MoreVert, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        // Song 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OK Kanmani - Mental Manadhil", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("SonyMusicSouthVEVO", color = Color.Gray, fontSize = 10.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFF1F5F9)) {
                                        Text("320K", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Icon(Icons.Filled.MoreVert, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Floating Amber Circle Play Button (Docked on right edge per Screenshot 2)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-20).dp, y = (-20).dp)
                        .size(46.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
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
