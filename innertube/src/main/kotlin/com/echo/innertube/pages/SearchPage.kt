package com.echo.innertube.pages

import com.echo.innertube.models.Album
import com.echo.innertube.models.AlbumItem
import com.echo.innertube.models.Artist
import com.echo.innertube.models.ArtistItem
import com.echo.innertube.models.MusicResponsiveListItemRenderer
import com.echo.innertube.models.PlaylistItem
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.YTItem
import com.echo.innertube.models.oddElements
import com.echo.innertube.models.splitBySeparator
import com.echo.innertube.utils.parseTime

data class SearchResult(
    val items: List<YTItem>,
    val continuation: String? = null,
)

object SearchPage {
    fun toYTItem(renderer: MusicResponsiveListItemRenderer): YTItem? {
        val secondaryLine =
            renderer.flexColumns
                .getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.splitBySeparator()
                ?: return null
        return when {
            renderer.isSong -> {
                SongItem(
                    id = renderer.playlistItemData?.videoId
                        ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                        ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
                        ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    artists =
                        secondaryLine.firstOrNull()?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: return null,
                    album =
                        secondaryLine.getOrNull(1)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                            )
                        },
                    duration =
                        secondaryLine
                            .lastOrNull()
                            ?.firstOrNull()
                            ?.text
                            ?.parseTime(),
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit =
                        renderer.badges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null,
                    libraryAddToken = PageHelper.extractFeedbackToken(renderer.menu?.menuRenderer?.items?.find {
                        it.toggleMenuServiceItemRenderer?.defaultIcon?.iconType?.startsWith("LIBRARY_") == true
                    }?.toggleMenuServiceItemRenderer, "LIBRARY_ADD"),
                    libraryRemoveToken = PageHelper.extractFeedbackToken(renderer.menu?.menuRenderer?.items?.find {
                        it.toggleMenuServiceItemRenderer?.defaultIcon?.iconType?.startsWith("LIBRARY_") == true
                    }?.toggleMenuServiceItemRenderer, "LIBRARY_SAVED"),
                    musicVideoType = renderer.musicVideoType
                )
            }
            renderer.isArtist -> {
                val artistBrowseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null
                val artistTitle = renderer.flexColumns
                    .firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text
                    ?.runs
                    ?.firstOrNull()
                    ?.text
                    ?: return null

                ArtistItem(
                    id = artistBrowseId,
                    title = artistTitle,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                    shuffleEndpoint = renderer.menu
                        ?.menuRenderer
                        ?.items
                        ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                        ?.menuNavigationItemRenderer
                        ?.navigationEndpoint
                        ?.watchPlaylistEndpoint,
                    radioEndpoint = renderer.menu
                        ?.menuRenderer
                        ?.items
                        ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                        ?.menuNavigationItemRenderer
                        ?.navigationEndpoint
                        ?.watchPlaylistEndpoint,
                )
            }
            renderer.isAlbum -> {
                val albumBrowseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null
                val albumTitle = renderer.flexColumns
                    .firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text
                    ?.runs
                    ?.firstOrNull()
                    ?.text ?: return null

                val albumPlaylistId = renderer.overlay
                    ?.musicItemThumbnailOverlayRenderer
                    ?.content
                    ?.musicPlayButtonRenderer
                    ?.playNavigationEndpoint
                    ?.anyWatchEndpoint
                    ?.playlistId
                    ?: renderer.menu?.menuRenderer?.items
                        ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                        ?.menuNavigationItemRenderer
                        ?.navigationEndpoint
                        ?.watchPlaylistEndpoint
                        ?.playlistId
                    ?: if (albumBrowseId.startsWith("MPREb_")) "OLAK5uy_${albumBrowseId.removePrefix("MPREb_")}" else ""

                val parsedArtists = secondaryLine.getOrNull(1)?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                } ?: secondaryLine.firstOrNull()?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                }

                AlbumItem(
                    browseId = albumBrowseId,
                    playlistId = albumPlaylistId,
                    title = albumTitle,
                    artists = parsedArtists,
                    year = secondaryLine
                        .getOrNull(2)
                        ?.firstOrNull()
                        ?.text
                        ?.toIntOrNull()
                        ?: secondaryLine.getOrNull(1)?.lastOrNull()?.text?.toIntOrNull(),
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: "",
                    explicit = renderer.badges?.find {
                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                    } != null,
                )
            }
            renderer.isPlaylist -> {
                val playlistId = renderer.navigationEndpoint
                    ?.browseEndpoint
                    ?.browseId
                    ?.removePrefix("VL") ?: return null

                val playlistTitle = renderer.flexColumns
                    .firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text
                    ?.runs
                    ?.firstOrNull()
                    ?.text ?: return null

                PlaylistItem(
                    id = playlistId,
                    title = playlistTitle,
                    author = secondaryLine.firstOrNull()?.firstOrNull()?.let {
                        Artist(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId,
                        )
                    } ?: secondaryLine.getOrNull(1)?.firstOrNull()?.let {
                        Artist(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId,
                        )
                    },
                    songCountText = renderer.flexColumns
                        .getOrNull(1)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.lastOrNull()
                        ?.text,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                    playEndpoint = renderer.overlay
                        ?.musicItemThumbnailOverlayRenderer
                        ?.content
                        ?.musicPlayButtonRenderer
                        ?.playNavigationEndpoint
                        ?.watchPlaylistEndpoint,
                    shuffleEndpoint = renderer.menu
                        ?.menuRenderer
                        ?.items
                        ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                        ?.menuNavigationItemRenderer
                        ?.navigationEndpoint
                        ?.watchPlaylistEndpoint,
                    radioEndpoint = renderer.menu
                        ?.menuRenderer
                        ?.items
                        ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                        ?.menuNavigationItemRenderer
                        ?.navigationEndpoint
                        ?.watchPlaylistEndpoint,
                )
            }
            else -> null
        }
    }
}
