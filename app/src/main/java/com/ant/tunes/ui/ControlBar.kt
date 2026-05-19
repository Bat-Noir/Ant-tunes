package com.ant.tunes.ui

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ant.tunes.data.DownloadState
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.AntBlack
import com.ant.tunes.ui.theme.LocalAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlBar(
    context: Context,
    title: String,
    artist: String,
    album: String,
    onPrev: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    isPlaying: Boolean = false,
    // PATCH 2 — new params
    isLiked: Boolean = false,
    onToggleLike: () -> Unit = {}
) {
    val accent = LocalAccentColor.current

    val currentSong by PlayerManager.currentSong.collectAsState()
    val repeatMode by PlayerManager.repeatMode.collectAsState()
    val position by PlayerManager.currentPosition.collectAsState()
    val duration by PlayerManager.duration.collectAsState()
    val isPlayingState by PlayerManager.isPlayingFlow.collectAsState()
// 🟢 Add this state to track when the file is actively downloading
    var isDownloading by remember { mutableStateOf(false) }

    var isShuffle by remember { mutableStateOf(false) }
    // 🟢 ADDED: Buffering State & Shimmer Animation
    val isBuffering by PlayerManager.isBuffering.collectAsState()

    val shimmerX by androidx.compose.animation.core.rememberInfiniteTransition(label = "seek_shimmer")
        .animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "sx"
        )

    // PATCH 2 — removed local isLiked state, now driven by param
    val safePosition = position.coerceAtLeast(0L)
    val safeDuration = duration.coerceAtLeast(0L)
    val actualProgress = if (safeDuration > 0) safePosition.toFloat() / safeDuration else 0f

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(actualProgress) {
        if (!isDragging) sliderPosition = actualProgress
    }

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    var currentVolume by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        )
    }

    var thumbRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlayingState) {
        if (isPlayingState) {
            var lastFrameTime = withFrameNanos { it }
            while (true) {
                val currentFrameTime = withFrameNanos { it }
                val deltaMs = (currentFrameTime - lastFrameTime) / 1_000_000f
                lastFrameTime = currentFrameTime
                thumbRotation = (thumbRotation + (deltaMs * 360f / 1500f)) % 360f
            }
        }
    }

    val artistAlbumText = if (
        album.isNotBlank() &&
        album != "Unknown Album" &&
        album != "Unknown"
    ) "$artist • $album" else artist

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {

        // ── 1. TITLE ──
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── 2. SHUFFLE / ARTIST+ALBUM / LIKE ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { isShuffle = !isShuffle },
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    Icons.Default.Shuffle, "Shuffle",
                    tint = if (isShuffle) accent else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = artistAlbumText,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            // PATCH 2 — wired to isLiked param + onToggleLike callback
            IconButton(
                onClick = { onToggleLike() },
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🟢 ADDED: Dynamic Buffering / Slider Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            if (isBuffering) {
                // ── SPOTIFY-STYLE LOADING SHIMMER ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.3f)
                            .offset(x = (shimmerX * androidx.compose.ui.platform.LocalDensity.current.run {
                                300.dp.toPx()
                            } / androidx.compose.ui.platform.LocalDensity.current.density).dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        accent.copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                ),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            } else {
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        isDragging = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        PlayerManager.seekTo((sliderPosition * safeDuration).toLong())
                    },
                    modifier = Modifier.fillMaxWidth().height(30.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent,
                        activeTrackColor = accent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .graphicsLayer { rotationZ = thumbRotation }
                                .background(accent, RoundedCornerShape(3.dp))
                        )
                    }
                )
            }
        }


        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayTime = if (isDragging) {
                    (sliderPosition * safeDuration).toLong()
                } else safePosition
                Text(formatTime(displayTime),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall)
                Text(formatTime(safeDuration),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 4. MAIN CONTROLS ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val downloadState = PlayerManager.downloadStates[currentSong?.id]
                ?: DownloadState.NOT_DOWNLOADED

            // 🟢 Auto-stop the spinner when the background download finishes!
            LaunchedEffect(downloadState) {
                if (downloadState == DownloadState.DOWNLOADED) {
                    isDownloading = false
                }
            }

            // ── DOWNLOAD BUTTON WITH SPINNER ──
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = accent,
                        strokeWidth = 2.5.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                } else if (downloadState == DownloadState.DOWNLOADED) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone, // Looks cleaner than standard Check
                        contentDescription = "Downloaded",
                        tint = accent,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    IconButton(
                        onClick = {
                            isDownloading = true
                            currentSong?.let { PlayerManager.downloadSong(context, it) }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onPrev, modifier = Modifier.size(58.dp)) {
                Icon(Icons.Default.SkipPrevious, null,
                    tint = Color.White, modifier = Modifier.size(34.dp))
            }

            Box(
                modifier = Modifier
                    .size(66.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isPlayingState) Icons.Default.Pause
                        else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = AntBlack,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            IconButton(onClick = onNext, modifier = Modifier.size(58.dp)) {
                Icon(Icons.Default.SkipNext, null,
                    tint = Color.White, modifier = Modifier.size(34.dp))
            }

            IconButton(
                onClick = { PlayerManager.toggleRepeat() },
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = when (repeatMode) {
                        PlayerManager.RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatMode == PlayerManager.RepeatMode.OFF) Color.White
                    else accent,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 5. VOLUME & QUEUE ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VolumeUp, null,
                tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Slider(
                value = currentVolume,
                onValueChange = {
                    currentVolume = it
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
                },
                valueRange = 0f..maxVolume,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    thumbColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = onOpenQueue,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.Default.QueueMusic, "Open Queue",
                    tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }


fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}