package com.recall.app.core.database

import com.recall.app.core.database.dao.AppSettingDao
import com.recall.app.core.database.entity.AppSettingEntity
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcludedDirectoriesRepository @Inject constructor(
    private val appSettingDao: AppSettingDao,
) {
    suspend fun getExcludedBucketIds(): Set<String> {
        val raw = appSettingDao.getValue(EXCLUDED_BUCKET_IDS_KEY) ?: return emptySet()
        return parseBucketIds(raw)
    }

    suspend fun saveExcludedBucketIds(bucketIds: Set<String>) {
        appSettingDao.set(
            AppSettingEntity(
                key = EXCLUDED_BUCKET_IDS_KEY,
                value = JSONArray(bucketIds.toList()).toString(),
            ),
        )
    }

    private fun parseBucketIds(raw: String): Set<String> {
        return try {
            val array = JSONArray(raw)
            buildSet {
                for (index in 0 until array.length()) {
                    add(array.getString(index))
                }
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    private companion object {
        const val EXCLUDED_BUCKET_IDS_KEY = "excluded_bucket_ids"
    }
}
