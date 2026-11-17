package com.parkmate.app.system.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.parkmate.app.data.repository.SettingsRepository
import com.parkmate.app.system.notification.ParkMateNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ParkingTimerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: ParkMateNotificationHelper

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val alarmType = intent.getStringExtra(ParkingTimerScheduler.EXTRA_ALARM_TYPE) ?: return
        val address = intent.getStringExtra(ParkingTimerScheduler.EXTRA_ADDRESS) ?: "Saved Spot"

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = settingsRepository.getSettingsOnce()
                if (settings.notificationsEnabled) {
                    when (alarmType) {
                        ParkingTimerScheduler.TYPE_WARNING_10_MIN -> {
                            notificationHelper.showTimerWarningNotification(address)
                        }
                        ParkingTimerScheduler.TYPE_EXPIRED -> {
                            notificationHelper.showTimerExpiredNotification(address)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
