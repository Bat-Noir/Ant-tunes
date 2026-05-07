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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ant.tunes.data.Song
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.*
import java.util.UUID

// 🟢 Explicit imports to fix all "Unresolved Reference" and "getValue/setValue" errors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.collectAsState

// ═══════════════════════════════════════
// 🟢 GLOBAL STATES & DATA STRUCTURES
// ═══════════════════════════════════════
class PlaylistData(val id: String, var name: MutableState<String>) {
    val tracks = mutableStateListOf<Song>()
}

val globalPlaylists = mutableStateListOf<PlaylistData>()
val globalLikedSongs = mutableStateListOf<Song>()
var TargetPlaylistId by mutableStateOf<String?>(null)
var RequestTabSwitch by mutableStateOf<NavTab?>(null)
var RequestFullScreenPlayer by mutableStateOf(false)

// 🟢 NEW: Global state to tell MainActivity when a Library sub-screen is open
var IsLibrarySubScreenActive by mutableStateOf(false)

// ═══════════════════════════════════════
// 📚 LIBRARY ROUTER
// ═══════════════════════════════════════
@Composable
fun LibraryScreen(vm: com.ant.tunes.viewmodel.PlayerViewModel) {
    var activeRoute by remember { mutableStateOf("Main") }
    var selectedPlaylistId by remember { mutableStateOf("") }

    // 🟢 Update the global state so MainActivity knows to hide the Bottom Nav
    IsLibrarySubScreenActive = activeRoute != "Main"

    when (activeRoute) {
        "Main" -> LibraryMain(
            onNavigate = { route, id ->
                if (route == "PlaylistDetail") selectedPlaylistId = id ?: ""
                activeRoute = route
            }
        )
        "Liked" -> LikedSongsScreen(onBack = { activeRoute = "Main" })
        "Downloads" -> OfflineScreen(onBack = { activeRoute = "Main" })
        "PlaylistDetail" -> PlaylistDetailScreen(playlistId = selectedPlaylistId, onBack = { activeRoute = "Main" })
    }
}

// ═══════════════════════════════════════
// 📚 LIBRARY MAIN UI
// ═══════════════════════════════════════
@Composable
fun LibraryMain(onNavigate: (String, String?) -> Unit) {
    val songs by PlayerManager.downloadedSongs.collectAsState()
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    var selectedChip by remember { mutableStateOf("All") }
    val chips = listOf("All", "Playlists", "Downloads")

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<PlaylistData?>(null) }
    var playlistInputName by remember { mutableStateOf("") }

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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = AntGlassBorder,
                        focusedTextColor = AntText, unfocusedTextColor = AntText,
                        cursorColor = accent,
                        focusedContainerColor = AntSurface1, unfocusedContainerColor = AntSurface1
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (playlistInputName.isNotBlank()) {
                        globalPlaylists.add(PlaylistData(UUID.randomUUID().toString(), mutableStateOf(playlistInputName)))
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = AntGlassBorder,
                        focusedTextColor = AntText, unfocusedTextColor = AntText,
                        cursorColor = accent,
                        focusedContainerColor = AntSurface1, unfocusedContainerColor = AntSurface1
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (playlistInputName.isNotBlank()) showRenameDialog?.name?.value = playlistInputName
                    playlistInputName = ""
                    showRenameDialog = null
                }) { Text("Rename", color = accent) }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = null }) { Text("Cancel", color = AntText2) } },
            containerColor = AntSurface2
        )
    }

    // ── MAIN CONTENT ──
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 160.dp)) {
        item {
            Spacer(modifier = Modifier.height(56.dp))
            Text("YOUR LIBRARY", style = MaterialTheme.typography.displayMedium, color = AntText, modifier = Modifier.padding(bottom = 16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(chips) { chip ->
                    val isSelected = selectedChip == chip
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) accent.copy(alpha = 0.15f) else AntSurface1).border(1.dp, if (isSelected) accent.copy(alpha = 0.5f) else AntGlassBorder, RoundedCornerShape(20.dp)).clickable { selectedChip = chip }.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text(chip, style = MaterialTheme.typography.labelMedium, color = if (isSelected) accent else AntText2) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (selectedChip == "All") {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibraryCard("Liked Songs", "Playlist", Icons.Default.Favorite, listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)), Modifier.weight(1f)) { onNavigate("Liked", null) }
                    LibraryCard("Downloads", "Offline", Icons.Default.Download, listOf(Color(0xFF10B981), Color(0xFF047857)), Modifier.weight(1f)) { onNavigate("Downloads", null) }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (selectedChip == "All" || selectedChip == "Playlists") {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("YOUR PLAYLISTS", style = MaterialTheme.typography.labelLarge, color = AntText3)
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
                            DropdownMenuItem(text = { Text("Delete", color = Color(0xFFFF4444)) }, onClick = { showMenu = false; globalPlaylists.remove(playlist) })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

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
                        Image(painter = rememberAsyncImagePainter(song.albumArt), contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
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
