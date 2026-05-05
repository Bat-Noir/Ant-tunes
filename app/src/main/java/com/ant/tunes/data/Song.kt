package com.ant.tunes.data

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: String,
    val streamUrl: String,

    // 🔥 NEW
    val duration: Long = 0L,
    val album: String = "",
    val source: String = "saavn",

    // 🔥 OFFLINE
    val isDownloaded: Boolean = false,
    val localPath: String = ""
)