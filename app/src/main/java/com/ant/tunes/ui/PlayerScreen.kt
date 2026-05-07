package com.ant.tunes.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.*
import com.ant.tunes.viewmodel.PlayerViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@Composable
fun PlayerScreen(onOpenProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val isPlaying by PlayerManager.isPlayingFlow.collectAsState()
    val vm: PlayerViewModel = viewModel()
    val recommendedSongs by vm.recommendedSongs.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        vm.isPlayerExpanded.value = expanded
    }

    // PATCH 1 — observe RequestFullScreenPlayer
    LaunchedEffect(RequestFullScreenPlayer) {
        if (RequestFullScreenPlayer) {
            expanded = true
            RequestFullScreenPlayer = false
        }
    }

    val expandAnim by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ), label = "expand"
    )

    LaunchedEffect(Unit) {
        PlayerManager.loadLibrary(context)
        PlayerManager.startProgressUpdates()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeContent(
            vm = vm,
            recommendedSongs = recommendedSongs,
            onMiniPlayerClick = { expanded = true },
            onOpenProfile = onOpenProfile
        )

        if (expandAnim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(expandAnim)
                    .graphicsLayer { translationY = (1f - expandAnim) * 800f }
                    .background(AntBlack)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 30f) expanded = false
                        }
                    }
            ) {
                FullPlayer(isPlaying = isPlaying, onCollapse = { expanded = false })
            }
        }

        if (expandAnim < 1f) {
            MiniPlayerBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 15.dp, start = 16.dp, end = 16.dp)
                    .alpha(1f - expandAnim),
                onClick = { expanded = true },
                isPlaying = isPlaying
            )
        }
    }
}

// ═══════════════════════════════════════
// 🏠 HOME CONTENT
// ═══════════════════════════════════════
@Composable
fun HomeContent(
    vm: PlayerViewModel,
    recommendedSongs: List<com.ant.tunes.data.Song>,
    onMiniPlayerClick: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentSong by PlayerManager.currentSong.collectAsState()
    val isPlaying by PlayerManager.isPlayingFlow.collectAsState()
    val position by PlayerManager.currentPosition.collectAsState()
    val duration by PlayerManager.duration.collectAsState()
    val progress = if (duration > 0) position.toFloat() / duration else 0f

    val downloadedSongs by PlayerManager.downloadedSongs.collectAsState()
    var showOffline by remember { mutableStateOf(false) }

    // 🟢 Read Username from SharedPreferences
    val prefs = context.getSharedPreferences("ant_prefs", android.content.Context.MODE_PRIVATE)
    val userName by remember { mutableStateOf(prefs.getString("user_name", "Listener")?.ifEmpty { "Listener" } ?: "Listener") }

    // 🟢 REAL DATA STATEFLOWS
    val recentSongs by PlayerManager.recentlyPlayed.collectAsState()
    val topTracks by PlayerManager.topTracks.collectAsState()


    val accent = LocalAccentColor.current
    val accentDim = accent.copy(alpha = 0.15f)
    val accentHot = accent.copy(alpha = 0.3f)

    val dotAlpha by rememberInfiniteTransition(label = "dot")
        .animateFloat(
            initialValue = 1f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "dotAlpha"
        )

    val vinylRotation by rememberInfiniteTransition(label = "vinyl")
        .animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
            label = "vinylRot"
        )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = 0.dp, bottom = 220.dp
        )
    ) {
        // ── HERO ──
        item {
            Spacer(Modifier.height(56.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).alpha(dotAlpha).background(accent, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("ANT TUNES",
                        style = MaterialTheme.typography.labelLarge, color = AntText3)
                }
                IconButton(
                    onClick = { onOpenProfile() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(AntSurface1, CircleShape)
                        .border(1.dp, AntGlassBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Person, null,
                        tint = AntText2, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Welcome back,\n$userName",
                style = MaterialTheme.typography.displayLarge, color = AntText)
            Spacer(Modifier.height(24.dp))
        }

        // ── FEATURED CARD ──
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(accentDim, AntBlack)))
                    .border(1.dp, accentHot, RoundedCornerShape(24.dp))
                    .clickable { onMiniPlayerClick() }
            ) {
                if (!currentSong?.albumArt.isNullOrEmpty()) {
                    AsyncImage(
                        model = currentSong?.albumArt,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(0.15f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .offset(x = 160.dp, y = (-40).dp)
                        .blur(20.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(accent.copy(alpha = 0.35f), Color.Transparent)
                            ), CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 20.dp)
                        .rotate(if (isPlaying) vinylRotation else 0f)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(AntBlack, accentDim, AntBlack, accentDim, AntBlack)
                            )
                        )
                        .border(1.5.dp, accentHot, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentSong?.albumArt.isNullOrEmpty()) {
                        AsyncImage(
                            model = currentSong?.albumArt,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(80.dp).clip(CircleShape)
                        )
                    } else {
                        Box(Modifier.size(80.dp).clip(CircleShape).background(AntSurface2))
                    }
                    Box(Modifier.size(14.dp).background(AntBlack, CircleShape))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(AntBlack.copy(alpha = 0.85f), Color.Transparent),
                                endX = 500f
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.65f)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(16.dp).height(1.dp).background(accent))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isPlaying) "NOW PLAYING" else "LAST PLAYED",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        currentSong?.title ?: "Play Something",
                        style = MaterialTheme.typography.titleLarge,
                        color = AntText, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        currentSong?.artist ?: "Search a song to start",
                        style = MaterialTheme.typography.labelMedium,
                        color = AntText2, maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(3.dp)
                        .align(Alignment.BottomCenter)
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
            Spacer(Modifier.height(28.dp))
        }

        // ── OFFLINE ALBUM ──
        if (downloadedSongs.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("OFFLINE ALBUM",
                        style = MaterialTheme.typography.labelLarge, color = AntText3)
                    Text("${downloadedSongs.size} TRACKS",
                        style = MaterialTheme.typography.labelSmall, color = accent)
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AntSurface1)
                        .border(1.dp, AntGlassBorder, RoundedCornerShape(20.dp))
                        .clickable { showOffline = true }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) {
                            if (downloadedSongs.size >= 4) {
                                Column {
                                    Row {
                                        AsyncImage(model = downloadedSongs[0].albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp))
                                        AsyncImage(model = downloadedSongs[1].albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp))
                                    }
                                    Row {
                                        AsyncImage(model = downloadedSongs[2].albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp))
                                        AsyncImage(model = downloadedSongs[3].albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp))
                                    }
                                }
                            } else {
                                AsyncImage(
                                    model = downloadedSongs[0].albumArt,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("My Downloads",
                                style = MaterialTheme.typography.titleSmall, color = AntText)
                            Text("${downloadedSongs.size} songs • offline",
                                style = MaterialTheme.typography.labelMedium, color = AntText2)
                        }
                        Icon(
                            Icons.Default.PlayArrow, null,
                            tint = accent,
                            modifier = Modifier
                                .size(40.dp)
                                .background(accentDim, CircleShape)
                                .padding(8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }

        // ── RECENTLY PLAYED ──
        if (recentSongs.isNotEmpty()) {
            item {
                Text("RECENTLY PLAYED",
                    style = MaterialTheme.typography.labelLarge, color = AntText3)
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(recentSongs) { song ->
                        // PATCH 3 — trigger expand on tap
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable {
                                    PlayerManager.playStream(context, song)
                                    RequestFullScreenPlayer = true
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                AsyncImage(
                                    model = song.albumArt,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow, null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(song.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = AntText, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text(song.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = AntText2, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }

        // (Up Next Queue Removed from Home Screen)

        // ── TOP TRACKS ──
        if (topTracks.isNotEmpty()) {
            item {
                Text("YOUR TOP TRACKS",
                    style = MaterialTheme.typography.labelLarge, color = AntText3)
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(topTracks.take(10)) { song ->

                    // PATCH 3 — trigger expand on tap
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable {
                                    PlayerManager.playStream(context, song)
                                    RequestFullScreenPlayer = true
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AntSurface2)
                            ) {
                                AsyncImage(
                                    model = song.albumArt,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(song.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = AntText, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text(song.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = AntText2, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (showOffline) {
        OfflineScreen(onBack = { showOffline = false })
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
    var showOptionsMenu by remember { mutableStateOf(false) } // 🟢 ADDED MENU STATE
    var selectedTab by remember { mutableStateOf("Art") }

    val vm: PlayerViewModel = viewModel() // 🟢 Needed for Lyrics
    val currentLyrics by vm.currentLyrics
    val isLyricsLoading by vm.isLyricsLoading

    val accent = LocalAccentColor.current


    // PATCH 2 — liked state
    val isLiked = currentSong?.let { song ->
        globalLikedSongs.any { it.id == song.id }
    } ?: false

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
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

                // Art / Lyrics toggle
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

                // 🟢 WIRED 3-DOT MENU
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
                    VinylArt(imageUrl = currentSong?.albumArt, isPlaying = isPlaying)
                } else {
                    // 🟢 WIRED LYRICS TAB
                    LyricsTab(lyrics = currentLyrics, isLoading = isLyricsLoading)
                }

            }

            // PATCH 2 — pass liked state + toggle to ControlBar
            ControlBar(
                context = context,
                title = currentSong?.title ?: "Play Something",
                artist = currentSong?.artist ?: "Lil Bro",
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
                    // ✅ THIS IS THE FIXED LINE:
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        QueuePanel(
                            isExpanded = true,
                            onSongSelected = { showQueue = false }
                        )
                    }
                }
            }
        }
    }
    // ── OPTIONS MENU BOTTOM SHEET ──
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
    val accentHot = accent.copy(alpha = 0.3f)

    if (currentSong == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xE6080808))
            .border(1.dp, accentHot, RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            accent.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
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