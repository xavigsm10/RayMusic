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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
                                    Toast.makeText(context, context.getString(R.string.menu_removed_from_favorites), Toast.LENGTH_SHORT).show()
                                } else {
                                    LibraryManager.saveItem(libraryItem)
                                    Toast.makeText(context, context.getString(R.string.menu_added_to_favorites), Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = stringResource(R.string.dialog_favorite),
                                    tint = if (isSaved) Color(0xFFFA243C) else Color.White
                                )
                            }

                            // Close button
                            IconButton(onClick = { handleDismiss() }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_action), tint = Color.Gray)
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
                                label = stringResource(R.string.menu_play_next),
                                onClick = {
                                    val current = PlaybackQueue.currentSong
                                    val qItem = QueueItem(song.title, song.artist, song.thumbnail, song.id, song.album)
                                    if (current == null) {
                                        onSongSelected(PlayerState(song.title, song.artist, song.thumbnail, song.id, album = song.album))
                                    } else {
                                        PlaybackQueue.queue = listOf(qItem) + PlaybackQueue.queue
                                        PlaybackQueue.onQueueChanged?.invoke()
                                        Toast.makeText(context, context.getString(R.string.menu_play_next_toast), Toast.LENGTH_SHORT).show()
                                    }
                                    handleDismiss()
                                }
                            )

                            HorizontalActionButton(
                                icon = Icons.Default.PlaylistAdd,
                                label = stringResource(R.string.playlists),
                                onClick = { isPlaylistsScreen = true }
                            )

                            HorizontalActionButton(
                                icon = Icons.Default.Share,
                                label = stringResource(R.string.compartir),
                                onClick = {
                                    val shareUrl = "https://music.youtube.com/watch?v=${song.id}"
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, song.title)
                                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.menu_share_song)))
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
                                label = stringResource(R.string.menu_iniciar_radio),
                                onClick = {
                                    onSongSelected(
                                        PlayerState(
                                            title = song.title,
                                            artist = song.artist,
                                            artUrl = song.thumbnail,
                                            videoId = song.id,
                                            queue = emptyList(),
                                            isExclusiveQueue = false,
                                            album = song.album
                                        )
                                    )
                                    handleDismiss()
                                }
                            )

                            VerticalMenuActionItem(
                                icon = Icons.Default.Queue,
                                label = stringResource(R.string.menu_agregar_a_fila),
                                onClick = {
                                    val current = PlaybackQueue.currentSong
                                    val qItem = QueueItem(song.title, song.artist, song.thumbnail, song.id, song.album)
                                    if (current == null) {
                                        onSongSelected(PlayerState(song.title, song.artist, song.thumbnail, song.id, album = song.album))
                                    } else {
                                        PlaybackQueue.queue = PlaybackQueue.queue + listOf(qItem)
                                        PlaybackQueue.onQueueChanged?.invoke()
                                        Toast.makeText(context, context.getString(R.string.menu_added_to_queue), Toast.LENGTH_SHORT).show()
                                    }
                                    handleDismiss()
                                }
                            )

                            VerticalMenuActionItem(
                                icon = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                label = stringResource(if (isSaved) R.string.menu_eliminar_de_biblioteca else R.string.menu_guardar_en_biblioteca),
                                onClick = {
                                    if (isSaved) {
                                        LibraryManager.removeItem(song.id)
                                        Toast.makeText(context, context.getString(R.string.menu_eliminado_de_biblioteca), Toast.LENGTH_SHORT).show()
                                    } else {
                                        LibraryManager.saveItem(libraryItem)
                                        Toast.makeText(context, context.getString(R.string.menu_anadido_a_biblioteca), Toast.LENGTH_SHORT).show()
                                    }
                                    handleDismiss()
                                }
                            )

                            VerticalMenuActionItem(
                                icon = Icons.Default.ArrowDownward,
                                label = stringResource(R.string.descargar),
                                onClick = {
                                    downloadSong(context, song.id, song.title, song.artist, song.thumbnail, song.album)
                                    handleDismiss()
                                }
                            )

                            if (onGoToAlbum != null && !song.album.isNullOrBlank()) {
                                VerticalMenuActionItem(
                                    icon = Icons.Default.Album,
                                    label = stringResource(R.string.menu_ir_al_album),
                                    onClick = {
                                        onGoToAlbum()
                                        handleDismiss()
                                    }
                                )
                            }

                            if (onGoToArtist != null) {
                                VerticalMenuActionItem(
                                    icon = Icons.Default.Mic,
                                    label = stringResource(R.string.menu_ir_al_artista),
                                    onClick = {
                                        onGoToArtist()
                                        handleDismiss()
                                    }
                                )
                            }

                            VerticalMenuActionItem(
                                icon = Icons.Default.Info,
                                label = stringResource(R.string.menu_ver_creditos),
                                onClick = { showCreditsDialog = true }
                            )

                            VerticalMenuActionItem(
                                icon = Icons.Default.PushPin,
                                label = stringResource(R.string.menu_fijar_accesos_directos),
                                onClick = {
                                    LibraryManager.saveItem(libraryItem)
                                    Toast.makeText(context, context.getString(R.string.menu_fijado_accesos_directos), Toast.LENGTH_SHORT).show()
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
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = stringResource(R.string.lyrics_menu_back), tint = Color(0xFFFA243C))
                            }
                            Text(
                                text = stringResource(R.string.menu_anadir_a_playlist),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { handleDismiss() }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_action), tint = Color.Gray)
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
                                    text = stringResource(R.string.menu_nueva_playlist_btn),
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
                                            Toast.makeText(context, context.getString(R.string.menu_anadido_a_playlist_format, playlist.name), Toast.LENGTH_SHORT).show()
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
                                            text = stringResource(R.string.menu_canciones_count_format, playlist.items.size),
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
                        text = stringResource(R.string.menu_nueva_playlist_title),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text(stringResource(R.string.menu_nombre_playlist_label), color = Color.Gray) },
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
                            Text(stringResource(R.string.dialog_cancel), color = Color.Gray)
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
                                    Toast.makeText(context, context.getString(R.string.menu_playlist_creada_cancion_anadida), Toast.LENGTH_SHORT).show()
                                    showNewPlaylistDialog = false
                                    handleDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFA243C))
                        ) {
                            Text(stringResource(R.string.dialog_create), color = Color.White)
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
                        text = stringResource(R.string.menu_creditos_titulo),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    CreditItem(label = stringResource(R.string.menu_creditos_titulo_label), value = song.title)
                    CreditItem(label = stringResource(R.string.menu_creditos_artista_label), value = song.artist)
                    CreditItem(label = stringResource(R.string.menu_creditos_album_label), value = song.album ?: stringResource(R.string.menu_creditos_desconocido))
                    CreditItem(label = stringResource(R.string.menu_creditos_videoid_label), value = song.id)
                    CreditItem(label = stringResource(R.string.menu_creditos_proveedor_label), value = "YouTube Music / InnerTube")

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showCreditsDialog = false
                            handleDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFA243C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.menu_creditos_entendido), color = Color.White)
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
    tracks: List<com.echo.innertube.models.SongItem>? = null,
    onGoToArtist: (() -> Unit)? = null
) {
    val glassScope = this
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var isPlaylistsScreen by remember { mutableStateOf(false) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }

    val savedItems by LibraryManager.savedItems.collectAsState()
    val isSaved = remember(savedItems, album.id) { savedItems.any { it.id == album.id } }
    var isFavorite by remember(savedItems, album.id) { mutableStateOf(savedItems.any { it.id == album.id }) }

    val libraryAlbumItem = remember(album) {
        LibraryItem(
            id = album.id,
            title = album.title,
            subtitle = album.artist,
            thumbnail = album.thumbnail,
            type = ItemType.ALBUM
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
        if (isPlaylistsScreen) {
            isPlaylistsScreen = false
        } else {
            handleDismiss()
        }
    }

    // Full screen overlay with subtle dimming
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
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
            enter = androidx.compose.animation.scaleIn(
                initialScale = 0.15f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.92f, 0.04f),
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.72f,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(150)),
            exit = androidx.compose.animation.scaleOut(
                targetScale = 0.15f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.92f, 0.04f),
                animationSpec = androidx.compose.animation.core.tween(160)
            ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(140)),
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 58.dp, end = 16.dp)
                .wrapContentSize()
        ) {
            glassScope.GlassBox(
                modifier = Modifier
                    .width(268.dp)
                    .wrapContentHeight()
                    .border(
                        width = 0.8.dp,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                blur = 0.95f,
                centerDistortion = 0.1f,
                scale = 0.02f,
                warpEdges = 0.4f,
                tint = Color(0xFF1A1A1C).copy(alpha = 0.86f),
                shape = RoundedCornerShape(24.dp),
                elevation = 16.dp
            ) {
                if (!isPlaylistsScreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        // ── Top Horizontal 3-Action Grid ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Agregar / Agregado
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isSaved) {
                                            LibraryManager.removeItem(album.id)
                                            Toast.makeText(context, "Eliminado de la biblioteca", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onSaveAlbumToLibrary()
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = if (isSaved) Color(0xFFFA243C) else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isSaved) "Agregado" else "Agregar",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }

                            // 2. Agregar a Favoritos
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        isFavorite = !isFavorite
                                        if (isFavorite) {
                                            LibraryManager.saveItem(libraryAlbumItem)
                                            Toast.makeText(context, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) Color(0xFFFA243C) else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isFavorite) "En Favoritos" else "Favorito",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }

                            // 3. Compartir
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val pId = album.playlistId.ifEmpty { album.id }.removePrefix("VL")
                                        val shareUrl = "https://music.youtube.com/playlist?list=$pId"
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, album.title)
                                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Compartir álbum"))
                                        handleDismiss()
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.IosShare,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
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

                        Divider(color = Color.White.copy(alpha = 0.12f), thickness = 0.6.dp)

                        // ── Vertical Action Options ──
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // 1. Agregar a playlist
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isPlaylistsScreen = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Agregar a playlist",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                            // 2. Poner a continuación
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddAlbumToQueue()
                                        handleDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueuePlayNext,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Poner a continuación",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                            // 3. Poner después
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddAlbumToQueue()
                                        handleDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Queue,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Poner después",
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = album.title,
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                            // 4. Descargar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        handleDismiss()
                                        Toast.makeText(context, "Obteniendo pistas para descargar...", Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    val tracksToDownload = if (!tracks.isNullOrEmpty()) {
                                                        tracks
                                                    } else {
                                                        val isAlbum = album.id.startsWith("MPREb") || album.id.startsWith("FEmusic")
                                                        if (isAlbum) {
                                                            YouTube.album(album.id).getOrNull()?.songs
                                                                ?: run {
                                                                    val pId = album.playlistId.ifEmpty { album.id }.removePrefix("VL")
                                                                    YouTube.playlist(pId).getOrNull()?.songs
                                                                }
                                                        } else {
                                                            val pId = album.playlistId.ifEmpty { album.id }.removePrefix("VL")
                                                            YouTube.playlist(pId).getOrNull()?.songs
                                                                ?: run {
                                                                    YouTube.album(album.id).getOrNull()?.songs
                                                                }
                                                        }
                                                    }

                                                    withContext(Dispatchers.Main) {
                                                        if (tracksToDownload.isNullOrEmpty()) {
                                                            Toast.makeText(context, "No se encontraron pistas para descargar", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Iniciando descarga de ${tracksToDownload.size} canciones...", Toast.LENGTH_SHORT).show()
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
                                                Toast.makeText(context, "Error al descargar: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Descargar",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }

                            if (onGoToArtist != null) {
                                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onGoToArtist()
                                            handleDismiss()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "Ver artista",
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = album.artist,
                                            color = Color.White.copy(alpha = 0.55f),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                            // 5. Sugerir menos
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Toast.makeText(context, "Se sugerirá menos contenido similar", Toast.LENGTH_SHORT).show()
                                        handleDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbDownOffAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Sugerir menos",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                } else {
                    // Playlists Selector Sub-screen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isPlaylistsScreen = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBackIosNew,
                                    contentDescription = "Volver",
                                    tint = Color(0xFFFA243C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Agregar a playlist",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))

                        val playlists by LibraryManager.playlists.collectAsState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Crear nueva playlist
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showNewPlaylistDialog = true }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFFFA243C),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Nueva playlist...",
                                    color = Color(0xFFFA243C),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f))

                            playlists.forEach { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                val tracksToAdd = if (!tracks.isNullOrEmpty()) {
                                                    tracks
                                                } else {
                                                    val isAlbum = album.id.startsWith("MPREb") || album.id.startsWith("FEmusic")
                                                    withContext(Dispatchers.IO) {
                                                        if (isAlbum) YouTube.album(album.id).getOrNull()?.songs
                                                        else YouTube.playlist(album.playlistId.ifEmpty { album.id }.removePrefix("VL")).getOrNull()?.songs
                                                    }
                                                }
                                                if (!tracksToAdd.isNullOrEmpty()) {
                                                    tracksToAdd.forEach { track ->
                                                        val trackLibItem = LibraryItem(
                                                            id = track.id,
                                                            title = track.title,
                                                            subtitle = track.artists.joinToString { it.name },
                                                            thumbnail = track.thumbnail,
                                                            type = ItemType.SONG,
                                                            album = album.title
                                                        )
                                                        LibraryManager.addSongToPlaylist(playlist.id, trackLibItem)
                                                    }
                                                    Toast.makeText(context, "Se agregaron las canciones a ${playlist.name}", Toast.LENGTH_SHORT).show()
                                                }
                                                handleDismiss()
                                            }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = playlist.name,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${playlist.items.size} canciones",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Divider(color = Color.White.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewPlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        androidx.compose.ui.window.Dialog(onDismissRequest = { showNewPlaylistDialog = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF252528))
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
                        label = { Text("Nombre de la playlist") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFA243C),
                            focusedLabelColor = Color(0xFFFA243C)
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showNewPlaylistDialog = false }) {
                            Text("Cancelar", color = Color.Gray)
                        }
                        TextButton(onClick = {
                            if (playlistName.isNotBlank()) {
                                LibraryManager.createPlaylist(playlistName)
                                scope.launch {
                                    val newPlaylist = LibraryManager.playlists.value.firstOrNull { it.name == playlistName }
                                    val tracksToAdd = if (!tracks.isNullOrEmpty()) {
                                        tracks
                                    } else {
                                        val isAlbum = album.id.startsWith("MPREb") || album.id.startsWith("FEmusic")
                                        withContext(Dispatchers.IO) {
                                            if (isAlbum) YouTube.album(album.id).getOrNull()?.songs
                                            else YouTube.playlist(album.playlistId.ifEmpty { album.id }.removePrefix("VL")).getOrNull()?.songs
                                        }
                                    }
                                    if (!tracksToAdd.isNullOrEmpty() && newPlaylist != null) {
                                        tracksToAdd.forEach { track ->
                                            val trackLibItem = LibraryItem(
                                                id = track.id,
                                                title = track.title,
                                                subtitle = track.artists.joinToString { it.name },
                                                thumbnail = track.thumbnail,
                                                type = ItemType.SONG,
                                                album = album.title
                                            )
                                            LibraryManager.addSongToPlaylist(newPlaylist.id, trackLibItem)
                                        }
                                    }
                                    Toast.makeText(context, "Playlist creada con las canciones del álbum", Toast.LENGTH_SHORT).show()
                                    showNewPlaylistDialog = false
                                    handleDismiss()
                                }
                            }
                        }) {
                            Text("Crear", color = Color(0xFFFA243C), fontWeight = FontWeight.Bold)
                        }
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
                                        Toast.makeText(context, "${context.getString(R.string.anadir_a_playlist)}: ${targetPl.name}", Toast.LENGTH_SHORT).show()
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
                    Text(stringResource(R.string.menu_view_grid), color = Color.White, fontSize = 15.sp)
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
                    Text(stringResource(R.string.menu_view_list), color = Color.White, fontSize = 15.sp)
                }

                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                // Sort Options
                val sortOptions = listOf(
                    "title" to stringResource(R.string.menu_sort_title_label),
                    "date_added" to stringResource(R.string.menu_sort_date_added),
                    "last_played" to stringResource(R.string.menu_sort_last_played),
                    "last_updated" to stringResource(R.string.menu_sort_last_updated),
                    "type" to stringResource(R.string.menu_sort_playlist_type)
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
                    "title" to stringResource(R.string.menu_sort_title_label),
                    "date_added" to stringResource(R.string.menu_sort_date_added),
                    "last_played" to stringResource(R.string.menu_sort_last_played),
                    "last_updated" to stringResource(R.string.menu_sort_last_updated),
                    "type" to stringResource(R.string.menu_sort_playlist_type)
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
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val menuWidth = 280.dp
        val padding = 16.dp
        val estimatedHeight = 470.dp

        val screenWidthDp = maxWidth
        val screenHeightDp = maxHeight

        var targetLeft = (screenWidthDp - menuWidth) / 2
        var targetTop = (screenHeightDp - estimatedHeight) / 2

        this@PlayerOptionsMenu.GlassBox(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = targetLeft, y = targetTop)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .width(menuWidth)
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
                                    queue = emptyList(),
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
    availableProviders: List<com.mrtdk.liquid_glass.utils.LyricsFetchResult> = emptyList(),
    currentProviderIndex: Int = 0,
    onSelectProviderIndex: (Int) -> Unit = {},
    isRomajiEnabled: Boolean,
    onToggleRomaji: () -> Unit,
    lyricsOffset: Int,
    onAdjustOffset: () -> Unit,
    onAdjustOffsetDelta: (Float) -> Unit = {},
    onResetOffset: () -> Unit = {},
    onEditLyrics: () -> Unit,
    onReloadLyrics: () -> Unit,
    onSearchManually: () -> Unit,
    onSearchOnline: () -> Unit,
    isTranslationEnabled: Boolean = true,
    onToggleTranslation: () -> Unit = {},
    isAccompanimentEnabled: Boolean = true,
    onToggleAccompaniment: () -> Unit = {},
    isKaraokeEnabled: Boolean = true,
    onToggleKaraoke: () -> Unit = {},
    isDuetEnabled: Boolean = true,
    onToggleDuet: () -> Unit = {},
    onCopyLyricsAsFormat: (String) -> Unit = {},
    pivotBounds: androidx.compose.ui.geometry.Rect? = null
) {
    var visible by remember { mutableStateOf(false) }
    var showProviderSelection by remember { mutableStateOf(false) }
    var showExportFormatSelection by remember { mutableStateOf(false) }
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
        } else if (showExportFormatSelection) {
            showExportFormatSelection = false
        } else {
            handleDismiss()
        }
    }

    val dominantColor by LibraryManager.currentDominantColor.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val menuWidth = 300.dp
        val padding = 16.dp
        val estimatedHeight = 440.dp

        val screenWidthDp = maxWidth
        val screenHeightDp = maxHeight

        var targetLeft = (screenWidthDp - menuWidth) / 2
        var targetTop = (screenHeightDp - estimatedHeight) / 2

        if (pivotBounds != null) {
            with(density) {
                val pivotLeftDp = pivotBounds.left.toDp()
                val pivotRightDp = pivotBounds.right.toDp()
                val pivotTopDp = pivotBounds.top.toDp()
                val pivotBottomDp = pivotBounds.bottom.toDp()

                targetLeft = (pivotRightDp - menuWidth).coerceIn(padding, screenWidthDp - menuWidth - padding)

                targetTop = pivotBottomDp + 8.dp
                if (targetTop + estimatedHeight > screenHeightDp - padding) {
                    targetTop = pivotTopDp - estimatedHeight - 8.dp
                }
                targetTop = targetTop.coerceIn(padding, screenHeightDp - estimatedHeight - padding)
            }
        }

        this@LyricsOptionsMenu.GlassBox(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = targetLeft, y = targetTop)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    if (pivotBounds != null) {
                        val menuWidthPx = if (size.width > 0f) size.width else with(density) { menuWidth.toPx() }
                        val menuHeightPx = if (size.height > 0f) size.height else with(density) { estimatedHeight.toPx() }

                        val targetLeftPx = with(density) { targetLeft.toPx() }
                        val targetTopPx = with(density) { targetTop.toPx() }

                        val pivotFractionX = ((pivotBounds.center.x - targetLeftPx) / menuWidthPx).coerceIn(0f, 1f)
                        val pivotFractionY = ((pivotBounds.center.y - targetTopPx) / menuHeightPx).coerceIn(0f, 1f)
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(pivotFractionX, pivotFractionY)
                    }
                }
                .width(menuWidth)
                .heightIn(max = screenHeightDp - 60.dp),
            blur = 0.8f,
            scale = 0.02f,
            centerDistortion = 0.1f,
            warpEdges = 0.4f,
            elevation = 6.dp,
            shape = RoundedCornerShape(cornerRadius.dp),
            tint = dominantColor.copy(alpha = 0.28f),
            darkness = 0.22f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .let { if (blurPx > 0.1f) it.blur(blurPx.dp) else it }
                    .padding(vertical = 12.dp)
            ) {
                if (showProviderSelection) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showProviderSelection = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.lyrics_menu_back), tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Distribuidores (${availableProviders.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (availableProviders.isNotEmpty()) {
                            availableProviders.forEachIndexed { index, provider ->
                                val isSelected = index == currentProviderIndex || provider.providerName.equals(selectedProvider, ignoreCase = true)
                                val itemSyncColor = when (provider.syncType.lowercase()) {
                                    "syllable", "richsync" -> Color(0xFFFDE69B)
                                    "word" -> Color(0xFFAAD1FF)
                                    "line", "linesync" -> Color(0xFFC9F8DA)
                                    else -> Color.White.copy(alpha = 0.6f)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable {
                                            onSelectProviderIndex(index)
                                            onSelectProvider(provider.providerName)
                                            showProviderSelection = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SyncTypeBadge(syncType = provider.syncType, color = itemSyncColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = provider.providerName,
                                        color = if (isSelected) itemSyncColor else Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = itemSyncColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            listOf("Better Lyrics", "BiniLyrics", "LRCLib", "Musixmatch", "YouTube").forEach { name ->
                                val isSelected = name.equals(selectedProvider, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable {
                                            onSelectProvider(name)
                                            showProviderSelection = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        color = if (isSelected) Color(0xFFFDE69B) else Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFFFDE69B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (showExportFormatSelection) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showExportFormatSelection = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.lyrics_menu_back), tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.lyrics_menu_export_lyrics),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 6.dp))

                    val formats = listOf("LRC", "ELRC", "TTML")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        formats.forEach { format ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCopyLyricsAsFormat(format)
                                        handleDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = format,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // --- Better Lyrics & Glassy Music Settings Panel (Separated Pill Cards) ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Title
                        Text(
                            text = "Ajustes de Letras",
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )

                        // --- Pill 1: Distribuidor y Tipografía ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        ) {
                            val activeProvName = availableProviders.getOrNull(currentProviderIndex)?.providerName ?: selectedProvider.ifEmpty { "Better Lyrics" }
                            val activeSyncType = availableProviders.getOrNull(currentProviderIndex)?.syncType ?: "syllable"
                            val provSyncColor = when (activeSyncType.lowercase()) {
                                "syllable", "richsync" -> Color(0xFFFDE69B)
                                "word" -> Color(0xFFAAD1FF)
                                "line", "linesync" -> Color(0xFFC9F8DA)
                                else -> Color.White.copy(alpha = 0.6f)
                            }

                            VerticalMenuActionItem(
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                label = "Distribuidor de Letras",
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SyncTypeBadge(syncType = activeSyncType, color = provSyncColor, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = "$activeProvName (${if (availableProviders.isNotEmpty()) "${currentProviderIndex + 1}/${availableProviders.size}" else "1/1"})",
                                            color = provSyncColor,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                },
                                onClick = {
                                    showProviderSelection = true
                                }
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 12.dp))

                            var currentFont by remember { 
                                mutableStateOf(com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_font_family") ?: "Satoshi") 
                            }
                            VerticalMenuActionItem(
                                icon = Icons.Default.FontDownload,
                                label = "Fuente de Letras",
                                trailingContent = {
                                    Text(
                                        text = currentFont,
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    val nextFont = when (currentFont) {
                                        "Satoshi" -> "Inter"
                                        "Inter" -> "Sistema"
                                        else -> "Satoshi"
                                    }
                                    currentFont = nextFont
                                    com.mrtdk.liquid_glass.data.LibraryManager.saveString("lyrics_font_family", nextFont)
                                }
                            )
                        }

                        // --- Pill 2: Animaciones y Motor ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        ) {
                            var isGlowEnabled by remember {
                                mutableStateOf((com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_glow_enabled") ?: "true") == "true")
                            }
                            VerticalMenuActionItem(
                                icon = Icons.Default.AutoAwesome,
                                label = "Resplandor Karaoke (Glow)",
                                trailingContent = {
                                    androidx.compose.material3.Switch(
                                        checked = isGlowEnabled,
                                        onCheckedChange = {
                                            isGlowEnabled = it
                                            com.mrtdk.liquid_glass.data.LibraryManager.saveString("lyrics_glow_enabled", it.toString())
                                        },
                                        colors = androidx.compose.material3.SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFFDE69B),
                                            checkedTrackColor = Color(0xFFFDE69B).copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.graphicsLayer { scaleX = 0.8f; scaleY = 0.8f }
                                    )
                                },
                                onClick = {
                                    isGlowEnabled = !isGlowEnabled
                                    com.mrtdk.liquid_glass.data.LibraryManager.saveString("lyrics_glow_enabled", isGlowEnabled.toString())
                                }
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 12.dp))

                            var scrollMode by remember {
                                mutableStateOf(com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_scroll_mode") ?: "GlassyFlow")
                            }
                            VerticalMenuActionItem(
                                icon = Icons.Default.SwapVert,
                                label = "Desplazamiento",
                                trailingContent = {
                                    Text(
                                        text = scrollMode,
                                        color = Color(0xFFC9F8DA),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    val nextMode = if (scrollMode == "GlassyFlow") "Smooth" else "GlassyFlow"
                                    scrollMode = nextMode
                                    com.mrtdk.liquid_glass.data.LibraryManager.saveString("lyrics_scroll_mode", nextMode)
                                }
                            )
                        }

                        // --- Pill 3: Traducción y Desfase ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        ) {
                            VerticalMenuActionItem(
                                icon = Icons.Default.Translate,
                                label = "Romanización (Romaji / Pinyin)",
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

                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 12.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        Text("Desfase de Tiempo", color = Color.White, fontSize = 14.sp)
                                    }
                                    Text(
                                        text = "${if (lyricsOffset >= 0) "+" else ""}${String.format("%.1f", lyricsOffset / 1000f)}s",
                                        color = Color(0xFFFDE69B),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.10f))
                                            .clickable { onAdjustOffsetDelta(-0.5f) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-0.5s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.10f))
                                            .clickable { onAdjustOffsetDelta(-0.1f) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-0.1s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .clickable { onResetOffset() }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("0.0s", color = Color(0xFFFDE69B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.10f))
                                            .clickable { onAdjustOffsetDelta(0.1f) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+0.1s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.10f))
                                            .clickable { onAdjustOffsetDelta(0.5f) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+0.5s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // --- Pill 4: Acciones Rápidas ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .padding(vertical = 8.dp),
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
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Editar", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onReloadLyrics()
                                        handleDismiss()
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Recargar", tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Recargar", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onSearchManually()
                                        handleDismiss()
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = "Reportar", tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Reportar", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassBoxScope.ArtistOptionsMenu(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    artists: List<String>,
    onDismiss: () -> Unit,
    onArtistSelected: (String) -> Unit,
    pivotBounds: androidx.compose.ui.geometry.Rect? = null
) {
    var visible by remember { mutableStateOf(false) }

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
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val menuWidth = 280.dp
        val padding = 16.dp
        val estimatedHeight = (88 + artists.size * 48).dp

        val screenWidthDp = maxWidth
        val screenHeightDp = maxHeight

        var targetLeft = (screenWidthDp - menuWidth) / 2
        var targetTop = (screenHeightDp - estimatedHeight) / 2

        if (pivotBounds != null) {
            with(density) {
                val pivotLeftDp = pivotBounds.left.toDp()
                val pivotRightDp = pivotBounds.right.toDp()
                val pivotTopDp = pivotBounds.top.toDp()
                val pivotBottomDp = pivotBounds.bottom.toDp()
                val pivotCenterXDp = (pivotLeftDp + pivotRightDp) / 2f

                targetLeft = (pivotCenterXDp - menuWidth / 2).coerceIn(padding, screenWidthDp - menuWidth - padding)

                targetTop = pivotBottomDp + 8.dp
                if (targetTop + estimatedHeight > screenHeightDp - padding) {
                    targetTop = pivotTopDp - estimatedHeight - 8.dp
                }
                targetTop = targetTop.coerceIn(padding, screenHeightDp - estimatedHeight - padding)
            }
        }

        this@ArtistOptionsMenu.GlassBox(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = targetLeft, y = targetTop)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    if (pivotBounds != null) {
                        val menuWidthPx = if (size.width > 0f) size.width else with(density) { menuWidth.toPx() }
                        val menuHeightPx = if (size.height > 0f) size.height else with(density) { estimatedHeight.toPx() }

                        val targetLeftPx = with(density) { targetLeft.toPx() }
                        val targetTopPx = with(density) { targetTop.toPx() }

                        val pivotFractionX = ((pivotBounds.center.x - targetLeftPx) / menuWidthPx).coerceIn(0f, 1f)
                        val pivotFractionY = ((pivotBounds.center.y - targetTopPx) / menuHeightPx).coerceIn(0f, 1f)
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(pivotFractionX, pivotFractionY)
                    }
                }
                .width(menuWidth)
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
                // Header Title
                Text(
                    text = if (artists.size > 1) stringResource(R.string.artist_menu_select_title) else stringResource(R.string.artist_menu_single_title),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    artists.forEach { artist ->
                        val labelText = if (artists.size > 1) stringResource(R.string.artist_menu_view_artist_format, artist) else stringResource(R.string.artist_menu_view_artist)
                        VerticalMenuActionItem(
                            icon = Icons.Default.Person,
                            label = labelText,
                            onClick = {
                                onArtistSelected(artist)
                                handleDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}