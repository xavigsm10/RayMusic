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
import androidx.compose.ui.input.nestedscroll.nestedScroll

val LocalBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

object SharedTransitionState {
    var lastClickBounds: Rect? = null
    var lastOpenedId: String? = null
    var isDetailOpen: Boolean by mutableStateOf(false)
    val carouselItemBounds = mutableStateMapOf<String, Rect>()
    val animatingItemIds = mutableStateListOf<String>()
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

fun Modifier.sharedTransitionElement(itemId: String): Modifier = composed {
    val isDetailOpen = SharedTransitionState.isDetailOpen
    val lastOpenedId = SharedTransitionState.lastOpenedId
    val isOpened = isDetailOpen && lastOpenedId == itemId
    
    this
        .onGloballyPositioned { coords ->
            val bounds = coords.boundsInRoot()
            if (bounds.width > 0f && bounds.height > 0f) {
                SharedTransitionState.carouselItemBounds[itemId] = bounds
            }
        }
        .graphicsLayer {
            alpha = if (isOpened) 0f else 1f
        }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

val DetailEntrySpringSpec = spring<Float>(
    dampingRatio = 0.85f,
    stiffness = 200f
)

val DetailExitSpringSpec = spring<Float>(
    dampingRatio = 0.78f,
    stiffness = 165f
)

@Composable
fun SharedElementTransitionContainer(
    onBack: () -> Unit,
    shrinkToTarget: Boolean = true,
    enableSwipeToDismiss: Boolean = true,
    slideToSide: Boolean = false,
    animate: Boolean = true,
    staticContainer: Boolean = false,
    content: @Composable (progress: Float, dismiss: () -> Unit) -> Unit
) {
    DisposableEffect(Unit) {
        SharedTransitionState.isDetailOpen = true
        onDispose {
            SharedTransitionState.isDetailOpen = false
        }
    }
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        
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
        
        val dismissAction = remember(scope, progress, onBack, animate) {
            {
                scope.launch {
                    if (animate) {
                        progress.animateTo(
                            targetValue = 0f,
                            animationSpec = DetailExitSpringSpec
                        )
                    } else {
                        progress.snapTo(0f)
                    }
                    SharedTransitionState.isDetailOpen = false
                    onBack()
                }
                Unit
            }
        }
        
        androidx.activity.compose.BackHandler(enabled = progress.value > 0.01f) {
            dismissAction()
        }
        
        LaunchedEffect(Unit) {
            if (animate) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = DetailEntrySpringSpec
                )
            } else {
                progress.snapTo(1f)
            }
        }
        
        val currentProgress = progress.value
        
        // Interpolated geometry values
        val currentLeft = if (staticContainer) {
            0f
        } else if (slideToSide) {
            lerpFloat(screenWidth, 0f, currentProgress)
        } else if (shrinkToTarget) {
            lerpFloat(sourceBounds.left, 0f, currentProgress)
        } else {
            0f
        }
        val currentTop = if (staticContainer) {
            0f
        } else if (slideToSide) {
            0f
        } else if (shrinkToTarget) {
            lerpFloat(sourceBounds.top, 0f, currentProgress)
        } else {
            lerpFloat(screenHeight, 0f, currentProgress)
        }
        val currentWidth = if (staticContainer) {
            screenWidth
        } else if (slideToSide) {
            screenWidth
        } else if (shrinkToTarget) {
            lerpFloat(sourceBounds.width, screenWidth, currentProgress).coerceAtLeast(0f)
        } else {
            screenWidth
        }
        val currentHeight = if (staticContainer) {
            screenHeight
        } else if (slideToSide) {
            screenHeight
        } else if (shrinkToTarget) {
            lerpFloat(sourceBounds.height, screenHeight, currentProgress).coerceAtLeast(0f)
        } else {
            screenHeight
        }
        val currentCornerRadius = if (staticContainer) {
            0f
        } else if (slideToSide) {
            0f
        } else if (shrinkToTarget) {
            lerpFloat(24f, 0f, currentProgress).coerceAtLeast(0f)
        } else {
            0f
        }
        
        val currentWidthDp = with(density) { currentWidth.toDp() }
        val currentHeightDp = with(density) { currentHeight.toDp() }
        
        val nestedScrollConnection = remember(scope, progress, onBack, screenHeight) {
            object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                override fun onPreScroll(
                    available: androidx.compose.ui.geometry.Offset,
                    source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
                ): androidx.compose.ui.geometry.Offset {
                    val delta = available.y
                    if (dragY > 0f && delta < 0f) {
                        val oldDragY = dragY
                        dragY = (dragY + delta).coerceAtLeast(0f)
                        val consumed = dragY - oldDragY
                        val newProgress = (1f - (dragY / (screenHeight * 0.8f))).coerceIn(0f, 1f)
                        scope.launch {
                            progress.snapTo(newProgress)
                        }
                        return androidx.compose.ui.geometry.Offset(0f, consumed)
                    }
                    return androidx.compose.ui.geometry.Offset.Zero
                }
                
                override fun onPostScroll(
                    consumed: androidx.compose.ui.geometry.Offset,
                    available: androidx.compose.ui.geometry.Offset,
                    source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
                ): androidx.compose.ui.geometry.Offset {
                    val delta = available.y
                    if (delta > 0f) {
                        dragY += delta
                        val newProgress = (1f - (dragY / (screenHeight * 0.8f))).coerceIn(0f, 1f)
                        scope.launch {
                            progress.snapTo(newProgress)
                        }
                        return androidx.compose.ui.geometry.Offset(0f, delta)
                    }
                    return androidx.compose.ui.geometry.Offset.Zero
                }
                
                override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                    if (dragY > 0f) {
                        scope.launch {
                            if (dragY > screenHeight * 0.2f) {
                                progress.animateTo(0f, DetailExitSpringSpec)
                                SharedTransitionState.isDetailOpen = false
                                onBack()
                            } else {
                                progress.animateTo(1f, DetailEntrySpringSpec)
                            }
                            dragY = 0f
                        }
                        return available
                    }
                    return androidx.compose.ui.unit.Velocity.Zero
                }
            }
        }
        
        val dragModifier = if (enableSwipeToDismiss) {
            Modifier
                .nestedScroll(nestedScrollConnection)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragY = 0f },
                        onDragEnd = {
                            scope.launch {
                                if (dragY > screenHeight * 0.2f) {
                                    progress.animateTo(0f, DetailExitSpringSpec)
                                    SharedTransitionState.isDetailOpen = false
                                    onBack()
                                } else {
                                    progress.animateTo(1f, DetailEntrySpringSpec)
                                }
                                dragY = 0f
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                progress.animateTo(1f, DetailEntrySpringSpec)
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
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(dragModifier)
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
}
