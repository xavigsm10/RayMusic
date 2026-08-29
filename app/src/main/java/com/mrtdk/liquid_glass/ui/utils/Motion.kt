package com.mrtdk.liquid_glass.ui.utils

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object Motion {
    const val MorphStiffness = 950f

    fun <T> morph(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = MorphStiffness,
    )

    const val MorphEnterMillis = 320
    val MorphEnterEasing: Easing = FastOutSlowInEasing
    fun <T> morphEnter(): FiniteAnimationSpec<T> = tween(MorphEnterMillis, easing = MorphEnterEasing)

    const val MorphExitMillis = 240
    val MorphExitEasing: Easing = FastOutSlowInEasing
    fun <T> morphExit(): FiniteAnimationSpec<T> = tween(MorphExitMillis, easing = MorphExitEasing)

    const val SelectStiffness = 750f
    fun <T> select(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = SelectStiffness,
    )

    const val PressStiffness = 1220f
    fun <T> press(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = PressStiffness,
    )

    const val AppearStiffness = 450f
    fun <T> appear(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = AppearStiffness,
    )

    const val AppearExitMillis = 240
    fun <T> appearExit(): FiniteAnimationSpec<T> = tween(AppearExitMillis, easing = MorphExitEasing)

    const val PushMillis = 260
    val PushEasing: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    fun <T> push(): FiniteAnimationSpec<T> = tween(PushMillis, easing = PushEasing)

    const val PushParallax = 0.30f
    const val PushDimAlpha = 0.85f
    fun parallaxOffset(fullWidth: Int): Int = -(fullWidth * PushParallax).toInt()
}
