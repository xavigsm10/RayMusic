package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import com.mrtdk.liquid_glass.ui.components.LiquidButton
import com.mrtdk.liquid_glass.ui.components.LocalBackdrop
import com.mrtdk.liquid_glass.ui.components.SharedElementTransitionContainer
import com.mrtdk.liquid_glass.ui.components.SharedTransitionState
import com.mrtdk.glass.GlassBox
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import com.mrtdk.liquid_glass.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.echo.innertube.YouTube
import com.echo.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import com.mrtdk.liquid_glass.ui.components.AppleMusicSongMenu
import com.mrtdk.liquid_glass.ui.components.AppleMusicAlbumMenu
import com.mrtdk.liquid_glass.ui.components.ContextMenuSong
import com.mrtdk.liquid_glass.ui.components.ContextMenuAlbum
import com.mrtdk.liquid_glass.playback.PlaybackQueue
import com.mrtdk.liquid_glass.ui.screens.QueueItem
import com.mrtdk.liquid_glass.ui.screens.PlayerState

data class AlbumState(
    val id: String,        // browseId
    val playlistId: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val year: Int? = null
)

@Composable
fun AlbumScreen(
    albumState: AlbumState,
    onBack: () -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    onArtistSelected: (com.mrtdk.liquid_glass.ui.screens.ArtistState) -> Unit = {},
    onAlbumSelected: (com.mrtdk.liquid_glass.ui.screens.AlbumState) -> Unit = {},
    onDominantColorChanged: (Color) -> Unit = {},
    isPaused: Boolean = false
) {
    val context = LocalContext.current
    var activeSongForMenu by remember { mutableStateOf<ContextMenuSong?>(null) }
    var showAlbumMenu by remember { mutableStateOf(false) }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    var tracks by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var dominantColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }
    var albumError by remember { mutableStateOf<String?>(null) }
    val savedItems by LibraryManager.savedItems.collectAsState()
    val isSaved = savedItems.any { it.id == albumState.id }

    val isMichaelAlbum = albumState.title.equals("Michael: Songs From the Motion Picture", ignoreCase = true)
    val isThrillerAlbum = albumState.title.equals("Thriller", ignoreCase = true) || 
                          (albumState.title.contains("Thriller", ignoreCase = true) && albumState.artist.contains("Michael Jackson", ignoreCase = true))
    val isAfterHoursAlbum = albumState.title.equals("After Hours", ignoreCase = true) ||
                            (albumState.title.contains("After Hours", ignoreCase = true) && albumState.artist.contains("The Weeknd", ignoreCase = true))
    val isAroundTheFurAlbum = albumState.title.equals("Around the Fur", ignoreCase = true) ||
                              (albumState.title.contains("Around the Fur", ignoreCase = true) && albumState.artist.contains("Deftones", ignoreCase = true))
    val isBadAlbum = albumState.title.equals("Bad", ignoreCase = true) &&
                     albumState.artist.contains("Michael Jackson", ignoreCase = true)
    // Albums that should never use animated artwork (wrong cache hits from similar-named albums)
    val isAnimatedArtworkBlocked = isMichaelAlbum

    val hdThumb = albumState.thumbnail
        ?.replace("=w226-h226", "=w720-h720")
        ?.replace("=w120-h120", "=w720-h720")

    val headerArt = hdThumb
    val songArtUrl = hdThumb

    val albumHeightRatio = when {
        isAroundTheFurAlbum -> 1.40f
        isAfterHoursAlbum -> 1.62f
        isMichaelAlbum -> 1.35f
        else -> 1.25f
    }

    val isVerticalAlbum = isAfterHoursAlbum || isAroundTheFurAlbum

    val animatedImageLoader = remember(context) {
        val global = coil.Coil.imageLoader(context)
        coil.ImageLoader.Builder(context)
            .memoryCache(global.memoryCache)
            .diskCache(global.diskCache)
            .allowHardware(true)
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

    var animatedArtworkUrl by remember(albumState.artist, albumState.title) {
        // Block cached animated artwork for albums that have known wrong cache entries
        val cached = if (isAnimatedArtworkBlocked) null
                     else com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.get(albumState.artist, albumState.title)
        mutableStateOf(cached)
    }

    LaunchedEffect(albumState.artist, albumState.title) {
        val artist = albumState.artist
        val album = albumState.title
        // Block animated artwork for specific albums to prevent wrong cache hits
        if (isAnimatedArtworkBlocked) return@LaunchedEffect
        if (animatedArtworkUrl != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
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
                    val isVertical = album.contains("After Hours", ignoreCase = true) ||
                            album.contains("Around the Fur", ignoreCase = true)
                    val streamUrl = if (isVertical) {
                        obj.optString("url_tall").takeIf { it.isNotBlank() } 
                            ?: obj.optString("url").takeIf { it.isNotBlank() }
                    } else {
                        obj.optString("url").takeIf { it.isNotBlank() } 
                            ?: obj.optString("url_tall").takeIf { it.isNotBlank() }
                    }
                    if (!streamUrl.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            animatedArtworkUrl = streamUrl
                            com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.put(artist, album, streamUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Extract dominant colour
    LaunchedEffect(headerArt) {
        if (!headerArt.isNullOrBlank()) {
            withContext(Dispatchers.Default) {
                val request = ImageRequest.Builder(context)
                    .data(headerArt)
                    .allowHardware(false)
                    .size(150)
                    .memoryCachePolicy(coil.request.CachePolicy.READ_ONLY)
                    .build()
                val result = coil.Coil.imageLoader(context).execute(request)
                if (result is coil.request.SuccessResult) {
                    val drawable = result.drawable
                    val bmp = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        ?: android.graphics.Bitmap.createBitmap(
                            drawable.intrinsicWidth.coerceAtLeast(1),
                            drawable.intrinsicHeight.coerceAtLeast(1),
                            android.graphics.Bitmap.Config.ARGB_8888
                        ).also { b ->
                            val c = android.graphics.Canvas(b)
                            drawable.setBounds(0, 0, c.width, c.height)
                            drawable.draw(c)
                        }
                    try {
                        var r = 0L; var g = 0L; var b = 0L
                        val y = bmp.height - 1
                        val w = bmp.width
                        val step = maxOf(1, w / 16)
                        var count = 0
                        for (x in 0 until w step step) {
                            val pixel = bmp.getPixel(x, y)
                            r += android.graphics.Color.red(pixel)
                            g += android.graphics.Color.green(pixel)
                            b += android.graphics.Color.blue(pixel)
                            count++
                        }
                        val sampledColor = Color((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
                        withContext(Dispatchers.Main) {
                            dominantColor = sampledColor
                            onDominantColorChanged(sampledColor)
                        }
                    } catch (e: Exception) { }
                }
            }
        }
    }

    // Load album/playlist tracks
    LaunchedEffect(albumState.id) {
        withContext(Dispatchers.IO) {
            if (albumState.id.startsWith("offline_album_")) {
                val localDownloads = LibraryManager.getDownloadedSongsForAlbum(albumState.title)
                if (localDownloads.isNotEmpty()) {
                    tracks = localDownloads.map { dl ->
                        com.echo.innertube.models.SongItem(
                            id = dl.id,
                            title = dl.title,
                            artists = listOf(com.echo.innertube.models.Artist(name = dl.subtitle, id = null)),
                            album = com.echo.innertube.models.Album(name = dl.album ?: albumState.title, id = albumState.id),
                            thumbnail = dl.thumbnail ?: albumState.thumbnail ?: "",
                            explicit = false
                        )
                    }
                }
            } else if (albumState.id.startsWith("replay_album_")) {
                val albumTitle = albumState.title
                val history = LibraryManager.getPlaybackHistory()
                val albumSongs = history
                    .filter { it.album != null && it.album.equals(albumTitle, ignoreCase = true) }
                    .groupBy { it.songId }
                    .map { (songId, records) -> Pair(songId, records) }
                    .sortedByDescending { it.second.size }
                    .map { (songId, records) ->
                        val first = records.first()
                        com.echo.innertube.models.SongItem(
                            id = songId,
                            title = first.title,
                            artists = listOf(com.echo.innertube.models.Artist(name = first.artist, id = null)),
                            album = com.echo.innertube.models.Album(name = first.album ?: albumTitle, id = albumState.id),
                            thumbnail = first.thumbnail ?: albumState.thumbnail ?: "",
                            explicit = false
                        )
                    }
                tracks = albumSongs
            } else {
                var loaded = false
                val isAlbum = albumState.id.startsWith("MPREb") || albumState.id.startsWith("FEmusic")
                val errors = mutableListOf<String>()
                if (isAlbum) {
                    YouTube.album(albumState.id).onSuccess { albumPage ->
                        tracks = albumPage.songs
                        loaded = true
                    }.onFailure { err ->
                        errors.add("Album API error: ${err.localizedMessage ?: err.toString()}")
                        val pId = albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")
                        YouTube.playlist(pId).onSuccess { playlistPage ->
                            tracks = playlistPage.songs
                            loaded = true
                        }.onFailure { err2 ->
                            errors.add("Playlist fallback error: ${err2.localizedMessage ?: err2.toString()}")
                        }
                    }
                } else {
                    val pId = albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")
                    YouTube.playlist(pId).onSuccess { playlistPage ->
                        tracks = playlistPage.songs
                        loaded = true
                    }.onFailure { err ->
                        errors.add("Playlist API error: ${err.localizedMessage ?: err.toString()}")
                        YouTube.album(albumState.id).onSuccess { albumPage ->
                            tracks = albumPage.songs
                            loaded = true
                        }.onFailure { err2 ->
                            errors.add("Album fallback error: ${err2.localizedMessage ?: err2.toString()}")
                        }
                    }
                }
                
                // Fallback to downloaded tracks if online fetch failed or returned empty
                if (!loaded || tracks.isEmpty()) {
                    val localDownloads = LibraryManager.getDownloadedSongsForAlbum(albumState.title)
                    if (localDownloads.isNotEmpty()) {
                        tracks = localDownloads.map { dl ->
                            com.echo.innertube.models.SongItem(
                                id = dl.id,
                                title = dl.title,
                                artists = listOf(com.echo.innertube.models.Artist(name = dl.subtitle, id = null)),
                                album = com.echo.innertube.models.Album(name = dl.album ?: albumState.title, id = albumState.id),
                                thumbnail = dl.thumbnail ?: albumState.thumbnail ?: "",
                                explicit = false
                            )
                        }
                        albumError = null
                    } else {
                        if (errors.isNotEmpty()) {
                            albumError = errors.joinToString("\n")
                        }
                    }
                } else {
                    albumError = null
                }
            }
        }
    }

    val localBackdrop = rememberLayerBackdrop()

    val isLightBackground = dominantColor.luminance() > 0.5f
    val contentColor = if (isLightBackground) Color(0xFF1E1E1E) else Color.White

    SharedElementTransitionContainer(
        onBack = onBack,
        shrinkToTarget = false,
        enableSwipeToDismiss = false,
        slideToSide = false,
        staticContainer = true
    ) { progress, dismiss ->
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

        val lastClickBounds = SharedTransitionState.carouselItemBounds[albumState.id] ?: SharedTransitionState.lastClickBounds
        val sourceX = lastClickBounds?.left ?: (screenWidth / 2f - 100f)
        val sourceY = lastClickBounds?.top ?: (screenHeight / 2f - 100f)
        val sourceW = lastClickBounds?.width ?: 200f
        val sourceH = lastClickBounds?.height ?: 200f

        val curX = sourceX + progress * (0f - sourceX)
        val curY = sourceY + progress * (0f - sourceY)
        val curW = sourceW + progress * (screenWidth - sourceW)
        val curH = sourceH + progress * (screenHeight - sourceH)
        val curCorner = 24f * (1f - progress)

        val popScaleBack by animateFloatAsState(
            targetValue = if (progress > 0.80f) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
            label = "popScaleBack"
        )
        val popScaleShare by animateFloatAsState(
            targetValue = if (progress > 0.85f) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
            label = "popScaleShare"
        )
        val popScaleMore by animateFloatAsState(
            targetValue = if (progress > 0.90f) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
            label = "popScaleMore"
        )
        val contentAlpha = ((progress - 0.4f).coerceAtLeast(0f) / 0.6f)

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Static parent screen dimming overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = progress * 0.6f))
            )

            val translationYVal = with(density) { ((1f - progress) * 80f).dp.toPx() }
            com.mrtdk.glass.GlassContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = contentAlpha
                        translationY = translationYVal
                    },
                useShader = true,
                content = {
                    val listState = rememberLazyListState()
                    val firstIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
                    val firstOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }
                    val scrollOffsetPx by remember {
                        derivedStateOf {
                            (firstIndex * 400 + firstOffset).toFloat()
                        }
                    }
                    val blurRadiusDp by remember {
                        derivedStateOf {
                            (scrollOffsetPx / 10f).coerceIn(0f, 32f).dp
                        }
                    }
                    val heroAlpha by remember {
                        derivedStateOf {
                            (1f - (scrollOffsetPx / 1200f)).coerceIn(0.7f, 1f)
                        }
                    }
                    val heroParallaxY by remember {
                        derivedStateOf {
                            -(scrollOffsetPx * 0.22f).coerceAtLeast(0f)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(dominantColor.copy(alpha = contentAlpha))
                    ) {
                        // ── FIXED / PARALLAX HERO ARTWORK BACKDROP ──
                        val heroHeightRatio = albumHeightRatio
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / heroHeightRatio)
                                .graphicsLayer {
                                    translationY = heroParallaxY
                                    alpha = if (progress < 0.99f) 0f else heroAlpha
                                }
                                .then(
                                    if (blurRadiusDp > 0.5.dp && android.os.Build.VERSION.SDK_INT >= 31) {
                                        Modifier.blur(blurRadiusDp)
                                    } else Modifier
                                )
                        ) {
                            // Base sharp album cover
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(headerArt).crossfade(true).build(),
                                imageLoader = animatedImageLoader,
                                contentDescription = albumState.title,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier.fillMaxSize()
                            )

                            var isVideoPlaying by remember(albumState.id) { mutableStateOf(false) }
                            val currentAnimatedUrl = animatedArtworkUrl

                            if (!currentAnimatedUrl.isNullOrBlank()) {
                                com.mrtdk.liquid_glass.ui.components.AnimatedArtworkPlayer(
                                    videoUrl = currentAnimatedUrl,
                                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (isVideoPlaying) 1f else 0f },
                                    isPaused = isPaused,
                                    onPlaybackStarted = { isVideoPlaying = true }
                                )
                            }

                            // Lower gradient overlay at the bottom edge of the image
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color.Transparent,
                                            0.70f to Color.Transparent,
                                            0.92f to dominantColor.copy(alpha = 0.85f),
                                            1.0f to dominantColor.copy(alpha = contentAlpha)
                                        )
                                    )
                            )
                        }

                        // ── SCROLLABLE ALBUM CONTENT ──
                        val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
                        val heroHeightDp = screenWidthDp * heroHeightRatio
                        val spacerHeightDp = (heroHeightDp - 16.dp).coerceAtLeast(0.dp)

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Spacer pushing content down so title begins right at the bottom edge of the image
                            item {
                                Spacer(modifier = Modifier.height(spacerHeightDp))
                            }

                            // ── ALBUM TITLE & METADATA (Scrolls upwards) ──
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 2.dp)
                                        .graphicsLayer { alpha = contentAlpha },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = albumState.title,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 24.sp,
                                        style = TextStyle(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.6f),
                                                offset = Offset(1f, 1f),
                                                blurRadius = 3f
                                            )
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = albumState.artist,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        style = TextStyle(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.6f),
                                                offset = Offset(1f, 1f),
                                                blurRadius = 3f
                                            )
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Bandas sonoras • ${albumState.year ?: 2026} • ",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            painter = painterResource(id = R.drawable.apple_lossless_seeklogo),
                                            contentDescription = "Lossless",
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.height(8.dp).width(14.dp)
                                        )
                                        Text(
                                            text = " Lossless",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // ── ACTION BUTTONS: shuffle | ▶ Play | + ──
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp)
                                        .graphicsLayer { alpha = contentAlpha },
                                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val lightTranslucent = Color.White.copy(alpha = 0.22f)

                                    // Shuffle button
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(lightTranslucent)
                                            .clickable {
                                                if (tracks.isNotEmpty()) {
                                                    val shuffledTracks = tracks.shuffled()
                                                    val s = shuffledTracks.first()
                                                    val albumQueue = shuffledTracks.drop(1).map { t ->
                                                        QueueItem(
                                                            title = t.title,
                                                            artist = t.artists.joinToString { it.name },
                                                            artUrl = songArtUrl,
                                                            videoId = t.id,
                                                            album = albumState.title,
                                                            albumId = albumState.id
                                                        )
                                                    }
                                                    onSongSelected(
                                                        PlayerState(
                                                            title = s.title,
                                                            artist = s.artists.joinToString { it.name },
                                                            artUrl = songArtUrl,
                                                            videoId = s.id,
                                                            queue = albumQueue,
                                                            isExclusiveQueue = true,
                                                            album = albumState.title,
                                                            albumId = albumState.id
                                                        )
                                                    )
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.shuffle),
                                            contentDescription = "Shuffle",
                                            tint = Color.White,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }

                                    // ▶ Play button
                                    val playTextColor = if (dominantColor.luminance() > 0.65f) Color(0xFF1E1E1E) else dominantColor
                                    Box(
                                        modifier = Modifier
                                            .width(155.dp)
                                            .height(46.dp)
                                            .clip(RoundedCornerShape(23.dp))
                                            .background(Color.White)
                                            .clickable {
                                                tracks.firstOrNull()?.let { s ->
                                                    val albumQueue = tracks.drop(1).map { t ->
                                                        QueueItem(
                                                            title = t.title,
                                                            artist = t.artists.joinToString { it.name },
                                                            artUrl = songArtUrl,
                                                            videoId = t.id,
                                                            album = albumState.title,
                                                            albumId = albumState.id
                                                        )
                                                    }
                                                    onSongSelected(
                                                        PlayerState(
                                                            title = s.title,
                                                            artist = s.artists.joinToString { it.name },
                                                            artUrl = songArtUrl,
                                                            videoId = s.id,
                                                            queue = albumQueue,
                                                            isExclusiveQueue = true,
                                                            album = albumState.title,
                                                            albumId = albumState.id
                                                        )
                                                    )
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = playTextColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = "Reproducir",
                                                color = playTextColor,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // + button
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(lightTranslucent)
                                            .clickable { 
                                                if (isSaved) {
                                                    LibraryManager.removeItem(albumState.id)
                                                } else {
                                                    LibraryManager.saveItem(LibraryItem(
                                                        id = albumState.id,
                                                        title = albumState.title,
                                                        subtitle = albumState.artist,
                                                        thumbnail = hdThumb,
                                                        type = ItemType.ALBUM
                                                    ))
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = "Add/Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            // ── EDITORIAL REVIEW / DESCRIPTION ──
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp)
                                        .graphicsLayer { alpha = contentAlpha }
                                ) {
                                    Text(
                                        text = if (isMichaelAlbum) {
                                            "Su leyenda cobra nueva vida en una retrospectiva trepidante."
                                        } else {
                                            "Álbum completo en alta fidelidad y sonido envolvente."
                                        },
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 13.5.sp,
                                        lineHeight = 17.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .background(Color.White.copy(alpha = 0.14f))
                                    )
                                }
                            }

                            // ── TRACK LIST ──
                            items(tracks.size) { i ->
                                val song = tracks[i]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(dominantColor.copy(alpha = contentAlpha))
                                        .graphicsLayer { alpha = contentAlpha }
                                        .clickable {
                                            val albumQueue = tracks.drop(i + 1).map { t ->
                                                QueueItem(
                                                    title = t.title,
                                                    artist = t.artists.joinToString { it.name },
                                                    artUrl = songArtUrl,
                                                    videoId = t.id,
                                                    album = albumState.title,
                                                    albumId = albumState.id
                                                )
                                            }
                                            onSongSelected(
                                                PlayerState(
                                                    title = song.title,
                                                    artist = song.artists.joinToString { it.name },
                                                    artUrl = songArtUrl,
                                                    videoId = song.id,
                                                    queue = albumQueue,
                                                    isExclusiveQueue = true,
                                                    album = albumState.title,
                                                    albumId = albumState.id
                                                )
                                            )
                                        }
                                        .padding(horizontal = 20.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${i + 1}",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            song.title,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (song.artists.isNotEmpty()) {
                                            Text(
                                                song.artists.joinToString { it.name },
                                                color = Color.White.copy(alpha = 0.55f),
                                                fontSize = 12.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val songArtistNames = song.artists.joinToString { it.name }
                                            activeSongForMenu = ContextMenuSong(
                                                id = song.id,
                                                title = song.title,
                                                artist = songArtistNames,
                                                thumbnail = songArtUrl,
                                                album = albumState.title,
                                                artistId = song.artists.firstOrNull()?.id
                                            )
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreHoriz,
                                            null,
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                // Divider
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 44.dp, end = 20.dp)
                                        .height(0.5.dp)
                                        .background(Color.White.copy(alpha = 0.08f))
                                )
                            }

                            // Bottom padding
                            item {
                                Spacer(modifier = Modifier.height(120.dp))
                            }
                        }
                    }
                },
                glassContent = {
                    val scope = this
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bigger circular back button
                        scope.GlassBox(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = popScaleBack
                                    scaleY = popScaleBack
                                    alpha = popScaleBack
                                }
                                .clickable { dismiss() },
                            shape = CircleShape,
                            tint = dominantColor.copy(alpha = 0.35f),
                            blur = 0.8f,
                            centerDistortion = 0.1f,
                            scale = 0.02f,
                            warpEdges = 0.4f,
                            elevation = 4.dp,
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        // Capsule containing Share and More options
                        scope.GlassBox(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = popScaleShare
                                    scaleY = popScaleShare
                                    alpha = popScaleShare
                                }
                                .height(44.dp),
                            shape = RoundedCornerShape(percent = 50),
                            tint = dominantColor.copy(alpha = 0.35f),
                            blur = 0.8f,
                            centerDistortion = 0.1f,
                            scale = 0.02f,
                            warpEdges = 0.4f,
                            elevation = 4.dp,
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val shareUrl = "https://music.youtube.com/playlist?list=${albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")}"
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, albumState.title)
                                            putExtra(android.content.Intent.EXTRA_TEXT, "$shareUrl")
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir"))
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.IosShare,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showAlbumMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    activeSongForMenu?.let { song ->
                        AppleMusicSongMenu(
                            song = song,
                            onDismiss = { activeSongForMenu = null },
                            onGoToArtist = {
                                val aId = song.artistId ?: song.artist
                                onArtistSelected(
                                    com.mrtdk.liquid_glass.ui.screens.ArtistState(
                                        id = aId,
                                        name = song.artist,
                                        thumbnail = null
                                    )
                                )
                            },
                            onGoToAlbum = null,
                            onSongSelected = onSongSelected
                        )
                    }

                    if (showAlbumMenu) {
                        AppleMusicAlbumMenu(
                            album = ContextMenuAlbum(
                                id = albumState.id,
                                playlistId = albumState.playlistId,
                                title = albumState.title,
                                artist = albumState.artist,
                                thumbnail = albumState.thumbnail,
                                year = albumState.year
                            ),
                            onDismiss = { showAlbumMenu = false },
                            tracks = tracks,
                            onAddAlbumToQueue = {
                                if (tracks.isNotEmpty()) {
                                    val current = PlaybackQueue.currentSong
                                    val qItems = tracks.map { t ->
                                        QueueItem(
                                            title = t.title,
                                            artist = t.artists.joinToString { it.name },
                                            artUrl = songArtUrl,
                                            videoId = t.id,
                                            album = albumState.title
                                        )
                                    }
                                    if (current == null) {
                                        val s = tracks.first()
                                        onSongSelected(
                                            PlayerState(
                                                title = s.title,
                                                artist = s.artists.joinToString { it.name },
                                                artUrl = songArtUrl,
                                                videoId = s.id,
                                                queue = qItems.drop(1),
                                                isExclusiveQueue = true,
                                                album = albumState.title
                                            )
                                        )
                                    } else {
                                        PlaybackQueue.queue = PlaybackQueue.queue + qItems
                                        PlaybackQueue.onQueueChanged?.invoke()
                                        Toast.makeText(context, "Álbum añadido a la cola", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onSaveAlbumToLibrary = {
                                LibraryManager.saveItem(
                                    LibraryItem(
                                        id = albumState.id,
                                        title = albumState.title,
                                        subtitle = albumState.artist,
                                        thumbnail = hdThumb,
                                        type = ItemType.ALBUM
                                    )
                                )
                                Toast.makeText(context, "Álbum guardado en la biblioteca", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
            if (progress < 0.99f) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(curX.roundToInt(), curY.roundToInt()) }
                            .size(with(density) { curW.toDp() }, with(density) { curH.toDp() })
                            .clip(RoundedCornerShape(curCorner.dp))
                            .background(dominantColor)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Cover Art at the top of the card
                            val coverHeightRatio = 1f + progress * (albumHeightRatio - 1f)
                            val coverHeight = curW * coverHeightRatio
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(with(density) { coverHeight.toDp() })
                            ) {
                                if (headerArt != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(headerArt).crossfade(false).build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    }
                                }
                                
                                // Gradient fade at the bottom of the cover art
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                0.0f to Color.Transparent,
                                                0.75f to Color.Transparent,
                                                1.0f to dominantColor
                                            )
                                        )
                                )
                            }
                            
                            // Details below the cover art (Title, Artist, Action Buttons)
                            val detailsAlpha = ((progress - 0.1f) / 0.9f).coerceIn(0f, 1f)
                            val detailsTranslationY = with(density) { ((1f - progress) * 20f).dp.toPx() }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .graphicsLayer {
                                        alpha = detailsAlpha
                                        translationY = detailsTranslationY
                                    }
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = albumState.title,
                                    color = contentColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = albumState.artist,
                                    color = contentColor.copy(alpha = 0.8f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Buttons Row (Shuffle, Play, Add)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val darkTranslucent = Color.Black.copy(alpha = 0.35f)
                                    
                                    // Shuffle button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(darkTranslucent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = "Shuffle",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    // Play button
                                    Box(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("Play", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    // Add button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(darkTranslucent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = "Add/Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    val transitionTopBar = @Composable {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .graphicsLayer {
                                        scaleX = popScaleBack
                                        scaleY = popScaleBack
                                        alpha = popScaleBack
                                    }
                                    .clip(CircleShape)
                                    .background(dominantColor.copy(alpha = 0.35f))
                                    .clickable { dismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBackIosNew,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = popScaleShare
                                        scaleY = popScaleShare
                                        alpha = popScaleShare
                                    }
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(dominantColor.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val shareUrl = "https://music.youtube.com/album/${albumState.id}"
                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, albumState.title)
                                                putExtra(android.content.Intent.EXTRA_TEXT, "$shareUrl")
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir"))
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.IosShare,
                                            contentDescription = "Share",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showAlbumMenu = true },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    transitionTopBar()
                }
            }
        }
    }

    if (albumError != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { albumError = null },
            title = {
                Text(
                    text = "Error al cargar canciones",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "No se pudieron cargar las canciones del álbum. Por favor, toma una captura de pantalla de este error para enviársela al desarrollador:",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = albumError ?: "",
                            color = Color.Red,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Error RayMusic", albumError)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Copiado al portapapeles", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copiar", color = Color(0xFFE91E63))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { albumError = null }) {
                    Text("Cerrar", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            textContentColor = Color.White
        )
    }
}


