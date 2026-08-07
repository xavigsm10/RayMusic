package com.mrtdk.liquid_glass.playback

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.Executors

object ListenTogetherServers {
    private const val SERVER_JSON_URL = "https://raw.githubusercontent.com/EchoMusicApp/Echo-Music/refs/heads/main/app/server.json"
    private val executor = Executors.newSingleThreadExecutor()

    val defaultServerUrls = listOf(
        "wss://devilmi-vivi-music-listen-together.hf.space",
        "wss://iad1tya-echomusic.hf.space/ws"
    )

    @Volatile
    var currentServerUrl: String = defaultServerUrls[0]
        private set

    init {
        fetchLatestServerUrl()
    }

    fun fetchLatestServerUrl() {
        executor.execute {
            try {
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder().url(SERVER_JSON_URL).build()
                val response = client.newCall(request).execute()
                response.body?.string()?.let { jsonString ->
                    val jsonObject = JSONObject(jsonString)
                    if (jsonObject.has("serverUrl")) {
                        val fetchedUrl = jsonObject.getString("serverUrl")
                        if (fetchedUrl.isNotBlank()) {
                            currentServerUrl = fetchedUrl
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback implicitly retained
            }
        }
    }
}
