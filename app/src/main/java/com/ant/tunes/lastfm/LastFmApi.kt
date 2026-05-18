package com.ant.tunes.lastfm

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

interface LastFmApi {

    @GET("2.0/")
    suspend fun getUserPlaylists(
        @Query("method") method: String = "user.getPlaylists",
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): Response<LastFmPlaylistsResponse>

    @GET("2.0/")
    suspend fun getPlaylistTracks(
        @Query("method") method: String = "playlist.fetch",
        @Query("playlistid") playlistId: String,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): Response<LastFmPlaylistTracksResponse>
    @GET("2.0/")
    suspend fun getTopTracks(
        @Query("method") method: String = "user.getTopTracks",
        @Query("user") user: String,
        @Query("period") period: String = "1month",
        @Query("limit") limit: Int = 20,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): Response<LastFmTopTracksResponse>

    @GET("2.0/")
    suspend fun getRecentTracks(
        @Query("method") method: String = "user.getRecentTracks",
        @Query("user") user: String,
        @Query("limit") limit: Int = 20,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): Response<LastFmRecentTracksResponse>

    @GET("2.0/")
    suspend fun getSimilarTracks(
        @Query("method") method: String = "track.getSimilar",
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("limit") limit: Int = 10,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): Response<LastFmSimilarTracksResponse>

    @GET("2.0/")
    suspend fun scrobble(
        @Query("method") method: String = "track.scrobble",
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("timestamp") timestamp: Long,
        @Query("api_key") apiKey: String,
        @Query("sk") sessionKey: String,
        @Query("api_sig") apiSig: String,
        @Query("format") format: String = "json"
    ): Response<Any>

    @GET("2.0/")
    suspend fun getToken(
        @Query("method") method: String = "auth.getToken",
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): Response<LastFmTokenResponse>

    @GET("2.0/")
    suspend fun getSession(
        @Query("method") method: String = "auth.getSession",
        @Query("token") token: String,
        @Query("api_key") apiKey: String,
        @Query("api_sig") apiSig: String,
        @Query("format") format: String = "json"
    ): Response<LastFmSessionResponse>
}

// ── RESPONSE MODELS ──
data class LastFmTopTracksResponse(val toptracks: TopTracksBody?)
data class TopTracksBody(val track: List<TopTrackItem>?)
data class TopTrackItem(
    val name: String?,
    val playcount: String?,
    val artist: ArtistItem?,
    val image: List<ImageItem>?
)

data class LastFmRecentTracksResponse(val recenttracks: RecentTracksBody?)
data class RecentTracksBody(val track: List<RecentTrackItem>?)
data class RecentTrackItem(
    val name: String?,
    val artist: ArtistAttr?,
    val image: List<ImageItem>?
)

data class LastFmSimilarTracksResponse(val similartracks: SimilarTracksBody?)
data class SimilarTracksBody(val track: List<SimilarTrackItem>?)
data class SimilarTrackItem(
    val name: String?,
    val artist: ArtistItem?
)

data class LastFmTokenResponse(val token: String?)
data class LastFmSessionResponse(val session: SessionBody?)
data class SessionBody(val name: String?, val key: String?)

data class ArtistItem(val name: String?)

// 🟢 FIXED: Bypassing the Android compiler block
data class ArtistAttr(
    @SerializedName("#text") val text: String?
)

data class ImageItem(
    @SerializedName("#text") val text: String?,
    val size: String?
)

data class LastFmPlaylistsResponse(val playlists: PlaylistsBody?)
data class PlaylistsBody(val playlist: List<LastFmPlaylistItem>?)
data class LastFmPlaylistItem(
    val id: String?,
    val title: String?,
    val size: String?,
    val image: List<ImageItem>?
)

data class LastFmPlaylistTracksResponse(val playlist: PlaylistTracksBody?)
data class PlaylistTracksBody(val track: List<PlaylistTrackItem>?)
data class PlaylistTrackItem(
    val name: String?,
    val creator: String?,
    val image: List<ImageItem>?
)