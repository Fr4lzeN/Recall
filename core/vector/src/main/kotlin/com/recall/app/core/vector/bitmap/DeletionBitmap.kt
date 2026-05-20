package com.recall.app.core.vector.bitmap

import java.util.BitSet

class DeletionBitmap(private val capacity: Int) {
    private val bitset = BitSet(capacity)

    fun markDeleted(index: Int) {
        require(index in 0 until capacity)
        bitset.set(index)
    }

    fun isDeleted(index: Int): Boolean = bitset.get(index)

    fun deletedCount(): Int = bitset.cardinality()

    fun liveCount(): Int = capacity - deletedCount()

    fun deadRatio(): Float = if (capacity > 0) deletedCount().toFloat() / capacity else 0f

    fun clear() = bitset.clear()
}
