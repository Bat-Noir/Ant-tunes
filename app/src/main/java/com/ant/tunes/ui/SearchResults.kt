package com.ant.tunes.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun SearchResults(
    query: String,
    onSongClick: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val results = viewModel.combinedResults
    val isLoading by viewModel.loading
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            viewModel.searchSongs(query)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .distinctUntilChanged()
            .collectLatest { lastIndex ->
                if (lastIndex != null &&
                    lastIndex >= results.size - 3 &&
                    !viewModel.loading.value
                ) {
                    viewModel.loadMore(query)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 470.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2A2A2A).copy(alpha = 0.95f),
                        Color(0xFF1A1A1A).copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "SEARCH RESULTS",
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            Text(
                text = "Results: ${results.size}",
                color = Color.Gray.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                itemsIndexed(
                    items = results,
                    key = { index, song -> "${song.source}_${song.id}_${index}" }
                ) { _, song ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = LocalIndication.current,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                PlayerManager.play(
                                    context,
                                    results,
                                    results.indexOf(song)
                                )
                                onSongClick()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(song.albumArt),
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(text = song.title, color = Color.White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = song.artist, color = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                // 🔥 SOURCE BADGE
                                Text(
                                    text = song.source.uppercase(),
                                    color = when (song.source) {
                                        "gaana" -> Color(0xFFFF6B35)
                                        else -> Color(0xFF1DB954)
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}