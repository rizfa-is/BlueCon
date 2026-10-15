package com.siosin.bluecon.domain.model

data class HistoryState(
    val devices: List<DeviceHistory> = emptyList(),
    val error: String? = null,
)
