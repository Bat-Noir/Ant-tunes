package com.ant.tunes.network

import com.google.gson.annotations.SerializedName

data class GaanaSearchResponse(
    val count: Int,
    val results: List<GaanaSong>
)

data class GaanaSongResponse(
    val title: String?,
    val artist: String?,

    // 🟢 Bulletproof Aliases: Catch the link no matter what they name it!
    @SerializedName(value = "audio_url", alternate = ["link", "stream_url", "hls_url", "stream"])
    val audio_url: String?,

    val thumb: String?,
    val status: Boolean?, // 🟢 FIXED: Made nullable so Gson doesn't silently default to false!

    val duration: String? = "",

    @SerializedName(value = "album_title", alternate = ["album"])
    val album_title: String? = null
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
    @SerializedName(value = "audio_url", alternate = ["link", "stream_url", "hls_url", "stream"])
    val audio_url: String?,

    val available: Boolean?,
    val title: String?,
    val status: Boolean?
)
