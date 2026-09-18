package com.kyant.backdrop.highlight

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import expo.modules.androidglassview.backdrop.highlight.highlight as expoHighlight

typealias Highlight = expo.modules.androidglassview.backdrop.highlight.Highlight
typealias HighlightStyle = expo.modules.androidglassview.backdrop.highlight.HighlightStyle

fun Modifier.highlight(
    shape: () -> Shape,
    highlight: () -> Highlight? = { Highlight.Default }
): Modifier = this.expoHighlight(shape, highlight)
