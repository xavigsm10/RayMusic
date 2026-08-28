package com.mrtdk.liquid_glass.ui.components

import android.os.Build
import androidx.annotation.OptIn
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.skydoves.cloudy.cloudy

/**
 * Reflejo de artwork invertido estilo Apple Music:
 * - Capa 1 (Fondo): Difuminado extremo horizontal de colores (debajo de la barra de progreso).
 * - Capa 2 (Media): Difuminado horizontal notable (56dp X, 14dp Y) activo hasta la barra de progreso.
 * - Capa 3 (Costura superior): Empalme suave isotrópico (12dp) en la unión exacta para evitar cualquier línea divisoria.
 * - Soporte para sincronización frame-perfect a 60 FPS con el reproductor principal.
 */
@OptIn(UnstableApi::class)
@Composable
fun GraduatedBlurArtwork(
    imageUrl: Any?,
    videoUrl: String? = null,
    modifier: Modifier = Modifier,
    blurRadiusX: Dp = 140.dp,
    blurRadiusY: Dp = 30.dp,
    horizontalDiffuseRadiusX: Dp = 36.dp,
    horizontalDiffuseRadiusY: Dp = 14.dp,
    frostedRadius: Dp = 12.dp,
    verticalScale: Float = -9.0f,
    pivotY: Float = 0f,
    horizontalScale: Float = 1.0f,
    blurTransitionEndFraction: Float = 0.28f,
    syncWithPlayer: ExoPlayer? = null,
    imageLoader: ImageLoader? = null
) {
    val isMirrored = verticalScale < 0f
    val absVerticalScale = kotlin.math.abs(verticalScale).coerceAtLeast(0.01f)

    val containerHeightState = remember { mutableIntStateOf(0) }
    val containerHeightPx = containerHeightState.intValue

    val density = LocalDensity.current
    val blurRadiusXPx = with(density) { blurRadiusX.toPx() }
    val blurRadiusYPx = with(density) { blurRadiusY.toPx() }
    val horizontalDiffuseXPx = with(density) { horizontalDiffuseRadiusX.toPx() }
    val horizontalDiffuseYPx = with(density) { horizontalDiffuseRadiusY.toPx() }
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
        // CAPA 1 (Fondo): Difuminado extremo horizontal de colores (debajo del slider)
        // =========================================================================
        val extremeBlurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

        if (!videoUrl.isNullOrBlank()) {
            AnimatedArtworkPlayer(
                videoUrl = videoUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .then(transformModifier)
                    .then(extremeBlurModifier),
                enableFrameCapture = false,
                isPaused = false,
                syncWithPlayer = syncWithPlayer
            )
        } else if (imageUrl is ImageBitmap) {
            Image(
                bitmap = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .then(transformModifier)
                    .then(extremeBlurModifier)
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
                    .then(extremeBlurModifier)
            )
        }

        // =========================================================================
        // CAPA 2 (Media): Difuminado horizontal notable hasta el slider
        // =========================================================================
        val horizontalBlurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                renderEffect = BlurEffect(
                    horizontalDiffuseXPx,
                    horizontalDiffuseYPx,
                    TileMode.Clamp
                )
            }
        } else {
            Modifier.cloudy(radius = 24)
        }

        val horizontalFadeModifier = Modifier
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
                            (endF - 0.04f).coerceAtLeast(0f) to Color.Black,
                            (endF + 0.06f).coerceAtMost(1f) to Color.Transparent,
                            1.0f to Color.Transparent
                        )
                    ),
                    blendMode = BlendMode.DstIn
                )
            }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(horizontalFadeModifier)
        ) {
            if (!videoUrl.isNullOrBlank()) {
                AnimatedArtworkPlayer(
                    videoUrl = videoUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(transformModifier)
                        .then(horizontalBlurModifier),
                    enableFrameCapture = false,
                    isPaused = false,
                    syncWithPlayer = syncWithPlayer
                )
            } else if (imageUrl is ImageBitmap) {
                Image(
                    bitmap = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(transformModifier)
                        .then(horizontalBlurModifier)
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
                        .then(horizontalBlurModifier)
                )
            }
        }

        // =========================================================================
        // CAPA 3 (Superior): Empalme isotrópico suave (12dp) en la costura exacta
        // Se desvanece suavemente en los primeros ~25dp hacia la difusión horizontal
        // =========================================================================
        val seamBlurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                renderEffect = BlurEffect(
                    frostedRadiusPx,
                    frostedRadiusPx,
                    TileMode.Clamp
                )
            }
        } else {
            Modifier.cloudy(radius = 12)
        }

        val seamFadeModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                val seamHeightPx = with(density) { 24.dp.toPx() }
                val h = size.height
                val seamFraction = if (h > 0) (seamHeightPx / h).coerceIn(0.02f, 0.12f) else 0.05f
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black,
                            seamFraction * 0.4f to Color.Black.copy(alpha = 0.8f),
                            seamFraction to Color.Transparent,
                            1.0f to Color.Transparent
                        )
                    ),
                    blendMode = BlendMode.DstIn
                )
            }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(seamFadeModifier)
        ) {
            if (!videoUrl.isNullOrBlank()) {
                AnimatedArtworkPlayer(
                    videoUrl = videoUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(transformModifier)
                        .then(seamBlurModifier),
                    enableFrameCapture = false,
                    isPaused = false,
                    syncWithPlayer = syncWithPlayer
                )
            } else if (imageUrl is ImageBitmap) {
                Image(
                    bitmap = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(transformModifier)
                        .then(seamBlurModifier)
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
                        .then(seamBlurModifier)
                )
            }
        }
    }
}
