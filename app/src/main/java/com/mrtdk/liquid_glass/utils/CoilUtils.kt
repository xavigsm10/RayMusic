package com.mrtdk.liquid_glass.utils

import coil.intercept.Interceptor
import coil.request.ImageResult
import android.util.Log

object CoilUtils {
    fun upgradeThumbQuality(url: String?): String? {
        if (url == null) return null
        if (url.startsWith("file:///android_asset/")) {
            return url
        }
        
        val upgraded = when {
            url.contains("mzstatic.com") -> {
                url.replace(Regex("/\\d+x\\d+bb\\.[a-zA-Z0-9]+$"), "/1000x1000bb.jpg")
                   .replace(Regex("/\\d+x\\d+sr\\.[a-zA-Z0-9]+$"), "/1000x1000bb.jpg")
                   .replace(Regex("/\\d+x\\d+bb\\-\\d+\\.[a-zA-Z0-9]+$"), "/1000x1000bb.jpg")
                   .replace(Regex("/\\d+x\\d+\\.[a-zA-Z0-9]+$"), "/1000x1000bb.jpg")
            }
           
            url.contains("yt3.ggpht.com") -> {
                val baseUrl = url.split("=")[0].split("-s")[0]
                "$baseUrl=s1200"
            }
           
            url.contains("googleusercontent.com") || url.contains("ggpht.com") -> {
                if (url.contains(Regex("=[ws]\\d+"))) {
                    url.replace(Regex("=[ws]\\d+.*$"), "=w1200-h1200-l90-rj")
                } else if (url.contains(Regex("-[ws]\\d+"))) {
                    url.replace(Regex("-[ws]\\d+.*$"), "-w1200-h1200")
                } else if (url.contains(Regex("/[ws]\\d+"))) {
                    url.replace(Regex("/[ws]\\d+.*$"), "/s1200")
                } else {
                    val index = url.indexOf("=w").takeIf { it != -1 } ?: url.indexOf("=s")
                    if (index != -1) {
                        url.substring(0, index) + "=w1200-h1200-l90-rj"
                    } else {
                        val clean = url.substringBefore("?")
                        clean + "=w1200-h1200-l90-rj"
                    }
                }
            }
           
            url.contains("ytimg.com/vi") -> {
                url.replace("/default.jpg", "/maxresdefault.jpg")
                   .replace("/hqdefault.jpg", "/maxresdefault.jpg")
                   .replace("/mqdefault.jpg", "/maxresdefault.jpg")
                   .replace("/sddefault.jpg", "/maxresdefault.jpg")
                   .replace("/default.webp", "/maxresdefault.jpg")
                   .replace("/hqdefault.webp", "/maxresdefault.jpg")
                   .replace("/mqdefault.webp", "/maxresdefault.jpg")
                   .replace("/sddefault.webp", "/maxresdefault.jpg")
            }
            
            url.contains("=w") || url.contains("=s") -> {
                val index = url.indexOf("=w").takeIf { it != -1 } ?: url.indexOf("=s")
                if (index != -1) {
                    url.substring(0, index) + "=w1200-h1200-l90-rj"
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
