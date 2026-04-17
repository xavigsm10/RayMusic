package com.mrtdk.liquid_glass

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mrtdk.glass.GlassContainer
import com.mrtdk.glass.GlassBox
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.mrtdk.liquid_glass.playback.MusicPlayer
import com.mrtdk.liquid_glass.ui.LiquidBottomNavBar
import com.mrtdk.liquid_glass.ui.components.MiniPlayer
import com.mrtdk.liquid_glass.ui.screens.AlbumScreen
import com.mrtdk.liquid_glass.ui.screens.AlbumState
import com.mrtdk.liquid_glass.ui.screens.PlaylistDetailScreen
import com.mrtdk.liquid_glass.data.Playlist
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.ui.screens.ArtistScreen
import com.mrtdk.liquid_glass.ui.screens.ArtistState
import com.mrtdk.liquid_glass.ui.screens.BibliotecaScreen
import com.mrtdk.liquid_glass.ui.screens.BusquedaScreen
import com.mrtdk.liquid_glass.ui.screens.InicioScreen
import com.mrtdk.liquid_glass.ui.screens.NovedadesScreen
import com.mrtdk.liquid_glass.ui.screens.PlayerScreen
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import com.mrtdk.liquid_glass.ui.theme.LiquidglassuicomponentTheme

class MainActivity : ComponentActivity() {
    private var musicPlayer: MusicPlayer? = null

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Liquidglassuicomponent)
        enableEdgeToEdge()
        musicPlayer = MusicPlayer(this)
        com.mrtdk.liquid_glass.data.LibraryManager.init(applicationContext)

        setContent {
            LiquidglassuicomponentTheme {
                val context = LocalContext.current
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                val permissionState = rememberPermissionState(permission)

                if (!permissionState.status.isGranted) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Se necesita permiso para leer tu música local", color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { permissionState.launchPermissionRequest() }) {
                                Text("Conceder Permiso")
                            }
                        }
                    }
                } else {
                    var selectedIndex by remember { mutableIntStateOf(0) }
                    var playerState by remember { mutableStateOf<PlayerState?>(null) }
                    var showPlayer by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        playerState = com.mrtdk.liquid_glass.data.LibraryManager.getLastPlayerState()
                    }

                    LaunchedEffect(playerState) {
                        com.mrtdk.liquid_glass.data.LibraryManager.saveLastPlayerState(playerState)
                    }
                    
                    var searchQuery by remember { mutableStateOf("") }
                    var isSearchSubmitted by remember { mutableStateOf(false) }

                    // Detail screen states
                    var artistDetail by remember { mutableStateOf<ArtistState?>(null) }
                    var albumDetail by remember { mutableStateOf<AlbumState?>(null) }
                    var playlistDetail by remember { mutableStateOf<Playlist?>(null) }

                    // Dominant color extraction for glass tints
                    var globalDominantColor by remember { mutableStateOf(Color.White.copy(alpha = 0.15f)) }
                    var contentTintColor by remember { mutableStateOf(Color.White) }
                    LaunchedEffect(playerState?.artUrl) {
                        val url = playerState?.artUrl
                        if (url != null) {
                            val hdUrl = if (url is String) {
                                when {
                                    url.contains("=w") -> url.substringBefore("=w") + "=w300-h300-rj"
                                    url.contains("ytimg.com/vi/") -> url.replace("hqdefault", "mqdefault")
                                    else -> url
                                }
                            } else url
                            val request = coil.request.ImageRequest.Builder(context)
                                .data(hdUrl).allowHardware(false).size(100).build()
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
                                    globalDominantColor = sampledColor
                                    contentTintColor = Color.White
                                } catch (e: Exception) { }
                            }
                        } else {
                            globalDominantColor = Color.White.copy(alpha = 0.15f)
                            contentTintColor = Color.White
                        }
                    }

                    val isPlaying by musicPlayer!!.isPlaying.collectAsState()
                    val currentPosition by musicPlayer!!.currentPosition.collectAsState()
                    val duration by musicPlayer!!.duration.collectAsState()

                    var isBottomBarCollapsed by remember { mutableStateOf(false) }

                    val nestedScrollConnection = remember {
                        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                                if (available.y < -30f) isBottomBarCollapsed = true
                                return androidx.compose.ui.geometry.Offset.Zero
                            }
                        }
                    }

                    // Helper to play a song
                    val playSong: (PlayerState) -> Unit = { state ->
                        playerState = state
                        showPlayer = true
                        // Track recently played
                        LibraryManager.addRecentlyPlayed(
                            com.mrtdk.liquid_glass.data.LibraryItem(
                                id = state.videoId ?: state.title,
                                title = state.title,
                                subtitle = state.artist,
                                thumbnail = state.artUrl?.toString(),
                                type = com.mrtdk.liquid_glass.data.ItemType.SONG
                            )
                        )
                        if (state.contentUri != null) musicPlayer?.playLocalSong(state.contentUri)
                        else if (state.videoId != null) musicPlayer?.playOnlineSong(state.videoId)
                    }

                    // Auto-play next song when current song ends
                    val songEndedCount by musicPlayer!!.songEnded.collectAsState()
                    LaunchedEffect(songEndedCount) {
                        if (songEndedCount > 0 && playerState != null) {
                            val queue = playerState!!.queue
                            if (queue.isNotEmpty()) {
                                val nextItem = queue.first()
                                val remainingQueue = queue.drop(1)
                                val nextState = PlayerState(
                                    title = nextItem.title,
                                    artist = nextItem.artist,
                                    artUrl = nextItem.artUrl,
                                    videoId = nextItem.videoId,
                                    queue = remainingQueue
                                )
                                playerState = nextState
                                showPlayer = true
                                LibraryManager.addRecentlyPlayed(
                                    com.mrtdk.liquid_glass.data.LibraryItem(
                                        id = nextItem.videoId ?: nextItem.title,
                                        title = nextItem.title,
                                        subtitle = nextItem.artist,
                                        thumbnail = nextItem.artUrl?.toString(),
                                        type = com.mrtdk.liquid_glass.data.ItemType.SONG
                                    )
                                )
                                if (nextItem.videoId != null) musicPlayer?.playOnlineSong(nextItem.videoId)
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Black
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black).nestedScroll(nestedScrollConnection)) {
                            GlassContainer(
                                modifier = Modifier.fillMaxSize().background(Color.Black),
                                content = {
                                    // Combine state to trigger AnimatedContent
                                    data class NavState(val tab: Int, val playlist: Playlist?, val album: AlbumState?, val artist: ArtistState?)
                                    val currentNav = NavState(selectedIndex, playlistDetail, albumDetail, artistDetail)
                                    
                                    androidx.compose.animation.AnimatedContent(
                                        targetState = currentNav,
                                        modifier = Modifier.fillMaxSize().background(Color.Black),
                                        transitionSpec = {
                                            if ((targetState.playlist != null && initialState.playlist == null) || 
                                                (targetState.album != null && initialState.album == null) || 
                                                (targetState.artist != null && initialState.artist == null)) {
                                                // Forward (Slide in from right)
                                                androidx.compose.animation.slideInHorizontally(
                                                    animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                                                ) { width -> width } + androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.slideOutHorizontally(
                                                    animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                                                ) { width -> -width / 3 } + androidx.compose.animation.fadeOut()
                                            } else if ((initialState.playlist != null && targetState.playlist == null) || 
                                                     (initialState.album != null && targetState.album == null) || 
                                                     (initialState.artist != null && targetState.artist == null)) {
                                                // Backward (Slide out to right)
                                                androidx.compose.animation.slideInHorizontally(
                                                    animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                                                ) { width -> -width / 3 } + androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.slideOutHorizontally(
                                                    animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                                                ) { width -> width } + androidx.compose.animation.fadeOut()
                                            } else {
                                                // Tab switch (Crossfade)
                                                androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut()
                                            }
                                        },
                                        label = "ios_page_transition"
                                    ) { state ->
                                        when {
                                            state.playlist != null -> {
                                            PlaylistDetailScreen(
                                                playlist = state.playlist,
                                                onBack = { playlistDetail = null },
                                                onSongSelected = playSong
                                            )
                                        }
                                        state.album != null -> {
                                            AlbumScreen(
                                                albumState = state.album,
                                                onBack = { albumDetail = null },
                                                onSongSelected = playSong
                                            )
                                        }
                                        state.artist != null -> {
                                            ArtistScreen(
                                                artistState = state.artist,
                                                onBack = { artistDetail = null },
                                                onSongSelected = playSong,
                                                onAlbumSelected = { album ->
                                                    albumDetail = album
                                                }
                                            )
                                        }
                                        else -> {
                                            when (state.tab) {
                                                0 -> InicioScreen(
                                                    innerPadding = innerPadding,
                                                    playerState = playerState,
                                                    onSongSelected = playSong,
                                                    onArtistSelected = { artistDetail = it },
                                                    onAlbumSelected = { albumDetail = it }
                                                )
                                                1 -> NovedadesScreen(innerPadding, onSongSelected = playSong)
                                                3 -> BibliotecaScreen(
                                                    innerPadding = innerPadding, 
                                                    onSongSelected = playSong,
                                                    onPlaylistSelected = { playlistDetail = it }
                                                )
                                                4 -> BusquedaScreen(
                                                    innerPadding = innerPadding,
                                                    query = searchQuery,
                                                    isSubmitted = isSearchSubmitted,
                                                    onSongSelected = playSong,
                                                    onArtistSelected = { artist ->
                                                        artistDetail = artist
                                                    },
                                                    onAlbumSelected = { album ->
                                                        albumDetail = album
                                                    }
                                                )
                                                else -> DemoBackground(innerPadding)
                                            }
                                        }
                                    }
                                    }
                                },
                                glassContent = {
                                    val scope = this
                                    val imeBottom = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
                                    val isKeyboardOpen = imeBottom > 0
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(
                                                top = innerPadding.calculateTopPadding(),
                                                bottom = if (isKeyboardOpen) 0.dp else innerPadding.calculateBottomPadding()
                                            )
                                            .imePadding(),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        val isCollapsed = isBottomBarCollapsed && playerState != null
                                        
                                        val springSpec = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(
                                            dampingRatio = 0.8f,
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                        )
                                        val floatSpringSpec = androidx.compose.animation.core.spring<Float>(
                                            dampingRatio = 0.8f,
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                        )

                                        val bottomPad = if (isKeyboardOpen) 0.dp else 16.dp
                                        val navBarHeightWithPadding = 72.dp + bottomPad

                                        val mpPadStart by androidx.compose.animation.core.animateDpAsState(if (isCollapsed) 80.dp else 16.dp, springSpec)
                                        val mpPadEnd by androidx.compose.animation.core.animateDpAsState(if (isCollapsed) 80.dp else 16.dp, springSpec)
                                        val mpPadBottom by androidx.compose.animation.core.animateDpAsState(if (isCollapsed) bottomPad else navBarHeightWithPadding + 12.dp, springSpec)

                                        val navBarOffset by androidx.compose.animation.core.animateDpAsState(if (isCollapsed) 100.dp else 0.dp, springSpec)
                                        val navBarAlpha by androidx.compose.animation.core.animateFloatAsState(if (isCollapsed) 0f else 1f, floatSpringSpec)

                                        val btnOffset by androidx.compose.animation.core.animateDpAsState(if (isCollapsed) 0.dp else 50.dp, springSpec)
                                        val btnAlpha by androidx.compose.animation.core.animateFloatAsState(if (isCollapsed) 1f else 0f, floatSpringSpec)

                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(200.dp).align(Alignment.BottomCenter)
                                        ) {
                                            // LiquidBottomNavBar (Expanded)
                                            if (navBarAlpha > 0.01f) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = bottomPad)
                                                        .offset(y = navBarOffset)
                                                        .graphicsLayer { alpha = navBarAlpha }
                                                ) {
                                                    LiquidBottomNavBar(
                                                        glassScope = scope,
                                                        selectedIndex = selectedIndex,
                                                        tintColor = globalDominantColor.copy(alpha = 0.35f),
                                                        contentColor = contentTintColor,
                                                        onTabSelected = { newIndex ->
                                                            artistDetail = null
                                                            albumDetail = null
                                                            playlistDetail = null
                                                            selectedIndex = newIndex
                                                            if (newIndex != 4) {
                                                                searchQuery = ""
                                                                isSearchSubmitted = false
                                                            }
                                                        },
                                                        searchQuery = searchQuery,
                                                        onSearchQueryChange = { 
                                                            searchQuery = it 
                                                            isSearchSubmitted = false
                                                        },
                                                        onSearchSubmit = { isSearchSubmitted = true }
                                                    )
                                                }
                                            }

                                            // MiniPlayer (Shared)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter)
                                                    .padding(start = mpPadStart, end = mpPadEnd, bottom = mpPadBottom)
                                            ) {
                                                scope.MiniPlayer(
                                                    playerState = playerState,
                                                    isPlaying = isPlaying,
                                                    onTogglePlayPause = { musicPlayer?.togglePlayPause() },
                                                    onClick = { if (playerState != null) showPlayer = true },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    tintColor = globalDominantColor.copy(alpha = 0.45f),
                                                    contentColor = contentTintColor
                                                )
                                            }

                                            // Collapsed Buttons (Home & Search)
                                            if (btnAlpha > 0.01f) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.BottomCenter)
                                                        .padding(horizontal = 16.dp)
                                                        .padding(bottom = bottomPad)
                                                        .offset(y = btnOffset)
                                                        .graphicsLayer { alpha = btnAlpha },
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    scope.GlassBox(
                                                        modifier = Modifier.size(56.dp).clickable { 
                                                            isBottomBarCollapsed = false
                                                            selectedIndex = 0
                                                        },
                                                        shape = CircleShape,
                                                        tint = globalDominantColor.copy(alpha = 0.35f),
                                                        blur = 0.8f, centerDistortion = 0.1f, scale = 0.02f, warpEdges = 0.4f, elevation = 4.dp
                                                    ) {
                                                        Box(modifier=Modifier.fillMaxSize(), contentAlignment=Alignment.Center) {
                                                            Icon(Icons.Default.Home, contentDescription="Home", tint = Color(0xFFFA243C))
                                                        }
                                                    }
                                                    
                                                    scope.GlassBox(
                                                        modifier = Modifier.size(56.dp).clickable { 
                                                            isBottomBarCollapsed = false
                                                            selectedIndex = 4 
                                                        },
                                                        shape = CircleShape,
                                                        tint = globalDominantColor.copy(alpha = 0.35f),
                                                        blur = 0.8f, centerDistortion = 0.1f, scale = 0.02f, warpEdges = 0.4f, elevation = 4.dp
                                                    ) {
                                                        Box(modifier=Modifier.fillMaxSize(), contentAlignment=Alignment.Center) {
                                                            Icon(Icons.Default.Search, contentDescription="Search", tint = contentTintColor)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                            
                            PlayerScreen(
                                playerState = playerState,
                                isVisible = showPlayer,
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                duration = duration,
                                isBottomBarCollapsed = isBottomBarCollapsed,
                                onClose = { showPlayer = false },
                                onTogglePlayPause = { musicPlayer?.togglePlayPause() },
                                onSeek = { musicPlayer?.seekTo(it) },
                                onVolumeChange = { musicPlayer?.setVolume(it) },
                                onArtistSelected = { artist ->
                                    showPlayer = false
                                    artistDetail = artist
                                },
                                onSongSelected = playSong
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayer?.release()
    }
}

@Composable
fun DemoBackground(innerPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = innerPadding.calculateTopPadding() + 16.dp,
            bottom = innerPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(20) { index ->
            val colors = listOf(
                Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
                Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4),
                Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50)
            )
            val color1 = colors[index % colors.size]
            val color2 = colors[(index + 1) % colors.size]
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(color1, color2)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Elemento ${index + 1}", color = Color.White)
            }
        }
    }
}