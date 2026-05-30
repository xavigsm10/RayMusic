package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.echo.innertube.YouTube
import com.echo.innertube.models.AlbumItem
import com.echo.innertube.models.PlaylistItem
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.ArtistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class CategoriaState {
    var isLoading by mutableStateOf(true)
    var playlists by mutableStateOf<List<PlaylistItem>>(emptyList())
    var albums by mutableStateOf<List<AlbumItem>>(emptyList())
    var songs by mutableStateOf<List<SongItem>>(emptyList())
    var artists by mutableStateOf<List<ArtistItem>>(emptyList())
}

@Composable
fun CategoriaScreen(
    category: SearchCategory,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    onAlbumSelected: (AlbumState) -> Unit,
    onPlaylistSelected: (AlbumState) -> Unit,
    onArtistSelected: (ArtistState) -> Unit
) {
    val context = LocalContext.current
    val state = remember { CategoriaState() }
    
    LaunchedEffect(category.name) {
        state.isLoading = true
        withContext(Dispatchers.IO) {
            val d1 = async {
                YouTube.search(category.name, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST).getOrNull()?.items?.filterIsInstance<PlaylistItem>()?.take(16) ?: emptyList()
            }
            val d2 = async {
                val rawAlbums = YouTube.search(category.name, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()?.items?.filterIsInstance<AlbumItem>() ?: emptyList()
                rawAlbums.filter { a ->
                    val titleLower = a.title.lowercase()
                    val artistText = a.artists?.joinToString { it.name }?.lowercase() ?: ""
                    val isMix = titleLower.contains("mix") || titleLower.contains("compilation") ||
                        titleLower.contains("playlist") || titleLower.contains("mashup") ||
                        titleLower.contains("medley") || titleLower.contains("recopilación")
                    val isGenericArtist = artistText.contains("various") || artistText.contains("varios") ||
                        artistText.contains("topic") || artistText.isEmpty() || artistText.contains("mix")
                    !isMix && !isGenericArtist
                }.take(16)
            }
            val d3 = async {
                YouTube.search(category.name, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterIsInstance<SongItem>()?.take(24) ?: emptyList()
            }
            val d4 = async {
                YouTube.search(category.name, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()?.items?.filterIsInstance<ArtistItem>()?.take(16) ?: emptyList()
            }
            
            state.playlists = d1.await()
            state.albums = d2.await()
            state.songs = d3.await()
            state.artists = d4.await()
        }
        state.isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(innerPadding)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Atrás", tint = Color(0xFFFA243C))
            }
            Text(
                text = category.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE91E63))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 180.dp)
            ) {
                // ── CAROUSEL DESTACADO (Hero Carousel) ──────────
                if (state.songs.isNotEmpty()) {
                    val featuredItems = state.songs.take(5)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp)
                    ) {
                        items(featuredItems.size) { index ->
                            val song = featuredItems[index]
                            val hdThumb = song.thumbnail.let {
                                when {
                                    it.contains("=w") -> it.substringBefore("=w") + "=w720-h720-l90-rj"
                                    it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                    else -> it
                                }
                            }
                            Column(modifier = Modifier.width(340.dp).clickable {
                                onSongSelected(PlayerState(
                                    title = song.title,
                                    artist = song.artists.joinToString { it.name },
                                    artUrl = hdThumb,
                                    videoId = song.id
                                ))
                            }) {
                                Text("NUEVO DESCUBRIMIENTO", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(song.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.DarkGray)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                        contentDescription = "Hero Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Dark gradient overlay
                                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha=0.9f)),
                                        startY = 250f
                                    )))
                                    
                                    // Badge Top Left
                                    Row(
                                        modifier = Modifier.padding(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha=0.6f)).padding(horizontal=6.dp, vertical=4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Audio espacial con Dolby Atmos", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    // Bottom Text and Mini Cover
                                    Row(
                                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Explora lo mejor de ${song.artists.firstOrNull()?.name ?: category.name}.",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 20.sp
                                        )
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(),
                                            contentDescription = "Mini Cover",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Playlists (Apple Music Style - 2 Rows)
                if (state.playlists.isNotEmpty()) {
                    Text(
                        text = "Playlists destacadas",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                    )
                    
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(460.dp)
                    ) {
                        items(state.playlists) { item ->
                            Column(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable {
                                        onPlaylistSelected(
                                            AlbumState(
                                                id = item.id,
                                                playlistId = item.id,
                                                title = item.title,
                                                artist = item.author?.name ?: "",
                                                thumbnail = item.thumbnail
                                            )
                                        )
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.thumbnail?.replace("w226", "w400")?.replace("h226", "h400"))
                                        .crossfade(true).build(),
                                    contentDescription = "Playlist Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.author?.name ?: "",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Canciones (Apple Music Style - 4 Rows)
                if (state.songs.isNotEmpty()) {
                    Text(
                        text = "Mejores Canciones",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 12.dp)
                    )
                    
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(4),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(state.songs) { item ->
                            val hdThumb = item.thumbnail.let {
                                when {
                                    it.contains("=w") -> it.substringBefore("=w") + "=w300-h300-l90-rj"
                                    it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "mqdefault")
                                    else -> it
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .width(300.dp)
                                    .clickable {
                                        onSongSelected(
                                            PlayerState(
                                                title = item.title,
                                                artist = item.artists.joinToString { it.name },
                                                artUrl = hdThumb,
                                                videoId = item.id
                                            )
                                        )
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(hdThumb).crossfade(true).build(),
                                    contentDescription = "Song Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artists.joinToString { it.name },
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Albums (Apple Music Style - 2 Rows)
                if (state.albums.isNotEmpty()) {
                    Text(
                        text = "Álbumes imprescindibles",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 12.dp)
                    )
                    
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(460.dp)
                    ) {
                        items(state.albums) { item ->
                            Column(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable {
                                        onAlbumSelected(
                                            AlbumState(
                                                id = item.browseId,
                                                playlistId = item.playlistId,
                                                title = item.title,
                                                artist = item.artists?.joinToString { it.name } ?: "",
                                                thumbnail = item.thumbnail,
                                                year = item.year
                                            )
                                        )
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.thumbnail?.replace("w226", "w400")?.replace("h226", "h400"))
                                        .crossfade(true).build(),
                                    contentDescription = "Album Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.artists?.joinToString { it.name } ?: "",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Artists (Apple Music Style - Circular avatars)
                if (state.artists.isNotEmpty()) {
                    Text(
                        text = "Artistas que nos encantan",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 12.dp)
                    )
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.artists) { item ->
                            Column(
                                modifier = Modifier
                                    .width(120.dp)
                                    .clickable {
                                        onArtistSelected(ArtistState(id = item.id, name = item.title, thumbnail = item.thumbnail))
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.thumbnail?.let {
                                            when {
                                                it.contains("=w") -> it.substringBefore("=w") + "=w400-h400-l90-rj"
                                                else -> it
                                            }
                                        })
                                        .crossfade(true).build(),
                                    contentDescription = "Artist Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
