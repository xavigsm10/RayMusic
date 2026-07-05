package com.mrtdk.liquid_glass.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    fun handleDismiss() {
        if (!downloading) {
            visible = false
            onDismiss()
        }
    }

    BackHandler(enabled = visible) {
        handleDismiss()
    }

    // Full-screen overlay dimming
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val menuWidth = 300.dp

        this@UpdateDialog.GlassBox(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .width(menuWidth)
                .wrapContentHeight(),
            blur = 0.8f,
            scale = 0.02f,
            centerDistortion = 0.1f,
            warpEdges = 0.4f,
            elevation = 4.dp,
            shape = RoundedCornerShape(cornerRadius.dp),
            tint = dominantColor.copy(alpha = 0.25f),
            darkness = 0.2f
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.actualizacion_disponible),
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Versión ${releaseInfo.versionName}",
                    color = Color(0xFFFA243C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Scrollable Changelog Section
                val changelogText = remember(releaseInfo.body) {
                    releaseInfo.body?.takeIf { it.isNotBlank() } ?: "Mejoras de estabilidad y rendimiento."
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Novedades:",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = changelogText,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                if (downloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.descargando, (progress * 100).toInt()),
                        color = Color.LightGray,
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
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                
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
                                .clickable { handleDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = stringResource(R.string.cancelar), color = Color.LightGray, fontSize = 16.sp)
                        }
                        Box(modifier = Modifier.width(0.5.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.1f)))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
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
                            .clickable {
                                apkFile?.let { Updater.installApk(context, it) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(R.string.instalar), color = Color(0xFFFA243C), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}