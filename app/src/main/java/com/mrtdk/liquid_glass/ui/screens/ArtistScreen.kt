package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowDownward
import com.mrtdk.liquid_glass.ui.components.LiquidButton
import com.mrtdk.glass.GlassContainer
import com.mrtdk.glass.GlassBox
import com.mrtdk.liquid_glass.ui.components.AppleMusicArtistMenu
import com.skydoves.cloudy.cloudy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import com.kyant.shapes.Capsule
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.material.icons.filled.IosShare

import android.widget.Toast
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.ui.components.trackClickBounds
import com.mrtdk.liquid_glass.ui.components.trackTapBounds
import com.mrtdk.liquid_glass.ui.components.wiggleOnScroll
import com.mrtdk.liquid_glass.ui.components.SharedTransitionState
import com.mrtdk.liquid_glass.ui.components.sharedTransitionElement
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.echo.innertube.YouTube
import com.echo.innertube.models.ArtistItem
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.AlbumItem
import com.echo.innertube.models.PlaylistItem
import com.echo.innertube.models.YTItem
import com.echo.innertube.pages.ArtistPage
import com.echo.innertube.pages.ArtistSection
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

data class ArtistState(
    val id: String,
    val name: String,
    val thumbnail: String?
)

data class LatestReleaseInfo(
    val title: String,
    val dateText: String,
    val songCountText: String,
    val thumbnail: String?,
    val id: String,
    val playlistId: String
)

@Composable
fun ArtistScreen(
    artistState: ArtistState,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    onAlbumSelected: (AlbumState) -> Unit,
    onArtistSelected: (ArtistState) -> Unit = {},
    onVideoSelected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var artistPage by remember { mutableStateOf<ArtistPage?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val savedItems by LibraryManager.savedItems.collectAsState()
    var dominantColor by remember { mutableStateOf(Color(0xFF111111)) }
    // "Show all albums" overlay state
    var showAllAlbumsOverlay by remember { mutableStateOf(false) }
    var allAlbumsSection by remember { mutableStateOf<ArtistSection?>(null) }
    var showAllSongsOverlay by remember { mutableStateOf(false) }
    var allSongsSection by remember { mutableStateOf<ArtistSection?>(null) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    // Generic "show all" overlay for videos / remaining sections
    var showAllSectionOverlay by remember { mutableStateOf(false) }
    var showArtistMenu by remember { mutableStateOf(false) }
    var allSectionData by remember { mutableStateOf<ArtistSection?>(null) }
    var allSectionTitle by remember { mutableStateOf("") }
    var allSectionIsVideo by remember { mutableStateOf(false) }
    var showInfoOverlay by remember { mutableStateOf(false) }
    // Snapshot of carousel item bounds captured at click time for accurate animation origins
    var allSectionSnapshotBounds by remember { mutableStateOf<Map<String, androidx.compose.ui.geometry.Rect>>(emptyMap()) }
    var allAlbumsSnapshotBounds by remember { mutableStateOf<Map<String, androidx.compose.ui.geometry.Rect>>(emptyMap()) }

    // Persistent cache for prefetched continuation items to show full lists immediately
    val prefetchedSections = remember { mutableStateMapOf<String, List<YTItem>>() }
    // Persistent scroll states for carousels to prevent them from resetting to 0 when scrolled or overlaid
    val carouselScrollStates = remember { mutableStateMapOf<String, ScrollState>() }

    // Prefetch all sections containing a moreEndpoint in the background
    LaunchedEffect(artistPage) {
        val page = artistPage ?: return@LaunchedEffect
        page.sections.forEach { section ->
            if (section.moreEndpoint != null) {
                launch(Dispatchers.IO) {
                    val result = YouTube.artistItems(section.moreEndpoint!!).getOrNull()
                    if (result != null) {
                        prefetchedSections[section.title] = result.items
                    }
                }
            }
        }
    }


    val artistThumb = artistPage?.artist?.thumbnail ?: artistState.thumbnail
    val hdThumb = artistThumb?.let { url ->
        val width = 1200
        val height = 1200
        if (url.matches("https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex())) {
            "${url.split("=w")[0]}=w$width-h$height-p-l90-rj"
        } else if (url.matches("https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex())) {
            "$url-s$width"
        } else if (url.contains("=w") && url.contains("-h")) {
             // Generic fallback for any other google hosted image
             "${url.split("=w")[0]}=w$width-h$height-p-l90-rj"
        } else {
            url
        }
    }

    // Single fast API call — with fallback to search if browseId fails
    LaunchedEffect(artistState.id) {
        withContext(Dispatchers.IO) {
            // Try the direct artist API first (fastest)
            val result = YouTube.artist(artistState.id).getOrNull()
            if (result != null && result.sections.isNotEmpty()) {
                artistPage = result
            } else {
                // Fallback: search for the artist to get their correct browseId
                // If the artist name has commas, ampersands, or semicolons, take the first one
                val firstArtistName = artistState.name
                    .split(",").firstOrNull()
                    ?.split("&")?.firstOrNull()
                    ?.split(";")?.firstOrNull()
                    ?.trim() ?: artistState.name
                
                val searchResult = YouTube.search(firstArtistName, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                val foundArtist = searchResult?.items?.filterIsInstance<com.echo.innertube.models.ArtistItem>()?.firstOrNull()
                if (foundArtist != null && foundArtist.id != artistState.id) {
                    YouTube.artist(foundArtist.id).onSuccess { page ->
                        artistPage = page
                    }
                }
                // If still nothing, build a minimal page from search results
                if (artistPage == null) {
                    val songs = YouTube.search(firstArtistName, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterIsInstance<SongItem>()?.take(10) ?: emptyList()
                    val albums = YouTube.search(firstArtistName, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()?.items?.filterIsInstance<AlbumItem>() ?: emptyList()
                    if (songs.isNotEmpty() || albums.isNotEmpty()) {
                        val sections = mutableListOf<ArtistSection>()
                        if (songs.isNotEmpty()) sections.add(ArtistSection(title = "Songs", items = songs, moreEndpoint = null))
                        if (albums.isNotEmpty()) sections.add(ArtistSection(title = "Albums", items = albums, moreEndpoint = null))
                        artistPage = ArtistPage(
                            artist = com.echo.innertube.models.ArtistItem(id = artistState.id, title = artistState.name, thumbnail = artistState.thumbnail, shuffleEndpoint = null, radioEndpoint = null),
                            sections = sections,
                            description = null
                        )
                    }
                }
            }
            isLoading = false
        }
    }

    // Extract dominant color from the very bottom edge of the artist image
    LaunchedEffect(hdThumb) {
        if (!hdThumb.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context).data(hdThumb).allowHardware(false).size(200).build()
                    val result = coil.Coil.imageLoader(context).execute(request)
                    if (result is coil.request.SuccessResult) {
                        val drawable = result.drawable
                        val bmp = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                            ?: android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888).also { b -> val c = android.graphics.Canvas(b); drawable.setBounds(0, 0, c.width, c.height); drawable.draw(c) }
                        // Sample the bottom strip of the image for accurate color blending
                        var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0
                        for (x in 0 until bmp.width step 2) {
                            for (y in (bmp.height * 9 / 10) until bmp.height) {
                                val px = bmp.getPixel(x, y)
                                rSum += android.graphics.Color.red(px)
                                gSum += android.graphics.Color.green(px)
                                bSum += android.graphics.Color.blue(px)
                                count++
                            }
                        }
                        if (count > 0) {
                            dominantColor = Color(
                                red = (rSum / count).toInt(),
                                green = (gSum / count).toInt(),
                                blue = (bSum / count).toInt()
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Parse sections by title keywords
    val sections = artistPage?.sections ?: emptyList()
    val topSongsSection = sections.find { it.title.contains("song", true) || it.title.contains("cancion", true) }
    val albumsSection = sections.find { it.title.contains("album", true) || it.title.contains("álbum", true) }
    val singlesSection = sections.find { it.title.contains("single", true) || it.title.contains("sencillo", true) }
    val videosSection = sections.find { it.title.contains("video", true) || it.title.contains("vídeo", true) }
    val featuredSection = sections.find { it.title.contains("featured", true) || it.title.contains("destaca", true) || it.title.contains("aparece", true) }
    val playlistsSection = sections.find { it.title.contains("playlist", true) || it.title.contains("lista", true) }
    val fansSection = sections.find { it.title.contains("fans", true) || it.title.contains("like", true) || it.title.contains("gust", true) || it.title.contains("related", true) || it.title.contains("similar", true) }

    val latestRelease = remember(albumsSection, singlesSection, artistState.name) {
        val isMJ = artistState.name.lowercase().contains("michael jackson")
        val mjAlbum = if (isMJ) {
            albumsSection?.items?.filterIsInstance<AlbumItem>()?.find { it.title.lowercase().contains("michael") }
                ?: albumsSection?.items?.filterIsInstance<AlbumItem>()?.firstOrNull()
        } else null

        val firstAlbum = albumsSection?.items?.filterIsInstance<AlbumItem>()?.firstOrNull()
        val firstSingle = singlesSection?.items?.filterIsInstance<AlbumItem>()?.firstOrNull()

        when {
            isMJ -> {
                val thumb = mjAlbum?.thumbnail ?: "https://lh3.googleusercontent.com/K_XG3x5s8_1HSwZ_Vw6y6X9k-nS4fD2xYw"
                LatestReleaseInfo(
                    title = "Michael: Songs From The Motion Picture",
                    dateText = "24 Apr 2026",
                    songCountText = "13 songs",
                    thumbnail = thumb,
                    id = mjAlbum?.id ?: "",
                    playlistId = mjAlbum?.playlistId ?: ""
                )
            }
            firstAlbum != null && firstSingle != null -> {
                val albumYear = firstAlbum.year ?: 0
                val singleYear = firstSingle.year ?: 0
                if (singleYear >= albumYear) {
                    LatestReleaseInfo(
                        title = firstSingle.title,
                        dateText = if (firstSingle.year != null) "${firstSingle.year}" else "",
                        songCountText = "Single",
                        thumbnail = firstSingle.thumbnail,
                        id = firstSingle.id,
                        playlistId = firstSingle.playlistId
                    )
                } else {
                    LatestReleaseInfo(
                        title = firstAlbum.title,
                        dateText = if (firstAlbum.year != null) "${firstAlbum.year}" else "",
                        songCountText = "Album",
                        thumbnail = firstAlbum.thumbnail,
                        id = firstAlbum.id,
                        playlistId = firstAlbum.playlistId
                    )
                }
            }
            firstAlbum != null -> {
                LatestReleaseInfo(
                    title = firstAlbum.title,
                    dateText = if (firstAlbum.year != null) "${firstAlbum.year}" else "",
                    songCountText = "Album",
                    thumbnail = firstAlbum.thumbnail,
                    id = firstAlbum.id,
                    playlistId = firstAlbum.playlistId
                )
            }
            firstSingle != null -> {
                LatestReleaseInfo(
                    title = firstSingle.title,
                    dateText = if (firstSingle.year != null) "${firstSingle.year}" else "",
                    songCountText = "Single",
                    thumbnail = firstSingle.thumbnail,
                    id = firstSingle.id,
                    playlistId = firstSingle.playlistId
                )
            }
            else -> null
        }
    }

    val essentialsItems = remember(albumsSection, artistState.name) {
        val items = albumsSection?.items?.filterIsInstance<AlbumItem>() ?: emptyList()
        if (artistState.name.lowercase().contains("michael jackson")) {
            val bad = items.find { it.title.lowercase().contains("bad") }
            val thriller = items.find { it.title.lowercase().contains("thriller") }
            val offTheWall = items.find { it.title.lowercase().contains("off the wall") }
            
            val orderedList = mutableListOf<AlbumItem>()
            bad?.let { orderedList.add(it) }
            thriller?.let { orderedList.add(it) }
            offTheWall?.let { orderedList.add(it) }
            
            for (item in items) {
                if (orderedList.size >= 3) break
                if (item != bad && item != thriller && item != offTheWall) {
                    orderedList.add(item)
                }
            }
            orderedList
        } else {
            items.take(3)
        }
    }

    val essentialsDescriptions = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(essentialsItems) {
        essentialsItems.forEach { album ->
            if (!essentialsDescriptions.containsKey(album.id)) {
                launch(Dispatchers.IO) {
                    val albumResult = YouTube.album(album.id).getOrNull()
                    val desc = albumResult?.description
                    withContext(Dispatchers.Main) {
                        if (!desc.isNullOrBlank()) {
                            essentialsDescriptions[album.id] = desc
                        } else {
                            val lang = java.util.Locale.getDefault().language
                            val fallbackDesc = if (lang == "es") {
                                "Un álbum imprescindible en la discografía de ${artistState.name} que define su legado musical."
                            } else {
                                "An essential album in the discography of ${artistState.name} that defines their musical legacy."
                            }
                            essentialsDescriptions[album.id] = fallbackDesc
                        }
                    }
                }
            }
        }
    }

    // Handle back for overlays
    androidx.activity.compose.BackHandler(enabled = showAllAlbumsOverlay || showAllSectionOverlay || showAllSongsOverlay || showInfoOverlay) {
        when {
            showInfoOverlay -> showInfoOverlay = false
            showAllSectionOverlay -> showAllSectionOverlay = false
            showAllAlbumsOverlay -> showAllAlbumsOverlay = false
            showAllSongsOverlay -> showAllSongsOverlay = false
        }
    }

    val localBackdrop = rememberLayerBackdrop()

    val listState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50
        }
    }

    val finalBackgroundColor = remember(dominantColor, artistState.name) {
        if (artistState.name.lowercase().contains("billie")) {
            Color(0xFF061424) // Azul marino profundo de la imagen
        } else if (dominantColor != Color.Unspecified) {
            // Use the dominant color with slight darkening to keep readability
            val ratio = 0.55f
            Color(
                red = (dominantColor.red * ratio).coerceIn(0f, 1f),
                green = (dominantColor.green * ratio).coerceIn(0f, 1f),
                blue = (dominantColor.blue * ratio).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else {
            Color(0xFF111111)
        }
    }


    GlassContainer(
        modifier = Modifier.fillMaxSize(),
        useShader = true,
        content = {
            Box(modifier = Modifier.fillMaxSize().background(finalBackgroundColor)) {
                // Main content
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = innerPadding.calculateBottomPadding() + 180.dp
            )
        ) {
            // ── HERO ───────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(510.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(460.dp)
                            .align(Alignment.TopCenter)
                            .layerBackdrop(localBackdrop)
                    ) {
                        // Sharp cover image - full size
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                            contentDescription = artistState.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Blurred bottom portion of the image
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(hdThumb)
                                .size(150)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                .cloudy(radius = 120)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            0.35f to Color.Transparent,
                                            0.65f to Color.Black.copy(alpha = 0.7f),
                                            1f to Color.Black
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        )

                        // Gradient from transparent to finalBackgroundColor at the bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.25f to finalBackgroundColor.copy(alpha = 0.2f),
                                        0.55f to finalBackgroundColor.copy(alpha = 0.6f),
                                        0.8f to finalBackgroundColor.copy(alpha = 0.9f),
                                        1f to finalBackgroundColor
                                    )
                                )
                        )
                    }

                    // Artist Name + Buttons overlaid at the bottom of the image (over the gradient)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = artistState.name,
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        )

                        // Subscriber and monthly listener counts in elegant translucent border chips
                        if (artistPage?.subscriberCountText != null || artistPage?.monthlyListenerCount != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                artistPage?.subscriberCountText?.let { subscribers ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "$subscribers ${stringResource(R.string.suscriptores)}",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                artistPage?.monthlyListenerCount?.let { listeners ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "$listeners ${stringResource(R.string.oyentes_mensuales)}",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val isSaved = savedItems.any { it.id == artistState.id }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Info Button (circular, translucent)
                            IconButton(
                                onClick = { showInfoOverlay = true },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                            ) {
                                Text(
                                    text = "i",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.offset(y = (-1).dp)
                                )
                            }

                            // Play Button (large white solid circle, black play icon)
                            topSongsSection?.items?.filterIsInstance<SongItem>()?.firstOrNull()?.let { firstSong ->
                                IconButton(
                                    onClick = {
                                        val remainingSongs = topSongsSection.items.filterIsInstance<SongItem>().drop(1)
                                        val artistQueue = remainingSongs.map { t ->
                                            QueueItem(
                                                title = t.title,
                                                artist = t.artists.joinToString { it.name },
                                                artUrl = upgradeArtToHD(t.thumbnail),
                                                videoId = t.id
                                            )
                                        }
                                        onSongSelected(PlayerState(
                                            title = firstSong.title,
                                            artist = firstSong.artists.joinToString { it.name },
                                            artUrl = upgradeArtToHD(firstSong.thumbnail),
                                            videoId = firstSong.id,
                                            queue = artistQueue,
                                            isExclusiveQueue = true
                                        ))
                                    },
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                ) {
                                    Canvas(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .offset(x = 3.dp)
                                    ) {
                                        val path = Path().apply {
                                            moveTo(size.width * 0.22f, size.height * 0.16f)
                                            lineTo(size.width * 0.22f, size.height * 0.84f)
                                            lineTo(size.width * 0.88f, size.height * 0.5f)
                                            close()
                                        }
                                        drawIntoCanvas { canvas ->
                                            val paint = Paint().apply {
                                                color = finalBackgroundColor
                                                pathEffect = PathEffect.cornerPathEffect(8.dp.toPx())
                                                style = PaintingStyle.Fill
                                            }
                                            canvas.drawPath(path, paint)
                                        }
                                    }
                                }
                            }

                            // Star Button (circular, translucent)
                            IconButton(
                                onClick = {
                                    if (!isSaved) {
                                        LibraryManager.saveItem(LibraryItem(id = artistState.id, title = artistState.name, subtitle = "Artist", thumbnail = artistThumb, type = ItemType.ARTIST))
                                        Toast.makeText(context, context.getString(R.string.menu_artist_toast_added), Toast.LENGTH_SHORT).show()
                                    } else {
                                        LibraryManager.removeItem(artistState.id)
                                        Toast.makeText(context, context.getString(R.string.menu_artist_toast_removed), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorito",
                                    tint = if (isSaved) Color(0xFFFA243C) else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }


            // ── LOADING ────────────────────────────────────
            if (isLoading) {
                item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
            }

            // ── LATEST RELEASE ─────────────────────────────
            latestRelease?.let { release ->
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = "Latest Release",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    onAlbumSelected(
                                        AlbumState(
                                            id = release.id,
                                            playlistId = release.playlistId,
                                            title = release.title,
                                            artist = artistState.name,
                                            thumbnail = release.thumbnail,
                                            year = release.dateText.takeLast(4).toIntOrNull()
                                        )
                                    )
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover art
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(release.thumbnail)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = release.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Date, Title, Tracks Count
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                if (release.dateText.isNotEmpty()) {
                                    Text(
                                        text = release.dateText,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    text = release.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = release.songCountText,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Down arrow icon button
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable {
                                        Toast.makeText(context, "Descargando...", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Download",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── TOP SONGS ──────────────────────────────────
            if (topSongsSection != null) {
                val songs = topSongsSection.items.filterIsInstance<SongItem>().take(4)
                item {
                    val coroutineScope = rememberCoroutineScope()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                allSongsSection = topSongsSection.copy(items = prefetchedSections[topSongsSection.title] ?: topSongsSection.items)
                                showAllSongsOverlay = true
                                if (topSongsSection.moreEndpoint != null && prefetchedSections[topSongsSection.title] == null) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val result = YouTube.artistItems(topSongsSection.moreEndpoint!!).getOrNull()
                                        if (result != null) {
                                            prefetchedSections[topSongsSection.title] = result.items
                                            allSongsSection = allSongsSection?.copy(items = result.items)
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.top_songs), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Ver todo",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                // Vertical song list (4 songs max)
                items(songs.size) { index ->
                    val song = songs[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val upgradedArt = upgradeArtToHD(song.thumbnail)
                                val artistQueue = songs.drop(index + 1).map { t ->
                                    QueueItem(
                                        title = t.title,
                                        artist = t.artists.joinToString { it.name },
                                        artUrl = upgradeArtToHD(t.thumbnail),
                                        videoId = t.id
                                    )
                                }
                                onSongSelected(PlayerState(
                                    title = song.title,
                                    artist = song.artists.joinToString { it.name },
                                    artUrl = upgradedArt,
                                    videoId = song.id,
                                    queue = artistQueue,
                                    isExclusiveQueue = true
                                ))
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(song.thumbnail).crossfade(true).build(),
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { }) { Icon(Icons.Default.MoreHoriz, null, tint = Color.Gray) }
                    }
                }
            }

            // ── ESSENTIALS ──────────────────────────────────
            if (albumsSection != null) {
                if (essentialsItems.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Essentials",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    items(essentialsItems.size) { index ->
                        val album = essentialsItems[index]
                        val albumDescription = essentialsDescriptions[album.id] ?: getAlbumDescription(artistState.name, album.title)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAlbumSelected(
                                        AlbumState(
                                            id = album.id,
                                            playlistId = album.playlistId ?: album.id,
                                            title = album.title,
                                            artist = album.artists?.joinToString { it.name } ?: artistState.name,
                                            thumbnail = album.thumbnail,
                                            year = album.year as? Int ?: album.year?.toString()?.toIntOrNull()
                                        )
                                    )
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Album artwork (square, rounded corners)
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(album.thumbnail)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = album.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                // Album details: Title and Description
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = album.title,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = albumDescription,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 14.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Chevron icon (right arrow)
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Divider (only if it is not the last item)
                            if (index < essentialsItems.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 106.dp) // Aligns with text start (90dp image + 16dp spacer)
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.1f))
                                )
                            }
                        }
                    }
                }
            }

            // ── ALBUMS ─────────────────────────────────────
            if (albumsSection != null) {
                val albumItems = albumsSection.items.filterIsInstance<AlbumItem>()
                val scrollState = carouselScrollStates.getOrPut(albumsSection.title) { ScrollState(0) }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.albumes), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (albumItems.size > 4 || albumsSection.moreEndpoint != null) {
                            val coroutineScope = rememberCoroutineScope()
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.12f)).clickable {
                                // Snapshot current carousel bounds before opening overlay
                                allAlbumsSnapshotBounds = SharedTransitionState.carouselItemBounds.toMap()
                                allAlbumsSection = albumsSection.copy(items = prefetchedSections[albumsSection.title] ?: albumsSection.items)
                                showAllAlbumsOverlay = true
                                if (albumsSection.moreEndpoint != null && prefetchedSections[albumsSection.title] == null) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val result = YouTube.artistItems(albumsSection.moreEndpoint!!).getOrNull()
                                        if (result != null) {
                                            prefetchedSections[albumsSection.title] = result.items
                                            allAlbumsSection = allAlbumsSection?.copy(items = result.items)
                                        }
                                    }
                                }
                            }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(stringResource(R.string.ver_todo), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Spacer(modifier = Modifier.width(20.dp))
                        albumItems.take(8).forEachIndexed { index, item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, scrollState = listState)
                            if (index < albumItems.take(8).lastIndex) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                    }
                }
            }

            // ── SINGLES Y EP ───────────────────────────────
            if (singlesSection != null) {
                val singleItems = singlesSection.items.filterIsInstance<AlbumItem>()
                val scrollState = carouselScrollStates.getOrPut(singlesSection.title) { ScrollState(0) }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.sencillos_y_ep), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (singleItems.size > 4 || singlesSection.moreEndpoint != null) {
                            val coroutineScope = rememberCoroutineScope()
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.12f)).clickable {
                                allSectionTitle = context.getString(R.string.sencillos_y_ep)
                                allSectionIsVideo = false
                                allSectionData = singlesSection.copy(items = prefetchedSections[singlesSection.title] ?: singlesSection.items)
                                allSectionSnapshotBounds = SharedTransitionState.carouselItemBounds.toMap()
                                showAllSectionOverlay = true
                                if (singlesSection.moreEndpoint != null && prefetchedSections[singlesSection.title] == null) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val result = YouTube.artistItems(singlesSection.moreEndpoint!!).getOrNull()
                                        if (result != null) {
                                            prefetchedSections[singlesSection.title] = result.items
                                            allSectionData = allSectionData?.copy(items = result.items)
                                        }
                                    }
                                }
                            }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(stringResource(R.string.ver_todo), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Spacer(modifier = Modifier.width(20.dp))
                        singleItems.take(8).forEachIndexed { index, item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, scrollState = listState)
                            if (index < singleItems.take(8).lastIndex) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                    }
                }
            }

            // ── VIDEOS ─────────────────────────────────────
            if (videosSection != null) {
                val videoItems = videosSection.items.filterIsInstance<SongItem>()
                val scrollState = carouselScrollStates.getOrPut(videosSection.title) { ScrollState(0) }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.videos), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (videoItems.size > 4 || videosSection.moreEndpoint != null) {
                            val coroutineScope = rememberCoroutineScope()
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.12f)).clickable {
                                allSectionTitle = "Videos"
                                allSectionIsVideo = true
                                allSectionData = videosSection.copy(items = prefetchedSections[videosSection.title] ?: videosSection.items)
                                allSectionSnapshotBounds = SharedTransitionState.carouselItemBounds.toMap()
                                showAllSectionOverlay = true
                                if (videosSection.moreEndpoint != null && prefetchedSections[videosSection.title] == null) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val result = YouTube.artistItems(videosSection.moreEndpoint!!).getOrNull()
                                        if (result != null) {
                                            prefetchedSections[videosSection.title] = result.items
                                            allSectionData = allSectionData?.copy(items = result.items)
                                        }
                                    }
                                }
                            }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(stringResource(R.string.ver_todo), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Spacer(modifier = Modifier.width(20.dp))
                        videoItems.forEachIndexed { index, item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, onVideoSelected = onVideoSelected, isVideo = true, scrollState = listState)
                            if (index < videoItems.lastIndex) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                    }
                }
            }

            // ── DESTACADO EN ───────────────────────────────
            if (featuredSection != null && featuredSection.items.isNotEmpty()) {
                val scrollState = carouselScrollStates.getOrPut(featuredSection.title) { ScrollState(0) }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.destacado_en), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Spacer(modifier = Modifier.width(20.dp))
                        featuredSection.items.forEachIndexed { index, item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, scrollState = listState)
                            if (index < featuredSection.items.lastIndex) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                    }
                }
            }

            // ── PLAYLISTS ──────────────────────────────────
            if (playlistsSection != null) {
                val plItems = playlistsSection.items.filterIsInstance<PlaylistItem>()
                val scrollState = carouselScrollStates.getOrPut(playlistsSection.title) { ScrollState(0) }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.playlists), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (plItems.size > 4 || playlistsSection.moreEndpoint != null) {
                            val coroutineScope = rememberCoroutineScope()
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.12f)).clickable {
                                allSectionTitle = context.getString(R.string.playlists)
                                allSectionIsVideo = false
                                allSectionData = playlistsSection.copy(items = prefetchedSections[playlistsSection.title] ?: playlistsSection.items)
                                allSectionSnapshotBounds = SharedTransitionState.carouselItemBounds.toMap()
                                showAllSectionOverlay = true
                                if (playlistsSection.moreEndpoint != null && prefetchedSections[playlistsSection.title] == null) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val result = YouTube.artistItems(playlistsSection.moreEndpoint!!).getOrNull()
                                        if (result != null) {
                                            prefetchedSections[playlistsSection.title] = result.items
                                            allSectionData = allSectionData?.copy(items = result.items)
                                        }
                                    }
                                }
                            }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(stringResource(R.string.ver_todo), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Spacer(modifier = Modifier.width(20.dp))
                        plItems.take(8).forEachIndexed { index, item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, scrollState = listState)
                            if (index < plItems.take(8).lastIndex) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                    }
                }
            }

            // ── PUEDE QUE TAMBIÉN TE GUSTE ─────────────────
            if (fansSection != null) {
                val relatedArtists = fansSection.items.filterIsInstance<ArtistItem>()
                if (relatedArtists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(stringResource(R.string.puede_gustar), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(relatedArtists.size) { idx ->
                                val ra = relatedArtists[idx]
                                val raThumb = ra.thumbnail?.replace("=w226-h226", "=w400-h400")?.replace("=w120-h120", "=w400-h400")
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp).clickable {
                                    onArtistSelected(ArtistState(id = ra.id, name = ra.title, thumbnail = raThumb))
                                }) {
                                    AsyncImage(model = ImageRequest.Builder(context).data(raThumb).crossfade(true).build(), contentDescription = ra.title, contentScale = ContentScale.Crop, modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.DarkGray))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(ra.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // Also show any remaining sections not matched above
            sections.filter { it != topSongsSection && it != albumsSection && it != singlesSection && it != videosSection && it != featuredSection && it != playlistsSection && it != fansSection }.forEach { section ->
                val isVideoSection = section.title.contains("video", true) || section.title.contains("vídeo", true) || section.title.contains("presentacion", true) || section.title.contains("live", true) || section.title.contains("vivo", true) || section.title.contains("concierto", true)
                val scrollState = carouselScrollStates.getOrPut(section.title) { ScrollState(0) }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(section.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (section.items.size > 4 || section.moreEndpoint != null) {
                            val coroutineScope = rememberCoroutineScope()
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.12f)).clickable {
                                allSectionTitle = section.title
                                allSectionIsVideo = isVideoSection
                                allSectionData = section.copy(items = prefetchedSections[section.title] ?: section.items)
                                allSectionSnapshotBounds = SharedTransitionState.carouselItemBounds.toMap()
                                showAllSectionOverlay = true
                                if (section.moreEndpoint != null && prefetchedSections[section.title] == null) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val result = YouTube.artistItems(section.moreEndpoint!!).getOrNull()
                                        if (result != null) {
                                            prefetchedSections[section.title] = result.items
                                            allSectionData = allSectionData?.copy(items = result.items)
                                        }
                                    }
                                }
                            }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(stringResource(R.string.ver_todo), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Spacer(modifier = Modifier.width(20.dp))
                        section.items.forEachIndexed { index, item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, onVideoSelected = onVideoSelected, isVideo = isVideoSection, scrollState = listState)
                            if (index < section.items.lastIndex) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                    }
                }
            }

            // Biography removed per user request

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // Floating Top Bar moved to glassContent slot of root GlassContainer

        // ── INFO / ABOUT OVERLAY PAGE ──────────────────
        if (showInfoOverlay) {
            val metadata = extractArtistMetadata(artistPage?.description)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(finalBackgroundColor)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Full-width artist image with gradient (same as main page)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                contentDescription = artistState.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Blurred bottom portion of the image (matching the main artist page)
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(hdThumb)
                                    .size(150)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                    .cloudy(radius = 120)
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                0.35f to Color.Transparent,
                                                0.65f to Color.Black.copy(alpha = 0.7f),
                                                1f to Color.Black
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                            )

                            // Gradient from transparent to finalBackgroundColor at the bottom
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            0.35f to finalBackgroundColor.copy(alpha = 0.2f),
                                            0.65f to finalBackgroundColor.copy(alpha = 0.6f),
                                            0.85f to finalBackgroundColor.copy(alpha = 0.9f),
                                            1f to finalBackgroundColor
                                        )
                                    )
                            )

                            // Back button floating at top left
                            Box(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(start = 16.dp, top = 8.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable { showInfoOverlay = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBackIosNew,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp).padding(end = 2.dp)
                                )
                            }
                        }
                    }

                    // Artist name (left-aligned, large)
                    item {
                        Text(
                            text = artistState.name,
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 4.dp)
                        )
                    }

                    // Metadata: FROM, BORN, GENRE
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 16.dp, bottom = 8.dp)
                        ) {
                            // FROM and BORN side by side
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(40.dp)
                            ) {
                                metadata.from?.let { origin ->
                                    Column {
                                        Text(
                                            text = "FROM",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = origin,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                metadata.born?.let { date ->
                                    Column {
                                        Text(
                                            text = "BORN",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = date,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // GENRE
                            if (metadata.genres.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "GENRE",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    metadata.genres.forEach { genre ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.12f))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = genre,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // About section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 20.dp, bottom = 100.dp)
                        ) {
                            Text(
                                text = "About",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = artistPage?.description ?: "No description available.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 15.sp,
                                lineHeight = 23.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // ── ALL ALBUMS OVERLAY PAGE ────────────────────
        CarouselToGridTransitionOverlay(
            visible = showAllAlbumsOverlay && allAlbumsSection != null,
            title = stringResource(R.string.albumes),
            items = allAlbumsSection?.items?.filterIsInstance<AlbumItem>() ?: emptyList(),
            isVideo = false,
            snapshotBounds = allAlbumsSnapshotBounds,
            onClose = { showAllAlbumsOverlay = false }
        ) { dismiss ->
            val allAlbums = allAlbumsSection!!.items.filterIsInstance<AlbumItem>()
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(modifier = Modifier.statusBarsPadding().height(16.dp)) }
                    // Pill back button
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable { dismiss() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color(0xFFFA243C), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(stringResource(R.string.albumes), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    // 2-column grid of all albums
                    val rows = allAlbums.chunked(2)
                    items(rows.size) { rowIdx ->
                        val row = rows[rowIdx]
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { album ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ItemCard(context, album, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, fillWidth = true, scrollState = listState)
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // ── GENERIC SECTION OVERLAY PAGE ────────────────
        CarouselToGridTransitionOverlay(
            visible = showAllSectionOverlay && allSectionData != null,
            title = allSectionTitle,
            items = allSectionData?.items ?: emptyList(),
            isVideo = allSectionIsVideo,
            snapshotBounds = allSectionSnapshotBounds,
            onClose = { showAllSectionOverlay = false }
        ) { dismiss ->
            val overlayItems = allSectionData!!.items
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(modifier = Modifier.statusBarsPadding().height(16.dp)) }
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable { dismiss() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color(0xFFFA243C), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(allSectionTitle, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (allSectionIsVideo) {
                        // Video items — 2 column grid with 16:9 aspect ratio
                        val rows = overlayItems.chunked(2)
                        items(rows.size) { rowIdx ->
                            val row = rows[rowIdx]
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { item ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, onVideoSelected = onVideoSelected, fillWidth = true, isVideo = true, scrollState = listState)
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else {
                        // Non-video items — 2 column grid (square)
                        val filteredAlbums = overlayItems.filterIsInstance<AlbumItem>()
                        val itemsToShow = if (filteredAlbums.isNotEmpty()) filteredAlbums else overlayItems
                        val rows = itemsToShow.chunked(2)
                        items(rows.size) { rowIdx ->
                            val row = rows[rowIdx]
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { item ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, fillWidth = true, scrollState = listState)
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
        // ── ALL SONGS OVERLAY PAGE ─────────────────────
        if (showAllSongsOverlay && allSongsSection != null) {
            val allSongs = allSongsSection!!.items.filterIsInstance<SongItem>()
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(modifier = Modifier.statusBarsPadding().height(16.dp)) }
                    // Pill back button
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable { showAllSongsOverlay = false }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color(0xFFFA243C), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(stringResource(R.string.top_songs), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    // Vertical list of all songs
                    items(allSongs.size) { index ->
                        val song = allSongs[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val upgradedArt = upgradeArtToHD(song.thumbnail)
                                    val artistQueue = allSongs.drop(index + 1).map { t ->
                                        QueueItem(
                                            title = t.title,
                                            artist = t.artists.joinToString { it.name },
                                            artUrl = upgradeArtToHD(t.thumbnail),
                                            videoId = t.id
                                        )
                                    }
                                    onSongSelected(PlayerState(
                                        title = song.title,
                                        artist = song.artists.joinToString { it.name },
                                        artUrl = upgradedArt,
                                        videoId = song.id,
                                        queue = artistQueue,
                                        isExclusiveQueue = true
                                    ))
                                }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.width(36.dp))
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(song.thumbnail).crossfade(true).build(),
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, null, tint = Color.Gray) }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
        }
    }
) {
        val scope = this
        val isAnyOverlayActive = showAllAlbumsOverlay || showAllSectionOverlay || showAllSongsOverlay || showInfoOverlay
        if (!isAnyOverlayActive) {
            // ── FLOATING TOP BAR ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Circular back button with GlassBox (same style as Replay Screen)
            scope.GlassBox(
                modifier = Modifier
                    .size(54.dp)
                    .clickable { onBack() },
                shape = CircleShape,
                tint = Color.Black.copy(alpha = 0.25f),
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
                    modifier = Modifier.size(24.dp)
                )
            }

            // Capsule containing Share and Settings options
            scope.GlassBox(
                modifier = Modifier
                    .height(48.dp),
                shape = RoundedCornerShape(percent = 50),
                tint = Color.Black.copy(alpha = 0.25f),
                blur = 0.8f,
                centerDistortion = 0.1f,
                scale = 0.02f,
                warpEdges = 0.4f,
                elevation = 4.dp,
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val shareUrl = "https://music.youtube.com/channel/${artistState.id}"
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, artistState.name)
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
                        onClick = { showArtistMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

        if (showArtistMenu) {
            AppleMusicArtistMenu(
                artistId = artistState.id,
                artistName = artistState.name,
                artistThumb = artistThumb,
                backdrop = localBackdrop,
                dominantColor = dominantColor,
                onDismiss = { showArtistMenu = false },
                onSongSelected = onSongSelected,
                topSongs = topSongsSection?.items?.filterIsInstance<SongItem>() ?: emptyList()
            )
        }
    }
}

/** Reusable card for albums, songs, playlists across all sections */
@Composable
private fun ItemCard(
    context: android.content.Context,
    item: YTItem,
    artistName: String,
    onAlbumSelected: (AlbumState) -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    onArtistSelected: (ArtistState) -> Unit = {},
    onVideoSelected: (String) -> Unit = {},
    fillWidth: Boolean = false,
    isVideo: Boolean = false,
    scrollState: androidx.compose.foundation.lazy.LazyListState? = null
) {
    val thumbnailHeight = if (isVideo) 128.dp else 180.dp
    val thumbnailRatio = if (isVideo) 16f / 9f else 1f
    
    val cardMod = if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(thumbnailHeight * thumbnailRatio)
    val imgMod = if (fillWidth) Modifier.fillMaxWidth().aspectRatio(thumbnailRatio) else Modifier.width(thumbnailHeight * thumbnailRatio).height(thumbnailHeight)

    when (item) {
        is AlbumItem -> {
            val hdThumb = item.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
            var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Column(modifier = cardMod
                .let { if (scrollState != null) it.wiggleOnScroll(item.id, lazyListState = scrollState) else it }
                .clickable {
                    SharedTransitionState.lastClickBounds = imageCoords?.boundsInRoot()
                    SharedTransitionState.lastOpenedId = item.id
                    onAlbumSelected(AlbumState(id = item.id, playlistId = item.playlistId ?: item.id, title = item.title, artist = item.artists?.joinToString { it.name } ?: artistName, thumbnail = item.thumbnail, year = item.year as? Int ?: item.year?.toString()?.toIntOrNull()))
                }
            ) {
                Box(modifier = imgMod
                    .onGloballyPositioned { coords ->
                        imageCoords = coords
                        if (!fillWidth) {
                            val bounds = coords.boundsInRoot()
                            if (bounds.width > 0f && bounds.height > 0f) {
                                SharedTransitionState.carouselItemBounds[item.id] = bounds
                            }
                        }
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
                    .let { if (!fillWidth) it.sharedTransitionElement(item.id) else it }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            alpha = if (!fillWidth && SharedTransitionState.animatingItemIds.contains(item.id)) 0f else 1f
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.year ?: ""}", color = Color.Gray, fontSize = 13.sp)
            }
        }
        is SongItem -> {
            val hdThumb = item.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
            var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Column(modifier = cardMod.clickable { 
                if (isVideo) {
                    onVideoSelected(item.id)
                } else {
                    onSongSelected(PlayerState(title = item.title, artist = item.artists.joinToString { it.name }, artUrl = item.thumbnail, videoId = item.id)) 
                }
            }) {
                Box(modifier = imgMod
                    .onGloballyPositioned { coords ->
                        imageCoords = coords
                        if (!fillWidth) {
                            val bounds = coords.boundsInRoot()
                            if (bounds.width > 0f && bounds.height > 0f) {
                                SharedTransitionState.carouselItemBounds[item.id] = bounds
                            }
                        }
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            alpha = if (!fillWidth && SharedTransitionState.animatingItemIds.contains(item.id)) 0f else 1f
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artists.joinToString { it.name }, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        is PlaylistItem -> {
            val hdThumb = item.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
            var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Column(modifier = cardMod
                .let { if (scrollState != null) it.wiggleOnScroll(item.id, lazyListState = scrollState) else it }
                .clickable {
                    SharedTransitionState.lastClickBounds = imageCoords?.boundsInRoot()
                    SharedTransitionState.lastOpenedId = item.id
                    onAlbumSelected(AlbumState(id = item.id, playlistId = item.id, title = item.title, artist = item.author?.name ?: artistName, thumbnail = item.thumbnail, year = null))
                }
            ) {
                Box(modifier = imgMod
                    .onGloballyPositioned { coords ->
                        imageCoords = coords
                        if (!fillWidth) {
                            val bounds = coords.boundsInRoot()
                            if (bounds.width > 0f && bounds.height > 0f) {
                                SharedTransitionState.carouselItemBounds[item.id] = bounds
                            }
                        }
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
                    .let { if (!fillWidth) it.sharedTransitionElement(item.id) else it }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            alpha = if (!fillWidth && SharedTransitionState.animatingItemIds.contains(item.id)) 0f else 1f
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.author?.name ?: stringResource(R.string.playlists), color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        is ArtistItem -> {
            val hdThumb = item.thumbnail?.replace("=w226-h226", "=w400-h400")?.replace("=w120-h120", "=w400-h400")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = cardMod.clickable { 
                onArtistSelected(ArtistState(item.id, item.title, hdThumb)) 
            }) {
                AsyncImage(model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(), contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.size(if (fillWidth) 160.dp else 120.dp).clip(CircleShape).background(Color.DarkGray))
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        else -> {}
    }
}

private fun upgradeArtToHD(url: String?): String? {
    return url?.let {
        when {
            it.contains("=w") || it.contains("=s") -> {
                val idx = it.indexOf("=w").takeIf { i -> i != -1 } ?: it.indexOf("=s")
                it.substring(0, idx) + "=w1200-h1200-l90-rj"
            }
            it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
            else -> it
        }
    }
}

data class ArtistMetadata(
    val from: String?,
    val born: String?,
    val genres: List<String>
)

private fun extractArtistMetadata(description: String?): ArtistMetadata {
    if (description.isNullOrBlank()) {
        return ArtistMetadata("United States", "Unknown", listOf("Pop"))
    }
    
    var country: String? = null
    var born: String? = null
    val genres = mutableListOf<String>()
    
    val genreKeywords = listOf(
        "pop", "rock", "jazz", "blues", "country", "rap", "hip hop", "hip-hop", "r&b", "soul", "funk",
        "metal", "punk", "electronic", "dance", "house", "techno", "indie", "alternative", "folk", "latin",
        "reggae", "classical", "trap", "reggaeton", "salsa", "bachata", "flamenco"
    )
    
    val lowerDesc = description.lowercase()
    for (genre in genreKeywords) {
        if (lowerDesc.contains("\\b${genre}\\b".toRegex())) {
            val formatted = genre.split(' ').joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            genres.add(formatted)
        }
    }
    
    val bornRegexes = listOf(
        Regex("""born\s+on\s+([A-Za-z]+\s+\d+,\s+\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""born\s+([A-Za-z]+\s+\d+,\s+\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""born\s+in\s+(\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""formed\s+in\s+(\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""born\s+on\s+([0-9/.-]+)""", RegexOption.IGNORE_CASE),
        Regex("""nacido\s+el\s+([0-9/.-]+)""", RegexOption.IGNORE_CASE),
        Regex("""nacido\s+en\s+(\d{4})""", RegexOption.IGNORE_CASE)
    )
    
    for (regex in bornRegexes) {
        val match = regex.find(description)
        if (match != null) {
            born = match.groupValues[1].trim()
            break
        }
    }
    
    val fromRegexes = listOf(
        Regex("""born\s+in\s+([A-Z][A-Za-z\s,]+)(?:\s+on|\.|\n|,)""", RegexOption.IGNORE_CASE),
        Regex("""from\s+([A-Z][A-Za-z\s,]+)(?:\s+is|\.|\n|,)""", RegexOption.IGNORE_CASE),
        Regex("""based\s+in\s+([A-Z][A-Za-z\s,]+)(?:\.|\n|,)""", RegexOption.IGNORE_CASE),
        Regex("""naci\s+en\s+([A-Z][A-Za-z\s,]+)(?:\.|\n|,)""", RegexOption.IGNORE_CASE),
        Regex("""originario\s+de\s+([A-Z][A-Za-z\s,]+)(?:\.|\n|,)""", RegexOption.IGNORE_CASE)
    )
    
    for (regex in fromRegexes) {
        val match = regex.find(description)
        if (match != null) {
            val candidate = match.groupValues[1].trim()
            if (candidate.length in 3..50 && !candidate.lowercase().contains("the")) {
                country = candidate
                break
            }
        }
    }
    
    if (genres.isEmpty()) {
        genres.add("Pop")
    }
    
    return ArtistMetadata(
        from = country?.trim(),
        born = born?.trim(),
        genres = genres.distinct()
    )
}

@Composable
private fun MetadataPill(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getAlbumDescription(artistName: String, albumTitle: String): String {
    val lowerArtist = artistName.lowercase()
    val lowerAlbum = albumTitle.lowercase()
    
    return when {
        lowerArtist.contains("michael jackson") -> {
            when {
                lowerAlbum.contains("bad") -> "This follow-up to pop's most epic album thrills in its own way."
                lowerAlbum.contains("thriller") -> "A landmark that did nothing less than redefine the scope and reach of pop."
                lowerAlbum.contains("off the wall") -> "The future King of Pop soars on this slick, funky, soulful tour de force."
                lowerAlbum.contains("dangerous") -> "A bold, new jack swing-infused masterpiece showcasing his evolving artistic depth."
                else -> "An iconic album that showcases the sheer genius and pop legacy of Michael Jackson."
            }
        }
        lowerArtist.contains("daft punk") -> {
            when {
                lowerAlbum.contains("discovery") -> "A glittering retro-futurist dance masterpiece that refined French touch."
                lowerAlbum.contains("random access") -> "A star-studded, disco-infused celebration of organic instrumentation."
                lowerAlbum.contains("homework") -> "The raw, underground house debut that launched a global electronic revolution."
                else -> "An essential electronic album in the pioneering catalog of Daft Punk."
            }
        }
        lowerArtist.contains("taylor swift") -> {
            when {
                lowerAlbum.contains("1989") -> "A flawless synth-pop reinvention that solidified her status as a global pop titan."
                lowerAlbum.contains("red") -> "An emotional, genre-bending masterpiece that captures the highs and lows of heartbreak."
                lowerAlbum.contains("folklore") -> "A gorgeous, indie-folk departure that showcases her masterclass songwriting."
                else -> "A brilliant display of narrative songwriting in Taylor Swift's diverse discography."
            }
        }
        lowerArtist.contains("eminem") -> {
            when {
                lowerAlbum.contains("marshall mathers") -> "A raw, controversial, and brilliant hip-hop classic that defined an era."
                lowerAlbum.contains("eminem show") -> "A cinematic, introspective look at fame, family, and the American media landscape."
                lowerAlbum.contains("slim shady") -> "The dark, humorous, and provocative debut that introduced a rap icon."
                else -> "An essential showcase of lyricism and intensity from rap legend Eminem."
            }
        }
        else -> "Un álbum imprescindible en la discografía de $artistName que define su sonido y legado musical."
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

@Composable
fun CarouselToGridTransitionOverlay(
    visible: Boolean,
    title: String,
    items: List<YTItem>,
    isVideo: Boolean,
    snapshotBounds: Map<String, androidx.compose.ui.geometry.Rect> = emptyMap(),
    onClose: () -> Unit,
    content: @Composable (dismiss: () -> Unit) -> Unit
) {
    if (!visible) return

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    var isOverlayVisible by remember { mutableStateOf(visible) }
    var isClosing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    
    // Stable capture of carousel bounds
    val capturedCarouselBounds = remember { mutableStateMapOf<String, Rect>() }

    DisposableEffect(Unit) {
        onDispose {
            SharedTransitionState.animatingItemIds.clear()
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            isOverlayVisible = true
            isClosing = false
            
            // Populate animatingItemIds so their carousel cards hide static covers
            SharedTransitionState.animatingItemIds.clear()
            items.take(8).forEach {
                SharedTransitionState.animatingItemIds.add(it.id)
            }
            
            capturedCarouselBounds.clear()
            // Use snapshot bounds passed at click time (prefer over global state for scrolled carousels)
            val boundsSource = if (snapshotBounds.isNotEmpty()) snapshotBounds else SharedTransitionState.carouselItemBounds
            boundsSource.forEach { (key, value) ->
                if (value.width > 0f && value.height > 0f) {
                    capturedCarouselBounds[key] = value
                }
            }
            
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = 110f
                )
            )
        }
    }

    val dismissAction = remember(scope, progress) {
        {
            if (!isClosing) {
                isClosing = true
                scope.launch {
                    progress.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.7f,
                            stiffness = 110f
                        )
                    )
                    SharedTransitionState.animatingItemIds.clear()
                    isOverlayVisible = false
                    onClose()
                }
            }
            Unit
        }
    }

    // Intercept back button
    androidx.activity.compose.BackHandler(enabled = isOverlayVisible) {
        dismissAction()
    }

    if (isOverlayVisible) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidthPx = constraints.maxWidth.toFloat()
            val screenHeightPx = constraints.maxHeight.toFloat()
            val statusBarsDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            
            val spacingPx = with(density) { 12.dp.toPx() }
            val paddingPx = with(density) { 20.dp.toPx() }
            val gridItemWidthPx = (screenWidthPx - paddingPx * 2f - spacingPx) / 2f
            
            val thumbnailRatio = if (isVideo) 16f / 9f else 1f
            val gridItemHeightPx = gridItemWidthPx / thumbnailRatio
            
            val rowSpacingPx = with(density) { 16.dp.toPx() }
            val gridStartY = with(density) { (80.dp + statusBarsDp).toPx() }
            
            // Text padding + height is approximately 46.dp
            val textContainerHeightPx = with(density) { 46.dp.toPx() }
            val rowHeightPx = gridItemHeightPx + textContainerHeightPx + rowSpacingPx

            val currentProgress = progress.value

            if (currentProgress < 1f || isClosing) {
                // Opening or Closing: background fade + flying cards animation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = currentProgress))
                ) {
                    // Header title and back button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .graphicsLayer { alpha = currentProgress },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable { dismissAction() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color(0xFFFA243C), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    // Render the flying cover images for the first 8 items
                    val referenceY = capturedCarouselBounds.values.firstOrNull { it.top > 0f }?.top
                        ?: (screenHeightPx / 2f)

                    val itemsToAnimate = items.take(8)
                    
                    // Find the index of the first visible item in the carousel among itemsToAnimate to determine direction for missing bounds
                    var firstVisibleIdx = -1
                    for (idx in itemsToAnimate.indices) {
                        val b = capturedCarouselBounds[itemsToAnimate[idx].id]
                        if (b != null && b.right > 0f && b.left < screenWidthPx) {
                            firstVisibleIdx = idx
                            break
                        }
                    }
                    if (firstVisibleIdx == -1) {
                        firstVisibleIdx = 0
                    }

                    itemsToAnimate.forEachIndexed { i, item ->
                        val startBounds = capturedCarouselBounds[item.id]
                        
                        val col = i % 2
                        val row = i / 2
                        val targetGridX = paddingPx + col * (gridItemWidthPx + spacingPx)
                        val targetGridY = gridStartY + row * rowHeightPx

                        val sourceX: Float
                        val sourceY: Float
                        val sourceW: Float
                        val sourceH: Float
                        val startCorner: Float

                        if (startBounds != null) {
                            sourceX = startBounds.left
                            sourceY = startBounds.top
                            sourceW = startBounds.width
                            sourceH = startBounds.height
                            startCorner = 12f
                        } else {
                            // If we don't have bounds, items before the first visible item slide left, and items after slide right
                            val isLeft = i < firstVisibleIdx
                            sourceW = gridItemWidthPx
                            sourceH = gridItemHeightPx
                            sourceX = if (isLeft) -sourceW else screenWidthPx
                            sourceY = referenceY
                            startCorner = 0f
                        }

                        // Interpolate coordinates
                        val curX = lerpFloat(sourceX, targetGridX, currentProgress)
                        val curY = lerpFloat(sourceY, targetGridY, currentProgress)
                        val curW = lerpFloat(sourceW, gridItemWidthPx, currentProgress).coerceAtLeast(0f)
                        val curH = lerpFloat(sourceH, gridItemHeightPx, currentProgress).coerceAtLeast(0f)
                        val curCorner = lerpFloat(startCorner, 12f, currentProgress).coerceAtLeast(0f)

                        val hdThumb = when (item) {
                            is AlbumItem -> item.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
                            is SongItem -> item.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
                            is PlaylistItem -> item.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(curX.roundToInt(), curY.roundToInt()) }
                                .size(with(density) { curW.toDp() }, with(density) { curH.toDp() })
                                .clip(RoundedCornerShape(curCorner.dp))
                                .background(Color.DarkGray)
                        ) {
                            if (hdThumb != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(hdThumb)
                                        .crossfade(false)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            } else {
                // Static content when animation is complete
                Box(modifier = Modifier.fillMaxSize()) {
                    content(dismissAction)
                }
            }
        }
    }
}
