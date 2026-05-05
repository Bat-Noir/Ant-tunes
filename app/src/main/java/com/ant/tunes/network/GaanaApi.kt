package com.ant.tunes.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GaanaApi {

    @GET("api/search")
    suspend fun searchSongs(
        @Query("q") query: String,
        @Query("type") type: String = "song",
        @Query("limit") limit: Int = 10
    ): Response<GaanaSearchResponse>

    @GET("api/stream")
    suspend fun getStreamUrl(
        @Query("seokey") seokey: String
    ): Response<GaanaStreamResponse>
}