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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovedadesScreen(
    innerPadding: PaddingValues, 
    onSongSelected: (PlayerState) -> Unit = {},
    onAlbumSelected: (AlbumState) -> Unit = {}
) {
    val context = LocalContext.current
    var trendingSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var newReleaseAlbums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var featuredAlbums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Fetch charts for trending/top songs
            YouTube.getChartsPage().onSuccess { chartsPage ->
                val songs = chartsPage.sections
                    .flatMap { it.items }
                    .filterIsInstance<SongItem>()
                    .take(20)
                if (songs.isNotEmpty()) {
                    trendingSongs = songs
                }
            }

            // Fetch explore page for new release albums
            YouTube.explore().onSuccess { page ->
                val albums = page.newReleaseAlbums
                newReleaseAlbums = albums
                featuredAlbums = albums.take(6)
            }

            // Fallback: if charts didn't return songs, search for trending
            if (trendingSongs.isEmpty()) {
                YouTube.search("trending music 2026", YouTube.SearchFilter.FILTER_SONG).onSuccess { result ->
                    trendingSongs = result.items.filterIsInstance<SongItem>().take(20)
                }
            }
        }
    }

    // Auto-scrolling pager
    val pagerState = rememberPagerState(pageCount = { featuredAlbums.size.coerceAtLeast(1) })
    LaunchedEffect(pagerState, featuredAlbums.size) {
        if (featuredAlbums.size <= 1) return@LaunchedEffect
        while (true) { delay(4500); pagerState.animateScrollToPage((pagerState.currentPage + 1) % featuredAlbums.size) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 16.dp, bottom = innerPadding.calculateBottomPadding() + 180.dp)
    ) {
        // ── FEATURED ALBUMS CAROUSEL (big cards with text overlay at top) ──
        item {
            Text("Novedades", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))

            if (featuredAlbums.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                    Text("Cargando novedades...", color = Color.Gray)
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    val album = featuredAlbums[page]
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
                    repeat(featuredAlbums.size) { i ->
                        Box(modifier = Modifier.padding(horizontal = 3.dp).size(if (pagerState.currentPage == i) 8.dp else 6.dp).clip(CircleShape)
                            .background(if (pagerState.currentPage == i) Color.White else Color.White.copy(alpha = 0.3f)))
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // ── LATEST SONGS (Trending/Charts — real popular songs) ──────────
        if (trendingSongs.isNotEmpty()) {
            item {
                Text("Latest Songs", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Spacer(modifier = Modifier.height(4.dp))
            }

            val chunkedSongs = trendingSongs.chunked(2)
            items(chunkedSongs.size) { idx ->
                val rowSongs = chunkedSongs[idx]
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    rowSongs.forEachIndexed { songIdx, s ->
                        val songThumb = s.thumbnail?.replace("=w226-h226", "=w200-h200")?.replace("=w120-h120", "=w200-h200")
                        val durationText = s.duration?.let {
                            val totalSec = it / 1000
                            val min = totalSec / 60
                            val sec = totalSec % 60
                            "%d:%02d".format(min, sec)
                        } ?: ""

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSongSelected(PlayerState(title = s.title, artist = s.artists.joinToString { it.name }, artUrl = songThumb, videoId = s.id))
                                }
                                .padding(vertical = 8.dp)
                                .padding(end = if (songIdx == 0) 8.dp else 0.dp, start = if (songIdx == 1) 8.dp else 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1C1C1E))) {
                                AsyncImage(model = ImageRequest.Builder(context).data(songThumb).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(s.artists.joinToString { it.name }, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (durationText.isNotEmpty()) {
                                Text(durationText, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                    if (rowSongs.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                // Subtle divider below the row, but not after the last one
                if (idx < chunkedSongs.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(Color.White.copy(alpha = 0.06f)))
                }
            }
        }

        // ── NEW RELEASES (Album carousel) ──────────────────
        if (newReleaseAlbums.size > 6) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                Text("New Releases", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val remaining = newReleaseAlbums.drop(6)
                    items(remaining.size) { idx ->
                        val album = remaining[idx]
                        val hdThumb = album.thumbnail?.replace("=w226-h226", "=w540-h540")?.replace("=w120-h120", "=w540-h540")
                        Column(modifier = Modifier.width(160.dp).clickable {
                            onAlbumSelected(AlbumState(id = album.id, playlistId = album.playlistId ?: album.id, title = album.title, artist = album.artists?.joinToString { it.name } ?: "", thumbnail = hdThumb, year = album.year as? Int ?: album.year?.toString()?.toIntOrNull()))
                        }) {
                            Box(modifier = Modifier.size(160.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
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
