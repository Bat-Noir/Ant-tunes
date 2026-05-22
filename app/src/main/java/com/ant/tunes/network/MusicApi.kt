package com.ant.tunes.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApi {

    @GET("api/search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 10
    ): Response<SearchResponse>

    @GET("api/search/albums")
    suspend fun searchAlbums(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<AlbumSearchResponse>

    @GET("api/search/artists")
    suspend fun searchArtists(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ArtistSearchResponse>

    // 🟢 FIXED: Now returns DetailsResponse so it can read 'songs' and 'topSongs'!
    @GET("api/albums")
    suspend fun getAlbumDetails(
        @Query("id") id: String
    ): Response<DetailsResponse>

    // 🟢 FIXED: Now returns DetailsResponse!
    @GET("api/artists")
    suspend fun getArtistDetails(
        @Query("id") id: String,
        @Query("limit") limit: Int = 100,      // Forces max tracks
        @Query("songCount") songCount: Int = 100 // Fallback for certain API wrappers
    ): Response<DetailsResponse>
}
