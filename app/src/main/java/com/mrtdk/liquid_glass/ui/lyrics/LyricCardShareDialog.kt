package com.mrtdk.liquid_glass.ui.lyrics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.mrtdk.liquid_glass.data.lyrics.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class CardAspectRatio {
    STORY_9_16,
    SQUARE_1_1
}

/**
 * Lyric Card Generator and Sharing Dialog.
 *
 * Produces aesthetic liquid-glass cards for Instagram Stories, WhatsApp, and social media.
 */
@Composable
fun LyricCardShareDialog(
    selectedLines: List<LyricLine>,
    songTitle: String,
    artistName: String,
    artUrl: Any?,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedRatio by remember { mutableStateOf(CardAspectRatio.STORY_9_16) }
    var isExporting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.94f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF18181D))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Compartir Letra",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tarjeta estética estilo Apple Music",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { onDismissRequest() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Aspect Ratio Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isStory = selectedRatio == CardAspectRatio.STORY_9_16
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isStory) Color.White.copy(alpha = 0.20f) else Color.Transparent)
                            .clickable { selectedRatio = CardAspectRatio.STORY_9_16 }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "📱 Historia (9:16)",
                            color = if (isStory) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = if (isStory) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    val isSquare = selectedRatio == CardAspectRatio.SQUARE_1_1
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSquare) Color.White.copy(alpha = 0.20f) else Color.Transparent)
                            .clickable { selectedRatio = CardAspectRatio.SQUARE_1_1 }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "🔲 Cuadrada (1:1)",
                            color = if (isSquare) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = if (isSquare) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Card Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LyricCardPreview(
                        lines = selectedLines,
                        songTitle = songTitle,
                        artistName = artistName,
                        artUrl = artUrl,
                        ratio = selectedRatio
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Copiar Texto
                    OutlinedButton(
                        onClick = {
                            val plainLyrics = selectedLines.joinToString("\n") { it.text }
                            val shareText = "\"$plainLyrics\"\n— $songTitle · $artistName\nEscuchando en RayMusic"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Letra", shareText))
                            Toast.makeText(context, "Letra copiada al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar", fontSize = 13.sp)
                    }

                    // Guardar en Galería
                    OutlinedButton(
                        onClick = {
                            if (isExporting) return@OutlinedButton
                            isExporting = true
                            coroutineScope.launch {
                                val bitmap = generateCardBitmap(context, selectedLines, songTitle, artistName, artUrl, selectedRatio)
                                saveBitmapToGallery(context, bitmap, songTitle)
                                isExporting = false
                            }
                        },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar", fontSize = 13.sp)
                    }

                    // Compartir en Redes
                    Button(
                        onClick = {
                            if (isExporting) return@Button
                            isExporting = true
                            coroutineScope.launch {
                                val bitmap = generateCardBitmap(context, selectedLines, songTitle, artistName, artUrl, selectedRatio)
                                shareBitmap(context, bitmap, songTitle, artistName)
                                isExporting = false
                            }
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Visual Preview of the aesthetic lyric card.
 */
@Composable
private fun LyricCardPreview(
    lines: List<LyricLine>,
    songTitle: String,
    artistName: String,
    artUrl: Any?,
    ratio: CardAspectRatio
) {
    val isStory = ratio == CardAspectRatio.STORY_9_16
    val cardModifier = if (isStory) {
        Modifier
            .fillMaxHeight(0.95f)
            .aspectRatio(9f / 16f)
    } else {
        Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(1f)
    }

    Box(
        modifier = cardModifier
            .shadow(20.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF281C3E),
                        Color(0xFF1E284A),
                        Color(0xFF121422)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Artwork & Track info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = artUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (isStory) 52.dp else 46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songTitle,
                        color = Color.White,
                        fontSize = if (isStory) 15.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artistName,
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = if (isStory) 13.sp else 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Middle: Lyrics Verses
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = if (isStory) 24.dp else 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "“",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = if (isStory) 42.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 24.sp
                )

                lines.take(6).forEach { line ->
                    Text(
                        text = line.text,
                        color = Color.White,
                        fontSize = when {
                            lines.size <= 2 && isStory -> 22.sp
                            lines.size <= 3 -> 17.sp
                            else -> 14.sp
                        },
                        fontWeight = FontWeight.Bold,
                        lineHeight = when {
                            lines.size <= 2 && isStory -> 28.sp
                            lines.size <= 3 -> 23.sp
                            else -> 19.sp
                        },
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }

            // Bottom: RayMusic Branding
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♪", color = Color.White, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RayMusic",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "Letras en tiempo real",
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Generates an ultra-crisp Bitmap representation of the lyric card for export.
 */
private suspend fun generateCardBitmap(
    context: Context,
    lines: List<LyricLine>,
    songTitle: String,
    artistName: String,
    artUrl: Any?,
    ratio: CardAspectRatio
): Bitmap = withContext(Dispatchers.IO) {
    val width = 1080
    val height = if (ratio == CardAspectRatio.STORY_9_16) 1920 else 1080

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 1. Fluid gradient background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                android.graphics.Color.parseColor("#2C1844"),
                android.graphics.Color.parseColor("#1B2A4A"),
                android.graphics.Color.parseColor("#11131E")
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // 2. Load Artwork Bitmap if possible
    var artworkBitmap: Bitmap? = null
    try {
        if (artUrl != null) {
            val req = ImageRequest.Builder(context)
                .data(artUrl)
                .allowHardware(false)
                .build()
            val result = Coil.imageLoader(context).execute(req)
            if (result is SuccessResult) {
                val drawable = result.drawable
                val bmp = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                drawable.setBounds(0, 0, c.width, c.height)
                drawable.draw(c)
                artworkBitmap = bmp
            }
        }
    } catch (_: Exception) {}

    val isStory = ratio == CardAspectRatio.STORY_9_16
    val padding = 90f
    var currentY = padding + 60f

    // 3. Draw Track Artwork & Info
    val artSize = if (isStory) 160f else 130f
    val artRect = RectF(padding, currentY, padding + artSize, currentY + artSize)
    val artRadius = 36f

    if (artworkBitmap != null) {
        val roundedArt = getRoundedCornerBitmap(artworkBitmap, artRadius)
        canvas.drawBitmap(roundedArt, null, artRect, null)
    } else {
        val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#33FFFFFF")
        }
        canvas.drawRoundRect(artRect, artRadius, artRadius, placeholderPaint)
    }

    // Title and Artist text
    val textX = padding + artSize + 36f
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = if (isStory) 52f else 44f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#B3FFFFFF")
        textSize = if (isStory) 40f else 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    val maxTextWidth = (width - textX - padding).toInt().coerceAtLeast(100)
    val titleLayout = StaticLayout.Builder.obtain(songTitle, 0, songTitle.length, titlePaint, maxTextWidth)
        .setMaxLines(1)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()

    val artistLayout = StaticLayout.Builder.obtain(artistName, 0, artistName.length, artistPaint, maxTextWidth)
        .setMaxLines(1)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()

    canvas.save()
    canvas.translate(textX, currentY + (artSize - titleLayout.height - artistLayout.height) / 2f)
    titleLayout.draw(canvas)
    canvas.translate(0f, titleLayout.height.toFloat() + 10f)
    artistLayout.draw(canvas)
    canvas.restore()

    currentY += artSize + (if (isStory) 140f else 80f)

    // 4. Quotation Mark
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#55FFFFFF")
        textSize = if (isStory) 130f else 100f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("“", padding, currentY, quotePaint)
    currentY += if (isStory) 70f else 50f

    // 5. Selected Lyric Lines
    val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = when {
            lines.size <= 2 && isStory -> 64f
            lines.size <= 3 -> 52f
            else -> 42f
        }
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val maxLyricWidth = (width - padding * 2).toInt().coerceAtLeast(100)
    for (line in lines.take(6)) {
        val layout = StaticLayout.Builder.obtain(line.text, 0, line.text.length, lyricPaint, maxLyricWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

        canvas.save()
        canvas.translate(padding, currentY)
        layout.draw(canvas)
        canvas.restore()

        currentY += layout.height + (if (isStory) 34f else 24f)
    }

    // 6. Bottom RayMusic Branding
    val brandY = height - padding - 40f
    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#80FFFFFF")
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("♪  RayMusic", padding, brandY, brandPaint)

    val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#55FFFFFF")
        textSize = 28f
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("Letras de Apple Music", width - padding, brandY, watermarkPaint)

    bitmap
}

/**
 * Creates rounded bitmap for album art.
 */
private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float): Bitmap {
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rect = Rect(0, 0, bitmap.width, bitmap.height)
    val rectF = RectF(rect)
    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)
    return output
}

/**
 * Shares Bitmap directly via FileProvider and Android Sharesheet.
 */
private fun shareBitmap(context: Context, bitmap: Bitmap, title: String, artist: String) {
    try {
        val cachePath = File(context.cacheDir, "shared_lyrics")
        cachePath.mkdirs()
        val file = File(cachePath, "lyric_card_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.FileProvider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, "Letras de \"$title\" - $artist en RayMusic")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Compartir letra"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al compartir: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Saves Bitmap to device photo gallery via MediaStore.
 */
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String) {
    try {
        val filename = "RayMusic_Lyrics_${title.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "RayMusic")
            }
            val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = context.contentResolver.openOutputStream(imageUri)
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + "RayMusic"
            val file = File(imagesDir)
            if (!file.exists()) file.mkdirs()
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "Tarjeta guardada en la galería", Toast.LENGTH_SHORT).show()
        } ?: throw IllegalStateException("No se pudo abrir el stream de guardado")
    } catch (e: Exception) {
        Toast.makeText(context, "Error al guardar en galería: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
