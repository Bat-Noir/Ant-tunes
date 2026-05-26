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
import com.ant.tunes.ui.globalFollowedArtists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private var currentScrobbledSongId: String? = null
    private var searchJob: Job? = null
    private var searchPage = 1
    private var searchHasMore = true
    private var recPage = 1

    // 🟢 Upgraded to a StateFlow so HomeScreen can track the loading spinner
    val isLoadingRecommendations = MutableStateFlow(false)
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

    // 🟢 ADDED: Timed Lyrics State
    val currentLrcLines = mutableStateOf<List<LrcLine>>(emptyList())

    data class LrcLine(val timeMs: Long, val text: String)

    // 🟢 NEW: Last.fm states
    val lastFmTopTracks = MutableStateFlow<List<Song>>(emptyList())
    val lastFmRecentTracks = MutableStateFlow<List<Song>>(emptyList())
    val isLastFmConnected = MutableStateFlow(false)

    // 🟢 Suggested Albums State
    val recommendedAlbums = MutableStateFlow<List<com.ant.tunes.ui.BrowseCard>>(emptyList())

    fun fetchSuggestedAlbums(artist: String) {
        viewModelScope.launch {
            // 🟢 FIX: Split by comma and take the first artist so Last.fm doesn't crash!
            val cleanArtist = artist.split(",").firstOrNull()?.trim() ?: artist
            val albums = LastFmRepository.getTopAlbums(cleanArtist)
            recommendedAlbums.value = albums
        }
    }

    // 🟢 THE ANTI-JUNK BOUNCER
    private fun isJunk(title: String, seedSong: Song): Boolean {
        val junkRegex =
            Regex("(?i)(slowed|reverb|8d|cover|karaoke|instrumental|mashup|remix\\b(?!official)|bass\\s*boosted|ringtone|shorts)")
        if (junkRegex.containsMatchIn(title)) return true

        val liveRegex = Regex("(?i)(live|concert|performance)")
        val seedIsLive = liveRegex.containsMatchIn(seedSong.title)
        if (!seedIsLive && liveRegex.containsMatchIn(title)) return true

        return false
    }
    // ═══════════════════════════════════════
    // 🟢 HOME SCREEN DYNAMIC ENGINE
    // ═══════════════════════════════════════

    val dynamicArtistName = mutableStateOf<String?>(null)
    val dynamicArtistTracks = mutableStateListOf<Song>()

    fun generateDynamicArtistRow(followedArtists: List<com.ant.tunes.ui.BrowseCard>) {
        // 🟢 CHANGED: Removed "|| dynamicArtistName.value != null"
        if (followedArtists.isEmpty()) return

        viewModelScope.launch {
            try {
                // 1. Pick a random artist from their library
                val randomArtist = followedArtists.random()
                dynamicArtistName.value = randomArtist.title

                // 2. Fetch tracks based on that artist
                val response =
                    RetrofitClient.api.searchSongs(query = randomArtist.title, page = 1, limit = 15)
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
                            album = it.album?.name ?: "",
                            source = "saavn"
                        )
                    }
                    dynamicArtistTracks.clear()
                    // Shuffle the results so it feels fresh every time!
                    dynamicArtistTracks.addAll(newSongs.shuffled().take(10))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 🟢 DUAL-SOURCE LYRICS FETCHER
    fun fetchLyrics(song: Song?) {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }

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
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                currentLyrics.value = null
                isLyricsLoading.value = false
            }
        }
    }

    // ── MATH HELPERS FOR LYRICS MATCHING ──
    // Simple similarity score 0.0-1.0
    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val longer = if (a.length > b.length) a else b
        val shorter = if (a.length > b.length) b else a
        if (longer.contains(shorter)) return shorter.length.toDouble() / longer.length
        // Levenshtein-based
        val editDist = levenshtein(a, b)
        return (longer.length - editDist).toDouble() / longer.length
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
        return dp[a.length][b.length]
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
        } catch (e: Exception) {
            null
        }
    }


    // 🟢 REVIVE DEAD LINKS (Edo Tensei)
    fun playFreshTrack(context: android.content.Context, song: Song) {
        viewModelScope.launch {
            try {
                // Instantly search the exact song to get a fresh, unexpired CDN link
                val response = RetrofitClient.api.searchSongs(
                    query = "${song.title} ${song.artist}",
                    page = 1,
                    limit = 1
                )
                val freshUrl =
                    response.body()?.data?.results?.firstOrNull()?.downloadUrl?.lastOrNull()?.url

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
            val response =
                RetrofitClient.api.searchSongs(query = query, page = searchPage, limit = 10)
            if (response.isSuccessful) {
                val apiSongs = response.body()?.data?.results ?: emptyList()

                // 🟢 RUN THROUGH iTUNES ENGINE IN PARALLEL
                val newSongs = kotlinx.coroutines.coroutineScope {
                    apiSongs.map { track ->
                        async {
                            val url = track.downloadUrl.lastOrNull()?.url ?: return@async null
                            val rawTitle = track.name
                            val rawArtist = track.artists.primary.firstOrNull()?.name ?: "Unknown"

                            // Hijack Saavn artwork with pristine iTunes metadata
                            val itunes = fetchiTunesData(rawTitle, rawArtist)

                            Song(
                                id = track.id,
                                title = rawTitle,
                                artist = rawArtist,
                                albumArt = itunes?.first ?: track.image.lastOrNull()?.url ?: "",
                                streamUrl = url,
                                duration = 0L,
                                album = itunes?.second ?: track.album?.name ?: "",
                                source = "saavn"
                            )
                        }
                    }.awaitAll().filterNotNull()
                }

                _results.addAll(newSongs)
                if (newSongs.isEmpty()) searchHasMore = false else searchPage++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

                                val streamDetail = RetrofitClient.gaanaApi.getStreamUrl(gaanaSong.seokey)
                                val streamBody = streamDetail.body()

                                val audioUrl = streamBody?.audio_url ?: body?.audio_url ?: return@async null

                                val rawTitle = body?.title ?: gaanaSong.title
                                val rawArtist = body?.artist ?: gaanaSong.subtitle

                                // 🟢 THE iTUNES UPGRADE FOR GAANA
                                val itunes = fetchiTunesData(rawTitle, rawArtist)

                                Song(
                                    id = gaanaSong.id,
                                    title = rawTitle,
                                    artist = rawArtist,
                                    albumArt = itunes?.first ?: body?.thumb ?: gaanaSong.thumb, // HD Cover injected!
                                    streamUrl = audioUrl,
                                    album = itunes?.second ?: body?.album_title ?: "",          // Real album injected!
                                    permanentUrl = gaanaSong.seokey,
                                    source = "gaana"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }
                _gaanaResults.addAll(mappedSongs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ═══════════════════════════════════════
    // 🟢 TASK 6: THE iTUNES METADATA ENGINE
    // ═══════════════════════════════════════
    private suspend fun fetchiTunesData(title: String, artist: String): Pair<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                // Clean garbage like (Official Video) so iTunes can match the real song
                val cleanTitle = title.replace(Regex("\\[.*?\\]|\\(.*?\\)|(?i)official|video|audio|lyric"), "").trim()
                val cleanArtist = artist.split(",").firstOrNull()?.trim() ?: artist

                val query = java.net.URLEncoder.encode("$cleanTitle $cleanArtist", "UTF-8")
                val urlStr = "https://itunes.apple.com/search?term=$query&entity=song&limit=1"

                val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000

                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(resp)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val track = results.getJSONObject(0)
                        val artwork100 = track.optString("artworkUrl100", "")
                        val albumName = track.optString("collectionName", "")

                        if (artwork100.isNotBlank()) {
                            // 🔥 THE HACK: Turn 100x100 into massive 1000x1000 HD art!
                            val hdArt = artwork100.replace("100x100bb.jpg", "1000x1000bb.jpg")
                            return@withContext Pair(hdArt, albumName)
                        }
                    }
                }
                null
            } catch (e: Exception) {
                null // If it fails, silently fail and use original art. No crashes!
            }
        }
    }

    // 🟢 FETCHES PRISTINE ALBUM ART FOR ALBUM SEARCH RESULTS
    private suspend fun fetchiTunesAlbumArt(albumName: String, artistName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val cleanAlbum = albumName.replace(Regex("\\[.*?\\]|\\(.*?\\)"), "").trim()
                val query = java.net.URLEncoder.encode("$cleanAlbum $artistName", "UTF-8")
                val urlStr = "https://itunes.apple.com/search?term=$query&entity=album&limit=1"

                val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2000

                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(resp)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val artwork100 = results.getJSONObject(0).optString("artworkUrl100", "")
                        if (artwork100.isNotBlank()) {
                            return@withContext artwork100.replace("100x100bb.jpg", "1000x1000bb.jpg")
                        }
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun fetchYouTube(query: String) {
        try {
            val results = NewPipeHelper.search(query)
            val mappedSongs = kotlinx.coroutines.coroutineScope {
                results.map { song ->
                    async {
                        try {
                            val audioUrl = NewPipeHelper.getAudioUrl(song.streamUrl) ?: return@async null

                            // 🟢 THE iTUNES UPGRADE FOR YOUTUBE
                            val itunes = fetchiTunesData(song.title, song.artist)

                            song.copy(
                                streamUrl = audioUrl,
                                source = "youtube",
                                albumArt = itunes?.first ?: song.albumArt, // HD Cover injected!
                                album = itunes?.second ?: song.album       // Real album injected!
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            _youtubeResults.addAll(mappedSongs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        if (isLoadingRecommendations.value) return // 🟢 FIXED: Added .value
        viewModelScope.launch {
            try {
                isLoadingRecommendations.value = true // 🟢 FIXED: Added .value
                val artistToSearch =
                    seedSong.artist.split(",").firstOrNull()?.trim() ?: seedSong.artist

                val response = RetrofitClient.api.searchSongs(
                    query = artistToSearch,
                    page = recPage,
                    limit = 30
                )

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
                isLoadingRecommendations.value = false // 🟢 FIXED: Added .value
            }
        }
    }

    // 🟢 PUBLIC FALLBACK — fetch trending from Last.fm public charts
    fun fetchPublicCharts() {
        viewModelScope.launch {
            try {
                // Last.fm top tracks chart — NO login needed
                val url =
                    "https://ws.audioscrobbler.com/2.0/?method=chart.getTopTracks&api_key=e2427f83cfff636cb919ccdc4db1b4c1&format=json&limit=20"
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
                        } catch (e: Exception) {
                            continue
                        }
                    }
                    if (publicSongs.isNotEmpty()) {
                        _publicCharts.value = publicSongs
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                        val response =
                            RetrofitClient.api.searchSongs(query = query, page = 1, limit = 3)
                        val best =
                            response.body()?.data?.results?.firstOrNull() ?: return@mapNotNull null
                        val url = best.downloadUrl.lastOrNull()?.url ?: return@mapNotNull null

                        Song(
                            id = best.id,
                            title = lastFmTrack.name,
                            artist = lastFmTrack.artist,
                            albumArt = lastFmTrack.imageUrl.ifEmpty {
                                best.image.lastOrNull()?.url ?: ""
                            },
                            streamUrl = url,
                            source = "saavn"
                        )
                    } catch (e: Exception) {
                        null
                    }
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
                        val response =
                            RetrofitClient.api.searchSongs(query = query, page = 1, limit = 3)
                        val best =
                            response.body()?.data?.results?.firstOrNull() ?: return@mapNotNull null
                        val url = best.downloadUrl.lastOrNull()?.url ?: return@mapNotNull null

                        Song(
                            id = best.id,
                            title = track.name,
                            artist = track.artist,
                            albumArt = track.imageUrl.ifEmpty {
                                best.image.lastOrNull()?.url ?: ""
                            },
                            streamUrl = url,
                            source = "saavn"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                lastFmRecentTracks.value = resolved
            }
        }
    }

    // 🟢 Smart recommendations via Last.fm global similar tracks (NO LOGIN REQUIRED)
    // 🟢 INFINITE AUTO-PILOT QUEUE ENGINE
    fun loadLastFmRecommendations(currentSong: Song) {
        if (isLoadingRecommendations.value) return

        viewModelScope.launch {
            try {
                isLoadingRecommendations.value = true

                // 🟢 FIX: Clean the artist name here too!
                val cleanArtist =
                    currentSong.artist.split(",").firstOrNull()?.trim() ?: currentSong.artist

                // 🔥 MAX POWER: Ask Last.fm using ONLY the primary artist
                val similar = LastFmRepository.getSimilarTracks(
                    track = currentSong.title,
                    artist = cleanArtist
                )

                if (similar.isEmpty()) {
// ... rest of the function remains exactly the same ...

                    loadMoreRecommendations(currentSong) // Fallback to Saavn
                    return@launch
                }

                // 🔥 PARALLEL RESOLUTION: Rapidly scan Saavn for the top 20 best AI matches
                val resolved = kotlinx.coroutines.coroutineScope {
                    similar.take(20).map { track ->
                        async(Dispatchers.IO) {
                            try {
                                // Filter out live/karaoke junk before searching
                                if (isJunk(track.name, currentSong)) return@async null

                                val query = "${track.name} ${track.artist}"
                                val response = RetrofitClient.api.searchSongs(
                                    query = query,
                                    page = 1,
                                    limit = 2
                                )
                                val best = response.body()?.data?.results?.firstOrNull()
                                    ?: return@async null
                                val url = best.downloadUrl.lastOrNull()?.url ?: return@async null

                                Song(
                                    id = best.id,
                                    title = track.name,
                                    artist = track.artist,
                                    albumArt = best.image.lastOrNull()?.url ?: "",
                                    streamUrl = url,
                                    source = "saavn"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                if (resolved.isNotEmpty()) {
                    // Inject these fresh 20 tracks into the UI row AND the background player queue!
                    val combined =
                        (resolved + _recommendedSongs.value).distinctBy { it.id }.take(50)
                    _recommendedSongs.value = combined
                    PlayerManager.addToQueue(resolved)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingRecommendations.value = false
            }
        }
    }

    // ═══════════════════════════════════════
    // 🟢 ALBUM & ARTIST SEARCH STATES
    // ═══════════════════════════════════════

    private val _albumResults = mutableStateListOf<com.ant.tunes.ui.BrowseCard>()
    val albumSearchList: List<com.ant.tunes.ui.BrowseCard> get() = _albumResults

    private val _artistResults = mutableStateListOf<com.ant.tunes.ui.BrowseCard>()
    val artistSearchList: List<com.ant.tunes.ui.BrowseCard> get() = _artistResults

    // ═══════════════════════════════════════
    // 🟢 TASK 7.6: THE RUTHLESS iTUNES ENGINE
    // ═══════════════════════════════════════

    // 🔥 THE BOUNCER: Kills karaoke, 8-bit, and tribute garbage instantly
    private fun isJunkText(text: String): Boolean {
        return Regex("(?i)(karaoke|tribute|8-bit|16-bit|cover|instrumental|mashup|emulation|lullaby|arcade|originally performed|piano)").containsMatchIn(text)
    }

    fun searchAlbums(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _albumResults.clear()
            try {
                val itunesCards = mutableListOf<com.ant.tunes.ui.BrowseCard>()
                withContext(Dispatchers.IO) {
                    val itunesQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val urlStr = "https://itunes.apple.com/search?term=$itunesQuery&entity=album&limit=15"
                    val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                    if (conn.responseCode == 200) {
                        val results = org.json.JSONObject(conn.inputStream.bufferedReader().readText()).optJSONArray("results")
                        if (results != null) {
                            for (i in 0 until results.length()) {
                                val item = results.getJSONObject(i)
                                if (item.optString("wrapperType") == "collection") {
                                    val albumName = item.optString("collectionName")
                                    val artistName = item.optString("artistName")

                                    // 🟢 RUTHLESS FILTER
                                    if (isJunkText(albumName) || isJunkText(artistName)) continue

                                    itunesCards.add(
                                        com.ant.tunes.ui.BrowseCard(
                                            id = "itunes_album_${item.optLong("collectionId")}",
                                            title = albumName,
                                            imageUrl = item.optString("artworkUrl100", "").replace("100x100bb.jpg", "1000x1000bb.jpg")
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                val response = RetrofitClient.api.searchAlbums(query = query, page = 1)
                val saavnCards = response.body()?.data?.results?.map {
                    com.ant.tunes.ui.BrowseCard(id = it.id, title = it.name ?: "Unknown", imageUrl = it.image.lastOrNull()?.url ?: "")
                } ?: emptyList()

                val combined = (itunesCards + saavnCards).distinctBy { it.title.lowercase() }
                _albumResults.addAll(combined)
            } catch (e: Exception) { e.printStackTrace() }
            finally { _isSearching.value = false }
        }
    }

    fun searchArtists(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _artistResults.clear()
            try {
                val itunesCards = mutableListOf<com.ant.tunes.ui.BrowseCard>()
                withContext(Dispatchers.IO) {
                    val itunesQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    // 🟢 STRICT ENTITY: ONLY search verified Apple Music Artists!
                    val urlStr = "https://itunes.apple.com/search?term=$itunesQuery&entity=musicArtist&limit=5"
                    val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                    if (conn.responseCode == 200) {
                        val results = org.json.JSONObject(conn.inputStream.bufferedReader().readText()).optJSONArray("results")
                        if (results != null) {
                            for (i in 0 until results.length()) {
                                val item = results.getJSONObject(i)
                                val artistId = item.optLong("artistId")
                                val artistName = item.optString("artistName")

                                // 🟢 RUTHLESS FILTER
                                if (isJunkText(artistName)) continue

                                // 🟢 ARTWORK STEALER: Ping their top song just for the 4K cover
                                try {
                                    val artUrl = "https://itunes.apple.com/lookup?id=$artistId&entity=song&limit=1"
                                    val artConn = java.net.URL(artUrl).openConnection() as java.net.HttpURLConnection
                                    if (artConn.responseCode == 200) {
                                        val artResults = org.json.JSONObject(artConn.inputStream.bufferedReader().readText()).optJSONArray("results")
                                        if (artResults != null && artResults.length() > 1) { // 0 is artist, 1 is top song
                                            val songItem = artResults.getJSONObject(1)
                                            val image = songItem.optString("artworkUrl100", "").replace("100x100bb.jpg", "1000x1000bb.jpg")
                                            itunesCards.add(
                                                com.ant.tunes.ui.BrowseCard(
                                                    id = "itunes_artist_$artistId",
                                                    title = artistName,
                                                    imageUrl = image
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }

                val response = RetrofitClient.api.searchArtists(query = query, page = 1)
                val saavnCards = response.body()?.data?.results?.map {
                    com.ant.tunes.ui.BrowseCard(id = it.id, title = it.name ?: "Unknown", imageUrl = it.image.lastOrNull()?.url ?: "")
                } ?: emptyList()

                val combined = (itunesCards + saavnCards).distinctBy { it.title.lowercase() }
                _artistResults.addAll(combined)
            } catch (e: Exception) { e.printStackTrace() }
            finally { _isSearching.value = false }
        }
    }

    fun loadAlbumById(albumId: String) {
        viewModelScope.launch {
            try {
                isAlbumLoading.value = true
                _albumTracks.clear()

                if (albumId.startsWith("itunes_album_")) {
                    val realId = albumId.replace("itunes_album_", "")
                    withContext(Dispatchers.IO) {
                        val urlStr = "https://itunes.apple.com/lookup?id=$realId&entity=song"
                        val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                        if (conn.responseCode == 200) {
                            val results = org.json.JSONObject(conn.inputStream.bufferedReader().readText()).optJSONArray("results")
                            if (results != null) {
                                val ghostTracks = mutableListOf<Song>()
                                for (i in 0 until results.length()) {
                                    val item = results.getJSONObject(i)
                                    val trackName = item.optString("trackName")

                                    // 🟢 FILTER BY TRACK AND KILL JUNK
                                    if (item.optString("wrapperType") == "track" && !isJunkText(trackName)) {
                                        ghostTracks.add(
                                            Song(
                                                id = "itunes_${item.optLong("trackId")}",
                                                title = trackName,
                                                artist = item.optString("artistName"),
                                                albumArt = item.optString("artworkUrl100", "").replace("100x100bb.jpg", "1000x1000bb.jpg"),
                                                streamUrl = "ghost_track",
                                                album = item.optString("collectionName"),
                                                source = "youtube"
                                            )
                                        )
                                    }
                                }
                                withContext(Dispatchers.Main) { _albumTracks.addAll(ghostTracks) }
                            }
                        }
                    }
                    return@launch
                }

                // Saavn Fallback
                val response = RetrofitClient.api.getAlbumDetails(id = albumId)
                if (response.isSuccessful) {
                    val apiSongs = response.body()?.data?.songs ?: emptyList()
                    val newSongs = apiSongs.mapNotNull {
                        val url = it.downloadUrl.lastOrNull()?.url ?: return@mapNotNull null
                        Song(
                            id = it.id, title = it.name, artist = it.artists.primary.firstOrNull()?.name ?: "Unknown",
                            albumArt = it.image.lastOrNull()?.url ?: "", streamUrl = url, album = it.album?.name ?: "", source = "saavn"
                        )
                    }
                    _albumTracks.addAll(newSongs)
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { isAlbumLoading.value = false }
        }
    }

    fun loadArtistById(artistId: String) {
        viewModelScope.launch {
            try {
                isAlbumLoading.value = true
                _albumTracks.clear()

                if (artistId.startsWith("itunes_artist_")) {
                    val realId = artistId.replace("itunes_artist_", "")
                    withContext(Dispatchers.IO) {
                        val urlStr = "https://itunes.apple.com/lookup?id=$realId&entity=song&limit=30"
                        val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                        if (conn.responseCode == 200) {
                            val results = org.json.JSONObject(conn.inputStream.bufferedReader().readText()).optJSONArray("results")
                            if (results != null) {
                                val ghostTracks = mutableListOf<Song>()
                                for (i in 0 until results.length()) {
                                    val item = results.getJSONObject(i)
                                    val trackName = item.optString("trackName")

                                    // 🟢 FILTER BY TRACK AND KILL JUNK
                                    if (item.optString("wrapperType") == "track" && !isJunkText(trackName)) {
                                        ghostTracks.add(
                                            Song(
                                                id = "itunes_${item.optLong("trackId")}",
                                                title = trackName,
                                                artist = item.optString("artistName"),
                                                albumArt = item.optString("artworkUrl100", "").replace("100x100bb.jpg", "1000x1000bb.jpg"),
                                                streamUrl = "ghost_track",
                                                album = item.optString("collectionName"),
                                                source = "youtube"
                                            )
                                        )
                                    }
                                }
                                withContext(Dispatchers.Main) { _albumTracks.addAll(ghostTracks) }
                            }
                        }
                    }
                    return@launch
                }

                // Saavn Fallback
                val response = RetrofitClient.api.getArtistDetails(id = artistId)
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    val apiSongs = data?.topSongs ?: data?.songs ?: emptyList()
                    val newSongs = apiSongs.mapNotNull {
                        val url = it.downloadUrl.lastOrNull()?.url ?: return@mapNotNull null
                        Song(
                            id = it.id, title = it.name, artist = it.artists.primary.firstOrNull()?.name ?: "Unknown",
                            albumArt = it.image.lastOrNull()?.url ?: "", streamUrl = url, album = it.album?.name ?: "", source = "saavn"
                        )
                    }
                    _albumTracks.addAll(newSongs)
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { isAlbumLoading.value = false }
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
        // 🟢 1. RE-VIVE DYNAMIC ARTIST: Delay slightly to ensure global data is loaded, then trigger
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            if (globalFollowedArtists.isNotEmpty()) {
                generateDynamicArtistRow(globalFollowedArtists)
            }
        }

        // ═══════════════════════════════════════
        // 🟢 THE LAST.FM SCROBBLER & TELEMETRY TRACKER
        // ═══════════════════════════════════════
        viewModelScope.launch {
            PlayerManager.currentPosition.collect { position ->
                val song = PlayerManager.currentSong.value ?: return@collect
                val duration = PlayerManager.duration.value

                // 🔥 THE RULE: If song plays for 30 seconds OR reaches 50%, it counts as a real play!
                if (position > 30_000 || (duration > 0 && position > duration / 2)) {
                    if (currentScrobbledSongId != song.id) {
                        currentScrobbledSongId = song.id

                        // 1. Log to our Local Telemetry
                        com.ant.tunes.player.AppDataManager.incrementPlayCount(appContext, song)

                        // 🟢 ADDED THIS: Instantly refresh the UI leaderboard!
                        PlayerManager.refreshTopTracks(appContext)


                        // 2. Silently update Last.fm to train the algorithm!
                        if (isLastFmConnected.value) {
                            try {
                                LastFmRepository.scrobbleTrack(appContext, song.title, song.artist)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }

        // Reset the tracker when the song changes so the next song can be counted
        viewModelScope.launch {
            PlayerManager.currentSong.collect { song ->
                if (song?.id != currentScrobbledSongId) {
                    currentScrobbledSongId = null
                }
            }
        }

        // 🟢 2. ALWAYS load trending data immediately for all users
        fetchPublicCharts()

        // 🟢 3. ONLY load private data if they logged into Last.fm
        if (LastFmAuthManager.isLoggedIn.value) {
            fetchLastFmData()
        }

        // 🔥 ALWAYS use Last.fm for the queue, logged in or not!
        PlayerManager.onNeedMoreSongs = { seedSong ->
            loadLastFmRecommendations(seedSong)
        }

        // 🟢 4. Auto-fetch Lyrics, Albums AND initial Recommendations
        viewModelScope.launch {
            PlayerManager.currentSong.collect { song ->
                if (song != null) {
                    fetchLyrics(song)

                    // Fetch albums for the primary artist
                    val cleanArtist = song.artist.split(",").firstOrNull()?.trim() ?: song.artist
                    fetchSuggestedAlbums(cleanArtist)

                    // 🟢 THE NEW USER FIX: If "Made For You" is empty, generate it immediately!
                    if (_recommendedSongs.value.isEmpty()) {
                        loadLastFmRecommendations(song)
                    }
                }
            }
        }
    }
}