package com.mrtdk.liquid_glass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.mrtdk.liquid_glass.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrtdk.glass.GlassBox
import com.mrtdk.glass.GlassBoxScope
import com.mrtdk.liquid_glass.ui.screens.PlayerState

@Composable
fun GlassBoxScope.MiniPlayer(
    playerState: PlayerState?,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hideImage: Boolean = false,
    tintColor: Color = Color.White.copy(alpha = 0.15f),
    contentColor: Color = Color.White
) {
    if (playerState == null) return

    val context = LocalContext.current

    GlassBox(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        blur = 0.8f,
        centerDistortion = 0.1f,
        scale = 0.02f,
        warpEdges = 0.4f,
        tint = tintColor,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(playerState.artUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(if (hideImage) 0f else 1f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerState.title,
                    color = contentColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playerState.artist,
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(id = if (isPlaying) R.drawable.pause else R.drawable.resume),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = contentColor
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.forward),
                contentDescription = "Next",
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
