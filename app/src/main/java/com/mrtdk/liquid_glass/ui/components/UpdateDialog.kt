package com.mrtdk.liquid_glass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.utils.Updater
import java.io.File

@Composable
fun UpdateDialog(
    releaseInfo: Updater.ReleaseInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var downloadComplete by remember { mutableStateOf(false) }
    var apkFile by remember { mutableStateOf<File?>(null) }

    Dialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !downloading, dismissOnClickOutside = !downloading)
    ) {
        Box(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1E1E1E))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.actualizacion_disponible),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.update_dialog_desc, releaseInfo.versionName),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                if (downloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.descargando, (progress * 100).toInt()),
                        color = Color.Gray,
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
                        trackColor = Color(0xFF333333)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                
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
                            Text(text = stringResource(R.string.cancelar), color = Color(0xFFFA243C), fontSize = 17.sp)
                        }
                        Box(modifier = Modifier.width(0.5.dp).fillMaxHeight().background(Color(0xFF333333)))
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
                                            onDismiss()
                                        }
                                    })
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = stringResource(R.string.actualizar), color = Color(0xFFFA243C), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else if (downloading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(R.string.descargando_ellipsis), color = Color.Gray, fontSize = 17.sp)
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
                        Text(text = stringResource(R.string.instalar), color = Color(0xFFFA243C), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}