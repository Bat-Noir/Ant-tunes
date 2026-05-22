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

// ═══════════════════════════════════════
// 🟢 ALBUM & ARTIST SEARCH MODELS
// ═══════════════════════════════════════

data class AlbumSearchResponse(
    val success: Boolean,
    val data: AlbumSearchData
)

data class AlbumSearchData(
    val results: List<AlbumDto>
)

data class AlbumDto(
    val id: String,
    val name: String,
    val description: String?,
    val year: String?,
    val image: List<Image> // Reuses your existing Image class!
)

data class ArtistSearchResponse(
    val success: Boolean,
    val data: ArtistSearchData
)

data class ArtistSearchData(
    val results: List<ArtistDto>
)

data class ArtistDto(
    val id: String,
    val name: String,
    val role: String?,
    val image: List<Image> // Reuses your existing Image class!
)

// ═══════════════════════════════════════
// 🟢 ALBUM & ARTIST DETAILS MODELS (THE MISSING PIECE!)
// ═══════════════════════════════════════

data class DetailsResponse(
    val success: Boolean,
    val data: DetailsData?
)

data class DetailsData(
    val id: String?,
    val name: String?,
    val songs: List<SongDto>?,      // Albums put their tracks here!
    val topSongs: List<SongDto>?    // Artists put their tracks here!
)
