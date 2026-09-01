package com.mrtdk.liquid_glass.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.painter.Painter
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.ui.components.Material3SettingsGroup
import com.mrtdk.liquid_glass.ui.components.Material3SettingsItem
import com.mrtdk.liquid_glass.utils.LocaleUtils
import com.mrtdk.liquid_glass.utils.Updater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SettingsSubScreen {
    LYRICS,
    PLAYER,
    LISTEN_TOGETHER,
    SPOTIFY,
    CONTENT,
    PRIVACY,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onDisableScreenshotChanged: (Boolean) -> Unit,
    onUpdateAvailable: (Updater.ReleaseInfo) -> Unit,
    onGlassStyleChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var activeSubScreen by remember { mutableStateOf<SettingsSubScreen?>(null) }

    // Intercept back navigation to return to main settings menu if inside a subscreen
    BackHandler {
        if (activeSubScreen != null) {
            activeSubScreen = null
        } else {
            onBack()
        }
    }

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "settings_navigation"
    ) { subScreen ->
        when (subScreen) {
            null -> MainSettingsMenu(
                onBack = onBack,
                onNavigateTo = { activeSubScreen = it },
                onGlassStyleChanged = onGlassStyleChanged
            )
            SettingsSubScreen.LYRICS -> LyricsSettingsScreen(
                onBack = { activeSubScreen = null }
            )
            SettingsSubScreen.PLAYER -> PlayerSettingsScreen(
                onBack = { activeSubScreen = null },
                onOpenEqualizer = onOpenEqualizer
            )
            SettingsSubScreen.LISTEN_TOGETHER -> ListenTogetherSettingsScreen(
                onBack = { activeSubScreen = null }
            )
            SettingsSubScreen.SPOTIFY -> SpotifySettingsScreen(
                onBack = { activeSubScreen = null }
            )
            SettingsSubScreen.CONTENT -> ContentSettingsScreen(
                onBack = { activeSubScreen = null }
            )
            SettingsSubScreen.PRIVACY -> PrivacySettingsScreen(
                onBack = { activeSubScreen = null },
                onDisableScreenshotChanged = onDisableScreenshotChanged
            )
            SettingsSubScreen.ABOUT -> AboutSettingsScreen(
                onBack = { activeSubScreen = null },
                onUpdateAvailable = onUpdateAvailable
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsMenu(
    onBack: () -> Unit,
    onNavigateTo: (SettingsSubScreen) -> Unit,
    onGlassStyleChanged: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var showGlassStyleDialog by remember { mutableStateOf(false) }
    var showArtworkStyleDialog by remember { mutableStateOf(false) }

    val isDarkMode by com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = stringResource(R.string.back_action),
                    tint = Color(0xFFFA243C)
                )
            }
            Text(
                text = stringResource(R.string.ajustes),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_appearance),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(id = R.drawable.lyrics),
                        title = { Text(stringResource(R.string.settings_lyrics)) },
                        description = { Text(stringResource(R.string.settings_lyrics_desc)) },
                        onClick = { onNavigateTo(SettingsSubScreen.LYRICS) }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.DarkMode),
                        title = { Text(stringResource(R.string.theme_app_title)) },
                        description = {
                            Text(if (isDarkMode) stringResource(R.string.theme_dark_mode_default) else stringResource(R.string.theme_light_mode))
                        },
                        onClick = { showThemeDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Palette),
                        title = { Text(stringResource(R.string.liquid_glass)) },
                        description = {
                            val currentStyle = LibraryManager.getGlassStyle()
                            val currentStyleName = when (currentStyle) {
                                "transparent" -> stringResource(R.string.vidrio_liquido_transparente)
                                "solid" -> "Sólido (Material 3)"
                                else -> stringResource(R.string.vidrio_liquido_transparente)
                            }
                            Text(currentStyleName)
                        },
                        onClick = { showGlassStyleDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Tune),
                        title = { Text("Apariencia") },
                        description = {
                            val currentStyle = LibraryManager.getPlayerArtworkStyle()
                            val currentStyleName = when (currentStyle) {
                                "normal" -> "Normal"
                                else -> "Fullartwork"
                            }
                            Text(currentStyleName)
                        },
                        onClick = { showArtworkStyleDialog = true }
                    )
                )
            )

            if (showThemeDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeDialog = false },
                    title = { Text(stringResource(R.string.theme_app_title), color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        com.mrtdk.liquid_glass.ui.theme.ThemeManager.setThemeMode(com.mrtdk.liquid_glass.ui.theme.ThemeManager.MODE_DARK)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isDarkMode,
                                    onClick = {
                                        com.mrtdk.liquid_glass.ui.theme.ThemeManager.setThemeMode(com.mrtdk.liquid_glass.ui.theme.ThemeManager.MODE_DARK)
                                        showThemeDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.theme_dark_mode_default), color = Color.White)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        com.mrtdk.liquid_glass.ui.theme.ThemeManager.setThemeMode(com.mrtdk.liquid_glass.ui.theme.ThemeManager.MODE_LIGHT)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !isDarkMode,
                                    onClick = {
                                        com.mrtdk.liquid_glass.ui.theme.ThemeManager.setThemeMode(com.mrtdk.liquid_glass.ui.theme.ThemeManager.MODE_LIGHT)
                                        showThemeDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.theme_light_mode), color = Color.White)
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showThemeDialog = false }) {
                            Text(stringResource(R.string.cancelar), color = Color(0xFFFA243C))
                        }
                    },
                    containerColor = com.mrtdk.liquid_glass.ui.theme.ThemeManager.surfaceColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_behavior),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.PlayArrow),
                        title = { Text(stringResource(R.string.settings_player_sound)) },
                        description = { Text(stringResource(R.string.settings_player_sound_desc)) },
                        onClick = { onNavigateTo(SettingsSubScreen.PLAYER) }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.MusicNote),
                        title = { Text(stringResource(R.string.settings_spotify)) },
                        description = { Text(stringResource(R.string.settings_spotify_desc)) },
                        onClick = { onNavigateTo(SettingsSubScreen.SPOTIFY) }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Language),
                        title = { Text(stringResource(R.string.settings_content)) },
                        description = { Text(stringResource(R.string.settings_content_desc)) },
                        onClick = { onNavigateTo(SettingsSubScreen.CONTENT) }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_privacy),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Security),
                        title = { Text(stringResource(R.string.settings_privacy)) },
                        description = { Text(stringResource(R.string.settings_privacy_desc)) },
                        onClick = { onNavigateTo(SettingsSubScreen.PRIVACY) }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_app),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Info),
                        title = { Text(stringResource(R.string.acerca_de)) },
                        description = { Text(stringResource(R.string.settings_about_desc)) },
                        onClick = { onNavigateTo(SettingsSubScreen.ABOUT) }
                    )
                )
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    if (showGlassStyleDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.liquid_glass),
            options = listOf(
                "transparent" to stringResource(R.string.vidrio_liquido_transparente),
                "solid" to "Sólido (Material 3)"
            ),
            selectedValue = LibraryManager.getGlassStyle(),
            onDismiss = { showGlassStyleDialog = false },
            onSelect = {
                LibraryManager.saveGlassStyle(it)
                onGlassStyleChanged(it)
            }
        )
    }

    if (showArtworkStyleDialog) {
        SingleChoiceDialog(
            title = "Apariencia",
            options = listOf(
                "fullartwork" to "Fullartwork",
                "normal" to "Normal"
            ),
            selectedValue = LibraryManager.getPlayerArtworkStyle(),
            onDismiss = { showArtworkStyleDialog = false },
            onSelect = {
                LibraryManager.savePlayerArtworkStyle(it)
            }
        )
    }
}

// Sub-screen: Lyrics
@Composable
fun LyricsSettingsScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    var lyricsTextPosition by remember { mutableStateOf(LibraryManager.getString("lyrics_text_position", "left") ?: "left") }
    var lyricsAnimationStyle by remember { mutableStateOf(LibraryManager.getString("lyrics_animation_style", "echomusic_1") ?: "echomusic_1") }
    var lyricsGlowEffect by remember { mutableStateOf(LibraryManager.getString("lyrics_glow_effect", "false") == "true") }
    var lyricsAppleBlur by remember { mutableStateOf(LibraryManager.getString("lyrics_apple_blur", "true") == "true") }
    var lyricsStandardBlur by remember { mutableStateOf(LibraryManager.getString("lyrics_standard_blur", "false") == "true") }
    var lyricsTextSize by remember { mutableStateOf(LibraryManager.getString("lyrics_text_size", "28")?.toFloatOrNull() ?: 28f) }
    var lyricsLineSpacing by remember { mutableStateOf(LibraryManager.getString("lyrics_line_spacing", "1.3")?.toFloatOrNull() ?: 1.3f) }
    var lyricsClickChange by remember { mutableStateOf(LibraryManager.getString("lyrics_click_change", "true") == "true") }
    var lyricsAutoScroll by remember { mutableStateOf(LibraryManager.getString("lyrics_auto_scroll", "true") == "true") }
    var swipeLyrics by remember { mutableStateOf(LibraryManager.getString("lyrics_swipe_to_change_song", "false") == "true") }
    var enableLyricsThumbnailPlayPause by remember { mutableStateOf(LibraryManager.getString("lyrics_thumbnail_play_pause", "false") == "true") }
    var hideStatusBarOnFullscreen by remember { mutableStateOf(LibraryManager.getString("hide_status_bar_on_fullscreen", "false") == "true") }

    var showPositionDialog by remember { mutableStateOf(false) }
    var showAnimationDialog by remember { mutableStateOf(false) }
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var showSpacingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    tint = Color(0xFFFA243C)
                )
            }
            Text(
                text = stringResource(R.string.settings_lyrics),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_visual),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(id = R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_align_title)) },
                        description = {
                            Text(
                                when (lyricsTextPosition) {
                                    "left" -> stringResource(R.string.lyrics_align_left)
                                    "center" -> stringResource(R.string.lyrics_align_center)
                                    "right" -> stringResource(R.string.lyrics_align_right)
                                    else -> stringResource(R.string.lyrics_align_left)
                                }
                            )
                        },
                        onClick = { showPositionDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(id = R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_anim_title)) },
                        description = {
                            Text(
                                when (lyricsAnimationStyle) {
                                    "none" -> stringResource(R.string.lyrics_anim_none)
                                    "fade" -> stringResource(R.string.lyrics_anim_fade)
                                    "glow" -> stringResource(R.string.lyrics_anim_glow)
                                    "slide" -> stringResource(R.string.lyrics_anim_slide)
                                    "karaoke" -> stringResource(R.string.lyrics_anim_karaoke)
                                    "echomusic_1" -> stringResource(R.string.lyrics_anim_echo_fluid)
                                    "apple" -> stringResource(R.string.lyrics_anim_apple)
                                    "apple_v2" -> stringResource(R.string.lyrics_anim_apple_v2)
                                    "lyrics_v2" -> stringResource(R.string.lyrics_anim_fluid_v2)
                                    "metro_lyrics" -> stringResource(R.string.lyrics_anim_metro)
                                    else -> stringResource(R.string.lyrics_anim_echo_fluid)
                                }
                            )
                        },
                        onClick = { showAnimationDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(id = R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_glow_title)) },
                        description = { Text(stringResource(R.string.lyrics_glow_desc)) },
                        trailingContent = {
                            Switch(
                                checked = lyricsGlowEffect,
                                onCheckedChange = { value ->
                                    lyricsGlowEffect = value
                                    LibraryManager.saveString("lyrics_glow_effect", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !lyricsGlowEffect
                            lyricsGlowEffect = value
                            LibraryManager.saveString("lyrics_glow_effect", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(id = R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_size_title)) },
                        description = { Text("${lyricsTextSize.roundToInt()} sp") },
                        onClick = { showTextSizeDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(id = R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_spacing_title)) },
                        description = { Text("${String.format("%.1f", lyricsLineSpacing)}x") },
                        onClick = { showSpacingDialog = true }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_effects),
                items = listOfNotNull(
                    if (lyricsAnimationStyle == "echomusic_1") {
                        Material3SettingsItem(
                            icon = painterResource(id = R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_blur_bg_title)) },
                            description = { Text(stringResource(R.string.lyrics_blur_bg_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = lyricsAppleBlur,
                                    onCheckedChange = { value ->
                                        lyricsAppleBlur = value
                                        LibraryManager.saveString("lyrics_apple_blur", value.toString())
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                                )
                            },
                            onClick = {
                                val value = !lyricsAppleBlur
                                lyricsAppleBlur = value
                                LibraryManager.saveString("lyrics_apple_blur", value.toString())
                            }
                        )
                    } else null,
                    Material3SettingsItem(
                        icon = painterResource(id = R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_blur_std_title)) },
                        description = { Text(stringResource(R.string.lyrics_blur_std_desc)) },
                        trailingContent = {
                            Switch(
                                checked = lyricsStandardBlur,
                                onCheckedChange = { value ->
                                    lyricsStandardBlur = value
                                    LibraryManager.saveString("lyrics_standard_blur", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !lyricsStandardBlur
                            lyricsStandardBlur = value
                            LibraryManager.saveString("lyrics_standard_blur", value.toString())
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_interaction),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(id = R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_seek_title)) },
                        description = { Text(stringResource(R.string.lyrics_seek_desc)) },
                        trailingContent = {
                            Switch(
                                checked = lyricsClickChange,
                                onCheckedChange = { value ->
                                    lyricsClickChange = value
                                    LibraryManager.saveString("lyrics_click_change", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !lyricsClickChange
                            lyricsClickChange = value
                            LibraryManager.saveString("lyrics_click_change", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(id = R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_autoscroll_title)) },
                        description = { Text(stringResource(R.string.lyrics_autoscroll_desc)) },
                        trailingContent = {
                            Switch(
                                checked = lyricsAutoScroll,
                                onCheckedChange = { value ->
                                    lyricsAutoScroll = value
                                    LibraryManager.saveString("lyrics_auto_scroll", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !lyricsAutoScroll
                            lyricsAutoScroll = value
                            LibraryManager.saveString("lyrics_auto_scroll", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Swipe),
                        title = { Text(stringResource(R.string.lyrics_swipe_title)) },
                        description = { Text(stringResource(R.string.lyrics_swipe_desc)) },
                        trailingContent = {
                            Switch(
                                checked = swipeLyrics,
                                onCheckedChange = { value ->
                                    swipeLyrics = value
                                    LibraryManager.saveString("lyrics_swipe_to_change_song", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !swipeLyrics
                            swipeLyrics = value
                            LibraryManager.saveString("lyrics_swipe_to_change_song", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.PlayCircle),
                        title = { Text(stringResource(R.string.lyrics_thumb_play_title)) },
                        description = { Text(stringResource(R.string.lyrics_thumb_play_desc)) },
                        trailingContent = {
                            Switch(
                                checked = enableLyricsThumbnailPlayPause,
                                onCheckedChange = { value ->
                                    enableLyricsThumbnailPlayPause = value
                                    LibraryManager.saveString("lyrics_thumbnail_play_pause", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !enableLyricsThumbnailPlayPause
                            enableLyricsThumbnailPlayPause = value
                            LibraryManager.saveString("lyrics_thumbnail_play_pause", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Fullscreen),
                        title = { Text(stringResource(R.string.lyrics_statusbar_title)) },
                        description = { Text(stringResource(R.string.lyrics_statusbar_desc)) },
                        trailingContent = {
                            Switch(
                                checked = hideStatusBarOnFullscreen,
                                onCheckedChange = { value ->
                                    hideStatusBarOnFullscreen = value
                                    LibraryManager.saveString("hide_status_bar_on_fullscreen", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !hideStatusBarOnFullscreen
                            hideStatusBarOnFullscreen = value
                            LibraryManager.saveString("hide_status_bar_on_fullscreen", value.toString())
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    // Dialogs
    if (showPositionDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.lyrics_align_title),
            options = listOf(
                "left" to stringResource(R.string.lyrics_align_left),
                "center" to stringResource(R.string.lyrics_align_center),
                "right" to stringResource(R.string.lyrics_align_right)
            ),
            selectedValue = lyricsTextPosition,
            onDismiss = { showPositionDialog = false },
            onSelect = {
                lyricsTextPosition = it
                LibraryManager.saveString("lyrics_text_position", it)
            }
        )
    }

    if (showAnimationDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.lyrics_anim_title),
            options = listOf(
                "none" to stringResource(R.string.lyrics_anim_none),
                "fade" to stringResource(R.string.lyrics_anim_fade),
                "glow" to stringResource(R.string.lyrics_anim_glow),
                "slide" to stringResource(R.string.lyrics_anim_slide),
                "karaoke" to stringResource(R.string.lyrics_anim_karaoke),
                "echomusic_1" to stringResource(R.string.lyrics_anim_echo_fluid),
                "apple" to stringResource(R.string.lyrics_anim_apple),
                "apple_v2" to stringResource(R.string.lyrics_anim_apple_v2),
                "lyrics_v2" to stringResource(R.string.lyrics_anim_fluid_v2),
                "metro_lyrics" to stringResource(R.string.lyrics_anim_metro)
            ),
            selectedValue = lyricsAnimationStyle,
            onDismiss = { showAnimationDialog = false },
            onSelect = {
                lyricsAnimationStyle = it
                LibraryManager.saveString("lyrics_animation_style", it)
            }
        )
    }

    if (showTextSizeDialog) {
        SliderDialog(
            title = stringResource(R.string.lyrics_size_title),
            value = lyricsTextSize,
            valueRange = 20f..44f,
            onDismiss = { showTextSizeDialog = false },
            onSave = {
                lyricsTextSize = it
                LibraryManager.saveString("lyrics_text_size", it.toString())
            }
        )
    }

    if (showSpacingDialog) {
        SliderDialog(
            title = stringResource(R.string.lyrics_spacing_title),
            value = lyricsLineSpacing,
            valueRange = 1.0f..2.5f,
            steps = 15,
            isFloat = true,
            onDismiss = { showSpacingDialog = false },
            onSave = {
                lyricsLineSpacing = it
                LibraryManager.saveString("lyrics_line_spacing", it.toString())
            }
        )
    }
}

// Sub-screen: Player
@Composable
fun PlayerSettingsScreen(
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val scrollState = rememberScrollState()
    var audioQuality by remember { mutableStateOf(LibraryManager.getString("audio_quality", "auto") ?: "auto") }
    var autoplaySimilar by remember { mutableStateOf(LibraryManager.getString("autoplay_similar", "true") == "true") }
    var autoDownloadOnLike by remember { mutableStateOf(LibraryManager.getString("auto_download_on_like", "false") == "true") }
    var persistentQueue by remember { mutableStateOf(LibraryManager.getString("persistent_queue", "true") == "true") }

    var showQualityDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    tint = Color(0xFFFA243C)
                )
            }
            Text(
                text = stringResource(R.string.settings_player_sound),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_sound_quality),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.GraphicEq),
                        title = { Text(stringResource(R.string.player_quality_title)) },
                        description = {
                            Text(
                                when (audioQuality) {
                                    "low" -> "Baja (48 kbps AAC / 50 kbps Opus)"
                                    "high" -> "Alta (160 kbps Opus / 128 kbps AAC)"
                                    else -> "Automático (160 kbps Opus)"
                                }
                            )
                        },
                        onClick = { showQualityDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Equalizer),
                        title = { Text(stringResource(R.string.player_eq_title)) },
                        description = { Text(stringResource(R.string.player_eq_desc)) },
                        onClick = onOpenEqualizer
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_metrics),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.AutoAwesome),
                        title = { Text(stringResource(R.string.player_autoplay_title)) },
                        description = { Text(stringResource(R.string.player_autoplay_desc)) },
                        trailingContent = {
                            Switch(
                                checked = autoplaySimilar,
                                onCheckedChange = { value ->
                                    autoplaySimilar = value
                                    LibraryManager.saveString("autoplay_similar", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !autoplaySimilar
                            autoplaySimilar = value
                            LibraryManager.saveString("autoplay_similar", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.DownloadForOffline),
                        title = { Text(stringResource(R.string.player_like_download_title)) },
                        description = { Text(stringResource(R.string.player_like_download_desc)) },
                        trailingContent = {
                            Switch(
                                checked = autoDownloadOnLike,
                                onCheckedChange = { value ->
                                    autoDownloadOnLike = value
                                    LibraryManager.saveString("auto_download_on_like", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !autoDownloadOnLike
                            autoDownloadOnLike = value
                            LibraryManager.saveString("auto_download_on_like", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.QueueMusic),
                        title = { Text(stringResource(R.string.player_persistent_queue_title)) },
                        description = { Text(stringResource(R.string.player_persistent_queue_desc)) },
                        trailingContent = {
                            Switch(
                                checked = persistentQueue,
                                onCheckedChange = { value ->
                                    persistentQueue = value
                                    LibraryManager.saveString("persistent_queue", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !persistentQueue
                            persistentQueue = value
                            LibraryManager.saveString("persistent_queue", value.toString())
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    if (showQualityDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.player_quality_title),
            options = listOf(
                "auto" to "Automático (Recomendado - 160 kbps)",
                "high" to "Alta (160 kbps Opus / 128 kbps AAC)",
                "low" to "Baja (48 kbps AAC / 50 kbps Opus)"
            ),
            selectedValue = audioQuality,
            onDismiss = { showQualityDialog = false },
            onSelect = {
                audioQuality = it
                LibraryManager.saveString("audio_quality", it)
            }
        )
    }
}

// Sub-screen: Listen Together
@Composable
fun ListenTogetherSettingsScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val ltManager = remember { com.mrtdk.liquid_glass.listentogether.ListenTogetherManager.getInstance(context) }

    val roomState by ltManager.roomState.collectAsState()
    val role by ltManager.role.collectAsState()
    val connState by ltManager.connectionState.collectAsState()

    val isConnected = ltManager.isInRoom
    val roomCode = roomState?.roomCode ?: ""
    val roleStr = when (role) {
        com.mrtdk.liquid_glass.listentogether.RoomRole.HOST -> "host"
        com.mrtdk.liquid_glass.listentogether.RoomRole.GUEST -> "guest"
        else -> "none"
    }
    var username by remember { mutableStateOf(ltManager.client.storedUsername ?: LibraryManager.getString("listen_together_username", "") ?: "") }
    var serverUrl by remember { mutableStateOf(ltManager.client.currentServerUrl) }
    val usersCount = roomState?.users?.size ?: 0
    val connectedUsers = roomState?.users?.map { it.username } ?: emptyList()
    val hostUsername = roomState?.users?.find { it.isHost }?.username ?: ""
    val isConnecting = connState == com.mrtdk.liquid_glass.listentogether.ConnectionState.CONNECTING || connState == com.mrtdk.liquid_glass.listentogether.ConnectionState.RECONNECTING

    var smartResync by remember { mutableStateOf(LibraryManager.getString("listen_together_smart_resync", "true") == "true") }
    var syncVolume by remember { mutableStateOf(LibraryManager.getString("listen_together_sync_volume", "true") == "true") }
    var autoApproval by remember { mutableStateOf(LibraryManager.getString("listen_together_auto_approval", "false") == "true") }

    var showUsernameDialog by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    tint = Color(0xFFFA243C)
                )
            }
            Text(
                text = stringResource(R.string.settings_listen_together),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // --- Status indicator ---
            // Status pill (connected / disconnected)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isConnected) stringResource(R.string.together_status_connected) else stringResource(R.string.together_status_disconnected),
                    color = if (isConnected) Color(0xFF4CAF50) else Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            // Show connecting spinner while waiting for WebSocket response
            if (isConnecting) {
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFA243C),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Conectando a la sala...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (isConnected && roomCode.isNotBlank()) {
                // ---- Prominent Room Code Card (like Echo-Music) ----
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    ),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.together_room_code_label),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = roomCode,
                            color = Color(0xFFFA243C),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 8.sp
                        )
                        if (roleStr == "guest" && hostUsername.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Sala de $hostUsername",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Role badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (roleStr == "host") Color(0xFFFA243C).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(50.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (roleStr == "host") "👑 Anfitrión" else "🎵 Invitado",
                                    color = if (roleStr == "host") Color(0xFFFA243C) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            // User count badge
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "$usersCount",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        // Copy button
                        val clipboardManager = LocalClipboardManager.current
                        Button(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(roomCode))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Copiar código", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // ---- Connected Members Section ----
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color(0xFF1C1C1E)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "MIEMBROS DE LA SALA ($usersCount)",
                            color = Color(0xFFFA243C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        val connectedUsersList = connectedUsers
                        if (connectedUsersList.isEmpty()) {
                            Text(
                                text = "Solo tú estás en la sala.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            connectedUsersList.forEach { userInRoom ->
                                val isHostUser = userInRoom == hostUsername
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isHostUser) Icons.Default.Star else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isHostUser) Color(0xFFFA243C) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = userInRoom,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (isHostUser) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(Anfitrión)",
                                            color = Color(0xFFFA243C),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (userInRoom == username) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(Tú)",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ---- Leave Room Button ----
                Button(
                    onClick = { ltManager.leaveRoom() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFA243C).copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = Color(0xFFFA243C), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.together_btn_leave), color = Color(0xFFFA243C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

            } else {
                // Not connected — create / join buttons
                Material3SettingsGroup(
                    title = stringResource(R.string.together_status_title),
                    items = listOf(
                        Material3SettingsItem(
                            icon = rememberPainter(Icons.Default.AddHomeWork),
                            title = { Text(stringResource(R.string.together_btn_create)) },
                            description = { Text(stringResource(R.string.together_create_room_desc)) },
                            onClick = {
                                if (username.isBlank()) showUsernameDialog = true
                                else {
                                    ltManager.createRoom(username)
                                }
                            }
                        ),
                        Material3SettingsItem(
                            icon = rememberPainter(Icons.Default.GroupAdd),
                            title = { Text(stringResource(R.string.together_btn_join)) },
                            description = { Text(stringResource(R.string.together_join_room_desc)) },
                            onClick = {
                                if (username.isBlank()) showUsernameDialog = true
                                else showJoinDialog = true
                            }
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_room_profile),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Person),
                        title = { Text(stringResource(R.string.together_username_title)) },
                        description = { Text(if (username.isEmpty()) stringResource(R.string.together_undefined) else username) },
                        onClick = { showUsernameDialog = true }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_room_config),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Sync),
                        title = { Text(stringResource(R.string.together_resync_title)) },
                        description = { Text(stringResource(R.string.together_resync_desc)) },
                        trailingContent = {
                            Switch(
                                checked = smartResync,
                                onCheckedChange = { value ->
                                    smartResync = value
                                    LibraryManager.saveString("listen_together_smart_resync", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !smartResync
                            smartResync = value
                            LibraryManager.saveString("listen_together_smart_resync", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.VolumeUp),
                        title = { Text(stringResource(R.string.together_volume_title)) },
                        description = { Text(stringResource(R.string.together_volume_desc)) },
                        trailingContent = {
                            Switch(
                                checked = syncVolume,
                                onCheckedChange = { value ->
                                    syncVolume = value
                                    LibraryManager.saveString("listen_together_sync_volume", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !syncVolume
                            syncVolume = value
                            LibraryManager.saveString("listen_together_sync_volume", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.CheckCircleOutline),
                        title = { Text(stringResource(R.string.together_approval_title)) },
                        description = { Text(stringResource(R.string.together_approval_desc)) },
                        trailingContent = {
                            Switch(
                                checked = autoApproval,
                                onCheckedChange = { value ->
                                    autoApproval = value
                                    LibraryManager.saveString("listen_together_auto_approval", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !autoApproval
                            autoApproval = value
                            LibraryManager.saveString("listen_together_auto_approval", value.toString())
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    if (showUsernameDialog) {
        InputDialog(
            title = stringResource(R.string.together_username_title),
            placeholder = stringResource(R.string.together_username_placeholder),
            initialValue = username,
            onDismiss = { showUsernameDialog = false },
            onSave = {
                username = it
                ltManager.client.storedUsername = it
                LibraryManager.saveString("listen_together_username", it)
            }
        )
    }

    if (showServerDialog) {
        InputDialog(
            title = stringResource(R.string.together_server_title),
            placeholder = "wss://...",
            initialValue = serverUrl,
            onDismiss = { showServerDialog = false },
            onSave = {
                serverUrl = it
                ltManager.client.currentServerUrl = it
            }
        )
    }

    if (showJoinDialog) {
        InputDialog(
            title = stringResource(R.string.together_btn_join),
            placeholder = stringResource(R.string.together_join_placeholder),
            initialValue = "",
            onDismiss = { showJoinDialog = false },
            onSave = { code ->
                val trimmedCode = code.uppercase().trim()
                if (trimmedCode.isNotBlank()) {
                    ltManager.joinRoom(trimmedCode, username)
                }
            }
        )
    }

}

// Sub-screen: Content
@Composable
fun ContentSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentLang = remember { LibraryManager.getAppLanguage(context) }
    var contentLanguage by remember { mutableStateOf(LibraryManager.getString("content_language", "system") ?: "system") }
    var contentCountry by remember { mutableStateOf(LibraryManager.getString("content_country", "system") ?: "system") }
    var suggestionRegion by remember { mutableStateOf(LibraryManager.getString("suggestion_region", "system") ?: "system") }

    var hideExplicit by remember { mutableStateOf(LibraryManager.getString("hide_explicit", "false") == "true") }
    var hideVideoSongs by remember { mutableStateOf(LibraryManager.getString("hide_video_songs", "false") == "true") }
    var hideYoutubeShorts by remember { mutableStateOf(LibraryManager.getString("hide_youtube_shorts", "false") == "true") }

    var showLangDialog by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    tint = Color(0xFFFA243C)
                )
            }
            Text(
                text = stringResource(R.string.settings_content),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_lang_locale),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Translate),
                        title = { Text(stringResource(R.string.idioma_app)) },
                        description = {
                            Text(
                                when (currentLang) {
                                    "es" -> "Español"
                                    "en" -> "English"
                                    "pt" -> "Português"
                                    "tr" -> "Türkçe"
                                    else -> stringResource(R.string.predeterminado_sistema)
                                }
                            )
                        },
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } else {
                                showLangDialog = true
                            }
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Language),
                        title = { Text(stringResource(R.string.content_lang_title)) },
                        description = { Text(if (contentLanguage == "system") stringResource(R.string.predeterminado_sistema) else contentLanguage.uppercase()) },
                        onClick = { showCountryDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Place),
                        title = { Text(stringResource(R.string.content_region_title)) },
                        description = { Text(if (suggestionRegion == "system") stringResource(R.string.predeterminado_sistema) else suggestionRegion.uppercase()) },
                        onClick = { showRegionDialog = true }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_content_filtering),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Explicit),
                        title = { Text(stringResource(R.string.content_explicit_title)) },
                        description = { Text(stringResource(R.string.content_explicit_desc)) },
                        trailingContent = {
                            Switch(
                                checked = hideExplicit,
                                onCheckedChange = { value ->
                                    hideExplicit = value
                                    LibraryManager.saveString("hide_explicit", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !hideExplicit
                            hideExplicit = value
                            LibraryManager.saveString("hide_explicit", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.VideocamOff),
                        title = { Text(stringResource(R.string.content_video_title)) },
                        description = { Text(stringResource(R.string.content_video_desc)) },
                        trailingContent = {
                            Switch(
                                checked = hideVideoSongs,
                                onCheckedChange = { value ->
                                    hideVideoSongs = value
                                    LibraryManager.saveString("hide_video_songs", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !hideVideoSongs
                            hideVideoSongs = value
                            LibraryManager.saveString("hide_video_songs", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.OndemandVideo),
                        title = { Text(stringResource(R.string.content_shorts_title)) },
                        description = { Text(stringResource(R.string.content_shorts_desc)) },
                        trailingContent = {
                            Switch(
                                checked = hideYoutubeShorts,
                                onCheckedChange = { value ->
                                    hideYoutubeShorts = value
                                    LibraryManager.saveString("hide_youtube_shorts", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !hideYoutubeShorts
                            hideYoutubeShorts = value
                            LibraryManager.saveString("hide_youtube_shorts", value.toString())
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    if (showLangDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.language_dialog_title),
            options = listOf(
                "system" to stringResource(R.string.predeterminado_sistema),
                "es" to "Español",
                "en" to "English",
                "pt" to "Português",
                "tr" to "Türkçe"
            ),
            selectedValue = currentLang ?: "system",
            onDismiss = { showLangDialog = false },
            onSelect = { langCode ->
                LibraryManager.saveAppLanguage(context, langCode)
                LocaleUtils.applyLocale(context)
                // Restart app to apply locale cleanly
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(intent)
                if (context is Activity) {
                    context.finish()
                }
            }
        )
    }

    if (showCountryDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.content_lang_title),
            options = listOf(
                "system" to "Predeterminado",
                "es" to "Español",
                "en" to "Inglés",
                "fr" to "Francés",
                "pt" to "Português",
                "tr" to "Turco",
                "ja" to "Japonés"
            ),
            selectedValue = contentLanguage,
            onDismiss = { showCountryDialog = false },
            onSelect = {
                contentLanguage = it
                LibraryManager.saveString("content_language", it)
            }
        )
    }

    if (showRegionDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.content_region_title),
            options = listOf(
                "system" to "Predeterminado",
                "us" to "Estados Unidos",
                "es" to "España",
                "mx" to "México",
                "ar" to "Argentina",
                "br" to "Brasil"
            ),
            selectedValue = suggestionRegion,
            onDismiss = { showRegionDialog = false },
            onSelect = {
                suggestionRegion = it
                LibraryManager.saveString("suggestion_region", it)
            }
        )
    }
}

// Sub-screen: Privacy
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    onDisableScreenshotChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var pauseListenHistory by remember { mutableStateOf(LibraryManager.getString("pause_listen_history", "false") == "true") }
    var pauseSearchHistory by remember { mutableStateOf(LibraryManager.getString("pause_search_history", "false") == "true") }
    var disableScreenshot by remember { mutableStateOf(LibraryManager.getString("disable_screenshot", "false") == "true") }

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearSearchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    tint = Color(0xFFFA243C)
                )
            }
            Text(
                text = stringResource(R.string.settings_privacy),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_play_history),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.History),
                        title = { Text(stringResource(R.string.privacy_pause_history_title)) },
                        description = { Text(stringResource(R.string.privacy_pause_history_desc)) },
                        trailingContent = {
                            Switch(
                                checked = pauseListenHistory,
                                onCheckedChange = { value ->
                                    pauseListenHistory = value
                                    LibraryManager.saveString("pause_listen_history", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !pauseListenHistory
                            pauseListenHistory = value
                            LibraryManager.saveString("pause_listen_history", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.DeleteOutline),
                        title = { Text(stringResource(R.string.privacy_clear_history_title)) },
                        description = { Text(stringResource(R.string.privacy_clear_history_desc)) },
                        onClick = { showClearHistoryDialog = true }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_search_history),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.SearchOff),
                        title = { Text(stringResource(R.string.privacy_pause_search_title)) },
                        description = { Text(stringResource(R.string.privacy_pause_search_desc)) },
                        trailingContent = {
                            Switch(
                                checked = pauseSearchHistory,
                                onCheckedChange = { value ->
                                    pauseSearchHistory = value
                                    LibraryManager.saveString("pause_search_history", value.toString())
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !pauseSearchHistory
                            pauseSearchHistory = value
                            LibraryManager.saveString("pause_search_history", value.toString())
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.DeleteSweep),
                        title = { Text(stringResource(R.string.privacy_clear_search_title)) },
                        description = { Text(stringResource(R.string.privacy_clear_search_desc)) },
                        onClick = { showClearSearchDialog = true }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_screen_security),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.Screenshot),
                        title = { Text(stringResource(R.string.privacy_screenshot_title)) },
                        description = { Text(stringResource(R.string.privacy_screenshot_desc)) },
                        trailingContent = {
                            Switch(
                                checked = disableScreenshot,
                                onCheckedChange = { value ->
                                    disableScreenshot = value
                                    LibraryManager.saveString("disable_screenshot", value.toString())
                                    onDisableScreenshotChanged(value)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFA243C))
                            )
                        },
                        onClick = {
                            val value = !disableScreenshot
                            disableScreenshot = value
                            LibraryManager.saveString("disable_screenshot", value.toString())
                            onDisableScreenshotChanged(value)
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    if (showClearHistoryDialog) {
        ConfirmDialog(
            title = stringResource(R.string.privacy_clear_history_confirm_title),
            message = stringResource(R.string.privacy_clear_history_confirm_desc),
            onDismiss = { showClearHistoryDialog = false },
            onConfirm = {
                showClearHistoryDialog = false
                LibraryManager.clearPlaybackHistory()
                Toast.makeText(context, context.getString(R.string.privacy_history_cleared), Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showClearSearchDialog) {
        ConfirmDialog(
            title = stringResource(R.string.privacy_clear_search_confirm_title),
            message = stringResource(R.string.privacy_clear_search_confirm_desc),
            onDismiss = { showClearSearchDialog = false },
            onConfirm = {
                showClearSearchDialog = false
                Toast.makeText(context, context.getString(R.string.privacy_search_cleared), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// Sub-screen: About
@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit,
    onUpdateAvailable: (Updater.ReleaseInfo) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    tint = Color(0xFFFA243C)
                )
            }
            Text(
                text = stringResource(R.string.acerca_de),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Brand Logo (Real RayMusic launcher icon)
            val appIconDrawable = remember(context) {
                try {
                    context.packageManager.getApplicationIcon(context.packageName)
                } catch (e: Exception) {
                    null
                }
            }
            if (appIconDrawable != null) {
                coil.compose.AsyncImage(
                    model = appIconDrawable,
                    contentDescription = "Logo RayMusic",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.splash_logo),
                    contentDescription = "Logo RayMusic",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "RayMusic",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.version_actual, com.mrtdk.liquid_glass.BuildConfig.VERSION_NAME),
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_updates_dev),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.SystemUpdate),
                        title = { Text(stringResource(R.string.buscar_actualizaciones)) },
                        description = { Text(stringResource(R.string.settings_updates_check_desc)) },
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.buscando_actualizaciones), Toast.LENGTH_SHORT).show()
                            Updater.checkUpdate { info ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    if (info != null) {
                                        onUpdateAvailable(info)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.ultima_version_ok), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(id = R.drawable.ic_github),
                        title = { Text(stringResource(R.string.repositorio_app)) },
                        description = { Text(stringResource(R.string.ver_codigo_github)) },
                        onClick = {
                            uriHandler.openUri("https://github.com/xavigsm10/RayMusic")
                        }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(id = R.drawable.ic_paypal),
                        title = { Text("PayPal") },
                        description = { Text(stringResource(R.string.settings_paypal_desc)) },
                        onClick = {
                            uriHandler.openUri("https://www.paypal.me/XaviGranja")
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// Helpers Composable Dialogs
@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (option.first == selectedValue),
                                onClick = {
                                    onSelect(option.first)
                                    onDismiss()
                                }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option.first == selectedValue),
                            onClick = {
                                onSelect(option.first)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFA243C), unselectedColor = Color.Gray)
                        )
                        Text(
                            text = option.second,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFFFA243C))
            }
        },
        containerColor = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun SliderDialog(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    isFloat: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    var tempValue by remember { mutableFloatStateOf(value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFloat) String.format("%.2f", tempValue) else tempValue.roundToInt().toString(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = tempValue,
                    onValueChange = { tempValue = it },
                    valueRange = valueRange,
                    steps = steps,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFFA243C),
                        inactiveTrackColor = Color.DarkGray
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(tempValue)
                onDismiss()
            }) {
                Text("Guardar", color = Color(0xFFFA243C))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun InputDialog(
    title: String,
    placeholder: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textState by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                placeholder = { Text(placeholder, color = Color.DarkGray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFA243C),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(textState.trim())
                onDismiss()
            }) {
                Text("Aceptar", color = Color(0xFFFA243C))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = Color.Gray, fontSize = 16.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirmar", color = Color(0xFFFA243C))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun SpotifySettingsScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isLoggedIn by com.mrtdk.liquid_glass.spotify.SpotifySession.isLoggedIn.collectAsState()
    var showLoginDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && com.mrtdk.liquid_glass.spotify.SpotifySession.userName.isBlank()) {
            val user = com.mrtdk.liquid_glass.spotify.Spotify.me().getOrNull()
            if (user != null && !user.displayName.isNullOrBlank()) {
                com.mrtdk.liquid_glass.spotify.SpotifySession.saveSession(
                    com.mrtdk.liquid_glass.spotify.SpotifySession.spDc,
                    com.mrtdk.liquid_glass.spotify.SpotifyInternalToken(
                        accessToken = com.mrtdk.liquid_glass.spotify.SpotifySession.accessToken,
                        accessTokenExpirationTimestampMs = com.mrtdk.liquid_glass.spotify.SpotifySession.tokenExpirationMs
                    ),
                    displayName = user.displayName,
                    uid = user.id
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.mrtdk.liquid_glass.ui.theme.ThemeManager.backgroundColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    tint = Color(0xFFFA243C)
                )
            }
            Text(
                text = "Spotify",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_account_link),
                items = listOf(
                    Material3SettingsItem(
                        icon = rememberPainter(Icons.Default.AccountCircle),
                        title = { Text(stringResource(R.string.spotify_account_status)) },
                        description = {
                            Text(
                                if (isLoggedIn) stringResource(R.string.spotify_connected_as, com.mrtdk.liquid_glass.spotify.SpotifySession.userName.ifBlank { com.mrtdk.liquid_glass.spotify.SpotifySession.userId })
                                else stringResource(R.string.spotify_not_connected)
                            )
                        },
                        onClick = {
                            if (!isLoggedIn) {
                                showLoginDialog = true
                            }
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoggedIn) {
                Material3SettingsGroup(
                    title = stringResource(R.string.settings_section_spotify_actions),
                    items = listOf(
                        Material3SettingsItem(
                            icon = rememberPainter(Icons.Default.Sync),
                            title = { Text(if (isSyncing) stringResource(R.string.spotify_syncing_playlists) else stringResource(R.string.spotify_sync_playlists_now)) },
                            description = { Text(stringResource(R.string.spotify_sync_desc)) },
                            onClick = {
                                if (!isSyncing) {
                                    isSyncing = true
                                    scope.launch {
                                        LibraryManager.syncSpotifyPlaylists()
                                        isSyncing = false
                                        Toast.makeText(context, context.getString(R.string.spotify_synced_toast), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ),
                        Material3SettingsItem(
                            icon = rememberPainter(Icons.Default.Logout),
                            title = { Text(stringResource(R.string.spotify_logout)) },
                            description = { Text(stringResource(R.string.spotify_logout_desc)) },
                            onClick = {
                                com.mrtdk.liquid_glass.spotify.SpotifySession.logout()
                                Toast.makeText(context, context.getString(R.string.spotify_logged_out_toast), Toast.LENGTH_SHORT).show()
                            }
                        )
                    )
                )
            } else {
                Button(
                    onClick = { showLoginDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.spotify_login_btn), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showLoginDialog) {
        com.mrtdk.liquid_glass.ui.components.SpotifyLoginDialog(
            onDismiss = { showLoginDialog = false },
            onSuccess = {
                showLoginDialog = false
                Toast.makeText(context, context.getString(R.string.spotify_login_success_toast), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun rememberPainter(imageVector: androidx.compose.ui.graphics.vector.ImageVector): Painter {
    return androidx.compose.ui.graphics.vector.rememberVectorPainter(imageVector)
}
