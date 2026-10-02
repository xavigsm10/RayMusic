package com.mrtdk.liquid_glass.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.utils.Updater
import com.mrtdk.glass.GlassBoxScope
import com.mrtdk.glass.GlassBox
import com.mrtdk.liquid_glass.data.LibraryManager
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun GlassBoxScope.UpdateDialog(
    releaseInfo: Updater.ReleaseInfo,
    onDismiss: () -> Unit
) {
    UpdateDialogContent(
        glassScope = this,
        releaseInfo = releaseInfo,
        onDismiss = onDismiss
    )
}

@Composable
fun UpdateDialog(
    releaseInfo: Updater.ReleaseInfo,
    onDismiss: () -> Unit
) {
    UpdateDialogContent(
        glassScope = null,
        releaseInfo = releaseInfo,
        onDismiss = onDismiss
    )
}

@Composable
private fun UpdateDialogContent(
    glassScope: GlassBoxScope?,
    releaseInfo: Updater.ReleaseInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var downloadComplete by remember { mutableStateOf(false) }
    var apkFile by remember { mutableStateOf<File?>(null) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "dialogAlpha"
    )
    val cornerRadius by animateFloatAsState(
        targetValue = if (visible) 24f else 80f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "dialogCornerRadius"
    )

    val dominantColor by LibraryManager.currentDominantColor.collectAsState()

    val isSolid = com.mrtdk.liquid_glass.BuildConfig.IS_LITE ||
            com.mrtdk.glass.LocalGlassStyle.current == "solid"
    val isDark = com.mrtdk.liquid_glass.ui.theme.ThemeManager.isDarkMode.collectAsState().value

    fun handleDismiss() {
        if (!downloading) {
            visible = false
            onDismiss()
        }
    }

    BackHandler(enabled = visible) {
        handleDismiss()
    }

    val dimColor = rememberAndroidLiquidGlassDimColor(isDark)

    // Full-screen overlay dimming
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dimColor.copy(alpha = dimColor.alpha * alpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val menuWidth = 345.dp

        val cardModifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .width(menuWidth)
            .wrapContentHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Prevent dismissing on card click */ }

        if (isSolid) {
            val surfaceBg = if (isDark) dominantColor.copy(alpha = 0.12f).compositeOver(Color(0xFF22232A)) else Color(0xFFFFFFFF)
            val surfaceBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
            Surface(
                modifier = cardModifier,
                shape = RoundedCornerShape(cornerRadius.dp),
                color = surfaceBg,
                shadowElevation = 16.dp,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, surfaceBorder)
            ) {
                UpdateInnerContent(
                    context = context,
                    releaseInfo = releaseInfo,
                    isSolid = true,
                    isDark = isDark,
                    downloading = downloading,
                    progress = progress,
                    downloadComplete = downloadComplete,
                    apkFile = apkFile,
                    onStartDownload = {
                        downloading = true
                        Updater.downloadApk(context, releaseInfo.downloadUrl, { p ->
                            progress = p
                        }, { file ->
                            downloading = false
                            if (file != null) {
                                downloadComplete = true
                                apkFile = file
                            } else {
                                handleDismiss()
                            }
                        })
                    },
                    onInstallApk = {
                        apkFile?.let { Updater.installApk(context, it) }
                    },
                    onDismiss = { handleDismiss() }
                )
            }
        } else if (glassScope != null) {
            glassScope.GlassBox(
                modifier = cardModifier,
                blur = 0.85f,
                scale = 0.02f,
                centerDistortion = 0.1f,
                warpEdges = 0.4f,
                elevation = 6.dp,
                shape = RoundedCornerShape(cornerRadius.dp),
                tint = dominantColor.copy(alpha = 0.28f),
                darkness = 0.25f
            ) {
                UpdateInnerContent(
                    context = context,
                    releaseInfo = releaseInfo,
                    isSolid = false,
                    isDark = isDark,
                    downloading = downloading,
                    progress = progress,
                    downloadComplete = downloadComplete,
                    apkFile = apkFile,
                    onStartDownload = {
                        downloading = true
                        Updater.downloadApk(context, releaseInfo.downloadUrl, { p ->
                            progress = p
                        }, { file ->
                            downloading = false
                            if (file != null) {
                                downloadComplete = true
                                apkFile = file
                            } else {
                                handleDismiss()
                            }
                        })
                    },
                    onInstallApk = {
                        apkFile?.let { Updater.installApk(context, it) }
                    },
                    onDismiss = { handleDismiss() }
                )
            }
        } else {
            val backdrop = LocalBackdrop.current
            Box(
                modifier = cardModifier
                    .androidLiquidGlassEffect(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(cornerRadius.dp) },
                        isDark = isDark,
                        refractionHeight = 24.dp,
                        refractionAmount = 48.dp
                    )
            ) {
                UpdateInnerContent(
                    context = context,
                    releaseInfo = releaseInfo,
                    isSolid = false,
                    isDark = isDark,
                    downloading = downloading,
                    progress = progress,
                    downloadComplete = downloadComplete,
                    apkFile = apkFile,
                    onStartDownload = {
                        downloading = true
                        Updater.downloadApk(context, releaseInfo.downloadUrl, { p ->
                            progress = p
                        }, { file ->
                            downloading = false
                            if (file != null) {
                                downloadComplete = true
                                apkFile = file
                            } else {
                                handleDismiss()
                            }
                        })
                    },
                    onInstallApk = {
                        apkFile?.let { Updater.installApk(context, it) }
                    },
                    onDismiss = { handleDismiss() }
                )
            }
        }
    }
}

@Composable
private fun UpdateInnerContent(
    context: android.content.Context,
    releaseInfo: Updater.ReleaseInfo,
    isSolid: Boolean,
    isDark: Boolean,
    downloading: Boolean,
    progress: Float,
    downloadComplete: Boolean,
    apkFile: File?,
    onStartDownload: () -> Unit,
    onInstallApk: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp)
    ) {
                // Rocket / Sparkle icon badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                listOf(
                                    Color(0xFFFA243C).copy(alpha = 0.35f),
                                    Color(0xFFFA243C).copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFFFA243C).copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = Color(0xFFFA243C),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.actualizacion_disponible),
                    color = if (isDark) Color.White else Color(0xFF1C1C1E),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFA243C).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "v${releaseInfo.versionName}",
                            color = Color(0xFFFA243C),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RayMusic",
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF1C1C1E).copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Scrollable Changelog Section
                val scrollState = rememberScrollState()
                val bodyText = releaseInfo.body?.takeIf { it.isNotBlank() }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(min = 140.dp, max = 260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSolid) {
                                if (isDark) Color(0xFF2C2D35) else Color(0xFFF1F2F8)
                            } else {
                                Color.White.copy(alpha = 0.05f)
                            }
                        )
                        .border(
                            if (isSolid) 1.dp else 0.5.dp,
                            if (isSolid) {
                                if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                            } else {
                                Color.White.copy(alpha = 0.12f)
                            },
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Novedades de la versión:",
                            color = if (isDark) Color.White else Color(0xFF1C1C1E),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (bodyText != null && bodyText.contains("\n") && !bodyText.equals("null", ignoreCase = true)) {
                            // Render GitHub release body lines
                            val lines = bodyText.lines()
                            lines.forEach { line ->
                                val trimmed = line.trim()
                                when {
                                    trimmed.startsWith("### ") || trimmed.startsWith("## ") -> {
                                        Text(
                                            text = trimmed.removePrefix("### ").removePrefix("## "),
                                            color = Color(0xFFFA243C),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                        )
                                    }
                                    trimmed.startsWith("* ") || trimmed.startsWith("- ") -> {
                                        val content = trimmed.substring(2)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "•",
                                                color = Color(0xFFFA243C),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text(
                                                text = content.replace("**", ""),
                                                color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF1C1C1E).copy(alpha = 0.85f),
                                                fontSize = 11.5.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                    trimmed.isNotBlank() && !trimmed.startsWith("#") && !trimmed.startsWith("---") -> {
                                        Text(
                                            text = trimmed.replace("**", ""),
                                            color = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF1C1C1E).copy(alpha = 0.75f),
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Rich curated categories fallback
                            com.mrtdk.liquid_glass.data.ReleaseNotes.categories.forEachIndexed { idx, cat ->
                                if (idx > 0) Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = Color(0xFFFA243C),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = cat.categoryName,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                cat.items.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 4.dp, bottom = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "•",
                                            color = Color(0xFFFA243C),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 5.dp)
                                        )
                                        Column {
                                            Text(
                                                text = item.title,
                                                color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1C1C1E).copy(alpha = 0.9f),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = item.description,
                                                color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF1C1C1E).copy(alpha = 0.65f),
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Slide down indicator when scroll is available
                    if (scrollState.canScrollForward) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(26.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            if (isSolid) {
                                                if (isDark) Color(0xFF2C2D35) else Color(0xFFF1F2F8)
                                            } else {
                                                Color(0xFF16161A).copy(alpha = 0.9f)
                                            }
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Desliza para ver más",
                                    color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1C1C1E).copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1C1C1E).copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                if (downloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.descargando, (progress * 100).toInt()),
                        color = if (isDark) Color.LightGray else Color.DarkGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFFFA243C),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f))
                
                if (!downloadComplete && !downloading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = stringResource(R.string.cancelar), color = if (isDark) Color.LightGray else Color(0xFF707074), fontSize = 16.sp)
                        }
                        Box(modifier = Modifier.width(0.5.dp).fillMaxHeight().background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onStartDownload() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = stringResource(R.string.actualizar), color = Color(0xFFFA243C), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (downloading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(R.string.descargando_ellipsis), color = Color.Gray, fontSize = 16.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable { onInstallApk() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(R.string.instalar), color = Color(0xFFFA243C), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
}