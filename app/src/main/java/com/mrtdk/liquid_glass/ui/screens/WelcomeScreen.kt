package com.mrtdk.liquid_glass.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.spotify.SpotifySession
import com.mrtdk.liquid_glass.ui.components.SpotifyLoginDialog
import com.mrtdk.liquid_glass.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AppleRed = Color(0xFFFA233B)
private val AppleRedDark = Color(0xFFD61E33)
private val AppleCardBg = Color(0xFF1C1C1E)
private val AppleSurfaceLight = Color(0xFF2C2C2E)

@Composable
fun WelcomeScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    // System Appearance States
    var selectedGlassStyle by remember { mutableStateOf(LibraryManager.getGlassStyle()) }
    var selectedArtworkStyle by remember { mutableStateOf(LibraryManager.getPlayerArtworkStyle()) }
    var selectedBackdropStyle by remember { mutableStateOf(LibraryManager.getFullArtworkBackdropStyle()) }
    var selectedBottomTabsStyle by remember { mutableStateOf(LibraryManager.getBottomTabsStyle()) }

    // Spotify States
    val isSpotifyLoggedIn by SpotifySession.isLoggedIn.collectAsState()
    var showSpotifyLoginDialog by remember { mutableStateOf(false) }
    var isSyncingSpotify by remember { mutableStateOf(false) }

    // Back handling within onboarding
    BackHandler(enabled = currentStep > 0) {
        currentStep--
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Stepper Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 0) {
                    IconButton(
                        onClick = { currentStep-- },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0x33FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Atrás",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }

                // Step progress pills (Apple style)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { index ->
                        val isActive = index == currentStep
                        val isPassed = index < currentStep
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(if (isActive) 26.dp else 10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        isActive -> AppleRed
                                        isPassed -> Color.White.copy(alpha = 0.7f)
                                        else -> Color.White.copy(alpha = 0.2f)
                                    }
                                )
                        )
                    }
                }

                // Step counter or Skip button
                if (currentStep in 1 until totalSteps - 1) {
                    Text(
                        text = "Paso ${currentStep + 1}/$totalSteps",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }

            // Animated Page Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing)) { width -> width / 3 } +
                                    fadeIn(animationSpec = tween(350)))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { width -> -width / 3 } +
                                            fadeOut(animationSpec = tween(300))
                                )
                        } else {
                            (slideInHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing)) { width -> -width / 3 } +
                                    fadeIn(animationSpec = tween(350)))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { width -> width / 3 } +
                                            fadeOut(animationSpec = tween(300))
                                )
                        }
                    },
                    label = "onboarding_step_transition",
                    modifier = Modifier.fillMaxSize()
                ) { step ->
                    when (step) {
                        0 -> WelcomeIntroStep()
                        1 -> InterfaceStyleStep(
                            currentStyle = selectedGlassStyle,
                            onStyleSelected = {
                                selectedGlassStyle = it
                                LibraryManager.saveGlassStyle(it)
                            }
                        )
                        2 -> ArtworkAndBackdropStep(
                            currentArtworkStyle = selectedArtworkStyle,
                            onArtworkStyleSelected = {
                                selectedArtworkStyle = it
                                LibraryManager.savePlayerArtworkStyle(it)
                            },
                            currentBackdropStyle = selectedBackdropStyle,
                            onBackdropStyleSelected = {
                                selectedBackdropStyle = it
                                LibraryManager.saveFullArtworkBackdropStyle(it)
                            },
                            currentBottomTabsStyle = selectedBottomTabsStyle,
                            onBottomTabsStyleSelected = {
                                selectedBottomTabsStyle = it
                                LibraryManager.saveBottomTabsStyle(it)
                            }
                        )
                        3 -> SpotifyConnectStep(
                            isLoggedIn = isSpotifyLoggedIn,
                            userName = SpotifySession.userName,
                            isSyncing = isSyncingSpotify,
                            onConnectClick = { showSpotifyLoginDialog = true },
                            onSyncClick = {
                                isSyncingSpotify = true
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    try {
                                        LibraryManager.syncSpotifyPlaylists()
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isSyncingSpotify = false
                                            Toast.makeText(context, "Playlists sincronizadas exitosamente", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Exception) {
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isSyncingSpotify = false
                                            Toast.makeText(context, "Error al sincronizar playlists", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                        4 -> WelcomeReadyStep(
                            glassStyle = selectedGlassStyle,
                            artworkStyle = selectedArtworkStyle,
                            isSpotifyConnected = isSpotifyLoggedIn
                        )
                    }
                }
            }

            // Bottom Action Pill Button (Apple Music style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            LibraryManager.setCompletedOnboarding(true)
                            onFinish()
                        }
                    },
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(30.dp), spotColor = AppleRed.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when (currentStep) {
                                0 -> "Comenzar personalización"
                                totalSteps - 1 -> "Empezar a escuchar"
                                else -> "Continuar"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (currentStep in 1 until totalSteps - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Omitir paso",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                currentStep++
                            }
                            .padding(vertical = 4.dp, horizontal = 12.dp)
                    )
                }
            }
        }
    }

    if (showSpotifyLoginDialog) {
        SpotifyLoginDialog(
            onDismiss = { showSpotifyLoginDialog = false },
            onSuccess = {
                showSpotifyLoginDialog = false
                isSyncingSpotify = true
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        LibraryManager.syncSpotifyPlaylists()
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isSyncingSpotify = false
                            Toast.makeText(context, "Conectado a Spotify exitosamente", Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isSyncingSpotify = false
                        }
                    }
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 0: INTRODUCCIÓN Y NOVEDADES (APPLE MUSIC "WHAT'S NEW" STYLE)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WelcomeIntroStep() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // RayMusic Red Icon with Rounded Borders
        Box(
            modifier = Modifier
                .size(92.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = AppleRed.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(24.dp))
                .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "RayMusic Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Version Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AppleRed.copy(alpha = 0.15f))
                .border(1.dp, AppleRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "VERSIÓN 0.6.5",
                color = AppleRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Te damos la bienvenida a",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Text(
            text = "RayMusic",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Apple Music Feature Row Items
        FeatureItem(
            icon = Icons.Default.Palette,
            iconTint = Color(0xFFFF5252),
            title = "Vidrio Líquido & Material 3",
            description = "Elige entre un diseño translúcido con desenfoque de cristal o una cápsula sólida de alto contraste."
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeatureItem(
            icon = Icons.Default.Tune,
            iconTint = Color(0xFFFF4081),
            title = "Portadas Animadas en Fullartwork",
            description = "Disfruta de portadas en movimiento y videos oficiales que transforman por completo el reproductor."
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeatureItem(
            icon = Icons.Default.QueueMusic,
            iconTint = Color(0xFF1ED760),
            title = "Tus Playlists de Spotify",
            description = "Importa y sincroniza fácilmente tus listas de reproducción personales con un solo toque."
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeatureItem(
            icon = Icons.Default.GraphicEq,
            iconTint = Color(0xFF448AFF),
            title = "Audio Hi-Res Lossless",
            description = "Experiencia de sonido puro sin pérdida, ecualización dinámica y letras sincronizadas en tiempo real."
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.5.sp,
                lineHeight = 18.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 1: ESTILO DE INTERFAZ (LIQUID GLASS VS MATERIAL 3)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InterfaceStyleStep(
    currentStyle: String,
    onStyleSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Estilo de Interfaz",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Personaliza el aspecto de la barra de navegación y el minireproductor a tu preferencia.",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 14.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Option 1: Vidrio Líquido (Liquid Glass)
        InterfacePreviewCard(
            title = "Vidrio Líquido (Liquid Glass)",
            description = "Efecto de cristal translúcido con desenfoque de fondo en tiempo real y reflejos dinámicos.",
            imageRes = R.drawable.preview_liquid_glass,
            isSelected = currentStyle == "transparent",
            onClick = { onStyleSelected("transparent") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Option 2: Material 3 Expressive (Sólido)
        InterfacePreviewCard(
            title = "Material 3 Expressive (Sólido)",
            description = "Cápsula oscura sólida y elegante con alto contraste y enfoque limpio en los controles.",
            imageRes = R.drawable.preview_material3,
            isSelected = currentStyle == "solid",
            onClick = { onStyleSelected("solid") }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun InterfacePreviewCard(
    title: String,
    description: String,
    imageRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AppleRed else Color.White.copy(alpha = 0.15f)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) AppleCardBg else Color(0xFF141416)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Preview Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Apple Style Radio / Check Circle
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AppleRed else Color.White.copy(alpha = 0.1f))
                        .border(
                            1.5.dp,
                            if (isSelected) AppleRed else Color.White.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 2: REPRODUCTOR, FULLARTWORK Y FONDOS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ArtworkAndBackdropStep(
    currentArtworkStyle: String,
    onArtworkStyleSelected: (String) -> Unit,
    currentBackdropStyle: String,
    onBackdropStyleSelected: (String) -> Unit,
    currentBottomTabsStyle: String,
    onBottomTabsStyleSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Portadas y Reproductor",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Configura cómo deseas disfrutar las portadas y el fondo inmersivo al expandir una canción.",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 14.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Option 1: Portadas animadas solamente con fullartwork (Recomendado)
        ArtworkOptionCard(
            title = "Portadas animadas solamente con fullartwork",
            description = "Las canciones con video se expanden a pantalla completa inmersiva; las portadas estáticas originales se muestran en modo normal con tarjeta.",
            badge = "RECOMENDADO",
            isSelected = currentArtworkStyle == "animated_fullartwork",
            onClick = { onArtworkStyleSelected("animated_fullartwork") }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Option 2: Fullartwork siempre
        ArtworkOptionCard(
            title = "Fullartwork completo",
            description = "Todas las portadas (tanto estáticas como animadas) se expanden ocupando toda la pantalla.",
            badge = null,
            isSelected = currentArtworkStyle == "fullartwork",
            onClick = { onArtworkStyleSelected("fullartwork") }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Option 3: Normal
        ArtworkOptionCard(
            title = "Normal (Tarjeta estándar)",
            description = "Tarjeta cuadrada centrada clásica con bordes redondeados y sombra elegante estilo Apple Music.",
            badge = null,
            isSelected = currentArtworkStyle == "normal",
            onClick = { onArtworkStyleSelected("normal") }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Section: Estilo de Fondo de Fullartwork
        Text(
            text = "Efecto de fondo en Fullartwork",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SubSelectorPill(
                title = "Reflejo Apple Music",
                subtitle = "Gradiente dinámico",
                isSelected = currentBackdropStyle == "apple_music",
                modifier = Modifier.weight(1f),
                onClick = { onBackdropStyleSelected("apple_music") }
            )
            SubSelectorPill(
                title = "Fondo completo",
                subtitle = "Fluido animado",
                isSelected = currentBackdropStyle == "accord",
                modifier = Modifier.weight(1f),
                onClick = { onBackdropStyleSelected("accord") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Estilo de Pestañas
        Text(
            text = "Estilo de pestañas inferiores",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SubSelectorPill(
                title = "Estilo iOS 26",
                subtitle = "Barra estándar",
                isSelected = currentBottomTabsStyle == "ios26",
                modifier = Modifier.weight(1f),
                onClick = { onBottomTabsStyleSelected("ios26") }
            )
            SubSelectorPill(
                title = "Estilo iOS 27",
                subtitle = "Cápsula flotante",
                isSelected = currentBottomTabsStyle == "ios27",
                modifier = Modifier.weight(1f),
                onClick = { onBottomTabsStyleSelected("ios27") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ArtworkOptionCard(
    title: String,
    description: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AppleRed else Color.White.copy(alpha = 0.15f)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) AppleCardBg else Color(0xFF141416)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppleRed.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = AppleRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.5.sp,
                    lineHeight = 16.5.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Checkmark Circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) AppleRed else Color.White.copy(alpha = 0.1f))
                    .border(
                        1.5.dp,
                        if (isSelected) AppleRed else Color.White.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubSelectorPill(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AppleRed else Color.White.copy(alpha = 0.15f)
    val bgColor = if (isSelected) AppleRed.copy(alpha = 0.15f) else Color(0xFF141416)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                fontSize = 13.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = if (isSelected) AppleRed else Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 3: PLAYLISTS DE SPOTIFY (CARGA TUS PLAYLISTS EN RAYMUSIC)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SpotifyConnectStep(
    isLoggedIn: Boolean,
    userName: String,
    isSyncing: Boolean,
    onConnectClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Spotify Icon Emblem
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFF1ED760).copy(alpha = 0.15f))
                .border(1.5.dp, Color(0xFF1ED760).copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_spotify),
                contentDescription = "Spotify",
                tint = Color(0xFF1ED760),
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Tus Playlists de Spotify",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Conecta tu cuenta para importar todas tus playlists personales y escucharlas sin anuncios en RayMusic.",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 14.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoggedIn) {
            // Already Connected Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141416))
                    .border(1.dp, Color(0xFF1ED760).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1ED760))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cuenta Conectada",
                            color = Color(0xFF1ED760),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = userName.ifBlank { "Usuario de Spotify" },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onSyncClick,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1ED760),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sincronizando...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sincronizar playlists ahora", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Connect Button Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141416))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Importación Instantánea",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "RayMusic leerá tus listas de Spotify y buscará las canciones con la más alta fidelidad de audio automáticamente.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onConnectClick,
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1ED760),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_spotify),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Cargar mis playlists de Spotify",
                            color = Color.Black,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 4: CONFIRMACIÓN Y RESUMEN FINAL
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WelcomeReadyStep(
    glassStyle: String,
    artworkStyle: String,
    isSpotifyConnected: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Checkmark badge in Apple Red
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AppleRed.copy(alpha = 0.15f))
                .border(2.dp, AppleRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AppleRed,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "¡Todo Listo!",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tu experiencia en RayMusic v0.6.5 ha sido configurada a tu medida.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AppleCardBg)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Resumen de configuración",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                SummaryRow(
                    label = "Estilo de interfaz",
                    value = if (glassStyle == "transparent") "Vidrio Líquido (Translúcido)" else "Material 3 Expressive (Sólido)",
                    icon = Icons.Default.Palette
                )

                Spacer(modifier = Modifier.height(14.dp))

                SummaryRow(
                    label = "Reproductor y portadas",
                    value = when (artworkStyle) {
                        "normal" -> "Normal (Tarjeta)"
                        "animated_fullartwork" -> "Animadas con Fullartwork"
                        else -> "Fullartwork Completo"
                    },
                    icon = Icons.Default.Tune
                )

                Spacer(modifier = Modifier.height(14.dp))

                SummaryRow(
                    label = "Spotify Playlists",
                    value = if (isSpotifyConnected) "Conectado" else "No conectado",
                    icon = Icons.Default.QueueMusic
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Puedes modificar estas opciones en cualquier momento desde Ajustes > Apariencia.",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppleRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        Text(
            text = value,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
    }
}
