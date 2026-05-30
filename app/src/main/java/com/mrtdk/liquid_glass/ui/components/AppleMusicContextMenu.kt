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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler

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
    onSaveAlbumToLibrary: () -> Unit
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
                                    withContext(Dispatchers.IO) {
                                        val isAlbum = album.id.startsWith("MPREb") || album.id.startsWith("FEmusic")
                                        val tracks = if (isAlbum) {
                                            com.echo.innertube.YouTube.album(album.id).getOrNull()?.songs
                                        } else {
                                            val pId = album.playlistId.ifEmpty { album.id }.removePrefix("VL")
                                            com.echo.innertube.YouTube.playlist(pId).getOrNull()?.songs
                                        }
                                        
                                        withContext(Dispatchers.Main) {
                                            if (tracks.isNullOrEmpty()) {
                                                Toast.makeText(context, "No se pudieron obtener las pistas del álbum", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Iniciando descarga de ${tracks.size} pistas...", Toast.LENGTH_SHORT).show()
                                                tracks.forEach { track ->
                                                    downloadSong(
                                                        context = context,
                                                        videoId = track.id,
                                                        title = track.title,
                                                        artist = track.artists.joinToString { it.name },
                                                        artUrl = track.thumbnail,
                                                        album = album.title
                                                    )
                                                }
                                            }
                                        }
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
            Icon(imageVector = icon, contentDescription = label, tint = Color.White)
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
private fun VerticalMenuActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
