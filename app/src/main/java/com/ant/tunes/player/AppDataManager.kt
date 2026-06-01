package com.ant.tunes.player

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.ant.tunes.data.Song
import com.ant.tunes.data.SourceType
import com.ant.tunes.ui.PlaylistData
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.emptyList

object AppDataManager {

    // ═══════════════════════════════════════
    // 🟢 TELEMETRY & LIVE LEADERBOARD ENGINE
    // ═══════════════════════════════════════

    fun incrementPlayCount(context: Context, song: Song) {
        val prefs = context.getSharedPreferences("ant_telemetry", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("play_count_${song.id}", 0)
        prefs.edit().putInt("play_count_${song.id}", currentCount + 1).apply()

        // 🟢 NEW: Update the Live Leaderboard!
        val currentTopTracks = loadTopTracks(context).toMutableList()
        currentTopTracks.removeAll { it.id == song.id } // Remove old entry
        currentTopTracks.add(song) // Add updated entry

        // Sort the entire list by who has the highest play count
        currentTopTracks.sortByDescending { getPlayCount(context, it.id) }

        // Save the top 20 most played songs back to the device
        saveTopTracks(context, currentTopTracks.take(20))
    }

    fun getPlayCount(context: Context, songId: String): Int {
        val prefs = context.getSharedPreferences("ant_telemetry", Context.MODE_PRIVATE)
        return prefs.getInt("play_count_$songId", 0)
    }

    private fun saveTopTracks(context: Context, songs: List<Song>) {
        val prefs = context.getSharedPreferences("ant_telemetry", Context.MODE_PRIVATE)
        val array = JSONArray()
        songs.forEach { song ->
            val obj = JSONObject()
            obj.put("id", song.id)
            obj.put("title", song.title)
            obj.put("artist", song.artist)
            obj.put("albumArt", song.albumArt)
            obj.put("streamUrl", song.streamUrl)
            obj.put("album", song.album ?: "")
            obj.put("source", song.source ?: "")
            array.put(obj)
        }
        prefs.edit().putString("top_tracks_data", array.toString()).apply()
    }

    fun loadTopTracks(context: Context): List<Song> {
        val prefs = context.getSharedPreferences("ant_telemetry", Context.MODE_PRIVATE)
        val json = prefs.getString("top_tracks_data", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Song(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    artist = obj.getString("artist"),
                    albumArt = obj.getString("albumArt"),
                    streamUrl = obj.getString("streamUrl"),
                    album = obj.optString("album", ""),
                    source = obj.optString("source", "")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    // ── LIKED SONGS ──
    fun saveLikedSongs(context: Context, songs: List<Song>) {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        val array = JSONArray()
        songs.forEach { song ->
            val obj = JSONObject()
            obj.put("id", song.id)
            obj.put("title", song.title)
            obj.put("artist", song.artist)
            obj.put("albumArt", song.albumArt)
            obj.put("streamUrl", song.streamUrl)
            obj.put("album", song.album ?: "")
            obj.put("source", song.source ?: "")
            array.put(obj)
        }
        prefs.edit().putString("liked_songs", array.toString()).apply()
    }

    fun loadLikedSongs(context: Context): List<Song> {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("liked_songs", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Song(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    artist = obj.getString("artist"),
                    albumArt = obj.getString("albumArt"),
                    streamUrl = obj.getString("streamUrl"),
                    album = obj.optString("album", ""),
                    source = obj.optString("source", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── SAVED ALBUMS ──
    // 🟢 Renamed to match LibraryScreen!
    fun saveSavedAlbums(context: Context, albums: List<com.ant.tunes.ui.BrowseCard>) {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        val array = org.json.JSONArray()
        albums.forEach { a ->
            val obj = org.json.JSONObject()
            obj.put("id", a.id)
            obj.put("title", a.title)
            obj.put("imageUrl", a.imageUrl)
            array.put(obj)
        }
        prefs.edit().putString("saved_albums", array.toString()).apply()
    }

    fun loadSavedAlbums(context: Context): List<com.ant.tunes.ui.BrowseCard> {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("saved_albums", null) ?: return emptyList()
        return try {
            val array = org.json.JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                com.ant.tunes.ui.BrowseCard(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    imageUrl = obj.getString("imageUrl")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    // ── FOLLOWED ARTISTS ──
    // 🟢 Renamed to match LibraryScreen!
    fun saveFollowedArtists(context: Context, artists: List<com.ant.tunes.ui.BrowseCard>) {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        val array = org.json.JSONArray()
        artists.forEach { a ->
            val obj = org.json.JSONObject()
            obj.put("id", a.id)
            obj.put("title", a.title)
            obj.put("imageUrl", a.imageUrl)
            array.put(obj)
        }
        prefs.edit().putString("followed_artists", array.toString()).apply()
    }

    fun loadFollowedArtists(context: Context): List<com.ant.tunes.ui.BrowseCard> {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("followed_artists", null) ?: return emptyList()
        return try {
            val array = org.json.JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                com.ant.tunes.ui.BrowseCard(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    imageUrl = obj.getString("imageUrl")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    // ── PLAYLISTS ──
    fun savePlaylists(context: Context, playlists: List<PlaylistData>) {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        val array = JSONArray()
        playlists.forEach { playlist ->
            val obj = JSONObject()
            obj.put("id", playlist.id)
            obj.put("name", playlist.name.value)
            val tracksArray = JSONArray()
            playlist.tracks.forEach { song ->
                val trackObj = JSONObject()
                trackObj.put("id", song.id)
                trackObj.put("title", song.title)
                trackObj.put("artist", song.artist)
                trackObj.put("albumArt", song.albumArt)
                trackObj.put("album", song.album)
                trackObj.put("source", song.source)
                trackObj.put("sourceType", song.sourceType.name)
                trackObj.put("permanentUrl", song.permanentUrl)
                trackObj.put("videoId", song.videoId)
                trackObj.put("duration", song.duration)
                // ✅ NEVER save streamUrl — it expires!
                tracksArray.put(trackObj)
            }
            obj.put("tracks", tracksArray)
            array.put(obj)
        }
        prefs.edit().putString("playlists", array.toString()).apply()
    }

    fun loadPlaylists(context: Context): List<PlaylistData> {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("playlists", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val playlist = PlaylistData(
                    id = obj.getString("id"),
                    name = mutableStateOf(obj.getString("name"))
                )
                val tracksArray = obj.getJSONArray("tracks")
                (0 until tracksArray.length()).forEach { j ->
                    val t = tracksArray.getJSONObject(j)
                    playlist.tracks.add(
                        Song(
                            id = t.getString("id"),
                            title = t.getString("title"),
                            artist = t.getString("artist"),
                            albumArt = t.optString("albumArt", ""),
                            album = t.optString("album", ""),
                            source = t.optString("source", ""),
                            sourceType = try {
                                SourceType.valueOf(t.optString("sourceType", "UNKNOWN"))
                            } catch (e: Exception) {
                                SourceType.UNKNOWN
                            },
                            permanentUrl = t.optString("permanentUrl", ""),
                            videoId = t.optString("videoId", ""),
                            duration = t.optLong("duration", 0L),
                            streamUrl = "" // ✅ never restore old stream URLs
                        )
                    )
                }
                playlist
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
