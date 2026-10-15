package com.siosin.bluecon.domain.usecase.history

import com.siosin.bluecon.bluetooth.ScanState
import com.siosin.bluecon.domain.usecase.scanner.ObserveScannedDevicesUseCase
import com.siosin.bluecon.domain.usecase.scanner.ObserveScanStateUseCase
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive

/** Run once in application scope, independent of screens and configuration changes. */
class SyncScanHistoryUseCase(
    private val observeScanState: ObserveScanStateUseCase,
    private val observeScannedDevices: ObserveScannedDevicesUseCase,
    private val saveDeviceHistory: SaveDeviceHistoryUseCase,
) {
    suspend operator fun invoke() {
        val devices = observeScannedDevices()
        observeScanState().collectLatest { state ->
            if (state == ScanState.Scanning) {
                while (currentCoroutineContext().isActive) {
                    delay(2_000)
                    saveDeviceHistory(devices.value)
                }
            } else {
                // Flush the latest snapshot on stop; do not poll while idle.
                saveDeviceHistory(devices.value)
            }
        }
    }
}
