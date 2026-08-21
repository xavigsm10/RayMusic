package com.mrtdk.liquid_glass.ui.screens



import android.net.Uri

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.slideInVertically

import androidx.compose.animation.slideOutVertically

import androidx.compose.animation.fadeIn

import androidx.compose.animation.fadeOut

import androidx.compose.animation.AnimatedContent

import androidx.compose.animation.scaleIn

import androidx.compose.animation.scaleOut

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.Image

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.gestures.detectVerticalDragGestures

import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.animation.core.tween

import androidx.compose.animation.core.Animatable

import androidx.compose.animation.core.spring

import androidx.compose.animation.core.Spring

import androidx.compose.animation.togetherWith

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.ui.input.nestedscroll.NestedScrollConnection

import androidx.compose.ui.input.nestedscroll.NestedScrollSource

import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.ui.unit.Velocity

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.Menu

import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.res.stringResource

import com.mrtdk.liquid_glass.R

import androidx.compose.material.icons.filled.Star

import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.border

import androidx.compose.ui.platform.LocalConfiguration

import android.content.res.Configuration

import androidx.compose.foundation.layout.ExperimentalLayoutApi

import androidx.compose.foundation.layout.FlowRow

import androidx.compose.foundation.lazy.rememberLazyListState
import kotlin.math.roundToInt

import androidx.compose.animation.animateColorAsState

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.core.animateDpAsState

import androidx.compose.foundation.interaction.collectIsPressedAsState

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.blur

import androidx.compose.ui.draw.BlurredEdgeTreatment

import androidx.compose.ui.draw.clipToBounds

import com.skydoves.cloudy.cloudy

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Size

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.Shadow

import androidx.compose.ui.graphics.luminance

import androidx.compose.ui.graphics.Path

import androidx.compose.ui.graphics.Paint

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.FilterQuality

import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.unit.IntSize

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.graphics.BlendMode

import androidx.compose.ui.graphics.CompositingStrategy

import androidx.compose.ui.draw.drawWithContent

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.foundation.gestures.awaitFirstDown

import androidx.compose.foundation.gestures.waitForUpOrCancellation

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.ui.layout.LayoutCoordinates

import com.kyant.backdrop.backdrops.layerBackdrop

import com.kyant.backdrop.backdrops.rememberLayerBackdrop

import com.kyant.backdrop.drawBackdrop

import com.kyant.backdrop.effects.blur

import com.kyant.backdrop.effects.lens

import com.kyant.backdrop.effects.vibrancy

import com.mrtdk.glass.GlassContainer

import com.mrtdk.glass.GlassBox

import com.mrtdk.glass.GlassBoxScope

import androidx.activity.compose.BackHandler

import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.filled.Smartphone

import androidx.compose.material.icons.filled.Speaker

import androidx.compose.material.icons.filled.Laptop

import androidx.compose.material.icons.filled.Wifi

import com.mrtdk.liquid_glass.ui.components.PlayerOptionsMenu
import com.mrtdk.liquid_glass.ui.components.GraduatedBlurArtwork
import com.mrtdk.liquid_glass.ui.components.LyricsOptionsMenu
import com.mrtdk.liquid_glass.ui.components.ArtistOptionsMenu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.animation.core.animateFloat

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import coil.compose.AsyncImage

import coil.request.ImageRequest

import android.app.DownloadManager

import android.os.Environment

import android.widget.Toast

import kotlinx.coroutines.CoroutineScope

import androidx.compose.material.icons.filled.ArrowDownward

import kotlinx.coroutines.launch

import com.mrtdk.liquid_glass.data.ItemType

import com.mrtdk.liquid_glass.data.LibraryItem

import com.mrtdk.liquid_glass.data.LibraryManager

import com.echo.innertube.YouTube

import com.echo.innertube.models.SongItem

import com.echo.innertube.models.WatchEndpoint

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics

val ISyncedLine.timeMs: Long
    get() = this.start.toLong()

val ISyncedLine.text: String
    get() = when (this) {
        is com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine -> this.content
        is com.mocharealm.accompanist.lyrics.core.model.synced.UncheckedSyncedLine -> this.content
        is com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine -> this.syllables.joinToString("") { it.content }
        else -> ""
    }

val ISyncedLine.translationText: String?
    get() = when (this) {
        is com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine -> this.translation
        is com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine.MainKaraokeLine -> this.translation
        is com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine.AccompanimentKaraokeLine -> this.translation
        else -> null
    }

data class PlayerState(

    val title: String,

    val artist: String,

    val artUrl: Any?,

    val videoId: String? = null,

    val contentUri: Uri? = null,

    val duration: Long = 0L,

    val queue: List<QueueItem> = emptyList(),

    val isExclusiveQueue: Boolean = false,

    val album: String? = null,

    val albumId: String? = null,

    val playlistId: String? = null,

    val playlistName: String? = null

)

@androidx.compose.runtime.Immutable
data class QueueItemRowData(
    val title: String,
    val artist: String,
    val artUrl: Any?
)

@androidx.compose.runtime.Immutable
data class UpNextSongRowData(
    val title: String,
    val artist: String,
    val thumbnail: String?
)



data class QueueItem(

    val title: String,

    val artist: String,

    val artUrl: Any?,

    val videoId: String? = null,

    val album: String? = null,

    val albumId: String? = null,

    val playlistId: String? = null,

    val playlistName: String? = null

)



object AudioRoutingState {

    var connectedDeviceName: String? by mutableStateOf(null)

    var showAudioRoutingMenu by mutableStateOf(false)

}



object ArtistSelectionState {

    var artistsToShowDialog: List<String>? by mutableStateOf(null)

}



@Composable

fun BluetoothIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {

    Canvas(modifier = modifier.size(24.dp)) {

        val w = size.width

        val h = size.height

        val path = Path().apply {

            moveTo(w * 0.25f, h * 0.75f)

            lineTo(w * 0.75f, h * 0.25f)

            lineTo(w * 0.5f, h * 0.05f)

            lineTo(w * 0.5f, h * 0.95f)

            lineTo(w * 0.75f, h * 0.75f)

            lineTo(w * 0.25f, h * 0.25f)

        }

        drawPath(

            path = path,

            color = tint,

            style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)

        )

    }

}



@Composable

fun RoutingDeviceRow(

    icon: @Composable (tint: Color) -> Unit,

    text: String,

    isActive: Boolean,

    volumePosition: Float,

    onVolumeChange: (Float) -> Unit,

    onClick: () -> Unit

) {

    val currentVolumePosition by rememberUpdatedState(volumePosition)

    val currentOnVolumeChange by rememberUpdatedState(onVolumeChange)

    

    if (isActive) {

        BoxWithConstraints(

            modifier = Modifier

                .fillMaxWidth()

                .height(48.dp)

                .clip(RoundedCornerShape(24.dp))

                .background(Color.White.copy(alpha = 0.08f))

                .pointerInput(Unit) {

                    detectTapGestures { offset ->

                        val progress = (offset.x / size.width).coerceIn(0f, 1f)

                        currentOnVolumeChange(progress)

                    }

                }

                .pointerInput(Unit) {

                    detectHorizontalDragGestures { change, dragAmount ->

                        val newVolume = (currentVolumePosition + (dragAmount / size.width)).coerceIn(0f, 1f)

                        currentOnVolumeChange(newVolume)

                    }

                }

        ) {

            val widthPx = maxWidth

            

            // 1. Unfilled layer (Dark background, white text)

            Row(

                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),

                verticalAlignment = Alignment.CenterVertically

            ) {

                icon(Color.White)

                Spacer(modifier = Modifier.width(12.dp))

                Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))

            }



            // 2. Filled layer (White background, dark text), clipped to volumePosition

            Box(

                modifier = Modifier

                    .fillMaxHeight()

                    .fillMaxWidth(volumePosition)

                    .clipToBounds()

                    .background(Color.White)

            ) {

                Row(

                    modifier = Modifier

                        .width(widthPx)

                        .fillMaxHeight()

                        .padding(horizontal = 16.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    icon(Color.Black)

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(text, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

                    Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))

                }

            }

        }

    } else {

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .height(48.dp)

                .clip(RoundedCornerShape(24.dp))

                .background(Color.White.copy(alpha = 0.04f))

                .clickable { onClick() }

                .padding(horizontal = 16.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            icon(Color.White.copy(alpha = 0.7f))

            Spacer(modifier = Modifier.width(12.dp))

            Text(text, color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))

        }

    }

}



@Composable

fun GlassBoxScope.AudioRoutingMenu(

    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,

    onDismiss: () -> Unit,

    playerState: PlayerState?,

    volumePosition: Float,

    onVolumeChange: (Float) -> Unit

) {

    var visible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()



    LaunchedEffect(Unit) {

        visible = true

    }



    val scale by animateFloatAsState(

        targetValue = if (visible) 1f else 0.4f,

        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),

        label = "routingScale"

    )

    val alpha by animateFloatAsState(

        targetValue = if (visible) 1f else 0f,

        animationSpec = tween(durationMillis = 200),

        label = "routingAlpha"

    )

    val cornerRadius by animateFloatAsState(

        targetValue = if (visible) 28f else 80f,

        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),

        label = "routingCornerRadius"

    )

    val blurPx by animateFloatAsState(

        targetValue = if (visible) 0f else 15f,

        animationSpec = tween(durationMillis = 180),

        label = "routingContentBlur"

    )



    val context = LocalContext.current

    var connectedBluetoothDeviceName by remember { mutableStateOf<String?>(null) }



    // Query bonded/connected Bluetooth devices

    fun checkConnectedA2dp() {

        try {

            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled &&

                (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED ||

                 android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S)) {

                

                bluetoothAdapter.getProfileProxy(context, object : android.bluetooth.BluetoothProfile.ServiceListener {

                    override fun onServiceConnected(profile: Int, proxy: android.bluetooth.BluetoothProfile) {

                        if (profile == android.bluetooth.BluetoothProfile.A2DP) {

                            val a2dp = proxy as android.bluetooth.BluetoothA2dp

                            val connectedDevices = a2dp.connectedDevices

                            if (connectedDevices.isNotEmpty()) {

                                val devName = connectedDevices.firstOrNull()?.name

                                if (devName != null) {

                                    connectedBluetoothDeviceName = devName

                                    if (AudioRoutingState.connectedDeviceName == null) {

                                        AudioRoutingState.connectedDeviceName = devName

                                    }

                                }

                            } else {

                                connectedBluetoothDeviceName = null

                            }

                        }

                        bluetoothAdapter.closeProfileProxy(profile, proxy)

                    }

                    override fun onServiceDisconnected(profile: Int) {

                        if (profile == android.bluetooth.BluetoothProfile.A2DP) {

                            connectedBluetoothDeviceName = null

                        }

                    }

                }, android.bluetooth.BluetoothProfile.A2DP)

            }

        } catch (e: Exception) {

            // ignore

        }

    }



    DisposableEffect(context) {

        val filter = android.content.IntentFilter().apply {

            addAction(android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)

            addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)

        }

        val receiver = object : android.content.BroadcastReceiver() {

            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {

                checkConnectedA2dp()

            }

        }

        context.registerReceiver(receiver, filter)

        checkConnectedA2dp()

        onDispose {

            context.unregisterReceiver(receiver)

        }

    }



    fun handleDismiss() {

        visible = false

        onDismiss()

    }



    BackHandler(enabled = visible) {

        handleDismiss()

    }



    val dominantColor by LibraryManager.currentDominantColor.collectAsState()

    val tintColor = remember(dominantColor) { dominantColor.copy(alpha = 0.35f) }



    Box(

        modifier = Modifier

            .fillMaxSize()

            .background(Color.Black.copy(alpha = 0.4f))

            .clickable(

                interactionSource = remember { MutableInteractionSource() },

                indication = null

            ) { handleDismiss() }

    )



    Box(

        modifier = Modifier.fillMaxSize(),

        contentAlignment = Alignment.BottomCenter

    ) {

        Box(

            modifier = Modifier

                .padding(bottom = 120.dp)

                .graphicsLayer {

                    scaleX = scale

                    scaleY = scale

                    this.alpha = alpha

                }

                .width(320.dp)

                .wrapContentHeight()

                .drawBackdrop(

                    backdrop = backdrop,

                    shape = { RoundedCornerShape(cornerRadius.dp) },

                    effects = {

                        vibrancy()

                        blur(8f.dp.toPx())

                        lens(24f.dp.toPx(), 24f.dp.toPx())

                    },

                    onDrawSurface = {

                        drawRect(tintColor)

                    }

                )

                .clip(RoundedCornerShape(cornerRadius.dp))

        ) {

            Column(

                modifier = Modifier

                    .fillMaxWidth()

                    .let { if (blurPx > 0.1f) it.blur(blurPx.dp) else it }

                    .padding(16.dp),

                verticalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                // 1. Cellular Speaker row

                RoutingDeviceRow(

                    icon = { tint -> Icon(Icons.Default.Smartphone, null, tint = tint, modifier = Modifier.size(20.dp)) },

                    text = stringResource(R.string.celular_speaker),

                    isActive = AudioRoutingState.connectedDeviceName == null,

                    volumePosition = volumePosition,

                    onVolumeChange = onVolumeChange,

                    onClick = { AudioRoutingState.connectedDeviceName = null }

                )



                // 2. Real connected Bluetooth device row (if active/connected)

                val isBtActive = connectedBluetoothDeviceName != null

                if (isBtActive && connectedBluetoothDeviceName != null) {

                    val devName = connectedBluetoothDeviceName!!

                    RoutingDeviceRow(

                        icon = { tint -> BluetoothIcon(modifier = Modifier.size(20.dp), tint = tint) },

                        text = devName,

                        isActive = AudioRoutingState.connectedDeviceName == devName,

                        volumePosition = volumePosition,

                        onVolumeChange = onVolumeChange,

                        onClick = { AudioRoutingState.connectedDeviceName = devName }

                    )

                }



                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))



                // 3. Search WiFi devices action

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .height(48.dp)

                        .clip(RoundedCornerShape(24.dp))

                        .background(Color.White.copy(alpha = 0.08f))

                        .clickable {

                            try {

                                val intent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)

                                context.startActivity(intent)

                            } catch (e: Exception) {

                                Toast.makeText(context, "No se pudieron abrir los ajustes de WiFi", Toast.LENGTH_SHORT).show()

                            }

                        }

                        .padding(horizontal = 16.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Icon(Icons.Default.Wifi, null, tint = Color.White, modifier = Modifier.size(20.dp))

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(

                        text = stringResource(R.string.wifi_settings_action),

                        color = Color.White.copy(alpha = 0.8f),

                        fontSize = 14.sp,

                        fontWeight = FontWeight.Medium,

                        modifier = Modifier.weight(1f)

                    )

                }



                // 4. Search Bluetooth devices action

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .height(48.dp)

                        .clip(RoundedCornerShape(24.dp))

                        .background(Color.White.copy(alpha = 0.08f))

                        .clickable {

                            try {

                                val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)

                                context.startActivity(intent)

                            } catch (e: Exception) {

                                Toast.makeText(context, "No se pudieron abrir los ajustes de Bluetooth", Toast.LENGTH_SHORT).show()

                            }

                        }

                        .padding(horizontal = 16.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    BluetoothIcon(modifier = Modifier.size(20.dp), tint = Color.White)

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(

                        text = stringResource(R.string.bluetooth_settings_action),

                        color = Color.White.copy(alpha = 0.8f),

                        fontSize = 14.sp,

                        fontWeight = FontWeight.Medium,

                        modifier = Modifier.weight(1f)

                    )

                }

            }

        }

    }

}



private fun extractDominantColor(bitmap: android.graphics.Bitmap, isBillieJean: Boolean): Color {

    val palette = androidx.palette.graphics.Palette.from(bitmap).maximumColorCount(8).generate()

    val domRgb = palette.getDominantColor(android.graphics.Color.DKGRAY)

    val vibRgb = palette.getVibrantColor(domRgb)

    val mutedRgb = palette.getMutedColor(domRgb)

    

    val chosenRgb = if (vibRgb != domRgb && Color(vibRgb).luminance() > 0.1f) {

        vibRgb

    } else if (mutedRgb != domRgb && Color(mutedRgb).luminance() > 0.1f) {

        mutedRgb

    } else {

        domRgb

    }

    

    val extractedColor = Color(chosenRgb)

    

    return if (isBillieJean) {

        val skinTone = Color(0xFF6E472A) 

        Color(

            red = (extractedColor.red * 0.5f + skinTone.red * 0.5f),

            green = (extractedColor.green * 0.5f + skinTone.green * 0.5f),

            blue = (extractedColor.blue * 0.5f + skinTone.blue * 0.5f),

            alpha = 1f

        )

    } else {

        extractedColor

    }

}



@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

@Composable

fun PlayerScreen(

    playerState: PlayerState?,

    isVisible: Boolean,

    isPlaying: Boolean,

    currentPosition: Long,

    duration: Long,

    isBottomBarCollapsed: Boolean = false,

    upNextSongs: List<SongItem> = emptyList(),

    onUpNextSongsChange: (List<SongItem>) -> Unit = {},

    songHistory: List<PlayerState> = emptyList(),

    onSkipNext: () -> Unit = {},

    onSkipPrevious: () -> Unit = {},

    onClose: () -> Unit,

    onTogglePlayPause: () -> Unit,

    onSeek: (Long) -> Unit,

    onVolumeChange: (Float) -> Unit,

    onArtistSelected: (com.mrtdk.liquid_glass.ui.screens.ArtistState) -> Unit = {},

    onAlbumSelected: (com.mrtdk.liquid_glass.ui.screens.AlbumState) -> Unit = {},

    onSongSelected: (PlayerState) -> Unit = {},

    onSongSelectedFromQueue: (PlayerState) -> Unit = {},

    shuffleModeEnabled: Boolean = false,

    repeatMode: Int = androidx.media3.common.Player.REPEAT_MODE_OFF,

    onToggleShuffle: () -> Unit = {},

    onToggleRepeat: () -> Unit = {},

    onDominantColorChanged: (Color) -> Unit = {},

    playbackError: String? = null,

    onClearPlaybackError: () -> Unit = {}

) {

    AnimatedVisibility(

        visible = isVisible,

        enter = androidx.compose.animation.slideInVertically(

               initialOffsetY = { it },

               animationSpec = androidx.compose.animation.core.tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing)

           ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),

        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(100))

    ) {

        if (playerState == null) return@AnimatedVisibility



        val context = LocalContext.current

        val localBackdrop = rememberLayerBackdrop()



        if (playbackError != null) {

            androidx.compose.material3.AlertDialog(

                onDismissRequest = onClearPlaybackError,

                title = {

                    Text(

                        text = "Error de reproducción",

                        color = Color.White,

                        fontWeight = FontWeight.Bold

                    )

                },

                text = {

                    Column {

                        Text(
                            text = "No se pudo reproducir la canción. Por favor, toma una captura de pantalla de este error para enviársela al desarrollador:",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {

                            Text(

                                text = playbackError ?: "",

                                color = Color.Red,

                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,

                                fontSize = 12.sp

                            )

                        }

                    }

                },

                confirmButton = {

                    androidx.compose.material3.TextButton(

                        onClick = {

                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

                            val clip = android.content.ClipData.newPlainText("Error RayMusic", playbackError)

                            clipboard.setPrimaryClip(clip)

                            android.widget.Toast.makeText(context, "Copiado al portapapeles", android.widget.Toast.LENGTH_SHORT).show()

                        }

                    ) {

                        Text("Copiar", color = Color(0xFFE91E63))

                    }

                },

                dismissButton = {

                    androidx.compose.material3.TextButton(onClick = onClearPlaybackError) {

                        Text("Cerrar", color = Color.White)

                    }

                },

                containerColor = Color(0xFF1E1E1E),

                textContentColor = Color.White

            )

        }



        val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }

        val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() }

        var volumePosition by remember { 

            mutableFloatStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) / maxVolume)

        }

        val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

        val scope = rememberCoroutineScope()

        val dragOffsetY = remember { Animatable(0f) }

        LaunchedEffect(isVisible) {
            if (isVisible) {
                dragOffsetY.snapTo(0f)
            }
        }

        val offsetY = dragOffsetY.value

        

        val queueListState = androidx.compose.foundation.lazy.rememberLazyListState()

        val lyricsListState = androidx.compose.foundation.lazy.rememberLazyListState()

        var showQueue by remember { mutableStateOf(false) }

        var showLyrics by remember { mutableStateOf(false) }

        var showLyricsControls by remember { mutableStateOf(true) }

        var lyricsControlsHideTrigger by remember { mutableStateOf(0) }



        LaunchedEffect(showLyrics) {

            if (showLyrics) {

                showLyricsControls = true

                lyricsControlsHideTrigger++

            }

        }

        val hideStatusBarOnFullscreen = remember { com.mrtdk.liquid_glass.data.LibraryManager.getString("hide_status_bar_on_fullscreen", "false") == "true" }
        val windowContext = androidx.compose.ui.platform.LocalContext.current
        LaunchedEffect(isVisible, hideStatusBarOnFullscreen) {
            val activity = windowContext as? android.app.Activity ?: return@LaunchedEffect
            val window = activity.window
            if (hideStatusBarOnFullscreen && isVisible) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    window.insetsController?.show(android.view.WindowInsets.Type.statusBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
            }
        }



        LaunchedEffect(showLyrics, showLyricsControls, lyricsControlsHideTrigger) {

            if (showLyrics && showLyricsControls) {

                kotlinx.coroutines.delay(2000L)

                showLyricsControls = false

            }

        }

        

                var showOptionsMenu by remember { mutableStateOf(false) }
        var showLyricsMenu by remember { mutableStateOf(false) }
        var showLyricsOptionsMenu by remember { mutableStateOf(false) }
        var lyricsOffset by remember { mutableStateOf(0) }
        var lyricsReloadTrigger by remember { mutableStateOf(0) }
        var showLyricsOffsetDialog by remember { mutableStateOf(false) }
        var showLyricsEditDialog by remember { mutableStateOf(false) }
        var isAutoScrollEnabled by remember { mutableStateOf(true) }
        var scrollToCurrentTrigger by remember { mutableStateOf(0) }
        var menuPivotBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
        var showPlaylistMenu by remember { mutableStateOf(false) }
        var showNewPlaylistDialog by remember { mutableStateOf(false) }
        var showArtistOptionsMenu by remember { mutableStateOf(false) }
        var artistMenuOptions by remember { mutableStateOf<List<String>>(emptyList()) }
        var artistPivotBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

        

        var swipeDirection by remember { mutableIntStateOf(1) }

        val isBadSong = remember(playerState) {

            val title = playerState?.title ?: ""

            val artist = playerState?.artist ?: ""

            val album = playerState?.album ?: ""

            (album.contains("Bad", ignoreCase = true) || title.contains("Bad", ignoreCase = true)) &&

            artist.contains("Michael Jackson", ignoreCase = true)

        }

        val hdArtUrl = remember(playerState?.artUrl, playerState?.title, playerState?.artist) {

            val url = playerState?.artUrl ?: return@remember null

            val urlString = url.toString()

            if (urlString.startsWith("file:///android_asset/")) {

                url

            } else {

                val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(urlString) ?: urlString

                if (url is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded

            }

        }



        var dominantColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }

        var bottomAverageColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }

        var rightSideAverageColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }

        var parentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

        var sliderCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }



        var lyricsLines by remember { mutableStateOf<List<com.mocharealm.accompanist.lyrics.core.model.ISyncedLine>?>(null) }

        var showManualLyricsSearch by remember { mutableStateOf(false) }

        var manualLyricsQueryTitle by remember { mutableStateOf(playerState?.title ?: "") }

        var manualLyricsQueryArtist by remember { mutableStateOf(playerState?.artist ?: "") }



        var selectedLyricsProvider by remember { mutableStateOf("Auto") }
        var currentLyricsProviderName by remember { mutableStateOf("") }
        var currentLyricsSyncType by remember { mutableStateOf("line") }
        var availableLyricsProviders by remember { mutableStateOf<List<com.mrtdk.liquid_glass.utils.LyricsFetchResult>>(emptyList()) }
        var currentLyricsProviderIndex by remember { mutableStateOf(0) }

        var isRomajiEnabled by remember { mutableStateOf(false) }

        LaunchedEffect(playerState?.videoId) {
            if (playerState?.videoId != null) {
                val offsetKey = "lyrics_offset_${playerState.videoId}"
                val romajiKey = "romanize_lyrics_${playerState.videoId}"
                lyricsOffset = com.mrtdk.liquid_glass.data.LibraryManager.getString(offsetKey, "0")?.toIntOrNull() ?: 0
                isRomajiEnabled = com.mrtdk.liquid_glass.data.LibraryManager.getString(romajiKey, "false") == "true"
            } else {
                lyricsOffset = 0
                isRomajiEnabled = false
            }
            isAutoScrollEnabled = true
        }

        LaunchedEffect(playerState?.title, playerState?.artist, selectedLyricsProvider, isRomajiEnabled, lyricsReloadTrigger) {
            val videoId = playerState?.videoId
            val songTitle = playerState?.title
            val songArtist = playerState?.artist

            if (playerState != null && songTitle != null && songArtist != null) {
                // 1. Instant Cache Display (0ms, sin esperar a la red si ya está en memoria)
                if (lyricsReloadTrigger == 0 && (selectedLyricsProvider.isEmpty() || selectedLyricsProvider == "Auto")) {
                    val cachedResult = com.mrtdk.liquid_glass.utils.LyricsProvider.getCachedLyrics(songArtist, songTitle)
                    if (cachedResult != null && cachedResult.lyrics != null) {
                        currentLyricsProviderName = cachedResult.providerName
                        currentLyricsSyncType = cachedResult.syncType
                        val processed = if (isRomajiEnabled) {
                            val prefs = com.mrtdk.liquid_glass.utils.LyricsRomanizationPreferences(true, true, true, true, true)
                            com.mrtdk.liquid_glass.utils.LyricsUtils.romanizeSyncedLyrics(cachedResult.lyrics, prefs)
                        } else {
                            cachedResult.lyrics
                        }
                        lyricsLines = processed?.lines
                    } else {
                        lyricsLines = null
                        currentLyricsProviderName = ""
                        currentLyricsSyncType = "line"
                    }
                } else {
                    lyricsLines = null
                    currentLyricsProviderName = ""
                    currentLyricsSyncType = "line"
                }

                launch {
                    val customLyricsKey = "custom_lyrics_${videoId ?: ""}"
                    val customLyricsText = if (videoId != null) com.mrtdk.liquid_glass.data.LibraryManager.getString(customLyricsKey) else null
                    val durSec = (duration / 1000).toInt()

                    if (!customLyricsText.isNullOrBlank()) {
                        val parsed = com.mrtdk.liquid_glass.utils.LyricsProvider.parseSyncedLyrics(customLyricsText)
                        if (parsed != null) {
                            val customRes = com.mrtdk.liquid_glass.utils.LyricsFetchResult(parsed, "Personalizado", "line")
                            availableLyricsProviders = listOf(customRes)
                            currentLyricsProviderIndex = 0
                            lyricsLines = parsed.lines
                            currentLyricsProviderName = "Personalizado"
                            currentLyricsSyncType = "line"
                            return@launch
                        }
                    }

                    val allProviders = com.mrtdk.liquid_glass.utils.LyricsProvider.fetchAllAvailableProviders(
                        videoId ?: "",
                        songTitle,
                        songArtist,
                        durSec,
                        playerState.album
                    )

                    if (allProviders.isNotEmpty()) {
                        availableLyricsProviders = allProviders
                        val targetIndex = if (selectedLyricsProvider.isNotEmpty() && selectedLyricsProvider != "Auto") {
                            allProviders.indexOfFirst { it.providerName.equals(selectedLyricsProvider, ignoreCase = true) }.coerceAtLeast(0)
                        } else {
                            0
                        }
                        currentLyricsProviderIndex = targetIndex
                        val activeResult = allProviders[targetIndex]
                        currentLyricsProviderName = activeResult.providerName
                        currentLyricsSyncType = activeResult.syncType

                        val lines = activeResult.lyrics
                        val processedLines = if (isRomajiEnabled && lines != null) {
                            val prefs = com.mrtdk.liquid_glass.utils.LyricsRomanizationPreferences(true, true, true, true, true)
                            com.mrtdk.liquid_glass.utils.LyricsUtils.romanizeSyncedLyrics(lines, prefs)
                        } else {
                            lines
                        }
                        lyricsLines = processedLines?.lines
                    }
                }
            }
        }

        // Prefetching en segundo plano para las próximas canciones en la cola (aparecen en 0ms al pasar de canción)
        LaunchedEffect(playerState?.videoId, playerState?.title) {
            val queue = com.mrtdk.liquid_glass.playback.PlaybackQueue.queue
            val upNext = com.mrtdk.liquid_glass.playback.PlaybackQueue.upNextSongs

            queue.take(3).forEach { qItem ->
                if (qItem.title.isNotBlank() && qItem.artist.isNotBlank()) {
                    val alreadyCached = com.mrtdk.liquid_glass.utils.LyricsProvider.getCachedLyrics(qItem.artist, qItem.title) != null
                    if (!alreadyCached) {
                        launch(Dispatchers.IO) {
                            com.mrtdk.liquid_glass.utils.LyricsProvider.fetchAutoLyrics(
                                qItem.videoId ?: "",
                                qItem.title,
                                qItem.artist,
                                -1,
                                qItem.album
                            )
                        }
                    }
                }
            }

            upNext.take(2).forEach { songItem ->
                val artName = songItem.artists.firstOrNull()?.name.orEmpty()
                if (songItem.title.isNotBlank() && artName.isNotBlank()) {
                    val alreadyCached = com.mrtdk.liquid_glass.utils.LyricsProvider.getCachedLyrics(artName, songItem.title) != null
                    if (!alreadyCached) {
                        launch(Dispatchers.IO) {
                            com.mrtdk.liquid_glass.utils.LyricsProvider.fetchAutoLyrics(
                                songItem.id,
                                songItem.title,
                                artName,
                                -1,
                                songItem.album?.name
                            )
                        }
                    }
                }
            }
        }



        var animatedArtworkUrl by remember(playerState?.artist, playerState?.album, playerState?.title) {

            val artist = playerState?.artist

            val album = playerState?.album

            val title = playerState?.title

            val cached = if (!artist.isNullOrBlank()) {

                val cacheKey = if (!album.isNullOrBlank()) album else title

                if (!cacheKey.isNullOrBlank()) {

                    com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.get(artist, cacheKey)

                } else null

            } else null

            mutableStateOf(cached)

        }

        var isVideoPlaying by remember { mutableStateOf(false) }



        LaunchedEffect(playerState?.artist, playerState?.album, playerState?.title) {

            val artist = playerState?.artist

            val album = playerState?.album

            val title = playerState?.title

            isVideoPlaying = false

            

            if (artist.isNullOrBlank()) return@LaunchedEffect

            if (animatedArtworkUrl != null) return@LaunchedEffect

            

            withContext(Dispatchers.IO) {

                var foundUrl: String? = null

                

                // Try 1: Search using album name if available

                if (!album.isNullOrBlank()) {

                    try {

                        val cleanArtist = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(artist)

                        val cleanAlbum = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(album)

                        val encodedArtist = java.net.URLEncoder.encode(cleanArtist, "UTF-8")

                        val encodedAlbum = java.net.URLEncoder.encode(cleanAlbum, "UTF-8")

                        val url = java.net.URL("https://artwork.m8tec.top/api/v1/artwork/search?artist=$encodedArtist&album=$encodedAlbum")

                        val conn = url.openConnection() as java.net.HttpURLConnection

                        conn.connectTimeout = 3000

                        conn.readTimeout = 3000

                        if (conn.responseCode == 200) {

                            val text = conn.inputStream.bufferedReader().readText()

                            val obj = org.json.JSONObject(text)

                            val streamUrl = obj.optString("url").takeIf { it.isNotBlank() } 

                                ?: obj.optString("url_tall").takeIf { it.isNotBlank() }

                            if (!streamUrl.isNullOrBlank()) {

                                foundUrl = streamUrl

                            }

                        }

                    } catch (e: Exception) {

                        e.printStackTrace()

                    }

                }

                

                // Try 2: Fallback or query using song title if not found yet

                if (foundUrl == null && !title.isNullOrBlank()) {

                    try {

                        val cleanArtist = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(artist)

                        val cleanTitle = com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.cleanTerm(title)

                        val encodedArtist = java.net.URLEncoder.encode(cleanArtist, "UTF-8")

                        val encodedTitle = java.net.URLEncoder.encode(cleanTitle, "UTF-8")

                        val url = java.net.URL("https://artwork.m8tec.top/api/v1/artwork/search?artist=$encodedArtist&album=$encodedTitle")

                        val conn = url.openConnection() as java.net.HttpURLConnection

                        conn.connectTimeout = 3000

                        conn.readTimeout = 3000

                        if (conn.responseCode == 200) {

                            val text = conn.inputStream.bufferedReader().readText()

                            val obj = org.json.JSONObject(text)

                            val streamUrl = obj.optString("url").takeIf { it.isNotBlank() } 

                                ?: obj.optString("url_tall").takeIf { it.isNotBlank() }

                            if (!streamUrl.isNullOrBlank()) {

                                foundUrl = streamUrl

                            }

                        }

                    } catch (e: Exception) {

                        e.printStackTrace()

                    }

                }

                

                if (foundUrl != null) {

                    withContext(Dispatchers.Main) {

                        animatedArtworkUrl = foundUrl

                        val cacheKeyAlbum = if (!album.isNullOrBlank()) album else title ?: ""

                        com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.put(artist, cacheKeyAlbum, foundUrl!!)

                    }

                }

            }

        }



        var coverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        var reflectionSkew by remember { mutableStateOf(0.12f) }

        LaunchedEffect(hdArtUrl) {
            coverBitmap = null
            reflectionSkew = 0.12f

            if (hdArtUrl != null) {
                withContext(Dispatchers.Default) {
                    val request = ImageRequest.Builder(context)
                        .data(hdArtUrl)
                        .allowHardware(false)
                        .size(200)
                        .memoryCachePolicy(coil.request.CachePolicy.READ_ONLY)
                        .build()

                    val result = coil.Coil.imageLoader(context).execute(request)
                    if (result is coil.request.SuccessResult) {
                        val drawable = result.drawable
                        val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                            ?: android.graphics.Bitmap.createBitmap(
                                drawable.intrinsicWidth.coerceAtLeast(1),
                                drawable.intrinsicHeight.coerceAtLeast(1),
                                android.graphics.Bitmap.Config.ARGB_8888
                            ).also {
                                val canvas = android.graphics.Canvas(it)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)
                            }

                        if (bitmap != null) {
                            val asComposeBmp = bitmap.asImageBitmap()
                            val skew = calculateDominantSkew(bitmap)

                            try {
                                var r = 0L; var g = 0L; var b = 0L
                                val yCoord = bitmap.height - 1
                                val w = bitmap.width
                                val stepX = maxOf(1, w / 16)
                                var countX = 0
                                for (x in 0 until w step stepX) {
                                    val pixel = bitmap.getPixel(x, yCoord)
                                    r += android.graphics.Color.red(pixel)
                                    g += android.graphics.Color.green(pixel)
                                    b += android.graphics.Color.blue(pixel)
                                    countX++
                                }
                                val avgColor = Color((r / countX).toInt(), (g / countX).toInt(), (b / countX).toInt())

                                var rRight = 0L; var gRight = 0L; var bRight = 0L
                                val xCoord = bitmap.width - 1
                                val h = bitmap.height
                                val stepY = maxOf(1, h / 16)
                                var countY = 0
                                for (y in 0 until h step stepY) {
                                    val pixel = bitmap.getPixel(xCoord, y)
                                    rRight += android.graphics.Color.red(pixel)
                                    gRight += android.graphics.Color.green(pixel)
                                    bRight += android.graphics.Color.blue(pixel)
                                    countY++
                                }
                                val rightColor = Color((rRight / countY).toInt(), (gRight / countY).toInt(), (bRight / countY).toInt())

                                withContext(Dispatchers.Main) {
                                    coverBitmap = asComposeBmp
                                    reflectionSkew = skew
                                    bottomAverageColor = avgColor
                                    dominantColor = avgColor
                                    onDominantColorChanged(avgColor)
                                    rightSideAverageColor = rightColor
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    coverBitmap = asComposeBmp
                                    reflectionSkew = skew
                                }
                            }
                        }
                    }
                }
            }

        }

        

        val isLightBackground = run {
            val r = bottomAverageColor.red
            val g = bottomAverageColor.green
            val b = bottomAverageColor.blue
            val maxCh = maxOf(r, g, b)
            val minCh = minOf(r, g, b)
            bottomAverageColor.luminance() > 0.90f && (maxCh - minCh) < 0.12f && minCh > 0.85f
        }

        val contentColor = if (isLightBackground) Color(0xFF1A1A1A) else Color.White



        val animatedImageLoader = remember(context) {

            coil.ImageLoader.Builder(context)

                .components {

                    add(com.mrtdk.liquid_glass.utils.CoilUtils.HdThumbnailInterceptor())

                    if (android.os.Build.VERSION.SDK_INT >= 28) {

                        add(coil.decode.ImageDecoderDecoder.Factory())

                    } else {

                        add(coil.decode.GifDecoder.Factory())

                    }

                }

                .build()

        }



        val savedItems by LibraryManager.savedItems.collectAsState()

        val isSaved = savedItems.any { it.id == playerState?.videoId }

        val starTint by androidx.compose.animation.animateColorAsState(targetValue = if(isSaved) Color(0xFFFA243C) else contentColor, label="starTint")



        GlassContainer(

            modifier = Modifier.fillMaxSize(),

            useShader = true,

            content = {

                BoxWithConstraints(

                    modifier = Modifier

                        .fillMaxSize()

                        .onGloballyPositioned { parentCoordinates = it }

                        .layerBackdrop(localBackdrop)

                ) {

            val maxWidth = maxWidth

            val maxHeight = maxHeight



            val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE



            if (isLandscape) {

                LandscapePlayerLayout(

                    playerState = playerState,

                    isPlaying = isPlaying,

                    currentPosition = currentPosition,

                    duration = duration,

                    upNextSongs = upNextSongs,

                    onUpNextSongsChange = onUpNextSongsChange,

                    onSkipNext = onSkipNext,

                    onSkipPrevious = onSkipPrevious,

                    onClose = onClose,

                    onTogglePlayPause = onTogglePlayPause,

                    onSeek = onSeek,

                    onVolumeChange = onVolumeChange,

                    onArtistSelected = onArtistSelected,

                    onAlbumSelected = onAlbumSelected,

                    onSongSelected = onSongSelected,

                    onSongSelectedFromQueue = onSongSelectedFromQueue,

                    shuffleModeEnabled = shuffleModeEnabled,

                    repeatMode = repeatMode,

                    onToggleShuffle = onToggleShuffle,

                    onToggleRepeat = onToggleRepeat,

                    showLyrics = showLyrics,

                    onShowLyricsChange = { showLyrics = it },

                    showQueue = showQueue,

                    onShowQueueChange = { showQueue = it },

                    volumePosition = volumePosition,

                    onVolumePositionChange = { volumePosition = it },

                    coverBitmap = coverBitmap,

                    onCoverBitmapChange = { coverBitmap = it },

                    dominantColor = dominantColor,

                    onDominantColorChange = { dominantColor = it },

                    bottomAverageColor = bottomAverageColor,

                    onBottomAverageColorChange = { bottomAverageColor = it },

                    rightSideAverageColor = rightSideAverageColor,

                    onRightSideAverageColorChange = { rightSideAverageColor = it },

                    hdArtUrl = hdArtUrl,

                    lyricsLines = lyricsLines,

                    isRomajiEnabled = isRomajiEnabled,

                    onToggleRomaji = { isRomajiEnabled = !isRomajiEnabled },

                    isSaved = isSaved,

                    animatedArtworkUrl = animatedArtworkUrl,

                    isVideoPlaying = isVideoPlaying,

                    onVideoPlayingChange = { isVideoPlaying = it },
                    animatedImageLoader = animatedImageLoader,
                    isLightBackground = isLightBackground,
                    contentColor = contentColor,
                    onShowOptionsMenu = { bounds -> menuPivotBounds = bounds; showOptionsMenu = true },
                    onShowLyricsMenu = { showLyricsOptionsMenu = true },
                    onShowPlaylistMenu = { showPlaylistMenu = true },
                    onShowArtistMenu = { artists, bounds ->
                        artistMenuOptions = artists
                        artistPivotBounds = bounds
                        showArtistOptionsMenu = true
                    },
                    isBottomBarCollapsed = isBottomBarCollapsed,
                    isAutoScrollEnabled = isAutoScrollEnabled,
                    scrollToCurrentTrigger = scrollToCurrentTrigger,
                    onAutoScrollChange = { isAutoScrollEnabled = it },
                    lyricsOffset = lyricsOffset
                )
            } else {
                val lyricsImageSize = 84.dp

            val isOverlayActive = showLyrics || showQueue

            

            // Calculating destinations depending on normal vs collapsed bottom bar

            val normalTargetOffsetX = 28.dp

            val collapsedTargetOffsetX = 92.dp

            val normalTargetOffsetY = maxHeight - 148.dp

            val collapsedTargetOffsetY = maxHeight - 64.dp

            

            val targetOffsetX = if (isBottomBarCollapsed) collapsedTargetOffsetX else normalTargetOffsetX

            val targetOffsetY = if (isBottomBarCollapsed) collapsedTargetOffsetY else normalTargetOffsetY

            

            val density = androidx.compose.ui.platform.LocalDensity.current

            val maxDragDistance = with(density) { targetOffsetY.toPx() }

            val sliderYDp = remember(sliderCoordinates, parentCoordinates) {

                val sliderCoords = sliderCoordinates

                val parentCoords = parentCoordinates

                if (sliderCoords != null && parentCoords != null && sliderCoords.isAttached && parentCoords.isAttached) {

                    val localOffset = parentCoords.localPositionOf(sliderCoords, Offset.Zero)

                    with(density) { localOffset.y.toDp() }

                } else {

                    0.dp

                }

            }

            val dragProgress = if (maxDragDistance > 0f) (offsetY / maxDragDistance).coerceIn(0f, 1f) else 0f

            var frozenSliderYDp by remember { mutableStateOf(0.dp) }

            // Capture stable slider position only after the overlay has fully closed and
            // the layout has settled (wait one frame after isOverlayActive becomes false).
            LaunchedEffect(isOverlayActive, dragProgress) {
                if (!isOverlayActive && dragProgress == 0f) {
                    // Wait for fade animation + layout to settle before reading coordinates
                    kotlinx.coroutines.delay(350L)
                    val y = sliderYDp
                    if (y > 0.dp) frozenSliderYDp = y
                }
            }

            // On first load (before any overlay), also capture immediately
            if (!isOverlayActive && dragProgress == 0f && sliderYDp > 0.dp && frozenSliderYDp == 0.dp) {
                frozenSliderYDp = sliderYDp
            }

            val stableSliderYDp = if (frozenSliderYDp > 0.dp) frozenSliderYDp else sliderYDp

            

            val bgAlpha = 1f - dragProgress

            

            val expandedWidth = maxWidth

            val expandedHeight = maxWidth * 1.15f

            val expandedX = 0.dp

            val expandedY = 0.dp

            

            val startWidth = if (isOverlayActive) lyricsImageSize else expandedWidth

            val startHeight = if (isOverlayActive) lyricsImageSize else expandedHeight

            val startOffsetX = if (isOverlayActive) 24.dp else expandedX

            val startOffsetY = if (isOverlayActive) 64.dp else expandedY

            val startCorner = if (isOverlayActive) 8.dp else 12.dp



            val p = dragProgress.coerceIn(0f, 1f)

            val imgOffsetYTarget = androidx.compose.ui.unit.lerp(startOffsetY, targetOffsetY, p)

            val threshold = 0.85f
            val imgWidthTarget: androidx.compose.ui.unit.Dp
            val imgHeightTarget: androidx.compose.ui.unit.Dp
            val imgOffsetXTarget: androidx.compose.ui.unit.Dp
            val imageCornerTarget: androidx.compose.ui.unit.Dp

            if (isOverlayActive) {
                imgWidthTarget = androidx.compose.ui.unit.lerp(startWidth, 40.dp, p)
                imgHeightTarget = androidx.compose.ui.unit.lerp(startHeight, 40.dp, p)
                imgOffsetXTarget = androidx.compose.ui.unit.lerp(startOffsetX, targetOffsetX, p)
                imageCornerTarget = androidx.compose.ui.unit.lerp(startCorner, 20.dp, p)
            } else {
                if (p <= threshold) {
                    imgWidthTarget = startWidth
                    imgHeightTarget = startHeight
                    imgOffsetXTarget = startOffsetX
                    imageCornerTarget = startCorner
                } else {
                    val p2 = if (threshold < 1f) (p - threshold) / (1f - threshold) else 1f
                    val p2Smooth = p2 * p2
                    imgWidthTarget = androidx.compose.ui.unit.lerp(startWidth, 40.dp, p2Smooth)
                    imgHeightTarget = androidx.compose.ui.unit.lerp(startHeight, 40.dp, p2Smooth)
                    imgOffsetXTarget = androidx.compose.ui.unit.lerp(startOffsetX, targetOffsetX, p2Smooth)
                    imageCornerTarget = androidx.compose.ui.unit.lerp(startCorner, 20.dp, p2Smooth)
                }
            }

            val contentAlpha = (1f - p * 2.2f).coerceIn(0f, 1f)
            val overlayAlpha = (1f - p * 2.2f).coerceIn(0f, 1f)



            val animatedImgWidth by androidx.compose.animation.core.animateDpAsState(imgWidthTarget)
            val animatedImgHeight by androidx.compose.animation.core.animateDpAsState(imgHeightTarget)
            val animatedImgOffsetX by androidx.compose.animation.core.animateDpAsState(imgOffsetXTarget)
            val animatedImgOffsetY by androidx.compose.animation.core.animateDpAsState(imgOffsetYTarget)
            val animatedImgCorner by androidx.compose.animation.core.animateDpAsState(imageCornerTarget)

            val imgWidth = if (dragProgress > 0f) imgWidthTarget else animatedImgWidth
            val imgHeight = if (dragProgress > 0f) imgHeightTarget else animatedImgHeight
            val imgOffsetX = if (dragProgress > 0f) imgOffsetXTarget else animatedImgOffsetX
            val imgOffsetY = if (dragProgress > 0f) imgOffsetYTarget else animatedImgOffsetY
            val imgCorner = if (dragProgress > 0f) imageCornerTarget else animatedImgCorner



            val detailsOffsetYTarget = if (isOverlayActive) 64.dp else (imgOffsetYTarget + imgHeightTarget + 12.dp)

            val detailsOffsetY by androidx.compose.animation.core.animateDpAsState(detailsOffsetYTarget)



            val detailsOffsetXTarget = if (isOverlayActive) 124.dp else 24.dp

            val detailsOffsetX by androidx.compose.animation.core.animateDpAsState(detailsOffsetXTarget)



            val detailsWidthTarget = if (isOverlayActive) (maxWidth - 124.dp - 24.dp) else (maxWidth - 48.dp)

            val detailsWidth by androidx.compose.animation.core.animateDpAsState(detailsWidthTarget)



            val titleFontSizeTarget = if (isOverlayActive) 18f else 24f

            val titleFontSizeFloat by androidx.compose.animation.core.animateFloatAsState(titleFontSizeTarget)

            val titleFontSize = titleFontSizeFloat.sp



            val artistFontSizeTarget = if (isOverlayActive) 15f else 18f

            val artistFontSizeFloat by androidx.compose.animation.core.animateFloatAsState(artistFontSizeTarget)

            val artistFontSize = artistFontSizeFloat.sp



            val starIconSizeTarget = if (isOverlayActive) 20.dp else 18.dp

            val starIconSize by androidx.compose.animation.core.animateDpAsState(starIconSizeTarget)



            val moreIconSizeTarget = if (isOverlayActive) 24.dp else 18.dp

            val moreIconSize by androidx.compose.animation.core.animateDpAsState(moreIconSizeTarget)



            val nestedScrollConnection = remember(maxDragDistance, onClose, showLyrics, showQueue) {

                object : NestedScrollConnection {

                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {

                        if (showLyrics || showQueue) return Offset.Zero

                        val delta = available.y

                        val currentOffsetY = dragOffsetY.value

                        if (currentOffsetY > 0f && delta < 0f) {

                            val newOffset = (currentOffsetY + delta).coerceAtLeast(0f)

                            val consumed = newOffset - currentOffsetY

                            scope.launch { dragOffsetY.snapTo(newOffset) }

                            return Offset(0f, consumed)

                        }

                        return Offset.Zero

                    }



                    override fun onPostScroll(

                        consumed: Offset,

                        available: Offset,

                        source: NestedScrollSource

                    ): Offset {

                        if (showLyrics || showQueue) return Offset.Zero

                        val delta = available.y

                        val currentOffsetY = dragOffsetY.value

                        

                        val isAtTop = when {

                            showLyrics -> lyricsListState.firstVisibleItemIndex == 0 && lyricsListState.firstVisibleItemScrollOffset == 0

                            showQueue -> queueListState.firstVisibleItemIndex == 0 && queueListState.firstVisibleItemScrollOffset == 0

                            else -> true

                        }

                        

                        if (delta > 0f && isAtTop) {
                            val newOffset = (currentOffsetY + delta).coerceAtLeast(0f)
                            scope.launch { dragOffsetY.snapTo(newOffset) }
                            return Offset(0f, delta)
                        }

                        return Offset.Zero

                    }



                    override suspend fun onPreFling(available: Velocity): Velocity {

                        if (showLyrics || showQueue) return Velocity.Zero

                        val currentOffsetY = dragOffsetY.value

                        if (currentOffsetY > with(density) { 150.dp.toPx() }) {

                            dragOffsetY.animateTo(

                                targetValue = maxDragDistance,

                                animationSpec = tween(300, easing = FastOutSlowInEasing)

                            )

                            onClose()

                        } else {

                            dragOffsetY.animateTo(0f, spring())

                        }

                        return Velocity.Zero

                    }

                }

            }



            Box(

                modifier = Modifier

                    .fillMaxSize()

                    .graphicsLayer {
                        alpha = bgAlpha
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                dominantColor.copy(alpha = 1.0f),
                                Color(0xFF16181B),
                                Color(0xFF101113)
                            )
                        )
                    )
                    .pointerInput(showLyrics, showQueue) {

                        if (!showLyrics && !showQueue) {

                            detectVerticalDragGestures(

                                onDragEnd = {

                                    val currentOffsetY = dragOffsetY.value

                                    if (currentOffsetY > with(density) { 150.dp.toPx() }) {

                                        scope.launch {

                                            dragOffsetY.animateTo(

                                                targetValue = maxDragDistance,

                                                animationSpec = tween(300, easing = FastOutSlowInEasing)

                                            )

                                            onClose()

                                        }

                                    } else {

                                        scope.launch {

                                            dragOffsetY.animateTo(0f, spring())

                                        }

                                    }

                                }

                            ) { change, dragAmount ->

                                if (dragAmount > 0f || dragOffsetY.value > 0f) {
                                    val newOffset = (dragOffsetY.value + dragAmount).coerceAtLeast(0f)
                                    scope.launch { dragOffsetY.snapTo(newOffset) }
                                }

                            }

                        }

                    }
            ) {

            // Capa 4: Reflejo invertido estilo Apple Music (solo en vista principal)
            val mirrorArtModel = hdArtUrl ?: playerState?.artUrl
            if ((coverBitmap != null || mirrorArtModel != null) && dragProgress < 1f && !showLyrics && !showQueue) {
                val reflectionWidth = maxWidth
                val reflectionX = 0.dp
                val childWidth = expandedWidth
                val childOffsetX = expandedX
                val overlapY = 0.dp
                val baseReflectionY = expandedY + expandedHeight
                val reflectionY = baseReflectionY
                val reflectionHeight = (maxHeight - baseReflectionY).coerceAtLeast(expandedHeight)
                val mirrorImageHeight = reflectionHeight
                
                val stretchY = 3.80f
                val verticalScale = -stretchY
                val pivotY = 0f

                val blurRadiusPx = with(density) { 15.dp.toPx() }
                val maskBitmapCache = remember { arrayOfNulls<androidx.compose.ui.graphics.ImageBitmap>(1) }

                Box(
                    modifier = Modifier
                        .offset(x = reflectionX, y = reflectionY)
                        .width(reflectionWidth)
                        .height(reflectionHeight)
                        .clipToBounds()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            alpha = (1f - dragProgress).coerceIn(0f, 1f)
                        }
                ) {
                    val currentBitmap = coverBitmap
                    val mirrorModel = mirrorArtModel ?: currentBitmap

                    Box(
                        modifier = Modifier
                            .offset(x = childOffsetX, y = 0.dp)
                            .width(childWidth)
                            .height(mirrorImageHeight)
                    ) {
                        if (currentBitmap != null || mirrorModel != null) {
                            val sliderFraction = if (reflectionHeight > 0.dp && stableSliderYDp > baseReflectionY) {
                                ((stableSliderYDp - baseReflectionY) / reflectionHeight).coerceIn(0.15f, 0.45f)
                            } else {
                                0.28f
                            }

                            val currentAnimatedUrl = animatedArtworkUrl

                            // Reflejo con difuminado estilo Apple Music (difusión horizontal expandida de tonos de fondo)
                            com.mrtdk.liquid_glass.ui.components.GraduatedBlurArtwork(
                                imageUrl = currentBitmap ?: mirrorModel,
                                videoUrl = if (isVideoPlaying && !currentAnimatedUrl.isNullOrBlank()) currentAnimatedUrl else null,
                                modifier = Modifier.fillMaxSize(),
                                blurRadiusX = 150.dp,
                                blurRadiusY = 75.dp,
                                frostedRadius = 25.dp,
                                verticalScale = verticalScale,
                                pivotY = 0f,
                                horizontalScale = 1.0f,
                                blurTransitionEndFraction = sliderFraction,
                                imageLoader = animatedImageLoader
                            )
                        }
                    }
                }
            }


            // LYRICS / QUEUE OVERLAY

            AnimatedVisibility(

                visible = showLyrics || showQueue,

                enter = fadeIn(),

                exit = fadeOut()

            ) {

                 Box(

                     modifier = Modifier

                         .fillMaxSize()

                         .graphicsLayer { alpha = overlayAlpha }

                 ) {

                     // Full-screen rich blurred background image of album cover for lyrics/queue view
                     val currentOverlayCover = coverBitmap
                     val overlayArtSource = currentOverlayCover ?: mirrorArtModel
                     val currentAnimatedUrl = animatedArtworkUrl

                     if (isVideoPlaying && !currentAnimatedUrl.isNullOrBlank()) {
                         Box(
                             modifier = Modifier
                                 .fillMaxSize()
                                 .graphicsLayer {
                                     scaleX = 1.35f
                                     scaleY = 1.35f
                                 }
                                 .blur(120.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                         ) {
                             com.mrtdk.liquid_glass.ui.components.AnimatedArtworkPlayer(
                                 videoUrl = currentAnimatedUrl,
                                 modifier = Modifier.fillMaxSize(),
                                 isPaused = false,
                                 enableFrameCapture = false
                             )
                         }
                     } else if (overlayArtSource != null) {
                         if (overlayArtSource is ImageBitmap) {
                             Image(
                                 bitmap = overlayArtSource,
                                 contentDescription = null,
                                 contentScale = ContentScale.Crop,
                                 modifier = Modifier
                                     .fillMaxSize()
                                     .graphicsLayer {
                                         scaleX = 1.35f
                                         scaleY = 1.35f
                                     }
                                     .blur(120.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                             )
                         } else {
                             AsyncImage(
                                 model = ImageRequest.Builder(context)
                                     .data(overlayArtSource)
                                     .crossfade(true)
                                     .build(),
                                 imageLoader = animatedImageLoader,
                                 contentDescription = null,
                                 contentScale = ContentScale.Crop,
                                 modifier = Modifier
                                     .fillMaxSize()
                                     .graphicsLayer {
                                         scaleX = 1.35f
                                         scaleY = 1.35f
                                     }
                                     .blur(120.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                             )
                         }
                     }

                     // Capa de difusión de colores vivos y contraste de texto para letras y cola
                     Box(
                         modifier = Modifier
                             .fillMaxSize()
                             .background(
                                 Brush.verticalGradient(
                                     colors = listOf(
                                         dominantColor.copy(alpha = 0.32f),
                                         bottomAverageColor.copy(alpha = 0.28f),
                                         Color.Black.copy(alpha = 0.45f)
                                     )
                                 )
                             )
                     )
                 }

                      // Height of the content area = exactly the cover image height (player controls start below)

                      // For lyrics immersive mode (controls hidden) animate up to full screen height

                      val overlayContentHeight by animateDpAsState(

                          targetValue = if (showLyrics && !showLyricsControls) maxHeight else expandedY + expandedHeight + 72.dp,

                          animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),

                          label = "overlayContentHeight"

                      )

                       Column(
                           modifier = Modifier
                               .fillMaxWidth()
                               .height(overlayContentHeight)
                       ) {

                       if (showQueue) {
                           Spacer(modifier = Modifier.height(148.dp))
                       } else {
                           // Lyrics: slightly lower start so lyrics appear above the player controls
                           Spacer(modifier = Modifier.height(120.dp))
                       }
                       
                       if (showQueue) {
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val shuffleInteraction = remember { MutableInteractionSource() }
                            val isShuffleActive = shuffleModeEnabled
                            val isShufflePressed by shuffleInteraction.collectIsPressedAsState()
                            val shuffleScale by animateFloatAsState(targetValue = if (isShufflePressed) 0.85f else 1.0f, label = "shuffleScale")
                            
                            val activeBg = contentColor.copy(alpha = 0.9f)
                            val activeIcon = if (contentColor == Color.White) dominantColor else Color.White

                            val shuffleBgColor by animateColorAsState(targetValue = if (isShuffleActive) activeBg else contentColor.copy(alpha=0.15f), label = "shuffleBg")
                            val shuffleIconColor by animateColorAsState(targetValue = if (isShuffleActive) activeIcon else contentColor.copy(alpha=0.5f), label = "shuffleIcon")

                            val repeatInteraction = remember { MutableInteractionSource() }
                            val isRepeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF
                            val isRepeatPressed by repeatInteraction.collectIsPressedAsState()
                            val repeatScale by animateFloatAsState(targetValue = if (isRepeatPressed) 0.85f else 1.0f, label = "repeatScale")
                            val repeatBgColor by animateColorAsState(targetValue = if (isRepeatActive) activeBg else contentColor.copy(alpha=0.15f), label = "repeatBg")
                            val repeatIconColor by animateColorAsState(targetValue = if (isRepeatActive) activeIcon else contentColor.copy(alpha=0.5f), label = "repeatIcon")

                            val autoplayInteraction = remember { MutableInteractionSource() }
                            val isAutoplayActive = !playerState.isExclusiveQueue
                            val isAutoplayPressed by autoplayInteraction.collectIsPressedAsState()
                            val autoplayScale by animateFloatAsState(targetValue = if (isAutoplayPressed) 0.85f else 1.0f, label = "autoplayScale")
                            val autoplayBgColor by animateColorAsState(targetValue = if (isAutoplayActive) activeBg else contentColor.copy(alpha=0.15f), label = "autoplayBg")
                            val autoplayIconColor by animateColorAsState(targetValue = if (isAutoplayActive) activeIcon else contentColor.copy(alpha=0.5f), label = "autoplayIcon")

                            val romajiInteraction = remember { MutableInteractionSource() }
                            val isRomajiActive = isRomajiEnabled
                            val isRomajiPressed by romajiInteraction.collectIsPressedAsState()
                            val romajiScale by animateFloatAsState(targetValue = if (isRomajiPressed) 0.85f else 1.0f, label = "romajiScale")
                            val romajiBgColor by animateColorAsState(targetValue = if (isRomajiActive) activeBg else contentColor.copy(alpha=0.15f), label = "romajiBg")
                            val romajiIconColor by animateColorAsState(targetValue = if (isRomajiActive) activeIcon else contentColor.copy(alpha=0.5f), label = "romajiIcon")

                            Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = shuffleScale, scaleY = shuffleScale)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(shuffleBgColor)
                                        .clickable(
                                            interactionSource = shuffleInteraction,
                                            indication = null,
                                            onClick = onToggleShuffle
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(painterResource(id = R.drawable.shuffle), "Shuffle", tint = shuffleIconColor, modifier = Modifier.size(20.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = repeatScale, scaleY = repeatScale)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(repeatBgColor)
                                        .clickable(
                                            interactionSource = repeatInteraction,
                                            indication = null,
                                            onClick = onToggleRepeat
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val repeatIcon = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                                    Icon(repeatIcon, "Repeat", tint = repeatIconColor, modifier = Modifier.size(20.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = autoplayScale, scaleY = autoplayScale)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(autoplayBgColor)
                                        .clickable(
                                            interactionSource = autoplayInteraction,
                                            indication = null,
                                            onClick = {
                                                onSongSelected(playerState.copy(isExclusiveQueue = !playerState.isExclusiveQueue))
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AllInclusive, "Autoplay", tint = autoplayIconColor, modifier = Modifier.size(20.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = romajiScale, scaleY = romajiScale)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(romajiBgColor)
                                        .clickable(
                                            interactionSource = romajiInteraction,
                                            indication = null,
                                            onClick = {
                                                isRomajiEnabled = !isRomajiEnabled
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(if (isRomajiActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff, "Toggle", tint = romajiIconColor, modifier = Modifier.size(24.dp))
                                }
                            }

                             if (playerState != null && playerState.queue.isNotEmpty()) {
                               Text(text = stringResource(R.string.siguiente_en_album_playlist), color = contentColor, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp, bottom = 4.dp))
                             } else if (playerState?.isExclusiveQueue != true) {
                                  Column(modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp, bottom = 4.dp)) {
                                      Text(text = stringResource(R.string.continue_playing), color = contentColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                      Text(text = stringResource(R.string.autoplaying_similar_music), color = contentColor.copy(alpha = 0.7f), fontSize = 13.5.sp)
                                  }
                             }
                          
                            // Fading edge Box: clips queue list before it reaches the player controls area
                             Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                            LazyColumn(
                                state = queueListState, 
                                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), 
                                contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                               if (playerState != null) {
                                   item {
                                       Text(
                                           text = stringResource(R.string.player_queue_now_playing),
                                           color = contentColor,
                                           fontSize = 17.sp,
                                           fontWeight = FontWeight.Bold,
                                           modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                       )
                                      Row(
                                          verticalAlignment = Alignment.CenterVertically,
                                          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                      ) {
                                          val upgradedArt = playerState.artUrl?.let {
                                              val itStr = it.toString()
                                              if (itStr.startsWith("file:///android_asset/")) {
                                                  it
                                              } else {
                                                  val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(itStr) ?: itStr
                                                  if (it is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
                                              }
                                          } ?: playerState.artUrl

                                          AsyncImage(
                                              model = ImageRequest.Builder(context).data(upgradedArt).crossfade(false).build(),
                                              contentDescription = null,
                                              modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
                                          )
                                          Spacer(modifier = Modifier.width(12.dp))
                                          Column(modifier = Modifier.weight(1f)) {
                                              Text(playerState.title ?: "", color = contentColor, fontSize = 16.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                                              Text(playerState.artist ?: "", color = contentColor.copy(alpha=0.6f), fontSize = 14.sp, maxLines = 1)
                                          }
                                          PlayingEqualizer(color = contentColor, isPlaying = isPlaying, modifier = Modifier.size(24.dp))
                                      }
                                      androidx.compose.material3.Divider(color = Color.DarkGray.copy(alpha = 0.5f))
                                  }
                              }

                              // 1. Manual Queue Section (Album/Playlist)

                              if (playerState != null && playerState.queue.isNotEmpty()) {
                                   val state = playerState
                                   items(
                                       count = state.queue.size,
                                       key = { index -> "${state.queue[index].videoId}_$index" }
                                   ) { index ->
                                        val qItem = state.queue[index]
                                        val isCurrent = state.videoId != null && qItem.videoId == state.videoId

                                        val rowData = remember(qItem.title, qItem.artist, qItem.artUrl) {
                                            QueueItemRowData(
                                                title = qItem.title,
                                                artist = qItem.artist,
                                                artUrl = qItem.artUrl
                                            )
                                        }

                                        val onRowClick = remember(qItem, index, state) {
                                            {
                                                swipeDirection = 1
                                                val upgradedArt = qItem.artUrl?.let {
                                                    val itStr = it.toString()
                                                    if (itStr.startsWith("file:///android_asset/")) {
                                                        it
                                                    } else {
                                                        val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(itStr) ?: itStr
                                                        if (it is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
                                                    }
                                                } ?: qItem.artUrl
                                                val remaining = state.queue.drop(index + 1)
                                                onSongSelectedFromQueue(PlayerState(
                                                    title = qItem.title,
                                                    artist = qItem.artist,
                                                    artUrl = upgradedArt,
                                                    videoId = qItem.videoId,
                                                    queue = remaining,
                                                    isExclusiveQueue = state.isExclusiveQueue,
                                                    album = qItem.album,
                                                    albumId = qItem.albumId
                                                ))
                                            }
                                        }

                                        QueueItemRow(
                                             rowData = rowData,
                                             contentColor = contentColor,
                                             isPlaying = if (isCurrent) isPlaying else false,
                                             isCurrentPlayingItem = isCurrent,
                                             context = context,
                                             titleFontSize = 16.sp,
                                             artistFontSize = 14.sp,
                                             onClick = onRowClick
                                        )
                                    }
                               }

                               if (playerState?.isExclusiveQueue != true) {
                                   val state = playerState
                                   itemsIndexed(
                                       items = upNextSongs,
                                       key = { index, song -> "${song.id}_$index" }
                                   ) { i, song ->
                                       val isCurrent = state != null && song.id == state.videoId

                                       val rowData = remember(song.title, song.artists, song.thumbnail) {
                                           UpNextSongRowData(
                                               title = song.title,
                                               artist = song.artists.joinToString { it.name },
                                               thumbnail = song.thumbnail
                                           )
                                       }

                                       val onRowClick = remember(song, i, upNextSongs, state) {
                                           {
                                               swipeDirection = 1
                                               val upgradedArt = song.thumbnail?.let {
                                                   com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(it) ?: it
                                               } ?: song.thumbnail
                                               onUpNextSongsChange(upNextSongs.drop(i + 1))
                                               onSongSelectedFromQueue(PlayerState(
                                                   title = song.title,
                                                   artist = song.artists.joinToString { it.name },
                                                   artUrl = upgradedArt,
                                                   videoId = song.id,
                                                   isExclusiveQueue = state?.isExclusiveQueue ?: false,
                                                   album = song.album?.name,
                                                   albumId = song.album?.id
                                               ))
                                           }
                                       }

                                       UpNextSongRow(
                                           rowData = rowData,
                                           contentColor = contentColor,
                                           context = context,
                                           isPlaying = if (isCurrent) isPlaying else false,
                                           isCurrentPlayingItem = isCurrent,
                                           titleFontSize = 16.sp,
                                           artistFontSize = 14.sp,
                                           onClick = onRowClick
                                       )
                                   }
                               }

                          }

                      }

                      } else if (showLyrics) {

                              Spacer(modifier = Modifier.height(16.dp))

                              Box(
                                  modifier = Modifier
                                      .weight(1f)
                                      .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                      .drawWithContent {
                                          drawContent()
                                          // Fade the lyrics at top and bottom for immersive depth
                                          drawRect(
                                              brush = Brush.verticalGradient(
                                                  colorStops = arrayOf(
                                                      0.0f to Color.Transparent,
                                                      0.04f to Color.Black.copy(alpha = 0.7f),
                                                      0.08f to Color.Black,
                                                      0.90f to Color.Black,
                                                      0.96f to Color.Black.copy(alpha = 0.3f),
                                                      1.0f to Color.Transparent
                                                  )
                                              ),
                                              blendMode = BlendMode.DstIn
                                          )
                                      }
                                      .clipToBounds()
                                      .pointerInput(Unit) {
                                          val swipeLyrics = com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_swipe_to_change_song", "false") == "true"
                                          if (swipeLyrics) {
                                              var dragAccumulator = 0f
                                              detectHorizontalDragGestures(
                                                  onDragStart = { dragAccumulator = 0f },
                                                  onDragEnd = {
                                                      if (dragAccumulator < -100f) {
                                                          onSkipNext()
                                                      } else if (dragAccumulator > 100f) {
                                                          onSkipPrevious()
                                                      }
                                                  },
                                                  onHorizontalDrag = { change, dragAmount ->
                                                      change.consume()
                                                      dragAccumulator += dragAmount
                                                  }
                                              )
                                          }
                                      }
                                      .clickable(
                                          interactionSource = remember { MutableInteractionSource() },
                                          indication = null
                                      ) {
                                          val lyricsThumbPlayPause = com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_thumbnail_play_pause", "false") == "true"
                                          if (lyricsThumbPlayPause) {
                                              onTogglePlayPause()
                                          } else {
                                              showLyricsControls = !showLyricsControls
                                              if (showLyricsControls) {
                                                  lyricsControlsHideTrigger++
                                              }
                                          }
                                      }
                              ) {
                                  val currentLyricsLines = lyricsLines

                                  if (currentLyricsLines != null && currentLyricsLines.isNotEmpty()) {
                                      val isSynced = currentLyricsLines.any { it.timeMs > 0L }

                                      val lyricsScrollConnection = remember {
                                          object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                                              override fun onPostScroll(
                                                  consumed: Offset,
                                                  available: Offset,
                                                  source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
                                              ): Offset {
                                                  if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
                                                      isAutoScrollEnabled = false
                                                  }
                                                  return Offset.Zero
                                              }

                                              override suspend fun onPostFling(
                                                  consumed: Velocity,
                                                  available: Velocity
                                              ): Velocity {
                                                  isAutoScrollEnabled = false
                                                  return Velocity.Zero
                                              }
                                          }
                                      }

                                      val lyricsTextSize = com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_text_size", "28")?.toFloatOrNull() ?: 28f
                                      val lyricsLineSpacing = com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_line_spacing", "1.3")?.toFloatOrNull() ?: 1.3f
                                      val lyricsGlowEffect = com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_glow_effect", "false") == "true"
                                      val lyricsTextPosition = com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_text_position", "left") ?: "left"
                                      val lyricsClickChange = com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_click_change", "true") == "true"
                                      val lyricsAutoScroll = com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_auto_scroll", "true") == "true"

                                      // Better Lyrics Display Items Model (Lines + Instrumental Breaks)
                                      val displayItems = remember(currentLyricsLines) {
                                          val items = mutableListOf<Any>()
                                          if (!isSynced) {
                                              currentLyricsLines.forEachIndexed { idx, line ->
                                                  items.add(Pair(idx, line))
                                              }
                                              return@remember items
                                          }

                                          val firstLine = currentLyricsLines.firstOrNull { it.timeMs > 0L }
                                          if (firstLine != null && firstLine.timeMs >= 5000L) {
                                              items.add(Triple(0L, firstLine.timeMs, 0L)) // Intro instrumental break
                                          }

                                          for (i in currentLyricsLines.indices) {
                                              val line = currentLyricsLines[i]
                                              val nextLine = currentLyricsLines.getOrNull(i + 1)
                                              items.add(Pair(i, line))

                                              if (nextLine != null && line.timeMs > 0L && nextLine.timeMs > line.timeMs) {
                                                  val estimatedEnd = line.timeMs + 3500L
                                                  val gap = nextLine.timeMs - estimatedEnd
                                                  if (gap >= 4500L) {
                                                      items.add(Triple(estimatedEnd, nextLine.timeMs, estimatedEnd))
                                                  }
                                              }
                                          }
                                          items
                                      }

                                      val activeItemIndex = remember(currentPosition, lyricsOffset, displayItems) {
                                          if (!isSynced) return@remember 0
                                          val pos = currentPosition + lyricsOffset
                                          displayItems.indexOfLast { item ->
                                              when (item) {
                                                  is Pair<*, *> -> {
                                                      val line = item.second as com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
                                                      line.timeMs != -1L && line.timeMs <= pos + 400
                                                  }
                                                  is Triple<*, *, *> -> {
                                                      val start = item.first as Long
                                                      val end = item.second as Long
                                                      pos in start until end
                                                  }
                                                  else -> false
                                              }
                                          }
                                      }

                                      LaunchedEffect(activeItemIndex, isAutoScrollEnabled, scrollToCurrentTrigger, lyricsAutoScroll) {
                                          if (!isSynced || !lyricsAutoScroll) return@LaunchedEffect
                                          if (!isAutoScrollEnabled && scrollToCurrentTrigger == 0) return@LaunchedEffect
                                          if (activeItemIndex >= 0 && !lyricsListState.isScrollInProgress) {
                                              lyricsListState.animateScrollToItem(activeItemIndex.coerceAtLeast(0), scrollOffset = -120)
                                          }
                                      }

                                      val infiniteTransition = rememberInfiniteTransition(label = "instrumentalWave")
                                      val instrumentalWavePhase by infiniteTransition.animateFloat(
                                          initialValue = 0f,
                                          targetValue = 1f,
                                          animationSpec = infiniteRepeatable(
                                              animation = tween(1200, easing = LinearEasing),
                                              repeatMode = RepeatMode.Restart
                                          ),
                                          label = "instrumentalWavePhase"
                                      )

                                      LazyColumn(
                                          state = lyricsListState,
                                          modifier = Modifier
                                              .fillMaxSize()
                                              .padding(horizontal = 24.dp)
                                              .nestedScroll(lyricsScrollConnection)
                                              .nestedScroll(nestedScrollConnection),
                                          verticalArrangement = Arrangement.spacedBy((22 * lyricsLineSpacing / 1.3f).dp)
                                      ) {
                                          item { Spacer(modifier = Modifier.height(70.dp)) }

                                          items(displayItems.size) { itemIdx ->
                                              val item = displayItems[itemIdx]
                                              val isItemActive = isSynced && itemIdx == activeItemIndex
                                              val distance = if (isSynced && activeItemIndex >= 0) kotlin.math.abs(itemIdx - activeItemIndex) else 0

                                              val targetAlpha = when {
                                                  !isSynced || isItemActive -> 1f
                                                  distance == 1 -> 0.32f
                                                  distance == 2 -> 0.20f
                                                  else -> 0.12f
                                              }

                                              val targetScale = when {
                                                  !isSynced || isItemActive -> 1.04f
                                                  distance == 1 -> 0.94f
                                                  distance == 2 -> 0.88f
                                                  else -> 0.82f
                                              }

                                              val targetBlur = if (!isItemActive && isSynced) {
                                                  when (distance) {
                                                      1 -> 4.dp
                                                      2 -> 8.dp
                                                      else -> 14.dp
                                                  }
                                              } else 0.dp

                                              val animAlpha by animateFloatAsState(targetAlpha, animationSpec = tween(260, easing = FastOutSlowInEasing), label = "itemAlpha")
                                              val animScale by animateFloatAsState(targetScale, animationSpec = tween(320, easing = FastOutSlowInEasing), label = "itemScale")
                                              val animBlur by androidx.compose.animation.core.animateDpAsState(targetBlur, animationSpec = tween(350, easing = FastOutSlowInEasing), label = "itemBlur")

                                              when (item) {
                                                  is Triple<*, *, *> -> {
                                                      // Instrumental Break Row
                                                      val startMs = item.first as Long
                                                      val endMs = item.second as Long
                                                      val seekTarget = item.third as Long
                                                      val breakDuration = (endMs - startMs).coerceAtLeast(1L)
                                                      val breakProgress = if (isItemActive) {
                                                          ((currentPosition + lyricsOffset - startMs).toFloat() / breakDuration).coerceIn(0f, 1f)
                                                      } else 0f

                                                      Row(
                                                          modifier = Modifier
                                                              .fillMaxWidth()
                                                              .graphicsLayer {
                                                                  scaleX = animScale
                                                                  scaleY = animScale
                                                                  alpha = animAlpha
                                                                  transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                                                      when (lyricsTextPosition) {
                                                                          "center" -> 0.5f
                                                                          "right" -> 1f
                                                                          else -> 0f
                                                                      },
                                                                      0.5f
                                                                  )
                                                              }
                                                              .then(if (animBlur > 0.dp) Modifier.blur(animBlur) else Modifier)
                                                              .clickable(
                                                                  enabled = lyricsClickChange,
                                                                  onClick = {
                                                                      onSeek((seekTarget - lyricsOffset).coerceAtLeast(0L))
                                                                      showLyricsControls = true
                                                                      lyricsControlsHideTrigger++
                                                                  }
                                                              )
                                                              .padding(vertical = 10.dp),
                                                          horizontalArrangement = when (lyricsTextPosition) {
                                                              "center" -> Arrangement.Center
                                                              "right" -> Arrangement.End
                                                              else -> Arrangement.Start
                                                          },
                                                          verticalAlignment = Alignment.CenterVertically
                                                      ) {
                                                          Row(
                                                              modifier = Modifier
                                                                  .background(
                                                                      color = contentColor.copy(alpha = if (isItemActive) 0.14f else 0.06f),
                                                                      shape = CircleShape
                                                                  )
                                                                  .padding(horizontal = 16.dp, vertical = 8.dp),
                                                              horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                              verticalAlignment = Alignment.CenterVertically
                                                          ) {
                                                              listOf(0f, 0.33f, 0.66f).forEach { offset ->
                                                                  val pulse = if (isItemActive) {
                                                                      val shifted = (instrumentalWavePhase + offset) % 1f
                                                                      (kotlin.math.sin(shifted * Math.PI * 2).toFloat() * 0.5f + 0.5f)
                                                                  } else 0f
                                                                  Box(
                                                                      modifier = Modifier
                                                                          .size((7 + pulse * 4).dp)
                                                                          .background(
                                                                              color = if (isItemActive) contentColor else contentColor.copy(alpha = 0.5f),
                                                                              shape = CircleShape
                                                                          )
                                                                  )
                                                              }
                                                              Spacer(modifier = Modifier.width(4.dp))
                                                              Text(
                                                                  text = "♪",
                                                                  color = if (isItemActive) contentColor else contentColor.copy(alpha = 0.5f),
                                                                  fontSize = 17.sp,
                                                                  fontWeight = FontWeight.Bold
                                                              )
                                                          }
                                                      }
                                                  }
                                                  is Pair<*, *> -> {
                                                      // Lyrics Line Item
                                                      val lineIdx = item.first as Int
                                                      val line = item.second as com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
                                                      val isPast = isSynced && line.timeMs != -1L && (currentPosition + lyricsOffset) > line.timeMs
                                                      val nextLineTime = currentLyricsLines.getOrNull(lineIdx + 1)?.timeMs

                                                      val lineDuration = remember(line.timeMs, nextLineTime) {
                                                          if (nextLineTime != null && nextLineTime > 0 && line.timeMs > 0) nextLineTime - line.timeMs else 4000L
                                                      }
                                                      val activeDuration = remember(lineDuration) {
                                                          (lineDuration * 0.95).toLong().coerceAtLeast(300L)
                                                      }
                                                      val lineRelTime = if (isItemActive && line.timeMs > 0) {
                                                          ((currentPosition + lyricsOffset) - line.timeMs).coerceAtLeast(0L)
                                                      } else if (isPast) activeDuration else 0L

                                                      // Word / Syllable items for RichSync swipe
                                                      val wordData = remember(line.text, activeDuration) {
                                                          val words = line.text.split(" ").filter { it.isNotEmpty() }
                                                          if (words.isEmpty()) {
                                                              listOf(Triple(line.text, 0L, activeDuration))
                                                          } else {
                                                              val totalChars = line.text.length
                                                              var accumulatedTime = 0L
                                                              words.mapIndexed { wIdx, word ->
                                                                  val charCount = if (wIdx < words.lastIndex) word.length + 1 else word.length
                                                                  val wordStart = accumulatedTime
                                                                  val wordDur = if (totalChars > 0) (activeDuration * charCount.toFloat() / totalChars).toLong() else activeDuration
                                                                  accumulatedTime += wordDur
                                                                  Triple(if (wIdx < words.lastIndex) "$word " else word, wordStart, wordStart + wordDur)
                                                              }
                                                          }
                                                      }

                                                      Column(
                                                          modifier = Modifier
                                                              .fillMaxWidth()
                                                              .graphicsLayer {
                                                                  scaleX = animScale
                                                                  scaleY = animScale
                                                                  alpha = animAlpha
                                                                  transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                                                      when (lyricsTextPosition) {
                                                                          "center" -> 0.5f
                                                                          "right" -> 1f
                                                                          else -> 0f
                                                                      },
                                                                      0.5f
                                                                  )
                                                              }
                                                              .then(if (animBlur > 0.dp) Modifier.blur(animBlur) else Modifier)
                                                              .clickable(
                                                                  enabled = lyricsClickChange,
                                                                  onClick = {
                                                                      if (line.timeMs != -1L) onSeek((line.timeMs - lyricsOffset).coerceAtLeast(0L))
                                                                      showLyricsControls = true
                                                                      lyricsControlsHideTrigger++
                                                                  }
                                                              ),
                                                          horizontalAlignment = when (lyricsTextPosition) {
                                                              "center" -> Alignment.CenterHorizontally
                                                              "right" -> Alignment.End
                                                              else -> Alignment.Start
                                                          }
                                                      ) {
                                                          @OptIn(ExperimentalLayoutApi::class)
                                                          FlowRow(
                                                              modifier = Modifier.fillMaxWidth(),
                                                              horizontalArrangement = when (lyricsTextPosition) {
                                                                  "center" -> Arrangement.Center
                                                                  "right" -> Arrangement.End
                                                                  else -> Arrangement.Start
                                                              },
                                                              verticalArrangement = Arrangement.spacedBy(2.dp)
                                                          ) {
                                                              wordData.forEach { (wordText, startRel, endRel) ->
                                                                  val wordDur = (endRel - startRel).coerceAtLeast(1L)
                                                                  val wordProgress by animateFloatAsState(
                                                                      targetValue = when {
                                                                          lineRelTime >= endRel -> 1f
                                                                          lineRelTime < startRel -> 0f
                                                                          else -> (lineRelTime - startRel).toFloat() / wordDur
                                                                      },
                                                                      animationSpec = tween(
                                                                          durationMillis = wordDur.coerceIn(140L, 260L).toInt(),
                                                                          easing = FastOutSlowInEasing
                                                                      ),
                                                                      label = "wordProgress"
                                                                  )

                                                                  val finalFontWeight = if (isItemActive) FontWeight.Black else FontWeight.Bold

                                                                  Text(
                                                                      text = wordText,
                                                                      fontSize = lyricsTextSize.sp,
                                                                      letterSpacing = (-0.03).sp,
                                                                      style = TextStyle(
                                                                          brush = if (isItemActive) Brush.horizontalGradient(
                                                                              0.0f to Color.White,
                                                                              (wordProgress - 0.04f).coerceAtLeast(0f) to Color.White,
                                                                              (wordProgress + 0.04f).coerceAtMost(1f) to Color.White.copy(alpha = 0.55f),
                                                                              1.0f to Color.White.copy(alpha = 0.55f)
                                                                          ) else null,
                                                                          fontWeight = finalFontWeight,
                                                                          lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                                                          shadow = if (lyricsGlowEffect && isItemActive && wordProgress > 0.05f) Shadow(
                                                                              color = Color.White.copy(alpha = 0.95f * wordProgress),
                                                                              offset = Offset.Zero,
                                                                              blurRadius = (26f * wordProgress).coerceAtLeast(0.1f)
                                                                          ) else null
                                                                      ),
                                                                      color = if (!isItemActive) contentColor.copy(alpha = 0.85f) else Color.Unspecified
                                                                  )
                                                              }
                                                          }

                                                          // Translated / Romanized Subtitle
                                                          val translation = line.translationText
                                                          if (!translation.isNullOrBlank()) {
                                                              Spacer(modifier = Modifier.height(4.dp))
                                                              Text(
                                                                  text = translation,
                                                                  fontSize = (lyricsTextSize * 0.62f).sp,
                                                                  fontWeight = FontWeight.SemiBold,
                                                                  color = contentColor.copy(alpha = if (isItemActive) 0.68f else 0.22f),
                                                                  lineHeight = (lyricsTextSize * 0.78f).sp
                                                              )
                                                          }
                                                      }
                                                  }
                                              }
                                          }

                                          // Provider Badge / Footer
                                          item {
                                              Spacer(modifier = Modifier.height(28.dp))
                                              Row(
                                                  modifier = Modifier
                                                      .fillMaxWidth()
                                                      .padding(bottom = 280.dp),
                                                  horizontalArrangement = when (lyricsTextPosition) {
                                                      "center" -> Arrangement.Center
                                                      "right" -> Arrangement.End
                                                      else -> Arrangement.Start
                                                  }
                                              ) {
                                                  Row(
                                                      modifier = Modifier
                                                          .background(
                                                              color = contentColor.copy(alpha = 0.10f),
                                                              shape = RoundedCornerShape(20.dp)
                                                          )
                                                          .clickable {
                                                              showLyricsOptionsMenu = true
                                                          }
                                                          .padding(horizontal = 14.dp, vertical = 8.dp),
                                                      verticalAlignment = Alignment.CenterVertically,
                                                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                  ) {
                                                      Icon(
                                                          imageVector = Icons.Default.MusicNote,
                                                          contentDescription = null,
                                                          tint = contentColor.copy(alpha = 0.65f),
                                                          modifier = Modifier.size(15.dp)
                                                      )
                                                      Text(
                                                          text = stringResource(
                                                              R.string.lyrics_provided_by,
                                                              currentLyricsProviderName.ifEmpty { "Better Lyrics" }
                                                          ),
                                                          color = contentColor.copy(alpha = 0.65f),
                                                          fontSize = 12.sp,
                                                          fontWeight = FontWeight.Medium
                                                      )
                                                  }
                                              }
                                          }
                                      }

                                      // Floating Resume Auto-Scroll Button
                                      androidx.compose.animation.AnimatedVisibility(
                                          visible = !isAutoScrollEnabled && isSynced,
                                          enter = fadeIn() + slideInVertically { it / 2 },
                                          exit = fadeOut() + slideOutVertically { it / 2 },
                                          modifier = Modifier
                                              .align(Alignment.BottomCenter)
                                              .padding(bottom = 24.dp)
                                      ) {
                                          Row(
                                              modifier = Modifier
                                                  .background(
                                                      color = if (isLightBackground) Color(0xEEFFFFFF) else Color(0xEE1E1E1E),
                                                      shape = RoundedCornerShape(24.dp)
                                                  )
                                                  .border(
                                                      width = 1.dp,
                                                      color = if (isLightBackground) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.15f),
                                                      shape = RoundedCornerShape(24.dp)
                                                  )
                                                  .clickable {
                                                      isAutoScrollEnabled = true
                                                      scrollToCurrentTrigger++
                                                  }
                                                  .padding(horizontal = 16.dp, vertical = 10.dp),
                                              verticalAlignment = Alignment.CenterVertically,
                                              horizontalArrangement = Arrangement.spacedBy(8.dp)
                                          ) {
                                              Icon(
                                                  imageVector = Icons.Default.VerticalAlignBottom,
                                                  contentDescription = stringResource(R.string.lyrics_resume_autoscroll),
                                                  tint = if (isLightBackground) Color.Black else Color.White,
                                                  modifier = Modifier.size(16.dp)
                                              )
                                              Text(
                                                  text = stringResource(R.string.lyrics_resume_autoscroll),
                                                  color = if (isLightBackground) Color.Black else Color.White,
                                                  fontSize = 13.sp,
                                                  fontWeight = FontWeight.SemiBold
                                              )
                                          }
                                      }
                                  } else {
                                      Box(
                                          modifier = Modifier
                                              .fillMaxSize()
                                              .clipToBounds()
                                              .pointerInput(Unit) {
                                                  val swipeLyrics = com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_swipe_to_change_song", "false") == "true"
                                                  if (swipeLyrics) {
                                                      var dragAccumulator = 0f
                                                      detectHorizontalDragGestures(
                                                          onDragStart = { dragAccumulator = 0f },
                                                          onDragEnd = {
                                                              if (dragAccumulator < -100f) {
                                                                  onSkipNext()
                                                              } else if (dragAccumulator > 100f) {
                                                                  onSkipPrevious()
                                                              }
                                                          },
                                                          onHorizontalDrag = { change, dragAmount ->
                                                              change.consume()
                                                              dragAccumulator += dragAmount
                                                          }
                                                      )
                                                  }
                                              }
                                              .clickable(
                                                  interactionSource = remember { MutableInteractionSource() },
                                                  indication = null
                                              ) {
                                                  val lyricsThumbPlayPause = com.mrtdk.liquid_glass.data.LibraryManager.getString("lyrics_thumbnail_play_pause", "false") == "true"
                                                  if (lyricsThumbPlayPause) {
                                                      onTogglePlayPause()
                                                  } else {
                                                      showLyricsControls = !showLyricsControls
                                                      if (showLyricsControls) {
                                                          lyricsControlsHideTrigger++
                                                      }
                                                  }
                                              },
                                          contentAlignment = Alignment.Center
                                      ) {
                                          CircularProgressIndicator(color = contentColor)
                                      }
                                  }
                              }
                          }
                      }
                                        } // Closes Box of overlay

             } // Closes AnimatedVisibility of overlay



             // THE MAIN IMAGE (Capa 3: Portada Principal)

            Box(

                modifier = Modifier

                    .offset(x = imgOffsetX, y = imgOffsetY)

                    .size(width = imgWidth, height = imgHeight)

                    .then(

                        if (showLyrics) {

                            Modifier.clickable(

                                interactionSource = remember { MutableInteractionSource() },

                                indication = null

                            ) {

                                showLyrics = false

                            }

                        } else Modifier

                    )

                    .graphicsLayer {

                        shape = if (!isOverlayActive && dragProgress == 0f) {

                            RoundedCornerShape(

                                topStart = imgCorner.toPx(),

                                topEnd = imgCorner.toPx(),

                                bottomStart = 0f,

                                bottomEnd = 0f

                            )

                        } else {

                            RoundedCornerShape(imgCorner.toPx())

                        }

                        clip = true

                        // Apply offscreen strategy only when player is not fully collapsed

                        compositingStrategy = if (dragProgress < 1f) {

                            CompositingStrategy.Offscreen

                        } else {

                            CompositingStrategy.Auto

                        }

                    }
            ) {

                // Base sharp album cover (always drawn in background during drag or before playback starts)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(hdArtUrl)
                        .crossfade(true)
                        .build(),
                    imageLoader = animatedImageLoader,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                val currentAnimatedUrl = animatedArtworkUrl

                if (!currentAnimatedUrl.isNullOrBlank()) {
                    DisposableEffect(Unit) {
                        onDispose {
                            isVideoPlaying = false
                        }
                    }
                    com.mrtdk.liquid_glass.ui.components.AnimatedArtworkPlayer(
                        videoUrl = currentAnimatedUrl,
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (isVideoPlaying) 1f else 0f },
                        isPaused = false,
                        enableFrameCapture = (dragProgress == 0f && !isOverlayActive),
                        onPlaybackStarted = { isVideoPlaying = true },
                        onFrameCaptured = { frameBitmap ->
                            coverBitmap = frameBitmap.asImageBitmap()
                            reflectionSkew = calculateDominantSkew(frameBitmap)

                            try {
                                // Promedio de la fila inferior de píxeles
                                var r = 0L; var g = 0L; var b = 0L
                                val yCoord = frameBitmap.height - 1
                                val w = frameBitmap.width

                                for (x in 0 until w) {
                                    val pixel = frameBitmap.getPixel(x, yCoord)
                                    r += android.graphics.Color.red(pixel)
                                    g += android.graphics.Color.green(pixel)
                                    b += android.graphics.Color.blue(pixel)
                                }

                                val avgColor = Color((r / w).toInt(), (g / w).toInt(), (b / w).toInt())
                                bottomAverageColor = avgColor
                                dominantColor = avgColor
                                onDominantColorChanged(avgColor)
                            } catch (e: Exception) { }
                        }
                    )
                }

                // Capa de desenfoque blur recortada exclusivamente en la sección bajo la curva elevada (1:1 en ancho y escala con difuminado suave en el borde curvo)
                if (dragProgress < 1f && !isOverlayActive) {
                    val blurRadiusPx = with(density) { 15.dp.toPx() }
                    val maskBitmapCache = remember { arrayOfNulls<androidx.compose.ui.graphics.ImageBitmap>(1) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()

                                val w = size.width
                                val h = size.height
                                val width = w.roundToInt()
                                val height = h.roundToInt()

                                if (width > 0 && height > 0) {
                                    val currentBmp = maskBitmapCache[0]
                                    if (currentBmp == null || currentBmp.width != width || currentBmp.height != height) {
                                        val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                                        val canvas = android.graphics.Canvas(bmp)

                                        val paint = android.graphics.Paint().apply {
                                            isAntiAlias = true
                                            color = android.graphics.Color.BLACK
                                            style = android.graphics.Paint.Style.FILL
                                            maskFilter = android.graphics.BlurMaskFilter(blurRadiusPx, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                        }

                                        val path = android.graphics.Path().apply {
                                            val lY = height - with(density) { 95.dp.toPx() }
                                            val rY = height - with(density) { 115.dp.toPx() }
                                            val mY = height - with(density) { 6.dp.toPx() }
                                            val ext = blurRadiusPx

                                            moveTo(-ext, lY)
                                            lineTo(0f, lY)
                                            cubicTo(
                                                width * 0.28f, mY + with(density) { 4.dp.toPx() },
                                                width * 0.65f, mY + with(density) { 8.dp.toPx() },
                                                width.toFloat(), rY
                                            )
                                            lineTo(width + ext, rY)
                                            lineTo(width + ext, height + ext)
                                            lineTo(-ext, height + ext)
                                            close()
                                        }

                                        canvas.drawPath(path, paint)
                                        maskBitmapCache[0] = bmp.asImageBitmap()
                                    }
                                }

                                maskBitmapCache[0]?.let { bmp ->
                                    drawImage(
                                        image = bmp,
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                            }
                    ) {
                        val currentBitmap = coverBitmap
                        if (currentBitmap != null) {
                            Image(
                                bitmap = currentBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(25.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(hdArtUrl)
                                    .crossfade(true)
                                    .build(),
                                imageLoader = animatedImageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(25.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                            )
                        }
                    }
                }

                // Capa transparente para captura de gestos de deslizamiento hacia abajo (funciona tanto con imagen estática como animada)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(showLyrics, showQueue) {
                            if (!showLyrics && !showQueue) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        val currentOffsetY = dragOffsetY.value
                                        if (currentOffsetY > with(density) { 150.dp.toPx() }) {
                                            scope.launch {
                                                dragOffsetY.animateTo(
                                                    targetValue = maxDragDistance,
                                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                                )
                                                onClose()
                                            }
                                        } else {
                                            scope.launch {
                                                dragOffsetY.animateTo(0f, spring())
                                            }
                                        }
                                    }
                                ) { change, dragAmount ->
                                    if (dragAmount > 0f || dragOffsetY.value > 0f) {
                                        val newOffset = (dragOffsetY.value + dragAmount).coerceAtLeast(0f)
                                        scope.launch { dragOffsetY.snapTo(newOffset) }
                                    }
                                }
                            }
                        }
                )
            }









            // GLOBAL PLAYBACK CONTROLS (Unified bottom controls with fixed height relative to cover image)

             AnimatedVisibility(

                 visible = !showLyrics || showLyricsControls,

                 modifier = Modifier.align(Alignment.BottomCenter),

                 enter = fadeIn(),

                 exit = fadeOut()

             ) {

                 Box(
                      modifier = Modifier
                          .fillMaxWidth()
                          .height(maxHeight - (expandedY + expandedHeight))
                          .graphicsLayer {
                              alpha = contentAlpha
                          }
                          .pointerInput(Unit) {} // Consume all pointer inputs so clicks don't fall through to the lists underneath
                          .background(Color.Transparent)
                  ) {

                     Column(

                         modifier = Modifier

                             .fillMaxSize()

                             .padding(horizontal = 24.dp)

                             .padding(top = 0.dp)

                             .padding(bottom = 0.dp)

                     ) {

                         Spacer(modifier = Modifier.height(72.dp)) 

                         AppleMusicSlider(

                             value = progress, onValueChange = { onSeek((it * duration).toLong()) },

                             modifier = Modifier

                                 .fillMaxWidth()

                                 .height(24.dp)

                                 .onGloballyPositioned { coords ->

                                     sliderCoordinates = coords

                                 },

                             activeColor = contentColor,

                             inactiveColor = contentColor.copy(alpha = 0.3f),

                             barHeightDp = 8.dp

                         )

                         Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                             Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)

                             Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)

                         }

                         

                         Spacer(modifier = Modifier.height(16.dp))

                         

                         Box(

                             modifier = Modifier

                                 .fillMaxWidth()

                                 .weight(1f)

                                 .padding(bottom = 32.dp)

                         ) {

                             PlayerBottomControls(

                                 progress = progress, currentPosition = currentPosition, duration = duration,

                                 isPlaying = isPlaying, contentColor = contentColor, volumePosition = volumePosition,

                                 showLyrics = showLyrics, showQueue = showQueue,

                                 onSeek = onSeek, onTogglePlayPause = onTogglePlayPause, onVolumeChange = { v -> 

                                     volumePosition = v

                                     audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (v * maxVolume).toInt(), 0)

                                     onVolumeChange(v) 

                                 },

                                 onToggleLyrics = { showLyrics = !showLyrics; showQueue = false }, onToggleQueue = { showQueue = !showQueue; showLyrics = false },

                                 includeVolumeAndIcons = true,

                                 includeProgress = false,

                                 onSkipNext = { swipeDirection = 1; onSkipNext() },

                                 onSkipPrevious = { swipeDirection = -1; onSkipPrevious() },

                                 fillHeight = true

                             )

                         }

                     }

                 }

             }



             // UNIFIED SONG DETAILS HEADER (Placed AFTER controls in Z-order so star/3-dots are clickable in main view)

             val isLightBackground = contentColor != Color.White

             Row(
                  modifier = Modifier
                      .offset(x = detailsOffsetX, y = detailsOffsetY)
                      .width(detailsWidth)
                      .heightIn(min = 48.dp)
                      .graphicsLayer {
                          alpha = contentAlpha
                      },
                  verticalAlignment = Alignment.CenterVertically
              ) {

                 var dragAccumulator by remember { mutableStateOf(0f) }

                 Box(

                     modifier = Modifier

                         .weight(1f)

                         .pointerInput(playerState) {

                             detectHorizontalDragGestures(

                                 onDragEnd = {

                                     if (dragAccumulator < -60f) { swipeDirection = 1; onSkipNext() }

                                     else if (dragAccumulator > 60f) { swipeDirection = -1; onSkipPrevious() }

                                     dragAccumulator = 0f

                                 },

                                 onHorizontalDrag = { change, dragAmount ->

                                     dragAccumulator += dragAmount

                                     change.consume()

                                 }

                             )

                         }

                 ) {

                     val dir = swipeDirection

                     androidx.compose.animation.AnimatedContent(

                         targetState = playerState,

                         transitionSpec = {

                             (androidx.compose.animation.slideInHorizontally { width -> dir * width } + fadeIn()).togetherWith(

                                 androidx.compose.animation.slideOutHorizontally { width -> dir * -width } + fadeOut()

                             )

                         }, label = "textSlide"

                     ) { state ->

                         Column(

                             modifier = Modifier

                                 .fillMaxWidth()

                         ) {

                             Text(

                                 text = state?.title ?: "",

                                 color = contentColor,

                                 fontSize = titleFontSize,

                                 fontWeight = FontWeight.Bold,

                                 maxLines = 1,

                                 overflow = TextOverflow.Ellipsis

                             )

                             Spacer(modifier = Modifier.height(2.dp))

                             var artistCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                             Text(
                                 text = state?.artist ?: "",
                                 color = contentColor.copy(alpha = 0.7f),
                                 fontSize = artistFontSize,
                                 maxLines = 1,
                                 overflow = TextOverflow.Ellipsis,
                                 modifier = Modifier
                                     .onGloballyPositioned { artistCoords = it }
                                     .clickable {
                                         if (state?.artist != null) {
                                             val artistList = state.artist.split(", ").filter { it.isNotEmpty() }
                                             if (artistList.isNotEmpty()) {
                                                 val parentCoords = parentCoordinates
                                                 if (parentCoords != null && artistCoords != null && parentCoords.isAttached && artistCoords!!.isAttached) {
                                                     val localOffset = parentCoords.localPositionOf(artistCoords!!, Offset.Zero)
                                                     val size = artistCoords!!.size
                                                     artistPivotBounds = androidx.compose.ui.geometry.Rect(localOffset, androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat()))
                                                 } else {
                                                     artistPivotBounds = artistCoords?.boundsInRoot()
                                                 }
                                                 artistMenuOptions = artistList
                                                 showArtistOptionsMenu = true
                                             }
                                         }
                                     }
                             )

                         }

                     }

                 }

                 Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {

                     Box(

                         modifier = Modifier

                             .size(36.dp)

                             .clip(CircleShape)

                             .background(

                                 if (isSaved) contentColor else contentColor.copy(alpha = 0.15f)

                             )

                             .clickable {

                                 if (playerState != null) {

                                     if (!isSaved) {

                                         LibraryManager.saveItem(LibraryItem(playerState.videoId ?: "", playerState.title, playerState.artist, playerState.artUrl?.toString(), ItemType.SONG))

                                     } else {

                                         LibraryManager.removeItem(playerState.videoId ?: "")

                                     }

                                 }

                             },

                         contentAlignment = Alignment.Center

                     ) {

                         AsyncImage(

                             model = "file:///android_asset/img reproductor/c.png",

                             contentDescription = "Fav",

                             colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(

                                 if (isSaved) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor

                             ),

                             modifier = Modifier.size(starIconSize)

                         )

                     }

                                                         var threeDotsCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                      Box(
                          modifier = Modifier
                              .onGloballyPositioned { threeDotsCoords = it }
                              .size(32.dp).clip(CircleShape).background(contentColor.copy(alpha = 0.15f)).clickable { 
                                  val parentCoords = parentCoordinates
                                  if (parentCoords != null && threeDotsCoords != null && parentCoords.isAttached && threeDotsCoords!!.isAttached) {
                                      val localOffset = parentCoords.localPositionOf(threeDotsCoords!!, Offset.Zero)
                                      val size = threeDotsCoords!!.size
                                      menuPivotBounds = androidx.compose.ui.geometry.Rect(localOffset, androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat()))
                                  } else {
                                      menuPivotBounds = threeDotsCoords?.boundsInRoot()
                                  }
                                  if (showLyrics) {
                                      showLyricsOptionsMenu = true
                                  } else {
                                      showOptionsMenu = true
                                  }
                              },

                         contentAlignment = Alignment.Center

                     ) {

                         androidx.compose.foundation.Canvas(modifier = Modifier.size(moreIconSize)) {

                             val r = 1.5.dp.toPx()

                             val space = 4.dp.toPx()

                             val cx = size.width / 2f

                             val cy = size.height / 2f

                             drawCircle(contentColor, radius = r, center = Offset(cx - space - r * 2, cy))

                             drawCircle(contentColor, radius = r, center = Offset(cx, cy))

                             drawCircle(contentColor, radius = r, center = Offset(cx + space + r * 2, cy))

                         }

                     }

                 }

             }

         } // end inner Box

        } // end BoxWithConstraints







        if (showLyricsMenu) {

            ModalBottomSheet(onDismissRequest = { showLyricsMenu = false }, containerColor = Color(0xFF1E1E1E)) {

                Column(modifier = Modifier.padding(minOf(16.dp, 24.dp))) {

                    Text(text = "Proveedor de letras", color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold)

                    Spacer(modifier=Modifier.height(16.dp))

                    

                    Row(modifier=Modifier.fillMaxWidth().clickable { showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {

                        Text("LRCLIB (Activo)", color=Color.White, fontSize=16.sp)

                    }

                    Row(modifier=Modifier.fillMaxWidth().clickable { showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {

                        Text("KuGou (Próximamente)", color=Color.Gray, fontSize=16.sp)

                    }

                    Row(modifier=Modifier.fillMaxWidth().clickable { showLyricsMenu=false }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {

                        Text("Musixmatch (Próximamente)", color=Color.Gray, fontSize=16.sp)

                    }

                    

                    Spacer(modifier=Modifier.height(16.dp))

                    androidx.compose.material3.Divider(color = Color.DarkGray)

                    Spacer(modifier=Modifier.height(16.dp))

                    

                    Row(modifier=Modifier.fillMaxWidth().clickable { 

                        showLyricsMenu=false 

                        try {

                            val intent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH).apply {

                                putExtra(android.app.SearchManager.QUERY, "${playerState?.artist ?: ""} ${playerState?.title ?: ""} lyrics")

                            }

                            context.startActivity(intent)

                        } catch (e: Exception) {}

                    }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {

                        Icon(Icons.Default.Search, contentDescription=null, tint=Color.White)

                        Spacer(modifier=Modifier.width(16.dp))

                        Text(stringResource(R.string.buscar_letra_internet), color=Color.White, fontSize=16.sp)

                    }

                    Spacer(modifier=Modifier.height(32.dp))

                }

            }

        }

                if (showLyricsOffsetDialog) {
            var tempOffset by remember { mutableStateOf(lyricsOffset) }
            var textFieldVal by remember { mutableStateOf(tempOffset.toString()) }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showLyricsOffsetDialog = false },
                containerColor = Color(0xFF1E1E1E),
                title = { Text(stringResource(R.string.lyrics_offset_title), color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = textFieldVal,
                                onValueChange = { newText ->
                                    val sanitized = newText.filter { it.isDigit() || (it == '-' && newText.indexOf('-') == 0) }
                                    val limited = if (sanitized.startsWith('-')) sanitized.take(6) else sanitized.take(5)
                                    textFieldVal = limited
                                    if (limited.isNotEmpty() && limited != "-") {
                                        limited.toIntOrNull()?.let { parsed ->
                                            tempOffset = parsed.coerceIn(-9999, 9999)
                                        }
                                    } else if (limited.isEmpty()) {
                                        tempOffset = 0
                                    }
                                },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                ),
                                modifier = Modifier.width(120.dp),
                                colors = androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color(0xFFFA243C),
                                    unfocusedIndicatorColor = Color.Gray
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "ms", color = Color.Gray, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            
                            if (tempOffset != 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    tempOffset = 0
                                    textFieldVal = "0"
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.lyrics_offset_reset), tint = Color(0xFFFA243C))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = {
                                tempOffset = (tempOffset - 50).coerceIn(-3000, 3000)
                                textFieldVal = tempOffset.toString()
                            }) {
                                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.lyrics_offset_decrease), tint = Color.White)
                            }
                            
                            androidx.compose.material3.Slider(
                                value = tempOffset.toFloat().coerceIn(-3000f, 3000f),
                                onValueChange = { newValue ->
                                    val rounded = (newValue / 100f).roundToInt() * 100
                                    tempOffset = rounded
                                    textFieldVal = rounded.toString()
                                },
                                valueRange = -3000f..3000f,
                                steps = 59,
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = Color(0xFFFA243C),
                                    activeTrackColor = Color(0xFFFA243C),
                                    inactiveTrackColor = Color.DarkGray
                                )
                            )
                            
                            IconButton(onClick = {
                                tempOffset = (tempOffset + 50).coerceIn(-3000, 3000)
                                textFieldVal = tempOffset.toString()
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.lyrics_offset_increase), tint = Color.White)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        ) {
                            Text(text = "-3000ms", color = Color.Gray, fontSize = 12.sp)
                            Text(text = "+3000ms", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        lyricsOffset = tempOffset
                        showLyricsOffsetDialog = false
                        if (playerState?.videoId != null) {
                            com.mrtdk.liquid_glass.data.LibraryManager.saveString("lyrics_offset_${playerState.videoId}", tempOffset.toString())
                        }
                    }) {
                        Text(stringResource(R.string.lyrics_offset_accept), color = Color(0xFFFA243C))
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showLyricsOffsetDialog = false }) {
                        Text(stringResource(R.string.cancelar), color = Color.Gray)
                    }
                }
            )
        }

        if (showLyricsEditDialog) {
            val initialText = remember(lyricsLines) {
                lyricsLines?.joinToString("\n") { line ->
                    if (line.timeMs >= 0L) {
                        val min = line.timeMs / 1000 / 60
                        val sec = (line.timeMs / 1000) % 60
                        val ms = line.timeMs % 1000
                        String.format("[%02d:%02d.%02d] %s", min, sec, ms / 10, line.text)
                    } else {
                        line.text
                    }
                }.orEmpty()
            }
            var tempLyricsText by remember { mutableStateOf(initialText) }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showLyricsEditDialog = false },
                containerColor = Color(0xFF1E1E1E),
                title = { Text("Editar Letras", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.OutlinedTextField(
                            value = tempLyricsText,
                            onValueChange = { tempLyricsText = it },
                            label = { Text("Letras (Formato LRC o Texto plano)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp),
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color(0xFFFA243C),
                                unfocusedIndicatorColor = Color.Gray
                            )
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showLyricsEditDialog = false
                        if (playerState?.videoId != null) {
                            com.mrtdk.liquid_glass.data.LibraryManager.saveString("custom_lyrics_${playerState.videoId}", tempLyricsText)
                            lyricsReloadTrigger++
                            Toast.makeText(context, "Letras guardadas", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Guardar", color = Color(0xFFFA243C))
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showLyricsEditDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }

        

        val coroutineScope = rememberCoroutineScope()

        if (showManualLyricsSearch) {

            androidx.compose.material3.AlertDialog(

                onDismissRequest = { showManualLyricsSearch = false },

                containerColor = Color(0xFF1E1E1E),

                title = { Text(stringResource(R.string.buscar_letra), color = Color.White) },

                text = {

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        androidx.compose.material3.OutlinedTextField(

                            value = manualLyricsQueryTitle,

                            onValueChange = { manualLyricsQueryTitle = it },

                            label = { Text(stringResource(R.string.titulo_cancion), color = Color.Gray) },

                            singleLine = true,

                            colors = androidx.compose.material3.TextFieldDefaults.colors(

                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,

                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent

                            )

                        )

                        androidx.compose.material3.OutlinedTextField(

                            value = manualLyricsQueryArtist,

                            onValueChange = { manualLyricsQueryArtist = it },

                            label = { Text(stringResource(R.string.artista), color = Color.Gray) },

                            singleLine = true,

                            colors = androidx.compose.material3.TextFieldDefaults.colors(

                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,

                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent

                            )

                        )

                    }

                },

                confirmButton = {

                    androidx.compose.material3.TextButton(onClick = {

                        showManualLyricsSearch = false

                        lyricsLines = null // reset so user sees loading

                        val targetTitle = manualLyricsQueryTitle
                        val targetArtist = manualLyricsQueryArtist
                        val durSec = (duration / 1000).toInt()
                        val videoId = playerState?.videoId ?: ""

                        coroutineScope.launch {
                            val result = when (selectedLyricsProvider) {
                                "BetterLyrics" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchBetterLyrics(targetTitle, targetArtist, durSec)
                                "Unison" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchUnisonLyrics(videoId, targetTitle, targetArtist, durSec, playerState?.album)
                                "BiniLyrics", "LyricsPlus" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchBiniLyrics(targetTitle, targetArtist, durSec)
                                "LRCLIB" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchLRCLib(targetTitle, targetArtist, durSec)
                                "KuGou" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchKuGouLyrics(targetTitle, targetArtist)
                                "YouTube Captions", "YouTube Subtitle" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchYouTubeCaptions(videoId)
                                "YouTube Music" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchYouTubeLyrics(videoId)
                                "SimpMusic" -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchSimpMusicLyrics(targetTitle, targetArtist)
                                else -> com.mrtdk.liquid_glass.utils.LyricsProvider.fetchAutoLyrics(videoId, targetTitle, targetArtist, durSec, playerState?.album)
                            }
                            val lines = result?.lyrics
                            if (lines != null) {
                                val formatted = lines.lines.joinToString("\n") { line ->
                                    if (line.timeMs >= 0L) {
                                        val min = line.timeMs / 1000 / 60
                                        val sec = (line.timeMs / 1000) % 60
                                        val ms = line.timeMs % 1000
                                        String.format("[%02d:%02d.%02d] %s", min, sec, ms / 10, line.text)
                                    } else {
                                        line.text
                                    }
                                }
                                com.mrtdk.liquid_glass.data.LibraryManager.saveString("custom_lyrics_${playerState?.videoId}", formatted)
                                lyricsReloadTrigger++
                            }
                        }

                    }) {

                        Text(stringResource(R.string.search_action), color = Color(0xFFFA243C))

                    }

                },

                dismissButton = {

                    androidx.compose.material3.TextButton(onClick = { showManualLyricsSearch = false }) {

                        Text(stringResource(R.string.cancelar), color = Color.Gray)

                    }

                }

            )

        }

        

        if (showPlaylistMenu) {

            ModalBottomSheet(onDismissRequest = { showPlaylistMenu = false }, containerColor = Color(0xFF1E1E1E)) {

                val playlists by com.mrtdk.liquid_glass.data.LibraryManager.playlists.collectAsState()

                Column(modifier = Modifier.padding(horizontal=16.dp, vertical=8.dp).fillMaxWidth()) {

                    Text(stringResource(R.string.anadir_a_playlist), color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold)

                    Spacer(modifier=Modifier.height(16.dp))

                    Row(modifier=Modifier.fillMaxWidth().clickable { showPlaylistMenu=false; showNewPlaylistDialog=true }.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {

                        Box(modifier=Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray), contentAlignment=Alignment.Center) {

                            Icon(Icons.Default.Add, null, tint=Color.White)

                        }

                        Spacer(modifier=Modifier.width(16.dp))

                        Text(stringResource(R.string.nueva_playlist_ellipsis), color=Color(0xFFFA243C), fontSize=16.sp)

                    }

                    

                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max=300.dp)) {

                        items(playlists.size) { i ->

                            val pl = playlists[i]

                            Row(modifier=Modifier.fillMaxWidth().clickable { 

                                com.mrtdk.liquid_glass.data.LibraryManager.addSongToPlaylist(pl.id, com.mrtdk.liquid_glass.data.LibraryItem(playerState?.videoId?:"", playerState?.title?:"", playerState?.artist?:"", playerState?.artUrl?.toString(), com.mrtdk.liquid_glass.data.ItemType.SONG))

                                showPlaylistMenu=false 

                            }.padding(vertical=8.dp), verticalAlignment=Alignment.CenterVertically) {

                                Box(modifier=Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {

                                    if (pl.items.isNotEmpty() && pl.items.first().thumbnail != null) {

                                        AsyncImage(model=pl.items.first().thumbnail, contentDescription=null, modifier=Modifier.fillMaxSize(), contentScale=ContentScale.Crop)

                                    }

                                }

                                Spacer(modifier=Modifier.width(16.dp))

                                Column {

                                    Text(pl.name, color=Color.White, fontSize=16.sp)

                                    Text(stringResource(R.string.num_canciones, pl.items.size), color=Color.Gray, fontSize=14.sp)

                                }

                            }

                        }

                    }

                    Spacer(modifier=Modifier.height(32.dp))

                }

            }

        }

        

        if (showNewPlaylistDialog) {

            var newPlaylistName by remember { mutableStateOf("") }

            AlertDialog(

                onDismissRequest = { showNewPlaylistDialog = false },

                title = { Text(stringResource(R.string.nueva_playlist), color = Color.White) },

                text = {

                    OutlinedTextField(

                        value = newPlaylistName,

                        onValueChange = { newPlaylistName = it },

                        label = { Text(stringResource(R.string.nombre)) },

                        singleLine = true,

                        colors = OutlinedTextFieldDefaults.colors(

                            focusedTextColor=Color.White, 

                            unfocusedTextColor=Color.White,

                            focusedBorderColor = Color(0xFFFA243C),

                            focusedLabelColor = Color(0xFFFA243C)

                        )

                    )

                },

                confirmButton = {

                    TextButton(onClick = {

                        if(newPlaylistName.isNotBlank()){

                            com.mrtdk.liquid_glass.data.LibraryManager.createPlaylist(newPlaylistName)

                        }

                        showNewPlaylistDialog = false

                    }) { Text(stringResource(R.string.crear), color = Color(0xFFFA243C)) }

                },

                dismissButton = {

                    TextButton(onClick = { showNewPlaylistDialog = false }) { Text(stringResource(R.string.cancelar), color = Color.Gray) }

                },

                containerColor = Color(0xFF2C2C2C)
            )
        }
    },
    glassContent = {
        val glassScope = this
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 115.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sincronizar letra pill (if user scrolled away)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showLyrics && !isAutoScrollEnabled,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
                ) {
                    val dominantColor by LibraryManager.currentDominantColor.collectAsState()
                    glassScope.GlassBox(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, dominantColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .clickable {
                                isAutoScrollEnabled = true
                                scrollToCurrentTrigger++
                            }
                            .height(38.dp)
                            .wrapContentWidth(),
                        blur = 0.8f,
                        scale = 0.02f,
                        centerDistortion = 0.1f,
                        warpEdges = 0.4f,
                        elevation = 6.dp,
                        shape = RoundedCornerShape(50),
                        tint = dominantColor.copy(alpha = 0.35f),
                        darkness = 0.2f
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.sync_lyrics),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = stringResource(R.string.sync_lyrics),
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

            }
        }
        if (showLyricsOptionsMenu) {
            val lyricsMenuScope = rememberCoroutineScope()
            glassScope.LyricsOptionsMenu(
                backdrop = localBackdrop,
                onDismiss = { showLyricsOptionsMenu = false },
                playerState = playerState,
                selectedProvider = selectedLyricsProvider,
                onSelectProvider = { selectedLyricsProvider = it },
                availableProviders = availableLyricsProviders,
                currentProviderIndex = currentLyricsProviderIndex,
                onSelectProviderIndex = { newIndex ->
                    if (newIndex in availableLyricsProviders.indices) {
                        currentLyricsProviderIndex = newIndex
                        val activeResult = availableLyricsProviders[newIndex]
                        selectedLyricsProvider = activeResult.providerName
                        currentLyricsProviderName = activeResult.providerName
                        currentLyricsSyncType = activeResult.syncType
                        val lines = activeResult.lyrics
                        lyricsLines = lines?.lines
                        if (isRomajiEnabled && lines != null) {
                            lyricsMenuScope.launch {
                                val prefs = com.mrtdk.liquid_glass.utils.LyricsRomanizationPreferences(true, true, true, true, true)
                                val processed = com.mrtdk.liquid_glass.utils.LyricsUtils.romanizeSyncedLyrics(lines, prefs)
                                lyricsLines = processed.lines
                            }
                        }
                    }
                },
                isRomajiEnabled = isRomajiEnabled,
                onToggleRomaji = {
                    isRomajiEnabled = !isRomajiEnabled
                    if (playerState?.videoId != null) {
                        com.mrtdk.liquid_glass.data.LibraryManager.saveString("romanize_lyrics_${playerState.videoId}", isRomajiEnabled.toString())
                    }
                },
                lyricsOffset = lyricsOffset,
                onAdjustOffset = { showLyricsOffsetDialog = true },
                onAdjustOffsetDelta = { deltaSec ->
                    lyricsOffset += (deltaSec * 1000).toInt()
                    if (playerState?.videoId != null) {
                        com.mrtdk.liquid_glass.data.LibraryManager.saveString("lyrics_offset_${playerState.videoId}", lyricsOffset.toString())
                    }
                },
                onResetOffset = {
                    lyricsOffset = 0
                    if (playerState?.videoId != null) {
                        com.mrtdk.liquid_glass.data.LibraryManager.saveString("lyrics_offset_${playerState.videoId}", "0")
                    }
                },
                onEditLyrics = { showLyricsEditDialog = true },
                onReloadLyrics = {
                    if (playerState?.videoId != null) {
                        com.mrtdk.liquid_glass.data.LibraryManager.saveString("custom_lyrics_${playerState.videoId}", null)
                        com.mrtdk.liquid_glass.data.LibraryManager.saveString("lyrics_offset_${playerState.videoId}", "0")
                    }
                    lyricsOffset = 0
                    lyricsReloadTrigger++
                    Toast.makeText(context, context.getString(R.string.lyrics_restored_toast), Toast.LENGTH_SHORT).show()
                },
                onSearchManually = { showManualLyricsSearch = true },
                onSearchOnline = {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(android.app.SearchManager.QUERY, (playerState?.artist ?: "") + " " + (playerState?.title ?: "") + " lyrics")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                },
                pivotBounds = menuPivotBounds
            )
        }
        if (showOptionsMenu) {
            PlayerOptionsMenu(
                backdrop = localBackdrop,
                onDismiss = { showOptionsMenu = false },
                playerState = playerState,
                isSaved = isSaved,
                onToggleSaved = {
                    if (playerState != null) {
                        if (!isSaved) {
                            LibraryManager.saveItem(LibraryItem(playerState.videoId ?: "", playerState.title, playerState.artist, playerState.artUrl?.toString(), ItemType.SONG))
                        } else {
                            LibraryManager.removeItem(playerState.videoId ?: "")
                        }
                    }
                },
                onDownload = {
                    if (playerState?.videoId != null) {
                        downloadSong(context, playerState.videoId, playerState.title, playerState.artist, playerState.artUrl?.toString(), playerState.album)
                    }
                },
                onAddToPlaylist = {
                    showPlaylistMenu = true
                },
                onSongSelected = { targetState ->
                    onSongSelected(targetState)
                },
                onAlbumSelected = { album ->
                    showOptionsMenu = false
                    onAlbumSelected(album)
                },
                pivotBounds = menuPivotBounds
            )
        }
        if (showArtistOptionsMenu) {
            ArtistOptionsMenu(
                backdrop = localBackdrop,
                artists = artistMenuOptions,
                onDismiss = { showArtistOptionsMenu = false },
                onArtistSelected = { artistName ->
                    onArtistSelected(com.mrtdk.liquid_glass.ui.screens.ArtistState(
                        id = artistName,
                        name = artistName,
                        thumbnail = null
                    ))
                },
                pivotBounds = artistPivotBounds
            )
        }

        if (AudioRoutingState.showAudioRoutingMenu) {

            AudioRoutingMenu(

                backdrop = localBackdrop,

                onDismiss = { AudioRoutingState.showAudioRoutingMenu = false },

                playerState = playerState,

                volumePosition = volumePosition,

                onVolumeChange = { v ->

                    volumePosition = v

                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (v * maxVolume).toInt(), 0)

                    onVolumeChange(v)

                }

            )

        }

    }

)

}

}

@Composable

fun PlayerBottomControls(

    progress: Float, currentPosition: Long, duration: Long,

    isPlaying: Boolean, contentColor: Color, volumePosition: Float,

    showLyrics: Boolean, showQueue: Boolean,

    onSeek: (Long) -> Unit, onTogglePlayPause: () -> Unit, onVolumeChange: (Float) -> Unit,

    onToggleLyrics: () -> Unit, onToggleQueue: () -> Unit,

    includeVolumeAndIcons: Boolean = true,

    includeProgress: Boolean = true,

    onSkipNext: () -> Unit = {},

    onSkipPrevious: () -> Unit = {},

    fillHeight: Boolean = false

) {

    val isLightBackground = contentColor != Color.White

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)) {

        if (includeProgress) {

            AppleMusicSlider(

                value = progress, onValueChange = { onSeek((it * duration).toLong()) },

                modifier = Modifier.fillMaxWidth().height(24.dp),

                activeColor = contentColor,

                inactiveColor = contentColor.copy(alpha = 0.3f),

                barHeightDp = 8.dp

            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)

                Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)

            }

            Spacer(modifier = Modifier.height(30.dp))

        }

        

        if (fillHeight) {

            Spacer(modifier = Modifier.weight(0.7f))

        }

        

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {

            AnimatedSkipButton(

                iconId = R.drawable.previous,

                contentDescription = "Previous",

                contentColor = contentColor,

                sizeDp = 84.dp,

                iconSizeDp = 64.dp,

                onClick = onSkipPrevious

            )

            Spacer(modifier = Modifier.width(16.dp))

            val playPauseInteractionSource = remember { MutableInteractionSource() }

            val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()



            val playPauseBgColor by animateColorAsState(

                targetValue = if (isPlayPausePressed) contentColor.copy(alpha = 0.12f) else Color.Transparent,

                label = "playPauseBg"

            )



            val playPauseRotation by animateFloatAsState(

                targetValue = if (isPlaying) 180f else 0f,

                animationSpec = spring(

                    dampingRatio = Spring.DampingRatioMediumBouncy,

                    stiffness = Spring.StiffnessLow

                ),

                label = "playPauseButtonRotation"

            )



            Box(

                modifier = Modifier

                    .size(84.dp)

                    .clip(CircleShape)

                    .background(playPauseBgColor)

                    .clickable(

                        interactionSource = playPauseInteractionSource,

                        indication = androidx.compose.foundation.LocalIndication.current,

                        onClick = onTogglePlayPause

                    ),

                contentAlignment = Alignment.Center

            ) {

                AnimatedContent(

                    targetState = isPlaying,

                    transitionSpec = {

                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)))

                            .togetherWith(fadeOut(animationSpec = tween(90)) + scaleOut(targetScale = 0.3f, animationSpec = tween(90)))

                    },

                    label = "playPauseIcon"

                ) { playing ->

                    Icon(

                        painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.resume),

                        contentDescription = if (playing) "Pause" else "Play",

                        tint = contentColor,

                        modifier = Modifier

                            .size(64.dp)

                            .graphicsLayer {

                                rotationZ = playPauseRotation

                            }

                    )

                }

            }

            Spacer(modifier = Modifier.width(16.dp))

            AnimatedSkipButton(

                iconId = R.drawable.forward,

                contentDescription = "Next",

                contentColor = contentColor,

                sizeDp = 84.dp,

                iconSizeDp = 64.dp,

                onClick = onSkipNext

            )

        }

        

        if (fillHeight) {

            Spacer(modifier = Modifier.weight(1.3f))

        }

        

        if (includeVolumeAndIcons) {

            if (!fillHeight) {

                Spacer(modifier = Modifier.height(70.dp))

            }

            val deviceLabel = AudioRoutingState.connectedDeviceName ?: stringResource(R.string.celular_speaker)

            Text(

                text = deviceLabel,

                color = contentColor.copy(alpha = 0.5f),

                fontSize = 12.sp,

                fontWeight = FontWeight.Bold,

                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)

            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                Icon(

                    painter = painterResource(id = R.drawable.albumspeaker),

                    contentDescription = "Low volume",

                    tint = contentColor.copy(alpha = 0.7f),

                    modifier = Modifier.size(16.dp)

                )

                Spacer(modifier = Modifier.width(20.dp))

                AppleMusicSlider(

                    value = volumePosition, onValueChange = { onVolumeChange(it) },

                    modifier = Modifier.weight(1f).height(24.dp),

                    activeColor = contentColor,

                    inactiveColor = contentColor.copy(alpha = 0.2f),

                    barHeightDp = 8.dp

                )

                Spacer(modifier = Modifier.width(20.dp))

                Icon(

                    painter = painterResource(id = R.drawable.albumspeakerlarge),

                    contentDescription = "High volume",

                    tint = contentColor.copy(alpha = 0.7f),

                    modifier = Modifier.size(24.dp)

                )

            }

            if (!fillHeight) {

                Spacer(modifier = Modifier.height(32.dp))

            } else {

                Spacer(modifier = Modifier.height(24.dp))

            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                Box(

                    modifier = Modifier

                        .size(40.dp)

                        .clip(CircleShape)

                        .background(if (showLyrics) contentColor else Color.Transparent)

                        .clickable { onToggleLyrics() },

                    contentAlignment = Alignment.Center

                ) {

                    AsyncImage(

                        model = "file:///android_asset/img reproductor/Letras.png",

                        contentDescription = "Lyrics",

                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(

                            if (showLyrics) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor

                        ),

                        modifier = Modifier.size(20.dp)

                    )

                }

                AsyncImage(

                    model = "file:///android_asset/img reproductor/parlante.png",

                    contentDescription = "Format",

                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor),

                    modifier = Modifier

                        .height(20.dp)

                        .padding(horizontal = 8.dp)

                        .clickable(

                            interactionSource = remember { MutableInteractionSource() },

                            indication = null

                        ) { AudioRoutingState.showAudioRoutingMenu = true }

                )

                Box(

                    modifier = Modifier

                        .size(40.dp)

                        .clip(CircleShape)

                        .background(if (showQueue) contentColor else Color.Transparent)

                        .clickable { onToggleQueue() },

                    contentAlignment = Alignment.Center

                ) {

                    Icon(

                        painter = painterResource(id = R.drawable.nextinfo),

                        contentDescription = "Next Info",

                        tint = if (showQueue) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor,

                        modifier = Modifier.size(24.dp)

                    )

                }

            }

            if (fillHeight) {

                Spacer(modifier = Modifier.height(16.dp))

            }

        }

    }

}



fun formatDuration(durationMs: Long): String {

    val seconds = (durationMs / 1000) % 60

    val minutes = (durationMs / (1000 * 60)) % 60

    return "%d:%02d".format(minutes, seconds)

}



@Composable

fun AnimatedSkipButton(

    iconId: Int,

    contentDescription: String,

    contentColor: Color,

    sizeDp: androidx.compose.ui.unit.Dp = 56.dp,

    iconSizeDp: androidx.compose.ui.unit.Dp = 52.dp,

    onClick: () -> Unit

) {

    var isPressed by remember { mutableStateOf(false) }

    val scale by androidx.compose.animation.core.animateFloatAsState(if (isPressed) 0.85f else 1f, label="")

    val bgAlpha by androidx.compose.animation.core.animateFloatAsState(if (isPressed) 0.15f else 0f, label="")

    

    Box(

        modifier = Modifier

            .size(sizeDp)

            .clip(CircleShape)

            .background(contentColor.copy(alpha = bgAlpha))

            .clickable(

                interactionSource = remember { MutableInteractionSource() },

                indication = null,

                onClick = onClick

            )

            .pointerInput(Unit) {

                awaitPointerEventScope {

                    while (true) {

                        val down = awaitFirstDown()

                        isPressed = true

                        waitForUpOrCancellation()

                        isPressed = false

                    }

                }

            },

        contentAlignment = Alignment.Center

    ) {

        Icon(

            painter = painterResource(id = iconId),

            contentDescription = contentDescription,

            tint = contentColor,

            modifier = Modifier

                .size(iconSizeDp)

                .graphicsLayer { scaleX = scale; scaleY = scale }

        )

    }

}



@Composable

fun AppleMusicSlider(

    value: Float,

    onValueChange: (Float) -> Unit,

    modifier: Modifier = Modifier,

    activeColor: Color = Color.White,

    inactiveColor: Color = Color.White.copy(alpha = 0.3f),

    barHeightDp: androidx.compose.ui.unit.Dp = 8.dp

) {

    var isDragging by remember { mutableStateOf(false) }

    val scale by androidx.compose.animation.core.animateFloatAsState(

        targetValue = if (isDragging) 1.5f else 1f,

        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150, easing = androidx.compose.animation.core.FastOutSlowInEasing),

        label = "slider_scale"

    )

    var sliderWidth by remember { mutableFloatStateOf(0f) }



    Box(

        modifier = modifier

            .graphicsLayer {

                scaleY = scale

            }

            .pointerInput(Unit) {

                awaitPointerEventScope {

                    while (true) {

                        val down = awaitFirstDown()

                        isDragging = true

                        if (sliderWidth > 0) {

                            onValueChange((down.position.x / sliderWidth).coerceIn(0f, 1f))

                        }

                        down.consume()

                        

                        while (true) {

                            val event = awaitPointerEvent()

                            val dragEvent = event.changes.firstOrNull()

                            if (dragEvent != null && dragEvent.pressed) {

                                if (sliderWidth > 0) {

                                    onValueChange((dragEvent.position.x / sliderWidth).coerceIn(0f, 1f))

                                }

                                dragEvent.consume()

                            } else {

                                break

                            }

                        }

                        isDragging = false

                    }

                }

            }

            .onSizeChanged { sliderWidth = it.width.toFloat() }

    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            val height = size.height

            val width = size.width

            val barHeight = barHeightDp.toPx()

            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2, barHeight / 2)

            val centerY = height / 2 - barHeight / 2



            // Inactive track

            drawRoundRect(

                color = inactiveColor,

                topLeft = Offset(0f, centerY),

                size = Size(width, barHeight),

                cornerRadius = cornerRadius

            )



            // Active track

            drawRoundRect(

                color = activeColor,

                topLeft = Offset(0f, centerY),

                size = Size(width * value, barHeight),

                cornerRadius = cornerRadius

            )

        }

    }

}



fun downloadSong(context: android.content.Context, videoId: String, title: String, artist: String, artUrl: String?, album: String? = null, silent: Boolean = false) {

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {

        if (androidx.core.content.ContextCompat.checkSelfPermission(

                context,

                android.Manifest.permission.POST_NOTIFICATIONS

            ) != android.content.pm.PackageManager.PERMISSION_GRANTED

        ) {

            if (context is android.app.Activity) {

                androidx.core.app.ActivityCompat.requestPermissions(

                    context,

                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),

                    101

                )

            }

        }

    }



    try {

        val metaString = "$title||$artist||${artUrl ?: ""}||${album ?: ""}"

        val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest.Builder(videoId, android.net.Uri.parse("yt://$videoId"))

            .setCustomCacheKey(videoId)

            .setData(metaString.toByteArray(Charsets.UTF_8))

            .build()

        

        androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(

            context,

            com.mrtdk.liquid_glass.playback.ExoDownloadService::class.java,

            downloadRequest,

            false

        )

        if (!silent) {

            Toast.makeText(context, "Descarga agregada a la cola: $title", Toast.LENGTH_SHORT).show()

        }

    } catch (e: Exception) {

        Toast.makeText(context, "Error al iniciar descarga: ${e.message}", Toast.LENGTH_SHORT).show()

    }

}



@Composable

fun LandscapePlayerLayout(

    playerState: PlayerState?,

    isPlaying: Boolean,

    currentPosition: Long,

    duration: Long,

    upNextSongs: List<com.echo.innertube.models.SongItem>,

    onUpNextSongsChange: (List<com.echo.innertube.models.SongItem>) -> Unit,

    onSkipNext: () -> Unit,

    onSkipPrevious: () -> Unit,

    onClose: () -> Unit,

    onTogglePlayPause: () -> Unit,

    onSeek: (Long) -> Unit,

    onVolumeChange: (Float) -> Unit,

    onArtistSelected: (com.mrtdk.liquid_glass.ui.screens.ArtistState) -> Unit,

    onAlbumSelected: (com.mrtdk.liquid_glass.ui.screens.AlbumState) -> Unit,

    onSongSelected: (PlayerState) -> Unit,

    onSongSelectedFromQueue: (PlayerState) -> Unit,

    shuffleModeEnabled: Boolean,

    repeatMode: Int,

    onToggleShuffle: () -> Unit,

    onToggleRepeat: () -> Unit,

    showLyrics: Boolean,

    onShowLyricsChange: (Boolean) -> Unit,

    showQueue: Boolean,

    onShowQueueChange: (Boolean) -> Unit,

    volumePosition: Float,

    onVolumePositionChange: (Float) -> Unit,

    coverBitmap: ImageBitmap?,

    onCoverBitmapChange: (ImageBitmap?) -> Unit,

    dominantColor: Color,

    onDominantColorChange: (Color) -> Unit,

    bottomAverageColor: Color,

    onBottomAverageColorChange: (Color) -> Unit,

    rightSideAverageColor: Color,

    onRightSideAverageColorChange: (Color) -> Unit,

    hdArtUrl: Any?,

    lyricsLines: List<com.mocharealm.accompanist.lyrics.core.model.ISyncedLine>?,

    isRomajiEnabled: Boolean,

    onToggleRomaji: () -> Unit,

    isSaved: Boolean,

    animatedArtworkUrl: String?,

    isVideoPlaying: Boolean,

    onVideoPlayingChange: (Boolean) -> Unit,

    animatedImageLoader: coil.ImageLoader,

    isLightBackground: Boolean,

    contentColor: Color,

    onShowOptionsMenu: (androidx.compose.ui.geometry.Rect?) -> Unit,

    onShowLyricsMenu: () -> Unit,

    onShowPlaylistMenu: () -> Unit,

    onShowArtistMenu: (List<String>, androidx.compose.ui.geometry.Rect?) -> Unit,

    isBottomBarCollapsed: Boolean,

    isAutoScrollEnabled: Boolean = true,

    scrollToCurrentTrigger: Int = 0,

    onAutoScrollChange: (Boolean) -> Unit = {},

    lyricsOffset: Int = 0

) {

    val context = LocalContext.current

    val density = androidx.compose.ui.platform.LocalDensity.current

    val dragOffsetY = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()



    val isBadSong = remember(playerState) {

        val title = playerState?.title ?: ""

        val artist = playerState?.artist ?: ""

        val album = playerState?.album ?: ""

        (album.contains("Bad", ignoreCase = true) || title.contains("Bad", ignoreCase = true)) &&

        artist.contains("Michael Jackson", ignoreCase = true)

    }



    BoxWithConstraints(

        modifier = Modifier

            .fillMaxSize()

    ) {

        val maxWidth = maxWidth

        val maxHeight = maxHeight

        val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }

        val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() }



        val normalTargetOffsetX = 28.dp

        val collapsedTargetOffsetX = 92.dp

        val normalTargetOffsetY = maxHeight - 148.dp

        val collapsedTargetOffsetY = maxHeight - 64.dp

        

        val targetOffsetX = if (isBottomBarCollapsed) collapsedTargetOffsetX else normalTargetOffsetX

        val targetOffsetY = if (isBottomBarCollapsed) collapsedTargetOffsetY else normalTargetOffsetY

        

        val maxDragDistance = with(density) { targetOffsetY.toPx() }

        val dragProgress = if (maxDragDistance > 0f) (dragOffsetY.value / maxDragDistance).coerceIn(0f, 1f) else 0f

        val bgAlpha = 1f - dragProgress



        val startWidth = maxHeight

        val startHeight = maxHeight

        val startOffsetX = 0.dp

        val startOffsetY = 0.dp

        val startCorner = 0.dp



        val threshold = 0.85f

        

        val imgWidthTarget: androidx.compose.ui.unit.Dp

        val imgHeightTarget: androidx.compose.ui.unit.Dp

        val imgOffsetXTarget: androidx.compose.ui.unit.Dp

        val imageCornerTarget: androidx.compose.ui.unit.Dp

        val contentAlpha: Float

        

        val p = dragProgress.coerceIn(0f, 1f)

        val imgOffsetYTarget = androidx.compose.ui.unit.lerp(startOffsetY, targetOffsetY, p)

        if (dragProgress <= threshold) {

            val p1 = if (threshold > 0f) dragProgress / threshold else 0f

            imgWidthTarget = startWidth

            imgHeightTarget = startHeight

            imgOffsetXTarget = startOffsetX

            imageCornerTarget = startCorner

            contentAlpha = (1f - p1).coerceIn(0f, 1f)

        } else {

            val p2 = if (threshold < 1f) (dragProgress - threshold) / (1f - threshold) else 1f

            imgWidthTarget = androidx.compose.ui.unit.lerp(startWidth, 40.dp, p2)

            imgHeightTarget = androidx.compose.ui.unit.lerp(startHeight, 40.dp, p2)

            imgOffsetXTarget = androidx.compose.ui.unit.lerp(startOffsetX, targetOffsetX, p2)

            imageCornerTarget = androidx.compose.ui.unit.lerp(startCorner, 20.dp, p2)

            contentAlpha = 0f

        }



        val imgWidth by androidx.compose.animation.core.animateDpAsState(imgWidthTarget, label = "imgWidth")

        val imgHeight by androidx.compose.animation.core.animateDpAsState(imgHeightTarget, label = "imgHeight")

        val imgOffsetX by androidx.compose.animation.core.animateDpAsState(imgOffsetXTarget, label = "imgOffsetX")

        val imgOffsetY by androidx.compose.animation.core.animateDpAsState(imgOffsetYTarget, label = "imgOffsetY")

        val imgCorner by androidx.compose.animation.core.animateDpAsState(imageCornerTarget, label = "imgCorner")



        Box(

            modifier = Modifier

                .fillMaxSize()

                .background(rightSideAverageColor.copy(alpha = bgAlpha))

                .pointerInput(showLyrics, showQueue) {

                    if (!showLyrics && !showQueue) {

                        detectVerticalDragGestures(

                            onDragEnd = {

                                val currentOffsetY = dragOffsetY.value

                                if (currentOffsetY > with(density) { 150.dp.toPx() }) {

                                    scope.launch {

                                        dragOffsetY.animateTo(

                                            targetValue = maxDragDistance,

                                            animationSpec = tween(300, easing = FastOutSlowInEasing)

                                        )

                                        onClose()

                                    }

                                } else {

                                    scope.launch {

                                        dragOffsetY.animateTo(0f, spring())

                                    }

                                }

                            }

                        ) { change, dragAmount ->

                            if (dragAmount > 0f || dragOffsetY.value > 0f) {

                                val newOffset = (dragOffsetY.value + dragAmount * 0.7f).coerceAtLeast(0f)

                                scope.launch { dragOffsetY.snapTo(newOffset) }

                            }

                        }

                    }

                }

        ) {







        // 2. Left side Album Art (with morphing layout)

        Box(

            modifier = Modifier

                .offset(x = imgOffsetX, y = imgOffsetY)

                .size(width = imgWidth, height = imgHeight)

                .clip(RoundedCornerShape(imgCorner))

        ) {

            // Sharp base cover

            AsyncImage(

                model = ImageRequest.Builder(context)

                    .data(hdArtUrl)

                    .crossfade(true)

                    .build(),

                imageLoader = animatedImageLoader,

                contentDescription = "Album Art",

                contentScale = ContentScale.Crop,

                modifier = Modifier

                    .fillMaxSize()

                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }

                    .drawWithContent {

                        drawContent()

                        drawRect(

                            brush = Brush.horizontalGradient(

                                colorStops = arrayOf(

                                    0.0f to Color.Black,

                                    (0.75f + 0.25f * (1f - contentAlpha)).coerceIn(0.75f, 1.0f) to Color.Black,

                                    1.0f to Color.Black.copy(alpha = 1f - contentAlpha)

                                )

                            ),

                            blendMode = BlendMode.DstIn

                        )

                    }

            )



            val currentAnimatedUrl = animatedArtworkUrl

            if (!currentAnimatedUrl.isNullOrBlank()) {

                DisposableEffect(Unit) {

                    onDispose {

                        onVideoPlayingChange(false)

                    }

                }

                com.mrtdk.liquid_glass.ui.components.AnimatedArtworkPlayer(

                    videoUrl = currentAnimatedUrl,

                    modifier = Modifier

                        .fillMaxSize()

                        .graphicsLayer { 

                            alpha = if (isVideoPlaying) 1f else 0f 

                            compositingStrategy = CompositingStrategy.Offscreen

                        }

                        .drawWithContent {

                            drawContent()

                            drawRect(

                                brush = Brush.horizontalGradient(

                                    colorStops = arrayOf(

                                        0.0f to Color.Black,

                                        (0.75f + 0.25f * (1f - contentAlpha)).coerceIn(0.75f, 1.0f) to Color.Black,

                                        1.0f to Color.Black.copy(alpha = 1f - contentAlpha)

                                    )

                                ),

                                blendMode = BlendMode.DstIn

                            )

                        },

                    isPaused = false,

                    enableFrameCapture = true,

                    onPlaybackStarted = { onVideoPlayingChange(true) },

                    onFrameCaptured = { frameBitmap ->

                        onCoverBitmapChange(frameBitmap.asImageBitmap())

                        try {

                            var r = 0L; var g = 0L; var b = 0L

                            val yCoord = frameBitmap.height - 1

                            val w = frameBitmap.width

                            for (x in 0 until w) {

                                val pixel = frameBitmap.getPixel(x, yCoord)

                                r += android.graphics.Color.red(pixel)

                                g += android.graphics.Color.green(pixel)

                                b += android.graphics.Color.blue(pixel)

                            }

                            val avgColor = Color((r / w).toInt(), (g / w).toInt(), (b / w).toInt())

                            onBottomAverageColorChange(avgColor)

                            onDominantColorChange(avgColor)



                            // Promedio de la columna derecha de píxeles

                            var rRight = 0L; var gRight = 0L; var bRight = 0L

                            val xCoord = frameBitmap.width - 1

                            val h = frameBitmap.height

                            for (y in 0 until h) {

                                val pixel = frameBitmap.getPixel(xCoord, y)

                                rRight += android.graphics.Color.red(pixel)

                                gRight += android.graphics.Color.green(pixel)

                                bRight += android.graphics.Color.blue(pixel)

                            }

                            onRightSideAverageColorChange(Color((rRight / h).toInt(), (gRight / h).toInt(), (bRight / h).toInt()))

                        } catch (e: Exception) { }

                    }

                )

            }



            // Blurred overlay to smooth the transition on the right edge

            val currentBitmap = coverBitmap

            if (currentBitmap != null && contentAlpha > 0f) {

                Image(

                    bitmap = currentBitmap,

                    contentDescription = null,

                    contentScale = ContentScale.Crop,

                    modifier = Modifier

                        .fillMaxSize()

                        .graphicsLayer { 

                            alpha = contentAlpha

                            compositingStrategy = CompositingStrategy.Offscreen 

                        }

                        .cloudy(radius = 100)

                        .drawWithContent {

                            drawContent()

                            drawRect(

                                brush = Brush.horizontalGradient(

                                    colorStops = arrayOf(

                                        0.0f to Color.Transparent,

                                        0.70f to Color.Transparent,

                                        0.85f to Color.Black,

                                        1.0f to Color.Transparent

                                    )

                                ),

                                blendMode = BlendMode.DstIn

                            )

                        }

                )

            } else if (contentAlpha > 0f) {

                AsyncImage(

                    model = ImageRequest.Builder(context)

                        .data(hdArtUrl)

                        .size(150)

                        .crossfade(true)

                        .build(),

                    contentDescription = null,

                    contentScale = ContentScale.Crop,

                    modifier = Modifier

                        .fillMaxSize()

                        .graphicsLayer { 

                            alpha = contentAlpha

                            compositingStrategy = CompositingStrategy.Offscreen 

                        }

                        .cloudy(radius = 100)

                        .drawWithContent {

                            drawContent()

                            drawRect(

                                brush = Brush.horizontalGradient(

                                    colorStops = arrayOf(

                                        0.0f to Color.Transparent,

                                        0.70f to Color.Transparent,

                                        0.85f to Color.Black,

                                        1.0f to Color.Transparent

                                    )

                                ),

                                blendMode = BlendMode.DstIn

                            )

                        }

                )

            }

        }



        // 3. Right side Content

        val rightSideWidth = maxWidth - maxHeight

        Box(

            modifier = Modifier

                .align(Alignment.CenterEnd)

                .width(rightSideWidth)

                .fillMaxHeight()

                .padding(vertical = 16.dp, horizontal = 24.dp)

                .graphicsLayer {

                    alpha = contentAlpha

                }

        ) {

            Column(modifier = Modifier.fillMaxSize()) {

                // Shared Top Header Row: Title/Artist and Star/Options buttons

                Row(

                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    var dragAccumulator by remember { mutableStateOf(0f) }

                    Box(

                        modifier = Modifier

                            .weight(1f)

                            .pointerInput(playerState) {

                                detectHorizontalDragGestures(

                                    onDragEnd = {

                                        if (dragAccumulator < -60f) { onSkipNext() }

                                        else if (dragAccumulator > 60f) { onSkipPrevious() }

                                        dragAccumulator = 0f

                                    },

                                    onHorizontalDrag = { change, dragAmount ->

                                        dragAccumulator += dragAmount

                                        change.consume()

                                    }

                                )

                            }

                    ) {

                        Column(

                            modifier = Modifier

                                .fillMaxWidth()

                        ) {

                            Text(

                                text = playerState?.title ?: "",

                                color = contentColor,

                                fontSize = 22.sp,

                                fontWeight = FontWeight.Bold,

                                maxLines = 1,

                                overflow = TextOverflow.Ellipsis

                            )



                            Spacer(modifier = Modifier.height(1.dp))



                            var artistCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                            Text(
                                text = playerState?.artist ?: "",
                                color = contentColor.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .onGloballyPositioned { artistCoords = it }
                                    .clickable {
                                        if (playerState?.artist != null) {
                                            val artistList = playerState.artist.split(", ").filter { it.isNotEmpty() }
                                            if (artistList.isNotEmpty()) {
                                                onShowArtistMenu(artistList, artistCoords?.boundsInRoot())
                                            }
                                        }
                                    }
                            )

                        }

                    }



                    Row(

                        horizontalArrangement = Arrangement.spacedBy(16.dp),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Box(

                            modifier = Modifier

                                .size(36.dp)

                                .clip(CircleShape)

                                .background(

                                    if (isSaved) contentColor else contentColor.copy(alpha = 0.15f)

                                )

                                .clickable {

                                    if (playerState != null) {

                                        if (!isSaved) {

                                            LibraryManager.saveItem(

                                                LibraryItem(

                                                    playerState.videoId ?: "",

                                                    playerState.title,

                                                    playerState.artist,

                                                    playerState.artUrl?.toString(),

                                                    ItemType.SONG

                                                )

                                            )

                                        } else {

                                            LibraryManager.removeItem(playerState.videoId ?: "")

                                        }

                                    }

                                },

                            contentAlignment = Alignment.Center

                        ) {

                            AsyncImage(

                                model = "file:///android_asset/img reproductor/c.png",

                                contentDescription = "Fav",

                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(

                                    if (isSaved) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor

                                ),

                                modifier = Modifier.size(18.dp)

                            )

                        }

                                                var threeDotsCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { threeDotsCoords = it }
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(contentColor.copy(alpha = 0.15f))
                                .clickable { onShowOptionsMenu(threeDotsCoords?.boundsInRoot()) },

                            contentAlignment = Alignment.Center

                        ) {

                            Canvas(modifier = Modifier.size(18.dp)) {

                                val r = 1.5.dp.toPx()

                                val space = 4.dp.toPx()

                                val cx = size.width / 2f

                                val cy = size.height / 2f

                                drawCircle(contentColor, radius = r, center = Offset(cx - space - r * 2, cy))

                                drawCircle(contentColor, radius = r, center = Offset(cx, cy))

                                drawCircle(contentColor, radius = r, center = Offset(cx + space + r * 2, cy))

                            }

                        }

                    }

                }



                Spacer(modifier = Modifier.height(8.dp))



                // Switch right column contents based on showLyrics or showQueue

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

                    if (showLyrics) {

                        // View 2: Lyrics View

                        Box(modifier = Modifier.fillMaxSize()) {

                            val lyricsListState = rememberLazyListState()

                            val currentLyricsLines = lyricsLines

                            if (currentLyricsLines != null && currentLyricsLines.isNotEmpty()) {

                                val isSynced = currentLyricsLines.any { it.timeMs > 0L }

                                

                                                                val lyricsScrollConnection = remember {
                                    object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                                        override fun onPostScroll(
                                            consumed: Offset,
                                            available: Offset,
                                            source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
                                        ): Offset {
                                            if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
                                                onAutoScrollChange(false)
                                            }
                                            return Offset.Zero
                                        }

                                        override suspend fun onPostFling(
                                            consumed: Velocity,
                                            available: Velocity
                                        ): Velocity {
                                            onAutoScrollChange(false)
                                            return Velocity.Zero
                                        }
                                    }
                                }

                                LaunchedEffect(currentPosition, currentLyricsLines, isAutoScrollEnabled, scrollToCurrentTrigger) {
                                    if (!isSynced) return@LaunchedEffect
                                    if (!isAutoScrollEnabled && scrollToCurrentTrigger == 0) return@LaunchedEffect
                                    val currentIdx = currentLyricsLines.indexOfLast { it.timeMs != -1L && it.timeMs <= currentPosition + lyricsOffset + 500 }
                                    if (currentIdx >= 0 && !lyricsListState.isScrollInProgress) {
                                        lyricsListState.animateScrollToItem(currentIdx.coerceAtLeast(0), scrollOffset = -100)
                                    }
                                }

                                LazyColumn(
                                    state = lyricsListState,
                                    modifier = Modifier.fillMaxSize().nestedScroll(lyricsScrollConnection),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    item { Spacer(modifier = Modifier.height(16.dp)) }
                                    items(currentLyricsLines.size) { i ->
                                        val line = currentLyricsLines[i]
                                        val isCurrent = isSynced && line.timeMs != -1L && (currentPosition + lyricsOffset) >= line.timeMs && 
                                            (i == currentLyricsLines.lastIndex || (currentPosition + lyricsOffset) < currentLyricsLines[i+1].timeMs)
                                        val isPast = isSynced && line.timeMs != -1L && (currentPosition + lyricsOffset) > line.timeMs
                                        val distance = if (isSynced) {
                                            val curIdx = currentLyricsLines.indexOfLast { it.timeMs != -1L && it.timeMs <= (currentPosition + lyricsOffset) + 500 }
                                            if (curIdx >= 0) kotlin.math.abs(i - curIdx) else 0
                                        } else 0

                                        

                                        val lineDuration = remember(line.timeMs) {

                                            val nextLineTime = currentLyricsLines.getOrNull(i + 1)?.timeMs

                                            if (nextLineTime != null && nextLineTime > 0 && line.timeMs > 0) nextLineTime - line.timeMs else 4000L

                                        }

                                        val activeDuration = (lineDuration * 0.95).toLong().coerceAtLeast(300L)

                                        val lineRelTime = if (isCurrent && line.timeMs > 0) ((currentPosition + lyricsOffset) - line.timeMs).coerceAtLeast(0L) else if (isPast) activeDuration else 0L

                                        

                                        val targetAlpha = when {

                                            !isSynced || isCurrent -> 1f

                                            distance == 1 -> 0.55f

                                            distance == 2 -> 0.4f

                                            else -> 0.3f

                                        }

                                        val targetScale = when {

                                            !isSynced || isCurrent -> 1.03f

                                            distance == 1 -> 0.97f

                                            distance >= 2 -> 0.88f

                                            else -> 1f

                                        }

                                        val targetBlur = if (!isCurrent && isSynced) {

                                            when (distance) {

                                                1 -> 2f

                                                2 -> 3f

                                                else -> 4f

                                            }

                                        } else 0f

                                        

                                        val animAlpha by animateFloatAsState(targetAlpha, animationSpec = tween(260), label="lyricsAlpha")

                                        val animScale by animateFloatAsState(targetScale, animationSpec = tween(320), label="lyricsScale")

                                        val animBlur by animateFloatAsState(targetBlur, animationSpec = tween(420), label="lyricsBlur")

                                        

                                        val wordData = remember(line.text, activeDuration) {

                                            val words = line.text.split(" ").filter { it.isNotEmpty() }

                                            if (words.isEmpty()) {

                                                listOf(Triple(line.text, 0L, activeDuration))

                                            } else {

                                                val totalChars = line.text.length

                                                var accumulatedTime = 0L

                                                words.mapIndexed { wordIndex, word ->

                                                    val charCount = if (wordIndex < words.lastIndex) word.length + 1 else word.length

                                                    val wordStart = accumulatedTime

                                                    val wordDur = if (totalChars > 0) (activeDuration * charCount.toFloat() / totalChars).toLong() else activeDuration

                                                    accumulatedTime += wordDur

                                                    Triple(if (wordIndex < words.lastIndex) "$word " else word, wordStart, wordStart + wordDur)

                                                }

                                            }

                                        }



                                        @OptIn(ExperimentalLayoutApi::class)

                                        FlowRow(

                                            modifier = Modifier

                                                .fillMaxWidth()

                                                .graphicsLayer {

                                                    scaleX = animScale; scaleY = animScale

                                                    alpha = animAlpha

                                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)

                                                }

                                                .then(if (animBlur > 0f) Modifier.blur(animBlur.dp) else Modifier)

                                                .clickable { 
                                                           if(line.timeMs != -1L) onSeek((line.timeMs - lyricsOffset).coerceAtLeast(0L))

                                                },

                                            horizontalArrangement = Arrangement.Start,

                                            verticalArrangement = Arrangement.spacedBy(4.dp)

                                        ) {

                                            wordData.forEach { (wordText, startRelative, endRelative) ->

                                                val wordDuration = (endRelative - startRelative).coerceAtLeast(1L)

                                                val wordProgress by animateFloatAsState(

                                                    targetValue = when {

                                                        lineRelTime >= endRelative -> 1f

                                                        lineRelTime < startRelative -> 0f

                                                        else -> (lineRelTime - startRelative).toFloat() / wordDuration

                                                    },

                                                    animationSpec = tween(

                                                        durationMillis = wordDuration.coerceIn(140L, 260L).toInt(),

                                                        easing = FastOutSlowInEasing

                                                    ),

                                                    label = "wordProgress"

                                                )

                                                val finalFontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold

                                                Text(

                                                    text = wordText,

                                                    fontSize = 24.sp,

                                                    style = TextStyle(

                                                        brush = if (isCurrent) Brush.horizontalGradient(

                                                            0.0f to contentColor,

                                                            (wordProgress - 0.05f).coerceAtLeast(0f) to contentColor,

                                                            (wordProgress + 0.05f).coerceAtMost(1f) to contentColor.copy(alpha = 0.4f),

                                                            1.0f to contentColor.copy(alpha = 0.4f)

                                                        ) else null,

                                                        fontWeight = finalFontWeight,

                                                        lineHeight = 32.sp,

                                                        shadow = if (isCurrent && wordProgress > 0.1f) Shadow(

                                                            color = contentColor.copy(alpha = 0.6f * wordProgress),

                                                            offset = Offset.Zero,

                                                            blurRadius = (12f * wordProgress).coerceAtLeast(0.1f)

                                                        ) else null

                                                    ),

                                                    color = if (!isCurrent) contentColor else Color.Unspecified

                                                )

                                            }

                                        }

                                    }

                                    item { Spacer(modifier = Modifier.height(80.dp)) }

                                }

                            } else {

                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                                    CircularProgressIndicator(color = contentColor)

                                }

                            }

                        }

                    } else if (showQueue) {

                        // View 3: Queue View

                        val queueListState = rememberLazyListState()

                        Column(modifier = Modifier.fillMaxSize()) {

                            val shuffleInteraction = remember { MutableInteractionSource() }

                            val isShuffleActive = shuffleModeEnabled

                            val isShufflePressed by shuffleInteraction.collectIsPressedAsState()

                            val shuffleScale by animateFloatAsState(targetValue = if (isShufflePressed) 0.85f else 1.0f, label = "shuffleScale")

                            

                            val activeBg = contentColor.copy(alpha = 0.9f)

                            val activeIcon = if (contentColor == Color.White) rightSideAverageColor else Color.White



                            val shuffleBgColor by animateColorAsState(targetValue = if (isShuffleActive) activeBg else contentColor.copy(alpha=0.15f), label = "shuffleBg")

                            val shuffleIconColor by animateColorAsState(targetValue = if (isShuffleActive) activeIcon else contentColor.copy(alpha=0.5f), label = "shuffleIcon")



                            val repeatInteraction = remember { MutableInteractionSource() }

                            val isRepeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF

                            val isRepeatPressed by repeatInteraction.collectIsPressedAsState()

                            val repeatScale by animateFloatAsState(targetValue = if (isRepeatPressed) 0.85f else 1.0f, label = "repeatScale")

                            val repeatBgColor by animateColorAsState(targetValue = if (isRepeatActive) activeBg else contentColor.copy(alpha=0.15f), label = "repeatBg")

                            val repeatIconColor by animateColorAsState(targetValue = if (isRepeatActive) activeIcon else contentColor.copy(alpha=0.5f), label = "repeatIcon")



                            val autoplayInteraction = remember { MutableInteractionSource() }

                            val isAutoplayActive = playerState?.isExclusiveQueue != true

                            val isAutoplayPressed by autoplayInteraction.collectIsPressedAsState()

                            val autoplayScale by animateFloatAsState(targetValue = if (isAutoplayPressed) 0.85f else 1.0f, label = "autoplayScale")

                            val autoplayBgColor by animateColorAsState(targetValue = if (isAutoplayActive) activeBg else contentColor.copy(alpha=0.15f), label = "autoplayBg")

                            val autoplayIconColor by animateColorAsState(targetValue = if (isAutoplayActive) activeIcon else contentColor.copy(alpha=0.5f), label = "autoplayIcon")



                            val romajiInteraction = remember { MutableInteractionSource() }

                            val isRomajiActive = isRomajiEnabled

                            val isRomajiPressed by romajiInteraction.collectIsPressedAsState()

                            val romajiScale by animateFloatAsState(targetValue = if (isRomajiPressed) 0.85f else 1.0f, label = "romajiScale")

                            val romajiBgColor by animateColorAsState(targetValue = if (isRomajiActive) activeBg else contentColor.copy(alpha=0.15f), label = "romajiBg")

                            val romajiIconColor by animateColorAsState(targetValue = if (isRomajiActive) activeIcon else contentColor.copy(alpha=0.5f), label = "romajiIcon")



                            Row(

                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),

                                horizontalArrangement = Arrangement.spacedBy(12.dp)

                            ) {

                                Box(

                                    modifier = Modifier

                                        .weight(1f)

                                        .graphicsLayer(scaleX = shuffleScale, scaleY = shuffleScale)

                                        .height(36.dp)

                                        .clip(RoundedCornerShape(50))

                                        .background(shuffleBgColor)

                                        .clickable(

                                            interactionSource = shuffleInteraction,

                                            indication = null,

                                            onClick = onToggleShuffle

                                        ),

                                    contentAlignment=Alignment.Center

                                ) {

                                    Icon(painterResource(id = R.drawable.shuffle), "Shuffle", tint=shuffleIconColor, modifier=Modifier.size(18.dp))

                                }

                                Box(

                                    modifier = Modifier

                                        .weight(1f)

                                        .graphicsLayer(scaleX = repeatScale, scaleY = repeatScale)

                                        .height(36.dp)

                                        .clip(RoundedCornerShape(50))

                                        .background(repeatBgColor)

                                        .clickable(

                                            interactionSource = repeatInteraction,

                                            indication = null,

                                            onClick = onToggleRepeat

                                        ),

                                    contentAlignment=Alignment.Center

                                ) {

                                    Icon(if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint=repeatIconColor, modifier=Modifier.size(18.dp))

                                }

                                Box(

                                    modifier = Modifier

                                        .weight(1f)

                                        .graphicsLayer(scaleX = autoplayScale, scaleY = autoplayScale)

                                        .height(36.dp)

                                        .clip(RoundedCornerShape(50))

                                        .background(autoplayBgColor)

                                        .clickable(

                                            interactionSource = autoplayInteraction,

                                            indication = null,

                                            onClick = {

                                                if (playerState != null) {

                                                    onSongSelected(playerState.copy(isExclusiveQueue = !playerState.isExclusiveQueue))

                                                }

                                            }

                                        ),

                                    contentAlignment=Alignment.Center

                                ) {

                                    Icon(Icons.Default.AllInclusive, "Autoplay", tint=autoplayIconColor, modifier=Modifier.size(18.dp))

                                }

                                Box(

                                    modifier = Modifier

                                        .weight(1f)

                                        .graphicsLayer(scaleX = romajiScale, scaleY = romajiScale)

                                        .height(36.dp)

                                        .clip(RoundedCornerShape(50))

                                        .background(romajiBgColor)

                                        .clickable(

                                            interactionSource = romajiInteraction,

                                            indication = null,

                                            onClick = onToggleRomaji

                                        ),

                                    contentAlignment=Alignment.Center

                                ) {

                                    Icon(if (isRomajiActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff, "Toggle", tint=romajiIconColor, modifier=Modifier.size(22.dp))

                                }

                            }



                            if (playerState != null && playerState.queue.isNotEmpty()) {

                                Text(text = stringResource(R.string.siguiente_en_album_playlist), color=contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=8.dp, bottom=6.dp))

                            } else if (playerState?.isExclusiveQueue != true) {

                                Column(modifier = Modifier.padding(top=8.dp, bottom=6.dp)) {

                                    Text(text = stringResource(R.string.continue_playing), color=contentColor, fontSize=16.sp, fontWeight=FontWeight.Bold)

                                    Text(text = stringResource(R.string.autoplaying_similar_music), color=contentColor.copy(alpha=0.7f), fontSize=12.sp)

                                }

                            }



                            LazyColumn(

                                state = queueListState,

                                modifier = Modifier.weight(1f).fillMaxWidth(),

                                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),

                                verticalArrangement = Arrangement.spacedBy(8.dp)

                            ) {

                                if (playerState != null) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.player_queue_now_playing),
                                            color = contentColor,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            val upgradedArt = playerState.artUrl?.let {
                                                val itStr = it.toString()
                                                if (itStr.startsWith("file:///android_asset/")) {
                                                    it
                                                } else {
                                                    val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(itStr) ?: itStr
                                                    if (it is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
                                                }
                                            } ?: playerState.artUrl

                                            AsyncImage(
                                                model = ImageRequest.Builder(context).data(upgradedArt).crossfade(false).build(),
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(playerState.title ?: "", color = contentColor, fontSize = 15.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                                                Text(playerState.artist ?: "", color = contentColor.copy(alpha=0.6f), fontSize = 13.sp, maxLines = 1)
                                            }
                                            PlayingEqualizer(color = contentColor, isPlaying = isPlaying, modifier = Modifier.size(24.dp))
                                        }
                                        androidx.compose.material3.Divider(color = Color.DarkGray.copy(alpha = 0.5f))
                                    }
                                }

                                if (playerState != null && playerState.queue.isNotEmpty()) {
                                     val state = playerState
                                     items(
                                         count = state.queue.size,
                                         key = { index -> "${state.queue[index].videoId}_$index" }
                                     ) { index ->
                                         val qItem = state.queue[index]
                                         val isCurrent = state.videoId != null && qItem.videoId == state.videoId

                                         val rowData = remember(qItem.title, qItem.artist, qItem.artUrl) {
                                             QueueItemRowData(
                                                 title = qItem.title,
                                                 artist = qItem.artist,
                                                 artUrl = qItem.artUrl
                                             )
                                         }

                                         val onRowClick = remember(qItem, index, state) {
                                             {
                                                 val remaining = state.queue.drop(index + 1)
                                                 onSongSelectedFromQueue(PlayerState(
                                                     title = qItem.title,
                                                     artist = qItem.artist,
                                                     artUrl = qItem.artUrl?.let {
                                                         val itStr = it.toString()
                                                         if (itStr.startsWith("file:///android_asset/")) {
                                                             it
                                                         } else {
                                                             val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(itStr) ?: itStr
                                                             if (it is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
                                                         }
                                                     } ?: qItem.artUrl,
                                                     videoId = qItem.videoId,
                                                     queue = remaining,
                                                     isExclusiveQueue = state.isExclusiveQueue,
                                                     album = qItem.album,
                                                     albumId = qItem.albumId
                                                 ))
                                             }
                                         }

                                         QueueItemRow(
                                             rowData = rowData,
                                             contentColor = contentColor,
                                             isPlaying = if (isCurrent) isPlaying else false,
                                             isCurrentPlayingItem = isCurrent,
                                             context = context,
                                             titleFontSize = 15.sp,
                                             artistFontSize = 13.sp,
                                             onClick = onRowClick
                                         )
                                     }
                                 }

                                 if (playerState?.isExclusiveQueue != true) {
                                     val state = playerState
                                     itemsIndexed(
                                         items = upNextSongs,
                                         key = { index, song -> "${song.id}_$index" }
                                     ) { i, song ->
                                         val isCurrent = state != null && song.id == state.videoId

                                         val rowData = remember(song.title, song.artists, song.thumbnail) {
                                             UpNextSongRowData(
                                                 title = song.title,
                                                 artist = song.artists.joinToString { it.name },
                                                 thumbnail = song.thumbnail
                                             )
                                         }

                                         val onRowClick = remember(song, i, upNextSongs, state) {
                                             {
                                                 val upgradedArt = song.thumbnail?.let {
                                                     com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(it) ?: it
                                                 } ?: song.thumbnail
                                                 onUpNextSongsChange(upNextSongs.drop(i + 1))
                                                 onSongSelectedFromQueue(PlayerState(
                                                     title = song.title,
                                                     artist = song.artists.joinToString { it.name },
                                                     artUrl = upgradedArt,
                                                     videoId = song.id,
                                                     isExclusiveQueue = state?.isExclusiveQueue ?: false,
                                                     album = song.album?.name,
                                                     albumId = song.album?.id
                                                 ))
                                             }
                                         }

                                         UpNextSongRow(
                                             rowData = rowData,
                                             contentColor = contentColor,
                                             context = context,
                                             isPlaying = if (isCurrent) isPlaying else false,
                                             isCurrentPlayingItem = isCurrent,
                                             titleFontSize = 15.sp,
                                             artistFontSize = 13.sp,
                                             onClick = onRowClick
                                         )
                                     }
                                 }
                                 item { Spacer(modifier = Modifier.height(40.dp)) }

                            }

                        }

                    } else {

                        // View 1: Main Controls Layout (Slider, Controls, Volume)

                        Column(modifier = Modifier.fillMaxSize()) {

                            Spacer(modifier = Modifier.weight(1f))



                            // Apple Music slider

                            val progressVal = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

                            AppleMusicSlider(

                                value = progressVal,

                                onValueChange = { onSeek((it * duration).toLong()) },

                                modifier = Modifier.fillMaxWidth().height(24.dp),

                                activeColor = contentColor,

                                inactiveColor = contentColor.copy(alpha = 0.3f),

                                barHeightDp = 8.dp

                            )

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween

                            ) {

                                Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)

                                Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)

                            }



                            Spacer(modifier = Modifier.height(16.dp))



                            // Playback controls (Prev, Play, Next)

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.Center,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                AnimatedSkipButton(

                                    iconId = R.drawable.previous,

                                    contentDescription = "Previous",

                                    contentColor = contentColor,

                                    sizeDp = 84.dp,

                                    iconSizeDp = 64.dp,

                                    onClick = onSkipPrevious

                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                

                                val playPauseInteractionSource = remember { MutableInteractionSource() }

                                val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()

                                val playPauseBgColor by animateColorAsState(

                                    targetValue = if (isPlayPausePressed) contentColor.copy(alpha = 0.12f) else Color.Transparent,

                                    label = "playPauseBg"

                                )

                                val playPauseRotation by animateFloatAsState(

                                    targetValue = if (isPlaying) 180f else 0f,

                                    animationSpec = spring(

                                        dampingRatio = Spring.DampingRatioMediumBouncy,

                                        stiffness = Spring.StiffnessLow

                                    ),

                                    label = "playPauseButtonRotation"

                                )



                                Box(

                                    modifier = Modifier

                                        .size(84.dp)

                                        .clip(CircleShape)

                                        .background(playPauseBgColor)

                                        .clickable(

                                            interactionSource = playPauseInteractionSource,

                                            indication = androidx.compose.foundation.LocalIndication.current,

                                            onClick = onTogglePlayPause

                                        ),

                                    contentAlignment = Alignment.Center

                                ) {

                                    AnimatedContent(

                                        targetState = isPlaying,

                                        transitionSpec = {

                                            (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)))

                                                .togetherWith(fadeOut(animationSpec = tween(90)) + scaleOut(targetScale = 0.3f, animationSpec = tween(90)))

                                        },

                                        label = "playPauseIcon"

                                    ) { playing ->

                                        Icon(

                                            painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.resume),

                                            contentDescription = if (playing) "Pause" else "Play",

                                            tint = contentColor,

                                            modifier = Modifier

                                                .size(64.dp)

                                                .graphicsLayer {

                                                    rotationZ = playPauseRotation

                                                }

                                        )

                                    }

                                }



                                Spacer(modifier = Modifier.width(16.dp))

                                AnimatedSkipButton(

                                    iconId = R.drawable.forward,

                                    contentDescription = "Next",

                                    contentColor = contentColor,

                                    sizeDp = 84.dp,

                                    iconSizeDp = 64.dp,

                                    onClick = onSkipNext

                                )

                            }



                            Spacer(modifier = Modifier.height(24.dp))



                            val deviceLabel = AudioRoutingState.connectedDeviceName ?: stringResource(R.string.celular_speaker)

                            Text(

                                text = deviceLabel,

                                color = contentColor.copy(alpha = 0.5f),

                                fontSize = 12.sp,

                                fontWeight = FontWeight.Bold,

                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)

                            )



                            // Volume Row

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    painter = painterResource(id = R.drawable.albumspeaker),

                                    contentDescription = "Low volume",

                                    tint = contentColor.copy(alpha = 0.7f),

                                    modifier = Modifier.size(16.dp)

                                )

                                Spacer(modifier = Modifier.width(20.dp))

                                AppleMusicSlider(

                                    value = volumePosition,

                                    onValueChange = { v ->

                                        onVolumePositionChange(v)

                                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (v * maxVolume).toInt(), 0)

                                        onVolumeChange(v)

                                    },

                                    modifier = Modifier.weight(1f).height(24.dp),

                                    activeColor = contentColor,

                                    inactiveColor = contentColor.copy(alpha = 0.2f),

                                    barHeightDp = 8.dp

                                )

                                Spacer(modifier = Modifier.width(20.dp))

                                Icon(

                                    painter = painterResource(id = R.drawable.albumspeakerlarge),

                                    contentDescription = "High volume",

                                    tint = contentColor.copy(alpha = 0.7f),

                                    modifier = Modifier.size(24.dp)

                                )

                            }

                            Spacer(modifier = Modifier.weight(1f))

                        }

                    }

                }



                // Shared Bottom Bar Buttons Row (Lyrics, Cast/Format, Queue)

                Row(

                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Box(

                        modifier = Modifier

                            .size(40.dp)

                            .clip(CircleShape)

                            .background(if (showLyrics) contentColor else Color.Transparent)

                            .clickable {

                                onShowLyricsChange(!showLyrics)

                                onShowQueueChange(false)

                            },

                        contentAlignment = Alignment.Center

                    ) {

                        AsyncImage(

                            model = "file:///android_asset/img reproductor/Letras.png",

                            contentDescription = "Lyrics",

                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(

                                if (showLyrics) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor

                            ),

                            modifier = Modifier.size(20.dp)

                        )

                    }



                    AsyncImage(

                        model = "file:///android_asset/img reproductor/parlante.png",

                        contentDescription = "Format",

                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor),

                        modifier = Modifier

                            .height(20.dp)

                            .padding(horizontal = 8.dp)

                            .clickable(

                                interactionSource = remember { MutableInteractionSource() },

                                indication = null

                            ) { AudioRoutingState.showAudioRoutingMenu = true }

                    )



                    Box(

                        modifier = Modifier

                            .size(40.dp)

                            .clip(CircleShape)

                            .background(if (showQueue) contentColor else Color.Transparent)

                            .clickable {

                                onShowQueueChange(!showQueue)

                                onShowLyricsChange(false)

                            },

                        contentAlignment = Alignment.Center

                    ) {

                        Icon(

                            painter = painterResource(id = R.drawable.nextinfo),

                            contentDescription = "Next Info",

                            tint = if (showQueue) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor,

                            modifier = Modifier.size(24.dp)

                        )

                    }

                }

            }

        }

        }

    }

}

@Composable
private fun PlayingEqualizer(
    color: Color,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier.size(24.dp)
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "eq")
    
    val heightFraction1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 450, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar1"
    )
    
    val heightFraction2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar2"
    )
    
    val heightFraction3 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.7f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar3"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barWidth = 3.dp.toPx()
        val spacing = 3.dp.toPx()
        
        val totalWidth = barWidth * 3 + spacing * 2
        val startX = (w - totalWidth) / 2f
        
        val h1 = if (isPlaying) heightFraction1 else 0.2f
        val h2 = if (isPlaying) heightFraction2 else 0.3f
        val h3 = if (isPlaying) heightFraction3 else 0.15f
        
        drawRect(
            color = color,
            topLeft = Offset(startX, h * (1f - h1)),
            size = androidx.compose.ui.geometry.Size(barWidth, h * h1)
        )
        
        drawRect(
            color = color,
            topLeft = Offset(startX + barWidth + spacing, h * (1f - h2)),
            size = androidx.compose.ui.geometry.Size(barWidth, h * h2)
        )
        
        drawRect(
            color = color,
            topLeft = Offset(startX + (barWidth + spacing) * 2f, h * (1f - h3)),
            size = androidx.compose.ui.geometry.Size(barWidth, h * h3)
        )
    }
}

@Composable
private fun QueueItemRow(
    rowData: QueueItemRowData,
    contentColor: Color,
    isPlaying: Boolean,
    isCurrentPlayingItem: Boolean,
    context: android.content.Context,
    titleFontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    artistFontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    onClick: () -> Unit
) {
    val upgradedArt = remember(rowData.artUrl) {
        rowData.artUrl?.let {
            val itStr = it.toString()
            if (itStr.startsWith("file:///android_asset/")) {
                it
            } else {
                val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(itStr) ?: itStr
                if (it is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
            }
        } ?: rowData.artUrl
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        AsyncImage(
            model = upgradedArt,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rowData.title,
                color = contentColor,
                fontSize = titleFontSize,
                maxLines = 1,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = rowData.artist,
                color = contentColor.copy(alpha = 0.6f),
                fontSize = artistFontSize,
                maxLines = 1
            )
        }
        if (isCurrentPlayingItem) {
            PlayingEqualizer(color = contentColor, isPlaying = isPlaying, modifier = Modifier.size(24.dp))
        } else {
            Icon(Icons.Default.Menu, contentDescription = null, tint = contentColor.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun UpNextSongRow(
    rowData: UpNextSongRowData,
    contentColor: Color,
    context: android.content.Context,
    isPlaying: Boolean,
    isCurrentPlayingItem: Boolean,
    titleFontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    artistFontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    onClick: () -> Unit
) {
    val hdThumb = remember(rowData.thumbnail) {
        rowData.thumbnail?.let {
            com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(it) ?: it
        } ?: rowData.thumbnail
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        AsyncImage(
            model = hdThumb,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rowData.title,
                color = contentColor,
                fontSize = titleFontSize,
                maxLines = 1,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = rowData.artist,
                color = contentColor.copy(alpha = 0.6f),
                fontSize = artistFontSize,
                maxLines = 1
            )
        }
        if (isCurrentPlayingItem) {
            PlayingEqualizer(color = contentColor, isPlaying = isPlaying, modifier = Modifier.size(24.dp))
        } else {
            Icon(Icons.Default.Menu, contentDescription = null, tint = contentColor.copy(alpha = 0.6f))
        }
    }
}

private fun calculateDominantSkew(bitmap: android.graphics.Bitmap): Float {
    val w = bitmap.width
    val h = bitmap.height
    if (w < 40 || h < 40) return 0.15f // Fallback

    val y1 = h - 2
    val y2 = h - 12

    var bestDx = 0
    var minDiff = Long.MAX_VALUE

    val range = 12 // test shifts from -12 to 12
    val margin = 15

    // Cache luminance values for rows to make it super fast
    val lum1 = IntArray(w)
    val lum2 = IntArray(w)
    for (x in 0 until w) {
        val p1 = bitmap.getPixel(x, y1)
        lum1[x] = ((p1 shr 16 and 0xFF) * 3 + (p1 shr 8 and 0xFF) * 6 + (p1 and 0xFF)) / 10

        val p2 = bitmap.getPixel(x, y2)
        lum2[x] = ((p2 shr 16 and 0xFF) * 3 + (p2 shr 8 and 0xFF) * 6 + (p2 and 0xFF)) / 10
    }

    for (dx in -range..range) {
        var diff = 0L
        var count = 0
        for (x in margin until (w - margin)) {
            val x2 = x + dx
            if (x2 in 0 until w) {
                diff += kotlin.math.abs(lum1[x] - lum2[x2])
                count++
            }
        }
        if (count > 0) {
            val avgDiff = diff / count
            if (avgDiff < minDiff) {
                minDiff = avgDiff
                bestDx = dx
            }
        }
    }

    val skew = -(bestDx / 10f) * 0.22f
    return skew.coerceIn(-0.25f, 0.25f)
}