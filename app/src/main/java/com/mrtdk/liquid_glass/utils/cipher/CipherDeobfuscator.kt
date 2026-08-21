package com.mrtdk.liquid_glass.utils.cipher

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CipherDeobfuscator {
    private const val TAG = "CipherDeobfusc"

    lateinit var appContext: Context
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Build the cipher WebView before the first stream needs it, so the
     * player-JS download + WebView spin-up never sits on the first-play path.
     * Best-effort: any failure just means the lazy path takes over later.
     */
    suspend fun prewarm() {
        try {
            getOrCreateWebView(forceRefresh = false)
        } catch (e: Exception) {
            Log.e(TAG, "Cipher WebView prewarm failed: ${e.message}")
        }
    }

    private var cipherWebView: CipherWebView? = null
    private var currentPlayerHash: String? = null

    suspend fun deobfuscateStreamUrl(signatureCipher: String, videoId: String): String? {
        return try {
            deobfuscateInternal(signatureCipher, videoId, isRetry = false)
        } catch (e: Exception) {
            Log.e(TAG, "Cipher deobfuscation failed, retrying with fresh JS: ${e.message}")
            // Invalidate cache and retry once with fresh player JS
            try {
                PlayerJsFetcher.invalidateCache()
                closeWebView()
                deobfuscateInternal(signatureCipher, videoId, isRetry = true)
            } catch (retryE: Exception) {
                Log.e(TAG, "Cipher deobfuscation retry also failed: ${retryE.message}")
                null
            }
        }
    }

    private suspend fun deobfuscateInternal(signatureCipher: String, videoId: String, isRetry: Boolean): String? {
        // Parse the signatureCipher query string
        val params = parseQueryParams(signatureCipher)
        val obfuscatedSig = params["s"]
        val sigParam = params["sp"] ?: "signature"
        val baseUrl = params["url"]

        if (obfuscatedSig == null || baseUrl == null) {
            Log.e(TAG, "Could not parse signatureCipher params: s=${obfuscatedSig != null}, url=${baseUrl != null}")
            return null
        }

        Log.d(TAG, "Deobfuscating cipher for $videoId: sig=${obfuscatedSig.take(20)}..., sp=$sigParam")

        val webView = getOrCreateWebView(forceRefresh = isRetry)
            ?: return null

        // Deobfuscate signature
        val deobfuscatedSig = webView.deobfuscateSignature(obfuscatedSig)

        // Build the URL with deobfuscated signature
        val separator = if ("?" in baseUrl) "&" else "?"
        val finalUrl = "$baseUrl${separator}${sigParam}=${Uri.encode(deobfuscatedSig)}"

        Log.d(TAG, "Custom cipher deobfuscation succeeded for $videoId")
        return finalUrl
    }

    /**
     * Transform the 'n' parameter in a streaming URL to avoid throttling/403.
     * Uses the runtime-discovered n-function from the player JS WebView.
     * Returns the URL with the transformed 'n' value, or the original URL if transform fails.
     */
    suspend fun transformNParamInUrl(url: String): String {
        return try {
            transformNInternal(url)
        } catch (e: Exception) {
            Log.e(TAG, "N-transform failed, returning original URL: ${e.message}")
            url
        }
    }

    private suspend fun transformNInternal(url: String): String {
        // Extract the 'n' parameter value from the URL
        val nMatch = Regex("[?&]n=([^&]+)").find(url)
        if (nMatch == null) {
            Log.d(TAG, "No 'n' parameter found in URL, skipping transform")
            return url
        }
        val nValue = Uri.decode(nMatch.groupValues[1])
        Log.d(TAG, "N-param found: $nValue")

        val webView = getOrCreateWebView(forceRefresh = false) ?: return url

        if (!webView.nFunctionAvailable) {
            Log.e(TAG, "N-transform function was not discovered at init time")
            return url
        }

        val transformedN = webView.transformN(nValue)
        Log.d(TAG, "N-param transformed: $nValue -> $transformedN")

        // Replace n= parameter in URL
        return url.replaceFirst(
            Regex("([?&])n=[^&]+"),
            "$1n=${Uri.encode(transformedN)}"
        )
    }

    private suspend fun getOrCreateWebView(forceRefresh: Boolean): CipherWebView? {
        if (!forceRefresh && cipherWebView != null) {
            return cipherWebView
        }

        // Close existing WebView if any
        if (cipherWebView != null) {
            closeWebView()
        }

        // Fetch player JS
        val result = PlayerJsFetcher.getPlayerJs(forceRefresh = forceRefresh)
        if (result == null) {
            Log.e(TAG, "Failed to get player JS")
            return null
        }
        val (playerJs, hash) = result

        // Extract signature function info
        val sigInfo = FunctionNameExtractor.extractSigFunctionInfo(playerJs)

        if (sigInfo == null) {
            Log.e(TAG, "Could not extract signature function info from player JS")
            return null
        }

        // Extract n-transform function info (for throttle avoidance / 403 fix)
        val nFuncInfo = FunctionNameExtractor.extractNFunctionInfo(playerJs)
        if (nFuncInfo == null) {
            Log.e(TAG, "Could not extract n-function info from player JS (will try brute-force)")
        }

        currentPlayerHash = hash
        val wv = CipherWebView.create(appContext, playerJs, sigInfo, nFuncInfo)
        cipherWebView = wv
        return wv
    }

    fun closeWebView() {
        cipherWebView?.close()
        cipherWebView = null
        currentPlayerHash = null
    }

    private fun parseQueryParams(queryString: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (param in queryString.split("&")) {
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                result[Uri.decode(parts[0])] = Uri.decode(parts[1])
            }
        }
        return result
    }
}
