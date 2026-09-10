package com.mrtdk.liquid_glass.ui.components

import android.view.LayoutInflater
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mrtdk.liquid_glass.R

object CanvasVideoCache {
    @Volatile
    private var cache: androidx.media3.datasource.cache.SimpleCache? = null
    @Volatile
    private var cacheDataSourceFactory: androidx.media3.datasource.cache.CacheDataSource.Factory? = null

    @Synchronized
    fun getCacheDataSourceFactory(context: android.content.Context): androidx.media3.datasource.cache.CacheDataSource.Factory {
        cacheDataSourceFactory?.let { return it }

        val appContext = context.applicationContext
        val cacheDir = appContext.cacheDir.resolve("canvas_video_cache")
        val evictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024L) // 100 MB max for motion covers
        val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(appContext)

        val simpleCache = try {
            androidx.media3.datasource.cache.SimpleCache(cacheDir, evictor, databaseProvider)
        } catch (_: Exception) {
            try {
                cacheDir.deleteRecursively()
                androidx.media3.datasource.cache.SimpleCache(cacheDir, evictor, databaseProvider)
            } catch (_: Exception) {
                null
            }
        }
        cache = simpleCache

        val upstreamFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setUserAgent("RayMusic/1.0")

        val factory = if (simpleCache != null) {
            androidx.media3.datasource.cache.CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } else {
            androidx.media3.datasource.cache.CacheDataSource.Factory()
                .setUpstreamDataSourceFactory(upstreamFactory)
        }

        cacheDataSourceFactory = factory
        return factory
    }
}

object AnimatedArtworkCache {
    private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun cleanTerm(term: String): String {
        return com.mrtdk.liquid_glass.canvas.UnifiedCanvasProvider.normalizeCanvasSongTitle(term)
    }

    fun get(artist: String, albumOrTitle: String): String? {
        val cleanArtist = com.mrtdk.liquid_glass.canvas.UnifiedCanvasProvider.normalizeCanvasArtistName(artist)
        val cleanTitle = cleanTerm(albumOrTitle)
        val key = "echo_motion_v3_${cleanArtist}_${cleanTitle}".lowercase().trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
        memoryCache[key]?.let { return it }
        val persisted = com.mrtdk.liquid_glass.data.LibraryManager.getString(key)
        if (!persisted.isNullOrBlank()) {
            if (persisted.contains("m8tec.top")) {
                com.mrtdk.liquid_glass.data.LibraryManager.saveString(key, "")
                return null
            }
            memoryCache[key] = persisted
            return persisted
        }
        return null
    }

    fun getForSong(artist: String, title: String, album: String? = null): String? {
        get(artist, title)?.let { return it }
        if (!album.isNullOrBlank()) {
            get(artist, album)?.let { return it }
        }
        return null
    }

    fun put(artist: String, albumOrTitle: String, url: String) {
        if (url.isBlank() || url.contains("m8tec.top")) return
        val cleanArtist = com.mrtdk.liquid_glass.canvas.UnifiedCanvasProvider.normalizeCanvasArtistName(artist)
        val cleanTitle = cleanTerm(albumOrTitle)
        val key = "echo_motion_v3_${cleanArtist}_${cleanTitle}".lowercase().trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
        memoryCache[key] = url
        com.mrtdk.liquid_glass.data.LibraryManager.saveString(key, url)
    }

    fun putForSong(artist: String, title: String, album: String? = null, url: String) {
        put(artist, title, url)
        if (!album.isNullOrBlank()) {
            put(artist, album, url)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AnimatedArtworkPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    enableFrameCapture: Boolean = false,
    isPaused: Boolean = false,
    syncWithPlayer: ExoPlayer? = null,
    onPlayerCreated: (ExoPlayer) -> Unit = {},
    onPlaybackStarted: () -> Unit = {},
    onFrameCaptured: ((android.graphics.Bitmap) -> Unit)? = null,
    cornerRadius: Dp = 0.dp,
    clipToBounds: Boolean = false
) {
    val context = LocalContext.current
    var isFirstFrameRendered by remember(videoUrl) { mutableStateOf(false) }

    // Initialize ExoPlayer with disk-cached media source to eliminate runaway data usage
    val exoPlayer = remember {
        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSize(1080, 1920)
                    .setMaxVideoFrameRate(60)
            )
        }
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
            CanvasVideoCache.getCacheDataSourceFactory(context)
        )
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .build().apply {
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
                    .build()
                playWhenReady = !isPaused
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f // Mute
            }
    }

    LaunchedEffect(exoPlayer) {
        onPlayerCreated(exoPlayer)
    }

    // Handle smooth synchronization without decoder stalls
    LaunchedEffect(syncWithPlayer, exoPlayer) {
        val master = syncWithPlayer ?: return@LaunchedEffect
        
        // Immediate sync upon connection
        exoPlayer.playWhenReady = master.playWhenReady
        if (master.playbackState == Player.STATE_READY || master.playbackState == Player.STATE_BUFFERING) {
            val drift = kotlin.math.abs(exoPlayer.currentPosition - master.currentPosition)
            if (drift > 1000) {
                exoPlayer.seekTo(master.currentPosition)
            }
        }

        val syncListener = object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                exoPlayer.seekTo(newPosition.positionMs)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                exoPlayer.playWhenReady = isPlaying
                val drift = kotlin.math.abs(exoPlayer.currentPosition - master.currentPosition)
                if (drift > 1000) {
                    exoPlayer.seekTo(master.currentPosition)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    exoPlayer.playWhenReady = master.playWhenReady
                    val drift = kotlin.math.abs(exoPlayer.currentPosition - master.currentPosition)
                    if (drift > 1000) {
                        exoPlayer.seekTo(master.currentPosition)
                    }
                }
            }
        }
        master.addListener(syncListener)

        // Low-overhead drift correction (checks once every 1.5s instead of aggressive 20ms seek loop)
        try {
            while (true) {
                if (master.isPlaying && !isPaused) {
                    val drift = kotlin.math.abs(exoPlayer.currentPosition - master.currentPosition)
                    if (drift > 1500) {
                        exoPlayer.seekTo(master.currentPosition)
                    }
                }
                kotlinx.coroutines.delay(1500)
            }
        } finally {
            master.removeListener(syncListener)
        }
    }

    // Handle ExoPlayer lifecycle & frame callback
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
                onPlaybackStarted()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Set media source when URL changes
    LaunchedEffect(videoUrl) {
        isFirstFrameRendered = false
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.prepare()
        if (syncWithPlayer != null) {
            exoPlayer.seekTo(syncWithPlayer.currentPosition)
        }
    }

    LaunchedEffect(isPaused) {
        exoPlayer.playWhenReady = !isPaused
    }

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    LaunchedEffect(playerViewRef, videoUrl, enableFrameCapture, onFrameCaptured, isPaused) {
        if (!enableFrameCapture || onFrameCaptured == null || isPaused) return@LaunchedEffect
        val pView = playerViewRef ?: return@LaunchedEffect
        // Wait for player to be ready and playing
        while (exoPlayer.playbackState != Player.STATE_READY) {
            kotlinx.coroutines.delay(100)
        }

        var textureView: TextureView? = null
        for (i in 0 until 30) {
            textureView = (pView.videoSurfaceView as? TextureView) ?: findTextureView(pView)
            if (textureView != null && textureView.isAvailable) break
            kotlinx.coroutines.delay(100)
        }

        val tv = textureView ?: return@LaunchedEffect
        while (!tv.isAvailable) {
            kotlinx.coroutines.delay(50)
        }

        // Immediately capture first frame without waiting
        try {
            val initialBmp = tv.getBitmap(120, 160)
            if (initialBmp != null) {
                onFrameCaptured(initialBmp)
            }
        } catch (_: Exception) { }

        // Periodically capture the frame of the TextureView
        val reusableBmp = android.graphics.Bitmap.createBitmap(120, 160, android.graphics.Bitmap.Config.ARGB_8888)
        while (true) {
            if (exoPlayer.isPlaying && enableFrameCapture && !isPaused && tv.isAvailable) {
                try {
                    val bmp = tv.getBitmap(reusableBmp)
                    if (bmp != null) {
                        onFrameCaptured(bmp)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(90) // Optimal live reflection updates with minimal GPU/CPU load
            } else {
                kotlinx.coroutines.delay(200) // Sleep when paused or frame capture disabled
            }
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFirstFrameRendered && !isPaused) 1f else 0f,
        animationSpec = tween(250),
        label = "animatedArtworkAlpha"
    )

    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }

    // Reusable outline provider that does not allocate per-frame
    val outlineProvider = remember(cornerRadiusPx, clipToBounds) {
        if (clipToBounds || cornerRadiusPx > 0f) {
            object : android.view.ViewOutlineProvider() {
                override fun getOutline(v: android.view.View, outline: android.graphics.Outline) {
                    if (cornerRadiusPx > 0f) {
                        outline.setRoundRect(0, 0, v.width, v.height, cornerRadiusPx)
                    } else {
                        outline.setRect(0, 0, v.width, v.height)
                    }
                }
            }
        } else null
    }

    // Render using AndroidView without consuming touch gestures, hidden until first frame is rendered
    AndroidView(
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.player_view_texture, null) as PlayerView).also { view ->
                view.useController = false
                view.isClickable = false
                view.isFocusable = false
                view.setOnTouchListener { _, _ -> false }
                if (outlineProvider != null) {
                    view.clipToOutline = true
                    view.outlineProvider = outlineProvider
                }
                playerViewRef = view
            }
        },
        update = { view ->
            view.player = exoPlayer
            view.isClickable = false
            view.isFocusable = false
            view.setOnTouchListener { _, _ -> false }
            if (outlineProvider != null) {
                view.clipToOutline = true
                view.outlineProvider = outlineProvider
            } else {
                view.clipToOutline = false
                view.outlineProvider = null
            }
            playerViewRef = view
        },
        modifier = modifier
            .then(
                if (cornerRadius > 0.dp) {
                    Modifier.clip(RoundedCornerShape(cornerRadius))
                } else if (clipToBounds) {
                    Modifier.clipToBounds()
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                alpha = animatedAlpha
            }
    )
}

private fun findTextureView(view: android.view.ViewGroup): TextureView? {
    for (i in 0 until view.childCount) {
        val child = view.getChildAt(i)
        if (child is TextureView) {
            return child
        } else if (child is android.view.ViewGroup) {
            val tv = findTextureView(child)
            if (tv != null) return tv
        }
    }
    return null
}
