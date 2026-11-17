package com.parkmate.app.data.repository

import com.parkmate.app.data.model.ParkingSpot
import kotlinx.coroutines.flow.Flow

interface ParkingRepository {
    fun observeActiveSpot(): Flow<ParkingSpot?>
    suspend fun getActiveSpotOnce(): ParkingSpot?
    fun observeAllSpots(): Flow<List<ParkingSpot>>
    fun observeSpotById(spotId: Long): Flow<ParkingSpot?>
    suspend fun getSpotByIdOnce(spotId: Long): ParkingSpot?
    suspend fun saveNewSpot(spot: ParkingSpot): Long
    suspend fun updateSpot(spot: ParkingSpot)
    suspend fun deleteSpot(spotId: Long)
    suspend fun clearAllSpots()
    suspend fun seedSampleSpots()
}
