package com.ant.tunes.service

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ant.tunes.player.PlayerManager
import kotlinx.coroutines.*
import coil.ImageLoader
import coil.request.ImageRequest
import android.graphics.drawable.BitmapDrawable
import android.util.Log

class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.d("ANT_DEBUG", "SERVICE CREATED")
        PlayerManager.init(this)
        val player = PlayerManager.getPlayer()!!

        mediaSession = MediaSession.Builder(this, player).build()

        // 🔥 CRITICAL: start foreground IMMEDIATELY
        val initialNotification = NotificationHelper.createNotification(
            context = this,
            mediaSession = mediaSession!!,
            song = com.ant.tunes.data.Song(
                id = "0",
                title = "Starting...",
                artist = "AntTunes",
                albumArt = "",
                streamUrl = ""
            )
        )

        startForeground(1, initialNotification)

        val imageLoader = ImageLoader(this)

        // 🔥 THEN collect updates
        // 🔥 THEN collect updates
        serviceScope.launch {
            PlayerManager.currentSong.collect { song ->
                val session = mediaSession ?: return@collect
                val currentSong = song ?: return@collect

                // 1. 🟢 INJECT METADATA FOR THE NATIVE LOCKSCREEN
                val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(currentSong.title)
                    .setArtist(currentSong.artist)
                    .setArtworkUri(android.net.Uri.parse(currentSong.albumArt))
                    .build()

                val player = PlayerManager.getPlayer()
                player?.currentMediaItem?.let { item ->
                    val index = player.currentMediaItemIndex
                    if (index >= 0) {
                        player.replaceMediaItem(index, item.buildUpon().setMediaMetadata(mediaMetadata).build())
                    }
                }

                // Show base notification immediately
                val baseNotification = NotificationHelper.createNotification(
                    this@MusicService, session, currentSong, null
                )
                startForeground(1, baseNotification)

                // 2. 🟢 FORCE COIL TO FINISH BEFORE GC KILLS IT
                try {
                    val request = ImageRequest.Builder(this@MusicService)
                        .data(currentSong.albumArt)
                        .size(coil.size.Size.ORIGINAL)
                        .allowHardware(false)
                        .build()

                    val result = imageLoader.execute(request) // Blocks until downloaded!
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap

                    val updatedNotification = NotificationHelper.createNotification(
                        this@MusicService, session, currentSong, bitmap
                    )
                    startForeground(1, updatedNotification)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        Log.d("ANT_DEBUG", "START COMMAND CALLED")
        super.onStartCommand(intent, flags, startId)

        val session = mediaSession ?: return START_STICKY

        val song = PlayerManager.currentSong.value

        // ✅ ALWAYS start foreground even if no song yet
        val notification = NotificationHelper.createNotification(
            context = this,
            mediaSession = session,
            song = song ?: com.ant.tunes.data.Song(
                id = "0",
                title = "Loading...",
                artist = "Please wait",
                albumArt = "",
                streamUrl = ""
            )
        )

        startForeground(1, notification)

        return START_STICKY
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}