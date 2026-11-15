package com.example.ui.screens

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.WatchSyncRecord
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.watch.WatchConnectionStatus
import com.example.watch.WatchH9BleManager
import com.example.watch.WatchH9State

private val laxasfitManualModels = listOf(
    "Watch H9",
    "Watch H9 Pro",
    "Laxasfit GT",
    "Laxasfit Watch Pro",
    "Laxasfit Band H8",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WatchH9Screen(
    watchManager: WatchH9BleManager,
    watchState: WatchH9State,
    profile: UserProfile?,
    syncHistory: List<WatchSyncRecord>,
    onSyncTelemetryToCloud: (
        deviceName: String,
        deviceAddress: String,
        heartRateBpm: Int,
        batteryPercent: Int,
        stepsToday: Int,
        source: String,
    ) -> Unit,
) {
    var companionError by remember { mutableStateOf<String?>(null) }
    var selectedManualModel by remember {
        mutableStateOf(profile?.watchH9PairedName ?: laxasfitManualModels.first())
    }
    var manualHrText by remember { mutableStateOf((profile?.lastRestingHr ?: 72).toString()) }
    var manualBatteryText by remember { mutableStateOf((profile?.lastWatchBattery ?: 85).toString()) }
    var manualStepsText by remember { mutableStateOf("2800") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            watchManager.startScan()
        }
    }

    val companionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            watchManager.startScan()
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        watchManager.refreshHardwareStatus()
    }

    LaunchedEffect(Unit) {
        watchManager.refreshHardwareStatus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Laxasfit Devices & Watch H9",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Compatible with Watch H9 and all Laxasfit watches & fitness bands via Bluetooth Low Energy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_watch_h9_sync),
                        contentDescription = "Laxasfit and Watch H9 smartwatch sync illustration",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = when (watchState.status) {
                                    WatchConnectionStatus.CONNECTED -> Icons.Rounded.BluetoothConnected
                                    WatchConnectionStatus.SCANNING -> Icons.AutoMirrored.Rounded.BluetoothSearching
                                    else -> Icons.Rounded.Bluetooth
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column {
                                Text(
                                    text = watchState.connectedDeviceName
                                        ?: profile?.watchH9PairedName
                                        ?: "Watch H9 / Laxasfit",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = watchState.connectedDeviceFamily ?: statusLabel(watchState.status),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (watchState.status == WatchConnectionStatus.SCANNING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.MonitorHeart,
                            label = "Heart Rate",
                            value = (watchState.liveHeartRateBpm ?: profile?.lastRestingHr)
                                ?.let { "$it BPM" } ?: "-- BPM",
                        )
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.BatteryChargingFull,
                            label = "Watch Battery",
                            value = (watchState.batteryPercent ?: profile?.lastWatchBattery)
                                ?.let { "$it%" } ?: "--%",
                        )
                    }

                    watchState.lastSyncMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    (watchState.errorMessage ?: companionError)?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when (watchState.status) {
                            WatchConnectionStatus.BLUETOOTH_OFF -> {
                                Button(
                                    onClick = {
                                        enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("enable_bluetooth_button"),
                                ) {
                                    Text("Enable Bluetooth")
                                }
                            }
                            WatchConnectionStatus.CONNECTED -> {
                                Button(
                                    onClick = {
                                        onSyncTelemetryToCloud(
                                            watchState.connectedDeviceName ?: "Watch H9",
                                            watchState.connectedDeviceAddress ?: "LAXASFIT-BLE",
                                            watchState.liveHeartRateBpm ?: profile?.lastRestingHr ?: 72,
                                            watchState.batteryPercent ?: profile?.lastWatchBattery ?: 85,
                                            0,
                                            "ble_gatt",
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sync_watch_cloud_button"),
                                ) {
                                    Icon(
                                        Icons.Rounded.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Now")
                                }
                                OutlinedButton(
                                    onClick = { watchManager.disconnect() },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Disconnect")
                                }
                            }
                            WatchConnectionStatus.SCANNING -> {
                                OutlinedButton(
                                    onClick = { watchManager.stopScan() },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Stop Scan")
                                }
                            }
                            else -> {
                                Button(
                                    onClick = {
                                        if (watchManager.hasRequiredPermissions()) {
                                            watchManager.startScan()
                                        } else {
                                            permissionLauncher.launch(watchManager.requiredPermissions())
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("scan_watch_h9_button"),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.BluetoothSearching,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan Laxasfit")
                                }
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            FilledTonalButton(
                                onClick = {
                                    companionError = null
                                    watchManager.startCompanionAssociation(
                                        onIntentSenderReady = { sender ->
                                            companionLauncher.launch(
                                                IntentSenderRequest.Builder(sender).build()
                                            )
                                        },
                                        onError = { err -> companionError = err },
                                    )
                                },
                                modifier = Modifier.testTag("companion_pair_button"),
                            ) {
                                Icon(
                                    Icons.Rounded.Watch,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pair")
                            }
                        }
                    }
                }
            }
        }

        if (watchState.discoveredDevices.isNotEmpty()) {
            item {
                Text(
                    text = "Nearby BLE Devices",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(watchState.discoveredDevices, key = { it.address }) { device ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                if (device.isLikelyLaxasfit) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Text(
                                            text = device.matchedFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${device.address} · Signal ${device.rssi} dBm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                watchManager.connectToDevice(
                                    address = device.address,
                                    displayName = device.name,
                                    familyLabel = device.matchedFamily,
                                )
                            }
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "All Compatible Laxasfit Devices",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    WatchH9BleManager.compatibleFamilies.forEach { family ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp),
                            )
                            Column {
                                Text(
                                    text = "${family.displayName} (${family.modelsLabel})",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = family.supportedProfiles,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Manual reading log supporting any Laxasfit device model
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Log Reading from Laxasfit Screen",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        laxasfitManualModels.forEach { model ->
                            FilterChip(
                                selected = selectedManualModel == model,
                                onClick = { selectedManualModel = model },
                                label = { Text(model) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = manualHrText,
                            onValueChange = { manualHrText = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text("HR (BPM)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = manualBatteryText,
                            onValueChange = { manualBatteryText = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text("Battery %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = manualStepsText,
                            onValueChange = { manualStepsText = it.filter { ch -> ch.isDigit() }.take(6) },
                            label = { Text("Steps") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            val hr = manualHrText.toIntOrNull()?.coerceIn(40, 210) ?: 72
                            val bat = manualBatteryText.toIntOrNull()?.coerceIn(0, 100) ?: 85
                            val steps = manualStepsText.toIntOrNull()?.coerceIn(0, 150000) ?: 0
                            onSyncTelemetryToCloud(
                                selectedManualModel,
                                profile?.watchH9MacAddress ?: "LAXASFIT-MANUAL",
                                hr,
                                bat,
                                steps,
                                "manual_watch_entry",
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_manual_watch_sync_button"),
                    ) {
                        Icon(
                            Icons.Rounded.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save $selectedManualModel Snapshot")
                    }
                }
            }
        }

        if (syncHistory.isNotEmpty()) {
            item {
                Text(
                    text = "Synced Watch History",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(syncHistory, key = { it.id }) { sync ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${sync.deviceName} · ${sync.recoveryStatus}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Battery ${sync.batteryPercent}% · ${sync.stepsToday} steps · ${if (sync.source == "ble_gatt") "BLE GATT" else "Watch Entry"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${sync.heartRateBpm} BPM",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun statusLabel(status: WatchConnectionStatus): String = when (status) {
    WatchConnectionStatus.BLUETOOTH_UNAVAILABLE -> "BLE adapter unavailable on this device"
    WatchConnectionStatus.BLUETOOTH_OFF -> "Bluetooth is turned off"
    WatchConnectionStatus.DISCONNECTED -> "Ready for Watch H9 & all Laxasfit watches"
    WatchConnectionStatus.SCANNING -> "Scanning nearby Laxasfit Bluetooth LE devices..."
    WatchConnectionStatus.CONNECTING -> "Connecting to GATT server..."
    WatchConnectionStatus.CONNECTED -> "Connected · Live Heart Rate stream"
    WatchConnectionStatus.ERROR -> "Check Bluetooth permissions"
}
