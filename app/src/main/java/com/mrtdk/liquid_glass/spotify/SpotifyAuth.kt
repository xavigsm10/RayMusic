package com.mrtdk.liquid_glass.spotify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor

object SpotifyAuth {
    private const val TOKEN_URL = "https://open.spotify.com/api/token"
    private const val SERVER_TIME_URL = "https://open.spotify.com/api/server-time"
    private const val NUANCE_GIST_URL =
        "https://api.github.com/gists/22ed9c6ba463899e933427f7de1f0eef"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    const val LOGIN_URL =
        "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com%2F" +
            "&method=password&allow_password=1&allow_signup=1"

    const val SIGNUP_URL = "https://www.spotify.com/signup"

    private data class Nuance(val s: String, val v: Int)

    suspend fun fetchAccessToken(
        spDc: String = "",
        spKey: String = "",
    ): Result<SpotifyInternalToken> = runCatching {
        val nuance = fetchNuance()
        val serverTimeSec = fetchServerTime()
        val totp = generateTotp(nuance.s, serverTimeSec)

        val tokenUrl = buildString {
            append(TOKEN_URL)
            append("?reason=transport")
            append("&productType=web-player")
            append("&totp=$totp")
            append("&totpServer=$totp")
            append("&totpVer=${nuance.v}")
        }

        val headers = mutableMapOf<String, String>()
        if (spDc.isNotBlank()) {
            val cookieHeader = buildString {
                append("sp_dc=$spDc")
                if (spKey.isNotEmpty()) {
                    append("; sp_key=$spKey")
                }
            }
            headers["Cookie"] = cookieHeader
        }

        val body = withContext(Dispatchers.IO) {
            httpGet(tokenUrl, headers)
        }

        val json = JSONObject(body)
        val accessToken = json.optString("accessToken", "")
        val expMs = json.optLong("accessTokenExpirationTimestampMs", 0L)
        val isAnon = json.optBoolean("isAnonymous", false)
        val clientId = json.optString("clientId", "")

        val token = SpotifyInternalToken(
            accessToken = accessToken,
            accessTokenExpirationTimestampMs = expMs,
            isAnonymous = isAnon,
            clientId = clientId
        )

        if (token.accessToken.isBlank()) {
            throw Exception("Received empty token from Spotify")
        }

        token
    }

    private suspend fun fetchNuance(): Nuance = withContext(Dispatchers.IO) {
        val body = try {
            httpGet(NUANCE_GIST_URL, emptyMap())
        } catch (e: Exception) {
            throw Exception("Failed to fetch TOTP secret from gist: ${e.message}")
        }
        val json = JSONObject(body)
        val files = json.optJSONObject("files") ?: throw Exception("Gist has no files")
        val fileKeys = files.keys()
        val firstKey = if (fileKeys.hasNext()) fileKeys.next() else throw Exception("Gist has no files")
        val contentStr = files.optJSONObject(firstKey)?.optString("content", "") ?: throw Exception("Gist content empty")

        val array = JSONArray(contentStr)
        var maxV = -1
        var maxS = ""
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val v = item.optInt("v", 0)
            val s = item.optString("s", "")
            if (v > maxV) {
                maxV = v
                maxS = s
            }
        }

        if (maxS.isEmpty()) {
            throw Exception("No nuance data found in gist")
        }

        Nuance(maxS, maxV)
    }

    private suspend fun fetchServerTime(): Long = withContext(Dispatchers.IO) {
        val body = try {
            httpGet(SERVER_TIME_URL, emptyMap())
        } catch (e: Exception) {
            throw Exception("Failed to fetch Spotify server time: ${e.message}")
        }
        val json = JSONObject(body)
        json.optLong("serverTime", System.currentTimeMillis() / 1000)
    }

    private fun generateTotp(secret: String, serverTimeSec: Long): String {
        val key = base32Decode(secret)
        val interval = 30L
        val timeStep = floor(serverTimeSec.toDouble() / interval).toLong()

        val timeBytes = ByteArray(8)
        var value = timeStep
        for (i in 7 downTo 0) {
            timeBytes[i] = (value and 0xFF).toByte()
            value = value shr 8
        }

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(timeBytes)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val code = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        val otp = code % 1_000_000
        return otp.toString().padStart(6, '0')
    }

    private fun base32Decode(input: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleaned = input.uppercase().replace("=", "")

        val output = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (c in cleaned) {
            val value = alphabet.indexOf(c)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }

        return output.toByteArray()
    }

    private fun httpGet(urlString: String, extraHeaders: Map<String, String>): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")
            connection.setRequestProperty("Accept-Language", "en")
            for ((key, value) in extraHeaders) {
                connection.setRequestProperty(key, value)
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw Exception("HTTP $responseCode: $errorBody")
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
