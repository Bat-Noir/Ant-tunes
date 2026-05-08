package com.ant.tunes.network

data class SearchResponse(
    val success: Boolean,
    val data: Data
)

data class Data(
    val results: List<SongDto>
)

data class SongDto(
    val id: String,
    val name: String,
    val url: String,
    val image: List<Image>,
    val downloadUrl: List<DownloadUrl>,
    val artists: Artists,
    val album: Album? = null // 🔥 Catch the album!
)


data class Image(
    val quality: String,
    val url: String
)

data class DownloadUrl(
    val quality: String,
    val url: String
)

data class Artists(
    val primary: List<Artist>
)

data class Artist(
    val name: String
)

// 🔥 New class to parse the Saavn album object
data class Album(
    val id: String?,
    val name: String?,
    val url: String?
)
