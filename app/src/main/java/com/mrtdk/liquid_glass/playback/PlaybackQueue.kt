package com.mrtdk.liquid_glass.playback

import android.net.Uri
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import com.mrtdk.liquid_glass.ui.screens.QueueItem

object PlaybackQueue {
    @Volatile
    var currentSong: PlayerState? = null

    @Volatile
    var queue: List<QueueItem> = emptyList()

    @Volatile
    var upNextSongs: List<com.echo.innertube.models.SongItem> = emptyList()

    @Volatile
    var isExclusiveQueue: Boolean = false

    val songHistory = mutableListOf<PlayerState>()

    @Volatile
    var queueSeedVideoId: String? = null

    @Volatile
    var queueContinuation: String? = null

    @Volatile
    var queueEndpoint: com.echo.innertube.models.WatchEndpoint? = null

    @Volatile
    var onQueueChanged: (() -> Unit)? = null
    
    @Volatile
    var onCurrentSongChanged: ((PlayerState?) -> Unit)? = null

    @Synchronized
    fun getNextSongAndAdvance(repeatMode: Int = androidx.media3.common.Player.REPEAT_MODE_OFF): PlayerState? {
        val current = currentSong ?: return null
        if (queue.isNotEmpty()) {
            val next = queue.first()
            songHistory.add(current)
            
            val nextState = PlayerState(
                title = next.title,
                artist = next.artist,
                artUrl = next.artUrl,
                videoId = next.videoId,
                contentUri = null,
                queue = queue.drop(1),
                isExclusiveQueue = isExclusiveQueue,
                album = next.album
            )
            currentSong = nextState
            queue = queue.drop(1)
            onCurrentSongChanged?.invoke(nextState)
            onQueueChanged?.invoke()
            return nextState
        } else if (!isExclusiveQueue && upNextSongs.isNotEmpty()) {
            val next = upNextSongs.first()
            songHistory.add(current)
            
            val upgradedArt = next.thumbnail?.let {
                if (it.contains("=w") || it.contains("=s")) {
                    val idx = it.indexOf("=w").takeIf { j -> j != -1 } ?: it.indexOf("=s")
                    it.substring(0, idx) + "=w1200-h1200-l90-rj"
                }
                else if (it.contains("ytimg.com/vi/")) it.replace("hqdefault", "maxresdefault").replace("mqdefault", "maxresdefault")
                else it
            } ?: next.thumbnail

            val nextState = PlayerState(
                title = next.title,
                artist = next.artists.joinToString { it.name },
                artUrl = upgradedArt,
                videoId = next.id,
                contentUri = null,
                isExclusiveQueue = false,
                album = next.album?.name
            )
            currentSong = nextState
            upNextSongs = upNextSongs.drop(1)
            onCurrentSongChanged?.invoke(nextState)
            onQueueChanged?.invoke()
            return nextState
        } else if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ALL && songHistory.isNotEmpty()) {
            val allSongs = songHistory + listOf(current)
            songHistory.clear()
            val first = allSongs.first()
            val remaining = allSongs.drop(1).map { state ->
                QueueItem(
                    title = state.title,
                    artist = state.artist,
                    artUrl = state.artUrl,
                    videoId = state.videoId,
                    album = state.album
                )
            }
            val nextState = PlayerState(
                title = first.title,
                artist = first.artist,
                artUrl = first.artUrl,
                videoId = first.videoId,
                contentUri = first.contentUri,
                queue = remaining,
                isExclusiveQueue = isExclusiveQueue,
                album = first.album
            )
            currentSong = nextState
            queue = remaining
            onCurrentSongChanged?.invoke(nextState)
            onQueueChanged?.invoke()
            return nextState
        }
        return null
    }

    @Synchronized
    fun getPreviousSongAndGoBack(): PlayerState? {
        if (songHistory.isNotEmpty()) {
            val prev = songHistory.removeLast()
            currentSong = prev
            onCurrentSongChanged?.invoke(prev)
            onQueueChanged?.invoke()
            return prev
        }
        return null
    }
}
