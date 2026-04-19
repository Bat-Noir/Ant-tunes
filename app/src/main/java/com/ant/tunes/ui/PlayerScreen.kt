package com.ant.tunes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.ant.tunes.player.PlayerManager
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ant.tunes.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen() {

    var searchQuery by remember { mutableStateOf("") }
    var showLibrary by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    // 🔥 CONNECT PLAYER STATE
    val currentSong by PlayerManager.currentSong.collectAsState(initial = null)
    val isPlaying by PlayerManager.isPlayingFlow.collectAsState(initial = false)
    val viewModel: PlayerViewModel = viewModel()
    val recommendedSongs by viewModel.recommendedSongs.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        PlayerManager.loadLibrary(context)
    }

    DisposableEffect(Unit) {
        PlayerManager.startProgressUpdates()
        onDispose { }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 🔍 SEARCH BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        showSearch = it.isNotEmpty()
                    },
                    placeholder = { Text("Search for...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(30.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                showSearch = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showLibrary = true },
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 🎧 CENTER AREA (NOW LIVE 🔥)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {

                VinylArt(imageUrl = currentSong?.albumArt)

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = currentSong?.title ?: "Play Something",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = currentSong?.artist ?: "Lil Bro",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 🎚 SEEK BAR (will auto update after next step)
            SeekBar()

            Spacer(modifier = Modifier.height(6.dp))

            // 🎮 CONTROLS (LIVE 🔥)
            ControlBar(
                context = context, // 🔥 FIX
                onPrev = { PlayerManager.previous() },
                onPlayPause = { PlayerManager.togglePlayPause() },
                onNext = { PlayerManager.next() },
                onRepeat = {},
                isPlaying = isPlaying
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 📜 QUEUE
            QueuePanel()

            Spacer(modifier = Modifier.height(20.dp))
            if (recommendedSongs.isNotEmpty()) {

                Text(
                    text = "Recommended for you",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )

                LazyRow {
                    items(recommendedSongs) { song ->

                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                                .width(140.dp)
                                .clickable {
                                    PlayerManager.playStream(context, song)
                                }
                        ) {

                            AsyncImage(
                                model = song.albumArt,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = song.title,
                                maxLines = 1,
                                fontSize = 14.sp
                            )

                            Text(
                                text = song.artist,
                                maxLines = 1,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // 🔍 SEARCH RESULTS (FIXED CLOSE 🔥)
        if (showSearch) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 70.dp)
            ) {
                SearchResults(
                    query = searchQuery,
                    onSongClick = {
                        showSearch = false
                        searchQuery = ""   // 🔥 THIS FIX
                    }
                )
            }
        }

        // 📥 LIBRARY
        if (showLibrary) {
            LibraryOverlay(onClose = { showLibrary = false })
        }
    }
}