package com.mrtdk.liquid_glass.utils

import android.util.Log
import com.echo.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

object BotDetectionMitigator {
    private const val TAG = "BotDetectionMitigator"
    private val failureCount = AtomicInteger(0)

    private val GEO_ERROR_SIGNATURES = listOf(
        "not available in your country",
        "not available in your region",
        "not available in this country",
        "not available in this region",
        "geo-restricted",
        "GEO_RESTRICTED",
        "NOT_AVAILABLE_IN_THIS_COUNTRY",
        "only available in certain countries",
        "country restriction",
        "region restriction",
    )

    private val BOT_ERROR_SIGNATURES = listOf(
        "Sign in to confirm",
        "confirm you're not a bot",
        "automated queries",
        "Error 2000",
        "403",
        "This content isn't available on this device",
    )

    fun notifyPlaybackFailure(isLoggedIn: Boolean, errorMessage: String? = null): Boolean {
        if (isLoggedIn) return false
        if (isGeoError(errorMessage)) return false

        failureCount.incrementAndGet()
        return true
    }

    fun notifyPlaybackSuccess() {
        failureCount.set(0)
    }

    suspend fun rotateGuestSession() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Rotating guest session to bypass bot detection...")
        try {
            val currentLocale = YouTube.locale
            YouTube.visitorData = null
            YouTube.refreshVisitorData().onSuccess { newData ->
                Log.i(TAG, "New visitorData obtained successfully for region ${currentLocale.gl}: $newData")
            }.onFailure { e ->
                Log.e(TAG, "Failed to refresh visitorData during rotation", e)
                YouTube.locale = currentLocale
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in rotateGuestSession", e)
        }
        failureCount.set(0)
    }

    fun isGeoError(message: String?): Boolean {
        if (message == null) return false
        val lower = message.lowercase()
        return GEO_ERROR_SIGNATURES.any { lower.contains(it.lowercase()) }
    }

    fun isBotDetectionError(message: String?): Boolean {
        if (message == null) return false
        val lower = message.lowercase()
        return BOT_ERROR_SIGNATURES.any { lower.contains(it.lowercase()) }
    }

    fun reset() {
        failureCount.set(0)
    }
}
