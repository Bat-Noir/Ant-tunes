package com.ant.tunes.player

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ant.tunes.data.DownloadState
import com.ant.tunes.data.Song
import com.ant.tunes.service.MusicService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object PlayerManager {

    private var player: ExoPlayer? = null

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
    var onNeedMoreSongs: ((String) -> Unit)? = null
    private var progressJob: Job? = null

    private var lastLoadTriggerIndex = -1
    fun getPlayer(): ExoPlayer? = player

    enum class RepeatMode {
        OFF,
        ALL,
        ONE
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

        updateRepeatMode() // 🔥 THIS LINE WAS MISSING
    }

    fun seekToIndex(index: Int) {
        player?.seekTo(index, 0)
    }

    // 🔥 INIT
    fun init(context: Context) {
        if (player == null) {
            player = ExoPlayer.Builder(context).build()
            player?.repeatMode = Player.REPEAT_MODE_OFF
            player?.addListener(object : Player.Listener {

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {

                    val index = player?.currentMediaItemIndex ?: 0
                    currentIndex = index

                    val currentList = _playlistFlow.value
                    _currentSong.value = currentList.getOrNull(index)

                    _duration.value = player?.duration ?: 0L

                    savePlaybackState(context) // 🔥 SAVE HERE

                    // 🔥 LOAD MORE WHEN NEAR END
                    val remaining = _playlistFlow.value.size - currentIndex

                    if (!_isOfflineMode.value && remaining <= 2 && currentIndex != lastLoadTriggerIndex) {
                        lastLoadTriggerIndex = currentIndex

                        _currentSong.value?.artist?.let { artist ->
                            onNeedMoreSongs?.invoke(artist)
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

    // 🔥 LOAD LIBRARY
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
                localPath = obj.getString("localPath")
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
            jsonArray.put(obj)
        }

        prefs.edit().putString("songs", jsonArray.toString()).apply()
    }

    // 🔥 DOWNLOAD
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

                val targetDir = if (musicDir.exists() || musicDir.mkdirs()) {
                    musicDir
                } else downloadDir

                val safeName = "${song.title}-${song.artist}"
                    .replace("[^a-zA-Z0-9]".toRegex(), "_")

                val file = File(targetDir, "$safeName.mp3")
                val output = FileOutputStream(file)

                val buffer = ByteArray(1024)
                var total = 0L
                val fileLength = connection.contentLength

                var count: Int

                while (input.read(buffer).also { count = it } != -1) {
                    total += count
                    output.write(buffer, 0, count)

                    val progress = total.toFloat() / fileLength
                    _downloadProgress[song.id] = progress
                }

                output.flush()
                output.close()
                input.close()

                _downloadStates[song.id] = DownloadState.DOWNLOADED

                val savedSong = song.copy(
                    isDownloaded = true,
                    localPath = file.absolutePath
                )

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

    // 🔥 DELETE
    fun deleteSong(context: Context, song: Song) {

        try {
            val file = File(song.localPath)
            if (file.exists()) file.delete()

            _downloadStates[song.id] = DownloadState.NOT_DOWNLOADED
            _downloadProgress[song.id] = 0f

            _downloadedSongs.value =
                _downloadedSongs.value.filter { it.id != song.id }

            saveLibrary(context)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 🔥 PLAY ONLINE
    fun play(context: Context, songs: List<Song>, index: Int) {
        val intent = Intent(context, MusicService::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        _isOfflineMode.value = false

        playlist = songs
        _playlistFlow.value = songs
        currentIndex = index

        val mediaItems = songs.map { MediaItem.fromUri(it.streamUrl) }

        player?.setMediaItems(mediaItems)
        player?.prepare()
        player?.seekTo(index, 0)
        player?.play()
        _duration.value = player?.duration ?: 0L
        _currentSong.value = songs.getOrNull(index)
    }

    fun addToQueue(newSongs: List<Song>) {

        if (newSongs.isEmpty()) return

        // 🔥 update internal playlist
        val filtered = newSongs.filter { new ->
            playlist.none { it.id == new.id }
        }

        playlist = playlist + filtered
        _playlistFlow.value = playlist

        val mediaItems = filtered.map {
            MediaItem.fromUri(it.streamUrl)
        }

        player?.addMediaItems(mediaItems)
    }
    // 🔥 PLAY SINGLE
    fun playStream(context: Context, song: Song) {

        val intent = Intent(context, MusicService::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

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

            val mediaItem = MediaItem.fromUri(song.streamUrl)

            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
        }

        _currentSong.value = song
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun next() {
        player?.seekToNextMediaItem()
    }

    fun previous() {
        player?.seekToPreviousMediaItem()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

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
                    isDownloaded = obj.getBoolean("isDownloaded")
                )
            )
        }

        playlist = songs
        _playlistFlow.value = songs
        currentIndex = index
        _isOfflineMode.value = isOffline

        val mediaItems = songs.map {
            if (it.isDownloaded) MediaItem.fromUri(it.localPath)
            else MediaItem.fromUri(it.streamUrl)
        }

        player?.setMediaItems(mediaItems)
        player?.prepare()
        player?.seekTo(index, position)

        _currentSong.value = songs.getOrNull(index)
    }

    // 🔥 SEEK BAR UPDATES
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