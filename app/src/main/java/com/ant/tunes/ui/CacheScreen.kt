package com.ant.tunes.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.automirrored.filled.ArrowBack // 🟢 Using the AutoMirrored version to prevent deprecation warnings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil.compose.rememberAsyncImagePainter
import com.ant.tunes.data.Song
import com.ant.tunes.player.CacheManager
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.*

@OptIn(UnstableApi::class)
@Composable
fun CacheScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    // 🟢 Pulling the REAL history directly from the PlayerManager brain
    val recentSongs by PlayerManager.recentlyPlayed.collectAsState()

    // 🟢 FIXED: Swapped .streamUrl to .id so it matches our bulletproof Cache Engine
    var cachedSongs by remember(recentSongs) {
        mutableStateOf(recentSongs.filter { s ->
            CacheManager.isCached(context, s.id)
        })
    }

    Column(modifier = Modifier.fillMaxSize().background(AntBlack).statusBarsPadding()) {
        // ── HEADER ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.background(AntSurface2, CircleShape)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AntText)
            }
            Spacer(Modifier.width(16.dp))
            Text("Smart Cache", style = MaterialTheme.typography.headlineSmall, color = AntText, modifier = Modifier.weight(1f))

            IconButton(onClick = {
                CacheManager.clearCache(context)
                onBack() // 🟢 This kills the screen instantly after wiping the cache
            }) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = Color(0xFFFF4444))
            }
        }

        if (cachedSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs cached yet. Keep listening!", color = AntText3)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                item {
                    Text(
                        "${cachedSongs.size} TRACKS READY OFFLINE",
                        style = MaterialTheme.typography.labelLarge,
                        color = AntText3,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                itemsIndexed(cachedSongs) { index: Int, song: Song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                PlayerManager.play(context, cachedSongs, index)
                                com.ant.tunes.ui.RequestFullScreenPlayer = true
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Image(
                            painter = rememberAsyncImagePainter(song.albumArt),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                song.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = AntText,
                                maxLines = 1
                            )
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = AntText2,
                                maxLines = 1
                            )
                            // 🟢 FIXED: Swapped .streamUrl to .id
                            Text(
                                com.ant.tunes.player.CacheManager.formatSize(
                                    com.ant.tunes.player.CacheManager.getCachedSongSize(
                                        context,
                                        song.id
                                    )
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = AntText3
                            )
                        }

                        // 🟢 FIXED: Swapped .streamUrl to .id
                        IconButton(
                            onClick = {
                                com.ant.tunes.player.CacheManager.deleteCachedSong(
                                    context,
                                    song.id
                                )
                                // Instantly remove from UI
                                cachedSongs = cachedSongs.filter { it.id != song.id }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete, null,
                                tint = accent.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
