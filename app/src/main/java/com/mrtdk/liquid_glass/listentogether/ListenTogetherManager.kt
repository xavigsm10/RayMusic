package com.mrtdk.liquid_glass.listentogether

import android.content.Context
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ListenTogetherManager(
    val client: ListenTogetherClient,
    private val context: Context
) {
    companion object {
        @Volatile
        private var instance: ListenTogetherManager? = null

        fun getInstance(context: Context): ListenTogetherManager {
            return instance ?: synchronized(this) {
                val client = ListenTogetherClient.getInstance(context)
                instance ?: ListenTogetherManager(client, context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile
    var isSyncing: Boolean = false
        private set

    var onSongSelectedCallback: ((PlayerState) -> Unit)? = null
    var onTogglePlayPauseCallback: (() -> Unit)? = null
    var onSeekCallback: ((Float) -> Unit)? = null

    // Expose client state flows directly - same as Convx
    val connectionState: StateFlow<ConnectionState> = client.connectionState
    val roomState: StateFlow<RoomState?> = client.roomState
    val role: StateFlow<RoomRole> = client.role
    val userId: StateFlow<String?> = client.userId
    val pendingJoinRequests: StateFlow<List<JoinRequestPayload>> = client.pendingJoinRequests
    val pendingSuggestions: StateFlow<List<SuggestionReceivedPayload>> = client.pendingSuggestions

    // Computed properties - same as Convx
    val isInRoom: Boolean get() = client.isInRoom
    val isHost: Boolean get() = client.isHost

    init {
        scope.launch {
            client.events.collect { event ->
                when (event) {
                    is ListenTogetherEvent.PlaybackSync -> {
                        if (!isHost) {
                            handleHostPlaybackAction(event.action)
                        }
                    }
                    is ListenTogetherEvent.SyncStateReceived -> {
                        if (!isHost && event.state.currentTrack != null) {
                            handleSyncState(event.state)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleHostPlaybackAction(action: PlaybackActionPayload) {
        isSyncing = true
        try {
            when (action.action) {
                PlaybackActions.CHANGE_TRACK -> {
                    val track = action.trackInfo
                    if (track != null) {
                        val state = PlayerState(
                            title = track.title,
                            artist = track.artist,
                            artUrl = track.thumbnail,
                            videoId = track.id,
                            album = track.album
                        )
                        onSongSelectedCallback?.invoke(state)
                    }
                }
                PlaybackActions.PLAY, PlaybackActions.PAUSE -> {
                    onTogglePlayPauseCallback?.invoke()
                }
                PlaybackActions.SEEK -> {
                    val posMs = action.position ?: 0L
                    onSeekCallback?.invoke(posMs.toFloat())
                }
            }
        } finally {
            isSyncing = false
        }
    }

    private fun handleSyncState(sync: SyncStatePayload) {
        isSyncing = true
        try {
            val track = sync.currentTrack
            if (track != null) {
                val state = PlayerState(
                    title = track.title,
                    artist = track.artist,
                    artUrl = track.thumbnail,
                    videoId = track.id,
                    album = track.album
                )
                onSongSelectedCallback?.invoke(state)
                val posMs = sync.position
                if (posMs > 0) {
                    onSeekCallback?.invoke(posMs.toFloat())
                }
            }
        } finally {
            isSyncing = false
        }
    }

    fun connect() { client.connect() }
    fun disconnect() { client.disconnect() }
    fun createRoom(username: String) { client.createRoom(username) }
    fun joinRoom(roomCode: String, username: String) { client.joinRoom(roomCode, username) }
    fun leaveRoom() { client.leaveRoom() }

    fun onSongSelectedAttempt(playerState: PlayerState): Boolean {
        if (isInRoom && !isHost) {
            val videoId = playerState.videoId ?: return false
            val track = TrackInfo(
                id = videoId,
                title = playerState.title ?: "Desconocido",
                artist = playerState.artist ?: "Desconocido",
                album = playerState.album,
                thumbnail = playerState.artUrl?.toString()
            )
            client.suggestTrack(track)
            android.widget.Toast.makeText(context, "Sugerencia enviada al anfitrion. Solo el anfitrion puede cambiar canciones.", android.widget.Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    fun approveSuggestion(suggestionId: String, trackInfo: TrackInfo) {
        client.approveSuggestion(suggestionId)
        val state = PlayerState(
            title = trackInfo.title,
            artist = trackInfo.artist,
            artUrl = trackInfo.thumbnail,
            videoId = trackInfo.id,
            album = trackInfo.album
        )
        onSongSelectedCallback?.invoke(state)
    }

    fun broadcastSongChange(playerState: PlayerState) {
        if (!isInRoom || !isHost || isSyncing) return
        val videoId = playerState.videoId ?: return
        val track = TrackInfo(
            id = videoId,
            title = playerState.title ?: "Unknown Title",
            artist = playerState.artist ?: "Unknown Artist",
            album = playerState.album,
            thumbnail = playerState.artUrl?.toString()
        )
        client.sendPlaybackAction(
            PlaybackActionPayload(
                action = PlaybackActions.CHANGE_TRACK,
                trackId = videoId,
                trackInfo = track
            )
        )
    }

    fun broadcastPlayPause(isPlaying: Boolean) {
        if (!isInRoom || !isHost || isSyncing) return
        client.sendPlaybackAction(
            PlaybackActionPayload(
                action = if (isPlaying) PlaybackActions.PLAY else PlaybackActions.PAUSE
            )
        )
    }

    fun broadcastSeek(positionMs: Long) {
        if (!isInRoom || !isHost || isSyncing) return
        client.sendPlaybackAction(
            PlaybackActionPayload(
                action = PlaybackActions.SEEK,
                position = positionMs
            )
        )
    }
}
