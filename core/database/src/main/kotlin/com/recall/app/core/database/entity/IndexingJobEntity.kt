package com.recall.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "indexing_jobs",
    indices = [Index("status"), Index("media_item_id")],
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class IndexingJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "media_item_id") val mediaItemId: Long,
    val status: IndexingStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
)

enum class IndexingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}
