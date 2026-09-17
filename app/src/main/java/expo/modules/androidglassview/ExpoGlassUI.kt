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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import expo.modules.androidglassview.backdrop.Backdrop
import expo.modules.androidglassview.backdrop.backdrops.emptyBackdrop
import expo.modules.androidglassview.backdrop.drawBackdrop
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

interface GlassScope {
    fun Modifier.glassBackground(
        shape: CornerBasedShape,
        elevation: Dp = 0.dp,
        tint: Color = Color.Transparent,
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
    ): Modifier = this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                listOf(
                    tint.copy(alpha = (tint.alpha * 0.95f).coerceIn(0f, 1f)),
                    tint.copy(alpha = (tint.alpha * 0.75f).coerceIn(0f, 1f)),
                    tint
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

/**
 * Dedicated Glass Pill for Back and Share buttons using expo-android-glass-view.
 */
@Composable
fun ExpoGlassPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    tint: Color = Color.White.copy(alpha = 0.15f),
    dark: Boolean = true,
    isInteractive: Boolean = true,
    backdrop: Backdrop = LocalBackdrop.current,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val glassStyle = LocalGlassStyle.current
    val isLightweight = LocalLightweightGlass.current
    val isSolid = glassStyle == "solid"
    val interactionSource = remember { MutableInteractionSource() }
    val animationScope = rememberCoroutineScope()
    val effectsSupported = isRenderEffectSupported()

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val state = remember(dark, tint) {
        GlassState().apply {
            this.dark = dark
            this.themed = true
            this.tintColor = tint
            this.chromaticAberration = true
            this.depthEffect = true
            this.highlight = true
            this.shadow = true
        }
    }

    if (isSolid) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(Color(0xFF28282C), shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = Color.White),
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    } else {
        Box(
            modifier = modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        if (!isLightweight) {
                            glassEffects(state)
                        } else {
                            glassEffects(state, effectScale = 1.5f)
                        }
                    },
                    highlight = { if (state.highlight) state.rim else null },
                    shadow = { if (state.shadow) state.dropShadow else null },
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
                    onDrawSurface = { glassSurface(state, effectsSupported) }
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
 * Dedicated Glass Menu Card for 3-dots menus using expo-android-glass-view.
 */
@Composable
fun ExpoGlassMenuCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(24.dp),
    dark: Boolean = true,
    tint: Color = Color(0xFF1E1E1E).copy(alpha = 0.85f),
    backdrop: Backdrop = LocalBackdrop.current,
    content: @Composable BoxScope.() -> Unit
) {
    val glassStyle = LocalGlassStyle.current
    val isLightweight = LocalLightweightGlass.current
    val isSolid = glassStyle == "solid"
    val effectsSupported = isRenderEffectSupported()

    val state = remember(dark, tint) {
        GlassState().apply {
            this.dark = dark
            this.themed = true
            this.tintColor = tint
            this.chromaticAberration = true
            this.depthEffect = true
            this.highlight = true
            this.shadow = true
        }
    }

    if (isSolid) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(Color(0xFF1E1E1E), shape),
            content = content
        )
    } else {
        Box(
            modifier = modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        if (!isLightweight) {
                            glassEffects(state)
                        } else {
                            glassEffects(state, effectScale = 1.5f)
                        }
                    },
                    highlight = { Highlight(width = 0.8.dp, alpha = 0.45f) },
                    shadow = { Shadow(radius = 16.dp, color = Color.Black.copy(alpha = 0.25f)) },
                    onDrawSurface = { glassSurface(state, effectsSupported) }
                ),
            content = content
        )
    }
}

/**
 * Standard GlassBox implementation for screens and morphing pills.
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
    tint: Color = Color.Transparent,
    @FloatRange(from = 0.0, to = 1.0)
    darkness: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    warpEdges: Float = 0f,
    content: @Composable BoxScope.() -> Unit = { },
) {
    val glassStyle = LocalGlassStyle.current
    val isSolid = glassStyle == "solid"
    val isLightweight = LocalLightweightGlass.current
    val effectsSupported = isRenderEffectSupported()
    val backdrop = LocalBackdrop.current

    val state = remember(tint, blur) {
        GlassState().apply {
            this.dark = true
            this.themed = true
            this.tintColor = tint
            this.chromaticAberration = true
            this.depthEffect = true
            this.highlight = true
            this.shadow = elevation > 0.dp
        }
    }

    if (isSolid) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(if (tint != Color.Transparent) tint else Color(0xFF242428), shape),
            contentAlignment = contentAlignment,
            propagateMinConstraints = propagateMinConstraints,
            content = content
        )
    } else {
        Box(
            modifier = modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        if (!isLightweight) {
                            glassEffects(state)
                        } else {
                            glassEffects(state, effectScale = 1.5f)
                        }
                    },
                    highlight = { Highlight(width = 0.75.dp, alpha = 0.4f) },
                    shadow = { if (elevation > 0.dp) Shadow(radius = elevation, color = Color.Black.copy(alpha = 0.2f)) else null },
                    onDrawSurface = { glassSurface(state, effectsSupported) }
                ),
            contentAlignment = contentAlignment,
            propagateMinConstraints = propagateMinConstraints,
            content = content
        )
    }
}

/**
 * Root container for glass content.
 */
@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    useShader: Boolean = true,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val glassScope = remember { GlassScopeImpl() }
    Box(modifier = modifier) {
        Box(modifier = Modifier.matchParentSize()) {
            content()
        }
        val boxScopeImpl = remember(glassScope) {
            GlassBoxScopeImpl(this, glassScope)
        }
        boxScopeImpl.glassContent()
    }
}
