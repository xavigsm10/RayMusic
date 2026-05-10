package com.mrtdk.liquid_glass

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.withContext
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
import com.mrtdk.liquid_glass.ui.screens.VideoPlayerScreen
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

                    // Persistent states for tabs
                    val inicioState = remember { com.mrtdk.liquid_glass.ui.screens.InicioState() }
                    val novedadesState = remember { com.mrtdk.liquid_glass.ui.screens.NovedadesState() }
                    val busquedaState = remember { com.mrtdk.liquid_glass.ui.screens.BusquedaState() }

                    // Detail screen states
                    var artistDetail by remember { mutableStateOf<ArtistState?>(null) }
                    var albumDetail by remember { mutableStateOf<AlbumState?>(null) }
                    var playlistDetail by remember { mutableStateOf<Playlist?>(null) }
                    var videoDetail by remember { mutableStateOf<String?>(null) }
                    var categoryDetail by remember { mutableStateOf<com.mrtdk.liquid_glass.ui.screens.SearchCategory?>(null) }

                    // Handle system back navigation
                    androidx.activity.compose.BackHandler(
                        enabled = showPlayer || videoDetail != null || playlistDetail != null || albumDetail != null || artistDetail != null || categoryDetail != null || (selectedIndex == 4 && isSearchSubmitted) || selectedIndex != 0
                    ) {
                        when {
                            showPlayer -> showPlayer = false
                            videoDetail != null -> videoDetail = null
                            playlistDetail != null -> playlistDetail = null
                            albumDetail != null -> albumDetail = null
                            artistDetail != null -> artistDetail = null
                            categoryDetail != null -> categoryDetail = null
                            selectedIndex == 4 && isSearchSubmitted -> {
                                isSearchSubmitted = false
                                searchQuery = ""
                            }
                            selectedIndex != 0 -> selectedIndex = 0
                        }
                    }

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
                        else if (state.videoId != null) musicPlayer?.playOnlineSong(state.videoId, state.title, state.artist, state.artUrl?.toString())
                    }

                    var upNextSongs by remember { mutableStateOf<List<com.echo.innertube.models.SongItem>>(emptyList()) }
                    var queueSeedVideoId by remember { mutableStateOf<String?>(null) }
                    var queueContinuation by remember { mutableStateOf<String?>(null) }
                    var queueEndpoint by remember { mutableStateOf<com.echo.innertube.models.WatchEndpoint?>(null) }
                    val songHistory = remember { androidx.compose.runtime.mutableStateListOf<PlayerState>() }

                    LaunchedEffect(playerState?.videoId) {
                        val vid = playerState?.videoId ?: return@LaunchedEffect
                        if (upNextSongs.isEmpty() || queueSeedVideoId != vid) {
                            queueSeedVideoId = vid
                            withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val endpoint = com.echo.innertube.models.WatchEndpoint(videoId = vid)
                                com.echo.innertube.YouTube.next(endpoint).onSuccess { nextResult ->
                                    queueEndpoint = nextResult.endpoint
                                    queueContinuation = nextResult.continuation
                                    val items = nextResult.items
                                    val nextItems = if (items.isNotEmpty() && items.first().id == vid) items.drop(1) else items
                                    upNextSongs = nextItems
                                }
                            }
                        }
                    }
                    
                    // Refetch more songs when queue gets low for infinite playback
                    LaunchedEffect(upNextSongs.size) {
                        if (upNextSongs.size <= 3 && queueEndpoint != null && queueContinuation != null) {
                            withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.echo.innertube.YouTube.next(queueEndpoint!!, queueContinuation).onSuccess { nextResult ->
                                    queueEndpoint = nextResult.endpoint
                                    queueContinuation = nextResult.continuation
                                    val existingIds = upNextSongs.map { it.id }.toSet()
                                    val newSongs = nextResult.items.filter { it.id !in existingIds }
                                    if (newSongs.isNotEmpty()) {
                                        upNextSongs = upNextSongs + newSongs
                                    }
                                }
                            }
                        }
                    }

                    val skipNextFun: () -> Unit = {
                        if (playerState != null && playerState!!.queue.isNotEmpty()) {
                            val next = playerState!!.queue.first()
                            songHistory.add(playerState!!)
                            playSong(PlayerState(
                                title = next.title,
                                artist = next.artist,
                                artUrl = next.artUrl,
                                videoId = next.videoId,
                                queue = playerState!!.queue.drop(1)
                            ))
                        } else if (upNextSongs.isNotEmpty()) {
                            val next = upNextSongs.first()
                            songHistory.add(playerState!!)
                            val upgradedArt = next.thumbnail?.let {
                                if (it.contains("=w")) it.substringBefore("=w") + "=w1200-h1200-l90-rj"
                                else if (it.contains("ytimg.com/vi/")) it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                else it
                            } ?: next.thumbnail
                            upNextSongs = upNextSongs.drop(1)
                            playSong(PlayerState(
                                title = next.title,
                                artist = next.artists.joinToString { it.name },
                                artUrl = upgradedArt,
                                videoId = next.id
                            ))
                        }
                    }

                    val skipPreviousFun: () -> Unit = {
                        if (songHistory.isNotEmpty()) {
                            val prev = songHistory.removeLast()
                            playSong(prev)
                        }
                    }

                    // Auto-play next song when current song ends
                    val songEndedCount by musicPlayer!!.songEnded.collectAsState()
                    LaunchedEffect(songEndedCount) {
                        if (songEndedCount > 0 && playerState != null) {
                            skipNextFun()
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Black
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black).nestedScroll(nestedScrollConnection)) {
                            GlassContainer(
                                modifier = Modifier.fillMaxSize().background(Color.Black),
                                content = {                                     // Combine state to trigger AnimatedContent
                                    data class NavState(val tab: Int, val playlist: Playlist?, val album: AlbumState?, val artist: ArtistState?, val videoId: String?, val category: com.mrtdk.liquid_glass.ui.screens.SearchCategory?)
                                    val currentNav = NavState(selectedIndex, playlistDetail, albumDetail, artistDetail, videoDetail, categoryDetail)
                                    
                                    androidx.compose.animation.AnimatedContent(
                                        targetState = currentNav,
                                        modifier = Modifier.fillMaxSize().background(Color.Black),
                                        transitionSpec = {
                                            if ((targetState.playlist != null && initialState.playlist == null) || 
                                                (targetState.album != null && initialState.album == null) || 
                                                (targetState.artist != null && initialState.artist == null) ||
                                                (targetState.category != null && initialState.category == null)) {
                                                // Forward (Slide in from right) — fast, snappy transition
                                                androidx.compose.animation.slideInHorizontally(
                                                    animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                                ) { width -> width } + androidx.compose.animation.fadeIn(
                                                    animationSpec = androidx.compose.animation.core.tween(200)
                                                ) togetherWith androidx.compose.animation.slideOutHorizontally(
                                                    animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                                ) { width -> -width / 4 } + androidx.compose.animation.fadeOut(
                                                    animationSpec = androidx.compose.animation.core.tween(150)
                                                )
                                            } else if ((initialState.playlist != null && targetState.playlist == null) || 
                                                     (initialState.album != null && targetState.album == null) || 
                                                     (initialState.artist != null && targetState.artist == null) ||
                                                     (initialState.category != null && targetState.category == null)) {
                                                // Backward (Slide out to right) — fast, snappy transition
                                                androidx.compose.animation.slideInHorizontally(
                                                    animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                                ) { width -> -width / 4 } + androidx.compose.animation.fadeIn(
                                                    animationSpec = androidx.compose.animation.core.tween(200)
                                                ) togetherWith androidx.compose.animation.slideOutHorizontally(
                                                    animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                                ) { width -> width } + androidx.compose.animation.fadeOut(
                                                    animationSpec = androidx.compose.animation.core.tween(150)
                                                )
                                            } else {
                                                // Tab switch (Fast crossfade)
                                                androidx.compose.animation.fadeIn(
                                                    animationSpec = androidx.compose.animation.core.tween(150)
                                                ) togetherWith androidx.compose.animation.fadeOut(
                                                    animationSpec = androidx.compose.animation.core.tween(150)
                                                )
                                            }
                                        },
                                        label = "ios_page_transition"
                                    ) { state ->
                                        when {
                                            state.videoId != null -> {
                                                VideoPlayerScreen(
                                                    videoId = state.videoId,
                                                    onBack = { 
                                                        videoDetail = null
                                                        // Resume music if it was playing? 
                                                        // For now just stop video.
                                                    }
                                                )
                                            }
                                            state.playlist != null -> {
                                            PlaylistDetailScreen(
                                                playlist = state.playlist,
                                                onBack = { playlistDetail = null },
                                                onSongSelected = playSong,
                                                onArtistSelected = {
                                                    playlistDetail = null // Close playlist to show artist screen? No, actually just show the screen! But artistDetail and playlistDetail are mutually exclusive in `NavState`? Wait! Let's check `NavState`.
                                                    artistDetail = it
                                                }
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
                                                },
                                                onArtistSelected = { artist ->
                                                    artistDetail = artist
                                                },
                                                onVideoSelected = { videoId ->
                                                    musicPlayer?.pause()
                                                    videoDetail = videoId
                                                }
                                            )
                                        }
                                        state.category != null -> {
                                            com.mrtdk.liquid_glass.ui.screens.CategoriaScreen(
                                                category = state.category,
                                                innerPadding = innerPadding,
                                                onBack = { categoryDetail = null },
                                                onSongSelected = playSong,
                                                onAlbumSelected = { album ->
                                                    albumDetail = album
                                                },
                                                onPlaylistSelected = { playlist ->
                                                    albumDetail = playlist
                                                },
                                                onArtistSelected = { artist ->
                                                    artistDetail = artist
                                                }
                                            )
                                        }
                                        else -> {
                                            when (state.tab) {
                                                0 -> InicioScreen(
                                                    innerPadding = innerPadding,
                                                    playerState = playerState,
                                                    state = inicioState,
                                                    onSongSelected = playSong,
                                                    onArtistSelected = { artistDetail = it },
                                                    onAlbumSelected = { albumDetail = it },
                                                    onVideoSelected = { videoId ->
                                                        musicPlayer?.pause()
                                                        videoDetail = videoId
                                                    }
                                                )
                                                1 -> NovedadesScreen(
                                                    innerPadding = innerPadding,
                                                    state = novedadesState,
                                                    onSongSelected = playSong,
                                                    onAlbumSelected = { albumDetail = it },
                                                    onVideoSelected = { videoId ->
                                                        musicPlayer?.pause()
                                                        videoDetail = videoId
                                                    }
                                                )
                                                3 -> BibliotecaScreen(
                                                    innerPadding = innerPadding, 
                                                    onSongSelected = playSong,
                                                    onPlaylistSelected = { playlistDetail = it },
                                                    onArtistSelected = { artistDetail = it }
                                                )
                                                4 -> BusquedaScreen(
                                                    innerPadding = innerPadding,
                                                    query = searchQuery,
                                                    isSubmitted = isSearchSubmitted,
                                                    state = busquedaState,
                                                    onSongSelected = playSong,
                                                    onArtistSelected = { artist ->
                                                        artistDetail = artist
                                                    },
                                                    onAlbumSelected = { album ->
                                                        albumDetail = album
                                                    },
                                                    onVideoSelected = { videoId ->
                                                        musicPlayer?.pause()
                                                        videoDetail = videoId
                                                    },
                                                    onCategorySelected = { category ->
                                                        categoryDetail = category
                                                    }
                                                )
                                                2 -> com.mrtdk.liquid_glass.ui.screens.RadioScreen(
                                                    innerPadding = innerPadding,
                                                    onSearchResult = { recognizedText ->
                                                        searchQuery = recognizedText
                                                        isSearchSubmitted = true
                                                        selectedIndex = 4 // Navigate to search screen
                                                    }
                                                )
                                                else -> DemoBackground(innerPadding)
                                            }
                                        }
                                    }
                                    }
                                },
                                glassContent = {
                                    if (videoDetail == null) {
                                        val scope = this
                                        val imeBottom = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
                                        val isKeyboardOpen = imeBottom > 0
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(innerPadding)
                                                .consumeWindowInsets(innerPadding)
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

                                            val bottomPad = if (isKeyboardOpen) 2.dp else 16.dp
                                            val navBarHeightWithPadding = 72.dp + bottomPad

                                            val mpPadStart by androidx.compose.animation.core.animateDpAsState(if (isCollapsed) 80.dp else 16.dp, springSpec, label = "mpPadStart")
                                            val mpPadEnd by androidx.compose.animation.core.animateDpAsState(if (isCollapsed) 80.dp else 16.dp, springSpec, label = "mpPadEnd")
                                            val mpPadBottom by androidx.compose.animation.core.animateDpAsState(if (isCollapsed) bottomPad else navBarHeightWithPadding + 12.dp, springSpec, label = "mpPadBottom")

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
                                                        onTogglePlayPause = { 
                                                            if (duration <= 0L && playerState != null) {
                                                                val state = playerState!!
                                                                if (state.contentUri != null) musicPlayer?.playLocalSong(state.contentUri)
                                                                else if (state.videoId != null) musicPlayer?.playOnlineSong(state.videoId, state.title, state.artist, state.artUrl?.toString())
                                                            } else {
                                                                musicPlayer?.togglePlayPause() 
                                                            }
                                                        },
                                                        onClick = { if (playerState != null) showPlayer = true },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        hideImage = showPlayer,
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
                                                            },
                                                            shape = CircleShape,
                                                            tint = globalDominantColor.copy(alpha = 0.35f),
                                                            blur = 0.8f, centerDistortion = 0.1f, scale = 0.02f, warpEdges = 0.4f, elevation = 4.dp
                                                        ) {
                                                            Box(modifier=Modifier.fillMaxSize(), contentAlignment=Alignment.Center) {
                                                                Icon(androidx.compose.ui.res.painterResource(id = R.drawable.nav_inicio), contentDescription="Home", tint = Color(0xFFFA243C), modifier = Modifier.size(28.dp))
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
                                                                Icon(Icons.Default.Search, contentDescription="Search", tint = contentTintColor, modifier = Modifier.size(32.dp))
                                                            }
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
                                upNextSongs = upNextSongs,
                                onUpNextSongsChange = { upNextSongs = it },
                                songHistory = songHistory,
                                onSkipNext = skipNextFun,
                                onSkipPrevious = skipPreviousFun,
                                onClose = { showPlayer = false },
                                onTogglePlayPause = { 
                                    if (duration <= 0L && playerState != null) {
                                        val state = playerState!!
                                        if (state.contentUri != null) musicPlayer?.playLocalSong(state.contentUri)
                                        else if (state.videoId != null) musicPlayer?.playOnlineSong(state.videoId, state.title, state.artist, state.artUrl?.toString())
                                    } else {
                                        musicPlayer?.togglePlayPause() 
                                    }
                                },
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