package com.kyant.backdrop.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import expo.modules.androidglassview.backdrop.Backdrop
import expo.modules.androidglassview.backdrop.backdrops.layerBackdrop as expoLayerBackdrop
import expo.modules.androidglassview.backdrop.backdrops.rememberLayerBackdrop as expoRememberLayerBackdrop
import expo.modules.androidglassview.backdrop.backdrops.rememberCombinedBackdrop as expoRememberCombinedBackdrop
import expo.modules.androidglassview.backdrop.backdrops.emptyBackdrop as expoEmptyBackdrop

typealias LayerBackdrop = expo.modules.androidglassview.backdrop.backdrops.LayerBackdrop

fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier =
    expoLayerBackdrop(backdrop)

@Composable
fun rememberLayerBackdrop(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    onDraw: ContentDrawScope.() -> Unit = { drawContent() }
): LayerBackdrop = expoRememberLayerBackdrop(graphicsLayer, onDraw)

@Composable
fun rememberCombinedBackdrop(backdrop1: Backdrop, backdrop2: Backdrop): Backdrop =
    expoRememberCombinedBackdrop(backdrop1, backdrop2)

@Composable
fun rememberCombinedBackdrop(backdrop1: Backdrop, backdrop2: Backdrop, backdrop3: Backdrop): Backdrop =
    expoRememberCombinedBackdrop(backdrop1, backdrop2, backdrop3)

@Composable
fun rememberCombinedBackdrop(vararg backdrops: Backdrop): Backdrop =
    expoRememberCombinedBackdrop(*backdrops)

fun emptyBackdrop(): Backdrop = expoEmptyBackdrop()
