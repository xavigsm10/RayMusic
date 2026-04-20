package com.mrtdk.liquid_glass.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import com.mrtdk.glass.GlassBox
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
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    onAlbumSelected: (AlbumState) -> Unit
) {
    val context = LocalContext.current
    var topSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var albums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    val savedItems by LibraryManager.savedItems.collectAsState()

    // Dominant colour extracted from artist photo
    var dominantColor by remember { mutableStateOf(Color(0xFF111111)) }
    
    var artistThumb by remember { mutableStateOf(artistState.thumbnail) }

    val hdThumb = artistThumb
        ?.replace("=w226-h226", "=w800-h800")
        ?.replace("=w120-h120", "=w800-h800")

    // Extract dominant color from artist image
    LaunchedEffect(hdThumb) {
        if (!hdThumb.isNullOrBlank()) {
            val request = ImageRequest.Builder(context)
                .data(hdThumb).allowHardware(false).size(200).build()
            val result = coil.Coil.imageLoader(context).execute(request)
            if (result is coil.request.SuccessResult) {
                val drawable = result.drawable
                val bmp = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    ?: android.graphics.Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        android.graphics.Bitmap.Config.ARGB_8888
                    ).also { b ->
                        val c = android.graphics.Canvas(b)
                        drawable.setBounds(0, 0, c.width, c.height)
                        drawable.draw(c)
                    }
                try {
                    dominantColor = Color(bmp.getPixel(bmp.width / 2, bmp.height * 3 / 4))
                } catch (e: Exception) { }
            }
        }
    }

    // Load top songs for this artist
    LaunchedEffect(artistState.id) {
        withContext(Dispatchers.IO) {
            YouTube.search(artistState.name, YouTube.SearchFilter.FILTER_SONG).onSuccess { r ->
                topSongs = r.items.filterIsInstance<SongItem>().take(5)
            }
            YouTube.search(artistState.name, YouTube.SearchFilter.FILTER_ALBUM).onSuccess { r ->
                albums = r.items.filterIsInstance<AlbumItem>()
            }
            if (artistThumb == null) {
                YouTube.search(artistState.name, YouTube.SearchFilter.FILTER_ARTIST).onSuccess { r ->
                    val actualArtist = r.items.filterIsInstance<ArtistItem>().firstOrNull()
                    if (actualArtist != null) {
                        artistThumb = actualArtist.thumbnail
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── HERO: Artist photo with gradient overlay ───────────────
        item {
            com.mrtdk.glass.GlassContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                content = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(hdThumb).crossfade(true).build(),
                            contentDescription = artistState.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Gradient: transparent top → dominant color at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.55f to dominantColor.copy(alpha = 0.5f),
                                        1f to Color.Black
                                    )
                                )
                        )

                        // Artist name + label at bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = artistState.name,
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Artist",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 15.sp
                            )
                        }
                    }
                },
                glassContent = {
                    val scope = this
                    scope.GlassBox(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onBack() },
                        shape = CircleShape,
                        tint = dominantColor.copy(alpha = 0.35f),
                        blur = 0.8f,
                        centerDistortion = 0.2f,
                        scale = 0.02f,
                        warpEdges = 0.6f,
                        elevation = 8.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                }
            )
        }

        // ── ACTION BUTTONS: shuffle | ▶ Reproducir | + ────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button (circle)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        val w = size.width; val h = size.height
                        // Two crossing arrows — simplified as X shape
                        drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, 0f),
                            androidx.compose.ui.geometry.Offset(w, h), strokeWidth = 3f)
                        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w, 0f),
                            androidx.compose.ui.geometry.Offset(0f, h), strokeWidth = 3f)
                    }
                }

                // ▶ Reproducir button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                        .clickable {
                            topSongs.firstOrNull()?.let { s ->
                                onSongSelected(
                                    PlayerState(
                                        title = s.title,
                                        artist = s.artists.joinToString { it.name },
                                        artUrl = s.thumbnail,
                                        videoId = s.id
                                    )
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            drawPath(Path().apply {
                                moveTo(0f, 0f); lineTo(size.width, size.height / 2f)
                                lineTo(0f, size.height); close()
                            }, Color.Black)
                        }
                        Text(
                            text = "Reproducir",
                            color = Color.Black,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                val isSaved = savedItems.any { it.id == artistState.id }
                val iconTint by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSaved) Color(0xFFFA243C) else Color.White,
                    label = "iconTint"
                )
                
                // + button (circle)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            if (!isSaved) {
                                LibraryManager.saveItem(
                                    LibraryItem(
                                        id = artistState.id,
                                        title = artistState.name,
                                        subtitle = "Artist",
                                        thumbnail = artistThumb, // Use the fetched real artist image
                                        type = ItemType.ARTIST
                                    )
                                )
                            } else {
                                LibraryManager.removeItem(artistState.id)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Add",
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ── TOP SONGS ──────────────────────────────────────────────
        item {
            Text(
                text = "Top songs",
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(Color.Black).padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        items(topSongs.size) { i ->
            val song = topSongs[i]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .clickable {
                        onSongSelected(
                            PlayerState(
                                title = song.title,
                                artist = song.artists.joinToString { it.name },
                                artUrl = song.thumbnail,
                                videoId = song.id
                            )
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${i + 1}",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier.width(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, color = Color.White, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                }
            }
            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp)
                .background(Color.White.copy(alpha = 0.06f)))
        }

        // ── ALBUMS ─────────────────────────────────────────────────
        if (albums.isNotEmpty()) {
            item {
                Text(
                    text = "Albums",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Color.Black)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                )
                LazyRow(
                    modifier = Modifier.background(Color.Black),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(albums.size) { idx ->
                        val album = albums[idx]
                        val hdAlbumThumb = album.thumbnail
                            ?.replace("=w226-h226", "=w540-h540")
                            ?.replace("=w120-h120", "=w540-h540")
                        Column(modifier = Modifier.width(180.dp).clickable {
                            onAlbumSelected(
                                AlbumState(
                                    id = album.id,
                                    playlistId = album.playlistId ?: album.id,
                                    title = album.title,
                                    artist = artistState.name,
                                    thumbnail = album.thumbnail,
                                    year = album.year as? Int ?: album.year?.toString()?.toIntOrNull()
                                )
                            )
                        }) {
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(hdAlbumThumb).crossfade(true).build(),
                                    contentDescription = album.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(album.title, color = Color.White, fontSize = 15.sp,
                                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${album.year ?: ""}", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
