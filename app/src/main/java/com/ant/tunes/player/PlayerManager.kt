package com.ant.tunes.player

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ant.tunes.data.DownloadState
import com.ant.tunes.data.Song
import com.ant.tunes.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.Normalizer

object PlayerManager {

    private var player: ExoPlayer? = null
    private var appContext: Context? = null // Used for background history saving

    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = 0

    // 🔥 STATES
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode

    private val _downloadStates = mutableStateMapOf<String, DownloadState>()
    val downloadStates: Map<String, DownloadState> = _downloadStates

    private val _downloadProgress = mutableStateMapOf<String, Float>()
    val downloadProgress: Map<String, Float> = _downloadProgress

    private val _isPlaying = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _playlistFlow = MutableStateFlow<List<Song>>(emptyList())
    val playlistFlow: StateFlow<List<Song>> = _playlistFlow

    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    // 🟢 NEW: REAL DATA TRACKING STATES
    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayed

    private val _topTracks = MutableStateFlow<List<Song>>(emptyList())
    val topTracks: StateFlow<List<Song>> = _topTracks

    private val songPlayCounts = mutableMapOf<String, Int>()

    // 🟢 RECOMMENDATION TRIGGER
    var onNeedMoreSongs: ((Song) -> Unit)? = null

    // 🟢 DE-DUPLICATOR SET
    private val playedSongFingerprints = mutableSetOf<String>()

    private var progressJob: Job? = null
    private var lastLoadTriggerIndex = -1

    fun getPlayer(): ExoPlayer? = player

    enum class RepeatMode { OFF, ALL, ONE }

    // 🟢 SLEEP TIMER STATE
    private var sleepTimerJob: Job? = null
    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes

    // 🟢 SET SLEEP TIMER
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes

        if (minutes > 0) {
            sleepTimerJob = CoroutineScope(Dispatchers.Main).launch {
                var timeLeft = minutes * 60
                while (timeLeft > 0) {
                    delay(1000)
                    timeLeft--
                }
                // Time's up! Pause the music.
                player?.pause()
                _sleepTimerMinutes.value = 0
            }
        }
    }

    // 🟢 APPLY AUDIO TWEAKS (Call this when player initializes and when settings change)
    @OptIn(UnstableApi::class)
    fun applyAudioTweaks(context: Context) {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)

        // Gapless (Skip Silence)
        val skipSilence = prefs.getBoolean("gapless_playback", false)
        player?.skipSilenceEnabled = skipSilence

        // (Mono and Normalize would need custom AudioProcessors injected into ExoPlayer Builder here)
    }


    // 🟢 STRING NORMALIZER FOR DE-DUPLICATION
    private fun generateFingerprint(song: Song): String {
        val title = song.title.lowercase().replace(Regex("\\[.*?\\]|\\(.*?\\)"), "").trim()
        val artist = song.artist.lowercase().replace(Regex("\\[.*?\\]|\\(.*?\\)"), "").trim()
        val normalizedTitle = Normalizer.normalize(title, Normalizer.Form.NFD).replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        val normalizedArtist = Normalizer.normalize(artist, Normalizer.Form.NFD).replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return "$normalizedTitle|$normalizedArtist"
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        updateRepeatMode()
    }

    // 🟢 REORDER QUEUE ITEMS
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= playlist.size || toIndex >= playlist.size) return

        val mutablePlaylist = playlist.toMutableList()
        val item = mutablePlaylist.removeAt(fromIndex)
        mutablePlaylist.add(toIndex, item)

        playlist = mutablePlaylist
        _playlistFlow.value = playlist

        player?.moveMediaItem(fromIndex, toIndex)

        if (currentIndex == fromIndex) {
            currentIndex = toIndex
        } else if (currentIndex in (fromIndex + 1)..toIndex) {
            currentIndex--
        } else if (currentIndex in toIndex until fromIndex) {
            currentIndex++
        }
    }

    fun seekToIndex(index: Int) {
        player?.seekTo(index, 0)
    }

    @OptIn(UnstableApi::class)
    fun init(context: Context) {
        appContext = context.applicationContext
        loadHistory() // 🟢 Load saved history on startup

        if (player == null) {
            // 🟢 Inject the Cache DataSource Factory
            val dataSourceFactory = CacheManager.getCacheDataSourceFactory(context)

            player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(
                    androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                        .setDataSourceFactory(dataSourceFactory)
                )
                .build()


            player?.repeatMode = Player.REPEAT_MODE_OFF
            // 🟢 Apply saved audio tweaks (Gapless, etc.)
            applyAudioTweaks(context)

            player?.addListener(object : Player.Listener {


                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    val index = player?.currentMediaItemIndex ?: 0
                    currentIndex = index

                    val currentList = _playlistFlow.value
                    val newCurrentSong = currentList.getOrNull(index)
                    _currentSong.value = newCurrentSong

                    newCurrentSong?.let { song ->
                        playedSongFingerprints.add(generateFingerprint(song))

                        // 🟢 Trigger LOCAL tracking (History & Top Tracks)
                        appContext?.let { ctx -> trackSongPlay(ctx, song) }

                        // ✅ NEW: Trigger LAST.FM Scrobbling (Cloud Sync)
                        appContext?.let { ctx ->
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    com.ant.tunes.lastfm.LastFmRepository.scrobble(
                                        context = ctx,
                                        track   = song.title,
                                        artist  = song.artist
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    _duration.value = player?.duration ?: 0L
                    savePlaybackState(context)

                    val remaining = _playlistFlow.value.size - currentIndex

                    if (!_isOfflineMode.value && remaining <= 2 && currentIndex != lastLoadTriggerIndex) {
                        lastLoadTriggerIndex = currentIndex
                        _currentSong.value?.let { currentTrack ->
                            onNeedMoreSongs?.invoke(currentTrack)
                        }
                    }
                }


                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED && _isOfflineMode.value) {
                        when (_repeatMode.value) {
                            RepeatMode.ONE -> {
                                player?.seekTo(0)
                                player?.play()
                            }
                            RepeatMode.ALL -> playNextOffline(true)
                            RepeatMode.OFF -> playNextOffline(false)
                        }
                    }
                    savePlaybackState(context)
                }
            })
        }
    }

    // ═══════════════════════════════════════
    // 🟢 REAL DATA TRACKING SYSTEM
    // ═══════════════════════════════════════
    private fun trackSongPlay(context: Context, song: Song) {
        // 1. Update "Recently Played"
        val recentList = _recentlyPlayed.value.toMutableList()
        recentList.removeAll { it.id == song.id } // Remove duplicate if it was played recently
        recentList.add(0, song) // Add to the very front
        val limitedRecent = recentList.take(20) // Keep it to the last 20
        _recentlyPlayed.value = limitedRecent

        // 2. Update Play Counts
        val currentCount = songPlayCounts[song.id] ?: 0
        songPlayCounts[song.id] = currentCount + 1

        // 3. Update "Top Tracks"
        val topList = _topTracks.value.toMutableList()
        val existingIndex = topList.indexOfFirst { it.id == song.id }
        if (existingIndex != -1) {
            topList[existingIndex] = song // Update in case metadata changed
        } else {
            topList.add(song)
        }

        // Sort by the play count from highest to lowest
        topList.sortByDescending { songPlayCounts[it.id] ?: 0 }
        val limitedTop = topList.take(20)
        _topTracks.value = limitedTop

        // 4. Persist to disk quietly
        CoroutineScope(Dispatchers.IO).launch {
            saveHistory(context, limitedRecent, limitedTop, songPlayCounts)
        }
    }

    // 🟢 SERVICE RESURRECTION JUTSU
    private fun wakeUpService() {
        appContext?.let { ctx ->
            try {
                val intent = Intent(ctx, MusicService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    ctx.startForegroundService(intent)
                } else {
                    ctx.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveHistory(context: Context, recent: List<Song>, top: List<Song>, counts: Map<String, Int>) {
        val prefs = context.getSharedPreferences("user_history", Context.MODE_PRIVATE)

        fun songToJson(s: Song) = JSONObject().apply {
            put("id", s.id)
            put("title", s.title)
            put("artist", s.artist)
            put("albumArt", s.albumArt)
            put("streamUrl", s.streamUrl)
            put("localPath", s.localPath ?: "")
            put("source", s.source)
        }

        val recentArray = JSONArray().apply { recent.forEach { put(songToJson(it)) } }
        val topArray = JSONArray().apply { top.forEach { put(songToJson(it)) } }
        val countsObject = JSONObject().apply { counts.forEach { (id, count) -> put(id, count) } }

        prefs.edit()
            .putString("recent", recentArray.toString())
            .putString("top", topArray.toString())
            .putString("counts", countsObject.toString())
            .apply()
    }

    private fun loadHistory() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences("user_history", Context.MODE_PRIVATE)

        val recentStr = prefs.getString("recent", "[]")
        val topStr = prefs.getString("top", "[]")
        val countsStr = prefs.getString("counts", "{}")

        fun jsonToSong(obj: JSONObject): Song {
            return Song(
                id = obj.getString("id"),
                title = obj.getString("title"),
                artist = obj.getString("artist"),
                albumArt = obj.optString("albumArt", ""),
                streamUrl = obj.getString("streamUrl"),
                localPath = obj.optString("localPath", ""),
                source = obj.optString("source", "saavn"),
                isDownloaded = obj.optString("localPath", "").isNotEmpty()
            )
        }

        try {
            val countsObj = JSONObject(countsStr)
            countsObj.keys().forEach { key -> songPlayCounts[key] = countsObj.getInt(key) }

            val recentArray = JSONArray(recentStr)
            val recentList = mutableListOf<Song>()
            for (i in 0 until recentArray.length()) recentList.add(jsonToSong(recentArray.getJSONObject(i)))
            _recentlyPlayed.value = recentList

            val topArray = JSONArray(topStr)
            val topList = mutableListOf<Song>()
            for (i in 0 until topArray.length()) topList.add(jsonToSong(topArray.getJSONObject(i)))
            _topTracks.value = topList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // ═══════════════════════════════════════

    private fun savePlaybackState(context: Context) {
        val prefs = context.getSharedPreferences("player_state", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()

        _playlistFlow.value.forEach { song ->
            val obj = JSONObject()
            obj.put("id", song.id)
            obj.put("title", song.title)
            obj.put("artist", song.artist)
            obj.put("albumArt", song.albumArt)
            obj.put("streamUrl", song.streamUrl)
            obj.put("localPath", song.localPath)
            obj.put("isDownloaded", song.isDownloaded)
            obj.put("source", song.source)
            jsonArray.put(obj)
        }

        prefs.edit()
            .putString("playlist", jsonArray.toString())
            .putInt("index", currentIndex)
            .putLong("position", player?.currentPosition ?: 0L)
            .putBoolean("offline", _isOfflineMode.value)
            .apply()
    }

    private fun playNextOffline(loop: Boolean) {
        if (playlist.isEmpty()) return
        val nextIndex = currentIndex + 1

        if (nextIndex < playlist.size) {
            currentIndex = nextIndex
        } else if (loop) {
            currentIndex = 0
        } else {
            return
        }

        player?.seekTo(currentIndex, 0)
        player?.play()
        _currentSong.value = playlist[currentIndex]
    }

    fun updateRepeatMode() {
        player?.repeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun loadLibrary(context: Context) {
        val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)
        val json = prefs.getString("songs", null) ?: return

        val jsonArray = JSONArray(json)
        val loadedSongs = mutableListOf<Song>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val song = Song(
                id = obj.getString("id"),
                title = obj.getString("title"),
                artist = obj.getString("artist"),
                albumArt = obj.getString("albumArt"),
                streamUrl = obj.getString("localPath"),
                isDownloaded = true,
                localPath = obj.getString("localPath"),
                source = obj.optString("source", "offline")
            )

            loadedSongs.add(song)
            _downloadStates[song.id] = DownloadState.DOWNLOADED
            _downloadProgress[song.id] = 1f
        }
        _downloadedSongs.value = loadedSongs
    }

    private fun saveLibrary(context: Context) {
        val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()

        _downloadedSongs.value.forEach { song ->
            val obj = JSONObject()
            obj.put("id", song.id)
            obj.put("title", song.title)
            obj.put("artist", song.artist)
            obj.put("albumArt", song.albumArt)
            obj.put("localPath", song.localPath)
            obj.put("source", song.source)
            jsonArray.put(obj)
        }
        prefs.edit().putString("songs", jsonArray.toString()).apply()
    }

    fun downloadSong(context: Context, song: Song) {
        if (_downloadStates[song.id] == DownloadState.DOWNLOADING) return

        _downloadStates[song.id] = DownloadState.DOWNLOADING
        _downloadProgress[song.id] = 0f

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(song.streamUrl)
                val connection = url.openConnection()
                connection.connect()
                val input = connection.getInputStream()

                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = if (musicDir.exists() || musicDir.mkdirs()) musicDir else downloadDir

                val safeName = "${song.title}-${song.artist}".replace("[^a-zA-Z0-9]".toRegex(), "_")
                val file = File(targetDir, "$safeName.mp3")
                val output = FileOutputStream(file)

                val buffer = ByteArray(1024)
                var total = 0L
                val fileLength = connection.contentLength
                var count: Int

                while (input.read(buffer).also { count = it } != -1) {
                    total += count
                    output.write(buffer, 0, count)
                    _downloadProgress[song.id] = total.toFloat() / fileLength
                }

                output.flush()
                output.close()
                input.close()

                _downloadStates[song.id] = DownloadState.DOWNLOADED
                val savedSong = song.copy(isDownloaded = true, localPath = file.absolutePath)

                if (_downloadedSongs.value.none { it.id == song.id }) {
                    _downloadedSongs.value = _downloadedSongs.value + savedSong
                }
                saveLibrary(context)

            } catch (e: Exception) {
                e.printStackTrace()
                _downloadStates[song.id] = DownloadState.NOT_DOWNLOADED
            }
        }
    }

    fun deleteSong(context: Context, song: Song) {
        try {
            val file = File(song.localPath)
            if (file.exists()) file.delete()
            _downloadStates[song.id] = DownloadState.NOT_DOWNLOADED
            _downloadProgress[song.id] = 0f
            _downloadedSongs.value = _downloadedSongs.value.filter { it.id != song.id }
            saveLibrary(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addToDownloaded(context: Context, song: Song) {
        if (_downloadedSongs.value.any { it.id == song.id }) return
        val savedSong = song.copy(isDownloaded = true, localPath = song.streamUrl)
        _downloadedSongs.value = _downloadedSongs.value + savedSong
        _downloadStates[song.id] = DownloadState.DOWNLOADED
        _downloadProgress[song.id] = 1f
        saveLibrary(context)
    }

    fun play(context: Context, songs: List<Song>, index: Int) {
        val intent = Intent(context, MusicService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        _isOfflineMode.value = false
        playedSongFingerprints.clear()

        playlist = songs
        _playlistFlow.value = songs
        currentIndex = index

        val mediaItems = songs.map { song ->
            if (song.source == "gaana") {
                MediaItem.Builder().setUri(song.streamUrl).setMimeType("application/x-mpegURL").build()
            } else {
                MediaItem.fromUri(song.streamUrl)
            }
        }

        player?.setMediaItems(mediaItems)
        player?.prepare()
        player?.seekTo(index, 0)
        player?.play()
        _duration.value = player?.duration ?: 0L
        _currentSong.value = songs.getOrNull(index)
    }

    fun addToQueue(newSongs: List<Song>) {
        if (newSongs.isEmpty()) return

        val filtered = newSongs.filter { newSong ->
            val fingerprint = generateFingerprint(newSong)
            val isUnique = playlist.none { it.id == newSong.id } && !playedSongFingerprints.contains(fingerprint)

            if (isUnique) {
                playedSongFingerprints.add(fingerprint)
                true
            } else false
        }

        if (filtered.isEmpty()) return

        playlist = playlist + filtered
        _playlistFlow.value = playlist

        val mediaItems = filtered.map { song ->
            when (song.source) {
                "gaana" -> MediaItem.Builder().setUri(song.streamUrl).setMimeType("application/x-mpegURL").build()
                else -> MediaItem.fromUri(song.streamUrl)
            }
        }

        player?.addMediaItems(mediaItems)
    }

    fun playStream(context: Context, song: Song) {
        val intent = Intent(context, MusicService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        playedSongFingerprints.clear()

        if (song.isDownloaded) {
            _isOfflineMode.value = true
            playlist = _downloadedSongs.value
            _playlistFlow.value = playlist
            currentIndex = playlist.indexOfFirst { it.id == song.id }
            val mediaItems = playlist.map { MediaItem.fromUri(it.localPath) }
            player?.setMediaItems(mediaItems)
            player?.prepare()
            player?.seekTo(currentIndex, 0)
            player?.play()
        } else {
            _isOfflineMode.value = false
            playlist = _playlistFlow.value.ifEmpty { listOf(song) }
            _playlistFlow.value = playlist
            currentIndex = 0
            val mediaItem = if (song.source == "gaana") {
                MediaItem.Builder().setUri(song.streamUrl).setMimeType("application/x-mpegURL").build()
            } else {
                MediaItem.fromUri(song.streamUrl)
            }
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
        }
        _currentSong.value = song
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                wakeUpService() // 🔥 Force wake the service on resume!
                it.play()
            }
        }
    }

    fun next() {
        wakeUpService() // 🔥 Force wake the service!
        player?.seekToNextMediaItem()
    }

    fun previous() {
        wakeUpService() // 🔥 Force wake the service!
        player?.seekToPreviousMediaItem()
    }


    fun seekTo(position: Long) { player?.seekTo(position) }

    fun release() {
        player?.release()
        player = null
    }

    fun restorePlayback(context: Context) {
        val prefs = context.getSharedPreferences("player_state", Context.MODE_PRIVATE)
        val json = prefs.getString("playlist", null) ?: return
        val index = prefs.getInt("index", 0)
        val position = prefs.getLong("position", 0L)
        val isOffline = prefs.getBoolean("offline", false)

        val jsonArray = JSONArray(json)
        val songs = mutableListOf<Song>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            songs.add(
                Song(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    artist = obj.getString("artist"),
                    albumArt = obj.getString("albumArt"),
                    streamUrl = obj.getString("streamUrl"),
                    localPath = obj.optString("localPath"),
                    isDownloaded = obj.getBoolean("isDownloaded"),
                    source = obj.optString("source", "unknown")
                )
            )
        }

        playlist = songs
        _playlistFlow.value = songs
        currentIndex = index
        _isOfflineMode.value = isOffline

        songs.forEach { playedSongFingerprints.add(generateFingerprint(it)) }

        val mediaItems = songs.map {
            if (it.isDownloaded) MediaItem.fromUri(it.localPath)
            else MediaItem.fromUri(it.streamUrl)
        }

        player?.setMediaItems(mediaItems)
        player?.prepare()
        player?.seekTo(index, position)
        _currentSong.value = songs.getOrNull(index)
    }

    fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                player?.let { exoPlayer ->
                    _currentPosition.value = maxOf(0L, exoPlayer.currentPosition)
                    _duration.value = maxOf(0L, exoPlayer.duration)
                }
                delay(500)
            }
        }
    }
}
