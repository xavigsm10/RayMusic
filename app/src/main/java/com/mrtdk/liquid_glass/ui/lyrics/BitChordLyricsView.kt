package com.mrtdk.liquid_glass.ui.lyrics

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mrtdk.liquid_glass.data.lyrics.CharGrowth
import com.mrtdk.liquid_glass.data.lyrics.Genius
import com.mrtdk.liquid_glass.data.lyrics.GrowingWord
import com.mrtdk.liquid_glass.data.lyrics.LyricAlignment
import com.mrtdk.liquid_glass.data.lyrics.LyricLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs

// ── Visual Constants from BitChord ──────────────────────────────────────────
private val GLOW_RADIUS = 6.dp
private val GLOW_ROOM = 10.dp
private val BACKING_FONT_SIZE = 23.sp
private val BACKING_LINE_HEIGHT = 29.sp
private const val BACKING_ALPHA = 0.72f
private val WIPE_FEATHER = 30.dp
private val WORD_RISE = 2.dp
private const val GROW_HEADROOM = 3f
private val DUET_LANE = 44.dp
private val GAP_ROW_HEIGHT = 40.dp
private val GAP_ROW_SPACING = 16.dp
private val LINE_FALLOFF_ALPHA = floatArrayOf(1f, 0.8f, 0.7f, 0.58f, 0.46f)
private val LINE_FALLOFF_BLUR = arrayOf(0.dp, 1.dp, 1.dp, 1.7.dp, 2.4.dp)
private val SKELETON_BLOCKS = listOf(
    floatArrayOf(0.97f, 0.54f),
    floatArrayOf(0.92f, 0.99f, 0.41f),
    floatArrayOf(0.68f),
    floatArrayOf(0.95f, 0.73f),
    floatArrayOf(0.89f, 0.96f, 0.37f),
)
private val SKELETON_BAR = 26.dp
private val SKELETON_LEADING = 15.dp
private val SKELETON_BLOCK_GAP = 35.dp
private const val SKELETON_PERIOD_MS = 1_400
private const val BROWSING_ALPHA = 0.8f
private const val INACTIVE_SCALE = 0.98f
private const val PRESSED_SCALE = 0.96f
private const val GAP_DOTS = 3
private val GAP_DOT_SIZE = 13.dp
private val GAP_DOT_GAP = 5.dp
private const val GAP_DOT_REST = 0.25f
private const val GAP_REST_SCALE = 0.76f
private const val SCROLL_LEAD_MIN_MS = 350L
private const val SCROLL_LEAD_MAX_MS = 500L
private val LYRIC_EASING = CubicBezierEasing(0.41f, 0f, 0.12f, 0.99f)
private const val LYRIC_SETTLE_MS = 400
private const val STAGGER_STEPS = 3
private const val STAGGER_FRACTION = 0.06f
private const val UNSUNG_ALPHA = 0.45f
private const val GLOW_ALPHA = 0.62f
private val CONTROLS_SCROLL_SLOP = 20.dp
private val LYRICS_GUTTER = 24.dp

private class ScrollRun(val id: Int, val delta: Float, val durationMs: Int) {
    val spanMs: Float get() = durationMs * (1f + STAGGER_FRACTION * STAGGER_STEPS)
}

/** Keep every unfinished vocal visible, including overlaps spanning more than two rows. */
internal fun activeLyricRows(lines: List<LyricLine>, positionMs: Long): List<Int> {
    val latest = lines.indexOfLast { it.timeMs <= positionMs }
    if (latest < 0) return emptyList()
    return (0..latest).filter { index ->
        val line = lines[index]
        index == latest || (!line.isGap &&
            (line.hasKnownEnd || line.background?.hasKnownEnd == true) &&
            line.timeMs <= positionMs && positionMs < line.endMs)
    }
}

/** Polling jitter must not rewind a word highlight or briefly reactivate the previous line. */
internal fun reconcileLyricPosition(displayedMs: Long, reportedMs: Long): Long =
    if (abs(displayedMs - reportedMs) <= 250L) maxOf(displayedMs, reportedMs)
    else reportedMs

private fun scrollLead(lines: List<LyricLine>, positionMs: Long): Long {
    val current = lines.indexOfLast { it.timeMs <= positionMs }
    if (current < 0) return SCROLL_LEAD_MIN_MS
    val next = lines.getOrNull(current + 1) ?: return SCROLL_LEAD_MIN_MS
    val gap = next.timeMs - lines[current].endMs
    return gap.coerceIn(SCROLL_LEAD_MIN_MS, SCROLL_LEAD_MAX_MS)
}

@Composable
fun rememberIsForeground(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return foreground
}

@Composable
fun rememberLyricClock(positionMs: Long, isPlaying: Boolean): MutableLongState {
    val clock = remember { mutableLongStateOf(positionMs) }
    val foreground = rememberIsForeground()
    LaunchedEffect(positionMs, isPlaying, foreground) {
        clock.longValue = reconcileLyricPosition(clock.longValue, positionMs)
        if (!isPlaying || !foreground) return@LaunchedEffect
        val firstFrame = withFrameMillis { it }
        while (true) {
            withFrameMillis { frame ->
                clock.longValue = maxOf(clock.longValue, positionMs + frame - firstFrame)
            }
        }
    }
    return clock
}

/**
 * Main BitChord Lyrics Panel Composable.
 */
@Composable
fun BitChordLyricsView(
    lines: List<LyricLine>,
    positionMs: Long,
    isPlaying: Boolean,
    looking: Boolean = false,
    onSeekToLine: (Long) -> Unit = {},
    controlsOpen: Boolean = true,
    onRevealControls: () -> Unit = {},
    onHideControls: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val clock = rememberLyricClock(positionMs, isPlaying)
    val isSynced = remember(lines) { lines.any { it.timeMs > 0L } }
    val duet = remember(lines) { lines.any { it.alignment == LyricAlignment.End } }

    val activeRows by remember(lines, isSynced) {
        derivedStateOf {
            if (!isSynced) emptyList() else activeLyricRows(lines, clock.longValue)
        }
    }
    val scrollLine = activeRows.firstOrNull() ?: -1
    val leadLine by remember(lines, isSynced) {
        derivedStateOf {
            if (!isSynced) {
                -1
            } else {
                val now = clock.longValue
                activeLyricRows(lines, now + scrollLead(lines, now)).firstOrNull() ?: -1
            }
        }
    }
    val focusLine = if (leadLine >= 0) leadLine else scrollLine
    val listState = rememberLazyListState()

    val viewportHeight by remember(listState) {
        derivedStateOf { listState.layoutInfo.viewportSize.height }
    }
    val keepScroll = remember(listState) { keepScrollInList(listState) }
    var browsing by remember { mutableStateOf(false) }

    val glowing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val hideControls by rememberUpdatedState(onHideControls)
    val revealControls by rememberUpdatedState(onRevealControls)

    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                browsing = true
            }
        }
    }

    val controlsSlopPx = with(LocalDensity.current) { CONTROLS_SCROLL_SLOP.toPx() }
    val controlsOnScroll = remember(listState, controlsSlopPx) {
        object : NestedScrollConnection {
            private var travel = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    if (travel != 0f && (travel > 0f) != (available.y > 0f)) travel = 0f
                    travel += available.y
                    if (travel <= -controlsSlopPx) {
                        travel = 0f
                        hideControls()
                    } else if (travel >= controlsSlopPx) {
                        travel = 0f
                        revealControls()
                    }
                }
                return Offset.Zero
            }
        }
    }

    val currentLine by rememberUpdatedState(focusLine)
    val activeOnScreen by remember(listState) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.any { it.index == currentLine }
        }
    }
    LaunchedEffect(browsing, activeOnScreen, listState.isScrollInProgress) {
        if (browsing && activeOnScreen && !listState.isScrollInProgress) {
            delay(600)
            browsing = false
        }
    }

    LaunchedEffect(browsing, listState.isScrollInProgress) {
        if (browsing && !listState.isScrollInProgress) {
            delay(5_000)
            browsing = false
        }
    }

    var run by remember(lines) { mutableStateOf(ScrollRun(0, 0f, LYRIC_SETTLE_MS)) }
    val since = remember(lines) { mutableFloatStateOf(0f) }
    LaunchedEffect(run.id) {
        if (run.id == 0) return@LaunchedEffect
        val spanMs = maxOf(1, run.spanMs.toInt())
        animate(
            initialValue = 0f,
            targetValue = run.spanMs,
            animationSpec = tween(spanMs, easing = LinearEasing),
        ) { value, _ -> since.floatValue = value }
    }

    var placed by remember(lines) { mutableStateOf(false) }
    LaunchedEffect(focusLine, browsing, controlsOpen) {
        if (isSynced && !browsing && focusLine >= 0 && focusLine in lines.indices) {
            snapshotFlow { listState.layoutInfo.viewportSize.height }.first { it > 0 }
            val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == focusLine }
            when {
                !placed -> {
                    listState.scrollToItem(focusLine, scrollOffset = 0)
                    placed = true
                }
                visible != null -> {
                    val span = maxOf(50, scrollLead(lines, clock.longValue).toInt())
                    run = ScrollRun(run.id + 1, visible.offset.toFloat(), span)
                    listState.animateScrollBy(
                        value = visible.offset.toFloat(),
                        animationSpec = tween(durationMillis = span, easing = LYRIC_EASING),
                    )
                }
                else -> listState.animateScrollToItem(focusLine, scrollOffset = 0)
            }
        }
    }

    if (lines.isEmpty()) {
        if (looking) {
            LyricsSkeleton(modifier)
        } else {
            Box(modifier, contentAlignment = Alignment.Center) {
                Text(
                    text = "No se encontraron letras para esta pista",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .nestedScroll(controlsOnScroll)
            .nestedScroll(keepScroll)
            .revealLyricsControlsOnTap(!controlsOpen) { onRevealControls() }
            .fadingEdges(),
        contentPadding = PaddingValues(
            top = 40.dp - GLOW_ROOM,
            bottom = with(LocalDensity.current) { viewportHeight.toDp() } * 0.8f,
            start = LYRICS_GUTTER - GLOW_ROOM,
            end = LYRICS_GUTTER - GLOW_ROOM,
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        itemsIndexed(
            items = lines,
            key = { index, line -> "${line.timeMs}_${index}" }
        ) { index, line ->
            if (!isSynced && Genius.isSectionHeader(line.text)) {
                val sectionTitle = line.text.removePrefix("[").removeSuffix("]").trim()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (index == 0) 6.dp else 24.dp, bottom = 8.dp)
                        .padding(horizontal = GLOW_ROOM),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 11.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = sectionTitle.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.3.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }
                return@itemsIndexed
            }

            if (!isSynced && line.isGap) {
                Spacer(Modifier.height(14.dp))
                return@itemsIndexed
            }

            val offset = if (scrollLine < 0) 0 else index - scrollLine
            val distance = abs(offset)
            val isActive = isSynced && index in activeRows
            val step = distance.coerceAtMost(LINE_FALLOFF_ALPHA.lastIndex)
            val blur by animateDpAsState(
                targetValue = when {
                    !isSynced || browsing || isActive -> 0.dp
                    else -> LINE_FALLOFF_BLUR[step]
                },
                animationSpec = tween(LYRIC_SETTLE_MS, easing = LYRIC_EASING),
                label = "lyricBlur",
            )
            val lineAlpha by animateFloatAsState(
                targetValue = when {
                    !isSynced -> 0.95f
                    isActive -> 1f
                    browsing -> BROWSING_ALPHA
                    else -> LINE_FALLOFF_ALPHA[step]
                },
                animationSpec = tween(LYRIC_SETTLE_MS, easing = LYRIC_EASING),
                label = "lyricAlpha",
            )

            if (line.isGap) {
                val until = lines.getOrNull(index + 1)?.timeMs ?: line.endMs
                val swell by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = if (isActive) 400 else 350,
                        easing = LYRIC_EASING,
                    ),
                    label = "gapSwell",
                )
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .height((GAP_ROW_HEIGHT + GAP_ROW_SPACING) * swell)
                        .clipToBounds(),
                ) {
                    Box(
                        modifier = Modifier
                            .lyricBlur(blur)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = isSynced) { onSeekToLine(line.timeMs) }
                            .padding(GLOW_ROOM)
                            .size(
                                width = GAP_DOT_SIZE * 3 + GAP_DOT_GAP * 2,
                                height = GAP_DOT_SIZE,
                            )
                            .graphicsLayer {
                                val grow = GAP_REST_SCALE + (1f - GAP_REST_SCALE) * swell
                                scaleX = grow
                                scaleY = grow
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                alpha = lineAlpha * swell
                            }
                            .drawBehind {
                                val span = (until - line.timeMs).coerceAtLeast(1L)
                                val through = ((clock.longValue - line.timeMs).toFloat() / span).coerceIn(0f, 1f)
                                val radius = GAP_DOT_SIZE.toPx() / 2f
                                val stride = (GAP_DOT_SIZE + GAP_DOT_GAP).toPx()
                                repeat(GAP_DOTS) { dot ->
                                    val lit = (through * GAP_DOTS - dot).coerceIn(0f, 1f)
                                    drawCircle(
                                        color = Color.White.copy(
                                            alpha = GAP_DOT_REST + (1f - GAP_DOT_REST) * lit,
                                        ),
                                        radius = radius,
                                        center = Offset(radius + dot * stride, size.height / 2f),
                                    )
                                }
                            }
                            .semantics { contentDescription = "Instrumental" },
                    )
                }
            } else {
                val alignEnd = duet && line.alignment == LyricAlignment.End
                val style = if (isSynced) {
                    MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        lineHeight = 41.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
                    )
                } else {
                    MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 30.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
                    )
                }

                val sung = offset < 0
                val behind = if (run.delta >= 0f) index - focusLine else focusLine - index
                val staggerDelay = behind.coerceIn(0, STAGGER_STEPS) * STAGGER_FRACTION * run.durationMs
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = when {
                        pressed -> PRESSED_SCALE
                        isActive -> 1f
                        else -> INACTIVE_SCALE
                    },
                    animationSpec = tween(
                        durationMillis = if (pressed) 120 else LYRIC_SETTLE_MS,
                        easing = LYRIC_EASING,
                    ),
                    label = "lyricScale",
                )
                val glow by animateFloatAsState(
                    targetValue = if (isActive && glowing) GLOW_ALPHA else 0f,
                    animationSpec = tween(durationMillis = 420),
                    label = "lyricGlow",
                )

                val shape = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (duet && alignEnd) DUET_LANE else 0.dp,
                        end = if (duet && !alignEnd) DUET_LANE else 0.dp,
                    )
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(if (alignEnd) 1f else 0f, 0.5f)
                        alpha = lineAlpha
                        translationY = if (staggerDelay <= 0f) {
                            0f
                        } else {
                            val duration = maxOf(1, run.durationMs).toFloat()
                            val elapsed = since.floatValue
                            run.delta * (
                                LYRIC_EASING.transform((elapsed / duration).coerceIn(0f, 1f)) -
                                LYRIC_EASING.transform(((elapsed - staggerDelay) / duration).coerceIn(0f, 1f))
                            )
                        }
                    }
                    .lyricBlur(blur)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        enabled = isSynced,
                        interactionSource = interaction,
                        indication = null,
                    ) { onSeekToLine(line.timeMs) }

                AnimatedContent(
                    targetState = line,
                    transitionSpec = {
                        val duration = 380
                        val fadeSpec = tween<Float>(duration, easing = FastOutSlowInEasing)
                        (fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)).using(
                            SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> tween(duration, easing = FastOutSlowInEasing) })
                        )
                    },
                    label = "lyricsLineTransition",
                    modifier = shape,
                ) { renderedLine ->
                    Column {
                        PanelVoice(
                            line = renderedLine,
                            clock = clock,
                            style = style,
                            isActive = isActive,
                            sung = sung,
                            synced = isSynced,
                            browsing = browsing,
                            glowAlpha = glow,
                            room = GLOW_ROOM,
                            alignEnd = alignEnd,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        renderedLine.background?.let { backing ->
                            PanelVoice(
                                line = backing.withoutBracketPunctuation(),
                                clock = clock,
                                style = style.copy(
                                    fontSize = BACKING_FONT_SIZE,
                                    lineHeight = BACKING_LINE_HEIGHT,
                                ),
                                isActive = isActive,
                                sung = sung,
                                synced = isSynced,
                                browsing = browsing,
                                glowAlpha = 0f,
                                room = 0.dp,
                                alignEnd = alignEnd,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = GLOW_ROOM, end = GLOW_ROOM, bottom = GLOW_ROOM)
                                    .graphicsLayer { alpha = BACKING_ALPHA },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelVoice(
    line: LyricLine,
    clock: MutableLongState,
    style: TextStyle,
    isActive: Boolean,
    sung: Boolean,
    synced: Boolean,
    browsing: Boolean,
    glowAlpha: Float,
    room: Dp,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    if (line.isWordSynced && !browsing) {
        val tail by animateFloatAsState(
            targetValue = if (sung) 1f else UNSUNG_ALPHA,
            label = "lyricTail",
        )
        SweptLyricLine(
            line = line,
            clock = clock,
            style = style,
            dimAlpha = tail,
            modifier = modifier,
            glowAlpha = glowAlpha,
            glowRoom = room,
            feather = isActive,
            alignEnd = alignEnd,
        )
    } else if (line.isWordSynced) {
        val tail by animateFloatAsState(
            targetValue = if (sung) 1f else UNSUNG_ALPHA,
            label = "lyricTail",
        )
        SweptLyricLine(
            line = line,
            clock = clock,
            style = style,
            dimAlpha = tail,
            modifier = modifier,
            glowAlpha = 0f,
            glowRoom = room,
            alignEnd = alignEnd,
        )
    } else {
        val lit by animateFloatAsState(
            targetValue = if (!synced || sung || isActive) 1f else UNSUNG_ALPHA,
            label = "lyricLit",
        )
        Text(
            text = line.text,
            style = style,
            color = Color.White.copy(alpha = lit),
            modifier = modifier.padding(room),
        )
    }
}

@Composable
private fun SweptLyricLine(
    line: LyricLine,
    clock: MutableLongState,
    style: TextStyle,
    dimAlpha: Float,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    glowAlpha: Float = 0f,
    glowRadius: Dp = GLOW_RADIUS,
    glowRoom: Dp = 0.dp,
    feather: Boolean = false,
    rise: Boolean = true,
    alignEnd: Boolean = false,
) {
    var layout by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    val growth = remember { CharGrowth() }
    val room = if (glowRoom > 0.dp) Modifier.padding(glowRoom) else Modifier

    val riseAgainst: (Modifier) -> Modifier = { inner ->
        if (!rise) {
            inner
        } else {
            Modifier
                .drawWithContent {
                    val measured = layout
                    if (measured == null || line.words.isEmpty()) {
                        drawContent()
                    } else {
                        riseWith(
                            layout = measured,
                            line = line,
                            positionMs = clock.longValue,
                            inset = glowRoom.toPx(),
                            peak = WORD_RISE.toPx(),
                            growth = growth,
                        )
                    }
                }
                .then(inner)
        }
    }

    val sweep = Modifier.drawWithContent {
        val position = clock.longValue
        when {
            position >= line.endMs -> drawContent()
            position <= line.timeMs -> Unit
            else -> layout?.let { sweepTo(it, line.revealedChars(position), feather) }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = if (alignEnd) Alignment.TopEnd else Alignment.TopStart,
    ) {
        Text(
            text = line.text,
            style = style,
            color = Color.White.copy(alpha = dimAlpha),
            maxLines = maxLines,
            overflow = overflow,
            onTextLayout = { layout = it },
            modifier = riseAgainst(room),
        )
        if (glowAlpha > 0.01f) {
            Text(
                text = line.text,
                style = style,
                color = Color.White,
                maxLines = maxLines,
                overflow = overflow,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = glowAlpha
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .blur(glowRadius, BlurredEdgeTreatment.Unbounded)
                    .then(room)
                    .drawWithContent {
                        val measured = layout ?: return@drawWithContent
                        glowGrown(
                            layout = measured,
                            line = line,
                            positionMs = clock.longValue,
                            inset = glowRoom.toPx(),
                            peak = WORD_RISE.toPx(),
                            growth = growth,
                        )
                    },
            )
        }
        Text(
            text = line.text,
            style = style,
            color = Color.White,
            maxLines = maxLines,
            overflow = overflow,
            modifier = riseAgainst(
                Modifier
                    .graphicsLayer {
                        compositingStrategy = if (feather) {
                            CompositingStrategy.Offscreen
                        } else {
                            CompositingStrategy.Auto
                        }
                    }
                    .then(room)
                    .then(sweep),
            ),
        )
    }
}

private fun ContentDrawScope.glowGrown(
    layout: TextLayoutResult,
    line: LyricLine,
    positionMs: Long,
    inset: Float,
    peak: Float,
    growth: CharGrowth,
) {
    if (!line.isGrowing(positionMs)) return
    val em = layout.layoutInput.style.fontSize.toPx()
    val length = layout.layoutInput.text.length
    for (word in line.growingWords) {
        if (positionMs < word.startMs || positionMs > word.restsAtMs) continue
        val span = line.wordSpans[word.index]
        val fall = line.wordFall(word.index, positionMs)
        val charStart = maxOf(0, span.first)
        val charEnd = minOf(span.last, length - 1)
        for (char in charStart..charEnd) {
            word.sampleInto(char - span.first, positionMs, growth)
            if (growth.bloom <= 0.01f) continue
            val visualLine = layout.getLineForOffset(char)
            val from = layout.xOn(char, visualLine, inset)
            val to = layout.xOn(char + 1, visualLine, inset)
            if (to <= from) continue
            val dx = growth.shift * em
            val dy = -growth.rise * peak * fall
            val rowTop = layout.getLineTop(visualLine) + inset
            val bottom = layout.getLineBottom(visualLine) + inset
            val overhang = (to - from) * (growth.scale - 1f) / 2f
            clipRect(
                left = from - overhang + dx,
                top = rowTop - peak * GROW_HEADROOM,
                right = to + overhang + dx,
                bottom = bottom,
            ) {
                translate(left = dx, top = dy) {
                    scale(
                        growth.scale,
                        growth.scale,
                        Offset((from + to) / 2f, (rowTop + bottom) / 2f),
                    ) {
                        this@glowGrown.drawContent()
                    }
                }
                drawRect(
                    color = Color.White.copy(alpha = growth.bloom),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
    }
}

private fun ContentDrawScope.riseWith(
    layout: TextLayoutResult,
    line: LyricLine,
    positionMs: Long,
    inset: Float,
    peak: Float,
    growth: CharGrowth,
) {
    if (!line.isLifted(positionMs)) {
        drawContent()
        return
    }
    val em = layout.layoutInput.style.fontSize.toPx()
    for (visualLine in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(visualLine)
        val lineEnd = layout.getLineEnd(visualLine, visibleEnd = true)
        val top = layout.getLineTop(visualLine) + inset
        val bottom = layout.getLineBottom(visualLine) + inset
        var at = lineStart
        var edge = layout.getLineLeft(visualLine) + inset
        for (index in line.words.indices) {
            val span = line.wordSpans[index]
            val start = maxOf(span.first, lineStart)
            val end = minOf(span.last + 1, lineEnd)
            if (start >= end) continue
            val held = line.growingAt(index)?.takeIf { positionMs in it.startMs..it.restsAtMs }
            val lift = line.wordLift(index, positionMs)
            if (held == null && lift <= 0.01f) continue
            val from = layout.xOn(start, visualLine, inset)
            val to = layout.xOn(end, visualLine, inset)
            if (to <= from) continue
            if (start > at) sliceRisen(edge, top, from, bottom, 0f)
            if (held != null) {
                growEach(
                    layout, held, line, positionMs, visualLine,
                    start, end, top, bottom, inset, peak, em, growth,
                )
            } else {
                sliceRisen(from, top - peak, to, bottom, -lift * peak)
            }
            at = end
            edge = to
        }
        if (at < lineEnd) {
            sliceRisen(edge, top, layout.getLineRight(visualLine) + inset, bottom, 0f)
        }
    }
}

private fun ContentDrawScope.growEach(
    layout: TextLayoutResult,
    word: GrowingWord,
    line: LyricLine,
    positionMs: Long,
    visualLine: Int,
    start: Int,
    end: Int,
    top: Float,
    bottom: Float,
    inset: Float,
    peak: Float,
    em: Float,
    growth: CharGrowth,
) {
    val fall = line.wordFall(word.index, positionMs)
    val first = line.wordSpans[word.index].first
    val ceiling = top - peak * GROW_HEADROOM
    val middle = (top + bottom) / 2f
    val length = layout.layoutInput.text.length
    val charStart = maxOf(0, start)
    val charEnd = minOf(end, length)
    for (char in charStart until charEnd) {
        word.sampleInto(char - first, positionMs, growth)
        val from = layout.xOn(char, visualLine, inset)
        val to = layout.xOn(char + 1, visualLine, inset)
        if (to <= from) continue
        val dx = growth.shift * em
        val dy = -growth.rise * peak * fall
        val overhang = (to - from) * (growth.scale - 1f) / 2f
        clipRect(
            left = from - overhang + dx,
            top = ceiling,
            right = to + overhang + dx,
            bottom = bottom,
        ) {
            translate(left = dx, top = dy) {
                scale(growth.scale, growth.scale, Offset((from + to) / 2f, middle)) {
                    this@growEach.drawContent()
                }
            }
        }
    }
}

private fun TextLayoutResult.xOn(offset: Int, visualLine: Int, inset: Float): Float {
    val left = getLineLeft(visualLine) + inset
    val right = getLineRight(visualLine) + inset
    return when {
        offset <= getLineStart(visualLine) -> left
        offset >= getLineEnd(visualLine, visibleEnd = true) -> right
        else -> (getHorizontalPosition(offset, usePrimaryDirection = true) + inset)
            .coerceIn(left, right)
    }
}

private fun ContentDrawScope.sliceRisen(
    from: Float,
    top: Float,
    to: Float,
    bottom: Float,
    dy: Float,
) {
    if (to <= from) return
    clipRect(left = from, top = top, right = to, bottom = bottom) {
        translate(top = dy) { this@sliceRisen.drawContent() }
    }
}

private fun horizontalAt(
    layout: TextLayoutResult,
    chars: Float,
    visualLine: Int,
): Float {
    val lineStart = layout.getLineStart(visualLine)
    val lineEnd = layout.getLineEnd(visualLine, visibleEnd = true)
    val index = chars.toInt().coerceIn(lineStart, lineEnd)
    val here = layout.xOn(index, visualLine, 0f)
    val next = layout.xOn((index + 1).coerceAtMost(lineEnd), visualLine, 0f)
    return here + (next - here) * (chars - index)
}

private fun ContentDrawScope.sweepTo(
    layout: TextLayoutResult,
    revealedChars: Float,
    feather: Boolean,
) {
    if (revealedChars <= 0f) return
    if (revealedChars >= layout.layoutInput.text.length) {
        drawContent()
        return
    }
    for (visualLine in 0 until layout.lineCount) {
        val start = layout.getLineStart(visualLine)
        if (revealedChars <= start) return
        val end = layout.getLineEnd(visualLine, visibleEnd = true)
        val cut = revealedChars < end
        val right = if (cut) {
            horizontalAt(layout, revealedChars, visualLine)
        } else {
            layout.getLineRight(visualLine)
        }
        val top = layout.getLineTop(visualLine)
        val bottom = layout.getLineBottom(visualLine)
        clipRect(
            left = layout.getLineLeft(visualLine),
            top = top,
            right = right,
            bottom = bottom,
        ) {
            this@sweepTo.drawContent()
        }
        if (!feather || !cut) continue
        clipRect(top = top, bottom = bottom) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.White,
                    1f to Color.Transparent,
                    startX = (right - WIPE_FEATHER.toPx()).coerceAtLeast(layout.getLineLeft(visualLine)),
                    endX = right,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
}

private fun LyricLine.withoutBracketPunctuation(): LyricLine = copy(
    text = text.stripParens(),
    words = words.mapNotNull { word ->
        word.text.stripParens().takeIf { it.isNotEmpty() }?.let { word.copy(text = it) }
    },
)

private fun String.stripParens(): String = replace("(", "").replace(")", "").trim()

private fun keepScrollInList(listState: LazyListState) = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = available

    override suspend fun onPreFling(available: Velocity): Velocity =
        if (available.y > 0f && !listState.canScrollBackward) available else Velocity.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}

private fun Modifier.fadingEdges(): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fade = 28.dp.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = fade,
            ),
            blendMode = BlendMode.DstIn,
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = size.height - fade,
                endY = size.height,
            ),
            blendMode = BlendMode.DstIn,
        )
    }


private const val TAP_SLOP_FACTOR = 2.5f

@Composable
internal fun Modifier.revealLyricsControlsOnTap(
    enabled: Boolean,
    onReveal: () -> Unit,
): Modifier {
    val currentOnReveal = rememberUpdatedState(onReveal)
    return pointerInput(enabled) {
        if (!enabled) return@pointerInput
        val tapSlop = viewConfiguration.touchSlop * TAP_SLOP_FACTOR
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.y < size.height / 2f) return@awaitEachGesture
            var dragged = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if ((change.position - down.position).getDistance() > tapSlop ||
                    event.changes.size > 1
                ) {
                    dragged = true
                } else if (change.positionChange() != Offset.Zero) {
                    change.consume()
                }
                if (!change.pressed) {
                    if (!dragged) {
                        change.consume()
                        currentOnReveal.value()
                    }
                    break
                }
            } while (true)
        }
    }
}

@Composable
private fun LyricsSkeleton(modifier: Modifier = Modifier) {
    val sweep = rememberInfiniteTransition(label = "lyricsSkeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(SKELETON_PERIOD_MS, easing = LinearEasing),
        ),
        label = "sweep",
    )
    BoxWithConstraints(modifier.padding(top = 40.dp).padding(horizontal = LYRICS_GUTTER)) {
        val column = maxWidth
        Column(verticalArrangement = Arrangement.spacedBy(SKELETON_BLOCK_GAP)) {
            SKELETON_BLOCKS.forEach { rows ->
                Column(verticalArrangement = Arrangement.spacedBy(SKELETON_LEADING)) {
                    rows.forEach { fraction ->
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(SKELETON_BAR)
                                .clip(RoundedCornerShape(4.dp))
                                .drawWithCache {
                                    val full = column.toPx()
                                    val band = full * 0.45f
                                    val startX = -band + sweep.value * (full + band * 2)
                                    val brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.10f),
                                            Color.White.copy(alpha = 0.26f),
                                            Color.White.copy(alpha = 0.10f),
                                        ),
                                        startX = startX,
                                        endX = startX + band,
                                    )
                                    onDrawWithContent { drawRect(brush) }
                                },
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.lyricBlur(radius: Dp): Modifier =
    if (radius > 0.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius, BlurredEdgeTreatment.Unbounded)
    } else {
        this
    }
