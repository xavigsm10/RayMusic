package com.mrtdk.liquid_glass.utils

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import coil.size.Size
import coil.transform.Transformation

/**
 * A Coil [Transformation] that applies an intense blur effect to an image.
 * Uses downscaling (sampling) combined with RenderScript's ScriptIntrinsicBlur.
 * Downscaling by a factor (e.g. 4x or 8x) allows achieving an extremely intense blur
 * (equivalent to a 80px-100px radius) while keeping the actual blur radius within
 * RenderScript's maximum limit of 25f. This also significantly improves performance
 * and reduces memory usage.
 */
class BlurTransformation(
    private val context: Context,
    private val radius: Float = 25f,
    private val sampling: Float = 4f
) : Transformation {

    override val cacheKey: String = "BlurTransformation(radius=$radius, sampling=$sampling)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Downscale the bitmap to reduce memory footprint and increase blur effect intensity
        val width = (input.width / sampling).toInt().coerceAtLeast(1)
        val height = (input.height / sampling).toInt().coerceAtLeast(1)

        val scaledBitmap = Bitmap.createScaledBitmap(input, width, height, false)
        val blurredBitmap = Bitmap.createBitmap(scaledBitmap)

        var rs: RenderScript? = null
        var inputAllocation: Allocation? = null
        var outputAllocation: Allocation? = null
        var blurScript: ScriptIntrinsicBlur? = null

        try {
            rs = RenderScript.create(context)
            rs.messageHandler = RenderScript.RSMessageHandler()
            inputAllocation = Allocation.createFromBitmap(
                rs,
                scaledBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            outputAllocation = Allocation.createTyped(rs, inputAllocation.type)
            blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

            blurScript.setInput(inputAllocation)
            blurScript.setRadius(radius.coerceIn(0f, 25f))
            blurScript.forEach(outputAllocation)
            outputAllocation.copyTo(blurredBitmap)
        } catch (e: Exception) {
            // Fallback: If RenderScript fails for any reason, return the downscaled image.
            // When scaled up by the ImageView with scaleType="centerCrop", it will still look semi-blurred.
            return scaledBitmap
        } finally {
            inputAllocation?.destroy()
            outputAllocation?.destroy()
            blurScript?.destroy()
            rs?.destroy()
        }

        if (scaledBitmap != blurredBitmap) {
            scaledBitmap.recycle()
        }
        
        return blurredBitmap
    }
}
