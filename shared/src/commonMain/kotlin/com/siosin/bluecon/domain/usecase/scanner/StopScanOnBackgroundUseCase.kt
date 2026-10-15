package com.siosin.bluecon.domain.usecase.scanner

import com.siosin.bluecon.bluetooth.StopReason
import com.siosin.bluecon.domain.repository.ScannerRepository

class StopScanOnBackgroundUseCase(private val repository: ScannerRepository) {
    operator fun invoke() = repository.stop(StopReason.BACKGROUND)
}
