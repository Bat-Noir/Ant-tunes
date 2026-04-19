package com.ant.tunes.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ant.tunes.data.Song
import com.ant.tunes.network.RetrofitClient
import com.ant.tunes.network.SongDto
import com.ant.tunes.player.PlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {

    private var searchJob: Job? = null

    // 🔍 SEARCH PAGINATION
    private var searchPage = 1
    private var searchHasMore = true

    // 🎧 RECOMMENDATION PAGINATION
    private var recPage = 1
    private var recHasMore = true
    private var isLoadingRecommendations = false

    // 🔥 UI STATE
    private val _isSearching = mutableStateOf(false)
    val loading: State<Boolean> = _isSearching

    private val _results = mutableStateListOf<SongDto>()
    val searchResults: List<SongDto> get() = _results

    private val _recommendedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recommendedSongs: StateFlow<List<Song>> = _recommendedSongs

    // =========================
    // 🔍 SEARCH
    // =========================
    fun searchSongs(query: String) {
        searchJob?.cancel()

        searchPage = 1
        searchHasMore = true
        _results.clear()

        loadMore(query)
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
                    val newSongs = response.body()?.data?.results ?: emptyList()

                    _results.addAll(newSongs)

                    if (newSongs.isEmpty()) {
                        searchHasMore = false
                    } else {
                        searchPage++
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    // =========================
    // 🎧 INITIAL RECOMMENDATION LOAD
    // =========================
    fun loadRecommendations(artist: String) {

        if (isLoadingRecommendations) return

        recPage = 1
        recHasMore = true
        _recommendedSongs.value = emptyList()

        loadMoreRecommendations(artist)
    }

    // =========================
    // ♾️ INFINITE QUEUE LOAD
    // =========================
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

                    // 🔥 APPEND (NOT REPLACE)
                    _recommendedSongs.value =
                        _recommendedSongs.value + newSongs

                    // 🔥 ADD TO PLAYER QUEUE
                    PlayerManager.addToQueue(newSongs)

                    if (newSongs.isEmpty()) {
                        recHasMore = false
                    } else {
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

    // =========================
    // 🎯 CONNECT TO PLAYER
    // =========================
    init {
        PlayerManager.onNeedMoreSongs = { artist ->
            loadMoreRecommendations(artist)
        }
    }
}