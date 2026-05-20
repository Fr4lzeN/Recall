package com.recall.app.core.vector.segment

data class VectorPosting(
    val mediaItemId: Long,
    val segmentId: Long,
    val localIndex: Int,
    val embeddingVersion: Int = 0,
)

interface VectorPostingStore {
    suspend fun getByMediaItem(mediaItemId: Long): VectorPosting?

    suspend fun getBySegment(segmentId: Long): List<VectorPosting>

    suspend fun insertAll(postings: List<VectorPosting>)

    suspend fun deleteByMediaItems(mediaItemIds: List<Long>)
}
