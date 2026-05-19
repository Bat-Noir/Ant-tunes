package com.ant.tunes.resolver

import android.util.Log
import com.ant.tunes.NewPipeHelper
import com.ant.tunes.data.Song

class YouTubeStreamResolver : StreamResolver {
    override suspend fun resolve(song: Song): String? {
        return try {
            val videoId = song.extractedVideoId()

            // If we have a video ID, resolve directly
            if (videoId.isNotBlank()) {
                val url = "https://www.youtube.com/watch?v=$videoId"
                val audio = NewPipeHelper.getAudioUrl(url)
                if (audio != null) {
                    Log.d("YTResolver", "✅ Resolved via videoId: $videoId")
                    return audio
                }
            }

            // Fallback: search YouTube
            val searchResults = NewPipeHelper.search("${song.title} ${song.artist}")
            val best = searchResults.firstOrNull() ?: return null
            NewPipeHelper.getAudioUrl(best.streamUrl)?.also {
                Log.d("YTResolver", "✅ Resolved via search: ${song.title}")
            }
        } catch (e: Exception) {
            Log.e("YTResolver", "❌ Failed: ${e.message}")
            null
        }
    }
}
