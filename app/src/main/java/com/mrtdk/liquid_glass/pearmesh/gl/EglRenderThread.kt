package com.mrtdk.liquid_glass.pearmesh.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.os.Debug
import android.util.Log
import android.view.Surface
import com.mrtdk.liquid_glass.pearmesh.RendererState
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EglRenderThread(
    private val context: Context,
    private val surface: Surface,
    initialWidth: Int,
    initialHeight: Int,
    private val stateProvider: () -> RendererState,
    private val statsListener: (RenderDebugInfo) -> Unit = {},
    private val onRenderingPaused: () -> Unit = {},
    private val maxContinuousRenderMillis: Long? = null,
    initiallyRenderingEnabled: Boolean = true,
    private val frameCaptureRequest: AtomicReference<((Bitmap) -> Unit)?>? = null,
) {
    private val rendererId = nextRendererId.incrementAndGet()
    private val running = AtomicBoolean(false)
    private val renderingEnabled = AtomicBoolean(initiallyRenderingEnabled)
    private val idle = AtomicBoolean(false)
    private val size = AtomicLong(packSize(initialWidth, initialHeight))
    private val renderedFrameCount = AtomicLong(0L)
    private val renderSessionGeneration = AtomicLong(0L)
    private val pauseCallbackPending = AtomicBoolean(false)
    private val renderingLock = ReentrantLock()
    private val renderingCondition = renderingLock.newCondition()
    private var thread: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        logAod("start enabled=${renderingEnabled.get()}")
        thread = Thread(::run, "PearMesh-EGL").also { it.start() }
    }

    fun resize(width: Int, height: Int) {
        size.set(packSize(width, height))
    }

    fun setRenderingEnabled(enabled: Boolean) {
        if (renderingEnabled.getAndSet(enabled) == enabled) return
        if (!enabled) pauseCallbackPending.set(true)
        else {
            pauseCallbackPending.set(false)
            idle.set(false)
        }
        logAod("enabled=$enabled totalFrames=${renderedFrameCount.get()}")
        wakeRenderer()
    }

    fun restartRenderSession() {
        if (!renderingEnabled.get()) return
        renderSessionGeneration.incrementAndGet()
        idle.set(false)
        logAod("sessionRestart totalFrames=${renderedFrameCount.get()}")
        wakeRenderer()
    }

    /** Wakes a power-saving idle renderer and starts a fresh continuous-render window. */
    fun wakeFromIdle() {
        if (!renderingEnabled.get()) return
        if (idle.getAndSet(false)) {
            renderSessionGeneration.incrementAndGet()
            logAod("wakeFromIdle totalFrames=${renderedFrameCount.get()}")
            wakeRenderer()
        }
    }

    private fun wakeRenderer() {
        renderingLock.withLock {
            renderingCondition.signalAll()
        }
        thread?.interrupt()
    }

    fun stopAndJoin() {
        running.set(false)
        logAod("stop totalFrames=${renderedFrameCount.get()}")
        renderingLock.withLock {
            renderingCondition.signalAll()
        }
        val activeThread = thread ?: return
        activeThread.interrupt()
        if (Thread.currentThread() !== activeThread) {
            runCatching { activeThread.join(1_000L) }
        }
        thread = null
    }

    private fun run() {
        var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        var renderer: PearMeshGlRenderer? = null

        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display !== EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed" }
            check(EGL14.eglInitialize(display, IntArray(2), 0, IntArray(2), 1)) {
                "eglInitialize failed: 0x${EGL14.eglGetError().toString(16)}"
            }

            val config = chooseConfig(display)
            eglContext = EGL14.eglCreateContext(
                display,
                config,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE),
                0,
            )
            check(eglContext !== EGL14.EGL_NO_CONTEXT) {
                "eglCreateContext failed: 0x${EGL14.eglGetError().toString(16)}"
            }

            eglSurface = EGL14.eglCreateWindowSurface(
                display,
                config,
                surface,
                intArrayOf(EGL14.EGL_NONE),
                0,
            )
            check(eglSurface !== EGL14.EGL_NO_SURFACE) {
                "eglCreateWindowSurface failed: 0x${EGL14.eglGetError().toString(16)}"
            }
            check(
                EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)
            ) { "eglMakeCurrent failed: 0x${EGL14.eglGetError().toString(16)}" }
            EGL14.eglSwapInterval(display, 1)

            renderer = PearMeshGlRenderer(context, stateProvider(), frameCaptureRequest)
            var nextFrame = System.nanoTime()
            var previousFrameStart = nextFrame
            var animationNanos = 0L
            var statsWindowStart = System.nanoTime()
            var statsFrameCount = 0
            var statsRenderNanos = 0L
            var diagnosticWindowCount = 0
            var renderSessionStart = System.nanoTime()
            var observedSessionGeneration = renderSessionGeneration.get()
            var lastThreadCpuNanos = Debug.threadCpuTimeNanos()
            while (running.get()) {
                val resumedAfterPause = awaitRenderingEnabled()
                if (!running.get()) break

                val frameStart = System.nanoTime()
                val state = stateProvider()
                val sessionGeneration = renderSessionGeneration.get()
                if (resumedAfterPause || sessionGeneration != observedSessionGeneration) {
                    previousFrameStart = frameStart
                    nextFrame = frameStart
                    renderSessionStart = frameStart
                    observedSessionGeneration = sessionGeneration
                } else {
                    if (state.isPlaybackPlaying || !state.pauseFlowEnabled) {
                        animationNanos += (frameStart - previousFrameStart).coerceAtLeast(0L)
                    }
                    previousFrameStart = frameStart
                }
                val packedSize = size.get()
                val width = unpackWidth(packedSize)
                val height = unpackHeight(packedSize)
                if (width > 0 && height > 0) {
                    val renderStart = System.nanoTime()
                    renderer.render(
                        width = width,
                        height = height,
                        state = state,
                        time = animationNanos / 1_000_000_000.0,
                    )
                    statsFrameCount++
                    statsRenderNanos += System.nanoTime() - renderStart
                    if (!EGL14.eglSwapBuffers(display, eglSurface)) {
                        val error = EGL14.eglGetError()
                        if (error == EGL14.EGL_BAD_SURFACE || error == EGL14.EGL_BAD_NATIVE_WINDOW) {
                            break
                        }
                        error("eglSwapBuffers failed: 0x${error.toString(16)}")
                    }
                    renderedFrameCount.incrementAndGet()
                }

                val statsElapsed = System.nanoTime() - statsWindowStart
                if (statsElapsed >= 1_000_000_000L) {
                    val threadCpuNanos = Debug.threadCpuTimeNanos()
                    val cpuPercent = ((threadCpuNanos - lastThreadCpuNanos) * 100.0 / statsElapsed)
                        .toFloat()
                    val fps = statsFrameCount * 1_000_000_000f / statsElapsed
                    val renderCpuPercent = cpuPercent.coerceAtLeast(0f)
                    statsListener(
                        RenderDebugInfo(
                            fps = fps,
                            frameTimeMs = if (statsFrameCount == 0) 0f
                            else statsRenderNanos / statsFrameCount / 1_000_000f,
                            renderCpuPercent = renderCpuPercent,
                            surfaceWidth = width,
                            surfaceHeight = height,
                            outputWidth = renderer.outputWidthForDebug,
                            outputHeight = renderer.outputHeightForDebug,
                            targetFps = state.targetFrameRate,
                            gpuRenderer = renderer.gpuRendererForDebug,
                        ),
                    )
                    diagnosticWindowCount++
                    if (diagnosticWindowCount >= DIAGNOSTIC_WINDOW_COUNT) {
                        logAod(
                            "heartbeat fps=${"%.1f".format(fps)} " +
                                "cpu=${"%.1f".format(renderCpuPercent)}% " +
                                "totalFrames=${renderedFrameCount.get()}",
                        )
                        diagnosticWindowCount = 0
                    }
                    statsWindowStart = System.nanoTime()
                    statsFrameCount = 0
                    statsRenderNanos = 0L
                    lastThreadCpuNanos = threadCpuNanos
                }
                val frameNanos = 1_000_000_000L / state.targetFrameRate
                nextFrame += frameNanos
                val remaining = nextFrame - System.nanoTime()
                if (remaining > 0L) {
                    try {
                        Thread.sleep(remaining / 1_000_000L, (remaining % 1_000_000L).toInt())
                    } catch (_: InterruptedException) {
                        // Re-check running before the next frame.
                    }
                } else if (remaining < -frameNanos * 2L) {
                    nextFrame = System.nanoTime()
                }

                val renderLimitNanos = maxContinuousRenderMillis?.times(1_000_000L)
                if (renderLimitNanos != null && frameStart - renderSessionStart >= renderLimitNanos) {
                    if (idle.compareAndSet(false, true)) {
                        logAod("idleAfter=${maxContinuousRenderMillis}ms totalFrames=${renderedFrameCount.get()}")
                    }
                }
            }
        } catch (throwable: Throwable) {
            if (running.get()) {
                Log.e(TAG, "EGL renderer stopped", throwable)
            }
        } finally {
            renderer?.close()
            if (display !== EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    display,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT,
                )
                if (eglSurface !== EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(display, eglSurface)
                }
                if (eglContext !== EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(display, eglContext)
                }
                EGL14.eglTerminate(display)
            }
            running.set(false)
        }
    }

    private fun awaitRenderingEnabled(): Boolean {
        var waited = false
        renderingLock.withLock {
            while (running.get() && (!renderingEnabled.get() || idle.get())) {
                if (!waited) {
                    waited = true
                    val reason = if (idle.get()) "idle" else "disabled"
                    logAod("parked reason=$reason totalFrames=${renderedFrameCount.get()}")
                }
                if (!renderingEnabled.get() && pauseCallbackPending.compareAndSet(true, false)) {
                    logAod("disabledWhileParked totalFrames=${renderedFrameCount.get()}")
                    onRenderingPaused()
                }
                try {
                    renderingCondition.await()
                } catch (_: InterruptedException) {
                    // Re-check both flags after lifecycle or shutdown changes.
                }
            }
        }
        if (waited && running.get()) {
            logAod("resumed totalFrames=${renderedFrameCount.get()}")
        }
        return waited
    }

    private fun logAod(message: String) {
        Log.i(AOD_TAG, "renderer=$rendererId $message")
    }

    private fun chooseConfig(display: EGLDisplay): EGLConfig {
        val attributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        check(EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0)) {
            "eglChooseConfig failed: 0x${EGL14.eglGetError().toString(16)}"
        }
        check(count[0] > 0) { "No GLES 3 window EGLConfig is available" }
        return checkNotNull(configs[0])
    }

    private companion object {
        const val TAG = "PearMesh"
        const val AOD_TAG = "PearWallAod"
        const val DIAGNOSTIC_WINDOW_COUNT = 5
        val nextRendererId = AtomicInteger(0)

        fun packSize(width: Int, height: Int): Long =
            (width.toLong() shl 32) or (height.toLong() and 0xffffffffL)

        fun unpackWidth(size: Long): Int = (size shr 32).toInt()
        fun unpackHeight(size: Long): Int = size.toInt()
    }
}
