package com.recall.app.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.recall.app.core.database.RecallDatabase
import com.recall.app.core.database.entity.MediaItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaItemDaoTest {
    private lateinit var db: RecallDatabase
    private lateinit var dao: MediaItemDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecallDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.mediaItemDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun upsertAll_insertsItems() = runTest {
        val items = listOf(
            sampleMediaItem(id = 1L, dateTaken = 100L),
            sampleMediaItem(id = 2L, dateTaken = 200L),
        )
        dao.upsertAll(items)

        assertEquals(2, dao.observeAll().first().size)
        assertNotNull(dao.getById(1L))
        assertNotNull(dao.getById(2L))
    }

    @Test
    fun observeAll_returnsItemsSortedByDateTakenDesc() = runTest {
        dao.upsertAll(
            listOf(
                sampleMediaItem(id = 1L, dateTaken = 100L),
                sampleMediaItem(id = 2L, dateTaken = 300L),
                sampleMediaItem(id = 3L, dateTaken = 200L),
            ),
        )

        val ids = dao.observeAll().first().map { it.id }
        assertEquals(listOf(2L, 3L, 1L), ids)
    }

    @Test
    fun getById_returnsCorrectItem() = runTest {
        val item = sampleMediaItem(id = 42L, displayName = "vacation.jpg", dateTaken = 999L)
        dao.upsertAll(listOf(item))

        val loaded = dao.getById(42L)
        assertNotNull(loaded)
        assertEquals("vacation.jpg", loaded!!.displayName)
        assertEquals(999L, loaded.dateTaken)
    }

    @Test
    fun getUnindexed_returnsOnlyUnindexedItems() = runTest {
        dao.upsertAll(
            listOf(
                sampleMediaItem(id = 1L, isIndexed = false),
                sampleMediaItem(id = 2L, isIndexed = true),
                sampleMediaItem(id = 3L, isIndexed = false, isDeleted = true),
                sampleMediaItem(id = 4L, isIndexed = false),
            ),
        )

        val unindexed = dao.getUnindexed(limit = 10)
        assertEquals(2, unindexed.size)
        assertEquals(setOf(1L, 4L), unindexed.map { it.id }.toSet())
    }

    @Test
    fun markIndexed_updatesFieldsCorrectly() = runTest {
        dao.upsertAll(listOf(sampleMediaItem(id = 1L, isIndexed = false)))

        dao.markIndexed(
            id = 1L,
            isIndexed = true,
            version = 3,
            segmentId = 7L,
            localIndex = 12,
        )

        val updated = dao.getById(1L)!!
        assertTrue(updated.isIndexed)
        assertEquals(3, updated.embeddingVersion)
        assertEquals(7L, updated.segmentId)
        assertEquals(12, updated.localVectorIndex)
    }

    @Test
    fun markDeleted_setsIsDeletedTrue() = runTest {
        dao.upsertAll(
            listOf(
                sampleMediaItem(id = 1L),
                sampleMediaItem(id = 2L),
            ),
        )

        dao.markDeleted(listOf(1L))

        val deleted = dao.getById(1L)!!
        assertTrue(deleted.isDeleted)
        assertFalse(dao.observeAll().first().any { it.id == 1L })
    }

    @Test
    fun getAllActiveIds_excludesDeletedItems() = runTest {
        dao.upsertAll(
            listOf(
                sampleMediaItem(id = 1L),
                sampleMediaItem(id = 2L),
                sampleMediaItem(id = 3L, isDeleted = true),
            ),
        )

        val activeIds = dao.getAllActiveIds()
        assertEquals(setOf(1L, 2L), activeIds.toSet())
    }

    private fun sampleMediaItem(
        id: Long,
        displayName: String = "photo_$id.jpg",
        dateTaken: Long? = 1000L,
        isIndexed: Boolean = false,
        isDeleted: Boolean = false,
    ) = MediaItemEntity(
        id = id,
        uri = "content://media/external/images/media/$id",
        displayName = displayName,
        dateTaken = dateTaken,
        dateAdded = 1000L,
        mimeType = "image/jpeg",
        width = 1920,
        height = 1080,
        size = 1024L,
        duration = null,
        isIndexed = isIndexed,
        isDeleted = isDeleted,
    )
}
