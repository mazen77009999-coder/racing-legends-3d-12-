package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RacingColorScheme = darkColorScheme(
    primary = RacingCrimson,
    onPrimary = TextPrimary,
    primaryContainer = Color(0xFF4C0519),
    onPrimaryContainer = RacingCrimsonLight,
    secondary = ElectricCyan,
    onSecondary = CarbonObsidian,
    secondaryContainer = Color(0xFF083344),
    onSecondaryContainer = ElectricCyan,
    tertiary = ChampionshipGold,
    onTertiary = CarbonObsidian,
    background = CarbonObsidian,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = CarbonSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CarbonBorder,
    error = WarningAmber
)

private val RacingShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun RacingLegendsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RacingColorScheme,
        typography = RacingTypography,
        shapes = RacingShapes,
        content = content
    )
}
