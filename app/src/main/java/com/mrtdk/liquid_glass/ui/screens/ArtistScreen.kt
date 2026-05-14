package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import com.mrtdk.glass.GlassBox // kept for potential future use
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
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

@Composable
fun ArtistScreen(
    artistState: ArtistState,
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
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    // Generic "show all" overlay for videos / remaining sections
    var showAllSectionOverlay by remember { mutableStateOf(false) }
    var allSectionData by remember { mutableStateOf<ArtistSection?>(null) }
    var allSectionTitle by remember { mutableStateOf("") }
    var allSectionIsVideo by remember { mutableStateOf(false) }

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
                val searchResult = YouTube.search(artistState.name, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                val foundArtist = searchResult?.items?.filterIsInstance<com.echo.innertube.models.ArtistItem>()?.firstOrNull()
                if (foundArtist != null && foundArtist.id != artistState.id) {
                    YouTube.artist(foundArtist.id).onSuccess { page ->
                        artistPage = page
                    }
                }
                // If still nothing, build a minimal page from search results
                if (artistPage == null) {
                    val songs = YouTube.search(artistState.name, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterIsInstance<SongItem>()?.take(10) ?: emptyList()
                    val albums = YouTube.search(artistState.name, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()?.items?.filterIsInstance<AlbumItem>() ?: emptyList()
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

    // Extract dominant color
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
                        dominantColor = Color(bmp.getPixel(bmp.width / 2, bmp.height * 3 / 4))
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

    // Handle back for overlays
    androidx.activity.compose.BackHandler(enabled = showAllAlbumsOverlay || showAllSectionOverlay) {
        when {
            showAllSectionOverlay -> showAllSectionOverlay = false
            showAllAlbumsOverlay -> showAllAlbumsOverlay = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Main content
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── HERO ───────────────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                    // Clean artist image — no blur, no gradient, no zoom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111111))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                            contentDescription = artistState.name,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Subtle bottom fade to black for smooth transition
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.65f to Color.Transparent,
                                        1f to Color.Black
                                    )
                                )
                        )
                    }
                        // Artist name and description BELOW image
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(horizontal = 20.dp)
                                .padding(top = 16.dp, bottom = 4.dp)
                        ) {
                            Text(
                                artistState.name,
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (artistPage?.description != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    artistPage!!.description!!,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                )
                                if (!isDescriptionExpanded) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Ver más", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isDescriptionExpanded = true })
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Ocultar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isDescriptionExpanded = false })
                                }
                            }
                        }
                    }
                    // Back button floating on top of image — GlassBox pill
                    com.mrtdk.glass.GlassContainer(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .size(48.dp),
                        content = {
                            Box(modifier = Modifier.fillMaxSize())
                        },
                        glassContent = {
                            val scope = this
                            scope.GlassBox(
                                modifier = Modifier.fillMaxSize().clip(CircleShape).clickable { onBack() },
                                shape = CircleShape,
                                tint = dominantColor.copy(alpha = 0.35f),
                                blur = 0.8f,
                                centerDistortion = 0.2f,
                                scale = 0.02f,
                                warpEdges = 0.6f,
                                elevation = 8.dp
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White)
                                }
                            }
                        }
                    )
                }
            }

            // ── ACTION BUTTONS ─────────────────────────────
            item {
                Row(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable { }, contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(22.dp)) { drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 3f); drawLine(Color.White, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(0f, size.height), strokeWidth = 3f) }
                    }
                    Box(modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(26.dp)).background(Color.White).clickable {
                        topSongsSection?.items?.filterIsInstance<SongItem>()?.firstOrNull()?.let { s -> onSongSelected(PlayerState(title = s.title, artist = s.artists.joinToString { it.name }, artUrl = s.thumbnail, videoId = s.id)) }
                    }, contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Canvas(modifier = Modifier.size(16.dp)) { drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, size.height / 2f); lineTo(0f, size.height); close() }, Color.Black) }
                            Text("Reproducir", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    val isSaved = savedItems.any { it.id == artistState.id }
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable {
                        if (!isSaved) LibraryManager.saveItem(LibraryItem(id = artistState.id, title = artistState.name, subtitle = "Artist", thumbnail = artistThumb, type = ItemType.ARTIST))
                        else LibraryManager.removeItem(artistState.id)
                    }, contentAlignment = Alignment.Center) {
                        val iconTint by animateColorAsState(if (isSaved) Color(0xFFFA243C) else Color.White, label = "")
                        Icon(if (isSaved) Icons.Default.Check else Icons.Default.Add, "Add", tint = iconTint, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // ── LOADING ────────────────────────────────────
            if (isLoading) {
                item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
            }

            // ── TOP SONGS ──────────────────────────────────
            if (topSongsSection != null) {
                val songs = topSongsSection.items.filterIsInstance<SongItem>()
                item { Text("Top songs", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
                items(songs.take(5).size) { i ->
                    val song = songs[i]
                    Row(modifier = Modifier.fillMaxWidth().background(Color.Black).clickable { onSongSelected(PlayerState(title = song.title, artist = song.artists.joinToString { it.name }, artUrl = song.thumbnail, videoId = song.id)) }.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.width(28.dp))
                        AsyncImage(model = ImageRequest.Builder(context).data(song.thumbnail).crossfade(true).build(), contentDescription = song.title, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, null, tint = Color.Gray) }
                    }
                }
            }

            // ── ALBUMS ─────────────────────────────────────
            if (albumsSection != null) {
                val albumItems = albumsSection.items.filterIsInstance<AlbumItem>()
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Álbumes", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (albumItems.size > 4 || albumsSection.moreEndpoint != null) {
                            val coroutineScope = rememberCoroutineScope()
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.12f)).clickable {
                                allAlbumsSection = albumsSection; 
                                showAllAlbumsOverlay = true
                                if (albumsSection.moreEndpoint != null && allAlbumsSection?.items?.size == albumsSection.items.size) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val result = YouTube.artistItems(albumsSection.moreEndpoint!!).getOrNull()
                                        if (result != null) {
                                            allAlbumsSection = allAlbumsSection?.copy(items = result.items)
                                        }
                                    }
                                }
                            }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("Ver todo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(albumItems.take(8).size) { idx -> ItemCard(context, albumItems[idx], artistState.name, onAlbumSelected, onSongSelected, onArtistSelected) }
                    }
                }
            }

            // ── SINGLES Y EP ───────────────────────────────
            if (singlesSection != null) {
                val singleItems = singlesSection.items.filterIsInstance<AlbumItem>()
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sencillos y EP", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(singleItems.size) { idx -> ItemCard(context, singleItems[idx], artistState.name, onAlbumSelected, onSongSelected, onArtistSelected) }
                    }
                }
            }

            // ── VIDEOS ─────────────────────────────────────
            if (videosSection != null) {
                val videoItems = videosSection.items.filterIsInstance<SongItem>()
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Videos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (videoItems.size > 4 || videosSection.moreEndpoint != null) {
                            val coroutineScope = rememberCoroutineScope()
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.12f)).clickable {
                                allSectionTitle = "Videos"
                                allSectionIsVideo = true
                                allSectionData = videosSection
                                showAllSectionOverlay = true
                                if (videosSection.moreEndpoint != null) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val result = YouTube.artistItems(videosSection.moreEndpoint!!).getOrNull()
                                        if (result != null) allSectionData = allSectionData?.copy(items = result.items)
                                    }
                                }
                            }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("Ver todo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(videoItems.size) { idx ->
                            val v = videoItems[idx]
                            ItemCard(context, v, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, onVideoSelected = onVideoSelected, isVideo = true)
                        }
                    }
                }
            }

            // ── DESTACADO EN ───────────────────────────────
            if (featuredSection != null) {
                val featuredItems = featuredSection.items.filterIsInstance<AlbumItem>()
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Destacado en", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(featuredItems.size) { idx -> ItemCard(context, featuredItems[idx], artistState.name, onAlbumSelected, onSongSelected, onArtistSelected) }
                    }
                }
            }

            // ── PLAYLISTS ──────────────────────────────────
            if (playlistsSection != null) {
                val plItems = playlistsSection.items.filterIsInstance<PlaylistItem>()
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Playlists", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(plItems.size) { idx ->
                            val pl = plItems[idx]
                            ItemCard(context, pl, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected)
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
                        Text("Puede que también te guste", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
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
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(section.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (section.items.size > 4 || section.moreEndpoint != null) {
                            val coroutineScope = rememberCoroutineScope()
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.12f)).clickable {
                                allSectionTitle = section.title
                                allSectionIsVideo = isVideoSection
                                allSectionData = section
                                showAllSectionOverlay = true
                                if (section.moreEndpoint != null) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val result = YouTube.artistItems(section.moreEndpoint!!).getOrNull()
                                        if (result != null) allSectionData = allSectionData?.copy(items = result.items)
                                    }
                                }
                            }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("Ver todo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(section.items.size) { idx -> ItemCard(context, section.items[idx], artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, onVideoSelected = onVideoSelected, isVideo = isVideoSection) }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // ── ALL ALBUMS OVERLAY PAGE ────────────────────
        if (showAllAlbumsOverlay && allAlbumsSection != null) {
            val allAlbums = allAlbumsSection!!.items.filterIsInstance<AlbumItem>()
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(modifier = Modifier.statusBarsPadding().height(16.dp)) }
                    // Pill back button
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable { showAllAlbumsOverlay = false }, contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Álbumes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                                    ItemCard(context, album, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, fillWidth = true)
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
        if (showAllSectionOverlay && allSectionData != null) {
            val overlayItems = allSectionData!!.items
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Spacer(modifier = Modifier.statusBarsPadding().height(16.dp)) }
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable { showAllSectionOverlay = false }, contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
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
                                        ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, onVideoSelected = onVideoSelected, fillWidth = true, isVideo = true)
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
                                        ItemCard(context, item, artistState.name, onAlbumSelected, onSongSelected, onArtistSelected, fillWidth = true)
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
    isVideo: Boolean = false
) {
    val thumbnailHeight = if (isVideo) 128.dp else 180.dp
    val thumbnailRatio = if (isVideo) 16f / 9f else 1f
    
    val cardMod = if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(thumbnailHeight * thumbnailRatio)
    val imgMod = if (fillWidth) Modifier.fillMaxWidth().aspectRatio(thumbnailRatio) else Modifier.width(thumbnailHeight * thumbnailRatio).height(thumbnailHeight)

    when (item) {
        is AlbumItem -> {
            val hdThumb = item.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
            Column(modifier = cardMod.clickable {
                onAlbumSelected(AlbumState(id = item.id, playlistId = item.playlistId ?: item.id, title = item.title, artist = item.artists?.joinToString { it.name } ?: artistName, thumbnail = item.thumbnail, year = item.year as? Int ?: item.year?.toString()?.toIntOrNull()))
            }) {
                Box(modifier = imgMod.clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
                    AsyncImage(model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(), contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.year ?: ""}", color = Color.Gray, fontSize = 13.sp)
            }
        }
        is SongItem -> {
            val hdThumb = item.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
            Column(modifier = cardMod.clickable { 
                if (isVideo) {
                    onVideoSelected(item.id)
                } else {
                    onSongSelected(PlayerState(title = item.title, artist = item.artists.joinToString { it.name }, artUrl = item.thumbnail, videoId = item.id)) 
                }
            }) {
                Box(modifier = imgMod.clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
                    AsyncImage(model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(), contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artists.joinToString { it.name }, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        is PlaylistItem -> {
            val hdThumb = item.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
            Column(modifier = cardMod.clickable {
                onAlbumSelected(AlbumState(id = item.id, playlistId = item.id, title = item.title, artist = item.author?.name ?: artistName, thumbnail = item.thumbnail, year = null))
            }) {
                Box(modifier = imgMod.clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
                    AsyncImage(model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(), contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.author?.name ?: "Playlist", color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
