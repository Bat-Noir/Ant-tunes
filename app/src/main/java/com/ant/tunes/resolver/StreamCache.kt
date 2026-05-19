package com.ant.tunes.resolver

import android.util.LruCache

object StreamCache {

    // In-memory cache: songId → freshUrl
    // Max 50 entries, entries expire after 25 min
    private data class CacheEntry(
        val url: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val cache = LruCache<String, CacheEntry>(50)
    private const val TTL_MS = 25 * 60 * 1000L // 25 minutes

    fun get(songId: String): String? {
        val entry = cache.get(songId) ?: return null
        val age = System.currentTimeMillis() - entry.timestamp
        return if (age < TTL_MS) entry.url else null.also { cache.remove(songId) }
    }

    fun put(songId: String, url: String) {
        cache.put(songId, CacheEntry(url))
    }

    private fun invalidate(songId: String) = cache.remove(songId)
    fun clear() = cache.evictAll()
}
