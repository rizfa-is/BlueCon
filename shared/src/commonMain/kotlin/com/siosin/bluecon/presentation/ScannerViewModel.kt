package com.siosin.bluecon.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.bluetooth.ScanState
import com.siosin.bluecon.domain.DeviceHistoryRepository
import com.siosin.bluecon.domain.HistoryState
import com.siosin.bluecon.domain.ScanDevices
import com.siosin.bluecon.domain.filterDevices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScannerTab { SCANNER, HISTORY }

data class ScannerUiState(
    val scanState: ScanState = ScanState.Idle,
    val devices: List<BleDevice> = emptyList(),
    val history: HistoryState = HistoryState(),
    val query: String = "",
    val minimumRssi: Int? = null,
    val selectedId: String? = null,
    val tab: ScannerTab = ScannerTab.SCANNER,
) {
    val visibleDevices get() = filterDevices(devices, query, minimumRssi)
    val selectedDevice get() = devices.firstOrNull { it.id == selectedId }
    val scanning get() = scanState == ScanState.Scanning
}

class ScannerViewModel(
    private val scanDevices: ScanDevices,
    private val history: DeviceHistoryRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ScannerUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch { scanDevices.devices.collect { devices -> mutableState.update { it.copy(devices = devices) } } }
        viewModelScope.launch { scanDevices.state.collect { status -> mutableState.update { it.copy(scanState = status) } } }
        viewModelScope.launch { history.state.collect { saved -> mutableState.update { it.copy(history = saved) } } }
    }

    fun start() = scanDevices.start()
    fun stop() = scanDevices.stop()
    fun onBackground() = scanDevices.onBackground()
    fun search(value: String) { mutableState.update { it.copy(query = value) } }
    fun setMinimumRssi(value: Int?) { mutableState.update { it.copy(minimumRssi = value) } }
    fun select(id: String) { mutableState.update { it.copy(selectedId = id) } }
    fun back() { mutableState.update { it.copy(selectedId = null) } }
    fun changeTab(tab: ScannerTab) { mutableState.update { it.copy(tab = tab, selectedId = null) } }
    fun clearHistory() { viewModelScope.launch { history.clear() } }
}
