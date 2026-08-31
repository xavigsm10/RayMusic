package com.mrtdk.liquid_glass.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.imageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Reflejo de artwork invertido estilo Apple Music:
 * - Capa estática base siempre visible de forma inmediata (sin pantallas negras).
 * - Difuminado progresivo horizontal suave hacia el final de la pantalla (sliderThresholdDp).
 * - Preserva 100% de los efectos, contraste y posición tanto en imágenes estáticas como con video en movimiento.
 */
@OptIn(UnstableApi::class)
@Composable
fun GraduatedBlurArtwork(
    imageUrl: Any?,
    videoUrl: String? = null,
    modifier: Modifier = Modifier,
    mildBlurRadiusX: Dp = 40.dp,
    mildBlurRadiusY: Dp = 14.dp,
    strongBlurRadiusX: Dp = 160.dp,
    strongBlurRadiusY: Dp = 16.dp,
    sliderThresholdDp: Dp = 50.dp,
    verticalScale: Float = -4.0f,
    pivotY: Float = 0f,
    horizontalScale: Float = 1.0f,
    horizontalBias: Float = 0.0f,
    imageScale: Float = 1.0f,
    scaleOriginX: Float = 0.5f,
    scaleOriginY: Float = 0.0f,
    blurTransitionEndFraction: Float = 0.28f,
    syncWithPlayer: ExoPlayer? = null,
    imageLoader: ImageLoader? = null
) {
    val isMirrored = verticalScale < 0f
    val absVerticalScale = kotlin.math.abs(verticalScale).coerceAtLeast(0.01f)

    val containerHeightState = remember { mutableIntStateOf(0) }
    val containerHeightPx = containerHeightState.intValue

    Box(
        modifier = modifier.onSizeChanged { containerHeightState.intValue = it.height }
    ) {
        val density = LocalDensity.current

        val transformModifier = Modifier.graphicsLayer {
            scaleX = horizontalScale
            scaleY = verticalScale
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                0.5f,
                pivotY
            )

            // Tras la inversión vertical, alinea el borde superior del reflejo
            // exactamente con el borde inferior de la carátula superior.
            if (isMirrored && containerHeightPx > 0) {
                translationY = containerHeightPx * absVerticalScale
            }
        }

        val internalZoomModifier = Modifier.graphicsLayer {
            scaleX = imageScale
            scaleY = imageScale
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(scaleOriginX, scaleOriginY)
        }

        val artworkAlignment = BiasAlignment(horizontalBias = horizontalBias, verticalBias = -1.0f)

        // 1. Capa Base: Difuminado moderado (inmediato, 0 retardo)
        StaticArtworkLayer(
            imageUrl = imageUrl,
            artworkAlignment = artworkAlignment,
            transformModifier = transformModifier,
            internalZoomModifier = internalZoomModifier,
            blurRadiusX = mildBlurRadiusX,
            blurRadiusY = mildBlurRadiusY,
            imageLoader = imageLoader
        )

        // 2. Capa Superior: Difuminado fuerte en gradación suave hacia los controles
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    val thresholdPx = with(density) { sliderThresholdDp.toPx() }
                    val h = size.height
                    if (h > 0f) {
                        val tFrac = (thresholdPx / h).coerceIn(0.05f, 0.6f)
                        val startFrac = (tFrac * 0.60f).coerceIn(0f, 1f)
                        val midFrac = (tFrac * 1.05f).coerceIn(startFrac, 1f)
                        val endFrac = (tFrac * 1.50f).coerceIn(midFrac, 1f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                startFrac to Color.Transparent,
                                midFrac to Color.Black.copy(alpha = 0.55f),
                                endFrac to Color.Black,
                                1.0f to Color.Black
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        ) {
            StaticArtworkLayer(
                imageUrl = imageUrl,
                artworkAlignment = artworkAlignment,
                transformModifier = transformModifier,
                internalZoomModifier = internalZoomModifier,
                blurRadiusX = strongBlurRadiusX,
                blurRadiusY = strongBlurRadiusY,
                imageLoader = imageLoader
            )
        }
    }
}

@Composable
private fun StaticArtworkLayer(
    imageUrl: Any?,
    artworkAlignment: Alignment,
    transformModifier: Modifier,
    internalZoomModifier: Modifier,
    blurRadiusX: Dp = 0.dp,
    blurRadiusY: Dp = 0.dp,
    imageLoader: ImageLoader? = null
) {
    val context = LocalContext.current
    val baseModifier = Modifier
        .fillMaxSize()
        .then(transformModifier)
        .then(internalZoomModifier)
        .let {
            if (blurRadiusX > 0.dp || blurRadiusY > 0.dp) {
                it.blur(blurRadiusX, blurRadiusY, edgeTreatment = BlurredEdgeTreatment.Rectangle)
            } else {
                it
            }
        }

    if (imageUrl is ImageBitmap) {
        Image(
            bitmap = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
            alignment = artworkAlignment,
            modifier = baseModifier
        )
    } else if (imageUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(false)
                .build(),
            imageLoader = imageLoader ?: context.imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
            alignment = artworkAlignment,
            modifier = baseModifier
        )
    }
}
