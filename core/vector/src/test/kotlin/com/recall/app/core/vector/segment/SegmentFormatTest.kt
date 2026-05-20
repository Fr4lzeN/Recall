package com.recall.app.core.vector.segment

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SegmentFormatTest {

    @Test
    fun validateChecksum_acceptsValidFile() {
        val dir = createTempDir()
        val segment = writeMinimalSegment(dir, vectorCount = 2, dimensions = 4)
        SegmentFormat.validateChecksum(segment.readBytes())
        segment.delete()
        dir.deleteRecursively()
    }

    @Test
    fun validateChecksum_rejectsCorruptChecksum() {
        val dir = createTempDir()
        val segment = writeMinimalSegment(dir, vectorCount = 2, dimensions = 4)
        val bytes = segment.readBytes()
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0xFF).toByte()
        val ex = assertThrows(SegmentFormatException::class.java) {
            SegmentFormat.validateChecksum(bytes)
        }
        assertTrue(ex.message!!.contains("Checksum mismatch"))
        segment.delete()
        dir.deleteRecursively()
    }

    @Test
    fun readHeader_rejectsInvalidMagic() {
        val buffer = ByteBuffer.allocate(SegmentFormat.HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("BAD!".toByteArray())
        buffer.rewind()
        assertThrows(SegmentFormatException::class.java) {
            SegmentFormat.readHeader(buffer)
        }
    }

    private fun writeMinimalSegment(dir: File, vectorCount: Int, dimensions: Int): File {
        val vectors = List(vectorCount) { FloatArray(dimensions) { 0.1f * (it + 1) } }
        val export = com.recall.app.core.vector.hnsw.SegmentGraphExport(
            entryPoint = 0,
            maxLevel = 0,
            m = 16,
            vectors = vectors,
            levels = IntArray(vectorCount),
            neighborsByLayer = List(vectorCount) { arrayOf(emptyList()) },
        )
        val size = SegmentFormat.totalFileSize(vectorCount, dimensions, export)
        val bytes = ByteArray(size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        SegmentFormat.writeHeader(
            buffer = buffer,
            vectorCount = vectorCount,
            dimensions = dimensions,
            quantizationType = SegmentFormat.QuantizationType.NONE,
            entryPoint = 0,
            maxLevel = 0,
            m = 16,
        )
        SegmentFormat.writeVectors(buffer, vectors)
        SegmentFormat.writeGraph(buffer, export)
        val checksum = SegmentFormat.computeCrc32(bytes, length = size - SegmentFormat.FOOTER_SIZE)
        buffer.putInt(checksum)
        val file = File(dir, "test.rvsf")
        file.writeBytes(bytes)
        return file
    }

    private fun createTempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "segment-format-${System.nanoTime()}")
        dir.mkdirs()
        return dir
    }
}
