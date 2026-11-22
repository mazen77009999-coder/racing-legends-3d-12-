package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.data.model.GameCatalog
import com.example.ui.components.PaddockTopBar
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonObsidian
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState

@Composable
fun ChampionshipScreen(
    uiState: MainUiState,
    onStartRound: (Int) -> Unit,
    onResetChampionship: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val profile = uiState.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        PaddockTopBar(
            title = "WORLD GT CHAMPIONSHIP",
            credits = profile.credits,
            onBack = onBack,
            trailingContent = {
                Surface(
                    color = ChampionshipGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, ChampionshipGold, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        "${profile.championshipPoints} PTS",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = ChampionshipGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GameCatalog.championshipRounds.forEach { round ->
                val track = GameCatalog.trackById(round.trackId)
                val isCompleted = profile.championshipCompleted || round.roundNumber < profile.championshipRound
                val isUnlocked = round.roundNumber <= profile.championshipRound

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CarbonSurface)
                        .border(
                            1.5.dp,
                            when {
                                isCompleted -> TelemetryGreen
                                isUnlocked -> RacingCrimson
                                else -> CarbonBorder
                            },
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = track.imageRes),
                        contentDescription = round.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        CarbonObsidian.copy(alpha = 0.35f),
                                        CarbonObsidian.copy(alpha = 0.94f)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isCompleted -> Icons.Default.CheckCircle
                                isUnlocked -> Icons.Default.EmojiEvents
                                else -> Icons.Default.Lock
                            },
                            contentDescription = "Round status",
                            tint = when {
                                isCompleted -> TelemetryGreen
                                isUnlocked -> ChampionshipGold
                                else -> TextSecondary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            when {
                                isCompleted -> "COMPLETED"
                                isUnlocked -> "ACTIVE ROUND"
                                else -> "LOCKED"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(round.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("${track.name} • ${round.laps} Laps", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(
                            "PRIZE POOL: +${round.prizeCredits} CR",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { onStartRound(round.roundNumber) },
                            enabled = isUnlocked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleted) TelemetryGreen else RacingCrimson
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("champ_round_${round.roundNumber}")
                        ) {
                            Text(
                                if (isCompleted) "REPLAY ROUND" else if (isUnlocked) "RACE ROUND ${round.roundNumber}" else "LOCKED",
                                color = if (isCompleted) CarbonObsidian else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (profile.championshipCompleted) {
                    "CHAMPIONSHIP COMPLETE! Total Points: ${profile.championshipPoints}"
                } else {
                    "Score points (25 / 18 / 15 / 12) across all 3 circuits to win the title."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (profile.championshipCompleted) ChampionshipGold else TextSecondary
            )

            OutlinedButton(
                onClick = onResetChampionship,
                modifier = Modifier.height(48.dp)
            ) {
                Text("RESET SEASON", color = TextPrimary)
            }
        }
    }
}
