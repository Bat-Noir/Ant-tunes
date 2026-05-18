package com.ant.tunes.player

import android.content.Context
import com.ant.tunes.data.Song
import org.json.JSONArray
import org.json.JSONObject

object AppDataManager {

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
                    id       = obj.getString("id"),
                    title    = obj.getString("title"),
                    artist   = obj.getString("artist"),
                    albumArt = obj.getString("albumArt"),
                    streamUrl = obj.getString("streamUrl"),
                    album    = obj.optString("album", ""),
                    source   = obj.optString("source", "")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    // ── PLAYLISTS ──
    fun savePlaylists(context: Context, playlists: List<com.ant.tunes.ui.PlaylistData>) {
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
                trackObj.put("streamUrl", song.streamUrl)
                trackObj.put("album", song.album ?: "")
                trackObj.put("source", song.source ?: "")
                tracksArray.put(trackObj)
            }
            obj.put("tracks", tracksArray)
            array.put(obj)
        }
        prefs.edit().putString("playlists", array.toString()).apply()
    }

    fun loadPlaylists(context: Context): List<com.ant.tunes.ui.PlaylistData> {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("playlists", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val playlist = com.ant.tunes.ui.PlaylistData(
                    id = obj.getString("id"),
                    name = androidx.compose.runtime.mutableStateOf(obj.getString("name"))
                )
                val tracksArray = obj.getJSONArray("tracks")
                (0 until tracksArray.length()).forEach { j ->
                    val trackObj = tracksArray.getJSONObject(j)
                    playlist.tracks.add(
                        Song(
                            id        = trackObj.getString("id"),
                            title     = trackObj.getString("title"),
                            artist    = trackObj.getString("artist"),
                            albumArt  = trackObj.getString("albumArt"),
                            streamUrl = trackObj.getString("streamUrl"),
                            album     = trackObj.optString("album", ""),
                            source    = trackObj.optString("source", "")
                        )
                    )
                }
                playlist
            }
        } catch (e: Exception) { emptyList() }
    }
}