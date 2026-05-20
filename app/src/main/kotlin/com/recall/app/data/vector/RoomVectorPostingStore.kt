package com.recall.app.data.vector

import com.recall.app.core.database.dao.VectorPostingDao
import com.recall.app.core.database.entity.VectorPostingEntity
import com.recall.app.core.vector.segment.VectorPosting
import com.recall.app.core.vector.segment.VectorPostingStore

class RoomVectorPostingStore(
    private val postingDao: VectorPostingDao,
) : VectorPostingStore {

    override suspend fun getByMediaItem(mediaItemId: Long): VectorPosting? {
        return postingDao.getByMediaItem(mediaItemId)?.toPosting()
    }

    override suspend fun getBySegment(segmentId: Long): List<VectorPosting> {
        return postingDao.getBySegment(segmentId).map { it.toPosting() }
    }

    override suspend fun insertAll(postings: List<VectorPosting>) {
        postingDao.insertAll(
            postings.map {
                VectorPostingEntity(
                    mediaItemId = it.mediaItemId,
                    segmentId = it.segmentId,
                    localIndex = it.localIndex,
                    embeddingVersion = it.embeddingVersion,
                )
            },
        )
    }

    override suspend fun deleteByMediaItems(mediaItemIds: List<Long>) {
        if (mediaItemIds.isNotEmpty()) {
            postingDao.deleteByMediaItems(mediaItemIds)
        }
    }

    private fun VectorPostingEntity.toPosting() = VectorPosting(
        mediaItemId = mediaItemId,
        segmentId = segmentId,
        localIndex = localIndex,
        embeddingVersion = embeddingVersion,
    )
}
