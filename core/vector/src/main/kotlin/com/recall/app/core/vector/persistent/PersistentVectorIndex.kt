package com.recall.app.core.vector.persistent

import com.recall.app.core.vector.PersistableVectorIndex
import com.recall.app.core.vector.SearchResult
import com.recall.app.core.vector.hnsw.HnswIndex
import java.io.File

class PersistentVectorIndex(
    private val dimensions: Int,
    private val indexFile: File,
    m: Int = 16,
    efConstruction: Int = 200,
    efSearch: Int = 50,
) : PersistableVectorIndex {

    private var index: HnswIndex

    init {
        index = if (indexFile.exists() && indexFile.length() > 0) {
            try {
                HnswIndex.deserialize(indexFile.readBytes(), dimensions)
            } catch (_: Exception) {
                indexFile.delete()
                HnswIndex(dimensions, m, efConstruction, efSearch)
            }
        } else {
            HnswIndex(dimensions, m, efConstruction, efSearch)
        }
    }

    override suspend fun add(id: Long, vector: FloatArray) {
        index.add(id, vector)
    }

    override suspend fun addBatch(entries: List<Pair<Long, FloatArray>>) {
        index.addBatch(entries)
    }

    override suspend fun search(query: FloatArray, topK: Int): List<SearchResult> {
        return index.search(query, topK)
    }

    override suspend fun remove(id: Long) {
        index.remove(id)
    }

    override suspend fun getVector(id: Long): FloatArray? = index.getVector(id)

    override suspend fun contains(id: Long): Boolean = index.contains(id)

    override fun size(): Int = index.size()

    override fun dimensions(): Int = index.dimensions()

    override fun clear() {
        index.clear()
        indexFile.delete()
    }

    override fun persist() {
        val bytes = index.serialize()
        val tmpFile = File(indexFile.parent, "${indexFile.name}.tmp")
        tmpFile.writeBytes(bytes)
        tmpFile.renameTo(indexFile)
    }
}
