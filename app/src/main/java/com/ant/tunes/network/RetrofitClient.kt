package com.ant.tunes.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 🎵 Saavn API
    private const val BASE_URL =
        "https://jiosaavn-api.ant-tunes.workers.dev/"

    // 🎵 Gaana API
    private const val GAANA_BASE_URL =
        "https://ant-gaana-backend.vercel.app/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-api-key", "ant_tunes_123_secure")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .build()

    // 🎵 Saavn API
    val api: MusicApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicApi::class.java)
    }

    // 🎵 Gaana API
    val gaanaApi: GaanaApi by lazy {
        Retrofit.Builder()
            .baseUrl(GAANA_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GaanaApi::class.java)
    }
}