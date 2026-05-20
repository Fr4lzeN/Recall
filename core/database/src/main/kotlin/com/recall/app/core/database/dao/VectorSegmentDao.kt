package com.recall.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.recall.app.core.database.entity.VectorSegmentEntity

@Dao
interface VectorSegmentDao {
    @Query("SELECT * FROM vector_segments WHERE is_frozen = 1")
    suspend fun getActiveFrozenSegments(): List<VectorSegmentEntity>

    @Insert
    suspend fun insert(segment: VectorSegmentEntity): Long

    @Query("UPDATE vector_segments SET deleted_count = deleted_count + 1 WHERE id = :segmentId")
    suspend fun incrementDeletedCount(segmentId: Long)

    @Query(
        "SELECT * FROM vector_segments WHERE CAST(deleted_count AS REAL) / total_count > :threshold " +
            "AND is_frozen = 1",
    )
    suspend fun getSegmentsNeedingCompaction(threshold: Float): List<VectorSegmentEntity>

    @Delete
    suspend fun delete(segment: VectorSegmentEntity)
}
