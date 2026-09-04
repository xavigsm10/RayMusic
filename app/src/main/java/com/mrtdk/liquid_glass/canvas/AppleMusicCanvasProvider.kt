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

    private suspend fun searchAndFetchMotion(
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
            val token = AppleMusicTokenProvider.getToken()
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

            data class ScoredItem(val score: Int, val obj: JSONObject)
            val scoredList = mutableListOf<ScoredItem>()

            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val attributes = obj.optJSONObject("attributes") ?: continue
                val resultArtistName = attributes.optString("artistName")
                val resultName = attributes.optString("name")
                val resultCollectionName = attributes.optString("collectionName")

                // Filtering blacklist
                val nameLower = resultName.lowercase(Locale.ROOT)
                val collLower = resultCollectionName.lowercase(Locale.ROOT)
                val isBlacklisted = nameLower.contains("playlist") || nameLower.contains("set list") ||
                        collLower.contains("playlist") || collLower.contains("set list") ||
                        nameLower.contains("essentials") || collLower.contains("essentials") ||
                        collLower.contains("dj mix") || collLower.contains("mixed") ||
                        collLower.contains("apple music") || collLower.contains("today's hits") ||
                        nameLower.contains("session") || collLower.contains("session")

                if (isBlacklisted) continue

                // Artist match check
                val artistMatch = resultArtistName.equals(artist, ignoreCase = true)
                val artistFuzzy = resultArtistName.contains(artist, ignoreCase = true) || artist.contains(resultArtistName, ignoreCase = true)
                if (!artistFuzzy) continue

                var score = if (artistMatch) 10 else 5

                val nameMatch = resultName.equals(term, ignoreCase = true)
                val nameFuzzy = resultName.contains(term, ignoreCase = true) || term.contains(resultName, ignoreCase = true)
                if (nameMatch) {
                    score += 15
                } else if (nameFuzzy) {
                    score += 7
                } else {
                    score -= 10
                }

                if (!album.isNullOrBlank() && resultCollectionName.isNotBlank()) {
                    val albumMatch = resultCollectionName.equals(album, ignoreCase = true)
                    val albumFuzzy = resultCollectionName.contains(album, ignoreCase = true) || album.contains(resultCollectionName, ignoreCase = true)
                    if (albumMatch) score += 20
                    else if (albumFuzzy) score += 10
                }

                scoredList.add(ScoredItem(score, obj))
            }

            scoredList.sortByDescending { it.score }

            for ((score, obj) in scoredList) {
                if (score < 12) continue

                val attributes = obj.optJSONObject("attributes") ?: continue
                val resultName = attributes.optString("name")
                val resultArtistName = attributes.optString("artistName")

                // 1. Resolve Album ID
                var targetAlbumId: String? = null
                val objType = obj.optString("type")
                if (objType == "songs" || type == "songs") {
                    val relationships = obj.optJSONObject("relationships")
                    val albumsData = relationships?.optJSONObject("albums")?.optJSONArray("data")
                    targetAlbumId = albumsData?.optJSONObject(0)?.optString("id")
                        ?: attributes.optString("collectionId").takeIf { it.isNotBlank() }

                    if (targetAlbumId.isNullOrBlank()) {
                        val urlStr = attributes.optString("url")
                        if (urlStr.contains("/album/")) {
                            val albumPart = urlStr.substringAfter("/album/", "").substringBefore("?")
                            val id = albumPart.substringAfterLast("/", "")
                            if (id.isNotBlank() && id.all { it.isDigit() }) {
                                targetAlbumId = id
                            }
                        }
                    }
                } else if (objType == "albums" || type == "albums") {
                    targetAlbumId = obj.optString("id")
                }

                if (targetAlbumId.isNullOrBlank() || targetAlbumId.startsWith("pl.")) continue

                // 2. Direct editorialVideo in result
                val editorialVideo = attributes.optJSONObject("editorialVideo")
                if (editorialVideo != null) {
                    val videoUrl = extractEditorialVideoUrl(editorialVideo)
                    if (!videoUrl.isNullOrBlank()) {
                        val collName = attributes.optString("collectionName")
                        val resolvedAlbumName = if (type == "songs") collName else resultName
                        return CanvasArtwork(
                            name = resultName,
                            artist = resultArtistName,
                            albumId = targetAlbumId,
                            albumName = resolvedAlbumName,
                            videoUrl = videoUrl,
                            animated = videoUrl
                        )
                    }
                }

                // 3. Lookup parent album motion
                val fetched = fetchMotionArtwork(
                    albumId = targetAlbumId,
                    storefront = storefront,
                    fallbackArtist = resultArtistName,
                    titleOverride = if (type == "songs") resultName else null,
                    artistOverride = if (type == "songs") resultArtistName else null
                )
                if (fetched != null) return fetched
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchMotionArtwork(
        albumId: String,
        storefront: String,
        fallbackArtist: String?,
        titleOverride: String? = null,
        artistOverride: String? = null,
    ): CanvasArtwork? {
        if (albumId.startsWith("pl.")) return null
        return try {
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/albums/$albumId?extend=editorialVideo"
            val token = AppleMusicTokenProvider.getToken()
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Origin", "https://music.apple.com")
                .header("Referer", "https://music.apple.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            val root = JSONObject(body)
            val data = root.optJSONArray("data")
            if (data == null || data.length() == 0) return null
            val albumObj = data.optJSONObject(0) ?: return null
            val attributes = albumObj.optJSONObject("attributes") ?: return null
            val albumName = attributes.optString("name")
            val artistName = attributes.optString("artistName").ifBlank { fallbackArtist ?: "" }

            // Playlist/station filtering
            val nameLower = albumName.lowercase(Locale.ROOT)
            val isBlacklisted = nameLower.contains("playlist") || nameLower.contains("set list") ||
                    nameLower.contains("essentials") || nameLower.contains("dj mix") ||
                    nameLower.contains("mixed") || nameLower.contains("apple music") ||
                    nameLower.contains("today's hits") || nameLower.contains("session")
            if (isBlacklisted) return null

            val finalTitle = titleOverride ?: albumName
            val finalArtist = artistOverride ?: artistName

            val ev = attributes.optJSONObject("editorialVideo")
            if (ev != null) {
                val videoUrl = extractEditorialVideoUrl(ev)
                if (!videoUrl.isNullOrBlank()) {
                    return CanvasArtwork(
                        name = finalTitle,
                        artist = finalArtist,
                        albumId = albumId,
                        albumName = albumName,
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
