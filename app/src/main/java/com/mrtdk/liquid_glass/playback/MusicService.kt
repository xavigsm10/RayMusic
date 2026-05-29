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
import com.mrtdk.liquid_glass.R


@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer

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
            val metadata = androidx.media3.common.MediaMetadata.Builder().apply {
                setTitle(state.title)
                setArtist(state.artist)
                state.artUrl?.toString()?.let { setArtworkUri(android.net.Uri.parse(it)) }
            }.build()
            
            val mediaItem = androidx.media3.common.MediaItem.Builder()
                .setMediaId(state.videoId)
                .setUri(android.net.Uri.parse("yt://${state.videoId}"))
                .setMediaMetadata(metadata)
                .build()
            
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        val okHttpDataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(
            OkHttpClient.Builder()
                .proxy(YouTube.proxy)
                .addInterceptor { chain ->
                    val request = chain.request()
                    val clientParam = request.url.queryParameter("c")
                    
                    val c = clientParam?.trim().orEmpty()
                    
                    val ua = when {
                        c.equals("WEB_REMIX", ignoreCase = true) ||
                            c.equals("WEB", ignoreCase = true) ||
                            c.equals("WEB_CREATOR", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.USER_AGENT_WEB
            
                        c.equals("TVHTML5", ignoreCase = true) ||
                            c.equals("TVHTML5_SIMPLY_EMBEDDED_PLAYER", ignoreCase = true) ||
                            c.equals("TVHTML5_SIMPLY", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.TVHTML5.userAgent
            
                        c.startsWith("IOS", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.IOS.userAgent
            
                        c.startsWith("ANDROID_VR", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.ANDROID_VR_NO_AUTH.userAgent
            
                        c.startsWith("ANDROID_CREATOR", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.ANDROID_CREATOR.userAgent
            
                        c.startsWith("ANDROID", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.MOBILE.userAgent
            
                        c.startsWith("VISIONOS", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.VISIONOS.userAgent
                        
                        c.equals("NEWPIPE_FALLBACK", ignoreCase = true) -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
            
                        else -> com.echo.innertube.models.YouTubeClient.ANDROID_VR_NO_AUTH.userAgent
                    }
                    
                    val origin = when {
                        c.equals("WEB_REMIX", ignoreCase = true) ||
                            c.equals("WEB", ignoreCase = true) ||
                            c.equals("WEB_CREATOR", ignoreCase = true) ->
                            "https://music.youtube.com"
            
                        c.equals("TVHTML5", ignoreCase = true) ||
                            c.equals("TVHTML5_SIMPLY_EMBEDDED_PLAYER", ignoreCase = true) ||
                            c.equals("TVHTML5_SIMPLY", ignoreCase = true) ->
                            "https://www.youtube.com"
            
                        else -> null
                    }
                    
                    val referer = when {
                        c.equals("WEB_REMIX", ignoreCase = true) ||
                            c.equals("WEB", ignoreCase = true) ||
                            c.equals("WEB_CREATOR", ignoreCase = true) ->
                            "https://music.youtube.com/"
            
                        c.equals("TVHTML5", ignoreCase = true) ||
                            c.equals("TVHTML5_SIMPLY_EMBEDDED_PLAYER", ignoreCase = true) ||
                            c.equals("TVHTML5_SIMPLY", ignoreCase = true) ->
                            "https://www.youtube.com/tv"
            
                        else -> null
                    }
                    
                    val builder = request.newBuilder().header("User-Agent", ua)
                    if (origin != null) {
                        builder.header("Origin", origin)
                        builder.header("Referer", referer ?: "")
                    }
                    if (!c.contains("NO_AUTH", ignoreCase = true) && !c.contains("TVHTML5_SIMPLY", ignoreCase = true)) {
                        YouTube.cookie?.let { builder.header("Cookie", it) }
                    }
                    
                    chain.proceed(builder.build())
                }
                .proxyAuthenticator { _, response ->
                    YouTube.proxyAuth?.let { auth ->
                        response.request.newBuilder()
                            .header("Proxy-Authorization", auth)
                            .build()
                    } ?: response.request
                }
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .build()
        )

        val resolvingDataSourceFactory = androidx.media3.datasource.ResolvingDataSource.Factory(okHttpDataSourceFactory) { dataSpec ->
            if (dataSpec.uri.scheme == "yt") {
                val videoId = dataSpec.uri.host ?: dataSpec.uri.toString().removePrefix("yt://")
                
                activeResolutions.incrementAndGet()
                acquireLocks()

                val streamUrl = try {
                    kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                        com.mrtdk.liquid_glass.playback.MusicPlayer.resolveUrl(videoId)
                    }
                } finally {
                    activeResolutions.decrementAndGet()
                    mainHandler.post { updateWakeLocks() }
                }

                if (streamUrl != null) {
                    dataSpec.withUri(android.net.Uri.parse(streamUrl))
                } else {
                    dataSpec
                }
            } else {
                dataSpec
            }
        }

        val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, resolvingDataSourceFactory)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
            .setDataSourceFactory(defaultDataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
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
            private var lastFailedMediaId: String? = null
            private var retryCount = 0

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateWakeLocks()
                checkForegroundState()

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
                val isNetworkError = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                        error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                        error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                        error.cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException ||
                        error.cause is java.io.IOException

                if (isNetworkError && currentMediaId != null) {
                    if (currentMediaId == lastFailedMediaId) {
                        retryCount++
                    } else {
                        lastFailedMediaId = currentMediaId
                        retryCount = 1
                    }

                    if (retryCount <= 3) {
                        android.util.Log.w("MusicService", "Playback failed with network error (code ${error.errorCode}), retrying ($retryCount/3): ${error.message}")
                        com.mrtdk.liquid_glass.playback.MusicPlayer.clearCache(currentMediaId)
                        player.prepare()
                        player.play()
                    } else {
                        android.util.Log.e("MusicService", "Max retries reached for mediaId $currentMediaId. Stopping playback.")
                    }
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

    companion object {
        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
    }
}
