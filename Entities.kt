package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val driverName: String = "Alex Mercer",
    val credits: Int = 2000,
    val totalRaces: Int = 0,
    val totalWins: Int = 0,
    val activeCarId: String = "apex_v8",
    val graphicsQuality: String = "HIGH",
    val audioEnabled: Boolean = true,
    val masterVolume: Float = 0.85f,
    val defaultCamera: String = "CHASE",
    val championshipRound: Int = 1,
    val championshipPoints: Int = 0,
    val championshipCompleted: Boolean = false
)

@Entity(tableName = "car_customizations")
data class CarCustomizationEntity(
    @PrimaryKey val carId: String,
    val isUnlocked: Boolean,
    val paintHex: String,
    val rimStyle: String,
    val spoilerStyle: String,
    val engineLevel: Int = 0,
    val turboLevel: Int = 0,
    val gripLevel: Int = 0,
    val brakeLevel: Int = 0
)

@Entity(tableName = "track_records")
data class TrackRecordEntity(
    @PrimaryKey val trackId: String,
    val bestLapMs: Long = 0L,
    val bestRaceMs: Long = 0L,
    val bestPosition: Int = 0,
    val racesCompleted: Int = 0
)
