package com.recall.app.core.media.sync

import com.recall.app.core.common.RecallDispatchers
import com.recall.app.core.media.scanner.MediaScanner
import com.recall.app.core.media.scanner.ScannedMediaItem
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSyncManager @Inject constructor(
    private val mediaScanner: MediaScanner,
    private val dispatchers: RecallDispatchers,
) {
    suspend fun performFullSync(): SyncResult = withContext(dispatchers.io) {
        SyncResult(items = mediaScanner.scanAll())
    }

    suspend fun scanSince(lastScanTimestamp: Long): List<ScannedMediaItem> = withContext(dispatchers.io) {
        mediaScanner.scanSince(lastScanTimestamp)
    }

    suspend fun detectDeletions(knownIds: List<Long>): List<Long> = withContext(dispatchers.io) {
        val currentIds = mediaScanner.scanAll().map { it.id }.toSet()
        knownIds.filter { it !in currentIds }
    }
}
