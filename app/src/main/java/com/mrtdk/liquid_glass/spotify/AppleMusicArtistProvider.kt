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
    val infoBgColorHex: String?,
    val videoUrl: String? = null,
    val genre: String? = null,
    val from: String? = null,
    val born: String? = null,
    val bio: String? = null
)

/**
 * Unified provider for official Apple Music artist assets:
 * - Studio portrait images (1000x1000)
 * - Official typography wordmarks / logos (transparent PNG)
 * - Curated mobile background colors (for dynamic video & hero)
 * - Curated mobile info / about modal colors
 * - Dynamic motion header video (HLS / m3u8)
 * - Metadata (Genre, From/Origin, Born/Formed, Bio) via Apple Music, MusicBrainz & Wikipedia
 */
object AppleMusicArtistProvider {
    private val memoryCache = ConcurrentHashMap<String, AppleMusicArtistData>()
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<AppleMusicArtistData?>>()
    private val providerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private const val NONE_SENTINEL = "NONE"
    private const val CACHE_PREFIX = "am_v5_"

    private val LOGO_REGEX = Regex(""""artistLogo"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)"""")
    private val CIRCLE_IMG_REGEX = Regex(""""circleArtwork"\s*:\s*\{"dictionary":\{[^}]*"url"\s*:\s*"([^"]+)"""")
    private val FALLBACK_IMG_REGEX = Regex(""""fallbackArtwork"\s*:\s*\{"dictionary":\{[^}]*"url"\s*:\s*"([^"]+)"""")
    private val AMI_IDENTITY_REGEX = Regex(""""url"\s*:\s*"(https://[^"]*ami-identity[^"]*)"""")

    private val VIDEO_URL_REGEX = Regex(""""video"\s*:\s*"(https://[^"]+m3u8)"""")
    private val VIDEO_BG_REGEX = Regex(""""videoArtwork"\s*:\s*\{"dictionary":\{[^}]*"bgColor"\s*:\s*"([0-9a-fA-F]{6})"""")
    private val COLOR_BACKDROP_REGEX = Regex(""""colorBackdropArtwork"\s*:\s*\{"dictionary":\{[^}]*"bgColor"\s*:\s*"([0-9a-fA-F]{6})"""")
    private val CIRCLE_BG_REGEX = Regex(""""circleArtwork"\s*:\s*\{"dictionary":\{[^}]*"bgColor"\s*:\s*"([0-9a-fA-F]{6})"""")
    private val LOGO_BG_REGEX = Regex(""""artistLogo"\s*:\s*\{"dictionary":\{[^}]*"bgColor"\s*:\s*"([0-9a-fA-F]{6})"""")
    private val FALLBACK_BG_REGEX = Regex(""""fallbackArtwork"\s*:\s*\{"dictionary":\{[^}]*"bgColor"\s*:\s*"([0-9a-fA-F]{6})"""")
    private val ANY_BG_REGEX = Regex(""""bgColor"\s*:\s*"([0-9a-fA-F]{6})"""")

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
        val savedVid = LibraryManager.getString("${CACHE_PREFIX}vid_$cacheKey")
        val savedGenre = LibraryManager.getString("${CACHE_PREFIX}genre_$cacheKey")
        val savedFrom = LibraryManager.getString("${CACHE_PREFIX}from_$cacheKey")
        val savedBorn = LibraryManager.getString("${CACHE_PREFIX}born_$cacheKey")
        val savedBio = LibraryManager.getString("${CACHE_PREFIX}bio_$cacheKey")

        if (savedImg != null || savedLogo != null || savedVid != null || savedGenre != null || savedFrom != null) {
            val data = AppleMusicArtistData(
                imageUrl = if (savedImg == NONE_SENTINEL) null else savedImg,
                logoUrl = if (savedLogo == NONE_SENTINEL) null else savedLogo,
                bgColorHex = savedBg?.takeIf { it.isNotBlank() },
                infoBgColorHex = savedInfoBg?.takeIf { it.isNotBlank() },
                videoUrl = if (savedVid == NONE_SENTINEL) null else savedVid?.takeIf { it.isNotBlank() },
                genre = if (savedGenre == NONE_SENTINEL) null else savedGenre?.takeIf { it.isNotBlank() },
                from = if (savedFrom == NONE_SENTINEL) null else savedFrom?.takeIf { it.isNotBlank() },
                born = if (savedBorn == NONE_SENTINEL) null else savedBorn?.takeIf { it.isNotBlank() },
                bio = if (savedBio == NONE_SENTINEL) null else savedBio?.takeIf { it.isNotBlank() }
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
            val pVid = LibraryManager.getString("${CACHE_PREFIX}vid_$primaryKey")
            val pGenre = LibraryManager.getString("${CACHE_PREFIX}genre_$primaryKey")
            val pFrom = LibraryManager.getString("${CACHE_PREFIX}from_$primaryKey")
            val pBorn = LibraryManager.getString("${CACHE_PREFIX}born_$primaryKey")
            val pBio = LibraryManager.getString("${CACHE_PREFIX}bio_$primaryKey")
            if (pImg != null || pLogo != null || pVid != null || pGenre != null || pFrom != null) {
                val data = AppleMusicArtistData(
                    imageUrl = if (pImg == NONE_SENTINEL) null else pImg,
                    logoUrl = if (pLogo == NONE_SENTINEL) null else pLogo,
                    bgColorHex = pBg?.takeIf { it.isNotBlank() },
                    infoBgColorHex = pInfoBg?.takeIf { it.isNotBlank() },
                    videoUrl = if (pVid == NONE_SENTINEL) null else pVid?.takeIf { it.isNotBlank() },
                    genre = if (pGenre == NONE_SENTINEL) null else pGenre?.takeIf { it.isNotBlank() },
                    from = if (pFrom == NONE_SENTINEL) null else pFrom?.takeIf { it.isNotBlank() },
                    born = if (pBorn == NONE_SENTINEL) null else pBorn?.takeIf { it.isNotBlank() },
                    bio = if (pBio == NONE_SENTINEL) null else pBio?.takeIf { it.isNotBlank() }
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

    fun getCachedVideoUrl(artistName: String): String? {
        return getCachedArtistData(artistName)?.videoUrl
    }

    fun getCachedGenre(artistName: String): String? {
        return getCachedArtistData(artistName)?.genre
    }

    fun getCachedFrom(artistName: String): String? {
        return getCachedArtistData(artistName)?.from
    }

    fun getCachedBorn(artistName: String): String? {
        return getCachedArtistData(artistName)?.born
    }

    fun getCachedBio(artistName: String): String? {
        return getCachedArtistData(artistName)?.bio
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

        val toStore = result ?: AppleMusicArtistData(null, null, null, null, null, null, null, null, null)
        memoryCache[cacheKey] = toStore
        LibraryManager.saveString("${CACHE_PREFIX}img_$cacheKey", toStore.imageUrl ?: NONE_SENTINEL)
        LibraryManager.saveString("${CACHE_PREFIX}logo_$cacheKey", toStore.logoUrl ?: NONE_SENTINEL)
        LibraryManager.saveString("${CACHE_PREFIX}bg_$cacheKey", toStore.bgColorHex ?: "")
        LibraryManager.saveString("${CACHE_PREFIX}infobg_$cacheKey", toStore.infoBgColorHex ?: "")
        LibraryManager.saveString("${CACHE_PREFIX}vid_$cacheKey", toStore.videoUrl ?: NONE_SENTINEL)
        LibraryManager.saveString("${CACHE_PREFIX}genre_$cacheKey", toStore.genre ?: NONE_SENTINEL)
        LibraryManager.saveString("${CACHE_PREFIX}from_$cacheKey", toStore.from ?: NONE_SENTINEL)
        LibraryManager.saveString("${CACHE_PREFIX}born_$cacheKey", toStore.born ?: NONE_SENTINEL)
        LibraryManager.saveString("${CACHE_PREFIX}bio_$cacheKey", toStore.bio ?: NONE_SENTINEL)

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

    suspend fun getArtistVideoUrl(artistName: String): String? {
        return getArtistData(artistName)?.videoUrl
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
            val primaryGenre = artistObj.optString("primaryGenreName").takeIf { it.isNotBlank() }

            // 1b. Fetch additional metadata (origin, born date, bio) in parallel or sequence
            val bioInfo = fetchArtistMetadata(cleanArtistName(artistName), primaryGenre)

            if (artistLinkUrl.isNullOrBlank()) {
                return AppleMusicArtistData(
                    imageUrl = null,
                    logoUrl = null,
                    bgColorHex = null,
                    infoBgColorHex = null,
                    videoUrl = null,
                    genre = bioInfo.genre,
                    from = bioInfo.from,
                    born = bioInfo.born,
                    bio = bioInfo.bio
                )
            }

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

            if (html.isBlank()) {
                return AppleMusicArtistData(
                    imageUrl = null,
                    logoUrl = null,
                    bgColorHex = null,
                    infoBgColorHex = null,
                    videoUrl = null,
                    genre = bioInfo.genre,
                    from = bioInfo.from,
                    born = bioInfo.born,
                    bio = bioInfo.bio
                )
            }

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
                        ?: FALLBACK_BG_REGEX.find(html)
                        ?: COLOR_BACKDROP_REGEX.find(html)
                        ?: ANY_BG_REGEX.find(html)
                    circleBg?.let { "#${it.groupValues[1]}" }
                }
            }

            // c) Dynamic Motion Video URL (HLS / m3u8)
            val videoUrl = VIDEO_URL_REGEX.find(html)?.groupValues?.getOrNull(1)

            return AppleMusicArtistData(
                imageUrl = imageUrl,
                logoUrl = logoUrl,
                bgColorHex = heroBg,
                infoBgColorHex = infoBg,
                videoUrl = videoUrl,
                genre = bioInfo.genre,
                from = bioInfo.from,
                born = bioInfo.born,
                bio = bioInfo.bio
            )
        } catch (_: Exception) {
            return null
        }
    }

    private data class ArtistBioInfo(
        val genre: String?,
        val from: String?,
        val born: String?,
        val bio: String?
    )

    private fun fetchArtistMetadata(artistName: String, primaryGenre: String?): ArtistBioInfo {
        var from: String? = null
        var born: String? = null
        var bio: String? = null
        var genre: String? = primaryGenre

        val clean = cleanArtistName(artistName)

        // 1. MusicBrainz for exact From (city/country) & Born/Formed date
        try {
            val mbUrl = "https://musicbrainz.org/ws/2/artist/?query=artist:${URLEncoder.encode(clean, "UTF-8")}&fmt=json&limit=1"
            val connMb = (URL(mbUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3200
                readTimeout = 3200
                setRequestProperty("User-Agent", "RayMusic/2.0 ( contact@raymusic.app )")
                setRequestProperty("Accept", "application/json")
            }
            if (connMb.responseCode == 200) {
                val mbJson = connMb.inputStream.bufferedReader().use { it.readText() }
                val mbRoot = JSONObject(mbJson)
                val artistsArr = mbRoot.optJSONArray("artists")
                if (artistsArr != null && artistsArr.length() > 0) {
                    val mbArtist = artistsArr.getJSONObject(0)
                    val beginArea = mbArtist.optJSONObject("begin-area")?.optString("name")?.trim()?.takeIf { it.isNotBlank() }
                    val area = mbArtist.optJSONObject("area")?.optString("name")?.trim()?.takeIf { it.isNotBlank() }
                    from = when {
                        beginArea != null && area != null && !area.equals(beginArea, ignoreCase = true) -> "$beginArea, $area"
                        beginArea != null -> beginArea
                        area != null -> area
                        else -> null
                    }
                    val rawBegin = mbArtist.optJSONObject("life-span")?.optString("begin")?.trim()?.takeIf { it.isNotBlank() }
                    if (rawBegin != null) {
                        born = formatDisplayDate(rawBegin)
                    }
                    if (genre.isNullOrBlank()) {
                        val tags = mbArtist.optJSONArray("tags")
                        if (tags != null && tags.length() > 0) {
                            val tag = tags.getJSONObject(0).optString("name")
                            if (tag.isNotBlank()) {
                                genre = tag.replaceFirstChar { it.uppercase() }
                            }
                        }
                    }
                }
            }
            connMb.disconnect()
        } catch (_: Exception) {}

        // 2. Wikipedia summary for Bio & fallback for Born / From
        try {
            val wikiTitle = clean.replace(' ', '_')
            val wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/${URLEncoder.encode(wikiTitle, "UTF-8")}"
            val connWiki = (URL(wikiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3200
                readTimeout = 3200
                setRequestProperty("User-Agent", "RayMusic/2.0 ( contact@raymusic.app )")
                setRequestProperty("Accept", "application/json")
            }
            if (connWiki.responseCode == 200) {
                val wikiJson = connWiki.inputStream.bufferedReader().use { it.readText() }
                val wikiRoot = JSONObject(wikiJson)
                val extract = wikiRoot.optString("extract").trim().takeIf { it.isNotBlank() }
                val desc = wikiRoot.optString("description").trim().takeIf { it.isNotBlank() }
                bio = extract

                if (born.isNullOrBlank() && extract != null) {
                    val bornMatch = Regex("""\(born\s+([A-Za-z0-9,\s]+?)\)""", RegexOption.IGNORE_CASE).find(extract)
                        ?: Regex("""born\s+(\d{1,2}\s+[A-Za-z]+\s+\d{4})""", RegexOption.IGNORE_CASE).find(extract)
                        ?: Regex("""born\s+([A-Za-z]+\s+\d{1,2},\s+\d{4})""", RegexOption.IGNORE_CASE).find(extract)
                        ?: Regex("""\(born\s+(\d{4})\)""", RegexOption.IGNORE_CASE).find(desc ?: "")
                    bornMatch?.groupValues?.getOrNull(1)?.let { raw ->
                        born = formatDisplayDate(raw.trim())
                    }
                }

                if (from.isNullOrBlank() && (desc != null || extract != null)) {
                    val combined = "$desc. $extract"
                    from = when {
                        combined.contains("American", ignoreCase = true) -> "United States"
                        combined.contains("English", ignoreCase = true) || combined.contains("British", ignoreCase = true) -> "United Kingdom"
                        combined.contains("Canadian", ignoreCase = true) -> "Canada"
                        combined.contains("Australian", ignoreCase = true) -> "Australia"
                        combined.contains("Spanish", ignoreCase = true) || combined.contains("español", ignoreCase = true) -> "Spain"
                        combined.contains("Mexican", ignoreCase = true) || combined.contains("mexicano", ignoreCase = true) -> "Mexico"
                        combined.contains("Colombian", ignoreCase = true) || combined.contains("colombiano", ignoreCase = true) -> "Colombia"
                        combined.contains("Argentine", ignoreCase = true) || combined.contains("argentino", ignoreCase = true) -> "Argentina"
                        combined.contains("Puerto Rican", ignoreCase = true) -> "Puerto Rico"
                        combined.contains("French", ignoreCase = true) || combined.contains("francés", ignoreCase = true) -> "France"
                        combined.contains("German", ignoreCase = true) || combined.contains("alemán", ignoreCase = true) -> "Germany"
                        combined.contains("Italian", ignoreCase = true) || combined.contains("italiano", ignoreCase = true) -> "Italy"
                        combined.contains("Japanese", ignoreCase = true) || combined.contains("japonés", ignoreCase = true) -> "Japan"
                        combined.contains("South Korean", ignoreCase = true) || combined.contains("coreano", ignoreCase = true) -> "South Korea"
                        combined.contains("Swedish", ignoreCase = true) -> "Sweden"
                        else -> null
                    }
                }
            }
            connWiki.disconnect()
        } catch (_: Exception) {}

        return ArtistBioInfo(
            genre = genre ?: "Pop",
            from = from,
            born = born,
            bio = bio
        )
    }

    fun formatDisplayDate(raw: String): String {
        val trimmed = raw.trim()
        val monthsEs = arrayOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
        )
        val monthMap = mapOf(
            "january" to 0, "jan" to 0, "enero" to 0,
            "february" to 1, "feb" to 1, "febrero" to 1,
            "march" to 2, "mar" to 2, "marzo" to 2,
            "april" to 3, "apr" to 3, "abril" to 3,
            "may" to 4, "mayo" to 4,
            "june" to 5, "jun" to 5, "junio" to 5,
            "july" to 6, "jul" to 6, "julio" to 6,
            "august" to 7, "aug" to 7, "agosto" to 7,
            "september" to 8, "sep" to 8, "septiembre" to 8,
            "october" to 9, "oct" to 9, "octubre" to 9,
            "november" to 10, "nov" to 10, "noviembre" to 10,
            "december" to 11, "dec" to 11, "diciembre" to 11
        )

        // YYYY-MM-DD
        val ymd = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""").find(trimmed)
        if (ymd != null) {
            val y = ymd.groupValues[1]
            val m = (ymd.groupValues[2].toIntOrNull() ?: 1) - 1
            val d = ymd.groupValues[3].toIntOrNull() ?: 1
            return "$d de ${monthsEs.getOrElse(m) { "enero" }} de $y"
        }

        // YYYY-MM
        val ym = Regex("""^(\d{4})-(\d{1,2})$""").find(trimmed)
        if (ym != null) {
            val y = ym.groupValues[1]
            val m = (ym.groupValues[2].toIntOrNull() ?: 1) - 1
            return "${monthsEs.getOrElse(m) { "enero" }} de $y"
        }

        // "Month Day, Year" or "Month Day Year"
        val mdy = Regex("""^([A-Za-z]+)\s+(\d{1,2})(?:,|\s+de)?\s+(\d{4})$""").find(trimmed)
        if (mdy != null) {
            val mStr = mdy.groupValues[1].lowercase()
            val d = mdy.groupValues[2].toIntOrNull() ?: 1
            val y = mdy.groupValues[3]
            val m = monthMap[mStr] ?: 0
            return "$d de ${monthsEs[m]} de $y"
        }

        // "Day Month Year" or "Day de Month de Year"
        val dmy = Regex("""^(\d{1,2})\s+(?:de\s+)?([A-Za-z]+)\s+(?:de\s+)?(\d{4})$""").find(trimmed)
        if (dmy != null) {
            val d = dmy.groupValues[1].toIntOrNull() ?: 1
            val mStr = dmy.groupValues[2].lowercase()
            val y = dmy.groupValues[3]
            val m = monthMap[mStr] ?: 0
            return "$d de ${monthsEs[m]} de $y"
        }

        return trimmed
    }
}
