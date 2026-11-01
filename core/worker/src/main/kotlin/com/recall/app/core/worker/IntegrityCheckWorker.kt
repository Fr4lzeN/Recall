package com.recall.app.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.recall.app.core.worker.recovery.FailedJobRequeuer
import com.recall.app.core.worker.recovery.StartupIntegrityChecker
import com.recall.app.core.database.dao.AppSettingDao
import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.vector.VectorIndex
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class IntegrityCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val integrityChecker: StartupIntegrityChecker,
    private val failedJobRequeuer: FailedJobRequeuer,
    private val appSettingDao: AppSettingDao,
    private val embeddingModel: EmbeddingModel,
    private val vectorIndex: VectorIndex,
    private val indexResetter: IndexResetter,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        embeddingModel.initializationError?.let { error ->
            return Result.failure(workDataOf(EmbeddingWorker.KEY_ERROR to error))
        }
        if (appSettingDao.getValue(AppSettingsKeys.FORCE_REINDEX) == "true") {
            indexResetter.reset(vectorIndex, embeddingModel.modelId)
            appSettingDao.delete(AppSettingsKeys.FORCE_REINDEX)
        }
        val report = integrityChecker.runChecks()
        failedJobRequeuer.requeueEligibleFailedJobs()
        return Result.success(
            workDataOf(
                "checks_run" to report.checksRun,
                "issues_found" to report.issuesFound,
            ),
        )
    }
}
