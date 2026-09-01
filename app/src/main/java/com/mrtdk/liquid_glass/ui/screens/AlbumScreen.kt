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
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import android.content.Intent
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbDownOffAlt
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import kotlinx.coroutines.launch
import android.widget.Toast
import com.mrtdk.liquid_glass.ui.components.AppleMusicSongMenu
import com.mrtdk.liquid_glass.ui.components.AppleMusicAlbumMenu
import com.mrtdk.liquid_glass.ui.components.ContextMenuSong
import com.mrtdk.liquid_glass.ui.components.ContextMenuAlbum
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
    onVideoSelected: ((String) -> Unit)? = null,
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
    var artistPageData by remember(albumState.artist, albumState.id) { mutableStateOf<com.echo.innertube.pages.ArtistPage?>(null) }
    var albumDescription by remember(albumState.id) { mutableStateOf<String?>(null) }
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
        coil.Coil.imageLoader(context)
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
            val cleanArtist = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(artist)
            val cleanAlbum = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(album)

            // 1. Unified Echo-Music Canvas Provider (EchoMusic, ArchiveTune, Tidal, AppleMusic)
            var streamUrl = com.mrtdk.liquid_glass.canvas.UnifiedCanvasProvider.getSongOrAlbumCanvas(cleanAlbum, cleanArtist, cleanAlbum)

            // 2. Fallback to m8tec
            if (streamUrl == null) {
                try {
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
                        streamUrl = if (isVertical) {
                            obj.optString("url_tall").takeIf { it.isNotBlank() } 
                                ?: obj.optString("url").takeIf { it.isNotBlank() }
                        } else {
                            obj.optString("url").takeIf { it.isNotBlank() } 
                                ?: obj.optString("url_tall").takeIf { it.isNotBlank() }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (!streamUrl.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    animatedArtworkUrl = streamUrl
                    com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.put(artist, album, streamUrl)
                }
            }
        }
    }

    // Extract dominant colour
    LaunchedEffect(headerArt, isMichaelAlbum) {
        if (isMichaelAlbum) {
            val michaelColor = Color(0xFFC33826)
            dominantColor = michaelColor
            onDominantColorChanged(michaelColor)
            return@LaunchedEffect
        }
        if (!headerArt.isNullOrBlank()) {
            withContext(Dispatchers.Default) {
                val request = ImageRequest.Builder(context)
                    .data(headerArt)
                    .allowHardware(false)
                    .size(180)
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
                        val palette = androidx.palette.graphics.Palette.from(bmp).maximumColorCount(24).generate()
                        val swatch = palette.dominantSwatch 
                            ?: palette.vibrantSwatch 
                            ?: palette.darkVibrantSwatch 
                            ?: palette.lightVibrantSwatch 
                            ?: palette.mutedSwatch
                        val sampledColor = swatch?.rgb?.let { Color(it) } ?: Color(0xFF1E1E1E)
                        withContext(Dispatchers.Main) {
                            dominantColor = sampledColor
                            onDominantColorChanged(sampledColor)
                        }
                    } catch (e: Exception) { }
                }
            }
        }
    }

    // Load album/playlist tracks & artist info
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
                        albumDescription = albumPage.description
                        loaded = true
                        val artistId = albumPage.album.artists?.firstOrNull()?.id
                        if (!artistId.isNullOrBlank()) {
                            YouTube.artist(artistId).onSuccess { artPage ->
                                artistPageData = artPage
                            }
                        }
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
                            albumDescription = albumPage.description
                            loaded = true
                            val artistId = albumPage.album.artists?.firstOrNull()?.id
                            if (!artistId.isNullOrBlank()) {
                                YouTube.artist(artistId).onSuccess { artPage ->
                                    artistPageData = artPage
                                }
                            }
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

    // Artist Fallback Search if artistPageData is still empty
    LaunchedEffect(albumState.artist) {
        if (artistPageData == null && albumState.artist.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val cleanArtistName = albumState.artist.split(",", "&", ";").firstOrNull()?.trim() ?: albumState.artist
                    val searchRes = YouTube.search(cleanArtistName, YouTube.SearchFilter.FILTER_ARTIST)
                    val foundArtist = searchRes.getOrNull()?.items?.filterIsInstance<com.echo.innertube.models.ArtistItem>()?.firstOrNull()
                    if (foundArtist != null) {
                        YouTube.artist(foundArtist.id).onSuccess { artPage ->
                            artistPageData = artPage
                        }
                    }
} catch (e: Exception) { }
            }
        }
    }

    val localBackdrop = rememberLayerBackdrop()

    val isLightBackground = dominantColor.luminance() > 0.52f
    val primaryTextColor = if (isLightBackground) Color(0xFF151515) else Color.White
    val secondaryTextColor = if (isLightBackground) Color(0xFF151515).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.85f)
    val tertiaryTextColor = if (isLightBackground) Color(0xFF151515).copy(alpha = 0.52f) else Color.White.copy(alpha = 0.60f)
    val dividerColor = if (isLightBackground) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.12f)
    val trackNumberColor = if (isLightBackground) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.50f)
    val circularButtonBg = if (isLightBackground) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.22f)
    val playButtonBg = if (isLightBackground) Color(0xFF151515) else Color.White
    val playButtonTextColor = if (isLightBackground) Color.White else (if (dominantColor.luminance() > 0.65f) Color(0xFF151515) else dominantColor)
    val glassButtonTint = if (isLightBackground) Color.White.copy(alpha = 0.65f) else dominantColor.copy(alpha = 0.35f)
    val glassIconTint = if (isLightBackground) Color(0xFF151515) else Color.White

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
                            (scrollOffsetPx / 6f).coerceIn(0f, 32f).dp
                        }
                    }
                    val heroAlpha by remember {
                        derivedStateOf {
                            (1f - (scrollOffsetPx / 850f)).coerceIn(0.55f, 1f)
                        }
                    }
                    val heroParallaxY by remember {
                        derivedStateOf {
                            -(scrollOffsetPx * 0.42f)
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

                            // Subtle gradient fade only at the very bottom edge of the image
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color.Transparent,
                                            0.78f to Color.Transparent,
                                            0.94f to dominantColor.copy(alpha = 0.6f),
                                            1.0f to dominantColor.copy(alpha = contentAlpha)
                                        )
                                    )
                            )
                        }

                        // ── SCROLLABLE ALBUM CONTENT ──
                        val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
                        val heroHeightDp = screenWidthDp * heroHeightRatio
                        val spacerHeightDp = (heroHeightDp - 22.dp).coerceAtLeast(0.dp)

                        val videoSection = remember(artistPageData) {
                            artistPageData?.sections?.firstOrNull { 
                                it.title.contains("Video", ignoreCase = true) || it.title.contains("Vídeo", ignoreCase = true) 
                            }
                        }
                        val otherAlbumsSection = remember(artistPageData, albumState.title) {
                            val section = artistPageData?.sections?.firstOrNull { 
                                it.title.contains("Álbum", ignoreCase = true) || it.title.contains("Album", ignoreCase = true) || it.title.contains("Discograf", ignoreCase = true)
                            }
                            section?.copy(items = section.items.filter { 
                                it is com.echo.innertube.models.AlbumItem && !it.title.equals(albumState.title, ignoreCase = true) 
                            })
                        }
                        val singlesSection = remember(artistPageData) {
                            artistPageData?.sections?.firstOrNull { 
                                it.title.contains("Sencillo", ignoreCase = true) || it.title.contains("Single", ignoreCase = true) || it.title.contains("EP", ignoreCase = true)
                            }
                        }
                        val appearsOnSection = remember(artistPageData) {
                            artistPageData?.sections?.firstOrNull { 
                                it.title.contains("Aparece", ignoreCase = true) || it.title.contains("Featured", ignoreCase = true) || it.title.contains("Playlist", ignoreCase = true) || it.title.contains("Lista", ignoreCase = true) || it.title.contains("Destacado", ignoreCase = true)
                            }
                        }

                        val editorialText = remember(albumDescription, albumState.artist, albumState.title, isMichaelAlbum) {
                            if (!albumDescription.isNullOrBlank()) {
                                albumDescription!!
                            } else if (isMichaelAlbum) {
                                "Su leyenda cobra nueva vida en una retrospectiva trepidante."
                            } else {
                                "Álbum completo en alta fidelidad y sonido envolvente."
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Spacer pushing content down so title begins right at the bottom edge of the image
                            item {
                                Spacer(modifier = Modifier.height(spacerHeightDp))
                            }

                            // Smooth gradient transition coat into solid dominantColor
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                0.0f to Color.Transparent,
                                                1.0f to dominantColor.copy(alpha = contentAlpha)
                                            )
                                        )
                                )
                            }

                            // ── ALBUM TITLE & METADATA ──
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(dominantColor.copy(alpha = contentAlpha))
                                        .padding(horizontal = 20.dp, vertical = 2.dp)
                                        .graphicsLayer { alpha = contentAlpha },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = albumState.title,
                                        color = primaryTextColor,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 24.sp,
                                        style = TextStyle(
                                            shadow = if (isLightBackground) null else Shadow(
                                                color = Color.Black.copy(alpha = 0.6f),
                                                offset = Offset(1f, 1f),
                                                blurRadius = 3f
                                            )
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = albumState.artist,
                                        color = secondaryTextColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        style = TextStyle(
                                            shadow = if (isLightBackground) null else Shadow(
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
                                            color = tertiaryTextColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            painter = painterResource(id = R.drawable.apple_lossless_seeklogo),
                                            contentDescription = "Lossless",
                                            tint = tertiaryTextColor,
                                            modifier = Modifier.height(8.dp).width(14.dp)
                                        )
                                        Text(
                                            text = " Lossless",
                                            color = tertiaryTextColor,
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
                                        .background(dominantColor.copy(alpha = contentAlpha))
                                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp)
                                        .graphicsLayer { alpha = contentAlpha },
                                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Shuffle button
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(circularButtonBg)
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
                                            tint = primaryTextColor,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }

                                    // ▶ Play button
                                    Box(
                                        modifier = Modifier
                                            .width(155.dp)
                                            .height(46.dp)
                                            .clip(RoundedCornerShape(23.dp))
                                            .background(playButtonBg)
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
                                                tint = playButtonTextColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = "Reproducir",
                                                color = playButtonTextColor,
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
                                            .background(circularButtonBg)
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
                                            tint = primaryTextColor,
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
                                        .background(dominantColor.copy(alpha = contentAlpha))
                                        .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp)
                                        .graphicsLayer { alpha = contentAlpha }
                                ) {
                                    Text(
                                        text = editorialText,
                                        color = secondaryTextColor,
                                        fontSize = 13.5.sp,
                                        lineHeight = 17.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .background(dividerColor)
                                    )
                                }
                            }

                            // ── TRACK LIST ──
                            items(
                                count = tracks.size,
                                key = { i -> tracks[i].id.ifEmpty { "$i" } },
                                contentType = { "album_track" }
                            ) { i ->
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
                                        color = trackNumberColor,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            song.title,
                                            color = primaryTextColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (song.artists.isNotEmpty()) {
                                            Text(
                                                song.artists.joinToString { it.name },
                                                color = tertiaryTextColor,
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
                                            tint = tertiaryTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                // Divider
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(dominantColor.copy(alpha = contentAlpha))
                                        .padding(start = 44.dp, end = 20.dp)
                                        .height(0.5.dp)
                                        .background(dividerColor)
                                )
                            }

                            // ── ALBUM FOOTER / RAY DIGITAL MASTER & CREDITS ──
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(dominantColor.copy(alpha = contentAlpha))
                                        .padding(horizontal = 20.dp, vertical = 16.dp)
                                        .graphicsLayer { alpha = contentAlpha }
                                ) {
                                    // Ray Digital Master badge row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.apple_lossless_seeklogo),
                                            contentDescription = "Ray Digital Master",
                                            tint = primaryTextColor.copy(alpha = 0.9f),
                                            modifier = Modifier.height(13.dp).width(19.dp)
                                        )
                                        Text(
                                            text = "Ray Digital Master",
                                            color = primaryTextColor.copy(alpha = 0.9f),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Release Date
                                    val releaseYear = albumState.year ?: 2026
                                    val releaseDateString = if (isMichaelAlbum) "24 de abril de 2026" else "$releaseYear"
                                    Text(
                                        text = releaseDateString,
                                        color = secondaryTextColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Song Count & Total Minutes
                                    val totalSecs = tracks.sumOf { it.duration ?: 0 }
                                    val totalMins = if (totalSecs > 0) totalSecs / 60 else (tracks.size * 4).coerceAtLeast(1)
                                    Text(
                                        text = "${tracks.size} canciones, $totalMins minutos",
                                        color = secondaryTextColor.copy(alpha = 0.85f),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Normal
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Copyright line
                                    val copyrightNotice = if (isMichaelAlbum || albumState.artist.contains("Michael Jackson", ignoreCase = true)) {
                                        "℗ This compilation (P) $releaseYear MJJP Records, LLC / Distributed by Sony Music Entertainment"
                                    } else {
                                        "℗ $releaseYear ${albumState.artist} / Distributed by Ray Music Entertainment"
                                    }
                                    Text(
                                        text = copyrightNotice,
                                        color = tertiaryTextColor,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // ── ARTIST SECTION 1: VIDEOS MUSICALES ──
                            if (videoSection != null && videoSection.items.isNotEmpty()) {
                                val videoItems = videoSection.items.filterIsInstance<SongItem>()
                                if (videoItems.isNotEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(dominantColor.copy(alpha = contentAlpha))
                                                .graphicsLayer { alpha = contentAlpha }
                                        ) {
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Text(
                                                text = "Vídeos musicales",
                                                color = primaryTextColor,
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                            )
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 20.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(videoItems.size) { idx ->
                                                    val v = videoItems[idx]
                                                    val vThumb = v.thumbnail.replace("=w226-h226", "=w800-h800").replace("=w120-h120", "=w800-h800")
                                                    Column(
                                                        modifier = Modifier
                                                            .width(310.dp)
                                                            .clickable {
                                                                if (onVideoSelected != null) {
                                                                    onVideoSelected(v.id)
                                                                } else {
                                                                    onSongSelected(PlayerState(title = v.title, artist = v.artists.joinToString { it.name }, artUrl = vThumb, videoId = v.id))
                                                                }
                                                            }
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(175.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(Color.DarkGray)
                                                        ) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(context).data(vThumb).crossfade(true).build(),
                                                                contentDescription = v.title,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = v.title,
                                                            color = primaryTextColor,
                                                            fontSize = 14.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = v.artists.joinToString { it.name },
                                                            color = tertiaryTextColor,
                                                            fontSize = 12.5.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ── ARTIST SECTION 2: MÁS DE [ARTISTA] ──
                            val albumsList = otherAlbumsSection?.items?.filterIsInstance<com.echo.innertube.models.AlbumItem>().orEmpty()
                            if (albumsList.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(dominantColor.copy(alpha = contentAlpha))
                                            .graphicsLayer { alpha = contentAlpha }
                                    ) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Más de ${albumState.artist}",
                                                color = primaryTextColor,
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = tertiaryTextColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            items(albumsList.size) { idx ->
                                                val a = albumsList[idx]
                                                val aThumb = a.thumbnail.replace("=w226-h226", "=w600-h600").replace("=w120-h120", "=w600-h600")
                                                Column(
                                                    modifier = Modifier
                                                        .width(155.dp)
                                                        .clickable {
                                                            onAlbumSelected(
                                                                AlbumState(
                                                                    id = a.browseId,
                                                                    playlistId = a.playlistId,
                                                                    title = a.title,
                                                                    artist = a.artists?.joinToString { it.name } ?: albumState.artist,
                                                                    thumbnail = aThumb,
                                                                    year = a.year
                                                                )
                                                            )
                                                        }
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(155.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(Color.DarkGray)
                                                    ) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(context).data(aThumb).crossfade(true).build(),
                                                            contentDescription = a.title,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = a.title,
                                                        color = primaryTextColor,
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${a.year ?: ""}",
                                                        color = tertiaryTextColor,
                                                        fontSize = 12.5.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ── ARTIST SECTION 3: APARECE EN ──
                            val appearsList = appearsOnSection?.items.orEmpty()
                            if (appearsList.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(dominantColor.copy(alpha = contentAlpha))
                                            .graphicsLayer { alpha = contentAlpha }
                                    ) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Aparece en",
                                                color = primaryTextColor,
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = tertiaryTextColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            items(appearsList.size) { idx ->
                                                val item = appearsList[idx]
                                                val title = when (item) {
                                                    is com.echo.innertube.models.PlaylistItem -> item.title
                                                    is com.echo.innertube.models.AlbumItem -> item.title
                                                    else -> ""
                                                }
                                                val rawThumb = when (item) {
                                                    is com.echo.innertube.models.PlaylistItem -> item.thumbnail
                                                    is com.echo.innertube.models.AlbumItem -> item.thumbnail
                                                    else -> ""
                                                }
                                                val thumb = rawThumb?.replace("=w226-h226", "=w600-h600")?.replace("=w120-h120", "=w600-h600")
                                                Column(
                                                    modifier = Modifier
                                                        .width(155.dp)
                                                        .clickable {
                                                            when (item) {
                                                                is com.echo.innertube.models.AlbumItem -> {
                                                                    onAlbumSelected(
                                                                        AlbumState(
                                                                            id = item.browseId,
                                                                            playlistId = item.playlistId,
                                                                            title = item.title,
                                                                            artist = item.artists?.joinToString { it.name } ?: albumState.artist,
                                                                            thumbnail = thumb,
                                                                            year = item.year
                                                                        )
                                                                    )
                                                                }
                                                                is com.echo.innertube.models.PlaylistItem -> {
                                                                    onAlbumSelected(
                                                                        AlbumState(
                                                                            id = item.id,
                                                                            playlistId = item.id,
                                                                            title = item.title,
                                                                            artist = item.author?.name ?: "Playlist",
                                                                            thumbnail = thumb
                                                                        )
                                                                    )
                                                                }
                                                                else -> {}
                                                            }
                                                        }
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(155.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(Color.DarkGray)
                                                    ) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(context).data(thumb).crossfade(true).build(),
                                                            contentDescription = title,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = title,
                                                        color = primaryTextColor,
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Bottom padding
                            item {
                                Spacer(modifier = Modifier.height(130.dp))
                            }
                        }
                    }
                },
                glassContent = {
                    val scope = this

                    // Semi-transparent overlay to dismiss morphing menu when clicking outside
                    if (showAlbumMenu) {
                        androidx.activity.compose.BackHandler {
                            showAlbumMenu = false
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) { showAlbumMenu = false }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        // Bigger circular back button
                        scope.GlassBox(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = popScaleBack
                                    scaleY = popScaleBack
                                    alpha = if (showAlbumMenu) popScaleBack * 0.4f else popScaleBack
                                }
                                .clickable(enabled = !showAlbumMenu) { dismiss() },
                            shape = CircleShape,
                            tint = glassButtonTint,
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
                                tint = glassIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Morphing Liquid Glass Pill -> Menu on the Top-Right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .graphicsLayer {
                                    scaleX = popScaleShare
                                    scaleY = popScaleShare
                                    alpha = popScaleShare
                                }
                        ) {
                            AlbumTopRightMorphingPill(
                                glassScope = scope,
                                albumState = albumState,
                                isExpanded = showAlbumMenu,
                                onExpandChange = { showAlbumMenu = it },
                                glassButtonTint = glassButtonTint,
                                glassIconTint = glassIconTint,
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
                                },
                                onGoToArtist = {
                                    val aId = artistPageData?.artist?.id ?: albumState.artist
                                    onArtistSelected(
                                        com.mrtdk.liquid_glass.ui.screens.ArtistState(
                                            id = aId,
                                            name = albumState.artist,
                                            thumbnail = null
                                        )
                                    )
                                }
                            )
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
                                    color = primaryTextColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = albumState.artist,
                                    color = secondaryTextColor,
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
                                    // Shuffle button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(circularButtonBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = "Shuffle",
                                            tint = primaryTextColor,
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
                                            .background(playButtonBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = playButtonTextColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("Play", color = playButtonTextColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    // Add button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(circularButtonBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = "Add/Remove",
                                            tint = primaryTextColor,
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
                                    .background(glassButtonTint)
                                    .clickable { dismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBackIosNew,
                                    contentDescription = "Back",
                                    tint = glassIconTint,
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
                                    .background(glassButtonTint),
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
                                            tint = glassIconTint,
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
                                            tint = glassIconTint,
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

private fun getAlbumEditorialDescription(artistName: String, albumTitle: String): String {
    val lowerArtist = artistName.lowercase()
    val lowerAlbum = albumTitle.lowercase()
    return when {
        lowerArtist.contains("michael jackson") -> {
            when {
                lowerAlbum.contains("bad") -> "La continuación del álbum más épico del pop deslumbra con una potencia rítmica inigualable."
                lowerAlbum.contains("thriller") -> "Una obra maestra legendaria que redefinió el alcance global de la música pop."
                lowerAlbum.contains("off the wall") -> "El Rey del Pop se eleva en una brillante exhibición de funk, disco y soul."
                lowerAlbum.contains("dangerous") -> "Una producción vanguardista cargada de new jack swing y pasión artística."
                else -> "Una colección esencial que celebra el legado eterno y el genio musical de Michael Jackson."
            }
        }
        lowerArtist.contains("the weeknd") -> "Un viaje sonoro inmersivo repleto de sintetizadores ochenteros y producción cinematográfica."
        lowerArtist.contains("deftones") -> "Una explosión visceral de rock alternativo con melodías densas y guitarras envolventes."
        lowerArtist.contains("daft punk") -> "Un hito de la música electrónica con grooves futuristas y producción revolucionaria."
        else -> "Álbum completo de ${artistName} en sonido envolvente de alta fidelidad."
    }
}

@Composable
fun AlbumTopRightMorphingPill(
    glassScope: com.mrtdk.glass.GlassBoxScope,
    albumState: AlbumState,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    glassButtonTint: Color,
    glassIconTint: Color,
    tracks: List<SongItem>,
    onAddAlbumToQueue: () -> Unit,
    onSaveAlbumToLibrary: () -> Unit,
    onGoToArtist: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPlaylistsScreen by remember { mutableStateOf(false) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    // Reset internal screen if menu closes
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            isPlaylistsScreen = false
            showNewPlaylistDialog = false
        }
    }

    val savedItems by LibraryManager.savedItems.collectAsState()
    val isSaved = savedItems.any { it.id == albumState.id }
    var isFavorite by remember(albumState.id) { mutableStateOf(false) }

    val morphProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "albumPillToMenuMorph"
    )

    // Smooth continuous 2D size & corner interpolation
    val targetMenuHeight = if (isPlaylistsScreen) 370.dp else 390.dp
    val morphWidth = androidx.compose.ui.unit.lerp(86.dp, 268.dp, morphProgress)
    val morphHeight = androidx.compose.ui.unit.lerp(44.dp, targetMenuHeight, morphProgress)
    val morphCorner = androidx.compose.ui.unit.lerp(22.dp, 24.dp, morphProgress)
    val morphTint = androidx.compose.ui.graphics.lerp(glassButtonTint, Color(0xFF1B1B1E).copy(alpha = 0.88f), morphProgress)

    // Smooth crossfade opacities
    val pillIconsAlpha = ((0.28f - morphProgress) / 0.28f).coerceIn(0f, 1f)
    val menuContentAlpha = ((morphProgress - 0.22f) / 0.78f).coerceIn(0f, 1f)

    glassScope.GlassBox(
        modifier = Modifier
            .size(width = morphWidth, height = morphHeight)
            .clip(RoundedCornerShape(morphCorner))
            .then(
                if (morphProgress > 0.05f) {
                    Modifier.border(
                        width = 0.8.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = morphProgress * 0.35f),
                                Color.White.copy(alpha = morphProgress * 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(morphCorner)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(morphCorner),
        tint = morphTint,
        blur = 0.85f,
        centerDistortion = 0.1f,
        scale = 0.02f,
        warpEdges = 0.4f,
        elevation = if (isExpanded) 16.dp else 4.dp,
        contentAlignment = Alignment.TopEnd
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Collapsed: Original Share + 3 Dots Capsule
            if (pillIconsAlpha > 0.001f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .height(44.dp)
                        .padding(horizontal = 6.dp)
                        .graphicsLayer { alpha = pillIconsAlpha },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val shareUrl = "https://music.youtube.com/playlist?list=${albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")}"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, albumState.title)
                                putExtra(Intent.EXTRA_TEXT, shareUrl)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = "Share",
                            tint = glassIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onExpandChange(true) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = glassIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. Expanded: Apple Music Liquid Glass Menu
            if (menuContentAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = menuContentAlpha }
                ) {
                    if (!isPlaylistsScreen) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp)
                        ) {
                            // ── Top 3 Actions Row (Agregar, Favorito, Compartir) ──
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            // 1. Agregar
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isSaved) {
                                            LibraryManager.removeItem(albumState.id)
                                            Toast.makeText(context, "Eliminado de la biblioteca", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onSaveAlbumToLibrary()
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = if (isSaved) Color(0xFFFA243C) else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isSaved) "Agregado" else "Agregar",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }

                            // 2. Favorito
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        isFavorite = !isFavorite
                                        if (isFavorite) {
                                            onSaveAlbumToLibrary()
                                            Toast.makeText(context, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) Color(0xFFFA243C) else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isFavorite) "En Favoritos" else "Favorito",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }

                            // 3. Compartir
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val pId = albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")
                                        val shareUrl = "https://music.youtube.com/playlist?list=$pId"
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, albumState.title)
                                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Compartir"))
                                        onExpandChange(false)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.IosShare,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Compartir",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.12f), thickness = 0.6.dp)

                        // ── Vertical Action Options ──
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // 1. Agregar a playlist
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isPlaylistsScreen = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Agregar a playlist",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                            // 2. Poner a continuación
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddAlbumToQueue()
                                        onExpandChange(false)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueuePlayNext,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Poner a continuación",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                            // 3. Poner después
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddAlbumToQueue()
                                        onExpandChange(false)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Queue,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Poner después",
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = albumState.title,
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                            // 4. Descargar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onExpandChange(false)
                                        Toast.makeText(context, "Obteniendo canciones para descargar...", Toast.LENGTH_SHORT).show()
                                        coroutineScope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    val tracksToDownload = if (tracks.isNotEmpty()) {
                                                        tracks
                                                    } else {
                                                        val isAlbum = albumState.id.startsWith("MPREb") || albumState.id.startsWith("FEmusic")
                                                        if (isAlbum) {
                                                            YouTube.album(albumState.id).getOrNull()?.songs
                                                                ?: run {
                                                                    val pId = albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")
                                                                    YouTube.playlist(pId).getOrNull()?.songs
                                                                }
                                                        } else {
                                                            val pId = albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")
                                                            YouTube.playlist(pId).getOrNull()?.songs
                                                                ?: run {
                                                                    YouTube.album(albumState.id).getOrNull()?.songs
                                                                }
                                                        }
                                                    }

                                                    withContext(Dispatchers.Main) {
                                                        if (tracksToDownload.isNullOrEmpty()) {
                                                            Toast.makeText(context, "No se encontraron pistas para descargar", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Descargando ${tracksToDownload.size} canciones del álbum...", Toast.LENGTH_SHORT).show()
                                                            tracksToDownload.forEach { track ->
                                                                downloadSong(
                                                                    context = context,
                                                                    videoId = track.id,
                                                                    title = track.title,
                                                                    artist = track.artists.joinToString { it.name },
                                                                    artUrl = track.thumbnail,
                                                                    album = albumState.title,
                                                                    silent = true
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error al descargar: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Descargar",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                            // 5. Ver artista
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onGoToArtist()
                                        onExpandChange(false)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Ver artista",
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = albumState.artist,
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                            // 6. Sugerir menos
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Toast.makeText(context, "Se sugerirá menos contenido similar", Toast.LENGTH_SHORT).show()
                                        onExpandChange(false)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbDownOffAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Sugerir menos",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                } else {
                    // ── Sub-pantalla: Agregar a Playlist ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isPlaylistsScreen = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBackIosNew,
                                    contentDescription = "Volver",
                                    tint = Color(0xFFFA243C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Agregar a playlist",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))

                        val playlists by LibraryManager.playlists.collectAsState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Crear nueva playlist
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showNewPlaylistDialog = true }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFFFA243C),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Nueva playlist...",
                                    color = Color(0xFFFA243C),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f))

                            playlists.forEach { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                val tracksToAdd = if (tracks.isNotEmpty()) {
                                                    tracks
                                                } else {
                                                    val isAlbum = albumState.id.startsWith("MPREb") || albumState.id.startsWith("FEmusic")
                                                    withContext(Dispatchers.IO) {
                                                        if (isAlbum) YouTube.album(albumState.id).getOrNull()?.songs
                                                        else YouTube.playlist(albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")).getOrNull()?.songs
                                                    }
                                                }
                                                if (!tracksToAdd.isNullOrEmpty()) {
                                                    tracksToAdd.forEach { track ->
                                                        val trackLibItem = LibraryItem(
                                                            id = track.id,
                                                            title = track.title,
                                                            subtitle = track.artists.joinToString { it.name },
                                                            thumbnail = track.thumbnail,
                                                            type = ItemType.SONG,
                                                            album = albumState.title
                                                        )
                                                        LibraryManager.addSongToPlaylist(playlist.id, trackLibItem)
                                                    }
                                                    Toast.makeText(context, "Se agregaron las canciones a ${playlist.name}", Toast.LENGTH_SHORT).show()
                                                }
                                                onExpandChange(false)
                                            }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = playlist.name,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${playlist.items.size} canciones",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Divider(color = Color.White.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Diálogo para crear nueva playlist
    if (showNewPlaylistDialog) {
        Dialog(onDismissRequest = { showNewPlaylistDialog = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1E22))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Nueva Playlist",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        placeholder = { Text("Nombre de la playlist", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFA243C),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showNewPlaylistDialog = false }) {
                            Text("Cancelar", color = Color.Gray)
                        }
                        TextButton(onClick = {
                            if (playlistName.isNotBlank()) {
                                LibraryManager.createPlaylist(playlistName)
                                coroutineScope.launch {
                                    val newPlaylist = LibraryManager.playlists.value.firstOrNull { it.name == playlistName }
                                    val tracksToAdd = if (tracks.isNotEmpty()) {
                                        tracks
                                    } else {
                                        val isAlbum = albumState.id.startsWith("MPREb") || albumState.id.startsWith("FEmusic")
                                        withContext(Dispatchers.IO) {
                                            if (isAlbum) YouTube.album(albumState.id).getOrNull()?.songs
                                            else YouTube.playlist(albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")).getOrNull()?.songs
                                        }
                                    }
                                    if (!tracksToAdd.isNullOrEmpty() && newPlaylist != null) {
                                        tracksToAdd.forEach { track ->
                                            val trackLibItem = LibraryItem(
                                                id = track.id,
                                                title = track.title,
                                                subtitle = track.artists.joinToString { it.name },
                                                thumbnail = track.thumbnail,
                                                type = ItemType.SONG,
                                                album = albumState.title
                                            )
                                            LibraryManager.addSongToPlaylist(newPlaylist.id, trackLibItem)
                                        }
                                    }
                                    Toast.makeText(context, "Playlist creada con las canciones del álbum", Toast.LENGTH_SHORT).show()
                                    showNewPlaylistDialog = false
                                    onExpandChange(false)
                                }
                            }
                        }) {
                            Text("Crear", color = Color(0xFFFA243C), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

