package com.ant.tunes.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.ant.tunes.data.Song
import com.ant.tunes.resolver.StreamCache
import com.ant.tunes.resolver.StreamResolverFactory

object PlaybackRepository {

    @OptIn(UnstableApi::class)
    suspend fun getFreshStreamUrl(
        context: Context,
        song: Song
    ): String? {

        // 1. Offline — use local file
        if (song.isDownloaded && song.localPath.isNotBlank()) {
            Log.d("PlaybackRepo", "📁 Using local: ${song.title}")
            return song.localPath
        }

        // 2. In-memory stream cache (fresh URL from this session)
        val cached = StreamCache.get(song.id)
        if (cached != null) {
            Log.d("PlaybackRepo", "⚡ Stream cache hit: ${song.title}")
            return cached
        }

        // 3. Check media cache (CacheManager) using verified isCached method
        if (song.streamUrl.isNotBlank() && CacheManager.isCached(context, song.streamUrl)) {
            Log.d("PlaybackRepo", "💾 Media cache hit: ${song.title}")
            return song.streamUrl
        }

        // 4. Resolve fresh URL
        Log.d("PlaybackRepo", "🌐 Resolving: ${song.title}")
        val resolver = StreamResolverFactory.get(song)
        val freshUrl = resolver.resolve(song)

        if (freshUrl != null) {
            StreamCache.put(song.id, freshUrl)
        }

        return freshUrl
    }
}
