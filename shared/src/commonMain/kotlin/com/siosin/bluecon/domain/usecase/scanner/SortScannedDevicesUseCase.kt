package com.siosin.bluecon.domain.usecase.scanner

import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.domain.model.DeviceSort

/** Recent signals come first in every mode; address provides deterministic tie-breaking. */
class SortScannedDevicesUseCase {
    operator fun invoke(devices: List<BleDevice>, sort: DeviceSort): List<BleDevice> {
        val order = when (sort) {
            DeviceSort.NAMED_FIRST -> compareBy<BleDevice> { it.name.isNullOrBlank() }.thenByDescending { it.rawRssi }
            DeviceSort.SIGNAL_STRENGTH -> compareByDescending<BleDevice> { it.rawRssi }
            DeviceSort.NAME -> compareBy<BleDevice> { it.name.isNullOrBlank() }
                .thenBy { it.name.orEmpty().trim().lowercase() }.thenByDescending { it.rawRssi }
            DeviceSort.LAST_SEEN -> compareByDescending<BleDevice> { it.lastSeenAtMillis }.thenByDescending { it.rawRssi }
        }
        return devices.sortedWith(compareBy<BleDevice> { it.isStale }.then(order).thenBy { it.id })
    }
}
