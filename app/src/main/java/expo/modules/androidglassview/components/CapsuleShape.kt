package expo.modules.androidglassview.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

/**
 * Stand-in for Kyant's `Capsule()` from io.github.kyant0:shapes (not bundled). A circular-corner
 * capsule instead of a continuous-curvature one; the lens effect supports any CornerBasedShape.
 */
internal val CapsuleShape: Shape = RoundedCornerShape(percent = 50)
