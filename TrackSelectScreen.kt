package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GameCatalog
import com.example.ui.components.PaddockTopBar
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonObsidian
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.RaceViewModel

@Composable
fun TrackSelectScreen(
    uiState: MainUiState,
    onStartRace: (trackId: String, laps: Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedTrackId by remember { mutableStateOf(GameCatalog.tracks.first().id) }
    var selectedLaps by remember { mutableIntStateOf(3) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        PaddockTopBar(
            title = "SELECT CIRCUIT",
            credits = uiState.profile.credits,
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GameCatalog.tracks.forEach { track ->
                val isSelected = track.id == selectedTrackId
                val record = uiState.trackRecords[track.id]

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CarbonSurface)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) RacingCrimson else CarbonBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedTrackId = track.id }
                        .testTag("track_card_${track.id}")
                ) {
                    Image(
                        painter = painterResource(id = track.imageRes),
                        contentDescription = track.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        CarbonObsidian.copy(alpha = 0.55f),
                                        CarbonObsidian.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )

                    Surface(
                        color = if (isSelected) RacingCrimson else CarbonObsidian.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            track.difficultyLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(track.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("${track.location} • ${track.lengthLabel}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (record != null && record.bestLapMs > 0) {
                                "BEST LAP: ${RaceViewModel.formatTimeMs(record.bestLapMs)}"
                            } else {
                                "BEST LAP: NO RECORD"
                            },
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (record != null && record.bestLapMs > 0) TelemetryGreen else ElectricCyan
                        )
                    }
                }
            }
        }

        // Bottom Launch Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("RACE LAPS:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                listOf(2, 3, 5).forEach { laps ->
                    FilterChip(
                        selected = selectedLaps == laps,
                        onClick = { selectedLaps = laps },
                        label = { Text("$laps Laps") }
                    )
                }
            }

            Button(
                onClick = { onStartRace(selectedTrackId, selectedLaps) },
                colors = ButtonDefaults.buttonColors(containerColor = RacingCrimson),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(50.dp)
                    .width(220.dp)
                    .testTag("launch_race_button")
            ) {
                Icon(Icons.Default.Flag, contentDescription = "Launch Race")
                Spacer(modifier = Modifier.width(8.dp))
                Text("START RACE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
