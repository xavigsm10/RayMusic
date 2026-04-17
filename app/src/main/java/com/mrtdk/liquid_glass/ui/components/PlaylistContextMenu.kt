package com.mrtdk.liquid_glass.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mrtdk.liquid_glass.data.Playlist
import com.mrtdk.liquid_glass.data.LibraryManager
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlaylistContextMenuOverlay(
    playlist: Playlist?,
    onDismiss: () -> Unit
) {
    if (playlist == null) return

    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Dim background
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Peek Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    val coverUrl = playlist.coverUrl ?: (if (playlist.items.isNotEmpty()) playlist.items.first().thumbnail else null)
                    if (coverUrl != null) {
                        AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.fillMaxSize().padding(64.dp))
                    }
                    
                    // Simple gradient and title over it
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.7f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.8f)
                            ))
                    )
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                    ) {
                        Text(playlist.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("My Playlist", color = Color.Gray, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Menu Options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2C2C2E).copy(alpha = 0.95f))
                ) {
                    // Top row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.ArrowDownward, null, tint = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text("Descargar", color = Color.White, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.StarBorder, null, tint = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text("Favorito", color = Color.White, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.IosShare, null, tint = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text("Compartir", color = Color.White, fontSize = 10.sp)
                        }
                    }
                    
                    Divider(color = Color.DarkGray)

                    // Actions
                    MenuRow(Icons.Default.PlayArrow, "Reproducir") { onDismiss() }
                    MenuRow(Icons.Default.Shuffle, "Aleatorio") { onDismiss() }
                    
                    Divider(color = Color.DarkGray)

                    MenuRow(
                        if (playlist.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                        if (playlist.isPinned) "Quitar Fijado Playlist" else "Fijar Playlist"
                    ) {
                        LibraryManager.togglePinPlaylist(playlist.id)
                        onDismiss()
                    }
                    MenuRow(Icons.Default.PlaylistAdd, "Añadir a una Playlist") { onDismiss() }
                    
                    Divider(color = Color.DarkGray)
                    
                    MenuRow(Icons.Default.Edit, "Editar") { onDismiss() }
                    MenuRow(Icons.Default.People, "Gestionar Colaboración") { onDismiss() }
                    MenuRow(Icons.Default.Folder, "Mover a Carpeta") { onDismiss() }
                    MenuRow(Icons.Default.QueueMusic, "Reproducir Siguiente") { onDismiss() }
                }
            }
        }
    }
}

@Composable
fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = Color.White, fontSize = 16.sp)
    }
}
