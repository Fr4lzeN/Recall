package com.recall.app.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.recall.app.core.database.RecallDatabase
import com.recall.app.core.database.entity.IndexingJobEntity
import com.recall.app.core.database.entity.IndexingStatus
import com.recall.app.core.database.entity.MediaItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IndexingJobDaoTest {
    private lateinit var db: RecallDatabase
    private lateinit var jobDao: IndexingJobDao
    private lateinit var mediaDao: MediaItemDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecallDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        jobDao = db.indexingJobDao()
        mediaDao = db.mediaItemDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAll_andGetByStatus() = runTest {
        mediaDao.upsertAll(listOf(sampleMediaItem(1L), sampleMediaItem(2L), sampleMediaItem(3L)))
        val now = System.currentTimeMillis()
        jobDao.insertAll(
            listOf(
                sampleJob(mediaItemId = 1L, status = IndexingStatus.PENDING, createdAt = now),
                sampleJob(mediaItemId = 2L, status = IndexingStatus.COMPLETED, createdAt = now + 1),
                sampleJob(mediaItemId = 3L, status = IndexingStatus.PENDING, createdAt = now + 2),
            ),
        )

        val pending = jobDao.getByStatus(IndexingStatus.PENDING, limit = 10)
        assertEquals(2, pending.size)
        assertTrue(pending.all { it.status == IndexingStatus.PENDING })
        assertEquals(listOf(1L, 3L), pending.map { it.mediaItemId })
    }

    @Test
    fun updateStatus_changesStatusAndIncrementsRetryCount() = runTest {
        mediaDao.upsertAll(listOf(sampleMediaItem(1L)))
        jobDao.insertAll(
            listOf(
                sampleJob(
                    mediaItemId = 1L,
                    status = IndexingStatus.PENDING,
                    retryCount = 2,
                ),
            ),
        )
        val jobId = jobDao.getByStatus(IndexingStatus.PENDING, limit = 1).first().id
        val updatedAt = System.currentTimeMillis()

        jobDao.updateStatus(
            id = jobId,
            status = IndexingStatus.FAILED,
            updatedAt = updatedAt,
            error = "timeout",
        )

        val updated = jobDao.getByStatus(IndexingStatus.FAILED, limit = 1).single()
        assertEquals(IndexingStatus.FAILED, updated.status)
        assertEquals(3, updated.retryCount)
        assertEquals("timeout", updated.errorMessage)
        assertEquals(updatedAt, updated.updatedAt)
    }

    @Test
    fun requeueProcessingJobs_setsProcessingToPending() = runTest {
        mediaDao.upsertAll(listOf(sampleMediaItem(1L), sampleMediaItem(2L)))
        val now = System.currentTimeMillis()
        jobDao.insertAll(
            listOf(
                sampleJob(mediaItemId = 1L, status = IndexingStatus.PROCESSING, createdAt = now),
                sampleJob(mediaItemId = 2L, status = IndexingStatus.COMPLETED, createdAt = now),
            ),
        )
        val requeueAt = now + 1000

        jobDao.requeueProcessingJobs(requeueAt)

        val processing = jobDao.getByStatus(IndexingStatus.PROCESSING, limit = 10)
        assertTrue(processing.isEmpty())

        val pending = jobDao.getByStatus(IndexingStatus.PENDING, limit = 10)
        assertEquals(1, pending.size)
        assertEquals(IndexingStatus.PENDING, pending.first().status)
        assertEquals(requeueAt, pending.first().updatedAt)

        val completed = jobDao.getByStatus(IndexingStatus.COMPLETED, limit = 10)
        assertEquals(1, completed.size)
    }

    @Test
    fun deleteCompleted_removesCompletedJobs() = runTest {
        mediaDao.upsertAll(
            listOf(
                sampleMediaItem(1L),
                sampleMediaItem(2L),
                sampleMediaItem(3L),
            ),
        )
        jobDao.insertAll(
            listOf(
                sampleJob(mediaItemId = 1L, status = IndexingStatus.COMPLETED),
                sampleJob(mediaItemId = 2L, status = IndexingStatus.PENDING),
                sampleJob(mediaItemId = 3L, status = IndexingStatus.COMPLETED),
            ),
        )

        jobDao.deleteCompleted()

        assertTrue(jobDao.getByStatus(IndexingStatus.COMPLETED, limit = 10).isEmpty())
        assertEquals(1, jobDao.getByStatus(IndexingStatus.PENDING, limit = 10).size)
    }

    private fun sampleMediaItem(id: Long) = MediaItemEntity(
        id = id,
        uri = "content://media/$id",
        displayName = "photo_$id.jpg",
        dateTaken = 1000L,
        dateAdded = 1000L,
        mimeType = "image/jpeg",
        width = 100,
        height = 100,
        size = 1000L,
        duration = null,
    )

    private fun sampleJob(
        mediaItemId: Long,
        status: IndexingStatus,
        createdAt: Long = System.currentTimeMillis(),
        retryCount: Int = 0,
    ) = IndexingJobEntity(
        mediaItemId = mediaItemId,
        status = status,
        createdAt = createdAt,
        updatedAt = createdAt,
        retryCount = retryCount,
    )
}
