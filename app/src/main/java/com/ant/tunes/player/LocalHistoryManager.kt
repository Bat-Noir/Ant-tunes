package com.ant.tunes.player

import android.content.Context
import com.ant.tunes.data.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow // 🟢 Added required import

object LocalHistoryManager {
    private const val PREFS_NAME = "ant_tunes_local_data"
    private const val KEY_RECENT = "recent_tracks"
    private const val KEY_TOP = "top_tracks"

    // 🟢 NEW: Prevents pausing and resuming from tracking the same song twice
    private var lastTrackedSongId: String? = null

    // 🟢 Call this ONCE when the app starts
    fun loadHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<List<Song>>() {}.type

        try {
            val recentJson = prefs.getString(KEY_RECENT, "[]")
            val topJson = prefs.getString(KEY_TOP, "[]")

            val recentList: List<Song> = gson.fromJson(recentJson, type) ?: emptyList()
            val topList: List<Song> = gson.fromJson(topJson, type) ?: emptyList()

            // 🟢 FIXED: Bypasses the read-only lock using MutableStateFlow cast
            (PlayerManager.recentlyPlayed as MutableStateFlow).value = recentList
            (PlayerManager.topTracks as MutableStateFlow).value = topList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 🟢 Call this EVERY TIME a song plays
    fun trackSongPlayed(context: Context, song: Song) {
        // 🟢 THE FIX: If we just tracked this exact song, ignore it! No more pause/play spam.
        if (song.id == lastTrackedSongId) return
        lastTrackedSongId = song.id

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()

        // 1. Update Recently Played (Push to top, limit to 30)
        val currentRecent = PlayerManager.recentlyPlayed.value.toMutableList()
        currentRecent.removeAll { it.id == song.id } // Remove duplicate if it exists
        currentRecent.add(0, song) // Push to the very front
        val trimmedRecent = currentRecent.take(30)

        // 🟢 FIXED: Bypasses the read-only lock using MutableStateFlow cast
        (PlayerManager.recentlyPlayed as MutableStateFlow).value = trimmedRecent

        // 2. Update Top Tracks (Simple logic: if played again, bump it up the charts)
        val currentTop = PlayerManager.topTracks.value.toMutableList()
        val existingIndex = currentTop.indexOfFirst { it.id == song.id }

        if (existingIndex != -1) {
            // Song exists? Bump it up 2 spots in the Top Tracks rankings
            val item = currentTop.removeAt(existingIndex)
            val newIndex = maxOf(0, existingIndex - 2)
            currentTop.add(newIndex, item)
        } else {
            // New song? Add it to the tracking list
            currentTop.add(song)
        }
        val trimmedTop = currentTop.take(30)

        // 🟢 FIXED: Bypasses the read-only lock using MutableStateFlow cast
        (PlayerManager.topTracks as MutableStateFlow).value = trimmedTop

        // 3. Save permanently to device storage
        prefs.edit()
            .putString(KEY_RECENT, gson.toJson(trimmedRecent))
            .putString(KEY_TOP, gson.toJson(trimmedTop))
            .apply()
    }
}
