package com.recall.app.core.vector.segment

import com.recall.app.core.vector.hnsw.HnswIndex
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

object SegmentWriter {
    suspend fun write(
        outputDir: File,
        segmentId: Long,
        entries: List<Pair<Long, FloatArray>>,
        dimensions: Int,
        quantizationType: SegmentFormat.QuantizationType = SegmentFormat.QuantizationType.NONE,
        m: Int = 16,
        efConstruction: Int = 200,
    ): SegmentInfo {
        require(entries.isNotEmpty()) { "Cannot write empty segment" }
        require(entries.all { it.second.size == dimensions }) {
            "All vectors must have $dimensions dimensions"
        }

        outputDir.mkdirs()
        val fileName = "segment_${segmentId}.rvsf"
        val finalFile = File(outputDir, fileName)
        val tmpFile = File(outputDir, "$fileName.tmp")

        val index = HnswIndex(dimensions = dimensions, m = m, efConstruction = efConstruction)
        val localEntries = entries.mapIndexed { localIndex, (_, vector) ->
            localIndex.toLong() to vector.copyOf()
        }
        index.addBatch(localEntries)

        val export = index.exportForSegment(entries.size)
        val vectorCount = entries.size
        val fileSize = SegmentFormat.totalFileSize(vectorCount, dimensions, export)
        val bytes = ByteArray(fileSize)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        SegmentFormat.writeHeader(
            buffer = buffer,
            vectorCount = vectorCount,
            dimensions = dimensions,
            quantizationType = quantizationType,
            entryPoint = export.entryPoint,
            maxLevel = export.maxLevel,
            m = export.m,
        )
        SegmentFormat.writeVectors(buffer, export.vectors)
        SegmentFormat.writeGraph(buffer, export)

        val checksum = SegmentFormat.computeCrc32(bytes, length = fileSize - SegmentFormat.FOOTER_SIZE)
        buffer.putInt(checksum)

        FileChannel.open(tmpFile.toPath(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING).use { channel ->
            channel.write(ByteBuffer.wrap(bytes))
        }

        if (finalFile.exists()) {
            finalFile.delete()
        }
        if (!tmpFile.renameTo(finalFile)) {
            tmpFile.delete()
            error("Failed to rename segment temp file to ${finalFile.absolutePath}")
        }

        return SegmentInfo(
            id = segmentId,
            filePath = finalFile.absolutePath,
            vectorCount = vectorCount,
            dimensions = dimensions,
            quantizationType = quantizationType.name,
            isFrozen = true,
            deletedCount = 0,
        )
    }
}
