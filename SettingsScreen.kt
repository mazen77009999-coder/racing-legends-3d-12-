package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CameraMode
import com.example.data.model.GraphicsQuality
import com.example.ui.components.MotorsportPanel
import com.example.ui.components.PaddockTopBar
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonObsidian
import com.example.ui.theme.CarbonSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onUpdateSettings: (
        driverName: String?,
        quality: GraphicsQuality?,
        audioEnabled: Boolean?,
        masterVolume: Float?,
        defaultCamera: CameraMode?
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var driverNameInput by remember(uiState.profile.driverName) {
        mutableStateOf(uiState.profile.driverName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        PaddockTopBar(
            title = "GRAPHICS & SYSTEM SETTINGS",
            credits = uiState.profile.credits,
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Graphics Presets
            MotorsportPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("3D GRAPHICS QUALITY", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    GraphicsQuality.entries.forEach { preset ->
                        val selected = uiState.graphicsQuality == preset
                        Surface(
                            color = if (selected) RacingCrimson.copy(alpha = 0.2f) else CarbonSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.5.dp,
                                    if (selected) RacingCrimson else CarbonBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onUpdateSettings(null, preset, null, null, null)
                                }
                                .testTag("quality_${preset.name}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(preset.displayName.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(preset.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Right: Audio, Camera & Driver Identity
            MotorsportPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("AUDIO & ENGINE SYNTH", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Engine, Tire Screech & SFX", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Switch(
                            checked = uiState.profile.audioEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateSettings(null, null, enabled, null, null)
                            }
                        )
                    }

                    Column {
                        Text(
                            "Master Volume: ${(uiState.profile.masterVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Slider(
                            value = uiState.profile.masterVolume,
                            onValueChange = { vol ->
                                onUpdateSettings(null, null, null, vol, null)
                            },
                            enabled = uiState.profile.audioEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan
                            )
                        )
                    }

                    Text("DEFAULT RACE CAMERA", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CameraMode.entries.forEach { cam ->
                            FilterChip(
                                selected = uiState.defaultCamera == cam,
                                onClick = { onUpdateSettings(null, null, null, null, cam) },
                                label = { Text(cam.label) }
                            )
                        }
                    }

                    Text("DRIVER PROFILE", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    OutlinedTextField(
                        value = driverNameInput,
                        onValueChange = {
                            driverNameInput = it
                            if (it.isNotBlank()) {
                                onUpdateSettings(it.trim(), null, null, null, null)
                            }
                        },
                        label = { Text("Driver Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
