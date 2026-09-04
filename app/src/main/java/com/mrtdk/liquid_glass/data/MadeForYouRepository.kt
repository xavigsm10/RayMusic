package com.mrtdk.liquid_glass.data

import androidx.compose.ui.graphics.Color
import com.echo.innertube.models.SongItem
import java.util.concurrent.ConcurrentHashMap

data class MadeForYouPlaylist(
    val id: String,
    val title: String,
    val artistsSubtitle: String,
    val gradientColors: List<Color>,
    val seedSong: SongItem? = null,
    val songs: List<SongItem> = emptyList()
)

object MadeForYouRepository {
    private val playlists = ConcurrentHashMap<String, MadeForYouPlaylist>()

    fun register(playlist: MadeForYouPlaylist) {
        playlists[playlist.id] = playlist
    }

    fun registerAll(list: List<MadeForYouPlaylist>) {
        list.forEach { playlists[it.id] = it }
    }

    fun get(id: String): MadeForYouPlaylist? = playlists[id]

    fun getAll(): List<MadeForYouPlaylist> = playlists.values.toList()

    fun clear() {
        playlists.clear()
    }
}
