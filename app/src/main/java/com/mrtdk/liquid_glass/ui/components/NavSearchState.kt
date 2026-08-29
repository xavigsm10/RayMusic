package com.mrtdk.liquid_glass.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Search input state shared between navigation bar and search screen.
 */
@Immutable
data class NavSearchState(
    val visualActive: Boolean = false,
    val keyboardActive: Boolean = false,
    val query: TextFieldValue = TextFieldValue(),
    val onQueryChange: (TextFieldValue) -> Unit = {},
    val onSubmit: (String) -> Unit = {},
    val onTapSearchIcon: () -> Unit = {},
    val onTapBar: () -> Unit = {},
    val onExit: () -> Unit = {},
    val onCloseKeyboard: () -> Unit = {},
    val focusRequester: FocusRequester = FocusRequester(),
)

val LocalNavSearchState = staticCompositionLocalOf { NavSearchState() }
