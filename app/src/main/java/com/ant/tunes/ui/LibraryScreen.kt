@file:OptIn(ExperimentalMaterial3Api::class)

package com.ant.tunes.ui

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.ant.tunes.data.Song
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.*
import java.util.UUID
import kotlinx.coroutines.launch

// ═══════════════════════════════════════
// 🟢 GLOBAL STATES & DATA STRUCTURES
// ═══════════════════════════════════════
class PlaylistData(val id: String, var name: MutableState<String>) {
    val tracks = mutableStateListOf<Song>()
}

val globalPlaylists = mutableStateListOf<PlaylistData>()
val globalLikedSongs = mutableStateListOf<Song>()
// 🟢 NEW: Global States for Albums and Artists!
val globalSavedAlbums = mutableStateListOf<BrowseCard>()
val globalFollowedArtists = mutableStateListOf<BrowseCard>()

fun initGlobalData(context: android.content.Context) {
    if (globalPlaylists.isEmpty()) {
        globalPlaylists.addAll(com.ant.tunes.player.AppDataManager.loadPlaylists(context))
    }
    if (globalLikedSongs.isEmpty()) {
        globalLikedSongs.addAll(com.ant.tunes.player.AppDataManager.loadLikedSongs(context))
    }
    // 🟢 NEW: Load Albums and Artists on Boot
    if (globalSavedAlbums.isEmpty()) {
        globalSavedAlbums.addAll(com.ant.tunes.player.AppDataManager.loadSavedAlbums(context))
    }
    if (globalFollowedArtists.isEmpty()) {
        globalFollowedArtists.addAll(com.ant.tunes.player.AppDataManager.loadFollowedArtists(context))
    }
}
var TargetPlaylistId by mutableStateOf<String?>(null)
var RequestTabSwitch by mutableStateOf<NavTab?>(null)
var RequestFullScreenPlayer by mutableStateOf(false)

var IsLibrarySubScreenActive by mutableStateOf(false)

// ═══════════════════════════════════════
// 📚 LIBRARY ROUTER
// ═══════════════════════════════════════
@Composable
fun LibraryScreen(vm: com.ant.tunes.viewmodel.PlayerViewModel) {
    var activeRoute by remember { mutableStateOf("Main") }
    var selectedPlaylistId by remember { mutableStateOf("") }

    // 🟢 NEW: Native Routing states for Albums and Artists
    var selectedAlbum by remember { mutableStateOf<BrowseCard?>(null) }
    var selectedArtist by remember { mutableStateOf<BrowseCard?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    IsLibrarySubScreenActive = activeRoute != "Main" || selectedAlbum != null || selectedArtist != null

    androidx.activity.compose.BackHandler(enabled = IsLibrarySubScreenActive) {
        if (selectedAlbum != null) selectedAlbum = null
        else if (selectedArtist != null) selectedArtist = null
        else activeRoute = "Main"
    }

    // 🟢 Bottom Sheet for adding to playlist from Album/Artist views
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
                                    android.widget.Toast.makeText(context, "Added to ${playlist.name.value}", android.widget.Toast.LENGTH_SHORT).show()
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

    // 🟢 Handle Screens!
    if (selectedAlbum != null) {
        // 🟢 Wait for the ViewModel to finish clearing the old list and fetching the new one
        AlbumScreen(
            album = selectedAlbum!!,
            albumTracks = vm.albumTracks, // This automatically updates when the ViewModel finishes!
            isLoading = vm.isAlbumLoading.value,
            onBack = {
                selectedAlbum = null
                // Don't clear the tracks here, let the ViewModel handle it on the next click!
            },
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
    } else {

        when (activeRoute) {
            "Main" -> LibraryMain(
                onNavigate = { route, id ->
                    if (route == "PlaylistDetail") selectedPlaylistId = id ?: ""
                    activeRoute = route
                },
                onOpenAlbum = {
                    selectedAlbum = it
                    vm.loadAlbumById(it.id)
                },
                onOpenArtist = {
                    selectedArtist = it
                    vm.loadArtistById(it.id)
                }
            )
            "Liked" -> LikedSongsScreen(onBack = { activeRoute = "Main" })
            "Downloads" -> OfflineScreen(onBack = { activeRoute = "Main" })
            "PlaylistDetail" -> PlaylistDetailScreen(playlistId = selectedPlaylistId, onBack = { activeRoute = "Main" })
        }
    }
}


// ═══════════════════════════════════════
// 📚 LIBRARY MAIN UI
// ═══════════════════════════════════════
@Composable
fun LibraryMain(onNavigate: (String, String?) -> Unit, onOpenAlbum: (BrowseCard) -> Unit, onOpenArtist: (BrowseCard) -> Unit) {
    val songs by PlayerManager.downloadedSongs.collectAsState()
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    var selectedChip by remember { mutableStateOf("All") }
    val chips = listOf("All", "Playlists", "Albums", "Artists", "Downloads")

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<PlaylistData?>(null) }
    var playlistInputName by remember { mutableStateOf("") }
    var showImportSheet by remember { mutableStateOf(false) }

    // ── DIALOGS ──
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", color = AntText) },
            text = {
                OutlinedTextField(
                    value = playlistInputName,
                    onValueChange = { playlistInputName = it },
                    singleLine = true,
                    placeholder = { Text("Playlist name...", color = AntText3) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = AntGlassBorder, focusedTextColor = AntText, unfocusedTextColor = AntText, cursorColor = accent, focusedContainerColor = AntSurface1, unfocusedContainerColor = AntSurface1),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (playlistInputName.isNotBlank()) {
                        globalPlaylists.add(PlaylistData(java.util.UUID.randomUUID().toString(), mutableStateOf(playlistInputName)))
                        com.ant.tunes.player.AppDataManager.savePlaylists(context, globalPlaylists)
                    }
                    playlistInputName = ""
                    showCreateDialog = false
                }) { Text("Create", color = accent) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = AntText2) } },
            containerColor = AntSurface2
        )
    }

    if (showRenameDialog != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Playlist", color = AntText) },
            text = {
                OutlinedTextField(
                    value = playlistInputName,
                    onValueChange = { playlistInputName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = AntGlassBorder, focusedTextColor = AntText, unfocusedTextColor = AntText, cursorColor = accent, focusedContainerColor = AntSurface1, unfocusedContainerColor = AntSurface1),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (playlistInputName.isNotBlank()) {
                        showRenameDialog?.name?.value = playlistInputName
                        com.ant.tunes.player.AppDataManager.savePlaylists(context, globalPlaylists)
                    }
                    playlistInputName = ""
                    showRenameDialog = null
                }) { Text("Rename", color = accent) }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = null }) { Text("Cancel", color = AntText2) } },
            containerColor = AntSurface2
        )
    }

    if (showImportSheet) {
        ImportPlaylistSheet(onDismiss = { showImportSheet = false }, context = context)
    }

    // ── MAIN CONTENT ──
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 160.dp)) {

        // 🟢 HEADER SECTION
        item {
            Spacer(modifier = Modifier.height(56.dp))
            Text("YOUR LIBRARY", style = MaterialTheme.typography.displayMedium, color = AntText, modifier = Modifier.padding(bottom = 16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(chips) { chip ->
                    val isSelected = selectedChip == chip
                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) accent.copy(alpha = 0.15f) else AntSurface1).border(1.dp, if (isSelected) accent.copy(alpha = 0.5f) else AntGlassBorder, RoundedCornerShape(20.dp)).clickable { selectedChip = chip }.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text(chip, style = MaterialTheme.typography.labelMedium, color = if (isSelected) accent else AntText2) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 🟢 OFFLINE / LIKED SECTION
        if (selectedChip == "All") {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibraryCard("Liked Songs", "Playlist", Icons.Default.Favorite, listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)), Modifier.weight(1f)) { onNavigate("Liked", null) }
                    LibraryCard("Downloads", "Offline", Icons.Default.Download, listOf(Color(0xFF10B981), Color(0xFF047857)), Modifier.weight(1f)) { onNavigate("Downloads", null) }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 🟢 ALBUMS SECTION
        if (selectedChip == "All" || selectedChip == "Albums") {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SAVED ALBUMS", style = MaterialTheme.typography.labelLarge, color = AntText3)
                    if (selectedChip == "Albums") Text("${globalSavedAlbums.size} ALBUMS", style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }

            if (globalSavedAlbums.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) { Text("No saved albums yet.", style = MaterialTheme.typography.labelLarge, color = AntText3) } }
            } else {
                val displayAlbums = if (selectedChip == "All") globalSavedAlbums.take(3) else globalSavedAlbums
                items(items = displayAlbums, key = { "album_${it.id}" }) { album ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onOpenAlbum(album) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        coil.compose.AsyncImage(model = album.imageUrl, contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(album.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Album", style = MaterialTheme.typography.bodySmall, color = AntText2)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // 🟢 ARTISTS SECTION
        if (selectedChip == "All" || selectedChip == "Artists") {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("FOLLOWED ARTISTS", style = MaterialTheme.typography.labelLarge, color = AntText3)
                    if (selectedChip == "Artists") Text("${globalFollowedArtists.size} ARTISTS", style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }

            if (globalFollowedArtists.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) { Text("No followed artists yet.", style = MaterialTheme.typography.labelLarge, color = AntText3) } }
            } else {
                val displayArtists = if (selectedChip == "All") globalFollowedArtists.take(3) else globalFollowedArtists
                items(items = displayArtists, key = { "artist_${it.id}" }) { artist ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onOpenArtist(artist) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        coil.compose.AsyncImage(model = artist.imageUrl, contentDescription = null, modifier = Modifier.size(52.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(artist.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Artist", style = MaterialTheme.typography.bodySmall, color = AntText2)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(0.dp)) }
        }

        // 🟢 PLAYLISTS SECTION
        if (selectedChip == "All" || selectedChip == "Playlists") {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("YOUR PLAYLISTS", style = MaterialTheme.typography.labelLarge, color = AntText3)
                    TextButton(onClick = { showImportSheet = true }) { Text("IMPORT", style = MaterialTheme.typography.labelSmall, color = accent) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AntSurface1).clickable { playlistInputName = ""; showCreateDialog = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(AntSurface2), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = accent) }
                    Spacer(Modifier.width(16.dp))
                    Text("Create New Playlist", style = MaterialTheme.typography.titleMedium, color = AntText)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(globalPlaylists) { playlist ->
                var showMenu by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onNavigate("PlaylistDetail", playlist.id) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(AntSurface2), contentAlignment = Alignment.Center) { Icon(Icons.Default.QueueMusic, null, tint = AntText2) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(playlist.name.value, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${playlist.tracks.size} tracks", style = MaterialTheme.typography.bodySmall, color = AntText2)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = AntText2) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color(0xFF1E1E1E))) {
                            DropdownMenuItem(text = { Text("Rename", color = Color.White) }, onClick = { showMenu = false; playlistInputName = playlist.name.value; showRenameDialog = playlist })
                            DropdownMenuItem(text = { Text("Delete", color = Color(0xFFFF4444)) }, onClick = { showMenu = false; globalPlaylists.remove(playlist); com.ant.tunes.player.AppDataManager.savePlaylists(context, globalPlaylists) })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // 🟢 DOWNLOADS SECTION
        if (selectedChip == "Downloads") {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("DOWNLOADED SONGS", style = MaterialTheme.typography.labelLarge, color = AntText3)
                    Text("${songs.size} TRACKS", style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }

            if (songs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NO DOWNLOADS YET", style = MaterialTheme.typography.labelLarge, color = AntText3)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("TAP ↓ ON ANY SONG TO SAVE", style = MaterialTheme.typography.labelSmall, color = AntText3)
                        }
                    }
                }
            } else {
                itemsIndexed(songs) { _, song ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AntSurface1).clickable {
                            PlayerManager.playStream(context, song)
                            RequestFullScreenPlayer = true
                        }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painter = coil.compose.rememberAsyncImagePainter(song.albumArt), contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = AntText2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { PlayerManager.deleteSong(context, song) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, "Delete", tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
//... (Keep the rest of your ImportPlaylistSheet and LibraryCard functions below here exactly as they are) ...

// New composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlaylistSheet(
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    val accent = LocalAccentColor.current
    val coroutineScope = rememberCoroutineScope()

    var isLoadingPlaylists by remember { mutableStateOf(false) }
    var playlists by remember {
        mutableStateOf<List<com.ant.tunes.lastfm.LastFmPlaylistItem>>(emptyList())
    }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var importingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var doneIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errorMessage by remember { mutableStateOf("") }
    var isImportingAll by remember { mutableStateOf(false) }

    val isLoggedIn = com.ant.tunes.lastfm.LastFmRepository.isLoggedIn(context)
    val username = com.ant.tunes.lastfm.LastFmRepository.getUsername(context) ?: ""

    LaunchedEffect(Unit) {
        if (!isLoggedIn) return@LaunchedEffect
        isLoadingPlaylists = true
        errorMessage = ""
        playlists = com.ant.tunes.lastfm.LastFmRepository
            .getUserPlaylistsDirect(context)
        isLoadingPlaylists = false
        if (playlists.isEmpty()) {
            errorMessage = "No playlists found on Last.fm for @$username"
        }
    }

    suspend fun importSingle(p: com.ant.tunes.lastfm.LastFmPlaylistItem) {
        val pid = p.id ?: return
        if (doneIds.contains(pid)) return
        importingIds = importingIds + pid
        try {
            val tracks = com.ant.tunes.lastfm.LastFmRepository
                .getPlaylistTracksDirect(pid)
            val playlist = com.ant.tunes.ui.PlaylistData(
                java.util.UUID.randomUUID().toString(),
                androidx.compose.runtime.mutableStateOf(p.title ?: "Imported")
            )
            tracks.forEach { track ->
                playlist.tracks.add(
                    com.ant.tunes.data.Song(
                        id          = "${track.name}_${track.creator}",
                        title       = track.name ?: "",
                        artist      = track.creator ?: "",
                        albumArt    = "",
                        sourceType  = com.ant.tunes.data.SourceType.LASTFM_IMPORT,
                        source      = "lastfm_import"
                    )
                )
            }
            if (!globalPlaylists.any { it.name.value == playlist.name.value }) {
                globalPlaylists.add(playlist)
                com.ant.tunes.player.AppDataManager.savePlaylists(
                    context, globalPlaylists
                )
            }
            doneIds = doneIds + pid
        } catch (e: Exception) {
            errorMessage = "Failed: ${p.title}: ${e.message}"
        } finally {
            importingIds = importingIds - pid
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0E0E0E),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = AntText3)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ── HEADER ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("IMPORT FROM LAST.FM",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent)
                    if (username.isNotBlank()) {
                        Text("@$username",
                            style = MaterialTheme.typography.labelSmall,
                            color = AntText3)
                    }
                }
                if (selectedIds.isNotEmpty()) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isImportingAll = true
                                selectedIds.forEach { id ->
                                    playlists.find { it.id == id }
                                        ?.let { importSingle(it) }
                                }
                                isImportingAll = false
                                selectedIds = emptySet()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp, vertical = 8.dp
                        )
                    ) {
                        if (isImportingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            "IMPORT ${selectedIds.size}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp)

            // ── CONTENT ──
            when {
                !isLoggedIn -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Connect Last.fm in Profile → Integrations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AntText3,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                isLoadingPlaylists -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = accent, strokeWidth = 2.dp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Loading playlists...",
                                style = MaterialTheme.typography.labelMedium,
                                color = AntText3)
                        }
                    }
                }

                playlists.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            errorMessage.ifEmpty { "No playlists found" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = AntText3,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                else -> {
                    // Select all row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (selectedIds.size == playlists.size) {
                                    emptySet()
                                } else {
                                    playlists.mapNotNull { it.id }.toSet()
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedIds.size == playlists.size,
                            onCheckedChange = {
                                selectedIds = if (it) {
                                    playlists.mapNotNull { p -> p.id }.toSet()
                                } else emptySet()
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = accent,
                                uncheckedColor = AntText3
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Select All",
                            style = MaterialTheme.typography.titleSmall,
                            color = AntText2)
                        Spacer(Modifier.weight(1f))
                        Text("${playlists.size} PLAYLISTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = AntText3)
                    }

                    HorizontalDivider(
                        color = AntGlassBorder, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    // ── PLAYLIST LIST ──
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp),
                        contentPadding = PaddingValues(
                            horizontal = 20.dp,
                            vertical = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = playlists,
                            key = { it.id ?: it.title ?: "" }
                        ) { p ->
                            val pid = p.id ?: return@items
                            val isSelected = selectedIds.contains(pid)
                            val isImporting = importingIds.contains(pid)
                            val isDone = doneIds.contains(pid)
                            val alreadyExists = globalPlaylists.any {
                                it.name.value == p.title
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            isDone -> accent.copy(alpha = 0.08f)
                                            isSelected -> AntSurface2
                                            else -> AntSurface1
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isDone -> accent.copy(alpha = 0.3f)
                                            isSelected -> accent.copy(alpha = 0.2f)
                                            else -> AntGlassBorder
                                        },
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable(enabled = !isDone && !alreadyExists) {
                                        selectedIds = if (isSelected) {
                                            selectedIds - pid
                                        } else {
                                            selectedIds + pid
                                        }
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Checkbox
                                if (!isDone && !alreadyExists) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedIds = if (it) {
                                                selectedIds + pid
                                            } else selectedIds - pid
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = accent,
                                            uncheckedColor = AntText3
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }

                                // Icon
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFFD51007),
                                                    Color(0xFF8B0000)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("lfm",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White)
                                }

                                Spacer(Modifier.width(12.dp))

                                // Info
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        p.title ?: "Playlist",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = when {
                                            isDone || alreadyExists -> AntText2
                                            else -> AntText
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        when {
                                            alreadyExists -> "Already imported"
                                            isDone -> "Imported ✓"
                                            else -> "${p.size ?: "?"} tracks"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            isDone -> accent
                                            alreadyExists -> AntText3
                                            else -> AntText3
                                        }
                                    )
                                }

                                // Action
                                when {
                                    isImporting -> CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = accent,
                                        strokeWidth = 2.dp
                                    )
                                    isDone -> Icon(
                                        Icons.Default.Check, null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    alreadyExists -> Icon(
                                        Icons.Default.Check, null,
                                        tint = AntText3,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    else -> TextButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                importSingle(p)
                                            }
                                        },
                                        contentPadding = PaddingValues(
                                            horizontal = 12.dp, vertical = 4.dp
                                        )
                                    ) {
                                        Text("IMPORT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accent)
                                    }
                                }
                            }
                        }
                    }

                    if (errorMessage.isNotBlank()) {
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier.padding(
                                horizontal = 20.dp, vertical = 8.dp
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun LibraryCard(title: String, subtitle: String, icon: ImageVector, gradient: List<Color>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(AntSurface1).border(1.dp, AntGlassBorder, RoundedCornerShape(16.dp)).clickable { onClick() }.padding(16.dp)) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(gradient)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
