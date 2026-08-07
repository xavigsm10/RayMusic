package com.mrtdk.liquid_glass.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mrtdk.liquid_glass.listentogether.*
import com.mrtdk.liquid_glass.ui.theme.ThemeManager

private val AmRed = Color(0xFFFF2D55)
private val AmGreen = Color(0xFF34C759)
private val AmOrange = Color(0xFFFF9500)
private val AmBlue = Color(0xFF007AFF)

@Composable private fun surfaceBg(isDark: Boolean) = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)
@Composable private fun cardBg(isDark: Boolean) = if (isDark) Color(0xFF1C1C1E) else Color.White
@Composable private fun labelColor(@Suppress("UNUSED_PARAMETER") isDark: Boolean) = Color(0xFF8E8E93)
@Composable private fun primaryText(isDark: Boolean) = if (isDark) Color.White else Color(0xFF1C1C1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark by ThemeManager.isDarkMode.collectAsState()
    val manager = remember { ListenTogetherManager.getInstance(context) }

    val connectionState by manager.connectionState.collectAsState()
    val roomState by manager.roomState.collectAsState()
    val userId by manager.userId.collectAsState()
    val pendingJoinRequests by manager.pendingJoinRequests.collectAsState()
    val pendingSuggestions by manager.pendingSuggestions.collectAsState()

    var savedUsername by rememberSaveable { mutableStateOf("") }
    var usernameInput by rememberSaveable { mutableStateOf("") }
    var roomCodeInput by rememberSaveable { mutableStateOf("") }
    var isCreatingRoom by rememberSaveable { mutableStateOf(false) }
    var isJoiningRoom by rememberSaveable { mutableStateOf(false) }
    var joinErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUserForMenu by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUsername by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(manager) {
        manager.client.events.collect { event ->
            when (event) {
                is ListenTogetherEvent.JoinRejected -> {
                    joinErrorMessage = when {
                        event.reason.isBlank() -> "Solicitud rechazada por el anfitrion"
                        event.reason.contains("invalid", ignoreCase = true) -> "Codigo de sala invalido"
                        else -> "Solicitud rechazada: ${event.reason}"
                    }
                    isJoiningRoom = false; isCreatingRoom = false
                }
                is ListenTogetherEvent.JoinApproved -> { isJoiningRoom = false; joinErrorMessage = null }
                is ListenTogetherEvent.RoomCreated -> {
                    isCreatingRoom = false
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("Room Code", event.roomCode))
                    Toast.makeText(context, "Sala creada! Codigo: ${event.roomCode}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    val isInRoom = manager.isInRoom
    val isHost = roomState?.hostId == userId

    if (selectedUserForMenu != null && selectedUsername != null) {
        AmUserActionDialog(
            username = selectedUsername ?: "",
            isDark = isDark,
            onKick = {
                selectedUserForMenu?.let { manager.client.kickUser(it, "Removido por el anfitrion") }
                selectedUserForMenu = null; selectedUsername = null
            },
            onTransferOwnership = {
                selectedUserForMenu?.let { manager.client.transferHost(it) }
                selectedUserForMenu = null; selectedUsername = null
            },
            onDismiss = { selectedUserForMenu = null; selectedUsername = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(surfaceBg(isDark)).padding(innerPadding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { AmNavigationHeader(isDark = isDark, onBack = onBack) }
            if (!isInRoom) { item { AmHeroSection(isDark = isDark) } }
            item {
                AmConnectionStatusCard(
                    connectionState = connectionState, isDark = isDark,
                    onConnect = { manager.connect() },
                    onDisconnect = { manager.disconnect() },
                    onReconnect = { manager.connect() }
                )
            }
            if (connectionState == ConnectionState.CONNECTED && !isInRoom) {
                item {
                    Text("La conexion se mantiene activa mientras estes en una sala.", style = MaterialTheme.typography.bodySmall,
                        color = labelColor(isDark), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                }
            }
            if (isInRoom) {
                roomState?.let { room ->
                    item { AmRoomStatusCard(roomCode = room.roomCode, isHost = isHost, context = context, isDark = isDark) }
                    item {
                        AmConnectedUsersSection(users = room.users, isHost = isHost, currentUserId = userId ?: "", isDark = isDark,
                            onUserClick = { id, name -> if (isHost && id != (userId ?: "")) { selectedUserForMenu = id; selectedUsername = name } })
                    }
                    if (isHost && pendingJoinRequests.isNotEmpty()) {
                        item {
                            AmPendingJoinRequestsSection(requests = pendingJoinRequests, isDark = isDark,
                                onApprove = { manager.client.approveJoin(it) },
                                onReject = { manager.client.rejectJoin(it, "Rechazado") })
                        }
                    }
                    if (isHost && pendingSuggestions.isNotEmpty()) {
                        item {
                            AmPendingSuggestionsSection(suggestions = pendingSuggestions, isDark = isDark,
                                onApprove = { id -> pendingSuggestions.firstOrNull { it.suggestionId == id }?.let { manager.approveSuggestion(id, it.trackInfo) } },
                                onReject = { manager.client.rejectSuggestion(it, "Rechazado") })
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = { manager.leaveRoom() }, modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AmRed.copy(alpha = 0.12f))) {
                            Icon(Icons.Default.ExitToApp, null, tint = AmRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salir de la Sala", color = AmRed, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            } else {
                item {
                    AmJoinCreateRoomSection(
                        usernameInput = usernameInput, onUsernameChange = { usernameInput = it },
                        roomCodeInput = roomCodeInput, onRoomCodeChange = { if (it.length <= 8) roomCodeInput = it.uppercase() },
                        savedUsername = savedUsername, isCreatingRoom = isCreatingRoom, isJoiningRoom = isJoiningRoom,
                        joinErrorMessage = joinErrorMessage, isDark = isDark,
                        onCreateRoom = {
                            val name = usernameInput.trim().ifBlank { savedUsername.trim() }
                            if (name.isNotBlank()) {
                                savedUsername = name; isCreatingRoom = true; isJoiningRoom = false; joinErrorMessage = null
                                manager.connect(); manager.createRoom(name)
                            } else Toast.makeText(context, "Ingresa tu nombre de usuario", Toast.LENGTH_SHORT).show()
                        },
                        onJoinRoom = {
                            val name = usernameInput.trim().ifBlank { savedUsername.trim() }
                            if (name.isNotBlank()) {
                                savedUsername = name; isJoiningRoom = true; isCreatingRoom = false; joinErrorMessage = null
                                manager.connect(); manager.joinRoom(roomCodeInput, name)
                            } else Toast.makeText(context, "Ingresa tu nombre de usuario", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AmNavigationHeader(isDark: Boolean, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBackIosNew, "Atras", tint = AmRed, modifier = Modifier.size(20.dp)) }
        Text("Escuchar Juntos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryText(isDark), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AmHeroSection(isDark: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(AmRed.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.People, null, tint = AmRed, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Listen Together", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = primaryText(isDark))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Escucha musica en tiempo real con amigos", fontSize = 14.sp, color = labelColor(isDark), lineHeight = 18.sp)
        }
    }
}

@Composable
private fun AmConnectionStatusCard(connectionState: ConnectionState, isDark: Boolean, onConnect: () -> Unit, onDisconnect: () -> Unit, onReconnect: () -> Unit) {
    val statusColor = when (connectionState) {
        ConnectionState.CONNECTED -> AmGreen
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> AmOrange
        ConnectionState.ERROR -> AmRed
        ConnectionState.DISCONNECTED -> labelColor(isDark)
    }
    val statusText = when (connectionState) {
        ConnectionState.CONNECTED -> "Conectado"
        ConnectionState.CONNECTING -> "Conectando..."
        ConnectionState.RECONNECTING -> "Reconectando..."
        ConnectionState.ERROR -> "Error de conexion"
        ConnectionState.DISCONNECTED -> "Desconectado"
    }
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        shape = RoundedCornerShape(16.dp),
        color = if (connectionState == ConnectionState.DISCONNECTED) cardBg(isDark) else statusColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(statusColor))
                Spacer(modifier = Modifier.width(10.dp))
                Text(statusText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = statusColor, modifier = Modifier.weight(1f))
            }
            if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(8.dp)), color = statusColor, trackColor = statusColor.copy(alpha = 0.2f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
                    Button(onClick = onConnect, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AmRed)) {
                        Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Conectar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                } else {
                    Button(onClick = onDisconnect, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA))) {
                        Text("Desconectar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = primaryText(isDark))
                    }
                    FilledTonalButton(onClick = onReconnect, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(12.dp)) {
                        Text("Reconectar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AmJoinCreateRoomSection(
    usernameInput: String, onUsernameChange: (String) -> Unit,
    roomCodeInput: String, onRoomCodeChange: (String) -> Unit,
    savedUsername: String, isCreatingRoom: Boolean, isJoiningRoom: Boolean,
    joinErrorMessage: String?, isDark: Boolean,
    onCreateRoom: () -> Unit, onJoinRoom: () -> Unit
) {
    val hasUsername = usernameInput.trim().isNotBlank() || savedUsername.isNotBlank()
    val hasRoomCode = roomCodeInput.trim().isNotEmpty()
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = cardBg(isDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("UNIRSE O CREAR SALA", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = AmRed, modifier = Modifier.fillMaxWidth())
            AmTextField(value = usernameInput, onValueChange = onUsernameChange, label = "Nombre de usuario", placeholder = "Ej. Juan", leadingIcon = Icons.Default.Person, isDark = isDark,
                trailingContent = if (usernameInput.isNotBlank()) {{ IconButton(onClick = { onUsernameChange("") }) { Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp), tint = labelColor(isDark)) }}} else null)
            AmTextField(value = roomCodeInput, onValueChange = { if (it.length <= 8) onRoomCodeChange(it.uppercase()) }, label = "Codigo de sala (opcional)", placeholder = "Ej. AB12C345",
                leadingIcon = Icons.Default.Group, isDark = isDark, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                trailingContent = if (roomCodeInput.isNotBlank()) {{ IconButton(onClick = { onRoomCodeChange("") }) { Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp), tint = labelColor(isDark)) }}} else null)
            AnimatedVisibility(visible = isJoiningRoom || isCreatingRoom, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = AmBlue.copy(alpha = 0.10f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(14.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AmBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isCreatingRoom) "Creando sala..." else "Esperando aprobacion...", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AmBlue)
                    }
                }
            }
            AnimatedVisibility(visible = joinErrorMessage != null, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = AmRed.copy(alpha = 0.10f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = AmRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(joinErrorMessage ?: "", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AmRed)
                    }
                }
            }
            AnimatedVisibility(visible = hasUsername && !hasRoomCode) {
                Button(onClick = onCreateRoom, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = hasUsername && !isCreatingRoom && !isJoiningRoom,
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AmRed)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Crear Sala", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            AnimatedVisibility(visible = hasUsername && hasRoomCode) {
                Button(onClick = onJoinRoom, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = hasUsername && hasRoomCode && !isCreatingRoom && !isJoiningRoom,
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AmBlue)) {
                    Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Unirse a la Sala", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun AmRoomStatusCard(roomCode: String, isHost: Boolean, context: Context, isDark: Boolean) {
    val inviteLink = remember(roomCode) { "https://listen-together.app/join?code=$roomCode" }
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = cardBg(isDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CODIGO DE SALA", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = labelColor(isDark))
            Spacer(modifier = Modifier.height(8.dp))
            Text(roomCode, fontSize = 42.sp, fontWeight = FontWeight.Black, color = AmRed, letterSpacing = 6.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(50.dp), color = if (isHost) AmRed.copy(alpha = 0.10f) else AmBlue.copy(alpha = 0.10f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Icon(if (isHost) Icons.Default.WorkspacePremium else Icons.Default.Person, null, tint = if (isHost) AmRed else AmBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isHost) "Eres el Anfitrion" else "Eres Invitado", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isHost) AmRed else AmBlue)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (isHost) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("Link", inviteLink))
                        Toast.makeText(context, "Enlace copiado", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar enlace", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    FilledTonalButton(onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("Room Code", roomCode))
                        Toast.makeText(context, "Codigo copiado: $roomCode", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar codigo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                FilledTonalButton(onClick = {
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("Room Code", roomCode))
                    Toast.makeText(context, "Codigo copiado: $roomCode", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.fillMaxWidth(0.7f).height(44.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copiar codigo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AmConnectedUsersSection(users: List<UserInfo>, isHost: Boolean, currentUserId: String, isDark: Boolean, onUserClick: (String, String) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = cardBg(isDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("USUARIOS CONECTADOS (${users.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = AmRed)
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                users.forEach { user ->
                    AmUserAvatar(user = user, isCurrentUser = user.userId == currentUserId, isClickable = isHost && user.userId != currentUserId, isDark = isDark, onClick = { onUserClick(user.userId, user.username) })
                }
                if (users.isEmpty()) Text("Sin usuarios conectados", fontSize = 14.sp, color = labelColor(isDark))
            }
        }
    }
}

@Composable
private fun AmUserAvatar(user: UserInfo, isCurrentUser: Boolean, isClickable: Boolean, isDark: Boolean, onClick: () -> Unit) {
    val avatarColor = when { user.isHost -> AmRed; isCurrentUser -> AmBlue; else -> if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6) }
    val textColor = if (user.isHost || isCurrentUser) Color.White else primaryText(isDark)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp).let { if (isClickable) it.clickable(onClick = onClick) else it }) {
        Box(contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.size(54.dp), shape = CircleShape, color = avatarColor) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(user.username.take(1).uppercase(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
            if (user.isHost || isCurrentUser) {
                Surface(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).size(20.dp), shape = CircleShape, color = if (user.isHost) AmRed else AmBlue) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(if (user.isHost) Icons.Default.WorkspacePremium else Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(user.username, fontSize = 12.sp, fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium, color = primaryText(isDark), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(when { user.isHost -> "Anfitrion"; isCurrentUser -> "Tu"; else -> "" }, fontSize = 11.sp, color = if (user.isHost) AmRed else if (isCurrentUser) AmBlue else Color.Transparent)
    }
}

@Composable
private fun AmPendingJoinRequestsSection(requests: List<JoinRequestPayload>, isDark: Boolean, onApprove: (String) -> Unit, onReject: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = cardBg(isDark), border = androidx.compose.foundation.BorderStroke(1.dp, AmRed.copy(alpha = 0.35f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationImportant, null, tint = AmRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("SOLICITUDES DE UNION (${requests.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = AmRed)
            }
            Spacer(modifier = Modifier.height(12.dp))
            requests.forEach { req ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = AmRed.copy(alpha = 0.15f)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text(req.username.take(1).uppercase(), fontWeight = FontWeight.Bold, color = AmRed) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(req.username, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = primaryText(isDark), modifier = Modifier.weight(1f))
                    IconButton(onClick = { onApprove(req.userId) }) { Icon(Icons.Default.Check, null, tint = AmRed, modifier = Modifier.size(22.dp)) }
                    IconButton(onClick = { onReject(req.userId) }) { Icon(Icons.Default.Close, null, tint = labelColor(isDark), modifier = Modifier.size(22.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AmPendingSuggestionsSection(suggestions: List<SuggestionReceivedPayload>, isDark: Boolean, onApprove: (String) -> Unit, onReject: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = cardBg(isDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QueueMusic, null, tint = AmRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("SUGERENCIAS DE CANCIONES (${suggestions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = AmRed)
            }
            Spacer(modifier = Modifier.height(12.dp))
            suggestions.forEach { sug ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(AmRed.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, null, tint = AmRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sug.trackInfo.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryText(isDark), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${sug.trackInfo.artist} - ${sug.fromUsername}", fontSize = 12.sp, color = labelColor(isDark), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { onApprove(sug.suggestionId) }) { Icon(Icons.Default.Check, null, tint = AmRed, modifier = Modifier.size(22.dp)) }
                    IconButton(onClick = { onReject(sug.suggestionId) }) { Icon(Icons.Default.Close, null, tint = labelColor(isDark), modifier = Modifier.size(22.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AmUserActionDialog(username: String, isDark: Boolean, onKick: () -> Unit, onTransferOwnership: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(0.88f).padding(16.dp), shape = RoundedCornerShape(24.dp), color = cardBg(isDark)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = AmRed.copy(alpha = 0.12f)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text(username.take(1).uppercase(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AmRed) }
                }
                Text("Gestionar usuario", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryText(isDark))
                Text(username, fontSize = 14.sp, color = labelColor(isDark))
                Spacer(modifier = Modifier.height(4.dp))
                AmDialogOption(Icons.Default.PersonRemove, "Expulsar usuario", "El usuario podra intentar unirse de nuevo", AmRed, AmRed.copy(alpha = 0.08f), isDark, onKick)
                AmDialogOption(Icons.Default.WorkspacePremium, "Transferir anfitrion", "Este usuario sera el nuevo anfitrion", AmBlue, AmBlue.copy(alpha = 0.08f), isDark, onTransferOwnership)
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar", color = labelColor(isDark), fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun AmDialogOption(icon: ImageVector, title: String, subtitle: String, iconColor: Color, bgColor: Color, isDark: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = bgColor) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = iconColor)
                Text(subtitle, fontSize = 12.sp, color = labelColor(isDark), lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun AmTextField(
    value: String, onValueChange: (String) -> Unit, label: String, placeholder: String,
    leadingIcon: ImageVector, isDark: Boolean,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingContent: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = labelColor(isDark)) },
        leadingIcon = { Icon(leadingIcon, null, tint = AmRed, modifier = Modifier.size(20.dp)) },
        trailingIcon = trailingContent, singleLine = true, keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = primaryText(isDark),
            unfocusedTextColor = primaryText(isDark),
            focusedBorderColor = AmRed,
            unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.15f),
            focusedContainerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF9F9F9),
            unfocusedContainerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF9F9F9),
            focusedLabelColor = AmRed, unfocusedLabelColor = labelColor(isDark), cursorColor = AmRed
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
