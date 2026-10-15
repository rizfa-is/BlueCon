package com.siosin.bluecon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val lastRssi: Int,
    val lastSeenAtMillis: Long,
)
