package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceStanding
import com.example.data.model.TelemetryState
import com.example.data.model.TrackSpec
import com.example.ui.theme.CarbonObsidian
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.RaceViewModel

@Composable
fun TelemetryHud(
    telemetry: TelemetryState,
    track: TrackSpec,
    lapBannerText: String?,
    onInputChanged: (throttle: Float, brake: Float, steerLeft: Float, steerRight: Float) -> Unit,
    onCycleCamera: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var throttleHeld by remember { mutableStateOf(false) }
    var brakeHeld by remember { mutableStateOf(false) }
    var leftHeld by remember { mutableStateOf(false) }
    var rightHeld by remember { mutableStateOf(false) }

    fun pushInputs() {
        onInputChanged(
            if (throttleHeld) 1f else 0f,
            if (brakeHeld) 1f else 0f,
            if (leftHeld) 1f else 0f,
            if (rightHeld) 1f else 0f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // TOP TELEMETRY BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Position, Lap, Lap Timers
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CarbonObsidian.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, RacingCrimson.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("POS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                "${telemetry.position}/${telemetry.totalRacers}",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (telemetry.position == 1) ChampionshipGold else TextPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(Color.White.copy(alpha = 0.16f))
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LAP", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                "${telemetry.lap}/${telemetry.totalLaps}",
                                style = MaterialTheme.typography.titleLarge,
                                color = ElectricCyan
                            )
                        }
                    }
                }

                Surface(
                    color = CarbonObsidian.copy(alpha = 0.76f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                        Text(
                            "LAP  ${RaceViewModel.formatTimeMs(telemetry.lapTimeMs)}",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "BEST ${RaceViewModel.formatTimeMs(telemetry.bestLapMs)}",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = TelemetryGreen
                        )
                    }
                }
            }

            // Center: Starting Lights & Lap Banner
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(
                    visible = telemetry.countdownStep >= 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    StartingLightsBar(step = telemetry.countdownStep)
                }

                AnimatedVisibility(
                    visible = lapBannerText != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = RacingCrimson.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = lapBannerText.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                if (telemetry.isDrifting) {
                    Surface(
                        color = WarningAmber.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "DRIFTING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CarbonObsidian,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Right: Minimap + FPS + Camera Switch + Pause
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackMinimap(
                    track = track,
                    standings = telemetry.standings,
                    modifier = Modifier.size(64.dp)
                )

                Surface(
                    color = CarbonObsidian.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${telemetry.fps} FPS",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = TelemetryGreen,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        IconButton(
                            onClick = onCycleCamera,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("camera_toggle_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Switch camera view",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    telemetry.cameraMode.label.uppercase(),
                                    fontSize = 8.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = onPauseClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("pause_race_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause race",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // BOTTOM ROW: Steering Pads (Left) | Speedo/RPM/Gear Gauge (Center) | Brake/Gas Pedals (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Steering Controls
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TouchSteerButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Steer Left",
                    isActive = leftHeld,
                    testTag = "steer_left_button",
                    onPressedChange = { down ->
                        leftHeld = down
                        pushInputs()
                    }
                )
                TouchSteerButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Steer Right",
                    isActive = rightHeld,
                    testTag = "steer_right_button",
                    onPressedChange = { down ->
                        rightHeld = down
                        pushInputs()
                    }
                )
            }

            // Center Speedometer + RPM Gauge + Gear Indicator
            SpeedRpmCluster(
                speedKmh = telemetry.speedKmh,
                rpmNorm = telemetry.rpmNorm,
                rpm = telemetry.rpm,
                gear = telemetry.gear
            )

            // Right Brake & Accelerator Pedals
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                TouchPedalButton(
                    label = "BRAKE",
                    accentColor = RacingCrimson,
                    isActive = brakeHeld,
                    widthDp = 74,
                    heightDp = 82,
                    testTag = "brake_button",
                    onPressedChange = { down ->
                        brakeHeld = down
                        pushInputs()
                    }
                )
                TouchPedalButton(
                    label = "GAS",
                    accentColor = TelemetryGreen,
                    isActive = throttleHeld,
                    widthDp = 80,
                    heightDp = 104,
                    testTag = "throttle_button",
                    onPressedChange = { down ->
                        throttleHeld = down
                        pushInputs()
                    }
                )
            }
        }
    }
}

@Composable
private fun StartingLightsBar(step: Int) {
    Surface(
        color = CarbonObsidian.copy(alpha = 0.88f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val litCount = if (step > 0) 6 - step else 0
                for (i in 0 until 5) {
                    val isRed = step > 0 && i < litCount
                    val isGreen = step == 0
                    val color = when {
                        isGreen -> TelemetryGreen
                        isRed -> RacingCrimson
                        else -> Color(0xFF272D3D)
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = if (step == 0) "LIGHTS OUT — GO!" else "GET READY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (step == 0) TelemetryGreen else TextPrimary
            )
        }
    }
}

@Composable
fun SpeedRpmCluster(
    speedKmh: Int,
    rpmNorm: Float,
    rpm: Int,
    gear: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CarbonObsidian.copy(alpha = 0.78f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
            .testTag("speedometer_cluster")
    ) {
        Box(
            modifier = Modifier
                .width(196.dp)
                .height(92.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 9.dp.toPx()
                val arcSize = Size(size.width - 16.dp.toPx(), (size.height - 6.dp.toPx()) * 1.65f)
                val topLeft = Offset(8.dp.toPx(), 4.dp.toPx())

                // Background track arc
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = 185f,
                    sweepAngle = 170f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active RPM sweep
                val clamped = rpmNorm.coerceIn(0f, 1f)
                val rpmColor = when {
                    clamped > 0.86f -> RacingCrimson
                    clamped > 0.65f -> ChampionshipGold
                    else -> ElectricCyan
                }
                drawArc(
                    brush = Brush.horizontalGradient(listOf(ElectricCyan, rpmColor)),
                    startAngle = 185f,
                    sweepAngle = 170f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$speedKmh",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "KM/H • $rpm RPM",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }

                Surface(
                    color = RacingCrimson.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, RacingCrimson, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = gear,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gear == "R") WarningAmber else ElectricCyan,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackMinimap(
    track: TrackSpec,
    standings: List<RaceStanding>,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CarbonObsidian.copy(alpha = 0.78f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp)
        ) {
            val pts = track.minimapPoints
            if (pts.size < 3) return@Canvas
            val minX = pts.minOf { it.first }
            val maxX = pts.maxOf { it.first }
            val minY = pts.minOf { it.second }
            val maxY = pts.maxOf { it.second }
            val spanX = (maxX - minX).coerceAtLeast(1f)
            val spanY = (maxY - minY).coerceAtLeast(1f)
            val scale = minOf(size.width / spanX, size.height / spanY) * 0.9f
            val offsetX = (size.width - spanX * scale) * 0.5f - minX * scale
            val offsetY = (size.height - spanY * scale) * 0.5f - minY * scale

            fun mapPt(p: Pair<Float, Float>): Offset =
                Offset(p.first * scale + offsetX, p.second * scale + offsetY)

            val path = Path()
            val mapped = pts.map(::mapPt)
            path.moveTo(mapped.first().x, mapped.first().y)
            for (i in 1 until mapped.size) {
                path.lineTo(mapped[i].x, mapped[i].y)
            }
            path.close()

            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.35f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw racers interpolated along polyline
            standings.forEach { racer ->
                val u = racer.progressU.coerceIn(0f, 0.999f) * mapped.size
                val idx = u.toInt() % mapped.size
                val nextIdx = (idx + 1) % mapped.size
                val frac = u - u.toInt()
                val p0 = mapped[idx]
                val p1 = mapped[nextIdx]
                val pos = Offset(
                    p0.x + (p1.x - p0.x) * frac,
                    p0.y + (p1.y - p0.y) * frac
                )
                drawCircle(
                    color = if (racer.isPlayer) ElectricCyan else RacingCrimson,
                    radius = if (racer.isPlayer) 4.dp.toPx() else 2.8.dp.toPx(),
                    center = pos
                )
            }
        }
    }
}

@Composable
private fun TouchSteerButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    testTag: String,
    onPressedChange: (Boolean) -> Unit
) {
    val bg = if (isActive) ElectricCyan.copy(alpha = 0.38f) else CarbonSurface.copy(alpha = 0.72f)
    val border = if (isActive) ElectricCyan else Color.White.copy(alpha = 0.24f)

    Box(
        modifier = Modifier
            .size(width = 82.dp, height = 74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .testTag(testTag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressedChange(true)
                    waitForUpOrCancellation()
                    onPressedChange(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) TextPrimary else TextSecondary,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun TouchPedalButton(
    label: String,
    accentColor: Color,
    isActive: Boolean,
    widthDp: Int,
    heightDp: Int,
    testTag: String,
    onPressedChange: (Boolean) -> Unit
) {
    val bg = if (isActive) accentColor.copy(alpha = 0.45f) else CarbonSurface.copy(alpha = 0.75f)
    val border = if (isActive) accentColor else accentColor.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .size(width = widthDp.dp, height = heightDp.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .testTag(testTag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressedChange(true)
                    waitForUpOrCancellation()
                    onPressedChange(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
    }
}
