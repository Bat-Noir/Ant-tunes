package com.ant.tunes.resolver

import com.ant.tunes.data.Song

interface StreamResolver {
    suspend fun resolve(song: Song): String?
}
