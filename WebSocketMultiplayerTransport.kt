package com.example.multiplayer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketMultiplayerTransport : MultiplayerTransport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(8, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var localPlayerId: String = "driver_${System.currentTimeMillis() % 10000}"

    private val _connectionState = MutableStateFlow(MultiplayerConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<MultiplayerConnectionState> = _connectionState.asStateFlow()

    private val _lobbyState = MutableStateFlow<LobbyRoomState?>(null)
    override val lobbyState: StateFlow<LobbyRoomState?> = _lobbyState.asStateFlow()

    private val _latencyMs = MutableStateFlow(0)
    override val latencyMs: StateFlow<Int> = _latencyMs.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _raceStartEvents = MutableSharedFlow<LobbyRoomState>(extraBufferCapacity = 4)
    override val raceStartEvents: SharedFlow<LobbyRoomState> = _raceStartEvents.asSharedFlow()

    override fun connect(
        serverUrl: String,
        roomCode: String,
        driverName: String,
        carId: String,
        paintHex: String,
        trackId: String
    ) {
        disconnect()
        _errorMessage.value = null
        _connectionState.value = MultiplayerConnectionState.CONNECTING

        val request = try {
            Request.Builder().url(serverUrl.trim()).build()
        } catch (e: IllegalArgumentException) {
            _connectionState.value = MultiplayerConnectionState.ERROR
            _errorMessage.value = "Invalid WebSocket URL. Use ws:// or wss://."
            return
        }

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = MultiplayerConnectionState.CONNECTED
                val joinMsg = WireEnvelope(
                    type = "JOIN_ROOM",
                    roomCode = roomCode,
                    playerId = localPlayerId,
                    driverName = driverName,
                    carId = carId,
                    paintHex = paintHex,
                    trackId = trackId
                )
                webSocket.send(MultiplayerCodec.encode(joinMsg))
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val env = MultiplayerCodec.decode(text) ?: return
                when (env.type) {
                    "LOBBY_STATE" -> {
                        env.lobby?.let { _lobbyState.value = it }
                    }
                    "RACE_START" -> {
                        env.lobby?.let {
                            _lobbyState.value = it
                            _raceStartEvents.tryEmit(it)
                        }
                    }
                    "PONG" -> {
                        val sent = env.sentAtMs ?: 0L
                        if (sent > 0L) {
                            _latencyMs.value = (System.currentTimeMillis() - sent).toInt().coerceAtLeast(1)
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = MultiplayerConnectionState.ERROR
                _errorMessage.value = t.message ?: "Unable to reach multiplayer relay server."
                heartbeatJob?.cancel()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = MultiplayerConnectionState.DISCONNECTED
                heartbeatJob?.cancel()
            }
        })
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _connectionState.value == MultiplayerConnectionState.CONNECTED) {
                val ping = WireEnvelope(
                    type = "PING",
                    playerId = localPlayerId,
                    sentAtMs = System.currentTimeMillis()
                )
                webSocket?.send(MultiplayerCodec.encode(ping))
                delay(2500)
            }
        }
    }

    override fun setTrack(trackId: String) {
        webSocket?.send(
            MultiplayerCodec.encode(
                WireEnvelope(type = "SET_TRACK", playerId = localPlayerId, trackId = trackId)
            )
        )
    }

    override fun setReady(ready: Boolean) {
        webSocket?.send(
            MultiplayerCodec.encode(
                WireEnvelope(type = "SET_READY", playerId = localPlayerId, ready = ready)
            )
        )
    }

    override fun requestStartMatch() {
        webSocket?.send(
            MultiplayerCodec.encode(
                WireEnvelope(type = "START_MATCH", playerId = localPlayerId)
            )
        )
    }

    override fun sendTelemetry(packet: TelemetryPacket) {
        webSocket?.send(
            MultiplayerCodec.encode(
                WireEnvelope(type = "TELEMETRY", playerId = localPlayerId, telemetry = packet)
            )
        )
    }

    override fun disconnect() {
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
        _lobbyState.value = null
        _connectionState.value = MultiplayerConnectionState.DISCONNECTED
    }
}
