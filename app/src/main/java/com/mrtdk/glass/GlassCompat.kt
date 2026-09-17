package com.mrtdk.glass

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import expo.modules.androidglassview.GlassBox

val LocalGlassStyle = expo.modules.androidglassview.LocalGlassStyle
val LocalLightweightGlass = expo.modules.androidglassview.LocalLightweightGlass

typealias GlassScope = expo.modules.androidglassview.GlassScope
typealias GlassBoxScope = expo.modules.androidglassview.GlassBoxScope

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
    (this as expo.modules.androidglassview.GlassBoxScope).GlassBox(
        modifier = modifier,
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints,
        scale = scale,
        blur = blur,
        centerDistortion = centerDistortion,
        shape = shape,
        elevation = elevation,
        tint = tint,
        darkness = darkness,
        warpEdges = warpEdges,
        content = content
    )
}

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    useShader: Boolean = true,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    expo.modules.androidglassview.GlassContainer(
        modifier = modifier,
        useShader = useShader,
        content = content,
        glassContent = glassContent
    )
}
