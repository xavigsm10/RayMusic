package com.mrtdk.liquid_glass.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ArtistVideoCanvasProvider {
    private const val BASE_URL = "https://artwork-archivetune.koiiverse.cloud/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client get() = CanvasNetworkClient.okHttpClient

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val TTL_MS = 60_000L

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
        duration: Int? = null,
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        val key = cacheKey("sa", song, artist, album.orEmpty(), duration?.toString().orEmpty())
        cache[key]?.let { entry ->
            if (entry.expiresAtMs > System.currentTimeMillis()) return@withContext entry.value
            cache.remove(key)
        }

        val urlBuilder = BASE_URL.toHttpUrlOrNull()?.newBuilder() ?: return@withContext null
        urlBuilder.addQueryParameter("s", song)
        urlBuilder.addQueryParameter("a", artist)
        if (!album.isNullOrBlank()) urlBuilder.addQueryParameter("al", album)
        if (duration != null && duration > 0) urlBuilder.addQueryParameter("d", duration.toString())

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val value = try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                json.decodeFromString<CanvasArtwork>(body)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        cache[key] = CacheEntry(
            value = value,
            expiresAtMs = System.currentTimeMillis() + TTL_MS,
        )

        value
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        val normalized = parts
            .map { it.trim().lowercase(Locale.ROOT) }
            .joinToString("|")
        return "$prefix|$normalized"
    }
}
