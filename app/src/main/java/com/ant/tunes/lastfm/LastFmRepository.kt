package com.ant.tunes.lastfm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest

object LastFmRepository {

    private const val API_KEY    = "e2427f83cfff636cb919ccdc4db1b4c1"
    private const val SECRET     = "8cfbc869e3692a842d07877748c2e9b3"
    private const val BASE_URL   = "https://ws.audioscrobbler.com/"
    private const val PREFS_NAME = "ant_prefs"
    private const val KEY_SESSION = "lastfm_session_key"
    private const val KEY_USER    = "lastfm_username"
    private const val KEY_TOKEN   = "lastfm_token"

    private val api: LastFmApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LastFmApi::class.java)
    }

    // ── AUTH ──
    fun getSessionKey(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SESSION, null)

    fun getUsername(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER, null)

    fun saveSession(context: Context, username: String, sessionKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSION, sessionKey)
            .putString(KEY_USER, username)
            .apply()
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SESSION)
            .remove(KEY_USER)
            .remove(KEY_TOKEN)
            .apply()
    }

    fun isLoggedIn(context: Context) = getSessionKey(context) != null

    // ── GET TOKEN (Step 1 of auth) ──
    suspend fun getToken(): String? = withContext(Dispatchers.IO) {
        try {
            val response = api.getToken(apiKey = API_KEY)
            response.body()?.token
        } catch (e: Exception) {
            Log.e("LastFm", "getToken failed: ${e.message}")
            null
        }
    }

    // ── GET SESSION (Step 3 of auth, after user approves) ──
    suspend fun getSession(token: String): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            try {
                val sig = md5("api_key${API_KEY}methodauth.getSessiontoken${token}${SECRET}")
                val response = api.getSession(
                    token = token,
                    apiKey = API_KEY,
                    apiSig = sig
                )
                val session = response.body()?.session
                if (session?.key != null && session.name != null) {
                    Pair(session.name, session.key)
                } else null
            } catch (e: Exception) {
                Log.e("LastFm", "getSession failed: ${e.message}")
                null
            }
        }

    // ── TOP TRACKS ──
    suspend fun getTopTracks(context: Context): List<LastFmTopTrack> =
        withContext(Dispatchers.IO) {
            val username = getUsername(context) ?: return@withContext emptyList()
            try {
                val response = api.getTopTracks(user = username, apiKey = API_KEY)
                response.body()?.toptracks?.track?.mapNotNull { item ->
                    LastFmTopTrack(
                        name      = item.name ?: return@mapNotNull null,
                        artist    = item.artist?.name ?: return@mapNotNull null,
                        // 🟢 FIXED: Changed `#text` to `text`
                        imageUrl  = item.image?.lastOrNull()?.text ?: "",
                        playCount = item.playcount?.toIntOrNull() ?: 0
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e("LastFm", "getTopTracks failed: ${e.message}")
                emptyList()
            }
        }

    // ── RECENTLY PLAYED ──
    suspend fun getRecentTracks(context: Context): List<LastFmRecentTrack> =
        withContext(Dispatchers.IO) {
            val username = getUsername(context) ?: return@withContext emptyList()
            try {
                val response = api.getRecentTracks(user = username, apiKey = API_KEY)
                response.body()?.recenttracks?.track?.mapNotNull { item ->
                    LastFmRecentTrack(
                        name     = item.name ?: return@mapNotNull null,
                        // 🟢 FIXED: Changed `#text` to `text`
                        artist   = item.artist?.text ?: return@mapNotNull null,
                        imageUrl = item.image?.lastOrNull()?.text ?: ""
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e("LastFm", "getRecentTracks failed: ${e.message}")
                emptyList()
            }
        }

    // ── SIMILAR TRACKS (recommendations) ──
    suspend fun getSimilarTracks(
        track: String,
        artist: String
    ): List<LastFmSimilarTrack> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSimilarTracks(
                track  = track,
                artist = artist,
                apiKey = API_KEY
            )
            response.body()?.similartracks?.track?.mapNotNull { item ->
                LastFmSimilarTrack(
                    name   = item.name ?: return@mapNotNull null,
                    artist = item.artist?.name ?: return@mapNotNull null
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("LastFm", "getSimilarTracks failed: ${e.message}")
            emptyList()
        }
    }

    // ── SCROBBLE ──
    suspend fun scrobble(
        context: Context,
        track: String,
        artist: String
    ) = withContext(Dispatchers.IO) {
        val sessionKey = getSessionKey(context) ?: return@withContext
        try {
            val timestamp = System.currentTimeMillis() / 1000
            val sig = md5(
                "api_key${API_KEY}artist${artist}" +
                        "methodtrack.scrobblesk${sessionKey}" +
                        "timestamp${timestamp}track${track}${SECRET}"
            )
            api.scrobble(
                track     = track,
                artist    = artist,
                timestamp = timestamp,
                apiKey    = API_KEY,
                sessionKey = sessionKey,
                apiSig    = sig
            )
            Log.d("LastFm", "✅ Scrobbled: $track by $artist")
        } catch (e: Exception) {
            Log.e("LastFm", "scrobble failed: ${e.message}")
        }
    }

    suspend fun getUserPlaylists(context: Context): List<LastFmPlaylistItem> =
        withContext(Dispatchers.IO) {
            val username = getUsername(context) ?: return@withContext emptyList()
            try {
                val response = api.getUserPlaylists(
                    user = username,
                    apiKey = API_KEY
                )
                response.body()?.playlists?.playlist ?: emptyList()
            } catch (e: Exception) {
                Log.e("LastFm", "getUserPlaylists failed: ${e.message}")
                emptyList()
            }
        }

    suspend fun getPlaylistTracks(playlistId: String): List<PlaylistTrackItem> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getPlaylistTracks(
                    playlistId = playlistId,
                    apiKey = API_KEY
                )
                response.body()?.playlist?.track ?: emptyList()
            } catch (e: Exception) {
                Log.e("LastFm", "getPlaylistTracks failed: ${e.message}")
                emptyList()
            }
        }

    // ── MD5 HELPER ──
    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
