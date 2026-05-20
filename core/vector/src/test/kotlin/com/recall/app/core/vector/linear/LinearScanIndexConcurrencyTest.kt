package com.recall.app.core.vector.linear

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class LinearScanIndexConcurrencyTest {
    private lateinit var index: LinearScanIndex

    @Before
    fun setUp() {
        index = LinearScanIndex(dimensions = 8)
    }

    @Test
    fun concurrentAddAndSearch_doesNotCrash() = runTest {
        val random = Random(7)
        val jobs = (1..50).map { batch ->
            async {
                repeat(20) { i ->
                    val id = batch * 1000L + i
                    val vector = FloatArray(8) { random.nextFloat() }
                    index.add(id, vector)
                    index.search(vector, topK = 3)
                }
            }
        }
        jobs.awaitAll()
        assertTrue(index.size() > 0)
    }

    @Test
    fun searchDuringAdd_returnsResultsWithoutCrashing() = runTest {
        val query = FloatArray(8) { 1f }
        val addJob = async {
            repeat(500) { i ->
                index.add(i.toLong(), FloatArray(8) { it.toFloat() })
            }
        }
        val searchJob = async {
            repeat(500) {
                val results = index.search(query, topK = 5)
                assertTrue(results.size <= 5)
            }
        }
        addJob.await()
        searchJob.await()
    }

    @Test
    fun concurrentAddBatchAndRemove_doesNotCrash() = runTest {
        val jobs = listOf(
            async {
                repeat(30) { batch ->
                    val entries = (0 until 50).map { i ->
                        (batch * 50L + i) to FloatArray(8) { 0.1f * i }
                    }
                    index.addBatch(entries)
                }
            },
            async {
                repeat(200) {
                    index.remove(it.toLong())
                    index.contains(it.toLong())
                }
            },
            async {
                repeat(200) {
                    index.search(FloatArray(8) { 0.5f }, topK = 10)
                }
            },
        )
        jobs.awaitAll()
    }
}
