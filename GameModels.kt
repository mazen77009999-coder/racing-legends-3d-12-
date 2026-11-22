package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R

enum class GraphicsQuality(
    val displayName: String,
    val description: String
) {
    LOW("Low", "Battery saver • 1.0x render • Shadows off"),
    MEDIUM("Medium", "Balanced • 1.35x render • Standard shadows"),
    HIGH("High", "Crisp PBR • 1.75x render • Soft shadows & reflections"),
    ULTRA("Ultra", "S23 Ultra Tuned • 2.25x render • Dynamic headlights & max FX")
}

enum class CameraMode(val label: String) {
    CHASE("Chase"),
    COCKPIT("Cockpit"),
    FRONT("Bumper")
}

enum class RimStyle(val id: String, val displayName: String) {
    FORGED_5("FORGED_5", "5-Spoke Forged"),
    TURBINE("TURBINE", "Aero Turbine"),
    SPLIT_MESH("SPLIT_MESH", "Gold Split-Mesh")
}

enum class SpoilerStyle(val id: String, val displayName: String) {
    LIP("LIP", "Street Lip"),
    GT_WING("GT_WING", "Carbon GT Wing"),
    ENDURANCE("ENDURANCE", "Twin Endurance")
}

data class PaintOption(
    val name: String,
    val hex: String
)

data class CarSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val priceCredits: Int,
    val topSpeedKmh: Int,
    val accelRating: Int, // 1..100
    val handlingRating: Int, // 1..100
    val brakingRating: Int, // 1..100
    val defaultPaintHex: String,
    val isEv: Boolean = false
)

data class TrackSpec(
    val id: String,
    val name: String,
    val location: String,
    val lengthLabel: String,
    val defaultLaps: Int,
    val difficultyLabel: String,
    @DrawableRes val imageRes: Int,
    val minimapPoints: List<Pair<Float, Float>>
)

data class ChampionshipRound(
    val roundNumber: Int,
    val title: String,
    val trackId: String,
    val laps: Int,
    val prizeCredits: Int
)

data class RaceStanding(
    val id: String,
    val name: String,
    val carId: String,
    val isPlayer: Boolean,
    val position: Int,
    val bestLapMs: Long,
    val finishTimeMs: Long,
    val progressU: Float
)

data class TelemetryState(
    val speedKmh: Int = 0,
    val rpm: Int = 1100,
    val rpmNorm: Float = 0.12f,
    val maxRpm: Int = 8200,
    val gear: String = "1",
    val lap: Int = 1,
    val totalLaps: Int = 3,
    val position: Int = 4,
    val totalRacers: Int = 4,
    val lapTimeMs: Long = 0L,
    val bestLapMs: Long = 0L,
    val elapsedMs: Long = 0L,
    val countdownStep: Int = 5,
    val cameraMode: CameraMode = CameraMode.CHASE,
    val isDrifting: Boolean = false,
    val fps: Int = 60,
    val finished: Boolean = false,
    val standings: List<RaceStanding> = emptyList()
)

data class RaceLaunchConfig(
    val trackId: String,
    val laps: Int,
    val mode: RaceMode,
    val championshipRound: Int? = null,
    val isMultiplayer: Boolean = false
)

enum class RaceMode {
    QUICK_RACE,
    CHAMPIONSHIP,
    MULTIPLAYER
}

object GameCatalog {
    val paintOptions = listOf(
        PaintOption("Crimson Corsa", "#E11D48"),
        PaintOption("Electric Cyan", "#00E5FF"),
        PaintOption("Cobalt Blue", "#2563EB"),
        PaintOption("Solar Gold", "#F59E0B"),
        PaintOption("Emerald GT", "#10B981"),
        PaintOption("Arctic Pearl", "#F1F5F9"),
        PaintOption("Stealth Carbon", "#334155"),
        PaintOption("Sunset Orange", "#F97316")
    )

    val cars = listOf(
        CarSpec(
            id = "apex_v8",
            name = "Apex V8 GT",
            subtitle = "Twin-Turbo V8 • Rear-Wheel GT",
            priceCredits = 0,
            topSpeedKmh = 266,
            accelRating = 74,
            handlingRating = 78,
            brakingRating = 76,
            defaultPaintHex = "#E11D48"
        ),
        CarSpec(
            id = "valkyrie_rs",
            name = "Valkyrie RS",
            subtitle = "Naturally Aspirated V10 • Track Aero",
            priceCredits = 4500,
            topSpeedKmh = 292,
            accelRating = 84,
            handlingRating = 86,
            brakingRating = 85,
            defaultPaintHex = "#2563EB"
        ),
        CarSpec(
            id = "spectre_ev",
            name = "Spectre Hyper-EV",
            subtitle = "Tri-Motor AWD • Instant Torque",
            priceCredits = 9000,
            topSpeedKmh = 310,
            accelRating = 94,
            handlingRating = 88,
            brakingRating = 90,
            defaultPaintHex = "#00E5FF",
            isEv = true
        ),
        CarSpec(
            id = "titan_lm",
            name = "Titan LM Prototype",
            subtitle = "Le Mans Hypercar • Extreme Downforce",
            priceCredits = 16000,
            topSpeedKmh = 335,
            accelRating = 96,
            handlingRating = 97,
            brakingRating = 96,
            defaultPaintHex = "#F59E0B"
        )
    )

    val tracks = listOf(
        TrackSpec(
            id = "silverpeak_gp",
            name = "Silverpeak GP Circuit",
            location = "Sunset International Arena",
            lengthLabel = "4.8 km",
            defaultLaps = 3,
            difficultyLabel = "Pro GP",
            imageRes = R.drawable.img_track_grandprix,
            minimapPoints = listOf(
                0f to -140f, 0f to 20f, 15f to 150f, 95f to 220f, 205f to 185f,
                240f to 85f, 175f to 15f, 210f to -85f, 265f to -185f,
                185f to -270f, 75f to -245f, 12f to -205f
            )
        ),
        TrackSpec(
            id = "neo_tokyo",
            name = "Neo Tokyo Midnight",
            location = "Metropolitan Express Loop",
            lengthLabel = "5.1 km",
            defaultLaps = 3,
            difficultyLabel = "Technical Night",
            imageRes = R.drawable.img_track_night_city,
            minimapPoints = listOf(
                0f to -150f, 0f to 40f, 35f to 165f, 145f to 195f, 235f to 115f,
                190f to 15f, 115f to -20f, 180f to -125f, 235f to -225f,
                125f to -280f, 25f to -240f
            )
        ),
        TrackSpec(
            id = "alpine_pass",
            name = "Mont Blanc Alpine Pass",
            location = "High-Altitude Ridge",
            lengthLabel = "5.4 km",
            defaultLaps = 3,
            difficultyLabel = "Master",
            imageRes = R.drawable.img_track_alpine,
            minimapPoints = listOf(
                0f to -135f, 0f to 35f, -55f to 155f, 25f to 250f, 145f to 225f,
                220f to 120f, 165f to 10f, 240f to -105f, 210f to -235f,
                95f to -270f, 10f to -215f
            )
        )
    )

    val championshipRounds = listOf(
        ChampionshipRound(1, "Round 1: Sunset Opener", "silverpeak_gp", 3, 2500),
        ChampionshipRound(2, "Round 2: Midnight Showdown", "neo_tokyo", 3, 4000),
        ChampionshipRound(3, "Round 3: Alpine Grand Finale", "alpine_pass", 3, 7500)
    )

    fun carById(id: String): CarSpec = cars.firstOrNull { it.id == id } ?: cars.first()
    fun trackById(id: String): TrackSpec = tracks.firstOrNull { it.id == id } ?: tracks.first()
}
