package com.mrtdk.liquid_glass

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mrtdk.liquid_glass.playback.MusicPlayer
import com.mrtdk.liquid_glass.ui.LiquidBottomNavBar
import com.mrtdk.liquid_glass.ui.components.MiniPlayer
import com.mrtdk.liquid_glass.ui.components.LocalBackdrop
import com.mrtdk.liquid_glass.ui.components.SharedElementTransitionContainer
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.mrtdk.liquid_glass.ui.screens.AlbumScreen
import com.mrtdk.liquid_glass.ui.screens.AlbumState
import com.mrtdk.liquid_glass.ui.screens.PlaylistDetailScreen
import com.mrtdk.liquid_glass.ui.screens.FavoriteSongsScreen
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
    private var navigateToDownloads by androidx.compose.runtime.mutableStateOf(false)
    private var initialLibraryCategory by androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val showDownloads = intent.getBooleanExtra("navigate_to_downloads", false)
        if (showDownloads) {
            navigateToDownloads = true
            initialLibraryCategory = "Descargados"
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        com.mrtdk.liquid_glass.utils.LocaleUtils.applyLocale(this)
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Liquidglassuicomponent)
        enableEdgeToEdge()
        musicPlayer = MusicPlayer(this)
        com.mrtdk.liquid_glass.data.LibraryManager.init(applicationContext)

        val showDownloads = intent.getBooleanExtra("navigate_to_downloads", false)
        if (showDownloads) {
            navigateToDownloads = true
            initialLibraryCategory = "Descargados"
        }

        setContent {
            LiquidglassuicomponentTheme {
                val context = LocalContext.current

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermissionState = rememberPermissionState(
                        permission = Manifest.permission.POST_NOTIFICATIONS
                    )
                    LaunchedEffect(Unit) {
                        if (!notificationPermissionState.status.isGranted) {
                            notificationPermissionState.launchPermissionRequest()
                        }
                    }
                }

                var selectedIndex by remember { mutableIntStateOf(com.mrtdk.liquid_glass.data.LibraryManager.getLastTab()) }
                LaunchedEffect(selectedIndex) {
                    com.mrtdk.liquid_glass.data.LibraryManager.saveLastTab(selectedIndex)
                }
                LaunchedEffect(navigateToDownloads) {
                    if (navigateToDownloads) {
                        selectedIndex = 3
                        navigateToDownloads = false
                    }
                }
                var glassStyle by remember { mutableStateOf(LibraryManager.getGlassStyle()) }
                var playerState by remember { mutableStateOf<PlayerState?>(null) }
                var showPlayer by remember { mutableStateOf(false) }
                    var upNextSongs by remember { mutableStateOf<List<com.echo.innertube.models.SongItem>>(emptyList()) }
                    var queueSeedVideoId by remember { mutableStateOf<String?>(null) }
                    var queueContinuation by remember { mutableStateOf<String?>(null) }
                    var queueEndpoint by remember { mutableStateOf<com.echo.innertube.models.WatchEndpoint?>(null) }
                    val songHistory = remember { androidx.compose.runtime.mutableStateListOf<PlayerState>() }

                    LaunchedEffect(Unit) {
                        val lastState = com.mrtdk.liquid_glass.data.LibraryManager.getLastPlayerState()
                        playerState = lastState
                        com.mrtdk.liquid_glass.playback.PlaybackQueue.currentSong = lastState
                        if (lastState != null) {
                            com.mrtdk.liquid_glass.playback.PlaybackQueue.queue = lastState.queue
                            com.mrtdk.liquid_glass.playback.PlaybackQueue.isExclusiveQueue = lastState.isExclusiveQueue
                        }

                        com.mrtdk.liquid_glass.playback.PlaybackQueue.onCurrentSongChanged = { newSong ->
                            playerState = newSong
                        }
                        com.mrtdk.liquid_glass.playback.PlaybackQueue.onQueueChanged = {
                            upNextSongs = com.mrtdk.liquid_glass.playback.PlaybackQueue.upNextSongs
                            queueSeedVideoId = com.mrtdk.liquid_glass.playback.PlaybackQueue.queueSeedVideoId
                            queueContinuation = com.mrtdk.liquid_glass.playback.PlaybackQueue.queueContinuation
                            queueEndpoint = com.mrtdk.liquid_glass.playback.PlaybackQueue.queueEndpoint
                            songHistory.clear()
                            songHistory.addAll(com.mrtdk.liquid_glass.playback.PlaybackQueue.songHistory)
                        }
                    }

                    LaunchedEffect(playerState) {
                        com.mrtdk.liquid_glass.data.LibraryManager.saveLastPlayerState(playerState)
                    }
                    
                    var searchQuery by remember { mutableStateOf("") }
                    var isSearchSubmitted by remember { mutableStateOf(false) }
                    
                    var updateReleaseInfo by remember { mutableStateOf<com.mrtdk.liquid_glass.utils.Updater.ReleaseInfo?>(null) }
                    LaunchedEffect(Unit) {
                        com.mrtdk.liquid_glass.utils.Updater.checkUpdate { info ->
                            if (info != null) {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    updateReleaseInfo = info
                                }
                            }
                        }
                    }

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
                    var showFavoriteSongs by remember { mutableStateOf(false) }

                    // Handle system back navigation
                    androidx.activity.compose.BackHandler(
                        enabled = showPlayer || videoDetail != null || playlistDetail != null || albumDetail != null || artistDetail != null || categoryDetail != null || showFavoriteSongs || (selectedIndex == 4 && isSearchSubmitted) || selectedIndex != 0
                    ) {
                        when {
                            showPlayer -> showPlayer = false
                            showFavoriteSongs -> showFavoriteSongs = false
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
                                    url.contains("=w") || url.contains("=s") -> {
                                        val idx = url.indexOf("=w").takeIf { j -> j != -1 } ?: url.indexOf("=s")
                                        url.substring(0, idx) + "=w300-h300-rj"
                                    }
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
                                    LibraryManager.currentDominantColor.value = sampledColor
                                    contentTintColor = Color.White
                                } catch (e: Exception) { }
                            }
                        } else {
                            globalDominantColor = Color.White.copy(alpha = 0.15f)
                            LibraryManager.currentDominantColor.value = Color.White.copy(alpha = 0.15f)
                            contentTintColor = Color.White
                        }
                    }

                    val isPlaying by musicPlayer!!.isPlaying.collectAsState()
                    val currentPosition by musicPlayer!!.currentPosition.collectAsState()
                    val duration by musicPlayer!!.duration.collectAsState()
                    val shuffleModeEnabled by musicPlayer!!.shuffleModeEnabled.collectAsState()
                    val repeatMode by musicPlayer!!.repeatMode.collectAsState()

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
                    val playSongInternal: (PlayerState, Boolean) -> Unit = { state, keepQueue ->
                        playerState = state
                        showPlayer = true
                        
                        if (!keepQueue) {
                            // Reset autoplay recommendation queue and continuation details
                            upNextSongs = emptyList()
                            queueSeedVideoId = state.videoId
                            queueContinuation = null
                            queueEndpoint = null
                            
                            com.mrtdk.liquid_glass.playback.PlaybackQueue.upNextSongs = emptyList()
                            com.mrtdk.liquid_glass.playback.PlaybackQueue.queueSeedVideoId = state.videoId
                            com.mrtdk.liquid_glass.playback.PlaybackQueue.queueContinuation = null
                            com.mrtdk.liquid_glass.playback.PlaybackQueue.queueEndpoint = null
                        } else {
                            // If keeping queue, seed video ID is still the new song
                            queueSeedVideoId = state.videoId
                            com.mrtdk.liquid_glass.playback.PlaybackQueue.queueSeedVideoId = state.videoId
                        }
                        
                        com.mrtdk.liquid_glass.playback.PlaybackQueue.currentSong = state
                        com.mrtdk.liquid_glass.playback.PlaybackQueue.queue = state.queue
                        com.mrtdk.liquid_glass.playback.PlaybackQueue.isExclusiveQueue = state.isExclusiveQueue
                        
                        com.mrtdk.liquid_glass.playback.PlaybackQueue.songHistory.clear()
                        com.mrtdk.liquid_glass.playback.PlaybackQueue.songHistory.addAll(songHistory)
                        songHistory.clear()

                        com.mrtdk.liquid_glass.playback.PlaybackQueue.onQueueChanged?.invoke()

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
                        if (state.contentUri != null) musicPlayer?.playLocalSong(state.contentUri, state.title, state.artist, state.artUrl?.toString())
                        else if (state.videoId != null) musicPlayer?.playOnlineSong(state.videoId, state.title, state.artist, state.artUrl?.toString())
                    }

                    val playSong: (PlayerState) -> Unit = { state -> playSongInternal(state, false) }
                    val playSongFromQueue: (PlayerState) -> Unit = { state -> playSongInternal(state, true) }

                    LaunchedEffect(Unit) {
                        androidx.compose.runtime.snapshotFlow { 
                            Pair(playerState?.videoId, playerState?.isExclusiveQueue)
                        }.collect { (vid, isExclusive) ->
                            if (vid == null) return@collect
                            if (isExclusive == true) {
                                upNextSongs = emptyList()
                                com.mrtdk.liquid_glass.playback.PlaybackQueue.upNextSongs = emptyList()
                                com.mrtdk.liquid_glass.playback.PlaybackQueue.onQueueChanged?.invoke()
                                return@collect
                            }
                            if (upNextSongs.isEmpty()) {
                                queueSeedVideoId = vid
                                com.mrtdk.liquid_glass.playback.PlaybackQueue.queueSeedVideoId = vid
                                withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val endpoint = com.echo.innertube.models.WatchEndpoint(videoId = vid)
                                    com.echo.innertube.YouTube.next(endpoint).onSuccess { nextResult ->
                                        queueEndpoint = nextResult.endpoint
                                        com.mrtdk.liquid_glass.playback.PlaybackQueue.queueEndpoint = nextResult.endpoint
                                        queueContinuation = nextResult.continuation
                                        com.mrtdk.liquid_glass.playback.PlaybackQueue.queueContinuation = nextResult.continuation
                                        val items = nextResult.items
                                        val nextItems = if (items.isNotEmpty() && items.first().id == vid) items.drop(1) else items
                                        upNextSongs = nextItems
                                        com.mrtdk.liquid_glass.playback.PlaybackQueue.upNextSongs = nextItems
                                        com.mrtdk.liquid_glass.playback.PlaybackQueue.onQueueChanged?.invoke()
                                    }
                                }
                            }
                        }
                    }
                    
                    // Refetch more songs when queue gets low for infinite playback
                    LaunchedEffect(Unit) {
                        androidx.compose.runtime.snapshotFlow { upNextSongs.size }
                            .collect { size ->
                                if (size in 1..3 && queueEndpoint != null && queueContinuation != null) {
                                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        com.echo.innertube.YouTube.next(queueEndpoint!!, queueContinuation).onSuccess { nextResult ->
                                            queueEndpoint = nextResult.endpoint
                                            com.mrtdk.liquid_glass.playback.PlaybackQueue.queueEndpoint = nextResult.endpoint
                                            queueContinuation = nextResult.continuation
                                            com.mrtdk.liquid_glass.playback.PlaybackQueue.queueContinuation = nextResult.continuation
                                            val existingIds = upNextSongs.map { it.id }.toSet()
                                            val newSongs = nextResult.items.filter { it.id !in existingIds }
                                            if (newSongs.isNotEmpty()) {
                                                val updatedList = upNextSongs + newSongs
                                                upNextSongs = updatedList
                                                com.mrtdk.liquid_glass.playback.PlaybackQueue.upNextSongs = updatedList
                                                com.mrtdk.liquid_glass.playback.PlaybackQueue.onQueueChanged?.invoke()
                                            }
                                        }
                                    }
                                }
                            }
                    }

                    val skipNextFun: () -> Unit = {
                        val nextState = com.mrtdk.liquid_glass.playback.PlaybackQueue.getNextSongAndAdvance()
                        if (nextState != null) {
                            if (nextState.contentUri != null) musicPlayer?.playLocalSong(nextState.contentUri, nextState.title, nextState.artist, nextState.artUrl?.toString())
                            else if (nextState.videoId != null) musicPlayer?.playOnlineSong(nextState.videoId, nextState.title, nextState.artist, nextState.artUrl?.toString())
                        }
                    }

                    val skipPreviousFun: () -> Unit = {
                        val prevState = com.mrtdk.liquid_glass.playback.PlaybackQueue.getPreviousSongAndGoBack()
                        if (prevState != null) {
                            if (prevState.contentUri != null) musicPlayer?.playLocalSong(prevState.contentUri, prevState.title, prevState.artist, prevState.artUrl?.toString())
                            else if (prevState.videoId != null) musicPlayer?.playOnlineSong(prevState.videoId, prevState.title, prevState.artist, prevState.artUrl?.toString())
                        }
                    }

                    CompositionLocalProvider(com.mrtdk.glass.LocalGlassStyle provides glassStyle) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Black
                        ) { innerPadding ->
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black).nestedScroll(nestedScrollConnection)) {
                                val mainBackdrop = rememberLayerBackdrop()
                                GlassContainer(
                                    modifier = Modifier.fillMaxSize().background(Color.Black),
                                    useShader = (videoDetail == null),
                                    content = {
                                    // Only tab index is tracked in AnimatedContent
                                    // Album, playlist, artist, category, video are overlays using SharedElementTransitionContainer or direct overlays
                                    Box(modifier = Modifier.fillMaxSize().layerBackdrop(mainBackdrop)) {
                                        androidx.compose.animation.AnimatedContent(
                                            targetState = selectedIndex,
                                            modifier = Modifier.fillMaxSize().background(Color.Black),
                                            transitionSpec = {
                                                // Fast crossfade for tab switches
                                                androidx.compose.animation.fadeIn(
                                                    animationSpec = androidx.compose.animation.core.tween(150)
                                                ) togetherWith androidx.compose.animation.fadeOut(
                                                    animationSpec = androidx.compose.animation.core.tween(150)
                                                )
                                            },
                                            label = "ios_page_transition"
                                        ) { tabIndex ->
                                            when (tabIndex) {
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
                                                    onArtistSelected = { artistDetail = it },
                                                    onAlbumSelected = { albumDetail = it },
                                                    initialCategoryKey = initialLibraryCategory,
                                                    onCategoryConsumed = { initialLibraryCategory = null },
                                                    onGlassStyleChanged = { glassStyle = it },
                                                    onFavoriteSongsSelected = { showFavoriteSongs = true }
                                                )
                                                4 -> BusquedaScreen(
                                                    innerPadding = innerPadding,
                                                    query = searchQuery,
                                                    isSubmitted = isSearchSubmitted,
                                                    state = busquedaState,
                                                    onSongSelected = playSong,
                                                    onArtistSelected = { artist -> artistDetail = artist },
                                                    onAlbumSelected = { album -> albumDetail = album },
                                                    onVideoSelected = { videoId ->
                                                        musicPlayer?.pause()
                                                        videoDetail = videoId
                                                    },
                                                    onCategorySelected = { category -> categoryDetail = category }
                                                )
                                                2 -> com.mrtdk.liquid_glass.ui.screens.RadioScreen(
                                                    innerPadding = innerPadding,
                                                    onSearchResult = { recognizedText ->
                                                        searchQuery = recognizedText
                                                        isSearchSubmitted = true
                                                        selectedIndex = 4
                                                    }
                                                )
                                                else -> DemoBackground(innerPadding)
                                            }
                                        }

                                         if (artistDetail != null) {
                                             SharedElementTransitionContainer(onBack = { artistDetail = null }, shrinkToTarget = false, enableSwipeToDismiss = false) { _, _ ->
                                                 ArtistScreen(
                                                     artistState = artistDetail!!,
                                                     innerPadding = innerPadding,
                                                     onBack = { artistDetail = null },
                                                     onSongSelected = playSong,
                                                     onAlbumSelected = { album -> albumDetail = album },
                                                     onArtistSelected = { artist -> artistDetail = artist },
                                                     onVideoSelected = { videoId ->
                                                         musicPlayer?.pause()
                                                         videoDetail = videoId
                                                     }
                                                 )
                                             }
                                         }

                                         if (albumDetail != null) {
                                             SharedElementTransitionContainer(onBack = { albumDetail = null }, enableSwipeToDismiss = false) { _, _ ->
                                                 AlbumScreen(
                                                     albumState = albumDetail!!,
                                                     onBack = { albumDetail = null },
                                                     onSongSelected = playSong,
                                                     onArtistSelected = { artist -> artistDetail = artist },
                                                     onAlbumSelected = { album -> albumDetail = album },
                                                     onDominantColorChanged = { color -> globalDominantColor = color },
                                                     isPaused = showPlayer
                                                 )
                                             }
                                         }

                                         if (playlistDetail != null) {
                                             SharedElementTransitionContainer(onBack = { playlistDetail = null }) { _, _ ->
                                                 PlaylistDetailScreen(
                                                     playlist = playlistDetail!!,
                                                     onBack = { playlistDetail = null },
                                                     onSongSelected = playSong,
                                                     onArtistSelected = { artistDetail = it }
                                                 )
                                             }
                                         }

                                         androidx.compose.animation.AnimatedVisibility(
                                             visible = categoryDetail != null,
                                             enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(100)),
                                             exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(80))
                                         ) {
                                             val cat = categoryDetail
                                             if (cat != null) {
                                                 androidx.activity.compose.BackHandler { categoryDetail = null }
                                                 com.mrtdk.liquid_glass.ui.screens.CategoriaScreen(
                                                     category = cat,
                                                     innerPadding = innerPadding,
                                                     onBack = { categoryDetail = null },
                                                     onSongSelected = playSong,
                                                     onAlbumSelected = { album -> albumDetail = album },
                                                     onPlaylistSelected = { playlist -> albumDetail = playlist },
                                                     onArtistSelected = { artist -> artistDetail = artist }
                                                 )
                                             }
                                         }

                                         if (showFavoriteSongs) {
                                             SharedElementTransitionContainer(onBack = { showFavoriteSongs = false }) { _, _ ->
                                                 FavoriteSongsScreen(
                                                     onBack = { showFavoriteSongs = false },
                                                     onSongSelected = playSong
                                                 )
                                             }
                                         }
                                    }
                                    },
                                    glassContent = {
                                    if (videoDetail == null) {
                                        val scope = this
                                        CompositionLocalProvider(LocalBackdrop provides mainBackdrop) {
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
                                                            selectedIndex = selectedIndex,
                                                            tintColor = globalDominantColor.copy(alpha = 0.35f),
                                                            contentColor = contentTintColor,
                                                            onTabSelected = { newIndex ->
                                                                artistDetail = null
                                                                albumDetail = null
                                                                playlistDetail = null
                                                                categoryDetail = null
                                                                videoDetail = null
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
                                                                artistDetail = null
                                                                albumDetail = null
                                                                playlistDetail = null
                                                                categoryDetail = null
                                                                videoDetail = null
                                                            },
                                                            onSearchSubmit = { 
                                                                isSearchSubmitted = true
                                                                artistDetail = null
                                                                albumDetail = null
                                                                playlistDetail = null
                                                                categoryDetail = null
                                                                videoDetail = null
                                                            }
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
                                                    MiniPlayer(
                                                        playerState = playerState,
                                                        isPlaying = isPlaying,
                                                        onTogglePlayPause = { 
                                                            if (duration <= 0L && playerState != null) {
                                                                val state = playerState!!
                                                                if (state.contentUri != null) musicPlayer?.playLocalSong(state.contentUri, state.title, state.artist, state.artUrl?.toString())
                                                                else if (state.videoId != null) musicPlayer?.playOnlineSong(state.videoId, state.title, state.artist, state.artUrl?.toString())
                                                            } else {
                                                                musicPlayer?.togglePlayPause() 
                                                            }
                                                        },
                                                        onClick = { if (playerState != null) showPlayer = true },
                                                        onNext = skipNextFun,
                                                        onPrevious = skipPreviousFun,
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
                                                        Box(
                                                            modifier = Modifier
                                                                .size(56.dp)
                                                                .drawBackdrop(
                                                                    backdrop = mainBackdrop,
                                                                    shape = { CircleShape },
                                                                    effects = {
                                                                        vibrancy()
                                                                        blur(8f.dp.toPx())
                                                                        lens(24f.dp.toPx(), 24f.dp.toPx())
                                                                    },
                                                                    onDrawSurface = { drawRect(globalDominantColor.copy(alpha = 0.45f)) }
                                                                )
                                                                .clip(CircleShape)
                                                                .clickable { 
                                                                    isBottomBarCollapsed = false
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.nav_inicio),
                                                                contentDescription = "Home",
                                                                tint = Color(0xFFFA243C),
                                                                modifier = Modifier.size(28.dp)
                                                            )
                                                        }
                                                        
                                                        Box(
                                                            modifier = Modifier
                                                                .size(56.dp)
                                                                .drawBackdrop(
                                                                    backdrop = mainBackdrop,
                                                                    shape = { CircleShape },
                                                                    effects = {
                                                                        vibrancy()
                                                                        blur(8f.dp.toPx())
                                                                        lens(24f.dp.toPx(), 24f.dp.toPx())
                                                                    },
                                                                    onDrawSurface = { drawRect(globalDominantColor.copy(alpha = 0.45f)) }
                                                                )
                                                                .clip(CircleShape)
                                                                .clickable { 
                                                                    isBottomBarCollapsed = false
                                                                    selectedIndex = 4 
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Search,
                                                                contentDescription = "Search",
                                                                tint = contentTintColor,
                                                                modifier = Modifier.size(32.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        } // end CompositionLocalProvider(LocalBackdrop)
                                    }
                                }
                            )



                            // ── Apple Music expand overlay: Video Player ────────────────────────
                            if (videoDetail != null) {
                                VideoPlayerScreen(
                                    videoId = videoDetail!!,
                                    onBack = { videoDetail = null }
                                )
                            }

                            PlayerScreen(
                                playerState = playerState,
                                isVisible = showPlayer,
                                onDominantColorChanged = { color ->
                                    globalDominantColor = color
                                },
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                duration = duration,
                                isBottomBarCollapsed = isBottomBarCollapsed,
                                upNextSongs = upNextSongs,
                                onUpNextSongsChange = { 
                                    upNextSongs = it 
                                    com.mrtdk.liquid_glass.playback.PlaybackQueue.upNextSongs = it
                                },
                                songHistory = songHistory,
                                onSkipNext = skipNextFun,
                                onSkipPrevious = skipPreviousFun,
                                onClose = { showPlayer = false },
                                onTogglePlayPause = { 
                                    if (duration <= 0L && playerState != null) {
                                        val state = playerState!!
                                        if (state.contentUri != null) musicPlayer?.playLocalSong(state.contentUri, state.title, state.artist, state.artUrl?.toString())
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
                                onAlbumSelected = { album ->
                                    showPlayer = false
                                    albumDetail = album
                                },
                                onSongSelected = playSong,
                                onSongSelectedFromQueue = playSongFromQueue,
                                shuffleModeEnabled = shuffleModeEnabled,
                                repeatMode = repeatMode,
                                onToggleShuffle = { musicPlayer?.setShuffleModeEnabled(!shuffleModeEnabled) },
                                onToggleRepeat = {
                                    val nextMode = when (repeatMode) {
                                        androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                        androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                        androidx.media3.common.Player.REPEAT_MODE_ONE -> androidx.media3.common.Player.REPEAT_MODE_OFF
                                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                                    }
                                    musicPlayer?.setRepeatMode(nextMode)
                                }
                            )
                        }
                    }
                }
                    
                    updateReleaseInfo?.let { info ->
                        com.mrtdk.liquid_glass.ui.components.UpdateDialog(
                            releaseInfo = info,
                            onDismiss = { updateReleaseInfo = null }
                        )
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