package com.ant.tunes.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import java.io.File

@UnstableApi
object CacheManager {
    private var simpleCache: SimpleCache? = null

    // 🟢 INITIALIZE CACHE
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
            // 🟢 FIXED: Default to false
            val isCacheEnabled = prefs.getBoolean("cache_enabled", false)

            val cacheLimit = if (isCacheEnabled) {
                prefs.getInt("cache_limit_mb", 100).toLong() * 1024 * 1024
            } else {
                1024 * 1024
            }

            val cacheDir = File(context.cacheDir, "media_cache")
            val databaseProvider = StandaloneDatabaseProvider(context)
            val evictor = LeastRecentlyUsedCacheEvictor(cacheLimit)

            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    // 🟢 BUG 2 FIX: Only returns true if the song is 100% downloaded (No half-baked chunks)
    // 🟢 BUG FIX UPGRADE: The Forgiving Cache Check
    fun isCached(context: Context, songId: String?): Boolean {
        if (songId == null) return false

        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        // 🟢 FIXED: Default to false
        if (!prefs.getBoolean("cache_enabled", false)) return false

        return try {
            val cache = getCache(context)
            val contentLength = androidx.media3.datasource.cache.ContentMetadata.getContentLength(cache.getContentMetadata(songId))

            if (contentLength.toInt() != androidx.media3.common.C.LENGTH_UNSET && contentLength > 0L) {
                val cachedBytes = cache.getCachedBytes(songId, 0, contentLength)
                // 🔥 Allow a 2% margin of error because ExoPlayer sometimes drops the last 1KB chunk
                cachedBytes >= (contentLength * 0.98).toLong()
            } else {
                // Fallback for streams that don't have a length header (requires at least 100KB)
                cache.getCachedSpans(songId).sumOf { it.length } > 100_000
            }
        } catch (e: Exception) {
            false
        }
    }


    // 🟢 BUG 4 FIX: Delete directly using the permanent song.id
    fun deleteCachedSong(context: Context, songId: String): Boolean {
        return try {
            getCache(context).removeResource(songId)
            true
        } catch (e: Exception) { false }
    }

    fun getCachedSongSize(context: Context, songId: String): Long {
        return try {
            getCache(context).getCachedSpans(songId).sumOf { it.length }
        } catch (e: Exception) { 0L }
    }

    fun formatSize(bytes: Long): String = when {
        bytes > 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes > 1_000     -> "%.0f KB".format(bytes / 1_000.0)
        else              -> "$bytes B"
    }

    // 🟢 ENGINE FIX: Cleaned up. ExoPlayer will now use customCacheKey automatically!
    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro)")

        val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(
            context,
            httpDataSourceFactory
        )

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return DataSource.Factory {
            // 🟢 FIXED: Default to false
            if (prefs.getBoolean("cache_enabled", false)) {
                cacheDataSourceFactory.createDataSource()
            } else {
                defaultDataSourceFactory.createDataSource()
            }
        }
    }

    fun clearCache(context: Context) {
        try {
            simpleCache?.release()
            simpleCache = null
            File(context.cacheDir, "media_cache").deleteRecursively()
            getCache(context)
        } catch (e: Exception) { e.printStackTrace() }
    }
}
