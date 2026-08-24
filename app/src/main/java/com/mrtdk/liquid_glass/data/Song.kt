package com.mrtdk.liquid_glass.data

import android.net.Uri

@androidx.compose.runtime.Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumArtUri: Uri?,
    val contentUri: Uri
)