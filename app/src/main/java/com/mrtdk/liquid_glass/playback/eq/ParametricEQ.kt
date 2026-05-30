package com.mrtdk.liquid_glass.playback.eq

data class ParametricEQBand(
    val frequency: Double,
    val gain: Double,
    val q: Double = 1.41,
    val filterType: FilterType = FilterType.PK,
    val enabled: Boolean = true
)

data class ParametricEQ(
    val preamp: Double,
    val bands: List<ParametricEQBand>
)
