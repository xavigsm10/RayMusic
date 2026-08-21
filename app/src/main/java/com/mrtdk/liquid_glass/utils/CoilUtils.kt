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

    fun upgradeThumbQuality(url: String?): String? {
        if (url == null) return null
        if (url.startsWith("file:///android_asset/")) {
            return url
        }
        
        val upgraded = when {
            url.contains("mzstatic.com") -> {
                url.replace(REGEX_MZSTATIC_1, "/500x500bb.jpg")
                   .replace(REGEX_MZSTATIC_2, "/500x500bb.jpg")
                   .replace(REGEX_MZSTATIC_3, "/500x500bb.jpg")
                   .replace(REGEX_MZSTATIC_4, "/500x500bb.jpg")
            }
           
            url.contains("yt3.ggpht.com") -> {
                val baseUrl = url.split("=")[0].split("-s")[0]
                "$baseUrl=s540"
            }
           
            url.contains("googleusercontent.com") || url.contains("ggpht.com") -> {
                if (url.contains(REGEX_WS_1)) {
                    url.replace(REGEX_WS_1_REPLACE, "=w540-h540-l90-rj")
                } else if (url.contains(REGEX_WS_2)) {
                    url.replace(REGEX_WS_2_REPLACE, "-w540-h540")
                } else if (url.contains(REGEX_WS_3)) {
                    url.replace(REGEX_WS_3_REPLACE, "/s540")
                } else {
                    val index = url.indexOf("=w").takeIf { it != -1 } ?: url.indexOf("=s")
                    if (index != -1) {
                        url.substring(0, index) + "=w540-h540-l90-rj"
                    } else {
                        val clean = url.substringBefore("?")
                        clean + "=w540-h540-l90-rj"
                    }
                }
            }
           
            url.contains("ytimg.com/vi") -> {
                url.replace("/default.jpg", "/hqdefault.jpg")
                   .replace("/default.webp", "/hqdefault.jpg")
                   .replace("/sddefault.jpg", "/hqdefault.jpg")
                   .replace("/sddefault.webp", "/hqdefault.jpg")
            }
             
            url.contains("=w") || url.contains("=s") -> {
                val index = url.indexOf("=w").takeIf { it != -1 } ?: url.indexOf("=s")
                if (index != -1) {
                    url.substring(0, index) + "=w540-h540-l90-rj"
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
                val hdUrl = upgradeThumbQuality(url)
                if (hdUrl != url) {
                    Log.d("CoilUtils", "Upgraded URL: $url -> $hdUrl")
                    val newRequest = request.newBuilder()
                        .data(hdUrl)
                        .build()
                    return chain.proceed(newRequest)
                } else {
                    Log.d("CoilUtils", "No upgrade needed for URL: $url")
                }
            }
            return chain.proceed(request)
        }
    }
}
