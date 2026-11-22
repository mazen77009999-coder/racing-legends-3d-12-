package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ChampionshipScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MultiplayerScreen
import com.example.ui.screens.RaceScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrackSelectScreen
import com.example.ui.theme.RacingLegendsTheme
import com.example.ui.viewmodel.MainViewModel

object Routes {
    const val HOME = "home"
    const val TRACK_SELECT = "track_select"
    const val GARAGE = "garage"
    const val CHAMPIONSHIP = "championship"
    const val MULTIPLAYER = "multiplayer"
    const val SETTINGS = "settings"
    const val RACE = "race"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureImmersiveMode()

        setContent {
            RacingLegendsTheme {
                RacingLegendsApp()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configureImmersiveMode()
        }
    }

    private fun configureImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun RacingLegendsApp(
    mainViewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val launchConfig by mainViewModel.launchConfig.collectAsStateWithLifecycle()

    val backendMode by mainViewModel.backendMode.collectAsStateWithLifecycle()
    val mpConnState by mainViewModel.mpConnectionState.collectAsStateWithLifecycle()
    val mpLobby by mainViewModel.mpLobbyState.collectAsStateWithLifecycle()
    val mpLatency by mainViewModel.mpLatencyMs.collectAsStateWithLifecycle()
    val mpError by mainViewModel.mpError.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                uiState = uiState,
                onQuickRaceClick = { navController.navigate(Routes.TRACK_SELECT) },
                onChampionshipClick = { navController.navigate(Routes.CHAMPIONSHIP) },
                onGarageClick = { navController.navigate(Routes.GARAGE) },
                onMultiplayerClick = { navController.navigate(Routes.MULTIPLAYER) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.TRACK_SELECT) {
            TrackSelectScreen(
                uiState = uiState,
                onStartRace = { trackId, laps ->
                    mainViewModel.prepareQuickRace(trackId, laps)
                    navController.navigate(Routes.RACE)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GARAGE) {
            GarageScreen(
                uiState = uiState,
                onSelectCar = { carId ->
                    mainViewModel.clearFeedback()
                    mainViewModel.selectGarageCar(carId)
                },
                onUnlockOrEquip = { carId -> mainViewModel.purchaseOrEquipCar(carId) },
                onUpdatePaint = { carId, hex -> mainViewModel.updatePaint(carId, hex) },
                onUpdateRims = { carId, rim -> mainViewModel.updateRims(carId, rim) },
                onUpdateSpoiler = { carId, spoiler -> mainViewModel.updateSpoiler(carId, spoiler) },
                onUpgrade = { carId, type -> mainViewModel.upgradeCar(carId, type) },
                onBack = {
                    mainViewModel.clearFeedback()
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.CHAMPIONSHIP) {
            ChampionshipScreen(
                uiState = uiState,
                onStartRound = { round ->
                    mainViewModel.prepareChampionshipRound(round)
                    navController.navigate(Routes.RACE)
                },
                onResetChampionship = { mainViewModel.resetChampionship() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MULTIPLAYER) {
            MultiplayerScreen(
                uiState = uiState,
                backendMode = backendMode,
                connectionState = mpConnState,
                lobbyState = mpLobby,
                latencyMs = mpLatency,
                errorMessage = mpError,
                onSelectBackendMode = { mainViewModel.setMultiplayerBackendMode(it) },
                onConnect = { url, code, trackId ->
                    mainViewModel.connectMultiplayer(url, code, trackId)
                },
                onSelectTrack = { mainViewModel.setMultiplayerTrack(it) },
                onToggleReady = { mainViewModel.setMultiplayerReady(it) },
                onLaunchMatch = {
                    mainViewModel.launchMultiplayerMatch()
                    navController.navigate(Routes.RACE)
                },
                onDisconnect = { mainViewModel.disconnectMultiplayer() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                uiState = uiState,
                onUpdateSettings = { name, quality, audio, vol, cam ->
                    mainViewModel.updateSettings(name, quality, audio, vol, cam)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RACE) {
            RaceScreen(
                uiState = uiState,
                launchConfig = launchConfig,
                onRecordResult = { trackId, pos, bestLap, totalTime, mode, round ->
                    mainViewModel.recordRaceResult(trackId, pos, bestLap, totalTime, mode, round)
                },
                onSendMultiplayerTelemetry = { speed, lap, u ->
                    mainViewModel.sendMultiplayerTelemetry(speed, lap, u)
                },
                onNextChampionshipRound = { nextRound ->
                    mainViewModel.prepareChampionshipRound(nextRound)
                },
                onExitToMenu = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
    }
}
