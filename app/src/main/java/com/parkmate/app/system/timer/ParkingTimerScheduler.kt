package com.parkmate.app.system.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.parkmate.app.core.math.ParkingTimerCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParkingTimerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val EXTRA_ALARM_TYPE = "extra_alarm_type"
        const val EXTRA_ADDRESS = "extra_address"
        const val TYPE_WARNING_10_MIN = "warning_10_min"
        const val TYPE_EXPIRED = "expired"
        private const val REQ_WARNING = 3101
        private const val REQ_EXPIRED = 3102
    }

    private val alarmManager: AlarmManager? by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    }

    fun scheduleTimerAlarms(
        expiryEpochMillis: Long,
        addressLine: String,
        remindTenMinutesBefore: Boolean
    ) {
        cancelTimerAlarms()
        val now = System.currentTimeMillis()
        if (expiryEpochMillis <= now) return

        if (remindTenMinutesBefore) {
            val warningTrigger = expiryEpochMillis - ParkingTimerCalculator.TEN_MINUTES_MILLIS
            if (warningTrigger > now + 5_000L) {
                scheduleSingleAlarm(
                    requestCode = REQ_WARNING,
                    triggerAtMillis = warningTrigger,
                    alarmType = TYPE_WARNING_10_MIN,
                    addressLine = addressLine
                )
            }
        }

        scheduleSingleAlarm(
            requestCode = REQ_EXPIRED,
            triggerAtMillis = expiryEpochMillis,
            alarmType = TYPE_EXPIRED,
            addressLine = addressLine
        )
    }

    fun cancelTimerAlarms() {
        val manager = alarmManager ?: return
        listOf(REQ_WARNING, REQ_EXPIRED).forEach { reqCode ->
            val intent = Intent(context, ParkingTimerAlarmReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                manager.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun scheduleSingleAlarm(
        requestCode: Int,
        triggerAtMillis: Long,
        alarmType: String,
        addressLine: String
    ) {
        val manager = alarmManager ?: return
        val intent = Intent(context, ParkingTimerAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_TYPE, alarmType)
            putExtra(EXTRA_ADDRESS, addressLine)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }

        runCatching {
            if (canExact) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending
                )
            } else {
                // Reliable fallback when SCHEDULE_EXACT_ALARM is not granted
                manager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    60_000L,
                    pending
                )
            }
        }.onFailure {
            runCatching {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending
                )
            }
        }
    }
}
