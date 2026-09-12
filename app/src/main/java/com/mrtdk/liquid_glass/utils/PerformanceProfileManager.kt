package com.mrtdk.liquid_glass.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PerformanceTier {
    LOW_END,
    MID_RANGE,
    HIGH_END
}

data class PerformanceConfig(
    val tier: PerformanceTier,
    val fluidScale: Float,
    val fluidBlurDp: Float,
    val lyricsBlurEnabled: Boolean,
    val maxThumbnailSizeDp: Int,
    val maxCoverArtSizeDp: Int,
    val reflectionBlurRadius: Float,
    val reflectionMeshSize: Int,
    val maxHistorySize: Int,
    val motionCoverIntervalMs: Long
)

object PerformanceProfileManager {
    private val LOW_CONFIG = PerformanceConfig(
        tier = PerformanceTier.LOW_END,
        fluidScale = 0.40f,
        fluidBlurDp = 26f,
        lyricsBlurEnabled = true,
        maxThumbnailSizeDp = 180,
        maxCoverArtSizeDp = 480,
        reflectionBlurRadius = 45f,
        reflectionMeshSize = 8,
        maxHistorySize = 50,
        motionCoverIntervalMs = 200L
    )

    private val MID_CONFIG = PerformanceConfig(
        tier = PerformanceTier.MID_RANGE,
        fluidScale = 0.50f,
        fluidBlurDp = 24f,
        lyricsBlurEnabled = true,
        maxThumbnailSizeDp = 200,
        maxCoverArtSizeDp = 600,
        reflectionBlurRadius = 55f,
        reflectionMeshSize = 8,
        maxHistorySize = 80,
        motionCoverIntervalMs = 160L
    )

    private val HIGH_CONFIG = PerformanceConfig(
        tier = PerformanceTier.HIGH_END,
        fluidScale = 0.75f,
        fluidBlurDp = 48f,
        lyricsBlurEnabled = true,
        maxThumbnailSizeDp = 280,
        maxCoverArtSizeDp = 900,
        reflectionBlurRadius = 95f,
        reflectionMeshSize = 12,
        maxHistorySize = 150,
        motionCoverIntervalMs = 90L
    )

    private val _config = MutableStateFlow(MID_CONFIG)
    val config: StateFlow<PerformanceConfig> = _config.asStateFlow()

    fun getConfig(): PerformanceConfig {
        if (LibraryManager.getFullArtworkBackdropStyle() == "accord") {
            return LOW_CONFIG
        }
        return _config.value
    }

    fun init(context: Context) {
        val savedTier = LibraryManager.getString("performance_tier_preference", "auto")
        val resolvedTier = when (savedTier) {
            "low" -> PerformanceTier.LOW_END
            "mid" -> PerformanceTier.MID_RANGE
            "high" -> PerformanceTier.HIGH_END
            else -> detectTier(context)
        }
        if (LibraryManager.getFullArtworkBackdropStyle() == "accord") {
            setTier(PerformanceTier.LOW_END)
        } else {
            setTier(resolvedTier)
        }
    }

    fun setTier(tier: PerformanceTier) {
        _config.value = when (tier) {
            PerformanceTier.LOW_END -> LOW_CONFIG
            PerformanceTier.MID_RANGE -> MID_CONFIG
            PerformanceTier.HIGH_END -> HIGH_CONFIG
        }
    }

    fun detectTier(context: Context): PerformanceTier {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val isLowRam = am?.isLowRamDevice == true
        val cpuCores = Runtime.getRuntime().availableProcessors()

        return when {
            isLowRam || totalRamMb < 4200 || cpuCores <= 4 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                PerformanceTier.LOW_END
            }
            totalRamMb < 7500 || cpuCores < 8 -> {
                PerformanceTier.MID_RANGE
            }
            else -> {
                PerformanceTier.HIGH_END
            }
        }
    }
}
