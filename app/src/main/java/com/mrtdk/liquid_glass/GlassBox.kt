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
    val warpEdges: Float,
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
                kotlin.math.abs(warpEdges - other.warpEdges) < tolerance &&
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
        warpEdges: Float = 0f,
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
    warpEdges: Float = 0f,
    content: @Composable BoxScope.() -> Unit = { },
) {
    val id = remember { Random.nextLong() }
    Box(
        modifier = modifier.glassBackground(
            id, scale, blur, centerDistortion, shape, elevation, tint, darkness, warpEdges
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
        warpEdges: Float,
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
                warpEdges = warpEdges,
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
                val warpEdges = FloatArray(maxElements)
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
                    warpEdges[i] = element.warpEdges
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
                shader.setFloatUniform("glassWarpEdges", warpEdges)
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
    uniform float glassWarpEdges[10];
    uniform float glassBlurs[10];

    // Calculate warp region boundaries
    // Returns 0.0 if pixel is in inner region (no warp)
    // Returns 1.0 if pixel is in warp region
    // warpEdges: 0.0 = no warp region, 1.0 = warp region extends to 50% between edge and center
    float calculateWarpRegion(float2 localCoord, float2 halfSize, float cornerRadius, float warpEdges) {
        if (warpEdges <= 0.0) return 0.0;
        
        // Calculate SDF for the outer element boundary
        float2 absCoord = abs(localCoord);
        float2 rectSize = halfSize - float2(cornerRadius);
        float2 d = absCoord - rectSize;
        float outsideDistance = length(max(d, 0.0));
        float insideDistance = min(max(d.x, d.y), 0.0);
        float outerSdf = outsideDistance + insideDistance - cornerRadius;
        
        // Only process pixels inside the element
        if (outerSdf >= 0.0) return 0.0;
        
        // Calculate the inset distance based on the smaller dimension
        float minDimension = min(halfSize.x, halfSize.y);
        float maxInset = minDimension * 0.5; // Maximum 50% of the smaller dimension radius
        float currentInset = warpEdges * maxInset;
        
        // Create inner boundary by shrinking the element uniformly
        float2 innerHalfSize = halfSize - float2(currentInset);
        
        // Calculate inner corner radius proportionally
        // If the inner area is smaller, the corner radius should be proportionally smaller
        float scaleFactor = min(innerHalfSize.x / halfSize.x, innerHalfSize.y / halfSize.y);
        float innerCornerRadius = cornerRadius * scaleFactor;
        
        // Make sure inner corner radius doesn't become negative and isn't larger than the inner dimensions
        innerCornerRadius = max(0.0, min(innerCornerRadius, min(innerHalfSize.x, innerHalfSize.y)));
        
        // Calculate SDF for the inner boundary
        float2 innerRectSize = innerHalfSize - float2(innerCornerRadius);
        float2 innerD = absCoord - innerRectSize;
        float innerOutsideDistance = length(max(innerD, 0.0));
        float innerInsideDistance = min(max(innerD.x, innerD.y), 0.0);
        float innerSdf = innerOutsideDistance + innerInsideDistance - innerCornerRadius;
        
        // Return 1.0 if in warp region (between outer and inner boundaries), 0.0 if in inner region
        return innerSdf > 0.0 ? 1.0 : 0.0;
    }

    // Calculate warp distortion for edge areas
    // Pulls pixels from inner region and creates barrel distortion effect
    float2 calculateWarpDistortion(float2 localCoord, float2 halfSize, float cornerRadius, float warpEdges) {
        if (warpEdges <= 0.0) return localCoord;
        
        // Calculate the inset distance and inner boundaries (same as in calculateWarpRegion)
        float minDimension = min(halfSize.x, halfSize.y);
        float maxInset = minDimension * 0.5;
        float currentInset = warpEdges * maxInset;
        float2 innerHalfSize = halfSize - float2(currentInset);
        
        float scaleFactor = min(innerHalfSize.x / halfSize.x, innerHalfSize.y / halfSize.y);
        float innerCornerRadius = cornerRadius * scaleFactor;
        innerCornerRadius = max(0.0, min(innerCornerRadius, min(innerHalfSize.x, innerHalfSize.y)));
        
        // Calculate SDF for the inner boundary
        float2 absCoord = abs(localCoord);
        float2 innerRectSize = innerHalfSize - float2(innerCornerRadius);
        float2 innerD = absCoord - innerRectSize;
        float innerOutsideDistance = length(max(innerD, 0.0));
        float innerInsideDistance = min(max(innerD.x, innerD.y), 0.0);
        float innerSdf = innerOutsideDistance + innerInsideDistance - innerCornerRadius;
        
        // Only apply distortion in warp region
        if (innerSdf <= 0.0) return localCoord;
        
        // Calculate distance from inner boundary (normalized)
        float maxWarpDistance = currentInset;
        float warpDistance = min(innerSdf, maxWarpDistance);
        float normalizedWarpDistance = warpDistance / maxWarpDistance; // 0.0 at inner boundary, 1.0 at outer edge
        
        // Create radial distortion vector from center
        float2 centerDirection = normalize(localCoord);
        float distanceFromCenter = length(localCoord);
        
        // Calculate warp intensity (stronger near outer edges)
        float warpIntensity = normalizedWarpDistance * normalizedWarpDistance; // Quadratic falloff
        warpIntensity *= warpEdges * 2.0; // Scale by warp parameter
        
        // Create barrel distortion effect - pull pixels from inner area
        // The idea is to map current position to a position closer to the inner boundary
        float pullStrength = warpIntensity * 0.8; // How much to pull towards inner area
        
        // Calculate the direction to the nearest point on inner boundary
        float2 nearestInnerPoint = localCoord;
        
        // Simple approach: scale down the coordinate towards the inner boundary
        float targetScale = 1.0 - pullStrength;
        targetScale = max(0.1, targetScale); // Prevent complete collapse
        
        float2 pulledCoord = localCoord * targetScale;
        
        // Add some radial distortion for more realistic lens effect
        float radialDistortion = warpIntensity * 0.3;
        float2 radialOffset = centerDirection * radialDistortion * distanceFromCenter * 0.1;
        
        // For strong warp, add some swirl/reflection effects
        if (warpEdges > 0.7 && normalizedWarpDistance > 0.8) {
            float angle = atan(localCoord.y, localCoord.x);
            float swirl = normalizedWarpDistance * warpEdges * 0.5;
            angle += swirl;
            float r = length(pulledCoord);
            pulledCoord = float2(cos(angle), sin(angle)) * r;
        }
        
        return pulledCoord + radialOffset;
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
        float rimWidth = 5.0;
        if (sdf > 0.0 && sdf < rimWidth) {
            // Basic intensity falls off from edge outward
            float intensity = (rimWidth - sdf) / rimWidth;
            
            // Calculate vertical position factor (-1 at top, +1 at bottom)
            float verticalPos = localCoord.y / halfSize.y;
            
            // Lighting factor: stronger at top (negative Y), weaker at bottom (positive Y)
            // Map from [-1, 1] to [1.2, 0.7] - stronger highlight at top, weaker at bottom
            float lightingFactor = mix(1.2, 0.7, (verticalPos + 1.0) * 0.5);
            
            return intensity * 0.8 * lightingFactor;
        }
        
        return 0.0;
    }

    float4 main(float2 fragCoord) {
        float2 finalCoord = fragCoord;
        float shadowAlpha = 0.0;
        float rimHighlight = 0.0;
        float4 tintColor = float4(0.0);
        float darknessEffect = 0.0;
        float blurRadius = 0.0;

        
        // Apply lens effects and calculate elevation shadows for each glass element
        for (int i = 0; i < 10; i++) {
            if (i >= elementsCount) break;
            
            float2 glassPosition = glassPositions[i];
            float2 glassSize = glassSizes[i];
            float cornerRadius = cornerRadii[i];
            float glassScale = glassScales[i];
            float elevation = elevations[i];
            float warpEdges = glassWarpEdges[i];
            float blur = glassBlurs[i];
            
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
            
            // Apply blur for this element if we're inside it
            if (sdf < 0.0 && blur > 0.0) {
                blurRadius = max(blurRadius, blur * 20.0); // Scale blur value
            }
            
            // Check if we're in a warp region
            float warpRegionFactor = calculateWarpRegion(localCoord, halfSize, cornerRadius, warpEdges);
            if (warpRegionFactor > 0.0) {
                // Apply warp distortion in warp region
                float2 warpedCoord = calculateWarpDistortion(localCoord, halfSize, cornerRadius, warpEdges);
                float2 warpedFragCoord = lensCenter + warpedCoord;
                
                // Apply lens effect to the warped coordinates
                float centerDistort = centerDistortions[i];
                finalCoord = calculateLensEffect(warpedFragCoord, glassPosition, glassSize, cornerRadius, glassScale, centerDistort);
            } else {
                // Apply lens effect only to non-warp regions (inner region)
                float centerDistort = centerDistortions[i];
                finalCoord = calculateLensEffect(finalCoord, glassPosition, glassSize, cornerRadius, glassScale, centerDistort);
            }
            
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
            }
        }
        
        // Sample the background with modified coordinates
        float4 color = contents.eval(finalCoord);
        
        // Apply blur effect if needed
        if (blurRadius > 0.0) {
            float4 blurredColor = float4(0.0);
            float totalWeight = 0.0;
            
            // Enhanced blur implementation with more samples
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -5; dy <= 5; dy++) {
                    float2 sampleCoord = finalCoord + float2(float(dx), float(dy)) * blurRadius / 5.0;
                    // Gaussian-like weight falloff for better quality
                    float distance = sqrt(float(dx * dx + dy * dy));
                    float weight = exp(-distance * distance / 8.0);
                    blurredColor += contents.eval(sampleCoord) * weight;
                    totalWeight += weight;
                }
            }
            color = blurredColor / totalWeight;
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
            float2 reflectionOffset = surfaceNormal * 24.0;
            float4 reflectedColor = contents.eval(fragCoord + reflectionOffset);
            
            // Brighten the reflected color to create highlight effect with minimum intensity
            reflectedColor.rgb = reflectedColor.rgb * 1.8 + 0.35; 
            
            // Ensure minimum highlight intensity even on dark backgrounds
            float minHighlightIntensity = 0.15; // Minimum highlight brightness
            reflectedColor.rgb = max(reflectedColor.rgb, float3(minHighlightIntensity));
            
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
                    modifier = Modifier.size(80.dp),
                    scale = 0.3f,
                    blur = 0.0f,
                    centerDistortion = 0.2f,
                    shape = CircleShape,
                    contentAlignment = Alignment.Center,
                    tint = Color.Blue.copy(alpha = 0.5f),
                    warpEdges = 0.5f, // Test barrel distortion
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
                    warpEdges = 0.7f, // Strong barrel distortion
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Glass content")
                }

                this@GlassContainer.GlassBox(
                    modifier = Modifier.size(80.dp),
                    scale = 1f,
                    blur = 1f,
                    centerDistortion = 1f,
                    shape = CircleShape,
                    elevation = 6.dp,
                    tint = Color.Red.copy(alpha = 0.1f),
                    darkness = 0.3f,
                    warpEdges = 0.9f, // Maximum barrel distortion with swirl effects
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    )
}
