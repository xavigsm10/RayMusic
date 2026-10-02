package com.mrtdk.liquid_glass.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.mrtdk.liquid_glass.ui.theme.ThemeManager

/**
 * Scrim/dimming color from AndroidLiquidGlass DialogContent.
 */
@Composable
fun rememberAndroidLiquidGlassDimColor(
    isDark: Boolean = ThemeManager.isDarkMode.collectAsState().value
): Color {
    val isLightTheme = !isDark
    return if (isLightTheme) Color(0xFF29293A).copy(alpha = 0.23f)
    else Color(0xFF121212).copy(alpha = 0.56f)
}

/**
 * Exact backdrop effect from AndroidLiquidGlass (DialogContent):
 * - colorControls: brightness (0.2f light / 0f dark), saturation 1.5f
 * - blur: 16dp light / 8dp dark
 * - lens: refractionHeight, refractionAmount, depthEffect = true
 * - highlight: Highlight.Plain
 * - onDrawSurface: containerColor (0xFFFAFAFA 60% light / 0xFF121212 40% dark)
 */
fun Modifier.androidLiquidGlassEffect(
    backdrop: Backdrop,
    shape: () -> Shape,
    isDark: Boolean = true,
    containerColor: Color? = null,
    refractionHeight: Dp = 24.dp,
    refractionAmount: Dp = 48.dp,
    depthEffect: Boolean = true
): Modifier {
    val isLightTheme = !isDark
    val defaultContainer = if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = 0.6f)
    else Color(0xFF121212).copy(alpha = 0.4f)
    val surfaceColor = containerColor ?: defaultContainer

    return this.drawBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = {
            colorControls(
                brightness = if (isLightTheme) 0.2f else 0f,
                saturation = 1.5f
            )
            blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
            lens(
                refractionHeight = refractionHeight.toPx(),
                refractionAmount = refractionAmount.toPx(),
                depthEffect = depthEffect
            )
        },
        highlight = { Highlight.Plain },
        onDrawSurface = { drawRect(surfaceColor) }
    )
}

fun Modifier.androidLiquidGlassEffect(
    shape: Shape,
    backdrop: Backdrop,
    isDark: Boolean = true,
    containerColor: Color? = null,
    refractionHeight: Dp = 24.dp,
    refractionAmount: Dp = 48.dp,
    depthEffect: Boolean = true
): Modifier = androidLiquidGlassEffect(
    backdrop = backdrop,
    shape = { shape },
    isDark = isDark,
    containerColor = containerColor,
    refractionHeight = refractionHeight,
    refractionAmount = refractionAmount,
    depthEffect = depthEffect
)
