package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.mrtdk.liquid_glass.ui.components.trackClickBounds
import com.mrtdk.liquid_glass.ui.components.trackTapBounds
import com.mrtdk.liquid_glass.ui.components.wiggleOnScroll
import com.mrtdk.liquid_glass.ui.components.SharedTransitionState
import com.mrtdk.liquid_glass.ui.components.SharedElementTransitionContainer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.echo.innertube.YouTube
import com.echo.innertube.models.AlbumItem
import com.echo.innertube.models.PlaylistItem
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.ArtistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import com.echo.innertube.models.Artist
import com.echo.innertube.models.Album

/** Upgrades any thumbnail URL to the highest quality available.
 *  Handles Apple Music CDN, YouTube Music and YouTube thumbnail formats. */
private fun upgradeThumbQuality(url: String?): String? {
    if (url == null) return null
    return when {
        url.contains("mzstatic.com") -> url
            .replace(Regex("/\\d+x\\d+bb\\.jpg$"), "/600x600bb.jpg")
            .replace(Regex("/\\d+x\\d+bb\\.webp$"), "/600x600bb.jpg")
        url.contains("lh3.googleusercontent.com") ->
            url.replace(Regex("=w\\d+-h\\d+.*$"), "=w600-h600-l90-rj")
        url.contains("yt3.ggpht.com") ->
            url.replace(Regex("=w\\d+-h\\d+.*$"), "=w600-h600-l90-rj")
                .replace(Regex("=s\\d+$"), "=s600")
        url.contains("ytimg.com/vi/") -> url
            .replace("hqdefault.jpg", "maxresdefault.jpg")
            .replace("mqdefault.jpg", "maxresdefault.jpg")
            .replace("sddefault.jpg", "maxresdefault.jpg")
        url.contains("=w226") || url.contains("=w120") ->
            url.replace("=w226-h226", "=w600-h600").replace("=w120-h120", "=w600-h600")
        else -> url
    }
}

private fun parseSongItem(obj: JSONObject): SongItem {
    val id = obj.getString("id")
    val title = obj.getString("title")
    val thumbnail = obj.getString("thumbnail")
    val explicit = obj.optBoolean("explicit", false)
    val artistsArray = obj.getJSONArray("artists")
    val artistsList = mutableListOf<Artist>()
    for (i in 0 until artistsArray.length()) {
        val aObj = artistsArray.getJSONObject(i)
        artistsList.add(Artist(
            name = aObj.getString("name"),
            id = aObj.optString("id").takeIf { it.isNotEmpty() }
        ))
    }
    return SongItem(
        id = id,
        title = title,
        artists = artistsList,
        thumbnail = upgradeThumbQuality(thumbnail) ?: thumbnail,
        explicit = explicit
    )
}

private fun parseAlbumItem(obj: JSONObject): AlbumItem {
    val browseId = obj.getString("browseId")
    val playlistId = obj.getString("playlistId")
    val title = obj.getString("title")
    val thumbnail = obj.getString("thumbnail")
    val explicit = obj.optBoolean("explicit", false)
    val artistsArray = obj.optJSONArray("artists")
    val artistsList = if (artistsArray != null) {
        val list = mutableListOf<Artist>()
        for (i in 0 until artistsArray.length()) {
            val aObj = artistsArray.getJSONObject(i)
            list.add(Artist(
                name = aObj.getString("name"),
                id = aObj.optString("id").takeIf { it.isNotEmpty() }
            ))
        }
        list
    } else null
    val year = if (obj.has("year")) obj.getInt("year") else null
    return AlbumItem(
        browseId = browseId,
        playlistId = playlistId,
        title = title,
        artists = artistsList,
        year = year,
        thumbnail = upgradeThumbQuality(thumbnail) ?: thumbnail,
        explicit = explicit
    )
}

private fun parsePlaylistItem(obj: JSONObject): PlaylistItem {
    val id = obj.getString("id")
    val title = obj.getString("title")
    val thumbnail = obj.optString("thumbnail", "")
    val authorObj = obj.optJSONObject("author")
    val author = if (authorObj != null) {
        Artist(
            name = authorObj.getString("name"),
            id = authorObj.optString("id").takeIf { it.isNotEmpty() }
        )
    } else null
    val songCountText = obj.optString("songCountText", "Playlist")
    return PlaylistItem(
        id = id,
        title = title,
        author = author,
        songCountText = songCountText,
        thumbnail = upgradeThumbQuality(thumbnail) ?: thumbnail,
        playEndpoint = null,
        shuffleEndpoint = null,
        radioEndpoint = null
    )
}

private fun parseArtistItem(obj: JSONObject): ArtistItem {
    val id = obj.getString("id")
    val title = obj.getString("title")
    val thumbnail = obj.optString("thumbnail", "")
    return ArtistItem(
        id = id,
        title = title,
        thumbnail = upgradeThumbQuality(thumbnail) ?: thumbnail,
        shuffleEndpoint = null,
        radioEndpoint = null
    )
}

class CategoriaState {
    var isLoading by mutableStateOf(true)
    var playlists by mutableStateOf<List<PlaylistItem>>(emptyList())
    var albums by mutableStateOf<List<AlbumItem>>(emptyList())
    var songs by mutableStateOf<List<SongItem>>(emptyList())
    var artists by mutableStateOf<List<ArtistItem>>(emptyList())
}

@Composable
fun CategoriaScreen(
    category: SearchCategory,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    onAlbumSelected: (AlbumState) -> Unit = {},
    onPlaylistSelected: (AlbumState) -> Unit = {},
    onArtistSelected: (ArtistState) -> Unit = {}
) {
    val context = LocalContext.current
    val state = remember { CategoriaState() }

    // Local navigation stack for album/artist overlays within the category page
    var localAlbumDetail by remember { mutableStateOf<AlbumState?>(null) }
    var localArtistDetail by remember { mutableStateOf<ArtistState?>(null) }

    // Handle system back: close local overlays first, then go back to search
    androidx.activity.compose.BackHandler(enabled = localAlbumDetail != null || localArtistDetail != null) {
        when {
            localAlbumDetail != null -> localAlbumDetail = null
            localArtistDetail != null -> localArtistDetail = null
        }
    }
    
    LaunchedEffect(category.name) {
        state.isLoading = true
        withContext(Dispatchers.IO) {
            var loadedOffline = false
            try {
                val inputStream = context.assets.open("categorias_apple.json")
                val jsonStr = inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                if (root.has(category.name)) {
                    val catData = root.getJSONObject(category.name)
                    
                    val songsList = mutableListOf<SongItem>()
                    val songsArray = catData.getJSONArray("songs")
                    for (i in 0 until songsArray.length()) {
                        songsList.add(parseSongItem(songsArray.getJSONObject(i)))
                    }
                    
                    val albumsList = mutableListOf<AlbumItem>()
                    val albumsArray = catData.getJSONArray("albums")
                    for (i in 0 until albumsArray.length()) {
                        albumsList.add(parseAlbumItem(albumsArray.getJSONObject(i)))
                    }
                    
                    val playlistsList = mutableListOf<PlaylistItem>()
                    val playlistsArray = catData.getJSONArray("playlists")
                    for (i in 0 until playlistsArray.length()) {
                        playlistsList.add(parsePlaylistItem(playlistsArray.getJSONObject(i)))
                    }
                    
                    val artistsList = mutableListOf<ArtistItem>()
                    val artistsArray = catData.getJSONArray("artists")
                    for (i in 0 until artistsArray.length()) {
                        artistsList.add(parseArtistItem(artistsArray.getJSONObject(i)))
                    }
                    
                    state.songs = songsList
                    state.albums = albumsList
                    state.playlists = playlistsList
                    state.artists = artistsList
                    loadedOffline = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            if (!loadedOffline) {
                val d1 = async {
                    YouTube.search(category.name, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST).getOrNull()?.items?.filterIsInstance<PlaylistItem>()?.take(16) ?: emptyList()
                }
                val d2 = async {
                    val rawAlbums = YouTube.search(category.name, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()?.items?.filterIsInstance<AlbumItem>() ?: emptyList()
                    rawAlbums.filter { a ->
                        val titleLower = a.title.lowercase()
                        val artistText = a.artists?.joinToString { it.name }?.lowercase() ?: ""
                        val isMix = titleLower.contains("mix") || titleLower.contains("compilation") ||
                            titleLower.contains("playlist") || titleLower.contains("mashup") ||
                            titleLower.contains("medley") || titleLower.contains("recopilación")
                        val isGenericArtist = artistText.contains("various") || artistText.contains("varios") ||
                            artistText.contains("topic") || artistText.isEmpty() || artistText.contains("mix")
                        !isMix && !isGenericArtist
                    }.take(16)
                }
                val d3 = async {
                    YouTube.search(category.name, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterIsInstance<SongItem>()?.take(30) ?: emptyList()
                }
                val d4 = async {
                    YouTube.search(category.name, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()?.items?.filterIsInstance<ArtistItem>()?.take(16) ?: emptyList()
                }
                
                state.playlists = d1.await()
                state.albums = d2.await()
                state.songs = d3.await()
                state.artists = d4.await()
            }
        }
        state.isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(innerPadding)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Atrás", tint = Color(0xFFFA243C))
            }
            Text(
                text = category.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE91E63))
            }
        } else {
            val verticalScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .padding(bottom = 180.dp)
            ) {
                // ── CAROUSEL DESTACADO (Hero Carousel) ──────────
                if (state.songs.isNotEmpty()) {
                    val featuredItems = state.songs.take(5)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp)
                    ) {
                        items(featuredItems.size) { index ->
                            val song = featuredItems[index]
                            val hdThumb = song.thumbnail.let {
                                when {
                                    it.contains("=w") -> it.substringBefore("=w") + "=w720-h720-l90-rj"
                                    it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                    else -> it
                                }
                            }
                            Column(modifier = Modifier.width(340.dp).clickable {
                                onSongSelected(PlayerState(
                                    title = song.title,
                                    artist = song.artists.joinToString { it.name },
                                    artUrl = hdThumb,
                                    videoId = song.id
                                ))
                            }) {
                                Text("NUEVO DESCUBRIMIENTO", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(song.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artists.joinToString { it.name }, color = Color.Gray, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.DarkGray)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                        contentDescription = "Hero Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Dark gradient overlay
                                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha=0.9f)),
                                        startY = 250f
                                    )))
                                    
                                    // Badge Top Left
                                    Row(
                                        modifier = Modifier.padding(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha=0.6f)).padding(horizontal=6.dp, vertical=4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Audio espacial con Dolby Atmos", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    // Bottom Text and Mini Cover
                                    Row(
                                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Explora lo mejor de ${song.artists.firstOrNull()?.name ?: category.name}.",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 20.sp
                                        )
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(hdThumb).crossfade(false).build(),
                                            contentDescription = "Mini Cover",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Playlists (Apple Music Style - 2 Rows)
                if (state.playlists.isNotEmpty()) {
                    Text(
                        text = "Playlists destacadas",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                    )
                    
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(460.dp)
                    ) {
                        items(state.playlists) { item ->
                            var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            val pressScale = remember { Animatable(1f) }
                            val pressScopePlaylist = rememberCoroutineScope()
                            Column(
                                modifier = Modifier
                                    .width(160.dp)
                                    .graphicsLayer {
                                        scaleX = pressScale.value
                                        scaleY = pressScale.value
                                    }
                                    .pointerInput(item.id) {
                                        detectTapGestures(
                                            onPress = {
                                                pressScopePlaylist.launch {
                                                    pressScale.animateTo(0.93f, spring(dampingRatio = 0.6f, stiffness = 600f))
                                                }
                                                val released = tryAwaitRelease()
                                                pressScopePlaylist.launch {
                                                    pressScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
                                                }
                                                if (released) {
                                                    SharedTransitionState.lastClickBounds = imageCoords?.boundsInRoot()
                                                    SharedTransitionState.lastOpenedId = item.id
                                                    localAlbumDetail = AlbumState(
                                                        id = item.id,
                                                        playlistId = item.id,
                                                        title = item.title,
                                                        artist = item.author?.name ?: "",
                                                        thumbnail = item.thumbnail
                                                    )
                                                }
                                            }
                                        )
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.thumbnail?.replace("w226", "w400")?.replace("h226", "h400"))
                                        .crossfade(false).build(),
                                    contentDescription = "Playlist Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(160.dp)
                                        .onGloballyPositioned { imageCoords = it }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.author?.name ?: "",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Canciones (Apple Music Style - 4 Rows)
                if (state.songs.isNotEmpty()) {
                    Text(
                        text = "Mejores Canciones",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 12.dp)
                    )
                    
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(4),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(state.songs) { item ->
                            val hdThumb = item.thumbnail.let {
                                when {
                                    it.contains("=w") -> it.substringBefore("=w") + "=w300-h300-l90-rj"
                                    it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "mqdefault")
                                    else -> it
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .width(300.dp)
                                    .clickable {
                                        onSongSelected(
                                            PlayerState(
                                                title = item.title,
                                                artist = item.artists.joinToString { it.name },
                                                artUrl = hdThumb,
                                                videoId = item.id
                                            )
                                        )
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(hdThumb).crossfade(false).build(),
                                    contentDescription = "Song Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artists.joinToString { it.name },
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Albums (Apple Music Style - 2 Rows)
                if (state.albums.isNotEmpty()) {
                    Text(
                        text = "Álbumes imprescindibles",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 12.dp)
                    )
                    
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(460.dp)
                    ) {
                        items(state.albums) { item ->
                            var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            val pressScale = remember { Animatable(1f) }
                            val pressScopeAlbum = rememberCoroutineScope()
                            Column(
                                modifier = Modifier
                                    .width(160.dp)
                                    .graphicsLayer {
                                        scaleX = pressScale.value
                                        scaleY = pressScale.value
                                    }
                                    .pointerInput(item.browseId) {
                                        detectTapGestures(
                                            onPress = {
                                                pressScopeAlbum.launch {
                                                    pressScale.animateTo(0.93f, spring(dampingRatio = 0.6f, stiffness = 600f))
                                                }
                                                val released = tryAwaitRelease()
                                                pressScopeAlbum.launch {
                                                    pressScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
                                                }
                                                if (released) {
                                                    SharedTransitionState.lastClickBounds = imageCoords?.boundsInRoot()
                                                    SharedTransitionState.lastOpenedId = item.browseId
                                                    localAlbumDetail = AlbumState(
                                                        id = item.browseId,
                                                        playlistId = item.playlistId,
                                                        title = item.title,
                                                        artist = item.artists?.joinToString { it.name } ?: "",
                                                        thumbnail = item.thumbnail,
                                                        year = item.year
                                                    )
                                                }
                                            }
                                        )
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.thumbnail?.replace("w226", "w400")?.replace("h226", "h400"))
                                        .crossfade(false).build(),
                                    contentDescription = "Album Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(160.dp)
                                        .onGloballyPositioned { imageCoords = it }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.artists?.joinToString { it.name } ?: "",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Artists (Apple Music Style - Circular avatars)
                if (state.artists.isNotEmpty()) {
                    Text(
                        text = "Artistas que nos encantan",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 12.dp)
                    )
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.artists) { item ->
                            val pressScale = remember { Animatable(1f) }
                            val pressScopeArtist = rememberCoroutineScope()
                            Column(
                                modifier = Modifier
                                    .width(120.dp)
                                    .graphicsLayer {
                                        scaleX = pressScale.value
                                        scaleY = pressScale.value
                                    }
                                    .pointerInput(item.id) {
                                        detectTapGestures(
                                            onPress = {
                                                pressScopeArtist.launch {
                                                    pressScale.animateTo(0.93f, spring(dampingRatio = 0.6f, stiffness = 600f))
                                                }
                                                val released = tryAwaitRelease()
                                                pressScopeArtist.launch {
                                                    pressScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
                                                }
                                                if (released) {
                                                    localArtistDetail = ArtistState(id = item.id, name = item.title, thumbnail = item.thumbnail)
                                                }
                                            }
                                        )
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.thumbnail?.let {
                                            when {
                                                it.contains("=w") -> it.substringBefore("=w") + "=w400-h400-l90-rj"
                                                else -> it
                                            }
                                        })
                                        .crossfade(false).build(),
                                    contentDescription = "Artist Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Local Album overlay (shown on top of the category page) ──
    if (localAlbumDetail != null) {
        SharedElementTransitionContainer(
            onBack = { localAlbumDetail = null },
            enableSwipeToDismiss = false
        ) { _, _ ->
            AlbumScreen(
                albumState = localAlbumDetail!!,
                onBack = { localAlbumDetail = null },
                onSongSelected = onSongSelected,
                onArtistSelected = { artist -> localArtistDetail = artist },
                onAlbumSelected = { album -> localAlbumDetail = album },
                onDominantColorChanged = {}
            )
        }
    }

    // ── Local Artist overlay (shown on top of the category page) ──
    if (localArtistDetail != null) {
        SharedElementTransitionContainer(
            onBack = { localArtistDetail = null },
            enableSwipeToDismiss = false
        ) { _, _ ->
            ArtistScreen(
                artistState = localArtistDetail!!,
                innerPadding = innerPadding,
                onBack = { localArtistDetail = null },
                onSongSelected = onSongSelected,
                onAlbumSelected = { album -> localAlbumDetail = album },
                onArtistSelected = { artist -> localArtistDetail = artist }
            )
        }
    }
    } // end Box
}
