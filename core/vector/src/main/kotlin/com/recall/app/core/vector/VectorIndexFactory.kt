package com.recall.app.core.vector

import com.recall.app.core.vector.hnsw.HnswIndex
import com.recall.app.core.vector.linear.LinearScanIndex

object VectorIndexFactory {
    fun createLinearScan(dimensions: Int): VectorIndex {
        return LinearScanIndex(dimensions)
    }

    fun createHnsw(
        dimensions: Int,
        m: Int = 16,
        efConstruction: Int = 200,
        efSearch: Int = 50,
    ): VectorIndex = HnswIndex(dimensions, m, efConstruction, efSearch)
}
