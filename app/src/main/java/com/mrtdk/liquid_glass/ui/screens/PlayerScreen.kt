package com.mrtdk.liquid_glass.ui.screens



import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.positionInRoot

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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import androidx.compose.ui.input.pointer.PointerEventPass

import androidx.compose.foundation.gestures.awaitFirstDown

import androidx.compose.foundation.gestures.waitForUpOrCancellation

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent

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

@androidx.compose.runtime.Immutable
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

@androidx.compose.runtime.Immutable
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
    val path = remember { Path() }
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        path.rewind()
        path.moveTo(w * 0.25f, h * 0.75f)
        path.lineTo(w * 0.75f, h * 0.25f)
        path.lineTo(w * 0.5f, h * 0.05f)
        path.lineTo(w * 0.5f, h * 0.95f)
        path.lineTo(w * 0.75f, h * 0.75f)
        path.lineTo(w * 0.25f, h * 0.25f)

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
    val isLightweight = com.mrtdk.glass.LocalLightweightGlass.current

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
                        if (!isLightweight) {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(24f.dp.toPx(), 24f.dp.toPx())
                        } else {
                            blur(2f.dp.toPx())
                        }
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

@Composable
fun AutomixIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 1.6.dp.toPx()
        val radius = this.size.height * 0.28f
        val centerY = this.size.height / 2f
        val leftCenterX = this.size.width * 0.38f
        val rightCenterX = this.size.width * 0.62f

        // Left circle: subtle thin outline
        drawCircle(
            color = tint.copy(alpha = 0.45f),
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(leftCenterX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // Right circle: bold ring with center fill
        drawCircle(
            color = tint,
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(rightCenterX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth * 2.2f)
        )
        drawCircle(
            color = tint,
            radius = radius * 0.45f,
            center = androidx.compose.ui.geometry.Offset(rightCenterX, centerY)
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

@Composable

fun PlayerScreen(

    playerState: PlayerState?,

    isVisible: Boolean,

    isPlaying: Boolean,

    currentPosition: Long = 0L,

    musicPlayer: com.mrtdk.liquid_glass.playback.MusicPlayer? = null,

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
    onQueueChange: ((List<QueueItem>) -> Unit)? = null,

    shuffleModeEnabled: Boolean = false,

    repeatMode: Int = androidx.media3.common.Player.REPEAT_MODE_OFF,

    onToggleShuffle: () -> Unit = {},

    onToggleRepeat: () -> Unit = {},

    onDominantColorChanged: (Color) -> Unit = {},

    playbackError: String? = null,

    onClearPlaybackError: () -> Unit = {},

    onToggleAutoplay: (() -> Unit)? = null

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

        var isQueueItemDragging by remember { mutableStateOf(false) }

        LaunchedEffect(showQueue) {
            if (!showQueue) {
                isQueueItemDragging = false
            }
        }



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
        var openDirectlyInProvidersView by remember { mutableStateOf(false) }
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

        var isAutoMixing by remember { mutableStateOf(com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutoMixing) }
        DisposableEffect(Unit) {
            val listener: (Boolean) -> Unit = { isAutoMixing = it }
            com.mrtdk.liquid_glass.playback.PlaybackQueue.onAutoMixTransitionChanged = listener
            onDispose {
                if (com.mrtdk.liquid_glass.playback.PlaybackQueue.onAutoMixTransitionChanged == listener) {
                    com.mrtdk.liquid_glass.playback.PlaybackQueue.onAutoMixTransitionChanged = null
                }
            }
        }



        var lyricsLines by remember { mutableStateOf<List<com.mocharealm.accompanist.lyrics.core.model.ISyncedLine>?>(null) }
        var bitChordLyrics by remember { mutableStateOf<List<com.mrtdk.liquid_glass.data.lyrics.LyricLine>?>(null) }
        var isLyricsLoading by remember { mutableStateOf(false) }
        var isLyricsNotFound by remember { mutableStateOf(false) }

        var showManualLyricsSearch by remember { mutableStateOf(false) }
        var manualLyricsQueryTitle by remember { mutableStateOf(playerState?.title ?: "") }
        var manualLyricsQueryArtist by remember { mutableStateOf(playerState?.artist ?: "") }

        var selectedLyricsProvider by remember { mutableStateOf("Auto") }
        var currentLyricsProviderName by remember { mutableStateOf("") }
        var currentLyricsSyncType by remember { mutableStateOf("line") }
        var availableLyricsProviders by remember { mutableStateOf<List<com.mrtdk.liquid_glass.utils.LyricsFetchResult>>(emptyList()) }
        var currentLyricsProviderIndex by remember { mutableStateOf(0) }

        var isRomajiEnabled by remember { mutableStateOf(false) }

        var isAutomixEnabled by remember {
            mutableStateOf(
                com.mrtdk.liquid_glass.data.LibraryManager.getString("automix_enabled", "true") == "true"
            )
        }

        val onToggleAutomix: () -> Unit = {
            val next = !isAutomixEnabled
            isAutomixEnabled = next
            com.mrtdk.liquid_glass.data.LibraryManager.saveString("automix_enabled", next.toString())
            com.mrtdk.liquid_glass.playback.PlaybackQueue.isAutomixEnabled = next
            android.widget.Toast.makeText(
                context,
                if (next) "AutoMix activado" else "AutoMix desactivado",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

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

        LaunchedEffect(playerState?.title, playerState?.artist, selectedLyricsProvider, lyricsReloadTrigger) {
            val videoId = playerState?.videoId ?: ""
            val songTitle = playerState?.title
            val songArtist = playerState?.artist

            if (playerState != null && songTitle != null && songArtist != null) {
                bitChordLyrics = null
                lyricsLines = null
                isLyricsLoading = true
                isLyricsNotFound = false

                launch(kotlinx.coroutines.Dispatchers.IO) {
                    val durMs = if (duration > 0) duration else (playerState.duration ?: 0L)
                    val result = com.mrtdk.liquid_glass.data.lyrics.LyricsRepository.lyrics(
                        videoId = videoId,
                        title = songTitle,
                        artist = songArtist,
                        durationMs = durMs,
                        album = playerState.album,
                        prioritizeSyllableSync = true
                    )
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (result != null && result.lines.isNotEmpty()) {
                            bitChordLyrics = result.lines
                            lyricsLines = result.lines.map { com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine(it.text, null, it.timeMs.toInt(), (it.timeMs + 5000L).toInt()) }
                            currentLyricsProviderName = result.source.name
                            currentLyricsSyncType = if (result.lines.any { it.isWordSynced }) "syllable" else "line"
                            isLyricsLoading = false
                            isLyricsNotFound = false
                        } else {
                            bitChordLyrics = emptyList()
                            lyricsLines = emptyList()
                            isLyricsLoading = false
                            isLyricsNotFound = true
                        }
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



        var animatedArtworkUrl by remember(playerState?.artist, playerState?.title, playerState?.album) {
            val artist = playerState?.artist
            val title = playerState?.title
            val album = playerState?.album
            val cached = if (!artist.isNullOrBlank() && !title.isNullOrBlank()) {
                com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.getForSong(artist, title, album)
            } else null
            mutableStateOf(cached)
        }

        var isVideoPlaying by remember { mutableStateOf(false) }
        var coverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        var motionCoverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        var accordBackdropBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        var lyricsBackdropBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        var hasGeneratedMotionBackdrop by remember(playerState?.artist, playerState?.title) { mutableStateOf(false) }
        var frameToken by remember { mutableStateOf(0L) }
        var lastColorSampleTime by remember { mutableLongStateOf(0L) }
        var reflectionSkew by remember { mutableStateOf(0.12f) }
        var masterAnimatedPlayer by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }
        val fullArtworkBackdropStyle by LibraryManager.fullArtworkBackdropStyle.collectAsState()
        val isUltraPerformance by LibraryManager.ultraPerformanceMode.collectAsState()

        LaunchedEffect(playerState?.artist, playerState?.title, playerState?.album) {
            val artist = playerState?.artist
            val title = playerState?.title
            val album = playerState?.album
            isVideoPlaying = false
            motionCoverBitmap = null
            if (!isUltraPerformance) {
                coverBitmap = null
            }
            hasGeneratedMotionBackdrop = false
            frameToken++

            if (artist.isNullOrBlank() || title.isNullOrBlank()) return@LaunchedEffect
            if (animatedArtworkUrl != null) return@LaunchedEffect

            withContext(Dispatchers.IO) {
                // Unified Echo-Music Canvas Provider for Songs (EchoMusic, Tidal, AppleMusic, ArchiveTune)
                val foundUrl = com.mrtdk.liquid_glass.canvas.UnifiedCanvasProvider.getSongCanvas(
                    songTitle = title,
                    artist = artist,
                    album = album
                )

                if (foundUrl != null) {
                    withContext(Dispatchers.Main) {
                        animatedArtworkUrl = foundUrl
                        com.mrtdk.liquid_glass.ui.components.AnimatedArtworkCache.putForSong(artist, title, album, foundUrl)
                    }
                }
            }
        }



        LaunchedEffect(hdArtUrl, playerState?.title, playerState?.artist) {
            motionCoverBitmap = null
            if (!isUltraPerformance) {
                coverBitmap = null
            }
            frameToken++
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
                                val w = bitmap.width
                                val h = bitmap.height
                                val bottomPixels = IntArray(w)
                                bitmap.getPixels(bottomPixels, 0, w, 0, h - 1, w, 1)
                                var r = 0L; var g = 0L; var b = 0L
                                val stepX = maxOf(1, w / 16)
                                var countX = 0
                                for (x in 0 until w step stepX) {
                                    val pixel = bottomPixels[x]
                                    r += android.graphics.Color.red(pixel)
                                    g += android.graphics.Color.green(pixel)
                                    b += android.graphics.Color.blue(pixel)
                                    countX++
                                }
                                val avgColor = Color((r / countX).toInt(), (g / countX).toInt(), (b / countX).toInt())

                                val rightPixels = IntArray(h)
                                bitmap.getPixels(rightPixels, 0, 1, w - 1, 0, 1, h)
                                var rRight = 0L; var gRight = 0L; var bRight = 0L
                                val stepY = maxOf(1, h / 16)
                                var countY = 0
                                for (y in 0 until h step stepY) {
                                    val pixel = rightPixels[y]
                                    rRight += android.graphics.Color.red(pixel)
                                    gRight += android.graphics.Color.green(pixel)
                                    bRight += android.graphics.Color.blue(pixel)
                                    countY++
                                }
                                val rightColor = Color((rRight / countY).toInt(), (gRight / countY).toInt(), (bRight / countY).toInt())

                                val palette = try {
                                    androidx.palette.graphics.Palette.from(bitmap).maximumColorCount(12).generate()
                                } catch (e: Exception) { null }
                                val domRgb = palette?.getDominantColor(android.graphics.Color.DKGRAY)
                                val vibRgb = palette?.getVibrantColor(domRgb ?: android.graphics.Color.DKGRAY)
                                val bestDominant = if (vibRgb != null && vibRgb != android.graphics.Color.DKGRAY) {
                                    Color(vibRgb)
                                } else if (domRgb != null && domRgb != android.graphics.Color.DKGRAY) {
                                    Color(domRgb)
                                } else {
                                    avgColor
                                }

                                withContext(Dispatchers.Main) {
                                    if ((!isVideoPlaying && animatedArtworkUrl.isNullOrBlank()) || fullArtworkBackdropStyle == "accord") {
                                        coverBitmap = asComposeBmp
                                        frameToken++
                                    }
                                    reflectionSkew = skew
                                    bottomAverageColor = avgColor
                                    dominantColor = bestDominant
                                    onDominantColorChanged(bestDominant)
                                    rightSideAverageColor = rightColor
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    if (!isVideoPlaying && animatedArtworkUrl.isNullOrBlank()) {
                                        coverBitmap = asComposeBmp
                                        frameToken++
                                    }
                                    reflectionSkew = skew
                                }
                            }
                        }
                    }
                }
            }

        }

        // Generar fondo difuminado estático para la vista de letras y cola de reproducción (estilo Apple Music, 100% estático)
        LaunchedEffect(coverBitmap, hdArtUrl, playerState?.artUrl) {
            val src = coverBitmap?.asAndroidBitmap()
            if (src != null && !src.isRecycled) {
                lyricsBackdropBitmap = com.mrtdk.liquid_glass.ui.components.AccordBackdropGenerator.generateLyricsBlurredBackdrop(src)
            } else {
                val urlStr = (hdArtUrl ?: playerState?.artUrl)?.toString()
                if (!urlStr.isNullOrBlank()) {
                    withContext(Dispatchers.IO) {
                        try {
                            val req = coil.request.ImageRequest.Builder(context)
                                .data(urlStr)
                                .allowHardware(false)
                                .size(160)
                                .build()
                            val res = coil.Coil.imageLoader(context).execute(req)
                            if (res is coil.request.SuccessResult) {
                                val bmp = (res.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                if (bmp != null && !bmp.isRecycled) {
                                    val generated = com.mrtdk.liquid_glass.ui.components.AccordBackdropGenerator.generateLyricsBlurredBackdrop(bmp)
                                    withContext(Dispatchers.Main) {
                                        lyricsBackdropBitmap = generated
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        

        // En las vistas de letras y cola el fondo fluido es oscuro: se fuerza el
        // color original blanco y se conserva el adaptativo solo en la vista principal.
        val isOverlayView = showLyrics || showQueue
        val isLightBackground = if (isOverlayView) {
            false
        } else run {
            val r = bottomAverageColor.red
            val g = bottomAverageColor.green
            val b = bottomAverageColor.blue
            val maxCh = maxOf(r, g, b)
            val minCh = minOf(r, g, b)
            bottomAverageColor.luminance() > 0.90f && (maxCh - minCh) < 0.12f && minCh > 0.85f
        }

        val contentColor = if (isLightBackground) Color(0xFF1A1A1A) else Color.White
        val sliderActiveColor = if (isLightBackground) Color(0xFF1A1A1A) else Color(0xFFE5E5EA)
        val sliderInactiveColor = if (isLightBackground) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.18f)



        val animatedImageLoader = remember(context) {
            coil.Coil.imageLoader(context)
        }



        val isSaved by androidx.compose.runtime.produceState(initialValue = false, playerState?.videoId) {
            LibraryManager.savedItems.collect { list ->
                value = list.any { it.id == playerState?.videoId }
            }
        }



        val isOverlayActive = showLyrics || showQueue
        val density = androidx.compose.ui.platform.LocalDensity.current
        val screenHeightPx = remember(context) { context.resources.displayMetrics.heightPixels.toFloat() }
        GlassContainer(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val currentDragY = dragOffsetY.value
                    translationY = currentDragY
                    val dragFraction = if (screenHeightPx > 0f) (currentDragY / screenHeightPx).coerceIn(0f, 1f) else 0f
                    val cornerRadiusPx = if (!isOverlayActive) {
                        val maxCorner = 36.dp.toPx()
                        (dragFraction * 4f).coerceIn(0f, 1f) * maxCorner
                    } else 0f

                    shape = RoundedCornerShape(
                        topStart = cornerRadiusPx,
                        topEnd = cornerRadiusPx,
                        bottomStart = 0f,
                        bottomEnd = 0f
                    )
                    clip = true
                    shadowElevation = if (dragFraction > 0f) 28.dp.toPx() else 0f
                }
                .pointerInput(showLyrics, showQueue) {
                    if (!showLyrics && !showQueue) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val currentOffsetY = dragOffsetY.value
                                val thresholdPx = with(density) { 120.dp.toPx() }
                                if (currentOffsetY > thresholdPx) {
                                    scope.launch {
                                        dragOffsetY.animateTo(
                                            targetValue = screenHeightPx,
                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                        )
                                        onClose()
                                    }
                                } else {
                                    scope.launch {
                                        dragOffsetY.animateTo(
                                            0f,
                                            spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
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
                },
            useShader = true,

            content = {

                BoxWithConstraints(

                    modifier = Modifier

                        .fillMaxSize()

                        .onGloballyPositioned { parentCoordinates = it }

                        .let { if (!isUltraPerformance) it.layerBackdrop(localBackdrop) else it }

                ) {

            val maxWidth = maxWidth

            val maxHeight = maxHeight



            val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE



            if (isLandscape) {
                val landscapePosition by if (musicPlayer != null) {
                    musicPlayer.currentPosition.collectAsState()
                } else {
                    androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(currentPosition) }
                }
                LandscapePlayerLayout(
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    playerState = playerState,
                    isPlaying = isPlaying,
                    currentPosition = landscapePosition,
                    duration = duration,
                    upNextSongs = upNextSongs,
                    shuffleModeEnabled = shuffleModeEnabled,
                    repeatMode = repeatMode,
                    showLyrics = showLyrics,
                    showQueue = showQueue,
                    volumePosition = volumePosition,
                    coverBitmap = coverBitmap,
                    hdArtUrl = hdArtUrl,
                    bitChordLyrics = bitChordLyrics,
                    isLyricsLoading = isLyricsLoading,
                    lyricsLines = lyricsLines,
                    isRomajiEnabled = isRomajiEnabled,
                    isSaved = isSaved,
                    animatedArtworkUrl = animatedArtworkUrl,
                    isVideoPlaying = isVideoPlaying,
                    animatedImageLoader = animatedImageLoader,
                    isBottomBarCollapsed = isBottomBarCollapsed,
                    isAutoScrollEnabled = isAutoScrollEnabled,
                    scrollToCurrentTrigger = scrollToCurrentTrigger,
                    lyricsOffset = lyricsOffset,
                    colors = remember(
                        dominantColor, bottomAverageColor, rightSideAverageColor,
                        contentColor, isLightBackground, sliderActiveColor, sliderInactiveColor
                    ) {
                        LandscapePlayerColors(
                            dominantColor = dominantColor,
                            bottomAverageColor = bottomAverageColor,
                            rightSideAverageColor = rightSideAverageColor,
                            contentColor = contentColor,
                            isLightBackground = isLightBackground,
                            sliderActiveColor = sliderActiveColor,
                            sliderInactiveColor = sliderInactiveColor
                        )
                    },
                    callbacks = remember(
                        onUpNextSongsChange, onSkipNext, onSkipPrevious, onClose,
                        onTogglePlayPause, onSeek, onVolumeChange, onArtistSelected,
                        onAlbumSelected, onSongSelected, onSongSelectedFromQueue,
                        onToggleShuffle, onToggleRepeat, isBottomBarCollapsed
                    ) {
                        LandscapePlayerCallbacks(
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
                            onToggleShuffle = onToggleShuffle,
                            onToggleRepeat = onToggleRepeat,
                            onShowLyricsChange = { showLyrics = it },
                            onShowQueueChange = { showQueue = it },
                            onVolumePositionChange = { volumePosition = it },
                            onCoverBitmapChange = { coverBitmap = it },
                            onDominantColorChange = { dominantColor = it },
                            onBottomAverageColorChange = { bottomAverageColor = it },
                            onRightSideAverageColorChange = { rightSideAverageColor = it },
                            onToggleRomaji = { isRomajiEnabled = !isRomajiEnabled },
                            onVideoPlayingChange = { isVideoPlaying = it },
                            onShowOptionsMenu = { bounds -> menuPivotBounds = bounds; showOptionsMenu = true },
                            onShowLyricsMenu = { openDirectlyInProvidersView = false; showLyricsOptionsMenu = true },
                            onShowPlaylistMenu = { showPlaylistMenu = true },
                            onShowArtistMenu = { artists, bounds ->
                                artistMenuOptions = artists
                                artistPivotBounds = bounds
                                showArtistOptionsMenu = true
                            },
                            onAutoScrollChange = { isAutoScrollEnabled = it },
                            isAutomixEnabled = isAutomixEnabled,
                            onToggleAutomix = onToggleAutomix,
                            onToggleAutoplay = onToggleAutoplay
                        )
                    }
                )
            } else {
                val lyricsImageSize = 60.dp

            val isOverlayActive = showLyrics || showQueue

            var draggingSection by remember { mutableStateOf<String?>(null) }
            var draggingIndex by remember { mutableIntStateOf(-1) }
            var targetDropIndex by remember { mutableIntStateOf(-1) }
            var queueDragOffsetY by remember { mutableFloatStateOf(0f) }
            var queueDragOffsetX by remember { mutableFloatStateOf(0f) }
            var listScrollAccumulator by remember { mutableFloatStateOf(0f) }
            var queueViewportHeightPx by remember { mutableFloatStateOf(0f) }
            var queueTouchYInViewport by remember { mutableFloatStateOf(0f) }
            var autoScrollVelocity by remember { mutableFloatStateOf(0f) }
            var draggedItemInitialYInBox by remember { mutableFloatStateOf(0f) }
            var draggedItemTitle by remember { mutableStateOf("") }
            var draggedItemArtist by remember { mutableStateOf("") }
            var draggedItemArtUrl by remember { mutableStateOf<Any?>(null) }
            var playerBoxRootY by remember { mutableFloatStateOf(0f) }

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

            

            val playerArtworkStyle by LibraryManager.playerArtworkStyle.collectAsState()

            val screenWidthPx = with(density) { maxWidth.roundToPx() }
            val screenHeightPx = with(density) { maxHeight.roundToPx() }

            LaunchedEffect(hdArtUrl, playerState?.artUrl, playerState?.title, playerState?.artist, fullArtworkBackdropStyle, screenWidthPx, screenHeightPx, animatedArtworkUrl, isUltraPerformance) {
                if (!isUltraPerformance && fullArtworkBackdropStyle == "accord" && screenWidthPx > 0 && screenHeightPx > 0) {
                    val artModel = hdArtUrl ?: playerState?.artUrl
                    if (artModel != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                val loader = coil.Coil.imageLoader(context)
                                val req = ImageRequest.Builder(context)
                                    .data(artModel)
                                    .allowHardware(false)
                                    .build()
                                val result = (loader.execute(req) as? coil.request.SuccessResult)?.drawable
                                val bmp = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: run {
                                    if (result != null) {
                                        val b = android.graphics.Bitmap.createBitmap(
                                            result.intrinsicWidth.coerceAtLeast(1),
                                            result.intrinsicHeight.coerceAtLeast(1),
                                            android.graphics.Bitmap.Config.ARGB_8888
                                        )
                                        val c = android.graphics.Canvas(b)
                                        result.setBounds(0, 0, c.width, c.height)
                                        result.draw(c)
                                        b
                                    } else null
                                }
                                if (bmp != null && !bmp.isRecycled) {
                                    val generated = com.mrtdk.liquid_glass.ui.components.AccordBackdropGenerator.generateAccordBackdrop(
                                        source = bmp,
                                        width = screenWidthPx,
                                        height = screenHeightPx,
                                        includeCover = false
                                    )
                                    withContext(Dispatchers.Main) {
                                        accordBackdropBitmap = generated
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } else if (fullArtworkBackdropStyle != "accord") {
                    accordBackdropBitmap = null
                }
            }

            val hasAnimatedCover = !animatedArtworkUrl.isNullOrBlank() || isVideoPlaying
            val isNormalArtwork = when (playerArtworkStyle) {
                "normal" -> true
                "animated_fullartwork" -> !hasAnimatedCover
                else -> false
            }

            val controlsBaseY = maxWidth * 1.23f

            val normalCardSize = minOf(maxWidth - 56.dp, (controlsBaseY - 8.dp - 64.dp - 24.dp).coerceAtLeast(220.dp))
            val normalX = (maxWidth - normalCardSize) / 2
            val normalY = 56.dp + ((controlsBaseY - 8.dp - 56.dp - 20.dp) - normalCardSize).coerceAtLeast(0.dp) / 2

            val expandedWidth = if (isNormalArtwork) normalCardSize else maxWidth
            val expandedHeight = if (isNormalArtwork) normalCardSize else (maxWidth * 1.35f)
            val expandedX = if (isNormalArtwork) normalX else 0.dp
            val expandedY = if (isNormalArtwork) normalY else 0.dp
            val defaultCorner = if (isNormalArtwork) 22.dp else 12.dp

            val p = dragProgress.coerceIn(0f, 1f)
            val contentAlpha = if (isOverlayActive) (1f - p * 2.2f).coerceIn(0f, 1f) else 1f
            val overlayAlpha = (1f - p * 2.2f).coerceIn(0f, 1f)

            val startWidth = if (isOverlayActive) lyricsImageSize else expandedWidth
            val startHeight = if (isOverlayActive) lyricsImageSize else expandedHeight
            val startOffsetX = if (isOverlayActive) 24.dp else expandedX
            val startOffsetY = if (isOverlayActive) 64.dp else expandedY
            val startCorner = if (isOverlayActive) 12.dp else defaultCorner

            val imgWidthTarget: androidx.compose.ui.unit.Dp
            val imgHeightTarget: androidx.compose.ui.unit.Dp
            val imgOffsetXTarget: androidx.compose.ui.unit.Dp
            val imgOffsetYTarget: androidx.compose.ui.unit.Dp
            val imageCornerTarget: androidx.compose.ui.unit.Dp

            if (isOverlayActive) {
                imgWidthTarget = androidx.compose.ui.unit.lerp(startWidth, 40.dp, p)
                imgHeightTarget = androidx.compose.ui.unit.lerp(startHeight, 40.dp, p)
                imgOffsetXTarget = androidx.compose.ui.unit.lerp(startOffsetX, targetOffsetX, p)
                imgOffsetYTarget = androidx.compose.ui.unit.lerp(startOffsetY, targetOffsetY, p)
                imageCornerTarget = androidx.compose.ui.unit.lerp(startCorner, 20.dp, p)
            } else {
                imgWidthTarget = startWidth
                imgHeightTarget = startHeight
                imgOffsetXTarget = startOffsetX
                imgOffsetYTarget = startOffsetY
                imageCornerTarget = startCorner
            }

            val appleOverlaySpringFloat = spring<Float>(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
            )

            val overlayTransitionProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isOverlayActive) 1f else 0f,
                animationSpec = appleOverlaySpringFloat,
                label = "overlayTransitionProgress"
            )

            val appleCardSpringDp = spring<androidx.compose.ui.unit.Dp>(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
            )

            val animatedImgWidth by androidx.compose.animation.core.animateDpAsState(
                targetValue = imgWidthTarget,
                animationSpec = appleCardSpringDp,
                label = "imgWidth"
            )
            val animatedImgHeight by androidx.compose.animation.core.animateDpAsState(
                targetValue = imgHeightTarget,
                animationSpec = appleCardSpringDp,
                label = "imgHeight"
            )
            val animatedImgOffsetX by androidx.compose.animation.core.animateDpAsState(
                targetValue = imgOffsetXTarget,
                animationSpec = appleCardSpringDp,
                label = "imgOffsetX"
            )
            val animatedImgOffsetY by androidx.compose.animation.core.animateDpAsState(
                targetValue = imgOffsetYTarget,
                animationSpec = appleCardSpringDp,
                label = "imgOffsetY"
            )
            val animatedImgCorner by androidx.compose.animation.core.animateDpAsState(
                targetValue = imageCornerTarget,
                animationSpec = appleCardSpringDp,
                label = "imgCorner"
            )

            val imgWidth = if (dragProgress > 0f) imgWidthTarget else animatedImgWidth
            val imgHeight = if (dragProgress > 0f) imgHeightTarget else animatedImgHeight
            val imgOffsetX = if (dragProgress > 0f) imgOffsetXTarget else animatedImgOffsetX
            val imgOffsetY = if (dragProgress > 0f) imgOffsetYTarget else animatedImgOffsetY
            val imgCorner = if (dragProgress > 0f) imageCornerTarget else animatedImgCorner

            val detailsOffsetYTarget = if (isOverlayActive) {
                if (p > 0f) androidx.compose.ui.unit.lerp(startOffsetY + 6.dp, targetOffsetY, p) else (startOffsetY + 6.dp)
            } else {
                if (p > 0f) androidx.compose.ui.unit.lerp(controlsBaseY - 8.dp, targetOffsetY + 4.dp, p) else (controlsBaseY - 8.dp)
            }

            val animatedDetailsOffsetY by androidx.compose.animation.core.animateDpAsState(
                targetValue = detailsOffsetYTarget,
                animationSpec = appleCardSpringDp,
                label = "detailsOffsetY"
            )
            val detailsOffsetY = if (dragProgress > 0f) detailsOffsetYTarget else animatedDetailsOffsetY

            val isAccordActive = fullArtworkBackdropStyle == "accord" && !isNormalArtwork

            val detailsOffsetXTarget = if (isOverlayActive) {
                if (p > 0f) androidx.compose.ui.unit.lerp(startOffsetX + lyricsImageSize + 14.dp, targetOffsetX + 48.dp, p) else (startOffsetX + lyricsImageSize + 14.dp)
            } else {
                if (p > 0f) androidx.compose.ui.unit.lerp(34.dp, targetOffsetX + 48.dp, p) else 34.dp
            }

            val animatedDetailsOffsetX by androidx.compose.animation.core.animateDpAsState(
                targetValue = detailsOffsetXTarget,
                animationSpec = appleCardSpringDp,
                label = "detailsOffsetX"
            )
            val detailsOffsetX = if (dragProgress > 0f) detailsOffsetXTarget else animatedDetailsOffsetX

            val detailsWidthTarget = if (isOverlayActive) (maxWidth - (startOffsetX + lyricsImageSize + 14.dp) - 20.dp) else (maxWidth - 68.dp)

            val animatedDetailsWidth by androidx.compose.animation.core.animateDpAsState(
                targetValue = detailsWidthTarget,
                animationSpec = appleCardSpringDp,
                label = "detailsWidth"
            )
            val detailsWidth = if (dragProgress > 0f) detailsWidthTarget else animatedDetailsWidth

            val titleFontSizeTarget = if (isOverlayActive) 17f else 21f
            val titleFontSizeFloat by androidx.compose.animation.core.animateFloatAsState(
                targetValue = titleFontSizeTarget,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                label = "titleFontSize"
            )
            val titleFontSize = titleFontSizeFloat.sp

            val artistFontSizeTarget = if (isOverlayActive) 14f else 16f
            val artistFontSizeFloat by androidx.compose.animation.core.animateFloatAsState(
                targetValue = artistFontSizeTarget,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                label = "artistFontSize"
            )
            val artistFontSize = artistFontSizeFloat.sp

            val starIconSizeTarget = if (isOverlayActive) 20.dp else 22.dp
            val starIconSize by androidx.compose.animation.core.animateDpAsState(starIconSizeTarget, label = "starIconSize")

            val moreIconSizeTarget = if (isOverlayActive) 18.dp else 18.dp
            val moreIconSize by androidx.compose.animation.core.animateDpAsState(moreIconSizeTarget, label = "moreIconSize")



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



            val normalTopColor = dominantColor.copy(alpha = 1.0f)
            val normalMidColor = bottomAverageColor.copy(alpha = 1.0f)
            val normalBottomColor = Color(
                red = (bottomAverageColor.red * 0.50f + dominantColor.red * 0.20f).coerceIn(0f, 1f),
                green = (bottomAverageColor.green * 0.50f + dominantColor.green * 0.20f).coerceIn(0f, 1f),
                blue = (bottomAverageColor.blue * 0.50f + dominantColor.blue * 0.20f).coerceIn(0f, 1f),
                alpha = 1.0f
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isNormalArtwork) {
                                listOf(
                                    normalTopColor,
                                    normalMidColor,
                                    normalBottomColor
                                )
                            } else if (fullArtworkBackdropStyle == "accord") {
                                listOf(
                                    dominantColor.copy(alpha = 1.0f),
                                    bottomAverageColor.copy(alpha = 0.95f),
                                    Color(
                                        red = (bottomAverageColor.red * 0.70f + dominantColor.red * 0.30f).coerceIn(0f, 1f),
                                        green = (bottomAverageColor.green * 0.70f + dominantColor.green * 0.30f).coerceIn(0f, 1f),
                                        blue = (bottomAverageColor.blue * 0.70f + dominantColor.blue * 0.30f).coerceIn(0f, 1f),
                                        alpha = 1.0f
                                    )
                                )
                            } else {
                                listOf(
                                    dominantColor.copy(alpha = 1.0f),
                                    Color(0xFF16181B),
                                    Color(0xFF101113)
                                )
                            }
                        )
                    )
                    .onGloballyPositioned { coordinates ->
                        playerBoxRootY = coordinates.positionInRoot().y
                    }
            ) {

            // Capa Fondo Ultra Rendimiento (Gradiente nativo por hardware directo, 0ms CPU / 0ms GPU)
            if (isUltraPerformance && !isNormalArtwork) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    dominantColor.copy(alpha = 0.82f),
                                    dominantColor.copy(alpha = 0.45f),
                                    Color.Black
                                )
                            )
                        )
                )
            } else if (fullArtworkBackdropStyle == "accord" && !isNormalArtwork) {
                val accordBmp = accordBackdropBitmap
                val backdropAlpha by animateFloatAsState(
                    targetValue = if (accordBmp != null) 1f else 0f,
                    animationSpec = tween(220),
                    label = "accordBackdropAlpha"
                )
                if (backdropAlpha > 0f) {
                    androidx.compose.animation.Crossfade(
                        targetState = accordBmp,
                        animationSpec = tween(350),
                        label = "accordCrossfade"
                    ) { currentBmp ->
                        if (currentBmp != null) {
                            Image(
                                bitmap = currentBmp,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = backdropAlpha
                                    }
                            )
                        }
                    }

                    // Capa Contrast Scrim adaptativa de Accord 2.0 para oscurecer fondos claros (asegura texto y controles nítidos)
                    val contrastAlpha = com.mrtdk.liquid_glass.ui.components.AccordBackdropGenerator.lastContrastScrimAlpha
                    if (contrastAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = contrastAlpha * backdropAlpha
                                }
                                .background(Color.Black)
                        )
                    }
                }
            }

            // Capa 4: Reflejo invertido estilo Apple Music (solo para fullartwork y cuando NO es modo Accord NI Ultra Rendimiento)
            val mirrorArtModel = hdArtUrl ?: playerState?.artUrl
            if (!isUltraPerformance && !isNormalArtwork && (coverBitmap != null || mirrorArtModel != null) && dragProgress < 1f && overlayTransitionProgress < 0.99f && fullArtworkBackdropStyle != "accord") {
                val reflectionWidth = maxWidth
                val reflectionX = 0.dp
                val childWidth = expandedWidth
                val childOffsetX = expandedX
                val reflectionOverlap = 32.dp
                val baseReflectionY = (expandedY + expandedHeight) - reflectionOverlap
                val reflectionY = baseReflectionY
                val reflectionHeight = (maxHeight - baseReflectionY).coerceAtLeast(expandedHeight)
                
                val verticalScale = -4.0f
                val pivotY = 0f

                Box(
                    modifier = Modifier
                        .offset(x = reflectionX, y = reflectionY)
                        .width(reflectionWidth)
                        .height(reflectionHeight)
                        .clipToBounds()
                ) {
                    val currentBitmap = if (!animatedArtworkUrl.isNullOrBlank()) {
                        if (isVideoPlaying) coverBitmap else null
                    } else {
                        coverBitmap
                    }
                    val mirrorModel = if (!animatedArtworkUrl.isNullOrBlank()) {
                        if (isVideoPlaying && coverBitmap != null) coverBitmap else mirrorArtModel
                    } else {
                        mirrorArtModel ?: currentBitmap
                    }

                        Box(
                            modifier = Modifier
                                .offset(x = childOffsetX, y = 0.dp)
                                .width(childWidth)
                                .height(expandedHeight)
                                .graphicsLayer {
                                    alpha = (1f - overlayTransitionProgress)
                                }
                        ) {
                            // Reflejo invertido con difuminado horizontal progresivo
                            com.mrtdk.liquid_glass.ui.components.GraduatedBlurArtwork(
                                imageUrl = currentBitmap ?: mirrorModel,
                                videoUrl = animatedArtworkUrl,
                                modifier = Modifier.fillMaxSize(),
                                mildBlurRadiusX = 40.dp,
                                mildBlurRadiusY = 14.dp,
                                strongBlurRadiusX = 180.dp,
                                strongBlurRadiusY = 55.dp,
                                sliderThresholdDp = 50.dp,
                                verticalScale = verticalScale,
                                pivotY = 0f,
                                horizontalScale = 1.0f,
                                frameToken = frameToken,
                                syncWithPlayer = masterAnimatedPlayer,
                                imageLoader = animatedImageLoader
                            )
                        }
                    }
                }


            // LYRICS / QUEUE OVERLAY (Synchronized with artwork card spring)
            if (showLyrics || showQueue || overlayTransitionProgress > 0.005f) {
                 Box(
                     modifier = Modifier
                         .fillMaxSize()
                         .graphicsLayer {
                             alpha = (overlayAlpha * overlayTransitionProgress).coerceIn(0f, 1f)
                             translationY = with(density) { (80.dp * (1f - overlayTransitionProgress)).toPx() }
                             compositingStrategy = CompositingStrategy.ModulateAlpha
                         }
                 ) {
                     val secCol = if (rightSideAverageColor != Color.Transparent && rightSideAverageColor != dominantColor) {
                         rightSideAverageColor
                     } else {
                         bottomAverageColor
                     }
                     val fluidPrimary = if (isNormalArtwork) normalTopColor else dominantColor
                     val fluidSecondary = if (isNormalArtwork) normalMidColor else secCol
                     val fluidAccent = if (isNormalArtwork) normalBottomColor else bottomAverageColor

                      // Fondo de formas estáticas (sin movimiento) para letras y cola: ahorra recursos.
                      if (!isUltraPerformance && fullArtworkBackdropStyle != "accord") {
                          com.mrtdk.liquid_glass.ui.components.RayMusicStaticFluidBackground(
                              primaryColor = fluidPrimary,
                              secondaryColor = fluidSecondary,
                              accentColor = fluidAccent,
                              modifier = Modifier.fillMaxSize()
                          )
                     } else if (isUltraPerformance) {
                         Box(
                             modifier = Modifier
                                 .fillMaxSize()
                                 .background(
                                     Brush.verticalGradient(
                                         listOf(
                                             fluidPrimary.copy(alpha = 0.85f),
                                             Color.Black
                                         )
                                     )
                                 )
                         )
                     }
                 }

                      // Height of the content area: terminates ~5px (6dp) right above the seekbar
                      val seekbarTopFromBottom = (maxHeight - (controlsBaseY + 72.dp)).coerceAtLeast(140.dp)
                      val overlayBottomPadding by animateDpAsState(
                          targetValue = if (showLyrics && !showLyricsControls) 0.dp else (seekbarTopFromBottom + 6.dp),
                          animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                          label = "overlayBottomPadding"
                      )

                       Column(
                           modifier = Modifier
                               .fillMaxSize()
                               .padding(bottom = overlayBottomPadding.coerceAtLeast(0.dp))
                               .clipToBounds()
                       ) {

                       Spacer(modifier = Modifier.height(148.dp))
                       
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

                            val automixInteraction = remember { MutableInteractionSource() }
                            val isAutomixActive = isAutomixEnabled
                            val isAutomixPressed by automixInteraction.collectIsPressedAsState()
                            val automixScale by animateFloatAsState(targetValue = if (isAutomixPressed) 0.85f else 1.0f, label = "automixScale")
                            val automixBgColor by animateColorAsState(targetValue = if (isAutomixActive) activeBg else contentColor.copy(alpha=0.15f), label = "automixBg")
                            val automixIconColor by animateColorAsState(targetValue = if (isAutomixActive) activeIcon else contentColor.copy(alpha=0.5f), label = "automixIcon")

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
                                    Icon(painterResource(id = R.drawable.shuffle), "Shuffle", tint = shuffleIconColor, modifier = Modifier.size(36.dp))
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
                                    Icon(repeatIcon, "Repeat", tint = repeatIconColor, modifier = Modifier.size(24.dp))
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
                                                if (onToggleAutoplay != null) {
                                                    onToggleAutoplay()
                                                } else {
                                                    val newExclusive = !playerState.isExclusiveQueue
                                                    com.mrtdk.liquid_glass.playback.PlaybackQueue.isExclusiveQueue = newExclusive
                                                }
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AllInclusive, "Autoplay", tint = autoplayIconColor, modifier = Modifier.size(24.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(scaleX = automixScale, scaleY = automixScale)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(automixBgColor)
                                        .clickable(
                                            interactionSource = automixInteraction,
                                            indication = null,
                                            onClick = onToggleAutomix
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AutomixIcon(tint = automixIconColor, size = 24.dp)
                                }
                            }

                            val albumSubtitle = playerState?.album?.takeIf { it.isNotBlank() }?.let { "De $it" }
                                ?: (playerState?.artist?.takeIf { it.isNotBlank() }?.let { "De $it" } ?: stringResource(R.string.autoplaying_similar_music))

                            Column(modifier = Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp, bottom = 4.dp)) {
                                Text(
                                    text = if (playerState != null && playerState.queue.isNotEmpty()) stringResource(R.string.siguiente_en_album_playlist) else stringResource(R.string.continue_playing),
                                    color = contentColor,
                                    fontSize = 17.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = albumSubtitle,
                                    color = contentColor.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                                             val queueDensity = androidx.compose.ui.platform.LocalDensity.current
                                             val queueRowHeightPx = with(queueDensity) { 62.dp.toPx() }
                                             val queueHaptic = LocalHapticFeedback.current

                                              // Auto-scroll continuo y suave a 60fps mientras se mantenga cerca de los bordes
                                              LaunchedEffect(isQueueItemDragging, autoScrollVelocity) {
                                                  if (!isQueueItemDragging || autoScrollVelocity == 0f) return@LaunchedEffect
                                                  while (isQueueItemDragging && autoScrollVelocity != 0f) {
                                                      val canScroll = if (autoScrollVelocity > 0f) queueListState.canScrollForward else queueListState.canScrollBackward
                                                      if (!canScroll) {
                                                          kotlinx.coroutines.delay(20L)
                                                          continue
                                                      }

                                                      val consumed = queueListState.scrollBy(autoScrollVelocity)
                                                      if (consumed == 0f) {
                                                          kotlinx.coroutines.delay(20L)
                                                          continue
                                                      }
                                                      listScrollAccumulator += consumed

                                                      val section = draggingSection
                                                      val curr = draggingIndex
                                                      val totalEffectiveOffset = queueDragOffsetY + listScrollAccumulator
                                                      if (section == "queue" && playerState != null && curr in playerState.queue.indices) {
                                                          val qSize = playerState.queue.size
                                                          val itemsMoved = (totalEffectiveOffset / queueRowHeightPx).roundToInt()
                                                          val newTarget = (curr + itemsMoved).coerceIn(0, qSize - 1)
                                                          if (newTarget != targetDropIndex) {
                                                              targetDropIndex = newTarget
                                                              queueHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                          }
                                                      } else if (section == "up_next" && curr in upNextSongs.indices) {
                                                          val unSize = upNextSongs.size
                                                          val itemsMoved = (totalEffectiveOffset / queueRowHeightPx).roundToInt()
                                                          val newTarget = (curr + itemsMoved).coerceIn(0, unSize - 1)
                                                          if (newTarget != targetDropIndex) {
                                                              targetDropIndex = newTarget
                                                              queueHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                          }
                                                      }

                                                      kotlinx.coroutines.delay(16L)
                                                  }
                                              }

                               Box(
                                   modifier = Modifier
                                       .weight(1f)
                                       .fillMaxWidth()
                                       .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                       .drawWithContent {
                                           drawContent()
                                           val bottomFadePx = with(density) { 16.dp.toPx() }
                                           val h = size.height
                                           val bottomFadeStart = if (h > bottomFadePx) (h - bottomFadePx) / h else 0.96f
                                           drawRect(
                                               brush = Brush.verticalGradient(
                                                   colorStops = arrayOf(
                                                       0.0f to Color.Transparent,
                                                       0.03f to Color.Black,
                                                       bottomFadeStart to Color.Black,
                                                       1.0f to Color.Transparent
                                                   )
                                               ),
                                               blendMode = BlendMode.DstIn
                                           )
                                       }
                                       .onGloballyPositioned { coordinates ->
                                           queueViewportHeightPx = coordinates.size.height.toFloat()
                                       }
                               ) {
                               LazyColumn(
                                   state = queueListState, 
                                   userScrollEnabled = !isQueueItemDragging,
                                   modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), 
                                   contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                                   verticalArrangement = Arrangement.spacedBy(4.dp)
                               ) {
                                 // 1. Manual Queue Section (Album/Playlist)
                                 if (playerState != null && playerState.queue.isNotEmpty()) {
                                      val state = playerState
                                      items(
                                          count = state.queue.size,
                                          key = { index -> "${state.queue[index].videoId ?: "q"}_$index" },
                                          contentType = { "queue_item" }
                                      ) { index ->
                                           val qItem = state.queue[index]
                                           val isCurrent = state.videoId != null && qItem.videoId == state.videoId
                                           val isThisDragging = draggingSection == "queue" && draggingIndex == index

                                           val targetShift = when {
                                               draggingSection != "queue" || draggingIndex == -1 -> 0f
                                               isThisDragging -> 0f
                                               draggingIndex < targetDropIndex && index > draggingIndex && index <= targetDropIndex -> -queueRowHeightPx
                                               draggingIndex > targetDropIndex && index < draggingIndex && index >= targetDropIndex -> queueRowHeightPx
                                               else -> 0f
                                           }
                                           val animatedShiftY by animateFloatAsState(
                                               targetValue = targetShift,
                                               animationSpec = spring(
                                                   dampingRatio = Spring.DampingRatioLowBouncy,
                                                   stiffness = Spring.StiffnessMediumLow
                                               ),
                                               label = "shift_q_$index"
                                           )

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
                                                   val remaining = state.queue.toMutableList().apply {
                                                       if (index in indices) removeAt(index)
                                                   }
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
                                                 titleFontSize = 15.sp,
                                                 artistFontSize = 13.sp,
                                                 onClick = onRowClick,
                                                 isDragging = isThisDragging,
                                                 isAnyDragging = isQueueItemDragging,
                                                 dragTranslationX = if (isThisDragging) queueDragOffsetX else 0f,
                                                 dragTranslationY = if (isThisDragging) queueDragOffsetY else animatedShiftY,
                                                 onDragStart = { startY, globalY ->
                                                      draggingSection = "queue"
                                                      draggingIndex = index
                                                      targetDropIndex = index
                                                      queueDragOffsetY = 0f
                                                      queueDragOffsetX = 0f
                                                      listScrollAccumulator = 0f
                                                      draggedItemInitialYInBox = globalY - playerBoxRootY
                                                      draggedItemTitle = qItem.title
                                                      draggedItemArtist = qItem.artist
                                                      draggedItemArtUrl = qItem.artUrl
                                                      queueTouchYInViewport = startY
                                                      autoScrollVelocity = 0f
                                                      isQueueItemDragging = true
                                                  },
                                                 onDragDelta = { dx, dy ->
                                                     queueDragOffsetY += dy
                                                     queueDragOffsetX = (queueDragOffsetX + dx * 0.40f).coerceIn(-48f, 48f)
                                                     queueTouchYInViewport += dy

                                                     val qSize = state.queue.size
                                                     val totalEffectiveOffset = queueDragOffsetY + listScrollAccumulator
                                                     val itemsMoved = (totalEffectiveOffset / queueRowHeightPx).roundToInt()
                                                     val newTarget = (draggingIndex + itemsMoved).coerceIn(0, qSize - 1)
                                                     if (newTarget != targetDropIndex) {
                                                         targetDropIndex = newTarget
                                                         queueHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                     }

                                                     val topZone = queueRowHeightPx * 1.5f
                                                     val bottomZone = queueViewportHeightPx - (queueRowHeightPx * 1.5f)
                                                     autoScrollVelocity = when {
                                                         queueViewportHeightPx <= 0f -> 0f
                                                         queueTouchYInViewport < topZone -> {
                                                             val factor = ((topZone - queueTouchYInViewport) / topZone).coerceIn(0f, 1f)
                                                             -(8f + factor * 22f)
                                                         }
                                                         queueTouchYInViewport > bottomZone -> {
                                                             val factor = ((queueTouchYInViewport - bottomZone) / (queueViewportHeightPx - bottomZone).coerceAtLeast(1f)).coerceIn(0f, 1f)
                                                             (8f + factor * 22f)
                                                         }
                                                         else -> 0f
                                                     }
                                                 },
                                                 onDragEnd = {
                                                     val from = draggingIndex
                                                     val to = targetDropIndex
                                                     if (from != -1 && to != -1 && from != to && from in state.queue.indices && to in state.queue.indices) {
                                                         val mutable = state.queue.toMutableList()
                                                         val item = mutable.removeAt(from)
                                                         mutable.add(to, item)
                                                         if (onQueueChange != null) onQueueChange(mutable) else com.mrtdk.liquid_glass.playback.PlaybackQueue.queue = mutable
                                                     }
                                                     draggingSection = null
                                                     draggingIndex = -1
                                                     targetDropIndex = -1
                                                     queueDragOffsetY = 0f
                                                     queueDragOffsetX = 0f
                                                     listScrollAccumulator = 0f
                                                     autoScrollVelocity = 0f
                                                     isQueueItemDragging = false
                                                 }
                                            )
                                       }
                                  }

                                 // 2. Up Next / Autoplay Section
                                 if (upNextSongs.isNotEmpty()) {
                                      val state = playerState
                                      itemsIndexed(
                                          items = upNextSongs,
                                          key = { _, song -> song.id },
                                          contentType = { _, _ -> "up_next_item" }
                                      ) { i, song ->
                                          val isCurrent = state != null && song.id == state.videoId
                                          val isThisDragging = draggingSection == "up_next" && draggingIndex == i

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
                                                   val remaining = upNextSongs.toMutableList().apply {
                                                       if (i in indices) removeAt(i)
                                                   }
                                                   onUpNextSongsChange(remaining)
                                                   onSongSelectedFromQueue(PlayerState(
                                                       title = song.title,
                                                       artist = song.artists.joinToString { it.name },
                                                       artUrl = upgradedArt,
                                                       videoId = song.id,
                                                       queue = state?.queue ?: emptyList(),
                                                       isExclusiveQueue = state?.isExclusiveQueue ?: false,
                                                       album = song.album?.name,
                                                       albumId = song.album?.id
                                                   ))
                                               }
                                           }

                                           val targetShift = when {
                                               draggingSection != "up_next" || draggingIndex == -1 -> 0f
                                               isThisDragging -> 0f
                                               draggingIndex < targetDropIndex && i > draggingIndex && i <= targetDropIndex -> -queueRowHeightPx
                                               draggingIndex > targetDropIndex && i < draggingIndex && i >= targetDropIndex -> queueRowHeightPx
                                               else -> 0f
                                           }
                                           val animatedShiftY by animateFloatAsState(
                                               targetValue = targetShift,
                                               animationSpec = spring(
                                                   dampingRatio = Spring.DampingRatioLowBouncy,
                                                   stiffness = Spring.StiffnessMediumLow
                                               ),
                                               label = "shift_un_$i"
                                           )

                                           UpNextSongRow(
                                               rowData = rowData,
                                               contentColor = contentColor,
                                               context = context,
                                               isPlaying = if (isCurrent) isPlaying else false,
                                               isCurrentPlayingItem = isCurrent,
                                               titleFontSize = 15.sp,
                                               artistFontSize = 13.sp,
                                               onClick = onRowClick,
                                               isDragging = isThisDragging,
                                               isAnyDragging = isQueueItemDragging,
                                               dragTranslationX = if (isThisDragging) queueDragOffsetX else 0f,
                                               dragTranslationY = if (isThisDragging) queueDragOffsetY else animatedShiftY,
                                               onDragStart = { startY, globalY ->
                                                    draggingSection = "up_next"
                                                    draggingIndex = i
                                                    targetDropIndex = i
                                                    queueDragOffsetY = 0f
                                                    queueDragOffsetX = 0f
                                                    listScrollAccumulator = 0f
                                                    draggedItemInitialYInBox = globalY - playerBoxRootY
                                                    draggedItemTitle = song.title
                                                    draggedItemArtist = song.artists.joinToString { it.name }
                                                    draggedItemArtUrl = song.thumbnail
                                                    queueTouchYInViewport = startY
                                                    autoScrollVelocity = 0f
                                                    isQueueItemDragging = true
                                                },
                                                onDragDelta = { dx, dy ->
                                                    queueDragOffsetY += dy
                                                    queueDragOffsetX = (queueDragOffsetX + dx * 0.40f).coerceIn(-48f, 48f)
                                                    queueTouchYInViewport += dy

                                                    val unSize = upNextSongs.size
                                                    val totalEffectiveOffset = queueDragOffsetY + listScrollAccumulator
                                                    val itemsMoved = (totalEffectiveOffset / queueRowHeightPx).roundToInt()
                                                    val newTarget = (draggingIndex + itemsMoved).coerceIn(0, unSize - 1)
                                                    if (newTarget != targetDropIndex) {
                                                        targetDropIndex = newTarget
                                                        queueHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }

                                                    val topZone = queueRowHeightPx * 1.5f
                                                    val bottomZone = queueViewportHeightPx - (queueRowHeightPx * 1.5f)
                                                    autoScrollVelocity = when {
                                                        queueViewportHeightPx <= 0f -> 0f
                                                        queueTouchYInViewport < topZone -> {
                                                            val factor = ((topZone - queueTouchYInViewport) / topZone).coerceIn(0f, 1f)
                                                            -(8f + factor * 22f)
                                                        }
                                                        queueTouchYInViewport > bottomZone -> {
                                                            val factor = ((queueTouchYInViewport - bottomZone) / (queueViewportHeightPx - bottomZone).coerceAtLeast(1f)).coerceIn(0f, 1f)
                                                            (8f + factor * 22f)
                                                        }
                                                        else -> 0f
                                                    }
                                                },
                                                onDragEnd = {
                                                    val from = draggingIndex
                                                    val to = targetDropIndex
                                                    if (from != -1 && to != -1 && from != to && from in upNextSongs.indices && to in upNextSongs.indices) {
                                                        val mutable = upNextSongs.toMutableList()
                                                        val item = mutable.removeAt(from)
                                                        mutable.add(to, item)
                                                        onUpNextSongsChange(mutable)
                                                    }
                                                    draggingSection = null
                                                    draggingIndex = -1
                                                    targetDropIndex = -1
                                                    queueDragOffsetY = 0f
                                                    queueDragOffsetX = 0f
                                                    listScrollAccumulator = 0f
                                                    autoScrollVelocity = 0f
                                                    isQueueItemDragging = false
                                                }
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
                                      .fillMaxWidth()
                              ) {
                                  Box(
                                      modifier = Modifier
                                          .fillMaxSize()
                                          .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                      .drawWithContent {
                                          drawContent()
                                          val bottomFadePx = with(density) { 16.dp.toPx() }
                                          val h = size.height
                                          val bottomFadeStart = if (h > bottomFadePx) (h - bottomFadePx) / h else 0.96f
                                          drawRect(
                                              brush = Brush.verticalGradient(
                                                  colorStops = arrayOf(
                                                      0.0f to Color.Transparent,
                                                      0.03f to Color.Black,
                                                      bottomFadeStart to Color.Black,
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
                                  val livePosition by if (musicPlayer != null) {
                                      musicPlayer.currentPosition.collectAsState()
                                  } else {
                                      androidx.compose.runtime.remember(currentPosition) { androidx.compose.runtime.mutableLongStateOf(currentPosition) }
                                  }
                                  val effectivePosition = if (musicPlayer != null) livePosition else currentPosition

                                  val currentLyrics = bitChordLyrics

                                  if (currentLyrics != null && currentLyrics.isNotEmpty()) {
                                      com.mrtdk.liquid_glass.ui.lyrics.BitChordLyricsView(
                                          lines = currentLyrics,
                                          positionMs = (effectivePosition + lyricsOffset).coerceAtLeast(0L),
                                          isPlaying = isPlaying,
                                          looking = isLyricsLoading,
                                          onSeekToLine = { seekPos ->
                                              onSeek(seekPos)
                                              showLyricsControls = true
                                              lyricsControlsHideTrigger++
                                          },
                                          controlsOpen = showLyricsControls,
                                          onRevealControls = {
                                              showLyricsControls = true
                                              lyricsControlsHideTrigger++
                                          },
                                          onHideControls = {
                                              showLyricsControls = false
                                          },
                                          modifier = Modifier.fillMaxSize()
                                      )
                                  } else if (isLyricsLoading) {
                                      com.mrtdk.liquid_glass.ui.lyrics.BitChordLyricsView(
                                          lines = emptyList(),
                                          positionMs = effectivePosition,
                                          isPlaying = isPlaying,
                                          looking = true,
                                          modifier = Modifier.fillMaxSize()
                                      )
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
                                          if (isLyricsLoading) {
                                              CircularProgressIndicator(
                                                  color = contentColor,
                                                  modifier = Modifier.size(36.dp),
                                                  strokeWidth = 3.dp
                                              )
                                          } else {
                                              Column(
                                                  horizontalAlignment = Alignment.CenterHorizontally,
                                                  verticalArrangement = Arrangement.spacedBy(14.dp),
                                                  modifier = Modifier.padding(horizontal = 32.dp)
                                              ) {
                                                  Box(
                                                      modifier = Modifier
                                                          .size(60.dp)
                                                          .clip(CircleShape)
                                                          .background(contentColor.copy(alpha = 0.12f)),
                                                      contentAlignment = Alignment.Center
                                                  ) {
                                                      Icon(
                                                          imageVector = Icons.Default.MusicNote,
                                                          contentDescription = null,
                                                          tint = contentColor.copy(alpha = 0.7f),
                                                          modifier = Modifier.size(30.dp)
                                                      )
                                                  }

                                                  Text(
                                                      text = "Letras no disponibles",
                                                      color = contentColor.copy(alpha = 0.85f),
                                                      fontSize = 17.sp,
                                                      fontWeight = FontWeight.Bold
                                                  )

                                                  Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                      Box(
                                                          modifier = Modifier
                                                              .clip(RoundedCornerShape(20.dp))
                                                              .background(contentColor.copy(alpha = 0.15f))
                                                              .clickable {
                                                                  showManualLyricsSearch = true
                                                              }
                                                              .padding(horizontal = 14.dp, vertical = 8.dp)
                                                      ) {
                                                          Text(
                                                              text = "Buscar letras",
                                                              color = contentColor,
                                                              fontSize = 12.sp,
                                                              fontWeight = FontWeight.SemiBold
                                                          )
                                                      }

                                                      Box(
                                                          modifier = Modifier
                                                              .clip(RoundedCornerShape(20.dp))
                                                              .background(contentColor.copy(alpha = 0.08f))
                                                              .clickable {
                                                                  lyricsReloadTrigger++
                                                              }
                                                              .padding(horizontal = 14.dp, vertical = 8.dp)
                                                      ) {
                                                          Text(
                                                              text = "Reintentar",
                                                              color = contentColor.copy(alpha = 0.75f),
                                                              fontSize = 12.sp,
                                                              fontWeight = FontWeight.Medium
                                                          )
                                                      }
                                                  }
                                              }
                                          }
                                      }
                                  }
                              }
                          }
                      }
                  }
              } // Closes Box of overlay

             } // Closes AnimatedVisibility of overlay



            val playPauseScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isNormalArtwork && !isPlaying && !isOverlayActive && dragProgress == 0f) 0.85f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "playPauseScale"
            )

            val targetFgAlpha = 1f
            val fgArtworkAlpha by animateFloatAsState(
                targetValue = targetFgAlpha,
                animationSpec = tween(200),
                label = "fgArtworkAlpha"
            )

            Box(
                modifier = Modifier
                    .offset(x = imgOffsetX, y = imgOffsetY)
                    .size(width = imgWidth, height = imgHeight)
                    .then(
                        if (isOverlayActive) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showLyrics = false
                                showQueue = false
                            }
                        } else Modifier
                    )
                    .graphicsLayer {
                        scaleX = playPauseScale
                        scaleY = playPauseScale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                        alpha = fgArtworkAlpha
                    }
                    .then(
                        if (isNormalArtwork && !isOverlayActive && dragProgress == 0f) {
                            Modifier.shadow(elevation = 16.dp * playPauseScale, shape = RoundedCornerShape(imgCorner))
                        } else Modifier
                    )
                    .then(
                        if (isNormalArtwork) {
                            Modifier.clip(RoundedCornerShape(imgCorner))
                        } else Modifier
                    )
                    .graphicsLayer {
                        shape = if (!isOverlayActive) {
                            if (isNormalArtwork) {
                                RoundedCornerShape(imgCorner.toPx())
                            } else {
                                RoundedCornerShape(
                                    topStart = imgCorner.toPx(),
                                    topEnd = imgCorner.toPx(),
                                    bottomStart = 0f,
                                    bottomEnd = 0f
                                )
                            }
                        } else {
                            RoundedCornerShape(imgCorner.toPx())
                        }
                        clip = true
                        compositingStrategy = if (dragProgress < 1f) {
                            CompositingStrategy.Offscreen
                        } else {
                            CompositingStrategy.Auto
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        if (!isOverlayActive && !isNormalArtwork) {
                            val h = size.height
                            if (fullArtworkBackdropStyle == "accord") {
                                val startFrac = 0.55f
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.00f to Color.Black,
                                        startFrac to Color.Black,
                                        startFrac + (1f - startFrac) * 0.20f to Color.Black.copy(alpha = 0.95f),
                                        startFrac + (1f - startFrac) * 0.40f to Color.Black.copy(alpha = 0.78f),
                                        startFrac + (1f - startFrac) * 0.60f to Color.Black.copy(alpha = 0.50f),
                                        startFrac + (1f - startFrac) * 0.80f to Color.Black.copy(alpha = 0.22f),
                                        1.00f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            } else {
                                val fadePx = with(density) { 32.dp.toPx() }
                                if (h > fadePx) {
                                    val startFrac = (h - fadePx) / h
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.0f to Color.Black,
                                            startFrac to Color.Black,
                                            1.0f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                            }
                        }
                    }
            ) {

                val artworkBiasAlignment = Alignment.TopCenter

                val currentAnimatedUrl = animatedArtworkUrl

                // Base sharp album cover (always drawn in background during drag or before playback starts)
                // In lyrics and queue views, it strictly displays the original static image!
                val artData = playerState?.artUrl ?: hdArtUrl
                androidx.compose.animation.AnimatedContent(
                    targetState = artData,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(700, easing = FastOutSlowInEasing)) +
                         scaleIn(initialScale = 0.93f, animationSpec = tween(700, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(600, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 1.05f, animationSpec = tween(600, easing = FastOutSlowInEasing))
                            )
                    },
                    label = "albumArtAutoMixTransition",
                    modifier = Modifier.fillMaxSize()
                ) { currentArt ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentArt)
                            .crossfade(true)
                            .build(),
                        imageLoader = animatedImageLoader,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        alignment = artworkBiasAlignment,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!currentAnimatedUrl.isNullOrBlank()) {
                    DisposableEffect(currentAnimatedUrl) {
                        onDispose {
                            isVideoPlaying = false
                            motionCoverBitmap = null
                        }
                    }
                    val videoOverlayAlpha = if (isOverlayActive) {
                        (1f - (overlayTransitionProgress - 0.85f).coerceAtLeast(0f) / 0.15f).coerceIn(0f, 1f)
                    } else {
                        (1f - overlayTransitionProgress).coerceIn(0f, 1f)
                    }
                    com.mrtdk.liquid_glass.ui.components.AnimatedArtworkPlayer(
                        videoUrl = currentAnimatedUrl,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isNormalArtwork || isOverlayActive || overlayTransitionProgress > 0f) {
                                    Modifier.clip(RoundedCornerShape(imgCorner))
                                } else {
                                    Modifier
                                }
                            )
                            .graphicsLayer {
                                alpha = if (isVideoPlaying) videoOverlayAlpha else 0f
                            },
                        isPaused = isOverlayActive && overlayTransitionProgress >= 0.98f,
                        enableFrameCapture = (dragProgress == 0f) && !isOverlayActive && overlayTransitionProgress == 0f,
                        onPlayerCreated = { masterAnimatedPlayer = it },
                        onPlaybackStarted = { isVideoPlaying = true },
                            onFrameCaptured = { frameBitmap ->
                                val bmp = frameBitmap.asImageBitmap()
                                coverBitmap = bmp
                                motionCoverBitmap = bmp
                                frameToken++

                                val now = android.os.SystemClock.uptimeMillis()
                                if (now - lastColorSampleTime >= 250L) {
                                    lastColorSampleTime = now
                                    reflectionSkew = calculateDominantSkew(frameBitmap)

                                    try {
                                        val w = frameBitmap.width
                                        val h = frameBitmap.height

                                        // 1. Muestreo de la franja inferior (75% al 95%) para bottomAverageColor
                                        val startY = (h * 0.75f).toInt().coerceIn(0, h - 1)
                                        val endY = (h * 0.95f).toInt().coerceIn(startY + 1, h)
                                        var rBottom = 0L; var gBottom = 0L; var bBottom = 0L
                                        var countBottom = 0
                                        val stepX = maxOf(1, w / 16)
                                        val stepY = maxOf(1, (endY - startY) / 4)
                                        for (y in startY until endY step stepY) {
                                            for (x in 0 until w step stepX) {
                                                val pixel = frameBitmap.getPixel(x, y)
                                                rBottom += (pixel shr 16 and 0xFF)
                                                gBottom += (pixel shr 8 and 0xFF)
                                                bBottom += (pixel and 0xFF)
                                                countBottom++
                                            }
                                        }
                                        if (countBottom > 0) {
                                            bottomAverageColor = Color((rBottom / countBottom).toInt(), (gBottom / countBottom).toInt(), (bBottom / countBottom).toInt())
                                        }

                                        // 2. Muestreo de la zona central (20% al 70%) para dominantColor del video
                                        val domStartY = (h * 0.20f).toInt().coerceIn(0, h - 1)
                                        val domEndY = (h * 0.70f).toInt().coerceIn(domStartY + 1, h)
                                        var rDom = 0L; var gDom = 0L; var bDom = 0L
                                        var countDom = 0
                                        val stepDomY = maxOf(1, (domEndY - domStartY) / 5)
                                        for (y in domStartY until domEndY step stepDomY) {
                                            for (x in 0 until w step stepX) {
                                                val pixel = frameBitmap.getPixel(x, y)
                                                rDom += (pixel shr 16 and 0xFF)
                                                gDom += (pixel shr 8 and 0xFF)
                                                bDom += (pixel and 0xFF)
                                                countDom++
                                            }
                                        }
                                        if (countDom > 0) {
                                            val newDom = Color((rDom / countDom).toInt(), (gDom / countDom).toInt(), (bDom / countDom).toInt())
                                            val dr = kotlin.math.abs(newDom.red - dominantColor.red)
                                            val dg = kotlin.math.abs(newDom.green - dominantColor.green)
                                            val db = kotlin.math.abs(newDom.blue - dominantColor.blue)
                                            if (dr > 0.05f || dg > 0.05f || db > 0.05f) {
                                                dominantColor = newDom
                                                onDominantColorChanged(newDom)
                                            }
                                        }

                                        // 3. Generar el fondo Accord difuminado con los colores del video en movimiento
                                        if (fullArtworkBackdropStyle == "accord" && !hasGeneratedMotionBackdrop) {
                                            hasGeneratedMotionBackdrop = true
                                            val bmpCopy = try {
                                                frameBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                                            } catch (_: Exception) { null }
                                            if (bmpCopy != null) {
                                                val screenW = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
                                                val screenH = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
                                                scope.launch(Dispatchers.Default) {
                                                    try {
                                                        val motionBackdrop = com.mrtdk.liquid_glass.ui.components.AccordBackdropGenerator.generateAccordBackdrop(
                                                            source = bmpCopy,
                                                            width = screenW,
                                                            height = screenH,
                                                            includeCover = false
                                                        )
                                                        withContext(Dispatchers.Main) {
                                                            accordBackdropBitmap = motionBackdrop
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        }
                                    } catch (_: Exception) { }
                                }
                            },
                            cornerRadius = if (isNormalArtwork || isOverlayActive || overlayTransitionProgress > 0f) imgCorner else 0.dp,
                            clipToBounds = isNormalArtwork || isOverlayActive || overlayTransitionProgress > 0f
                        )
                    }


                // Capa de desenfoque GPU sobre la curva inferior de la carátula
                // key(artUrl) → recomposición total al cambiar canción — sin imagen anterior stale
                // DstIn bezier recorta solo la franja inferior, alineada con la portada principal
                val blurArtKey = playerState?.artUrl ?: hdArtUrl
                if (!isNormalArtwork && fullArtworkBackdropStyle != "accord") {
                    val blurLayerAlpha = (1f - overlayTransitionProgress).coerceIn(0f, 1f)
                    val blurRadiusMaskPx = with(density) { 18.dp.toPx() }

                    key(blurArtKey) {
                        val maskBitmapCacheBlur = remember { arrayOfNulls<androidx.compose.ui.graphics.ImageBitmap>(1) }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = blurLayerAlpha
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()

                                    val width = size.width.roundToInt()
                                    val height = size.height.roundToInt()

                                    if (width > 0 && height > 0) {
                                        val currentBmp = maskBitmapCacheBlur[0]
                                        if (currentBmp == null || currentBmp.width != width || currentBmp.height != height) {
                                            val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                                            val canvas = android.graphics.Canvas(bmp)

                                            val paint = android.graphics.Paint().apply {
                                                isAntiAlias = true
                                                color = android.graphics.Color.BLACK
                                                style = android.graphics.Paint.Style.FILL
                                                maskFilter = android.graphics.BlurMaskFilter(blurRadiusMaskPx, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                            }

                                            val path = android.graphics.Path().apply {
                                                val lY = height - with(density) { 125.dp.toPx() }
                                                val rY = height - with(density) { 105.dp.toPx() }
                                                val mY = height - with(density) { 38.dp.toPx() }
                                                val ext = blurRadiusMaskPx

                                                moveTo(-ext, lY)
                                                lineTo(0f, lY)
                                                cubicTo(
                                                    width * 0.28f, mY + with(density) { 6.dp.toPx() },
                                                    width * 0.65f, mY + with(density) { 8.dp.toPx() },
                                                    width.toFloat(), rY
                                                )
                                                lineTo(width + ext, rY)
                                                lineTo(width + ext, height + ext)
                                                lineTo(-ext, height + ext)
                                                close()
                                            }

                                            canvas.drawPath(path, paint)
                                            maskBitmapCacheBlur[0] = bmp.asImageBitmap()
                                        }
                                    }

                                    maskBitmapCacheBlur[0]?.let { bmp ->
                                        drawImage(image = bmp, blendMode = BlendMode.DstIn)
                                    }
                                }
                        ) {
                            if (isVideoPlaying) {
                                val currentMotionBmp = motionCoverBitmap
                                val token = frameToken
                                if (currentMotionBmp != null) {
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                    renderEffect = android.graphics.RenderEffect
                                                        .createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.MIRROR)
                                                        .asComposeRenderEffect()
                                                }
                                            }
                                            .then(
                                                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                                                    Modifier.blur(14.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                                                } else Modifier
                                            )
                                    ) {
                                        val _t = token
                                        val cW = currentMotionBmp.width.toFloat()
                                        val cH = currentMotionBmp.height.toFloat()
                                        if (cW > 0f && cH > 0f && size.width > 0f && size.height > 0f) {
                                            val scale = maxOf(size.width / cW, size.height / cH)
                                            val scaledW = cW * scale
                                            val scaledH = cH * scale
                                            val srcX = ((scaledW - size.width) / 2f) / scale
                                            val srcY = 0f
                                            val srcW = size.width / scale
                                            val srcH = size.height / scale

                                            drawImage(
                                                image = currentMotionBmp,
                                                srcOffset = androidx.compose.ui.unit.IntOffset(
                                                    srcX.roundToInt().coerceIn(0, currentMotionBmp.width - 1),
                                                    srcY.roundToInt().coerceIn(0, currentMotionBmp.height - 1)
                                                ),
                                                srcSize = androidx.compose.ui.unit.IntSize(
                                                    srcW.roundToInt().coerceIn(1, currentMotionBmp.width),
                                                    srcH.roundToInt().coerceIn(1, currentMotionBmp.height)
                                                ),
                                                dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
                                                dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt())
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        bottomAverageColor.copy(alpha = 0.45f),
                                                        dominantColor.copy(alpha = 0.65f)
                                                    )
                                                )
                                            )
                                    )
                                }
                            } else {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(blurArtKey)
                                            .crossfade(false)
                                            .build(),
                                        imageLoader = animatedImageLoader,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        alignment = artworkBiasAlignment,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                renderEffect = android.graphics.RenderEffect
                                                    .createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.MIRROR)
                                                    .asComposeRenderEffect()
                                            }
                                    )
                                } else {
                                    // API < 31: gradiente como fallback
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                                                    startY = 0f,
                                                    endY = Float.POSITIVE_INFINITY
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }


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
                visible = (!showLyrics || showLyricsControls),
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(240)) { it / 2 },
                exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(200)) { it / 2 }
            ) {
                val currentControlsBaseY = maxWidth * 1.23f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((maxHeight - currentControlsBaseY).coerceAtLeast(0.dp))
                        .graphicsLayer {
                            alpha = contentAlpha
                        }
                        .pointerInput(Unit) {} // Consume all pointer inputs so clicks don't fall through to the lists underneath
                        .background(Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 34.dp)
                            .padding(top = 0.dp)
                            .padding(bottom = 0.dp)
                    ) {
                        Spacer(modifier = Modifier.height(72.dp))

                        IsolatedPlayerSeekbar(
                            musicPlayer = musicPlayer,
                            duration = duration,
                            fallbackPosition = currentPosition,
                            sliderActiveColor = sliderActiveColor,
                            sliderInactiveColor = sliderInactiveColor,
                            contentColor = contentColor,
                            onSeek = onSeek,
                            onPositioned = { coords ->
                                sliderCoordinates = coords
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            PlayerBottomControls(
                                progress = 0f, currentPosition = 0L, duration = duration,
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
                                fillHeight = true,
                                sliderActiveColor = sliderActiveColor,
                                sliderInactiveColor = sliderInactiveColor
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
                    .heightIn(min = 48.dp),
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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnimatedVisibility(
                                visible = isAutoMixing,
                                enter = fadeIn(animationSpec = tween(350)) + slideInVertically(animationSpec = tween(350)) { -it / 2 },
                                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { -it / 2 }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.16f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AutomixIcon(modifier = Modifier.size(12.dp), tint = Color.White)
                                    Text(
                                        text = "AutoMix",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Text(
                                text = state?.title ?: "",
                                color = contentColor,
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            var artistCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                            Text(
                                text = state?.artist ?: "",
                                color = contentColor.copy(alpha = 0.72f),
                                fontSize = artistFontSize,
                                fontWeight = FontWeight.Normal,
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

                Row(
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.15f))
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
                        if (isSaved) {
                            Icon(
                                painter = painterResource(id = R.drawable.fav),
                                contentDescription = "Fav",
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            AsyncImage(
                                model = "file:///android_asset/img reproductor/c.png",
                                contentDescription = "Fav",
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    var threeDotsCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { threeDotsCoords = it }
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.15f))
                            .clickable { 
                                val parentCoords = parentCoordinates
                                if (parentCoords != null && threeDotsCoords != null && parentCoords.isAttached && threeDotsCoords!!.isAttached) {
                                    val localOffset = parentCoords.localPositionOf(threeDotsCoords!!, Offset.Zero)
                                    val size = threeDotsCoords!!.size
                                    menuPivotBounds = androidx.compose.ui.geometry.Rect(localOffset, androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat()))
                                } else {
                                    menuPivotBounds = threeDotsCoords?.boundsInRoot()
                                }
                                if (showLyrics) {
                                    openDirectlyInProvidersView = false
                                    showLyricsOptionsMenu = true
                                } else {
                                    showOptionsMenu = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(moreIconSize)) {
                            val r = 1.8.dp.toPx()
                            val space = 3.5.dp.toPx()
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            drawCircle(contentColor, radius = r, center = Offset(cx - space - r * 2, cy))
                            drawCircle(contentColor, radius = r, center = Offset(cx, cy))
                            drawCircle(contentColor, radius = r, center = Offset(cx + space + r * 2, cy))
                        }
                    }
                }
            }

              if (isQueueItemDragging && draggingIndex != -1 && draggingSection != null) {
                  val density = androidx.compose.ui.platform.LocalDensity.current
                  val draggedInitialYDp = with(density) { draggedItemInitialYInBox.toDp() }
                  val dragOffsetYDp = with(density) { queueDragOffsetY.toDp() }
                  val dragOffsetXDp = with(density) { queueDragOffsetX.toDp() }
                  Box(
                      modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 24.dp)
                          .offset(x = dragOffsetXDp, y = draggedInitialYDp + dragOffsetYDp)
                          .zIndex(9999f)
                  ) {
                      FloatingQueueDragCard(
                          title = draggedItemTitle,
                          artist = draggedItemArtist,
                          artUrl = draggedItemArtUrl,
                          contentColor = contentColor,
                          context = context
                      )
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
                title = { Text(stringResource(R.string.lyrics_edit_dialog_title), color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.OutlinedTextField(
                            value = tempLyricsText,
                            onValueChange = { tempLyricsText = it },
                            label = { Text(stringResource(R.string.lyrics_edit_label), color = Color.Gray) },
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
                            Toast.makeText(context, context.getString(R.string.lyrics_saved_toast), Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text(stringResource(R.string.guardar), color = Color(0xFFFA243C))
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showLyricsEditDialog = false }) {
                        Text(stringResource(R.string.cancelar), color = Color.Gray)
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
                        items(
                            count = playlists.size,
                            key = { i -> playlists[i].id },
                            contentType = { "playlist_choice" }
                        ) { i ->

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
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
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
                        tint = com.mrtdk.glass.DarkGrayGlassTint,
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
                onDismiss = { 
                    showLyricsOptionsMenu = false
                    openDirectlyInProvidersView = false
                },
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
                        bitChordLyrics = lines?.lines?.map { com.mrtdk.liquid_glass.data.lyrics.LyricLine(it.timeMs, it.text) }
                        if (isRomajiEnabled && lines != null) {
                            lyricsMenuScope.launch {
                                val prefs = com.mrtdk.liquid_glass.utils.LyricsRomanizationPreferences(true, true, true, true, true)
                                val processed = com.mrtdk.liquid_glass.utils.LyricsUtils.romanizeSyncedLyrics(lines, prefs)
                                lyricsLines = processed.lines
                                bitChordLyrics = processed.lines.map { com.mrtdk.liquid_glass.data.lyrics.LyricLine(it.timeMs, it.text) }
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
                pivotBounds = menuPivotBounds,
                initialShowProviderSelection = openDirectlyInProvidersView
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
fun LosslessBadge(
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(contentColor.copy(alpha = 0.14f))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.apple_lossless_seeklogo),
                contentDescription = "Lossless",
                tint = contentColor.copy(alpha = 0.85f),
                modifier = Modifier
                    .height(8.5.dp)
                    .width(14.dp)
            )
            Text(
                text = "Lossless",
                color = contentColor.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 11.sp
            )
        }
    }
}

@Composable
private fun IsolatedPlayerSeekbar(
    musicPlayer: com.mrtdk.liquid_glass.playback.MusicPlayer?,
    duration: Long,
    fallbackPosition: Long,
    sliderActiveColor: Color,
    sliderInactiveColor: Color,
    contentColor: Color,
    onSeek: (Long) -> Unit,
    onPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit
) {
    val livePosition by if (musicPlayer != null) {
        musicPlayer.currentPosition.collectAsState()
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(fallbackPosition) }
    }
    val effectivePos = if (musicPlayer != null) livePosition else fallbackPosition
    var scrubPos by remember { mutableStateOf<Long?>(null) }
    val displayPos = scrubPos ?: effectivePos
    val progress = if (duration > 0) displayPos.toFloat() / duration.toFloat() else 0f

    AppleMusicSlider(
        value = progress,
        onValueChange = { scrubPos = (it * duration).toLong() },
        onValueChangeFinished = { finalProg ->
            onSeek((finalProg * duration).toLong())
            scrubPos = null
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .onGloballyPositioned { coords ->
                onPositioned(coords)
            },
        activeColor = sliderActiveColor,
        inactiveColor = sliderInactiveColor,
        barHeightDp = 8.dp
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(formatDuration(displayPos), color = contentColor.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Normal)
        LosslessBadge(
            contentColor = contentColor,
            onClick = { AudioRoutingState.showAudioRoutingMenu = true }
        )
        Text("-${formatDuration((duration - displayPos).coerceAtLeast(0L))}", color = contentColor.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Normal)
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
    fillHeight: Boolean = false,
    sliderActiveColor: Color = if (contentColor != Color.White) Color(0xFF1A1A1A) else Color(0xFFE5E5EA),
    sliderInactiveColor: Color = if (contentColor != Color.White) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.18f)
) {
    val isLightBackground = contentColor != Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (includeProgress) Modifier.padding(horizontal = 24.dp) else Modifier)
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
    ) {
        if (includeProgress) {
            var scrubPos by remember { mutableStateOf<Long?>(null) }
            val displayPos = scrubPos ?: currentPosition
            val currentProgress = if (duration > 0) displayPos.toFloat() / duration.toFloat() else 0f
            AppleMusicSlider(
                value = currentProgress,
                onValueChange = { scrubPos = (it * duration).toLong() },
                onValueChangeFinished = { finalProg ->
                    onSeek((finalProg * duration).toLong())
                    scrubPos = null
                },
                modifier = Modifier.fillMaxWidth().height(26.dp),
                activeColor = sliderActiveColor,
                inactiveColor = sliderInactiveColor,
                barHeightDp = 8.dp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatDuration(displayPos), color = contentColor.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Normal)
                LosslessBadge(
                    contentColor = contentColor,
                    onClick = { AudioRoutingState.showAudioRoutingMenu = true }
                )
                Text("-${formatDuration((duration - displayPos).coerceAtLeast(0L))}", color = contentColor.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        if (fillHeight) {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedSkipButton(
                iconId = R.drawable.previous,
                contentDescription = "Previous",
                contentColor = contentColor,
                sizeDp = 74.dp,
                iconSizeDp = 60.dp,
                onClick = onSkipPrevious
            )

            Spacer(modifier = Modifier.width(22.dp))

            val playPauseInteractionSource = remember { MutableInteractionSource() }
            val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()

            val playPauseBgColor by animateColorAsState(
                targetValue = if (isPlayPausePressed) contentColor.copy(alpha = 0.10f) else Color.Transparent,
                label = "playPauseBg"
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
                        modifier = Modifier.size(70.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(22.dp))

            AnimatedSkipButton(
                iconId = R.drawable.forward,
                contentDescription = "Next",
                contentColor = contentColor,
                sizeDp = 74.dp,
                iconSizeDp = 60.dp,
                onClick = onSkipNext
            )
        }

        if (fillHeight) {
            Spacer(modifier = Modifier.weight(1.1f))
        }

        if (includeVolumeAndIcons) {
            if (!fillHeight) {
                Spacer(modifier = Modifier.height(30.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.albumspeaker),
                    contentDescription = "Low volume",
                    tint = contentColor.copy(alpha = 0.55f),
                    modifier = Modifier.size(15.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                AppleMusicSlider(
                    value = volumePosition, onValueChange = { onVolumeChange(it) },
                    modifier = Modifier.weight(1f).height(26.dp),
                    activeColor = sliderActiveColor,
                    inactiveColor = sliderInactiveColor,
                    barHeightDp = 8.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    painter = painterResource(id = R.drawable.albumspeakerlarge),
                    contentDescription = "High volume",
                    tint = contentColor.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!fillHeight) {
                Spacer(modifier = Modifier.height(28.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(60.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (showLyrics) contentColor else Color.Transparent)
                        .clickable { onToggleLyrics() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "file:///android_asset/img reproductor/Letras.png",
                        contentDescription = "Lyrics",
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                            if (showLyrics) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { AudioRoutingState.showAudioRoutingMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "file:///android_asset/img reproductor/parlante.png",
                        contentDescription = "Format",
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.65f)),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (showQueue) contentColor else Color.Transparent)
                        .clickable { onToggleQueue() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.nextinfo),
                        contentDescription = "Next Info",
                        tint = if (showQueue) (if (isLightBackground) Color.White else Color(0xFF1A1A1A)) else contentColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            if (fillHeight) {
                Spacer(modifier = Modifier.weight(1.5f))
            } else {
                Spacer(modifier = Modifier.height(44.dp))
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
    onValueChangeFinished: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFE5E5EA),
    inactiveColor: Color = Color.White.copy(alpha = 0.18f),
    barHeightDp: androidx.compose.ui.unit.Dp = 7.dp
) {
    var isDragging by remember { mutableStateOf(false) }
    var localDragValue by remember { mutableStateOf<Float?>(null) }

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
                        val startProg = if (sliderWidth > 0) (down.position.x / sliderWidth).coerceIn(0f, 1f) else 0f
                        localDragValue = startProg
                        onValueChange(startProg)
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val dragEvent = event.changes.firstOrNull()
                            if (dragEvent != null && dragEvent.pressed) {
                                if (sliderWidth > 0) {
                                    val currentProg = (dragEvent.position.x / sliderWidth).coerceIn(0f, 1f)
                                    localDragValue = currentProg
                                    onValueChange(currentProg)
                                }
                                dragEvent.consume()
                            } else {
                                break
                            }
                        }

                        val finalVal = localDragValue ?: value
                        onValueChangeFinished?.invoke(finalVal)
                        localDragValue = null
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
            val displayValue = (localDragValue ?: value).coerceIn(0f, 1f)
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(0f, centerY),
                size = Size(width * displayValue, barHeight),
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



data class LandscapePlayerColors(
    val dominantColor: Color,
    val bottomAverageColor: Color,
    val rightSideAverageColor: Color,
    val contentColor: Color,
    val isLightBackground: Boolean,
    val sliderActiveColor: Color,
    val sliderInactiveColor: Color
)

data class LandscapePlayerCallbacks(
    val onUpNextSongsChange: (List<com.echo.innertube.models.SongItem>) -> Unit,
    val onSkipNext: () -> Unit,
    val onSkipPrevious: () -> Unit,
    val onClose: () -> Unit,
    val onTogglePlayPause: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onVolumeChange: (Float) -> Unit,
    val onArtistSelected: (com.mrtdk.liquid_glass.ui.screens.ArtistState) -> Unit,
    val onAlbumSelected: (com.mrtdk.liquid_glass.ui.screens.AlbumState) -> Unit,
    val onSongSelected: (PlayerState) -> Unit,
    val onSongSelectedFromQueue: (PlayerState) -> Unit,
    val onToggleShuffle: () -> Unit,
    val onToggleRepeat: () -> Unit,
    val onShowLyricsChange: (Boolean) -> Unit,
    val onShowQueueChange: (Boolean) -> Unit,
    val onVolumePositionChange: (Float) -> Unit,
    val onCoverBitmapChange: (ImageBitmap?) -> Unit,
    val onDominantColorChange: (Color) -> Unit,
    val onBottomAverageColorChange: (Color) -> Unit,
    val onRightSideAverageColorChange: (Color) -> Unit,
    val onToggleRomaji: () -> Unit,
    val onVideoPlayingChange: (Boolean) -> Unit,
    val onShowOptionsMenu: (androidx.compose.ui.geometry.Rect?) -> Unit,
    val onShowLyricsMenu: () -> Unit,
    val onShowPlaylistMenu: () -> Unit,
    val onShowArtistMenu: (List<String>, androidx.compose.ui.geometry.Rect?) -> Unit,
    val onAutoScrollChange: (Boolean) -> Unit = {},
    val isAutomixEnabled: Boolean = true,
    val onToggleAutomix: () -> Unit = {},
    val onToggleAutoplay: (() -> Unit)? = null
)

@Composable
fun LandscapePlayerLayout(
    maxWidth: androidx.compose.ui.unit.Dp,
    maxHeight: androidx.compose.ui.unit.Dp,
    playerState: PlayerState?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    upNextSongs: List<com.echo.innertube.models.SongItem>,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    showLyrics: Boolean,
    showQueue: Boolean,
    volumePosition: Float,
    coverBitmap: ImageBitmap?,
    hdArtUrl: Any?,
    bitChordLyrics: List<com.mrtdk.liquid_glass.data.lyrics.LyricLine>? = null,
    isLyricsLoading: Boolean = false,
    lyricsLines: List<com.mocharealm.accompanist.lyrics.core.model.ISyncedLine>? = null,
    isRomajiEnabled: Boolean,
    isSaved: Boolean,
    animatedArtworkUrl: String?,
    isVideoPlaying: Boolean,
    animatedImageLoader: coil.ImageLoader,
    isBottomBarCollapsed: Boolean,
    isAutoScrollEnabled: Boolean = true,
    scrollToCurrentTrigger: Int = 0,
    lyricsOffset: Int = 0,
    colors: LandscapePlayerColors,
    callbacks: LandscapePlayerCallbacks
) {
    val dominantColor = colors.dominantColor
    val bottomAverageColor = colors.bottomAverageColor
    val rightSideAverageColor = colors.rightSideAverageColor
    val contentColor = colors.contentColor
    val isLightBackground = colors.isLightBackground
    val sliderActiveColor = colors.sliderActiveColor
    val sliderInactiveColor = colors.sliderInactiveColor

    val onUpNextSongsChange = callbacks.onUpNextSongsChange
    val onSkipNext = callbacks.onSkipNext
    val onSkipPrevious = callbacks.onSkipPrevious
    val onClose = callbacks.onClose
    val onTogglePlayPause = callbacks.onTogglePlayPause
    val onSeek = callbacks.onSeek
    val onVolumeChange = callbacks.onVolumeChange
    val onArtistSelected = callbacks.onArtistSelected
    val onAlbumSelected = callbacks.onAlbumSelected
    val onSongSelected = callbacks.onSongSelected
    val onSongSelectedFromQueue = callbacks.onSongSelectedFromQueue
    val onToggleShuffle = callbacks.onToggleShuffle
    val onToggleRepeat = callbacks.onToggleRepeat
    val onShowLyricsChange = callbacks.onShowLyricsChange
    val onShowQueueChange = callbacks.onShowQueueChange
    val onVolumePositionChange = callbacks.onVolumePositionChange
    val onCoverBitmapChange = callbacks.onCoverBitmapChange
    val onDominantColorChange = callbacks.onDominantColorChange
    val onBottomAverageColorChange = callbacks.onBottomAverageColorChange
    val onRightSideAverageColorChange = callbacks.onRightSideAverageColorChange
    val onToggleRomaji = callbacks.onToggleRomaji
    val onVideoPlayingChange = callbacks.onVideoPlayingChange
    val onShowOptionsMenu = callbacks.onShowOptionsMenu
    val onShowLyricsMenu = callbacks.onShowLyricsMenu
    val onShowPlaylistMenu = callbacks.onShowPlaylistMenu
    val onShowArtistMenu = callbacks.onShowArtistMenu
    val onAutoScrollChange = callbacks.onAutoScrollChange
    val isAutomixEnabled = callbacks.isAutomixEnabled
    val onToggleAutomix = callbacks.onToggleAutomix
    val onToggleAutoplay = callbacks.onToggleAutoplay

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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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

                            alpha = if (isVideoPlaying && !showLyrics && !showQueue) 1f else 0f 

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

                    isPaused = !isPlaying || showLyrics || showQueue,

                    enableFrameCapture = !showLyrics && !showQueue,

                    onPlaybackStarted = { onVideoPlayingChange(true) },

                    onFrameCaptured = { frameBitmap ->

                        onCoverBitmapChange(frameBitmap.asImageBitmap())

                        try {
                            var r = 0L; var g = 0L; var b = 0L
                            val yCoord = frameBitmap.height - 1
                            val w = frameBitmap.width
                            val stepX = maxOf(1, w / 24)
                            var countX = 0

                            for (x in 0 until w step stepX) {
                                val pixel = frameBitmap.getPixel(x, yCoord)
                                r += (pixel shr 16 and 0xFF)
                                g += (pixel shr 8 and 0xFF)
                                b += (pixel and 0xFF)
                                countX++
                            }

                            val avgColor = Color((r / countX).toInt(), (g / countX).toInt(), (b / countX).toInt())
                            onBottomAverageColorChange(avgColor)
                            val lastDom = dominantColor
                            val dr = kotlin.math.abs(avgColor.red - lastDom.red)
                            val dg = kotlin.math.abs(avgColor.green - lastDom.green)
                            val db = kotlin.math.abs(avgColor.blue - lastDom.blue)
                            if (dr > 0.05f || dg > 0.05f || db > 0.05f) {
                                onDominantColorChange(avgColor)
                            }

                            // Promedio de la columna derecha de píxeles optimizado con muestreo por pasos
                            var rRight = 0L; var gRight = 0L; var bRight = 0L
                            val xCoord = frameBitmap.width - 1
                            val h = frameBitmap.height
                            val stepY = maxOf(1, h / 24)
                            var countY = 0

                            for (y in 0 until h step stepY) {
                                val pixel = frameBitmap.getPixel(xCoord, y)
                                rRight += (pixel shr 16 and 0xFF)
                                gRight += (pixel shr 8 and 0xFF)
                                bRight += (pixel and 0xFF)
                                countY++
                            }

                            if (countY > 0) {
                                val rightColor = Color((rRight / countY).toInt(), (gRight / countY).toInt(), (bRight / countY).toInt())
                                val lastRight = rightSideAverageColor
                                val drR = kotlin.math.abs(rightColor.red - lastRight.red)
                                val dgR = kotlin.math.abs(rightColor.green - lastRight.green)
                                val dbR = kotlin.math.abs(rightColor.blue - lastRight.blue)
                                if (drR > 0.05f || dgR > 0.05f || dbR > 0.05f) {
                                    onRightSideAverageColorChange(rightColor)
                                }
                            }
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

                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.graphicsLayer {
                                    renderEffect = BlurEffect(90f, 90f, TileMode.Clamp)
                                }
                            } else {
                                Modifier.cloudy(radius = 35)
                            }
                        )
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
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.graphicsLayer {
                                    renderEffect = BlurEffect(90f, 90f, TileMode.Clamp)
                                }
                            } else {
                                Modifier.cloudy(radius = 35)
                            }
                        )
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
                                .background(contentColor.copy(alpha = 0.15f))
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
                            if (isSaved) {
                                Icon(
                                    painter = painterResource(id = R.drawable.fav),
                                    contentDescription = "Fav",
                                    tint = contentColor,
                                    modifier = Modifier.size(23.dp)
                                )
                            } else {
                                AsyncImage(
                                    model = "file:///android_asset/img reproductor/c.png",
                                    contentDescription = "Fav",
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                        LandscapeLyricsView(
                            lyrics = bitChordLyrics,
                            isLoading = isLyricsLoading,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            lyricsOffset = lyricsOffset,
                            contentColor = contentColor,
                            onSeek = onSeek
                        )
                    } else if (showQueue) {
                        LandscapeQueueView(
                            playerState = playerState,
                            isPlaying = isPlaying,
                            shuffleModeEnabled = shuffleModeEnabled,
                            repeatMode = repeatMode,
                            onToggleShuffle = onToggleShuffle,
                            onToggleRepeat = onToggleRepeat,
                            onSongSelected = onSongSelected,
                            onSongSelectedFromQueue = onSongSelectedFromQueue,
                            isAutomixEnabled = isAutomixEnabled,
                            onToggleAutomix = onToggleAutomix,
                            upNextSongs = upNextSongs,
                            onUpNextSongsChange = onUpNextSongsChange,
                            contentColor = contentColor,
                            rightSideAverageColor = rightSideAverageColor,
                            context = context,
                            onToggleAutoplay = onToggleAutoplay
                        )
                    } else {
                        LandscapeControlsView(
                            duration = duration,
                            currentPosition = currentPosition,
                            onSeek = onSeek,
                            sliderActiveColor = sliderActiveColor,
                            sliderInactiveColor = sliderInactiveColor,
                            contentColor = contentColor,
                            onSkipPrevious = onSkipPrevious,
                            isPlaying = isPlaying,
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipNext = onSkipNext,
                            volumePosition = volumePosition,
                            onVolumePositionChange = onVolumePositionChange,
                            audioManager = audioManager,
                            maxVolume = maxVolume,
                            onVolumeChange = onVolumeChange
                        )
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
private fun LandscapeLyricsView(
    lyrics: List<com.mrtdk.liquid_glass.data.lyrics.LyricLine>?,
    isLoading: Boolean,
    isPlaying: Boolean,
    currentPosition: Long,
    lyricsOffset: Int,
    contentColor: Color,
    onSeek: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val currentLyrics = lyrics
        if (currentLyrics != null && currentLyrics.isNotEmpty()) {
            com.mrtdk.liquid_glass.ui.lyrics.BitChordLyricsView(
                lines = currentLyrics,
                positionMs = (currentPosition + lyricsOffset).coerceAtLeast(0L),
                isPlaying = isPlaying,
                looking = isLoading,
                onSeekToLine = onSeek,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            com.mrtdk.liquid_glass.ui.lyrics.BitChordLyricsView(
                lines = emptyList(),
                positionMs = currentPosition,
                isPlaying = isPlaying,
                looking = true,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No se encontraron letras",
                    color = contentColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun LandscapeQueueView(
    playerState: PlayerState?,
    isPlaying: Boolean,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSongSelected: (PlayerState) -> Unit,
    onSongSelectedFromQueue: (PlayerState) -> Unit,
    isAutomixEnabled: Boolean,
    onToggleAutomix: () -> Unit,
    upNextSongs: List<com.echo.innertube.models.SongItem>,
    onUpNextSongsChange: (List<com.echo.innertube.models.SongItem>) -> Unit,
    contentColor: Color,
    rightSideAverageColor: Color,
    context: android.content.Context,
    onToggleAutoplay: (() -> Unit)? = null
) {
    val queueListState = rememberLazyListState()
    Column(modifier = Modifier.fillMaxSize()) {
        val shuffleInteraction = remember { MutableInteractionSource() }
        val isShuffleActive = shuffleModeEnabled
        val isShufflePressed by shuffleInteraction.collectIsPressedAsState()
        val shuffleScale by animateFloatAsState(targetValue = if (isShufflePressed) 0.85f else 1.0f, label = "shuffleScale")
        val activeBg = contentColor.copy(alpha = 0.9f)
        val activeIcon = if (contentColor == Color.White) rightSideAverageColor else Color.White
        val shuffleBgColor by animateColorAsState(targetValue = if (isShuffleActive) activeBg else contentColor.copy(alpha = 0.15f), label = "shuffleBg")
        val shuffleIconColor by animateColorAsState(targetValue = if (isShuffleActive) activeIcon else contentColor.copy(alpha = 0.5f), label = "shuffleIcon")

        val repeatInteraction = remember { MutableInteractionSource() }
        val isRepeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF
        val isRepeatPressed by repeatInteraction.collectIsPressedAsState()
        val repeatScale by animateFloatAsState(targetValue = if (isRepeatPressed) 0.85f else 1.0f, label = "repeatScale")
        val repeatBgColor by animateColorAsState(targetValue = if (isRepeatActive) activeBg else contentColor.copy(alpha = 0.15f), label = "repeatBg")
        val repeatIconColor by animateColorAsState(targetValue = if (isRepeatActive) activeIcon else contentColor.copy(alpha = 0.5f), label = "repeatIcon")

        val autoplayInteraction = remember { MutableInteractionSource() }
        val isAutoplayActive = playerState?.isExclusiveQueue != true
        val isAutoplayPressed by autoplayInteraction.collectIsPressedAsState()
        val autoplayScale by animateFloatAsState(targetValue = if (isAutoplayPressed) 0.85f else 1.0f, label = "autoplayScale")
        val autoplayBgColor by animateColorAsState(targetValue = if (isAutoplayActive) activeBg else contentColor.copy(alpha = 0.15f), label = "autoplayBg")
        val autoplayIconColor by animateColorAsState(targetValue = if (isAutoplayActive) activeIcon else contentColor.copy(alpha = 0.5f), label = "autoplayIcon")

        val automixInteraction = remember { MutableInteractionSource() }
        val isAutomixActive = isAutomixEnabled
        val isAutomixPressed by automixInteraction.collectIsPressedAsState()
        val automixScale by animateFloatAsState(targetValue = if (isAutomixPressed) 0.85f else 1.0f, label = "automixScale")
        val automixBgColor by animateColorAsState(targetValue = if (isAutomixActive) activeBg else contentColor.copy(alpha = 0.15f), label = "automixBg")
        val automixIconColor by animateColorAsState(targetValue = if (isAutomixActive) activeIcon else contentColor.copy(alpha = 0.5f), label = "automixIcon")

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
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(id = R.drawable.shuffle), "Shuffle", tint = shuffleIconColor, modifier = Modifier.size(18.dp))
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
                contentAlignment = Alignment.Center
            ) {
                Icon(if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint = repeatIconColor, modifier = Modifier.size(18.dp))
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
                            if (onToggleAutoplay != null) {
                                onToggleAutoplay()
                            } else if (playerState != null) {
                                val newExclusive = !playerState.isExclusiveQueue
                                com.mrtdk.liquid_glass.playback.PlaybackQueue.isExclusiveQueue = newExclusive
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AllInclusive, "Autoplay", tint = autoplayIconColor, modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer(scaleX = automixScale, scaleY = automixScale)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(automixBgColor)
                    .clickable(
                        interactionSource = automixInteraction,
                        indication = null,
                        onClick = onToggleAutomix
                    ),
                contentAlignment = Alignment.Center
            ) {
                AutomixIcon(tint = automixIconColor, size = 20.dp)
            }
        }

        if (playerState != null && playerState.queue.isNotEmpty()) {
            Text(text = stringResource(R.string.siguiente_en_album_playlist), color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
        } else if (upNextSongs.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)) {
                Text(text = stringResource(R.string.continue_playing), color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.autoplaying_similar_music), color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }

        val lsDensity = androidx.compose.ui.platform.LocalDensity.current
        val lsRowHeightPx = with(lsDensity) { 60.dp.toPx() }
        val lsHaptic = LocalHapticFeedback.current

        var lsDraggingSection by remember { mutableStateOf<String?>(null) }
        var lsDraggingIndex by remember { mutableIntStateOf(-1) }
        var lsTargetDropIndex by remember { mutableIntStateOf(-1) }
        var lsDragOffsetY by remember { mutableFloatStateOf(0f) }
        var lsDragOffsetX by remember { mutableFloatStateOf(0f) }

        LazyColumn(
            state = queueListState,
            userScrollEnabled = (lsDraggingIndex == -1),
            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
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
                            Text(playerState.artist ?: "", color = contentColor.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1)
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
                    key = { index -> state.queue[index].videoId ?: index.toString() },
                    contentType = { "queue_item" }
                ) { index ->
                    val qItem = state.queue[index]
                    val isCurrent = state.videoId != null && qItem.videoId == state.videoId
                    val isThisDragging = lsDraggingSection == "queue" && lsDraggingIndex == index

                    val targetShift = when {
                        lsDraggingSection != "queue" || lsDraggingIndex == -1 -> 0f
                        isThisDragging -> 0f
                        lsDraggingIndex < lsTargetDropIndex && index > lsDraggingIndex && index <= lsTargetDropIndex -> -lsRowHeightPx
                        lsDraggingIndex > lsTargetDropIndex && index < lsDraggingIndex && index >= lsTargetDropIndex -> lsRowHeightPx
                        else -> 0f
                    }
                    val animatedShiftY by animateFloatAsState(
                        targetValue = targetShift,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "ls_shift_q_$index"
                    )

                    val rowData = remember(qItem.title, qItem.artist, qItem.artUrl) {
                        QueueItemRowData(
                            title = qItem.title,
                            artist = qItem.artist,
                            artUrl = qItem.artUrl
                        )
                    }

                    val onRowClick = remember(qItem, index, state) {
                        {
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
                        titleFontSize = 15.sp,
                        artistFontSize = 13.sp,
                        onClick = onRowClick,
                        isDragging = isThisDragging,
                        isAnyDragging = (lsDraggingIndex != -1),
                        dragTranslationX = if (isThisDragging) lsDragOffsetX else 0f,
                        dragTranslationY = if (isThisDragging) lsDragOffsetY else animatedShiftY,
                        onDragStart = { _, _ ->
                            lsDraggingSection = "queue"
                            lsDraggingIndex = index
                            lsTargetDropIndex = index
                            lsDragOffsetY = 0f
                            lsDragOffsetX = 0f
                        },
                        onDragDelta = { dx: Float, dy: Float ->
                            lsDragOffsetY += dy
                            lsDragOffsetX = (lsDragOffsetX + dx * 0.40f).coerceIn(-48f, 48f)
                            val itemsMoved = (lsDragOffsetY / lsRowHeightPx).roundToInt()
                            val newTarget = (index + itemsMoved).coerceIn(0, state.queue.size - 1)
                            if (newTarget != lsTargetDropIndex) {
                                lsTargetDropIndex = newTarget
                                lsHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            val from = lsDraggingIndex
                            val to = lsTargetDropIndex
                            if (from != -1 && to != -1 && from != to && from in state.queue.indices && to in state.queue.indices) {
                                val mutable = state.queue.toMutableList()
                                val item = mutable.removeAt(from)
                                mutable.add(to, item)
                                onSongSelected(state.copy(queue = mutable))
                            }
                            lsDraggingSection = null
                            lsDraggingIndex = -1
                            lsTargetDropIndex = -1
                            lsDragOffsetY = 0f
                            lsDragOffsetX = 0f
                        }
                    )
                }
            }

            if (upNextSongs.isNotEmpty()) {
                val state = playerState
                itemsIndexed(
                    items = upNextSongs,
                    key = { _, song -> song.id },
                    contentType = { _, _ -> "up_next_item" }
                ) { i, song ->
                    val isCurrent = state != null && song.id == state.videoId
                    val isThisDragging = lsDraggingSection == "up_next" && lsDraggingIndex == i

                    val targetShift = when {
                        lsDraggingSection != "up_next" || lsDraggingIndex == -1 -> 0f
                        isThisDragging -> 0f
                        lsDraggingIndex < lsTargetDropIndex && i > lsDraggingIndex && i <= lsTargetDropIndex -> -lsRowHeightPx
                        lsDraggingIndex > lsTargetDropIndex && i < lsDraggingIndex && i >= lsTargetDropIndex -> lsRowHeightPx
                        else -> 0f
                    }
                    val animatedShiftY by animateFloatAsState(
                        targetValue = targetShift,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "ls_shift_un_$i"
                    )

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
                            val remaining = upNextSongs.toMutableList().apply {
                                if (i in indices) removeAt(i)
                            }
                            onUpNextSongsChange(remaining)
                            onSongSelectedFromQueue(PlayerState(
                                title = song.title,
                                artist = song.artists.joinToString { it.name },
                                artUrl = upgradedArt,
                                videoId = song.id,
                                queue = state?.queue ?: emptyList(),
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
                        onClick = onRowClick,
                        isDragging = isThisDragging,
                        isAnyDragging = (lsDraggingIndex != -1),
                        dragTranslationX = if (isThisDragging) lsDragOffsetX else 0f,
                        dragTranslationY = if (isThisDragging) lsDragOffsetY else animatedShiftY,
                        onDragStart = { _, _ ->
                            lsDraggingSection = "up_next"
                            lsDraggingIndex = i
                            lsTargetDropIndex = i
                            lsDragOffsetY = 0f
                            lsDragOffsetX = 0f
                        },
                        onDragDelta = { dx: Float, dy: Float ->
                            lsDragOffsetY += dy
                            lsDragOffsetX = (lsDragOffsetX + dx * 0.40f).coerceIn(-48f, 48f)
                            val itemsMoved = (lsDragOffsetY / lsRowHeightPx).roundToInt()
                            val newTarget = (i + itemsMoved).coerceIn(0, upNextSongs.size - 1)
                            if (newTarget != lsTargetDropIndex) {
                                lsTargetDropIndex = newTarget
                                lsHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            val from = lsDraggingIndex
                            val to = lsTargetDropIndex
                            if (from != -1 && to != -1 && from != to && from in upNextSongs.indices && to in upNextSongs.indices) {
                                val mutable = upNextSongs.toMutableList()
                                val item = mutable.removeAt(from)
                                mutable.add(to, item)
                                onUpNextSongsChange(mutable)
                            }
                            lsDraggingSection = null
                            lsDraggingIndex = -1
                            lsTargetDropIndex = -1
                            lsDragOffsetY = 0f
                            lsDragOffsetX = 0f
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun LandscapeControlsView(
    duration: Long,
    currentPosition: Long,
    onSeek: (Long) -> Unit,
    sliderActiveColor: Color,
    sliderInactiveColor: Color,
    contentColor: Color,
    onSkipPrevious: () -> Unit,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    volumePosition: Float,
    onVolumePositionChange: (Float) -> Unit,
    audioManager: android.media.AudioManager,
    maxVolume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(1f))

        // Apple Music slider
        val progressVal = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

        AppleMusicSlider(
            value = progressVal,
            onValueChange = { onSeek((it * duration).toLong()) },
            modifier = Modifier.fillMaxWidth().height(24.dp),
            activeColor = sliderActiveColor,
            inactiveColor = sliderInactiveColor,
            barHeightDp = 8.dp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.50f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.50f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                    .size(96.dp)
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
                            .size(76.dp)
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

            Spacer(modifier = Modifier.width(10.dp))

            AppleMusicSlider(
                value = volumePosition,
                onValueChange = { v ->
                    onVolumePositionChange(v)
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (v * maxVolume).toInt(), 0)
                    onVolumeChange(v)
                },
                modifier = Modifier.weight(1f).height(24.dp),
                activeColor = sliderActiveColor,
                inactiveColor = sliderInactiveColor,
                barHeightDp = 8.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

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
private fun AnimatedLiquidMeshBackground(
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_mesh")
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t2"
    )
    val t3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t3"
    )
    val t4 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 19000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t4"
    )

    // Colores vivos reales de la carátula sin oscurecerlos
    val safePrimary = remember(primaryColor) {
        if (primaryColor == Color.Transparent || primaryColor == Color.Black) {
            Color(0xFFD49A3D)
        } else if (primaryColor.luminance() < 0.12f) {
            Color(
                red = (primaryColor.red * 2.2f + 0.18f).coerceIn(0f, 1f),
                green = (primaryColor.green * 2.2f + 0.15f).coerceIn(0f, 1f),
                blue = (primaryColor.blue * 2.2f + 0.10f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else primaryColor
    }

    val safeSecColor = remember(secondaryColor, safePrimary) {
        if (secondaryColor == Color.Transparent || secondaryColor == Color.Black || secondaryColor == primaryColor) {
            Color(
                red = (safePrimary.red * 0.92f + 0.08f).coerceIn(0f, 1f),
                green = (safePrimary.green * 0.78f + 0.06f).coerceIn(0f, 1f),
                blue = (safePrimary.blue * 0.52f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else if (secondaryColor.luminance() < 0.12f) {
            Color(
                red = (secondaryColor.red * 2.2f + 0.18f).coerceIn(0f, 1f),
                green = (secondaryColor.green * 2.2f + 0.15f).coerceIn(0f, 1f),
                blue = (secondaryColor.blue * 2.2f + 0.10f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else secondaryColor
    }

    val safeAccentColor = remember(accentColor, safeSecColor, safePrimary) {
        if (accentColor == Color.Transparent || accentColor == Color.Black || accentColor == primaryColor) {
            safeSecColor
        } else if (accentColor.luminance() < 0.12f) {
            Color(
                red = (accentColor.red * 2.2f + 0.18f).coerceIn(0f, 1f),
                green = (accentColor.green * 2.2f + 0.15f).coerceIn(0f, 1f),
                blue = (accentColor.blue * 2.2f + 0.10f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else accentColor
    }

    val highlightColor = remember(safePrimary, safeSecColor) {
        Color(
            red = (safePrimary.red * 0.6f + safeSecColor.red * 0.4f + 0.12f).coerceIn(0f, 1f),
            green = (safePrimary.green * 0.6f + safeSecColor.green * 0.4f + 0.12f).coerceIn(0f, 1f),
            blue = (safePrimary.blue * 0.6f + safeSecColor.blue * 0.4f + 0.06f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    val path1 = remember { Path() }
    val path2 = remember { Path() }
    val path3 = remember { Path() }
    val path4 = remember { Path() }
    val perfConfig = com.mrtdk.liquid_glass.utils.PerformanceProfileManager.getConfig()

    Box(modifier = modifier) {
        // 1. Fondo base degradado con los tonos reales luminosos del artwork
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to safePrimary,
                    0.50f to safeSecColor,
                    1.0f to safeAccentColor
                )
            )
        }

        // 2. Curvas de gusano gaussianas fluidas con los colores de la carátula
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(54.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val strokeWidthWorm = w * 0.48f

            // Gusano 1: Sinuoso superior-medio (safePrimary a highlightColor)
            val p1x0 = -w * 0.15f
            val p1y0 = h * (0.18f + 0.09f * kotlin.math.sin(t1.toDouble()).toFloat())
            val p1c1x = w * (0.25f + 0.18f * kotlin.math.cos(t2.toDouble()).toFloat())
            val p1c1y = h * (0.08f + 0.12f * kotlin.math.sin((t1 * 0.8f).toDouble()).toFloat())
            val p1c2x = w * (0.65f + 0.16f * kotlin.math.sin(t3.toDouble()).toFloat())
            val p1c2y = h * (0.34f + 0.10f * kotlin.math.cos((t2 * 0.7f).toDouble()).toFloat())
            val p1x1 = w * 1.15f
            val p1y1 = h * (0.22f + 0.11f * kotlin.math.sin((t4 * 0.9f).toDouble()).toFloat())

            path1.rewind()
            path1.moveTo(p1x0, p1y0)
            path1.cubicTo(p1c1x, p1c1y, p1c2x, p1c2y, p1x1, p1y1)
            drawPath(
                path = path1,
                brush = Brush.linearGradient(
                    colors = listOf(safePrimary.copy(alpha = 0.95f), highlightColor.copy(alpha = 0.88f), safeSecColor.copy(alpha = 0.70f)),
                    start = Offset(p1x0, p1y0),
                    end = Offset(p1x1, p1y1)
                ),
                style = Stroke(
                    width = strokeWidthWorm,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Gusano 2: Curva sinuosa media-cruzada (safeSecColor a safePrimary)
            val p2x0 = w * 1.15f
            val p2y0 = h * (0.42f + 0.10f * kotlin.math.cos((t2 * 0.85f).toDouble()).toFloat())
            val p2c1x = w * (0.75f + 0.18f * kotlin.math.sin(t1.toDouble()).toFloat())
            val p2c1y = h * (0.28f + 0.14f * kotlin.math.cos(t3.toDouble()).toFloat())
            val p2c2x = w * (0.28f + 0.16f * kotlin.math.cos((t4 * 0.75f).toDouble()).toFloat())
            val p2c2y = h * (0.58f + 0.12f * kotlin.math.sin((t2 * 0.9f).toDouble()).toFloat())
            val p2x1 = -w * 0.15f
            val p2y1 = h * (0.48f + 0.10f * kotlin.math.cos((t1 * 0.7f).toDouble()).toFloat())

            path2.rewind()
            path2.moveTo(p2x0, p2y0)
            path2.cubicTo(p2c1x, p2c1y, p2c2x, p2c2y, p2x1, p2y1)
            drawPath(
                path = path2,
                brush = Brush.linearGradient(
                    colors = listOf(safeSecColor.copy(alpha = 0.95f), safePrimary.copy(alpha = 0.85f), safeAccentColor.copy(alpha = 0.70f)),
                    start = Offset(p2x0, p2y0),
                    end = Offset(p2x1, p2y1)
                ),
                style = Stroke(
                    width = strokeWidthWorm * 1.05f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Gusano 3: Curva sinuosa inferior (safeAccentColor a safeSecColor)
            val p3x0 = -w * 0.15f
            val p3y0 = h * (0.72f + 0.11f * kotlin.math.sin(t3.toDouble()).toFloat())
            val p3c1x = w * (0.32f + 0.20f * kotlin.math.cos((t4 * 0.8f).toDouble()).toFloat())
            val p3c1y = h * (0.86f + 0.08f * kotlin.math.sin(t2.toDouble()).toFloat())
            val p3c2x = w * (0.70f + 0.15f * kotlin.math.sin((t1 * 0.65f).toDouble()).toFloat())
            val p3c2y = h * (0.64f + 0.13f * kotlin.math.cos(t3.toDouble()).toFloat())
            val p3x1 = w * 1.15f
            val p3y1 = h * (0.80f + 0.09f * kotlin.math.sin((t2 * 0.8f).toDouble()).toFloat())

            path3.rewind()
            path3.moveTo(p3x0, p3y0)
            path3.cubicTo(p3c1x, p3c1y, p3c2x, p3c2y, p3x1, p3y1)
            drawPath(
                path = path3,
                brush = Brush.linearGradient(
                    colors = listOf(safeAccentColor.copy(alpha = 0.95f), safeSecColor.copy(alpha = 0.80f), highlightColor.copy(alpha = 0.65f)),
                    start = Offset(p3x0, p3y0),
                    end = Offset(p3x1, p3y1)
                ),
                style = Stroke(
                    width = strokeWidthWorm,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Gusano 4: Gusano vertical-diagonal fluido que entrelaza las bandas
            val p4x0 = w * (0.45f + 0.18f * kotlin.math.sin((t2 * 0.7f).toDouble()).toFloat())
            val p4y0 = -h * 0.08f
            val p4c1x = w * (0.22f + 0.22f * kotlin.math.cos(t1.toDouble()).toFloat())
            val p4c1y = h * (0.38f + 0.10f * kotlin.math.sin(t4.toDouble()).toFloat())
            val p4c2x = w * (0.78f + 0.16f * kotlin.math.sin(t3.toDouble()).toFloat())
            val p4c2y = h * (0.62f + 0.12f * kotlin.math.cos((t1 * 0.85f).toDouble()).toFloat())
            val p4x1 = w * (0.50f + 0.20f * kotlin.math.cos(t2.toDouble()).toFloat())
            val p4y1 = h * 1.08f

            path4.rewind()
            path4.moveTo(p4x0, p4y0)
            path4.cubicTo(p4c1x, p4c1y, p4c2x, p4c2y, p4x1, p4y1)
            drawPath(
                path = path4,
                brush = Brush.linearGradient(
                    colors = listOf(highlightColor.copy(alpha = 0.85f), safePrimary.copy(alpha = 0.75f), safeAccentColor.copy(alpha = 0.80f)),
                    start = Offset(p4x0, p4y0),
                    end = Offset(p4x1, p4y1)
                ),
                style = Stroke(
                    width = strokeWidthWorm * 0.85f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // 3. Filtro medio oscuro elegante que da contraste perfecto al texto y a la cola/letras manteniendo los colores vivos
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.42f),
                    0.45f to Color.Black.copy(alpha = 0.36f),
                    1.0f to Color.Black.copy(alpha = 0.44f)
                )
            )
        }
    }
}

@Composable
private fun QueueItemRow(
    rowData: QueueItemRowData,
    contentColor: Color,
    isPlaying: Boolean,
    isCurrentPlayingItem: Boolean,
    context: android.content.Context,
    titleFontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    artistFontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    onClick: () -> Unit,
    isDragging: Boolean = false,
    isAnyDragging: Boolean = false,
    dragTranslationX: Float = 0f,
    dragTranslationY: Float = 0f,
    onDragStart: (Float, Float) -> Unit = { _, _ -> },
    onDragDelta: (Float, Float) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {}
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnClick by rememberUpdatedState(onClick)
    val haptic = LocalHapticFeedback.current
    val touchSlopPx = androidx.compose.ui.platform.LocalViewConfiguration.current.touchSlop
    var rowYInParent by remember { mutableFloatStateOf(0f) }
    var rowGlobalY by remember { mutableFloatStateOf(0f) }

    val pinnableContainer = androidx.compose.ui.layout.LocalPinnableContainer.current
    DisposableEffect(isDragging) {
        val handle = if (isDragging) pinnableContainer?.pin() else null
        onDispose {
            handle?.release()
        }
    }

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

    val imageModel = remember(upgradedArt, context) {
        ImageRequest.Builder(context)
            .data(upgradedArt)
            .size(140)
            .memoryCacheKey(upgradedArt?.toString())
            .crossfade(false)
            .build()
    }

    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        label = "dragScale"
    )
    val animatedElevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isDragging) 12.dp else 0.dp,
        label = "dragElevation"
    )

    val rowAlpha = if (isDragging) 0f else if (isAnyDragging) 0.45f else 1f
    val rowShape = RoundedCornerShape(14.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 50f else 0f)
            .onGloballyPositioned { coordinates ->
                rowYInParent = coordinates.positionInParent().y
                rowGlobalY = coordinates.positionInRoot().y
            }
            .graphicsLayer {
                translationX = dragTranslationX
                translationY = dragTranslationY
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = rowAlpha
            }
            .then(
                if (isDragging) {
                    Modifier
                        .shadow(
                            elevation = animatedElevation,
                            shape = rowShape,
                            ambientColor = Color.Black.copy(alpha = 0.45f),
                            spotColor = Color.Black.copy(alpha = 0.45f)
                        )
                        .background(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = rowShape
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = rowShape
                        )
                } else {
                    Modifier
                }
            )
            .clip(rowShape)
            .clickable(enabled = !isDragging, onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 8.dp)
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rowData.title,
                color = contentColor,
                fontSize = titleFontSize,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.40f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 6f
                    )
                )
            )
            Text(
                text = rowData.artist,
                color = contentColor.copy(alpha = 0.70f),
                fontSize = artistFontSize,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.35f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 5f
                    )
                )
            )
        }
        if (isCurrentPlayingItem) {
            PlayingEqualizer(color = contentColor, isPlaying = isPlaying, modifier = Modifier.size(22.dp))
        } else {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Reorder",
                tint = if (isDragging) Color.White else contentColor.copy(alpha = 0.50f),
                modifier = Modifier
                    .size(36.dp)
                    .padding(4.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Se consume desde el inicio para no duplicar el tap de la fila,
                            // pero el drag visual solo arranca tras superar el slop:
                            // soltar antes equivale a tap en el handle y selecciona la canción.
                            down.consume()
                            var pointerId = down.id
                            var totalDx = 0f
                            var totalDy = 0f
                            var dragStarted = false
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val dragChange = event.changes.firstOrNull { it.id == pointerId }
                                        ?: event.changes.firstOrNull { it.pressed }
                                    if (dragChange == null || !dragChange.pressed) {
                                        break
                                    }
                                    val deltaY = dragChange.position.y - dragChange.previousPosition.y
                                    val deltaX = dragChange.position.x - dragChange.previousPosition.x
                                    dragChange.consume()
                                    pointerId = dragChange.id
                                    if (!dragStarted) {
                                        totalDx += deltaX
                                        totalDy += deltaY
                                        if (kotlin.math.hypot(totalDx, totalDy) <= touchSlopPx) {
                                            continue
                                        }
                                        dragStarted = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentOnDragStart(rowYInParent + down.position.y, rowGlobalY)
                                        currentOnDragDelta(totalDx, totalDy)
                                        totalDx = 0f
                                        totalDy = 0f
                                        continue
                                    }
                                    currentOnDragDelta(deltaX, deltaY)
                                }
                            } finally {
                                if (dragStarted) {
                                    currentOnDragEnd()
                                } else {
                                    currentOnClick()
                                }
                            }
                        }
                    }
            )
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
    titleFontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    artistFontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    onClick: () -> Unit,
    isDragging: Boolean = false,
    isAnyDragging: Boolean = false,
    dragTranslationX: Float = 0f,
    dragTranslationY: Float = 0f,
    onDragStart: (Float, Float) -> Unit = { _, _ -> },
    onDragDelta: (Float, Float) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {}
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnClick by rememberUpdatedState(onClick)
    val haptic = LocalHapticFeedback.current
    val touchSlopPx = androidx.compose.ui.platform.LocalViewConfiguration.current.touchSlop
    var rowYInParent by remember { mutableFloatStateOf(0f) }
    var rowGlobalY by remember { mutableFloatStateOf(0f) }

    val pinnableContainer = androidx.compose.ui.layout.LocalPinnableContainer.current
    DisposableEffect(isDragging) {
        val handle = if (isDragging) pinnableContainer?.pin() else null
        onDispose {
            handle?.release()
        }
    }

    val hdThumb = remember(rowData.thumbnail) {
        rowData.thumbnail?.let {
            com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(it) ?: it
        } ?: rowData.thumbnail
    }

    val imageModel = remember(hdThumb, context) {
        ImageRequest.Builder(context)
            .data(hdThumb)
            .size(140)
            .memoryCacheKey(hdThumb)
            .crossfade(false)
            .build()
    }

    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        label = "dragScale"
    )
    val animatedElevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isDragging) 12.dp else 0.dp,
        label = "dragElevation"
    )

    val rowAlpha = if (isDragging) 0f else if (isAnyDragging) 0.45f else 1f
    val rowShape = RoundedCornerShape(14.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 50f else 0f)
            .onGloballyPositioned { coordinates ->
                rowYInParent = coordinates.positionInParent().y
                rowGlobalY = coordinates.positionInRoot().y
            }
            .graphicsLayer {
                translationX = dragTranslationX
                translationY = dragTranslationY
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = rowAlpha
            }
            .then(
                if (isDragging) {
                    Modifier
                        .shadow(
                            elevation = animatedElevation,
                            shape = rowShape,
                            ambientColor = Color.Black.copy(alpha = 0.45f),
                            spotColor = Color.Black.copy(alpha = 0.45f)
                        )
                        .background(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = rowShape
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = rowShape
                        )
                } else {
                    Modifier
                }
            )
            .clip(rowShape)
            .clickable(enabled = !isDragging, onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 8.dp)
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rowData.title,
                color = contentColor,
                fontSize = titleFontSize,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.40f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 6f
                    )
                )
            )
            Text(
                text = rowData.artist,
                color = contentColor.copy(alpha = 0.70f),
                fontSize = artistFontSize,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.35f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 5f
                    )
                )
            )
        }
        if (isCurrentPlayingItem) {
            PlayingEqualizer(color = contentColor, isPlaying = isPlaying, modifier = Modifier.size(22.dp))
        } else {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Reorder",
                tint = if (isDragging) Color.White else contentColor.copy(alpha = 0.50f),
                modifier = Modifier
                    .size(36.dp)
                    .padding(4.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Se consume desde el inicio para no duplicar el tap de la fila,
                            // pero el drag visual solo arranca tras superar el slop:
                            // soltar antes equivale a tap en el handle y selecciona la canción.
                            down.consume()
                            var pointerId = down.id
                            var totalDx = 0f
                            var totalDy = 0f
                            var dragStarted = false
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val dragChange = event.changes.firstOrNull { it.id == pointerId }
                                        ?: event.changes.firstOrNull { it.pressed }
                                    if (dragChange == null || !dragChange.pressed) {
                                        break
                                    }
                                    val deltaY = dragChange.position.y - dragChange.previousPosition.y
                                    val deltaX = dragChange.position.x - dragChange.previousPosition.x
                                    dragChange.consume()
                                    pointerId = dragChange.id
                                    if (!dragStarted) {
                                        totalDx += deltaX
                                        totalDy += deltaY
                                        if (kotlin.math.hypot(totalDx, totalDy) <= touchSlopPx) {
                                            continue
                                        }
                                        dragStarted = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentOnDragStart(rowYInParent + down.position.y, rowGlobalY)
                                        currentOnDragDelta(totalDx, totalDy)
                                        totalDx = 0f
                                        totalDy = 0f
                                        continue
                                    }
                                    currentOnDragDelta(deltaX, deltaY)
                                }
                            } finally {
                                if (dragStarted) {
                                    currentOnDragEnd()
                                } else {
                                    currentOnClick()
                                }
                            }
                        }
                    }
            )
        }
    }
}

private val skewLumCache1 = java.lang.ThreadLocal.withInitial { IntArray(256) }
private val skewLumCache2 = java.lang.ThreadLocal.withInitial { IntArray(256) }
private val skewPixelsCache1 = java.lang.ThreadLocal.withInitial { IntArray(256) }
private val skewPixelsCache2 = java.lang.ThreadLocal.withInitial { IntArray(256) }

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

    // Cache luminance values for rows with zero-allocation buffers and single native bulk reads
    val lum1 = if (w <= 256) skewLumCache1.get()!! else IntArray(w)
    val lum2 = if (w <= 256) skewLumCache2.get()!! else IntArray(w)
    val pix1 = if (w <= 256) skewPixelsCache1.get()!! else IntArray(w)
    val pix2 = if (w <= 256) skewPixelsCache2.get()!! else IntArray(w)

    bitmap.getPixels(pix1, 0, w, 0, y1, w, 1)
    bitmap.getPixels(pix2, 0, w, 0, y2, w, 1)

    for (x in 0 until w) {
        val p1 = pix1[x]
        lum1[x] = ((p1 shr 16 and 0xFF) * 3 + (p1 shr 8 and 0xFF) * 6 + (p1 and 0xFF)) / 10

        val p2 = pix2[x]
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

@Composable
private fun FloatingQueueDragCard(
    title: String,
    artist: String,
    artUrl: Any?,
    contentColor: Color,
    context: android.content.Context,
    titleFontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    artistFontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    val rowShape = RoundedCornerShape(14.dp)
    val upgradedArt = remember(artUrl) {
        artUrl?.let {
            val itStr = it.toString()
            if (itStr.startsWith("file:///android_asset/")) {
                it
            } else {
                val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(itStr) ?: itStr
                if (it is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
            }
        } ?: artUrl
    }

    val imageModel = remember(upgradedArt, context) {
        ImageRequest.Builder(context)
            .data(upgradedArt)
            .size(140)
            .memoryCacheKey(upgradedArt?.toString())
            .crossfade(false)
            .build()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = 1.04f
                scaleY = 1.04f
            }
            .shadow(
                elevation = 16.dp,
                shape = rowShape,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = Color.Black.copy(alpha = 0.55f)
            )
            .background(
                color = Color.White.copy(alpha = 0.24f),
                shape = rowShape
            )
            .border(
                width = 1.2.dp,
                color = Color.White.copy(alpha = 0.40f),
                shape = rowShape
            )
            .clip(rowShape)
            .padding(vertical = 5.dp, horizontal = 8.dp)
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = contentColor,
                fontSize = titleFontSize,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.40f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 6f
                    )
                )
            )
            Text(
                text = artist,
                color = contentColor.copy(alpha = 0.70f),
                fontSize = artistFontSize,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.35f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 5f
                    )
                )
            )
        }
        Icon(
            Icons.Default.Menu,
            contentDescription = "Reorder",
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .padding(4.dp)
        )
    }
}