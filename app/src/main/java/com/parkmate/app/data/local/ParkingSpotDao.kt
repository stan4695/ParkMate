package com.parkmate.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingSpotDao {

    @Query("SELECT * FROM parking_spots WHERE isActive = 1 ORDER BY savedAtMillis DESC LIMIT 1")
    fun observeActiveSpot(): Flow<ParkingSpotEntity?>

    @Query("SELECT * FROM parking_spots WHERE isActive = 1 ORDER BY savedAtMillis DESC LIMIT 1")
    suspend fun getActiveSpotOnce(): ParkingSpotEntity?

    @Query("SELECT * FROM parking_spots ORDER BY savedAtMillis DESC")
    fun observeAllSpots(): Flow<List<ParkingSpotEntity>>

    @Query("SELECT * FROM parking_spots WHERE id = :spotId LIMIT 1")
    fun observeSpotById(spotId: Long): Flow<ParkingSpotEntity?>

    @Query("SELECT * FROM parking_spots WHERE id = :spotId LIMIT 1")
    suspend fun getSpotByIdOnce(spotId: Long): ParkingSpotEntity?

    @Query("UPDATE parking_spots SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllSpots()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(entity: ParkingSpotEntity): Long

    @Update
    suspend fun updateSpot(entity: ParkingSpotEntity)

    @Query("DELETE FROM parking_spots WHERE id = :spotId")
    suspend fun deleteSpotById(spotId: Long)

    @Query("DELETE FROM parking_spots")
    suspend fun deleteAllSpots()

    @Transaction
    suspend fun saveNewActiveSpot(entity: ParkingSpotEntity): Long {
        deactivateAllSpots()
        return insertSpot(entity.copy(isActive = true))
    }
}
