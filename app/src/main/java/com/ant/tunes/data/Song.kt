package com.ant.tunes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SourceType { SAAVN, GAANA, YOUTUBE, LOCAL, LASTFM_IMPORT, UNKNOWN }

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: String = "",

    // ✅ NEVER store expiring stream URLs here
    // Instead store permanent identifiers
    val sourceType: SourceType = SourceType.UNKNOWN,
    val permanentUrl: String = "",   // saavn permanent ID or gaana seokey
    val videoId: String = "",        // YouTube video ID only (no full URL)
    val duration: Long = 0L,
    val album: String = "",

    // Legacy field — kept for backward compatibility during migration
    // Will be IGNORED for playback, used only as fallback
    @Deprecated("Use resolver layer instead")
    val streamUrl: String = "",

    // Offline
    val isDownloaded: Boolean = false,
    val localPath: String = "",

    // Source string for UI badges
    val source: String = ""
) {
    // Derive sourceType from legacy source string during migration
    fun resolvedSourceType(): SourceType = when {
        sourceType != SourceType.UNKNOWN -> sourceType
        source.equals("saavn", true) -> SourceType.SAAVN
        source.equals("gaana", true) -> SourceType.GAANA
        source.equals("youtube", true) -> SourceType.YOUTUBE
        source.equals("local", true) -> SourceType.LOCAL
        else -> SourceType.UNKNOWN
    }

    // Extract YouTube video ID from legacy stream URL
    fun extractedVideoId(): String = when {
        videoId.isNotBlank() -> videoId
        streamUrl.contains("watch?v=") ->
            streamUrl.substringAfter("watch?v=").substringBefore("&")
        streamUrl.contains("youtu.be/") ->
            streamUrl.substringAfter("youtu.be/").substringBefore("?")
        else -> ""
    }
}
