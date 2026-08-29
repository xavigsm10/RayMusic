package com.mrtdk.liquid_glass.ui.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Springy "wobble" on press for an existing [interactionSource]: the target scales down
 * while held and overshoots back on release (low damping = bouncy).
 */
fun Modifier.pressWobble(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.86f,
) = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.38f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressWobble",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Clickable with no ripple by default. */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onClick: () -> Unit
): Modifier = clickable(
    interactionSource = interactionSource,
    indication = indication,
    enabled = enabled,
    onClick = onClick
)

/** [combinedClickable] with no ripple by default. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedBounceClick(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = combinedClickable(
    interactionSource = interactionSource,
    indication = indication,
    enabled = enabled,
    onLongClick = onLongClick,
    onDoubleClick = onDoubleClick,
    onClick = onClick
)
