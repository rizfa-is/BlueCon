package com.siosin.bluecon.domain.usecase.scanner

import com.siosin.bluecon.bluetooth.StopReason
import com.siosin.bluecon.domain.repository.ScannerRepository

class StopScanUseCase(private val repository: ScannerRepository) {
    operator fun invoke() = repository.stop(StopReason.USER)
}
