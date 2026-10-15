package com.siosin.bluecon.domain

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

/** Application session policy, independent of Activity and the platform BLE adapter. */
class ScanDevices(private val repository: ScannerRepository) {
    val devices get() = repository.devices
    val state get() = repository.state

    fun start() = repository.start(ScanConfig(durationMillis = 120_000))
    fun stop() = repository.stop(StopReason.USER)
    fun onBackground() = repository.stop(StopReason.BACKGROUND)
}

enum class DeviceSort { NAMED_FIRST, SIGNAL_STRENGTH, NAME, LAST_SEEN }

fun filterDevices(
    devices: List<BleDevice>,
    query: String,
    minimumRssi: Int?,
    sort: DeviceSort = DeviceSort.NAMED_FIRST,
): List<BleDevice> {
    val search = query.trim()
    val order = when (sort) {
        DeviceSort.NAMED_FIRST -> compareBy<BleDevice> { it.name.isNullOrBlank() }.thenByDescending { it.rawRssi }
        DeviceSort.SIGNAL_STRENGTH -> compareByDescending<BleDevice> { it.rawRssi }
        DeviceSort.NAME -> compareBy<BleDevice> { it.name.isNullOrBlank() }
            .thenBy { it.name.orEmpty().trim().lowercase() }.thenByDescending { it.rawRssi }
        DeviceSort.LAST_SEEN -> compareByDescending<BleDevice> { it.lastSeenAtMillis }.thenByDescending { it.rawRssi }
    }
    return devices.filter {
        (search.isEmpty() || it.id.contains(search, ignoreCase = true) ||
            it.name.orEmpty().contains(search, ignoreCase = true)) &&
            (minimumRssi == null || it.rawRssi >= minimumRssi)
    }.sortedWith(compareBy<BleDevice> { it.isStale }.then(order).thenBy { it.id })
}
