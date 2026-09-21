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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
    contentColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    playbackProgress: () -> Float = { 0f },
    onSeek: (Float) -> Unit = {},
    landingTrigger: Long = 0L,
) {
    if (playerState == null) return

    val isDarkMode by com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState()

    val effectiveTitleColor = if (contentColor != Color.Unspecified) {
        if (!isDarkMode && contentColor == Color.White) Color(0xFF2C2C2E) else contentColor
    } else {
        if (isDarkMode) Color.White else Color(0xFF2C2C2E)
    }

    val effectiveSubtextColor = if (contentColor != Color.Unspecified) {
        if (!isDarkMode && contentColor == Color.White) Color(0xFF636366) else contentColor.copy(alpha = 0.7f)
    } else {
        if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color(0xFF636366)
    }

    val effectiveIconColor = if (contentColor != Color.Unspecified) {
        if (!isDarkMode && contentColor == Color.White) Color(0xFF3C3C40) else contentColor
    } else {
        if (isDarkMode) Color.White else Color(0xFF3C3C40)
    }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val densityScale = density.density

    val jumpOffsetY = remember { Animatable(0f) }
    val jumpScale = remember { Animatable(1f) }
    val artScale = remember { Animatable(1f) }

    var lastArtUrl by remember { mutableStateOf(playerState.artUrl) }
    var lastTrigger by remember { mutableLongStateOf(landingTrigger) }

    LaunchedEffect(playerState.artUrl, landingTrigger) {
        val artChanged = lastArtUrl != null && lastArtUrl != playerState.artUrl
        val triggered = landingTrigger > 0L && landingTrigger != lastTrigger
        if (artChanged || triggered) {
            coroutineScope.launch {
                jumpOffsetY.snapTo(-14f * densityScale)
                jumpOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            coroutineScope.launch {
                jumpScale.snapTo(1.07f)
                jumpScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            coroutineScope.launch {
                artScale.snapTo(0.82f)
                artScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
        lastArtUrl = playerState.artUrl
        lastTrigger = landingTrigger
    }

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
    // Minireproductor compacto: píldora baja con carátula e iconos medianos.
    val containerHeight = if (isInline) 44.dp else 52.dp
    val artSize = if (isInline) 32.dp else 38.dp
    val artCornerRadius = if (isInline) 8.dp else 10.dp

    var miniPlayerSwipeDirection by remember { mutableIntStateOf(1) }

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
                scaleX = pressScale * jumpScale.value
                scaleY = pressScale * jumpScale.value
                translationY = jumpOffsetY.value
            }
            .clip(pillShape)
            .clipToBounds()
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = pressInteractionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(
                    horizontal = if (isInline) 8.dp else 10.dp,
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
                    .graphicsLayer {
                        scaleX = artScale.value
                        scaleY = artScale.value
                    }
                    .clip(RoundedCornerShape(artCornerRadius)),
            )

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
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
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
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

                                val threshold = 28f * densityScale
                                val shouldChangeSong = dragged > threshold ||
                                        (velocity > 0.35f && dragged > 12f * densityScale)

                                if (shouldChangeSong) {
                                    if (currentOffset > 0) {
                                        miniPlayerSwipeDirection = -1
                                        onPrevious()
                                    } else {
                                        miniPlayerSwipeDirection = 1
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
                AnimatedContent(
                    targetState = (playerState.title.ifEmpty { "Reproduciendo" }) to (playerState.artist.ifEmpty { "Artista" }),
                    transitionSpec = {
                        val dir = miniPlayerSwipeDirection
                        (slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { width -> dir * width } + fadeIn(tween(180))).togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) { width -> dir * -width } + fadeOut(tween(150))
                        )
                    },
                    label = "miniPlayerSongSlide"
                ) { (titleText, artistText) ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = if (isInline) 13.sp else 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = effectiveTitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = artistText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = if (isInline) 10.5.sp else 12.sp
                            ),
                            color = effectiveSubtextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(if (isInline) 36.dp else 40.dp),
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
                        tint = effectiveIconColor,
                        modifier = Modifier
                            .size(if (isInline) 22.dp else 26.dp)
                            .graphicsLayer {
                                rotationZ = playPauseRotation
                            }
                    )
                }
            }

            Spacer(Modifier.width(if (isInline) 2.dp else 4.dp))
            IconButton(
                onClick = {
                    miniPlayerSwipeDirection = 1
                    onNext()
                },
                modifier = Modifier.size(if (isInline) 32.dp else 36.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.forward),
                    contentDescription = "Next",
                    tint = effectiveIconColor,
                    modifier = Modifier.size(if (isInline) 20.dp else 24.dp)
                )
            }
        }
    }
}
