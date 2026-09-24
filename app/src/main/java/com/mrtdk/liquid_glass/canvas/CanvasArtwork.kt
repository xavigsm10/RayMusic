package com.mrtdk.liquid_glass.canvas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CanvasArtwork(
    val name: String? = null,
    val artist: String? = null,
    @SerialName("albumId")
    val albumId: String? = null,
    val albumName: String? = null,
    val static: String? = null,
    val animated: String? = null,
    val videoUrl: String? = null,
) {
    val preferredAnimationUrl: String?
        get() {
            val candidate = animated?.takeIf { it.isNotBlank() } ?: videoUrl?.takeIf { it.isNotBlank() }
            return candidate?.takeIf { isValidVideoUrl(it) }
        }

    companion object {
        fun isValidVideoUrl(url: String): Boolean {
            if (url.isBlank()) return false
            val clean = url.substringBefore('?').lowercase()
            // Reject known static image formats
            val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".tiff", ".svg", ".ico")
            if (imageExtensions.any { clean.endsWith(it) }) return false

            // Accept video extensions and known video streaming paths
            val isKnownVideoExtension = clean.endsWith(".mp4") || clean.endsWith(".m3u8") ||
                    clean.endsWith(".mov") || clean.endsWith(".webm") || clean.endsWith(".ts")
            val isKnownVideoPath = clean.contains("/videos/") || clean.contains("editorialvideo") ||
                    clean.contains("video_") || clean.contains("/video/") || clean.contains(".m3u8")
            return isKnownVideoExtension || isKnownVideoPath
        }
    }
}
