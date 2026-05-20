package com.recall.app.core.vector.bitmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DeletionBitmapTest {
    @Test
    fun markDeleted_andIsDeleted() {
        val bitmap = DeletionBitmap(capacity = 10)
        assertFalse(bitmap.isDeleted(3))
        bitmap.markDeleted(3)
        assertTrue(bitmap.isDeleted(3))
        assertFalse(bitmap.isDeleted(4))
    }

    @Test
    fun deletedCount_tracksMarkedEntries() {
        val bitmap = DeletionBitmap(capacity = 5)
        assertEquals(0, bitmap.deletedCount())
        bitmap.markDeleted(0)
        bitmap.markDeleted(2)
        assertEquals(2, bitmap.deletedCount())
        assertEquals(3, bitmap.liveCount())
    }

    @Test
    fun deadRatio_computesFraction() {
        val bitmap = DeletionBitmap(capacity = 4)
        assertEquals(0f, bitmap.deadRatio(), 1e-5f)
        bitmap.markDeleted(0)
        bitmap.markDeleted(1)
        assertEquals(0.5f, bitmap.deadRatio(), 1e-5f)
    }

    @Test
    fun clear_resetsAllBits() {
        val bitmap = DeletionBitmap(capacity = 3)
        bitmap.markDeleted(0)
        bitmap.markDeleted(2)
        bitmap.clear()
        assertEquals(0, bitmap.deletedCount())
        assertFalse(bitmap.isDeleted(0))
        assertFalse(bitmap.isDeleted(2))
    }

    @Test
    fun markDeleted_outOfRange_throws() {
        val bitmap = DeletionBitmap(capacity = 3)
        assertThrows(IllegalArgumentException::class.java) {
            bitmap.markDeleted(-1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            bitmap.markDeleted(3)
        }
    }

    @Test
    fun deadRatio_zeroCapacity_returnsZero() {
        val bitmap = DeletionBitmap(capacity = 0)
        assertEquals(0f, bitmap.deadRatio(), 1e-5f)
    }
}
