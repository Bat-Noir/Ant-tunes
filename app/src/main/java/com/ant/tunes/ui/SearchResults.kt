package com.ant.tunes.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
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

    // 🔥 FIX: no more State<>
    val results = viewModel.searchResults
    val isLoading by viewModel.loading
    val listState = rememberLazyListState()
    val context = LocalContext.current
    // 🔥 SEARCH TRIGGER
    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            viewModel.searchSongs(query)
        }
    }

    // 🔥 PAGINATION SCROLL LISTENER
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .distinctUntilChanged()
            .collectLatest { lastIndex ->

                if (
                    lastIndex != null &&
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

                // 🔥 SONG LIST
                itemsIndexed(
                    items = results,
                    key = { index, song -> "${song.id}_${index}" }
                ) { _, song ->

                    val imageUrl = song.image.lastOrNull()?.url
                    val artist = song.artists.primary.firstOrNull()?.name ?: "Unknown"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = LocalIndication.current,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {

                                val songsList = results.map {
                                    com.ant.tunes.data.Song(
                                        id = it.id,
                                        title = it.name,
                                        artist = it.artists.primary.firstOrNull()?.name ?: "Unknown",
                                        albumArt = it.image.lastOrNull()?.url ?: "",
                                        streamUrl = it.downloadUrl.lastOrNull()?.url ?: it.url
                                    )
                                }

                                val clickedIndex = results.indexOf(song)

                                PlayerManager.play(context, songsList, clickedIndex)

                                onSongClick()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Image(
                            painter = rememberAsyncImagePainter(imageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {

                            Text(
                                text = song.name,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = artist,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // 🔥 LOADING ITEM (CORRECT PLACE)
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