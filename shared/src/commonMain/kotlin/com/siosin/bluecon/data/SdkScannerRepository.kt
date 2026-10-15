package com.siosin.bluecon.data

import com.siosin.bluecon.bluetooth.BluetoothScanner
import com.siosin.bluecon.bluetooth.ScanConfig
import com.siosin.bluecon.bluetooth.StopReason
import com.siosin.bluecon.domain.ScannerRepository

class SdkScannerRepository(private val scanner: BluetoothScanner) : ScannerRepository {
    override val devices get() = scanner.devices
    override val state get() = scanner.state
    override fun start(config: ScanConfig) = scanner.start(config)
    override fun stop(reason: StopReason) = scanner.stop(reason)
}
