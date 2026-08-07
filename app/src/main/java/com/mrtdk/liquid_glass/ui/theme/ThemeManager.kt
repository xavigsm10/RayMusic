package com.mrtdk.liquid_glass.ui.theme

import androidx.compose.ui.graphics.Color
import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ThemeManager {
    private const val KEY_THEME_MODE = "app_theme_mode"
    const val MODE_DARK = "DARK"
    const val MODE_LIGHT = "LIGHT"

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun init() {
        val saved = LibraryManager.getString(KEY_THEME_MODE, MODE_DARK) ?: MODE_DARK
        _isDarkMode.value = saved != MODE_LIGHT
    }

    fun setThemeMode(mode: String) {
        val isDark = mode != MODE_LIGHT
        _isDarkMode.value = isDark
        LibraryManager.saveString(KEY_THEME_MODE, if (isDark) MODE_DARK else MODE_LIGHT)
    }

    fun getThemeMode(): String {
        return if (_isDarkMode.value) MODE_DARK else MODE_LIGHT
    }

    val backgroundColor: Color
        get() = if (_isDarkMode.value) Color(0xFF000000) else Color(0xFFF2F2F7)

    val surfaceColor: Color
        get() = if (_isDarkMode.value) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)

    val textColor: Color
        get() = if (_isDarkMode.value) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)

    val subtextColor: Color
        get() = if (_isDarkMode.value) Color(0xFFAAAAAA) else Color(0xFF636366)

    val dividerColor: Color
        get() = if (_isDarkMode.value) Color.DarkGray.copy(alpha = 0.5f) else Color(0xFFE5E5EA)

    val glassContainerColor: Color
        get() = if (_isDarkMode.value) Color(0xFF1C1C1E).copy(alpha = 0.8f) else Color(0xFFFFFFFF).copy(alpha = 0.9f)

    val accentColor: Color = Color(0xFFFA243C)
}
