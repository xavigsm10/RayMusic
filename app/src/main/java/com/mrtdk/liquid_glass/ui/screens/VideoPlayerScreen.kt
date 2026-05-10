package com.mrtdk.liquid_glass.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.echo.innertube.YouTube
import com.echo.innertube.models.YouTubeClient
import com.echo.innertube.models.response.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import android.util.Log

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    
    // Force landscape orientation and enable fullscreen immersive mode
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        onDispose {
            // Restore original orientation
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isLoading = false
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                errorMessage = error.message ?: "Playback error"
                isLoading = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(videoId) {
        isLoading = true
        errorMessage = null
        
        try {
            // Fetch video stream URL from YouTube using parallel requests for faster loading
            withContext(Dispatchers.IO) {
                // Try first 3 most reliable clients in parallel for faster response
                val priorityClients = listOf(
                    YouTubeClient.ANDROID_VR_NO_AUTH,
                    YouTubeClient.IOS,
                    YouTubeClient.WEB
                )
                
                Log.d("VideoPlayer", "Starting parallel requests for videoId: $videoId")
                
                // Launch parallel requests to all priority clients
                val deferredResults = priorityClients.map { client ->
                    async {
                        try {
                            Log.d("VideoPlayer", "Requesting from ${client.clientName}")
                            val signatureTimestamp = if (client.useSignatureTimestamp) {
                                com.echo.innertube.NewPipeUtils.getSignatureTimestamp(videoId).getOrNull()
                            } else null
                            
                            val result = YouTube.player(videoId, client = client, signatureTimestamp = signatureTimestamp)
                            client to result
                        } catch (e: Exception) {
                            Log.e("VideoPlayer", "Error with ${client.clientName}: ${e.message}")
                            client to Result.failure<PlayerResponse>(e)
                        }
                    }
                }
                
                // Wait for all requests to complete
                val results = deferredResults.awaitAll()
                
                var bestUrl: String? = null
                var lastError: String? = null
                var usedClientName = "IOS"
                
                // Process results in order of priority
                for ((client, result) in results) {
                    result.onSuccess { playerResponse ->
                        Log.d("VideoPlayer", "Got response from ${client.clientName}, status: ${playerResponse.playabilityStatus.status}")
                        
                        if (playerResponse.playabilityStatus.status != "OK") {
                            lastError = playerResponse.playabilityStatus.reason ?: "Playback not available"
                            Log.d("VideoPlayer", "Playability status not OK: $lastError")
                            return@onSuccess
                        }
                        
                        // Get the best video stream
                        val streamingData = playerResponse.streamingData
                        if (streamingData == null) {
                            Log.d("VideoPlayer", "No streaming data from ${client.clientName}")
                            lastError = "No streaming data available"
                            return@onSuccess
                        }
                        
                        val formats = streamingData.formats ?: emptyList()
                        val adaptiveFormats = streamingData.adaptiveFormats ?: emptyList()
                        
                        Log.d("VideoPlayer", "Found ${formats.size} regular formats and ${adaptiveFormats.size} adaptive formats")
                        
                        // Prefer formats with both video and audio (regular formats)
                        val formatsToTry = formats.filter { format ->
                            format.mimeType?.contains("video") == true
                        }.sortedByDescending { it.width ?: 0 } // Try highest resolution first
                        
                        for (format in formatsToTry) {
                            val decipheredUrl = try {
                                com.echo.innertube.NewPipeUtils.getStreamUrl(format, videoId).getOrNull()
                            } catch (e: Exception) {
                                null
                            }
                            
                            val candidateUrl = decipheredUrl ?: format.url
                            
                            if (candidateUrl != null) {
                                bestUrl = candidateUrl
                                usedClientName = client.clientName
                                Log.d("VideoPlayer", "Found format with ${client.clientName}: ${format.mimeType}, quality: ${format.qualityLabel}")
                                break
                            }
                        }
                        
                        if (bestUrl == null) {
                            Log.d("VideoPlayer", "No valid format found with ${client.clientName}")
                            lastError = "No playable format found"
                        }
                    }.onFailure { error ->
                        lastError = error.message ?: "Failed to load video"
                        Log.e("VideoPlayer", "Error with ${client.clientName}: $lastError")
                    }
                    
                    // If we found a working URL, stop trying other clients
                    if (bestUrl != null) {
                        Log.d("VideoPlayer", "Successfully got stream URL from ${client.clientName}")
                        break
                    }
                }
                
                // If parallel requests failed, try fallback clients sequentially
                if (bestUrl == null) {
                    Log.d("VideoPlayer", "Trying fallback clients")
                    val fallbackClients = listOf(
                        YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
                        YouTubeClient.ANDROID_VR_1_61_48
                    )
                    
                    for (client in fallbackClients) {
                        Log.d("VideoPlayer", "Trying fallback client ${client.clientName}")
                        
                        val result = YouTube.player(videoId, client = client)
                        result.onSuccess { playerResponse ->
                            if (playerResponse.playabilityStatus.status == "OK") {
                                val streamingData = playerResponse.streamingData
                                if (streamingData != null) {
                                    val formats = streamingData.formats ?: emptyList()
                                    val adaptiveFormats = streamingData.adaptiveFormats ?: emptyList()
                                    
                                    val formatsToTry = formats.filter { format ->
                                        format.mimeType?.contains("video") == true
                                    }.sortedByDescending { it.width ?: 0 }
                                    
                                    for (format in formatsToTry) {
                                        val decipheredUrl = try {
                                            com.echo.innertube.NewPipeUtils.getStreamUrl(format, videoId).getOrNull()
                                        } catch (e: Exception) {
                                            null
                                        }
                                        
                                        val candidateUrl = decipheredUrl ?: format.url
                                        
                                        if (candidateUrl != null) {
                                            bestUrl = candidateUrl
                                            Log.d("VideoPlayer", "Found format with fallback ${client.clientName}")
                                            break
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (bestUrl != null) break
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (bestUrl != null) {
                        // Append client name if missing to prevent 403
                        val finalUrl = if (bestUrl!!.contains("c=")) bestUrl!! else {
                            bestUrl!! + (if (bestUrl!!.contains("?")) "&" else "?") + "c=IOS"
                        }
                        
                        videoUrl = finalUrl
                        val mediaItem = MediaItem.fromUri(finalUrl)
                        
                        val ua = when {
                            usedClientName.contains("IOS", ignoreCase = true) -> "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X; en_US)"
                            usedClientName.contains("ANDROID", ignoreCase = true) -> "com.google.android.youtube/19.25.39 (Linux; U; Android 14; en_US; Pixel 8 Pro; Build/AP1A.240305.019)"
                            else -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
                        }
                        
                        val dataSourceFactory = DefaultHttpDataSource.Factory()
                            .setUserAgent(ua)
                            .setConnectTimeoutMs(10000)
                            .setReadTimeoutMs(10000)
                        
                        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaItem)
                        
                        exoPlayer.setMediaSource(mediaSource)
                        exoPlayer.prepare()
                        Log.d("VideoPlayer", "Player prepared and ready")
                    } else {
                        errorMessage = lastError ?: "No video stream found"
                        isLoading = false
                        Log.e("VideoPlayer", "Failed to get stream URL: $errorMessage")
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Error loading video"
            isLoading = false
            Log.e("VideoPlayer", "Exception while loading video", e)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = "Loading video...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Error message
        errorMessage?.let { error ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = error,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = onBack
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}
