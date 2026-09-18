package com.kyant.backdrop.shadow

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import expo.modules.androidglassview.backdrop.shadow.shadow as expoShadow
import expo.modules.androidglassview.backdrop.shadow.innerShadow as expoInnerShadow

typealias Shadow = expo.modules.androidglassview.backdrop.shadow.Shadow
typealias InnerShadow = expo.modules.androidglassview.backdrop.shadow.InnerShadow

fun Modifier.shadow(
    shape: () -> Shape,
    shadow: () -> Shadow? = { Shadow.Default }
): Modifier = this.expoShadow(shape, shadow)

fun Modifier.innerShadow(
    shape: () -> Shape,
    shadow: () -> InnerShadow? = { InnerShadow.Default }
): Modifier = this.expoInnerShadow(shape, shadow)
