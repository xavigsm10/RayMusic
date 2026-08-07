package com.mrtdk.liquid_glass.playback

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ListenTogetherManager {
    private const val TAG = "ListenTogetherManager"
    private const val PING_INTERVAL_MS = 25000L
    private const val MAX_RECONNECT_ATTEMPTS = 15

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    var isConnected = false
        private set

    var roomCode = ""
        private set

    var role = "none" // "host", "guest", "none"
        private set

    var username = ""

    var sessionToken: String = ""
        private set

    var userId: String = ""
        private set

    var serverUrl = ListenTogetherServers.currentServerUrl
        set(value) {
            field = value
            com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_server_url", value)
        }

    var hostUsername = ""
        private set

    val users = mutableListOf<String>()
    val logs = mutableListOf<String>()

    // Event listeners
    var onStateChanged: (() -> Unit)? = null
    var onPlaybackActionReceived: ((action: String, position: Long?, trackId: String?, title: String?, artist: String?, artUrl: String?) -> Unit)? = null
    var onJoinRequestReceived: ((userId: String, username: String) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var isSyncing = false

    private var isExplicitDisconnect = false
    private var reconnectAttempts = 0

    private val pingRunnable = object : Runnable {
        override fun run() {
            if (isConnected && webSocket != null) {
                sendPing()
                mainHandler.postDelayed(this, PING_INTERVAL_MS)
            }
        }
    }

    init {
        // Load configurations on initialization
        val savedServer = com.mrtdk.liquid_glass.data.LibraryManager.getString("listen_together_server_url", "")
        if (!savedServer.isNullOrBlank()) {
            serverUrl = savedServer
        } else {
            serverUrl = ListenTogetherServers.currentServerUrl
        }

        val savedUser = com.mrtdk.liquid_glass.data.LibraryManager.getString("listen_together_username", "")
        if (!savedUser.isNullOrBlank()) {
            username = savedUser
        }

        val savedToken = com.mrtdk.liquid_glass.data.LibraryManager.getString("listen_together_session_token", "")
        if (!savedToken.isNullOrBlank()) {
            sessionToken = savedToken
        }

        val savedRoom = com.mrtdk.liquid_glass.data.LibraryManager.getString("listen_together_room_code", "")
        if (!savedRoom.isNullOrBlank()) {
            roomCode = savedRoom
        }
    }

    private fun log(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formattedLog = "[$time] $message"
        logs.add(formattedLog)
        if (logs.size > 200) {
            logs.removeAt(0)
        }
        runOnMain { onStateChanged?.invoke() }
    }

    private fun startPingLoop() {
        stopPingLoop()
        mainHandler.postDelayed(pingRunnable, PING_INTERVAL_MS)
    }

    private fun stopPingLoop() {
        mainHandler.removeCallbacks(pingRunnable)
    }

    fun sendPing() {
        val socket = webSocket ?: return
        try {
            val envelope = ProtoWriter().apply {
                writeString(1, "ping")
            }
            socket.send(envelope.toByteArray().toByteString())
        } catch (e: Exception) {
            log("Error al enviar ping: ${e.message}")
        }
    }

    fun createRoom(user: String) {
        isExplicitDisconnect = false
        reconnectAttempts = 0
        username = user
        com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_username", user)
        role = "host"
        connect("create_room", null)
    }

    fun joinRoom(code: String, user: String) {
        isExplicitDisconnect = false
        reconnectAttempts = 0
        username = user
        com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_username", user)
        role = "guest"
        connect("join_room", code)
    }

    fun leaveRoom() {
        log("Saliendo de la sala...")
        isExplicitDisconnect = true
        stopPingLoop()
        webSocket?.close(1000, "User initiated disconnect")
        webSocket = null
        isConnected = false
        roomCode = ""
        role = "none"
        sessionToken = ""
        userId = ""
        hostUsername = ""
        users.clear()

        com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_session_token", "")
        com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_room_code", "")

        runOnMain { onStateChanged?.invoke() }
    }

    fun approveJoin(userId: String) {
        val socket = webSocket ?: return
        try {
            val payload = ProtoWriter().apply {
                writeString(1, userId)
            }
            val envelope = ProtoWriter().apply {
                writeString(1, "approve_join")
                writeBytes(2, payload.toByteArray())
            }
            val data = envelope.toByteArray()
            socket.send(data.toByteString())
            log("Aprobado ingreso de usuario: $userId")
        } catch (e: Exception) {
            log("Error al aprobar ingreso: ${e.message}")
        }
    }

    fun rejectJoin(userId: String, reason: String? = null) {
        val socket = webSocket ?: return
        try {
            val payload = ProtoWriter().apply {
                writeString(1, userId)
                if (reason != null) {
                    writeString(2, reason)
                }
            }
            val envelope = ProtoWriter().apply {
                writeString(1, "reject_join")
                writeBytes(2, payload.toByteArray())
            }
            val data = envelope.toByteArray()
            socket.send(data.toByteString())
            log("Rechazado ingreso de usuario: $userId")
        } catch (e: Exception) {
            log("Error al rechazar ingreso: ${e.message}")
        }
    }

    fun clearLogs() {
        logs.clear()
        runOnMain { onStateChanged?.invoke() }
    }

    private fun decompress(data: ByteArray): ByteArray {
        if (data.size < 2 || data[0] != 0x1f.toByte() || data[1] != 0x8b.toByte()) {
            return data
        }
        return try {
            val bis = java.io.ByteArrayInputStream(data)
            val gis = java.util.zip.GZIPInputStream(bis)
            val bos = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len: Int
            while (gis.read(buffer).also { len = it } > 0) {
                bos.write(buffer, 0, len)
            }
            gis.close()
            bos.toByteArray()
        } catch (e: Exception) {
            log("Error de descompresión GZIP: ${e.message}")
            data
        }
    }

    fun sendPlaybackAction(
        action: String,
        position: Long,
        trackId: String?,
        title: String? = null,
        artist: String? = null,
        artUrl: String? = null
    ) {
        val socket = webSocket ?: return
        if (isSyncing) return // Avoid loop when we are receiving and applying state

        try {
            val payload = ProtoWriter().apply {
                writeString(1, action)
                if (trackId != null) {
                    writeString(2, trackId)
                }
                writeVarint(3, position)

                if (trackId != null && title != null) {
                    val trackInfoWriter = ProtoWriter().apply {
                        writeString(1, trackId)
                        writeString(2, title)
                        writeString(3, artist ?: "")
                        if (artUrl != null) {
                            writeString(6, artUrl)
                        }
                    }
                    writeBytes(4, trackInfoWriter.toByteArray())
                }
            }
            val envelope = ProtoWriter().apply {
                writeString(1, "playback_action")
                writeBytes(2, payload.toByteArray())
            }
            val data = envelope.toByteArray()
            socket.send(data.toByteString())
            log("Enviado: $action a la posición $position ms")
        } catch (e: Exception) {
            log("Error al enviar acción: ${e.message}")
        }
    }

    private fun connect(initialAction: String, roomToJoin: String?) {
        if (!isExplicitDisconnect) {
            stopPingLoop()
            webSocket?.close(1000, "Reconnecting")
            webSocket = null
        }

        val targetUrl = if (serverUrl.isBlank()) ListenTogetherServers.currentServerUrl else serverUrl
        log("Conectando al servidor: $targetUrl...")

        val request = Request.Builder()
            .url(targetUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                reconnectAttempts = 0
                log("Conexión WebSocket establecida con éxito.")
                startPingLoop()

                try {
                    val envelope = ProtoWriter().apply {
                        if (sessionToken.isNotEmpty() && initialAction != "create_room" && initialAction != "join_room") {
                            writeString(1, "reconnect")
                            val payload = ProtoWriter().apply {
                                writeString(1, sessionToken)
                            }
                            writeBytes(2, payload.toByteArray())
                            log("Enviando reconexión con token de sesión...")
                        } else {
                            writeString(1, initialAction)
                            if (initialAction == "create_room") {
                                val payload = ProtoWriter().apply {
                                    writeString(1, username)
                                }
                                writeBytes(2, payload.toByteArray())
                            } else if (initialAction == "join_room" && roomToJoin != null) {
                                val payload = ProtoWriter().apply {
                                    writeString(1, roomToJoin)
                                    writeString(2, username)
                                }
                                writeBytes(2, payload.toByteArray())
                            }
                        }
                    }
                    val data = envelope.toByteArray()
                    webSocket.send(data.toByteString())
                    log("Enviada solicitud inicial: ${if (sessionToken.isNotEmpty()) "reconnect" else initialAction}")
                } catch (e: Exception) {
                    log("Error al enviar solicitud inicial: ${e.message}")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    if (type == "pong") {
                        // Keep-alive pong received
                    }
                } catch (e: Exception) {
                    // Ignore JSON parsing errors
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                try {
                    val data = bytes.toByteArray()
                    val reader = ProtoReader(data)
                    val type = reader.getString(1)
                    var payloadBytes = reader.getBytes(2) ?: byteArrayOf()

                    val compressed = reader.getVarint(3) != 0L
                    if (compressed) {
                        payloadBytes = decompress(payloadBytes)
                    }

                    when (type) {
                        "pong" -> {
                            // Ping ACK received from server
                        }
                        "room_created" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            roomCode = payloadReader.getString(1)
                            userId = payloadReader.getString(2)
                            sessionToken = payloadReader.getString(3)
                            role = "host"
                            hostUsername = username
                            log("Sala creada con éxito. Código de sala: $roomCode")

                            com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_session_token", sessionToken)
                            com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_room_code", roomCode)

                            users.clear()
                            users.add(username)
                            runOnMain { onStateChanged?.invoke() }
                        }
                        "join_approved" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            roomCode = payloadReader.getString(1)
                            userId = payloadReader.getString(2)
                            sessionToken = payloadReader.getString(3)
                            role = "guest"
                            log("Unido con éxito a la sala: $roomCode")

                            com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_session_token", sessionToken)
                            com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_room_code", roomCode)

                            val stateBytes = payloadReader.getBytes(4)
                            if (stateBytes != null) {
                                parseAndApplyRoomState(stateBytes)
                            }
                            runOnMain { onStateChanged?.invoke() }
                        }
                        "reconnected" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            roomCode = payloadReader.getString(1)
                            userId = payloadReader.getString(2)
                            val isHost = payloadReader.getVarint(4) != 0L
                            role = if (isHost) "host" else "guest"
                            log("Reconectado exitosamente a la sala $roomCode como $role")

                            val stateBytes = payloadReader.getBytes(3)
                            if (stateBytes != null) {
                                parseAndApplyRoomState(stateBytes)
                            }
                            runOnMain { onStateChanged?.invoke() }
                        }
                        "join_request" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            val requestUserId = payloadReader.getString(1)
                            val requestUsername = payloadReader.getString(2)
                            log("Solicitud de ingreso recibida de $requestUsername")

                            val autoApprovalEnabled = com.mrtdk.liquid_glass.data.LibraryManager.getString("listen_together_auto_approval", "false") == "true"
                            if (autoApprovalEnabled) {
                                log("Auto-aprobando solicitud de $requestUsername")
                                approveJoin(requestUserId)
                            } else {
                                runOnMain {
                                    onJoinRequestReceived?.invoke(requestUserId, requestUsername)
                                }
                            }
                        }
                        "join_rejected" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            val reason = payloadReader.getString(1)
                            log("Solicitud de ingreso rechazada: $reason")
                            leaveRoom()
                        }
                        "kicked" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            val reason = payloadReader.getString(1)
                            log("Fuiste expulsado de la sala: $reason")
                            leaveRoom()
                        }
                        "sync_playback" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            val action = payloadReader.getString(1)
                            val trackId = payloadReader.getString(2)
                            val pos = payloadReader.getVarint(3)

                            val trackInfoBytes = payloadReader.getBytes(4)
                            var title: String? = null
                            var artist: String? = null
                            var artUrl: String? = null
                            if (trackInfoBytes != null) {
                                val trackInfoReader = ProtoReader(trackInfoBytes)
                                title = trackInfoReader.getString(2)
                                artist = trackInfoReader.getString(3)
                                artUrl = trackInfoReader.getString(6)
                            }

                            log("Recibido sync: $action a la posición $pos ms")
                            runOnMain {
                                onPlaybackActionReceived?.invoke(action, pos, trackId, title, artist, artUrl)
                            }
                        }
                        "user_joined" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            val joinedUser = payloadReader.getString(2)
                            log("Usuario unido: $joinedUser")
                            if (joinedUser.isNotEmpty() && !users.contains(joinedUser)) {
                                users.add(joinedUser)
                            }
                            runOnMain { onStateChanged?.invoke() }
                        }
                        "user_left" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            val leftUser = payloadReader.getString(2)
                            log("Usuario se retiró: $leftUser")
                            if (leftUser.isNotEmpty()) {
                                users.remove(leftUser)
                            }
                            runOnMain { onStateChanged?.invoke() }
                        }
                        "error" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            val errMsg = payloadReader.getString(2)
                            log("Error del servidor: $errMsg")
                        }
                    }
                } catch (e: Exception) {
                    log("Error al procesar mensaje binario: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                log("Cerrando conexión WebSocket: $reason ($code)")
                stopPingLoop()
                isConnected = false
                runOnMain { onStateChanged?.invoke() }
                handleAutoReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                log("Conexión WebSocket cerrada.")
                stopPingLoop()
                isConnected = false
                runOnMain { onStateChanged?.invoke() }
                handleAutoReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                log("Fallo en la conexión WebSocket: ${t.message}")
                stopPingLoop()
                isConnected = false
                runOnMain { onStateChanged?.invoke() }
                handleAutoReconnect()
            }
        })
    }

    private fun parseAndApplyRoomState(stateBytes: ByteArray) {
        val stateReader = ProtoReader(stateBytes)
        val isPlaying = stateReader.getVarint(5) != 0L
        val pos = stateReader.getVarint(6)
        val userInfos = stateReader.getRepeatedMessages(3)
        users.clear()
        var hostName = ""
        userInfos.forEach { userInfo ->
            val name = userInfo.getString(2)
            val isHost = userInfo.getVarint(3) != 0L
            if (name.isNotEmpty()) {
                users.add(name)
                if (isHost) {
                    hostName = name
                }
            }
        }
        hostUsername = hostName
        val currentTrackBytes = stateReader.getBytes(4)
        var trackId = ""
        var title: String? = null
        var artist: String? = null
        var artUrl: String? = null
        if (currentTrackBytes != null) {
            val trackReader = ProtoReader(currentTrackBytes)
            trackId = trackReader.getString(1)
            title = trackReader.getString(2)
            artist = trackReader.getString(3)
            artUrl = trackReader.getString(6)
        }

        log("Estado de sala sincronizado: track=$trackId, reproduciendo=$isPlaying, pos=$pos ms")
        runOnMain {
            onPlaybackActionReceived?.invoke(if (isPlaying) "play" else "pause", pos, trackId, title, artist, artUrl)
        }
    }

    private fun handleAutoReconnect() {
        if (isExplicitDisconnect || sessionToken.isBlank()) return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            log("Límite máximo de reconexiones alcanzado.")
            return
        }

        reconnectAttempts++
        val delayMs = (1000L * reconnectAttempts).coerceAtMost(20000L)
        log("Reconectando automáticamente en ${delayMs / 1000}s (Intento $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)...")
        mainHandler.postDelayed({
            if (!isConnected && !isExplicitDisconnect) {
                connect("reconnect", null)
            }
        }, delayMs)
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}

class ProtoReader(val data: ByteArray) {
    val fields = mutableMapOf<Int, MutableList<ProtoValue>>()

    sealed class ProtoValue {
        class Varint(val value: Long) : ProtoValue()
        class LengthDelimited(val value: ByteArray) : ProtoValue()
    }

    init {
        val buffer = java.nio.ByteBuffer.wrap(data)
        while (buffer.hasRemaining()) {
            val key = readVarint(buffer)
            val tag = (key ushr 3).toInt()
            val wireType = (key and 0x07).toInt()
            when (wireType) {
                0 -> {
                    val value = readVarint(buffer)
                    fields.getOrPut(tag) { mutableListOf() }.add(ProtoValue.Varint(value))
                }
                2 -> {
                    val len = readVarint(buffer).toInt()
                    val bytes = ByteArray(len)
                    buffer.get(bytes)
                    fields.getOrPut(tag) { mutableListOf() }.add(ProtoValue.LengthDelimited(bytes))
                }
                1 -> {
                    if (buffer.remaining() >= 8) buffer.getLong() else break
                }
                5 -> {
                    if (buffer.remaining() >= 4) buffer.getInt() else break
                }
                else -> throw IllegalArgumentException("Unsupported wire type: $wireType")
            }
        }
    }

    private fun readVarint(buffer: java.nio.ByteBuffer): Long {
        var result = 0L
        var shift = 0
        while (shift < 64) {
            val b = buffer.get().toInt()
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        throw IllegalArgumentException("Malformed varint")
    }

    fun getVarint(tag: Int, default: Long = 0L): Long {
        val list = fields[tag] ?: return default
        val last = list.lastOrNull() as? ProtoValue.Varint ?: return default
        return last.value
    }

    fun getString(tag: Int, default: String = ""): String {
        val list = fields[tag] ?: return default
        val last = list.lastOrNull() as? ProtoValue.LengthDelimited ?: return default
        return String(last.value, Charsets.UTF_8)
    }

    fun getBytes(tag: Int): ByteArray? {
        val list = fields[tag] ?: return null
        val last = list.lastOrNull() as? ProtoValue.LengthDelimited ?: return null
        return last.value
    }

    fun getRepeatedMessages(tag: Int): List<ProtoReader> {
        val list = fields[tag] ?: return emptyList()
        return list.mapNotNull {
            (it as? ProtoValue.LengthDelimited)?.let { bytes -> ProtoReader(bytes.value) }
        }
    }
}

class ProtoWriter {
    private val out = java.io.ByteArrayOutputStream()

    fun writeVarint(tag: Int, value: Long) {
        val key = (tag shl 3) or 0
        writeRawVarint(key.toLong())
        writeRawVarint(value)
    }

    fun writeString(tag: Int, value: String) {
        writeBytes(tag, value.toByteArray(Charsets.UTF_8))
    }

    fun writeBytes(tag: Int, value: ByteArray) {
        val key = (tag shl 3) or 2
        writeRawVarint(key.toLong())
        writeRawVarint(value.size.toLong())
        out.write(value)
    }

    fun writeMessage(tag: Int, inner: ProtoWriter) {
        writeBytes(tag, inner.toByteArray())
    }

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun writeRawVarint(value: Long) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0L) {
                out.write(v.toInt())
                break
            } else {
                out.write((v.toInt() and 0x7F) or 0x80)
                v = v ushr 7
            }
        }
    }
}
