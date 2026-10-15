package com.siosin.bluecon.domain.usecase.scanner

import com.siosin.bluecon.domain.repository.ScannerRepository

class ObserveScannedDevicesUseCase(private val repository: ScannerRepository) {
    operator fun invoke() = repository.devices
}
