/*
 * Copyright 2025 Kyant (https://github.com/Kyant0/AndroidLiquidGlass)
 * Licensed under the Apache License, Version 2.0. See THIRD_PARTY_NOTICES.md.
 *
 * From the Backdrop Catalog sample app. Changes from upstream: package relocated, made internal,
 * configurable colours, an onValueChangeFinished callback, and the whole slider takes drags
 * (pressing anywhere moves the thumb under the finger) instead of only the thumb, with taps
 * on the track handled by the same gesture.
 */
package expo.modules.androidglassview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import expo.modules.androidglassview.backdrop.Backdrop
import expo.modules.androidglassview.backdrop.backdrops.layerBackdrop
import expo.modules.androidglassview.backdrop.backdrops.rememberBackdrop
import expo.modules.androidglassview.backdrop.backdrops.rememberCombinedBackdrop
import expo.modules.androidglassview.backdrop.backdrops.rememberLayerBackdrop
import expo.modules.androidglassview.backdrop.drawBackdrop
import expo.modules.androidglassview.backdrop.effects.blur
import expo.modules.androidglassview.backdrop.effects.lens
import expo.modules.androidglassview.backdrop.highlight.Highlight
import expo.modules.androidglassview.backdrop.shadow.InnerShadow
import expo.modules.androidglassview.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onDragStarted: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    trackColor: Color? = null
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accent = accentColor
        ?: if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val track = trackColor
        ?: if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {},
                onDrag = { _, _ -> }
            )
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }
                .collectLatest { value ->
                    if (dampedDragAnimation.targetValue != value) {
                        dampedDragAnimation.updateValue(value)
                    }
                }
        }

        val valueAt: (Float) -> Float = { x ->
            val fraction = (x / trackWidth).fastCoerceIn(0f, 1f)
            val position = if (isLtr) fraction else 1f - fraction
            valueRange.start + (valueRange.endInclusive - valueRange.start) * position
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(dampedDragAnimation, trackWidth, isLtr) {
                    inspectDragGestures(
                        onDragStart = { down ->
                            onDragStarted()
                            dampedDragAnimation.press()
                            onValueChange(valueAt(down.position.x))
                        },
                        onDragEnd = { up ->
                            // On a busy frame the last move can lag behind; the lift point wins.
                            onValueChange(valueAt(up.position.x))
                            dampedDragAnimation.release()
                            onValueChangeFinished()
                        },
                        onDragCancel = {
                            dampedDragAnimation.release()
                            onValueChangeFinished()
                        }
                    ) { change, _ ->
                        onValueChange(valueAt(change.position.x))
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(Modifier.layerBackdrop(trackBackdrop)) {
                Box(
                    Modifier
                        .clip(CapsuleShape)
                        .background(track)
                        .height(6f.dp)
                        .fillMaxWidth()
                )

                Box(
                    Modifier
                        .clip(CapsuleShape)
                        .background(accent)
                        .height(6f.dp)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                            layout(width, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
                )
            }

            Box(
                Modifier
                    .graphicsLayer {
                        translationX =
                            (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                                .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                    }
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(
                            backdrop,
                            rememberBackdrop(trackBackdrop) { drawBackdrop ->
                                val progress = dampedDragAnimation.pressProgress
                                val scaleX = lerp(2f / 3f, 1f, progress)
                                val scaleY = lerp(0f, 1f, progress)
                                scale(scaleX, scaleY) {
                                    drawBackdrop()
                                }
                            }
                        ),
                        shape = { CapsuleShape },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            blur(8f.dp.toPx() * (1f - progress))
                            lens(
                                10f.dp.toPx() * progress,
                                14f.dp.toPx() * progress,
                                chromaticAberration = true
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Ambient.copy(
                                width = Highlight.Ambient.width / 1.5f,
                                blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                alpha = progress
                            )
                        },
                        shadow = {
                            Shadow(
                                radius = 4f.dp,
                                color = Color.Black.copy(alpha = 0.05f)
                            )
                        },
                        innerShadow = {
                            val progress = dampedDragAnimation.pressProgress
                            InnerShadow(
                                radius = 4f.dp * progress,
                                alpha = progress
                            )
                        },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            val velocity = dampedDragAnimation.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            drawRect(Color.White.copy(alpha = 1f - progress))
                        }
                    )
                    .size(40f.dp, 24f.dp)
            )
        }
    }
}
