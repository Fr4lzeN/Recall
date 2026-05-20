package com.recall.app.core.vector.segment

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemorySegmentManifest : SegmentManifest {
    private val segments = ConcurrentHashMap<Long, SegmentInfo>()
    private val nextId = AtomicLong(1)

    override suspend fun getActiveSegments(): List<SegmentInfo> = segments.values.toList()

    override suspend fun addSegment(segment: SegmentInfo): Long {
        val id = if (segment.id > 0) segment.id else nextId.getAndIncrement()
        segments[id] = segment.copy(id = id)
        return id
    }

    override suspend fun removeSegment(segmentId: Long) {
        segments.remove(segmentId)
    }

    override suspend fun updateDeletedCount(segmentId: Long, deletedCount: Int) {
        val current = segments[segmentId] ?: return
        segments[segmentId] = current.copy(deletedCount = deletedCount)
    }
}

class InMemoryVectorPostingStore : VectorPostingStore {
    private val byMediaItem = ConcurrentHashMap<Long, VectorPosting>()
    private val bySegment = ConcurrentHashMap<Long, MutableList<VectorPosting>>()

    override suspend fun getByMediaItem(mediaItemId: Long): VectorPosting? = byMediaItem[mediaItemId]

    override suspend fun getBySegment(segmentId: Long): List<VectorPosting> =
        bySegment[segmentId]?.toList() ?: emptyList()

    override suspend fun insertAll(postings: List<VectorPosting>) {
        postings.forEach { posting ->
            byMediaItem[posting.mediaItemId] = posting
            bySegment.computeIfAbsent(posting.segmentId) { mutableListOf() }.add(posting)
        }
    }

    override suspend fun deleteByMediaItems(mediaItemIds: List<Long>) {
        mediaItemIds.forEach { id ->
            val posting = byMediaItem.remove(id) ?: return@forEach
            bySegment[posting.segmentId]?.removeAll { it.mediaItemId == id }
        }
    }
}
