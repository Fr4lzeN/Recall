package com.recall.app.core.database

import com.recall.app.core.database.converter.Converters
import com.recall.app.core.database.entity.IndexingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun indexingStatus_roundTrips_allValues() {
        IndexingStatus.entries.forEach { status ->
            val stored = converters.fromIndexingStatus(status)
            assertEquals(status.name, stored)
            assertEquals(status, converters.toIndexingStatus(stored))
        }
    }

    @Test
    fun indexingStatus_deserializesKnownValues() {
        assertEquals(IndexingStatus.PENDING, converters.toIndexingStatus("PENDING"))
        assertEquals(IndexingStatus.PROCESSING, converters.toIndexingStatus("PROCESSING"))
        assertEquals(IndexingStatus.COMPLETED, converters.toIndexingStatus("COMPLETED"))
        assertEquals(IndexingStatus.FAILED, converters.toIndexingStatus("FAILED"))
    }
}
