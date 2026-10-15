package com.siosin.bluecon.domain.usecase.scanner

import com.siosin.bluecon.domain.repository.ScannerRepository

class ObserveScanStateUseCase(private val repository: ScannerRepository) {
    operator fun invoke() = repository.state
}
