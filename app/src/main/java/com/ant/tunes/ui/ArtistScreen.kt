package com.ant.tunes.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ant.tunes.data.Song
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.AntBlack
import com.ant.tunes.ui.theme.AntGlassBorder
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntSurface2
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor

@Composable
fun ArtistScreen(
    artist: BrowseCard,
    artistTracks: List<Song>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    // 🟢 FIXED: Check if the artist is already in our saved global list!
    var isFollowed by remember { mutableStateOf(globalFollowedArtists.any { it.id == artist.id }) }

    Column(modifier = Modifier.fillMaxSize().background(AntBlack)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(AntSurface1, AntBlack), startY = 0f)))

            AsyncImage(
                model = artist.imageUrl,
                contentDescription = artist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(200.dp).clip(CircleShape).border(2.dp, accent.copy(alpha=0.5f), CircleShape)
            )

            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).statusBarsPadding().size(40.dp).background(AntSurface2, CircleShape).border(1.dp, AntGlassBorder, CircleShape).align(Alignment.TopStart)) {
                Icon(Icons.Default.ArrowBack, "Go Back", tint = AntText, modifier = Modifier.size(20.dp))
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(artist.title, style = MaterialTheme.typography.displayMedium, color = AntText, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        if (artistTracks.isNotEmpty()) {
                            PlayerManager.play(context, artistTracks, 0)
                            RequestFullScreenPlayer = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("PLAY", style = MaterialTheme.typography.labelMedium, color = AntBlack)
                }

                OutlinedButton(
                    onClick = {
                        if (artistTracks.isNotEmpty()) {
                            PlayerManager.play(context, artistTracks.shuffled(), 0)
                            RequestFullScreenPlayer = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, AntGlassBorder),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("SHUFFLE", style = MaterialTheme.typography.labelMedium, color = AntText)
                }

                Button(
                    onClick = {
                        // 🟢 FIXED: Actually modify the list and save it to AppDataManager!
                        if (isFollowed) {
                            globalFollowedArtists.removeAll { it.id == artist.id }
                        } else {
                            globalFollowedArtists.add(artist)
                        }

                        isFollowed = !isFollowed
                        com.ant.tunes.player.AppDataManager.saveFollowedArtists(context, globalFollowedArtists)
                        Toast.makeText(context, if(isFollowed) "Following ${artist.title}" else "Unfollowed", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if(isFollowed) AntSurface1 else accent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if(isFollowed) "FOLLOWING" else "FOLLOW", style = MaterialTheme.typography.labelMedium, color = if(isFollowed) accent else AntBlack, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("TOP TRACKS", style = MaterialTheme.typography.labelMedium, color = AntText3)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
                    itemsIndexed(artistTracks) { index, song ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                PlayerManager.play(context, artistTracks, index)
                                RequestFullScreenPlayer = true
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = AntText3, modifier = Modifier.width(30.dp))

                            AsyncImage(
                                model = song.albumArt,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1)
                                Text(song.album, style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1)
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
