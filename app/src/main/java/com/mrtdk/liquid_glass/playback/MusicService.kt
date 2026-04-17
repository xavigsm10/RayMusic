package com.mrtdk.liquid_glass.playback

import android.app.PendingIntent
import android.content.Intent
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

@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer

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

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
            .setDataSourceFactory(okHttpDataSourceFactory)

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

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
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
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
