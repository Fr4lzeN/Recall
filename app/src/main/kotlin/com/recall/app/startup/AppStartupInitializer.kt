package com.recall.app.startup

import com.recall.app.core.worker.IndexingPipelineManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStartupInitializer @Inject constructor(
    private val pipelineManager: IndexingPipelineManager,
) {
    fun initialize() {
        pipelineManager.startFullIndexing()
    }
}
