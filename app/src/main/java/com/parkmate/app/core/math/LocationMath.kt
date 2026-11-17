package com.parkmate.app.core.math

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object LocationMath {
    private const val EARTH_RADIUS_METERS = 6371000.0
    private const val WALKING_SPEED_METERS_PER_MIN = 80.0 // ~4.8 km/h standard walking speed

    /**
     * Computes great-circle distance in meters between two WGS84 coordinates using the Haversine formula.
     */
    fun distanceMeters(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Double {
        if (startLat == endLat && startLng == endLng) return 0.0
        val dLat = Math.toRadians(endLat - startLat)
        val dLng = Math.toRadians(endLng - startLng)
        val lat1 = Math.toRadians(startLat)
        val lat2 = Math.toRadians(endLat)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                sin(dLng / 2) * sin(dLng / 2) * cos(lat1) * cos(lat2)
        val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Computes initial forward azimuth (bearing) in degrees [0, 360) from start to end coordinate.
     */
    fun initialBearingDegrees(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Float {
        if (startLat == endLat && startLng == endLng) return 0f
        val lat1 = Math.toRadians(startLat)
        val lat2 = Math.toRadians(endLat)
        val dLng = Math.toRadians(endLng - startLng)

        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val theta = Math.toDegrees(atan2(y, x))
        return ((theta + 360.0) % 360.0).toFloat()
    }

    /**
     * Computes the relative clockwise arrow rotation in degrees [-180, 180] given target bearing and device azimuth.
     */
    fun relativeRotationDegrees(targetBearing: Float, deviceAzimuth: Float): Float {
        var diff = (targetBearing - deviceAzimuth) % 360f
        if (diff < -180f) diff += 360f
        if (diff > 180f) diff -= 360f
        return diff
    }

    /**
     * Estimates walking time in minutes (at least 1 minute when distance > 15m).
     */
    fun walkingTimeMinutes(distanceMeters: Double): Int {
        if (distanceMeters <= 15.0) return 1
        return max(1, (distanceMeters / WALKING_SPEED_METERS_PER_MIN).roundToInt())
    }

    /**
     * Converts a bearing in degrees [0, 360) to an 8-wind cardinal direction string.
     */
    fun cardinalDirection(bearingDegrees: Float): String {
        val normalized = ((bearingDegrees % 360f) + 360f) % 360f
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((normalized + 22.5f) / 45f).toInt() % 8
        return directions[index]
    }
}
