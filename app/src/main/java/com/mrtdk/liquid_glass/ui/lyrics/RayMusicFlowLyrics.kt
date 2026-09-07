package com.mrtdk.liquid_glass.ui.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.Font
import com.mrtdk.liquid_glass.R
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mrtdk.liquid_glass.ui.screens.timeMs
import com.mrtdk.liquid_glass.ui.screens.text
import com.mrtdk.liquid_glass.ui.screens.translationText
import kotlin.math.abs
import kotlin.math.sin

/**
 * Familia tipográfica oficial de Glassy Music (Satoshi Variable / Geometry Grotesque)
 * Instanciada directamente desde Satoshi-Variable oficial de Glassy Music:
 * - SatoshiBlack: peso 900 Ultra-Bold contundente para la línea activa
 * - SatoshiBold: peso 700 Bold para líneas secundarias
 * - SatoshiMedium: peso 500 para traducciones y metadatos
 */
val SatoshiBlack = FontFamily(Font(R.font.satoshi_black))
val SatoshiBold = FontFamily(Font(R.font.satoshi_bold))
val SatoshiMedium = FontFamily(Font(R.font.satoshi_medium))

val SatoshiFontFamily = FontFamily(
    Font(R.font.satoshi_black, FontWeight.Black),
    Font(R.font.satoshi_black, FontWeight.ExtraBold),
    Font(R.font.satoshi_black, FontWeight.W900),
    Font(R.font.satoshi_black, FontWeight.W800),
    Font(R.font.satoshi_bold, FontWeight.Bold),
    Font(R.font.satoshi_bold, FontWeight.SemiBold),
    Font(R.font.satoshi_bold, FontWeight.W700),
    Font(R.font.satoshi_bold, FontWeight.W600),
    Font(R.font.satoshi_bold, FontWeight.Normal),
    Font(R.font.satoshi_bold, FontWeight.W400),
    Font(R.font.satoshi_medium, FontWeight.Medium),
    Font(R.font.satoshi_medium, FontWeight.W500)
)

/**
 * Modelos de elementos para la lista de RayMusic Flow (Líneas y Pausas Instrumentales)
 */
sealed class RayMusicFlowItem {
    data class Line(val originalIndex: Int, val syncedLine: ISyncedLine) : RayMusicFlowItem()
    data class InstrumentalBreak(val startMs: Long, val endMs: Long, val seekTargetMs: Long) : RayMusicFlowItem()
}

/**
 * Calcula con precisión la duración de canto de una línea y si debe seguirle un InstrumentalBreak.
 * Réplica exacta de `instrumentalBreaks.ts` y `lyricFixers` de Glassy Music:
 * - Si es KaraokeLine con sílabas, la duración es el final de la última sílaba.
 * - Si es LRC con timestamp por línea, estima la duración natural según conteo de palabras y caracteres (~480ms por palabra + 30ms por carácter).
 * - Si tras cantar la línea completa resta un silencio (silenceGap) >= 4800ms antes de la siguiente línea,
 *   se crea una pausa instrumental independiente (`··· ♪`).
 * - Si no hay silencio suficiente, la duración de canto se distribuye holgadamente sobre el intervalo sin cortar la frase.
 */
internal fun calculateLineSingingDuration(
    line: ISyncedLine,
    nextLineTimeMs: Long?
): Pair<Long, Boolean> {
    if (line is KaraokeLine && line.syllables.isNotEmpty()) {
        val syllablesEnd = line.syllables.maxOf { it.end }
        val singingDuration = (syllablesEnd - line.timeMs).coerceAtLeast(1200L)
        if (nextLineTimeMs != null && nextLineTimeMs > line.timeMs) {
            val totalGap = nextLineTimeMs - line.timeMs
            val silenceGap = nextLineTimeMs - (line.timeMs + singingDuration)
            if (silenceGap >= 3200L) {
                return Pair(singingDuration, true)
            } else {
                return Pair(minOf(singingDuration + 300L, totalGap), false)
            }
        }
        return Pair(singingDuration, false)
    }

    val words = line.text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val wordCount = words.size.coerceAtLeast(1)
    val charCount = line.text.trim().length
    // Promedio de duración natural de canto: ~480ms por palabra + 30ms por carácter
    val estimatedSingingDuration = (wordCount * 480L + charCount * 30L).coerceIn(2400L, 9500L)

    if (nextLineTimeMs != null && nextLineTimeMs > line.timeMs) {
        val totalGap = nextLineTimeMs - line.timeMs
        val silenceGap = totalGap - estimatedSingingDuration
        if (silenceGap >= 3200L) {
            return Pair(estimatedSingingDuration, true)
        } else {
            val activeDur = (totalGap * 0.94f).toLong().coerceAtLeast(1200L)
            return Pair(activeDur, false)
        }
    }

    return Pair(estimatedSingingDuration, false)
}

/**
 * RayMusic Flow Engine
 *
 * Motor de renderizado y animación fluida de letras para RayMusic.
 * Implementa la misma física, curvas elásticas de resorte y transiciones de GlassyFlow Turbo:
 * - Scroll con física elástica de resorte (Spring Physics) y anclaje dinámico a ~32% de altura.
 * - Enfoque y desenfoque progresivo por distancia (Distance Blur & Scale).
 * - Animación palabra por palabra (Karaoke sweep) con iluminación sutil.
 * - Indicador elástico de compases en silencios/instrumentales (Tacet).
 * - Tipografía y métricas modernas inspiradas en Satoshi Bold (-0.035em tracking, peso bold/black).
 * - Interacción táctil para saltar (seek) instantáneamente con resorte de recuperación.
 */
@Composable
fun RayMusicFlowLyrics(
    lyricsLines: List<ISyncedLine>?,
    currentPosition: Long,
    lyricsOffset: Long,
    isAutoScrollEnabled: Boolean,
    onAutoScrollChange: (Boolean) -> Unit,
    scrollToCurrentTrigger: Int,
    lyricsTextSize: Float = 28f,
    lyricsLineSpacing: Float = 1.35f,
    lyricsGlowEffect: Boolean = true,
    lyricsTextPosition: String = "left",
    lyricsClickChange: Boolean = true,
    lyricsAutoScroll: Boolean = true,
    contentColor: Color = Color.White,
    currentLyricsProviderName: String = "RayMusic",
    currentLyricsSyncType: String = "syllable",
    onSeek: (Long) -> Unit,
    onShowLyricsMenu: () -> Unit = {},
    onShowDistributorsMenu: () -> Unit = onShowLyricsMenu,
    lyricsListState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    if (lyricsLines.isNullOrEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cargando letra...",
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    val isSynced = remember(lyricsLines) {
        lyricsLines.any { it.timeMs > 0L }
    }

    // Detector de interacción del usuario para pausar el auto-scroll
    val lyricsScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    onAutoScrollChange(false)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                onAutoScrollChange(false)
                return Velocity.Zero
            }
        }
    }

    // Construcción de la lista de elementos (Líneas + Pausas instrumentales)
    val displayItems = remember(lyricsLines, isSynced) {
        val items = mutableListOf<RayMusicFlowItem>()
        if (!isSynced) {
            lyricsLines.forEachIndexed { idx, line ->
                items.add(RayMusicFlowItem.Line(idx, line))
            }
            return@remember items
        }

        val firstLine = lyricsLines.firstOrNull { it.timeMs > 0L }
        if (firstLine != null && firstLine.timeMs >= 2000L) {
            items.add(RayMusicFlowItem.InstrumentalBreak(0L, firstLine.timeMs, 0L))
        }

        for (i in lyricsLines.indices) {
            val line = lyricsLines[i]
            val nextLine = lyricsLines.getOrNull(i + 1)
            items.add(RayMusicFlowItem.Line(i, line))

            if (nextLine != null && line.timeMs > 0L && nextLine.timeMs > line.timeMs) {
                val (lineDuration, hasBreak) = calculateLineSingingDuration(line, nextLine.timeMs)
                if (hasBreak) {
                    val breakStart = line.timeMs + lineDuration
                    val breakEnd = nextLine.timeMs
                    items.add(RayMusicFlowItem.InstrumentalBreak(breakStart, breakEnd, breakStart))
                }
            }
        }
        items
    }

    // Índice del elemento activo según la posición de reproducción
    val activeItemIndex = remember(currentPosition, lyricsOffset, displayItems, isSynced) {
        if (!isSynced || displayItems.isEmpty()) return@remember 0
        val pos = currentPosition + lyricsOffset
        val index = displayItems.indexOfLast { item ->
            when (item) {
                is RayMusicFlowItem.Line -> {
                    item.syncedLine.timeMs != -1L && item.syncedLine.timeMs <= pos
                }
                is RayMusicFlowItem.InstrumentalBreak -> {
                    pos in item.startMs until item.endMs
                }
            }
        }
        if (index >= 0) index else 0
    }

    // RayMusic Flow Spring Scroll Engine: Desplazamiento elástico continuo
    LaunchedEffect(activeItemIndex, isAutoScrollEnabled, scrollToCurrentTrigger, lyricsAutoScroll) {
        if (!isSynced || !lyricsAutoScroll) return@LaunchedEffect
        if (!isAutoScrollEnabled && scrollToCurrentTrigger == 0) return@LaunchedEffect

        if (activeItemIndex >= 0 && !lyricsListState.isScrollInProgress) {
            // Anclaje óptimo a ~32% de altura de pantalla (scrollOffset = -190)
            lyricsListState.animateScrollToItem(
                index = activeItemIndex.coerceAtLeast(0),
                scrollOffset = -190
            )
        }
    }

    // Oscilador armónico para pausas instrumentales (Tacet)
    val infiniteTransition = rememberInfiniteTransition(label = "tacetWave")
    val tacetPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "tacetPhase"
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = lyricsListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .nestedScroll(lyricsScrollConnection),
            verticalArrangement = Arrangement.spacedBy((24 * lyricsLineSpacing / 1.35f).dp)
        ) {
            item { Spacer(modifier = Modifier.height(72.dp)) }

            items(
                count = displayItems.size,
                key = { itemIdx ->
                    when (val it = displayItems[itemIdx]) {
                        is RayMusicFlowItem.Line -> "line_${it.originalIndex}_${it.syncedLine.timeMs}"
                        is RayMusicFlowItem.InstrumentalBreak -> "break_${it.startMs}_${it.endMs}"
                    }
                }
            ) { itemIdx ->
                val item = displayItems[itemIdx]
                val isItemActive = isSynced && itemIdx == activeItemIndex
                val distance = if (isSynced && activeItemIndex >= 0) abs(itemIdx - activeItemIndex) else 0

                // RayMusic Flow Distance Physics: Curvas de opacidad, escala y desenfoque (idénticas a Glassy)
                val targetAlpha = when {
                    !isSynced || isItemActive -> 1.0f
                    distance == 1 -> 0.38f
                    distance == 2 -> 0.22f
                    else -> 0.12f
                }

                val targetScale = when {
                    !isSynced || isItemActive -> 1.03f
                    distance == 1 -> 0.95f
                    distance == 2 -> 0.89f
                    else -> 0.84f
                }

                val targetBlur = if (!isItemActive && isSynced) {
                    when (distance) {
                        1 -> 2.5.dp
                        2 -> 5.dp
                        else -> 9.dp
                    }
                } else 0.dp

                val animAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    label = "flowAlpha"
                )
                val animScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "flowScale"
                )
                val animBlur by animateDpAsState(
                    targetValue = targetBlur,
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    label = "flowBlur"
                )

                val transformOrigin = TransformOrigin(
                    when (lyricsTextPosition) {
                        "center" -> 0.5f
                        "right" -> 1f
                        else -> 0f
                    },
                    0.5f
                )

                when (item) {
                    is RayMusicFlowItem.InstrumentalBreak -> {
                        // Fila de Pausa Instrumental (Tacet - Cápsula de cristal líquido compacta)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = animScale
                                    scaleY = animScale
                                    alpha = animAlpha
                                    this.transformOrigin = transformOrigin
                                }
                                .then(if (animBlur > 0.dp) Modifier.blur(animBlur) else Modifier)
                                .clickable(
                                    enabled = lyricsClickChange,
                                    onClick = {
                                        onSeek((item.seekTargetMs - lyricsOffset).coerceAtLeast(0L))
                                    }
                                )
                                .padding(vertical = 12.dp),
                            horizontalArrangement = when (lyricsTextPosition) {
                                "center" -> Arrangement.Center
                                "right" -> Arrangement.End
                                else -> Arrangement.Start
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = if (isItemActive) 0.12f else 0.06f))
                                    .border(0.8.dp, Color.White.copy(alpha = if (isItemActive) 0.18f else 0.08f), RoundedCornerShape(24.dp))
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(0f, 0.33f, 0.66f).forEach { phaseOffset ->
                                    val pulse = if (isItemActive) {
                                        val shifted = (tacetPhase + phaseOffset) % 1f
                                        (sin(shifted * Math.PI * 2).toFloat() * 0.5f + 0.5f)
                                    } else 0f

                                    Box(
                                        modifier = Modifier
                                            .size((5.5f + pulse * 4f).dp)
                                            .background(
                                                color = if (isItemActive) Color.White else Color.White.copy(alpha = 0.40f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "♪",
                                    color = if (isItemActive) Color.White else Color.White.copy(alpha = 0.40f),
                                    fontSize = 14.sp,
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is RayMusicFlowItem.Line -> {
                        val line = item.syncedLine
                        val currentPosWithOffset = currentPosition + lyricsOffset
                        val isLineStarted = isSynced && line.timeMs != -1L && currentPosWithOffset >= line.timeMs

                        val nextLineTime = lyricsLines.getOrNull(item.originalIndex + 1)?.timeMs
                        val (lineDuration, _) = remember(line, nextLineTime) {
                            calculateLineSingingDuration(line, nextLineTime)
                        }
                        val activeDuration = remember(lineDuration) {
                            lineDuration.coerceAtLeast(300L)
                        }
                        val isPast = isSynced && line.timeMs != -1L && currentPosWithOffset > (line.timeMs + lineDuration)

                        val lineRelTime = if (isItemActive && isLineStarted && line.timeMs > 0) {
                            (currentPosWithOffset - line.timeMs).coerceAtLeast(0L)
                        } else if (isPast) activeDuration else -1L

                        // Desglose de palabras / sílabas para barrido karaoke continuo
                        val wordData = remember(line, activeDuration) {
                            if (line is KaraokeLine) {
                                val syllables = line.syllables
                                if (syllables.isNotEmpty()) {
                                    syllables.map { syl ->
                                        val sStart = (syl.start - line.timeMs).coerceAtLeast(0L)
                                        val sEnd = (syl.end - line.timeMs).coerceAtLeast(sStart + 50L)
                                        Triple(syl.content, sStart, sEnd)
                                    }
                                } else {
                                    listOf(Triple(line.text, 0L, activeDuration))
                                }
                            } else {
                                val words = line.text.split(" ").filter { it.isNotEmpty() }
                                if (words.isEmpty()) {
                                    listOf(Triple(line.text, 0L, activeDuration))
                                } else {
                                    val totalChars = line.text.length
                                    var accumulatedTime = 0L
                                    words.mapIndexed { wIdx, word ->
                                        val charCount = if (wIdx < words.lastIndex) word.length + 1 else word.length
                                        val wordStart = accumulatedTime
                                        val wordDur = if (totalChars > 0) {
                                            (activeDuration * charCount.toFloat() / totalChars).toLong()
                                        } else activeDuration
                                        accumulatedTime += wordDur
                                        Triple(if (wIdx < words.lastIndex) "$word " else word, wordStart, wordStart + wordDur)
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = animScale
                                    scaleY = animScale
                                    alpha = animAlpha
                                    this.transformOrigin = transformOrigin
                                }
                                .then(if (animBlur > 0.dp) Modifier.blur(animBlur) else Modifier)
                                .clickable(
                                    enabled = lyricsClickChange,
                                    onClick = {
                                        if (line.timeMs != -1L) {
                                            onSeek((line.timeMs - lyricsOffset).coerceAtLeast(0L))
                                        }
                                    }
                                ),
                            horizontalAlignment = when (lyricsTextPosition) {
                                "center" -> Alignment.CenterHorizontally
                                "right" -> Alignment.End
                                "left" -> Alignment.Start
                                else -> Alignment.Start
                            }
                        ) {
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = when (lyricsTextPosition) {
                                    "center" -> Arrangement.Center
                                    "right" -> Arrangement.End
                                    "left" -> Arrangement.Start
                                    else -> Arrangement.Start
                                },
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                wordData.forEach { (wordText, startRel, endRel) ->
                                    val wordDur = (endRel - startRel).coerceAtLeast(1L)
                                    val isCurrentWord = isItemActive && isLineStarted && lineRelTime >= 0L && lineRelTime in startRel..endRel
                                    val isPastWord = (isItemActive && isLineStarted && lineRelTime > endRel) || isPast
                                    val isFutureWord = !isPastWord && !isCurrentWord

                                    val wordProgress by animateFloatAsState(
                                        targetValue = when {
                                            !isLineStarted || lineRelTime < 0L -> 0f
                                            lineRelTime >= endRel -> 1f
                                            lineRelTime < startRel -> 0f
                                            else -> (lineRelTime - startRel).toFloat() / wordDur
                                        },
                                        animationSpec = tween(
                                            durationMillis = (wordDur * 0.40f).toInt().coerceIn(60, 160),
                                            easing = LinearEasing
                                        ),
                                        label = "flowWordProgress"
                                    )

                                    // blyrics-letter-wave + blyrics-wobble: Movimiento dinámico, orgánico y suave (Glassy Music)
                                    // Elevación vertical continua con física de resorte amortiguado de baja rigidez
                                    val animTransY by animateFloatAsState(
                                        targetValue = if (isCurrentWord) -3.2f else 0f,
                                        animationSpec = if (isCurrentWord) {
                                            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                                        } else {
                                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                        },
                                        label = "animTransY"
                                    )

                                    // Desplazamiento horizontal elástico y micro-escala suave al iniciar la palabra
                                    val animTransX by animateFloatAsState(
                                        targetValue = if (isCurrentWord && wordProgress < 0.65f) 1.5f else 0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                        label = "animTransX"
                                    )

                                    val animScale by animateFloatAsState(
                                        targetValue = if (isCurrentWord) 1.035f else 1.0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                                        label = "animScale"
                                    )

                                    // Tipografía Satoshi Black 900 para máxima contundencia en la línea activa, Bold 700 en secundarias
                                    val wordFont = if (isItemActive) SatoshiBlack else SatoshiBold

                                    Box(
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = animScale
                                            scaleY = animScale
                                            translationX = animTransX.dp.toPx()
                                            translationY = animTransY.dp.toPx()
                                        }
                                    ) {
                                        // Capa 1 (Base): Texto base atenuado con aura luminosa suave en la frase activa
                                        Text(
                                            text = wordText,
                                            fontFamily = wordFont,
                                            fontSize = lyricsTextSize.sp,
                                            letterSpacing = (-0.035).sp,
                                            lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                            color = if (isItemActive) Color.White.copy(alpha = 0.40f) else contentColor.copy(alpha = 0.85f),
                                            style = TextStyle(
                                                shadow = if (lyricsGlowEffect && isItemActive) {
                                                    Shadow(
                                                        color = Color.White.copy(alpha = 0.30f),
                                                        offset = Offset.Zero,
                                                        blurRadius = 10f
                                                    )
                                                } else null
                                            )
                                        )

                                        // Capa 2 (Marcado Activo): Marcado progresivo limpio en blanco puro con brillo luminoso (Glow)
                                        if (isItemActive && (isPastWord || isCurrentWord)) {
                                            Text(
                                                text = wordText,
                                                fontFamily = wordFont,
                                                fontSize = lyricsTextSize.sp,
                                                letterSpacing = (-0.035).sp,
                                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                                color = Color.White,
                                                style = TextStyle(
                                                    shadow = if (lyricsGlowEffect) {
                                                        Shadow(
                                                            color = Color.White.copy(alpha = if (isCurrentWord) 0.95f else 0.50f),
                                                            offset = Offset.Zero,
                                                            blurRadius = if (isCurrentWord) 22f else 12f
                                                        )
                                                    } else null
                                                ),
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .drawWithContent {
                                                        if (isPastWord || wordProgress >= 1f) {
                                                            drawContent()
                                                        } else if (wordProgress > 0f) {
                                                            clipRect(
                                                                left = 0f,
                                                                top = 0f,
                                                                right = size.width * wordProgress,
                                                                bottom = size.height
                                                            ) {
                                                                this@drawWithContent.drawContent()
                                                            }
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            // Letra traducida o romanizada si existe
                            val translation = line.translationText
                            if (!translation.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = translation,
                                    fontFamily = SatoshiMedium,
                                    fontSize = (lyricsTextSize * 0.62f).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = contentColor.copy(alpha = if (isItemActive) 0.68f else 0.22f),
                                    lineHeight = (lyricsTextSize * 0.78f).sp
                                )
                            }
                        }
                    }
                }
            }

            // Pie de Página con insignia del motor RayMusic
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 90.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val provSyncColor = when (currentLyricsSyncType.lowercase()) {
                        "syllable", "richsync" -> Color(0xFFFDE69B)
                        "word" -> Color(0xFFAAD1FF)
                        "line", "linesync" -> Color(0xFFC9F8DA)
                        else -> contentColor.copy(alpha = 0.55f)
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(contentColor.copy(alpha = 0.12f))
                            .clickable { onShowDistributorsMenu() }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ElectricBolt,
                            contentDescription = "RayMusic",
                            tint = contentColor.copy(alpha = 0.80f),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "RayMusic",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = contentColor.copy(alpha = 0.4f)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(provSyncColor)
                        )
                        Text(
                            text = currentLyricsProviderName.ifEmpty { "Sincronizado" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}
