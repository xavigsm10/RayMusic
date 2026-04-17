package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.IosShare
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
import androidx.compose.ui.graphics.luminance
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AlbumState(
    val id: String,        // browseId
    val playlistId: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val year: Int? = null
)

@Composable
fun AlbumScreen(
    albumState: AlbumState,
    onBack: () -> Unit,
    onSongSelected: (PlayerState) -> Unit
) {
    val context = LocalContext.current
    var tracks by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var dominantColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }

    val hdThumb = albumState.thumbnail
        ?.replace("=w226-h226", "=w720-h720")
        ?.replace("=w120-h120", "=w720-h720")

    // Extract dominant colour
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
                    dominantColor = Color(bmp.getPixel(bmp.width / 2, bmp.height - 1))
                } catch (e: Exception) { }
            }
        }
    }

    // Load album/playlist tracks
    LaunchedEffect(albumState.id) {
        withContext(Dispatchers.IO) {
            val isAlbum = albumState.id.startsWith("MPREb") || albumState.id.startsWith("FEmusic")
            if (isAlbum) {
                YouTube.album(albumState.id).onSuccess { albumPage ->
                    tracks = albumPage.songs
                }.onFailure {
                    val pId = albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")
                    YouTube.playlist(pId).onSuccess { playlistPage ->
                        tracks = playlistPage.songs
                    }
                }
            } else {
                val pId = albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")
                YouTube.playlist(pId).onSuccess { playlistPage ->
                    tracks = playlistPage.songs
                }.onFailure {
                    YouTube.album(albumState.id).onSuccess { albumPage ->
                        tracks = albumPage.songs
                    }
                }
            }
        }
    }

    val isLightBackground = dominantColor.luminance() > 0.6f
    val contentColor = if (isLightBackground) Color(0xFF1E1E1E) else Color.White

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(dominantColor)
    ) {
        // ── HERO: Album art with gradient overlay ──────────────────
        item {
            com.mrtdk.glass.GlassContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                content = {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(hdThumb).crossfade(true).build(),
                        contentDescription = albumState.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient: transparent → dominant color at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.6f to Color.Transparent,
                                    0.85f to dominantColor.copy(alpha = 0.6f),
                                    1.0f to dominantColor
                                )
                            )
                    )
                },
                glassContent = {
                    val scope = this
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        scope.GlassBox(
                            modifier = Modifier.size(48.dp).clip(CircleShape).clickable { onBack() },
                            shape = CircleShape,
                            tint = dominantColor.copy(alpha = 0.35f),
                            blur = 0.8f,
                            centerDistortion = 0.2f,
                            scale = 0.02f,
                            warpEdges = 0.6f,
                            elevation = 8.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White)
                            }
                        }
                        scope.GlassBox(
                            modifier = Modifier.height(48.dp).width(104.dp).clip(RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            tint = dominantColor.copy(alpha = 0.35f),
                            blur = 0.8f,
                            centerDistortion = 0.2f,
                            scale = 0.02f,
                            warpEdges = 0.6f,
                            elevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.IosShare, "Share", tint = Color.White, modifier = Modifier.size(24.dp).clickable {
                                    val shareUrl = "https://music.youtube.com/playlist?list=${albumState.playlistId.ifEmpty { albumState.id }.removePrefix("VL")}"
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, albumState.title)
                                        putExtra(android.content.Intent.EXTRA_TEXT, "$shareUrl")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir"))
                                })
                                Icon(Icons.Default.MoreVert, "More", tint = Color.White, modifier = Modifier.size(24.dp).clickable { })
                            }
                        }
                    }
                }
            )
        }

        // ── ALBUM INFO (on dominant color background) ──────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(dominantColor)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = albumState.title,
                    color = contentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = albumState.artist,
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        if (albumState.year != null) append("${albumState.year} · ")
                        append("🎵 Dolby Atmos · 🔊 Lossless")
                    },
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        // ── ACTION BUTTONS: shuffle | ▶ Play | + ──────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(dominantColor)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.15f))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val w = size.width; val h = size.height
                        drawLine(contentColor, androidx.compose.ui.geometry.Offset(0f, 0f),
                            androidx.compose.ui.geometry.Offset(w, h), strokeWidth = 2.5f)
                        drawLine(contentColor, androidx.compose.ui.geometry.Offset(w, 0f),
                            androidx.compose.ui.geometry.Offset(0f, h), strokeWidth = 2.5f)
                    }
                }

                // ▶ Play button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(contentColor)
                        .clickable {
                            tracks.firstOrNull()?.let { s ->
                                val albumQueue = tracks.drop(1).map { t ->
                                    QueueItem(
                                        title = t.title,
                                        artist = t.artists.joinToString { it.name },
                                        artUrl = hdThumb,
                                        videoId = t.id
                                    )
                                }
                                onSongSelected(
                                    PlayerState(
                                        title = s.title,
                                        artist = s.artists.joinToString { it.name },
                                        artUrl = hdThumb,
                                        videoId = s.id,
                                        queue = albumQueue
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
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawPath(Path().apply {
                                moveTo(0f, 0f); lineTo(size.width, size.height / 2f)
                                lineTo(0f, size.height); close()
                            }, if (isLightBackground) Color.White else Color.Black)
                        }
                        Text("Play", color = if (isLightBackground) Color.White else Color.Black, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // + button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.15f))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, "Add", tint = contentColor, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Transition from dominant color to black
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(dominantColor)
            )
        }

        // ── TRACK LIST ─────────────────────────────────────────────
        items(tracks.size) { i ->
            val song = tracks[i]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(dominantColor)
                    .clickable {
                        val albumQueue = tracks.drop(i + 1).map { t ->
                            QueueItem(
                                title = t.title,
                                artist = t.artists.joinToString { it.name },
                                artUrl = hdThumb,
                                videoId = t.id
                            )
                        }
                        onSongSelected(
                            PlayerState(
                                title = song.title,
                                artist = song.artists.joinToString { it.name },
                                artUrl = hdThumb,
                                videoId = song.id,
                                queue = albumQueue
                            )
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${i + 1}",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    modifier = Modifier.width(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title, color = contentColor, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (song.artists.isNotEmpty()) {
                        Text(
                            song.artists.joinToString { it.name },
                            color = contentColor.copy(alpha = 0.6f), fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreVert, null, tint = contentColor.copy(alpha = 0.6f))
                }
            }
            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp)
                .padding(start = 48.dp)
                .background(contentColor.copy(alpha = 0.06f)))
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
