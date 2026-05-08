package com.ant.tunes

import android.util.Log
import com.ant.tunes.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.TimeUnit

private const val TAG = "NewPipeHelper"

class NewPipeDownloader : Downloader() {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val reqBuilder = okhttp3.Request.Builder()
            .url(request.url())
            // ✅ Android YouTube app client — bypasses bot detection
            .header("User-Agent",
                "com.google.android.youtube/19.09.37 " +
                        "(Linux; U; Android 11; en_US; Pixel 4 XL; " +
                        "Build/RQ3A.210805.001+TV; +https://www.google.com/bot.html) " +
                        "gzip"
            )
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Content-Type", "application/json")
            .header("X-YouTube-Client-Name", "3")         // ✅ 3 = ANDROID
            .header("X-YouTube-Client-Version", "19.09.37")
            .header("X-Goog-Api-Format-Version", "2")

        // Let NewPipe add its own headers on top
        request.headers().forEach { (key, values) ->
            values.forEach { value -> reqBuilder.addHeader(key, value) }
        }

        if (request.httpMethod() == "POST") {
            val bodyBytes = request.dataToSend() ?: ByteArray(0)
            reqBuilder.post(
                bodyBytes.toRequestBody("application/json".toMediaType())
            )
        } else {
            reqBuilder.get()
        }

        val response = client.newCall(reqBuilder.build()).execute()
        val bodyStr = response.body?.string() ?: ""

        // 🔍 Debug: log if still getting HTML
        if (bodyStr.trimStart().startsWith("<")) {
            Log.e(TAG, "❌ Still got HTML! URL: ${request.url()}")
        }

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            bodyStr,
            response.request.url.toString()
        )
    }
}

object NewPipeHelper {

    @Volatile private var initialized = false

    fun init() {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    NewPipe.init(NewPipeDownloader())
                    initialized = true
                    Log.d(TAG, "✅ NewPipe initialized")
                }
            }
        }
    }

    suspend fun searchNextPage(query: String, offset: Int): List<Song> =
        withContext(Dispatchers.IO) {
            try {
                init()
                val extractor = ServiceList.YouTube.getSearchExtractor(
                    query,
                    listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
                    ""
                )
                extractor.fetchPage()

                // get next page if available
                val nextPage = extractor.initialPage.nextPage
                if (nextPage != null) {
                    val nextPageResult = extractor.getPage(nextPage)
                    nextPageResult.items
                        .filterIsInstance<StreamInfoItem>()
                        .drop(offset.coerceAtLeast(0))
                        .take(10)
                        .mapNotNull { item ->
                            try {
                                Song(
                                    id = item.url,
                                    title = item.name ?: "Unknown",
                                    artist = item.uploaderName ?: "Unknown",
                                    albumArt = item.thumbnails.firstOrNull()?.url ?: "",
                                    streamUrl = item.url,
                                    album = "YouTube",
                                    source = "youtube"
                                )
                            } catch (e: Exception) { null }
                        }
                } else emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            init()

            val extractor = ServiceList.YouTube.getSearchExtractor(
                query,
                listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
                ""
            )
            extractor.fetchPage()

            val items = extractor.initialPage.items
            Log.d(TAG, "🔍 '$query' → ${items.size} items total")

            val streams = items.filterIsInstance<StreamInfoItem>()
            Log.d(TAG, "🎵 StreamInfoItems: ${streams.size}")

            streams.take(10).mapNotNull { item ->
                try {
                    Song(
                        id        = item.url,
                        title     = item.name ?: "Unknown",
                        artist    = item.uploaderName ?: "Unknown",
                        albumArt  = item.thumbnails.firstOrNull()?.url ?: "",
                        streamUrl = item.url,
                        album     = "YouTube",
                        source    = "youtube"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Map failed: ${e.message}")
                    null
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Search failed: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAudioUrl(videoUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            init()
            val extractor = ServiceList.YouTube.getStreamExtractor(videoUrl)
            extractor.fetchPage()

            val best = extractor.audioStreams.maxByOrNull { it.averageBitrate }
            Log.d(TAG, "✅ Audio: ${best?.averageBitrate}kbps")
            best?.content

        } catch (e: Exception) {
            Log.e(TAG, "❌ getAudioUrl failed: ${e.message}")
            null
        }
    }
}