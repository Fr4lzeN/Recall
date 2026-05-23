package com.recall.app.core.vector.clustering

import com.recall.app.core.vector.distance.VectorDistance
import kotlin.math.sqrt
import kotlin.random.Random

data class Cluster(
    val centroid: FloatArray,
    val memberIds: List<Long>,
)

object KMeansClustering {
    private const val DEFAULT_MAX_ITERATIONS = 20
    private const val RANDOM_SEED = 42L

    fun suggestK(itemCount: Int): Int {
        if (itemCount <= 0) return 3
        val raw = sqrt(itemCount / 2.0).toInt()
        return raw.coerceIn(3, 12)
    }

    fun cluster(
        items: List<Pair<Long, FloatArray>>,
        k: Int,
        maxIterations: Int = DEFAULT_MAX_ITERATIONS,
    ): List<Cluster> {
        if (items.isEmpty()) return emptyList()
        if (k <= 0) return emptyList()
        val effectiveK = minOf(k, items.size)
        val vectors = items.map { (_, vector) -> normalize(vector.copyOf()) }
        val ids = items.map { it.first }

        var centroids = initializeCentroidsKMeansPlusPlus(vectors, effectiveK)
        var assignments = IntArray(items.size)

        repeat(maxIterations) {
            var changed = false
            for (i in vectors.indices) {
                val nearest = nearestCentroid(vectors[i], centroids)
                if (assignments[i] != nearest) {
                    assignments[i] = nearest
                    changed = true
                }
            }
            centroids = recomputeCentroids(vectors, assignments, effectiveK, centroids)
            if (!changed) return@repeat
        }

        return buildClusters(ids, assignments, centroids, effectiveK)
    }

    private fun initializeCentroidsKMeansPlusPlus(
        vectors: List<FloatArray>,
        k: Int,
    ): List<FloatArray> {
        val random = Random(RANDOM_SEED)
        val centroids = mutableListOf<FloatArray>()
        centroids.add(vectors[random.nextInt(vectors.size)].copyOf())

        while (centroids.size < k) {
            val distances = vectors.map { vector ->
                centroids.minOf { centroid -> cosineDistance(vector, centroid) }
            }
            val total = distances.sum()
            val pick = if (total <= 0f) {
                random.nextInt(vectors.size)
            } else {
                var threshold = random.nextFloat() * total
                var index = 0
                for (i in distances.indices) {
                    threshold -= distances[i]
                    if (threshold <= 0f) {
                        index = i
                        break
                    }
                    index = i
                }
                index
            }
            centroids.add(vectors[pick].copyOf())
        }
        return centroids
    }

    private fun nearestCentroid(vector: FloatArray, centroids: List<FloatArray>): Int {
        var bestIndex = 0
        var bestDistance = Float.MAX_VALUE
        centroids.forEachIndexed { index, centroid ->
            val distance = cosineDistance(vector, centroid)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun recomputeCentroids(
        vectors: List<FloatArray>,
        assignments: IntArray,
        k: Int,
        previousCentroids: List<FloatArray>,
    ): List<FloatArray> {
        val dimensions = vectors.first().size
        val sums = Array(k) { FloatArray(dimensions) }
        val counts = IntArray(k)

        vectors.forEachIndexed { index, vector ->
            val cluster = assignments[index]
            counts[cluster]++
            for (d in 0 until dimensions) {
                sums[cluster][d] += vector[d]
            }
        }

        return List(k) { cluster ->
            if (counts[cluster] == 0) {
                previousCentroids[cluster].copyOf()
            } else {
                val averaged = FloatArray(dimensions) { d -> sums[cluster][d] / counts[cluster] }
                normalize(averaged)
            }
        }
    }

    private fun buildClusters(
        ids: List<Long>,
        assignments: IntArray,
        centroids: List<FloatArray>,
        k: Int,
    ): List<Cluster> {
        val members = Array(k) { mutableListOf<Long>() }
        ids.forEachIndexed { index, id ->
            members[assignments[index]].add(id)
        }
        return centroids.mapIndexed { index, centroid ->
            Cluster(
                centroid = centroid.copyOf(),
                memberIds = members[index].toList(),
            )
        }.filter { it.memberIds.isNotEmpty() }
    }

    private fun cosineDistance(a: FloatArray, b: FloatArray): Float {
        return 1f - VectorDistance.cosineSimilarity(a, b)
    }

    private fun normalize(vector: FloatArray): FloatArray {
        var norm = 0f
        for (value in vector) {
            norm += value * value
        }
        norm = sqrt(norm)
        if (norm <= 0f) return vector
        for (i in vector.indices) {
            vector[i] /= norm
        }
        return vector
    }
}
