package com.mrtdk.liquid_glass.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object TidalCanvasProvider {
    private const val BASE_URL = "https://api.tidal.com/v1/"
    private const val TIDAL_TOKEN = "vNVdglQOjFJJGG2U"

    private val client get() = CanvasNetworkClient.okHttpClient

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long
    )

    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    private val countryCode by lazy {
        val country = Locale.getDefault().country
        if (country.length == 2) country.uppercase(Locale.ROOT) else "US"
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        val key = cacheKey("search_song", song, artist, album ?: "")
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withContext it.value }

        val query = if (!album.isNullOrBlank()) "$album $artist $song" else "$artist $song"

        val result = searchOnTidal(
            query = query,
            types = "TRACKS",
            songValidation = song,
            artistValidation = artist
        )
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        result
    }

    suspend fun getByAlbumArtist(
        album: String,
        artist: String
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        val key = cacheKey("search_album", album, artist)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withContext it.value }

        val result = searchOnTidal(
            query = "$album $artist",
            types = "ALBUMS",
            songValidation = null,
            artistValidation = artist,
            albumValidation = album
        )
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        result
    }

    private fun searchOnTidal(
        query: String,
        types: String,
        songValidation: String? = null,
        artistValidation: String? = null,
        albumValidation: String? = null
    ): CanvasArtwork? {
        return try {
            val url = "${BASE_URL}search".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("query", query)
                ?.addQueryParameter("limit", "10")
                ?.addQueryParameter("types", types)
                ?.addQueryParameter("countryCode", countryCode)
                ?.build() ?: return null

            val request = Request.Builder()
                .url(url)
                .header("X-Tidal-Token", TIDAL_TOKEN)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val jsonStr = response.body?.string().orEmpty()
            val root = JSONObject(jsonStr)
            val sectionKey = types.lowercase(Locale.ROOT) // "tracks" or "albums"
            val section = root.optJSONObject(sectionKey) ?: return null
            val items = section.optJSONArray("items") ?: return null

            for (i in 0 until items.length()) {
                val obj = items.optJSONObject(i) ?: continue
                val title = obj.optString("title")

                if (songValidation != null && title.isNotBlank() && !title.contains(songValidation, ignoreCase = true) && !songValidation.contains(title, ignoreCase = true)) {
                    continue
                }
                if (albumValidation != null && title.isNotBlank() && !title.contains(albumValidation, ignoreCase = true) && !albumValidation.contains(title, ignoreCase = true)) {
                    continue
                }

                // Check for video cover (videoCover or mediaMetadata)
                val videoCover = obj.optString("videoCover").takeIf { it.isNotBlank() }
                if (videoCover != null) {
                    val formattedVideoUrl = formatTidalVideoUrl(videoCover)
                    if (formattedVideoUrl != null) {
                        return CanvasArtwork(
                            name = title,
                            artist = artistValidation,
                            videoUrl = formattedVideoUrl,
                            animated = formattedVideoUrl
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun formatTidalVideoUrl(videoCover: String): String? {
        val clean = videoCover.replace("-", "/")
        return "https://resources.tidal.com/videos/$clean/1280x1280.mp4"
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}
