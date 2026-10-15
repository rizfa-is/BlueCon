package com.siosin.bluecon.domain.usecase.scanner

import com.siosin.bluecon.bluetooth.ScanConfig
import com.siosin.bluecon.domain.repository.ScannerRepository

/** Application policy: each foreground scan lasts at most two minutes. */
class StartScanUseCase(private val repository: ScannerRepository) {
    operator fun invoke() = repository.start(ScanConfig(durationMillis = 120_000))
}
