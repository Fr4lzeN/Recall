package com.recall.app.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndexingPipelineManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    fun startFullIndexing() {
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
            scanWork,
        )
            .then(embedWork)
            .enqueue()
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
    }

    fun observePipelineStatus(): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_INDEX_PIPELINE)
    }

    companion object {
        private const val UNIQUE_INDEX_PIPELINE = "recall-index-pipeline"
        private const val UNIQUE_PERIODIC_SCAN = "recall-periodic-scan"
    }
}
