package com.ant.tunes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.AntBlack
import com.ant.tunes.ui.theme.AntGlassBorder
import com.ant.tunes.ui.theme.AntGlassBorderHot
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntSurface2
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.LocalAccentColor
import com.ant.tunes.viewmodel.PlayerViewModel

var IsHomeSubScreenActive by mutableStateOf(false)

@Composable
fun PlayerScreen(
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: PlayerViewModel = viewModel()
    val recommendedSongs by vm.recommendedSongs.collectAsState()
    var showCache by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        PlayerManager.loadLibrary(context)
        PlayerManager.startProgressUpdates()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        HomeScreen(
            vm = vm,
            recommendedSongs = recommendedSongs,
            onMiniPlayerClick = { vm.isPlayerExpanded.value = true }, // 🟢 Triggers global state directly!
            onOpenProfile = onOpenProfile,
            onOpenSettings = onOpenSettings,
            showCache = showCache,
            onShowCacheChange = { showCache = it }
        )

        // 🟢 WRAP IN A DIALOG TO COVER THE BOTTOM NAV
        if (showCache) {
            Dialog(
                onDismissRequest = { showCache = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false // Draws over everything!
                )
            ) {
                CacheScreen(onBack = { showCache = false })
            }
        }
    }
}


// ═══════════════════════════════════════
// 🎵 FULL PLAYER
// ═══════════════════════════════════════
@Composable
fun FullPlayer(isPlaying: Boolean, onCollapse: () -> Unit) {
    val context = LocalContext.current
    val currentSong by PlayerManager.currentSong.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Art") }

    val vm: PlayerViewModel = viewModel()
    val currentLyrics by vm.currentLyrics
    val isLyricsLoading by vm.isLyricsLoading
    var isLyricsFullScreen by remember { mutableStateOf(false) }
    val lrcLines by vm.currentLrcLines
    val accent = LocalAccentColor.current

    val isLiked = currentSong?.let { song ->
        globalLikedSongs.any { it.id == song.id }
    } ?: false

    // 🟢 1. DYNAMIC COLOR STATES
    var dominantColor by remember { mutableStateOf(AntBlack) }
    val animatedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
        label = "bg_color"
    )

    // 🟢 2. APPLY THE FULL-BLEED GLOW GRADIENT
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.85f), // 🔥 High intensity at the top
                        animatedColor.copy(alpha = 0.4f),  // 🔥 Color bleeds through the middle
                        AntBlack // Deep black at the bottom so controls pop
                    )
                    // Removed endY so it stretches perfectly to the bottom of any screen size!
                )
            )
    ) {

    Column(
            // ... (keep the rest of your Column modifiers exactly as they were)
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding() + 8.dp
                )
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── HEADER ROW ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowDown, null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .background(AntSurface2.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selectedTab == "Art") accent else Color.Transparent)
                            .clickable { selectedTab = "Art" }
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Art",
                            color = if (selectedTab == "Art") AntBlack else Color.White,
                            style = MaterialTheme.typography.labelLarge)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selectedTab == "Lyrics") accent else Color.Transparent)
                            .clickable { selectedTab = "Lyrics" }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Lyrics",
                            color = if (selectedTab == "Lyrics") AntBlack else Color.White,
                            style = MaterialTheme.typography.labelLarge)
                    }
                }

                IconButton(onClick = { showOptionsMenu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.MoreVert, "Options",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (selectedTab == "Art") {
                    VinylArt(
                        imageUrl = currentSong?.albumArt,
                        isPlaying = isPlaying,
                        onColorExtracted = { dominantColor = it } // 🟢 FIXED: Receives the color!
                    )
                } else {
                    LyricsTab(
                        lyrics = currentLyrics,
                        isLoading = isLyricsLoading,
                        lrcLines = lrcLines,
                        isFullScreen = isLyricsFullScreen,
                        onToggleFullScreen = { isLyricsFullScreen = !isLyricsFullScreen },
                        // 🟢 WIRED: Triggers the ViewModel to fetch again for the current song!
                        onRetry = { currentSong?.let { vm.fetchLyrics(it) } }
                    )
                }
            }

            val rawArtist = currentSong?.artist?.replace(" • YouTube", "", ignoreCase = true) ?: "Unknown Artist"
            val rawAlbum = currentSong?.album ?: ""
            val rawTitle = currentSong?.title ?: ""

            val displayArtistText = if (
                rawAlbum.isNotBlank() &&
                rawAlbum.lowercase() != rawArtist.lowercase() &&
                rawAlbum.lowercase() != rawTitle.lowercase() &&
                rawAlbum.lowercase() != "youtube"
            ) {
                "$rawArtist  •  $rawAlbum"
            } else {
                rawArtist
            }

            ControlBar(
                context = context,
                title = currentSong?.title ?: "Play Something",
                artist = displayArtistText,
                album = currentSong?.album ?: "",
                onPrev = { PlayerManager.previous() },
                onPlayPause = { PlayerManager.togglePlayPause() },
                onNext = { PlayerManager.next() },
                onOpenQueue = { showQueue = true },
                isPlaying = isPlaying,
                isLiked = isLiked,
                onToggleLike = {
                    currentSong?.let { song ->
                        if (isLiked) globalLikedSongs.removeAll { it.id == song.id }
                        else globalLikedSongs.add(song)
                        com.ant.tunes.player.AppDataManager.saveLikedSongs(context, globalLikedSongs)
                    }
                }
            )

            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 16.dp
                )
            )
        }

        // ── QUEUE OVERLAY ──
        if (showQueue) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AntBlack.copy(alpha = 0.95f))
                    .clickable { showQueue = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = WindowInsets.statusBars
                                .asPaddingValues()
                                .calculateTopPadding()
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("UP NEXT",
                            style = MaterialTheme.typography.displayMedium,
                            color = AntText)
                        IconButton(onClick = { showQueue = false }) {
                            Icon(Icons.Default.Close, "Close Queue", tint = AntText)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        QueuePanel(
                            isExpanded = true,
                            onSongSelected = { showQueue = false }
                        )
                    }
                }
            }
        }

        // ── FULLSCREEN LYRICS OVERLAY ──
        if (isLyricsFullScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AntBlack)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 40f) isLyricsFullScreen = false
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(AntSurface1)
                        .border(1.dp, AntGlassBorderHot, RoundedCornerShape(28.dp))
                ) {
                    LyricsTab(
                        lyrics = currentLyrics,
                        isLoading = isLyricsLoading,
                        lrcLines = lrcLines,
                        isFullScreen = true,
                        onToggleFullScreen = { isLyricsFullScreen = false }
                    )
                }

                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(AntGlassBorder, RoundedCornerShape(2.dp))
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
        }
    }

    if (showOptionsMenu) {
        PlayerOptionsMenu(onDismiss = { showOptionsMenu = false })
    }
}

// ═══════════════════════════════════════
// 🎵 MINI PLAYER BAR
// ═══════════════════════════════════════
@Composable
fun MiniPlayerBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isPlaying: Boolean
) {
    val currentSong by PlayerManager.currentSong.collectAsState()
    val position by PlayerManager.currentPosition.collectAsState()
    val duration by PlayerManager.duration.collectAsState()
    val progress = if (duration > 0) position.toFloat() / duration else 0f

    val accent = LocalAccentColor.current

    if (currentSong == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(67.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(AntBlack.copy(alpha = 0.90f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(36.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            AsyncImage(
                model = currentSong?.albumArt,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AntSurface2)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(currentSong?.title ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    color = AntText, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(currentSong?.artist ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = AntText2, maxLines = 1)
            }
            IconButton(
                onClick = { PlayerManager.previous() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.SkipPrevious, null,
                    tint = AntText2, modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = { PlayerManager.togglePlayPause() },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            IconButton(
                onClick = { PlayerManager.next() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.SkipNext, null,
                    tint = AntText2, modifier = Modifier.size(20.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(AntGlassBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(accent)
            )
        }
    }
}
