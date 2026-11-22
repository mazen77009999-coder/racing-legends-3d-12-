package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.GameCatalog
import com.example.multiplayer.LobbyRoomState
import com.example.multiplayer.MultiplayerBackendMode
import com.example.multiplayer.MultiplayerConnectionState
import com.example.ui.components.MotorsportPanel
import com.example.ui.components.PaddockTopBar
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
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.MainUiState

@Composable
fun MultiplayerScreen(
    uiState: MainUiState,
    backendMode: MultiplayerBackendMode,
    connectionState: MultiplayerConnectionState,
    lobbyState: LobbyRoomState?,
    latencyMs: Int,
    errorMessage: String?,
    onSelectBackendMode: (MultiplayerBackendMode) -> Unit,
    onConnect: (serverUrl: String, roomCode: String, trackId: String) -> Unit,
    onSelectTrack: (String) -> Unit,
    onToggleReady: (Boolean) -> Unit,
    onLaunchMatch: () -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onDisconnect()
        onBack()
    }

    var serverUrl by remember { mutableStateOf("ws://10.0.2.2:8080/ws") }
    var roomCode by remember { mutableStateOf("RL3D-88") }
    var selectedTrackId by remember { mutableStateOf(GameCatalog.tracks.first().id) }

    val isConnected = connectionState == MultiplayerConnectionState.CONNECTED
    val localReady = lobbyState?.players?.firstOrNull { it.isHost }?.isReady == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        PaddockTopBar(
            title = "ONLINE MULTIPLAYER LOBBY",
            credits = uiState.profile.credits,
            onBack = {
                onDisconnect()
                onBack()
            },
            trailingContent = {
                if (isConnected) {
                    Surface(
                        color = TelemetryGreen.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.border(1.dp, TelemetryGreen, RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            "PING ${latencyMs}ms",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = TelemetryGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Backend Transport & Connection Setup
            MotorsportPanel(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("TRANSPORT ARCHITECTURE", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = backendMode == MultiplayerBackendMode.LOCAL_RELAY,
                            onClick = { onSelectBackendMode(MultiplayerBackendMode.LOCAL_RELAY) },
                            label = { Text("Local Relay Sim") }
                        )
                        FilterChip(
                            selected = backendMode == MultiplayerBackendMode.WEBSOCKET_SERVER,
                            onClick = { onSelectBackendMode(MultiplayerBackendMode.WEBSOCKET_SERVER) },
                            label = { Text("WebSocket Server") }
                        )
                    }

                    Text(
                        text = if (backendMode == MultiplayerBackendMode.LOCAL_RELAY) {
                            "Uses built-in Moshi WireEnvelope relay loopback to simulate 4-player lobby & telemetry sync locally."
                        } else {
                            "Connects via OkHttp WebSocket to an external relay server implementing the WireEnvelope JSON protocol."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    if (backendMode == MultiplayerBackendMode.WEBSOCKET_SERVER) {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("WebSocket Relay URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { roomCode = it.uppercase() },
                        label = { Text("Room Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("LOBBY CIRCUIT", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GameCatalog.tracks.forEach { t ->
                            FilterChip(
                                selected = (lobbyState?.trackId ?: selectedTrackId) == t.id,
                                onClick = {
                                    selectedTrackId = t.id
                                    if (isConnected) onSelectTrack(t.id)
                                },
                                label = { Text(t.name.substringBefore(" "), maxLines = 1) }
                            )
                        }
                    }

                    errorMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = WarningAmber)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (!isConnected) {
                        Button(
                            onClick = { onConnect(serverUrl, roomCode, selectedTrackId) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("mp_connect_button")
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = CarbonObsidian)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (connectionState == MultiplayerConnectionState.CONNECTING) "CONNECTING..." else "JOIN / HOST ROOM",
                                color = CarbonObsidian,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("LEAVE ROOM", color = RacingCrimson)
                        }
                    }
                }
            }

            // Right: 4-Player Grid Roster & Race Launch
            MotorsportPanel(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lobbyState?.let { "ROOM ${it.roomCode} • ${GameCatalog.trackById(it.trackId).name}" }
                                    ?: "WAITING FOR ROOM CONNECTION",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                "${lobbyState?.players?.size ?: 0}/4 DRIVERS",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }

                        if (lobbyState == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Join or host a room on the left to sync with racers.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            lobbyState.players.forEach { slot ->
                                val paint = runCatching { Color(android.graphics.Color.parseColor(slot.paintHex)) }
                                    .getOrDefault(RacingCrimson)
                                Surface(
                                    color = CarbonSurfaceElevated,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (slot.isReady) TelemetryGreen.copy(alpha = 0.6f) else CarbonBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(paint)
                                            )
                                            Column {
                                                Text(
                                                    text = if (slot.isHost) "${slot.driverName} (YOU)" else slot.driverName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    GameCatalog.carById(slot.carId).name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                "${slot.pingMs}ms",
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            Icon(
                                                imageVector = if (slot.isReady) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                                contentDescription = if (slot.isReady) "Ready" else "Waiting",
                                                tint = if (slot.isReady) TelemetryGreen else ChampionshipGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onToggleReady(!localReady) },
                            enabled = isConnected,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(if (localReady) "UNREADY" else "READY UP", color = TextPrimary)
                        }
                        Button(
                            onClick = onLaunchMatch,
                            enabled = isConnected,
                            colors = ButtonDefaults.buttonColors(containerColor = RacingCrimson),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("mp_start_match_button")
                        ) {
                            Text("START MULTIPLAYER RACE", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
