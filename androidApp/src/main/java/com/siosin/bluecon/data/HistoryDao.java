package com.siosin.bluecon.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface HistoryDao {
    @Query("SELECT * FROM device_history ORDER BY lastSeenAtMillis DESC, id ASC")
    List<HistoryEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(List<HistoryEntity> devices);

    @Query("DELETE FROM device_history")
    void clear();

    @Query("DELETE FROM device_history WHERE id NOT IN (SELECT id FROM device_history ORDER BY lastSeenAtMillis DESC, id ASC LIMIT 2000)")
    void trim();
}
