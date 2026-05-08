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

    // 🟢 ADD THIS: Checks if the song is actually on the disk
    // 🟢 UPDATED: Smarter check to ensure the song is actually cached
    // 🟢 1. THE UI FIX: Forces the UI to hide if the user turned the setting off!
    fun isCached(context: android.content.Context, url: String?): Boolean {
        if (url == null) return false

        // 🔥 Read the settings toggle first!
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("cache_enabled", true)) {
            return false // Instantly hides the UI if disabled
        }

        return try {
            val cache = getCache(context)
            val cleanKey = url.substringBefore("?")
            cache.getCachedSpans(cleanKey).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }



    // 🟢 INITIALIZE CACHE
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
            val isCacheEnabled = prefs.getBoolean("cache_enabled", true)

            // Get user limit from settings (default 500MB)
            val cacheLimit = if (isCacheEnabled) {
                prefs.getInt("cache_limit_mb", 500).toLong() * 1024 * 1024
            } else {
                1024 * 1024 // Minimal 1MB if disabled (needed for Media3 buffer)
            }

            val cacheDir = File(context.cacheDir, "media_cache")
            val databaseProvider = StandaloneDatabaseProvider(context)
            val evictor = LeastRecentlyUsedCacheEvictor(cacheLimit)

            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    // 🟢 CREATE CACHE DATASOURCE FACTORY
    // 🟢 2. THE ENGINE FIX: Direct HTTP Fetcher to bypass Cloudflare
    // 🟢 CREATE CACHE DATASOURCE FACTORY
    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)

        // 1. The Stealth Fetcher (Bypasses Cloudflare)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")

        // 2. The Standard Downloader
        val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(
            context,
            httpDataSourceFactory
        )

        // 3. The Cache Key Stripper (Fixes dynamic Saavn URLs)
        val cacheKeyFactory = androidx.media3.datasource.cache.CacheKeyFactory { dataSpec ->
            dataSpec.uri.toString().substringBefore("?")
        }

        // 4. The Cache Engine
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(getCache(context))
            .setCacheKeyFactory(cacheKeyFactory)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // 🔥 THE ULTIMATE FIX: The Dynamic Wrapper!
        // ExoPlayer calls this block EVERY SINGLE TIME a new song starts.
        // It reads the live Settings instantly—no app restart required!
        return DataSource.Factory {
            val isCacheEnabled = prefs.getBoolean("cache_enabled", true)
            if (isCacheEnabled) {
                cacheDataSourceFactory.createDataSource() // Caches the song!
            } else {
                defaultDataSourceFactory.createDataSource() // Just streams it!
            }
        }
    }




    // 🟢 CLEAR CACHE (For your Settings button)
    fun clearCache(context: Context) {
        try {
            simpleCache?.release()
            simpleCache = null
            File(context.cacheDir, "media_cache").deleteRecursively()
            getCache(context) // Re-init
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
