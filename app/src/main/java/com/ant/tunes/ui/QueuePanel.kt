package com.ant.tunes.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ant.tunes.data.Song
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor


@Composable
fun QueuePanel(
    isExpanded: Boolean = false,
    onSongSelected: () -> Unit = {}
) {
    val context = LocalContext.current
    val playlist by PlayerManager.playlistFlow.collectAsState()
    val downloadedSongs by PlayerManager.downloadedSongs.collectAsState()
    val currentSong by PlayerManager.currentSong.collectAsState()
    val isOffline by PlayerManager.isOfflineMode.collectAsState()
    val accent = LocalAccentColor.current // 🟢 Dynamic Accent

    val displayList = if (isOffline) downloadedSongs else playlist

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 🟢 FIXED: Solid dark background when expanded removes the transparency bleed
            .background(if (isExpanded) Color(0xFF121212) else AntSurface1, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = if (isOffline) "OFFLINE QUEUE" else "UP NEXT",
            style = MaterialTheme.typography.labelLarge,
            color = AntText3,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        if (displayList.isEmpty()) {
            Text(
                text = "No songs in queue",
                style = MaterialTheme.typography.bodySmall,
                color = AntText3,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            if (isExpanded) {
                // 🟢 FIXED: Infinite Scrolling for the Full Player Overlay
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(displayList) { index, song ->
                        val isCurrent = currentSong?.id == song.id
                        QueueItemRow(
                            song = song,
                            isCurrent = isCurrent,
                            index = index,
                            isLastItem = index == displayList.size - 1,
                            isExpanded = true,
                            accent = accent,
                            onClick = {
                                if (isOffline) PlayerManager.playStream(context, song)
                                else PlayerManager.play(context, displayList, index)

                                RequestFullScreenPlayer = true // Instantly expands/ensures player is up
                                onSongSelected() // 🟢 Instantly closes the queue overlay
                            },
                            onMoveUp = { PlayerManager.moveQueueItem(index, index - 1) },
                            onMoveDown = { PlayerManager.moveQueueItem(index, index + 1) }
                        )
                    }
                }
            } else {
                // 🟢 Standard static view (Max 5 items) to prevent crashing the HomeContent scrolling
                displayList.take(5).forEachIndexed { index, song ->
                    val isCurrent = currentSong?.id == song.id
                    QueueItemRow(
                        song = song,
                        isCurrent = isCurrent,
                        index = index,
                        isLastItem = false,
                        isExpanded = false,
                        accent = accent,
                        onClick = {
                            if (isOffline) PlayerManager.playStream(context, song)
                            else PlayerManager.seekToIndex(index)

                            RequestFullScreenPlayer = true
                        },
                        onMoveUp = {},
                        onMoveDown = {}
                    )
                }
                if (displayList.size > 5) {
                    Text(
                        text = "+ ${displayList.size - 5} MORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = AntText3,
                        modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QueueItemRow(
    song: Song,
    isCurrent: Boolean,
    index: Int,
    isLastItem: Boolean,
    isExpanded: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .background(
                if (isCurrent) accent.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(song.albumArt),
            contentDescription = null,
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) accent else AntText,
                maxLines = 1
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = AntText3,
                maxLines = 1
            )
        }

        // 🟢 FIXED: Reordering buttons (Only visible in expanded queue)
        if (isExpanded) {
            if (index > 0) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowUpward, null, tint = AntText3, modifier = Modifier.size(18.dp))
                }
            }
            if (!isLastItem) {
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowDownward, null, tint = AntText3, modifier = Modifier.size(18.dp))
                }
            }
        } else if (isCurrent) {
            Box(modifier = Modifier.size(6.dp).background(accent, CircleShape))
        }
    }
}
