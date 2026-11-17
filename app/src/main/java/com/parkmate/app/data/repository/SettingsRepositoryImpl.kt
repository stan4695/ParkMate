package com.parkmate.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.parkmate.app.data.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "parkmate_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val CAR_BT_NAME = stringPreferencesKey("car_bt_name")
        val CAR_BT_ADDRESS = stringPreferencesKey("car_bt_address")
        val AUTO_SAVE = booleanPreferencesKey("auto_save_on_disconnect")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }

    override val userSettings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val savedAddress = prefs[Keys.CAR_BT_ADDRESS].orEmpty()
        val savedName = if (savedAddress.isBlank()) "" else prefs[Keys.CAR_BT_NAME].orEmpty()
        UserSettings(
            carBluetoothName = savedName,
            carBluetoothAddress = savedAddress,
            autoSaveOnDisconnect = prefs[Keys.AUTO_SAVE] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true
        )
    }

    override suspend fun getSettingsOnce(): UserSettings = userSettings.first()

    override suspend fun setCarBluetoothDevice(name: String, address: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CAR_BT_NAME] = name
            prefs[Keys.CAR_BT_ADDRESS] = address
        }
    }

    override suspend fun setAutoSaveOnDisconnect(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SAVE] = enabled
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS] = enabled
        }
    }
}
