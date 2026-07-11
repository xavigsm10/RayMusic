package com.mrtdk.liquid_glass.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.mrtdk.liquid_glass.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import iad1tya.echo.music.shazamkit.Shazam
import iad1tya.echo.music.shazamkit.ShazamSignatureGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.mrtdk.liquid_glass.utils.AudioResampler
import com.mrtdk.liquid_glass.utils.DecodedAudio
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Composable
fun RadioScreen(
    innerPadding: PaddingValues,
    onSearchResult: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            isListening = true
        }
    }

    val colorPrimary = Color(0xFFFA243C) // Apple Music / liquid_glass red-pink
    
    // Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val scope = rememberCoroutineScope()
    var resultText by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(isListening) {
        if (isListening) {
            resultText = null
            try {
                val audioData = withContext(Dispatchers.IO) {
                    recordMicPcm16Mono(sampleRateHz = 44100, recordMs = 10000L)
                }
                isProcessing = true
                
                val decodedAudio = DecodedAudio(
                    data = audioData,
                    channelCount = 1,
                    sampleRate = 44100,
                    pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
                )
                
                val resampledAudio = withContext(Dispatchers.Default) {
                    AudioResampler.resample(decodedAudio, 16000).getOrNull()
                }
                
                val signature = if (resampledAudio != null) {
                    withContext(Dispatchers.Default) {
                        val shorts = ShortArray(resampledAudio.data.size / 2)
                        ByteBuffer.wrap(resampledAudio.data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                        
                        ShazamSignatureGenerator().apply { feedPcm16Mono(shorts) }.nextSignatureOrNull()
                    }
                } else null

                if (signature != null) {
                    val result = withContext(Dispatchers.IO) {
                        Shazam.recognize(signature.uri, signature.sampleDurationMs)
                    }
                    result.fold(
                        onSuccess = { res ->
                            resultText = "${res.title} - ${res.artist}"
                            // Provide a small delay so user can read the result before navigating
                            delay(1000)
                            onSearchResult("${res.title} ${res.artist}")
                        },
                        onFailure = { resultText = context.getString(R.string.radio_no_matches) }
                    )
                } else {
                    resultText = context.getString(R.string.radio_failed_signature)
                }
            } catch (e: Exception) {
                resultText = context.getString(R.string.radio_error_listening)
            } finally {
                isListening = false
                isProcessing = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isListening) {
                    // Outer Pulse
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(colorPrimary.copy(alpha = alpha))
                    )
                    // Inner Pulse
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(scale * 0.85f)
                            .clip(CircleShape)
                            .background(colorPrimary.copy(alpha = alpha * 1.5f))
                    )
                }
                
                // Big Apple Music Style Button
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(colorPrimary)
                        .clickable {
                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (!isListening && !isProcessing) {
                                    isListening = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Listening Microphone",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = when {
                    isProcessing -> stringResource(R.string.radio_processing)
                    isListening -> stringResource(R.string.radio_listening)
                    resultText != null -> resultText!!
                    else -> stringResource(R.string.radio_tap_to_identify)
                },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.radio_hear_clearly),
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private suspend fun recordMicPcm16Mono(
    sampleRateHz: Int,
    recordMs: Long,
): ByteArray = withContext(Dispatchers.IO) {
    val channel = AudioFormat.CHANNEL_IN_MONO
    val encoding = AudioFormat.ENCODING_PCM_16BIT
    val minBuffer = AudioRecord.getMinBufferSize(sampleRateHz, channel, encoding).coerceAtLeast(4096)
    val record = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRateHz,
        channel,
        encoding,
        minBuffer,
    )

    val outputStream = ByteArrayOutputStream()
    val buffer = ByteArray(minBuffer)
    val startTime = System.currentTimeMillis()

    try {
        record.startRecording()

        while (System.currentTimeMillis() - startTime < recordMs && isActive) {
            val bytesRead = record.read(buffer, 0, minBuffer)
            if (bytesRead > 0) {
                outputStream.write(buffer, 0, bytesRead)
            }
        }
    } finally {
        runCatching { record.stop() }
        runCatching { record.release() }
    }

    outputStream.toByteArray()
}
