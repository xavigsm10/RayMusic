package com.mrtdk.liquid_glass.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.spotify.Spotify
import com.mrtdk.liquid_glass.spotify.SpotifyAuth
import com.mrtdk.liquid_glass.spotify.SpotifySession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Spotify login dialog — replicates Spotui's finishLogin() pattern:
 *   1. Poll for sp_dc cookie
 *   2. Stop WebView
 *   3. fetchAccessToken (with retries)
 *   4. Save session → call onSuccess
 *   5. Profile name + library sync happen asynchronously AFTER dialog closes
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SpotifyLoginDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    val tokenFetchStarted = remember { AtomicBoolean(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun extractCookie(cookieName: String): String? {
        val cookieManager = CookieManager.getInstance()
        val domains = listOf("https://open.spotify.com", "https://accounts.spotify.com", "https://spotify.com")
        for (domain in domains) {
            val cookies = cookieManager.getCookie(domain) ?: continue
            val match = cookies.split(";")
                .mapNotNull {
                    val parts = it.trim().split("=", limit = 2)
                    if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
                }
                .firstOrNull { it.first == cookieName && it.second.isNotBlank() }
                ?.second
            if (!match.isNullOrBlank()) return match
        }
        return null
    }

    // Poll for sp_dc cookie
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (tokenFetchStarted.get()) continue
            val spDc = extractCookie("sp_dc")
            if (!spDc.isNullOrBlank() && tokenFetchStarted.compareAndSet(false, true)) {
                isProcessing = true
                hasError = false
                statusMessage = "Conectando..."
                webViewRef?.stopLoading()

                scope.launch(Dispatchers.IO) {
                    var lastError: Throwable? = null

                    repeat(3) { attempt ->
                        val result = SpotifyAuth.fetchAccessToken(spDc)
                        result.onSuccess { token ->
                            SpotifySession.saveSession(spDc, token, "", "")

                            withContext(Dispatchers.Main) { statusMessage = "¡Sesión iniciada!" }
                            delay(300)
                            withContext(Dispatchers.Main) { onSuccess() }

                            scope.launch(Dispatchers.IO) {
                                try {
                                    val user = Spotify.me().getOrNull()
                                    if (user != null) {
                                        val displayName = user.displayName ?: user.id ?: ""
                                        val uid = user.id
                                        SpotifySession.saveSession(spDc, token, displayName, uid)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            scope.launch(Dispatchers.IO) {
                                try {
                                    LibraryManager.syncSpotifyPlaylists()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            return@launch
                        }.onFailure { e ->
                            lastError = e
                            if (attempt < 2) delay(800)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        statusMessage = "Error: ${lastError?.message ?: "Error desconocido"}"
                        hasError = true
                    }
                    tokenFetchStarted.set(false)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            // Full-screen WebView with a slim top bar
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 40.dp),
                factory = { ctx ->
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)

                    WebView(ctx).apply {
                        webViewRef = this
                        setBackgroundColor(android.graphics.Color.parseColor("#121212"))
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            @Suppress("DEPRECATION")
                            databaseEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(false)
                            cacheMode = WebSettings.LOAD_DEFAULT

                            // Strip "; wv" and "Version/X.X " so Google reCAPTCHA Enterprise and
                            // Spotify do not detect WebView and block storage access / form display
                            val defaultUa = userAgentString
                            userAgentString = defaultUa.replace("; wv", "").replace(Regex("Version/[0-9.]+ "), "")
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoadingPage = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoadingPage = false
                                cookieManager.flush()
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                @Suppress("DEPRECATION")
                                super.onReceivedError(view, errorCode, description, failingUrl)
                                isLoadingPage = false
                            }
                        }

                        loadUrl(SpotifyAuth.LOGIN_URL)
                    }
                }
            )

            // Center loading spinner while web page is loading
            if (isLoadingPage && !isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF1DB954),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Slim top bar: title or status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(40.dp)
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isProcessing) statusMessage.ifBlank { "Conectando..." } else "Iniciar sesión en Spotify",
                        color = if (hasError) Color(0xFFE22134) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
