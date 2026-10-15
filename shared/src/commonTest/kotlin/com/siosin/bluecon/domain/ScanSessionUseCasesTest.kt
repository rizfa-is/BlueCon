package com.siosin.bluecon.domain

import com.siosin.bluecon.bluetooth.ScanState
import com.siosin.bluecon.bluetooth.StopReason
import com.siosin.bluecon.domain.usecase.scanner.StartScanUseCase
import com.siosin.bluecon.domain.usecase.scanner.StopScanOnBackgroundUseCase
import com.siosin.bluecon.domain.usecase.scanner.StopScanUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class ScanSessionUseCasesTest {
    @Test
    fun foregroundScanUsesTwoMinuteSessionPolicy() {
        val repository = FakeScannerRepository()
        StartScanUseCase(repository)()
        assertEquals(120_000L, repository.requestedConfig?.durationMillis)
        assertEquals(ScanState.Scanning, repository.state.value)
    }

    @Test
    fun userStopAndBackgroundStopKeepDistinctReasons() {
        val repository = FakeScannerRepository()
        val start = StartScanUseCase(repository)
        start()
        StopScanUseCase(repository)()
        assertEquals(ScanState.Stopped(StopReason.USER), repository.state.value)
        start()
        StopScanOnBackgroundUseCase(repository)()
        assertEquals(ScanState.Stopped(StopReason.BACKGROUND), repository.state.value)
    }
}
