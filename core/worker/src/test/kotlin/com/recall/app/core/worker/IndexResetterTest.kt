package com.recall.app.core.worker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.recall.app.core.database.RecallDatabase
import com.recall.app.core.database.entity.AppSettingEntity
import com.recall.app.core.database.entity.IndexingJobEntity
import com.recall.app.core.database.entity.IndexingStatus
import com.recall.app.core.database.entity.MediaItemEntity
import com.recall.app.core.vector.VectorIndex
import com.recall.app.core.vector.linear.LinearScanIndex
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IndexResetterTest {
    private lateinit var database: RecallDatabase
    private lateinit var index: LinearScanIndex
    private lateinit var resetter: IndexResetter

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RecallDatabase::class.java)
            .allowMainThreadQueries().build()
        index = LinearScanIndex(3)
        index.add(1L, floatArrayOf(1f, 0f, 0f))
        database.mediaItemDao().upsertAll(listOf(MediaItemEntity(
            id = 1L, uri = "content://media/1", displayName = "kept.jpg", dateTaken = null,
            dateAdded = 0L, mimeType = "image/jpeg", width = 1, height = 1, size = 1L,
            duration = null, isIndexed = true, embeddingVersion = 1, segmentId = 1L, localVectorIndex = 0,
        )))
        database.indexingJobDao().insertAll(listOf(IndexingJobEntity(
            mediaItemId = 1L, status = IndexingStatus.COMPLETED, createdAt = 0L, updatedAt = 0L,
        )))
        database.appSettingDao().set(AppSettingEntity(AppSettingsKeys.EMBEDDING_MODEL_ID, "old-model"))
        database.appSettingDao().set(AppSettingEntity(AppSettingsKeys.LAST_MEDIA_SCAN_TIMESTAMP, "123"))
        resetter = IndexResetter(database.mediaItemDao(), database.indexingJobDao(), database.appSettingDao(), IndexWriteCoordinator())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun resetInvalidatesVectorsAndJobsWhilePreservingMediaAndSetsNewIdentity() = runBlocking {
        resetter.reset(index, "new-model")
        assertEquals(0, index.size())
        val item = database.mediaItemDao().getById(1L)!!
        assertEquals("kept.jpg", item.displayName)
        assertFalse(item.isIndexed)
        assertNull(item.localVectorIndex)
        assertTrue(database.indexingJobDao().getByStatus(IndexingStatus.COMPLETED, 10).isEmpty())
        assertNull(database.appSettingDao().getValue(AppSettingsKeys.LAST_MEDIA_SCAN_TIMESTAMP))
        assertEquals("new-model", database.appSettingDao().getValue(AppSettingsKeys.EMBEDDING_MODEL_ID))
    }

    @Test
    fun failedResetNeverCertifiesIncompatibleVectorsAsCurrent() = runBlocking {
        val brokenIndex = object : VectorIndex by index {
            override fun clear() = throw IllegalStateException("storage unavailable")
        }
        try {
            resetter.reset(brokenIndex, "new-model")
            fail("reset must report storage failure")
        } catch (_: IllegalStateException) {
            assertNull(database.appSettingDao().getValue(AppSettingsKeys.EMBEDDING_MODEL_ID))
            assertTrue(database.mediaItemDao().getById(1L)!!.isIndexed)
        }
    }
}
