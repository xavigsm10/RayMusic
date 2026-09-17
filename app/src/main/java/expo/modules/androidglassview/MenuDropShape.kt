package expo.modules.androidglassview

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/** The disappearing menu is pulled into the source at its upper right shoulder. */
internal class MenuDropShape(private val radius: Dp, private val neck: Float) : Shape {
  override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
    val w = size.width
    val h = size.height
    val r = with(density) { radius.toPx() }.coerceIn(0f, size.minDimension / 2f)
    val c = r * .55228475f
    val round = arrayOf(
      floatArrayOf(r, 0f),
      floatArrayOf(r, 0f, w-r, 0f, w-r, 0f),
      floatArrayOf(w-r+c, 0f, w, r-c, w, r),
      floatArrayOf(w, r, w, h-r, w, h-r),
      floatArrayOf(w, h-r+c, w-r+c, h, w-r, h),
      floatArrayOf(w-r, h, r, h, r, h),
      floatArrayOf(r-c, h, 0f, h-r+c, 0f, h-r),
      floatArrayOf(0f, h-r, 0f, r, 0f, r),
      floatArrayOf(0f, r-c, r-c, 0f, r, 0f)
    )
    val drop = arrayOf(
      floatArrayOf(.55f, .16f),
      floatArrayOf(.66f, .08f, .61f, 0f, .78f, 0f),
      floatArrayOf(.98f, 0f, .91f, .20f, .98f, .38f),
      floatArrayOf(1f, .52f, 1f, .69f, .94f, .80f),
      floatArrayOf(.88f, .96f, .76f, 1f, .58f, 1f),
      floatArrayOf(.48f, 1f, .39f, 1f, .30f, .97f),
      floatArrayOf(.08f, .94f, 0f, .82f, 0f, .68f),
      floatArrayOf(0f, .49f, .05f, .40f, .17f, .33f),
      floatArrayOf(.30f, .25f, .44f, .25f, .55f, .16f)
    )
    fun point(segment: Int, coordinate: Int): Float {
      val from = round[segment][coordinate]
      val to = drop[segment][coordinate] * if (coordinate % 2 == 0) w else h
      return from + (to - from) * neck.coerceIn(0f, 1f)
    }
    return Outline.Generic(Path().apply {
      moveTo(point(0,0), point(0,1))
      for (i in 1..8) cubicTo(point(i,0), point(i,1), point(i,2), point(i,3), point(i,4), point(i,5))
      close()
    })
  }
}
