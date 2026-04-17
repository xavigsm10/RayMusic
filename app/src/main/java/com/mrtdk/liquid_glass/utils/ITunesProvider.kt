package com.mrtdk.liquid_glass.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ITunesProvider {
    suspend fun fetchHighResArtwork(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            // Buscamos con el nombre de la pista y el artista
            val query = URLEncoder.encode("$title $artist", "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$query&entity=song&limit=1")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val response = InputStreamReader(connection.inputStream).readText()
                val json = JSONObject(response)
                val resultsCount = json.optInt("resultCount", 0)
                
                if (resultsCount > 0) {
                    val firstResult = json.getJSONArray("results").getJSONObject(0)
                    // iTunes normalmente devuelve artworkUrl100 o artworkUrl60
                    val artwork100 = firstResult.optString("artworkUrl100", "")
                    if (artwork100.isNotEmpty()) {
                        // Truco para obtener calidad original o alta resolución (1200x1200bb.jpg o superior)
                        // Apple permite cambiar dimensiones en la URL sustituyendo el 100x100bb por 1200x1200bb
                        return@withContext artwork100.replace("100x100bb.jpg", "1200x1200bb.jpg")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ITunesProvider", "Error fetching iTunes artwork", e)
        }
        null
    }
}
