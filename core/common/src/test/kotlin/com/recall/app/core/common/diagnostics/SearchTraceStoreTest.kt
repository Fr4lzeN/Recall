package com.recall.app.core.common.diagnostics

import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SearchTraceStoreTest {
    @get:Rule val directory = TemporaryFolder()

    @Test
    fun russianQueryAndFullVectorsRoundTripWithoutLoss() {
        val record = sample().copy(
            query = "кот на пляже",
            text = DiagnosticText("кот на пляже", intArrayOf(49406, 123, 49407, 0), listOf("<SOT>", "кот", "<EOT>", "<PAD>"), 1, false),
            queryVector = floatArrayOf(0.6f, 0.8f),
            candidates = listOf(DiagnosticCandidate(42, 1, 0.9f, disposition = "displayed",
                uri = "content://media/42", displayName = "кот.jpg", vector = floatArrayOf(0.8f, 0.6f), exactCosine = 0.96f)),
            displayedIds = listOf(42),
        )
        val json = JSONObject(SearchTraceJson.encode(record))
        assertEquals(record.query, json.getString("query"))
        assertEquals(4, json.getJSONObject("text").getJSONArray("tokenIds").length())
        val vector = json.getJSONObject("queryVector")
        assertEquals(2, vector.getJSONArray("values").length())
        assertEquals(0.6f.toDouble(), vector.getJSONArray("values").getDouble(0), 0.0)
        assertEquals(1.0, vector.getDouble("norm"), 1e-6)
        assertEquals(64, vector.getString("sha256Float32LittleEndian").length)
        assertEquals("кот.jpg", json.getJSONArray("candidates").getJSONObject(0).getString("displayName"))
    }

    @Test
    fun invalidNumbersRemainDiagnosableAndDoNotBreakJson() {
        val json = JSONObject(SearchTraceJson.encode(sample().copy(
            queryVector = floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY, 1f),
            minSimilarity = Float.NaN,
        )))
        assertTrue(json.isNull("minSimilarity"))
        val vector = json.getJSONObject("queryVector")
        assertEquals(2, vector.getInt("nonFiniteCount"))
        assertTrue(vector.getJSONArray("values").isNull(0))
        assertTrue(vector.isNull("norm"))
    }

    @Test
    fun retentionKeepsLatestSearchesAndZipIsPortable() {
        val root = directory.newFolder()
        val store = SearchTraceStore(root, maxFiles = 2)
        (1..3).forEach { store.write(sample(it.toLong())) }
        assertEquals(2, root.listFiles()!!.size)
        val zip = export(store)
        assertEquals(3, zip.size)
        assertEquals(2, JSONObject(zip.getValue("manifest.json")).getInt("traceCount"))
        val traces = zip.filterKeys { it.startsWith("traces/") }.values.map(::JSONObject)
        assertEquals(setOf(2L, 3L), traces.map { it.getLong("timestampMillis") }.toSet())
        assertTrue(traces.all { it.getString("query") == "кот" })
    }

    @Test
    fun byteLimitEvictsOldDataAndOversizedTraceIsNotWritten() {
        val bytes = SearchTraceJson.encode(sample(1)).toByteArray().size
        val root = directory.newFolder()
        val store = SearchTraceStore(root, maxTotalBytes = bytes.toLong() + 16)
        store.write(sample(1))
        store.write(sample(2))
        assertEquals(1, root.listFiles()!!.size)
        assertEquals(2L, JSONObject(root.listFiles()!!.single().readText()).getLong("timestampMillis"))
        val tooSmall = SearchTraceStore(directory.newFolder(), maxFileBytes = 10)
        assertNull(tooSmall.write(sample()))
    }

    @Test
    fun identifiersCannotEscapePrivateDirectoryAndClearRemovesTraceData() {
        val root = directory.newFolder()
        val store = SearchTraceStore(root)
        val name = store.write(sample().copy(traceId = "../../elsewhere"))!!
        assertFalse(name.contains('/'))
        assertEquals(root.canonicalFile, root.resolve(name).canonicalFile.parentFile)
        store.clear()
        assertTrue(root.listFiles()!!.isEmpty())
        assertEquals(0, JSONObject(export(store).getValue("manifest.json")).getInt("traceCount"))
    }

    @Test
    fun boundedCaptureStatesWhatWasOmitted() {
        val candidates = (1..205).map { DiagnosticCandidate(it.toLong(), it, 0.5f, disposition = "below_similarity") }
        val json = JSONObject(SearchTraceJson.encode(sample().copy(candidates = candidates, uniqueCandidateCount = candidates.size)))
        assertEquals(200, json.getJSONArray("candidates").length())
        assertEquals(205, json.getInt("uniqueCandidateCount"))
        assertTrue(json.getBoolean("candidateCaptureTruncated"))
    }

    @Test
    fun duplicateObservationsDoNotHideMissingCandidateVectors() {
        val duplicate = DiagnosticCandidate(1, 1, 0.5f, disposition = "duplicate")
        val json = JSONObject(SearchTraceJson.encode(sample().copy(
            candidates = listOf(duplicate, duplicate.copy(rank = 2)), uniqueCandidateCount = 2,
        )))
        assertEquals(2, json.getInt("capturedCandidateCount"))
        assertEquals(1, json.getInt("capturedUniqueCandidateCount"))
        assertTrue(json.getBoolean("candidateCaptureTruncated"))
    }

    private fun export(store: SearchTraceStore): Map<String, String> {
        val output = ByteArrayOutputStream()
        store.exportTo(output)
        return buildMap {
            ZipInputStream(output.toByteArray().inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    put(entry.name, zip.readBytes().toString(Charsets.UTF_8))
                }
            }
        }
    }

    private fun sample(time: Long = 1) = SearchDiagnosticRecord(
        timestampMillis = time, durationMillis = 5, query = "кот", modelName = "test", modelId = "test-v1",
        dimensions = 2, indexType = "test", indexSizeAtStart = 1, indexSizeAtEnd = 1,
        indexedMediaCount = 1, totalMediaCount = 1, minSimilarity = 0.2f, status = "completed",
        traceId = "trace-$time",
    )
}
