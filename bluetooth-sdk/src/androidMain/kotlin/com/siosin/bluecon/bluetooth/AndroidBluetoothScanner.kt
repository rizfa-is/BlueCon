package com.siosin.bluecon.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground BLE advertising scanner, API 24+. Does not pair, connect, request UI
 * permissions, or start services. All mutable session state is confined to main.
 *
 * With [derivesPhysicalLocation] false, the host must declare neverForLocation
 * and accept Android's beacon filtering. The default conservatively requests location.
 */
class AndroidBluetoothScanner(
    context: Context,
    private val derivesPhysicalLocation: Boolean = true,
    signalProcessor: SignalProcessor = EmaSignalProcessor(),
) : BluetoothScanner {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val main = Handler(Looper.getMainLooper())
    private val tracker = DeviceTracker(signalProcessor)
    private val mutableState = MutableStateFlow<ScanState>(ScanState.Idle)
    private val mutableDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val state = mutableState.asStateFlow()
    override val devices = mutableDevices.asStateFlow()

    private var platformScanner: BluetoothLeScanner? = null
    private var callback: ScanCallback? = null
    private var ticker: Job? = null
    private var sessionId = 0L
    private var config = ScanConfig()
    private var receiverRegistered = false
    private var closed = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Re-read platform state instead of trusting broadcast extras.
            onMain {
                if (callback != null) readinessBlocker()?.let { finish(ScanState.Blocked(it)) }
            }
        }
    }

    fun requiredPermissions(): List<String> = AndroidPermissionPolicy.requiredPermissions(
        Build.VERSION.SDK_INT, derivesPhysicalLocation,
    )

    /** Refreshes an idle host after returning from system settings; never starts a scan. */
    fun refreshReadiness() = onMain {
        if (closed || callback != null) return@onMain
        val blocker = readinessBlocker()
        if (blocker != null) mutableState.value = ScanState.Blocked(blocker)
        else if (mutableState.value is ScanState.Blocked) mutableState.value = ScanState.Idle
    }

    /** Safe to call before presenting the host's permission/settings UI. */
    @SuppressLint("MissingPermission")
    fun readinessBlocker(): ScanBlocker? {
        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return ScanBlocker.Unsupported
        }
        val missing = requiredPermissions().filter {
            appContext.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) return ScanBlocker.MissingPermissions(missing)
        return try {
            val adapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter
                ?: return ScanBlocker.Unsupported
            if (!adapter.isEnabled) return ScanBlocker.BluetoothOff
            if (AndroidPermissionPolicy.requiresLocationServices(Build.VERSION.SDK_INT, derivesPhysicalLocation)) {
                val location = appContext.getSystemService(LocationManager::class.java)
                val enabled = if (Build.VERSION.SDK_INT >= 28) {
                    location?.isLocationEnabled == true
                } else {
                    location?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                        location?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
                }
                if (!enabled) return ScanBlocker.LocationOff
            }
            null
        } catch (_: SecurityException) {
            ScanBlocker.MissingPermissions(requiredPermissions())
        }
    }

    @SuppressLint("MissingPermission")
    override fun start(config: ScanConfig) = onMain {
        if (closed || callback != null) return@onMain
        readinessBlocker()?.let {
            mutableState.value = ScanState.Blocked(it)
            return@onMain
        }
        val filters = try {
            config.serviceUuids.map {
                ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(it))).build()
            }
        } catch (_: IllegalArgumentException) {
            mutableState.value = ScanState.Failed(null, "Invalid service UUID.")
            return@onMain
        }
        this.config = config
        tracker.clear()
        mutableDevices.value = emptyList()
        val generation = ++sessionId
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) = onMain {
                if (generation == sessionId && callback != null) record(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) = onMain {
                for (result in results) {
                    if (generation != sessionId || callback == null) break
                    record(result)
                }
            }

            override fun onScanFailed(errorCode: Int) = onMain {
                if (generation == sessionId && callback != null) {
                    finish(ScanState.Failed(errorCode, scanErrorMessage(errorCode)))
                }
            }
        }
        try {
            registerReceiver()
            val adapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter
            val scanner = adapter?.bluetoothLeScanner
            if (scanner == null) {
                mutableState.value = ScanState.Blocked(ScanBlocker.BluetoothOff)
                return@onMain
            }
            platformScanner = scanner
            callback = scanCallback
            val settings = ScanSettings.Builder()
                .setScanMode(if (config.highResponsiveness) ScanSettings.SCAN_MODE_LOW_LATENCY else ScanSettings.SCAN_MODE_BALANCED)
                .setReportDelay(0)
                .build()
            mutableState.value = ScanState.Scanning
            startPlatformScan(scanner, filters, settings, scanCallback)
            // A synchronous platform failure may have ended the session already.
            if (generation != sessionId) return@onMain
            val startedAt = SystemClock.elapsedRealtime()
            ticker = scope.launch {
                var lastPermissionCheck = startedAt
                while (isActive && generation == sessionId) {
                    delay(config.publishIntervalMillis)
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastPermissionCheck >= 1_000) {
                        lastPermissionCheck = now
                        readinessBlocker()?.let { finish(ScanState.Blocked(it)); return@launch }
                    }
                    mutableDevices.value = tracker.snapshot(now, config)
                    if (now - startedAt >= config.durationMillis) {
                        finish(ScanState.Stopped(StopReason.TIMEOUT))
                        return@launch
                    }
                }
            }
        } catch (_: SecurityException) {
            finish(ScanState.Blocked(ScanBlocker.MissingPermissions(requiredPermissions())))
        } catch (error: RuntimeException) {
            finish(ScanState.Failed(null, error.message ?: "Unable to start scanning."))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startPlatformScan(scanner: BluetoothLeScanner, filters: List<ScanFilter>, settings: ScanSettings, callback: ScanCallback) {
        scanner.startScan(filters, settings, callback)
    }

    private fun record(result: ScanResult) {
        try {
            val now = SystemClock.elapsedRealtime()
            val observedAt = result.timestampNanos / 1_000_000
            val elapsed = observedAt.coerceIn(0, now)
            tracker.record(
                id = result.device.address,
                // Advertised name avoids another metadata call and works for unpaired devices.
                name = result.scanRecord?.deviceName,
                rssi = result.rssi,
                wallTimeMillis = System.currentTimeMillis() - (now - elapsed),
                elapsedMillis = elapsed,
                config = config,
                advertisement = result.advertisementData(),
            )
        } catch (_: SecurityException) {
            finish(ScanState.Blocked(ScanBlocker.MissingPermissions(requiredPermissions())))
        } catch (error: RuntimeException) {
            finish(ScanState.Failed(null, error.message ?: "Unable to process the advertising report."))
        }
    }

    override fun stop(reason: StopReason) = onMain {
        if (!closed && callback != null) finish(ScanState.Stopped(reason))
    }

    @SuppressLint("MissingPermission")
    private fun finish(next: ScanState) {
        ++sessionId
        ticker?.cancel()
        ticker = null
        val oldCallback = callback
        callback = null
        try {
            if (oldCallback != null) platformScanner?.stopScan(oldCallback)
        } catch (_: SecurityException) {
            // Permission may have been revoked between readiness check and cleanup.
        } catch (_: IllegalStateException) {
            // Adapter may already have been switched off.
        } finally {
            platformScanner = null
            mutableDevices.value = tracker.snapshot(SystemClock.elapsedRealtime(), config)
            mutableState.value = next
        }
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
            addAction(LocationManager.MODE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            // Bluetooth broadcasts can originate from a privileged non-system UID.
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
        receiverRegistered = true
    }

    override fun close() = onMain {
        if (closed) return@onMain
        closed = true
        finish(ScanState.Closed)
        if (receiverRegistered) {
            appContext.unregisterReceiver(receiver)
            receiverRegistered = false
        }
        tracker.clear()
        mutableDevices.value = emptyList()
        scope.cancel()
    }

    private inline fun onMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post { block() }
    }
}

private fun scanErrorMessage(code: Int): String = when (code) {
    ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scanning is already running."
    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Scanner registration failed. Turn Bluetooth off and on, then try again."
    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "This device does not support the requested scan mode."
    5 -> "Scanner resources are in use. Stop other Bluetooth scans and try again."
    6 -> "Scanning was started too often. Wait before trying again."
    else -> "Scan failed (code $code)."
}
