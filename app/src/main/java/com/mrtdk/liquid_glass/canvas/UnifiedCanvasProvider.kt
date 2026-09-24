package com.mrtdk.liquid_glass.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object UnifiedCanvasProvider {

    private data class CacheEntry(
        val url: String?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    /**
     * Resolves song-specific motion artwork matching Echo-Music resolution:
     * 1. Check in-memory fast cache
     * 2. Normalize title and artist
     * 3. Providers in priority order:
     *    - EchoMusicCanvasProvider (direct manifest - instant 0ms)
     *    - TidalCanvasProvider (Tidal video cover for song)
     *    - AppleMusicCanvasProvider (Apple Music song search with parent album motion lookup)
     *    - ArtistVideoCanvasProvider (ArchiveTune fallback)
     * 4. Strict Echo-Music validation:
     *    - Artist match verification
     *    - Title/Album match verification to prevent cross-song/cross-album pollution
     */
    suspend fun getSongCanvas(
        songTitle: String,
        artist: String,
        album: String? = null
    ): String? = withContext(Dispatchers.IO) {
        if (songTitle.isBlank() || artist.isBlank()) return@withContext null

        val rawTitle = songTitle.trim()
        val rawArtist = artist.trim()
        val requestedAlbum = album?.trim().orEmpty()

        val normalizedTitle = normalizeCanvasSongTitle(rawTitle)
        val normalizedArtist = normalizeCanvasArtistName(rawArtist)

        val cacheKey = "song|${normalizedTitle.lowercase(Locale.ROOT)}|${normalizedArtist.lowercase(Locale.ROOT)}|${requestedAlbum.lowercase(Locale.ROOT)}"
        cache[cacheKey]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withContext it.url }

        val storefront = runCatching {
            val country = Locale.getDefault().country
            if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
        }.getOrDefault("us")

        val searchPairs = linkedSetOf(
            normalizedTitle to normalizedArtist,
            rawTitle to normalizedArtist,
            normalizedTitle to rawArtist,
            rawTitle to rawArtist
        ).filter { (s, a) -> s.isNotBlank() && a.isNotBlank() }

        var candidateArtwork: CanvasArtwork? = null

        for ((s, a) in searchPairs) {
            // 1. EchoMusic direct JSON manifest (0ms instant)
            try {
                val echoRes = EchoMusicCanvasProvider.getBySongArtist(s, a)
                if (!echoRes?.preferredAnimationUrl.isNullOrBlank()) {
                    candidateArtwork = echoRes
                    break
                }
            } catch (_: Exception) {}

            // 2. Tidal video covers
            try {
                val tidalRes = TidalCanvasProvider.getBySongArtist(s, a, requestedAlbum.ifBlank { null })
                if (!tidalRes?.preferredAnimationUrl.isNullOrBlank()) {
                    candidateArtwork = tidalRes
                    break
                }
            } catch (_: Exception) {}

            // 3. Apple Music by song/artist with album resolution
            try {
                val amRes = AppleMusicCanvasProvider.getBySongArtist(s, a, requestedAlbum.ifBlank { null }, storefront)
                if (!amRes?.preferredAnimationUrl.isNullOrBlank()) {
                    candidateArtwork = amRes
                    break
                }
            } catch (_: Exception) {}

            // 4. ArchiveTune / Koiiverse fallback
            try {
                val archiveRes = ArtistVideoCanvasProvider.getBySongArtist(s, a, requestedAlbum.ifBlank { null })
                if (!archiveRes?.preferredAnimationUrl.isNullOrBlank()) {
                    candidateArtwork = archiveRes
                    break
                }
            } catch (_: Exception) {}
        }

        // 5. Album motion fallback if song search did not yield direct motion video
        if (candidateArtwork == null && requestedAlbum.isNotBlank()) {
            val albumCandidates = linkedSetOf(requestedAlbum, normalizeCanvasSongTitle(requestedAlbum))
            for (alb in albumCandidates.filter { it.isNotBlank() }) {
                try {
                    val amAlb = AppleMusicCanvasProvider.getByAlbumArtist(alb, rawArtist, storefront)
                    if (!amAlb?.preferredAnimationUrl.isNullOrBlank()) {
                        candidateArtwork = amAlb
                        break
                    }
                } catch (_: Exception) {}

                try {
                    val tidalAlb = TidalCanvasProvider.getByAlbumArtist(alb, rawArtist)
                    if (!tidalAlb?.preferredAnimationUrl.isNullOrBlank()) {
                        candidateArtwork = tidalAlb
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        // Strict motion validation algorithm
        val validated = candidateArtwork?.let { artwork ->
            val localArtists = splitAndNormalizeArtists(rawArtist)
            val returnedArtists = splitAndNormalizeArtists(artwork.artist.orEmpty())

            fun isFuzzyMatch(a: String, b: String): Boolean {
                val aClean = a.trim()
                val bClean = b.trim()
                if (aClean.isBlank() || bClean.isBlank()) return false
                if (aClean.length <= 3 || bClean.length <= 3) {
                    return aClean.equals(bClean, ignoreCase = true)
                }
                return aClean.contains(bClean, ignoreCase = true) || bClean.contains(aClean, ignoreCase = true)
            }

            val artistMatches = if (localArtists.isNotEmpty() && returnedArtists.isNotEmpty()) {
                localArtists.any { local ->
                    returnedArtists.any { ret -> isFuzzyMatch(local, ret) }
                }
            } else true

            val canvasAlbumName = artwork.albumName
            val canvasSongName = artwork.name

            val normReqTitle = normalizeCanvasSongTitle(rawTitle)
            val normReqAlb = if (requestedAlbum.isNotBlank()) normalizeCanvasSongTitle(requestedAlbum) else ""
            val normCanvasSong = if (!canvasSongName.isNullOrBlank()) normalizeCanvasSongTitle(canvasSongName) else null
            val normCanvasAlb = if (!canvasAlbumName.isNullOrBlank()) normalizeCanvasSongTitle(canvasAlbumName) else null

            val titleMatches = when {
                // 1. If motion song title is present, it MUST match the requested song title
                normCanvasSong != null && normReqTitle.isNotBlank() -> {
                    isFuzzyMatch(canvasSongName!!, rawTitle) || isFuzzyMatch(normCanvasSong, normReqTitle)
                }
                // 2. If it is an album-level canvas and song's album was provided, verify album match
                normCanvasAlb != null && requestedAlbum.isNotBlank() -> {
                    isFuzzyMatch(canvasAlbumName!!, requestedAlbum) || isFuzzyMatch(normCanvasAlb, normReqAlb)
                }
                // 3. When played individually from search/home (requestedAlbum is blank), an album canvas
                // ONLY matches if the song title matches the album name (i.e. self-titled album or title track)
                normCanvasAlb != null && requestedAlbum.isBlank() -> {
                    isFuzzyMatch(canvasAlbumName!!, rawTitle) || isFuzzyMatch(normCanvasAlb, normReqTitle)
                }
                else -> false
            }

            if (artistMatches && titleMatches) artwork else null
        }

        val foundUrl = validated?.preferredAnimationUrl
        cache[cacheKey] = CacheEntry(foundUrl, System.currentTimeMillis() + (if (foundUrl != null) CACHE_TTL_MS else (1000L * 60 * 30)))
        foundUrl
    }

    /**
     * Resolves album-specific motion artwork matching Echo-Music CanvasAlbum:
     * 1. Check in-memory fast cache
     * 2. Normalize album title, artist, and lead track
     * 3. Providers:
     *    - EchoMusicCanvasProvider (direct manifest - instant)
     *    - AppleMusicCanvasProvider (Apple Music editorialVideo for album)
     *    - TidalCanvasProvider (Tidal album cover)
     * 4. Validates artist and album title to prevent mismatched artwork
     */
    suspend fun getAlbumCanvas(
        albumTitle: String,
        artist: String,
        firstSongTitle: String? = null
    ): String? = withContext(Dispatchers.IO) {
        if (albumTitle.isBlank() || artist.isBlank()) return@withContext null

        val rawAlbum = albumTitle.trim()
        val rawArtist = artist.trim()
        val rawFirstSong = firstSongTitle?.trim()

        val normAlbum = normalizeCanvasSongTitle(rawAlbum)
        val normArtist = normalizeCanvasArtistName(rawArtist)
        val normFirstSong = rawFirstSong?.let { normalizeCanvasSongTitle(it) }

        val cacheKey = "album|${normAlbum.lowercase(Locale.ROOT)}|${normArtist.lowercase(Locale.ROOT)}"
        cache[cacheKey]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withContext it.url }

        val storefront = runCatching {
            val country = Locale.getDefault().country
            if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
        }.getOrDefault("us")

        val searchTasks = linkedSetOf(
            normAlbum to normArtist,
            rawAlbum to normArtist,
            normAlbum to rawArtist,
            rawAlbum to rawArtist
        )
        if (!normFirstSong.isNullOrBlank()) {
            searchTasks.add(normFirstSong to normArtist)
            searchTasks.add(rawFirstSong!! to normArtist)
            searchTasks.add(normFirstSong to rawArtist)
            searchTasks.add(rawFirstSong to rawArtist)
        }

        var candidateArtwork: CanvasArtwork? = null

        for ((s, a) in searchTasks.filter { (s, a) -> s.isNotBlank() && a.isNotBlank() }) {
            // 1. EchoMusic direct JSON manifest
            try {
                val echoRes = EchoMusicCanvasProvider.getBySongArtist(s, a)
                if (!echoRes?.preferredAnimationUrl.isNullOrBlank()) {
                    candidateArtwork = echoRes
                    break
                }
            } catch (_: Exception) {}

            // 2. Apple Music album editorial video
            try {
                val amRes = AppleMusicCanvasProvider.getByAlbumArtist(s, a, storefront)
                if (!amRes?.preferredAnimationUrl.isNullOrBlank()) {
                    candidateArtwork = amRes
                    break
                }
            } catch (_: Exception) {}

            // 3. Tidal album cover
            try {
                val tidalRes = TidalCanvasProvider.getByAlbumArtist(s, a)
                if (!tidalRes?.preferredAnimationUrl.isNullOrBlank()) {
                    candidateArtwork = tidalRes
                    break
                }
            } catch (_: Exception) {}
        }

        val validated = candidateArtwork?.let { artwork ->
            val localArtists = splitAndNormalizeArtists(rawArtist)
            val returnedArtists = splitAndNormalizeArtists(artwork.artist.orEmpty())

            fun isFuzzyMatch(a: String, b: String): Boolean {
                val aClean = a.trim()
                val bClean = b.trim()
                if (aClean.isBlank() || bClean.isBlank()) return false
                if (aClean.length <= 3 || bClean.length <= 3) {
                    return aClean.equals(bClean, ignoreCase = true)
                }
                return aClean.contains(bClean, ignoreCase = true) || bClean.contains(aClean, ignoreCase = true)
            }

            val artistMatches = if (localArtists.isNotEmpty() && returnedArtists.isNotEmpty()) {
                localArtists.any { local -> returnedArtists.any { ret -> isFuzzyMatch(local, ret) } }
            } else true

            val canvasAlbumName = artwork.albumName ?: artwork.name
            val albumMatches = if (canvasAlbumName != null && rawAlbum.isNotBlank()) {
                val normCanvasAlb = normalizeCanvasSongTitle(canvasAlbumName)
                isFuzzyMatch(canvasAlbumName, rawAlbum) || isFuzzyMatch(normCanvasAlb, normAlbum)
            } else false

            if (artistMatches && albumMatches) artwork else null
        }

        val foundUrl = validated?.preferredAnimationUrl
        cache[cacheKey] = CacheEntry(foundUrl, System.currentTimeMillis() + (if (foundUrl != null) CACHE_TTL_MS else (1000L * 60 * 30)))
        foundUrl
    }

    /**
     * Backwards-compatible router delegating to getAlbumCanvas or getSongCanvas.
     */
    suspend fun getSongOrAlbumCanvas(
        songOrAlbum: String,
        artist: String,
        album: String? = null
    ): String? = withContext(Dispatchers.IO) {
        if (!album.isNullOrBlank() && songOrAlbum.equals(album, ignoreCase = true)) {
            getAlbumCanvas(albumTitle = songOrAlbum, artist = artist)
        } else {
            getSongCanvas(songTitle = songOrAlbum, artist = artist, album = album)
        }
    }

    /**
     * Resolves artist motion video header for ArtistScreen.
     */
    suspend fun getArtistMotionVideo(artistName: String): String? = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext null
        try {
            AppleMusicArtistBackgroundProvider.getByArtistName(artistName)
        } catch (_: Exception) {
            null
        }
    }

    fun normalizeCanvasSongTitle(raw: String): String {
        val stripped = raw
            .replace(Regex("\\s*\\[[^]]*]"), "")
            .replace(
                Regex(
                    "\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(Regex("\\s+"), " ")
            .trim()

        return stripped
            .trim('-')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun normalizeCanvasArtistName(raw: String): String {
        val first = raw
            .split(
                Regex(
                    "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
                    RegexOption.IGNORE_CASE,
                ),
                limit = 2,
            ).firstOrNull().orEmpty()

        return first.replace(Regex("\\s+"), " ").trim()
    }

    fun splitAndNormalizeArtists(raw: String): List<String> {
        return raw
            .split(
                Regex(
                    "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b|/|;)",
                    RegexOption.IGNORE_CASE,
                )
            )
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
    }
}
