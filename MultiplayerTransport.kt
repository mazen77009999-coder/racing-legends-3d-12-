package com.example.multiplayer

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class MultiplayerConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class MultiplayerBackendMode {
    LOCAL_RELAY,
    WEBSOCKET_SERVER
}

interface MultiplayerTransport {
    val connectionState: StateFlow<MultiplayerConnectionState>
    val lobbyState: StateFlow<LobbyRoomState?>
    val latencyMs: StateFlow<Int>
    val errorMessage: StateFlow<String?>
    val raceStartEvents: SharedFlow<LobbyRoomState>

    fun connect(
        serverUrl: String,
        roomCode: String,
        driverName: String,
        carId: String,
        paintHex: String,
        trackId: String
    )

    fun setTrack(trackId: String)
    fun setReady(ready: Boolean)
    fun requestStartMatch()
    fun sendTelemetry(packet: TelemetryPacket)
    fun disconnect()
}
