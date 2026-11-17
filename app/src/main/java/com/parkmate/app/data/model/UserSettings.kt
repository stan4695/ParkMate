package com.parkmate.app.data.model

data class UserSettings(
    val carBluetoothName: String = "",
    val carBluetoothAddress: String = "",
    val autoSaveOnDisconnect: Boolean = false,
    val notificationsEnabled: Boolean = true
)
