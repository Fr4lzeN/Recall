package com.recall.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [
        Index("date_taken"),
        Index("is_indexed"),
        Index("mime_type"),
        Index("segment_id"),
    ],
)
data class MediaItemEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "date_taken") val dateTaken: Long?,
    @ColumnInfo(name = "date_added") val dateAdded: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val duration: Long?,
    @ColumnInfo(name = "is_indexed") val isIndexed: Boolean = false,
    @ColumnInfo(name = "embedding_version") val embeddingVersion: Int? = null,
    @ColumnInfo(name = "segment_id") val segmentId: Long? = null,
    @ColumnInfo(name = "local_vector_index") val localVectorIndex: Int? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
)
