package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import android.content.Intent
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.data.Playlist
import com.mrtdk.liquid_glass.data.PlaybackRecord
import com.mrtdk.glass.GlassContainer
import com.mrtdk.glass.GlassBox
import com.mrtdk.liquid_glass.ui.components.LocalBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.lens
import com.kyant.shapes.Capsule
import androidx.compose.material.icons.filled.IosShare
import com.mrtdk.liquid_glass.utils.CoilUtils
import kotlinx.coroutines.delay
import java.util.Locale

// Data structures for Replay Stats
data class ArtistStat(val id: String, val name: String, val thumbnail: String?, val minutes: Int)
data class SongStat(val id: String, val title: String, val artist: String, val thumbnail: String?, val plays: Int)
data class AlbumStat(val id: String, val title: String, val artist: String, val thumbnail: String?, val minutes: Int)
data class ComparisonData(
    val artist2026: ArtistStat,
    val artist2025: ArtistStat,
    val song2026: SongStat,
    val song2025: SongStat
)
data class PlaylistStat(val id: String, val title: String, val author: String, val thumbnail: String?, val minutes: Int)
data class GenreStat(val name: String, val percentage: Int, val minutes: Int)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReplayScreen(
    onBack: () -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    onArtistSelected: (ArtistState) -> Unit,
    onAlbumSelected: (AlbumState) -> Unit,
    onPlaylistSelected: (Playlist) -> Unit
) {
    val context = LocalContext.current
    val recentlyPlayed by LibraryManager.recentlyPlayed.collectAsState()
    
    // Select Month and Year State
    var selectedMonthIndex by remember { mutableStateOf(0) }
    var selectedYear by remember { mutableStateOf("2026") }
    var showYearDropdown by remember { mutableStateOf(false) }
    
    // Story reel visibility
    var showHighlightsReel by remember { mutableStateOf(false) }

    // Locale Month Names
    val isEnglish = Locale.getDefault().language == "en"
    val months = if (isEnglish) {
        listOf(selectedYear, "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    } else {
        listOf(selectedYear, "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    }

    // Helper to calculate statistics scaling factor depending on the selected month
    val scaleFactor = if (selectedMonthIndex == 0) 1.0 else 0.08 + (0.04 * (selectedMonthIndex % 3))

    var playbackHistory by remember { mutableStateOf<List<PlaybackRecord>>(emptyList()) }
    LaunchedEffect(Unit) {
        playbackHistory = LibraryManager.getPlaybackHistory()
    }

    val filteredHistory = remember(playbackHistory, selectedMonthIndex, selectedYear) {
        val calendar = java.util.Calendar.getInstance()
        playbackHistory.filter { record ->
            calendar.timeInMillis = record.timestamp
            val recordYear = calendar.get(java.util.Calendar.YEAR).toString()
            val yearMatches = recordYear == selectedYear
            
            if (!yearMatches) return@filter false
            
            if (selectedMonthIndex == 0) {
                true
            } else {
                val recordMonth = calendar.get(java.util.Calendar.MONTH) // 0-indexed
                val targetMonth = selectedMonthIndex + 4
                recordMonth == targetMonth
            }
        }
    }

    val hasEnoughMusic = playbackHistory.size >= 5

    // Blended Stats calculation (User database + package reference data)
    val artistsList = remember(filteredHistory) {
        filteredHistory
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
    }

    val songsList = remember(filteredHistory) {
        filteredHistory
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
    }

    val albumsList = remember(filteredHistory) {
        filteredHistory
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
    }

    val playlistsList = remember(filteredHistory) {
        filteredHistory
            .filter { it.playlistId != null && it.playlistName != null }
            .groupBy { it.playlistId }
            .map { (playlistId, records) ->
                val first = records.first()
                PlaylistStat(
                    id = playlistId ?: "",
                    title = first.playlistName ?: "",
                    author = "RayMusic User",
                    thumbnail = first.thumbnail,
                    minutes = records.size * 3
                )
            }
            .sortedByDescending { it.minutes }
    }

    val totalHistoryMinutes = filteredHistory.size * 3
    val genresList = remember(totalHistoryMinutes) {
        if (totalHistoryMinutes == 0) emptyList()
        else listOf(
            GenreStat(if (isEnglish) "Spanish Pop" else "Pop en español", 45, (totalHistoryMinutes * 0.45).toInt()),
            GenreStat(if (isEnglish) "Latin Urban" else "Urbano latino", 35, (totalHistoryMinutes * 0.35).toInt()),
            GenreStat(if (isEnglish) "Latin Music" else "Música latina", 20, (totalHistoryMinutes * 0.20).toInt())
        )
    }

    val comparisonStats = remember(playbackHistory) {
        val calendar = java.util.Calendar.getInstance()
        
        fun getTopArtistForYear(year: String): ArtistStat? {
            val yearRecords = playbackHistory.filter { record ->
                calendar.timeInMillis = record.timestamp
                calendar.get(java.util.Calendar.YEAR).toString() == year
            }
            val topArtist = yearRecords
                .groupBy { it.artist }
                .maxByOrNull { it.value.size }
                ?.value?.firstOrNull()
            return topArtist?.let {
                ArtistStat(
                    id = it.songId,
                    name = it.artist,
                    thumbnail = it.thumbnail,
                    minutes = yearRecords.filter { r -> r.artist == it.artist }.size * 3
                )
            }
        }

        fun getTopSongForYear(year: String): SongStat? {
            val yearRecords = playbackHistory.filter { record ->
                calendar.timeInMillis = record.timestamp
                calendar.get(java.util.Calendar.YEAR).toString() == year
            }
            val topSong = yearRecords
                .groupBy { it.songId }
                .maxByOrNull { it.value.size }
                ?.value?.firstOrNull()
            return topSong?.let {
                SongStat(
                    id = it.songId,
                    title = it.title,
                    artist = it.artist,
                    thumbnail = it.thumbnail,
                    plays = yearRecords.filter { r -> r.songId == it.songId }.size
                )
            }
        }

        val artist2026 = getTopArtistForYear("2026")
        val artist2025 = getTopArtistForYear("2025")
        val song2026 = getTopSongForYear("2026")
        val song2025 = getTopSongForYear("2025")
        
        val finalArtist2026 = artist2026 ?: ArtistStat("mock_2026_art", "Morat", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=100&fit=crop", 14436)
        val finalArtist2025 = artist2025 ?: ArtistStat("mock_2025_art", "Morat", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=100&fit=crop", 15999)
        val finalSong2026 = song2026 ?: SongStat("mock_2026_song", "Sin Ti", "Morat", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=100&fit=crop", 401)
        val finalSong2025 = song2025 ?: SongStat("mock_2025_song", "Domingo De Bajón", "Morat", "https://images.unsplash.com/photo-1511735111819-9a3f7709049c?w=100&fit=crop", 380)
        
        ComparisonData(finalArtist2026, finalArtist2025, finalSong2026, finalSong2025)
    }

    val monthlyArtists = remember(playbackHistory, selectedYear) {
        val calendar = java.util.Calendar.getInstance()
        val monthsRange = 5..11
        val monthNames = if (isEnglish) {
            listOf("JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER")
        } else {
            listOf("JUNIO", "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE")
        }
        
        monthsRange.mapIndexed { idx, calendarMonth ->
            val monthRecords = playbackHistory.filter { record ->
                calendar.timeInMillis = record.timestamp
                calendar.get(java.util.Calendar.YEAR).toString() == selectedYear &&
                        calendar.get(java.util.Calendar.MONTH) == calendarMonth
            }
            val topArtistForMonth = monthRecords
                .groupBy { it.artist }
                .maxByOrNull { it.value.size }
                ?.value?.firstOrNull()
            
            val artistStat = topArtistForMonth?.let {
                ArtistStat(
                    id = it.songId,
                    name = it.artist,
                    thumbnail = it.thumbnail,
                    minutes = monthRecords.filter { r -> r.artist == it.artist }.size * 3
                )
            }
            Pair(monthNames[idx], artistStat)
        }.filter { it.second != null }
    }

    val monthlySongs = remember(playbackHistory, selectedYear) {
        val calendar = java.util.Calendar.getInstance()
        val monthsRange = 5..11
        val monthNames = if (isEnglish) {
            listOf("JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER")
        } else {
            listOf("JUNIO", "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE")
        }
        
        monthsRange.mapIndexed { idx, calendarMonth ->
            val monthRecords = playbackHistory.filter { record ->
                calendar.timeInMillis = record.timestamp
                calendar.get(java.util.Calendar.YEAR).toString() == selectedYear &&
                        calendar.get(java.util.Calendar.MONTH) == calendarMonth
            }
            val topSongForMonth = monthRecords
                .groupBy { it.songId }
                .maxByOrNull { it.value.size }
                ?.value?.firstOrNull()
            
            val songStat = topSongForMonth?.let {
                SongStat(
                    id = it.songId,
                    title = it.title,
                    artist = it.artist,
                    thumbnail = it.thumbnail,
                    plays = monthRecords.filter { r -> r.songId == it.songId }.size
                )
            }
            Triple(monthNames[idx], songStat, false)
        }.filter { it.second != null }
    }

    val monthlyAlbums = remember(playbackHistory, selectedYear) {
        val calendar = java.util.Calendar.getInstance()
        val monthsRange = 5..11
        val monthNames = if (isEnglish) {
            listOf("JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER")
        } else {
            listOf("JUNIO", "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE")
        }
        
        monthsRange.mapIndexed { idx, calendarMonth ->
            val monthRecords = playbackHistory.filter { record ->
                calendar.timeInMillis = record.timestamp
                calendar.get(java.util.Calendar.YEAR).toString() == selectedYear &&
                        calendar.get(java.util.Calendar.MONTH) == calendarMonth
            }
            val topAlbumForMonth = monthRecords
                .filter { it.album != null }
                .groupBy { it.album }
                .maxByOrNull { it.value.size }
                ?.value?.firstOrNull()
            
            val albumStat = topAlbumForMonth?.let {
                AlbumStat(
                    id = it.songId,
                    title = it.album ?: "",
                    artist = it.artist,
                    thumbnail = it.thumbnail,
                    minutes = monthRecords.filter { r -> r.album == it.album }.size * 3
                )
            }
            Triple(monthNames[idx], albumStat, false)
        }.filter { it.second != null }
    }

    val localBackdrop = rememberLayerBackdrop()
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val topBarAlpha by animateFloatAsState(
        targetValue = if (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 20) 0.85f else 0f,
        label = "topBarAlpha"
    )
    GlassContainer(
        modifier = Modifier.fillMaxSize(),
        useShader = true,
        content = {
            if (!hasEnoughMusic) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF2F2F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFF9500).copy(alpha = 0.25f), // Peach Orange
                                        Color(0xFFFFCC00).copy(alpha = 0.20f), // Yellow
                                        Color(0xFF4CD964).copy(alpha = 0.12f), // Green
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Replay '26",
                                color = Color.Black,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isEnglish) 
                                    "Listen to at least 5 songs to unlock your personalized music history."
                                else 
                                    "Escucha al menos 5 canciones para desbloquear tu historia musical.",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.05f))
                            ) {
                                val progress = (playbackHistory.size / 5f).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFFF9500),
                                                    Color(0xFF4CD964)
                                                )
                                            )
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${playbackHistory.size} / 5",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF2F2F7))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFF9500).copy(alpha = 0.25f), // Peach Orange
                                        Color(0xFFFFCC00).copy(alpha = 0.20f), // Yellow
                                        Color(0xFF4CD964).copy(alpha = 0.12f), // Green
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(localBackdrop),
                        contentPadding = PaddingValues(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 70.dp,
                            bottom = 120.dp
                        )
                    ) {

            // Row 2: Title and Year Dropdown Inline
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.replay_screen_title),
                        color = Color.Black,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE5E5EA))
                                .clickable { showYearDropdown = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedYear,
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "▼",
                                color = Color.Black.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }

                        DropdownMenu(
                            expanded = showYearDropdown,
                            onDismissRequest = { showYearDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("2026", color = Color.Black) },
                                onClick = {
                                    selectedYear = "2026"
                                    showYearDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("2025", color = Color.Black) },
                                onClick = {
                                    selectedYear = "2025"
                                    showYearDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Row 3: Month Selection (plain text, no capsule backgrounds)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                ) {
                    itemsIndexed(months) { index, month ->
                        val isSelected = selectedMonthIndex == index
                        Box(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedMonthIndex = index }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = month,
                                color = if (isSelected) Color.Black else Color(0xFF8E8E93),
                                fontSize = 17.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Featured Reel Cover Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .aspectRatio(1f) // Square card like screenshots
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF9500), // Yellow/orange
                                    Color(0xFFFFCC00),
                                    Color(0xFF4CD964)  // Green
                                )
                            )
                        )
                        .clickable { showHighlightsReel = true }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = stringResource(R.string.replay_revive_this_year),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 38.sp
                        )

                        // Highlights Reel Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Reel",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.replay_highlights_reel),
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(36.dp))
            }

            // Top Artists Section
            if (artistsList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.replay_top_artists),
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier
                                .size(16.dp)
                                .scale(-1f, 1f) // Flip horizontally for ChevronRight behavior
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(artistsList.take(5)) { index, artist ->
                            Box(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(230.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        onArtistSelected(ArtistState(artist.id, artist.name, artist.thumbnail))
                                    }
                            ) {
                                // Background blur or subtle layout
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = Color.Black.copy(alpha = 0.5f),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(artist.thumbnail)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = artist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                    )

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = artist.name,
                                            color = Color.Black,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.replay_minutos, String.format(Locale.getDefault(), "%,d", artist.minutes)),
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Top Songs Section
            if (songsList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.replay_top_songs),
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier
                                .size(16.dp)
                                .scale(-1f, 1f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        songsList.take(5).forEachIndexed { index, song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSongSelected(PlayerState(
                                            title = song.title,
                                            artist = song.artist,
                                            artUrl = song.thumbnail,
                                            videoId = song.id.takeIf { !it.startsWith("mock") }
                                        ))
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(24.dp)
                                )

                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(song.thumbnail)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = song.title,
                                        color = Color.Black,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = stringResource(R.string.replay_reproducciones, song.plays),
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Top Albums Section
            if (albumsList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.replay_top_albums),
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier
                                .size(16.dp)
                                .scale(-1f, 1f)
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(albumsList) { index, album ->
                            Column(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable {
                                        onAlbumSelected(AlbumState(
                                            id = "replay_album_${album.title}",
                                            playlistId = "replay_album_${album.title}",
                                            title = album.title,
                                            artist = album.artist,
                                            thumbnail = album.thumbnail
                                        ))
                                    }
                            ) {
                                Box(modifier = Modifier.size(130.dp)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(album.thumbnail)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = album.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    )
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = album.title,
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = album.artist,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.replay_minutos, String.format(Locale.getDefault(), "%,d", album.minutes)),
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Top Playlists Section
            if (playlistsList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.replay_top_playlists),
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier
                                .size(16.dp)
                                .scale(-1f, 1f)
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(playlistsList) { index, playlist ->
                            Column(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable {
                                        onAlbumSelected(AlbumState(
                                            id = playlist.id,
                                            playlistId = playlist.id,
                                            title = playlist.title,
                                            artist = playlist.author,
                                            thumbnail = playlist.thumbnail
                                        ))
                                    }
                            ) {
                                Box(modifier = Modifier.size(130.dp)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(playlist.thumbnail)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = playlist.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    )
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = playlist.title,
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = playlist.author,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.replay_minutos, String.format(Locale.getDefault(), "%,d", playlist.minutes)),
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Top Genres Section
            if (genresList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.replay_top_genres),
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFA243C), // Apple Music Red/Pink
                                        Color(0xFFFF9500)  // Orange
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = genresList.first().name,
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 38.sp
                            )
                            
                            genresList.drop(1).take(2).forEach { genre ->
                                Text(
                                    text = genre.name,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.replay_minutos, String.format(Locale.getDefault(), "%,d", genresList.sumOf { it.minutes })),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // NEW SECTIONS: MONTHLY BREAKDOWNS, COMPARISON & LAST YEAR
            // ═══════════════════════════════════════════════════════════

            // 1. Tus top artistas por mes
            item {
                if (monthlyArtists.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(36.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.replay_top_artists_by_month),
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier
                                .size(16.dp)
                                .scale(-1f, 1f)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(monthlyArtists) { index, itemData ->
                            val (month, artist) = itemData
                            if (artist != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .clickable {
                                            onArtistSelected(ArtistState(artist.id, artist.name, artist.thumbnail))
                                        }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(artist.thumbnail)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = artist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color.Black.copy(alpha = 0.05f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = month,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = artist.name,
                                            color = Color.Black,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Top",
                                            tint = Color(0xFFFA243C),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Tus top canciones por mes
            item {
                if (monthlySongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(36.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.replay_top_songs_by_month),
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier
                                .size(16.dp)
                                .scale(-1f, 1f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        monthlySongs.forEach { (month, song, isExplicit) ->
                            if (song != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSongSelected(PlayerState(
                                                title = song.title,
                                                artist = song.artist,
                                                artUrl = song.thumbnail,
                                                videoId = null
                                            ))
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(song.thumbnail)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = month,
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = song.title,
                                                color = Color.Black,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                        }
                                        Text(
                                            text = song.artist,
                                            color = Color.Gray,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Download icon
                                    IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Downloaded",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    
                                    // More option
                                    IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    // Favorite star
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Favorite",
                                        tint = Color(0xFFFA243C),
                                        modifier = Modifier.size(14.dp).padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Tus top álbumes por mes
            item {
                if (monthlyAlbums.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(36.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.replay_top_albums_by_month),
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier
                                .size(16.dp)
                                .scale(-1f, 1f)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(monthlyAlbums) { index, itemData ->
                            val (month, album, isExplicit) = itemData
                            if (album != null) {
                                Column(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable {
                                            onAlbumSelected(AlbumState(
                                                id = "replay_album_${album.title}",
                                                playlistId = "replay_album_${album.title}",
                                                title = album.title,
                                                artist = album.artist,
                                                thumbnail = album.thumbnail
                                            ))
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
                                            .size(130.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = month,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = album.title,
                                            color = Color.Black,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                    Text(
                                        text = album.artist,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            /*
            // 4. Comparación con el año pasado
            item {
                Spacer(modifier = Modifier.height(36.dp))
                Text(
                    text = stringResource(R.string.replay_comparison_title),
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Artist Comparison Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.replay_comparison_artists),
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Year 2026
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("2026", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Morat", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("14,436 min", color = Color.Gray, fontSize = 11.sp)
                                }
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=100&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Year 2025
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("2025", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Morat", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("15,999 min", color = Color.Gray, fontSize = 11.sp)
                                }
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=100&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                )
                            }
                        }
                    }

                    // Song Comparison Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.replay_comparison_songs),
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Year 2026
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("2026", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Sin Ti", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("401 plays", color = Color.Gray, fontSize = 11.sp)
                                }
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=100&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Year 2025
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("2025", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Domingo De...", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("380 plays", color = Color.Gray, fontSize = 11.sp)
                                }
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1511735111819-9a3f7709049c?w=100&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                                )
                            }
                        }
                    }
                }
            }
            */

            // 5. Revive tus top canciones de 2025
            item {
                Spacer(modifier = Modifier.height(36.dp))
                Text(
                    text = stringResource(R.string.replay_revive_last_year_songs),
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF9500), // Yellow/orange
                                    Color(0xFF4CD964)  // Green
                                )
                            )
                        )
                        .clickable {
                            val playlistSongs = songsList.map { song ->
                                LibraryItem(
                                    id = song.id,
                                    title = song.title,
                                    subtitle = song.artist,
                                    thumbnail = song.thumbnail,
                                    type = ItemType.SONG
                                )
                            }
                            val replayPlaylist = Playlist(
                                id = "replay_2026",
                                name = "Replay 2026",
                                items = playlistSongs,
                                coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&fit=crop"
                            )
                            onPlaylistSelected(replayPlaylist)
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Replay",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "\'26",
                                color = Color.White,
                                fontSize = 84.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 80.sp
                            )
                        }

                        Text(
                            text = "Morat, Bad Bunny, Danny Ocean, Aitana, The Police, Quevedo y más",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

                }
            }

            // FULL SCREEN HIGHLIGHTS STORIES REEL
            AnimatedVisibility(
                visible = showHighlightsReel,
                enter = fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.9f),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.9f)
            ) {
                HighlightsReelDialog(
                    artists = artistsList,
                    songs = songsList,
                    genres = genresList,
                    year = selectedYear,
                    onClose = { showHighlightsReel = false }
                )
            }
        },
        glassContent = {
            val scope = this
            if (!showHighlightsReel) {
                // Fixed Top Bar (Stays at the top, only contains back & share buttons)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Back button
                    scope.GlassBox(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable { onBack() },
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
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Share button
                    scope.GlassBox(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${context.getString(R.string.share_playlist_prefix)} RayMusic Replay $selectedYear!")
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
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════
// HIGHLIGHTS REEL DIALOG (Instagram-style stories)
// ═══════════════════════════════════════════════════════════
@Composable
fun HighlightsReelDialog(
    artists: List<ArtistStat>,
    songs: List<SongStat>,
    genres: List<GenreStat>,
    year: String = "2026",
    onClose: () -> Unit
) {
    val totalSlides = 4
    var currentSlide by remember { mutableStateOf(0) }
    var slideProgress by remember { mutableStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    // Timer effect
    LaunchedEffect(currentSlide, isPaused) {
        if (isPaused) return@LaunchedEffect
        slideProgress = 0f
        val steps = 100
        val stepMs = 40L // 40ms * 100 = 4 seconds per slide
        for (i in 1..steps) {
            delay(stepMs)
            slideProgress = i / 100f
        }
        if (currentSlide < totalSlides - 1) {
            currentSlide++
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background subtle abstract animation/glow depending on slide
        val bgGradient = when (currentSlide) {
            0 -> Brush.radialGradient(colors = listOf(Color(0xFFFF9500).copy(alpha = 0.4f), Color.Black), radius = 1200f)
            1 -> Brush.radialGradient(colors = listOf(Color(0xFF5856D6).copy(alpha = 0.4f), Color.Black), radius = 1200f)
            2 -> Brush.radialGradient(colors = listOf(Color(0xFFFF2D55).copy(alpha = 0.4f), Color.Black), radius = 1200f)
            else -> Brush.radialGradient(colors = listOf(Color(0xFF4CD964).copy(alpha = 0.4f), Color.Black), radius = 1200f)
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
        )

        // Slide Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp, bottom = 48.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (currentSlide) {
                0 -> {
                    // Slide 1: Minutos totales
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = year,
                            color = Color(0xFFFF9500),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (Locale.getDefault().language == "en") "Your total listening time was" else "Tus minutos totales escuchados",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        val totalMinutes = artists.sumOf { it.minutes }
                        Text(
                            text = String.format(Locale.getDefault(), "%,d", totalMinutes),
                            color = Color.White,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (Locale.getDefault().language == "en") "minutes" else "minutos",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                1 -> {
                    // Slide 2: Artista top
                    val topArtist = artists.firstOrNull() ?: ArtistStat("art_top", "Morat", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&fit=crop", 14436)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (Locale.getDefault().language == "en") "YOUR TOP ARTIST" else "TU ARTISTA TOP",
                            color = Color(0xFF5856D6),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        AsyncImage(
                            model = topArtist.thumbnail,
                            contentDescription = topArtist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(240.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color(0xFF5856D6), CircleShape)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = topArtist.name,
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = String.format(Locale.getDefault(), if (Locale.getDefault().language == "en") "%,d minutes listened" else "%,d minutos escuchados", topArtist.minutes),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                2 -> {
                    // Slide 3: Canción top
                    val topSong = songs.firstOrNull() ?: SongStat("song_top", "Sin Ti", "Morat", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&fit=crop", 401)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (Locale.getDefault().language == "en") "YOUR TOP SONG" else "TU CANCIÓN TOP",
                            color = Color(0xFFFF2D55),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        AsyncImage(
                            model = topSong.thumbnail,
                            contentDescription = topSong.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(4.dp, Color(0xFFFF2D55), RoundedCornerShape(20.dp))
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = topSong.title,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = topSong.artist,
                            color = Color.Gray,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = String.format(Locale.getDefault(), if (Locale.getDefault().language == "en") "%,d plays" else "%,d reproducciones", topSong.plays),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                else -> {
                    // Slide 4: Géneros top
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (Locale.getDefault().language == "en") "YOUR TOP GENRES" else "TUS GÉNEROS TOP",
                            color = Color(0xFF4CD964),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(36.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            genres.take(3).forEachIndexed { idx, genre ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(genre.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("${genre.percentage}%", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    // Simulated bar progress
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.1f))
                                    ) {
                                        val animatedWidth = remember { Animatable(0f) }
                                        LaunchedEffect(Unit) {
                                            animatedWidth.animateTo(
                                                targetValue = genre.percentage / 100f,
                                                animationSpec = tween(1000, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(animatedWidth.value)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xFF4CD964),
                                                            Color(0xFF5AC8FA)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tap Left/Right Areas to Switch Slides
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentSlide > 0) currentSlide--
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentSlide < totalSlides - 1) {
                            currentSlide++
                        } else {
                            onClose()
                        }
                    }
            )
        }

        // Top Segmented Progress Bar & Close Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0 until totalSlides) {
                    val progress = when {
                        i < currentSlide -> 1f
                        i > currentSlide -> 0f
                        else -> slideProgress
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Replay $year",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
