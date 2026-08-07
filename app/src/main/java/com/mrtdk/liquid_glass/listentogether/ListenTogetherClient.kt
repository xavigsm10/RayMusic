package com.mrtdk.liquid_glass.listentogether

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

sealed class ListenTogetherEvent {
    data class Connected(val userId: String) : ListenTogetherEvent()
    data object Disconnected : ListenTogetherEvent()
    data class ConnectionError(val error: String) : ListenTogetherEvent()
    data class RoomCreated(val roomCode: String, val userId: String) : ListenTogetherEvent()
    data class JoinRequestReceived(val userId: String, val username: String) : ListenTogetherEvent()
    data class JoinApproved(val roomCode: String, val userId: String, val state: RoomState) : ListenTogetherEvent()
    data class JoinRejected(val reason: String) : ListenTogetherEvent()
    data class UserJoined(val userId: String, val username: String) : ListenTogetherEvent()
    data class UserLeft(val userId: String, val username: String) : ListenTogetherEvent()
    data class HostChanged(val newHostId: String, val newHostName: String) : ListenTogetherEvent()
    data class Kicked(val reason: String) : ListenTogetherEvent()
    data class PlaybackSync(val action: PlaybackActionPayload) : ListenTogetherEvent()
    data class SyncStateReceived(val state: SyncStatePayload) : ListenTogetherEvent()
    data class ServerError(val code: String, val message: String) : ListenTogetherEvent()
}

sealed class PendingAction {
    data class CreateRoom(val username: String) : PendingAction()
    data class JoinRoom(val roomCode: String, val username: String) : PendingAction()
}

class ListenTogetherClient(private val context: Context) {
    companion object {
        @Volatile
        private var instance: ListenTogetherClient? = null

        fun getInstance(context: Context): ListenTogetherClient {
            return instance ?: synchronized(this) {
                instance ?: ListenTogetherClient(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val codec = MessageCodec()

    private var okHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var pendingAction: PendingAction? = null
    private var storedUsername: String? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _roomState = MutableStateFlow<RoomState?>(null)
    val roomState: StateFlow<RoomState?> = _roomState.asStateFlow()

    private val _role = MutableStateFlow(RoomRole.NONE)
    val role: StateFlow<RoomRole> = _role.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _pendingJoinRequests = MutableStateFlow<List<JoinRequestPayload>>(emptyList())
    val pendingJoinRequests: StateFlow<List<JoinRequestPayload>> = _pendingJoinRequests.asStateFlow()

    private val _pendingSuggestions = MutableStateFlow<List<SuggestionReceivedPayload>>(emptyList())
    val pendingSuggestions: StateFlow<List<SuggestionReceivedPayload>> = _pendingSuggestions.asStateFlow()

    private val _events = MutableSharedFlow<ListenTogetherEvent>()
    val events: SharedFlow<ListenTogetherEvent> = _events.asSharedFlow()

    var currentServerUrl: String = ListenTogetherServers.defaultServerUrl
        private set

    val isInRoom: Boolean get() = _roomState.value != null
    val isHost: Boolean get() = _role.value == RoomRole.HOST

    fun connect(serverUrl: String = currentServerUrl) {
        if (_connectionState.value == ConnectionState.CONNECTED || _connectionState.value == ConnectionState.CONNECTING) {
            if (serverUrl == currentServerUrl && webSocket != null) return
            disconnect()
        }
        currentServerUrl = serverUrl
        _connectionState.value = ConnectionState.CONNECTING
        val request = Request.Builder().url(serverUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED
                executePendingAction()
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.ERROR
                scope.launch { _events.emit(ListenTogetherEvent.ConnectionError(t.message ?: "Connection failure")) }
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
                scope.launch { _events.emit(ListenTogetherEvent.Disconnected) }
            }
        })
    }

    private fun executePendingAction() {
        val action = pendingAction ?: return
        pendingAction = null
        when (action) {
            is PendingAction.CreateRoom -> send(MessageTypes.CREATE_ROOM, CreateRoomPayload(action.username))
            is PendingAction.JoinRoom -> send(MessageTypes.JOIN_ROOM, JoinRoomPayload(action.roomCode.uppercase(), action.username))
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _roomState.value = null
        _role.value = RoomRole.NONE
        _userId.value = null
        _pendingJoinRequests.value = emptyList()
        _pendingSuggestions.value = emptyList()
        pendingAction = null
    }

    private fun send(type: String, payload: Any? = null) {
        val messageStr = codec.encode(type, payload)
        webSocket?.send(messageStr)
    }

    fun createRoom(username: String) {
        storedUsername = username
        if (_connectionState.value == ConnectionState.CONNECTED && webSocket != null) {
            send(MessageTypes.CREATE_ROOM, CreateRoomPayload(username))
        } else {
            pendingAction = PendingAction.CreateRoom(username)
            connect()
        }
    }

    fun joinRoom(roomCode: String, username: String) {
        val cleanCode = roomCode.uppercase().trim()
        storedUsername = username
        if (_connectionState.value == ConnectionState.CONNECTED && webSocket != null) {
            send(MessageTypes.JOIN_ROOM, JoinRoomPayload(cleanCode, username))
        } else {
            pendingAction = PendingAction.JoinRoom(cleanCode, username)
            connect()
        }
    }

    fun leaveRoom() {
        send(MessageTypes.LEAVE_ROOM)
        _roomState.value = null
        _role.value = RoomRole.NONE
        _pendingJoinRequests.value = emptyList()
        _pendingSuggestions.value = emptyList()
    }

    fun approveJoin(userId: String) {
        val req = _pendingJoinRequests.value.firstOrNull { it.userId == userId }
        send(MessageTypes.APPROVE_JOIN, ApproveJoinPayload(userId))
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != userId }
        if (req != null) {
            val current = _roomState.value
            if (current != null) {
                val newUser = UserInfo(userId = req.userId, username = req.username, isHost = false)
                if (current.users.none { it.userId == req.userId }) {
                    _roomState.value = current.copy(users = current.users + newUser)
                }
            }
        }
    }

    fun rejectJoin(userId: String, reason: String? = null) {
        send(MessageTypes.REJECT_JOIN, RejectJoinPayload(userId, reason))
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != userId }
    }

    fun sendPlaybackAction(action: PlaybackActionPayload) { send(MessageTypes.PLAYBACK_ACTION, action) }
    fun suggestTrack(track: TrackInfo) { send(MessageTypes.SUGGEST_TRACK, SuggestTrackPayload(track)) }
    fun approveSuggestion(suggestionId: String) {
        send(MessageTypes.APPROVE_SUGGESTION, ApproveSuggestionPayload(suggestionId))
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
    }
    fun rejectSuggestion(suggestionId: String, reason: String? = null) {
        send(MessageTypes.REJECT_SUGGESTION, RejectSuggestionPayload(suggestionId, reason))
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
    }
    fun kickUser(userId: String, reason: String? = null) { send(MessageTypes.KICK_USER, KickUserPayload(userId, reason)) }
    fun transferHost(newHostId: String) { send(MessageTypes.TRANSFER_HOST, TransferHostPayload(newHostId)) }

    private fun handleIncomingMessage(text: String) {
        try {
            val message = codec.decode(text)
            val json = codec.jsonInstance()
            val payload = message.payload
            when (message.type) {
                MessageTypes.ROOM_CREATED -> {
                    if (payload != null) {
                        val res = json.decodeFromJsonElement(RoomCreatedPayload.serializer(), payload)
                        _userId.value = res.userId
                        _role.value = RoomRole.HOST
                        _roomState.value = RoomState(
                            roomCode = res.roomCode,
                            hostId = res.userId,
                            users = listOf(UserInfo(userId = res.userId, username = storedUsername ?: "", isHost = true))
                        )
 scope.launch { _events.emit(ListenTogetherEvent.RoomCreated(res.roomCode, res.userId)) }
 }
 }
 MessageTypes.JOIN_REQUEST -> {
 if (payload != null) {
 val req = json.decodeFromJsonElement(JoinRequestPayload.serializer(), payload)
 _pendingJoinRequests.value = _pendingJoinRequests.value + req
 scope.launch { _events.emit(ListenTogetherEvent.JoinRequestReceived(req.userId, req.username)) }
 }
 }
 MessageTypes.JOIN_APPROVED -> {
 if (payload != null) {
 val app = json.decodeFromJsonElement(JoinApprovedPayload.serializer(), payload)
 _userId.value = app.userId
 _role.value = RoomRole.GUEST
 _roomState.value = app.state
 scope.launch { _events.emit(ListenTogetherEvent.JoinApproved(app.roomCode, app.userId, app.state)) }
 }
 }
 MessageTypes.JOIN_REJECTED -> {
 if (payload != null) {
 val rej = json.decodeFromJsonElement(JoinRejectedPayload.serializer(), payload)
 scope.launch { _events.emit(ListenTogetherEvent.JoinRejected(rej.reason)) }
 }
 }
 MessageTypes.USER_JOINED -> {
 if (payload != null) {
 val u = json.decodeFromJsonElement(UserJoinedPayload.serializer(), payload)
 val currentRoom = _roomState.value
 if (currentRoom != null) {
 _roomState.value = currentRoom.copy(users = currentRoom.users + UserInfo(u.userId, u.username, isHost = false))
 }
 scope.launch { _events.emit(ListenTogetherEvent.UserJoined(u.userId, u.username)) }
 }
 }
 MessageTypes.USER_LEFT -> {
 if (payload != null) {
 val u = json.decodeFromJsonElement(UserLeftPayload.serializer(), payload)
 val currentRoom = _roomState.value
 if (currentRoom != null) {
 _roomState.value = currentRoom.copy(users = currentRoom.users.filter { it.userId != u.userId })
 }
 scope.launch { _events.emit(ListenTogetherEvent.UserLeft(u.userId, u.username)) }
 }
 }
 MessageTypes.SYNC_PLAYBACK -> {
 if (payload != null) {
 val action = json.decodeFromJsonElement(PlaybackActionPayload.serializer(), payload)
 scope.launch { _events.emit(ListenTogetherEvent.PlaybackSync(action)) }
 }
 }
 MessageTypes.SYNC_STATE -> {
 if (payload != null) {
 val sync = json.decodeFromJsonElement(SyncStatePayload.serializer(), payload)
 scope.launch { _events.emit(ListenTogetherEvent.SyncStateReceived(sync)) }
 }
 }
 MessageTypes.HOST_CHANGED -> {
 if (payload != null) {
 val hc = json.decodeFromJsonElement(HostChangedPayload.serializer(), payload)
 _role.value = if (hc.newHostId == _userId.value) RoomRole.HOST else RoomRole.GUEST
 scope.launch { _events.emit(ListenTogetherEvent.HostChanged(hc.newHostId, hc.newHostName)) }
 }
 }
 MessageTypes.KICKED -> {
 if (payload != null) {
 val k = json.decodeFromJsonElement(KickedPayload.serializer(), payload)
 _roomState.value = null
 _role.value = RoomRole.NONE
 scope.launch { _events.emit(ListenTogetherEvent.Kicked(k.reason)) }
 }
 }
 MessageTypes.SUGGESTION_RECEIVED -> {
 if (payload != null) {
 val sug = json.decodeFromJsonElement(SuggestionReceivedPayload.serializer(), payload)
 _pendingSuggestions.value = _pendingSuggestions.value + sug
 }
 }
 MessageTypes.ERROR -> {
 if (payload != null) {
 val err = json.decodeFromJsonElement(ErrorPayload.serializer(), payload)
 scope.launch { _events.emit(ListenTogetherEvent.ServerError(err.code, err.message)) }
 }
 }
 }
 } catch (e: Exception) {
 e.printStackTrace()
 }
 }
}
