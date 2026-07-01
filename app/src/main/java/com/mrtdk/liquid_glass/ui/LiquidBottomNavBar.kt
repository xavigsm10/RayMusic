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
    tintColor: Color = Color.White.copy(alpha = 0.15f),
    contentColor: Color = Color.White,
    collapseProgress: Float = 0f
) {
    val imeBottom = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
    val isKeyboardOpen = imeBottom > 0
    val focusManager = LocalFocusManager.current

    val isCollapsing = collapseProgress > 0.001f && collapseProgress < 0.999f
    val isSearchActive = selectedIndex == 4
    
    val navSpringSpec = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 300f
    )
    val navDpSpringSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.85f,
        stiffness = 300f
    )

    val searchProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = navSpringSpec
    )
    
    val isTransitioning = (searchProgress > 0.001f && searchProgress < 0.999f) || isCollapsing
    
    val xWidth by animateDpAsState(
        targetValue = if (isSearchActive && isKeyboardOpen) 64.dp else 0.dp,
        animationSpec = navDpSpringSpec
    )
    
    val spacingSearchX by animateDpAsState(
        targetValue = if (isSearchActive && isKeyboardOpen) 12.dp else 0.dp,
        animationSpec = navDpSpringSpec
    )

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    val backdrop = LocalBackdrop.current
    val combineProgress = if (searchProgress > collapseProgress) searchProgress else collapseProgress
    val navBarHeight = 84.dp - 20.dp * combineProgress

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
            // Main Navigation Pill (shrinks smoothly to become circular Home button)
            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val progress = if (searchProgress > collapseProgress) searchProgress else collapseProgress
                        val currentAvailableWidth = parentWidth - xWidth - spacingSearchX - 12.dp
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
                    .let { mod ->
                        if (isSearchActive || collapseProgress > 0f || searchProgress > 0.001f) {
                            mod
                                .clip(Capsule())
                                .clipToBounds()
                                .drawWithContent {
                                    drawContent()
                                    val strokeWidth = 1.dp.toPx()
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.15f),
                                        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f),
                                        size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                    )
                                }
                        } else {
                            mod
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.requiredWidth(mainTabsMaxWidth)
                ) {
                    MainTabs(selectedIndex, onTabSelected, contentColor, tintColor, searchProgress, collapseProgress)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Search Pill (expands smoothly to become the search bar)
            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val progress = searchProgress
                        val currentAvailableWidth = parentWidth - xWidth - spacingSearchX - 12.dp
                        val widthDp = 56.dp + (currentAvailableWidth - 112.dp) * progress * (1f - collapseProgress)
                        val widthPx = widthDp.roundToPx()
                        val heightPx = (56.dp + 8.dp * progress * (1f - collapseProgress)).roundToPx()
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
                    .then(
                        if (isTransitioning) {
                            Modifier.background(tintColor)
                        } else {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { Capsule() },
                                effects = {
                                    vibrancy()
                                    blur(8f.dp.toPx())
                                    lens(24f.dp.toPx(), 24f.dp.toPx())
                                },
                                onDrawSurface = { drawRect(tintColor) }
                            )
                        }
                    )
            ) {
                val showSearchInput = isSearchActive && collapseProgress < 0.5f
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { if (!isSearchActive) onTabSelected(4) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (!showSearchInput) Arrangement.Center else Arrangement.Start
                ) {
                    if (!showSearchInput) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_action),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_action),
                            tint = Color.White,
                            modifier = Modifier
                                .padding(start = 20.dp, end = 12.dp)
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onTabSelected(4) }
                        )
                        val textAlpha = (1f - collapseProgress * 2f).coerceIn(0f, 1f)
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .padding(end = 8.dp)
                                .graphicsLayer { alpha = textAlpha },
                            textStyle = TextStyle(color = contentColor, fontSize = 16.sp),
                            cursorBrush = SolidColor(contentColor),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit(searchQuery) }),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(stringResource(R.string.search_placeholder), color = contentColor.copy(alpha = 0.5f * textAlpha), fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty() && textAlpha > 0.1f) {
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
                                    }
                                    .graphicsLayer { alpha = textAlpha },
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
            if (xWidth > 0.5.dp) {
                Spacer(modifier = Modifier.width(spacingSearchX))
                Box(
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val progress = searchProgress
                            val widthPx = xWidth.roundToPx()
                            val heightPx = (56.dp + 8.dp * progress * (1f - collapseProgress)).roundToPx()
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
                        .then(
                            if (isTransitioning) {
                                Modifier.background(tintColor)
                            } else {
                                Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { Capsule() },
                                    effects = {
                                        vibrancy()
                                        blur(8f.dp.toPx())
                                        lens(24f.dp.toPx(), 24f.dp.toPx())
                                    },
                                    onDrawSurface = { drawRect(tintColor) }
                                )
                            }
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
                        tint = contentColor,
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
    searchProgress: Float = 0f,
    collapseProgress: Float = 0f
) {
    val backdrop = LocalBackdrop.current
    val tabsCount = tabs.size

    val combineProgress = if (searchProgress > collapseProgress) searchProgress else collapseProgress

    LiquidBottomTabs(
        selectedTabIndex = { selectedIndex },
        onTabSelected = onTabSelected,
        backdrop = backdrop,
        tabsCount = tabsCount,
        containerColor = tintColor,
        accentColor = Color(0xFFFA243C),
        searchProgress = combineProgress,
        collapseProgress = collapseProgress,
        modifier = Modifier.fillMaxSize()
    ) {
        tabs.forEachIndexed { index, pair ->
            val tabText = stringResource(id = pair.first)
            
            val isSelected = selectedIndex == index
            val itemColor = if (index == 0) {
                androidx.compose.ui.graphics.lerp(
                    if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                    Color(0xFFFA243C),
                    combineProgress
                )
            } else {
                if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
            }
            val iconSize = 26.dp

            val tabWeight = if (index == 0) 1f else (1f - combineProgress).coerceAtLeast(0.0001f)
            val tabAlpha = if (index == 0) 1f else (1f - combineProgress)
            val textAlpha = 1f - combineProgress

            LiquidBottomTab(
                onClick = { onTabSelected(index) },
                weight = tabWeight
            ) {
                val iconOffsetY = 5.dp * (1f - combineProgress)
                Icon(
                    painter = painterResource(id = pair.second),
                    contentDescription = tabText,
                    tint = itemColor,
                    modifier = Modifier
                        .size(iconSize)
                        .offset(y = iconOffsetY)
                        .graphicsLayer { alpha = tabAlpha }
                )
                if (textAlpha > 0.05f) {
                    Text(
                        text = tabText,
                        color = itemColor,
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier
                            .offset(y = (-3).dp)
                            .graphicsLayer { alpha = textAlpha }
                    )
                }
            }
        }
    }
}