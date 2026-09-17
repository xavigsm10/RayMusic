/*
 * Copyright 2025 Kyant (https://github.com/Kyant0/AndroidLiquidGlass)
 * Licensed under the Apache License, Version 2.0. See THIRD_PARTY_NOTICES.md.
 *
 * Vendored into expo-android-glass-view. Changes from upstream: package relocated
 * from com.kyant.backdrop, Kotlin Multiplatform expect/actual merged into Android-only
 * code, dependency on io.github.kyant0:shapes removed, Kotlin 2.1 compatible syntax.
 */
package expo.modules.androidglassview.backdrop.internal

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path

internal fun Canvas.clipOutline(outline: Outline, path: Path?) {
    when (outline) {
        is Outline.Rectangle -> clipRect(outline.rect)
        is Outline.Rounded -> {
            path!!.rewind()
            path.addRoundRect(outline.roundRect)
            clipPath(path)
        }

        is Outline.Generic -> clipPath(outline.path)
    }
}
