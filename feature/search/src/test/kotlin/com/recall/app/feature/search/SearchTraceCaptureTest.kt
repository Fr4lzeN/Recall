package com.recall.app.feature.search

import com.recall.app.core.common.diagnostics.DiagnosticText
import com.recall.app.core.vector.SearchResult
import com.recall.app.core.vector.VectorIndexDiagnosticSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SearchTraceCaptureTest {
    @Test
    fun `bounded trace retains all displayed photos past excluded candidates`() = runTest {
        val capture = capture(candidateLimit = 50)
        val candidates = (1L..150L).map { hit(it, 0.9f) }
        capture.onRound(150, candidates)
        candidates.forEachIndexed { rank, result ->
            capture.onCandidate(rank + 1, result, media(result.id), if (result.id <= 100) "excluded_directory" else "displayed")
        }
        val record = capture.finish(
            status = "completed", error = null, displayedIds = (101L..150L).toList(),
            indexSizeAtEnd = 150, endRevision = null,
            loadVector = { null }, loadMedia = { emptyList() },
        )
        assertEquals(50, record.candidates.size)
        assertEquals((101L..150L).toList(), record.candidates.map { it.mediaId })
        assertEquals(150, record.uniqueCandidateCount)
        assertEquals(50, record.rounds.single().candidateIds.size)
        assertEquals(150, record.rounds.single().returned)
    }

    @Test
    fun `exact reference compares raw candidates before relevance filtering`() = runTest {
        val capture = capture()
        val vector = floatArrayOf(1f, 0f)
        val tokenIds = intArrayOf(49406, 123, 49407)
        capture.embedding(vector, DiagnosticText("кот", tokenIds, listOf("<sot>", "кот", "<eot>"), 1, false))
        vector[0] = 0f
        tokenIds[1] = 999
        capture.snapshot(VectorIndexDiagnosticSnapshot(listOf(
            1L to floatArrayOf(1f, 0f),
            2L to floatArrayOf(0f, 1f),
            3L to floatArrayOf(1f, 1f),
        ), "exact", 7L))
        val raw = listOf(hit(1L, 1f), hit(3L, 0.70710677f), hit(2L, 0f))
        capture.onRound(3, raw)
        raw.forEachIndexed { rank, item -> capture.onCandidate(rank + 1, item, media(item.id), if (item.id == 1L) "displayed" else "below_similarity") }
        val record = capture.finish("completed", null, listOf(1L), 3, 7L,
            loadVector = { error("snapshot must supply vectors") }, loadMedia = { ids -> ids.map(::media) })
        assertEquals(1f, record.queryVector!![0], 0f)
        assertEquals(123, record.text!!.tokenIds!![1])
        assertEquals(1f, record.exactReference!!.recallAtK!!, 0f)
        assertEquals(3, record.exactReference!!.comparedTopK)
        assertTrue(record.exactReference!!.isStable)
        assertEquals(listOf(1L, 3L, 2L), record.exactReference!!.top.map { it.mediaId })
        assertEquals(1f, record.candidates.first().exactCosine!!, 0f)
    }

    @Test
    fun `concurrent index change invalidates recall interpretation`() = runTest {
        val capture = capture()
        capture.embedding(floatArrayOf(1f, 0f), DiagnosticText())
        capture.snapshot(VectorIndexDiagnosticSnapshot(listOf(1L to floatArrayOf(1f, 0f)), "exact", 7L))
        capture.onRound(1, listOf(hit(1, 1f)))
        val record = capture.finish("completed", null, listOf(1), 2, 8L,
            loadVector = { null }, loadMedia = { emptyList() })
        assertFalse(record.exactReference!!.isStable)
        assertNull(record.exactReference!!.recallAtK)
        assertEquals("index_changed_during_search", record.exactReference!!.reason)
    }

    @Test
    fun `unavailable vectors do not discard failed request diagnostic`() = runTest {
        val capture = capture()
        capture.onRound(1, listOf(hit(1, 0.4f)))
        capture.onCandidate(1, hit(1, 0.4f), null, "missing_metadata")
        val record = capture.finish("failed", "model failure", emptyList(), 1, null,
            loadVector = { error("storage failure") }, loadMedia = { error("metadata failure") })
        assertEquals("failed", record.status)
        assertEquals("model failure", record.error)
        assertNull(record.candidates.single().vector)
        assertFalse(record.exactReference!!.available)
    }

    @Test
    fun `cancelled request does not claim its selected photo was displayed`() = runTest {
        val capture = capture()
        capture.onRound(1, listOf(hit(1, 0.9f)))
        capture.onCandidate(1, hit(1, 0.9f), media(1), "displayed")
        val record = capture.finish("cancelled", null, emptyList(), 1, null,
            loadVector = { null }, loadMedia = { emptyList() })
        assertTrue(record.displayedIds.isEmpty())
        assertEquals("selected_before_cancelled", record.candidates.single().disposition)
    }

    private fun capture(candidateLimit: Int = 200) = SearchTraceCapture(
        query = "кот", modelName = "test", modelId = "test-v1", dimensions = 2,
        indexType = "test", indexSizeAtStart = 150, indexedMediaCount = 150,
        totalMediaCount = 150, minSimilarity = 0.8f, candidateLimit = candidateLimit,
    )

    private fun hit(id: Long, score: Float) = SearchResult(id, score, 1f - score)
    private fun media(id: Long) = SearchableMedia(id, "content://photos/$id", "$id.jpg", "image/jpeg", null, "allowed", false)
}
