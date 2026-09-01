package com.mrtdk.liquid_glass.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.echo.innertube.YouTube
import com.echo.innertube.models.AlbumItem
import com.echo.innertube.models.ArtistItem
import com.echo.innertube.models.PlaylistItem
import com.echo.innertube.models.SongItem
import com.mrtdk.glass.GlassBoxScope
import com.mrtdk.glass.GlassContainer
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.data.RecentSearchItem
import com.mrtdk.liquid_glass.playback.PlaybackQueue
import com.mrtdk.liquid_glass.ui.components.AppleMusicAlbumMenu
import com.mrtdk.liquid_glass.ui.components.AppleMusicSongMenu
import com.mrtdk.liquid_glass.ui.components.ContextMenuAlbum
import com.mrtdk.liquid_glass.ui.components.ContextMenuSong
import com.mrtdk.liquid_glass.ui.theme.ThemeManager
import com.mrtdk.liquid_glass.ui.utils.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader

data class SearchCategory(val name: String, val imageUrl: String)

fun loadCategories(context: android.content.Context): List<SearchCategory> {
    return try {
        val inputStream = context.assets.open("datos.json")
        val jsonStr = InputStreamReader(inputStream).readText()
        val jsonArray = JSONArray(jsonStr)
        val result = mutableListOf<SearchCategory>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val name = obj.getString("name")
            val remoteUrl = obj.optString("url")
            val finalUrl = if (!remoteUrl.isNullOrEmpty() && remoteUrl.startsWith("http")) {
                remoteUrl
            } else {
                val fileName = name.replace("&", "").replace(",", "") + ".webp"
                "file:///android_asset/img/$fileName"
            }
            result.add(SearchCategory(name, finalUrl))
        }
        result
    } catch (e: Exception) {
        emptyList()
    }
}

class BusquedaState {
    var displayResults by mutableStateOf<List<Any>>(emptyList())
    var quickSongs by mutableStateOf<List<SongItem>>(emptyList())
    var quickArtists by mutableStateOf<List<ArtistItem>>(emptyList())
    var suggestions by mutableStateOf<List<String>>(emptyList())
    var isSearching by mutableStateOf(false)
    var isQuickSearching by mutableStateOf(false)
    var selectedTab by mutableIntStateOf(0)
    var searchSource by mutableIntStateOf(0) // 0 = RayMusic, 1 = Biblioteca
    var isFullResultsMode by mutableStateOf(false)
    var lastFullQuery by mutableStateOf("")
    var lastFullTab by mutableIntStateOf(-1)
}

@Composable
fun BusquedaScreen(
    innerPadding: PaddingValues,
    query: String,
    isSubmitted: Boolean,
    state: BusquedaState = remember { BusquedaState() },
    onSongSelected: (PlayerState) -> Unit,
    onArtistSelected: (ArtistState) -> Unit = {},
    onAlbumSelected: (AlbumState) -> Unit = {},
    onVideoSelected: (String) -> Unit = {},
    onCategorySelected: (SearchCategory) -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onSubmitChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activeSongForMenu by remember { mutableStateOf<ContextMenuSong?>(null) }
    var activeAlbumForMenu by remember { mutableStateOf<ContextMenuAlbum?>(null) }
    val tabNames = listOf(
        stringResource(R.string.search_tab_top),
        stringResource(R.string.search_tab_artists),
        stringResource(R.string.search_tab_albums),
        stringResource(R.string.search_tab_songs)
    )

    val recentSearches by LibraryManager.recentSearches.collectAsState()
    val savedLibraryItems by LibraryManager.savedItems.collectAsState()
    val downloadedLibraryItems by LibraryManager.downloadedSongs.collectAsState()

    // If query is empty or changed, reset full results mode if user types new characters
    var previousQuery by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (query != previousQuery) {
            previousQuery = query
            if (query.isEmpty()) {
                state.isFullResultsMode = false
                state.displayResults = emptyList()
                state.suggestions = emptyList()
                state.quickSongs = emptyList()
                state.quickArtists = emptyList()
            } else if (!isSubmitted) {
                state.isFullResultsMode = false
            }
        }
    }

    // Synchronize isSubmitted with state.isFullResultsMode
    LaunchedEffect(isSubmitted) {
        if (isSubmitted && query.isNotBlank()) {
            state.isFullResultsMode = true
        }
    }

    // 1. Quick suggestions & preview results when typing in RayMusic mode (Mode B)
    LaunchedEffect(query, state.searchSource, state.isFullResultsMode) {
        if (state.searchSource != 0 || state.isFullResultsMode || query.isBlank()) {
            state.isQuickSearching = false
            if (query.isBlank()) {
                state.suggestions = emptyList()
                state.quickSongs = emptyList()
                state.quickArtists = emptyList()
            }
            return@LaunchedEffect
        }

        delay(180) // debounce keystrokes
        state.isQuickSearching = true

        try {
            withContext(Dispatchers.IO) {
                // Fetch suggestions
                val suggRes = try {
                    YouTube.searchSuggestions(query).getOrNull()
                } catch (_: Exception) { null }

                val suggestionsList = suggRes?.queries?.take(4) ?: emptyList()

                // Fetch quick artists and songs
                val quickArtistsList = try {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()?.items?.filterIsInstance<ArtistItem>()?.take(2) ?: emptyList()
                } catch (_: Exception) { emptyList() }

                val quickSongsList = try {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterIsInstance<SongItem>()?.take(4) ?: emptyList()
                } catch (_: Exception) { emptyList() }

                state.suggestions = suggestionsList
                state.quickArtists = quickArtistsList
                state.quickSongs = quickSongsList
            }
        } catch (_: kotlin.coroutines.cancellation.CancellationException) {
            // Cancelled due to new keystroke, normal behavior
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            state.isQuickSearching = false
        }
    }

    // 2. Full Search Results (Mode C)
    LaunchedEffect(query, state.selectedTab, state.isFullResultsMode, state.searchSource) {
        if (state.searchSource != 0 || !state.isFullResultsMode || query.length < 2) {
            if (!state.isFullResultsMode && query.length < 2) {
                state.displayResults = emptyList()
            }
            return@LaunchedEffect
        }

        if (state.lastFullQuery == query && state.lastFullTab == state.selectedTab && state.displayResults.isNotEmpty() && !state.isSearching) {
            return@LaunchedEffect
        }

        state.isSearching = true
        state.lastFullQuery = query
        state.lastFullTab = state.selectedTab

        try {
            withContext(Dispatchers.IO) {
                if (state.selectedTab == 0) {
                    // Top Results: mixed results
                    val songRes = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                    if (songRes.isSuccess) {
                        val result = songRes.getOrNull()
                        val allSongs = result?.items?.filterIsInstance<SongItem>().orEmpty()
                        val songs = (allSongs.filter { !it.isVideoSong } + allSongs.filter { it.isVideoSong }).take(30)
                        val artists = try { YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()?.items?.filterIsInstance<ArtistItem>()?.take(3) ?: emptyList() } catch (_: Exception) { emptyList() }
                        val albums = try { YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()?.items?.filterIsInstance<AlbumItem>()?.take(3) ?: emptyList() } catch (_: Exception) { emptyList() }
                        state.displayResults = artists + albums + songs
                    } else {
                        val summaryRes = YouTube.searchSummary(query)
                        if (summaryRes.isSuccess) {
                            val items = summaryRes.getOrNull()?.summaries?.flatMap { it.items }.orEmpty()
                            state.displayResults = items
                        } else {
                            state.displayResults = emptyList()
                        }
                    }
                } else {
                    val filter = when (state.selectedTab) {
                        1 -> YouTube.SearchFilter.FILTER_ARTIST
                        2 -> YouTube.SearchFilter.FILTER_ALBUM
                        3 -> YouTube.SearchFilter.FILTER_SONG
                        else -> YouTube.SearchFilter.FILTER_SONG
                    }
                    val filterRes = YouTube.search(query, filter)
                    if (filterRes.isSuccess) {
                        val result = filterRes.getOrNull()
                        state.displayResults = when (state.selectedTab) {
                            1 -> result?.items?.filterIsInstance<ArtistItem>()?.take(30) ?: emptyList()
                            2 -> result?.items?.filterIsInstance<AlbumItem>()?.take(30) ?: emptyList()
                            else -> {
                                val allSongs = result?.items?.filterIsInstance<SongItem>().orEmpty()
                                (allSongs.filter { !it.isVideoSong } + allSongs.filter { it.isVideoSong }).take(30)
                            }
                        }
                    } else {
                        state.displayResults = emptyList()
                    }
                }
            }
        } catch (_: kotlin.coroutines.cancellation.CancellationException) {
            // Coroutine cancelled on tab switch or query change
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            state.isSearching = false
        }
    }

    val categories = remember { loadCategories(context) }
    val listState = rememberLazyListState()

    GlassContainer(
        modifier = Modifier.fillMaxSize(),
        useShader = true,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ThemeManager.backgroundColor)
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    )
            ) {
                // Top Origin Selector (RayMusic | Biblioteca) - Only shown while writing (Mode B), hidden on initial screen & full results
                val shouldShowSourceSelector = (query.isNotEmpty() && !state.isFullResultsMode) || (state.searchSource == 1 && !state.isFullResultsMode)
                if (shouldShowSourceSelector) {
                    SearchSourceSelector(
                        selectedSource = state.searchSource,
                        onSourceSelected = { state.searchSource = it },
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                if (state.searchSource == 1) {
                    // BIBLIOTECA SEARCH MODE
                    val filteredLocal = remember(query, savedLibraryItems, downloadedLibraryItems) {
                        val combined = (savedLibraryItems + downloadedLibraryItems).distinctBy { it.id }
                        if (query.isBlank()) combined
                        else combined.filter {
                            it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 180.dp)
                    ) {
                        if (query.isBlank()) {
                            item {
                                Text(
                                    text = stringResource(R.string.nav_biblioteca),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeManager.textColor,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }

                        if (filteredLocal.isEmpty() && query.isNotBlank()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No se encontraron elementos en tu biblioteca",
                                        color = ThemeManager.subtextColor,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredLocal, key = { it.id }) { item ->
                                LibrarySearchResultRow(
                                    item = item,
                                    onClick = {
                                        when (item.type) {
                                            ItemType.SONG -> {
                                                onSongSelected(
                                                    PlayerState(
                                                        title = item.title,
                                                        artist = item.subtitle,
                                                        artUrl = item.thumbnail,
                                                        videoId = item.id,
                                                        album = item.album
                                                    )
                                                )
                                            }
                                            ItemType.ARTIST -> {
                                                onArtistSelected(
                                                    ArtistState(
                                                        id = item.id,
                                                        name = item.title,
                                                        thumbnail = item.thumbnail
                                                    )
                                                )
                                            }
                                            ItemType.ALBUM -> {
                                                onAlbumSelected(
                                                    AlbumState(
                                                        id = item.id,
                                                        playlistId = item.id,
                                                        title = item.title,
                                                        artist = item.subtitle,
                                                        thumbnail = item.thumbnail
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else if (query.isEmpty()) {
                    // MODE A: INITIAL STATE (Recent searches or Categories) - Imagen 3
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 180.dp)
                    ) {
                        if (recentSearches.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Búsquedas recientes",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeManager.textColor
                                    )
                                    Text(
                                        text = "Borrar",
                                        color = ThemeManager.accentColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .bounceClick { LibraryManager.clearRecentSearches() }
                                            .padding(vertical = 4.dp, horizontal = 6.dp)
                                    )
                                }
                            }

                            items(recentSearches, key = { "${it.type}_${it.id}_${it.timestamp}" }) { recentItem ->
                                RecentSearchRow(
                                    item = recentItem,
                                    onClick = {
                                        when (recentItem.type) {
                                            "ARTIST" -> {
                                                onArtistSelected(
                                                    ArtistState(
                                                        id = recentItem.id,
                                                        name = recentItem.title,
                                                        thumbnail = recentItem.thumbnail
                                                    )
                                                )
                                            }
                                            "ALBUM" -> {
                                                onAlbumSelected(
                                                    AlbumState(
                                                        id = recentItem.id,
                                                        playlistId = recentItem.albumId ?: recentItem.id,
                                                        title = recentItem.title,
                                                        artist = recentItem.subtitle,
                                                        thumbnail = recentItem.thumbnail
                                                    )
                                                )
                                            }
                                            else -> {
                                                onSongSelected(
                                                    PlayerState(
                                                        title = recentItem.title,
                                                        artist = recentItem.subtitle,
                                                        artUrl = recentItem.thumbnail,
                                                        videoId = recentItem.id,
                                                        album = recentItem.album,
                                                        albumId = recentItem.albumId
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onMoreClick = {
                                        if (recentItem.type == "SONG") {
                                            activeSongForMenu = ContextMenuSong(
                                                id = recentItem.id,
                                                title = recentItem.title,
                                                artist = recentItem.subtitle,
                                                thumbnail = recentItem.thumbnail,
                                                album = recentItem.album,
                                                albumId = recentItem.albumId
                                            )
                                        }
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Explorar géneros",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeManager.textColor,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.search_action),
                                        color = ThemeManager.textColor,
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Grid of Categories
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                categories.chunked(2).forEach { rowCategories ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowCategories.forEach { category ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1.5f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(ThemeManager.surfaceColor)
                                                    .clickable { onCategorySelected(category) }
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(category.imageUrl)
                                                        .crossfade(false)
                                                        .build(),
                                                    contentDescription = category.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.25f))
                                                )
                                                Text(
                                                    text = category.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(12.dp)
                                                )
                                            }
                                        }
                                        if (rowCategories.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (!state.isFullResultsMode) {
                    // MODE B: WRITING / INSTANT SUGGESTIONS & PREVIEW (Imagen 4)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 180.dp)
                    ) {
                        // Autocompletion text suggestions
                        if (state.suggestions.isNotEmpty()) {
                            items(state.suggestions) { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onQueryChange(suggestion)
                                            onSubmitChange(true)
                                            state.isFullResultsMode = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = ThemeManager.subtextColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = suggestion,
                                        color = ThemeManager.textColor,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Quick Artist Matches
                        if (state.quickArtists.isNotEmpty()) {
                            items(state.quickArtists, key = { "quick_art_${it.id}" }) { artist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            LibraryManager.addRecentSearch(
                                                RecentSearchItem(
                                                    id = artist.id,
                                                    title = artist.title,
                                                    subtitle = "Artista",
                                                    thumbnail = artist.thumbnail,
                                                    type = "ARTIST"
                                                )
                                            )
                                            onArtistSelected(
                                                ArtistState(
                                                    id = artist.id,
                                                    name = artist.title,
                                                    thumbnail = artist.thumbnail
                                                )
                                            )
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color.DarkGray)
                                    ) {
                                        com.mrtdk.liquid_glass.spotify.SpotifyArtistAvatar(
                                            artistName = artist.title,
                                            fallbackUrl = artist.thumbnail,
                                            contentDescription = artist.title,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artist.title,
                                            color = ThemeManager.textColor,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stringResource(R.string.search_type_artist),
                                            color = ThemeManager.subtextColor,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = ThemeManager.subtextColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Quick Song Matches
                        if (state.quickSongs.isNotEmpty()) {
                            items(state.quickSongs, key = { "quick_song_${it.id}" }) { song ->
                                val hdThumb = song.thumbnail.let {
                                    when {
                                        it.contains("=w") -> it.substringBefore("=w") + "=w1200-h1200-l90-rj"
                                        it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                        else -> it
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val songItem = RecentSearchItem(
                                                id = song.id,
                                                title = song.title,
                                                subtitle = song.artists.joinToString { it.name },
                                                thumbnail = hdThumb,
                                                type = "SONG",
                                                album = song.album?.name,
                                                albumId = song.album?.id
                                            )
                                            LibraryManager.addRecentSearch(songItem)
                                            onSongSelected(
                                                PlayerState(
                                                    title = song.title,
                                                    artist = song.artists.joinToString { it.name },
                                                    artUrl = hdThumb,
                                                    videoId = song.id,
                                                    album = song.album?.name,
                                                    albumId = song.album?.id
                                                )
                                            )
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.DarkGray)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(hdThumb).crossfade(false).build(),
                                            contentDescription = "Art",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = ThemeManager.textColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${stringResource(R.string.search_type_song)} · ${song.artists.joinToString { it.name }}",
                                            color = ThemeManager.subtextColor,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = {
                                        activeSongForMenu = ContextMenuSong(
                                            id = song.id,
                                            title = song.title,
                                            artist = song.artists.joinToString { it.name },
                                            thumbnail = hdThumb,
                                            album = song.album?.name,
                                            artistId = song.artists.firstOrNull()?.id,
                                            albumId = song.album?.id
                                        )
                                    }) {
                                        Icon(Icons.Default.MoreHoriz, null, tint = ThemeManager.subtextColor)
                                    }
                                }
                            }
                        }

                        // "Mostrar todos los resultados" Button at the bottom (Imagen 4)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        onSubmitChange(true)
                                        state.isFullResultsMode = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Mostrar todos los resultados",
                                    color = ThemeManager.accentColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // MODE C: FULL SEARCH RESULTS (Imagen 5)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 180.dp)
                    ) {
                        // Filter tabs row
                        item {
                            LazyRow(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(tabNames.size) { i ->
                                    val isSelected = i == state.selectedTab
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isSelected) ThemeManager.accentColor
                                                else ThemeManager.surfaceColor
                                            )
                                            .clickable { state.selectedTab = i }
                                            .padding(horizontal = 18.dp, vertical = 9.dp)
                                    ) {
                                        Text(
                                            text = tabNames[i],
                                            color = if (isSelected) Color.White else ThemeManager.subtextColor,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Loading indicator
                        if (state.isSearching) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = ThemeManager.accentColor)
                                }
                            }
                        }

                        // Full Results list
                        if (state.displayResults.isNotEmpty()) {
                            items(
                                count = state.displayResults.size,
                                key = { index ->
                                    when (val itm = state.displayResults[index]) {
                                        is SongItem -> "song_${itm.id}"
                                        is AlbumItem -> "album_${itm.id}"
                                        is ArtistItem -> "artist_${itm.id}"
                                        is PlaylistItem -> "playlist_${itm.id}"
                                        else -> "$index"
                                    }
                                },
                                contentType = { index ->
                                    state.displayResults[index]::class.java.simpleName
                                }
                            ) { index ->
                                val item = state.displayResults[index]

                                when (item) {
                                    is SongItem -> {
                                        val hdThumb = item.thumbnail.let {
                                            when {
                                                it.contains("=w") -> it.substringBefore("=w") + "=w1200-h1200-l90-rj"
                                                it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                                else -> it
                                            }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    LibraryManager.addRecentSearch(
                                                        RecentSearchItem(
                                                            id = item.id,
                                                            title = item.title,
                                                            subtitle = item.artists.joinToString { it.name },
                                                            thumbnail = hdThumb,
                                                            type = "SONG",
                                                            album = item.album?.name,
                                                            albumId = item.album?.id
                                                        )
                                                    )
                                                    onSongSelected(
                                                        PlayerState(
                                                            title = item.title,
                                                            artist = item.artists.joinToString { it.name },
                                                            artUrl = hdThumb,
                                                            videoId = item.id,
                                                            album = item.album?.name,
                                                            albumId = item.album?.id
                                                        )
                                                    )
                                                }
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.DarkGray)
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(hdThumb).crossfade(false).build(),
                                                    contentDescription = "Art",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    item.title,
                                                    color = ThemeManager.textColor,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${stringResource(R.string.search_type_song)} · ${item.artists.joinToString { it.name }}",
                                                    color = ThemeManager.subtextColor,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(onClick = {
                                                activeSongForMenu = ContextMenuSong(
                                                    id = item.id,
                                                    title = item.title,
                                                    artist = item.artists.joinToString { it.name },
                                                    thumbnail = hdThumb,
                                                    album = item.album?.name,
                                                    artistId = item.artists.firstOrNull()?.id,
                                                    albumId = item.album?.id
                                                )
                                            }) {
                                                Icon(Icons.Default.MoreVert, null, tint = ThemeManager.subtextColor)
                                            }
                                        }
                                    }
                                    is ArtistItem -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    LibraryManager.addRecentSearch(
                                                        RecentSearchItem(
                                                            id = item.id,
                                                            title = item.title,
                                                            subtitle = "Artista",
                                                            thumbnail = item.thumbnail,
                                                            type = "ARTIST"
                                                        )
                                                    )
                                                    onArtistSelected(
                                                        ArtistState(
                                                            id = item.id,
                                                            name = item.title,
                                                            thumbnail = item.thumbnail
                                                        )
                                                    )
                                                }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.DarkGray)
                                            ) {
                                                com.mrtdk.liquid_glass.spotify.SpotifyArtistAvatar(
                                                    artistName = item.title,
                                                    fallbackUrl = item.thumbnail,
                                                    contentDescription = item.title,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    item.title,
                                                    color = ThemeManager.textColor,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    stringResource(R.string.search_type_artist),
                                                    color = ThemeManager.subtextColor,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = ThemeManager.subtextColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    is AlbumItem -> {
                                        val hdThumb = item.thumbnail.let {
                                            when {
                                                it.contains("=w") -> it.substringBefore("=w") + "=w1200-h1200-l90-rj"
                                                it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                                else -> it
                                            }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    LibraryManager.addRecentSearch(
                                                        RecentSearchItem(
                                                            id = item.id,
                                                            title = item.title,
                                                            subtitle = item.artists?.joinToString { it.name } ?: "",
                                                            thumbnail = hdThumb,
                                                            type = "ALBUM"
                                                        )
                                                    )
                                                    onAlbumSelected(
                                                        AlbumState(
                                                            id = item.id,
                                                            playlistId = item.playlistId,
                                                            title = item.title,
                                                            artist = item.artists?.joinToString { it.name } ?: "",
                                                            thumbnail = hdThumb,
                                                            year = item.year
                                                        )
                                                    )
                                                }
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.DarkGray)
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(hdThumb).crossfade(false).build(),
                                                    contentDescription = "Album Art",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    item.title,
                                                    color = ThemeManager.textColor,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${stringResource(R.string.search_type_album)} · ${item.artists?.joinToString { it.name } ?: ""}",
                                                    color = ThemeManager.subtextColor,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(onClick = {
                                                activeAlbumForMenu = ContextMenuAlbum(
                                                    id = item.id,
                                                    playlistId = item.playlistId,
                                                    title = item.title,
                                                    artist = item.artists?.joinToString { it.name } ?: "",
                                                    thumbnail = hdThumb,
                                                    year = item.year
                                                )
                                            }) {
                                                Icon(Icons.Default.MoreVert, null, tint = ThemeManager.subtextColor)
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (!state.isSearching) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No hay resultados para \"$query\"",
                                        color = ThemeManager.subtextColor,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        glassContent = {
            // Context menus
            if (activeSongForMenu != null) {
                val song = activeSongForMenu!!
                AppleMusicSongMenu(
                    song = song,
                    onDismiss = { activeSongForMenu = null },
                    onGoToArtist = {
                        if (song.artistId != null) {
                            onArtistSelected(
                                ArtistState(
                                    id = song.artistId,
                                    name = song.artist,
                                    thumbnail = song.thumbnail
                                )
                            )
                        } else {
                            coroutineScope.launch(Dispatchers.IO) {
                                val artistRes = YouTube.search(song.artist, YouTube.SearchFilter.FILTER_ARTIST)
                                val artistItem = artistRes.getOrNull()?.items?.filterIsInstance<ArtistItem>()?.firstOrNull()
                                withContext(Dispatchers.Main) {
                                    if (artistItem != null) {
                                        onArtistSelected(
                                            ArtistState(
                                                id = artistItem.id,
                                                name = artistItem.title,
                                                thumbnail = artistItem.thumbnail
                                            )
                                        )
                                    } else {
                                        onArtistSelected(
                                            ArtistState(
                                                id = "",
                                                name = song.artist,
                                                thumbnail = song.thumbnail
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onGoToAlbum = {
                        if (song.albumId != null) {
                            onAlbumSelected(
                                AlbumState(
                                    id = song.albumId,
                                    playlistId = song.albumId,
                                    title = song.album ?: "",
                                    artist = song.artist,
                                    thumbnail = song.thumbnail
                                )
                            )
                        } else if (song.album != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val albumRes = YouTube.search("${song.album} ${song.artist}", YouTube.SearchFilter.FILTER_ALBUM)
                                val albumItem = albumRes.getOrNull()?.items?.filterIsInstance<AlbumItem>()?.firstOrNull()
                                withContext(Dispatchers.Main) {
                                    if (albumItem != null) {
                                        onAlbumSelected(
                                            AlbumState(
                                                id = albumItem.id,
                                                playlistId = albumItem.id,
                                                title = albumItem.title,
                                                artist = albumItem.artists?.joinToString { it.name } ?: song.artist,
                                                thumbnail = albumItem.thumbnail
                                            )
                                        )
                                    } else {
                                        Toast.makeText(context, "Álbum no encontrado", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    onSongSelected = onSongSelected
                )
            }

            if (activeAlbumForMenu != null) {
                val album = activeAlbumForMenu!!
                AppleMusicAlbumMenu(
                    album = album,
                    onDismiss = { activeAlbumForMenu = null },
                    onAddAlbumToQueue = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val albumPage = YouTube.album(album.id).getOrNull()
                            val albumTracks = albumPage?.songs.orEmpty()
                            if (albumTracks.isNotEmpty()) {
                                val qItems = albumTracks.map { t ->
                                    QueueItem(
                                        title = t.title,
                                        artist = t.artists.joinToString { it.name },
                                        artUrl = album.thumbnail,
                                        videoId = t.id,
                                        album = album.title,
                                        albumId = album.id
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    PlaybackQueue.queue = PlaybackQueue.queue + qItems
                                    PlaybackQueue.onQueueChanged?.invoke()
                                    Toast.makeText(context, "Álbum añadido a la cola", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onSaveAlbumToLibrary = {
                        LibraryManager.saveItem(
                            LibraryItem(
                                id = album.id,
                                title = album.title,
                                subtitle = album.artist,
                                thumbnail = album.thumbnail,
                                type = ItemType.ALBUM
                            )
                        )
                        Toast.makeText(context, "Álbum guardado en la biblioteca", Toast.LENGTH_SHORT).show()
                    },
                    onGoToArtist = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val artistRes = YouTube.search(album.artist, YouTube.SearchFilter.FILTER_ARTIST)
                            val artistItem = artistRes.getOrNull()?.items?.filterIsInstance<ArtistItem>()?.firstOrNull()
                            withContext(Dispatchers.Main) {
                                if (artistItem != null) {
                                    onArtistSelected(
                                        ArtistState(
                                            id = artistItem.id,
                                            name = artistItem.title,
                                            thumbnail = artistItem.thumbnail
                                        )
                                    )
                                } else {
                                    onArtistSelected(
                                        ArtistState(
                                            id = "",
                                            name = album.artist,
                                            thumbnail = album.thumbnail
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun SearchSourceSelector(
    selectedSource: Int,
    onSourceSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val containerBg = if (isDarkMode) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)
    val activeBg = if (isDarkMode) Color(0xFF3A3A3C) else Color.White
    val activeTextColor = if (isDarkMode) Color.White else Color.Black
    val inactiveTextColor = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF8E8E93)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(containerBg)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val sources = listOf("RayMusic", stringResource(R.string.nav_biblioteca))
        sources.forEachIndexed { index, title ->
            val isSelected = selectedSource == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSelected) activeBg else Color.Transparent)
                    .clickable { onSourceSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) activeTextColor else inactiveTextColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RecentSearchRow(
    item: RecentSearchItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.type == "ARTIST") {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ) {
                com.mrtdk.liquid_glass.spotify.SpotifyArtistAvatar(
                    artistName = item.title,
                    fallbackUrl = item.thumbnail,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = ThemeManager.textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.search_type_artist),
                    color = ThemeManager.subtextColor,
                    fontSize = 13.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ThemeManager.subtextColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.thumbnail).crossfade(false).build(),
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = ThemeManager.textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitlePrefix = if (item.type == "ALBUM") stringResource(R.string.search_type_album) else stringResource(R.string.search_type_song)
                Text(
                    text = if (item.subtitle.isNotBlank()) "$subtitlePrefix · ${item.subtitle}" else subtitlePrefix,
                    color = ThemeManager.subtextColor,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreHoriz, null, tint = ThemeManager.subtextColor)
            }
        }
    }
}

@Composable
private fun LibrarySearchResultRow(
    item: LibraryItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isArtist = item.type == ItemType.ARTIST
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(if (isArtist) CircleShape else RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        ) {
            if (isArtist) {
                com.mrtdk.liquid_glass.spotify.SpotifyArtistAvatar(
                    artistName = item.title,
                    fallbackUrl = item.thumbnail,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.thumbnail).crossfade(false).build(),
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = ThemeManager.textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when (item.type) {
                    ItemType.SONG -> "${stringResource(R.string.search_type_song)} · ${item.subtitle}"
                    ItemType.ARTIST -> stringResource(R.string.search_type_artist)
                    ItemType.ALBUM -> "${stringResource(R.string.search_type_album)} · ${item.subtitle}"
                },
                color = ThemeManager.subtextColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isArtist) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ThemeManager.subtextColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
