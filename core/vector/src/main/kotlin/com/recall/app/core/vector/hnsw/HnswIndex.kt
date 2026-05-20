package com.recall.app.core.vector.hnsw

import com.recall.app.core.vector.SearchResult
import com.recall.app.core.vector.VectorIndex
import com.recall.app.core.vector.distance.VectorDistance
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.PriorityQueue
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.ln
import kotlin.math.min

class HnswIndex(
    private val dimensions: Int,
    private val m: Int = 16,
    private val efConstruction: Int = 200,
    private val efSearch: Int = 50,
    private val maxLevel: Int = 16,
) : VectorIndex {

    private data class Node(
        val id: Long,
        val vector: FloatArray,
        val level: Int,
        val neighbors: Array<MutableList<Long>>,
    )

    private data class ScoredId(val id: Long, val distance: Float) : Comparable<ScoredId> {
        override fun compareTo(other: ScoredId): Int = distance.compareTo(other.distance)
    }

    private val nodes = ConcurrentHashMap<Long, Node>()
    private var entryPointId: Long? = null
    private var currentMaxLevel: Int = 0
    private val lock = ReentrantReadWriteLock()
    private val random = Random(42)
    private val mL = 1.0 / ln(m.toDouble())

    override suspend fun add(id: Long, vector: FloatArray) {
        require(vector.size == dimensions) { "Expected $dimensions dimensions, got ${vector.size}" }
        lock.write {
            if (nodes.containsKey(id)) {
                removeInternal(id)
            }
            insert(id, vector.copyOf())
        }
    }

    override suspend fun addBatch(entries: List<Pair<Long, FloatArray>>) {
        lock.write {
            entries.forEach { (id, vector) ->
                require(vector.size == dimensions) { "Expected $dimensions dimensions, got ${vector.size}" }
                if (nodes.containsKey(id)) {
                    removeInternal(id)
                }
                insert(id, vector.copyOf())
            }
        }
    }

    override suspend fun search(query: FloatArray, topK: Int): List<SearchResult> {
        require(query.size == dimensions) { "Expected $dimensions dimensions, got ${query.size}" }
        if (topK <= 0) return emptyList()

        return lock.read {
            val entry = entryPointId ?: return@read emptyList()
            if (nodes.isEmpty()) return@read emptyList()

            var currentEp = entry
            for (layer in currentMaxLevel downTo 1) {
                currentEp = greedyClosest(query, currentEp, layer)
            }

            val ef = maxOf(efSearch, topK * 15)
            val candidates = searchLayer(query, setOf(currentEp), ef = ef, layer = 0)
            candidates
                .take(topK)
                .map { (nodeId, dist) ->
                    val score = 1f - dist
                    SearchResult(id = nodeId, score = score, distance = dist)
                }
        }
    }

    override suspend fun remove(id: Long) {
        lock.write { removeInternal(id) }
    }

    override suspend fun contains(id: Long): Boolean = lock.read { nodes.containsKey(id) }

    override fun size(): Int = nodes.size

    override fun dimensions(): Int = dimensions

    override fun clear() {
        lock.write {
            nodes.clear()
            entryPointId = null
            currentMaxLevel = 0
        }
    }

    fun serialize(): ByteArray = lock.read {
        val nodeList = nodes.values.toList()
        var size = 4 + 4 + 4 + 8 + 4 // header
        nodeList.forEach { node ->
            size += 8 + 4 + dimensions * 4
            for (layer in 0..node.level) {
                size += 4 + node.neighbors[layer].size * 8
            }
        }

        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(dimensions)
        buffer.putInt(m)
        buffer.putInt(nodeList.size)
        buffer.putLong(entryPointId ?: -1L)
        buffer.putInt(currentMaxLevel)

        nodeList.forEach { node ->
            buffer.putLong(node.id)
            buffer.putInt(node.level)
            node.vector.forEach { buffer.putFloat(it) }
            for (layer in 0..node.level) {
                val neighbors = node.neighbors[layer]
                buffer.putInt(neighbors.size)
                neighbors.forEach { buffer.putLong(it) }
            }
        }
        buffer.array()
    }

    private fun insert(id: Long, vector: FloatArray) {
        val level = randomLevel()
        val node = Node(
            id = id,
            vector = vector,
            level = level,
            neighbors = Array(level + 1) { mutableListOf() },
        )
        nodes[id] = node

        val entry = entryPointId
        if (entry == null) {
            entryPointId = id
            currentMaxLevel = level
            return
        }

        var ep = checkNotNull(entry)
        if (level <= currentMaxLevel) {
            for (layer in currentMaxLevel downTo level + 1) {
                ep = greedyClosest(vector, ep, layer)
            }
        }

        for (layer in min(level, currentMaxLevel) downTo 0) {
            val maxConn = maxConnections(layer)
            val candidates = searchLayer(vector, setOf(ep), ef = efConstruction, layer = layer)
            val selected = selectNeighbors(vector, candidates, maxConn)
            node.neighbors[layer].addAll(selected)
            for (neighborId in selected) {
                val neighbor = nodes[neighborId] ?: continue
                if (layer > neighbor.level) continue
                neighbor.neighbors[layer].add(id)
                if (neighbor.neighbors[layer].size > maxConn) {
                    val neighborCandidates = neighbor.neighbors[layer].mapNotNull { nid ->
                        val n = nodes[nid] ?: return@mapNotNull null
                        nid to distanceVectors(neighbor.vector, n.vector)
                    }
                    val pruned = selectNeighbors(vector, neighborCandidates, maxConn)
                    neighbor.neighbors[layer].clear()
                    neighbor.neighbors[layer].addAll(pruned)
                }
            }
            if (candidates.isNotEmpty()) {
                ep = candidates.first().first
            }
        }

        if (level > currentMaxLevel) {
            currentMaxLevel = level
            entryPointId = id
        }
    }

    private fun removeInternal(id: Long) {
        val node = nodes.remove(id) ?: return
        for (layer in 0..node.level) {
            node.neighbors[layer].forEach { neighborId ->
                nodes[neighborId]?.neighbors?.getOrNull(layer)?.remove(id)
            }
        }
        if (entryPointId == id) {
            entryPointId = nodes.values.maxByOrNull { it.level }?.id
            currentMaxLevel = nodes.values.maxOfOrNull { it.level } ?: 0
        }
    }

    private fun searchLayer(
        query: FloatArray,
        entryPoints: Set<Long>,
        ef: Int,
        layer: Int,
    ): List<Pair<Long, Float>> {
        val visited = HashSet<Long>(entryPoints)
        val candidates = PriorityQueue<ScoredId>()
        val results = PriorityQueue<ScoredId>(compareByDescending { it.distance })

        for (ep in entryPoints) {
            if (!nodes.containsKey(ep)) continue
            val dist = distance(query, ep)
            candidates.add(ScoredId(ep, dist))
            results.add(ScoredId(ep, dist))
        }

        while (candidates.isNotEmpty()) {
            val nearest = candidates.poll() ?: break
            val farthest = results.peek() ?: break
            if (nearest.distance > farthest.distance) break

            val current = nodes[nearest.id] ?: continue
            if (layer > current.level) continue
            val neighborIds = current.neighbors[layer]
            for (neighborId in neighborIds) {
                if (!visited.add(neighborId)) continue
                if (!nodes.containsKey(neighborId)) continue
                val dist = distance(query, neighborId)
                val worst = results.peek()
                if (worst == null || dist < worst.distance || results.size < ef) {
                    candidates.add(ScoredId(neighborId, dist))
                    results.add(ScoredId(neighborId, dist))
                    if (results.size > ef) {
                        results.poll()
                    }
                }
            }
        }

        return results.sortedBy { it.distance }.map { it.id to it.distance }
    }

    private fun selectNeighbors(
        query: FloatArray,
        candidates: List<Pair<Long, Float>>,
        maxNeighbors: Int,
    ): List<Long> {
        if (candidates.isEmpty()) return emptyList()
        val sorted = candidates.sortedBy { it.second }
        val selected = mutableListOf<Long>()
        for ((candidateId, _) in sorted) {
            if (selected.size >= maxNeighbors) break
            val candidateVector = nodes[candidateId]?.vector ?: continue
            var good = true
            for (selectedId in selected) {
                val selectedVector = nodes[selectedId]?.vector ?: continue
                if (distanceVectors(candidateVector, selectedVector) <
                    distanceVectors(query, candidateVector)
                ) {
                    good = false
                    break
                }
            }
            if (good) selected.add(candidateId)
        }
        if (selected.size < maxNeighbors) {
            for ((candidateId, _) in sorted) {
                if (candidateId !in selected) {
                    selected.add(candidateId)
                    if (selected.size >= maxNeighbors) break
                }
            }
        }
        return selected
    }

    private fun greedyClosest(query: FloatArray, startId: Long, layer: Int): Long {
        var current = startId
        while (true) {
            val node = nodes[current] ?: return current
            if (layer > node.level) return current
            var bestId = current
            var bestDist = distance(query, current)
            for (neighborId in node.neighbors[layer]) {
                if (!nodes.containsKey(neighborId)) continue
                val dist = distance(query, neighborId)
                if (dist < bestDist) {
                    bestDist = dist
                    bestId = neighborId
                }
            }
            if (bestId == current) return current
            current = bestId
        }
    }

    private fun maxConnections(layer: Int): Int = if (layer == 0) m * 2 else m

    private fun randomLevel(): Int {
        val r = random.nextDouble().coerceIn(1e-10, 1.0 - 1e-10)
        val level = (-ln(r) * mL).toInt()
        return min(level, maxLevel)
    }

    private fun distance(query: FloatArray, nodeId: Long): Float {
        val node = nodes[nodeId] ?: return Float.MAX_VALUE
        return distanceVectors(query, node.vector)
    }

    private fun distanceVectors(a: FloatArray, b: FloatArray): Float {
        return 1f - VectorDistance.cosineSimilarity(a, b)
    }

    /**
     * Exports graph structure for on-disk segment files. Nodes must use contiguous local IDs
     * `0..<vectorCount` (typically assigned by [SegmentWriter]).
     */
    fun exportEntries(): List<Pair<Long, FloatArray>> = lock.read {
        nodes.values.map { it.id to it.vector.copyOf() }.sortedBy { it.first }
    }

    fun exportForSegment(vectorCount: Int): SegmentGraphExport = lock.read {
        require(vectorCount > 0) { "vectorCount must be positive" }
        val vectors = Array(vectorCount) { i ->
            val node = nodes[i.toLong()] ?: error("Missing node for local index $i")
            node.vector.copyOf()
        }
        val levels = IntArray(vectorCount) { i -> nodes[i.toLong()]!!.level }
        val neighborsByLayer = List(vectorCount) { i ->
            val node = nodes[i.toLong()]!!
            Array(node.level + 1) { layer ->
                node.neighbors[layer].map { neighborId ->
                    neighborId.toInt().also { local ->
                        require(local in 0 until vectorCount) {
                            "Neighbor id $neighborId out of range for segment size $vectorCount"
                        }
                    }
                }
            }
        }
        val entry = entryPointId?.toInt() ?: -1
        SegmentGraphExport(
            entryPoint = entry,
            maxLevel = currentMaxLevel,
            m = m,
            vectors = vectors.toList(),
            levels = levels,
            neighborsByLayer = neighborsByLayer,
        )
    }

    companion object {
        private const val NULL_ENTRY_POINT = -1L

        fun deserialize(data: ByteArray, dimensions: Int): HnswIndex {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val fileDimensions = buffer.getInt()
            require(fileDimensions == dimensions) {
                "Expected dimensions $dimensions, got $fileDimensions"
            }
            val fileM = buffer.getInt()
            val nodeCount = buffer.getInt()
            val entryPoint = buffer.getLong()
            val fileMaxLevel = buffer.getInt()

            val index = HnswIndex(dimensions = dimensions, m = fileM)
            index.currentMaxLevel = fileMaxLevel
            index.entryPointId = if (entryPoint == NULL_ENTRY_POINT) null else entryPoint

            repeat(nodeCount) {
                val id = buffer.getLong()
                val level = buffer.getInt()
                val vector = FloatArray(dimensions) { buffer.getFloat() }
                val neighbors = Array(level + 1) { layer ->
                    val count = buffer.getInt()
                    MutableList(count) { buffer.getLong() }
                }
                index.nodes[id] = Node(id, vector, level, neighbors)
            }
            return index
        }
    }
}
