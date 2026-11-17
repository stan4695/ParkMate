package com.parkmate.app.data.repository

import com.parkmate.app.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val userSettings: Flow<UserSettings>
    suspend fun getSettingsOnce(): UserSettings
    suspend fun setCarBluetoothDevice(name: String, address: String)
    suspend fun setAutoSaveOnDisconnect(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
