package com.mrtdk.liquid_glass.data.lyrics

/** Shorter instrumental breaks aren't worth interrupting the line for. */
internal const val MIN_GAP_MS = 4_000L

/**
 * Marks the instrumental stretches with blank lines, the way an LRC file
 * marks them with a bare timestamp.
 *
 * A break is only drawn where the line before it says when its singing
 * stopped — see [LyricLine.hasKnownEnd]. Given that, the note appears the
 * moment the vocal ends rather than several seconds later once the next line
 * was due, which is the whole advantage over [LrcLib.parseLrc]'s stamp-to-stamp
 * guess. Without it there is nothing to measure silence against: the distance
 * to the next stamp is the line's own slot, and treating that as a break puts a
 * note after every single line of a line-synced source.
 */
internal fun List<LyricLine>.withInstrumentalGaps(): List<LyricLine> {
    if (isEmpty() || any { it.isGap }) return this
    val out = ArrayList<LyricLine>(size + 4)
    // Nothing stands for the intro, so give the run-up its own break.
    if (first().timeMs >= MIN_GAP_MS) {
        out += LyricLine(0L, "", sungUntilMs = first().timeMs)
    }
    forEachIndexed { index, line ->
        out += line
        val next = getOrNull(index + 1) ?: return@forEachIndexed
        val lineEnd = when {
            line.hasKnownEnd -> line.endMs
            else -> {
                // If not word-synced, estimate singing duration from line length
                val chars = line.text.trim().length
                val estimatedDuration = (chars * 120L + 1200L).coerceIn(2000L, 5000L)
                minOf(line.timeMs + estimatedDuration, next.timeMs)
            }
        }
        val silence = next.timeMs - lineEnd
        if (silence >= MIN_GAP_MS && lineEnd > line.timeMs) {
            out += LyricLine(lineEnd, "", sungUntilMs = next.timeMs)
        }
    }
    return out
}
