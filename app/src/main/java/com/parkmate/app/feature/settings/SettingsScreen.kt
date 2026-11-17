package com.parkmate.app.feature.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkmate.app.R
import com.parkmate.app.core.ui.components.ParkMateSettingRow
import com.parkmate.app.core.ui.components.ParkMateTextButton
import com.parkmate.app.core.ui.components.ParkMateTopBar
import com.parkmate.app.core.ui.theme.ParkMateThemeTokens

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ParkMateThemeTokens.colors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsAndDevices()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val fgLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.advanceOrFinishAutoSaveFlow()
    }

    val bgLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.advanceOrFinishAutoSaveFlow()
    }

    val btConnectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.advanceOrFinishAutoSaveFlow()
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionsAndDevices()
    }

    // In-App Rationale Dialogs for sequential Auto-Save permissions
    if (state.permissionRationaleStep != AutoSavePermissionStep.NONE) {
        val messageRes = when (state.permissionRationaleStep) {
            AutoSavePermissionStep.FOREGROUND_LOCATION -> R.string.bt_rationale_fg_location
            AutoSavePermissionStep.BACKGROUND_LOCATION -> R.string.bt_rationale_bg_location
            AutoSavePermissionStep.BLUETOOTH_CONNECT -> R.string.bt_rationale_bluetooth
            AutoSavePermissionStep.NONE -> R.string.bt_rationale_fg_location
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissRationale() },
            title = { Text(stringResource(R.string.bt_rationale_title)) },
            text = { Text(stringResource(messageRes)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (state.permissionRationaleStep) {
                            AutoSavePermissionStep.FOREGROUND_LOCATION -> {
                                fgLocationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            AutoSavePermissionStep.BACKGROUND_LOCATION -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                } else {
                                    viewModel.advanceOrFinishAutoSaveFlow()
                                }
                            }
                            AutoSavePermissionStep.BLUETOOTH_CONNECT -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    btConnectLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                } else {
                                    viewModel.advanceOrFinishAutoSaveFlow()
                                }
                            }
                            AutoSavePermissionStep.NONE -> Unit
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_allow), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.dismissRationale()
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                    ) {
                        Text(stringResource(R.string.action_open_settings))
                    }
                    TextButton(onClick = { viewModel.dismissRationale() }) {
                        Text(stringResource(R.string.action_not_now))
                    }
                }
            }
        )
    }

    // Bonded Bluetooth Device Picker Dialog (only real paired devices)
    if (state.showDevicePickerDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDevicePicker() },
            title = { Text(stringResource(R.string.bt_picker_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.bondedDevices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.bt_picker_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted
                        )
                    } else {
                        state.bondedDevices.forEach { device ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.bg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.selectCarBluetoothDevice(device) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bluetooth,
                                        contentDescription = null,
                                        tint = colors.blue
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.text
                                        )
                                        Text(
                                            text = device.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.muted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDevicePicker() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
    ) {
        ParkMateTopBar(
            title = stringResource(R.string.settings_title),
            eyebrow = stringResource(R.string.settings_eyebrow)
        )

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // CONNECTED CAR section
            Text(
                text = stringResource(R.string.settings_section_car),
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 10.dp)
            )

            val hasCarSelected = state.settings.carBluetoothName.isNotBlank() &&
                    state.settings.carBluetoothAddress.isNotBlank()

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 13.dp)) {
                    ParkMateSettingRow(
                        icon = Icons.Rounded.Bluetooth,
                        title = if (hasCarSelected) {
                            state.settings.carBluetoothName
                        } else {
                            stringResource(R.string.settings_no_car_selected)
                        },
                        subtitle = if (hasCarSelected) {
                            "${stringResource(R.string.settings_car_connected_sub)} (${state.settings.carBluetoothAddress})"
                        } else {
                            stringResource(R.string.settings_car_tap_to_choose)
                        },
                        onClick = { viewModel.openBluetoothDevicePicker() },
                        trailing = if (hasCarSelected) {
                            {
                                Box(
                                    modifier = Modifier
                                        .size(25.dp)
                                        .clip(CircleShape)
                                        .background(colors.blue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            null
                        }
                    )
                    HorizontalDivider(
                        color = colors.line,
                        modifier = Modifier.padding(start = 51.dp)
                    )
                    ParkMateSettingRow(
                        icon = Icons.Rounded.LocationOn,
                        title = stringResource(R.string.settings_auto_save_title),
                        subtitle = stringResource(R.string.settings_auto_save_sub),
                        checked = state.settings.autoSaveOnDisconnect,
                        onCheckedChange = { viewModel.onAutoSaveToggleRequested(it) }
                    )
                }
            }

            ParkMateTextButton(
                text = stringResource(R.string.settings_choose_device),
                onClick = { viewModel.openBluetoothDevicePicker() },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // PREFERENCES section
            Text(
                text = stringResource(R.string.settings_section_preferences),
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 10.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 13.dp)) {
                    ParkMateSettingRow(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.settings_notifications_title),
                        subtitle = stringResource(R.string.settings_notifications_sub),
                        checked = state.settings.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.hasNotificationsPermission) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.setNotificationsEnabled(enabled)
                        }
                    )
                    HorizontalDivider(
                        color = colors.line,
                        modifier = Modifier.padding(start = 51.dp)
                    )
                    ParkMateSettingRow(
                        icon = Icons.Rounded.MyLocation,
                        title = stringResource(R.string.settings_location_access_title),
                        subtitle = when {
                            state.hasBackgroundLocation -> stringResource(R.string.settings_location_allowed_always)
                            state.hasForegroundLocation -> stringResource(R.string.settings_location_allowed_foreground)
                            else -> stringResource(R.string.settings_location_not_allowed)
                        },
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        },
                        trailing = {
                            Text(
                                text = when {
                                    state.hasBackgroundLocation -> stringResource(R.string.settings_status_allowed)
                                    state.hasForegroundLocation -> stringResource(R.string.settings_status_limited)
                                    else -> stringResource(R.string.settings_status_denied)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.hasForegroundLocation) colors.statusGreenText else colors.blue
                            )
                        }
                    )
                    HorizontalDivider(
                        color = colors.line,
                        modifier = Modifier.padding(start = 51.dp)
                    )
                    ParkMateSettingRow(
                        icon = Icons.Rounded.Bluetooth,
                        title = stringResource(R.string.settings_bluetooth_access_title),
                        subtitle = if (state.hasBluetoothConnect) {
                            stringResource(R.string.settings_bluetooth_allowed)
                        } else {
                            stringResource(R.string.settings_bluetooth_not_allowed)
                        },
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !state.hasBluetoothConnect) {
                                btConnectLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            }
                        },
                        trailing = {
                            Text(
                                text = if (state.hasBluetoothConnect) {
                                    stringResource(R.string.settings_status_allowed)
                                } else {
                                    stringResource(R.string.settings_status_denied)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.hasBluetoothConnect) colors.statusGreenText else colors.blue
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Privacy Note Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.line.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
