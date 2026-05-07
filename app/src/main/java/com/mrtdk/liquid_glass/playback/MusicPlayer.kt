package com.mrtdk.liquid_glass.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.echo.innertube.YouTube
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class MusicPlayer(private val context: Context) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var pollingJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _songEnded = MutableStateFlow(0)
    val songEnded: StateFlow<Int> = _songEnded

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startPolling()
                    } else {
                        stopPolling()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = controller?.duration ?: 0L
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        _songEnded.value = _songEnded.value + 1
                    }
                }
                
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _duration.value = controller?.duration ?: 0L
                }
            })
            // Initial state
            _isPlaying.value = controller?.isPlaying ?: false
            _duration.value = controller?.duration ?: 0L
            if (_isPlaying.value) startPolling()
        }, MoreExecutors.directExecutor())
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (true) {
                _currentPosition.value = controller?.currentPosition ?: 0L
                delay(50)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }

    fun playLocalSong(contentUri: Uri) {
        val mediaItem = MediaItem.fromUri(contentUri)
        controller?.setMediaItem(mediaItem)
        controller?.prepare()
        controller?.play()
    }

    fun playOnlineSong(videoId: String) {
        scope.launch {
            val streamUrl = withContext(Dispatchers.IO) {
                var formatUrl: String? = null
                var usedClient = ""
                var fallbackUrl: String? = null
                var fallbackClient = ""
                
                if (YouTube.cookie == null) {
                    // Try to grab a fresh visitorData to prevent LOGIN_REQUIRED or UNPLAYABLE errors
                    if (YouTube.visitorData == null) {
                        try {
                            YouTube.visitorData = YouTube.visitorData().getOrNull()
                        } catch (e: Exception) {
                            android.util.Log.e("MusicPlayer", "Failed to fetch visitorData: ${e.message}")
                        }
                    }
                }
                
                val httpClient = okhttp3.OkHttpClient.Builder().proxy(YouTube.proxy).build()

                val clientsToTry = listOf(
                    com.echo.innertube.models.YouTubeClient.ANDROID_VR_NO_AUTH,
                    com.echo.innertube.models.YouTubeClient.WEB_REMIX,
                    com.echo.innertube.models.YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
                    com.echo.innertube.models.YouTubeClient.TVHTML5,
                    com.echo.innertube.models.YouTubeClient.ANDROID_VR_1_43_32,
                    com.echo.innertube.models.YouTubeClient.ANDROID_VR_1_61_48,
                    com.echo.innertube.models.YouTubeClient.ANDROID_CREATOR,
                    com.echo.innertube.models.YouTubeClient.IPADOS,
                    com.echo.innertube.models.YouTubeClient.MOBILE,
                    com.echo.innertube.models.YouTubeClient.IOS,
                    com.echo.innertube.models.YouTubeClient.WEB,
                    com.echo.innertube.models.YouTubeClient.WEB_CREATOR
                )

                for (client in clientsToTry) {
                    try {
                        android.util.Log.d("MusicPlayer", "Testing client: ${client.clientName}")
                        val signatureTimestamp = if (client.useSignatureTimestamp) com.echo.innertube.NewPipeUtils.getSignatureTimestamp(videoId).getOrNull() else null
                        val playerResponse = YouTube.player(videoId, null, client, signatureTimestamp).getOrNull()
                        
                        android.util.Log.d("MusicPlayer", "Client ${client.clientName} playabilityStatus: ${playerResponse?.playabilityStatus?.status}")
                        
                        // Enrich the response using NewPipe's extractor, just like Echo-Music does
                        val responseToUse = if (playerResponse != null) {
                            YouTube.newPipePlayer(videoId, playerResponse) ?: playerResponse
                        } else null
                        
                        val formats = responseToUse?.streamingData?.adaptiveFormats ?: emptyList()
                        android.util.Log.d("MusicPlayer", "Client ${client.clientName} returned ${formats.size} adaptive formats.")
                        
                        val format = formats.findLast { it.itag == 251 || it.itag == 140 } ?: formats.find { it.mimeType.startsWith("audio/") }
                        
                        if (format != null) {
                            val candidateUrl = com.echo.innertube.NewPipeUtils.getStreamUrl(format, videoId).getOrNull() ?: format.url
                            
                            if (candidateUrl != null) {
                                val cName = client.clientName
                                val ua = when {
                                    cName.equals("WEB_REMIX", ignoreCase = true) || cName.equals("WEB", ignoreCase = true) || cName.equals("WEB_CREATOR", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.USER_AGENT_WEB
                                    cName.equals("TVHTML5", ignoreCase = true) || cName.contains("TVHTML5_SIMPLY", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.TVHTML5.userAgent
                                    cName.startsWith("IOS", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.IOS.userAgent
                                    cName.startsWith("ANDROID_VR", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.ANDROID_VR_NO_AUTH.userAgent
                                    cName.startsWith("ANDROID", ignoreCase = true) -> com.echo.innertube.models.YouTubeClient.MOBILE.userAgent
                                    else -> com.echo.innertube.models.YouTubeClient.ANDROID_VR_NO_AUTH.userAgent
                                }
                                
                                val origin = when {
                                    cName.equals("WEB_REMIX", ignoreCase = true) || cName.equals("WEB", ignoreCase = true) || cName.equals("WEB_CREATOR", ignoreCase = true) -> "https://music.youtube.com"
                                    cName.equals("TVHTML5", ignoreCase = true) || cName.contains("TVHTML5_SIMPLY", ignoreCase = true) -> "https://www.youtube.com"
                                    else -> null
                                }
                                
                                val referer = when {
                                    cName.equals("WEB_REMIX", ignoreCase = true) || cName.equals("WEB", ignoreCase = true) || cName.equals("WEB_CREATOR", ignoreCase = true) -> "https://music.youtube.com/"
                                    cName.equals("TVHTML5", ignoreCase = true) || cName.contains("TVHTML5_SIMPLY", ignoreCase = true) -> "https://www.youtube.com/tv"
                                    else -> null
                                }

                                val testUrl = if (candidateUrl.contains("c=")) candidateUrl else candidateUrl + (if (candidateUrl.contains("?")) "&" else "?") + "c=" + cName
                                
                                if (fallbackUrl == null) {
                                    android.util.Log.d("MusicPlayer", "Assigned fallback candidate to: ${client.clientName}")
                                    fallbackUrl = candidateUrl
                                    fallbackClient = client.clientName
                                }
                                
                                val requestBuilder = okhttp3.Request.Builder()
                                    .get()
                                    .url(testUrl)
                                    .header("User-Agent", ua)
                                    .header("Range", "bytes=0-1")
                                    
                                if (origin != null) {
                                    requestBuilder.header("Origin", origin)
                                    requestBuilder.header("Referer", referer ?: "")
                                }
                                    
                                YouTube.cookie?.let { requestBuilder.header("Cookie", it) }
                                
                                try {
                                    val response = httpClient.newCall(requestBuilder.build()).execute()
                                    android.util.Log.d("MusicPlayer", "Client ${client.clientName} validation HTTP code: ${response.code}")
                                    if (response.isSuccessful || response.code == 206) {
                                        android.util.Log.d("MusicPlayer", "Client ${client.clientName} VALIDATED OK!")
                                        formatUrl = candidateUrl
                                        usedClient = client.clientName
                                        break
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.d("MusicPlayer", "Client ${client.clientName} HTTP validation crashed: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicPlayer", "Client ${client.clientName} crashed completely wrapper: ${e.message}")
                        continue
                    }
                }
                
                // If everything failed validation, use the first fallback we found
                if (formatUrl == null && fallbackUrl != null) {
                    formatUrl = fallbackUrl
                    usedClient = fallbackClient
                }
                
                // If everything fails, try NewPipe stream fallback directly like Echo-Music does
                if (formatUrl == null) {
                    val fallbackStreams = YouTube.getNewPipeStreamUrls(videoId)
                    val fallback = fallbackStreams.find { it.first == 251 || it.first == 140 } ?: fallbackStreams.firstOrNull()
                    if (fallback != null) {
                        formatUrl = fallback.second
                        usedClient = "NEWPIPE_FALLBACK"
                    }
                }
                
                formatUrl
            }
            if (streamUrl != null) {
                android.util.Log.d("MusicPlayer", "Playing stream: $streamUrl")
                
                val mediaItem = MediaItem.Builder()
                    .setMediaId(videoId)
                    .setUri(Uri.parse(streamUrl))
                    .build()
                    
                controller?.setMediaItem(mediaItem)
                controller?.prepare()
                controller?.play()
            }
        }
    }

    fun togglePlayPause() {
        if (controller?.isPlaying == true) {
            controller?.pause()
        } else {
            controller?.play()
        }
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
        _currentPosition.value = position
    }

    fun setVolume(volume: Float) {
        controller?.volume = volume
    }

    fun release() {
        stopPolling()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
