package com.siosin.bluecon.domain.usecase.history

import com.siosin.bluecon.domain.model.DeviceHistory

/** Preserve the repository's most-recent-first ordering while searching. */
class SearchDeviceHistoryUseCase {
    operator fun invoke(devices: List<DeviceHistory>, query: String): List<DeviceHistory> {
        val search = query.trim()
        return devices.filter {
            search.isEmpty() || it.id.contains(search, ignoreCase = true) ||
                it.name.orEmpty().contains(search, ignoreCase = true)
        }
    }
}
