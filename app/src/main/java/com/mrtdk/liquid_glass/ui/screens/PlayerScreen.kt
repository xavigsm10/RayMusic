package com.mrtdk.liquid_glass.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
    val albumId: String? = null
)

data class QueueItem(
    val title: String,
    val artist: String,
    val artUrl: Any?,
    val videoId: String? = null,
    val album: String? = null,
    val albumId: String? = null
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
        val skinTone = Color(0xFF6E472A) // Rich brown skin tone
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
        val hdArtUrl = remember(playerState?.artUrl, playerState?.title, playerState?.artist) {
            val url = playerState?.artUrl
            val urlString = url?.toString() ?: ""

            // Check if it is already a local asset url passed from AlbumScreen
            val isLocalArt = urlString.startsWith("file:///android_asset/")

            when {
                isLocalArt -> url
                url is String -> {
                    when {
                        url.contains("=w") || url.contains("=s") -> {
                            val index = url.indexOf("=w").takeIf { it != -1 } ?: url.indexOf("=s")
                            url.substring(0, index) + "=w1200-h1200-l90-rj"
                        }
                        url.contains("ytimg.com/vi/") -> url
                            .replace("hqdefault", "maxresdefault")
                            .replace("mqdefault", "maxresdefault")
                            .replace("sddefault", "maxresdefault")
                            .replace("default", "maxresdefault")
                        else -> url
                    }
                }
                else -> url
            }
        }

        var dominantColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }
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
                            val isBillieJean = playerState?.title?.contains("billie jean", ignoreCase = true) == true ||
                                               playerState?.artist?.contains("michael jackson", ignoreCase = true) == true
                            val sampledColor = extractDominantColor(bitmap, isBillieJean)
                            dominantColor = sampledColor
                            onDominantColorChanged(sampledColor)
                        } catch (e: Exception) { }
                    }
                }
            }
        }
        
        val isLightBackground = dominantColor.luminance() > 0.35f
        val contentColor = if (isLightBackground) Color(0xFF1A1A1A) else Color.White

        val animatedImageLoader = remember(context) {
            coil.ImageLoader.Builder(context)
                .components {
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { parentCoordinates = it }
                .layerBackdrop(localBackdrop)
        ) {
            val maxWidth = maxWidth
            val maxHeight = maxHeight

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
            val dragProgress = if (maxDragDistance > 0f) (offsetY / maxDragDistance).coerceIn(0f, 1f) else 0f
            
            val bgAlpha = 1f - dragProgress
            
            val expandedWidth = maxWidth
            val expandedHeight = maxWidth * 1.15f
            val expandedX = 0.dp
            val expandedY = 0.dp
            
            val threshold = 0.70f
            
            val imgWidthTarget: androidx.compose.ui.unit.Dp
            val imgHeightTarget: androidx.compose.ui.unit.Dp
            val imgOffsetXTarget: androidx.compose.ui.unit.Dp
            val imgOffsetYTarget: androidx.compose.ui.unit.Dp
            val imageCornerTarget: androidx.compose.ui.unit.Dp
            val contentAlpha: Float
            
            if (isOverlayActive) {
                imgWidthTarget = lyricsImageSize
                imgHeightTarget = lyricsImageSize
                imgOffsetXTarget = 24.dp
                imgOffsetYTarget = 64.dp
                imageCornerTarget = 8.dp
                contentAlpha = 1f
            } else {
                if (dragProgress <= threshold) {
                    val p1 = if (threshold > 0f) dragProgress / threshold else 0f
                    imgWidthTarget = expandedWidth
                    imgHeightTarget = expandedHeight
                    imgOffsetXTarget = expandedX
                    imgOffsetYTarget = expandedY
                    imageCornerTarget = 12.dp
                    contentAlpha = (1f - p1).coerceIn(0f, 1f)
                } else {
                    val p2 = if (threshold < 1f) (dragProgress - threshold) / (1f - threshold) else 1f
                    imgWidthTarget = androidx.compose.ui.unit.lerp(expandedWidth, 40.dp, p2)
                    imgHeightTarget = androidx.compose.ui.unit.lerp(expandedHeight, 40.dp, p2)
                    
                    imgOffsetXTarget = androidx.compose.ui.unit.lerp(expandedX, targetOffsetX, p2)
                    imgOffsetYTarget = expandedY
                    
                    imageCornerTarget = androidx.compose.ui.unit.lerp(12.dp, 20.dp, p2)
                    contentAlpha = 0f
                }
            }

            val imgWidth by androidx.compose.animation.core.animateDpAsState(imgWidthTarget)
            val imgHeight by androidx.compose.animation.core.animateDpAsState(imgHeightTarget)
            val imgOffsetX by androidx.compose.animation.core.animateDpAsState(imgOffsetXTarget)
            val imgOffsetY by androidx.compose.animation.core.animateDpAsState(imgOffsetYTarget)
            val imgCorner by androidx.compose.animation.core.animateDpAsState(imageCornerTarget)

            val nestedScrollConnection = remember(maxDragDistance, onClose) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
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
                    .background(dominantColor.copy(alpha = bgAlpha))
                    .drawWithContent {
                        if (dragProgress == 0f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        dominantColor.copy(alpha = 0.25f),
                                        dominantColor.copy(alpha = 0.7f),
                                        dominantColor
                                    ),
                                    startY = this.size.height * 0.55f,
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
            if (currentCoverBitmap != null && !isOverlayActive && dragProgress == 0f) {
                val overlapDp = 10.dp
                val density = androidx.compose.ui.platform.LocalDensity.current
                val parentCoords = parentCoordinates
                val sliderCoords = sliderCoordinates
                val sliderYInParent = if (parentCoords != null && sliderCoords != null && parentCoords.isAttached && sliderCoords.isAttached) {
                    parentCoords.localPositionOf(sliderCoords, androidx.compose.ui.geometry.Offset.Zero).y
                } else {
                    0f
                }
                val sliderYDp = with(density) { sliderYInParent.toDp() }
                
                val blurHeight = if (sliderYDp > 0.dp) {
                    (sliderYDp - (imgOffsetY + imgHeight - overlapDp)).coerceAtLeast(0.dp)
                } else {
                    maxHeight - (imgOffsetY + imgHeight) + overlapDp
                }
                // Caja del reflejo posicionada bajo la portada, desvaneciéndose suavemente al fondo
                Box(
                    modifier = Modifier
                        .offset(x = 0.dp, y = imgOffsetY + imgHeight - overlapDp)
                        .fillMaxWidth()
                        .height(blurHeight)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .blur(25.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Black,
                                        0.6f to Color.Black.copy(alpha = 0.8f),
                                        1.0f to Color.Transparent
                                    )
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val sampleHeight = 5 // Altura de muestreo del borde
                        drawImage(
                            image = currentCoverBitmap,
                            srcOffset = IntOffset(0, currentCoverBitmap.height - sampleHeight),
                            srcSize = IntSize(currentCoverBitmap.width, sampleHeight),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            filterQuality = FilterQuality.Low
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
                        // Apply offscreen strategy only when player is fully expanded and overlay is inactive
                        compositingStrategy = if (!isOverlayActive && dragProgress == 0f) {
                            CompositingStrategy.Offscreen
                        } else {
                            CompositingStrategy.Auto
                        }
                    }
                    .then(
                        if (!isOverlayActive && dragProgress == 0f) {
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

                if (!currentAnimatedUrl.isNullOrBlank() && (dragProgress == 0f || dragProgress == 1f)) {
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
                                val isBillieJean = playerState?.title?.contains("billie jean", ignoreCase = true) == true ||
                                                   playerState?.artist?.contains("michael jackson", ignoreCase = true) == true
                                val sampledColor = extractDominantColor(frameBitmap, isBillieJean)
                                dominantColor = sampledColor
                                onDominantColorChanged(sampledColor)
                            } catch (e: Exception) { }
                        }
                    )
                }

                // Blurred bottom overlay to fade/blur the bottom of the image
                if (!isOverlayActive && dragProgress == 0f) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(hdArtUrl)
                            .size(150) // Downsample to 150x150 for a much more aggressive, premium soft blur
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




            // LYRICS / QUEUE OVERLAY
            AnimatedVisibility(
                visible = showLyrics || showQueue,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                 Column(modifier = Modifier.fillMaxSize()) {
                      Row(
                          modifier = Modifier.fillMaxWidth().padding(top = 64.dp, start = 124.dp, end = 24.dp), 
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
                            Spacer(modifier = Modifier.height(44.dp))
                            
                            val shuffleInteraction = remember { MutableInteractionSource() }
                            val isShuffleActive = shuffleModeEnabled
                            val isShufflePressed by shuffleInteraction.collectIsPressedAsState()
                            val shuffleScale by animateFloatAsState(targetValue = if (isShufflePressed) 0.85f else 1.0f, label = "shuffleScale")
                            val shuffleBgColor by animateColorAsState(targetValue = if (isShuffleActive) Color(0xFFFA243C) else contentColor.copy(alpha=0.15f), label = "shuffleBg")
                            val shuffleIconColor by animateColorAsState(targetValue = if (isShuffleActive) Color.White else contentColor.copy(alpha=0.5f), label = "shuffleIcon")

                            val repeatInteraction = remember { MutableInteractionSource() }
                            val isRepeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF
                            val isRepeatPressed by repeatInteraction.collectIsPressedAsState()
                            val repeatScale by animateFloatAsState(targetValue = if (isRepeatPressed) 0.85f else 1.0f, label = "repeatScale")
                            val repeatBgColor by animateColorAsState(targetValue = if (isRepeatActive) Color(0xFFFA243C) else contentColor.copy(alpha=0.15f), label = "repeatBg")
                            val repeatIconColor by animateColorAsState(targetValue = if (isRepeatActive) Color.White else contentColor.copy(alpha=0.5f), label = "repeatIcon")

                            val autoplayInteraction = remember { MutableInteractionSource() }
                            val isAutoplayActive = !playerState.isExclusiveQueue
                            val isAutoplayPressed by autoplayInteraction.collectIsPressedAsState()
                            val autoplayScale by animateFloatAsState(targetValue = if (isAutoplayPressed) 0.85f else 1.0f, label = "autoplayScale")
                            val autoplayBgColor by animateColorAsState(targetValue = if (isAutoplayActive) Color(0xFFFA243C) else contentColor.copy(alpha=0.15f), label = "autoplayBg")
                            val autoplayIconColor by animateColorAsState(targetValue = if (isAutoplayActive) Color.White else contentColor.copy(alpha=0.5f), label = "autoplayIcon")

                            val romajiInteraction = remember { MutableInteractionSource() }
                            val isRomajiActive = isRomajiEnabled
                            val isRomajiPressed by romajiInteraction.collectIsPressedAsState()
                            val romajiScale by animateFloatAsState(targetValue = if (isRomajiPressed) 0.85f else 1.0f, label = "romajiScale")
                            val romajiBgColor by animateColorAsState(targetValue = if (isRomajiActive) Color(0xFFFA243C) else contentColor.copy(alpha=0.15f), label = "romajiBg")
                            val romajiIconColor by animateColorAsState(targetValue = if (isRomajiActive) Color.White else contentColor.copy(alpha=0.5f), label = "romajiIcon")

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
                               Text(text = stringResource(R.string.siguiente_en_album_playlist), color=contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=32.dp, start=24.dp, end=24.dp, bottom=16.dp))
                            } else if (playerState?.isExclusiveQueue != true) {
                                 Column(modifier = Modifier.padding(top=32.dp, start=24.dp, end=24.dp, bottom=16.dp)) {
                                     Text(text = stringResource(R.string.continue_playing), color=contentColor, fontSize=18.sp, fontWeight=FontWeight.Bold)
                                     Text(text = stringResource(R.string.autoplaying_similar_music), color=contentColor.copy(alpha=0.7f), fontSize=14.sp)
                                 }
                             }
                          
                           LazyColumn(state = queueListState, modifier = Modifier.weight(1f).padding(horizontal = 24.dp).nestedScroll(nestedScrollConnection), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                              // 1. Manual Queue Section (Album/Playlist)
                              if (playerState != null && playerState.queue.isNotEmpty()) {
                                   items(playerState.queue.size) { index ->
                                       val qItem = playerState.queue[index]
                                       val upgradedArt = qItem.artUrl?.let {
                                           val itStr = it.toString()
                                           if (itStr.contains("=w") || itStr.contains("=s")) {
                                               val idx = itStr.indexOf("=w").takeIf { j -> j != -1 } ?: itStr.indexOf("=s")
                                               itStr.substring(0, idx) + "=w1200-h1200-l90-rj"
                                           } else if (itStr.contains("ytimg.com/vi/")) {
                                               itStr.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault").replace("sddefault", "maxresdefault").replace("default", "maxresdefault")
                                           } else it
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
                                               isExclusiveQueue = playerState.isExclusiveQueue
                                           ))
                                       }) {
                                          AsyncImage(model = ImageRequest.Builder(context).data(qItem.artUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
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
                                       if (it.contains("=w") || it.contains("=s")) {
                                           val idx = it.indexOf("=w").takeIf { j -> j != -1 } ?: it.indexOf("=s")
                                           it.substring(0, idx) + "=w540-h540-l90-rj"
                                       }
                                       else if (it.contains("ytimg.com/vi/")) it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                       else it
                                   } ?: song.thumbnail
                                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                                      swipeDirection = 1
                                      val upgradedArt = song.thumbnail?.let {
                                          if (it.contains("=w")) it.substringBefore("=w") + "=w1200-h1200-l90-rj"
                                          else if (it.contains("ytimg.com/vi/")) it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                          else it
                                      } ?: song.thumbnail
                                      // Remove clicked song and all before it from the queue
                                      onUpNextSongsChange(upNextSongs.drop(i + 1))
                                      onSongSelectedFromQueue(PlayerState(
                                          title = song.title,
                                          artist = song.artists.joinToString { it.name },
                                          artUrl = upgradedArt,
                                          videoId = song.id,
                                          isExclusiveQueue = playerState.isExclusiveQueue
                                      ))
                                  }) {
                                      AsyncImage(model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
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
                                         item { Spacer(modifier = Modifier.height(200.dp)) }
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
                      
                      AnimatedVisibility(
                           visible = !showLyrics || showLyricsControls,
                           enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                           exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                      ) {
                          Box(
                               modifier = Modifier
                                   .fillMaxWidth()
                                   .background( Brush.verticalGradient(listOf(Color.Transparent, dominantColor.copy(alpha=0.8f), dominantColor, dominantColor)) )
                           ) {
                               Column(
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .padding(horizontal = 24.dp)
                                       .padding(top = 40.dp) // difuminado padding
                                       .padding(bottom = 48.dp)
                               ) {
                                  AppleMusicSlider(
                                      value = progress, onValueChange = { onSeek((it * duration).toLong()) },
                                      modifier = Modifier.fillMaxWidth().height(24.dp),
                                      activeColor = contentColor,
                                      inactiveColor = contentColor.copy(alpha = 0.3f),
                                      barHeightDp = 6.dp
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
                      // Gradient background of dominant color starting from bottom of cover image
                      Box(
                          modifier = Modifier
                              .offset(x = 0.dp, y = imgOffsetY + imgHeight)
                              .fillMaxWidth()
                              .height(maxHeight - (imgOffsetY + imgHeight))
                              .background(
                                  Brush.verticalGradient(
                                      colors = listOf(
                                          Color.Transparent,
                                          dominantColor.copy(alpha = 0.6f),
                                          dominantColor.copy(alpha = 0.95f)
                                      )
                                  )
                              )
                      )
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
                          barHeightDp = 6.dp
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

        if (showOptionsMenu) {
            com.mrtdk.liquid_glass.ui.components.PlayerOptionsMenu(
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
                barHeightDp = 6.dp
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
                onClick = onSkipPrevious
            )
            Spacer(modifier = Modifier.width(36.dp))
            Box(modifier = Modifier.size(72.dp).clickable { onTogglePlayPause() }, contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = if (isPlaying) R.drawable.pause else R.drawable.resume),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = contentColor,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.width(36.dp))
            AnimatedSkipButton(
                iconId = R.drawable.forward,
                contentDescription = "Next",
                contentColor = contentColor,
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
                Spacer(modifier = Modifier.width(16.dp))
                AppleMusicSlider(
                    value = volumePosition, onValueChange = { onVolumeChange(it) },
                    modifier = Modifier.weight(1f).height(24.dp),
                    activeColor = contentColor,
                    inactiveColor = contentColor.copy(alpha = 0.2f),
                    barHeightDp = 6.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
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
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(if (isPressed) 0.85f else 1f, label="")
    val bgAlpha by androidx.compose.animation.core.animateFloatAsState(if (isPressed) 0.15f else 0f, label="")
    
    Box(
        modifier = Modifier
            .size(56.dp)
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
                .size(52.dp)
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
    barHeightDp: androidx.compose.ui.unit.Dp = 6.dp
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
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    coroutineScope.launch {
        if (!silent) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Obteniendo enlace de descarga...", Toast.LENGTH_SHORT).show()
            }
        }
        val streamUrl = com.mrtdk.liquid_glass.playback.MusicPlayer.resolveUrl(videoId)
        if (streamUrl != null) {
            withContext(Dispatchers.Main) {
                try {
                    val uri = Uri.parse(streamUrl)
                    val cleanTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    val cleanArtist = artist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    val filename = "${cleanArtist} - ${cleanTitle}.mp3"
                    
                    val request = DownloadManager.Request(uri)
                        .setTitle("${artist} - ${title}")
                        .setDescription("Descargando canción para el modo offline...")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MUSIC, filename)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)
                        .setMimeType("audio/mpeg")
                    
                    val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(request)
                    
                    val fileUri = Uri.fromFile(java.io.File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), filename))
                    val libraryItem = LibraryItem(
                        id = videoId,
                        title = title,
                        subtitle = artist,
                        thumbnail = artUrl,
                        type = ItemType.SONG,
                        album = album
                    )
                    LibraryManager.saveDownloadedSong(libraryItem)
                    
                    LibraryManager.saveString("local_uri_$videoId", fileUri.toString())
                    
                    if (!silent) {
                        Toast.makeText(context, "Iniciando descarga: ${title}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al descargar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No se pudo obtener el enlace de descarga de YouTube", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
