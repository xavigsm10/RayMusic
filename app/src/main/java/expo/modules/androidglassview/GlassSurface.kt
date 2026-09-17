package expo.modules.androidglassview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import expo.modules.androidglassview.backdrop.effects.colorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import expo.modules.androidglassview.backdrop.Backdrop
import expo.modules.androidglassview.backdrop.BackdropEffectScope
import expo.modules.androidglassview.backdrop.drawBackdrop
import expo.modules.androidglassview.backdrop.effects.linearBlur
import expo.modules.androidglassview.backdrop.effects.blur
import expo.modules.androidglassview.backdrop.effects.lens
import expo.modules.androidglassview.backdrop.effects.vibrancy
import expo.modules.androidglassview.backdrop.highlight.Highlight
import expo.modules.androidglassview.backdrop.isRenderEffectSupported
import expo.modules.androidglassview.backdrop.shadow.Shadow

/** Glass parameters set from React props. Lengths are in dp, like React Native styles. */
internal class GlassState {
  var cornerRadius by mutableFloatStateOf(DEFAULT_CORNER_RADIUS)
  var themed by mutableStateOf(true)
  var dark by mutableStateOf(false)
  var blurGradient by mutableStateOf(false)
  var blurOverride by mutableStateOf<Float?>(null)
  var heightOverride by mutableStateOf<Float?>(null)
  var amountOverride by mutableStateOf<Float?>(null)
  var fallbackOverride by mutableStateOf<Color?>(null)
  val blurRadius get() = blurOverride ?: if (themed) DEFAULT_BLUR_RADIUS else 2f
  val refractionHeight get() = heightOverride ?: if (themed) DEFAULT_REFRACTION_HEIGHT else 12f
  val refractionAmount get() = amountOverride ?: if (themed) DEFAULT_REFRACTION_AMOUNT else 24f
  val fallbackColor get() = fallbackOverride ?: if (dark) Color(0xFF242B33) else DEFAULT_FALLBACK_COLOR
  val rim get() = if (!themed) Highlight.Default else if (dark) Highlight(width = .6f.dp, alpha = .38f) else Highlight(width = .6f.dp, alpha = .5f)
  val dropShadow get() = if (!themed) Shadow.Default else if (dark) Shadow(radius = 12.dp, color = Color.Black.copy(alpha = .16f)) else Shadow(radius = 12.dp, color = Color.Black.copy(alpha = .12f))
  var chromaticAberration by mutableStateOf(false)
  var depthEffect by mutableStateOf(false)
  var vibrancyOverride by mutableStateOf<Boolean?>(null)
  val vibrancy get() = vibrancyOverride ?: !themed
  var highlight by mutableStateOf(true)
  var shadow by mutableStateOf(true)
  var tintColor by mutableStateOf(Color.Unspecified)
  var surfaceColor by mutableStateOf(Color.Unspecified)

  companion object {
    // Resting material calibrated visually against the supplied iOS light/dark recordings.
    const val DEFAULT_CORNER_RADIUS = 24f
    const val DEFAULT_BLUR_RADIUS = 9f
    const val DEFAULT_REFRACTION_HEIGHT = 2f
    const val DEFAULT_REFRACTION_AMOUNT = 3f
    val DEFAULT_FALLBACK_COLOR = Color(0xB3FFFFFF)
  }
}

/** Vibrancy, blur and lens as configured by [state]. */
internal fun BackdropEffectScope.glassEffects(
  state: GlassState,
  effectScale: Float = 1f,
  material: Float = 1f,
  press: Float = 0f
) {
  // At rest every component uses exactly the same material. The menu can interpolate
  // from its small source bubble without changing its established motion timeline.
  val progress = ((material - .65f) / .30f).coerceIn(0f, 1f)
  val settled = progress * progress * (3f - 2f * progress)
  val blurRadius = .65f + (state.blurRadius - .65f) * settled
  if (!state.dark && state.vibrancy) vibrancy()
  if (blurRadius > 0f) {
    val radius = (blurRadius / effectScale).dp.toPx()
    if (state.blurGradient) linearBlur(radius) else blur(radius)
  }
  glassAppearance(state, material, press)
  if (state.dark && state.vibrancyOverride == true) vibrancy()
  val height = state.refractionHeight + (9f - state.refractionHeight) * (1f - material)
  val amount = state.refractionAmount + (16f - state.refractionAmount) * (1f - material)
  if (height > 0f && amount > 0f) lens(
    (height / effectScale).dp.toPx(), (amount / effectScale).dp.toPx(),
    depthEffect = state.depthEffect, chromaticAberration = state.chromaticAberration
  )
}

/** Colour response tuned against the supplied light/dark iOS recordings. */
internal fun BackdropEffectScope.glassAppearance(
  state: GlassState, material: Float = 1f, press: Float = 0f, strength: Float = 1f
) {
  if (!state.themed) return
  val rg = if (state.dark) .27f - .136f * material + .055f * press else .28f
  val b = if (state.dark) .28f - .093f * material + .065f * press else .28f
  val rOffset = if (state.dark) 14f + 9.53f * material else 250f * .72f
  val gOffset = if (state.dark) 16f + 11.79f * material else 250f * .72f
  val bOffset = if (state.dark) 18f + 8.13f * material else 252f * .72f
  val rGain = 1f + (rg - 1f) * strength
  val bGain = 1f + (b - 1f) * strength
  colorFilter(ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
    rGain,0f,0f,0f,rOffset * strength,
    0f,rGain,0f,0f,gOffset * strength,
    0f,0f,bGain,0f,bOffset * strength,
    0f,0f,0f,1f,0f
  ))))
}

/** Fallback, tint and surface colours painted over the backdrop, as configured by [state]. */
internal fun DrawScope.glassSurface(state: GlassState, effectsSupported: Boolean) {
  if (!effectsSupported) drawRect(state.fallbackColor)
  val tint = state.tintColor
  if (tint.isSpecified) {
    drawRect(tint, blendMode = BlendMode.Hue)
    drawRect(tint.copy(alpha = 0.75f * tint.alpha))
  }
  val surface = state.surfaceColor
  if (surface.isSpecified) drawRect(surface)
}

@Composable
internal fun GlassSurface(state: GlassState, backdrop: Backdrop) {
  val effectsSupported = isRenderEffectSupported()
  Box(
    Modifier
      .fillMaxSize()
      .drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedCornerShape(state.cornerRadius.dp) },
        effects = { glassEffects(state) },
        highlight = { if (state.highlight) state.rim else null },
        shadow = { if (state.shadow) state.dropShadow else null },
        onDrawSurface = { glassSurface(state, effectsSupported) }
      )
  )
}
