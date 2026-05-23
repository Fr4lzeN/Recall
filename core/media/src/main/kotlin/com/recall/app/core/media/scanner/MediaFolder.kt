package com.recall.app.core.media.scanner

data class MediaFolder(
    val bucketId: String,
    val displayName: String,
    val path: String,
    val itemCount: Int,
    val coverUris: List<String>,
)
