package expo.modules.androidglassview

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/** Small native vector glyphs remain sharp while the menu's content lens deforms them. */
internal fun DrawScope.drawMenuIcon(name: String, color: Color) {
  if (name == "chevron" || name == "chevron-down") {
    val path = Path().apply {
      if (name == "chevron") {
        moveTo(size.width * .2f, size.height * .25f)
        lineTo(size.width * .75f, size.height * .5f)
        lineTo(size.width * .2f, size.height * .75f)
      } else {
        moveTo(size.width * .12f, size.height * .38f)
        lineTo(size.width * .5f, size.height * .66f)
        lineTo(size.width * .88f, size.height * .38f)
      }
    }
    drawPath(path, color, style = Stroke(1.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
    return
  }
  withTransform({ scale(size.width / 20f, size.height / 20f, Offset.Zero) }) {
    val stroke = Stroke(1.45f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun line(vararg coordinates: Float) {
      val p=Path().apply { moveTo(coordinates[0],coordinates[1]); for(i in 2 until coordinates.size step 2) lineTo(coordinates[i],coordinates[i+1]) }
      drawPath(p,color,style=stroke)
    }
    fun rect(x:Float,y:Float,w:Float,h:Float,r:Float=1.4f) =
      drawRoundRect(color,Offset(x,y),Size(w,h),CornerRadius(r),style=stroke)
    when(name) {
      "check" -> line(2f,10f,6f,14f,12f,5f)
      "sort" -> { line(1f,5f,19f,5f);line(4f,10f,16f,10f);line(7f,15f,13f,15f) }
      "grid" -> for(y in 0..2) for(x in 0..2) rect(1f+x*6.5f,1f+y*6.5f,4f,4f,.6f)
      "heart" -> drawPath(Path().apply { moveTo(10f,18f); cubicTo(-6f,8f,3f,-4f,10f,4f); cubicTo(17f,-4f,26f,8f,10f,18f); close() },color,style=stroke)
      "adjustments" -> { for(y in listOf(4f,10f,16f)) line(1f,y,19f,y)
        drawCircle(color,1.8f,Offset(13f,4f),style=stroke);drawCircle(color,1.8f,Offset(6f,10f),style=stroke);drawCircle(color,1.8f,Offset(12f,16f),style=stroke) }
      "photo" -> { rect(1f,2f,18f,16f);drawCircle(color,1.5f,Offset(6f,7f));
        drawPath(Path().apply { moveTo(2f,16f);lineTo(8f,10f);lineTo(11f,13f);lineTo(14f,9f);lineTo(18f,13f);lineTo(18f,17f);close() },color) }
      "video" -> { rect(1f,4f,13f,12f);line(14f,7f,19f,4f,19f,16f,14f,13f) }
      "screenshot" -> { rect(4f,5f,12f,10f,2f);drawCircle(color,2.5f,Offset(10f,10f),style=stroke)
        line(1f,5f,1f,2f,5f,2f);line(15f,2f,19f,2f,19f,5f);line(1f,15f,1f,18f,5f,18f);line(15f,18f,19f,18f,19f,15f) }
      "album" -> { rect(2f,4f,16f,14f);line(5f,1f,15f,1f);line(1f,1f,19f,19f) }
      "zoom-in", "zoom-out" -> { drawCircle(color,6.3f,Offset(8f,8f),style=stroke);line(12.7f,12.7f,19f,19f);line(5f,8f,11f,8f);if(name=="zoom-in") line(8f,5f,8f,11f) }
      "aspect" -> { rect(2f,5f,16f,10f);line(8f,1f,10f,3f,12f,1f);line(8f,19f,10f,17f,12f,19f) }
      "people" -> { drawCircle(color,3f,Offset(10f,8f));drawCircle(color.copy(alpha=.65f),2.2f,Offset(3f,5f));drawCircle(color.copy(alpha=.65f),2.2f,Offset(17f,5f))
        drawRoundRect(color,Offset(5f,12f),Size(10f,6f),CornerRadius(3f));rect(0f,9f,3f,4f,.7f);rect(17f,9f,3f,4f,.7f) }
    }
  }
}
