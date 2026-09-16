package com.mrtdk.liquid_glass.data.lyrics

import com.echo.innertube.YouTube
import com.echo.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lyrics exposed by YouTube Music's Lyrics tab for the exact playing video. */
object YouTubeMusicLyrics {
    suspend fun lyrics(videoId: String): List<LyricLine>? = withContext(Dispatchers.IO) {
        if (!YOUTUBE_ID.matches(videoId)) return@withContext null
        val next = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull() ?: return@withContext null
        val endpoint = next.lyricsEndpoint ?: return@withContext null
        val text = YouTube.lyrics(endpoint).getOrNull()?.trim() ?: return@withContext null
        text.lineSequence().map(String::trim).filter(String::isNotEmpty)
            .map { LyricLine(0L, it) }.toList().takeIf { it.isNotEmpty() }
    }
}

/** Timed YouTube transcript/captions for the exact playing video. */
object YouTubeTranscriptLyrics {
    suspend fun lyrics(videoId: String): List<LyricLine>? = withContext(Dispatchers.IO) {
        if (!YOUTUBE_ID.matches(videoId)) return@withContext null
        val transcriptStr = YouTube.transcript(videoId).getOrNull() ?: return@withContext null
        LrcLib.parseLrc(transcriptStr).takeIf { it.isNotEmpty() }
    }
}

private val YOUTUBE_ID = Regex("""[A-Za-z0-9_-]{11}""")
