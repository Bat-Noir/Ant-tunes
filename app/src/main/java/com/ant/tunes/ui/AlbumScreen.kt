package com.ant.tunes.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ant.tunes.data.Song
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.*

@Composable
fun AlbumScreen(
    album: BrowseCard,
    albumTracks: List<Song>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current
    var isSaved by remember { mutableStateOf(globalSavedAlbums.any { it.id == album.id }) }

    Column(modifier = Modifier.fillMaxSize().background(AntBlack)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            AsyncImage(model = album.imageUrl, contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, AntBlack), startY = 300f)))
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).statusBarsPadding().size(40.dp).background(AntSurface2, CircleShape).border(1.dp, AntGlassBorder, CircleShape).align(Alignment.TopStart)) {
                Icon(Icons.Default.ArrowBack, "Go Back", tint = AntText, modifier = Modifier.size(20.dp))
            }
            Text(album.title, style = MaterialTheme.typography.displayMedium, color = AntText, modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 16.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        if (albumTracks.isNotEmpty()) {
                            PlayerManager.play(context, albumTracks, 0)
                            RequestFullScreenPlayer = true // 🟢 TRIGGER FULLSCREEN
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("PLAY", style = MaterialTheme.typography.labelLarge, color = AntBlack)
                }
                OutlinedButton(
                    onClick = {
                        if (albumTracks.isNotEmpty()) {
                            PlayerManager.play(context, albumTracks.shuffled(), 0)
                            RequestFullScreenPlayer = true // 🟢 TRIGGER FULLSCREEN
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, AntGlassBorder)
                ) {
                    Text("SHUFFLE", style = MaterialTheme.typography.labelLarge, color = AntText)
                }
                IconButton(
                    onClick = {
                        // 🟢 FIXED: Actually modify the list and save it to AppDataManager!
                        if (isSaved) {
                            globalSavedAlbums.removeAll { it.id == album.id }
                        } else {
                            globalSavedAlbums.add(album)
                        }

                        isSaved = !isSaved
                        com.ant.tunes.player.AppDataManager.saveAlbums(context, globalSavedAlbums)
                        Toast.makeText(context, if(isSaved) "Album Saved!" else "Album Removed", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(48.dp).background(AntSurface1, CircleShape).border(1.dp, AntGlassBorder, CircleShape)
                ) {
                    Icon(if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Save Album", tint = if (isSaved) accent else AntText)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("TRACKS", style = MaterialTheme.typography.labelMedium, color = AntText3)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
                    itemsIndexed(albumTracks) { index, song ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                PlayerManager.play(context, albumTracks, index)
                                RequestFullScreenPlayer = true // 🟢 TRIGGER FULLSCREEN
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = AntText3, modifier = Modifier.width(30.dp))

                            AsyncImage(
                                model = song.albumArt.ifEmpty { album.imageUrl },
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1)
                                Text(song.artist, style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1)
                            }
                            IconButton(onClick = { onAddToPlaylist(song) }) { Icon(Icons.Default.Add, "Add to Playlist", tint = accent) }
                        }
                        HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
