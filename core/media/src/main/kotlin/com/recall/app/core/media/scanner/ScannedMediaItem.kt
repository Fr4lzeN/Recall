package com.recall.app.core.media.scanner

import android.net.Uri

data class ScannedMediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTaken: Long?,
    val dateAdded: Long,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val duration: Long?,
    val bucketId: String,
)
