package com.mrtdk.liquid_glass.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrtdk.glass.GlassContainer
import com.mrtdk.glass.GlassBox
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.data.Playlist
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsListScreen(
    onBack: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val playlists by LibraryManager.playlists.collectAsState()
    
    var showCreateOptions by remember { mutableStateOf(false) }
    var showCreateModal by remember { mutableStateOf(false) }
    var contextMenuPlaylist by remember { mutableStateOf<Playlist?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 80.dp
            )
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color(0xFFFA243C))
                    }
                    
                    // Add, Sort, More pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1C1C1E))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                IconButton(onClick = { showCreateOptions = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                                }
                                DropdownMenu(
                                    expanded = showCreateOptions,
                                    onDismissRequest = { showCreateOptions = false },
                                    modifier = Modifier.background(Color(0xFF2C2C2E))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Create New Playlist", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                                        onClick = { 
                                            showCreateOptions = false
                                            showCreateModal = true 
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Create New Folder", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color.White) },
                                        onClick = { showCreateOptions = false }
                                    )
                                }
                            }
                            IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort", tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Playlists",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 16.dp)
                )
            }

            // Fake "Favorite Songs" item at top
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF91B8CA).copy(alpha = 0.2f)), // A light grayish/blueish tint
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFA243C), modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Favorite Songs",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
                androidx.compose.material3.Divider(modifier = Modifier.padding(start = 104.dp), color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
            }

            items(playlists) { pl ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(pl) {
                            detectTapGestures(
                                onTap = { onPlaylistSelected(pl) },
                                onLongPress = { contextMenuPlaylist = pl }
                            )
                        }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1C1C1E)),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageUrl = pl.coverUrl ?: (if (pl.items.isNotEmpty()) pl.items.first().thumbnail else null)
                        if (imageUrl != null) {
                            AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pl.name,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (pl.items.isNotEmpty()) "${pl.items.size} canciones" else "Empty", // Simulated curator "Andrew Lambrou" or count
                            color = Color.Gray,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
                androidx.compose.material3.Divider(modifier = Modifier.padding(start = 104.dp), color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
        
        com.mrtdk.liquid_glass.ui.components.PlaylistContextMenuOverlay(playlist = contextMenuPlaylist, onDismiss = { contextMenuPlaylist = null })
    }

    if (showCreateModal) {
        CreatePlaylistModal(onDismiss = { showCreateModal = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistModal(onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var showInProfile by remember { mutableStateOf(true) }
    
    var showPhotoOptions by remember { mutableStateOf(false) }

    // Image positioning state
    var imageOffsetX by remember { mutableFloatStateOf(0f) }
    var imageOffsetY by remember { mutableFloatStateOf(0f) }
    var imageScale by remember { mutableFloatStateOf(1f) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coverUri = it
            // Reset positioning when a new image is selected
            imageOffsetX = 0f
            imageOffsetY = 0f
            imageScale = 1f
        }
    }

    // Detect keyboard
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = imeBottom > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                Text("New Playlist", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                
                IconButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            LibraryManager.createPlaylist(title, coverUri?.toString())
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(if (title.isNotBlank()) Color(0xFFFA243C) else Color.DarkGray)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Content area adjusts based on keyboard
            if (!isKeyboardOpen) {
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Cover Box with image positioning
            val coverSize = if (isKeyboardOpen) 120.dp else 200.dp
            Box(
                modifier = Modifier
                    .size(coverSize)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C2C2E))
                    .clickable(enabled = coverUri == null) { showPhotoOptions = true },
                contentAlignment = Alignment.Center
            ) {
                if (coverUri != null) {
                    // Image with pan and zoom gestures for positioning
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    imageScale = (imageScale * zoom).coerceIn(1f, 5f)
                                    // Limit pan generously so they can move it around freely
                                    val maxPanX = 500f * imageScale
                                    val maxPanY = 500f * imageScale
                                    imageOffsetX = (imageOffsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                    imageOffsetY = (imageOffsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                                }
                            }
                    ) {
                        AsyncImage(
                            model = coverUri,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = imageScale
                                    scaleY = imageScale
                                    translationX = imageOffsetX
                                    translationY = imageOffsetY
                                }
                        )
                    }
                    
                    // Overlay controls when image is selected
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                    ) {
                        // Change photo button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { showPhotoOptions = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Change", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        
                        // Pan hint
                        if (imageOffsetX == 0f && imageOffsetY == 0f && imageScale == 1f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Pinch & drag to position", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFFA243C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White)
                    }
                }

                DropdownMenu(
                    expanded = showPhotoOptions,
                    onDismissRequest = { showPhotoOptions = false },
                    modifier = Modifier.background(Color(0xFF2C2C2E))
                ) {
                    DropdownMenuItem(
                        text = { Text("Take Photo", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White) },
                        onClick = { showPhotoOptions = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Choose Photo", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White) },
                        onClick = {
                            showPhotoOptions = false
                            photoPickerLauncher.launch("image/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Choose File", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White) },
                        onClick = { showPhotoOptions = false }
                    )
                }
            }

            if (!isKeyboardOpen) {
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Title input
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Playlist Title", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color(0xFFFA243C),
                    unfocusedIndicatorColor = Color.DarkGray
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            if (!isKeyboardOpen) {
                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.material3.Divider(color = Color.DarkGray, modifier = Modifier.fillMaxWidth(0.9f))

                Row(
                    modifier = Modifier.fillMaxWidth(0.9f).padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show on My Profile and in Search", color = Color.White, fontSize = 14.sp)
                    Switch(
                        checked = showInProfile,
                        onCheckedChange = { showInProfile = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    onBack: () -> Unit,
    onSongSelected: (com.mrtdk.liquid_glass.ui.screens.PlayerState) -> Unit,
    onArtistSelected: (com.mrtdk.liquid_glass.ui.screens.ArtistState) -> Unit = {}
) {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color(0xFF2B2B2B)) }
    var contentColor by remember { mutableStateOf(Color.White) }
    
    val coverUrl = playlist.coverUrl ?: (if (playlist.items.isNotEmpty()) playlist.items.first().thumbnail else null)
    
    // Exact same color extraction logic
    LaunchedEffect(coverUrl) {
        if (coverUrl != null) {
            val hdUrl = if (coverUrl is String) {
                when {
                    coverUrl.contains("=w") -> coverUrl.substringBefore("=w") + "=w500-h500-rj"
                    coverUrl.contains("ytimg.com/vi/") -> coverUrl.replace("hqdefault", "maxresdefault")
                    else -> coverUrl
                }
            } else coverUrl
            
            val request = ImageRequest.Builder(context).data(hdUrl).allowHardware(false).size(100).build()
            val result = coil.Coil.imageLoader(context).execute(request)
            if (result is coil.request.SuccessResult) {
                val drawable = result.drawable
                val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    ?: android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).also {
                        val canvas = android.graphics.Canvas(it)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                    }
                try {
                    val sampledColor = Color(bitmap.getPixel(bitmap.width / 2, bitmap.height - 1))
                    dominantColor = sampledColor
                    contentColor = if (sampledColor.luminance() > 0.5f) Color.Black else Color.White
                } catch (e: Exception) {}
            }
        }
    }

    // Use the playlist cover URL for all songs in this playlist
    val playlistCoverForPlayer = coverUrl

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(dominantColor)
    ) {
        // Hero section
        item {
            GlassContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                content = {
                    if (coverUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(coverUrl).crossfade(true).build(),
                            contentDescription = playlist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1E)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.7f to Color.Transparent,
                                    0.85f to dominantColor.copy(alpha = 0.25f),
                                    0.95f to dominantColor.copy(alpha = 0.7f),
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
                        // Back `<`
                        scope.GlassBox(
                            modifier = Modifier.size(48.dp).clip(CircleShape).clickable { onBack() },
                            shape = CircleShape,
                            tint = dominantColor.copy(alpha = 0.35f),
                            blur = 0.8f, centerDistortion = 0.2f, scale = 0.02f, warpEdges = 0.6f, elevation = 8.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White)
                            }
                        }
                        
                        // Action buttons (people, download, more)
                        scope.GlassBox(
                            modifier = Modifier.height(48.dp).width(136.dp).clip(RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            tint = dominantColor.copy(alpha = 0.35f),
                            blur = 0.8f, centerDistortion = 0.2f, scale = 0.02f, warpEdges = 0.6f, elevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.People, "Collaborate", tint = Color.White, modifier = Modifier.size(24.dp).clickable {})
                                Icon(Icons.Default.ArrowDownward, "Download", tint = Color.White, modifier = Modifier.size(24.dp).clickable {})
                                Icon(Icons.Default.MoreHoriz, "More", tint = Color.White, modifier = Modifier.size(24.dp).clickable {})
                            }
                        }
                    }
                }
            )
        }

        // Title info
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playlist.name,
                    color = contentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "My Playlist", color = contentColor.copy(alpha = 0.8f), fontSize = 14.sp)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                }
            }
        }

        // Play and Shuffle buttons (Image 5 style)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(contentColor.copy(alpha = 0.15f))
                        .clickable {
                            if (playlist.items.isNotEmpty()) {
                                val first = playlist.items.first()
                                val remainingQueue = playlist.items.drop(1).filter { it.type == com.mrtdk.liquid_glass.data.ItemType.SONG }.map { t ->
                                    com.mrtdk.liquid_glass.ui.screens.QueueItem(
                                        title = t.title,
                                        artist = t.subtitle,
                                        artUrl = playlistCoverForPlayer ?: t.thumbnail,
                                        videoId = t.id
                                    )
                                }
                                onSongSelected(com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                    first.title, first.subtitle, 
                                    playlistCoverForPlayer ?: first.thumbnail, 
                                    first.id,
                                    queue = remainingQueue,
                                    isExclusiveQueue = true
                                ))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = contentColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play", color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(contentColor.copy(alpha = 0.15f))
                        .clickable { /* Shuffle */ },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = contentColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shuffle", color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Songs list - use playlist cover as the player art
        itemsIndexed(playlist.items) { trackIndex, track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (track.type == com.mrtdk.liquid_glass.data.ItemType.ARTIST) {
                            onArtistSelected(com.mrtdk.liquid_glass.ui.screens.ArtistState(
                                id = track.id,
                                name = track.title, // Title is the artist name in LibraryItem
                                thumbnail = track.thumbnail
                            ))
                        } else {
                            // Build queue from remaining songs after this one
                            val remainingQueue = playlist.items.drop(trackIndex + 1).filter { it.type == com.mrtdk.liquid_glass.data.ItemType.SONG }.map { t ->
                                com.mrtdk.liquid_glass.ui.screens.QueueItem(
                                    title = t.title,
                                    artist = t.subtitle,
                                    artUrl = playlistCoverForPlayer ?: t.thumbnail,
                                    videoId = t.id
                                )
                            }
                            onSongSelected(com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                track.title, track.subtitle, 
                                playlistCoverForPlayer ?: track.thumbnail, 
                                track.id,
                                queue = remainingQueue,
                                isExclusiveQueue = true
                            ))
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = playlistCoverForPlayer ?: track.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title, 
                        color = contentColor, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.subtitle, 
                        color = contentColor.copy(alpha=0.6f), 
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.MoreHoriz, "More", tint = contentColor.copy(alpha=0.6f))
            }
            androidx.compose.material3.Divider(modifier = Modifier.padding(start = 84.dp), color = contentColor.copy(alpha = 0.1f), thickness = 0.5.dp)
        }
        
        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}
