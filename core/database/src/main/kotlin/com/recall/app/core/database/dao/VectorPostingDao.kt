package com.recall.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recall.app.core.database.entity.VectorPostingEntity

@Dao
interface VectorPostingDao {
    @Query("SELECT * FROM vector_postings WHERE media_item_id = :mediaItemId")
    suspend fun getByMediaItem(mediaItemId: Long): VectorPostingEntity?

    @Query("SELECT * FROM vector_postings WHERE segment_id = :segmentId")
    suspend fun getBySegment(segmentId: Long): List<VectorPostingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(postings: List<VectorPostingEntity>)

    @Query("DELETE FROM vector_postings WHERE media_item_id IN (:mediaItemIds)")
    suspend fun deleteByMediaItems(mediaItemIds: List<Long>)
}
