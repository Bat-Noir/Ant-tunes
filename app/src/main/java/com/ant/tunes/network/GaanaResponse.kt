package com.ant.tunes.network

data class GaanaSearchResponse(
    val count: Int,
    val results: List<GaanaSong>
)

data class GaanaSongResponse(
    val title: String?,
    val artist: String?,
    val audio_url: String?,
    val thumb: String?,
    val status: Boolean,
    val duration: String? = ""
)

data class GaanaSong(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val seokey: String,
    val thumb: String,
    val language: String = ""
)

data class GaanaStreamResponse(
    val audio_url: String?,
    val available: Boolean,
    val title: String?,
    val status: Boolean
)