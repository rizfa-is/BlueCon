package com.siosin.bluecon.domain

import com.siosin.bluecon.bluetooth.BleDevice
import kotlinx.coroutines.flow.StateFlow

data class DeviceHistory(
    val id: String,
    val name: String?,
    val lastRssi: Int,
    val lastSeenAtMillis: Long,
)

data class HistoryState(
    val devices: List<DeviceHistory> = emptyList(),
    val error: String? = null,
)

interface DeviceHistoryRepository {
    val state: StateFlow<HistoryState>
    suspend fun save(devices: List<BleDevice>)
    suspend fun clear()
}
