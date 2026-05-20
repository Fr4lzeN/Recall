package com.recall.app.core.vector.segment

import com.recall.app.core.vector.SearchResult
import com.recall.app.core.vector.bitmap.DeletionBitmap
import com.recall.app.core.vector.distance.VectorDistance
import java.util.PriorityQueue

object SegmentHnswSearch {
    fun search(
        reader: SegmentReader,
        deletionBitmap: DeletionBitmap?,
        query: FloatArray,
        localIndexToMediaId: LongArray,
        topK: Int,
        efSearch: Int = 50,
    ): List<SearchResult> {
        require(query.size == reader.dimensions) {
            "Expected ${reader.dimensions} dimensions, got ${query.size}"
        }
        if (topK <= 0 || reader.vectorCount == 0 || reader.entryPoint < 0) return emptyList()

        var currentEp = reader.entryPoint
        if (deletionBitmap?.isDeleted(currentEp) == true) {
            currentEp = findLiveEntry(reader, deletionBitmap) ?: return emptyList()
        }

        for (layer in reader.maxLevel downTo 1) {
            currentEp = greedyClosest(reader, deletionBitmap, query, currentEp, layer)
        }

        val ef = maxOf(efSearch, topK * 15)
        val candidates = searchLayer(reader, deletionBitmap, query, setOf(currentEp), ef, layer = 0)
        return candidates
            .asSequence()
            .filter { (localIndex, _) -> deletionBitmap?.isDeleted(localIndex) != true }
            .take(topK)
            .map { (localIndex, dist) ->
                SearchResult(
                    id = localIndexToMediaId[localIndex],
                    score = 1f - dist,
                    distance = dist,
                )
            }
            .toList()
    }

    private fun findLiveEntry(reader: SegmentReader, deletionBitmap: DeletionBitmap): Int? {
        for (i in 0 until reader.vectorCount) {
            if (!deletionBitmap.isDeleted(i)) return i
        }
        return null
    }

    private fun searchLayer(
        reader: SegmentReader,
        deletionBitmap: DeletionBitmap?,
        query: FloatArray,
        entryPoints: Set<Int>,
        ef: Int,
        layer: Int,
    ): List<Pair<Int, Float>> {
        val visited = HashSet<Int>(entryPoints)
        val candidates = PriorityQueue<ScoredLocal>(compareBy { it.distance })
        val results = PriorityQueue<ScoredLocal>(compareByDescending { it.distance })

        for (ep in entryPoints) {
            if (deletionBitmap?.isDeleted(ep) == true) continue
            val dist = distance(reader, query, ep)
            candidates.add(ScoredLocal(ep, dist))
            results.add(ScoredLocal(ep, dist))
        }

        while (candidates.isNotEmpty()) {
            val nearest = candidates.poll() ?: break
            val farthest = results.peek() ?: break
            if (nearest.distance > farthest.distance) break

            if (layer > reader.readNodeLevel(nearest.localIndex)) continue

            for (neighborId in reader.readNeighbors(nearest.localIndex, layer)) {
                if (!visited.add(neighborId)) continue
                if (deletionBitmap?.isDeleted(neighborId) == true) continue
                val dist = distance(reader, query, neighborId)
                val worst = results.peek()
                if (worst == null || dist < worst.distance || results.size < ef) {
                    candidates.add(ScoredLocal(neighborId, dist))
                    results.add(ScoredLocal(neighborId, dist))
                    if (results.size > ef) {
                        results.poll()
                    }
                }
            }
        }

        return results.sortedBy { it.distance }.map { it.localIndex to it.distance }
    }

    private fun greedyClosest(
        reader: SegmentReader,
        deletionBitmap: DeletionBitmap?,
        query: FloatArray,
        startId: Int,
        layer: Int,
    ): Int {
        var current = startId
        while (true) {
            if (layer > reader.readNodeLevel(current)) return current
            var bestId = current
            var bestDist = distance(reader, query, current)
            for (neighborId in reader.readNeighbors(current, layer)) {
                if (deletionBitmap?.isDeleted(neighborId) == true) continue
                val dist = distance(reader, query, neighborId)
                if (dist < bestDist) {
                    bestDist = dist
                    bestId = neighborId
                }
            }
            if (bestId == current) return current
            current = bestId
        }
    }

    private fun distance(reader: SegmentReader, query: FloatArray, localIndex: Int): Float {
        val vector = reader.readVector(localIndex)
        return 1f - VectorDistance.cosineSimilarity(query, vector)
    }

    private data class ScoredLocal(val localIndex: Int, val distance: Float)
}
