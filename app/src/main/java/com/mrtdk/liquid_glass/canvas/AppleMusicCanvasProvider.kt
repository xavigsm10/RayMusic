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

object AppleMusicCanvasProvider {
    private const val AMP_BASE_URL = "https://amp-api.music.apple.com"

    private val client get() = CanvasNetworkClient.okHttpClient

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    suspend fun getByAlbumArtist(
        album: String,
        artist: String,
        storefront: String = "us",
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        val key = cacheKey("album", album, artist, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withContext it.value }

        val result = searchAndFetchMotion(album, artist, album, storefront, "albums")
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        result
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
        storefront: String = "us",
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        val key = cacheKey("song", song, artist, album ?: "", storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withContext it.value }

        val result = searchAndFetchMotion(song, artist, album, storefront, "songs")
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        result
    }

    private fun searchAndFetchMotion(
        term: String,
        artist: String,
        album: String?,
        storefront: String,
        type: String,
    ): CanvasArtwork? {
        return try {
            var query = if (term.contains(artist, ignoreCase = true)) term else "$artist $term"
            if (!album.isNullOrBlank() && !query.contains(album, ignoreCase = true)) {
                query = "$query $album"
            }
            val token = kotlinx.coroutines.runBlocking { AppleMusicTokenProvider.getToken() }
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/search".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("term", query)
                ?.addQueryParameter("types", type)
                ?.addQueryParameter("limit", "10")
                ?.addQueryParameter("extend", "editorialVideo")
                ?.addQueryParameter("include", "albums")
                ?.build() ?: return null

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Origin", "https://music.apple.com")
                .header("Referer", "https://music.apple.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val jsonStr = response.body?.string().orEmpty()
            val root = JSONObject(jsonStr)
            val results = root.optJSONObject("results")?.optJSONObject(type)?.optJSONArray("data") ?: return null

            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val attributes = obj.optJSONObject("attributes") ?: continue
                val editorialVideo = attributes.optJSONObject("editorialVideo") ?: continue

                val videoUrl = extractEditorialVideoUrl(editorialVideo)
                if (!videoUrl.isNullOrBlank()) {
                    val name = attributes.optString("name")
                    val artistName = attributes.optString("artistName")
                    return CanvasArtwork(
                        name = name,
                        artist = artistName,
                        videoUrl = videoUrl,
                        animated = videoUrl
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractEditorialVideoUrl(editorialData: JSONObject): String? {
        val preferredKeys = listOf("motionDetailRaw", "motionDetailTall", "motionDetailSquare", "motionTallVideo3x4", "motionSquareVideo1x1")
        for (key in preferredKeys) {
            val obj = editorialData.optJSONObject(key)
            val videoUrl = obj?.optString("video")
            if (!videoUrl.isNullOrBlank()) return videoUrl
        }
        val keys = editorialData.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = editorialData.optJSONObject(key)
            val videoUrl = obj?.optString("video")
            if (!videoUrl.isNullOrBlank()) return videoUrl
        }
        return null
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}
