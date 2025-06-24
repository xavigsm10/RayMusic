package com.mrtdk.liquid_glass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrtdk.glass.GlassBox
import com.mrtdk.glass.GlassBoxScope
import com.mrtdk.glass.GlassContainer
import com.mrtdk.liquid_glass.ui.theme.LiquidglassuicomponentTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiquidglassuicomponentTheme {
                GlassBoxDemo()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBoxDemo() {
    // Bottom sheet state - open to half height
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    // State for controlling the glass button parameters
    var buttonWidth by remember { mutableFloatStateOf(200f) }
    var buttonHeight by remember { mutableFloatStateOf(60f) }
    var cornerRadius by remember { mutableFloatStateOf(30f) }
    var blur by remember { mutableFloatStateOf(0.4f) }
    var scale by remember { mutableFloatStateOf(0.3f) }
    var distortion by remember { mutableFloatStateOf(0f) }
    var elevation by remember { mutableFloatStateOf(8f) }
    var tintRed by remember { mutableFloatStateOf(255f) }
    var tintGreen by remember { mutableFloatStateOf(255f) }
    var tintBlue by remember { mutableFloatStateOf(255f) }
    var tintAlpha by remember { mutableFloatStateOf(200f) }
    var darkness by remember { mutableFloatStateOf(0f) }
    var warpEdges by remember { mutableFloatStateOf(0.5f) }

    // Alignment state: 0 = Start, 1 = Center, 2 = End
    var alignment by remember { mutableIntStateOf(1) }

    // Show bottom sheet state
    var showSettings by remember { mutableStateOf(false) }

    // Reset function to default values
    fun resetToDefaults() {
        buttonWidth = 200f
        buttonHeight = 60f
        cornerRadius = 30f
        blur = 0.4f
        scale = 0.3f
        distortion = 0f
        elevation = 8f
        tintRed = 255f
        tintGreen = 255f
        tintBlue = 255f
        tintAlpha = 200f
        darkness = 0f
        warpEdges = 0.5f
        alignment = 1  // Center
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GlassContainer(
            modifier = Modifier.fillMaxSize(),
            content = {
                // Background content with scrollable text and images
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1a1a2e), // Dark navy
                                    Color(0xFF2d1b69), // Deep purple
                                    Color(0xFF4a148c), // Royal purple
                                    Color(0xFF1a237e), // Indigo
                                    Color(0xFF0d47a1), // Bright blue
                                    Color(0xFF6a1b9a), // Amethyst
                                    Color(0xFF283593), // Deep indigo
                                    Color(0xFF1565c0), // Azure
                                    Color(0xFF16213e), // Deep blue
                                    Color(0xFF0f3460)  // Ocean blue
                                ),
                            )
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(16.dp)
                ) {
                    InfoCards()
                    LoremIpsumText()

                    DemoImages()
                    ChessboardPattern()

                    Spacer(modifier = Modifier.height(64.dp))
                }
            },
            glassContent = {
                GlassButton(
                    alignment = alignment,
                    buttonWidth = buttonWidth,
                    buttonHeight = buttonHeight,
                    cornerRadius = cornerRadius,
                    scale = scale,
                    blur = blur,
                    distortion = distortion,
                    elevation = elevation,
                    tintRed = tintRed,
                    tintGreen = tintGreen,
                    tintBlue = tintBlue,
                    tintAlpha = tintAlpha,
                    darkness = darkness,
                    warpEdges = warpEdges
                )

                // Settings button to open bottom sheet - positioned above modal sheet
                GlassBox(
                    modifier = Modifier
                        .clickable { showSettings = true }
                        .padding(horizontal = 16.dp, vertical = 32.dp)
                        .size(48.dp)
                        .align(Alignment.TopEnd),
                    elevation = 8.dp,
                    warpEdges = 0.8f,
                    darkness = 0.5f,
                    contentAlignment = Alignment.Center,
                    scale = 0.1f,
                    blur = 0.4f,
                    shape = RoundedCornerShape(16.dp),
                    tint = Color.LightGray,

                    ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.Black,
                    )
                }
            }
        )

        // Modal Bottom Sheet
        if (showSettings) {
            BackHandler {
                showSettings = false
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color(0xFF1a1a1a)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.safeContent)
                ) {

                    Header(onResetClick = ::resetToDefaults) {
                        showSettings = false
                    }

                    // Size controls
                    Text(
                        "Size",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SliderWithLabel(
                        label = "Width",
                        value = buttonWidth,
                        onValueChange = { buttonWidth = it },
                        valueRange = 100f..400f,
                        steps = 29
                    )

                    SliderWithLabel(
                        label = "Height",
                        value = buttonHeight,
                        onValueChange = { buttonHeight = it },
                        valueRange = 40f..120f,
                        steps = 7
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shape controls
                    Text(
                        "Shape",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SliderWithLabel(
                        label = "Corner Radius",
                        value = cornerRadius,
                        onValueChange = { cornerRadius = it },
                        valueRange = 0f..50f,
                        steps = 49
                    )

                    AlignmentSelector(
                        alignment = alignment,
                        onAlignmentChange = { alignment = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Glass effects
                    Text(
                        "Glass Effects",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SliderWithLabel(
                        label = "Scale",
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0f..2f,
                        steps = 39,
                        isPercentage = true
                    )

                    SliderWithLabel(
                        label = "Blur",
                        value = blur,
                        onValueChange = { blur = it },
                        valueRange = 0f..2f,
                        steps = 39,
                        isPercentage = true
                    )

                    SliderWithLabel(
                        label = "Distortion",
                        value = distortion,
                        onValueChange = { distortion = it },
                        valueRange = 0f..2f,
                        steps = 39,
                        isPercentage = true
                    )

                    SliderWithLabel(
                        label = "Elevation",
                        value = elevation,
                        onValueChange = { elevation = it },
                        valueRange = 0f..24f,
                        steps = 23
                    )

                    SliderWithLabel(
                        label = "Darkness",
                        value = darkness,
                        onValueChange = { darkness = it },
                        valueRange = 0f..1f,
                        steps = 19,
                        isPercentage = true
                    )

                    SliderWithLabel(
                        label = "Warp Edges",
                        value = warpEdges,
                        onValueChange = { warpEdges = it },
                        valueRange = 0f..1f,
                        steps = 19,
                        isPercentage = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tint controls
                    Text(
                        "Tint Color",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ColorSliderWithLabel(
                        label = "Red",
                        value = tintRed,
                        onValueChange = { tintRed = it },
                        valueRange = 0f..255f,
                        steps = 254,
                        color = Color.Red
                    )

                    ColorSliderWithLabel(
                        label = "Green",
                        value = tintGreen,
                        onValueChange = { tintGreen = it },
                        valueRange = 0f..255f,
                        steps = 254,
                        color = Color.Green
                    )

                    ColorSliderWithLabel(
                        label = "Blue",
                        value = tintBlue,
                        onValueChange = { tintBlue = it },
                        valueRange = 0f..255f,
                        steps = 254,
                        color = Color.Blue
                    )

                    AlphaSliderWithLabel(
                        label = "Alpha",
                        value = tintAlpha,
                        onValueChange = { tintAlpha = it },
                        valueRange = 0f..255f,
                        steps = 254,
                        currentColor = Color(
                            tintRed / 255f,
                            tintGreen / 255f,
                            tintBlue / 255f,
                            1f
                        )
                    )

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoCards() {
    repeat(3) { index ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (index) {
                    0 -> Color(0xFF667eea).copy(alpha = 0.3f)
                    1 -> Color(0xFF764ba2).copy(alpha = 0.3f)
                    else -> Color(0xFFf093fb).copy(alpha = 0.3f)
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = when (index) {
                    0 -> "Glass Effect Demo\n\nThis is a demonstration of advanced glass morphism effects with real-time parameter control. The glass elements create realistic distortion, blur, and lighting effects."
                    1 -> "Interactive Controls\n\nUse the settings panel below to adjust all parameters in real-time. You can modify size, shape, distortion, colors, and lighting effects to see how they affect the glass appearance."
                    else -> "Advanced Rendering\n\nThe glass effects are implemented using custom AGSL shaders that provide hardware-accelerated rendering with support for multiple glass elements, elevation shadows, and rim highlights."
                },
                modifier = Modifier.padding(16.dp),
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun LoremIpsumText() {
    Text(
        text = buildString {
            repeat(2) {
                append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ")
                append("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ")
                append("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris. ")
                append("Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore. ")
                append("Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n\n")
            }
        },
        color = Color.White.copy(alpha = 0.8f),
        fontSize = 16.sp,
        lineHeight = 24.sp,
        textAlign = TextAlign.Justify
    )
}

@Composable
private fun DemoImages() {
    repeat(4) { index ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    when (index) {
                        0 -> gridImageModifier()
                        1 -> solidColorModifier(color = Color.Black)
                        2 -> solidColorModifier(color = Color.White)
                        else -> rainbowGradientModifier()
                    }
                )
        ) {
            if (index == 1 || index == 2) {
                TextContent(if (index == 1) Color.White else Color.Black)
            }
        }
    }
}

private fun gridImageModifier(): Modifier {
    return Modifier
        .background(Color.White, RoundedCornerShape(12.dp))
        .drawBehind {
            val gridSize = 20.dp.toPx()
            val cols = (size.width / gridSize).toInt()
            val rows = (size.height / gridSize).toInt()

            // Draw vertical lines
            for (i in 0..cols) {
                val x = i * gridSize
                drawLine(
                    color = Color.Black.copy(alpha = 0.3f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw horizontal lines
            for (i in 0..rows) {
                val y = i * gridSize
                drawLine(
                    color = Color.Black.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
}

private fun solidColorModifier(color: Color): Modifier {
    return Modifier.background(
        color,
        RoundedCornerShape(12.dp)
    )
}

private fun rainbowGradientModifier(): Modifier {
    return Modifier.background(
        Brush.linearGradient(
            colors = listOf(
                Color.Red,
                Color(0xFFFF7F00), // Orange
                Color.Yellow,
                Color.Green,
                Color.Blue,
                Color(0xFF4B0082), // Indigo
                Color(0xFF9400D3)  // Violet
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        ),
        RoundedCornerShape(12.dp)
    )
}

@Composable
private fun TextContent(color: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GLASS MORPHISM",
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Advanced UI Effects",
            color = color.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Real-time rendering with\ncustom AGSL shaders",
            color = color.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun ChessboardPattern() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                val cellSize = 40.dp.toPx()
                val cols = (size.width / cellSize).toInt()
                val rows = (size.height / cellSize).toInt()

                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val isWhite = (row + col) % 2 == 0
                        val color = if (isWhite) Color.White else Color.Black

                        drawRect(
                            color = color,
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = androidx.compose.ui.geometry.Size(
                                cellSize,
                                cellSize
                            )
                        )
                    }
                }
            }
    )
}

@Composable
private fun GlassBoxScope.GlassButton(
    alignment: Int,
    buttonWidth: Float,
    buttonHeight: Float,
    cornerRadius: Float,
    scale: Float,
    blur: Float,
    distortion: Float,
    elevation: Float,
    tintRed: Float,
    tintGreen: Float,
    tintBlue: Float,
    tintAlpha: Float,
    darkness: Float,
    warpEdges: Float
) {
    val buttonAlignment = when (alignment) {
        0 -> Alignment.BottomStart
        2 -> Alignment.BottomEnd
        else -> Alignment.BottomCenter
    }

    GlassBox(
        modifier = Modifier
            .align(buttonAlignment)
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 16.dp)
            .size(buttonWidth.dp, buttonHeight.dp),
        shape = RoundedCornerShape(cornerRadius.dp),
        contentAlignment = Alignment.Center,
        scale = scale,
        blur = blur,
        centerDistortion = distortion,
        elevation = elevation.dp,
        tint = Color(
            tintRed / 255f,
            tintGreen / 255f,
            tintBlue / 255f,
            tintAlpha / 255f
        ),
        darkness = darkness,
        warpEdges = warpEdges,
    ) {
        Text(
            "Glass Button",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun Header(onResetClick: () -> Unit, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(end = 8.dp)
            )
            Text(
                "Glass Parameters",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Button(
            onClick = onResetClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF667eea).copy(alpha = 0.3f),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "Reset",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AlignmentSelector(
    alignment: Int,
    onAlignmentChange: (Int) -> Unit
) {
    Text("Alignment", color = Color.White, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("Start", "Center", "End").forEachIndexed { index, label ->
            FilterChip(
                onClick = { onAlignmentChange(index) },
                label = { Text(label) },
                selected = alignment == index,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF667eea)
                )
            )
        }
    }
}

@Composable
fun SliderWithLabel(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    isPercentage: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Text(
                text = if (isPercentage) {
                    when {
                        valueRange.endInclusive <= 1f -> "${(value * 100).roundToInt()}%"
                        valueRange.endInclusive <= 2f -> "${(value * 50).roundToInt()}%"
                        else -> "${value.roundToInt()}"
                    }
                } else "${value.roundToInt()}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF667eea),
                activeTrackColor = Color(0xFF667eea),
                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSliderWithLabel(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Text(
                text = "${value.roundToInt()}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.3f)
            ),
            track = { sliderState ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    color
                                )
                            )
                        )
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlphaSliderWithLabel(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    currentColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Text(
                text = "${value.roundToInt()}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = currentColor,
                activeTrackColor = currentColor,
                inactiveTrackColor = currentColor.copy(alpha = 0.3f)
            ),
            track = { sliderState ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    currentColor
                                )
                            )
                        )
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
