package com.mrtdk.liquid_glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

                // For now, test with fixed coordinates where we know the button should be
                if (glassScope.elements.isNotEmpty()) {
                    val element = glassScope.elements.first()
                    shader.setFloatUniform("glassPosition", element.position.x, element.position.y)
                    shader.setFloatUniform("glassSize", element.size.width, element.size.height)
                    shader.setFloatUniform("glassStrength", element.displacementScale)
                    shader.setFloatUniform("cornerRadius", element.cornerRadius)

                    println("glassSize ${element.size.width}, ${element.size.height}")

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
    uniform float2 glassPosition;
    uniform float2 glassSize;
    uniform float glassStrength;
    uniform float cornerRadius;

    float4 main(float2 fragCoord) {
        float2 uv = fragCoord / resolution;
        
        // Check if we're inside the glass area with rounded corners
        float2 glassTopLeft = glassPosition;
        float2 glassBottomRight = glassPosition + glassSize;
        
        // First check if we're in the rectangular bounds
        bool inRect = fragCoord.x >= glassTopLeft.x && fragCoord.x <= glassBottomRight.x &&
                     fragCoord.y >= glassTopLeft.y && fragCoord.y <= glassBottomRight.y;
        
        bool insideGlass = false;
        if (inRect) {
            if (cornerRadius <= 0.0) {
                // No rounding, just use rectangle
                insideGlass = true;
            } else {
                // Calculate distance to rounded rectangle
                float2 center = glassPosition + glassSize * 0.5;
                float2 halfSize = glassSize * 0.5 - cornerRadius;
                float2 pos = abs(fragCoord - center);
                
                // Distance to rounded rectangle
                float2 d = max(pos - halfSize, 0.0);
                float dist = length(d) - cornerRadius;
                
                insideGlass = dist <= 0.0;
            }
        }
        
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
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(200.dp, 64.dp)
                    .glassBackground(
                        displacementScale = 0.5f,
                        blur = 1.0f,
                        centerDistortion = 1.0f,
                        cornerRadius = 16.dp,
                    )
            ) {
                Text("Glass content")
            }
        }
    )
}
