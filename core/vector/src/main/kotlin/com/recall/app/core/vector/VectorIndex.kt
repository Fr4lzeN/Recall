package com.recall.app.core.vector

data class SearchResult(
    val id: Long,          // mediaItemId
    val score: Float,      // similarity score (higher = more similar for cosine)
    val distance: Float,   // raw distance value
)

interface VectorIndex {
    suspend fun add(id: Long, vector: FloatArray)
    suspend fun addBatch(entries: List<Pair<Long, FloatArray>>)
    suspend fun search(query: FloatArray, topK: Int): List<SearchResult>
    suspend fun remove(id: Long)
    suspend fun contains(id: Long): Boolean
    fun size(): Int
    fun dimensions(): Int
    fun clear()
}
