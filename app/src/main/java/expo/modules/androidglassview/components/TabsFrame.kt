package expo.modules.androidglassview.components

/**
 * Where the tabs are drawn right now, in the host's coordinates. React Native lays the tabs out
 * for the expanded bar; while the bar minimizes, [mapX] moves a point from those slots to the
 * current, narrower ones.
 */
internal class TabsFrame(
    /** 0 = expanded, 1 = minimized. */
    val minimizeProgress: Float,
    private val expandedSlotsLeft: Float,
    private val slotsLeft: Float,
    private val slotsScale: Float,
    /** Vertical centre of the bar. */
    val centerY: Float
) {
    fun mapX(x: Float): Float = slotsLeft + (x - expandedSlotsLeft) * slotsScale
}
