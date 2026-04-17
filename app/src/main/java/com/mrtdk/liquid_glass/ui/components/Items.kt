package com.mrtdk.liquid_glass.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrtdk.liquid_glass.data.Song
import java.util.Locale

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(72.dp)
            .padding(horizontal = 8.dp)
    ) {
        Box(Modifier.padding(6.dp), contentAlignment = Alignment.Center) { thumbnailContent() }
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(
                text = title, 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle, 
                    color = Color.Gray, 
                    fontSize = 14.sp, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailingContent()
    }
}

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    thumbnailContent: @Composable BoxScope.() -> Unit,
    fillMaxWidth: Boolean = false,
) {
    Column(
        modifier = if (fillMaxWidth) {
            modifier.fillMaxWidth()
        } else {
            modifier.width(160.dp)
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = if (fillMaxWidth) {
                Modifier.fillMaxWidth().aspectRatio(1f)
            } else {
                Modifier.size(160.dp)
            }
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = subtitle,
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ItemThumbnail(
    data: Any?,
    shape: Shape = RoundedCornerShape(8.dp),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.DarkGray)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(data)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SongListItem(song: Song, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    ListItem(
        title = song.title,
        subtitle = "${song.artist} • ${formatDuration(song.duration)}",
        thumbnailContent = {
            ItemThumbnail(
                data = song.albumArtUri,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(54.dp)
            )
        },
        modifier = modifier.clickable { onClick() }
    )
}

@Composable
fun SongGridItem(song: Song, modifier: Modifier = Modifier, fillMaxWidth: Boolean = false, onClick: () -> Unit = {}) {
    GridItem(
        title = song.title,
        subtitle = song.artist,
        thumbnailContent = {
            ItemThumbnail(
                data = song.albumArtUri,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxSize()
            )
        },
        fillMaxWidth = fillMaxWidth,
        modifier = modifier.clickable { onClick() }
    )
}

@Composable
fun AlbumGridItem(albumName: String, artistName: String, albumArtUri: Uri?, modifier: Modifier = Modifier, fillMaxWidth: Boolean = false, onClick: () -> Unit = {}) {
    GridItem(
        title = albumName,
        subtitle = artistName,
        thumbnailContent = {
            ItemThumbnail(
                data = albumArtUri,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxSize()
            )
        },
        fillMaxWidth = fillMaxWidth,
        modifier = modifier.clickable { onClick() }
    )
}

@Composable
fun ArtistGridItem(artistName: String, albumArtUri: Uri?, modifier: Modifier = Modifier, fillMaxWidth: Boolean = false, onClick: () -> Unit = {}) {
    GridItem(
        title = artistName,
        subtitle = "Artista",
        thumbnailContent = {
            ItemThumbnail(
                data = albumArtUri,
                shape = CircleShape,
                modifier = Modifier.fillMaxSize()
            )
        },
        fillMaxWidth = fillMaxWidth,
        modifier = modifier.clickable { onClick() }
    )
}

@Composable
fun AlbumListItem(albumName: String, artistName: String, albumArtUri: Uri?, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    ListItem(
        title = albumName,
        subtitle = artistName,
        thumbnailContent = {
            ItemThumbnail(
                data = albumArtUri,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(54.dp)
            )
        },
        modifier = modifier.clickable { onClick() }
    )
}

@Composable
fun ArtistListItem(artistName: String, albumArtUri: Uri?, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    ListItem(
        title = artistName,
        subtitle = "Artista",
        thumbnailContent = {
            ItemThumbnail(
                data = albumArtUri,
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            )
        },
        modifier = modifier.clickable { onClick() }
    )
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
