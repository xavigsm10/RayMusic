package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LocalMediaScanner
import com.mrtdk.liquid_glass.data.Song
import com.echo.innertube.YouTube
import com.echo.innertube.models.ArtistItem
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.WatchEndpoint
import com.echo.innertube.pages.RelatedPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InicioScreen(
    innerPadding: PaddingValues,
    playerState: PlayerState? = null,
    onSongSelected: (PlayerState) -> Unit = {},
    onArtistSelected: (ArtistState) -> Unit = {},
    onAlbumSelected: (AlbumState) -> Unit = {}
) {
    val context = LocalContext.current
    var localSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var homePage by remember { mutableStateOf<com.echo.innertube.pages.HomePage?>(null) }

    // Quick picks from related content
    var quickPickSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }

    // Multiple "Similar to" sections
    data class SimilarSection(val artistName: String, val artists: List<ArtistItem>)
    var similarSections by remember { mutableStateOf<List<SimilarSection>>(emptyList()) }

    // "Selecciones para ti" songs
    var seleccionesParaTi by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var seleccionesTitle by remember { mutableStateOf<String?>(null) }

    // Recently played from LibraryManager
    val recentlyPlayed by LibraryManager.recentlyPlayed.collectAsState()

    // Initial load — local songs and youtube homepage
    LaunchedEffect(Unit) {
        val scanner = LocalMediaScanner(context)
        localSongs = scanner.getLocalSongs()

        withContext(Dispatchers.IO) {
            YouTube.home().onSuccess { page ->
                homePage = page

                var current = page
                var fetched = 0
                while (current.continuation != null && fetched < 4) {
                    val next = YouTube.home(continuation = current.continuation).getOrNull()
                    if (next != null) {
                        current = next
                        homePage = homePage?.copy(
                            sections = homePage!!.sections + next.sections,
                            continuation = next.continuation
                        )
                        fetched++
                    } else break
                }
            }
        }
    }

    // Dynamic Quick Picks + Similar sections when a song is playing
    LaunchedEffect(playerState?.videoId) {
        val vid = playerState?.videoId ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            YouTube.next(WatchEndpoint(videoId = vid)).onSuccess { nextResult ->
                val relatedEndpoint = nextResult.relatedEndpoint ?: return@onSuccess
                YouTube.related(relatedEndpoint).onSuccess { relatedPage ->
                    quickPickSongs = relatedPage.songs.take(6)
                    seleccionesParaTi = relatedPage.songs.drop(6).take(10)
                    seleccionesTitle = playerState.artist

                    // Build multiple "Similar to" sections
                    val sections = mutableListOf<SimilarSection>()

                    // Primary: similar to current artist
                    if (relatedPage.artists.isNotEmpty()) {
                        sections.add(SimilarSection(
                            artistName = playerState.artist,
                            artists = relatedPage.artists.take(6)
                        ))
                    }

                    // Try to get additional similar sections from related songs' artists
                    val uniqueArtistNames = relatedPage.songs
                        .flatMap { it.artists }
                        .map { it.name }
                        .distinct()
                        .filter { it != playerState.artist }
                        .take(3)

                    for (artistName in uniqueArtistNames) {
                        val artistSongs = relatedPage.songs.filter { song ->
                            song.artists.any { it.name == artistName }
                        }
                        if (artistSongs.size >= 2) {
                            // Use the artists from the related page that aren't the main one
                            val otherArtists = relatedPage.artists.filter { it.title != artistName }.take(4)
                            if (otherArtists.isNotEmpty()) {
                                sections.add(SimilarSection(
                                    artistName = artistName,
                                    artists = otherArtists
                                ))
                            }
                        }
                        if (sections.size >= 3) break
                    }

                    similarSections = sections
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 24.dp,
            bottom = innerPadding.calculateBottomPadding() + 180.dp
        )
    ) {
        item {
            Text(
                text = "Home",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ═══════════════════════════════════════════════════════════
        // FEATURED SUGGESTION CARDS — "Sugerencias destacadas para ti"
        // ═══════════════════════════════════════════════════════════
        val featuredSection = homePage?.sections?.firstOrNull()
        if (featuredSection != null && featuredSection.items.isNotEmpty()) {
            item {
                Text(
                    text = "Sugerencias destacadas para ti",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(featuredSection.items.take(10).size) { index ->
                        val itm = featuredSection.items[index]
                        FeaturedSuggestionCard(
                            context = context,
                            item = itm,
                            onSongSelected = onSongSelected,
                            onAlbumSelected = onAlbumSelected,
                            onArtistSelected = onArtistSelected
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // QUICK PICKS — "Selecciones rápidas"
        // ═══════════════════════════════════════════════════════════
        if (quickPickSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Selecciones rápidas", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2C2C2E))
                            .clickable {
                                if (quickPickSongs.isNotEmpty()) {
                                    val first = quickPickSongs.first()
                                    val hdThumb = upgradeThumbHD(first.thumbnail)
                                    onSongSelected(PlayerState(title = first.title, artist = first.artists.joinToString { it.name }, artUrl = hdThumb, videoId = first.id))
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play all", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play all", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Quick pick songs as vertical list items
            items(quickPickSongs.size) { index ->
                val song = quickPickSongs[index]
                val hdThumb = upgradeThumb(song.thumbnail)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSongSelected(PlayerState(
                                title = song.title,
                                artist = song.artists.joinToString { it.name },
                                artUrl = upgradeThumbHD(song.thumbnail),
                                videoId = song.id
                            ))
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // ═══════════════════════════════════════════════════════════
        // SIMILAR TO [ARTIST] — Multiple sections
        // ═══════════════════════════════════════════════════════════
        similarSections.forEach { section ->
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Similar a", color = Color.Gray, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = section.artistName,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text("→", color = Color.Gray, fontSize = 22.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(section.artists.size) { index ->
                        val artist = section.artists[index]
                        val hdThumb = upgradeThumb(artist.thumbnail)
                        Column(
                            modifier = Modifier.width(160.dp).clickable {
                                onArtistSelected(ArtistState(id = artist.id, name = artist.title, thumbnail = hdThumb))
                            },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                contentDescription = artist.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(160.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(artist.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // RECENTLY PLAYED — tracked play history (larger carousel cards)
        // ═══════════════════════════════════════════════════════════
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Escuchado recientemente", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = "See all", tint = Color.Gray)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recentlyPlayed.take(20).size) { index ->
                        val item = recentlyPlayed[index]
                        val hdThumb = upgradeThumb(item.thumbnail)
                        Column(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable {
                                    onSongSelected(PlayerState(
                                        title = item.title,
                                        artist = item.subtitle,
                                        artUrl = upgradeThumbHD(item.thumbnail),
                                        videoId = item.id
                                    ))
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // Selecciones para ti (because you listened to...)
        // ═══════════════════════════════════════════════════════════
        if (seleccionesParaTi.isNotEmpty() && seleccionesTitle != null) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Porque escuchaste a", color = Color.Gray, fontSize = 13.sp)
                    Text(seleccionesTitle!!, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(seleccionesParaTi.size) { index ->
                        val song = seleccionesParaTi[index]
                        val hdThumb = upgradeThumb(song.thumbnail)
                        Column(
                            modifier = Modifier.width(160.dp).clickable {
                                onSongSelected(PlayerState(title = song.title, artist = song.artists.joinToString { it.name }, artUrl = upgradeThumbHD(song.thumbnail), videoId = song.id))
                            }
                        ) {
                            Box(
                                modifier = Modifier.size(160.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1C1C1E))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(song.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // YouTube Music Home Page Sections (skip first since we used it for featured)
        // ═══════════════════════════════════════════════════════════
        if (homePage == null) {
            item {
                Text(
                    text = "Cargando tus recomendaciones...",
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            // Skip first section since it's used for featured cards
            homePage!!.sections.drop(1).forEach { section ->
                if (section.items.isNotEmpty()) {
                    item {
                        SectionTitle(section.title, isDark = true, small = false)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(section.items.size) { index ->
                                val itm = section.items[index]
                                var titleStr = ""
                                var subtitleStr = ""
                                var thumbUrl: String? = null
                                var clickAction: () -> Unit = {}
                                var isCircle = false

                                when (itm) {
                                    is com.echo.innertube.models.SongItem -> {
                                        titleStr = itm.title
                                        subtitleStr = itm.artists.joinToString { it.name }
                                        thumbUrl = itm.thumbnail
                                        clickAction = {
                                            onSongSelected(PlayerState(title = titleStr, artist = subtitleStr, artUrl = upgradeThumbHD(thumbUrl), videoId = itm.id))
                                        }
                                    }
                                    is com.echo.innertube.models.AlbumItem -> {
                                        titleStr = itm.title
                                        subtitleStr = itm.artists?.joinToString { it.name } ?: "Album"
                                        thumbUrl = itm.thumbnail
                                        clickAction = {
                                            onAlbumSelected(AlbumState(id = itm.id, playlistId = itm.playlistId ?: itm.id, title = titleStr, artist = subtitleStr, thumbnail = thumbUrl, year = itm.year as? Int ?: itm.year?.toString()?.toIntOrNull()))
                                        }
                                    }
                                    is com.echo.innertube.models.ArtistItem -> {
                                        titleStr = itm.title
                                        subtitleStr = "Artist"
                                        thumbUrl = itm.thumbnail
                                        isCircle = true
                                        clickAction = {
                                            onArtistSelected(ArtistState(id = itm.id, name = titleStr, thumbnail = thumbUrl))
                                        }
                                    }
                                    is com.echo.innertube.models.PlaylistItem -> {
                                        titleStr = itm.title
                                        subtitleStr = itm.author?.name ?: "Playlist"
                                        thumbUrl = itm.thumbnail
                                        clickAction = {
                                            onAlbumSelected(AlbumState(id = itm.id, playlistId = itm.id, title = titleStr, artist = subtitleStr, thumbnail = thumbUrl, year = null))
                                        }
                                    }
                                    is com.echo.innertube.models.EpisodeItem -> {
                                        titleStr = itm.title
                                        subtitleStr = itm.author?.name ?: "Podcast"
                                        thumbUrl = itm.thumbnail
                                    }
                                    is com.echo.innertube.models.PodcastItem -> {
                                        titleStr = itm.title
                                        subtitleStr = itm.author?.name ?: "Podcast"
                                        thumbUrl = itm.thumbnail
                                        clickAction = {
                                            onAlbumSelected(AlbumState(id = itm.id, playlistId = itm.id, title = titleStr, artist = subtitleStr, thumbnail = thumbUrl, year = null))
                                        }
                                    }
                                    else -> {}
                                }

                                val hdThumb = upgradeThumb(thumbUrl)

                                // Standard card (160dp for consistency with recently played)
                                Column(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .clickable(onClick = clickAction)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(160.dp)
                                            .clip(if (isCircle) CircleShape else RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1C1C1E))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                            contentDescription = titleStr,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(titleStr, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(subtitleStr, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Featured Suggestion Card — large card with image + color gradient
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun FeaturedSuggestionCard(
    context: android.content.Context,
    item: com.echo.innertube.models.YTItem,
    onSongSelected: (PlayerState) -> Unit,
    onAlbumSelected: (AlbumState) -> Unit,
    onArtistSelected: (ArtistState) -> Unit
) {
    var titleStr = ""
    var subtitleStr = ""
    var thumbUrl: String? = null
    var labelStr = ""
    var clickAction: () -> Unit = {}

    when (item) {
        is com.echo.innertube.models.SongItem -> {
            titleStr = item.title
            subtitleStr = item.artists.joinToString { it.name }
            thumbUrl = item.thumbnail
            labelStr = "Canción"
            clickAction = { onSongSelected(PlayerState(title = titleStr, artist = subtitleStr, artUrl = upgradeThumbHD(thumbUrl), videoId = item.id)) }
        }
        is com.echo.innertube.models.AlbumItem -> {
            titleStr = item.title
            subtitleStr = item.artists?.joinToString { it.name } ?: "Album"
            thumbUrl = item.thumbnail
            labelStr = "Álbum"
            clickAction = { onAlbumSelected(AlbumState(id = item.id, playlistId = item.playlistId ?: item.id, title = titleStr, artist = subtitleStr, thumbnail = thumbUrl, year = item.year as? Int ?: item.year?.toString()?.toIntOrNull())) }
        }
        is com.echo.innertube.models.ArtistItem -> {
            titleStr = item.title
            subtitleStr = "Artista"
            thumbUrl = item.thumbnail
            labelStr = "Artista"
            clickAction = { onArtistSelected(ArtistState(id = item.id, name = titleStr, thumbnail = thumbUrl)) }
        }
        is com.echo.innertube.models.PlaylistItem -> {
            titleStr = item.title
            subtitleStr = item.author?.name ?: "Playlist"
            thumbUrl = item.thumbnail
            labelStr = "Playlist"
            clickAction = { onAlbumSelected(AlbumState(id = item.id, playlistId = item.id, title = titleStr, artist = subtitleStr, thumbnail = thumbUrl, year = null)) }
        }
        else -> {
            titleStr = "Unknown"
            subtitleStr = ""
        }
    }

    val hdThumb = upgradeThumb(thumbUrl)

    // Extract dominant color for gradient bottom
    var dominantColor by remember { mutableStateOf(Color(0xFF1C1C1E)) }
    var cardTextColor by remember { mutableStateOf(Color.White) }

    LaunchedEffect(hdThumb) {
        if (hdThumb != null) {
            withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context).data(hdThumb).allowHardware(false).size(80).build()
                    val result = coil.Coil.imageLoader(context).execute(request)
                    if (result is coil.request.SuccessResult) {
                        val drawable = result.drawable
                        val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                            ?: android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).also {
                                val canvas = android.graphics.Canvas(it)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)
                            }
                        val sampledColor = Color(bitmap.getPixel(bitmap.width / 2, bitmap.height - 1))
                        dominantColor = sampledColor
                        cardTextColor = if (sampledColor.luminance() > 0.5f) Color.Black else Color.White
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Column(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = clickAction)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF1C1C1E))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                contentDescription = titleStr,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, dominantColor)
                        )
                    )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(dominantColor)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (labelStr.isNotEmpty()) {
                Text(labelStr.uppercase(), color = cardTextColor.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(titleStr, color = cardTextColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitleStr, color = cardTextColor.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// Utility functions for thumbnail quality upgrades
private fun upgradeThumb(url: String?): String? {
    return url?.let {
        when {
            it.contains("=w") -> it.substringBefore("=w") + "=w540-h540-l90-rj"
            it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
            else -> it
        }
    }
}

private fun upgradeThumbHD(url: String?): String? {
    return url?.let {
        when {
            it.contains("=w") -> it.substringBefore("=w") + "=w1200-h1200-l90-rj"
            it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
            else -> it
        }
    }
}

@Composable
fun SectionTitle(title: String, isDark: Boolean = true, small: Boolean = false) {
    Text(
        text = title,
        color = if (small) Color.Gray else Color.White,
        fontSize = if (small) 12.sp else 22.sp,
        fontWeight = if (small) FontWeight.SemiBold else FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = if (small) 4.dp else 12.dp)
    )
}