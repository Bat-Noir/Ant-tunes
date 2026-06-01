package com.ant.tunes.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ant.tunes.data.Song
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor
import com.ant.tunes.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun YtMusicHomepage(vm: PlayerViewModel, onNavigateToPlayer: () -> Unit) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    // States for holding extracted feed items
    var quickPicks by remember { mutableStateOf<List<Song>>(emptyList()) }
    var recommendedAlbums by remember { mutableStateOf<List<com.ant.tunes.ui.BrowseCard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            // Pulls live data from the authenticated YTM layout engine
            val tracks = com.ant.tunes.ytmusic.YoutubeSyncEngine.fetchTracksFromPlaylist("LM").take(6)
            quickPicks = tracks
            recommendedAlbums = com.ant.tunes.ytmusic.YoutubeSyncEngine.fetchLibraryItems("albums").take(5)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accent, strokeWidth = 2.dp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 56.dp, bottom = 140.dp)
        ) {
            // ── PREMIUM HEADER ──
            item {
                Text("YTM DASHBOARD", style = MaterialTheme.typography.displayMedium, color = AntText)
                Text("Powered by your connected library", style = MaterialTheme.typography.labelSmall, color = AntText3)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── QUICK PICKS GRID (2x3 Layout) ──
            item {
                Text("QUICK PICKS", style = MaterialTheme.typography.labelLarge, color = AntText3)
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickPicks.chunked(2).forEach { rowSongs ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowSongs.forEach { song ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AntSurface1)
                                        .clickable {
                                            // Intercepts and executes playback routing via NewPipe/iTunes matching
                                            playWithEnhancedMetadata(context, song)
                                            onNavigateToPlayer()
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    coil.compose.AsyncImage(
                                        model = song.albumArt,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(song.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = AntText2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            if (rowSongs.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── RECOMMENDED ALBUMS ──
            item {
                Text("RECOMMENDED ALBUMS", style = MaterialTheme.typography.labelLarge, color = AntText3)
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(recommendedAlbums) { album ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable {
                                    vm.loadAlbumById(album.id)
                                    // Handle navigation context or open details sub-screen here
                                }
                        ) {
                            coil.compose.AsyncImage(
                                model = album.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(album.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ── PLAYBACK ENGINE BRIDGING ──
private fun playWithEnhancedMetadata(context: android.content.Context, song: Song) {
    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        var finalTrack = song

        // 1. iTunes Artwork API Resolution Fallback (Fetches ultra high-res covers instead of small thumbnails)
        try {
            val query = java.net.URLEncoder.encode("${song.title} ${song.artist}", "UTF-8")
            val url = java.net.URL("https://itunes.apple.com/search?term=$query&media=music&limit=1")
            val connection = url.openConnection() as java.net.HttpURLConnection
            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }

            if (jsonText.contains("artworkUrl100")) {
                val resolvedArt = jsonText.substringAfter("artworkUrl100\":\"").substringBefore("\"")
                    .replace("100x100bb.jpg", "600x600bb.jpg") // Force high-res scaling
                finalTrack = song.copy(albumArt = resolvedArt)
            }
        } catch (e: Exception) {
            e.printStackTrace() // Use standard thumbnail if lookup fails
        }

        // 2. Stream URL resolving via NewPipe Extractor
        try {
            // Assuming your local NewPipe Link Extractor architecture takes the song audio signature/ID:
            // val streamUrl = com.ant.tunes.newpipe.NewPipeExtractor.getAudioStreamUrl(song.id)
            // finalTrack = finalTrack.copy(source = streamUrl)

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                PlayerManager.playStream(context, finalTrack)
            }
        } catch (e: Exception) {
            // Playback fallback
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                PlayerManager.playStream(context, finalTrack)
            }
        }
    }
}
