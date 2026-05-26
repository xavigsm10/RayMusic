package com.mrtdk.liquid_glass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.res.painterResource
import com.mrtdk.liquid_glass.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrtdk.glass.GlassBox
import com.mrtdk.glass.GlassBoxScope
import com.mrtdk.liquid_glass.utils.DampedDragAnimation
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

val tabs = listOf(
    "Inicio" to R.drawable.nav_inicio,
    "Novedades" to R.drawable.nav_novedades,
    "Radio" to R.drawable.nav_radio,
    "Biblioteca" to R.drawable.nav_biblioteca
)

@Composable
fun LiquidBottomNavBar(
    glassScope: GlassBoxScope,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    tintColor: Color = Color.White.copy(alpha = 0.15f),
    contentColor: Color = Color.White
) {
    val imeBottom = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
    val isKeyboardOpen = imeBottom > 0

    val isSearchActive = selectedIndex == 4
    val mainWeight by animateFloatAsState(if (isSearchActive) 0.0001f else 1f, animationSpec = tween(400))
    val searchWeight by animateFloatAsState(if (isSearchActive) 1f else 0.0001f, animationSpec = tween(400))
    
    val searchWidth by androidx.compose.animation.core.animateDpAsState(if (!isSearchActive) 56.dp else 0.dp, animationSpec = tween(400))
    val homeWidth by androidx.compose.animation.core.animateDpAsState(if (isSearchActive && !isKeyboardOpen) 56.dp else 0.dp, animationSpec = tween(400))
    val xWidth by androidx.compose.animation.core.animateDpAsState(if (isSearchActive && isKeyboardOpen) 56.dp else 0.dp, animationSpec = tween(400))

    val spacingMainSearch by androidx.compose.animation.core.animateDpAsState(if (!isSearchActive && mainWeight > 0.01f) 12.dp else 0.dp, animationSpec = tween(400))
    val spacingHomeSearch by androidx.compose.animation.core.animateDpAsState(if (isSearchActive && !isKeyboardOpen) 12.dp else 0.dp, animationSpec = tween(400))
    val spacingSearchX by androidx.compose.animation.core.animateDpAsState(if (isSearchActive && isKeyboardOpen) 12.dp else 0.dp, animationSpec = tween(400))

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Pill
        if (mainWeight > 0.01f) {
            glassScope.GlassBox(
                modifier = Modifier
                    .weight(mainWeight)
                    .fillMaxHeight(),
                shape = CircleShape,
                blur = 0.8f,
                centerDistortion = 0.2f,
                scale = 0.02f,
                warpEdges = 0.6f,
                tint = tintColor,
                elevation = 8.dp
            ) {
                MainTabs(glassScope, selectedIndex, onTabSelected, contentColor, tintColor)
            }
            Spacer(modifier = Modifier.width(spacingMainSearch))
        }

        // Home Pill
        if (homeWidth > 0.5.dp) {
            glassScope.GlassBox(
                modifier = Modifier
                    .width(homeWidth)
                    .height(56.dp),
                shape = CircleShape,
                blur = 0.8f,
                centerDistortion = 0.2f,
                scale = 0.02f,
                warpEdges = 0.6f,
                tint = tintColor,
                elevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().clickable { onTabSelected(0) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.mrtdk.liquid_glass.R.drawable.nav_inicio),
                        contentDescription = stringResource(R.string.nav_inicio),
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(spacingHomeSearch))
        }

        // Search Pill
        val searchModifier = if (isSearchActive) Modifier.weight(searchWeight) else Modifier.width(searchWidth)
        glassScope.GlassBox(
            modifier = searchModifier.height(56.dp),
            shape = CircleShape,
            blur = 0.8f,
            centerDistortion = 0.2f,
            scale = 0.02f,
            warpEdges = 0.6f,
            tint = Color.White.copy(alpha = 0.15f),
            elevation = 8.dp
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
                        tint = contentColor,
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
            glassScope.GlassBox(
                modifier = Modifier
                    .width(xWidth)
                    .height(56.dp),
                shape = CircleShape,
                blur = 0.8f,
                centerDistortion = 0.2f,
                scale = 0.02f,
                warpEdges = 0.6f,
                tint = Color.White.copy(alpha = 0.15f),
                elevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().clickable { 
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
}

@Composable
fun MainTabs(
    glassScope: GlassBoxScope,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    contentColor: Color = Color.White,
    tintColor: Color = Color.White.copy(alpha = 0.15f)
) {
    val animationScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val tabsCount = tabs.size

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        val tabWidth = with(density) { constraints.maxWidth.toFloat() / tabsCount }
        
        var initialDragPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        var hasDragged by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }

        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.15f,
                onDragStarted = { position ->
                    initialDragPosition = position
                    hasDragged = false
                    isDragging = true
                },
                onDragStopped = {
                    isDragging = false
                    if (!hasDragged) {
                        // It was a tap — immediately switch to tapped tab
                        val tappedIndex = (initialDragPosition.x / tabWidth).toInt().coerceIn(0, tabsCount - 1)
                        onTabSelected(tappedIndex)
                        animateToValue(tappedIndex.toFloat())
                    } else {
                        // It was a drag — snap to nearest tab using current value
                        val targetIndex = value.roundToInt().coerceIn(0, tabsCount - 1)
                        onTabSelected(targetIndex)
                        animateToValue(targetIndex.toFloat())
                    }
                },
                onDrag = { _, dragAmount ->
                    // Small threshold to distinguish tap from drag
                    if (kotlin.math.abs(dragAmount.x) > 2f || kotlin.math.abs(dragAmount.y) > 2f) {
                        hasDragged = true
                    }
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth)
                            .coerceIn(0f, (tabsCount - 1).toFloat())
                    )
                }
            )
        }

        LaunchedEffect(selectedIndex) {
            if (dampedDragAnimation.targetValue.roundToInt() != selectedIndex) {
                dampedDragAnimation.animateToValue(selectedIndex.toFloat())
            }
        }

        // Gesture Tracking Box covering the entire row
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(dampedDragAnimation.modifier)
        )

        // Flat Stadium Indicator
        Box(
            Modifier
                .graphicsLayer {
                    translationX = dampedDragAnimation.value * tabWidth
                }
                .fillMaxHeight()
                .width(with(density) { tabWidth.toDp() })
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(64.dp)
                    .height(56.dp)
                    .background(contentColor.copy(alpha = 0.1f), CircleShape)
            )
        }

        // Tabs
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, pair ->
                val tabText = when (index) {
                    0 -> stringResource(R.string.nav_inicio)
                    1 -> stringResource(R.string.nav_novedades)
                    2 -> stringResource(R.string.nav_radio)
                    3 -> stringResource(R.string.nav_biblioteca)
                    else -> pair.first
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val isSelected = selectedIndex == index
                    val selectedTint = Color(0xFFFA243C)
                    Icon(
                        painter = painterResource(id = pair.second),
                        contentDescription = tabText,
                        tint = if (isSelected) selectedTint else contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tabText,
                        color = if (isSelected) selectedTint else contentColor.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}