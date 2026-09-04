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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mrtdk.liquid_glass.R

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

    fun put(artist: String, albumOrTitle: String, url: String) {
        if (url.isBlank() || url.contains("m8tec.top")) return
        val cleanArtist = com.mrtdk.liquid_glass.canvas.UnifiedCanvasProvider.normalizeCanvasArtistName(artist)
        val cleanTitle = cleanTerm(albumOrTitle)
        val key = "echo_motion_v3_${cleanArtist}_${cleanTitle}".lowercase().trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
        memoryCache[key] = url
        com.mrtdk.liquid_glass.data.LibraryManager.saveString(key, url)
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AnimatedArtworkPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    enableFrameCapture: Boolean = true,
    isPaused: Boolean = false,
    syncWithPlayer: ExoPlayer? = null,
    onPlayerCreated: (ExoPlayer) -> Unit = {},
    onPlaybackStarted: () -> Unit = {},
    onFrameCaptured: (android.graphics.Bitmap) -> Unit = {}
) {
    val context = LocalContext.current
    var isFirstFrameRendered by remember(videoUrl) { mutableStateOf(false) }

    // Initialize ExoPlayer inside remember to keep instance alive
    val exoPlayer = remember {
        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setForceHighestSupportedBitrate(true)
            )
        }
        ExoPlayer.Builder(context)
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

    // Handle synchronization if syncWithPlayer is provided
    LaunchedEffect(syncWithPlayer, exoPlayer) {
        val master = syncWithPlayer ?: return@LaunchedEffect
        
        // Immediate sync upon connection
        exoPlayer.playWhenReady = master.playWhenReady
        if (master.playbackState == Player.STATE_READY || master.playbackState == Player.STATE_BUFFERING) {
            val drift = kotlin.math.abs(exoPlayer.currentPosition - master.currentPosition)
            if (drift > 20) {
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
                if (drift > 20) {
                    exoPlayer.seekTo(master.currentPosition)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    exoPlayer.playWhenReady = master.playWhenReady
                    val drift = kotlin.math.abs(exoPlayer.currentPosition - master.currentPosition)
                    if (drift > 20) {
                        exoPlayer.seekTo(master.currentPosition)
                    }
                }
            }
        }
        master.addListener(syncListener)

        // Continuous fine-grained synchronization
        try {
            while (true) {
                if (master.isPlaying) {
                    val drift = kotlin.math.abs(exoPlayer.currentPosition - master.currentPosition)
                    if (drift > 20) {
                        exoPlayer.seekTo(master.currentPosition)
                    }
                }
                kotlinx.coroutines.delay(20)
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

    LaunchedEffect(playerViewRef, videoUrl, enableFrameCapture) {
        if (!enableFrameCapture) return@LaunchedEffect
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
        } catch (e: Exception) { }

        // Periodically capture the frame of the TextureView
        val reusableBmp = android.graphics.Bitmap.createBitmap(120, 160, android.graphics.Bitmap.Config.ARGB_8888)
        while (true) {
            if (exoPlayer.isPlaying && enableFrameCapture && tv.isAvailable) {
                try {
                    val bmp = tv.getBitmap(reusableBmp)
                    if (bmp != null) {
                        onFrameCaptured(bmp)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            kotlinx.coroutines.delay(35) // Smooth ~28-30fps live reflection & blur curve updates with 0 heap allocations
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFirstFrameRendered) 1f else 0f,
        animationSpec = tween(250),
        label = "animatedArtworkAlpha"
    )

    // Render using AndroidView without consuming touch gestures, hidden until first frame is rendered
    AndroidView(
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.player_view_texture, null) as PlayerView).also { view ->
                view.useController = false
                view.isClickable = false
                view.isFocusable = false
                view.setOnTouchListener { _, _ -> false }
                playerViewRef = view
            }
        },
        update = { view ->
            view.player = exoPlayer
            view.isClickable = false
            view.isFocusable = false
            view.setOnTouchListener { _, _ -> false }
            playerViewRef = view
        },
        modifier = modifier.graphicsLayer {
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
