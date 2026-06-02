package com.mrtdk.liquid_glass.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import android.net.Uri
import androidx.compose.ui.graphics.Color

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
    val isPinned: Boolean = false,
    val timestamp: Long = 0L
)

object LibraryManager {
    private const val PREFS_NAME = "liquid_glass_library"
    val currentDominantColor = MutableStateFlow<Color>(Color.White.copy(alpha = 0.15f))
    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context
    private lateinit var dbHelper: LibraryDatabaseHelper
    private var isInitialized = false

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

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        this.context = context.applicationContext
        prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        dbHelper = LibraryDatabaseHelper(this.context)

        // Perform library items migration if not already migrated
        val isLibraryMigrated = dbHelper.getSetting("migrated_to_db") == "true" || prefs.getBoolean("migrated_to_db", false)
        if (!isLibraryMigrated) {
            migrateFromSharedPrefs()
        } else {
            // Ensure the flag is stored in DB settings for consistency
            dbHelper.saveSetting("migrated_to_db", "true")
        }

        // Perform settings migration if not already migrated
        if (dbHelper.getSetting("migrated_settings_to_db") != "true") {
            migrateSettingsFromSharedPrefs()
        }

        // Load data from DB into Flows
        _savedItems.value = dbHelper.getSavedItems()
        _playlists.value = dbHelper.getPlaylists()
        _recentlyPlayed.value = dbHelper.getRecentlyPlayed()
        _downloadedSongs.value = dbHelper.getDownloadedSongs()

        isInitialized = true
    }

    private fun migrateFromSharedPrefs() {
        try {
            dbHelper.writableDatabase.use { db ->
                db.beginTransaction()
                try {
                    // 1. Migrate saved items
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
                        for (item in items) {
                            dbHelper.insertSavedItemDirect(db, item)
                        }
                    }

                    // 2. Migrate playlists
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
                        for (playlist in pl) {
                            dbHelper.insertPlaylistDirect(db, playlist)
                        }
                    }

                    // 3. Migrate recently played
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
                        for (item in items) {
                            dbHelper.insertRecentlyPlayedDirect(db, item)
                        }
                    }

                    // 4. Migrate downloaded songs
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
                        for (item in items) {
                            dbHelper.insertDownloadedSongDirect(db, item)
                        }
                    }

                    dbHelper.saveSettingDirect(db, "migrated_to_db", "true")
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun migrateSettingsFromSharedPrefs() {
        try {
            dbHelper.writableDatabase.use { db ->
                db.beginTransaction()
                try {
                    val keys = listOf(
                        "app_language",
                        "glass_style",
                        "audio_quality",
                        "last_player_state",
                        "cache_featured_suggestions",
                        "cache_quick_picks",
                        "cache_selecciones",
                        "cache_playlists",
                        "cache_similar_sections",
                        "cache_selecciones_title"
                    )
                    for (key in keys) {
                        if (prefs.contains(key)) {
                            val value = prefs.getString(key, null)
                            dbHelper.saveSettingDirect(db, key, value)
                        }
                    }
                    if (prefs.contains("last_tab_index")) {
                        val value = prefs.getInt("last_tab_index", 0)
                        dbHelper.saveSettingDirect(db, "last_tab_index", value.toString())
                    }
                    dbHelper.saveSettingDirect(db, "migrated_settings_to_db", "true")
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveItem(item: LibraryItem) {
        if (!isInitialized) return
        dbHelper.insertSavedItem(item)
        _savedItems.value = dbHelper.getSavedItems()
    }

    fun saveDownloadedSong(item: LibraryItem) {
        if (!isInitialized) return
        dbHelper.insertDownloadedSong(item)
        _downloadedSongs.value = dbHelper.getDownloadedSongs()
    }

    fun deleteDownloadedSong(context: Context, id: String) {
        if (!isInitialized) return
        dbHelper.deleteDownloadedSong(id)
        _downloadedSongs.value = dbHelper.getDownloadedSongs()
        val fileUriStr = getString("local_uri_$id")
        if (!fileUriStr.isNullOrBlank()) {
            try {
                val uri = Uri.parse(fileUriStr)
                val file = java.io.File(uri.path ?: "")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            saveString("local_uri_$id", null)
        }
    }

    fun removeItem(id: String) {
        if (!isInitialized) return
        dbHelper.deleteSavedItem(id)
        _savedItems.value = dbHelper.getSavedItems()
    }

    fun addRecentlyPlayed(item: LibraryItem) {
        if (!isInitialized) return
        dbHelper.insertRecentlyPlayed(item)
        _recentlyPlayed.value = dbHelper.getRecentlyPlayed()
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
        if (!isInitialized) return
        val id = java.util.UUID.randomUUID().toString()
        val finalCoverUrl = if (coverUrl != null && coverUrl.startsWith("content://")) {
            savePlaylistCover(context, id, Uri.parse(coverUrl))
        } else {
            coverUrl
        }
        dbHelper.insertPlaylist(Playlist(id, name, emptyList(), finalCoverUrl, false))
        _playlists.value = dbHelper.getPlaylists()
    }

    fun updatePlaylist(playlistId: String, name: String, coverUrl: String?) {
        if (!isInitialized) return
        dbHelper.updatePlaylist(playlistId, name, coverUrl)
        _playlists.value = dbHelper.getPlaylists()
    }

    fun togglePinPlaylist(playlistId: String) {
        if (!isInitialized) return
        dbHelper.togglePinPlaylist(playlistId)
        _playlists.value = dbHelper.getPlaylists()
    }

    fun deletePlaylist(playlistId: String) {
        if (!isInitialized) return
        dbHelper.deletePlaylist(playlistId)
        _playlists.value = dbHelper.getPlaylists()
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        if (!isInitialized) return
        dbHelper.renamePlaylist(playlistId, newName)
        _playlists.value = dbHelper.getPlaylists()
    }

    fun addSongToPlaylist(playlistId: String, song: LibraryItem) {
        if (!isInitialized) return
        if (dbHelper.isSongInPlaylist(playlistId, song.id)) return
        dbHelper.addSongToPlaylist(playlistId, song)
        _playlists.value = dbHelper.getPlaylists()
    }

    fun saveLastPlayerState(state: PlayerState?) {
        if (!isInitialized) return
        if (state == null) {
            dbHelper.saveSetting("last_player_state", null)
        } else {
            val ser = "${state.title}<||>${state.artist}<||>${state.artUrl?.toString() ?: ""}<||>${state.videoId ?: ""}<||>${state.contentUri?.toString() ?: ""}"
            dbHelper.saveSetting("last_player_state", ser)
        }
    }

    fun getLastPlayerState(): PlayerState? {
        if (!isInitialized) return null
        val ser = dbHelper.getSetting("last_player_state") ?: return null
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
        if (!isInitialized) {
            init(context)
        }
        return dbHelper.getSetting("app_language", "SYSTEM_DEFAULT") ?: "SYSTEM_DEFAULT"
    }

    fun saveAppLanguage(context: Context, lang: String) {
        if (!isInitialized) {
            init(context)
        }
        dbHelper.saveSetting("app_language", lang)
    }

    fun saveString(key: String, value: String?) {
        if (!isInitialized) return
        dbHelper.saveSetting(key, value)
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        if (!isInitialized) return defaultValue
        return dbHelper.getSetting(key, defaultValue)
    }

    fun saveLastTab(index: Int) {
        if (!isInitialized) return
        dbHelper.saveSettingInt("last_tab_index", index)
    }

    fun getLastTab(): Int {
        if (!isInitialized) return 0
        return dbHelper.getSettingInt("last_tab_index", 0)
    }

    fun getGlassStyle(): String {
        if (!isInitialized) return "transparent"
        return dbHelper.getSetting("glass_style", "transparent") ?: "transparent"
    }

    fun saveGlassStyle(style: String) {
        if (!isInitialized) return
        dbHelper.saveSetting("glass_style", style)
    }
}
