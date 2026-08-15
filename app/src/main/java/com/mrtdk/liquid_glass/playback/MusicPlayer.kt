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
    var controller: MediaController? = null
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

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError

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

                    // ListenTogether sync broadcast
                    if (com.mrtdk.liquid_glass.playback.ListenTogetherManager.isConnected &&
                        com.mrtdk.liquid_glass.playback.ListenTogetherManager.role == "host" &&
                        !com.mrtdk.liquid_glass.playback.ListenTogetherManager.isSyncing) {
                        val currentPos = controller?.currentPosition ?: 0L
                        val trackId = controller?.currentMediaItem?.mediaId
                        val metadata = controller?.currentMediaItem?.mediaMetadata
                        val title = metadata?.title?.toString()
                        val artist = metadata?.artist?.toString()
                        val artUrl = metadata?.artworkUri?.toString()
                        com.mrtdk.liquid_glass.playback.ListenTogetherManager.sendPlaybackAction(
                            if (isPlaying) "play" else "pause",
                            currentPos,
                            trackId,
                            title,
                            artist,
                            artUrl
                        )
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

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val rootCause = error.cause ?: error
                    _playbackError.value = "Playback error (code ${error.errorCode}): ${error.message}\nCause: ${rootCause.localizedMessage ?: rootCause.toString()}"
                }
                
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _duration.value = controller?.duration ?: 0L
                    _playbackError.value = null

                    // ListenTogether sync broadcast
                    if (com.mrtdk.liquid_glass.playback.ListenTogetherManager.isConnected &&
                        com.mrtdk.liquid_glass.playback.ListenTogetherManager.role == "host" &&
                        !com.mrtdk.liquid_glass.playback.ListenTogetherManager.isSyncing) {
                        val trackId = mediaItem?.mediaId
                        val metadata = mediaItem?.mediaMetadata
                        val title = metadata?.title?.toString()
                        val artist = metadata?.artist?.toString()
                        val artUrl = metadata?.artworkUri?.toString()
                        com.mrtdk.liquid_glass.playback.ListenTogetherManager.sendPlaybackAction(
                            "play",
                            0L,
                            trackId,
                            title,
                            artist,
                            artUrl
                        )
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                        // ListenTogether sync broadcast
                        if (com.mrtdk.liquid_glass.playback.ListenTogetherManager.isConnected &&
                            com.mrtdk.liquid_glass.playback.ListenTogetherManager.role == "host" &&
                            !com.mrtdk.liquid_glass.playback.ListenTogetherManager.isSyncing) {
                            val trackId = controller?.currentMediaItem?.mediaId
                            val metadata = controller?.currentMediaItem?.mediaMetadata
                            val title = metadata?.title?.toString()
                            val artist = metadata?.artist?.toString()
                            val artUrl = metadata?.artworkUri?.toString()
                            com.mrtdk.liquid_glass.playback.ListenTogetherManager.sendPlaybackAction(
                                "seek",
                                newPosition.positionMs,
                                trackId,
                                title,
                                artist,
                                artUrl
                            )
                        }
                    }
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _shuffleModeEnabled.value = shuffleModeEnabled
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                }
            })
            // Initial state
            _isPlaying.value = controller?.isPlaying ?: false
            _duration.value = controller?.duration ?: 0L
            _shuffleModeEnabled.value = controller?.shuffleModeEnabled ?: false
            _repeatMode.value = controller?.repeatMode ?: Player.REPEAT_MODE_OFF
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

    fun playLocalSong(contentUri: Uri, title: String? = null, artist: String? = null, artUrl: String? = null) {
        _playbackError.value = null
        val metadata = androidx.media3.common.MediaMetadata.Builder().apply {
            title?.let { setTitle(it) }
            artist?.let { setArtist(it) }
            artUrl?.let { setArtworkUri(Uri.parse(it)) }
        }.build()
        val mediaItem = MediaItem.Builder()
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()
        controller?.setMediaItem(mediaItem)
        controller?.prepare()
        controller?.play()
    }

    fun playOnlineSong(videoId: String, title: String? = null, artist: String? = null, artUrl: String? = null) {
        _playbackError.value = null
        if (title != null || artist != null) {
            songMetadataCache[videoId] = Pair(title.orEmpty(), artist.orEmpty())
        }
        android.util.Log.d("MusicPlayer", "Playing stream instantly: yt://$videoId")
        
        val localUriStr = com.mrtdk.liquid_glass.data.LibraryManager.getString("local_uri_$videoId")
        if (localUriStr != null) {
            try {
                val localUri = Uri.parse(localUriStr)
                val file = java.io.File(localUri.path ?: "")
                if (file.exists()) {
                    android.util.Log.d("MusicPlayer", "Playing offline downloaded file: $localUriStr")
                    playLocalSong(localUri, title, artist, artUrl)
                    return
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicPlayer", "Error checking offline file path", e)
            }
        }
        
        val metadata = androidx.media3.common.MediaMetadata.Builder().apply {
            title?.let { setTitle(it) }
            artist?.let { setArtist(it) }
            artUrl?.let { setArtworkUri(Uri.parse(it)) }
        }.build()
        
        val mediaItem = MediaItem.Builder()
            .setMediaId(videoId)
            .setUri(Uri.parse("yt://$videoId"))
            .setCustomCacheKey(videoId)
            .setMediaMetadata(metadata)
            .build()
            
        controller?.setMediaItem(mediaItem)
        controller?.prepare()
        controller?.play()
    }

    fun playOnlineSongs(songs: List<MediaItem>, startIndex: Int = 0) {
        _playbackError.value = null
        songs.forEach { item ->
            val vId = item.mediaId
            val t = item.mediaMetadata.title?.toString().orEmpty()
            val a = item.mediaMetadata.artist?.toString().orEmpty()
            if (vId.isNotBlank() && (t.isNotBlank() || a.isNotBlank())) {
                songMetadataCache[vId] = Pair(t, a)
            }
        }
        controller?.setMediaItems(songs, startIndex, 0L)
        controller?.prepare()
        controller?.play()
    }

    fun addOnlineSongToQueue(videoId: String, title: String? = null, artist: String? = null, artUrl: String? = null) {
        if (title != null || artist != null) {
            songMetadataCache[videoId] = Pair(title.orEmpty(), artist.orEmpty())
        }
        val metadata = androidx.media3.common.MediaMetadata.Builder().apply {
            title?.let { setTitle(it) }
            artist?.let { setArtist(it) }
            artUrl?.let { setArtworkUri(Uri.parse(it)) }
        }.build()
        
        val mediaItem = MediaItem.Builder()
            .setMediaId(videoId)
            .setUri(Uri.parse("yt://$videoId"))
            .setCustomCacheKey(videoId)
            .setMediaMetadata(metadata)
            .build()
            
        controller?.addMediaItem(mediaItem)
    }

    fun togglePlayPause() {
        if (controller?.isPlaying == true) {
            pause()
        } else {
            controller?.play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
        _currentPosition.value = position
    }

    fun setVolume(volume: Float) {
        controller?.setVolume(volume)
    }

    fun clearPlaybackError() {
        _playbackError.value = null
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun setRepeatMode(repeatMode: Int) {
        controller?.repeatMode = repeatMode
    }

    fun reloadCurrentSong() {
        val currentMediaItem = controller?.currentMediaItem ?: return
        val currentPosition = controller?.currentPosition ?: 0L
        val videoId = currentMediaItem.mediaId
        
        if (!videoId.isNullOrBlank()) {
            clearCache(videoId)
        }
        
        controller?.let { c ->
            val wasPlaying = c.playWhenReady
            c.setMediaItem(currentMediaItem)
            c.seekTo(currentPosition)
            c.prepare()
            if (wasPlaying) {
                c.play()
            }
        }
    }

    fun release() {
        stopPolling()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }

    companion object {
        private val songUrlCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()
        private val spotifyToYtCache = java.util.concurrent.ConcurrentHashMap<String, String>()
        val songMetadataCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, String>>()

        fun clearCache(videoId: String) {
            songUrlCache.remove(videoId)
            spotifyToYtCache.remove(videoId)
            songMetadataCache.remove(videoId)
        }

        private fun isYouTubeId(id: String): Boolean {
            return id.length == 11 && !id.contains(" ") && !id.contains(":") && !id.contains("/")
        }

        suspend fun resolveUrl(videoId: String): String? = withContext(Dispatchers.IO) {
            val quality = com.mrtdk.liquid_glass.data.LibraryManager.getString("audio_quality", "high") ?: "high"
            val preferLow = quality == "low"

            // Check cache for this exact videoId (Spotify or YT)
            songUrlCache[videoId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                return@withContext it.first
            }

            var targetVideoId = videoId

            val cachedMeta = songMetadataCache[videoId]
            val currentSong = com.mrtdk.liquid_glass.playback.PlaybackQueue.currentSong
            val queueItem = com.mrtdk.liquid_glass.playback.PlaybackQueue.queue.find { it.videoId == videoId }
            val songTitle = cachedMeta?.first?.ifEmpty { null }
                ?: currentSong?.takeIf { it.videoId == videoId }?.title
                ?: queueItem?.title
            val songArtist = cachedMeta?.second?.ifEmpty { null }
                ?: currentSong?.takeIf { it.videoId == videoId }?.artist
                ?: queueItem?.artist

            // If not a valid 11-char YouTube ID (e.g. Spotify ID or custom ID)
            if (!isYouTubeId(targetVideoId)) {
                spotifyToYtCache[videoId]?.let { cachedYtId ->
                    targetVideoId = cachedYtId
                } ?: run {
                    val query = when {
                        !songTitle.isNullOrBlank() && !songArtist.isNullOrBlank() -> "$songArtist $songTitle"
                        !songTitle.isNullOrBlank() -> songTitle
                        else -> videoId
                    }.trim()

                    if (query.isNotBlank()) {
                        val foundYtId = try {
                            val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                ?: YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
                            searchResult?.items?.firstOrNull()?.id
                        } catch (e: Exception) {
                            android.util.Log.e("MusicPlayer", "Failed YouTube search for Spotify track $query", e)
                            null
                        }

                        if (foundYtId != null && isYouTubeId(foundYtId)) {
                            spotifyToYtCache[videoId] = foundYtId
                            targetVideoId = foundYtId
                            android.util.Log.d("MusicPlayer", "Mapped Spotify track $videoId -> YT $targetVideoId ($query)")
                        }
                    }
                }
            }

            // Extract stream URL for targetVideoId
            var formatUrl = extractDirectStreamUrl(targetVideoId, preferLow)

            // Fallback: If direct stream extraction failed and we have song info, attempt search fallback
            if (formatUrl == null && (!songTitle.isNullOrBlank() || !songArtist.isNullOrBlank())) {
                val fallbackQuery = listOfNotNull(songArtist, songTitle).joinToString(" ").trim()
                if (fallbackQuery.isNotBlank()) {
                    val fallbackYtId = try {
                        val searchResult = YouTube.search(fallbackQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                            ?: YouTube.search(fallbackQuery, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
                        searchResult?.items?.firstOrNull()?.id
                    } catch (_: Exception) { null }

                    if (fallbackYtId != null && isYouTubeId(fallbackYtId)) {
                        formatUrl = extractDirectStreamUrl(fallbackYtId, preferLow)
                        if (formatUrl != null) {
                            spotifyToYtCache[videoId] = fallbackYtId
                        }
                    }
                }
            }

            if (formatUrl != null) {
                // Cache stream URL for 6 hours
                songUrlCache[videoId] = Pair(formatUrl, System.currentTimeMillis() + 6 * 60 * 60 * 1000L)
            }

            formatUrl
        }

        private suspend fun extractDirectStreamUrl(videoId: String, preferLow: Boolean): String? = withContext(Dispatchers.IO) {
            if (!isYouTubeId(videoId)) return@withContext null

            if (YouTube.cookie == null && YouTube.visitorData == null) {
                try { YouTube.visitorData = YouTube.visitorData().getOrNull() } catch (_: Exception) {}
            }

            var formatUrl: String? = null
            var usedClient = "ANDROID"

            kotlinx.coroutines.coroutineScope {
                val clientsToTry = listOf(
                    com.echo.innertube.models.YouTubeClient.IOS,
                    com.echo.innertube.models.YouTubeClient.WEB_REMIX,
                    com.echo.innertube.models.YouTubeClient.ANDROID_VR_NO_AUTH,
                    com.echo.innertube.models.YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER
                )

                val httpClient = okhttp3.OkHttpClient.Builder()
                    .proxy(YouTube.proxy)
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .fastFallback(true)
                    .build()

                val channel = kotlinx.coroutines.channels.Channel<Pair<String, String>?>(clientsToTry.size + 1)

                // Job 1: NewPipe Extraction
                launch(Dispatchers.IO) {
                    try {
                        val fallbackStreams = YouTube.getNewPipeStreamUrls(videoId)
                        val sortedFallback = fallbackStreams.filter { it.first == 251 || it.first == 140 || it.first == 250 || it.first == 249 || it.first == 139 || it.first == 171 }
                            .sortedBy { itag ->
                                when (itag.first) {
                                    139 -> 48
                                    249 -> 50
                                    250 -> 70
                                    140 -> 128
                                    171 -> 128
                                    251 -> 160
                                    else -> 128
                                }
                            }
                        val fallback = if (preferLow) {
                            sortedFallback.firstOrNull() ?: fallbackStreams.firstOrNull()
                        } else {
                            sortedFallback.lastOrNull() ?: fallbackStreams.firstOrNull()
                        }
                        if (fallback != null) {
                            android.util.Log.d("MusicPlayer", "Fast playback using NEWPIPE")
                            channel.trySend(Pair(fallback.second, "ANDROID"))
                            return@launch
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicPlayer", "NewPipe fallback failed: ${e.message}")
                    }
                    channel.trySend(null)
                }

                val jobs = clientsToTry.map { client ->
                    launch(Dispatchers.IO) {
                        try {
                            val signatureTimestamp = if (client.useSignatureTimestamp) com.echo.innertube.NewPipeUtils.getSignatureTimestamp(videoId).getOrNull() else null
                            val playerResponse = YouTube.player(videoId, null, client, signatureTimestamp).getOrNull()

                            val responseToUse = try {
                                if (playerResponse != null) YouTube.newPipePlayer(videoId, playerResponse) ?: playerResponse else null
                            } catch (_: Exception) {
                                playerResponse
                            }

                            val formats = responseToUse?.streamingData?.adaptiveFormats ?: emptyList()
                            val audioFormats = formats.filter { it.mimeType.startsWith("audio/") }
                            val format = if (preferLow) {
                                audioFormats.minByOrNull { it.bitrate } ?: formats.find { it.mimeType.startsWith("audio/") }
                            } else {
                                audioFormats.maxByOrNull { it.bitrate } ?: formats.find { it.mimeType.startsWith("audio/") }
                            }

                            if (format != null) {
                                val candidateUrl = try {
                                    com.echo.innertube.NewPipeUtils.getStreamUrl(format, videoId).getOrNull()
                                } catch (_: Exception) {
                                    null
                                } ?: format.url

                                if (!candidateUrl.isNullOrBlank()) {
                                    channel.trySend(Pair(candidateUrl, client.clientName))
                                    return@launch
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("MusicPlayer", "Client ${client.clientName} failed for $videoId: ${e.message}")
                        }
                        channel.trySend(null)
                    }
                }

                for (i in 0..clientsToTry.size) {
                    val result = channel.receive()
                    if (result != null) {
                        formatUrl = result.first
                        usedClient = result.second
                        jobs.forEach { it.cancel() }
                        break
                    }
                }
            }

            // Fallback: If still null, try raw NewPipe stream info directly
            if (formatUrl == null) {
                try {
                    val fallbackStreams = YouTube.getNewPipeStreamUrls(videoId)
                    val rawUrl = fallbackStreams.firstOrNull()?.second
                    if (rawUrl != null) {
                        formatUrl = rawUrl
                        usedClient = "ANDROID"
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MusicPlayer", "Final NewPipe fallback failed: ${e.message}")
                }
            }

            if (formatUrl != null && !formatUrl!!.contains("c=")) {
                formatUrl = formatUrl + (if (formatUrl!!.contains("?")) "&" else "?") + "c=" + usedClient
            }

            formatUrl
        }
    }
}
