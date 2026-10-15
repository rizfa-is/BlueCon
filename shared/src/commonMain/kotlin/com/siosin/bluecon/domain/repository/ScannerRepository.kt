package com.siosin.bluecon.domain.repository

import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.bluetooth.ScanConfig
import com.siosin.bluecon.bluetooth.ScanState
import com.siosin.bluecon.bluetooth.StopReason
import kotlinx.coroutines.flow.StateFlow

interface ScannerRepository {
    val devices: StateFlow<List<BleDevice>>
    val state: StateFlow<ScanState>
    fun start(config: ScanConfig)
    fun stop(reason: StopReason)
}
