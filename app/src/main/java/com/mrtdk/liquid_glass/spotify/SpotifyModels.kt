package com.mrtdk.liquid_glass.spotify

data class SpotifyImage(
    val url: String = "",
    val height: Int? = null,
    val width: Int? = null,
)

data class SpotifyToken(
    val accessToken: String,
    val tokenType: String,
    val scope: String = "",
    val expiresIn: Int,
    val refreshToken: String? = null,
)

data class SpotifyInternalToken(
    val accessToken: String,
    val accessTokenExpirationTimestampMs: Long,
    val isAnonymous: Boolean = false,
    val clientId: String = "",
)

data class SpotifyArtist(
    val id: String = "",
    val name: String = "",
    val images: List<SpotifyImage> = emptyList(),
    val genres: List<String> = emptyList(),
    val popularity: Int? = null,
    val uri: String? = null,
)

data class SpotifySimpleArtist(
    val id: String = "",
    val name: String = "",
    val uri: String? = null,
)

data class SpotifySimpleAlbum(
    val id: String = "",
    val name: String = "",
    val images: List<SpotifyImage> = emptyList(),
    val uri: String? = null,
)

data class SpotifyAlbum(
    val id: String = "",
    val name: String = "",
    val artists: List<SpotifySimpleArtist> = emptyList(),
    val images: List<SpotifyImage> = emptyList(),
    val releaseDate: String? = null,
    val uri: String? = null,
)

data class SpotifyTrack(
    val id: String = "",
    val name: String = "",
    val artists: List<SpotifySimpleArtist> = emptyList(),
    val album: SpotifySimpleAlbum? = null,
    val durationMs: Int = 0,
    val uri: String? = null,
)

data class SpotifyPlaylist(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val images: List<SpotifyImage> = emptyList(),
    val owner: SpotifyPlaylistOwner? = null,
    val tracks: SpotifyPlaylistTracksRef? = null,
    val uri: String? = null,
    val public: Boolean? = null,
    val collaborative: Boolean = false,
    val snapshotId: String? = null,
)

data class SpotifyPlaylistOwner(
    val id: String = "",
    val displayName: String? = null,
    val uri: String? = null,
)

data class SpotifyPlaylistTracksRef(
    val total: Int = 0,
    val href: String? = null,
)

data class SpotifyPlaylistTrack(
    val addedAt: String? = null,
    val track: SpotifyTrack? = null,
    val isLocal: Boolean = false,
    val uid: String? = null,
)

data class SpotifyPaging<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0,
)

data class SpotifyUser(
    val id: String = "",
    val displayName: String? = null,
    val images: List<SpotifyImage> = emptyList(),
    val uri: String? = null,
)
