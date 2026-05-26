package com.mrtdk.liquid_glass.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
import com.mrtdk.liquid_glass.data.LibraryManager

object LocaleUtils {
    const val SYSTEM_DEFAULT = "SYSTEM_DEFAULT"

    @Suppress("DEPRECATION")
    fun setAppLocale(context: Context, locale: Locale) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        
        // Also update applicationContext to make sure string updates apply to application context
        val appContext = context.applicationContext
        val appConfig = Configuration(appContext.resources.configuration)
        appConfig.setLocale(locale)
        appContext.resources.updateConfiguration(appConfig, appContext.resources.displayMetrics)
    }

    fun applyLocale(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val lang = LibraryManager.getAppLanguage(context)
            val locale = if (lang == SYSTEM_DEFAULT) {
                Locale.getDefault()
            } else {
                Locale.forLanguageTag(lang)
            }
            setAppLocale(context, locale)
        }
    }
}
