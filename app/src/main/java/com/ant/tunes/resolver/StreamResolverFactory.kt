package com.ant.tunes.resolver

import com.ant.tunes.data.Song
import com.ant.tunes.data.SourceType

object StreamResolverFactory {
    fun get(song: Song): StreamResolver = when (song.resolvedSourceType()) {
        SourceType.SAAVN         -> SaavnStreamResolver()
        SourceType.GAANA         -> GaanaStreamResolver()
        SourceType.YOUTUBE       -> YouTubeStreamResolver()
        SourceType.LOCAL         -> LocalStreamResolver()
        SourceType.LASTFM_IMPORT -> SaavnStreamResolver() // resolve via Saavn
        SourceType.UNKNOWN       -> SmartStreamResolver() // tries all 3
    }
}
