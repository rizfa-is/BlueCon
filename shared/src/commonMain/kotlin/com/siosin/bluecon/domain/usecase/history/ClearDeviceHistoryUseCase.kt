package com.siosin.bluecon.domain.usecase.history

import com.siosin.bluecon.domain.repository.DeviceHistoryRepository

class ClearDeviceHistoryUseCase(private val repository: DeviceHistoryRepository) {
    suspend operator fun invoke() = repository.clear()
}
