package com.parkmate.app.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkingTimerCalculatorTest {

    @Test
    fun `calculateState returns inactive when expiry is null`() {
        val state = ParkingTimerCalculator.calculateState(
            nowMillis = 1_000_000L,
            startEpochMillis = null,
            expiryEpochMillis = null
        )
        assertFalse(state.isActive)
        assertFalse(state.isExpired)
        assertFalse(state.isAlmostExpired)
        assertEquals(0, state.remainingMinutes)
    }

    @Test
    fun `calculateState flags almostExpired when 10 minutes or less remain`() {
        val now = 1_000_000L
        val start = now - 50 * 60_000L
        val expiry = now + 8 * 60_000L

        val state = ParkingTimerCalculator.calculateState(now, start, expiry)
        assertTrue(state.isActive)
        assertTrue(state.isAlmostExpired)
        assertFalse(state.isExpired)
        assertEquals(8, state.remainingMinutes)
    }

    @Test
    fun `calculateState flags expired when nowMillis reaches or passes expiry`() {
        val now = 2_000_000L
        val start = now - 30 * 60_000L
        val expiry = now - 1_000L

        val state = ParkingTimerCalculator.calculateState(now, start, expiry)
        assertFalse(state.isActive)
        assertTrue(state.isExpired)
        assertFalse(state.isAlmostExpired)
        assertEquals(0, state.remainingMinutes)
        assertEquals(0f, state.progressFraction, 0.001f)
    }

    @Test
    fun `computeNewExpiry extends active timer or starts from now if expired`() {
        val now = 5_000_000L
        val activeExpiry = now + 10 * 60_000L

        val (_, extendedFromActive) = ParkingTimerCalculator.computeNewExpiry(
            nowMillis = now,
            currentExpiryMillis = activeExpiry,
            additionalMinutes = 15
        )
        assertEquals(now + 25 * 60_000L, extendedFromActive)

        val pastExpiry = now - 5 * 60_000L
        val (_, extendedFromExpired) = ParkingTimerCalculator.computeNewExpiry(
            nowMillis = now,
            currentExpiryMillis = pastExpiry,
            additionalMinutes = 30
        )
        assertEquals(now + 30 * 60_000L, extendedFromExpired)
    }
}
