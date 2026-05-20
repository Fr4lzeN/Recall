package com.recall.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recall.app.core.database.entity.IndexingJobEntity
import com.recall.app.core.database.entity.IndexingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface IndexingJobDao {
    @Query("SELECT * FROM indexing_jobs WHERE status = :status ORDER BY created_at ASC LIMIT :limit")
    suspend fun getByStatus(status: IndexingStatus, limit: Int): List<IndexingJobEntity>

    @Query("SELECT COUNT(*) FROM indexing_jobs WHERE status = :status")
    fun observeCountByStatus(status: IndexingStatus): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jobs: List<IndexingJobEntity>)

    @Query(
        "UPDATE indexing_jobs SET status = :status, updated_at = :updatedAt, " +
            "error_message = :error, retry_count = retry_count + 1 WHERE id = :id",
    )
    suspend fun updateStatus(
        id: Long,
        status: IndexingStatus,
        updatedAt: Long,
        error: String? = null,
    )

    @Query("UPDATE indexing_jobs SET status = 'PENDING', updated_at = :updatedAt WHERE status = 'PROCESSING'")
    suspend fun requeueProcessingJobs(updatedAt: Long)

    @Query("DELETE FROM indexing_jobs WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()
}
