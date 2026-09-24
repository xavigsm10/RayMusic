package com.mrtdk.liquid_glass.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import android.net.Uri
import androidx.compose.ui.graphics.Color

enum class ItemType { ALBUM, ARTIST, SONG }

@androidx.compose.runtime.Immutable
data class RecentSearchItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnail: String? = null,
    val type: String = "SONG", // "ARTIST", "SONG", "ALBUM"
    val album: String? = null,
    val albumId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@androidx.compose.runtime.Immutable
data class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnail: String?,
    val type: ItemType,
    val album: String? = null
)

@androidx.compose.runtime.Immutable
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
    private val settingsCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private val _savedItems = MutableStateFlow<List<LibraryItem>>(emptyList())
    val savedItems: StateFlow<List<LibraryItem>> = _savedItems

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _recentlyPlayed = MutableStateFlow<List<LibraryItem>>(emptyList())
    val recentlyPlayed: StateFlow<List<LibraryItem>> = _recentlyPlayed

    private val _downloadedSongs = MutableStateFlow<List<LibraryItem>>(emptyList())
    val downloadedSongs: StateFlow<List<LibraryItem>> = _downloadedSongs

    private val _recentSearches = MutableStateFlow<List<RecentSearchItem>>(emptyList())
    val recentSearches: StateFlow<List<RecentSearchItem>> = _recentSearches

    private val _glassStyle = MutableStateFlow("transparent")
    val glassStyle: StateFlow<String> = _glassStyle

    private val _bottomTabsStyle = MutableStateFlow("ios26")
    val bottomTabsStyle: StateFlow<String> = _bottomTabsStyle

    private val _playerArtworkStyle = MutableStateFlow("fullartwork")
    val playerArtworkStyle: StateFlow<String> = _playerArtworkStyle

    private val _fullArtworkBackdropStyle = MutableStateFlow("apple_music")
    val fullArtworkBackdropStyle: StateFlow<String> = _fullArtworkBackdropStyle

    private val _ultraPerformanceMode = MutableStateFlow(false)
    val ultraPerformanceMode: StateFlow<Boolean> = _ultraPerformanceMode

    private val _pinnedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedItemIds: StateFlow<Set<String>> = _pinnedItemIds

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
        // NOTA: isInitialized se activa DESPUÉS de cargar los ajustes de apariencia:
        // los getters devuelven el valor en memoria una vez inicializado, así que
        // cargarlos con el flag ya activo siempre devolvería el valor por defecto
        // y las opciones elegidas (tema, bottom tabs, etc.) se restablecerían.

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

        // Load UI appearance settings immediately (fast key-value reads)
        _glassStyle.value = getGlassStyle()
        _bottomTabsStyle.value = getBottomTabsStyle()
        _playerArtworkStyle.value = getPlayerArtworkStyle()
        _fullArtworkBackdropStyle.value = getFullArtworkBackdropStyle()
        _ultraPerformanceMode.value = isUltraPerformanceMode()

        val initialPinned = mutableSetOf<String>()
        try {
            prefs.all.forEach { (key, value) ->
                if (value == "true" && (key.startsWith("song_pinned_") || key.startsWith("item_pinned_") || key.startsWith("artist_pinned_") || key.startsWith("album_pinned_"))) {
                    val id = key.removePrefix("song_pinned_")
                        .removePrefix("item_pinned_")
                        .removePrefix("artist_pinned_")
                        .removePrefix("album_pinned_")
                    if (id.isNotBlank()) initialPinned.add(id)
                }
            }
        } catch (_: Exception) {}
        _pinnedItemIds.value = initialPinned

        isInitialized = true

        // Load heavy data collections asynchronously on Dispatchers.IO to avoid blocking main thread at startup
        CoroutineScope(Dispatchers.IO).launch {
            val saved = dbHelper.getSavedItems()
            val pl = dbHelper.getPlaylists()
            val recent = dbHelper.getRecentlyPlayed()
            val downloaded = dbHelper.getDownloadedSongs()
            val searches = loadRecentSearchesFromDb()
            _savedItems.value = saved
            _playlists.value = pl
            _recentlyPlayed.value = recent
            _downloadedSongs.value = downloaded
            _recentSearches.value = searches
        }

        com.mrtdk.liquid_glass.spotify.SpotifySession.init()
        com.mrtdk.liquid_glass.ui.theme.ThemeManager.init()

        if (com.mrtdk.liquid_glass.spotify.SpotifySession.isLoggedIn.value) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    syncSpotifyPlaylists()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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
                        "bottom_tabs_style",
                        "player_artwork_style",
                        "full_artwork_backdrop_style",
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

    fun isItemPinned(id: String): Boolean {
        if (_pinnedItemIds.value.contains(id)) return true
        if (::prefs.isInitialized) {
            if (prefs.getString("song_pinned_$id", null) == "true") return true
            if (prefs.getString("item_pinned_$id", null) == "true") return true
            if (prefs.getString("artist_pinned_$id", null) == "true") return true
            if (prefs.getString("album_pinned_$id", null) == "true") return true
        }
        return false
    }

    fun setItemPinned(id: String, pinned: Boolean) {
        if (::prefs.isInitialized) {
            val str = if (pinned) "true" else "false"
            prefs.edit()
                .putString("song_pinned_$id", str)
                .putString("item_pinned_$id", str)
                .putString("artist_pinned_$id", str)
                .putString("album_pinned_$id", str)
                .apply()
        }
        val set = _pinnedItemIds.value.toMutableSet()
        if (pinned) set.add(id) else set.remove(id)
        _pinnedItemIds.value = set
    }

    fun togglePinItem(id: String): Boolean {
        val next = !isItemPinned(id)
        setItemPinned(id, next)
        return next
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
        
        try {
            androidx.media3.exoplayer.offline.DownloadService.sendRemoveDownload(
                context,
                com.mrtdk.liquid_glass.playback.ExoDownloadService::class.java,
                id,
                false
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

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

    suspend fun syncSpotifyPlaylists() {
        if (!isInitialized) return
        try {
            com.mrtdk.liquid_glass.spotify.SpotifySession.ensureValidToken()
            
            // 1. Sync Playlists
            val spotifyPlaylists = com.mrtdk.liquid_glass.spotify.Spotify.myPlaylists().getOrNull() ?: emptyList()
            for (spPlaylist in spotifyPlaylists) {
                if (spPlaylist.id.isBlank() || spPlaylist.name.isBlank()) continue
                val playlistId = "spotify_${spPlaylist.id}"
                val existing = _playlists.value.find { it.id == playlistId || it.id == spPlaylist.id }
                val coverUrl = spPlaylist.images.firstOrNull()?.url

                if (existing == null) {
                    dbHelper.insertPlaylist(Playlist(playlistId, spPlaylist.name, emptyList(), coverUrl, false, System.currentTimeMillis()))
                } else {
                    dbHelper.updatePlaylist(existing.id, spPlaylist.name, coverUrl ?: existing.coverUrl)
                }
            }
            _playlists.value = dbHelper.getPlaylists()

            // 2. Sync Albums
            val spotifyAlbums = com.mrtdk.liquid_glass.spotify.Spotify.myAlbums().getOrNull() ?: emptyList()
            for (album in spotifyAlbums) {
                if (album.id.isBlank() || album.name.isBlank()) continue
                val albumId = "spotify_album_${album.id}"
                val artistStr = album.artists.joinToString(", ") { it.name }
                val item = LibraryItem(
                    id = albumId,
                    title = album.name,
                    subtitle = if (artistStr.isNotBlank()) "Álbum • $artistStr" else "Álbum",
                    thumbnail = album.images.firstOrNull()?.url,
                    type = ItemType.ALBUM,
                    album = album.name
                )
                dbHelper.insertSavedItem(item)
            }

            // 3. Sync Artists
            val spotifyArtists = com.mrtdk.liquid_glass.spotify.Spotify.myArtists().getOrNull() ?: emptyList()
            for (artist in spotifyArtists) {
                if (artist.id.isBlank() || artist.name.isBlank()) continue
                val artistId = "spotify_artist_${artist.id}"
                val item = LibraryItem(
                    id = artistId,
                    title = artist.name,
                    subtitle = "Artista",
                    thumbnail = artist.images.firstOrNull()?.url,
                    type = ItemType.ARTIST
                )
                dbHelper.insertSavedItem(item)
            }

            _savedItems.value = dbHelper.getSavedItems()

            // 4. Sync Liked Songs / Favoritos ("Canciones que te gustan")
            val allSavedTracks = mutableListOf<com.mrtdk.liquid_glass.spotify.SpotifyTrack>()
            var savedOffset = 0
            val savedLimit = 50
            while (true) {
                val savedPage = com.mrtdk.liquid_glass.spotify.Spotify.mySavedTracks(limit = savedLimit, offset = savedOffset).getOrNull() ?: break
                if (savedPage.isEmpty()) break
                allSavedTracks.addAll(savedPage)
                for (track in savedPage) {
                    if (track.id.isBlank() || track.name.isBlank()) continue
                    val artistStr = track.artists.joinToString(", ") { it.name }
                    val coverUrl = track.album?.images?.firstOrNull()?.url
                    val item = LibraryItem(
                        id = track.id,
                        title = track.name,
                        subtitle = artistStr,
                        thumbnail = coverUrl,
                        type = ItemType.SONG,
                        album = track.album?.name
                    )
                    dbHelper.insertSavedItem(item)
                }
                if (savedPage.size < savedLimit) break
                savedOffset += savedPage.size
                if (savedOffset >= 200) break
            }

            if (allSavedTracks.isNotEmpty()) {
                val likedPlaylistId = "spotify_liked_songs"
                val existingLiked = _playlists.value.find { it.id == likedPlaylistId }
                val likedItems = allSavedTracks.map { track ->
                    val artistStr = track.artists.joinToString(", ") { it.name }
                    LibraryItem(
                        id = track.id,
                        title = track.name,
                        subtitle = artistStr,
                        thumbnail = track.album?.images?.firstOrNull()?.url,
                        type = ItemType.SONG,
                        album = track.album?.name
                    )
                }
                val coverUrl = likedItems.firstOrNull()?.thumbnail
                if (existingLiked == null) {
                    dbHelper.insertPlaylist(Playlist(likedPlaylistId, "Canciones que te gustan", likedItems, coverUrl, true, System.currentTimeMillis()))
                } else {
                    dbHelper.insertPlaylist(existingLiked.copy(items = likedItems, coverUrl = coverUrl ?: existingLiked.coverUrl))
                }
                _playlists.value = dbHelper.getPlaylists()
            }

            _savedItems.value = dbHelper.getSavedItems()

            // Asynchronously fetch tracks for each imported playlist
            for (spPlaylist in spotifyPlaylists) {
                if (spPlaylist.id.isBlank()) continue
                val playlistId = "spotify_${spPlaylist.id}"
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    try {
                        fetchSpotifyPlaylistTracks(playlistId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchSpotifyPlaylistTracks(playlistId: String) {
        if (!isInitialized) return
        try {
            val allTracks = mutableListOf<com.mrtdk.liquid_glass.spotify.SpotifyTrack>()
            if (playlistId == "spotify_liked_songs") {
                var offset = 0
                val limit = 50
                while (true) {
                    val page = com.mrtdk.liquid_glass.spotify.Spotify.mySavedTracks(limit = limit, offset = offset).getOrNull() ?: break
                    if (page.isEmpty()) break
                    allTracks.addAll(page)
                    if (page.size < limit) break
                    offset += page.size
                    if (offset >= 200) break
                }
            } else {
                val rawId = playlistId.removePrefix("spotify_")
                var offset = 0
                val limit = 100
                while (true) {
                    val page = com.mrtdk.liquid_glass.spotify.Spotify.playlistTracks(rawId, limit = limit, offset = offset).getOrNull() ?: break
                    if (page.isEmpty()) break
                    allTracks.addAll(page)
                    if (page.size < limit) break
                    offset += page.size
                }
            }
            if (allTracks.isEmpty()) return

            val items = allTracks.map { track ->
                LibraryItem(
                    id = track.id,
                    title = track.name,
                    subtitle = track.artists.firstOrNull()?.name ?: "",
                    thumbnail = track.album?.images?.firstOrNull()?.url,
                    type = ItemType.SONG,
                    album = track.album?.name
                )
            }
            val existing = _playlists.value.find { it.id == playlistId }
            if (existing != null) {
                val updated = existing.copy(items = items)
                dbHelper.insertPlaylist(updated)
                _playlists.value = dbHelper.getPlaylists()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        if (value == null) {
            settingsCache.remove(key)
        } else {
            settingsCache[key] = value
        }
        try {
            if (value == null) {
                prefs.edit().remove(key).apply()
            } else {
                prefs.edit().putString(key, value).apply()
            }
        } catch (_: Exception) {}
        dbHelper.saveSetting(key, value)
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        if (!isInitialized) return defaultValue
        settingsCache[key]?.let { return it }
        val fromDb = dbHelper.getSetting(key, null)
        if (fromDb != null) {
            settingsCache[key] = fromDb
            return fromDb
        }
        val fromPrefs = try { prefs.getString(key, null) } catch (_: Exception) { null }
        if (fromPrefs != null) {
            settingsCache[key] = fromPrefs
            return fromPrefs
        }
        return defaultValue
    }

    fun saveInt(key: String, value: Int) {
        if (!isInitialized) return
        settingsCache[key] = value.toString()
        try {
            prefs.edit().putInt(key, value).apply()
        } catch (_: Exception) {}
        dbHelper.saveSettingInt(key, value)
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        if (!isInitialized) return defaultValue
        settingsCache[key]?.toIntOrNull()?.let { return it }
        val fromDb = dbHelper.getSetting(key, null)?.toIntOrNull()
        if (fromDb != null) {
            settingsCache[key] = fromDb.toString()
            return fromDb
        }
        val fromPrefs = try { prefs.getInt(key, defaultValue) } catch (_: Exception) { defaultValue }
        settingsCache[key] = fromPrefs.toString()
        return fromPrefs
    }

    fun saveLastTab(index: Int) {
        if (!isInitialized) return
        saveInt("last_tab_index", index)
    }

    fun getLastTab(): Int {
        if (!isInitialized) return 0
        return getInt("last_tab_index", 0)
    }

    fun getGlassStyle(): String {
        if (isInitialized) return _glassStyle.value
        val fromDb = dbHelper.getSetting("glass_style", null)
        val style = fromDb ?: try { prefs.getString("glass_style", null) } catch (_: Exception) { null } ?: "transparent"
        return if (style == "semitransparent" || style == "semitransparente") "solid" else style
    }

    fun hasCompletedOnboarding(): Boolean {
        if (!isInitialized) return false
        settingsCache["has_completed_onboarding_v065"]?.let { return it == "true" }
        val fromDb = dbHelper.getSetting("has_completed_onboarding_v065", null)
        if (fromDb != null) {
            settingsCache["has_completed_onboarding_v065"] = fromDb
            return fromDb == "true"
        }
        val fromPrefs = try { prefs.getString("has_completed_onboarding_v065", null) } catch (_: Exception) { null }
        if (fromPrefs != null) {
            settingsCache["has_completed_onboarding_v065"] = fromPrefs
            return fromPrefs == "true"
        }
        return false
    }

    fun setCompletedOnboarding(completed: Boolean) {
        if (!isInitialized) return
        val strVal = completed.toString()
        settingsCache["has_completed_onboarding_v065"] = strVal
        try {
            prefs.edit().putString("has_completed_onboarding_v065", strVal).apply()
        } catch (_: Exception) {}
        dbHelper.saveSetting("has_completed_onboarding_v065", strVal)
    }

    fun saveGlassStyle(style: String) {
        if (!isInitialized) return
        _glassStyle.value = style
        try {
            prefs.edit().putString("glass_style", style).apply()
        } catch (_: Exception) {}
        dbHelper.saveSetting("glass_style", style)
    }

    fun getBottomTabsStyle(): String {
        if (isInitialized) return _bottomTabsStyle.value
        val fromDb = dbHelper.getSetting("bottom_tabs_style", null)
        if (fromDb != null) return fromDb
        val fromPrefs = try { prefs.getString("bottom_tabs_style", null) } catch (_: Exception) { null }
        if (fromPrefs != null) return fromPrefs
        return "ios26"
    }

    fun saveBottomTabsStyle(style: String) {
        if (!isInitialized) return
        _bottomTabsStyle.value = style
        try {
            prefs.edit().putString("bottom_tabs_style", style).apply()
        } catch (_: Exception) {}
        dbHelper.saveSetting("bottom_tabs_style", style)
    }

    fun getPlayerArtworkStyle(): String {
        if (isInitialized) return _playerArtworkStyle.value
        val fromDb = dbHelper.getSetting("player_artwork_style", null)
        if (fromDb != null) return fromDb
        val fromPrefs = try { prefs.getString("player_artwork_style", null) } catch (_: Exception) { null }
        if (fromPrefs != null) return fromPrefs
        return "fullartwork"
    }

    fun savePlayerArtworkStyle(style: String) {
        if (!isInitialized) return
        _playerArtworkStyle.value = style
        try {
            prefs.edit().putString("player_artwork_style", style).apply()
        } catch (_: Exception) {}
        dbHelper.saveSetting("player_artwork_style", style)
    }

    fun getFullArtworkBackdropStyle(): String {
        if (isInitialized) return _fullArtworkBackdropStyle.value
        val fromDb = dbHelper.getSetting("full_artwork_backdrop_style", null)
        if (fromDb != null) return fromDb
        val fromPrefs = try { prefs.getString("full_artwork_backdrop_style", null) } catch (_: Exception) { null }
        if (fromPrefs != null) return fromPrefs
        return "apple_music"
    }

    fun saveFullArtworkBackdropStyle(style: String) {
        if (!isInitialized) return
        _fullArtworkBackdropStyle.value = style
        try {
            prefs.edit().putString("full_artwork_backdrop_style", style).apply()
        } catch (_: Exception) {}
        dbHelper.saveSetting("full_artwork_backdrop_style", style)
    }

    fun isUltraPerformanceMode(): Boolean {
        if (isInitialized) return _ultraPerformanceMode.value
        val fromDb = dbHelper.getSetting("ultra_performance_mode", null)
        if (fromDb != null) return fromDb == "true"
        val fromPrefs = try { prefs.getString("ultra_performance_mode", null) } catch (_: Exception) { null }
        if (fromPrefs != null) return fromPrefs == "true"
        return false
    }

    fun saveUltraPerformanceMode(enabled: Boolean) {
        if (!isInitialized) return
        _ultraPerformanceMode.value = enabled
        val strVal = enabled.toString()
        try {
            prefs.edit().putString("ultra_performance_mode", strVal).apply()
        } catch (_: Exception) {}
        dbHelper.saveSetting("ultra_performance_mode", strVal)
    }

    fun getDownloadedSongsForAlbum(albumName: String): List<LibraryItem> {
        if (!isInitialized) return emptyList()
        return dbHelper.getDownloadedSongsForAlbum(albumName)
    }

    fun addPlaybackRecord(songId: String, title: String, artist: String, thumbnail: String?, album: String?, playlistId: String?, playlistName: String?) {
        if (!isInitialized) return
        dbHelper.insertPlaybackRecord(songId, title, artist, thumbnail, album, playlistId, playlistName, System.currentTimeMillis())
    }

    fun getPlaybackHistory(): List<PlaybackRecord> {
        if (!isInitialized) return emptyList()
        return dbHelper.getPlaybackHistory()
    }

    fun clearPlaybackHistory() {
        if (!isInitialized) return
        dbHelper.clearPlaybackHistory()
    }

    fun addRecentSearch(item: RecentSearchItem) {
        if (item.id.isBlank() && item.title.isBlank()) return
        val current = _recentSearches.value.filterNot { it.id == item.id || (it.title.equals(item.title, ignoreCase = true) && it.type == item.type) }
        val updated = (listOf(item) + current).take(25)
        _recentSearches.value = updated
        saveRecentSearchesDirect(updated)
    }

    fun removeRecentSearch(id: String) {
        val updated = _recentSearches.value.filterNot { it.id == id }
        _recentSearches.value = updated
        saveRecentSearchesDirect(updated)
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        saveRecentSearchesDirect(emptyList())
    }

    private fun saveRecentSearchesDirect(list: List<RecentSearchItem>) {
        if (!isInitialized) return
        try {
            val arr = org.json.JSONArray()
            for (itm in list) {
                val obj = org.json.JSONObject()
                obj.put("id", itm.id)
                obj.put("title", itm.title)
                obj.put("subtitle", itm.subtitle)
                obj.put("thumbnail", itm.thumbnail ?: "")
                obj.put("type", itm.type)
                obj.put("album", itm.album ?: "")
                obj.put("albumId", itm.albumId ?: "")
                obj.put("timestamp", itm.timestamp)
                arr.put(obj)
            }
            dbHelper.saveSetting("recent_searches_json", arr.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadRecentSearchesFromDb(): List<RecentSearchItem> {
        val jsonStr = dbHelper.getSetting("recent_searches_json", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(jsonStr)
            val list = mutableListOf<RecentSearchItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RecentSearchItem(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        subtitle = obj.optString("subtitle", ""),
                        thumbnail = obj.optString("thumbnail", "").takeIf { it.isNotBlank() },
                        type = obj.optString("type", "SONG"),
                        album = obj.optString("album", "").takeIf { it.isNotBlank() },
                        albumId = obj.optString("albumId", "").takeIf { it.isNotBlank() },
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class PlaybackRecord(
    val songId: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val album: String?,
    val playlistId: String?,
    val playlistName: String?,
    val timestamp: Long
)
