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
import android.os.Bundle
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mrtdk.liquid_glass.R


@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    lateinit var playerA: ExoPlayer
    lateinit var playerB: ExoPlayer
    lateinit var activePlayer: ExoPlayer
    lateinit var standbyPlayer: ExoPlayer

    val player: ExoPlayer get() = activePlayer

    private lateinit var eqProcessorA: com.mrtdk.liquid_glass.playback.eq.CustomEqualizerAudioProcessor
    private lateinit var eqProcessorB: com.mrtdk.liquid_glass.playback.eq.CustomEqualizerAudioProcessor

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var hasBoundClients = false
    private var idleStopJob: Job? = null
    private var hasCalledStartForeground = false

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val activeResolutions = java.util.concurrent.atomic.AtomicInteger(0)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null

    private var isCrossfading = false
    private var isPreloading = false
    private var preloadedSongKey: String? = null
    private var preloadedState: com.mrtdk.liquid_glass.ui.screens.PlayerState? = null
    private var crossfadeJob: Job? = null
    private var trackEndMonitorJob: Job? = null
    private var recoveryJob: Job? = null
    private var radioRefillJob: Job? = null
    private val songRetryCounts = java.util.concurrent.ConcurrentHashMap<String, Int>()

    private val mediaAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private fun buildMediaItem(state: com.mrtdk.liquid_glass.ui.screens.PlayerState): androidx.media3.common.MediaItem {
        val metadata = androidx.media3.common.MediaMetadata.Builder().apply {
            setTitle(state.title)
            setArtist(state.artist)
            state.artUrl?.toString()?.let { setArtworkUri(android.net.Uri.parse(it)) }
        }.build()

        val isLocal = state.contentUri != null && state.contentUri.scheme != "yt"
        return if (isLocal) {
            androidx.media3.common.MediaItem.Builder()
                .setUri(state.contentUri)
                .setMediaMetadata(metadata)
                .build()
        } else {
            val videoId = state.videoId?.takeIf { it.isNotBlank() }
                ?: state.contentUri?.toString()?.removePrefix("yt://")?.takeIf { it.isNotBlank() }
                ?: ""
            if (videoId.isNotBlank()) {
                com.mrtdk.liquid_glass.playback.MusicPlayer.songMetadataCache[videoId] = Pair(state.title, state.artist)
            }
            androidx.media3.common.MediaItem.Builder()
                .setMediaId(videoId)
                .setUri(android.net.Uri.parse("yt://$videoId"))
                .setCustomCacheKey(videoId)
                .setMediaMetadata(metadata)
                .build()
        }
    }

    private fun preloadNextSong(nextSong: com.mrtdk.liquid_glass.ui.screens.PlayerState) {
        val key = nextSong.videoId ?: nextSong.contentUri?.toString() ?: nextSong.title
        preloadedSongKey = key
        preloadedState = nextSong
        isPreloading = true
        serviceScope.launch {
            try {
                android.util.Log.d("MusicService", "AutoMix: Preloading next song in background: ${nextSong.title}")
                val mediaItem = buildMediaItem(nextSong)
                standbyPlayer.volume = 0f
                standbyPlayer.playWhenReady = false
                standbyPlayer.setMediaItem(mediaItem)
                standbyPlayer.prepare()
            } catch (e: Exception) {
                android.util.Log.e("MusicService", "AutoMix: Error preloading next song: ${e.message}")
            } finally {
                isPreloading = false
            }
        }
    }

    /**
     * Cancela un crossfade a medias y deja los players en estado sano para que una
     * pulsación manual de siguiente nunca se pierda. Solo se usa en skips manuales;
     * el avance automático del automix no pasa por aquí.
     */
    private fun cancelCrossfadeToIdle() {
        try { crossfadeJob?.cancel() } catch (_: Exception) {}
        crossfadeJob = null
        isCrossfading = false
        com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutoMixing = false
        try { activePlayer.volume = 1f } catch (_: Exception) {}
        try { standbyPlayer.stop(); standbyPlayer.clearMediaItems() } catch (_: Exception) {}
        preloadedSongKey = null
        preloadedState = null
    }

    /**
     * Rellena upNext con autoplay cuando la cola se vacía, para que "siguiente"
     * funcione siempre. No toca el automix: solo agrega canciones a la cola.
     */
    private fun triggerRadioRefillIfNeeded() {
        val queue = com.mrtdk.liquid_glass.playback.PlaybackQueue
        if (queue.upNextSongs.isNotEmpty()) return
        if (com.mrtdk.liquid_glass.data.LibraryManager.getString("autoplay_similar", "true") != "true") return
        if (queue.isExclusiveQueue) return
        val seed = queue.currentSong?.videoId ?: queue.queueSeedVideoId ?: return
        if (radioRefillJob?.isActive == true) return
        radioRefillJob = serviceScope.launch(Dispatchers.IO) {
            try {
                var result = YouTube.next(com.echo.innertube.models.WatchEndpoint(videoId = seed)).getOrNull()
                if (result == null || result.items.isEmpty()) {
                    val fallback = com.echo.innertube.models.WatchEndpoint(videoId = seed, playlistId = "RDAMVM$seed")
                    result = YouTube.next(fallback).getOrNull()
                }
                val res = result ?: return@launch
                val nonVideo = res.items.filterNot { it.isVideoSong }
                val finalItems = nonVideo.ifEmpty { res.items }
                val nextItems = if (finalItems.isNotEmpty() && finalItems.first().id == seed) finalItems.drop(1) else finalItems
                if (nextItems.isEmpty()) return@launch
                withContext(Dispatchers.Main) {
                    queue.queueEndpoint = res.endpoint
                    queue.queueContinuation = res.continuation
                    queue.upNextSongs = nextItems
                    queue.onQueueChanged?.invoke()
                }
            } catch (e: Exception) {
                android.util.Log.w("MusicService", "Radio refill failed: ${e.message}")
            }
        }
    }

    private fun startCrossfadeTransition(
        durationMs: Long,
        targetState: com.mrtdk.liquid_glass.ui.screens.PlayerState,
        isNaturalEnding: Boolean = false
    ) {
        if (isCrossfading) return
        isCrossfading = true
        com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutoMixing = true

        crossfadeJob?.cancel()
        crossfadeJob = serviceScope.launch {
            try {
                val key = targetState.videoId ?: targetState.contentUri?.toString() ?: targetState.title
                if (preloadedSongKey != key || standbyPlayer.playbackState == Player.STATE_IDLE) {
                    preloadedSongKey = key
                    preloadedState = targetState
                    val mediaItem = buildMediaItem(targetState)
                    standbyPlayer.volume = 0f
                    standbyPlayer.playWhenReady = false
                    standbyPlayer.setMediaItem(mediaItem)
                    standbyPlayer.prepare()
                }

                var waitCount = 0
                while ((standbyPlayer.playbackState == Player.STATE_BUFFERING || standbyPlayer.playbackState == Player.STATE_IDLE) && waitCount < 50) {
                    delay(40L)
                    waitCount++
                }

                if (standbyPlayer.playerError != null) {
                    android.util.Log.w("MusicService", "standbyPlayer error, falling back to activePlayer: ${standbyPlayer.playerError?.message}")
                    isCrossfading = false
                    com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutoMixing = false
                    activePlayer.volume = 1f
                    playSongState(targetState)
                    return@launch
                }

                standbyPlayer.volume = 0f
                standbyPlayer.play()

                val steps = (durationMs / 25L).toInt().coerceAtLeast(10)
                val stepDelay = durationMs / steps

                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val rad = (t * (Math.PI / 2.0)).toFloat()
                    val outVol = kotlin.math.cos(rad).coerceIn(0f, 1f)
                    val inVol = kotlin.math.sin(rad).coerceIn(0f, 1f)

                    activePlayer.volume = outVol
                    standbyPlayer.volume = inVol

                    delay(stepDelay)
                }

                activePlayer.volume = 0f
                standbyPlayer.volume = 1f

                completeCrossfade(targetState, isNaturalEnding)
            } catch (e: Exception) {
                android.util.Log.e("MusicService", "AutoMix crossfade error", e)
                completeCrossfade(targetState, isNaturalEnding)
            }
        }
    }

    private fun completeCrossfade(
        targetState: com.mrtdk.liquid_glass.ui.screens.PlayerState,
        isNaturalEnding: Boolean
    ) {
        activePlayer.stop()
        activePlayer.clearMediaItems()
        activePlayer.volume = 1f

        activePlayer.setAudioAttributes(mediaAudioAttributes, false)
        standbyPlayer.setAudioAttributes(mediaAudioAttributes, true)

        val temp = activePlayer
        activePlayer = standbyPlayer
        standbyPlayer = temp

        val currentQueueSong = com.mrtdk.liquid_glass.playback.PlaybackQueue.currentSong
        val targetKey = targetState.videoId ?: targetState.contentUri?.toString()
        val currentKey = currentQueueSong?.videoId ?: currentQueueSong?.contentUri?.toString()
        if (currentKey != targetKey) {
            com.mrtdk.liquid_glass.playback.PlaybackQueue.getNextSongAndAdvance(activePlayer.repeatMode)
        }

        updateMediaSessionPlayer()

        preloadedSongKey = null
        preloadedState = null
        isCrossfading = false
        com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutoMixing = false

        startTrackEndMonitor()
        checkForegroundState()
    }

    private fun updateMediaSessionPlayer() {
        val forwardingPlayer = createForwardingPlayer(activePlayer)
        mediaSession?.setPlayer(forwardingPlayer)
    }

    private fun startTrackEndMonitor() {
        trackEndMonitorJob?.cancel()
        if (!com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutomixEnabled) return
        trackEndMonitorJob = serviceScope.launch {
            while (isActive) {
                delay(200L)
                if (!activePlayer.isPlaying || isCrossfading) continue
                val dur = activePlayer.duration
                val pos = activePlayer.currentPosition
                if (dur > 15_000L && pos > 0L) {
                    val remainingMs = dur - pos

                    // 1. Preload next song at 22s before end
                    if (remainingMs in 8_600L..25_000L && !isPreloading) {
                        val nextSong = com.mrtdk.liquid_glass.playback.PlaybackQueue.peekNextSong(activePlayer.repeatMode)
                        if (nextSong != null) {
                            val key = nextSong.videoId ?: nextSong.contentUri?.toString() ?: nextSong.title
                            if (preloadedSongKey != key) {
                                preloadNextSong(nextSong)
                            }
                        }
                    }

                    // 2. Trigger crossfade at 8.0s before end
                    if (remainingMs in 100L..8_500L && !isCrossfading) {
                        val nextSong = preloadedState ?: com.mrtdk.liquid_glass.playback.PlaybackQueue.peekNextSong(activePlayer.repeatMode)
                        if (nextSong != null) {
                            startCrossfadeTransition(
                                durationMs = remainingMs.coerceIn(4500L, 8000L),
                                targetState = nextSong,
                                isNaturalEnding = true
                            )
                        }
                    }
                } else if (dur in 1L..15_000L && pos > 0L) {
                    val remainingMs = dur - pos
                    if (!isPreloading && preloadedSongKey == null) {
                        val nextSong = com.mrtdk.liquid_glass.playback.PlaybackQueue.peekNextSong(activePlayer.repeatMode)
                        if (nextSong != null) {
                            preloadNextSong(nextSong)
                        }
                    }
                    if (remainingMs in 100L..4_000L && !isCrossfading) {
                        val nextSong = preloadedState ?: com.mrtdk.liquid_glass.playback.PlaybackQueue.peekNextSong(activePlayer.repeatMode)
                        if (nextSong != null) {
                            startCrossfadeTransition(
                                durationMs = remainingMs.coerceAtLeast(1500L),
                                targetState = nextSong,
                                isNaturalEnding = true
                            )
                        }
                    }
                }
            }
        }
    }

    private fun playSongState(state: com.mrtdk.liquid_glass.ui.screens.PlayerState) {
        val automix = com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutomixEnabled
        if (automix && activePlayer.isPlaying) {
            startCrossfadeTransition(durationMs = 1500L, targetState = state, isNaturalEnding = false)
        } else {
            activePlayer.volume = 1f
            val mediaItem = buildMediaItem(state)
            activePlayer.setMediaItem(mediaItem)
            activePlayer.prepare()
            activePlayer.play()
            if (automix) {
                startTrackEndMonitor()
            }
        }
    }

    private fun handleNewMediaItem(mediaItem: androidx.media3.common.MediaItem, startPositionMs: Long = 0L) {
        val automix = com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutomixEnabled
        if (automix && activePlayer.isPlaying) {
            val videoId = mediaItem.mediaId.takeIf { it.isNotBlank() }
                ?: mediaItem.localConfiguration?.uri?.let { uri ->
                    if (uri.scheme == "yt") uri.toString().removePrefix("yt://") else null
                }
            val isLocal = mediaItem.localConfiguration?.uri?.scheme != "yt" && mediaItem.localConfiguration?.uri != null
            val state = com.mrtdk.liquid_glass.ui.screens.PlayerState(
                title = mediaItem.mediaMetadata.title?.toString() ?: "",
                artist = mediaItem.mediaMetadata.artist?.toString() ?: "",
                artUrl = mediaItem.mediaMetadata.artworkUri?.toString(),
                videoId = videoId,
                contentUri = if (isLocal) (mediaItem.requestMetadata.mediaUri ?: mediaItem.localConfiguration?.uri) else null
            )
            startCrossfadeTransition(durationMs = 1500L, targetState = state, isNaturalEnding = false)
        } else {
            activePlayer.volume = 1f
            if (startPositionMs > 0L) {
                activePlayer.setMediaItem(mediaItem, startPositionMs)
            } else {
                activePlayer.setMediaItem(mediaItem)
            }
            activePlayer.prepare()
            activePlayer.play()
            if (automix) {
                startTrackEndMonitor()
            }
        }
    }

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

    private fun handlePlaybackRecovery(targetPlayer: ExoPlayer, error: androidx.media3.common.PlaybackException, mediaId: String) {
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

        val currentPos = if (is416) 0L else targetPlayer.currentPosition
        val currentIndex = targetPlayer.currentMediaItemIndex

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
                    targetPlayer.seekTo(currentIndex, currentPos)
                    targetPlayer.prepare()
                    targetPlayer.play()
                    android.util.Log.d("MusicService", "Playback silently recovered and re-prepared at $currentPos ms for $mediaId")
                } catch (e: Exception) {
                    android.util.Log.e("MusicService", "Silent recovery prepare failed for $mediaId", e)
                }
            }
        }
    }

    private fun createPlayerListener(targetPlayer: ExoPlayer): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateWakeLocks()
                checkForegroundState()

                if (playbackState == Player.STATE_READY) {
                    songRetryCounts.clear()
                    if (targetPlayer == activePlayer && com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutomixEnabled) {
                        startTrackEndMonitor()
                    }
                }

                if (playbackState == Player.STATE_ENDED) {
                    if (targetPlayer == activePlayer && !isCrossfading) {
                        val nextState = com.mrtdk.liquid_glass.playback.PlaybackQueue.getNextSongAndAdvance(activePlayer.repeatMode)
                        if (nextState != null) {
                            playSongState(nextState)
                        }
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateWakeLocks()
                checkForegroundState()
                if (playWhenReady && targetPlayer == activePlayer && com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutomixEnabled) {
                    startTrackEndMonitor()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK && targetPlayer == activePlayer) {
                    if (isCrossfading) {
                        crossfadeJob?.cancel()
                        standbyPlayer.stop()
                        standbyPlayer.clearMediaItems()
                        activePlayer.volume = 1f
                        isCrossfading = false
                        preloadedSongKey = null
                        preloadedState = null
                    }
                    activePlayer.volume = 1f
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val currentMediaId = targetPlayer.currentMediaItem?.mediaId
                if (currentMediaId != null) {
                    handlePlaybackRecovery(targetPlayer, error, currentMediaId)
                }
            }
        }
    }

    private fun createPlayerInstance(
        processor: com.mrtdk.liquid_glass.playback.eq.CustomEqualizerAudioProcessor,
        handleAudioFocus: Boolean,
        dataSourceFactory: androidx.media3.datasource.DataSource.Factory,
        extractorsFactory: androidx.media3.extractor.ExtractorsFactory
    ): ExoPlayer {
        val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink? {
                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(processor))
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,
                45_000,
                500,
                1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(mediaAudioAttributes, handleAudioFocus)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }

    private fun createForwardingPlayer(targetPlayer: ExoPlayer): androidx.media3.common.ForwardingPlayer {
        return object : androidx.media3.common.ForwardingPlayer(targetPlayer) {
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
                performSeekToNextMediaItem()
            }

            override fun seekToPrevious() {
                performSeekToPreviousMediaItem()
            }

            override fun seekToNextMediaItem() {
                performSeekToNextMediaItem()
            }

            override fun seekToPreviousMediaItem() {
                performSeekToPreviousMediaItem()
            }

            override fun pause() {
                activePlayer.pause()
                if (isCrossfading) {
                    standbyPlayer.pause()
                }
            }

            override fun play() {
                activePlayer.play()
                if (isCrossfading) {
                    standbyPlayer.play()
                }
            }

            override fun prepare() {
                if (!isCrossfading) {
                    super.prepare()
                }
            }

            override fun setMediaItem(mediaItem: androidx.media3.common.MediaItem) {
                handleNewMediaItem(mediaItem)
            }

            override fun setMediaItem(mediaItem: androidx.media3.common.MediaItem, startPositionMs: Long) {
                handleNewMediaItem(mediaItem, startPositionMs)
            }

            override fun setMediaItem(mediaItem: androidx.media3.common.MediaItem, resetPosition: Boolean) {
                handleNewMediaItem(mediaItem)
            }

            override fun setMediaItems(mediaItems: MutableList<androidx.media3.common.MediaItem>) {
                if (mediaItems.isNotEmpty()) {
                    handleNewMediaItem(mediaItems.first())
                } else {
                    super.setMediaItems(mediaItems)
                }
            }

            override fun setMediaItems(mediaItems: MutableList<androidx.media3.common.MediaItem>, resetPosition: Boolean) {
                if (mediaItems.isNotEmpty()) {
                    handleNewMediaItem(mediaItems.first())
                } else {
                    super.setMediaItems(mediaItems, resetPosition)
                }
            }

            override fun setMediaItems(
                mediaItems: MutableList<androidx.media3.common.MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ) {
                if (mediaItems.isNotEmpty()) {
                    val item = mediaItems.getOrNull(startIndex) ?: mediaItems.first()
                    handleNewMediaItem(item, startPositionMs)
                } else {
                    super.setMediaItems(mediaItems, startIndex, startPositionMs)
                }
            }
        }
    }

    fun performSeekToNextMediaItem() {
        val queue = com.mrtdk.liquid_glass.playback.PlaybackQueue
        var nextState = queue.getNextSongAndAdvance(activePlayer.repeatMode)
        if (nextState == null && activePlayer.repeatMode == Player.REPEAT_MODE_ONE) {
            nextState = queue.currentSong
        }
        if (nextState == null) {
            triggerRadioRefillIfNeeded()
            try { activePlayer.seekTo(0); activePlayer.play() } catch (_: Exception) {}
            return
        }
        if (isCrossfading) {
            crossfadeJob?.cancel()
            isCrossfading = false
            com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutoMixing = false
        }
        playSongState(nextState)
    }

    fun performSeekToPreviousMediaItem() {
        if (isCrossfading) {
            crossfadeJob?.cancel()
            isCrossfading = false
            com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutoMixing = false
        }
        val prevState = com.mrtdk.liquid_glass.playback.PlaybackQueue.getPreviousSongAndGoBack()
        if (prevState != null) {
            playSongState(prevState)
        } else {
            try { activePlayer.seekTo(0); activePlayer.play() } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        com.mrtdk.liquid_glass.data.LibraryManager.init(applicationContext)
        com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutomixEnabled =
            com.mrtdk.liquid_glass.data.LibraryManager.getString("automix_enabled", "false") == "true"
        com.mrtdk.liquid_glass.playback.eq.EqualizerService.init(applicationContext)
        eqProcessorA = com.mrtdk.liquid_glass.playback.eq.CustomEqualizerAudioProcessor()
        eqProcessorB = com.mrtdk.liquid_glass.playback.eq.CustomEqualizerAudioProcessor()
        com.mrtdk.liquid_glass.playback.eq.EqualizerService.addAudioProcessor(eqProcessorA)
        com.mrtdk.liquid_glass.playback.eq.EqualizerService.addAudioProcessor(eqProcessorB)

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

        playerA = createPlayerInstance(eqProcessorA, handleAudioFocus = true, dataSourceFactory = dataSourceFactory, extractorsFactory = extractorsFactory)
        playerB = createPlayerInstance(eqProcessorB, handleAudioFocus = false, dataSourceFactory = dataSourceFactory, extractorsFactory = extractorsFactory)

        playerA.addListener(createPlayerListener(playerA))
        playerB.addListener(createPlayerListener(playerB))

        activePlayer = playerA
        standbyPlayer = playerB

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val forwardingPlayer = createForwardingPlayer(activePlayer)

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val connectionResult = super.onConnect(session, controller)
                    val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                        .add(SessionCommand("ACTION_SEEK_NEXT", Bundle.EMPTY))
                        .add(SessionCommand("ACTION_SEEK_PREVIOUS", Bundle.EMPTY))
                        .build()
                    val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(playerCommands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): com.google.common.util.concurrent.ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        "ACTION_SEEK_NEXT" -> {
                            performSeekToNextMediaItem()
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "ACTION_SEEK_PREVIOUS" -> {
                            performSeekToPreviousMediaItem()
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }

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
        try {
            if (::playerA.isInitialized) {
                playerA.playWhenReady = false
                playerA.stop()
                playerA.clearMediaItems()
            }
            if (::playerB.isInitialized) {
                playerB.playWhenReady = false
                playerB.stop()
                playerB.clearMediaItems()
            }
        } catch (_: Exception) {}
        try {
            stopForegroundAndSelf()
        } catch (_: Exception) {
            try {
                stopSelf()
            } catch (_: Exception) {}
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceJob.cancel()
        cancelIdleStop()
        releaseLocks()
        if (::eqProcessorA.isInitialized) {
            com.mrtdk.liquid_glass.playback.eq.EqualizerService.removeAudioProcessor(eqProcessorA)
        }
        if (::eqProcessorB.isInitialized) {
            com.mrtdk.liquid_glass.playback.eq.EqualizerService.removeAudioProcessor(eqProcessorB)
        }
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        if (::playerA.isInitialized) {
            playerA.release()
        }
        if (::playerB.isInitialized) {
            playerB.release()
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
        val isPlayingA = ::playerA.isInitialized && playerA.playWhenReady &&
                (playerA.playbackState == Player.STATE_BUFFERING || playerA.playbackState == Player.STATE_READY)
        val isPlayingB = ::playerB.isInitialized && playerB.playWhenReady &&
                (playerB.playbackState == Player.STATE_BUFFERING || playerB.playbackState == Player.STATE_READY)
        val shouldHold = isResolving || isPlayingA || isPlayingB
        
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

            // 1. If fully downloaded offline, play immediately without network
            val isFullyDownloaded = downloadCache.isCached(mediaId, 0, -1) || downloadUtil.downloads.value[mediaId]?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
            if (isFullyDownloaded) {
                return@Factory dataSpec.buildUpon()
                    .setKey(mediaId)
                    .build()
            }

            // 2. If valid stream URL is cached in memory, use it with 512KB chunking
            val cachedUrl = com.mrtdk.liquid_glass.playback.MusicPlayer.getCachedUrl(mediaId)
            val streamUrl = if (!cachedUrl.isNullOrBlank()) {
                cachedUrl
            } else {
                activeResolutions.incrementAndGet()
                acquireLocks()

                try {
                    kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                        com.mrtdk.liquid_glass.playback.MusicPlayer.resolveUrl(mediaId)
                    }
                } catch (e: Exception) {
                    null
                } finally {
                    activeResolutions.decrementAndGet()
                    mainHandler.post { updateWakeLocks() }
                }
            }

            if (!streamUrl.isNullOrBlank()) {
                dataSpec.buildUpon()
                    .setUri(android.net.Uri.parse(streamUrl))
                    .setKey(mediaId)
                    .build()
                    .subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
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
