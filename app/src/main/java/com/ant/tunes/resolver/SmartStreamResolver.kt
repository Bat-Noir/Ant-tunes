package com.ant.tunes.resolver

import android.util.Log
import com.ant.tunes.data.Song

// Tries all sources when sourceType is unknown
class SmartStreamResolver : StreamResolver {
    private val resolvers = listOf(
        SaavnStreamResolver(),
        GaanaStreamResolver(),
        YouTubeStreamResolver()
    )

    override suspend fun resolve(song: Song): String? {
        for (resolver in resolvers) {
            val result = resolver.resolve(song)
            if (!result.isNullOrBlank()) {
                Log.d("SmartResolver", "✅ Resolved ${song.title} via ${resolver::class.simpleName}")
                return result
            }
        }
        Log.e("SmartResolver", "❌ All resolvers failed for ${song.title}")
        return null
    }
}
