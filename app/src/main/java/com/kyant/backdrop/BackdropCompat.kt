package com.kyant.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import expo.modules.androidglassview.backdrop.Backdrop as ExpoBackdrop
import expo.modules.androidglassview.backdrop.BackdropEffectScope as ExpoBackdropEffectScope
import expo.modules.androidglassview.backdrop.backdrops.LayerBackdrop as ExpoLayerBackdrop
import expo.modules.androidglassview.backdrop.drawBackdrop as expoDrawBackdrop
import expo.modules.androidglassview.backdrop.backdrops.layerBackdrop as expoLayerBackdrop
import expo.modules.androidglassview.backdrop.backdrops.rememberLayerBackdrop as expoRememberLayerBackdrop
import expo.modules.androidglassview.backdrop.effects.blur as expoBlur
import expo.modules.androidglassview.backdrop.effects.lens as expoLens
import expo.modules.androidglassview.backdrop.effects.vibrancy as expoVibrancy
import expo.modules.androidglassview.backdrop.highlight.Highlight as ExpoHighlight
import expo.modules.androidglassview.backdrop.shadow.Shadow as ExpoShadow
import expo.modules.androidglassview.backdrop.shadow.InnerShadow as ExpoInnerShadow

typealias Backdrop = ExpoBackdrop
typealias BackdropEffectScope = ExpoBackdropEffectScope
typealias LayerBackdrop = ExpoLayerBackdrop

fun Modifier.drawBackdrop(
    backdrop: ExpoBackdrop,
    shape: () -> Shape,
    effects: ExpoBackdropEffectScope.() -> Unit,
    highlight: (() -> ExpoHighlight?)? = { ExpoHighlight.Default },
    shadow: (() -> ExpoShadow?)? = { ExpoShadow.Default },
    innerShadow: (() -> ExpoInnerShadow?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: ExpoLayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = { it() },
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null,
    clipToShape: Boolean = true,
    backdropScale: Float = 1f
): Modifier = expoDrawBackdrop(
    backdrop = backdrop,
    shape = shape,
    effects = effects,
    highlight = highlight,
    shadow = shadow,
    innerShadow = innerShadow,
    layerBlock = layerBlock,
    exportedBackdrop = exportedBackdrop,
    onDrawBehind = onDrawBehind,
    onDrawBackdrop = onDrawBackdrop,
    onDrawSurface = onDrawSurface,
    onDrawFront = onDrawFront,
    clipToShape = clipToShape,
    backdropScale = backdropScale
)
