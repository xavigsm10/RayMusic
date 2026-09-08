@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.mrtdk.liquid_glass.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.ui.components.FloatingMiniPlayer
import com.mrtdk.liquid_glass.ui.components.LiquidBottomTab
import com.mrtdk.liquid_glass.ui.components.LiquidBottomTabs
import com.mrtdk.liquid_glass.ui.components.LocalBackdrop
import com.mrtdk.liquid_glass.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import com.mrtdk.liquid_glass.ui.components.floatingtabbar.rememberFloatingTabBarScrollConnection
import com.mrtdk.liquid_glass.ui.components.shapes.ContinuousRoundedRectangle
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import kotlinx.coroutines.delay

data class NavTabItem(
    val index: Int,
    val titleRes: Int,
    val iconRes: Int? = null,
    val imageVector: ImageVector? = null
)

// The 4 primary navigation tabs grouped together in the main pill
val MainNavTabs = listOf(
    NavTabItem(0, R.string.nav_inicio, iconRes = R.drawable.nav_inicio),
    NavTabItem(1, R.string.nav_novedades, iconRes = R.drawable.nav_novedades),
    NavTabItem(2, R.string.nav_radio, iconRes = R.drawable.nav_radio),
    NavTabItem(3, R.string.nav_biblioteca, iconRes = R.drawable.nav_biblioteca)
)

private val MiniPlayerShape = ContinuousRoundedRectangle(percent = 50)

private enum class LiquidNavVisualState {
    INLINE,
    EXPANDED,
    SEARCH_EXPANDED
}

/**
 * Bottom navigation bar with Convx-style shared element morph animations:
 * - When tapping the search pill, it morphs and smoothly expands into a wide search bar with direct typing.
 * - Alongside the expanded search bar, the Home ("casa") pill is displayed, allowing a 1-tap return.
 * - When scrolling down, smoothly morphs between expanded layout and inline 48dp layout.
 * - The standalone search pill visibly grows and shrinks (64dp <-> 48dp).
 * - The active tab icon and tab group glide and morph into the inline circle.
 * - The mini player slides down from the top dock into the inline middle slot.
 */
@Composable
fun LiquidBottomNavBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    isSearchInputActive: Boolean = false,
    onSearchInputActiveChange: (Boolean) -> Unit = {},
    playerState: PlayerState? = null,
    isPlaying: Boolean = false,
    onTogglePlayPause: () -> Unit = {},
    onMiniPlayerClick: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    playbackProgress: () -> Float = { 0f },
    onSeek: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    tintColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    collapseProgress: Float = 0f,
    scrollConnection: FloatingTabBarScrollConnection = rememberFloatingTabBarScrollConnection(),
    pureBlack: Boolean = false,
    tabPosition: (() -> Float?)? = null
) {
    val isDarkMode by com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState()
    val backdrop = LocalBackdrop.current

    val activeAccentColor = Color(0xFFFA243C)
    val tabTextColor = if (isDarkMode) Color.White else Color.Black

    val actualTintColor = if (tintColor != Color.Unspecified) tintColor
    else if (!isDarkMode) Color(0xFFFAFAFA).copy(alpha = 0.55f) else Color(0xFF161618).copy(alpha = 0.55f)

    val actualContentColor = if (contentColor != Color.Unspecified) contentColor
    else tabTextColor

    val glassStyle = com.mrtdk.glass.LocalGlassStyle.current
    val isSolid = glassStyle == "solid"
    val solidBgColor = if (isDarkMode) Color(0xFF242428) else Color(0xFFE8E8EC)

    var lastActiveMainTab by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in 0..3) {
            lastActiveMainTab = selectedIndex
        }
    }

    val isSearchActive = selectedIndex == 4 || isSearchInputActive
    val visualState = when {
        scrollConnection.isInline -> LiquidNavVisualState.INLINE
        isSearchActive -> LiquidNavVisualState.SEARCH_EXPANDED
        else -> LiquidNavVisualState.EXPANDED
    }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0


    // System back handler while in search mode
    BackHandler(enabled = visualState == LiquidNavVisualState.SEARCH_EXPANDED) {
        if (isKeyboardOpen) {
            keyboardController?.hide()
            focusManager.clearFocus()
        } else {
            onTabSelected(lastActiveMainTab)
        }
    }

    val capsuleBackdropModifier = remember(isSolid, backdrop, solidBgColor, actualTintColor) {
        if (isSolid) {
            Modifier
                .clip(Capsule())
                .background(solidBgColor)
        } else {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(24.dp.toPx(), 24.dp.toPx())
                },
                highlight = { Highlight.Default.copy(alpha = 0.35f) },
                shadow = { Shadow.Default },
                onDrawSurface = { drawRect(actualTintColor) }
            )
        }
    }

    val miniPlayerBackdropModifier = remember(isSolid, backdrop, solidBgColor, actualTintColor) {
        if (isSolid) {
            Modifier
                .clip(MiniPlayerShape)
                .background(solidBgColor)
        } else {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { MiniPlayerShape },
                effects = {
                    vibrancy()
                    blur(6.dp.toPx())
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        lens(
                            refractionHeight = 16.dp.toPx(),
                            refractionAmount = 24.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = false
                        )
                    }
                },
                highlight = { Highlight.Default.copy(alpha = 0.25f) },
                shadow = { Shadow.Default },
                onDrawSurface = { drawRect(actualTintColor) }
            )
        }
    }

    SharedTransitionLayout(modifier = modifier.fillMaxWidth()) {
        val navBoundsTransform = remember {
            BoundsTransform { _, _ ->
                spring(
                    dampingRatio = 0.88f,
                    stiffness = 175f
                )
            }
        }

        AnimatedContent(
            targetState = visualState,
            transitionSpec = {
                fadeIn(tween(360, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(tween(240, easing = FastOutSlowInEasing))
            },
            contentAlignment = Alignment.BottomCenter,
            label = "navBarSharedMorphTransition"
        ) { targetVisual ->
            when (targetVisual) {
                LiquidNavVisualState.INLINE -> {
                    // ── INLINE ROW (Collapsed when scrolling down - Convx layout) ──────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Auto },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Active Tab Pill / Circle (sharedElement morph with tabGroup)
                        val currentTab = if (selectedIndex in 0..3) MainNavTabs[selectedIndex] else MainNavTabs.getOrElse(lastActiveMainTab.coerceIn(0, 3)) { MainNavTabs[0] }

                        Box(
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState("tabGroup"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = navBoundsTransform,
                                    zIndexInOverlay = 1f
                                )
                                .skipToLookaheadSize()
                                .size(48.dp)
                                .then(capsuleBackdropModifier)
                                .clip(Capsule())
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = {
                                        if (selectedIndex == 4) {
                                            onTabSelected(currentTab.index)
                                        } else {
                                            scrollConnection.expand()
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentTab.iconRes != null) {
                                Box(
                                    modifier = Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState("tab#${currentTab.index}-icon"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = navBoundsTransform,
                                        zIndexInOverlay = 2f
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(currentTab.iconRes),
                                        contentDescription = stringResource(currentTab.titleRes),
                                        tint = if (selectedIndex in 0..3) activeAccentColor else actualContentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // 2. Inline MiniPlayer Accessory (sharedElement morph with accessory)
                        if (playerState != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState("accessory"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = navBoundsTransform,
                                        zIndexInOverlay = 1f
                                    )
                                    .skipToLookaheadSize()
                            ) {
                                FloatingMiniPlayer(
                                    isInline = true,
                                    playerState = playerState,
                                    isPlaying = isPlaying,
                                    onTogglePlayPause = onTogglePlayPause,
                                    onClick = onMiniPlayerClick,
                                    onNext = onNext,
                                    onPrevious = onPrevious,
                                    contentColor = actualContentColor,
                                    playbackProgress = playbackProgress,
                                    onSeek = onSeek,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(miniPlayerBackdropModifier)
                                )
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        // 3. Standalone Search Circle (sharedElement morph with standaloneTab - 48dp)
                        val isSearchSelected = selectedIndex == 4
                        val searchColor = if (isSearchSelected) activeAccentColor else actualContentColor.copy(alpha = 0.65f)

                        Box(
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState("standaloneTab"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = navBoundsTransform,
                                    zIndexInOverlay = 1f
                                )
                                .skipToLookaheadSize()
                                .size(48.dp)
                                .then(capsuleBackdropModifier)
                                .clip(Capsule())
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = {
                                        onTabSelected(4)
                                        scrollConnection.expand()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState("searchIcon"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = navBoundsTransform,
                                    zIndexInOverlay = 2f
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.nav_search),
                                    contentDescription = stringResource(R.string.search_action),
                                    tint = searchColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                LiquidNavVisualState.EXPANDED -> {
                    // ── EXPANDED COLUMN (Normal state - Convx layout) ─────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Auto },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Floating MiniPlayer accessory (docked above - sharedElement with accessory)
                        if (playerState != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState("accessory"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = navBoundsTransform,
                                        zIndexInOverlay = 1f
                                    )
                                    .skipToLookaheadSize()
                            ) {
                                FloatingMiniPlayer(
                                    isInline = false,
                                    playerState = playerState,
                                    isPlaying = isPlaying,
                                    onTogglePlayPause = onTogglePlayPause,
                                    onClick = onMiniPlayerClick,
                                    onNext = onNext,
                                    onPrevious = onPrevious,
                                    contentColor = actualContentColor,
                                    playbackProgress = playbackProgress,
                                    onSeek = onSeek,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(miniPlayerBackdropModifier)
                                )
                            }
                        }

                        // Navigation Row: [ Home, New, Radio, Library Pill ] + [ Standalone Search Pill ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pill 1: Liquid Bottom Tabs (sharedElement with tabGroup)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState("tabGroup"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = navBoundsTransform,
                                        zIndexInOverlay = 1f
                                    )
                                    .skipToLookaheadSize()
                            ) {
                                LiquidBottomTabs(
                                    selectedTabIndex = { if (selectedIndex in 0..3) selectedIndex else lastActiveMainTab },
                                    onTabSelected = onTabSelected,
                                    backdrop = backdrop,
                                    tabsCount = MainNavTabs.size,
                                    accentColor = activeAccentColor,
                                    containerColor = if (isSolid) solidBgColor else actualTintColor,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MainNavTabs.forEach { tabItem ->
                                        val isSelected = tabItem.index == selectedIndex
                                        val isSharedIcon = tabItem.index == lastActiveMainTab || isSelected
                                        val baseColor = if (isSelected) activeAccentColor else actualContentColor.copy(alpha = 0.65f)

                                        LiquidBottomTab(
                                            onClick = { onTabSelected(tabItem.index) }
                                        ) {
                                            if (tabItem.iconRes != null) {
                                                Box(
                                                    modifier = if (isSharedIcon) {
                                                        Modifier.sharedElement(
                                                            sharedContentState = rememberSharedContentState("tab#${tabItem.index}-icon"),
                                                            animatedVisibilityScope = this@AnimatedContent,
                                                            boundsTransform = navBoundsTransform,
                                                            zIndexInOverlay = 2f
                                                        )
                                                    } else Modifier
                                                ) {
                                                    Icon(
                                                        painter = painterResource(tabItem.iconRes),
                                                        contentDescription = stringResource(tabItem.titleRes),
                                                        tint = baseColor,
                                                        modifier = Modifier
                                                            .padding(top = 2.dp)
                                                            .size(28.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = stringResource(tabItem.titleRes),
                                                color = baseColor,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // Pill 2: Standalone Search Pill (sharedElement with standaloneTab - 64dp)
                            val isSearchSelected = selectedIndex == 4
                            val searchColor = if (isSearchSelected) activeAccentColor else actualContentColor.copy(alpha = 0.65f)

                            Box(
                                modifier = Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState("standaloneTab"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = navBoundsTransform,
                                        zIndexInOverlay = 1f
                                    )
                                    .skipToLookaheadSize()
                                    .size(64.dp)
                                    .then(capsuleBackdropModifier)
                                    .clip(Capsule())
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        role = Role.Tab,
                                        onClick = { onTabSelected(4) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState("searchIcon"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = navBoundsTransform,
                                        zIndexInOverlay = 2f
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.nav_search),
                                        contentDescription = stringResource(R.string.search_action),
                                        tint = searchColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                LiquidNavVisualState.SEARCH_EXPANDED -> {
                    // ── SEARCH EXPANDED ROW (Convx-style expanded search bar + Home pill) ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Auto },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Floating MiniPlayer accessory (docked above - hidden if keyboard is open to save screen space)
                        if (playerState != null && !isKeyboardOpen) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState("accessory"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = navBoundsTransform,
                                        zIndexInOverlay = 1f
                                    )
                                    .skipToLookaheadSize()
                            ) {
                                FloatingMiniPlayer(
                                    isInline = false,
                                    playerState = playerState,
                                    isPlaying = isPlaying,
                                    onTogglePlayPause = onTogglePlayPause,
                                    onClick = onMiniPlayerClick,
                                    onNext = onNext,
                                    onPrevious = onPrevious,
                                    contentColor = actualContentColor,
                                    playbackProgress = playbackProgress,
                                    onSeek = onSeek,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(miniPlayerBackdropModifier)
                                )
                            }
                        }

                        // Search Row: [ Companion Pill (48dp) ] + [ Expanded Search Bar with Direct Writing (48dp) ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val previousTab = MainNavTabs.getOrElse(lastActiveMainTab.coerceIn(0, 3)) { MainNavTabs[0] }

                            // 1. Companion Pill (sharedElement morph with tabGroup - 48dp)
                            Box(
                                modifier = Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState("tabGroup"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = navBoundsTransform,
                                        zIndexInOverlay = 1f
                                    )
                                    .skipToLookaheadSize()
                                    .size(48.dp)
                                    .then(capsuleBackdropModifier)
                                    .clip(Capsule())
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        role = Role.Tab,
                                        onClick = { onTabSelected(previousTab.index) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState("tab#${previousTab.index}-icon"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = navBoundsTransform,
                                        zIndexInOverlay = 2f
                                    )
                                ) {
                                    if (previousTab.iconRes != null) {
                                        Icon(
                                            painter = painterResource(previousTab.iconRes),
                                            contentDescription = stringResource(previousTab.titleRes),
                                            tint = actualContentColor.copy(alpha = 0.85f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // 2. Expanded Search Bar (sharedElement morph with standaloneTab - weight(1f), 48dp height)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState("standaloneTab"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = navBoundsTransform,
                                        zIndexInOverlay = 1f
                                    )
                                    .skipToLookaheadSize()
                                    .then(capsuleBackdropModifier)
                                    .clip(Capsule())
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            try {
                                                focusRequester.requestFocus()
                                                keyboardController?.show()
                                            } catch (_: Exception) {}
                                        }
                                    ),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Morphing Search Icon (shares searchIcon tag)
                                    Box(
                                        modifier = Modifier.sharedElement(
                                            sharedContentState = rememberSharedContentState("searchIcon"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            boundsTransform = navBoundsTransform,
                                            zIndexInOverlay = 2f
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.nav_search),
                                            contentDescription = stringResource(R.string.search_action),
                                            tint = activeAccentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Direct Text Input Field
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.search_placeholder),
                                                color = actualContentColor.copy(alpha = 0.45f),
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = onSearchQueryChange,
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = actualContentColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            cursorBrush = SolidColor(activeAccentColor),
                                            keyboardOptions = KeyboardOptions(
                                                imeAction = ImeAction.Search
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onSearch = { onSearchSubmit(searchQuery) }
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(focusRequester)
                                                .onFocusChanged { focusState ->
                                                    onSearchInputActiveChange(focusState.isFocused)
                                                }
                                        )
                                    }

                                    // Clear query button ("X")
                                    if (searchQuery.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(Capsule())
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = { onSearchQueryChange("") }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.close_action),
                                                tint = actualContentColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}