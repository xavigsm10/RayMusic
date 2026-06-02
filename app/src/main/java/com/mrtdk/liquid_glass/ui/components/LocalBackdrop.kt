package com.mrtdk.liquid_glass.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val LocalBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

object SharedTransitionState {
    var lastClickBounds: Rect? = null
    var lastOpenedId: String? = null
}

fun Modifier.wiggleOnScroll(
    itemId: String,
    scrollState: androidx.compose.foundation.lazy.grid.LazyGridState? = null,
    lazyListState: androidx.compose.foundation.lazy.LazyListState? = null,
    customScrollState: androidx.compose.foundation.ScrollState? = null
): Modifier = composed {
    val lastOpenedId = SharedTransitionState.lastOpenedId
    if (lastOpenedId == null || itemId != lastOpenedId) return@composed this

    var wiggleCount by remember { mutableStateOf(0) }
    val wiggleOffset = remember { Animatable(0f) }
    
    val isScrollInProgress = scrollState?.isScrollInProgress 
        ?: lazyListState?.isScrollInProgress 
        ?: customScrollState?.isScrollInProgress 
        ?: false
    
    LaunchedEffect(isScrollInProgress) {
        if (isScrollInProgress) {
            wiggleCount++
            if (wiggleCount >= 4) {
                // Shake back and forth
                for (i in 1..4) {
                    wiggleOffset.animateTo(12f, androidx.compose.animation.core.spring(dampingRatio = 0.3f, stiffness = 800f))
                    wiggleOffset.animateTo(-12f, androidx.compose.animation.core.spring(dampingRatio = 0.3f, stiffness = 800f))
                }
                wiggleOffset.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 500f))
                wiggleCount = 0
            }
        }
    }
    
    LaunchedEffect(wiggleCount) {
        if (wiggleCount > 0) {
            kotlinx.coroutines.delay(1500)
            wiggleCount = 0
        }
    }
    
    this.graphicsLayer {
        translationX = wiggleOffset.value
        rotationZ = wiggleOffset.value * 0.4f
    }
}

fun Modifier.trackClickBounds(onClick: () -> Unit): Modifier = composed {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    this
        .onGloballyPositioned { coords = it }
        .clickable {
            SharedTransitionState.lastClickBounds = coords?.boundsInRoot()
            onClick()
        }
}

fun Modifier.trackTapBounds(
    onLongPress: (() -> Unit)? = null,
    onTap: () -> Unit
): Modifier = composed {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    this
        .onGloballyPositioned { coords = it }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    SharedTransitionState.lastClickBounds = coords?.boundsInRoot()
                    onTap()
                },
                onLongPress = {
                    onLongPress?.invoke()
                }
            )
        }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

@Composable
fun SharedElementTransitionContainer(
    onBack: () -> Unit,
    content: @Composable (progress: Float, dismiss: () -> Unit) -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val lastClickBounds = SharedTransitionState.lastClickBounds
    val sourceBounds = lastClickBounds ?: Rect(
        screenWidth / 2f - 100f,
        screenHeight / 2f - 100f,
        screenWidth / 2f + 100f,
        screenHeight / 2f + 100f
    )
    
    val progress = remember { Animatable(0f) }
    var dragY by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    val dismissAction = remember(scope, progress, onBack) {
        {
            scope.launch {
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 300f
                    )
                )
                onBack()
            }
            Unit
        }
    }
    
    androidx.activity.compose.BackHandler(enabled = progress.value > 0.01f) {
        dismissAction()
    }
    
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = 200f
            )
        )
    }
    
    val currentProgress = progress.value
    
    // Interpolated geometry values
    val currentLeft = lerpFloat(sourceBounds.left, 0f, currentProgress)
    val currentTop = lerpFloat(sourceBounds.top, 0f, currentProgress)
    val currentWidth = lerpFloat(sourceBounds.width, screenWidth, currentProgress).coerceAtLeast(0f)
    val currentHeight = lerpFloat(sourceBounds.height, screenHeight, currentProgress).coerceAtLeast(0f)
    val currentCornerRadius = lerpFloat(24f, 0f, currentProgress).coerceAtLeast(0f)
    
    val currentWidthDp = with(density) { currentWidth.toDp() }
    val currentHeightDp = with(density) { currentHeight.toDp() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragY = 0f },
                    onDragEnd = {
                        scope.launch {
                            if (dragY > screenHeight * 0.2f) {
                                progress.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 300f))
                                onBack()
                            } else {
                                progress.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 200f))
                            }
                            dragY = 0f
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            progress.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 200f))
                            dragY = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (dragAmount.y > 0 || dragY > 0) {
                            dragY += dragAmount.y
                            val newProgress = (1f - (dragY / (screenHeight * 0.8f))).coerceIn(0f, 1f)
                            scope.launch {
                                progress.snapTo(newProgress)
                            }
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(currentLeft.roundToInt(), currentTop.roundToInt()) }
                .size(currentWidthDp, currentHeightDp)
                .graphicsLayer {
                    clip = true
                    shape = RoundedCornerShape(currentCornerRadius.dp)
                }
        ) {
            content(currentProgress, dismissAction)
        }
    }
}
