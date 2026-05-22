package com.ant.tunes.ui

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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

    val localTop by PlayerManager.topTracks.collectAsState()
    val publicCharts by vm.publicCharts.collectAsState()
    val trendingSongs = localTop.ifEmpty { publicCharts }

    val isLoading by vm.loading

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var query by remember { mutableStateOf("") }
    val accent = LocalAccentColor.current
    var selectedAlbum by remember { mutableStateOf<BrowseCard?>(null) }
    var selectedArtist by remember { mutableStateOf<BrowseCard?>(null) }
    var selectedCategory by remember { mutableStateOf("Songs") }
    val categories = listOf("Songs", "Albums", "Artists")

    val albumResults = vm.albumSearchList
    val artistResults = vm.artistSearchList

    var selectedSourceFilter by remember { mutableStateOf("All") }
    val sourceFilters = listOf("All", "Saavn", "Gaana", "YouTube")

    val filteredResults = if (selectedSourceFilter == "All") {
        results
    } else {
        results.filter {
            when (selectedSourceFilter) {
                "YouTube" -> it.source.equals("youtube", ignoreCase = true)
                "Gaana" -> it.source.equals("gaana", ignoreCase = true)
                "Saavn" -> it.source.contains("saavn", ignoreCase = true)
                else -> true
            }
        }
    }

    IsSearchUIActive = query.isNotEmpty()

    BackHandler(enabled = IsSearchUIActive || selectedAlbum != null || selectedArtist != null) {
        if (selectedAlbum != null) {
            selectedAlbum = null
        } else if (selectedArtist != null) {
            selectedArtist = null
        } else {
            query = ""
            focusManager.clearFocus()
        }
    }

    val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
    var stealthMode by remember { mutableStateOf(prefs.getBoolean("stealth", false)) }
    var searchHistory by remember {
        mutableStateOf(
            prefs.getString("search_history", "")?.split(",")?.filter { it.isNotBlank() }
                ?: emptyList()
        )
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == "stealth") stealthMode = sp.getBoolean("stealth", false)
            if (key == "search_history") searchHistory =
                sp.getString("search_history", "")?.split(",")?.filter { it.isNotBlank() }
                    ?: emptyList()
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

    LaunchedEffect(query, selectedCategory) {
        if (query.isNotBlank()) {
            delay(300)
            when (selectedCategory) {
                "Songs" -> vm.searchSongs(query)
                "Albums" -> vm.searchAlbums(query)
                "Artists" -> vm.searchArtists(query)
            }
        }
    }

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

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

    LaunchedEffect(selectedSourceFilter) {
        listState.scrollToItem(0)
    }

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
                                    com.ant.tunes.player.AppDataManager.savePlaylists(context, globalPlaylists)
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

    // 🟢 ROUTING TO THE NEW FILES!
    if (selectedAlbum != null) {
        AlbumScreen(
            album = selectedAlbum!!,
            albumTracks = vm.albumTracks,
            isLoading = vm.isAlbumLoading.value,
            onBack = { selectedAlbum = null },
            onAddToPlaylist = { songToAddToPlaylist = it }
        )
    } else if (selectedArtist != null) {
        ArtistScreen(
            artist = selectedArtist!!,
            artistTracks = vm.albumTracks, // We will rename/split this backend state next
            isLoading = vm.isAlbumLoading.value,
            onBack = { selectedArtist = null },
            onAddToPlaylist = { songToAddToPlaylist = it }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(56.dp))

            if (TargetPlaylistId != null) {
                val p = globalPlaylists.find { it.id == TargetPlaylistId }
                if (p != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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

            Row(
                modifier = Modifier.fillMaxWidth().clip(CircleShape).background(AntSurface2)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = AntText,
                        unfocusedTextColor = AntText,
                        cursorColor = accent
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
                    IconButton(onClick = { query = ""; focusManager.clearFocus() }, modifier = Modifier.size(32.dp)) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_close), "Clear", tint = AntText3, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) accent else AntSurface1)
                            .border(1.dp, if (isSelected) accent else AntGlassBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(category, style = MaterialTheme.typography.labelMedium, color = if (isSelected) AntBlack else AntText2)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (query.isNotEmpty() && selectedCategory == "Songs") {
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
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (query.isEmpty()) {
                LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
                    if (searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("RECENT SEARCHES", style = MaterialTheme.typography.labelLarge, color = AntText3)
                                TextButton(onClick = { prefs.edit().putString("search_history", "").apply() }, contentPadding = PaddingValues(0.dp)) {
                                    Text("CLEAR", style = MaterialTheme.typography.labelMedium, color = accent)
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(searchHistory) { hist ->
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                            .background(AntSurface1)
                                            .clickable { query = hist; saveSearch(hist) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
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
                        // 🟢 FIXED: Changed '_' to 'index' so we can pass it to the player
                        itemsIndexed(items = trendingSongs.take(15)) { index, song ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(
                                    indication = LocalIndication.current,
                                    interactionSource = remember { MutableInteractionSource() }) {

                                    // 🟢 FIXED: Send the full list and exact index to update the metadata queue!
                                    PlayerManager.play(context, trendingSongs.take(15), index)
                                    RequestFullScreenPlayer = true

                                }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            Image(
                                    painter = rememberAsyncImagePainter(song.albumArt),
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp))
                                )
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
                // 🟢 UNIVERSAL LOADING & EMPTY STATES
                val isCurrentEmpty = when (selectedCategory) {
                    "Songs" -> filteredResults.isEmpty()
                    "Albums" -> albumResults.isEmpty()
                    "Artists" -> artistResults.isEmpty()
                    else -> true
                }

                if (isLoading && isCurrentEmpty) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(40.dp))
                    }
                } else if (!isLoading && isCurrentEmpty) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No results found in $selectedCategory", style = MaterialTheme.typography.labelLarge, color = AntText3)
                    }
                } else {
                    when (selectedCategory) {
                        "Albums" -> {
                            Text("${albumResults.size} ALBUMS", style = MaterialTheme.typography.labelMedium, color = AntText3, modifier = Modifier.padding(vertical = 8.dp))
                            AlbumGrid(albums = albumResults, onClick = {
                                selectedAlbum = it
                                vm.loadBrowseCategory(it.title) // 🟢 FIXED: Fetching by ID, no more duplicates!
                            })
                        }

                        "Artists" -> {
                            Text("${artistResults.size} ARTISTS", style = MaterialTheme.typography.labelMedium, color = AntText3, modifier = Modifier.padding(vertical = 8.dp))
                            ArtistGrid(artists = artistResults, onClick = {
                                selectedArtist = it
                                vm.loadBrowseCategory(it.title) // 🟢 FIXED: Fetching by ID, no more duplicates!
                            })
                        }

                        "Songs" -> {
                            Text("${filteredResults.size} SONGS", style = MaterialTheme.typography.labelMedium, color = AntText3, modifier = Modifier.padding(vertical = 8.dp))

                            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 160.dp)) {
                                // 🟢 FIXED: Changed '_' to 'index'
                                itemsIndexed(items = filteredResults, key = { index, song -> "${song.source}_${song.id}_$index" }) { index, song ->
                                    SwipeableSongRow(
                                        song = song,
                                        context = context,
                                        accent = accent,
                                        onSongClick = {
                                            // 🟢 FIXED: Use the exact layout index, guaranteeing a 1:1 metadata match
                                            PlayerManager.play(context, filteredResults, index)
                                            saveSearch(query)
                                            focusManager.clearFocus()
                                            RequestFullScreenPlayer = true
                                        },
                                        onAddToPlaylist = {
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
                                        }
                                    )
                                    HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp)
                                }

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
    }
}

// ── EXTRACTED COMPONENTS ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableSongRow(
    song: Song,
    context: android.content.Context,
    accent: Color,
    onSongClick: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!globalLikedSongs.any { it.id == song.id }) {
                        globalLikedSongs.add(song)
                        com.ant.tunes.player.AppDataManager.saveLikedSongs(context, globalLikedSongs)
                    }
                    android.widget.Toast.makeText(context, "❤️ Added to Liked Songs", android.widget.Toast.LENGTH_SHORT).show()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    PlayerManager.insertNext(song, context)
                    android.widget.Toast.makeText(context, "➕ Added to play next", android.widget.Toast.LENGTH_SHORT).show()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
                    else -> Arrangement.End
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFEC4899).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Favorite, null, tint = Color(0xFFEC4899), modifier = Modifier.size(22.dp))
                        }
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.QueueMusic, null, tint = accent, modifier = Modifier.size(22.dp))
                        }
                    }
                    else -> {}
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AntBlack)
                .clickable(indication = androidx.compose.foundation.LocalIndication.current, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                    onSongClick()
                }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(model = song.albumArt, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)))
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
            IconButton(onClick = onAddToPlaylist) {
                Icon(Icons.Default.Add, "Add to Playlist", tint = accent)
            }
        }
    }
}

@Composable
fun AlbumGrid(albums: List<BrowseCard>, onClick: (BrowseCard) -> Unit) {
    if (albums.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No albums found.", color = AntText3)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(bottom = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums) { album ->
            Column(modifier = Modifier.clickable { onClick(album) }) {
                AsyncImage(
                    model = album.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
                Text(album.title, color = AntText, maxLines = 1, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
fun ArtistGrid(artists: List<BrowseCard>, onClick: (BrowseCard) -> Unit) {
    if (artists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No artists found.", color = AntText3)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(bottom = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(artists) { artist ->
            Column(
                modifier = Modifier.clickable { onClick(artist) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = artist.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).border(1.dp, AntGlassBorder, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
                Text(artist.title, color = AntText, maxLines = 1, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            }
        }
    }
}
