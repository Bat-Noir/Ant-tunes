package com.ant.tunes.resolver

import com.ant.tunes.data.Song

class LocalStreamResolver : StreamResolver {
    override suspend fun resolve(song: Song): String? {
        return if (song.localPath.isNotBlank()) song.localPath
        else if (song.streamUrl.isNotBlank()) song.streamUrl
        else null
    }
}
