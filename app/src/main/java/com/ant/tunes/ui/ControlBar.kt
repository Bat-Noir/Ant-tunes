package com.ant.tunes.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.data.DownloadState

@Composable
fun ControlBar(
    context: Context,
    onDownload: () -> Unit = {},
    onPrev: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onRepeat: () -> Unit = {},
    isPlaying: Boolean = false
) {

    val currentSong by PlayerManager.currentSong.collectAsState()
    val repeatMode by PlayerManager.repeatMode.collectAsState()
    val songId = currentSong?.id

    val downloadState = if (songId != null) {
        PlayerManager.downloadStates[songId] ?: DownloadState.NOT_DOWNLOADED
    } else DownloadState.NOT_DOWNLOADED

    val progress = if (songId != null) {
        PlayerManager.downloadProgress[songId] ?: 0f
    } else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 🔥 DOWNLOAD BUTTON (SMART)
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {

            when (downloadState) {

                DownloadState.NOT_DOWNLOADED -> {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                currentSong?.let {
                                    PlayerManager.downloadSong(context, it)
                                }
                            }
                    )
                }

                DownloadState.DOWNLOADING -> {
                    CircularProgressIndicator(
                        progress = progress,
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DownloadState.DOWNLOADED -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Downloaded",
                        tint = Color.Green,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        IconButton(onClick = onPrev) {
            Icon(Icons.Default.SkipPrevious, null, tint = Color.White)
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        IconButton(onClick = onNext) {
            Icon(Icons.Default.SkipNext, null, tint = Color.White)
        }

        IconButton(onClick = { PlayerManager.toggleRepeat() }) {
            Icon(
                imageVector = when (repeatMode) {
                    PlayerManager.RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = when (repeatMode) {
                    PlayerManager.RepeatMode.OFF -> Color.Gray
                    else -> Color.Green
                }
            )
        }
    }
}