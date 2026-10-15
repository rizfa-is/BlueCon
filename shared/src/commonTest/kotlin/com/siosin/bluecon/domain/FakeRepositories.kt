package com.siosin.bluecon.domain

import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.bluetooth.ScanConfig
import com.siosin.bluecon.bluetooth.ScanState
import com.siosin.bluecon.bluetooth.StopReason
import com.siosin.bluecon.domain.model.HistoryState
import com.siosin.bluecon.domain.repository.DeviceHistoryRepository
import com.siosin.bluecon.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeScannerRepository : ScannerRepository {
    override val devices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val state = MutableStateFlow<ScanState>(ScanState.Idle)
    var requestedConfig: ScanConfig? = null

    override fun start(config: ScanConfig) {
        requestedConfig = config
        state.value = ScanState.Scanning
    }

    override fun stop(reason: StopReason) { state.value = ScanState.Stopped(reason) }
}

internal class FakeDeviceHistoryRepository : DeviceHistoryRepository {
    override val state = MutableStateFlow(HistoryState())
    val savedBatches = mutableListOf<List<BleDevice>>()
    var clearCount = 0

    override suspend fun save(devices: List<BleDevice>) { savedBatches += devices.toList() }
    override suspend fun clear() {
        clearCount++
        state.value = HistoryState()
    }
}
