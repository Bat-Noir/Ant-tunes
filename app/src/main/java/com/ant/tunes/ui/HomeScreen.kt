package com.ant.tunes.ui

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.ant.tunes.lastfm.LastFmAuthManager
import com.ant.tunes.player.CacheManager
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.AntBlack
import com.ant.tunes.ui.theme.AntGlassBorder
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntSurface2
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor
import com.ant.tunes.viewmodel.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun HomeScreen(
    vm: PlayerViewModel,
    recommendedSongs: List<com.ant.tunes.data.Song>,
    onMiniPlayerClick: () -> Unit,
    onOpenProfile: () -> Unit = {},
    showCache: Boolean,
    onShowCacheChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val connectivityManager = context.getSystemService(
        Context.CONNECTIVITY_SERVICE
    ) as android.net.ConnectivityManager
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    val isOnline = capabilities != null && (
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
            )

    val currentSong by PlayerManager.currentSong.collectAsState()
    val isPlaying by PlayerManager.isPlayingFlow.collectAsState()
    val position by PlayerManager.currentPosition.collectAsState()
    val duration by PlayerManager.duration.collectAsState()
    val progress = if (duration > 0) position.toFloat() / duration else 0f

    val downloadedSongs by PlayerManager.downloadedSongs.collectAsState()
    var showOffline by remember { mutableStateOf(false) }

    LaunchedEffect(showCache, showOffline) {
        com.ant.tunes.ui.IsHomeSubScreenActive = showCache || showOffline
    }

    androidx.activity.compose.BackHandler(enabled = showOffline) {
        showOffline = false
    }

    val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
    val userName by remember {
        mutableStateOf(
            prefs.getString("user_name", "Listener")?.ifEmpty { "Listener" } ?: "Listener")
    }

    val localRecent by PlayerManager.recentlyPlayed.collectAsState()
    val localTopTracks by PlayerManager.topTracks.collectAsState()
    val lastFmRecent by vm.lastFmRecentTracks.collectAsState()
    val lastFmTop by vm.lastFmTopTracks.collectAsState()
    val isLastFmConnected by LastFmAuthManager.isLoggedIn.collectAsState()
    val publicCharts by vm.publicCharts.collectAsState()

    // 🟢 FIXED: Local History is now KING. Last.fm is just a fallback.
    // 🟢 FIXED: Local History is now KING. Shows ONLY what you actually played.
    val recentSongs = when {
        localRecent.isNotEmpty() -> localRecent
        isLastFmConnected && lastFmRecent.isNotEmpty() -> lastFmRecent
        else -> emptyList()
    }

    val topTracks = when {
        localTopTracks.isNotEmpty() -> localTopTracks
        isLastFmConnected && lastFmTop.isNotEmpty() -> lastFmTop
        publicCharts.isNotEmpty() -> publicCharts
        else -> emptyList()
    }

    val topTracksLabel = when {
        isLastFmConnected && lastFmTop.isNotEmpty() -> "YOUR TOP TRACKS"
        localTopTracks.isNotEmpty() -> "YOUR TOP TRACKS"
        else -> "TRENDING NOW"
    }

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

    // 🟢 NEW: State for the Refresh Button Spin
    var refreshRotation by remember { mutableFloatStateOf(0f) }
    val animatedRefresh by animateFloatAsState(
        targetValue = refreshRotation,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "refreshSpin"
    )


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = 0.dp, bottom = 220.dp
        )
    ) {
        // ── OFFLINE BANNER ──
        item {
            if (!isOnline) {
                Spacer(Modifier.height(48.dp))
                NoInternetBanner()
                Spacer(Modifier.height(12.dp))
            }
        }

        // ── HERO ──
        item {
            Spacer(Modifier.height(if (!isOnline) 8.dp else 56.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).alpha(dotAlpha).background(accent, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("ANT TUNES", style = MaterialTheme.typography.labelLarge, color = AntText3)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 🟢 UPGRADED: Manual Refresh Button with Spin Animation
                    IconButton(
                        onClick = {
                            refreshRotation += 360f // 🟢 Triggers a smooth clockwise spin!
                            vm.fetchPublicCharts() // Refresh Trending
                            currentSong?.let { vm.loadLastFmRecommendations(it) } // Refresh Discover
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = accent,
                            modifier = Modifier.rotate(animatedRefresh) // 🟢 Attaches the rotation physics
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Profile Button
                    IconButton(
                        onClick = { onOpenProfile() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, accent.copy(alpha = 0.4f), CircleShape)
                    ) {
                        com.ant.tunes.ui.AvatarDisplay(
                            userName = userName,
                            size = 36.dp,
                            accent = accent
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Welcome back,\n$userName",
                style = MaterialTheme.typography.displayLarge,
                color = AntText
            )
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
                                listOf(
                                    accent.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
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
                                listOf(
                                    AntBlack,
                                    accentDim,
                                    AntBlack,
                                    accentDim,
                                    AntBlack
                                )
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
                    modifier = Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(
                                AntBlack.copy(alpha = 0.85f),
                                Color.Transparent
                            ), endX = 500f
                        )
                    )
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(0.65f)
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
                        color = AntText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        currentSong?.artist ?: "Search a song to start",
                        style = MaterialTheme.typography.labelMedium,
                        color = AntText2,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter)
                        .background(AntGlassBorder)
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(progress)
                            .background(accent)
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        // ── 1. SMART CACHE ALBUM ──
        val cachedSongs = recentSongs.filter { s -> CacheManager.isCached(context, s.id) } // 🟢 FIXED

        if (cachedSongs.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SMART CACHE",
                        style = MaterialTheme.typography.labelLarge,
                        color = AntText3
                    )
                    Text(
                        "${cachedSongs.size} TRACKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AntSurface1)
                        .border(1.dp, AntGlassBorder, RoundedCornerShape(20.dp))
                        .clickable { onShowCacheChange(true) }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) {
                            if (cachedSongs.size >= 4) {
                                Column {
                                    Row {
                                        AsyncImage(
                                            model = cachedSongs[0].albumArt,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        AsyncImage(
                                            model = cachedSongs[1].albumArt,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Row {
                                        AsyncImage(
                                            model = cachedSongs[2].albumArt,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        AsyncImage(
                                            model = cachedSongs[3].albumArt,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            } else {
                                AsyncImage(
                                    model = cachedSongs[0].albumArt,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Smart Cache",
                                style = MaterialTheme.typography.titleSmall,
                                color = AntText
                            )
                            Text(
                                "${cachedSongs.size} songs • auto-cached",
                                style = MaterialTheme.typography.labelMedium,
                                color = AntText2
                            )
                        }
                        Icon(
                            Icons.Default.PlayArrow,
                            "Play Cache",
                            tint = accent,
                            modifier = Modifier.size(40.dp)
                                .background(accent.copy(alpha = 0.15f), CircleShape).padding(8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }

        // ── 2. MADE FOR YOU (LAST.FM DISCOVER) ──
        if (recommendedSongs.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "MADE FOR YOU",
                        style = MaterialTheme.typography.labelLarge,
                        color = AntText3
                    )
                    Text("DISCOVER", style = MaterialTheme.typography.labelSmall, color = accent)
                }
                Spacer(Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(recommendedSongs) { song ->
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable {
                                    PlayerManager.play(
                                        context,
                                        recommendedSongs,
                                        recommendedSongs.indexOf(song)
                                    )
                                    RequestFullScreenPlayer = true
                                }
                        ) {
                            Box(
                                modifier = Modifier.size(140.dp).clip(RoundedCornerShape(16.dp))
                                    .background(AntSurface2)
                            ) {
                                AsyncImage(
                                    model = song.albumArt,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                        .background(
                                            AntBlack.copy(alpha = 0.6f),
                                            RoundedCornerShape(8.dp)
                                        ).border(0.5.dp, accentHot, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "98% Match",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                song.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = AntText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = AntText2,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }

        // ── 3. OFFLINE ALBUM ──
        if (downloadedSongs.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "OFFLINE ALBUM",
                        style = MaterialTheme.typography.labelLarge,
                        color = AntText3
                    )
                    Text(
                        "${downloadedSongs.size} TRACKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
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
                                        AsyncImage(
                                            model = downloadedSongs[0].albumArt,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        AsyncImage(
                                            model = downloadedSongs[1].albumArt,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Row {
                                        AsyncImage(
                                            model = downloadedSongs[2].albumArt,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        AsyncImage(
                                            model = downloadedSongs[3].albumArt,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(32.dp)
                                        )
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
                            Text(
                                "My Downloads",
                                style = MaterialTheme.typography.titleSmall,
                                color = AntText
                            )
                            Text(
                                "${downloadedSongs.size} songs • offline",
                                style = MaterialTheme.typography.labelMedium,
                                color = AntText2
                            )
                        }
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            tint = accent,
                            modifier = Modifier.size(40.dp).background(accentDim, CircleShape)
                                .padding(8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }

        // ── 4. RECENTLY PLAYED ──
        if (recentSongs.isNotEmpty()) {
            item {
                Text(
                    "RECENTLY PLAYED",
                    style = MaterialTheme.typography.labelLarge,
                    color = AntText3
                )
                Spacer(Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(recentSongs) { song ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable {
                                    PlayerManager.play(
                                        context,
                                        recentSongs,
                                        recentSongs.indexOf(song)
                                    )
                                    RequestFullScreenPlayer = true
                                }
                        ) {
                            Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp))) {
                                AsyncImage(
                                    model = song.albumArt,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                song.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = AntText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = AntText2,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }

        // ── 5. TOP TRACKS / TRENDING NOW ──
        if (topTracks.isNotEmpty()) {
            item {
                Text(topTracksLabel, style = MaterialTheme.typography.labelLarge, color = AntText3)
                Spacer(Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(topTracks.take(10)) { song ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable {
                                    val top10List = topTracks.take(10)
                                    PlayerManager.play(context, top10List, top10List.indexOf(song))
                                    RequestFullScreenPlayer = true
                                }
                        ) {
                            Box(
                                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp))
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
                            Text(
                                song.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = AntText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = AntText2,
                                maxLines = 1
                            )
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

