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
        if (itemCount <= 1) return itemCount.coerceAtLeast(0)
        return sqrt(itemCount.toDouble()).toInt().coerceIn(2, 32).coerceAtMost(itemCount)
    }

    fun clusterAutomatically(
        items: List<Pair<Long, FloatArray>>,
        maxClusters: Int = 32,
        minSilhouette: Float = 0.2f,
        checkCancellation: () -> Unit = {},
    ): List<Cluster> {
        checkCancellation()
        if (items.size < 3 || maxClusters < 2) return cluster(items, k = 1, checkCancellation = checkCancellation)
        val sample = items.shuffled(Random(RANDOM_SEED)).take(160)
        val distances = Array(sample.size) { FloatArray(sample.size) }
        var maxDistance = 0f
        for (i in sample.indices) {
            checkCancellation()
            for (j in 0 until i) {
                val distance = cosineDistance(sample[i].second, sample[j].second).coerceAtLeast(0f)
                distances[i][j] = distance
                distances[j][i] = distance
                maxDistance = maxOf(maxDistance, distance)
            }
        }
        if (maxDistance < 1e-4f) return cluster(items, k = 1, checkCancellation = checkCancellation)

        val limit = minOf(maxClusters, sample.size / 2)
        val candidates = (listOf(2, 3, 4, 6, 8, 12, 16, 24, 32) + limit)
            .filter { it in 2..limit }.distinct().sorted()
        var selectedK = 1
        var bestScore = minSilhouette
        for (k in candidates) {
            checkCancellation()
            val clusters = cluster(sample, k, restarts = 1, checkCancellation = checkCancellation)
            if (clusters.size < 2) continue
            val groupById = clusters.flatMapIndexed { group, c -> c.memberIds.map { it to group } }.toMap()
            val assignments = IntArray(sample.size) { groupById.getValue(sample[it].first) }
            val counts = IntArray(clusters.size)
            assignments.forEach { counts[it]++ }
            var total = 0f
            for (i in sample.indices) {
                if (i % 32 == 0) checkCancellation()
                val group = assignments[i]
                if (counts[group] == 1) continue
                val sums = FloatArray(clusters.size)
                for (j in sample.indices) sums[assignments[j]] += distances[i][j]
                val ownDistance = sums[group] / (counts[group] - 1)
                val otherDistance = sums.indices.filter { it != group }
                    .minOf { sums[it] / counts[it] }
                val denominator = maxOf(ownDistance, otherDistance)
                if (denominator > 0f) total += (otherDistance - ownDistance) / denominator
            }
            val score = total / sample.size
            if (score > bestScore + 1e-4f) {
                bestScore = score
                selectedK = clusters.size
            }
        }
        return cluster(items, selectedK, checkCancellation = checkCancellation)
    }

    fun cluster(
        items: List<Pair<Long, FloatArray>>,
        k: Int,
        maxIterations: Int = DEFAULT_MAX_ITERATIONS,
        restarts: Int = 3,
        checkCancellation: () -> Unit = {},
    ): List<Cluster> {
        checkCancellation()
        if (items.isEmpty()) return emptyList()
        if (k <= 0) return emptyList()
        require(maxIterations > 0) { "maxIterations must be positive" }
        require(restarts > 0) { "restarts must be positive" }
        val dimensions = items.first().second.size
        require(dimensions > 0 && items.all { it.second.size == dimensions })
        require(items.all { (_, vector) -> vector.all { it.isFinite() } })
        val effectiveK = minOf(k, items.size)
        val vectors = items.mapIndexed { index, (_, vector) ->
            if (index % 32 == 0) checkCancellation()
            normalize(vector.copyOf())
        }
        val ids = items.map { it.first }

        var bestClusters = emptyList<Cluster>()
        var bestLoss = Float.POSITIVE_INFINITY
        repeat(restarts) { restart ->
            checkCancellation()
            var centroids = initializeCentroidsKMeansPlusPlus(vectors, effectiveK, RANDOM_SEED + restart)
            val assignments = IntArray(items.size) { -1 }
            for (iteration in 0 until maxIterations) {
                checkCancellation()
                var changed = false
                for (i in vectors.indices) {
                    if (i % 32 == 0) checkCancellation()
                    val nearest = nearestCentroid(vectors[i], centroids)
                    if (assignments[i] != nearest) {
                        assignments[i] = nearest
                        changed = true
                    }
                }
                centroids = recomputeCentroids(vectors, assignments, effectiveK, centroids)
                if (!changed) break
            }
            val loss = vectors.indices.sumOf { i -> cosineDistance(vectors[i], centroids[assignments[i]]).toDouble() }.toFloat()
            if (loss < bestLoss) {
                bestLoss = loss
                bestClusters = buildClusters(ids, assignments, centroids, effectiveK)
            }
        }
        return bestClusters
    }

    private fun initializeCentroidsKMeansPlusPlus(
        vectors: List<FloatArray>,
        k: Int,
        seed: Long,
    ): List<FloatArray> {
        val random = Random(seed)
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
