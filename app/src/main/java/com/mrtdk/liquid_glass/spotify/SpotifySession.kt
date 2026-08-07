package com.mrtdk.liquid_glass.spotify

import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SpotifySession {
    private const val KEY_SP_DC = "spotify_sp_dc"
    private const val KEY_ACCESS_TOKEN = "spotify_access_token"
    private const val KEY_TOKEN_EXPIRATION = "spotify_token_expiration"
    private const val KEY_USER_ID = "spotify_user_id"
    private const val KEY_USER_NAME = "spotify_user_name"

    var spDc: String = ""
        private set

    var accessToken: String = ""
        private set

    var tokenExpirationMs: Long = 0L
        private set

    var userId: String = ""
        private set

    var userName: String = ""
        private set

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun init() {
        spDc = LibraryManager.getString(KEY_SP_DC, "") ?: ""
        accessToken = LibraryManager.getString(KEY_ACCESS_TOKEN, "") ?: ""
        tokenExpirationMs = LibraryManager.getString(KEY_TOKEN_EXPIRATION, "0")?.toLongOrNull() ?: 0L
        userId = LibraryManager.getString(KEY_USER_ID, "") ?: ""
        userName = LibraryManager.getString(KEY_USER_NAME, "") ?: ""

        _isLoggedIn.value = spDc.isNotBlank()
    }

    suspend fun saveSession(
        spDcCookie: String,
        token: SpotifyInternalToken,
        displayName: String = "",
        uid: String = ""
    ) {
        if (spDcCookie.isNotBlank()) spDc = spDcCookie
        accessToken = token.accessToken
        tokenExpirationMs = token.accessTokenExpirationTimestampMs
        if (displayName.isNotBlank()) userName = displayName
        if (uid.isNotBlank()) userId = uid

        LibraryManager.saveString(KEY_SP_DC, spDc)
        LibraryManager.saveString(KEY_ACCESS_TOKEN, accessToken)
        LibraryManager.saveString(KEY_TOKEN_EXPIRATION, tokenExpirationMs.toString())
        LibraryManager.saveString(KEY_USER_ID, userId)
        LibraryManager.saveString(KEY_USER_NAME, userName)

        _isLoggedIn.value = spDc.isNotBlank()
    }

    fun isTokenExpired(): Boolean {
        if (tokenExpirationMs == 0L) return true
        return System.currentTimeMillis() >= (tokenExpirationMs - 60_000L) // Refresh 1 minute before expiration
    }

    suspend fun ensureValidToken(): Boolean {
        if (accessToken.isNotBlank() && !isTokenExpired()) {
            return true
        }

        return try {
            val result = SpotifyAuth.fetchAccessToken(spDc)
            result.getOrNull()?.let { token ->
                accessToken = token.accessToken
                tokenExpirationMs = token.accessTokenExpirationTimestampMs
                LibraryManager.saveString(KEY_ACCESS_TOKEN, accessToken)
                LibraryManager.saveString(KEY_TOKEN_EXPIRATION, tokenExpirationMs.toString())
                if (spDc.isNotBlank() && !token.isAnonymous) {
                    _isLoggedIn.value = true
                }
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun logout() {
        spDc = ""
        accessToken = ""
        tokenExpirationMs = 0L
        userId = ""
        userName = ""

        LibraryManager.saveString(KEY_SP_DC, "")
        LibraryManager.saveString(KEY_ACCESS_TOKEN, "")
        LibraryManager.saveString(KEY_TOKEN_EXPIRATION, "0")
        LibraryManager.saveString(KEY_USER_ID, "")
        LibraryManager.saveString(KEY_USER_NAME, "")

        _isLoggedIn.value = false
    }
}
