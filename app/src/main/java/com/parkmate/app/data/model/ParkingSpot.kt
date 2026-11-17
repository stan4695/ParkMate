package com.parkmate.app.data.model

data class ParkingSpot(
    val id: Long = 0L,
    val latitude: Double,
    val longitude: Double,
    val addressLine: String,
    val cityDistrict: String,
    val savedAtMillis: Long,
    val isActive: Boolean,
    val tags: List<String> = emptyList(),
    val note: String = "",
    val photoPaths: List<String> = emptyList(),
    val timerStartMillis: Long? = null,
    val timerExpiryMillis: Long? = null,
    val timerRemindBeforeExpiry: Boolean = true
)
