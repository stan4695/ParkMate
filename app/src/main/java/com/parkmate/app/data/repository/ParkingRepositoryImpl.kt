package com.parkmate.app.data.repository

import com.parkmate.app.data.local.ParkingSpotDao
import com.parkmate.app.data.local.toDomain
import com.parkmate.app.data.local.toEntity
import com.parkmate.app.data.model.ParkingSpot
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class ParkingRepositoryImpl @Inject constructor(
    private val dao: ParkingSpotDao
) : ParkingRepository {

    override fun observeActiveSpot(): Flow<ParkingSpot?> =
        dao.observeActiveSpot().map { it?.toDomain() }

    override suspend fun getActiveSpotOnce(): ParkingSpot? =
        dao.getActiveSpotOnce()?.toDomain()

    override fun observeAllSpots(): Flow<List<ParkingSpot>> =
        dao.observeAllSpots().map { list -> list.map { it.toDomain() } }

    override fun observeSpotById(spotId: Long): Flow<ParkingSpot?> =
        dao.observeSpotById(spotId).map { it?.toDomain() }

    override suspend fun getSpotByIdOnce(spotId: Long): ParkingSpot? =
        dao.getSpotByIdOnce(spotId)?.toDomain()

    override suspend fun saveNewSpot(spot: ParkingSpot): Long =
        dao.saveNewActiveSpot(spot.toEntity())

    override suspend fun updateSpot(spot: ParkingSpot) {
        dao.updateSpot(spot.toEntity())
    }

    override suspend fun deleteSpot(spotId: Long) {
        val existing = dao.getSpotByIdOnce(spotId)?.toDomain()
        dao.deleteSpotById(spotId)
        existing?.photoPaths?.forEach { path ->
            withContext(Dispatchers.IO) {
                runCatching { File(path).delete() }
            }
        }
    }

    override suspend fun clearAllSpots() {
        dao.deleteAllSpots()
    }

    override suspend fun seedSampleSpots() {
        val now = System.currentTimeMillis()
        val samples = listOf(
            ParkingSpot(
                latitude = 44.4396,
                longitude = 26.0963,
                addressLine = "Piața Amzei 4",
                cityDistrict = "Bucharest, Sector 1",
                savedAtMillis = now - 3 * 24 * 60 * 60 * 1000L,
                isActive = false,
                tags = listOf("Zone A", "Near exit"),
                note = "Across from the bakery entrance"
            ),
            ParkingSpot(
                latitude = 44.4378,
                longitude = 26.1195,
                addressLine = "Strada Plantelor 8",
                cityDistrict = "Bucharest, Sector 2",
                savedAtMillis = now - 24 * 60 * 60 * 1000L,
                isActive = false,
                tags = listOf("Zone B"),
                note = "Street parking under linden tree"
            ),
            ParkingSpot(
                latitude = 44.4449,
                longitude = 26.0908,
                addressLine = "Calea Victoriei 102",
                cityDistrict = "Bucharest, Sector 1",
                savedAtMillis = now - 6 * 60 * 60 * 1000L,
                isActive = false,
                tags = listOf("Level -1", "Row 14"),
                note = "Underground garage near north ramp"
            ),
            ParkingSpot(
                latitude = 44.4412,
                longitude = 26.0979,
                addressLine = "Strada Exemplu 12",
                cityDistrict = "Bucharest, Sector 1",
                savedAtMillis = now - (2 * 60 + 14) * 60 * 1000L,
                isActive = true,
                tags = listOf("Level -2", "Zone B", "Pillar C4"),
                note = "Near the elevator, row 4",
                timerStartMillis = now - 42 * 60 * 1000L,
                timerExpiryMillis = now + 18 * 60 * 1000L,
                timerRemindBeforeExpiry = true
            )
        )
        dao.deactivateAllSpots()
        samples.forEach { spot ->
            dao.insertSpot(spot.toEntity())
        }
    }
}
