package com.recall.app.core.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.recall.app.core.database.dao.IndexingJobDao
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.database.entity.IndexingStatus
import com.recall.app.core.media.thumbnail.ThumbnailLoader
import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.vector.VectorIndex
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EmbeddingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val indexingJobDao: IndexingJobDao,
    private val mediaItemDao: MediaItemDao,
    private val thumbnailLoader: ThumbnailLoader,
    private val embeddingModel: EmbeddingModel,
    private val vectorIndex: VectorIndex,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        indexingJobDao.requeueProcessingJobs(System.currentTimeMillis())

        val jobs = indexingJobDao.getByStatus(IndexingStatus.PENDING, BATCH_SIZE)
        if (jobs.isEmpty()) {
            return Result.success()
        }

        var processed = 0
        var failed = 0

        for (job in jobs) {
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
                    vectorIndex.add(mediaItem.id, embedding)

                    mediaItemDao.markIndexed(
                        id = mediaItem.id,
                        isIndexed = true,
                        version = 1,
                        segmentId = 0,
                        localIndex = vectorIndex.size() - 1,
                    )

                    indexingJobDao.updateStatus(
                        job.id,
                        IndexingStatus.COMPLETED,
                        System.currentTimeMillis(),
                    )
                    processed++
                } finally {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }

                setProgress(
                    workDataOf(
                        KEY_PROCESSED to processed,
                        KEY_TOTAL to jobs.size,
                    ),
                )
            } catch (e: Exception) {
                indexingJobDao.updateStatus(
                    job.id,
                    IndexingStatus.FAILED,
                    System.currentTimeMillis(),
                    e.message,
                )
                failed++
                if (job.retryCount >= MAX_RETRIES) continue
            }
        }

        val remaining = indexingJobDao.getByStatus(IndexingStatus.PENDING, 1)
        return if (remaining.isNotEmpty()) {
            Result.success(workDataOf(KEY_HAS_MORE to true))
        } else {
            Result.success(
                workDataOf(
                    KEY_PROCESSED to processed,
                    KEY_FAILED to failed,
                ),
            )
        }
    }

    private suspend fun failJob(jobId: Long, message: String) {
        indexingJobDao.updateStatus(
            jobId,
            IndexingStatus.FAILED,
            System.currentTimeMillis(),
            message,
        )
    }

    companion object {
        const val BATCH_SIZE = 20
        private const val MAX_RETRIES = 3
        private const val KEY_PROCESSED = "processed"
        private const val KEY_TOTAL = "total"
        private const val KEY_FAILED = "failed"
        private const val KEY_HAS_MORE = "has_more"
    }
}
