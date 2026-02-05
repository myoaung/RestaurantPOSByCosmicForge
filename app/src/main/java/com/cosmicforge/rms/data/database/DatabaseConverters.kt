package com.cosmicforge.rms.data.database

import androidx.room.TypeConverter
import com.cosmicforge.rms.data.database.entities.SyncStatus
import java.util.Date

/**
 * Type converters for Room Database
 */
class DatabaseConverters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    // Sync Status converters for SyncQueueEntity
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return SyncStatus.valueOf(value)
    }
}
