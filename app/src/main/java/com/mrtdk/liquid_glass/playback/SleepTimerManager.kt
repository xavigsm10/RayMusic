package com.mrtdk.liquid_glass.playback

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Gestor global del Temporizador de Reposo (Sleep Timer) al estilo Apple Music.
 * Permite apagar la música tras un tiempo fijado (15m, 30m, 45m, 1h...)
 * o "Al finalizar la canción actual", aplicando un suave fundido de salida (fade-out)
 * en los últimos 8 segundos para evitar un corte brusco.
 */
object SleepTimerManager {

    enum class TimerMode {
        OFF,
        DURATION,
        END_OF_SONG
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    private val _mode = MutableStateFlow(TimerMode.OFF)
    val mode: StateFlow<TimerMode> = _mode.asStateFlow()

    private val _remainingSeconds = MutableStateFlow<Long?>(null)
    val remainingSeconds: StateFlow<Long?> = _remainingSeconds.asStateFlow()

    val isActive: Boolean
        get() = _mode.value != TimerMode.OFF

    // Callback invocado por MusicService para ejecutar la acción de pausa y fade-out
    var onPerformFadeOutAndPause: ((durationMs: Long, onComplete: () -> Unit) -> Unit)? = null
    var onPauseImmediate: (() -> Unit)? = null

    /**
     * Inicia un temporizador para apagarse tras X minutos.
     */
    fun startTimerMinutes(minutes: Int) {
        cancelTimer()
        if (minutes <= 0) return

        val totalSecs = minutes.toLong() * 60L
        _mode.value = TimerMode.DURATION
        _remainingSeconds.value = totalSecs

        timerJob = scope.launch {
            var current = totalSecs
            var hasStartedFade = false

            while (isActive && current > 0) {
                delay(1000L)
                current--
                _remainingSeconds.value = current

                // Iniciar fade-out suave en los últimos 8 segundos
                if (current <= 8 && !hasStartedFade) {
                    hasStartedFade = true
                    onPerformFadeOutAndPause?.invoke(current * 1000L) {
                        finishTimer()
                    }
                }
            }

            if (!hasStartedFade) {
                finishTimer()
            }
        }
    }

    /**
     * Activa el modo de apagar la reproducción al finalizar la canción en curso.
     */
    fun startEndOfSong() {
        cancelTimer()
        _mode.value = TimerMode.END_OF_SONG
        _remainingSeconds.value = null
    }

    /**
     * Invocado por MusicService cuando una canción concluye de forma natural.
     * Si el modo es END_OF_SONG, pausará la reproducción.
     */
    fun onSongFinishedNaturally(): Boolean {
        if (_mode.value == TimerMode.END_OF_SONG) {
            onPauseImmediate?.invoke()
            finishTimer()
            return true
        }
        return false
    }

    /**
     * Cancela el temporizador activo sin pausar la música.
     */
    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _mode.value = TimerMode.OFF
        _remainingSeconds.value = null
    }

    private fun finishTimer() {
        timerJob?.cancel()
        timerJob = null
        _mode.value = TimerMode.OFF
        _remainingSeconds.value = null
        onPauseImmediate?.invoke()
    }

    /**
     * Devuelve el tiempo restante formateado como mm:ss (ej: "18:42")
     * o la descripción del modo activo.
     */
    fun getFormattedRemaining(): String? {
        return when (_mode.value) {
            TimerMode.OFF -> null
            TimerMode.END_OF_SONG -> "Al finalizar canción"
            TimerMode.DURATION -> {
                val secs = _remainingSeconds.value ?: return null
                val m = secs / 60
                val s = secs % 60
                String.format(Locale.getDefault(), "%02d:%02d", m, s)
            }
        }
    }
}
