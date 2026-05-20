package com.recall.app.core.worker.recovery

import android.content.Context
import android.util.Log
import com.recall.app.core.database.dao.AppSettingDao
import com.recall.app.core.database.dao.IndexingJobDao
import com.recall.app.core.database.entity.IndexingStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartupIntegrityChecker @Inject constructor(
    private val indexingJobDao: IndexingJobDao,
    @Suppress("UnusedPrivateProperty") private val appSettingDao: AppSettingDao,
    @ApplicationContext private val context: Context,
) {
    suspend fun runChecks(): IntegrityReport {
        val issues = mutableListOf<String>()

        val requeued = requeueStuckJobs()
        if (requeued > 0) issues.add("Requeued $requeued stuck PROCESSING jobs")

        val cleaned = cleanOrphanedTempFiles()
        if (cleaned > 0) issues.add("Cleaned $cleaned orphaned temp files")

        verifySegmentsDirectory()

        val purged = purgeOldCompletedJobs()
        if (purged > 0) issues.add("Purged $purged old completed jobs")

        return IntegrityReport(
            checksRun = 4,
            issuesFound = issues.size,
            issues = issues,
            timestamp = System.currentTimeMillis(),
        )
    }

    private suspend fun requeueStuckJobs(): Int {
        val stuckJobs = indexingJobDao.getByStatus(IndexingStatus.PROCESSING, Int.MAX_VALUE)
        if (stuckJobs.isNotEmpty()) {
            indexingJobDao.requeueProcessingJobs(System.currentTimeMillis())
        }
        return stuckJobs.size
    }

    private fun cleanOrphanedTempFiles(): Int {
        val segmentsDir = File(context.filesDir, "segments")
        if (!segmentsDir.exists()) return 0
        val tempFiles = segmentsDir.listFiles { f -> f.name.endsWith(".tmp") } ?: return 0
        tempFiles.forEach { it.delete() }
        return tempFiles.size
    }

    private fun verifySegmentsDirectory() {
        val segmentsDir = File(context.filesDir, "segments")
        Log.i(TAG, "Segments directory exists: ${segmentsDir.exists()}")
    }

    private suspend fun purgeOldCompletedJobs(): Int {
        indexingJobDao.deleteCompleted()
        return 0
    }

    companion object {
        private const val TAG = "StartupIntegrityChecker"
    }
}

data class IntegrityReport(
    val checksRun: Int,
    val issuesFound: Int,
    val issues: List<String>,
    val timestamp: Long,
)
