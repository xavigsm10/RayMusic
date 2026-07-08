package com.mrtdk.liquid_glass.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser

object LyricsProvider {
    fun parseSyncedLyrics(text: String): SyncedLyrics? {
        return AutoParser().parse(text)
    }

    suspend fun fetchLyrics(title: String, artist: String): SyncedLyrics? = withContext(Dispatchers.IO) {
        try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val url = URL("https://lrclib.net/api/get?track_name=$encodedTitle&artist_name=$encodedArtist")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "LiquidGlassMusic/1.0.0")

            if (connection.responseCode == 200) {
                val response = InputStreamReader(connection.inputStream).readText()
                val json = JSONObject(response)
                val syncedLyrics = json.optString("syncedLyrics", "")
                
                if (syncedLyrics.isNotEmpty()) {
                    return@withContext AutoParser().parse(syncedLyrics)
                }
                
                val plainLyrics = json.optString("plainLyrics", "")
                if (plainLyrics.isNotEmpty()) {
                    return@withContext AutoParser().parse(plainLyrics)
                }
            }
        } catch (e: Exception) {
            Log.e("LyricsProvider", "Error fetching lyrics", e)
        }
        null
    }

    suspend fun fetchKuGouLyrics(title: String, artist: String): SyncedLyrics? = withContext(Dispatchers.IO) {
        try {
            val keyword = "$title - $artist"
            val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
            val searchUrl = URL("https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=$encodedKeyword")
            val searchConn = searchUrl.openConnection() as HttpURLConnection
            searchConn.requestMethod = "GET"
            
            if (searchConn.responseCode == 200) {
                val searchResponse = InputStreamReader(searchConn.inputStream).readText()
                val searchJson = JSONObject(searchResponse)
                val candidates = searchJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val id = firstCandidate.getString("id")
                    val accessKey = firstCandidate.getString("accesskey")
                    
                    val downloadUrl = URL("https://lyrics.kugou.com/download?fmt=lrc&charset=utf8&client=pc&ver=1&id=$id&accesskey=$accessKey")
                    val downloadConn = downloadUrl.openConnection() as HttpURLConnection
                    downloadConn.requestMethod = "GET"
                    
                    if (downloadConn.responseCode == 200) {
                        val downloadResponse = InputStreamReader(downloadConn.inputStream).readText()
                        val downloadJson = JSONObject(downloadResponse)
                        val contentBase64 = downloadJson.optString("content", "")
                        if (contentBase64.isNotEmpty()) {
                            val decodedBytes = Base64.decode(contentBase64, Base64.DEFAULT)
                            val decodedLyrics = String(decodedBytes, Charsets.UTF_8)
                            return@withContext AutoParser().parse(decodedLyrics)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LyricsProvider", "Error fetching KuGou lyrics", e)
        }
        null
    }

    suspend fun fetchBetterLyrics(title: String, artist: String, duration: Int = -1): SyncedLyrics? = withContext(Dispatchers.IO) {
        try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            var urlString = "https://lyrics-api.boidu.dev/getLyrics?s=$encodedTitle&a=$encodedArtist"
            if (duration > 0) urlString += "&d=$duration"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val response = InputStreamReader(connection.inputStream).readText()
                val json = JSONObject(response)
                val ttml = json.optString("ttml", "")
                if (ttml.isNotEmpty()) {
                    return@withContext AutoParser().parse(ttml)
                }
            }
        } catch (e: Exception) {
            Log.e("LyricsProvider", "Error fetching BetterLyrics", e)
        }
        null
    }

    suspend fun fetchLyricsPlus(title: String, artist: String, duration: Int = -1): SyncedLyrics? = withContext(Dispatchers.IO) {
        val baseUrls = listOf(
            "https://lyricsplus.binimum.org",
            "https://lyricsplus.atomix.one",
            "https://lyricsplus-seven.vercel.app"
        )
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val encodedArtist = URLEncoder.encode(artist, "UTF-8")
        val dur = if (duration > 0) duration else -1
        
        for (baseUrl in baseUrls) {
            try {
                val urlString = "$baseUrl/v2/lyrics/get?title=$encodedTitle&artist=$encodedArtist&duration=$dur&source=apple,lyricsplus,musixmatch,spotify,musixmatch-word"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                
                if (connection.responseCode == 200) {
                    val response = InputStreamReader(connection.inputStream).readText()
                    val json = JSONObject(response)
                    val lyricsArray = json.optJSONArray("lyrics")
                    if (lyricsArray != null && lyricsArray.length() > 0) {
                        val lines = mutableListOf<com.mocharealm.accompanist.lyrics.core.model.ISyncedLine>()
                        for (i in 0 until lyricsArray.length()) {
                            val lineObj = lyricsArray.getJSONObject(i)
                            val time = lineObj.optLong("time", -1L).toInt()
                            val text = lineObj.optString("text", "")
                            if (text.isNotBlank()) {
                                val nextTime = if (i < lyricsArray.length() - 1) lyricsArray.getJSONObject(i + 1).optLong("time", -1L).toInt() else -1
                                val endTime = if (nextTime > time) nextTime else time + 4000
                                lines.add(com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine(text, null, time, endTime))
                            }
                        }
                        if (lines.isNotEmpty()) return@withContext SyncedLyrics(lines = lines)
                    }
                }
            } catch (e: Exception) {
                // Continue to next URL
            }
        }
        null
    }

    suspend fun fetchSimpMusicLyrics(title: String, artist: String): SyncedLyrics? = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("$title $artist", "UTF-8")
            val searchUrl = URL("https://api-lyrics.simpmusic.org/search?q=$query&limit=1")
            val searchConn = searchUrl.openConnection() as HttpURLConnection
            searchConn.requestMethod = "GET"
            
            if (searchConn.responseCode == 200) {
                val searchResponse = InputStreamReader(searchConn.inputStream).readText()
                val searchJson = JSONObject(searchResponse)
                val dataArray = searchJson.optJSONArray("data")
                if (dataArray != null && dataArray.length() > 0) {
                    val videoId = dataArray.getJSONObject(0).optString("videoId")
                    if (videoId.isNotEmpty()) {
                        val lyricsUrl = URL("https://api-lyrics.simpmusic.org/lyrics?id=$videoId")
                        val lyricsConn = lyricsUrl.openConnection() as HttpURLConnection
                        lyricsConn.requestMethod = "GET"
                        if (lyricsConn.responseCode == 200) {
                            val lyricsResponse = InputStreamReader(lyricsConn.inputStream).readText()
                            val lyricsJson = JSONObject(lyricsResponse)
                            val lyricsDataArray = lyricsJson.optJSONArray("data")
                            if (lyricsDataArray != null && lyricsDataArray.length() > 0) {
                                val lyricItem = lyricsDataArray.getJSONObject(0)
                                val syncedLyrics = lyricItem.optString("syncedLyrics", "")
                                if (syncedLyrics.isNotEmpty()) {
                                    return@withContext AutoParser().parse(syncedLyrics)
                                }
                                val plainLyric = lyricItem.optString("plainLyric", "")
                                if (plainLyric.isNotEmpty()) {
                                    return@withContext AutoParser().parse(plainLyric)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LyricsProvider", "Error fetching SimpMusic lyrics", e)
        }
        null
    }

    suspend fun fetchYouTubeLyrics(id: String): SyncedLyrics? = withContext(Dispatchers.IO) {
        try {
            val nextResult = com.echo.innertube.YouTube.next(com.echo.innertube.models.WatchEndpoint(videoId = id)).getOrNull()
            val lyricsEndpoint = nextResult?.lyricsEndpoint
            if (lyricsEndpoint != null) {
                val lyricsStr = com.echo.innertube.YouTube.lyrics(lyricsEndpoint).getOrNull()
                if (!lyricsStr.isNullOrEmpty()) {
                    return@withContext AutoParser().parse(lyricsStr)
                }
            }
        } catch (e: Exception) {
            Log.e("LyricsProvider", "Error fetching YouTube lyrics", e)
        }
        null
    }

    suspend fun fetchYouTubeSubtitleLyrics(id: String): SyncedLyrics? = withContext(Dispatchers.IO) {
        try {
            val lyricsStr = com.echo.innertube.YouTube.transcript(id).getOrNull()
            if (!lyricsStr.isNullOrEmpty()) {
                return@withContext AutoParser().parse(lyricsStr)
            }
        } catch (e: Exception) {
            Log.e("LyricsProvider", "Error fetching YouTube Subtitle lyrics", e)
        }
        null
    }
}
