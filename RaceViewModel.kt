package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.model.CameraMode
import com.example.data.model.RaceStanding
import com.example.data.model.TelemetryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class RaceViewModel : ViewModel() {

    private val _telemetry = MutableStateFlow(TelemetryState())
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _lapBannerText = MutableStateFlow<String?>(null)
    val lapBannerText: StateFlow<String?> = _lapBannerText.asStateFlow()

    private val _earnedCredits = MutableStateFlow<Int?>(null)
    val earnedCredits: StateFlow<Int?> = _earnedCredits.asStateFlow()

    var resultRecorded = false
        private set

    fun resetForNewRace(totalLaps: Int, initialCamera: CameraMode) {
        resultRecorded = false
        _isPaused.value = false
        _lapBannerText.value = null
        _earnedCredits.value = null
        _telemetry.value = TelemetryState(
            totalLaps = totalLaps,
            cameraMode = initialCamera
        )
    }

    fun setPaused(paused: Boolean) {
        _isPaused.value = paused
    }

    fun markResultRecorded(creditsEarned: Int) {
        resultRecorded = true
        _earnedCredits.value = creditsEarned
    }

    fun onJsTelemetry(json: String) {
        runCatching {
            val obj = JSONObject(json)
            val standingsArray = obj.optJSONArray("standings")
            val standings = mutableListOf<RaceStanding>()
            if (standingsArray != null) {
                for (i in 0 until standingsArray.length()) {
                    val item = standingsArray.getJSONObject(i)
                    standings.add(
                        RaceStanding(
                            id = item.optString("id"),
                            name = item.optString("name"),
                            carId = item.optString("carId"),
                            isPlayer = item.optBoolean("isPlayer"),
                            position = item.optInt("position"),
                            bestLapMs = item.optLong("bestLapMs"),
                            finishTimeMs = item.optLong("finishTimeMs"),
                            progressU = item.optDouble("u", 0.0).toFloat()
                        )
                    )
                }
            }
            val camName = obj.optString("cameraMode", "CHASE")
            val camMode = runCatching { CameraMode.valueOf(camName) }.getOrDefault(CameraMode.CHASE)

            _telemetry.value = TelemetryState(
                speedKmh = obj.optInt("speedKmh", 0),
                rpm = obj.optInt("rpm", 1000),
                rpmNorm = obj.optDouble("rpmNorm", 0.1).toFloat(),
                maxRpm = obj.optInt("maxRpm", 8200),
                gear = obj.optString("gear", "1"),
                lap = obj.optInt("lap", 1),
                totalLaps = obj.optInt("totalLaps", 3),
                position = obj.optInt("position", 4),
                totalRacers = obj.optInt("totalRacers", 4),
                lapTimeMs = obj.optLong("lapTimeMs", 0L),
                bestLapMs = obj.optLong("bestLapMs", 0L),
                elapsedMs = obj.optLong("elapsedMs", 0L),
                countdownStep = obj.optInt("countdownStep", -1),
                cameraMode = camMode,
                isDrifting = obj.optBoolean("isDrifting", false),
                fps = obj.optInt("fps", 60),
                finished = obj.optBoolean("finished", false),
                standings = standings
            )
        }
    }

    fun onJsEvent(json: String) {
        runCatching {
            val obj = JSONObject(json)
            when (obj.optString("type")) {
                "lap_complete" -> {
                    val lapNum = obj.optInt("lap", 1)
                    val lapMs = obj.optLong("lapTimeMs", 0L)
                    _lapBannerText.value = "LAP $lapNum  •  ${formatTimeMs(lapMs)}"
                }
            }
        }
    }

    fun clearLapBanner() {
        _lapBannerText.value = null
    }

    companion object {
        fun formatTimeMs(ms: Long): String {
            if (ms <= 0L) return "--:--.--"
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hundredths = (ms % 1000) / 10
            return "%d:%02d.%02d".format(minutes, seconds, hundredths)
        }
    }
}
