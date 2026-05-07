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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    var quickPickSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }

    // Multiple "Similar to" sections
    data class SimilarSection(val artistName: String, val items: List<com.echo.innertube.models.YTItem>)
    var similarSections by remember { mutableStateOf<List<SimilarSection>>(emptyList()) }

    // "Selecciones para ti" songs
    var seleccionesParaTi by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var seleccionesTitle by remember { mutableStateOf<String?>(null) }
    
    // Sugerencias destacadas
    var featuredSuggestions by remember { mutableStateOf<List<com.echo.innertube.models.YTItem>>(emptyList()) }

    // Recently played from LibraryManager
    val recentlyPlayed by LibraryManager.recentlyPlayed.collectAsState()

    // Initial load — local songs and youtube homepage (in parallel for speed)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Run local scan and YouTube home in parallel
            val localDeferred = async {
                val scanner = LocalMediaScanner(context)
                scanner.getLocalSongs()
            }
            val homeDeferred = async {
                YouTube.home().getOrNull()
            }

            localSongs = localDeferred.await()
            val page = homeDeferred.await()
            
            if (page != null) {
                homePage = page
                // Only fetch 2 continuations initially for faster first paint
                var current: com.echo.innertube.pages.HomePage = page
                var fetched = 0
                while (current.continuation != null && fetched < 2) {
                    val cont = current.continuation ?: break
                    val next = YouTube.home(continuation = cont).getOrNull()
                    if (next != null) {
                        current = next
                        homePage = homePage?.copy(
                            sections = (homePage?.sections ?: emptyList()) + next.sections,
                            continuation = next.continuation
                        )
                        fetched++
                    } else break
                }
            }
        }
    }

    // Dynamic Quick Picks + Similar sections mixed algorithm
    var algorithmSeeds by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }

    LaunchedEffect(recentlyPlayed) {
        val recentSongs = recentlyPlayed.filter { it.type == ItemType.SONG }
        if (recentSongs.isNotEmpty() && algorithmSeeds.isEmpty()) {
            // First load: pick 3 diverse seeds from the top 15 recently played
            algorithmSeeds = recentSongs.take(15).shuffled().take(3)
        } else if (recentSongs.isNotEmpty()) {
            // Update seeds when a new song is played to keep it fresh but mixed
            val latest = recentSongs.first()
            if (!algorithmSeeds.contains(latest)) {
                algorithmSeeds = (listOf(latest) + algorithmSeeds.take(2))
            }
        }
    }

    LaunchedEffect(algorithmSeeds) {
        if (algorithmSeeds.isEmpty()) return@LaunchedEffect
        
        withContext(Dispatchers.IO) {
            val allQuickPicks = mutableListOf<SongItem>()
            val allParaTi = mutableListOf<SongItem>()
            val allSuggestions = mutableListOf<com.echo.innertube.models.YTItem>()
            val sections = mutableListOf<SimilarSection>()

            // Fetch related pages in parallel for a richer mix
            val deferreds = algorithmSeeds.map { song ->
                async {
                    val vid = song.id
                    val artist = song.subtitle
                    val nextResult = YouTube.next(WatchEndpoint(videoId = vid)).getOrNull()
                    val relatedEndpoint = nextResult?.relatedEndpoint
                    if (relatedEndpoint != null) {
                        val relatedPage = YouTube.related(relatedEndpoint).getOrNull()
                        if (relatedPage != null) {
                            Pair(artist, relatedPage)
                        } else null
                    } else null
                }
            }

            val results = deferreds.awaitAll().filterNotNull()

            for ((seedArtist, relatedPage) in results) {
                allQuickPicks.addAll(relatedPage.songs.take(6))
                allParaTi.addAll(relatedPage.songs.drop(6).take(5))

                val availableSongs = relatedPage.songs.drop(11)
                allSuggestions.addAll(availableSongs.take(4))

                // Primary similar section for this seed
                val primaryItems = mutableListOf<com.echo.innertube.models.YTItem>()
                primaryItems.addAll(relatedPage.artists.take(3))
                primaryItems.addAll(availableSongs.drop(4).take(3))
                if (primaryItems.isNotEmpty()) {
                    sections.add(SimilarSection(
                        artistName = seedArtist,
                        items = primaryItems.shuffled()
                    ))
                }
            }

            quickPickSongs = allQuickPicks.distinctBy { it.id }.shuffled().take(12)
            seleccionesParaTi = allParaTi.distinctBy { it.id }.shuffled().take(10)

            if (results.isNotEmpty()) {
                val primaryArtist = results.first().first
                seleccionesTitle = if (primaryArtist.isNotEmpty() && primaryArtist != "Artistas") primaryArtist else "Mix para ti"
            }

            featuredSuggestions = allSuggestions.distinctBy {
                when (it) {
                    is SongItem -> it.id
                    is com.echo.innertube.models.AlbumItem -> it.id
                    is com.echo.innertube.models.ArtistItem -> it.id
                    else -> it.toString()
                }
            }.shuffled().take(7)

            similarSections = sections.distinctBy { it.artistName }.take(5)
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
        val displaySuggestions = if (featuredSuggestions.isNotEmpty()) featuredSuggestions 
                                 else homePage?.sections?.firstOrNull()?.items ?: emptyList()
                                 
        if (displaySuggestions.isNotEmpty()) {
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
                    items(displaySuggestions.take(7).size) { index ->
                        val itm = displaySuggestions[index]
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

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val columns = quickPickSongs.chunked(3)
                    items(columns.size) { colIndex ->
                        Column(modifier = Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            columns[colIndex].forEach { song ->
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
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(song.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // ═══════════════════════════════════════════════════════════
        // KEEP LISTENING — "Sigue escuchando" (2-row horizontal grid)
        // ═══════════════════════════════════════════════════════════
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sigue escuchando", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val columns = recentlyPlayed.take(20).chunked(2)
                    items(columns.size) { colIndex ->
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            columns[colIndex].forEach { item ->
                                val hdThumb = upgradeThumb(item.thumbnail)
                                Row(
                                    modifier = Modifier
                                        .width(340.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (item.type == ItemType.SONG) {
                                                onSongSelected(PlayerState(
                                                    title = item.title,
                                                    artist = item.subtitle,
                                                    artUrl = upgradeThumbHD(item.thumbnail),
                                                    videoId = item.id
                                                ))
                                            } else {
                                                onAlbumSelected(AlbumState(
                                                    id = item.id,
                                                    playlistId = item.id,
                                                    title = item.title,
                                                    artist = item.subtitle,
                                                    thumbnail = item.thumbnail,
                                                    year = null
                                                ))
                                            }
                                        }
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(86.dp).clip(if (item.type == ItemType.ARTIST) CircleShape else RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(item.subtitle, color = Color.Gray, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // SIMILAR TO [ARTIST] — Multiple sections
        // ═══════════════════════════════════════════════════════════
        similarSections.forEach { section ->
            item {
                Text("Similar a", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(section.artistName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = "More", tint = Color.Gray)
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(section.items.take(10).size) { index ->
                        val item = section.items[index]
                        val hdThumb = upgradeThumb(
                            when (item) {
                                is com.echo.innertube.models.ArtistItem -> item.thumbnail
                                is com.echo.innertube.models.SongItem -> item.thumbnail
                                is com.echo.innertube.models.AlbumItem -> item.thumbnail
                                else -> null
                            }
                        )
                        val title = when (item) {
                            is com.echo.innertube.models.ArtistItem -> item.title
                            is com.echo.innertube.models.SongItem -> item.title
                            is com.echo.innertube.models.AlbumItem -> item.title
                            else -> "Desconocido"
                        }
                        val subtitle = when (item) {
                            is com.echo.innertube.models.ArtistItem -> "Artista"
                            is com.echo.innertube.models.SongItem -> "Canción • ${item.artists.joinToString { it.name }}"
                            is com.echo.innertube.models.AlbumItem -> "Álbum • ${item.year ?: ""}"
                            else -> ""
                        }
                        val isCircle = item is com.echo.innertube.models.ArtistItem
                        Column(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable {
                                    when (item) {
                                        is com.echo.innertube.models.ArtistItem -> {
                                            onArtistSelected(com.mrtdk.liquid_glass.ui.screens.ArtistState(item.id, item.title, item.thumbnail))
                                        }
                                        is com.echo.innertube.models.SongItem -> {
                                            onSongSelected(PlayerState(item.title, item.artists.joinToString { it.name }, item.thumbnail, item.id))
                                        }
                                        is com.echo.innertube.models.AlbumItem -> {
                                            onAlbumSelected(com.mrtdk.liquid_glass.ui.screens.AlbumState(item.id, item.playlistId ?: item.id, item.title, item.artists?.joinToString { it.name } ?: "Varios", item.thumbnail, item.year as? Int))
                                        }
                                        else -> {}
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(160.dp).clip(if (isCircle) CircleShape else RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(subtitle, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // (Old Escuchado Recientemente block removed in favor of Sigue escuchando)

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
// Featured Suggestion Card — full-bleed image with overlaid text
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

    // Single Box: image fills entire card, text overlays at the bottom
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .clickable(onClick = clickAction)
    ) {
        // Layer 1: Full-bleed album art
        AsyncImage(
            model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
            contentDescription = titleStr,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Bottom gradient overlay (transparent → dark)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Layer 3: Text floating over the gradient
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(
                titleStr,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitleStr,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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