package com.siosin.bluecon.domain.usecase.scanner

import com.siosin.bluecon.bluetooth.BleDevice

/** Search and inclusive RSSI filtering preserve the input order. */
class FilterScannedDevicesUseCase {
    operator fun invoke(devices: List<BleDevice>, query: String, minimumRssi: Int?): List<BleDevice> {
        val search = query.trim()
        return devices.filter {
            (search.isEmpty() || it.id.contains(search, ignoreCase = true) ||
                it.name.orEmpty().contains(search, ignoreCase = true)) &&
                (minimumRssi == null || it.rawRssi >= minimumRssi)
        }
    }
}
