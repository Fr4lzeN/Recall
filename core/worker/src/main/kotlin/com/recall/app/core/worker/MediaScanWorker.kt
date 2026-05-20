package com.recall.app.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.recall.app.core.database.dao.AppSettingDao
import com.recall.app.core.database.dao.IndexingJobDao
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.database.entity.AppSettingEntity
import com.recall.app.core.database.entity.IndexingJobEntity
import com.recall.app.core.database.entity.IndexingStatus
import com.recall.app.core.database.entity.MediaItemEntity
import com.recall.app.core.media.scanner.MediaScanner
import com.recall.app.core.media.scanner.ScannedMediaItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MediaScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val mediaScanner: MediaScanner,
    private val mediaItemDao: MediaItemDao,
    private val indexingJobDao: IndexingJobDao,
    private val appSettingDao: AppSettingDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val lastScanTimestamp = appSettingDao
            .getValue(AppSettingsKeys.LAST_MEDIA_SCAN_TIMESTAMP)
            ?.toLongOrNull()
            ?: 0L
        val isFullScan = lastScanTimestamp == 0L

        setProgress(workDataOf(KEY_PHASE to PHASE_SCANNING))

        val scanned = if (isFullScan) {
            mediaScanner.scanAll()
        } else {
            mediaScanner.scanSince(lastScanTimestamp)
        }

        setProgress(
            workDataOf(
                KEY_PHASE to PHASE_UPSERTING,
                KEY_SCANNED to scanned.size,
            ),
        )

        val existingById = if (scanned.isEmpty()) {
            emptyMap()
        } else {
            scanned.mapNotNull { item ->
                mediaItemDao.getById(item.id)?.let { item.id to it }
            }.toMap()
        }

        val entities = scanned.map { it.toEntity(existingById[it.id]) }
        if (entities.isNotEmpty()) {
            mediaItemDao.upsertAll(entities)
        }

        var deletedCount = 0
        if (isFullScan) {
            val scannedIds = scanned.map { it.id }.toSet()
            val activeIds = mediaItemDao.getAllActiveIds()
            val deletedIds = activeIds.filter { it !in scannedIds }
            if (deletedIds.isNotEmpty()) {
                mediaItemDao.markDeleted(deletedIds)
                deletedCount = deletedIds.size
            }
        }

        setProgress(workDataOf(KEY_PHASE to PHASE_QUEUEING))

        val activeJobMediaIds = buildSet {
            indexingJobDao.getByStatus(IndexingStatus.PENDING, ACTIVE_JOB_LOOKUP_LIMIT)
                .forEach { add(it.mediaItemId) }
            indexingJobDao.getByStatus(IndexingStatus.PROCESSING, ACTIVE_JOB_LOOKUP_LIMIT)
                .forEach { add(it.mediaItemId) }
        }

        val now = System.currentTimeMillis()
        val newJobs = entities
            .filter { !it.isIndexed && !it.isDeleted && it.id !in activeJobMediaIds }
            .map { entity ->
                IndexingJobEntity(
                    mediaItemId = entity.id,
                    status = IndexingStatus.PENDING,
                    createdAt = now,
                    updatedAt = now,
                )
            }

        if (newJobs.isNotEmpty()) {
            indexingJobDao.insertAll(newJobs)
        }

        appSettingDao.set(
            AppSettingEntity(
                key = AppSettingsKeys.LAST_MEDIA_SCAN_TIMESTAMP,
                value = now.toString(),
            ),
        )

        setProgress(
            workDataOf(
                KEY_PHASE to PHASE_DONE,
                KEY_NEW_ITEMS to newJobs.size,
                KEY_DELETED_ITEMS to deletedCount,
            ),
        )

        return Result.success(
            workDataOf(
                KEY_NEW_ITEMS to newJobs.size,
                KEY_DELETED_ITEMS to deletedCount,
            ),
        )
    }

    private fun ScannedMediaItem.toEntity(existing: MediaItemEntity?): MediaItemEntity {
        return MediaItemEntity(
            id = id,
            uri = uri.toString(),
            displayName = displayName,
            dateTaken = dateTaken,
            dateAdded = dateAdded,
            mimeType = mimeType,
            width = width,
            height = height,
            size = size,
            duration = duration,
            isIndexed = existing?.isIndexed ?: false,
            embeddingVersion = existing?.embeddingVersion,
            segmentId = existing?.segmentId,
            localVectorIndex = existing?.localVectorIndex,
            isDeleted = false,
        )
    }

    companion object {
        private const val ACTIVE_JOB_LOOKUP_LIMIT = 10_000
        private const val KEY_PHASE = "phase"
        private const val KEY_SCANNED = "scanned"
        private const val KEY_NEW_ITEMS = "new_items"
        private const val KEY_DELETED_ITEMS = "deleted_items"
        private const val PHASE_SCANNING = "scanning"
        private const val PHASE_UPSERTING = "upserting"
        private const val PHASE_QUEUEING = "queueing"
        private const val PHASE_DONE = "done"
    }
}
