package com.recall.app.core.vector.segment

data class SegmentInfo(
    val id: Long,
    val filePath: String,
    val vectorCount: Int,
    val dimensions: Int,
    val quantizationType: String,
    val isFrozen: Boolean,
    val deletedCount: Int,
)
