package com.mrtdk.liquid_glass.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import android.net.Uri

enum class ItemType { ALBUM, ARTIST, SONG }

data class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnail: String?,
    val type: ItemType,
    val album: String? = null
)

data class Playlist(
    val id: String,
    val name: String,
    val items: List<LibraryItem>,
    val coverUrl: String? = null,
    val isPinned: Boolean = false
)

object LibraryManager {
    private const val PREFS_NAME = "liquid_glass_library"
    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context

    private val _savedItems = MutableStateFlow<List<LibraryItem>>(emptyList())
    val savedItems: StateFlow<List<LibraryItem>> = _savedItems

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _recentlyPlayed = MutableStateFlow<List<LibraryItem>>(emptyList())
    val recentlyPlayed: StateFlow<List<LibraryItem>> = _recentlyPlayed

    private val _downloadedSongs = MutableStateFlow<List<LibraryItem>>(emptyList())
    val downloadedSongs: StateFlow<List<LibraryItem>> = _downloadedSongs

    private fun parseItemType(value: String): ItemType? {
        return try {
            ItemType.valueOf(value)
        } catch (e: Exception) {
            null
        }
    }

    fun init(context: Context) {
        this.context = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString("saved_items", "") ?: ""
        if (saved.isNotEmpty()) {
            val items = saved.split("|||").mapNotNull { itemStr ->
                val parts = itemStr.split("||")
                if (parts.size >= 5) {
                    val type = parseItemType(parts[4]) ?: return@mapNotNull null
                    val albumVal = if (parts.size >= 6) parts[5].takeIf { it.isNotBlank() } else null
                    LibraryItem(
                        id = parts[0],
                        title = parts[1],
                        subtitle = parts[2],
                        thumbnail = parts[3].takeIf { it.isNotBlank() },
                        type = type,
                        album = albumVal
                    )
                } else null
            }
            _savedItems.value = items
        }
        
        val savedPlaylists = prefs.getString("playlists", "") ?: ""
        if (savedPlaylists.isNotEmpty()) {
            val pl = savedPlaylists.split("@@@").mapNotNull { pStr ->
                val pParts = pStr.split("@@")
                if (pParts.size >= 3) {
                    val id = pParts[0]
                    val name = pParts[1]
                    val pItemsStr = pParts[2]
                    val coverUrl = if (pParts.size >= 4) pParts[3].takeIf { it.isNotBlank() } else null
                    val isPinned = if (pParts.size >= 5) pParts[4] == "true" else false
                    val pItems = if (pItemsStr.isNotBlank()) pItemsStr.split("|||").mapNotNull { itemStr ->
                        val parts = itemStr.split("||")
                        if (parts.size >= 5) {
                            val type = parseItemType(parts[4]) ?: return@mapNotNull null
                            val albumVal = if (parts.size >= 6) parts[5].takeIf { it.isNotBlank() } else null
                            LibraryItem(parts[0], parts[1], parts[2], parts[3].takeIf { it.isNotBlank() }, type, albumVal)
                        } else null
                    } else emptyList()
                    Playlist(id, name, pItems, coverUrl, isPinned)
                } else null
            }
            _playlists.value = pl
        }

        // Load recently played
        val savedRecent = prefs.getString("recently_played", "") ?: ""
        if (savedRecent.isNotEmpty()) {
            val items = savedRecent.split("|||").mapNotNull { itemStr ->
                val parts = itemStr.split("||")
                if (parts.size >= 5) {
                    val type = parseItemType(parts[4]) ?: return@mapNotNull null
                    val albumVal = if (parts.size >= 6) parts[5].takeIf { it.isNotBlank() } else null
                    LibraryItem(parts[0], parts[1], parts[2], parts[3].takeIf { it.isNotBlank() }, type, albumVal)
                } else null
            }
            _recentlyPlayed.value = items
        }

        // Load downloaded songs
        val savedDownloaded = prefs.getString("downloaded_songs", "") ?: ""
        if (savedDownloaded.isNotEmpty()) {
            val items = savedDownloaded.split("|||").mapNotNull { itemStr ->
                val parts = itemStr.split("||")
                if (parts.size >= 5) {
                    val type = parseItemType(parts[4]) ?: return@mapNotNull null
                    val albumVal = if (parts.size >= 6) parts[5].takeIf { it.isNotBlank() } else null
                    LibraryItem(parts[0], parts[1], parts[2], parts[3].takeIf { it.isNotBlank() }, type, albumVal)
                } else null
            }
            _downloadedSongs.value = items
        }
    }

    fun saveItem(item: LibraryItem) {
        if (_savedItems.value.any { it.id == item.id }) return
        val newList = listOf(item) + _savedItems.value
        _savedItems.value = newList
        saveToPrefs(newList)
    }

    fun saveDownloadedSong(item: LibraryItem) {
        if (_downloadedSongs.value.any { it.id == item.id }) return
        val newList = listOf(item) + _downloadedSongs.value
        _downloadedSongs.value = newList
        val serialized = newList.joinToString("|||") { 
            "${it.id}||${it.title}||${it.subtitle}||${it.thumbnail ?: ""}||${it.type.name}||${it.album ?: ""}" 
        }
        prefs.edit().putString("downloaded_songs", serialized).apply()
    }

    fun removeItem(id: String) {
        val newList = _savedItems.value.filter { it.id != id }
        _savedItems.value = newList
        saveToPrefs(newList)
    }

    private fun saveToPrefs(list: List<LibraryItem>) {
        val serialized = list.joinToString("|||") { 
            "${it.id}||${it.title}||${it.subtitle}||${it.thumbnail ?: ""}||${it.type.name}||${it.album ?: ""}" 
        }
        prefs.edit().putString("saved_items", serialized).apply()
    }

    fun addRecentlyPlayed(item: LibraryItem) {
        val filtered = _recentlyPlayed.value.filter { it.id != item.id }
        val newList = (listOf(item) + filtered).take(30)
        _recentlyPlayed.value = newList
        saveRecentlyPlayedToPrefs(newList)
    }

    private fun saveRecentlyPlayedToPrefs(list: List<LibraryItem>) {
        val serialized = list.joinToString("|||") {
            "${it.id}||${it.title}||${it.subtitle}||${it.thumbnail ?: ""}||${it.type.name}||${it.album ?: ""}"
        }
        prefs.edit().putString("recently_played", serialized).apply()
    }

    fun savePlaylistCover(context: Context, playlistId: String, sourceUri: Uri): String? {
        return try {
            val coversDir = java.io.File(context.filesDir, "playlist_covers")
            if (!coversDir.exists()) {
                coversDir.mkdirs()
            }
            val targetFile = java.io.File(coversDir, "${playlistId}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                java.io.FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createPlaylist(name: String, coverUrl: String? = null) {
        val id = java.util.UUID.randomUUID().toString()
        val finalCoverUrl = if (coverUrl != null && coverUrl.startsWith("content://")) {
            savePlaylistCover(context, id, Uri.parse(coverUrl))
        } else {
            coverUrl
        }
        val newList = listOf(Playlist(id, name, emptyList(), finalCoverUrl, false)) + _playlists.value
        _playlists.value = newList
        savePlaylistsToPrefs(newList)
    }

    fun updatePlaylist(playlistId: String, name: String, coverUrl: String?) {
        val newList = _playlists.value.map {
            if (it.id == playlistId) {
                it.copy(name = name, coverUrl = coverUrl)
            } else it
        }
        _playlists.value = newList
        savePlaylistsToPrefs(newList)
    }

    fun togglePinPlaylist(playlistId: String) {
        val newList = _playlists.value.map { if (it.id == playlistId) it.copy(isPinned = !it.isPinned) else it }
        _playlists.value = newList
        savePlaylistsToPrefs(newList)
    }

    fun deletePlaylist(playlistId: String) {
        val newList = _playlists.value.filter { it.id != playlistId }
        _playlists.value = newList
        savePlaylistsToPrefs(newList)
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val newList = _playlists.value.map { if (it.id == playlistId) it.copy(name = newName) else it }
        _playlists.value = newList
        savePlaylistsToPrefs(newList)
    }

    fun addSongToPlaylist(playlistId: String, song: LibraryItem) {
        val newList = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                if (pl.items.any { it.id == song.id }) pl else pl.copy(items = listOf(song) + pl.items)
            } else pl
        }
        _playlists.value = newList
        savePlaylistsToPrefs(newList)
    }

    private fun savePlaylistsToPrefs(list: List<Playlist>) {
        val serialized = list.joinToString("@@@") { pl ->
            val itemsStr = pl.items.joinToString("|||") { "${it.id}||${it.title}||${it.subtitle}||${it.thumbnail ?: ""}||${it.type.name}||${it.album ?: ""}" }
            "${pl.id}@@${pl.name}@@$itemsStr@@${pl.coverUrl ?: ""}@@${pl.isPinned}"
        }
        prefs.edit().putString("playlists", serialized).apply()
    }

    fun saveLastPlayerState(state: PlayerState?) {
        if (state == null) {
            prefs.edit().remove("last_player_state").apply()
        } else {
            val ser = "${state.title}<||>${state.artist}<||>${state.artUrl?.toString() ?: ""}<||>${state.videoId ?: ""}<||>${state.contentUri?.toString() ?: ""}"
            prefs.edit().putString("last_player_state", ser).apply()
        }
    }

    fun getLastPlayerState(): PlayerState? {
        val ser = prefs.getString("last_player_state", "") ?: ""
        if (ser.isBlank()) return null
        val parts = ser.split("<||>")
        if (parts.size >= 5) {
            val title = parts[0]
            val artist = parts[1]
            val artUrl = parts[2].takeIf { it.isNotBlank() }
            val videoId = parts[3].takeIf { it.isNotBlank() }
            val uriStr = parts[4].takeIf { it.isNotBlank() }
            val contentUri = uriStr?.let { Uri.parse(it) }
            return PlayerState(title, artist, artUrl, videoId, contentUri)
        }
        return null
    }

    fun getAppLanguage(context: Context): String {
        if (!::prefs.isInitialized) {
            init(context)
        }
        return prefs.getString("app_language", "SYSTEM_DEFAULT") ?: "SYSTEM_DEFAULT"
    }

    fun saveAppLanguage(context: Context, lang: String) {
        if (!::prefs.isInitialized) {
            init(context)
        }
        prefs.edit().putString("app_language", lang).apply()
    }

    fun saveString(key: String, value: String?) {
        if (value == null) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, value).apply()
        }
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return prefs.getString(key, defaultValue)
    }

    fun saveLastTab(index: Int) {
        prefs.edit().putInt("last_tab_index", index).apply()
    }

    fun getLastTab(): Int {
        if (!::prefs.isInitialized) return 0
        return prefs.getInt("last_tab_index", 0)
    }

    fun getGlassStyle(): String {
        return prefs.getString("glass_style", "transparent") ?: "transparent"
    }

    fun saveGlassStyle(style: String) {
        prefs.edit().putString("glass_style", style).apply()
    }
}
