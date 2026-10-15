package com.siosin.bluecon.domain

import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.bluetooth.proximityZone
import com.siosin.bluecon.domain.usecase.history.SaveDeviceHistoryUseCase
import com.siosin.bluecon.domain.usecase.history.SyncScanHistoryUseCase
import com.siosin.bluecon.domain.usecase.scanner.ObserveScannedDevicesUseCase
import com.siosin.bluecon.domain.usecase.scanner.ObserveScanStateUseCase
import com.siosin.bluecon.domain.usecase.scanner.StartScanUseCase
import com.siosin.bluecon.domain.usecase.scanner.StopScanOnBackgroundUseCase
import com.siosin.bluecon.domain.usecase.scanner.StopScanUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SyncScanHistoryUseCaseTest {
    private fun device(rssi: Int, timestamp: Long) = BleDevice("AA", "Sensor", rssi, rssi.toDouble(), proximityZone(rssi), timestamp)

    @Test
    fun syncBatchesLatestObservationsFlushesOnStopAndDoesNotPollWhileIdle() = runTest {
        val scanner = FakeScannerRepository()
        val history = FakeDeviceHistoryRepository()
        val sync = SyncScanHistoryUseCase(
            ObserveScanStateUseCase(scanner), ObserveScannedDevicesUseCase(scanner), SaveDeviceHistoryUseCase(history),
        )
        val job = backgroundScope.launch { sync() }
        runCurrent()
        history.savedBatches.clear() // Ignore initial idle flush.

        StartScanUseCase(scanner)()
        runCurrent()
        scanner.devices.value = listOf(device(-70, 100))
        advanceTimeBy(1_999)
        runCurrent()
        assertEquals(0, history.savedBatches.size)
        scanner.devices.value = listOf(device(-60, 200))
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(scanner.devices.value), history.savedBatches)

        scanner.devices.value = listOf(device(-50, 300))
        StopScanUseCase(scanner)()
        runCurrent()
        assertEquals(2, history.savedBatches.size)
        assertEquals(scanner.devices.value, history.savedBatches.last())
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(2, history.savedBatches.size)
        job.cancel()
    }

    @Test
    fun backgroundStopCancelsThePendingBatchAndRestartUsesANewWindow() = runTest {
        val scanner = FakeScannerRepository()
        val history = FakeDeviceHistoryRepository()
        val sync = SyncScanHistoryUseCase(
            ObserveScanStateUseCase(scanner), ObserveScannedDevicesUseCase(scanner), SaveDeviceHistoryUseCase(history),
        )
        val job = backgroundScope.launch { sync() }
        StartScanUseCase(scanner)()
        scanner.devices.value = listOf(device(-70, 100))
        runCurrent()
        advanceTimeBy(1_000)
        StopScanOnBackgroundUseCase(scanner)()
        runCurrent()
        assertEquals(listOf(scanner.devices.value), history.savedBatches)
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(1, history.savedBatches.size)

        StartScanUseCase(scanner)()
        runCurrent()
        scanner.devices.value = listOf(device(-40, 200))
        advanceTimeBy(1_999)
        runCurrent()
        assertEquals(1, history.savedBatches.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, history.savedBatches.size)
        assertEquals(scanner.devices.value, history.savedBatches.last())

        job.cancel()
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(2, history.savedBatches.size)
    }
}
