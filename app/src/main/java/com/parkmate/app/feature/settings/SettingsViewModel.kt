package com.parkmate.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkmate.app.data.model.UserSettings
import com.parkmate.app.data.repository.SettingsRepository
import com.parkmate.app.system.bluetooth.BluetoothCarMonitor
import com.parkmate.app.system.bluetooth.BondedBluetoothDevice
import com.parkmate.app.system.location.LocationClientHelper
import com.parkmate.app.system.notification.ParkMateNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AutoSavePermissionStep {
    NONE,
    FOREGROUND_LOCATION,
    BACKGROUND_LOCATION,
    BLUETOOTH_CONNECT
}

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val hasForegroundLocation: Boolean = false,
    val hasBackgroundLocation: Boolean = false,
    val hasBluetoothConnect: Boolean = false,
    val hasNotificationsPermission: Boolean = false,
    val bondedDevices: List<BondedBluetoothDevice> = emptyList(),
    val showDevicePickerDialog: Boolean = false,
    val permissionRationaleStep: AutoSavePermissionStep = AutoSavePermissionStep.NONE
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationClientHelper: LocationClientHelper,
    private val bluetoothCarMonitor: BluetoothCarMonitor,
    private val notificationHelper: ParkMateNotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.userSettings.collectLatest { userSettings ->
                _uiState.update { it.copy(settings = userSettings) }
            }
        }
        refreshPermissionsAndDevices()
    }

    fun refreshPermissionsAndDevices() {
        val fg = locationClientHelper.hasForegroundLocationPermission()
        val bg = locationClientHelper.hasBackgroundLocationPermission()
        val bt = bluetoothCarMonitor.hasBluetoothConnectPermission()
        val notif = notificationHelper.hasNotificationPermission()
        val devices = if (bt) bluetoothCarMonitor.getBondedDevices() else emptyList()

        _uiState.update {
            it.copy(
                hasForegroundLocation = fg,
                hasBackgroundLocation = bg,
                hasBluetoothConnect = bt,
                hasNotificationsPermission = notif,
                bondedDevices = devices
            )
        }
    }

    /**
     * Enforces sequential permission requests for Auto-Save:
     * 1) Foreground location -> 2) Background location -> 3) BLUETOOTH_CONNECT.
     */
    fun onAutoSaveToggleRequested(enable: Boolean) {
        if (!enable) {
            viewModelScope.launch {
                settingsRepository.setAutoSaveOnDisconnect(false)
            }
            return
        }

        refreshPermissionsAndDevices()
        val current = _uiState.value
        when {
            !current.hasForegroundLocation -> {
                _uiState.update { it.copy(permissionRationaleStep = AutoSavePermissionStep.FOREGROUND_LOCATION) }
            }
            !current.hasBackgroundLocation -> {
                _uiState.update { it.copy(permissionRationaleStep = AutoSavePermissionStep.BACKGROUND_LOCATION) }
            }
            !current.hasBluetoothConnect -> {
                _uiState.update { it.copy(permissionRationaleStep = AutoSavePermissionStep.BLUETOOTH_CONNECT) }
            }
            else -> {
                viewModelScope.launch {
                    settingsRepository.setAutoSaveOnDisconnect(true)
                }
            }
        }
    }

    fun advanceOrFinishAutoSaveFlow() {
        refreshPermissionsAndDevices()
        val current = _uiState.value
        when {
            !current.hasForegroundLocation -> {
                _uiState.update { it.copy(permissionRationaleStep = AutoSavePermissionStep.NONE) }
            }
            !current.hasBackgroundLocation -> {
                _uiState.update { it.copy(permissionRationaleStep = AutoSavePermissionStep.BACKGROUND_LOCATION) }
            }
            !current.hasBluetoothConnect -> {
                _uiState.update { it.copy(permissionRationaleStep = AutoSavePermissionStep.BLUETOOTH_CONNECT) }
            }
            else -> {
                _uiState.update { it.copy(permissionRationaleStep = AutoSavePermissionStep.NONE) }
                viewModelScope.launch {
                    settingsRepository.setAutoSaveOnDisconnect(true)
                }
            }
        }
    }

    fun dismissRationale() {
        _uiState.update { it.copy(permissionRationaleStep = AutoSavePermissionStep.NONE) }
    }

    fun openBluetoothDevicePicker() {
        refreshPermissionsAndDevices()
        if (!_uiState.value.hasBluetoothConnect) {
            _uiState.update { it.copy(permissionRationaleStep = AutoSavePermissionStep.BLUETOOTH_CONNECT) }
        } else {
            _uiState.update {
                it.copy(
                    bondedDevices = bluetoothCarMonitor.getBondedDevices(),
                    showDevicePickerDialog = true
                )
            }
        }
    }

    fun dismissDevicePicker() {
        _uiState.update { it.copy(showDevicePickerDialog = false) }
    }

    fun selectCarBluetoothDevice(device: BondedBluetoothDevice) {
        viewModelScope.launch {
            settingsRepository.setCarBluetoothDevice(device.name, device.address)
            _uiState.update { it.copy(showDevicePickerDialog = false) }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }
}
