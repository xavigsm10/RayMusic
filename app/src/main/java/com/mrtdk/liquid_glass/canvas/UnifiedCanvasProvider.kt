package com.mrtdk.liquid_glass.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UnifiedCanvasProvider {

    /**
     * Resolves animated moving artwork for albums and songs in priority order:
     * 1. EchoMusicCanvasProvider (direct JSON manifest - instant)
     * 2. ArtistVideoCanvasProvider (ArchiveTune/Koiiverse)
     * 3. TidalCanvasProvider (Tidal video cover)
     * 4. AppleMusicCanvasProvider (Apple Music AMP)
     */
    suspend fun getSongOrAlbumCanvas(
        songOrAlbum: String,
        artist: String,
        album: String? = null
    ): String? = withContext(Dispatchers.IO) {
        if (songOrAlbum.isBlank() || artist.isBlank()) return@withContext null

        val cleanTitle = songOrAlbum.trim()
        val cleanArtist = artist.trim()
        val cleanAlbum = album?.trim()

        // 1. EchoMusic Canvas Manifest
        try {
            val echoRes = EchoMusicCanvasProvider.getBySongArtist(cleanTitle, cleanArtist)
            if (!echoRes?.preferredAnimationUrl.isNullOrBlank()) {
                return@withContext echoRes?.preferredAnimationUrl
            }
        } catch (_: Exception) {}

        // 2. ArtistVideo (ArchiveTune / Koiiverse)
        try {
            val archiveTuneRes = ArtistVideoCanvasProvider.getBySongArtist(cleanTitle, cleanArtist, cleanAlbum)
            if (!archiveTuneRes?.preferredAnimationUrl.isNullOrBlank()) {
                return@withContext archiveTuneRes?.preferredAnimationUrl
            }
        } catch (_: Exception) {}

        // 3. Tidal Video Covers
        try {
            val tidalRes = if (!cleanAlbum.isNullOrBlank()) {
                TidalCanvasProvider.getByAlbumArtist(cleanAlbum, cleanArtist)
                    ?: TidalCanvasProvider.getBySongArtist(cleanTitle, cleanArtist, cleanAlbum)
            } else {
                TidalCanvasProvider.getBySongArtist(cleanTitle, cleanArtist)
            }
            if (!tidalRes?.preferredAnimationUrl.isNullOrBlank()) {
                return@withContext tidalRes?.preferredAnimationUrl
            }
        } catch (_: Exception) {}

        // 4. Apple Music Editorial Video / Motion Artwork
        try {
            val amRes = if (!cleanAlbum.isNullOrBlank()) {
                AppleMusicCanvasProvider.getByAlbumArtist(cleanAlbum, cleanArtist)
                    ?: AppleMusicCanvasProvider.getBySongArtist(cleanTitle, cleanArtist, cleanAlbum)
            } else {
                AppleMusicCanvasProvider.getBySongArtist(cleanTitle, cleanArtist)
            }
            if (!amRes?.preferredAnimationUrl.isNullOrBlank()) {
                return@withContext amRes?.preferredAnimationUrl
            }
        } catch (_: Exception) {}

        null
    }

    /**
     * Resolves artist motion video header for ArtistScreen.
     */
    suspend fun getArtistMotionVideo(artistName: String): String? = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext null
        try {
            AppleMusicArtistBackgroundProvider.getByArtistName(artistName)
        } catch (_: Exception) {
            null
        }
    }
}
