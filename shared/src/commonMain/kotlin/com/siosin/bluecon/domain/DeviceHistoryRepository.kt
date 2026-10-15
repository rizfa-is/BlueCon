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

/** Preserve the repository's most-recent-first ordering while searching. */
fun filterDeviceHistory(devices: List<DeviceHistory>, query: String): List<DeviceHistory> {
    val search = query.trim()
    return devices.filter {
        search.isEmpty() || it.id.contains(search, ignoreCase = true) ||
            it.name.orEmpty().contains(search, ignoreCase = true)
    }
}
