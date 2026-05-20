package com.recall.app.core.vector.segment

import com.recall.app.core.vector.hnsw.SegmentGraphExport
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

object SegmentFormat {
    val MAGIC_BYTES: ByteArray = byteArrayOf('R'.code.toByte(), 'V'.code.toByte(), 'S'.code.toByte(), 'F'.code.toByte())
    const val VERSION: Int = 1
    const val HEADER_SIZE: Int = 32
    const val FOOTER_SIZE: Int = 4

    const val OFFSET_MAGIC: Int = 0
    const val OFFSET_VERSION: Int = 4
    const val OFFSET_VECTOR_COUNT: Int = 8
    const val OFFSET_DIMENSIONS: Int = 12
    const val OFFSET_QUANT_TYPE: Int = 16
    const val OFFSET_ENTRY_POINT: Int = 20
    const val OFFSET_MAX_LEVEL: Int = 24
    const val OFFSET_M: Int = 28

    enum class QuantizationType(val code: Byte) {
        NONE(0),
        ;

        companion object {
            fun fromCode(code: Byte): QuantizationType {
                return entries.firstOrNull { it.code == code }
                    ?: throw SegmentFormatException("Unknown quantization type: $code")
            }
        }
    }

    fun vectorSectionOffset(): Int = HEADER_SIZE

    fun vectorSectionSize(vectorCount: Int, dimensions: Int): Int = vectorCount * dimensions * Float.SIZE_BYTES

    fun graphSectionOffset(vectorCount: Int, dimensions: Int): Int =
        vectorSectionOffset() + vectorSectionSize(vectorCount, dimensions)

    fun graphSectionSize(export: SegmentGraphExport): Int {
        var size = 0
        for (i in export.vectors.indices) {
            size += Int.SIZE_BYTES // level
            val layerCount = export.levels[i] + 1
            for (layer in 0 until layerCount) {
                val neighbors = export.neighborsByLayer[i][layer]
                size += Int.SIZE_BYTES + neighbors.size * Int.SIZE_BYTES
            }
        }
        return size
    }

    fun totalFileSize(vectorCount: Int, dimensions: Int, export: SegmentGraphExport): Int =
        HEADER_SIZE + vectorSectionSize(vectorCount, dimensions) + graphSectionSize(export) + FOOTER_SIZE

    fun writeHeader(
        buffer: ByteBuffer,
        vectorCount: Int,
        dimensions: Int,
        quantizationType: QuantizationType,
        entryPoint: Int,
        maxLevel: Int,
        m: Int,
    ) {
        buffer.put(MAGIC_BYTES)
        buffer.putInt(VERSION)
        buffer.putInt(vectorCount)
        buffer.putInt(dimensions)
        buffer.put(quantizationType.code)
        buffer.put(0)
        buffer.put(0)
        buffer.put(0)
        buffer.putInt(entryPoint)
        buffer.putInt(maxLevel)
        buffer.putInt(m)
    }

    fun readHeader(buffer: ByteBuffer): SegmentHeader {
        val magic = ByteArray(4)
        buffer.get(magic)
        if (!magic.contentEquals(MAGIC_BYTES)) {
            throw SegmentFormatException("Invalid magic bytes: ${magic.decodeToString()}")
        }
        val version = buffer.getInt()
        if (version != VERSION) {
            throw SegmentFormatException("Unsupported segment version: $version")
        }
        val vectorCount = buffer.getInt()
        val dimensions = buffer.getInt()
        val quantType = QuantizationType.fromCode(buffer.get())
        buffer.position(buffer.position() + 3) // padding
        val entryPoint = buffer.getInt()
        val maxLevel = buffer.getInt()
        val m = buffer.getInt()
        return SegmentHeader(
            vectorCount = vectorCount,
            dimensions = dimensions,
            quantizationType = quantType,
            entryPoint = entryPoint,
            maxLevel = maxLevel,
            m = m,
        )
    }

    fun writeVectors(buffer: ByteBuffer, vectors: List<FloatArray>) {
        vectors.forEach { vector ->
            vector.forEach { buffer.putFloat(it) }
        }
    }

    fun writeGraph(buffer: ByteBuffer, export: SegmentGraphExport) {
        for (i in export.vectors.indices) {
            buffer.putInt(export.levels[i])
            val layerCount = export.levels[i] + 1
            for (layer in 0 until layerCount) {
                val neighbors = export.neighborsByLayer[i][layer]
                buffer.putInt(neighbors.size)
                neighbors.forEach { buffer.putInt(it) }
            }
        }
    }

    fun computeCrc32(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        val crc = CRC32()
        crc.update(data, offset, length)
        return crc.value.toInt()
    }

    fun computeCrc32(buffer: ByteBuffer, length: Int): Int {
        val slice = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        slice.position(0)
        slice.limit(length)
        val bytes = ByteArray(length)
        slice.get(bytes)
        return computeCrc32(bytes)
    }

    fun validateChecksum(fileBytes: ByteArray) {
        if (fileBytes.size < HEADER_SIZE + FOOTER_SIZE) {
            throw SegmentFormatException("Segment file too small: ${fileBytes.size} bytes")
        }
        val expected = ByteBuffer.wrap(fileBytes, fileBytes.size - FOOTER_SIZE, FOOTER_SIZE)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
        val actual = computeCrc32(fileBytes, length = fileBytes.size - FOOTER_SIZE)
        if (expected != actual) {
            throw SegmentFormatException("Checksum mismatch: expected $expected, computed $actual")
        }
    }
}

data class SegmentHeader(
    val vectorCount: Int,
    val dimensions: Int,
    val quantizationType: SegmentFormat.QuantizationType,
    val entryPoint: Int,
    val maxLevel: Int,
    val m: Int,
)

class SegmentFormatException(message: String) : Exception(message)
