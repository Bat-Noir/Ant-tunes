package com.ant.tunes.ytmusic

import com.ant.tunes.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object YoutubeSyncEngine {

    private fun findObjectsWithKey(json: Any?, targetKey: String): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        when (json) {
            is JSONObject -> {
                if (json.has(targetKey)) {
                    val obj = json.optJSONObject(targetKey)
                    if (obj != null) results.add(obj)
                }
                val keys = json.keys()
                while (keys.hasNext()) {
                    results.addAll(findObjectsWithKey(json.opt(keys.next()), targetKey))
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    results.addAll(findObjectsWithKey(json.opt(i), targetKey))
                }
            }
        }
        return results
    }

    private suspend fun makeRequestWithPayload(payload: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = YoutubeAuthManager.getAuthHeader()
                val cookies = YoutubeAuthManager.getRawCookies()
                if (authHeader == null || cookies.isEmpty()) return@withContext null

                val url = URL("https://music.youtube.com/youtubei/v1/browse?alt=json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Authorization", authHeader)
                conn.setRequestProperty("Cookie", cookies)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Origin", "https://music.youtube.com")
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")

                OutputStreamWriter(conn.outputStream).use { it.write(payload) }

                if (conn.responseCode == 200) {
                    JSONObject(conn.inputStream.bufferedReader().readText())
                } else null
            } catch (e: Exception) { null }
        }
    }

    suspend fun fetchTracksFromPlaylist(playlistId: String): List<Song> {
        return withContext(Dispatchers.IO) {
            val songs = mutableListOf<Song>()
            val browseId = if (!playlistId.startsWith("VL") && !playlistId.startsWith("FE")) "VL$playlistId" else playlistId

            var continuationToken: String? = null
            var pagesFetched = 0

            while (pagesFetched == 0 || continuationToken != null) {
                if (pagesFetched >= 30) break // Safety switch: stops at ~3000 songs

                val payload = if (pagesFetched == 0) {
                    """{"context": {"client": {"clientName": "WEB_REMIX", "clientVersion": "1.20230522.01.00"}}, "browseId": "$browseId"}"""
                } else {
                    """{"context": {"client": {"clientName": "WEB_REMIX", "clientVersion": "1.20230522.01.00"}}, "continuation": "$continuationToken"}"""
                }

                val json = makeRequestWithPayload(payload) ?: break

                val trackNodes = mutableListOf<org.json.JSONObject>()
                var searchArea: Any? = null

                if (pagesFetched == 0) {
                    val shelf = findObjectsWithKey(json, "musicPlaylistShelfRenderer").firstOrNull()
                    if (shelf != null) {
                        trackNodes.addAll(findObjectsWithKey(shelf, "musicResponsiveListItemRenderer"))
                        searchArea = shelf
                    }
                } else {
                    val action = findObjectsWithKey(json, "appendContinuationItemsAction").firstOrNull()
                        ?: findObjectsWithKey(json, "continuationContents").firstOrNull()

                    if (action != null) {
                        trackNodes.addAll(findObjectsWithKey(action, "musicResponsiveListItemRenderer"))
                        searchArea = action
                    }
                }

                if (trackNodes.isEmpty()) break

                for (node in trackNodes) {
                    val str = node.toString()
                    val videoIdMatch = Regex("\"videoId\":\"([a-zA-Z0-9_-]{11})\"").find(str)
                    val videoId = videoIdMatch?.groupValues?.get(1) ?: ""

                    if (videoId.isNotBlank()) {
                        var title = "Unknown"
                        var artist = "Unknown"

                        val flexColumns = node.optJSONArray("flexColumns")
                        if (flexColumns != null) {
                            val col1 = flexColumns.optJSONObject(0)?.toString() ?: ""
                            val titleMatch = Regex("\"text\":\"(.*?)\"").find(col1)
                            title = titleMatch?.groupValues?.get(1) ?: "Unknown"

                            val col2 = flexColumns.optJSONObject(1)?.toString() ?: ""
                            val artistMatch = Regex("\"text\":\"(.*?)\"").find(col2)
                            artist = artistMatch?.groupValues?.get(1) ?: "Unknown"
                        }

                        val thumbnails = findObjectsWithKey(node, "musicThumbnailRenderer").firstOrNull()?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        var bestThumb = thumbnails?.optJSONObject(thumbnails.length() - 1)?.optString("url", "") ?: ""

                        // 🟢 FORCE HIGH-RES: Strip YouTube's low-quality limiters and demand 1000x1000 covers!
                        if (bestThumb.contains("=w")) {
                            bestThumb = bestThumb.replace(Regex("=w\\d+-h\\d+.*"), "=w1000-h1000")
                        } else if (bestThumb.contains("hqdefault.jpg")) {
                            bestThumb = bestThumb.replace("hqdefault.jpg", "maxresdefault.jpg")
                        }

                        songs.add(
                            Song(
                                id = videoId,
                                videoId = videoId,
                                title = title,
                                artist = artist,
                                albumArt = bestThumb,
                                streamUrl = "https://www.youtube.com/watch?v=$videoId",
                                permanentUrl = "https://www.youtube.com/watch?v=$videoId",
                                source = "youtube",
                                sourceType = com.ant.tunes.data.SourceType.YOUTUBE
                            )
                        )
                    }
                }

                val cmd = findObjectsWithKey(searchArea, "continuationCommand").firstOrNull()
                val ncd = findObjectsWithKey(searchArea, "nextContinuationData").firstOrNull()

                val token1 = cmd?.optString("token", "")
                val token2 = ncd?.optString("continuation", "")

                continuationToken = if (!token1.isNullOrBlank()) token1 else if (!token2.isNullOrBlank()) token2 else null

                pagesFetched++
            }

            songs.distinctBy { it.id }
        }
    }

    suspend fun fetchLibraryItems(category: String): List<com.ant.tunes.ui.BrowseCard> {
        return withContext(Dispatchers.IO) {
            val items = mutableListOf<com.ant.tunes.ui.BrowseCard>()

            val idsToTry = when(category) {
                "albums" -> listOf("FEmusic_liked_albums", "FEmusic_library_corpus_albums")
                "artists" -> listOf("FEmusic_liked_artists", "FEmusic_library_corpus_track_artists")
                "playlists" -> listOf("FEmusic_liked_playlists")
                else -> listOf(category)
            }

            for (id in idsToTry) {
                val payload = """{"context": {"client": {"clientName": "WEB_REMIX", "clientVersion": "1.20230522.01.00"}}, "browseId": "$id"}"""
                val json = makeRequestWithPayload(payload) ?: continue

                val grids = findObjectsWithKey(json, "musicTwoRowItemRenderer")
                val lists = findObjectsWithKey(json, "musicResponsiveListItemRenderer")

                for (node in (grids + lists)) {
                    val browseEndpoints = findObjectsWithKey(node, "browseEndpoint")
                    val endpointId = browseEndpoints.firstOrNull()?.optString("browseId", "") ?: ""

                    if (endpointId.isNotBlank()) {
                        var title = "Unknown"
                        val str = node.toString()

                        if (node.has("title")) {
                            val titleMatch = Regex("\"text\":\"(.*?)\"").find(node.optJSONObject("title").toString())
                            title = titleMatch?.groupValues?.get(1) ?: "Unknown"
                        }
                        if (title == "Unknown") {
                            val titleMatch = Regex("\"text\":\"(.*?)\"").find(str)
                            title = titleMatch?.groupValues?.get(1) ?: "Unknown"
                        }

                        val thumbnails = findObjectsWithKey(node, "musicThumbnailRenderer").firstOrNull()?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        var imageUrl = thumbnails?.optJSONObject(thumbnails.length() - 1)?.optString("url", "") ?: ""

                        // 🟢 FORCE HIGH-RES FOR LIBRARY COVERS TOO
                        if (imageUrl.contains("=w")) {
                            imageUrl = imageUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w1000-h1000")
                        } else if (imageUrl.contains("hqdefault.jpg")) {
                            imageUrl = imageUrl.replace("hqdefault.jpg", "maxresdefault.jpg")
                        }

                        if (!title.contains("New playlist", true) && !title.contains("Shuffle all", true)) {
                            items.add(com.ant.tunes.ui.BrowseCard(id = endpointId, title = title, imageUrl = imageUrl))
                        }
                    }
                }
                if (items.isNotEmpty()) break
            }
            items.distinctBy { it.id }
        }
    }

    suspend fun searchVideoIdForTrack(query: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // The result is already a Song object, so we just grab its videoId!
                val results = com.ant.tunes.NewPipeHelper.search(query)
                results.firstOrNull()?.videoId
            } catch (e: Exception) { null }
        }
    }

    // 🟢 NEW: Scrapes EVERY dynamic carousel, and scrolls to the bottom of the feed!
    suspend fun fetchDynamicHomeCarousels(): List<YtmHomeShelf> {
        return withContext(Dispatchers.IO) {
            val carousels = mutableListOf<YtmHomeShelf>()
            var continuationToken: String? = null
            var pagesFetched = 0

            while (pagesFetched == 0 || continuationToken != null) {
                if (pagesFetched >= 10) break // Safety switch: stops after 10 pages of scrolling

                val payload = if (pagesFetched == 0) {
                    """{"context": {"client": {"clientName": "WEB_REMIX", "clientVersion": "1.20230522.01.00"}}, "browseId": "FEmusic_home"}"""
                } else {
                    """{"context": {"client": {"clientName": "WEB_REMIX", "clientVersion": "1.20230522.01.00"}}, "continuation": "$continuationToken"}"""
                }

                val json = makeRequestWithPayload(payload) ?: break

                val shelves = findObjectsWithKey(json, "musicCarouselShelfRenderer")

                for (shelf in shelves) {
                    var shelfTitle = "Recommended"
                    val header = findObjectsWithKey(shelf, "musicCarouselShelfBasicHeaderRenderer").firstOrNull()
                    if (header != null) {
                        val titleMatch = Regex("\"text\":\"(.*?)\"").find(header.toString())
                        if (titleMatch != null) shelfTitle = titleMatch.groupValues[1]
                    }

                    val albums = mutableListOf<com.ant.tunes.ui.BrowseCard>()
                    val songs = mutableListOf<Song>()

                    // 1. SCAN FOR ALBUMS & MIXES
                    val grids = findObjectsWithKey(shelf, "musicTwoRowItemRenderer")
                    for (node in grids) {
                        var title = "Unknown"
                        val titleMatch = Regex("\"text\":\"(.*?)\"").find(node.optJSONObject("title")?.toString() ?: "")
                        if (titleMatch != null) title = titleMatch.groupValues[1]

                        val thumbnails = findObjectsWithKey(node, "musicThumbnailRenderer").firstOrNull()?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        var imageUrl = thumbnails?.optJSONObject(thumbnails.length() - 1)?.optString("url", "") ?: ""

                        if (imageUrl.contains("=w")) imageUrl = imageUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w1000-h1000")
                        else if (imageUrl.contains("hqdefault.jpg")) imageUrl = imageUrl.replace("hqdefault.jpg", "maxresdefault.jpg")

                        val browseId = findObjectsWithKey(node, "browseEndpoint").firstOrNull()?.optString("browseId")
                        val watchIdMatch = Regex("\"videoId\":\"([a-zA-Z0-9_-]{11})\"").find(node.toString())

                        if (browseId != null && browseId.isNotBlank()) {
                            albums.add(com.ant.tunes.ui.BrowseCard(id = browseId, title = title, imageUrl = imageUrl))
                        } else if (watchIdMatch != null) {
                            val videoId = watchIdMatch.groupValues[1]
                            songs.add(Song(id=videoId, videoId=videoId, title=title, artist="YouTube Music", albumArt=imageUrl, streamUrl="https://www.youtube.com/watch?v=$videoId", permanentUrl="https://www.youtube.com/watch?v=$videoId", source="youtube", sourceType=com.ant.tunes.data.SourceType.YOUTUBE))
                        }
                    }

                    // 2. SCAN FOR DIRECT PLAYABLE SONGS
                    val lists = findObjectsWithKey(shelf, "musicResponsiveListItemRenderer")
                    for (node in lists) {
                        val videoIdMatch = Regex("\"videoId\":\"([a-zA-Z0-9_-]{11})\"").find(node.toString())
                        val videoId = videoIdMatch?.groupValues?.get(1)

                        if (videoId != null && videoId.isNotBlank()) {
                            var title = "Unknown"
                            var artist = "Unknown"
                            val flexColumns = node.optJSONArray("flexColumns")
                            if (flexColumns != null) {
                                val col1 = flexColumns.optJSONObject(0)?.toString() ?: ""
                                val titleMatchStr = Regex("\"text\":\"(.*?)\"").find(col1)
                                title = titleMatchStr?.groupValues?.get(1) ?: "Unknown"

                                val col2 = flexColumns.optJSONObject(1)?.toString() ?: ""
                                val artistMatch = Regex("\"text\":\"(.*?)\"").find(col2)
                                artist = artistMatch?.groupValues?.get(1) ?: "Unknown"
                            }

                            val thumbnails = findObjectsWithKey(node, "musicThumbnailRenderer").firstOrNull()?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            var bestThumb = thumbnails?.optJSONObject(thumbnails.length() - 1)?.optString("url", "") ?: ""

                            if (bestThumb.contains("=w")) bestThumb = bestThumb.replace(Regex("=w\\d+-h\\d+.*"), "=w1000-h1000")
                            else if (bestThumb.contains("hqdefault.jpg")) bestThumb = bestThumb.replace("hqdefault.jpg", "maxresdefault.jpg")

                            songs.add(Song(id=videoId, videoId=videoId, title=title, artist=artist, albumArt=bestThumb, streamUrl="https://www.youtube.com/watch?v=$videoId", permanentUrl="https://www.youtube.com/watch?v=$videoId", source="youtube", sourceType=com.ant.tunes.data.SourceType.YOUTUBE))
                        }
                    }

                    if (albums.isNotEmpty() || songs.isNotEmpty()) {
                        carousels.add(YtmHomeShelf(title = shelfTitle, albums = albums.distinctBy { it.id }, songs = songs.distinctBy { it.id }))
                    }
                }

                // 🟢 FETCH THE NEXT PAGE TOKEN
                val cmd = findObjectsWithKey(json, "continuationCommand").firstOrNull()
                val ncd = findObjectsWithKey(json, "nextContinuationData").firstOrNull()
                val token1 = cmd?.optString("token", "")
                val token2 = ncd?.optString("continuation", "")
                continuationToken = if (!token1.isNullOrBlank()) token1 else if (!token2.isNullOrBlank()) token2 else null

                pagesFetched++
            }
            carousels
        }
    }
}

// 🟢 NEW: Data Class to hold the complex feed structures
data class YtmHomeShelf(
    val title: String,
    val albums: List<com.ant.tunes.ui.BrowseCard>,
    val songs: List<Song>
)
