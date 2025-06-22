package com.mrtdk.liquid_glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
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
import kotlin.random.Random

data class GlassElement(
    val id: String,
    val position: Offset,
    val size: Size,
    val scale: Float,
    val blur: Float,
    val centerDistortion: Float,
    val cornerRadius: Float,
    val elevation: Float,
    val tint: Color,
    val darkness: Float,
) {
    // Проверяем равенство с допуском для Float значений
    fun equalsWithTolerance(other: GlassElement): Boolean {
        if (id != other.id) return false

        val tolerance = 0.01f
        val positionDiff = (position - other.position)
        val positionDistance =
            kotlin.math.sqrt(positionDiff.x * positionDiff.x + positionDiff.y * positionDiff.y)
        return positionDistance < tolerance &&
                kotlin.math.abs(size.width - other.size.width) < tolerance &&
                kotlin.math.abs(size.height - other.size.height) < tolerance &&
                kotlin.math.abs(scale - other.scale) < tolerance &&
                kotlin.math.abs(blur - other.blur) < tolerance &&
                kotlin.math.abs(centerDistortion - other.centerDistortion) < tolerance &&
                kotlin.math.abs(cornerRadius - other.cornerRadius) < tolerance &&
                kotlin.math.abs(elevation - other.elevation) < tolerance &&
                kotlin.math.abs(darkness - other.darkness) < tolerance &&
                tint == other.tint
    }
}

interface GlassScope {
    fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp = 0.dp,
        tint: Color = Color.Transparent,
        darkness: Float = 0f,
    ): Modifier
}

interface GlassBoxScope : BoxScope, GlassScope

@Composable
fun GlassBoxScope.GlassBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    scale: Float = 0f,
    blur: Float = 0f,
    centerDistortion: Float = 0f,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    elevation: Dp = 0.dp,
    tint: Color = Color.Transparent,
    darkness: Float = 0f,
    content: @Composable BoxScope.() -> Unit = { },
) {
    val id = remember { Random.nextLong() }
    Box(
        modifier = modifier.glassBackground(
            id, scale, blur, centerDistortion, shape, elevation, tint, darkness
        ),
        contentAlignment, propagateMinConstraints, content
    )
}

private class GlassBoxScopeImpl(
    boxScope: BoxScope,
    glassScope: GlassScope
) : GlassBoxScope, BoxScope by boxScope,
    GlassScope by glassScope {

}

private class GlassScopeImpl(private val density: Density) : GlassScope {

    var updateCounter by mutableStateOf(0)
    val elements: MutableList<GlassElement> = mutableListOf()
    private val activeElements = mutableSetOf<String>()

    fun markElementAsActive(elementId: String) {
        activeElements.add(elementId)
    }

    fun cleanupInactiveElements() {
        val elementsToRemove = elements.filter { it.id !in activeElements }
        if (elementsToRemove.isNotEmpty()) {
            elements.removeAll { it.id !in activeElements }
            updateCounter++
        }
        activeElements.clear()
    }

    override fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp,
        tint: Color,
        darkness: Float,
    ): Modifier = this
        .background(color = Color.Transparent, shape = shape)
        .onGloballyPositioned { coordinates ->
            val elementId = "glass_$id"
            markElementAsActive(elementId)

            val position = coordinates.positionInRoot()
            val size = coordinates.size.toSize()

            val element = GlassElement(
                id = elementId,
                position = position,
                size = size,
                cornerRadius = shape.topStart.toPx(size, density),
                scale = scale,
                blur = blur,
                centerDistortion = centerDistortion,
                elevation = with(density) { elevation.toPx() },
                tint = tint,
                darkness = darkness,
            )

            // Находим существующий элемент с таким же ID
            val existingIndex = elements.indexOfFirst { it.id == element.id }

            // Обновляем только если элемент изменился
            if (existingIndex == -1) {
                // Новый элемент
                elements.add(element)
                updateCounter++
            } else {
                // Проверяем, изменился ли элемент с допуском для Float значений
                val existing = elements[existingIndex]
                if (!existing.equalsWithTolerance(element)) {
                    elements[existingIndex] = element
                    updateCounter++
                }
            }
        }
}

@Composable
fun GlassContainer(
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val glassScope = remember { GlassScopeImpl(density) }

    // Пересоздаем шейдер при каждом изменении элементов
    val shader = remember(glassScope.updateCounter) {
        RuntimeShader(GLASS_DISPLACEMENT_SHADER)
    }

    // Очищаем неактивные элементы при каждой композиции
    SideEffect {
        glassScope.cleanupInactiveElements()
    }

    // Очищаем элементы при выходе из композиции
    DisposableEffect(Unit) {
        onDispose {
            glassScope.elements.clear()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                shader.setFloatUniform("resolution", size.width, size.height)
                val a = glassScope.updateCounter

                // Получаем текущие элементы
                val elements = glassScope.elements.also { println(it) }

                // Ограничиваем количество элементов и очищаем массивы
                val maxElements = 10
                val positions = FloatArray(maxElements * 2)
                val sizes = FloatArray(maxElements * 2)
                val scales = FloatArray(maxElements)
                val radii = FloatArray(maxElements)
                val elevations = FloatArray(maxElements)
                val centerDistortions = FloatArray(maxElements)
                val tints = FloatArray(maxElements * 4)
                val darkness = FloatArray(maxElements)
                val blurs = FloatArray(maxElements)

                val elementsCount = minOf(elements.size, maxElements)
                shader.setIntUniform("elementsCount", elementsCount)

                for (i in 0 until elementsCount) {
                    val element = elements[i]
                    positions[i * 2] = element.position.x
                    positions[i * 2 + 1] = element.position.y
                    sizes[i * 2] = element.size.width
                    sizes[i * 2 + 1] = element.size.height
                    scales[i] = element.scale
                    radii[i] = element.cornerRadius
                    elevations[i] = element.elevation
                    centerDistortions[i] = element.centerDistortion

                    tints[i * 4] = element.tint.red
                    tints[i * 4 + 1] = element.tint.green
                    tints[i * 4 + 2] = element.tint.blue
                    tints[i * 4 + 3] = element.tint.alpha

                    darkness[i] = element.darkness
                    blurs[i] = element.blur
                }

                // Всегда устанавливаем униформы, даже если массивы пустые
                shader.setFloatUniform("glassPositions", positions)
                shader.setFloatUniform("glassSizes", sizes)
                shader.setFloatUniform("glassScales", scales)
                shader.setFloatUniform("cornerRadii", radii)
                shader.setFloatUniform("elevations", elevations)
                shader.setFloatUniform("centerDistortions", centerDistortions)
                shader.setFloatUniform("glassTints", tints)
                shader.setFloatUniform("glassDarkness", darkness)
                shader.setFloatUniform("glassBlurs", blurs)

                // Применяем shader effect с blur внутри шейдера
                renderEffect = RenderEffect.createRuntimeShaderEffect(
                    shader, "contents"
                ).asComposeRenderEffect()
            }
    ) {
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
    uniform float centerDistortions[10];
    uniform float glassTints[40]; // 10 elements * 4 components (r,g,b,a)
    uniform float glassDarkness[10];
    uniform float glassBlurs[10];

    // High-quality box blur with 15x15 sampling pattern
    float4 calculateBoxBlur(float2 fragCoord, float blurRadius) {
        if (blurRadius <= 0.0) {
            return contents.eval(fragCoord);
        }
        
        // Convert blur from [0,1] to pixel radius (max 30 pixels)
        float actualBlurRadius = blurRadius * 30.0;
        
        float4 blurredColor = float4(0.0);
        float totalSamples = 0.0;
        
        // 15x15 box blur (225 samples total)
        for (int x = -7; x <= 7; x++) {
            for (int y = -7; y <= 7; y++) {
                float2 offset = float2(float(x), float(y)) * actualBlurRadius / 7.0;
                float2 sampleCoord = fragCoord + offset;
                
                blurredColor += contents.eval(sampleCoord);
                totalSamples += 1.0;
            }
        }
        
        return blurredColor / totalSamples;
    }

    // Simplified lens-aware blur using fixed constant loop
    float4 calculateLensAwareBlur(float2 fragCoord, float blurRadius) {
        if (blurRadius <= 0.0) {
            return contents.eval(fragCoord);
        }
        
        // Convert blur from [0,1] to pixel radius (max 30 pixels)
        float actualBlurRadius = blurRadius * 30.0;
        
        float4 blurredColor = float4(0.0);
        float totalSamples = 0.0;
        
        // Simplified 9x9 box blur to avoid shader complexity
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                float2 offset = float2(float(x), float(y)) * actualBlurRadius / 4.0;
                float2 sampleCoord = fragCoord + offset;
                
                // Apply basic lens distortion inline without loops
                float2 lensedCoord = sampleCoord;
                
                // Check first element only to avoid complex loops
                if (elementsCount > 0) {
                    float2 glassPosition = glassPositions[0];
                    float2 glassSize = glassSizes[0];
                    float cornerRadius = cornerRadii[0];
                    float glassScale = glassScales[0];
                    float centerDistort = centerDistortions[0];
                    
                    // Simplified lens effect inline
                    float2 lensCenter = glassPosition + glassSize * 0.5;
                    float2 localCoord = lensedCoord - lensCenter;
                    float2 halfSize = glassSize * 0.5;
                    
                    // Basic lens scaling without complex SDF
                    if (length(localCoord) < length(halfSize) && glassScale > 0.0) {
                        float scaleFactor = 1.0 + glassScale;
                        lensedCoord = lensCenter + localCoord / scaleFactor;
                    }
                }
                
                blurredColor += contents.eval(lensedCoord);
                totalSamples += 1.0;
            }
        }
        
        return blurredColor / totalSamples;
    }

    // Calculate lens effect using UV scaling
    float2 calculateLensEffect(float2 fragCoord, float2 glassPosition, float2 glassSize, float cornerRadius, float scale, float centerDistortion) {
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
            
            // Base scale factor from scale parameter
            float baseScaleFactor = 1.0 + scale;
            
            // Calculate distortion factor based on centerDistortion parameter
            float distortionFactor = 1.0;
            if (centerDistortion > 0.0) {
                // Create convex distortion profile - stronger in center, weaker at edges
                float convexProfile = 1.0 - smoothstep(0.0, 1.0, normalizedDist);
                
                // Apply center distortion: 
                // - At center: full scale + distortion
                // - At edges: just base scale
                distortionFactor = 1.0 + centerDistortion * convexProfile;
            }
            
            // Combine base scale with distortion
            float finalScaleFactor = baseScaleFactor * distortionFactor;
            
            // Apply scaling
            float2 newCoord = lensCenter + (fragCoord - lensCenter) / finalScaleFactor;
            
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
        float4 tintColor = float4(0.0);
        float darknessEffect = 0.0;
        float currentBlur = 0.0;

        
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
            
            // Calculate SDF for this element
            float2 absCoord = abs(localCoord);
            float2 rectSize = halfSize - float2(cornerRadius);
            float2 d = absCoord - rectSize;
            float outsideDistance = length(max(d, 0.0));
            float insideDistance = min(max(d.x, d.y), 0.0);
            float sdf = outsideDistance + insideDistance - cornerRadius;
            
            // Apply lens effect
            float centerDistort = centerDistortions[i];
            finalCoord = calculateLensEffect(finalCoord, glassPosition, glassSize, cornerRadius, glassScale, centerDistort);
            
            // Calculate elevation shadow if enabled
            if (elevation > 0.0) {
                float currentShadow = calculateElevationShadow(localCoord, halfSize, cornerRadius, elevation);
                shadowAlpha = max(shadowAlpha, currentShadow);
            }
            
            // Calculate rim highlight
            float currentRim = calculateRimHighlight(localCoord, halfSize, cornerRadius);
            rimHighlight = max(rimHighlight, currentRim);
            
            // Apply tint, lightness, and darkness effects inside the element
            if (sdf < 0.0) {
                // Extract tint color for this element
                float4 elementTint = float4(
                    glassTints[i * 4],
                    glassTints[i * 4 + 1], 
                    glassTints[i * 4 + 2],
                    glassTints[i * 4 + 3]
                );
                
                // Apply tint if it has alpha > 0
                if (elementTint.a > 0.0) {
                    tintColor = mix(tintColor, elementTint, elementTint.a);
                }
                
                // Apply darkness effects
                float currentDarkness = glassDarkness[i];
                
                // DARKNESS: Emanates from edges inward
                if (currentDarkness > 0.0) {
                    float minDimension = min(halfSize.x, halfSize.y);
                    float maxRadius = minDimension * 0.8; // 80% of element's smaller dimension
                    float distanceFromEdge = abs(sdf);
                    
                    if (distanceFromEdge < maxRadius) {
                        // Maximum intensity at edges, fades toward center
                        float darknessIntensity = (maxRadius - distanceFromEdge) / maxRadius;
                        darknessIntensity = smoothstep(0.0, 1.0, darknessIntensity);
                        darknessEffect = max(darknessEffect, currentDarkness * darknessIntensity);
                    }
                }
                
                // Track the blur level for this pixel
                currentBlur = max(currentBlur, glassBlurs[i]);
            }
        }
        
        // Sample the background: apply lens effects first, then blur
        float4 color;
        if (currentBlur <= 0.0) {
            // No blur - use pre-calculated lens-distorted coordinates
            color = contents.eval(finalCoord);
        } else {
            // Apply blur with lens effects
            color = calculateLensAwareBlur(fragCoord, currentBlur);
        }
        
        // Apply tint color blending
        if (tintColor.a > 0.0) {
            color.rgb = mix(color.rgb, tintColor.rgb, tintColor.a * 0.5);
        }
        
        // Apply darkness effect (darkening)
        if (darknessEffect > 0.0) {
            color.rgb = mix(color.rgb, float3(0.0), darknessEffect * 0.5);
        }
        
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
            float4 reflectedColor = calculateLensAwareBlur(fragCoord + reflectionOffset, currentBlur * 0.5);
            
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
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black, Color.Blue)
                        )
                    )
            ) {
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

            Row(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                this@GlassContainer.GlassBox(
                    modifier = Modifier.size(64.dp),
                    scale = 0.3f,
                    blur = 0f,
                    centerDistortion = 0f,
                    shape = CircleShape,
                    contentAlignment = Alignment.Center,
                    tint = Color.Blue.copy(alpha = 0.5f),
                ) {
                    Icon(Icons.Default.Add, null)
                }

                this@GlassContainer.GlassBox(
                    modifier = Modifier.size(200.dp, 300.dp),
                    scale = 0.5f,
                    blur = 0.5f,
                    centerDistortion = 0.5f,
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp,
                    darkness = 0.5f,
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Glass content")
                }

                this@GlassContainer.GlassBox(
                    modifier = Modifier.size(64.dp),
                    scale = 1f,
                    blur = 1.0f,
                    centerDistortion = 1f,
                    shape = CircleShape,
                    elevation = 6.dp,
                    tint = Color.Red.copy(alpha = 0.1f),
                    darkness = 0.3f,
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    )
}
