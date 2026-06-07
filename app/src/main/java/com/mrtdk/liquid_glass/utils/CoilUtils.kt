package com.mrtdk.liquid_glass.utils

import coil.intercept.Interceptor
import coil.request.ImageResult

object CoilUtils {
    fun upgradeThumbQuality(url: String?): String? {
        if (url == null) return null
        
        // Don't upgrade local assets
        if (url.startsWith("file:///android_asset/")) {
            return url
        }
        
        return when {
            // Apple Music CDN: upgrade to 1000x1000bb.jpg
            url.contains("mzstatic.com") -> {
                url.replace(Regex("/\\d+x\\d+bb\\.jpg$"), "/1000x1000bb.jpg")
                   .replace(Regex("/\\d+x\\d+bb\\.webp$"), "/1000x1000bb.jpg")
                   .replace(Regex("/\\d+x\\d+sr\\.jpg$"), "/1000x1000bb.jpg")
                   .replace(Regex("/\\d+x\\d+sr\\.webp$"), "/1000x1000bb.jpg")
                   .replace(Regex("/\\d+x\\d+bb\\-\\d+\\.jpg$"), "/1000x1000bb.jpg")
            }
            // YouTube Music: upgrade to =w1000-h1000-l90-rj
            url.contains("lh3.googleusercontent.com") -> {
                if (url.contains("=w") || url.contains("=s")) {
                    url.replace(Regex("=w\\d+-h\\d+.*$"), "=w1000-h1000-l90-rj")
                       .replace(Regex("=s\\d+.*$"), "=s1000")
                } else {
                    url + "=w1000-h1000-l90-rj"
                }
            }
            url.contains("yt3.ggpht.com") -> {
                if (url.contains("=w") || url.contains("=s")) {
                    url.replace(Regex("=w\\d+-h\\d+.*$"), "=w1000-h1000-l90-rj")
                       .replace(Regex("=s\\d+$"), "=s1000")
                } else {
                    url + "=w1000-h1000-l90-rj"
                }
            }
            // YouTube video thumbnails: upgrade to maxresdefault.jpg
            url.contains("ytimg.com/vi/") -> {
                url.replace("/default.jpg", "/maxresdefault.jpg")
                   .replace("/hqdefault.jpg", "/maxresdefault.jpg")
                   .replace("/mqdefault.jpg", "/maxresdefault.jpg")
                   .replace("/sddefault.jpg", "/maxresdefault.jpg")
            }
            // General =w or =s query params
            url.contains("=w") || url.contains("=s") -> {
                val index = url.indexOf("=w").takeIf { it != -1 } ?: url.indexOf("=s")
                if (index != -1) {
                    url.substring(0, index) + "=w1000-h1000-l90-rj"
                } else {
                    url
                }
            }
            else -> url
        }
    }

    class HdThumbnailInterceptor : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val request = chain.request
            val data = request.data
            
            // Skip resizing if explicit size limit or low-res placeholder is requested
            // (e.g. size=200 or size=100 in dominant color sampling requests to avoid huge bitmaps)
            val isSampler = request.allowHardware == false
            if (isSampler) {
                return chain.proceed(request)
            }

            val url = when (data) {
                is String -> data
                is android.net.Uri -> data.toString()
                else -> null
            }
            if (url != null) {
                val hdUrl = upgradeThumbQuality(url)
                if (hdUrl != url) {
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
