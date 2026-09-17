/*
 * Copyright 2025 Kyant (https://github.com/Kyant0/AndroidLiquidGlass)
 * Licensed under the Apache License, Version 2.0. See THIRD_PARTY_NOTICES.md.
 *
 * From the Backdrop Catalog sample app (LiquidBottomTabs + LiquidBottomTab). Changes from
 * upstream: package relocated, made internal, configurable colours, sized by React Native, the
 * Compose tab content slot became a draw callback (the React Native tab views), the bar itself
 * takes the touches (a tap selects the tab under the finger, like upstream's tab click; a
 * horizontal drag from anywhere on the bar scrubs the droplet between tabs, iOS-style), and the
 * bar can minimize (shorter and narrower, with the labels fading out).
 */
package expo.modules.androidglassview.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import expo.modules.androidglassview.backdrop.Backdrop
import expo.modules.androidglassview.GlassState
import expo.modules.androidglassview.glassEffects
import expo.modules.androidglassview.glassSurface
import expo.modules.androidglassview.backdrop.isRenderEffectSupported
import expo.modules.androidglassview.backdrop.backdrops.layerBackdrop
import expo.modules.androidglassview.backdrop.backdrops.rememberCombinedBackdrop
import expo.modules.androidglassview.backdrop.backdrops.rememberLayerBackdrop
import expo.modules.androidglassview.backdrop.drawBackdrop
import expo.modules.androidglassview.backdrop.effects.blur
import expo.modules.androidglassview.backdrop.effects.lens
import expo.modules.androidglassview.backdrop.effects.vibrancy
import expo.modules.androidglassview.backdrop.highlight.Highlight
import expo.modules.androidglassview.backdrop.shadow.InnerShadow
import expo.modules.androidglassview.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

/**
 * @param drawTabs draws the tab content. `origin` is the position of the current draw scope in
 *   the bar, `frame` where the tab slots are right now, `tint` (when set) turns the content into
 *   a silhouette of that colour and `scale` magnifies every tab.
 * @param minimized whether the bar should be minimized: shorter and narrower, labels gone. The
 *   change is animated.
 * @param onExpand called when the user touches the bar (a tap, a scrub or the droplet).
 */
@Composable
internal fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    state: GlassState,
    tabsCount: Int,
    drawTabs: DrawScope.(origin: Offset, frame: TabsFrame, tint: Color?, scale: Float) -> Unit,
    modifier: Modifier = Modifier,
    minimized: () -> Boolean = { false },
    onExpand: () -> Unit = {},
    accentColor: Color? = null,
    containerColor: Color? = null
) {
    val isLightTheme = !state.dark
    val effectsSupported = isRenderEffectSupported()
    val accent = accentColor
        ?: if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)

    val tabsBackdrop = rememberLayerBackdrop()
    val currentOnExpand by rememberUpdatedState(onExpand)

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val maxWidth = constraints.maxWidth
        val maxHeight = constraints.maxHeight
        val geometry = remember(density, maxWidth, maxHeight, tabsCount) {
            TabsGeometry(density, maxWidth.toFloat(), maxHeight.toFloat(), tabsCount)
        }

        // 0 = expanded, 1 = minimized. Only read while laying out and drawing, so the animation
        // neither recomposes nor restarts the gestures.
        val minimizeAnimation = remember { Animatable(if (minimized()) 1f else 0f) }
        LaunchedEffect(minimizeAnimation) {
            snapshotFlow { minimized() }
                .collectLatest { isMinimized ->
                    minimizeAnimation.animateTo(if (isMinimized) 1f else 0f, MinimizeAnimationSpec)
                }
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density, maxWidth) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }
        val dampedDragAnimation = remember(animationScope, tabsCount, geometry) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = currentIndex.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = { currentOnExpand() },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().coerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    val tabWidth = geometry.tabWidth(minimizeAnimation.value)
                    if (tabWidth > 0f) {
                        updateValue(
                            (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                                .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                        )
                    }
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    currentIndex = index
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope, dampedDragAnimation) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    val tabWidth = geometry.tabWidth(minimizeAnimation.value)
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        // The bar: glass + the tab content.
        Box(
            Modifier
                .placeAt { geometry.barBounds(minimizeAnimation.value) }
                .graphicsLayer {
                    translationX = panelOffset
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(state.cornerRadius.dp) },
                    effects = { glassEffects(state) },
                    highlight = { if (state.highlight) state.rim else null },
                    shadow = { if (state.shadow) state.dropShadow else null },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = {
                        glassSurface(state, effectsSupported)
                        containerColor?.let { drawRect(it) }
                    },
                    onDrawFront = {
                        val progress = minimizeAnimation.value
                        drawTabs(
                            Offset(geometry.inset(progress), geometry.barTop(progress)),
                            geometry.frame(progress),
                            null,
                            1f
                        )
                    }
                )
                .then(interactiveHighlight.modifier)
                // A tap selects the tab under the finger, exactly like upstream's tab click. Unlike
                // upstream (where only the droplet can be dragged), a horizontal drag that starts
                // anywhere on the bar pulls the droplet under the finger and scrubs between tabs;
                // releasing selects the nearest one.
                .pointerInput(dampedDragAnimation, tabsCount, isLtr, geometry) {
                    val lastIndex = (tabsCount - 1).toFloat()
                    // x is in the bar's own coordinates, which follow the minimize animation.
                    val valueAt: (Float) -> Float = { x ->
                        val tabWidth = geometry.tabWidth(minimizeAnimation.value)
                        val slot =
                            if (tabWidth > 0f) ((x - geometry.padding) / tabWidth - 0.5f).fastCoerceIn(0f, lastIndex)
                            else 0f
                        if (isLtr) slot else lastIndex - slot
                    }
                    var startX = 0f
                    var scrubbing = false
                    var scrubValue = 0f
                    val settle: () -> Unit = {
                        val target = scrubValue.fastRoundToInt().coerceIn(0, tabsCount - 1)
                        if (target != currentIndex) {
                            currentIndex = target
                        } else {
                            dampedDragAnimation.animateToValue(target.toFloat())
                        }
                    }
                    inspectDragGestures(
                        onDragStart = { down ->
                            startX = down.position.x
                            scrubbing = false
                        },
                        onDragEnd = { up ->
                            if (scrubbing) {
                                settle()
                            } else {
                                currentIndex = valueAt(up.position.x).fastRoundToInt()
                                    .coerceIn(0, tabsCount - 1)
                                currentOnExpand()
                            }
                            scrubbing = false
                        },
                        onDragCancel = {
                            if (scrubbing) settle()
                            scrubbing = false
                        }
                    ) { change, _ ->
                        if (scrubbing) {
                            scrubValue = valueAt(change.position.x)
                            dampedDragAnimation.updateValue(scrubValue)
                        } else if (abs(change.position.x - startX) > viewConfiguration.touchSlop) {
                            scrubbing = true
                            currentOnExpand()
                            scrubValue = valueAt(change.position.x)
                            dampedDragAnimation.press()
                            // Jump to the finger without feeding the velocity (no stretch).
                            dampedDragAnimation.moveToValue(scrubValue)
                        }
                    }
                }
        )

        // Invisible copy of the bar with the content tinted in the accent colour. It is only
        // recorded into `tabsBackdrop`, which the droplet refracts: under the droplet the tabs
        // show up in the accent colour, magnified while it is pressed.
        Box(
            Modifier
                .placeAt { geometry.innerBounds(minimizeAnimation.value) }
                .clearAndSetSemantics {}
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .graphicsLayer {
                    translationX = panelOffset
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(state.cornerRadius.dp) },
                    effects = { glassEffects(state) },
                    highlight = { if (state.highlight) state.rim else null },
                    shadow = { if (state.shadow) state.dropShadow else null },
                    onDrawSurface = {
                        glassSurface(state, effectsSupported)
                        containerColor?.let { drawRect(it) }
                    },
                    onDrawFront = {
                        val progress = minimizeAnimation.value
                        drawTabs(
                            Offset(geometry.inset(progress), geometry.barTop(progress) + geometry.padding),
                            geometry.frame(progress),
                            accent,
                            lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                        )
                    }
                )
                .then(interactiveHighlight.modifier)
        )

        // The droplet: selection indicator, draggable.
        Box(
            Modifier
                .placeAt { geometry.dropletBounds(minimizeAnimation.value) }
                .graphicsLayer {
                    val tabWidth = geometry.tabWidth(minimizeAnimation.value)
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else (tabsCount - 1 - dampedDragAnimation.value) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { CapsuleShape },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        val sizeScale = geometry.sizeScale(minimizeAnimation.value)
                        lens(
                            10f.dp.toPx() * progress * sizeScale,
                            14f.dp.toPx() * progress * sizeScale,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 8f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.1f)
                            else Color.White.copy(0.1f),
                            alpha = 1f - progress
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                )
        )
    }
}

private val BarPadding = 4.dp

/** Minimized bar, like iOS 26 tab bars on scroll: shorter and narrower. */
private val MinimizedHeight = 48.dp
private val MinimizedInset = 34.dp

/** The minimized bar never squeezes a tab below this width. */
private val MinimizedMinTabWidth = 48.dp

/** Critically damped: a direction change mid-animation retargets smoothly, without overshoot. */
private val MinimizeAnimationSpec = spring(dampingRatio = 1f, stiffness = 600f, visibilityThreshold = 0.001f)

/** The bar's layout as a function of the minimize progress, in px and host coordinates. */
private class TabsGeometry(density: Density, val width: Float, val height: Float, val tabsCount: Int) {

    val padding: Float
    private val minimizedHeight: Float
    private val minimizedInset: Float

    init {
        with(density) {
            padding = BarPadding.toPx()
            minimizedHeight = min(height, MinimizedHeight.toPx())
            minimizedInset = MinimizedInset.toPx()
                .coerceAtMost(((width - tabsCount * MinimizedMinTabWidth.toPx()) / 2f).coerceAtLeast(0f))
        }
    }

    fun barHeight(progress: Float): Float = lerp(height, minimizedHeight, progress)

    fun inset(progress: Float): Float = lerp(0f, minimizedInset, progress)

    // Bottom-anchored: a minimizing bar sinks towards the edge of the screen.
    fun barTop(progress: Float): Float = height - barHeight(progress)

    fun tabWidth(progress: Float): Float =
        ((width - inset(progress) * 2f - padding * 2f) / tabsCount).coerceAtLeast(0f)

    fun slotsLeft(progress: Float): Float = inset(progress) + padding

    /** Refraction sizes shrink with the bar. */
    fun sizeScale(progress: Float): Float = if (height > 0f) barHeight(progress) / height else 1f

    fun barBounds(progress: Float): Rect =
        Rect(inset(progress), barTop(progress), width - inset(progress), height)

    fun innerBounds(progress: Float): Rect =
        Rect(inset(progress), barTop(progress) + padding, width - inset(progress), height - padding)

    fun dropletBounds(progress: Float): Rect {
        val left = slotsLeft(progress)
        return Rect(left, barTop(progress) + padding, left + tabWidth(progress), height - padding)
    }

    fun frame(progress: Float): TabsFrame {
        val expandedTabWidth = tabWidth(0f)
        return TabsFrame(
            minimizeProgress = progress,
            expandedSlotsLeft = slotsLeft(0f),
            slotsLeft = slotsLeft(progress),
            slotsScale = if (expandedTabWidth > 0f) tabWidth(progress) / expandedTabWidth else 1f,
            centerY = barTop(progress) + barHeight(progress) / 2f
        )
    }
}

/** Lays the node out at [bounds]; read while laying out, so animating them only re-lays it out. */
private fun Modifier.placeAt(bounds: () -> Rect): Modifier =
    layout { measurable, constraints ->
        val rect = bounds()
        val placeable = measurable.measure(
            Constraints.fixed(
                rect.width.fastRoundToInt().coerceAtLeast(0),
                rect.height.fastRoundToInt().coerceAtLeast(0)
            )
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(rect.left.fastRoundToInt(), rect.top.fastRoundToInt())
        }
    }
