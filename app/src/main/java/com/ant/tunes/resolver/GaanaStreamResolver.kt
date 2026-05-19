package com.ant.tunes.resolver

import android.util.Log
import com.ant.tunes.data.Song
import com.ant.tunes.network.RetrofitClient

class GaanaStreamResolver : StreamResolver {
    override suspend fun resolve(song: Song): String? {
        return try {
            // Try with permanentUrl (seokey) first
            if (song.permanentUrl.isNotBlank()) {
                val detail = RetrofitClient.gaanaApi.getSong(song.permanentUrl)
                val body = detail.body()
                if (body?.status == true && body.audio_url?.isNotBlank() == true) {
                    Log.d("GaanaResolver", "✅ Resolved via seokey: ${song.title}")
                    return body.audio_url
                }
            }

            // Fallback: search
            val response = RetrofitClient.gaanaApi.searchSongs(
                query = "${song.title} ${song.artist}",
                type = "song",
                limit = 5
            )
            val songs = response.body()?.results ?: return null
            val match = songs.firstOrNull { it.id == song.id }
                ?: songs.firstOrNull {
                    it.title.contains(song.title, ignoreCase = true)
                }
                ?: songs.firstOrNull()

            match?.let { s ->
                val detail = RetrofitClient.gaanaApi.getSong(s.seokey)
                detail.body()?.audio_url
            }?.also {
                Log.d("GaanaResolver", "✅ Resolved via search: ${song.title}")
            }
        } catch (e: Exception) {
            Log.e("GaanaResolver", "❌ Failed: ${e.message}")
            null
        }
    }
}
