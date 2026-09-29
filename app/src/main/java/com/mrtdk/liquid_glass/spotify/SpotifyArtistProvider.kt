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
import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object SpotifyArtistProvider {
    val artistImageCache = ConcurrentHashMap<String, String>()
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<String?>>()
    private val providerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun isYouTubeUrl(url: String?): Boolean {
        if (url == null) return false
        return url.contains("googleusercontent.com") ||
               url.contains("ytimg.com") ||
               url.contains("ggpht.com")
    }

    fun cleanArtistName(rawName: String): String {
        return rawName.trim()
            .removePrefix("Álbum • ")
            .removePrefix("Album • ")
            .removePrefix("Artista • ")
            .removePrefix("Artist • ")
    }

    /**
     * Synchronous 0ms RAM & disk cache lookup.
     * Returns immediately on the initial Compose frame.
     */
    fun getCachedArtistImageUrl(artistName: String): String? {
        val cleanName = cleanArtistName(artistName)
        if (cleanName.isBlank()) return null

        val cacheKey = cleanName.lowercase().trim()
        artistImageCache[cacheKey]?.let { return it }

        val savedUrl = LibraryManager.getString("spotify_artist_img_$cacheKey")
        if (!savedUrl.isNullOrBlank()) {
            artistImageCache[cacheKey] = savedUrl
            return savedUrl
        }

        // Also check primary artist if collaboration
        val primaryName = cleanName.split(",", "&", " feat.", " ft.", " / ", ";").firstOrNull()?.trim()
        if (!primaryName.isNullOrBlank() && !primaryName.equals(cleanName, ignoreCase = true)) {
            val primaryKey = primaryName.lowercase().trim()
            artistImageCache[primaryKey]?.let { return it }
            val primarySavedUrl = LibraryManager.getString("spotify_artist_img_$primaryKey")
            if (!primarySavedUrl.isNullOrBlank()) {
                artistImageCache[primaryKey] = primarySavedUrl
                return primarySavedUrl
            }
        }

        return null
    }

    suspend fun getArtistImageUrl(artistName: String): String? = withContext(Dispatchers.IO) {
        val cleanName = cleanArtistName(artistName)
        if (cleanName.isBlank()) return@withContext null

        val cacheKey = cleanName.lowercase().trim()
        
        // 1. Instant cache check
        getCachedArtistImageUrl(cleanName)?.let { return@withContext it }

        // 2. In-flight request deduplication (prevents redundant cellular data & network spam)
        val deferred = inFlightRequests.computeIfAbsent(cacheKey) {
            providerScope.async {
                fetchOnlineWithFallbacks(cleanName)
            }
        }

        val resultUrl = try {
            deferred.await()
        } finally {
            inFlightRequests.remove(cacheKey)
        }

        if (!resultUrl.isNullOrBlank()) {
            artistImageCache[cacheKey] = resultUrl
            LibraryManager.saveString("spotify_artist_img_$cacheKey", resultUrl)
            return@withContext resultUrl
        }

        null
    }

    private suspend fun fetchOnlineWithFallbacks(cleanName: String): String? {
        // Direct query
        var resultUrl = fetchOnline(cleanName)
        if (!resultUrl.isNullOrBlank()) return resultUrl

        // Primary artist fallback if multiple artists are in the string (e.g. "Michael Jackson, Justin Bieber")
        val primaryName = cleanName.split(",", "&", " feat.", " ft.", " / ", ";").firstOrNull()?.trim()
        if (!primaryName.isNullOrBlank() && !primaryName.equals(cleanName, ignoreCase = true)) {
            resultUrl = fetchOnline(primaryName)
            if (!resultUrl.isNullOrBlank()) return resultUrl
        }

        return null
    }

    private suspend fun fetchOnline(query: String): String? {
        // Step 1: Ultra-fast public studio portrait API (~60-100ms, official 1000x1000px artwork, zero token overhead)
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val conn = URL("https://api.deezer.com/search/artist?q=$encoded&limit=1").openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val data = json.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val artObj = data.getJSONObject(0)
                    val pic = artObj.optString("picture_xl", "").ifEmpty {
                        artObj.optString("picture_big", "").ifEmpty {
                            artObj.optString("picture_medium", "")
                        }
                    }
                    if (pic.isNotBlank()) {
                        return pic
                    }
                }
            }
        } catch (_: Exception) {}

        // Step 2: Spotify Web API (if session already active or fallback)
        try {
            if (SpotifySession.accessToken.isNotBlank() || SpotifySession.ensureValidToken()) {
                val imageUrl = Spotify.searchArtistImage(query).getOrNull()
                if (!imageUrl.isNullOrBlank()) {
                    return imageUrl
                }
            }
        } catch (_: Exception) {}

        return null
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
    var spotifyUrl by remember(artistName) {
        mutableStateOf(SpotifyArtistProvider.getCachedArtistImageUrl(artistName))
    }

    LaunchedEffect(artistName) {
        if (artistName.isNotBlank()) {
            if (spotifyUrl == null) {
                val url = SpotifyArtistProvider.getArtistImageUrl(artistName)
                if (!url.isNullOrBlank()) {
                    spotifyUrl = url
                }
            }
            AppleMusicLogoProvider.prefetch(artistName)
        }
    }

    // Never display YouTube Music photo for artists
    val isYouTube = SpotifyArtistProvider.isYouTubeUrl(fallbackUrl)
    val effectiveFallback = if (isYouTube) null else fallbackUrl
    val displayUrl = spotifyUrl ?: effectiveFallback

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
