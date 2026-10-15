package com.siosin.bluecon.domain.usecase.history

import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.domain.repository.DeviceHistoryRepository

class SaveDeviceHistoryUseCase(private val repository: DeviceHistoryRepository) {
    suspend operator fun invoke(devices: List<BleDevice>) = repository.save(devices)
}
