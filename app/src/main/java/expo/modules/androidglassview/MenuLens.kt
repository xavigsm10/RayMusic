package expo.modules.androidglassview

import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

/** Refracts labels only. The sampled photographic backdrop must stay in window coordinates. */
@RequiresApi(33)
internal class MenuLens {
  private val shader = android.graphics.RuntimeShader("""
    uniform shader content;
    uniform float2 resolution;
    uniform float amount;
    half4 main(float2 p) {
      float2 halfSize = resolution * 0.5;
      float2 uv = (p - halfSize) / halfSize;
      float r2 = dot(uv, uv);
      // The centre remains readable while labels bend into the curved perimeter.
      float radial = 0.25 + 0.75 * min(r2, 1.1);
      float2 q = uv * (1.0 - amount * radial);
      q.y += amount * 0.13 * sin(uv.y * 3.14159) * uv.x * uv.x;
      return content.eval(q * halfSize + halfSize);
    }
  """)

  fun update(width: Float, height: Float, amount: Float, blur: Float): RenderEffect? {
    if (width <= 0f || height <= 0f || (amount < .001f && blur < .1f)) return null
    shader.setFloatUniform("resolution",width,height)
    shader.setFloatUniform("amount",amount)
    // RenderEffect captures shader uniforms: create the effect after updating them.
    val warp=android.graphics.RenderEffect.createRuntimeShaderEffect(shader,"content")
    return (if(blur >= .1f) android.graphics.RenderEffect.createChainEffect(warp,
      android.graphics.RenderEffect.createBlurEffect(blur,blur,Shader.TileMode.DECAL)) else warp).asComposeRenderEffect()
  }
}
