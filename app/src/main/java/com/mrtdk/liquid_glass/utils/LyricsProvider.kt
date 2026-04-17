package com.mrtdk.liquid_glass.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(val timeMs: Long, val text: String)

object LyricsProvider {
    suspend fun fetchLyrics(title: String, artist: String): List<LyricLine>? = withContext(Dispatchers.IO) {
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
                    return@withContext parseSyncedLyrics(syncedLyrics)
                }
                
                val plainLyrics = json.optString("plainLyrics", "")
                if (plainLyrics.isNotEmpty()) {
                    // Si no hay sincronizada, devolvemos renglones como lista pero sin tiempo (0ms)
                    return@withContext plainLyrics.split("\n").map { LyricLine(-1L, it.trim()) }
                }
            }
        } catch (e: Exception) {
            Log.e("LyricsProvider", "Error fetching lyrics", e)
        }
        null
    }

    private fun parseSyncedLyrics(syncedLyrics: String): List<LyricLine> {
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})] (.*)")
        val lines = mutableListOf<LyricLine>()
        
        syncedLyrics.lineSequence().forEach { line ->
            val matchResult = regex.find(line)
            if (matchResult != null) {
                val (minStr, secStr, msStr, text) = matchResult.destructured
                val msParsed = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                val timeMs = (minStr.toLong() * 60 * 1000) + (secStr.toLong() * 1000) + msParsed
                lines.add(LyricLine(timeMs, text.trim()))
            } else {
                // If it doesn't match the timestamp, it might be a blank line or metadata
                if (line.isNotBlank() && !line.startsWith("[")) {
                     lines.add(LyricLine(-1L, line.trim()))
                }
            }
        }
        return lines.sortedBy { if (it.timeMs == -1L) Long.MAX_VALUE else it.timeMs }
    }
}
