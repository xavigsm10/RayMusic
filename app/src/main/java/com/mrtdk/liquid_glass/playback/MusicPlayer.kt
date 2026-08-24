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
import com.echo.innertube.models.SongItem
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

    private var retryAttempts = 0
    private val MAX_RETRY_ATTEMPTS = 3

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
                    val ltManager = com.mrtdk.liquid_glass.listentogether.ListenTogetherManager.getInstance(context)
                    if (ltManager.isInRoom && ltManager.isHost && !ltManager.isSyncing) {
                        ltManager.broadcastPlayPause(isPlaying)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = controller?.duration ?: 0L
                        _playbackError.value = null
                        retryAttempts = 0
                        com.mrtdk.liquid_glass.utils.BotDetectionMitigator.notifyPlaybackSuccess()
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        _songEnded.value = _songEnded.value + 1
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val rootCause = error.cause ?: error
                    val errorMsg = error.message.orEmpty()
                    val causeMsg = rootCause.message.orEmpty()
                    val is403OrNetwork = errorMsg.contains("403") ||
                            causeMsg.contains("403") ||
                            error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                            error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                            error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                            error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                            rootCause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException

                    val currentMediaItem = controller?.currentMediaItem
                    val currentVideoId = currentMediaItem?.mediaId

                    if (is403OrNetwork && retryAttempts < MAX_RETRY_ATTEMPTS) {
                        retryAttempts++
                        android.util.Log.d("MusicPlayer", "Recoverable playback error ($errorMsg), silent recovery attempt #$retryAttempts")

                        scope.launch(Dispatchers.IO) {
                            if (!currentVideoId.isNullOrBlank()) {
                                clearCache(currentVideoId)
                                com.echo.innertube.YouTubeExtractor.clearCache()
                            }
                            if (errorMsg.contains("403") || causeMsg.contains("403") || rootCause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                                com.mrtdk.liquid_glass.utils.BotDetectionMitigator.notifyPlaybackFailure(YouTube.cookie != null, error.message)
                                com.mrtdk.liquid_glass.utils.BotDetectionMitigator.rotateGuestSession()
                            }
                            delay(350)
                            withContext(Dispatchers.Main) {
                                reloadCurrentSong()
                            }
                        }
                        return
                    }

                    _playbackError.value = "Playback error (code ${error.errorCode}): ${error.message}\nCause: ${rootCause.localizedMessage ?: rootCause.toString()}"
                }
                
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _duration.value = controller?.duration ?: 0L
                    _playbackError.value = null
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                        // ListenTogether sync broadcast
                        val ltManager = com.mrtdk.liquid_glass.listentogether.ListenTogetherManager.getInstance(context)
                        if (ltManager.isInRoom && ltManager.isHost && !ltManager.isSyncing) {
                            ltManager.broadcastSeek(newPosition.positionMs)
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

        fun getCachedUrl(videoId: String): String? {
            return songUrlCache[videoId]?.takeIf { it.second > System.currentTimeMillis() }?.first
        }

        fun clearCache(videoId: String) {
            songUrlCache.remove(videoId)
            spotifyToYtCache.remove(videoId)
            songMetadataCache.remove(videoId)
        }

        private fun isYouTubeId(id: String): Boolean {
            return id.length == 11 && !id.contains(" ") && !id.contains(":") && !id.contains("/")
        }

        suspend fun resolveUrl(videoId: String): String? = withContext(Dispatchers.IO) {
            val quality = com.mrtdk.liquid_glass.data.LibraryManager.getString("audio_quality", "auto") ?: "auto"
            val preferLow = quality.equals("low", ignoreCase = true)

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
                            val songItems = searchResult?.items?.filterIsInstance<SongItem>().orEmpty()
                            songItems.firstOrNull { !it.isVideoSong }?.id
                                ?: songItems.firstOrNull()?.id
                                ?: searchResult?.items?.firstOrNull()?.id
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

            // Fallback: If direct stream extraction failed, attempt metadata lookup and search fallback
            if (formatUrl == null) {
                var resolvedTitle = songTitle
                var resolvedArtist = songArtist

                if (resolvedTitle.isNullOrBlank() && resolvedArtist.isNullOrBlank()) {
                    try {
                        val nextResult = YouTube.next(com.echo.innertube.models.WatchEndpoint(videoId = targetVideoId)).getOrNull()
                        val item = nextResult?.items?.firstOrNull()
                        if (item != null) {
                            resolvedTitle = item.title
                            resolvedArtist = item.artists?.firstOrNull()?.name
                        }
                    } catch (_: Exception) {}
                }

                if (!resolvedTitle.isNullOrBlank() || !resolvedArtist.isNullOrBlank()) {
                    val fallbackQuery = listOfNotNull(resolvedArtist, resolvedTitle).joinToString(" ").trim()
                    if (fallbackQuery.isNotBlank()) {
                        val fallbackYtId = try {
                            val searchResult = YouTube.search(fallbackQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                ?: YouTube.search(fallbackQuery, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
                            val songItems = searchResult?.items?.filterIsInstance<SongItem>().orEmpty()
                            songItems.firstOrNull { !it.isVideoSong }?.id
                                ?: songItems.firstOrNull()?.id
                                ?: searchResult?.items?.firstOrNull()?.id
                        } catch (_: Exception) { null }

                        if (fallbackYtId != null && isYouTubeId(fallbackYtId) && fallbackYtId != targetVideoId) {
                            formatUrl = extractDirectStreamUrl(fallbackYtId, preferLow)
                            if (formatUrl != null) {
                                spotifyToYtCache[videoId] = fallbackYtId
                                android.util.Log.d("MusicPlayer", "Recovered dead videoId $videoId -> playable $fallbackYtId for '$fallbackQuery'")
                            }
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

            try {
                com.mrtdk.liquid_glass.utils.YTPlayerUtils.resolveStreamUrl(videoId, preferLow)
            } catch (e: Exception) {
                android.util.Log.e("MusicPlayer", "YTPlayerUtils resolution failed: ${e.message}")
                null
            }
        }
    }
}
