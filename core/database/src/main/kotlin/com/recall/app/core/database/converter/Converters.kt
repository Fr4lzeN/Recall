package com.recall.app.core.database.converter

import androidx.room.TypeConverter
import com.recall.app.core.database.entity.IndexingStatus

class Converters {
    @TypeConverter
    fun fromIndexingStatus(value: IndexingStatus): String = value.name

    @TypeConverter
    fun toIndexingStatus(value: String): IndexingStatus = IndexingStatus.valueOf(value)
}
