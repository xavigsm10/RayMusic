package com.mrtdk.liquid_glass.spotify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Spotify {
    private const val GQL_URL = "https://api-partner.spotify.com/pathfinder/v2/query"
    private const val REST_URL = "https://api.spotify.com/v1"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private const val HASH_LIBRARY_V3 = "973e511ca44261fda7eebac8b653155e7caee3675abb4fb110cc1b8c78b091c3"
    private const val HASH_FETCH_PLAYLIST = "346811f856fb0b7e4f6c59f8ebea78dd081c6e2fb01b77c954b26259d5fc6763"
    private const val HASH_ARTIST_OVERVIEW = "5b9e64f43843fa3a9b6a98543600299b0a2cbbbccfdcdcef2402eb9c1017ca4c"
    private const val HASH_PROFILE_ATTRIBUTES = "53bcb064f6cd18c23f752bc324a791194d20df612d8e1239c735144ab0399ced"

    suspend fun me(): Result<SpotifyUser> = withContext(Dispatchers.IO) {
        runCatching {
            SpotifySession.ensureValidToken()
            val token = SpotifySession.accessToken

            // GQL profileAttributes
            try {
                val response = graphqlPost("profileAttributes", HASH_PROFILE_ATTRIBUTES, JSONObject(), token)
                val profile = response.optJSONObject("data")?.optJSONObject("me")?.optJSONObject("profile")
                if (profile != null) {
                    val uri = profile.optString("uri", "")
                    val id = if (uri.contains(":")) uri.substringAfterLast(":") else uri
                    val name = profile.optString("name", profile.optString("displayName", id))
                    if (name.isNotBlank()) {
                        return@runCatching SpotifyUser(id = id, displayName = name)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // REST fallback
            val jsonStr = httpGet("$REST_URL/me", mapOf("Authorization" to "Bearer $token", "App-Platform" to "WebPlayer"))
            val json = JSONObject(jsonStr)
            val id = json.optString("id", "")
            val name = json.optString("display_name", json.optString("id", ""))
            SpotifyUser(id = id, displayName = name)
        }
    }

    suspend fun myPlaylists(limit: Int = 50, offset: Int = 0): Result<List<SpotifyPlaylist>> = withContext(Dispatchers.IO) {
        runCatching {
            SpotifySession.ensureValidToken()
            val token = SpotifySession.accessToken

            val allPlaylists = mutableListOf<SpotifyPlaylist>()
            var currentOffset = offset
            var hasMore = true
            var safetyCounter = 0

            while (hasMore && safetyCounter < 10) {
                safetyCounter++
                try {
                    val variables = JSONObject().apply {
                        put("filters", JSONArray().apply { put("Playlists") })
                        put("order", JSONObject.NULL)
                        put("textFilter", "")
                        put("features", JSONArray().apply {
                            put("LIKED_SONGS")
                            put("YOUR_EPISODES_V2")
                            put("PRERELEASES")
                            put("EVENTS")
                        })
                        put("limit", limit)
                        put("offset", currentOffset)
                        put("flatten", true)
                        put("expandedFolders", JSONArray())
                        put("folderUri", JSONObject.NULL)
                        put("includeFoldersWhenFlattening", false)
                    }

                    val gqlResult = graphqlPost("libraryV3", HASH_LIBRARY_V3, variables, token)
                    val playlists = parseGqlLibraryPlaylists(gqlResult)
                    if (playlists.isEmpty()) {
                        hasMore = false
                    } else {
                        allPlaylists.addAll(playlists)
                        if (playlists.size < limit) {
                            hasMore = false
                        } else {
                            currentOffset += limit
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    hasMore = false
                }
            }

            if (allPlaylists.isNotEmpty()) return@runCatching allPlaylists

            // REST Fallback
            val jsonStr = httpGet("$REST_URL/me/playlists?limit=$limit&offset=$offset", mapOf("Authorization" to "Bearer $token", "App-Platform" to "WebPlayer"))
            val json = JSONObject(jsonStr)
            val items = json.optJSONArray("items") ?: JSONArray()
            val result = mutableListOf<SpotifyPlaylist>()

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optString("id", "")
                val name = item.optString("name", "")
                val description = item.optString("description", "")
                val imagesArray = item.optJSONArray("images")
                val images = mutableListOf<SpotifyImage>()
                if (imagesArray != null) {
                    for (j in 0 until imagesArray.length()) {
                        val img = imagesArray.optJSONObject(j) ?: continue
                        images.add(SpotifyImage(img.optString("url")))
                    }
                }
                val tracksObj = item.optJSONObject("tracks")
                val totalTracks = tracksObj?.optInt("total", 0) ?: 0

                result.add(
                    SpotifyPlaylist(
                        id = id,
                        name = name,
                        description = description,
                        images = images,
                        tracks = SpotifyPlaylistTracksRef(total = totalTracks)
                    )
                )
            }
            result
        }
    }

    suspend fun playlistTracks(playlistId: String, limit: Int = 100, offset: Int = 0): Result<List<SpotifyTrack>> = withContext(Dispatchers.IO) {
        runCatching {
            SpotifySession.ensureValidToken()
            val token = SpotifySession.accessToken
            val rawId = playlistId.removePrefix("spotify_")

            // GQL fetchPlaylist
            try {
                val variables = JSONObject().apply {
                    put("uri", "spotify:playlist:$rawId")
                    put("offset", offset)
                    put("limit", limit)
                    put("enableWatchFeedEntrypoint", false)
                }

                val response = graphqlPost("fetchPlaylist", HASH_FETCH_PLAYLIST, variables, token)
                val tracks = parseGqlPlaylistTracks(response)
                if (tracks.isNotEmpty()) {
                    return@runCatching tracks
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Try REST API for tracks fallback
            val jsonStr = httpGet("$REST_URL/playlists/$rawId/tracks?limit=$limit&offset=$offset", mapOf("Authorization" to "Bearer $token", "App-Platform" to "WebPlayer"))
            val json = JSONObject(jsonStr)
            val items = json.optJSONArray("items") ?: JSONArray()
            val tracks = mutableListOf<SpotifyTrack>()

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val trackObj = item.optJSONObject("track") ?: continue
                val id = trackObj.optString("id", "")
                val name = trackObj.optString("name", "")
                val durationMs = trackObj.optInt("duration_ms", 0)

                val artistsArr = trackObj.optJSONArray("artists")
                val artists = mutableListOf<SpotifySimpleArtist>()
                if (artistsArr != null) {
                    for (a in 0 until artistsArr.length()) {
                        val art = artistsArr.optJSONObject(a) ?: continue
                        artists.add(SpotifySimpleArtist(art.optString("id"), art.optString("name")))
                    }
                }

                val albumObj = trackObj.optJSONObject("album")
                var album: SpotifySimpleAlbum? = null
                if (albumObj != null) {
                    val albumImagesArr = albumObj.optJSONArray("images")
                    val albumImages = mutableListOf<SpotifyImage>()
                    if (albumImagesArr != null) {
                        for (imgIdx in 0 until albumImagesArr.length()) {
                            val img = albumImagesArr.optJSONObject(imgIdx) ?: continue
                            albumImages.add(SpotifyImage(img.optString("url")))
                        }
                    }
                    album = SpotifySimpleAlbum(
                        id = albumObj.optString("id"),
                        name = albumObj.optString("name"),
                        images = albumImages
                    )
                }

                tracks.add(SpotifyTrack(id = id, name = name, artists = artists, album = album, durationMs = durationMs))
            }
            tracks
        }
    }

    suspend fun searchArtistImage(artistName: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            if (artistName.isBlank()) return@runCatching null
            SpotifySession.ensureValidToken()
            var token = SpotifySession.accessToken
            if (token.isBlank()) return@runCatching null

            val headers = mapOf(
                "Authorization" to "Bearer $token",
                "App-Platform" to "WebPlayer",
                "Spotify-App-Version" to "1.2.56.502.g87a81093"
            )

            val encodedQuery = URLEncoder.encode(artistName, "UTF-8")
            val jsonStr = try {
                httpGet("$REST_URL/search?q=$encodedQuery&type=artist&limit=1", headers)
            } catch (e: Exception) {
                SpotifyAuth.fetchAccessToken("").getOrNull()?.let { newToken ->
                    SpotifySession.saveSession("", newToken)
                    token = newToken.accessToken
                    httpGet("$REST_URL/search?q=$encodedQuery&type=artist&limit=1", mapOf(
                        "Authorization" to "Bearer $token",
                        "App-Platform" to "WebPlayer"
                    ))
                } ?: throw e
            }

            val json = JSONObject(jsonStr)
            val artists = json.optJSONObject("artists")?.optJSONArray("items") ?: JSONArray()

            if (artists.length() > 0) {
                val artistObj = artists.getJSONObject(0)
                val images = artistObj.optJSONArray("images")
                if (images != null && images.length() > 0) {
                    val imgUrl = images.getJSONObject(0).optString("url", "")
                    if (imgUrl.isNotBlank()) {
                        return@runCatching imgUrl
                    }
                }
            }
            null
        }
    }

    private fun parseGqlLibraryPlaylists(responseJson: JSONObject): List<SpotifyPlaylist> {
        val playlists = mutableListOf<SpotifyPlaylist>()
        val dataObj = responseJson.optJSONObject("data") ?: return emptyList()
        val meObj = dataObj.optJSONObject("me") ?: return emptyList()
        val libraryV3 = meObj.optJSONObject("libraryV3") ?: return emptyList()
        val items = libraryV3.optJSONArray("items") ?: return emptyList()

        for (i in 0 until items.length()) {
            val itemElem = items.optJSONObject(i) ?: continue
            val wrapper = itemElem.optJSONObject("item") ?: continue
            val typeName = wrapper.optString("__typename", "")

            if (typeName.isNotBlank() && typeName != "PlaylistResponseWrapper" && !typeName.contains("Playlist", ignoreCase = true)) {
                continue
            }

            val data = wrapper.optJSONObject("data") ?: wrapper
            val dataTypeName = data.optString("__typename", "")
            if (dataTypeName.isNotBlank() && dataTypeName != "Playlist") {
                continue
            }

            val uri = wrapper.optString("_uri", data.optString("uri", wrapper.optString("uri", "")))
            val id = if (uri.contains(":")) uri.substringAfterLast(":") else uri
            val name = data.optString("name", wrapper.optString("name", ""))
            if (name.isBlank() && id.isBlank()) continue

            val description = data.optString("description", "")
            val images = parseGqlPlaylistImages(data.optJSONObject("images") ?: wrapper.optJSONObject("images"))
            val ownerData = data.optJSONObject("ownerV2")?.optJSONObject("data")
            val ownerId = ownerData?.optString("uri", "")?.substringAfterLast(":") ?: ownerData?.optString("id", "") ?: ""
            val owner = SpotifyPlaylistOwner(
                id = ownerId,
                displayName = ownerData?.optString("name"),
                uri = ownerData?.optString("uri")
            )

            playlists.add(SpotifyPlaylist(id = id, name = name, description = description, images = images, owner = owner, uri = uri))
        }
        return playlists
    }

    private fun parseGqlPlaylistImages(imagesObj: JSONObject?): List<SpotifyImage> {
        if (imagesObj == null) return emptyList()
        val images = mutableListOf<SpotifyImage>()

        val itemsArr = imagesObj.optJSONArray("items")
        if (itemsArr != null) {
            for (i in 0 until itemsArr.length()) {
                val group = itemsArr.optJSONObject(i) ?: continue
                val sourcesArr = group.optJSONArray("sources")
                if (sourcesArr != null) {
                    for (s in 0 until sourcesArr.length()) {
                        val src = sourcesArr.optJSONObject(s) ?: continue
                        val url = src.optString("url", "")
                        if (url.isNotBlank()) images.add(SpotifyImage(url))
                    }
                } else {
                    val url = group.optString("url", "")
                    if (url.isNotBlank()) images.add(SpotifyImage(url))
                }
            }
        } else {
            val sourcesArr = imagesObj.optJSONArray("sources")
            if (sourcesArr != null) {
                for (s in 0 until sourcesArr.length()) {
                    val src = sourcesArr.optJSONObject(s) ?: continue
                    val url = src.optString("url", "")
                    if (url.isNotBlank()) images.add(SpotifyImage(url))
                }
            }
        }
        return images
    }

    private fun parseGqlPlaylistTracks(responseJson: JSONObject): List<SpotifyTrack> {
        val tracks = mutableListOf<SpotifyTrack>()
        val dataObj = responseJson.optJSONObject("data") ?: return emptyList()
        val playlistV2 = dataObj.optJSONObject("playlistV2") ?: return emptyList()
        val content = playlistV2.optJSONObject("content") ?: return emptyList()
        val itemsArr = content.optJSONArray("items") ?: return emptyList()

        for (i in 0 until itemsArr.length()) {
            val elem = itemsArr.optJSONObject(i) ?: continue
            val itemWrapper = elem.optJSONObject("itemV2") ?: continue
            val itemData = itemWrapper.optJSONObject("data") ?: continue
            val wrapperUri = itemWrapper.optString("_uri", itemWrapper.optString("uri", itemData.optString("uri", "")))
            val trackId = if (wrapperUri.contains(":")) wrapperUri.substringAfterLast(":") else wrapperUri
            val name = itemData.optString("name", "")
            if (name.isBlank() && trackId.isBlank()) continue

            val durationObj = itemData.optJSONObject("duration")
            val durationMs = durationObj?.optInt("totalMilliseconds", 0) ?: itemData.optInt("durationMs", itemData.optInt("duration_ms", 0))

            val artists = mutableListOf<SpotifySimpleArtist>()
            val artistsArr = itemData.optJSONObject("artists")?.optJSONArray("items")
            if (artistsArr != null) {
                for (a in 0 until artistsArr.length()) {
                    val artElem = artistsArr.optJSONObject(a) ?: continue
                    val artData = artElem.optJSONObject("profile") ?: artElem
                    val artUri = artElem.optString("uri", artData.optString("uri", ""))
                    val artId = if (artUri.contains(":")) artUri.substringAfterLast(":") else artUri
                    val artName = artData.optString("name", "")
                    artists.add(SpotifySimpleArtist(artId, artName))
                }
            }

            val albumData = itemData.optJSONObject("albumOfTrack")
            var album: SpotifySimpleAlbum? = null
            if (albumData != null) {
                val albumUri = albumData.optString("uri", "")
                val albumId = if (albumUri.contains(":")) albumUri.substringAfterLast(":") else albumUri
                val albumName = albumData.optString("name", "")
                val coverArtSources = albumData.optJSONObject("coverArt")?.optJSONArray("sources")
                val albumImages = mutableListOf<SpotifyImage>()
                if (coverArtSources != null) {
                    for (s in 0 until coverArtSources.length()) {
                        val src = coverArtSources.optJSONObject(s) ?: continue
                        val url = src.optString("url", "")
                        if (url.isNotBlank()) albumImages.add(SpotifyImage(url))
                    }
                }
                album = SpotifySimpleAlbum(id = albumId, name = albumName, images = albumImages)
            }

            tracks.add(SpotifyTrack(id = trackId, name = name, artists = artists, album = album, durationMs = durationMs))
        }
        return tracks
    }

    suspend fun myArtists(limit: Int = 50, offset: Int = 0): Result<List<SpotifyArtist>> = withContext(Dispatchers.IO) {
        runCatching {
            SpotifySession.ensureValidToken()
            val token = SpotifySession.accessToken

            val allArtists = mutableListOf<SpotifyArtist>()
            var currentOffset = offset
            var hasMore = true
            var safetyCounter = 0

            while (hasMore && safetyCounter < 10) {
                safetyCounter++
                val variables = JSONObject().apply {
                    put("filters", JSONArray().apply { put("Artists") })
                    put("order", JSONObject.NULL)
                    put("textFilter", "")
                    put("features", JSONArray().apply {
                        put("LIKED_SONGS")
                        put("YOUR_EPISODES_V2")
                        put("PRERELEASES")
                        put("EVENTS")
                    })
                    put("limit", limit)
                    put("offset", currentOffset)
                    put("flatten", false)
                    put("expandedFolders", JSONArray())
                    put("folderUri", JSONObject.NULL)
                    put("includeFoldersWhenFlattening", true)
                }

                val gqlResult = graphqlPost("libraryV3", HASH_LIBRARY_V3, variables, token)
                val libraryData = gqlResult.optJSONObject("data")?.optJSONObject("me")?.optJSONObject("libraryV3")
                val itemsArr = libraryData?.optJSONArray("items")

                if (itemsArr == null || itemsArr.length() == 0) {
                    hasMore = false
                    break
                }

                val countBefore = allArtists.size
                for (i in 0 until itemsArr.length()) {
                    val itemElem = itemsArr.optJSONObject(i) ?: continue
                    val wrapper = itemElem.optJSONObject("item") ?: continue
                    val typeName = wrapper.optString("__typename", "")
                    if (!typeName.contains("Artist", ignoreCase = true)) continue

                    val data = wrapper.optJSONObject("data") ?: continue
                    val artistUri = wrapper.optString("_uri", data.optString("uri", ""))
                    if (artistUri.isBlank()) continue
                    val artistId = if (artistUri.contains(":")) artistUri.substringAfterLast(":") else artistUri

                    val name = data.optJSONObject("profile")?.optString("name") ?: data.optString("name", "")
                    if (name.isBlank()) continue

                    val images = mutableListOf<SpotifyImage>()
                    val sourcesArr = data.optJSONObject("visuals")?.optJSONObject("avatarImage")?.optJSONArray("sources")
                    if (sourcesArr != null) {
                        for (s in 0 until sourcesArr.length()) {
                            val src = sourcesArr.optJSONObject(s) ?: continue
                            val url = src.optString("url", "")
                            if (url.isNotBlank()) images.add(SpotifyImage(url))
                        }
                    }

                    allArtists.add(SpotifyArtist(id = artistId, name = name, images = images, uri = artistUri))
                }

                if (itemsArr.length() < limit || allArtists.size == countBefore) {
                    hasMore = false
                } else {
                    currentOffset += limit
                }
            }

            allArtists
        }
    }

    suspend fun myAlbums(limit: Int = 50, offset: Int = 0): Result<List<SpotifyAlbum>> = withContext(Dispatchers.IO) {
        runCatching {
            SpotifySession.ensureValidToken()
            val token = SpotifySession.accessToken

            val allAlbums = mutableListOf<SpotifyAlbum>()
            var currentOffset = offset
            var hasMore = true
            var safetyCounter = 0

            while (hasMore && safetyCounter < 10) {
                safetyCounter++
                val variables = JSONObject().apply {
                    put("filters", JSONArray().apply { put("Albums") })
                    put("order", JSONObject.NULL)
                    put("textFilter", "")
                    put("features", JSONArray().apply {
                        put("LIKED_SONGS")
                        put("YOUR_EPISODES_V2")
                        put("PRERELEASES")
                        put("EVENTS")
                    })
                    put("limit", limit)
                    put("offset", currentOffset)
                    put("flatten", false)
                    put("expandedFolders", JSONArray())
                    put("folderUri", JSONObject.NULL)
                    put("includeFoldersWhenFlattening", true)
                }

                val gqlResult = graphqlPost("libraryV3", HASH_LIBRARY_V3, variables, token)
                val libraryData = gqlResult.optJSONObject("data")?.optJSONObject("me")?.optJSONObject("libraryV3")
                val itemsArr = libraryData?.optJSONArray("items")

                if (itemsArr == null || itemsArr.length() == 0) {
                    hasMore = false
                    break
                }

                val countBefore = allAlbums.size
                for (i in 0 until itemsArr.length()) {
                    val itemElem = itemsArr.optJSONObject(i) ?: continue
                    val wrapper = itemElem.optJSONObject("item") ?: continue
                    val typeName = wrapper.optString("__typename", "")
                    if (!typeName.contains("Album", ignoreCase = true)) continue

                    val data = wrapper.optJSONObject("data") ?: continue
                    val albumUri = wrapper.optString("_uri", data.optString("uri", ""))
                    if (albumUri.isBlank()) continue
                    val albumId = if (albumUri.contains(":")) albumUri.substringAfterLast(":") else albumUri
                    val name = data.optString("name", "")
                    if (name.isBlank()) continue

                    val artists = mutableListOf<SpotifySimpleArtist>()
                    val artistsArr = data.optJSONObject("artists")?.optJSONArray("items")
                    if (artistsArr != null) {
                        for (a in 0 until artistsArr.length()) {
                            val artElem = artistsArr.optJSONObject(a) ?: continue
                            val artName = artElem.optJSONObject("profile")?.optString("name", "") ?: artElem.optString("name", "")
                            val artUri = artElem.optString("uri", "")
                            val artId = if (artUri.contains(":")) artUri.substringAfterLast(":") else artUri
                            if (artName.isNotBlank()) artists.add(SpotifySimpleArtist(id = artId, name = artName, uri = artUri))
                        }
                    }

                    val images = mutableListOf<SpotifyImage>()
                    val sourcesArr = data.optJSONObject("coverArt")?.optJSONArray("sources")
                    if (sourcesArr != null) {
                        for (s in 0 until sourcesArr.length()) {
                            val src = sourcesArr.optJSONObject(s) ?: continue
                            val url = src.optString("url", "")
                            if (url.isNotBlank()) images.add(SpotifyImage(url))
                        }
                    }

                    allAlbums.add(SpotifyAlbum(id = albumId, name = name, artists = artists, images = images, uri = albumUri))
                }

                if (itemsArr.length() < limit || allAlbums.size == countBefore) {
                    hasMore = false
                } else {
                    currentOffset += limit
                }
            }

            allAlbums
        }
    }

    private fun graphqlPost(operationName: String, sha256Hash: String, variables: JSONObject, token: String): JSONObject {
        val body = JSONObject().apply {
            put("variables", variables)
            put("operationName", operationName)
            put("extensions", JSONObject().apply {
                put("persistedQuery", JSONObject().apply {
                    put("version", 1)
                    put("sha256Hash", sha256Hash)
                })
            })
        }

        val response = httpPostJson(GQL_URL, body.toString(), mapOf(
            "Authorization" to "Bearer $token",
            "app-platform" to "WebPlayer",
            "Origin" to "https://open.spotify.com",
            "Referer" to "https://open.spotify.com/",
            "Accept" to "application/json",
            "Spotify-App-Version" to "1.2.56.502.g87a81093"
        ))
        return JSONObject(response)
    }

    private fun httpPostJson(urlString: String, jsonBody: String, extraHeaders: Map<String, String>): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")
            for ((key, value) in extraHeaders) {
                connection.setRequestProperty(key, value)
            }

            connection.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw Exception("HTTP $responseCode: $errorBody")
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun httpGet(urlString: String, extraHeaders: Map<String, String>): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")
            for ((key, value) in extraHeaders) {
                connection.setRequestProperty(key, value)
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw Exception("HTTP $responseCode: $errorBody")
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
