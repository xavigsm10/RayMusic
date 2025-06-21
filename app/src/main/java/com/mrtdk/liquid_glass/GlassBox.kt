package com.mrtdk.liquid_glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.intellij.lang.annotations.Language

data class GlassElement(
    val position: Offset,
    val size: Size,
    val scale: Float,
    val blur: Float,
    val centerDistortion: Float,
    val cornerRadius: Float
)

interface GlassScope {
    fun Modifier.glassBackground(
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
    ): Modifier
}

interface GlassBoxScope : BoxScope, GlassScope

private class GlassBoxScopeImpl(
    boxScope: BoxScope,
    glassScope: GlassScope
) : GlassBoxScope, BoxScope by boxScope,
    GlassScope by glassScope

private class GlassScopeImpl(private val density: Density) : GlassScope {

    var updateCounter by mutableStateOf(0)
    val elements: MutableList<GlassElement> = mutableListOf()

    override fun Modifier.glassBackground(
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
    ): Modifier = this
        .background(color = Color.Transparent, shape = shape)
        .onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            val size = coordinates.size.toSize()

            // Use actual position from layout
            val adjustedPosition = position


            val element = GlassElement(
                position = adjustedPosition,
                size = size,
                cornerRadius = shape.topStart.toPx(size, density),
                scale = scale,
                blur = blur,
                centerDistortion = centerDistortion,
            )

            // Add or update glass element
            // Remove existing element with same position/size if any
            elements.removeAll { existing -> 
                existing.position == element.position && existing.size == element.size 
            }
            elements.add(element)
            updateCounter++ // Trigger recomposition

            // Debug print
            // Element registered successfully
        }
}

@Composable
fun GlassContainer(
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val glassScope = remember { GlassScopeImpl(density) }
    val shader = remember { RuntimeShader(GLASS_DISPLACEMENT_SHADER) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                shader.setFloatUniform("resolution", size.width, size.height)
                val a = glassScope.updateCounter

                // Pass all glass elements to shader
                val elements = glassScope.elements
                shader.setIntUniform("elementsCount", elements.size)
                
                // Prepare arrays for shader (up to 10 elements)
                val maxElements = 10
                val positions = FloatArray(maxElements * 2) // x,y pairs
                val sizes = FloatArray(maxElements * 2) // width,height pairs  
                val scales = FloatArray(maxElements)
                val radii = FloatArray(maxElements)
                
                for (i in 0 until minOf(elements.size, maxElements)) {
                    val element = elements[i]
                    positions[i * 2] = element.position.x
                    positions[i * 2 + 1] = element.position.y
                    sizes[i * 2] = element.size.width
                    sizes[i * 2 + 1] = element.size.height
                    scales[i] = element.scale
                    radii[i] = element.cornerRadius
                }
                
                shader.setFloatUniform("glassPositions", positions)
                shader.setFloatUniform("glassSizes", sizes)
                shader.setFloatUniform("glassScales", scales)
                shader.setFloatUniform("cornerRadii", radii)
                
                if (elements.isNotEmpty()) {
                    println("Rendering ${elements.size} glass elements")
                    renderEffect = RenderEffect.createRuntimeShaderEffect(
                        shader, "contents"
                    ).asComposeRenderEffect()
                }
            }
    ) {
        // Background content
        content()

    }
    Box(modifier = Modifier.fillMaxSize()) {
        GlassBoxScopeImpl(this, glassScope).glassContent()
    }
}


@Language("AGSL")
private val GLASS_DISPLACEMENT_SHADER = """
    uniform float2 resolution;
    uniform shader contents;
    uniform int elementsCount;
    uniform float2 glassPositions[10];
    uniform float2 glassSizes[10];
    uniform float glassScales[10];
    uniform float cornerRadii[10];

    // Calculate lens effect using UV scaling
    float2 calculateLensEffect(float2 fragCoord, float2 glassPosition, float2 glassSize, float cornerRadius, float scale) {
        if (scale <= 0.0) return fragCoord;
        
        // Convert glass parameters to UV space
        float2 lensCenter = glassPosition + glassSize * 0.5;
        float2 lensSize = glassSize;
        
        // Convert to relative coordinates [-1, 1] within lens
        float2 rel = (fragCoord - lensCenter) / (lensSize * 0.5);
        
        // Normalize corner radius to relative space
        float normalizedCornerRadius = cornerRadius / (min(lensSize.x, lensSize.y) * 0.5);
        
        // SDF for rounded rectangle
        float2 ab = abs(rel) - (float2(1.0) - normalizedCornerRadius);
        float sdf = length(max(ab, 0.0)) - normalizedCornerRadius;
       
        // Apply lens effect only inside the lens
        if (sdf < 0.0) {
            // Distance from center normalized to [0, 1]
            float dist = length(rel);
            float normalizedDist = dist / length(float2(1.0));
            
            // Create convex distortion profile
            // Maximum distortion at center, smooth falloff to edges
            float convexFactor = 1.0 - smoothstep(0.0, 1.0, normalizedDist);
            
            // Apply convex scaling: stronger in center, weaker at edges
            float distortionStrength = scale * (0.3 + 0.7 * convexFactor);
            
            // Calculate scale factor
            float scaleFactor = 1.0 + distortionStrength;
            
            // Apply scaling with convex distortion
            float2 newCoord = lensCenter + (fragCoord - lensCenter) / scaleFactor;
            
            return newCoord;
        }
        
        return fragCoord;
    }

    float4 main(float2 fragCoord) {
        float2 finalCoord = fragCoord;
        
        // Apply lens effects for each glass element
        for (int i = 0; i < 10; i++) {
            if (i >= elementsCount) break;
            
            float2 glassPosition = glassPositions[i];
            float2 glassSize = glassSizes[i];
            float cornerRadius = cornerRadii[i];
            float glassScale = glassScales[i];
            
            finalCoord = calculateLensEffect(finalCoord, glassPosition, glassSize, cornerRadius, glassScale);
        }
        
        // Sample the background with modified coordinates
        float4 color = contents.eval(finalCoord);
        
        return color;
    }
""".trimIndent()

@Preview(showBackground = true)
@Composable
fun GlassContainerPreview() {
    GlassContainer(
        content = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                repeat(10) {
                    Row(
                        Modifier
                            .padding(12.dp)
                            .background(color = Color.Gray.copy(red = 0.1f * it))
                            .height(64.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        repeat(10) {
                            Text("Hello, world!")
                        }
                    }
                }
            }
        },
        glassContent = {
            val glassModifier = Modifier.glassBackground(
                scale = 0f,
                blur = 0f,
                centerDistortion = 0f,
                shape = CircleShape,
            )

            val glassModifier2 = Modifier.glassBackground(
                scale = 0.5f,
                blur = 0.5f,
                centerDistortion = 0.5f,
                shape = RoundedCornerShape(16.dp),
            )

            val glassModifier3 = Modifier.glassBackground(
                scale = 1f,
                blur = 1.0f,
                centerDistortion = 1f,
                shape = CircleShape,
            )

            Row(Modifier.align(Alignment.Center).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FloatingActionButton(
                    modifier = glassModifier,
                    containerColor = Color.Transparent,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    onClick = {},
                ) {
                    Icon(Icons.Default.Add, null)
                }
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = glassModifier2
                        .size(200.dp, 300.dp)
                ) {
                    Text("Glass content")
                }
                FloatingActionButton(
                    modifier = glassModifier3,
                    shape = CircleShape,
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    onClick = {},
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    )
}
