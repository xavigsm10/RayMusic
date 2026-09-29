package com.mrtdk.liquid_glass.spotify

import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides official Apple Music artist typography wordmarks / logos
 * extracted directly from the Apple Music catalog / web artist header.
 */
object AppleMusicLogoProvider {
    private val logoMemoryCache = ConcurrentHashMap<String, String>()
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<String?>>()
    private val providerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private const val NONE_SENTINEL = "NONE"

    private val LOGO_REGEX = Regex(""""artistLogo"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)"""")

    fun cleanArtistName(rawName: String): String {
        return rawName.trim()
            .removePrefix("Álbum • ")
            .removePrefix("Album • ")
            .removePrefix("Artista • ")
            .removePrefix("Artist • ")
    }

    private fun normalizeKey(name: String): String {
        return cleanArtistName(name)
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    /**
     * Synchronous 0ms RAM & disk cache lookup.
     * Returns the logo URL if present, or null if not yet cached or if the artist has no official logo.
     */
    fun getCachedLogoUrl(artistName: String): String? {
        val clean = cleanArtistName(artistName)
        if (clean.isBlank()) return null

        val cacheKey = normalizeKey(clean)
        if (cacheKey.isEmpty()) return null

        // 1. RAM Cache check
        val memValue = logoMemoryCache[cacheKey]
        if (memValue != null) {
            return if (memValue == NONE_SENTINEL) null else memValue
        }

        // 2. Disk Cache check
        val saved = LibraryManager.getString("am_logo_$cacheKey")
        if (!saved.isNullOrBlank()) {
            logoMemoryCache[cacheKey] = saved
            return if (saved == NONE_SENTINEL) null else saved
        }

        // Check primary artist for collaborations
        val primary = clean.split(",", "&", " feat.", " ft.", " / ", ";").firstOrNull()?.trim()
        if (!primary.isNullOrBlank() && !primary.equals(clean, ignoreCase = true)) {
            val primaryKey = normalizeKey(primary)
            val primaryMem = logoMemoryCache[primaryKey]
            if (primaryMem != null) {
                return if (primaryMem == NONE_SENTINEL) null else primaryMem
            }
            val primarySaved = LibraryManager.getString("am_logo_$primaryKey")
            if (!primarySaved.isNullOrBlank()) {
                logoMemoryCache[primaryKey] = primarySaved
                return if (primarySaved == NONE_SENTINEL) null else primarySaved
            }
        }

        return null
    }

    /**
     * Asynchronously resolves the official Apple Music artist typography logo.
     * Deduplicates in-flight requests and permanently caches both hits and misses.
     */
    suspend fun getArtistLogoUrl(artistName: String): String? = withContext(Dispatchers.IO) {
        val clean = cleanArtistName(artistName)
        if (clean.isBlank()) return@withContext null

        val cacheKey = normalizeKey(clean)
        if (cacheKey.isEmpty()) return@withContext null

        // Instant cache check
        val memValue = logoMemoryCache[cacheKey]
        if (memValue != null) {
            return@withContext if (memValue == NONE_SENTINEL) null else memValue
        }
        val saved = LibraryManager.getString("am_logo_$cacheKey")
        if (!saved.isNullOrBlank()) {
            logoMemoryCache[cacheKey] = saved
            return@withContext if (saved == NONE_SENTINEL) null else saved
        }

        // In-flight deduplication
        val deferred = inFlightRequests.computeIfAbsent(cacheKey) {
            providerScope.async {
                fetchLogoFromAppleMusic(clean)
            }
        }

        val result = try {
            deferred.await()
        } finally {
            inFlightRequests.remove(cacheKey)
        }

        val toStore = result ?: NONE_SENTINEL
        logoMemoryCache[cacheKey] = toStore
        LibraryManager.saveString("am_logo_$cacheKey", toStore)

        result
    }

    /**
     * Non-blocking background prefetch for when artists appear in carousels/lists.
     */
    fun prefetch(artistName: String) {
        val clean = cleanArtistName(artistName)
        if (clean.isBlank()) return
        val cacheKey = normalizeKey(clean)
        if (cacheKey.isEmpty() || logoMemoryCache.containsKey(cacheKey)) return

        providerScope.launch {
            getArtistLogoUrl(clean)
        }
    }

    private fun fetchLogoFromAppleMusic(artistName: String): String? {
        try {
            // 1. Search iTunes for the official Apple Music Artist Page URL
            val itunesUrl = "https://itunes.apple.com/search?term=${URLEncoder.encode(artistName, "UTF-8")}&entity=musicArtist&limit=1"
            val connSearch = (URL(itunesUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", "RayMusic/1.0 (Android)")
            }

            if (connSearch.responseCode != 200) {
                connSearch.disconnect()
                return null
            }

            val searchJson = connSearch.inputStream.bufferedReader().use { it.readText() }
            connSearch.disconnect()

            val rootJson = JSONObject(searchJson)
            val results = rootJson.optJSONArray("results") ?: return null
            if (results.length() == 0) return null

            val artistObj = results.getJSONObject(0)
            val artistLinkUrl = artistObj.optString("artistLinkUrl")
            if (artistLinkUrl.isNullOrBlank()) return null

            // 2. Stream-read the Apple Music artist page HTML and break early as soon as logo is found
            val pageConn = (URL(artistLinkUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            }

            if (pageConn.responseCode != 200) {
                pageConn.disconnect()
                return null
            }

            val reader = pageConn.inputStream.bufferedReader()
            val sb = java.lang.StringBuilder(32768)
            val buffer = CharArray(4096)
            var bytesRead: Int
            var rawTemplateUrl: String? = null

            while (reader.read(buffer).also { bytesRead = it } != -1) {
                sb.append(buffer, 0, bytesRead)
                val match = LOGO_REGEX.find(sb)
                if (match != null) {
                    rawTemplateUrl = match.groupValues[1]
                    break
                }
                // Stop early if header section explicitly has null artistLogo
                if (sb.contains("\"artistLogo\":null")) {
                    break
                }
                // Cap window to avoid downloading rest of huge artist pages
                if (sb.length > 120_000) {
                    break
                }
            }
            reader.close()
            pageConn.disconnect()

            if (rawTemplateUrl.isNullOrBlank()) return null

            // Format template URL into high-resolution transparent PNG (1000x500.png)
            return rawTemplateUrl
                .replace("{w}x{h}{c}.{f}", "1000x500.png")
                .replace("{w}x{h}.{f}", "1000x500.png")
        } catch (_: Exception) {
            return null
        }
    }
}
