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
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    
    var isConnected = false
        private set
        
    var roomCode = ""
        private set
        
    var role = "none" // "host", "guest", "none"
        private set
        
    var username = ""

    var serverUrl = "wss://iad1tya-echomusic.hf.space/ws"
        set(value) {
            field = value
            com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_server_url", value)
        }

    val users = mutableListOf<String>()
    val logs = mutableListOf<String>()

    // Event listeners
    var onStateChanged: (() -> Unit)? = null
    var onPlaybackActionReceived: ((action: String, position: Long?, trackId: String?) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var isSyncing = false

    init {
        // Load configurations on initialization
        val savedServer = com.mrtdk.liquid_glass.data.LibraryManager.getString("listen_together_server_url", "")
        if (!savedServer.isNullOrBlank()) {
            serverUrl = savedServer
        }
        val savedUser = com.mrtdk.liquid_glass.data.LibraryManager.getString("listen_together_username", "")
        if (!savedUser.isNullOrBlank()) {
            username = savedUser
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

    fun createRoom(user: String) {
        username = user
        com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_username", user)
        role = "host"
        connect("create_room", null)
    }

    fun joinRoom(code: String, user: String) {
        username = user
        com.mrtdk.liquid_glass.data.LibraryManager.saveString("listen_together_username", user)
        role = "guest"
        connect("join_room", code)
    }

    fun leaveRoom() {
        log("Saliendo de la sala...")
        webSocket?.close(1000, "User initiated disconnect")
        webSocket = null
        isConnected = false
        roomCode = ""
        role = "none"
        users.clear()
        runOnMain { onStateChanged?.invoke() }
    }

    fun clearLogs() {
        logs.clear()
        runOnMain { onStateChanged?.invoke() }
    }

    fun sendPlaybackAction(action: String, position: Long, trackId: String?) {
        val socket = webSocket ?: return
        if (isSyncing) return // Avoid loop when we are receiving and applying state

        try {
            val payload = ProtoWriter().apply {
                writeString(1, action)
                if (trackId != null) {
                    writeString(2, trackId)
                }
                writeVarint(3, position)
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
        leaveRoom() // Close existing first
        
        log("Conectando al servidor: $serverUrl...")
        
        val request = Request.Builder()
            .url(serverUrl)
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                log("Conexión WebSocket establecida con éxito.")
                
                try {
                    val envelope = ProtoWriter().apply {
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
                    val data = envelope.toByteArray()
                    webSocket.send(data.toByteString())
                    log("Enviada solicitud inicial: $initialAction")
                } catch (e: Exception) {
                    log("Error al enviar solicitud inicial: ${e.message}")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // El servidor remoto Go solo envía mensajes binarios (Protobuf),
                // pero dejamos esto por compatibilidad.
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                try {
                    val data = bytes.toByteArray()
                    val reader = ProtoReader(data)
                    val type = reader.getString(1)
                    val payloadBytes = reader.getBytes(2) ?: byteArrayOf()

                    when (type) {
                        "room_created" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            roomCode = payloadReader.getString(1)
                            role = "host"
                            log("Sala creada con éxito. Código de sala: $roomCode")
                            users.clear()
                            users.add(username)
                            runOnMain { onStateChanged?.invoke() }
                        }
                        "join_approved" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            roomCode = payloadReader.getString(1)
                            role = "guest"
                            log("Unido con éxito a la sala: $roomCode")
                            
                            val stateBytes = payloadReader.getBytes(4)
                            if (stateBytes != null) {
                                val stateReader = ProtoReader(stateBytes)
                                val isPlaying = stateReader.getVarint(5) != 0L
                                val pos = stateReader.getVarint(6)
                                val userInfos = stateReader.getRepeatedMessages(3)
                                users.clear()
                                userInfos.forEach { userInfo ->
                                    val name = userInfo.getString(2)
                                    if (name.isNotEmpty()) {
                                        users.add(name)
                                    }
                                }
                                val currentTrackBytes = stateReader.getBytes(4)
                                val trackId = if (currentTrackBytes != null) {
                                    ProtoReader(currentTrackBytes).getString(1)
                                } else ""
                                
                                log("Sincronización inicial: track=$trackId, reproduciendo=$isPlaying, pos=$pos ms")
                                runOnMain {
                                    onPlaybackActionReceived?.invoke(if (isPlaying) "play" else "pause", pos, trackId)
                                }
                            }
                        }
                        "sync_playback" -> {
                            val payloadReader = ProtoReader(payloadBytes)
                            val action = payloadReader.getString(1)
                            val trackId = payloadReader.getString(2)
                            val pos = payloadReader.getVarint(3)
                            
                            log("Recibido sync: $action a la posición $pos ms")
                            runOnMain {
                                onPlaybackActionReceived?.invoke(action, pos, trackId)
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
                isConnected = false
                roomCode = ""
                role = "none"
                users.clear()
                runOnMain { onStateChanged?.invoke() }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                log("Conexión WebSocket cerrada.")
                isConnected = false
                roomCode = ""
                role = "none"
                users.clear()
                runOnMain { onStateChanged?.invoke() }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                log("Fallo en la conexión WebSocket: ${t.message}")
                isConnected = false
                roomCode = ""
                role = "none"
                users.clear()
                runOnMain { onStateChanged?.invoke() }
            }
        })
    }

    private fun updateUsersList(usersJson: org.json.JSONArray?) {
        // Obsoleto, la lista de usuarios se actualiza dinámicamente mediante Protobuf
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
