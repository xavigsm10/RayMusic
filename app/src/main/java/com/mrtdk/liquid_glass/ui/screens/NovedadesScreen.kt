package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.mrtdk.liquid_glass.data.LocalMediaScanner
import com.mrtdk.liquid_glass.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovedadesScreen(innerPadding: PaddingValues, onSongSelected: (PlayerState) -> Unit = {}) {
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var onlineSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val scanner = LocalMediaScanner(context)
        songs = scanner.getLocalSongs()
        withContext(Dispatchers.IO) {
            YouTube.search("new music 2024", YouTube.SearchFilter.FILTER_SONG).onSuccess { result ->
                onlineSongs = result.items.filterIsInstance<SongItem>().take(30)
            }
        }
    }

    // Carousel of featured songs (first 6)
    val featuredSongs = onlineSongs.take(6)
    val pagerState = rememberPagerState(pageCount = { featuredSongs.size.coerceAtLeast(1) })

    // Auto-advance the carousel every 4 seconds
    LaunchedEffect(pagerState, featuredSongs.size) {
        if (featuredSongs.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % featuredSongs.size
            pagerState.animateScrollToPage(next)
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
        // Header
        item {
            Text(
                text = "Novedades",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Featured album banner — CAROUSEL
        item {
            if (featuredSongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cargando novedades...", color = Color.Gray)
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "ÁLBUM IMPRESCINDIBLE",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // The horizontal pager carousel
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    ) { page ->
                        val song = featuredSongs[page]
                        val hdThumb = song.thumbnail
                            ?.replace("=w226-h226", "=w720-h720")
                            ?.replace("=w120-h120", "=w720-h720")

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.DarkGray)
                                .clickable {
                                    onSongSelected(
                                        PlayerState(
                                            title = song.title,
                                            artist = song.artists.joinToString { it.name },
                                            artUrl = hdThumb,
                                            videoId = song.id
                                        )
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(hdThumb).crossfade(true).build(),
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                            startY = 80f
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artists.joinToString { it.name },
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dot indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(featuredSongs.size) { i ->
                            val isSelected = pagerState.currentPage == i
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color.White
                                        else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // "Canciones nuevas" section header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Canciones nuevas", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(">", color = Color.Gray, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Vertical list of new songs
        val newSongs = onlineSongs.drop(6)
        items(newSongs.size) { idx ->
            val s = newSongs[idx]
            val hdThumb = s.thumbnail
                ?.replace("=w226-h226", "=w540-h540")
                ?.replace("=w120-h120", "=w540-h540")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSongSelected(
                            PlayerState(
                                title = s.title,
                                artist = s.artists.joinToString { it.name },
                                artUrl = hdThumb,
                                videoId = s.id
                            )
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(hdThumb).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.title, color = Color.White, fontSize = 15.sp,
                        fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.artists.joinToString { it.name }, color = Color.Gray,
                        fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                }
            }
        }
    }
}
