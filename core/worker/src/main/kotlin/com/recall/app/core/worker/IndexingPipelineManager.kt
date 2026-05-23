package com.recall.app.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.recall.app.core.database.dao.AppSettingDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndexingPipelineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettingDao: AppSettingDao,
) {
    private val workManager = WorkManager.getInstance(context)

    fun startIntegrityCheck() {
        val integrityWork = OneTimeWorkRequestBuilder<IntegrityCheckWorker>()
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_INTEGRITY_CHECK,
            ExistingWorkPolicy.KEEP,
            integrityWork,
        )
    }

    fun startFullIndexing() {
        val integrityWork = OneTimeWorkRequestBuilder<IntegrityCheckWorker>()
            .build()

        val scanWork = OneTimeWorkRequestBuilder<MediaScanWorker>()
            .build()

        val embedWork = OneTimeWorkRequestBuilder<EmbeddingWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build(),
            )
            .build()

        workManager.beginUniqueWork(
            UNIQUE_INDEX_PIPELINE,
            ExistingWorkPolicy.KEEP,
            integrityWork,
        )
            .then(scanWork)
            .then(embedWork)
            .enqueue()
    }

    suspend fun startFullReindex() {
        appSettingDao.delete(AppSettingsKeys.LAST_MEDIA_SCAN_TIMESTAMP)
        workManager.cancelUniqueWork(UNIQUE_INDEX_PIPELINE)
        startFullIndexing()
    }

    fun startPeriodicScan() {
        val periodicWork = PeriodicWorkRequestBuilder<MediaScanWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SCAN,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork,
        )
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(UNIQUE_INDEX_PIPELINE)
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_SCAN)
        workManager.cancelUniqueWork(UNIQUE_INTEGRITY_CHECK)
    }

    fun observePipelineStatus(): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_INDEX_PIPELINE)
    }

    companion object {
        private const val UNIQUE_INDEX_PIPELINE = "recall-index-pipeline"
        private const val UNIQUE_PERIODIC_SCAN = "recall-periodic-scan"
        private const val UNIQUE_INTEGRITY_CHECK = "recall-integrity-check"
    }
}
