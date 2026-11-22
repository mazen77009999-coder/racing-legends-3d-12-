package com.example

import com.example.multiplayer.LobbyRoomState
import com.example.multiplayer.MultiplayerCodec
import com.example.multiplayer.MultiplayerPlayerSlot
import com.example.multiplayer.TelemetryPacket
import com.example.multiplayer.WireEnvelope
import com.example.ui.viewmodel.RaceViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `formatTimeMs formats lap times accurately`() {
        assertEquals("--:--.--", RaceViewModel.formatTimeMs(0L))
        assertEquals("1:05.42", RaceViewModel.formatTimeMs(65420L))
        assertEquals("0:48.09", RaceViewModel.formatTimeMs(48095L))
    }

    @Test
    fun `MultiplayerCodec encodes and decodes lobby and telemetry envelopes`() {
        val lobby = LobbyRoomState(
            roomCode = "RL3D-88",
            trackId = "neo_tokyo",
            laps = 3,
            players = listOf(
                MultiplayerPlayerSlot("p1", "Alex", "apex_v8", "#E11D48", true, true, 14)
            ),
            raceStarting = false
        )
        val envelope = WireEnvelope(
            type = "LOBBY_STATE",
            lobby = lobby,
            telemetry = TelemetryPacket("p1", 210, 2, 0.55f, 123456L)
        )
        val json = MultiplayerCodec.encode(envelope)
        val decoded = MultiplayerCodec.decode(json)

        assertNotNull(decoded)
        assertEquals("LOBBY_STATE", decoded?.type)
        assertEquals("RL3D-88", decoded?.lobby?.roomCode)
        assertEquals("neo_tokyo", decoded?.lobby?.trackId)
        assertEquals(210, decoded?.telemetry?.speedKmh)
    }
}
