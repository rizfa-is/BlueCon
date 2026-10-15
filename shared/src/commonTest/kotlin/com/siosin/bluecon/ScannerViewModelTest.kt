package com.siosin.bluecon

import androidx.lifecycle.viewModelScope
import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.bluetooth.ScanConfig
import com.siosin.bluecon.bluetooth.ScanState
import com.siosin.bluecon.bluetooth.StopReason
import com.siosin.bluecon.bluetooth.proximityZone
import com.siosin.bluecon.domain.DeviceHistory
import com.siosin.bluecon.domain.DeviceSort
import com.siosin.bluecon.domain.DeviceHistoryRepository
import com.siosin.bluecon.domain.HistoryState
import com.siosin.bluecon.domain.ScanDevices
import com.siosin.bluecon.domain.ScannerRepository
import com.siosin.bluecon.presentation.ScannerTab
import com.siosin.bluecon.presentation.ScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {
    private class FakeScanner : ScannerRepository {
        override val devices = MutableStateFlow<List<BleDevice>>(emptyList())
        override val state = MutableStateFlow<ScanState>(ScanState.Idle)
        override fun start(config: ScanConfig) { state.value = ScanState.Scanning }
        override fun stop(reason: StopReason) { state.value = ScanState.Stopped(reason) }
    }

    private class FakeHistory : DeviceHistoryRepository {
        override val state = MutableStateFlow(HistoryState())
        override suspend fun save(devices: List<BleDevice>) = Unit
        override suspend fun clear() { state.value = HistoryState() }
    }

    @Test
    fun targetSelectionSurvivesMissingSamplesAndReacquiresTheSameAddress() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val scanner = FakeScanner()
        val model = ScannerViewModel(ScanDevices(scanner), FakeHistory())
        try {
            runCurrent()
            model.select("AA")
            assertNull(model.state.value.selectedDevice)
            scanner.devices.value = listOf(BleDevice("AA", "Beacon", -60, -60.0, proximityZone(-60), 100))
            runCurrent()
            assertEquals("Beacon", model.state.value.selectedDevice?.name)
            scanner.devices.value = emptyList()
            runCurrent()
            assertEquals("AA", model.state.value.selectedId)
            assertEquals(true, model.state.value.selectedDevice?.isStale)
            assertEquals(-60, model.state.value.selectedDevice?.rawRssi)
            scanner.devices.value = listOf(BleDevice("AA", "Beacon", -50, -50.0, proximityZone(-50), 200))
            runCurrent()
            assertEquals(-50, model.state.value.selectedDevice?.rawRssi)
            model.changeTab(ScannerTab.HISTORY)
            assertNull(model.state.value.selectedId)
            assertNull(model.state.value.selectedSnapshot)
        } finally {
            model.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun clearingHistoryUpdatesTheObservedUiState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val history = FakeHistory()
        val model = ScannerViewModel(ScanDevices(FakeScanner()), history)
        try {
            history.state.value = HistoryState(listOf(DeviceHistory("AA", "Beacon", -60, 100)))
            runCurrent()
            assertEquals(1, model.state.value.history.devices.size)
            model.clearHistory()
            runCurrent()
            assertEquals(emptyList(), model.state.value.history.devices)
        } finally {
            model.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun searchAndSortSurviveTabChangesAndApplyToIncomingObservations() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val scanner = FakeScanner()
        val history = FakeHistory()
        val model = ScannerViewModel(ScanDevices(scanner), history)
        try {
            model.search("beacon")
            model.searchHistory("bb:02")
            model.setSort(DeviceSort.SIGNAL_STRENGTH)
            model.changeTab(ScannerTab.HISTORY)
            history.state.value = HistoryState(listOf(
                DeviceHistory("AA:01", "Beacon", -70, 100),
                DeviceHistory("BB:02", null, -50, 200),
            ))
            scanner.devices.value = listOf(
                BleDevice("AA", "Beacon A", -70, -70.0, proximityZone(-70), 100),
                BleDevice("BB", "Beacon B", -50, -50.0, proximityZone(-50), 100),
            )
            runCurrent()
            assertEquals(listOf("BB:02"), model.state.value.visibleHistory.map { it.id })
            model.changeTab(ScannerTab.SCANNER)
            assertEquals("beacon", model.state.value.query)
            assertEquals("bb:02", model.state.value.historyQuery)
            assertEquals(listOf("BB", "AA"), model.state.value.visibleDevices.map { it.id })
        } finally {
            model.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun selectingASavedAddressDoesNotReuseAnotherDevicesAdvertisingDetails() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val scanner = FakeScanner()
        val history = FakeHistory()
        val model = ScannerViewModel(ScanDevices(scanner), history)
        try {
            scanner.devices.value = listOf(BleDevice("LIVE", "Sensor", -50, -50.0, proximityZone(-50), 200))
            history.state.value = HistoryState(listOf(DeviceHistory("SAVED", "Saved sensor", -70, 100)))
            runCurrent()
            model.select("LIVE")
            assertEquals("LIVE", model.state.value.selectedDevice?.id)
            model.select("SAVED")
            assertNull(model.state.value.selectedDevice)
            assertEquals("Saved sensor", model.state.value.selectedHistoryDevice?.name)
            model.back()
            assertNull(model.state.value.selectedHistoryDevice)
        } finally {
            model.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }
}
