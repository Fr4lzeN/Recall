package com.recall.app.feature.search

import com.recall.app.core.vector.SearchResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultSelectionTest {
    @Test
    fun `excluded photos do not consume top fifty and database rows keep vector rank`() = runTest {
        val ranked = (1L..100L).map { hit(it, 1f - it / 1000f) }
        val requests = mutableListOf<Int>()
        val loadedIds = mutableListOf<Long>()
        val results = SearchResultSelection.collect(
            indexSize = ranked.size,
            minSimilarity = 0.2f,
            excludedBucketIds = setOf("excluded"),
            searchCandidates = { count -> requests.add(count); ranked.take(count) },
            loadMedia = { ids ->
                loadedIds.addAll(ids)
                ids.reversed().map { media(it, if (it <= 50) "excluded" else "allowed") }
            },
        )
        assertEquals(listOf(50, 100), requests)
        assertEquals((51L..100L).toList(), results.map { it.mediaId })
        assertEquals(100, loadedIds.size)
        assertEquals(100, loadedIds.toSet().size)
    }

    @Test
    fun `unrelated nearest photos produce empty filtered results`() = runTest {
        var calls = 0
        val results = SearchResultSelection.collect(
            indexSize = 600,
            minSimilarity = 0.2f,
            excludedBucketIds = emptySet(),
            searchCandidates = { count -> calls++; (1L..count.toLong()).map { hit(it, 0.1f) } },
            loadMedia = { ids -> ids.map { media(it) } },
        )
        assertTrue(results.isEmpty())
        assertEquals(1, calls)
    }

    @Test
    fun `show nearest disables threshold without restoring deleted or duplicate rows`() = runTest {
        val candidates = listOf(hit(-1, 1f), hit(1, 0.1f), hit(1, 0.1f), hit(2, -0.2f), hit(3, -0.3f))
        val results = SearchResultSelection.collect(
            indexSize = candidates.size,
            minSimilarity = SearchResultSelection.NO_SIMILARITY_FILTER,
            excludedBucketIds = emptySet(),
            searchCandidates = { candidates },
            loadMedia = { ids -> ids.map { media(it).copy(isDeleted = it == 2L) } },
        )
        assertEquals(listOf(1L, 3L), results.map { it.mediaId })
    }

    @Test
    fun `missing rows are fetched only once during expansion`() = runTest {
        val ranked = (1L..100L).map { hit(it, 0.9f) }
        val loads = mutableListOf<Long>()
        val results = SearchResultSelection.collect(
            indexSize = 100,
            minSimilarity = 0.2f,
            excludedBucketIds = emptySet(),
            searchCandidates = { ranked.take(it) },
            loadMedia = { ids -> loads.addAll(ids); ids.filter { it > 50 }.map { media(it) } },
        )
        assertEquals((51L..100L).toList(), results.map { it.mediaId })
        assertEquals(100, loads.size)
    }

    @Test
    fun `nonfinite scores never appear in the grid`() = runTest {
        val hits = listOf(hit(1, Float.NaN), hit(2, Float.POSITIVE_INFINITY), hit(3, 0.9f))
        val results = SearchResultSelection.collect(
            indexSize = 3,
            minSimilarity = -1f,
            excludedBucketIds = emptySet(),
            searchCandidates = { hits },
            loadMedia = { ids -> ids.map { media(it) } },
        )
        assertEquals(listOf(3L), results.map { it.mediaId })
    }

    private fun hit(id: Long, score: Float) = SearchResult(id, score, 1f - score)

    private fun media(id: Long, bucketId: String = "allowed") = SearchableMedia(
        id = id,
        uri = "content://photos/$id",
        displayName = "$id.jpg",
        mimeType = "image/jpeg",
        dateTaken = id,
        bucketId = bucketId,
        isDeleted = false,
    )
}
