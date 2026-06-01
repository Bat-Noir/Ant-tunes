package com.ant.tunes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntSurface2
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor
import com.ant.tunes.viewmodel.PlayerViewModel

@Composable
fun YtMusicDashboardSection(vm: PlayerViewModel, onAlbumClick: (BrowseCard) -> Unit) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    // 🟢 STICKY DATA: Reference the ViewModel state instead of local 'remember'
    val dynamicCarousels = vm.dashboardCarousels.value
    var isLoading by remember { mutableStateOf(!vm.isDashboardLoaded) }

    LaunchedEffect(Unit) {
        // 🟢 ONLY fetch if we haven't loaded data yet
        if (!vm.isDashboardLoaded) {
            isLoading = true
            try {
                vm.dashboardCarousels.value = com.ant.tunes.ytmusic.YoutubeSyncEngine.fetchDynamicHomeCarousels()
                vm.isDashboardLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent, strokeWidth = 2.dp)
            }
        } else {
            dynamicCarousels.forEach { shelf ->
                Text(shelf.title.uppercase(), style = MaterialTheme.typography.labelLarge, color = AntText3)
                Spacer(modifier = Modifier.height(12.dp))

                if (shelf.songs.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(shelf.songs.chunked(3)) { columnSongs ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.width(320.dp)) {
                                columnSongs.forEach { song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AntSurface1)
                                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                PlayerManager.play(context, listOf(song), 0)
                                                RequestFullScreenPlayer = true
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = song.albumArt,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column(modifier = Modifier.padding(horizontal = 10.dp).weight(1f)) {
                                            Text(song.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = AntText2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                if (shelf.albums.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        // In YtMusicDashboardSection, inside the LazyRow for albums
                        items(shelf.albums) { album ->
                            Column(
                                modifier = Modifier.width(140.dp).clickable {
                                    // 🟢 CORRECT: Just update the state. The UI will swap automatically.
                                    onAlbumClick(album)
                                }
                            ) {
                                AsyncImage(
                                    model = album.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(140.dp).clip(RoundedCornerShape(16.dp)).background(AntSurface2),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(album.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
