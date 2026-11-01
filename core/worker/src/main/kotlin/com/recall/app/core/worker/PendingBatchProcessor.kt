package com.recall.app.core.worker

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class BatchResult(val processed: Int, val failed: Int)

internal data class QueueResult(
    val processed: Int,
    val failed: Int,
    val exhausted: Boolean,
)

/** Commits bounded batches before fetching more work, so cancellation can resume from the queue. */
internal suspend fun <T> processPendingBatches(
    batchSize: Int,
    loadBatch: suspend (Int) -> List<T>,
    shouldContinue: () -> Boolean = { true },
    processBatch: suspend (List<T>) -> BatchResult,
): QueueResult {
    require(batchSize > 0)
    var processed = 0
    var failed = 0
    while (shouldContinue()) {
        currentCoroutineContext().ensureActive()
        val batch = loadBatch(batchSize)
        if (batch.isEmpty()) return QueueResult(processed, failed, exhausted = true)
        val result = processBatch(batch)
        require(result.processed + result.failed == batch.size) {
            "Every indexing job must finish or fail before loading the next batch"
        }
        processed += result.processed
        failed += result.failed
    }
    return QueueResult(processed, failed, exhausted = false)
}
