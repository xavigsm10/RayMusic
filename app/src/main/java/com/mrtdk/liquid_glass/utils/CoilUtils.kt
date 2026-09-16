package com.mrtdk.liquid_glass.utils

import coil.intercept.Interceptor
import coil.request.ImageResult
import android.util.Log

object CoilUtils {
    private val REGEX_MZSTATIC_1 = Regex("/\\d+x\\d+bb\\.[a-zA-Z0-9]+$")
    private val REGEX_MZSTATIC_2 = Regex("/\\d+x\\d+sr\\.[a-zA-Z0-9]+$")
    private val REGEX_MZSTATIC_3 = Regex("/\\d+x\\d+bb\\-\\d+\\.[a-zA-Z0-9]+$")
    private val REGEX_MZSTATIC_4 = Regex("/\\d+x\\d+\\.[a-zA-Z0-9]+$")
    private val REGEX_WS_1 = Regex("=[ws]\\d+")
    private val REGEX_WS_1_REPLACE = Regex("=[ws]\\d+.*$")
    private val REGEX_WS_2 = Regex("-[ws]\\d+")
    private val REGEX_WS_2_REPLACE = Regex("-[ws]\\d+.*$")
    private val REGEX_WS_3 = Regex("/[ws]\\d+")
    private val REGEX_WS_3_REPLACE = Regex("/[ws]\\d+.*$")

    fun upgradeThumbQuality(url: String?, targetDimension: Int = 540): String? {
        if (url == null) return null
        if (url.startsWith("file:///android_asset/")) {
            return url
        }
        val targetSize = targetDimension.coerceIn(160, 800)
        val mzTarget = if (targetSize <= 240) "250x250bb.jpg" else "500x500bb.jpg"
        
        val upgraded = when {
            url.contains("mzstatic.com") -> {
                url.replace(REGEX_MZSTATIC_1, "/$mzTarget")
                   .replace(REGEX_MZSTATIC_2, "/$mzTarget")
                   .replace(REGEX_MZSTATIC_3, "/$mzTarget")
                   .replace(REGEX_MZSTATIC_4, "/$mzTarget")
            }
           
            url.contains("yt3.ggpht.com") -> {
                val baseUrl = url.split("=")[0].split("-s")[0]
                "$baseUrl=s$targetSize"
            }
           
            url.contains("googleusercontent.com") || url.contains("ggpht.com") -> {
                if (url.contains(REGEX_WS_1)) {
                    url.replace(REGEX_WS_1_REPLACE, "=w$targetSize-h$targetSize-l90-rj")
                } else if (url.contains(REGEX_WS_2)) {
                    url.replace(REGEX_WS_2_REPLACE, "-w$targetSize-h$targetSize")
                } else if (url.contains(REGEX_WS_3)) {
                    url.replace(REGEX_WS_3_REPLACE, "/s$targetSize")
                } else {
                    val index = url.indexOf("=w").takeIf { it != -1 } ?: url.indexOf("=s")
                    if (index != -1) {
                        url.substring(0, index) + "=w$targetSize-h$targetSize-l90-rj"
                    } else {
                        val clean = url.substringBefore("?")
                        clean + "=w$targetSize-h$targetSize-l90-rj"
                    }
                }
            }
           
            url.contains("ytimg.com/vi") -> {
                if (targetSize <= 240) {
                    url.replace("/default.jpg", "/mqdefault.jpg")
                       .replace("/default.webp", "/mqdefault.jpg")
                       .replace("/maxresdefault.jpg", "/mqdefault.jpg")
                       .replace("/hqdefault.jpg", "/mqdefault.jpg")
                } else {
                    url.replace("/default.jpg", "/hqdefault.jpg")
                       .replace("/default.webp", "/hqdefault.jpg")
                       .replace("/sddefault.jpg", "/hqdefault.jpg")
                       .replace("/sddefault.webp", "/hqdefault.jpg")
                }
            }
             
            url.contains("=w") || url.contains("=s") -> {
                val index = url.indexOf("=w").takeIf { it != -1 } ?: url.indexOf("=s")
                if (index != -1) {
                    url.substring(0, index) + "=w$targetSize-h$targetSize-l90-rj"
                } else {
                    url
                }
            }
            else -> url
        }
        
        return upgraded
    }

    class HdThumbnailInterceptor : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val request = chain.request
            val data = request.data

            val url = when (data) {
                is String -> data
                is android.net.Uri -> data.toString()
                else -> null
            }
            if (url != null) {
                val size = chain.size
                val targetDim = when (val w = size.width) {
                    is coil.size.Dimension.Pixels -> {
                        when {
                            w.px <= 180 -> 200
                            w.px <= 320 -> 360
                            else -> 540
                        }
                    }
                    else -> 540
                }
                val hdUrl = upgradeThumbQuality(url, targetDim)
                if (hdUrl != null && hdUrl != url) {
                    val newRequest = request.newBuilder()
                        .data(hdUrl)
                        .build()
                    return chain.proceed(newRequest)
                }
            }
            return chain.proceed(request)
        }
    }
}
