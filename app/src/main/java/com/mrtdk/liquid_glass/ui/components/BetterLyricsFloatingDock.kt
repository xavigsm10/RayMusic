package com.mrtdk.liquid_glass.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mrtdk.liquid_glass.utils.LyricsFetchResult

@Composable
fun BetterLyricsFloatingDock(
    availableProviders: List<LyricsFetchResult>,
    currentProviderIndex: Int,
    onSelectProviderIndex: (Int) -> Unit,
    isTranslateEnabled: Boolean,
    onToggleTranslate: () -> Unit,
    offsetSeconds: Float,
    onAdjustOffset: (Float) -> Unit,
    onResetOffset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var isOffsetMenuOpen by remember { mutableStateOf(false) }

    val currentProvider = availableProviders.getOrNull(currentProviderIndex) 
        ?: LyricsFetchResult(null, "Better Lyrics", "syllable")
    
    val safeIndex = if (currentProviderIndex in availableProviders.indices) currentProviderIndex else 0
    val totalProviders = availableProviders.size.coerceAtLeast(1)

    val syncColor = when (currentProvider.syncType.lowercase()) {
        "syllable", "richsync" -> Color(0xFFFDE69B) // Gold
        "word" -> Color(0xFFAAD1FF)                 // Blue
        "line", "linesync" -> Color(0xFFC9F8DA)     // Green
        else -> Color.White.copy(alpha = 0.6f)      // Gray
    }

    Box(
        modifier = modifier
            .padding(bottom = 20.dp)
            .wrapContentSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // --- Floating Provider Dropdown Menu (Popup) ---
        if (isMenuOpen && availableProviders.isNotEmpty()) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = androidx.compose.ui.unit.IntOffset(0, -110),
                onDismissRequest = { isMenuOpen = false },
                properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
            ) {
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1C1C22).copy(alpha = 0.94f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                        .padding(vertical = 6.dp, horizontal = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        availableProviders.forEachIndexed { index, provider ->
                            val isSelected = index == safeIndex
                            val itemSyncColor = when (provider.syncType.lowercase()) {
                                "syllable", "richsync" -> Color(0xFFFDE69B)
                                "word" -> Color(0xFFAAD1FF)
                                "line", "linesync" -> Color(0xFFC9F8DA)
                                else -> Color.White.copy(alpha = 0.6f)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                    )
                                    .clickable {
                                        onSelectProviderIndex(index)
                                        isMenuOpen = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sync Type Badge Icon
                                SyncTypeBadge(
                                    syncType = provider.syncType,
                                    color = itemSyncColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = provider.providerName,
                                    color = if (isSelected) itemSyncColor else Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Floating Offset Adjuster Menu (Popup) ---
        if (isOffsetMenuOpen) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = androidx.compose.ui.unit.IntOffset(60, -110),
                onDismissRequest = { isOffsetMenuOpen = false },
                properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1C22).copy(alpha = 0.94f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onAdjustOffset(-0.5f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-0.5s", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onAdjustOffset(-0.1f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-0.1s", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onResetOffset() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Reset", color = Color(0xFFFDE69B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onAdjustOffset(0.1f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+0.1s", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onAdjustOffset(0.5f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+0.5s", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Main Floating Dock Pill (Ultra-clean, exact replica of Glassy Music screenshots) ---
        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1E1E22).copy(alpha = 0.88f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Prev Arrow
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable {
                        val prev = if (safeIndex > 0) safeIndex - 1 else totalProviders - 1
                        onSelectProviderIndex(prev)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Provider",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Provider Name, Badge & Index (e.g. Better Lyrics Portato 1/7)
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { isMenuOpen = !isMenuOpen }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SyncTypeBadge(
                    syncType = currentProvider.syncType,
                    color = syncColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = currentProvider.providerName,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${safeIndex + 1}/$totalProviders",
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Next Arrow
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable {
                        val next = (safeIndex + 1) % totalProviders
                        onSelectProviderIndex(next)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Provider",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Divider 1
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            // 2. Translation / Romanization Button (文A)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isTranslateEnabled) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onToggleTranslate() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Romanize / Translate",
                    tint = if (isTranslateEnabled) Color(0xFFFA243C) else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Divider 2
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            // 3. Timing Offset Controls (Clock + [-] [0.0s] [+])
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { isOffsetMenuOpen = !isOffsetMenuOpen },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Offset Menu",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(13.5.dp)
                    )
                }

                // Decrement Button
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onAdjustOffset(-0.1f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "—",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val offsetSign = if (offsetSeconds > 0.001f) "+" else ""
                val offsetText = String.format("%.1fs", offsetSeconds)
                Text(
                    text = "$offsetSign$offsetText",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onResetOffset() }
                        .padding(horizontal = 2.dp)
                )

                // Increment Button
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onAdjustOffset(0.1f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SyncTypeBadge(
    syncType: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Renders segmented dashes exactly like Glassy Music's icon:
    // Word sync (Portato): 3 bars
    // Syllable sync (Better Lyrics / BiniLyrics): 2 bars
    // Line sync (LRCLib / Legato / Musixmatch): 1 bar
    // Plain: 1 empty/dashed bar
    when (syncType.lowercase()) {
        "word" -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(3.dp).height(7.dp).clip(RoundedCornerShape(1.dp)).background(color))
                Box(modifier = Modifier.width(3.dp).height(7.dp).clip(RoundedCornerShape(1.dp)).background(color))
                Box(modifier = Modifier.width(3.dp).height(7.dp).clip(RoundedCornerShape(1.dp)).background(color))
            }
        }
        "syllable", "richsync" -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(4.5.dp).height(7.dp).clip(RoundedCornerShape(1.dp)).background(color))
                Box(modifier = Modifier.width(4.5.dp).height(7.dp).clip(RoundedCornerShape(1.dp)).background(color))
            }
        }
        "line", "linesync" -> {
            Box(
                modifier = modifier
                    .width(10.dp)
                    .height(6.5.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
        else -> {
            Box(
                modifier = modifier
                    .width(10.dp)
                    .height(6.5.dp)
                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(1.5.dp))
            )
        }
    }
}
