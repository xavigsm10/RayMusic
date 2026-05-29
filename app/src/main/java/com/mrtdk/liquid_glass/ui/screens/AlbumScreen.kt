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
import androidx.compose.material.icons.filled.Check
import com.mrtdk.glass.GlassBox
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import com.mrtdk.liquid_glass.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
    onSongSelected: (PlayerState) -> Unit,
    onDominantColorChanged: (Color) -> Unit = {},
    isPaused: Boolean = false
) {
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    var tracks by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var dominantColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }
    val savedItems by LibraryManager.savedItems.collectAsState()
    val isSaved = savedItems.any { it.id == albumState.id }

    val isMichaelAlbum = albumState.title.equals("Michael: Songs From the Motion Picture", ignoreCase = true)
    val isThrillerAlbum = albumState.title.equals("Thriller", ignoreCase = true) || 
                          (albumState.title.contains("Thriller", ignoreCase = true) && albumState.artist.contains("Michael Jackson", ignoreCase = true))
    val isAfterHoursAlbum = albumState.title.equals("After Hours", ignoreCase = true) ||
                            (albumState.title.contains("After Hours", ignoreCase = true) && albumState.artist.contains("The Weeknd", ignoreCase = true))
    val isAroundTheFurAlbum = albumState.title.equals("Around the Fur", ignoreCase = true) ||
                              (albumState.title.contains("Around the Fur", ignoreCase = true) && albumState.artist.contains("Deftones", ignoreCase = true))

    val hdThumb = albumState.thumbnail
        ?.replace("=w226-h226", "=w720-h720")
        ?.replace("=w120-h120", "=w720-h720")

    val headerArt = hdThumb
    val songArtUrl = hdThumb

    val albumHeightRatio = when {
        isAroundTheFurAlbum -> 1.40f
        isAfterHoursAlbum -> 1.62f
        else -> 1.05f
    }

    val isVerticalAlbum = isAfterHoursAlbum || isAroundTheFurAlbum

    val animatedImageLoader = remember(context) {
        coil.ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(coil.decode.ImageDecoderDecoder.Factory())
                } else {
                    add(coil.decode.GifDecoder.Factory())
                }
            }
            .build()
    }

    var animatedArtworkUrl by remember(albumState.artist, albumState.title) {
        mutableStateOf(com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.get(albumState.artist, albumState.title))
    }

    LaunchedEffect(albumState.artist, albumState.title) {
        val artist = albumState.artist
        val album = albumState.title
        if (animatedArtworkUrl != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val cleanArtist = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(artist)
                val cleanAlbum = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(album)
                val encodedArtist = java.net.URLEncoder.encode(cleanArtist, "UTF-8")
                val encodedAlbum = java.net.URLEncoder.encode(cleanAlbum, "UTF-8")
                val url = java.net.URL("https://artwork.m8tec.top/api/v1/artwork/search?artist=$encodedArtist&album=$encodedAlbum")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().readText()
                    val obj = org.json.JSONObject(text)
                    val isVertical = album.contains("After Hours", ignoreCase = true) ||
                            album.contains("Around the Fur", ignoreCase = true)
                    val streamUrl = if (isVertical) {
                        obj.optString("url_tall").takeIf { it.isNotBlank() } 
                            ?: obj.optString("url").takeIf { it.isNotBlank() }
                    } else {
                        obj.optString("url").takeIf { it.isNotBlank() } 
                            ?: obj.optString("url_tall").takeIf { it.isNotBlank() }
                    }
                    if (!streamUrl.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            animatedArtworkUrl = streamUrl
                            com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.put(artist, album, streamUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Extract dominant colour
    LaunchedEffect(headerArt) {
        if (!headerArt.isNullOrBlank()) {
            val request = ImageRequest.Builder(context)
                .data(headerArt).allowHardware(false).size(200).build()
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
                    var r = 0L; var g = 0L; var b = 0L
                    val y = bmp.height - 1
                    val w = bmp.width
                    for (x in 0 until w) {
                        val pixel = bmp.getPixel(x, y)
                        r += android.graphics.Color.red(pixel)
                        g += android.graphics.Color.green(pixel)
                        b += android.graphics.Color.blue(pixel)
                    }
                    val sampledColor = Color((r / w).toInt(), (g / w).toInt(), (b / w).toInt())
                    dominantColor = sampledColor
                    onDominantColorChanged(sampledColor)
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
                    .aspectRatio(1f / albumHeightRatio),
                useShader = true,
                content = {
                    // Base sharp album cover (always drawn in background to prevent black flashes)
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(headerArt).crossfade(true).build(),
                        imageLoader = animatedImageLoader,
                        contentDescription = albumState.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )

                    var isVideoPlaying by remember(albumState.id) { mutableStateOf(false) }
                    val currentAnimatedUrl = animatedArtworkUrl

                    if (!currentAnimatedUrl.isNullOrBlank()) {
                        com.mrtdk.liquid_glass.ui.components.AnimatedArtworkPlayer(
                            videoUrl = currentAnimatedUrl,
                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (isVideoPlaying) 1f else 0f },
                            isPaused = isPaused,
                            onPlaybackStarted = { isVideoPlaying = true },
                            onFrameCaptured = { frameBitmap ->
                                try {
                                    var r = 0L; var g = 0L; var b = 0L
                                    val y = frameBitmap.height - 1
                                    val w = frameBitmap.width
                                    for (x in 0 until w) {
                                        val pixel = frameBitmap.getPixel(x, y)
                                        r += android.graphics.Color.red(pixel)
                                        g += android.graphics.Color.green(pixel)
                                        b += android.graphics.Color.blue(pixel)
                                    }
                                    val sampledColor = Color((r / w).toInt(), (g / w).toInt(), (b / w).toInt())
                                    dominantColor = sampledColor
                                    onDominantColorChanged(sampledColor)
                                } catch (e: Exception) { }
                            }
                        )
                    }
                    // Gradient: transparent → dominant color at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.75f to Color.Transparent,
                                    1.0f to dominantColor
                                )
                            )
                    )
                    if (isVerticalAlbum) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp)
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = albumState.title,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            offset = Offset(2f, 2f),
                                            blurRadius = 4f
                                        )
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = albumState.artist,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            offset = Offset(2f, 2f),
                                            blurRadius = 4f
                                        )
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (albumState.year != null) {
                                    Text(
                                        text = albumState.year.toString(),
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        style = TextStyle(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.8f),
                                                offset = Offset(2f, 2f),
                                                blurRadius = 4f
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
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
        if (!isVerticalAlbum) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = albumState.title,
                    color = contentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = albumState.artist,
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (albumState.year != null) {
                    Text(
                        text = albumState.year.toString(),
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

        // ── ACTION BUTTONS: shuffle | ▶ Play | + ──────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                        .clickable {
                            if (tracks.isNotEmpty()) {
                                val shuffledTracks = tracks.shuffled()
                                val s = shuffledTracks.first()
                                val albumQueue = shuffledTracks.drop(1).map { t ->
                                    QueueItem(
                                        title = t.title,
                                        artist = t.artists.joinToString { it.name },
                                        artUrl = songArtUrl,
                                        videoId = t.id,
                                        album = albumState.title
                                    )
                                }
                                onSongSelected(
                                    PlayerState(
                                        title = s.title,
                                        artist = s.artists.joinToString { it.name },
                                        artUrl = songArtUrl,
                                        videoId = s.id,
                                        queue = albumQueue,
                                        isExclusiveQueue = true,
                                        album = albumState.title
                                    )
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.shuffle),
                        contentDescription = "Shuffle",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
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
                                        artUrl = songArtUrl,
                                        videoId = t.id,
                                        album = albumState.title
                                    )
                                }
                                onSongSelected(
                                    PlayerState(
                                        title = s.title,
                                        artist = s.artists.joinToString { it.name },
                                        artUrl = songArtUrl,
                                        videoId = s.id,
                                        queue = albumQueue,
                                        isExclusiveQueue = true,
                                        album = albumState.title
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
                            }, dominantColor)
                        }
                        Text("Play", color = dominantColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // + button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.15f))
                        .clickable { 
                            if (isSaved) {
                                LibraryManager.removeItem(albumState.id)
                            } else {
                                LibraryManager.saveItem(LibraryItem(
                                    id = albumState.id,
                                    title = albumState.title,
                                    subtitle = albumState.artist,
                                    thumbnail = hdThumb,
                                    type = ItemType.ALBUM
                                ))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isSaved) Icons.Default.Check else Icons.Default.Add, "Add/Remove", tint = contentColor, modifier = Modifier.size(20.dp))
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
                                artUrl = songArtUrl,
                                videoId = t.id,
                                album = albumState.title
                            )
                        }
                        onSongSelected(
                            PlayerState(
                                title = song.title,
                                artist = song.artists.joinToString { it.name },
                                artUrl = songArtUrl,
                                videoId = song.id,
                                queue = albumQueue,
                                isExclusiveQueue = true,
                                album = albumState.title
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
