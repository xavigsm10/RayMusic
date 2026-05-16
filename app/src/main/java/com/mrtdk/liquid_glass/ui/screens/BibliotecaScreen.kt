package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.mrtdk.liquid_glass.data.LocalMediaScanner
import com.mrtdk.liquid_glass.data.Song
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.ui.components.SongGridItem
import com.mrtdk.liquid_glass.ui.components.PlaylistContextMenuOverlay

import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Settings
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

@Composable
fun BibliotecaScreen(
    innerPadding: PaddingValues,
    onSongSelected: (com.mrtdk.liquid_glass.ui.screens.PlayerState) -> Unit = {},
    onArtistSelected: (com.mrtdk.liquid_glass.ui.screens.ArtistState) -> Unit = {},
    onPlaylistSelected: (com.mrtdk.liquid_glass.data.Playlist) -> Unit = {},
    onAlbumSelected: (com.mrtdk.liquid_glass.ui.screens.AlbumState) -> Unit = {}
) {
    val context = LocalContext.current
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
    var selectedCategory by remember { mutableStateOf<ItemType?>(null) }
    var showCategoryDetail by remember { mutableStateOf(false) }
    var selectedCategoryName by remember { mutableStateOf("") }
    
    var contextMenuPlaylist by remember { mutableStateOf<com.mrtdk.liquid_glass.data.Playlist?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val scanner = LocalMediaScanner(context)
        songs = scanner.getLocalSongs()
    }
    
    if (showSettings) {
        val uriHandler = LocalUriHandler.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding() + 120.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.IconButton(onClick = { showSettings = false }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color(0xFFFA243C))
                }
                Text(
                    text = "Ajustes",
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
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "ACERCA DE",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E))
                        .clickable {
                            checkForUpdates(context)
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Buscar actualizaciones",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Versión actual: 0.5.3",
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
        }
        return
    }
    
    if (showCategoryDetail) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding() + 120.dp)
        ) {
            if (selectedCategoryName != "Playlists") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = { showCategoryDetail = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color(0xFFFA243C))
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
            if (selectedCategoryName == "Playlists") {
                PlaylistsListScreen(
                    onBack = { showCategoryDetail = false },
                    onPlaylistSelected = { pl -> onPlaylistSelected(pl) },
                    paddingValues = innerPadding
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val filteredItems = savedItems.filter { it.type == selectedCategory }
                    items(filteredItems.size) { i ->
                        val item = filteredItems[i]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (item.type) {
                                        ItemType.SONG -> {
                                            onSongSelected(
                                                com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                                    title = item.title,
                                                    artist = item.subtitle,
                                                    artUrl = item.thumbnail,
                                                    videoId = item.id
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
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pinnedPlaylists.size) { i ->
                        val pl = pinnedPlaylists[i]
                        Column(
                            modifier = Modifier
                                .width(160.dp)
                                .pointerInput(pl) {
                                    detectTapGestures(
                                        onTap = { onPlaylistSelected(pl) },
                                        onLongPress = { contextMenuPlaylist = pl }
                                    )
                                },
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (item.second != null || item.first == "Playlists") {
                                    selectedCategory = item.second
                                    selectedCategoryName = item.first
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
                                text = item.first,
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
                text = "Añadido recientemente",
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
                        .clickable {
                            when (item.type) {
                                ItemType.SONG -> {
                                    onSongSelected(
                                        com.mrtdk.liquid_glass.ui.screens.PlayerState(
                                            title = item.title,
                                            artist = item.subtitle,
                                            artUrl = item.thumbnail,
                                            videoId = item.id
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
    PlaylistContextMenuOverlay(playlist = contextMenuPlaylist, onDismiss = { contextMenuPlaylist = null })
}

fun checkForUpdates(context: android.content.Context) {
    Toast.makeText(context, "Buscando actualizaciones...", Toast.LENGTH_SHORT).show()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("https://api.github.com/repos/xavigsm10/RayMusic/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonObject = JSONObject(response)
                val tagName = jsonObject.getString("tag_name")
                val assets = jsonObject.getJSONArray("assets")
                var downloadUrl: String? = null
                
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (downloadUrl != null) {
                        val currentVersion = "0.5.3"
                        if (tagName.replace("v", "") != currentVersion) {
                            downloadApk(context, downloadUrl, tagName)
                        } else {
                            Toast.makeText(context, "Ya tienes la última versión ($currentVersion)", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "No se encontró el APK en la última versión", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al buscar actualizaciones", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun downloadApk(context: android.content.Context, url: String, version: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("RayMusic $version")
            .setDescription("Descargando actualización...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "RayMusic_$version.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        
        val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Descargando actualización...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al iniciar descarga: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}