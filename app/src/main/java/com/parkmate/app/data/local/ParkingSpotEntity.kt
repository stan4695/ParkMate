package com.parkmate.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.parkmate.app.data.model.ParkingSpot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "parking_spots")
data class ParkingSpotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val latitude: Double,
    val longitude: Double,
    val addressLine: String,
    val cityDistrict: String,
    val savedAtMillis: Long,
    val isActive: Boolean,
    val tagsJson: String,
    val note: String,
    val photoPathsJson: String,
    val timerStartMillis: Long?,
    val timerExpiryMillis: Long?,
    val timerRemindBeforeExpiry: Boolean
)

object SpotJsonCodec {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun encodeList(items: List<String>): String = json.encodeToString(items)

    @TypeConverter
    fun decodeList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }
}

fun ParkingSpotEntity.toDomain(): ParkingSpot = ParkingSpot(
    id = id,
    latitude = latitude,
    longitude = longitude,
    addressLine = addressLine,
    cityDistrict = cityDistrict,
    savedAtMillis = savedAtMillis,
    isActive = isActive,
    tags = SpotJsonCodec.decodeList(tagsJson),
    note = note,
    photoPaths = SpotJsonCodec.decodeList(photoPathsJson),
    timerStartMillis = timerStartMillis,
    timerExpiryMillis = timerExpiryMillis,
    timerRemindBeforeExpiry = timerRemindBeforeExpiry
)

fun ParkingSpot.toEntity(): ParkingSpotEntity = ParkingSpotEntity(
    id = id,
    latitude = latitude,
    longitude = longitude,
    addressLine = addressLine,
    cityDistrict = cityDistrict,
    savedAtMillis = savedAtMillis,
    isActive = isActive,
    tagsJson = SpotJsonCodec.encodeList(tags),
    note = note.take(200),
    photoPathsJson = SpotJsonCodec.encodeList(photoPaths.take(4)),
    timerStartMillis = timerStartMillis,
    timerExpiryMillis = timerExpiryMillis,
    timerRemindBeforeExpiry = timerRemindBeforeExpiry
)
