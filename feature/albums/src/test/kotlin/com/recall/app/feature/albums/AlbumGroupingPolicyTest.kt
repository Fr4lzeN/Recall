package com.recall.app.feature.albums

import com.recall.app.core.vector.clustering.Cluster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.CancellationException

class AlbumGroupingPolicyTest {
    private val cat = floatArrayOf(1f, 0f)
    private val dog = floatArrayOf(0f, 1f)
    private val concepts = mapOf("Cats" to cat, "Dogs" to dog)

    @Test
    fun `mixed group keeps generic name even when centroid favors cats`() {
        val name = AlbumGroupingPolicy.name(
            centroid = floatArrayOf(0.8f, 0.6f),
            memberVectors = listOf(cat, cat, dog, dog),
            conceptEmbeddings = concepts,
            fallbackName = "Album 1",
        )
        assertEquals("Album 1", name)
    }

    @Test
    fun `supported label requires agreement across group members`() {
        assertEquals(
            "Cats",
            AlbumGroupingPolicy.name(cat, listOf(cat, cat, cat, dog), concepts, "Album 1"),
        )
    }

    @Test
    fun `ambiguous centroid does not force a semantic name`() {
        assertEquals(
            "Album 2",
            AlbumGroupingPolicy.name(floatArrayOf(1f, 1f), listOf(cat, dog), concepts, "Album 2"),
        )
    }

    @Test
    fun `boundary photo can belong to two distinct albums`() {
        val boundary = floatArrayOf(1f, 1f)
        val clusters = listOf(Cluster(cat, listOf(1, 3)), Cluster(dog, listOf(2)))
        val overlapped = AlbumGroupingPolicy.withOverlap(
            clusters,
            listOf(1L to cat, 2L to dog, 3L to boundary),
        )
        assertEquals(listOf(1L, 3L), overlapped[0].memberIds)
        assertEquals(listOf(2L, 3L), overlapped[1].memberIds)
    }

    @Test
    fun `near duplicate centroids do not duplicate all members`() {
        val clusters = listOf(Cluster(cat, listOf(1)), Cluster(cat.copyOf(), listOf(2)))
        assertEquals(
            listOf(listOf(1L), listOf(2L)),
            AlbumGroupingPolicy.withOverlap(clusters, listOf(1L to cat, 2L to cat))
                .map { it.memberIds },
        )
    }

    @Test
    fun `a photograph gets at most one additional album`() {
        val a = floatArrayOf(1f, 1f, 0f)
        val b = floatArrayOf(1f, -1f, 0f)
        val c = floatArrayOf(1f, 0f, 1f)
        val boundary = floatArrayOf(1f, 0f, 0f)
        val clusters = listOf(Cluster(a, listOf(1)), Cluster(b, listOf(2)), Cluster(c, listOf(3)))
        val result = AlbumGroupingPolicy.withOverlap(clusters, listOf(1L to boundary))
        assertEquals(2, result.count { 1L in it.memberIds })
    }

    @Test
    fun `overlap processing stops when the album job is cancelled`() {
        var checks = 0
        assertThrows(CancellationException::class.java) {
            AlbumGroupingPolicy.withOverlap(
                clusters = listOf(Cluster(cat, listOf(1)), Cluster(dog, listOf(2))),
                vectors = List(100) { (it + 1).toLong() to floatArrayOf(1f, 1f) },
                checkCancellation = {
                    checks++
                    if (checks == 5) throw CancellationException("A newer album refresh started")
                },
            )
        }
        assertEquals(5, checks)
    }

    @Test
    fun `semantic naming stops when the album job is cancelled`() {
        var checks = 0
        assertThrows(CancellationException::class.java) {
            AlbumGroupingPolicy.name(
                centroid = cat,
                memberVectors = List(100) { cat },
                conceptEmbeddings = concepts,
                fallbackName = "Album 1",
                checkCancellation = {
                    checks++
                    if (checks == 5) throw CancellationException("A newer album refresh started")
                },
            )
        }
        assertEquals(5, checks)
    }
}
