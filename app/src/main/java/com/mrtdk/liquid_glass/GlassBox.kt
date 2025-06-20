package com.mrtdk.liquid_glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.intellij.lang.annotations.Language

data class GlassElement(
    val position: Offset,
    val size: Size,
    val shape: Shape,
    val displacementScale: Float,
    val blur: Float,
    val centerDistortion: Float
)

interface GlassScope {
    fun Modifier.glassBackground(
        displacementScale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: Shape = RoundedCornerShape(0.dp)
    ): Modifier
}

private class GlassScopeImpl : GlassScope {

    val elements: MutableList<GlassElement> = mutableListOf()

    override fun Modifier.glassBackground(
        displacementScale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: Shape
    ): Modifier = this.onGloballyPositioned { coordinates ->
        val position = coordinates.positionInParent()
        val size = coordinates.size.toSize()
        
        val element = GlassElement(
            position = position,
            size = size,
            shape = shape,
            displacementScale = displacementScale,
            blur = blur,
            centerDistortion = centerDistortion
        )
        
        // Update or add glass element
        val existingIndex = elements.indexOfFirst {
            it.position == position && it.size == size 
        }
        if (existingIndex >= 0) {
            elements[existingIndex] = element
        } else {
            elements.add(element)
        }
    }
}

@Composable
fun GlassContainer(
    content: @Composable () -> Unit,
    glassContent: @Composable GlassScope.() -> Unit,
) {
    val glassScope = remember { GlassScopeImpl() }
    
    val time by produceState(0f) {
        while (true) {
            withInfiniteAnimationFrameMillis { value = it / 1000f }
        }
    }
    CircleShape
    val density = LocalDensity.current
    val shader = remember { RuntimeShader(GLASS_DISPLACEMENT_SHADER) }
    Box(modifier = Modifier.fillMaxSize()) {
        // Background content with glass effect applied
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    shader.setFloatUniform("time", time)
                    shader.setFloatUniform("resolution", size.width, size.height)
                    
                    // Pass glass elements data to shader
                    val elementsCount = minOf(glassScope.elements.size, MAX_GLASS_ELEMENTS)
                    shader.setIntUniform("elementsCount", elementsCount)
                    
                    for (i in 0 until elementsCount) {
                        val element = glassScope.elements[i]
                        val prefix = "elements[$i]."
                        
                        shader.setFloatUniform("${prefix}position", element.position.x, element.position.y)
                        shader.setFloatUniform("${prefix}size", element.size.width, element.size.height)
                        shader.setFloatUniform("${prefix}displacementScale", element.displacementScale)
                        shader.setFloatUniform("${prefix}blur", element.blur)
                        shader.setFloatUniform("${prefix}centerDistortion", element.centerDistortion)
                        
                        // Shape information (simplified for shader)
                        val cornerRadius = when (element.shape) {
                            is RoundedCornerShape -> with(density) {
                                element.shape.topStart.toPx(element.size, this)
                            }
                            else -> 0f
                        }
                        shader.setFloatUniform("${prefix}cornerRadius", cornerRadius)
                        
                        val isCircle = false //todo
                        shader.setFloatUniform("${prefix}isCircle", if (isCircle) 1.0f else 0.0f)
                    }
                    
                    renderEffect = RenderEffect.createRuntimeShaderEffect(
                        shader, "contents"
                    ).asComposeRenderEffect()
                }
        ) {
            content()
        }
        
        // Glass content overlay (invisible but positioned for coordinate tracking)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent { 
                    // Don't draw glass content, just track positions
                }
        ) {
            glassScope.glassContent()
        }
    }
}

private const val MAX_GLASS_ELEMENTS = 8

@Language("AGSL")
private val GLASS_DISPLACEMENT_SHADER = """
    uniform float2 resolution;
    uniform float time;
    uniform int elementsCount;
    uniform shader contents;
    
    struct GlassElement {
        float2 position;
        float2 size;
        float displacementScale;
        float blur;
        float centerDistortion;
        float cornerRadius;
        float isCircle;
    };
    
    uniform GlassElement elements[8];
    
    // Noise functions for displacement
    float hash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
    }
    
    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        
        return mix(
            mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
            mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
            u.y
        );
    }
    
    float fbm(float2 p) {
        float value = 0.0;
        float amplitude = 0.5;
        float frequency = 1.0;
        
        for (int i = 0; i < 3; i++) {
            value += amplitude * noise(p * frequency);
            amplitude *= 0.5;
            frequency *= 2.0;
        }
        
        return value;
    }
    
    // Check if point is inside rounded rectangle or circle
    float getShapeMask(float2 uv, GlassElement element) {
        float2 center = element.position + element.size * 0.5;
        float2 relativePos = uv - center;
        
        if (element.isCircle > 0.5) {
            // Circle shape
            float radius = min(element.size.x, element.size.y) * 0.5;
            float distance = length(relativePos);
            return 1.0 - smoothstep(radius - 2.0, radius, distance);
        } else {
            // Rounded rectangle
            float2 halfSize = element.size * 0.5;
            float2 distance = abs(relativePos) - halfSize + element.cornerRadius;
            float outsideDistance = length(max(distance, 0.0));
            float insideDistance = min(max(distance.x, distance.y), 0.0);
            float totalDistance = outsideDistance + insideDistance - element.cornerRadius;
            
            return 1.0 - smoothstep(-2.0, 2.0, totalDistance);
        }
    }
    
    // Generate displacement for a glass element
    float2 generateDisplacement(float2 uv, GlassElement element, float time) {
        float2 center = element.position + element.size * 0.5;
        float2 localUV = (uv - center) / max(element.size.x, element.size.y);
        
        // Base noise displacement
        float2 noiseCoord = localUV * 4.0 + time * 0.3;
        float noiseValue = fbm(noiseCoord);
        
        // Radial distortion from center
        float2 radialDir = normalize(localUV);
        float distanceFromCenter = length(localUV);
        
        // Center distortion effect
        float centerEffect = element.centerDistortion * 
            exp(-distanceFromCenter * 3.0) * 
            sin(time * 2.0 + distanceFromCenter * 8.0);
        
        // Combine displacements
        float2 displacement = radialDir * centerEffect * 0.02;
        displacement += float2(
            sin(noiseValue * 6.28318) * 0.01,
            cos(noiseValue * 6.28318) * 0.01
        ) * element.displacementScale;
        
        // Apply shape mask
        float mask = getShapeMask(uv, element);
        displacement *= mask;
        
        return displacement;
    }
    
    // Apply blur effect
    float4 applyBlur(float2 uv, GlassElement element, float intensity) {
        float4 color = float4(0.0);
        float totalWeight = 0.0;
        
        float2 center = element.position + element.size * 0.5;
        float distanceFromCenter = length(uv - center) / max(element.size.x, element.size.y);
        
        float blurRadius = intensity * element.blur * 0.005 * (1.0 + distanceFromCenter);
        int samples = 8;
        
        for (int i = 0; i < 8; i++) {
            float angle = float(i) * 6.28318 / 8.0;
            float2 offset = float2(cos(angle), sin(angle)) * blurRadius;
            
            float weight = 1.0;
            color += contents.eval((uv + offset) * resolution) * weight;
            totalWeight += weight;
        }
        
        // Center sample
        color += contents.eval(uv * resolution) * 2.0;
        totalWeight += 2.0;
        
        return color / totalWeight;
    }

    float4 main(in float2 fragCoord) {
        float2 uv = fragCoord / resolution;
        
        // Accumulate displacement from all glass elements
        float2 totalDisplacement = float2(0.0);
        float totalBlurIntensity = 0.0;
        
        for (int i = 0; i < elementsCount && i < 8; i++) {
            GlassElement element = elements[i];
            
            // Check if current pixel is affected by this glass element
            float mask = getShapeMask(fragCoord, element);
            
            if (mask > 0.01) {
                // Add displacement
                totalDisplacement += generateDisplacement(fragCoord, element, time);
                
                // Add blur intensity
                totalBlurIntensity += element.blur * mask;
            }
        }
        
        // Apply displacement
        float2 finalUV = uv + totalDisplacement / resolution;
        
        // Sample with or without blur
        float4 color;
        if (totalBlurIntensity > 0.01) {
            // Find the most relevant glass element for blur
            float maxBlur = 0.0;
            int blurElementIndex = -1;
            
            for (int i = 0; i < elementsCount && i < 8; i++) {
                float mask = getShapeMask(fragCoord, elements[i]);
                float blurContribution = elements[i].blur * mask;
                if (blurContribution > maxBlur) {
                    maxBlur = blurContribution;
                    blurElementIndex = i;
                }
            }
            
            if (blurElementIndex >= 0) {
                color = applyBlur(finalUV, elements[blurElementIndex], totalBlurIntensity);
            } else {
                color = contents.eval(finalUV * resolution);
            }
        } else {
            color = contents.eval(finalUV * resolution);
        }
        
        return color;
    }
""".trimIndent()

@Preview(showBackground = true)
@Composable
fun GlassContainerPreview() {
    GlassContainer(
        content = {
            Column {
                repeat(10) {
                    Row {
                        repeat(10) {
                            Text("Hello, world!")
                        }
                    }
                }
            }
        },
        glassContent = {
            Button(
                onClick = { },
                modifier = Modifier.glassBackground(
                    displacementScale = 1.0f,
                    blur = 1.0f,
                    centerDistortion = 1.0f
                )
            ) {
                Text("Glass content")
            }
        }
    )
}