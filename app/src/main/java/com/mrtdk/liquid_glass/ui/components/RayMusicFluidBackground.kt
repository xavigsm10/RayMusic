package com.mrtdk.liquid_glass.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * RayMusic Fluid Background
 *
 * Motor de fondo fluido en movimiento en tiempo real para RayMusic.
 * Emula la estética y comportamiento del shader dinámico de Glassy Music (@kawarp/core)
 * extrayendo y procesando los tonos vivos de la carátula:
 * - Algoritmo de control de luminosidad (Luminance Guard) para que nunca deslumbres
 *   y mantenga siempre legibles las letras blancas.
 * - Malla de fluidos multicapa basada en cintas cúbicas sinuosas (Cubic Bezier Ribbons)
 *   y orbes radiales con 5 osciladores de fase no armónicos continuos.
 * - Difusión gaussiana profunda (56dp) con borde no acotado.
 * - Transición de color suave (700ms) entre cambios de canción.
 */
@Composable
fun RayMusicFluidBackground(
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    // 1. Algoritmo de Luminancia y Contraste (idéntico al de album-color-theme de Glassy)
    val safePrimary = remember(primaryColor) {
        processColor(primaryColor, defaultFallback = Color(0xFF2C3E50))
    }
    val safeSecondary = remember(secondaryColor, safePrimary) {
        if (secondaryColor == Color.Transparent || secondaryColor == Color.Black || secondaryColor == primaryColor) {
            Color(
                red = (safePrimary.red * 0.85f + 0.10f).coerceIn(0f, 1f),
                green = (safePrimary.green * 0.70f + 0.08f).coerceIn(0f, 1f),
                blue = (safePrimary.blue * 0.60f + 0.15f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else {
            processColor(secondaryColor, defaultFallback = safePrimary)
        }
    }
    val safeAccent = remember(accentColor, safeSecondary, safePrimary) {
        if (accentColor == Color.Transparent || accentColor == Color.Black || accentColor == primaryColor) {
            Color(
                red = (safeSecondary.red * 0.75f + safePrimary.red * 0.25f).coerceIn(0f, 1f),
                green = (safeSecondary.green * 0.80f + 0.05f).coerceIn(0f, 1f),
                blue = (safeSecondary.blue * 0.90f + 0.10f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else {
            processColor(accentColor, defaultFallback = safeSecondary)
        }
    }

    // Color de resalte orgánico para las crestas del fluido
    val safeHighlight = remember(safePrimary, safeSecondary) {
        Color(
            red = (safePrimary.red * 0.65f + safeSecondary.red * 0.35f + 0.08f).coerceIn(0f, 1f),
            green = (safePrimary.green * 0.65f + safeSecondary.green * 0.35f + 0.08f).coerceIn(0f, 1f),
            blue = (safePrimary.blue * 0.65f + safeSecondary.blue * 0.35f + 0.06f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    // Transición suave entre cambios de pista (Crossfade fluido de colores)
    val animPrimary by animateColorAsState(safePrimary, tween(750, easing = FastOutSlowInEasing), label = "fluidPrimary")
    val animSecondary by animateColorAsState(safeSecondary, tween(750, easing = FastOutSlowInEasing), label = "fluidSecondary")
    val animAccent by animateColorAsState(safeAccent, tween(750, easing = FastOutSlowInEasing), label = "fluidAccent")
    val animHighlight by animateColorAsState(safeHighlight, tween(750, easing = FastOutSlowInEasing), label = "fluidHighlight")

    // 2. Osciladores de Fase Continua (5 frecuencias no armónicas para movimiento caótico natural)
    val infiniteTransition = rememberInfiniteTransition(label = "raymusic_fluid_motion")
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase_t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase_t2"
    )
    val t3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(23000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase_t3"
    )
    val t4 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase_t4"
    )
    val t5 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(27000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase_t5"
    )

    // Pulso sutil reactivo si está reproduciéndose música
    val pulseT by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fluid_pulse"
    )
    val pulseFactor = if (isPlaying) (1f + 0.04f * pulseT) else 1f

    val path1 = remember { Path() }
    val path2 = remember { Path() }
    val path3 = remember { Path() }
    val path4 = remember { Path() }

    val colorsRibbon1 = remember(animPrimary, animHighlight, animSecondary) {
        listOf(
            animPrimary.copy(alpha = 0.65f),
            animHighlight.copy(alpha = 0.55f),
            animSecondary.copy(alpha = 0.40f)
        )
    }
    val colorsRibbon2 = remember(animSecondary, animPrimary, animAccent) {
        listOf(
            animSecondary.copy(alpha = 0.60f),
            animPrimary.copy(alpha = 0.50f),
            animAccent.copy(alpha = 0.40f)
        )
    }
    val colorsRibbon3 = remember(animAccent, animSecondary, animHighlight) {
        listOf(
            animAccent.copy(alpha = 0.55f),
            animSecondary.copy(alpha = 0.45f),
            animHighlight.copy(alpha = 0.35f)
        )
    }
    val colorsRibbon4 = remember(animHighlight, animPrimary, animAccent) {
        listOf(
            animHighlight.copy(alpha = 0.50f),
            animPrimary.copy(alpha = 0.45f),
            animAccent.copy(alpha = 0.40f)
        )
    }
    val colorsOrb = remember(animHighlight, animSecondary) {
        listOf(
            animHighlight.copy(alpha = 0.40f),
            animSecondary.copy(alpha = 0.22f),
            Color.Transparent
        )
    }

    Box(modifier = modifier) {
        // Capa 1: Base oscura atmosférica (Negro cósmico profundo con gradiente ambiental)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF07080B))
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to animPrimary.copy(alpha = 0.40f),
                    0.45f to animSecondary.copy(alpha = 0.28f),
                    1.0f to animAccent.copy(alpha = 0.20f)
                )
            )
        }

        // Capa 2: Malla de cintas sinuosas fluidas (Domain Warping) con desenfoque optimizado
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(48.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val strokeRibbon = w * 0.54f * pulseFactor

            // Cinta 1: Flujo superior sinuoso oblicuo (Primary -> Highlight -> Secondary)
            val p1x0 = -w * 0.20f
            val p1y0 = h * (0.16f + 0.10f * sin(t1.toDouble()).toFloat())
            val p1c1x = w * (0.28f + 0.20f * cos(t2.toDouble()).toFloat())
            val p1c1y = h * (0.06f + 0.14f * sin((t1 * 0.85f).toDouble()).toFloat())
            val p1c2x = w * (0.68f + 0.18f * sin(t3.toDouble()).toFloat())
            val p1c2y = h * (0.36f + 0.12f * cos((t2 * 0.75f).toDouble()).toFloat())
            val p1x1 = w * 1.20f
            val p1y1 = h * (0.20f + 0.12f * sin((t4 * 0.90f).toDouble()).toFloat())

            path1.rewind()
            path1.moveTo(p1x0, p1y0)
            path1.cubicTo(p1c1x, p1c1y, p1c2x, p1c2y, p1x1, p1y1)
            drawPath(
                path = path1,
                brush = Brush.linearGradient(
                    colors = colorsRibbon1,
                    start = Offset(p1x0, p1y0),
                    end = Offset(p1x1, p1y1)
                ),
                style = Stroke(width = strokeRibbon, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Cinta 2: Flujo medio contra-rotativo (Secondary -> Primary -> Accent)
            val p2x0 = w * 1.20f
            val p2y0 = h * (0.44f + 0.11f * cos((t2 * 0.85f).toDouble()).toFloat())
            val p2c1x = w * (0.76f + 0.20f * sin(t1.toDouble()).toFloat())
            val p2c1y = h * (0.26f + 0.15f * cos(t3.toDouble()).toFloat())
            val p2c2x = w * (0.26f + 0.18f * cos((t4 * 0.70f).toDouble()).toFloat())
            val p2c2y = h * (0.60f + 0.13f * sin((t2 * 0.95f).toDouble()).toFloat())
            val p2x1 = -w * 0.20f
            val p2y1 = h * (0.46f + 0.11f * cos((t1 * 0.75f).toDouble()).toFloat())

            path2.rewind()
            path2.moveTo(p2x0, p2y0)
            path2.cubicTo(p2c1x, p2c1y, p2c2x, p2c2y, p2x1, p2y1)
            drawPath(
                path = path2,
                brush = Brush.linearGradient(
                    colors = colorsRibbon2,
                    start = Offset(p2x0, p2y0),
                    end = Offset(p2x1, p2y1)
                ),
                style = Stroke(width = strokeRibbon * 1.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Cinta 3: Flujo inferior profundo (Accent -> Secondary -> Highlight)
            val p3x0 = -w * 0.20f
            val p3y0 = h * (0.74f + 0.12f * sin(t3.toDouble()).toFloat())
            val p3c1x = w * (0.30f + 0.22f * cos((t4 * 0.85f).toDouble()).toFloat())
            val p3c1y = h * (0.88f + 0.10f * sin(t2.toDouble()).toFloat())
            val p3c2x = w * (0.72f + 0.17f * sin((t1 * 0.65f).toDouble()).toFloat())
            val p3c2y = h * (0.62f + 0.14f * cos(t3.toDouble()).toFloat())
            val p3x1 = w * 1.20f
            val p3y1 = h * (0.82f + 0.10f * sin((t2 * 0.85f).toDouble()).toFloat())

            path3.rewind()
            path3.moveTo(p3x0, p3y0)
            path3.cubicTo(p3c1x, p3c1y, p3c2x, p3c2y, p3x1, p3y1)
            drawPath(
                path = path3,
                brush = Brush.linearGradient(
                    colors = colorsRibbon3,
                    start = Offset(p3x0, p3y0),
                    end = Offset(p3x1, p3y1)
                ),
                style = Stroke(width = strokeRibbon, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Cinta 4: Diagonal sinuosa que entreteje los cuadrantes
            val p4x0 = w * (0.45f + 0.18f * sin((t2 * 0.65f).toDouble()).toFloat())
            val p4y0 = -h * 0.10f
            val p4c1x = w * (0.20f + 0.22f * cos(t1.toDouble()).toFloat())
            val p4c1y = h * (0.36f + 0.11f * sin(t4.toDouble()).toFloat())
            val p4c2x = w * (0.80f + 0.16f * sin(t3.toDouble()).toFloat())
            val p4c2y = h * (0.64f + 0.12f * cos((t1 * 0.90f).toDouble()).toFloat())
            val p4x1 = w * (0.50f + 0.20f * cos(t2.toDouble()).toFloat())
            val p4y1 = h * 1.10f

            path4.rewind()
            path4.moveTo(p4x0, p4y0)
            path4.cubicTo(p4c1x, p4c1y, p4c2x, p4c2y, p4x1, p4y1)
            drawPath(
                path = path4,
                brush = Brush.linearGradient(
                    colors = colorsRibbon4,
                    start = Offset(p4x0, p4y0),
                    end = Offset(p4x1, p4y1)
                ),
                style = Stroke(width = strokeRibbon * 0.88f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Orbe radial pulsante central (Atmospheric Glow)
            val orbCenterX = w * (0.50f + 0.16f * cos((t5 * 0.70f).toDouble()).toFloat())
            val orbCenterY = h * (0.50f + 0.14f * sin((t5 * 0.85f).toDouble()).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = colorsOrb,
                    center = Offset(orbCenterX, orbCenterY),
                    radius = w * 0.60f * pulseFactor
                ),
                center = Offset(orbCenterX, orbCenterY),
                radius = w * 0.60f * pulseFactor
            )
        }

        // Capa 3: Viñeta de contraste sutil superior e inferior
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.35f),
                    0.18f to Color.Transparent,
                    0.72f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.55f)
                )
            )
        }
    }
}

/**
 * Normaliza y oscurece dinámicamente un color para asegurar legibilidad
 * sin alterar la profundidad natural de carátulas oscuras (Idéntico a album-color-theme de Glassy).
 */
private fun processColor(color: Color, defaultFallback: Color): Color {
    if (color == Color.Transparent || color == Color.Black) {
        return defaultFallback
    }

    var r = color.red
    var g = color.green
    var b = color.blue

    // Regla de Oro (idéntica a album-color-theme-modded de Glassy):
    // Si la luminancia supera 0.45f, se oscurece progresivamente para asegurar que el texto blanco siempre tenga contraste perfecto.
    // Los colores oscuros naturales se respetan para mantener el fondo negro cósmico profundo de la carátula.
    var currLum = Color(r, g, b, 1f).luminance()
    var factor = 1.0f
    while (currLum > 0.45f && factor > 0.30f) {
        factor *= 0.88f
        r *= 0.88f
        g *= 0.88f
        b *= 0.88f
        currLum = Color(r, g, b, 1f).luminance()
    }

    return Color(r, g, b, 1f)
}
