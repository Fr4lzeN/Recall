package com.recall.app.feature.albums

import com.recall.app.core.vector.clustering.Cluster
import com.recall.app.core.vector.distance.VectorDistance

internal object AlbumGroupingPolicy {
    // Conservative heuristics: uncertain groups keep a generic name.
    private const val MIN_LABEL_SIMILARITY = 0.20f
    private const val MIN_LABEL_MARGIN = 0.025f
    private const val MIN_MEMBER_AGREEMENT = 0.70f
    private const val MIN_OVERLAP_SIMILARITY = 0.65f
    private const val MAX_OVERLAP_GAP = 0.03f
    private const val MAX_CENTROID_SIMILARITY = 0.95f

    fun withOverlap(
        clusters: List<Cluster>,
        vectors: List<Pair<Long, FloatArray>>,
        checkCancellation: () -> Unit = {},
    ): List<Cluster> {
        checkCancellation()
        if (clusters.size < 2) return clusters
        val primaryById = mutableMapOf<Long, Int>()
        clusters.forEachIndexed { index, cluster ->
            cluster.memberIds.forEach {
                checkCancellation()
                primaryById[it] = index
            }
        }
        val members = clusters.map { it.memberIds.toMutableSet() }
        for ((id, vector) in vectors) {
            checkCancellation()
            val primary = primaryById[id] ?: continue
            val primaryScore = similarity(vector, clusters[primary].centroid)
            val secondary = clusters.indices.asSequence()
                .filter { it != primary }
                .map {
                    checkCancellation()
                    it to similarity(vector, clusters[it].centroid)
                }
                .maxByOrNull { it.second } ?: continue
            if (secondary.second >= MIN_OVERLAP_SIMILARITY &&
                kotlin.math.abs(primaryScore - secondary.second) <= MAX_OVERLAP_GAP &&
                similarity(clusters[primary].centroid, clusters[secondary.first].centroid) <
                MAX_CENTROID_SIMILARITY
            ) {
                // At most one additional album for a photograph on a cluster boundary.
                members[secondary.first].add(id)
            }
        }
        return clusters.mapIndexed { index, cluster ->
            checkCancellation()
            cluster.copy(memberIds = members[index].toList())
        }
    }

    fun name(
        centroid: FloatArray,
        memberVectors: List<FloatArray>,
        conceptEmbeddings: Map<String, FloatArray>,
        fallbackName: String,
        checkCancellation: () -> Unit = {},
    ): String {
        checkCancellation()
        if (memberVectors.isEmpty() || conceptEmbeddings.isEmpty()) return fallbackName
        val best = confidentLabel(centroid, conceptEmbeddings, checkCancellation) ?: return fallbackName
        val agreement = memberVectors.count {
            checkCancellation()
            confidentLabel(it, conceptEmbeddings, checkCancellation) == best
        }.toFloat() / memberVectors.size
        return if (agreement >= MIN_MEMBER_AGREEMENT) best else fallbackName
    }

    private fun confidentLabel(
        vector: FloatArray,
        concepts: Map<String, FloatArray>,
        checkCancellation: () -> Unit,
    ): String? {
        val ranked = concepts.map { (name, embedding) ->
            checkCancellation()
            name to similarity(vector, embedding)
        }
            .sortedByDescending { it.second }
        val best = ranked.firstOrNull() ?: return null
        val runnerUp = ranked.getOrNull(1)?.second ?: Float.NEGATIVE_INFINITY
        return best.first.takeIf {
            best.second >= MIN_LABEL_SIMILARITY && best.second - runnerUp >= MIN_LABEL_MARGIN
        }
    }

    private fun similarity(a: FloatArray, b: FloatArray): Float =
        VectorDistance.cosineSimilarity(a, b).takeIf { it.isFinite() } ?: -1f
}
