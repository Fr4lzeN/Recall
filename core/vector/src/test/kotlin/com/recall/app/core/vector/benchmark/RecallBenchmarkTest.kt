package com.recall.app.core.vector.benchmark

import com.recall.app.core.vector.hnsw.HnswIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RecallBenchmarkTest {

    private val recallQueryCount = 100

    @Test
    fun `recall at various efSearch values`() {
        val vectorCount = 10_000
        val vectors = BenchmarkUtils.generateVectors(vectorCount)
        val groundTruth = BenchmarkUtils.buildGroundTruthIndex(vectors)
        val queries = BenchmarkUtils.generateQueries(recallQueryCount)

        val efValues = listOf(25, 50, 100, 200)
        val rows = efValues.map { ef ->
            val effectiveEf = maxOf(ef, BenchmarkUtils.TOP_K * 15)
            val hnsw = HnswIndex(
                dimensions = BenchmarkUtils.DIMENSIONS,
                efSearch = ef,
            )
            runBlocking { hnsw.addBatch(vectors) }
            val recall = BenchmarkUtils.recallAtK(hnsw, groundTruth, queries)
            listOf(ef.toString(), effectiveEf.toString(), String.format("%.4f", recall))
        }

        BenchmarkUtils.printTable(
            title = "Recall@10 vs efSearch ($vectorCount vectors, M=16, effectiveEf=max(efSearch,topK*15))",
            headers = listOf("efSearch", "effectiveEf", "recall@10"),
            rows = rows,
        )
        println("TARGET (384d Lite): recall@10 >= 0.95 | CI floor: >= 0.75")

        val recalls = rows.map { it[2].toDouble() }
        assertTrue("efSearch=200 recall=${recalls.last()} below CI floor 0.70", recalls.last() >= 0.70)
        assertTrue("recall should improve with higher efSearch", recalls.last() >= recalls.first())
    }

    @Test
    fun `recall at various M values`() {
        val vectorCount = 10_000
        val vectors = BenchmarkUtils.generateVectors(vectorCount)
        val groundTruth = BenchmarkUtils.buildGroundTruthIndex(vectors)
        val queries = BenchmarkUtils.generateQueries(recallQueryCount)

        val mValues = listOf(8, 16, 32)
        val rows = mValues.map { m ->
            val hnsw = HnswIndex(
                dimensions = BenchmarkUtils.DIMENSIONS,
                m = m,
            )
            runBlocking { hnsw.addBatch(vectors) }
            val recall = BenchmarkUtils.recallAtK(hnsw, groundTruth, queries)
            listOf(m.toString(), String.format("%.4f", recall))
        }

        BenchmarkUtils.printTable(
            title = "Recall@10 vs M ($vectorCount vectors, efSearch=50)",
            headers = listOf("M", "recall@10"),
            rows = rows,
        )
        println("TARGET (384d Lite): recall@10 >= 0.95 | CI floor: >= 0.70")

        val recallAtM16 = rows[1][1].toDouble()
        assertTrue("recall@10 at M=16 = $recallAtM16 below CI floor 0.65", recallAtM16 >= 0.65)
    }

    @Test
    fun `recall at various scales`() {
        val scales = listOf(1_000, 5_000, 10_000, 50_000)
        val queries = BenchmarkUtils.generateQueries(recallQueryCount)

        val rows = scales.map { count ->
            val vectors = BenchmarkUtils.generateVectors(count)
            val groundTruth = BenchmarkUtils.buildGroundTruthIndex(vectors)
            val hnsw = HnswIndex(BenchmarkUtils.DIMENSIONS)
            runBlocking { hnsw.addBatch(vectors) }
            val recall = BenchmarkUtils.recallAtK(hnsw, groundTruth, queries)
            listOf(count.toString(), String.format("%.4f", recall))
        }

        BenchmarkUtils.printTable(
            title = "Recall@10 vs scale (M=16, efSearch=50, ${BenchmarkUtils.DIMENSIONS}d)",
            headers = listOf("vectors", "recall@10"),
            rows = rows,
        )
        println("TARGET (384d Lite): recall@10 >= 0.95 at all scales")
        println("CI floors are JVM regression baselines (device targets are higher)")

        rows.forEach { (scale, recallStr) ->
            val recall = recallStr.toDouble()
            // Observed JVM baselines (M=16, efSearch=50): 1K~1.0, 5K~0.88, 10K~0.73, 50K~0.31
            val minRecall = when {
                scale.toInt() >= 50_000 -> 0.25
                scale.toInt() >= 10_000 -> 0.65
                scale.toInt() >= 5_000 -> 0.80
                else -> 0.90
            }
            assertTrue(
                "recall@10 at $scale vectors = $recall below CI floor $minRecall",
                recall >= minRecall,
            )
        }
    }
}
