package com.ant.tunes.resolver

import android.util.Log
import com.ant.tunes.data.Song
import com.ant.tunes.network.RetrofitClient

class GaanaStreamResolver : StreamResolver {
    override suspend fun resolve(song: Song): String? {
        return try {
            // Try with permanentUrl (seokey) first
            if (song.permanentUrl.isNotBlank()) {
                // 🟢 1. Hit the dedicated STREAM endpoint first!
                val streamDetail = RetrofitClient.gaanaApi.getStreamUrl(song.permanentUrl)
                var audioUrl = streamDetail.body()?.audio_url

                // 🟢 2. Fallback to the metadata endpoint just in case
                if (audioUrl.isNullOrBlank()) {
                    val detail = RetrofitClient.gaanaApi.getSong(song.permanentUrl)
                    audioUrl = detail.body()?.audio_url
                }

                // 🟢 NO MORE STATUS TRAP. Just check if we got the link!
                if (!audioUrl.isNullOrBlank()) {
                    Log.d("GaanaResolver", "✅ Resolved via seokey: ${song.title}")
                    return audioUrl
                }
            }

            // Fallback: search
            val response = RetrofitClient.gaanaApi.searchSongs(
                query = "${song.title} ${song.artist}",
                type = "song",
                limit = 5
            )
            val songs = response.body()?.results ?: return null

            // Note: We leave this filter alone because it's just a fallback for broken links
            val match = songs.firstOrNull { it.id == song.id }
                ?: songs.firstOrNull {
                    it.title.contains(song.title, ignoreCase = true)
                }
                ?: songs.firstOrNull()

            match?.let { s ->
                // 🟢 Hit the stream endpoint here too!
                val streamDetail = RetrofitClient.gaanaApi.getStreamUrl(s.seokey)
                var matchedUrl = streamDetail.body()?.audio_url

                if (matchedUrl.isNullOrBlank()) {
                    val detail = RetrofitClient.gaanaApi.getSong(s.seokey)
                    matchedUrl = detail.body()?.audio_url
                }
                matchedUrl
            }?.also {
                Log.d("GaanaResolver", "✅ Resolved via search: ${song.title}")
            }
        } catch (e: Exception) {
            Log.e("GaanaResolver", "❌ Failed: ${e.message}")
            null
        }
    }
}
