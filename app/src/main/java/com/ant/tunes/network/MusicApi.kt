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

}