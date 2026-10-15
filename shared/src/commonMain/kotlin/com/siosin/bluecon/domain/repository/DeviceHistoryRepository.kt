package com.siosin.bluecon.domain.repository

import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.domain.model.HistoryState
import kotlinx.coroutines.flow.StateFlow

interface DeviceHistoryRepository {
    val state: StateFlow<HistoryState>
    suspend fun save(devices: List<BleDevice>)
    suspend fun clear()
}
