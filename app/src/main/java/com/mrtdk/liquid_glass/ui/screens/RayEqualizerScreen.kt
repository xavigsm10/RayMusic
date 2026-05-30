package com.mrtdk.liquid_glass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.playback.eq.EqualizerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RayEqualizerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var eqEnabled by remember { mutableStateOf(EqualizerService.isEnabled) }
    var isAdvanced by remember { mutableStateOf(EqualizerService.isAdvancedMode) }
    var preampValue by remember { mutableStateOf(EqualizerService.preamp.toFloat()) }

    val simpleGains = remember { mutableStateListOf<Float>() }
    val advancedGains = remember { mutableStateListOf<Float>() }

    // Sync from service on start
    LaunchedEffect(Unit) {
        simpleGains.clear()
        EqualizerService.getSimpleGains().forEach { simpleGains.add(it.toFloat()) }
        
        advancedGains.clear()
        EqualizerService.getAdvancedGains().forEach { advancedGains.add(it.toFloat()) }
    }

    val appLang = com.mrtdk.liquid_glass.data.LibraryManager.getAppLanguage(context)
    val isEs = appLang.startsWith("es")
    val title = if (isEs) "RayEqualizador" else "RayEqualizer"

    // Helper to refresh gains from service
    fun refreshGains() {
        simpleGains.clear()
        EqualizerService.getSimpleGains().forEach { simpleGains.add(it.toFloat()) }
        advancedGains.clear()
        EqualizerService.getAdvancedGains().forEach { advancedGains.add(it.toFloat()) }
        preampValue = EqualizerService.preamp.toFloat()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = stringResource(R.string.back_action),
                    tint = Color(0xFFFA243C)
                )
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            // Reset Button
            if (eqEnabled) {
                IconButton(onClick = {
                    EqualizerService.reset()
                    refreshGains()
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (isEs) "Restablecer" else "Reset",
                        tint = Color(0xFFFA243C)
                    )
                }
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 120.dp) // Extra bottom padding for bottom nav / mini player
        ) {
            // Enable Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEs) "Ecualizador" else "Equalizer",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEs) "Procesamiento de audio en tiempo real" else "Real-time audio processing",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = eqEnabled,
                        onCheckedChange = { checked ->
                            eqEnabled = checked
                            EqualizerService.setEnabled(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFA243C),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF2C2C2E)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (eqEnabled) {
                // Segmented Mode Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Simple Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isAdvanced) Color(0xFF2C2C2E) else Color.Transparent)
                                .clickable {
                                    isAdvanced = false
                                    EqualizerService.setAdvancedMode(false)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEs) "Simple" else "Simple",
                                color = if (!isAdvanced) Color.White else Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Advanced Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAdvanced) Color(0xFF2C2C2E) else Color.Transparent)
                                .clickable {
                                    isAdvanced = true
                                    EqualizerService.setAdvancedMode(true)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEs) "Avanzado" else "Advanced",
                                color = if (isAdvanced) Color.White else Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preamp Slider
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Preamp",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%+.1f dB", preampValue),
                                color = Color(0xFFFA243C),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = preampValue,
                            onValueChange = { value ->
                                preampValue = value
                                EqualizerService.setPreamp(value.toDouble())
                            },
                            valueRange = -15f..15f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFFFA243C),
                                inactiveTrackColor = Color(0xFF2C2C2E)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Equalizer Vertical Sliders (fixed height so it works inside scroll)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isAdvanced) {
                            // Simple Mode (3 Sliders: Graves, Medios, Agudos)
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (simpleGains.size >= 3) {
                                    val labels = listOf(
                                        if (isEs) "Graves" else "Bass",
                                        if (isEs) "Medios" else "Mids",
                                        if (isEs) "Agudos" else "Treble"
                                    )
                                    val subLabels = listOf("31 - 250 Hz", "500 - 4 kHz", "8 - 16 kHz")

                                    for (i in 0..2) {
                                        VerticalSlider(
                                            value = simpleGains[i],
                                            onValueChange = { newValue ->
                                                simpleGains[i] = newValue
                                                EqualizerService.setSimpleGains(
                                                    simpleGains[0].toDouble(),
                                                    simpleGains[1].toDouble(),
                                                    simpleGains[2].toDouble()
                                                )
                                            },
                                            label = labels[i],
                                            subLabel = subLabels[i],
                                            modifier = Modifier.width(90.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Advanced Mode (10 Frequencies)
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (advancedGains.size >= 10) {
                                    val labels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
                                    for (i in 0..9) {
                                        VerticalSlider(
                                            value = advancedGains[i],
                                            onValueChange = { newValue ->
                                                advancedGains[i] = newValue
                                                EqualizerService.setAdvancedGain(i, newValue.toDouble())
                                            },
                                            label = labels[i],
                                            subLabel = "Hz",
                                            modifier = Modifier.width(55.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reset Button
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            EqualizerService.reset()
                            refreshGains()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFFFA243C),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEs) "Restablecer Ecualizador" else "Reset Equalizer",
                            color = Color(0xFFFA243C),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ========== Preset Categories ==========
                val categories = EqualizerService.presetCategories

                categories.forEach { category ->
                    val categoryTitle = when (category.key) {
                        "ray_signature" -> "RaySignature"
                        "dolby_atmos" -> "Dolby Atmos"
                        "dirac_audio" -> "Dirac Audio"
                        else -> category.key
                    }

                    // Category Header
                    Text(
                        text = categoryTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )

                    // Presets Grid (FlowRow-like using Rows)
                    val presetsPerRow = 3
                    val chunkedPresets = category.presets.chunked(presetsPerRow)
                    chunkedPresets.forEach { rowPresets ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                val displayName = getPresetDisplayName(preset, isEs)

                                // Check if current gains match this preset
                                val isSelected = if (isAdvanced) {
                                    preset.advancedGains.map { it.toFloat() }
                                        .zip(advancedGains)
                                        .all { (p, g) -> kotlin.math.abs(p - g) < 0.1f }
                                } else {
                                    preset.simpleGains.map { it.toFloat() }
                                        .zip(simpleGains)
                                        .all { (p, g) -> kotlin.math.abs(p - g) < 0.1f }
                                }

                                val chipColor = when (category.key) {
                                    "dolby_atmos" -> if (isSelected) Color(0xFF6366F1) else Color(0xFF1C1C1E)
                                    "dirac_audio" -> if (isSelected) Color(0xFF059669) else Color(0xFF1C1C1E)
                                    else -> if (isSelected) Color(0xFFFA243C) else Color(0xFF1C1C1E)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipColor)
                                        .clickable {
                                            EqualizerService.applyPreset(preset)
                                            refreshGains()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayName,
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                            // Fill remaining cells with invisible spacers
                            val emptySlots = presetsPerRow - rowPresets.size
                            repeat(emptySlots) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                // Disabled Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEs) "Ecualizador Desactivado" else "Equalizer Disabled",
                        color = Color.DarkGray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun getPresetDisplayName(preset: EqualizerService.Preset, isEs: Boolean): String {
    return when (preset.key) {
        "flat" -> if (isEs) "Plano" else "Flat"
        "ray_signature_preset" -> "RaySignature"
        "acoustic" -> if (isEs) "Acústica" else "Acoustic"
        "3d_stage" -> "3D Stage"
        "bass_booster" -> if (isEs) "Más Graves" else "Bass Boost"
        "pure_clarity" -> if (isEs) "Claridad Pura" else "Pure Clarity"
        "soft_bass" -> if (isEs) "Graves Suaves" else "Soft Bass"
        "electronic" -> if (isEs) "Electrónica" else "Electronic"
        "rock" -> "Rock"
        "pop" -> "Pop"
        "jazz" -> "Jazz"
        "voice" -> if (isEs) "Voz" else "Voice"
        "dolby_open" -> "Dolby Open"
        "dolby_rich" -> "Dolby Rich"
        "dolby_focused" -> "Dolby Focused"
        "dirac_music" -> "Dirac Music"
        "dirac_movie" -> "Dirac Movie"
        "dirac_game" -> "Dirac Game"
        else -> preset.name
    }
}

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = -12f..12f,
    label: String,
    subLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxHeight().padding(vertical = 16.dp)
    ) {
        Text(
            text = String.format(java.util.Locale.US, "%+.1f", value),
            color = Color(0xFFFA243C),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Track Box
        Box(
            modifier = Modifier
                .weight(1f)
                .width(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2C2C2E))
                .pointerInput(valueRange) {
                    detectTapGestures { offset ->
                        val height = size.height
                        if (height > 0) {
                            val fraction = 1f - (offset.y / height)
                            val newValue = (valueRange.start + fraction * (valueRange.endInclusive - valueRange.start))
                                .coerceIn(valueRange.start, valueRange.endInclusive)
                            onValueChange(newValue)
                        }
                    }
                }
                .pointerInput(valueRange) {
                    var currentY = 0f
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Calculate current Y based on fractional value
                            val rangeLen = valueRange.endInclusive - valueRange.start
                            val fraction = (value - valueRange.start) / rangeLen
                            currentY = size.height * (1f - fraction)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val height = size.height
                            if (height > 0) {
                                currentY += dragAmount.y
                                val fraction = 1f - (currentY / height)
                                val newValue = (valueRange.start + fraction * (valueRange.endInclusive - valueRange.start))
                                    .coerceIn(valueRange.start, valueRange.endInclusive)
                                onValueChange(newValue)
                            }
                        }
                    )
                }
        ) {
            val rangeLen = valueRange.endInclusive - valueRange.start
            val fraction = ((value - valueRange.start) / rangeLen).coerceIn(0f, 1f)
            
            // Colored fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFFFA243C))
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = subLabel,
            color = Color.Gray,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}
