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
import kotlinx.coroutines.Dispatchers
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.layout
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.mrtdk.liquid_glass.ui.components.UpdateDialog
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
import com.mrtdk.liquid_glass.ui.screens.ReplayScreen
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
        
        // Initial setup for screenshots security
        val disableSec = com.mrtdk.liquid_glass.data.LibraryManager.getString("disable_screenshot", "false") == "true"
        if (disableSec) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        
        // Request highest refresh rate (90Hz/120Hz+) if supported by display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val modes = display?.supportedModes
                val activeMode = display?.mode
                if (modes != null && activeMode != null) {
                    val maxRefresh = modes.maxOfOrNull { it.refreshRate } ?: 60f
                    if (maxRefresh > 60f) {
                        val bestMode = modes.filter { it.physicalWidth == activeMode.physicalWidth && it.physicalHeight == activeMode.physicalHeight }
                            .maxByOrNull { it.refreshRate }
                        if (bestMode != null) {
                            val lp = window.attributes
                            lp.preferredDisplayModeId = bestMode.modeId
                            window.attributes = lp
                        }
                    }
                }
            } catch (e: Exception) { }
        }

        // Configure global Coil ImageLoader with memory and disk caches
        val globalImageLoader = coil.ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .allowHardware(true)
            .crossfade(true)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(coil.decode.ImageDecoderDecoder.Factory())
                } else {
                    add(coil.decode.GifDecoder.Factory())
                }
                add(com.mrtdk.liquid_glass.utils.CoilUtils.HdThumbnailInterceptor())
            }
            .build()
        coil.Coil.setImageLoader(globalImageLoader)
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

                var pendingJoinRequest by remember { mutableStateOf<Pair<String, String>?>(null) }
                val newLtManager = remember { com.mrtdk.liquid_glass.listentogether.ListenTogetherManager.getInstance(context) }

                LaunchedEffect(newLtManager) {
                    newLtManager.client.events.collect { event ->
                        if (event is com.mrtdk.liquid_glass.listentogether.ListenTogetherEvent.JoinRequestReceived) {
                            pendingJoinRequest = Pair(event.userId, event.username)
                        }
                    }
                }

                if (pendingJoinRequest != null) {
                    val activeReq = pendingJoinRequest!!
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {
                            newLtManager.client.rejectJoin(activeReq.first, "Rechazado por el usuario")
                            pendingJoinRequest = null
                        },
                        title = { Text("Solicitud de ingreso", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                        text = { Text("El usuario ${activeReq.second} quiere unirse a tu sala.", color = Color.White) },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    newLtManager.client.approveJoin(activeReq.first)
                                    pendingJoinRequest = null
                                }
                            ) {
                                Text("Aceptar", color = Color(0xFFFF2D55), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    newLtManager.client.rejectJoin(activeReq.first, "Rechazado por el usuario")
                                    pendingJoinRequest = null
                                }
                            ) {
                                Text("Rechazar", color = Color(0xFF8E8E93))
                            }
                        },
                        containerColor = Color(0xFF1C1C1E)
                    )
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
                val lastSavedState = remember { com.mrtdk.liquid_glass.data.LibraryManager.getLastPlayerState() }
                var playerState by remember { mutableStateOf<PlayerState?>(lastSavedState) }
                var isFirstStateLoad by remember { mutableStateOf(true) }
                var showPlayer by remember { mutableStateOf(false) }
                    var upNextSongs by remember { mutableStateOf<List<com.echo.innertube.models.SongItem>>(emptyList()) }
                    var queueSeedVideoId by remember { mutableStateOf<String?>(null) }
                    var queueContinuation by remember { mutableStateOf<String?>(null) }
                    var queueEndpoint by remember { mutableStateOf<com.echo.innertube.models.WatchEndpoint?>(null) }
                    val songHistory = remember { androidx.compose.runtime.mutableStateListOf<PlayerState>() }
 
                    LaunchedEffect(Unit) {
                        com.mrtdk.liquid_glass.playback.PlaybackQueue.currentSong = lastSavedState
                        if (lastSavedState != null) {
                            com.mrtdk.liquid_glass.playback.PlaybackQueue.queue = lastSavedState.queue
                            com.mrtdk.liquid_glass.playback.PlaybackQueue.isExclusiveQueue = lastSavedState.isExclusiveQueue
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
                        if (playerState != null) {
                            if (isFirstStateLoad && playerState == lastSavedState) {
                                isFirstStateLoad = false
                            } else {
                                isFirstStateLoad = false
                                val pauseHistory = com.mrtdk.liquid_glass.data.LibraryManager.getString("pause_listen_history", "false") == "true"
                                if (!pauseHistory) {
                                    // Track recently played
                                    com.mrtdk.liquid_glass.data.LibraryManager.addRecentlyPlayed(
                                        com.mrtdk.liquid_glass.data.LibraryItem(
                                            id = playerState!!.videoId ?: playerState!!.title,
                                            title = playerState!!.title,
                                            subtitle = playerState!!.artist,
                                            thumbnail = playerState!!.artUrl?.toString(),
                                            type = com.mrtdk.liquid_glass.data.ItemType.SONG,
                                            album = playerState!!.album
                                        )
                                    )
                                    // Track in complete playback history
                                    com.mrtdk.liquid_glass.data.LibraryManager.addPlaybackRecord(
                                        songId = playerState!!.videoId ?: playerState!!.title,
                                        title = playerState!!.title,
                                        artist = playerState!!.artist,
                                        thumbnail = playerState!!.artUrl?.toString(),
                                        album = playerState!!.album,
                                        playlistId = playerState!!.playlistId,
                                        playlistName = playerState!!.playlistName
                                    )
                                }
                            }
                        }
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
                    var showReplay by remember { mutableStateOf(false) }
                    var showListenTogether by remember { mutableStateOf(false) }
                    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

                    LaunchedEffect(showPlayer, videoDetail, artistDetail, albumDetail, playlistDetail) {
                        if (showPlayer || videoDetail != null || artistDetail != null || albumDetail != null || playlistDetail != null) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    }

                    // Handle system back navigation
                    androidx.activity.compose.BackHandler(
                        enabled = showPlayer || showReplay || showListenTogether || videoDetail != null || playlistDetail != null || albumDetail != null || artistDetail != null || categoryDetail != null || showFavoriteSongs || (selectedIndex == 4 && isSearchSubmitted) || selectedIndex != 0
                    ) {
                        when {
                            videoDetail != null -> videoDetail = null
                            playlistDetail != null -> playlistDetail = null
                            albumDetail != null -> albumDetail = null
                            artistDetail != null -> artistDetail = null
                            categoryDetail != null -> categoryDetail = null
                            showPlayer -> showPlayer = false
                            showReplay -> showReplay = false
                            showFavoriteSongs -> showFavoriteSongs = false
                            showListenTogether -> showListenTogether = false
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
                            withContext(Dispatchers.Default) {
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
                                    .data(hdUrl).allowHardware(false).size(80).build()
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
                                        withContext(Dispatchers.Main) {
                                            globalDominantColor = sampledColor
                                            LibraryManager.currentDominantColor.value = sampledColor
                                            contentTintColor = Color.White
                                        }
                                    } catch (e: Exception) { }
                                }
                            }
                        } else {
                            globalDominantColor = Color.White.copy(alpha = 0.15f)
                            LibraryManager.currentDominantColor.value = Color.White.copy(alpha = 0.15f)
                            contentTintColor = Color.White
                        }
                    }

                    val isPlaying by musicPlayer!!.isPlaying.collectAsState()
                    val playbackError by musicPlayer!!.playbackError.collectAsState()
                    val currentPosition by musicPlayer!!.currentPosition.collectAsState()
                    val duration by musicPlayer!!.duration.collectAsState()
                    val shuffleModeEnabled by musicPlayer!!.shuffleModeEnabled.collectAsState()
                    val repeatMode by musicPlayer!!.repeatMode.collectAsState()

                    val floatingNavBarScrollConnection = com.mrtdk.liquid_glass.ui.components.floatingtabbar.rememberFloatingTabBarScrollConnection()
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = 0) { 4 }
                    val mainCoroutineScope = rememberCoroutineScope()

                    val tabPositionProvider = remember(pagerState) {
                        {
                            if (pagerState.isScrollInProgress) {
                                pagerState.currentPage + pagerState.currentPageOffsetFraction
                            } else {
                                null
                            }
                        }
                    }

                    LaunchedEffect(pagerState.currentPage) {
                        if (selectedIndex != 4 && selectedIndex != pagerState.currentPage) {
                            selectedIndex = pagerState.currentPage
                        }
                    }

                    val listenTogetherManager = remember { com.mrtdk.liquid_glass.listentogether.ListenTogetherManager.getInstance(context) }

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

                        if (state.contentUri != null) musicPlayer?.playLocalSong(state.contentUri, state.title, state.artist, state.artUrl?.toString())
                        else if (state.videoId != null) musicPlayer?.playOnlineSong(state.videoId, state.title, state.artist, state.artUrl?.toString())

                        listenTogetherManager.broadcastSongChange(state)
                    }

                    val playSong: (PlayerState) -> Unit = { state ->
                        if (listenTogetherManager.onSongSelectedAttempt(state)) {
                            playSongInternal(state, false)
                        }
                    }
                    val playSongFromQueue: (PlayerState) -> Unit = { state ->
                        if (listenTogetherManager.onSongSelectedAttempt(state)) {
                            playSongInternal(state, true)
                        }
                    }

                    LaunchedEffect(listenTogetherManager) {
                        listenTogetherManager.onSongSelectedCallback = { targetState ->
                            playSongInternal(targetState, false)
                        }
                        listenTogetherManager.onTogglePlayPauseCallback = {
                            musicPlayer?.togglePlayPause()
                        }
                        listenTogetherManager.onSeekCallback = { posMs ->
                            musicPlayer?.seekTo(posMs.toLong())
                        }
                    }

                    var radioLoadingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
                    val radioScope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        androidx.compose.runtime.snapshotFlow { 
                            Pair(playerState?.videoId, playerState?.isExclusiveQueue)
                        }.collect { (vid, isExclusive) ->
                            if (vid == null) return@collect
                            if (isExclusive == true) {
                                radioLoadingJob?.cancel()
                                upNextSongs = emptyList()
                                com.mrtdk.liquid_glass.playback.PlaybackQueue.upNextSongs = emptyList()
                                com.mrtdk.liquid_glass.playback.PlaybackQueue.onQueueChanged?.invoke()
                                return@collect
                            }
                            
                            val isAutoplayEnabled = com.mrtdk.liquid_glass.data.LibraryManager.getString("autoplay_similar", "true") == "true"
                            if (isAutoplayEnabled) {
                                // If upNextSongs is already populated (user playing within the queue), do not overwrite with a new radio list
                                if (upNextSongs.isNotEmpty()) {
                                    return@collect
                                }
                                radioLoadingJob?.cancel()
                                radioLoadingJob = radioScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    queueSeedVideoId = vid
                                    com.mrtdk.liquid_glass.playback.PlaybackQueue.queueSeedVideoId = vid

                                    val endpoint = com.echo.innertube.models.WatchEndpoint(videoId = vid)
                                    var result = com.echo.innertube.YouTube.next(endpoint).getOrNull()

                                    // Fallback to RDAMVM radio playlist if primary endpoint failed or is empty
                                    if (result == null || result.items.isEmpty()) {
                                        val fallbackEndpoint = com.echo.innertube.models.WatchEndpoint(videoId = vid, playlistId = "RDAMVM$vid")
                                        result = com.echo.innertube.YouTube.next(fallbackEndpoint).getOrNull()
                                    }

                                    if (result != null) {
                                        val ep = result.endpoint
                                        val cont = result.continuation
                                        val nonVideoItems = result.items.filterNot { it.isVideoSong }
                                        val finalItems = nonVideoItems.ifEmpty { result.items }
                                        val nextItems = if (finalItems.isNotEmpty() && finalItems.first().id == vid) finalItems.drop(1) else finalItems

                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            queueEndpoint = ep
                                            com.mrtdk.liquid_glass.playback.PlaybackQueue.queueEndpoint = ep
                                            queueContinuation = cont
                                            com.mrtdk.liquid_glass.playback.PlaybackQueue.queueContinuation = cont
                                            upNextSongs = nextItems
                                            com.mrtdk.liquid_glass.playback.PlaybackQueue.upNextSongs = nextItems
                                            com.mrtdk.liquid_glass.playback.PlaybackQueue.onQueueChanged?.invoke()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Refetch more songs when queue gets low for infinite playback
                    LaunchedEffect(Unit) {
                        androidx.compose.runtime.snapshotFlow { upNextSongs.size }
                            .collect { size ->
                                val currentEp = queueEndpoint
                                val currentCont = queueContinuation
                                if (size in 1..3 && currentEp != null && currentCont != null) {
                                    val isAutoplayEnabled = com.mrtdk.liquid_glass.data.LibraryManager.getString("autoplay_similar", "true") == "true"
                                    if (isAutoplayEnabled) {
                                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            com.echo.innertube.YouTube.next(currentEp, currentCont).onSuccess { nextResult ->
                                                val newEp = nextResult.endpoint
                                                val newCont = nextResult.continuation
                                                val existingIds = upNextSongs.map { it.id }.toSet()
                                                val nonVideoNew = nextResult.items.filterNot { it.isVideoSong }
                                                val finalNew = nonVideoNew.ifEmpty { nextResult.items }
                                                val newSongs = finalNew.filter { it.id !in existingIds }
                                                if (newSongs.isNotEmpty()) {
                                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        queueEndpoint = newEp
                                                        com.mrtdk.liquid_glass.playback.PlaybackQueue.queueEndpoint = newEp
                                                        queueContinuation = newCont
                                                        com.mrtdk.liquid_glass.playback.PlaybackQueue.queueContinuation = newCont
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
                            }
                    }

                    val skipNextFun: () -> Unit = {
                        if (listenTogetherManager.isInRoom && !listenTogetherManager.isHost) {
                            android.widget.Toast.makeText(context, "Solo el anfitrion puede cambiar canciones.", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            val nextState = com.mrtdk.liquid_glass.playback.PlaybackQueue.getNextSongAndAdvance()
                            if (nextState != null) {
                                if (nextState.contentUri != null) musicPlayer?.playLocalSong(nextState.contentUri, nextState.title, nextState.artist, nextState.artUrl?.toString())
                                else if (nextState.videoId != null) musicPlayer?.playOnlineSong(nextState.videoId, nextState.title, nextState.artist, nextState.artUrl?.toString())
                                listenTogetherManager.broadcastSongChange(nextState)
                            }
                        }
                    }

                    val skipPreviousFun: () -> Unit = {
                        if (listenTogetherManager.isInRoom && !listenTogetherManager.isHost) {
                            android.widget.Toast.makeText(context, "Solo el anfitrion puede cambiar canciones.", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            val prevState = com.mrtdk.liquid_glass.playback.PlaybackQueue.getPreviousSongAndGoBack()
                            if (prevState != null) {
                                if (prevState.contentUri != null) musicPlayer?.playLocalSong(prevState.contentUri, prevState.title, prevState.artist, prevState.artUrl?.toString())
                                else if (prevState.videoId != null) musicPlayer?.playOnlineSong(prevState.videoId, prevState.title, prevState.artist, prevState.artUrl?.toString())
                                listenTogetherManager.broadcastSongChange(prevState)
                            }
                        }
                    }

                    CompositionLocalProvider(com.mrtdk.glass.LocalGlassStyle provides glassStyle) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Black
                        ) { innerPadding ->
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black).nestedScroll(floatingNavBarScrollConnection)) {
                                val mainBackdrop = rememberLayerBackdrop()
                                GlassContainer(
                                    modifier = Modifier.fillMaxSize().background(Color.Black),
                                    useShader = false,
                                    content = {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        // Pager for main tabs (0: Inicio, 1: Novedades, 2: Radio, 3: Biblioteca)
                                        // Search (4) is rendered as an overlay on top
                                        Box(modifier = Modifier.fillMaxSize().layerBackdrop(mainBackdrop)) {
                                            androidx.compose.foundation.pager.HorizontalPager(
                                                state = pagerState,
                                                modifier = Modifier.fillMaxSize().background(Color.Black),
                                                userScrollEnabled = true,
                                            ) { page ->
                                                when (page) {
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
                                                        },
                                                        onReplaySelected = { showReplay = true },
                                                        onListenTogetherSelected = { showListenTogether = true }
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
                                                    2 -> com.mrtdk.liquid_glass.ui.screens.RadioScreen(
                                                        innerPadding = innerPadding,
                                                        onSongRecognized = { recognizedPlayerState ->
                                                            playSong(recognizedPlayerState)
                                                        },
                                                        onSearchResult = { recognizedText ->
                                                            searchQuery = recognizedText
                                                            isSearchSubmitted = true
                                                            selectedIndex = 4
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
                                                        onFavoriteSongsSelected = { showFavoriteSongs = true },
                                                        onUpdateAvailable = { updateReleaseInfo = it },
                                                        onDisableScreenshotChanged = { disable ->
                                                            if (disable) {
                                                                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                                                            } else {
                                                                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                                                            }
                                                        }
                                                    )
                                                }
                                            }

                                            // Search overlay (Page 4)
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = selectedIndex == 4,
                                                enter = androidx.compose.animation.fadeIn(com.mrtdk.liquid_glass.ui.utils.Motion.appear()),
                                                exit = androidx.compose.animation.fadeOut(com.mrtdk.liquid_glass.ui.utils.Motion.appear()),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                BusquedaScreen(
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
                                            }
                                        }

                                        if (showReplay) {
                                            SharedElementTransitionContainer(
                                                onBack = { showReplay = false },
                                                shrinkToTarget = false,
                                                enableSwipeToDismiss = false
                                            ) { _, _ ->
                                                ReplayScreen(
                                                    onBack = { showReplay = false },
                                                    onSongSelected = playSong,
                                                    onArtistSelected = { artistDetail = it },
                                                    onAlbumSelected = { albumDetail = it },
                                                    onPlaylistSelected = { playlistDetail = it }
                                                )
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
                                            AlbumScreen(
                                                albumState = albumDetail!!,
                                                onBack = { albumDetail = null },
                                                onSongSelected = playSong,
                                                onArtistSelected = { artist -> artistDetail = artist },
                                                onAlbumSelected = { album -> albumDetail = album },
                                                onVideoSelected = { videoId -> videoDetail = videoId },
                                                onDominantColorChanged = { color -> globalDominantColor = color },
                                                isPaused = showPlayer
                                            )
                                        }

                                        if (playlistDetail != null) {
                                            PlaylistDetailScreen(
                                                playlist = playlistDetail!!,
                                                onBack = { playlistDetail = null },
                                                onSongSelected = playSong,
                                                onArtistSelected = { artistDetail = it }
                                            )
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

                                        if (showListenTogether) {
                                            androidx.activity.compose.BackHandler { showListenTogether = false }
                                            com.mrtdk.liquid_glass.ui.screens.ListenTogetherScreen(
                                                innerPadding = innerPadding,
                                                onBack = { showListenTogether = false }
                                            )
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
                                                .navigationBarsPadding()
                                                .imePadding(),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            val bottomPad = if (isKeyboardOpen) 2.dp else 8.dp

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .widthIn(max = 500.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .padding(horizontal = 16.dp)
                                                    .padding(bottom = bottomPad)
                                            ) {
                                                LiquidBottomNavBar(
                                                    selectedIndex = selectedIndex,
                                                    tintColor = globalDominantColor.copy(alpha = 0.35f),
                                                    contentColor = contentTintColor,
                                                    scrollConnection = floatingNavBarScrollConnection,
                                                    tabPosition = null,
                                                    playerState = playerState,
                                                    isPlaying = isPlaying,
                                                    playbackProgress = { if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                                                    onSeek = { frac -> if (duration > 0) musicPlayer?.seekTo((frac * duration).toLong()) },
                                                    onTogglePlayPause = { 
                                                        if (listenTogetherManager.isInRoom && !listenTogetherManager.isHost) {
                                                            android.widget.Toast.makeText(context, "Solo el anfitrion puede controlar la reproduccion.", android.widget.Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            if (duration <= 0L && playerState != null) {
                                                                val state = playerState!!
                                                                if (state.contentUri != null) musicPlayer?.playLocalSong(state.contentUri, state.title, state.artist, state.artUrl?.toString())
                                                                else if (state.videoId != null) musicPlayer?.playOnlineSong(state.videoId, state.title, state.artist, state.artUrl?.toString())
                                                            } else {
                                                                musicPlayer?.togglePlayPause() 
                                                            }
                                                            if (listenTogetherManager.isInRoom && listenTogetherManager.isHost) {
                                                                listenTogetherManager.broadcastPlayPause(!isPlaying)
                                                            }
                                                        }
                                                    },
                                                    onMiniPlayerClick = { if (playerState != null) showPlayer = true },
                                                    onNext = skipNextFun,
                                                    onPrevious = skipPreviousFun,
                                                    onTabSelected = { newIndex ->
                                                        artistDetail = null
                                                        albumDetail = null
                                                        playlistDetail = null
                                                        categoryDetail = null
                                                        videoDetail = null
                                                        selectedIndex = newIndex
                                                        if (newIndex in 0..3) {
                                                            mainCoroutineScope.launch { pagerState.slideToPage(newIndex) }
                                                        }
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
                                            if (updateReleaseInfo != null) {
                                                scope.UpdateDialog(
                                                    releaseInfo = updateReleaseInfo!!,
                                                    onDismiss = { updateReleaseInfo = null }
                                                )
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
                                isBottomBarCollapsed = floatingNavBarScrollConnection.isInline,
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
                                     if (listenTogetherManager.isInRoom && !listenTogetherManager.isHost) {
                                         android.widget.Toast.makeText(context, "Solo el anfitrion puede controlar la reproduccion.", android.widget.Toast.LENGTH_SHORT).show()
                                     } else {
                                         if (duration <= 0L && playerState != null) {
                                             val state = playerState!!
                                             if (state.contentUri != null) musicPlayer?.playLocalSong(state.contentUri, state.title, state.artist, state.artUrl?.toString())
                                             else if (state.videoId != null) musicPlayer?.playOnlineSong(state.videoId, state.title, state.artist, state.artUrl?.toString())
                                         } else {
                                             musicPlayer?.togglePlayPause() 
                                         }
                                         if (listenTogetherManager.isInRoom && listenTogetherManager.isHost) {
                                             listenTogetherManager.broadcastPlayPause(!isPlaying)
                                         }
                                     }
                                 },
                                 onSeek = { posMs -> 
                                     if (listenTogetherManager.isInRoom && !listenTogetherManager.isHost) {
                                         android.widget.Toast.makeText(context, "Solo el anfitrion puede mover la musica.", android.widget.Toast.LENGTH_SHORT).show()
                                     } else {
                                         musicPlayer?.seekTo(posMs)
                                         if (listenTogetherManager.isInRoom && listenTogetherManager.isHost) {
                                             listenTogetherManager.broadcastSeek(posMs)
                                         }
                                     }
                                 },
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
                                },
                                playbackError = playbackError,
                                onClearPlaybackError = { musicPlayer?.clearPlaybackError() }
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

suspend fun androidx.compose.foundation.pager.PagerState.slideToPage(page: Int) {
    val from = currentPage
    if (page - from > 1 || from - page > 1) {
        scrollToPage(page + if (page > from) -1 else 1)
    }
    animateScrollToPage(page)
}