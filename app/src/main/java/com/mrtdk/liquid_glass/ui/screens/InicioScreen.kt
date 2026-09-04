package com.mrtdk.liquid_glass.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import coil.Coil
import coil.request.SuccessResult

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.mrtdk.liquid_glass.ui.components.unclippedBoundsInRoot
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
import com.mrtdk.liquid_glass.data.MadeForYouPlaylist
import com.mrtdk.liquid_glass.data.MadeForYouRepository
import com.mrtdk.liquid_glass.ui.components.MadeForYouCardContent
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
import com.skydoves.cloudy.cloudy
import androidx.compose.foundation.Image
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
data class ElMundoDeArtist(val artistName: String, val albums: List<com.echo.innertube.models.AlbumItem>)

data class ArtistStation(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryThumb: String?,
    val secondaryThumbs: List<String>,
    val backgroundColor: Color,
    val isPersonal: Boolean = false,
    val isDiscovery: Boolean = false,
    val candidateSongs: List<SongItem> = emptyList()
)

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

    // Nuevas secciones
    var elMundoDeArtist by mutableStateOf<ElMundoDeArtist?>(null)
    var paraFiestasItems by mutableStateOf<List<com.echo.innertube.models.YTItem>>(emptyList())
    var madeForYouPlaylists by mutableStateOf<List<MadeForYouPlaylist>>(emptyList())
    var artistStations by mutableStateOf<List<ArtistStation>>(emptyList())
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
    onReplaySelected: () -> Unit = {},
    onListenTogetherSelected: () -> Unit = {}
) {
    val context = LocalContext.current
    var activeSimilarSection by remember { mutableStateOf<SimilarSection?>(null) }
    var similarSectionSnapshotBounds by remember { mutableStateOf<Map<String, androidx.compose.ui.geometry.Rect>>(emptyMap()) }
    val similarGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    var activeRecentlyPlayedSection by remember { mutableStateOf(false) }
    var recentlyPlayedSnapshotBounds by remember { mutableStateOf<Map<String, androidx.compose.ui.geometry.Rect>>(emptyMap()) }

    var activePorqueEscuchasteSection by remember { mutableStateOf<PorqueEscuchasteSection?>(null) }
    var porqueEscuchasteSnapshotBounds by remember { mutableStateOf<Map<String, androidx.compose.ui.geometry.Rect>>(emptyMap()) }

    var activeSeleccionesSection by remember { mutableStateOf(false) }
    var seleccionesSnapshotBounds by remember { mutableStateOf<Map<String, androidx.compose.ui.geometry.Rect>>(emptyMap()) }

    // Recently played from LibraryManager
    val recentlyPlayed by LibraryManager.recentlyPlayed.collectAsState()
    val stationPlayIndices = remember { mutableStateMapOf<String, Int>() }

    // Initial load — restore from cache first, then fetch youtube homepage in background
    LaunchedEffect(Unit) {
        if (!state.isLoaded) {
            withContext(Dispatchers.Default) {
                // Restore from cache immediately in background thread
                val cachedSuggestionsStr = LibraryManager.getString("cache_featured_suggestions")
                val cachedQuickPicksStr = LibraryManager.getString("cache_quick_picks")
                val cachedSeleccionesStr = LibraryManager.getString("cache_selecciones")
                val cachedPlaylistsStr = LibraryManager.getString("cache_playlists")
                val cachedSimilarStr = LibraryManager.getString("cache_similar_sections")
                val cachedTitle = LibraryManager.getString("cache_selecciones_title")

                if (!cachedSuggestionsStr.isNullOrBlank()) {
                    val sList = deserializeYTItemList(cachedSuggestionsStr)
                    val qList = deserializeYTItemList(cachedQuickPicksStr ?: "").filterIsInstance<SongItem>()
                    val pList = deserializeYTItemList(cachedSeleccionesStr ?: "").filterIsInstance<SongItem>()
                    val plList = deserializeYTItemList(cachedPlaylistsStr ?: "").filterIsInstance<PlaylistItem>()
                    val simList = deserializeSimilarSections(cachedSimilarStr ?: "")
                    
                    withContext(Dispatchers.Main) {
                        state.featuredSuggestions = sList
                        state.quickPickSongs = qList
                        state.seleccionesParaTi = pList
                        state.featuredPlaylists = plList
                        state.similarSections = simList
                        state.seleccionesTitle = cachedTitle
                        state.isLoaded = true
                    }
                }
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
                                val songs = section.items.filterIsInstance<SongItem>()
                                quickPicksTemp = songs.filterNot { it.isVideoSong }.ifEmpty { songs }
                            }
                            title.contains("similar a") || title.contains("similar to") -> {
                                val artistMatch = Regex("similar a (.*)", RegexOption.IGNORE_CASE).find(section.title)
                                    ?: Regex("similar to (.*)", RegexOption.IGNORE_CASE).find(section.title)
                                val artistName = artistMatch?.groupValues?.get(1)?.trim() ?: "Artistas similares"
                                similarList.add(SimilarSection(artistName, section.items))
                            }
                            title.contains("selecciones para ti") || title.contains("picks for you") || title.contains("mixes para ti") -> {
                                if (seleccionesListTemp.isEmpty()) {
                                    val songs = section.items.filterIsInstance<SongItem>()
                                    seleccionesListTemp = songs.filterNot { it.isVideoSong }.ifEmpty { songs }
                                    seleccionesTitleTemp = section.title
                                }
                            }
                            title.contains("recomendados") || title.contains("recommended") || title.contains("sugerencias destacadas") -> {
                                suggestionsList.addAll(section.items)
                            }
                        }
                    }

                    if (quickPicksTemp.isEmpty() && sects.isNotEmpty()) {
                        val songs = sects.flatMap { it.items.filterIsInstance<SongItem>() }
                        quickPicksTemp = (songs.filterNot { it.isVideoSong }.ifEmpty { songs }).take(12)
                    }
                    if (suggestionsList.isEmpty() && sects.isNotEmpty()) {
                        suggestionsList.addAll(sects.flatMap { it.items }.take(15))
                    }
                    if (seleccionesListTemp.isEmpty() && sects.isNotEmpty()) {
                        val songs = sects.flatMap { it.items.filterIsInstance<SongItem>() }
                        seleccionesListTemp = (songs.filterNot { it.isVideoSong }.ifEmpty { songs }).drop(12).take(15)
                    }

                    withContext(Dispatchers.Main) {
                        if (similarList.isNotEmpty()) state.similarSections = similarList.take(2)
                        if (seleccionesListTemp.isNotEmpty()) state.seleccionesParaTi = seleccionesListTemp
                        if (!seleccionesTitleTemp.isNullOrBlank()) state.seleccionesTitle = seleccionesTitleTemp
                        if (suggestionsList.isNotEmpty()) state.featuredSuggestions = suggestionsList
                        if (quickPicksTemp.isNotEmpty()) state.quickPickSongs = quickPicksTemp
                        if (playlistList.isNotEmpty()) state.featuredPlaylists = playlistList.distinctBy { it.id }
                        state.isLoaded = true

                        // Cache the loaded data
                        if (state.featuredSuggestions.isNotEmpty()) LibraryManager.saveString("cache_featured_suggestions", serializeYTItemList(state.featuredSuggestions))
                        if (state.quickPickSongs.isNotEmpty()) LibraryManager.saveString("cache_quick_picks", serializeYTItemList(state.quickPickSongs))
                        if (state.seleccionesParaTi.isNotEmpty()) LibraryManager.saveString("cache_selecciones", serializeYTItemList(state.seleccionesParaTi))
                        if (state.featuredPlaylists.isNotEmpty()) LibraryManager.saveString("cache_playlists", serializeYTItemList(state.featuredPlaylists))
                        if (state.similarSections.isNotEmpty()) LibraryManager.saveString("cache_similar_sections", serializeSimilarSections(state.similarSections))
                        if (!state.seleccionesTitle.isNullOrBlank()) LibraryManager.saveString("cache_selecciones_title", state.seleccionesTitle)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        state.isLoaded = true
                    }
                }
            }
        }
    }

    // Dynamic Quick Picks + Similar sections + "Porque escuchaste" mixed algorithm
    var algorithmSeeds by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }

    LaunchedEffect(playerState?.videoId, playerState?.title, playerState?.artist, recentlyPlayed) {
        val currentPlayingSeed = if (playerState != null && !playerState.title.isNullOrBlank()) {
            LibraryItem(
                id = playerState.videoId ?: playerState.title,
                title = playerState.title,
                subtitle = playerState.artist ?: "",
                thumbnail = playerState.artUrl?.toString(),
                type = ItemType.SONG,
                album = playerState.album
            )
        } else null

        val recentSongs = recentlyPlayed.filter { it.type == ItemType.SONG }
        val allSeeds = (listOfNotNull(currentPlayingSeed) + recentSongs)
            .distinctBy { (it.subtitle.ifBlank { it.title }).lowercase().trim() }

        if (allSeeds.isNotEmpty()) {
            val primary = allSeeds.first()
            val remaining = allSeeds.drop(1).shuffled().take(4)
            algorithmSeeds = listOf(primary) + remaining
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

            // Fetch related pages and next queues in parallel for guaranteed recommendations
            val deferreds = algorithmSeeds.map { song ->
                async {
                    var vid = song.id
                    val artist = song.subtitle.ifEmpty { song.title }
                    
                    if (vid.length != 11) {
                        val searchResult = YouTube.search(query = "${song.subtitle} ${song.title}", filter = com.echo.innertube.YouTube.SearchFilter.FILTER_SONG).getOrNull()
                        val songItems = searchResult?.items?.filterIsInstance<SongItem>().orEmpty()
                        val match = songItems.firstOrNull { !it.isVideoSong } ?: songItems.firstOrNull()
                        if (match != null) {
                            vid = match.id
                        }
                    }

                    var fetchedSongs = mutableListOf<SongItem>()
                    var fetchedArtists = mutableListOf<com.echo.innertube.models.ArtistItem>()
                    var fetchedPlaylists = mutableListOf<com.echo.innertube.models.PlaylistItem>()

                    if (vid.length == 11) {
                        val nextResult = YouTube.next(WatchEndpoint(videoId = vid)).getOrNull()
                        if (nextResult != null) {
                            val nonVideo = nextResult.items.filterNot { it.isVideoSong }
                            fetchedSongs.addAll(nonVideo.ifEmpty { nextResult.items })
                        }

                        val relatedEndpoint = nextResult?.relatedEndpoint
                        if (relatedEndpoint != null) {
                            val relatedPage = YouTube.related(relatedEndpoint).getOrNull()
                            if (relatedPage != null) {
                                val nonVideo = relatedPage.songs.filterNot { it.isVideoSong }
                                fetchedSongs.addAll(nonVideo.ifEmpty { relatedPage.songs })
                                fetchedArtists.addAll(relatedPage.artists)
                                fetchedPlaylists.addAll(relatedPage.playlists)
                            }
                        }
                    }

                    // Fallback to direct artist search if next didn't produce enough
                    if (fetchedSongs.size < 6 && artist.isNotBlank() && artist != "Artistas") {
                        val artistSongSearch = YouTube.search(query = artist, filter = com.echo.innertube.YouTube.SearchFilter.FILTER_SONG).getOrNull()
                        if (artistSongSearch != null) {
                            val songItems = artistSongSearch.items.filterIsInstance<SongItem>()
                            val nonVideo = songItems.filterNot { it.isVideoSong }
                            fetchedSongs.addAll(nonVideo.ifEmpty { songItems })
                        }
                    }

                    if (fetchedArtists.isEmpty() && artist.isNotBlank() && artist != "Artistas") {
                        val artistSearch = YouTube.search(query = artist, filter = com.echo.innertube.YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                        if (artistSearch != null) {
                            fetchedArtists.addAll(artistSearch.items.filterIsInstance<com.echo.innertube.models.ArtistItem>())
                        }
                    }

                    val distinctSongs = fetchedSongs.distinctBy { it.id }
                    val distinctArtists = fetchedArtists.distinctBy { it.id }

                    if (distinctSongs.isNotEmpty()) {
                        Triple(artist, distinctSongs, distinctArtists)
                    } else null
                }
            }

            val results = deferreds.awaitAll().filterNotNull().distinctBy { it.first.lowercase().trim() }

            for ((seedArtist, songs, artists) in results) {
                allQuickPicks.addAll(songs.take(8))
                allParaTi.addAll(songs.drop(8).take(10))

                val availableSongs = songs.drop(18)
                allSuggestions.addAll(availableSongs.take(8))

                // Primary similar section for this seed
                val primaryItems = mutableListOf<com.echo.innertube.models.YTItem>()
                primaryItems.addAll(artists.take(8))
                primaryItems.addAll(songs.drop(4).take(10))
                if (primaryItems.isNotEmpty() && seedArtist.isNotBlank() && seedArtist != "Artistas") {
                    sections.add(SimilarSection(
                        artistName = seedArtist,
                        items = primaryItems.shuffled()
                    ))
                }

                // Add a "Porque escuchaste a" section for this seed
                val seedSongs = songs.take(20)
                if (seedSongs.isNotEmpty() && seedArtist.isNotBlank() && seedArtist != "Artistas") {
                    porqueEscuchasteList.add(PorqueEscuchasteSection(
                        artistName = seedArtist,
                        songs = seedSongs
                    ))
                }
            }

            // 1. Fetch albums for "El mundo de [Artista]"
            val primaryArtist = results.firstOrNull()?.first?.takeIf { it.isNotBlank() && it != "Artistas" }
                ?: algorithmSeeds.firstOrNull()?.subtitle?.takeIf { it.isNotBlank() && it != "Artistas" }
                ?: algorithmSeeds.firstOrNull()?.title ?: ""

            var fetchedElMundoDe: ElMundoDeArtist? = null
            if (primaryArtist.isNotBlank() && primaryArtist != "Artistas") {
                val albumsResult = YouTube.search(primaryArtist, com.echo.innertube.YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
                val albums = albumsResult?.items?.filterIsInstance<com.echo.innertube.models.AlbumItem>()?.distinctBy { it.id }?.take(15) ?: emptyList()
                if (albums.isNotEmpty()) {
                    fetchedElMundoDe = ElMundoDeArtist(artistName = primaryArtist, albums = albums)
                }
            }

            // 2. Fetch results for "Para fiestas"
            var fetchedParaFiestas: List<com.echo.innertube.models.YTItem> = emptyList()
            val fiestaQuery = if (primaryArtist.isNotBlank() && primaryArtist != "Artistas") "Para la fiesta $primaryArtist" else "Fiesta"
            val fiestaRes = YouTube.search(fiestaQuery, com.echo.innertube.YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST).getOrNull()
            val fiestaPlaylists = fiestaRes?.items?.filterIsInstance<com.echo.innertube.models.PlaylistItem>() ?: emptyList()
            if (fiestaPlaylists.isNotEmpty()) {
                fetchedParaFiestas = fiestaPlaylists.take(15)
            } else {
                val fiestaSongsRes = YouTube.search(fiestaQuery, com.echo.innertube.YouTube.SearchFilter.FILTER_SONG).getOrNull()
                fetchedParaFiestas = fiestaSongsRes?.items?.filterIsInstance<SongItem>()?.take(15) ?: emptyList()
            }

            // 3. Generate "Playlists hechas para ti" (Image 1 style)
            val allArtists = results.map { it.first }.filter { it.isNotBlank() && it != "Artistas" }
            val relatedArtistNames = results.flatMap { it.third.map { a -> a.title } }.filter { it.isNotBlank() }
            val allCombinedArtists = (allArtists + relatedArtistNames).distinct()

            val topArtistsStr = allCombinedArtists.take(5).joinToString(", ") + if (allCombinedArtists.size > 5) " y más" else ""
            val chillArtistsStr = allCombinedArtists.reversed().take(5).joinToString(", ") + if (allCombinedArtists.size > 5) " y más" else ""
            val newMusicArtistsStr = allCombinedArtists.shuffled().take(5).joinToString(", ") + if (allCombinedArtists.size > 5) " y más" else ""
            val workoutArtistsStr = allCombinedArtists.drop(2).take(5).joinToString(", ") + if (allCombinedArtists.size > 7) " y más" else ""

            val allSongs = results.flatMap { it.second }

            val generatedPlaylists = mutableListOf<MadeForYouPlaylist>()
            if (allSongs.isNotEmpty()) {
                val animateTitle = context.getString(R.string.playlist_animate_title)
                val chillTitle = context.getString(R.string.playlist_chill_title)
                val nuevaMusicaTitle = context.getString(R.string.playlist_nueva_musica_title)
                val entrenarTitle = context.getString(R.string.playlist_entrenar_title)

                val animateSub = if (allCombinedArtists.isNotEmpty()) {
                    context.getString(R.string.and_more_artists, allCombinedArtists.take(5).joinToString(", "))
                } else {
                    context.getString(R.string.playlist_animate_fallback_artists)
                }

                val chillSub = if (allCombinedArtists.isNotEmpty()) {
                    context.getString(R.string.and_more_artists, allCombinedArtists.reversed().take(5).joinToString(", "))
                } else {
                    context.getString(R.string.playlist_chill_fallback_artists)
                }

                val nuevaMusicaSub = if (allCombinedArtists.isNotEmpty()) {
                    context.getString(R.string.and_more_artists, allCombinedArtists.shuffled().take(5).joinToString(", "))
                } else {
                    context.getString(R.string.playlist_nueva_musica_fallback_artists)
                }

                val entrenarSub = if (allCombinedArtists.isNotEmpty()) {
                    context.getString(R.string.and_more_artists, allCombinedArtists.drop(2).take(5).joinToString(", "))
                } else {
                    context.getString(R.string.playlist_entrenar_fallback_artists)
                }

                generatedPlaylists.add(
                    MadeForYouPlaylist(
                        id = "made_for_you_animate",
                        title = animateTitle,
                        artistsSubtitle = animateSub,
                        gradientColors = listOf(Color(0xFFE62B00), Color(0xFFFF4100), Color(0xFFC00000)),
                        seedSong = allSongs.firstOrNull(),
                        songs = allSongs.take(25)
                    )
                )
                generatedPlaylists.add(
                    MadeForYouPlaylist(
                        id = "made_for_you_chill",
                        title = chillTitle,
                        artistsSubtitle = chillSub,
                        gradientColors = listOf(Color(0xFF0F58A0), Color(0xFF1E88B5), Color(0xFF00897B)),
                        seedSong = allSongs.drop(4).firstOrNull() ?: allSongs.firstOrNull(),
                        songs = allSongs.drop(4).take(25)
                    )
                )
                generatedPlaylists.add(
                    MadeForYouPlaylist(
                        id = "made_for_you_nueva_musica",
                        title = nuevaMusicaTitle,
                        artistsSubtitle = nuevaMusicaSub,
                        gradientColors = listOf(Color(0xFFE24C78), Color(0xFFFF7597), Color(0xFFC2185B)),
                        seedSong = allSongs.drop(8).firstOrNull() ?: allSongs.firstOrNull(),
                        songs = allSongs.drop(8).take(25)
                    )
                )
                generatedPlaylists.add(
                    MadeForYouPlaylist(
                        id = "made_for_you_entrenar",
                        title = entrenarTitle,
                        artistsSubtitle = entrenarSub,
                        gradientColors = listOf(Color(0xFF5B2C8C), Color(0xFF8E24AA), Color(0xFFAB47BC)),
                        seedSong = allSongs.drop(12).firstOrNull() ?: allSongs.firstOrNull(),
                        songs = allSongs.drop(12).take(25)
                    )
                )
                MadeForYouRepository.registerAll(generatedPlaylists)
            }

            // 4. Generate "Estaciones para ti" (Image 2 style)
            val stationColors = listOf(
                Color(0xFF194A8D), // Blue
                Color(0xFFA31D24), // Crimson
                Color(0xFF4A1525), // Burgundy
                Color(0xFF36433E), // Slate olive
                Color(0xFF5A3825), // Brown/ochre
                Color(0xFF1B4D3E)  // Forest
            )

            val generatedStations = mutableListOf<ArtistStation>()
            if (allSongs.isNotEmpty()) {
                val username = LibraryManager.getString("spotify_user_name", "")?.ifBlank { "RayMusic" } ?: "RayMusic"
                generatedStations.add(
                    ArtistStation(
                        id = "station_personal",
                        title = context.getString(R.string.estacion_personal, username),
                        subtitle = "",
                        primaryThumb = null,
                        secondaryThumbs = emptyList(),
                        backgroundColor = Color(0xFFFF3B30),
                        isPersonal = true,
                        candidateSongs = allSongs.shuffled()
                    )
                )

                val discoveryPool = (results.flatMap { it.third.flatMap { a -> allSongs.filter { s -> s.artists.any { art -> art.name.contains(a.title, ignoreCase = true) } } } } + allSongs.shuffled()).distinctBy { it.id }.ifEmpty { allSongs.shuffled() }
                generatedStations.add(
                    ArtistStation(
                        id = "station_discovery",
                        title = context.getString(R.string.estacion_por_descubrir),
                        subtitle = "",
                        primaryThumb = null,
                        secondaryThumbs = emptyList(),
                        backgroundColor = Color(0xFF5856D6),
                        isDiscovery = true,
                        candidateSongs = discoveryPool
                    )
                )

                results.forEachIndexed { i, (seedArtist, songs, relatedArtists) ->
                    if (seedArtist.isNotBlank() && seedArtist != "Artistas") {
                        val mainThumb = relatedArtists.firstOrNull()?.thumbnail ?: songs.firstOrNull()?.thumbnail
                        val secThumbs = relatedArtists.drop(1).mapNotNull { it.thumbnail }.take(2).let { list ->
                            if (list.size < 2) {
                                (list + songs.drop(1).mapNotNull { it.thumbnail }).take(2)
                            } else list
                        }
                        val color = stationColors[i % stationColors.size]
                        val artistSongPool = songs.ifEmpty { allSongs.filter { s -> s.artists.any { it.name.contains(seedArtist, ignoreCase = true) } } }
                        generatedStations.add(
                            ArtistStation(
                                id = "station_${seedArtist.trim()}",
                                title = seedArtist,
                                subtitle = context.getString(R.string.y_artistas_similares, seedArtist),
                                primaryThumb = mainThumb,
                                secondaryThumbs = secThumbs,
                                backgroundColor = color,
                                isPersonal = false,
                                isDiscovery = false,
                                candidateSongs = artistSongPool
                            )
                        )
                    }
                }
            }

            val uniqueStations = generatedStations.distinctBy { it.id }

            withContext(Dispatchers.Main) {
                if (fetchedElMundoDe != null) {
                    state.elMundoDeArtist = fetchedElMundoDe
                }
                if (fetchedParaFiestas.isNotEmpty()) {
                    state.paraFiestasItems = fetchedParaFiestas
                }
                if (generatedPlaylists.isNotEmpty()) {
                    state.madeForYouPlaylists = generatedPlaylists
                }
                if (uniqueStations.isNotEmpty()) {
                    state.artistStations = uniqueStations
                }

                if (allQuickPicks.isNotEmpty()) {
                    state.quickPickSongs = allQuickPicks.distinctBy { it.id }.shuffled().take(12)
                }
                if (allParaTi.isNotEmpty()) {
                    state.seleccionesParaTi = allParaTi.distinctBy { it.id }.shuffled().take(20)
                }
                if (porqueEscuchasteList.isNotEmpty()) {
                    // Strictly limit to 2 sections
                    state.porqueEscuchasteSections = porqueEscuchasteList.distinctBy { it.artistName }.take(2)
                }

                if (results.isNotEmpty()) {
                    val primarySeedArtist = results.first().first
                    if (primarySeedArtist.isNotEmpty() && primarySeedArtist != "Artistas") {
                        state.seleccionesTitle = primarySeedArtist
                    }
                }

                if (allSuggestions.isNotEmpty()) {
                    state.featuredSuggestions = allSuggestions.distinctBy {
                        when (it) {
                            is SongItem -> it.id
                            is com.echo.innertube.models.AlbumItem -> it.id
                            is com.echo.innertube.models.ArtistItem -> it.id
                            else -> it.toString()
                        }
                    }.shuffled().take(15)
                }

                // Extract playlists from homePage
                val homePlaylists = state.homePage?.sections?.flatMap { it.items.filterIsInstance<com.echo.innertube.models.PlaylistItem>() } ?: emptyList()
                val combinedPlaylists = (allPlaylists + homePlaylists).distinctBy { it.id }
                if (combinedPlaylists.isNotEmpty()) {
                    state.featuredPlaylists = combinedPlaylists.shuffled().take(10)
                }

                // Merge similar sections from seeds and homePage — strictly limit to 2 sections
                if (sections.isNotEmpty()) {
                    val currentSimilar = state.similarSections
                    state.similarSections = (sections + currentSimilar).distinctBy { it.artistName }.take(2)
                }

                // Cache the updated recommendations
                if (state.featuredSuggestions.isNotEmpty()) LibraryManager.saveString("cache_featured_suggestions", serializeYTItemList(state.featuredSuggestions))
                if (state.quickPickSongs.isNotEmpty()) LibraryManager.saveString("cache_quick_picks", serializeYTItemList(state.quickPickSongs))
                if (state.seleccionesParaTi.isNotEmpty()) LibraryManager.saveString("cache_selecciones", serializeYTItemList(state.seleccionesParaTi))
                if (state.featuredPlaylists.isNotEmpty()) LibraryManager.saveString("cache_playlists", serializeYTItemList(state.featuredPlaylists))
                if (state.similarSections.isNotEmpty()) LibraryManager.saveString("cache_similar_sections", serializeSimilarSections(state.similarSections))
                if (!state.seleccionesTitle.isNullOrBlank()) LibraryManager.saveString("cache_selecciones_title", state.seleccionesTitle)
            }
        }
    }

    val listState = rememberLazyListState()
    var savedIndex by remember { mutableStateOf(-1) }
    var savedOffset by remember { mutableStateOf(0) }

    val outerOnAlbumSelected = onAlbumSelected
    val onAlbumSelected: (AlbumState) -> Unit = { album ->
        savedIndex = listState.firstVisibleItemIndex
        savedOffset = listState.firstVisibleItemScrollOffset
        outerOnAlbumSelected(album)
    }

    LaunchedEffect(SharedTransitionState.isDetailOpen) {
        if (!SharedTransitionState.isDetailOpen && savedIndex != -1) {
            listState.scrollToItem(savedIndex, savedOffset)
            savedIndex = -1
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 24.dp,
                bottom = innerPadding.calculateBottomPadding() + 180.dp
            )
        ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.nav_inicio),
                    color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onListenTogetherSelected,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Escuchar juntos",
                        tint = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
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
                    color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        count = displaySuggestions.take(15).size,
                        key = { index ->
                            val item = displaySuggestions.getOrNull(index)
                            val baseId = when (item) {
                                is com.echo.innertube.models.SongItem -> item.id
                                is com.echo.innertube.models.AlbumItem -> item.id
                                is com.echo.innertube.models.ArtistItem -> item.id
                                is com.echo.innertube.models.PlaylistItem -> item.id
                                else -> "$index"
                            }
                            "${baseId}_$index"
                        },
                        contentType = { "suggestion_card" }
                    ) { index ->
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
        // ESCUCHADO RECIENTEMENTE (Anteriormente "Sigue escuchando")
        // ═══════════════════════════════════════════════════════════
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            recentlyPlayedSnapshotBounds = SharedTransitionState.carouselItemBounds.toMap()
                            activeRecentlyPlayedSection = true
                        }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.escuchado_recientemente), color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = "More", tint = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val itemsToDisplay = recentlyPlayed.take(20)
                    items(
                        count = itemsToDisplay.size,
                        key = { index -> "${itemsToDisplay.getOrNull(index)?.id ?: index}_$index" },
                        contentType = { "recent_item" }
                    ) { index ->
                        val item = itemsToDisplay[index]
                        val hdThumb = upgradeThumb(item.thumbnail)
                        val isCircle = item.type == ItemType.ARTIST
                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .wiggleOnScroll(item.id, lazyListState = listState)
                                .clickable {
                                    SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
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
                                    .onGloballyPositioned { coords ->
                                        imageCoords = coords
                                        val bounds = coords.unclippedBoundsInRoot()
                                        if (bounds.width > 0f && bounds.height > 0f) {
                                            SharedTransitionState.carouselItemBounds[item.id] = bounds
                                        }
                                    }
                                    .clip(if (isCircle) CircleShape else RoundedCornerShape(12.dp))
                                    .graphicsLayer {
                                        alpha = if (SharedTransitionState.animatingItemIds.contains(item.id)) 0f else 1f
                                    }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(item.title, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.subtitle, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // EL MUNDO DE [ARTISTA] — Álbumes del artista que escuchó
        // ═══════════════════════════════════════════════════════════
        val elMundoDe = state.elMundoDeArtist
        if (elMundoDe != null && elMundoDe.albums.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.el_mundo_de, elMundoDe.artistName),
                        color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        count = elMundoDe.albums.size,
                        key = { index -> "${elMundoDe.albums.getOrNull(index)?.id ?: index}_$index" },
                        contentType = { "mundo_album" }
                    ) { index ->
                        val album = elMundoDe.albums[index]
                        val hdThumb = upgradeThumb(album.thumbnail)
                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .wiggleOnScroll(album.id, lazyListState = listState)
                                .clickable {
                                    SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
                                    SharedTransitionState.lastOpenedId = album.id
                                    onAlbumSelected(
                                        AlbumState(
                                            id = album.id,
                                            playlistId = album.playlistId ?: album.id,
                                            title = album.title,
                                            artist = elMundoDe.artistName,
                                            thumbnail = hdThumb,
                                            year = album.year as? Int ?: album.year?.toString()?.toIntOrNull()
                                        )
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(180.dp)
                                    .onGloballyPositioned { coords ->
                                        imageCoords = coords
                                        val bounds = coords.unclippedBoundsInRoot()
                                        if (bounds.width > 0f && bounds.height > 0f) {
                                            SharedTransitionState.carouselItemBounds[album.id] = bounds
                                        }
                                    }
                                    .graphicsLayer {
                                        alpha = if (SharedTransitionState.animatingItemIds.contains(album.id) || (SharedTransitionState.isDetailOpen && SharedTransitionState.lastOpenedId == album.id)) 0f else 1f
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = album.title,
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val yearStr = album.year?.toString()?.let { " • $it" } ?: ""
                            Text(
                                text = "Álbum$yearStr",
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // PARA FIESTAS — Resultados de YouTube Music según su estilo
        // ═══════════════════════════════════════════════════════════
        if (state.paraFiestasItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.para_fiestas),
                        color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val fiestaItems = state.paraFiestasItems
                    items(
                        count = fiestaItems.size,
                        key = { index ->
                            val itm = fiestaItems.getOrNull(index)
                            val baseId = when (itm) {
                                is PlaylistItem -> itm.id
                                is SongItem -> itm.id
                                is com.echo.innertube.models.AlbumItem -> itm.id
                                else -> "$index"
                            }
                            "${baseId}_$index"
                        },
                        contentType = { "fiesta_item" }
                    ) { index ->
                        val itm = fiestaItems[index]
                        val thumb = upgradeThumb(
                            when (itm) {
                                is PlaylistItem -> itm.thumbnail
                                is SongItem -> itm.thumbnail
                                is com.echo.innertube.models.AlbumItem -> itm.thumbnail
                                else -> null
                            }
                        )
                        val title = when (itm) {
                            is PlaylistItem -> itm.title
                            is SongItem -> itm.title
                            is com.echo.innertube.models.AlbumItem -> itm.title
                            else -> ""
                        }
                        val subtitle = when (itm) {
                            is PlaylistItem -> itm.author?.name ?: "Playlist"
                            is SongItem -> itm.artists.joinToString { it.name }
                            is com.echo.innertube.models.AlbumItem -> itm.artists?.joinToString { it.name } ?: "Álbum"
                            else -> ""
                        }
                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .wiggleOnScroll(
                                    when (itm) {
                                        is PlaylistItem -> itm.id
                                        is SongItem -> itm.id
                                        is com.echo.innertube.models.AlbumItem -> itm.id
                                        else -> "$index"
                                    },
                                    lazyListState = listState
                                )
                                .clickable {
                                    SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
                                    when (itm) {
                                        is PlaylistItem -> {
                                            SharedTransitionState.lastOpenedId = itm.id
                                            onAlbumSelected(
                                                AlbumState(
                                                    id = itm.id,
                                                    playlistId = itm.id,
                                                    title = itm.title,
                                                    artist = itm.author?.name ?: "Playlist",
                                                    thumbnail = thumb,
                                                    year = null
                                                )
                                            )
                                        }
                                        is SongItem -> {
                                            onSongSelected(
                                                PlayerState(
                                                    title = itm.title,
                                                    artist = itm.artists.joinToString { it.name },
                                                    artUrl = upgradeThumbHD(itm.thumbnail),
                                                    videoId = itm.id
                                                )
                                            )
                                        }
                                        is com.echo.innertube.models.AlbumItem -> {
                                            SharedTransitionState.lastOpenedId = itm.id
                                            onAlbumSelected(
                                                AlbumState(
                                                    id = itm.id,
                                                    playlistId = itm.playlistId ?: itm.id,
                                                    title = itm.title,
                                                    artist = itm.artists?.joinToString { it.name } ?: "",
                                                    thumbnail = thumb,
                                                    year = itm.year as? Int ?: itm.year?.toString()?.toIntOrNull()
                                                )
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(thumb).crossfade(true).build(),
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(180.dp)
                                    .onGloballyPositioned { imageCoords = it }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = title,
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // PLAYLISTS HECHAS PARA TI (Imagen 1 style — RayMusic)
        // ═══════════════════════════════════════════════════════════
        if (state.madeForYouPlaylists.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.playlists_hechas_para_ti),
                    color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val playlists = state.madeForYouPlaylists
                    items(
                        count = playlists.size,
                        key = { index -> "${playlists.getOrNull(index)?.id ?: index}_$index" },
                        contentType = { "made_for_you_playlist" }
                    ) { index ->
                        val pl = playlists[index]
                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        MadeForYouPlaylistCard(
                            playlist = pl,
                            modifier = Modifier
                                .wiggleOnScroll(pl.id, lazyListState = listState)
                                .onGloballyPositioned { coords ->
                                    imageCoords = coords
                                    val bounds = coords.unclippedBoundsInRoot()
                                    if (bounds.width > 0f && bounds.height > 0f) {
                                        SharedTransitionState.carouselItemBounds[pl.id] = bounds
                                    }
                                }
                                .graphicsLayer {
                                    alpha = if (SharedTransitionState.animatingItemIds.contains(pl.id) || (SharedTransitionState.isDetailOpen && SharedTransitionState.lastOpenedId == pl.id)) 0f else 1f
                                },
                            onSelected = {
                                SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
                                SharedTransitionState.lastOpenedId = pl.id
                                onAlbumSelected(
                                    AlbumState(
                                        id = pl.id,
                                        playlistId = pl.id,
                                        title = pl.title,
                                        artist = pl.artistsSubtitle,
                                        thumbnail = null,
                                        year = null
                                    )
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // ESTACIONES PARA TI (Imagen 2 style — RayMusic burbujas y radio)
        // ═══════════════════════════════════════════════════════════
        if (state.artistStations.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.estaciones_para_ti),
                    color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val stations = state.artistStations.distinctBy { it.id }
                    items(
                        count = stations.size,
                        key = { index -> "${stations.getOrNull(index)?.id ?: index}_$index" },
                        contentType = { "artist_station" }
                    ) { index ->
                        val station = stations[index]
                        ArtistStationCard(
                            context = context,
                            station = station,
                            onSelected = {
                                val songs = station.candidateSongs
                                if (songs.isNotEmpty()) {
                                    val currentIdx = stationPlayIndices[station.id] ?: -1
                                    val nextIdx = (currentIdx + 1) % songs.size
                                    stationPlayIndices[station.id] = nextIdx
                                    val song = songs[nextIdx]
                                    val stationQueue = (songs.drop(nextIdx + 1) + songs.take(nextIdx)).map { s ->
                                        QueueItem(
                                            title = s.title,
                                            artist = s.artists.joinToString { it.name },
                                            artUrl = upgradeThumbHD(s.thumbnail),
                                            videoId = s.id,
                                            album = s.album?.name
                                        )
                                    }
                                    onSongSelected(
                                        PlayerState(
                                            title = song.title,
                                            artist = song.artists.joinToString { it.name },
                                            artUrl = upgradeThumbHD(song.thumbnail),
                                            videoId = song.id,
                                            queue = stationQueue,
                                            isExclusiveQueue = true,
                                            album = song.album?.name
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // SIMILAR A [ARTISTA] — Estrictamente 2 secciones
        // ═══════════════════════════════════════════════════════════
        val similarSectionsToDisplay = state.similarSections.take(2)
        similarSectionsToDisplay.forEachIndexed { index, section ->
            item {
                Text(stringResource(R.string.similar_a), color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            similarSectionSnapshotBounds = SharedTransitionState.carouselItemBounds.toMap()
                            activeSimilarSection = section
                        }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(section.artistName, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = "More", tint = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor)
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
                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .wiggleOnScroll(item.id, lazyListState = listState)
                                .clickable {
                                    SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
                                    when (item) {
                                        is com.echo.innertube.models.ArtistItem -> {
                                            SharedTransitionState.lastOpenedId = item.id
                                            onArtistSelected(com.mrtdk.liquid_glass.ui.screens.ArtistState(item.id, item.title, item.thumbnail))
                                        }
                                        is com.echo.innertube.models.SongItem -> {
                                            onSongSelected(PlayerState(item.title, item.artists.joinToString { it.name }, item.thumbnail, item.id, album = item.album?.name, albumId = item.album?.id))
                                        }
                                        is com.echo.innertube.models.AlbumItem -> {
                                            SharedTransitionState.lastOpenedId = item.id
                                            onAlbumSelected(com.mrtdk.liquid_glass.ui.screens.AlbumState(item.id, item.playlistId ?: item.id, item.title, item.artists?.joinToString { it.name } ?: "Varios", item.thumbnail, item.year as? Int))
                                        }
                                        else -> {}
                                    }
                                }
                        ) {
                            if (isCircle) {
                                com.mrtdk.liquid_glass.spotify.SpotifyArtistAvatar(
                                    artistName = title,
                                    fallbackUrl = hdThumb,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(180.dp)
                                        .onGloballyPositioned { coords ->
                                            imageCoords = coords
                                            val bounds = coords.unclippedBoundsInRoot()
                                            if (bounds.width > 0f && bounds.height > 0f) {
                                                SharedTransitionState.carouselItemBounds[item.id] = bounds
                                            }
                                        }
                                        .clip(CircleShape)
                                        .graphicsLayer {
                                            alpha = if (SharedTransitionState.animatingItemIds.contains(item.id)) 0f else 1f
                                        }
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(180.dp)
                                        .onGloballyPositioned { coords ->
                                            imageCoords = coords
                                            val bounds = coords.unclippedBoundsInRoot()
                                            if (bounds.width > 0f && bounds.height > 0f) {
                                                SharedTransitionState.carouselItemBounds[item.id] = bounds
                                            }
                                        }
                                        .clip(RoundedCornerShape(12.dp))
                                        .graphicsLayer {
                                            alpha = if (SharedTransitionState.animatingItemIds.contains(item.id)) 0f else 1f
                                        }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(title, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(subtitle, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // PORQUE ESCUCHASTE A [ARTISTA] — Estrictamente 2 secciones
        // ═══════════════════════════════════════════════════════════
        val porqueEscuchasteToDisplay = state.porqueEscuchasteSections.take(2)
        if (porqueEscuchasteToDisplay.isNotEmpty()) {
            porqueEscuchasteToDisplay.forEach { section ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                porqueEscuchasteSnapshotBounds = SharedTransitionState.carouselItemBounds.toMap()
                                activePorqueEscuchasteSection = section
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.porque_escuchaste), color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor, fontSize = 13.sp)
                            Text(section.artistName, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "More", tint = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(section.songs.size) { index ->
                            val song = section.songs[index]
                            val hdThumb = upgradeThumb(song.thumbnail)
                            var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            Column(
                                modifier = Modifier.width(180.dp).clickable {
                                    SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
                                    onSongSelected(PlayerState(title = song.title, artist = song.artists.joinToString { it.name }, artUrl = upgradeThumbHD(song.thumbnail), videoId = song.id, album = song.album?.name, albumId = song.album?.id))
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .onGloballyPositioned { coords ->
                                            imageCoords = coords
                                            val bounds = coords.unclippedBoundsInRoot()
                                            if (bounds.width > 0f && bounds.height > 0f) {
                                                SharedTransitionState.carouselItemBounds[song.id] = bounds
                                            }
                                        }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1C1C1E))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                alpha = if (SharedTransitionState.animatingItemIds.contains(song.id)) 0f else 1f
                                            }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(song.title, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artists.joinToString { it.name }, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else if (state.seleccionesParaTi.isNotEmpty() && state.seleccionesTitle != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            seleccionesSnapshotBounds = SharedTransitionState.carouselItemBounds.toMap()
                            activeSeleccionesSection = true
                        }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.porque_escuchaste), color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor, fontSize = 13.sp)
                        Text(state.seleccionesTitle!!, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "More", tint = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.seleccionesParaTi.size) { index ->
                        val song = state.seleccionesParaTi[index]
                        val hdThumb = upgradeThumb(song.thumbnail)
                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Column(
                            modifier = Modifier.width(180.dp).clickable {
                                SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
                                onSongSelected(PlayerState(title = song.title, artist = song.artists.joinToString { it.name }, artUrl = upgradeThumbHD(song.thumbnail), videoId = song.id, album = song.album?.name, albumId = song.album?.id))
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .onGloballyPositioned { coords ->
                                        imageCoords = coords
                                        val bounds = coords.unclippedBoundsInRoot()
                                        if (bounds.width > 0f && bounds.height > 0f) {
                                            SharedTransitionState.carouselItemBounds[song.id] = bounds
                                        }
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            alpha = if (SharedTransitionState.animatingItemIds.contains(song.id)) 0f else 1f
                                        }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(song.title, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artists.joinToString { it.name }, color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
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
    val activeSection = activeSimilarSection
    CarouselToGridTransitionOverlay(
        visible = activeSection != null,
        title = activeSection?.artistName ?: "",
        items = activeSection?.items ?: emptyList(),
        isVideo = false,
        snapshotBounds = similarSectionSnapshotBounds,
        onClose = { activeSimilarSection = null }
    ) { dismiss ->
        val overlayItems = activeSection!!.items
        Box(modifier = Modifier.fillMaxSize().background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = { dismiss() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back_action),
                            tint = Color(0xFFFA243C)
                        )
                    }
                    Text(
                        text = activeSection.artistName,
                        color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
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
                    items(overlayItems.size) { index ->
                        val item = overlayItems[index]
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

                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wiggleOnScroll(itemId, similarGridState)
                                .clickable {
                                    SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
                                    when (item) {
                                        is com.echo.innertube.models.ArtistItem -> {
                                            SharedTransitionState.lastOpenedId = item.id
                                            onArtistSelected(com.mrtdk.liquid_glass.ui.screens.ArtistState(item.id, item.title, item.thumbnail))
                                        }
                                        is com.echo.innertube.models.SongItem -> {
                                            onSongSelected(PlayerState(item.title, item.artists.joinToString { it.name }, upgradeThumbHD(item.thumbnail), item.id, album = item.album?.name, albumId = item.album?.id))
                                        }
                                        is com.echo.innertube.models.AlbumItem -> {
                                            SharedTransitionState.lastOpenedId = item.id
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
                                    .onGloballyPositioned { imageCoords = it }
                                    .clip(if (isCircle) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                            ) {
                                if (isCircle) {
                                    com.mrtdk.liquid_glass.spotify.SpotifyArtistAvatar(
                                        artistName = title,
                                        fallbackUrl = hdThumb,
                                        contentDescription = title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                        contentDescription = title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = title,
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor,
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

    // Overlay recently played
    val mappedRecentlyPlayed = remember(recentlyPlayed) {
        recentlyPlayed.map { it.toYTItem() }
    }
    val recentlyPlayedGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    CarouselToGridTransitionOverlay(
        visible = activeRecentlyPlayedSection,
        title = stringResource(R.string.sigue_escuchando),
        items = mappedRecentlyPlayed,
        isVideo = false,
        snapshotBounds = recentlyPlayedSnapshotBounds,
        onClose = { activeRecentlyPlayedSection = false }
    ) { dismiss ->
        Box(modifier = Modifier.fillMaxSize().background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = { dismiss() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back_action),
                            tint = Color(0xFFFA243C)
                        )
                    }
                    Text(
                        text = stringResource(R.string.sigue_escuchando),
                        color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    state = recentlyPlayedGridState,
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
                    items(mappedRecentlyPlayed.size) { index ->
                        val item = mappedRecentlyPlayed[index]
                        val origItem = recentlyPlayed[index]
                        val hdThumb = upgradeThumb(item.thumbnail)
                        val isCircle = origItem.type == ItemType.ARTIST
                        
                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wiggleOnScroll(item.id, recentlyPlayedGridState)
                                .clickable {
                                    SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
                                    if (origItem.type == ItemType.SONG) {
                                        onSongSelected(PlayerState(
                                            title = origItem.title,
                                            artist = origItem.subtitle,
                                            artUrl = upgradeThumbHD(origItem.thumbnail),
                                            videoId = origItem.id
                                        ))
                                    } else {
                                        SharedTransitionState.lastOpenedId = origItem.id
                                        onAlbumSelected(AlbumState(
                                            id = origItem.id,
                                            playlistId = origItem.id,
                                            title = origItem.title,
                                            artist = origItem.subtitle,
                                            thumbnail = origItem.thumbnail,
                                            year = null
                                        ))
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .onGloballyPositioned { imageCoords = it }
                                    .clip(if (isCircle) CircleShape else RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isCircle) "Artista" else origItem.subtitle,
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor,
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

    // Overlay porque escuchaste
    val activePorqueEscuchaste = activePorqueEscuchasteSection
    val porqueEscuchasteGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    CarouselToGridTransitionOverlay(
        visible = activePorqueEscuchaste != null,
        title = activePorqueEscuchaste?.artistName ?: "",
        items = activePorqueEscuchaste?.songs ?: emptyList(),
        isVideo = false,
        snapshotBounds = porqueEscuchasteSnapshotBounds,
        onClose = { activePorqueEscuchasteSection = null }
    ) { dismiss ->
        val overlayItems = activePorqueEscuchaste!!.songs
        Box(modifier = Modifier.fillMaxSize().background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = { dismiss() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back_action),
                            tint = Color(0xFFFA243C)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = stringResource(R.string.porque_escuchaste),
                            color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor,
                            fontSize = 11.sp
                        )
                        Text(
                            text = activePorqueEscuchaste.artistName,
                            color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    state = porqueEscuchasteGridState,
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
                    items(overlayItems.size) { index ->
                        val item = overlayItems[index]
                        val hdThumb = upgradeThumb(item.thumbnail)
                        
                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wiggleOnScroll(item.id, porqueEscuchasteGridState)
                                .clickable {
                                    SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
                                    onSongSelected(PlayerState(item.title, item.artists.joinToString { it.name }, upgradeThumbHD(item.thumbnail), item.id, album = item.album?.name, albumId = item.album?.id))
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .onGloballyPositioned { imageCoords = it }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Canción • ${item.artists.joinToString { it.name }}",
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor,
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

    // Overlay throwback jams
    val seleccionesGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    CarouselToGridTransitionOverlay(
        visible = activeSeleccionesSection,
        title = state.seleccionesTitle ?: "",
        items = state.seleccionesParaTi,
        isVideo = false,
        snapshotBounds = seleccionesSnapshotBounds,
        onClose = { activeSeleccionesSection = false }
    ) { dismiss ->
        val overlayItems = state.seleccionesParaTi
        Box(modifier = Modifier.fillMaxSize().background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = { dismiss() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back_action),
                            tint = Color(0xFFFA243C)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = stringResource(R.string.porque_escuchaste),
                            color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor,
                            fontSize = 11.sp
                        )
                        Text(
                            text = state.seleccionesTitle ?: "",
                            color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    state = seleccionesGridState,
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
                    items(overlayItems.size) { index ->
                        val item = overlayItems[index]
                        val hdThumb = upgradeThumb(item.thumbnail)
                        
                        var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wiggleOnScroll(item.id, seleccionesGridState)
                                .clickable {
                                    SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
                                    onSongSelected(PlayerState(item.title, item.artists.joinToString { it.name }, upgradeThumbHD(item.thumbnail), item.id, album = item.album?.name, albumId = item.album?.id))
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .onGloballyPositioned { imageCoords = it }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Canción • ${item.artists.joinToString { it.name }}",
                                color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor,
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

private fun LibraryItem.toYTItem(): com.echo.innertube.models.YTItem {
    return when (type) {
        ItemType.ARTIST -> com.echo.innertube.models.ArtistItem(
            id = id,
            title = title,
            thumbnail = thumbnail ?: "",
            shuffleEndpoint = null,
            radioEndpoint = null
        )
        ItemType.SONG -> com.echo.innertube.models.SongItem(
            id = id,
            title = title,
            artists = listOf(com.echo.innertube.models.Artist(subtitle ?: "", null)),
            thumbnail = thumbnail ?: "",
            explicit = false
        )
        ItemType.ALBUM -> com.echo.innertube.models.AlbumItem(
            browseId = id,
            playlistId = id,
            title = title,
            artists = listOf(com.echo.innertube.models.Artist(subtitle ?: "", null)),
            year = null,
            thumbnail = thumbnail ?: "",
            explicit = false
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// Playlists Hechas Para Ti Card (Image 1 style)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun MadeForYouPlaylistCard(
    playlist: MadeForYouPlaylist,
    modifier: Modifier = Modifier,
    onSelected: () -> Unit
) {
    Box(
        modifier = modifier
            .width(195.dp)
            .height(265.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelected)
    ) {
        MadeForYouCardContent(
            title = playlist.title,
            artistsSubtitle = playlist.artistsSubtitle,
            gradientColors = playlist.gradientColors,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// Estaciones Para Ti Card (Image 2 style)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun ArtistStationCard(
    context: android.content.Context,
    station: ArtistStation,
    onSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(175.dp)
            .clickable { onSelected() }
    ) {
        Box(
            modifier = Modifier
                .size(175.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(station.backgroundColor)
        ) {
            if (station.isPersonal) {
                // Geometric Chevrons (Warm Orange / Coral / Pink)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val brush1 = Brush.linearGradient(listOf(Color(0xFFFF9500), Color(0xFFFF2D55)))
                    val brush2 = Brush.linearGradient(listOf(Color(0xFFFF3B30), Color(0xFFFF2D55), Color(0xFFD6002A)))
                    val brush3 = Brush.linearGradient(listOf(Color(0xFFFF5E3A), Color(0xFFFF2A68)))

                    val p1 = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w * 0.55f, h * 0.5f)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(p1, brush = brush1)

                    val p2 = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.18f, h * 0.12f)
                        lineTo(w * 0.72f, h * 0.5f)
                        lineTo(w * 0.18f, h * 0.88f)
                        close()
                    }
                    drawPath(p2, brush = brush2)

                    val p3 = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.36f, h * 0.24f)
                        lineTo(w * 0.90f, h * 0.5f)
                        lineTo(w * 0.36f, h * 0.76f)
                        close()
                    }
                    drawPath(p3, brush = brush3)
                }
            } else if (station.isDiscovery) {
                // Geometric Radial Rays (Deep Purple / Indigo / Blue / Cyan)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val originX = w * 0.05f
                    val originY = h * 0.5f
                    val colors = listOf(
                        Color(0xFF5856D6), Color(0xFF3634A3), Color(0xFF1B237C),
                        Color(0xFF007AFF), Color(0xFF00C7BE), Color(0xFF30D158),
                        Color(0xFF5856D6), Color(0xFF3634A3)
                    )
                    val rayCount = 12
                    for (i in 0 until rayCount) {
                        val angle1 = (i.toFloat() / rayCount) * (Math.PI.toFloat()) - (Math.PI.toFloat() / 2f)
                        val angle2 = ((i + 1).toFloat() / rayCount) * (Math.PI.toFloat()) - (Math.PI.toFloat() / 2f)
                        val r = w * 1.5f
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(originX, originY)
                            lineTo(originX + r * kotlin.math.cos(angle1), originY + r * kotlin.math.sin(angle1))
                            lineTo(originX + r * kotlin.math.cos(angle2), originY + r * kotlin.math.sin(angle2))
                            close()
                        }
                        drawPath(path, color = colors[i % colors.size])
                    }
                }
            } else {
                // Three circular avatars collage (Main artist + 2 similar artists)
                val mainThumb = upgradeThumb(station.primaryThumb)
                val sec1 = upgradeThumb(station.secondaryThumbs.getOrNull(0))
                val sec2 = upgradeThumb(station.secondaryThumbs.getOrNull(1) ?: station.secondaryThumbs.getOrNull(0))

                Box(modifier = Modifier.fillMaxSize()) {
                    // Big circle (Main artist)
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(mainThumb).crossfade(true).build(),
                        contentDescription = station.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .offset(x = 8.dp, y = 38.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    )

                    // Secondary circle 1 (Top right)
                    if (sec1 != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(sec1).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(62.dp)
                                .offset(x = 98.dp, y = 52.dp)
                                .clip(CircleShape)
                                .border(2.dp, station.backgroundColor, CircleShape)
                        )
                    }

                    // Secondary circle 2 (Bottom right)
                    if (sec2 != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(sec2).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .offset(x = 86.dp, y = 110.dp)
                                .clip(CircleShape)
                                .border(2.dp, station.backgroundColor, CircleShape)
                        )
                    }
                }
            }

            // Top right: "RayMusic"
            Text(
                text = "RayMusic",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (station.isPersonal || station.isDiscovery) station.title else station.subtitle,
            color = com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private object SuggestionCardCache {
    val dominantColorMap = java.util.concurrent.ConcurrentHashMap<String, Color>()
    val coverBitmapMap = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()
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
            clickAction = { onSongSelected(PlayerState(title = titleStr, artist = subtitleStr, artUrl = upgradeThumbHD(thumbUrl), videoId = item.id, album = (item as? com.echo.innertube.models.SongItem)?.album?.name, albumId = (item as? com.echo.innertube.models.SongItem)?.album?.id)) }
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
    val cachedColor = hdThumb?.let { SuggestionCardCache.dominantColorMap[it] } ?: Color(0xFF1C1C1E)
    val cachedBitmap = hdThumb?.let { SuggestionCardCache.coverBitmapMap[it] }
    var dominantColor by remember(hdThumb) { mutableStateOf(cachedColor) }
    var coverBitmap by remember(hdThumb) { mutableStateOf(cachedBitmap) }

    LaunchedEffect(hdThumb) {
        if (hdThumb != null && (coverBitmap == null || dominantColor == Color(0xFF1C1C1E))) {
            withContext(Dispatchers.Default) {
                val request = ImageRequest.Builder(context)
                    .data(hdThumb)
                    .allowHardware(false)
                    .size(100)
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
                    
                    val asComposeBmp = bitmap.asImageBitmap()
                    try {
                        var r = 0L; var g = 0L; var b = 0L
                        val y = bitmap.height - 1
                        val w = bitmap.width
                        val step = maxOf(1, w / 16)
                        var count = 0
                        for (x in 0 until w step step) {
                            val pixel = bitmap.getPixel(x, y)
                            r += android.graphics.Color.red(pixel)
                            g += android.graphics.Color.green(pixel)
                            b += android.graphics.Color.blue(pixel)
                            count++
                        }
                        val sampledColor = Color((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
                        SuggestionCardCache.dominantColorMap[hdThumb] = sampledColor
                        SuggestionCardCache.coverBitmapMap[hdThumb] = asComposeBmp
                        withContext(Dispatchers.Main) {
                            coverBitmap = asComposeBmp
                            dominantColor = sampledColor
                        }
                    } catch (e: Exception) {
                        SuggestionCardCache.coverBitmapMap[hdThumb] = asComposeBmp
                        withContext(Dispatchers.Main) {
                            coverBitmap = asComposeBmp
                        }
                    }
                }
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
                SharedTransitionState.lastClickBounds = imageCoords?.unclippedBoundsInRoot()
                clickAction()
            }
    ) {
        // Capa 1: Reflejo Líquido Estirado 1D
        val currentCoverBitmap = coverBitmap
        if (currentCoverBitmap != null) {
            val overlapDp = 20.dp
            Box(
                modifier = Modifier
                    .offset(y = 270.dp - overlapDp) // Empieza a los 250.dp
                    .size(width = 280.dp, height = 110.dp + overlapDp) // Altura es 130.dp
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .blur(15.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
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
                    val containerW = 280.dp.toPx()
                    val containerH = 270.dp.toPx()
                    val bitmapW = currentCoverBitmap.width.toFloat()
                    val bitmapH = currentCoverBitmap.height.toFloat()

                    val containerRatio = containerW / containerH
                    val bitmapRatio = bitmapW / bitmapH

                    val srcX: Float
                    val srcWidth: Float
                    val srcY: Float

                    val sampleHeight = 5
                    val sampleH = sampleHeight.coerceAtMost(currentCoverBitmap.height).coerceAtLeast(1)

                    if (containerRatio < bitmapRatio) {
                        // El contenedor es proporcionalmente más alto (se recorta izq/der)
                        val scale = containerH / bitmapH
                        val visibleWidth = containerW / scale
                        srcX = (bitmapW - visibleWidth) / 2f
                        srcWidth = visibleWidth
                        srcY = bitmapH - sampleH
                    } else {
                        // El contenedor es proporcionalmente más ancho (se recorta arriba/abajo)
                        val scale = containerW / bitmapW
                        val visibleHeight = containerW / scale
                        srcX = 0f
                        srcWidth = bitmapW
                        val cropY = (bitmapH - visibleHeight) / 2f
                        val visibleBottomY = cropY + visibleHeight
                        srcY = (visibleBottomY - sampleH).coerceIn(0f, bitmapH - sampleH)
                    }

                    val srcXInt = srcX.toInt().coerceIn(0, currentCoverBitmap.width - 1)
                    val srcWInt = srcWidth.toInt().coerceIn(1, currentCoverBitmap.width - srcXInt)
                    val srcYInt = srcY.toInt().coerceIn(0, currentCoverBitmap.height - sampleH)

                    drawImage(
                        image = currentCoverBitmap,
                        srcOffset = IntOffset(srcXInt, srcYInt),
                        srcSize = IntSize(srcWInt, sampleH),
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
                                0.95f to Color.Black,
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
        color = if (small) com.mrtdk.liquid_glass.ui.theme.ThemeManager.subtextColor else com.mrtdk.liquid_glass.ui.theme.ThemeManager.textColor,
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