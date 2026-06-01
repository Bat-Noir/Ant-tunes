package com.ant.tunes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ant.tunes.player.AppDataManager
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.AntBlack
import com.ant.tunes.ui.theme.AntGlassBorder
import com.ant.tunes.ui.theme.AntGlassBorderHot
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntSurface2
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor

@Composable
fun LikedSongsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current
    val currentSong by PlayerManager.currentSong.collectAsState()

    // 🟢 SEARCH STATES
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val songs = if (searchQuery.isBlank()) {
        globalLikedSongs
    } else {
        globalLikedSongs.filter { it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(AntBlack)) {
        Box(modifier = Modifier.size(300.dp).offset(x = (-50).dp, y = (-80).dp).blur(80.dp).background(Brush.radialGradient(listOf(accent.copy(alpha = 0.15f), Color.Transparent)), CircleShape))

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 200.dp)) {
            item {
                Spacer(Modifier.height(80.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text("LIKED SONGS", style = MaterialTheme.typography.displayLarge, color = AntText, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(4.dp))
                Text("${globalLikedSongs.size} TRACKS", style = MaterialTheme.typography.labelLarge, color = accent, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(24.dp))
            }

            item {
                Row(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                PlayerManager.play(context, songs, 0)
                                RequestFullScreenPlayer = true
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PLAY ALL", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                PlayerManager.play(context, songs.shuffled(), 0)
                                RequestFullScreenPlayer = true
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, AntGlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AntText)
                    ) {
                        Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("SHUFFLE", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            if (songs.isEmpty() && searchQuery.isBlank()) {
                item {
                    Text("No liked songs yet. Tap the heart icon in the player!", color = AntText3, modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp))
                }
            } else if (songs.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Text("No songs found matching '$searchQuery'", color = AntText3, modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp))
                }
            } else {
                itemsIndexed(songs) { index, song ->
                    val isCurrent = currentSong?.id == song.id
                    Row(
                        modifier = Modifier.fillMaxWidth().background(if (isCurrent) accent.copy(alpha = 0.08f) else Color.Transparent).clickable {
                            PlayerManager.play(context, songs, index)
                            RequestFullScreenPlayer = true
                        }.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) { if (isCurrent) EqBars() else Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = AntText3) }
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(AntSurface2)) { AsyncImage(model = song.albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.titleSmall, color = if (isCurrent) accent else AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1)
                        }
                        IconButton(onClick = { globalLikedSongs.remove(song) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Favorite, null, tint = Color(0xFFEC4899), modifier = Modifier.size(20.dp))
                        }
                    }
                    HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }

        // 🟢 EXPANDING TOP BAR (Liked Songs)
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).background(AntSurface2, CircleShape).border(1.dp, AntGlassBorder, CircleShape)) {
                Icon(Icons.Default.ArrowBack, null, tint = AntText, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            AnimatedVisibility(visible = isSearchActive, modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search liked...", color = AntText3, style = MaterialTheme.typography.labelMedium) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = AntGlassBorder,
                        focusedTextColor = AntText, unfocusedTextColor = AntText,
                        focusedContainerColor = AntSurface1, unfocusedContainerColor = AntSurface1
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(50.dp)
                )
            }

            if (!isSearchActive) Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))

            IconButton(onClick = {
                isSearchActive = !isSearchActive
                if (!isSearchActive) searchQuery = ""
            }, modifier = Modifier.size(40.dp).background(AntSurface2, CircleShape).border(1.dp, AntGlassBorder, CircleShape)) {
                Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, null, tint = AntText, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(playlistId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current
    val currentSong by PlayerManager.currentSong.collectAsState()

    val playlist = globalPlaylists.find { it.id == playlistId }
    if (playlist == null) {
        onBack()
        return
    }

    // 🟢 SEARCH STATES
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTracks = if (searchQuery.isBlank()) {
        playlist.tracks
    } else {
        playlist.tracks.filter { it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(AntBlack)) {
        Box(modifier = Modifier.size(300.dp).offset(x = (-50).dp, y = (-80).dp).blur(80.dp).background(Brush.radialGradient(listOf(accent.copy(alpha = 0.15f), Color.Transparent)), CircleShape))

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 200.dp)) {
            item {
                Spacer(Modifier.height(80.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(AntSurface2).border(1.dp, AntGlassBorderHot, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QueueMusic, null, tint = accent, modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(playlist.name.value.uppercase(), style = MaterialTheme.typography.displayLarge, color = AntText, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(4.dp))
                Text("PLAYLIST • ${playlist.tracks.size} TRACKS", style = MaterialTheme.typography.labelLarge, color = accent, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(24.dp))
            }

            item {
                Row(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (filteredTracks.isNotEmpty()) {
                                PlayerManager.play(context, filteredTracks, 0)
                                RequestFullScreenPlayer = true
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PLAY ALL", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = {
                            TargetPlaylistId = playlist.id
                            RequestTabSwitch = NavTab.SEARCH
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, AntGlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AntText)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ADD TRACK", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            itemsIndexed(filteredTracks) { displayIndex, song ->
                val isCurrent = currentSong?.id == song.id
                val actualIndex = playlist.tracks.indexOf(song) // 🟢 Track original index for moving/deleting

                Row(
                    modifier = Modifier.fillMaxWidth().background(if (isCurrent) accent.copy(alpha = 0.08f) else Color.Transparent).clickable {
                        PlayerManager.play(context, filteredTracks, displayIndex)
                        RequestFullScreenPlayer = true
                    }.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(AntSurface2)) { AsyncImage(model = song.albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleSmall, color = if (isCurrent) accent else AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1)
                    }

                    // 🟢 Hide reorder arrows when searching to prevent bugs!
                    if (!isSearchActive) {
                        if (actualIndex > 0) {
                            IconButton(onClick = {
                                val item = playlist.tracks.removeAt(actualIndex)
                                playlist.tracks.add(actualIndex - 1, item)
                                AppDataManager.savePlaylists(context, globalPlaylists)
                            }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowUpward, null, tint = AntText3, modifier = Modifier.size(20.dp)) }
                        }
                        if (actualIndex < playlist.tracks.size - 1) {
                            IconButton(onClick = {
                                val item = playlist.tracks.removeAt(actualIndex)
                                playlist.tracks.add(actualIndex + 1, item)
                                AppDataManager.savePlaylists(context, globalPlaylists)
                            }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowDownward, null, tint = AntText3, modifier = Modifier.size(20.dp)) }
                        }
                    }

                    IconButton(onClick = {
                        playlist.tracks.remove(song)
                        AppDataManager.savePlaylists(context, globalPlaylists)
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.RemoveCircle, null, tint = Color(0xFFFF4444).copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }

        // 🟢 EXPANDING TOP BAR (Custom Playlists)
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).background(AntSurface2, CircleShape).border(1.dp, AntGlassBorder, CircleShape)) {
                Icon(Icons.Default.ArrowBack, null, tint = AntText, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            AnimatedVisibility(visible = isSearchActive, modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search playlist...", color = AntText3, style = MaterialTheme.typography.labelMedium) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = AntGlassBorder,
                        focusedTextColor = AntText, unfocusedTextColor = AntText,
                        focusedContainerColor = AntSurface1, unfocusedContainerColor = AntSurface1
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(50.dp)
                )
            }

            if (!isSearchActive) Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))

            IconButton(onClick = {
                isSearchActive = !isSearchActive
                if (!isSearchActive) searchQuery = ""
            }, modifier = Modifier.size(40.dp).background(AntSurface2, CircleShape).border(1.dp, AntGlassBorder, CircleShape)) {
                Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, null, tint = AntText, modifier = Modifier.size(20.dp))
            }
        }
    }
}
