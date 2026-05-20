package com.recall.app.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.recall.app.core.worker.recovery.FailedJobRequeuer
import com.recall.app.core.worker.recovery.StartupIntegrityChecker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class IntegrityCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val integrityChecker: StartupIntegrityChecker,
    private val failedJobRequeuer: FailedJobRequeuer,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
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
