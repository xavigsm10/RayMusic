package com.mrtdk.liquid_glass.utils

import android.util.Base64
import android.util.Log
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class LyricsFetchResult(
    val lyrics: SyncedLyrics?,
    val providerName: String,
    val syncType: String = "line" // "syllable", "word", "line", "plain"
)

object LyricsProvider {
    // Instant In-Memory Cache (Key: "artist:::title")
    private val lyricsCache = ConcurrentHashMap<String, LyricsFetchResult>()

    fun cleanSearchTerm(term: String): String {
        var cleaned = term
        cleaned = cleaned.replace(Regex("(?i)\\b(feat\\.?|ft\\.?|with)\\b.*"), "")
        cleaned = cleaned.replace(Regex("(?i)\\b(official\\s*video|official\\s*audio|video\\s*oficial|audio\\s*oficial|visualizer|lyric\\s*video|remastered|remaster|deluxe|live)\\b.*"), "")
        cleaned = cleaned.replace(Regex("\\([^\\)]*\\)"), "")
        cleaned = cleaned.replace(Regex("\\[[^\\]]*\\]"), "")
        cleaned = cleaned.replace(Regex("[-–—:_\\s]+$"), "")
        return cleaned.trim().replace(Regex("\\s+"), " ").ifEmpty { term }
    }

    fun getCacheKey(artist: String, title: String): String {
        return "${cleanSearchTerm(artist).lowercase()}:::${cleanSearchTerm(title).lowercase()}"
    }

    fun getCachedLyrics(artist: String, title: String): LyricsFetchResult? {
        val key = getCacheKey(artist, title)
        return lyricsCache[key]
    }

    fun putCachedLyrics(artist: String, title: String, result: LyricsFetchResult) {
        val key = getCacheKey(artist, title)
        lyricsCache[key] = result
    }

    fun parseSyncedLyrics(text: String): SyncedLyrics? {
        return try {
            AutoParser().parse(text)
        } catch (e: Exception) {
            null
        }
    }

    private fun detectSyncType(syncedLyrics: SyncedLyrics?): String {
        if (syncedLyrics == null) return "plain"
        val lines = syncedLyrics.lines
        if (lines.isEmpty()) return "plain"
        val hasKaraoke = lines.any { it is KaraokeLine }
        if (hasKaraoke) return "syllable"
        val hasTiming = lines.any { it.start > 0 }
        return if (hasTiming) "line" else "plain"
    }

    // 1. Better Lyrics (bLyrics / TTML RichSync)
    suspend fun fetchBetterLyrics(title: String, artist: String, duration: Int = -1): LyricsFetchResult? = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = cleanSearchTerm(title)
            val cleanArtist = cleanSearchTerm(artist)
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            var urlString = "https://lyrics-api.boidu.dev/getLyrics?s=$encodedTitle&a=$encodedArtist"
            if (duration > 0) urlString += "&d=$duration"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2500
                readTimeout = 2500
                setRequestProperty("User-Agent", "BetterLyrics/2.1.0 RayMusic/1.0")
            }

            if (connection.responseCode == 200) {
                val response = InputStreamReader(connection.inputStream).readText()
                val json = JSONObject(response)
                val ttml = json.optString("ttml", "")
                if (ttml.isNotEmpty()) {
                    val parsed = AutoParser().parse(ttml)
                    if (parsed != null && parsed.lines.isNotEmpty()) {
                        val res = LyricsFetchResult(parsed, "Better Lyrics", detectSyncType(parsed))
                        putCachedLyrics(artist, title, res)
                        return@withContext res
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsProvider", "Error fetching BetterLyrics: ${e.message}")
        }
        null
    }

    // 2. Unison (Community Lyrics via Unison API)
    suspend fun fetchUnisonLyrics(videoId: String, title: String, artist: String, duration: Int = -1, album: String? = null): LyricsFetchResult? = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = cleanSearchTerm(title)
            val cleanArtist = cleanSearchTerm(artist)
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            var urlString = "https://unison.boidu.dev/lyrics?v=$videoId&song=$encodedTitle&artist=$encodedArtist"
            if (duration > 0) urlString += "&duration=$duration"
            if (!album.isNullOrBlank()) urlString += "&album=${URLEncoder.encode(cleanSearchTerm(album), "UTF-8")}"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2500
                readTimeout = 2500
                setRequestProperty("User-Agent", "BetterLyrics/2.1.0 RayMusic/1.0")
            }

            if (connection.responseCode == 200) {
                val response = InputStreamReader(connection.inputStream).readText()
                val json = JSONObject(response)
                val data = json.optJSONObject("data")
                if (data != null) {
                    val rawLyrics = data.optString("lyrics", "")
                    val syncType = data.optString("syncType", "linesync")
                    if (rawLyrics.isNotEmpty()) {
                        val parsed = AutoParser().parse(rawLyrics)
                        if (parsed != null && parsed.lines.isNotEmpty()) {
                            val resolvedSync = when (syncType) {
                                "richsync" -> "syllable"
                                "linesync" -> "line"
                                else -> detectSyncType(parsed)
                            }
                            val res = LyricsFetchResult(parsed, "Unison", resolvedSync)
                            putCachedLyrics(artist, title, res)
                            return@withContext res
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsProvider", "Error fetching Unison: ${e.message}")
        }
        null
    }

    // 3. BiniLyrics / LyricsPlus
    suspend fun fetchBiniLyrics(title: String, artist: String, duration: Int = -1): LyricsFetchResult? = withContext(Dispatchers.IO) {
        val baseUrls = listOf(
            "https://lyricsplus.binimum.org",
            "https://lyricsplus.atomix.one",
            "https://lyricsplus-seven.vercel.app"
        )
        val cleanTitle = cleanSearchTerm(title)
        val cleanArtist = cleanSearchTerm(artist)
        val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
        val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
        val dur = if (duration > 0) duration else -1

        for (baseUrl in baseUrls) {
            try {
                val urlString = "$baseUrl/v2/lyrics/get?title=$encodedTitle&artist=$encodedArtist&duration=$dur&source=apple,lyricsplus,musixmatch,spotify,musixmatch-word"
                val url = URL(urlString)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 2000
                    readTimeout = 2000
                    setRequestProperty("User-Agent", "BetterLyrics/2.1.0 RayMusic/1.0")
                }

                if (connection.responseCode == 200) {
                    val response = InputStreamReader(connection.inputStream).readText()
                    val json = JSONObject(response)
                    val lyricsArray = json.optJSONArray("lyrics")
                    if (lyricsArray != null && lyricsArray.length() > 0) {
                        val lines = mutableListOf<ISyncedLine>()
                        for (i in 0 until lyricsArray.length()) {
                            val lineObj = lyricsArray.getJSONObject(i)
                            val time = lineObj.optLong("time", -1L).toInt()
                            val text = lineObj.optString("text", "")
                            if (text.isNotBlank()) {
                                val nextTime = if (i < lyricsArray.length() - 1) lyricsArray.getJSONObject(i + 1).optLong("time", -1L).toInt() else -1
                                val endTime = if (nextTime > time) nextTime else time + 4000
                                lines.add(SyncedLine(text, null, time, endTime))
                            }
                        }
                        if (lines.isNotEmpty()) {
                            val parsed = SyncedLyrics(lines = lines)
                            val res = LyricsFetchResult(parsed, "BiniLyrics", "line")
                            putCachedLyrics(artist, title, res)
                            return@withContext res
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to next mirror
            }
        }
        null
    }

    // 4. LRCLib (Synced + Plain)
    suspend fun fetchLRCLib(title: String, artist: String, duration: Int = -1): LyricsFetchResult? = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = cleanSearchTerm(title)
            val cleanArtist = cleanSearchTerm(artist)
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            var urlString = "https://lrclib.net/api/get?track_name=$encodedTitle&artist_name=$encodedArtist"
            if (duration > 0) urlString += "&duration=$duration"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                setRequestProperty("User-Agent", "BetterLyrics/2.1.0 RayMusic/1.0")
            }

            if (connection.responseCode == 200) {
                val response = InputStreamReader(connection.inputStream).readText()
                val json = JSONObject(response)
                val syncedLyrics = json.optString("syncedLyrics", "")
                if (syncedLyrics.isNotEmpty()) {
                    val parsed = AutoParser().parse(syncedLyrics)
                    if (parsed != null && parsed.lines.isNotEmpty()) {
                        val res = LyricsFetchResult(parsed, "LRCLIB", "line")
                        putCachedLyrics(artist, title, res)
                        return@withContext res
                    }
                }
                val plainLyrics = json.optString("plainLyrics", "")
                if (plainLyrics.isNotEmpty()) {
                    val parsed = AutoParser().parse(plainLyrics)
                    if (parsed != null && parsed.lines.isNotEmpty()) {
                        val res = LyricsFetchResult(parsed, "LRCLIB", "plain")
                        putCachedLyrics(artist, title, res)
                        return@withContext res
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsProvider", "Error fetching LRCLIB: ${e.message}")
        }
        null
    }

    // 5. KuGou
    suspend fun fetchKuGouLyrics(title: String, artist: String): LyricsFetchResult? = withContext(Dispatchers.IO) {
        try {
            val keyword = "${cleanSearchTerm(title)} - ${cleanSearchTerm(artist)}"
            val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
            val searchUrl = URL("https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=$encodedKeyword")
            val searchConn = (searchUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
            }

            if (searchConn.responseCode == 200) {
                val searchResponse = InputStreamReader(searchConn.inputStream).readText()
                val searchJson = JSONObject(searchResponse)
                val candidates = searchJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val id = firstCandidate.getString("id")
                    val accessKey = firstCandidate.getString("accesskey")

                    val downloadUrl = URL("https://lyrics.kugou.com/download?fmt=lrc&charset=utf8&client=pc&ver=1&id=$id&accesskey=$accessKey")
                    val downloadConn = (downloadUrl.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 2000
                        readTimeout = 2000
                    }

                    if (downloadConn.responseCode == 200) {
                        val downloadResponse = InputStreamReader(downloadConn.inputStream).readText()
                        val downloadJson = JSONObject(downloadResponse)
                        val contentBase64 = downloadJson.optString("content", "")
                        if (contentBase64.isNotEmpty()) {
                            val decodedBytes = Base64.decode(contentBase64, Base64.DEFAULT)
                            val decodedLyrics = String(decodedBytes, Charsets.UTF_8)
                            val parsed = AutoParser().parse(decodedLyrics)
                            if (parsed != null && parsed.lines.isNotEmpty()) {
                                val res = LyricsFetchResult(parsed, "KuGou", "line")
                                putCachedLyrics(artist, title, res)
                                return@withContext res
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsProvider", "Error fetching KuGou: ${e.message}")
        }
        null
    }

    // 6. YouTube Captions (Synced Subtitles)
    suspend fun fetchYouTubeCaptions(videoId: String): LyricsFetchResult? = withContext(Dispatchers.IO) {
        try {
            val lyricsStr = com.echo.innertube.YouTube.transcript(videoId).getOrNull()
            if (!lyricsStr.isNullOrEmpty()) {
                val parsed = AutoParser().parse(lyricsStr)
                if (parsed != null && parsed.lines.isNotEmpty()) {
                    val res = LyricsFetchResult(parsed, "YouTube Captions", "line")
                    return@withContext res
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsProvider", "Error fetching YouTube captions: ${e.message}")
        }
        null
    }

    // 7. YouTube Music (Plain)
    suspend fun fetchYouTubeLyrics(videoId: String): LyricsFetchResult? = withContext(Dispatchers.IO) {
        try {
            val nextResult = com.echo.innertube.YouTube.next(com.echo.innertube.models.WatchEndpoint(videoId = videoId)).getOrNull()
            val lyricsEndpoint = nextResult?.lyricsEndpoint
            if (lyricsEndpoint != null) {
                val lyricsStr = com.echo.innertube.YouTube.lyrics(lyricsEndpoint).getOrNull()
                if (!lyricsStr.isNullOrEmpty()) {
                    val parsed = AutoParser().parse(lyricsStr)
                    if (parsed != null && parsed.lines.isNotEmpty()) {
                        val res = LyricsFetchResult(parsed, "YouTube Music", "plain")
                        return@withContext res
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsProvider", "Error fetching YouTube lyrics: ${e.message}")
        }
        null
    }

    // 8. SimpMusic
    suspend fun fetchSimpMusicLyrics(title: String, artist: String): LyricsFetchResult? = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("${cleanSearchTerm(title)} ${cleanSearchTerm(artist)}", "UTF-8")
            val searchUrl = URL("https://api-lyrics.simpmusic.org/search?q=$query&limit=1")
            val searchConn = (searchUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
            }

            if (searchConn.responseCode == 200) {
                val searchResponse = InputStreamReader(searchConn.inputStream).readText()
                val searchJson = JSONObject(searchResponse)
                val dataArray = searchJson.optJSONArray("data")
                if (dataArray != null && dataArray.length() > 0) {
                    val videoId = dataArray.getJSONObject(0).optString("videoId")
                    if (videoId.isNotEmpty()) {
                        val lyricsUrl = URL("https://api-lyrics.simpmusic.org/lyrics?id=$videoId")
                        val lyricsConn = (lyricsUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 2000
                            readTimeout = 2000
                        }
                        if (lyricsConn.responseCode == 200) {
                            val lyricsResponse = InputStreamReader(lyricsConn.inputStream).readText()
                            val lyricsJson = JSONObject(lyricsResponse)
                            val lyricsDataArray = lyricsJson.optJSONArray("data")
                            if (lyricsDataArray != null && lyricsDataArray.length() > 0) {
                                val lyricItem = lyricsDataArray.getJSONObject(0)
                                val syncedLyrics = lyricItem.optString("syncedLyrics", "")
                                if (syncedLyrics.isNotEmpty()) {
                                    val parsed = AutoParser().parse(syncedLyrics)
                                    if (parsed != null) return@withContext LyricsFetchResult(parsed, "SimpMusic", "line")
                                }
                                val plainLyric = lyricItem.optString("plainLyric", "")
                                if (plainLyric.isNotEmpty()) {
                                    val parsed = AutoParser().parse(plainLyric)
                                    if (parsed != null) return@withContext LyricsFetchResult(parsed, "SimpMusic", "plain")
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsProvider", "Error fetching SimpMusic: ${e.message}")
        }
        null
    }

    // Unified Automatic Fetch: Parallel High-Speed Resolution (Glassy/BetterLyrics standard)
    suspend fun fetchAutoLyrics(videoId: String, title: String, artist: String, duration: Int = -1, album: String? = null): LyricsFetchResult? = withContext(Dispatchers.IO) {
        // 0. Cache Check (0ms instantaneous)
        getCachedLyrics(artist, title)?.let { return@withContext it }

        val available = fetchAllAvailableProviders(videoId, title, artist, duration, album)
        val best = available.firstOrNull { it.syncType == "syllable" }
            ?: available.firstOrNull { it.syncType == "word" }
            ?: available.firstOrNull { it.syncType == "line" }
            ?: available.firstOrNull()

        if (best != null) {
            putCachedLyrics(artist, title, best)
        }
        best
    }

    // Unified Automatic Fetch: Returns all available distributors for the floating dock (up to 8 distributors)
    suspend fun fetchAllAvailableProviders(videoId: String, title: String, artist: String, duration: Int = -1, album: String? = null): List<LyricsFetchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LyricsFetchResult>()

        coroutineScope {
            val blDeferred = async { fetchBetterLyrics(title, artist, duration) }
            val unisonDeferred = async { fetchUnisonLyrics(videoId, title, artist, duration, album) }
            val biniDeferred = async { fetchBiniLyrics(title, artist, duration) }
            val lrcDeferred = async { fetchLRCLib(title, artist, duration) }
            val kugouDeferred = async { fetchKuGouLyrics(title, artist) }
            val ytCaptionsDeferred = async { if (videoId.isNotBlank()) fetchYouTubeCaptions(videoId) else null }
            val ytMusicDeferred = async { if (videoId.isNotBlank()) fetchYouTubeLyrics(videoId) else null }

            blDeferred.await()?.let { 
                results.add(it)
                // Add Portato variant if syllable sync is available
                if (it.syncType == "syllable") {
                    results.add(LyricsFetchResult(it.lyrics, "Better Lyrics Portato", "word"))
                    results.add(LyricsFetchResult(it.lyrics, "Better Lyrics Legato", "line"))
                }
            }
            biniDeferred.await()?.let { 
                results.add(it)
                results.add(LyricsFetchResult(it.lyrics, "Musixmatch", "line"))
            }
            unisonDeferred.await()?.let { results.add(it) }
            lrcDeferred.await()?.let { 
                results.add(it)
                if (it.syncType != "plain") {
                    results.add(LyricsFetchResult(it.lyrics, "LRCLib", "plain"))
                }
            }
            kugouDeferred.await()?.let { results.add(it) }
            ytCaptionsDeferred.await()?.let { results.add(it) }
            ytMusicDeferred.await()?.let { results.add(LyricsFetchResult(it.lyrics, "YouTube", "plain")) }
        }

        if (results.isEmpty()) {
            fetchAutoLyrics(videoId, title, artist, duration, album)?.let { results.add(it) }
        }

        results.distinctBy { "${it.providerName}:::${it.syncType}" }
    }

    // Backwards compatibility aliases
    suspend fun fetchLyrics(title: String, artist: String): SyncedLyrics? {
        return fetchLRCLib(title, artist)?.lyrics
    }

    suspend fun fetchLyricsPlus(title: String, artist: String, duration: Int = -1): SyncedLyrics? {
        return fetchBiniLyrics(title, artist, duration)?.lyrics
    }

    suspend fun fetchYouTubeSubtitleLyrics(id: String): SyncedLyrics? {
        return fetchYouTubeCaptions(id)?.lyrics
    }
}
