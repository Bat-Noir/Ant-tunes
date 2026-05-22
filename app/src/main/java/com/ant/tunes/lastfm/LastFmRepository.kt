package com.ant.tunes.lastfm

import android.content.Context
import android.util.Log
import com.ant.tunes.ui.BrowseCard
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

    suspend fun getTopAlbums(artist: String): List<BrowseCard> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTopAlbums(artist = artist, apiKey = API_KEY)
            response.body()?.topalbums?.album?.mapNotNull { item ->
                BrowseCard(
                    id = item.name ?: "", // Last.fm albums are best identified by name+artist
                    title = item.name ?: return@mapNotNull null,
                    imageUrl = item.image?.lastOrNull()?.text ?: ""
                )
            } ?: emptyList()
        } catch (e: Exception) { emptyList() }
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
    // 🟢 FIXED: Now matches your PlayerViewModel call and actually performs the POST
    suspend fun scrobbleTrack(
        context: Context,
        track: String,
        artist: String
    ) = withContext(Dispatchers.IO) {
        val sessionKey = getSessionKey(context) ?: return@withContext
        try {
            val timestamp = System.currentTimeMillis() / 1000

            // 🟢 CRITICAL: Last.fm requires keys sorted alphabetically for the signature
            // method, api_key, artist, sk, timestamp, track + SECRET
            val params = sortedMapOf(
                "api_key" to API_KEY,
                "artist" to artist,
                "method" to "track.scrobble",
                "sk" to sessionKey,
                "timestamp" to timestamp.toString(),
                "track" to track
            )

            val sigBuilder = StringBuilder()
            params.forEach { (k, v) -> sigBuilder.append(k).append(v) }
            sigBuilder.append(SECRET) // Append secret at the end
            val sig = md5(sigBuilder.toString())

            val response = api.scrobble(
                track = track,
                artist = artist,
                timestamp = timestamp,
                apiKey = API_KEY,
                sessionKey = sessionKey,
                apiSig = sig
            )

            if (response.isSuccessful) {
                Log.d("LastFm", "✅ SUCCESSFULLY SCROBBLED: $track by $artist")
            } else {
                Log.e("LastFm", "❌ Scrobble failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("LastFm", "Scrobble connection error: ${e.message}")
        }
    }


    // 🟢 Fetch user's playlists via XSPF endpoint (no auth needed!)
    // 🟢 UPDATED: Now requires context to inject the Session Key
    // 🟢 FIXED: Removed the Session Key trap and cleaned the username
    suspend fun getUserPlaylistsDirect(context: android.content.Context): List<LastFmPlaylistItem> =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            // Strip "@" just in case the UI accidentally saved it
            val rawUsername = getUsername(context)?.removePrefix("@") ?: return@withContext emptyList()

            try {
                // Public playlists only need the username and API key!
                val url = "https://ws.audioscrobbler.com/2.0/?method=user.getPlaylists" +
                        "&user=${java.net.URLEncoder.encode(rawUsername, "UTF-8")}" +
                        "&api_key=${API_KEY}" +
                        "&format=json"

                android.util.Log.d("LastFm", "Fetching playlists for: $rawUsername")
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("User-Agent", "AntTunes/1.0 (Android)")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val code = conn.responseCode
                if (code == 200) {
                    val resp = conn.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(resp)

                    if (json.has("error")) return@withContext emptyList()

                    val playlistsObj = json.optJSONObject("playlists") ?: return@withContext emptyList()
                    val arr = playlistsObj.optJSONArray("playlist")

                    if (arr != null) {
                        (0 until arr.length()).mapNotNull { i ->
                            val p = arr.getJSONObject(i)
                            LastFmPlaylistItem(
                                id    = p.optString("id").ifBlank { null },
                                title = p.optString("title").ifBlank { "Playlist" },
                                size  = p.optString("size"),
                                image = null
                            )
                        }
                    } else {
                        val single = playlistsObj.optJSONObject("playlist")
                        if (single != null) {
                            listOf(
                                LastFmPlaylistItem(
                                    id    = single.optString("id").ifBlank { null },
                                    title = single.optString("title").ifBlank { "Playlist" },
                                    size  = single.optString("size"),
                                    image = null
                                )
                            )
                        } else emptyList()
                    }
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    // 🟢 Fetch tracks for a playlist via XSPF
    suspend fun getPlaylistTracksDirect(playlistId: String): List<PlaylistTrackItem> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://ws.audioscrobbler.com/2.0/?method=playlist.fetch&playlistid=${playlistId}&api_key=${API_KEY}&format=json"
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("User-Agent", "AntTunes/1.0")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(resp)
                    val playlist = json.optJSONObject("playlist")
                        ?: return@withContext emptyList()
                    val trackArr = playlist.optJSONArray("track")
                        ?: return@withContext emptyList()

                    (0 until trackArr.length()).mapNotNull { i ->
                        val t = trackArr.getJSONObject(i)
                        PlaylistTrackItem(
                            name    = t.optString("title").ifBlank { t.optString("name") },
                            creator = t.optString("creator").ifBlank { t.optString("artist") },
                            image   = null
                        )
                    }
                } else emptyList()
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
