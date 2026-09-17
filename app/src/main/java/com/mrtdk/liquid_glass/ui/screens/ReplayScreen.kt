package com.mrtdk.liquid_glass.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrtdk.glass.GlassBox
import com.mrtdk.glass.GlassContainer
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.data.PlaybackRecord
import com.mrtdk.liquid_glass.data.Playlist
import java.util.Calendar

// Data structures for Replay Stats
data class ArtistStat(val id: String, val name: String, val thumbnail: String?, val minutes: Int)
data class SongStat(val id: String, val title: String, val artist: String, val thumbnail: String?, val plays: Int)
data class AlbumStat(val id: String, val title: String, val artist: String, val thumbnail: String?, val minutes: Int)

private enum class ReplayView {
    MAIN,
    TOP_SONGS_DETAIL,
    TOP_ALBUMS_DETAIL
}

@Composable
fun ReplayScreen(
    onBack: () -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    onArtistSelected: (ArtistState) -> Unit,
    onAlbumSelected: (AlbumState) -> Unit,
    onPlaylistSelected: (Playlist) -> Unit
) {
    val context = LocalContext.current
    var currentView by remember { mutableStateOf(ReplayView.MAIN) }

    // Year & Month Selection (Defaulting to August 2026 matching screenshots)
    var selectedYear by remember { mutableStateOf("2026") }
    var selectedMonthIndex by remember { mutableIntStateOf(7) } // 7 = August (0-indexed)
    var showYearDropdown by remember { mutableStateOf(false) }

    val years = listOf("2026")
    val monthShortNames = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sept.", "oct.", "nov.", "dic.")

    fun getMonthFullDisplay(idx: Int): String = when (idx) {
        0 -> "enero"
        1 -> "febrero"
        2 -> "marzo"
        3 -> "abril"
        4 -> "mayo"
        5 -> "junio"
        6 -> "julio"
        7 -> "agosto"
        8 -> "septiembre"
        9 -> "octubre"
        10 -> "noviembre"
        11 -> "diciembre"
        else -> "este mes"
    }

    // System Back Handler
    BackHandler(enabled = currentView != ReplayView.MAIN) {
        currentView = ReplayView.MAIN
    }

    // Load user's real playback records from database
    var playbackHistory by remember { mutableStateOf<List<PlaybackRecord>>(emptyList()) }
    LaunchedEffect(Unit) {
        playbackHistory = withContext(Dispatchers.IO) {
            LibraryManager.getPlaybackHistory()
        }
    }

    // Filter playback history by selected year and month
    val filteredHistory = remember(playbackHistory, selectedYear, selectedMonthIndex) {
        val calendar = Calendar.getInstance()
        playbackHistory.filter { record ->
            calendar.timeInMillis = record.timestamp
            val yearMatches = calendar.get(Calendar.YEAR).toString() == selectedYear
            val monthMatches = calendar.get(Calendar.MONTH) == selectedMonthIndex
            yearMatches && monthMatches
        }
    }

    // Aggregate statistics with sample fallback if user has little or no history yet
    val totalMinutes = remember(filteredHistory) {
        val calculated = filteredHistory.size * 3
        if (calculated > 0) calculated else 75
    }

    val artistsList = remember(filteredHistory) {
        val grouped = filteredHistory
            .groupBy { it.artist }
            .map { (artistName, records) ->
                ArtistStat(
                    id = records.first().songId,
                    name = artistName,
                    thumbnail = records.firstOrNull { it.thumbnail != null }?.thumbnail,
                    minutes = records.size * 3
                )
            }
            .sortedByDescending { it.minutes }

        if (grouped.isNotEmpty()) grouped else listOf(
            ArtistStat("sample_mj", "Michael Jackson", "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Michael_Jackson_in_1988.jpg/800px-Michael_Jackson_in_1988.jpg", 31),
            ArtistStat("sample_j5", "Jackson 5", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/Jackson_5_1974.jpg/800px-Jackson_5_1974.jpg", 7),
            ArtistStat("sample_sc", "Sabrina Carpenter", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d3/Sabrina_Carpenter_November_2024.jpg/800px-Sabrina_Carpenter_November_2024.jpg", 5),
            ArtistStat("sample_mg", "Manuel García", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4e/Manuel_Garc%C3%ADa_en_Concepci%C3%B3n_%28cropped%29.jpg/800px-Manuel_Garc%C3%ADa_en_Concepci%C3%B3n_%28cropped%29.jpg", 4)
        )
    }

    // Official artist portrait thumbnails cache resolved from YouTube Music
    val officialArtistImages = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(artistsList) {
        artistsList.forEach { artist ->
            if (!officialArtistImages.containsKey(artist.name)) {
                launch(Dispatchers.IO) {
                    try {
                        val searchRes = com.echo.innertube.YouTube.search(
                            artist.name,
                            com.echo.innertube.YouTube.SearchFilter.FILTER_ARTIST
                        ).getOrNull()
                        val artistItem = searchRes?.items?.filterIsInstance<com.echo.innertube.models.ArtistItem>()?.firstOrNull {
                            it.title.equals(artist.name, ignoreCase = true)
                        } ?: searchRes?.items?.filterIsInstance<com.echo.innertube.models.ArtistItem>()?.firstOrNull()

                        val thumb = artistItem?.thumbnail?.let {
                            com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(it) ?: it
                        }
                        if (!thumb.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                officialArtistImages[artist.name] = thumb
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val songsList = remember(filteredHistory) {
        val grouped = filteredHistory
            .groupBy { it.songId }
            .map { (songId, records) ->
                val first = records.first()
                SongStat(
                    id = songId,
                    title = first.title,
                    artist = first.artist,
                    thumbnail = first.thumbnail,
                    plays = records.size
                )
            }
            .sortedByDescending { it.plays }

        if (grouped.isNotEmpty()) grouped else listOf(
            SongStat("s1", "Wanna Be Startin' Somethin'", "Michael Jackson", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200&fit=crop", 4),
            SongStat("s2", "I'll Be There", "Jackson 5", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=200&fit=crop", 4),
            SongStat("s3", "Don't Stop 'Til You Get Enough", "Michael Jackson", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=200&fit=crop", 3),
            SongStat("s4", "Bad (2012 Remaster)", "Michael Jackson", "https://images.unsplash.com/photo-1511735111819-9a3f7709049c?w=200&fit=crop", 3),
            SongStat("s5", "Baby Be Mine", "Michael Jackson", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200&fit=crop", 2),
            SongStat("s6", "Juno", "Sabrina Carpenter", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=200&fit=crop", 2),
            SongStat("s7", "Human Nature", "Michael Jackson", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=200&fit=crop", 2),
            SongStat("s8", "La Danza de las Libelulas", "Manuel García", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=200&fit=crop", 2),
            SongStat("s9", "Thriller", "Michael Jackson", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200&fit=crop", 2)
        )
    }

    val albumsList = remember(filteredHistory) {
        val grouped = filteredHistory
            .filter { it.album != null }
            .groupBy { it.album }
            .map { (albumName, records) ->
                val first = records.first()
                AlbumStat(
                    id = first.songId,
                    title = albumName ?: "",
                    artist = first.artist,
                    thumbnail = first.thumbnail,
                    minutes = records.size * 3
                )
            }
            .sortedByDescending { it.minutes }

        if (grouped.isNotEmpty()) grouped else listOf(
            AlbumStat("a1", "Michael: Songs From The Motion Picture", "Michael Jackson", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&fit=crop", 17),
            AlbumStat("a2", "Thriller", "Michael Jackson", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300&fit=crop", 15),
            AlbumStat("a3", "Off the Wall", "Michael Jackson", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&fit=crop", 2),
            AlbumStat("a4", "Bad", "Michael Jackson", "https://images.unsplash.com/photo-1511735111819-9a3f7709049c?w=300&fit=crop", 2)
        )
    }

    val achievementMinutes = remember(totalMinutes) {
        if (totalMinutes > 0) totalMinutes else 2500
    }
    val achievementArtists = remember(artistsList) {
        if (artistsList.isNotEmpty()) artistsList.size else 100
    }

    GlassContainer(
        modifier = Modifier.fillMaxSize(),
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Warm ambient glow in top-right corner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE53935).copy(alpha = 0.82f),
                                    Color(0xFFFB8C00).copy(alpha = 0.45f),
                                    Color.Transparent
                                ),
                                center = Offset(Float.POSITIVE_INFINITY, 0f),
                                radius = 950f
                            )
                        )
                )

                AnimatedContent(
                    targetState = currentView,
                    transitionSpec = {
                        fadeIn(tween(220, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(tween(180, easing = FastOutSlowInEasing))
                    },
                    label = "replayScreenViewTransition"
                ) { view ->
                    when (view) {
                        ReplayView.MAIN -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Spacer for top glass buttons bar
                                item {
                                    Spacer(modifier = Modifier.height(64.dp))
                                }

                                // Header: "Replay" + Year Picker Capsule
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Replay",
                                            color = Color.White,
                                            fontSize = 38.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Box {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(24.dp))
                                                    .background(Color(0x33FFFFFF))
                                                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                                                    .clickable { showYearDropdown = true }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = selectedYear,
                                                    color = Color.White,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = showYearDropdown,
                                                onDismissRequest = { showYearDropdown = false },
                                                modifier = Modifier.background(Color(0xFF1E1E20))
                                            ) {
                                                years.forEach { year ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = year,
                                                                color = if (year == selectedYear) Color(0xFFFA243C) else Color.White
                                                            )
                                                        },
                                                        onClick = {
                                                            selectedYear = year
                                                            showYearDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Month selector horizontal bar
                                item {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        itemsIndexed(monthShortNames) { idx, name ->
                                            val isSelected = idx == selectedMonthIndex
                                            Text(
                                                text = name,
                                                color = if (isSelected) Color.White else Color(0xFF8E8E93),
                                                fontSize = if (isSelected) 18.sp else 16.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        selectedMonthIndex = idx
                                                    }
                                                    .padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Big summary statement
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "En ${getMonthFullDisplay(selectedMonthIndex)}, escuchaste",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$totalMinutes minutos.",
                                            color = Color.White,
                                            fontSize = 38.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                // Section 1: Top Artistas
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Top artistas",
                                                    color = Color.White,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            itemsIndexed(artistsList, key = { index, artist -> "replay_artist_${artist.id.ifEmpty { "$index" }}" }) { index, artist ->
                                                val officialThumb = officialArtistImages[artist.name] ?: artist.thumbnail
                                                Card(
                                                    modifier = Modifier
                                                        .width(220.dp)
                                                        .height(280.dp)
                                                        .clip(RoundedCornerShape(18.dp))
                                                        .clickable {
                                                            onArtistSelected(
                                                                ArtistState(
                                                                    id = artist.id,
                                                                    name = artist.name,
                                                                    thumbnail = officialThumb
                                                                )
                                                            )
                                                        },
                                                    shape = RoundedCornerShape(18.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                                                ) {
                                                    Box(modifier = Modifier.fillMaxSize()) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(context)
                                                                .data(officialThumb)
                                                                .crossfade(true)
                                                                .build(),
                                                            contentDescription = artist.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )

                                                        // Bottom dark scrim
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(
                                                                    Brush.verticalGradient(
                                                                        listOf(
                                                                            Color.Transparent,
                                                                            Color.Transparent,
                                                                            Color.Black.copy(alpha = 0.85f)
                                                                        )
                                                                    )
                                                                )
                                                        )

                                                        // Big position number
                                                        Text(
                                                            text = "${index + 1}",
                                                            color = Color.White,
                                                            fontSize = 44.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier
                                                                .padding(top = 10.dp, start = 14.dp)
                                                                .align(Alignment.TopStart)
                                                        )

                                                        // Name & minutes
                                                        Column(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomStart)
                                                                .padding(14.dp)
                                                        ) {
                                                            Text(
                                                                text = artist.name,
                                                                color = Color.White,
                                                                fontSize = 18.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "${artist.minutes} minutos",
                                                                color = Color.White.copy(alpha = 0.7f),
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Section 2: Top Canciones
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { currentView = ReplayView.TOP_SONGS_DETAIL },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Top canciones",
                                                    color = Color.White,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        // Top 4 preview list
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            songsList.take(4).forEachIndexed { index, song ->
                                                ReplaySongRow(
                                                    rank = index + 1,
                                                    song = song,
                                                    onClick = {
                                                        onSongSelected(
                                                            PlayerState(
                                                                title = song.title,
                                                                artist = song.artist,
                                                                artUrl = song.thumbnail,
                                                                videoId = song.id
                                                            )
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Section 3: Top Álbumes
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { currentView = ReplayView.TOP_ALBUMS_DETAIL },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Top álbumes",
                                                    color = Color.White,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            itemsIndexed(albumsList, key = { index, album -> "replay_album_${album.id.ifEmpty { "$index" }}" }) { index, album ->
                                                Column(
                                                    modifier = Modifier
                                                        .width(160.dp)
                                                        .clickable {
                                                            onAlbumSelected(
                                                                AlbumState(
                                                                    id = album.id,
                                                                    playlistId = album.id,
                                                                    title = album.title,
                                                                    artist = album.artist,
                                                                    thumbnail = album.thumbnail
                                                                )
                                                            )
                                                        }
                                                ) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(album.thumbnail)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = album.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(160.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "${index + 1}",
                                                        color = Color.White,
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = album.title,
                                                        color = Color.White,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = album.artist,
                                                        color = Color.White.copy(alpha = 0.65f),
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${album.minutes} minutos",
                                                        color = Color.White.copy(alpha = 0.55f),
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Section 4: Tus Logros
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Tus logros",
                                                color = Color.White,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            // Card 1: Minutos reproducidos
                                            item {
                                                Card(
                                                    modifier = Modifier
                                                        .width(200.dp)
                                                        .height(240.dp)
                                                        .clip(RoundedCornerShape(18.dp)),
                                                    shape = RoundedCornerShape(18.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181A))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(
                                                                Brush.radialGradient(
                                                                    colors = listOf(
                                                                        Color(0xFFE53935).copy(alpha = 0.55f),
                                                                        Color(0xFFFB8C00).copy(alpha = 0.25f),
                                                                        Color.Transparent
                                                                    ),
                                                                    center = Offset(0f, 600f),
                                                                    radius = 450f
                                                                )
                                                            )
                                                            .padding(16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            MetallicSpeedometerBadge(
                                                                text = "- ${achievementMinutes.coerceAtLeast(2500)} -",
                                                                modifier = Modifier.size(115.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(18.dp))
                                                            Text(
                                                                text = "Minutos reproducidos",
                                                                color = Color.White,
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "En curso",
                                                                color = Color(0xFFFFA726),
                                                                fontSize = 13.sp,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Card 2: Artistas reproducidos
                                            item {
                                                Card(
                                                    modifier = Modifier
                                                        .width(200.dp)
                                                        .height(240.dp)
                                                        .clip(RoundedCornerShape(18.dp)),
                                                    shape = RoundedCornerShape(18.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181A))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(
                                                                Brush.radialGradient(
                                                                    colors = listOf(
                                                                        Color(0xFFF57C00).copy(alpha = 0.55f),
                                                                        Color(0xFFFFB300).copy(alpha = 0.25f),
                                                                        Color.Transparent
                                                                    ),
                                                                    center = Offset(300f, 600f),
                                                                    radius = 450f
                                                                )
                                                            )
                                                            .padding(16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            MetallicStarBadge(
                                                                text = "${achievementArtists.coerceAtLeast(100)}",
                                                                modifier = Modifier.size(115.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(18.dp))
                                                            Text(
                                                                text = "Artistas reproducidos",
                                                                color = Color.White,
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "En curso",
                                                                color = Color(0xFFFFA726),
                                                                fontSize = 13.sp,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(120.dp))
                                }
                            }
                        }

                        ReplayView.TOP_SONGS_DETAIL -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item { Spacer(modifier = Modifier.height(64.dp)) }

                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Top canciones",
                                            color = Color.White,
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "${getMonthFullDisplay(selectedMonthIndex)} de $selectedYear",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }

                                // Play and Shuffle pill buttons
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                songsList.firstOrNull()?.let { firstSong ->
                                                    onSongSelected(
                                                        PlayerState(
                                                            title = firstSong.title,
                                                            artist = firstSong.artist,
                                                            artUrl = firstSong.thumbnail,
                                                            videoId = firstSong.id
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242426))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White
                                                )
                                                Text(
                                                    text = "Reproducir",
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                songsList.shuffled().firstOrNull()?.let { randSong ->
                                                    onSongSelected(
                                                        PlayerState(
                                                            title = randSong.title,
                                                            artist = randSong.artist,
                                                            artUrl = randSong.thumbnail,
                                                            videoId = randSong.id
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242426))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Shuffle,
                                                    contentDescription = null,
                                                    tint = Color.White
                                                )
                                                Text(
                                                    text = "Aleatorio",
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                itemsIndexed(songsList, key = { index, song -> "replay_song_${song.id.ifEmpty { "$index" }}" }) { index, song ->
                                    ReplaySongRow(
                                        rank = index + 1,
                                        song = song,
                                        onClick = {
                                            onSongSelected(
                                                PlayerState(
                                                    title = song.title,
                                                    artist = song.artist,
                                                    artUrl = song.thumbnail,
                                                    videoId = song.id
                                                )
                                            )
                                        }
                                    )
                                }

                                item { Spacer(modifier = Modifier.height(120.dp)) }
                            }
                        }

                        ReplayView.TOP_ALBUMS_DETAIL -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Spacer(modifier = Modifier.height(64.dp))
                                Text(
                                    text = "Top álbumes",
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${getMonthFullDisplay(selectedMonthIndex)} de $selectedYear",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(18.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(albumsList, key = { index, album -> "replay_grid_album_${album.id.ifEmpty { "$index" }}" }) { index, album ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onAlbumSelected(
                                                        AlbumState(
                                                            id = album.id,
                                                            playlistId = album.id,
                                                            title = album.title,
                                                            artist = album.artist,
                                                            thumbnail = album.thumbnail
                                                        )
                                                    )
                                                }
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(album.thumbnail)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = album.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "${index + 1}",
                                                color = Color.White,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = album.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = album.artist,
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${album.minutes} minutos",
                                                color = Color.White.copy(alpha = 0.55f),
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        glassContent = {
            val scope = this
            // Fixed Top Bar: Preserving the exact RayMusic glass back and share pill buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Back button pill
                scope.GlassBox(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(54.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (currentView != ReplayView.MAIN) {
                                currentView = ReplayView.MAIN
                            } else {
                                onBack()
                            }
                        },
                    shape = CircleShape,
                    tint = Color.Black.copy(alpha = 0.25f),
                    blur = 0.8f,
                    centerDistortion = 0.1f,
                    scale = 0.02f,
                    warpEdges = 0.4f,
                    elevation = 4.dp,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.back_action),
                        tint = if (currentView != ReplayView.MAIN) Color(0xFFFA243C) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Share button pill
                scope.GlassBox(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(54.dp)
                        .clip(CircleShape)
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "¡Echa un vistazo a mi RayMusic Replay $selectedYear!")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.compartir)))
                        },
                    shape = CircleShape,
                    tint = Color.Black.copy(alpha = 0.25f),
                    blur = 0.8f,
                    centerDistortion = 0.1f,
                    scale = 0.02f,
                    warpEdges = 0.4f,
                    elevation = 4.dp,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = stringResource(R.string.compartir),
                        tint = if (currentView != ReplayView.MAIN) Color(0xFFFA243C) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun ReplaySongRow(
    rank: Int,
    song: SongStat,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Rank number
        Text(
            text = "$rank",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp),
            textAlign = TextAlign.Center
        )

        // Thumbnail
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.thumbnail)
                .crossfade(true)
                .build(),
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
        )

        // Title and artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.plays} reproducciones",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3-dots menu
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF1E1E20))
            ) {
                DropdownMenuItem(
                    text = { Text("Reproducir", color = Color.White) },
                    onClick = {
                        showMenu = false
                        onClick()
                    }
                )
            }
        }
    }
}

/**
 * 3D Metallic chrome dial badge (speedometer style) for "Minutos reproducidos"
 */
@Composable
private fun MetallicSpeedometerBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f
            val ringThickness = outerRadius * 0.16f

            // Outer chrome metallic ring
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFF8E8E93),
                        Color(0xFFE5E5EA),
                        Color(0xFF48484A),
                        Color(0xFFD1D1D6),
                        Color(0xFFFFFFFF)
                    ),
                    center = center
                ),
                radius = outerRadius,
                center = center
            )

            // Inner dark dial face
            val innerRadius = outerRadius - ringThickness
            drawCircle(
                color = Color(0xFF161618),
                radius = innerRadius,
                center = center
            )

            // 12 clock / speedometer tick marks
            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30.0) - 90.0)
                val startR = innerRadius - (outerRadius * 0.06f)
                val endR = innerRadius - (outerRadius * 0.18f)

                val startX = (center.x + startR * kotlin.math.cos(angle)).toFloat()
                val startY = (center.y + startR * kotlin.math.sin(angle)).toFloat()
                val endX = (center.x + endR * kotlin.math.cos(angle)).toFloat()
                val endY = (center.y + endR * kotlin.math.sin(angle)).toFloat()

                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.5f
                )
            }
        }

        // Center badge text
        Text(
            text = text,
            color = Color(0xFFE0E0E0),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 3D Metallic chrome circular badge with diamond/star emblem for "Artistas reproducidos"
 */
@Composable
private fun MetallicStarBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f
            val ringThickness = outerRadius * 0.16f

            // Outer chrome metallic ring
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFF9E9E9E),
                        Color(0xFFF2F2F7),
                        Color(0xFF3A3A3C),
                        Color(0xFFD1D1D6),
                        Color(0xFFFFFFFF)
                    ),
                    center = center
                ),
                radius = outerRadius,
                center = center
            )

            // Inner dark dial face
            val innerRadius = outerRadius - ringThickness
            drawCircle(
                color = Color(0xFF161618),
                radius = innerRadius,
                center = center
            )

            // 4-pointed metallic diamond star outline
            val starSpan = innerRadius * 0.85f
            val starPath = Path().apply {
                moveTo(center.x, center.y - starSpan)
                quadraticTo(center.x, center.y, center.x + starSpan, center.y)
                quadraticTo(center.x, center.y, center.x, center.y + starSpan)
                quadraticTo(center.x, center.y, center.x - starSpan, center.y)
                quadraticTo(center.x, center.y, center.x, center.y - starSpan)
                close()
            }

            drawPath(
                path = starPath,
                color = Color.White.copy(alpha = 0.35f),
                style = Stroke(width = 2.5f)
            )
        }

        // Center badge text
        Text(
            text = text,
            color = Color(0xFFE0E0E0),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}
