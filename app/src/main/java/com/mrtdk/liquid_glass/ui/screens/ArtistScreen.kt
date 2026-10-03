package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInRoot
import com.mrtdk.liquid_glass.ui.components.LiquidButton
import com.mrtdk.glass.GlassContainer
import com.mrtdk.glass.GlassBox
import com.mrtdk.glass.DarkGrayGlassTint
import com.mrtdk.liquid_glass.ui.components.AppleMusicArtistMenu
import com.mrtdk.liquid_glass.ui.components.ArtistMenuInnerContent
import com.mrtdk.liquid_glass.ui.components.AppleMusicSongMenu
import com.mrtdk.liquid_glass.ui.components.ContextMenuSong
import android.os.Build
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import com.kyant.shapes.Capsule
import com.mrtdk.liquid_glass.ui.components.shapes.ContinuousCapsule
import com.mrtdk.liquid_glass.ui.components.shapes.ContinuousRoundedRectangle
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.ui.res.painterResource

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
import com.mrtdk.liquid_glass.ui.components.DetailBackPillButton
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import com.mrtdk.liquid_glass.ui.components.unclippedBoundsInRoot
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.Dp
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

@androidx.compose.runtime.Immutable
data class ArtistState(
    val id: String,
    val name: String,
    val thumbnail: String?
)

@androidx.compose.runtime.Immutable
data class LatestReleaseInfo(
    val title: String,
    val dateText: String,
    val songCountText: String,
    val thumbnail: String?,
    val id: String,
    val playlistId: String
)

/**
 * Adapts an artist background color to the dark theme according to Apple Music's OKLCH color engine.
 * If lightness or chroma exceed dark-theme legibility thresholds, it scales them down smoothly
 * (max lightness = 0.30, max chroma = 0.125), keeping hue and character intact.
 */
private fun adjustColorForDarkTheme(color: Color): Color {
    val r = color.red
    val g = color.green
    val b = color.blue

    fun srgbToLinear(c: Float): Double =
        if (c <= 0.04045f) c.toDouble() / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)

    fun linearToSrgb(c: Double): Float =
        (if (c <= 0.0031308) 12.92 * c else 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055).toFloat().coerceIn(0f, 1f)

    val lr = srgbToLinear(r)
    val lg = srgbToLinear(g)
    val lb = srgbToLinear(b)

    val l = Math.cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb)
    val m = Math.cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb)
    val s = Math.cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb)

    val L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val bVal = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s

    val C = Math.sqrt(a * a + bVal * bVal)
    val h = Math.atan2(bVal, a)

    val maxL = 0.30
    val maxC = 0.125

    if (L <= maxL && C <= maxC) {
        return color
    }

    val newL = Math.min(L, maxL)
    val newC = Math.min(C, maxC)

    val newA = newC * Math.cos(h)
    val newB = newC * Math.sin(h)

    val l_ = newL + 0.3963377774 * newA + 0.2158037573 * newB
    val m_ = newL - 0.1055613458 * newA - 0.0638541728 * newB
    val s_ = newL - 0.0894841775 * newA - 1.2914855480 * newB

    val lr_ = l_ * l_ * l_
    val lg_ = m_ * m_ * m_
    val lb_ = s_ * s_ * s_

    val rOut = +4.0767434770 * lr_ - 3.3077115913 * lg_ + 0.2309699292 * lb_
    val gOut = -1.2684380046 * lr_ + 2.6097574011 * lg_ - 0.3413193965 * lb_
    val bOut = -0.0041960863 * lr_ - 0.7034186147 * lg_ + 1.7076147010 * lb_

    return Color(
        red = linearToSrgb(rOut),
        green = linearToSrgb(gOut),
        blue = linearToSrgb(bOut),
        alpha = color.alpha
    )
}

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
    val isDarkMode by com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState()
    var artistPage by remember { mutableStateOf<ArtistPage?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var artistError by remember { mutableStateOf<String?>(null) }
    val isSaved by androidx.compose.runtime.produceState(initialValue = false, artistState.id) {
        LibraryManager.savedItems.collect { list ->
            value = list.any { it.id == artistState.id }
        }
    }
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
    var artistScreenRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var artistMenuPivotBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var activeSongForMenu by remember { mutableStateOf<ContextMenuSong?>(null) }
    var activeSongPivotBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
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
    val carouselLazyListStates = remember { mutableStateMapOf<String, LazyListState>() }

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


    val currentArtistName = artistPage?.artist?.title ?: artistState.name
    var appleMusicArtistThumb by remember(currentArtistName) { 
        mutableStateOf<String?>(
            com.mrtdk.liquid_glass.spotify.AppleMusicArtistProvider.getCachedImageUrl(currentArtistName)
                ?: artistState.thumbnail
        ) 
    }
    var artistLogoUrl by remember(currentArtistName) {
        mutableStateOf<String?>(
            com.mrtdk.liquid_glass.spotify.AppleMusicArtistProvider.getCachedLogoUrl(currentArtistName)
        )
    }
    var appleMusicBgColorHex by remember(currentArtistName) {
        mutableStateOf<String?>(
            com.mrtdk.liquid_glass.spotify.AppleMusicArtistProvider.getCachedBgColor(currentArtistName)
        )
    }
    var appleMusicInfoBgColorHex by remember(currentArtistName) {
        mutableStateOf<String?>(
            com.mrtdk.liquid_glass.spotify.AppleMusicArtistProvider.getCachedInfoBgColor(currentArtistName)
        )
    }
    var artistMotionVideoUrl by remember(currentArtistName) {
        mutableStateOf<String?>(
            com.mrtdk.liquid_glass.spotify.AppleMusicArtistProvider.getCachedVideoUrl(currentArtistName)
        )
    }
    var artistGenre by remember(currentArtistName) {
        mutableStateOf<String?>(
            com.mrtdk.liquid_glass.spotify.AppleMusicArtistProvider.getCachedGenre(currentArtistName)
        )
    }
    var artistFrom by remember(currentArtistName) {
        mutableStateOf<String?>(
            com.mrtdk.liquid_glass.spotify.AppleMusicArtistProvider.getCachedFrom(currentArtistName)
        )
    }
    var artistBorn by remember(currentArtistName) {
        mutableStateOf<String?>(
            com.mrtdk.liquid_glass.spotify.AppleMusicArtistProvider.getCachedBorn(currentArtistName)
        )
    }
    var artistBio by remember(currentArtistName) {
        mutableStateOf<String?>(
            com.mrtdk.liquid_glass.spotify.AppleMusicArtistProvider.getCachedBio(currentArtistName)
        )
    }

    val density = LocalDensity.current
    var mainHeroCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var mainLogoTopInHeroDp by remember { mutableStateOf<Dp?>(null) }
    var mainLogoHeightDp by remember { mutableStateOf<Dp?>(null) }

    LaunchedEffect(currentArtistName) {
        if (currentArtistName.isNotBlank()) {
            // 1. Unified Apple Music data fetch (studio portrait + official logo + background color + video + metadata)
            launch(Dispatchers.IO) {
                val data = com.mrtdk.liquid_glass.spotify.AppleMusicArtistProvider.getArtistData(currentArtistName)
                if (data != null) {
                    withContext(Dispatchers.Main) {
                        if (!data.imageUrl.isNullOrBlank()) {
                            appleMusicArtistThumb = data.imageUrl
                        }
                        if (!data.logoUrl.isNullOrBlank()) {
                            artistLogoUrl = data.logoUrl
                        }
                        if (!data.bgColorHex.isNullOrBlank()) {
                            appleMusicBgColorHex = data.bgColorHex
                        }
                        if (!data.infoBgColorHex.isNullOrBlank()) {
                            appleMusicInfoBgColorHex = data.infoBgColorHex
                        }
                        if (!data.videoUrl.isNullOrBlank()) {
                            artistMotionVideoUrl = data.videoUrl
                        }
                        if (!data.genre.isNullOrBlank()) {
                            artistGenre = data.genre
                        }
                        if (!data.from.isNullOrBlank()) {
                            artistFrom = data.from
                        }
                        if (!data.born.isNullOrBlank()) {
                            artistBorn = data.born
                        }
                        if (!data.bio.isNullOrBlank()) {
                            artistBio = data.bio
                        }
                    }
                }
            }
            // 2. Fetch motion video in parallel
            launch(Dispatchers.IO) {
                val motionUrl = com.mrtdk.liquid_glass.canvas.UnifiedCanvasProvider.getArtistMotionVideo(currentArtistName)
                if (!motionUrl.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        artistMotionVideoUrl = motionUrl
                    }
                }
            }
        }
    }

    // Use official Apple Music portrait with artistState.thumbnail as immediate fallback
    val artistThumb = appleMusicArtistThumb ?: artistState.thumbnail
    val hdThumb = artistThumb

    // Single fast API call — with fallback to search if browseId fails
    LaunchedEffect(artistState.id) {
        withContext(Dispatchers.IO) {
            val errors = mutableListOf<String>()
            val isChannelId = artistState.id.startsWith("UC") || artistState.id.startsWith("FE")
            
            // Only try direct artist browse API if it is an actual YouTube channel / browse ID
            if (isChannelId) {
                val result = YouTube.artist(artistState.id)
                if (result.isSuccess && result.getOrNull()?.sections?.isNotEmpty() == true) {
                    artistPage = result.getOrNull()
                } else {
                    val errVal = result.exceptionOrNull()
                    if (errVal != null) {
                        errors.add("Artist API error: ${errVal.localizedMessage ?: errVal.toString()}")
                    }
                }
            }
            
            // Fallback: search for the artist if direct browse wasn't attempted or failed
            if (artistPage == null) {
                // If the artist name has commas, ampersands, or semicolons, take the first one
                val firstArtistName = artistState.name
                    .split(",").firstOrNull()
                    ?.split("&")?.firstOrNull()
                    ?.split(";")?.firstOrNull()
                    ?.trim() ?: artistState.name
                
                val searchResult = YouTube.search(firstArtistName, YouTube.SearchFilter.FILTER_ARTIST)
                var foundArtist = searchResult.getOrNull()?.items?.filterIsInstance<com.echo.innertube.models.ArtistItem>()?.firstOrNull()

                // Fallback to searchSummary if FILTER_ARTIST returned nothing
                if (foundArtist == null) {
                    val summaryRes = YouTube.searchSummary(firstArtistName).getOrNull()
                    foundArtist = summaryRes?.summaries?.flatMap { it.items }?.filterIsInstance<com.echo.innertube.models.ArtistItem>()?.firstOrNull()
                }

                if (foundArtist != null && foundArtist.id != artistState.id) {
                    val result2 = YouTube.artist(foundArtist.id)
                    if (result2.isSuccess && result2.getOrNull()?.sections?.isNotEmpty() == true) {
                        artistPage = result2.getOrNull()
                        errors.clear() // Success on fallback, clear any previous browse errors
                    } else {
                        val errVal2 = result2.exceptionOrNull()
                        if (errVal2 != null) {
                            errors.add("Artist fallback error: ${errVal2.localizedMessage ?: errVal2.toString()}")
                        }
                    }
                }
                
                // If still nothing, build a page from search results
                if (artistPage == null) {
                    val songsResult = YouTube.search(firstArtistName, YouTube.SearchFilter.FILTER_SONG)
                    val albumsResult = YouTube.search(firstArtistName, YouTube.SearchFilter.FILTER_ALBUM)
                    
                    val songItems = songsResult.getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                    val songs = (songItems.filterNot { it.isVideoSong }.ifEmpty { songItems }).take(10)
                    val albums = albumsResult.getOrNull()?.items?.filterIsInstance<AlbumItem>() ?: emptyList()
                    
                    if (songs.isNotEmpty() || albums.isNotEmpty()) {
                        val sections = mutableListOf<ArtistSection>()
                        if (songs.isNotEmpty()) sections.add(ArtistSection(title = "Songs", items = songs, moreEndpoint = null))
                        if (albums.isNotEmpty()) sections.add(ArtistSection(title = "Albums", items = albums, moreEndpoint = null))
                        artistPage = ArtistPage(
                            artist = com.echo.innertube.models.ArtistItem(id = foundArtist?.id ?: artistState.id, title = artistState.name, thumbnail = artistState.thumbnail, shuffleEndpoint = null, radioEndpoint = null),
                            sections = sections,
                            description = null
                        )
                        errors.clear() // Successfully created fallback page
                    } else {
                        if (songsResult.isFailure) {
                            val sErr = songsResult.exceptionOrNull()
                            if (sErr != null) errors.add("Song search error: ${sErr.localizedMessage ?: sErr.toString()}")
                        }
                        if (albumsResult.isFailure) {
                            val aErr = albumsResult.exceptionOrNull()
                            if (aErr != null) errors.add("Album search error: ${aErr.localizedMessage ?: aErr.toString()}")
                        }
                    }
                }
            }
            if (artistPage == null && errors.isNotEmpty()) {
                artistError = errors.joinToString("\n")
            } else {
                artistError = null
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
                            val fallbackDesc = when (lang) {
                                "es" -> "Un álbum imprescindible en la discografía de ${artistState.name} que define su legado musical."
                                "pt" -> "Um álbum essencial na discografia de ${artistState.name} que define o seu legado musical."
                                "tr" -> "${artistState.name} diskografisinde müzikal mirasını tanımlayan temel bir albüm."
                                else -> "An essential album in the discography of ${artistState.name} that defines their musical legacy."
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

    val listState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50
        }
    }
    val isHeroOffscreen by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    val finalBackgroundColor = remember(appleMusicBgColorHex, dominantColor, artistState.name) {
        val baseColor = if (!appleMusicBgColorHex.isNullOrBlank()) {
            try {
                val parsed = android.graphics.Color.parseColor(appleMusicBgColorHex)
                Color(parsed)
            } catch (_: Exception) {
                if (dominantColor != Color.Unspecified) dominantColor else Color(0xFF111111)
            }
        } else if (artistState.name.lowercase().contains("billie")) {
            Color(0xFF061424) // Azul marino profundo de la imagen
        } else if (dominantColor != Color.Unspecified) {
            dominantColor
        } else {
            Color(0xFF111111)
        }
        adjustColorForDarkTheme(baseColor)
    }

    val infoCardBackgroundColor = remember(appleMusicInfoBgColorHex, dominantColor, artistState.name) {
        val baseColor = if (!appleMusicInfoBgColorHex.isNullOrBlank()) {
            try {
                val parsed = android.graphics.Color.parseColor(appleMusicInfoBgColorHex)
                Color(parsed)
            } catch (_: Exception) {
                Color(0xFF222B38)
            }
        } else if (dominantColor != Color.Unspecified) {
            dominantColor
        } else {
            Color(0xFF222B38)
        }
        adjustColorForDarkTheme(baseColor)
    }

    val animatedBackgroundColor by animateColorAsState(
        targetValue = finalBackgroundColor,
        animationSpec = tween(durationMillis = 400),
        label = "artistBgColor"
    )

    val animatedInfoBgColor by animateColorAsState(
        targetValue = infoCardBackgroundColor,
        animationSpec = tween(durationMillis = 400),
        label = "artistInfoBgColor"
    )

    GlassContainer(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { artistScreenRootCoords = it },
        useShader = true,
        content = {
            Box(modifier = Modifier.fillMaxSize().background(animatedBackgroundColor)) {
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
                val hasLatestRelease = latestRelease != null
                val heroHeight = if (hasLatestRelease) 665.dp else 555.dp
                val originalHeight = 440.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .onGloballyPositioned { mainHeroCoords = it }
                ) {
                    val hasMotionVideo = !artistMotionVideoUrl.isNullOrBlank()

                    if (!hasMotionVideo) {
                        // 1. IMAGEN SUPERIOR (Fondo base nítido completo, 100% sólido de 0 a 350dp, fundiéndose suavemente al reflejo)
                        val sharpTotalHeight = originalHeight + 20.dp
                        val sharpFadeStart = (350.dp / sharpTotalHeight).coerceIn(0f, 1f)
                        val sharpFadeMid = (410.dp / sharpTotalHeight).coerceIn(sharpFadeStart, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(sharpTotalHeight)
                                .align(Alignment.TopCenter)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.00f to Color.Black,
                                            sharpFadeStart to Color.Black,
                                            sharpFadeMid to Color.Black.copy(alpha = 0.50f),
                                            1.00f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        ) {
                            if (!hdThumb.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                    contentDescription = artistState.name,
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF222228),
                                                    Color(0xFF141416)
                                                )
                                            )
                                        )
                                )
                            }
                        }

                        // 1b. DIFUMINADO EN LA PARTE DE ABAJO DE LA IMAGEN SUPERIOR
                        val topDifStart = (310.dp / sharpTotalHeight).coerceIn(0f, 1f)
                        val topDifFull = (385.dp / sharpTotalHeight).coerceIn(0f, 1f)
                        val topDifFade = (430.dp / sharpTotalHeight).coerceIn(0f, 1f)

                        if (!hdThumb.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(sharpTotalHeight)
                                    .align(Alignment.TopCenter)
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0.00f to Color.Transparent,
                                                topDifStart to Color.Transparent,
                                                topDifFull to Color.Black,
                                                topDifFade to Color.Black.copy(alpha = 0.70f),
                                                1.00f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                renderEffect = android.graphics.RenderEffect
                                                    .createBlurEffect(22f, 22f, android.graphics.Shader.TileMode.MIRROR)
                                                    .asComposeRenderEffect()
                                            }
                                        }
                                        .then(
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                                Modifier.blur(14.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                                            } else Modifier
                                        )
                                )
                            }
                        }

                        // 1c. DIFUSIÓN HORIZONTAL EN LA PARTE DE ABAJO DE LA IMAGEN SUPERIOR
                        if (!hdThumb.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(sharpTotalHeight)
                                    .align(Alignment.TopCenter)
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0.00f to Color.Transparent,
                                                topDifStart to Color.Transparent,
                                                topDifFull to Color.Black.copy(alpha = 0.80f),
                                                topDifFade to Color.Black.copy(alpha = 0.55f),
                                                1.00f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = 1.04f
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                renderEffect = android.graphics.RenderEffect
                                                    .createBlurEffect(120f, 25f, android.graphics.Shader.TileMode.MIRROR)
                                                    .asComposeRenderEffect()
                                            }
                                        }
                                        .then(
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                                Modifier.blur(100.dp, 22.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                                            } else Modifier
                                        )
                                )
                            }
                        }

                        // 2. REFLEJO INVERTIDO (Donde termina la imagen original, estirado hacia abajo y disuelto suavemente)
                        val stretchFactor = 2.2f
                        val originY = stretchFactor / (1f + stretchFactor)
                        val reflOverlap = 30.dp
                        val reflFadeIn = (reflOverlap / originalHeight).coerceIn(0f, 1f)
                        val reflRemaining = 1f - reflFadeIn
                        val reflStop1 = reflFadeIn + (50.dp / originalHeight) * reflRemaining
                        val reflStop2 = reflFadeIn + (100.dp / originalHeight) * reflRemaining
                        val reflStop3 = reflFadeIn + (140.dp / originalHeight) * reflRemaining
                        val reflStop4 = reflFadeIn + (175.dp / originalHeight) * reflRemaining
                        val reflStopEnd = reflFadeIn + (210.dp / originalHeight) * reflRemaining

                        if (!hdThumb.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(originalHeight)
                                    .offset(y = originalHeight - reflOverlap)
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0.00f to Color.Transparent,
                                                (reflFadeIn * 0.5f) to Color.Black.copy(alpha = 0.50f),
                                                reflFadeIn to Color.Black,
                                                reflStop1 to Color.Black.copy(alpha = 0.88f),
                                                reflStop2 to Color.Black.copy(alpha = 0.65f),
                                                reflStop3 to Color.Black.copy(alpha = 0.35f),
                                                reflStop4 to Color.Black.copy(alpha = 0.12f),
                                                reflStopEnd to Color.Transparent,
                                                1.00f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleY = -stretchFactor
                                            transformOrigin = TransformOrigin(0.5f, originY)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                renderEffect = android.graphics.RenderEffect
                                                    .createBlurEffect(40f, 50f, android.graphics.Shader.TileMode.MIRROR)
                                                    .asComposeRenderEffect()
                                            }
                                        }
                                        .then(
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                                Modifier.blur(30.dp, 36.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                                            } else Modifier
                                        )
                                )
                            }
                        }

                        // 2b. CAPA DE DIFUMINADO HORIZONTAL DEL REFLEJO INVERTIDO (Donde termina la imagen original)
                        if (!hdThumb.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(originalHeight)
                                    .offset(y = originalHeight - reflOverlap)
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0.00f to Color.Transparent,
                                                (reflFadeIn * 0.5f) to Color.Black.copy(alpha = 0.35f),
                                                reflFadeIn to Color.Black.copy(alpha = 0.75f),
                                                reflStop1 to Color.Black.copy(alpha = 0.68f),
                                                reflStop2 to Color.Black.copy(alpha = 0.48f),
                                                reflStop3 to Color.Black.copy(alpha = 0.25f),
                                                reflStop4 to Color.Black.copy(alpha = 0.08f),
                                                reflStopEnd to Color.Transparent,
                                                1.00f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleY = -stretchFactor
                                            scaleX = 1.04f
                                            transformOrigin = TransformOrigin(0.5f, originY)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                renderEffect = android.graphics.RenderEffect
                                                    .createBlurEffect(80f, 40f, android.graphics.Shader.TileMode.MIRROR)
                                                    .asComposeRenderEffect()
                                            }
                                        }
                                        .then(
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                                Modifier.blur(55.dp, 28.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                                            } else Modifier
                                        )
                                )
                            }
                        }

                        // 3. DIFUMINADO UNIFICADOR DE COLOR PROGRESIVO SOBRE LA IMAGEN Y REFLEJO (Estilo Apple Music)
                        val fadeStart = (300.dp / heroHeight).coerceIn(0f, 1f)
                        val nameTop = (365.dp / heroHeight).coerceIn(fadeStart, 1f)
                        val nameCenter = (405.dp / heroHeight).coerceIn(nameTop, 1f)
                        val seamPos = (originalHeight / heroHeight).coerceIn(nameCenter, 1f) // 440dp
                        val buttonsCenter = (480.dp / heroHeight).coerceIn(seamPos, 1f)
                        val buttonsBottom = (515.dp / heroHeight).coerceIn(buttonsCenter, 1f)
                        val blendNearSolid = ((if (hasLatestRelease) 538.dp else 530.dp) / heroHeight).coerceIn(buttonsBottom, 1f)
                        val blendSolid = ((if (hasLatestRelease) 560.dp else 545.dp) / heroHeight).coerceIn(blendNearSolid, 1f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.00f to Color.Transparent,
                                            fadeStart to Color.Transparent,                                 // 300dp: imagen superior nítida
                                            nameTop to animatedBackgroundColor.copy(alpha = 0.20f),        // 365dp: inicio suave del velo de color
                                            nameCenter to animatedBackgroundColor.copy(alpha = 0.45f),     // 405dp: sobre el nombre del artista
                                            seamPos to animatedBackgroundColor.copy(alpha = 0.68f),        // 440dp: disuelve la unión con el reflejo de forma continua
                                            buttonsCenter to animatedBackgroundColor.copy(alpha = 0.84f),  // 480dp: sobre los botones de acción
                                            buttonsBottom to animatedBackgroundColor.copy(alpha = 0.92f),  // 515dp: transición continua sin cortes
                                            blendNearSolid to animatedBackgroundColor.copy(alpha = 0.98f), // 538dp: casi sólido al llegar a la tarjeta
                                            blendSolid to animatedBackgroundColor,                          // 560dp: 100% sólido unificado
                                            1.00f to animatedBackgroundColor
                                        )
                                    )
                                )
                        )
                    } else {
                        // DYNAMIC MOTION VIDEO (When artist has motion video, strictly NO static image underneath!)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(originalHeight + 60.dp)
                                .align(Alignment.TopCenter)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()

                                    // Apple Music curved difuminado: elliptical radial gradient mask
                                    // Arcs down gracefully in the center while curving up on the sides
                                    val curveBrush = Brush.radialGradient(
                                        colorStops = arrayOf(
                                            0.00f to Color.Black,
                                            0.42f to Color.Black,
                                            0.68f to Color.Black.copy(alpha = 0.85f),
                                            0.86f to Color.Black.copy(alpha = 0.40f),
                                            1.00f to Color.Transparent
                                        ),
                                        center = Offset(size.width / 2f, size.height * 0.12f),
                                        radius = size.height * 0.92f
                                    )
                                    drawRect(brush = curveBrush, blendMode = BlendMode.DstIn)

                                    // Linear bottom fade: ensures seamless dissolution into animatedBackgroundColor
                                    val verticalFade = Brush.verticalGradient(
                                        0.00f to Color.Black,
                                        0.50f to Color.Black,
                                        0.75f to Color.Black.copy(alpha = 0.85f),
                                        0.90f to Color.Black.copy(alpha = 0.35f),
                                        1.00f to Color.Transparent
                                    )
                                    drawRect(brush = verticalFade, blendMode = BlendMode.DstIn)
                                }
                        ) {
                            com.mrtdk.liquid_glass.ui.components.AnimatedArtworkPlayer(
                                videoUrl = artistMotionVideoUrl!!,
                                modifier = Modifier.fillMaxSize(),
                                enableFrameCapture = false,
                                isPaused = isHeroOffscreen || showAllAlbumsOverlay || showAllSongsOverlay || showAllSectionOverlay || showInfoOverlay
                            )
                        }
                    }

                    // Foreground: Artist Name/Logo + Action Buttons + Latest Release Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { logoCoords ->
                                    mainHeroCoords?.let { hero ->
                                        val localY = hero.localPositionOf(logoCoords, Offset.Zero).y
                                        if (localY > 0f) {
                                            mainLogoTopInHeroDp = with(density) { localY.toDp() }
                                            mainLogoHeightDp = with(density) { logoCoords.size.height.toDp() }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!artistLogoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(artistLogoUrl)
                                        .allowRgb565(false)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = artistState.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .heightIn(min = 44.dp, max = 80.dp)
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp)
                                )
                            } else {
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
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

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

                            val allTopSongs = topSongsSection?.items?.filterIsInstance<SongItem>().orEmpty()
                            val cleanTopSongs = allTopSongs.filterNot { it.isVideoSong }.ifEmpty { allTopSongs }
                            cleanTopSongs.firstOrNull()?.let { firstSong ->
                                IconButton(
                                    onClick = {
                                        val remainingSongs = cleanTopSongs.drop(1)
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

                        // Latest Release Card: directly under buttons, NO header text, matching Image 2
                        if (latestRelease != null) {
                            val release = latestRelease
                            Spacer(modifier = Modifier.height(30.dp))
                            var latestReleaseCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        val bounds = latestReleaseCoords?.unclippedBoundsInRoot()
                                        SharedTransitionState.lastOpenedSource = "latest_release"
                                        SharedTransitionState.lastClickBounds = bounds
                                        SharedTransitionState.lastOpenedId = release.id
                                        if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
                                            SharedTransitionState.carouselItemBounds[release.id] = bounds
                                        }
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
                                // Cover art (Apple Music mobile standard: 76.dp, 6.dp radius)
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .onGloballyPositioned { latestReleaseCoords = it }
                                        .sharedTransitionElement(release.id, source = "latest_release")
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF222222))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(release.thumbnail)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = release.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Date, Title, Song Count / Single
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = release.title,
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (release.dateText.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = release.dateText,
                                            color = Color.White.copy(alpha = 0.64f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                    if (release.songCountText.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = release.songCountText,
                                            color = Color.White.copy(alpha = 0.64f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                val isLatestReleaseSaved by androidx.compose.runtime.produceState(initialValue = false, release.id) {
                                    LibraryManager.savedItems.collect { list ->
                                        value = list.any { it.id == release.id }
                                    }
                                }

                                // Add / Save circular button
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .clickable {
                                            if (!isLatestReleaseSaved) {
                                                LibraryManager.saveItem(
                                                    LibraryItem(
                                                        id = release.id,
                                                        title = release.title,
                                                        subtitle = if (release.songCountText.contains("Single", true)) "Single" else "Album",
                                                        thumbnail = release.thumbnail,
                                                        type = ItemType.ALBUM
                                                    )
                                                )
                                                Toast.makeText(context, "Álbum guardado en la biblioteca", Toast.LENGTH_SHORT).show()
                                            } else {
                                                LibraryManager.removeItem(release.id)
                                                Toast.makeText(context, "Eliminado de la biblioteca", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isLatestReleaseSaved) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = if (isLatestReleaseSaved) "Saved" else "Add",
                                        tint = Color.White.copy(alpha = 0.95f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }


            // ── LOADING ────────────────────────────────────
            if (isLoading) {
                item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
            }


            // ── TOP SONGS ──────────────────────────────────
            if (topSongsSection != null) {
                val allTop = topSongsSection.items.filterIsInstance<SongItem>()
                val songs = (allTop.filterNot { it.isVideoSong }.ifEmpty { allTop }).take(4)
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
                        Text(stringResource(R.string.top_songs), color = Color.White.copy(alpha = 0.95f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Ver todo",
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                // Vertical song list (4 songs max)
                items(
                    count = songs.size,
                    key = { index -> songs[index].id.ifEmpty { "$index" } },
                    contentType = { "artist_song" }
                ) { index ->
                    val song = songs[index]
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Keyline divider between tracks (Apple Music style, starts at text offset)
                        if (index > 0) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 84.dp)
                                    .height(0.5.dp)
                                    .background(Color.White.copy(alpha = 0.12f))
                            )
                        }
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
                                Text(song.title, color = Color.White.copy(alpha = 0.95f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artists.joinToString { it.name }, color = Color.White.copy(alpha = 0.64f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            var songDotsCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            val dotsInteractionSource = remember { MutableInteractionSource() }
                            val isDotsPressed by dotsInteractionSource.collectIsPressedAsState()
                            val dotsPressScale by animateFloatAsState(
                                targetValue = if (isDotsPressed) 0.86f else 1f,
                                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                                label = "topSongDotsPress"
                            )
                            val isThisSongActive = activeSongForMenu?.id == song.id

                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { songDotsCoords = it }
                                    .size(36.dp)
                                    .graphicsLayer {
                                        scaleX = dotsPressScale
                                        scaleY = dotsPressScale
                                        alpha = if (isThisSongActive) 0f else 1f
                                    }
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = dotsInteractionSource,
                                        indication = null,
                                        enabled = !isThisSongActive
                                    ) {
                                        val rootCoords = artistScreenRootCoords
                                        if (rootCoords != null && songDotsCoords != null && rootCoords.isAttached && songDotsCoords!!.isAttached) {
                                            val localOffset = rootCoords.localPositionOf(songDotsCoords!!, Offset.Zero)
                                            val size = songDotsCoords!!.size
                                            activeSongPivotBounds = Rect(localOffset, Size(size.width.toFloat(), size.height.toFloat()))
                                        } else {
                                            activeSongPivotBounds = songDotsCoords?.boundsInRoot()
                                        }
                                        activeSongForMenu = ContextMenuSong(
                                            id = song.id,
                                            title = song.title,
                                            artist = song.artists.joinToString { it.name },
                                            thumbnail = song.thumbnail,
                                            album = song.album?.name,
                                            artistId = song.artists.firstOrNull()?.id ?: artistState.id,
                                            albumId = song.album?.id
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MoreHoriz,
                                    null,
                                    tint = Color.White.copy(alpha = 0.64f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
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
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    items(
                        count = essentialsItems.size,
                        key = { index -> essentialsItems[index].id.ifEmpty { "$index" } },
                        contentType = { "artist_essential" }
                    ) { index ->
                        val album = essentialsItems[index]
                        val albumDescription = essentialsDescriptions[album.id] ?: getAlbumDescription(artistState.name, album.title)
                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val bounds = imageCoords?.unclippedBoundsInRoot()
                                    SharedTransitionState.lastOpenedSource = "essentials"
                                    SharedTransitionState.lastClickBounds = bounds
                                    SharedTransitionState.lastOpenedId = album.id
                                    if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
                                        SharedTransitionState.carouselItemBounds[album.id] = bounds
                                    }
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
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .onGloballyPositioned { coords ->
                                            imageCoords = coords
                                        }
                                        .sharedTransitionElement(album.id, source = "essentials")
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(album.thumbnail)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = album.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Album details: Title and Description
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = album.title,
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = albumDescription,
                                        color = Color.White.copy(alpha = 0.64f),
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
                                    tint = Color.White.copy(alpha = 0.45f),
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
                                        .height(0.5.dp)
                                        .background(Color.White.copy(alpha = 0.12f))
                                )
                            }
                        }
                    }
                }
            }

            // ── ALBUMS ─────────────────────────────────────
            if (albumsSection != null) {
                val albumItems = albumsSection.items.filterIsInstance<AlbumItem>()
                val lazyRowState = carouselLazyListStates.getOrPut(albumsSection.title) { LazyListState() }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    val isClickable = albumItems.size > 4 || albumsSection.moreEndpoint != null
                    val coroutineScope = rememberCoroutineScope()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isClickable) {
                                    Modifier.clickable {
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
                                    }
                                } else Modifier
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.albumes), color = Color.White.copy(alpha = 0.95f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (isClickable) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Ver todo",
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    LazyRow(
                        state = lazyRowState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(albumItems.take(8), key = { "artist_album_${it.id}" }) { item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, scrollState = listState)
                        }
                    }
                }
            }

            // ── SINGLES Y EP ───────────────────────────────
            if (singlesSection != null) {
                val singleItems = singlesSection.items.filterIsInstance<AlbumItem>()
                val lazyRowState = carouselLazyListStates.getOrPut(singlesSection.title) { LazyListState() }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    val isClickable = singleItems.size > 4 || singlesSection.moreEndpoint != null
                    val coroutineScope = rememberCoroutineScope()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isClickable) {
                                    Modifier.clickable {
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
                                    }
                                } else Modifier
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.sencillos_y_ep), color = Color.White.copy(alpha = 0.95f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (isClickable) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Ver todo",
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    LazyRow(
                        state = lazyRowState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(singleItems.take(8), key = { "artist_single_${it.id}" }) { item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, scrollState = listState)
                        }
                    }
                }
            }

            // ── VIDEOS ─────────────────────────────────────
            if (videosSection != null) {
                val videoItems = videosSection.items.filterIsInstance<SongItem>()
                val lazyRowState = carouselLazyListStates.getOrPut(videosSection.title) { LazyListState() }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    val isClickable = videoItems.size > 4 || videosSection.moreEndpoint != null
                    val coroutineScope = rememberCoroutineScope()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isClickable) {
                                    Modifier.clickable {
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
                                    }
                                } else Modifier
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.videos), color = Color.White.copy(alpha = 0.95f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (isClickable) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Ver todo",
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    LazyRow(
                        state = lazyRowState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(videoItems, key = { "artist_video_${it.id}" }) { item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, onVideoSelected = onVideoSelected, isVideo = true, scrollState = listState)
                        }
                    }
                }
            }

            // ── DESTACADO EN ───────────────────────────────
            if (featuredSection != null && featuredSection.items.isNotEmpty()) {
                val lazyRowState = carouselLazyListStates.getOrPut(featuredSection.title) { LazyListState() }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.destacado_en), color = Color.White.copy(alpha = 0.95f), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    LazyRow(
                        state = lazyRowState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(featuredSection.items, key = { "artist_featured_${it.id}" }) { item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, scrollState = listState)
                        }
                    }
                }
            }

            // ── PLAYLISTS ──────────────────────────────────
            if (playlistsSection != null) {
                val plItems = playlistsSection.items.filterIsInstance<PlaylistItem>()
                val lazyRowState = carouselLazyListStates.getOrPut(playlistsSection.title) { LazyListState() }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    val isClickable = plItems.size > 4 || playlistsSection.moreEndpoint != null
                    val coroutineScope = rememberCoroutineScope()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isClickable) {
                                    Modifier.clickable {
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
                                    }
                                } else Modifier
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.playlists), color = Color.White.copy(alpha = 0.95f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (isClickable) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Ver todo",
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    LazyRow(
                        state = lazyRowState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(plItems.take(8), key = { "artist_pl_${it.id}" }) { item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, scrollState = listState)
                        }
                    }
                }
            }

            // ── PUEDE QUE TAMBIÉN TE GUSTE ─────────────────
            if (fansSection != null) {
                val relatedArtists = fansSection.items.filterIsInstance<ArtistItem>()
                if (relatedArtists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(stringResource(R.string.puede_gustar), color = Color.White.copy(alpha = 0.95f), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(relatedArtists.size, key = { idx -> "artist_rel_${relatedArtists[idx].id}" }) { idx ->
                                val ra = relatedArtists[idx]
                                val raThumb = ra.thumbnail?.replace("=w226-h226", "=w400-h400")?.replace("=w120-h120", "=w400-h400")
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp).clickable {
                                    val spThumb = com.mrtdk.liquid_glass.spotify.SpotifyArtistProvider.getCachedArtistImageUrl(ra.title)
                                    onArtistSelected(ArtistState(id = ra.id, name = ra.title, thumbnail = spThumb ?: raThumb))
                                }) {
                                    com.mrtdk.liquid_glass.spotify.SpotifyArtistAvatar(
                                        artistName = ra.title,
                                        fallbackUrl = raThumb,
                                        contentDescription = ra.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.DarkGray)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(ra.title, color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // Also show any remaining sections not matched above
            sections.filter { it != topSongsSection && it != albumsSection && it != singlesSection && it != videosSection && it != featuredSection && it != playlistsSection && it != fansSection }.forEach { section ->
                val isVideoSection = section.title.contains("video", true) || section.title.contains("vídeo", true) || section.title.contains("presentacion", true) || section.title.contains("live", true) || section.title.contains("vivo", true) || section.title.contains("concierto", true)
                val lazyRowState = carouselLazyListStates.getOrPut(section.title) { LazyListState() }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    val isClickable = section.items.size > 4 || section.moreEndpoint != null
                    val coroutineScope = rememberCoroutineScope()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isClickable) {
                                    Modifier.clickable {
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
                                    }
                                } else Modifier
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(section.title, color = Color.White.copy(alpha = 0.95f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (isClickable) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Ver todo",
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    LazyRow(
                        state = lazyRowState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(section.items) { item ->
                            ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, onVideoSelected = onVideoSelected, isVideo = isVideoSection, scrollState = listState)
                        }
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
                    .background(animatedInfoBgColor)
            ) {
                val infoScrollState = rememberScrollState()
                val hasLatestRelease = latestRelease != null
                val logoTop = mainLogoTopInHeroDp ?: (if (hasLatestRelease) 375.dp else 410.dp)
                val logoHeight = mainLogoHeightDp ?: 56.dp
                val originalHeight = 440.dp
                val heroHeight = 520.dp

                // Full-width artist hero backdrop with complete reflection & diffusion effect (matching main artist view)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .graphicsLayer {
                            translationY = -infoScrollState.value.toFloat()
                        }
                        .clipToBounds()
                ) {
                    // 1. IMAGEN SUPERIOR (Fondo base nítido completo, 100% sólido de 0 a 350dp, fundiéndose suavemente al reflejo)
                    val sharpTotalHeight = originalHeight + 20.dp
                    val sharpFadeStart = (350.dp / sharpTotalHeight).coerceIn(0f, 1f)
                    val sharpFadeMid = (410.dp / sharpTotalHeight).coerceIn(sharpFadeStart, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(sharpTotalHeight)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.00f to Color.Black,
                                        sharpFadeStart to Color.Black,
                                        sharpFadeMid to Color.Black.copy(alpha = 0.50f),
                                        1.00f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    ) {
                        if (!hdThumb.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                contentDescription = artistState.name,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF222228),
                                                Color(0xFF141416)
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // 1b. DIFUMINADO EN LA PARTE DE ABAJO DE LA IMAGEN SUPERIOR
                    val topDifStart = (310.dp / sharpTotalHeight).coerceIn(0f, 1f)
                    val topDifFull = (385.dp / sharpTotalHeight).coerceIn(0f, 1f)
                    val topDifFade = (430.dp / sharpTotalHeight).coerceIn(0f, 1f)

                    if (!hdThumb.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(sharpTotalHeight)
                                .align(Alignment.TopCenter)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.00f to Color.Transparent,
                                        topDifStart to Color.Transparent,
                                        topDifFull to Color.Black,
                                        topDifFade to Color.Black.copy(alpha = 0.70f),
                                        1.00f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            renderEffect = android.graphics.RenderEffect
                                                .createBlurEffect(22f, 22f, android.graphics.Shader.TileMode.MIRROR)
                                                .asComposeRenderEffect()
                                        }
                                    }
                                    .then(
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                            Modifier.blur(14.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                                        } else Modifier
                                    )
                            )
                        }
                    }

                    // 1c. DIFUSIÓN HORIZONTAL EN LA PARTE DE ABAJO DE LA IMAGEN SUPERIOR
                    if (!hdThumb.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(sharpTotalHeight)
                                .align(Alignment.TopCenter)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.00f to Color.Transparent,
                                            topDifStart to Color.Transparent,
                                            topDifFull to Color.Black.copy(alpha = 0.80f),
                                            topDifFade to Color.Black.copy(alpha = 0.55f),
                                            1.00f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = 1.04f
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            renderEffect = android.graphics.RenderEffect
                                                .createBlurEffect(120f, 25f, android.graphics.Shader.TileMode.MIRROR)
                                                .asComposeRenderEffect()
                                        }
                                    }
                                    .then(
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                            Modifier.blur(100.dp, 22.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                                        } else Modifier
                                    )
                            )
                        }
                    }

                    // 2. REFLEJO INVERTIDO (Donde termina la imagen original, estirado hacia abajo y disuelto suavemente)
                    val stretchFactor = 2.2f
                    val originY = stretchFactor / (1f + stretchFactor)
                    val reflOverlap = 30.dp
                    val reflFadeIn = (reflOverlap / originalHeight).coerceIn(0f, 1f)
                    val reflRemaining = 1f - reflFadeIn
                    val reflStop1 = reflFadeIn + (50.dp / originalHeight) * reflRemaining
                    val reflStop2 = reflFadeIn + (100.dp / originalHeight) * reflRemaining
                    val reflStop3 = reflFadeIn + (140.dp / originalHeight) * reflRemaining
                    val reflStop4 = reflFadeIn + (175.dp / originalHeight) * reflRemaining
                    val reflStopEnd = reflFadeIn + (210.dp / originalHeight) * reflRemaining

                    if (!hdThumb.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(originalHeight)
                                .offset(y = originalHeight - reflOverlap)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.00f to Color.Transparent,
                                            (reflFadeIn * 0.5f) to Color.Black.copy(alpha = 0.50f),
                                            reflFadeIn to Color.Black,
                                            reflStop1 to Color.Black.copy(alpha = 0.88f),
                                            reflStop2 to Color.Black.copy(alpha = 0.65f),
                                            reflStop3 to Color.Black.copy(alpha = 0.35f),
                                            reflStop4 to Color.Black.copy(alpha = 0.12f),
                                            reflStopEnd to Color.Transparent,
                                            1.00f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleY = -stretchFactor
                                        transformOrigin = TransformOrigin(0.5f, originY)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            renderEffect = android.graphics.RenderEffect
                                                .createBlurEffect(40f, 50f, android.graphics.Shader.TileMode.MIRROR)
                                                .asComposeRenderEffect()
                                        }
                                    }
                                    .then(
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                            Modifier.blur(30.dp, 36.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                                        } else Modifier
                                    )
                            )
                        }
                    }

                    // 2b. CAPA DE DIFUMINADO HORIZONTAL DEL REFLEJO INVERTIDO (Donde termina la imagen original)
                    if (!hdThumb.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(originalHeight)
                                .offset(y = originalHeight - reflOverlap)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.00f to Color.Transparent,
                                            (reflFadeIn * 0.5f) to Color.Black.copy(alpha = 0.35f),
                                            reflFadeIn to Color.Black.copy(alpha = 0.75f),
                                            reflStop1 to Color.Black.copy(alpha = 0.68f),
                                            reflStop2 to Color.Black.copy(alpha = 0.48f),
                                            reflStop3 to Color.Black.copy(alpha = 0.25f),
                                            reflStop4 to Color.Black.copy(alpha = 0.08f),
                                            reflStopEnd to Color.Transparent,
                                            1.00f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleY = -stretchFactor
                                        scaleX = 1.04f
                                        transformOrigin = TransformOrigin(0.5f, originY)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            renderEffect = android.graphics.RenderEffect
                                                .createBlurEffect(80f, 40f, android.graphics.Shader.TileMode.MIRROR)
                                                .asComposeRenderEffect()
                                        }
                                    }
                                    .then(
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                            Modifier.blur(55.dp, 28.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                                        } else Modifier
                                    )
                            )
                        }
                    }

                    // 3. DIFUMINADO UNIFICADOR DE COLOR PROGRESIVO SOBRE LA IMAGEN Y REFLEJO (Estilo Apple Music)
                    val fadeStart = (300.dp / heroHeight).coerceIn(0f, 1f)
                    val nameTop = (365.dp / heroHeight).coerceIn(fadeStart, 1f)
                    val nameCenter = (405.dp / heroHeight).coerceIn(nameTop, 1f)
                    val seamPos = (originalHeight / heroHeight).coerceIn(nameCenter, 1f) // 440dp
                    val reflMid = (475.dp / heroHeight).coerceIn(seamPos, 1f)
                    val blendNearSolid = (500.dp / heroHeight).coerceIn(reflMid, 1f)
                    val blendSolid = (515.dp / heroHeight).coerceIn(blendNearSolid, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Transparent,
                                        fadeStart to Color.Transparent,                                 // 300dp: imagen superior nítida
                                        nameTop to animatedInfoBgColor.copy(alpha = 0.20f),             // 365dp: inicio suave del velo de color
                                        nameCenter to animatedInfoBgColor.copy(alpha = 0.45f),          // 405dp: sobre el nombre del artista
                                        seamPos to animatedInfoBgColor.copy(alpha = 0.68f),             // 440dp: disuelve la unión con el reflejo de forma continua
                                        reflMid to animatedInfoBgColor.copy(alpha = 0.84f),             // 475dp: transición continua sin cortes
                                        blendNearSolid to animatedInfoBgColor.copy(alpha = 0.98f),      // 500dp: casi sólido
                                        blendSolid to animatedInfoBgColor,                              // 515dp: 100% sólido unificado
                                        1.00f to animatedInfoBgColor
                                    )
                                )
                            )
                    )
                }

                // Foreground scrollable content (continuous flow matching user screenshot)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(infoScrollState)
                ) {
                    // Foreground: Artist Name / Logo overlay matching main view position & alignment
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = logoTop)
                            .height(logoHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!artistLogoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(artistLogoUrl)
                                    .allowRgb565(false)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = artistState.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .heightIn(min = 44.dp, max = 80.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                            )
                        } else {
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
                        }
                    }

                    // Foreground: From and Born (placed right below the artist name)
                    val displayFrom = artistFrom ?: metadata.from
                    val displayBorn = artistBorn ?: metadata.born
                    if (!displayFrom.isNullOrBlank() || !displayBorn.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 16.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            if (!displayFrom.isNullOrBlank()) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "From",
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = displayFrom,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (!displayBorn.isNullOrBlank()) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Born",
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = displayBorn,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Genre (Pill chip, title-cased label)
                    val displayGenre = artistGenre ?: metadata.genres.firstOrNull() ?: "Pop"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 14.dp)
                    ) {
                        Text(
                            text = "Genre",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.14f))
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = displayGenre,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // About section
                    val displayBio = artistBio ?: artistPage?.description
                    if (!displayBio.isNullOrBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 22.dp, bottom = 120.dp)
                        ) {
                            Text(
                                text = "About",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = displayBio,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 15.sp,
                                lineHeight = 23.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Botón atrás flotante en la esquina superior izquierda
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 8.dp)
                ) {
                    DetailBackPillButton(
                        isDarkMode = isDarkMode,
                        onClick = { showInfoOverlay = false }
                    )
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
            Box(modifier = Modifier.fillMaxSize().background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(modifier = Modifier.statusBarsPadding().height(16.dp)) }
                    // Pill back button
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            DetailBackPillButton(isDarkMode = isDarkMode, onClick = dismiss)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(stringResource(R.string.albumes), color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
            Box(modifier = Modifier.fillMaxSize().background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(modifier = Modifier.statusBarsPadding().height(16.dp)) }
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            DetailBackPillButton(isDarkMode = isDarkMode, onClick = dismiss)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(allSectionTitle, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
            Box(modifier = Modifier.fillMaxSize().background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(modifier = Modifier.statusBarsPadding().height(16.dp)) }
                    // Pill back button
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            DetailBackPillButton(isDarkMode = isDarkMode, onClick = { showAllSongsOverlay = false })
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(stringResource(R.string.top_songs), color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    // Vertical list of all songs
                    items(allSongs.size) { index ->
                        val song = allSongs[index]
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (index > 0) {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 96.dp)
                                        .height(0.5.dp)
                                        .background(if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f))
                                )
                            }
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
                                Text("${index + 1}", color = if (isDarkMode) Color.White.copy(alpha = 0.64f) else Color.Black.copy(alpha = 0.64f), fontSize = 16.sp, modifier = Modifier.width(36.dp))
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(song.thumbnail).crossfade(true).build(),
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, color = if (isDarkMode) Color.White.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.95f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artists.joinToString { it.name }, color = if (isDarkMode) Color.White.copy(alpha = 0.64f) else Color.Black.copy(alpha = 0.64f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                var songDotsCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                                val dotsInteractionSource = remember { MutableInteractionSource() }
                                val isDotsPressed by dotsInteractionSource.collectIsPressedAsState()
                                val dotsPressScale by animateFloatAsState(
                                    targetValue = if (isDotsPressed) 0.86f else 1f,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                                    label = "allSongDotsPress"
                                )
                                val isThisSongActive = activeSongForMenu?.id == song.id

                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { songDotsCoords = it }
                                        .size(36.dp)
                                        .graphicsLayer {
                                            scaleX = dotsPressScale
                                            scaleY = dotsPressScale
                                            alpha = if (isThisSongActive) 0f else 1f
                                        }
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = dotsInteractionSource,
                                            indication = null,
                                            enabled = !isThisSongActive
                                        ) {
                                            val rootCoords = artistScreenRootCoords
                                            if (rootCoords != null && songDotsCoords != null && rootCoords.isAttached && songDotsCoords!!.isAttached) {
                                                val localOffset = rootCoords.localPositionOf(songDotsCoords!!, Offset.Zero)
                                                val size = songDotsCoords!!.size
                                                activeSongPivotBounds = Rect(localOffset, Size(size.width.toFloat(), size.height.toFloat()))
                                            } else {
                                                activeSongPivotBounds = songDotsCoords?.boundsInRoot()
                                            }
                                            activeSongForMenu = ContextMenuSong(
                                                id = song.id,
                                                title = song.title,
                                                artist = song.artists.joinToString { it.name },
                                                thumbnail = song.thumbnail,
                                                album = song.album?.name,
                                                artistId = song.artists.firstOrNull()?.id ?: artistState.id,
                                                albumId = song.album?.id
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.tres_puntos),
                                        contentDescription = "More",
                                        tint = if (isDarkMode) Color.White.copy(alpha = 0.64f) else Color.Black.copy(alpha = 0.64f),
                                        modifier = Modifier.width(20.dp).height(16.dp)
                                    )
                                }
                            }
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
            // Semi-transparent overlay to dismiss morphing menu when clicking outside
            if (showArtistMenu) {
                androidx.activity.compose.BackHandler {
                    showArtistMenu = false
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showArtistMenu = false }
                )
            }

            // ── FLOATING TOP BAR ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val isSolid = com.mrtdk.glass.LocalGlassStyle.current == "solid" || LibraryManager.getGlassStyle() == "solid"
                val artistHeaderIconTint = if (isSolid || isDarkMode) Color.White else Color(0xFF151515)
                // Circular back button with GlassBox (liquid glass)
                scope.GlassBox(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(48.dp)
                        .graphicsLayer {
                            alpha = if (showArtistMenu) 0.4f else 1f
                        }
                        .clickable(enabled = !showArtistMenu) { onBack() },
                    shape = CircleShape,
                    tint = Color.Unspecified,
                    blur = 0.8f,
                    centerDistortion = 0.1f,
                    scale = 0.02f,
                    warpEdges = 0.4f,
                    elevation = 16.dp,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.flecha_atras),
                        contentDescription = "Back",
                        tint = artistHeaderIconTint,
                        modifier = Modifier.size(20.dp).offset(x = (-1).dp)
                    )
                }

                // Morphing Liquid Glass Pill -> Menu on Top-Right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp)
                ) {
                    ArtistTopRightMorphingPill(
                        glassScope = scope,
                        artistState = artistState,
                        artistThumb = hdThumb,
                        isExpanded = showArtistMenu,
                        onExpandChange = { showArtistMenu = it },
                        glassIconTint = artistHeaderIconTint,
                        topSongs = topSongsSection?.items?.filterIsInstance<SongItem>()?.let { l -> l.filterNot { it.isVideoSong }.ifEmpty { l } } ?: emptyList(),
                        onSongSelected = onSongSelected
                    )
                }
            }
        }

        activeSongForMenu?.let { cSong ->
            AppleMusicSongMenu(
                song = cSong,
                onDismiss = {
                    activeSongForMenu = null
                    activeSongPivotBounds = null
                },
                onGoToArtist = null,
                onGoToAlbum = if (cSong.albumId != null && cSong.album != null) {
                    {
                        onAlbumSelected(
                            AlbumState(
                                id = cSong.albumId,
                                playlistId = cSong.albumId,
                                title = cSong.album,
                                artist = cSong.artist,
                                thumbnail = null
                            )
                        )
                    }
                } else null,
                onSongSelected = onSongSelected,
                pivotBounds = activeSongPivotBounds
            )
        }

        if (artistError != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { artistError = null },
                title = {
                    Text(
                        text = "Error al cargar artista",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "No se pudo cargar la información del artista. Por favor, toma una captura de pantalla de este error para enviársela al desarrollador:",
                            color = Color.Gray,
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
                                text = artistError ?: "",
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
                            val clip = android.content.ClipData.newPlainText("Error RayMusic", artistError)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Copiado al portapapeles", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Copiar", color = Color(0xFFE91E63))
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { artistError = null }) {
                        Text("Cerrar", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E1E1E),
                textContentColor = Color.White
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
                    val bounds = imageCoords?.unclippedBoundsInRoot()
                    SharedTransitionState.lastOpenedSource = "carousel"
                    SharedTransitionState.lastClickBounds = bounds
                    SharedTransitionState.lastOpenedId = item.id
                    if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
                        SharedTransitionState.carouselItemBounds[item.id] = bounds
                    }
                    onAlbumSelected(AlbumState(id = item.id, playlistId = item.playlistId ?: item.id, title = item.title, artist = item.artists?.joinToString { it.name } ?: artistName, thumbnail = item.thumbnail, year = item.year as? Int ?: item.year?.toString()?.toIntOrNull()))
                }
            ) {
                Box(modifier = imgMod
                    .onGloballyPositioned { coords ->
                        imageCoords = coords
                        if (!fillWidth) {
                            val bounds = coords.unclippedBoundsInRoot()
                            if (bounds.width > 0f && bounds.height > 0f) {
                                SharedTransitionState.carouselItemBounds[item.id] = bounds
                            }
                        }
                    }
                    .let { if (!fillWidth) it.sharedTransitionElement(item.id) else it }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161618))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(hdThumb).size(360).crossfade(true).build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.title, color = Color.White.copy(alpha = 0.95f), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.year ?: ""}", color = Color.White.copy(alpha = 0.64f), fontSize = 13.sp)
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
                            val bounds = coords.unclippedBoundsInRoot()
                            if (bounds.width > 0f && bounds.height > 0f) {
                                SharedTransitionState.carouselItemBounds[item.id] = bounds
                            }
                        }
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161618))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(hdThumb).size(360).crossfade(true).build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.title, color = Color.White.copy(alpha = 0.95f), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artists.joinToString { it.name }, color = Color.White.copy(alpha = 0.64f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        is PlaylistItem -> {
            val hdThumb = item.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
            var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Column(modifier = cardMod
                .let { if (scrollState != null) it.wiggleOnScroll(item.id, lazyListState = scrollState) else it }
                .clickable {
                    val bounds = imageCoords?.unclippedBoundsInRoot()
                    SharedTransitionState.lastOpenedSource = "carousel"
                    SharedTransitionState.lastClickBounds = bounds
                    SharedTransitionState.lastOpenedId = item.id
                    if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
                        SharedTransitionState.carouselItemBounds[item.id] = bounds
                    }
                    onAlbumSelected(AlbumState(id = item.id, playlistId = item.id, title = item.title, artist = item.author?.name ?: artistName, thumbnail = item.thumbnail, year = null))
                }
            ) {
                Box(modifier = imgMod
                    .onGloballyPositioned { coords ->
                        imageCoords = coords
                        if (!fillWidth) {
                            val bounds = coords.unclippedBoundsInRoot()
                            if (bounds.width > 0f && bounds.height > 0f) {
                                SharedTransitionState.carouselItemBounds[item.id] = bounds
                            }
                        }
                    }
                    .let { if (!fillWidth) it.sharedTransitionElement(item.id) else it }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161618))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(hdThumb).size(360).crossfade(true).build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.title, color = Color.White.copy(alpha = 0.95f), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.author?.name ?: stringResource(R.string.playlists), color = Color.White.copy(alpha = 0.64f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        is ArtistItem -> {
            val hdThumb = item.thumbnail?.replace("=w226-h226", "=w400-h400")?.replace("=w120-h120", "=w400-h400")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = cardMod.clickable { 
                val spThumb = com.mrtdk.liquid_glass.spotify.SpotifyArtistProvider.getCachedArtistImageUrl(item.title)
                onArtistSelected(ArtistState(item.id, item.title, spThumb ?: hdThumb)) 
            }) {
                com.mrtdk.liquid_glass.spotify.SpotifyArtistAvatar(
                    artistName = item.title,
                    fallbackUrl = hdThumb,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(if (fillWidth) 160.dp else 120.dp).clip(CircleShape).background(Color.DarkGray)
                )
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
        "metal", "punk", "electronic", "dance", "techno", "indie", "alternative", "folk", "latin",
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

    val scope = rememberCoroutineScope()
    var isOverlayVisible by remember { mutableStateOf(visible) }
    var isClosing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            isOverlayVisible = true
            isClosing = false
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 190f
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
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 300f
                        )
                    )
                    isOverlayVisible = false
                    onClose()
                }
            }
            Unit
        }
    }

    // Intercept back button
    androidx.activity.compose.BackHandler(enabled = isOverlayVisible && !isClosing) {
        dismissAction()
    }

    if (isOverlayVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Al entrar: progress 0 -> 1 => translationX: width -> 0 (de derecha a izquierda)
                    // Al salir: progress 1 -> 0 => translationX: 0 -> width (de izquierda a derecha)
                    translationX = (1f - progress.value) * size.width
                }
        ) {
            content(dismissAction)
        }
    }
}

@Composable
fun ArtistTopRightMorphingPill(
    glassScope: com.mrtdk.glass.GlassBoxScope,
    artistState: ArtistState,
    artistThumb: String?,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    glassIconTint: Color,
    topSongs: List<com.echo.innertube.models.SongItem>,
    onSongSelected: (PlayerState) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val savedItems by LibraryManager.savedItems.collectAsState()
    val isFavorite = remember(savedItems, artistState.id) { savedItems.any { it.id == artistState.id } }

    val morphProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "artistPillToMenuMorph"
    )

    // Smooth continuous 2D size & corner interpolation
    val targetMenuHeight = 350.dp
    val morphWidth = androidx.compose.ui.unit.lerp(124.dp, 268.dp, morphProgress)
    val morphHeight = androidx.compose.ui.unit.lerp(44.dp, targetMenuHeight, morphProgress)
    val morphShape = com.mrtdk.liquid_glass.ui.components.shapes.lerp(
        start = ContinuousCapsule,
        stop = ContinuousRoundedRectangle(24.dp),
        fraction = morphProgress
    )
    val morphTint = Color.Unspecified

    // Smooth crossfade opacities
    val pillIconsAlpha = ((0.28f - morphProgress) / 0.28f).coerceIn(0f, 1f)
    val menuContentAlpha = ((morphProgress - 0.22f) / 0.78f).coerceIn(0f, 1f)

    glassScope.GlassBox(
        modifier = Modifier
            .size(width = morphWidth, height = morphHeight)
            .clip(morphShape),
        shape = morphShape,
        tint = morphTint,
        blur = 0.85f,
        centerDistortion = 0.1f,
        scale = 0.02f,
        warpEdges = 0.4f,
        elevation = 16.dp,
        contentAlignment = Alignment.TopEnd
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Collapsed: Original Share + 3 Dots Capsule
            if (pillIconsAlpha > 0.001f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(124.dp)
                        .height(44.dp)
                        .padding(start = 6.dp, end = 8.dp)
                        .graphicsLayer { alpha = pillIconsAlpha },
                    horizontalArrangement = Arrangement.SpaceBetween,
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
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.compartir),
                            contentDescription = "Share",
                            tint = glassIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { onExpandChange(true) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.tres_puntos),
                            contentDescription = "More",
                            tint = glassIconTint,
                            modifier = Modifier.width(22.dp).height(16.dp)
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        ArtistMenuInnerContent(
                            context = context,
                            artistId = artistState.id,
                            artistName = artistState.name,
                            artistThumb = artistThumb,
                            isFavorite = isFavorite,
                            topSongs = topSongs,
                            onSongSelected = onSongSelected,
                            scope = coroutineScope,
                            onDismiss = { onExpandChange(false) }
                        )
                    }
                }
            }
        }
    }
}

