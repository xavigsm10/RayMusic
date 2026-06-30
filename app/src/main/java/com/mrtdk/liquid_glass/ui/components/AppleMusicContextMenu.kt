package com.mrtdk.liquid_glass.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrtdk.glass.GlassBox
import com.mrtdk.glass.GlassBoxScope
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.data.Playlist
import com.mrtdk.liquid_glass.playback.PlaybackQueue
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import com.mrtdk.liquid_glass.ui.screens.QueueItem
import com.mrtdk.liquid_glass.ui.screens.downloadSong
import com.echo.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.ui.draw.blur
import com.kyant.shapes.Capsule
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.ExperimentalAnimationApi

data class ContextMenuSong(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val album: String? = null,
    val artistId: String? = null,
    val albumId: String? = null
)

data class ContextMenuAlbum(
    val id: String,
    val playlistId: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val year: Int? = null
)

@Composable
fun GlassBoxScope.AppleMusicSongMenu(
    song: ContextMenuSong,
    onDismiss: () -> Unit,
    onGoToAlbum: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    onSongSelected: (PlayerState) -> Unit
) {
    val glassScope = this
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var isPlaylistsScreen by remember { mutableStateOf(false) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }

    val savedItems by LibraryManager.savedItems.collectAsState()
    val isSaved = remember(savedItems, song.id) { savedItems.any { it.id == song.id } }

    val libraryItem = remember(song) {
        LibraryItem(
            id = song.id,
            title = song.title,
            subtitle = song.artist,
            thumbnail = song.thumbnail,
            type = ItemType.SONG,
            album = song.album
        )
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    fun handleDismiss() {
        visible = false
        onDismiss()
    }

    BackHandler(enabled = visible) {
        handleDismiss()
    }

    // Semi-transparent overlay to tap and dismiss
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
            val currentQueueSong = PlaybackQueue.currentSong
            val bottomPadding = if (currentQueueSong != null) 176.dp else 100.dp

            glassScope.GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = bottomPadding),
                blur = 0.9f,
                scale = 0.02f,
                tint = Color(0xFF1E1E1E).copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp),
                elevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (!isPlaylistsScreen) {
                        // Song Details Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(song.thumbnail)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = song.artist,
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Like / Favorite toggle
                            IconButton(onClick = {
                                if (isSaved) {
                                    LibraryManager.removeItem(song.id)
                                    Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                                } else {
                                    LibraryManager.saveItem(libraryItem)
                                    Toast.makeText(context, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorito",
                                    tint = if (isSaved) Color(0xFFFA243C) else Color.White
                                )
                            }

                            // Close button
                            IconButton(onClick = { handleDismiss() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Horizontal Action Row (Play Next, Save to Playlist, Share)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            HorizontalActionButton(
                                icon = Icons.Default.QueuePlayNext,
                                label = "Siguiente",
                                onClick = {
                                    val current = PlaybackQueue.currentSong
                                    val qItem = QueueItem(song.title, song.artist, song.thumbnail, song.id, song.album)
                                    if (current == null) {
                                        onSongSelected(PlayerState(song.title, song.artist, song.thumbnail, song.id, album = song.album))
                                    } else {
                                        PlaybackQueue.queue = listOf(qItem) + PlaybackQueue.queue
                                        PlaybackQueue.onQueueChanged?.invoke()
                                        Toast.makeText(context, "Se reproducirá a continuación", Toast.LENGTH_SHORT).show()
                                    }
                                    handleDismiss()
                                }
                            )

                            HorizontalActionButton(
                                icon = Icons.Default.PlaylistAdd,
                                label = "Playlist",
                                onClick = { isPlaylistsScreen = true }
                            )

                            HorizontalActionButton(
                                icon = Icons.Default.Share,
                                label = "Compartir",
                                onClick = {
                                    val shareUrl = "https://music.youtube.com/watch?v=${song.id}"
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, song.title)
                                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir canción"))
                                    handleDismiss()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Vertical Actions List
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            VerticalMenuActionItem(
                                icon = Icons.Default.Radio,
                                label = "Iniciar radio",
                                onClick = {
                                    onSongSelected(
                                        PlayerState(
                                            title = song.title,
                                            artist = song.artist,
                                            artUrl = song.thumbnail,
                                            videoId = song.id,
                                            isExclusiveQueue = false,
                                            album = song.album
                                        )
                                    )
                                    handleDismiss()
                                }
                            )

                            VerticalMenuActionItem(
                                icon = Icons.Default.Queue,
                                label = "Agregar a la fila",
                                onClick = {
                                    val current = PlaybackQueue.currentSong
                                    val qItem = QueueItem(song.title, song.artist, song.thumbnail, song.id, song.album)
                                    if (current == null) {
                                        onSongSelected(PlayerState(song.title, song.artist, song.thumbnail, song.id, album = song.album))
                                    } else {
                                        PlaybackQueue.queue = PlaybackQueue.queue + listOf(qItem)
                                        PlaybackQueue.onQueueChanged?.invoke()
                                        Toast.makeText(context, "Añadido a la cola", Toast.LENGTH_SHORT).show()
                                    }
                                    handleDismiss()
                                }
                            )

                            VerticalMenuActionItem(
                                icon = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                label = if (isSaved) "Eliminar de biblioteca" else "Guardar en biblioteca",
                                onClick = {
                                    if (isSaved) {
                                        LibraryManager.removeItem(song.id)
                                        Toast.makeText(context, "Eliminado de biblioteca", Toast.LENGTH_SHORT).show()
                                    } else {
                                        LibraryManager.saveItem(libraryItem)
                                        Toast.makeText(context, "Añadido a biblioteca", Toast.LENGTH_SHORT).show()
                                    }
                                    handleDismiss()
                                }
                            )

                            VerticalMenuActionItem(
                                icon = Icons.Default.ArrowDownward,
                                label = "Descargar",
                                onClick = {
                                    downloadSong(context, song.id, song.title, song.artist, song.thumbnail, song.album)
                                    handleDismiss()
                                }
                            )

                            if (onGoToAlbum != null && !song.album.isNullOrBlank()) {
                                VerticalMenuActionItem(
                                    icon = Icons.Default.Album,
                                    label = "Ir al álbum",
                                    onClick = {
                                        onGoToAlbum()
                                        handleDismiss()
                                    }
                                )
                            }

                            if (onGoToArtist != null) {
                                VerticalMenuActionItem(
                                    icon = Icons.Default.Mic,
                                    label = "Ir al artista",
                                    onClick = {
                                        onGoToArtist()
                                        handleDismiss()
                                    }
                                )
                            }

                            VerticalMenuActionItem(
                                icon = Icons.Default.Info,
                                label = "Ver créditos de la canción",
                                onClick = { showCreditsDialog = true }
                            )

                            VerticalMenuActionItem(
                                icon = Icons.Default.PushPin,
                                label = "Fijar en Accesos directos",
                                onClick = {
                                    LibraryManager.saveItem(libraryItem)
                                    Toast.makeText(context, "Fijado en accesos directos", Toast.LENGTH_SHORT).show()
                                    handleDismiss()
                                }
                            )
                        }
                    } else {
                        // Playlists Selection Screen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { isPlaylistsScreen = false }) {
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Atrás", tint = Color(0xFFFA243C))
                            }
                            Text(
                                text = "Añadir a playlist",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { handleDismiss() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val playlists by LibraryManager.playlists.collectAsState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // "Create new playlist" action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showNewPlaylistDialog = true }
                                    .padding(vertical = 14.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFFFA243C),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Nueva playlist...",
                                    color = Color(0xFFFA243C),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Divider(color = Color.White.copy(alpha = 0.08f))

                            playlists.forEach { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            LibraryManager.addSongToPlaylist(playlist.id, libraryItem)
                                            Toast.makeText(context, "Añadido a ${playlist.name}", Toast.LENGTH_SHORT).show()
                                            handleDismiss()
                                        }
                                        .padding(vertical = 14.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = playlist.name,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${playlist.items.size} canciones",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Divider(color = Color.White.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }
        }
    }

    // New Playlist esmerilado alert dialog
    if (showNewPlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showNewPlaylistDialog = false }) {
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF2C2C2E))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Nueva playlist",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Nombre de la playlist", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFFFA243C),
                            focusedIndicatorColor = Color(0xFFFA243C)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showNewPlaylistDialog = false }) {
                            Text("Cancelar", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (playlistName.isNotBlank()) {
                                    LibraryManager.createPlaylist(playlistName)
                                    // Fetch the newly created playlist to add this song to it
                                    val newPlaylist = LibraryManager.playlists.value.firstOrNull { it.name == playlistName }
                                    newPlaylist?.let {
                                        LibraryManager.addSongToPlaylist(it.id, libraryItem)
                                    }
                                    Toast.makeText(context, "Playlist creada y canción añadida", Toast.LENGTH_SHORT).show()
                                    showNewPlaylistDialog = false
                                    handleDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFA243C))
                        ) {
                            Text("Crear", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Credits esmerilado dialog
    if (showCreditsDialog) {
        Dialog(onDismissRequest = { showCreditsDialog = false }) {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF2C2C2E))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Créditos de la canción",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    CreditItem(label = "Título", value = song.title)
                    CreditItem(label = "Artista", value = song.artist)
                    CreditItem(label = "Álbum", value = song.album ?: "Desconocido")
                    CreditItem(label = "ID del Video", value = song.id)
                    CreditItem(label = "Proveedor", value = "YouTube Music / InnerTube")

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showCreditsDialog = false
                            handleDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFA243C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entendido", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun GlassBoxScope.AppleMusicAlbumMenu(
    album: ContextMenuAlbum,
    onDismiss: () -> Unit,
    onAddAlbumToQueue: () -> Unit,
    onSaveAlbumToLibrary: () -> Unit,
    tracks: List<com.echo.innertube.models.SongItem>? = null
) {
    val glassScope = this
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    fun handleDismiss() {
        visible = false
        onDismiss()
    }

    BackHandler(enabled = visible) {
        handleDismiss()
    }

    // Full screen overlay to detect clicks and dismiss
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 60.dp, end = 16.dp)
                .wrapContentSize()
        ) {
            glassScope.GlassBox(
                modifier = Modifier
                    .width(220.dp)
                    .wrapContentHeight(),
                blur = 0.95f,
                scale = 0.05f,
                tint = Color(0xFF1E1E1E).copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp),
                elevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {

                    // 1. Add to queue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAddAlbumToQueue()
                                handleDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Queue, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add to Queue", color = Color.White, fontSize = 15.sp)
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    // 2. Save Playlist/Album
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSaveAlbumToLibrary()
                                handleDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FavoriteBorder, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Save Playlist", color = Color.White, fontSize = 15.sp)
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    // 3. Download for offline mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                handleDismiss()
                                // Run background download of all tracks of this album/playlist
                                Toast.makeText(context, "Obteniendo pistas del álbum...", Toast.LENGTH_SHORT).show()
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            val tracksToDownload = if (!tracks.isNullOrEmpty()) {
                                                tracks
                                            } else {
                                                val isAlbum = album.id.startsWith("MPREb") || album.id.startsWith("FEmusic")
                                                if (isAlbum) {
                                                    com.echo.innertube.YouTube.album(album.id).getOrNull()?.songs
                                                        ?: run {
                                                            val pId = album.playlistId.ifEmpty { album.id }.removePrefix("VL")
                                                            com.echo.innertube.YouTube.playlist(pId).getOrNull()?.songs
                                                        }
                                                } else {
                                                    val pId = album.playlistId.ifEmpty { album.id }.removePrefix("VL")
                                                    com.echo.innertube.YouTube.playlist(pId).getOrNull()?.songs
                                                        ?: run {
                                                            com.echo.innertube.YouTube.album(album.id).getOrNull()?.songs
                                                        }
                                                }
                                            }
                                            
                                            withContext(Dispatchers.Main) {
                                                if (tracksToDownload.isNullOrEmpty()) {
                                                    Toast.makeText(context, "No se pudieron obtener las pistas del álbum", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Iniciando descarga de ${tracksToDownload.size} pistas...", Toast.LENGTH_SHORT).show()
                                                    tracksToDownload.forEach { track ->
                                                        downloadSong(
                                                            context = context,
                                                            videoId = track.id,
                                                            title = track.title,
                                                            artist = track.artists.joinToString { it.name },
                                                            artUrl = track.thumbnail,
                                                            album = album.title,
                                                            silent = true
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al obtener pistas: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowDownward, null, tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Descargar para offline", color = Color(0xFFFA243C), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HorizontalActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun VerticalMenuActionItem(
    icon: ImageVector,
    label: String,
    iconTint: Color = Color.White.copy(alpha = 0.7f),
    textColor: Color = Color.White,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GlassBoxScope.AppleMusicPlaylistMenu(
    playlist: Playlist,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    dominantColor: Color,
    onDismiss: () -> Unit,
    onSortSelected: (String) -> Unit,
    currentSort: String,
    onSongSelected: (PlayerState) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var currentMenuScreen by remember { mutableStateOf("main") }

    val favsStr = LibraryManager.getString("favorite_playlists", "") ?: ""
    val favList = remember(favsStr) { favsStr.split(",").filter { it.isNotBlank() }.toMutableList() }
    val isFavorite = remember(favList, playlist.id) { favList.contains(playlist.id) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    val isPinned = playlist.isPinned

    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "menuAlpha"
    )
    val cornerRadius by animateFloatAsState(
        targetValue = if (visible) 24f else 80f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuCornerRadius"
    )
    val blurPx by animateFloatAsState(
        targetValue = if (visible) 0f else 15f,
        animationSpec = tween(durationMillis = 180),
        label = "menuContentBlur"
    )

    fun handleDismiss() {
        visible = false
        onDismiss()
    }

    BackHandler(enabled = visible) {
        handleDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 60.dp, end = 16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .width(260.dp)
                .wrapContentHeight()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(cornerRadius.dp) },
                    effects = {
                        vibrancy()
                        blur(12f.dp.toPx())
                        lens(16f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(dominantColor.copy(alpha = 0.35f))
                    }
                )
                .clip(RoundedCornerShape(cornerRadius.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (blurPx > 0.1f) it.blur(blurPx.dp) else it }
                    .padding(vertical = 8.dp)
            ) {
                AnimatedContent(
                    targetState = currentMenuScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                    },
                    label = "menuScreenAnimation"
                ) { screen ->
                    when (screen) {
                        "main" -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    HorizontalActionButton(
                                        icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        label = stringResource(R.string.menu_favorite),
                                        tint = if (isFavorite) Color(0xFFFA243C) else Color.White
                                    ) {
                                        val newFavList = if (isFavorite) {
                                            favList.filter { it != playlist.id }
                                        } else {
                                            favList + playlist.id
                                        }
                                        LibraryManager.saveString("favorite_playlists", newFavList.joinToString(","))
                                        Toast.makeText(
                                            context,
                                            if (isFavorite) context.getString(R.string.menu_toast_removed_fav) else context.getString(R.string.menu_toast_added_fav),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    HorizontalActionButton(
                                        icon = Icons.Default.IosShare,
                                        label = stringResource(R.string.menu_share)
                                    ) {
                                        val shareUrl = if (playlist.id.startsWith("VL") || playlist.id.startsWith("PL")) {
                                            "https://music.youtube.com/playlist?list=${playlist.id.removePrefix("VL")}"
                                        } else {
                                            "https://raymusic.mrtdk.com/playlist/${playlist.id}"
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, playlist.name)
                                            putExtra(Intent.EXTRA_TEXT, "${context.getString(R.string.share_playlist_prefix)} $shareUrl")
                                        }
                                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.compartir)))
                                        handleDismiss()
                                    }
                                }

                                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                                VerticalMenuActionItem(
                                    icon = Icons.Default.PushPin,
                                    label = if (isPinned) stringResource(R.string.menu_unpin_playlist) else stringResource(R.string.menu_pin_playlist),
                                    iconTint = if (isPinned) Color(0xFFFA243C) else Color.White.copy(alpha = 0.7f)
                                ) {
                                    LibraryManager.togglePinPlaylist(playlist.id)
                                    handleDismiss()
                                }

                                VerticalMenuActionItem(
                                    icon = Icons.Default.PlaylistAdd,
                                    label = stringResource(R.string.menu_add_to_playlist)
                                ) {
                                    showAddToPlaylistDialog = true
                                }

                                VerticalMenuActionItem(
                                    icon = Icons.Default.Edit,
                                    label = stringResource(R.string.menu_edit)
                                ) {
                                    showEditDialog = true
                                }

                                VerticalMenuActionItem(
                                    icon = Icons.Default.People,
                                    label = stringResource(R.string.menu_start_collaboration)
                                ) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Collaboration Link", "https://raymusic.mrtdk.com/collab/${playlist.id}")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, context.getString(R.string.menu_toast_collaboration), Toast.LENGTH_SHORT).show()
                                    handleDismiss()
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currentMenuScreen = "sort" }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(R.string.menu_sort_by),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currentMenuScreen = "folder" }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(R.string.menu_move_to_folder),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                                VerticalMenuActionItem(
                                    icon = Icons.Default.QueueMusic,
                                    label = stringResource(R.string.menu_play_next)
                                ) {
                                    val songs = playlist.items.filter { it.type == ItemType.SONG }
                                    if (songs.isNotEmpty()) {
                                        val newItems = songs.map { t ->
                                            QueueItem(t.title, t.subtitle, playlist.coverUrl ?: t.thumbnail, t.id, t.album)
                                        }
                                        PlaybackQueue.queue = newItems + PlaybackQueue.queue
                                        PlaybackQueue.onQueueChanged?.invoke()
                                        Toast.makeText(context, context.getString(R.string.menu_play_next), Toast.LENGTH_SHORT).show()
                                    }
                                    handleDismiss()
                                }

                                VerticalMenuActionItem(
                                    icon = Icons.Default.ThumbDown,
                                    label = stringResource(R.string.menu_suggest_less)
                                ) {
                                    LibraryManager.saveString("suggest_less_playlist_${playlist.id}", "true")
                                    Toast.makeText(context, context.getString(R.string.menu_toast_suggest_less), Toast.LENGTH_SHORT).show()
                                    handleDismiss()
                                }

                                val downloadedSongs by LibraryManager.downloadedSongs.collectAsState()
                                val isAnyDownloaded = remember(downloadedSongs, playlist.items) {
                                    playlist.items.any { track -> downloadedSongs.any { it.id == track.id } }
                                }

                                VerticalMenuActionItem(
                                    icon = if (isAnyDownloaded) Icons.Default.DeleteOutline else Icons.Default.ArrowDownward,
                                    label = if (isAnyDownloaded) stringResource(R.string.menu_remove_download) else stringResource(R.string.menu_download),
                                    iconTint = if (isAnyDownloaded) Color(0xFFFA243C) else Color.White.copy(alpha = 0.7f)
                                ) {
                                    val songs = playlist.items.filter { it.type == ItemType.SONG }
                                    if (songs.isNotEmpty()) {
                                        if (isAnyDownloaded) {
                                            songs.forEach { song ->
                                                LibraryManager.deleteDownloadedSong(context, song.id)
                                            }
                                            Toast.makeText(context, context.getString(R.string.menu_remove_download), Toast.LENGTH_SHORT).show()
                                        } else {
                                            songs.forEach { song ->
                                                downloadSong(context, song.id, song.title, song.subtitle, song.thumbnail, playlist.name)
                                            }
                                            Toast.makeText(context, "${context.getString(R.string.descargando_ellipsis)} (${songs.size})", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    handleDismiss()
                                }
                            }
                        }

                        "sort" -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currentMenuScreen = "main" }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFFFA243C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(R.string.menu_sort_by),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                                val sortOptions = listOf(
                                    "default" to R.string.menu_sort_default,
                                    "title" to R.string.menu_sort_title,
                                    "artist" to R.string.menu_sort_artist,
                                    "album" to R.string.menu_sort_album
                                )

                                sortOptions.forEach { (optionKey, stringResId) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSortSelected(optionKey)
                                                handleDismiss()
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(stringResId),
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (currentSort == optionKey) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFFFA243C),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "folder" -> {
                            val foldersStr = LibraryManager.getString("playlist_folders", "") ?: ""
                            val folders = remember(foldersStr) { foldersStr.split(",").filter { it.isNotBlank() } }
                            val currentFolder = remember(playlist.id) { LibraryManager.getString("playlist_folder_${playlist.id}", "") ?: "" }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currentMenuScreen = "main" }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFFFA243C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(R.string.menu_move_folder_title),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    folders.forEach { folder ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    LibraryManager.saveString("playlist_folder_${playlist.id}", folder)
                                                    Toast.makeText(context, "${context.getString(R.string.menu_folder_created)}: $folder", Toast.LENGTH_SHORT).show()
                                                    handleDismiss()
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = folder,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (currentFolder == folder) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFA243C),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showNewFolderDialog = true }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFFFA243C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.menu_folder_new),
                                        color = Color(0xFFFA243C),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var editNameText by remember { mutableStateOf(playlist.name) }
        var selectedImageUri by remember { mutableStateOf<Uri?>(playlist.coverUrl?.let { Uri.parse(it) }) }
        val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
            }
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.menu_edit), color = Color.White) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C1C1E))
                            .clickable {
                                photoPickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val coverUrl = selectedImageUri ?: playlist.coverUrl ?: (if (playlist.items.isNotEmpty()) playlist.items.first().thumbnail else null)
                        if (coverUrl != null) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Edit Cover",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = { editNameText = it },
                        label = { Text(stringResource(R.string.nombre)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFA243C),
                            focusedLabelColor = Color(0xFFFA243C)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentUri = selectedImageUri
                        if (editNameText.isNotBlank()) {
                            val finalCoverUrl = if (currentUri != null && currentUri.scheme == "content") {
                                LibraryManager.savePlaylistCover(context, playlist.id, currentUri)
                            } else {
                                currentUri?.toString()
                            }
                            LibraryManager.updatePlaylist(playlist.id, editNameText, finalCoverUrl)
                        }
                        showEditDialog = false
                        handleDismiss()
                    }
                ) {
                    Text(stringResource(R.string.crear), color = Color(0xFFFA243C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancelar), color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2C2C2C)
        )
    }

    if (showAddToPlaylistDialog) {
        val playlists by LibraryManager.playlists.collectAsState()
        val targetPlaylists = playlists.filter { it.id != playlist.id }

        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = { Text(stringResource(R.string.menu_add_to_playlist), color = Color.White) },
            text = {
                if (targetPlaylists.isEmpty()) {
                    Text(stringResource(R.string.no_resultados_para, ""), color = Color.Gray)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        targetPlaylists.forEach { targetPl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playlist.items.forEach { song ->
                                            LibraryManager.addSongToPlaylist(targetPl.id, song)
                                        }
                                        Toast.makeText(context, "${context.getString(R.string.añadir_a_playlist)}: ${targetPl.name}", Toast.LENGTH_SHORT).show()
                                        showAddToPlaylistDialog = false
                                        handleDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(targetPl.name, color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text(stringResource(R.string.cancelar), color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2C2C2C)
        )
    }

    if (showNewFolderDialog) {
        var folderNameText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text(stringResource(R.string.menu_folder_new), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = folderNameText,
                    onValueChange = { folderNameText = it },
                    label = { Text(stringResource(R.string.menu_folder_name_label)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFA243C),
                        focusedLabelColor = Color(0xFFFA243C)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderNameText.isNotBlank()) {
                            val foldersStr = LibraryManager.getString("playlist_folders", "") ?: ""
                            val foldersList = foldersStr.split(",").filter { it.isNotBlank() }.toMutableList()
                            if (!foldersList.contains(folderNameText)) {
                                foldersList.add(folderNameText)
                                LibraryManager.saveString("playlist_folders", foldersList.joinToString(","))
                            }
                            LibraryManager.saveString("playlist_folder_${playlist.id}", folderNameText)
                            Toast.makeText(context, "${context.getString(R.string.menu_folder_created)}: $folderNameText", Toast.LENGTH_SHORT).show()
                        }
                        showNewFolderDialog = false
                        handleDismiss()
                    }
                ) {
                    Text(stringResource(R.string.crear), color = Color(0xFFFA243C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text(stringResource(R.string.cancelar), color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2C2C2C)
        )
    }
}

@Composable
fun GlassBoxScope.AppleMusicArtistMenu(
    artistId: String,
    artistName: String,
    artistThumb: String?,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    dominantColor: Color,
    onDismiss: () -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    topSongs: List<com.echo.innertube.models.SongItem>
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    val savedItems by LibraryManager.savedItems.collectAsState()
    val isFavorite = remember(savedItems, artistId) { savedItems.any { it.id == artistId } }

    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "menuAlpha"
    )
    val cornerRadius by animateFloatAsState(
        targetValue = if (visible) 24f else 80f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuCornerRadius"
    )
    val blurPx by animateFloatAsState(
        targetValue = if (visible) 0f else 15f,
        animationSpec = tween(durationMillis = 180),
        label = "menuContentBlur"
    )

    fun handleDismiss() {
        visible = false
        onDismiss()
    }

    BackHandler(enabled = visible) {
        handleDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 60.dp, end = 16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .width(260.dp)
                .wrapContentHeight()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(cornerRadius.dp) },
                    effects = {
                        vibrancy()
                        blur(12f.dp.toPx())
                        lens(16f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(dominantColor.copy(alpha = 0.35f))
                    }
                )
                .clip(RoundedCornerShape(cornerRadius.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (blurPx > 0.1f) it.blur(blurPx.dp) else it }
                    .padding(vertical = 8.dp)
            ) {
                // Horizontal actions: Favorito & Compartir
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HorizontalActionButton(
                        icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        label = if (isFavorite) stringResource(R.string.menu_artist_remove_favorite) else stringResource(R.string.menu_artist_add_favorite),
                        tint = if (isFavorite) Color(0xFFFA243C) else Color.White
                    ) {
                        if (isFavorite) {
                            LibraryManager.removeItem(artistId)
                            Toast.makeText(context, context.getString(R.string.menu_artist_toast_removed), Toast.LENGTH_SHORT).show()
                        } else {
                            LibraryManager.saveItem(LibraryItem(id = artistId, title = artistName, subtitle = "Artist", thumbnail = artistThumb, type = ItemType.ARTIST))
                            Toast.makeText(context, context.getString(R.string.menu_artist_toast_added), Toast.LENGTH_SHORT).show()
                        }
                    }

                    HorizontalActionButton(
                        icon = Icons.Default.IosShare,
                        label = stringResource(R.string.menu_artist_share)
                    ) {
                        val shareUrl = "https://music.youtube.com/channel/$artistId"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, artistName)
                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.compartir)))
                        handleDismiss()
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                // Vertical actions: Crear Emisora, Abrir en Clásica, Sugerir menos
                VerticalMenuActionItem(
                    icon = Icons.Default.Radio,
                    label = stringResource(R.string.menu_artist_create_radio)
                ) {
                    val firstSong = topSongs.firstOrNull()
                    if (firstSong != null) {
                        onSongSelected(PlayerState(
                            title = firstSong.title,
                            artist = firstSong.artists.joinToString { it.name },
                            artUrl = firstSong.thumbnail,
                            videoId = firstSong.id,
                            isExclusiveQueue = false,
                            queue = emptyList()
                        ))
                    } else {
                        Toast.makeText(context, "No hay canciones populares para crear emisora", Toast.LENGTH_SHORT).show()
                    }
                    handleDismiss()
                }

                VerticalMenuActionItem(
                    icon = Icons.Default.OpenInNew,
                    label = stringResource(R.string.menu_artist_open_classical)
                ) {
                    Toast.makeText(context, context.getString(R.string.menu_artist_toast_classical), Toast.LENGTH_SHORT).show()
                    handleDismiss()
                }

                VerticalMenuActionItem(
                    icon = Icons.Default.ThumbDown,
                    label = stringResource(R.string.menu_artist_suggest_less)
                ) {
                    LibraryManager.saveString("suggest_less_artist_$artistId", "true")
                    Toast.makeText(context, context.getString(R.string.menu_artist_toast_suggest_less), Toast.LENGTH_SHORT).show()
                    handleDismiss()
                }
            }
        }
    }
}

@Composable
fun GlassBoxScope.AppleMusicCreateMenu(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    onDismiss: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onCreateFolder: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val dominantColor by LibraryManager.currentDominantColor.collectAsState()
    val tintColor = remember(dominantColor) { dominantColor.copy(alpha = 0.35f) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "menuAlpha"
    )
    val cornerRadius by animateFloatAsState(
        targetValue = if (visible) 24f else 80f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuCornerRadius"
    )
    val blurPx by animateFloatAsState(
        targetValue = if (visible) 0f else 15f,
        animationSpec = tween(durationMillis = 180),
        label = "menuContentBlur"
    )

    fun handleDismiss() {
        visible = false
        onDismiss()
    }

    BackHandler(enabled = visible) {
        handleDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 60.dp, end = 16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .width(260.dp)
                .wrapContentHeight()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(cornerRadius.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(tintColor)
                    }
                )
                .clip(RoundedCornerShape(cornerRadius.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (blurPx > 0.1f) it.blur(blurPx.dp) else it }
                    .padding(vertical = 8.dp)
            ) {
                VerticalMenuActionItem(
                    icon = Icons.Default.Add,
                    label = stringResource(R.string.create_new_playlist)
                ) {
                    onCreatePlaylist()
                    handleDismiss()
                }

                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                VerticalMenuActionItem(
                    icon = Icons.Default.CreateNewFolder,
                    label = stringResource(R.string.create_new_folder)
                ) {
                    onCreateFolder()
                    handleDismiss()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GlassBoxScope.PlaylistsPageMoreMenu(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    onDismiss: () -> Unit,
    currentViewMode: String,
    onViewModeSelected: (String) -> Unit,
    currentSort: String,
    onSortSelected: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val dominantColor by LibraryManager.currentDominantColor.collectAsState()
    val tintColor = remember(dominantColor) { dominantColor.copy(alpha = 0.35f) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "menuAlpha"
    )
    val cornerRadius by animateFloatAsState(
        targetValue = if (visible) 24f else 80f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuCornerRadius"
    )
    val blurPx by animateFloatAsState(
        targetValue = if (visible) 0f else 15f,
        animationSpec = tween(durationMillis = 180),
        label = "menuContentBlur"
    )

    fun handleDismiss() {
        visible = false
        onDismiss()
    }

    BackHandler(enabled = visible) {
        handleDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 60.dp, end = 16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .width(260.dp)
                .wrapContentHeight()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(cornerRadius.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(tintColor)
                    }
                )
                .clip(RoundedCornerShape(cornerRadius.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (blurPx > 0.1f) it.blur(blurPx.dp) else it }
                    .padding(vertical = 8.dp)
            ) {
                // View Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onViewModeSelected("grid")
                            handleDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        if (currentViewMode == "grid") {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.GridView, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Cuadrícula", color = Color.White, fontSize = 15.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onViewModeSelected("list")
                            handleDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        if (currentViewMode == "list") {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Lista", color = Color.White, fontSize = 15.sp)
                }

                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                // Sort Options
                val sortOptions = listOf(
                    "title" to "Título",
                    "date_added" to "Fecha de inclusión",
                    "last_played" to "Fecha de última reproducción",
                    "last_updated" to "Fecha de actualización",
                    "type" to "Tipo de playlist"
                )

                sortOptions.forEach { (optionKey, optionLabel) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSortSelected(optionKey)
                                handleDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            if (currentSort == optionKey) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(optionLabel, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GlassBoxScope.PlaylistsPageSortMenu(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    onDismiss: () -> Unit,
    currentSort: String,
    onSortSelected: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val dominantColor by LibraryManager.currentDominantColor.collectAsState()
    val tintColor = remember(dominantColor) { dominantColor.copy(alpha = 0.35f) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "menuAlpha"
    )
    val cornerRadius by animateFloatAsState(
        targetValue = if (visible) 24f else 80f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuCornerRadius"
    )
    val blurPx by animateFloatAsState(
        targetValue = if (visible) 0f else 15f,
        animationSpec = tween(durationMillis = 180),
        label = "menuContentBlur"
    )

    fun handleDismiss() {
        visible = false
        onDismiss()
    }

    BackHandler(enabled = visible) {
        handleDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 60.dp, end = 16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .width(260.dp)
                .wrapContentHeight()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(cornerRadius.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(tintColor)
                    }
                )
                .clip(RoundedCornerShape(cornerRadius.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (blurPx > 0.1f) it.blur(blurPx.dp) else it }
                    .padding(vertical = 8.dp)
            ) {
                val sortOptions = listOf(
                    "title" to "Título",
                    "date_added" to "Fecha de inclusión",
                    "last_played" to "Fecha de última reproducción",
                    "last_updated" to "Fecha de actualización",
                    "type" to "Tipo de playlist"
                )

                sortOptions.forEach { (optionKey, optionLabel) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSortSelected(optionKey)
                                handleDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            if (currentSort == optionKey) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(optionLabel, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun GlassBoxScope.PlayerOptionsMenu(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    onDismiss: () -> Unit,
    playerState: PlayerState?,
    isSaved: Boolean,
    onToggleSaved: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    onAlbumSelected: (com.mrtdk.liquid_glass.ui.screens.AlbumState) -> Unit,
    pivotBounds: androidx.compose.ui.geometry.Rect? = null
) {
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "menuAlpha"
    )
    val cornerRadius by animateFloatAsState(
        targetValue = if (visible) 24f else 80f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuCornerRadius"
    )
    val blurPx by animateFloatAsState(
        targetValue = if (visible) 0f else 15f,
        animationSpec = tween(durationMillis = 180),
        label = "menuContentBlur"
    )

    val context = LocalContext.current

    fun handleDismiss() {
        visible = false
        onDismiss()
    }

    BackHandler(enabled = visible) {
        handleDismiss()
    }

    // Dynamic tint color for liquidglass effect
    val dominantColor by LibraryManager.currentDominantColor.collectAsState()
    val tintColor = remember(dominantColor) { dominantColor.copy(alpha = 0.35f) }

        Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val screenCenterX = constraints.maxWidth.toFloat() / 2f
        val screenCenterY = constraints.maxHeight.toFloat() / 2f
        val progress = ((scale - 0.4f) / 0.6f).coerceIn(0f, 1f)

        this@PlayerOptionsMenu.GlassBox(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    if (pivotBounds != null) {
                        translationX = (1f - progress) * (pivotBounds.center.x - screenCenterX)
                        translationY = (1f - progress) * (pivotBounds.center.y - screenCenterY)
                    }
                }
                .width(280.dp)
                .wrapContentHeight(),
            blur = 0.8f,
            scale = 0.02f,
            centerDistortion = 0.1f,
            warpEdges = 0.4f,
            elevation = 4.dp,
            shape = RoundedCornerShape(cornerRadius.dp),
            tint = dominantColor.copy(alpha = 0.25f),
            darkness = 0.2f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (blurPx > 0.1f) it.blur(blurPx.dp) else it }
                    .padding(vertical = 12.dp)
            ) {
                // Horizontal row of action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Descargar
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onDownload()
                                handleDismiss()
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ArrowCircleDown, contentDescription = stringResource(R.string.player_menu_download), tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.player_menu_download), color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }

                    // Favorito
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onToggleSaved()
                                handleDismiss()
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(if (isSaved) R.string.player_menu_favorite else R.string.player_menu_add_favorite),
                            tint = if (isSaved) Color(0xFFFA243C) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSaved) stringResource(R.string.player_menu_favorite) else stringResource(R.string.player_menu_add_favorite),
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Compartir
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (playerState?.videoId != null) {
                                    val shareUrl = "https://music.youtube.com/watch?v=${playerState.videoId}"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, playerState.title)
                                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                                    }
                                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.compartir)))
                                }
                                handleDismiss()
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = stringResource(R.string.player_menu_share), tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.player_menu_share), color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                // Vertical Actions List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Fijar canción
                    val isPinned = remember(playerState?.videoId) {
                        val key = "song_pinned_${playerState?.videoId ?: ""}"
                        LibraryManager.getString(key) == "true"
                    }
                    VerticalMenuActionItem(
                        icon = Icons.Default.PushPin,
                        label = if (isPinned) stringResource(R.string.player_menu_unpin_song) else stringResource(R.string.player_menu_pin_song)
                    ) {
                        if (playerState?.videoId != null) {
                            val key = "song_pinned_${playerState.videoId}"
                            val newPinned = !isPinned
                            LibraryManager.saveString(key, if (newPinned) "true" else "false")
                            Toast.makeText(context, if (newPinned) context.getString(R.string.toast_song_pinned) else context.getString(R.string.toast_song_unpinned), Toast.LENGTH_SHORT).show()
                        }
                        handleDismiss()
                    }

                    // Añadir a una playlist
                    VerticalMenuActionItem(
                        icon = Icons.Default.PlaylistAdd,
                        label = stringResource(R.string.player_menu_add_to_playlist)
                    ) {
                        onAddToPlaylist()
                        handleDismiss()
                    }

                    // Crear emisora
                    VerticalMenuActionItem(
                        icon = Icons.Default.Radio,
                        label = stringResource(R.string.player_menu_create_station)
                    ) {
                        if (playerState != null) {
                            onSongSelected(
                                PlayerState(
                                    title = playerState.title,
                                    artist = playerState.artist,
                                    artUrl = playerState.artUrl,
                                    videoId = playerState.videoId,
                                    isExclusiveQueue = false,
                                    album = playerState.album
                                )
                            )
                            Toast.makeText(context, context.getString(R.string.toast_starting_station, playerState.title), Toast.LENGTH_SHORT).show()
                        }
                        handleDismiss()
                    }

                    // Ir al álbum
                    VerticalMenuActionItem(
                        icon = Icons.Default.Album,
                        label = stringResource(R.string.player_menu_go_to_album)
                    ) {
                        if (playerState != null) {
                            if (!playerState.albumId.isNullOrBlank()) {
                                onAlbumSelected(
                                    com.mrtdk.liquid_glass.ui.screens.AlbumState(
                                        id = playerState.albumId,
                                        playlistId = playerState.albumId,
                                        title = playerState.album ?: playerState.title,
                                        artist = playerState.artist,
                                        thumbnail = playerState.artUrl?.toString()
                                    )
                                )
                                handleDismiss()
                            } else {
                                // Fallback: If offline/local or no internet
                                val isOffline = playerState.contentUri != null || (!playerState.album.isNullOrBlank() && LibraryManager.getDownloadedSongsForAlbum(playerState.album).isNotEmpty())
                                if (isOffline && !playerState.album.isNullOrBlank()) {
                                    onAlbumSelected(
                                        com.mrtdk.liquid_glass.ui.screens.AlbumState(
                                            id = "offline_album_${playerState.album}",
                                            playlistId = "offline_album_${playerState.album}",
                                            title = playerState.album,
                                            artist = playerState.artist,
                                            thumbnail = playerState.artUrl?.toString()
                                        )
                                    )
                                    handleDismiss()
                                } else {
                                    // Online search fallback
                                    scope.launch {
                                        Toast.makeText(context, context.getString(R.string.toast_searching_album), Toast.LENGTH_SHORT).show()
                                        withContext(Dispatchers.IO) {
                                            val query = "${playerState.album ?: playerState.title} ${playerState.artist}"
                                            val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
                                            val albumItem = searchResult?.items?.filterIsInstance<com.echo.innertube.models.AlbumItem>()?.firstOrNull {
                                                it.title.equals(playerState.album, ignoreCase = true)
                                            } ?: searchResult?.items?.filterIsInstance<com.echo.innertube.models.AlbumItem>()?.firstOrNull()

                                            if (albumItem != null) {
                                                withContext(Dispatchers.Main) {
                                                    onAlbumSelected(
                                                        com.mrtdk.liquid_glass.ui.screens.AlbumState(
                                                            id = albumItem.browseId,
                                                            playlistId = albumItem.playlistId,
                                                            title = albumItem.title,
                                                            artist = albumItem.artists?.joinToString { it.name } ?: playerState.artist,
                                                            thumbnail = albumItem.thumbnail
                                                        )
                                                    )
                                                    handleDismiss()
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    if (!playerState.album.isNullOrBlank()) {
                                                        // Last fallback: try to open offline
                                                        onAlbumSelected(
                                                            com.mrtdk.liquid_glass.ui.screens.AlbumState(
                                                                id = "offline_album_${playerState.album}",
                                                                playlistId = "offline_album_${playerState.album}",
                                                                title = playerState.album,
                                                                artist = playerState.artist,
                                                                thumbnail = playerState.artUrl?.toString()
                                                            )
                                                        )
                                                        handleDismiss()
                                                    } else {
                                                        Toast.makeText(context, context.getString(R.string.toast_album_info_unavailable), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_album_info_unavailable), Toast.LENGTH_SHORT).show()
                            handleDismiss()
                        }
                    }

                    // Ver créditos
                    VerticalMenuActionItem(
                        icon = Icons.Default.Info,
                        label = stringResource(R.string.player_menu_view_credits)
                    ) {
                        if (playerState != null) {
                            Toast.makeText(context, context.getString(R.string.toast_credits_perf_by, playerState.artist), Toast.LENGTH_SHORT).show()
                        }
                        handleDismiss()
                    }

                    // Compartir letra
                    VerticalMenuActionItem(
                        icon = Icons.Default.ChatBubble,
                        label = stringResource(R.string.player_menu_share_lyrics)
                    ) {
                        Toast.makeText(context, context.getString(R.string.toast_lyrics_shared), Toast.LENGTH_SHORT).show()
                        handleDismiss()
                    }

                    // Sugerir menos
                    VerticalMenuActionItem(
                        icon = Icons.Default.ThumbDown,
                        label = stringResource(R.string.player_menu_suggest_less)
                    ) {
                        Toast.makeText(context, context.getString(R.string.toast_suggestion_saved), Toast.LENGTH_SHORT).show()
                        handleDismiss()
                    }

                    // Eliminar de...
                    VerticalMenuActionItem(
                        icon = Icons.Default.Delete,
                        label = stringResource(R.string.player_menu_delete_library),
                        iconTint = Color(0xFFFA243C),
                        textColor = Color(0xFFFA243C)
                    ) {
                        if (playerState?.videoId != null) {
                            if (isSaved) {
                                LibraryManager.removeItem(playerState.videoId)
                                Toast.makeText(context, context.getString(R.string.toast_removed_favorites), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.toast_not_in_library), Toast.LENGTH_SHORT).show()
                            }
                        }
                        handleDismiss()
                    }
                }
            }
        }
    }
}

@Composable
fun GlassBoxScope.LyricsOptionsMenu(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    onDismiss: () -> Unit,
    playerState: PlayerState?,
    selectedProvider: String,
    onSelectProvider: (String) -> Unit,
    isRomajiEnabled: Boolean,
    onToggleRomaji: () -> Unit,
    lyricsOffset: Int,
    onAdjustOffset: () -> Unit,
    onEditLyrics: () -> Unit,
    onReloadLyrics: () -> Unit,
    onSearchManually: () -> Unit,
    onSearchOnline: () -> Unit,
    pivotBounds: androidx.compose.ui.geometry.Rect? = null
) {
    var visible by remember { mutableStateOf(false) }
    var showProviderSelection by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "menuAlpha"
    )
    val cornerRadius by animateFloatAsState(
        targetValue = if (visible) 24f else 80f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuCornerRadius"
    )
    val blurPx by animateFloatAsState(
        targetValue = if (visible) 0f else 15f,
        animationSpec = tween(durationMillis = 180),
        label = "menuContentBlur"
    )

    val context = LocalContext.current

    fun handleDismiss() {
        visible = false
        onDismiss()
    }

    BackHandler(enabled = visible) {
        if (showProviderSelection) {
            showProviderSelection = false
        } else {
            handleDismiss()
        }
    }

    val dominantColor by LibraryManager.currentDominantColor.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val screenCenterX = constraints.maxWidth.toFloat() / 2f
        val screenCenterY = constraints.maxHeight.toFloat() / 2f
        val progress = ((scale - 0.4f) / 0.6f).coerceIn(0f, 1f)

        this@LyricsOptionsMenu.GlassBox(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    if (pivotBounds != null) {
                        translationX = (1f - progress) * (pivotBounds.center.x - screenCenterX)
                        translationY = (1f - progress) * (pivotBounds.center.y - screenCenterY)
                    }
                }
                .width(280.dp)
                .wrapContentHeight(),
            blur = 0.8f,
            scale = 0.02f,
            centerDistortion = 0.1f,
            warpEdges = 0.4f,
            elevation = 4.dp,
            shape = RoundedCornerShape(cornerRadius.dp),
            tint = dominantColor.copy(alpha = 0.25f),
            darkness = 0.2f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (blurPx > 0.1f) it.blur(blurPx.dp) else it }
                    .padding(vertical = 12.dp)
            ) {
                if (showProviderSelection) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showProviderSelection = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Proveedor de Letras",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    val providers = listOf(
                        "LRCLIB",
                        "KuGou",
                        "BetterLyrics",
                        "LyricsPlus",
                        "SimpMusic",
                        "YouTube Music",
                        "YouTube Subtitle"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        providers.forEach { provider ->
                            val isSelected = provider == selectedProvider
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectProvider(provider)
                                        showProviderSelection = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSelected) "$provider (Activo)" else provider,
                                    color = if (isSelected) Color(0xFFFA243C) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = Color(0xFFFA243C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onEditLyrics()
                                    handleDismiss()
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Editar", color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onReloadLyrics()
                                    handleDismiss()
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recargar", tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Recargar", color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSearchManually()
                                    handleDismiss()
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Buscar", color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        VerticalMenuActionItem(
                            icon = Icons.Default.QueueMusic,
                            label = "Proveedor: $selectedProvider",
                            onClick = {
                                showProviderSelection = true
                            }
                        )

                        VerticalMenuActionItem(
                            icon = Icons.Default.History,
                            label = "Ajustar desfase",
                            trailingContent = {
                                Text(
                                    text = "${if (lyricsOffset >= 0) "+" else ""}${lyricsOffset}ms",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                onAdjustOffset()
                                handleDismiss()
                            }
                        )

                        VerticalMenuActionItem(
                            icon = Icons.Default.Translate,
                            label = "Romanizar pista actual",
                            trailingContent = {
                                androidx.compose.material3.Switch(
                                    checked = isRomajiEnabled,
                                    onCheckedChange = {
                                        onToggleRomaji()
                                    },
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFA243C),
                                        checkedTrackColor = Color(0xFFFA243C).copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.graphicsLayer { scaleX = 0.8f; scaleY = 0.8f }
                                )
                            },
                            onClick = {
                                onToggleRomaji()
                            }
                        )

                        VerticalMenuActionItem(
                            icon = Icons.Default.Language,
                            label = "Buscar en Internet",
                            onClick = {
                                onSearchOnline()
                                handleDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}