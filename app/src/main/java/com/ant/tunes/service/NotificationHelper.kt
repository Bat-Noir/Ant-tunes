package com.ant.tunes.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.ant.tunes.MainActivity
import com.ant.tunes.R
import com.ant.tunes.data.Song

object NotificationHelper {

    @OptIn(UnstableApi::class)
    fun createNotification(
        context: Context,
        mediaSession: MediaSession,
        song: Song,
        bitmap: Bitmap? = null
    ): Notification {

        val channelId = "music_channel"

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ STRONGER CHANNEL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Music Playback",
                NotificationManager.IMPORTANCE_DEFAULT // 🔥 FIXED
            )
            manager.createNotificationChannel(channel)
        }

        // 🔥 REQUIRED INTENT (VERY IMPORTANT)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setLargeIcon(bitmap?.copy(Bitmap.Config.ARGB_8888, false))

            .setContentIntent(pendingIntent) // 🔥 CRITICAL FIX

            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionCompatToken)
            )

            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)

            .build()
    }
}