package com.recall.app.core.media.sync

import com.recall.app.core.media.scanner.ScannedMediaItem

data class SyncResult(
    val items: List<ScannedMediaItem>,
)
