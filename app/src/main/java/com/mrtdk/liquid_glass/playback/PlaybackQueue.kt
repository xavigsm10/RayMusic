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

    @Volatile
    var isAutomixEnabled: Boolean = false

    val songHistory = mutableListOf<PlayerState>()

    private fun addToHistory(state: PlayerState) {
        songHistory.add(state)
        val maxSize = com.mrtdk.liquid_glass.utils.PerformanceProfileManager.getConfig().maxHistorySize
        while (songHistory.size > maxSize) {
            songHistory.removeAt(0)
        }
    }

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

    @Volatile
    var isAutoMixing: Boolean = false
        set(value) {
            field = value
            onAutoMixTransitionChanged?.invoke(value)
        }

    @Volatile
    var onAutoMixTransitionChanged: ((Boolean) -> Unit)? = null

    @Synchronized
    fun getNextSongAndAdvance(repeatMode: Int = androidx.media3.common.Player.REPEAT_MODE_OFF): PlayerState? {
        val current = currentSong ?: return null
        if (queue.isNotEmpty()) {
            val next = queue.first()
            addToHistory(current)
            
            val nextState = PlayerState(
                title = next.title,
                artist = next.artist,
                artUrl = next.artUrl?.let {
                    val itStr = it.toString()
                    if (itStr.startsWith("file:///android_asset/")) {
                        it
                    } else {
                        val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(itStr) ?: itStr
                        if (it is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
                    }
                } ?: next.artUrl,
                videoId = next.videoId,
                contentUri = null,
                queue = queue.drop(1),
                isExclusiveQueue = isExclusiveQueue,
                album = next.album,
                albumId = next.albumId,
                playlistId = next.playlistId,
                playlistName = next.playlistName
            )
            currentSong = nextState
            queue = queue.drop(1)
            onCurrentSongChanged?.invoke(nextState)
            onQueueChanged?.invoke()
            return nextState
        } else if (upNextSongs.isNotEmpty()) {
            val next = upNextSongs.first()
            addToHistory(current)
            
            val upgradedArt = next.thumbnail?.let {
                com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(it) ?: it
            } ?: next.thumbnail

            val nextState = PlayerState(
                title = next.title,
                artist = next.artists.joinToString { it.name },
                artUrl = upgradedArt,
                videoId = next.id,
                contentUri = null,
                isExclusiveQueue = isExclusiveQueue,
                album = next.album?.name,
                albumId = next.album?.id
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
                    album = state.album,
                    albumId = state.albumId,
                    playlistId = state.playlistId,
                    playlistName = state.playlistName
                )
            }
            val nextState = PlayerState(
                title = first.title,
                artist = first.artist,
                artUrl = first.artUrl?.let {
                    val itStr = it.toString()
                    if (itStr.startsWith("file:///android_asset/")) {
                        it
                    } else {
                        val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(itStr) ?: itStr
                        if (it is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
                    }
                } ?: first.artUrl,
                videoId = first.videoId,
                contentUri = first.contentUri,
                queue = remaining,
                isExclusiveQueue = isExclusiveQueue,
                album = first.album,
                albumId = first.albumId,
                playlistId = first.playlistId,
                playlistName = first.playlistName
            )
            currentSong = nextState
            queue = remaining
            onCurrentSongChanged?.invoke(nextState)
            onQueueChanged?.invoke()
            return nextState
        }

        // Fallback: si cola y upNext están vacías (por ejemplo reproducción individual directa),
        // avanzar a temas guardados o escuchados recientemente para que el botón/gesto siempre responda
        val saved = com.mrtdk.liquid_glass.data.LibraryManager.savedItems.value
            .filter { it.type == com.mrtdk.liquid_glass.data.ItemType.SONG }
            .filter { it.id != current.videoId }
        if (saved.isNotEmpty()) {
            val nextItem = saved.shuffled().first()
            addToHistory(current)
            val nextState = PlayerState(
                title = nextItem.title,
                artist = nextItem.subtitle,
                artUrl = nextItem.thumbnail,
                videoId = nextItem.id,
                contentUri = null,
                isExclusiveQueue = false,
                album = nextItem.album
            )
            currentSong = nextState
            onCurrentSongChanged?.invoke(nextState)
            onQueueChanged?.invoke()
            return nextState
        }

        val recent = com.mrtdk.liquid_glass.data.LibraryManager.recentlyPlayed.value
            .filter { it.type == com.mrtdk.liquid_glass.data.ItemType.SONG }
            .filter { it.id != current.videoId }
        if (recent.isNotEmpty()) {
            val nextItem = recent.shuffled().first()
            addToHistory(current)
            val nextState = PlayerState(
                title = nextItem.title,
                artist = nextItem.subtitle,
                artUrl = nextItem.thumbnail,
                videoId = nextItem.id,
                contentUri = null,
                isExclusiveQueue = false,
                album = nextItem.album
            )
            currentSong = nextState
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
            val cur = currentSong
            // Devolver la actual al frente de la cola para que siguiente/anterior
            // sean simétricos y funcionen siempre (A→B→A→B...). Solo temas online
            // (QueueItem no guarda contentUri de locales).
            if (cur != null && cur.videoId != null &&
                queue.firstOrNull()?.videoId != cur.videoId
            ) {
                queue = listOf(
                    QueueItem(
                        title = cur.title,
                        artist = cur.artist,
                        artUrl = cur.artUrl,
                        videoId = cur.videoId,
                        album = cur.album,
                        albumId = cur.albumId,
                        playlistId = cur.playlistId,
                        playlistName = cur.playlistName
                    )
                ) + queue
            }
            currentSong = prev
            onCurrentSongChanged?.invoke(prev)
            onQueueChanged?.invoke()
            return prev
        }

        // Fallback para retroceder si el historial en memoria está vacío (p. ej. recién iniciada la reproducción)
        val cur = currentSong
        val recent = com.mrtdk.liquid_glass.data.LibraryManager.recentlyPlayed.value
            .filter { it.type == com.mrtdk.liquid_glass.data.ItemType.SONG }
            .filter { it.id != cur?.videoId }
        if (recent.isNotEmpty()) {
            val prevItem = recent.first()
            val prevState = PlayerState(
                title = prevItem.title,
                artist = prevItem.subtitle,
                artUrl = prevItem.thumbnail,
                videoId = prevItem.id,
                contentUri = null,
                isExclusiveQueue = false,
                album = prevItem.album
            )
            if (cur != null && cur.videoId != null && queue.firstOrNull()?.videoId != cur.videoId) {
                queue = listOf(
                    QueueItem(
                        title = cur.title,
                        artist = cur.artist,
                        artUrl = cur.artUrl,
                        videoId = cur.videoId,
                        album = cur.album,
                        albumId = cur.albumId,
                        playlistId = cur.playlistId,
                        playlistName = cur.playlistName
                    )
                ) + queue
            }
            currentSong = prevState
            onCurrentSongChanged?.invoke(prevState)
            onQueueChanged?.invoke()
            return prevState
        }

        return null
    }

    @Synchronized
    fun peekNextSong(repeatMode: Int): PlayerState? {
        val current = currentSong ?: return null
        if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) {
            return current
        }
        if (queue.isNotEmpty()) {
            val next = queue.first()
            val upgradedArt = next.artUrl?.let {
                val itStr = it.toString()
                if (itStr.startsWith("file:///android_asset/")) {
                    it
                } else {
                    val upgraded = com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(itStr) ?: itStr
                    if (it is android.net.Uri) android.net.Uri.parse(upgraded) else upgraded
                }
            } ?: next.artUrl

            return PlayerState(
                title = next.title,
                artist = next.artist,
                artUrl = upgradedArt,
                videoId = next.videoId,
                contentUri = null,
                queue = queue.drop(1),
                isExclusiveQueue = isExclusiveQueue,
                album = next.album,
                albumId = next.albumId,
                playlistId = next.playlistId,
                playlistName = next.playlistName
            )
        } else if (upNextSongs.isNotEmpty()) {
            val next = upNextSongs.first()
            val upgradedArt = next.thumbnail?.let {
                com.mrtdk.liquid_glass.utils.CoilUtils.upgradeThumbQuality(it) ?: it
            } ?: next.thumbnail

            return PlayerState(
                title = next.title,
                artist = next.artists.joinToString { it.name },
                artUrl = upgradedArt,
                videoId = next.id,
                contentUri = null,
                isExclusiveQueue = isExclusiveQueue,
                album = next.album?.name,
                albumId = next.album?.id
            )
        } else if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ALL && songHistory.isNotEmpty()) {
            val allSongs = songHistory + listOf(current)
            val first = allSongs.first()
            val remaining = allSongs.drop(1).map { state ->
                QueueItem(
                    title = state.title,
                    artist = state.artist,
                    artUrl = state.artUrl,
                    videoId = state.videoId,
                    album = state.album,
                    albumId = state.albumId,
                    playlistId = state.playlistId,
                    playlistName = state.playlistName
                )
            }
            return PlayerState(
                title = first.title,
                artist = first.artist,
                artUrl = first.artUrl,
                videoId = first.videoId,
                contentUri = first.contentUri,
                queue = remaining,
                isExclusiveQueue = isExclusiveQueue,
                album = first.album,
                albumId = first.albumId,
                playlistId = first.playlistId,
                playlistName = first.playlistName
            )
        }
        return null
    }
}
