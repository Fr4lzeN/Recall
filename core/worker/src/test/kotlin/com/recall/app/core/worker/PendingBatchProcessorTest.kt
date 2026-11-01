package com.recall.app.core.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingBatchProcessorTest {
    @Test
    fun all600JobsAreCommittedInTwelveBatchesOf50() = runBlocking {
        val pending = (1..600).toMutableList()
        val committed = mutableListOf<Int>()
        val batchSizes = mutableListOf<Int>()
        val result = processPendingBatches(50, { limit -> pending.take(limit) }) { batch ->
            batchSizes += batch.size
            committed += batch
            pending.removeAll(batch.toSet())
            BatchResult(batch.size, 0)
        }
        assertEquals(List(12) { 50 }, batchSizes)
        assertEquals((1..600).toList(), committed)
        assertEquals(QueueResult(600, 0, true), result)
        assertTrue(pending.isEmpty())
    }

    @Test
    fun finalPartialBatchAndFailuresDoNotLeavePendingJobs() = runBlocking {
        val pending = (1..53).toMutableList()
        val batchSizes = mutableListOf<Int>()
        val result = processPendingBatches(50, { pending.take(it) }) { batch ->
            batchSizes += batch.size
            pending.removeAll(batch.toSet())
            val failures = batch.count { it % 10 == 0 }
            BatchResult(batch.size - failures, failures)
        }
        assertEquals(listOf(50, 3), batchSizes)
        assertEquals(QueueResult(48, 5, true), result)
    }

    @Test
    fun executionBudgetYieldsAndNextRunResumesRemainingQueue() = runBlocking {
        val pending = (1..125).toMutableList()
        var commits = 0
        suspend fun commit(batch: List<Int>): BatchResult {
            pending.removeAll(batch.toSet())
            commits++
            return BatchResult(batch.size, 0)
        }
        val first = processPendingBatches(50, { pending.take(it) }, { commits < 2 }, ::commit)
        assertEquals(QueueResult(100, 0, false), first)
        assertEquals((101..125).toList(), pending)
        val resumed = processPendingBatches(50, { pending.take(it) }, processBatch = ::commit)
        assertEquals(QueueResult(25, 0, true), resumed)
        assertTrue(pending.isEmpty())
    }

    @Test
    fun cancelledBatchDoesNotContinueOrLoseUncommittedJobs() = runBlocking {
        val pending = (1..100).toMutableList()
        var loads = 0
        var cancelled = false
        try {
            processPendingBatches(50, { limit -> loads++; pending.take(limit) }) {
                throw CancellationException("worker stopped")
            }
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertEquals(1, loads)
        assertEquals(100, pending.size)
    }

    @Test
    fun failedCommitStopsBeforeReadingAnotherBatch() = runBlocking {
        var loads = 0
        var failed = false
        try {
            processPendingBatches(50, { loads++; listOf(1, 2) }) {
                throw IllegalStateException("disk full")
            }
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue(failed)
        assertEquals(1, loads)
    }

    @Test
    fun emptyQueueDoesNotCommitAnything() = runBlocking {
        var committed = false
        val result = processPendingBatches<Int>(50, { emptyList() }) {
            committed = true
            BatchResult(0, 0)
        }
        assertFalse(committed)
        assertEquals(QueueResult(0, 0, true), result)
    }
}
