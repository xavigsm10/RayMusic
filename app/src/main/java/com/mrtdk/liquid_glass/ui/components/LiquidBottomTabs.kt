package com.mrtdk.liquid_glass.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.mrtdk.liquid_glass.ui.components.shapes.ContinuousRoundedRectangle
import com.mrtdk.liquid_glass.utils.DampedDragAnimation
import com.mrtdk.liquid_glass.utils.InteractiveHighlight
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private val TabsPillShape = ContinuousRoundedRectangle(percent = 50)

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    accentColor: Color? = null,
    tabPosition: (() -> Float?)? = null,
    backdropScale: Float = 0.33f,
    content: @Composable RowScope.() -> Unit
) {
    val isDarkMode by com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState()
    val isLightTheme = !isDarkMode
    val glassStyle = com.mrtdk.glass.LocalGlassStyle.current
    val isLightweight = com.mrtdk.glass.LocalLightweightGlass.current
    val isUltraPerf by com.mrtdk.liquid_glass.data.LibraryManager.ultraPerformanceMode.collectAsState()
    val currentBackdropStyle by com.mrtdk.liquid_glass.data.LibraryManager.fullArtworkBackdropStyle.collectAsState()
    val isFondoCompleto = currentBackdropStyle == "accord"
    val isSolid = glassStyle == "solid" || isUltraPerf
    val solidBgColor = if (isDarkMode) Color(0xFF242428) else Color(0xFFE8E8EC)

    // In Material 3 mode ("solid"): solid color and NO bubble effect
    if (isSolid) {
        Row(
            modifier
                .clip(TabsPillShape)
                .background(containerColor ?: solidBgColor)
                .height(64f.dp)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        return
    }

    val defaultAccentColor = Color(0xFFFA243C)
    val defaultContainerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    val actualAccentColor = accentColor ?: defaultAccentColor
    val actualContainerColor = containerColor ?: defaultContainerColor

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        // Panel elastic tug offset from Convx sliding animation
        val offsetAnimation = remember(tabsCount) { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(tabsCount) {
            mutableIntStateOf(selectedTabIndex().coerceIn(0, tabsCount - 1))
        }

        var hasDraggedPuck = false

        // DampedDragAnimation with Convx slide mechanics: critically damped and updateValue release
        val dampedDragAnimation = remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = currentIndex.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = { hasDraggedPuck = false },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
                    }
                    if (hasDraggedPuck) {
                        onTabSelected(targetIndex)
                    }
                },
                onDrag = { _, dragAmount ->
                    if (dragAmount != Offset.Zero) hasDraggedPuck = true
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                },
                velocityDampingRatio = 1f
            )
        }

        var hasSyncedSelection by remember(tabsCount) { mutableStateOf(false) }

        // Sync external tab index changes with AndroidLiquidGlass protruding bubble spring animation
        LaunchedEffect(selectedTabIndex, tabsCount) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    if (index in 0 until tabsCount) {
                        currentIndex = index
                        if (!hasSyncedSelection) {
                            hasSyncedSelection = true
                            dampedDragAnimation.updateValue(index.toFloat())
                            return@collectLatest
                        }
                        dampedDragAnimation.animateToValue(index.toFloat())
                    }
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        if (isUltraPerf) {
            val solidPuckColor = if (isDarkMode) Color(0xFF38383C) else Color(0xFFD1D1D6)

            // Layer 1: Background container bar with light blur
            Box(
                Modifier
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            blur(3f.dp.toPx() * backdropScale)
                        },
                        layerBlock = {
                            val progress = dampedDragAnimation.pressProgress
                            val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                            scaleX = scale
                            scaleY = scale
                        },
                        highlight = { Highlight.Default.copy(alpha = 0.35f) },
                        shadow = { Shadow.Default },
                        onDrawSurface = { drawRect(actualContainerColor) },
                        backdropScale = backdropScale
                    )
                    .then(interactiveHighlight.modifier)
                    .height(64f.dp)
                    .fillMaxWidth()
                    .padding(4f.dp)
            )

            // Layer 2: Solid sliding indicator puck (not transparent)
            Box(
                Modifier
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer {
                        translationX =
                            if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                            else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    }
                    .clip(Capsule())
                    .background(solidPuckColor)
                    .height(56f.dp)
                    .fillMaxWidth(1f / tabsCount)
            )

            // Layer 3: Tab icons and labels positioned on top of the solid puck
            Row(
                Modifier
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .then(interactiveHighlight.gestureModifier)
                    .then(dampedDragAnimation.modifier)
                    .height(64f.dp)
                    .fillMaxWidth()
                    .padding(4f.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        } else {
            // Layer 1: Outer capsule container
            if (isFondoCompleto) {
                Box(
                    Modifier
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .clip(TabsPillShape)
                        .background(actualContainerColor)
                        .border(
                            width = 0.75.dp,
                            color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                            shape = TabsPillShape
                        )
                        .then(interactiveHighlight.modifier)
                        .height(64f.dp)
                        .fillMaxWidth()
                        .padding(4f.dp)
                )
            } else {
                Row(
                    Modifier
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { TabsPillShape },
                            effects = {
                                if (!isLightweight) {
                                    vibrancy()
                                    blur(6f.dp.toPx() * backdropScale)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        lens(
                                            refractionHeight = 16f.dp.toPx() * backdropScale,
                                            refractionAmount = 24f.dp.toPx() * backdropScale,
                                            depthEffect = true,
                                            chromaticAberration = false
                                        )
                                    }
                                } else {
                                    blur(3f.dp.toPx() * backdropScale)
                                }
                            },
                            layerBlock = {
                                val progress = dampedDragAnimation.pressProgress
                                val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                                scaleX = scale
                                scaleY = scale
                            },
                            highlight = { Highlight.Default.copy(alpha = 0.25f) },
                            shadow = { Shadow.Default },
                            onDrawSurface = { drawRect(actualContainerColor) },
                            backdropScale = backdropScale
                        )
                        .then(interactiveHighlight.modifier)
                        .height(64f.dp)
                        .fillMaxWidth()
                        .padding(4f.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }

            // Layer 2: Hidden tinted tabs layer captured into tabsBackdrop for glass refraction
            CompositionLocalProvider(
                LocalLiquidBottomTabScale provides {
                    lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                }
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { TabsPillShape },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                if (!isLightweight) {
                                    vibrancy()
                                    blur(6f.dp.toPx() * backdropScale)
                                    if (progress > 0.01f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        lens(
                                            refractionHeight = 16f.dp.toPx() * backdropScale * progress,
                                            refractionAmount = 24f.dp.toPx() * backdropScale * progress,
                                            depthEffect = true,
                                            chromaticAberration = false
                                        )
                                    }
                                } else {
                                    blur(3f.dp.toPx() * backdropScale)
                                }
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = 0.25f * progress)
                            },
                            shadow = { Shadow.Default },
                            onDrawSurface = { drawRect(actualContainerColor) },
                            backdropScale = backdropScale
                        )
                        .then(interactiveHighlight.modifier)
                        .height(56f.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 4f.dp)
                        .graphicsLayer(colorFilter = ColorFilter.tint(actualAccentColor)),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }

            // Layer 3: Original LiquidBottomTabs selection puck from AndroidLiquidGlass
            Box(
                Modifier
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer {
                        translationX =
                            if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                            else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                    }
                    .then(interactiveHighlight.gestureModifier)
                    .then(dampedDragAnimation.modifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { TabsPillShape },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            if (progress > 0.01f) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    lens(
                                        10f.dp.toPx() * backdropScale * progress,
                                        14f.dp.toPx() * backdropScale * progress,
                                        chromaticAberration = true
                                    )
                                }
                            }
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = if (isFondoCompleto) 0.35f + 0.65f * progress else progress)
                        },
                        shadow = {
                            val progress = dampedDragAnimation.pressProgress
                            Shadow(alpha = if (isFondoCompleto) 0.25f + 0.75f * progress else progress)
                        },
                        innerShadow = {
                            val progress = dampedDragAnimation.pressProgress
                            InnerShadow(
                                radius = (if (isFondoCompleto) 6f else 8f).dp * (if (isFondoCompleto) (0.5f + 0.5f * progress) else progress),
                                alpha = if (isFondoCompleto) 0.30f + 0.70f * progress else progress
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
                            if (isFondoCompleto) {
                                if (isDarkMode) {
                                    drawRect(Color.White.copy(alpha = 0.14f))
                                    drawRect(Color(0xFF383840).copy(alpha = 0.35f))
                                } else {
                                    drawRect(Color.Black.copy(alpha = 0.08f))
                                }
                            } else {
                                drawRect(
                                    if (isLightTheme) Color.Black.copy(0.1f)
                                    else Color.White.copy(0.1f),
                                    alpha = 1f - progress
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            }
                        },
                        backdropScale = backdropScale
                    )
                    .then(
                        if (isFondoCompleto) {
                            Modifier.border(
                                width = 0.8.dp,
                                brush = Brush.verticalGradient(
                                    if (isDarkMode) listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.06f))
                                    else listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.40f))
                                ),
                                shape = TabsPillShape
                            )
                        } else Modifier
                    )
                    .height(56f.dp)
                    .fillMaxWidth(1f / tabsCount)
            )

            // Layer 4: When isFondoCompleto, tab row on top of the bubble
            if (isFondoCompleto) {
                Row(
                    Modifier
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .then(interactiveHighlight.gestureModifier)
                        .then(dampedDragAnimation.modifier)
                        .height(64f.dp)
                        .fillMaxWidth()
                        .padding(4f.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}
