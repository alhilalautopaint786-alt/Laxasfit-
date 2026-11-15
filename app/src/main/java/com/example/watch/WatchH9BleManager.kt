package com.example.watch

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.regex.Pattern

data class LaxasfitCompatibleFamily(
    val id: String,
    val displayName: String,
    val modelsLabel: String,
    val supportedProfiles: String,
)

data class DiscoveredWatchDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isLikelyLaxasfit: Boolean,
    val matchedFamily: String,
)

enum class WatchConnectionStatus {
    BLUETOOTH_UNAVAILABLE,
    BLUETOOTH_OFF,
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR,
}

data class WatchH9State(
    val bleSupported: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val status: WatchConnectionStatus = WatchConnectionStatus.DISCONNECTED,
    val connectedDeviceName: String? = null,
    val connectedDeviceAddress: String? = null,
    val connectedDeviceFamily: String? = null,
    val liveHeartRateBpm: Int? = null,
    val liveCadenceSpm: Int? = null,
    val batteryPercent: Int? = null,
    val discoveredDevices: List<DiscoveredWatchDevice> = emptyList(),
    val lastSyncMessage: String? = null,
    val errorMessage: String? = null,
)

class WatchH9BleManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _state = MutableStateFlow(
        WatchH9State(
            bleSupported = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            bluetoothEnabled = bluetoothAdapter?.isEnabled == true,
            status = when {
                bluetoothAdapter == null -> WatchConnectionStatus.BLUETOOTH_UNAVAILABLE
                bluetoothAdapter?.isEnabled != true -> WatchConnectionStatus.BLUETOOTH_OFF
                else -> WatchConnectionStatus.DISCONNECTED
            }
        )
    )
    val state: StateFlow<WatchH9State> = _state.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var writableSyncCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(appContext, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun refreshHardwareStatus() {
        val bleSupported = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val adapter = bluetoothAdapter
        val enabled = adapter?.isEnabled == true
        _state.update { current ->
            val nextStatus = when {
                adapter == null || !bleSupported -> WatchConnectionStatus.BLUETOOTH_UNAVAILABLE
                !enabled -> WatchConnectionStatus.BLUETOOTH_OFF
                current.status == WatchConnectionStatus.BLUETOOTH_UNAVAILABLE ||
                    current.status == WatchConnectionStatus.BLUETOOTH_OFF -> WatchConnectionStatus.DISCONNECTED
                else -> current.status
            }
            current.copy(
                bleSupported = bleSupported,
                bluetoothEnabled = enabled,
                status = nextStatus,
            )
        }
    }

    private fun classifyDeviceFamily(name: String, hasHrService: Boolean, hasFtmsService: Boolean): Pair<Boolean, String> {
        val lower = name.lowercase()
        return when {
            lower.contains("h9") -> true to "Watch H9 Series"
            lower.contains("laxasfit") || lower.contains("laxafit") -> true to "Laxasfit Watch Series"
            lower.contains("band") -> true to "Laxasfit Band Series"
            lower.contains("watch") || lower.contains("gt") || lower.contains("pro") -> true to "Smartwatch BLE"
            hasHrService || hasFtmsService -> true to "Universal BLE HR / Fitness"
            else -> false to "BLE Device"
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = try {
                device.name ?: result.scanRecord?.deviceName ?: "Unknown BLE Device"
            } catch (_: SecurityException) {
                "Unknown BLE Device"
            }
            val address = device.address ?: return
            val serviceUuids = result.scanRecord?.serviceUuids.orEmpty()
            val hasHr = serviceUuids.any { it.uuid == HEART_RATE_SERVICE_UUID }
            val hasFtms = serviceUuids.any { it.uuid == FITNESS_MACHINE_SERVICE_UUID }
            val (isCompatible, familyLabel) = classifyDeviceFamily(name, hasHr, hasFtms)

            val item = DiscoveredWatchDevice(
                name = name,
                address = address,
                rssi = result.rssi,
                isLikelyLaxasfit = isCompatible,
                matchedFamily = familyLabel,
            )
            _state.update { current ->
                val updated = (current.discoveredDevices.filterNot { it.address == address } + item)
                    .sortedWith(
                        compareByDescending<DiscoveredWatchDevice> { it.isLikelyLaxasfit }
                            .thenByDescending { it.rssi }
                    )
                current.copy(discoveredDevices = updated)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            _state.update {
                it.copy(
                    status = WatchConnectionStatus.ERROR,
                    errorMessage = "BLE scan failed (code $errorCode). Ensure Bluetooth is enabled.",
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        refreshHardwareStatus()
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _state.update {
                it.copy(
                    status = WatchConnectionStatus.BLUETOOTH_UNAVAILABLE,
                    errorMessage = "This device or emulator does not have a Bluetooth LE adapter.",
                )
            }
            return
        }
        if (!adapter.isEnabled) {
            _state.update {
                it.copy(
                    status = WatchConnectionStatus.BLUETOOTH_OFF,
                    errorMessage = "Turn on Bluetooth in system settings to scan for Laxasfit watches.",
                )
            }
            return
        }
        if (!hasRequiredPermissions()) {
            _state.update {
                it.copy(
                    status = WatchConnectionStatus.ERROR,
                    errorMessage = "Nearby Devices / Bluetooth permission is required to scan for Laxasfit devices.",
                )
            }
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _state.update {
                it.copy(
                    status = WatchConnectionStatus.ERROR,
                    errorMessage = "Bluetooth LE scanner is unavailable on this device.",
                )
            }
            return
        }

        if (isScanning) return
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        _state.update {
            it.copy(
                status = WatchConnectionStatus.SCANNING,
                discoveredDevices = emptyList(),
                errorMessage = null,
            )
        }
        isScanning = true
        scanner.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        isScanning = false
        try {
            if (hasRequiredPermissions()) {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping BLE scan", e)
        }
        _state.update { current ->
            if (current.status == WatchConnectionStatus.SCANNING) {
                current.copy(status = WatchConnectionStatus.DISCONNECTED)
            } else {
                current
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String, displayName: String? = null, familyLabel: String? = null) {
        stopScan()
        if (!hasRequiredPermissions()) {
            _state.update {
                it.copy(
                    status = WatchConnectionStatus.ERROR,
                    errorMessage = "Bluetooth permission missing.",
                )
            }
            return
        }
        val adapter = bluetoothAdapter ?: return
        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(address)
            activeGatt?.close()
            writableSyncCharacteristic = null
            val resolvedName = displayName ?: device.name ?: "Laxasfit Watch"
            val resolvedFamily = familyLabel ?: classifyDeviceFamily(resolvedName, false, false).second
            _state.update {
                it.copy(
                    status = WatchConnectionStatus.CONNECTING,
                    connectedDeviceName = resolvedName,
                    connectedDeviceAddress = address,
                    connectedDeviceFamily = resolvedFamily,
                    errorMessage = null,
                )
            }
            activeGatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect Laxasfit device GATT", e)
            _state.update {
                it.copy(
                    status = WatchConnectionStatus.ERROR,
                    errorMessage = e.localizedMessage ?: "Failed to connect to Laxasfit device.",
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing GATT", e)
        } finally {
            activeGatt = null
            writableSyncCharacteristic = null
            _state.update {
                it.copy(
                    status = WatchConnectionStatus.DISCONNECTED,
                    liveHeartRateBpm = null,
                    liveCadenceSpm = null,
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun pushYogaCueToWatch(routineTitle: String, targetHrMin: Int, targetHrMax: Int): Boolean {
        return writeWatchPayload(
            payloadText = "LAXASFIT|YOGA|${routineTitle.take(18)}|HR:$targetHrMin-$targetHrMax",
            friendlyLabel = routineTitle,
            targetHrMin = targetHrMin,
            targetHrMax = targetHrMax,
        )
    }

    @SuppressLint("MissingPermission")
    fun pushSportModeToWatch(
        sportName: String,
        watchCode: Int,
        targetHrMin: Int,
        targetHrMax: Int,
    ): Boolean {
        return writeWatchPayload(
            payloadText = "LAXASFIT|SPORT:$watchCode|${sportName.take(16)}|HR:$targetHrMin-$targetHrMax",
            friendlyLabel = "$sportName (#$watchCode)",
            targetHrMin = targetHrMin,
            targetHrMax = targetHrMax,
        )
    }

    @SuppressLint("MissingPermission")
    private fun writeWatchPayload(
        payloadText: String,
        friendlyLabel: String,
        targetHrMin: Int,
        targetHrMax: Int,
    ): Boolean {
        val gatt = activeGatt ?: return false
        val char = writableSyncCharacteristic
        val devName = _state.value.connectedDeviceName ?: "Laxasfit Watch"
        if (char == null || !hasRequiredPermissions()) {
            _state.update {
                it.copy(
                    lastSyncMessage = "Connected to $devName (HR stream active); target $targetHrMin–$targetHrMax BPM guided in app."
                )
            }
            return false
        }
        val payload = payloadText.toByteArray(StandardCharsets.UTF_8)
        return try {
            val written = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    char,
                    payload,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                ) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                run {
                    char.value = payload
                    gatt.writeCharacteristic(char)
                }
            }
            _state.update {
                it.copy(
                    lastSyncMessage = if (written) {
                        "Synced '$friendlyLabel' ($targetHrMin–$targetHrMax BPM) to $devName."
                    } else {
                        "$devName connected; telemetry stream active."
                    }
                )
            }
            written
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing payload to Laxasfit device", e)
            false
        }
    }

    fun startCompanionAssociation(
        onIntentSenderReady: (IntentSender) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onError("Companion Device Manager requires Android 8.0+.")
            return
        }
        val cdm = appContext.getSystemService(CompanionDeviceManager::class.java)
        if (cdm == null) {
            onError("CompanionDeviceManager is unavailable on this device.")
            return
        }
        val allLaxasfitFilter = BluetoothLeDeviceFilter.Builder()
            .setNamePattern(Pattern.compile(".*(H9|Laxasfit|Laxafit|Watch|Band|Fit).*", Pattern.CASE_INSENSITIVE))
            .build()
        val request = AssociationRequest.Builder()
            .addDeviceFilter(allLaxasfitFilter)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cdm.associate(
                request,
                appContext.mainExecutor,
                object : CompanionDeviceManager.Callback() {
                    override fun onAssociationPending(intentSender: IntentSender) {
                        onIntentSenderReady(intentSender)
                    }

                    override fun onFailure(error: CharSequence?) {
                        onError(error?.toString() ?: "Companion pairing failed.")
                    }
                }
            )
        } else {
            @Suppress("DEPRECATION")
            cdm.associate(
                request,
                object : CompanionDeviceManager.Callback() {
                    override fun onDeviceFound(chooserLauncher: IntentSender) {
                        onIntentSenderReady(chooserLauncher)
                    }

                    override fun onFailure(error: CharSequence?) {
                        onError(error?.toString() ?: "Companion pairing failed.")
                    }
                },
                null
            )
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _state.update {
                    it.copy(
                        status = WatchConnectionStatus.CONNECTED,
                        lastSyncMessage = "Connected to ${it.connectedDeviceName ?: "Laxasfit Watch"}. Discovering services...",
                    )
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                writableSyncCharacteristic = null
                _state.update {
                    it.copy(
                        status = WatchConnectionStatus.DISCONNECTED,
                        liveHeartRateBpm = null,
                        liveCadenceSpm = null,
                        lastSyncMessage = "${it.connectedDeviceName ?: "Laxasfit Watch"} disconnected.",
                    )
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val hrChar = gatt.getService(HEART_RATE_SERVICE_UUID)
                ?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
            enableNotifications(gatt, hrChar)

            val rscChar = gatt.getService(RSC_SERVICE_UUID)
                ?.getCharacteristic(RSC_MEASUREMENT_UUID)
            enableNotifications(gatt, rscChar)

            val batteryChar = gatt.getService(BATTERY_SERVICE_UUID)
                ?.getCharacteristic(BATTERY_LEVEL_UUID)
            if (batteryChar != null) {
                gatt.readCharacteristic(batteryChar)
            }

            writableSyncCharacteristic = gatt.services
                .flatMap { it.characteristics }
                .firstOrNull { char ->
                    (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                        (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                }

            val devName = _state.value.connectedDeviceName ?: "Laxasfit Watch"
            _state.update {
                it.copy(
                    lastSyncMessage = when {
                        hrChar != null && rscChar != null -> "HR & Cadence streams active on $devName."
                        hrChar != null -> "Heart Rate stream active on $devName."
                        else -> "Connected to $devName GATT server."
                    }
                )
            }
        }

        @SuppressLint("MissingPermission")
        private fun enableNotifications(gatt: BluetoothGatt, char: BluetoothGattCharacteristic?) {
            if (char == null) return
            gatt.setCharacteristicNotification(char, true)
            val cccd = char.getDescriptor(CCCD_UUID) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                run {
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(cccd)
                }
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                handleCharacteristicValue(characteristic.uuid, characteristic.value ?: byteArrayOf())
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicValue(characteristic.uuid, value)
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            @Suppress("DEPRECATION")
            handleCharacteristicValue(characteristic.uuid, characteristic.value ?: byteArrayOf())
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleCharacteristicValue(characteristic.uuid, value)
        }
    }

    private fun handleCharacteristicValue(uuid: UUID, value: ByteArray) {
        if (value.isEmpty()) return
        when (uuid) {
            HEART_RATE_MEASUREMENT_UUID -> {
                val flags = value[0].toInt()
                val isUint16 = (flags and 0x01) != 0
                val bpm = if (isUint16 && value.size >= 3) {
                    (value[1].toInt() and 0xFF) or ((value[2].toInt() and 0xFF) shl 8)
                } else if (value.size >= 2) {
                    value[1].toInt() and 0xFF
                } else {
                    return
                }
                _state.update {
                    it.copy(
                        liveHeartRateBpm = bpm,
                        lastSyncMessage = "Live HR (${it.connectedDeviceName ?: "Watch"}): $bpm BPM",
                    )
                }
            }
            RSC_MEASUREMENT_UUID -> {
                if (value.size >= 4) {
                    val cadence = value[3].toInt() and 0xFF
                    _state.update { it.copy(liveCadenceSpm = cadence) }
                }
            }
            BATTERY_LEVEL_UUID -> {
                val level = value[0].toInt() and 0xFF
                _state.update { it.copy(batteryPercent = level.coerceIn(0, 100)) }
            }
        }
    }

    companion object {
        private const val TAG = "WatchH9BleManager"
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val RSC_SERVICE_UUID: UUID = UUID.fromString("00001814-0000-1000-8000-00805f9b34fb")
        val RSC_MEASUREMENT_UUID: UUID = UUID.fromString("00002a53-0000-1000-8000-00805f9b34fb")
        val FITNESS_MACHINE_SERVICE_UUID: UUID = UUID.fromString("00001826-0000-1000-8000-00805f9b34fb")
        val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        val compatibleFamilies: List<LaxasfitCompatibleFamily> = listOf(
            LaxasfitCompatibleFamily(
                id = "h9_series",
                displayName = "Watch H9 Series",
                modelsLabel = "Watch H9 · H9 Pro · H9 Ultra",
                supportedProfiles = "BLE HR (0x180D) · Battery · Sport Mode Push",
            ),
            LaxasfitCompatibleFamily(
                id = "laxasfit_watch",
                displayName = "Laxasfit Watch Series",
                modelsLabel = "Laxasfit Watch · Pro · GT · Lite",
                supportedProfiles = "BLE HR (0x180D) · FTMS (0x1826) · Nordic UART",
            ),
            LaxasfitCompatibleFamily(
                id = "laxasfit_band",
                displayName = "Laxasfit Band Series",
                modelsLabel = "Laxasfit Band H5 · H8 · FitBand",
                supportedProfiles = "BLE HR (0x180D) · RSC Cadence (0x1814) · Battery",
            ),
            LaxasfitCompatibleFamily(
                id = "universal_ble",
                displayName = "Universal BLE Wearables",
                modelsLabel = "Any Standard GATT Heart Rate Watch",
                supportedProfiles = "Standard Bluetooth SIG Heart Rate & Battery",
            ),
        )
    }
}
