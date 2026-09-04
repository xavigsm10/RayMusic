@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.mrtdk.liquid_glass.ui.components.floatingtabbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
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
import com.mrtdk.liquid_glass.ui.components.shapes.ContinuousRoundedRectangle
import com.mrtdk.liquid_glass.utils.DampedDragAnimation
import com.mrtdk.liquid_glass.utils.InteractiveHighlight
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private val GooeyPeakBlur = 12.dp
private const val GooeyDurationMs = 300

private const val PuckRestHighlightAlpha = 0.5f
private const val PuckRestShadowAlpha = 0.35f

private enum class FloatingTabBarVisual { INLINE, EXPANDED, SEARCH_EXPANDED }

private val SearchBarRowHeight = 48.dp

internal val LocalTabBarBackdropFrozen = staticCompositionLocalOf<() -> Boolean> { { false } }

/**
 * A floating tab bar that transitions between inline and expanded states based on scroll behavior.
 */
@Composable
fun FloatingTabBar(
    selectedTabKey: Any?,
    scrollConnection: FloatingTabBarScrollConnection,
    modifier: Modifier = Modifier,
    tabBarContentModifier: Modifier = Modifier,
    inlineAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? = null,
    expandedAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? = null,
    colors: FloatingTabBarColors = FloatingTabBarDefaults.colors(),
    shapes: FloatingTabBarShapes = FloatingTabBarDefaults.shapes(),
    sizes: FloatingTabBarSizes = FloatingTabBarDefaults.sizes(),
    elevations: FloatingTabBarElevations = FloatingTabBarDefaults.elevations(),
    backdrop: Backdrop? = null,
    accentColor: Color? = null,
    tabPosition: (() -> Float?)? = null,
    searchMode: Boolean = false,
    searchBarContent: (@Composable (Modifier) -> Unit)? = null,
    expandedContentWidthPx: Int? = null,
    onExpandedWidthChanged: ((Int) -> Unit)? = null,
    content: FloatingTabBarScope.() -> Unit
) {
    val scope = FloatingTabBarScopeImpl().apply { content() }
    val isAccessoryShared = inlineAccessory != null && expandedAccessory != null

    val visual = when {
        scrollConnection.isInline -> FloatingTabBarVisual.INLINE
        searchMode && searchBarContent != null -> FloatingTabBarVisual.SEARCH_EXPANDED
        else -> FloatingTabBarVisual.EXPANDED
    }

    val transition = updateTransition(targetState = visual, label = "floatingTabBarVisual")
    val gooeyBlurPx = with(LocalDensity.current) { GooeyPeakBlur.toPx() }
    val gooeyProgress by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = GooeyDurationMs
                0f at 0
                1f atFraction 0.5f using FastOutSlowInEasing
                0f at GooeyDurationMs
            }
        },
        label = "gooeyProgress"
    ) { _ -> 0f }

    SharedTransitionLayout(modifier = modifier) {
        val frozenWhileAnimating = remember(transition) {
            { transition.currentState != transition.targetState }
        }
        CompositionLocalProvider(LocalTabBarBackdropFrozen provides frozenWhileAnimating) {
            Box {
                transition.AnimatedContent(
                    transitionSpec = {
                        fadeIn(tween(220, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(tween(180, easing = FastOutSlowInEasing))
                    },
                    contentAlignment = Alignment.BottomCenter
                ) { targetVisual ->
                    when (targetVisual) {
                        FloatingTabBarVisual.INLINE -> InlineBar(
                            scope = scope,
                            selectedTabKey = selectedTabKey,
                            accessory = inlineAccessory,
                            isAccessoryShared = isAccessoryShared,
                            onInlineTabClick = { scrollConnection.expand() },
                            colors = colors,
                            shapes = shapes,
                            sizes = sizes,
                            elevations = elevations,
                            tabBarContentModifier = tabBarContentModifier,
                            animatedVisibilityScope = this@AnimatedContent
                        )
                        FloatingTabBarVisual.EXPANDED -> ExpandedBar(
                            scope = scope,
                            selectedTabKey = selectedTabKey,
                            tabPosition = tabPosition,
                            accessory = expandedAccessory,
                            isAccessoryShared = isAccessoryShared,
                            colors = colors,
                            shapes = shapes,
                            sizes = sizes,
                            elevations = elevations,
                            tabBarContentModifier = tabBarContentModifier,
                            animatedVisibilityScope = this@AnimatedContent,
                            backdrop = backdrop,
                            accentColor = accentColor,
                            onWidthMeasured = onExpandedWidthChanged
                        )
                        FloatingTabBarVisual.SEARCH_EXPANDED -> SearchExpandedBar(
                            scope = scope,
                            selectedTabKey = selectedTabKey,
                            accessory = expandedAccessory,
                            isAccessoryShared = isAccessoryShared,
                            colors = colors,
                            shapes = shapes,
                            sizes = sizes,
                            elevations = elevations,
                            tabBarContentModifier = tabBarContentModifier,
                            animatedVisibilityScope = this@AnimatedContent,
                            searchBarContent = searchBarContent ?: {},
                            targetWidthPx = expandedContentWidthPx
                        )
                    }
                }
            }
        }
    }
}

/**
 * A [NestedScrollConnection] that handles scroll events to transition between inline and expanded states.
 */
class FloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    private val scrollThresholdPx: Float,
    private val expandThresholdPx: Float = scrollThresholdPx,
    private val inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown
) : NestedScrollConnection {
    var isInline by mutableStateOf(initialIsInline)
        private set

    private var accumulatedScroll = 0f

    fun expand() {
        isInline = false
        accumulatedScroll = 0f
    }

    fun inline() {
        isInline = true
        accumulatedScroll = 0f
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (inlineBehavior == FloatingTabBarInlineBehavior.Never) {
            return Offset.Zero
        }

        val scrollDelta = available.y

        if ((accumulatedScroll > 0 && scrollDelta < 0) || (accumulatedScroll < 0 && scrollDelta > 0)) {
            accumulatedScroll = 0f
        }

        accumulatedScroll += scrollDelta

        when (inlineBehavior) {
            FloatingTabBarInlineBehavior.OnScrollDown -> {
                if (accumulatedScroll <= -scrollThresholdPx && !isInline) {
                    isInline = true
                    accumulatedScroll = 0f
                } else if (accumulatedScroll >= expandThresholdPx && isInline) {
                    isInline = false
                    accumulatedScroll = 0f
                }
            }
            FloatingTabBarInlineBehavior.OnScrollUp -> {
                if (accumulatedScroll >= scrollThresholdPx && !isInline) {
                    isInline = true
                    accumulatedScroll = 0f
                } else if (accumulatedScroll <= -scrollThresholdPx && isInline) {
                    isInline = false
                    accumulatedScroll = 0f
                }
            }
            FloatingTabBarInlineBehavior.Never -> {}
        }

        return Offset.Zero
    }
}

@Composable
fun rememberFloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    scrollThreshold: Dp = 50.dp,
    expandThreshold: Dp = 8.dp,
    inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown
): FloatingTabBarScrollConnection = with(LocalDensity.current) {
    val scrollThresholdPx = scrollThreshold.toPx()
    val expandThresholdPx = expandThreshold.toPx()
    remember(scrollThresholdPx, expandThresholdPx, inlineBehavior, initialIsInline) {
        FloatingTabBarScrollConnection(
            initialIsInline,
            scrollThresholdPx,
            expandThresholdPx,
            inlineBehavior,
        )
    }
}

enum class FloatingTabBarInlineBehavior {
    Never,
    OnScrollDown,
    OnScrollUp
}

interface FloatingTabBarScope {
    fun tab(
        key: Any,
        title: @Composable () -> Unit,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)? = { LocalIndication.current }
    )

    fun standaloneTab(
        key: Any,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)? = { LocalIndication.current },
        title: @Composable () -> Unit = {}
    )
}

@Composable
private fun Modifier.tapClickable(
    indication: Indication?,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        onClick = onClick,
        indication = indication,
        interactionSource = interactionSource
    )
}

@Composable
private fun SharedTransitionScope.InlineBar(
    scope: FloatingTabBarScopeImpl,
    selectedTabKey: Any?,
    accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
    isAccessoryShared: Boolean,
    onInlineTabClick: () -> Unit,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    elevations: FloatingTabBarElevations,
    tabBarContentModifier: Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val inlineTab = scope.getInlineTab(selectedTabKey)
    val standaloneTab = scope.standaloneTab
    val hasInlineTab = inlineTab != null

    Row(
        horizontalArrangement = Arrangement.spacedBy(sizes.componentSpacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (accessory == null) Modifier.wrapContentWidth() else Modifier)
            .height(IntrinsicSize.Max)
    ) {
        if (hasInlineTab && inlineTab != null) {
            InlineTab(
                inlineTab = inlineTab,
                onInlineTabClick = onInlineTabClick,
                shapes = shapes,
                sizes = sizes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                tabBarContentModifier = tabBarContentModifier,
                modifier = Modifier
            )
        }

        if (accessory != null) {
            InlineAccessory(
                accessory = accessory,
                isAccessoryShared = isAccessoryShared,
                shapes = shapes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        if (standaloneTab != null) {
            InlineStandaloneTab(
                standaloneTab = standaloneTab,
                shapes = shapes,
                sizes = sizes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                tabBarContentModifier = tabBarContentModifier,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.InlineTab(
    inlineTab: FloatingTabBarTab,
    onInlineTabClick: () -> Unit,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: Modifier
) {
    Box(
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState("tabGroup"),
                animatedVisibilityScope = animatedVisibilityScope,
                zIndexInOverlay = 1f
            )
            .shadow(
                shape = shapes.tabBarShape,
                elevation = elevations.inlineElevation
            )
            .background(
                color = colors.backgroundColor,
                shape = shapes.tabBarShape
            )
            .clip(shapes.tabBarShape)
            .then(tabBarContentModifier)
            .tapClickable(
                indication = inlineTab.indication?.invoke(),
                onClick = {
                    onInlineTabClick()
                    inlineTab.onClick()
                }
            )
            .padding(sizes.tabInlineContentPadding)
    ) {
        Tab(
            icon = {
                Box(
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState("tab#${inlineTab.key}-icon"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        zIndexInOverlay = 1f
                    )
                ) {
                    inlineTab.icon()
                }
            },
            title = { inlineTab.title() },
            isInline = true
        )
    }
}

@Composable
private fun SharedTransitionScope.InlineStandaloneTab(
    standaloneTab: FloatingTabBarTab,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: Modifier
) {
    val glowScope = rememberCoroutineScope()
    val glow = remember(glowScope) { InteractiveHighlight(animationScope = glowScope) }

    Tab(
        icon = standaloneTab.icon,
        title = standaloneTab.title,
        isInline = true,
        isStandalone = true,
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState("standaloneTab"),
                animatedVisibilityScope = animatedVisibilityScope,
                zIndexInOverlay = 1f
            )
            .shadow(
                shape = shapes.standaloneTabShape,
                elevation = elevations.inlineElevation
            )
            .background(
                color = colors.backgroundColor,
                shape = shapes.standaloneTabShape
            )
            .clip(shapes.standaloneTabShape)
            .then(tabBarContentModifier)
            .then(glow.gestureModifier)
            .then(glow.modifier)
            .tapClickable(
                indication = standaloneTab.indication?.invoke(),
                onClick = standaloneTab.onClick
            )
            .padding(sizes.tabInlineContentPadding)
    )
}

@Composable
private fun SharedTransitionScope.InlineAccessory(
    accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
    isAccessoryShared: Boolean,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier
) {
    accessory?.let { accessoryComposable ->
        Box(
            modifier = modifier
                .then(
                    if (isAccessoryShared) {
                        Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState("accessory"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    } else {
                        Modifier.animateEnterExitAccessory(
                            sharedTransitionScope = this,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                )
        ) {
            accessoryComposable(
                Modifier
                    .fillMaxSize()
                    .shadow(
                        shape = shapes.accessoryShape,
                        elevation = elevations.inlineElevation
                    )
                    .background(color = colors.accessoryBackgroundColor, shapes.accessoryShape)
                    .clip(shapes.accessoryShape),
                animatedVisibilityScope
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.ExpandedBar(
    scope: FloatingTabBarScopeImpl,
    selectedTabKey: Any?,
    tabPosition: (() -> Float?)?,
    accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
    isAccessoryShared: Boolean,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    elevations: FloatingTabBarElevations,
    tabBarContentModifier: Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
    backdrop: Backdrop?,
    accentColor: Color?,
    onWidthMeasured: ((Int) -> Unit)? = null,
) {
    val hasTabGroup = scope.tabs.isNotEmpty()
    val standaloneTab = scope.standaloneTab
    val density = LocalDensity.current
    var tabRowWidthPx by remember { mutableIntStateOf(0) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(sizes.componentSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (accessory != null) {
            ExpandedAccessory(
                accessory = accessory,
                isAccessoryShared = isAccessoryShared,
                shapes = shapes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = if (tabRowWidthPx > 0) {
                    Modifier.width(with(density) { tabRowWidthPx.toDp() })
                } else {
                    Modifier.fillMaxWidth()
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(sizes.componentSpacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .onSizeChanged {
                    tabRowWidthPx = it.width
                    onWidthMeasured?.invoke(it.width)
                }
        ) {
            if (hasTabGroup) {
                ExpandedTabs(
                    scope = scope,
                    selectedTabKey = selectedTabKey,
                    tabPosition = tabPosition,
                    shapes = shapes,
                    sizes = sizes,
                    colors = colors,
                    elevations = elevations,
                    animatedVisibilityScope = animatedVisibilityScope,
                    tabBarContentModifier = tabBarContentModifier,
                    backdrop = backdrop,
                    accentColor = accentColor ?: colors.backgroundColor,
                    modifier = Modifier
                )
            }

            if (standaloneTab != null) {
                ExpandedStandaloneTab(
                    standaloneTab = standaloneTab,
                    shapes = shapes,
                    sizes = sizes,
                    colors = colors,
                    elevations = elevations,
                    animatedVisibilityScope = animatedVisibilityScope,
                    tabBarContentModifier = tabBarContentModifier,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                )
            }
        }
    }
}

@Composable
private fun SharedTransitionScope.SearchExpandedBar(
    scope: FloatingTabBarScopeImpl,
    selectedTabKey: Any?,
    accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
    isAccessoryShared: Boolean,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    elevations: FloatingTabBarElevations,
    tabBarContentModifier: Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
    searchBarContent: @Composable (Modifier) -> Unit,
    targetWidthPx: Int?,
) {
    val inlineTab = scope.getInlineTab(selectedTabKey)
    val density = LocalDensity.current
    val targetWidthModifier = if (targetWidthPx != null && targetWidthPx > 0) {
        Modifier.width(with(density) { targetWidthPx.toDp() })
    } else {
        Modifier.fillMaxWidth()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(sizes.componentSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (accessory != null) {
            ExpandedAccessory(
                accessory = accessory,
                isAccessoryShared = isAccessoryShared,
                shapes = shapes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = targetWidthModifier
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(sizes.componentSpacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(SearchBarRowHeight).then(targetWidthModifier)
        ) {
            if (inlineTab != null) {
                InlineTab(
                    inlineTab = inlineTab,
                    onInlineTabClick = {},
                    shapes = shapes,
                    sizes = sizes,
                    colors = colors,
                    elevations = elevations,
                    animatedVisibilityScope = animatedVisibilityScope,
                    tabBarContentModifier = tabBarContentModifier,
                    modifier = Modifier.fillMaxHeight()
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .sharedElement(
                        sharedContentState = rememberSharedContentState("standaloneTab"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        zIndexInOverlay = 1f
                    )
                    .shadow(
                        shape = shapes.tabBarShape,
                        elevation = elevations.expandedElevation
                    )
                    .background(
                        color = colors.backgroundColor,
                        shape = shapes.tabBarShape
                    )
                    .clip(shapes.tabBarShape)
                    .then(tabBarContentModifier)
            ) {
                searchBarContent(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun SharedTransitionScope.ExpandedStandaloneTab(
    standaloneTab: FloatingTabBarTab,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: Modifier
) {
    val glowScope = rememberCoroutineScope()
    val glow = remember(glowScope) { InteractiveHighlight(animationScope = glowScope) }

    Tab(
        icon = standaloneTab.icon,
        title = standaloneTab.title,
        isInline = false,
        isStandalone = true,
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState("standaloneTab"),
                animatedVisibilityScope = animatedVisibilityScope,
                zIndexInOverlay = 1f
            )
            .shadow(
                shape = shapes.standaloneTabShape,
                elevation = elevations.expandedElevation
            )
            .background(
                color = colors.backgroundColor,
                shape = shapes.standaloneTabShape
            )
            .clip(shapes.standaloneTabShape)
            .then(tabBarContentModifier)
            .then(glow.gestureModifier)
            .then(glow.modifier)
            .tapClickable(
                indication = standaloneTab.indication?.invoke(),
                onClick = standaloneTab.onClick
            )
            .padding(sizes.tabInlineContentPadding)
    )
}

@Composable
private fun SharedTransitionScope.ExpandedAccessory(
    accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
    isAccessoryShared: Boolean,
    shapes: FloatingTabBarShapes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier
) {
    accessory?.let { accessoryComposable ->
        Box(
            modifier = modifier
                .then(
                    if (isAccessoryShared) {
                        Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState("accessory"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    } else {
                        Modifier.animateEnterExitAccessory(
                            sharedTransitionScope = this,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                )
        ) {
            accessoryComposable(
                Modifier
                    .shadow(
                        shape = shapes.accessoryShape,
                        elevation = elevations.expandedElevation
                    )
                    .background(color = colors.accessoryBackgroundColor, shapes.accessoryShape)
                    .clip(shapes.accessoryShape),
                animatedVisibilityScope
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.ExpandedTabs(
    scope: FloatingTabBarScopeImpl,
    selectedTabKey: Any?,
    tabPosition: (() -> Float?)?,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: Modifier,
    backdrop: Backdrop?,
    accentColor: Color
) {
    val allTabs = scope.tabs
    val tabsCount = allTabs.size
    if (tabsCount == 0) return

    val currentTabs = rememberUpdatedState(allTabs)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val isLtr = layoutDirection == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    val puckWash = if (isDark) Color(28, 27, 28) else Color(0xFFF2F2F2)
    val puckRestAlpha = 0.8f

    val tabWidthPx = with(density) { sizes.tabWidth.toPx() }
    val paddingStartPx = with(density) { sizes.tabBarContentPadding.calculateStartPadding(layoutDirection).toPx() }
    val paddingEndPx = with(density) { sizes.tabBarContentPadding.calculateEndPadding(layoutDirection).toPx() }
    val totalWidthPx = tabWidthPx * tabsCount + paddingStartPx + paddingEndPx

    val offsetAnimation = remember(tabsCount) { Animatable(0f) }
    val panelOffset by remember(density, totalWidthPx) {
        derivedStateOf {
            val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
            with(density) {
                4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember(tabsCount) {
        mutableIntStateOf(
            allTabs.indexOfFirst { it.key == selectedTabKey }.coerceIn(0, tabsCount - 1)
        )
    }
    var hasDraggedPuck = false
    val pressedScale = 78f / 56f
    val dampedDragAnimation = remember(animationScope, tabsCount) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = currentIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = pressedScale,
            onDragStarted = { hasDraggedPuck = false },
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().coerceIn(0, tabsCount - 1)
                currentIndex = targetIndex
                updateValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
                if (hasDraggedPuck) {
                    currentTabs.value.getOrNull(targetIndex)?.onClick?.invoke()
                }
            },
            onDrag = { _, dragAmount ->
                if (dragAmount != Offset.Zero) hasDraggedPuck = true
                updateValue(
                    (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                        .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                )
                animationScope.launch {
                    offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                }
            },
            velocityDampingRatio = 1f,
        )
    }

    var hasSyncedSelection by remember(tabsCount) { mutableStateOf(false) }

    if (tabPosition != null) {
        LaunchedEffect(dampedDragAnimation, tabPosition) {
            snapshotFlow { tabPosition() }.collect { position ->
                if (position != null) dampedDragAnimation.snapValue(position)
            }
        }
    }

    LaunchedEffect(selectedTabKey, tabsCount) {
        val index = allTabs.indexOfFirst { it.key == selectedTabKey }
        if (index != -1) {
            currentIndex = index
            if (!hasSyncedSelection) {
                hasSyncedSelection = true
                dampedDragAnimation.updateValue(index.toFloat())
                return@LaunchedEffect
            }
            dampedDragAnimation.animateToValue(index.toFloat())
        }
    }

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                Offset(
                    if (isLtr) paddingStartPx + (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    else size.width - paddingStartPx - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset,
                    size.height / 2f
                )
            }
        )
    }

    val tabsBackdrop = rememberLayerBackdrop()

    Box(modifier.width(with(density) { totalWidthPx.toDp() })) {
        Row(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = panelOffset }
                .sharedElement(
                    sharedContentState = rememberSharedContentState("tabGroup"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    zIndexInOverlay = 1f
                )
                .shadow(
                    shape = shapes.tabBarShape,
                    elevation = elevations.expandedElevation
                )
                .background(
                    color = colors.backgroundColor,
                    shape = shapes.tabBarShape
                )
                .clip(shapes.tabBarShape)
                .then(tabBarContentModifier)
                .then(interactiveHighlight.modifier)
                .padding(sizes.tabBarContentPadding),
            horizontalArrangement = Arrangement.spacedBy(sizes.tabSpacing)
        ) {
            allTabs.forEachIndexed { index, tab ->
                Tab(
                    icon = {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = if (tab.key == selectedTabKey) 1f else 0.6f
                                }
                                .then(
                                    if (tab.key == selectedTabKey) {
                                        Modifier.sharedElement(
                                            sharedContentState = rememberSharedContentState("tab#${tab.key}-icon"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            zIndexInOverlay = 1f
                                        )
                                    } else {
                                        Modifier.animateEnterExitTab(
                                            sharedTransitionScope = this@ExpandedTabs,
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }
                                )
                        ) {
                            tab.icon()
                        }
                    },
                    title = {
                        Box(
                            Modifier.animateEnterExitTab(
                                sharedTransitionScope = this@ExpandedTabs,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        ) {
                            tab.title()
                        }
                    },
                    isInline = false,
                    isStandalone = false,
                    contentScale = if (backdrop != null && index == currentIndex) {
                        lerp(1f, 1.12f, dampedDragAnimation.pressProgress)
                    } else {
                        1f
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .skipToLookaheadSize()
                        .clip(shapes.tabShape)
                        .tapClickable(
                            indication = tab.indication?.invoke(),
                            onClick = tab.onClick
                        )
                        .padding(sizes.tabExpandedContentPadding)
                )
            }
        }

        if (backdrop != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .padding(sizes.tabBarContentPadding)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shapes.tabBarShape },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(
                                15f.dp.toPx() * progress,
                                18f.dp.toPx() * progress
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(colors.backgroundColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                horizontalArrangement = Arrangement.spacedBy(sizes.tabSpacing)
            ) {
                allTabs.forEachIndexed { index, tab ->
                    Tab(
                        icon = tab.icon,
                        title = tab.title,
                        isInline = false,
                        isStandalone = false,
                        contentScale = if (index == currentIndex) {
                            lerp(1f, 1.12f, dampedDragAnimation.pressProgress)
                        } else {
                            1f
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .skipToLookaheadSize()
                            .padding(sizes.tabExpandedContentPadding)
                    )
                }
            }
        }

        Box(
            Modifier
                .padding(sizes.tabBarContentPadding)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidthPx + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidthPx + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .width(sizes.tabWidth)
                .fillMaxHeight()
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                            shape = { shapes.tabShape },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                blur(3f.dp.toPx() * (1f - progress))
                                lens(
                                    lerp(0f.dp.toPx(), 10f.dp.toPx(), progress),
                                    lerp(0f.dp.toPx(), 12f.dp.toPx(), progress),
                                    chromaticAberration = progress > 0.01f
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(
                                    alpha = lerp(PuckRestHighlightAlpha, 1f, progress)
                                )
                            },
                            shadow = {
                                val progress = dampedDragAnimation.pressProgress
                                Shadow(alpha = lerp(PuckRestShadowAlpha, 1f, progress))
                            },
                            innerShadow = {
                                val progress = dampedDragAnimation.pressProgress
                                InnerShadow(radius = 8f.dp * progress, alpha = progress)
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
                                drawRect(puckWash.copy(alpha = puckRestAlpha * (1f - 0.75f * progress)))
                            }
                        )
                    } else {
                        // Sólido (Material 3) - No transparente, sin modo burbuja al presionar
                        val solidIndicatorColor = if (isDark) Color(0xFF383840) else Color(0xFFD6D6DC)
                        Modifier
                            .graphicsLayer {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            }
                            .shadow(
                                shape = shapes.tabShape,
                                elevation = 2.dp
                            )
                            .background(
                                color = solidIndicatorColor,
                                shape = shapes.tabShape
                            )
                            .clip(shapes.tabShape)
                    }
                )
        )

        val puckIndex by remember(tabsCount) {
            derivedStateOf {
                dampedDragAnimation.value
                    .fastRoundToInt()
                    .coerceIn(0, (tabsCount - 1).coerceAtLeast(0))
            }
        }
        Box(
            Modifier
                .padding(sizes.tabBarContentPadding)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidthPx + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidthPx + panelOffset
                    scaleX = dampedDragAnimation.scaleX
                    scaleY = dampedDragAnimation.scaleY
                    alpha = if (backdrop != null) {
                        (1f - dampedDragAnimation.pressProgress * 3f).fastCoerceIn(0f, 1f)
                    } else {
                        1f
                    }
                }
                .width(sizes.tabWidth)
                .fillMaxHeight()
                .clearAndSetSemantics {},
        ) {
            currentTabs.value.getOrNull(puckIndex)?.let { tab ->
                Tab(
                    icon = { tab.icon() },
                    title = { tab.title() },
                    isInline = false,
                    isStandalone = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(sizes.tabExpandedContentPadding),
                )
            }
        }
    }
}

@Composable
private fun Tab(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    isInline: Boolean,
    modifier: Modifier = Modifier,
    isStandalone: Boolean = false,
    contentScale: Float = 1f
) {
    val showTitle = !isStandalone && !isInline
    Column(
        verticalArrangement = if (showTitle) {
            Arrangement.spacedBy((-2).dp, Alignment.CenterVertically)
        } else {
            Arrangement.Center
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(
                if (contentScale != 1f) {
                    Modifier.graphicsLayer {
                        scaleX = contentScale
                        scaleY = contentScale
                    }
                } else {
                    Modifier
                }
            )
    ) {
        icon()
        if (showTitle) {
            title()
        }
    }
}

@Composable
private fun Modifier.animateEnterExitAccessory(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = with(sharedTransitionScope) {
    with(animatedVisibilityScope) {
        val animatedAlpha by transition.animateFloat(label = "accessoryAlpha") { targetState ->
            when (targetState) {
                EnterExitState.Visible -> 1f
                else -> 0f
            }
        }

        this@animateEnterExitAccessory
            .renderInSharedTransitionScopeOverlay()
            .graphicsLayer(
                compositingStrategy = CompositingStrategy.ModulateAlpha,
                alpha = animatedAlpha
            )
    }
}

@Composable
private fun Modifier.animateEnterExitTab(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = with(sharedTransitionScope) {
    with(animatedVisibilityScope) {
        val enterStartFraction = 0.5f
        val enterEndFraction = 0.8f
        val durationMs = 150

        val animatedAlpha by transition.animateFloat(
            transitionSpec = {
                keyframes {
                    durationMillis = durationMs
                    if (targetState == EnterExitState.Visible) {
                        0f atFraction enterStartFraction using FastOutSlowInEasing
                        1f atFraction enterEndFraction
                    }
                }
            },
            label = "tabAlpha"
        ) { targetState ->
            when (targetState) {
                EnterExitState.Visible -> 1f
                else -> 0f
            }
        }

        val blurRadius = with(LocalDensity.current) { 50.dp.toPx() }
        val animatedBlur by transition.animateFloat(
            transitionSpec = {
                keyframes {
                    durationMillis = durationMs
                    if (targetState == EnterExitState.Visible) {
                        blurRadius atFraction enterStartFraction using FastOutSlowInEasing
                        0f atFraction enterEndFraction
                    }
                }
            },
            label = "tabBlur"
        ) { targetState ->
            when (targetState) {
                EnterExitState.Visible -> 0f
                else -> blurRadius
            }
        }

        graphicsLayer {
            alpha = animatedAlpha
            renderEffect = BlurEffect(
                radiusX = animatedBlur,
                radiusY = animatedBlur
            )
        }
    }
}

private class FloatingTabBarScopeImpl : FloatingTabBarScope {
    val tabs = mutableStateListOf<FloatingTabBarTab>()
    var standaloneTab: FloatingTabBarTab? by mutableStateOf(null)
        private set
    private var inlineTab: FloatingTabBarTab? = null

    fun getInlineTab(selectedTabKey: Any?): FloatingTabBarTab? {
        val selectedTab = tabs.find { it.key == selectedTabKey }
        if (selectedTab != null) {
            inlineTab = selectedTab
            return selectedTab
        }
        return inlineTab
    }

    override fun tab(
        key: Any,
        title: @Composable () -> Unit,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)?
    ) {
        tabs.add(
            FloatingTabBarTab(
                key = key,
                title = title,
                icon = icon,
                onClick = onClick,
                indication = indication
            )
        )
    }

    override fun standaloneTab(
        key: Any,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)?,
        title: @Composable () -> Unit
    ) {
        standaloneTab = FloatingTabBarTab(
            key = key,
            title = title,
            icon = icon,
            onClick = onClick,
            indication = indication
        )
    }
}

private data class FloatingTabBarTab(
    val key: Any,
    val title: @Composable () -> Unit,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val indication: (@Composable () -> Indication)?
)

@Immutable
data class FloatingTabBarColors(
    val backgroundColor: Color,
    val accessoryBackgroundColor: Color,
)

@Immutable
data class FloatingTabBarShapes(
    val tabBarShape: Shape,
    val tabShape: Shape,
    val standaloneTabShape: Shape,
    val accessoryShape: Shape,
)

@Immutable
data class FloatingTabBarElevations(
    val inlineElevation: Dp,
    val expandedElevation: Dp,
)

@Immutable
data class FloatingTabBarSizes(
    val tabBarContentPadding: PaddingValues,
    val tabInlineContentPadding: PaddingValues,
    val tabExpandedContentPadding: PaddingValues,
    val componentSpacing: Dp,
    val tabSpacing: Dp,
    val tabWidth: Dp = FloatingTabBarDefaults.TabWidth,
)

object FloatingTabBarDefaults {
    val TabWidth: Dp = 88.dp

    @Composable
    fun colors(
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        accessoryBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ): FloatingTabBarColors = FloatingTabBarColors(
        backgroundColor = backgroundColor,
        accessoryBackgroundColor = accessoryBackgroundColor,
    )

    @Composable
    fun shapes(
        tabBarShape: Shape = ContinuousRoundedRectangle(percent = 50),
        tabShape: Shape = ContinuousRoundedRectangle(percent = 50),
        standaloneTabShape: Shape = ContinuousRoundedRectangle(percent = 50),
        accessoryShape: Shape = ContinuousRoundedRectangle(percent = 50),
    ): FloatingTabBarShapes = FloatingTabBarShapes(
        tabBarShape = tabBarShape,
        tabShape = tabShape,
        standaloneTabShape = standaloneTabShape,
        accessoryShape = accessoryShape,
    )

    @Composable
    fun sizes(
        tabBarContentPadding: PaddingValues = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
        tabInlineContentPadding: PaddingValues = PaddingValues(10.dp),
        tabExpandedContentPadding: PaddingValues = PaddingValues(vertical = 6.dp, horizontal = 6.dp),
        componentSpacing: Dp = 8.dp,
        tabSpacing: Dp = 0.dp,
        tabWidth: Dp = TabWidth,
    ): FloatingTabBarSizes = FloatingTabBarSizes(
        tabBarContentPadding = tabBarContentPadding,
        tabInlineContentPadding = tabInlineContentPadding,
        tabExpandedContentPadding = tabExpandedContentPadding,
        componentSpacing = componentSpacing,
        tabSpacing = tabSpacing,
        tabWidth = tabWidth,
    )

    @Composable
    fun elevations(
        inlineElevation: Dp = 6.dp,
        expandedElevation: Dp = 12.dp,
    ): FloatingTabBarElevations = FloatingTabBarElevations(
        inlineElevation = inlineElevation,
        expandedElevation = expandedElevation,
    )
}
