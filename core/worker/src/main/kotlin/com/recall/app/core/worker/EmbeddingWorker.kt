package com.recall.app.core.worker

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.recall.app.core.database.dao.IndexingJobDao
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.database.entity.IndexingStatus
import com.recall.app.core.database.entity.IndexingJobEntity
import com.recall.app.core.media.thumbnail.ThumbnailLoader
import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.vector.PersistableVectorIndex
import com.recall.app.core.vector.VectorIndex
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

@HiltWorker
class EmbeddingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val indexingJobDao: IndexingJobDao,
    private val mediaItemDao: MediaItemDao,
    private val thumbnailLoader: ThumbnailLoader,
    private val embeddingModel: EmbeddingModel,
    private val vectorIndex: VectorIndex,
    private val writeCoordinator: IndexWriteCoordinator,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        indexPendingJobs()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Indexing failed")))
    }

    private suspend fun indexPendingJobs(): Result {
        embeddingModel.initializationError?.let { error ->
            return Result.failure(workDataOf(KEY_ERROR to error))
        }
        indexingJobDao.requeueProcessingJobs(System.currentTimeMillis())
        val startedAt = SystemClock.elapsedRealtime()
        var processed = 0
        var failed = 0
        val result = processPendingBatches(
            batchSize = BATCH_SIZE,
            loadBatch = { limit -> indexingJobDao.getByStatus(IndexingStatus.PENDING, limit) },
            shouldContinue = { SystemClock.elapsedRealtime() - startedAt < MAX_RUN_TIME_MS },
        ) { jobs ->
            val batchResult = processBatch(jobs) { completed, errors ->
                setProgress(workDataOf(KEY_PROCESSED to processed + completed, KEY_FAILED to failed + errors))
            }
            processed += batchResult.processed
            failed += batchResult.failed
            batchResult
        }
        return if (result.exhausted) {
            Result.success(workDataOf(KEY_PROCESSED to result.processed, KEY_FAILED to result.failed))
        } else {
            // Resume the durable queue before WorkManager's execution deadline cancels this worker.
            Result.retry()
        }
    }

    private suspend fun processBatch(
        jobs: List<IndexingJobEntity>,
        onProgress: suspend (Int, Int) -> Unit,
    ): BatchResult {
        var processed = 0
        var failed = 0
        val embeddings = mutableListOf<Pair<Long, FloatArray>>()
        val pendingCompletions = mutableListOf<PendingCompletion>()

        for (job in jobs) {
            currentCoroutineContext().ensureActive()
            try {
                indexingJobDao.updateStatus(
                    job.id,
                    IndexingStatus.PROCESSING,
                    System.currentTimeMillis(),
                )

                val mediaItem = mediaItemDao.getById(job.mediaItemId)
                if (mediaItem == null || mediaItem.isDeleted) {
                    failJob(job.id, "Media item not found")
                    failed++
                    continue
                }

                val bitmap = thumbnailLoader.loadThumbnail(Uri.parse(mediaItem.uri))
                if (bitmap == null) {
                    failJob(job.id, "Thumbnail load failed")
                    failed++
                    continue
                }

                try {
                    val embedding = embeddingModel.embedImage(bitmap)
                    embeddings.add(mediaItem.id to embedding)
                    pendingCompletions.add(
                        PendingCompletion(jobId = job.id, mediaItemId = mediaItem.id),
                    )
                    processed++
                } finally {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }

                onProgress(processed, failed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                indexingJobDao.updateStatus(
                    job.id,
                    IndexingStatus.FAILED,
                    System.currentTimeMillis(),
                    e.message,
                )
                failed++
            }
        }

        if (embeddings.isNotEmpty()) {
            writeCoordinator.mutate {
                currentCoroutineContext().ensureActive()
                val startIndex = vectorIndex.size()
                vectorIndex.addBatch(embeddings)
                // Never mark a job complete while its vector only exists in volatile staging memory.
                (vectorIndex as? PersistableVectorIndex)?.persist()
                currentCoroutineContext().ensureActive()
                val completedAt = System.currentTimeMillis()
                pendingCompletions.forEachIndexed { index, pending ->
                    mediaItemDao.markIndexed(
                        id = pending.mediaItemId,
                        isIndexed = true,
                        version = 1,
                        segmentId = 0,
                        localIndex = startIndex + index,
                    )
                    indexingJobDao.updateStatus(
                        pending.jobId,
                        IndexingStatus.COMPLETED,
                        completedAt,
                    )
                }
            }
        }

        return BatchResult(processed, failed)
    }

    private suspend fun failJob(jobId: Long, message: String) {
        indexingJobDao.updateStatus(
            jobId,
            IndexingStatus.FAILED,
            System.currentTimeMillis(),
            message,
        )
    }

    private data class PendingCompletion(
        val jobId: Long,
        val mediaItemId: Long,
    )

    companion object {
        const val BATCH_SIZE = 50
        private const val KEY_PROCESSED = "processed"
        private const val KEY_FAILED = "failed"
        const val KEY_ERROR = "error"
        private const val MAX_RUN_TIME_MS = 8 * 60 * 1000L
    }
}
