package com.ant.tunes.resolver

import android.util.Log
import com.ant.tunes.data.Song
import com.ant.tunes.network.RetrofitClient

class SaavnStreamResolver : StreamResolver {
    override suspend fun resolve(song: Song): String? {
        return try {
            // Use song id to get fresh URL
            val query = "${song.title} ${song.artist}"
            val response = RetrofitClient.api.searchSongs(
                query = query, page = 1, limit = 5
            )
            val results = response.body()?.data?.results ?: return null

            // Find best match by id first, then title similarity
            val match = results.firstOrNull { it.id == song.id }
                ?: results.firstOrNull {
                    it.name.equals(song.title, ignoreCase = true) &&
                            it.artists.primary.any { a ->
                                a.name.contains(
                                    song.artist.split(",")[0].trim(),
                                    ignoreCase = true
                                )
                            }
                }
                ?: results.firstOrNull()

            match?.downloadUrl?.lastOrNull()?.url?.also {
                Log.d("SaavnResolver", "✅ Resolved: ${song.title} → $it")
            }
        } catch (e: Exception) {
            Log.e("SaavnResolver", "❌ Failed: ${e.message}")
            null
        }
    }
}
