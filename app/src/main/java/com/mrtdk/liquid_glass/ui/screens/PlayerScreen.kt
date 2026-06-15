package com.mrtdk.liquid_glass.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mrtdk.liquid_glass.R
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clipToBounds
import com.skydoves.cloudy.cloudy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.mrtdk.glass.GlassContainer
import com.mrtdk.glass.GlassBox
import com.mrtdk.liquid_glass.ui.components.PlayerOptionsMenu
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.app.DownloadManager
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import androidx.compose.material.icons.filled.ArrowDownward
import kotlinx.coroutines.launch
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import com.echo.innertube.YouTube
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlayerState(
    val title: String,
    val artist: String,
    val artUrl: Any?,
    val videoId: String? = null,
    val contentUri: Uri? = null,
    val duration: Long = 0L,
    val queue: List<QueueItem> = emptyList(),
    val isExclusiveQueue: Boolean = false,
    val album: String? = null,
    val albumId: String? = null,
    val playlistId: String? = null,
    val playlistName: String? = null
)

data class QueueItem(
    val title: String,
    val artist: String,
    val artUrl: Any?,
    val videoId: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val playlistId: String? = null,
    val playlistName: String? = null
)

private fun extractDominantColor(bitmap: android.graphics.Bitmap, isBillieJean: Boolean): Color {
    val palette = androidx.palette.graphics.Palette.from(bitmap).maximumColorCount(8).generate()
    val domRgb = palette.getDominantColor(android.graphics.Color.DKGRAY)
    val vibRgb = palette.getVibrantColor(domRgb)
    val mutedRgb = palette.getMutedColor(domRgb)
    
    val chosenRgb = if (vibRgb != domRgb && Color(vibRgb).luminance() > 0.1f) {
        vibRgb
    } else if (mutedRgb != domRgb && Color(mutedRgb).luminance() > 0.1f) {
        mutedRgb
    } else {
        domRgb
    }
    
    val extractedColor = Color(chosenRgb)
    
    return if (isBillieJean) {
        val skinTone = Color(0xFF6E472A) 
        Color(
            red = (extractedColor.red * 0.5f + skinTone.red * 0.5f),
            green = (extractedColor.green * 0.5f + skinTone.green * 0.5f),
            blue = (extractedColor.blue * 0.5f + skinTone.blue * 0.5f),
            alpha = 1f
        )
    } else {
        extractedColor
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerState: PlayerState?,
    isVisible: Boolean,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isBottomBarCollapsed: Boolean = false,
    upNextSongs: List<SongItem> = emptyList(),
    onUpNextSongsChange: (List<SongItem>) -> Unit = {},
    songHistory: List<PlayerState> = emptyList(),
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onArtistSelected: (com.mrtdk.liquid_glass.ui.screens.ArtistState) -> Unit = {},
    onAlbumSelected: (com.mrtdk.liquid_glass.ui.screens.AlbumState) -> Unit = {},
    onSongSelected: (PlayerState) -> Unit = {},
    onSongSelectedFromQueue: (PlayerState) -> Unit = {},
    shuffleModeEnabled: Boolean = false,
    repeatMode: Int = androidx.media3.common.Player.REPEAT_MODE_OFF,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onDominantColorChanged: (Color) -> Unit = {}
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.slideInVertically(
               initialOffsetY = { it },
               animationSpec = androidx.compose.animation.core.tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing)
           ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
        exit = androidx.compose.animation.slideOutVertically(
               targetOffsetY = { it },
               animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
           ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
    ) {
        if (playerState == null) return@AnimatedVisibility

        val context = LocalContext.current
        val localBackdrop = rememberLayerBackdrop()
        val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
        val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() }
        var volumePosition by remember { 
            mutableFloatStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) / maxVolume)
        }
        val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
        val scope = rememberCoroutineScope()
        val dragOffsetY = remember { Animatable(0f) }
        val offsetY = dragOffsetY.value
        
        val queueListState = androidx.compose.foundation.lazy.rememberLazyListState()
        val lyricsListState = androidx.compose.foundation.lazy.rememberLazyListState()
        var showQueue by remember { mutableStateOf(false) }
        var showLyrics by remember { mutableStateOf(false) }
        var showLyricsControls by remember { mutableStateOf(true) }
        var lyricsControlsHideTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(showLyrics) {
            if (showLyrics) {
                showLyricsControls = true
                lyricsControlsHideTrigger++
            }
        }

        LaunchedEffect(showLyrics, showLyricsControls, lyricsControlsHideTrigger) {
            if (showLyrics && showLyricsControls) {
                kotlinx.coroutines.delay(2000L)
                showLyricsControls = false
            }
        }
        
        var showOptionsMenu by remember { mutableStateOf(false) }
        var showLyricsMenu by remember { mutableStateOf(false) }
        var showPlaylistMenu by remember { mutableStateOf(false) }
        var showNewPlaylistDialog by remember { mutableStateOf(false) }
        
        var swipeDirection by remember { mutableIntStateOf(1) }
        val isBadSong = remember(playerState) {
            val title = playerState?.title ?: ""
            val artist = playerState?.artist ?: ""
            val album = playerState?.album ?: ""
            (album.contains("Bad", ignoreCase = true) || title.contains("Bad", ignoreCase = true)) &&
            artist.contains("Michael Jackson", ignoreCase = true)
        }
        val hdArtUrl = remember(playerState?.artUrl, playerState?.title, playerState?.artist) {
            val url = playerState?.artUrl ?: return@remember null
            val urlString = url.toString()
            if (urlString.startsWith("file:///android_asset/")) {
                url
            } else {
                val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(urlString) ?: urlString
                if (url is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
            }
        }

        var dominantColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }
        var bottomAverageColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }
        var rightSideAverageColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }
        var parentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var sliderCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

        var lyricsLines by remember { mutableStateOf<List<com.mrtdk.liquid_glass.utils.LyricLine>?>(null) }
        var showManualLyricsSearch by remember { mutableStateOf(false) }
        var manualLyricsQueryTitle by remember { mutableStateOf(playerState?.title ?: "") }
        var manualLyricsQueryArtist by remember { mutableStateOf(playerState?.artist ?: "") }

        var selectedLyricsProvider by remember { mutableStateOf("LRCLIB") }
        var isRomajiEnabled by remember { mutableStateOf(false) }

        LaunchedEffect(playerState?.title, playerState?.artist, selectedLyricsProvider, isRomajiEnabled) {
            lyricsLines = null
            if (playerState != null) {
                launch {
                    val lines = when (selectedLyricsProvider) {
                        "KuGou" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchKuGouLyrics(playerState.title, playerState.artist)
                        "BetterLyrics" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchBetterLyrics(playerState.title, playerState.artist)
                        "LyricsPlus" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchLyricsPlus(playerState.title, playerState.artist)
                        "SimpMusic" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchSimpMusicLyrics(playerState.title, playerState.artist)
                        "YouTube Music" -> playerState.videoId?.let { com.mrtdk.liquid_glass.utils.LyricsProvider.fetchYouTubeLyrics(it) }
                        "YouTube Subtitle" -> playerState.videoId?.let { com.mrtdk.liquid_glass.utils.LyricsProvider.fetchYouTubeSubtitleLyrics(it) }
                        else -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchLyrics(playerState.title, playerState.artist)
                    }
                    if (isRomajiEnabled && lines != null) {
                        val prefs = com.mrtdk.liquid_glass.utils.LyricsRomanizationPreferences(true, true, true, true, true)
                        lyricsLines = lines.map { line ->
                            val romanized = com.mrtdk.liquid_glass.utils.LyricsUtils.romanizeLyricsLine(line.text, prefs)
                            if (romanized != null) {
                                line.copy(text = romanized)
                            } else {
                                line
                            }
                        }
                    } else {
                        lyricsLines = lines
                    }
                }
            }
        }

        var animatedArtworkUrl by remember(playerState?.artist, playerState?.album, playerState?.title) {
            val artist = playerState?.artist
            val album = playerState?.album
            val title = playerState?.title
            val cached = if (!artist.isNullOrBlank()) {
                val cacheKey = if (!album.isNullOrBlank()) album else title
                if (!cacheKey.isNullOrBlank()) {
                    com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.get(artist, cacheKey)
                } else null
            } else null
            mutableStateOf(cached)
        }
        var isVideoPlaying by remember { mutableStateOf(false) }

        LaunchedEffect(playerState?.artist, playerState?.album, playerState?.title) {
            val artist = playerState?.artist
            val album = playerState?.album
            val title = playerState?.title
            isVideoPlaying = false
            
            if (artist.isNullOrBlank()) return@LaunchedEffect
            if (animatedArtworkUrl != null) return@LaunchedEffect
            
            withContext(Dispatchers.IO) {
                var foundUrl: String? = null
                
                // Try 1: Search using album name if available
                if (!album.isNullOrBlank()) {
                    try {
                        val cleanArtist = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(artist)
                        val cleanAlbum = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(album)
                        val encodedArtist = java.net.URLEncoder.encode(cleanArtist, "UTF-8")
                        val encodedAlbum = java.net.URLEncoder.encode(cleanAlbum, "UTF-8")
                        val url = java.net.URL("https://artwork.m8tec.top/api/v1/artwork/search?artist=$encodedArtist&album=$encodedAlbum")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 3000
                        conn.readTimeout = 3000
                        if (conn.responseCode == 200) {
                            val text = conn.inputStream.bufferedReader().readText()
                            val obj = org.json.JSONObject(text)
                            val streamUrl = obj.optString("url").takeIf { it.isNotBlank() } 
                                ?: obj.optString("url_tall").takeIf { it.isNotBlank() }
                            if (!streamUrl.isNullOrBlank()) {
                                foundUrl = streamUrl
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Try 2: Fallback or query using song title if not found yet
                if (foundUrl == null && !title.isNullOrBlank()) {
                    try {
                        val cleanArtist = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(artist)
                        val cleanTitle = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(title)
                        val encodedArtist = java.net.URLEncoder.encode(cleanArtist, "UTF-8")
                        val encodedTitle = java.net.URLEncoder.encode(cleanTitle, "UTF-8")
                        val url = java.net.URL("https://artwork.m8tec.top/api/v1/artwork/search?artist=$encodedArtist&album=$encodedTitle")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 3000
                        conn.readTimeout = 3000
                        if (conn.responseCode == 200) {
                            val text = conn.inputStream.bufferedReader().readText()
                            val obj = org.json.JSONObject(text)
                            val streamUrl = obj.optString("url").takeIf { it.isNotBlank() } 
                                ?: obj.optString("url_tall").takeIf { it.isNotBlank() }
                            if (!streamUrl.isNullOrBlank()) {
                                foundUrl = streamUrl
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                if (foundUrl != null) {
                    withContext(Dispatchers.Main) {
                        animatedArtworkUrl = foundUrl
                        val cacheKeyAlbum = if (!album.isNullOrBlank()) album else title ?: ""
                        com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.put(artist, cacheKeyAlbum, foundUrl!!)
                    }
                }
            }
        }

        var coverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(hdArtUrl) {
            coverBitmap = null
            if (hdArtUrl != null) {
                val request = ImageRequest.Builder(context)
                    .data(hdArtUrl)
                    .allowHardware(false)
                    .size(300)
                    .memoryCachePolicy(coil.request.CachePolicy.READ_ONLY)
                    .build()
                val result = coil.Coil.imageLoader(context).execute(request)
                if (result is coil.request.SuccessResult) {
                    val drawable = result.drawable
                    val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        ?: android.graphics.Bitmap.createBitmap(
                            drawable.intrinsicWidth.coerceAtLeast(1),
                            drawable.intrinsicHeight.coerceAtLeast(1),
                            android.graphics.Bitmap.Config.ARGB_8888
                        ).also {
                            val canvas = android.graphics.Canvas(it)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                        }
                    if (bitmap != null) {
                        coverBitmap = bitmap.asImageBitmap()
                        try {
                            // Mismo método de los álbumes (promedio de la fila inferior de píxeles)
                            var r = 0L; var g = 0L; var b = 0L
                            val yCoord = bitmap.height - 1
                            val w = bitmap.width
                            for (x in 0 until w) {
                                val pixel = bitmap.getPixel(x, yCoord)
                                r += android.graphics.Color.red(pixel)
                                g += android.graphics.Color.green(pixel)
                                b += android.graphics.Color.blue(pixel)
                            }
                            val avgColor = Color((r / w).toInt(), (g / w).toInt(), (b / w).toInt())
                            bottomAverageColor = avgColor
                            dominantColor = avgColor
                            onDominantColorChanged(avgColor)

                            // Promedio de la columna derecha de píxeles
                            var rRight = 0L; var gRight = 0L; var bRight = 0L
                            val xCoord = bitmap.width - 1
                            val h = bitmap.height
                            for (y in 0 until h) {
                                val pixel = bitmap.getPixel(xCoord, y)
                                rRight += android.graphics.Color.red(pixel)
                                gRight += android.graphics.Color.green(pixel)
                                bRight += android.graphics.Color.blue(pixel)
                            }
                            rightSideAverageColor = Color((rRight / h).toInt(), (gRight / h).toInt(), (bRight / h).toInt())
                        } catch (e: Exception) { }
                    }
                }
            }
        }
        
        val isLightBackground = bottomAverageColor.luminance() > 0.35f
        val contentColor = if (isLightBackground) Color(0xFF1A1A1A) else Color.White

        val animatedImageLoader = remember(context) {
            coil.ImageLoader.Builder(context)
                .components {
                    add(com.mrtdk.liquid_glass.utils.CoilUtils.HdThumbnailInterceptor())
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        add(coil.decode.ImageDecoderDecoder.Factory())
                    } else {
                        add(coil.decode.GifDecoder.Factory())
                    }
                }
                .build()
        }

        val savedItems by LibraryManager.savedItems.collectAsState()
        val isSaved = savedItems.any { it.id == playerState?.videoId }
        val starTint by androidx.compose.animation.animateColorAsState(targetValue = if(isSaved) Color(0xFFFA243C) else contentColor, label="starTint")

        GlassContainer(
            modifier = Modifier.fillMaxSize(),
            useShader = true,
            content = {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { parentCoordinates = it }
                        .layerBackdrop(localBackdrop)
                ) {
            val maxWidth = maxWidth
            val maxHeight = maxHeight

            val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                LandscapePlayerLayout(
                    playerState = playerState,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    upNextSongs = upNextSongs,
                    onUpNextSongsChange = onUpNextSongsChange,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onClose = onClose,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeek,
                    onVolumeChange = onVolumeChange,
                    onArtistSelected = onArtistSelected,
                    onAlbumSelected = onAlbumSelected,
                    onSongSelected = onSongSelected,
                    onSongSelectedFromQueue = onSongSelectedFromQueue,
                    shuffleModeEnabled = shuffleModeEnabled,
                    repeatMode = repeatMode,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    showLyrics = showLyrics,
                    onShowLyricsChange = { showLyrics = it },
                    showQueue = showQueue,
                    onShowQueueChange = { showQueue = it },
                    volumePosition = volumePosition,
                    onVolumePositionChange = { volumePosition = it },
                    coverBitmap = coverBitmap,
                    onCoverBitmapChange = { coverBitmap = it },
                    dominantColor = dominantColor,
                    onDominantColorChange = { dominantColor = it },
                    bottomAverageColor = bottomAverageColor,
                    onBottomAverageColorChange = { bottomAverageColor = it },
                    rightSideAverageColor = rightSideAverageColor,
                    onRightSideAverageColorChange = { rightSideAverageColor = it },
                    hdArtUrl = hdArtUrl,
                    lyricsLines = lyricsLines,
                    isRomajiEnabled = isRomajiEnabled,
                    onToggleRomaji = { isRomajiEnabled = !isRomajiEnabled },
                    isSaved = isSaved,
                    animatedArtworkUrl = animatedArtworkUrl,
                    isVideoPlaying = isVideoPlaying,
                    onVideoPlayingChange = { isVideoPlaying = it },
                    animatedImageLoader = animatedImageLoader,
                    isLightBackground = isLightBackground,
                    contentColor = contentColor,
                    onShowOptionsMenu = { showOptionsMenu = true },
                    onShowLyricsMenu = { showLyricsMenu = true },
                    onShowPlaylistMenu = { showPlaylistMenu = true },
                    isBottomBarCollapsed = isBottomBarCollapsed
                )
            } else {
                val lyricsImageSize = 84.dp
            val isOverlayActive = showLyrics || showQueue
            
            // Calculating destinations depending on normal vs collapsed bottom bar
            val normalTargetOffsetX = 28.dp
            val collapsedTargetOffsetX = 92.dp
            val normalTargetOffsetY = maxHeight - 148.dp
            val collapsedTargetOffsetY = maxHeight - 64.dp
            
            val targetOffsetX = if (isBottomBarCollapsed) collapsedTargetOffsetX else normalTargetOffsetX
            val targetOffsetY = if (isBottomBarCollapsed) collapsedTargetOffsetY else normalTargetOffsetY
            
            val density = androidx.compose.ui.platform.LocalDensity.current
            val maxDragDistance = with(density) { targetOffsetY.toPx() }
            val sliderYDp = remember(sliderCoordinates, parentCoordinates) {
                val sliderCoords = sliderCoordinates
                val parentCoords = parentCoordinates
                if (sliderCoords != null && parentCoords != null && sliderCoords.isAttached && parentCoords.isAttached) {
                    val localOffset = parentCoords.localPositionOf(sliderCoords, Offset.Zero)
                    with(density) { localOffset.y.toDp() }
                } else {
                    0.dp
                }
            }
            val dragProgress = if (maxDragDistance > 0f) (offsetY / maxDragDistance).coerceIn(0f, 1f) else 0f
            
            val bgAlpha = 1f - dragProgress
            
            val expandedWidth = maxWidth
            val expandedHeight = maxWidth * 1.15f
            val expandedX = 0.dp
            val expandedY = 0.dp
            
            val startWidth = if (isOverlayActive) lyricsImageSize else expandedWidth
            val startHeight = if (isOverlayActive) lyricsImageSize else expandedHeight
            val startOffsetX = if (isOverlayActive) 24.dp else expandedX
            val startOffsetY = if (isOverlayActive) 64.dp else expandedY
            val startCorner = if (isOverlayActive) 8.dp else 12.dp

            val threshold = 0.70f
            
            val imgWidthTarget: androidx.compose.ui.unit.Dp
            val imgHeightTarget: androidx.compose.ui.unit.Dp
            val imgOffsetXTarget: androidx.compose.ui.unit.Dp
            val imgOffsetYTarget: androidx.compose.ui.unit.Dp
            val imageCornerTarget: androidx.compose.ui.unit.Dp
            val contentAlpha: Float
            val overlayAlpha: Float
            
            if (dragProgress <= threshold) {
                val p1 = if (threshold > 0f) dragProgress / threshold else 0f
                imgWidthTarget = startWidth
                imgHeightTarget = startHeight
                imgOffsetXTarget = startOffsetX
                imgOffsetYTarget = startOffsetY
                imageCornerTarget = startCorner
                contentAlpha = (1f - p1).coerceIn(0f, 1f)
                overlayAlpha = (1f - p1).coerceIn(0f, 1f)
            } else {
                val p2 = if (threshold < 1f) (dragProgress - threshold) / (1f - threshold) else 1f
                imgWidthTarget = androidx.compose.ui.unit.lerp(startWidth, 40.dp, p2)
                imgHeightTarget = androidx.compose.ui.unit.lerp(startHeight, 40.dp, p2)
                imgOffsetXTarget = androidx.compose.ui.unit.lerp(startOffsetX, targetOffsetX, p2)
                imgOffsetYTarget = androidx.compose.ui.unit.lerp(startOffsetY, 0.dp, p2)
                imageCornerTarget = androidx.compose.ui.unit.lerp(startCorner, 20.dp, p2)
                contentAlpha = 0f
                overlayAlpha = 0f
            }

            val imgWidth by androidx.compose.animation.core.animateDpAsState(imgWidthTarget)
            val imgHeight by androidx.compose.animation.core.animateDpAsState(imgHeightTarget)
            val imgOffsetX by androidx.compose.animation.core.animateDpAsState(imgOffsetXTarget)
            val imgOffsetY by androidx.compose.animation.core.animateDpAsState(imgOffsetYTarget)
            val imgCorner by androidx.compose.animation.core.animateDpAsState(imageCornerTarget)

            val nestedScrollConnection = remember(maxDragDistance, onClose, showLyrics, showQueue) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (showLyrics || showQueue) return Offset.Zero
                        val delta = available.y
                        val currentOffsetY = dragOffsetY.value
                        if (currentOffsetY > 0f && delta < 0f) {
                            val newOffset = (currentOffsetY + delta).coerceAtLeast(0f)
                            val consumed = newOffset - currentOffsetY
                            scope.launch { dragOffsetY.snapTo(newOffset) }
                            return Offset(0f, consumed)
                        }
                        return Offset.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (showLyrics || showQueue) return Offset.Zero
                        val delta = available.y
                        val currentOffsetY = dragOffsetY.value
                        
                        val isAtTop = when {
                            showLyrics -> lyricsListState.firstVisibleItemIndex == 0 && lyricsListState.firstVisibleItemScrollOffset == 0
                            showQueue -> queueListState.firstVisibleItemIndex == 0 && queueListState.firstVisibleItemScrollOffset == 0
                            else -> true
                        }
                        
                        if (delta > 0f && isAtTop) {
                            val newOffset = (currentOffsetY + delta * 0.7f).coerceAtLeast(0f)
                            scope.launch { dragOffsetY.snapTo(newOffset) }
                            return Offset(0f, delta)
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        if (showLyrics || showQueue) return Velocity.Zero
                        val currentOffsetY = dragOffsetY.value
                        if (currentOffsetY > with(density) { 150.dp.toPx() }) {
                            dragOffsetY.animateTo(
                                targetValue = maxDragDistance,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                            onClose()
                        } else {
                            dragOffsetY.animateTo(0f, spring())
                        }
                        return Velocity.Zero
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = offsetY
                    }
                    .background(bottomAverageColor.copy(alpha = bgAlpha))
                    .drawWithContent {
                        if (dragProgress < 1f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        bottomAverageColor.copy(alpha = 0.10f),
                                        bottomAverageColor.copy(alpha = 0.30f),
                                        bottomAverageColor.copy(alpha = 0.55f),
                                        bottomAverageColor.copy(alpha = 0.80f),
                                        bottomAverageColor
                                    ),
                                    startY = this.size.height * 0.42f,
                                    endY = this.size.height
                                )
                            )
                        }
                        drawContent()
                    }
                    .pointerInput(showLyrics, showQueue) {
                        if (!showLyrics && !showQueue) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    val currentOffsetY = dragOffsetY.value
                                    if (currentOffsetY > with(density) { 150.dp.toPx() }) {
                                        scope.launch {
                                            dragOffsetY.animateTo(
                                                targetValue = maxDragDistance,
                                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                                            )
                                            onClose()
                                        }
                                    } else {
                                        scope.launch {
                                            dragOffsetY.animateTo(0f, spring())
                                        }
                                    }
                                }
                            ) { change, dragAmount ->
                                if (dragAmount > 0f || dragOffsetY.value > 0f) {
                                    val newOffset = (dragOffsetY.value + dragAmount * 0.7f).coerceAtLeast(0f)
                                    scope.launch { dragOffsetY.snapTo(newOffset) }
                                }
                            }
                        }
                    }
            ) {

            // Capa 1: Reflejo Líquido Estirado 1D (Proyección vertical de la carátula)
            val currentCoverBitmap = coverBitmap
            if (currentCoverBitmap != null && dragProgress < 1f && !isOverlayActive) {
                val overlapDp = 30.dp
                val blurHeight = if (sliderYDp > 0.dp) {
                    (sliderYDp - (imgOffsetY + imgHeight - overlapDp)).coerceAtLeast(0.dp)
                } else {
                    maxHeight - (imgOffsetY + imgHeight) + overlapDp
                }

                // Caja del reflejo posicionada bajo la portada, llenando todo el espacio inferior
                Box(
                    modifier = Modifier
                        .offset(x = 0.dp, y = imgOffsetY + imgHeight - overlapDp)
                        .fillMaxWidth()
                        .height(blurHeight + 120.dp)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .blur(25.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = if (isBadSong) {
                                        arrayOf(
                                            0.0f to Color.Black,
                                            0.05f to Color.Black.copy(alpha = 0.4f),
                                            0.1f to Color.Transparent,
                                            1.0f to Color.Transparent
                                        )
                                    } else {
                                        arrayOf(
                                            0.0f to Color.Black,
                                            0.05f to Color.Black,
                                            0.15f to Color.Black.copy(alpha = 0.9f),
                                            0.30f to Color.Black.copy(alpha = 0.7f),
                                            0.45f to Color.Black.copy(alpha = 0.45f),
                                            0.60f to Color.Black.copy(alpha = 0.25f),
                                            0.75f to Color.Black.copy(alpha = 0.10f),
                                            0.88f to Color.Black.copy(alpha = 0.03f),
                                            1.0f to Color.Transparent
                                        )
                                    }
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val imgX = imgOffsetX.toPx()
                        val imgW = imgWidth.toPx()
                        val screenW = size.width
                        val screenH = size.height

                        val imgXInt = imgX.toInt()
                        val imgWInt = imgW.toInt()
                        val screenWInt = screenW.toInt()

                        val sampleHeight = 5 // Altura de muestreo del borde
                        val sampleH = sampleHeight.coerceAtMost(currentCoverBitmap.height).coerceAtLeast(1)

                        // Mapeo del recorte de la portada debido a ContentScale.Crop en la pantalla
                        val containerW = imgW
                        val containerH = imgHeight.toPx()
                        val bitmapW = currentCoverBitmap.width.toFloat()
                        val bitmapH = currentCoverBitmap.height.toFloat()

                        val containerRatio = containerW / containerH
                        val bitmapRatio = bitmapW / bitmapH

                        val srcX: Float
                        val srcWidth: Float
                        val srcY: Float

                        if (containerRatio < bitmapRatio) {
                            // El contenedor es proporcionalmente más alto (se recorta izq/der)
                            val scale = containerH / bitmapH
                            val visibleWidth = containerW / scale
                            srcX = (bitmapW - visibleWidth) / 2f
                            srcWidth = visibleWidth
                            srcY = bitmapH - sampleH
                        } else {
                            // El contenedor es proporcionalmente más ancho (se recorta arriba/abajo)
                            val scale = containerW / bitmapW
                            val visibleHeight = containerH / scale
                            srcX = 0f
                            srcWidth = bitmapW
                            val cropY = (bitmapH - visibleHeight) / 2f
                            val visibleBottomY = cropY + visibleHeight
                            srcY = (visibleBottomY - sampleH).coerceIn(0f, bitmapH - sampleH)
                        }

                        val srcXInt = srcX.toInt().coerceIn(0, currentCoverBitmap.width - 1)
                        val srcWInt = srcWidth.toInt().coerceIn(1, currentCoverBitmap.width - srcXInt)
                        val srcYInt = srcY.toInt().coerceIn(0, currentCoverBitmap.height - sampleH)

                        // 1. Dibuja la parte izquierda (difuminado del píxel del borde izquierdo visible)
                        if (imgXInt > 0) {
                            drawImage(
                                image = currentCoverBitmap,
                                srcOffset = IntOffset(srcXInt, srcYInt),
                                srcSize = IntSize(1, sampleH),
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(imgXInt, screenH.toInt()),
                                filterQuality = FilterQuality.Low
                            )
                        }

                        // 2. Dibuja la parte central (alineada perfectamente con la carátula visible)
                        if (imgWInt > 0) {
                            drawImage(
                                image = currentCoverBitmap,
                                srcOffset = IntOffset(srcXInt, srcYInt),
                                srcSize = IntSize(srcWInt, sampleH),
                                dstOffset = IntOffset(imgXInt, 0),
                                dstSize = IntSize(imgWInt, screenH.toInt()),
                                filterQuality = FilterQuality.Low
                            )
                        }

                        // 3. Dibuja la parte derecha (difuminado del píxel del borde derecho visible)
                        val rightX = imgXInt + imgWInt
                        if (rightX < screenWInt) {
                            val rightW = screenWInt - rightX
                            if (rightW > 0) {
                                drawImage(
                                    image = currentCoverBitmap,
                                    srcOffset = IntOffset((srcXInt + srcWInt - 1).coerceIn(0, currentCoverBitmap.width - 1), srcYInt),
                                    srcSize = IntSize(1, sampleH),
                                    dstOffset = IntOffset(rightX, 0),
                                    dstSize = IntSize(rightW, screenH.toInt()),
                                    filterQuality = FilterQuality.Low
                                )
                            }
                        }
                    }
                }
            }

            // Capa 4: Reflejo invertido con difuminado extremo
            if (hdArtUrl != null && dragProgress < 1f) {
                val reflectionWidth: androidx.compose.ui.unit.Dp
                val reflectionHeight: androidx.compose.ui.unit.Dp
                val reflectionX: androidx.compose.ui.unit.Dp
                val reflectionY: androidx.compose.ui.unit.Dp
                val childWidth: androidx.compose.ui.unit.Dp
                val childOffsetX: androidx.compose.ui.unit.Dp
                val reflectionGradient: Array<Pair<Float, Color>>

                if (isOverlayActive) {
                    reflectionWidth = maxWidth
                    reflectionHeight = maxHeight
                    reflectionX = 0.dp
                    reflectionY = 0.dp
                    childWidth = maxWidth
                    childOffsetX = 0.dp
                    reflectionGradient = arrayOf(
                        0.0f to Color.Black,
                        0.85f to Color.Black,
                        1.0f to Color.Black.copy(alpha = 0.6f)
                    )
                } else {
                    reflectionWidth = maxWidth
                    val pad = 120.dp
                    reflectionX = 0.dp
                    childWidth = imgWidth
                    childOffsetX = imgOffsetX
                    
                    val overlapDp = 24.dp
                    val baseReflectionY = (if (sliderYDp > 0.dp) sliderYDp else (imgOffsetY + imgHeight)) - overlapDp
                    reflectionY = baseReflectionY - pad
                    reflectionHeight = imgHeight + pad * 2
                    
                    val hPx = with(density) { reflectionHeight.toPx() }
                    val padPx = with(density) { pad.toPx() }
                    val imgHPx = with(density) { imgHeight.toPx() }
                    val gapPx = with(density) { 40.dp.toPx() }
                    
                    reflectionGradient = if (isBadSong) {
                        arrayOf(
                            0.0f to Color.Transparent,
                            ((padPx - gapPx) / hPx).coerceIn(0f, 1f) to Color.Transparent,
                            (padPx / hPx).coerceIn(0f, 1f) to Color.Transparent,
                            ((padPx + imgHPx * 0.05f) / hPx).coerceIn(0f, 1f) to Color.Black,
                            ((padPx + imgHPx * 0.1f) / hPx).coerceIn(0f, 1f) to Color.Transparent,
                            1.0f to Color.Transparent
                        )
                    } else {
                        arrayOf(
                            0.0f to Color.Transparent,
                            ((padPx - gapPx) / hPx).coerceIn(0f, 1f) to Color.Transparent,
                            (padPx / hPx).coerceIn(0f, 1f) to Color.Transparent,
                            ((padPx + imgHPx * 0.25f) / hPx).coerceIn(0f, 1f) to Color.Black,
                            ((padPx + imgHPx * 0.5f) / hPx).coerceIn(0f, 1f) to Color.Black.copy(alpha = 0.8f),
                            ((padPx + imgHPx * 0.8f) / hPx).coerceIn(0f, 1f) to Color.Black.copy(alpha = 0.2f),
                            ((padPx + imgHPx) / hPx).coerceIn(0f, 1f) to Color.Transparent,
                            1.0f to Color.Transparent
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .offset(x = reflectionX, y = reflectionY)
                        .width(reflectionWidth)
                        .height(reflectionHeight)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            alpha = (1f - dragProgress).coerceIn(0f, 1f)
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = reflectionGradient
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    val currentBitmap = coverBitmap
                    if (currentBitmap != null) {
                        Image(
                            bitmap = currentBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .offset(x = childOffsetX, y = if (isOverlayActive) 0.dp else 120.dp)
                                .width(childWidth)
                                .height(if (isOverlayActive) reflectionHeight else imgHeight)
                                .graphicsLayer {
                                    scaleY = -1f // Invertido verticalmente
                                }
                                .blur(280.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(hdArtUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .offset(x = childOffsetX, y = if (isOverlayActive) 0.dp else 120.dp)
                                .width(childWidth)
                                .height(if (isOverlayActive) reflectionHeight else imgHeight)
                                .graphicsLayer {
                                    scaleY = -1f // Invertido verticalmente
                                }
                                .blur(280.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        )
                    }
                }
            }

            // THE MAIN IMAGE (Capa 3: Portada Principal)
            Box(
                modifier = Modifier
                    .offset(x = imgOffsetX, y = imgOffsetY)
                    .size(width = imgWidth, height = imgHeight)
                    .then(
                        if (showLyrics) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showLyrics = false
                            }
                        } else Modifier
                    )
                    .graphicsLayer {
                        shape = if (!isOverlayActive && dragProgress == 0f) {
                            RoundedCornerShape(
                                topStart = imgCorner.toPx(),
                                topEnd = imgCorner.toPx(),
                                bottomStart = 0f,
                                bottomEnd = 0f
                            )
                        } else {
                            RoundedCornerShape(imgCorner.toPx())
                        }
                        clip = true
                        // Apply offscreen strategy only when player is not fully collapsed
                        compositingStrategy = if (dragProgress < 1f) {
                            CompositingStrategy.Offscreen
                        } else {
                            CompositingStrategy.Auto
                        }
                    }
                    .then(
                        if (dragProgress < 1f) {
                            Modifier.drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Black,
                                            0.97f to Color.Black,
                                            1f to Color.Transparent
                                        )
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        } else Modifier
                    )
            ) {
                // Base sharp album cover (always drawn in background during drag or before playback starts)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(hdArtUrl)
                        .crossfade(true)
                        .build(),
                    imageLoader = animatedImageLoader,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                val currentAnimatedUrl = animatedArtworkUrl

                if (!currentAnimatedUrl.isNullOrBlank()) {
                    DisposableEffect(Unit) {
                        onDispose {
                            isVideoPlaying = false
                        }
                    }
                    com.mrtdk.liquid_glass.ui.components.AnimatedArtworkPlayer(
                        videoUrl = currentAnimatedUrl,
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (isVideoPlaying) 1f else 0f },
                        isPaused = false,
                        enableFrameCapture = (dragProgress == 0f && !isOverlayActive),
                        onPlaybackStarted = { isVideoPlaying = true },
                        onFrameCaptured = { frameBitmap ->
                            coverBitmap = frameBitmap.asImageBitmap()
                            try {
                                // Promedio de la fila inferior de píxeles
                                var r = 0L; var g = 0L; var b = 0L
                                val yCoord = frameBitmap.height - 1
                                val w = frameBitmap.width
                                for (x in 0 until w) {
                                    val pixel = frameBitmap.getPixel(x, yCoord)
                                    r += android.graphics.Color.red(pixel)
                                    g += android.graphics.Color.green(pixel)
                                    b += android.graphics.Color.blue(pixel)
                                }
                                val avgColor = Color((r / w).toInt(), (g / w).toInt(), (b / w).toInt())
                                bottomAverageColor = avgColor
                                dominantColor = avgColor
                                onDominantColorChanged(avgColor)
                            } catch (e: Exception) { }
                        }
                    )
                }

                // Blurred bottom overlay to fade/blur the bottom of the image
                if (dragProgress < 1f) {
                    val currentBitmap = coverBitmap
                    if (currentBitmap != null) {
                        Image(
                            bitmap = currentBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .cloudy(radius = 100)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.75f to Color.Transparent,
                                                0.88f to Color.Black.copy(alpha = 0.6f),
                                                1.0f to Color.Black
                                            )
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(hdArtUrl)
                                .size(150) // Downsample to 150x150 for a much more aggressive, premium soft blur
                                .memoryCachePolicy(coil.request.CachePolicy.READ_ONLY)
                                .crossfade(true)
                                .build(),
                            imageLoader = animatedImageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .cloudy(radius = 100)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.75f to Color.Transparent,
                                                0.88f to Color.Black.copy(alpha = 0.6f),
                                                1.0f to Color.Black
                                            )
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        )
                    }
                }
            }




            // LYRICS / QUEUE OVERLAY
            AnimatedVisibility(
                visible = showLyrics || showQueue,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                 Box(
                     modifier = Modifier
                         .fillMaxSize()
                         .graphicsLayer { alpha = overlayAlpha }
                 ) {
                      Column(
                          modifier = Modifier.fillMaxSize()
                      ) {
                      Row(
                          modifier = Modifier.fillMaxWidth().padding(top = 64.dp, start = 124.dp, end = 24.dp).heightIn(min = 84.dp), 
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                           Column(modifier = Modifier.weight(1f).clickable { 
                               if (playerState?.videoId != null) {
                                   onArtistSelected(com.mrtdk.liquid_glass.ui.screens.ArtistState(
                                       id = playerState.artist,
                                       name = playerState.artist,
                                       thumbnail = null
                                   ))
                               }
                           }) {
                               Text(playerState?.title ?: "", color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                               Text(playerState?.artist ?: "", color = contentColor.copy(alpha=0.7f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                           }
                           Box(
                               modifier = Modifier
                                   .size(36.dp)
                                   .clip(CircleShape)
                                   .background(
                                       if (isSaved) contentColor else contentColor.copy(alpha = 0.15f)
                                   )
                                   .clickable {
                                       if (playerState != null) {
                                           if (!isSaved) {
                                               LibraryManager.saveItem(LibraryItem(playerState.videoId ?: "", playerState.title, playerState.artist, playerState.artUrl?.toString(), ItemType.SONG))
                                           } else {
                                               LibraryManager.removeItem(playerState.videoId ?: "")
                                           }
                                       }
                                   },
                               contentAlignment = Alignment.Center
                           ) {
                               AsyncImage(
                                   model = "file:///android_asset/img reproductor/c.png",
                                   contentDescription = "Fav",
                                   colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                       if (isSaved) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor
                                   ),
                                   modifier = Modifier.size(20.dp)
                               )
                           }
                           Spacer(modifier = Modifier.width(20.dp))
                           Icon(Icons.Default.MoreVert, "More", tint = contentColor, modifier = Modifier.size(24.dp).clickable { 
                               if (showLyrics) {
                                   showLyricsMenu = true
                               } else {
                                   showOptionsMenu = true
                               }
                           })
                      }
                      
                                                                    if (showQueue) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            val shuffleInteraction = remember { MutableInteractionSource() }
                            val isShuffleActive = shuffleModeEnabled
                            val isShufflePressed by shuffleInteraction.collectIsPressedAsState()
                            val shuffleScale by animateFloatAsState(targetValue = if (isShufflePressed) 0.85f else 1.0f, label = "shuffleScale")
                            
                            val activeBg = contentColor.copy(alpha = 0.9f)
                            val activeIcon = if (contentColor == Color.White) dominantColor else Color.White

                            val shuffleBgColor by animateColorAsState(targetValue = if (isShuffleActive) activeBg else contentColor.copy(alpha=0.15f), label = "shuffleBg")
                            val shuffleIconColor by animateColorAsState(targetValue = if (isShuffleActive) activeIcon else contentColor.copy(alpha=0.5f), label = "shuffleIcon")

                            val repeatInteraction = remember { MutableInteractionSource() }
                            val isRepeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF
                            val isRepeatPressed by repeatInteraction.collectIsPressedAsState()
                            val repeatScale by animateFloatAsState(targetValue = if (isRepeatPressed) 0.85f else 1.0f, label = "repeatScale")
                            val repeatBgColor by animateColorAsState(targetValue = if (isRepeatActive) activeBg else contentColor.copy(alpha=0.15f), label = "repeatBg")
                            val repeatIconColor by animateColorAsState(targetValue = if (isRepeatActive) activeIcon else contentColor.copy(alpha=0.5f), label = "repeatIcon")

                            val autoplayInteraction = remember { MutableInteractionSource() }
                            val isAutoplayActive = !playerState.isExclusiveQueue
                            val isAutoplayPressed by autoplayInteraction.collectIsPressedAsState()
                            val autoplayScale by animateFloatAsState(targetValue = if (isAutoplayPressed) 0.85f else 1.0f, label = "autoplayScale")
                            val autoplayBgColor by animateColorAsState(targetValue = if (isAutoplayActive) activeBg else contentColor.copy(alpha=0.15f), label = "autoplayBg")
                            val autoplayIconColor by animateColorAsState(targetValue = if (isAutoplayActive) activeIcon else contentColor.copy(alpha=0.5f), label = "autoplayIcon")

                            val romajiInteraction = remember { MutableInteractionSource() }
                            val isRomajiActive = isRomajiEnabled
                            val isRomajiPressed by romajiInteraction.collectIsPressedAsState()
                            val romajiScale by animateFloatAsState(targetValue = if (isRomajiPressed) 0.85f else 1.0f, label = "romajiScale")
                            val romajiBgColor by animateColorAsState(targetValue = if (isRomajiActive) activeBg else contentColor.copy(alpha=0.15f), label = "romajiBg")
                            val romajiIconColor by animateColorAsState(targetValue = if (isRomajiActive) activeIcon else contentColor.copy(alpha=0.5f), label = "romajiIcon")

                            Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = shuffleScale, scaleY = shuffleScale)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(shuffleBgColor)
                                        .clickable(
                                            interactionSource = shuffleInteraction,
                                            indication = null,
                                            onClick = onToggleShuffle
                                        ),
                                    contentAlignment=Alignment.Center
                                ) {
                                    Icon(painterResource(id = R.drawable.shuffle), "Shuffle", tint=shuffleIconColor, modifier=Modifier.size(20.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = repeatScale, scaleY = repeatScale)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(repeatBgColor)
                                        .clickable(
                                            interactionSource = repeatInteraction,
                                            indication = null,
                                            onClick = onToggleRepeat
                                        ),
                                    contentAlignment=Alignment.Center
                                ) {
                                    Icon(if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint=repeatIconColor, modifier=Modifier.size(20.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = autoplayScale, scaleY = autoplayScale)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(autoplayBgColor)
                                        .clickable(
                                            interactionSource = autoplayInteraction,
                                            indication = null,
                                            onClick = {
                                                onSongSelected(playerState.copy(isExclusiveQueue = !playerState.isExclusiveQueue))
                                            }
                                        ),
                                    contentAlignment=Alignment.Center
                                ) {
                                    Icon(Icons.Default.AllInclusive, "Autoplay", tint=autoplayIconColor, modifier=Modifier.size(20.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = romajiScale, scaleY = romajiScale)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(romajiBgColor)
                                        .clickable(
                                            interactionSource = romajiInteraction,
                                            indication = null,
                                            onClick = {
                                                isRomajiEnabled = !isRomajiEnabled
                                            }
                                        ),
                                    contentAlignment=Alignment.Center
                                ) {
                                    Icon(if (isRomajiActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff, "Toggle", tint=romajiIconColor, modifier=Modifier.size(24.dp))
                                }
                            }
                             if (playerState != null && playerState.queue.isNotEmpty()) {
                               Text(text = stringResource(R.string.siguiente_en_album_playlist), color=contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=14.dp, start=24.dp, end=24.dp, bottom=8.dp))
                            } else if (playerState?.isExclusiveQueue != true) {
                                 Column(modifier = Modifier.padding(top=14.dp, start=24.dp, end=24.dp, bottom=8.dp)) {
                                     Text(text = stringResource(R.string.continue_playing), color=contentColor, fontSize=18.sp, fontWeight=FontWeight.Bold)
                                     Text(text = stringResource(R.string.autoplaying_similar_music), color=contentColor.copy(alpha=0.7f), fontSize=14.sp)
                                 }
                             }
                          
                           LazyColumn(
                                state = queueListState, 
                                modifier = Modifier.weight(1f).padding(horizontal = 24.dp).nestedScroll(nestedScrollConnection), 
                                contentPadding = PaddingValues(bottom = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                              // 1. Manual Queue Section (Album/Playlist)
                              if (playerState != null && playerState.queue.isNotEmpty()) {
                                   items(playerState.queue.size) { index ->
                                        val qItem = playerState.queue[index]
                                        val upgradedArt = qItem.artUrl?.let {
                                            val itStr = it.toString()
                                            if (itStr.startsWith("file:///android_asset/")) {
                                                it
                                            } else {
                                                val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(itStr) ?: itStr
                                                if (it is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
                                            }
                                        } ?: qItem.artUrl
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                                            swipeDirection = 1
                                            val remaining = playerState.queue.drop(index + 1)
                                            onSongSelectedFromQueue(PlayerState(
                                                title = qItem.title,
                                                artist = qItem.artist,
                                                artUrl = upgradedArt,
                                                videoId = qItem.videoId,
                                                queue = remaining,
                                                isExclusiveQueue = playerState.isExclusiveQueue,
                                                album = qItem.album,
                                                albumId = qItem.albumId
                                            ))
                                        }) {
                                           AsyncImage(model = ImageRequest.Builder(context).data(upgradedArt).crossfade(false).build(), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
                                           Spacer(modifier = Modifier.width(12.dp))
                                           Column(modifier = Modifier.weight(1f)) {
                                               Text(qItem.title, color = contentColor, fontSize = 16.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                               Text(qItem.artist, color = contentColor.copy(alpha=0.6f), fontSize = 14.sp, maxLines = 1)
                                           }
                                           Icon(Icons.Default.Menu, contentDescription=null, tint=contentColor.copy(alpha=0.6f))
                                       }
                                   }
                              }
                              if (playerState?.isExclusiveQueue != true) items(upNextSongs.size) { i ->
                                  val song = upNextSongs[i]
                                  val hdThumb = song.thumbnail?.let {
                                       com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(it) ?: it
                                   } ?: song.thumbnail
                                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                                      swipeDirection = 1
                                      val upgradedArt = song.thumbnail?.let {
                                          com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(it) ?: it
                                      } ?: song.thumbnail
                                      // Remove clicked song and all before it from the queue
                                      onUpNextSongsChange(upNextSongs.drop(i + 1))
                                      onSongSelectedFromQueue(PlayerState(
                                          title = song.title,
                                          artist = song.artists.joinToString { it.name },
                                          artUrl = upgradedArt,
                                          videoId = song.id,
                                          isExclusiveQueue = playerState.isExclusiveQueue,
                                          album = song.album?.name,
                                          albumId = song.album?.id
                                      ))
                                  }) {
                                      AsyncImage(model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
                                      Spacer(modifier = Modifier.width(12.dp))
                                      Column(modifier = Modifier.weight(1f)) {
                                          Text(song.title, color = contentColor, fontSize = 16.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                          Text(song.artists.joinToString { it.name }, color = contentColor.copy(alpha=0.6f), fontSize = 14.sp, maxLines = 1)
                                      }
                                      Icon(Icons.Default.Menu, contentDescription=null, tint=contentColor.copy(alpha=0.6f))
                                  }
                              }
                          }
                      } else if (showLyrics) {
                             Spacer(modifier = Modifier.height(44.dp))
                             Box(
                                 modifier = Modifier
                                     .weight(1f)
                                     .clipToBounds()
                                     .clickable(
                                         interactionSource = remember { MutableInteractionSource() },
                                         indication = null
                                     ) {
                                         showLyricsControls = !showLyricsControls
                                         if (showLyricsControls) {
                                             lyricsControlsHideTrigger++
                                         }
                                     }
                             ) {
                                 val currentLyricsLines = lyricsLines
                                 if (currentLyricsLines != null && currentLyricsLines.isNotEmpty()) {
                                     val isSynced = currentLyricsLines.any { it.timeMs > 0L }
                                     
                                     LaunchedEffect(currentPosition, currentLyricsLines) {
                                         if (!isSynced) return@LaunchedEffect
                                         val currentIdx = currentLyricsLines.indexOfLast { it.timeMs != -1L && it.timeMs <= currentPosition + 500 }
                                         if (currentIdx >= 0 && !lyricsListState.isScrollInProgress) {
                                             lyricsListState.animateScrollToItem(currentIdx.coerceAtLeast(0), scrollOffset = -100)
                                         }
                                     }
                                     
                                     LazyColumn(state = lyricsListState, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).nestedScroll(nestedScrollConnection), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                         item { Spacer(modifier = Modifier.height(60.dp)) }
                                         items(currentLyricsLines.size) { i ->
                                             val line = currentLyricsLines[i]
                                             val isCurrent = isSynced && line.timeMs != -1L && currentPosition >= line.timeMs && 
                                                 (i == currentLyricsLines.lastIndex || currentPosition < currentLyricsLines[i+1].timeMs)
                                             val isPast = isSynced && line.timeMs != -1L && currentPosition > line.timeMs
                                             val distance = if (isSynced) {
                                                 val curIdx = currentLyricsLines.indexOfLast { it.timeMs != -1L && it.timeMs <= currentPosition + 500 }
                                                 if (curIdx >= 0) kotlin.math.abs(i - curIdx) else 0
                                             } else 0
                                             
                                             // Compute word timing for word-by-word gradient fill
                                             val nextLineTime = currentLyricsLines.getOrNull(i + 1)?.timeMs
                                             val lineDuration = remember(line.timeMs, nextLineTime) {
                                                 if (nextLineTime != null && nextLineTime > 0 && line.timeMs > 0) nextLineTime - line.timeMs else 4000L
                                             }
                                             val activeDuration = remember(lineDuration) {
                                                 (lineDuration * 0.95).toLong().coerceAtLeast(300L)
                                             }
                                             val lineRelTime = if (isCurrent && line.timeMs > 0) (currentPosition - line.timeMs).coerceAtLeast(0L) else if (isPast) activeDuration else 0L
                                             
                                             val targetAlpha = when {
                                                 !isSynced || isCurrent -> 1f
                                                 distance == 1 -> 0.55f
                                                 distance == 2 -> 0.4f
                                                 else -> 0.3f
                                             }
                                             val targetScale = when {
                                                 !isSynced || isCurrent -> 1.05f
                                                 distance == 1 -> 0.95f
                                                 distance >= 2 -> 0.85f
                                                 else -> 1f
                                             }
                                             val targetBlur = if (!isCurrent && isSynced) {
                                                 when (distance) {
                                                     1 -> 2.5f
                                                     2 -> 4f
                                                     else -> 6f
                                                 }
                                             } else 0f
                                             
                                             val animAlpha by androidx.compose.animation.core.animateFloatAsState(targetAlpha, animationSpec = tween(260, easing = FastOutSlowInEasing), label="lyricsAlpha")
                                             val animScale by androidx.compose.animation.core.animateFloatAsState(targetScale, animationSpec = tween(320, easing = FastOutSlowInEasing), label="lyricsScale")
                                             val animBlur by androidx.compose.animation.core.animateFloatAsState(targetBlur, animationSpec = tween(420, easing = FastOutSlowInEasing), label="lyricsBlur")
                                             
                                             // Word-by-word data
                                             val wordData = remember(line.text, activeDuration) {
                                                 val words = line.text.split(" ").filter { it.isNotEmpty() }
                                                 if (words.isEmpty()) {
                                                     listOf(Triple(line.text, 0L, activeDuration))
                                                 } else {
                                                     val totalChars = line.text.length
                                                     var accumulatedTime = 0L
                                                     words.mapIndexed { wordIndex, word ->
                                                         val charCount = if (wordIndex < words.lastIndex) word.length + 1 else word.length
                                                         val wordStart = accumulatedTime
                                                         val wordDur = if (totalChars > 0) (activeDuration * charCount.toFloat() / totalChars).toLong() else activeDuration
                                                         accumulatedTime += wordDur
                                                         Triple(if (wordIndex < words.lastIndex) "$word " else word, wordStart, wordStart + wordDur)
                                                     }
                                                 }
                                             }
                                             
                                             // Word-by-word FlowRow rendering
                                             @OptIn(ExperimentalLayoutApi::class)
                                             FlowRow(
                                                 modifier = Modifier.fillMaxWidth()
                                                     .graphicsLayer {
                                                         scaleX = animScale; scaleY = animScale
                                                         alpha = animAlpha
                                                         transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                                     }
                                                     .then(if (animBlur > 0f) Modifier.blur(animBlur.dp) else Modifier)
                                                     .clickable { 
                                                          if(line.timeMs != -1L) onSeek(line.timeMs)
                                                          showLyricsControls = true
                                                          lyricsControlsHideTrigger++
                                                      },
                                                 horizontalArrangement = Arrangement.Start,
                                                 verticalArrangement = Arrangement.spacedBy(4.dp)
                                             ) {
                                                 wordData.forEach { (wordText, startRelative, endRelative) ->
                                                     val wordDuration = (endRelative - startRelative).coerceAtLeast(1L)
                                                     
                                                     val wordProgress by androidx.compose.animation.core.animateFloatAsState(
                                                         targetValue = when {
                                                             lineRelTime >= endRelative -> 1f
                                                             lineRelTime < startRelative -> 0f
                                                             else -> (lineRelTime - startRelative).toFloat() / wordDuration
                                                         },
                                                         animationSpec = tween(
                                                             durationMillis = wordDuration.coerceIn(140L, 260L).toInt(),
                                                             easing = FastOutSlowInEasing
                                                         ),
                                                         label = "wordProgress"
                                                     )
                                                     
                                                     val finalFontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold
                                                     
                                                     Text(
                                                         text = wordText,
                                                         fontSize = 28.sp,
                                                         style = TextStyle(
                                                             brush = if (isCurrent) Brush.horizontalGradient(
                                                                 0.0f to contentColor,
                                                                 (wordProgress - 0.05f).coerceAtLeast(0f) to contentColor,
                                                                 (wordProgress + 0.05f).coerceAtMost(1f) to contentColor.copy(alpha = 0.4f),
                                                                 1.0f to contentColor.copy(alpha = 0.4f)
                                                             ) else null,
                                                             fontWeight = finalFontWeight,
                                                             lineHeight = 36.sp,
                                                             shadow = if (isCurrent && wordProgress > 0.1f) Shadow(
                                                                 color = contentColor.copy(alpha = 0.6f * wordProgress),
                                                                 offset = Offset.Zero,
                                                                 blurRadius = (12f * wordProgress).coerceAtLeast(0.1f)
                                                             ) else null
                                                         ),
                                                         color = if (!isCurrent) contentColor else Color.Unspecified
                                                     )
                                                 }
                                             }
                                         }
                                         item { Spacer(modifier = Modifier.height(360.dp)) }
                                     }
                                 } else {
                                     Box(
                                         modifier = Modifier
                                             .fillMaxSize()
                                             .clickable(
                                                 interactionSource = remember { MutableInteractionSource() },
                                                 indication = null
                                             ) {
                                                 showLyricsControls = !showLyricsControls
                                                 if (showLyricsControls) {
                                                     lyricsControlsHideTrigger++
                                                 }
                                             },
                                         contentAlignment = Alignment.Center
                                     ) {
                                         CircularProgressIndicator(color = contentColor)
                                     }
                                 }
                            }
                       }
                      }
                      
                      AnimatedVisibility(
                           visible = !showLyrics || showLyricsControls,
                           modifier = Modifier.align(Alignment.BottomCenter),
                           enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                           exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                      ) {
                          Box(
                               modifier = Modifier
                                   .fillMaxWidth()
                                   .background(
                                       Brush.verticalGradient(
                                           colorStops = arrayOf(
                                               0.0f to Color.Transparent,
                                               0.15f to bottomAverageColor,
                                               1.0f to bottomAverageColor
                                           )
                                       )
                                   )
                           ) {
                               Column(
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .padding(horizontal = 24.dp)
                                       .padding(top = 20.dp) // difuminado padding
                                       .padding(bottom = 0.dp)
                               ) {
                                  AppleMusicSlider(
                                      value = progress, onValueChange = { onSeek((it * duration).toLong()) },
                                      modifier = Modifier.fillMaxWidth().height(24.dp),
                                      activeColor = contentColor,
                                      inactiveColor = contentColor.copy(alpha = 0.3f),
                                      barHeightDp = 8.dp
                                  )
                                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                      Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                      Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                  }
                                  
                                  Spacer(modifier = Modifier.height(32.dp))
                                  
                                  Box(
                                      modifier = Modifier
                                          .fillMaxWidth()
                                          .padding(bottom = 32.dp)
                                  ) {
                                      PlayerBottomControls( // We only want the icons, volume and stuff here, NO PROGRESS
                                          progress = progress, currentPosition = currentPosition, duration = duration,
                                          isPlaying = isPlaying, contentColor = contentColor, volumePosition = volumePosition,
                                          showLyrics = showLyrics, showQueue = showQueue,
                                          onSeek = onSeek, onTogglePlayPause = onTogglePlayPause, onVolumeChange = { v -> volumePosition = v; onVolumeChange(v) },
                                          onToggleLyrics = { showLyrics = !showLyrics; showQueue = false }, onToggleQueue = { showQueue = !showQueue; showLyrics = false },
                                          includeVolumeAndIcons = true,
                                          includeProgress = false,
                                          onSkipNext = { swipeDirection = 1; onSkipNext() },
                                          onSkipPrevious = { swipeDirection = -1; onSkipPrevious() }
                                      )
                                  }
                              }
                          }
                      }
                 }
            }

            // DETAILS AND CONTROLS (WHEN NO QUEUE AND NO LYRICS)
            AnimatedVisibility(
                 visible = !isOverlayActive,
                 modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                 enter = fadeIn(),
                 exit = fadeOut()
            ) {
                 Box(
                      modifier = Modifier
                          .fillMaxWidth()
                          .graphicsLayer { alpha = contentAlpha }
                  ) {
                     Column(
                          modifier = Modifier
                              .fillMaxWidth()
                              .padding(horizontal = 24.dp)
                     ) {
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                          var dragAccumulator by remember { mutableStateOf(0f) }
                          Box(modifier = Modifier.weight(1f).pointerInput(playerState) {
                              detectHorizontalDragGestures(
                                  onDragEnd = {
                                      if (dragAccumulator < -60f) { swipeDirection = 1; onSkipNext() }
                                      else if (dragAccumulator > 60f) { swipeDirection = -1; onSkipPrevious() }
                                      dragAccumulator = 0f
                                  },
                                  onHorizontalDrag = { change, dragAmount ->
                                      dragAccumulator += dragAmount
                                      change.consume()
                                  }
                              )
                          }) {
                              val dir = swipeDirection
                              androidx.compose.animation.AnimatedContent(
                                  targetState = playerState,
                                  transitionSpec = {
                                      (androidx.compose.animation.slideInHorizontally { width -> dir * width } + fadeIn()).togetherWith(
                                          androidx.compose.animation.slideOutHorizontally { width -> dir * -width } + fadeOut()
                                      )
                                  }, label="textSlide"
                              ) { state ->
                                  Column(modifier = Modifier.fillMaxWidth().clickable { 
                                      if (state.videoId != null) {
                                          onArtistSelected(com.mrtdk.liquid_glass.ui.screens.ArtistState(
                                              id = state.artist,
                                              name = state.artist,
                                              thumbnail = null
                                          ))
                                      }
                                  }) {
                                      Text(
                                          text = state.title,
                                          color = contentColor,
                                          fontSize = 24.sp,
                                          fontWeight = FontWeight.Bold,
                                          maxLines = 1,
                                          overflow = TextOverflow.Ellipsis
                                      )
                                      Spacer(modifier = Modifier.height(2.dp))
                                      Text(
                                          text = state.artist,
                                          color = contentColor.copy(alpha = 0.7f),
                                          fontSize = 18.sp,
                                          maxLines = 1,
                                          overflow = TextOverflow.Ellipsis
                                      )
                                  }
                              }
                          }
                          Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                              Box(
                                  modifier = Modifier
                                      .size(36.dp)
                                      .clip(CircleShape)
                                      .background(
                                          if (isSaved) contentColor else contentColor.copy(alpha = 0.15f)
                                      )
                                      .clickable {
                                          if (playerState != null) {
                                              if (!isSaved) {
                                                  LibraryManager.saveItem(LibraryItem(playerState.videoId ?: "", playerState.title, playerState.artist, playerState.artUrl?.toString(), ItemType.SONG))
                                              } else {
                                                  LibraryManager.removeItem(playerState.videoId ?: "")
                                              }
                                          }
                                      },
                                  contentAlignment = Alignment.Center
                              ) {
                                  AsyncImage(
                                      model = "file:///android_asset/img reproductor/c.png",
                                      contentDescription = "Fav",
                                      colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                          if (isSaved) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor
                                      ),
                                      modifier = Modifier.size(18.dp)
                                  )
                              }
                              Box(
                                  modifier = Modifier.size(32.dp).clip(CircleShape).background(contentColor.copy(alpha = 0.15f)).clickable { showOptionsMenu = true },
                                  contentAlignment = Alignment.Center
                              ) {
                                  androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                                      val r = 1.5.dp.toPx()
                                      val space = 4.dp.toPx()
                                      val cx = size.width / 2f
                                      val cy = size.height / 2f
                                      drawCircle(contentColor, radius = r, center = Offset(cx - space - r * 2, cy))
                                      drawCircle(contentColor, radius = r, center = Offset(cx, cy))
                                      drawCircle(contentColor, radius = r, center = Offset(cx + space + r * 2, cy))
                                  }
                              }
                          }
                      }
                      
                      Spacer(modifier = Modifier.height(24.dp))
                      
                      AppleMusicSlider(
                          value = progress, onValueChange = { onSeek((it * duration).toLong()) },
                          modifier = Modifier
                              .fillMaxWidth()
                              .height(24.dp)
                              .onGloballyPositioned { coords ->
                                  sliderCoordinates = coords
                              },
                          activeColor = contentColor,
                          inactiveColor = contentColor.copy(alpha = 0.3f),
                          barHeightDp = 8.dp
                      )
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                          Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                          Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                      }
                      
                      Spacer(modifier = Modifier.height(32.dp))
                      
                      Box(
                          modifier = Modifier
                              .fillMaxWidth()
                              .padding(bottom = 32.dp)
                      ) {
                          PlayerBottomControls( // We only want the icons, volume and stuff here, NO PROGRESS
                              progress = progress, currentPosition = currentPosition, duration = duration,
                              isPlaying = isPlaying, contentColor = contentColor, volumePosition = volumePosition,
                              showLyrics = showLyrics, showQueue = showQueue,
                              onSeek = onSeek, onTogglePlayPause = onTogglePlayPause, onVolumeChange = { v -> 
                                  volumePosition = v
                                  audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (v * maxVolume).toInt(), 0)
                                  onVolumeChange(v) 
                              },
                              onToggleLyrics = { showLyrics = !showLyrics; showQueue = false }, onToggleQueue = { showQueue = !showQueue; showLyrics = false },
                              includeVolumeAndIcons = true,
                              includeProgress = false,
                              onSkipNext = { swipeDirection = 1; onSkipNext() },
                              onSkipPrevious = { swipeDirection = -1; onSkipPrevious() }
                           )
                       }
                   }
                 }
              }
         } // end inner Box
        } // end BoxWithConstraints



        if (showLyricsMenu) {
            ModalBottomSheet(onDismissRequest = { showLyricsMenu = false }, containerColor = Color(0xFF1E1E1E)) {
                Column(modifier = Modifier.padding(minOf(16.dp, 24.dp))) {
                    Text(text = "Proveedor de letras", color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold)
                    Spacer(modifier=Modifier.height(16.dp))
                    
                    Row(modifier=Modifier.fillMaxWidth().clickable { showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text("LRCLIB (Activo)", color=Color.White, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text("KuGou (Próximamente)", color=Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text("Musixmatch (Próximamente)", color=Color.Gray, fontSize=16.sp)
                    }
                    
                    Spacer(modifier=Modifier.height(16.dp))
                    androidx.compose.material3.Divider(color = Color.DarkGray)
                    Spacer(modifier=Modifier.height(16.dp))
                    
                    Row(modifier=Modifier.fillMaxWidth().clickable { 
                        showLyricsMenu=false 
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH).apply {
                                putExtra(android.app.SearchManager.QUERY, "${playerState?.artist ?: ""} ${playerState?.title ?: ""} lyrics")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription=null, tint=Color.White)
                        Spacer(modifier=Modifier.width(16.dp))
                        Text(stringResource(R.string.buscar_letra_internet), color=Color.White, fontSize=16.sp)
                    }
                    Spacer(modifier=Modifier.height(32.dp))
                }
            }
        }

        if (showLyricsMenu) {
            ModalBottomSheet(onDismissRequest = { showLyricsMenu = false }, containerColor = Color(0xFF1E1E1E)) {
                Column(modifier = Modifier.padding(minOf(16.dp, 24.dp))) {
                    Text(text = "Proveedor de letras", color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold)
                    Spacer(modifier=Modifier.height(16.dp))
                    
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "LRCLIB"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "LRCLIB") "LRCLIB (Activo)" else "LRCLIB", color=if (selectedLyricsProvider == "LRCLIB") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "KuGou"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "KuGou") "KuGou (Activo)" else "KuGou", color=if (selectedLyricsProvider == "KuGou") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "BetterLyrics"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "BetterLyrics") "BetterLyrics (Activo)" else "BetterLyrics", color=if (selectedLyricsProvider == "BetterLyrics") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "LyricsPlus"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "LyricsPlus") "LyricsPlus (Activo)" else "LyricsPlus", color=if (selectedLyricsProvider == "LyricsPlus") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "SimpMusic"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "SimpMusic") "SimpMusic (Activo)" else "SimpMusic", color=if (selectedLyricsProvider == "SimpMusic") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "YouTube Music"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "YouTube Music") "YouTube Music (Activo)" else "YouTube Music", color=if (selectedLyricsProvider == "YouTube Music") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "YouTube Subtitle"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "YouTube Subtitle") "YouTube Subtitle (Activo)" else "YouTube Subtitle", color=if (selectedLyricsProvider == "YouTube Subtitle") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Spacer(modifier=Modifier.height(8.dp))
                    androidx.compose.material3.Divider(color = Color.DarkGray)
                    Spacer(modifier=Modifier.height(8.dp))
                    Row(modifier=Modifier.fillMaxWidth().clickable { isRomajiEnabled = !isRomajiEnabled; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (isRomajiEnabled) stringResource(R.string.desactivar_romaji) else stringResource(R.string.activar_romaji), color=Color.White, fontSize=16.sp)
                    }
                    
                    Spacer(modifier=Modifier.height(16.dp))
                    androidx.compose.material3.Divider(color = Color.DarkGray)
                    Spacer(modifier=Modifier.height(16.dp))
                    
                    Row(modifier=Modifier.fillMaxWidth().clickable { 
                        showLyricsMenu=false 
                        manualLyricsQueryTitle = playerState?.title ?: ""
                        manualLyricsQueryArtist = playerState?.artist ?: ""
                        showManualLyricsSearch = true
                    }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription=null, tint=Color.White)
                        Spacer(modifier=Modifier.width(16.dp))
                        Text(stringResource(R.string.buscar_letra_manualmente), color=Color.White, fontSize=16.sp)
                    }
                    Spacer(modifier=Modifier.height(32.dp))
                }
            }
        }
        
        val coroutineScope = rememberCoroutineScope()
        if (showManualLyricsSearch) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showManualLyricsSearch = false },
                containerColor = Color(0xFF1E1E1E),
                title = { Text(stringResource(R.string.buscar_letra), color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.OutlinedTextField(
                            value = manualLyricsQueryTitle,
                            onValueChange = { manualLyricsQueryTitle = it },
                            label = { Text(stringResource(R.string.titulo_cancion), color = Color.Gray) },
                            singleLine = true,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                            )
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = manualLyricsQueryArtist,
                            onValueChange = { manualLyricsQueryArtist = it },
                            label = { Text(stringResource(R.string.artista), color = Color.Gray) },
                            singleLine = true,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showManualLyricsSearch = false
                        lyricsLines = null // reset so user sees loading
                        val targetTitle = manualLyricsQueryTitle
                        val targetArtist = manualLyricsQueryArtist
                        coroutineScope.launch {
                            val lines = when (selectedLyricsProvider) {
                                "KuGou" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchKuGouLyrics(targetTitle, targetArtist)
                                "BetterLyrics" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchBetterLyrics(targetTitle, targetArtist)
                                "LyricsPlus" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchLyricsPlus(targetTitle, targetArtist)
                                "SimpMusic" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchSimpMusicLyrics(targetTitle, targetArtist)
                                "YouTube Music" -> playerState?.videoId?.let { com.mrtdk.liquid_glass.utils.LyricsProvider.fetchYouTubeLyrics(it) }
                                "YouTube Subtitle" -> playerState?.videoId?.let { com.mrtdk.liquid_glass.utils.LyricsProvider.fetchYouTubeSubtitleLyrics(it) }
                                else -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchLyrics(targetTitle, targetArtist)
                            }
                            if (isRomajiEnabled && lines != null) {
                                val prefs = com.mrtdk.liquid_glass.utils.LyricsRomanizationPreferences(true, true, true, true, true)
                                lyricsLines = lines.map { line ->
                                    val romanized = com.mrtdk.liquid_glass.utils.LyricsUtils.romanizeLyricsLine(line.text, prefs)
                                    if (romanized != null) {
                                        line.copy(text = romanized)
                                    } else {
                                        line
                                    }
                                }
                            } else {
                                lyricsLines = lines
                            }
                        }
                    }) {
                        Text(stringResource(R.string.search_action), color = Color(0xFFFA243C))
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showManualLyricsSearch = false }) {
                        Text(stringResource(R.string.cancelar), color = Color.Gray)
                    }
                }
            )
        }
        
        if (showPlaylistMenu) {
            ModalBottomSheet(onDismissRequest = { showPlaylistMenu = false }, containerColor = Color(0xFF1E1E1E)) {
                val playlists by com.mrtdk.liquid_glass.data.LibraryManager.playlists.collectAsState()
                Column(modifier = Modifier.padding(horizontal=16.dp, vertical=8.dp).fillMaxWidth()) {
                    Text(stringResource(R.string.añadir_a_playlist), color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold)
                    Spacer(modifier=Modifier.height(16.dp))
                    Row(modifier=Modifier.fillMaxWidth().clickable { showPlaylistMenu=false; showNewPlaylistDialog=true }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Box(modifier=Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray), contentAlignment=Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint=Color.White)
                        }
                        Spacer(modifier=Modifier.width(16.dp))
                        Text(stringResource(R.string.nueva_playlist_ellipsis), color=Color(0xFFFA243C), fontSize=16.sp)
                    }
                    
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max=300.dp)) {
                        items(playlists.size) { i ->
                            val pl = playlists[i]
                            Row(modifier=Modifier.fillMaxWidth().clickable { 
                                com.mrtdk.liquid_glass.data.LibraryManager.addSongToPlaylist(pl.id, com.mrtdk.liquid_glass.data.LibraryItem(playerState?.videoId?:"", playerState?.title?:"", playerState?.artist?:"", playerState?.artUrl?.toString(), com.mrtdk.liquid_glass.data.ItemType.SONG))
                                showPlaylistMenu=false 
                            }.padding(vertical=8.dp), verticalAlignment=Alignment.CenterVertically) {
                                Box(modifier=Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                                    if (pl.items.isNotEmpty() && pl.items.first().thumbnail != null) {
                                        AsyncImage(model=pl.items.first().thumbnail, contentDescription=null, modifier=Modifier.fillMaxSize(), contentScale=ContentScale.Crop)
                                    }
                                }
                                Spacer(modifier=Modifier.width(16.dp))
                                Column {
                                    Text(pl.name, color=Color.White, fontSize=16.sp)
                                    Text(stringResource(R.string.num_canciones, pl.items.size), color=Color.Gray, fontSize=14.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier=Modifier.height(32.dp))
                }
            }
        }
        
        if (showNewPlaylistDialog) {
            var newPlaylistName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewPlaylistDialog = false },
                title = { Text(stringResource(R.string.nueva_playlist), color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text(stringResource(R.string.nombre)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor=Color.White, 
                            unfocusedTextColor=Color.White,
                            focusedBorderColor = Color(0xFFFA243C),
                            focusedLabelColor = Color(0xFFFA243C)
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if(newPlaylistName.isNotBlank()){
                            com.mrtdk.liquid_glass.data.LibraryManager.createPlaylist(newPlaylistName)
                        }
                        showNewPlaylistDialog = false
                    }) { Text(stringResource(R.string.crear), color = Color(0xFFFA243C)) }
                },
                dismissButton = {
                    TextButton(onClick = { showNewPlaylistDialog = false }) { Text(stringResource(R.string.cancelar), color = Color.Gray) }
                },
                containerColor = Color(0xFF2C2C2C)
            )
        }
        }
    },
    glassContent = {
        if (showOptionsMenu) {
            PlayerOptionsMenu(
                backdrop = localBackdrop,
                onDismiss = { showOptionsMenu = false },
                playerState = playerState,
                isSaved = isSaved,
                onToggleSaved = {
                    if (playerState != null) {
                        if (!isSaved) {
                            LibraryManager.saveItem(LibraryItem(playerState.videoId ?: "", playerState.title, playerState.artist, playerState.artUrl?.toString(), ItemType.SONG))
                        } else {
                            LibraryManager.removeItem(playerState.videoId ?: "")
                        }
                    }
                },
                onDownload = {
                    if (playerState?.videoId != null) {
                        downloadSong(context, playerState.videoId, playerState.title, playerState.artist, playerState.artUrl?.toString(), playerState.album)
                    }
                },
                onAddToPlaylist = {
                    showPlaylistMenu = true
                },
                onSongSelected = { targetState ->
                    onSongSelected(targetState)
                },
                onAlbumSelected = { album ->
                    showOptionsMenu = false
                    onAlbumSelected(album)
                }
            )
        }
    }
)
}
}
@Composable
fun PlayerBottomControls(
    progress: Float, currentPosition: Long, duration: Long,
    isPlaying: Boolean, contentColor: Color, volumePosition: Float,
    showLyrics: Boolean, showQueue: Boolean,
    onSeek: (Long) -> Unit, onTogglePlayPause: () -> Unit, onVolumeChange: (Float) -> Unit,
    onToggleLyrics: () -> Unit, onToggleQueue: () -> Unit,
    includeVolumeAndIcons: Boolean = true,
    includeProgress: Boolean = true,
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {}
) {
    val isLightBackground = contentColor != Color.White
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        if (includeProgress) {
            AppleMusicSlider(
                value = progress, onValueChange = { onSeek((it * duration).toLong()) },
                modifier = Modifier.fillMaxWidth().height(24.dp),
                activeColor = contentColor,
                inactiveColor = contentColor.copy(alpha = 0.3f),
                barHeightDp = 8.dp
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            AnimatedSkipButton(
                iconId = R.drawable.previous,
                contentDescription = "Previous",
                contentColor = contentColor,
                sizeDp = 84.dp,
                iconSizeDp = 64.dp,
                onClick = onSkipPrevious
            )
            Spacer(modifier = Modifier.width(16.dp))
            val playPauseInteractionSource = remember { MutableInteractionSource() }
            val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()

            val playPauseBgColor by animateColorAsState(
                targetValue = if (isPlayPausePressed) contentColor.copy(alpha = 0.12f) else Color.Transparent,
                label = "playPauseBg"
            )

            val playPauseRotation by animateFloatAsState(
                targetValue = if (isPlaying) 180f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "playPauseButtonRotation"
            )

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(playPauseBgColor)
                    .clickable(
                        interactionSource = playPauseInteractionSource,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        onClick = onTogglePlayPause
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)))
                            .togetherWith(fadeOut(animationSpec = tween(90)) + scaleOut(targetScale = 0.3f, animationSpec = tween(90)))
                    },
                    label = "playPauseIcon"
                ) { playing ->
                    Icon(
                        painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.resume),
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = contentColor,
                        modifier = Modifier
                            .size(64.dp)
                            .graphicsLayer {
                                rotationZ = playPauseRotation
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            AnimatedSkipButton(
                iconId = R.drawable.forward,
                contentDescription = "Next",
                contentColor = contentColor,
                sizeDp = 84.dp,
                iconSizeDp = 64.dp,
                onClick = onSkipNext
            )
        }
        
        if (includeVolumeAndIcons) {
            Spacer(modifier = Modifier.height(70.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.albumspeaker),
                    contentDescription = "Low volume",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                AppleMusicSlider(
                    value = volumePosition, onValueChange = { onVolumeChange(it) },
                    modifier = Modifier.weight(1f).height(24.dp),
                    activeColor = contentColor,
                    inactiveColor = contentColor.copy(alpha = 0.2f),
                    barHeightDp = 8.dp
                )
                Spacer(modifier = Modifier.width(20.dp))
                Icon(
                    painter = painterResource(id = R.drawable.albumspeakerlarge),
                    contentDescription = "High volume",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (showLyrics) contentColor else Color.Transparent)
                        .clickable { onToggleLyrics() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "file:///android_asset/img reproductor/Letras.png",
                        contentDescription = "Lyrics",
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                            if (showLyrics) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                }
                AsyncImage(
                    model = "file:///android_asset/img reproductor/parlante.png",
                    contentDescription = "Format",
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor),
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* do nothing */ }
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (showQueue) contentColor else Color.Transparent)
                        .clickable { onToggleQueue() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.nextinfo),
                        contentDescription = "Next Info",
                        tint = if (showQueue) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
             Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun AnimatedSkipButton(
    iconId: Int,
    contentDescription: String,
    contentColor: Color,
    sizeDp: androidx.compose.ui.unit.Dp = 56.dp,
    iconSizeDp: androidx.compose.ui.unit.Dp = 52.dp,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(if (isPressed) 0.85f else 1f, label="")
    val bgAlpha by androidx.compose.animation.core.animateFloatAsState(if (isPressed) 0.15f else 0f, label="")
    
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(contentColor.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier
                .size(iconSizeDp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
        )
    }
}

@Composable
fun AppleMusicSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.3f),
    barHeightDp: androidx.compose.ui.unit.Dp = 8.dp
) {
    var isDragging by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isDragging) 1.5f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "slider_scale"
    )
    var sliderWidth by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleY = scale
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        isDragging = true
                        if (sliderWidth > 0) {
                            onValueChange((down.position.x / sliderWidth).coerceIn(0f, 1f))
                        }
                        down.consume()
                        
                        while (true) {
                            val event = awaitPointerEvent()
                            val dragEvent = event.changes.firstOrNull()
                            if (dragEvent != null && dragEvent.pressed) {
                                if (sliderWidth > 0) {
                                    onValueChange((dragEvent.position.x / sliderWidth).coerceIn(0f, 1f))
                                }
                                dragEvent.consume()
                            } else {
                                break
                            }
                        }
                        isDragging = false
                    }
                }
            }
            .onSizeChanged { sliderWidth = it.width.toFloat() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val width = size.width
            val barHeight = barHeightDp.toPx()
            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2, barHeight / 2)
            val centerY = height / 2 - barHeight / 2

            // Inactive track
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, centerY),
                size = Size(width, barHeight),
                cornerRadius = cornerRadius
            )

            // Active track
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(0f, centerY),
                size = Size(width * value, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}

fun downloadSong(context: android.content.Context, videoId: String, title: String, artist: String, artUrl: String?, album: String? = null, silent: Boolean = false) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            if (context is android.app.Activity) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    context,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    try {
        val metaString = "$title||$artist||${artUrl ?: ""}||${album ?: ""}"
        val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest.Builder(videoId, android.net.Uri.parse("yt://$videoId"))
            .setCustomCacheKey(videoId)
            .setData(metaString.toByteArray(Charsets.UTF_8))
            .build()
        
        androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
            context,
            com.mrtdk.liquid_glass.playback.ExoDownloadService::class.java,
            downloadRequest,
            false
        )
        if (!silent) {
            Toast.makeText(context, "Descarga agregada a la cola: $title", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error al iniciar descarga: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun LandscapePlayerLayout(
    playerState: PlayerState?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    upNextSongs: List<com.echo.innertube.models.SongItem>,
    onUpNextSongsChange: (List<com.echo.innertube.models.SongItem>) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onArtistSelected: (com.mrtdk.liquid_glass.ui.screens.ArtistState) -> Unit,
    onAlbumSelected: (com.mrtdk.liquid_glass.ui.screens.AlbumState) -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    onSongSelectedFromQueue: (PlayerState) -> Unit,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    showLyrics: Boolean,
    onShowLyricsChange: (Boolean) -> Unit,
    showQueue: Boolean,
    onShowQueueChange: (Boolean) -> Unit,
    volumePosition: Float,
    onVolumePositionChange: (Float) -> Unit,
    coverBitmap: ImageBitmap?,
    onCoverBitmapChange: (ImageBitmap?) -> Unit,
    dominantColor: Color,
    onDominantColorChange: (Color) -> Unit,
    bottomAverageColor: Color,
    onBottomAverageColorChange: (Color) -> Unit,
    rightSideAverageColor: Color,
    onRightSideAverageColorChange: (Color) -> Unit,
    hdArtUrl: Any?,
    lyricsLines: List<com.mrtdk.liquid_glass.utils.LyricLine>?,
    isRomajiEnabled: Boolean,
    onToggleRomaji: () -> Unit,
    isSaved: Boolean,
    animatedArtworkUrl: String?,
    isVideoPlaying: Boolean,
    onVideoPlayingChange: (Boolean) -> Unit,
    animatedImageLoader: coil.ImageLoader,
    isLightBackground: Boolean,
    contentColor: Color,
    onShowOptionsMenu: () -> Unit,
    onShowLyricsMenu: () -> Unit,
    onShowPlaylistMenu: () -> Unit,
    isBottomBarCollapsed: Boolean
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dragOffsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val isBadSong = remember(playerState) {
        val title = playerState?.title ?: ""
        val artist = playerState?.artist ?: ""
        val album = playerState?.album ?: ""
        (album.contains("Bad", ignoreCase = true) || title.contains("Bad", ignoreCase = true)) &&
        artist.contains("Michael Jackson", ignoreCase = true)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight
        val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
        val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() }

        val normalTargetOffsetX = 28.dp
        val collapsedTargetOffsetX = 92.dp
        val normalTargetOffsetY = maxHeight - 148.dp
        val collapsedTargetOffsetY = maxHeight - 64.dp
        
        val targetOffsetX = if (isBottomBarCollapsed) collapsedTargetOffsetX else normalTargetOffsetX
        val targetOffsetY = if (isBottomBarCollapsed) collapsedTargetOffsetY else normalTargetOffsetY
        
        val maxDragDistance = with(density) { targetOffsetY.toPx() }
        val dragProgress = if (maxDragDistance > 0f) (dragOffsetY.value / maxDragDistance).coerceIn(0f, 1f) else 0f
        val bgAlpha = 1f - dragProgress

        val startWidth = maxHeight
        val startHeight = maxHeight
        val startOffsetX = 0.dp
        val startOffsetY = 0.dp
        val startCorner = 0.dp

        val threshold = 0.70f
        
        val imgWidthTarget: androidx.compose.ui.unit.Dp
        val imgHeightTarget: androidx.compose.ui.unit.Dp
        val imgOffsetXTarget: androidx.compose.ui.unit.Dp
        val imgOffsetYTarget: androidx.compose.ui.unit.Dp
        val imageCornerTarget: androidx.compose.ui.unit.Dp
        val contentAlpha: Float
        
        if (dragProgress <= threshold) {
            val p1 = if (threshold > 0f) dragProgress / threshold else 0f
            imgWidthTarget = startWidth
            imgHeightTarget = startHeight
            imgOffsetXTarget = startOffsetX
            imgOffsetYTarget = startOffsetY
            imageCornerTarget = startCorner
            contentAlpha = (1f - p1).coerceIn(0f, 1f)
        } else {
            val p2 = if (threshold < 1f) (dragProgress - threshold) / (1f - threshold) else 1f
            imgWidthTarget = androidx.compose.ui.unit.lerp(startWidth, 40.dp, p2)
            imgHeightTarget = androidx.compose.ui.unit.lerp(startHeight, 40.dp, p2)
            imgOffsetXTarget = androidx.compose.ui.unit.lerp(startOffsetX, targetOffsetX, p2)
            imgOffsetYTarget = androidx.compose.ui.unit.lerp(startOffsetY, 0.dp, p2)
            imageCornerTarget = androidx.compose.ui.unit.lerp(startCorner, 20.dp, p2)
            contentAlpha = 0f
        }

        val imgWidth by androidx.compose.animation.core.animateDpAsState(imgWidthTarget, label = "imgWidth")
        val imgHeight by androidx.compose.animation.core.animateDpAsState(imgHeightTarget, label = "imgHeight")
        val imgOffsetX by androidx.compose.animation.core.animateDpAsState(imgOffsetXTarget, label = "imgOffsetX")
        val imgOffsetY by androidx.compose.animation.core.animateDpAsState(imgOffsetYTarget, label = "imgOffsetY")
        val imgCorner by androidx.compose.animation.core.animateDpAsState(imageCornerTarget, label = "imgCorner")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(rightSideAverageColor.copy(alpha = bgAlpha))
                .graphicsLayer {
                    translationY = dragOffsetY.value
                }
                .pointerInput(showLyrics, showQueue) {
                    if (!showLyrics && !showQueue) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val currentOffsetY = dragOffsetY.value
                                if (currentOffsetY > with(density) { 150.dp.toPx() }) {
                                    scope.launch {
                                        dragOffsetY.animateTo(
                                            targetValue = maxDragDistance,
                                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                                        )
                                        onClose()
                                    }
                                } else {
                                    scope.launch {
                                        dragOffsetY.animateTo(0f, spring())
                                    }
                                }
                            }
                        ) { change, dragAmount ->
                            if (dragAmount > 0f || dragOffsetY.value > 0f) {
                                val newOffset = (dragOffsetY.value + dragAmount * 0.7f).coerceAtLeast(0f)
                                scope.launch { dragOffsetY.snapTo(newOffset) }
                            }
                        }
                    }
                }
        ) {



        // 2. Left side Album Art (with morphing layout)
        Box(
            modifier = Modifier
                .offset(x = imgOffsetX, y = imgOffsetY)
                .size(width = imgWidth, height = imgHeight)
                .clip(RoundedCornerShape(imgCorner))
        ) {
            // Sharp base cover
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(hdArtUrl)
                    .crossfade(true)
                    .build(),
                imageLoader = animatedImageLoader,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black,
                                    (0.75f + 0.25f * (1f - contentAlpha)).coerceIn(0.75f, 1.0f) to Color.Black,
                                    1.0f to Color.Black.copy(alpha = 1f - contentAlpha)
                                )
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )

            val currentAnimatedUrl = animatedArtworkUrl
            if (!currentAnimatedUrl.isNullOrBlank()) {
                DisposableEffect(Unit) {
                    onDispose {
                        onVideoPlayingChange(false)
                    }
                }
                com.mrtdk.liquid_glass.ui.components.AnimatedArtworkPlayer(
                    videoUrl = currentAnimatedUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { 
                            alpha = if (isVideoPlaying) 1f else 0f 
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Black,
                                        (0.75f + 0.25f * (1f - contentAlpha)).coerceIn(0.75f, 1.0f) to Color.Black,
                                        1.0f to Color.Black.copy(alpha = 1f - contentAlpha)
                                    )
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        },
                    isPaused = false,
                    enableFrameCapture = true,
                    onPlaybackStarted = { onVideoPlayingChange(true) },
                    onFrameCaptured = { frameBitmap ->
                        onCoverBitmapChange(frameBitmap.asImageBitmap())
                        try {
                            var r = 0L; var g = 0L; var b = 0L
                            val yCoord = frameBitmap.height - 1
                            val w = frameBitmap.width
                            for (x in 0 until w) {
                                val pixel = frameBitmap.getPixel(x, yCoord)
                                r += android.graphics.Color.red(pixel)
                                g += android.graphics.Color.green(pixel)
                                b += android.graphics.Color.blue(pixel)
                            }
                            val avgColor = Color((r / w).toInt(), (g / w).toInt(), (b / w).toInt())
                            onBottomAverageColorChange(avgColor)
                            onDominantColorChange(avgColor)

                            // Promedio de la columna derecha de píxeles
                            var rRight = 0L; var gRight = 0L; var bRight = 0L
                            val xCoord = frameBitmap.width - 1
                            val h = frameBitmap.height
                            for (y in 0 until h) {
                                val pixel = frameBitmap.getPixel(xCoord, y)
                                rRight += android.graphics.Color.red(pixel)
                                gRight += android.graphics.Color.green(pixel)
                                bRight += android.graphics.Color.blue(pixel)
                            }
                            onRightSideAverageColorChange(Color((rRight / h).toInt(), (gRight / h).toInt(), (bRight / h).toInt()))
                        } catch (e: Exception) { }
                    }
                )
            }

            // Blurred overlay to smooth the transition on the right edge
            val currentBitmap = coverBitmap
            if (currentBitmap != null && contentAlpha > 0f) {
                Image(
                    bitmap = currentBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { 
                            alpha = contentAlpha
                            compositingStrategy = CompositingStrategy.Offscreen 
                        }
                        .cloudy(radius = 100)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.70f to Color.Transparent,
                                        0.85f to Color.Black,
                                        1.0f to Color.Transparent
                                    )
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                )
            } else if (contentAlpha > 0f) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(hdArtUrl)
                        .size(150)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { 
                            alpha = contentAlpha
                            compositingStrategy = CompositingStrategy.Offscreen 
                        }
                        .cloudy(radius = 100)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.70f to Color.Transparent,
                                        0.85f to Color.Black,
                                        1.0f to Color.Transparent
                                    )
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                )
            }
        }

        // 3. Right side Content
        val rightSideWidth = maxWidth - maxHeight
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(rightSideWidth)
                .fillMaxHeight()
                .padding(vertical = 16.dp, horizontal = 24.dp)
                .graphicsLayer {
                    alpha = contentAlpha
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Shared Top Header Row: Title/Artist and Star/Options buttons
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var dragAccumulator by remember { mutableStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(playerState) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (dragAccumulator < -60f) { onSkipNext() }
                                        else if (dragAccumulator > 60f) { onSkipPrevious() }
                                        dragAccumulator = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        dragAccumulator += dragAmount
                                        change.consume()
                                    }
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (playerState?.videoId != null) {
                                        onArtistSelected(
                                            com.mrtdk.liquid_glass.ui.screens.ArtistState(
                                                id = playerState.artist,
                                                name = playerState.artist,
                                                thumbnail = null
                                            )
                                        )
                                    }
                                }
                        ) {
                            Text(
                                text = playerState?.title ?: "",
                                color = contentColor,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = playerState?.artist ?: "",
                                color = contentColor.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSaved) contentColor else contentColor.copy(alpha = 0.15f)
                                )
                                .clickable {
                                    if (playerState != null) {
                                        if (!isSaved) {
                                            LibraryManager.saveItem(
                                                LibraryItem(
                                                    playerState.videoId ?: "",
                                                    playerState.title,
                                                    playerState.artist,
                                                    playerState.artUrl?.toString(),
                                                    ItemType.SONG
                                                )
                                            )
                                        } else {
                                            LibraryManager.removeItem(playerState.videoId ?: "")
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = "file:///android_asset/img reproductor/c.png",
                                contentDescription = "Fav",
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                    if (isSaved) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor
                                ),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(contentColor.copy(alpha = 0.15f))
                                .clickable { onShowOptionsMenu() },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(18.dp)) {
                                val r = 1.5.dp.toPx()
                                val space = 4.dp.toPx()
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                drawCircle(contentColor, radius = r, center = Offset(cx - space - r * 2, cy))
                                drawCircle(contentColor, radius = r, center = Offset(cx, cy))
                                drawCircle(contentColor, radius = r, center = Offset(cx + space + r * 2, cy))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Switch right column contents based on showLyrics or showQueue
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (showLyrics) {
                        // View 2: Lyrics View
                        Box(modifier = Modifier.fillMaxSize()) {
                            val lyricsListState = rememberLazyListState()
                            val currentLyricsLines = lyricsLines
                            if (currentLyricsLines != null && currentLyricsLines.isNotEmpty()) {
                                val isSynced = currentLyricsLines.any { it.timeMs > 0L }
                                
                                LaunchedEffect(currentPosition, currentLyricsLines) {
                                    if (!isSynced) return@LaunchedEffect
                                    val currentIdx = currentLyricsLines.indexOfLast { it.timeMs != -1L && it.timeMs <= currentPosition + 500 }
                                    if (currentIdx >= 0 && !lyricsListState.isScrollInProgress) {
                                        lyricsListState.animateScrollToItem(currentIdx.coerceAtLeast(0), scrollOffset = -100)
                                    }
                                }

                                LazyColumn(
                                    state = lyricsListState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    item { Spacer(modifier = Modifier.height(16.dp)) }
                                    items(currentLyricsLines.size) { i ->
                                        val line = currentLyricsLines[i]
                                        val isCurrent = isSynced && line.timeMs != -1L && currentPosition >= line.timeMs && 
                                            (i == currentLyricsLines.lastIndex || currentPosition < currentLyricsLines[i+1].timeMs)
                                        val isPast = isSynced && line.timeMs != -1L && currentPosition > line.timeMs
                                        val distance = if (isSynced) {
                                            val curIdx = currentLyricsLines.indexOfLast { it.timeMs != -1L && it.timeMs <= currentPosition + 500 }
                                            if (curIdx >= 0) kotlin.math.abs(i - curIdx) else 0
                                        } else 0
                                        
                                        val lineDuration = remember(line.timeMs) {
                                            val nextLineTime = currentLyricsLines.getOrNull(i + 1)?.timeMs
                                            if (nextLineTime != null && nextLineTime > 0 && line.timeMs > 0) nextLineTime - line.timeMs else 4000L
                                        }
                                        val activeDuration = (lineDuration * 0.95).toLong().coerceAtLeast(300L)
                                        val lineRelTime = if (isCurrent && line.timeMs > 0) (currentPosition - line.timeMs).coerceAtLeast(0L) else if (isPast) activeDuration else 0L
                                        
                                        val targetAlpha = when {
                                            !isSynced || isCurrent -> 1f
                                            distance == 1 -> 0.55f
                                            distance == 2 -> 0.4f
                                            else -> 0.3f
                                        }
                                        val targetScale = when {
                                            !isSynced || isCurrent -> 1.03f
                                            distance == 1 -> 0.97f
                                            distance >= 2 -> 0.88f
                                            else -> 1f
                                        }
                                        val targetBlur = if (!isCurrent && isSynced) {
                                            when (distance) {
                                                1 -> 2f
                                                2 -> 3f
                                                else -> 4f
                                            }
                                        } else 0f
                                        
                                        val animAlpha by animateFloatAsState(targetAlpha, animationSpec = tween(260), label="lyricsAlpha")
                                        val animScale by animateFloatAsState(targetScale, animationSpec = tween(320), label="lyricsScale")
                                        val animBlur by animateFloatAsState(targetBlur, animationSpec = tween(420), label="lyricsBlur")
                                        
                                        val wordData = remember(line.text, activeDuration) {
                                            val words = line.text.split(" ").filter { it.isNotEmpty() }
                                            if (words.isEmpty()) {
                                                listOf(Triple(line.text, 0L, activeDuration))
                                            } else {
                                                val totalChars = line.text.length
                                                var accumulatedTime = 0L
                                                words.mapIndexed { wordIndex, word ->
                                                    val charCount = if (wordIndex < words.lastIndex) word.length + 1 else word.length
                                                    val wordStart = accumulatedTime
                                                    val wordDur = if (totalChars > 0) (activeDuration * charCount.toFloat() / totalChars).toLong() else activeDuration
                                                    accumulatedTime += wordDur
                                                    Triple(if (wordIndex < words.lastIndex) "$word " else word, wordStart, wordStart + wordDur)
                                                }
                                            }
                                        }

                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    scaleX = animScale; scaleY = animScale
                                                    alpha = animAlpha
                                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                                }
                                                .then(if (animBlur > 0f) Modifier.blur(animBlur.dp) else Modifier)
                                                .clickable { 
                                                    if(line.timeMs != -1L) onSeek(line.timeMs)
                                                },
                                            horizontalArrangement = Arrangement.Start,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            wordData.forEach { (wordText, startRelative, endRelative) ->
                                                val wordDuration = (endRelative - startRelative).coerceAtLeast(1L)
                                                val wordProgress by animateFloatAsState(
                                                    targetValue = when {
                                                        lineRelTime >= endRelative -> 1f
                                                        lineRelTime < startRelative -> 0f
                                                        else -> (lineRelTime - startRelative).toFloat() / wordDuration
                                                    },
                                                    animationSpec = tween(
                                                        durationMillis = wordDuration.coerceIn(140L, 260L).toInt(),
                                                        easing = FastOutSlowInEasing
                                                    ),
                                                    label = "wordProgress"
                                                )
                                                val finalFontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold
                                                Text(
                                                    text = wordText,
                                                    fontSize = 24.sp,
                                                    style = TextStyle(
                                                        brush = if (isCurrent) Brush.horizontalGradient(
                                                            0.0f to contentColor,
                                                            (wordProgress - 0.05f).coerceAtLeast(0f) to contentColor,
                                                            (wordProgress + 0.05f).coerceAtMost(1f) to contentColor.copy(alpha = 0.4f),
                                                            1.0f to contentColor.copy(alpha = 0.4f)
                                                        ) else null,
                                                        fontWeight = finalFontWeight,
                                                        lineHeight = 32.sp,
                                                        shadow = if (isCurrent && wordProgress > 0.1f) Shadow(
                                                            color = contentColor.copy(alpha = 0.6f * wordProgress),
                                                            offset = Offset.Zero,
                                                            blurRadius = (12f * wordProgress).coerceAtLeast(0.1f)
                                                        ) else null
                                                    ),
                                                    color = if (!isCurrent) contentColor else Color.Unspecified
                                                )
                                            }
                                        }
                                    }
                                    item { Spacer(modifier = Modifier.height(80.dp)) }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = contentColor)
                                }
                            }
                        }
                    } else if (showQueue) {
                        // View 3: Queue View
                        val queueListState = rememberLazyListState()
                        Column(modifier = Modifier.fillMaxSize()) {
                            val shuffleInteraction = remember { MutableInteractionSource() }
                            val isShuffleActive = shuffleModeEnabled
                            val isShufflePressed by shuffleInteraction.collectIsPressedAsState()
                            val shuffleScale by animateFloatAsState(targetValue = if (isShufflePressed) 0.85f else 1.0f, label = "shuffleScale")
                            
                            val activeBg = contentColor.copy(alpha = 0.9f)
                            val activeIcon = if (contentColor == Color.White) rightSideAverageColor else Color.White

                            val shuffleBgColor by animateColorAsState(targetValue = if (isShuffleActive) activeBg else contentColor.copy(alpha=0.15f), label = "shuffleBg")
                            val shuffleIconColor by animateColorAsState(targetValue = if (isShuffleActive) activeIcon else contentColor.copy(alpha=0.5f), label = "shuffleIcon")

                            val repeatInteraction = remember { MutableInteractionSource() }
                            val isRepeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF
                            val isRepeatPressed by repeatInteraction.collectIsPressedAsState()
                            val repeatScale by animateFloatAsState(targetValue = if (isRepeatPressed) 0.85f else 1.0f, label = "repeatScale")
                            val repeatBgColor by animateColorAsState(targetValue = if (isRepeatActive) activeBg else contentColor.copy(alpha=0.15f), label = "repeatBg")
                            val repeatIconColor by animateColorAsState(targetValue = if (isRepeatActive) activeIcon else contentColor.copy(alpha=0.5f), label = "repeatIcon")

                            val autoplayInteraction = remember { MutableInteractionSource() }
                            val isAutoplayActive = playerState?.isExclusiveQueue != true
                            val isAutoplayPressed by autoplayInteraction.collectIsPressedAsState()
                            val autoplayScale by animateFloatAsState(targetValue = if (isAutoplayPressed) 0.85f else 1.0f, label = "autoplayScale")
                            val autoplayBgColor by animateColorAsState(targetValue = if (isAutoplayActive) activeBg else contentColor.copy(alpha=0.15f), label = "autoplayBg")
                            val autoplayIconColor by animateColorAsState(targetValue = if (isAutoplayActive) activeIcon else contentColor.copy(alpha=0.5f), label = "autoplayIcon")

                            val romajiInteraction = remember { MutableInteractionSource() }
                            val isRomajiActive = isRomajiEnabled
                            val isRomajiPressed by romajiInteraction.collectIsPressedAsState()
                            val romajiScale by animateFloatAsState(targetValue = if (isRomajiPressed) 0.85f else 1.0f, label = "romajiScale")
                            val romajiBgColor by animateColorAsState(targetValue = if (isRomajiActive) activeBg else contentColor.copy(alpha=0.15f), label = "romajiBg")
                            val romajiIconColor by animateColorAsState(targetValue = if (isRomajiActive) activeIcon else contentColor.copy(alpha=0.5f), label = "romajiIcon")

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = shuffleScale, scaleY = shuffleScale)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(shuffleBgColor)
                                        .clickable(
                                            interactionSource = shuffleInteraction,
                                            indication = null,
                                            onClick = onToggleShuffle
                                        ),
                                    contentAlignment=Alignment.Center
                                ) {
                                    Icon(painterResource(id = R.drawable.shuffle), "Shuffle", tint=shuffleIconColor, modifier=Modifier.size(18.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = repeatScale, scaleY = repeatScale)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(repeatBgColor)
                                        .clickable(
                                            interactionSource = repeatInteraction,
                                            indication = null,
                                            onClick = onToggleRepeat
                                        ),
                                    contentAlignment=Alignment.Center
                                ) {
                                    Icon(if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint=repeatIconColor, modifier=Modifier.size(18.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = autoplayScale, scaleY = autoplayScale)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(autoplayBgColor)
                                        .clickable(
                                            interactionSource = autoplayInteraction,
                                            indication = null,
                                            onClick = {
                                                if (playerState != null) {
                                                    onSongSelected(playerState.copy(isExclusiveQueue = !playerState.isExclusiveQueue))
                                                }
                                            }
                                        ),
                                    contentAlignment=Alignment.Center
                                ) {
                                    Icon(Icons.Default.AllInclusive, "Autoplay", tint=autoplayIconColor, modifier=Modifier.size(18.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = romajiScale, scaleY = romajiScale)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(romajiBgColor)
                                        .clickable(
                                            interactionSource = romajiInteraction,
                                            indication = null,
                                            onClick = onToggleRomaji
                                        ),
                                    contentAlignment=Alignment.Center
                                ) {
                                    Icon(if (isRomajiActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff, "Toggle", tint=romajiIconColor, modifier=Modifier.size(22.dp))
                                }
                            }

                            if (playerState != null && playerState.queue.isNotEmpty()) {
                                Text(text = stringResource(R.string.siguiente_en_album_playlist), color=contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=8.dp, bottom=6.dp))
                            } else if (playerState?.isExclusiveQueue != true) {
                                Column(modifier = Modifier.padding(top=8.dp, bottom=6.dp)) {
                                    Text(text = stringResource(R.string.continue_playing), color=contentColor, fontSize=16.sp, fontWeight=FontWeight.Bold)
                                    Text(text = stringResource(R.string.autoplaying_similar_music), color=contentColor.copy(alpha=0.7f), fontSize=12.sp)
                                }
                            }

                            LazyColumn(
                                state = queueListState,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (playerState != null && playerState.queue.isNotEmpty()) {
                                    items(playerState.queue.size) { index ->
                                        val qItem = playerState.queue[index]
                                        val upgradedArt = qItem.artUrl?.let {
                                            val itStr = it.toString()
                                            if (itStr.startsWith("file:///android_asset/")) {
                                                it
                                            } else {
                                                val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(itStr) ?: itStr
                                                if (it is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
                                            }
                                        } ?: qItem.artUrl

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                val remaining = playerState.queue.drop(index + 1)
                                                onSongSelectedFromQueue(PlayerState(
                                                    title = qItem.title,
                                                    artist = qItem.artist,
                                                    artUrl = upgradedArt,
                                                    videoId = qItem.videoId,
                                                    queue = remaining,
                                                    isExclusiveQueue = playerState.isExclusiveQueue,
                                                    album = qItem.album,
                                                    albumId = qItem.albumId
                                                ))
                                            }
                                        ) {
                                            AsyncImage(model = ImageRequest.Builder(context).data(upgradedArt).crossfade(false).build(), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(qItem.title, color = contentColor, fontSize = 15.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                                Text(qItem.artist, color = contentColor.copy(alpha=0.6f), fontSize = 13.sp, maxLines = 1)
                                            }
                                            Icon(Icons.Default.Menu, contentDescription=null, tint=contentColor.copy(alpha=0.6f))
                                        }
                                    }
                                }
                                if (playerState?.isExclusiveQueue != true) {
                                    items(upNextSongs.size) { i ->
                                        val song = upNextSongs[i]
                                        val hdThumb = song.thumbnail?.let {
                                            com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(it) ?: it
                                        } ?: song.thumbnail
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                val upgradedArt = song.thumbnail?.let {
                                                    com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(it) ?: it
                                                } ?: song.thumbnail
                                                onUpNextSongsChange(upNextSongs.drop(i + 1))
                                                onSongSelectedFromQueue(PlayerState(
                                                    title = song.title,
                                                    artist = song.artists.joinToString { it.name },
                                                    artUrl = upgradedArt,
                                                    videoId = song.id,
                                                    isExclusiveQueue = playerState?.isExclusiveQueue ?: false,
                                                    album = song.album?.name,
                                                    albumId = song.album?.id
                                                ))
                                            }
                                        ) {
                                            AsyncImage(model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(song.title, color = contentColor, fontSize = 15.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                                Text(song.artists.joinToString { it.name }, color = contentColor.copy(alpha=0.6f), fontSize = 13.sp, maxLines = 1)
                                            }
                                            Icon(Icons.Default.Menu, contentDescription=null, tint=contentColor.copy(alpha=0.6f))
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(40.dp)) }
                            }
                        }
                    } else {
                        // View 1: Main Controls Layout (Slider, Controls, Volume)
                        Column(modifier = Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.weight(1f))

                            // Apple Music slider
                            val progressVal = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                            AppleMusicSlider(
                                value = progressVal,
                                onValueChange = { onSeek((it * duration).toLong()) },
                                modifier = Modifier.fillMaxWidth().height(24.dp),
                                activeColor = contentColor,
                                inactiveColor = contentColor.copy(alpha = 0.3f),
                                barHeightDp = 8.dp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Playback controls (Prev, Play, Next)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedSkipButton(
                                    iconId = R.drawable.previous,
                                    contentDescription = "Previous",
                                    contentColor = contentColor,
                                    sizeDp = 84.dp,
                                    iconSizeDp = 64.dp,
                                    onClick = onSkipPrevious
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                val playPauseInteractionSource = remember { MutableInteractionSource() }
                                val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
                                val playPauseBgColor by animateColorAsState(
                                    targetValue = if (isPlayPausePressed) contentColor.copy(alpha = 0.12f) else Color.Transparent,
                                    label = "playPauseBg"
                                )
                                val playPauseRotation by animateFloatAsState(
                                    targetValue = if (isPlaying) 180f else 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "playPauseButtonRotation"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape)
                                        .background(playPauseBgColor)
                                        .clickable(
                                            interactionSource = playPauseInteractionSource,
                                            indication = androidx.compose.foundation.LocalIndication.current,
                                            onClick = onTogglePlayPause
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedContent(
                                        targetState = isPlaying,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)))
                                                .togetherWith(fadeOut(animationSpec = tween(90)) + scaleOut(targetScale = 0.3f, animationSpec = tween(90)))
                                        },
                                        label = "playPauseIcon"
                                    ) { playing ->
                                        Icon(
                                            painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.resume),
                                            contentDescription = if (playing) "Pause" else "Play",
                                            tint = contentColor,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .graphicsLayer {
                                                    rotationZ = playPauseRotation
                                                }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))
                                AnimatedSkipButton(
                                    iconId = R.drawable.forward,
                                    contentDescription = "Next",
                                    contentColor = contentColor,
                                    sizeDp = 84.dp,
                                    iconSizeDp = 64.dp,
                                    onClick = onSkipNext
                                )
                            }

                            Spacer(modifier = Modifier.height(40.dp))

                            // Volume Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.albumspeaker),
                                    contentDescription = "Low volume",
                                    tint = contentColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                                AppleMusicSlider(
                                    value = volumePosition,
                                    onValueChange = { v ->
                                        onVolumePositionChange(v)
                                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (v * maxVolume).toInt(), 0)
                                        onVolumeChange(v)
                                    },
                                    modifier = Modifier.weight(1f).height(24.dp),
                                    activeColor = contentColor,
                                    inactiveColor = contentColor.copy(alpha = 0.2f),
                                    barHeightDp = 8.dp
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.albumspeakerlarge),
                                    contentDescription = "High volume",
                                    tint = contentColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Shared Bottom Bar Buttons Row (Lyrics, Cast/Format, Queue)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (showLyrics) contentColor else Color.Transparent)
                            .clickable {
                                onShowLyricsChange(!showLyrics)
                                onShowQueueChange(false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = "file:///android_asset/img reproductor/Letras.png",
                            contentDescription = "Lyrics",
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                if (showLyrics) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AsyncImage(
                        model = "file:///android_asset/img reproductor/parlante.png",
                        contentDescription = "Format",
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor),
                        modifier = Modifier
                            .height(20.dp)
                            .padding(horizontal = 8.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* do nothing */ }
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (showQueue) contentColor else Color.Transparent)
                            .clickable {
                                onShowQueueChange(!showQueue)
                                onShowLyricsChange(false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.nextinfo),
                            contentDescription = "Next Info",
                            tint = if (showQueue) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        }
    }
}
