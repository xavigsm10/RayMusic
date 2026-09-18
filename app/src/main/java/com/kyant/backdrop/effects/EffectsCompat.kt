package com.kyant.backdrop.effects

import androidx.annotation.FloatRange
import expo.modules.androidglassview.backdrop.BackdropEffectScope
import expo.modules.androidglassview.backdrop.effects.blur as expoBlur
import expo.modules.androidglassview.backdrop.effects.lens as expoLens
import expo.modules.androidglassview.backdrop.effects.vibrancy as expoVibrancy

fun BackdropEffectScope.blur(radius: Float) = expoBlur(radius)

fun BackdropEffectScope.lens(
    @FloatRange(from = 0.0) refractionHeight: Float,
    @FloatRange(from = 0.0) refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false
) = expoLens(refractionHeight, refractionAmount, depthEffect, chromaticAberration)

fun BackdropEffectScope.vibrancy() = expoVibrancy()
