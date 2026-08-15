package com.mrtdk.liquid_glass.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.ui.components.LiquidBottomTab
import com.mrtdk.liquid_glass.ui.components.LiquidBottomTabs
import com.mrtdk.liquid_glass.ui.components.LocalBackdrop

val tabs = listOf(
    R.string.nav_inicio to R.drawable.nav_inicio,
    R.string.nav_novedades to R.drawable.nav_novedades,
    R.string.nav_radio to R.drawable.nav_radio,
    R.string.nav_biblioteca to R.drawable.nav_biblioteca
)

@Composable
fun LiquidBottomNavBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    tintColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    collapseProgress: Float = 0f
) {
    val isDarkMode by com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState()
    val actualTintColor = if (tintColor != Color.Unspecified) tintColor
                          else if (!isDarkMode) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)
    val actualContentColor = if (contentColor != Color.Unspecified) contentColor
                             else if (!isDarkMode) Color.Black else Color.White
    val imeBottom = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
    val isKeyboardOpen = imeBottom > 0
    val focusManager = LocalFocusManager.current

    val isCollapsing = collapseProgress > 0.001f && collapseProgress < 0.999f
    val isSearchActive = selectedIndex == 4
    
    val navSpringSpec = spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = 300f
    )
    val navDpSpringSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = 300f
    )

    val searchProgressState = animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = navSpringSpec
    )
    
    val xWidthState = animateDpAsState(
        targetValue = if (isSearchActive && isKeyboardOpen) 64.dp else 0.dp,
        animationSpec = navDpSpringSpec
    )
    
    val spacingSearchXState = animateDpAsState(
        targetValue = if (isSearchActive && isKeyboardOpen) 12.dp else 0.dp,
        animationSpec = navDpSpringSpec
    )

    val collapseProgressState = rememberUpdatedState(collapseProgress)
    val collapseProgressProvider = remember { { collapseProgressState.value } }
    val searchProgressProvider = remember { { searchProgressState.value } }

    val isAnimatingState = remember(isSearchActive) {
        derivedStateOf {
            val progress = searchProgressState.value
            val transitioning = progress > 0.001f && progress < 0.999f
            val collapsing = collapseProgressState.value > 0.001f && collapseProgressState.value < 0.999f
            transitioning || collapsing
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    val backdrop = LocalBackdrop.current
    val navBarHeight = 84.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(navBarHeight)
    ) {
        val parentWidth = maxWidth
        val mainTabsMaxWidth = parentWidth - 56.dp - 12.dp

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabWidthDp = (mainTabsMaxWidth - 8.dp) / 4
            val homeTabCenterX = 4.dp + tabWidthDp / 2
            val targetCenterX = 28.dp

            // Main Navigation Pill (shrinks smoothly to become circular Home button)
            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val progress = if (searchProgressState.value > collapseProgressState.value) searchProgressState.value else collapseProgressState.value
                        val currentAvailableWidth = parentWidth - xWidthState.value - spacingSearchXState.value - 12.dp
                        val widthDp = currentAvailableWidth - 56.dp - (currentAvailableWidth - 112.dp) * progress
                        val widthPx = widthDp.roundToPx()
                        val heightPx = (84.dp - 28.dp * progress).roundToPx()
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = widthPx, maxWidth = widthPx,
                                minHeight = heightPx, maxHeight = heightPx
                            )
                        )
                        layout(widthPx, heightPx) {
                            placeable.place(0, 0)
                        }
                    }
                    .let {
                        if (searchProgressState.value > 0.001f || collapseProgressState.value > 0.001f) it.clip(Capsule()) else it
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .requiredWidth(mainTabsMaxWidth)
                        .graphicsLayer {
                            clip = false
                            val progress = if (searchProgressState.value > collapseProgressState.value) searchProgressState.value else collapseProgressState.value
                            val currentAvailableWidth = parentWidth - xWidthState.value - spacingSearchXState.value - 12.dp
                            val widthDp = currentAvailableWidth - 56.dp - (currentAvailableWidth - 112.dp) * progress
                            val offsetDp = (targetCenterX - homeTabCenterX) * progress - (widthDp - mainTabsMaxWidth) / 2
                            translationX = offsetDp.toPx()
                        }
                ) {
                    MainTabs(
                        selectedIndex = selectedIndex,
                        onTabSelected = onTabSelected,
                        contentColor = actualContentColor,
                        tintColor = actualTintColor,
                        searchProgress = searchProgressProvider,
                        collapseProgress = collapseProgressProvider
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Search Pill (expands smoothly to become the search bar)
            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val progress = searchProgressState.value
                        val currentAvailableWidth = parentWidth - xWidthState.value - spacingSearchXState.value - 12.dp
                        val widthDp = 56.dp + (currentAvailableWidth - 112.dp) * progress * (1f - collapseProgressState.value)
                        val widthPx = widthDp.roundToPx()
                        val heightPx = (56.dp + 8.dp * progress * (1f - collapseProgressState.value)).roundToPx()
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = widthPx, maxWidth = widthPx,
                                minHeight = heightPx, maxHeight = heightPx
                            )
                        )
                        layout(widthPx, heightPx) {
                            placeable.place(0, 0)
                        }
                    }
                    .clip(Capsule())
                    .clipToBounds()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            if (!isAnimatingState.value) {
                                lens(24f.dp.toPx(), 24f.dp.toPx())
                            }
                        },
                        onDrawSurface = { drawRect(actualTintColor) }
                    )
            ) {
                val isSearchEnabled by remember {
                    derivedStateOf {
                        val progress = searchProgressState.value * (1f - collapseProgressState.value)
                        progress > 0.5f
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val progress = searchProgressState.value * (1f - collapseProgressState.value)
                            val offsetDp = -4.dp * (1f - progress)
                            translationX = offsetDp.toPx()
                        }
                        .clickable { 
                            if (collapseProgressState.value > 0.5f || !isSearchActive) {
                                onTabSelected(4)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_action),
                        tint = actualContentColor,
                        modifier = Modifier
                            .padding(start = 20.dp, end = 12.dp)
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { 
                                if (collapseProgressState.value > 0.5f || !isSearchActive) {
                                    onTabSelected(4)
                                }
                            }
                    )
                    
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { 
                                val progress = searchProgressState.value * (1f - collapseProgressState.value)
                                val textAlpha = progress.coerceIn(0f, 1f)
                                alpha = textAlpha
                                translationX = if (textAlpha < 0.01f) 10000f else 0f
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .padding(end = 8.dp),
                            textStyle = TextStyle(color = actualContentColor, fontSize = 16.sp),
                            cursorBrush = SolidColor(actualContentColor),
                            singleLine = true,
                            enabled = isSearchEnabled,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit(searchQuery) }),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(stringResource(R.string.search_placeholder), color = actualContentColor.copy(alpha = 0.5f), fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(22.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onSearchQueryChange("")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = contentColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Close Pill (X)
            val showClosePill by remember {
                derivedStateOf {
                    xWidthState.value > 0.5.dp
                }
            }
            if (showClosePill) {
                Spacer(
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val widthPx = spacingSearchXState.value.roundToPx()
                            val placeable = measurable.measure(constraints.copy(minWidth = widthPx, maxWidth = widthPx))
                            layout(widthPx, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val progress = searchProgressState.value
                            val widthPx = xWidthState.value.roundToPx()
                            val heightPx = (56.dp + 8.dp * progress * (1f - collapseProgressState.value)).roundToPx()
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = widthPx, maxWidth = widthPx,
                                    minHeight = heightPx, maxHeight = heightPx
                                )
                            )
                            layout(widthPx, heightPx) {
                                placeable.place(0, 0)
                            }
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                                if (!isAnimatingState.value) {
                                    lens(24f.dp.toPx(), 24f.dp.toPx())
                                }
                            },
                            onDrawSurface = { drawRect(actualTintColor) }
                        )
                        .clip(Capsule())
                        .clickable { 
                            if (searchQuery.isNotEmpty()) {
                                onSearchQueryChange("")
                            } else {
                                focusManager.clearFocus()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_action),
                        tint = actualContentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MainTabs(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    contentColor: Color = Color.White,
    tintColor: Color = Color.White.copy(alpha = 0.15f),
    searchProgress: () -> Float = { 0f },
    collapseProgress: () -> Float = { 0f }
) {
    val backdrop = LocalBackdrop.current
    val tabsCount = tabs.size

    val combineProgress = remember {
        {
            val s = searchProgress()
            val c = collapseProgress()
            if (s > c) s else c
        }
    }

    LiquidBottomTabs(
        selectedTabIndex = { selectedIndex },
        onTabSelected = onTabSelected,
        backdrop = backdrop,
        tabsCount = tabsCount,
        containerColor = tintColor,
        accentColor = Color(0xFFFA243C),
        searchProgress = combineProgress(),
        collapseProgress = collapseProgress(),
        modifier = Modifier.fillMaxSize()
    ) {
        tabs.forEachIndexed { index, pair ->
            val tabText = stringResource(id = pair.first)
            
            val isDarkMode by com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState()
            val isSelected = selectedIndex == index
            val baseColor = if (isDarkMode) {
                if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
            } else {
                if (isSelected) Color(0xFFFA243C) else Color.Black.copy(alpha = 0.6f)
            }
            val iconSize = 26.dp
            val tabWeight = 1f

            LiquidBottomTab(
                onClick = { onTabSelected(index) },
                weight = tabWeight
            ) {
                Icon(
                    painter = painterResource(id = pair.second),
                    contentDescription = tabText,
                    tint = baseColor,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            translationY = androidx.compose.ui.unit.lerp(5.dp, 7.dp, combineProgress()).toPx()
                            alpha = if (index == 0) 1f else (1f - combineProgress())
                            val color = if (index == 0) {
                                androidx.compose.ui.graphics.lerp(
                                    baseColor,
                                    Color(0xFFFA243C),
                                    combineProgress()
                                )
                            } else {
                                baseColor
                            }
                            colorFilter = ColorFilter.tint(color)
                        }
                )
                Text(
                    text = tabText,
                    color = baseColor,
                    fontSize = 10.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier
                        .offset(y = (-3).dp)
                        .graphicsLayer {
                            alpha = 1f - combineProgress()
                        }
                )
            }
        }
    }
}