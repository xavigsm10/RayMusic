package com.mrtdk.liquid_glass.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
    "Inicio" to R.drawable.nav_inicio,
    "Novedades" to R.drawable.nav_novedades,
    "Radio" to R.drawable.nav_radio,
    "Biblioteca" to R.drawable.nav_biblioteca
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
    contentColor: Color = Color.White
) {
    val imeBottom = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
    val isKeyboardOpen = imeBottom > 0

    val isSearchActive = selectedIndex == 4
    val mainWeight by animateFloatAsState(if (isSearchActive) 0.0001f else 1f, animationSpec = tween(400))
    val searchWeight by animateFloatAsState(if (isSearchActive) 1f else 0.0001f, animationSpec = tween(400))
    
    val searchWidth by animateDpAsState(if (!isSearchActive) 56.dp else 0.dp, animationSpec = tween(400))
    val homeWidth by animateDpAsState(if (isSearchActive && !isKeyboardOpen) 56.dp else 0.dp, animationSpec = tween(400))
    val xWidth by animateDpAsState(if (isSearchActive && isKeyboardOpen) 56.dp else 0.dp, animationSpec = tween(400))

    val spacingMainSearch by animateDpAsState(if (!isSearchActive && mainWeight > 0.01f) 12.dp else 0.dp, animationSpec = tween(400))
    val spacingHomeSearch by animateDpAsState(if (isSearchActive && !isKeyboardOpen) 12.dp else 0.dp, animationSpec = tween(400))
    val spacingSearchX by animateDpAsState(if (isSearchActive && isKeyboardOpen) 12.dp else 0.dp, animationSpec = tween(400))

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    val backdrop = LocalBackdrop.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Pill
        if (mainWeight > 0.01f) {
            Box(
                modifier = Modifier
                    .weight(mainWeight)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                MainTabs(selectedIndex, onTabSelected, contentColor, tintColor)
            }
            Spacer(modifier = Modifier.width(spacingMainSearch))
        }

        // Home Pill
        if (homeWidth > 0.5.dp) {
            Box(
                modifier = Modifier
                    .width(homeWidth)
                    .height(56.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(24f.dp.toPx(), 24f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(tintColor) }
                    )
                    .clip(Capsule())
                    .clickable { onTabSelected(0) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.nav_inicio),
                    contentDescription = stringResource(R.string.nav_inicio),
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(spacingHomeSearch))
        }

        // Search Pill
        val searchModifier = if (isSearchActive) Modifier.weight(searchWeight) else Modifier.width(searchWidth)
        Box(
            modifier = searchModifier
                .height(56.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(tintColor) }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { if (!isSearchActive) onTabSelected(4) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (!isSearchActive) Arrangement.Center else Arrangement.Start
            ) {
                if (!isSearchActive) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_action),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_action),
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = 20.dp, end = 12.dp)
                            .size(26.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(4) }
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .padding(end = 16.dp),
                        textStyle = TextStyle(color = contentColor, fontSize = 16.sp),
                        cursorBrush = SolidColor(contentColor),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchSubmit(searchQuery) }),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(stringResource(R.string.search_placeholder), color = contentColor.copy(alpha = 0.5f), fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }

        // Close Pill (X)
        if (xWidth > 0.5.dp) {
            Spacer(modifier = Modifier.width(spacingSearchX))
            Box(
                modifier = Modifier
                    .width(xWidth)
                    .height(56.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(24f.dp.toPx(), 24f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(tintColor) }
                    )
                    .clip(Capsule())
                    .clickable { 
                        if (searchQuery.isNotEmpty()) {
                            onSearchQueryChange("")
                        } else {
                            onTabSelected(0)
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

@Composable
fun MainTabs(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    contentColor: Color = Color.White,
    tintColor: Color = Color.White.copy(alpha = 0.15f)
) {
    val backdrop = LocalBackdrop.current
    val tabsCount = tabs.size

    LiquidBottomTabs(
        selectedTabIndex = { selectedIndex },
        onTabSelected = onTabSelected,
        backdrop = backdrop,
        tabsCount = tabsCount,
        containerColor = tintColor,
        accentColor = Color(0xFFFA243C),
        modifier = Modifier.fillMaxSize()
    ) {
        tabs.forEachIndexed { index, pair ->
            val tabText = when (index) {
                0 -> "Home"
                1 -> "New"
                2 -> "Radio"
                3 -> "Library"
                else -> pair.first
            }
            
            val isSelected = selectedIndex == index
            val itemColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
            val iconSize = when (index) {
                2, 3 -> 29.dp
                else -> 26.dp
            }

            LiquidBottomTab(
                onClick = { onTabSelected(index) }
            ) {
                Icon(
                    painter = painterResource(id = pair.second),
                    contentDescription = tabText,
                    tint = itemColor,
                    modifier = Modifier.size(iconSize)
                )
                Text(
                    text = tabText,
                    color = itemColor,
                    fontSize = 10.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}