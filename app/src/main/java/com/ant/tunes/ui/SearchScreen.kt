package com.ant.tunes.ui

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.ant.tunes.R
import com.ant.tunes.data.Song
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.AntBlack
import com.ant.tunes.ui.theme.AntGlassBorder
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntSurface2
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

data class BrowseCard(val id: String, val title: String, val imageUrl: String)

// Global state to tell MainActivity when to hide the Bottom Nav
var IsSearchUIActive by mutableStateOf(false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: com.ant.tunes.viewmodel.PlayerViewModel = viewModel()) {
    val results = vm.combinedResults
    // 🟢 BUG FIX: Bind trending to the real top tracks from PlayerManager so it always shows
    val trendingSongs by PlayerManager.topTracks.collectAsState()

    val isLoading by vm.loading
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var query by remember { mutableStateOf("") }
    val accent = LocalAccentColor.current
    var selectedAlbum by remember { mutableStateOf<BrowseCard?>(null) }

    // 🟢 SEARCH FILTER STATE
    var selectedSourceFilter by remember { mutableStateOf("All") }
    val sourceFilters = listOf("All", "Saavn", "Gaana", "YouTube")

    // 🟢 FIXED: Removed 'remember' to fix the filter bug.
    // Now it recalculates instantly the millisecond new results arrive from the background API!
    val filteredResults = if (selectedSourceFilter == "All") {
        results
    } else {
        results.filter {
            when(selectedSourceFilter) {
                "YouTube" -> it.source.equals("youtube", ignoreCase = true)
                "Gaana"   -> it.source.equals("gaana", ignoreCase = true)
                "Saavn"   -> it.source.contains("saavn", ignoreCase = true)
                else      -> true
            }
        }
    }

    IsSearchUIActive = query.isNotEmpty()

    BackHandler(enabled = IsSearchUIActive || selectedAlbum != null) {
        if (selectedAlbum != null) {
            selectedAlbum = null
        } else {
            query = ""
            focusManager.clearFocus()
        }
    }

    // ── SEARCH HISTORY LOGIC ──
    val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
    var stealthMode by remember { mutableStateOf(prefs.getBoolean("stealth", false)) }
    var searchHistory by remember { mutableStateOf(prefs.getString("search_history", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == "stealth") stealthMode = sp.getBoolean("stealth", false)
            if (key == "search_history") searchHistory = sp.getString("search_history", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun saveSearch(q: String) {
        if (stealthMode || q.isBlank()) return
        val current = searchHistory.toMutableList()
        current.remove(q)
        current.add(0, q)
        val limited = current.take(10)
        prefs.edit().putString("search_history", limited.joinToString(",")).apply()
    }

    // ── REAL-TIME SEARCH DEBOUNCE ──
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            delay(300)
            vm.searchSongs(query)
        }
    }

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    // 🟢 FIXED: rememberUpdatedState ensures the scrolling logic always sees the live, updated list size!
    val currentFilteredSize by rememberUpdatedState(filteredResults.size)
    val currentQuery by rememberUpdatedState(query)

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collectLatest { lastIndex ->
                if (lastIndex != null && lastIndex >= currentFilteredSize - 3 && !isLoading && currentQuery.isNotEmpty()) {
                    vm.loadMore(currentQuery)
                }
            }
    }

    // 🟢 UX FIX: Instantly scroll to top when changing filters so the user doesn't get stuck at the bottom of a blank page
    LaunchedEffect(selectedSourceFilter) {
        listState.scrollToItem(0)
    }


    // ── PLAYLIST BOTTOM SHEET ──
    if (songToAddToPlaylist != null) {
        ModalBottomSheet(
            onDismissRequest = { songToAddToPlaylist = null },
            containerColor = Color(0xFF121212),
            dragHandle = { BottomSheetDefaults.DragHandle(color = AntText3) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Add to Playlist", style = MaterialTheme.typography.titleLarge, color = AntText)
                Spacer(Modifier.height(16.dp))
                if (globalPlaylists.isEmpty()) {
                    Text("No playlists yet. Create one in the Library tab!", color = AntText3)
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                        items(globalPlaylists) { playlist ->
                            val isAdded = playlist.tracks.any { it.id == songToAddToPlaylist?.id }
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !isAdded) {
                                    playlist.tracks.add(songToAddToPlaylist!!)
                                    Toast.makeText(context, "Added to ${playlist.name.value}", Toast.LENGTH_SHORT).show()
                                    songToAddToPlaylist = null
                                }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QueueMusic, null, tint = if (isAdded) AntText3 else accent)
                                Spacer(Modifier.width(12.dp))
                                Text(playlist.name.value, color = if (isAdded) AntText3 else AntText, modifier = Modifier.weight(1f))
                                if (isAdded) Icon(Icons.Default.Check, null, tint = AntText3)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (selectedAlbum != null) {
        AlbumDetailView(
            album = selectedAlbum!!,
            albumTracks = vm.albumTracks,
            isLoading = vm.isAlbumLoading.value,
            onBack = { selectedAlbum = null },
            onAddToPlaylist = { songToAddToPlaylist = it }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(56.dp))

            if (TargetPlaylistId != null) {
                val p = globalPlaylists.find { it.id == TargetPlaylistId }
                if (p != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.15f)).padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Adding to: ${p.name.value}", color = accent, style = MaterialTheme.typography.labelLarge)
                        TextButton(onClick = { TargetPlaylistId = null }, contentPadding = PaddingValues(0.dp)) {
                            Text("DONE", color = accent)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    TargetPlaylistId = null
                }
            }

            // ── SEARCH BAR ──
            Row(
                modifier = Modifier.fillMaxWidth().clip(CircleShape).background(AntSurface2).padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ic_search), null, tint = AntText3, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search songs, artists...", style = MaterialTheme.typography.labelLarge, color = AntText3) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = AntText, unfocusedTextColor = AntText, cursorColor = accent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (query.isNotEmpty()) { saveSearch(query) }
                        focusManager.clearFocus()
                    }),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        focusManager.clearFocus()
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_close), "Clear", tint = AntText3, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 🟢 FILTERS ROW (Shows up when typing)
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(sourceFilters) { filter ->
                        val isSelected = selectedSourceFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) accent.copy(alpha = 0.15f) else AntSurface1)
                                .border(1.dp, if (isSelected) accent.copy(alpha = 0.5f) else AntGlassBorder, RoundedCornerShape(20.dp))
                                .clickable { selectedSourceFilter = filter }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(filter, style = MaterialTheme.typography.labelMedium, color = if (isSelected) accent else AntText2)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // ── STATE RENDERING ──
            if (query.isEmpty()) {
                LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
                    // Show History if available
                    if (searchHistory.isNotEmpty()) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("RECENT SEARCHES", style = MaterialTheme.typography.labelLarge, color = AntText3)
                                TextButton(onClick = { prefs.edit().putString("search_history", "").apply() }, contentPadding = PaddingValues(0.dp)) {
                                    Text("CLEAR", style = MaterialTheme.typography.labelMedium, color = accent)
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(searchHistory) { hist ->
                                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(AntSurface1).clickable { query = hist; saveSearch(hist) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text(hist, style = MaterialTheme.typography.bodyMedium, color = AntText)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    if (trendingSongs.isNotEmpty()) {
                        item {
                            Text("TRENDING TRACKS", style = MaterialTheme.typography.labelLarge, color = AntText3, modifier = Modifier.padding(bottom = 12.dp))
                        }
                        itemsIndexed(items = trendingSongs.take(15)) { _, song ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(indication = LocalIndication.current, interactionSource = remember { MutableInteractionSource() }) {
                                    vm.playFreshTrack(context, song) // 🟢 Fetch fresh URL first!
                                    RequestFullScreenPlayer = true
                                }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(painter = rememberAsyncImagePainter(song.albumArt), contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)))
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = AntText2, maxLines = 1)
                                }
                                IconButton(onClick = {
                                    if (TargetPlaylistId != null) {
                                        val p = globalPlaylists.find { it.id == TargetPlaylistId }
                                        p?.let {
                                            if (!it.tracks.any { t -> t.id == song.id }) {
                                                it.tracks.add(song)
                                                Toast.makeText(context, "Added to ${it.name.value}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        songToAddToPlaylist = song
                                    }
                                }) {
                                    Icon(Icons.Default.Add, "Add to Playlist", tint = accent)
                                }
                            }
                            HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp)
                        }
                    }
                }
            } else {
                // 🟢 NEW: Proper Loading and Empty States!
                if (isLoading && filteredResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(40.dp))
                    }
                } else if (!isLoading && filteredResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No results found in ${selectedSourceFilter}", style = MaterialTheme.typography.labelLarge, color = AntText3)
                    }
                } else {
                    Text("${filteredResults.size} RESULTS", style = MaterialTheme.typography.labelMedium, color = AntText3, modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 160.dp)) {
                        itemsIndexed(items = filteredResults, key = { index, song -> "${song.source}_${song.id}_$index" }) { _, song ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(indication = LocalIndication.current, interactionSource = remember { MutableInteractionSource() }) {
                                    PlayerManager.play(context, filteredResults, filteredResults.indexOf(song))
                                    saveSearch(query)
                                    focusManager.clearFocus()
                                    RequestFullScreenPlayer = true
                                }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(painter = rememberAsyncImagePainter(song.albumArt), contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)))
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = AntText2, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.background(color = when (song.source) { "gaana" -> Color(0xFFFF6B35) "youtube" -> Color(0xFFFF0000) else -> Color(0xFF1DB954) }, shape = RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text(song.source.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White)
                                        }
                                    }
                                }
                                IconButton(onClick = {
                                    if (TargetPlaylistId != null) {
                                        val p = globalPlaylists.find { it.id == TargetPlaylistId }
                                        if (p != null) {
                                            if (!p.tracks.any { it.id == song.id }) {
                                                p.tracks.add(song)
                                                Toast.makeText(context, "Added to ${p.name.value}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Already in playlist", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        songToAddToPlaylist = song
                                    }
                                }) {
                                    Icon(Icons.Default.Add, "Add to Playlist", tint = accent)
                                }
                            }
                            HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp)
                        }

                        // Bottom loading indicator for pagination
                        if (isLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Kept untouched just in case it's used elsewhere (though hidden from UI now)
@Composable
fun AlbumDetailView(
    album: BrowseCard,
    albumTracks: List<Song>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    Column(modifier = Modifier.fillMaxSize().background(AntBlack)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            AsyncImage(model = album.imageUrl, contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, AntBlack), startY = 300f)))
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).statusBarsPadding().size(40.dp).background(AntSurface2, CircleShape).border(1.dp, AntGlassBorder, CircleShape).align(Alignment.TopStart)) {
                Icon(Icons.Default.ArrowBack, "Go Back", tint = AntText, modifier = Modifier.size(20.dp))
            }
            Text(album.title, style = MaterialTheme.typography.displayMedium, color = AntText, modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 16.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { if (albumTracks.isNotEmpty()) PlayerManager.play(context, albumTracks, 0) }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text("PLAY", style = MaterialTheme.typography.labelLarge, color = AntBlack)
                }
                OutlinedButton(onClick = { if (albumTracks.isNotEmpty()) { PlayerManager.play(context, albumTracks.shuffled(), 0) } }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, AntGlassBorder)) {
                    Text("SHUFFLE", style = MaterialTheme.typography.labelLarge, color = AntText)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("TRACKS", style = MaterialTheme.typography.labelMedium, color = AntText3)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
                    itemsIndexed(albumTracks) { index, song ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { PlayerManager.play(context, albumTracks, index) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = AntText3, modifier = Modifier.width(30.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1)
                                Text(song.artist, style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1)
                            }
                            IconButton(onClick = {
                                if (TargetPlaylistId != null) {
                                    val p = globalPlaylists.find { it.id == TargetPlaylistId }
                                    if (p != null) {
                                        if (!p.tracks.any { it.id == song.id }) {
                                            p.tracks.add(song)
                                            Toast.makeText(context, "Added to ${p.name.value}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    onAddToPlaylist(song)
                                }
                            }) { Icon(Icons.Default.Add, "Add to Playlist", tint = accent) }
                        }
                        HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
