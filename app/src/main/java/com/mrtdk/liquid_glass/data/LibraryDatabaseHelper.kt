package com.mrtdk.liquid_glass.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LibraryDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ray_music_library.db"
        private const val DATABASE_VERSION = 3

        // Tables
        private const val TABLE_SAVED_ITEMS = "saved_items"
        private const val TABLE_DOWNLOADED_SONGS = "downloaded_songs"
        private const val TABLE_RECENTLY_PLAYED = "recently_played"
        private const val TABLE_PLAYLISTS = "playlists"
        private const val TABLE_PLAYLIST_ITEMS = "playlist_items"
        private const val TABLE_SETTINGS = "settings"

        // Common Columns
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_SUBTITLE = "subtitle"
        private const val KEY_THUMBNAIL = "thumbnail"
        private const val KEY_TYPE = "type"
        private const val KEY_ALBUM = "album"
        private const val KEY_TIMESTAMP = "timestamp"

        // Playlist specific columns
        private const val KEY_NAME = "name"
        private const val KEY_COVER_URL = "cover_url"
        private const val KEY_IS_PINNED = "is_pinned"

        // Playlist Items specific columns
        private const val KEY_PLAYLIST_ID = "playlist_id"
        private const val KEY_SONG_ID = "song_id"
        private const val KEY_POSITION = "position"

        // Settings specific columns
        private const val KEY_SETTINGS_KEY = "key"
        private const val KEY_SETTINGS_VALUE = "value"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSavedItems = ("CREATE TABLE " + TABLE_SAVED_ITEMS + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + KEY_TITLE + " TEXT,"
                + KEY_SUBTITLE + " TEXT,"
                + KEY_THUMBNAIL + " TEXT,"
                + KEY_TYPE + " TEXT,"
                + KEY_ALBUM + " TEXT,"
                + KEY_TIMESTAMP + " INTEGER" + ")")

        val createDownloadedSongs = ("CREATE TABLE " + TABLE_DOWNLOADED_SONGS + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + KEY_TITLE + " TEXT,"
                + KEY_SUBTITLE + " TEXT,"
                + KEY_THUMBNAIL + " TEXT,"
                + KEY_TYPE + " TEXT,"
                + KEY_ALBUM + " TEXT,"
                + KEY_TIMESTAMP + " INTEGER" + ")")

        val createRecentlyPlayed = ("CREATE TABLE " + TABLE_RECENTLY_PLAYED + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + KEY_TITLE + " TEXT,"
                + KEY_SUBTITLE + " TEXT,"
                + KEY_THUMBNAIL + " TEXT,"
                + KEY_TYPE + " TEXT,"
                + KEY_ALBUM + " TEXT,"
                + KEY_TIMESTAMP + " INTEGER" + ")")

        val createPlaylists = ("CREATE TABLE " + TABLE_PLAYLISTS + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + KEY_NAME + " TEXT,"
                + KEY_COVER_URL + " TEXT,"
                + KEY_IS_PINNED + " INTEGER DEFAULT 0,"
                + KEY_TIMESTAMP + " INTEGER" + ")")

        val createPlaylistItems = ("CREATE TABLE " + TABLE_PLAYLIST_ITEMS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_PLAYLIST_ID + " TEXT,"
                + KEY_SONG_ID + " TEXT,"
                + KEY_TITLE + " TEXT,"
                + KEY_SUBTITLE + " TEXT,"
                + KEY_THUMBNAIL + " TEXT,"
                + KEY_TYPE + " TEXT,"
                + KEY_ALBUM + " TEXT,"
                + KEY_POSITION + " INTEGER,"
                + "FOREIGN KEY(" + KEY_PLAYLIST_ID + ") REFERENCES " + TABLE_PLAYLISTS + "(" + KEY_ID + ") ON DELETE CASCADE" + ")")

        val createSettings = ("CREATE TABLE " + TABLE_SETTINGS + "("
                + KEY_SETTINGS_KEY + " TEXT PRIMARY KEY,"
                + KEY_SETTINGS_VALUE + " TEXT" + ")")

        db.execSQL(createSavedItems)
        db.execSQL(createDownloadedSongs)
        db.execSQL(createRecentlyPlayed)
        db.execSQL(createPlaylists)
        db.execSQL(createPlaylistItems)
        db.execSQL(createSettings)
        db.execSQL("CREATE INDEX idx_playlist_items_playlist_id ON " + TABLE_PLAYLIST_ITEMS + "(" + KEY_PLAYLIST_ID + ")")
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var currentVersion = oldVersion
        if (currentVersion < 2) {
            val createSettings = ("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + "("
                    + KEY_SETTINGS_KEY + " TEXT PRIMARY KEY,"
                    + KEY_SETTINGS_VALUE + " TEXT" + ")")
            db.execSQL(createSettings)
            currentVersion = 2
        }
        if (currentVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_PLAYLISTS ADD COLUMN $KEY_TIMESTAMP INTEGER DEFAULT 0")
            } catch (e: Exception) {
                // Column may already exist
            }
            currentVersion = 3
        }
    }

    // --- Settings CRUD ---

    fun getSetting(key: String, defaultValue: String? = null): String? {
        val db = this.readableDatabase
        val query = "SELECT $KEY_SETTINGS_VALUE FROM $TABLE_SETTINGS WHERE $KEY_SETTINGS_KEY = ?"
        db.rawQuery(query, arrayOf(key)).use { cursor ->
            if (cursor.moveToFirst()) {
                val valueIdx = cursor.getColumnIndex(KEY_SETTINGS_VALUE)
                if (valueIdx != -1) {
                    return cursor.getString(valueIdx)
                }
            }
        }
        return defaultValue
    }

    fun saveSetting(key: String, value: String?) {
        val db = this.writableDatabase
        saveSettingDirect(db, key, value)
    }

    fun saveSettingDirect(db: SQLiteDatabase, key: String, value: String?) {
        if (value == null) {
            db.delete(TABLE_SETTINGS, "$KEY_SETTINGS_KEY = ?", arrayOf(key))
        } else {
            val values = ContentValues().apply {
                put(KEY_SETTINGS_KEY, key)
                put(KEY_SETTINGS_VALUE, value)
            }
            db.insertWithOnConflict(TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getSettingInt(key: String, defaultValue: Int): Int {
        val strValue = getSetting(key) ?: return defaultValue
        return strValue.toIntOrNull() ?: defaultValue
    }

    fun saveSettingInt(key: String, value: Int) {
        saveSetting(key, value.toString())
    }

    // --- Saved Items CRUD ---

    fun getSavedItems(): List<LibraryItem> {
        val items = mutableListOf<LibraryItem>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_SAVED_ITEMS ORDER BY $KEY_TIMESTAMP DESC"
        db.rawQuery(selectQuery, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(KEY_ID)
                val titleIdx = cursor.getColumnIndex(KEY_TITLE)
                val subtitleIdx = cursor.getColumnIndex(KEY_SUBTITLE)
                val thumbnailIdx = cursor.getColumnIndex(KEY_THUMBNAIL)
                val typeIdx = cursor.getColumnIndex(KEY_TYPE)
                val albumIdx = cursor.getColumnIndex(KEY_ALBUM)

                if (idIdx != -1 && titleIdx != -1 && subtitleIdx != -1 && thumbnailIdx != -1 && typeIdx != -1 && albumIdx != -1) {
                    do {
                        val id = cursor.getString(idIdx)
                        val title = cursor.getString(titleIdx) ?: ""
                        val subtitle = cursor.getString(subtitleIdx) ?: ""
                        val thumbnail = cursor.getString(thumbnailIdx)
                        val typeName = cursor.getString(typeIdx) ?: "SONG"
                        val type = try { ItemType.valueOf(typeName) } catch(e: Exception) { ItemType.SONG }
                        val album = cursor.getString(albumIdx)

                        items.add(LibraryItem(id, title, subtitle, thumbnail, type, album))
                    } while (cursor.moveToNext())
                }
            }
        }
        return items
    }

    fun insertSavedItem(item: LibraryItem) {
        val db = this.writableDatabase
        insertSavedItemDirect(db, item)
    }

    fun insertSavedItemDirect(db: SQLiteDatabase, item: LibraryItem) {
        val values = ContentValues().apply {
            put(KEY_ID, item.id)
            put(KEY_TITLE, item.title)
            put(KEY_SUBTITLE, item.subtitle)
            put(KEY_THUMBNAIL, item.thumbnail)
            put(KEY_TYPE, item.type.name)
            put(KEY_ALBUM, item.album)
            put(KEY_TIMESTAMP, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_SAVED_ITEMS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteSavedItem(id: String) {
        val db = this.writableDatabase
        db.delete(TABLE_SAVED_ITEMS, "$KEY_ID = ?", arrayOf(id))
    }

    // --- Downloaded Songs CRUD ---

    fun getDownloadedSongs(): List<LibraryItem> {
        val items = mutableListOf<LibraryItem>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_DOWNLOADED_SONGS ORDER BY $KEY_TIMESTAMP DESC"
        db.rawQuery(selectQuery, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(KEY_ID)
                val titleIdx = cursor.getColumnIndex(KEY_TITLE)
                val subtitleIdx = cursor.getColumnIndex(KEY_SUBTITLE)
                val thumbnailIdx = cursor.getColumnIndex(KEY_THUMBNAIL)
                val typeIdx = cursor.getColumnIndex(KEY_TYPE)
                val albumIdx = cursor.getColumnIndex(KEY_ALBUM)

                if (idIdx != -1 && titleIdx != -1 && subtitleIdx != -1 && thumbnailIdx != -1 && typeIdx != -1 && albumIdx != -1) {
                    do {
                        val id = cursor.getString(idIdx)
                        val title = cursor.getString(titleIdx) ?: ""
                        val subtitle = cursor.getString(subtitleIdx) ?: ""
                        val thumbnail = cursor.getString(thumbnailIdx)
                        val typeName = cursor.getString(typeIdx) ?: "SONG"
                        val type = try { ItemType.valueOf(typeName) } catch(e: Exception) { ItemType.SONG }
                        val album = cursor.getString(albumIdx)

                        items.add(LibraryItem(id, title, subtitle, thumbnail, type, album))
                    } while (cursor.moveToNext())
                }
            }
        }
        return items
    }

    fun insertDownloadedSong(item: LibraryItem) {
        val db = this.writableDatabase
        insertDownloadedSongDirect(db, item)
    }

    fun insertDownloadedSongDirect(db: SQLiteDatabase, item: LibraryItem) {
        val values = ContentValues().apply {
            put(KEY_ID, item.id)
            put(KEY_TITLE, item.title)
            put(KEY_SUBTITLE, item.subtitle)
            put(KEY_THUMBNAIL, item.thumbnail)
            put(KEY_TYPE, item.type.name)
            put(KEY_ALBUM, item.album)
            put(KEY_TIMESTAMP, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_DOWNLOADED_SONGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteDownloadedSong(id: String) {
        val db = this.writableDatabase
        db.delete(TABLE_DOWNLOADED_SONGS, "$KEY_ID = ?", arrayOf(id))
    }

    fun isSongDownloaded(id: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_DOWNLOADED_SONGS WHERE $KEY_ID = ?"
        db.rawQuery(query, arrayOf(id)).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) > 0
            }
        }
        return false
    }

    // --- Recently Played CRUD ---

    fun getRecentlyPlayed(): List<LibraryItem> {
        val items = mutableListOf<LibraryItem>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_RECENTLY_PLAYED ORDER BY $KEY_TIMESTAMP DESC LIMIT 30"
        db.rawQuery(selectQuery, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(KEY_ID)
                val titleIdx = cursor.getColumnIndex(KEY_TITLE)
                val subtitleIdx = cursor.getColumnIndex(KEY_SUBTITLE)
                val thumbnailIdx = cursor.getColumnIndex(KEY_THUMBNAIL)
                val typeIdx = cursor.getColumnIndex(KEY_TYPE)
                val albumIdx = cursor.getColumnIndex(KEY_ALBUM)

                if (idIdx != -1 && titleIdx != -1 && subtitleIdx != -1 && thumbnailIdx != -1 && typeIdx != -1 && albumIdx != -1) {
                    do {
                        val id = cursor.getString(idIdx)
                        val title = cursor.getString(titleIdx) ?: ""
                        val subtitle = cursor.getString(subtitleIdx) ?: ""
                        val thumbnail = cursor.getString(thumbnailIdx)
                        val typeName = cursor.getString(typeIdx) ?: "SONG"
                        val type = try { ItemType.valueOf(typeName) } catch(e: Exception) { ItemType.SONG }
                        val album = cursor.getString(albumIdx)

                        items.add(LibraryItem(id, title, subtitle, thumbnail, type, album))
                    } while (cursor.moveToNext())
                }
            }
        }
        return items
    }

    fun insertRecentlyPlayed(item: LibraryItem) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            insertRecentlyPlayedDirect(db, item)
            // Limit to 30 items
            db.execSQL("DELETE FROM $TABLE_RECENTLY_PLAYED WHERE $KEY_ID NOT IN " +
                       "(SELECT $KEY_ID FROM $TABLE_RECENTLY_PLAYED ORDER BY $KEY_TIMESTAMP DESC LIMIT 30)")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertRecentlyPlayedDirect(db: SQLiteDatabase, item: LibraryItem) {
        val values = ContentValues().apply {
            put(KEY_ID, item.id)
            put(KEY_TITLE, item.title)
            put(KEY_SUBTITLE, item.subtitle)
            put(KEY_THUMBNAIL, item.thumbnail)
            put(KEY_TYPE, item.type.name)
            put(KEY_ALBUM, item.album)
            put(KEY_TIMESTAMP, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_RECENTLY_PLAYED, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Playlists CRUD ---

    fun getPlaylists(): List<Playlist> {
        val playlists = mutableListOf<Playlist>()
        val db = this.readableDatabase

        val selectQuery = "SELECT * FROM $TABLE_PLAYLISTS ORDER BY $KEY_IS_PINNED DESC, $KEY_TIMESTAMP DESC"
        db.rawQuery(selectQuery, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(KEY_ID)
                val nameIdx = cursor.getColumnIndex(KEY_NAME)
                val coverUrlIdx = cursor.getColumnIndex(KEY_COVER_URL)
                val isPinnedIdx = cursor.getColumnIndex(KEY_IS_PINNED)
                val timestampIdx = cursor.getColumnIndex(KEY_TIMESTAMP)

                if (idIdx != -1 && nameIdx != -1 && coverUrlIdx != -1 && isPinnedIdx != -1) {
                    do {
                        val playlistId = cursor.getString(idIdx)
                        val name = cursor.getString(nameIdx) ?: ""
                        val coverUrl = cursor.getString(coverUrlIdx)
                        val isPinned = cursor.getInt(isPinnedIdx) == 1
                        val timestamp = if (timestampIdx != -1) cursor.getLong(timestampIdx) else 0L

                        val items = getPlaylistItems(db, playlistId)
                        playlists.add(Playlist(playlistId, name, items, coverUrl, isPinned, timestamp))
                    } while (cursor.moveToNext())
                }
            }
        }
        return playlists
    }

    private fun getPlaylistItems(db: SQLiteDatabase, playlistId: String): List<LibraryItem> {
        val items = mutableListOf<LibraryItem>()
        val query = "SELECT * FROM $TABLE_PLAYLIST_ITEMS WHERE $KEY_PLAYLIST_ID = ? ORDER BY $KEY_POSITION ASC"
        db.rawQuery(query, arrayOf(playlistId)).use { cursor ->
            if (cursor.moveToFirst()) {
                val songIdIdx = cursor.getColumnIndex(KEY_SONG_ID)
                val titleIdx = cursor.getColumnIndex(KEY_TITLE)
                val subtitleIdx = cursor.getColumnIndex(KEY_SUBTITLE)
                val thumbnailIdx = cursor.getColumnIndex(KEY_THUMBNAIL)
                val typeIdx = cursor.getColumnIndex(KEY_TYPE)
                val albumIdx = cursor.getColumnIndex(KEY_ALBUM)

                if (songIdIdx != -1 && titleIdx != -1 && subtitleIdx != -1 && thumbnailIdx != -1 && typeIdx != -1 && albumIdx != -1) {
                    do {
                        val songId = cursor.getString(songIdIdx)
                        val title = cursor.getString(titleIdx) ?: ""
                        val subtitle = cursor.getString(subtitleIdx) ?: ""
                        val thumbnail = cursor.getString(thumbnailIdx)
                        val typeName = cursor.getString(typeIdx) ?: "SONG"
                        val type = try { ItemType.valueOf(typeName) } catch(e: Exception) { ItemType.SONG }
                        val album = cursor.getString(albumIdx)

                        items.add(LibraryItem(songId, title, subtitle, thumbnail, type, album))
                    } while (cursor.moveToNext())
                }
            }
        }
        return items
    }

    fun insertPlaylist(playlist: Playlist) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            insertPlaylistDirect(db, playlist)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertPlaylistDirect(db: SQLiteDatabase, playlist: Playlist) {
        val values = ContentValues().apply {
            put(KEY_ID, playlist.id)
            put(KEY_NAME, playlist.name)
            put(KEY_COVER_URL, playlist.coverUrl)
            put(KEY_IS_PINNED, if (playlist.isPinned) 1 else 0)
            put(KEY_TIMESTAMP, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_PLAYLISTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)

        db.delete(TABLE_PLAYLIST_ITEMS, "$KEY_PLAYLIST_ID = ?", arrayOf(playlist.id))
        playlist.items.forEachIndexed { index, item ->
            val itemValues = ContentValues().apply {
                put(KEY_PLAYLIST_ID, playlist.id)
                put(KEY_SONG_ID, item.id)
                put(KEY_TITLE, item.title)
                put(KEY_SUBTITLE, item.subtitle)
                put(KEY_THUMBNAIL, item.thumbnail)
                put(KEY_TYPE, item.type.name)
                put(KEY_ALBUM, item.album)
                put(KEY_POSITION, index)
            }
            db.insert(TABLE_PLAYLIST_ITEMS, null, itemValues)
        }
    }

    fun updatePlaylist(playlistId: String, name: String, coverUrl: String?) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, name)
            put(KEY_COVER_URL, coverUrl)
        }
        db.update(TABLE_PLAYLISTS, values, "$KEY_ID = ?", arrayOf(playlistId))
    }

    fun togglePinPlaylist(playlistId: String) {
        val db = this.writableDatabase
        db.execSQL("UPDATE $TABLE_PLAYLISTS SET $KEY_IS_PINNED = 1 - $KEY_IS_PINNED WHERE $KEY_ID = ?", arrayOf(playlistId))
    }

    fun deletePlaylist(playlistId: String) {
        val db = this.writableDatabase
        db.delete(TABLE_PLAYLISTS, "$KEY_ID = ?", arrayOf(playlistId))
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, newName)
        }
        db.update(TABLE_PLAYLISTS, values, "$KEY_ID = ?", arrayOf(playlistId))
    }

    fun addSongToPlaylist(playlistId: String, song: LibraryItem) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            var maxPosition = -1
            val query = "SELECT MAX($KEY_POSITION) FROM $TABLE_PLAYLIST_ITEMS WHERE $KEY_PLAYLIST_ID = ?"
            db.rawQuery(query, arrayOf(playlistId)).use { cursor ->
                if (cursor.moveToFirst()) {
                    maxPosition = cursor.getInt(0)
                }
            }

            val nextPos = maxPosition + 1
            val values = ContentValues().apply {
                put(KEY_PLAYLIST_ID, playlistId)
                put(KEY_SONG_ID, song.id)
                put(KEY_TITLE, song.title)
                put(KEY_SUBTITLE, song.subtitle)
                put(KEY_THUMBNAIL, song.thumbnail)
                put(KEY_TYPE, song.type.name)
                put(KEY_ALBUM, song.album)
                put(KEY_POSITION, nextPos)
            }
            db.insert(TABLE_PLAYLIST_ITEMS, null, values)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun isSongInPlaylist(playlistId: String, songId: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_PLAYLIST_ITEMS WHERE $KEY_PLAYLIST_ID = ? AND $KEY_SONG_ID = ?"
        db.rawQuery(query, arrayOf(playlistId, songId)).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) > 0
            }
        }
        return false
    }
}
