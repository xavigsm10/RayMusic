package com.mrtdk.liquid_glass.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.res.painterResource
import com.mrtdk.liquid_glass.R
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import com.echo.innertube.YouTube
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlayerState(
    val title: String,
    val artist: String,
    val artUrl: Any?,
    val videoId: String? = null,
    val contentUri: Uri? = null,
    val duration: Long = 0L,
    val queue: List<QueueItem> = emptyList()
)

data class QueueItem(
    val title: String,
    val artist: String,
    val artUrl: Any?,
    val videoId: String? = null
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerState: PlayerState?,
    isVisible: Boolean,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isBottomBarCollapsed: Boolean = false,
    upNextSongs: List<SongItem> = emptyList(),
    onUpNextSongsChange: (List<SongItem>) -> Unit = {},
    songHistory: List<PlayerState> = emptyList(),
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onArtistSelected: (com.mrtdk.liquid_glass.ui.screens.ArtistState) -> Unit = {},
    onSongSelected: (PlayerState) -> Unit = {}
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
    ) {
        if (playerState == null) return@AnimatedVisibility

        val context = LocalContext.current
        var volumePosition by remember { mutableFloatStateOf(0.7f) }
        val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
        var offsetY by remember { mutableFloatStateOf(0f) }
        var showQueue by remember { mutableStateOf(false) }
        var showLyrics by remember { mutableStateOf(false) }
        
        var showOptionsMenu by remember { mutableStateOf(false) }
        var showLyricsMenu by remember { mutableStateOf(false) }
        var showPlaylistMenu by remember { mutableStateOf(false) }
        var showNewPlaylistDialog by remember { mutableStateOf(false) }
        
        var swipeDirection by remember { mutableIntStateOf(1) }
        val hdArtUrl = remember(playerState?.artUrl) {
            val url = playerState?.artUrl
            if (url is String) {
                when {
                    url.contains("=w") -> url.substringBefore("=w") + "=w1200-h1200-l90-rj"
                    url.contains("ytimg.com/vi/") -> url
                        .replace("hqdefault", "maxresdefault")
                        .replace("mqdefault", "maxresdefault")
                    else -> url
                }
            } else url
        }

        var dominantColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }

        var lyricsLines by remember { mutableStateOf<List<com.mrtdk.liquid_glass.utils.LyricLine>?>(null) }
        var showManualLyricsSearch by remember { mutableStateOf(false) }
        var manualLyricsQueryTitle by remember { mutableStateOf(playerState?.title ?: "") }
        var manualLyricsQueryArtist by remember { mutableStateOf(playerState?.artist ?: "") }

        var selectedLyricsProvider by remember { mutableStateOf("LRCLIB") }
        var isRomajiEnabled by remember { mutableStateOf(false) }

        LaunchedEffect(playerState?.title, playerState?.artist, selectedLyricsProvider, isRomajiEnabled) {
            lyricsLines = null
            if (playerState != null) {
                launch {
                    val lines = when (selectedLyricsProvider) {
                        "KuGou" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchKuGouLyrics(playerState.title, playerState.artist)
                        "BetterLyrics" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchBetterLyrics(playerState.title, playerState.artist)
                        "LyricsPlus" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchLyricsPlus(playerState.title, playerState.artist)
                        "SimpMusic" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchSimpMusicLyrics(playerState.title, playerState.artist)
                        "YouTube Music" -> playerState.videoId?.let { com.mrtdk.liquid_glass.utils.LyricsProvider.fetchYouTubeLyrics(it) }
                        "YouTube Subtitle" -> playerState.videoId?.let { com.mrtdk.liquid_glass.utils.LyricsProvider.fetchYouTubeSubtitleLyrics(it) }
                        else -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchLyrics(playerState.title, playerState.artist)
                    }
                    if (isRomajiEnabled && lines != null) {
                        val prefs = com.mrtdk.liquid_glass.utils.LyricsRomanizationPreferences(true, true, true, true, true)
                        lyricsLines = lines.map { line ->
                            val romanized = com.mrtdk.liquid_glass.utils.LyricsUtils.romanizeLyricsLine(line.text, prefs)
                            if (romanized != null) {
                                line.copy(text = romanized)
                            } else {
                                line
                            }
                        }
                    } else {
                        lyricsLines = lines
                    }
                }
            }
        }

        LaunchedEffect(hdArtUrl) {
            if (hdArtUrl != null) {
                val request = ImageRequest.Builder(context)
                    .data(hdArtUrl)
                    .allowHardware(false)
                    .size(200)
                    .build()
                val result = coil.Coil.imageLoader(context).execute(request)
                if (result is coil.request.SuccessResult) {
                    val drawable = result.drawable
                    val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        ?: android.graphics.Bitmap.createBitmap(
                            drawable.intrinsicWidth.coerceAtLeast(1),
                            drawable.intrinsicHeight.coerceAtLeast(1),
                            android.graphics.Bitmap.Config.ARGB_8888
                        ).also {
                            val canvas = android.graphics.Canvas(it)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                        }
                    if (bitmap != null) {
                        try {
                            val colorInt = bitmap.getPixel(bitmap.width / 2, bitmap.height - 1)
                            dominantColor = Color(colorInt)
                        } catch (e: Exception) { }
                    }
                }
            }
        }
        
        val maxDragDistance = 250f 
        val dragProgress = (offsetY / maxDragDistance).coerceIn(0f, 1f)
        val imageScale = 1f - (dragProgress * 0.85f).coerceIn(0f, 1f)
        val cornerRadius = (12 + (dragProgress * 180)).dp 
        val bgAlpha = 1f - dragProgress
        val contentAlpha = 1f - (dragProgress * 2.5f).coerceIn(0f, 1f)

        val scrollState = rememberScrollState()

        val isLightBackground = dominantColor.luminance() > 0.6f
        val contentColor = if (isLightBackground) Color(0xFF1E1E1E) else Color.White

        val savedItems by LibraryManager.savedItems.collectAsState()
        val isSaved = savedItems.any { it.id == playerState?.videoId }
        val starTint by androidx.compose.animation.animateColorAsState(targetValue = if(isSaved) Color(0xFFFA243C) else contentColor, label="starTint")

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    if (offsetY > 0f && delta < 0f) {
                        val newOffset = (offsetY + delta).coerceAtLeast(0f)
                        val consumed = newOffset - offsetY
                        offsetY = newOffset
                        return Offset(0f, consumed)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val delta = available.y
                    if (delta > 0f && scrollState.value == 0) {
                        offsetY = (offsetY + delta * 0.7f).coerceAtLeast(0f)
                        return Offset(0f, delta)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (offsetY > 150f) {
                        onClose()
                    } else {
                        offsetY = 0f
                    }
                    return Velocity.Zero
                }
            }
        }

        // Contenedor principal anclado (sin mover) pero desvaneciéndose
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(dominantColor.copy(alpha = bgAlpha))
                .pointerInput(showLyrics, showQueue) {
                    if (!showLyrics && !showQueue) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (offsetY > 150f) {
                                    onClose()
                                } else {
                                    offsetY = 0f
                                }
                            }
                        ) { change, dragAmount ->
                            if (dragAmount > 0f || offsetY > 0f) {
                                offsetY = (offsetY + dragAmount * 0.7f).coerceAtLeast(0f)
                            }
                        }
                    }
                }
        ) {
            val maxWidth = maxWidth
            val maxHeight = maxHeight

            val lyricsImageSize = 84.dp
            val isOverlayActive = showLyrics || showQueue
            
            // Calculating destinations depending on normal vs collapsed bottom bar
            val normalTargetOffsetX = 28.dp
            val collapsedTargetOffsetX = 92.dp
            val normalTargetOffsetY = maxHeight - 148.dp
            val collapsedTargetOffsetY = maxHeight - 64.dp
            
            val targetOffsetX = if (isBottomBarCollapsed) collapsedTargetOffsetX else normalTargetOffsetX
            val targetOffsetY = if (isBottomBarCollapsed) collapsedTargetOffsetY else normalTargetOffsetY
            
            val imgWidthTarget = if (isOverlayActive) lyricsImageSize 
                else androidx.compose.ui.unit.lerp(maxWidth, 40.dp, dragProgress)
            val imgHeightTarget = if (isOverlayActive) lyricsImageSize 
                else androidx.compose.ui.unit.lerp(maxWidth / 0.92f, 40.dp, dragProgress)
                
            val imgOffsetXTarget = if (isOverlayActive) 24.dp 
                else androidx.compose.ui.unit.lerp(0.dp, targetOffsetX, dragProgress)
            val imgOffsetYTarget = if (isOverlayActive) 64.dp 
                else androidx.compose.ui.unit.lerp(0.dp, targetOffsetY, dragProgress)

            val imgWidth by androidx.compose.animation.core.animateDpAsState(imgWidthTarget)
            val imgHeight by androidx.compose.animation.core.animateDpAsState(imgHeightTarget)
            val imgOffsetX by androidx.compose.animation.core.animateDpAsState(imgOffsetXTarget)
            val imgOffsetY by androidx.compose.animation.core.animateDpAsState(imgOffsetYTarget)
            val imageCornerTarget = if (isOverlayActive) 8.dp else cornerRadius
            val imgCorner by androidx.compose.animation.core.animateDpAsState(imageCornerTarget)

            // THE MAIN IMAGE
            Box(
                modifier = Modifier
                    .offset(x = imgOffsetX, y = imgOffsetY)
                    .size(width = imgWidth, height = imgHeight)
                    .graphicsLayer {
                        shape = RoundedCornerShape(imgCorner.toPx())
                        clip = true
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(hdArtUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (!isOverlayActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.85f to Color.Transparent,
                                    1f to dominantColor.copy(alpha = contentAlpha)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(modifier = Modifier.width(40.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    }
                }
            }

            // LYRICS / QUEUE OVERLAY
            androidx.compose.animation.AnimatedVisibility(
                visible = showLyrics || showQueue,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                 Column(modifier = Modifier.fillMaxSize()) {                      Row(
                          modifier = Modifier.fillMaxWidth().padding(top = 64.dp, start = 124.dp, end = 24.dp), 
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                           Column(modifier = Modifier.weight(1f).clickable { 
                               if (playerState?.videoId != null) {
                                   onArtistSelected(com.mrtdk.liquid_glass.ui.screens.ArtistState(
                                       id = playerState.artist,
                                       name = playerState.artist,
                                       thumbnail = null
                                   ))
                               }
                           }) {
                               Text(playerState?.title ?: "", color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                               Text(playerState?.artist ?: "", color = contentColor.copy(alpha=0.7f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                           }
                           Icon(Icons.Default.Star, "Fav", tint = starTint, modifier = Modifier.size(24.dp).clickable {
                               if (playerState != null) {
                                   if (!isSaved) {
                                       LibraryManager.saveItem(LibraryItem(playerState.videoId ?: "", playerState.title, playerState.artist, playerState.artUrl?.toString(), ItemType.SONG))
                                   } else {
                                       LibraryManager.removeItem(playerState.videoId ?: "")
                                   }
                               }
                           })
                           Spacer(modifier = Modifier.width(20.dp))
                           Icon(Icons.Default.MoreVert, "More", tint = contentColor, modifier = Modifier.size(24.dp).clickable { 
                               if (showLyrics) {
                                   showLyricsMenu = true
                               } else {
                                   showOptionsMenu = true
                               }
                           })
                      }
                      
                      if (showQueue) {
                          Spacer(modifier = Modifier.height(24.dp))
                          Row(modifier = Modifier.fillMaxWidth().padding(start = 124.dp, end = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                               Box(modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha=0.15f)).clickable{}, contentAlignment=Alignment.Center) {
                                   Icon(painterResource(id = R.drawable.shuffle), "Shuffle", tint=contentColor, modifier=Modifier.size(20.dp))
                               }
                               Box(modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha=0.15f)).clickable{}, contentAlignment=Alignment.Center) {
                                   Icon(painterResource(id = R.drawable.repeat), "Repeat", tint=contentColor, modifier=Modifier.size(20.dp))
                               }
                          }
                           if (playerState != null && playerState.queue.isNotEmpty()) {
                               Text(text = "Siguiente en Album/Playlist", color=contentColor, fontSize=18.sp, fontWeight=FontWeight.Bold, modifier = Modifier.padding(top=32.dp, bottom=16.dp))
                           }
                           Text(text = "Continue Playing", color=contentColor, fontSize=18.sp, fontWeight=FontWeight.Bold, modifier = Modifier.padding(top=32.dp, start=24.dp, end=24.dp, bottom=16.dp))
                          
                          LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                              // 1. Manual Queue Section (Album/Playlist)
                              if (playerState != null && playerState.queue.isNotEmpty()) {
                                  items(playerState.queue.size) { index ->
                                      val qItem = playerState.queue[index]
                                      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                                          swipeDirection = 1
                                          val selectedItem = playerState.queue[index]
                                          val remaining = playerState.queue.drop(index + 1)
                                          val historyToAdd = playerState.queue.take(index).map { q -> PlayerState(q.title, q.artist, q.artUrl, q.videoId) }
                                          // Note: history modification should ideally be handled by parent, but we do onSongSelected
                                          onSongSelected(PlayerState(
                                              title = qItem.title,
                                              artist = qItem.artist,
                                              artUrl = qItem.artUrl,
                                              videoId = qItem.videoId,
                                              queue = remaining
                                          ))
                                      }) {
                                          AsyncImage(model = ImageRequest.Builder(context).data(qItem.artUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
                                          Spacer(modifier = Modifier.width(12.dp))
                                          Column(modifier = Modifier.weight(1f)) {
                                              Text(qItem.title, color = contentColor, fontSize = 16.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                              Text(qItem.artist, color = contentColor.copy(alpha=0.6f), fontSize = 14.sp, maxLines = 1)
                                          }
                                          Icon(Icons.Default.Menu, contentDescription=null, tint=contentColor.copy(alpha=0.6f))
                                      }
                                  }
                              }
                              items(upNextSongs.size) { i ->
                                  val song = upNextSongs[i]
                                  // Upgrade thumbnail to HD for display
                                  val hdThumb = song.thumbnail?.let {
                                      if (it.contains("=w")) it.substringBefore("=w") + "=w540-h540-l90-rj"
                                      else if (it.contains("ytimg.com/vi/")) it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                      else it
                                  } ?: song.thumbnail
                                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                                      swipeDirection = 1
                                      val upgradedArt = song.thumbnail?.let {
                                          if (it.contains("=w")) it.substringBefore("=w") + "=w1200-h1200-l90-rj"
                                          else if (it.contains("ytimg.com/vi/")) it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                                          else it
                                      } ?: song.thumbnail
                                      // Remove clicked song and all before it from the queue
                                      onUpNextSongsChange(upNextSongs.drop(i + 1))
                                      onSongSelected(PlayerState(
                                          title = song.title,
                                          artist = song.artists.joinToString { it.name },
                                          artUrl = upgradedArt,
                                          videoId = song.id
                                      ))
                                  }) {
                                      AsyncImage(model = ImageRequest.Builder(context).data(hdThumb).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
                                      Spacer(modifier = Modifier.width(12.dp))
                                      Column(modifier = Modifier.weight(1f)) {
                                          Text(song.title, color = contentColor, fontSize = 16.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                          Text(song.artists.joinToString { it.name }, color = contentColor.copy(alpha=0.6f), fontSize = 14.sp, maxLines = 1)
                                      }
                                      Icon(Icons.Default.Menu, contentDescription=null, tint=contentColor.copy(alpha=0.6f))
                                  }
                              }
                          }
                      } else if (showLyrics) {
                            Spacer(modifier = Modifier.height(44.dp))
                            Box(modifier = Modifier.weight(1f).clipToBounds()) {
                                 val currentLyricsLines = lyricsLines
                                 if (currentLyricsLines != null && currentLyricsLines.isNotEmpty()) {
                                     val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                                     val isSynced = currentLyricsLines.any { it.timeMs > 0L }
                                     
                                     LaunchedEffect(currentPosition, currentLyricsLines) {
                                         if (!isSynced) return@LaunchedEffect
                                         val currentIdx = currentLyricsLines.indexOfLast { it.timeMs != -1L && it.timeMs <= currentPosition + 500 }
                                         if (currentIdx >= 0 && !listState.isScrollInProgress) {
                                             listState.animateScrollToItem(currentIdx.coerceAtLeast(0), scrollOffset = -100)
                                         }
                                     }
                                     
                                     LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                         item { Spacer(modifier = Modifier.height(60.dp)) }
                                         items(currentLyricsLines.size) { i ->
                                             val line = currentLyricsLines[i]
                                             val isCurrent = isSynced && line.timeMs != -1L && currentPosition >= line.timeMs && 
                                                 (i == currentLyricsLines.lastIndex || currentPosition < currentLyricsLines[i+1].timeMs)
                                             val isPast = isSynced && line.timeMs != -1L && currentPosition > line.timeMs
                                             val distance = if (isSynced) {
                                                 val curIdx = currentLyricsLines.indexOfLast { it.timeMs != -1L && it.timeMs <= currentPosition + 500 }
                                                 if (curIdx >= 0) kotlin.math.abs(i - curIdx) else 0
                                             } else 0
                                             
                                             // Compute word timing for word-by-word gradient fill
                                             val nextLineTime = currentLyricsLines.getOrNull(i + 1)?.timeMs
                                             val lineDuration = remember(line.timeMs, nextLineTime) {
                                                 if (nextLineTime != null && nextLineTime > 0 && line.timeMs > 0) nextLineTime - line.timeMs else 4000L
                                             }
                                             val activeDuration = remember(lineDuration) {
                                                 (lineDuration * 0.95).toLong().coerceAtLeast(300L)
                                             }
                                             val lineRelTime = if (isCurrent && line.timeMs > 0) (currentPosition - line.timeMs).coerceAtLeast(0L) else if (isPast) activeDuration else 0L
                                             
                                             val targetAlpha = when {
                                                 !isSynced || isCurrent -> 1f
                                                 distance == 1 -> 0.55f
                                                 distance == 2 -> 0.4f
                                                 else -> 0.3f
                                             }
                                             val targetScale = when {
                                                 !isSynced || isCurrent -> 1.05f
                                                 distance == 1 -> 0.95f
                                                 distance >= 2 -> 0.85f
                                                 else -> 1f
                                             }
                                             val targetBlur = if (!isCurrent && isSynced) {
                                                 when (distance) {
                                                     1 -> 2.5f
                                                     2 -> 4f
                                                     else -> 6f
                                                 }
                                             } else 0f
                                             
                                             val animAlpha by androidx.compose.animation.core.animateFloatAsState(targetAlpha, animationSpec = tween(260, easing = FastOutSlowInEasing), label="lyricsAlpha")
                                             val animScale by androidx.compose.animation.core.animateFloatAsState(targetScale, animationSpec = tween(320, easing = FastOutSlowInEasing), label="lyricsScale")
                                             val animBlur by androidx.compose.animation.core.animateFloatAsState(targetBlur, animationSpec = tween(420, easing = FastOutSlowInEasing), label="lyricsBlur")
                                             
                                             // Word-by-word data
                                             val wordData = remember(line.text, activeDuration) {
                                                 val words = line.text.split(" ").filter { it.isNotEmpty() }
                                                 if (words.isEmpty()) {
                                                     listOf(Triple(line.text, 0L, activeDuration))
                                                 } else {
                                                     val totalChars = line.text.length
                                                     var accumulatedTime = 0L
                                                     words.mapIndexed { wordIndex, word ->
                                                         val charCount = if (wordIndex < words.lastIndex) word.length + 1 else word.length
                                                         val wordStart = accumulatedTime
                                                         val wordDur = if (totalChars > 0) (activeDuration * charCount.toFloat() / totalChars).toLong() else activeDuration
                                                         accumulatedTime += wordDur
                                                         Triple(if (wordIndex < words.lastIndex) "$word " else word, wordStart, wordStart + wordDur)
                                                     }
                                                 }
                                             }
                                             
                                             // Word-by-word FlowRow rendering
                                             @OptIn(ExperimentalLayoutApi::class)
                                             FlowRow(
                                                 modifier = Modifier.fillMaxWidth()
                                                     .graphicsLayer {
                                                         scaleX = animScale; scaleY = animScale
                                                         alpha = animAlpha
                                                         transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                                     }
                                                     .then(if (animBlur > 0f) Modifier.blur(animBlur.dp) else Modifier)
                                                     .clickable { if(line.timeMs != -1L) onSeek(line.timeMs) },
                                                 horizontalArrangement = Arrangement.Start,
                                                 verticalArrangement = Arrangement.spacedBy(4.dp)
                                             ) {
                                                 wordData.forEach { (wordText, startRelative, endRelative) ->
                                                     val wordDuration = (endRelative - startRelative).coerceAtLeast(1L)
                                                     
                                                     val wordProgress by androidx.compose.animation.core.animateFloatAsState(
                                                         targetValue = when {
                                                             lineRelTime >= endRelative -> 1f
                                                             lineRelTime < startRelative -> 0f
                                                             else -> (lineRelTime - startRelative).toFloat() / wordDuration
                                                         },
                                                         animationSpec = tween(
                                                             durationMillis = wordDuration.coerceIn(140L, 260L).toInt(),
                                                             easing = FastOutSlowInEasing
                                                         ),
                                                         label = "wordProgress"
                                                     )
                                                     
                                                     val finalFontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold
                                                     
                                                     Text(
                                                         text = wordText,
                                                         fontSize = 28.sp,
                                                         style = TextStyle(
                                                             brush = if (isCurrent) Brush.horizontalGradient(
                                                                 0.0f to contentColor,
                                                                 (wordProgress - 0.05f).coerceAtLeast(0f) to contentColor,
                                                                 (wordProgress + 0.05f).coerceAtMost(1f) to contentColor.copy(alpha = 0.4f),
                                                                 1.0f to contentColor.copy(alpha = 0.4f)
                                                             ) else null,
                                                             fontWeight = finalFontWeight,
                                                             lineHeight = 36.sp,
                                                             shadow = if (isCurrent && wordProgress > 0.1f) Shadow(
                                                                 color = contentColor.copy(alpha = 0.6f * wordProgress),
                                                                 offset = Offset.Zero,
                                                                 blurRadius = (12f * wordProgress).coerceAtLeast(0.1f)
                                                             ) else null
                                                         ),
                                                         color = if (!isCurrent) contentColor else Color.Unspecified
                                                     )
                                                 }
                                             }
                                         }
                                         item { Spacer(modifier = Modifier.height(200.dp)) }
                                     }
                                 } else {
                                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                         CircularProgressIndicator(color = contentColor)
                                     }
                                 }
                            }
                       }
                      
                      Box(
                          modifier = Modifier
                              .fillMaxWidth()
                              .background( Brush.verticalGradient(listOf(Color.Transparent, dominantColor.copy(alpha=0.8f), dominantColor, dominantColor)) )
                      ) {
                          Column(
                              modifier = Modifier
                                  .fillMaxWidth()
                                  .padding(horizontal = 24.dp)
                                  .padding(top = 40.dp) // difuminado padding
                          ) {
                              Slider(
                                  value = progress, onValueChange = { onSeek((it * duration).toLong()) },
                                  modifier = Modifier.fillMaxWidth().height(16.dp),
                                  colors = SliderDefaults.colors(
                                      thumbColor = Color.Transparent,
                                      activeTrackColor = contentColor.copy(alpha = 0.9f),
                                      inactiveTrackColor = contentColor.copy(alpha = 0.3f)
                                  )
                              )
                              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                  Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                  Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                              }
                              
                              Spacer(modifier = Modifier.height(32.dp))
                              
                              Box(
                                  modifier = Modifier
                                      .fillMaxWidth()
                                      .padding(bottom = 32.dp)
                              ) {
                                  PlayerBottomControls( // We only want the icons, volume and stuff here, NO PROGRESS
                                      progress = progress, currentPosition = currentPosition, duration = duration,
                                      isPlaying = isPlaying, contentColor = contentColor, volumePosition = volumePosition,
                                      showLyrics = showLyrics, showQueue = showQueue,
                                      onSeek = onSeek, onTogglePlayPause = onTogglePlayPause, onVolumeChange = { v -> volumePosition = v; onVolumeChange(v) },
                                      onToggleLyrics = { showLyrics = !showLyrics; showQueue = false }, onToggleQueue = { showQueue = !showQueue; showLyrics = false },
                                      includeVolumeAndIcons = true,
                                      includeProgress = false,
                                      onSkipNext = { swipeDirection = 1; onSkipNext() },
                                      onSkipPrevious = { swipeDirection = -1; onSkipPrevious() }
                                  )
                              }
                          }
                      }
                 }
            }

            // DETAILS AND CONTROLS (WHEN NO QUEUE AND NO LYRICS)
            androidx.compose.animation.AnimatedVisibility(
                 visible = !isOverlayActive,
                 modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                 enter = androidx.compose.animation.fadeIn(),
                 exit = androidx.compose.animation.fadeOut()
            ) {
                 Column(
                      modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 24.dp)
                          .graphicsLayer { alpha = contentAlpha }
                 ) {
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                          var dragAccumulator by remember { mutableStateOf(0f) }
                          Box(modifier = Modifier.weight(1f).pointerInput(playerState) {
                              detectHorizontalDragGestures(
                                  onDragEnd = {
                                      if (dragAccumulator < -60f) { swipeDirection = 1; onSkipNext() }
                                      else if (dragAccumulator > 60f) { swipeDirection = -1; onSkipPrevious() }
                                      dragAccumulator = 0f
                                  },
                                  onHorizontalDrag = { change, dragAmount ->
                                      dragAccumulator += dragAmount
                                      change.consume()
                                  }
                              )
                          }) {
                              val dir = swipeDirection
                              androidx.compose.animation.AnimatedContent(
                                  targetState = playerState,
                                  transitionSpec = {
                                      (androidx.compose.animation.slideInHorizontally { width -> dir * width } + androidx.compose.animation.fadeIn()).togetherWith(
                                          androidx.compose.animation.slideOutHorizontally { width -> dir * -width } + androidx.compose.animation.fadeOut()
                                      )
                                  }, label="textSlide"
                              ) { state ->
                                  Column(modifier = Modifier.fillMaxWidth().clickable { 
                                      if (state.videoId != null) {
                                          onArtistSelected(com.mrtdk.liquid_glass.ui.screens.ArtistState(
                                              id = state.artist,
                                              name = state.artist,
                                              thumbnail = null
                                          ))
                                      }
                                  }) {
                                      Text(
                                          text = state.title,
                                          color = contentColor,
                                          fontSize = 24.sp,
                                          fontWeight = FontWeight.Bold,
                                          maxLines = 1,
                                          overflow = TextOverflow.Ellipsis
                                      )
                                      Spacer(modifier = Modifier.height(2.dp))
                                      Text(
                                          text = state.artist,
                                          color = contentColor.copy(alpha = 0.7f),
                                          fontSize = 18.sp,
                                          maxLines = 1,
                                          overflow = TextOverflow.Ellipsis
                                      )
                                  }
                              }
                          }
                          Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                              Box(
                                  modifier = Modifier.size(32.dp).clip(CircleShape).background(contentColor.copy(alpha = 0.15f)).clickable {
                                      if (playerState != null) {
                                          if (!isSaved) {
                                              LibraryManager.saveItem(LibraryItem(playerState.videoId ?: "", playerState.title, playerState.artist, playerState.artUrl?.toString(), ItemType.SONG))
                                          } else {
                                              LibraryManager.removeItem(playerState.videoId ?: "")
                                          }
                                      }
                                  },
                                  contentAlignment = Alignment.Center
                              ) {
                                  Icon(Icons.Default.Star, contentDescription = "Fav", tint = starTint, modifier = Modifier.size(18.dp))
                              }
                              Box(
                                  modifier = Modifier.size(32.dp).clip(CircleShape).background(contentColor.copy(alpha = 0.15f)).clickable { showOptionsMenu = true },
                                  contentAlignment = Alignment.Center
                              ) {
                                  androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                                      val r = 1.5.dp.toPx()
                                      val space = 4.dp.toPx()
                                      val cx = size.width / 2f
                                      val cy = size.height / 2f
                                      drawCircle(contentColor, radius = r, center = Offset(cx - space - r * 2, cy))
                                      drawCircle(contentColor, radius = r, center = Offset(cx, cy))
                                      drawCircle(contentColor, radius = r, center = Offset(cx + space + r * 2, cy))
                                  }
                              }
                          }
                      }
                      
                      Spacer(modifier = Modifier.height(24.dp))
                      
                      Slider(
                          value = progress, onValueChange = { onSeek((it * duration).toLong()) },
                          modifier = Modifier.fillMaxWidth().height(16.dp),
                          colors = SliderDefaults.colors(
                              thumbColor = Color.Transparent,
                              activeTrackColor = contentColor.copy(alpha = 0.9f),
                              inactiveTrackColor = contentColor.copy(alpha = 0.3f)
                          )
                      )
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                          Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                          Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                      }
                      
                      Spacer(modifier = Modifier.height(32.dp))
                      
                      Box(
                          modifier = Modifier
                              .fillMaxWidth()
                              .padding(bottom = 32.dp)
                      ) {
                          PlayerBottomControls( // We only want the icons, volume and stuff here, NO PROGRESS
                              progress = progress, currentPosition = currentPosition, duration = duration,
                              isPlaying = isPlaying, contentColor = contentColor, volumePosition = volumePosition,
                              showLyrics = showLyrics, showQueue = showQueue,
                              onSeek = onSeek, onTogglePlayPause = onTogglePlayPause, onVolumeChange = { v -> volumePosition = v; onVolumeChange(v) },
                              onToggleLyrics = { showLyrics = !showLyrics; showQueue = false }, onToggleQueue = { showQueue = !showQueue; showLyrics = false },
                              includeVolumeAndIcons = true,
                              includeProgress = false,
                              onSkipNext = { swipeDirection = 1; onSkipNext() },
                              onSkipPrevious = { swipeDirection = -1; onSkipPrevious() }
                          )
                      }
                 }
            }
        }

        if (showOptionsMenu) {
            ModalBottomSheet(onDismissRequest = { showOptionsMenu = false }, containerColor = Color(0xFF1E1E1E)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(playerState?.title ?: "", color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold)
                    Spacer(modifier=Modifier.height(16.dp))
                    Row(modifier=Modifier.fillMaxWidth().clickable { showOptionsMenu=false; showPlaylistMenu=true }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.Menu, contentDescription=null, tint=Color.White)
                        Spacer(modifier=Modifier.width(16.dp))
                        Text("Añadir a playlist", color=Color.White, fontSize=16.sp)
                    }
                    Spacer(modifier=Modifier.height(32.dp))
                }
            }
        }

        if (showLyricsMenu) {
            ModalBottomSheet(onDismissRequest = { showLyricsMenu = false }, containerColor = Color(0xFF1E1E1E)) {
                Column(modifier = Modifier.padding(minOf(16.dp, 24.dp))) {
                    Text(text = "Proveedor de letras", color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold)
                    Spacer(modifier=Modifier.height(16.dp))
                    
                    Row(modifier=Modifier.fillMaxWidth().clickable { showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text("LRCLIB (Activo)", color=Color.White, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text("KuGou (Próximamente)", color=Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text("Musixmatch (Próximamente)", color=Color.Gray, fontSize=16.sp)
                    }
                    
                    Spacer(modifier=Modifier.height(16.dp))
                    androidx.compose.material3.Divider(color = Color.DarkGray)
                    Spacer(modifier=Modifier.height(16.dp))
                    
                    Row(modifier=Modifier.fillMaxWidth().clickable { 
                        showLyricsMenu=false 
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH).apply {
                                putExtra(android.app.SearchManager.QUERY, "${playerState?.artist ?: ""} ${playerState?.title ?: ""} lyrics")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription=null, tint=Color.White)
                        Spacer(modifier=Modifier.width(16.dp))
                        Text("Buscar letra en internet", color=Color.White, fontSize=16.sp)
                    }
                    Spacer(modifier=Modifier.height(32.dp))
                }
            }
        }

        if (showLyricsMenu) {
            ModalBottomSheet(onDismissRequest = { showLyricsMenu = false }, containerColor = Color(0xFF1E1E1E)) {
                Column(modifier = Modifier.padding(minOf(16.dp, 24.dp))) {
                    Text(text = "Proveedor de letras", color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold)
                    Spacer(modifier=Modifier.height(16.dp))
                    
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "LRCLIB"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "LRCLIB") "LRCLIB (Activo)" else "LRCLIB", color=if (selectedLyricsProvider == "LRCLIB") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "KuGou"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "KuGou") "KuGou (Activo)" else "KuGou", color=if (selectedLyricsProvider == "KuGou") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "BetterLyrics"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "BetterLyrics") "BetterLyrics (Activo)" else "BetterLyrics", color=if (selectedLyricsProvider == "BetterLyrics") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "LyricsPlus"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "LyricsPlus") "LyricsPlus (Activo)" else "LyricsPlus", color=if (selectedLyricsProvider == "LyricsPlus") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "SimpMusic"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "SimpMusic") "SimpMusic (Activo)" else "SimpMusic", color=if (selectedLyricsProvider == "SimpMusic") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "YouTube Music"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "YouTube Music") "YouTube Music (Activo)" else "YouTube Music", color=if (selectedLyricsProvider == "YouTube Music") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Row(modifier=Modifier.fillMaxWidth().clickable { selectedLyricsProvider = "YouTube Subtitle"; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (selectedLyricsProvider == "YouTube Subtitle") "YouTube Subtitle (Activo)" else "YouTube Subtitle", color=if (selectedLyricsProvider == "YouTube Subtitle") Color.White else Color.Gray, fontSize=16.sp)
                    }
                    Spacer(modifier=Modifier.height(8.dp))
                    androidx.compose.material3.Divider(color = Color.DarkGray)
                    Spacer(modifier=Modifier.height(8.dp))
                    Row(modifier=Modifier.fillMaxWidth().clickable { isRomajiEnabled = !isRomajiEnabled; showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text(if (isRomajiEnabled) "Desactivar Romaji" else "Activar Romaji", color=Color.White, fontSize=16.sp)
                    }
                    
                    Spacer(modifier=Modifier.height(16.dp))
                    androidx.compose.material3.Divider(color = Color.DarkGray)
                    Spacer(modifier=Modifier.height(16.dp))
                    
                    Row(modifier=Modifier.fillMaxWidth().clickable { 
                        showLyricsMenu=false 
                        manualLyricsQueryTitle = playerState?.title ?: ""
                        manualLyricsQueryArtist = playerState?.artist ?: ""
                        showManualLyricsSearch = true
                    }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription=null, tint=Color.White)
                        Spacer(modifier=Modifier.width(16.dp))
                        Text("Buscar letra manualmente", color=Color.White, fontSize=16.sp)
                    }
                    Spacer(modifier=Modifier.height(32.dp))
                }
            }
        }
        
        val coroutineScope = rememberCoroutineScope()
        if (showManualLyricsSearch) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showManualLyricsSearch = false },
                containerColor = Color(0xFF1E1E1E),
                title = { Text("Buscar letra", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.OutlinedTextField(
                            value = manualLyricsQueryTitle,
                            onValueChange = { manualLyricsQueryTitle = it },
                            label = { Text("Título de la canción", color = Color.Gray) },
                            singleLine = true,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                            )
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = manualLyricsQueryArtist,
                            onValueChange = { manualLyricsQueryArtist = it },
                            label = { Text("Artista", color = Color.Gray) },
                            singleLine = true,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showManualLyricsSearch = false
                        lyricsLines = null // reset so user sees loading
                        val targetTitle = manualLyricsQueryTitle
                        val targetArtist = manualLyricsQueryArtist
                        coroutineScope.launch {
                            val lines = when (selectedLyricsProvider) {
                                "KuGou" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchKuGouLyrics(targetTitle, targetArtist)
                                "BetterLyrics" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchBetterLyrics(targetTitle, targetArtist)
                                "LyricsPlus" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchLyricsPlus(targetTitle, targetArtist)
                                "SimpMusic" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchSimpMusicLyrics(targetTitle, targetArtist)
                                "YouTube Music" -> playerState?.videoId?.let { com.mrtdk.liquid_glass.utils.LyricsProvider.fetchYouTubeLyrics(it) }
                                "YouTube Subtitle" -> playerState?.videoId?.let { com.mrtdk.liquid_glass.utils.LyricsProvider.fetchYouTubeSubtitleLyrics(it) }
                                else -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchLyrics(targetTitle, targetArtist)
                            }
                            if (isRomajiEnabled && lines != null) {
                                val prefs = com.mrtdk.liquid_glass.utils.LyricsRomanizationPreferences(true, true, true, true, true)
                                lyricsLines = lines.map { line ->
                                    val romanized = com.mrtdk.liquid_glass.utils.LyricsUtils.romanizeLyricsLine(line.text, prefs)
                                    if (romanized != null) {
                                        line.copy(text = romanized)
                                    } else {
                                        line
                                    }
                                }
                            } else {
                                lyricsLines = lines
                            }
                        }
                    }) {
                        Text("Buscar", color = Color(0xFFFA243C))
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showManualLyricsSearch = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
        
        if (showPlaylistMenu) {
            ModalBottomSheet(onDismissRequest = { showPlaylistMenu = false }, containerColor = Color(0xFF1E1E1E)) {
                val playlists by com.mrtdk.liquid_glass.data.LibraryManager.playlists.collectAsState()
                Column(modifier = Modifier.padding(horizontal=16.dp, vertical=8.dp).fillMaxWidth()) {
                    Text("Añadir a playlist", color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold)
                    Spacer(modifier=Modifier.height(16.dp))
                    Row(modifier=Modifier.fillMaxWidth().clickable { showPlaylistMenu=false; showNewPlaylistDialog=true }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                        Box(modifier=Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray), contentAlignment=Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint=Color.White)
                        }
                        Spacer(modifier=Modifier.width(16.dp))
                        Text("Nueva playlist...", color=Color(0xFFFA243C), fontSize=16.sp)
                    }
                    
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max=300.dp)) {
                        items(playlists.size) { i ->
                            val pl = playlists[i]
                            Row(modifier=Modifier.fillMaxWidth().clickable { 
                                com.mrtdk.liquid_glass.data.LibraryManager.addSongToPlaylist(pl.id, com.mrtdk.liquid_glass.data.LibraryItem(playerState?.videoId?:"", playerState?.title?:"", playerState?.artist?:"", playerState?.artUrl?.toString(), com.mrtdk.liquid_glass.data.ItemType.SONG))
                                showPlaylistMenu=false 
                            }.padding(vertical=8.dp), verticalAlignment=Alignment.CenterVertically) {
                                Box(modifier=Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                                    if (pl.items.isNotEmpty() && pl.items.first().thumbnail != null) {
                                        AsyncImage(model=pl.items.first().thumbnail, contentDescription=null, modifier=Modifier.fillMaxSize(), contentScale=ContentScale.Crop)
                                    }
                                }
                                Spacer(modifier=Modifier.width(16.dp))
                                Column {
                                    Text(pl.name, color=Color.White, fontSize=16.sp)
                                    Text("${pl.items.size} canciones", color=Color.Gray, fontSize=14.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier=Modifier.height(32.dp))
                }
            }
        }
        
        if (showNewPlaylistDialog) {
            var newPlaylistName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewPlaylistDialog = false },
                title = { Text("Nueva playlist", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor=Color.White, 
                            unfocusedTextColor=Color.White,
                            focusedBorderColor = Color(0xFFFA243C),
                            focusedLabelColor = Color(0xFFFA243C)
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if(newPlaylistName.isNotBlank()){
                            com.mrtdk.liquid_glass.data.LibraryManager.createPlaylist(newPlaylistName)
                        }
                        showNewPlaylistDialog = false
                    }) { Text("Crear", color = Color(0xFFFA243C)) }
                },
                dismissButton = {
                    TextButton(onClick = { showNewPlaylistDialog = false }) { Text("Cancelar", color = Color.Gray) }
                },
                containerColor = Color(0xFF2C2C2C)
            )
        }
    }
}
@Composable
fun PlayerBottomControls(
    progress: Float, currentPosition: Long, duration: Long,
    isPlaying: Boolean, contentColor: Color, volumePosition: Float,
    showLyrics: Boolean, showQueue: Boolean,
    onSeek: (Long) -> Unit, onTogglePlayPause: () -> Unit, onVolumeChange: (Float) -> Unit,
    onToggleLyrics: () -> Unit, onToggleQueue: () -> Unit,
    includeVolumeAndIcons: Boolean = true,
    includeProgress: Boolean = true,
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        if (includeProgress) {
            Slider(
                value = progress, onValueChange = { onSeek((it * duration).toLong()) },
                modifier = Modifier.fillMaxWidth().height(16.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = contentColor.copy(alpha = 0.9f),
                    inactiveTrackColor = contentColor.copy(alpha = 0.3f)
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            AnimatedSkipButton(
                iconId = R.drawable.previous,
                contentDescription = "Previous",
                contentColor = contentColor,
                onClick = onSkipPrevious
            )
            Spacer(modifier = Modifier.width(36.dp))
            Box(modifier = Modifier.size(72.dp).clickable { onTogglePlayPause() }, contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = if (isPlaying) R.drawable.pause else R.drawable.resume),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = contentColor,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.width(36.dp))
            AnimatedSkipButton(
                iconId = R.drawable.forward,
                contentDescription = "Next",
                contentColor = contentColor,
                onClick = onSkipNext
            )
        }
        
        if (includeVolumeAndIcons) {
            Spacer(modifier = Modifier.height(70.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.albumspeaker),
                    contentDescription = "Low volume",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Slider(
                    value = volumePosition, onValueChange = { onVolumeChange(it) },
                    modifier = Modifier.weight(1f).height(16.dp),
                    colors = SliderDefaults.colors(thumbColor = contentColor, activeTrackColor = contentColor.copy(alpha = 0.9f), inactiveTrackColor = contentColor.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    painter = painterResource(id = R.drawable.albumspeakerlarge),
                    contentDescription = "High volume",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (showLyrics) contentColor.copy(alpha = 0.2f) else Color.Transparent).clickable { onToggleLyrics() }, contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.lyrics),
                        contentDescription = "Lyrics",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.apple_lossless_seeklogo),
                    contentDescription = "Format",
                    tint = contentColor,
                    modifier = Modifier.height(20.dp).padding(horizontal = 8.dp)
                )
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (showQueue) contentColor.copy(alpha = 0.2f) else Color.Transparent).clickable { onToggleQueue() }, contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.nextinfo),
                        contentDescription = "Next Info",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
             Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun AnimatedSkipButton(
    iconId: Int,
    contentDescription: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(if (isPressed) 0.85f else 1f, label="")
    val bgAlpha by androidx.compose.animation.core.animateFloatAsState(if (isPressed) 0.15f else 0f, label="")
    
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
        )
    }
}
