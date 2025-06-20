package com.mrtdk.liquid_glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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

    var updateCounter by mutableStateOf(0)
    val elements: MutableList<GlassElement> = mutableListOf()

    override fun Modifier.glassBackground(
        displacementScale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: Shape
    ): Modifier = this
        .background(color = Color.Transparent)
        .onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            val size = coordinates.size.toSize()
            
            // Use actual position from layout
            val adjustedPosition = position
            
            val element = GlassElement(
                position = adjustedPosition,
                size = size,
                shape = shape,
                displacementScale = displacementScale,
                blur = blur,
                centerDistortion = centerDistortion
            )
            
            // Update or add glass element
            elements.clear() // Clear and add new element
            elements.add(element)
            updateCounter++ // Trigger recomposition
            
            // Debug print
            // Element registered successfully
        }
}

@Composable
fun GlassContainer(
    content: @Composable () -> Unit,
    glassContent: @Composable GlassScope.() -> Unit,
) {
    val glassScope = remember { GlassScopeImpl() }
    val density = LocalDensity.current
    val shader = remember { RuntimeShader(GLASS_DISPLACEMENT_SHADER) }
        Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                shader.setFloatUniform("resolution", size.width, size.height)
                val a = glassScope.updateCounter

                // For now, test with fixed coordinates where we know the button should be
                if (glassScope.elements.isNotEmpty()) {
                    val element = glassScope.elements.first()
                    shader.setFloatUniform("glassPosition", element.position.x, element.position.y)
                    shader.setFloatUniform("glassSize", element.size.width, element.size.height)
                    shader.setFloatUniform("glassStrength", element.displacementScale)
                    // Element found and applied
                } else {
                    // No glass elements - no effect
                    shader.setFloatUniform("glassPosition", -1000f, -1000f)
                    shader.setFloatUniform("glassSize", 0f, 0f)
                    shader.setFloatUniform("glassStrength", 0f)
                }
                
                renderEffect = RenderEffect.createRuntimeShaderEffect(
                    shader, "contents"
                ).asComposeRenderEffect()
            }
    ) {
        // Background content
        content()
        
        // Glass content overlay
        glassScope.glassContent()
    }
}



@Language("AGSL")
private val GLASS_DISPLACEMENT_SHADER = """
    uniform float2 resolution;
    uniform shader contents;
    uniform float2 glassPosition;
    uniform float2 glassSize;
    uniform float glassStrength;

    float4 main(float2 fragCoord) {
        float2 uv = fragCoord / resolution;
        
        // Check if we're inside the glass area
        float2 glassTopLeft = glassPosition;
        float2 glassBottomRight = glassPosition + glassSize;
        
        bool insideGlass = fragCoord.x >= glassTopLeft.x && fragCoord.x <= glassBottomRight.x &&
                          fragCoord.y >= glassTopLeft.y && fragCoord.y <= glassBottomRight.y;
        
        float2 displacement = float2(0.0);
        
        if (insideGlass) {
            // Create displacement only in glass area
            float2 glassCenter = glassPosition + glassSize * 0.5;
            float2 localPos = (fragCoord - glassCenter) / max(glassSize.x, glassSize.y);
            
            // Radial displacement from center
            float distance = length(localPos);
            float2 direction = normalize(localPos);
            float effect = sin(distance * 15.0) * exp(-distance * 2.0) * glassStrength;
            
            displacement = direction * effect * 20.0;
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
                    Row(Modifier.padding(12.dp).background(color = Color.Gray.copy(red = 0.1f * it)).height(64.dp).horizontalScroll(rememberScrollState())) {
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .glassBackground(
                        displacementScale = 2.0f,
                        blur = 1.0f,
                        centerDistortion = 2.0f,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Text("Glass content")
            }
        }
    )
}
