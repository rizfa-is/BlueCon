package com.siosin.bluecon.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.bluetooth.ScanState
import com.siosin.bluecon.bluetooth.ProximityZone
import com.siosin.bluecon.domain.model.DeviceHistory
import com.siosin.bluecon.domain.model.DeviceSort
import com.siosin.bluecon.domain.model.HistoryState
import com.siosin.bluecon.domain.usecase.history.ClearDeviceHistoryUseCase
import com.siosin.bluecon.domain.usecase.history.ObserveDeviceHistoryUseCase
import com.siosin.bluecon.domain.usecase.history.SearchDeviceHistoryUseCase
import com.siosin.bluecon.domain.usecase.scanner.FilterScannedDevicesUseCase
import com.siosin.bluecon.domain.usecase.scanner.ObserveScannedDevicesUseCase
import com.siosin.bluecon.domain.usecase.scanner.ObserveScanStateUseCase
import com.siosin.bluecon.domain.usecase.scanner.SortScannedDevicesUseCase
import com.siosin.bluecon.domain.usecase.scanner.StartScanUseCase
import com.siosin.bluecon.domain.usecase.scanner.StopScanOnBackgroundUseCase
import com.siosin.bluecon.domain.usecase.scanner.StopScanUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScannerTab { SCANNER, HISTORY }

data class ScannerUiState(
    val scanState: ScanState = ScanState.Idle,
    val devices: List<BleDevice> = emptyList(),
    val history: HistoryState = HistoryState(),
    val visibleDevices: List<BleDevice> = emptyList(),
    val visibleHistory: List<DeviceHistory> = emptyList(),
    val query: String = "",
    val historyQuery: String = "",
    val sort: DeviceSort = DeviceSort.NAMED_FIRST,
    val minimumRssi: Int? = null,
    val selectedId: String? = null,
    val selectedSnapshot: BleDevice? = null,
    val tab: ScannerTab = ScannerTab.SCANNER,
) {
    val selectedDevice get() = devices.firstOrNull { it.id == selectedId }
        ?: selectedSnapshot?.takeIf { it.id == selectedId }
    val selectedHistoryDevice get() = history.devices.firstOrNull { it.id == selectedId }
    val scanning get() = scanState == ScanState.Scanning
}

class ScannerViewModel(
    private val startScan: StartScanUseCase,
    private val stopScan: StopScanUseCase,
    private val stopScanOnBackground: StopScanOnBackgroundUseCase,
    observeScannedDevices: ObserveScannedDevicesUseCase,
    observeScanState: ObserveScanStateUseCase,
    observeDeviceHistory: ObserveDeviceHistoryUseCase,
    private val filterScannedDevices: FilterScannedDevicesUseCase,
    private val sortScannedDevices: SortScannedDevicesUseCase,
    private val searchDeviceHistory: SearchDeviceHistoryUseCase,
    private val clearDeviceHistory: ClearDeviceHistoryUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ScannerUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            observeScannedDevices().collect { devices ->
                mutableState.update { current ->
                    val selected = devices.firstOrNull { it.id == current.selectedId }
                        ?: current.selectedSnapshot?.copy(isStale = true, zone = ProximityZone.LOST)
                    current.copy(devices = devices, selectedSnapshot = selected).withVisibleDevices()
                }
            }
        }
        viewModelScope.launch { observeScanState().collect { status -> mutableState.update { it.copy(scanState = status) } } }
        viewModelScope.launch {
            observeDeviceHistory().collect { saved ->
                mutableState.update { it.copy(history = saved).withVisibleHistory() }
            }
        }
    }

    fun start() = startScan()
    fun stop() = stopScan()
    fun onBackground() = stopScanOnBackground()
    fun search(value: String) { mutableState.update { it.copy(query = value).withVisibleDevices() } }
    fun searchHistory(value: String) { mutableState.update { it.copy(historyQuery = value).withVisibleHistory() } }
    fun setSort(value: DeviceSort) { mutableState.update { it.copy(sort = value).withVisibleDevices() } }
    fun setMinimumRssi(value: Int?) { mutableState.update { it.copy(minimumRssi = value).withVisibleDevices() } }
    fun select(id: String) { mutableState.update { it.copy(selectedId = id, selectedSnapshot = it.devices.firstOrNull { device -> device.id == id }) } }
    fun back() { mutableState.update { it.copy(selectedId = null, selectedSnapshot = null) } }
    fun changeTab(tab: ScannerTab) { mutableState.update { it.copy(tab = tab, selectedId = null, selectedSnapshot = null) } }
    fun clearHistory() { viewModelScope.launch { clearDeviceHistory() } }

    private fun ScannerUiState.withVisibleDevices() = copy(
        visibleDevices = sortScannedDevices(filterScannedDevices(devices, query, minimumRssi), sort),
    )

    private fun ScannerUiState.withVisibleHistory() = copy(
        visibleHistory = searchDeviceHistory(history.devices, historyQuery),
    )
}
