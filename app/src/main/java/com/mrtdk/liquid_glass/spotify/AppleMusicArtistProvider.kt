package com.mrtdk.liquid_glass.spotify

import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class AppleMusicArtistData(
    val imageUrl: String?,
    val logoUrl: String?,
    val bgColorHex: String?,
    val infoBgColorHex: String?
)

/**
 * Unified provider for official Apple Music artist assets:
 * - Studio portrait images (1000x1000)
 * - Official typography wordmarks / logos (transparent PNG)
 * - Curated mobile background colors (for dynamic video & hero)
 * - Curated mobile info / about modal colors
 */
object AppleMusicArtistProvider {
    private val memoryCache = ConcurrentHashMap<String, AppleMusicArtistData>()
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<AppleMusicArtistData?>>()
    private val providerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private const val NONE_SENTINEL = "NONE"
    private const val CACHE_PREFIX = "am_v4_"

    private val LOGO_REGEX = Regex(""""artistLogo"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)"""")
    private val CIRCLE_IMG_REGEX = Regex(""""circleArtwork"\s*:\s*\{"dictionary":\{[^}]*"url"\s*:\s*"([^"]+)"""")
    private val FALLBACK_IMG_REGEX = Regex(""""fallbackArtwork"\s*:\s*\{"dictionary":\{[^}]*"url"\s*:\s*"([^"]+)"""")
    private val AMI_IDENTITY_REGEX = Regex(""""url"\s*:\s*"(https://[^"]*ami-identity[^"]*)"""")

    private val VIDEO_BG_REGEX = Regex(""""videoArtwork"\s*:\s*\{"dictionary":\{[^}]*"bgColor"\s*:\s*"([0-9a-fA-F]{6})"""")
    private val COLOR_BACKDROP_REGEX = Regex(""""colorBackdropArtwork"\s*:\s*\{"dictionary":\{[^}]*"bgColor"\s*:\s*"([0-9a-fA-F]{6})"""")
    private val CIRCLE_BG_REGEX = Regex(""""circleArtwork"\s*:\s*\{"dictionary":\{[^}]*"bgColor"\s*:\s*"([0-9a-fA-F]{6})"""")
    private val LOGO_BG_REGEX = Regex(""""artistLogo"\s*:\s*\{"dictionary":\{[^}]*"bgColor"\s*:\s*"([0-9a-fA-F]{6})"""")

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
     * Synchronous 0ms RAM & disk cache lookup for all artist data.
     */
    fun getCachedArtistData(artistName: String): AppleMusicArtistData? {
        val clean = cleanArtistName(artistName)
        if (clean.isBlank()) return null
        val cacheKey = normalizeKey(clean)
        if (cacheKey.isEmpty()) return null

        // 1. RAM Cache
        memoryCache[cacheKey]?.let { return it }

        // 2. Disk Cache
        val savedImg = LibraryManager.getString("${CACHE_PREFIX}img_$cacheKey")
        val savedLogo = LibraryManager.getString("${CACHE_PREFIX}logo_$cacheKey")
        val savedBg = LibraryManager.getString("${CACHE_PREFIX}bg_$cacheKey")
        val savedInfoBg = LibraryManager.getString("${CACHE_PREFIX}infobg_$cacheKey")

        if (savedImg != null || savedLogo != null) {
            val data = AppleMusicArtistData(
                imageUrl = if (savedImg == NONE_SENTINEL) null else savedImg,
                logoUrl = if (savedLogo == NONE_SENTINEL) null else savedLogo,
                bgColorHex = savedBg?.takeIf { it.isNotBlank() },
                infoBgColorHex = savedInfoBg?.takeIf { it.isNotBlank() }
            )
            memoryCache[cacheKey] = data
            return data
        }

        // Primary artist fallback for collabs
        val primary = clean.split(",", "&", " feat.", " ft.", " / ", ";").firstOrNull()?.trim()
        if (!primary.isNullOrBlank() && !primary.equals(clean, ignoreCase = true)) {
            val primaryKey = normalizeKey(primary)
            memoryCache[primaryKey]?.let { return it }
            val pImg = LibraryManager.getString("${CACHE_PREFIX}img_$primaryKey")
            val pLogo = LibraryManager.getString("${CACHE_PREFIX}logo_$primaryKey")
            val pBg = LibraryManager.getString("${CACHE_PREFIX}bg_$primaryKey")
            val pInfoBg = LibraryManager.getString("${CACHE_PREFIX}infobg_$primaryKey")
            if (pImg != null || pLogo != null) {
                val data = AppleMusicArtistData(
                    imageUrl = if (pImg == NONE_SENTINEL) null else pImg,
                    logoUrl = if (pLogo == NONE_SENTINEL) null else pLogo,
                    bgColorHex = pBg?.takeIf { it.isNotBlank() },
                    infoBgColorHex = pInfoBg?.takeIf { it.isNotBlank() }
                )
                memoryCache[primaryKey] = data
                return data
            }
        }

        return null
    }

    fun getCachedImageUrl(artistName: String): String? {
        return getCachedArtistData(artistName)?.imageUrl
    }

    fun getCachedLogoUrl(artistName: String): String? {
        return getCachedArtistData(artistName)?.logoUrl
    }

    fun getCachedBgColor(artistName: String): String? {
        val clean = cleanArtistName(artistName)
        val lower = clean.lowercase()
        when {
            lower.contains("michael jackson") -> return "#1D120C" // Apple Music mobile espresso
            lower.contains("djo") -> return "#0B1729"             // Apple Music mobile midnight navy
            lower.contains("billie eilish") -> return "#0C1E2E"
            lower.contains("taylor swift") -> return "#070706"
            lower.contains("rosalia") || lower.contains("rosalía") -> return "#2B130D"
            lower.contains("coldplay") -> return "#0090D5"
            lower.contains("the beatles") -> return "#211A23"
        }
        return getCachedArtistData(artistName)?.bgColorHex
    }

    fun getCachedInfoBgColor(artistName: String): String? {
        val clean = cleanArtistName(artistName)
        val lower = clean.lowercase()
        when {
            lower.contains("michael jackson") -> return "#222B38" // Apple Music mobile slate indigo
            lower.contains("djo") -> return "#3E2F23"             // Apple Music mobile warm earth amber
        }
        return getCachedArtistData(artistName)?.infoBgColorHex
    }

    /**
     * Non-blocking background prefetch for lists and carousels.
     */
    fun prefetch(artistName: String) {
        val clean = cleanArtistName(artistName)
        if (clean.isBlank()) return
        val cacheKey = normalizeKey(clean)
        if (cacheKey.isEmpty() || memoryCache.containsKey(cacheKey)) return

        providerScope.launch {
            getArtistData(clean)
        }
    }

    /**
     * Full asynchronous resolution with in-flight deduplication.
     */
    suspend fun getArtistData(artistName: String): AppleMusicArtistData? = withContext(Dispatchers.IO) {
        val clean = cleanArtistName(artistName)
        if (clean.isBlank()) return@withContext null
        val cacheKey = normalizeKey(clean)
        if (cacheKey.isEmpty()) return@withContext null

        getCachedArtistData(clean)?.let { return@withContext it }

        val deferred = inFlightRequests.computeIfAbsent(cacheKey) {
            providerScope.async {
                fetchOnline(clean)
            }
        }

        val result = try {
            deferred.await()
        } finally {
            inFlightRequests.remove(cacheKey)
        }

        val toStore = result ?: AppleMusicArtistData(null, null, null, null)
        memoryCache[cacheKey] = toStore
        LibraryManager.saveString("${CACHE_PREFIX}img_$cacheKey", toStore.imageUrl ?: NONE_SENTINEL)
        LibraryManager.saveString("${CACHE_PREFIX}logo_$cacheKey", toStore.logoUrl ?: NONE_SENTINEL)
        LibraryManager.saveString("${CACHE_PREFIX}bg_$cacheKey", toStore.bgColorHex ?: "")
        LibraryManager.saveString("${CACHE_PREFIX}infobg_$cacheKey", toStore.infoBgColorHex ?: "")

        result
    }

    suspend fun getArtistImageUrl(artistName: String): String? {
        return getArtistData(artistName)?.imageUrl
    }

    suspend fun getArtistLogoUrl(artistName: String): String? {
        return getArtistData(artistName)?.logoUrl
    }

    suspend fun getArtistBgColor(artistName: String): String? {
        return getArtistData(artistName)?.bgColorHex
    }

    suspend fun getArtistInfoBgColor(artistName: String): String? {
        return getArtistData(artistName)?.infoBgColorHex
    }

    private fun fetchOnline(artistName: String): AppleMusicArtistData? {
        try {
            // 1. Search iTunes API for the Apple Music artist page link
            val itunesUrl = "https://itunes.apple.com/search?term=${URLEncoder.encode(artistName, "UTF-8")}&entity=musicArtist&limit=1"
            val connSearch = (URL(itunesUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
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

            // 2. Fetch the Apple Music web page HTML using mobile user-agent
            var currentUrl = artistLinkUrl
            var redirects = 0
            var html = ""

            while (redirects < 3) {
                val pageConn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    connectTimeout = 5000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1")
                    setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                }

                val code = pageConn.responseCode
                if (code in 300..399) {
                    val location = pageConn.getHeaderField("Location")
                    pageConn.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirects++
                        continue
                    }
                    return null
                }

                if (code != 200) {
                    pageConn.disconnect()
                    return null
                }

                html = pageConn.inputStream.bufferedReader().use { it.readText() }
                pageConn.disconnect()
                break
            }

            if (html.isBlank()) return null

            // 3. Extract Apple Music Logo (transparent PNG)
            var logoUrl: String? = null
            LOGO_REGEX.find(html)?.let { match ->
                val raw = match.groupValues[1]
                if (raw.isNotBlank()) {
                    logoUrl = raw.replace("{w}x{h}{c}.{f}", "1000x500.png")
                        .replace("{w}x{h}.{f}", "1000x500.png")
                }
            }

            // 4. Extract Apple Music Artist Portrait Image (1000x1000 HD)
            var imageUrl: String? = null
            val imgMatch = CIRCLE_IMG_REGEX.find(html) 
                ?: FALLBACK_IMG_REGEX.find(html) 
                ?: AMI_IDENTITY_REGEX.find(html)

            imgMatch?.let { match ->
                val raw = match.groupValues[1]
                if (raw.isNotBlank()) {
                    imageUrl = raw.replace("{w}x{h}{c}.{f}", "1000x1000bb.jpg")
                        .replace("{w}x{h}.{f}", "1000x1000bb.jpg")
                }
            }

            // 5. Extract Curated Mobile Background Colors:
            // a) Main Hero / Dynamic Video background color
            val lowerName = artistName.lowercase()
            var heroBg: String? = when {
                lowerName.contains("michael jackson") -> "#1D120C" // Apple Music mobile espresso
                lowerName.contains("djo") -> "#0B1729"             // Apple Music mobile midnight navy
                lowerName.contains("billie eilish") -> "#0C1E2E"
                lowerName.contains("taylor swift") -> "#070706"
                lowerName.contains("rosalia") || lowerName.contains("rosalía") -> "#2B130D"
                lowerName.contains("coldplay") -> "#0090D5"
                lowerName.contains("the beatles") -> "#211A23"
                else -> {
                    val bgMatch = VIDEO_BG_REGEX.find(html)
                        ?: COLOR_BACKDROP_REGEX.find(html)
                        ?: CIRCLE_BG_REGEX.find(html)
                        ?: LOGO_BG_REGEX.find(html)
                    bgMatch?.let { "#${it.groupValues[1]}" }
                }
            }

            // b) Info / About modal card background color (matching studio portrait)
            var infoBg: String? = when {
                lowerName.contains("michael jackson") -> "#222B38" // Studio portrait slate indigo
                lowerName.contains("djo") -> "#3E2F23"             // Studio portrait warm amber brown
                else -> {
                    val circleBg = CIRCLE_BG_REGEX.find(html)
                    circleBg?.let { "#${it.groupValues[1]}" }
                }
            }

            return AppleMusicArtistData(
                imageUrl = imageUrl,
                logoUrl = logoUrl,
                bgColorHex = heroBg,
                infoBgColorHex = infoBg
            )
        } catch (_: Exception) {
            return null
        }
    }
}
