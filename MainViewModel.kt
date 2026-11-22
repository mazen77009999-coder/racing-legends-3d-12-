package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CarCustomizationEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.TrackRecordEntity
import com.example.data.model.CameraMode
import com.example.data.model.GameCatalog
import com.example.data.model.GraphicsQuality
import com.example.data.model.RaceLaunchConfig
import com.example.data.model.RaceMode
import com.example.data.repository.RacingRepository
import com.example.data.repository.UpgradeType
import com.example.multiplayer.LobbyRoomState
import com.example.multiplayer.LocalRelayMultiplayerTransport
import com.example.multiplayer.MultiplayerBackendMode
import com.example.multiplayer.MultiplayerConnectionState
import com.example.multiplayer.MultiplayerTransport
import com.example.multiplayer.TelemetryPacket
import com.example.multiplayer.WebSocketMultiplayerTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val profile: PlayerProfileEntity = PlayerProfileEntity(),
    val cars: List<CarCustomizationEntity> = emptyList(),
    val trackRecords: Map<String, TrackRecordEntity> = emptyMap(),
    val selectedGarageCarId: String = "apex_v8",
    val feedbackMessage: String? = null
) {
    val activeCarCustomization: CarCustomizationEntity?
        get() = cars.firstOrNull { it.carId == profile.activeCarId } ?: cars.firstOrNull()

    val selectedGarageCar: CarCustomizationEntity?
        get() = cars.firstOrNull { it.carId == selectedGarageCarId } ?: activeCarCustomization

    val graphicsQuality: GraphicsQuality
        get() = runCatching { GraphicsQuality.valueOf(profile.graphicsQuality) }
            .getOrDefault(GraphicsQuality.HIGH)

    val defaultCamera: CameraMode
        get() = runCatching { CameraMode.valueOf(profile.defaultCamera) }
            .getOrDefault(CameraMode.CHASE)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RacingRepository(
        AppDatabase.getInstance(application).racingDao()
    )

    private val _selectedGarageCarId = MutableStateFlow("apex_v8")
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    private val _launchConfig = MutableStateFlow(
        RaceLaunchConfig(trackId = "silverpeak_gp", laps = 3, mode = RaceMode.QUICK_RACE)
    )
    val launchConfig: StateFlow<RaceLaunchConfig> = _launchConfig.asStateFlow()

    val uiState: StateFlow<MainUiState> = combine(
        repository.profileFlow,
        repository.carsFlow,
        repository.trackRecordsFlow,
        _selectedGarageCarId,
        _feedbackMessage
    ) { profile, cars, records, garageCarId, feedback ->
        MainUiState(
            profile = profile,
            cars = cars,
            trackRecords = records.associateBy { it.trackId },
            selectedGarageCarId = garageCarId,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    // Multiplayer backend manager
    private val localTransport = LocalRelayMultiplayerTransport()
    private val wsTransport = WebSocketMultiplayerTransport()

    private val _backendMode = MutableStateFlow(MultiplayerBackendMode.LOCAL_RELAY)
    val backendMode: StateFlow<MultiplayerBackendMode> = _backendMode.asStateFlow()

    private val _activeTransport = MutableStateFlow<MultiplayerTransport>(localTransport)

    private val _mpConnectionState = MutableStateFlow(MultiplayerConnectionState.DISCONNECTED)
    val mpConnectionState: StateFlow<MultiplayerConnectionState> = _mpConnectionState.asStateFlow()

    private val _mpLobbyState = MutableStateFlow<LobbyRoomState?>(null)
    val mpLobbyState: StateFlow<LobbyRoomState?> = _mpLobbyState.asStateFlow()

    private val _mpLatencyMs = MutableStateFlow(0)
    val mpLatencyMs: StateFlow<Int> = _mpLatencyMs.asStateFlow()

    private val _mpError = MutableStateFlow<String?>(null)
    val mpError: StateFlow<String?> = _mpError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            repository.profileFlow.collect { profile ->
                if (_selectedGarageCarId.value.isBlank()) {
                    _selectedGarageCarId.value = profile.activeCarId
                }
            }
        }
        observeTransport(localTransport, MultiplayerBackendMode.LOCAL_RELAY)
        observeTransport(wsTransport, MultiplayerBackendMode.WEBSOCKET_SERVER)
    }

    private fun observeTransport(transport: MultiplayerTransport, mode: MultiplayerBackendMode) {
        viewModelScope.launch {
            transport.connectionState.collect { state ->
                if (_backendMode.value == mode) _mpConnectionState.value = state
            }
        }
        viewModelScope.launch {
            transport.lobbyState.collect { lobby ->
                if (_backendMode.value == mode) _mpLobbyState.value = lobby
            }
        }
        viewModelScope.launch {
            transport.latencyMs.collect { ping ->
                if (_backendMode.value == mode) _mpLatencyMs.value = ping
            }
        }
        viewModelScope.launch {
            transport.errorMessage.collect { err ->
                if (_backendMode.value == mode) _mpError.value = err
            }
        }
    }

    fun selectGarageCar(carId: String) {
        _selectedGarageCarId.value = carId
        viewModelScope.launch {
            val car = uiState.value.cars.firstOrNull { it.carId == carId }
            if (car?.isUnlocked == true) {
                repository.selectActiveCar(carId)
            }
        }
    }

    fun purchaseOrEquipCar(carId: String) {
        viewModelScope.launch {
            val car = uiState.value.cars.firstOrNull { it.carId == carId } ?: return@launch
            if (car.isUnlocked) {
                repository.selectActiveCar(carId)
                _feedbackMessage.value = "${GameCatalog.carById(carId).name} selected for race!"
            } else {
                val success = repository.unlockCar(carId)
                _feedbackMessage.value = if (success) {
                    "${GameCatalog.carById(carId).name} unlocked & equipped!"
                } else {
                    "Not enough credits to unlock ${GameCatalog.carById(carId).name}."
                }
            }
        }
    }

    fun updatePaint(carId: String, hex: String) {
        viewModelScope.launch {
            repository.updateCarVisuals(carId = carId, paintHex = hex)
        }
    }

    fun updateRims(carId: String, rimStyle: String) {
        viewModelScope.launch {
            repository.updateCarVisuals(carId = carId, rimStyle = rimStyle)
        }
    }

    fun updateSpoiler(carId: String, spoilerStyle: String) {
        viewModelScope.launch {
            repository.updateCarVisuals(carId = carId, spoilerStyle = spoilerStyle)
        }
    }

    fun upgradeCar(carId: String, type: UpgradeType) {
        viewModelScope.launch {
            val ok = repository.upgradeCar(carId, type)
            _feedbackMessage.value = if (ok) {
                "${type.label} upgraded!"
            } else {
                "Need more credits or max level reached."
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun updateSettings(
        driverName: String? = null,
        quality: GraphicsQuality? = null,
        audioEnabled: Boolean? = null,
        masterVolume: Float? = null,
        defaultCamera: CameraMode? = null
    ) {
        viewModelScope.launch {
            repository.updateSettings(driverName, quality, audioEnabled, masterVolume, defaultCamera)
        }
    }

    fun prepareQuickRace(trackId: String, laps: Int) {
        _launchConfig.value = RaceLaunchConfig(
            trackId = trackId,
            laps = laps,
            mode = RaceMode.QUICK_RACE
        )
    }

    fun prepareChampionshipRound(roundNumber: Int) {
        val round = GameCatalog.championshipRounds.firstOrNull { it.roundNumber == roundNumber }
            ?: GameCatalog.championshipRounds.first()
        _launchConfig.value = RaceLaunchConfig(
            trackId = round.trackId,
            laps = round.laps,
            mode = RaceMode.CHAMPIONSHIP,
            championshipRound = round.roundNumber
        )
    }

    fun resetChampionship() {
        viewModelScope.launch {
            repository.resetChampionship()
        }
    }

    // Multiplayer actions
    fun setMultiplayerBackendMode(mode: MultiplayerBackendMode) {
        if (_backendMode.value == mode) return
        _activeTransport.value.disconnect()
        _backendMode.value = mode
        _activeTransport.value = when (mode) {
            MultiplayerBackendMode.LOCAL_RELAY -> localTransport
            MultiplayerBackendMode.WEBSOCKET_SERVER -> wsTransport
        }
        _mpConnectionState.value = _activeTransport.value.connectionState.value
        _mpLobbyState.value = _activeTransport.value.lobbyState.value
        _mpError.value = null
    }

    fun connectMultiplayer(serverUrl: String, roomCode: String, trackId: String) {
        val state = uiState.value
        val activeCar = state.activeCarCustomization
        _activeTransport.value.connect(
            serverUrl = serverUrl,
            roomCode = roomCode,
            driverName = state.profile.driverName,
            carId = state.profile.activeCarId,
            paintHex = activeCar?.paintHex ?: "#E11D48",
            trackId = trackId
        )
    }

    fun setMultiplayerTrack(trackId: String) {
        _activeTransport.value.setTrack(trackId)
    }

    fun setMultiplayerReady(ready: Boolean) {
        _activeTransport.value.setReady(ready)
    }

    fun launchMultiplayerMatch(): RaceLaunchConfig {
        _activeTransport.value.requestStartMatch()
        val lobby = _mpLobbyState.value
        val config = RaceLaunchConfig(
            trackId = lobby?.trackId ?: "silverpeak_gp",
            laps = lobby?.laps ?: 3,
            mode = RaceMode.MULTIPLAYER,
            isMultiplayer = true
        )
        _launchConfig.value = config
        return config
    }

    fun sendMultiplayerTelemetry(speedKmh: Int, lap: Int, progressU: Float) {
        if (_launchConfig.value.isMultiplayer) {
            _activeTransport.value.sendTelemetry(
                TelemetryPacket(
                    playerId = "local_host",
                    speedKmh = speedKmh,
                    lap = lap,
                    progressU = progressU,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun disconnectMultiplayer() {
        _activeTransport.value.disconnect()
    }

    suspend fun recordRaceResult(
        trackId: String,
        position: Int,
        bestLapMs: Long,
        totalTimeMs: Long,
        mode: RaceMode,
        championshipRound: Int?
    ): Int {
        return repository.recordRaceCompletion(
            trackId = trackId,
            position = position,
            bestLapMs = bestLapMs,
            totalTimeMs = totalTimeMs,
            mode = mode,
            championshipRound = championshipRound
        )
    }

    override fun onCleared() {
        super.onCleared()
        localTransport.disconnect()
        wsTransport.disconnect()
    }
}
