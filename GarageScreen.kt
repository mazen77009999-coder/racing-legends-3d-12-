package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GameCatalog
import com.example.data.model.RimStyle
import com.example.data.model.SpoilerStyle
import com.example.data.repository.UpgradeType
import com.example.ui.components.MotorsportPanel
import com.example.ui.components.PaddockTopBar
import com.example.ui.components.RacingWebController
import com.example.ui.components.RacingWebView
import com.example.ui.components.SpecStatBar
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonObsidian
import com.example.ui.theme.CarbonSurfaceElevated
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState
import org.json.JSONObject

@Composable
fun GarageScreen(
    uiState: MainUiState,
    onSelectCar: (String) -> Unit,
    onUnlockOrEquip: (String) -> Unit,
    onUpdatePaint: (String, String) -> Unit,
    onUpdateRims: (String, String) -> Unit,
    onUpdateSpoiler: (String, String) -> Unit,
    onUpgrade: (String, UpgradeType) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val webController = remember { RacingWebController() }
    val selectedId = uiState.selectedGarageCarId
    val spec = GameCatalog.carById(selectedId)
    val custom = uiState.selectedGarageCar
    val isEquipped = uiState.profile.activeCarId == selectedId
    val isUnlocked = custom?.isUnlocked == true

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Paint, 1=Aero & Rims, 2=Performance

    // Initialize 3D Garage turntable once
    LaunchedEffect(Unit) {
        val cfg = JSONObject().apply {
            put("carId", selectedId)
            put("paintHex", custom?.paintHex ?: spec.defaultPaintHex)
            put("rimStyle", custom?.rimStyle ?: "FORGED_5")
            put("spoilerStyle", custom?.spoilerStyle ?: "GT_WING")
            put("quality", uiState.graphicsQuality.name)
        }
        webController.startGarage(cfg.toString())
    }

    // Live-update 3D car whenever customization changes
    LaunchedEffect(selectedId, custom?.paintHex, custom?.rimStyle, custom?.spoilerStyle) {
        val cfg = JSONObject().apply {
            put("carId", selectedId)
            put("paintHex", custom?.paintHex ?: spec.defaultPaintHex)
            put("rimStyle", custom?.rimStyle ?: "FORGED_5")
            put("spoilerStyle", custom?.spoilerStyle ?: "GT_WING")
        }
        webController.updateGarageCar(cfg.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        PaddockTopBar(
            title = "3D SHOWROOM & TUNING",
            credits = uiState.profile.credits,
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Interactive 3D Car Showroom + Car Selector Strip
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
                ) {
                    RacingWebView(
                        controller = webController,
                        keepScreenOn = false,
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        color = CarbonObsidian.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            "DRAG TO ORBIT 360°",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    uiState.feedbackMessage?.let { msg ->
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                msg,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CarbonObsidian,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Car Selector Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(GameCatalog.cars) { car ->
                        val carCustom = uiState.cars.firstOrNull { it.carId == car.id }
                        val unlocked = carCustom?.isUnlocked == true
                        val isSelected = car.id == selectedId
                        Surface(
                            color = if (isSelected) RacingCrimson.copy(alpha = 0.24f) else CarbonSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .border(
                                    1.5.dp,
                                    if (isSelected) RacingCrimson else CarbonBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectCar(car.id) }
                                .testTag("garage_car_${car.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (!unlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = ChampionshipGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else if (uiState.profile.activeCarId == car.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Equipped",
                                        tint = TelemetryGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    car.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Right: Customization & Performance Panel
            MotorsportPanel(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .padding(bottom = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(spec.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text(spec.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                        // Tab Switcher
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Paint", "Aero & Rims", "Upgrades").forEachIndexed { idx, label ->
                                FilterChip(
                                    selected = selectedTab == idx,
                                    onClick = { selectedTab = idx },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RacingCrimson,
                                        selectedLabelColor = TextPrimary
                                    )
                                )
                            }
                        }

                        when (selectedTab) {
                            0 -> {
                                Text("METALLIC CLEARCOAT LIVERY", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GameCatalog.paintOptions.take(4).forEach { paint ->
                                        PaintSwatch(
                                            hex = paint.hex,
                                            selected = custom?.paintHex == paint.hex,
                                            onClick = { onUpdatePaint(selectedId, paint.hex) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GameCatalog.paintOptions.drop(4).forEach { paint ->
                                        PaintSwatch(
                                            hex = paint.hex,
                                            selected = custom?.paintHex == paint.hex,
                                            onClick = { onUpdatePaint(selectedId, paint.hex) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                SpecStatBar("TOP SPEED (${spec.topSpeedKmh + (custom?.engineLevel ?: 0) * 12} KM/H)", (spec.topSpeedKmh * 100 / 360) + (custom?.engineLevel ?: 0) * 3)
                                SpecStatBar("ACCELERATION", spec.accelRating + (custom?.turboLevel ?: 0) * 3, accentColor = RacingCrimson)
                                SpecStatBar("HANDLING", spec.handlingRating + (custom?.gripLevel ?: 0) * 3, accentColor = TelemetryGreen)
                            }
                            1 -> {
                                Text("FORGED WHEEL RIMS", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RimStyle.entries.forEach { rim ->
                                        FilterChip(
                                            selected = custom?.rimStyle == rim.id,
                                            onClick = { onUpdateRims(selectedId, rim.id) },
                                            label = { Text(rim.displayName, maxLines = 1) }
                                        )
                                    }
                                }
                                Text("REAR AERO WING", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpoilerStyle.entries.forEach { sp ->
                                        FilterChip(
                                            selected = custom?.spoilerStyle == sp.id,
                                            onClick = { onUpdateSpoiler(selectedId, sp.id) },
                                            label = { Text(sp.displayName, maxLines = 1) }
                                        )
                                    }
                                }
                            }
                            2 -> {
                                if (!isUnlocked) {
                                    Text(
                                        "Unlock this vehicle to install motorsport performance upgrades.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ChampionshipGold
                                    )
                                } else {
                                    UpgradeRow("Engine", custom?.engineLevel ?: 0, UpgradeType.ENGINE) {
                                        onUpgrade(selectedId, UpgradeType.ENGINE)
                                    }
                                    UpgradeRow("Turbocharger", custom?.turboLevel ?: 0, UpgradeType.TURBO) {
                                        onUpgrade(selectedId, UpgradeType.TURBO)
                                    }
                                    UpgradeRow("Tires & Suspension", custom?.gripLevel ?: 0, UpgradeType.GRIP) {
                                        onUpgrade(selectedId, UpgradeType.GRIP)
                                    }
                                    UpgradeRow("Carbon Brakes", custom?.brakeLevel ?: 0, UpgradeType.BRAKE) {
                                        onUpgrade(selectedId, UpgradeType.BRAKE)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onUnlockOrEquip(selectedId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                !isUnlocked -> ChampionshipGold
                                isEquipped -> TelemetryGreen
                                else -> RacingCrimson
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("garage_equip_button")
                    ) {
                        Text(
                            text = when {
                                !isUnlocked -> "UNLOCK FOR ${spec.priceCredits} CR"
                                isEquipped -> "EQUIPPED FOR RACE"
                                else -> "SELECT CAR"
                            },
                            color = if (!isUnlocked || isEquipped) CarbonObsidian else TextPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaintSwatch(
    hex: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(RacingCrimson)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Color.White else CarbonBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun UpgradeRow(
    title: String,
    level: Int,
    type: UpgradeType,
    onUpgrade: () -> Unit
) {
    val nextCost = type.baseCost * (level + 1)
    Surface(
        color = CarbonSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CarbonBorder, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Stage $level / 3", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            }
            Button(
                onClick = onUpgrade,
                enabled = level < 3,
                colors = ButtonDefaults.buttonColors(containerColor = RacingCrimson),
                modifier = Modifier.height(40.dp)
            ) {
                Text(if (level >= 3) "MAX" else "$nextCost CR", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
