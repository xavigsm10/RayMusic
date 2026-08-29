package com.mrtdk.liquid_glass.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.ui.components.shapes.ContinuousRoundedRectangle
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import com.mrtdk.liquid_glass.utils.InteractiveHighlight
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The floating mini player inspired by Convx/iOS:
 * - Docked above tabs in expanded mode (56.dp) with title, artist, play/pause and next controls
 * - Docked inline between active tab and search in collapsed/inline mode (48.dp)
 * - Restored original RayMusic icons (R.drawable.pause, R.drawable.resume, R.drawable.forward)
 * - Swipe horizontally to change song
 * - Press scale feedback (1.03f) and touch glow
 */
@Composable
fun FloatingMiniPlayer(
    isInline: Boolean,
    playerState: PlayerState?,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
    playbackProgress: () -> Float = { 0f },
    onSeek: (Float) -> Unit = {},
) {
    if (playerState == null) return

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val densityScale = density.density

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    val animationSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    }

    val configuration = LocalConfiguration.current
    val autoSwipeThreshold = remember(configuration.screenWidthDp, densityScale) {
        val screenWidthPx = configuration.screenWidthDp * densityScale
        (screenWidthPx * 0.25f).roundToInt()
    }

    val pressInteractionSource = remember { MutableInteractionSource() }
    val isPressed by pressInteractionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "miniPlayerPressScale",
    )

    val interactiveHighlight = remember(coroutineScope) { InteractiveHighlight(animationScope = coroutineScope) }

    val pillShape = ContinuousRoundedRectangle(percent = 50)
    val containerHeight = if (isInline) 48.dp else 56.dp
    val artSize = if (isInline) 34.dp else 38.dp
    val artCornerRadius = if (isInline) 8.dp else 9.dp

    val playPauseRotation by animateFloatAsState(
        targetValue = if (isPlaying) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "miniPlayPauseRotation"
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(containerHeight)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(pillShape)
            .clipToBounds()
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragStartTime = System.currentTimeMillis()
                        totalDragDistance = 0f
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(0f, animationSpec)
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragDistance += abs(dragAmount)
                        coroutineScope.launch {
                            offsetXAnimatable.snapTo(offsetXAnimatable.value + dragAmount)
                        }
                    },
                    onDragEnd = {
                        val dragDuration = System.currentTimeMillis() - dragStartTime
                        val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                        val currentOffset = offsetXAnimatable.value
                        val dragged = abs(currentOffset)

                        val shouldChangeSong = dragged > autoSwipeThreshold ||
                                (velocity > 0.55f && dragged > autoSwipeThreshold * 0.25f)

                        if (shouldChangeSong) {
                            if (currentOffset > 0) {
                                onPrevious()
                            } else {
                                onNext()
                            }
                        }
                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(0f, animationSpec)
                        }
                    }
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .clickable(
                    interactionSource = pressInteractionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(
                    horizontal = if (isInline) 8.dp else 12.dp,
                    vertical = if (isInline) 4.dp else 6.dp,
                ),
        ) {
            AsyncImage(
                model = playerState.artUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.nav_inicio),
                error = painterResource(R.drawable.nav_inicio),
                modifier = Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(artCornerRadius)),
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playerState.title.ifEmpty { "Reproduciendo" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = if (isInline) 13.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = playerState.artist.ifEmpty { "Artista" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = if (isInline) 10.sp else 11.sp
                    ),
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(if (isInline) 34.dp else 36.dp),
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)))
                            .togetherWith(fadeOut(animationSpec = tween(90)) + scaleOut(targetScale = 0.3f, animationSpec = tween(90)))
                    },
                    label = "miniPlayPauseIcon"
                ) { playing ->
                    Icon(
                        painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.resume),
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = contentColor,
                        modifier = Modifier
                            .size(if (isInline) 22.dp else 24.dp)
                            .graphicsLayer {
                                rotationZ = playPauseRotation
                            }
                    )
                }
            }

            if (!isInline) {
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.forward),
                        contentDescription = "Next",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
