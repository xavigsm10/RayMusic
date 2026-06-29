package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
// Removed LocalMediaScanner import
import com.mrtdk.liquid_glass.data.Song
import com.mrtdk.liquid_glass.data.LibraryItem
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.ui.components.SongGridItem
import com.mrtdk.liquid_glass.ui.components.PlaylistContextMenuOverlay
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.ui.components.trackClickBounds
import com.mrtdk.liquid_glass.ui.components.trackTapBounds
import com.mrtdk.liquid_glass.ui.components.wiggleOnScroll
import com.mrtdk.liquid_glass.ui.components.sharedTransitionElement
import com.mrtdk.liquid_glass.ui.components.SharedTransitionState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.platform.LocalUriHandler
import com.mrtdk.liquid_glass.data.ItemType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import android.os.Build
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.mrtdk.liquid_glass.utils.LocaleUtils
import androidx.compose.ui.window.Dialog

@Composable
fun BibliotecaScreen(
    innerPadding: PaddingValues,
    onSongSelected: (com.mrtdk.liquid_glass.ui.screens.PlayerState) -> Unit = {},
    onPlaylistSelected: (com.mrtdk.liquid_glass.data.Playlist) -> Unit = {},
    onArtistSelected: (com.mrtdk.liquid_glass.ui.screens.ArtistState) -> Unit = {},
    onAlbumSelected: (com.mrtdk.liquid_glass.ui.screens.AlbumState) -> Unit = {},
    initialCategoryKey: String? = null,
    onCategoryConsumed: () -> Unit = {},
    onGlassStyleChanged: (String) -> Unit = {},
    onFavoriteSongsSelected: () -> Unit = {}
) {
    val context = LocalContext.current
    val mainGridState = rememberLazyGridState()
    val categoryGridState = rememberLazyGridState()
    var savedMainIndex by remember { mutableStateOf(-1) }
    var savedMainOffset by remember { mutableStateOf(0) }
    var savedCategoryIndex by remember { mutableStateOf(-1) }
    var savedCategoryOffset by remember { mutableStateOf(0) }

    val outerOnAlbumSelected = onAlbumSelected
    val onAlbumSelected: (com.mrtdk.liquid_glass.ui.screens.AlbumState) -> Unit = { album ->
        savedMainIndex = mainGridState.firstVisibleItemIndex
        savedMainOffset = mainGridState.firstVisibleItemScrollOffset
        savedCategoryIndex = categoryGridState.firstVisibleItemIndex
        savedCategoryOffset = categoryGridState.firstVisibleItemScrollOffset
        outerOnAlbumSelected(album)
    }

    val outerOnPlaylistSelected = onPlaylistSelected
    val onPlaylistSelected: (com.mrtdk.liquid_glass.data.Playlist) -> Unit = { playlist ->
        savedMainIndex = mainGridState.firstVisibleItemIndex
        savedMainOffset = mainGridState.firstVisibleItemScrollOffset
        savedCategoryIndex = categoryGridState.firstVisibleItemIndex
        savedCategoryOffset = categoryGridState.firstVisibleItemScrollOffset
        outerOnPlaylistSelected(playlist)
    }

    LaunchedEffect(SharedTransitionState.isDetailOpen) {
        if (!SharedTransitionState.isDetailOpen) {
            if (savedMainIndex != -1) {
                mainGridState.scrollToItem(savedMainIndex, savedMainOffset)
                savedMainIndex = -1
            }
            if (savedCategoryIndex != -1) {
                categoryGridState.scrollToItem(savedCategoryIndex, savedCategoryOffset)
                savedCategoryIndex = -1
            }
        }
    }
    val menuItems = listOf(
        Triple("Playlists", null, Icons.Default.QueueMusic),
        Triple("Artistas", ItemType.ARTIST, Icons.Default.Mic),
        Triple("Álbumes", ItemType.ALBUM, Icons.Default.Album),
        Triple("Canciones", ItemType.SONG, Icons.Default.MusicNote),
        Triple("Descargados", null, Icons.Default.ArrowCircleDown)
    )
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val savedItems by LibraryManager.savedItems.collectAsState()
    val playlists by LibraryManager.playlists.collectAsState()
    val downloadedSongs by LibraryManager.downloadedSongs.collectAsState()
    var selectedCategory by remember { mutableStateOf<ItemType?>(null) }
    var showCategoryDetail by remember { mutableStateOf(false) }
    var selectedCategoryName by remember { mutableStateOf("") }
    
    var contextMenuPlaylist by remember { mutableStateOf<com.mrtdk.liquid_glass.data.Playlist?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showGlassStyleDialog by remember { mutableStateOf(false) }
    var updateReleaseInfo by remember { mutableStateOf<com.mrtdk.liquid_glass.utils.Updater.ReleaseInfo?>(null) }
    var selectedCategoryKey by remember { mutableStateOf("") }
    var showEqualizer by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    val labelDescargados = stringResource(R.string.descargados)
    LaunchedEffect(initialCategoryKey) {
        if (initialCategoryKey == "Descargados") {
            selectedCategory = null
            selectedCategoryName = labelDescargados
            selectedCategoryKey = "Descargados"
            showCategoryDetail = true
            onCategoryConsumed()
        }
    }

    // Removed local songs scanning LaunchEffect
    
    if (showEqualizer) {
        RayEqualizerScreen(onBack = { showEqualizer = false })
        return
    }

    if (showSettings) {
        val uriHandler = LocalUriHandler.current
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.IconButton(onClick = { showSettings = false }) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = stringResource(R.string.back_action), tint = Color(0xFFFA243C))
                }
                Text(
                    text = stringResource(R.string.ajustes),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.idioma_app_section),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } else {
                                    showLanguageDialog = true
                                }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.idioma_app),
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val currentLang = LibraryManager.getAppLanguage(context)
                            val currentLangName = when (currentLang) {
                                "es" -> "Español"
                                "en" -> "English"
                                else -> stringResource(R.string.predeterminado_sistema)
                            }
                            Text(
                                text = currentLangName,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.apariencia),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGlassStyleDialog = true
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.liquid_glass),
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val currentStyle = LibraryManager.getGlassStyle()
                            val currentStyleName = when (currentStyle) {
                                "transparent" -> stringResource(R.string.vidrio_liquido_transparente)
                                "semitransparent" -> stringResource(R.string.semitransparente)
                                else -> stringResource(R.string.vidrio_liquido_transparente)
                            }
                            Text(
                                text = currentStyleName,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.audio_settings_section),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showQualityDialog = true
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.audio_quality_label),
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val currentQuality = LibraryManager.getString("audio_quality", "high") ?: "high"
                            val currentQualityName = when (currentQuality) {
                                "low" -> stringResource(R.string.audio_quality_low)
                                else -> stringResource(R.string.audio_quality_high)
                            }
                            Text(
                                text = currentQualityName,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(start = 16.dp),
                        color = Color.DarkGray.copy(alpha = 0.5f),
                        thickness = 0.5.dp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showEqualizer = true
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val isEs = LibraryManager.getAppLanguage(context).startsWith("es")
                            Text(
                                text = if (isEs) "Ecualizador" else "Equalizer",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEs) "Configurar ecualizador" else "Configure equalizer",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.acerca_de),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, context.getString(R.string.buscando_actualizaciones), Toast.LENGTH_SHORT).show()
                                com.mrtdk.liquid_glass.utils.Updater.checkUpdate { info ->
                                    CoroutineScope(Dispatchers.Main).launch {
                                        if (info != null) {
                                            updateReleaseInfo = info
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.ultima_version_ok), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                             }
                             .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.buscar_actualizaciones),
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                             Text(
                                text = stringResource(R.string.version_actual, com.mrtdk.liquid_glass.BuildConfig.VERSION_NAME),
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    androidx.compose.material3.Divider(
                        modifier = Modifier.padding(start = 16.dp),
                        color = Color.DarkGray.copy(alpha = 0.5f),
                        thickness = 0.5.dp
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                uriHandler.openUri("https://github.com/xavigsm10/RayMusic")
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.mrtdk.liquid_glass.R.drawable.ic_github),
                                contentDescription = "GitHub",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp).padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.repositorio_app),
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.ver_codigo_github),
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 180.dp))
            }
        }
        
        if (showLanguageDialog) {
            Dialog(
                onDismissRequest = { showLanguageDialog = false }
            ) {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E1E1E))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.language_dialog_title),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val options = listOf(
                            LocaleUtils.SYSTEM_DEFAULT to stringResource(R.string.predeterminado_sistema),
                            "es" to "Español",
                            "en" to "English"
                        )
                        val currentLang = LibraryManager.getAppLanguage(context)
                        
                        options.forEach { (code, name) ->
                            val isSelected = currentLang == code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        LibraryManager.saveAppLanguage(context, code)
                                        LocaleUtils.applyLocale(context)
                                        showLanguageDialog = false
                                        (context as? ComponentActivity)?.recreate()
                                    }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color(0xFFFA243C) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFFFA243C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable { showLanguageDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.cancelar),
                                color = Color.Gray,
                                fontSize = 17.sp
                            )
                        }
                    }
                }
            }
        }

        if (showGlassStyleDialog) {
            Dialog(
                onDismissRequest = { showGlassStyleDialog = false }
            ) {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E1E1E))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.apariencia_dialog_title),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val options = listOf(
                            "transparent" to stringResource(R.string.vidrio_liquido_transparente),
                            "semitransparent" to stringResource(R.string.semitransparente)
                        )
                        val currentStyle = LibraryManager.getGlassStyle()
                        
                        options.forEach { (styleKey, name) ->
                            val isSelected = currentStyle == styleKey
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        LibraryManager.saveGlassStyle(styleKey)
                                        onGlassStyleChanged(styleKey)
                                        showGlassStyleDialog = false
                                    }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color(0xFFFA243C) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFFFA243C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable { showGlassStyleDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.cancelar),
                                color = Color.Gray,
                                fontSize = 17.sp
                            )
                        }
                    }
                }
            }
        }

        if (showQualityDialog) {
            Dialog(
                onDismissRequest = { showQualityDialog = false }
            ) {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E1E1E))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.audio_quality_dialog_title),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val options = listOf(
                            "low" to stringResource(R.string.audio_quality_low),
                            "high" to stringResource(R.string.audio_quality_high)
                        )
                        val currentQuality = LibraryManager.getString("audio_quality", "high") ?: "high"
                        
                        options.forEach { (qualityKey, name) ->
                            val isSelected = currentQuality == qualityKey
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        LibraryManager.saveString("audio_quality", qualityKey)
                                        showQualityDialog = false
                                        try {
                                            val musicPlayer = com.mrtdk.liquid_glass.playback.MusicPlayer(context)
                                            musicPlayer.reloadCurrentSong()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color(0xFFFA243C) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFFFA243C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable { showQualityDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.cancelar),
                                color = Color.Gray,
                                fontSize = 17.sp
                            )
                        }
                    }
                }
            }
        }

        updateReleaseInfo?.let { info ->
            com.mrtdk.liquid_glass.ui.components.UpdateDialog(
                releaseInfo = info,
                onDismiss = { updateReleaseInfo = null }
            )
        }
        return
    }
    
    if (showCategoryDetail) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (selectedCategoryKey != "Playlists") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = { showCategoryDetail = false }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = stringResource(R.string.back_action), tint = Color(0xFFFA243C))
                    }
                    Text(
                        text = selectedCategoryName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            if (selectedCategoryKey == "Playlists") {
                PlaylistsListScreen(
                    onBack = { showCategoryDetail = false },
                    onPlaylistSelected = { pl -> onPlaylistSelected(pl) },
                    onSongSelected = onSongSelected,
                    onFavoriteSongsSelected = onFavoriteSongsSelected,
                    paddingValues = innerPadding
                )
            } else {
                LazyVerticalGrid(
                    state = categoryGridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 180.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val filteredItems = if (selectedCategoryKey == "Descargados") {
                        val grouped = mutableListOf<LibraryItem>()
                        val albumGroups = downloadedSongs.groupBy { it.album }
                        albumGroups.forEach { (albumName, songsInAlbum) ->
                            if (albumName.isNullOrBlank()) {
                                grouped.addAll(songsInAlbum)
                            } else {
                                val firstSong = songsInAlbum.first()
                                grouped.add(
                                    LibraryItem(
                                        id = "offline_album_$albumName",
                                        title = albumName,
                                        subtitle = firstSong.subtitle,
                                        thumbnail = firstSong.thumbnail,
                                        type = ItemType.ALBUM,
                                        album = albumName
                                    )
                                )
                            }
                        }
                        grouped
                    } else {
                        savedItems.filter { it.type == selectedCategory }
                    }
                    items(filteredItems.size) { i ->
                        val item = filteredItems[i]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wiggleOnScroll(item.id, categoryGridState)
                                .trackClickBounds {
                                    SharedTransitionState.lastOpenedId = item.id
                                    when (item.type) {
                                        ItemType.SONG -> {
                                            onSongSelected(
                                                com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                                    title = item.title,
                                                    artist = item.subtitle,
                                                    artUrl = item.thumbnail,
                                                    videoId = item.id,
                                                    album = item.album
                                                )
                                            )
                                        }
                                        ItemType.ARTIST -> {
                                            onArtistSelected(
                                                com.mrtdk.liquid_glass.ui.screens.ArtistState(
                                                    id = item.id,
                                                    name = item.title,
                                                    thumbnail = item.thumbnail
                                                )
                                            )
                                        }
                                        ItemType.ALBUM -> {
                                            onAlbumSelected(
                                                com.mrtdk.liquid_glass.ui.screens.AlbumState(
                                                    id = item.id,
                                                    playlistId = item.id,
                                                    title = item.title,
                                                    artist = item.subtitle,
                                                    thumbnail = item.thumbnail
                                                )
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(if (item.type == ItemType.ARTIST) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                                    .sharedTransitionElement(item.id)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.thumbnail)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.subtitle,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = mainGridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = innerPadding.calculateTopPadding() + 32.dp,
            bottom = innerPadding.calculateBottomPadding() + 180.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Biblioteca",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                androidx.compose.material3.IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustes",
                        tint = Color(0xFFFA243C)
                    )
                }
            }
        }

        val pinnedPlaylists = playlists.filter { it.isPinned }
        if (pinnedPlaylists.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                val pinnedRowState = androidx.compose.foundation.lazy.rememberLazyListState()
                androidx.compose.foundation.lazy.LazyRow(
                    state = pinnedRowState,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pinnedPlaylists.size) { i ->
                        val pl = pinnedPlaylists[i]
                        Column(
                            modifier = Modifier
                                .width(160.dp)
                                .wiggleOnScroll(pl.id, lazyListState = pinnedRowState)
                                .trackTapBounds(
                                    onTap = {
                                        SharedTransitionState.lastOpenedId = pl.id
                                        onPlaylistSelected(pl)
                                    },
                                    onLongPress = { contextMenuPlaylist = pl }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E)),
                                contentAlignment = Alignment.Center
                            ) {
                                val coverUrl = pl.coverUrl ?: (if (pl.items.isNotEmpty()) pl.items.first().thumbnail else null)
                                if (coverUrl != null) {
                                    AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = pl.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        
        item(span = { GridItemSpan(2) }) {
            Column {
                menuItems.forEachIndexed { index, item ->
                    val labelText = when (item.first) {
                        "Playlists" -> stringResource(R.string.playlists)
                        "Artistas" -> stringResource(R.string.artistas)
                        "Álbumes" -> stringResource(R.string.albumes)
                        "Canciones" -> stringResource(R.string.canciones)
                        "Descargados" -> stringResource(R.string.descargados)
                        else -> item.first
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (item.second != null || item.first == "Playlists" || item.first == "Descargados") {
                                    selectedCategory = item.second
                                    selectedCategoryName = labelText
                                    selectedCategoryKey = item.first
                                    showCategoryDetail = true
                                }
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = item.third,
                                contentDescription = null,
                                tint = Color(0xFFFA243C),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = labelText,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha=0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (index < menuItems.size - 1) {
                        androidx.compose.material3.Divider(modifier = Modifier.padding(start = 40.dp), color = Color.DarkGray.copy(alpha=0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
        
        item(span = { GridItemSpan(2) }) {
            Text(
                text = stringResource(R.string.recently_added),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        if (savedItems.isNotEmpty()) {
            items(savedItems.size) { i ->
                val item = savedItems[i]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wiggleOnScroll(item.id, mainGridState)
                        .trackClickBounds {
                            SharedTransitionState.lastOpenedId = item.id
                            when (item.type) {
                                ItemType.SONG -> {
                                    onSongSelected(
                                        com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                            title = item.title,
                                            artist = item.subtitle,
                                            artUrl = item.thumbnail,
                                            videoId = item.id,
                                            album = item.album
                                        )
                                    )
                                }
                                ItemType.ARTIST -> {
                                    onArtistSelected(
                                        com.mrtdk.liquid_glass.ui.screens.ArtistState(
                                            id = item.id,
                                            name = item.title,
                                            thumbnail = item.thumbnail
                                        )
                                    )
                                }
                                ItemType.ALBUM -> {
                                    onAlbumSelected(
                                        com.mrtdk.liquid_glass.ui.screens.AlbumState(
                                            id = item.id,
                                            playlistId = item.id,
                                            title = item.title,
                                            artist = item.subtitle,
                                            thumbnail = item.thumbnail
                                        )
                                    )
                                }
                                else -> {}
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(if (item.type == ItemType.ARTIST) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C1C1E))
                            .sharedTransitionElement(item.id)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.thumbnail)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.subtitle,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            items(songs.size) { index ->
                SongGridItem(song = songs[index], fillMaxWidth = true)
            }
        }
    }
    }
    PlaylistContextMenuOverlay(
        playlist = contextMenuPlaylist,
        onDismiss = { contextMenuPlaylist = null },
        onSongSelected = onSongSelected
    )
}