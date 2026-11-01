package com.recall.app.core.worker

import com.recall.app.core.database.dao.AppSettingDao
import com.recall.app.core.database.dao.IndexingJobDao
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.database.entity.AppSettingEntity
import com.recall.app.core.vector.VectorIndex
import javax.inject.Inject

class IndexResetter @Inject constructor(
    private val mediaItemDao: MediaItemDao,
    private val indexingJobDao: IndexingJobDao,
    private val appSettingDao: AppSettingDao,
    private val writeCoordinator: IndexWriteCoordinator,
) {
    suspend fun reset(index: VectorIndex, modelId: String?) = writeCoordinator.mutate {
        // An interrupted reset must never leave the old vectors marked as compatible.
        appSettingDao.delete(AppSettingsKeys.EMBEDDING_MODEL_ID)
        index.clear()
        mediaItemDao.markAllUnindexed()
        indexingJobDao.deleteAll()
        appSettingDao.delete(AppSettingsKeys.LAST_MEDIA_SCAN_TIMESTAMP)
        if (modelId != null) {
            appSettingDao.set(AppSettingEntity(AppSettingsKeys.EMBEDDING_MODEL_ID, modelId))
        }
    }
}
