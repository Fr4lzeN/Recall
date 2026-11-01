package com.recall.app.core.worker

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class IndexWriteCoordinatorTest {
    @Test
    fun resetWaitsForAnOngoingBatchCommit() = runBlocking {
        val coordinator = IndexWriteCoordinator()
        val releaseBatch = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val batch = launch(start = CoroutineStart.UNDISPATCHED) {
            coordinator.mutate {
                events += "batch started"
                releaseBatch.await()
                events += "batch committed"
            }
        }
        val reset = launch(start = CoroutineStart.UNDISPATCHED) {
            coordinator.mutate { events += "index reset" }
        }
        assertEquals(listOf("batch started"), events)
        releaseBatch.complete(Unit)
        batch.join()
        reset.join()
        assertEquals(listOf("batch started", "batch committed", "index reset"), events)
    }

    @Test
    fun cancelledWaitingBatchCannotRepopulateResetIndex() = runBlocking {
        val coordinator = IndexWriteCoordinator()
        val releaseReset = CompletableDeferred<Unit>()
        var wroteVector = false
        val reset = launch(start = CoroutineStart.UNDISPATCHED) {
            coordinator.mutate { releaseReset.await() }
        }
        val batch = launch(start = CoroutineStart.UNDISPATCHED) {
            coordinator.mutate { wroteVector = true }
        }
        batch.cancelAndJoin()
        releaseReset.complete(Unit)
        reset.join()
        assertFalse(wroteVector)
    }
}
