package com.ant.tunes.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.itemsIndexed // 🟢 Ensures itemsIndexed works perfectly
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.*
import com.ant.tunes.viewmodel.PlayerViewModel

@Composable
fun LyricsTab(
    lyrics: String?,
    isLoading: Boolean,
    lrcLines: List<PlayerViewModel.LrcLine> = emptyList(),
    isFullScreen: Boolean = false,
    onToggleFullScreen: () -> Unit = {},
    onRetry: () -> Unit = {} // 🟢 ADDED: Command to retry fetching
) {
    val accent = LocalAccentColor.current
    val position by PlayerManager.currentPosition.collectAsState()
    val listState = rememberLazyListState()

    // Find current line index
    val currentLineIndex = remember(position, lrcLines) {
        if (lrcLines.isEmpty()) -1
        else {
            val idx = lrcLines.indexOfLast { it.timeMs <= position }
            if (idx < 0) 0 else idx
        }
    }

    // 🟢 FIXED: Added 'lrcLines' so it instantly re-syncs when you skip a song!
    LaunchedEffect(currentLineIndex, lrcLines) {
        if (currentLineIndex >= 0 && lrcLines.isNotEmpty()) {
            listState.animateScrollToItem(
                index = (currentLineIndex - 2).coerceAtLeast(0),
                scrollOffset = 0
            )
        }
    }

    val shimmer by rememberInfiniteTransition(label = "lyr")
        .animateFloat(
            initialValue = 0.15f, targetValue = 0.4f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "ls"
        )

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(7) { i ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(
                                    when (i % 3) { 0 -> 0.9f; 1 -> 0.7f; else -> 0.55f }
                                )
                                .height(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = shimmer))
                        )
                    }
                }
            }

            lrcLines.isNotEmpty() -> {
                // ── SYNCED SING-ALONG MODE ──
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        // 🟢 FIXED: Replaced 'horizontal' with 'start' and 'end'
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 60.dp,
                            end = 16.dp,
                            bottom = 140.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(lrcLines) { index, line ->
                            if (line.text.isBlank()) {
                                Spacer(Modifier.height(8.dp))
                                return@itemsIndexed
                            }

                            val isCurrent = index == currentLineIndex
                            val isPast = index < currentLineIndex

                            val alpha by animateFloatAsState(
                                targetValue = when {
                                    isCurrent -> 1f
                                    isPast    -> 0.45f
                                    else      -> 0.3f
                                },
                                animationSpec = tween(300),
                                label = "la$index"
                            )
                            val scale by animateFloatAsState(
                                targetValue = if (isCurrent) 1.05f else 1f,
                                animationSpec = spring(Spring.DampingRatioMediumBouncy),
                                label = "ls$index"
                            )

                            Text(
                                text = line.text,
                                style = if (isCurrent)
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isFullScreen) 26.sp else 22.sp
                                    )
                                else
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = if (isFullScreen) 20.sp else 16.sp
                                    ),
                                color = if (isCurrent) accent
                                else AntText.copy(alpha = alpha),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .animateItem() // Keeping your exact animation
                            )
                        }
                    }

                    // Fullscreen toggle button
                    IconButton(
                        onClick = onToggleFullScreen,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(
                                AntSurface1.copy(alpha = 0.5f), // 🟢 Transparent backing
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp, AntGlassBorder,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            if (isFullScreen) Icons.Default.FullscreenExit
                            else Icons.Default.Fullscreen,
                            null,
                            tint = AntText2,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            !lyrics.isNullOrEmpty() -> {
                // ── PLAIN LYRICS MODE ──
                Box(modifier = Modifier.fillMaxSize()) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            // 🟢 FIXED: Replaced 'horizontal' with 'start' and 'end'
                            .padding(
                                start = 16.dp,
                                top = 40.dp,
                                end = 16.dp,
                                bottom = 140.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        lyrics.split("\n").forEach { line ->
                            if (line.isBlank()) {
                                Spacer(Modifier.height(14.dp))
                            } else {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AntText.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Fullscreen toggle
                    IconButton(
                        onClick = onToggleFullScreen,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(AntSurface1.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .border(1.dp, AntGlassBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            if (isFullScreen) Icons.Default.FullscreenExit
                            else Icons.Default.Fullscreen,
                            null,
                            tint = AntText2,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            else -> {
                // ── EMPTY STATE WITH RETRY ──
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("♪",
                        style = MaterialTheme.typography.displayLarge,
                        color = AntText3)
                    Spacer(Modifier.height(12.dp))
                    Text("NO LYRICS FOUND",
                        style = MaterialTheme.typography.labelLarge,
                        color = AntText3)
                    Spacer(Modifier.height(6.dp))
                    Text("Lyrics unavailable for this track",
                        style = MaterialTheme.typography.bodySmall,
                        color = AntText3,
                        textAlign = TextAlign.Center)

                    Spacer(Modifier.height(24.dp))

                    // 🟢 UI FIX: The Retry Button
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = AntSurface1),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, AntGlassBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Retry", color = accent, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
