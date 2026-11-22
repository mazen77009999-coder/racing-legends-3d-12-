package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.RaceMode
import com.example.data.repository.RacingRepository
import com.example.data.repository.UpgradeType
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.RacingLegendsTheme
import com.example.ui.viewmodel.MainUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `app name matches Racing Legends 3D`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Racing Legends 3D", context.getString(R.string.app_name))
    }

    @Test
    fun `all local Threejs game assets are bundled`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rootAssets = context.assets.list("")?.toSet() ?: emptySet()
        val jsAssets = context.assets.list("js")?.toSet() ?: emptySet()

        assertTrue(rootAssets.contains("index.html"))
        assertTrue(jsAssets.contains("three.min.js"))
        assertTrue(jsAssets.contains("audio.js"))
        assertTrue(jsAssets.contains("textures.js"))
        assertTrue(jsAssets.contains("carBuilder.js"))
        assertTrue(jsAssets.contains("tracks.js"))
        assertTrue(jsAssets.contains("physics.js"))
        assertTrue(jsAssets.contains("engine.js"))
    }

    @Test
    fun `repository seeds cars records race rewards and upgrades locally`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = RacingRepository(db.racingDao())
            repo.ensureSeeded()

            val initialProfile = repo.profileFlow.first()
            val initialCars = repo.carsFlow.first()
            assertEquals(2000, initialProfile.credits)
            assertEquals(4, initialCars.size)
            assertTrue(initialCars.first { it.carId == "apex_v8" }.isUnlocked)
            assertFalse(initialCars.first { it.carId == "valkyrie_rs" }.isUnlocked)

            // Upgrade starter car engine
            val upgraded = repo.upgradeCar("apex_v8", UpgradeType.ENGINE)
            assertTrue(upgraded)
            assertEquals(1100, repo.profileFlow.first().credits)

            // Record championship race win
            val earned = repo.recordRaceCompletion(
                trackId = "silverpeak_gp",
                position = 1,
                bestLapMs = 54320L,
                totalTimeMs = 168900L,
                mode = RaceMode.CHAMPIONSHIP,
                championshipRound = 1
            )
            assertEquals(2400, earned)
            val updatedProfile = repo.profileFlow.first()
            assertEquals(3500, updatedProfile.credits)
            assertEquals(2, updatedProfile.championshipRound)
            assertEquals(25, updatedProfile.championshipPoints)
            assertEquals(1, updatedProfile.totalWins)

            val trackRecord = repo.trackRecordsFlow.first().first { it.trackId == "silverpeak_gp" }
            assertEquals(54320L, trackRecord.bestLapMs)
            assertEquals(1, trackRecord.bestPosition)
        } finally {
            db.close()
        }
    }

    @Test
    fun `home screen renders all mode buttons and dispatches clicks`() {
        var quickRaceClicked = false
        var garageClicked = false
        var champClicked = false
        var mpClicked = false
        var settingsClicked = false

        composeRule.setContent {
            RacingLegendsTheme {
                HomeScreen(
                    uiState = MainUiState(),
                    onQuickRaceClick = { quickRaceClicked = true },
                    onChampionshipClick = { champClicked = true },
                    onGarageClick = { garageClicked = true },
                    onMultiplayerClick = { mpClicked = true },
                    onSettingsClick = { settingsClicked = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("RACING LEGENDS 3D").assertIsDisplayed()

        composeRule.onNodeWithTag("quick_race_button").performClick()
        composeRule.onNodeWithTag("garage_button").performClick()
        composeRule.onNodeWithTag("championship_button").performClick()
        composeRule.onNodeWithTag("multiplayer_button").performClick()
        composeRule.onNodeWithTag("settings_button").performClick()

        assertTrue(quickRaceClicked)
        assertTrue(garageClicked)
        assertTrue(champClicked)
        assertTrue(mpClicked)
        assertTrue(settingsClicked)
    }

    @Test
    fun `RaceViewModel parses JS telemetry JSON correctly under Android runtime`() {
        val vm = com.example.ui.viewmodel.RaceViewModel()
        val sampleJson = """
            {
              "speedKmh": 248,
              "rpm": 7450,
              "rpmNorm": 0.89,
              "maxRpm": 8200,
              "gear": "5",
              "lap": 2,
              "totalLaps": 3,
              "position": 1,
              "totalRacers": 4,
              "lapTimeMs": 32150,
              "bestLapMs": 51400,
              "elapsedMs": 83550,
              "countdownStep": -1,
              "cameraMode": "COCKPIT",
              "isDrifting": true,
              "fps": 60,
              "finished": false,
              "standings": [
                {"id":"player","name":"Alex","carId":"apex_v8","isPlayer":true,"position":1,"bestLapMs":51400,"finishTimeMs":83550,"u":0.42}
              ]
            }
        """.trimIndent()

        vm.onJsTelemetry(sampleJson)
        val t = vm.telemetry.value
        assertEquals(248, t.speedKmh)
        assertEquals("5", t.gear)
        assertEquals(2, t.lap)
        assertEquals(1, t.position)
        assertEquals(com.example.data.model.CameraMode.COCKPIT, t.cameraMode)
        assertTrue(t.isDrifting)
        assertEquals(1, t.standings.size)
    }
}
