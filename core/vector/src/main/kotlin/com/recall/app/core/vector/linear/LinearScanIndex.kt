package com.recall.app.core.vector.linear

import com.recall.app.core.vector.SearchResult
import com.recall.app.core.vector.VectorIndex
import com.recall.app.core.vector.distance.VectorDistance
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LinearScanIndex(
    private val dimensions: Int,
) : VectorIndex {

    private val mutex = Mutex()
    private val vectors = mutableMapOf<Long, FloatArray>()

    override suspend fun add(id: Long, vector: FloatArray) {
        require(vector.size == dimensions) { "Expected $dimensions dimensions, got ${vector.size}" }
        mutex.withLock { vectors[id] = vector.copyOf() }
    }

    override suspend fun addBatch(entries: List<Pair<Long, FloatArray>>) {
        mutex.withLock {
            entries.forEach { (id, vector) ->
                require(vector.size == dimensions)
                vectors[id] = vector.copyOf()
            }
        }
    }

    override suspend fun search(query: FloatArray, topK: Int): List<SearchResult> {
        require(query.size == dimensions)
        val snapshot = mutex.withLock { vectors.toMap() }

        return snapshot.map { (id, vector) ->
            val similarity = VectorDistance.cosineSimilarity(query, vector)
            SearchResult(id = id, score = similarity, distance = 1f - similarity)
        }
            .sortedByDescending { it.score }
            .take(topK)
    }

    override suspend fun remove(id: Long) {
        mutex.withLock { vectors.remove(id) }
    }

    override suspend fun contains(id: Long): Boolean {
        return mutex.withLock { vectors.containsKey(id) }
    }

    override fun size(): Int = vectors.size

    override fun dimensions(): Int = dimensions

    override fun clear() {
        vectors.clear()
    }
}
