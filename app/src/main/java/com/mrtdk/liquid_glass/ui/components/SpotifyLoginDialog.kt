package com.mrtdk.liquid_glass.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

private const val CLEAN_MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

private fun isFacebookFlow(url: String?): Boolean {
    val u = url?.lowercase() ?: return false
    return u.contains("facebook.com") || u.contains("fb.com") || u.contains("login/facebook")
}

/**
 * Spotify login dialog — supports all login methods:
 *   - Direct email/password
 *   - Google (clean Mobile Chrome UA bypasses disallowed_useragent)
 *   - Facebook (Desktop UA bypasses broken login_via/app mobile switch)
 *   - Apple & Phone number (scoped Spotify layout CSS prevents pushing off-screen)
 *   - Full multi-window popup support and cookie capture
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
    var canGoBack by remember { mutableStateOf(false) }
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
            val spKey = extractCookie("sp_key") ?: ""
            if (!spDc.isNullOrBlank() && tokenFetchStarted.compareAndSet(false, true)) {
                isProcessing = true
                hasError = false
                statusMessage = "Conectando..."
                webViewRef?.stopLoading()

                scope.launch(Dispatchers.IO) {
                    var lastError: Throwable? = null

                    repeat(3) { attempt ->
                        val result = SpotifyAuth.fetchAccessToken(spDc, spKey)
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

    BackHandler(enabled = canGoBack && !isProcessing) {
        webViewRef?.goBack()
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
                    .padding(top = 44.dp),
                factory = { ctx ->
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.removeAllCookies(null)
                    cookieManager.flush()

                    WebView(ctx).apply {
                        webViewRef = this
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            @Suppress("DEPRECATION")
                            databaseEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(true)
                            userAgentString = CLEAN_MOBILE_USER_AGENT
                        }

                        addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun prepareFacebookLogin() {
                                post {
                                    settings.userAgentString = DESKTOP_USER_AGENT
                                }
                            }
                        }, "AndroidBridge")

                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                transport.webView = view
                                resultMsg.sendToTarget()
                                return true
                            }

                            override fun onCloseWindow(window: WebView?) {
                                if (window?.canGoBack() == true) {
                                    window.goBack()
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            private fun updateUaForUrl(wv: WebView?, targetUrl: String) {
                                val targetUa = if (isFacebookFlow(targetUrl)) DESKTOP_USER_AGENT else CLEAN_MOBILE_USER_AGENT
                                if (wv?.settings?.userAgentString != targetUa) {
                                    wv?.settings?.userAgentString = targetUa
                                }
                            }

                            private fun handleUrlNavigation(view: WebView?, targetUrl: String): Boolean {
                                val isFb = isFacebookFlow(targetUrl)
                                val targetUa = if (isFb) DESKTOP_USER_AGENT else CLEAN_MOBILE_USER_AGENT
                                if (view?.settings?.userAgentString != targetUa) {
                                    view?.settings?.userAgentString = targetUa
                                    if (isFb && (targetUrl.startsWith("http://") || targetUrl.startsWith("https://"))) {
                                        view?.loadUrl(targetUrl)
                                        return true
                                    }
                                }

                                if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                                    return false
                                }

                                try {
                                    val intent = Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME)
                                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                    if (!fallbackUrl.isNullOrBlank()) {
                                        view?.loadUrl(fallbackUrl)
                                        return true
                                    }
                                    if (targetUrl.startsWith("fb://") || targetUrl.startsWith("intent://")) {
                                        // Keep OAuth inside WebView, don't break out to native FB app
                                        return true
                                    }
                                    if (view?.context?.packageManager?.let { intent.resolveActivity(it) } != null) {
                                        view.context.startActivity(intent)
                                        return true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                return true
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                return handleUrlNavigation(view, url)
                            }

                            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                return handleUrlNavigation(view, url ?: return false)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                url?.let { updateUaForUrl(view, it) }
                                isLoadingPage = true
                                canGoBack = view?.canGoBack() ?: false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoadingPage = false
                                canGoBack = view?.canGoBack() ?: false
                                cookieManager.flush()
                                view?.let(::fixSpotifyLoginLayout)
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    isLoadingPage = false
                                }
                            }

                            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                super.onReceivedError(view, errorCode, description, failingUrl)
                                isLoadingPage = false
                            }

                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                if (request?.isForMainFrame == true) {
                                    isLoadingPage = false
                                }
                            }

                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                super.onReceivedSslError(view, handler, error)
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

            // Slim top bar: title or status with back and close buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(44.dp)
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canGoBack && !isProcessing) {
                        IconButton(onClick = { webViewRef?.goBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = Color.White
                            )
                        }
                    }
                    Text(
                        text = if (isProcessing) statusMessage.ifBlank { "Conectando..." } else "Iniciar sesión en Spotify",
                        color = if (hasError) Color(0xFFE22134) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
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

/**
 * Safely adjust Spotify layout without breaking external identity providers (Google, Facebook, Apple).
 */
private fun fixSpotifyLoginLayout(webView: WebView) {
    webView.post {
        webView.evaluateJavascript(
            """
            (function(){
              try {
                var host = window.location.hostname || '';
                if (!host.includes('spotify.com')) {
                  // For Facebook desktop form on mobile: ensure viewport meta tag so form fits screen
                  if (host.includes('facebook.com')) {
                    if (!document.querySelector('meta[name=viewport]')) {
                      var meta = document.createElement('meta');
                      meta.name = 'viewport';
                      meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=2.0';
                      document.head.appendChild(meta);
                    }
                  }
                  // Remove layout fix if present on non-Spotify domains
                  var oldStyle = document.getElementById('spotui-login-layout-fix');
                  if (oldStyle) oldStyle.remove();
                  return;
                }

                // We are on spotify.com
                if (!window.__fbListenerAttached) {
                  window.__fbListenerAttached = true;
                  document.addEventListener('click', function(e) {
                    try {
                      var el = e.target && e.target.closest ? e.target.closest('a, button, div[role="button"]') : null;
                      if (el) {
                        var href = ((el.getAttribute('href') || '') + ' ' + (el.href || '')).toLowerCase();
                        var text = ((el.textContent || '') + ' ' + (el.innerText || '')).toLowerCase();
                        if (href.indexOf('facebook') !== -1 || text.indexOf('facebook') !== -1) {
                          if (window.AndroidBridge && window.AndroidBridge.prepareFacebookLogin) {
                            window.AndroidBridge.prepareFacebookLogin();
                          }
                        }
                      }
                    } catch(err) {}
                  }, true);
                }

                var style = document.getElementById('spotui-login-layout-fix');
                if (!style) {
                  style = document.createElement('style');
                  style.id = 'spotui-login-layout-fix';
                  document.head.appendChild(style);
                }
                // Expand Spotify's login container using 100vh without locking html/body to fixed pixel height
                style.textContent = '#__next{min-height:100vh!important;} main{min-height:100vh!important;max-height:none!important;position:relative!important;overflow:auto!important;}';
              } catch(e) {}
            })();
            """.trimIndent(),
            null
        )
    }
}

