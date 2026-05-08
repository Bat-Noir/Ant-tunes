package com.ant.tunes.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ant.tunes.data.Song
import com.ant.tunes.network.RetrofitClient
import com.ant.tunes.player.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.ant.tunes.NewPipeHelper

class PlayerViewModel : ViewModel() {

    private var searchJob: Job? = null

    private var searchPage = 1
    private var searchHasMore = true

    private var recPage = 1
    private var recHasMore = true
    private var isLoadingRecommendations = false

    private val _isSearching = mutableStateOf(false)
    val loading: State<Boolean> = _isSearching

    private val _results = mutableStateListOf<Song>()
    val searchResults: List<Song> get() = _results

    private val _gaanaResults = mutableStateListOf<Song>()
    val gaanaResults: List<Song> get() = _gaanaResults

    private val _combinedResults = mutableStateListOf<Song>()
    val combinedResults: List<Song> get() = _combinedResults

    private val _youtubeResults = mutableStateListOf<Song>()
    val youtubeResults: List<Song> get() = _youtubeResults

    private val _recommendedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recommendedSongs: StateFlow<List<Song>> = _recommendedSongs

    private val _albumTracks = mutableStateListOf<Song>()
    val albumTracks: List<Song> get() = _albumTracks

    val isAlbumLoading = mutableStateOf(false)
    val isPlayerExpanded = mutableStateOf(false)

    // 🟢 NEW: LYRICS STATES
    val currentLyrics = mutableStateOf<String?>(null)
    val isLyricsLoading = mutableStateOf(false)

    // 🟢 THE ANTI-JUNK BOUNCER
    private fun isJunk(title: String, seedSong: Song): Boolean {
        val junkRegex = Regex("(?i)(slowed|reverb|8d|cover|karaoke|instrumental|mashup|remix\\b(?!official)|bass\\s*boosted|ringtone|shorts)")
        if (junkRegex.containsMatchIn(title)) return true

        val liveRegex = Regex("(?i)(live|concert|performance)")
        val seedIsLive = liveRegex.containsMatchIn(seedSong.title)
        if (!seedIsLive && liveRegex.containsMatchIn(title)) return true

        return false
    }

    // 🟢 LRCLIB UNIVERSAL LYRICS FETCHER
    private fun fetchLyrics(song: Song?) {
        if (song == null) {
            currentLyrics.value = null
            return
        }

        // Clean up title/artist so API finds matches easier (removes brackets like [Official Music Video])
        val cleanTitle = song.title.replace(Regex("\\[.*?\\]|\\(.*?\\)"), "").trim()
        val cleanArtist = song.artist.split(",").firstOrNull()?.trim() ?: song.artist

        viewModelScope.launch(Dispatchers.IO) {
            try {
                isLyricsLoading.value = true
                currentLyrics.value = null

                val urlStr = "https://lrclib.net/api/get?track_name=${URLEncoder.encode(cleanTitle, "UTF-8")}&artist_name=${URLEncoder.encode(cleanArtist, "UTF-8")}"
                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "AntTunes/1.0 (Android)")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val plainLyrics = json.optString("plainLyrics", "")

                    withContext(Dispatchers.Main) {
                        if (plainLyrics.isNotBlank()) {
                            currentLyrics.value = plainLyrics
                        } else {
                            currentLyrics.value = "No lyrics found for this track."
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        currentLyrics.value = "No lyrics found for this track."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    currentLyrics.value = "Error fetching lyrics. Check connection."
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLyricsLoading.value = false
                }
            }
        }
    }

    // 🟢 REVIVE DEAD LINKS (Edo Tensei)
    fun playFreshTrack(context: android.content.Context, song: Song) {
        viewModelScope.launch {
            try {
                // Instantly search the exact song to get a fresh, unexpired CDN link
                val response = RetrofitClient.api.searchSongs(query = "${song.title} ${song.artist}", page = 1, limit = 1)
                val freshUrl = response.body()?.data?.results?.firstOrNull()?.downloadUrl?.lastOrNull()?.url

                if (freshUrl != null) {
                    val freshSong = song.copy(streamUrl = freshUrl, source = "saavn")
                    PlayerManager.playStream(context, freshSong)
                } else {
                    // Fallback just in case
                    PlayerManager.playStream(context, song)
                }
            } catch (e: Exception) {
                PlayerManager.playStream(context, song)
            }
        }
    }


    fun loadBrowseCategory(categoryName: String) {
        viewModelScope.launch {
            try {
                isAlbumLoading.value = true
                _albumTracks.clear()

                val response = RetrofitClient.api.searchSongs(
                    query = categoryName,
                    page = 1,
                    limit = 30
                )

                if (response.isSuccessful) {
                    val apiSongs = response.body()?.data?.results ?: emptyList()
                    val newSongs = apiSongs.mapNotNull {
                        val url = it.downloadUrl.lastOrNull()?.url ?: return@mapNotNull null
                        Song(
                            id = it.id,
                            title = it.name,
                            artist = it.artists.primary.firstOrNull()?.name ?: "Unknown",
                            albumArt = it.image.lastOrNull()?.url ?: "",
                            streamUrl = url,
                            duration = 0L,
                            album = it.album?.name ?: categoryName, // 🔥 Catch the real album
                            source = "saavn"
                        )
                    }
                    _albumTracks.addAll(newSongs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isAlbumLoading.value = false
            }
        }
    }

    private fun rebuildCombinedResults() {
        _combinedResults.clear()
        val max = maxOf(_results.size, _gaanaResults.size, _youtubeResults.size)
        for (i in 0 until max) {
            if (i < _results.size) _combinedResults.add(_results[i])
            if (i < _gaanaResults.size) _combinedResults.add(_gaanaResults[i])
            if (i < _youtubeResults.size) _combinedResults.add(_youtubeResults[i])
        }
    }

    // ═══════════════════════════════════════
    // 🟢 MASTER SEARCH SYNC LOGIC
    // ═══════════════════════════════════════

    fun searchSongs(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true // Master loading state ON

            searchPage = 1
            searchHasMore = true
            _results.clear()
            _gaanaResults.clear()
            _youtubeResults.clear()
            _combinedResults.clear()

            // Run all 3 concurrent fetches
            val saavnJob = async { fetchSaavnPage(query) }
            val gaanaJob = async { fetchGaana(query) }
            val ytJob = async { fetchYouTube(query) }

            // Wait for all to finish
            awaitAll(saavnJob, gaanaJob, ytJob)

            rebuildCombinedResults()

            _isSearching.value = false // Master loading state OFF
        }
    }

    private suspend fun fetchSaavnPage(query: String) {
        if (!searchHasMore) return
        try {
            val response = RetrofitClient.api.searchSongs(query = query, page = searchPage, limit = 10)
            if (response.isSuccessful) {
                val apiSongs = response.body()?.data?.results ?: emptyList()
                val newSongs = apiSongs.mapNotNull {
                    val url = it.downloadUrl.lastOrNull()?.url ?: return@mapNotNull null
                    Song(
                        id = it.id,
                        title = it.name,
                        artist = it.artists.primary.firstOrNull()?.name ?: "Unknown",
                        albumArt = it.image.lastOrNull()?.url ?: "",
                        streamUrl = url,
                        duration = 0L,
                        album = it.album?.name ?: "", // 🔥 Catch the real Saavn album!
                        source = "saavn"
                    )
                }
                _results.addAll(newSongs)
                if (newSongs.isEmpty()) searchHasMore = false else searchPage++
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun fetchGaana(query: String) {
        try {
            val response = RetrofitClient.gaanaApi.searchSongs(query = query, type = "song", limit = 20)
            if (response.isSuccessful) {
                val songs = response.body()?.results ?: emptyList()
                val mappedSongs = kotlinx.coroutines.coroutineScope {
                    songs.map { gaanaSong ->
                        async {
                            try {
                                val songDetail = RetrofitClient.gaanaApi.getSong(gaanaSong.seokey)
                                val body = songDetail.body()
                                val audioUrl = body?.audio_url ?: return@async null
                                if (body.status != true) return@async null

                                Song(
                                    id = gaanaSong.id,
                                    title = body.title ?: gaanaSong.title,
                                    artist = body.artist ?: gaanaSong.subtitle,
                                    albumArt = body.thumb ?: gaanaSong.thumb,
                                    streamUrl = audioUrl,
                                    album = body.album_title ?: "", // 🔥 Catch the real Gaana album!
                                    source = "gaana"
                                )
                            } catch (e: Exception) { null }
                        }
                    }.awaitAll().filterNotNull().filter { song ->
                        song.title.contains(query, ignoreCase = true) || song.artist.contains(query, ignoreCase = true)
                    }
                }
                _gaanaResults.addAll(mappedSongs)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun fetchYouTube(query: String) {
        try {
            val results = NewPipeHelper.search(query)
            val mappedSongs = kotlinx.coroutines.coroutineScope {
                results.map { song ->
                    async {
                        try {
                            val audioUrl = NewPipeHelper.getAudioUrl(song.streamUrl) ?: return@async null
                            song.copy(streamUrl = audioUrl, source = "youtube")
                        } catch (e: Exception) { null }
                    }
                }.awaitAll().filterNotNull()
            }
            _youtubeResults.addAll(mappedSongs)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Upgraded infinite pagination that doesn't break the UI
    fun loadMore(query: String) {
        if (_isSearching.value || !searchHasMore) return
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            fetchSaavnPage(query)
            rebuildCombinedResults()
            _isSearching.value = false
        }
    }

    // ═══════════════════════════════════════

    // 🟢 DYNAMIC RECOMMENDATION QUEUE ENGINE
    fun loadMoreRecommendations(seedSong: Song) {
        if (isLoadingRecommendations) return
        viewModelScope.launch {
            try {
                isLoadingRecommendations = true
                val artistToSearch = seedSong.artist.split(",").firstOrNull()?.trim() ?: seedSong.artist

                val response = RetrofitClient.api.searchSongs(query = artistToSearch, page = recPage, limit = 30)

                if (response.isSuccessful) {
                    val results = response.body()?.data?.results ?: emptyList()
                    val newSongs = results
                        .mapNotNull { it ->
                            val url = it.downloadUrl.lastOrNull()?.url ?: return@mapNotNull null
                            Song(
                                id = it.id,
                                title = it.name,
                                artist = it.artists.primary.firstOrNull()?.name ?: "Unknown",
                                albumArt = it.image.lastOrNull()?.url ?: "",
                                streamUrl = url,
                                album = it.album?.name ?: "", // 🔥 Catch the real Saavn album!
                                source = "saavn"
                            )
                        }
                        .filter { !isJunk(it.title, seedSong) }

                    if (newSongs.isNotEmpty()) {
                        val combined = (_recommendedSongs.value + newSongs).distinctBy { it.id }
                        _recommendedSongs.value = combined
                        PlayerManager.addToQueue(newSongs)
                        recPage++
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingRecommendations = false
            }
        }
    }

    init {
        PlayerManager.onNeedMoreSongs = { seedSong ->
            loadMoreRecommendations(seedSong)
        }

        // 🟢 AUTO-FETCH LYRICS: Observes song changes and fetches lyrics silently
        viewModelScope.launch {
            PlayerManager.currentSong.collect { song ->
                fetchLyrics(song)
            }
        }
    }
}
