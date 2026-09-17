package expo.modules.androidglassview.backdrop.effects

import expo.modules.androidglassview.backdrop.BackdropEffectScope
import expo.modules.androidglassview.backdrop.isRuntimeShaderSupported

/** A separable Gaussian whose radius falls linearly from the top edge to zero at the bottom. */
internal fun BackdropEffectScope.linearBlur(radius: Float) {
    if (radius <= 0f) return
    if (!isRuntimeShaderSupported()) {
        blur(radius)
        return
    }
    // Vertical first: the horizontal pass samples the same y, so both kernels use the
    // output pixel's radius. Reversing the passes would mix different blur radii.
    for (vertical in listOf(true, false)) {
        runtimeShaderEffect(
            key = if (vertical) "LinearBlurVertical" else "LinearBlurHorizontal",
            shaderString = LinearBlurShader,
            uniformShaderName = "content"
        ) {
            setFloatUniform("bounds", size.width, size.height)
            setFloatUniform("sigma", radius * 0.57735f + 0.5f)
            setFloatUniform("axis", if (vertical) 0f else 1f, if (vertical) 1f else 0f)
        }
    }
}

private const val LinearBlurShader = """
uniform shader content;
uniform float2 bounds;
uniform float sigma;
uniform float2 axis;
half4 main(float2 position) {
    float strength = clamp(1.0 - position.y / max(bounds.y - 1.0, 1.0), 0.0, 1.0);
    if (strength <= 0.0001) return content.eval(position);
    float stepSize = sigma * strength * 0.5;
    half4 sum = half4(0.0);
    float total = 0.0;
    for (int i = -6; i <= 6; i++) {
        float weight = exp(-0.125 * float(i * i));
        float2 samplePosition = clamp(position + axis * (float(i) * stepSize), float2(0.5), max(bounds - 0.5, float2(0.5)));
        sum += content.eval(samplePosition) * half(weight);
        total += weight;
    }
    return sum / half(total);
}
"""
