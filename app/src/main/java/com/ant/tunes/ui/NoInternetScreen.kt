package com.ant.tunes.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor

@Composable
fun NoInternetBanner() {
    val alpha by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 0.6f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "a"
        )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFF6B6B).copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.WifiOff, null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(16.dp).alpha(alpha)
            )
            Spacer(Modifier.width(8.dp))
            Text("NO INTERNET — OFFLINE MODE",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFF6B6B))
        }
    }
}

// Loading shimmer for song rows
@Composable
fun ShimmerSongRow() {
    val shimmer by rememberInfiniteTransition(label = "shimmer")
        .animateFloat(
            initialValue = 0.2f, targetValue = 0.5f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "s"
        )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = shimmer))
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f).height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = shimmer))
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f).height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = shimmer * 0.7f))
            )
        }
    }
}

// Full offline screen shown on home when no internet + no cached data
@Composable
fun OfflineHomeState(onGoToDownloads: () -> Unit) {
    val accent = LocalAccentColor.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AntSurface1)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.WifiOff, null,
            tint = AntText3, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("YOU'RE OFFLINE",
            style = MaterialTheme.typography.titleSmall, color = AntText)
        Spacer(Modifier.height(8.dp))
        Text("No internet connection detected.\nYour downloaded songs are available.",
            style = MaterialTheme.typography.bodySmall,
            color = AntText2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onGoToDownloads,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("PLAY DOWNLOADS",
                style = MaterialTheme.typography.labelLarge)
        }
    }
}