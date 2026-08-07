package com.mrtdk.liquid_glass.spotify

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object SpotifyArtistProvider {
    private val artistImageCache = ConcurrentHashMap<String, String>()

    suspend fun getArtistImageUrl(artistName: String): String? = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext null

        val cacheKey = artistName.lowercase().trim()
        artistImageCache[cacheKey]?.let { return@withContext it }

        // Check local saved preferences
        val savedUrl = LibraryManager.getString("spotify_artist_img_$cacheKey")
        if (!savedUrl.isNullOrBlank()) {
            artistImageCache[cacheKey] = savedUrl
            return@withContext savedUrl
        }

        // Ensure we have a valid token (session token or anonymous token)
        if (!SpotifySession.ensureValidToken()) return@withContext null

        try {
            val imageUrl = Spotify.searchArtistImage(artistName).getOrNull()
            if (!imageUrl.isNullOrBlank()) {
                artistImageCache[cacheKey] = imageUrl
                LibraryManager.saveString("spotify_artist_img_$cacheKey", imageUrl)
                return@withContext imageUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
    var spotifyUrl by remember(artistName) { mutableStateOf<String?>(null) }

    LaunchedEffect(artistName) {
        if (artistName.isNotBlank()) {
            val url = SpotifyArtistProvider.getArtistImageUrl(artistName)
            if (!url.isNullOrBlank()) {
                spotifyUrl = url
            }
        }
    }

    val displayUrl = spotifyUrl ?: fallbackUrl

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(displayUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}
