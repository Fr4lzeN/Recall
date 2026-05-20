package com.recall.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "vector_postings",
    primaryKeys = ["media_item_id", "segment_id"],
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = VectorSegmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["segment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("segment_id")],
)
data class VectorPostingEntity(
    @ColumnInfo(name = "media_item_id") val mediaItemId: Long,
    @ColumnInfo(name = "segment_id") val segmentId: Long,
    @ColumnInfo(name = "local_index") val localIndex: Int,
    @ColumnInfo(name = "embedding_version") val embeddingVersion: Int,
)
