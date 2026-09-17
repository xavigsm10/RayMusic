package expo.modules.androidglassview

/** Independent size, centre and material tracks measured from the supplied 30 fps recording.
 * The centre drops below its resting position before the width and corners settle.
 * A scale around the button cannot reproduce that trajectory.
 */
internal object MenuMotion {
  const val OPEN_MS = 450
  const val CLOSE_MS = 333

  data class Frame(
    val width: Float, val height: Float, val centerX: Float, val centerY: Float,
    val roundness: Float, val textAlpha: Float, val textLens: Float, val textBlur: Float,
    val material: Float, val neck: Float = 0f, val sourceAlpha: Float = 0f
  )

  private val keys = arrayOf(
    Frame(.17f, .16f, 0f, 0f, .50f, 0f, .45f, 8f, 0f),
    Frame(.13f, .18f, .08f, .14f, .50f, 0f, .45f, 8f, .08f),
    Frame(.395f, .405f, .39f, .58f, .50f, .02f, .45f, 8f, .22f),
    Frame(.645f, .65f, .64f, .92f, .50f, .15f, .44f, 6f, .36f),
    Frame(.82f, .80f, .825f, 1.135f, .50f, .42f, .40f, 4.5f, .48f),
    Frame(.89f, .878f, .895f, 1.16f, .47f, .64f, .32f, 2.8f, .60f),
    Frame(.98f, .98f, .98f, 1.135f, .33f, .90f, .19f, 1.1f, .76f),
    Frame(1.017f, 1.04f, 1.015f, 1.09f, .23f, 1f, .08f, .35f, .89f),
    Frame(1.03f, 1.04f, 1.035f, 1.045f, .16f, 1f, .025f, 0f, .97f),
    Frame(1.022f, 1.025f, 1.03f, 1f, .135f, 1f, 0f, 0f, 1f),
    Frame(1.018f, 1.014f, 1.02f, .992f, .128f, 1f, 0f, 0f, 1f),
    Frame(1.01f, 1.007f, 1.01f, .996f, .128f, 1f, 0f, 0f, 1f),
    Frame(1.004f, 1.002f, 1.004f, 1f, .128f, 1f, 0f, 0f, 1f),
    Frame(1f, 1f, 1f, 1f, .128f, 1f, 0f, 0f, 1f)
  )

  private val closeKeys = arrayOf(
    Frame(1f, 1f, 1f, 1f, .128f, 1f, 0f, 0f, 1f),
    Frame(1f, 1f, 1f, 1f, .50f, 1f, 0f, 0f, .75f),
    Frame(.930f, .926f, .923f, 1.22f, .20f, .70f, .05f, 3.5f, .30f),
    Frame(.783f, .782f, .765f, 1.20f, .29f, .10f, .18f, 6f, .06f),
    Frame(.535f, .537f, .508f, .829f, .40f, 0f, 0f, 0f, .02f, .25f),
    Frame(.392f, .439f, .350f, .493f, .50f, 0f, 0f, 0f, .02f, .70f),
    Frame(.270f, .355f, .224f, .234f, .50f, 0f, 0f, 0f, .02f, 1f, .10f),
    Frame(.195f, .269f, .109f, .078f, .50f, 0f, 0f, 0f, .02f, .85f, .70f),
    Frame(.175f, .235f, .066f, .029f, .50f, 0f, 0f, 0f, .02f, .50f, 1f),
    Frame(.167f, .184f, 0f, -.034f, .50f, 0f, 0f, 0f, .02f, .25f, 1f),
    Frame(.17f, .16f, 0f, 0f, .50f, 0f, 0f, 0f, 0f, 0f, 1f)
  )

  fun sample(progress: Float): Frame = interpolate(keys, progress)

  private fun interpolate(keys: Array<Frame>, progress: Float): Frame {
    val position = progress.coerceIn(0f, 1f) * (keys.size - 1)
    val index = position.toInt().coerceAtMost(keys.lastIndex - 1)
    val t = (position - index).coerceIn(0f, 1f)
    // Smooth between the measured frames without turning plateaus into extra bounces.
    fun interpolate(value: (Frame) -> Float): Float {
      val a = value(keys[index]); val b = value(keys[index + 1]); val delta = b - a
      if (delta == 0f) return a
      fun tangent(slope: Float): Float = if (slope * delta <= 0f) 0f
        else slope.coerceIn(minOf(0f,3f*delta),maxOf(0f,3f*delta))
      val m0 = tangent((b-value(keys[(index-1).coerceAtLeast(0)]))/2f)
      val m1 = tangent((value(keys[(index+2).coerceAtMost(keys.lastIndex)])-a)/2f)
      val t2=t*t; val t3=t2*t
      return (2*t3-3*t2+1)*a + (t3-2*t2+t)*m0 + (-2*t3+3*t2)*b + (t3-t2)*m1
    }
    return Frame(interpolate { it.width },interpolate { it.height },interpolate { it.centerX },
      interpolate { it.centerY },interpolate { it.roundness },interpolate { it.textAlpha },
      interpolate { it.textLens },interpolate { it.textBlur },interpolate { it.material },
      interpolate { it.neck }, interpolate { it.sourceAlpha })
  }

  fun closing(progress: Float): Frame = interpolate(closeKeys, 1f - progress)
}
