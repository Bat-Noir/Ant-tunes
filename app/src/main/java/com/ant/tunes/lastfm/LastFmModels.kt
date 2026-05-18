package com.ant.tunes.lastfm

data class LastFmTopTrack(
    val name: String,
    val artist: String,
    val imageUrl: String,
    val playCount: Int
)

data class LastFmRecentTrack(
    val name: String,
    val artist: String,
    val imageUrl: String
)

data class LastFmSimilarTrack(
    val name: String,
    val artist: String
)