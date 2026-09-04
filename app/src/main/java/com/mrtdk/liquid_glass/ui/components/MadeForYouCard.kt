package com.mrtdk.liquid_glass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MadeForYouCardContent(
    title: String,
    artistsSubtitle: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    isHero: Boolean = false
) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = if (gradientColors.size >= 2) gradientColors else listOf(Color(0xFFE62B00), Color(0xFFFF4100)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .padding(if (isHero) 24.dp else 18.dp)
    ) {
        // Top-right: RayMusic label
        Text(
            text = "RayMusic",
            color = Color.White.copy(alpha = 0.95f),
            fontSize = if (isHero) 15.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Center: Title
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = if (isHero) 38.sp else 32.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = if (isHero) 44.sp else 36.sp
            )
        }

        // Bottom: Artists list
        if (artistsSubtitle.isNotBlank()) {
            Text(
                text = artistsSubtitle,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = if (isHero) 13.5.sp else 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = if (isHero) 18.sp else 16.sp,
                maxLines = if (isHero) 4 else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
