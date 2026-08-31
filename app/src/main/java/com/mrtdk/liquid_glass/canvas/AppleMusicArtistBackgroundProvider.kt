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

/**
 * Fetches Apple Music artist motion artwork (HLS / MP4 canvas) for ArtistScreen.
 */
object AppleMusicArtistBackgroundProvider {
    private const val AMP_BASE_URL = "https://amp-api.music.apple.com"

    private val client get() = CanvasNetworkClient.okHttpClient

    private data class CacheEntry(
        val videoUrl: String?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    suspend fun getByArtistName(
        artistName: String,
        storefront: String = "us",
    ): String? = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext null
        val key = cacheKey("artist", artistName, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withContext it.videoUrl }

        val result = searchAndFetchArtistMotion(artistName, storefront)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        result
    }

    private suspend fun searchAndFetchArtistMotion(
        artistName: String,
        storefront: String,
    ): String? {
        return try {
            val token = AppleMusicTokenProvider.getToken()
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/search".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("term", artistName)
                ?.addQueryParameter("types", "artists")
                ?.addQueryParameter("limit", "3")
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
            val results = root.optJSONObject("results")?.optJSONObject("artists")?.optJSONArray("data") ?: return null

            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val attributes = obj.optJSONObject("attributes") ?: continue
                val resultName = attributes.optString("name")
                val artistId = obj.optString("id")

                if (artistId.isNotBlank() && (resultName.contains(artistName, ignoreCase = true) || artistName.contains(resultName, ignoreCase = true))) {
                    val fetched = fetchArtistMotionByAppleId(artistId, storefront, token)
                    if (!fetched.isNullOrBlank()) {
                        return fetched
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchArtistMotionByAppleId(
        artistId: String,
        storefront: String,
        token: String
    ): String? {
        return try {
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/artists/$artistId".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("extend", "editorialVideo,editorialArtwork")
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
            val data = root.optJSONArray("data") ?: return null
            val artistObj = data.optJSONObject(0) ?: return null
            val attributes = artistObj.optJSONObject("attributes") ?: return null

            // 1. editorialVideo
            val ev = attributes.optJSONObject("editorialVideo")
            if (ev != null) {
                val videoUrl = extractEditorialVideoUrl(ev)
                if (!videoUrl.isNullOrBlank()) return videoUrl
            }

            // 2. editorialArtwork
            val ea = attributes.optJSONObject("editorialArtwork")
            if (ea != null) {
                val videoUrl = extractEditorialVideoUrl(ea)
                if (!videoUrl.isNullOrBlank()) return videoUrl
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
