package com.ant.tunes.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ant.tunes.player.PlayerManager
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBar() {

    val position by PlayerManager.currentPosition.collectAsState()
    val duration by PlayerManager.duration.collectAsState()
    val isPlaying by PlayerManager.isPlayingFlow.collectAsState()

    val safePosition = position.coerceAtLeast(0L)
    val safeDuration = duration.coerceAtLeast(0L)

    val progress = if (safeDuration > 0) safePosition.toFloat() / safeDuration else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ),
        label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {

        Slider(
            value = progress,
            onValueChange = {
                val newPosition = (it * safeDuration).toLong()
                PlayerManager.seekTo(newPosition)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp) // 🔥 shrink slider height
                .padding(bottom = 1.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .graphicsLayer {
                            rotationZ = if (isPlaying) rotation else 0f
                        }
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color.LightGray
                                )
                            ),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatTime(safePosition),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                formatTime(safeDuration),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}