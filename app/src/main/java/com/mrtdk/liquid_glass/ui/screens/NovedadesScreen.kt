package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.echo.innertube.YouTube
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.AlbumItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ChevronRight

class NovedadesState {
    var isLoaded by mutableStateOf(false)
    var trendingSongs by mutableStateOf<List<SongItem>>(emptyList())
    var newReleaseAlbums by mutableStateOf<List<AlbumItem>>(emptyList())
    var featuredAlbums by mutableStateOf<List<AlbumItem>>(emptyList())
    var featuredNewSongs by mutableStateOf<List<SongItem>>(emptyList())
    var everyoneListening by mutableStateOf<List<SongItem>>(emptyList())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovedadesScreen(
    innerPadding: PaddingValues, 
    state: NovedadesState = remember { NovedadesState() },
    onSongSelected: (PlayerState) -> Unit = {},
    onAlbumSelected: (AlbumState) -> Unit = {},
    onVideoSelected: (String) -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!state.isLoaded) {
            withContext(Dispatchers.IO) {
                // Helper: filter out mixes, compilations, and songs without known artist links
                fun filterRealArtistSongs(songs: List<SongItem>): List<SongItem> = songs.filter { s ->
                    val titleLower = s.title.lowercase()
                    val artistText = s.artists.joinToString { it.name }.lowercase()
                    // Exclude mixes, compilations, and generic entries
                    val isMix = titleLower.contains("mix") || titleLower.contains("compilation") ||
                        titleLower.contains("playlist") || titleLower.contains("mashup") ||
                        titleLower.contains("medley") || titleLower.contains("recopilación") ||
                        titleLower.contains("album") || titleLower.contains("álbum") ||
                        titleLower.contains("completo")
                    val isGenericArtist = artistText.contains("various") || artistText.contains("varios") ||
                        artistText.contains("topic") || s.artists.isEmpty() || artistText.contains("mix")
                    // Must have at least one artist with a browseId (real YTM artist page)
                    val hasRealArtist = s.artists.any { it.id != null }
                    val duration = s.duration
                    val isNormalDuration = duration == null || duration < 600 // max 10 mins
                    !isMix && !isGenericArtist && hasRealArtist && isNormalDuration
                }

                // Fetch charts for trending/top songs from known artists
                YouTube.getChartsPage().onSuccess { chartsPage ->
                    val allSongs = filterRealArtistSongs(
                        chartsPage.sections
                            .flatMap { it.items }
                            .filterIsInstance<SongItem>()
                    )

                    state.trendingSongs = allSongs.take(20)
                    // Use a different slice for "everyone listening"
                    state.everyoneListening = allSongs.drop(8).take(15)
                }

                // Fetch explore page for new release albums
                YouTube.explore().onSuccess { page ->
                    val albums = page.newReleaseAlbums
                    state.newReleaseAlbums = albums
                    state.featuredAlbums = albums.take(6)
                }

                // Featured new songs: search for recent hit songs from real artists
                YouTube.search("top hits songs 2025 2026", YouTube.SearchFilter.FILTER_SONG).onSuccess { result ->
                    state.featuredNewSongs = filterRealArtistSongs(
                        result.items.filterIsInstance<SongItem>()
                    ).take(16)
                }

                // Fallback: if charts didn't return songs, use curated search
                if (state.trendingSongs.isEmpty()) {
                    YouTube.search("top songs chart", YouTube.SearchFilter.FILTER_SONG).onSuccess { result ->
                        state.trendingSongs = filterRealArtistSongs(
                            result.items.filterIsInstance<SongItem>()
                        ).take(20)
                    }
                }

                // Fallback for everyone listening
                if (state.everyoneListening.isEmpty()) {
                    YouTube.search("most listened songs today", YouTube.SearchFilter.FILTER_SONG).onSuccess { result ->
                        state.everyoneListening = filterRealArtistSongs(
                            result.items.filterIsInstance<SongItem>()
                        ).take(15)
                    }
                }
            }
            state.isLoaded = true
        }
    }

    // Auto-scrolling pager
    val pagerState = rememberPagerState(pageCount = { state.featuredAlbums.size.coerceAtLeast(1) })
    LaunchedEffect(pagerState, state.featuredAlbums.size) {
        if (state.featuredAlbums.size <= 1) return@LaunchedEffect
        while (true) { delay(4500); pagerState.animateScrollToPage((pagerState.currentPage + 1) % state.featuredAlbums.size) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 16.dp, bottom = innerPadding.calculateBottomPadding() + 180.dp)
    ) {
        // ── FEATURED ALBUMS CAROUSEL (big cards with text overlay at top) ──
        item {
            Text("Novedades", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))

            if (state.featuredAlbums.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    val album = state.featuredAlbums[page]
                    val hdThumb = album.thumbnail?.replace("=w226-h226", "=w720-h720")?.replace("=w120-h120", "=w720-h720")
                    val artistText = album.artists?.joinToString { it.name } ?: "Varios"

                    Box(
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Color.DarkGray)
                            .clickable { onAlbumSelected(AlbumState(id = album.id, playlistId = album.playlistId ?: album.id, title = album.title, artist = artistText, thumbnail = hdThumb, year = album.year as? Int ?: album.year?.toString()?.toIntOrNull())) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                            contentDescription = album.title, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Top gradient for label
                        Box(modifier = Modifier.fillMaxWidth().height(90.dp).align(Alignment.TopCenter)
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))))
                        // Bottom gradient for title
                        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.45f).align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
                        // Label at top
                        Text(
                            "NEW ALBUM", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                        )
                        // Title + artist at bottom
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Text(album.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(artistText, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, maxLines = 1)
                        }
                    }
                }
                // Dot indicators
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(state.featuredAlbums.size) { i ->
                        Box(modifier = Modifier.padding(horizontal = 3.dp).size(if (pagerState.currentPage == i) 8.dp else 6.dp).clip(CircleShape)
                            .background(if (pagerState.currentPage == i) Color.White else Color.White.copy(alpha = 0.3f)))
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // ── CANCIONES NUEVAS DESTACADAS ──────────────────
        if (state.featuredNewSongs.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Canciones nuevas destacadas", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp).padding(start = 4.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val columns = state.featuredNewSongs.chunked(4)
                    items(columns.size) { colIndex ->
                        Column(modifier = Modifier.width(320.dp)) {
                            columns[colIndex].forEachIndexed { index, s ->
                                val songThumb = s.thumbnail?.replace("=w226-h226", "=w200-h200")?.replace("=w120-h120", "=w200-h200")
                                SongRow(context, s, songThumb, onSongSelected)
                                if (index < columns[colIndex].lastIndex) {
                                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.15f)))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── NEW RELEASES (Album carousel — 2 rows) ──────────────────
        if (state.newReleaseAlbums.size > 6) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                Text("New Releases", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val remaining = state.newReleaseAlbums.drop(6)
                    // Chunk into pairs for 2-row layout
                    val pairedColumns = remaining.chunked(2)
                    items(pairedColumns.size) { colIdx ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.width(160.dp)
                        ) {
                            pairedColumns[colIdx].forEach { album ->
                                val hdThumb = album.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
                                Column(modifier = Modifier.fillMaxWidth().clickable {
                                    onAlbumSelected(AlbumState(id = album.id, playlistId = album.playlistId ?: album.id, title = album.title, artist = album.artists?.joinToString { it.name } ?: "", thumbnail = hdThumb, year = album.year as? Int ?: album.year?.toString()?.toIntOrNull()))
                                }) {
                                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
                                        AsyncImage(model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(), contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(album.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(album.artists?.joinToString { it.name } ?: "", color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── CANCIONES DEL MOMENTO (Trending/Charts) ──────────
        if (state.trendingSongs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Canciones del momento", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp).padding(start = 4.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val columns = state.trendingSongs.chunked(4)
                    items(columns.size) { colIndex ->
                        Column(modifier = Modifier.width(320.dp)) {
                            columns[colIndex].forEachIndexed { index, s ->
                                val songThumb = s.thumbnail?.replace("=w226-h226", "=w200-h200")?.replace("=w120-h120", "=w200-h200")
                                SongRow(context, s, songThumb, onSongSelected)
                                if (index < columns[colIndex].lastIndex) {
                                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.15f)))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── TODA LA GENTE ESTÁ ESCUCHANDO... ──────────────
        if (state.everyoneListening.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Toda la gente está escuchando...", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.everyoneListening.size) { idx ->
                        val s = state.everyoneListening[idx]
                        val songThumb = s.thumbnail?.let {
                            when {
                                it.contains("=w") -> it.substringBefore("=w") + "=w540-h540-l90-rj"
                                it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault")
                                else -> it
                            }
                        } ?: s.thumbnail

                        Column(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable {
                                    onSongSelected(PlayerState(
                                        title = s.title,
                                        artist = s.artists.joinToString { it.name },
                                        artUrl = songThumb,
                                        videoId = s.id
                                    ))
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(songThumb).crossfade(true).build(),
                                    contentDescription = s.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                s.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                s.artists.joinToString { it.name },
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

/** Reusable song row used in "Canciones del momento" and "Canciones nuevas destacadas" */
@Composable
private fun SongRow(
    context: android.content.Context,
    s: SongItem,
    songThumb: String?,
    onSongSelected: (PlayerState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSongSelected(PlayerState(title = s.title, artist = s.artists.joinToString { it.name }, artUrl = songThumb, videoId = s.id))
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1C1C1E))) {
            AsyncImage(model = ImageRequest.Builder(context).data(songThumb).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (s.explicit) {
                    Box(modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha=0.3f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text("E", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(s.artists.joinToString { it.name }, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray, modifier = Modifier.padding(start = 8.dp).size(20.dp))
    }
}
