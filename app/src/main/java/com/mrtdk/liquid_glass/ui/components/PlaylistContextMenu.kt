package com.mrtdk.liquid_glass.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.data.Playlist
import com.mrtdk.liquid_glass.playback.PlaybackQueue
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import com.mrtdk.liquid_glass.ui.screens.QueueItem
import com.mrtdk.liquid_glass.ui.screens.downloadSong
import android.net.Uri

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistContextMenuOverlay(
    playlist: Playlist?,
    onDismiss: () -> Unit,
    onSongSelected: ((PlayerState) -> Unit)? = null
) {
    if (playlist == null) return

    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf(playlist.name) }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(playlist.coverUrl?.let { Uri.parse(it) }) }
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    val playlists by LibraryManager.playlists.collectAsState()

    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Dim/dismiss background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onDismiss() })
                    }
            )

            Column(
                modifier = Modifier
                    .width(320.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Peek Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    val coverUrl = playlist.coverUrl ?: (if (playlist.items.isNotEmpty()) playlist.items.first().thumbnail else null)
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(64.dp)
                        )
                    }

                    // Simple gradient and title over it
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.6f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.75f)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            playlist.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.my_playlist_subtitle),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Menu Options container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2C2C2E).copy(alpha = 0.95f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top row buttons: Descargar, Favorito, Compartir
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 2.1 Descargar Action
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val songs = playlist.items.filter { it.type == ItemType.SONG }
                                        if (songs.isNotEmpty()) {
                                            songs.forEach { song ->
                                                downloadSong(context, song.id, song.title, song.subtitle, song.thumbnail, playlist.name)
                                            }
                                            Toast.makeText(context, "Descargando ${songs.size} canciones...", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No hay canciones para descargar", Toast.LENGTH_SHORT).show()
                                        }
                                        onDismiss()
                                    }
                            ) {
                                Icon(Icons.Default.ArrowDownward, null, tint = Color.White)
                                Spacer(Modifier.height(4.dp))
                                Text("Descargar", color = Color.White, fontSize = 10.sp)
                            }

                            // 2.2 Favorito / Pin Action (Toggles pin)
                            val isPinned = playlist.isPinned
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        LibraryManager.togglePinPlaylist(playlist.id)
                                        onDismiss()
                                    }
                            ) {
                                Icon(
                                    if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                                    null,
                                    tint = if (isPinned) Color(0xFFFA243C) else Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Favorito", color = Color.White, fontSize = 10.sp)
                            }

                            // 2.3 Compartir Action
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, playlist.name)
                                            putExtra(Intent.EXTRA_TEXT, "Escucha mi playlist '${playlist.name}' en RayMusic!")
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Compartir Playlist"))
                                        onDismiss()
                                    }
                            ) {
                                Icon(Icons.Default.IosShare, null, tint = Color.White)
                                Spacer(Modifier.height(4.dp))
                                Text("Compartir", color = Color.White, fontSize = 10.sp)
                            }
                        }

                        Divider(color = Color.DarkGray.copy(alpha = 0.5f))

                        // 2.4 Reproducir Option
                        MenuRow(Icons.Default.PlayArrow, "Reproducir") {
                            if (playlist.items.isNotEmpty()) {
                                val first = playlist.items.first()
                                val remainingQueue = playlist.items.drop(1).filter { it.type == ItemType.SONG }.map { t ->
                                    QueueItem(
                                        title = t.title,
                                        artist = t.subtitle,
                                        artUrl = playlist.coverUrl ?: t.thumbnail,
                                        videoId = t.id
                                    )
                                }
                                onSongSelected?.invoke(
                                    PlayerState(
                                        first.title, first.subtitle,
                                        playlist.coverUrl ?: first.thumbnail,
                                        first.id,
                                        queue = remainingQueue,
                                        isExclusiveQueue = true
                                    )
                                )
                            }
                            onDismiss()
                        }

                        // 2.5 Aleatorio Option
                        MenuRow(Icons.Default.Shuffle, "Aleatorio") {
                            val songs = playlist.items.filter { it.type == ItemType.SONG }.shuffled()
                            if (songs.isNotEmpty()) {
                                val first = songs.first()
                                val remainingQueue = songs.drop(1).map { t ->
                                    QueueItem(
                                        title = t.title,
                                        artist = t.subtitle,
                                        artUrl = playlist.coverUrl ?: t.thumbnail,
                                        videoId = t.id
                                    )
                                }
                                onSongSelected?.invoke(
                                    PlayerState(
                                        first.title, first.subtitle,
                                        playlist.coverUrl ?: first.thumbnail,
                                        first.id,
                                        queue = remainingQueue,
                                        isExclusiveQueue = true
                                    )
                                )
                            }
                            onDismiss()
                        }

                        Divider(color = Color.DarkGray.copy(alpha = 0.5f))

                        // 2.6 Fijar / Quitar Fijado Option
                        MenuRow(
                            Icons.Default.PushPin,
                            if (playlist.isPinned) "Quitar Fijado Playlist" else "Fijar Playlist"
                        ) {
                            LibraryManager.togglePinPlaylist(playlist.id)
                            onDismiss()
                        }

                        // 2.7 Añadir a una Playlist Option
                        MenuRow(Icons.Default.PlaylistAdd, "Añadir a una Playlist") {
                            showAddToPlaylistDialog = true
                        }

                        Divider(color = Color.DarkGray.copy(alpha = 0.5f))

                        // 2.8 Editar Option
                        MenuRow(Icons.Default.Edit, "Editar") {
                            showEditDialog = true
                        }

                        // 2.9 Gestionar Colaboración Option
                        MenuRow(Icons.Default.People, "Gestionar Colaboración") {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Enlace de Colaboración", "https://raymusic.mrtdk.com/collab/${playlist.id}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Enlace de colaboración copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }

                        // 2.10 Mover a Carpeta Option
                        MenuRow(Icons.Default.Folder, "Mover a Carpeta") {
                            Toast.makeText(context, "Mover a carpeta estará disponible en una futura actualización", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }

                        // 2.11 Reproducir Siguiente Option
                        MenuRow(Icons.Default.QueueMusic, "Reproducir Siguiente") {
                            val newItems = playlist.items.filter { it.type == ItemType.SONG }.map { t ->
                                QueueItem(
                                    title = t.title,
                                    artist = t.subtitle,
                                    artUrl = playlist.coverUrl ?: t.thumbnail,
                                    videoId = t.id
                                )
                            }
                            PlaybackQueue.queue = newItems + PlaybackQueue.queue
                            PlaybackQueue.onQueueChanged?.invoke()
                            Toast.makeText(context, "Playlist se reproducirá a continuación", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                }
            }
        }
    }

    // Edit Name & Image Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Playlist", color = Color.White) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Clickable image preview
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
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
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
                        // Camera overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Cambiar Imagen",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = { editNameText = it },
                        label = { Text("Nombre") },
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
                        if (editNameText.isNotBlank()) {
                            val finalCoverUrl = if (selectedImageUri != null && selectedImageUri?.scheme == "content") {
                                LibraryManager.savePlaylistCover(context, playlist.id, selectedImageUri!!)
                            } else {
                                selectedImageUri?.toString()
                            }
                            LibraryManager.updatePlaylist(playlist.id, editNameText, finalCoverUrl)
                        }
                        showEditDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Guardar", color = Color(0xFFFA243C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2C2C2C)
        )
    }

    // Add all songs of this playlist to another playlist
    if (showAddToPlaylistDialog) {
        val targetPlaylists = playlists.filter { it.id != playlist.id }
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = { Text("Añadir a otra Playlist", color = Color.White) },
            text = {
                if (targetPlaylists.isEmpty()) {
                    Text("No tienes otras playlists", color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                        items(targetPlaylists) { targetPl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playlist.items.forEach { song ->
                                            LibraryManager.addSongToPlaylist(targetPl.id, song)
                                        }
                                        Toast.makeText(context, "Añadidas canciones a ${targetPl.name}", Toast.LENGTH_SHORT).show()
                                        showAddToPlaylistDialog = false
                                        onDismiss()
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
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2C2C2C)
        )
    }
}

@Composable
fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = Color.White, fontSize = 16.sp)
    }
}
