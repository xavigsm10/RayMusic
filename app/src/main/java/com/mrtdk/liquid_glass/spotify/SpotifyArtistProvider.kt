package com.mrtdk.liquid_glass.spotify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Artist image provider backed exclusively by official Apple Music studio portraits.
 */
object SpotifyArtistProvider {
    val artistImageCache = ConcurrentHashMap<String, String>()

    fun isYouTubeUrl(url: String?): Boolean {
        if (url == null) return false
        return url.contains("googleusercontent.com") ||
               url.contains("ytimg.com") ||
               url.contains("ggpht.com")
    }

    fun cleanArtistName(rawName: String): String {
        return AppleMusicArtistProvider.cleanArtistName(rawName)
    }

    /**
     * Synchronous 0ms RAM & disk cache lookup.
     * Returns the official Apple Music artist image URL immediately on the initial Compose frame.
     */
    fun getCachedArtistImageUrl(artistName: String): String? {
        val cleanName = cleanArtistName(artistName)
        if (cleanName.isBlank()) return null

        val appleMusicImg = AppleMusicArtistProvider.getCachedImageUrl(cleanName)
        if (!appleMusicImg.isNullOrBlank()) {
            artistImageCache[cleanName.lowercase().trim()] = appleMusicImg
            return appleMusicImg
        }

        val cacheKey = cleanName.lowercase().trim()
        artistImageCache[cacheKey]?.let { return it }

        return null
    }

    /**
     * Resolves the official Apple Music artist portrait image asynchronously.
     */
    suspend fun getArtistImageUrl(artistName: String): String? = withContext(Dispatchers.IO) {
        val cleanName = cleanArtistName(artistName)
        if (cleanName.isBlank()) return@withContext null

        val cacheKey = cleanName.lowercase().trim()
        
        getCachedArtistImageUrl(cleanName)?.let { return@withContext it }

        // Fetch official Apple Music artist image
        val appleMusicImg = AppleMusicArtistProvider.getArtistImageUrl(cleanName)
        if (!appleMusicImg.isNullOrBlank()) {
            artistImageCache[cacheKey] = appleMusicImg
            return@withContext appleMusicImg
        }

        null
    }
}

@Composable
fun SpotifyArtistAvatar(
    artistName: String,
    fallbackUrl: String?,
    contentDescription: String? = "Artist",
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var artistThumbUrl by remember(artistName) {
        mutableStateOf(SpotifyArtistProvider.getCachedArtistImageUrl(artistName))
    }

    LaunchedEffect(artistName) {
        if (artistName.isNotBlank()) {
            if (artistThumbUrl == null) {
                val url = SpotifyArtistProvider.getArtistImageUrl(artistName)
                if (!url.isNullOrBlank()) {
                    artistThumbUrl = url
                }
            }
            AppleMusicArtistProvider.prefetch(artistName)
        }
    }

    // Never display YouTube Music photo for artists
    val isYouTube = SpotifyArtistProvider.isYouTubeUrl(fallbackUrl)
    val effectiveFallback = if (isYouTube) null else fallbackUrl
    val displayUrl = artistThumbUrl ?: effectiveFallback

    if (displayUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(displayUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        // Dark placeholder while loading to avoid any flash of YouTube images
        Box(
            modifier = modifier.background(Color(0xFF202024))
        )
    }
}
