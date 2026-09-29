package com.mrtdk.liquid_glass.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrtdk.liquid_glass.playback.sing.AppleMusicSingManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Apple Music Sing (Karaoke) Microphone Bar & Slider Capsule.
 *
 * Implements the Apple Music Sing experience directly on the lyrics screen:
 * - Standby (collapsed): Sleek frosted glass microphone capsule bar (46.dp x 52.dp) docked on the right edge.
 * - Active (expanded): High-precision vertical volume slider bar (46.dp x 205.dp).
 * - Real-time Center Channel Vocal Suppression level control (0% to 100%).
 * - Dynamic fluid fill with Apple Music silver/white gradient.
 * - Live floating tooltip badge on the side with current percentage and status.
 * - Top collapse button to minimize.
 * - Bottom mic button to instantly toggle between full vocal suppression (0% karaoke) and previous volume.
 */
@Composable
fun AppleMusicSingSlider(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {}
) {
    val isSingEnabled by AppleMusicSingManager.isSingEnabled.collectAsState()
    val vocalVolume by AppleMusicSingManager.vocalVolume.collectAsState()

    var isDragging by remember { mutableStateOf(false) }
    var sliderHeightPx by remember { mutableFloatStateOf(1f) }

    // Animated smooth fill level
    val animatedFill by animateFloatAsState(
        targetValue = if (isSingEnabled) vocalVolume else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "singFill"
    )

    // Animated capsule height: collapses to 52.dp when Sing is off, expands to 205.dp when active
    val animatedCapsuleHeight by animateDpAsState(
        targetValue = if (isSingEnabled) 205.dp else 52.dp,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "capsuleHeight"
    )

    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isSingEnabled) 26.dp else 26.dp,
        label = "capsuleCorner"
    )

    // Tooltip auto-hide timeout
    var showTooltip by remember { mutableStateOf(false) }
    LaunchedEffect(isDragging, isSingEnabled) {
        if (isDragging) {
            showTooltip = true
        } else if (isSingEnabled) {
            showTooltip = true
            delay(1500)
            showTooltip = false
        } else {
            showTooltip = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.padding(end = 12.dp)
    ) {
        // Live Tooltip Badge (Left of slider)
        AnimatedVisibility(
            visible = isSingEnabled && (showTooltip || isDragging),
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.85f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.85f)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1E24).copy(alpha = 0.94f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val percent = (vocalVolume * 100f).roundToInt()
                val label = when {
                    percent <= 5 -> "Karaoke (Voz apagada)"
                    percent >= 95 -> "Voz Original (100%)"
                    else -> "Voz: $percent%"
                }
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }
        }

        // Apple Music Sing Vertical Capsule (Bar / Slider)
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(animatedCapsuleHeight)
                .shadow(
                    elevation = if (isSingEnabled) 16.dp else 10.dp,
                    shape = RoundedCornerShape(animatedCornerRadius),
                    spotColor = Color.Black.copy(alpha = 0.55f)
                )
                .clip(RoundedCornerShape(animatedCornerRadius))
                .background(
                    if (isSingEnabled) Color(0xFF202026).copy(alpha = 0.76f)
                    else Color(0xFF1C1C22).copy(alpha = 0.70f)
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isSingEnabled) 0.38f else 0.28f),
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = if (isSingEnabled) 0.28f else 0.18f)
                        )
                    ),
                    shape = RoundedCornerShape(animatedCornerRadius)
                )
                .onSizeChanged { sliderHeightPx = it.height.toFloat() }
                .then(
                    if (!isSingEnabled) {
                        Modifier.clickable {
                            AppleMusicSingManager.setSingEnabled(true)
                        }
                    } else {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val topPadding = 36.dp.toPx()
                                        val bottomPadding = 48.dp.toPx()
                                        val availableTrack = sliderHeightPx - topPadding - bottomPadding
                                        if (availableTrack > 0f) {
                                            val trackOffset = (offset.y - topPadding).coerceIn(0f, availableTrack)
                                            val newVol = 1f - (trackOffset / availableTrack)
                                            AppleMusicSingManager.setVocalVolume(newVol)
                                            showTooltip = true
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = { isDragging = false },
                                    onDragCancel = { isDragging = false },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val topPadding = 36.dp.toPx()
                                        val bottomPadding = 48.dp.toPx()
                                        val availableTrack = sliderHeightPx - topPadding - bottomPadding
                                        if (availableTrack > 0f) {
                                            val trackOffset = (change.position.y - topPadding).coerceIn(0f, availableTrack)
                                            val newVol = 1f - (trackOffset / availableTrack)
                                            AppleMusicSingManager.setVocalVolume(newVol)
                                        }
                                    }
                                )
                            }
                    }
                )
        ) {
            if (!isSingEnabled) {
                // Collapsed Standby Microphone Pill
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Apple Music Sing",
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(24.dp)
                    )
                    // Sparkle badge indicator
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFDE69B),
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                    )
                }
            } else {
                // Background Track Fill (from bottom upwards)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = animatedFill.coerceIn(0.04f, 1f))
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp, topStart = 16.dp, topEnd = 16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    Color.White.copy(alpha = 0.85f),
                                    Color(0xFFE5E5EA).copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // Top Minimize / Collapse Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .align(Alignment.TopCenter)
                        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                        .clickable {
                            AppleMusicSingManager.setSingEnabled(false)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimizar Sing",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Level guide ticks
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp, bottom = 52.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(4) { idx ->
                        if (idx > 0 && idx < 3) {
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(1.5.dp)
                                    .background(Color.White.copy(alpha = 0.28f), CircleShape)
                            )
                        }
                    }
                }

                // Interactive Mic Icon Button at Bottom of Capsule
                val micTint = if (animatedFill > 0.35f) Color(0xFF1C1C1E) else Color.White
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .align(Alignment.BottomCenter)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (vocalVolume <= 0.05f) {
                                AppleMusicSingManager.setVocalVolume(1f)
                            } else {
                                AppleMusicSingManager.setVocalVolume(0f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (vocalVolume <= 0.05f) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Apple Music Sing",
                        tint = micTint,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                scaleX = if (isDragging) 1.15f else 1.05f
                                scaleY = if (isDragging) 1.15f else 1.05f
                            }
                    )
                }
            }
        }
    }
}
