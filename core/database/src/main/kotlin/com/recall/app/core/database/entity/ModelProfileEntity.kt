package com.recall.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_profiles")
data class ModelProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dimensions: Int,
    @ColumnInfo(name = "quantization_type") val quantizationType: String,
    @ColumnInfo(name = "model_file_name") val modelFileName: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
)
