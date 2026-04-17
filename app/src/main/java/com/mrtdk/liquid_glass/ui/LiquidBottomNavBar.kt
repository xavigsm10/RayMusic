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
    "Inicio" to Icons.Default.Home,
    "Novedades" to Icons.Default.GridView,
    "Radio" to Icons.Default.Podcasts, // iOS radio tower approximation
    "Biblioteca" to Icons.Default.LibraryMusic
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
    val isSearchActive = selectedIndex == 4
    val mainWeight by animateFloatAsState(if (isSearchActive) 0.001f else 1f, animationSpec = tween(400))
    val searchWeight by animateFloatAsState(if (isSearchActive) 1f else 0.001f, animationSpec = tween(400))
    val searchWidth by androidx.compose.animation.core.animateDpAsState(if (isSearchActive) 0.dp else 72.dp, animationSpec = tween(400))

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
        horizontalArrangement = Arrangement.spacedBy(if (isSearchActive) 0.dp else 12.dp)
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
        }

        // Search Pill
        glassScope.GlassBox(
            modifier = Modifier
                .then(if (isSearchActive) Modifier.weight(searchWeight) else Modifier.width(searchWidth))
                .fillMaxHeight(),
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isSearchActive) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = contentColor
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color.White,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp)
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(color = contentColor, fontSize = 16.sp),
                        cursorBrush = SolidColor(contentColor),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchSubmit(searchQuery) }),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Buscar en RayMusic", color = contentColor.copy(alpha = 0.5f), fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    )
                    IconButton(onClick = {
                        onSearchQueryChange("")
                        onTabSelected(0)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = contentColor
                        )
                    }
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

        // Indicator Bubble — pure liquid glass that refracts icons behind it
        
        Box(
            Modifier
                .graphicsLayer {
                    val currentTabX = dampedDragAnimation.value * tabWidth
                    translationX = currentTabX

                    var scaleX = dampedDragAnimation.scaleX
                    var scaleY = dampedDragAnimation.scaleY
                    val velocity = dampedDragAnimation.velocity / 10f

                    scaleX /= 1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f)
                    scaleY *= 1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f)

                    this.scaleX = scaleX
                    this.scaleY = scaleY
                }
                .fillMaxHeight()
                .width(with(density) { tabWidth.toDp() })
                .padding(2.dp)
        ) {
            glassScope.GlassBox(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                shape = CircleShape,
                blur = 0.8f,
                centerDistortion = 0.2f,
                scale = 0.02f,
                warpEdges = 0.6f,
                tint = tintColor.copy(alpha = 0.4f),
                elevation = 4.dp
            ) {
                // Subtle inner tint — glass refraction does the rest
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(contentColor.copy(alpha = 0.05f))
                )
            }
        }

        // Tabs
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, pair ->
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
                        imageVector = pair.second,
                        contentDescription = pair.first,
                        tint = if (isSelected) selectedTint else contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = pair.first,
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