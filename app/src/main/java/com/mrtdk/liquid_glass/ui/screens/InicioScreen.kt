package com.mrtdk.liquid_glass.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import coil.Coil
import coil.request.SuccessResult

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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.ui.components.trackClickBounds
import com.mrtdk.liquid_glass.ui.components.trackTapBounds
import com.mrtdk.liquid_glass.ui.components.wiggleOnScroll
import com.mrtdk.liquid_glass.ui.components.SharedTransitionState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.mrtdk.liquid_glass.data.Song
import com.echo.innertube.YouTube
import org.json.JSONArray
import org.json.JSONObject
import com.echo.innertube.models.Artist
import com.echo.innertube.models.Album
import com.echo.innertube.models.PlaylistItem
import com.echo.innertube.models.YTItem
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import kotlinx.coroutines.coroutineScope
import com.echo.innertube.models.ArtistItem
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.WatchEndpoint
import com.echo.innertube.pages.RelatedPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

data class SimilarSection(val artistName: String, val items: List<com.echo.innertube.models.YTItem>)
data class PorqueEscuchasteSection(val artistName: String, val songs: List<SongItem>)

class InicioState {
    var isLoaded by mutableStateOf(false)
    var localSongs by mutableStateOf<List<Song>>(emptyList())
    var homePage by mutableStateOf<com.echo.innertube.pages.HomePage?>(null)
    var quickPickSongs by mutableStateOf<List<SongItem>>(emptyList())
    var similarSections by mutableStateOf<List<SimilarSection>>(emptyList())
    var porqueEscuchasteSections by mutableStateOf<List<PorqueEscuchasteSection>>(emptyList())
    var seleccionesParaTi by mutableStateOf<List<SongItem>>(emptyList())
    var seleccionesTitle by mutableStateOf<String?>(null)
    var featuredSuggestions by mutableStateOf<List<com.echo.innertube.models.YTItem>>(emptyList())
    var featuredPlaylists by mutableStateOf<List<com.echo.innertube.models.PlaylistItem>>(emptyList())
}

@Composable
fun InicioScreen(
    innerPadding: PaddingValues,
    playerState: PlayerState? = null,
    state: InicioState = remember { InicioState() },
    onSongSelected: (PlayerState) -> Unit = {},
    onArtistSelected: (ArtistState) -> Unit = {},
    onAlbumSelected: (AlbumState) -> Unit = {},
    onVideoSelected: (String) -> Unit = {},
    onReplaySelected: () -> Unit = {}
) {
    val context = LocalContext.current
    var activeSimilarSection by remember { mutableStateOf<SimilarSection?>(null) }
    val similarGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    androidx.activity.compose.BackHandler(enabled = activeSimilarSection != null) {
        activeSimilarSection = null
    }

    // Recently played from LibraryManager
    val recentlyPlayed by LibraryManager.recentlyPlayed.collectAsState()

    // Initial load — restore from cache first, then fetch youtube homepage in background
    LaunchedEffect(Unit) {
        if (!state.isLoaded) {
            // Restore from cache immediately
            val cachedSuggestionsStr = LibraryManager.getString("cache_featured_suggestions")
            val cachedQuickPicksStr = LibraryManager.getString("cache_quick_picks")
            val cachedSeleccionesStr = LibraryManager.getString("cache_selecciones")
            val cachedPlaylistsStr = LibraryManager.getString("cache_playlists")
            val cachedSimilarStr = LibraryManager.getString("cache_similar_sections")
            val cachedTitle = LibraryManager.getString("cache_selecciones_title")

            if (!cachedSuggestionsStr.isNullOrBlank()) {
                state.featuredSuggestions = deserializeYTItemList(cachedSuggestionsStr)
                state.quickPickSongs = deserializeYTItemList(cachedQuickPicksStr ?: "").filterIsInstance<SongItem>()
                state.seleccionesParaTi = deserializeYTItemList(cachedSeleccionesStr ?: "").filterIsInstance<SongItem>()
                state.featuredPlaylists = deserializeYTItemList(cachedPlaylistsStr ?: "").filterIsInstance<PlaylistItem>()
                state.similarSections = deserializeSimilarSections(cachedSimilarStr ?: "")
                state.seleccionesTitle = cachedTitle
                state.isLoaded = true
            }

            withContext(Dispatchers.IO) {
                val page = YouTube.home().getOrNull()
                if (page != null) {
                    state.homePage = page
                    // Fetch 2 continuations for richer data
                    var current: com.echo.innertube.pages.HomePage = page
                    var fetched = 0
                    while (current.continuation != null && fetched < 2) {
                        val cont = current.continuation ?: break
                        val next = YouTube.home(continuation = cont).getOrNull()
                        if (next != null) {
                            current = next
                            state.homePage = state.homePage?.copy(
                                sections = (state.homePage?.sections ?: emptyList()) + next.sections,
                                continuation = next.continuation
                            )
                            fetched++
                        } else break
                    }

                    val sects = state.homePage?.sections ?: emptyList()
                    val similarList = mutableListOf<SimilarSection>()
                    val suggestionsList = mutableListOf<YTItem>()
                    var seleccionesTitleTemp: String? = null
                    var seleccionesListTemp: List<SongItem> = emptyList()
                    var quickPicksTemp: List<SongItem> = emptyList()
                    val playlistList = mutableListOf<PlaylistItem>()

                    for (section in sects) {
                        val title = section.title.lowercase()
                        playlistList.addAll(section.items.filterIsInstance<PlaylistItem>())
                        when {
                            title.contains("vuelve a escuchar") || title.contains("listen again") || title.contains("vuelve a") -> {
                                quickPicksTemp = section.items.filterIsInstance<SongItem>()
                            }
                            title.contains("similar a") || title.contains("similar to") -> {
                                val artistMatch = Regex("similar a (.*)", RegexOption.IGNORE_CASE).find(section.title)
                                    ?: Regex("similar to (.*)", RegexOption.IGNORE_CASE).find(section.title)
                                val artistName = artistMatch?.groupValues?.get(1)?.trim() ?: "Artistas similares"
                                similarList.add(SimilarSection(artistName, section.items))
                            }
                            title.contains("selecciones para ti") || title.contains("picks for you") || title.contains("mixes para ti") -> {
                                if (seleccionesListTemp.isEmpty()) {
                                    seleccionesListTemp = section.items.filterIsInstance<SongItem>()
                                    seleccionesTitleTemp = section.title
                                }
                            }
                            title.contains("recomendados") || title.contains("recommended") || title.contains("sugerencias destacadas") -> {
                                suggestionsList.addAll(section.items)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        state.similarSections = similarList
                        state.seleccionesParaTi = seleccionesListTemp
                        state.seleccionesTitle = seleccionesTitleTemp
                        state.featuredSuggestions = suggestionsList
                        state.quickPickSongs = quickPicksTemp
                        state.featuredPlaylists = playlistList.distinctBy { it.id }
                        state.isLoaded = true

                        // Cache the loaded data
                        LibraryManager.saveString("cache_featured_suggestions", serializeYTItemList(state.featuredSuggestions))
                        LibraryManager.saveString("cache_quick_picks", serializeYTItemList(state.quickPickSongs))
                        LibraryManager.saveString("cache_selecciones", serializeYTItemList(state.seleccionesParaTi))
                        LibraryManager.saveString("cache_playlists", serializeYTItemList(state.featuredPlaylists))
                        LibraryManager.saveString("cache_similar_sections", serializeSimilarSections(state.similarSections))
                        LibraryManager.saveString("cache_selecciones_title", state.seleccionesTitle)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        state.isLoaded = true
                    }
                }
            }
        }
    }

    // Dynamic Quick Picks + Similar sections mixed algorithm
    var algorithmSeeds by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }

    LaunchedEffect(recentlyPlayed) {
        val recentSongs = recentlyPlayed.filter { it.type == ItemType.SONG }
        if (recentSongs.isNotEmpty() && algorithmSeeds.isEmpty()) {
            // First load: pick 5 diverse seeds from the top 20 recently played
            algorithmSeeds = recentSongs.take(20).shuffled().take(5)
        } else if (recentSongs.isNotEmpty()) {
            // Update seeds when a new song is played to keep it fresh but mixed
            val latest = recentSongs.first()
            if (!algorithmSeeds.contains(latest)) {
                algorithmSeeds = (listOf(latest) + algorithmSeeds.take(4))
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
            val porqueEscuchasteList = mutableListOf<PorqueEscuchasteSection>()
            val allPlaylists = mutableListOf<com.echo.innertube.models.PlaylistItem>()

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
                allPlaylists.addAll(relatedPage.playlists)

                val availableSongs = relatedPage.songs.drop(11)
                allSuggestions.addAll(availableSongs.take(8))

                // Primary similar section for this seed
                val primaryItems = mutableListOf<com.echo.innertube.models.YTItem>()
                primaryItems.addAll(relatedPage.artists.take(10))
                primaryItems.addAll(availableSongs.drop(4).take(10))
                if (primaryItems.isNotEmpty()) {
                    sections.add(SimilarSection(
                        artistName = seedArtist,
                        items = primaryItems.shuffled()
                    ))
                }

                // Add a "Porque escuchaste a" section for this seed
                val seedSongs = relatedPage.songs.take(20)
                if (seedSongs.isNotEmpty() && seedArtist.isNotEmpty() && seedArtist != "Artistas") {
                    porqueEscuchasteList.add(PorqueEscuchasteSection(
                        artistName = seedArtist,
                        songs = seedSongs
                    ))
                }
            }

            state.quickPickSongs = allQuickPicks.distinctBy { it.id }.shuffled().take(12)
            state.seleccionesParaTi = allParaTi.distinctBy { it.id }.shuffled().take(20)
            state.porqueEscuchasteSections = porqueEscuchasteList.distinctBy { it.artistName }

            if (results.isNotEmpty()) {
                val primaryArtist = results.first().first
                state.seleccionesTitle = if (primaryArtist.isNotEmpty() && primaryArtist != "Artistas") primaryArtist else "Mix para ti"
            }

            state.featuredSuggestions = allSuggestions.distinctBy {
                when (it) {
                    is SongItem -> it.id
                    is com.echo.innertube.models.AlbumItem -> it.id
                    is com.echo.innertube.models.ArtistItem -> it.id
                    else -> it.toString()
                }
            }.shuffled().take(15)

            // Extract playlists from homePage
            val homePlaylists = state.homePage?.sections?.flatMap { it.items.filterIsInstance<com.echo.innertube.models.PlaylistItem>() } ?: emptyList()
            state.featuredPlaylists = (allPlaylists + homePlaylists).distinctBy { it.id }.shuffled().take(10)

            // Merge similar sections from seeds and homePage
            val currentSimilar = state.similarSections
            state.similarSections = (sections + currentSimilar).distinctBy { it.artistName }.take(12)

            // Cache the updated recommendations
            LibraryManager.saveString("cache_featured_suggestions", serializeYTItemList(state.featuredSuggestions))
            LibraryManager.saveString("cache_quick_picks", serializeYTItemList(state.quickPickSongs))
            LibraryManager.saveString("cache_selecciones", serializeYTItemList(state.seleccionesParaTi))
            LibraryManager.saveString("cache_playlists", serializeYTItemList(state.featuredPlaylists))
            LibraryManager.saveString("cache_similar_sections", serializeSimilarSections(state.similarSections))
            LibraryManager.saveString("cache_selecciones_title", state.seleccionesTitle)
        }
    }

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
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
                text = stringResource(R.string.nav_inicio),
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
        val displaySuggestions = if (state.featuredSuggestions.isNotEmpty()) state.featuredSuggestions 
                                 else state.homePage?.sections?.firstOrNull()?.items ?: emptyList()
                                 
        if (displaySuggestions.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.sugerencias_destacadas),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displaySuggestions.take(15).size) { index ->
                        val itm = displaySuggestions[index]
                        FeaturedSuggestionCard(
                            context = context,
                            item = itm,
                            onSongSelected = onSongSelected,
                            onAlbumSelected = onAlbumSelected,
                            onArtistSelected = onArtistSelected,
                            scrollState = listState
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }



        // ═══════════════════════════════════════════════════════════
        // QUICK PICKS — "Selecciones rápidas"
        // ═══════════════════════════════════════════════════════════
        if (state.quickPickSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.selecciones_rapidas), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2C2C2E))
                            .clickable {
                                if (state.quickPickSongs.isNotEmpty()) {
                                    val first = state.quickPickSongs.first()
                                    val hdThumb = upgradeThumbHD(first.thumbnail)
                                    onSongSelected(PlayerState(title = first.title, artist = first.artists.joinToString { it.name }, artUrl = hdThumb, videoId = first.id))
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play all", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.play_all), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.quickPickSongs.size) { index ->
                        val song = state.quickPickSongs[index]
                        val hdThumb = upgradeThumb(song.thumbnail)
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .clickable {
                                    onSongSelected(PlayerState(
                                        title = song.title,
                                        artist = song.artists.joinToString { it.name },
                                        artUrl = upgradeThumbHD(song.thumbnail),
                                        videoId = song.id
                                    ))
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    Text(stringResource(R.string.sigue_escuchando), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val itemsToDisplay = recentlyPlayed.take(20)
                    items(itemsToDisplay.size) { index ->
                        val item = itemsToDisplay[index]
                        val hdThumb = upgradeThumb(item.thumbnail)
                        val isCircle = item.type == ItemType.ARTIST
                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .wiggleOnScroll(item.id, lazyListState = listState)
                                .clickable {
                                    SharedTransitionState.lastClickBounds = imageCoords?.boundsInRoot()
                                    if (item.type == ItemType.SONG) {
                                        onSongSelected(PlayerState(
                                            title = item.title,
                                            artist = item.subtitle,
                                            artUrl = upgradeThumbHD(item.thumbnail),
                                            videoId = item.id
                                        ))
                                    } else {
                                        SharedTransitionState.lastOpenedId = item.id
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
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(180.dp)
                                    .onGloballyPositioned { imageCoords = it }
                                    .clip(if (isCircle) CircleShape else RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.subtitle, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // SIMILAR TO [ARTIST] — Multiple sections (Playlist destacada dynamically embedded)
        // ═══════════════════════════════════════════════════════════
        val similarSections = state.similarSections
        val showPlaylistDestacadaAfterIndex = if (similarSections.size >= 3) 2 else similarSections.size - 1

        if (similarSections.isEmpty() && state.featuredPlaylists.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.playlist_destacada),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.featuredPlaylists.size) { playlistIndex ->
                        val playlist = state.featuredPlaylists[playlistIndex]
                        val hdThumb = upgradeThumb(playlist.thumbnail)
                        
                        Column(
                            modifier = Modifier
                                .width(320.dp)
                                .clickable {
                                    onAlbumSelected(AlbumState(
                                        id = playlist.id,
                                        playlistId = playlist.id,
                                        title = playlist.title,
                                        artist = playlist.author?.name ?: "Playlist",
                                        thumbnail = playlist.thumbnail,
                                        year = null
                                    ))
                                }
                        ) {
                            Text(
                                text = stringResource(R.string.nos_encanta),
                                color = Color(0xFFFA243C).copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\"${playlist.title}\"",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1C1C1E))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                    contentDescription = playlist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.4f)
                                                ),
                                                startY = 300f
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            similarSections.forEachIndexed { index, section ->
                item {
                    Text(stringResource(R.string.similar_a), color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeSimilarSection = section }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                        items(section.items.size) { idx ->
                            val item = section.items[idx]
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
                                    .width(180.dp)
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
                                    modifier = Modifier.size(180.dp).clip(if (isCircle) CircleShape else RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(subtitle, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                if (index == showPlaylistDestacadaAfterIndex && state.featuredPlaylists.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.playlist_destacada),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.featuredPlaylists.size) { playlistIndex ->
                                val playlist = state.featuredPlaylists[playlistIndex]
                                val hdThumb = upgradeThumb(playlist.thumbnail)
                                
                                Column(
                                    modifier = Modifier
                                        .width(320.dp)
                                        .clickable {
                                            onAlbumSelected(AlbumState(
                                                id = playlist.id,
                                                playlistId = playlist.id,
                                                title = playlist.title,
                                                artist = playlist.author?.name ?: "Playlist",
                                                thumbnail = playlist.thumbnail,
                                                year = null
                                            ))
                                        }
                                ) {
                                    Text(
                                        text = stringResource(R.string.nos_encanta),
                                        color = Color(0xFFFA243C).copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "\"${playlist.title}\"",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(320.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF1C1C1E))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                            contentDescription = playlist.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            Color.Black.copy(alpha = 0.4f)
                                                        ),
                                                        startY = 300f
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        // (Old Escuchado Recientemente block removed in favor of Sigue escuchando)

        // ═══════════════════════════════════════════════════════════
        // Secciones "Porque escuchaste a..." (Dynamic multiple sections)
        // ═══════════════════════════════════════════════════════════
        if (state.porqueEscuchasteSections.isNotEmpty()) {
            state.porqueEscuchasteSections.forEach { section ->
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(stringResource(R.string.porque_escuchaste), color = Color.Gray, fontSize = 13.sp)
                        Text(section.artistName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(section.songs.size) { index ->
                            val song = section.songs[index]
                            val hdThumb = upgradeThumb(song.thumbnail)
                            Column(
                                modifier = Modifier.width(180.dp).clickable {
                                    onSongSelected(PlayerState(title = song.title, artist = song.artists.joinToString { it.name }, artUrl = upgradeThumbHD(song.thumbnail), videoId = song.id))
                                }
                            ) {
                                Box(
                                    modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1C1C1E))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else if (state.seleccionesParaTi.isNotEmpty() && state.seleccionesTitle != null) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.porque_escuchaste), color = Color.Gray, fontSize = 13.sp)
                    Text(state.seleccionesTitle!!, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.seleccionesParaTi.size) { index ->
                        val song = state.seleccionesParaTi[index]
                        val hdThumb = upgradeThumb(song.thumbnail)
                        Column(
                            modifier = Modifier.width(180.dp).clickable {
                                onSongSelected(PlayerState(title = song.title, artist = song.artists.joinToString { it.name }, artUrl = upgradeThumbHD(song.thumbnail), videoId = song.id))
                            }
                        ) {
                            Box(
                                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1C1C1E))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // YouTube Music Home Page Sections (skip first since we used it for featured)
        // ═══════════════════════════════════════════════════════════
        if (state.homePage != null) {
            // Skip first section since it's used for featured cards
            state.homePage!!.sections.drop(1).forEach { section ->
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

                                // Standard card (180dp for consistency with recently played)
                                var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                                Column(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .clickable {
                                            SharedTransitionState.lastClickBounds = imageCoords?.boundsInRoot()
                                            clickAction()
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(180.dp)
                                            .onGloballyPositioned { imageCoords = it }
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
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(titleStr, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(subtitleStr, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════
        // REPLAY SECTION — "Replay: La música que más escuchas"
        // ═══════════════════════════════════════════════════════════
        item {
            SectionTitle(stringResource(R.string.replay_title), isDark = true, small = false)
            SectionTitle(stringResource(R.string.replay_subtitle), isDark = true, small = true)
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(280.dp)
                    .height(380.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF9500), // Orange/yellow
                                Color(0xFFFF2D55), // Pink/Red
                                Color(0xFF5856D6), // Purple
                                Color(0xFF5AC8FA)  // Cyan
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                        )
                    )
                    .wiggleOnScroll("replay_home_card", lazyListState = listState)
                    .clickable {
                        onReplaySelected()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Replay",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.replay_card_headline_1),
                            color = Color(0xFFFFCC00), // Golden Yellow
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 36.sp
                        )
                        Text(
                            text = stringResource(R.string.replay_card_headline_2),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 36.sp
                        )
                        Text(
                            text = stringResource(R.string.replay_card_headline_3),
                            color = Color(0xFF5AC8FA), // Cyan/Light Blue
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 36.sp
                        )
                    }
                    
                    Text(
                        text = stringResource(R.string.replay_card_footer),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    } // Cierra LazyColumn

    // Overlay similar
    androidx.compose.animation.AnimatedVisibility(
        visible = activeSimilarSection != null,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxSize()
    ) {
        val section = activeSimilarSection
        if (section != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = { activeSimilarSection = null }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back_action),
                            tint = Color(0xFFFA243C)
                        )
                    }
                    Text(
                        text = section.artistName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    state = similarGridState,
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 180.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(section.items.size) { index ->
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
                        val itemId = when (item) {
                            is com.echo.innertube.models.ArtistItem -> item.id
                            is com.echo.innertube.models.SongItem -> item.id
                            is com.echo.innertube.models.AlbumItem -> item.id
                            else -> item.hashCode().toString()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wiggleOnScroll(itemId, similarGridState)
                                .clickable {
                                    when (item) {
                                        is com.echo.innertube.models.ArtistItem -> {
                                            onArtistSelected(com.mrtdk.liquid_glass.ui.screens.ArtistState(item.id, item.title, item.thumbnail))
                                        }
                                        is com.echo.innertube.models.SongItem -> {
                                            onSongSelected(PlayerState(item.title, item.artists.joinToString { it.name }, upgradeThumbHD(item.thumbnail), item.id))
                                        }
                                        is com.echo.innertube.models.AlbumItem -> {
                                            onAlbumSelected(com.mrtdk.liquid_glass.ui.screens.AlbumState(item.id, item.playlistId ?: item.id, item.title, item.artists?.joinToString { it.name } ?: "Varios", item.thumbnail, item.year as? Int))
                                        }
                                        else -> {}
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(if (isCircle) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                color = Color.Gray,
                                fontSize = 12.sp,
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
    onArtistSelected: (ArtistState) -> Unit,
    scrollState: androidx.compose.foundation.lazy.LazyListState
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
            clickAction = { 
                SharedTransitionState.lastOpenedId = item.id
                onAlbumSelected(AlbumState(id = item.id, playlistId = item.playlistId ?: item.id, title = titleStr, artist = subtitleStr, thumbnail = thumbUrl, year = item.year as? Int ?: item.year?.toString()?.toIntOrNull())) 
            }
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
            clickAction = { 
                SharedTransitionState.lastOpenedId = item.id
                onAlbumSelected(AlbumState(id = item.id, playlistId = item.id, title = titleStr, artist = subtitleStr, thumbnail = thumbUrl, year = null)) 
            }
        }
        else -> {
            titleStr = "Unknown"
            subtitleStr = ""
        }
    }

    val hdThumb = upgradeThumb(thumbUrl)
    var dominantColor by remember { mutableStateOf(Color(0xFF1C1C1E)) }
    var coverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(hdThumb) {
        if (hdThumb != null) {
            val request = ImageRequest.Builder(context)
                .data(hdThumb)
                .allowHardware(false)
                .size(150)
                .build()
            val result = Coil.imageLoader(context).execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                val bitmap = (drawable as? BitmapDrawable)?.bitmap
                    ?: Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    ).also {
                        val canvas = Canvas(it)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                    }
                
                coverBitmap = bitmap.asImageBitmap()
                try {
                    var r = 0L; var g = 0L; var b = 0L
                    val y = bitmap.height - 1
                    val w = bitmap.width
                    for (x in 0 until w) {
                        val pixel = bitmap.getPixel(x, y)
                        r += android.graphics.Color.red(pixel)
                        g += android.graphics.Color.green(pixel)
                        b += android.graphics.Color.blue(pixel)
                    }
                    dominantColor = Color((r / w).toInt(), (g / w).toInt(), (b / w).toInt())
                } catch (e: Exception) { }
            }
        }
    }

    // Card container
    var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(380.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(dominantColor)
            .wiggleOnScroll(item.id, lazyListState = scrollState)
            .clickable {
                SharedTransitionState.lastClickBounds = imageCoords?.boundsInRoot()
                clickAction()
            }
    ) {
        // Capa 1: Reflejo Líquido Estirado 1D
        val currentCoverBitmap = coverBitmap
        if (currentCoverBitmap != null) {
            val overlapDp = 270.dp * 0.20f // 54.dp
            Box(
                modifier = Modifier
                    .offset(y = 270.dp - overlapDp) // Empieza a los 216.dp
                    .size(width = 280.dp, height = 110.dp + overlapDp) // Altura es 164.dp
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
                                    0.05f to Color.Black,
                                    0.15f to Color.Black.copy(alpha = 0.9f),
                                    0.30f to Color.Black.copy(alpha = 0.7f),
                                    0.45f to Color.Black.copy(alpha = 0.45f),
                                    0.60f to Color.Black.copy(alpha = 0.25f),
                                    0.75f to Color.Black.copy(alpha = 0.10f),
                                    0.88f to Color.Black.copy(alpha = 0.03f),
                                    1.0f to Color.Transparent
                                )
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sampleHeight = 5
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
        } else {
            // Fallback while loading
            Box(
                modifier = Modifier
                    .offset(y = 270.dp)
                    .size(width = 280.dp, height = 110.dp)
                    .background(dominantColor)
            )
        }

        // Capa 3: Portada Principal con fundido a transparente en la parte inferior
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = 280.dp, height = 270.dp)
                .onGloballyPositioned { imageCoords = it }
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Black,
                                0.80f to Color.Black,
                                1f to Color.Transparent
                            )
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                contentDescription = titleStr,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Text Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(110.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            val isLightBackground = dominantColor.luminance() > 0.5f
            val titleColor = if (isLightBackground) Color(0xFF1C1C1E) else Color.White
            val subtitleColor = if (isLightBackground) Color(0xFF5E5E62) else Color.White.copy(alpha = 0.7f)

            Text(
                titleStr,
                color = titleColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitleStr,
                color = subtitleColor,
                fontSize = 14.sp,
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
            it.contains("=w") || it.contains("=s") -> {
                val idx = it.indexOf("=w").takeIf { i -> i != -1 } ?: it.indexOf("=s")
                it.substring(0, idx) + "=w540-h540-l90-rj"
            }
            it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
            else -> it
        }
    }
}

private fun upgradeThumbHD(url: String?): String? {
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

private fun serializeYTItemList(list: List<YTItem>): String {
    val array = JSONArray()
    for (item in list) {
        val obj = JSONObject()
        when (item) {
            is SongItem -> {
                obj.put("type", "SongItem")
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("thumbnail", item.thumbnail)
                obj.put("explicit", item.explicit)
                val artistsArray = JSONArray()
                item.artists.forEach { artistsArray.put(it.name) }
                obj.put("artists", artistsArray)
            }
            is com.echo.innertube.models.AlbumItem -> {
                obj.put("type", "AlbumItem")
                obj.put("browseId", item.browseId)
                obj.put("playlistId", item.playlistId)
                obj.put("title", item.title)
                obj.put("thumbnail", item.thumbnail)
                obj.put("year", item.year ?: -1)
                val artistsArray = JSONArray()
                item.artists?.forEach { artistsArray.put(it.name) }
                obj.put("artists", artistsArray)
            }
            is com.echo.innertube.models.ArtistItem -> {
                obj.put("type", "ArtistItem")
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("thumbnail", item.thumbnail ?: "")
            }
            is PlaylistItem -> {
                obj.put("type", "PlaylistItem")
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("thumbnail", item.thumbnail ?: "")
                obj.put("author", item.author?.name ?: "")
            }
            else -> {}
        }
        array.put(obj)
    }
    return array.toString()
}

private fun deserializeYTItemList(jsonStr: String): List<YTItem> {
    if (jsonStr.isBlank()) return emptyList()
    val list = mutableListOf<YTItem>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val type = obj.getString("type")
            when (type) {
                "SongItem" -> {
                    val artistsList = mutableListOf<Artist>()
                    val artistsArray = obj.getJSONArray("artists")
                    for (j in 0 until artistsArray.length()) {
                        artistsList.add(Artist(artistsArray.getString(j), null))
                    }
                    list.add(SongItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artists = artistsList,
                        thumbnail = obj.getString("thumbnail"),
                        explicit = obj.optBoolean("explicit", false)
                    ))
                }
                "AlbumItem" -> {
                    val artistsList = mutableListOf<Artist>()
                    if (obj.has("artists")) {
                        val artistsArray = obj.getJSONArray("artists")
                        for (j in 0 until artistsArray.length()) {
                            artistsList.add(Artist(artistsArray.getString(j), null))
                        }
                    }
                    val yr = obj.optInt("year", -1)
                    list.add(com.echo.innertube.models.AlbumItem(
                        browseId = obj.getString("browseId"),
                        playlistId = obj.getString("playlistId"),
                        title = obj.getString("title"),
                        artists = if (artistsList.isEmpty()) null else artistsList,
                        year = if (yr == -1) null else yr,
                        thumbnail = obj.getString("thumbnail")
                    ))
                }
                "ArtistItem" -> {
                    list.add(com.echo.innertube.models.ArtistItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        thumbnail = obj.optString("thumbnail").takeIf { it.isNotBlank() },
                        shuffleEndpoint = null,
                        radioEndpoint = null
                    ))
                }
                "PlaylistItem" -> {
                    val authorName = obj.optString("author")
                    list.add(PlaylistItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        author = if (authorName.isNullOrBlank()) null else Artist(authorName, null),
                        songCountText = null,
                        thumbnail = obj.optString("thumbnail").takeIf { it.isNotBlank() },
                        playEndpoint = null,
                        shuffleEndpoint = null,
                        radioEndpoint = null
                    ))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun serializeSimilarSections(list: List<SimilarSection>): String {
    val array = JSONArray()
    for (sec in list) {
        val obj = JSONObject()
        obj.put("artistName", sec.artistName)
        obj.put("items", serializeYTItemList(sec.items))
        array.put(obj)
    }
    return array.toString()
}

private fun deserializeSimilarSections(jsonStr: String): List<SimilarSection> {
    if (jsonStr.isBlank()) return emptyList()
    val list = mutableListOf<SimilarSection>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val artistName = obj.getString("artistName")
            val itemsJson = obj.getString("items")
            list.add(SimilarSection(artistName, deserializeYTItemList(itemsJson)))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}