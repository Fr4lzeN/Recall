package com.recall.app.core.worker

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class IndexWriteCoordinator @Inject constructor() {
    private val mutex = Mutex()

    suspend fun <T> mutate(action: suspend () -> T): T = mutex.withLock {
        // A cancelled worker must not repopulate an index that has just been reset.
        currentCoroutineContext().ensureActive()
        action()
    }
}
