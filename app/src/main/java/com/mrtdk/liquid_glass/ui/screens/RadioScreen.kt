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
        targetValue = if (isListening) 1.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.5f else 0f,
        targetValue = if (isListening) 0.0f else 0f,
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
                val samples = withContext(Dispatchers.IO) {
                    recordMicPcm16Mono(sampleRateHz = 16000, recordMs = 4200L).first
                }
                isProcessing = true
                val signature = withContext(Dispatchers.Default) {
                    ShazamSignatureGenerator().apply { feedPcm16Mono(samples) }.nextSignatureOrNull()
                }
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
): Pair<ShortArray, Int> = withContext(Dispatchers.IO) {
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

    val totalSamples = ((recordMs / 1000.0) * sampleRateHz).toInt().coerceAtLeast(sampleRateHz)
    val output = ShortArray(totalSamples)
    val buffer = ShortArray(minBuffer / 2)

    try {
        record.startRecording()

        var written = 0
        while (written < output.size && isActive) {
            val read = record.read(buffer, 0, minOf(buffer.size, output.size - written))
            if (read > 0) {
                System.arraycopy(buffer, 0, output, written, read)
                written += read
            }
        }

        if (written <= 0) {
            ShortArray(0) to sampleRateHz
        } else {
            output.copyOf(written) to sampleRateHz
        }
    } finally {
        runCatching { record.stop() }
        runCatching { record.release() }
    }
}
