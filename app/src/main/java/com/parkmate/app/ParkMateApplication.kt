package com.parkmate.app

import android.app.Application
import com.parkmate.app.system.notification.ParkMateNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ParkMateApplication : Application() {

    @Inject
    lateinit var notificationHelper: ParkMateNotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper.ensureChannelsCreated()
    }
}
