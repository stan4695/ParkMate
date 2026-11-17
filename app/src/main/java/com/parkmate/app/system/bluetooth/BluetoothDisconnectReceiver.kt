package com.parkmate.app.system.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.parkmate.app.data.model.ParkingSpot
import com.parkmate.app.data.repository.ParkingRepository
import com.parkmate.app.data.repository.SettingsRepository
import com.parkmate.app.system.location.LocationClientHelper
import com.parkmate.app.system.location.LocationReverseGeocoder
import com.parkmate.app.system.notification.ParkMateNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BluetoothDisconnectReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SIMULATE_DISCONNECT = "com.parkmate.app.ACTION_SIMULATE_BT_DISCONNECT"
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var parkingRepository: ParkingRepository

    @Inject
    lateinit var locationClientHelper: LocationClientHelper

    @Inject
    lateinit var reverseGeocoder: LocationReverseGeocoder

    @Inject
    lateinit var notificationHelper: ParkMateNotificationHelper

    @Inject
    lateinit var bluetoothCarMonitor: BluetoothCarMonitor

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != BluetoothDevice.ACTION_ACL_DISCONNECTED && action != ACTION_SIMULATE_DISCONNECT) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = settingsRepository.getSettingsOnce()
                if (!settings.autoSaveOnDisconnect) return@launch

                if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    if (device != null && bluetoothCarMonitor.hasBluetoothConnectPermission()) {
                        val disconnectedAddress = runCatching { device.address }.getOrDefault("")
                        val disconnectedName = runCatching { device.name }.getOrDefault("")
                        val matchesAddress = settings.carBluetoothAddress.isNotBlank() &&
                                settings.carBluetoothAddress.equals(disconnectedAddress, ignoreCase = true)
                        val matchesName = settings.carBluetoothName.isNotBlank() &&
                                settings.carBluetoothName.equals(disconnectedName, ignoreCase = true)

                        if (!matchesAddress && !matchesName) {
                            return@launch
                        }
                    }
                }

                val coordinate = locationClientHelper.getCurrentHighAccuracyLocation()
                val resolved = reverseGeocoder.resolveAddress(coordinate.latitude, coordinate.longitude)
                val spot = ParkingSpot(
                    latitude = coordinate.latitude,
                    longitude = coordinate.longitude,
                    addressLine = resolved.addressLine,
                    cityDistrict = resolved.cityDistrict,
                    savedAtMillis = System.currentTimeMillis(),
                    isActive = true,
                    note = if (settings.carBluetoothName.isNotBlank()) {
                        "Auto-saved (${settings.carBluetoothName})"
                    } else {
                        "Auto-saved on Bluetooth disconnect"
                    }
                )
                parkingRepository.saveNewSpot(spot)
                if (settings.notificationsEnabled) {
                    notificationHelper.showAutoSavedNotification(resolved.addressLine)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
