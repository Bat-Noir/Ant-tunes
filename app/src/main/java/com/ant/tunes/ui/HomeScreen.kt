@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.ant.tunes.ui

import android.content.Context
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings // 🟢 ADD THIS
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.ant.tunes.data.Song
import com.ant.tunes.lastfm.LastFmAuthManager
import com.ant.tunes.player.CacheManager
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.*
import com.ant.tunes.viewmodel.PlayerViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun HomeScreen(
    vm: PlayerViewModel,
    recommendedSongs: List<Song>,
    onMiniPlayerClick: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {}, // 🟢 ADD THIS LINE
    showCache: Boolean,
    onShowCacheChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    // 🟢 ROUTING STATES FOR ALBUMS & ARTISTS
    var selectedAlbum by remember { mutableStateOf<BrowseCard?>(null) }
    var selectedArtist by remember { mutableStateOf<BrowseCard?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showOffline by remember { mutableStateOf(false) }

    LaunchedEffect(showCache, showOffline, selectedAlbum, selectedArtist) {
        com.ant.tunes.ui.IsHomeSubScreenActive = showCache || showOffline || selectedAlbum != null || selectedArtist != null
    }

    androidx.activity.compose.BackHandler(enabled = com.ant.tunes.ui.IsHomeSubScreenActive) {
        if (selectedAlbum != null) selectedAlbum = null
        else if (selectedArtist != null) selectedArtist = null
        else if (showOffline) showOffline = false
    }

    // 🟢 TIME-BASED GREETING LOGIC
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val (greeting, topGradientColor) = when (currentHour) {
        in 5..11 -> "Good Morning" to Color(0xFFFDB813).copy(alpha = 0.15f)
        in 12..17 -> "Good Afternoon" to Color(0xFFF47C20).copy(alpha = 0.15f)
        in 18..22 -> "Good Evening" to Color(0xFF8B5CF6).copy(alpha = 0.15f)
        else -> "Late Night Vibes" to Color(0xFF3B82F6).copy(alpha = 0.15f)
    }

    // 🟢 DYNAMIC ENGINE TRIGGER
    LaunchedEffect(globalFollowedArtists.size) {
        if (globalFollowedArtists.isNotEmpty()) {
            vm.generateDynamicArtistRow(globalFollowedArtists)
        }
    }

    val dynamicArtistName = vm.dynamicArtistName.value
    val dynamicArtistTracks = vm.dynamicArtistTracks

    // NETWORK
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    val isOnline = capabilities != null && (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET))

    // PLAYER STATES
    val currentSong by PlayerManager.currentSong.collectAsState()
    val isPlaying by PlayerManager.isPlayingFlow.collectAsState()
    val position by PlayerManager.currentPosition.collectAsState()
    val duration by PlayerManager.duration.collectAsState()
    val progress = if (duration > 0) position.toFloat() / duration else 0f
    val downloadedSongs by PlayerManager.downloadedSongs.collectAsState()

    // PREFS
    val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
    val userName by remember { mutableStateOf(prefs.getString("user_name", "Listener")?.ifEmpty { "Listener" } ?: "Listener") }

    // DATA SOURCES
    val localRecent by PlayerManager.recentlyPlayed.collectAsState()
    val localTopTracks by PlayerManager.topTracks.collectAsState()
    val lastFmRecent by vm.lastFmRecentTracks.collectAsState()
    val lastFmTop by vm.lastFmTopTracks.collectAsState()
    val isLastFmConnected by LastFmAuthManager.isLoggedIn.collectAsState()
    val publicCharts by vm.publicCharts.collectAsState()

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

    // ANIMATIONS
    val dotAlpha by rememberInfiniteTransition(label = "dot").animateFloat(initialValue = 1f, targetValue = 0.3f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "dotAlpha")
    val vinylRotation by rememberInfiniteTransition(label = "vinyl").animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "vinylRot")

    var refreshRotation by remember { mutableFloatStateOf(0f) }
    val animatedRefresh by animateFloatAsState(targetValue = refreshRotation, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing), label = "refreshSpin")


    // 🟢 APPLE MUSIC STYLE DEFINITIONS
    // We remove the light background entirely. We use a high-intensity white glassy border.
    val glassyShinyBorderIntensity = 0.6f
    val shinyGlassOutline = Color.White.copy(alpha = glassyShinyBorderIntensity)


    // 🟢 BOTTOM SHEET
    if (songToAddToPlaylist != null) {
        ModalBottomSheet(
            onDismissRequest = { songToAddToPlaylist = null },
            containerColor = Color(0xFF121212),
            dragHandle = { BottomSheetDefaults.DragHandle(color = AntText3) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Add to Playlist", style = MaterialTheme.typography.titleLarge, color = AntText)
                Spacer(Modifier.height(16.dp))
                if (globalPlaylists.isEmpty()) {
                    Text("No playlists yet.", color = AntText3)
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                        items(globalPlaylists) { playlist ->
                            val isAdded = playlist.tracks.any { it.id == songToAddToPlaylist?.id }
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !isAdded) {
                                    playlist.tracks.add(songToAddToPlaylist!!)
                                    com.ant.tunes.player.AppDataManager.savePlaylists(context, globalPlaylists)
                                    Toast.makeText(context, "Added to ${playlist.name.value}", Toast.LENGTH_SHORT).show()
                                    songToAddToPlaylist = null
                                }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QueueMusic, null, tint = if (isAdded) AntText3 else accent)
                                Spacer(Modifier.width(12.dp))
                                Text(playlist.name.value, color = if (isAdded) AntText3 else AntText, modifier = Modifier.weight(1f))
                                if (isAdded) Icon(Icons.Default.Check, null, tint = AntText3)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }


    // 🟢 ROUTING ROUTER
    if (selectedAlbum != null) {
        AlbumScreen(
            album = selectedAlbum!!,
            albumTracks = vm.albumTracks,
            isLoading = vm.isAlbumLoading.value,
            onBack = { selectedAlbum = null },
            onAddToPlaylist = { songToAddToPlaylist = it }
        )
    } else if (selectedArtist != null) {
        ArtistScreen(
            artist = selectedArtist!!,
            artistTracks = vm.albumTracks,
            isLoading = vm.isAlbumLoading.value,
            onBack = { selectedArtist = null },
            onAddToPlaylist = { songToAddToPlaylist = it }
        )
    } else if (showOffline) {
        OfflineScreen(onBack = { showOffline = false })
    } else {
        // 🟢 MAIN HOME SCREEN
        Box(modifier = Modifier.fillMaxSize().background(AntBlack)) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Brush.verticalGradient(listOf(topGradientColor, Color.Transparent)))
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 220.dp)
            ) {
                // ── OFFLINE BANNER ──
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (!isOnline) {
                            Spacer(Modifier.height(48.dp))
                            NoInternetBanner()
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                // ── HERO GREETING ──
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                            // 🟢 FIXED: Used spacedBy(12.dp) for perfect, even gaps
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        refreshRotation += 360f
                                        vm.fetchPublicCharts()
                                        currentSong?.let { vm.loadLastFmRecommendations(it) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, "Refresh", tint = accent, modifier = Modifier.rotate(animatedRefresh))
                                }

                                IconButton(
                                    onClick = { onOpenSettings() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    // 🟢 FIXED: Changed tint to 'accent' instead of 'AntText3'
                                    Icon(Icons.Default.Settings, "Settings", tint = accent)
                                }

                                IconButton(
                                    onClick = { onOpenProfile() },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).border(1.dp, accent.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    com.ant.tunes.ui.AvatarDisplay(userName = userName, size = 36.dp, accent = accent)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("$greeting,\n$userName", style = MaterialTheme.typography.displayLarge, color = AntText)
                        Spacer(Modifier.height(24.dp))
                    }
                }
// ── FEATURED VINYL CARD ──
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(accentDim, AntBlack))).border(1.dp, accentHot, RoundedCornerShape(24.dp)).clickable { onMiniPlayerClick() }
                        ) {
                            if (!currentSong?.albumArt.isNullOrEmpty()) {
                                AsyncImage(model = currentSong?.albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.15f))
                            }
                            Box(modifier = Modifier.size(240.dp).offset(x = 160.dp, y = (-40).dp).blur(20.dp).background(Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent)), CircleShape))
                            Box(
                                modifier = Modifier.size(160.dp).align(Alignment.CenterEnd).offset(x = 20.dp).rotate(if (isPlaying) vinylRotation else 0f).clip(CircleShape).background(Brush.sweepGradient(listOf(AntBlack, accentDim, AntBlack, accentDim, AntBlack))).border(1.5.dp, accentHot, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!currentSong?.albumArt.isNullOrEmpty()) {
                                    AsyncImage(model = currentSong?.albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp).clip(CircleShape))
                                } else {
                                    Box(Modifier.size(80.dp).clip(CircleShape).background(AntSurface2))
                                }
                                Box(Modifier.size(14.dp).background(AntBlack, CircleShape))
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(AntBlack.copy(alpha = 0.85f), Color.Transparent), endX = 500f)))
                            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(0.65f).padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.width(16.dp).height(1.dp).background(accent))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isPlaying) "NOW PLAYING" else "LAST PLAYED", style = MaterialTheme.typography.labelSmall, color = accent)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(currentSong?.title ?: "Play Something", style = MaterialTheme.typography.titleLarge, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(currentSong?.artist ?: "Search a song to start", style = MaterialTheme.typography.labelMedium, color = AntText2, maxLines = 1)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter).background(AntGlassBorder)) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(accent))
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }

                // ── JUMP BACK IN GRID ──
                if (globalSavedAlbums.isNotEmpty() || globalFollowedArtists.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            data class JumpItem(val title: String, val imageUrl: String, val type: String, val rawData: BrowseCard)

                            val mixedHistory = remember(globalSavedAlbums.size, globalFollowedArtists.size) {
                                val list = mutableListOf<JumpItem>()
                                globalSavedAlbums.take(3).forEach { list.add(JumpItem(it.title, it.imageUrl, "Album", it)) }
                                globalFollowedArtists.take(3).forEach { list.add(JumpItem(it.title, it.imageUrl, "Artist", it)) }
                                list.shuffled().take(6)
                            }

                            val chunked = mixedHistory.chunked(2)
                            chunked.forEach { rowItems ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    rowItems.forEach { item ->
                                        Row(
                                            // 🟢 FIXED: Dark background with intense glassy border only
                                            modifier = Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(12.dp))
                                                .background(AntSurface1) // Reverted to dark surface
                                                .border(0.5.dp, shinyGlassOutline, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    if (item.type == "Album") {
                                                        selectedAlbum = item.rawData
                                                        vm.loadAlbumById(item.rawData.id)
                                                    } else {
                                                        selectedArtist = item.rawData
                                                        vm.loadArtistById(item.rawData.id)
                                                    }
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.size(56.dp), contentScale = ContentScale.Crop)
                                            Text(item.title, style = MaterialTheme.typography.labelMedium, color = AntText, modifier = Modifier.padding(horizontal = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                // ── HEAVY ROTATION ──
                if (globalLikedSongs.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Your Liked Once", style = MaterialTheme.typography.titleMedium, color = AntText3, modifier = Modifier.padding(bottom = 12.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                val rotationTracks = globalLikedSongs.reversed().take(10)
                                itemsIndexed(rotationTracks) { index, song ->
                                    Column(
                                        modifier = Modifier.width(110.dp).clickable {
                                            PlayerManager.play(context, rotationTracks, index)
                                            vm.isPlayerExpanded.value = true
                                            RequestFullScreenPlayer = true
                                        },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AsyncImage(
                                            model = song.albumArt,
                                            contentDescription = null,
                                            modifier = Modifier.size(110.dp).clip(CircleShape).border(1.dp, AntGlassBorder, CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(song.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                // ── BECAUSE YOU LISTEN TO [ARTIST] ──
                if (dynamicArtistName != null && dynamicArtistTracks.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Because you listen to $dynamicArtistName",
                                style = MaterialTheme.typography.titleMedium,
                                color = AntText3,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                itemsIndexed(dynamicArtistTracks) { index, song ->
                                    Column(
                                        modifier = Modifier.width(130.dp).clickable {
                                            PlayerManager.play(context, dynamicArtistTracks, index)
                                            vm.isPlayerExpanded.value = true
                                            RequestFullScreenPlayer = true
                                        }
                                    ) {
                                        AsyncImage(
                                            model = song.albumArt,
                                            contentDescription = null,
                                            modifier = Modifier.size(130.dp).clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(song.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(song.artist, style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                // ── SMART CACHE ALBUM ──
                val cachedSongs = recentSongs.filter { s -> CacheManager.isCached(context, s.id) }
                if (cachedSongs.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("SMART CACHE", style = MaterialTheme.typography.labelLarge, color = AntText3)
                                Text("${cachedSongs.size} TRACKS", style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            Spacer(Modifier.height(12.dp))

                            Box(
                                // 🟢 FIXED: Dark background with intense glassy border only
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                                    .background(AntSurface1) // Reverted to dark surface
                                    .border(0.5.dp, shinyGlassOutline, RoundedCornerShape(20.dp))
                                    .clickable { onShowCacheChange(true) }.padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) {
                                        if (cachedSongs.size >= 4) {
                                            Column {
                                                Row {
                                                    AsyncImage(model = cachedSongs[0].albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp))
                                                    AsyncImage(model = cachedSongs[1].albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp))
                                                }
                                                Row {
                                                    AsyncImage(model = cachedSongs[2].albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp))
                                                    AsyncImage(model = cachedSongs[3].albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp))
                                                }
                                            }
                                        } else {
                                            AsyncImage(model = cachedSongs[0].albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Smart Cache", style = MaterialTheme.typography.titleSmall, color = AntText)
                                        Text("${cachedSongs.size} songs • auto-cached", style = MaterialTheme.typography.labelMedium, color = AntText2)
                                    }
                                    Icon(Icons.Default.PlayArrow, "Play Cache", tint = accent, modifier = Modifier.size(40.dp).background(accent.copy(alpha = 0.15f), CircleShape).padding(8.dp))
                                }
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }

                // ── MADE FOR YOU (LAST.FM DISCOVER) ──
                if (recommendedSongs.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("MADE FOR YOU", style = MaterialTheme.typography.labelLarge, color = AntText3)
                                Text("DISCOVER", style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            Spacer(Modifier.height(12.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(recommendedSongs) { index, song ->
                                    Column(
                                        modifier = Modifier.width(140.dp).clickable {
                                            PlayerManager.play(context, recommendedSongs, index)
                                            vm.isPlayerExpanded.value = true
                                            RequestFullScreenPlayer = true
                                        }
                                    ) {
                                        Box(modifier = Modifier.size(140.dp).clip(RoundedCornerShape(16.dp)).background(AntSurface2)) {
                                            AsyncImage(model = song.albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            Box(
                                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(AntBlack.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).border(0.5.dp, accentHot, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("98% Match", style = MaterialTheme.typography.labelSmall, color = accent, fontSize = 8.sp)
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(song.title, style = MaterialTheme.typography.labelLarge, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(song.artist, style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1)
                                    }
                                }
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }

                // ── RECENTLY PLAYED ──
                if (recentSongs.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("RECENTLY PLAYED", style = MaterialTheme.typography.labelLarge, color = AntText3)
                            Spacer(Modifier.height(12.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(recentSongs) { index, song ->
                                    Column(
                                        modifier = Modifier.width(120.dp).clickable {
                                            PlayerManager.play(context, recentSongs, index)
                                            vm.isPlayerExpanded.value = true
                                            RequestFullScreenPlayer = true
                                        }
                                    ) {
                                        Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp))) {
                                            AsyncImage(model = song.albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(song.title, style = MaterialTheme.typography.labelLarge, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(song.artist, style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1)
                                    }
                                }
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }

                // ── TOP TRACKS / TRENDING NOW ──
                if (topTracks.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(topTracksLabel, style = MaterialTheme.typography.labelLarge, color = AntText3)
                            Spacer(Modifier.height(12.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(topTracks.take(10)) { index, song ->
                                    Column(
                                        modifier = Modifier.width(120.dp).clickable {
                                            val top10List = topTracks.take(10)
                                            PlayerManager.play(context, top10List, index)
                                            vm.isPlayerExpanded.value = true
                                            RequestFullScreenPlayer = true
                                        }
                                    ) {
                                        Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)).background(AntSurface2)) {
                                            AsyncImage(model = song.albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(song.title, style = MaterialTheme.typography.labelLarge, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(song.artist, style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1)
                                    }
                                }
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}
