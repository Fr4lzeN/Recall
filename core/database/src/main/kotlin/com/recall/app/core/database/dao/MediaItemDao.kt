package com.recall.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.recall.app.core.database.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items WHERE is_deleted = 0 ORDER BY date_taken DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE id IN (:ids) AND is_deleted = 0 ORDER BY date_taken DESC")
    suspend fun getByIds(ids: List<Long>): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE is_indexed = 1 AND is_deleted = 0")
    suspend fun getIndexed(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE is_indexed = 0 AND is_deleted = 0 LIMIT :limit")
    suspend fun getUnindexed(limit: Int): List<MediaItemEntity>

    @Query("SELECT COUNT(*) FROM media_items WHERE is_deleted = 0")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE is_indexed = 1 AND is_deleted = 0")
    fun observeIndexedCount(): Flow<Int>

    @Upsert
    suspend fun upsertAll(items: List<MediaItemEntity>)

    @Query(
        "UPDATE media_items SET is_indexed = :isIndexed, embedding_version = :version, " +
            "segment_id = :segmentId, local_vector_index = :localIndex WHERE id = :id",
    )
    suspend fun markIndexed(
        id: Long,
        isIndexed: Boolean,
        version: Int,
        segmentId: Long,
        localIndex: Int,
    )

    @Query("UPDATE media_items SET is_deleted = 1 WHERE id IN (:ids)")
    suspend fun markDeleted(ids: List<Long>)

    @Query("SELECT id FROM media_items WHERE is_deleted = 0")
    suspend fun getAllActiveIds(): List<Long>
}
