package com.mrtdk.liquid_glass.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.res.stringResource
import com.mrtdk.glass.GlassContainer
import com.mrtdk.glass.GlassBox
import com.mrtdk.liquid_glass.ui.components.LiquidButton
import com.mrtdk.liquid_glass.ui.components.LocalBackdrop
import com.mrtdk.liquid_glass.ui.components.SharedElementTransitionContainer
import com.mrtdk.liquid_glass.ui.components.AppleMusicPlaylistMenu
import com.mrtdk.liquid_glass.ui.components.AppleMusicCreateMenu
import com.mrtdk.liquid_glass.ui.components.SharedTransitionState
import com.mrtdk.liquid_glass.ui.components.PlaylistsPageMoreMenu
import com.mrtdk.liquid_glass.ui.components.PlaylistsPageSortMenu
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.data.Playlist
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.ui.components.trackClickBounds
import com.mrtdk.liquid_glass.ui.components.trackTapBounds
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import com.echo.innertube.YouTube
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.ArtistItem
import com.echo.innertube.models.AlbumItem
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsListScreen(
    onBack: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onSongSelected: (com.mrtdk.liquid_glass.ui.screens.PlayerState) -> Unit,
    onFavoriteSongsSelected: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val playlists by LibraryManager.playlists.collectAsState()
    val savedItems by LibraryManager.savedItems.collectAsState()
    val favSongsCount = remember(savedItems) { savedItems.count { it.type == ItemType.SONG } }

    val viewMode = remember { mutableStateOf(LibraryManager.getString("playlist_view_mode", "list") ?: "list") }
    val sortBy = remember { mutableStateOf(LibraryManager.getString("playlist_sort_by", "date_added") ?: "date_added") }

    var showCreateOptions by remember { mutableStateOf(false) }
    var showCreateModal by remember { mutableStateOf(false) }
    var contextMenuPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val localBackdrop = rememberLayerBackdrop()
    val pillBackdrop = rememberLayerBackdrop()

    val sortedPlaylists = remember(playlists, sortBy.value) {
        playlists.sortedWith { a, b ->
            if (a.isPinned && !b.isPinned) -1
            else if (!a.isPinned && b.isPinned) 1
            else {
                when (sortBy.value) {
                    "title" -> a.name.lowercase().compareTo(b.name.lowercase())
                    "date_added" -> b.timestamp.compareTo(a.timestamp)
                    "last_played" -> {
                        val lpA = LibraryManager.getString("playlist_last_played_${a.id}")?.toLongOrNull() ?: 0L
                        val lpB = LibraryManager.getString("playlist_last_played_${b.id}")?.toLongOrNull() ?: 0L
                        lpB.compareTo(lpA)
                    }
                    "last_updated" -> {
                        val luA = LibraryManager.getString("playlist_last_updated_${a.id}")?.toLongOrNull() ?: a.timestamp
                        val luB = LibraryManager.getString("playlist_last_updated_${b.id}")?.toLongOrNull() ?: b.timestamp
                        luB.compareTo(luA)
                    }
                    "type" -> {
                        val typeA = if (a.id.startsWith("VL") || a.id.startsWith("PL")) 1 else 0
                        val typeB = if (b.id.startsWith("VL") || b.id.startsWith("PL")) 1 else 0
                        typeA.compareTo(typeB)
                    }
                    else -> b.timestamp.compareTo(a.timestamp)
                }
            }
        }
    }

    GlassContainer(
        modifier = Modifier.fillMaxSize(),
        useShader = true,
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black).layerBackdrop(pillBackdrop))
                Box(modifier = Modifier.fillMaxSize().layerBackdrop(localBackdrop)) {
                    if (viewMode.value == "grid") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding() + 8.dp,
                            bottom = paddingValues.calculateBottomPadding() + 80.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color(0xFFFA243C))
                                }
                                
                                // Add, Sort, More pill with glass effect
                                Box(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .drawBackdrop(
                                            backdrop = pillBackdrop,
                                            shape = { Capsule() },
                                            effects = {
                                                vibrancy()
                                                blur(2f.dp.toPx())
                                                lens(12f.dp.toPx(), 24f.dp.toPx())
                                            },
                                            onDrawSurface = {
                                                drawRect(Color(0xFF1C1C1E).copy(alpha = 0.35f))
                                            }
                                        )
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box {
                                            IconButton(onClick = { showCreateOptions = true }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        IconButton(onClick = { showSortMenu = true }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = stringResource(R.string.playlists),
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }

                        // Favorite Songs Grid Item
                        item {
                            var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SharedTransitionState.lastClickBounds = imageCoords?.boundsInRoot()
                                        SharedTransitionState.lastOpenedId = "favorite_songs"
                                        onFavoriteSongsSelected()
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF8B0000), Color(0xFFFA243C))
                                            )
                                        )
                                        .onGloballyPositioned { imageCoords = it },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.favorite_songs),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Text(
                                    text = stringResource(R.string.num_canciones, favSongsCount),
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        // Playlists Grid Items
                        items(sortedPlaylists.size) { index ->
                            val pl = sortedPlaylists[index]
                            var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                SharedTransitionState.lastClickBounds = imageCoords?.boundsInRoot()
                                                SharedTransitionState.lastOpenedId = pl.id
                                                LibraryManager.saveString("playlist_last_played_${pl.id}", System.currentTimeMillis().toString())
                                                onPlaylistSelected(pl)
                                            },
                                            onLongPress = { contextMenuPlaylist = pl }
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1C1C1E))
                                        .onGloballyPositioned { imageCoords = it },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val imageUrl = pl.coverUrl ?: (if (pl.items.isNotEmpty()) pl.items.first().thumbnail else null)
                                    if (imageUrl != null) {
                                        AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pl.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Text(
                                    text = if (pl.items.isNotEmpty()) stringResource(R.string.num_canciones, pl.items.size) else stringResource(R.string.num_canciones, 0),
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                } else {
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
                                        .height(48.dp)
                                        .drawBackdrop(
                                            backdrop = pillBackdrop,
                                            shape = { Capsule() },
                                            effects = {
                                                vibrancy()
                                                blur(2f.dp.toPx())
                                                lens(12f.dp.toPx(), 24f.dp.toPx())
                                            },
                                            onDrawSurface = {
                                                drawRect(Color(0xFF1C1C1E).copy(alpha = 0.35f))
                                            }
                                        )
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box {
                                            IconButton(onClick = { showCreateOptions = true }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        IconButton(onClick = { showSortMenu = true }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = stringResource(R.string.playlists),
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 16.dp)
                            )
                        }

                        // "Favorite Songs" item
                        item {
                            var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SharedTransitionState.lastClickBounds = imageCoords?.boundsInRoot()
                                        SharedTransitionState.lastOpenedId = "favorite_songs"
                                        onFavoriteSongsSelected()
                                    }
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { imageCoords = it }
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF8B0000), Color(0xFFFA243C))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.favorite_songs),
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.num_canciones, favSongsCount),
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                            }
                            androidx.compose.material3.Divider(modifier = Modifier.padding(start = 104.dp), color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }

                        items(sortedPlaylists) { pl ->
                            var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                SharedTransitionState.lastClickBounds = imageCoords?.boundsInRoot()
                                                SharedTransitionState.lastOpenedId = pl.id
                                                LibraryManager.saveString("playlist_last_played_${pl.id}", System.currentTimeMillis().toString())
                                                onPlaylistSelected(pl)
                                            },
                                            onLongPress = { contextMenuPlaylist = pl }
                                        )
                                    }
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { imageCoords = it }
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
                                        text = if (pl.items.isNotEmpty()) stringResource(R.string.num_canciones, pl.items.size) else stringResource(R.string.num_canciones, 0),
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
                }

                com.mrtdk.liquid_glass.ui.components.PlaylistContextMenuOverlay(
                    playlist = contextMenuPlaylist,
                    onDismiss = { contextMenuPlaylist = null },
                    onSongSelected = onSongSelected
                )
            }
        }
    },
        glassContent = {
            if (showCreateOptions) {
                AppleMusicCreateMenu(
                    backdrop = localBackdrop,
                    onDismiss = { showCreateOptions = false },
                    onCreatePlaylist = { showCreateModal = true },
                    onCreateFolder = { showCreateOptions = false }
                )
            }
            if (showMoreMenu) {
                PlaylistsPageMoreMenu(
                    backdrop = localBackdrop,
                    onDismiss = { showMoreMenu = false },
                    currentViewMode = viewMode.value,
                    onViewModeSelected = { mode ->
                        viewMode.value = mode
                        LibraryManager.saveString("playlist_view_mode", mode)
                    },
                    currentSort = sortBy.value,
                    onSortSelected = { sort ->
                        sortBy.value = sort
                        LibraryManager.saveString("playlist_sort_by", sort)
                    }
                )
            }
            if (showSortMenu) {
                PlaylistsPageSortMenu(
                    backdrop = localBackdrop,
                    onDismiss = { showSortMenu = false },
                    currentSort = sortBy.value,
                    onSortSelected = { sort ->
                        sortBy.value = sort
                        LibraryManager.saveString("playlist_sort_by", sort)
                    }
                )
            }
            if (showCreateModal) {
                CreatePlaylistModal(onDismiss = { showCreateModal = false })
            }
        }
    )
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
                Text(stringResource(R.string.new_playlist_title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                
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
                                Text(stringResource(R.string.pinch_drag_position), color = Color.White, fontSize = 10.sp)
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
                        text = { Text(stringResource(R.string.take_photo), color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White) },
                        onClick = { showPhotoOptions = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.choose_photo), color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White) },
                        onClick = {
                            showPhotoOptions = false
                            photoPickerLauncher.launch("image/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.choose_file), color = Color.White) },
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
                placeholder = { Text(stringResource(R.string.playlist_title_placeholder), color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
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
                    Text(stringResource(R.string.show_profile_search), color = Color.White, fontSize = 14.sp)
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
    val playlists by LibraryManager.playlists.collectAsState()
    val currentPlaylist = remember(playlists, playlist.id) {
        playlists.find { it.id == playlist.id } ?: playlist
    }
    val isReplay = remember(currentPlaylist.id) { currentPlaylist.id.startsWith("replay_") }
    val defaultDominantColor = if (isReplay) Color(0xFF12351B) else Color(0xFF2B2B2B)
    var showAddMusicOverlay by remember { mutableStateOf(false) }
    var dominantColor by remember(currentPlaylist.id) { mutableStateOf(defaultDominantColor) }
    var contentColor by remember(currentPlaylist.id) { mutableStateOf(Color.White) }
    
    val coverUrl = currentPlaylist.coverUrl ?: (if (currentPlaylist.items.isNotEmpty()) currentPlaylist.items.first().thumbnail else null)
    
    // Exact same color extraction logic
    LaunchedEffect(coverUrl, isReplay) {
        if (isReplay) {
            dominantColor = Color(0xFF12351B)
            contentColor = Color.White
        } else if (coverUrl != null) {
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
                    var r = 0L; var g = 0L; var b = 0L
                    val y = bitmap.height - 1
                    val w = bitmap.width
                    for (x in 0 until w) {
                        val pixel = bitmap.getPixel(x, y)
                        r += android.graphics.Color.red(pixel)
                        g += android.graphics.Color.green(pixel)
                        b += android.graphics.Color.blue(pixel)
                    }
                    val sampledColor = Color((r / w).toInt(), (g / w).toInt(), (b / w).toInt())
                    dominantColor = sampledColor
                    contentColor = if (sampledColor.luminance() > 0.5f) Color.Black else Color.White
                } catch (e: Exception) {}
            }
        }
    }

    // Use the playlist cover URL for all songs in this playlist
    val playlistCoverForPlayer = coverUrl

    val localBackdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        SharedElementTransitionContainer(
            onBack = onBack,
            enableSwipeToDismiss = !isReplay,
            shrinkToTarget = !isReplay,
            slideToSide = isReplay
        ) { progress, dismiss ->
            var showPlaylistMenu by remember { mutableStateOf(false) }
            var currentSort by remember { mutableStateOf("default") }

            val sortedItems = remember(currentPlaylist.items, currentSort) {
                when (currentSort) {
                    "title" -> currentPlaylist.items.sortedWith(compareBy<com.mrtdk.liquid_glass.data.LibraryItem> { it.title.lowercase() }.thenBy { it.subtitle.lowercase() })
                    "artist" -> currentPlaylist.items.sortedWith(compareBy<com.mrtdk.liquid_glass.data.LibraryItem> { it.subtitle.lowercase() }.thenBy { it.title.lowercase() })
                    "album" -> currentPlaylist.items.sortedWith(compareBy<com.mrtdk.liquid_glass.data.LibraryItem> { (it.album ?: "").lowercase() }.thenBy { it.title.lowercase() })
                    else -> currentPlaylist.items
                }
            }

            val popScaleBack by animateFloatAsState(
                targetValue = if (progress > 0.80f) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
                label = "popScaleBack"
            )
            val popScaleShare by animateFloatAsState(
                targetValue = if (progress > 0.85f) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
                label = "popScaleShare"
            )
            val popScaleMore by animateFloatAsState(
                targetValue = if (progress > 0.90f) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
                label = "popScaleMore"
            )
            val contentAlpha = ((progress - 0.4f).coerceAtLeast(0f) / 0.6f)

            GlassContainer(
                modifier = Modifier.fillMaxSize(),
                useShader = true,
                content = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(dominantColor.copy(alpha = contentAlpha))
                    ) {
            // Hero section
            item {
                GlassContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / 1.15f),
                    content = {
                        Box(modifier = Modifier.fillMaxSize().layerBackdrop(localBackdrop)) {
                            if (isReplay) {
                                val replayYearShort = currentPlaylist.id.substringAfter("replay_").takeLast(2)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFFF9500), // Yellow/orange
                                                    Color(0xFF4CD964)  // Green
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Replay",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "'$replayYearShort",
                                            color = Color.White,
                                            fontSize = 84.sp,
                                            fontWeight = FontWeight.Black,
                                            lineHeight = 80.sp
                                        )
                                    }
                                }
                            } else if (coverUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(coverUrl).crossfade(true).build(),
                                    contentDescription = currentPlaylist.name,
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
                                            0.85f to dominantColor.copy(alpha = 0.25f * contentAlpha),
                                            0.95f to dominantColor.copy(alpha = 0.7f * contentAlpha),
                                            1.0f to dominantColor.copy(alpha = contentAlpha)
                                        )
                                    )
                            )
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
                            // Bigger circular back button
                            scope.GlassBox(
                                modifier = Modifier
                                    .size(54.dp)
                                    .graphicsLayer {
                                        scaleX = popScaleBack
                                        scaleY = popScaleBack
                                        alpha = popScaleBack
                                    }
                                    .clip(CircleShape)
                                    .clickable { dismiss() },
                                shape = CircleShape,
                                tint = dominantColor.copy(alpha = 0.35f),
                                blur = 0.8f,
                                centerDistortion = 0.1f,
                                scale = 0.02f,
                                warpEdges = 0.4f,
                                elevation = 4.dp,
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBackIosNew,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            // Capsule containing Share and More options
                            scope.GlassBox(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = popScaleShare
                                        scaleY = popScaleShare
                                        alpha = popScaleShare
                                    }
                                    .height(48.dp),
                                shape = RoundedCornerShape(percent = 50),
                                tint = dominantColor.copy(alpha = 0.35f),
                                blur = 0.8f,
                                centerDistortion = 0.1f,
                                scale = 0.02f,
                                warpEdges = 0.4f,
                                elevation = 4.dp,
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val shareUrl = "https://music.youtube.com/playlist?list=${currentPlaylist.id.removePrefix("VL")}"
                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, currentPlaylist.name)
                                                putExtra(android.content.Intent.EXTRA_TEXT, "$shareUrl")
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir"))
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.IosShare,
                                            contentDescription = "Share",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            showPlaylistMenu = true
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
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
                        .padding(horizontal = 24.dp)
                        .graphicsLayer { alpha = contentAlpha },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentPlaylist.name,
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
                        Text(text = stringResource(R.string.my_playlist_subtitle), color = contentColor.copy(alpha = 0.8f), fontSize = 14.sp)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Play and Shuffle buttons
            if (currentPlaylist.items.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .graphicsLayer { alpha = contentAlpha },
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val buttonBg = Color.White
                        val redColor = Color(0xFFFA243C)

                        // Play button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(buttonBg)
                                .clickable {
                                    if (currentPlaylist.items.isNotEmpty()) {
                                        val songs = currentPlaylist.items.filter { it.type == com.mrtdk.liquid_glass.data.ItemType.SONG }
                                        if (songs.isNotEmpty()) {
                                            val favTitle = context.getString(R.string.favorite_songs)
                                            val isFavorites = currentPlaylist.id == "favorites"
                                            val first = songs.first()
                                            val remainingQueue = songs.drop(1).map { t ->
                                                com.mrtdk.liquid_glass.ui.screens.QueueItem(
                                                    title = t.title,
                                                    artist = t.subtitle,
                                                    artUrl = if (isFavorites) t.thumbnail else t.thumbnail ?: playlistCoverForPlayer,
                                                    videoId = t.id,
                                                    playlistId = if (isFavorites) "favorites" else currentPlaylist.id,
                                                    playlistName = if (isFavorites) favTitle else currentPlaylist.name
                                                )
                                            }
                                            onSongSelected(com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                                first.title, first.subtitle, 
                                                if (isFavorites) first.thumbnail else first.thumbnail ?: playlistCoverForPlayer, 
                                                first.id,
                                                queue = remainingQueue,
                                                isExclusiveQueue = true,
                                                playlistId = if (isFavorites) "favorites" else currentPlaylist.id,
                                                playlistName = if (isFavorites) favTitle else currentPlaylist.name
                                            ))
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = redColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.reproducir),
                                    color = redColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Shuffle button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(buttonBg)
                                .clickable {
                                    if (currentPlaylist.items.isNotEmpty()) {
                                        val songs = currentPlaylist.items.filter { it.type == com.mrtdk.liquid_glass.data.ItemType.SONG }
                                        if (songs.isNotEmpty()) {
                                            val shuffledSongs = songs.shuffled()
                                            val first = shuffledSongs.first()
                                            val remainingQueue = shuffledSongs.drop(1).map { t ->
                                                com.mrtdk.liquid_glass.ui.screens.QueueItem(
                                                    title = t.title,
                                                    artist = t.subtitle,
                                                    artUrl = t.thumbnail ?: playlistCoverForPlayer,
                                                    videoId = t.id,
                                                    playlistId = currentPlaylist.id,
                                                    playlistName = currentPlaylist.name
                                                )
                                            }
                                            onSongSelected(com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                                first.title, first.subtitle, 
                                                first.thumbnail ?: playlistCoverForPlayer, 
                                                first.id,
                                                queue = remainingQueue,
                                                isExclusiveQueue = true,
                                                playlistId = currentPlaylist.id,
                                                playlistName = currentPlaylist.name
                                            ))
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = redColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.shuffle_label),
                                    color = redColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Songs list or empty recommendations
            if (currentPlaylist.items.isEmpty()) {
                if (!isReplay) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        AddMusicRow(
                            onClick = { showAddMusicOverlay = true },
                            contentColor = contentColor
                        )
                    }
                    item {
                        SuggestedSongsSection(
                            playlistId = currentPlaylist.id,
                            playlistName = currentPlaylist.name,
                            contentColor = contentColor,
                            onAddSong = { song ->
                                LibraryManager.addSongToPlaylist(currentPlaylist.id, song)
                            }
                        )
                    }
                }
            } else {
                itemsIndexed(sortedItems) { trackIndex, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = contentAlpha }
                            .clickable {
                                if (track.type == com.mrtdk.liquid_glass.data.ItemType.ARTIST) {
                                    onArtistSelected(com.mrtdk.liquid_glass.ui.screens.ArtistState(
                                        id = track.id,
                                        name = track.title, // Title is the artist name in LibraryItem
                                        thumbnail = track.thumbnail
                                    ))
                                } else {
                                    // Build queue from remaining songs after this one
                                    val remainingQueue = sortedItems.drop(trackIndex + 1).filter { it.type == com.mrtdk.liquid_glass.data.ItemType.SONG }.map { t ->
                                        com.mrtdk.liquid_glass.ui.screens.QueueItem(
                                            title = t.title,
                                            artist = t.subtitle,
                                            artUrl = t.thumbnail ?: playlistCoverForPlayer,
                                            videoId = t.id,
                                            playlistId = currentPlaylist.id,
                                            playlistName = currentPlaylist.name
                                        )
                                    }
                                    onSongSelected(com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                        track.title, track.subtitle, 
                                        track.thumbnail ?: playlistCoverForPlayer, 
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
                                model = track.thumbnail ?: playlistCoverForPlayer,
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

                // Add music button and suggestions at the bottom
                if (!isReplay) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        AddMusicRow(
                            onClick = { showAddMusicOverlay = true },
                            contentColor = contentColor
                        )
                    }
                    item {
                        SuggestedSongsSection(
                            playlistId = currentPlaylist.id,
                            playlistName = currentPlaylist.name,
                            contentColor = contentColor,
                            onAddSong = { song ->
                                LibraryManager.addSongToPlaylist(currentPlaylist.id, song)
                            }
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
        },
        glassContent = {
            if (showPlaylistMenu) {
                AppleMusicPlaylistMenu(
                    playlist = currentPlaylist,
                    backdrop = localBackdrop,
                    dominantColor = dominantColor,
                    onDismiss = { showPlaylistMenu = false },
                    onSortSelected = { currentSort = it },
                    currentSort = currentSort,
                    onSongSelected = onSongSelected
                )
            }
        }
        )
    }

    if (showAddMusicOverlay) {
        AddMusicOverlay(
            playlistId = currentPlaylist.id,
            playlistName = currentPlaylist.name,
            onDismiss = { showAddMusicOverlay = false }
        )
    }
}
}

@Composable
fun FavoriteSongsScreen(
    onBack: () -> Unit,
    onSongSelected: (com.mrtdk.liquid_glass.ui.screens.PlayerState) -> Unit
) {
    val context = LocalContext.current
    val favoriteSongs by LibraryManager.savedItems.collectAsState()
    val songs = favoriteSongs.filter { it.type == com.mrtdk.liquid_glass.data.ItemType.SONG }

    // Dynamic color extraction from the first song thumbnail - same algorithm as albums/playlists
    var dominantColor by remember { mutableStateOf(Color(0xFF8B0000)) }
    var contentColor by remember { mutableStateOf(Color.White) }

    val firstThumbnail = songs.firstOrNull()?.thumbnail

    LaunchedEffect(firstThumbnail) {
        if (firstThumbnail != null) {
            val hdUrl = when {
                firstThumbnail.contains("=w") -> firstThumbnail.substringBefore("=w") + "=w500-h500-rj"
                firstThumbnail.contains("ytimg.com/vi/") -> firstThumbnail.replace("hqdefault", "maxresdefault")
                else -> firstThumbnail
            }
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
                    var r = 0L; var g = 0L; var b = 0L
                    val y = bitmap.height - 1
                    val w = bitmap.width
                    for (x in 0 until w) {
                        val pixel = bitmap.getPixel(x, y)
                        r += android.graphics.Color.red(pixel)
                        g += android.graphics.Color.green(pixel)
                        b += android.graphics.Color.blue(pixel)
                    }
                    val sampledColor = Color((r / w).toInt(), (g / w).toInt(), (b / w).toInt())
                    dominantColor = sampledColor
                    contentColor = if (sampledColor.luminance() > 0.5f) Color.Black else Color.White
                } catch (e: Exception) {}
            }
        }
    }

    val localBackdrop = rememberLayerBackdrop()

    SharedElementTransitionContainer(onBack = onBack) { progress, dismiss ->
        val popScaleBack by animateFloatAsState(
            targetValue = if (progress > 0.80f) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
            label = "popScaleBack"
        )
        val popScaleRight by animateFloatAsState(
            targetValue = if (progress > 0.87f) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
            label = "popScaleRight"
        )
        val contentAlpha = ((progress - 0.4f).coerceAtLeast(0f) / 0.6f)

        GlassContainer(
            modifier = Modifier.fillMaxSize(),
            useShader = true,
            content = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFA243C).copy(alpha = contentAlpha))
                ) {
                    // Hero section - star artwork with dynamic color
                    item {
                        GlassContainer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / 1.15f),
                            content = {
                                Box(modifier = Modifier.fillMaxSize().layerBackdrop(localBackdrop)) {
                                    // Dynamic-color gradient background as cover art
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFFFA243C),
                                                        Color(0xFFFA243C),
                                                        Color(0xFFE91E63),
                                                        Color(0xFF8B091A)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Decorative subtle circles
                                        androidx.compose.foundation.Canvas(
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            val cx = size.width / 2f
                                            val cy = size.height / 2f
                                            val starPositions = listOf(
                                                Pair(cx * 0.2f, cy * 0.3f) to 18f,
                                                Pair(cx * 1.7f, cy * 0.4f) to 14f,
                                                Pair(cx * 0.3f, cy * 1.6f) to 16f,
                                                Pair(cx * 1.6f, cy * 1.5f) to 20f,
                                                Pair(cx * 0.9f, cy * 0.15f) to 10f,
                                                Pair(cx * 1.1f, cy * 1.85f) to 12f
                                            )
                                            starPositions.forEach { (pos, r) ->
                                                drawCircle(
                                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f),
                                                    radius = r,
                                                    center = androidx.compose.ui.geometry.Offset(pos.first, pos.second)
                                                )
                                            }
                                        }
                                        // Red glow behind the star
                                        Box(
                                            modifier = Modifier
                                                .size(160.dp)
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = listOf(
                                                            Color(0xFFFA243C).copy(alpha = 0.8f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(120.dp)
                                        )
                                    }

                                    // Bottom fade to match dynamic background
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0.0f to Color.Transparent,
                                                    0.7f to Color.Transparent,
                                                    0.85f to Color(0xFFFA243C).copy(alpha = 0.25f * contentAlpha),
                                                    0.95f to Color(0xFFFA243C).copy(alpha = 0.7f * contentAlpha),
                                                    1.0f to Color(0xFFFA243C).copy(alpha = contentAlpha)
                                                )
                                            )
                                    )
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
                                     // ← Back button pill (left)
                                     scope.GlassBox(
                                         modifier = Modifier
                                             .size(54.dp)
                                             .graphicsLayer {
                                                 scaleX = popScaleBack
                                                 scaleY = popScaleBack
                                                 alpha = popScaleBack
                                             }
                                             .clickable { dismiss() },
                                         shape = CircleShape,
                                         tint = Color.White.copy(alpha = 0.15f),
                                         blur = 0.8f,
                                         centerDistortion = 0.1f,
                                         scale = 0.02f,
                                         warpEdges = 0.4f,
                                         elevation = 4.dp,
                                         contentAlignment = Alignment.Center
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.ArrowBackIosNew,
                                             contentDescription = "Back",
                                             tint = contentColor,
                                             modifier = Modifier.size(24.dp)
                                         )
                                     }
 
                                     // Right side: single capsule containing download and more options
                                     scope.GlassBox(
                                         modifier = Modifier
                                             .graphicsLayer {
                                                 scaleX = popScaleRight
                                                 scaleY = popScaleRight
                                                 alpha = popScaleRight
                                             }
                                             .height(48.dp),
                                         shape = RoundedCornerShape(percent = 50),
                                         tint = Color.White.copy(alpha = 0.15f),
                                         blur = 0.8f,
                                         centerDistortion = 0.1f,
                                         scale = 0.02f,
                                         warpEdges = 0.4f,
                                         elevation = 4.dp,
                                         contentAlignment = Alignment.Center
                                     ) {
                                         Row(
                                             modifier = Modifier.padding(horizontal = 8.dp),
                                             horizontalArrangement = Arrangement.spacedBy(8.dp),
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             IconButton(
                                                 onClick = { /* TODO: download all */ },
                                                 modifier = Modifier.size(40.dp)
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.ArrowCircleDown,
                                                     contentDescription = "Download",
                                                     tint = contentColor,
                                                     modifier = Modifier.size(22.dp)
                                                 )
                                             }
                                             IconButton(
                                                 onClick = { /* TODO: more options */ },
                                                 modifier = Modifier.size(40.dp)
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.MoreHoriz,
                                                     contentDescription = "More",
                                                     tint = contentColor,
                                                     modifier = Modifier.size(22.dp)
                                                 )
                                             }
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
                                .padding(horizontal = 24.dp)
                                .graphicsLayer { alpha = contentAlpha },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.favorite_songs),
                                color = contentColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.num_canciones, songs.size),
                                    color = contentColor.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Play and Shuffle buttons
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .graphicsLayer { alpha = contentAlpha },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val buttonBg = Color.White
                            val redColor = Color(0xFFFA243C)

                            // Play button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(buttonBg)
                                    .clickable {
                                        if (songs.isNotEmpty()) {
                                            val first = songs.first()
                                            val favTitle = context.getString(R.string.favorite_songs)
                                            val remainingQueue = songs.drop(1).map { t ->
                                                com.mrtdk.liquid_glass.ui.screens.QueueItem(
                                                    title = t.title,
                                                    artist = t.subtitle,
                                                    artUrl = t.thumbnail,
                                                    videoId = t.id,
                                                    playlistId = "favorites",
                                                    playlistName = favTitle
                                                )
                                            }
                                            onSongSelected(com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                                first.title, first.subtitle,
                                                first.thumbnail,
                                                first.id,
                                                queue = remainingQueue,
                                                isExclusiveQueue = true,
                                                playlistId = "favorites",
                                                playlistName = favTitle
                                            ))
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = redColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.reproducir),
                                        color = redColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Shuffle button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(buttonBg)
                                    .clickable {
                                        if (songs.isNotEmpty()) {
                                            val shuffledSongs = songs.shuffled()
                                            val first = shuffledSongs.first()
                                            val favTitle = context.getString(R.string.favorite_songs)
                                            val remainingQueue = shuffledSongs.drop(1).map { t ->
                                                com.mrtdk.liquid_glass.ui.screens.QueueItem(
                                                    title = t.title,
                                                    artist = t.subtitle,
                                                    artUrl = t.thumbnail,
                                                    videoId = t.id,
                                                    playlistId = "favorites",
                                                    playlistName = favTitle
                                                )
                                            }
                                            onSongSelected(com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                                first.title, first.subtitle,
                                                first.thumbnail,
                                                first.id,
                                                queue = remainingQueue,
                                                isExclusiveQueue = true,
                                                playlistId = "favorites",
                                                playlistName = favTitle
                                            ))
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = redColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.shuffle_label),
                                        color = redColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Empty state
                    if (songs.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp)
                                    .graphicsLayer { alpha = contentAlpha },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.no_favorite_songs),
                                    color = contentColor.copy(alpha = 0.6f),
                                    fontSize = 16.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Songs list
                    itemsIndexed(songs) { trackIndex, track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { alpha = contentAlpha }
                                .clickable {
                                    val favTitle = context.getString(R.string.favorite_songs)
                                    val remainingQueue = songs.drop(trackIndex + 1).map { t ->
                                        com.mrtdk.liquid_glass.ui.screens.QueueItem(
                                            title = t.title,
                                            artist = t.subtitle,
                                            artUrl = t.thumbnail,
                                            videoId = t.id,
                                            playlistId = "favorites",
                                            playlistName = favTitle
                                        )
                                    }
                                    onSongSelected(com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                        track.title, track.subtitle,
                                        track.thumbnail,
                                        track.id,
                                        queue = remainingQueue,
                                        isExclusiveQueue = true,
                                        playlistId = "favorites",
                                        playlistName = favTitle
                                    ))
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = track.thumbnail,
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
                                    color = contentColor.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Remove from favorites button
                            Icon(
                                Icons.Default.Star,
                                "Unfavorite",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        LibraryManager.removeItem(track.id)
                                    }
                            )
                        }
                        androidx.compose.material3.Divider(
                            modifier = Modifier.padding(start = 84.dp),
                            color = contentColor.copy(alpha = 0.1f),
                            thickness = 0.5.dp
                        )
                    }

                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            },
            glassContent = {}
        )
    }
}

val suggestedClassicalSongs = listOf(
    LibraryItem("58wDHoIlu-g", "21 Hungarian Dances, WoO 1: No. 5 in F-Sharp Minor (Orch. Schmeling)", "Filarmónica de Viena & Claudio Abbado", "https://i.ytimg.com/vi/58wDHoIlu-g/hqdefault.jpg", ItemType.SONG, "Classical"),
    LibraryItem("13p8vjTjP4c", "Swan Lake, Op. 20, Act I: Danse des coupes", "Ernest Ansermet & Orchestre de la Suisse Romande", "https://i.ytimg.com/vi/13p8vjTjP4c/hqdefault.jpg", ItemType.SONG, "Classical"),
    LibraryItem("yKOpU5y75O4", "William Tell: Overture", "London Symphony Orchestra & Peter Maag", "https://i.ytimg.com/vi/yKOpU5y75O4/hqdefault.jpg", ItemType.SONG, "Classical"),
    LibraryItem("mGQLXRTl3Z0", "Cello Suite No. 1 in G Major, BWV 1007: I. Prélude", "Jean-Guihen Queyras", "https://i.ytimg.com/vi/mGQLXRTl3Z0/hqdefault.jpg", ItemType.SONG, "Classical"),
    LibraryItem("cWc7vYjgnTs", "Turandot: Nessun dorma!", "Luciano Pavarotti", "https://i.ytimg.com/vi/cWc7vYjgnTs/hqdefault.jpg", ItemType.SONG, "Classical"),
    LibraryItem("k1-TrAvp_xs", "Requiem in D Minor, K. 626: Lacrimosa", "Herbert von Karajan & Berliner Philharmoniker", "https://i.ytimg.com/vi/k1-TrAvp_xs/hqdefault.jpg", ItemType.SONG, "Classical"),
    LibraryItem("3gr6n7u14kU", "Hungarian Dance No. 5 in G Minor", "NDR Elbphilharmonie Orchester", "https://i.ytimg.com/vi/3gr6n7u14kU/hqdefault.jpg", ItemType.SONG, "Classical"),
    LibraryItem("w4Z1eBprZ4Y", "The Nutcracker, Act II: Dance of the Sugar Plum Fairy", "Ernest Ansermet", "https://i.ytimg.com/vi/w4Z1eBprZ4Y/hqdefault.jpg", ItemType.SONG, "Classical"),
    LibraryItem("4j0p5c0TjYI", "Carmen Suite No. 1: III. Intermezzo", "Herbert von Karajan", "https://i.ytimg.com/vi/4j0p5c0TjYI/hqdefault.jpg", ItemType.SONG, "Classical"),
    LibraryItem("nPbxIT9W16A", "Eine kleine Nachtmusik, K. 525: I. Allegro", "Karl Böhm & Wiener Philharmoniker", "https://i.ytimg.com/vi/nPbxIT9W16A/hqdefault.jpg", ItemType.SONG, "Classical")
)

@Composable
fun AddMusicRow(onClick: () -> Unit, contentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFFA243C), modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Añadir música",
            color = contentColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SuggestedSongsSection(
    playlistId: String,
    playlistName: String,
    contentColor: Color,
    onAddSong: (LibraryItem) -> Unit
) {
    val playlists by LibraryManager.playlists.collectAsState()
    val currentPlaylist = remember(playlists, playlistId) {
        playlists.find { it.id == playlistId }
    }
    val playlistSongIds = remember(currentPlaylist) {
        currentPlaylist?.items?.map { it.id }?.toSet() ?: emptySet()
    }

    val allSuggestions = remember { suggestedClassicalSongs }
    val availableSuggestions = remember(allSuggestions, playlistSongIds) {
        allSuggestions.filter { it.id !in playlistSongIds }
    }

    var seed by remember { mutableStateOf(0) }
    val displayedSuggestions = remember(availableSuggestions, seed) {
        availableSuggestions.shuffled().take(5)
    }

    if (displayedSuggestions.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Canciones sugeridas",
                        color = contentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Escucha un fragmento y añade la pista a la playlist.",
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { seed++ }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = contentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            displayedSuggestions.forEach { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.subtitle,
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFA243C))
                            .clickable { onAddSong(song) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                androidx.compose.material3.Divider(
                    color = contentColor.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMusicOverlay(
    playlistId: String,
    playlistName: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Dismiss target on the top 12% of screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.12f)
                    .align(Alignment.TopCenter)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )

            AddMusicContent(
                playlistId = playlistId,
                playlistName = playlistName,
                onDismiss = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMusicContent(
    playlistId: String,
    playlistName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Any>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var bannerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val context = LocalContext.current

    val playlists by LibraryManager.playlists.collectAsState()
    val currentPlaylist = remember(playlists, playlistId) {
        playlists.find { it.id == playlistId }
    }
    val playlistSongIds = remember(currentPlaylist) {
        currentPlaylist?.items?.map { it.id }?.toSet() ?: emptySet()
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabNames = listOf("Resultados principales", "Artistas", "Álbumes", "Canciones")

    // Dynamic title in header
    var headerTitle by remember { mutableStateOf("Añadir a \"$playlistName\"") }

    fun triggerAddedBanner() {
        bannerJob?.cancel()
        bannerJob = coroutineScope.launch {
            headerTitle = "Se ha añadido 1 canción a \"$playlistName\""
            kotlinx.coroutines.delay(3000)
            headerTitle = "Añadir a \"$playlistName\""
        }
    }

    // Nested navigation states
    var activeAlbumId by remember { mutableStateOf<String?>(null) }
    var activeAlbumName by remember { mutableStateOf<String?>(null) }
    var activeArtistId by remember { mutableStateOf<String?>(null) }
    var activeArtistName by remember { mutableStateOf<String?>(null) }
    var nestedSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isNestedLoading by remember { mutableStateOf(false) }

    LaunchedEffect(query, selectedTab) {
        if (query.trim().length < 2) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        
        isSearching = true
        activeAlbumId = null
        activeArtistId = null
        nestedSongs = emptyList()
        
        withContext(Dispatchers.IO) {
            if (selectedTab == 0) {
                // Resultados principales: search songs, artists, albums in parallel
                YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).onSuccess { result ->
                    val songs = result.items.filterIsInstance<SongItem>().take(10)
                    val artists = try { YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()?.items?.filterIsInstance<ArtistItem>()?.take(3) ?: emptyList() } catch (_: Exception) { emptyList() }
                    val albums = try { YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()?.items?.filterIsInstance<AlbumItem>()?.take(3) ?: emptyList() } catch (_: Exception) { emptyList() }
                    searchResults = artists + albums + songs
                }.onFailure {
                    searchResults = emptyList()
                }
            } else {
                val filter = when (selectedTab) {
                    1 -> YouTube.SearchFilter.FILTER_ARTIST
                    2 -> YouTube.SearchFilter.FILTER_ALBUM
                    3 -> YouTube.SearchFilter.FILTER_SONG
                    else -> YouTube.SearchFilter.FILTER_SONG
                }
                YouTube.search(query, filter).onSuccess { result ->
                    searchResults = when (selectedTab) {
                        1 -> result.items.filterIsInstance<ArtistItem>().take(20)
                        2 -> result.items.filterIsInstance<AlbumItem>().take(20)
                        else -> result.items.filterIsInstance<SongItem>().take(20)
                    }
                }.onFailure {
                    searchResults = emptyList()
                }
            }
        }
        isSearching = false
    }

    LaunchedEffect(activeAlbumId) {
        if (activeAlbumId != null) {
            isNestedLoading = true
            withContext(Dispatchers.IO) {
                val isAlbum = activeAlbumId!!.startsWith("MPREb") || activeAlbumId!!.startsWith("FEmusic")
                if (isAlbum) {
                    YouTube.album(activeAlbumId!!).onSuccess { album ->
                        nestedSongs = album.songs
                    }.onFailure {
                        nestedSongs = emptyList()
                    }
                } else {
                    val pId = activeAlbumId!!.removePrefix("VL")
                    YouTube.playlist(pId).onSuccess { playlist ->
                        nestedSongs = playlist.songs
                    }.onFailure {
                        nestedSongs = emptyList()
                    }
                }
            }
            isNestedLoading = false
        }
    }

    LaunchedEffect(activeArtistId) {
        if (activeArtistId != null) {
            isNestedLoading = true
            withContext(Dispatchers.IO) {
                YouTube.artist(activeArtistId!!).onSuccess { page ->
                    val songsSection = page.sections.find { it.title.contains("song", true) || it.title.contains("cancion", true) }
                    nestedSongs = songsSection?.items?.filterIsInstance<SongItem>() ?: emptyList()
                }.onFailure {
                    nestedSongs = emptyList()
                }
            }
            isNestedLoading = false
        }
    }

    Column(
        modifier = modifier
            .background(
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .navigationBarsPadding()
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable {
                        if (activeAlbumId != null || activeArtistId != null) {
                            activeAlbumId = null
                            activeArtistId = null
                            nestedSongs = emptyList()
                        } else if (query.isNotEmpty()) {
                            query = ""
                        } else {
                            onDismiss()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (query.isNotEmpty() || activeAlbumId != null || activeArtistId != null) Icons.Default.ArrowBackIosNew else Icons.Default.Close,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = if (activeAlbumId != null) (activeAlbumName ?: "Álbum")
                       else if (activeArtistId != null) (activeArtistName ?: "Artista")
                       else headerTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFA243C))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Search Field & Clear/Microphone
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        text = "Artistas, canciones, letras y más",
                        color = Color.Gray
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = null, 
                            tint = Color.Gray,
                            modifier = Modifier.clickable { query = "" }
                        )
                    } else {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Gray)
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF2C2C2E),
                    unfocusedContainerColor = Color(0xFF2C2C2E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { query = "" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Search",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Filter tabs (only show when query is not empty and not in nested view)
        if (query.isNotEmpty() && activeAlbumId == null && activeArtistId == null) {
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabNames.size) { i ->
                    val isSelected = i == selectedTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) Color(0xFFFA243C)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .clickable { selectedTab = i }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = tabNames[i],
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // List Content
        Box(modifier = Modifier.fillMaxSize()) {
            if (activeAlbumId != null || activeArtistId != null) {
                // Nested view list
                if (isNestedLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFA243C))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(nestedSongs.size) { idx ->
                            val song = nestedSongs[idx]
                            val isAlreadyAdded = song.id in playlistSongIds
                            val hdThumb = song.thumbnail.let {
                                when {
                                    it.contains("=w") -> it.substringBefore("=w") + "=w500-h500-rj"
                                    it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                    else -> it
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = hdThumb,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                
                                if (isAlreadyAdded) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Added",
                                        tint = Color(0xFFFA243C),
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, Color(0xFFFA243C), CircleShape)
                                            .clickable {
                                                val songItem = LibraryItem(
                                                    id = song.id,
                                                    title = song.title,
                                                    subtitle = song.artists.joinToString { it.name },
                                                    thumbnail = hdThumb,
                                                    type = ItemType.SONG,
                                                    album = song.album?.name
                                                )
                                                LibraryManager.addSongToPlaylist(playlistId, songItem)
                                                triggerAddedBanner()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            androidx.compose.material3.Divider(
                                color = Color.White.copy(alpha = 0.1f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 88.dp)
                            )
                        }
                    }
                }
            } else if (query.trim().isEmpty()) {
                // Empty query - show directories and classical suggestions
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = "Biblioteca",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                    
                    val directories = listOf(
                        Triple("Artistas", Icons.Default.MusicNote, Color(0xFFFA243C)),
                        Triple("Álbumes", Icons.Default.Album, Color(0xFFFA243C)),
                        Triple("Canciones", Icons.Default.Star, Color(0xFFFA243C)),
                        Triple("Playlists", Icons.Default.List, Color(0xFFFA243C)),
                        Triple("Descargas", Icons.Default.ArrowCircleDown, Color(0xFFFA243C)),
                        Triple("Música recién añadida", Icons.Default.Schedule, Color(0xFFFA243C))
                    )

                    items(directories.size) { idx ->
                        val (title, icon, tint) = directories[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* Navigate */ }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(title, color = Color.White, fontSize = 16.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                        androidx.compose.material3.Divider(
                            color = Color.White.copy(alpha = 0.1f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 64.dp)
                        )
                    }

                    item {
                        Text(
                            text = "Canciones sugeridas",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
                        )
                    }

                    val availableSuggestions = suggestedClassicalSongs.filter { it.id !in playlistSongIds }
                    items(availableSuggestions.size) { idx ->
                        val song = availableSuggestions[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = song.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.subtitle, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color(0xFFFA243C), CircleShape)
                                    .clickable {
                                        LibraryManager.addSongToPlaylist(playlistId, song)
                                        triggerAddedBanner()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                            }
                        }
                        androidx.compose.material3.Divider(
                            color = Color.White.copy(alpha = 0.1f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 88.dp)
                        )
                    }
                }
            } else {
                // Query active - show search results
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFA243C))
                    }
                } else if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron resultados", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults.size) { idx ->
                            val item = searchResults[idx]
                            
                            when (item) {
                                is SongItem -> {
                                    val hdThumb = item.thumbnail.let {
                                        when {
                                            it.contains("=w") -> it.substringBefore("=w") + "=w500-h500-rj"
                                            it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                            else -> it
                                        }
                                    }
                                    
                                    val isAlreadyAdded = item.id in playlistSongIds

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = hdThumb,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Canción · ${item.artists.joinToString { it.name }}", color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        
                                        if (isAlreadyAdded) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Added",
                                                tint = Color(0xFFFA243C),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, Color(0xFFFA243C), CircleShape)
                                                    .clickable {
                                                        val songItem = LibraryItem(
                                                            id = item.id,
                                                            title = item.title,
                                                            subtitle = item.artists.joinToString { it.name },
                                                            thumbnail = hdThumb,
                                                            type = ItemType.SONG,
                                                            album = item.album?.name
                                                        )
                                                        LibraryManager.addSongToPlaylist(playlistId, songItem)
                                                        triggerAddedBanner()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFFA243C), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                                is AlbumItem -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                activeAlbumId = item.browseId
                                                activeAlbumName = item.title
                                            }
                                            .padding(horizontal = 24.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = item.thumbnail,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Álbum · ${item.year ?: ""}", color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                                is ArtistItem -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                activeArtistId = item.id
                                                activeArtistName = item.title
                                            }
                                            .padding(horizontal = 24.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = item.thumbnail,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Artista", color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                            }
                            androidx.compose.material3.Divider(
                                color = Color.White.copy(alpha = 0.1f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 88.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
