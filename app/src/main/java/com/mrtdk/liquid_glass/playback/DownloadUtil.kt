package com.mrtdk.liquid_glass.playback

import android.content.Context
import android.net.Uri
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import okhttp3.OkHttpClient
import java.util.concurrent.Executor
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.ItemType
import com.echo.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DownloadUtil private constructor(private val context: Context) {

    val databaseProvider: DatabaseProvider = StandaloneDatabaseProvider(context)
    
    val playerCache: SimpleCache = SimpleCache(
        context.filesDir.resolve("exoplayer"),
        LeastRecentlyUsedCacheEvictor(512 * 1024 * 1024L), // 512MB limit for stream player cache
        databaseProvider
    )

    val downloadCache: SimpleCache = SimpleCache(
        context.filesDir.resolve("download"),
        NoOpCacheEvictor(), // Caches downloaded files indefinitely
        databaseProvider
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val dataSourceFactory = ResolvingDataSource.Factory(
        CacheDataSource.Factory()
            .setCache(playerCache)
            .setUpstreamDataSourceFactory(
                OkHttpDataSource.Factory(
                    OkHttpClient.Builder()
                        .proxy(YouTube.proxy)
                        .proxyAuthenticator { _, response ->
                            YouTube.proxyAuth?.let { auth ->
                                response.request.newBuilder()
                                    .header("Proxy-Authorization", auth)
                                    .build()
                            } ?: response.request
                        }
                        .fastFallback(true)
                        .build()
                )
            )
    ) { dataSpec ->
        val mediaId = dataSpec.key ?: error("No media id")
        
        // If it's already in player cache, skip URL resolution
        if (playerCache.isCached(mediaId, dataSpec.position, if (dataSpec.length >= 0) dataSpec.length else 1)) {
            return@Factory dataSpec
        }

        // Otherwise resolve stream url
        val streamUrl = runBlocking(Dispatchers.IO) {
            com.mrtdk.liquid_glass.playback.MusicPlayer.resolveUrl(mediaId)
        } ?: error("Failed to resolve URL for download: $mediaId")

        dataSpec.withUri(Uri.parse(streamUrl))
    }

    val downloadNotificationHelper = DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    val downloadManager: DownloadManager = DownloadManager(
        context,
        databaseProvider,
        downloadCache,
        dataSourceFactory,
        Executor { it.run() }
    ).apply {
        maxParallelDownloads = 3
        addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) {
                downloads.update { map ->
                    map.toMutableMap().apply {
                        put(download.request.id, download)
                    }
                }

                scope.launch {
                    when (download.state) {
                        Download.STATE_COMPLETED -> {
                            try {
                                val metaString = String(download.request.data, Charsets.UTF_8)
                                val parts = metaString.split("||")
                                if (parts.size >= 4) {
                                    val title = parts[0]
                                    val artist = parts[1]
                                    val artUrl = parts[2].takeIf { it.isNotEmpty() }
                                    val album = parts[3].takeIf { it.isNotEmpty() }
                                    val libraryItem = LibraryItem(
                                        id = download.request.id,
                                        title = title,
                                        subtitle = artist,
                                        thumbnail = artUrl,
                                        type = ItemType.SONG,
                                        album = album
                                    )
                                    LibraryManager.saveDownloadedSong(libraryItem)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        Download.STATE_FAILED,
                        Download.STATE_REMOVING -> {
                            LibraryManager.deleteDownloadedSong(context, download.request.id)
                        }
                        else -> {}
                    }
                }
            }
        })
    }

    init {
        val result = mutableMapOf<String, Download>()
        try {
            val cursor = downloadManager.downloadIndex.getDownloads()
            while (cursor.moveToNext()) {
                result[cursor.download.request.id] = cursor.download
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        downloads.value = result
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    fun release() {
        scope.cancel()
    }

    companion object {
        @Volatile
        private var instance: DownloadUtil? = null

        fun getInstance(context: Context): DownloadUtil {
            return instance ?: synchronized(this) {
                instance ?: DownloadUtil(context.applicationContext).also { instance = it }
            }
        }
    }
}
