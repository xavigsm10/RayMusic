package expo.modules.androidglassview

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import expo.modules.androidglassview.backdrop.Backdrop
import expo.modules.androidglassview.backdrop.backdrops.emptyBackdrop
import expo.modules.androidglassview.backdrop.backdrops.layerBackdrop
import expo.modules.androidglassview.backdrop.backdrops.rememberLayerBackdrop
import expo.modules.androidglassview.backdrop.drawBackdrop
import expo.modules.androidglassview.backdrop.effects.blur
import expo.modules.androidglassview.backdrop.effects.colorControls
import expo.modules.androidglassview.backdrop.effects.lens
import expo.modules.androidglassview.backdrop.highlight.Highlight
import expo.modules.androidglassview.backdrop.isRenderEffectSupported
import expo.modules.androidglassview.backdrop.shadow.Shadow
import expo.modules.androidglassview.components.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

val LocalGlassStyle = staticCompositionLocalOf { "transparent" }
val LocalLightweightGlass = staticCompositionLocalOf { false }
val LocalBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

/**
 * Default untinted glass tint matching the Frosted variant from expo-android-glass-view.
 */
val DarkGrayGlassTint = Color.Unspecified

interface GlassScope {
    fun Modifier.glassBackground(
        shape: CornerBasedShape,
        elevation: Dp = 0.dp,
        tint: Color = Color.Unspecified,
        blur: Float = 0.8f,
    ): Modifier
}

interface GlassBoxScope : BoxScope, GlassScope

private class GlassBoxScopeImpl(
    val boxScope: BoxScope,
    val glassScope: GlassScope
) : GlassBoxScope, BoxScope by boxScope, GlassScope by glassScope

private class GlassScopeImpl : GlassScope {
    override fun Modifier.glassBackground(
        shape: CornerBasedShape,
        elevation: Dp,
        tint: Color,
        blur: Float,
    ): Modifier {
        val baseTint = if (tint.isSpecified) tint else Color.White.copy(alpha = 0.08f)
        val alpha = baseTint.alpha
        return this
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        baseTint.copy(alpha = (alpha * 0.95f).coerceIn(0f, 1f)),
                        baseTint.copy(alpha = (alpha * 0.75f).coerceIn(0f, 1f)),
                        baseTint
                    )
                ),
                shape = shape
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = shape
            )
    }
}

/**
 * Dedicated Glass Pill for Back and Share buttons using the "Frosted" variant from expo-android-glass-view.
 */
@Composable
fun ExpoGlassPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    tint: Color = Color.Unspecified,
    dark: Boolean = false,
    isInteractive: Boolean = true,
    backdrop: Backdrop = LocalBackdrop.current,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val glassStyle = LocalGlassStyle.current
    val isLightweight = LocalLightweightGlass.current
    val isSolid = glassStyle == "solid" || com.mrtdk.liquid_glass.BuildConfig.IS_LITE
    val interactionSource = remember { MutableInteractionSource() }
    val animationScope = rememberCoroutineScope()
    val effectsSupported = isRenderEffectSupported()

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val resolvedTint = if (tint == DarkGrayGlassTint) Color.Unspecified else tint

    val state = remember(resolvedTint) {
        GlassState().apply {
            this.themed = false
            this.dark = dark
            this.tintColor = resolvedTint
            this.heightOverride = 2f
            this.amountOverride = 3f
            this.blurOverride = 12f
            this.chromaticAberration = false // Pure neutral refraction without color fringes
            this.depthEffect = true
            this.highlight = true
            this.shadow = true
            this.vibrancyOverride = true
        }
    }

    if (isSolid) {
        val isDark = com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState().value
        val baseBg = if (isDark) Color(0xFF26272E) else Color(0xFFE8E9F0)
        val pillBg = if (resolvedTint.isSpecified) {
            if (resolvedTint.alpha < 1f) resolvedTint.compositeOver(baseBg) else resolvedTint
        } else baseBg
        val pillBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
        Box(
            modifier = modifier
                .clip(shape)
                .background(pillBg, shape)
                .border(1.dp, pillBorder, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = if (isDark) Color.White else Color.Black),
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    } else {
        val isDark = com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState().value
        val isLightTheme = !isDark
        val containerColor = if (resolvedTint.isSpecified) {
            resolvedTint
        } else if (isLightTheme) {
            Color(0xFFFAFAFA).copy(alpha = 0.6f)
        } else {
            Color(0xFF121212).copy(alpha = 0.4f)
        }

        Box(
            modifier = modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        colorControls(
                            brightness = if (isLightTheme) 0.2f else 0f,
                            saturation = 1.5f
                        )
                        blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
                        lens(
                            refractionHeight = 16f.dp.toPx(),
                            refractionAmount = 32f.dp.toPx(),
                            depthEffect = true
                        )
                    },
                    highlight = { Highlight.Plain },
                    shadow = { Shadow(radius = 16.dp, color = Color.Black.copy(alpha = 0.25f)) },
                    layerBlock = if (isInteractive) {
                        {
                            val width = size.width
                            val height = size.height
                            val progress = interactiveHighlight.pressProgress
                            val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                            val maxOffset = size.minDimension
                            val initialDerivative = 0.05f
                            val offset = interactiveHighlight.offset
                            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                            val maxDragScale = 4f.dp.toPx() / size.height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                            scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                        }
                    } else null,
                    onDrawSurface = { drawRect(containerColor) }
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = Color.White),
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * Dedicated Glass Menu Card for 3-dots menus using the "Frosted" variant from expo-android-glass-view.
 */
@Composable
fun ExpoGlassMenuCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(24.dp),
    dark: Boolean = false,
    tint: Color = Color.Unspecified,
    backdrop: Backdrop = LocalBackdrop.current,
    content: @Composable BoxScope.() -> Unit
) {
    val glassStyle = LocalGlassStyle.current
    val isLightweight = LocalLightweightGlass.current
    val isSolid = glassStyle == "solid" || com.mrtdk.liquid_glass.BuildConfig.IS_LITE
    val effectsSupported = isRenderEffectSupported()

    val resolvedTint = if (tint == DarkGrayGlassTint) Color.Unspecified else tint

    val state = remember(resolvedTint) {
        GlassState().apply {
            this.themed = false
            this.dark = dark
            this.tintColor = resolvedTint
            this.heightOverride = 2f
            this.amountOverride = 3f
            this.blurOverride = 12f
            this.chromaticAberration = false // Pure neutral refraction without color fringes
            this.depthEffect = true
            this.highlight = true
            this.shadow = true
            this.vibrancyOverride = true
        }
    }

    if (isSolid) {
        val isDark = com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState().value
        val baseBg = if (isDark) Color(0xFF22232A) else Color(0xFFFFFFFF)
        val menuBg = if (resolvedTint.isSpecified) {
            if (resolvedTint.alpha < 1f) resolvedTint.compositeOver(baseBg) else resolvedTint
        } else baseBg
        val menuBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
        Box(
            modifier = modifier
                .shadow(12.dp, shape)
                .clip(shape)
                .background(menuBg, shape)
                .border(1.dp, menuBorder, shape),
            content = content
        )
    } else {
        val isDark = com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState().value
        val isLightTheme = !isDark
        val containerColor = if (resolvedTint.isSpecified) {
            resolvedTint
        } else if (isLightTheme) {
            Color(0xFFFAFAFA).copy(alpha = 0.6f)
        } else {
            Color(0xFF121212).copy(alpha = 0.4f)
        }

        Box(
            modifier = modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        colorControls(
                            brightness = if (isLightTheme) 0.2f else 0f,
                            saturation = 1.5f
                        )
                        blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
                        lens(
                            refractionHeight = 24f.dp.toPx(),
                            refractionAmount = 48f.dp.toPx(),
                            depthEffect = true
                        )
                    },
                    highlight = { Highlight.Plain },
                    shadow = { Shadow(radius = 16.dp, color = Color.Black.copy(alpha = 0.25f)) },
                    onDrawSurface = { drawRect(containerColor) }
                ),
            content = content
        )
    }
}

/**
 * Standard GlassBox implementation using the exact AndroidLiquidGlass Dialog effect.
 */
@Composable
fun GlassBoxScope.GlassBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    @FloatRange(from = 0.0, to = 1.0)
    scale: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    blur: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    centerDistortion: Float = 0f,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    elevation: Dp = 0.dp,
    tint: Color = Color.Unspecified,
    @FloatRange(from = 0.0, to = 1.0)
    darkness: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    warpEdges: Float = 0f,
    backdrop: Backdrop = LocalBackdrop.current,
    depthEffect: Boolean = true,
    content: @Composable BoxScope.() -> Unit = { },
) {
    val glassStyle = LocalGlassStyle.current
    val isSolid = glassStyle == "solid" || com.mrtdk.liquid_glass.BuildConfig.IS_LITE

    val resolvedTint = if (tint == DarkGrayGlassTint) Color.Unspecified else tint

    if (isSolid) {
        val isDark = com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState().value
        val baseBg = if (isDark) Color(0xFF22232A) else Color(0xFFF1F2F6)
        val boxBg = if (resolvedTint.isSpecified) {
            if (resolvedTint.alpha < 1f) resolvedTint.compositeOver(baseBg) else resolvedTint
        } else baseBg
        val boxBorder = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.06f)
        Box(
            modifier = modifier
                .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape) else Modifier)
                .clip(shape)
                .background(boxBg, shape)
                .border(1.dp, boxBorder, shape),
            contentAlignment = contentAlignment,
            propagateMinConstraints = propagateMinConstraints,
            content = content
        )
    } else {
        val isDark = com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState().value
        val isLightTheme = !isDark
        val containerColor = if (resolvedTint.isSpecified) {
            resolvedTint
        } else if (isLightTheme) {
            Color(0xFFFAFAFA).copy(alpha = 0.6f)
        } else {
            Color(0xFF121212).copy(alpha = 0.4f)
        }

        Box(
            modifier = modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        colorControls(
                            brightness = if (isLightTheme) 0.2f else 0f,
                            saturation = 1.5f
                        )
                        blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
                        lens(
                            refractionHeight = 24f.dp.toPx(),
                            refractionAmount = 48f.dp.toPx(),
                            depthEffect = depthEffect
                        )
                    },
                    highlight = { Highlight.Plain },
                    shadow = {
                        val shadowRadius = if (elevation > 0.dp) elevation else 16.dp
                        Shadow(radius = shadowRadius, color = Color.Black.copy(alpha = 0.25f))
                    },
                    onDrawSurface = { drawRect(containerColor) }
                ),
            contentAlignment = contentAlignment,
            propagateMinConstraints = propagateMinConstraints,
            content = content
        )
    }
}

/**
 * Root container for glass content. Captures background content into layerBackdrop
 * and provides LocalBackdrop to glassContent.
 */
@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    useShader: Boolean = true,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val isSolid = com.mrtdk.liquid_glass.BuildConfig.IS_LITE ||
            com.mrtdk.glass.LocalGlassStyle.current == "solid" ||
            LocalGlassStyle.current == "solid"

    val glassScope = remember { GlassScopeImpl() }

    if (isSolid) {
        val emptyBackdrop = remember { emptyBackdrop() }
        Box(modifier = modifier) {
            content()
            CompositionLocalProvider(LocalBackdrop provides emptyBackdrop) {
                val boxScopeImpl = remember(glassScope) {
                    GlassBoxScopeImpl(this, glassScope)
                }
                boxScopeImpl.glassContent()
            }
        }
        return
    }

    val backdrop = rememberLayerBackdrop()

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(backdrop)
        ) {
            content()
        }
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            val boxScopeImpl = remember(glassScope) {
                GlassBoxScopeImpl(this, glassScope)
            }
            boxScopeImpl.glassContent()
        }
    }
}

