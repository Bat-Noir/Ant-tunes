package com.ant.tunes.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ant.tunes.player.PlayerManager

@Composable
fun QueuePanel() {
    val context = LocalContext.current
    val playlist by PlayerManager.playlistFlow.collectAsState()
    val downloadedSongs by PlayerManager.downloadedSongs.collectAsState()
    val currentSong by PlayerManager.currentSong.collectAsState()
    val isOffline by PlayerManager.isOfflineMode.collectAsState()

    // 🔥 SMART QUEUE SOURCE
    val displayList = if (isOffline) downloadedSongs else playlist

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp) // 🔥 slightly tighter
            .background(
                Color(0xFF1A1A1A),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {

        Text(
            text = if (isOffline) "Offline Queue" else "Up Next",
            color = Color.White.copy(alpha = 0.65f),
            style = MaterialTheme.typography.titleMedium, // 👈 bigger
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn {

            // 🔥 EMPTY STATE
            if (displayList.isEmpty()) {
                item {
                    Text(
                        text = "No songs in queue",
                        color = Color.Gray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            itemsIndexed(
                items = displayList,
                key = { index, song -> "${song.id}_${index}" }
            ) { index, song ->

                val isCurrent = currentSong?.id == song.id

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = LocalIndication.current,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {

                            if (isOffline) {
                                // 🔥 play from offline list
                                PlayerManager.playStream(context, song)
                            } else {
                                // 🔥 play from online queue
                                PlayerManager.seekToIndex(index)
                            }
                        }
                        .background(
                            if (isCurrent)
                                Color.White.copy(alpha = 0.08f)
                            else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp), // 🔥 reduced spacing
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Image(
                        painter = rememberAsyncImagePainter(song.albumArt),
                        contentDescription = null,
                        modifier = Modifier
                            .size(38.dp) // 🔥 slightly smaller
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {

                        Text(
                            text = song.title,
                            color = if (isCurrent) Color.White else Color.LightGray,
                            maxLines = 1
                        )

                        Text(
                            text = song.artist,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}