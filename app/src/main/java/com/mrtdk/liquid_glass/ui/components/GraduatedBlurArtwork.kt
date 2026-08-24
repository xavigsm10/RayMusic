package com.mrtdk.liquid_glass.ui.components

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.skydoves.cloudy.cloudy

/**
 * Reflejo de artwork estilo Apple Music:
 * - Capa 1 (Fondo): Difusión líquida profunda (blurRadius = 90dp) con desvanecimiento de luces en la barra,
 *   sumergiendo la zona inferior de los controles en los tonos oscuros profundos del artwork sin columnas claras.
 * - Capa 2 (Superior): Reflejo con difuminado idéntico al de la carátula superior (25dp) que nace al 100%
 *   en la base y se desvanece suavemente de forma progresiva hasta la barra de progreso.
 */
@Composable
fun GraduatedBlurArtwork(
    imageUrl: Any?,
    videoUrl: String? = null,
    modifier: Modifier = Modifier,
    blurRadiusX: Dp = 150.dp,
    blurRadiusY: Dp = 75.dp,
    frostedRadius: Dp = 25.dp,
    verticalScale: Float = -3.80f,
    pivotY: Float = 0f,
    horizontalScale: Float = 1.0f,
    blurTransitionEndFraction: Float = 0.28f,
    imageLoader: ImageLoader? = null
) {
    val isMirrored = verticalScale < 0f
    val absVerticalScale = kotlin.math.abs(verticalScale).coerceAtLeast(0.01f)

    val containerHeightState = remember { mutableIntStateOf(0) }
    val containerHeightPx = containerHeightState.intValue

    val density = LocalDensity.current
    val blurRadiusXPx = with(density) { blurRadiusX.toPx() }
    val blurRadiusYPx = with(density) { blurRadiusY.toPx() }
    val frostedRadiusPx = with(density) { frostedRadius.toPx() }

    Box(
        modifier = modifier.onSizeChanged { containerHeightState.intValue = it.height }
    ) {
        val context = LocalContext.current

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

        // =========================================================================
        // CAPA 1 (Fondo): Difusión líquida profunda con preservación de tonos oscuros
        // =========================================================================
        val deepBlurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                renderEffect = BlurEffect(
                    blurRadiusXPx,
                    blurRadiusYPx,
                    TileMode.Clamp
                )
            }
        } else {
            Modifier.cloudy(radius = 35)
        }

        if (imageUrl is ImageBitmap) {
            Image(
                bitmap = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .then(transformModifier)
                    .then(deepBlurModifier)
            )
        } else if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(false)
                    .build(),
                imageLoader = imageLoader ?: coil.compose.LocalImageLoader.current,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .then(transformModifier)
                    .then(deepBlurModifier)
            )
        }

        // =========================================================================
        // CAPA AMBIENTAL: Profundización de tonos oscuros debajo de la barra de progreso
        // Elimina cualquier proyección de luces/columnas claras sobre los controles
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val endF = blurTransitionEndFraction.coerceIn(0.05f, 1f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                endF * 0.90f to Color.Transparent,
                                endF + 0.12f to Color.Black.copy(alpha = 0.28f),
                                0.70f to Color.Black.copy(alpha = 0.55f),
                                1.0f to Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
                }
        )

        // =========================================================================
        // CAPA 2 (Superior): Reflejo con difuminado idéntico a la carátula (25dp) progresivo
        // =========================================================================
        val frostedBlurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                renderEffect = BlurEffect(
                    frostedRadiusPx,
                    frostedRadiusPx,
                    TileMode.Clamp
                )
            }
        } else {
            Modifier.cloudy(radius = 20)
        }

        val progressiveFadeModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                val endF = blurTransitionEndFraction.coerceIn(0.05f, 1f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black,
                            endF * 0.40f to Color.Black.copy(alpha = 0.80f),
                            endF * 0.75f to Color.Black.copy(alpha = 0.35f),
                            endF to Color.Transparent,
                            1.0f to Color.Transparent
                        )
                    ),
                    blendMode = BlendMode.DstIn
                )
            }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(progressiveFadeModifier)
        ) {
            if (imageUrl is ImageBitmap) {
                Image(
                    bitmap = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(transformModifier)
                        .then(frostedBlurModifier)
                )
            } else if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(false)
                        .build(),
                    imageLoader = imageLoader ?: coil.compose.LocalImageLoader.current,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(transformModifier)
                        .then(frostedBlurModifier)
                )
            }
        }
    }
}
