package com.siosin.bluecon.domain.usecase.history

import com.siosin.bluecon.domain.repository.DeviceHistoryRepository

class ObserveDeviceHistoryUseCase(private val repository: DeviceHistoryRepository) {
    operator fun invoke() = repository.state
}
