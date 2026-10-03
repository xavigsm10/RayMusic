package com.mrtdk.liquid_glass.pearmesh.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLUtils
import androidx.core.graphics.createBitmap
import com.mrtdk.liquid_glass.pearmesh.AudioPowerFrame
import com.mrtdk.liquid_glass.pearmesh.MoruStyle
import com.mrtdk.liquid_glass.pearmesh.PearMeshState
import com.mrtdk.liquid_glass.pearmesh.RendererState
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

internal class PearMeshGlRenderer(
    context: Context,
    initialState: RendererState,
    private val frameCaptureRequest: AtomicReference<((Bitmap) -> Unit)?>? = null,
) : Closeable {
    private val rotationProgram = GlProgram(
        context,
        "pearmesh/shaders/rotation.vert",
        "pearmesh/shaders/rotation.frag",
    )
    private val blurProgram = GlProgram(
        context,
        "pearmesh/shaders/fullscreen.vert",
        "pearmesh/shaders/blur.frag",
    )
    private val fullscreenMaterialProgram = GlProgram(
        context,
        "pearmesh/shaders/fullscreen.vert",
        "pearmesh/shaders/material.frag",
    )
    private val pinchMaterialProgram = GlProgram(
        context,
        "pearmesh/shaders/pinch.vert",
        "pearmesh/shaders/material.frag",
    )
    private val moruProgram = GlProgram(
        context,
        "pearmesh/shaders/fullscreen.vert",
        "pearmesh/shaders/moru.frag",
    )
    private val moruTextures = mapOf(
        MoruStyle.NARROW to MoruTextures(
            uploadAssetTexture(context, "pearmesh/moru/moru_narrow.png"),
            uploadAssetTexture(context, "pearmesh/moru/depth_light_shadow_narrow.png"),
        ),
        MoruStyle.WIDE to MoruTextures(
            uploadAssetTexture(context, "pearmesh/moru/moru_wide.png"),
            uploadAssetTexture(context, "pearmesh/moru/depth_light_shadow_wide.png"),
        ),
        MoruStyle.SMOOTH to MoruTextures(
            uploadAssetTexture(context, "pearmesh/moru/moru_smooth.png"),
            uploadAssetTexture(context, "pearmesh/moru/depth_light_shadow_smooth.png"),
        ),
    )
    private val quad = GlGeometry.quad()
    private var mesh = GlGeometry.mesh(
        PearMeshMesh.create(
            isPortrait = true,
            presetIndex = PearMeshState.DefaultPortraitPresetIndex,
        ),
    )

    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var outputWidth = 0
    private var outputHeight = 0
    private var isPortrait = true
    private var meshPresetIndex = PearMeshState.DefaultPortraitPresetIndex
    private var rotationTarget: RenderTarget? = null
    private var kawaseHalfTarget: RenderTarget? = null
    private var kawaseQuarterTarget: RenderTarget? = null
    private var kawaseEighthTarget: RenderTarget? = null
    private var lyricsBlurTarget: RenderTarget? = null
    private var ordinaryBlurTarget: RenderTarget? = null
    private var materialTarget: RenderTarget? = null
    private var moruTarget: RenderTarget? = null

    val outputWidthForDebug: Int get() = outputWidth
    val outputHeightForDebug: Int get() = outputHeight
    val gpuRendererForDebug: String = GLES30.glGetString(GLES30.GL_RENDERER) ?: "Unknown"

    private val initialArtwork = initialState.artwork?.takeUnless { it.bitmap.isRecycled }
    private var currentArtwork = uploadTexture(
        initialArtwork?.bitmap ?: createGrayArtwork()
    )
    private var previousArtwork = currentArtwork
    private var uploadedArtworkId = initialArtwork?.id ?: 0L
    private var observedArtworkId = uploadedArtworkId
    private var pendingArtwork: Bitmap? = null
    private var pendingArtworkId = 0L
    private var artworkTransitionStart = Double.NEGATIVE_INFINITY
    private var artworkAspect = initialArtwork?.bitmap?.let { it.width.toFloat() / it.height } ?: 1f
    private var lastImageScales = floatArrayOf(1f, 1f, 1f)
    private var resumeImageScales = lastImageScales.copyOf()
    private var resumeVisualStartNanos = 0L
    private var wasPlaybackPlaying = true


    init {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glClearColor(0f, 0f, 0f, 1f)
    }

    private var currentScrimAlpha = PearMeshState.DefaultScrimAlpha

    fun render(width: Int, height: Int, state: RendererState, time: Double) {
        currentScrimAlpha = state.scrimAlpha
        ensureSize(width, height, state)
        // Artwork transitions must finish even when playback flow is intentionally frozen.
        val artworkTime = System.nanoTime() / 1_000_000_000.0
        updateArtwork(state, artworkTime)
        val transitionMix = artworkTransitionMix(artworkTime)
        val currentLyricsMix = state.behindLyricsProgress
        if (state.isPlaybackPlaying && !wasPlaybackPlaying) {
            resumeImageScales = lastImageScales.copyOf()
            resumeVisualStartNanos = System.nanoTime()
        }
        wasPlaybackPlaying = state.isPlaybackPlaying

        val imageScales = if (!state.isPlaybackPlaying && state.pauseFlowEnabled) {
            lastImageScales
        } else {
            val targetScales = imageScales(state, time)
            if (resumeVisualStartNanos != 0L) {
                val progress = ((System.nanoTime() - resumeVisualStartNanos).toFloat() /
                        VISUAL_RESUME_SECONDS_NANOS).coerceIn(0f, 1f)
                FloatArray(3) { index ->
                    lerp(resumeImageScales[index], targetScales[index], progress)
                }.also {
                    if (progress >= 1f) resumeVisualStartNanos = 0L
                }
            } else {
                targetScales
            }.also { lastImageScales = it }
        }
        val flowSpeedMultiplier = if (state.flowSpeed == PearMeshState.FastFlowSpeed) 2.0 else 1.0
        val blurSigma = lerp(
            ORDINARY_BLUR_SIGMA,
            LYRICS_BLUR_SIGMA,
            currentLyricsMix,
        ) * state.renderScale * state.blurMultiplier

        val needsOrdinary = currentLyricsMix < 1f
        val needsLyrics = currentLyricsMix > 0f
        val lyricTarget = checkNotNull(lyricsBlurTarget)
        val ordinaryTarget = checkNotNull(ordinaryBlurTarget)

        val lyricTexture: Int
        val ordinaryTexture: Int
        if (needsOrdinary && needsLyrics) {
            renderBackdrop(
                floatArrayOf(1f, 1f, 1f),
                blurSigma,
                state.blurEnabled,
                ordinaryTarget,
                time,
                transitionMix,
                flowSpeedMultiplier
            )
            renderBackdrop(
                imageScales,
                blurSigma,
                state.blurEnabled,
                lyricTarget,
                time,
                transitionMix,
                flowSpeedMultiplier
            )
            ordinaryTexture = ordinaryTarget.texture
            lyricTexture = lyricTarget.texture
        } else {
            renderBackdrop(
                if (needsLyrics) imageScales else floatArrayOf(1f, 1f, 1f),
                blurSigma,
                state.blurEnabled,
                lyricTarget,
                time,
                transitionMix,
                flowSpeedMultiplier,
            )
            lyricTexture = lyricTarget.texture
            ordinaryTexture = lyricTarget.texture
        }

        renderMaterial(lyricTexture, ordinaryTexture, currentLyricsMix, time, state.moruStyle)
        frameCaptureRequest?.getAndSet(null)?.let(::captureFrame)
    }

    /** Reads the presented frame from the default framebuffer and hands it to the callback. */
    private fun captureFrame(callback: (Bitmap) -> Unit) {
        val width = surfaceWidth
        val height = surfaceHeight
        if (width <= 0 || height <= 0) return
        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        val pixels = IntArray(width * height)
        for (row in 0 until height) {
            // GL rows are bottom-up; flip while decoding.
            buffer.position((height - 1 - row) * width * 4)
            val base = row * width
            for (column in 0 until width) {
                val r = buffer.get().toInt() and 0xFF
                val g = buffer.get().toInt() and 0xFF
                val b = buffer.get().toInt() and 0xFF
                val a = buffer.get().toInt() and 0xFF
                pixels[base + column] = Color.argb(a, r, g, b)
            }
        }
        val bitmap = createBitmap(width, height)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        callback(bitmap)
    }

    private fun renderBackdrop(
        imageScales: FloatArray,
        blurSigma: Float,
        blurEnabled: Boolean,
        target: RenderTarget,
        time: Double,
        transitionMix: Float,
        flowSpeedMultiplier: Double,
    ) {
        val rotation = checkNotNull(rotationTarget)
        val half = checkNotNull(kawaseHalfTarget)
        val quarter = checkNotNull(kawaseQuarterTarget)
        val eighth = checkNotNull(kawaseEighthTarget)
        rotation.bind()
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        rotationProgram.use()
        rotationProgram.float("uTime", (time * flowSpeedMultiplier).toFloat())
        val aspect = outputWidth.toFloat() / outputHeight
        if (aspect >= 1f) {
            rotationProgram.vec2("uViewScale", 1f, aspect)
        } else {
            rotationProgram.vec2("uViewScale", 1f / aspect, 1f)
        }
        rotationProgram.vec3("uImageScales", imageScales[0], imageScales[1], imageScales[2])
        rotationProgram.float("uTransitionMix", transitionMix)
        rotationProgram.int("uCurrentArtwork", 0)
        rotationProgram.int("uPreviousArtwork", 1)
        bindTexture(0, currentArtwork)
        bindTexture(1, previousArtwork)

        rotationProgram.int("uArtworkFill", 1)
        rotationProgram.int("uInstance", 0)
        quad.draw()
        rotationProgram.int("uArtworkFill", 0)
        repeat(3) { instance ->
            rotationProgram.int("uInstance", instance)
            quad.draw()
        }

        if (!blurEnabled) {
            kawasePass(rotation, target, 0f, upsample = false)
            return
        }

        val kawaseOffset = blurSigma / KAWASE_SIGMA_PER_OFFSET
        kawasePass(rotation, half, kawaseOffset, upsample = false)
        kawasePass(half, quarter, kawaseOffset, upsample = false)
        kawasePass(quarter, eighth, kawaseOffset, upsample = false)
        kawasePass(eighth, quarter, kawaseOffset, upsample = true)
        kawasePass(quarter, half, kawaseOffset, upsample = true)
        kawasePass(half, target, kawaseOffset, upsample = true)
    }

    private fun kawasePass(
        source: RenderTarget,
        target: RenderTarget,
        offset: Float,
        upsample: Boolean,
    ) {
        target.bind()
        blurProgram.use()
        blurProgram.int("uSource", 0)
        blurProgram.vec2("uTexelSize", 1f / source.width, 1f / source.height)
        blurProgram.float("uOffset", offset)
        blurProgram.int("uUpsample", if (upsample) 1 else 0)
        bindTexture(0, source.texture)
        quad.draw()
    }

    private fun renderMaterial(
        lyricTexture: Int,
        ordinaryTexture: Int,
        modeMix: Float,
        time: Double,
        moruStyle: MoruStyle,
    ) {
        val destination = if (moruStyle != MoruStyle.OFF) {
            checkNotNull(moruTarget)
        } else {
            materialTarget
        }
        if (destination == null) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, surfaceWidth, surfaceHeight)
        } else {
            destination.bind()
        }
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        bindTexture(0, lyricTexture)
        bindTexture(1, ordinaryTexture)

        when {
            modeMix <= 0f -> drawFullscreenMaterial(MATERIAL_ORDINARY, modeMix, moruStyle)
            modeMix >= 1f -> {
                if (isPortrait) {
                    drawFullscreenMaterial(MATERIAL_LYRICS, modeMix, moruStyle)
                }
                drawPinchMaterial(MATERIAL_LYRICS, modeMix, time, moruStyle)
            }

            else -> {
                drawFullscreenMaterial(
                    if (isPortrait) MATERIAL_COMPOSITE else MATERIAL_LANDSCAPE_BACKGROUND,
                    modeMix,
                    moruStyle,
                )
                drawPinchMaterial(MATERIAL_COMPOSITE, modeMix, time, moruStyle)
            }
        }

        if (moruStyle != MoruStyle.OFF) {
            renderMoru(moruTarget!!.texture, moruStyle, time)
        } else if (destination != null) {
            blitToScreen(destination)
        }
    }

    private fun drawFullscreenMaterial(mode: Int, modeMix: Float, moruStyle: MoruStyle) {
        setMaterialUniforms(fullscreenMaterialProgram, mode, modeMix, moruStyle)
        quad.draw()
    }

    private fun drawPinchMaterial(mode: Int, modeMix: Float, time: Double, moruStyle: MoruStyle) {
        setMaterialUniforms(pinchMaterialProgram, mode, modeMix, moruStyle)
        pinchMaterialProgram.float("uTime", time.toFloat())
        if (isPortrait) {
            pinchMaterialProgram.vec4("uTextureTransform", 1f, 1f, 0f, 0f)
        } else {
            pinchMaterialProgram.vec4("uTextureTransform", 0.8f, 0.8f, 0.1f, 0.1f)
        }
        mesh.draw()
    }

    private fun setMaterialUniforms(
        program: GlProgram,
        mode: Int,
        modeMix: Float,
        moruStyle: MoruStyle,
    ) {
        program.use()
        program.int("uLyricsBackdrop", 0)
        program.int("uOrdinaryBackdrop", 1)
        val scrimAlpha = if (moruStyle == MoruStyle.OFF) {
            currentScrimAlpha
        } else {
            currentScrimAlpha * 0.5f
        }
        program.float("uBlackScrimAlpha", scrimAlpha)
        program.float("uLyricsModeMix", modeMix)
        program.float("uDitherStrength", 1f)
        program.int("uMaterialMode", mode)
    }

    private fun renderMoru(source: Int, style: MoruStyle, time: Double) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, surfaceWidth, surfaceHeight)
        moruProgram.use()
        moruProgram.int("uSource", 0)
        moruProgram.int("uNormal", 1)
        moruProgram.int("uLight", 2)
        moruProgram.int("uStyle", style.ordinal)
        val screenAspect = surfaceWidth.toFloat() / surfaceHeight.coerceAtLeast(1)
        // The source is already a screen-space FBO. Its artwork crop/MVP has
        // already been applied by the rotation and mesh passes.
        val mvpScaleX = 1f
        val normalScaleX = when (style) {
            MoruStyle.NARROW -> 0.15f
            MoruStyle.WIDE -> 0.31f
            MoruStyle.SMOOTH -> 0.24f
            MoruStyle.OFF -> 1f
        }
        moruProgram.float("uAspect", 1f / artworkAspect)
        moruProgram.float("uNormalScale", mvpScaleX / normalScaleX)
        moruProgram.float(
            "uIor", when (style) {
                MoruStyle.NARROW -> 0.68f
                MoruStyle.WIDE -> 0.58f
                MoruStyle.SMOOTH -> 0.60f
                MoruStyle.OFF -> 1f
            }
        )
        moruProgram.float("uSurfaceRatio", minOf(screenAspect, 1f / screenAspect))
        moruProgram.float(
            "uDisplacement", when (style) {
                MoruStyle.NARROW -> 0.36f
                MoruStyle.WIDE -> 0.58f
                MoruStyle.SMOOTH -> 0.37f
                MoruStyle.OFF -> 0f
            }
        )
        moruProgram.float(
            "uThickness", when (style) {
                MoruStyle.NARROW -> 0.30f
                MoruStyle.WIDE -> 0.36f
                MoruStyle.SMOOTH -> 0.06f
                MoruStyle.OFF -> 0f
            }
        )
        moruProgram.float("uDarkness", if (style == MoruStyle.WIDE) 0.10f else 0f)
        moruProgram.float("uLightness", if (style == MoruStyle.WIDE) 0.65f else 0.40f)
        moruProgram.float("uShadowness", if (style == MoruStyle.WIDE) 0.36f else 1f)
        bindTexture(0, source)
        val textures = moruTextures.getValue(style)
        bindTexture(1, textures.normal)
        bindTexture(2, textures.light)
        quad.draw()
    }

    private fun blitToScreen(source: RenderTarget) {
        GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, source.framebuffer)
        GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0)
        GLES30.glBlitFramebuffer(
            0,
            0,
            source.width,
            source.height,
            0,
            0,
            surfaceWidth,
            surfaceHeight,
            GLES30.GL_COLOR_BUFFER_BIT,
            GLES30.GL_LINEAR
        )
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun ensureSize(width: Int, height: Int, state: RendererState) {
        val newPortrait = height >= width
        val newPresetIndex = if (newPortrait) {
            state.portraitPresetIndex
        } else {
            state.landscapePresetIndex
        }
        if (newPortrait != isPortrait || newPresetIndex != meshPresetIndex) {
            isPortrait = newPortrait
            meshPresetIndex = newPresetIndex
            mesh.close()
            mesh = GlGeometry.mesh(
                PearMeshMesh.create(
                    isPortrait = isPortrait,
                    presetIndex = meshPresetIndex,
                ),
            )
        }

        val scaledWidth = max(1, (width * state.renderScale).roundToInt())
        val scaledHeight = max(1, (height * state.renderScale).roundToInt())
        if (
            width == surfaceWidth &&
            height == surfaceHeight &&
            scaledWidth == outputWidth &&
            scaledHeight == outputHeight
        ) return
        surfaceWidth = width
        surfaceHeight = height
        outputWidth = scaledWidth
        outputHeight = scaledHeight

        rotationTarget?.close()
        kawaseHalfTarget?.close()
        kawaseQuarterTarget?.close()
        kawaseEighthTarget?.close()
        lyricsBlurTarget?.close()
        ordinaryBlurTarget?.close()
        materialTarget?.close()
        moruTarget?.close()
        val backdropWidth = max(1, floor(outputWidth / BLUR_DOWNSAMPLE).toInt())
        val backdropHeight = max(1, floor(outputHeight / BLUR_DOWNSAMPLE).toInt())
        rotationTarget = RenderTarget.create(backdropWidth, backdropHeight)
        kawaseHalfTarget = RenderTarget.create(
            max(1, backdropWidth / 2),
            max(1, backdropHeight / 2),
        )
        kawaseQuarterTarget = RenderTarget.create(
            max(1, backdropWidth / 4),
            max(1, backdropHeight / 4),
        )
        kawaseEighthTarget = RenderTarget.create(
            max(1, backdropWidth / 8),
            max(1, backdropHeight / 8),
        )
        lyricsBlurTarget = RenderTarget.create(backdropWidth, backdropHeight)
        ordinaryBlurTarget = RenderTarget.create(backdropWidth, backdropHeight)
        materialTarget = if (outputWidth == width && outputHeight == height) {
            null
        } else {
            RenderTarget.create(outputWidth, outputHeight)
        }
        moruTarget = RenderTarget.create(outputWidth, outputHeight)
    }

    private fun updateArtwork(state: RendererState, time: Double) {
        val artwork = state.artwork ?: return
        if (artwork.id == observedArtworkId || artwork.bitmap.isRecycled) return
        observedArtworkId = artwork.id

        if (artworkTransitionStart.isFinite()) {
            pendingArtwork = artwork.bitmap
            pendingArtworkId = artwork.id
        } else {
            startArtworkTransition(artwork.bitmap, artwork.id, time)
        }
    }

    private fun startArtworkTransition(bitmap: Bitmap, id: Long, time: Double) {
        if (bitmap.isRecycled) return
        artworkAspect = bitmap.width.toFloat() / bitmap.height
        previousArtwork = currentArtwork
        currentArtwork = uploadTexture(bitmap)
        uploadedArtworkId = id
        artworkTransitionStart = time
    }

    private fun artworkTransitionMix(time: Double): Float {
        if (!artworkTransitionStart.isFinite()) return 1f
        val progress = ((time - artworkTransitionStart) / ARTWORK_TRANSITION_SECONDS).toFloat()
        if (progress < 1f) return progress.coerceIn(0f, 1f)

        if (previousArtwork != currentArtwork) {
            GLES30.glDeleteTextures(1, intArrayOf(previousArtwork), 0)
            previousArtwork = currentArtwork
        }
        artworkTransitionStart = Double.NEGATIVE_INFINITY

        val nextArtwork = pendingArtwork
        val nextArtworkId = pendingArtworkId
        pendingArtwork = null
        pendingArtworkId = 0L
        if (nextArtwork != null && nextArtworkId != uploadedArtworkId && !nextArtwork.isRecycled) {
            startArtworkTransition(nextArtwork, nextArtworkId, time)
            return 0f
        }
        return 1f
    }

    private fun imageScales(state: RendererState, time: Double): FloatArray {
        val demoPower = if (state.isDemoPulseEnabled) {
            val kick = max(0.0, sin(time * PI * 2.0 * 1.15)).pow(12.0)
            val body = max(0.0, sin(time * PI * 2.0 * 0.575 + 0.7)).pow(8.0)
            (kick * 0.86 + body * 0.14).toFloat()
        } else {
            -1f
        }
        val nowNanos = System.nanoTime()
        val audioFrame = state.audioFrame
        val audioCurrent = state.audioVisualizationEnabled &&
                audioFrame.currentUpdatedAtNanos != 0L &&
                nowNanos - audioFrame.currentUpdatedAtNanos <= AUDIO_REPORT_TIMEOUT_NANOS
        val lanePower = if (demoPower >= 0f) {
            FloatArray(3) { demoPower }
        } else if (audioCurrent) {
            interpolatedAudioPower(audioFrame, nowNanos)
        } else {
            FloatArray(3)
        }
        return FloatArray(3) { index ->
            1f + IMAGE_PULSE_INTENSITY * lanePower[index] * lanePower[index]
        }
    }

    private fun interpolatedAudioPower(frame: AudioPowerFrame, nowNanos: Long): FloatArray {
        val reportNanos = frame.currentUpdatedAtNanos - frame.previousUpdatedAtNanos
        if (frame.previousUpdatedAtNanos == 0L || reportNanos <= 0L) {
            return FloatArray(3) { frame.currentPower[it].coerceIn(0f, 1f) }
        }

        // Delay the sample by one report interval so both interpolation endpoints exist.
        val sampleNanos = nowNanos - reportNanos
        val mix = ((sampleNanos - frame.previousUpdatedAtNanos).toDouble() / reportNanos)
            .toFloat()
            .coerceIn(0f, 1f)
        return FloatArray(3) { index ->
            lerp(frame.previousPower[index], frame.currentPower[index], mix)
                .coerceIn(0f, 1f)
        }
    }

    private fun uploadTexture(bitmap: Bitmap): Int {
        val handle = IntArray(1)
        GLES30.glGenTextures(1, handle, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, handle[0])
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR_MIPMAP_LINEAR,
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        return handle[0]
    }

    private fun bindTexture(unit: Int, texture: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
    }

    override fun close() {
        rotationTarget?.close()
        kawaseHalfTarget?.close()
        kawaseQuarterTarget?.close()
        kawaseEighthTarget?.close()
        lyricsBlurTarget?.close()
        ordinaryBlurTarget?.close()
        materialTarget?.close()
        quad.close()
        mesh.close()
        rotationProgram.close()
        blurProgram.close()
        fullscreenMaterialProgram.close()
        pinchMaterialProgram.close()
        moruProgram.close()
        moruTarget?.close()
        val textures = if (currentArtwork == previousArtwork) {
            intArrayOf(currentArtwork)
        } else {
            intArrayOf(currentArtwork, previousArtwork)
        }
        GLES30.glDeleteTextures(textures.size, textures, 0)
        val moruHandles = moruTextures.values.flatMap { listOf(it.normal, it.light) }.toIntArray()
        GLES30.glDeleteTextures(moruHandles.size, moruHandles, 0)
    }

    private data class MoruTextures(val normal: Int, val light: Int)

    private fun uploadAssetTexture(context: Context, path: String): Int {
        val bitmap = context.assets.open(path).use { BitmapFactory.decodeStream(it) }
            ?: error("Unable to decode $path")
        val handle = IntArray(1)
        GLES30.glGenTextures(1, handle, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, handle[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        return handle[0]
    }

    private companion object {
        const val VISUAL_RESUME_SECONDS_NANOS = 180_000_000f
        const val BLUR_DOWNSAMPLE = 4f
        const val KAWASE_SIGMA_PER_OFFSET = 16f
        const val LYRICS_BLUR_SIGMA = 42.5f
        const val ORDINARY_BLUR_SIGMA = 80f
        const val ARTWORK_TRANSITION_SECONDS = 0.5
        const val IMAGE_PULSE_INTENSITY = 0.33f
        const val AUDIO_REPORT_TIMEOUT_NANOS = 250_000_000L
        const val MATERIAL_ORDINARY = 0
        const val MATERIAL_LYRICS = 1
        const val MATERIAL_COMPOSITE = 2
        const val MATERIAL_LANDSCAPE_BACKGROUND = 3
        fun lerp(from: Float, to: Float, amount: Float): Float =
            from + (to - from) * amount

        fun createGrayArtwork(): Bitmap =
            createBitmap(1, 1).apply {
                eraseColor(Color.GRAY)
            }
    }
}
