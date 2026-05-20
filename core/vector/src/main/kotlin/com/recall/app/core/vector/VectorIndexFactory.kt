package com.recall.app.core.vector

import com.recall.app.core.vector.linear.LinearScanIndex

object VectorIndexFactory {
    fun createLinearScan(dimensions: Int): VectorIndex {
        return LinearScanIndex(dimensions)
    }
}
