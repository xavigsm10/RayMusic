package com.mrtdk.liquid_glass.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.echo.innertube.NewPipeExtractor
import com.echo.innertube.YouTube
import com.echo.innertube.YouTubeExtractor
import com.echo.innertube.models.WatchEndpoint
import com.echo.innertube.models.YouTubeClient
import com.echo.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
import com.echo.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import com.echo.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import com.echo.innertube.models.YouTubeClient.Companion.IOS
import com.echo.innertube.models.YouTubeClient.Companion.IPADOS
import com.echo.innertube.models.YouTubeClient.Companion.MOBILE
import com.echo.innertube.models.YouTubeClient.Companion.TVHTML5
import com.echo.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.echo.innertube.models.YouTubeClient.Companion.WEB
import com.echo.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.echo.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.echo.innertube.models.response.PlayerResponse
import com.mrtdk.liquid_glass.jiosaavn.SaavnService
import com.mrtdk.liquid_glass.utils.potoken.PoTokenGenerator
import com.mrtdk.liquid_glass.utils.potoken.PoTokenResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object YTPlayerUtils {
    private const val TAG = "YTPlayerUtils"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .proxyAuthenticator { _, response ->
            YouTube.proxyAuth?.let { auth ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", auth)
                    .build()
            } ?: response.request
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .fastFallback(true)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    /**
     * Primary client for YouTube playback streams.
     * IOS & IPADOS currently bypass Google bot attestation checks completely.
     */
    private val MAIN_CLIENT: YouTubeClient = IOS

    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        IPADOS,
        ANDROID_VR_1_43_32,
        ANDROID_VR_1_61_48,
        ANDROID_CREATOR,
        MOBILE,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        TVHTML5,
        WEB_REMIX,
        WEB,
        WEB_CREATOR
    )

    data class PlaybackData(
        val format: PlayerResponse.StreamingData.Format?,
        val streamUrl: String,
        val expiresInSeconds: Int = 21600,
        val isSaavnStream: Boolean = false
    )

    fun init(context: Context) {
        YouTubeExtractor.cacheDir = context.cacheDir
    }

    /**
     * Resolves the best stream URL for playback.
     * Uses JioSaavn 320kbps as primary lossless source (like ViviMusic),
     * falling back smoothly to YouTube InnerTube streams with session rotation.
     */
    suspend fun resolveStreamUrl(
        videoId: String,
        preferLow: Boolean = false,
        playlistId: String? = null
    ): String? = withContext(Dispatchers.IO) {
        // ── 1. JioSaavn Fast Intercept (ViviMusic Pattern) ────────────────────
        try {
            val saavnStreamUrl = resolveSaavnStream(videoId)
            if (!saavnStreamUrl.isNullOrBlank()) {
                Log.i(TAG, "Successfully resolved lossless 320kbps stream via JioSaavn for videoId=$videoId")
                return@withContext saavnStreamUrl
            }
        } catch (e: Exception) {
            Log.d(TAG, "JioSaavn resolution skipped/failed for $videoId: ${e.message}")
        }

        // ── 2. YouTube InnerTube Playback Resolution ─────────────────────────
        val firstAttempt = resolvePlaybackData(videoId, preferLow, playlistId)
        if (firstAttempt.isSuccess) {
            BotDetectionMitigator.notifyPlaybackSuccess()
            return@withContext firstAttempt.getOrNull()?.streamUrl
        }

        // ── 3. Bot Detection Mitigation & Session Rotation ───────────────────
        if (YouTube.cookie == null) {
            Log.w(TAG, "First playback attempt failed for $videoId. Rotating guest session and retrying...")
            BotDetectionMitigator.rotateGuestSession()
            val retryAttempt = resolvePlaybackData(videoId, preferLow, playlistId)
            if (retryAttempt.isSuccess) {
                BotDetectionMitigator.notifyPlaybackSuccess()
                return@withContext retryAttempt.getOrNull()?.streamUrl
            }
        }

        firstAttempt.getOrNull()?.streamUrl
    }

    /**
     * Attempts to find a matching track on JioSaavn and retrieve direct 320kbps CDN URL.
     */
    private suspend fun resolveSaavnStream(videoId: String): String? = coroutineScope {
        val nextDeferred = async {
            try {
                val nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
                nextResult?.items?.getOrNull(nextResult.currentIndex ?: 0)
                    ?: nextResult?.items?.firstOrNull()
            } catch (_: Exception) { null }
        }

        val songItem = nextDeferred.await()
        val title = songItem?.title?.trim().orEmpty()
        val artists = songItem?.artists?.map { it.name.trim() } ?: emptyList()
        val artist = artists.joinToString(", ")
        val expectedDuration = songItem?.duration

        if (title.isBlank()) return@coroutineScope null

        val query = if (artist.isNotBlank()) "$title $artist" else title
        val songs = SaavnService.searchSongs(query).getOrNull() ?: return@coroutineScope null

        val wantedTitleLower = title.lowercase(java.util.Locale.US)
        val wantedArtistsLower = artists.map { it.lowercase(java.util.Locale.US) }

        val bestMatch = songs.firstOrNull { candidate ->
            val candidateTitleLower = candidate.name.lowercase(java.util.Locale.US)
            val candidateArtists = candidate.artists.primary.map { it.name.lowercase(java.util.Locale.US) }

            val titleMatches = candidateTitleLower == wantedTitleLower ||
                    candidateTitleLower.contains(wantedTitleLower) ||
                    wantedTitleLower.contains(candidateTitleLower)

            val artistMatches = wantedArtistsLower.isEmpty() ||
                    candidateArtists.any { ca -> wantedArtistsLower.any { wa -> ca.contains(wa) || wa.contains(ca) } }

            val durationMatches = if (expectedDuration != null && candidate.duration != null) {
                kotlin.math.abs(expectedDuration - candidate.duration) <= 15
            } else true

            titleMatches && (artistMatches || durationMatches)
        } ?: songs.firstOrNull()

        if (bestMatch != null) {
            val streamUrl = SaavnService.selectBestUrl(bestMatch.downloadUrl, "320kbps")
            if (!streamUrl.isNullOrBlank()) {
                return@coroutineScope streamUrl
            }
        }

        null
    }

    suspend fun resolvePlaybackData(
        videoId: String,
        preferLow: Boolean = false,
        playlistId: String? = null
    ): Result<PlaybackData> = runCatching {
        Log.d(TAG, "Resolving YouTube playback data for videoId=$videoId, playlistId=$playlistId")

        val isLoggedIn = YouTube.cookie != null
        val signatureTimestamp = try {
            NewPipeExtractor.getSignatureTimestamp(videoId).getOrNull()
        } catch (_: Exception) { null }

        var poToken: PoTokenResult? = null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        if (MAIN_CLIENT.useWebPoTokens && sessionId != null) {
            try {
                poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
            } catch (e: Exception) {
                Log.e(TAG, "PoToken generation failed for MAIN_CLIENT: ${e.message}")
            }
        }

        var mainPlayerResponse: PlayerResponse? = try {
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp, poToken?.playerRequestPoToken).getOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "Main client ${MAIN_CLIENT.clientName} failed: ${e.message}")
            null
        }

        var usedAgeRestrictedClient: YouTubeClient? = null
        val mainStatus = mainPlayerResponse?.playabilityStatus?.status
        val isAgeRestricted = mainStatus in listOf(
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED"
        )
        val wasOriginallyAgeRestricted = isAgeRestricted

        if (isAgeRestricted && isLoggedIn) {
            Log.d(TAG, "Age-restricted detected, using WEB_CREATOR")
            val creatorResponse = YouTube.player(videoId, playlistId, WEB_CREATOR, null, null).getOrNull()
            if (creatorResponse?.playabilityStatus?.status == "OK") {
                mainPlayerResponse = creatorResponse
                usedAgeRestrictedClient = WEB_CREATOR
            }
        }

        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var bestCandidateUrl: String? = null
        var bestCandidateFormat: PlayerResponse.StreamingData.Format? = null

        val startIndex = if (isAgeRestricted) 0 else -1

        for (clientIndex in startIndex until STREAM_FALLBACK_CLIENTS.size) {
            val client: YouTubeClient
            val streamPlayerResponse: PlayerResponse?

            if (clientIndex == -1) {
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
            } else {
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    continue
                }

                if (client.useWebPoTokens && poToken == null && sessionId != null) {
                    try {
                        poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Lazy PoToken generation failed for ${client.clientName}: ${e.message}")
                    }
                }

                val clientPoToken = if (client.useWebPoTokens) poToken?.playerRequestPoToken else null
                val clientSigTimestamp = if (wasOriginallyAgeRestricted || !client.useSignatureTimestamp) null else signatureTimestamp
                streamPlayerResponse = YouTube.player(videoId, playlistId, client, clientSigTimestamp, clientPoToken).getOrNull()
            }

            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                val foundFormat = findFormat(streamPlayerResponse, preferLow) ?: continue
                var currentUrl = findUrlOrNull(foundFormat, videoId, streamPlayerResponse, skipNewPipe = wasOriginallyAgeRestricted) ?: continue

                val currentClient = if (clientIndex == -1) (usedAgeRestrictedClient ?: MAIN_CLIENT) else STREAM_FALLBACK_CLIENTS[clientIndex]

                // Apply n-transform ONLY for web clients that require it
                if (currentClient.useWebPoTokens) {
                    try {
                        val transformed = YouTubeExtractor.deobfuscateUrlNParam(currentUrl)
                        if (transformed != currentUrl) {
                            currentUrl = transformed
                            Log.d(TAG, "YouTubeExtractor n-transform applied successfully for ${currentClient.clientName}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "YouTubeExtractor n-transform failed: ${e.message}")
                    }

                    // Apply PoToken parameter if web client (Base64 direct, do NOT Uri.encode)
                    if (poToken?.streamingDataPoToken != null) {
                        val separator = if ("?" in currentUrl) "&" else "?"
                        currentUrl = "${currentUrl}${separator}pot=${poToken.streamingDataPoToken}"
                    }
                }

                bestCandidateUrl = currentUrl
                bestCandidateFormat = foundFormat
                format = foundFormat
                streamUrl = currentUrl
                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds ?: 21600

                // If main client, last client or private track, skip network validation probe
                if (clientIndex == -1 || clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) {
                    Log.d(TAG, "Using stream from ${currentClient.clientName} directly")
                    break
                }

                // Pre-validation with HTTP byte-range GET (Range: bytes=0-0)
                if (validateStatus(currentUrl)) {
                    Log.d(TAG, "Stream validated OK with client: ${currentClient.clientName}")
                    break
                } else {
                    Log.w(TAG, "Stream validation probe failed for client: ${currentClient.clientName}")
                    if (currentClient.useWebPoTokens) {
                        try {
                            val nTransformed = YouTubeExtractor.deobfuscateUrlNParam(currentUrl)
                            if (nTransformed != currentUrl && validateStatus(nTransformed)) {
                                Log.d(TAG, "Alternate n-transform validated OK!")
                                streamUrl = nTransformed
                                bestCandidateUrl = nTransformed
                                break
                            }
                        } catch (_: Exception) {}
                    }
                }
            } else {
                val status = streamPlayerResponse?.playabilityStatus?.status
                val reason = streamPlayerResponse?.playabilityStatus?.reason
                Log.d(TAG, "Client ${client.clientName} failed with status=$status, reason=$reason")
            }
        }

        // If no stream passed validation probe, fall back to best candidate URL from clients
        if (streamUrl == null && bestCandidateUrl != null) {
            streamUrl = bestCandidateUrl
            format = bestCandidateFormat
            Log.d(TAG, "Using best candidate stream URL despite probe status")
        }

        if (streamUrl == null) {
            throw IllegalStateException("Failed to obtain playback stream for videoId=$videoId")
        }

        PlaybackData(
            format = format,
            streamUrl = streamUrl,
            expiresInSeconds = streamExpiresInSeconds ?: 21600
        )
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        preferLow: Boolean
    ): PlayerResponse.StreamingData.Format? {
        val formats = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio } ?: return null

        return if (preferLow) {
            formats.minByOrNull { it.bitrate }
        } else {
            // Prioritize Opus (audio/webm, itag 251) or high-bitrate AAC (itag 140)
            formats.filter { it.mimeType.contains("opus", ignoreCase = true) }
                .maxByOrNull { it.bitrate }
                ?: formats.maxByOrNull { it.bitrate }
        }
    }

    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        playerResponse: PlayerResponse,
        skipNewPipe: Boolean = false
    ): String? {
        // 1. Direct format URL
        if (!format.url.isNullOrBlank()) {
            return format.url
        }

        // 2. Custom JS deobfuscation with Mozilla Rhino for signatureCipher
        val cipherStr = format.signatureCipher
        if (!cipherStr.isNullOrBlank()) {
            try {
                val decrypted = YouTubeExtractor.decryptUrl(cipherStr)
                if (!decrypted.isNullOrBlank()) {
                    return decrypted
                }
            } catch (e: Exception) {
                Log.e(TAG, "Rhino JS decrypt failed: ${e.message}")
            }
        }

        // 3. Fallback to NewPipeExtractor if not age-restricted
        if (!skipNewPipe) {
            try {
                val npUrl = NewPipeExtractor.getStreamUrl(format, videoId)
                if (!npUrl.isNullOrBlank()) {
                    return npUrl
                }
            } catch (e: Exception) {
                Log.e(TAG, "NewPipe extractor fallback failed: ${e.message}")
            }
        }

        return null
    }

    private fun validateStatus(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                .header("Range", "bytes=0-0")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 206
            }
        } catch (_: Exception) {
            false
        }
    }
}
