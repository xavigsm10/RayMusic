package com.mrtdk.liquid_glass.playback.eq

import android.content.Context
import android.util.Log
import com.mrtdk.liquid_glass.data.LibraryManager
import java.util.concurrent.CopyOnWriteArrayList

object EqualizerService {
    private const val TAG = "EqualizerService"
    
    private val processors = CopyOnWriteArrayList<CustomEqualizerAudioProcessor>()
    
    val bandFrequencies = doubleArrayOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
    
    var isEnabled: Boolean = false
        private set
        
    var isAdvancedMode: Boolean = false
        private set
        
    private val simpleGains = doubleArrayOf(0.0, 0.0, 0.0)
    private val advancedGains = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    
    var preamp: Double = 0.0
        private set

    enum class Preset(val key: String, val category: String, val simpleGains: DoubleArray, val advancedGains: DoubleArray) {
        // RaySignature
        FLAT("flat", "ray_signature", doubleArrayOf(0.0, 0.0, 0.0), doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)),
        RAY_SIGNATURE("ray_signature_preset", "ray_signature", doubleArrayOf(2.5, 1.0, 2.0), doubleArrayOf(3.0, 2.5, 1.5, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 2.5)),
        ACOUSTIC("acoustic", "ray_signature", doubleArrayOf(2.0, 0.5, 1.5), doubleArrayOf(3.0, 2.0, 1.0, 0.5, 0.5, 1.0, 1.5, 2.0, 2.5, 2.0)),
        THREE_D_STAGE("3d_stage", "ray_signature", doubleArrayOf(1.5, -0.5, 1.5), doubleArrayOf(2.0, 1.5, 0.5, -0.5, -1.0, -0.5, 0.5, 1.5, 2.0, 2.5)),
        BASS_BOOSTER("bass_booster", "ray_signature", doubleArrayOf(4.5, 0.0, 0.0), doubleArrayOf(5.0, 4.0, 3.0, 1.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)),
        PURE_CLARITY("pure_clarity", "ray_signature", doubleArrayOf(-1.0, 2.5, 3.0), doubleArrayOf(-2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 3.5, 3.5, 3.0, 2.5)),
        SOFT_BASS("soft_bass", "ray_signature", doubleArrayOf(3.0, 0.5, -0.5), doubleArrayOf(3.5, 3.0, 2.5, 1.5, 0.5, 0.0, -0.5, -0.5, -0.5, -1.0)),
        ELECTRONIC("electronic", "ray_signature", doubleArrayOf(3.5, -1.5, 3.5), doubleArrayOf(4.0, 3.5, 2.0, 0.0, -2.0, -1.5, 0.0, 2.0, 3.5, 4.0)),
        ROCK("rock", "ray_signature", doubleArrayOf(3.0, -1.5, 3.0), doubleArrayOf(4.0, 3.0, 2.0, -1.0, -2.0, -1.0, 1.0, 2.0, 3.0, 4.0)),
        POP("pop", "ray_signature", doubleArrayOf(-1.0, 3.0, -1.0), doubleArrayOf(-1.5, -1.0, 0.0, 2.0, 4.0, 4.0, 2.0, 0.0, -1.0, -1.5)),
        JAZZ("jazz", "ray_signature", doubleArrayOf(2.0, -0.5, 1.5), doubleArrayOf(3.0, 2.0, 1.5, 2.0, -1.0, -1.0, 0.0, 1.0, 2.0, 3.0)),
        VOICE("voice", "ray_signature", doubleArrayOf(-2.0, 3.5, 0.5), doubleArrayOf(-3.0, -2.0, -1.5, 1.0, 3.0, 4.0, 4.0, 2.0, 0.0, -1.5)),

        // Dolby Atmos
        DOLBY_OPEN("dolby_open", "dolby_atmos", doubleArrayOf(1.0, 0.0, 1.5), doubleArrayOf(1.5, 1.0, 0.5, 0.0, -0.5, 0.0, 0.5, 1.0, 1.5, 2.0)),
        DOLBY_RICH("dolby_rich", "dolby_atmos", doubleArrayOf(3.0, 1.5, 1.0), doubleArrayOf(4.0, 3.5, 2.5, 2.0, 1.5, 1.0, 1.0, 0.5, 0.5, 1.0)),
        DOLBY_FOCUSED("dolby_focused", "dolby_atmos", doubleArrayOf(-1.0, 3.0, 1.5), doubleArrayOf(-2.0, -1.5, -0.5, 1.5, 3.0, 3.5, 3.0, 2.0, 1.5, 1.0)),

        // Dirac Audio
        DIRAC_MUSIC("dirac_music", "dirac_audio", doubleArrayOf(2.0, 1.0, 2.0), doubleArrayOf(2.5, 2.0, 1.5, 1.0, 0.5, 1.0, 1.5, 2.0, 2.5, 2.0)),
        DIRAC_MOVIE("dirac_movie", "dirac_audio", doubleArrayOf(3.5, 2.0, 0.5), doubleArrayOf(4.0, 3.5, 3.0, 2.5, 2.0, 1.5, 1.0, 0.5, 0.0, 0.5)),
        DIRAC_GAME("dirac_game", "dirac_audio", doubleArrayOf(2.5, -0.5, 3.5), doubleArrayOf(3.0, 2.5, 1.0, -0.5, -1.0, -0.5, 1.0, 2.5, 3.5, 4.0))
    }

    data class PresetCategory(val key: String, val presets: List<Preset>)

    val presetCategories: List<PresetCategory>
        get() = Preset.values().groupBy { it.category }
            .map { (cat, presets) -> PresetCategory(cat, presets) }

    fun init(context: Context) {
        isEnabled = LibraryManager.getString("eq_enabled", "false") == "true"
        isAdvancedMode = LibraryManager.getString("eq_mode", "simple") == "advanced"
        preamp = LibraryManager.getString("eq_preamp", "0.0")?.toDoubleOrNull() ?: 0.0
        
        val simpleStr = LibraryManager.getString("eq_gains_simple", "0.0,0.0,0.0") ?: "0.0,0.0,0.0"
        val simpleParts = simpleStr.split(",")
        for (i in 0 until minOf(simpleGains.size, simpleParts.size)) {
            simpleGains[i] = simpleParts[i].toDoubleOrNull() ?: 0.0
        }
        
        val advStr = LibraryManager.getString("eq_gains_advanced", "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0") ?: "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0"
        val advParts = advStr.split(",")
        for (i in 0 until minOf(advancedGains.size, advParts.size)) {
            advancedGains[i] = advParts[i].toDoubleOrNull() ?: 0.0
        }
        
        Log.d(TAG, "Initialized: enabled=$isEnabled, advanced=$isAdvancedMode, simpleGains=${simpleGains.joinToString()}, advancedGains=${advancedGains.joinToString()}")
    }

    fun addAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        processors.add(processor)
        applyCurrentProfileToProcessor(processor)
    }

    fun removeAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        processors.remove(processor)
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        LibraryManager.saveString("eq_enabled", enabled.toString())
        applyToAllProcessors()
    }

    @Synchronized
    fun setAdvancedMode(advanced: Boolean) {
        isAdvancedMode = advanced
        LibraryManager.saveString("eq_mode", if (advanced) "advanced" else "simple")
        applyToAllProcessors()
    }

    @Synchronized
    fun setPreamp(value: Double) {
        preamp = value.coerceIn(-15.0, 15.0)
        LibraryManager.saveString("eq_preamp", preamp.toString())
        applyToAllProcessors()
    }

    @Synchronized
    fun setSimpleGains(bass: Double, mid: Double, treble: Double) {
        simpleGains[0] = bass.coerceIn(-12.0, 12.0)
        simpleGains[1] = mid.coerceIn(-12.0, 12.0)
        simpleGains[2] = treble.coerceIn(-12.0, 12.0)
        
        LibraryManager.saveString("eq_gains_simple", simpleGains.joinToString(","))
        if (!isAdvancedMode) {
            applyToAllProcessors()
        }
    }

    @Synchronized
    fun setAdvancedGain(index: Int, gain: Double) {
        if (index in advancedGains.indices) {
            advancedGains[index] = gain.coerceIn(-12.0, 12.0)
            LibraryManager.saveString("eq_gains_advanced", advancedGains.joinToString(","))
            if (isAdvancedMode) {
                applyToAllProcessors()
            }
        }
    }

    fun getSimpleGains(): DoubleArray = simpleGains.clone()
    
    fun getAdvancedGains(): DoubleArray = advancedGains.clone()

    @Synchronized
    fun reset() {
        preamp = 0.0
        LibraryManager.saveString("eq_preamp", "0.0")
        for (i in simpleGains.indices) simpleGains[i] = 0.0
        LibraryManager.saveString("eq_gains_simple", simpleGains.joinToString(","))
        for (i in advancedGains.indices) advancedGains[i] = 0.0
        LibraryManager.saveString("eq_gains_advanced", advancedGains.joinToString(","))
        applyToAllProcessors()
    }

    @Synchronized
    fun applyPreset(preset: Preset) {
        System.arraycopy(preset.simpleGains, 0, simpleGains, 0, simpleGains.size)
        LibraryManager.saveString("eq_gains_simple", simpleGains.joinToString(","))
        
        System.arraycopy(preset.advancedGains, 0, advancedGains, 0, advancedGains.size)
        LibraryManager.saveString("eq_gains_advanced", advancedGains.joinToString(","))
        
        applyToAllProcessors()
    }

    private fun applyToAllProcessors() {
        processors.forEach { applyCurrentProfileToProcessor(it) }
    }

    private fun applyCurrentProfileToProcessor(processor: CustomEqualizerAudioProcessor) {
        if (!isEnabled) {
            processor.disable()
            return
        }

        val bands = mutableListOf<ParametricEQBand>()
        if (isAdvancedMode) {
            for (i in bandFrequencies.indices) {
                bands.add(
                    ParametricEQBand(
                        frequency = bandFrequencies[i],
                        gain = advancedGains[i],
                        q = 1.41,
                        filterType = FilterType.PK,
                        enabled = true
                    )
                )
            }
        } else {
            val bass = simpleGains[0]
            val mid = simpleGains[1]
            val treble = simpleGains[2]

            val blendedGains = doubleArrayOf(
                bass,                                 // 31 Hz
                bass,                                 // 62 Hz
                bass * 0.8 + mid * 0.2,               // 125 Hz
                bass * 0.5 + mid * 0.5,               // 250 Hz
                bass * 0.2 + mid * 0.8,               // 500 Hz
                mid,                                  // 1000 Hz
                mid * 0.8 + treble * 0.2,             // 2000 Hz
                mid * 0.5 + treble * 0.5,             // 4000 Hz
                mid * 0.2 + treble * 0.8,             // 8000 Hz
                treble                                // 16000 Hz
            )

            for (i in bandFrequencies.indices) {
                bands.add(
                    ParametricEQBand(
                        frequency = bandFrequencies[i],
                        gain = blendedGains[i],
                        q = 1.41,
                        filterType = FilterType.PK,
                        enabled = true
                    )
                )
            }
        }

        val profile = ParametricEQ(
            preamp = preamp,
            bands = bands
        )
        processor.applyProfile(profile)
    }
}
