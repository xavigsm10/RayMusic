package com.mrtdk.liquid_glass.ui.components.floatingtabbar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

private const val ThresholdSlope = 10f
private const val ThresholdMidpoint255 = 0.5f * 255f

private val ThresholdColorFilter = ColorMatrixColorFilter(
    ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, ThresholdSlope, -ThresholdSlope * ThresholdMidpoint255
        )
    )
)

private val gooeyEffectCache = HashMap<Int, RenderEffect>()

@RequiresApi(Build.VERSION_CODES.S)
private fun createGooeyEffect(blurRadiusPx: Float): RenderEffect? {
    val key = blurRadiusPx.toInt()
    if (key <= 0) return null
    return gooeyEffectCache.getOrPut(key) {
        val androidBlur = android.graphics.RenderEffect.createBlurEffect(
            key.toFloat(),
            key.toFloat(),
            android.graphics.Shader.TileMode.DECAL
        )
        val androidColorFilter = ThresholdColorFilter.asAndroidColorFilter()
        val chain = android.graphics.RenderEffect.createColorFilterEffect(
            androidColorFilter,
            androidBlur
        )
        chain.asComposeRenderEffect()
    }
}

/**
 * Composites this element into one offscreen layer and gooifies it: blurs by
 * [blurRadiusPx], then snaps the blur back to a crisp edge via an alpha
 * threshold.
 */
fun Modifier.gooey(blurRadiusPx: () -> Float): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    return this.graphicsLayer {
        val radius = blurRadiusPx()
        if (radius > 0f) {
            val effect = createGooeyEffect(radius)
            if (effect != null) {
                compositingStrategy = CompositingStrategy.Offscreen
                renderEffect = effect
            }
        } else {
            compositingStrategy = CompositingStrategy.Auto
            renderEffect = null
        }
    }
}
