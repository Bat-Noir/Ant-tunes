package com.ant.tunes.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ant.tunes.data.Song
import com.ant.tunes.network.RetrofitClient
import com.ant.tunes.player.PlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private fun rebuildCombinedResults() {
        _combinedResults.clear()
        val max = maxOf(_results.size, _gaanaResults.size, _youtubeResults.size)
        for (i in 0 until max) {
            if (i < _results.size) _combinedResults.add(_results[i])
            if (i < _gaanaResults.size) _combinedResults.add(_gaanaResults[i])
            if (i < _youtubeResults.size) _combinedResults.add(_youtubeResults[i])
        }
    }

    fun searchSongs(query: String) {
        searchJob?.cancel()
        searchPage = 1
        searchHasMore = true
        _results.clear()
        _gaanaResults.clear()
        _combinedResults.clear()
        loadMore(query)
        searchGaana(query)
        searchYouTube(query)
    }

    fun searchGaana(query: String) {
        _gaanaResults.clear()
        viewModelScope.launch {
            try {
                val response = RetrofitClient.gaanaApi.searchSongs(
                    query = query,
                    type = "song",
                    limit = 20
                )
                if (response.isSuccessful) {
                    val songs = response.body()?.results ?: emptyList()
                    val mappedSongs = kotlinx.coroutines.coroutineScope {
                        songs.map { gaanaSong ->
                            async {
                                try {
                                    val songDetail = RetrofitClient.gaanaApi
                                        .getSong(gaanaSong.seokey)
                                    val body = songDetail.body()
                                    val audioUrl = body?.audio_url
                                        ?: return@async null
                                    if (body.status != true) return@async null

                                    Song(
                                        id = gaanaSong.id,
                                        title = body.title ?: gaanaSong.title,
                                        artist = body.artist ?: gaanaSong.subtitle,
                                        albumArt = body.thumb ?: gaanaSong.thumb,
                                        streamUrl = audioUrl,
                                        album = "Gaana",
                                        source = "gaana"
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()

                            .filter { song ->
                                song.title.contains(query, ignoreCase = true) ||
                                        song.artist.contains(query, ignoreCase = true)
                            }

                    }
                    _gaanaResults.addAll(mappedSongs)
                    rebuildCombinedResults()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchYouTube(query: String) {
        _youtubeResults.clear()
        viewModelScope.launch {
            try {
                val results = NewPipeHelper.search(query)
                val mappedSongs = kotlinx.coroutines.coroutineScope {
                    results.map { song ->
                        async {
                            try {
                                val audioUrl = NewPipeHelper.getAudioUrl(song.streamUrl)
                                    ?: return@async null
                                song.copy(streamUrl = audioUrl)
                            } catch (e: Exception) { null }
                        }
                    }.awaitAll().filterNotNull()
                }
                _youtubeResults.addAll(mappedSongs)
                rebuildCombinedResults()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMore(query: String) {
        if (_isSearching.value || !searchHasMore) return
        searchJob = viewModelScope.launch {
            try {
                _isSearching.value = true
                val response = RetrofitClient.api.searchSongs(
                    query = query,
                    page = searchPage,
                    limit = 10
                )
                if (response.isSuccessful) {
                    val apiSongs = response.body()?.data?.results ?: emptyList()
                    val newSongs = apiSongs.mapNotNull {
                        val url = it.downloadUrl.lastOrNull()?.url
                            ?: return@mapNotNull null
                        Song(
                            id = it.id,
                            title = it.name,
                            artist = it.artists.primary.firstOrNull()?.name ?: "Unknown",
                            albumArt = it.image.lastOrNull()?.url ?: "",
                            streamUrl = url,
                            duration = 0L,
                            album = ""
                        )
                    }
                    _results.addAll(newSongs)
                    rebuildCombinedResults()
                    if (newSongs.isEmpty()) searchHasMore = false
                    else searchPage++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun loadRecommendations(artist: String) {
        if (isLoadingRecommendations) return
        recPage = 1
        recHasMore = true
        _recommendedSongs.value = emptyList()
        loadMoreRecommendations(artist)
    }

    fun loadMoreRecommendations(artist: String) {
        if (isLoadingRecommendations || !recHasMore) return
        viewModelScope.launch {
            try {
                isLoadingRecommendations = true
                val response = RetrofitClient.api.searchSongs(
                    query = artist,
                    page = recPage,
                    limit = 10
                )
                if (response.isSuccessful) {
                    val results = response.body()?.data?.results ?: emptyList()
                    val newSongs = results
                        .filter {
                            it.artists.primary.any { a ->
                                a.name.contains(artist, true)
                            }
                        }
                        .mapNotNull {
                            val url = it.downloadUrl.lastOrNull()?.url
                                ?: return@mapNotNull null
                            Song(
                                id = it.id,
                                title = it.name,
                                artist = it.artists.primary.firstOrNull()?.name ?: "Unknown",
                                albumArt = it.image.lastOrNull()?.url ?: "",
                                streamUrl = url
                            )
                        }
                    _recommendedSongs.value = _recommendedSongs.value + newSongs
                    PlayerManager.addToQueue(newSongs)
                    if (newSongs.isEmpty()) recHasMore = false
                    else recPage++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingRecommendations = false
            }
        }
    }

    init {
        PlayerManager.onNeedMoreSongs = { artist ->
            loadMoreRecommendations(artist)
        }
    }
}