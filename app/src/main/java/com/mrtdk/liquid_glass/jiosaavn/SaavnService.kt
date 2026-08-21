package com.mrtdk.liquid_glass.jiosaavn

import android.util.Base64
import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Serializable
data class SaavnDownloadUrl(
    @SerialName("quality") val quality: String = "",
    @SerialName("url")     val url: String     = ""
)

@Serializable
data class SaavnImage(
    @SerialName("quality") val quality: String = "",
    @SerialName("url")     val url: String     = ""
)

@Serializable
data class SaavnArtistItem(
    @SerialName("id")   val id: String   = "",
    @SerialName("name") val name: String = ""
)

@Serializable
data class SaavnArtists(
    @SerialName("primary")  val primary:  List<SaavnArtistItem> = emptyList(),
    @SerialName("featured") val featured: List<SaavnArtistItem> = emptyList(),
    @SerialName("all")      val all:      List<SaavnArtistItem> = emptyList()
)

@Serializable
data class SaavnAlbum(
    @SerialName("id")   val id:   String? = null,
    @SerialName("name") val name: String? = null
)

@Serializable
data class SaavnSong(
    @SerialName("id")              val id:              String                 = "",
    @SerialName("name")            val name:            String                 = "",
    @SerialName("duration")        val duration:        Int?                   = null,
    @SerialName("explicitContent") val explicitContent: Boolean                = false,
    @SerialName("artists")         val artists:         SaavnArtists           = SaavnArtists(),
    @SerialName("image")           val image:           List<SaavnImage>       = emptyList(),
    @SerialName("downloadUrl")     val downloadUrl:     List<SaavnDownloadUrl> = emptyList(),
    @SerialName("album")           val album:           SaavnAlbum?            = null,
    val isProOnly: Boolean = false
)

@Serializable
internal data class RawArtistMapItem(
    val id: String = "",
    val name: String = "",
    val role: String = "",
    val type: String = ""
)

@Serializable
internal data class RawArtistMap(
    @SerialName("primary_artists") val primaryArtists: List<RawArtistMapItem> = emptyList(),
    @SerialName("featured_artists") val featuredArtists: List<RawArtistMapItem> = emptyList(),
    val artists: List<RawArtistMapItem> = emptyList()
)

@Serializable
internal data class RawRights(
    val code: String = "",
    val cacheable: String = "",
    @SerialName("delete_cached_object") val deleteCachedObject: String = "",
    val reason: String = ""
) {
    val isProOnly: Boolean
        get() = code == "1" || reason.contains("Pro Only", ignoreCase = true)
}

@Serializable
internal data class RawMoreInfo(
    val album_id: String = "",
    val album: String = "",
    @SerialName("encrypted_media_url") val encryptedMediaUrl: String = "",
    val duration: String = "",
    val artistMap: RawArtistMap = RawArtistMap(),
    val rights: RawRights = RawRights()
)

@Serializable
internal data class RawSongItem(
    val id: String = "",
    val title: String = "",
    val type: String = "",
    val year: String = "",
    val image: String = "",
    val language: String = "",
    @SerialName("play_count") val playCount: String = "",
    @SerialName("explicit_content") val explicitContent: String = "",
    @SerialName("more_info") val moreInfo: RawMoreInfo = RawMoreInfo()
)

@Serializable
internal data class RawSearchResponse(
    val total: Int = 0,
    val start: Int = 0,
    val results: List<RawSongItem> = emptyList()
)

object SaavnService {
    private const val TAG = "SaavnService"
    private const val BASE_URL = "https://www.jiosaavn.com/api.php"

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .fastFallback(true)
        .build()

    private fun decryptUrl(encryptedUrl: String): String {
        if (encryptedUrl.isBlank()) return ""
        return try {
            val key = "38346591" // DES 8-byte key
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8).trim()
        } catch (e: Exception) {
            Log.e(TAG, "JioSaavn URL decryption failed", e)
            ""
        }
    }

    private fun createDownloadLinks(encryptedUrl: String): List<SaavnDownloadUrl> {
        val decryptedUrl = decryptUrl(encryptedUrl)
        if (decryptedUrl.isBlank()) return emptyList()

        val qualities = listOf(
            Pair("_96", "96kbps"),
            Pair("_160", "160kbps"),
            Pair("_320", "320kbps")
        )

        val suffixRegex = Regex("_(48|96|160|320)\\.(mp4|aac|mp3)$")
        return qualities.map { (suffix, bitrate) ->
            val url = if (decryptedUrl.contains(suffixRegex)) {
                decryptedUrl.replace(suffixRegex) { match ->
                    "${suffix}.${match.groupValues[2]}"
                }
            } else {
                decryptedUrl.replace("_96", suffix)
            }
            SaavnDownloadUrl(quality = bitrate, url = url)
        }
    }

    private fun createImageLinks(link: String): List<SaavnImage> {
        if (link.isBlank()) return emptyList()
        val qualities = listOf("50x50", "150x150", "500x500")
        val qualityRegex = Regex("150x150|50x50")
        val protocolRegex = Regex("^http://")

        return qualities.map { quality ->
            val url = link.replace(qualityRegex, quality).replace(protocolRegex, "https://")
            SaavnImage(quality = quality, url = url)
        }
    }

    private fun mapRawToSaavnSong(raw: RawSongItem): SaavnSong {
        val primaryArtists = raw.moreInfo.artistMap.primaryArtists.map {
            SaavnArtistItem(id = it.id, name = it.name)
        }
        val featuredArtists = raw.moreInfo.artistMap.featuredArtists.map {
            SaavnArtistItem(id = it.id, name = it.name)
        }
        val allArtists = raw.moreInfo.artistMap.artists.map {
            SaavnArtistItem(id = it.id, name = it.name)
        }

        // Clean HTML entities if any in title
        val cleanTitle = raw.title.replace("&quot;", "\"").replace("&amp;", "&").replace("&#039;", "'")

        return SaavnSong(
            id = raw.id,
            name = cleanTitle,
            duration = raw.moreInfo.duration.toIntOrNull(),
            explicitContent = raw.explicitContent == "1",
            artists = SaavnArtists(
                primary = primaryArtists,
                featured = featuredArtists,
                all = allArtists
            ),
            image = createImageLinks(raw.image),
            downloadUrl = createDownloadLinks(raw.moreInfo.encryptedMediaUrl),
            album = SaavnAlbum(
                id = raw.moreInfo.album_id,
                name = raw.moreInfo.album
            ),
            isProOnly = raw.moreInfo.rights.isProOnly
        )
    }

    suspend fun searchSongs(query: String): Result<List<SaavnSong>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val url = "$BASE_URL?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=android&q=${java.net.URLEncoder.encode(query, "UTF-8")}&p=1&n=10"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                .header("X-Forwarded-For", "49.36.0.1")
                .header("X-Real-IP", "49.36.0.1")
                .header("Accept-Language", "en-IN,en;q=0.9")
                .header("Cookie", "explicit_content=1")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
                val bodyStr = response.body?.string() ?: throw IllegalStateException("Empty body")
                val body = json.decodeFromString<RawSearchResponse>(bodyStr)
                body.results.map { mapRawToSaavnSong(it) }
            }
        }
    }

    fun selectBestUrl(urls: List<SaavnDownloadUrl>, quality: String = "320kbps"): String? {
        val filtered = urls.filter { it.url.isNotBlank() }
        if (filtered.isEmpty()) return null

        return filtered.firstOrNull { it.quality.equals(quality, ignoreCase = true) }?.url
            ?: filtered.firstOrNull { it.quality.equals("320kbps", ignoreCase = true) }?.url
            ?: filtered.firstOrNull { it.quality.equals("160kbps", ignoreCase = true) }?.url
            ?: filtered.firstOrNull()?.url
    }
}
