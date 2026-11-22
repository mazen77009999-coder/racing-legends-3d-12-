package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.GameCatalog
import com.example.data.model.RaceLaunchConfig
import com.example.data.model.RaceMode
import com.example.ui.components.MotorsportPanel
import com.example.ui.components.RacingWebController
import com.example.ui.components.RacingWebView
import com.example.ui.components.TelemetryHud
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonObsidian
import com.example.ui.theme.CarbonSurfaceElevated
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.RaceViewModel
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun RaceScreen(
    uiState: MainUiState,
    launchConfig: RaceLaunchConfig,
    onRecordResult: suspend (trackId: String, position: Int, bestLapMs: Long, totalTimeMs: Long, mode: RaceMode, champRound: Int?) -> Int,
    onSendMultiplayerTelemetry: (speedKmh: Int, lap: Int, progressU: Float) -> Unit,
    onNextChampionshipRound: (Int) -> Unit,
    onExitToMenu: () -> Unit,
    raceViewModel: RaceViewModel = viewModel()
) {
    val telemetry by raceViewModel.telemetry.collectAsStateWithLifecycle()
    val isPaused by raceViewModel.isPaused.collectAsStateWithLifecycle()
    val lapBannerText by raceViewModel.lapBannerText.collectAsStateWithLifecycle()
    val earnedCredits by raceViewModel.earnedCredits.collectAsStateWithLifecycle()

    val webController = androidx.compose.runtime.remember { RacingWebController() }
    val track = GameCatalog.trackById(launchConfig.trackId)

    fun startRaceInEngine() {
        raceViewModel.resetForNewRace(launchConfig.laps, uiState.defaultCamera)
        val activeCustom = uiState.activeCarCustomization
        val carSpec = GameCatalog.carById(uiState.profile.activeCarId)

        val playerJson = JSONObject().apply {
            put("name", uiState.profile.driverName)
            put("carId", carSpec.id)
            put("paintHex", activeCustom?.paintHex ?: carSpec.defaultPaintHex)
            put("rimStyle", activeCustom?.rimStyle ?: "FORGED_5")
            put("spoilerStyle", activeCustom?.spoilerStyle ?: "GT_WING")
            put("upgrades", JSONObject().apply {
                put("engine", activeCustom?.engineLevel ?: 0)
                put("turbo", activeCustom?.turboLevel ?: 0)
                put("grip", activeCustom?.gripLevel ?: 0)
                put("brake", activeCustom?.brakeLevel ?: 0)
            })
        }

        val opponentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "ai_1")
                put("name", "K. Rossi")
                put("carId", "valkyrie_rs")
                put("paintHex", "#2563EB")
                put("rimStyle", "FORGED_5")
                put("spoilerStyle", "GT_WING")
                put("aiSkill", 0.94)
            })
            put(JSONObject().apply {
                put("id", "ai_2")
                put("name", "M. Sato")
                put("carId", "spectre_ev")
                put("paintHex", "#10B981")
                put("rimStyle", "TURBINE")
                put("spoilerStyle", "ENDURANCE")
                put("aiSkill", 0.91)
            })
            put(JSONObject().apply {
                put("id", "ai_3")
                put("name", "E. Vance")
                put("carId", "apex_v8")
                put("paintHex", "#F59E0B")
                put("rimStyle", "SPLIT_MESH")
                put("spoilerStyle", "LIP")
                put("aiSkill", 0.87)
            })
        }

        val raceConfigJson = JSONObject().apply {
            put("trackId", launchConfig.trackId)
            put("laps", launchConfig.laps)
            put("quality", uiState.graphicsQuality.name)
            put("cameraMode", uiState.defaultCamera.name)
            put("player", playerJson)
            put("opponents", opponentsArray)
        }

        webController.setAudio(uiState.profile.audioEnabled, uiState.profile.masterVolume)
        webController.startRace(raceConfigJson.toString())
    }

    LaunchedEffect(launchConfig) {
        startRaceInEngine()
    }

    // Auto-dismiss lap completion banner
    LaunchedEffect(lapBannerText) {
        if (lapBannerText != null) {
            delay(2600)
            raceViewModel.clearLapBanner()
        }
    }

    // Persist race finish once & transmit multiplayer telemetry
    LaunchedEffect(telemetry.finished, telemetry.elapsedMs) {
        if (launchConfig.isMultiplayer && !telemetry.finished) {
            val playerStanding = telemetry.standings.firstOrNull { it.isPlayer }
            onSendMultiplayerTelemetry(telemetry.speedKmh, telemetry.lap, playerStanding?.progressU ?: 0f)
        }
        if (telemetry.finished && !raceViewModel.resultRecorded) {
            val reward = onRecordResult(
                launchConfig.trackId,
                telemetry.position,
                telemetry.bestLapMs,
                telemetry.elapsedMs,
                launchConfig.mode,
                launchConfig.championshipRound
            )
            raceViewModel.markResultRecorded(reward)
        }
    }

    BackHandler {
        if (!telemetry.finished) {
            val nextPaused = !isPaused
            raceViewModel.setPaused(nextPaused)
            webController.setPaused(nextPaused)
        } else {
            onExitToMenu()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RacingWebView(
            controller = webController,
            keepScreenOn = !isPaused && !telemetry.finished,
            onTelemetry = { json -> raceViewModel.onJsTelemetry(json) },
            onEvent = { json -> raceViewModel.onJsEvent(json) },
            modifier = Modifier.fillMaxSize()
        )

        if (!telemetry.finished) {
            TelemetryHud(
                telemetry = telemetry,
                track = track,
                lapBannerText = lapBannerText,
                onInputChanged = { t, b, l, r -> webController.setInput(t, b, l, r) },
                onCycleCamera = { webController.cycleCamera() },
                onPauseClick = {
                    raceViewModel.setPaused(true)
                    webController.setPaused(true)
                }
            )
        }

        // PAUSE OVERLAY
        if (isPaused && !telemetry.finished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CarbonObsidian.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                MotorsportPanel(
                    borderColor = ElectricCyan.copy(alpha = 0.7f),
                    modifier = Modifier.width(360.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("RACE PAUSED", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text(track.name, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                raceViewModel.setPaused(false)
                                webController.setPaused(false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TelemetryGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("RESUME RACE", color = CarbonObsidian, style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = {
                                raceViewModel.setPaused(false)
                                startRaceInEngine()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("RESTART RACE", color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = {
                                webController.setPaused(true)
                                onExitToMenu()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("EXIT TO PADDOCK", color = RacingCrimson)
                        }
                    }
                }
            }
        }

        // RACE RESULTS & PODIUM OVERLAY
        if (telemetry.finished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CarbonObsidian.copy(alpha = 0.86f)),
                contentAlignment = Alignment.Center
            ) {
                MotorsportPanel(
                    borderColor = if (telemetry.position == 1) ChampionshipGold else ElectricCyan,
                    modifier = Modifier
                        .width(560.dp)
                        .testTag("race_results_panel")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when (telemetry.position) {
                                        1 -> "1ST PLACE — VICTORY!"
                                        2 -> "2ND PLACE — PODIUM FINISH"
                                        3 -> "3RD PLACE — PODIUM FINISH"
                                        else -> "4TH PLACE — RACE COMPLETE"
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (telemetry.position == 1) ChampionshipGold else TextPrimary
                                )
                                Text(track.name, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            earnedCredits?.let { cr ->
                                Surface(
                                    color = ChampionshipGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.border(1.dp, ChampionshipGold, RoundedCornerShape(10.dp))
                                ) {
                                    Text(
                                        "+$cr CR",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = ChampionshipGold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Standings table
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            telemetry.standings.forEach { row ->
                                Surface(
                                    color = if (row.isPlayer) RacingCrimson.copy(alpha = 0.22f) else CarbonSurfaceElevated,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (row.isPlayer) RacingCrimson else CarbonBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                "P${row.position}",
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = if (row.position == 1) ChampionshipGold else ElectricCyan
                                            )
                                            Text(
                                                if (row.isPlayer) "${row.name} (YOU)" else row.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary
                                            )
                                            Text(
                                                GameCatalog.carById(row.carId).name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }

                                        Text(
                                            "BEST ${RaceViewModel.formatTimeMs(row.bestLapMs)}",
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TelemetryGreen
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onExitToMenu,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("PADDOCK MENU", color = TextPrimary)
                            }
                            OutlinedButton(
                                onClick = { startRaceInEngine() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("RACE AGAIN", color = ElectricCyan)
                            }
                            val nextRound = launchConfig.championshipRound?.plus(1)
                            if (launchConfig.mode == RaceMode.CHAMPIONSHIP && nextRound != null && nextRound <= GameCatalog.championshipRounds.size) {
                                Button(
                                    onClick = { onNextChampionshipRound(nextRound) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RacingCrimson),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(48.dp)
                                ) {
                                    Text("NEXT ROUND $nextRound", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
