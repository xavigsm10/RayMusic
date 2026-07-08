package com.mrtdk.liquid_glass.playback

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
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
            val msg = JSONObject().apply {
                put("type", "playback_action")
                put("payload", JSONObject().apply {
                    put("action", action)
                    put("position", position)
                    put("track_id", trackId ?: "")
                })
            }
            socket.send(msg.toString())
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
                    val payload = JSONObject().apply {
                        put("username", username)
                        if (roomToJoin != null) {
                            put("room_code", roomToJoin)
                        }
                    }
                    val msg = JSONObject().apply {
                        put("type", initialAction)
                        put("payload", payload)
                    }
                    webSocket.send(msg.toString())
                    log("Enviada solicitud inicial: $initialAction")
                } catch (e: Exception) {
                    log("Error al enviar solicitud inicial: ${e.message}")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    val payload = json.optJSONObject("payload") ?: JSONObject()

                    when (type) {
                        "room_created" -> {
                            roomCode = payload.optString("room_code")
                            role = "host"
                            log("Sala creada con éxito. Código de sala: $roomCode")
                            updateUsersList(payload.optJSONArray("users"))
                        }
                        "join_approved" -> {
                            roomCode = payload.optString("room_code")
                            role = "guest"
                            log("Unido con éxito a la sala: $roomCode")
                            updateUsersList(payload.optJSONArray("users"))
                            
                            // Synchronize player with current room state
                            val state = payload.optJSONObject("state") ?: JSONObject()
                            val track = state.optJSONObject("current_track") ?: JSONObject()
                            val isPlaying = state.optBoolean("is_playing", false)
                            val pos = state.optLong("position", 0L)
                            val trackId = track.optString("id", "")
                            
                            log("Sincronización inicial: track=$trackId, reproduciendo=$isPlaying, pos=$pos ms")
                            runOnMain {
                                onPlaybackActionReceived?.invoke(if (isPlaying) "play" else "pause", pos, trackId)
                            }
                        }
                        "sync_playback" -> {
                            val action = payload.optString("action")
                            val pos = payload.optLong("position", 0L)
                            val trackId = payload.optString("track_id")
                            
                            log("Recibido sync: $action a la posición $pos ms")
                            runOnMain {
                                onPlaybackActionReceived?.invoke(action, pos, trackId)
                            }
                        }
                        "user_joined" -> {
                            val joinedUser = payload.optString("username")
                            log("Usuario unido: $joinedUser")
                            if (!users.contains(joinedUser)) {
                                users.add(joinedUser)
                            }
                            runOnMain { onStateChanged?.invoke() }
                        }
                        "user_left" -> {
                            val leftUser = payload.optString("username")
                            log("Usuario se retiró: $leftUser")
                            users.remove(leftUser)
                            runOnMain { onStateChanged?.invoke() }
                        }
                        "error" -> {
                            val errMsg = payload.optString("message")
                            log("Error del servidor: $errMsg")
                        }
                    }
                } catch (e: Exception) {
                    log("Error al procesar mensaje JSON: ${e.message}")
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
        users.clear()
        if (usersJson != null) {
            for (i in 0 until usersJson.length()) {
                val userObj = usersJson.optJSONObject(i)
                if (userObj != null) {
                    val name = userObj.optString("username")
                    if (name.isNotEmpty()) {
                        users.add(name)
                    }
                }
            }
        }
        runOnMain { onStateChanged?.invoke() }
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}
