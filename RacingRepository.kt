package com.example.data.repository

import com.example.data.local.CarCustomizationEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.RacingDao
import com.example.data.local.TrackRecordEntity
import com.example.data.model.CameraMode
import com.example.data.model.GameCatalog
import com.example.data.model.GraphicsQuality
import com.example.data.model.RaceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class UpgradeType(val label: String, val baseCost: Int) {
    ENGINE("Engine", 900),
    TURBO("Turbocharger", 1100),
    GRIP("Tires & Suspension", 850),
    BRAKE("Carbon Brakes", 750)
}

class RacingRepository(private val dao: RacingDao) {

    val profileFlow: Flow<PlayerProfileEntity> = dao.observeProfile().map {
        it ?: PlayerProfileEntity()
    }

    val carsFlow: Flow<List<CarCustomizationEntity>> = dao.observeAllCars()
    val trackRecordsFlow: Flow<List<TrackRecordEntity>> = dao.observeTrackRecords()

    suspend fun ensureSeeded() {
        if (dao.getProfileOnce() == null) {
            dao.upsertProfile(PlayerProfileEntity())
        }
        val defaults = GameCatalog.cars.mapIndexed { idx, spec ->
            CarCustomizationEntity(
                carId = spec.id,
                isUnlocked = idx == 0, // Apex V8 GT unlocked from start
                paintHex = spec.defaultPaintHex,
                rimStyle = "FORGED_5",
                spoilerStyle = if (spec.id == "titan_lm") "ENDURANCE" else "GT_WING"
            )
        }
        dao.insertCarsIfAbsent(defaults)
    }

    suspend fun selectActiveCar(carId: String) {
        val car = dao.getCarOnce(carId) ?: return
        if (!car.isUnlocked) return
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(profile.copy(activeCarId = carId))
    }

    suspend fun unlockCar(carId: String): Boolean {
        val spec = GameCatalog.carById(carId)
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val car = dao.getCarOnce(carId) ?: return false
        if (car.isUnlocked) return true
        if (profile.credits < spec.priceCredits) return false

        dao.upsertProfile(
            profile.copy(
                credits = profile.credits - spec.priceCredits,
                activeCarId = carId
            )
        )
        dao.upsertCar(car.copy(isUnlocked = true))
        return true
    }

    suspend fun updateCarVisuals(
        carId: String,
        paintHex: String? = null,
        rimStyle: String? = null,
        spoilerStyle: String? = null
    ) {
        val car = dao.getCarOnce(carId) ?: return
        dao.upsertCar(
            car.copy(
                paintHex = paintHex ?: car.paintHex,
                rimStyle = rimStyle ?: car.rimStyle,
                spoilerStyle = spoilerStyle ?: car.spoilerStyle
            )
        )
    }

    suspend fun upgradeCar(carId: String, type: UpgradeType): Boolean {
        val car = dao.getCarOnce(carId) ?: return false
        if (!car.isUnlocked) return false
        val currentLevel = when (type) {
            UpgradeType.ENGINE -> car.engineLevel
            UpgradeType.TURBO -> car.turboLevel
            UpgradeType.GRIP -> car.gripLevel
            UpgradeType.BRAKE -> car.brakeLevel
        }
        if (currentLevel >= 3) return false
        val cost = type.baseCost * (currentLevel + 1)
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        if (profile.credits < cost) return false

        val updatedCar = when (type) {
            UpgradeType.ENGINE -> car.copy(engineLevel = currentLevel + 1)
            UpgradeType.TURBO -> car.copy(turboLevel = currentLevel + 1)
            UpgradeType.GRIP -> car.copy(gripLevel = currentLevel + 1)
            UpgradeType.BRAKE -> car.copy(brakeLevel = currentLevel + 1)
        }
        dao.upsertProfile(profile.copy(credits = profile.credits - cost))
        dao.upsertCar(updatedCar)
        return true
    }

    suspend fun updateSettings(
        driverName: String? = null,
        quality: GraphicsQuality? = null,
        audioEnabled: Boolean? = null,
        masterVolume: Float? = null,
        defaultCamera: CameraMode? = null
    ) {
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(
            profile.copy(
                driverName = driverName?.takeIf { it.isNotBlank() } ?: profile.driverName,
                graphicsQuality = quality?.name ?: profile.graphicsQuality,
                audioEnabled = audioEnabled ?: profile.audioEnabled,
                masterVolume = masterVolume ?: profile.masterVolume,
                defaultCamera = defaultCamera?.name ?: profile.defaultCamera
            )
        )
    }

    suspend fun recordRaceCompletion(
        trackId: String,
        position: Int,
        bestLapMs: Long,
        totalTimeMs: Long,
        mode: RaceMode,
        championshipRound: Int?
    ): Int {
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val positionReward = when (position) {
            1 -> 1800
            2 -> 1100
            3 -> 700
            else -> 400
        }
        val modeBonus = when (mode) {
            RaceMode.CHAMPIONSHIP -> 600
            RaceMode.MULTIPLAYER -> 900
            RaceMode.QUICK_RACE -> 0
        }
        val totalReward = positionReward + modeBonus

        val f1Points = when (position) {
            1 -> 25
            2 -> 18
            3 -> 15
            else -> 12
        }

        var nextRound = profile.championshipRound
        var champPoints = profile.championshipPoints
        var champCompleted = profile.championshipCompleted

        if (mode == RaceMode.CHAMPIONSHIP && championshipRound != null) {
            champPoints += f1Points
            if (championshipRound >= GameCatalog.championshipRounds.size) {
                champCompleted = true
            } else if (championshipRound >= profile.championshipRound) {
                nextRound = championshipRound + 1
            }
        }

        dao.upsertProfile(
            profile.copy(
                credits = profile.credits + totalReward,
                totalRaces = profile.totalRaces + 1,
                totalWins = profile.totalWins + if (position == 1) 1 else 0,
                championshipRound = nextRound,
                championshipPoints = champPoints,
                championshipCompleted = champCompleted
            )
        )

        val existingRecord = dao.getTrackRecordOnce(trackId) ?: TrackRecordEntity(trackId = trackId)
        val newBestLap = if (bestLapMs > 0 && (existingRecord.bestLapMs == 0L || bestLapMs < existingRecord.bestLapMs)) {
            bestLapMs
        } else existingRecord.bestLapMs

        val newBestRace = if (totalTimeMs > 0 && (existingRecord.bestRaceMs == 0L || totalTimeMs < existingRecord.bestRaceMs)) {
            totalTimeMs
        } else existingRecord.bestRaceMs

        val newBestPos = if (existingRecord.bestPosition == 0 || position < existingRecord.bestPosition) {
            position
        } else existingRecord.bestPosition

        dao.upsertTrackRecord(
            existingRecord.copy(
                bestLapMs = newBestLap,
                bestRaceMs = newBestRace,
                bestPosition = newBestPos,
                racesCompleted = existingRecord.racesCompleted + 1
            )
        )

        return totalReward
    }

    suspend fun resetChampionship() {
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(
            profile.copy(
                championshipRound = 1,
                championshipPoints = 0,
                championshipCompleted = false
            )
        )
    }
}
