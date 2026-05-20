package com.recall.app.core.vector.segment

interface SegmentManifest {
    suspend fun getActiveSegments(): List<SegmentInfo>
    suspend fun addSegment(segment: SegmentInfo)
    suspend fun removeSegment(segmentId: Long)
    suspend fun updateDeletedCount(segmentId: Long, deletedCount: Int)
}
