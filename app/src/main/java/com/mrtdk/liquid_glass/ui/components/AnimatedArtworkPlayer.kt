package com.mrtdk.liquid_glass.ui.components

import android.view.LayoutInflater
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        var cleaned = term
        // Remove feat/ft/with
        cleaned = cleaned.replace(Regex("(?i)\\b(feat\\.?|ft\\.?|with)\\b.*"), "")
        // Remove deluxe, remaster, live, ep, mono, version, etc.
        cleaned = cleaned.replace(Regex("(?i)\\b(remastered|remaster|deluxe|anniversary|live|special|explicit|single|ep|mono|stereo|re-recorded|edition|version)\\b.*"), "")
        // Remove parentheses/brackets and contents
        cleaned = cleaned.replace(Regex("\\([^\\)]*\\)"), "")
        cleaned = cleaned.replace(Regex("\\[[^\\]]*\\]"), "")
        // Remove trailing dashes and spaces
        cleaned = cleaned.replace(Regex("[-–—:_\\s]+$"), "")
        cleaned = cleaned.trim().replace(Regex("\\s+"), " ")
        return cleaned.ifEmpty { term }
    }

    fun get(artist: String, albumOrTitle: String): String? {
        val cleanArtist = cleanTerm(artist)
        val cleanAlbum = cleanTerm(albumOrTitle)
        val key = "anim_art_${cleanArtist}_${cleanAlbum}".lowercase().trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
        memoryCache[key]?.let { return it }
        val persisted = com.mrtdk.liquid_glass.data.LibraryManager.getString(key)
        if (persisted != null) {
            memoryCache[key] = persisted
            return persisted
        }
        return null
    }

    fun put(artist: String, albumOrTitle: String, url: String) {
        val cleanArtist = cleanTerm(artist)
        val cleanAlbum = cleanTerm(albumOrTitle)
        val key = "anim_art_${cleanArtist}_${cleanAlbum}".lowercase().trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
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
    onPlaybackStarted: () -> Unit = {},
    onFrameCaptured: (android.graphics.Bitmap) -> Unit = {}
) {
    val context = LocalContext.current

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

    // Handle ExoPlayer lifecycle
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
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
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.prepare()
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
        for (i in 0 until 10) {
            textureView = findTextureView(pView)
            if (textureView != null) break
            kotlinx.coroutines.delay(100)
        }

        val tv = textureView ?: return@LaunchedEffect

        // Periodically capture the frame of the TextureView
        while (true) {
            try {
                // Fetch a very small low-res bitmap to minimize overhead
                val bmp = tv.getBitmap(150, 150)
                if (bmp != null) {
                    onFrameCaptured(bmp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.delay(1000) // Capture frame every 1 second
        }
    }

    // Render using AndroidView
    AndroidView(
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.player_view_texture, null) as PlayerView).also {
                playerViewRef = it
            }
        },
        update = { view ->
            view.player = exoPlayer
            playerViewRef = view
        },
        modifier = modifier
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
