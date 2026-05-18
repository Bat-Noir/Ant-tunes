package com.ant.tunes.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ant.tunes.NewPipeHelper
import com.ant.tunes.data.Song
import com.ant.tunes.lastfm.LastFmAuthManager
import com.ant.tunes.lastfm.LastFmRepository
import com.ant.tunes.network.RetrofitClient
import com.ant.tunes.player.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LrcLine(val timeMs: Long, val text: String)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext


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
    // 🟢 NEW: LYRICS STATES
    val currentLyrics = mutableStateOf<String?>(null)
    val isLyricsLoading = mutableStateOf(false)

    // 🟢 ADDED: Timed Lyrics State
    val currentLrcLines = mutableStateOf<List<LrcLine>>(emptyList())

    data class LrcLine(val timeMs: Long, val text: String)

    // 🟢 NEW: Last.fm states
    val lastFmTopTracks = MutableStateFlow<List<Song>>(emptyList())
    val lastFmRecentTracks = MutableStateFlow<List<Song>>(emptyList())
    val isLastFmConnected = MutableStateFlow(false)


    // 🟢 THE ANTI-JUNK BOUNCER
    private fun isJunk(title: String, seedSong: Song): Boolean {
        val junkRegex = Regex("(?i)(slowed|reverb|8d|cover|karaoke|instrumental|mashup|remix\\b(?!official)|bass\\s*boosted|ringtone|shorts)")
        if (junkRegex.containsMatchIn(title)) return true

        val liveRegex = Regex("(?i)(live|concert|performance)")
        val seedIsLive = liveRegex.containsMatchIn(seedSong.title)
        if (!seedIsLive && liveRegex.containsMatchIn(title)) return true

        return false
    }

    // 🟢 DUAL-SOURCE LYRICS FETCHER
    private fun fetchLyrics(song: Song?) {
        if (song == null) {
            currentLyrics.value = null
            currentLrcLines.value = emptyList()
            return
        }
        val cleanTitle = song.title
            .replace(Regex("\\[.*?\\]|\\(.*?\\)"), "").trim()
        val cleanArtist = song.artist.split(",")
            .firstOrNull()?.trim() ?: song.artist

        viewModelScope.launch(Dispatchers.IO) {
            isLyricsLoading.value = true
            currentLyrics.value = null
            currentLrcLines.value = emptyList()

            // Try LRCLIB for synced lyrics first
            try {
                val urlStr = "https://lrclib.net/api/get?track_name=${
                    java.net.URLEncoder.encode(cleanTitle, "UTF-8")
                }&artist_name=${
                    java.net.URLEncoder.encode(cleanArtist, "UTF-8")
                }"
                val conn = java.net.URL(urlStr).openConnection()
                        as java.net.HttpURLConnection
                conn.setRequestProperty("User-Agent", "AntTunes/1.0")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(resp)

                    // Try synced (LRC) first
                    val syncedLyrics = json.optString("syncedLyrics", "")
                    if (syncedLyrics.isNotBlank()) {
                        val parsed = parseLrc(syncedLyrics)
                        withContext(Dispatchers.Main) {
                            currentLrcLines.value = parsed
                            currentLyrics.value = parsed.joinToString("\n") { it.text }
                            isLyricsLoading.value = false
                        }
                        return@launch
                    }

                    // Fallback to plain
                    val plain = json.optString("plainLyrics", "")
                    if (plain.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            currentLyrics.value = plain
                            isLyricsLoading.value = false
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }

            // Fallback to lyrics.ovh
            try {
                val url = "https://api.lyrics.ovh/v1/${
                    java.net.URLEncoder.encode(cleanArtist, "UTF-8")
                }/${
                    java.net.URLEncoder.encode(cleanTitle, "UTF-8")
                }"
                val conn = java.net.URL(url).openConnection()
                        as java.net.HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == 200) {
                    val json = org.json.JSONObject(
                        conn.inputStream.bufferedReader().readText()
                    )
                    val lyrics = json.optString("lyrics", "")
                    if (lyrics.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            currentLyrics.value = lyrics
                            isLyricsLoading.value = false
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }

            withContext(Dispatchers.Main) {
                currentLyrics.value = null
                isLyricsLoading.value = false
            }
        }
    }

    private suspend fun tryLrcLib(title: String, artist: String): String? {
        return try {
            val url = "https://lrclib.net/api/get?track_name=${
                java.net.URLEncoder.encode(title, "UTF-8")
            }&artist_name=${
                java.net.URLEncoder.encode(artist, "UTF-8")
            }"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "AntTunes/1.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode == 200) {
                val json = org.json.JSONObject(
                    conn.inputStream.bufferedReader().readText()
                )
                json.optString("plainLyrics", "").ifEmpty { null }
            } else null
        } catch (e: Exception) { null }
    }

    private suspend fun tryLyricsOvh(title: String, artist: String): String? {
        return try {
            val url = "https://api.lyrics.ovh/v1/${
                java.net.URLEncoder.encode(artist, "UTF-8")
            }/${
                java.net.URLEncoder.encode(title, "UTF-8")
            }"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode == 200) {
                val json = org.json.JSONObject(
                    conn.inputStream.bufferedReader().readText()
                )
                json.optString("lyrics", "").ifEmpty { null }
            } else null
        } catch (e: Exception) { null }
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

    // 🟢 PUBLIC FALLBACK — fetch trending from Last.fm public charts
    fun fetchPublicCharts() {
        viewModelScope.launch {
            try {
                // Last.fm top tracks chart — NO login needed
                val url = "https://ws.audioscrobbler.com/2.0/?method=chart.getTopTracks&api_key=e2427f83cfff636cb919ccdc4db1b4c1&format=json&limit=20"
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(response)
                    val tracks = json.getJSONObject("tracks").getJSONArray("track")
                    val publicSongs = mutableListOf<Song>()

                    for (i in 0 until minOf(tracks.length(), 20)) {
                        val t = tracks.getJSONObject(i)
                        val name = t.getString("name")
                        val artist = t.getJSONObject("artist").getString("name")
                        // resolve to audio
                        try {
                            val searchResp = RetrofitClient.api.searchSongs(
                                query = "$name $artist",
                                page = 1, limit = 2
                            )
                            val best = searchResp.body()?.data?.results?.firstOrNull()
                                ?: continue
                            val audioUrl = best.downloadUrl.lastOrNull()?.url ?: continue
                            publicSongs.add(
                                Song(
                                    id = best.id,
                                    title = name,
                                    artist = artist,
                                    albumArt = best.image.lastOrNull()?.url ?: "",
                                    streamUrl = audioUrl,
                                    source = "saavn"
                                )
                            )
                        } catch (e: Exception) { continue }
                    }
                    if (publicSongs.isNotEmpty()) {
                        _publicCharts.value = publicSongs
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private val _publicCharts = MutableStateFlow<List<Song>>(emptyList())
    val publicCharts: StateFlow<List<Song>> = _publicCharts

    fun fetchLastFmData() {
        viewModelScope.launch {
            isLastFmConnected.value = LastFmRepository.isLoggedIn(appContext)
            if (!isLastFmConnected.value) return@launch

            // Fetch top tracks
            val topTracks = LastFmRepository.getTopTracks(appContext)
            if (topTracks.isNotEmpty()) {
                val resolved = topTracks.take(15).mapNotNull { lastFmTrack ->
                    try {
                        val query = "${lastFmTrack.name} ${lastFmTrack.artist}"
                        val response = RetrofitClient.api.searchSongs(query = query, page = 1, limit = 3)
                        val best = response.body()?.data?.results?.firstOrNull() ?: return@mapNotNull null
                        val url = best.downloadUrl.lastOrNull()?.url ?: return@mapNotNull null

                        Song(
                            id = best.id,
                            title = lastFmTrack.name,
                            artist = lastFmTrack.artist,
                            albumArt = lastFmTrack.imageUrl.ifEmpty { best.image.lastOrNull()?.url ?: "" },
                            streamUrl = url,
                            source = "saavn"
                        )
                    } catch (e: Exception) { null }
                }
                lastFmTopTracks.value = resolved

                // 🔥 THE MISSING SPARK: Instantly generate the Discover row from their #1 Top Track!
                if (resolved.isNotEmpty()) {
                    loadLastFmRecommendations(resolved.first())
                }
            }


            // Fetch recently played
            val recent = LastFmRepository.getRecentTracks(appContext)
            if (recent.isNotEmpty()) {
                val resolved = recent.take(15).mapNotNull { track ->
                    try {
                        val query = "${track.name} ${track.artist}"
                        val response = RetrofitClient.api.searchSongs(query = query, page = 1, limit = 3)
                        val best = response.body()?.data?.results?.firstOrNull() ?: return@mapNotNull null
                        val url = best.downloadUrl.lastOrNull()?.url ?: return@mapNotNull null

                        Song(
                            id = best.id,
                            title = track.name,
                            artist = track.artist,
                            albumArt = track.imageUrl.ifEmpty { best.image.lastOrNull()?.url ?: "" },
                            streamUrl = url,
                            source = "saavn"
                        )
                    } catch (e: Exception) { null }
                }
                lastFmRecentTracks.value = resolved
            }
        }
    }

    // 🟢 Smart recommendations via Last.fm global similar tracks (NO LOGIN REQUIRED)
    fun loadLastFmRecommendations(currentSong: Song) {
        viewModelScope.launch {
            // 🔥 We removed the login check! It just uses your API key now.
            val similar = LastFmRepository.getSimilarTracks(track = currentSong.title, artist = currentSong.artist)
            if (similar.isEmpty()) {
                // If Last.fm fails to find similar songs, fallback to Saavn
                loadMoreRecommendations(currentSong)
                return@launch
            }

            val resolved = similar.mapNotNull { track ->
                try {
                    val query = "${track.name} ${track.artist}"
                    val response = RetrofitClient.api.searchSongs(query = query, page = 1, limit = 3)
                    val best = response.body()?.data?.results?.firstOrNull() ?: return@mapNotNull null
                    val url = best.downloadUrl.lastOrNull()?.url ?: return@mapNotNull null

                    Song(
                        id = best.id,
                        title = track.name,
                        artist = track.artist,
                        albumArt = best.image.lastOrNull()?.url ?: "",
                        streamUrl = url,
                        source = "saavn"
                    )
                } catch (e: Exception) { null }
            }

            if (resolved.isNotEmpty()) {
                val combined = (_recommendedSongs.value + resolved).distinctBy { it.id }
                _recommendedSongs.value = combined
                PlayerManager.addToQueue(resolved)
            }
        }
    }

    private fun parseLrc(lrc: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        // Regex to capture [mm:ss.xx] format
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
        lrc.lines().forEach { line ->
            val match = regex.find(line) ?: return@forEach
            val (min, sec, ms, text) = match.destructured
            val timeMs = (min.toLong() * 60 * 1000) +
                    (sec.toLong() * 1000) +
                    (ms.padEnd(3, '0').toLong())
            lines.add(LrcLine(timeMs, text.trim()))
        }
        return lines.sortedBy { it.timeMs }
    }

    init {
        // Load Last.fm private data ONLY if they bothered to log in
        if (LastFmAuthManager.isLoggedIn.value) {
            fetchLastFmData()
            fetchPublicCharts()
        }

        // 🔥 ALWAYS use Last.fm for the queue, logged in or not!
        PlayerManager.onNeedMoreSongs = { seedSong ->
            loadLastFmRecommendations(seedSong)
        }

        // 🟢 AUTO-FETCH LYRICS
        viewModelScope.launch {
            PlayerManager.currentSong.collect { song ->
                fetchLyrics(song)
            }
        }
    }
}
