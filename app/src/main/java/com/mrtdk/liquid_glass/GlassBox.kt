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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val cornerRadius: Float,
    val elevation: Float
)

interface GlassScope {
    fun Modifier.glassBackground(
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp = 0.dp,
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
        elevation: Dp,
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
                elevation = with(density) { elevation.toPx() },
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
                val elevations = FloatArray(maxElements)

                for (i in 0 until minOf(elements.size, maxElements)) {
                    val element = elements[i]
                    positions[i * 2] = element.position.x
                    positions[i * 2 + 1] = element.position.y
                    sizes[i * 2] = element.size.width
                    sizes[i * 2 + 1] = element.size.height
                    scales[i] = element.scale
                    radii[i] = element.cornerRadius
                    elevations[i] = element.elevation
                }

                shader.setFloatUniform("glassPositions", positions)
                shader.setFloatUniform("glassSizes", sizes)
                shader.setFloatUniform("glassScales", scales)
                shader.setFloatUniform("cornerRadii", radii)
                shader.setFloatUniform("elevations", elevations)

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
    uniform float elevations[10];

    // Calculate lens effect using UV scaling
    float2 calculateLensEffect(float2 fragCoord, float2 glassPosition, float2 glassSize, float cornerRadius, float scale) {
        if (scale <= 0.0) return fragCoord;
        
        // Convert glass parameters to UV space
        float2 lensCenter = glassPosition + glassSize * 0.5;
        float2 lensSize = glassSize;
        
        // Convert to pixel coordinates relative to lens center
        float2 localCoord = fragCoord - lensCenter;
        
        // Calculate half-sizes
        float2 halfSize = lensSize * 0.5;
        
        // SDF for rounded rectangle in pixel space
        float2 absCoord = abs(localCoord);
        float2 rectSize = halfSize - float2(cornerRadius);
        
        // Distance to the rounded rectangle
        float2 d = absCoord - rectSize;
        float outsideDistance = length(max(d, 0.0));
        float insideDistance = min(max(d.x, d.y), 0.0);
        float sdf = outsideDistance + insideDistance - cornerRadius;
       
        // Apply lens effect only inside the lens
        if (sdf < 0.0) {
            // Convert back to relative coordinates for lens calculation
            float2 rel = localCoord / halfSize;
            
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

    // Calculate elevation shadow alpha for given position relative to element
    float calculateElevationShadow(float2 localCoord, float2 halfSize, float cornerRadius, float elevation) {
        if (elevation <= 0.0) return 0.0;
        
        // Shadow offset and blur based on elevation
        float shadowOffsetY = elevation * 0.5; // Shadow moves down
        float shadowBlur = elevation * 2.0; // Shadow gets bigger and softer
        
        // Offset the shadow position downward
        float2 shadowCoord = localCoord - float2(0.0, shadowOffsetY);
        
        // Calculate SDF for the shadow (same shape as element but offset)
        float2 absCoord = abs(shadowCoord);
        float2 rectSize = halfSize - float2(cornerRadius);
        float2 d = absCoord - rectSize;
        float outsideDistance = length(max(d, 0.0));
        float insideDistance = min(max(d.x, d.y), 0.0);
        float shadowSdf = outsideDistance + insideDistance - cornerRadius;
        
        // Only render shadow outside the original element
        float2 originalAbsCoord = abs(localCoord);
        float2 originalD = originalAbsCoord - rectSize;
        float originalOutsideDistance = length(max(originalD, 0.0));
        float originalInsideDistance = min(max(originalD.x, originalD.y), 0.0);
        float originalSdf = originalOutsideDistance + originalInsideDistance - cornerRadius;
        
        // Shadow only appears outside the original element and within shadow blur range
        if (originalSdf <= 0.0 || shadowSdf > shadowBlur) return 0.0;
        
        // Smooth falloff
        float normalizedDist = shadowSdf / shadowBlur;
        return (1.0 - normalizedDist) * 0.15; // Reduced intensity from 0.4 to 0.15
    }

    // Calculate rim highlight effect with vertical lighting variation
    float calculateRimHighlight(float2 localCoord, float2 halfSize, float cornerRadius) {
        // Calculate SDF for current position
        float2 absCoord = abs(localCoord);
        float2 rectSize = halfSize - float2(cornerRadius);
        float2 d = absCoord - rectSize;
        float outsideDistance = length(max(d, 0.0));
        float insideDistance = min(max(d.x, d.y), 0.0);
        float sdf = outsideDistance + insideDistance - cornerRadius;
        
        // Rim highlight appears in a thin band around the element edge
        float rimWidth = 4.0;
        if (sdf > 0.0 && sdf < rimWidth) {
            // Basic intensity falls off from edge outward
            float intensity = (rimWidth - sdf) / rimWidth;
            
            // Calculate vertical position factor (-1 at top, +1 at bottom)
            float verticalPos = localCoord.y / halfSize.y;
            
            // Lighting factor: stronger at top (negative Y), weaker at bottom (positive Y)
            // Map from [-1, 1] to [1.2, 0.3] - stronger highlight at top, weaker at bottom
            float lightingFactor = mix(1.2, 0.7, (verticalPos + 1.0) * 0.5);
            
            return intensity * 0.6 * lightingFactor;
        }
        
        return 0.0;
    }

    float4 main(float2 fragCoord) {
        float2 finalCoord = fragCoord;
        float shadowAlpha = 0.0;
        float rimHighlight = 0.0;
        
        // Apply lens effects and calculate elevation shadows for each glass element
        for (int i = 0; i < 10; i++) {
            if (i >= elementsCount) break;
            
            float2 glassPosition = glassPositions[i];
            float2 glassSize = glassSizes[i];
            float cornerRadius = cornerRadii[i];
            float glassScale = glassScales[i];
            float elevation = elevations[i];
            
            float2 lensCenter = glassPosition + glassSize * 0.5;
            float2 localCoord = fragCoord - lensCenter;
            float2 halfSize = glassSize * 0.5;
            
            // Apply lens effect
            finalCoord = calculateLensEffect(finalCoord, glassPosition, glassSize, cornerRadius, glassScale);
            
            // Calculate elevation shadow if enabled
            if (elevation > 0.0) {
                float currentShadow = calculateElevationShadow(localCoord, halfSize, cornerRadius, elevation);
                shadowAlpha = max(shadowAlpha, currentShadow);
            }
            
            // Calculate rim highlight
            float currentRim = calculateRimHighlight(localCoord, halfSize, cornerRadius);
            rimHighlight = max(rimHighlight, currentRim);
        }
        
        // Sample the background with modified coordinates
        float4 color = contents.eval(finalCoord);
        
        // Apply rim highlight effect - reflects surrounding content
        if (rimHighlight > 0.0) {
            // Calculate surface normal for more realistic reflection
            float2 surfaceNormal = float2(0.0);
            
            // Find the closest glass element to determine surface normal
            for (int i = 0; i < 10; i++) {
                if (i >= elementsCount) break;
                
                float2 glassPosition = glassPositions[i];
                float2 glassSize = glassSizes[i];
                float cornerRadius = cornerRadii[i];
                
                float2 lensCenter = glassPosition + glassSize * 0.5;
                float2 localCoord = fragCoord - lensCenter;
                float2 halfSize = glassSize * 0.5;
                
                // Calculate if this pixel is in the rim highlight zone for this element
                float2 absCoord = abs(localCoord);
                float2 rectSize = halfSize - float2(cornerRadius);
                float2 d = absCoord - rectSize;
                float outsideDistance = length(max(d, 0.0));
                float insideDistance = min(max(d.x, d.y), 0.0);
                float sdf = outsideDistance + insideDistance - cornerRadius;
                
                if (sdf > 0.0 && sdf < 4.0) {
                    // Calculate surface normal by sampling SDF gradient
                    float epsilon = 1.0;
                    float2 localCoordX = localCoord + float2(epsilon, 0.0);
                    float2 localCoordY = localCoord + float2(0.0, epsilon);
                    
                    // Calculate SDF at offset positions
                    float2 absCoordX = abs(localCoordX);
                    float2 dX = absCoordX - rectSize;
                    float sdfX = length(max(dX, 0.0)) + min(max(dX.x, dX.y), 0.0) - cornerRadius;
                    
                    float2 absCoordY = abs(localCoordY);
                    float2 dY = absCoordY - rectSize;
                    float sdfY = length(max(dY, 0.0)) + min(max(dY.x, dY.y), 0.0) - cornerRadius;
                    
                    // Surface normal points outward from the element
                    surfaceNormal = normalize(float2(sdfX - sdf, sdfY - sdf));
                    break;
                }
            }
            
            // Use surface normal for simple reflection
            float2 reflectionOffset = surfaceNormal * 12.0;
            float4 reflectedColor = contents.eval(fragCoord + reflectionOffset);
            
            // Brighten the reflected color to create highlight effect  
            reflectedColor.rgb = reflectedColor.rgb * 1.5 + 0.2;
            
            // Mix with the base color
            color = mix(color, reflectedColor, rimHighlight);
        }
        
        // Apply shadow by darkening the color
        if (shadowAlpha > 0.0) {
            color.rgb = mix(color.rgb, float3(0.0), shadowAlpha);
        }
        
        return color;
    }
""".trimIndent()

@Preview(showBackground = true)
@Composable
fun GlassContainerPreview() {
    GlassContainer(
        content = {
            Column(Modifier.verticalScroll(rememberScrollState()).background(Brush.verticalGradient(
                listOf(Color.Black, Color.Blue)))) {
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
                elevation = 8.dp,
            )

            val glassModifier3 = Modifier.glassBackground(
                scale = 1f,
                blur = 1.0f,
                centerDistortion = 1f,
                shape = CircleShape,
                elevation = 6.dp,
            )

            Row(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
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
