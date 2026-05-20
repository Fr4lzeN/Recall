package com.recall.app.core.vector.segment

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class SegmentReader private constructor(
    private val file: File,
    private val accessFile: RandomAccessFile,
    private val channel: FileChannel,
    private val buffer: MappedByteBuffer,
    val header: SegmentHeader,
    private val graphNodeOffsets: IntArray,
) : Closeable {

    val vectorCount: Int get() = header.vectorCount
    val dimensions: Int get() = header.dimensions
    val entryPoint: Int get() = header.entryPoint
    val maxLevel: Int get() = header.maxLevel
    val m: Int get() = header.m

    fun readVector(localIndex: Int): FloatArray {
        require(localIndex in 0 until vectorCount) { "localIndex out of range: $localIndex" }
        val vectorOffset = SegmentFormat.vectorSectionOffset() + localIndex * dimensions * Float.SIZE_BYTES
        val vector = FloatArray(dimensions)
        val slice = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        slice.position(vectorOffset)
        for (i in 0 until dimensions) {
            vector[i] = slice.getFloat()
        }
        return vector
    }

    fun readNodeLevel(localIndex: Int): Int {
        require(localIndex in 0 until vectorCount) { "localIndex out of range: $localIndex" }
        val slice = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        slice.position(graphNodeOffsets[localIndex])
        return slice.getInt()
    }

    fun readNeighbors(localIndex: Int, layer: Int): List<Int> {
        require(localIndex in 0 until vectorCount) { "localIndex out of range: $localIndex" }
        val slice = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        slice.position(graphNodeOffsets[localIndex])
        val level = slice.getInt()
        require(layer in 0..level) { "layer $layer out of range for node level $level" }
        for (currentLayer in 0 until layer) {
            val count = slice.getInt()
            repeat(count) { slice.getInt() }
        }
        val neighborCount = slice.getInt()
        return List(neighborCount) { slice.getInt() }
    }

    override fun close() {
        channel.close()
        accessFile.close()
    }

    companion object {
        fun open(file: File): SegmentReader {
            require(file.exists() && file.length() > 0) { "Segment file does not exist: ${file.absolutePath}" }
            val raf = RandomAccessFile(file, "r")
            val channel = raf.channel
            val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            val headerSlice = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            val header = SegmentFormat.readHeader(headerSlice)
            val fileSize = channel.size().toInt()
            val fileBytes = ByteArray(fileSize)
            buffer.position(0)
            buffer.get(fileBytes, 0, fileSize)
            buffer.position(0)
            SegmentFormat.validateChecksum(fileBytes)

            val graphNodeOffsets = parseGraphOffsets(
                buffer = buffer,
                vectorCount = header.vectorCount,
                dimensions = header.dimensions,
            )

            return SegmentReader(file, raf, channel, buffer, header, graphNodeOffsets)
        }

        private fun parseGraphOffsets(
            buffer: MappedByteBuffer,
            vectorCount: Int,
            dimensions: Int,
        ): IntArray {
            val offsets = IntArray(vectorCount)
            var position = SegmentFormat.graphSectionOffset(vectorCount, dimensions)
            for (i in 0 until vectorCount) {
                offsets[i] = position
                val slice = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                slice.position(position)
                val level = slice.getInt()
                position += Int.SIZE_BYTES
                for (layer in 0..level) {
                    val count = slice.getInt()
                    position += Int.SIZE_BYTES + count * Int.SIZE_BYTES
                    slice.position(position)
                }
            }
            return offsets
        }
    }
}
