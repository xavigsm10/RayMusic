package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.echo.innertube.YouTube
import com.echo.innertube.models.ArtistItem
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.AlbumItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader

data class SearchCategory(val name: String, val imageUrl: String)

fun loadCategories(context: android.content.Context): List<SearchCategory> {
    return try {
        val inputStream = context.assets.open("datos.json")
        val jsonStr = InputStreamReader(inputStream).readText()
        val jsonArray = JSONArray(jsonStr)
        val result = mutableListOf<SearchCategory>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val name = obj.getString("name")
            val fileName = name.replace("&", "").replace(",", "") + ".webp"
            result.add(SearchCategory(name, "file:///android_asset/img/$fileName"))
        }
        result
    } catch (e: Exception) {
        emptyList()
    }
}

class BusquedaState {
    var displayResults by mutableStateOf<List<Any>>(emptyList())
    var isSearching by mutableStateOf(false)
    var selectedTab by mutableIntStateOf(0)
    var lastQuery by mutableStateOf("")
}

@Composable
fun BusquedaScreen(
    innerPadding: PaddingValues,
    query: String,
    isSubmitted: Boolean,
    state: BusquedaState = remember { BusquedaState() },
    onSongSelected: (PlayerState) -> Unit,
    onArtistSelected: (ArtistState) -> Unit = {},
    onAlbumSelected: (AlbumState) -> Unit = {},
    onVideoSelected: (String) -> Unit = {},
    onCategorySelected: (SearchCategory) -> Unit = {}
) {
    val context = LocalContext.current
    val tabs = listOf("Top Results", "Artists", "Albums", "Songs")

    // Re-search whenever the query is submitted OR the tab changes
    LaunchedEffect(query, state.selectedTab) {
        if (query.length < 2) {
            state.displayResults = emptyList()
            state.lastQuery = query
            return@LaunchedEffect
        }
        
        // Skip fetch if query and tab are exactly the same as cached
        if (state.lastQuery == query && state.displayResults.isNotEmpty() && !state.isSearching) {
            return@LaunchedEffect
        }
        
        state.isSearching = true
        state.lastQuery = query
        
        withContext(Dispatchers.IO) {
            val filter = when (state.selectedTab) {
                1 -> YouTube.SearchFilter.FILTER_ARTIST
                2 -> YouTube.SearchFilter.FILTER_ALBUM
                3 -> YouTube.SearchFilter.FILTER_SONG
                else -> YouTube.SearchFilter.FILTER_SONG // Top Results
            }
            YouTube.search(query, filter).onSuccess { result ->
                state.displayResults = when (state.selectedTab) {
                    1 -> result.items.filterIsInstance<ArtistItem>().take(20)
                    2 -> result.items.filterIsInstance<AlbumItem>().take(20)
                    else -> result.items.filterIsInstance<SongItem>().take(20)
                }
            }.onFailure {
                state.displayResults = emptyList()
            }
        }
        state.isSearching = false
    }

    val categories = remember { loadCategories(context) }

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = imeBottom > 0

    if (query.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Buscar",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { category ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray)
                            .clickable { onCategorySelected(category) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(category.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = category.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f))
                        )
                        Text(
                            text = category.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 16.dp,
            bottom = innerPadding.calculateBottomPadding() + 180.dp
        )
    ) {
        // Filter tabs row (after submit)
        if (query.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs.size) { i ->
                        val isSelected = i == state.selectedTab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) Color(0xFFE91E63)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .clickable { state.selectedTab = i }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = tabs[i],
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Loading indicator
        if (state.isSearching) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE91E63))
                }
            }
        }

        // Results list
        if (state.displayResults.isNotEmpty()) {
            items(state.displayResults.size) { index ->
                val item = state.displayResults[index]

                when (item) {
                    is SongItem -> {
                        val hdThumb = item.thumbnail.let {
                            when {
                                it.contains("=w") -> it.substringBefore("=w") + "=w1200-h1200-l90-rj"
                                it.contains("ytimg.com/vi/") -> it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                else -> it
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSongSelected(
                                        PlayerState(
                                            title = item.title,
                                            artist = item.artists.joinToString { it.name },
                                            artUrl = hdThumb,
                                            videoId = item.id
                                        )
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(hdThumb).crossfade(true).build(),
                                    contentDescription = "Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "Song · ${item.artists.joinToString { it.name }}",
                                    color = Color.Gray, fontSize = 13.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                            }
                        }
                    }
                    is ArtistItem -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onArtistSelected(
                                        ArtistState(
                                            id = item.id,
                                            name = item.title,
                                            thumbnail = item.thumbnail
                                        )
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.thumbnail).crossfade(true).build(),
                                    contentDescription = "Artist",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium, maxLines = 1)
                                Text("Artist", color = Color.Gray, fontSize = 13.sp)
                            }
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                            }
                        }
                    }
                    is AlbumItem -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAlbumSelected(
                                        AlbumState(
                                            id = item.browseId,
                                            playlistId = item.playlistId,
                                            title = item.title,
                                            artist = item.artists?.joinToString { it.name } ?: "",
                                            thumbnail = item.thumbnail,
                                            year = item.year
                                        )
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.thumbnail).crossfade(true).build(),
                                    contentDescription = "Album",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium, maxLines = 1)
                                Text("Album · ${item.year ?: ""}", color = Color.Gray, fontSize = 13.sp)
                            }
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        } else if (!state.isSearching && query.isNotEmpty() && state.displayResults.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("No hay resultados para \"$query\"", color = Color.Gray)
                }
            }
        }
    }
}
}
