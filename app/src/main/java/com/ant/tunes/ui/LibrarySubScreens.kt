package com.ant.tunes.ui

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.ant.tunes.ui.theme.AntSurface2
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor

@Composable
fun LikedSongsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val songs = globalLikedSongs
    val accent = LocalAccentColor.current
    val currentSong by PlayerManager.currentSong.collectAsState()

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
                Text("${songs.size} TRACKS", style = MaterialTheme.typography.labelLarge, color = accent, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(24.dp))
            }

            item {
                Row(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                PlayerManager.play(context, songs, 0)
                                RequestFullScreenPlayer = true // 🟢 Instant Play Fullscreen
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
                                RequestFullScreenPlayer = true // 🟢 Instant Play Fullscreen
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

            if (songs.isEmpty()) {
                item {
                    Text("No liked songs yet. Tap the heart icon in the player!", color = AntText3, modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp))
                }
            } else {
                itemsIndexed(songs) { index, song ->
                    val isCurrent = currentSong?.id == song.id
                    Row(
                        modifier = Modifier.fillMaxWidth().background(if (isCurrent) accent.copy(alpha = 0.08f) else Color.Transparent).clickable {
                            PlayerManager.play(context, songs, index)
                            RequestFullScreenPlayer = true // 🟢 Instant Play Fullscreen
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

        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).statusBarsPadding().size(40.dp).background(AntSurface2, CircleShape).border(1.dp, AntGlassBorder, CircleShape).align(Alignment.TopStart)) {
            Icon(Icons.Default.ArrowBack, null, tint = AntText, modifier = Modifier.size(20.dp))
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
                            if (playlist.tracks.isNotEmpty()) {
                                PlayerManager.play(context, playlist.tracks, 0)
                                RequestFullScreenPlayer = true // 🟢 Instant Play Fullscreen
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

            itemsIndexed(playlist.tracks) { index, song ->
                val isCurrent = currentSong?.id == song.id
                Row(
                    modifier = Modifier.fillMaxWidth().background(if (isCurrent) accent.copy(alpha = 0.08f) else Color.Transparent).clickable {
                        PlayerManager.play(context, playlist.tracks, index)
                        RequestFullScreenPlayer = true // 🟢 Instant Play Fullscreen pop-up
                    }.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(AntSurface2)) { AsyncImage(model = song.albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleSmall, color = if (isCurrent) accent else AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1)
                    }

                    if (index > 0) {
                        IconButton(onClick = {
                            val item = playlist.tracks.removeAt(index)
                            playlist.tracks.add(index - 1, item)
                            AppDataManager.savePlaylists(context, globalPlaylists)
                        }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowUpward, null, tint = AntText3, modifier = Modifier.size(20.dp)) }
                    }
                    if (index < playlist.tracks.size - 1) {
                        IconButton(onClick = {
                            val item = playlist.tracks.removeAt(index)
                            playlist.tracks.add(index + 1, item)
                            AppDataManager.savePlaylists(context, globalPlaylists)
                        }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowDownward, null, tint = AntText3, modifier = Modifier.size(20.dp)) }
                    }
                    IconButton(onClick = { playlist.tracks.remove(song) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.RemoveCircle, null, tint = Color(0xFFFF4444).copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }

        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).statusBarsPadding().size(40.dp).background(AntSurface2, CircleShape).border(1.dp, AntGlassBorder, CircleShape).align(Alignment.TopStart)) {
            Icon(Icons.Default.ArrowBack, null, tint = AntText, modifier = Modifier.size(20.dp))
        }
    }
}
