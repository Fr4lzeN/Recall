package com.recall.app.data.vector

import com.recall.app.core.database.dao.VectorSegmentDao
import com.recall.app.core.database.entity.VectorSegmentEntity
import com.recall.app.core.vector.segment.SegmentInfo
import com.recall.app.core.vector.segment.SegmentManifest

class RoomSegmentManifest(
    private val segmentDao: VectorSegmentDao,
) : SegmentManifest {

    override suspend fun getActiveSegments(): List<SegmentInfo> {
        return segmentDao.getActiveFrozenSegments().map { it.toSegmentInfo() }
    }

    override suspend fun addSegment(segment: SegmentInfo): Long {
        return segmentDao.insert(
            VectorSegmentEntity(
                filePath = segment.filePath,
                vectorCount = segment.vectorCount,
                dimensions = segment.dimensions,
                quantizationType = segment.quantizationType,
                createdAt = System.currentTimeMillis(),
                isFrozen = segment.isFrozen,
                deletedCount = segment.deletedCount,
                totalCount = segment.vectorCount,
            ),
        )
    }

    override suspend fun removeSegment(segmentId: Long) {
        val entity = segmentDao.getActiveFrozenSegments().firstOrNull { it.id == segmentId } ?: return
        segmentDao.delete(entity)
    }

    override suspend fun updateDeletedCount(segmentId: Long, deletedCount: Int) {
        segmentDao.setDeletedCount(segmentId, deletedCount)
    }

    private fun VectorSegmentEntity.toSegmentInfo() = SegmentInfo(
        id = id,
        filePath = filePath,
        vectorCount = vectorCount,
        dimensions = dimensions,
        quantizationType = quantizationType,
        isFrozen = isFrozen,
        deletedCount = deletedCount,
    )
}
