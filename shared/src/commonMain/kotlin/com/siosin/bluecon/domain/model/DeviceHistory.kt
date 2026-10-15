package com.siosin.bluecon.domain.model

data class DeviceHistory(
    val id: String,
    val name: String?,
    val lastRssi: Int,
    val lastSeenAtMillis: Long,
)
