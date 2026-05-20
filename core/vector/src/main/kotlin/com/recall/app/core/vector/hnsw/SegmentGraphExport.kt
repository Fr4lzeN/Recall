package com.recall.app.core.vector.hnsw

data class SegmentGraphExport(
    val entryPoint: Int,
    val maxLevel: Int,
    val m: Int,
    val vectors: List<FloatArray>,
    val levels: IntArray,
    val neighborsByLayer: List<Array<List<Int>>>,
)
