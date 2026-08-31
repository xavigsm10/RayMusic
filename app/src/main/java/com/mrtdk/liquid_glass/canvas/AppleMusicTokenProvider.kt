package com.mrtdk.liquid_glass.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

internal object AppleMusicTokenProvider {
    private var cachedToken: String? = null
    private val mutex = Mutex()

    private val httpClient get() = CanvasNetworkClient.okHttpClient

    private const val FALLBACK_TOKEN = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IldlYlBsYXlLaWQifQ.eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzc0NDU2MzgyLCJleHAiOjE3ODE3MTM5ODIsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ.4n8qYF4qa18sL1E0G9A3qX35cD8wQ-IJcS9Bh8ZT8JV_yLBtVq46B-9-2ZS3EvWHuw3yK9BYFYAhAdTaDm38vQ"

    suspend fun getToken(): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedToken?.let { return@withLock it }
            try {
                val req = Request.Builder()
                    .url("https://beta.music.apple.com")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val htmlBody = httpClient.newCall(req).execute().use { it.body?.string().orEmpty() }

                val indexJsRegex = Regex("""src="(/assets/index-[^"]+\.js)"""")
                val match = indexJsRegex.find(htmlBody)
                if (match != null) {
                    val indexJsUri = match.groupValues[1]
                    val jsReq = Request.Builder()
                        .url("https://beta.music.apple.com$indexJsUri")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()
                    val indexJsBody = httpClient.newCall(jsReq).execute().use { it.body?.string().orEmpty() }

                    val tokenRegex = Regex("""eyJ[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+""")
                    val tokenMatch = tokenRegex.find(indexJsBody)
                    if (tokenMatch != null) {
                        val token = tokenMatch.value
                        cachedToken = token
                        return@withLock token
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cachedToken = FALLBACK_TOKEN
            FALLBACK_TOKEN
        }
    }
}
