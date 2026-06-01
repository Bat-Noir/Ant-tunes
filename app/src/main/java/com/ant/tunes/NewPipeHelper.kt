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
import org.schabi.newpipe.extractor.localization.Localization // 🟢 CRITICAL FOR v0.26.2
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.TimeUnit

private const val TAG = "ANT_DEBUG_YOUTUBE"

class NewPipeDownloader : Downloader() {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val reqBuilder = okhttp3.Request.Builder().url(request.url())

        Log.d(TAG, "🌐 Requesting: ${request.url()}")

        request.headers().forEach { (key, values) ->
            values.forEach { value -> reqBuilder.addHeader(key, value) }
        }

        if (request.httpMethod() == "POST") {
            val bodyBytes = request.dataToSend() ?: ByteArray(0)
            reqBuilder.post(bodyBytes.toRequestBody("application/json".toMediaType()))
        } else {
            reqBuilder.get()
        }

        val response = client.newCall(reqBuilder.build()).execute()
        val bodyStr = response.body?.string() ?: ""

        Log.d(TAG, "📩 Response Code: ${response.code} for ${request.url()}")

        if (bodyStr.trimStart().startsWith("<")) {
            Log.e(TAG, "❌ HTML Block Detected (Captcha or Bot Check)! Preview: ${bodyStr.take(150)}")
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
                    try {
                        // 🟢 MUST HAVE LOCALIZATION OR APP CRASHES
                        NewPipe.init(NewPipeDownloader(), Localization.DEFAULT)
                        initialized = true
                        Log.d(TAG, "✅ NewPipe initialized (v0.26.2 X-Ray)")
                    } catch (t: Throwable) {
                        Log.e(TAG, "❌ Init Failed: ${t.message}")
                    }
                }
            }
        }
    }

    suspend fun searchNextPage(query: String, offset: Int): List<Song> =
        withContext(Dispatchers.IO) {
            try {
                init()
                val extractor = ServiceList.YouTube.getSearchExtractor(query, listOf(YoutubeSearchQueryHandlerFactory.VIDEOS), "")
                extractor.fetchPage()

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
                                    id = item.url, title = item.name ?: "Unknown", artist = item.uploaderName ?: "Unknown",
                                    albumArt = item.thumbnails.firstOrNull()?.url ?: "", streamUrl = item.url,
                                    videoId = item.url.substringAfter("v=").substringBefore("&"), permanentUrl = item.url,
                                    album = "YouTube", source = "youtube", sourceType = com.ant.tunes.data.SourceType.YOUTUBE
                                )
                            } catch (t: Throwable) { null } // 🟢 Throwable prevents UI crash
                        }
                } else emptyList()
            } catch (t: Throwable) {
                Log.e(TAG, "❌ Extractor Error (Next Page): ${t.message}")
                emptyList()
            }
        }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            init()
            Log.d(TAG, "🔍 Fetching Search Page for: '$query'")

            // 🟢 Changed from VIDEOS to emptyList() to see if YouTube broke the Video Tab ID
            val extractor = ServiceList.YouTube.getSearchExtractor(query, emptyList(), "")
            extractor.fetchPage()

            val items = extractor.initialPage.items
            Log.d(TAG, "📦 Raw Items Extracted: ${items.size}")

            if (items.isEmpty()) {
                val errors = extractor.initialPage.errors
                Log.e(TAG, "⚠️ 0 Items Found! Page Errors: ${errors.joinToString { it.message.toString() }}")
            }

            val streams = items.filterIsInstance<StreamInfoItem>()
            Log.d(TAG, "🎵 Audio/Video Streams found: ${streams.size}")

            streams.take(10).mapNotNull { item ->
                try {
                    val vidId = if (item.url.contains("v=")) item.url.substringAfter("v=").substringBefore("&") else item.url

                    Song(
                        id = item.url, title = item.name ?: "Unknown", artist = item.uploaderName ?: "Unknown",
                        albumArt = item.thumbnails.firstOrNull()?.url ?: "", streamUrl = item.url, videoId = vidId,
                        permanentUrl = item.url, album = "YouTube", source = "youtube", sourceType = com.ant.tunes.data.SourceType.YOUTUBE
                    )
                } catch (t: Throwable) { null }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "❌ Search Exception: ${t.message}")
            emptyList()
        }
    }

    suspend fun getAudioUrl(videoUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            init()
            Log.d(TAG, "🎵 Extracting Audio stream for: $videoUrl")
            val safeUrl = if (!videoUrl.contains("www.youtube.com") && videoUrl.contains("youtube.com")) {
                videoUrl.replace("youtube.com", "www.youtube.com")
            } else videoUrl

            val extractor = ServiceList.YouTube.getStreamExtractor(safeUrl)
            extractor.fetchPage()

            // 🟢 FIXED FALLBACK: No more 'audioBitrate' calls!
            // We grab the best audio-only stream. If that fails, we just grab the first available video stream.
            val bestAudio = extractor.audioStreams?.maxByOrNull { it.averageBitrate }
            val fallbackVideo = extractor.videoStreams?.firstOrNull()

            val finalUrl = bestAudio?.content ?: fallbackVideo?.content

            if (finalUrl != null) {
                Log.d(TAG, "✅ Audio extracted successfully!")
            } else {
                Log.e(TAG, "⚠️ Extractor succeeded but found ZERO streams!")
            }

            finalUrl
        } catch (t: Throwable) {
            Log.e(TAG, "❌ Audio Extraction Error: ${t.javaClass.simpleName} - ${t.message}")
            null
        }
    }
}
