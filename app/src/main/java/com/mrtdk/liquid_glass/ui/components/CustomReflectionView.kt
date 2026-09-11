package com.mrtdk.liquid_glass.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * A custom view that projects a detailed 1D color stretch from the bottom edge
 * of an album cover, warps it horizontally with an animated liquid mesh (sine waves),
 * and applies a massive hardware-accelerated blur (RenderEffect on Android 12+).
 */
class CustomReflectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true // Enable bilinear filtering for smooth scaling
    }
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private var cachedH = 0f
    private var bottomEdgeBitmap: Bitmap? = null
    private var vertices = FloatArray(0)
    private val bgExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    // Wave animation phase for liquid movement
    private var wavePhase = 0f
    private val waveAnimator = ValueAnimator.ofFloat(0f, 2f * Math.PI.toFloat()).apply {
        duration = 5000 // Loop every 5 seconds
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animator ->
            wavePhase = animator.animatedValue as Float
            invalidate()
        }
    }

    init {
        waveAnimator.start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!waveAnimator.isRunning) {
            waveAnimator.start()
        }
    }

    override fun onDetachedFromWindow() {
        waveAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            if (!waveAnimator.isRunning) waveAnimator.start()
        } else {
            if (waveAnimator.isRunning) waveAnimator.pause()
        }
    }

    /**
     * Extracts a thin band from the bottom edge of the album cover, rescales it
     * horizontally to a smaller size to smooth out noise, and triggers redraw.
     */
    fun setAlbumArt(bitmap: Bitmap) {
        bgExecutor.execute {
            try {
                val width = bitmap.width
                val height = bitmap.height
                
                // Grab the bottom 2% of the cover image (typically 10-20 pixels)
                val sampleHeight = (height * 0.02f).toInt().coerceIn(5, 25)
                val srcRect = Rect(0, height - sampleHeight, width, height)
                
                val sliceWidth = 128
                val sliceHeight = 8
                val sliceBmp = Bitmap.createBitmap(sliceWidth, sliceHeight, Bitmap.Config.ARGB_8888)
                val tempCanvas = Canvas(sliceBmp)
                
                val destRect = Rect(0, 0, sliceWidth, sliceHeight)
                val tempPaint = Paint(Paint.FILTER_BITMAP_FLAG)
                tempCanvas.drawBitmap(bitmap, srcRect, destRect, tempPaint)

                post {
                    val oldBmp = bottomEdgeBitmap
                    bottomEdgeBitmap = sliceBmp
                    oldBmp?.recycle()
                    setupBlurEffect()
                    invalidate()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Hardware-accelerated blur effect for Android 12+ adapted to device performance tier.
     */
    private fun setupBlurEffect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val perfConfig = com.mrtdk.liquid_glass.utils.PerformanceProfileManager.getConfig()
            val blurRadius = perfConfig.reflectionBlurRadius
            val blurEffect = RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                Shader.TileMode.MIRROR
            )
            setRenderEffect(blurEffect)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0f || h <= 0f) return

        val slice = bottomEdgeBitmap ?: return

        // 1. Save Layer for alpha blending (PorterDuff mask)
        val saveCount = canvas.saveLayer(0f, 0f, w, h, null)

        // 2. Generate liquid mesh grid vertices with pre-allocated zero-allocation buffer
        val perfConfig = com.mrtdk.liquid_glass.utils.PerformanceProfileManager.getConfig()
        val meshWidth = perfConfig.reflectionMeshSize
        val meshHeight = perfConfig.reflectionMeshSize
        val count = (meshWidth + 1) * (meshHeight + 1)
        val neededSize = count * 2
        if (vertices.size != neededSize) {
            vertices = FloatArray(neededSize)
        }

        var index = 0
        for (y in 0..meshHeight) {
            val fy = y.toFloat() / meshHeight
            val py = fy * h
            
            // Sine-wave horizontal offset that intensifies in the middle/lower part
            val waveOffset = (Math.sin((fy.toDouble() * 3.0) + wavePhase).toFloat() * 35f * (1f - fy)) +
                             (Math.cos((fy.toDouble() * 1.5) - wavePhase).toFloat() * 15f * (1f - fy))
            
            for (x in 0..meshWidth) {
                val fx = x.toFloat() / meshWidth
                val px = fx * w + waveOffset
                
                vertices[index++] = px
                vertices[index++] = py
            }
        }

        // 3. Draw the distorted bitmap using drawBitmapMesh
        canvas.drawBitmapMesh(slice, meshWidth, meshHeight, vertices, 0, null, 0, paint)

        // 4. Draw vertical gradient mask to fade to transparent at the bottom (cached Shader)
        if (cachedH != h) {
            cachedH = h
            maskPaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                Color.WHITE,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, maskPaint)

        canvas.restoreToCount(saveCount)
    }
}
