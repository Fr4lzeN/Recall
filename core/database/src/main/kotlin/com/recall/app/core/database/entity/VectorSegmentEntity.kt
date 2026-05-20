package com.recall.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vector_segments")
data class VectorSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "vector_count") val vectorCount: Int,
    val dimensions: Int,
    @ColumnInfo(name = "quantization_type") val quantizationType: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_frozen") val isFrozen: Boolean = false,
    @ColumnInfo(name = "deleted_count") val deletedCount: Int = 0,
    @ColumnInfo(name = "total_count") val totalCount: Int = 0,
)
