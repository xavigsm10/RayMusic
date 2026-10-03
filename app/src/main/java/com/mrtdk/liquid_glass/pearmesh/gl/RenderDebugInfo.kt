package com.mrtdk.liquid_glass.pearmesh.gl

data class RenderDebugInfo(
    val fps: Float = 0f,
    val frameTimeMs: Float = 0f,
    val renderCpuPercent: Float = 0f,
    val surfaceWidth: Int = 0,
    val surfaceHeight: Int = 0,
    val outputWidth: Int = 0,
    val outputHeight: Int = 0,
    val targetFps: Int = 0,
    val gpuRenderer: String = "Unknown",
)
