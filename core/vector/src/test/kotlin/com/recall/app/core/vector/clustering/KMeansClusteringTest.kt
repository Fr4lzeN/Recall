package com.recall.app.core.vector.clustering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class KMeansClusteringTest {

    @Test
    fun cluster_threeSeparatedGroups_producesThreeClusters() {
        val items = buildSeparatedClusters()
        val result = KMeansClustering.cluster(items, k = 3)

        assertEquals(3, result.size)
        val clusterMembers = result.map { it.memberIds.toSet() }.toSet()
        assertTrue(
            clusterMembers.contains(setOf(1L, 2L)) ||
                clusterMembers.contains(setOf(3L, 4L)) ||
                clusterMembers.contains(setOf(5L, 6L)),
        )
        result.forEach { cluster ->
            val ids = cluster.memberIds.toSet()
            assertTrue(
                ids == setOf(1L, 2L) || ids == setOf(3L, 4L) || ids == setOf(5L, 6L),
            )
        }
    }

    @Test
    fun cluster_kEqualsOne_putsAllItemsInOneCluster() {
        val items = buildSeparatedClusters()
        val result = KMeansClustering.cluster(items, k = 1)

        assertEquals(1, result.size)
        assertEquals(items.map { it.first }.toSet(), result[0].memberIds.toSet())
    }

    @Test
    fun cluster_emptyInput_returnsEmpty() {
        assertTrue(KMeansClustering.cluster(emptyList(), k = 3).isEmpty())
    }

    @Test
    fun cluster_centroidsAreNormalized() {
        val items = buildSeparatedClusters()
        val result = KMeansClustering.cluster(items, k = 3)
        result.forEach { cluster ->
            val norm = sqrt(cluster.centroid.fold(0f) { acc, v -> acc + v * v })
            assertEquals(1f, norm, 1e-5f)
        }
    }

    @Test
    fun suggestK_returnsValuesInRange() {
        assertEquals(3, KMeansClustering.suggestK(1))
        assertEquals(3, KMeansClustering.suggestK(10))
        assertEquals(5, KMeansClustering.suggestK(50))
        assertEquals(12, KMeansClustering.suggestK(500))
    }

    @Test
    fun cluster_sameInput_producesSameOutput() {
        val items = buildSeparatedClusters()
        val first = KMeansClustering.cluster(items, k = 3)
        val second = KMeansClustering.cluster(items, k = 3)

        assertEquals(first.size, second.size)
        first.zip(second).forEach { (a, b) ->
            assertEquals(a.memberIds.sorted(), b.memberIds.sorted())
            for (i in a.centroid.indices) {
                assertEquals(a.centroid[i], b.centroid[i], 1e-5f)
            }
        }
    }

    private fun buildSeparatedClusters(): List<Pair<Long, FloatArray>> {
        return listOf(
            1L to floatArrayOf(0.99f, 0.01f, 0f),
            2L to floatArrayOf(0.98f, 0.02f, 0f),
            3L to floatArrayOf(0.01f, 0.99f, 0f),
            4L to floatArrayOf(0.02f, 0.98f, 0f),
            5L to floatArrayOf(0f, 0.01f, 0.99f),
            6L to floatArrayOf(0f, 0.02f, 0.98f),
        )
    }
}
