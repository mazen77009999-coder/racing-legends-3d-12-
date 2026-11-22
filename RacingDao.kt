package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RacingDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM car_customizations")
    fun observeAllCars(): Flow<List<CarCustomizationEntity>>

    @Query("SELECT * FROM car_customizations WHERE carId = :carId")
    suspend fun getCarOnce(carId: String): CarCustomizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCar(car: CarCustomizationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCarsIfAbsent(cars: List<CarCustomizationEntity>)

    @Query("SELECT * FROM track_records")
    fun observeTrackRecords(): Flow<List<TrackRecordEntity>>

    @Query("SELECT * FROM track_records WHERE trackId = :trackId")
    suspend fun getTrackRecordOnce(trackId: String): TrackRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackRecord(record: TrackRecordEntity)
}
