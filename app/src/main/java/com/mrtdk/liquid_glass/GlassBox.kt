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
    val displacementScale: Float,
    val blur: Float,
    val centerDistortion: Float,
    val cornerRadius: Float
)

interface GlassScope {
    fun Modifier.glassBackground(
        displacementScale: Float,
        blur: Float,
        centerDistortion: Float,
        cornerRadius: Dp,
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
        displacementScale: Float,
        blur: Float,
        centerDistortion: Float,
        cornerRadius: Dp,
    ): Modifier = this
        .background(color = Color.Transparent, shape = RoundedCornerShape(cornerRadius))
        .onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            val size = coordinates.size.toSize()

            // Use actual position from layout
            val adjustedPosition = position


            val element = GlassElement(
                position = adjustedPosition,
                size = size,
                cornerRadius = with(density) { cornerRadius.toPx() },
                displacementScale = displacementScale,
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
                val strengths = FloatArray(maxElements)
                val radii = FloatArray(maxElements)
                
                for (i in 0 until minOf(elements.size, maxElements)) {
                    val element = elements[i]
                    positions[i * 2] = element.position.x
                    positions[i * 2 + 1] = element.position.y
                    sizes[i * 2] = element.size.width
                    sizes[i * 2 + 1] = element.size.height
                    strengths[i] = element.displacementScale
                    radii[i] = element.cornerRadius
                }
                
                shader.setFloatUniform("glassPositions", positions)
                shader.setFloatUniform("glassSizes", sizes)
                shader.setFloatUniform("glassStrengths", strengths)
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
    uniform float glassStrengths[10];
    uniform float cornerRadii[10];

    float4 main(float2 fragCoord) {
        float2 uv = fragCoord / resolution;
        
        // Process all glass elements in one pass
        bool insideGlass = false;
        float2 displacement = float2(0.0);
        
        // Apply displacement for each glass element separately
        for (int i = 0; i < 10; i++) {
            if (i >= elementsCount) break;
            
            float2 glassPosition = glassPositions[i];
            float2 glassSize = glassSizes[i];
            float cornerRadius = cornerRadii[i];
            float glassStrength = glassStrengths[i];
            
            // Check if current pixel is inside this glass element
            float2 glassTopLeft = glassPosition;
            float2 glassBottomRight = glassPosition + glassSize;
            
            bool inRect = fragCoord.x >= glassTopLeft.x && fragCoord.x <= glassBottomRight.x &&
                         fragCoord.y >= glassTopLeft.y && fragCoord.y <= glassBottomRight.y;
            
            bool inThisGlass = false;
            if (inRect) {
                if (cornerRadius <= 0.0) {
                    inThisGlass = true;
                } else {
                    float2 center = glassPosition + glassSize * 0.5;
                    float2 halfSize = glassSize * 0.5 - cornerRadius;
                    float2 pos = abs(fragCoord - center);
                    
                    float2 d = max(pos - halfSize, 0.0);
                    float dist = length(d) - cornerRadius;
                    
                    inThisGlass = dist <= 0.0;
                }
            }
            
            if (inThisGlass) {
                // Create displacement for this glass element
                float2 glassCenter = glassPosition + glassSize * 0.5;
                float2 localPos = (fragCoord - glassCenter) / max(glassSize.x, glassSize.y);
                
                // Radial displacement from center
                float distance = length(localPos);
                float2 direction = normalize(localPos);
                float effect = sin(distance * 15.0) * exp(-distance * 2.0) * glassStrength;
                
                displacement += direction * effect * 20.0;
                insideGlass = true;
            }
        }
        
        float2 finalCoord = fragCoord + displacement;
        float4 color = contents.eval(finalCoord);
        
        // Glass area visual effect (optional tint)
        if (insideGlass) {
            color.rgb += float3(0.05, 0.05, 0.1); // Subtle blue tint
        }
        
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
                displacementScale = 0.5f,
                blur = 1.0f,
                centerDistortion = 1.0f,
                cornerRadius = 32.dp,
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
                    modifier = glassModifier
                        .size(200.dp, 64.dp)
                ) {
                    Text("Glass content")
                }
                FloatingActionButton(
                    modifier = glassModifier,
                    shape = CircleShape,
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    onClick = {},
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    )
}
