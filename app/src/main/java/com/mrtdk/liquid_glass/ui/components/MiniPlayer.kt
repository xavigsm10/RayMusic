package com.mrtdk.liquid_glass.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.mrtdk.liquid_glass.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.launch

@Composable
fun GlassBoxScope.MiniPlayer(
    playerState: PlayerState?,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    hideImage: Boolean = false,
    tintColor: Color = Color.White.copy(alpha = 0.15f),
    contentColor: Color = Color.White
) {
    if (playerState == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Landing animation: triggers when full player closes (hideImage: true -> false)
    var previousHideImage by remember { mutableStateOf(hideImage) }
    val landingTranslateY = remember { Animatable(0f) }
    val landingScale = remember { Animatable(1f) }

    LaunchedEffect(hideImage) {
        if (previousHideImage && !hideImage) {
            // Full player just closed — animate the mini player "landing" with bounce
            launch {
                landingTranslateY.snapTo(-60f)
                landingTranslateY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                landingScale.snapTo(0.80f)
                landingScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
        previousHideImage = hideImage
    }

    val swipeOffsetX = remember { Animatable(0f) }

    GlassBox(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                translationY = landingTranslateY.value
                scaleX = landingScale.value
                scaleY = landingScale.value
            }
            .clip(CircleShape)
            .clickable { onClick() }
            .pointerInput(Unit) {
                val thresholdPx = 80.dp.toPx()
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val currentValue = swipeOffsetX.value
                            if (currentValue < -thresholdPx) {
                                onNext()
                                swipeOffsetX.snapTo(thresholdPx)
                                swipeOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            } else if (currentValue > thresholdPx) {
                                onPrevious()
                                swipeOffsetX.snapTo(-thresholdPx)
                                swipeOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            } else {
                                swipeOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            swipeOffsetX.animateTo(0f)
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val maxDrag = thresholdPx * 1.5f
                            val newValue = (swipeOffsetX.value + dragAmount).coerceIn(-maxDrag, maxDrag)
                            swipeOffsetX.snapTo(newValue)
                        }
                    }
                )
            },
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
                .graphicsLayer {
                    translationX = swipeOffsetX.value
                    val thresholdPx = 80.dp.toPx()
                    val progress = (Math.abs(swipeOffsetX.value) / thresholdPx).coerceIn(0f, 1f)
                    alpha = 1f - (progress * 0.7f)
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val titleLower = playerState.title.lowercase()
            val artistLower = playerState.artist.lowercase()
            val isTheWeeknd = artistLower.contains("the weeknd")
            val isDeftones = artistLower.contains("deftones")

            val correctedUrl = when {
                isTheWeeknd && (
                    titleLower.contains("alone again") ||
                    titleLower.contains("too late") ||
                    titleLower.contains("hardest to love") ||
                    titleLower.contains("scared to live") ||
                    titleLower.contains("snowchild") ||
                    titleLower.contains("escape from la") ||
                    titleLower.contains("heartless") ||
                    titleLower.contains("faith") ||
                    titleLower.contains("blinding lights") ||
                    titleLower.contains("in your eyes") ||
                    titleLower.contains("save your tears") ||
                    titleLower.contains("repeat after me") ||
                    titleLower.contains("after hours") ||
                    titleLower.contains("until i bleed out") ||
                    titleLower.contains("nothing compares") ||
                    titleLower.contains("missed you") ||
                    titleLower.contains("final lullaby")
                ) -> "file:///android_asset/fullartwork/after hours the weeknd.png"

                isDeftones && (
                    titleLower.contains("my own summer") ||
                    titleLower.contains("lhabia") ||
                    titleLower.contains("mascara") ||
                    titleLower.contains("around the fur") ||
                    titleLower.contains("rickets") ||
                    titleLower.contains("be quiet and drive") ||
                    titleLower.contains("lotion") ||
                    titleLower.contains("dai the flu") ||
                    titleLower.contains("headup") ||
                    titleLower.contains("mx") ||
                    titleLower.contains("damone")
                ) -> "file:///android_asset/fullartwork/deftones around the fur.png"

                else -> playerState.artUrl
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                    .data(correctedUrl)
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
            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.forward),
                    contentDescription = "Next",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
