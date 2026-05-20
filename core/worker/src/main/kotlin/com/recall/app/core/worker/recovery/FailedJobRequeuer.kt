package com.recall.app.core.worker.recovery

import com.recall.app.core.database.dao.IndexingJobDao
import com.recall.app.core.database.entity.IndexingStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FailedJobRequeuer @Inject constructor(
    private val indexingJobDao: IndexingJobDao,
) {
    companion object {
        const val MAX_RETRIES = 3
    }

    suspend fun requeueEligibleFailedJobs(): Int {
        val failedJobs = indexingJobDao.getByStatus(IndexingStatus.FAILED, Int.MAX_VALUE)
        val eligible = failedJobs.filter { it.retryCount < MAX_RETRIES }
        val now = System.currentTimeMillis()
        eligible.forEach { job ->
            indexingJobDao.updateStatus(
                id = job.id,
                status = IndexingStatus.PENDING,
                updatedAt = now,
                error = null,
            )
        }
        return eligible.size
    }
}
