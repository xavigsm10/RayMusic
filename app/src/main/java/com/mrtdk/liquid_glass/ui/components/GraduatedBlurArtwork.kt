package com.mrtdk.liquid_glass.ui.components

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.BlurEffect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.draw.drawWithContent
import com.skydoves.cloudy.cloudy

@Composable
fun GraduatedBlurArtwork(
    imageUrl: Any?,
    modifier: Modifier = Modifier,
    blurStartFraction: Float,
    blurFullFraction: Float,
    maxBlurRadius: Int,
    verticalScale: Float,
    pivotY: Float,
    lowResSize: Int = 150
) {
    Box(modifier = modifier) {
        val context = LocalContext.current

        // Capa A: Imagen base nítida (imagen normal, sin ningún efecto)
        if (imageUrl is ImageBitmap) {
            Image(
                bitmap = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.00f
                        scaleY = verticalScale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, pivotY)
                    }
            )
        } else if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.00f
                        scaleY = verticalScale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, pivotY)
                    }
            )
        }

        // Capa B: Imagen idéntica con blur extremo aplicada encima, enmascarada progresivamente
        val blendModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                val start = blurStartFraction.coerceIn(0f, 1f)
                val end = blurFullFraction.coerceIn(0f, 1f).coerceAtLeast(start + 0.01f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            start to Color.Transparent,
                            end to Color.Black,
                            1f to Color.Black
                        )
                    ),
                    blendMode = BlendMode.DstIn
                )
            }

        Box(modifier = blendModifier) {
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.graphicsLayer {
                    renderEffect = BlurEffect(maxBlurRadius.toFloat(), maxBlurRadius.toFloat(), TileMode.Clamp)
                }
            } else {
                Modifier.cloudy(radius = maxBlurRadius.coerceIn(1, 100))
            }

            if (imageUrl is ImageBitmap) {
                val lowResBitmap = remember(imageUrl, lowResSize) {
                    android.graphics.Bitmap.createScaledBitmap(
                        imageUrl.asAndroidBitmap(),
                        lowResSize,
                        lowResSize,
                        true
                    ).asImageBitmap()
                }
                Image(
                    bitmap = lowResBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.00f
                            scaleY = verticalScale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, pivotY)
                        }
                        .then(blurModifier)
                )
            } else if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .size(lowResSize, lowResSize) // Downsampling configurable para optimizar el rendimiento del blur
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.00f
                            scaleY = verticalScale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, pivotY)
                        }
                        .then(blurModifier)
                )
            }
        }
    }
}
