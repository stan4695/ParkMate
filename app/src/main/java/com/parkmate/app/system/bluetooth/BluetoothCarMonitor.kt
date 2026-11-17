package com.parkmate.app.system.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class BondedBluetoothDevice(
    val name: String,
    val address: String
)

@Singleton
class BluetoothCarMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun getBondedDevices(): List<BondedBluetoothDevice> {
        if (!hasBluetoothConnectPermission()) return emptyList()
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return emptyList()
        val adapter = btManager.adapter ?: return emptyList()
        val bonded = runCatching { adapter.bondedDevices }.getOrNull().orEmpty()
        return bonded.map { device ->
            BondedBluetoothDevice(
                name = device.name?.takeIf { it.isNotBlank() } ?: device.address,
                address = device.address
            )
        }.sortedBy { it.name.lowercase() }
    }
}
