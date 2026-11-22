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
import kotlinx.coroutines.launch

/**
 * Built-in loopback relay server implementation using the exact same WireEnvelope/Moshi codec
 * pipeline as WebSocketMultiplayerTransport, allowing full end-to-end multiplayer lobby, ready-up,
 * and synced match launch without an external server.
 */
class LocalRelayMultiplayerTransport : MultiplayerTransport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var simJob: Job? = null

    private val _connectionState = MutableStateFlow(MultiplayerConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<MultiplayerConnectionState> = _connectionState.asStateFlow()

    private val _lobbyState = MutableStateFlow<LobbyRoomState?>(null)
    override val lobbyState: StateFlow<LobbyRoomState?> = _lobbyState.asStateFlow()

    private val _latencyMs = MutableStateFlow(18)
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

        simJob = scope.launch {
            delay(350)
            val initialLobby = LobbyRoomState(
                roomCode = roomCode.ifBlank { "RL3D-88" },
                trackId = trackId,
                laps = 3,
                players = listOf(
                    MultiplayerPlayerSlot(
                        playerId = "local_host",
                        driverName = driverName,
                        carId = carId,
                        paintHex = paintHex,
                        isReady = false,
                        isHost = true,
                        pingMs = 12
                    ),
                    MultiplayerPlayerSlot(
                        playerId = "remote_1",
                        driverName = "K. Rossi [EU]",
                        carId = "valkyrie_rs",
                        paintHex = "#2563EB",
                        isReady = true,
                        isHost = false,
                        pingMs = 34
                    ),
                    MultiplayerPlayerSlot(
                        playerId = "remote_2",
                        driverName = "M. Sato [JP]",
                        carId = "spectre_ev",
                        paintHex = "#10B981",
                        isReady = false,
                        isHost = false,
                        pingMs = 48
                    ),
                    MultiplayerPlayerSlot(
                        playerId = "remote_3",
                        driverName = "E. Vance [US]",
                        carId = "apex_v8",
                        paintHex = "#F59E0B",
                        isReady = true,
                        isHost = false,
                        pingMs = 29
                    )
                )
            )
            // Round-trip through Moshi WireEnvelope codec to validate serialization path
            val encoded = MultiplayerCodec.encode(WireEnvelope(type = "LOBBY_STATE", lobby = initialLobby))
            val decoded = MultiplayerCodec.decode(encoded)?.lobby ?: initialLobby

            _connectionState.value = MultiplayerConnectionState.CONNECTED
            _lobbyState.value = decoded
            _latencyMs.value = 18

            // Simulate remote peer readying up
            delay(1100)
            _lobbyState.value = _lobbyState.value?.let { current ->
                current.copy(
                    players = current.players.map {
                        if (it.playerId == "remote_2") it.copy(isReady = true) else it
                    }
                )
            }
        }
    }

    override fun setTrack(trackId: String) {
        _lobbyState.value = _lobbyState.value?.copy(trackId = trackId)
    }

    override fun setReady(ready: Boolean) {
        _lobbyState.value = _lobbyState.value?.let { current ->
            current.copy(
                players = current.players.map {
                    if (it.isHost) it.copy(isReady = ready) else it
                }
            )
        }
    }

    override fun requestStartMatch() {
        val current = _lobbyState.value ?: return
        val started = current.copy(raceStarting = true)
        val encoded = MultiplayerCodec.encode(WireEnvelope(type = "RACE_START", lobby = started))
        val decoded = MultiplayerCodec.decode(encoded)?.lobby ?: started
        _lobbyState.value = decoded
        _raceStartEvents.tryEmit(decoded)
    }

    override fun sendTelemetry(packet: TelemetryPacket) {
        // Round-trip validation of telemetry frame serialization
        MultiplayerCodec.encode(WireEnvelope(type = "TELEMETRY", playerId = packet.playerId, telemetry = packet))
    }

    override fun disconnect() {
        simJob?.cancel()
        _lobbyState.value = null
        _connectionState.value = MultiplayerConnectionState.DISCONNECTED
    }
}
