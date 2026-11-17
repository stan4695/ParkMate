package com.parkmate.app.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationMathTest {

    @Test
    fun `distanceMeters returns zero for identical coordinates`() {
        val distance = LocationMath.distanceMeters(44.4412, 26.0979, 44.4412, 26.0979)
        assertEquals(0.0, distance, 0.01)
    }

    @Test
    fun `distanceMeters calculates accurate Haversine distance in Bucharest`() {
        // ~320m apart in central Bucharest
        val dist = LocationMath.distanceMeters(
            startLat = 44.4389,
            startLng = 26.0958,
            endLat = 44.4412,
            endLng = 26.0979
        )
        assertTrue("Expected ~300-330m but got $dist", dist in 290.0..340.0)
    }

    @Test
    fun `initialBearingDegrees computes cardinal bearings accurately`() {
        val dueNorth = LocationMath.initialBearingDegrees(44.0, 26.0, 45.0, 26.0)
        val dueEast = LocationMath.initialBearingDegrees(44.0, 26.0, 44.0, 27.0)
        val dueSouth = LocationMath.initialBearingDegrees(44.0, 26.0, 43.0, 26.0)
        val dueWest = LocationMath.initialBearingDegrees(44.0, 26.0, 44.0, 25.0)

        assertEquals(0f, dueNorth, 1.0f)
        assertEquals(90f, dueEast, 1.5f)
        assertEquals(180f, dueSouth, 1.0f)
        assertEquals(270f, dueWest, 1.5f)
    }

    @Test
    fun `relativeRotationDegrees normalizes wrap-around angles to -180_180`() {
        assertEquals(45f, LocationMath.relativeRotationDegrees(targetBearing = 30f, deviceAzimuth = 345f), 0.01f)
        assertEquals(-40f, LocationMath.relativeRotationDegrees(targetBearing = 350f, deviceAzimuth = 30f), 0.01f)
    }

    @Test
    fun `walkingTimeMinutes returns at least 1 minute and scales at 80m per min`() {
        assertEquals(1, LocationMath.walkingTimeMinutes(5.0))
        assertEquals(4, LocationMath.walkingTimeMinutes(320.0))
        assertEquals(10, LocationMath.walkingTimeMinutes(800.0))
    }

    @Test
    fun `cardinalDirection maps degrees to 8-wind compass points`() {
        assertEquals("N", LocationMath.cardinalDirection(0f))
        assertEquals("NE", LocationMath.cardinalDirection(42f))
        assertEquals("E", LocationMath.cardinalDirection(90f))
        assertEquals("SW", LocationMath.cardinalDirection(225f))
        assertEquals("NW", LocationMath.cardinalDirection(315f))
    }
}
