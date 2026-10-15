package com.siosin.bluecon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HistoryDao {
    @Query("SELECT * FROM device_history ORDER BY lastSeenAtMillis DESC, id ASC")
    fun getAll(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(devices: List<HistoryEntity>)

    @Query("DELETE FROM device_history")
    fun clear()

    @Query("DELETE FROM device_history WHERE id NOT IN (SELECT id FROM device_history ORDER BY lastSeenAtMillis DESC, id ASC LIMIT 2000)")
    fun trim()
}
