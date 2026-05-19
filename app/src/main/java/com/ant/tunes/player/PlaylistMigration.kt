package com.ant.tunes.player

import android.content.Context
import com.ant.tunes.data.Song
import com.ant.tunes.data.SourceType

object PlaylistMigration {

    fun migrateSong(old: Song): Song {
        // Extract YouTube video ID from old stream URL
        val ytVideoId = when {
            old.videoId.isNotBlank() -> old.videoId
            old.streamUrl.contains("watch?v=") ->
                old.streamUrl.substringAfter("watch?v=").substringBefore("&")
            old.streamUrl.contains("youtu.be/") ->
                old.streamUrl.substringAfter("youtu.be/").substringBefore("?")
            else -> ""
        }

        val resolvedSource = when {
            old.sourceType != SourceType.UNKNOWN -> old.sourceType
            old.source.equals("saavn", true) -> SourceType.SAAVN
            old.source.equals("gaana", true) -> SourceType.GAANA
            old.source.equals("youtube", true) -> SourceType.YOUTUBE
            old.source.equals("local", true) -> SourceType.LOCAL
            else -> SourceType.UNKNOWN
        }

        return old.copy(
            sourceType   = resolvedSource,
            videoId      = ytVideoId,
            streamUrl    = "" // clear expiring URL
        )
    }

    fun migratePlaylist(songs: List<Song>): List<Song> =
        songs.map { migrateSong(it) }

    // Call this once on app start
    fun runIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("playlist_migrated_v2", false)) return

        // Migrate playlists
        val playlists = AppDataManager.loadPlaylists(context)
        playlists.forEach { playlist ->
            val migrated = migratePlaylist(playlist.tracks.toList())
            playlist.tracks.clear()
            playlist.tracks.addAll(migrated)
        }
        if (playlists.isNotEmpty()) {
            AppDataManager.savePlaylists(context, playlists)
        }

        // Migrate liked songs
        val liked = AppDataManager.loadLikedSongs(context)
        val migratedLiked = migratePlaylist(liked)
        AppDataManager.saveLikedSongs(context, migratedLiked)

        prefs.edit().putBoolean("playlist_migrated_v2", true).apply()
        android.util.Log.d("Migration", "✅ Playlist migration v2 complete")
    }
}
