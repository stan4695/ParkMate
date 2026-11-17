package com.parkmate.app.core.math

import kotlin.math.ceil
import kotlin.math.max

data class TimerProgressState(
    val isActive: Boolean,
    val isExpired: Boolean,
    val isAlmostExpired: Boolean,
    val remainingMinutes: Int,
    val remainingMillis: Long,
    val progressFraction: Float,
    val expiryEpochMillis: Long?
)

object ParkingTimerCalculator {
    const val TEN_MINUTES_MILLIS = 10 * 60 * 1000L
    const val ONE_MINUTE_MILLIS = 60 * 1000L

    fun calculateState(
        nowMillis: Long,
        startEpochMillis: Long?,
        expiryEpochMillis: Long?
    ): TimerProgressState {
        if (expiryEpochMillis == null || expiryEpochMillis <= 0L) {
            return TimerProgressState(
                isActive = false,
                isExpired = false,
                isAlmostExpired = false,
                remainingMinutes = 0,
                remainingMillis = 0L,
                progressFraction = 0f,
                expiryEpochMillis = null
            )
        }

        val effectiveStart = startEpochMillis?.takeIf { it in 1 until expiryEpochMillis }
            ?: (expiryEpochMillis - 60 * ONE_MINUTE_MILLIS)
        val totalDuration = max(ONE_MINUTE_MILLIS, expiryEpochMillis - effectiveStart)
        val remainingMillis = max(0L, expiryEpochMillis - nowMillis)
        val remainingMinutes = ceil(remainingMillis.toDouble() / ONE_MINUTE_MILLIS.toDouble()).toInt()
        val isExpired = remainingMillis == 0L
        val isAlmostExpired = !isExpired && remainingMillis <= TEN_MINUTES_MILLIS
        val fraction = (remainingMillis.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

        return TimerProgressState(
            isActive = !isExpired,
            isExpired = isExpired,
            isAlmostExpired = isAlmostExpired,
            remainingMinutes = remainingMinutes,
            remainingMillis = remainingMillis,
            progressFraction = fraction,
            expiryEpochMillis = expiryEpochMillis
        )
    }

    /**
     * Extends an existing timer (or starts a new one from [nowMillis] if expired or null) by [additionalMinutes].
     */
    fun computeNewExpiry(
        nowMillis: Long,
        currentExpiryMillis: Long?,
        additionalMinutes: Int
    ): Pair<Long, Long> {
        val baseTime = if (currentExpiryMillis != null && currentExpiryMillis > nowMillis) {
            currentExpiryMillis
        } else {
            nowMillis
        }
        val newExpiry = baseTime + additionalMinutes * ONE_MINUTE_MILLIS
        return Pair(nowMillis, newExpiry)
    }
}
