package com.mrtdk.liquid_glass.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.playback.PlaybackQueue
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import com.mrtdk.liquid_glass.ui.screens.QueueItem
import com.mrtdk.liquid_glass.ui.screens.downloadSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryContextMenuTarget(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnail: String?,
    val type: ItemType,
    val album: String? = null,
    val albumId: String? = null,
    val playlistId: String? = null
)

@Composable
fun AppleMusicLibraryContextMenu(
    target: LibraryContextMenuTarget,
    onDismiss: () -> Unit,
    onOpenDetail: () -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    pivotBounds: Rect? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }

    val savedItems by LibraryManager.savedItems.collectAsState()
    val isFavorite = remember(savedItems, target.id) { savedItems.any { it.id == target.id } }
    val isPinned = remember(target.id) { LibraryManager.isItemPinned(target.id) }

    val isAlbum = target.type == ItemType.ALBUM
    val isSong = target.type == ItemType.SONG
    val isArtist = target.type == ItemType.ARTIST

    val morphAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        morphAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    val morphProgress = morphAnim.value

    fun handleDismiss(action: (() -> Unit)? = null) {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            morphAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMedium
                )
            )
            action?.invoke()
            onDismiss()
        }
    }

    BackHandler(enabled = true) {
        handleDismiss()
    }

    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = { handleDismiss() }
    ) {
        val scrimAlpha = (morphProgress * 0.42f).coerceIn(0f, 0.42f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { handleDismiss() }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val menuWidth = 295.dp
                val estimatedHeight = 520.dp

                val screenWidthDp = maxWidth
                val screenHeightDp = maxHeight

                val startLeft = if (pivotBounds != null) with(density) { pivotBounds.left.toDp() } else (screenWidthDp - menuWidth) / 2
                val startTop = if (pivotBounds != null) with(density) { pivotBounds.top.toDp() } else (screenHeightDp - estimatedHeight) / 2
                val startWidth = if (pivotBounds != null) with(density) { pivotBounds.width.toDp() } else 48.dp
                val startHeight = if (pivotBounds != null) with(density) { pivotBounds.height.toDp() } else 48.dp

                val targetLeft = ((screenWidthDp - menuWidth) / 2).coerceIn(16.dp, (screenWidthDp - menuWidth - 16.dp).coerceAtLeast(16.dp))
                val targetTop = if (pivotBounds != null) {
                    val pivotCenterYDp = startTop + startHeight / 2
                    val preferred = pivotCenterYDp - estimatedHeight * 0.45f
                    preferred.coerceIn(40.dp, (screenHeightDp - estimatedHeight - 20.dp).coerceAtLeast(40.dp))
                } else {
                    ((screenHeightDp - estimatedHeight) / 2).coerceAtLeast(40.dp)
                }

                val currentLeft = androidx.compose.ui.unit.lerp(startLeft, targetLeft, morphProgress)
                val currentTop = androidx.compose.ui.unit.lerp(startTop, targetTop, morphProgress)
                val currentWidth = androidx.compose.ui.unit.lerp(startWidth, menuWidth, morphProgress)
                val scale = 0.82f + 0.18f * morphProgress
                val contentAlpha = ((morphProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)

                Column(
                    modifier = Modifier
                        .offset(x = currentLeft, y = currentTop)
                        .width(currentWidth)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = contentAlpha
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── 1. Top Header Card (Artwork + Title + Artist + Chevron >) ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF2C2C2E).copy(alpha = 0.94f))
                            .border(
                                width = 0.6.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color.White.copy(alpha = 0.15f)),
                                onClick = { handleDismiss { onOpenDetail() } }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Artwork Thumbnail
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(if (isArtist) CircleShape else RoundedCornerShape(10.dp))
                                .background(Color(0xFF1C1C1E)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!target.thumbnail.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(target.thumbnail)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = target.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = if (isArtist) Icons.Default.Mic else if (isAlbum) Icons.Default.Album else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title & Subtitle
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = target.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = target.subtitle,
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Chevron Right
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // ── 2. Menu Card (3 Action buttons row + vertical menu items) ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF2C2C2E).copy(alpha = 0.94f))
                            .border(
                                width = 0.6.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(22.dp)
                            )
                    ) {
                        // 2A. Top 3 Action buttons row: [ Descargar ] [ Agregar a favoritos ] [ Compartir ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Descargar
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        handleDismiss {
                                            if (isSong) {
                                                downloadSong(context, target.id, target.title, target.subtitle, target.thumbnail, target.album)
                                                Toast.makeText(context, "Descargando canción...", Toast.LENGTH_SHORT).show()
                                            } else {
                                                scope.launch(Dispatchers.IO) {
                                                    val songs = com.echo.innertube.YouTube.album(target.id).getOrNull()?.songs.orEmpty()
                                                    if (songs.isNotEmpty()) {
                                                        songs.forEach { s ->
                                                            downloadSong(context, s.id, s.title, s.artists.joinToString { it.name }, s.thumbnail, target.title)
                                                        }
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "Descargando canciones del álbum...", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "Descargando...", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowCircleDown,
                                    contentDescription = "Descargar",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Descargar",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }

                            // Agregar a favoritos
                            Column(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val libItem = LibraryItem(
                                            id = target.id,
                                            title = target.title,
                                            subtitle = target.subtitle,
                                            thumbnail = target.thumbnail,
                                            type = target.type,
                                            album = target.album
                                        )
                                        if (isFavorite) {
                                            LibraryManager.removeItem(target.id)
                                            Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                                        } else {
                                            LibraryManager.saveItem(libItem)
                                            Toast.makeText(context, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favoritos",
                                    tint = if (isFavorite) Color(0xFFFA243C) else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isFavorite) "En favoritos" else "Agregar a favoritos",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }

                            // Compartir
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        handleDismiss {
                                            val url = if (isAlbum) {
                                                val pId = target.playlistId ?: target.albumId ?: target.id
                                                "https://music.youtube.com/playlist?list=${pId.removePrefix("VL")}"
                                            } else {
                                                "https://music.youtube.com/watch?v=${target.id}"
                                            }
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, target.title)
                                                putExtra(Intent.EXTRA_TEXT, url)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Compartir"))
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.compartir),
                                    contentDescription = "Compartir",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Compartir",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.1f),
                            thickness = 0.5.dp
                        )

                        // 2B. Vertical Menu Items List
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            // 1. Reproducir
                            LibraryMenuItemRow(
                                icon = Icons.Default.PlayArrow,
                                title = "Reproducir",
                                onClick = {
                                    handleDismiss {
                                        if (isSong) {
                                            onSongSelected(
                                                PlayerState(
                                                    title = target.title,
                                                    artist = target.subtitle,
                                                    artUrl = target.thumbnail,
                                                    videoId = target.id,
                                                    album = target.album,
                                                    albumId = target.albumId
                                                )
                                            )
                                        } else {
                                            scope.launch(Dispatchers.IO) {
                                                val songs = com.echo.innertube.YouTube.album(target.id).getOrNull()?.songs.orEmpty()
                                                val first = songs.firstOrNull()
                                                if (first != null) {
                                                    withContext(Dispatchers.Main) {
                                                        onSongSelected(
                                                            PlayerState(
                                                                title = first.title,
                                                                artist = first.artists.joinToString { it.name },
                                                                artUrl = target.thumbnail,
                                                                videoId = first.id,
                                                                album = target.title
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )

                            // 2. Aleatorio
                            LibraryMenuItemRow(
                                icon = Icons.Default.Shuffle,
                                title = "Aleatorio",
                                onClick = {
                                    handleDismiss {
                                        if (isSong) {
                                            onSongSelected(
                                                PlayerState(
                                                    title = target.title,
                                                    artist = target.subtitle,
                                                    artUrl = target.thumbnail,
                                                    videoId = target.id,
                                                    album = target.album,
                                                    albumId = target.albumId
                                                )
                                            )
                                        } else {
                                            scope.launch(Dispatchers.IO) {
                                                val songs = com.echo.innertube.YouTube.album(target.id).getOrNull()?.songs.orEmpty().shuffled()
                                                val first = songs.firstOrNull()
                                                if (first != null) {
                                                    withContext(Dispatchers.Main) {
                                                        onSongSelected(
                                                            PlayerState(
                                                                title = first.title,
                                                                artist = first.artists.joinToString { it.name },
                                                                artUrl = target.thumbnail,
                                                                videoId = first.id,
                                                                album = target.title
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )

                            // 3. Desanclar / Anclar álbum / canción
                            val pinText = if (isPinned) {
                                if (isAlbum) "Desanclar álbum" else if (isSong) "Desanclar canción" else "Desanclar"
                            } else {
                                if (isAlbum) "Anclar álbum" else if (isSong) "Anclar canción" else "Anclar"
                            }
                            LibraryMenuItemRow(
                                icon = Icons.Default.PushPin,
                                title = pinText,
                                tint = if (isPinned) Color(0xFFFA243C) else Color.White,
                                onClick = {
                                    handleDismiss {
                                        val next = !isPinned
                                        LibraryManager.setItemPinned(target.id, next)
                                        Toast.makeText(context, if (next) "Fijado en la biblioteca" else "Desfijado de la biblioteca", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // 4. Agregar a playlist
                            LibraryMenuItemRow(
                                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                                title = "Agregar a playlist",
                                onClick = {
                                    handleDismiss {
                                        Toast.makeText(context, "Elige una playlist para agregar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // 5. Poner a continuación
                            LibraryMenuItemRow(
                                icon = Icons.Default.QueuePlayNext,
                                title = "Poner a continuación",
                                onClick = {
                                    handleDismiss {
                                        val qItem = QueueItem(target.title, target.subtitle, target.thumbnail, target.id, target.album)
                                        PlaybackQueue.queue = listOf(qItem) + PlaybackQueue.queue
                                        PlaybackQueue.onQueueChanged?.invoke()
                                        Toast.makeText(context, "Se reproducirá a continuación", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // 6. Poner después (with subtitle)
                            LibraryMenuItemRow(
                                icon = Icons.Default.Queue,
                                title = "Poner después",
                                subtitle = target.title,
                                onClick = {
                                    handleDismiss {
                                        val qItem = QueueItem(target.title, target.subtitle, target.thumbnail, target.id, target.album)
                                        PlaybackQueue.queue = PlaybackQueue.queue + listOf(qItem)
                                        PlaybackQueue.onQueueChanged?.invoke()
                                        Toast.makeText(context, "Se agregó al final de la fila", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // 7. Sugerir menos
                            LibraryMenuItemRow(
                                icon = Icons.Default.ThumbDownOffAlt,
                                title = "Sugerir menos",
                                onClick = {
                                    handleDismiss {
                                        Toast.makeText(context, "Sugeriremos menos contenido como este", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryMenuItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
