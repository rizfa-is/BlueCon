package com.siosin.bluecon.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "device_history")
public class HistoryEntity {
    @PrimaryKey @NonNull public String id;
    @Nullable public String name;
    public int lastRssi;
    public long lastSeenAtMillis;

    public HistoryEntity(@NonNull String id, @Nullable String name, int lastRssi, long lastSeenAtMillis) {
        this.id = id;
        this.name = name;
        this.lastRssi = lastRssi;
        this.lastSeenAtMillis = lastSeenAtMillis;
    }
}
