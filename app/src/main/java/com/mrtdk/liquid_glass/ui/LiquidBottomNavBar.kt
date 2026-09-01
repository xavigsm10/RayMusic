package com.mrtdk.liquid_glass.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.ui.components.FloatingMiniPlayer
import com.mrtdk.liquid_glass.ui.components.LocalBackdrop
import com.mrtdk.liquid_glass.ui.components.NavSearchState
import com.mrtdk.liquid_glass.ui.components.floatingtabbar.FloatingTabBar
import com.mrtdk.liquid_glass.ui.components.floatingtabbar.FloatingTabBarDefaults
import com.mrtdk.liquid_glass.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import com.mrtdk.liquid_glass.ui.components.floatingtabbar.LocalTabBarBackdropFrozen
import com.mrtdk.liquid_glass.ui.components.floatingtabbar.rememberFloatingTabBarScrollConnection
import com.mrtdk.liquid_glass.ui.components.shapes.ContinuousRoundedRectangle
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import com.mrtdk.liquid_glass.ui.utils.Motion
import com.mrtdk.liquid_glass.ui.utils.bounceClick
import com.mrtdk.liquid_glass.utils.InteractiveHighlight
import kotlinx.coroutines.delay

private val NavBarShape = ContinuousRoundedRectangle(percent = 50)
private val NavBarSearchBarHeight = 48.dp
private val NavBarStandaloneReserve = 80.dp
private val NavBarMinTabWidth = 56.dp
private const val KeyboardOpenDelayMs = 260L

data class NavTabItem(
    val index: Int,
    val titleRes: Int,
    val iconRes: Int
)

val MainNavTabs = listOf(
    NavTabItem(0, R.string.nav_inicio, R.drawable.nav_inicio),
    NavTabItem(1, R.string.nav_novedades, R.drawable.nav_novedades),
    NavTabItem(2, R.string.nav_radio, R.drawable.nav_radio),
    NavTabItem(3, R.string.nav_biblioteca, R.drawable.nav_biblioteca)
)

/**
 * The floating navigation bar inspired by Convx/iOS:
 * - Fluid draggable spring puck (`DampedDragAnimation`) with glass refraction & glow (`InteractiveHighlight`)
 * - Superellipse continuous capsule (`ContinuousRoundedRectangle`)
 * - Gooey liquid morph during inline collapse/expansion
 * - Dynamic search morph expanding circular search button into search input bar docked to keyboard
 * - Integrated dockable MiniPlayer accessory (56.dp above tabs, 48.dp inline in center when collapsed)
 */
@Composable
fun LiquidBottomNavBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
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
    val isSearchActive = selectedIndex == 4
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var keyboardActive by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchActive) {
        if (!isSearchActive) {
            keyboardActive = false
        }
    }

    var textFieldValue by remember(searchQuery) {
        mutableStateOf(
            TextFieldValue(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        )
    }

    var lastSelectedTabIndex by remember { mutableStateOf(0) }
    if (selectedIndex != 4) {
        lastSelectedTabIndex = selectedIndex
    }

    val navSearchState = remember(
        isSearchActive,
        keyboardActive,
        textFieldValue,
        lastSelectedTabIndex
    ) {
        NavSearchState(
            visualActive = isSearchActive,
            keyboardActive = keyboardActive,
            query = textFieldValue,
            onQueryChange = { newTfv ->
                textFieldValue = newTfv
                onSearchQueryChange(newTfv.text)
            },
            onSubmit = { queryText ->
                onSearchSubmit(queryText)
                focusManager.clearFocus()
                keyboardActive = false
            },
            onTapSearchIcon = {
                onTabSelected(4)
                keyboardActive = false
            },
            onTapBar = {
                keyboardActive = true
            },
            onExit = {
                keyboardActive = false
                focusManager.clearFocus()
                onTabSelected(lastSelectedTabIndex)
            },
            onCloseKeyboard = {
                keyboardActive = false
                focusManager.clearFocus()
            },
            focusRequester = focusRequester
        )
    }

    val actualTintColor = if (tintColor != Color.Unspecified) tintColor
    else if (!isDarkMode) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)

    val actualContentColor = if (contentColor != Color.Unspecified) contentColor
    else if (!isDarkMode) Color.Black else Color.White

    AppFloatingNavBarChrome(
        selectedIndex = selectedIndex,
        lastSelectedTabIndex = lastSelectedTabIndex,
        onTabSelected = onTabSelected,
        playerState = playerState,
        isPlaying = isPlaying,
        onTogglePlayPause = onTogglePlayPause,
        onMiniPlayerClick = onMiniPlayerClick,
        onNext = onNext,
        onPrevious = onPrevious,
        playbackProgress = playbackProgress,
        onSeek = onSeek,
        scrollConnection = scrollConnection,
        pureBlack = pureBlack,
        tintColor = actualTintColor,
        contentColor = actualContentColor,
        searchModeActive = isSearchActive,
        navSearch = navSearchState,
        tabPosition = tabPosition,
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AppFloatingNavBarChrome(
    selectedIndex: Int,
    lastSelectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    playerState: PlayerState?,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    playbackProgress: () -> Float,
    onSeek: (Float) -> Unit,
    scrollConnection: FloatingTabBarScrollConnection,
    pureBlack: Boolean,
    tintColor: Color,
    contentColor: Color,
    searchModeActive: Boolean,
    navSearch: NavSearchState,
    tabPosition: (() -> Float?)?,
    modifier: Modifier,
) {
    BackHandler(enabled = searchModeActive, onBack = navSearch.onExit)

    var expandedContentWidthPx by remember { mutableStateOf<Int?>(null) }
    val isDarkMode by com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState()
    val backdrop = LocalBackdrop.current

    val backgroundColor = when {
        pureBlack -> Color.Black
        else -> Color.Transparent
    }

    val tabTextColor = if (isDarkMode) Color.White else Color.Black
    val activeAccentColor = Color(0xFFFA243C)
    val selectedContentColor = activeAccentColor
    val unselectedContentColor = tabTextColor.copy(alpha = 0.6f)
    val miniPlayerContentColor = tabTextColor

    val selectedTabKey = if (selectedIndex == 4) lastSelectedTabIndex else selectedIndex

    val surfaceTintColor = if (isDarkMode) Color(0xFF1A1A1A).copy(alpha = 0.35f) else Color(0xFFFAFAFA).copy(alpha = 0.45f)

    val glassStyle = com.mrtdk.glass.LocalGlassStyle.current
    val isSolid = glassStyle == "solid"
    val solidBgColor = if (isDarkMode) Color(0xFF242428) else Color(0xFFE8E8EC)

    val tabBarContentModifier = if (isSolid) {
        Modifier
            .clip(NavBarShape)
            .background(solidBgColor)
    } else {
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { NavBarShape },
                effects = {
                    vibrancy()
                    blur(3f.dp.toPx())
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        lens(
                            refractionHeight = 19f.dp.toPx(),
                            refractionAmount = 28f.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = true
                        )
                    }
                },
                highlight = { Highlight.Default.copy(alpha = 0.35f) },
                shadow = { Shadow.Default },
                onDrawSurface = { 
                    drawRect(if (tintColor != Color.Unspecified) tintColor.copy(alpha = 0.35f) else surfaceTintColor) 
                }
            )
    }

    val inlineAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? =
        if (playerState != null) {
            { accessoryModifier, _ ->
                FloatingMiniPlayer(
                    isInline = true,
                    playerState = playerState,
                    isPlaying = isPlaying,
                    onTogglePlayPause = onTogglePlayPause,
                    onClick = onMiniPlayerClick,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    contentColor = miniPlayerContentColor,
                    playbackProgress = playbackProgress,
                    onSeek = onSeek,
                    modifier = accessoryModifier.then(tabBarContentModifier),
                )
            }
        } else {
            null
        }

    val expandedAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? =
        if (playerState != null) {
            { accessoryModifier, _ ->
                FloatingMiniPlayer(
                    isInline = false,
                    playerState = playerState,
                    isPlaying = isPlaying,
                    onTogglePlayPause = onTogglePlayPause,
                    onClick = onMiniPlayerClick,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    contentColor = miniPlayerContentColor,
                    playbackProgress = playbackProgress,
                    onSeek = onSeek,
                    modifier = accessoryModifier.fillMaxWidth().then(tabBarContentModifier),
                )
            }
        } else {
            null
        }

    BoxWithConstraints(modifier) {
        val tabWidth = ((maxWidth - NavBarStandaloneReserve) / MainNavTabs.size)
            .coerceIn(NavBarMinTabWidth, FloatingTabBarDefaults.TabWidth)

        FloatingTabBar(
            selectedTabKey = selectedTabKey,
            tabPosition = tabPosition,
            scrollConnection = scrollConnection,
            modifier = Modifier.fillMaxWidth(),
            tabBarContentModifier = tabBarContentModifier,
            inlineAccessory = inlineAccessory,
            expandedAccessory = expandedAccessory,
            colors = FloatingTabBarDefaults.colors(
                backgroundColor = backgroundColor,
                accessoryBackgroundColor = backgroundColor,
            ),
            sizes = FloatingTabBarDefaults.sizes(
                tabBarContentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                tabExpandedContentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp),
                tabInlineContentPadding = PaddingValues(8.dp),
                tabWidth = tabWidth,
            ),
            backdrop = backdrop,
            accentColor = activeAccentColor,
            searchMode = searchModeActive,
            searchBarContent = if (searchModeActive) {
                { contentModifier ->
                    SearchBarInteractivePill(
                        state = navSearch,
                        contentColor = if (isDarkMode) Color.White else Color.Black,
                        modifier = contentModifier,
                    )
                }
            } else {
                null
            },
            expandedContentWidthPx = expandedContentWidthPx,
            onExpandedWidthChanged = { expandedContentWidthPx = it },
        ) {
            MainNavTabs.forEach { tabItem ->
                val isSelected = tabItem.index == selectedIndex
                tab(
                    key = tabItem.index,
                    title = {
                        Text(
                            text = stringResource(tabItem.titleRes),
                            color = if (isSelected) selectedContentColor else unselectedContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp,
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(tabItem.iconRes),
                            contentDescription = stringResource(tabItem.titleRes),
                            tint = if (isSelected) selectedContentColor else unselectedContentColor,
                            modifier = Modifier.size(28.dp),
                        )
                    },
                    onClick = {
                        if (searchModeActive) {
                            navSearch.onExit()
                        } else {
                            onTabSelected(tabItem.index)
                        }
                    },
                )
            }

            standaloneTab(
                key = 4,
                title = {
                    Text(
                        text = stringResource(R.string.search_action),
                        color = if (selectedIndex == 4) selectedContentColor else unselectedContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_action),
                        tint = if (selectedIndex == 4) selectedContentColor else unselectedContentColor,
                        modifier = Modifier.size(26.dp),
                    )
                },
                onClick = {
                    if (searchModeActive) {
                        navSearch.onTapBar()
                    } else {
                        navSearch.onTapSearchIcon()
                    }
                },
            )
        }
    }
}

/**
 * Interactive search pill row directly inside [SearchExpandedBar] with embedded [BasicTextField].
 */
@Composable
private fun SearchBarInteractivePill(
    state: NavSearchState,
    contentColor: Color,
    modifier: Modifier,
) {
    val glowScope = rememberCoroutineScope()
    val glow = remember(glowScope) { InteractiveHighlight(animationScope = glowScope) }

    LaunchedEffect(Unit) {
        delay(120)
        try {
            state.focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .then(glow.gestureModifier)
            .then(glow.modifier)
            .padding(horizontal = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .bounceClick { state.onExit() }
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.close_action),
                tint = contentColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (state.query.text.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = state.query,
                onValueChange = state.onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = contentColor,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(contentColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { state.onSubmit(state.query.text) },
                    onDone = { state.onSubmit(state.query.text) },
                    onGo = { state.onSubmit(state.query.text) },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(state.focusRequester)
                    .onKeyEvent {
                        if (it.key == Key.Enter) {
                            state.onSubmit(state.query.text)
                            true
                        } else {
                            false
                        }
                    }
            )
        }
        if (state.query.text.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .bounceClick { state.onQueryChange(TextFieldValue("")) }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_action),
                    tint = contentColor,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}