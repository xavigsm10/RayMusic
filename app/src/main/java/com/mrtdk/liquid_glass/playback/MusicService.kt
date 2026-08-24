package com.mrtdk.liquid_glass.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mrtdk.liquid_glass.MainActivity
import com.echo.innertube.YouTube
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import android.os.Build
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.media3.session.DefaultMediaNotificationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mrtdk.liquid_glass.R


@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
    private lateinit var eqProcessor: com.mrtdk.liquid_glass.playback.eq.CustomEqualizerAudioProcessor

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var hasBoundClients = false
    private var idleStopJob: Job? = null
    private var hasCalledStartForeground = false

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val activeResolutions = java.util.concurrent.atomic.AtomicInteger(0)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null

    private fun playSongState(state: com.mrtdk.liquid_glass.ui.screens.PlayerState) {
        if (state.contentUri != null) {
            val metadata = androidx.media3.common.MediaMetadata.Builder().apply {
                setTitle(state.title)
                setArtist(state.artist)
                state.artUrl?.toString()?.let { setArtworkUri(android.net.Uri.parse(it)) }
            }.build()
            val mediaItem = androidx.media3.common.MediaItem.Builder()
                .setUri(state.contentUri)
                .setMediaMetadata(metadata)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } else if (state.videoId != null) {
            com.mrtdk.liquid_glass.playback.MusicPlayer.songMetadataCache[state.videoId] = Pair(state.title, state.artist)
            val metadata = androidx.media3.common.MediaMetadata.Builder().apply {
                setTitle(state.title)
                setArtist(state.artist)
                state.artUrl?.toString()?.let { setArtworkUri(android.net.Uri.parse(it)) }
            }.build()
            
            val mediaItem = androidx.media3.common.MediaItem.Builder()
                .setMediaId(state.videoId)
                .setUri(android.net.Uri.parse("yt://${state.videoId}"))
                .setCustomCacheKey(state.videoId)
                .setMediaMetadata(metadata)
                .build()
            
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        com.mrtdk.liquid_glass.data.LibraryManager.init(applicationContext)
        com.mrtdk.liquid_glass.playback.eq.EqualizerService.init(applicationContext)
        eqProcessor = com.mrtdk.liquid_glass.playback.eq.CustomEqualizerAudioProcessor()
        com.mrtdk.liquid_glass.playback.eq.EqualizerService.addAudioProcessor(eqProcessor)

        com.mrtdk.liquid_glass.utils.YTPlayerUtils.init(applicationContext)
        serviceScope.launch(Dispatchers.IO) {
            com.echo.innertube.YouTubeExtractor.ensureInitialized()
        }

        val okHttpClient = OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .proxyAuthenticator { _, response ->
                YouTube.proxyAuth?.let { auth ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", auth)
                        .build()
                } ?: response.request
            }
            .fastFallback(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val dataSourceFactory = createDataSourceFactory(okHttpClient)

        val extractorsFactory = androidx.media3.extractor.ExtractorsFactory {
            arrayOf(
                androidx.media3.extractor.mkv.MatroskaExtractor(),        // .webm / Opus (YouTube)
                androidx.media3.extractor.mp4.FragmentedMp4Extractor(),   // fragmented .mp4 / AAC (YouTube)
                androidx.media3.extractor.mp4.Mp4Extractor(),             // regular .mp4 / AAC (JioSaavn)
            )
        }

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink? {
                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(eqProcessor))
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,
                45_000,
                500,
                1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : androidx.media3.common.Player.Listener {
            private var recoveryJob: Job? = null
            private val songRetryCounts = java.util.concurrent.ConcurrentHashMap<String, Int>()

            private fun getHttpResponseCode(error: androidx.media3.common.PlaybackException): Int {
                var cause: Throwable? = error.cause ?: error
                while (cause != null) {
                    if (cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                        return cause.responseCode
                    }
                    val msg = cause.message ?: ""
                    if (msg.contains("Response code: 403") || msg.contains("403")) {
                        return 403
                    }
                    if (msg.contains("Response code: 416") || msg.contains("416")) {
                        return 416
                    }
                    cause = cause.cause
                }
                return -1
            }

            private fun isExpiredUrlError(error: androidx.media3.common.PlaybackException): Boolean {
                val code = getHttpResponseCode(error)
                return code == 403 || error.message?.contains("403") == true
            }

            private fun isRangeNotSatisfiableError(error: androidx.media3.common.PlaybackException): Boolean {
                val code = getHttpResponseCode(error)
                return code == 416 || error.message?.contains("416") == true
            }

            private fun isPageReloadError(error: androidx.media3.common.PlaybackException): Boolean {
                val msg = (error.message.orEmpty() + " " + error.cause?.message.orEmpty()).lowercase()
                return msg.contains("page needs to be reloaded") || msg.contains("reload")
            }

            private fun handlePlaybackRecovery(error: androidx.media3.common.PlaybackException, mediaId: String) {
                val httpCode = getHttpResponseCode(error)
                val is403 = isExpiredUrlError(error)
                val is416 = isRangeNotSatisfiableError(error)
                val isReload = isPageReloadError(error)

                val currentRetries = songRetryCounts.getOrDefault(mediaId, 0)
                if (currentRetries >= 3) {
                    android.util.Log.e("MusicService", "Max retries (3) reached for $mediaId. Halting retry.")
                    return
                }

                songRetryCounts[mediaId] = currentRetries + 1
                android.util.Log.w("MusicService", "Recovering from error (http=$httpCode, is403=$is403, code=${error.errorCode}) for $mediaId, retry #${currentRetries + 1}/3")

                com.mrtdk.liquid_glass.playback.MusicPlayer.clearCache(mediaId)
                com.echo.innertube.YouTubeExtractor.clearCache()

                val currentPos = if (is416) 0L else player.currentPosition
                val currentIndex = player.currentMediaItemIndex

                val downloadUtil = com.mrtdk.liquid_glass.playback.DownloadUtil.getInstance(this@MusicService)
                val playerCache = downloadUtil.playerCache

                recoveryJob?.cancel()
                recoveryJob = serviceScope.launch(Dispatchers.IO) {
                    try {
                        playerCache.removeResource(mediaId)
                    } catch (_: Exception) {}
                    if (is403) {
                        com.mrtdk.liquid_glass.utils.BotDetectionMitigator.notifyPlaybackFailure(YouTube.cookie != null, error.message)
                        com.mrtdk.liquid_glass.utils.BotDetectionMitigator.rotateGuestSession()
                    }
                    delay(300)
                    withContext(Dispatchers.Main) {
                        try {
                            player.seekTo(currentIndex, currentPos)
                            player.prepare()
                            player.play()
                            android.util.Log.d("MusicService", "Playback silently recovered and re-prepared at $currentPos ms for $mediaId")
                        } catch (e: Exception) {
                            android.util.Log.e("MusicService", "Silent recovery prepare failed for $mediaId", e)
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateWakeLocks()
                checkForegroundState()

                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    songRetryCounts.clear()
                }

                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    val nextState = com.mrtdk.liquid_glass.playback.PlaybackQueue.getNextSongAndAdvance(player.repeatMode)
                    if (nextState != null) {
                        playSongState(nextState)
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateWakeLocks()
                checkForegroundState()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val currentMediaId = player.currentMediaItem?.mediaId
                if (currentMediaId != null) {
                    handlePlaybackRecovery(error, currentMediaId)
                }
            }
        })

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return if (command == Player.COMMAND_SEEK_TO_NEXT || 
                    command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                    command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) {
                    true
                } else {
                    super.isCommandAvailable(command)
                }
            }

            override fun hasNextMediaItem(): Boolean = true

            override fun hasPreviousMediaItem(): Boolean = true

            override fun seekToNext() {
                seekToNextMediaItem()
            }

            override fun seekToPrevious() {
                if (player.currentPosition > 3000) {
                    player.seekTo(0)
                } else {
                    seekToPreviousMediaItem()
                }
            }

            override fun seekToNextMediaItem() {
                val nextState = com.mrtdk.liquid_glass.playback.PlaybackQueue.getNextSongAndAdvance(player.repeatMode)
                if (nextState != null) {
                    playSongState(nextState)
                }
            }

            override fun seekToPreviousMediaItem() {
                val prevState = com.mrtdk.liquid_glass.playback.PlaybackQueue.getPreviousSongAndGoBack()
                if (prevState != null) {
                    playSongState(prevState)
                }
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): com.google.common.util.concurrent.ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    return com.google.common.util.concurrent.Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(emptyList(), C.INDEX_UNSET, C.TIME_UNSET)
                    )
                }
            })
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error creating notification channel: ${e.message}")
        }

        try {
            setMediaNotificationProvider(
                DefaultMediaNotificationProvider(
                    this,
                    { NOTIFICATION_ID },
                    CHANNEL_ID,
                    R.string.app_name
                ).apply {
                    setSmallIcon(R.drawable.ic_launcher_foreground)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error setting media notification provider: ${e.message}")
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null) {
            if (!player.playWhenReady || player.mediaItemCount == 0) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        cancelIdleStop()
        releaseLocks()
        if (::eqProcessor.isInitialized) {
            com.mrtdk.liquid_glass.playback.eq.EqualizerService.removeAudioProcessor(eqProcessor)
        }
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    @Synchronized
    private fun acquireLocks() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RayMusic:PlaybackWakeLock")
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire()
                android.util.Log.d("MusicService", "Acquired playback WakeLock")
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error acquiring WakeLock: ${e.message}")
        }

        try {
            if (wifiLock == null) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                wifiLock = wifiManager.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "RayMusic:PlaybackWifiLock")
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
                android.util.Log.d("MusicService", "Acquired playback WifiLock")
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error acquiring WifiLock: ${e.message}")
        }
    }

    @Synchronized
    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                android.util.Log.d("MusicService", "Released playback WakeLock")
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error releasing WakeLock: ${e.message}")
        }

        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                android.util.Log.d("MusicService", "Released playback WifiLock")
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error releasing WifiLock: ${e.message}")
        }
    }

    private fun updateWakeLocks() {
        val isResolving = activeResolutions.get() > 0
        val playWhenReady = player.playWhenReady
        val playbackState = player.playbackState
        val shouldHold = isResolving || (playWhenReady && (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY))
        
        if (shouldHold) {
            acquireLocks()
        } else {
            releaseLocks()
        }
    }

    override fun onBind(intent: Intent?): android.os.IBinder? {
        hasBoundClients = true
        cancelIdleStop()
        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        hasBoundClients = false
        scheduleStopIfIdle()
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        hasBoundClients = true
        cancelIdleStop()
        super.onRebind(intent)
    }

    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (startInForegroundRequired) {
            ensureStartedAsForeground()
        }
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    private fun promoteToStartedService() {
        try {
            startService(Intent(this, MusicService::class.java))
            android.util.Log.d("MusicService", "Promoted service to started service")
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error promoting service to started: ${e.message}")
        }
    }

    private fun ensureStartedAsForeground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (hasCalledStartForeground) return

        val notification = try {
            val contentIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.app_name))
                .setContentIntent(contentIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error building notification for foreground: ${e.message}")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            hasCalledStartForeground = true
            android.util.Log.d("MusicService", "Service started in foreground")
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error starting foreground service: ${e.message}")
        }
    }

    private fun cancelIdleStop() {
        idleStopJob?.cancel()
        idleStopJob = null
    }

    private fun stopForegroundAndSelf() {
        cancelIdleStop()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error stopping foreground: ${e.message}")
        }
        hasCalledStartForeground = false
        stopSelf()
    }

    private fun scheduleStopIfIdle() {
        if (hasBoundClients) return
        val state = player.playbackState
        val keepAlive = player.isPlaying ||
                (player.playWhenReady && (state == Player.STATE_BUFFERING || state == Player.STATE_READY))
        if (keepAlive) {
            cancelIdleStop()
            return
        }

        val delayMs = when (state) {
            Player.STATE_READY -> 5 * 60_000L
            Player.STATE_ENDED, Player.STATE_IDLE -> 30_000L
            else -> 60_000L
        }

        cancelIdleStop()
        idleStopJob = serviceScope.launch {
            delay(delayMs)
            if (hasBoundClients) return@launch
            val currentState = player.playbackState
            val shouldKeep = player.isPlaying ||
                    (player.playWhenReady && (currentState == Player.STATE_BUFFERING || currentState == Player.STATE_READY))
            if (shouldKeep) return@launch
            stopForegroundAndSelf()
        }
    }

    private fun checkForegroundState() {
        val state = player.playbackState
        val shouldHold = player.playWhenReady &&
                (state == Player.STATE_BUFFERING || state == Player.STATE_READY)

        if (shouldHold) {
            promoteToStartedService()
            ensureStartedAsForeground()
            cancelIdleStop()
        } else {
            scheduleStopIfIdle()
        }
    }

    private fun createCacheDataSource(okHttpClient: OkHttpClient): androidx.media3.datasource.cache.CacheDataSource.Factory {
        val downloadUtil = com.mrtdk.liquid_glass.playback.DownloadUtil.getInstance(this)
        val downloadCache = downloadUtil.downloadCache
        val playerCache = downloadUtil.playerCache

        return androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                androidx.media3.datasource.cache.CacheDataSource.Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(
                        androidx.media3.datasource.DefaultDataSource.Factory(
                            this,
                            androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(okHttpClient)
                        )
                    )
            )
            .setCacheWriteDataSinkFactory(null)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun createDataSourceFactory(okHttpClient: OkHttpClient): androidx.media3.datasource.DataSource.Factory {
        val downloadUtil = com.mrtdk.liquid_glass.playback.DownloadUtil.getInstance(this)
        val downloadCache = downloadUtil.downloadCache
        val playerCache = downloadUtil.playerCache

        return androidx.media3.datasource.ResolvingDataSource.Factory(createCacheDataSource(okHttpClient)) { dataSpec ->
            val mediaId = dataSpec.key ?: dataSpec.uri.host ?: dataSpec.uri.toString().removePrefix("yt://")

            // 1. If cached in downloadCache or playerCache, play immediately without network
            if (downloadCache.isCached(
                    mediaId,
                    dataSpec.position,
                    if (dataSpec.length >= 0) dataSpec.length else 1
                ) ||
                playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)
            ) {
                return@Factory dataSpec
            }

            // 2. If valid stream URL is cached in memory, use it with 512KB chunking
            com.mrtdk.liquid_glass.playback.MusicPlayer.getCachedUrl(mediaId)?.let { streamUrl ->
                return@Factory dataSpec.withUri(android.net.Uri.parse(streamUrl)).subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
            }

            activeResolutions.incrementAndGet()
            acquireLocks()

            val streamUrl = try {
                kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                    com.mrtdk.liquid_glass.playback.MusicPlayer.resolveUrl(mediaId)
                }
            } catch (e: Exception) {
                null
            } finally {
                activeResolutions.decrementAndGet()
                mainHandler.post { updateWakeLocks() }
            }

            if (!streamUrl.isNullOrBlank()) {
                dataSpec.withUri(android.net.Uri.parse(streamUrl)).subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
            } else {
                throw java.io.IOException("No se pudo obtener el flujo de reproducción para $mediaId")
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val CHUNK_LENGTH = 512 * 1024L
    }
}
